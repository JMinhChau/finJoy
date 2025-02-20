package com.example.finjoy

import androidx.room.*
import java.time.LocalDate

@Dao
interface RecurringTransactionDao {
    @Query("""
        SELECT r.id, r.name, r.amount, r.categoryId, r.daysOfMonth, 
               r.startDate, r.isActive, r.description,
               c.name as categoryName, c.type as categoryType,
               c.emoji as categoryEmoji
        FROM recurring_transactions r
        JOIN categories c ON r.categoryId = c.id
        WHERE r.id = :recurringTransactionId
    """)
    suspend fun getRecurringTransactionWithCategory(recurringTransactionId: Int): RecurringTransactionWithCategory?

    @Insert
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Query("""
        SELECT r.id, r.name, r.amount, r.categoryId, r.daysOfMonth, 
               r.startDate, r.isActive, r.description,
               c.name as categoryName, c.type as categoryType,
               c.emoji as categoryEmoji
        FROM recurring_transactions r
        JOIN categories c ON r.categoryId = c.id
        WHERE r.isActive = 1
        ORDER BY r.name ASC
    """)
    suspend fun getRecurringTransactionsWithCategory(): List<RecurringTransactionWithCategory>

    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE name = :name 
        AND startDate = :startDate 
        LIMIT 1
    """)
    suspend fun getRecurringByNameAndStartDate(
        name: String,
        startDate: LocalDate
    ): RecurringTransaction?
}