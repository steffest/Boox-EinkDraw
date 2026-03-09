package com.boox.einkdraw

import android.app.Dialog
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var penView: HardwarePenSurfaceView
    private lateinit var rootFrame: View
    private lateinit var brushRow: LinearLayout
    private lateinit var widthSeekBar: SeekBar
    private lateinit var widthValueLabel: TextView
    private lateinit var swatchBlack: View
    private lateinit var swatchWhite: View
    private lateinit var swatchBlue: View
    private lateinit var colorPickerPanel: View
    private lateinit var colorPickerView: CircularColorPickerView
    private lateinit var colorHexValue: TextView
    private lateinit var layerPanel: View
    private lateinit var layerDragHandle: View
    private lateinit var layerRecycler: RecyclerView
    private lateinit var fileMenuPanel: View
    private lateinit var buttonLayers: ImageButton
    private lateinit var buttonMenu: ImageButton
    private lateinit var buttonAddLayer: ImageButton
    private lateinit var buttonRemoveLayer: ImageButton
    private lateinit var layerAdapter: LayerListAdapter

    private val brushButtons = LinkedHashMap<HardwarePenStyle, ImageButton>(HardwarePenStyle.entries.size)
    private val brushWidths = HardwarePenStyle.entries.associateWith { it.defaultWidthPx }.toMutableMap()
    private var selectedBrushStyle: HardwarePenStyle = HardwarePenStyle.PENCIL
    private var selectedBrushBtn: ImageButton? = null
    private var selectedColorSwatch: View? = null
    private var pickerInFlight: Boolean = false
    private var activityPaused: Boolean = false
    private var uiTouchDepth: Int = 0
    private var aboutDialogVisible: Boolean = false
    private var layerDragDx: Float = 0f
    private var layerDragDy: Float = 0f
    private var currentInkColor: Int = Color.BLACK
    private var pickerDotColor: Int = Color.BLUE

    private val savePngLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let { savePng(it) }
        pickerInFlight = false
        updateRawSuppression()
    }

    private val loadPngLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { loadPng(it) }
        pickerInFlight = false
        updateRawSuppression()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootFrame = findViewById(R.id.rootFrame)
        penView = findViewById(R.id.penSurfaceView)
        brushRow = findViewById(R.id.brushButtonRow)
        widthSeekBar = findViewById(R.id.widthSeekBar)
        widthValueLabel = findViewById(R.id.widthValueLabel)
        swatchBlack = findViewById(R.id.swatchBlack)
        swatchWhite = findViewById(R.id.swatchWhite)
        swatchBlue = findViewById(R.id.swatchBlue)
        colorPickerPanel = findViewById(R.id.colorPickerPanel)
        colorPickerView = findViewById(R.id.colorPickerView)
        colorHexValue = findViewById(R.id.textColorHex)
        layerPanel = findViewById(R.id.layerPanel)
        layerDragHandle = findViewById(R.id.layerDragHandle)
        layerRecycler = findViewById(R.id.layerRecycler)
        fileMenuPanel = findViewById(R.id.fileMenuPanel)
        buttonLayers = findViewById(R.id.buttonLayers)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonAddLayer = findViewById(R.id.buttonAddLayer)
        buttonRemoveLayer = findViewById(R.id.buttonRemoveLayer)

        val loadBtn = findViewById<View>(R.id.buttonLoad)
        val clearLayerBtn = findViewById<View>(R.id.buttonClearLayer)
        val clearFileBtn = findViewById<View>(R.id.buttonClearFile)
        val saveBtn = findViewById<View>(R.id.buttonSave)
        val aboutBtn = findViewById<View>(R.id.buttonAbout)

        setupBrushButtons()
        setupWidthSeekBar()
        setupColorPickerPanel()
        setupColorSwatches()
        setupLayerPanel()
        setupLayerPanelDrag()

        guardRawMode(widthSeekBar)
        guardRawMode(loadBtn)
        guardRawMode(clearLayerBtn)
        guardRawMode(clearFileBtn)
        guardRawMode(saveBtn)
        guardRawMode(aboutBtn)
        guardRawMode(swatchBlack)
        guardRawMode(swatchWhite)
        guardRawMode(swatchBlue)
        guardRawMode(buttonLayers)
        guardRawMode(buttonMenu)
        guardRawMode(buttonAddLayer)
        guardRawMode(buttonRemoveLayer)
        guardRawMode(layerRecycler)
        guardRawMode(fileMenuPanel)
        guardRawMode(colorPickerPanel)
        guardRawMode(colorPickerView)

        loadBtn.setOnClickListener {
            fileMenuPanel.visibility = View.GONE
            pickerInFlight = true
            updateRawSuppression()
            loadPngLauncher.launch(arrayOf("image/png"))
        }
        clearLayerBtn.setOnClickListener {
            fileMenuPanel.visibility = View.GONE
            val ok = penView.clearCurrentLayer()
            if (!ok) {
                Toast.makeText(this, "No active layer", Toast.LENGTH_SHORT).show()
            }
            refreshLayerPanel()
            updateRawSuppression()
        }
        clearFileBtn.setOnClickListener {
            fileMenuPanel.visibility = View.GONE
            penView.clearFile()
            refreshLayerPanel()
            updateRawSuppression()
        }
        saveBtn.setOnClickListener {
            fileMenuPanel.visibility = View.GONE
            pickerInFlight = true
            updateRawSuppression()
            savePngLauncher.launch("drawing.png")
        }
        aboutBtn.setOnClickListener {
            fileMenuPanel.visibility = View.GONE
            showAboutDialog()
        }
        buttonLayers.setOnClickListener { toggleLayerPanel() }
        buttonMenu.setOnClickListener { toggleFileMenu() }

        selectBrush(HardwarePenStyle.PENCIL)
        refreshLayerPanel()
        ensureOverlayOrder()
        updateRawSuppression()
    }

    private fun setupBrushButtons() {
        HardwarePenStyle.entries.forEach { style ->
            val btn = ImageButton(this).apply {
                setImageResource(brushIconRes(style))
                background = null
                setPadding(dp(2), dp(1), dp(2), dp(1))
                scaleType = ImageView.ScaleType.MATRIX
                contentDescription = style.label
                alpha = 0.45f
                setOnClickListener { selectBrush(style) }
            }
            updateBrushIconTransform(btn, selected = false)
            guardRawMode(btn)
            brushButtons[style] = btn
            brushRow.addView(
                btn,
                LinearLayout.LayoutParams(
                    dp(24),
                    dp(30)
                ).also { it.marginEnd = dp(2) }
            )
        }
    }

    private fun brushIconRes(style: HardwarePenStyle): Int = when (style) {
        HardwarePenStyle.PENCIL -> R.drawable.ic_brush_pencil_tip
        HardwarePenStyle.FOUNTAIN -> R.drawable.ic_brush_fountain_tip
        HardwarePenStyle.MARKER -> R.drawable.ic_brush_marker_tip
        HardwarePenStyle.NEO_BRUSH -> R.drawable.ic_brush_neo_tip
        HardwarePenStyle.CHARCOAL -> R.drawable.ic_brush_charcoal_tip
        HardwarePenStyle.DASH -> R.drawable.ic_brush_dash_tip
        HardwarePenStyle.CHARCOAL_V2 -> R.drawable.ic_brush_charcoal_v2_tip
        HardwarePenStyle.SQUARE_PEN -> R.drawable.ic_brush_square_tip
    }

    private fun setupLayerPanel() {
        layerAdapter = LayerListAdapter(
            onSelect = { layerId ->
                if (penView.setActiveLayer(layerId)) refreshLayerPanel()
            },
            onToggleVisible = { layerId, visible ->
                if (penView.setLayerVisible(layerId, visible)) refreshLayerPanel()
            },
            onOpacityChanged = { layerId, opacity ->
                penView.setLayerOpacity(layerId, opacity)
            }
        )
        layerRecycler.layoutManager = LinearLayoutManager(this)
        layerRecycler.adapter = layerAdapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                layerAdapter.moveItem(from, to)
                penView.moveLayerByDisplayIndices(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                refreshLayerPanel()
            }
        })
        itemTouchHelper.attachToRecyclerView(layerRecycler)

        buttonAddLayer.setOnClickListener {
            penView.addLayer()
            refreshLayerPanel()
        }
        buttonRemoveLayer.setOnClickListener {
            val active = penView.getLayerInfos().firstOrNull { it.active }
            if (active == null) {
                Toast.makeText(this, "No active layer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val removed = penView.removeLayer(active.id)
            if (!removed) {
                Toast.makeText(this, "Cannot remove last layer", Toast.LENGTH_SHORT).show()
            }
            refreshLayerPanel()
        }
    }

    private fun setupLayerPanelDrag() {
        layerDragHandle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    beginUiTouch()
                    layerDragDx = layerPanel.x - event.rawX
                    layerDragDy = layerPanel.y - event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    moveLayerPanel(event.rawX + layerDragDx, event.rawY + layerDragDy)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    endUiTouch(false)
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    endUiTouch(true)
                    true
                }

                else -> false
            }
        }
    }

    private fun moveLayerPanel(targetX: Float, targetY: Float) {
        val parentW = rootFrame.width
        val parentH = rootFrame.height
        if (parentW <= 0 || parentH <= 0) return
        val maxX = (parentW - layerPanel.width).coerceAtLeast(0)
        val maxY = (parentH - layerPanel.height).coerceAtLeast(0)
        layerPanel.x = targetX.coerceIn(0f, maxX.toFloat())
        layerPanel.y = targetY.coerceIn(0f, maxY.toFloat())
        ensureOverlayOrder()
    }

    private fun toggleLayerPanel() {
        val willShow = layerPanel.visibility != View.VISIBLE
        if (willShow) {
            colorPickerPanel.visibility = View.GONE
            refreshPickerToggleSwatch(active = false)
        }
        layerPanel.visibility = if (willShow) View.VISIBLE else View.GONE
        if (willShow) refreshLayerPanel()
        ensureOverlayOrder()
        updateRawSuppression()
    }

    private fun toggleColorPickerPanel() {
        val willShow = colorPickerPanel.visibility != View.VISIBLE
        if (willShow) {
            layerPanel.visibility = View.GONE
            colorPickerView.setColor(pickerDotColor)
            colorHexValue.text = formatHex(pickerDotColor)
        }
        colorPickerPanel.visibility = if (willShow) View.VISIBLE else View.GONE
        refreshPickerToggleSwatch(active = willShow)
        ensureOverlayOrder()
        updateRawSuppression()
    }

    private fun toggleFileMenu() {
        val willShow = fileMenuPanel.visibility != View.VISIBLE
        fileMenuPanel.visibility = if (willShow) View.VISIBLE else View.GONE
        ensureOverlayOrder()
        updateRawSuppression()
    }

    private fun ensureOverlayOrder() {
        layerPanel.bringToFront()
        colorPickerPanel.bringToFront()
        fileMenuPanel.bringToFront()
    }

    private fun refreshLayerPanel() {
        layerAdapter.submit(penView.getLayerInfos())
    }

    private fun selectBrush(style: HardwarePenStyle) {
        selectedBrushStyle = style
        penView.setStyle(style)
        val width = brushWidths[style] ?: style.defaultWidthPx
        penView.setStrokeWidthPx(width)
        widthSeekBar.progress = widthToProgress(width)
        widthValueLabel.text = "${width.roundToInt()} px"

        selectedBrushBtn?.apply {
            alpha = 0.45f
            translationY = 0f
            updateBrushIconTransform(this, selected = false)
        }
        brushButtons[style]?.also { btn ->
            btn.alpha = 1f
            btn.translationY = dp(3).toFloat()
            updateBrushIconTransform(btn, selected = true)
            selectedBrushBtn = btn
        }
    }

    private fun updateBrushIconTransform(button: ImageButton, selected: Boolean) {
        val shiftY = if (selected) dpF(-7f) else dpF(-10f)
        button.imageMatrix = Matrix().apply { setTranslate(0f, shiftY) }
    }

    private fun setupWidthSeekBar() {
        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val width = progressToWidth(progress)
                widthValueLabel.text = "${width.roundToInt()} px"
                brushWidths[selectedBrushStyle] = width
                penView.setStrokeWidthPx(width)
            }

            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
        })
    }

    private fun setupColorPickerPanel() {
        colorPickerView.onColorChanged = { color ->
            applyColor(color, swatch = null, syncPicker = false)
        }
        colorHexValue.text = formatHex(currentInkColor)
        colorPickerView.setColor(pickerDotColor)
    }

    private fun setupColorSwatches() {
        bindColorSwatch(swatchBlack, Color.BLACK)
        bindColorSwatch(swatchWhite, Color.WHITE)
        swatchBlue.setOnClickListener { toggleColorPickerPanel() }
        refreshPickerToggleSwatch(active = false)
        applyColor(currentInkColor, swatch = swatchBlack, syncPicker = false)
    }

    private fun bindColorSwatch(swatch: View, color: Int) {
        swatch.setOnClickListener { applyColor(color, swatch, syncPicker = true) }
        swatch.background = createSwatchDrawable(color, selected = false)
    }

    private fun applyColor(color: Int, swatch: View?, syncPicker: Boolean) {
        currentInkColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
        penView.setStrokeColor(color)
        val previous = selectedColorSwatch
        previous?.background = createSwatchDrawable(selectedInkColorOf(previous), selected = false)
        if (swatch != null) {
            swatch.background = createSwatchDrawable(selectedInkColorOf(swatch), selected = true)
        }
        selectedColorSwatch = swatch
        if (swatch == null) {
            pickerDotColor = currentInkColor
        }
        colorHexValue.text = formatHex(currentInkColor)
        refreshPickerToggleSwatch(active = colorPickerPanel.visibility == View.VISIBLE)
        if (syncPicker) {
            colorPickerView.setColor(currentInkColor)
        }
    }

    private fun selectedInkColorOf(swatch: View?): Int = when (swatch?.id) {
        R.id.swatchWhite -> Color.WHITE
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

    private fun createPickerToggleDrawable(active: Boolean, fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            setStroke(if (active) dp(2) else dp(1), if (active) Color.WHITE else Color.LTGRAY)
        }
    }

    private fun refreshPickerToggleSwatch(active: Boolean) {
        swatchBlue.background = createPickerToggleDrawable(active = active, fillColor = pickerDotColor)
    }

    private fun formatHex(color: Int): String =
        String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))

    private fun progressToWidth(progress: Int): Float {
        val t = progress / 99f
        return 1f + t * t * 79f
    }

    private fun widthToProgress(width: Float): Int {
        val t = ((width - 1f) / 79f).coerceIn(0f, 1f)
        return (sqrt(t.toDouble()) * 99).roundToInt()
    }

    private fun savePng(uri: Uri) {
        val bmp = penView.exportBitmap() ?: run {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
            return
        }
        val ok = runCatching {
            contentResolver.openOutputStream(uri, "w")?.use {
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            } ?: false
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

    private fun beginUiTouch() {
        uiTouchDepth += 1
        updateRawSuppression()
    }

    private fun endUiTouch(reset: Boolean) {
        uiTouchDepth = if (reset) 0 else (uiTouchDepth - 1).coerceAtLeast(0)
        updateRawSuppression()
    }

    private fun guardRawMode(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> beginUiTouch()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> endUiTouch(false)
                MotionEvent.ACTION_CANCEL -> endUiTouch(true)
            }
            false
        }
    }

    private fun updateRawSuppression() {
        val suppress = activityPaused ||
            pickerInFlight ||
            aboutDialogVisible ||
            uiTouchDepth > 0 ||
            layerPanel.visibility == View.VISIBLE ||
            colorPickerPanel.visibility == View.VISIBLE ||
            fileMenuPanel.visibility == View.VISIBLE
        penView.setRawInputSuppressed(suppress)
    }

    private fun showAboutDialog() {
        aboutDialogVisible = true
        updateRawSuppression()

        val content = layoutInflater.inflate(R.layout.dialog_about, null)
        val imageView = content.findViewById<android.widget.ImageView>(R.id.aboutImage)
        val textColumn = content.findViewById<LinearLayout>(R.id.aboutTextColumn)
        val linkView = content.findViewById<TextView>(R.id.aboutLink)
        val okButton = content.findViewById<TextView>(R.id.aboutOkButton)
        val linkText = SpannableStringBuilder("Open source - fork me on Github").apply {
            val start = lastIndexOf("Github")
            if (start >= 0) {
                val end = start + "Github".length
                setSpan(
                    URLSpan("https://github.com/steffest/Boox-EinkDraw"),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        linkView.text = linkText
        linkView.movementMethod = LinkMovementMethod.getInstance()

        val dialog = Dialog(this, R.style.Theme_BooxEinkDraw_AboutDialog).apply {
            setContentView(content)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        okButton.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            aboutDialogVisible = false
            updateRawSuppression()
        }
        dialog.show()
        content.post {
            val side = textColumn.height.coerceAtLeast(dp(96))
            val lp = imageView.layoutParams
            if (lp.width != side || lp.height != side) {
                lp.width = side
                lp.height = side
                imageView.layoutParams = lp
            }
            dialog.window?.setLayout(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                if (dismissPanelsIfTappedOutside(ev.rawX, ev.rawY)) return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun dismissPanelsIfTappedOutside(rawX: Float, rawY: Float): Boolean {
        var dismissed = false

        if (layerPanel.visibility == View.VISIBLE &&
            !isPointInsideView(layerPanel, rawX, rawY) &&
            !isPointInsideView(buttonLayers, rawX, rawY)
        ) {
            layerPanel.visibility = View.GONE
            dismissed = true
        }

        if (fileMenuPanel.visibility == View.VISIBLE &&
            !isPointInsideView(fileMenuPanel, rawX, rawY) &&
            !isPointInsideView(buttonMenu, rawX, rawY)
        ) {
            fileMenuPanel.visibility = View.GONE
            dismissed = true
        }

        if (colorPickerPanel.visibility == View.VISIBLE &&
            !isPointInsideView(colorPickerPanel, rawX, rawY) &&
            !isPointInsideView(swatchBlue, rawX, rawY)
        ) {
            colorPickerPanel.visibility = View.GONE
            dismissed = true
        }

        if (dismissed) {
            refreshPickerToggleSwatch(active = colorPickerPanel.visibility == View.VISIBLE)
            updateRawSuppression()
            refreshUiAfterOverlayDismiss()
        }
        return dismissed
    }

    private fun refreshUiAfterOverlayDismiss() {
        rootFrame.post {
            rootFrame.invalidate()
            penView.invalidate()
        }
    }

    private fun isPointInsideView(view: View, rawX: Float, rawY: Float): Boolean {
        val r = Rect()
        view.getGlobalVisibleRect(r)
        return r.contains(rawX.roundToInt(), rawY.roundToInt())
    }

    override fun onPause() {
        super.onPause()
        activityPaused = true
        updateRawSuppression()
    }

    override fun onResume() {
        super.onResume()
        activityPaused = false
        updateRawSuppression()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun dpF(v: Float): Float = v * resources.displayMetrics.density
}
