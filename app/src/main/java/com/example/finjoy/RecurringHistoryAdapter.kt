package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecurringHistoryAdapter : RecyclerView.Adapter<RecurringHistoryAdapter.ViewHolder>() {
    private var items = listOf<HistoryItem>()

    data class HistoryItem(
        val amount: Double,
        val startDate: String,
        val endDate: String?
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val amountText: TextView = view.findViewById(R.id.amountText)
        val dateRangeText: TextView = view.findViewById(R.id.dateRangeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring_history, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.amountText.text = formatCurrency(item.amount)
        holder.dateRangeText.text = if (item.endDate != null) {
            "${item.startDate} → ${item.endDate}"
        } else {
            "From ${item.startDate} onwards"
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", kotlin.math.abs(amount))}"
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}