// CategoryActivity.kt
package com.example.finjoy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finjoy.databinding.ActivityCategoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.finjoy.databinding.DialogAddCategoryBinding


@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0

) {
    enum class TransactionType {
        INCOME, EXPENSE
    }
}


class CategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var db: AppDatabase
    private var categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        setupRecyclerView()
        setupAddCategoryButton()
        setupCloseButton()
        loadCategories()
    }

    private fun setupRecyclerView() {
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoryRecyclerView.adapter = CategoryAdapter(categories) { category ->
            val intent = intent.apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun setupAddCategoryButton() {
        binding.addCategoryBtn.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun setupCloseButton() {
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Category")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { dialog, _ ->
                val categoryName = dialogBinding.categoryNameInput.text.toString()
                val emoji = dialogBinding.emojiInput.text.toString()
                val type = when (dialogBinding.typeToggleGroup.checkedButtonId) {
                    R.id.incomeButton -> Category.TransactionType.INCOME
                    else -> Category.TransactionType.EXPENSE
                }

                if (categoryName.isNotBlank() && emoji.isNotBlank()) {
                    addCategory(Category(
                        name = categoryName,
                        type = type,
                        emoji = emoji
                    ))
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
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
                binding.categoryRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

}