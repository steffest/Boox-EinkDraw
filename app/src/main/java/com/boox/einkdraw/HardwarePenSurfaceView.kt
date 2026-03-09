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
        val color: Int,
        val points: List<TouchPoint>,
    )

    // ─── Active pen state ─────────────────────────────────────────────────────

    private var activeStyle = HardwarePenStyle.PENCIL
    private var activeWidthPx = HardwarePenStyle.PENCIL.defaultWidthPx
    private var activeColor = Color.BLACK
    private var rawInputSuppressed = false

    // ─── Persistent ink bitmap ────────────────────────────────────────────────

    private var inkBitmap: Bitmap? = null
    private var inkCanvas: Canvas? = null

    // ─── Snapshot of completed strokes ────────────────────────────────────────
    // Updated after every completed stroke so rebuildWithCurrentStroke can
    // restore the canvas with a fast blit instead of re-running native renderers.

    private var snapshotBitmap: Bitmap? = null
    private var snapshotCanvas: Canvas? = null

    // ─── Stroke history ───────────────────────────────────────────────────────

    private val completedStrokes = ArrayList<RecordedStroke>(512)

    // ─── In-flight stroke ─────────────────────────────────────────────────────

    private var strokeStyle = HardwarePenStyle.PENCIL
    private var strokeWidthPx = 5f
    private var strokeColor = Color.BLACK
    private val strokePoints = ArrayList<TouchPoint>(256)
    private val rawMovePoints = ArrayList<TouchPoint>(256)
    private var receivedAuthorityList = false
    private var needsFullRebuild = false
    private var pendingPenUpRefresh = false
    private var hasRenderedThisStroke = false
    private var strokeInProgress = false

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

    fun setStrokeColor(color: Int) {
        activeColor = color
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
        inkCanvas?.drawColor(Color.WHITE)
        snapshotCanvas?.drawColor(Color.WHITE)
        invalidateSurface()
    }

    fun loadCanvasBitmap(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || width <= 0 || height <= 0) return false
        allocateBitmap(width, height)
        val canvas = inkCanvas ?: return false
        canvas.drawColor(Color.WHITE)
        val dst = fitCenterRect(bitmap.width, bitmap.height, width, height)
        canvas.drawBitmap(bitmap, null, dst, null)
        completedStrokes.clear()
        strokePoints.clear()
        updateSnapshot()
        invalidateSurface()
        return true
    }

    fun exportBitmap(): Bitmap? =
        inkBitmap?.copy(Bitmap.Config.ARGB_8888, false)

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
        // Refresh e-ink hardware AFTER the bitmap has been drawn to the canvas,
        // so the controller reads the updated framebuffer (not the previous one).
        runCatching {
            EpdController.invalidate(this, UpdateMode.HAND_WRITING_REPAINT_MODE)
            EpdController.refreshScreen(rootView, UpdateMode.HAND_WRITING_REPAINT_MODE)
        }
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
        snapshotBitmap?.recycle()
        snapshotBitmap = null
        snapshotCanvas = null
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
        snapshotBitmap?.recycle()
        snapshotBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            snapshotCanvas = Canvas(bmp).apply { drawColor(Color.WHITE) }
        }
    }

    /** Save the current ink canvas state as the authoritative snapshot of completed strokes. */
    private fun updateSnapshot() {
        val src = inkBitmap ?: return
        val sc  = snapshotCanvas ?: return
        sc.drawBitmap(src, 0f, 0f, null)
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
                // 2. Hardware Preview Color — Apply correct alpha for translucent/textured brushes
                val hardwareColor = when (style) {
                    HardwarePenStyle.MARKER -> withAlpha(activeColor, 128)
                    // Charcoal hardware preview should keep full RGB; translucent ARGB
                    // can be coerced to black by the pen chip for these styles.
                    HardwarePenStyle.CHARCOAL -> withAlpha(activeColor, 255)
                    HardwarePenStyle.CHARCOAL_V2 -> withAlpha(activeColor, 255)
                    else -> withAlpha(activeColor, 255)
                }
                helper.setStrokeColor(hardwareColor)
                // 3. Limit rect
                helper.setLimitRect(Rect(0, 0, w, h), emptyList())
                // 4. Open (resets chip)
                helper.openRawDrawing()
                // 5. Style — MUST come after openRawDrawing or it reverts to FOUNTAIN
                helper.setStrokeStyle(style.hardwareStrokeStyle)
                // Some styles (notably Charcoal) reset chip-side color/width defaults when style changes.
                // Re-apply active pen params after selecting style.
                helper.setStrokeWidth(widthPx)
                helper.setStrokeColor(hardwareColor)
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

    /**
     * Restore the ink canvas to the last-known-good snapshot (completed strokes only).
     * This is a fast blit — no native rendering calls — safe to invoke from authority-list
     * callbacks that may fire multiple times per gesture.
     */
    private fun rebuildWithCurrentStroke() {
        val c = inkCanvas ?: return
        val snap = snapshotBitmap
        // Always clear first so the blit is a clean replace, not a composite
        c.drawColor(Color.WHITE)
        if (snap != null && !snap.isRecycled) {
            c.drawBitmap(snap, 0f, 0f, null)
        }
    }

    private fun invalidateSurface() {
        // Trigger onDraw; the EpdController refresh happens inside onDraw after the
        // bitmap has been composited, ensuring the hardware sees the updated framebuffer.
        invalidate()
    }

    private fun fitCenterRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): RectF {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return RectF(0f, 0f, dstW.toFloat(), dstH.toFloat())
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

    // ─── Point helpers ────────────────────────────────────────────────────────

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
        } else {
            strokePoints.clear()
            pts.forEach { appendPoint(it) }
            needsFullRebuild = true
            rebuildWithCurrentStroke()
        }
    }

    private fun isStylus(event: MotionEvent): Boolean {
        val idx = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        return event.getToolType(idx) in listOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER)
    }

    // ─── RawInputCallback ─────────────────────────────────────────────────────

    private val rawInputCallback = object : RawInputCallback() {

        override fun onBeginRawDrawing(success: Boolean, pt: TouchPoint?) {
            if (strokeInProgress) {
                // Some firmware versions emit duplicate begin events within one gesture.
                // Preserve accumulated points instead of resetting the stroke state.
                appendPoint(pt)
                appendRawMovePoint(pt)
                Log.d(TAG, "duplicate onBeginRawDrawing style=$activeStyle")
                return
            }
            strokeInProgress = true
            Log.d(TAG, "onBeginRawDrawing style=$activeStyle width=$activeWidthPx")
            strokePoints.clear()
            rawMovePoints.clear()
            strokeStyle = activeStyle
            strokeWidthPx = activeWidthPx
            strokeColor = activeColor
            needsFullRebuild = false
            receivedAuthorityList = false
            pendingPenUpRefresh = false
            hasRenderedThisStroke = false
            appendPoint(pt)
            appendRawMovePoint(pt)
        }

        override fun onRawDrawingTouchPointMoveReceived(pt: TouchPoint?) {
            appendPoint(pt)
            appendRawMovePoint(pt)
        }

        override fun onRawDrawingTouchPointListReceived(list: TouchPointList?) {
            val pts = list?.points ?: return
            if (pts.isEmpty()) return
            Log.d(TAG, "authorityList size=${pts.size}")
            mergeAuthorityList(pts)
            receivedAuthorityList = true
        }

        override fun onEndRawDrawing(success: Boolean, pt: TouchPoint?) {
            if (!strokeInProgress) return
            strokeInProgress = false
            Log.d(TAG, "onEndRawDrawing points=${strokePoints.size} gotList=$receivedAuthorityList")
            if (!receivedAuthorityList) {
                appendPoint(pt)
            }
            appendRawMovePoint(pt)
            val renderPts = chooseRenderPoints(strokeStyle)
            if (renderPts.size >= 2 && !hasRenderedThisStroke) {
                hasRenderedThisStroke = true
                val copy = ArrayList(renderPts)
                completedStrokes.add(RecordedStroke(strokeStyle, strokeWidthPx, strokeColor, copy))
                val source = if (renderPts === rawMovePoints) "rawMove" else "authority"
                val sig = signalOf(renderPts)
                Log.d(TAG, "render: style=$strokeStyle source=$source inkCanvas=${inkCanvas != null} pts=${copy.size} maxP=${sig.maxPressure} tiltNz=${sig.nonZeroTiltCount}")
                runCatching {
                    inkCanvas?.let {
                        OnyxStrokeRenderer.render(strokeStyle, copy, strokeWidthPx, strokeColor, it, maxPressure())
                    }
                }.onFailure { e ->
                    Log.e(TAG, "render threw: ${e.javaClass.simpleName}: ${e.message}", e)
                }
                updateSnapshot()
            }
            strokePoints.clear()
            rawMovePoints.clear()
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
