package com.example.finjoy

import android.content.Context
import androidx.core.content.ContextCompat
import kotlin.math.abs

object ColorUtils {
    // Predefined categories with fixed colors
    private val FIXED_CATEGORY_COLORS = mapOf(
        "Bills" to R.color.red,
        "Shopping" to R.color.orange,
        "Food" to R.color.yellow,
        "Transport" to R.color.pink,
        "Entertainment" to R.color.coral,
        "Healthcare" to R.color.ruby,
        "Education" to R.color.amber,
        "Salary" to R.color.green,
        "Freelance" to R.color.teal,
        "Investment" to R.color.teal_700
    )

    // Color banks for new categories
    private val EXPENSE_COLORS = listOf(
        R.color.red,
        R.color.orange,
        R.color.coral,
        R.color.ruby,
        R.color.pink,
        R.color.amber,
        R.color.red_light,
        R.color.brown
    )

    private val INCOME_COLORS = listOf(
        R.color.green,      // Primary green
        R.color.teal,       // Teal
        R.color.teal_700,   // Dark teal
        R.color.teal_200,   // Light teal
        R.color.blue,       // Blue (as a cool color variant)
        R.color.cyan        // Cyan (as another cool color variant)
    )

    fun getCategoryColor(context: Context, categoryName: String, type: Category.TransactionType): Int {
        // First check if it's a predefined category
        FIXED_CATEGORY_COLORS[categoryName]?.let {
            return ContextCompat.getColor(context, it)
        }

        // If not predefined, get a color based on category name hash
        val colorBank = if (type == Category.TransactionType.EXPENSE) EXPENSE_COLORS else INCOME_COLORS

        // Use modulo to ensure we always get a valid index, even if we have more categories than colors
        val index = abs(categoryName.hashCode()) % colorBank.size
        return ContextCompat.getColor(context, colorBank[index])
    }
}