package com.boox.einkdraw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Full-screen drawing surface backed by the Onyx hardware pen chip.
 *
 * Architecture:
 *  - TouchHelper drives zero-latency hardware preview (setRawDrawingRenderEnabled=false).
 *  - RawInputCallback accumulates points and renders to the active software layer.
 *  - On pen-up / onPenUpRefresh the hardware preview clears and composed layers are shown.
 */
class HardwarePenSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    companion object {
        private const val TAG = "HardwarePenSurface"
    }

    data class LayerInfo(
        val id: Int,
        val name: String,
        val visible: Boolean,
        val opacity: Float,
        val active: Boolean,
    )

    data class LayerSnapshot(
        val name: String,
        val visible: Boolean,
        val opacity: Float,
        val bitmap: Bitmap,
    )

    data class DocumentSnapshot(
        val width: Int,
        val height: Int,
        val activeLayerIndex: Int,
        val layers: List<LayerSnapshot>,
    )

    private data class LayerState(
        val id: Int,
        var name: String,
        var visible: Boolean,
        var opacity: Float,
        val bitmap: Bitmap,
        val canvas: Canvas,
        val snapshotBitmap: Bitmap,
        val snapshotCanvas: Canvas,
    )

    private enum class ViewGestureMode {
        NONE,
        PAN,
        PINCH,
    }

    // Active pen state
    private var activeStyle = HardwarePenStyle.PENCIL
    private var activeWidthPx = HardwarePenStyle.PENCIL.defaultWidthPx
    private var activeColor = Color.BLACK
    private var rawInputSuppressed = false

    // Viewport transform (software canvas only; hardware preview stays 1.0x)
    @Volatile
    private var viewScale = 1f
    @Volatile
    private var viewOffsetX = 0f
    @Volatile
    private var viewOffsetY = 0f
    private val minViewScale = 1f
    private val maxViewScale = 4f
    private var viewGestureMode = ViewGestureMode.NONE
    private var panLastX = 0f
    private var panLastY = 0f
    private var pinchStartDistance = 0f
    private var pinchStartScale = 1f
    private var pinchAnchorWorldX = 0f
    private var pinchAnchorWorldY = 0f

    // Layers (index 0 = bottom, last = top)
    private val layers = ArrayList<LayerState>(8)
    private var nextLayerId = 1
    private var activeLayerId = -1

    // In-flight stroke
    private var strokeStyle = HardwarePenStyle.PENCIL
    private var strokeWidthPx = 5f
    private var strokeColor = Color.BLACK
    private var strokeLayerId = -1
    private val strokePoints = ArrayList<TouchPoint>(256)
    private val rawMovePoints = ArrayList<TouchPoint>(256)
    private var receivedAuthorityList = false
    private var pendingPenUpRefresh = false
    private var hasRenderedThisStroke = false
    private var strokeInProgress = false
    private var strokeViewScale = 1f
    private var strokeViewOffsetX = 0f
    private var strokeViewOffsetY = 0f

    // TouchHelper (configured on a background thread)
    private val helperThread = Executors.newSingleThreadExecutor()
    private var touchHelper: TouchHelper? = null
    private var viewportListener: ((Float) -> Unit)? = null
    @Volatile
    private var viewportGestureSuppressRaw = false

    // Reused paint for layer-alpha composition
    private val layerPaint = Paint().apply { isFilterBitmap = true }

    // Public API

    fun setStyle(style: HardwarePenStyle) {
        activeStyle = style
        reconfigureTouchHelper()
    }

    fun setStrokeWidthPx(widthPx: Float) {
        activeWidthPx = widthPx.coerceIn(1f, 200f)
        reconfigureTouchHelper()
    }

    fun setStrokeColor(color: Int) {
        activeColor = color
        reconfigureTouchHelper()
    }

    fun setOnViewportChangedListener(listener: ((Float) -> Unit)?) {
        viewportListener = listener
        listener?.invoke(viewScale)
    }

    fun getViewScale(): Float = viewScale

    fun resetViewport() {
        viewScale = 1f
        viewOffsetX = 0f
        viewOffsetY = 0f
        viewGestureMode = ViewGestureMode.NONE
        setViewportGestureRawSuppressed(false)
        notifyViewportChanged()
        invalidateSurface()
    }

    /**
     * Suppress/restore hardware pen overlay while the user interacts with UI controls.
     */
    fun setRawInputSuppressed(suppressed: Boolean) {
        rawInputSuppressed = suppressed
        val helper = touchHelper ?: return
        helperThread.execute {
            runCatching { helper.setRawDrawingEnabled(!(suppressed || viewportGestureSuppressRaw)) }
        }
    }

    fun getLayerInfos(): List<LayerInfo> {
        val activeId = activeLayerId
        return layers.asReversed().map { layer ->
            LayerInfo(
                id = layer.id,
                name = layer.name,
                visible = layer.visible,
                opacity = layer.opacity,
                active = layer.id == activeId,
            )
        }
    }

    fun addLayer(): Int {
        ensureLayerStack(width, height)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return activeLayerId
        val layer = createLayer(
            id = nextLayerId++,
            name = "Layer ${layers.size + 1}",
            w = w,
            h = h,
            visible = true,
            opacity = 1f,
        )
        layers.add(layer)
        activeLayerId = layer.id
        updateSnapshot(layer.id)
        invalidateSurface()
        return layer.id
    }

    fun removeLayer(id: Int): Boolean {
        if (layers.size <= 1) return false
        val idx = layers.indexOfFirst { it.id == id }
        if (idx < 0) return false

        val removed = layers.removeAt(idx)
        recycleLayer(removed)

        if (activeLayerId == id) {
            val nextIdx = idx.coerceAtMost(layers.lastIndex)
            activeLayerId = layers[nextIdx].id
        }

        updateSnapshot(activeLayerId)
        invalidateSurface()
        return true
    }

    fun setActiveLayer(id: Int): Boolean {
        if (layers.none { it.id == id }) return false
        activeLayerId = id
        updateSnapshot(id)
        return true
    }

    /**
     * Reorder layers from top-down display indices.
     */
    fun moveLayerByDisplayIndices(fromDisplayIndex: Int, toDisplayIndex: Int): Boolean {
        val n = layers.size
        if (fromDisplayIndex !in 0 until n || toDisplayIndex !in 0 until n) return false
        val fromInternal = n - 1 - fromDisplayIndex
        val toInternal = n - 1 - toDisplayIndex
        if (fromInternal == toInternal) return true

        val layer = layers.removeAt(fromInternal)
        layers.add(toInternal, layer)
        invalidateSurface()
        return true
    }

    fun setLayerVisible(id: Int, visible: Boolean): Boolean {
        val layer = layerById(id) ?: return false
        if (layer.visible == visible) return true
        layer.visible = visible
        invalidateSurface()
        return true
    }

    fun setLayerOpacity(id: Int, opacity: Float): Boolean {
        val layer = layerById(id) ?: return false
        val v = opacity.coerceIn(0f, 1f)
        if (abs(layer.opacity - v) < 0.0001f) return true
        layer.opacity = v
        invalidateSurface()
        return true
    }

    fun clearCurrentLayer(): Boolean {
        ensureLayerStack(width, height)
        val layer = activeLayer() ?: return false
        clearBitmap(layer.canvas)
        clearBitmap(layer.snapshotCanvas)
        if (strokeLayerId == layer.id) {
            strokePoints.clear()
            rawMovePoints.clear()
            receivedAuthorityList = false
            pendingPenUpRefresh = false
            hasRenderedThisStroke = false
            strokeInProgress = false
        }
        invalidateSurface()
        return true
    }

    fun clearFile() {
        strokePoints.clear()
        rawMovePoints.clear()
        receivedAuthorityList = false
        pendingPenUpRefresh = false
        hasRenderedThisStroke = false
        strokeInProgress = false
        strokeLayerId = -1

        layers.forEach { recycleLayer(it) }
        layers.clear()
        nextLayerId = 1

        val w = width
        val h = height
        if (w > 0 && h > 0) {
            val base = createLayer(
                id = nextLayerId++,
                name = "Layer 1",
                w = w,
                h = h,
                visible = true,
                opacity = 1f,
            )
            layers.add(base)
            activeLayerId = base.id
            updateSnapshot(base.id)
        } else {
            activeLayerId = -1
        }
        invalidateSurface()
    }

    fun clearCanvas() {
        clearFile()
    }

    /**
     * Loads the bitmap into the currently active layer.
     */
    fun loadCanvasBitmap(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || width <= 0 || height <= 0) return false
        ensureLayerStack(width, height)
        val layer = activeLayer() ?: return false

        clearBitmap(layer.canvas)
        val dst = fitCenterRect(bitmap.width, bitmap.height, width, height)
        layer.canvas.drawBitmap(bitmap, null, dst, null)
        updateSnapshot(layer.id)
        invalidateSurface()
        return true
    }

    /**
     * Snapshot all layers for structured export (bottom -> top).
     */
    fun snapshotDocumentForExport(): DocumentSnapshot? {
        ensureLayerStack(width, height)
        val w = width
        val h = height
        if (w <= 0 || h <= 0 || layers.isEmpty()) return null
        val activeIndex = layers.indexOfFirst { it.id == activeLayerId }.coerceAtLeast(0)
        val snapshots = layers.map { layer ->
            LayerSnapshot(
                name = layer.name,
                visible = layer.visible,
                opacity = layer.opacity,
                bitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, false),
            )
        }
        return DocumentSnapshot(
            width = w,
            height = h,
            activeLayerIndex = activeIndex,
            layers = snapshots,
        )
    }

    /**
     * Replace the whole file with imported layers (bottom -> top).
     */
    fun replaceFileWithLayers(
        sourceWidth: Int,
        sourceHeight: Int,
        sourceLayers: List<LayerSnapshot>,
        activeLayerIndex: Int,
    ): Boolean {
        val w = width
        val h = height
        if (w <= 0 || h <= 0 || sourceLayers.isEmpty()) return false

        strokePoints.clear()
        rawMovePoints.clear()
        receivedAuthorityList = false
        pendingPenUpRefresh = false
        hasRenderedThisStroke = false
        strokeInProgress = false
        strokeLayerId = -1

        layers.forEach { recycleLayer(it) }
        layers.clear()
        nextLayerId = 1

        val srcW = if (sourceWidth > 0) sourceWidth else sourceLayers.first().bitmap.width
        val srcH = if (sourceHeight > 0) sourceHeight else sourceLayers.first().bitmap.height
        val dst = fitCenterRect(srcW, srcH, w, h)

        sourceLayers.forEachIndexed { index, source ->
            val layer = createLayer(
                id = nextLayerId++,
                name = if (source.name.isBlank()) "Layer ${index + 1}" else source.name,
                w = w,
                h = h,
                visible = source.visible,
                opacity = source.opacity.coerceIn(0f, 1f),
            )
            clearBitmap(layer.canvas)
            layer.canvas.drawBitmap(source.bitmap, null, dst, null)
            clearBitmap(layer.snapshotCanvas)
            layer.snapshotCanvas.drawBitmap(layer.bitmap, 0f, 0f, null)
            layers.add(layer)
        }

        val idx = activeLayerIndex.coerceIn(0, layers.lastIndex)
        activeLayerId = layers[idx].id
        updateSnapshot(activeLayerId)
        invalidateSurface()
        return true
    }

    /**
     * Exports the visible composed result of all layers.
     */
    fun exportBitmap(): Bitmap? {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        drawLayers(canvas)
        return out
    }

    // View lifecycle

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ensureLayerStack(w, h)
        clampViewport()
        notifyViewportChanged()
        ensureTouchHelper()
        reconfigureTouchHelper()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(viewOffsetX, viewOffsetY)
        canvas.scale(viewScale, viewScale)
        drawLayers(canvas)
        canvas.restore()
        runCatching {
            EpdController.invalidate(this, UpdateMode.HAND_WRITING_REPAINT_MODE)
            EpdController.refreshScreen(rootView, UpdateMode.HAND_WRITING_REPAINT_MODE)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (rawInputSuppressed) return false

        // Finger-only stream is reserved for viewport gestures.
        if (!eventHasStylus(event)) {
            handleViewportGesture(event)
            return true
        }

        // Ignore stylus stream while pinch is active.
        if (viewGestureMode == ViewGestureMode.PINCH) return true

        return touchHelper?.onTouchEvent(event) == true || isStylus(event)
    }

    override fun onDetachedFromWindow() {
        val helper = touchHelper
        touchHelper = null
        helperThread.execute {
            runCatching {
                helper?.setRawDrawingEnabled(false)
                helper?.closeRawDrawing()
            }
        }
        helperThread.shutdown()

        layers.forEach { recycleLayer(it) }
        layers.clear()

        super.onDetachedFromWindow()
    }

    // Layer management

    private fun ensureLayerStack(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        if (layers.isEmpty()) {
            val base = createLayer(
                id = nextLayerId++,
                name = "Layer 1",
                w = w,
                h = h,
                visible = true,
                opacity = 1f,
            )
            layers.add(base)
            activeLayerId = base.id
            updateSnapshot(base.id)
            return
        }

        if (layers[0].bitmap.width == w && layers[0].bitmap.height == h) {
            if (activeLayerId < 0) activeLayerId = layers.last().id
            return
        }

        // Size changed: recreate while preserving existing pixels.
        val oldLayers = ArrayList(layers)
        layers.clear()
        for (old in oldLayers) {
            val recreated = createLayer(
                id = old.id,
                name = old.name,
                w = w,
                h = h,
                visible = old.visible,
                opacity = old.opacity,
            )
            val dst = fitCenterRect(old.bitmap.width, old.bitmap.height, w, h)
            recreated.canvas.drawBitmap(old.bitmap, null, dst, null)
            clearBitmap(recreated.snapshotCanvas)
            recreated.snapshotCanvas.drawBitmap(recreated.bitmap, 0f, 0f, null)
            layers.add(recreated)
            recycleLayer(old)
        }
        if (layers.none { it.id == activeLayerId }) {
            activeLayerId = layers.last().id
        }
    }

    private fun createLayer(
        id: Int,
        name: String,
        w: Int,
        h: Int,
        visible: Boolean,
        opacity: Float,
    ): LayerState {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val snap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val snapCanvas = Canvas(snap)
        clearBitmap(canvas)
        clearBitmap(snapCanvas)
        return LayerState(
            id = id,
            name = name,
            visible = visible,
            opacity = opacity,
            bitmap = bmp,
            canvas = canvas,
            snapshotBitmap = snap,
            snapshotCanvas = snapCanvas,
        )
    }

    private fun recycleLayer(layer: LayerState) {
        if (!layer.bitmap.isRecycled) layer.bitmap.recycle()
        if (!layer.snapshotBitmap.isRecycled) layer.snapshotBitmap.recycle()
    }

    private fun layerById(id: Int): LayerState? = layers.firstOrNull { it.id == id }

    private fun activeLayer(): LayerState? = layerById(activeLayerId)

    // TouchHelper management

    private fun ensureTouchHelper() {
        if (touchHelper != null) return
        touchHelper = TouchHelper.create(
            this,
            TouchHelper.FEATURE_ALL_TOUCH_RENDER,
            rawInputCallback,
            false
        )
    }

    /**
     * Initialise / reconfigure the hardware chip.
     * The order here is critical.
     */
    private fun reconfigureTouchHelper() {
        val helper = touchHelper ?: return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val style = activeStyle
        val widthPx = activeWidthPx

        helperThread.execute {
            if (touchHelper !== helper) return@execute
            runCatching {
                // 1. Width
                helper.setStrokeWidth(widthPx)
                helper.enableFingerTouch(false)
                helper.onlyEnableFingerTouch(false)
                // 2. Preview color
                val hardwareColor = when (style) {
                    HardwarePenStyle.MARKER -> withAlpha(activeColor, 128)
                    HardwarePenStyle.CHARCOAL -> withAlpha(activeColor, 255)
                    HardwarePenStyle.CHARCOAL_V2 -> withAlpha(activeColor, 255)
                    else -> withAlpha(activeColor, 255)
                }
                helper.setStrokeColor(hardwareColor)
                // 3. Limits
                helper.setLimitRect(Rect(0, 0, w, h), emptyList())
                // 4. Open (resets chip)
                helper.openRawDrawing()
                // 5. Style after open
                helper.setStrokeStyle(style.hardwareStrokeStyle)
                // Re-apply params after style selection
                helper.setStrokeWidth(widthPx)
                helper.setStrokeColor(hardwareColor)
                // 6. Hardware draws live preview
                helper.setRawDrawingRenderEnabled(false)
                // 7. Enable unless suppressed by UI
                helper.setRawDrawingEnabled(!(rawInputSuppressed || viewportGestureSuppressRaw))
            }
        }
    }

    // Rendering helpers

    private fun maxPressure(): Float {
        val v = runCatching { EpdController.getMaxTouchPressure() }.getOrDefault(0f)
        return when {
            v > 0f -> v
            EpdController.MAX_TOUCH_PRESSURE > 0f -> EpdController.MAX_TOUCH_PRESSURE
            else -> 4096f
        }
    }

    private fun drawLayers(canvas: Canvas) {
        for (layer in layers) {
            if (!layer.visible || layer.opacity <= 0f) continue
            if (layer.opacity >= 0.999f) {
                canvas.drawBitmap(layer.bitmap, 0f, 0f, null)
            } else {
                layerPaint.alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
                canvas.drawBitmap(layer.bitmap, 0f, 0f, layerPaint)
            }
        }
        layerPaint.alpha = 255
    }

    private fun updateSnapshot(layerId: Int) {
        val layer = layerById(layerId) ?: return
        clearBitmap(layer.snapshotCanvas)
        layer.snapshotCanvas.drawBitmap(layer.bitmap, 0f, 0f, null)
    }

    private fun rebuildWithCurrentStroke(layerId: Int) {
        val layer = layerById(layerId) ?: return
        clearBitmap(layer.canvas)
        layer.canvas.drawBitmap(layer.snapshotBitmap, 0f, 0f, null)
    }

    private fun invalidateSurface() {
        invalidate()
    }

    private fun fitCenterRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): RectF {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) {
            return RectF(0f, 0f, dstW.toFloat(), dstH.toFloat())
        }
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = dstW.toFloat() / dstH.toFloat()
        return if (srcAspect > dstAspect) {
            val drawH = dstW / srcAspect
            val top = (dstH - drawH) * 0.5f
            RectF(0f, top, dstW.toFloat(), top + drawH)
        } else {
            val drawW = dstH * srcAspect
            val left = (dstW - drawW) * 0.5f
            RectF(left, 0f, left + drawW, dstH.toFloat())
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun clearBitmap(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun notifyViewportChanged() {
        viewportListener?.invoke(viewScale)
    }

    private fun setViewportGestureRawSuppressed(suppressed: Boolean) {
        if (viewportGestureSuppressRaw == suppressed) return
        viewportGestureSuppressRaw = suppressed
        val helper = touchHelper ?: return
        val shouldEnable = !(rawInputSuppressed || viewportGestureSuppressRaw)
        helperThread.execute {
            if (touchHelper !== helper) return@execute
            runCatching { helper.setRawDrawingEnabled(shouldEnable) }
        }
    }

    // Viewport gestures

    private fun handleViewportGesture(event: MotionEvent): Boolean {
        if (event.pointerCount <= 0) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (viewScale > 1.0001f) {
                    viewGestureMode = ViewGestureMode.PAN
                    setViewportGestureRawSuppressed(true)
                    panLastX = event.getX(0)
                    panLastY = event.getY(0)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                viewGestureMode = ViewGestureMode.NONE
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    beginPinch(event)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (viewGestureMode) {
                    ViewGestureMode.PINCH -> {
                        if (event.pointerCount >= 2) {
                            updatePinch(event)
                            return true
                        }
                    }

                    ViewGestureMode.PAN -> {
                        val x = event.getX(0)
                        val y = event.getY(0)
                        val dx = x - panLastX
                        val dy = y - panLastY
                        panLastX = x
                        panLastY = y
                        viewOffsetX += dx
                        viewOffsetY += dy
                        clampViewport()
                        invalidateSurface()
                        return true
                    }

                    ViewGestureMode.NONE -> Unit
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (viewGestureMode == ViewGestureMode.PINCH) {
                    val remaining = event.pointerCount - 1
                    if (remaining >= 2) beginPinchFromRemainingPointers(event, event.actionIndex)
                    else {
                        viewGestureMode = ViewGestureMode.NONE
                        setViewportGestureRawSuppressed(false)
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (viewGestureMode != ViewGestureMode.NONE) {
                    viewGestureMode = ViewGestureMode.NONE
                    setViewportGestureRawSuppressed(false)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
                if (viewportGestureSuppressRaw) {
                    setViewportGestureRawSuppressed(false)
                }
            }
        }
        return viewGestureMode != ViewGestureMode.NONE
    }

    private fun beginPinch(event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)
        val focusX = (x0 + x1) * 0.5f
        val focusY = (y0 + y1) * 0.5f
        pinchStartDistance = max(1f, distance(x0, y0, x1, y1))
        pinchStartScale = viewScale
        pinchAnchorWorldX = (focusX - viewOffsetX) / pinchStartScale
        pinchAnchorWorldY = (focusY - viewOffsetY) / pinchStartScale
        viewGestureMode = ViewGestureMode.PINCH
        setViewportGestureRawSuppressed(true)
        panLastX = focusX
        panLastY = focusY
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun beginPinchFromRemainingPointers(event: MotionEvent, liftedPointerIndex: Int) {
        if (event.pointerCount < 3) {
            viewGestureMode = ViewGestureMode.NONE
            return
        }
        val first = if (liftedPointerIndex == 0) 1 else 0
        val second = if (liftedPointerIndex <= 1) 2 else 1
        val x0 = event.getX(first)
        val y0 = event.getY(first)
        val x1 = event.getX(second)
        val y1 = event.getY(second)
        val focusX = (x0 + x1) * 0.5f
        val focusY = (y0 + y1) * 0.5f
        pinchStartDistance = max(1f, distance(x0, y0, x1, y1))
        pinchStartScale = viewScale
        pinchAnchorWorldX = (focusX - viewOffsetX) / pinchStartScale
        pinchAnchorWorldY = (focusY - viewOffsetY) / pinchStartScale
        viewGestureMode = ViewGestureMode.PINCH
        panLastX = focusX
        panLastY = focusY
    }

    private fun updatePinch(event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)
        val focusX = (x0 + x1) * 0.5f
        val focusY = (y0 + y1) * 0.5f
        val dist = max(1f, distance(x0, y0, x1, y1))
        val newScale = (pinchStartScale * (dist / pinchStartDistance)).coerceIn(minViewScale, maxViewScale)
        viewScale = newScale
        viewOffsetX = focusX - pinchAnchorWorldX * newScale
        viewOffsetY = focusY - pinchAnchorWorldY * newScale
        clampViewport()
        notifyViewportChanged()
        invalidateSurface()
    }

    private fun clampViewport() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (viewScale <= 1.0001f) {
            viewScale = 1f
            viewOffsetX = 0f
            viewOffsetY = 0f
            return
        }
        val scaledW = w * viewScale
        val scaledH = h * viewScale
        val minX = w - scaledW
        val minY = h - scaledH
        viewOffsetX = viewOffsetX.coerceIn(minX, 0f)
        viewOffsetY = viewOffsetY.coerceIn(minY, 0f)
    }

    private fun distance(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        return sqrt(dx * dx + dy * dy)
    }

    // Point helpers

    private fun mapStrokePoint(pt: TouchPoint?): TouchPoint? {
        pt ?: return null
        val s = strokeViewScale.coerceAtLeast(0.0001f)
        val x = ((pt.x - strokeViewOffsetX) / s).coerceIn(0f, (width - 1).coerceAtLeast(0).toFloat())
        val y = ((pt.y - strokeViewOffsetY) / s).coerceIn(0f, (height - 1).coerceAtLeast(0).toFloat())
        return TouchPoint(pt).also {
            it.x = x
            it.y = y
        }
    }

    private fun mapStrokePoints(points: List<TouchPoint>): List<TouchPoint> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<TouchPoint>(points.size)
        for (p in points) {
            val mapped = mapStrokePoint(p)
            if (mapped != null) out.add(mapped)
        }
        return out
    }

    private fun appendPoint(pt: TouchPoint?) {
        pt ?: return
        val copy = TouchPoint(pt)
        val last = strokePoints.lastOrNull()
        if (last != null &&
            abs(last.x - copy.x) < 0.1f &&
            abs(last.y - copy.y) < 0.1f &&
            abs(last.pressure - copy.pressure) < 0.001f &&
            abs(last.size - copy.size) < 0.001f &&
            last.tiltX == copy.tiltX &&
            last.tiltY == copy.tiltY
        ) return
        strokePoints.add(copy)
    }

    private fun appendRawMovePoint(pt: TouchPoint?) {
        pt ?: return
        val copy = TouchPoint(pt)
        val last = rawMovePoints.lastOrNull()
        if (last != null &&
            abs(last.x - copy.x) < 0.1f &&
            abs(last.y - copy.y) < 0.1f &&
            abs(last.pressure - copy.pressure) < 0.001f &&
            abs(last.size - copy.size) < 0.001f &&
            last.tiltX == copy.tiltX &&
            last.tiltY == copy.tiltY
        ) return
        rawMovePoints.add(copy)
    }

    private data class PointSignal(
        val maxPressure: Float,
        val nonZeroTiltCount: Int,
    )

    private fun signalOf(points: List<TouchPoint>): PointSignal {
        var maxP = 0f
        var tiltNz = 0
        for (p in points) {
            if (p.pressure > maxP) maxP = p.pressure
            if (p.tiltX != 0 || p.tiltY != 0) tiltNz++
        }
        return PointSignal(maxP, tiltNz)
    }

    private fun chooseRenderPoints(style: HardwarePenStyle): List<TouchPoint> {
        if (strokePoints.size < 2) return rawMovePoints
        if (style != HardwarePenStyle.CHARCOAL && style != HardwarePenStyle.CHARCOAL_V2) {
            return strokePoints
        }
        if (rawMovePoints.size < 2) return strokePoints

        val authority = signalOf(strokePoints)
        val raw = signalOf(rawMovePoints)
        val hasRicherTilt = raw.nonZeroTiltCount > authority.nonZeroTiltCount
        val hasRicherPressure = raw.maxPressure > 1.5f && authority.maxPressure <= 1.05f
        return if (hasRicherTilt || hasRicherPressure) rawMovePoints else strokePoints
    }

    /**
     * Reconcile with authoritative list from hardware.
     * If it diverges, replace and rebuild current stroke layer from snapshot.
     */
    private fun mergeAuthorityList(pts: List<TouchPoint>) {
        val common = minOf(strokePoints.size, pts.size)
        var prefixOk = true
        for (i in 0 until common) {
            val a = strokePoints[i]
            val b = pts[i]
            if (abs(a.x - b.x) > 0.35f || abs(a.y - b.y) > 0.35f) {
                prefixOk = false
                break
            }
        }

        if (prefixOk && pts.size >= strokePoints.size) {
            val before = strokePoints.size
            for (i in before until pts.size) appendPoint(pts[i])
        } else {
            strokePoints.clear()
            pts.forEach { appendPoint(it) }
            rebuildWithCurrentStroke(strokeLayerId)
        }
    }

    private fun isStylus(event: MotionEvent): Boolean {
        if (event.pointerCount <= 0) return false
        val idx = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        return event.getToolType(idx) in listOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER)
    }

    private fun eventHasStylus(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            val tool = event.getToolType(i)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) {
                return true
            }
        }
        return false
    }

    // RawInputCallback

    private val rawInputCallback = object : RawInputCallback() {

        override fun onBeginRawDrawing(success: Boolean, pt: TouchPoint?) {
            ensureLayerStack(width, height)

            if (strokeInProgress) {
                // Some firmware versions emit duplicate begin events inside one gesture.
                appendPoint(mapStrokePoint(pt))
                appendRawMovePoint(mapStrokePoint(pt))
                Log.d(TAG, "duplicate onBeginRawDrawing style=$activeStyle")
                return
            }

            strokeInProgress = true
            strokePoints.clear()
            rawMovePoints.clear()
            strokeStyle = activeStyle
            strokeViewScale = viewScale
            strokeViewOffsetX = viewOffsetX
            strokeViewOffsetY = viewOffsetY
            strokeWidthPx = (activeWidthPx / strokeViewScale.coerceAtLeast(1f)).coerceAtLeast(0.5f)
            strokeColor = activeColor
            strokeLayerId = activeLayerId
            receivedAuthorityList = false
            pendingPenUpRefresh = false
            hasRenderedThisStroke = false
            appendPoint(mapStrokePoint(pt))
            appendRawMovePoint(mapStrokePoint(pt))
            Log.d(TAG, "onBeginRawDrawing style=$activeStyle width=$activeWidthPx layer=$strokeLayerId")
        }

        override fun onRawDrawingTouchPointMoveReceived(pt: TouchPoint?) {
            appendPoint(mapStrokePoint(pt))
            appendRawMovePoint(mapStrokePoint(pt))
        }

        override fun onRawDrawingTouchPointListReceived(list: TouchPointList?) {
            val pts = list?.points ?: return
            if (pts.isEmpty()) return
            mergeAuthorityList(mapStrokePoints(pts))
            receivedAuthorityList = true
        }

        override fun onEndRawDrawing(success: Boolean, pt: TouchPoint?) {
            if (!strokeInProgress) return
            strokeInProgress = false

            if (!receivedAuthorityList) {
                appendPoint(mapStrokePoint(pt))
            }
            appendRawMovePoint(mapStrokePoint(pt))

            val renderPts = chooseRenderPoints(strokeStyle)
            val layer = layerById(strokeLayerId)
            if (renderPts.size >= 2 && !hasRenderedThisStroke && layer != null) {
                hasRenderedThisStroke = true
                val copy = ArrayList(renderPts)
                val source = if (renderPts === rawMovePoints) "rawMove" else "authority"
                val sig = signalOf(renderPts)
                Log.d(
                    TAG,
                    "render: style=$strokeStyle layer=$strokeLayerId source=$source pts=${copy.size} maxP=${sig.maxPressure} tiltNz=${sig.nonZeroTiltCount}"
                )
                runCatching {
                    OnyxStrokeRenderer.render(strokeStyle, copy, strokeWidthPx, strokeColor, layer.canvas, maxPressure())
                }.onFailure { e ->
                    Log.e(TAG, "render threw: ${e.javaClass.simpleName}: ${e.message}", e)
                }
                updateSnapshot(strokeLayerId)
            }

            strokePoints.clear()
            rawMovePoints.clear()
            receivedAuthorityList = false
            pendingPenUpRefresh = true

            // Fallback refresh if onPenUpRefresh does not fire in time.
            postDelayed({
                if (pendingPenUpRefresh) {
                    pendingPenUpRefresh = false
                    invalidateSurface()
                }
            }, 120L)
        }

        override fun onPenUpRefresh(rectF: RectF?) {
            if (!pendingPenUpRefresh) return
            pendingPenUpRefresh = false
            invalidateSurface()
        }

        override fun onBeginRawErasing(success: Boolean, pt: TouchPoint?) = Unit
        override fun onEndRawErasing(success: Boolean, pt: TouchPoint?) = Unit
        override fun onRawErasingTouchPointMoveReceived(pt: TouchPoint?) = Unit
        override fun onRawErasingTouchPointListReceived(list: TouchPointList?) = Unit
    }
}
