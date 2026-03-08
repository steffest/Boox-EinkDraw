package com.boox.einkdraw

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
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

    private var selectedBrushBtn: Button? = null

    private val savePngLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri -> uri?.let { savePng(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        penView = findViewById(R.id.penSurfaceView)
        brushRow = findViewById(R.id.brushButtonRow)
        widthSeekBar = findViewById(R.id.widthSeekBar)
        widthValueLabel = findViewById(R.id.widthValueLabel)

        val clearBtn = findViewById<Button>(R.id.buttonClear)
        val saveBtn = findViewById<Button>(R.id.buttonSave)

        setupBrushButtons()
        setupWidthSeekBar()

        // Raw-mode guard on every UI control: suppresses the hardware overlay while the user
        // touches a button or slider, so the E-ink screen can repaint the UI feedback.
        guardRawMode(widthSeekBar)
        guardRawMode(clearBtn)
        guardRawMode(saveBtn)

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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
