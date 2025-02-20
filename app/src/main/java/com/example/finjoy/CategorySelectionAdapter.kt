package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategorySelectionAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryEmoji: TextView = view.findViewById(R.id.categoryEmoji)
        val categoryName: TextView = view.findViewById(R.id.categoryNameText)
        val categoryType: TextView = view.findViewById(R.id.categoryTypeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selection, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryEmoji.text = category.emoji
        holder.categoryName.text = category.name
        holder.categoryType.text = category.type.toString().capitalize()
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }
}