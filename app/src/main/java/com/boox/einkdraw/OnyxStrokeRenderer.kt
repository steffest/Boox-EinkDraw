package com.boox.einkdraw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.NeoBrushPenWrapper
import com.onyx.android.sdk.pen.NeoCharcoalPenV2Wrapper
import com.onyx.android.sdk.pen.NeoCharcoalPenWrapper
import com.onyx.android.sdk.pen.NeoFountainPenWrapper
import com.onyx.android.sdk.pen.NeoMarkerPenWrapper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Software-canvas renderer that faithfully replicates each Onyx hardware stroke style.
 *
 * Strategy per style:
 *  PENCIL     – deterministic stipple dots scattered by position hash (grainy texture)
 *  FOUNTAIN   – NeoFountainPenWrapper (pressure-sensitive calligraphy outline)
 *  MARKER     – NeoMarkerPenWrapper on an off-screen layer composited at 50% alpha
 *  NEO_BRUSH  – NeoBrushPenWrapper (variable-width ink brush)
 *  CHARCOAL   – ring + dot cloud texture (V1 params)
 *  DASH       – DashPathEffect line
 *  CHARCOAL_V2– ring + dot cloud texture (V2 params, slightly larger halos)
 *  SQUARE_PEN – angle-keyed variable-width stroke (calligraphy chisel nib)
 */
object OnyxStrokeRenderer {

    fun render(
        style: HardwarePenStyle,
        points: List<TouchPoint>,
        widthPx: Float,
        canvas: Canvas,
        maxPressure: Float,
    ) {
        if (points.isEmpty()) return
        when (style) {
            HardwarePenStyle.PENCIL -> renderPencil(points, widthPx, canvas)
            HardwarePenStyle.FOUNTAIN -> renderFountain(points, widthPx, canvas)
            HardwarePenStyle.MARKER -> renderMarker(points, widthPx, canvas, maxPressure)
            HardwarePenStyle.NEO_BRUSH -> renderNeoBrush(points, widthPx, canvas)
            HardwarePenStyle.CHARCOAL -> renderCharcoal(points, widthPx, canvas, v2 = false)
            HardwarePenStyle.DASH -> renderDash(points, widthPx, canvas)
            HardwarePenStyle.CHARCOAL_V2 -> renderCharcoal(points, widthPx, canvas, v2 = true)
            HardwarePenStyle.SQUARE_PEN -> renderSquarePen(points, widthPx, canvas)
        }
    }

    // ─── PENCIL: deterministic stipple dots ───────────────────────────────────

