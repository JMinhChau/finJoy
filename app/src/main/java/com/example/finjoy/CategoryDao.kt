package com.example.finjoy

import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY display_order ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY display_order ASC")
    suspend fun getCategoriesByType(type: Category.TransactionType): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT MAX(display_order) FROM categories")
    suspend fun getMaxDisplayOrder(): Int?

    @Query("UPDATE categories SET display_order = :newOrder WHERE id = :categoryId")
    suspend fun updateCategoryOrder(categoryId: Int, newOrder: Int)

    suspend fun updateCategoryOrders(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            updateCategoryOrder(category.id, index)
        }
    }
}