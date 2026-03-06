package com.boox.einkdraw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val CANVAS_WIDTH = 930
        const val CANVAS_HEIGHT = 1240
    }

    private val drawBitmap: Bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.RGB_565)
    private val drawCanvas = Canvas(drawBitmap)

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = false
        isDither = false
    }

    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = false
        isDither = false
    }

    private var displayScale = 1f
    private var baseOffsetX = 0f
    private var baseOffsetY = 0f
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var lastPanFocusX = 0f
    private var lastPanFocusY = 0f
    private val displayRect = RectF()
    private var viewW = 0
    private var viewH = 0

    private var lastCanvasX = 0f
    private var lastCanvasY = 0f
    private var lastPressure = 1f
    private var touching = false
    private var lastAcceptedEventMs = 0L
    private var panning = false
    private var rawTouching = false
    private var rawLastCanvasX = 0f
    private var rawLastCanvasY = 0f
    private var rawLastPressure = 1f
    private var rawLastAcceptedEventMs = 0L
    private var rapidTouchHelper: TouchHelper? = null
    private var rapidInputSuppressed = false

    var drawColorArgb: Int = Color.BLACK
        set(value) {
            field = Color.rgb(Color.red(value), Color.green(value), Color.blue(value))
            applyRapidBrushSettings()
        }
    var minStrokePx = 1.4f
    var maxStrokePx = 5.0f
    var minEventIntervalMs = 4L
    var minSegmentDistancePx = 0.9f
    var minZoom = 1f
    var maxZoom = 4f
    var drawingEnabled = true
    var viewportGesturesEnabled = true
    var onViewportChanged: ((Float, Float, Float) -> Unit)? = null
    private var brushSizeMultiplier = 1f
    private var brushOpacityMultiplier = 1f

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldZoom = zoom
            val newZoom = (oldZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
            if (newZoom == oldZoom) return false

            val ratio = newZoom / oldZoom
            val focusX = detector.focusX
            val focusY = detector.focusY
            panX = (focusX - baseOffsetX) - ((focusX - baseOffsetX - panX) * ratio)
            panY = (focusY - baseOffsetY) - ((focusY - baseOffsetY - panY) * ratio)
            zoom = newZoom
            clampPan()
            updateDisplayRect()
            notifyViewportChanged()
            invalidate()
            return true
        }
    })

    init {
        setBackgroundColor(Color.WHITE)
        clear()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(drawBitmap, null, displayRect, bitmapPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (event.pointerCount >= 2) {
            if (!viewportGesturesEnabled) {
                touching = false
                panning = false
                return true
            }
            handleTwoFingerPan(event)
            touching = false
            return true
        }

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touching = false
            panning = false
            return true
        }

        if (!drawingEnabled) {
            return true
        }

        val x = toCanvasX(event.x)
        val y = toCanvasY(event.y)
        val p = normalizedPressure(event.pressure)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                lastCanvasX = x
                lastCanvasY = y
                lastPressure = p
                lastAcceptedEventMs = event.eventTime
                drawPoint(x, y, p)
                invalidateCanvasArea(x, y, x, y, maxStrokePx)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!touching) {
                    touching = true
                    lastCanvasX = x
                    lastCanvasY = y
                    lastPressure = p
                    lastAcceptedEventMs = event.eventTime
                }

                val historySize = event.historySize
                for (i in 0 until historySize) {
                    val tMs = event.getHistoricalEventTime(i)
                    val hx = toCanvasX(event.getHistoricalX(i))
                    val hy = toCanvasY(event.getHistoricalY(i))
                    val hp = normalizedPressure(event.getHistoricalPressure(i))
                    if (shouldAcceptSample(hx, hy, tMs)) {
                        drawSegment(lastCanvasX, lastCanvasY, lastPressure, hx, hy, hp)
                        lastCanvasX = hx
                        lastCanvasY = hy
                        lastPressure = hp
                        lastAcceptedEventMs = tMs
                    }
                }

                if (shouldAcceptSample(x, y, event.eventTime)) {
                    drawSegment(lastCanvasX, lastCanvasY, lastPressure, x, y, p)
                    lastCanvasX = x
                    lastCanvasY = y
                    lastPressure = p
                    lastAcceptedEventMs = event.eventTime
                }
            }
        }
        return true
    }

    fun clear() {
        drawCanvas.drawColor(Color.WHITE)
        invalidate()
    }

    fun loadBitmap(source: Bitmap) {
        val pixels = IntArray(CANVAS_WIDTH * CANVAS_HEIGHT)
        source.getPixels(pixels, 0, CANVAS_WIDTH, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val luminance = ((0.2126f * r) + (0.7152f * g) + (0.0722f * b)).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(255, luminance, luminance, luminance)
        }
        drawBitmap.setPixels(pixels, 0, CANVAS_WIDTH, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
        invalidate()
    }

    fun getBitmapCopy(): Bitmap {
        return drawBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun getViewport(): Triple<Float, Float, Float> = Triple(zoom, panX, panY)

    fun resetView() {
        zoom = 1f
        panX = 0f
        panY = 0f
        clampPan()
        updateDisplayRect()
        notifyViewportChanged()
        invalidate()
    }

    fun ingestRawPoint(viewX: Float, viewY: Float, rawPressure: Float, action: Int, eventTimeMs: Long) {
        val x = toCanvasX(viewX)
        val y = toCanvasY(viewY)
        val p = normalizedPressure(rawPressure)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                rawTouching = true
                rawLastCanvasX = x
                rawLastCanvasY = y
                rawLastPressure = p
                rawLastAcceptedEventMs = eventTimeMs
                drawPoint(x, y, p)
                invalidateCanvasArea(x, y, x, y, maxStrokePx * brushSizeMultiplier)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!rawTouching) {
                    rawTouching = true
                    rawLastCanvasX = x
                    rawLastCanvasY = y
                    rawLastPressure = p
                    rawLastAcceptedEventMs = eventTimeMs
                    return
                }
                if (shouldAcceptRawSample(x, y, eventTimeMs)) {
                    drawSegment(rawLastCanvasX, rawLastCanvasY, rawLastPressure, x, y, p)
                    rawLastCanvasX = x
                    rawLastCanvasY = y
                    rawLastPressure = p
                    rawLastAcceptedEventMs = eventTimeMs
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (rawTouching) {
                    if (shouldAcceptRawSample(x, y, eventTimeMs)) {
                        drawSegment(rawLastCanvasX, rawLastCanvasY, rawLastPressure, x, y, p)
                    }
                }
                rawTouching = false
            }
        }
    }

    fun setBrushSizeFromPercent(percent: Int) {
        val t = (percent.coerceIn(0, 100) / 100f)
        // Stronger response: roughly 4x larger range than before.
        brushSizeMultiplier = lerp(0.05f, 10.4f, t)
        applyRapidBrushSettings()
    }

    fun setBrushOpacityFromPercent(percent: Int) {
        val t = (percent.coerceIn(0, 100) / 100f)
        // Non-linear mapping gives much stronger visible effect at mid slider.
        brushOpacityMultiplier = t.toDouble().pow(3.0).toFloat()
        applyRapidBrushSettings()
    }

    fun setRapidModeEnabled(enabled: Boolean): Boolean {
        if (!enabled) {
            disableRapidMode()
            return true
        }
        if (rapidTouchHelper != null) {
            configureRapidTouchHelper()
            return true
        }
        val helper = runCatching { TouchHelper.create(this, 2, rapidRawCallback) }.getOrNull()
            ?: return false
        rapidTouchHelper = helper
        configureRapidTouchHelper()
        return true
    }

    fun disableRapidMode() {
        rapidInputSuppressed = false
        rapidTouchHelper?.closeRawDrawing()
        rapidTouchHelper = null
    }

    fun setRapidInputSuppressed(suppressed: Boolean) {
        rapidInputSuppressed = suppressed
        val helper = rapidTouchHelper ?: return
        helper.setRawDrawingEnabled(!suppressed)
        if (suppressed) {
            helper.setRawDrawingRenderEnabled(false)
        } else {
            helper.setRawDrawingRenderEnabled(true)
        }
    }

    private fun drawSegment(x0: Float, y0: Float, p0: Float, x1: Float, y1: Float, p1: Float) {
        val avgPressure = ((p0 + p1) * 0.5f).coerceIn(0f, 1f)
        strokePaint.strokeWidth = lerp(minStrokePx, maxStrokePx, avgPressure) * brushSizeMultiplier
        strokePaint.color = drawColorArgb
        strokePaint.alpha = (pressureToAlpha(avgPressure) * brushOpacityMultiplier).toInt().coerceIn(0, 255)
        drawCanvas.drawLine(x0, y0, x1, y1, strokePaint)
        invalidateCanvasArea(x0, y0, x1, y1, strokePaint.strokeWidth)
    }

    private fun drawPoint(x: Float, y: Float, pressure: Float) {
        strokePaint.strokeWidth = lerp(minStrokePx, maxStrokePx, pressure) * brushSizeMultiplier
        strokePaint.color = drawColorArgb
        strokePaint.alpha = (pressureToAlpha(pressure) * brushOpacityMultiplier).toInt().coerceIn(0, 255)
        drawCanvas.drawPoint(x, y, strokePaint)
    }

    private fun shouldAcceptSample(x: Float, y: Float, eventTimeMs: Long): Boolean {
        val dt = eventTimeMs - lastAcceptedEventMs
        if (dt >= minEventIntervalMs) return true
        val dx = x - lastCanvasX
        val dy = y - lastCanvasY
        return (dx * dx + dy * dy) >= (minSegmentDistancePx * minSegmentDistancePx)
    }

    private fun shouldAcceptRawSample(x: Float, y: Float, eventTimeMs: Long): Boolean {
        val dt = eventTimeMs - rawLastAcceptedEventMs
        if (dt >= minEventIntervalMs) return true
        val dx = x - rawLastCanvasX
        val dy = y - rawLastCanvasY
        return (dx * dx + dy * dy) >= (minSegmentDistancePx * minSegmentDistancePx)
    }

    private fun invalidateCanvasArea(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float) {
        val minCanvasX = min(x0, x1) - radius - 2f
        val maxCanvasX = max(x0, x1) + radius + 2f
        val minCanvasY = min(y0, y1) - radius - 2f
        val maxCanvasY = max(y0, y1) + radius + 2f

        val totalScale = displayScale * zoom
        val left = (baseOffsetX + panX + minCanvasX * totalScale).toInt()
        val top = (baseOffsetY + panY + minCanvasY * totalScale).toInt()
        val right = (baseOffsetX + panX + maxCanvasX * totalScale).toInt()
        val bottom = (baseOffsetY + panY + maxCanvasY * totalScale).toInt()
        invalidate(left, top, right, bottom)
    }

    private fun toCanvasX(viewX: Float): Float {
        val totalScale = displayScale * zoom
        val normalized = ((viewX - baseOffsetX - panX) / totalScale)
        return normalized.coerceIn(0f, (CANVAS_WIDTH - 1).toFloat())
    }

    private fun toCanvasY(viewY: Float): Float {
        val totalScale = displayScale * zoom
        val normalized = ((viewY - baseOffsetY - panY) / totalScale)
        return normalized.coerceIn(0f, (CANVAS_HEIGHT - 1).toFloat())
    }

    private fun normalizedPressure(rawPressure: Float): Float {
        val clamped = rawPressure.coerceIn(0.05f, 1f)
        // Slightly emphasize low/mid pressure so shading feels more responsive.
        return clamped.toDouble().pow(0.72).toFloat().coerceIn(0.05f, 1f)
    }

    private fun pressureToAlpha(pressure: Float): Int {
        return lerp(24f, 255f, pressure.coerceIn(0f, 1f)).toInt().coerceIn(24, 255)
    }

    private fun handleTwoFingerPan(event: MotionEvent) {
        val focusX = averageX(event)
        val focusY = averageY(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                panning = true
                lastPanFocusX = focusX
                lastPanFocusY = focusY
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    if (!panning) {
                        panning = true
                        lastPanFocusX = focusX
                        lastPanFocusY = focusY
                    }
                    val dx = focusX - lastPanFocusX
                    val dy = focusY - lastPanFocusY
                    panX += dx
                    panY += dy
                    clampPan()
                    updateDisplayRect()
                    notifyViewportChanged()
                    invalidate()
                    lastPanFocusX = focusX
                    lastPanFocusY = focusY
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                panning = false
            }
        }
    }

    private fun averageX(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) sum += event.getX(i)
        return sum / event.pointerCount.toFloat()
    }

    private fun averageY(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) sum += event.getY(i)
        return sum / event.pointerCount.toFloat()
    }

    private fun clampPan() {
        if (viewW <= 0 || viewH <= 0) return
        val scaledW = CANVAS_WIDTH * displayScale * zoom
        val scaledH = CANVAS_HEIGHT * displayScale * zoom
        val maxPanX = max(0f, (scaledW - viewW) * 0.5f)
        val maxPanY = max(0f, (scaledH - viewH) * 0.5f)
        panX = panX.coerceIn(-maxPanX, maxPanX)
        panY = panY.coerceIn(-maxPanY, maxPanY)
    }

    private fun updateDisplayRect() {
        val totalScale = displayScale * zoom
        val scaledW = CANVAS_WIDTH * totalScale
        val scaledH = CANVAS_HEIGHT * totalScale
        val left = baseOffsetX + panX
        val top = baseOffsetY + panY
        displayRect.set(left, top, left + scaledW, top + scaledH)
    }

    private fun notifyViewportChanged() {
        onViewportChanged?.invoke(zoom, panX, panY)
    }

    private fun configureRapidTouchHelper() {
        val helper = rapidTouchHelper ?: return
        if (width <= 0 || height <= 0) return
        val bounds = Rect(0, 0, width, height)
        helper.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)
        helper.openRawDrawing()
        helper.setRawDrawingRenderEnabled(!rapidInputSuppressed)
        helper.setRawDrawingEnabled(!rapidInputSuppressed)
        helper.setBrushRawDrawingEnabled(true)
        helper.setEraserRawDrawingEnabled(false)
        helper.setFilterRepeatMovePoint(false)
        helper.setPenUpRefreshTimeMs(1000)
        helper.setLimitRect(bounds, listOf())
        helper.setRawInputReaderEnable(!helper.isRawDrawingInputEnabled)
        applyRapidBrushSettings()
    }

    private val rapidRawCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onEndRawDrawing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {}

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList?) {}
        override fun onBeginRawErasing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onEndRawErasing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {}
        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {}

        override fun onPenActive(point: TouchPoint?) {
            rapidTouchHelper?.setRawDrawingEnabled(!rapidInputSuppressed)
        }

        override fun onPenUpRefresh(refreshRect: RectF?) {
            rapidTouchHelper?.isRawDrawingRenderEnabled = !rapidInputSuppressed
            super.onPenUpRefresh(refreshRect)
        }
    }

    private fun applyRapidBrushSettings() {
        val helper = rapidTouchHelper ?: return
        val width = (1.8f * brushSizeMultiplier).coerceIn(0.15f, 32f)
        val rapidColor = blendWithWhite(drawColorArgb, brushOpacityMultiplier.coerceIn(0f, 1f))
        helper.setStrokeWidth(width)
        helper.setStrokeColor(rapidColor)
    }

    private fun blendWithWhite(color: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        val r = lerp(255f, Color.red(color).toFloat(), t).toInt().coerceIn(0, 255)
        val g = lerp(255f, Color.green(color).toFloat(), t).toInt().coerceIn(0, 255)
        val b = lerp(255f, Color.blue(color).toFloat(), t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewW = w
        viewH = h
        val sx = w.toFloat() / CANVAS_WIDTH.toFloat()
        val sy = h.toFloat() / CANVAS_HEIGHT.toFloat()
        displayScale = min(sx, sy)

        val scaledW = CANVAS_WIDTH * displayScale
        val scaledH = CANVAS_HEIGHT * displayScale
        baseOffsetX = (w - scaledW) * 0.5f
        baseOffsetY = (h - scaledH) * 0.5f
        clampPan()
        updateDisplayRect()
        notifyViewportChanged()
        configureRapidTouchHelper()
    }

    override fun onDetachedFromWindow() {
        disableRapidMode()
        super.onDetachedFromWindow()
    }
}
