package com.example.external.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.external.R
import com.google.android.material.card.MaterialCardView
import java.util.Random

class CategoryAdapter(
    private val onCategorySelected: (String) -> Unit
) : ListAdapter<String, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedPosition = 0
    private val colorMap = mutableMapOf<String, Int>()
    private val random = Random(System.currentTimeMillis())

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.categoryCard)
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.categoryName.text = category
        
        // Generate a consistent color for this category
        if (!colorMap.containsKey(category)) {
            if (category == "All") {
                colorMap[category] = Color.GRAY
            } else {
                val color = Color.HSVToColor(floatArrayOf(
                    random.nextFloat() * 360,
                    0.5f + random.nextFloat() * 0.5f,
                    0.8f + random.nextFloat() * 0.2f
                ))
                colorMap[category] = color
            }
        }

        // Set selected state
        val isSelected = position == selectedPosition
        holder.cardView.strokeColor = if (isSelected) {
            colorMap[category] ?: Color.GRAY
        } else {
            Color.LTGRAY
        }
        
        holder.cardView.strokeWidth = if (isSelected) 3 else 1
        holder.categoryName.setTextColor(if (isSelected) colorMap[category] ?: Color.BLACK else Color.BLACK)

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onCategorySelected(category)
        }
    }

    fun getColorForCategory(category: String): Int {
        return colorMap[category] ?: Color.GRAY
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
} 