package com.example.finjoy

import androidx.room.*
import java.time.LocalDate

@Dao
interface RecurringTransactionHistoryDao {
    // Get all history entries for a specific recurring transaction
    // Orders by date descending (newest first)
    @Query("""
        SELECT * FROM recurring_transaction_history 
        WHERE recurringTransactionId = :recurringId 
        ORDER BY startDate DESC
    """)
    suspend fun getHistoryForTransaction(recurringId: Int): List<RecurringTransactionHistory>

    // Add a new history entry
    @Insert
    suspend fun insertHistory(history: RecurringTransactionHistory)

    // Get the amount that was active on a specific date
    @Query("""
        SELECT amount FROM recurring_transaction_history
        WHERE recurringTransactionId = :recurringId
        AND startDate <= :date
        ORDER BY startDate DESC
        LIMIT 1
    """)
    suspend fun getAmountForDate(recurringId: Int, date: LocalDate): Double?

    // Delete all history for a recurring transaction
    // (Usually not needed because of CASCADE delete)
    @Query("DELETE FROM recurring_transaction_history WHERE recurringTransactionId = :recurringId")
    suspend fun deleteHistoryForTransaction(recurringId: Int)
}
