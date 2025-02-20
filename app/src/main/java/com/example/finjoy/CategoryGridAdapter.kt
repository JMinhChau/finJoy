package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryGridAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_CATEGORY = 0
    private val VIEW_TYPE_ADD = 1
    private val sortedCategories = categories.sortedBy { it.displayOrder }

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.categoryEmoji)
        val name: TextView = view.findViewById(R.id.categoryName)
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_grid, parent, false)
                CategoryViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_grid_add, parent, false)
                AddViewHolder(view)
            }
        }
    }

    override fun getItemCount() = categories.size + 1 // +1 for the Add button

    override fun getItemViewType(position: Int): Int {
        return if (position == categories.size) VIEW_TYPE_ADD else VIEW_TYPE_CATEGORY
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_CATEGORY -> {
                val categoryHolder = holder as CategoryViewHolder
                val category = sortedCategories[position]  // Use sorted list
                categoryHolder.emoji.text = category.emoji
                categoryHolder.name.text = category.name
                holder.itemView.setOnClickListener { onCategoryClick(category) }
            }
            VIEW_TYPE_ADD -> {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}