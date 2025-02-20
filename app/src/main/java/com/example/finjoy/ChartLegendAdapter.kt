package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChartLegendAdapter : RecyclerView.Adapter<ChartLegendAdapter.ViewHolder>() {
    private var items = listOf<LegendItem>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val color: View = view.findViewById(R.id.colorIndicator)
        val category: TextView = view.findViewById(R.id.categoryName)
        val amount: TextView = view.findViewById(R.id.amount)
        val percentage: TextView = view.findViewById(R.id.percentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chart_legend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.color.setBackgroundColor(item.color)
        holder.category.text = item.category
        holder.amount.text = item.amount
        holder.percentage.text = item.percentage
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<LegendItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    data class LegendItem(
        val category: String,
        val amount: String,
        val percentage: String,
        val color: Int
    )
}