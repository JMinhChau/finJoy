package com.example.finjoy

import androidx.room.*
import java.time.LocalDate

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double,  // Positive for income, negative for expense
    val categoryId: Int,
    val daysOfMonth: String,
    val startDate: LocalDate,
    val isActive: Boolean = true,
    val description: String = ""
)