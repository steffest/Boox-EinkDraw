package com.boox.einkdraw

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var buttonRapidMode: Button
    private lateinit var sliderBrushSize: VerticalSeekBar
    private lateinit var sliderBrushOpacity: VerticalSeekBar
    private var rapidModeEnabled = false
    private var activePaletteColorArgb: Int = Color.BLACK
    private val paletteButtons = mutableListOf<Pair<ImageButton, PaletteColor>>()
    private var colorPickerPopup: PopupWindow? = null

    private data class PaletteColor(
        val name: String,
        val argb: Int
    )

    private val paletteColors = listOf(
        PaletteColor("black", Color.BLACK),
        PaletteColor("white", Color.WHITE),
        PaletteColor("green", Color.rgb(0, 170, 0)),
        PaletteColor("turquoise", Color.rgb(0, 180, 170)),
        PaletteColor("blue", Color.rgb(0, 70, 220)),
        PaletteColor("purple", Color.rgb(132, 58, 196)),
        PaletteColor("red", Color.rgb(220, 20, 60)),
        PaletteColor("orange", Color.rgb(255, 140, 0)),
        PaletteColor("yellow", Color.rgb(255, 215, 0))
    )

    private val openPngLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        openPng(uri)
    }

    private val savePngLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null) return@registerForActivityResult
        savePng(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)

        buttonRapidMode = findViewById(R.id.buttonRapidMode)
        val colorPalette: LinearLayout = findViewById(R.id.colorPalette)
        val buttonColorPicker: ImageButton = findViewById(R.id.buttonColorPicker)
        val buttonMenu: ImageButton = findViewById(R.id.buttonMenu)
        sliderBrushSize = findViewById(R.id.sliderBrushSize)
        sliderBrushOpacity = findViewById(R.id.sliderBrushOpacity)

        setupColorPalette(colorPalette)
        buttonColorPicker.setOnClickListener { showColorPickerPopup(it) }

        buttonMenu.setOnClickListener { anchor ->
            showActionsMenu(anchor)
        }

        buttonRapidMode.setOnClickListener {
            toggleRapidOverlay()
        }

        sliderBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setBrushSizeFromPercent(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        sliderBrushOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setBrushOpacityFromPercent(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        installRapidSliderTouchSuppression(sliderBrushSize)
        installRapidSliderTouchSuppression(sliderBrushOpacity)

        drawingView.setBrushSizeFromPercent(sliderBrushSize.progress)
        drawingView.setBrushOpacityFromPercent(sliderBrushOpacity.progress)

        applyRapidUiState(false)
    }

    override fun onResume() {
        super.onResume()
        updateRapidModeButtonText()
    }

    override fun onDestroy() {
        colorPickerPopup?.dismiss()
        drawingView.disableRapidMode()
        super.onDestroy()
    }

    private fun updateRapidModeButtonText() {
        val textRes = if (rapidModeEnabled) {
            R.string.rapid_mode_on
        } else {
            R.string.rapid_mode_off
        }
        drawingView.drawingEnabled = !rapidModeEnabled
        drawingView.viewportGesturesEnabled = true
        buttonRapidMode.setText(textRes)
    }

    private fun toggleRapidOverlay() {
        if (rapidModeEnabled) {
            drawingView.disableRapidMode()
            applyRapidUiState(false)
            return
        }

        val ok = drawingView.setRapidModeEnabled(true)
        if (!ok) {
            toast(R.string.rapid_overlay_not_supported)
            return
        }
        applyRapidUiState(true)
    }

    private fun openPng(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: run {
                    toast(R.string.error_open_png)
                    return
                }
                if (bitmap.width != DrawingView.CANVAS_WIDTH || bitmap.height != DrawingView.CANVAS_HEIGHT) {
                    toast(R.string.error_wrong_size)
                    return
                }
                drawingView.loadBitmap(bitmap)
            }
        } catch (_: IOException) {
            toast(R.string.error_open_png)
        } catch (_: SecurityException) {
            toast(R.string.error_open_png)
        }
    }

    private fun savePng(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "w").use { stream ->
                if (stream == null) {
                    toast(R.string.error_save_png)
                    return
                }
                val ok = drawingView.getBitmapCopy().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                if (!ok) {
                    toast(R.string.error_save_png)
                    return
                }
                toast(R.string.saved_ok)
            }
        } catch (_: IOException) {
            toast(R.string.error_save_png)
        } catch (_: SecurityException) {
            toast(R.string.error_save_png)
        }
    }

    private fun toast(textRes: Int) {
        Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show()
    }

    private fun applyRapidUiState(rapidOn: Boolean) {
        rapidModeEnabled = rapidOn
        if (!rapidOn) drawingView.setRapidInputSuppressed(false)
        sliderBrushSize.forceEinkRefresh = rapidOn
        sliderBrushOpacity.forceEinkRefresh = rapidOn
        sliderBrushSize.einkUpdateMode = UpdateMode.GU_FAST
        sliderBrushOpacity.einkUpdateMode = UpdateMode.GU_FAST
        runCatching {
            if (rapidOn) {
                EpdController.enableScreenUpdate(sliderBrushSize, true)
                EpdController.enableScreenUpdate(sliderBrushOpacity, true)
                EpdController.setViewDefaultUpdateMode(sliderBrushSize, UpdateMode.GU_FAST)
                EpdController.setViewDefaultUpdateMode(sliderBrushOpacity, UpdateMode.GU_FAST)
            } else {
                EpdController.resetViewUpdateMode(sliderBrushSize)
                EpdController.resetViewUpdateMode(sliderBrushOpacity)
            }
        }
        updateRapidModeButtonText()
    }

    private fun installRapidSliderTouchSuppression(slider: VerticalSeekBar) {
        slider.setOnTouchListener { _, event ->
            if (rapidModeEnabled) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> drawingView.setRapidInputSuppressed(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> drawingView.setRapidInputSuppressed(false)
                }
            }
            false
        }
    }

    private fun showActionsMenu(anchor: View) {
        val borderInset = dp(1f)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.menu_popup_background)
            setPadding(borderInset, borderInset, borderInset, borderInset)
        }

        val items = listOf(
            getString(R.string.open_png) to { openPngLauncher.launch(arrayOf("image/png")) },
            getString(R.string.export_png) to { savePngLauncher.launch(getString(R.string.save_filename)) },
            getString(R.string.clear) to { drawingView.clear() },
            getString(R.string.reset_view) to { drawingView.resetView() }
        )

        val popup = PopupWindow(
            content,
            dp(104f),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 0f
            isOutsideTouchable = true
        }

        val horizontalPadding = dp(8f)
        val verticalPadding = dp(7f)
        items.forEachIndexed { index, item ->
            val row = TextView(this).apply {
                text = item.first
                setTextColor(getColorStateList(R.color.menu_item_text_color))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                includeFontPadding = false
                isSingleLine = true
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                background = getDrawable(R.drawable.menu_item_background)
                setOnClickListener {
                    popup.dismiss()
                    item.second.invoke()
                }
            }
            content.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            if (index != items.lastIndex) {
                val divider = View(this).apply {
                    setBackgroundColor(0xFF000000.toInt())
                }
                content.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1f)
                    )
                )
            }
        }

        popup.showAsDropDown(anchor, 0, dp(4f), Gravity.END)
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun setupColorPalette(container: LinearLayout) {
        container.removeAllViews()
        paletteButtons.clear()
        activePaletteColorArgb = drawingView.drawColorArgb

        paletteColors.forEachIndexed { index, paletteColor ->
            val swatch = ImageButton(this).apply {
                background = buildSwatchDrawable(paletteColor.argb, paletteColor.argb == activePaletteColorArgb)
                contentDescription = paletteColor.name
                setOnClickListener {
                    applySelectedColor(paletteColor.argb)
                }
            }

            val params = LinearLayout.LayoutParams(dp(22f), dp(22f)).apply {
                if (index > 0) {
                    marginStart = dp(6f)
                }
            }
            container.addView(swatch, params)
            paletteButtons.add(swatch to paletteColor)
        }
    }

    private fun refreshPaletteSelection() {
        paletteButtons.forEach { (button, paletteColor) ->
            button.background = buildSwatchDrawable(paletteColor.argb, paletteColor.argb == activePaletteColorArgb)
        }
    }

    private fun buildSwatchDrawable(color: Int, selected: Boolean): LayerDrawable {
        val outer = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (selected) Color.BLACK else color)
            setStroke(dp(1f), Color.BLACK)
        }

        val inner = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(1f), if (color == Color.BLACK) Color.WHITE else Color.BLACK)
        }

        return LayerDrawable(arrayOf(outer, inner)).apply {
            val inset = if (selected) dp(3f) else dp(1f)
            setLayerInset(1, inset, inset, inset, inset)
        }
    }

    private fun applySelectedColor(color: Int) {
        val normalized = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
        activePaletteColorArgb = normalized
        drawingView.drawColorArgb = normalized
        refreshPaletteSelection()
    }

    private fun showColorPickerPopup(anchor: View) {
        if (colorPickerPopup?.isShowing == true) {
            colorPickerPopup?.dismiss()
            colorPickerPopup = null
            return
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.color_picker_popup_bg)
            setPadding(dp(8f), dp(6f), dp(8f), dp(8f))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = getString(R.string.pick_color)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        val closeButton = TextView(this).apply {
            text = "\u2715"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(8f), 0, dp(8f), 0)
            setOnClickListener { colorPickerPopup?.dismiss() }
        }
        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(8f)
            }
        )
        header.addView(
            closeButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6f)
            }
        )

        val wheelView = ColorWheelPickerView(this).apply {
            setColor(activePaletteColorArgb)
        }

        content.addView(
            wheelView,
            LinearLayout.LayoutParams(
                dp(150f),
                dp(150f)
            )
        )

        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val preview = View(this).apply {
            setBackgroundColor(activePaletteColorArgb)
        }
        val colorInfo = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(8f), 0, 0, 0)
        }
        infoRow.addView(preview, LinearLayout.LayoutParams(dp(24f), dp(24f)))
        infoRow.addView(
            colorInfo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(
            infoRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8f)
            }
        )

        val updateInfo = { color: Int ->
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            colorInfo.text = String.format(
                Locale.US,
                "#%06X  H:%d S:%d%% B:%d%%",
                (0xFFFFFF and color),
                hsv[0].toInt(),
                (hsv[1] * 100f).toInt(),
                (hsv[2] * 100f).toInt()
            )
            preview.setBackgroundColor(color)
        }
        updateInfo(activePaletteColorArgb)
        wheelView.onColorChanged = { pickedColor ->
            applySelectedColor(pickedColor)
            updateInfo(pickedColor)
        }

        colorPickerPopup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 0f
            isOutsideTouchable = true
            setOnDismissListener { colorPickerPopup = null }
        }

        colorPickerPopup?.showAsDropDown(anchor, 0, dp(4f), Gravity.END)
    }
}
