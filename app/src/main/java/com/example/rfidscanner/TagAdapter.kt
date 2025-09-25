package com.example.rfidscanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidscanner.model.RFIDTag
import java.text.SimpleDateFormat
import java.util.*

class TagAdapter(private var tags: List<RFIDTag>) : RecyclerView.Adapter<TagAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val epcShort: TextView = itemView.findViewById(R.id.epcShort)
        val rssi: TextView = itemView.findViewById(R.id.rssi)
        val seenCount: TextView = itemView.findViewById(R.id.seenCount)
        val detailsLayout: View = itemView.findViewById(R.id.detailsLayout)
        val epcFull: TextView = itemView.findViewById(R.id.epcFull)
        val firstSeen: TextView = itemView.findViewById(R.id.firstSeen)
        val lastSeen: TextView = itemView.findViewById(R.id.lastSeen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tag_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tag = tags[position]
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        holder.epcShort.text = if (tag.epc.length > 12) "${tag.epc.substring(0, 12)}..." else tag.epc
        holder.rssi.text = tag.rssi?.toString() ?: "N/A"
        holder.seenCount.text = tag.seenCount.toString()
        
        // Expanded details
        holder.epcFull.text = tag.epc
        holder.firstSeen.text = dateFormat.format(tag.firstSeen)
        holder.lastSeen.text = dateFormat.format(tag.lastSeen)
        
        val isExpanded = expandedPositions.contains(position)
        holder.detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = tags.size

    fun updateTags(newTags: List<RFIDTag>) {
        this.tags = newTags.sortedByDescending { it.lastSeen }
        expandedPositions.clear()
        notifyDataSetChanged()
    }
    fun containsTag(epc: String): Boolean {
        return tags.any { it.epc == epc }
    }
}