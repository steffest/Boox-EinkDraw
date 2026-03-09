package com.boox.einkdraw

import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var penView: HardwarePenSurfaceView
    private lateinit var brushRow: LinearLayout
    private lateinit var widthSeekBar: SeekBar
    private lateinit var widthValueLabel: TextView
    private lateinit var swatchBlack: View
    private lateinit var swatchWhite: View
    private lateinit var swatchBlue: View

    private var selectedBrushBtn: Button? = null
    private var selectedColorSwatch: View? = null
    private var selectedInkColor: Int = Color.BLACK
    private var pickerInFlight: Boolean = false

    private val savePngLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri -> uri?.let { savePng(it) } }

    private val loadPngLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { loadPng(it) }
        pickerInFlight = false
        penView.setRawInputSuppressed(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        penView = findViewById(R.id.penSurfaceView)
        brushRow = findViewById(R.id.brushButtonRow)
        widthSeekBar = findViewById(R.id.widthSeekBar)
        widthValueLabel = findViewById(R.id.widthValueLabel)
        swatchBlack = findViewById(R.id.swatchBlack)
        swatchWhite = findViewById(R.id.swatchWhite)
        swatchBlue = findViewById(R.id.swatchBlue)

        val loadBtn = findViewById<Button>(R.id.buttonLoad)
        val clearBtn = findViewById<Button>(R.id.buttonClear)
        val saveBtn = findViewById<Button>(R.id.buttonSave)

        setupBrushButtons()
        setupWidthSeekBar()
        setupColorSwatches()

        // Raw-mode guard on every UI control: suppresses the hardware overlay while the user
        // touches a button or slider, so the E-ink screen can repaint the UI feedback.
        guardRawMode(widthSeekBar)
        guardRawMode(loadBtn)
        guardRawMode(clearBtn)
        guardRawMode(saveBtn)
        guardRawMode(swatchBlack)
        guardRawMode(swatchWhite)
        guardRawMode(swatchBlue)

        loadBtn.setOnClickListener {
            pickerInFlight = true
            penView.setRawInputSuppressed(true)
            loadPngLauncher.launch(arrayOf("image/png"))
        }
        clearBtn.setOnClickListener { penView.clearCanvas() }
        saveBtn.setOnClickListener { savePngLauncher.launch("drawing.png") }

        // Start with Pencil selected
        selectBrush(HardwarePenStyle.PENCIL)
    }

    // ─── Brush buttons ────────────────────────────────────────────────────────

    private fun setupBrushButtons() {
        HardwarePenStyle.entries.forEach { style ->
            val btn = Button(this).apply {
                text = style.label
                isAllCaps = false
                minHeight = 0; minimumHeight = 0
                textSize = 12f
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener { selectBrush(style) }
            }
            guardRawMode(btn)
            brushRow.addView(btn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).also { it.marginEnd = dp(4) })
        }
    }

    private fun selectBrush(style: HardwarePenStyle) {
        penView.setStyle(style)

        // Snap width to the style's default when switching brushes
        val width = style.defaultWidthPx
        penView.setStrokeWidthPx(width)
        widthSeekBar.progress = widthToProgress(width)
        widthValueLabel.text = "${width.roundToInt()} px"

        // Highlight the correct button
        selectedBrushBtn?.apply { alpha = 0.55f; typeface = Typeface.DEFAULT }
        val idx = HardwarePenStyle.entries.indexOf(style)
        (brushRow.getChildAt(idx) as? Button)?.also { btn ->
            btn.alpha = 1f; btn.typeface = Typeface.DEFAULT_BOLD
            selectedBrushBtn = btn
        }
    }

    // ─── Width slider ─────────────────────────────────────────────────────────

    private fun setupWidthSeekBar() {
        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val width = progressToWidth(progress)
                widthValueLabel.text = "${width.roundToInt()} px"
                penView.setStrokeWidthPx(width)
            }
            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
        })
    }

    // ─── Color swatches ──────────────────────────────────────────────────────

    private fun setupColorSwatches() {
        bindColorSwatch(swatchBlack, Color.BLACK)
        bindColorSwatch(swatchWhite, Color.WHITE)
        bindColorSwatch(swatchBlue, Color.BLUE)
        selectColor(Color.BLACK, swatchBlack)
    }

    private fun bindColorSwatch(swatch: View, color: Int) {
        swatch.setOnClickListener { selectColor(color, swatch) }
        swatch.background = createSwatchDrawable(color, selected = false)
    }

    private fun selectColor(color: Int, swatch: View) {
        selectedInkColor = color
        penView.setStrokeColor(color)
        val previous = selectedColorSwatch
        previous?.background = createSwatchDrawable(selectedInkColorOf(previous), selected = false)
        swatch.background = createSwatchDrawable(color, selected = true)
        selectedColorSwatch = swatch
    }

    private fun selectedInkColorOf(swatch: View?): Int = when (swatch?.id) {
        R.id.swatchWhite -> Color.WHITE
        R.id.swatchBlue -> Color.BLUE
        else -> Color.BLACK
    }

    private fun createSwatchDrawable(fill: Int, selected: Boolean): GradientDrawable {
        val ringColor = when {
            selected && fill == Color.WHITE -> Color.BLACK
            selected -> Color.WHITE
            fill == Color.WHITE -> Color.DKGRAY
            else -> Color.LTGRAY
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke(if (selected) dp(2) else dp(1), ringColor)
        }
    }

    // SeekBar 0–99 → 2 px to 80 px (quadratic so fine control at small widths)
    private fun progressToWidth(progress: Int): Float {
        val t = progress / 99f
        return 2f + t * t * 78f
    }

    private fun widthToProgress(width: Float): Int {
        val t = ((width - 2f) / 78f).coerceIn(0f, 1f)
        return (sqrt(t.toDouble()) * 99).roundToInt()
    }

    // ─── Save PNG ─────────────────────────────────────────────────────────────

    private fun savePng(uri: Uri) {
        val bmp = penView.exportBitmap() ?: run {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            return
        }
        val ok = runCatching {
            contentResolver.openOutputStream(uri, "w")?.use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } ?: false
        }.getOrDefault(false)
        Toast.makeText(this, if (ok) "Saved" else "Save failed", Toast.LENGTH_SHORT).show()
    }

    private fun loadPng(uri: Uri) {
        val bitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        }.getOrNull()

        if (bitmap == null) {
            Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show()
            return
        }
        val ok = penView.loadCanvasBitmap(bitmap)
        bitmap.recycle()
        Toast.makeText(this, if (ok) "Loaded" else "Load failed", Toast.LENGTH_SHORT).show()
    }

    // ─── Raw-mode guard ───────────────────────────────────────────────────────

    /**
     * Temporarily suspends the hardware pen overlay while a UI control is being touched,
     * so Android can repaint the E-ink display to show button highlights etc.
     */
    private fun guardRawMode(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                    penView.setRawInputSuppressed(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                    penView.setRawInputSuppressed(false)
            }
            false  // don't consume — let the control handle the click/drag normally
        }
    }

    override fun onPause() {
        super.onPause()
        penView.setRawInputSuppressed(true)
    }

    override fun onResume() {
        super.onResume()
        if (!pickerInFlight) {
            penView.setRawInputSuppressed(false)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
