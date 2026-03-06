package com.boox.einkdraw

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var buttonColor: Button
    private lateinit var buttonRapidMode: Button
    private var rapidModeEnabled = false

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

        buttonColor = findViewById(R.id.buttonColor)
        buttonRapidMode = findViewById(R.id.buttonRapidMode)
        val buttonOpen: Button = findViewById(R.id.buttonOpen)
        val buttonExport: Button = findViewById(R.id.buttonExport)
        val buttonClear: Button = findViewById(R.id.buttonClear)
        val buttonResetView: Button = findViewById(R.id.buttonResetView)
        val sliderBrushSize: SeekBar = findViewById(R.id.sliderBrushSize)
        val sliderBrushOpacity: SeekBar = findViewById(R.id.sliderBrushOpacity)

        buttonColor.setOnClickListener {
            drawingView.drawColor = when (drawingView.drawColor) {
                DrawColor.BLACK -> DrawColor.WHITE
                DrawColor.WHITE -> DrawColor.BLACK
            }
            updateColorButtonText()
        }

        buttonOpen.setOnClickListener {
            openPngLauncher.launch(arrayOf("image/png"))
        }

        buttonExport.setOnClickListener {
            savePngLauncher.launch(getString(R.string.save_filename))
        }

        buttonClear.setOnClickListener {
            drawingView.clear()
        }

        buttonResetView.setOnClickListener {
            drawingView.resetView()
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

        drawingView.setBrushSizeFromPercent(sliderBrushSize.progress)
        drawingView.setBrushOpacityFromPercent(sliderBrushOpacity.progress)

        updateColorButtonText()
        applyRapidUiState(false)
    }

    override fun onResume() {
        super.onResume()
        updateRapidModeButtonText()
    }

    override fun onDestroy() {
        drawingView.disableRapidMode()
        super.onDestroy()
    }

    private fun updateColorButtonText() {
        val textRes = when (drawingView.drawColor) {
            DrawColor.BLACK -> R.string.color_black
            DrawColor.WHITE -> R.string.color_white
        }
        buttonColor.setText(textRes)
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
        updateRapidModeButtonText()
    }
}
