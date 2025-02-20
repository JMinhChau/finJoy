package com.example.finjoy

import java.time.LocalDate

data class RecurringTransactionWithCategory(
    val id: Int,
    val name: String,
    val amount: Double,
    val categoryId: Int,
    val daysOfMonth: String,
    val startDate: LocalDate,
    val isActive: Boolean,
    val description: String,
    val categoryName: String,
    val categoryType: Category.TransactionType,
    val categoryEmoji: String
)