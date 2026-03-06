package com.boox.einkdraw

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import kotlin.math.pow

class BooxRapidOverlayService : Service() {
    companion object {
        const val ACTION_STOP = "com.boox.einkdraw.action.STOP_RAPID_OVERLAY"
        const val ACTION_SET_VIEWPORT = "com.boox.einkdraw.action.SET_RAPID_VIEWPORT"
        const val ACTION_SET_BRUSH = "com.boox.einkdraw.action.SET_BRUSH"
        const val EXTRA_VIEWPORT_ZOOM = "viewport_zoom"
        const val EXTRA_VIEWPORT_PAN_X = "viewport_pan_x"
        const val EXTRA_VIEWPORT_PAN_Y = "viewport_pan_y"
        const val EXTRA_VIEW_LEFT = "view_left"
        const val EXTRA_VIEW_TOP = "view_top"
        const val EXTRA_VIEW_WIDTH = "view_width"
        const val EXTRA_VIEW_HEIGHT = "view_height"
        const val EXTRA_BRUSH_SIZE_PERCENT = "brush_size_percent"
        const val EXTRA_BRUSH_OPACITY_PERCENT = "brush_opacity_percent"
        private const val CHANNEL_ID = "boox_rapid_overlay_channel"
        private const val NOTIFICATION_ID = 101
        private const val STROKE_WIDTH = 1.8f
    }

    private lateinit var wm: WindowManager
    private lateinit var overlayView: SurfaceView
    private var touchHelper: TouchHelper? = null
    private var overlayScale = 1f
    private var overlayPanX = 0f
    private var overlayPanY = 0f
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var brushSizePercent = 40
    private var brushOpacityPercent = 100
    private val minOverlayZoom = 1f
    private val maxOverlayZoom = 4f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlaySurface()
        initRawDrawing()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SET_VIEWPORT) {
            val zoom = intent.getFloatExtra(EXTRA_VIEWPORT_ZOOM, overlayScale)
            val panX = intent.getFloatExtra(EXTRA_VIEWPORT_PAN_X, overlayPanX)
            val panY = intent.getFloatExtra(EXTRA_VIEWPORT_PAN_Y, overlayPanY)
            val viewLeft = intent.getIntExtra(EXTRA_VIEW_LEFT, 0)
            val viewTop = intent.getIntExtra(EXTRA_VIEW_TOP, 0)
            val viewWidth = intent.getIntExtra(EXTRA_VIEW_WIDTH, 0)
            val viewHeight = intent.getIntExtra(EXTRA_VIEW_HEIGHT, 0)
            setViewport(zoom, panX, panY, viewLeft, viewTop, viewWidth, viewHeight)
            return START_STICKY
        }
        if (intent?.action == ACTION_SET_BRUSH) {
            brushSizePercent = intent.getIntExtra(EXTRA_BRUSH_SIZE_PERCENT, brushSizePercent).coerceIn(0, 100)
            brushOpacityPercent = intent.getIntExtra(EXTRA_BRUSH_OPACITY_PERCENT, brushOpacityPercent).coerceIn(0, 100)
            applyRapidBrushSettings()
            return START_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        touchHelper?.closeRawDrawing()
        runCatching { wm.removeViewImmediate(overlayView) }
        touchHelper = null
        RapidInputTransform.clear()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.rapid_overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val stopIntent = Intent(this, BooxRapidOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(getString(R.string.rapid_overlay_title))
            .setContentText(getString(R.string.rapid_overlay_running))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createOverlaySurface() {
        overlayView = SurfaceView(this).apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            alpha = 1f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = 0.2f
        }

        wm.addView(overlayView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRawDrawing() {
        val helper = runCatching { TouchHelper.create(overlayView, 2, rawCallback) }.getOrNull()
        if (helper == null) {
            Toast.makeText(this, R.string.rapid_overlay_not_supported, Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        touchHelper = helper
        helper.setPenUpRefreshTimeMs(1000)
        overlayView.addOnLayoutChangeListener(layoutListener)
        overlayView.setOnTouchListener { _: View?, _: MotionEvent? -> true }
    }

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
        val helper = touchHelper ?: return@OnLayoutChangeListener
        val bounds = Rect(0, 0, right, bottom)
        overlayView.getLocalVisibleRect(bounds)
        helper.setStrokeColor(Color.BLACK)
        helper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
        helper.openRawDrawing()
        helper.setRawDrawingRenderEnabled(true)
        helper.setStrokeWidth(STROKE_WIDTH).setLimitRect(bounds, listOf())
        helper.setRawInputReaderEnable(!helper.isRawDrawingInputEnabled)
        applyRapidBrushSettings()
    }

    private val rawCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onEndRawDrawing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {}
        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {}
        override fun onBeginRawErasing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onEndRawErasing(success: Boolean, touchPoint: TouchPoint?) {}
        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {}
        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {}

        override fun onPenActive(point: TouchPoint?) {
            touchHelper?.setRawDrawingEnabled(true)
        }

        override fun onPenUpRefresh(refreshRect: RectF?) {
            touchHelper?.isRawDrawingRenderEnabled = true
            super.onPenUpRefresh(refreshRect)
        }
    }

    private fun applyRapidBrushSettings() {
        val helper = touchHelper ?: return
        val tSize = brushSizePercent / 100f
        val widthFactor = 0.05f + (tSize * 10.35f)
        val width = STROKE_WIDTH * widthFactor
        val tOpacity = (brushOpacityPercent / 100f).toDouble().pow(3.0).toFloat()
        val gray = (255 - (tOpacity * 255f)).toInt().coerceIn(0, 255)
        helper.setStrokeWidth(width)
        helper.setStrokeColor(Color.rgb(gray, gray, gray))
    }

    private fun setViewport(
        zoom: Float,
        panX: Float,
        panY: Float,
        viewLeft: Int,
        viewTop: Int,
        viewWidth: Int,
        viewHeight: Int
    ) {
        overlayScale = zoom.coerceIn(minOverlayZoom, maxOverlayZoom)
        overlayPanX = panX
        overlayPanY = panY
        viewportWidth = viewWidth
        viewportHeight = viewHeight
        clampOverlayPan()
        applyOverlayTransform()
        RapidInputTransform.update(
            overlayScale,
            overlayPanX,
            overlayPanY,
            viewLeft,
            viewTop,
            viewWidth,
            viewHeight,
            DrawingView.CANVAS_WIDTH,
            DrawingView.CANVAS_HEIGHT
        )
    }

    private fun applyOverlayTransform() {
        overlayView.scaleX = overlayScale
        overlayView.scaleY = overlayScale
        overlayView.translationX = overlayPanX
        overlayView.translationY = overlayPanY
    }

    private fun clampOverlayPan() {
        val w = if (viewportWidth > 0) viewportWidth.toFloat() else overlayView.width.toFloat()
        val h = if (viewportHeight > 0) viewportHeight.toFloat() else overlayView.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val maxPanX = ((overlayScale - 1f) * w * 0.5f).coerceAtLeast(0f)
        val maxPanY = ((overlayScale - 1f) * h * 0.5f).coerceAtLeast(0f)
        overlayPanX = overlayPanX.coerceIn(-maxPanX, maxPanX)
        overlayPanY = overlayPanY.coerceIn(-maxPanY, maxPanY)
    }
}
