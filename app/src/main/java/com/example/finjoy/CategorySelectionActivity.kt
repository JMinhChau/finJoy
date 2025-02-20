package com.example.finjoy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finjoy.databinding.ActivityCategorySelectionBinding
import com.example.finjoy.databinding.DialogAddCategoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategorySelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategorySelectionBinding
    private lateinit var db: AppDatabase
    private var categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategorySelectionAdapter
    private var currentTransactionType = Category.TransactionType.EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategorySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get transaction type from intent
        currentTransactionType = intent.getSerializableExtra("transactionType") as? Category.TransactionType
            ?: Category.TransactionType.EXPENSE

        db = AppDatabase.getInstance(applicationContext)

        setupViews()
        loadCategories()
    }

    private fun setupViews() {
        // Setup toolbar
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.title = "Select ${currentTransactionType.name.lowercase().capitalize()} Category"

        // Setup RecyclerView
        categoryAdapter = CategorySelectionAdapter(categories) { category ->
            setResult(RESULT_OK, Intent().apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
            })
            finish()
        }
        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CategorySelectionActivity)
            adapter = categoryAdapter
        }

        // Setup FAB for adding new category
        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        // Set initial type based on current transaction type
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

                    // Reload categories to show the new one
                    loadCategories()
                }

                Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedCategories = db.categoryDao()
                .getCategoriesByType(currentTransactionType)

            withContext(Dispatchers.Main) {
                categories.clear()
                categories.addAll(loadedCategories)
                categoryAdapter.notifyDataSetChanged()
            }
        }
    }
}