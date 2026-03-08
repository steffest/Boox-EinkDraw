package com.boox.einkdraw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

/**
 * Full-screen drawing surface backed by the Onyx hardware pen chip.
 *
 * Architecture:
 *  - TouchHelper drives zero-latency hardware preview (setRawDrawingRenderEnabled=false).
 *  - RawInputCallback accumulates points incrementally and renders to an ink bitmap in parallel.
 *  - On pen-up / onPenUpRefresh the hardware preview clears and our bitmap is revealed.
 *  - Every completed stroke is recorded for clear/rebuild.
 */
class HardwarePenSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    companion object {
        private const val TAG = "HardwarePenSurface"
    }

    private data class RecordedStroke(
        val style: HardwarePenStyle,
        val widthPx: Float,
        val points: List<TouchPoint>,
    )

    // ─── Active pen state ─────────────────────────────────────────────────────

    private var activeStyle = HardwarePenStyle.PENCIL
    private var activeWidthPx = HardwarePenStyle.PENCIL.defaultWidthPx
    private var rawInputSuppressed = false

    // ─── Persistent ink bitmap ────────────────────────────────────────────────

    private var inkBitmap: Bitmap? = null
    private var inkCanvas: Canvas? = null

    // ─── Stroke history ───────────────────────────────────────────────────────

    private val completedStrokes = ArrayList<RecordedStroke>(512)

    // ─── In-flight stroke ─────────────────────────────────────────────────────

    private var strokeStyle = HardwarePenStyle.PENCIL
    private var strokeWidthPx = 5f
    private val strokePoints = ArrayList<TouchPoint>(256)
    private var receivedAuthorityList = false
    private var needsFullRebuild = false
    private var pendingPenUpRefresh = false

    // ─── TouchHelper (configured on a background thread) ─────────────────────

    private val helperThread = Executors.newSingleThreadExecutor()
    private var touchHelper: TouchHelper? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setStyle(style: HardwarePenStyle) {
        activeStyle = style
        reconfigureTouchHelper()
    }

    fun setStrokeWidthPx(widthPx: Float) {
        activeWidthPx = widthPx.coerceIn(1f, 200f)
        reconfigureTouchHelper()
    }

    /**
     * Suppress/restore the hardware pen overlay while the user interacts with UI controls.
     * Must be called from the UI thread (e.g. via an onTouchListener on every UI button).
     */
    fun setRawInputSuppressed(suppressed: Boolean) {
        rawInputSuppressed = suppressed
        val helper = touchHelper ?: return
        helperThread.execute {
            runCatching { helper.setRawDrawingEnabled(!suppressed) }
        }
    }

    fun clearCanvas() {
        completedStrokes.clear()
        strokePoints.clear()
        rebuildBitmap()
        invalidateSurface()
    }

    fun exportBitmap(): Bitmap? {
        rebuildBitmap()
        return inkBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    // ─── View lifecycle ───────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        allocateBitmap(w, h)
        ensureTouchHelper()
        reconfigureTouchHelper()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        inkBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        inkBitmap?.recycle()
        inkBitmap = null
        inkCanvas = null
        super.onDetachedFromWindow()
    }

    // ─── Bitmap management ────────────────────────────────────────────────────

    private fun allocateBitmap(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val ex = inkBitmap
        if (ex != null && ex.width == w && ex.height == h) return
        ex?.recycle()
        inkBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            inkCanvas = Canvas(bmp).apply { drawColor(Color.WHITE) }
        }
    }

    // ─── TouchHelper management ───────────────────────────────────────────────

    private fun ensureTouchHelper() {
        if (touchHelper != null) return
        touchHelper = TouchHelper.create(this, TouchHelper.FEATURE_ALL_TOUCH_RENDER, rawInputCallback)
    }

    /**
     * Initialise / reconfigure the hardware chip.
     * The order here is critical — see AGENTS.md §3 for the exact sequence.
     */
    private fun reconfigureTouchHelper() {
        val helper = touchHelper ?: return
        val w = width; val h = height
        if (w <= 0 || h <= 0) return
        val style = activeStyle
        val widthPx = activeWidthPx

        helperThread.execute {
            if (touchHelper !== helper) return@execute
            runCatching {
                // 1. Width
                helper.setStrokeWidth(widthPx)
                // 2. Solid colour — transparency breaks the pencil shader
                helper.setStrokeColor(Color.BLACK)
                // 3. Limit rect
                helper.setLimitRect(Rect(0, 0, w, h), emptyList())
                // 4. Open (resets chip)
                helper.openRawDrawing()
                // 5. Style — MUST come after openRawDrawing or it reverts to FOUNTAIN
                helper.setStrokeStyle(style.hardwareStrokeStyle)
                // 6. Let the hardware chip draw the live preview (zero latency)
                helper.setRawDrawingRenderEnabled(false)
                // 7. Enable (or keep disabled while UI is active)
                helper.setRawDrawingEnabled(!rawInputSuppressed)
            }
        }
    }

    // ─── Rendering helpers ────────────────────────────────────────────────────

    private fun maxPressure(): Float {
        val v = runCatching { EpdController.getMaxTouchPressure() }.getOrDefault(0f)
        return when {
            v > 0f -> v
            EpdController.MAX_TOUCH_PRESSURE > 0f -> EpdController.MAX_TOUCH_PRESSURE
            else -> 4096f
        }
    }

    /** Full bitmap rebuild from the completed stroke list. */
    private fun rebuildBitmap() {
        val c = inkCanvas ?: return
        c.drawColor(Color.WHITE)
        val maxP = maxPressure()
        for (stroke in completedStrokes) {
            OnyxStrokeRenderer.render(stroke.style, stroke.points, stroke.widthPx, c, maxP)
        }
    }

    /** Rebuild including the in-flight stroke (used after an authority list conflict). */
    private fun rebuildWithCurrentStroke() {
        val c = inkCanvas ?: return
        c.drawColor(Color.WHITE)
        val maxP = maxPressure()
        for (stroke in completedStrokes) {
            OnyxStrokeRenderer.render(stroke.style, stroke.points, stroke.widthPx, c, maxP)
        }
        if (strokePoints.isNotEmpty()) {
            OnyxStrokeRenderer.render(strokeStyle, strokePoints, strokeWidthPx, c, maxP)
        }
    }

    /** Render only the new tail of the in-flight stroke since [fromIndex]. */
    private fun renderDeltaFrom(fromIndex: Int) {
        val c = inkCanvas ?: return
        if (strokePoints.isEmpty()) return
        val start = (fromIndex - 1).coerceAtLeast(0)
        val slice = strokePoints.subList(start, strokePoints.size).map { TouchPoint(it) }
        OnyxStrokeRenderer.render(strokeStyle, slice, strokeWidthPx, c, maxPressure())
    }

    private fun invalidateSurface() {
        postInvalidateOnAnimation()
        runCatching {
            EpdController.invalidate(this, UpdateMode.HAND_WRITING_REPAINT_MODE)
            EpdController.refreshScreen(rootView, UpdateMode.HAND_WRITING_REPAINT_MODE)
        }
    }

    // ─── Point helpers ────────────────────────────────────────────────────────

    private fun appendPoint(pt: TouchPoint?) {
        pt ?: return
        val copy = TouchPoint(pt)
        val last = strokePoints.lastOrNull()
        if (last != null &&
            abs(last.x - copy.x) < 0.1f &&
            abs(last.y - copy.y) < 0.1f &&
            abs(last.pressure - copy.pressure) < 0.001f
        ) return
        strokePoints.add(copy)
    }

    /**
     * Reconcile with the authoritative point list delivered by the hardware.
     * If it extends our running list cleanly, append the tail. Otherwise, replace and rebuild.
     */
    private fun mergeAuthorityList(pts: List<TouchPoint>) {
        val common = minOf(strokePoints.size, pts.size)
        var prefixOk = true
        for (i in 0 until common) {
            val a = strokePoints[i]; val b = pts[i]
            if (abs(a.x - b.x) > 0.35f || abs(a.y - b.y) > 0.35f) { prefixOk = false; break }
        }

        if (prefixOk && pts.size >= strokePoints.size) {
            val before = strokePoints.size
            for (i in before until pts.size) appendPoint(pts[i])
            if (!needsFullRebuild && strokePoints.size > before) renderDeltaFrom(before)
        } else {
            strokePoints.clear()
            pts.forEach { appendPoint(it) }
            needsFullRebuild = true
            rebuildWithCurrentStroke()
        }
        postInvalidateOnAnimation()
    }

    private fun isStylus(event: MotionEvent): Boolean {
        val idx = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        return event.getToolType(idx) in listOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER)
    }

    // ─── RawInputCallback ─────────────────────────────────────────────────────

    private val rawInputCallback = object : RawInputCallback() {

        override fun onBeginRawDrawing(success: Boolean, pt: TouchPoint?) {
            Log.d(TAG, "onBeginRawDrawing style=$activeStyle width=$activeWidthPx")
            strokePoints.clear()
            strokeStyle = activeStyle
            strokeWidthPx = activeWidthPx
            needsFullRebuild = false
            receivedAuthorityList = false
            pendingPenUpRefresh = false
            appendPoint(pt)
            if (strokePoints.isNotEmpty()) renderDeltaFrom(0)
            postInvalidateOnAnimation()
        }

        override fun onRawDrawingTouchPointMoveReceived(pt: TouchPoint?) {
            val before = strokePoints.size
            appendPoint(pt)
            if (!needsFullRebuild && strokePoints.size > before) {
                renderDeltaFrom(before)
                postInvalidateOnAnimation()
            }
        }

        override fun onRawDrawingTouchPointListReceived(list: TouchPointList?) {
            val pts = list?.points ?: return
            if (pts.isEmpty()) return
            Log.d(TAG, "authorityList size=${pts.size}")
            mergeAuthorityList(pts)
            receivedAuthorityList = true
        }

        override fun onEndRawDrawing(success: Boolean, pt: TouchPoint?) {
            Log.d(TAG, "onEndRawDrawing points=${strokePoints.size} gotList=$receivedAuthorityList")
            if (!receivedAuthorityList) {
                appendPoint(pt)
                if (strokePoints.isNotEmpty()) renderDeltaFrom((strokePoints.size - 1).coerceAtLeast(0))
            }
            if (strokePoints.isNotEmpty()) {
                completedStrokes.add(RecordedStroke(strokeStyle, strokeWidthPx, ArrayList(strokePoints)))
            }
            strokePoints.clear()
            receivedAuthorityList = false
            needsFullRebuild = false
            pendingPenUpRefresh = true
            // Fallback refresh if onPenUpRefresh doesn't fire within ~120 ms
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
