package com.example.finjoy

import MonthYearPickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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

class RecurringTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddRecurringBinding
    private lateinit var db: AppDatabase
    private var selectedCategoryId: Int? = null
    private var currentTransactionType = Category.TransactionType.EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecurringBinding.inflate(layoutInflater)
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)
        setupViews()
    }

    private fun setupViews() {
        // Setup category selection
        binding.categoryLayout.setOnClickListener { showCategoryBottomSheet() }
        binding.categoryInput.setOnClickListener { showCategoryBottomSheet() }

        // Setup month picker
        binding.startMonthLayout.setOnClickListener { showMonthYearPicker() }
        binding.startMonthInput.setOnClickListener { showMonthYearPicker() }

        binding.addButton.setOnClickListener { saveRecurringTransaction() }

        binding.typeToggleGroup.check(R.id.expenseButton)
        updateToggleButtonColors(R.id.expenseButton)

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
        binding.categoryInput.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
        }

        binding.startMonthInput.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
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


    private fun showMonthYearPicker() {
        val dialog = MonthYearPickerDialog()
        dialog.setListener { _, year, month, _ ->
            val selectedDate = LocalDate.of(year, month + 1, 1)
            binding.startMonthInput.setText(selectedDate.format(DateTimeFormatter.ofPattern("MMM yyyy")))
            binding.startMonthLayout.error = null
        }
        dialog.show(supportFragmentManager, "MonthYearPickerDialog")
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

            // Load only expense categories
            lifecycleScope.launch(Dispatchers.IO) {
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
            }

            bottomSheet.show()
        } catch (e: Exception) {
            Log.e("AddFixedCost", "Error showing category selector", e)
            Toast.makeText(this, "Error showing categories", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        // Set initial toggle based on current type
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
                    val newCategory = Category(
                        name = categoryName,
                        type = currentTransactionType,  // Use current type
                        emoji = emoji
                    )
                    db.categoryDao().insertCategory(newCategory)

                    withContext(Dispatchers.Main) {
                        showCategoryBottomSheet()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveRecurringTransaction() {
        // Validate inputs
        val name = binding.nameInput.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameLayout.error = "Please enter a name"
            return
        }

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

        if (selectedCategoryId == null) {
            binding.categoryLayout.error = "Please select a category"
            return
        }

        val daysStr = binding.dayInput.text.toString().trim()
        if (daysStr.isEmpty()) {
            binding.dayLayout.error = "Please enter days"
            return
        }

        // Validate days format and values
        try {
            val days = daysStr.split(",").map { it.trim().toInt() }
            for (day in days) {
                if (day < 1 || day > 31) {
                    binding.dayLayout.error = "Days must be between 1 and 31"
                    return
                }
            }
        } catch (e: NumberFormatException) {
            binding.dayLayout.error = "Please enter valid days (e.g., 1,15)"
            return
        }

        val startDateStr = binding.startMonthInput.text.toString()
        if (startDateStr.isEmpty()) {
            binding.startMonthLayout.error = "Please select start month"
            return
        }

        val startDate = try {
            // Parse the date string using the same formatter used to display it
            LocalDate.parse("01 " + startDateStr, DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            binding.startMonthLayout.error = "Invalid start date"
            return
        }

        Log.d("RecurringActivity", """
            Creating recurring transaction:
            Name: $name
            Amount: $amount
            Category: $selectedCategoryId
            Days: $daysStr
            Start Month: ${binding.startMonthInput.text}
            Description: ${binding.descriptionInput.text}
        """.trimIndent())

        val recurringTransaction = RecurringTransaction(
            name = name,
            amount = amount,
            categoryId = selectedCategoryId!!,
            daysOfMonth = daysStr,
            startDate = startDate,
            description = binding.descriptionInput.text.toString().trim()
        )
        Log.d("RecurringActivity", "Transaction object created: $recurringTransaction")
        // Save to database
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Debug log before insert
                Log.d("RecurringActivity", "About to insert transaction")

                db.recurringTransactionDao().insertRecurringTransaction(recurringTransaction)

                // Debug log after insert
                Log.d("RecurringActivity", "Transaction inserted successfully")

                // If including current month
                if (binding.includeCurrentMonthCheck.isChecked) {
                    val today = LocalDate.now()
                    Log.d("RecurringActivity", "Processing current month. Today: $today")

                    if (startDate.month == today.month && startDate.year == today.year) {
                        val days = daysStr.split(",")
                            .map { it.trim().toInt() }
                            .filter { it <= today.dayOfMonth }

                        Log.d("RecurringActivity", "Days to process: $days")

                        for (day in days) {
                            val transaction = Transaction(
                                categoryId = selectedCategoryId!!,
                                amount = amount,
                                description = "Recurring: $name",
                                date = today.withDayOfMonth(day)
                            )
                            db.transactionDao().insertTransaction(transaction)
                            Log.d("RecurringActivity", "Created transaction for day: $day")
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RecurringTransactionActivity,
                        "Recurring transaction added",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("RecurringActivity", "Error saving transaction", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RecurringTransactionActivity,
                        "Error adding recurring transaction: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

