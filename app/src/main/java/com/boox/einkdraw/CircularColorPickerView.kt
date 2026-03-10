package com.boox.einkdraw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Circular HSV picker:
 *  - outer ring: hue
 *  - inner square: shade selection for current hue
 */
class CircularColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onColorChanged: ((Int) -> Unit)? = null

    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val markerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val markerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2f)
    }
    private val markerStrokeDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x66000000
        strokeWidth = dp(1f)
    }

    private var cx = 0f
    private var cy = 0f
    private var outerRadius = 0f
    private var ringWidth = 0f
    private var ringRadius = 0f
    private var ringInnerRadius = 0f
    private var innerRadius = 0f
    private var shadeHalf = 0f

    private var hue = 220f
    private var selectedColor = Color.BLACK
    private var markerX = 0f
    private var markerY = 0f
    private var hasMarker = false
    private var suppressCallback = false

    private var wheelShader: Shader? = null
    private var innerBitmap: Bitmap? = null

    private enum class TouchTarget { NONE, HUE, SHADE }
    private var touchTarget = TouchTarget.NONE

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        cx = w * 0.5f
        cy = h * 0.5f
        val radius = min(w, h) * 0.5f
        outerRadius = radius - dp(6f)
        ringWidth = max(dp(18f), outerRadius * 0.20f)
        ringRadius = outerRadius - ringWidth * 0.5f
        ringInnerRadius = ringRadius - ringWidth * 0.5f
        innerRadius = max(0f, ringInnerRadius - dp(7f))
        shadeHalf = innerRadius / SQRT_2

        wheelPaint.strokeWidth = ringWidth
        wheelShader = SweepGradientCompat.create(cx, cy)
        wheelPaint.shader = wheelShader

        rebuildInnerBitmap()

        if (!hasMarker) {
            markerX = cx
            markerY = cy
            hasMarker = true
        }
        updateSelectedColorFromMarker(notify = false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (outerRadius <= 0f) return

        canvas.drawCircle(cx, cy, ringRadius, wheelPaint)
        innerBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        val hueRad = Math.toRadians(hue.toDouble())
        val hueX = cx + cos(hueRad).toFloat() * ringRadius
        val hueY = cy + sin(hueRad).toFloat() * ringRadius
        markerFillPaint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        canvas.drawCircle(hueX, hueY, dp(9f), markerFillPaint)
        canvas.drawCircle(hueX, hueY, dp(10f), markerStrokePaint)
        canvas.drawCircle(hueX, hueY, dp(10f), markerStrokeDarkPaint)

        markerFillPaint.color = selectedColor
        canvas.drawCircle(markerX, markerY, dp(6f), markerFillPaint)
        canvas.drawCircle(markerX, markerY, dp(7f), markerStrokePaint)
        canvas.drawCircle(markerX, markerY, dp(7f), markerStrokeDarkPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (outerRadius <= 0f) return false
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchTarget = when {
                    isInHueRing(x, y) -> TouchTarget.HUE
                    isInShadeArea(x, y) -> TouchTarget.SHADE
                    else -> TouchTarget.NONE
                }
                when (touchTarget) {
                    TouchTarget.HUE -> updateHueFromTouch(x, y, notify = true)
                    TouchTarget.SHADE -> updateShadeFromTouch(x, y, notify = true)
                    TouchTarget.NONE -> return false
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                when (touchTarget) {
                    TouchTarget.HUE -> {
                        updateHueFromTouch(x, y, notify = true)
                        return true
                    }
                    TouchTarget.SHADE -> {
                        updateShadeFromTouch(x, y, notify = true)
                        return true
                    }
                    TouchTarget.NONE -> return false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchTarget = TouchTarget.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setColor(color: Int) {
        selectedColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
        val hsv = FloatArray(3)
        Color.colorToHSV(selectedColor, hsv)
        if (hsv[1] > 0.01f && hsv[2] > 0.01f) {
            hue = hsv[0]
        }
        if (width <= 0 || height <= 0) {
            invalidate()
            return
        }
        suppressCallback = true
        rebuildInnerBitmap()
        positionMarkerForColor(selectedColor)
        updateSelectedColorFromMarker(notify = false)
        suppressCallback = false
        invalidate()
    }

    private fun updateHueFromTouch(x: Float, y: Float, notify: Boolean) {
        val angle = Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble()))
        hue = ((angle + 360.0) % 360.0).toFloat()
        rebuildInnerBitmap()
        if (!hasMarker) {
            markerX = cx
            markerY = cy
            hasMarker = true
        }
        updateSelectedColorFromMarker(notify = notify)
        invalidate()
    }

    private fun updateShadeFromTouch(x: Float, y: Float, notify: Boolean) {
        markerX = x.coerceIn(cx - shadeHalf, cx + shadeHalf)
        markerY = y.coerceIn(cy - shadeHalf, cy + shadeHalf)
        hasMarker = true
        updateSelectedColorFromMarker(notify = notify)
        invalidate()
    }

    private fun updateSelectedColorFromMarker(notify: Boolean) {
        val color = sampleInnerColor(markerX, markerY)
        selectedColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
        if (notify && !suppressCallback) {
            onColorChanged?.invoke(selectedColor)
        }
    }

    private fun rebuildInnerBitmap() {
        if (width <= 0 || height <= 0 || shadeHalf <= 0f) return

        val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        innerBitmap?.recycle()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        val hueR = Color.red(hueColor)
        val hueG = Color.green(hueColor)
        val hueB = Color.blue(hueColor)

        val minX = max(0, (cx - shadeHalf).roundToInt())
        val maxX = min(width - 1, (cx + shadeHalf).roundToInt())
        val minY = max(0, (cy - shadeHalf).roundToInt())
        val maxY = min(height - 1, (cy + shadeHalf).roundToInt())
        val side = shadeHalf * 2f

        var y = minY
        while (y <= maxY) {
            val yNorm = ((y - (cy - shadeHalf)) / side).coerceIn(0f, 1f)
            val valueFactor = (1f - yNorm).coerceIn(0f, 1f)
            var x = minX
            while (x <= maxX) {
                val xNorm = ((x - (cx - shadeHalf)) / side).coerceIn(0f, 1f)
                val baseR = lerp(255f, hueR.toFloat(), xNorm)
                val baseG = lerp(255f, hueG.toFloat(), xNorm)
                val baseB = lerp(255f, hueB.toFloat(), xNorm)
                val r = (baseR * valueFactor).roundToInt().coerceIn(0, 255)
                val g = (baseG * valueFactor).roundToInt().coerceIn(0, 255)
                val b = (baseB * valueFactor).roundToInt().coerceIn(0, 255)
                pixels[y * width + x] = Color.argb(255, r, g, b)
                x++
            }
            y++
        }

        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        innerBitmap = bmp
    }

    private fun positionMarkerForColor(targetColor: Int) {
        val bmp = innerBitmap ?: return
        val minX = max(0, (cx - shadeHalf).roundToInt())
        val maxX = min(bmp.width - 1, (cx + shadeHalf).roundToInt())
        val minY = max(0, (cy - shadeHalf).roundToInt())
        val maxY = min(bmp.height - 1, (cy + shadeHalf).roundToInt())

        var bestX = cx
        var bestY = cy
        var bestDist = Long.MAX_VALUE

        var y = minY
        while (y <= maxY) {
            var x = minX
            while (x <= maxX) {
                val c = bmp.getPixel(x, y)
                if (Color.alpha(c) > 10) {
                    val dr = Color.red(c) - Color.red(targetColor)
                    val dg = Color.green(c) - Color.green(targetColor)
                    val db = Color.blue(c) - Color.blue(targetColor)
                    val dist = (dr * dr + dg * dg + db * db).toLong()
                    if (dist < bestDist) {
                        bestDist = dist
                        bestX = x.toFloat()
                        bestY = y.toFloat()
                        if (dist == 0L) break
                    }
                }
                x += 2
            }
            if (bestDist == 0L) break
            y += 2
        }

        markerX = bestX
        markerY = bestY
        hasMarker = true
    }

    private fun isInHueRing(x: Float, y: Float): Boolean {
        val dx = x - cx
        val dy = y - cy
        val d = sqrt(dx * dx + dy * dy)
        return d >= ringInnerRadius && d <= outerRadius
    }

    private fun isInShadeArea(x: Float, y: Float): Boolean {
        return x >= cx - shadeHalf &&
            x <= cx + shadeHalf &&
            y >= cy - shadeHalf &&
            y <= cy + shadeHalf
    }

    private fun sampleInnerColor(x: Float, y: Float): Int {
        val bmp = innerBitmap ?: return selectedColor
        val px = x.roundToInt().coerceIn(0, bmp.width - 1)
        val py = y.roundToInt().coerceIn(0, bmp.height - 1)
        val c = bmp.getPixel(px, py)
        return if (Color.alpha(c) > 10) c else selectedColor
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        innerBitmap?.recycle()
        innerBitmap = null
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    companion object {
        private const val SQRT_2 = 1.4142135f
    }
}

private object SweepGradientCompat {
    private val HUES = intArrayOf(
        Color.RED,
        Color.YELLOW,
        Color.GREEN,
        Color.CYAN,
        Color.BLUE,
        Color.MAGENTA,
        Color.RED,
    )

    fun create(cx: Float, cy: Float): Shader = android.graphics.SweepGradient(cx, cy, HUES, null)
}
