package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class TopExpensesAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<TopExpensesAdapter.ViewHolder>() {
    private var items = listOf<CategoryExpense>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.category)
        val amount: TextView = view.findViewById(R.id.amount)
        val percentage: TextView = view.findViewById(R.id.percentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.apply {
            category.text = item.categoryName
            amount.text = "-$${String.format("%.2f", item.amount)}"
            percentage.text = String.format("%.1f%%", item.percentage)

            itemView.setOnClickListener {
                onItemClick(item.categoryName)
            }
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<CategoryExpense>) {
        items = newItems
        notifyDataSetChanged()
    }

    data class CategoryExpense(
        val categoryName: String,
        val amount: Double,
        val percentage: Double
    )
}