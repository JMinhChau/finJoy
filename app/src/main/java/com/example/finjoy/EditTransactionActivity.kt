package com.example.finjoy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.databinding.ActivityAddTransactionBinding
import com.example.finjoy.databinding.DialogAddCategoryBinding
import com.example.finjoy.ui.category.CategoryManagementActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.abs

class EditTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var db: AppDatabase
    private var selectedDate = LocalDate.now()
    private var currentTransactionType = Category.TransactionType.EXPENSE
    private var selectedCategoryId: Int? = null
    private var transactionId: Int = -1
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EditTransaction", "Starting EditTransactionActivity")
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        // Get transaction ID from intent
        transactionId = intent.getIntExtra("transaction_id", -1)
        Log.d("EditTransaction", "Transaction ID: $transactionId")
        if (transactionId == -1) {
            Toast.makeText(this, "Error loading transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupMenu()
        loadTransaction()
    }

    private fun loadTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transaction = db.transactionDao().getTransactionWithCategory(transactionId)

            withContext(Dispatchers.Main) {
                if (transaction != null) {
                    // Set amount
                    binding.amountInput.setText(abs(transaction.amount).toString())

                    // Set type
                    currentTransactionType = transaction.categoryType
                    binding.typeToggleGroup.check(
                        if (transaction.categoryType == Category.TransactionType.INCOME)
                            R.id.incomeButton else R.id.expenseButton
                    )

                    // Set category
                    selectedCategoryId = transaction.categoryId
                    binding.categoryInput.setText(transaction.categoryName)

                    // Set date
                    selectedDate = transaction.date
                    binding.dateInput.setText(selectedDate.format(dateFormatter))

                    // Set description
                    binding.descriptionInput.setText(transaction.description)
                } else {
                    Toast.makeText(this@EditTransactionActivity,
                        "Error loading transaction", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun setupViews() {
        // Change title and button text
        binding.topAppBar.setTitle(R.string.edit_transaction)
        binding.addTransactionBtn.setText(R.string.save_changes)

        // Setup type toggle initial colors
        updateToggleButtonColors(
            if (currentTransactionType == Category.TransactionType.INCOME)
                R.id.incomeButton else R.id.expenseButton
        )

        // Setup category selection
        binding.categoryLayout.setOnClickListener { showCategoryBottomSheet() }
        binding.categoryInput.setOnClickListener { showCategoryBottomSheet() }

        // Setup date picker
        binding.dateLayout.setOnClickListener { showDatePicker() }
        binding.dateInput.setOnClickListener { showDatePicker() }

        // Setup type toggle
        binding.typeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentTransactionType = when (checkedId) {
                    R.id.incomeButton -> {
                        updateToggleButtonColors(R.id.incomeButton)
                        Category.TransactionType.INCOME
                    }
                    else -> {
                        updateToggleButtonColors(R.id.expenseButton)
                        Category.TransactionType.EXPENSE
                    }
                }
                selectedCategoryId = null
                binding.categoryInput.setText("")
            }
        }

        // Setup save button
        binding.addTransactionBtn.setOnClickListener {
            updateTransaction()
        }

        // Setup back button
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun updateToggleButtonColors(checkedId: Int) {
        binding.apply {
            when (checkedId) {
                R.id.incomeButton -> {
                    incomeButton.setBackgroundColor(getColor(R.color.green))
                    expenseButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
                    incomeButton.setTextColor(getColor(R.color.white))
                    expenseButton.setTextColor(getColor(R.color.black))
                }
                R.id.expenseButton -> {
                    expenseButton.setBackgroundColor(getColor(R.color.red))
                    incomeButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
                    expenseButton.setTextColor(getColor(R.color.white))
                    incomeButton.setTextColor(getColor(R.color.black))
                }
            }
        }
    }

    private fun updateTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val amountText = binding.amountInput.text.toString()
                if (amountText.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.amountLayout.error = "Please enter an amount"
                    }
                    return@launch
                }

                if (selectedCategoryId == null) {
                    withContext(Dispatchers.Main) {
                        binding.categoryLayout.error = "Please select a category"
                    }
                    return@launch
                }

                val amount = try {
                    amountText.toDouble()
                } catch (e: NumberFormatException) {
                    withContext(Dispatchers.Main) {
                        binding.amountLayout.error = "Please enter a valid number"
                    }
                    return@launch
                }

                val finalAmount = if (currentTransactionType == Category.TransactionType.EXPENSE) {
                    -amount
                } else {
                    amount
                }

                val updatedTransaction = Transaction(
                    id = transactionId,
                    categoryId = selectedCategoryId!!,
                    amount = finalAmount,
                    description = binding.descriptionInput.text.toString().trim(),
                    date = selectedDate
                )

                db.transactionDao().updateTransaction(updatedTransaction)

                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK)
                    Toast.makeText(this@EditTransactionActivity,
                        "Transaction updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTransactionActivity,
                        "Error updating transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCategoryBottomSheet() {
        try {
            val bottomSheet = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.layout_category_bottom_sheet, null)
            bottomSheet.setContentView(bottomSheetView)

            // Setup close button
            bottomSheetView.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
                bottomSheet.dismiss()
            }

            // Setup edit button
            bottomSheetView.findViewById<ImageButton>(R.id.editButton).setOnClickListener {
                startActivity(Intent(this, CategoryManagementActivity::class.java))
                bottomSheet.dismiss()
            }

            val categoryGrid = bottomSheetView.findViewById<RecyclerView>(R.id.categoryGrid)
            categoryGrid.layoutManager = GridLayoutManager(this, 3)

            // Load categories from database
            lifecycleScope.launch(Dispatchers.IO) {
                val categories = db.categoryDao().getCategoriesByType(currentTransactionType)

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
            }

            bottomSheet.show()
        } catch (e: Exception) {
            Log.e("AddTransaction", "Error showing category selector", e)
            Toast.makeText(this, "Error showing categories", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        // Set initial toggle state based on current transaction type
        dialogBinding.typeToggleGroup.check(
            when(currentTransactionType) {
                Category.TransactionType.INCOME -> R.id.incomeButton
                Category.TransactionType.EXPENSE -> R.id.expenseButton
            }
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

                val type = when (dialogBinding.typeToggleGroup.checkedButtonId) {
                    R.id.incomeButton -> Category.TransactionType.INCOME
                    else -> Category.TransactionType.EXPENSE
                }

                // Add the new category
                lifecycleScope.launch(Dispatchers.IO) {
                    val newCategory = Category(
                        name = categoryName,
                        type = type,
                        emoji = emoji
                    )
                    db.categoryDao().insertCategory(newCategory)

                    // Reopen category bottom sheet to show the new category
                    withContext(Dispatchers.Main) {
                        showCategoryBottomSheet()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                binding.dateInput.setText(selectedDate.format(dateFormatter))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun setupMenu() {
        binding.topAppBar.inflateMenu(R.menu.edit_transaction_menu)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transaction = db.transactionDao().getTransactionWithCategory(transactionId)
                if (transaction != null) {
                    db.transactionDao().deleteTransaction(
                        Transaction(
                            id = transactionId,
                            categoryId = transaction.categoryId,
                            amount = transaction.amount,
                            description = transaction.description,
                            date = transaction.date
                        )
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditTransactionActivity,
                            "Transaction deleted", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTransactionActivity,
                        "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}