    private fun renderPencil(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.BLACK
            alpha = 255
            strokeWidth = 1f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = false
        }
        val stats = signalStats(points)
        if (points.size == 1) {
            val p = points[0]
            val sig = normalizedSignal(p, stats)
            stippleDot(p.x, p.y, pressureToRadius(widthPx, sig, HardwarePenStyle.PENCIL), sig, 0, canvas, paint)
            return
        }
        var prev = points[0]
        for (i in 1 until points.size) {
            val curr = points[i]
            val ps = normalizedSignal(prev, stats)
            val cs = normalizedSignal(curr, stats)
            stippleSegment(
                prev, curr,
                pressureToRadius(widthPx, ps, HardwarePenStyle.PENCIL),
                pressureToRadius(widthPx, cs, HardwarePenStyle.PENCIL),
                ps, cs, i, canvas, paint
            )
            prev = curr
        }
    }

    private fun stippleSegment(
        start: TouchPoint, end: TouchPoint,
        startR: Float, endR: Float,
        startSig: Float, endSig: Float,
        seed: Int, canvas: Canvas, paint: Paint,
    ) {
        val dx = end.x - start.x; val dy = end.y - start.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.001f) { stippleDot(start.x, start.y, startR, startSig, seed, canvas, paint); return }
        val steps = max(1, ceil(dist / 1.8f).toInt())
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            stippleDot(
                start.x + dx * t, start.y + dy * t,
                lerp(startR, endR, t), lerp(startSig, endSig, t),
                seed * 4096 + s, canvas, paint
            )
        }
    }

    private fun stippleDot(cx: Float, cy: Float, radius: Float, signal: Float, seed: Int, canvas: Canvas, paint: Paint) {
        val density = lerp(0.008f, 0.048f, signal)
        val count = max(1, (radius * radius * density * PI.toFloat()).toInt())
        var drawn = 0; var attempt = 0
        while (drawn < count && attempt < count * 3) {
            val rx = hashUnit(seed, attempt * 2) * 2f - 1f
            val ry = hashUnit(seed, attempt * 2 + 1) * 2f - 1f
            if (rx * rx + ry * ry <= 1f) { canvas.drawPoint(cx + rx * radius, cy + ry * radius, paint); drawn++ }
            attempt++
        }
    }

    // ─── FOUNTAIN: NeoFountainPenWrapper ──────────────────────────────────────

    private fun renderFountain(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        val paint = solidPaint()
        val min = widthPx * 0.18f
        val max = widthPx
        val pts = points.toArrayList()
        try {
            val result = if (NeoFountainPenWrapper.hasPressure(pts)) {
                NeoFountainPenWrapper.computeStrokePoints(pts, min, max, 1f)
            } else {
                NeoFountainPenWrapper.computeStrokePoints(pts, min, max)
            }
            NeoFountainPenWrapper.drawStroke(canvas, paint, result, min, max, 1f, false)
        } catch (_: Throwable) {
            fallbackPressureStroke(points, widthPx, canvas)
        }
    }

    // ─── MARKER: NeoMarkerPenWrapper, 50 % alpha offscreen composite ──────────

    private fun renderMarker(points: List<TouchPoint>, widthPx: Float, canvas: Canvas, maxPressure: Float) {
        if (points.size < 2) {
            val paint = solidPaint().apply { alpha = 128; style = Paint.Style.FILL }
            points.firstOrNull()?.let { canvas.drawCircle(it.x, it.y, widthPx / 2f, paint) }
            return
        }
        val bounds = pointBounds(points, widthPx)
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val layer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layer)
        val paint = solidPaint().apply { strokeWidth = widthPx }
        val translated = points.map { p -> TouchPoint(p).also { it.x -= bounds.left; it.y -= bounds.top } }

        try {
            val result = NeoMarkerPenWrapper.computeStrokePoints(translated.toArrayList(), widthPx, maxPressure)
            NeoMarkerPenWrapper.drawStroke(layerCanvas, paint, result, widthPx, false)
        } catch (_: Throwable) {
            fallbackPressureStroke(translated, widthPx, layerCanvas)
        }

        canvas.drawBitmap(layer, bounds.left.toFloat(), bounds.top.toFloat(), Paint().apply { alpha = 128 })
        layer.recycle()
    }

    // ─── NEO_BRUSH: NeoBrushPenWrapper ───────────────────────────────────────

    private fun renderNeoBrush(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        val paint = solidPaint()
        val min = widthPx * 0.06f
        val max = widthPx * 2.2f
        val pts = points.toArrayList()
        try {
            val result = NeoBrushPenWrapper.computeStrokePoints(pts, min, max)
            NeoBrushPenWrapper.drawStroke(canvas, paint, result, min, max, false)
        } catch (_: Throwable) {
            fallbackPressureStroke(points, widthPx, canvas)
        }
    }

    // ─── CHARCOAL / CHARCOAL_V2: ring + dot cloud texture ────────────────────

    private fun renderCharcoal(points: List<TouchPoint>, widthPx: Float, canvas: Canvas, v2: Boolean) {
        // Try the native Onyx charcoal wrappers first; fall back to the pure-Java cloud texture.
        val pts = points.toArrayList()
        val nativeOk: Boolean

        if (!v2) {
            nativeOk = runCatching {
                NeoCharcoalPenWrapper.drawNormalStroke(
                    buildCharcoalArgs(pts, widthPx, canvas, v2 = false)
                )
                true
            }.getOrDefault(false)
        } else {
            nativeOk = runCatching {
                NeoCharcoalPenV2Wrapper.drawNormalStroke(
                    buildCharcoalArgs(pts, widthPx, canvas, v2 = true)
                )
                true
            }.getOrDefault(false)
        }

        if (!nativeOk) charcoalCloudTexture(points, widthPx, canvas, v2)
    }

    private fun buildCharcoalArgs(
        pts: ArrayList<TouchPoint>,
        widthPx: Float,
        canvas: Canvas,
        v2: Boolean,
    ): com.onyx.android.sdk.pen.PenRenderArgs {
        val texture = if (v2) 2 else 1
        val penType = if (v2) 5 else 4

        val createArgs = com.onyx.android.sdk.data.note.ShapeCreateArgs()
            .setMaxPressure(4096f)
            .setPenAttrs(com.onyx.android.sdk.data.note.PenAttrs().setTexture(texture))
            .setTiltConfig(com.onyx.android.sdk.data.note.TiltConfig().setTiltEnabled(false))

        return com.onyx.android.sdk.pen.PenRenderArgs()
            .setCanvas(canvas)
            .setPaint(solidPaint().apply { strokeWidth = widthPx })
            .setPoints(pts)
            .setPenType(penType)
            .setColor(Color.BLACK)
            .setStrokeWidth(widthPx)
            .setCreateArgs(createArgs)
            .setTiltEnabled(false)
            .setScreenMatrix(android.graphics.Matrix())
            .setRenderMatrix(android.graphics.Matrix())
            .setContentRect(android.graphics.RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()))
            .setErase(false)
    }

    /** Pure-Java fallback: ring + dot cloud stamps along the stroke path. */
    private fun charcoalCloudTexture(points: List<TouchPoint>, widthPx: Float, canvas: Canvas, v2: Boolean) {
        val paint = Paint().apply {
            color = Color.BLACK; strokeWidth = 1f
            style = Paint.Style.STROKE; isAntiAlias = false
        }
        val stats = signalStats(points)
        if (points.size == 1) {
            val p = points[0]
            charcoalStamp(p.x, p.y, widthPx, normalizedSignal(p, stats), 0, canvas, paint, v2)
            return
        }
        var prev = points[0]
        for (i in 1 until points.size) {
            val curr = points[i]
            val ps = normalizedSignal(prev, stats); val cs = normalizedSignal(curr, stats)
            val dx = curr.x - prev.x; val dy = curr.y - prev.y
            val dist = sqrt(dx * dx + dy * dy)
            val step = if (v2) 8.5f else 9f
            val n = max(1, ceil(dist / step).toInt())
            for (s in 0..n) {
                val t = s.toFloat() / n
                charcoalStamp(prev.x + dx * t, prev.y + dy * t, widthPx, lerp(ps, cs, t), i * 8192 + s, canvas, paint, v2)
            }
            prev = curr
        }
    }

    private fun charcoalStamp(cx: Float, cy: Float, baseW: Float, sig: Float, seed: Int, canvas: Canvas, paint: Paint, v2: Boolean) {
        val outerR = max(0.8f, baseW * if (v2) 0.68f else 0.58f)
        val innerR = outerR * if (v2) 0.9f else 0.88f
        val edgeD = if (v2) lerp(0.0015f, 0.005f, sig) else lerp(0.002f, 0.006f, sig)
        val dotD = if (v2) lerp(0.001f, 0.0035f, sig) else lerp(0.0014f, 0.004f, sig)
        dotCloud(cx, cy, innerR, outerR, edgeD, seed, canvas, paint)
        dotCloud(cx, cy, 0f, outerR * 0.72f, dotD, seed + 97, canvas, paint)
        if (v2) dotCloud(cx, cy, outerR * 1.02f, outerR * 1.22f, lerp(0.001f, 0.003f, sig), seed + 211, canvas, paint)
    }

    private fun dotCloud(cx: Float, cy: Float, innerR: Float, outerR: Float, density: Float, seed: Int, canvas: Canvas, paint: Paint) {
        val n = max(1, (outerR * outerR * density * 0.1f).toInt())
        val innerRatioSq = if (outerR > 0) (innerR / outerR).pow(2) else 0f
        var drawn = 0; var attempt = 0
        while (drawn < n && attempt < n * 3) {
            val rx = hashUnit(seed, attempt * 2) * 2f - 1f
            val ry = hashUnit(seed, attempt * 2 + 1) * 2f - 1f
            val dSq = rx * rx + ry * ry
            if (dSq <= 1f && dSq >= innerRatioSq) { canvas.drawPoint(cx + rx * outerR, cy + ry * outerR, paint); drawn++ }
            attempt++
        }
    }

    // ─── DASH: DashPathEffect line ────────────────────────────────────────────

    private fun renderDash(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        val dashLen = widthPx * 3f
        val gapLen = widthPx * 1.5f
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = widthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(dashLen, gapLen), 0f)
        }
        drawPolyline(points, canvas, paint)
    }

    // ─── SQUARE_PEN: calligraphy chisel-nib ──────────────────────────────────

    /**
     * Simulates a flat-nibbed calligraphy pen:
     *   stroke width = widthPx × |sin(strokeAngle − nibAngle)|
     * Nib angle 45° produces the classic copperplate look:
     *   diagonal strokes → thick, horizontal/vertical strokes → thin.
     */
    private fun renderSquarePen(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        if (points.size < 2) {
            val paint = solidPaint().apply { style = Paint.Style.FILL }
            points.firstOrNull()?.let { canvas.drawCircle(it.x, it.y, widthPx / 2f, paint) }
            return
        }
        val nibAngle = (PI / 4.0).toFloat()   // 45° nib
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
            strokeJoin = Paint.Join.MITER
            isAntiAlias = true
        }
        var prev = points[0]
        for (i in 1 until points.size) {
            val curr = points[i]
            val dx = curr.x - prev.x; val dy = curr.y - prev.y
            if (abs(dx) < 0.01f && abs(dy) < 0.01f) { prev = curr; continue }
            val angle = atan2(dy, dx)
            val factor = max(0.12f, abs(sin(angle - nibAngle)))
            paint.strokeWidth = widthPx * factor
            canvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint)
            prev = curr
        }
    }

    // ─── Shared utilities ─────────────────────────────────────────────────────

    private fun drawPolyline(points: List<TouchPoint>, canvas: Canvas, paint: Paint) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            val p = points[0]; canvas.drawPoint(p.x, p.y, paint); return
        }
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        canvas.drawPath(path, paint)
    }

    /** Pressure-varying filled-circle fallback used when SDK wrappers are unavailable. */
    private fun fallbackPressureStroke(points: List<TouchPoint>, widthPx: Float, canvas: Canvas) {
        val paint = solidPaint().apply { style = Paint.Style.FILL }
        val stats = signalStats(points)
        if (points.size == 1) {
            val p = points[0]
            canvas.drawCircle(p.x, p.y, max(0.5f, pressureToRadius(widthPx, normalizedSignal(p, stats), HardwarePenStyle.FOUNTAIN)), paint)
            return
        }
        var prev = points[0]
        for (i in 1 until points.size) {
            val curr = points[i]
            val pr = pressureToRadius(widthPx, normalizedSignal(prev, stats), HardwarePenStyle.FOUNTAIN)
            val cr = pressureToRadius(widthPx, normalizedSignal(curr, stats), HardwarePenStyle.FOUNTAIN)
            interpolateCircles(prev, curr, pr, cr, canvas, paint)
            prev = curr
        }
    }

    private fun interpolateCircles(start: TouchPoint, end: TouchPoint, sr: Float, er: Float, canvas: Canvas, paint: Paint) {
        val dx = end.x - start.x; val dy = end.y - start.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.001f) { canvas.drawCircle(start.x, start.y, max(0.5f, sr), paint); return }
        val steps = max(1, ceil(dist / 0.8f).toInt())
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            canvas.drawCircle(start.x + dx * t, start.y + dy * t, max(0.5f, lerp(sr, er, t)), paint)
        }
    }

    private fun solidPaint() = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun pointBounds(points: List<TouchPoint>, padding: Float): Rect {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in points) { minX = min(minX, p.x); minY = min(minY, p.y); maxX = max(maxX, p.x); maxY = max(maxY, p.y) }
        val pad = ceil(padding).toInt() + 4
        return Rect(floor(minX).toInt() - pad, floor(minY).toInt() - pad, ceil(maxX).toInt() + pad, ceil(maxY).toInt() + pad)
    }

    private fun List<TouchPoint>.toArrayList(): ArrayList<TouchPoint> {
        val list = ArrayList<TouchPoint>(size)
        forEach { list.add(TouchPoint(it)) }
        return list
    }

    // ─── Signal / pressure helpers ────────────────────────────────────────────

    private data class SignalStats(val minP: Float, val maxP: Float, val hasP: Boolean)

    private fun signalStats(points: List<TouchPoint>): SignalStats {
        var minP = Float.MAX_VALUE; var maxP = 0f; var hasP = false
        for (p in points) {
            if (p.pressure > 0f) { minP = min(minP, p.pressure); maxP = max(maxP, p.pressure); hasP = true }
        }
        return SignalStats(if (hasP) minP else 0f, if (hasP) maxP else 0f, hasP)
    }

    private fun normalizedSignal(pt: TouchPoint, stats: SignalStats): Float {
        if (!stats.hasP || pt.pressure <= 0f) return 0.35f
        val norm = if (stats.maxP <= 1.05f) pt.pressure.coerceIn(0.01f, 1f)
                   else (pt.pressure / stats.maxP).coerceIn(0.01f, 1f)
        return norm.pow(0.85f).coerceIn(0.01f, 1f)
    }

    private fun pressureToRadius(baseWidth: Float, signal: Float, style: HardwarePenStyle): Float {
        val (curve, minF, maxF) = when (style) {
            HardwarePenStyle.PENCIL -> Triple(0.5f, 0.12f, 1.0f)
            HardwarePenStyle.FOUNTAIN -> Triple(0.5f, 0.10f, 1.05f)
            HardwarePenStyle.NEO_BRUSH -> Triple(0.30f, 0.06f, 2.2f)
            HardwarePenStyle.CHARCOAL -> Triple(0.44f, 0.24f, 2.45f)
            HardwarePenStyle.CHARCOAL_V2 -> Triple(0.42f, 0.34f, 2.95f)
            else -> Triple(0.5f, 0.20f, 1.0f)
        }
        return max(1f, lerp(baseWidth * minF, baseWidth * maxF, signal.coerceIn(0f, 1f).pow(curve)))
    }

    // ─── Math helpers ─────────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    /** Deterministic pseudo-random float in [0, 1) — same inputs → same dot positions. */
    private fun hashUnit(seed: Int, salt: Int): Float {
        var v = seed * 1103515245 + 12345 + salt * 374761393
        v = v xor (v ushr 16); v *= 668265263; v = v xor (v ushr 15)
        return (v ushr 1).toUInt().toFloat() / Int.MAX_VALUE.toFloat()
    }
}
