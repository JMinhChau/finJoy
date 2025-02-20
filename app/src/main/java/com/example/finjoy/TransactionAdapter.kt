package com.example.finjoy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class TransactionAdapter(
    private val onTransactionClick: (TransactionWithCategory) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items = listOf<TransactionListItem>()
    private val VIEW_TYPE_DATE = 0
    private val VIEW_TYPE_TRANSACTION = 1

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val totalAmount: TextView = view.findViewById(R.id.totalAmount)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.category)
        val emoji: TextView = view.findViewById(R.id.categoryEmoji)
        val amount: TextView = view.findViewById(R.id.amount)
        val description: TextView = view.findViewById(R.id.description)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionListItem.DateHeader -> VIEW_TYPE_DATE
            is TransactionListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.transaction_layout, parent, false)
                TransactionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.DateHeader -> {
                holder as DateViewHolder
                val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
                holder.dateText.text = item.date.format(formatter)
                holder.totalAmount.apply {
                    text = formatCurrency(item.totalAmount)
                    setTextColor(ContextCompat.getColor(
                        context,
                        if (item.totalAmount >= 0) R.color.green else R.color.red
                    ))
                }
            }
            is TransactionListItem.TransactionItem -> {
                holder as TransactionViewHolder
                val transaction = item.transaction

                holder.category.text = transaction.categoryName
                holder.emoji.text = transaction.categoryEmoji
                holder.amount.apply {
                    text = formatCurrency(transaction.amount)
                    setTextColor(ContextCompat.getColor(
                        context,
                        if (transaction.amount >= 0) R.color.green else R.color.red
                    ))
                }

                if (transaction.description.isNotBlank()) {
                    holder.description.apply {
                        text = transaction.description
                        visibility = View.VISIBLE
                    }
                } else {
                    holder.description.visibility = View.GONE
                }

                holder.itemView.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    private fun formatCurrency(amount: Double): String {
        val prefix = if (amount >= 0) "+" else ""
        return "$prefix$${String.format("%.2f", kotlin.math.abs(amount))}"
    }

    fun submitList(transactions: List<TransactionWithCategory>) {
        val groupedItems = mutableListOf<TransactionListItem>()

        // Group transactions by date
        val groupedByDate = transactions.groupBy { it.date }

        // For each date group, sorted by most recent first
        groupedByDate.entries.sortedByDescending { it.key }.forEach { (date, transactionsForDate) ->
            // Add date header with daily total
            val dailyTotal = transactionsForDate.sumOf { it.amount }
            groupedItems.add(TransactionListItem.DateHeader(date, dailyTotal, transactionsForDate))

            // Add all transactions for that date
            transactionsForDate.forEach { transaction ->
                groupedItems.add(TransactionListItem.TransactionItem(transaction))
            }
        }

        items = groupedItems
        notifyDataSetChanged()
    }
}