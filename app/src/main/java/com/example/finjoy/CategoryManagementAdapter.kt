package com.example.finjoy.ui.category

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.Category
import com.example.finjoy.databinding.ItemCategoryManagementBinding

class CategoryManagementAdapter(
    private val categories: List<Category>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryManagementAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemCategoryManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                categoryEmoji.text = category.emoji
                categoryName.text = category.name
                categoryType.text = category.type.toString().capitalize()

                // Setup drag handle touch listener
                dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag(this@ViewHolder)
                    }
                    false
                }

                // Setup delete button
                deleteButton.setOnClickListener {
                    onDelete(category)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}