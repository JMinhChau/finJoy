package com.example.finjoy.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finjoy.*
import com.example.finjoy.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var transactionAdapter: TransactionAdapter
    private val addTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("HomeFragment", "Add transaction returned OK")
                loadTransactions()
            }
        }

    private val editTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTransactions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())
        setupViews()
        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupViews() {
        // Setup RecyclerView
        transactionAdapter = TransactionAdapter(onTransactionClick = { transaction ->
            Log.d("HomeFragment", "Starting edit for transaction: ${transaction.id}")
            val intent = Intent(requireContext(), EditTransactionActivity::class.java).apply {
                putExtra("transaction_id", transaction.id)
            }
            try {
                editTransactionLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error launching EditTransactionActivity", e)
                Toast.makeText(requireContext(), "Error opening edit screen", Toast.LENGTH_SHORT).show()
            }
        })

        binding.recyclerview.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.addTransactionBtn.setOnClickListener {
            Log.d("HomeFragment", "Add transaction FAB clicked")
            try {
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                addTransactionLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error launching AddTransactionActivity", e)
                Toast.makeText(requireContext(), "Error opening add screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // First process any recurring transactions
                processRecurringTransactions()

                // Then load ALL transactions (including the newly created ones)
                val transactions = db.transactionDao().getTransactionsWithCategory()
                    .sortedByDescending { it.date }

                Log.d("HomeFragment", "Found ${transactions.size} transactions")
                transactions.forEach { transaction ->
                    Log.d("HomeFragment", "Transaction: ${transaction.description}, Amount: ${transaction.amount}")
                }

                withContext(Dispatchers.Main) {
                    transactionAdapter.submitList(transactions)
                    updateDashboard(transactions)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading transactions", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Error loading transactions",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processRecurringTransactions() {
        try {
            val today = LocalDate.now()
            val recurring = db.recurringTransactionDao()
                .getRecurringTransactionsWithCategory()

            Log.d("HomeFragment", "Processing recurring transactions from their start dates to $today")

            recurring.forEach { item ->
                val days = item.daysOfMonth.split(",").map { it.trim().toInt() }

                Log.d("HomeFragment", """
                Processing ${item.name}:
                - Start date: ${item.startDate}
                - Days: $days
                - Amount: ${item.amount}
            """.trimIndent())

                // Start from the first month
                var currentMonth = item.startDate
                while (!currentMonth.isAfter(today)) {
                    if (days.contains(currentMonth.dayOfMonth)) {
                        // Check if transaction already exists
                        val existing = db.transactionDao()
                            .getRecurringTransactionForDate(item.id, currentMonth)

                        if (existing == null) {
                            val transaction = Transaction(
                                categoryId = item.categoryId,
                                amount = item.amount,
                                description = "Recurring: ${item.name}",
                                date = currentMonth
                            )
                            db.transactionDao().insertTransaction(transaction)
                            Log.d("HomeFragment", "Created transaction for ${item.name} on ${currentMonth}")
                        }
                    }
                    currentMonth = currentMonth.plusDays(1)
                }
            }

            // Log what was created
            val allTransactions = db.transactionDao().getTransactionsWithCategory()
            Log.d("HomeFragment", "Created ${allTransactions.size} transactions")
            allTransactions.forEach { t ->
                Log.d("HomeFragment", "${t.description} on ${t.date} for ${t.amount}")
            }

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error processing recurring", e)
        }
    }
    private fun updateDashboard(transactions: List<TransactionWithCategory>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        transactions.forEach { transaction ->
            Log.d("HomeFragment", "Processing transaction: ${transaction.description} = ${transaction.amount}")
            if (transaction.amount >= 0) {
                totalIncome += transaction.amount
            } else {
                totalExpense += abs(transaction.amount)
            }
        }

        val balance = totalIncome - totalExpense

        Log.d("HomeFragment", """
        Income: $totalIncome
        Expense: $totalExpense
        Balance: $balance
    """.trimIndent())

        binding.balance.text = formatCurrency(balance)
        binding.budget.text = formatCurrency(totalIncome)
        binding.expense.text = formatCurrency(totalExpense)
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", abs(amount))}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}