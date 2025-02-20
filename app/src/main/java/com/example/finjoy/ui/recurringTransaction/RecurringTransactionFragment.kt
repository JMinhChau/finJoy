package com.example.finjoy

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
import com.example.finjoy.databinding.FragmentRecurringTransactionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class RecurringTransactionFragment : Fragment() {
    private var _binding: FragmentRecurringTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: RecurringTransactionAdapter

    private val addRecurringLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadRecurringTransactions()
        }
    }

    private val editRecurringLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadRecurringTransactions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())
        setupViews()
        debugCheckDatabase()
        loadRecurringTransactions()
    }

    private fun setupViews() {
        adapter = RecurringTransactionAdapter { recurringTransaction ->
            val intent = Intent(requireContext(), EditRecurringTransactionActivity::class.java).apply {
                putExtra("recurring_transaction_id", recurringTransaction.id)
            }
            editRecurringLauncher.launch(intent)
        }

        binding.recurringTransactionsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecurringTransactionFragment.adapter
        }

        binding.addRecurringButton.setOnClickListener {
            val intent = Intent(requireContext(), RecurringTransactionActivity::class.java)
            addRecurringLauncher.launch(intent)
        }

        binding.refreshButton.setOnClickListener {
            binding.refreshButton.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .start()
            processRecurringTransactions()
        }
    }

    private fun debugCheckDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = db.recurringTransactionDao().getRecurringTransactionsWithCategory().size
            Log.d("RecurringFragment", "Total recurring transactions in database: $count")
        }
    }

    private fun loadRecurringTransactions() {
        Log.d("RecurringFragment", "Starting to load transactions")
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactions = db.recurringTransactionDao()
                    .getRecurringTransactionsWithCategory()

                Log.d("RecurringFragment", "Found ${transactions.size} transactions")
                transactions.forEach {
                    Log.d("RecurringFragment", "Transaction: ${it.name}, Amount: ${it.amount}")
                }

                withContext(Dispatchers.Main) {
                    if (transactions.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recurringTransactionsList.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recurringTransactionsList.visibility = View.VISIBLE
                        adapter.submitList(transactions)
                    }
                }
            } catch (e: Exception) {
                Log.e("RecurringFragment", "Error loading transactions", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading transactions: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun processRecurringTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                Log.d("RecurringFragment", "Processing for date: $today")

                val recurring = db.recurringTransactionDao()
                    .getRecurringTransactionsWithCategory()

                recurring.forEach { item ->
                    val days = item.daysOfMonth.split(",").map { it.trim().toInt() }

                    // Check if transaction already exists for this specific recurring item today
                    val existingTransaction = db.transactionDao()
                        .getRecurringTransactionForDate(item.id, today)

                    // Only create transaction if:
                    // 1. Today is one of the recurring days
                    // 2. Today is not before start date
                    // 3. No transaction exists for this specific recurring item today
                    if (days.contains(today.dayOfMonth) &&
                        !today.isBefore(item.startDate) &&
                        existingTransaction == null) {

                        val transaction = Transaction(
                            categoryId = item.categoryId,
                            amount = item.amount,
                            description = "Recurring: ${item.name}",
                            date = today
                        )
                        db.transactionDao().insertTransaction(transaction)
                        Log.d("RecurringFragment", "Created transaction for ${item.name}")
                    } else {
                        Log.d("RecurringFragment",
                            "Skipped ${item.name} - already exists or not due today")
                    }
                }

                withContext(Dispatchers.Main) {
                    loadRecurringTransactions() // Refresh the list
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Error processing transactions",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}