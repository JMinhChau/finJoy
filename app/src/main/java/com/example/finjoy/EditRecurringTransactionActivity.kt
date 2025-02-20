package com.example.finjoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.databinding.ActivityAddRecurringBinding
import com.example.finjoy.databinding.DialogAddCategoryBinding
import com.example.finjoy.ui.category.CategoryManagementActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class EditRecurringTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddRecurringBinding
    private lateinit var db: AppDatabase
    private var selectedCategoryId: Int? = null
    private var currentTransactionType = Category.TransactionType.EXPENSE
    private var recurringTransactionId: Int = -1
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private var currentAmount: Double = 0.0
    private var currentRecurringTransaction: RecurringTransactionWithCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecurringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)
        recurringTransactionId = intent.getIntExtra("recurring_transaction_id", -1)

        if (recurringTransactionId == -1) {
            Toast.makeText(this, "Error loading recurring transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showEditDisclaimer()
        setupViews()
        loadRecurringTransaction()

        // Disable start month editing
        binding.startMonthInput.isEnabled = false
        binding.startMonthLayout.hint = "Start Month (Cannot be changed)"

        // Hide the "include current month" checkbox as it's only for new transactions
        binding.includeCurrentMonthCheck.visibility = View.GONE
    }

    private fun setupViews() {
        // Change title and button text
        binding.topAppBar.title = "Edit Recurring Transaction"
        binding.addButton.text = "Save Changes"

        // Disable type toggle
        binding.typeToggleGroup.isEnabled = false
        binding.incomeButton.isEnabled = false
        binding.expenseButton.isEnabled = false

        // Disable name input
        binding.nameInput.isEnabled = false
        binding.nameInput.alpha = 0.7f
        binding.nameLayout.hint = "Name (Cannot be changed)"

        // Disable category selection
        binding.categoryInput.isEnabled = false
        binding.categoryInput.alpha = 0.7f
        binding.categoryLayout.hint = "Category (Cannot be changed)"
        binding.categoryLayout.setOnClickListener(null)
        binding.categoryInput.setOnClickListener(null)

        // Setup save button
        binding.addButton.setOnClickListener { saveRecurringTransaction() }

        // Setup toolbar with delete option
        binding.topAppBar.inflateMenu(R.menu.edit_recurring_menu)
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_history -> {
                    showHistoryDialog()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadRecurringTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recurring = db.recurringTransactionDao()
                    .getRecurringTransactionWithCategory(recurringTransactionId)

                if (recurring == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EditRecurringTransactionActivity,
                            "Error: Transaction not found",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@launch
                }

                currentRecurringTransaction = recurring
                currentAmount = recurring.amount

                withContext(Dispatchers.Main) {
                    binding.apply {
                        nameInput.setText(recurring.name)
                        amountInput.setText(abs(recurring.amount).toString())
                        categoryInput.setText(recurring.categoryName)
                        dayInput.setText(recurring.daysOfMonth)
                        startMonthInput.setText(recurring.startDate.format(dateFormatter))
                        descriptionInput.setText(recurring.description)

                        // Set transaction type
                        currentTransactionType = recurring.categoryType
                        typeToggleGroup.check(
                            if (recurring.categoryType == Category.TransactionType.INCOME)
                                R.id.incomeButton else R.id.expenseButton
                        )

                        selectedCategoryId = recurring.categoryId
                    }
                }
            } catch (e: Exception) {
                Log.e("EditRecurring", "Error loading transaction", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditRecurringTransactionActivity,
                        "Error loading transaction: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun showCategoryBottomSheet() {
        try {
            val bottomSheet = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.layout_category_bottom_sheet, null)
            bottomSheet.setContentView(bottomSheetView)

            bottomSheetView.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
                bottomSheet.dismiss()
            }

            bottomSheetView.findViewById<ImageButton>(R.id.editButton).setOnClickListener {
                startActivity(Intent(this, CategoryManagementActivity::class.java))
                bottomSheet.dismiss()
            }

            val categoryGrid = bottomSheetView.findViewById<RecyclerView>(R.id.categoryGrid)
            categoryGrid.layoutManager = GridLayoutManager(this, 3)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val categories = db.categoryDao()
                        .getCategoriesByType(currentTransactionType)

                    withContext(Dispatchers.Main) {
                        val adapter = CategoryGridAdapter(
                            categories = categories,
                            onCategoryClick = { category ->
                                selectedCategoryId = category.id
                                binding.categoryInput.setText(category.name)
                                binding.categoryLayout.error = null
                                bottomSheet.dismiss()
                            },
                            onAddClick = {
                                showAddCategoryDialog()
                                bottomSheet.dismiss()
                            }
                        )
                        categoryGrid.adapter = adapter
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditRecurringTransactionActivity,
                            "Error loading categories", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            bottomSheet.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing category selector", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecurringTransaction() {
        // Only validate amount since other fields are locked
        val amountStr = binding.amountInput.text.toString()
        if (amountStr.isEmpty()) {
            binding.amountLayout.error = "Please enter an amount"
            return
        }

        val amount = try {
            amountStr.toDouble() * if (currentTransactionType == Category.TransactionType.EXPENSE) -1 else 1
        } catch (e: NumberFormatException) {
            binding.amountLayout.error = "Please enter a valid amount"
            return
        }

        // Get current transaction
        val recurring = currentRecurringTransaction ?: return

        // Create updated transaction (only amount and days can change)
        val updatedTransaction = RecurringTransaction(
            id = recurringTransactionId,
            name = recurring.name,  // Keep original name
            amount = amount,        // New amount
            categoryId = recurring.categoryId, // Keep original category
            daysOfMonth = binding.dayInput.text.toString().trim(),
            startDate = recurring.startDate,  // Keep original start date
            description = binding.descriptionInput.text.toString().trim()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Check if amount has changed
                if (amount != currentAmount) {
                    // Add a history entry
                    val historyEntry = RecurringTransactionHistory(
                        recurringTransactionId = recurringTransactionId,
                        amount = amount,
                        startDate = LocalDate.now(),
                        note = "Amount changed from ${formatCurrency(currentAmount)} to ${formatCurrency(amount)}"
                    )
                    db.recurringTransactionHistoryDao().insertHistory(historyEntry)
                }

                // Update the recurring transaction
                db.recurringTransactionDao().updateRecurringTransaction(updatedTransaction)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditRecurringTransactionActivity,
                        "Amount updated for future transactions",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditRecurringTransactionActivity,
                        "Error saving changes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Recurring Transaction")
            .setMessage("Are you sure you want to delete this recurring transaction? Past transactions will be kept.")
            .setPositiveButton("Delete") { _, _ -> deleteRecurringTransaction() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecurringTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recurring = currentRecurringTransaction
                if (recurring != null) {
                    db.recurringTransactionDao().deleteRecurringTransaction(
                        RecurringTransaction(
                            id = recurringTransactionId,
                            name = recurring.name,
                            amount = recurring.amount,
                            categoryId = recurring.categoryId,
                            daysOfMonth = recurring.daysOfMonth,
                            startDate = recurring.startDate,
                            description = recurring.description
                        )
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditRecurringTransactionActivity,
                            "Recurring transaction deleted", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditRecurringTransactionActivity,
                        "Error deleting recurring transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditDisclaimer() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About Editing")
            .setMessage("Changes will only affect future transactions. Past transactions will remain unchanged.")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", abs(amount))}"
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        // Set initial toggle based on current transaction type
        dialogBinding.typeToggleGroup.check(
            if (currentTransactionType == Category.TransactionType.INCOME)
                R.id.incomeButton else R.id.expenseButton
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Category")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = dialogBinding.categoryNameInput.text.toString()
                val emoji = dialogBinding.emojiInput.text.toString()

                if (categoryName.isBlank()) {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (emoji.isBlank()) {
                    Toast.makeText(this, "Please select an emoji", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val categories = db.categoryDao().getAllCategories()
                        val nextId = (categories.maxOfOrNull { it.id } ?: 0) + 1

                        val newCategory = Category(
                            id = nextId,
                            name = categoryName,
                            type = currentTransactionType,
                            emoji = emoji,
                            displayOrder = nextId
                        )
                        db.categoryDao().insertCategory(newCategory)

                        selectedCategoryId = nextId
                        withContext(Dispatchers.Main) {
                            binding.categoryInput.setText(categoryName)
                            binding.categoryLayout.error = null
                            showCategoryBottomSheet() // Refresh category list
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EditRecurringTransactionActivity,
                                "Error adding category",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showHistoryDialog() {
        val dialog = MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(R.layout.dialog_recurring_history)
            .create()

        dialog.show()

        // Setup dialog views after show() so we can access them
        dialog.findViewById<RecyclerView>(R.id.historyList)?.let { recyclerView ->
            val adapter = RecurringHistoryAdapter()
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            // Set recurring name
            dialog.findViewById<TextView>(R.id.nameLabel)?.text =
                "${currentRecurringTransaction?.name} Amount Changes"

            // Load history
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val history = db.recurringTransactionHistoryDao()
                        .getHistoryForTransaction(recurringTransactionId)
                        .sortedByDescending { it.startDate }

                    // Convert to display items with date ranges
                    val items = history.mapIndexed { index, item ->
                        RecurringHistoryAdapter.HistoryItem(
                            amount = item.amount,
                            startDate = item.startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            endDate = if (index > 0) {
                                history[index - 1].startDate
                                    .minusDays(1)
                                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                            } else null
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (items.isEmpty()) {
                            dialog.findViewById<TextView>(R.id.nameLabel)?.text =
                                "No amount changes recorded yet"
                        }
                        adapter.submitList(items)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EditRecurringTransactionActivity,
                            "Error loading history",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }
    }
}