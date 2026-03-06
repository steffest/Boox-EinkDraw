package com.boox.einkdraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatSeekBar
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import kotlin.math.roundToInt

class VerticalSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs) {

    var forceEinkRefresh: Boolean = false
    var einkUpdateMode: UpdateMode = UpdateMode.GU_FAST
    private val globalRect = Rect()

    private val trackRect = RectF()
    private val fillRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB8B8B8.toInt()
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2E2E2E.toInt()
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val contentLeft = paddingLeft.toFloat()
        val contentTop = paddingTop.toFloat()
        val contentRight = (width - paddingRight).toFloat()
        val contentBottom = (height - paddingBottom).toFloat()

        val contentWidth = (contentRight - contentLeft).coerceAtLeast(0f)
        val contentHeight = (contentBottom - contentTop).coerceAtLeast(0f)

        val trackWidth = (contentWidth * 0.7f).coerceAtLeast(8f)
        val trackLeft = contentLeft + (contentWidth - trackWidth) * 0.5f
        val trackRight = trackLeft + trackWidth
        val trackRadius = trackWidth * 0.5f

        trackRect.set(trackLeft, contentTop, trackRight, contentBottom)
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

        val range = max.coerceAtLeast(1)
        val normalized = (progress.toFloat() / range.toFloat()).coerceIn(0f, 1f)
        val fillTop = contentBottom - (contentHeight * normalized)

        fillRect.set(trackLeft, fillTop, trackRight, contentBottom)
        canvas.drawRoundRect(fillRect, trackRadius, trackRadius, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val y = event.y.coerceIn(paddingTop.toFloat(), (height - paddingBottom).toFloat())
                val travel = (height - paddingTop - paddingBottom).coerceAtLeast(1)
                val ratio = 1f - ((y - paddingTop) / travel.toFloat()).coerceIn(0f, 1f)
                val newProgress = (ratio * max).roundToInt().coerceIn(0, max)
                progress = newProgress
                requestVisualRefresh()
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    performClick()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun requestVisualRefresh() {
        invalidate()
        postInvalidateOnAnimation()
        val parentView = parent as? View
        parentView?.postInvalidateOnAnimation()
        rootView?.postInvalidateOnAnimation()

        if (forceEinkRefresh) {
            runCatching {
                EpdController.invalidate(this, einkUpdateMode)
                parentView?.let { EpdController.invalidate(it, einkUpdateMode) }
                EpdController.refreshScreen(rootView, einkUpdateMode)
                if (getGlobalVisibleRect(globalRect)) EpdController.handwritingRepaint(rootView, globalRect)
            }
        }
    }
}
