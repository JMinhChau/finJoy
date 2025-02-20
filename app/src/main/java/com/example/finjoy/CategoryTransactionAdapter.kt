package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class CategoryTransactionAdapter :
    RecyclerView.Adapter<CategoryTransactionAdapter.ViewHolder>() {

    private var items = listOf<TransactionWithCategory>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.description)
        val amount: TextView = view.findViewById(R.id.amount)
        val date: TextView = view.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = items[position]
        holder.description.text = transaction.description
        holder.amount.text = formatCurrency(transaction.amount)
        holder.date.text = transaction.date.format(DateTimeFormatter.ofPattern("MMM d"))

        holder.amount.setTextColor(
            ContextCompat.getColor(holder.itemView.context,
                if (transaction.amount >= 0) R.color.green else R.color.red)
        )
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", abs(amount))}"
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<TransactionWithCategory>) {
        items = newItems
        notifyDataSetChanged()
    }
}