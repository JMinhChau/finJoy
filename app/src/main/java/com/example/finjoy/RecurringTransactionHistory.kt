package com.example.finjoy

import androidx.room.*
import java.time.LocalDate

@Entity(
    tableName = "recurring_transaction_history",
    foreignKeys = [
        // This connects each history entry to a recurring transaction
        ForeignKey(
            entity = RecurringTransaction::class,  // Parent table
            parentColumns = ["id"],                // Parent table's key
            childColumns = ["recurringTransactionId"], // This table's reference
            onDelete = ForeignKey.CASCADE          // If recurring transaction is deleted,
            // delete its history too
        )
    ]
)
data class RecurringTransactionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,  // Unique ID for each history entry
    val recurringTransactionId: Int,  // Which recurring transaction this history belongs to
    val amount: Double,  // The new amount that takes effect
    val startDate: LocalDate,  // When this amount change takes effect
    val note: String = ""  // Optional note explaining why amount changed
)