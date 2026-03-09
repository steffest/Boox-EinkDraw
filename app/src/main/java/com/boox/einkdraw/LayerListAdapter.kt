package com.boox.einkdraw

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class LayerListAdapter(
    private val onSelect: (layerId: Int) -> Unit,
    private val onToggleVisible: (layerId: Int, visible: Boolean) -> Unit,
    private val onOpacityChanged: (layerId: Int, opacity: Float) -> Unit,
) : RecyclerView.Adapter<LayerListAdapter.LayerViewHolder>() {

    private val items = ArrayList<HardwarePenSurfaceView.LayerInfo>()

    init {
        setHasStableIds(true)
    }

    fun submit(list: List<HardwarePenSurfaceView.LayerInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices || from == to) return
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
    }

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layer_row, parent, false)
        return LayerViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        val item = items[position]
        holder.layerName.text = item.name
        holder.activeRadio.isChecked = item.active
        holder.opacityLabel.text = "${(item.opacity * 100f).roundToInt()}%"

        holder.visibilityButton.setImageResource(
            if (item.visible) R.drawable.ic_eye_open_black_24
            else R.drawable.ic_eye_closed_black_24
        )

        holder.seekOpacity.setOnSeekBarChangeListener(null)
        holder.seekOpacity.progress = (item.opacity * 100f).roundToInt().coerceIn(0, 100)
        holder.seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                holder.opacityLabel.text = "$progress%"
                onOpacityChanged(item.id, progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        val selectAction = View.OnClickListener { onSelect(item.id) }
        holder.itemView.setOnClickListener(selectAction)
        holder.activeRadio.setOnClickListener(selectAction)

        holder.visibilityButton.setOnClickListener {
            onToggleVisible(item.id, !item.visible)
        }
    }

    class LayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activeRadio: RadioButton = view.findViewById(R.id.radioActive)
        val layerName: TextView = view.findViewById(R.id.textLayerName)
        val visibilityButton: ImageButton = view.findViewById(R.id.buttonVisibility)
        val seekOpacity: SeekBar = view.findViewById(R.id.seekOpacity)
        val opacityLabel: TextView = view.findViewById(R.id.textOpacityValue)
    }
}
