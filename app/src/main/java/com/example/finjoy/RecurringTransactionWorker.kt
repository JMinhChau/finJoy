package com.example.finjoy

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("RecurringWorker", "Starting to process recurring transactions")
        val db = AppDatabase.getInstance(applicationContext)
        val today = LocalDate.now()

        try {
            // Get recurring transactions
            val recurringTransactions = db.recurringTransactionDao()
                .getRecurringTransactionsWithCategory()

            Log.d("RecurringWorker", "Found ${recurringTransactions.size} recurring transactions")

            // Process each recurring transaction
            recurringTransactions.forEach { recurring ->
                val days = recurring.daysOfMonth.split(",")
                    .map { it.trim().toInt() }

                Log.d("RecurringWorker", "Processing ${recurring.name} for days: $days")

                // If today's day is in the recurring days
                if (days.contains(today.dayOfMonth)) {
                    // Check if we already created this transaction
                    val existingTransaction = db.transactionDao()
                        .getRecurringTransactionForDate(recurring.id, today)

                    if (existingTransaction == null) {
                        // Create the transaction
                        val transaction = Transaction(
                            categoryId = recurring.categoryId,
                            amount = recurring.amount,
                            description = "Recurring: ${recurring.name}",
                            date = today
                        )
                        db.transactionDao().insertTransaction(transaction)
                        Log.d("RecurringWorker", "Created transaction for ${recurring.name}")
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("RecurringWorker", "Error processing recurring transactions", e)
            return Result.retry()
        }
    }
}