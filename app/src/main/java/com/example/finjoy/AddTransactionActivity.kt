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


class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var db: AppDatabase
    private var selectedDate = LocalDate.now()
    private var currentTransactionType = Category.TransactionType.EXPENSE
    private var selectedCategoryId: Int? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            // Initial states
            dateInput.setText(selectedDate.format(dateFormatter))
            typeToggleGroup.check(R.id.expenseButton)
            updateToggleButtonColors(R.id.expenseButton)
            amountInput.requestFocus()

            // Category selection
            categoryLayout.setOnClickListener {
                showCategoryBottomSheet()
            }
            categoryInput.setOnClickListener {
                showCategoryBottomSheet()
            }

            // Date picker
            dateLayout.setOnClickListener {
                showDatePicker()
            }
            dateInput.setOnClickListener {
                showDatePicker()
            }

            // Transaction type toggle
            typeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
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
                    categoryInput.setText("")
                }
            }

            amountInput.isEnabled = true
            dateInput.isFocusable = false
            categoryInput.isFocusable = false
            descriptionInput.isEnabled = true

            // Save button
            addTransactionBtn.setOnClickListener {
                saveTransaction()
            }

            // Back button
            topAppBar.setNavigationOnClickListener {
                finish()
            }
        }
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

    private fun saveTransaction() {
        Log.d("AddTransaction", "Starting to save transaction...")

        // Get and validate amount
        val amountText = binding.amountInput.text.toString()
        Log.d("AddTransaction", "Amount entered: $amountText")

        if (amountText.isEmpty()) {
            binding.amountLayout.error = "Please enter an amount"
            return
        }

        // Parse amount as Double explicitly
        val inputAmount = try {
            amountText.toDouble().also {
                Log.d("AddTransaction", "Parsed amount successfully")
            }
        } catch (e: NumberFormatException) {
            Log.e("AddTransaction", "Error parsing amount", e)
            binding.amountLayout.error = "Please enter a valid number"
            return
        }

        // Validate category selection
        if (selectedCategoryId == null) {
            binding.categoryLayout.error = "Please select a category"
            return
        }

        // Calculate final amount based on transaction type
        val finalAmount = inputAmount * if (currentTransactionType == Category.TransactionType.EXPENSE) -1 else 1
        Log.d("AddTransaction", "Final amount to save: $finalAmount")

        // Create transaction object
        val transaction = Transaction(
            categoryId = selectedCategoryId!!,
            amount = finalAmount,
            description = binding.descriptionInput.text.toString().trim(),
            date = selectedDate
        )
        Log.d("AddTransaction", "Created transaction object: $transaction")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.transactionDao().insertTransaction(transaction)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddTransactionActivity,
                        "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddTransactionActivity,
                        "Error saving transaction", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        // Set initial toggle based on current transaction type
        dialogBinding.typeToggleGroup.check(
            if (currentTransactionType == Category.TransactionType.INCOME)
                R.id.incomeButton else R.id.expenseButton
        )

        // Enable toggle group for selection
        dialogBinding.typeToggleGroup.isEnabled = true  // Allow type selection

        // Add listener to handle type changes
        dialogBinding.typeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.incomeButton -> {
                        dialogBinding.incomeButton.setBackgroundColor(getColor(R.color.green))
                        dialogBinding.expenseButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
                        dialogBinding.incomeButton.setTextColor(getColor(R.color.white))
                        dialogBinding.expenseButton.setTextColor(getColor(R.color.black))
                    }
                    R.id.expenseButton -> {
                        dialogBinding.expenseButton.setBackgroundColor(getColor(R.color.red))
                        dialogBinding.incomeButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
                        dialogBinding.expenseButton.setTextColor(getColor(R.color.white))
                        dialogBinding.incomeButton.setTextColor(getColor(R.color.black))
                    }
                }
            }
        }

        // Update colors for initial state
        if (currentTransactionType == Category.TransactionType.INCOME) {
            dialogBinding.incomeButton.setBackgroundColor(getColor(R.color.green))
            dialogBinding.expenseButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
            dialogBinding.incomeButton.setTextColor(getColor(R.color.white))
            dialogBinding.expenseButton.setTextColor(getColor(R.color.black))
        } else {
            dialogBinding.expenseButton.setBackgroundColor(getColor(R.color.red))
            dialogBinding.incomeButton.setBackgroundColor(getColor(com.google.android.material.R.color.material_dynamic_neutral80))
            dialogBinding.expenseButton.setTextColor(getColor(R.color.white))
            dialogBinding.incomeButton.setTextColor(getColor(R.color.black))
        }

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

                // Get the selected type from dialog
                val selectedType = when (dialogBinding.typeToggleGroup.checkedButtonId) {
                    R.id.incomeButton -> Category.TransactionType.INCOME
                    else -> Category.TransactionType.EXPENSE
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val categories = db.categoryDao().getAllCategories()
                        val nextId = (categories.maxOfOrNull { it.id } ?: 0) + 1

                        val newCategory = Category(
                            id = nextId,
                            name = categoryName,
                            type = selectedType,  // Use selected type from dialog
                            emoji = emoji,
                            displayOrder = nextId
                        )
                        db.categoryDao().insertCategory(newCategory)

                        // Only set as selected if type matches current transaction
                        if (selectedType == currentTransactionType) {
                            selectedCategoryId = nextId
                            withContext(Dispatchers.Main) {
                                binding.categoryInput.setText(categoryName)
                                binding.categoryLayout.error = null
                            }
                        }

                        withContext(Dispatchers.Main) {
                            showCategoryBottomSheet() // Refresh category list
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddTransactionActivity,
                                "Error adding category", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

            lifecycleScope.launch(Dispatchers.IO) {
                try {
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
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddTransactionActivity,
                            "Error loading categories",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            bottomSheet.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing category selector", Toast.LENGTH_SHORT).show()
        }
    }
}