package com.example.external.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.external.R
import com.google.android.material.card.MaterialCardView
import java.util.Random

class SubcategoryAdapter(
    private val onSubcategorySelected: (String) -> Unit
) : ListAdapter<String, SubcategoryAdapter.SubcategoryViewHolder>(SubcategoryDiffCallback()) {

    private var selectedPosition = -1 // -1 means no selection (show all)
    private val colorMap = mutableMapOf<String, Int>()
    private val random = Random(System.currentTimeMillis())

    class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.categoryCard)
        val subcategoryName: TextView = itemView.findViewById(R.id.categoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return SubcategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        val subcategory = getItem(position)
        holder.subcategoryName.text = subcategory
        
        // Generate a consistent color for this subcategory
        if (!colorMap.containsKey(subcategory)) {
            if (subcategory == "All") {
                colorMap[subcategory] = Color.LTGRAY
            } else {
                val color = Color.HSVToColor(floatArrayOf(
                    random.nextFloat() * 360,
                    0.3f + random.nextFloat() * 0.3f,
                    0.9f
                ))
                colorMap[subcategory] = color
            }
        }

        // Set selected state
        val isSelected = position == selectedPosition
        holder.cardView.strokeColor = if (isSelected) {
            colorMap[subcategory] ?: Color.GRAY
        } else {
            Color.LTGRAY
        }
        
        holder.cardView.strokeWidth = if (isSelected) 3 else 1
        holder.subcategoryName.setTextColor(if (isSelected) colorMap[subcategory] ?: Color.BLACK else Color.BLACK)

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == holder.adapterPosition) {
                -1 // Deselect if already selected
            } else {
                holder.adapterPosition
            }
            
            if (previousSelected >= 0) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
            onSubcategorySelected(if (selectedPosition >= 0) subcategory else "")
        }
    }

    fun clearSelection() {
        val previousSelected = selectedPosition
        selectedPosition = -1
        if (previousSelected >= 0) {
            notifyItemChanged(previousSelected)
        }
    }

    private class SubcategoryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
} 