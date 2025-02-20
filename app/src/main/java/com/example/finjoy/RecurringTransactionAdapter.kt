package com.example.finjoy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.databinding.ItemRecurringTransactionBinding
import kotlin.math.abs

class RecurringTransactionAdapter(
    private val onItemClick: (RecurringTransactionWithCategory) -> Unit
) : RecyclerView.Adapter<RecurringTransactionAdapter.ViewHolder>() {

    private var items = listOf<RecurringTransactionWithCategory>()

    class ViewHolder(
        private val binding: ItemRecurringTransactionBinding,
        private val onItemClick: (RecurringTransactionWithCategory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecurringTransactionWithCategory) {
            binding.apply {
                nameText.text = item.name
                categoryEmoji.text = item.categoryEmoji
                categoryText.text = item.categoryName
                amountText.apply {
                    val prefix = if (item.amount >= 0) "+" else "-"
                    text = "$prefix${String.format("%.2f", abs(item.amount))}"
                    setTextColor(ContextCompat.getColor(context,
                        if (item.amount >= 0) R.color.green else R.color.red))
                }
                daysText.text = "Every ${item.daysOfMonth} of the month"
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRecurringTransactionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<RecurringTransactionWithCategory>) {
        items = newItems
        notifyDataSetChanged()
    }
}
