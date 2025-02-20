package com.example.finjoy.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.AppDatabase
import com.example.finjoy.CategoryTransactionAdapter
import com.example.finjoy.ChartLegendAdapter
import com.example.finjoy.ColorUtils
import com.example.finjoy.R
import com.example.finjoy.TopExpensesAdapter
import com.example.finjoy.TransactionWithCategory
import com.example.finjoy.databinding.FragmentReportBinding
import com.example.finjoy.ui.chart.PieChartView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var chartLegendAdapter: ChartLegendAdapter
    private lateinit var topExpensesAdapter: TopExpensesAdapter
    private var startDate = LocalDate.now().withDayOfMonth(1)
    private var endDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())
        setupViews()
        setupLegendRecyclerView()
        loadTransactions()
    }

    private fun setupViews() {
        // Setup date pickers
        updateDateDisplay()
        binding.startDateInput.setOnClickListener { showDatePicker(true) }
        binding.endDateInput.setOnClickListener { showDatePicker(false) }

        // Setup expenses list with click handling
        setupExpensesList()

        // Setup legend
        setupLegendRecyclerView()
    }

    private fun setupExpensesList() {
        topExpensesAdapter = TopExpensesAdapter { categoryName ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val transactions = db.transactionDao().getTransactionsWithCategory()
                    val categoryTransactions = filterTransactionsByDate(transactions)
                        .filter { it.categoryName == categoryName }
                        .distinctBy { it.description } // Remove duplicates from recurring
                        .sortedByDescending { abs(it.amount) }
                        .take(3) // Top 3 transactions

                    withContext(Dispatchers.Main) {
                        showCategoryDetails(categoryName, categoryTransactions)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.topExpensesList.apply {
            adapter = topExpensesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupLegendRecyclerView() {
        chartLegendAdapter = ChartLegendAdapter()
        binding.chartLegend.apply {
            adapter = chartLegendAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStartDate) "Select Start Date" else "Select End Date")
            .setSelection(
                (if (isStartDate) startDate else endDate)
                    .toEpochDay() * 24 * 60 * 60 * 1000
            )
            .build()
            .apply {
                addOnPositiveButtonClickListener { timeInMillis ->
                    val selectedDate = Instant.ofEpochMilli(timeInMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    if (isStartDate) {
                        startDate = selectedDate
                        if (startDate.isAfter(endDate)) endDate = startDate
                    } else {
                        endDate = selectedDate
                        if (endDate.isBefore(startDate)) startDate = endDate
                    }

                    updateDateDisplay()
                    loadTransactions()
                }
            }.show(childFragmentManager, "datePicker")
    }

    private fun updateDateDisplay() {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        binding.startDateInput.setText(startDate.format(formatter))
        binding.endDateInput.setText(endDate.format(formatter))
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactions = db.transactionDao().getTransactionsWithCategory()
                val filtered = filterTransactionsByDate(transactions)
                withContext(Dispatchers.Main) {
                    updateCharts(filtered)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterTransactionsByDate(transactions: List<TransactionWithCategory>): List<TransactionWithCategory> {
        return transactions.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate)
        }
    }

    private fun updateCharts(transactions: List<TransactionWithCategory>) {
        val expenses = transactions.filter { it.amount < 0 }
        val income = transactions.filter { it.amount > 0 }  // Add this back
        val totalExpense = abs(expenses.sumOf { it.amount })
        val totalFlow = totalExpense + income.sumOf { it.amount }  // Total for pie chart percentages

        val pieData = mutableListOf<PieChartView.Slice>()
        val legendData = mutableListOf<ChartLegendAdapter.LegendItem>()

        // Add income data to pie chart
        income.groupBy { it.categoryName }
            .forEach { (category, transactions) ->
                val amount = transactions.sumOf { it.amount }
                val percentage = (amount / totalFlow * 100).toFloat()
                val color = ContextCompat.getColor(requireContext(), R.color.green)

                pieData.add(PieChartView.Slice(category, percentage, color))
                legendData.add(ChartLegendAdapter.LegendItem(
                    category = category,
                    amount = "+${formatCurrency(amount)}",
                    percentage = String.format("%.1f%%", percentage),
                    color = color
                ))
            }

        // Add expense data to pie chart
        expenses.groupBy { it.categoryName }
            .forEach { (category, transactions) ->
                val amount = abs(transactions.sumOf { it.amount })
                val percentage = (amount / totalFlow * 100).toFloat()
                // Get color using categoryType from first transaction in group
                val color = ColorUtils.getCategoryColor(
                    requireContext(),
                    category,
                    transactions.first().categoryType
                )

                pieData.add(PieChartView.Slice(category, percentage, color))
                legendData.add(ChartLegendAdapter.LegendItem(
                    category = category,
                    amount = "-${formatCurrency(amount)}",
                    percentage = String.format("%.1f%%", percentage),
                    color = color
                ))
            }

        binding.pieChart.setData(pieData)
        chartLegendAdapter.submitList(legendData)

        // Only update expense list
        val topExpenses = expenses
            .groupBy { it.categoryName }
            .map { (category, categoryTransactions) ->
                TopExpensesAdapter.CategoryExpense(
                    categoryName = category,
                    amount = abs(categoryTransactions.sumOf { it.amount }),
                    percentage = (abs(categoryTransactions.sumOf { it.amount }) / totalExpense * 100)
                )
            }
            .sortedByDescending { it.amount }
        topExpensesAdapter.submitList(topExpenses)
    }

    private fun showCategoryDetails(category: String, transactions: List<TransactionWithCategory>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$category Details")
            .setView(R.layout.category_details_dialog)
            .setPositiveButton("Close", null)
            .show()
            .also { dialog ->
                val dialogView = dialog.findViewById<RecyclerView>(R.id.transactionsList)
                dialogView?.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = CategoryTransactionAdapter().apply {
                        submitList(transactions.sortedByDescending { it.amount }.take(3))
                    }
                }
            }
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", abs(amount))}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}