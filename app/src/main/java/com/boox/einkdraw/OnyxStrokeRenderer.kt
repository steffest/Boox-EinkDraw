package com.boox.einkdraw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.NeoCharcoalPen
import com.onyx.android.sdk.pen.NeoCharcoalPenV2
import com.onyx.android.sdk.pen.NeoBrushPenWrapper
import com.onyx.android.sdk.pen.NeoFountainPenWrapper
import com.onyx.android.sdk.pen.NeoMarkerPenWrapper
import com.onyx.android.sdk.pen.NeoPen
import com.onyx.android.sdk.pen.NeoPenConfig
import com.onyx.android.sdk.pen.NeoPenUtils
import com.onyx.android.sdk.pen.NeoRenderPoint
import com.onyx.android.sdk.pen.PenResult
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

    // ─── Charcoal V2 rendering ────────────────────────────────────────────────

    fun render(
        style: HardwarePenStyle,
        points: List<TouchPoint>,
        widthPx: Float,
        color: Int,
        canvas: Canvas,
        maxPressure: Float,
    ) {
        if (points.isEmpty()) return
        when (style) {
            HardwarePenStyle.PENCIL -> renderPencil(points, widthPx, color, canvas)
            HardwarePenStyle.FOUNTAIN -> renderFountain(points, widthPx, color, canvas, maxPressure)
            HardwarePenStyle.MARKER -> renderMarker(points, widthPx, color, canvas, maxPressure)
            HardwarePenStyle.NEO_BRUSH -> renderNeoBrush(points, widthPx, color, canvas, maxPressure)
            HardwarePenStyle.CHARCOAL -> renderCharcoal(points, widthPx, color, canvas, v2 = false, maxPressure = maxPressure)
            HardwarePenStyle.DASH -> renderDash(points, widthPx, color, canvas)
            HardwarePenStyle.CHARCOAL_V2 -> renderCharcoal(points, widthPx, color, canvas, v2 = true, maxPressure = maxPressure)
            HardwarePenStyle.SQUARE_PEN -> renderSquarePen(points, widthPx, color, canvas)
        }
    }

    // ─── PENCIL: deterministic stipple dots ───────────────────────────────────

    private fun renderPencil(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas) {
        val paint = Paint().apply {
            this.color = color
            strokeWidth = widthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        drawPolyline(points, canvas, paint)
    }

    // ─── FOUNTAIN: NeoFountainPenWrapper ──────────────────────────────────────

    private fun renderFountain(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas, maxPressure: Float) {
        val paint = solidPaint(color)
        val pts = points.toArrayList()
        try {
            val pressureDivisor = nativePressureDivisor(pts, maxPressure)
            // Signature: (points, displayScale, strokeWidth, maxTouchPressure)
            // The wrapper divides each point.pressure by maxTouchPressure internally.
            val result = NeoFountainPenWrapper.computeStrokePoints(pts, 1f, widthPx, pressureDivisor)
            com.onyx.android.sdk.pen.PenUtils.drawStrokeByPointSize(canvas, paint, result, false)
        } catch (_: Throwable) {
            fallbackPressureStroke(points, widthPx, color, canvas, HardwarePenStyle.FOUNTAIN, maxPressure)
        }
    }

    // ─── MARKER: NeoMarkerPenWrapper, 50 % alpha offscreen composite ──────────

    private fun renderMarker(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas, maxPressure: Float) {
        android.util.Log.d("MarkerTest", "renderMarker: ${points.size} pts width=$widthPx")
        if (points.isEmpty()) return
        val pts = points.toArrayList()
        val paint = solidPaint(color).apply {
            strokeWidth = widthPx; isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        try {
            // Try the native wrapper — libneo_pen.so is now bundled in jniLibs/arm64-v8a
            val result = NeoMarkerPenWrapper.computeStrokePoints(pts, widthPx, maxPressure)
            android.util.Log.d("MarkerTest", "native OK: ${result?.size} result pts")
            NeoMarkerPenWrapper.drawStroke(canvas, paint, result, widthPx, false)
        } catch (e: Throwable) {
            // Native still fails for some reason — use pure-Kotlin replica
            android.util.Log.w("MarkerTest", "native failed (${e.javaClass.simpleName}), using fallback")
            renderMarkerFallback(pts, widthPx, paint, canvas)
        }
    }

    /** Pure-Kotlin replica of NeoMarkerPenWrapper.drawStroke — no native library needed. */
    private fun renderMarkerFallback(pts: ArrayList<TouchPoint>, widthPx: Float, paint: Paint, canvas: Canvas) {
        // Set TouchPoint.size based on pressure (what the native engine computes)
        val stats = signalStats(pts)
        for (p in pts) {
            p.size = widthPx * (0.4f + 0.6f * normalizedSignal(p, stats))
        }
        // Draw into offscreen bitmap at 100% opacity
        val bmp = android.graphics.Bitmap.createBitmap(canvas.width, canvas.height, android.graphics.Bitmap.Config.ARGB_8888)
        val bmpCanvas = Canvas(bmp)
        com.onyx.android.sdk.pen.PenUtils.drawStrokeByPointSize(bmpCanvas, paint, pts, false)
        // Compute bounding rect and inset by half stroke-width (same as NeoMarkerPenWrapper)
        var rect: android.graphics.Rect? = null
        for (p in pts) {
            if (rect == null) rect = android.graphics.Rect(p.x.toInt(), p.y.toInt(), p.x.toInt(), p.y.toInt())
            else rect.union(p.x.toInt(), p.y.toInt())
        }
        if (rect == null) { bmp.recycle(); return }
        rect.inset(-(widthPx / 2f).toInt(), -(widthPx / 2f).toInt())
        // Composite at alpha=128 (50%) — exactly like NeoMarkerPenWrapper.drawStroke
        val savedAlpha = paint.alpha
        paint.alpha = 128
        canvas.drawBitmap(bmp, rect, rect, paint)
        paint.alpha = savedAlpha
        bmp.recycle()
        android.util.Log.d("MarkerTest", "fallback done")
    }

    // ─── NEO_BRUSH: NeoBrushPenWrapper ───────────────────────────────────────

    private fun renderNeoBrush(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas, maxPressure: Float) {
        val pts = points.toArrayList()
        val paint = solidPaint(color)
        try {
            val pressureDivisor = nativePressureDivisor(pts, maxPressure)
            val result = NeoBrushPenWrapper.computeStrokePoints(pts, widthPx, pressureDivisor)
            if (!result.isNullOrEmpty()) {
                com.onyx.android.sdk.pen.PenUtils.drawStrokeByPointSize(canvas, paint, result, false)
            } else {
                fallbackPressureStroke(points, widthPx, color, canvas, HardwarePenStyle.NEO_BRUSH, maxPressure)
            }
        } catch (_: Throwable) {
            fallbackPressureStroke(points, widthPx, color, canvas, HardwarePenStyle.NEO_BRUSH, maxPressure)
        }
    }

    // ─── CHARCOAL / CHARCOAL_V2 ──────────────────────────────────────────────
    //
    // Uses native wrappers:
    //  - V1: NeoCharcoalPenWrapper.drawNormalStroke
    //  - V2: NeoCharcoalPenV2Wrapper.drawNormalStroke
    // A GC hint before each call encourages the JVM to collect Bitmap / PenResult
    // objects from the previous stroke before allocating new native stamps, preventing
    // heap exhaustion across many strokes.
    // Kotlin cloud-texture path is retained only as fallback.

    private fun renderCharcoal(
        points: List<TouchPoint>,
        widthPx: Float,
        color: Int,
        canvas: Canvas,
        v2: Boolean,
        maxPressure: Float,
    ) {
        if (points.size < 2) { charcoalCloudTexture(points, widthPx, color, canvas, v2); return }

        // Encourage GC to collect stamp objects from the previous stroke's render.
        System.gc()

        val prepared = prepareCharcoalPoints(points, maxPressure)
        val pressureInfo = prepared.second
        val penType = if (v2) {
            com.onyx.android.sdk.pen.NeoPenConfig.NEOPEN_PEN_TYPE_CHARCOAL_V2
        } else {
            com.onyx.android.sdk.pen.NeoPenConfig.NEOPEN_PEN_TYPE_CHARCOAL
        }
        val createArgs = com.onyx.android.sdk.data.note.ShapeCreateArgs()
            .setMaxPressure(pressureInfo.renderMaxPressure)

        val args = com.onyx.android.sdk.pen.PenRenderArgs()
            .setCanvas(canvas)
            .setPoints(prepared.first)
            .setStrokeWidth(widthPx)
            .setColor(color)
            .setContentRect(RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()))
            .setPenType(penType)
            .setScreenMatrix(android.graphics.Matrix())
            .setRenderMatrix(android.graphics.Matrix())
            .setTiltEnabled(true)
            .setErase(false)
            .setCreateArgs(createArgs)

        android.util.Log.d(
            "CharcoalTest",
            "v${if (v2) 2 else 1} pts=${prepared.first.size} pressure=[${"%.3f".format(pressureInfo.minPressure)},${"%.3f".format(pressureInfo.maxPressure)}] " +
                "renderMax=${"%.3f".format(pressureInfo.renderMaxPressure)} normalized=${pressureInfo.normalizedInput} " +
                charcoalTiltSummary(prepared.first)
        )

        runCatching {
            // Hardware preview path uses stronger pressure/min-width defaults than wrapper defaults.
            // Match those config values first; fallback to SDK wrapper if the custom path fails.
            val customDrawn = drawCharcoalWithHardwareLikeConfig(
                points = prepared.first,
                widthPx = widthPx,
                color = color,
                canvas = canvas,
                maxPressure = pressureInfo.renderMaxPressure,
                v2 = v2,
            )
            if (!customDrawn) {
                if (v2) {
                    com.onyx.android.sdk.pen.NeoCharcoalPenV2Wrapper.drawNormalStroke(args)
                } else {
                    com.onyx.android.sdk.pen.NeoCharcoalPenWrapper.drawNormalStroke(args)
                }
            }
        }.onFailure { e ->
            android.util.Log.w("CharcoalTest", "v${if (v2) 2 else 1} drawNormalStroke failed: ${e.message}")
            charcoalCloudTexture(points, widthPx, color, canvas, v2)
        }

        android.util.Log.d("CharcoalTest", "v${if (v2) 2 else 1} drawNormalStroke pts=${points.size}")
    }

    /**
     * Replica of the charcoal wrapper call chain with explicit pen config tuning to match
     * hardware-preview output (pressure response + minimum stamp width).
     */
    private fun drawCharcoalWithHardwareLikeConfig(
        points: List<TouchPoint>,
        widthPx: Float,
        color: Int,
        canvas: Canvas,
        maxPressure: Float,
        v2: Boolean,
    ): Boolean {
        if (points.size < 2) return false
        val safeMaxPressure = max(1f, maxPressure)
        val screenMatrix = Matrix()
        val penConfig = NeoPenConfig()
            .setColor(color)
            .setWidth(widthPx)
            .setTiltEnabled(true)
            .setRotateAngle(0)
            .setMaxTouchPressure(safeMaxPressure)
        penConfig.pressureSensitivity = 1.0f
        penConfig.minWidth = 1.0f

        val pen: NeoPen = if (v2) {
            NeoCharcoalPenV2.Companion.create(penConfig)
        } else {
            NeoCharcoalPen.Companion.create(penConfig)
        } ?: return false

        val bitmaps = ArrayList<Bitmap>(512)
        return try {
            val mapped = NeoPenUtils.mapToPenCanvas(points, screenMatrix)
            if (mapped.size < 2) return false
            for (p in mapped) {
                p.pressure /= safeMaxPressure
            }

            val renderPoints = ArrayList<NeoRenderPoint>(mapped.size * 3)
            invokePenDown(pen, mapped[0])?.let { NeoPenUtils.readTextureResult(it, bitmaps, renderPoints) }
            if (mapped.size > 2) {
                invokePenMove(pen, mapped.subList(1, mapped.size - 1))
                    ?.let { NeoPenUtils.readTextureResult(it, bitmaps, renderPoints) }
            }
            invokePenUp(pen, mapped[mapped.size - 1])?.let { NeoPenUtils.readTextureResult(it, bitmaps, renderPoints) }
            if (renderPoints.isEmpty() || bitmaps.isEmpty()) return false

            val inverse = Matrix()
            val mappedRenderPoints = if (screenMatrix.invert(inverse)) {
                NeoPenUtils.mapFromPenCanvas(renderPoints.toTypedArray(), bitmaps, inverse)
            } else {
                renderPoints.toTypedArray()
            }

            for (rp in mappedRenderPoints) {
                val idx = rp.bitmapIndex
                if (idx in 0 until bitmaps.size) {
                    canvas.drawBitmap(bitmaps[idx], rp.x, rp.y, null)
                }
            }
            true
        } finally {
            runCatching { pen.destroy() }
            bitmaps.forEach { bmp ->
                if (!bmp.isRecycled) runCatching { bmp.recycle() }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokePenDown(
        pen: NeoPen,
        point: TouchPoint,
    ): Pair<PenResult?, PenResult?>? = runCatching {
        val basePoint = Class.forName("com.onyx.android.sdk.base.data.TouchPoint")
        val fn = pen.javaClass.getMethod("onPenDown", basePoint, Boolean::class.javaPrimitiveType)
        fn.invoke(pen, point, true) as? Pair<PenResult?, PenResult?>
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun invokePenMove(
        pen: NeoPen,
        points: List<TouchPoint>,
    ): Pair<PenResult?, PenResult?>? = runCatching {
        val basePoint = Class.forName("com.onyx.android.sdk.base.data.TouchPoint")
        val fn = pen.javaClass.getMethod("onPenMove", List::class.java, basePoint, Boolean::class.javaPrimitiveType)
        fn.invoke(pen, points, null, true) as? Pair<PenResult?, PenResult?>
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun invokePenUp(
        pen: NeoPen,
        point: TouchPoint,
    ): Pair<PenResult?, PenResult?>? = runCatching {
        val basePoint = Class.forName("com.onyx.android.sdk.base.data.TouchPoint")
        val fn = pen.javaClass.getMethod("onPenUp", basePoint, Boolean::class.javaPrimitiveType)
        fn.invoke(pen, point, true) as? Pair<PenResult?, PenResult?>
    }.getOrNull()

    private data class PressureInfo(
        val minPressure: Float,
        val maxPressure: Float,
        val renderMaxPressure: Float,
        val normalizedInput: Boolean,
    )

    private fun charcoalTiltSummary(points: List<TouchPoint>): String {
        if (points.isEmpty()) return "tilt=none"
        var minTx = Int.MAX_VALUE
        var maxTx = Int.MIN_VALUE
        var minTy = Int.MAX_VALUE
        var maxTy = Int.MIN_VALUE
        var nonZero = 0
        for (p in points) {
            minTx = min(minTx, p.tiltX)
            maxTx = max(maxTx, p.tiltX)
            minTy = min(minTy, p.tiltY)
            maxTy = max(maxTy, p.tiltY)
            if (p.tiltX != 0 || p.tiltY != 0) nonZero++
        }
        return "tiltX=[$minTx,$maxTx] tiltY=[$minTy,$maxTy] nz=$nonZero"
    }

    /**
     * RawInput callbacks can deliver pressure in mixed units (0..1 or 0..MAX_TOUCH_PRESSURE).
     * The native charcoal wrapper always divides by createArgs.maxPressure, so we normalize
     * max-pressure per stroke to keep output consistent and avoid invisible strokes.
     */
    private fun prepareCharcoalPoints(points: List<TouchPoint>, deviceMaxPressure: Float): Pair<ArrayList<TouchPoint>, PressureInfo> {
        val out = ArrayList<TouchPoint>(points.size)
        var minP = Float.MAX_VALUE
        var maxP = 0f
        var hasPressure = false
        for (p in points) {
            val copy = TouchPoint(p)
            out.add(copy)
            if (copy.pressure > 0f) {
                hasPressure = true
                minP = min(minP, copy.pressure)
                maxP = max(maxP, copy.pressure)
            }
        }

        if (!hasPressure) {
            // Keep behavior deterministic even when pressure is missing entirely.
            val fallback = 0.35f
            for (p in out) p.pressure = fallback
            return out to PressureInfo(
                minPressure = fallback,
                maxPressure = fallback,
                renderMaxPressure = 1f,
                normalizedInput = true,
            )
        }

        val normalizedInput = maxP <= 1.5f
        val renderMax = if (normalizedInput) 1f else max(deviceMaxPressure, maxP)
        return out to PressureInfo(
            minPressure = minP,
            maxPressure = maxP,
            renderMaxPressure = renderMax,
            normalizedInput = normalizedInput,
        )
    }

    /**
     * Native pen wrappers normalize by dividing point.pressure by this divisor.
     * RawInput can deliver either already-normalized [0..1] or raw [0..MAX] pressure.
     */
    private fun nativePressureDivisor(points: List<TouchPoint>, deviceMaxPressure: Float): Float {
        var maxP = 0f
        for (p in points) {
            if (p.pressure > 0f) maxP = max(maxP, p.pressure)
        }
        if (maxP <= 0f) return 1f
        return if (maxP <= 1.5f) 1f else max(deviceMaxPressure, maxP)
    }

    /** Pure-Java fallback: ring + dot cloud stamps along the stroke path. */
    private fun charcoalCloudTexture(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas, v2: Boolean) {
        val paint = Paint().apply {
            this.color = withAlpha(color, 160) // Proper charcoal opacity layer
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = false
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

    // ─── DASH: black+white dashed track (matches Onyx overlay style) ─────────

    private fun renderDash(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas) {
        if (points.isEmpty()) return

        val dashPeriod = max(2f, widthPx * 3f)
        val phase = 0f
        val offset = max(0.5f, widthPx / 3f)

        val basePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = widthPx
            isAntiAlias = true
        }
        val whiteDash = Paint(basePaint).apply {
            this.color = Color.WHITE
            pathEffect = DashPathEffect(
                floatArrayOf(max(1f, dashPeriod - offset), dashPeriod + offset),
                phase,
            )
        }
        val blackDash = Paint(basePaint).apply {
            this.color = color
            pathEffect = DashPathEffect(floatArrayOf(dashPeriod, dashPeriod), phase)
        }

        if (points.size == 1) {
            val p = points[0]
            val radius = max(0.75f, widthPx * 0.5f)
            val whiteDot = Paint(basePaint).apply {
                style = Paint.Style.FILL
                this.color = Color.WHITE
                pathEffect = null
            }
            val blackDot = Paint(basePaint).apply {
                style = Paint.Style.FILL
                this.color = color
                pathEffect = null
            }
            canvas.drawCircle(p.x + offset, p.y + offset, radius, whiteDot)
            canvas.drawCircle(p.x, p.y, radius, blackDot)
            return
        }

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        canvas.save()
        canvas.translate(offset, offset)
        canvas.drawPath(path, whiteDash)
        canvas.restore()
        canvas.drawPath(path, blackDash)
    }

    // ─── SQUARE_PEN: calligraphy chisel-nib ──────────────────────────────────

    /**
     * Simulates a flat-nibbed calligraphy pen:
     *   stroke width = widthPx × |sin(strokeAngle − nibAngle)|
     * Nib angle 45° produces the classic copperplate look:
     *   diagonal strokes → thick, horizontal/vertical strokes → thin.
     */
    private fun renderSquarePen(points: List<TouchPoint>, widthPx: Float, color: Int, canvas: Canvas) {
        if (points.size < 2) {
            val paint = solidPaint(color).apply { style = Paint.Style.FILL }
            points.firstOrNull()?.let { canvas.drawCircle(it.x, it.y, widthPx / 2f, paint) }
            return
        }
        val nibAngle = (PI / 4.0).toFloat()   // 45° nib
        val paint = Paint().apply {
            this.color = color
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
    private fun fallbackPressureStroke(
        points: List<TouchPoint>,
        widthPx: Float,
        color: Int,
        canvas: Canvas,
        penStyle: HardwarePenStyle,
        maxPressure: Float,
    ) {
        val paint = solidPaint(color).apply { style = Paint.Style.FILL }
        val pressureDivisor = nativePressureDivisor(points, maxPressure)
        if (points.size == 1) {
            val p = points[0]
            canvas.drawCircle(p.x, p.y, max(0.5f, pressureToRadius(widthPx, normalizedSignalAbsolute(p, pressureDivisor), penStyle)), paint)
            return
        }
        var prev = points[0]
        for (i in 1 until points.size) {
            val curr = points[i]
            val pr = pressureToRadius(widthPx, normalizedSignalAbsolute(prev, pressureDivisor), penStyle)
            val cr = pressureToRadius(widthPx, normalizedSignalAbsolute(curr, pressureDivisor), penStyle)
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

    private fun solidPaint(color: Int) = Paint().apply {
        this.color = color
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

    /**
     * Absolute pressure normalization (vs per-stroke relative), used by fallback
     * paths to avoid width spikes on short low-pressure strokes.
     */
    private fun normalizedSignalAbsolute(pt: TouchPoint, pressureDivisor: Float): Float {
        if (pt.pressure <= 0f) return 0.35f
        val norm = if (pressureDivisor <= 1.5f) {
            pt.pressure.coerceIn(0.01f, 1f)
        } else {
            (pt.pressure / pressureDivisor).coerceIn(0.01f, 1f)
        }
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

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    /** Deterministic pseudo-random float in [0, 1) — same inputs → same dot positions. */
    private fun hashUnit(seed: Int, salt: Int): Float {
        var v = seed * 1103515245 + 12345 + salt * 374761393
        v = v xor (v ushr 16); v *= 668265263; v = v xor (v ushr 15)
        return (v ushr 1).toUInt().toFloat() / Int.MAX_VALUE.toFloat()
    }
}
