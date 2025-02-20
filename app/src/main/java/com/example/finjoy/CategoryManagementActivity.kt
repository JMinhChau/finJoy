package com.example.finjoy.ui.category

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finjoy.AppDatabase
import com.example.finjoy.Category
import com.example.finjoy.R
import com.example.finjoy.databinding.ActivityCategoryManagementBinding
import com.example.finjoy.databinding.DialogAddCategoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryManagementBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryManagementAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)
        setupViews()
        loadCategories()
    }

    private fun setupViews() {
        // Setup toolbar
        binding.topAppBar.setNavigationOnClickListener { finish() }

        // Setup RecyclerView
        adapter = CategoryManagementAdapter(
            categories = categories,
            onStartDrag = { viewHolder -> touchHelper.startDrag(viewHolder) },
            onDelete = { category -> showDeleteConfirmation(category) }
        )

        binding.categoryList.apply {
            layoutManager = LinearLayoutManager(this@CategoryManagementActivity)
            adapter = this@CategoryManagementActivity.adapter
        }

        // Attach ItemTouchHelper
        touchHelper.attachToRecyclerView(binding.categoryList)

        // Setup FAB
        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showDeleteConfirmation(category: Category) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.categoryDao().deleteCategory(category)
            loadCategories()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

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
                    Toast.makeText(this, "Please enter an emoji", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = when (dialogBinding.typeToggleGroup.checkedButtonId) {
                    R.id.incomeButton -> Category.TransactionType.INCOME
                    else -> Category.TransactionType.EXPENSE
                }

                addCategory(Category(
                    name = categoryName,
                    type = type,
                    emoji = emoji
                ))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCategory(category: Category) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.categoryDao().insertCategory(category)
            loadCategories()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedCategories = db.categoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                categories.clear()
                categories.addAll(loadedCategories)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPos = viewHolder.bindingAdapterPosition
            val toPos = target.bindingAdapterPosition

            // Update list
            categories.add(toPos, categories.removeAt(fromPos))
            adapter.notifyItemMoved(fromPos, toPos)

            // Save new order immediately
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    db.categoryDao().updateCategoryOrders(categories)
                } catch (e: Exception) {
                    Log.e("CategoryManagement", "Error updating order", e)
                }
            }

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }
    })
}