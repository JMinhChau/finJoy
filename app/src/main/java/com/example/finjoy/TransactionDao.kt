package com.example.finjoy

import androidx.room.*
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.*, 
               c.name as categoryName, 
               c.type as categoryType,
               c.emoji as categoryEmoji
        FROM transactions t 
        JOIN categories c ON t.categoryId = c.id
    """)
    suspend fun getTransactionsWithCategory(): List<TransactionWithCategory>

    @Query("""
        SELECT t.*, 
               c.name as categoryName, 
               c.type as categoryType,
               c.emoji as categoryEmoji
        FROM transactions t 
        JOIN categories c ON t.categoryId = c.id
        WHERE t.id = :transactionId
    """)
    suspend fun getTransactionWithCategory(transactionId: Int): TransactionWithCategory?

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("""
    SELECT t.* FROM transactions t 
    WHERE t.description = 'Recurring: ' || (
        SELECT name FROM recurring_transactions WHERE id = :recurringId
    )
    AND t.date = :transactionDate
""")
    suspend fun getRecurringTransactionForDate(
        recurringId: Int,
        transactionDate: LocalDate
    ): Transaction?

    @Query("""
    SELECT * FROM transactions 
    WHERE date = :date 
    AND description = :description 
    AND amount = :amount 
    LIMIT 1
""")
    suspend fun getTransactionForDateAndDesc(
        date: LocalDate,
        description: String,
        amount: Double
    ): Transaction?
}