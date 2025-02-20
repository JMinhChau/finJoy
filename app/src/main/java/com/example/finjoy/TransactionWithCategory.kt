package com.example.finjoy

import java.time.LocalDate

data class TransactionWithCategory(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryType: Category.TransactionType,
    val categoryEmoji: String,
    val amount: Double,
    val description: String,
    val date: LocalDate
)