package com.example.finjoy

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

class DataExporter(private val context: Context) {
    private val db = AppDatabase.getInstance(context)

    suspend fun exportData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val transactions = db.transactionDao().getTransactionsWithCategory()
                val recurring = db.recurringTransactionDao().getRecurringTransactionsWithCategory()

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    // Write transactions
                    output.write("### TRANSACTIONS ###\n".toByteArray())
                    output.write("date,amount,category,description\n".toByteArray())
                    transactions.forEach { transaction ->
                        val line = "${transaction.date},${transaction.amount}," +
                                "${transaction.categoryName},${transaction.description}\n"
                        output.write(line.toByteArray())
                    }

                    // Write recurring transactions
                    output.write("\n### RECURRING ###\n".toByteArray())
                    output.write("name,amount,category,days,startDate,description\n".toByteArray())
                    recurring.forEach { recurringTx ->
                        val line = "${recurringTx.name},${recurringTx.amount}," +
                                "${recurringTx.categoryName},${recurringTx.daysOfMonth}," +
                                "${recurringTx.startDate},${recurringTx.description}\n"
                        output.write(line.toByteArray())
                    }
                }
            } catch (e: Exception) {
                Log.e("DataExporter", "Error exporting data", e)
                throw e
            }
        }
    }

    suspend fun importData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val reader = BufferedReader(InputStreamReader(input))
                    var currentLine: String? = null
                    var currentSection = ""

                    while (reader.readLine().also { currentLine = it } != null) {
                        when {
                            currentLine?.startsWith("### ") == true -> {
                                currentSection = currentLine ?: ""
                                reader.readLine() // Skip header line
                            }
                            currentLine?.isNotBlank() == true -> {
                                when (currentSection) {
                                    "### TRANSACTIONS ###" ->
                                        currentLine?.let { importTransaction(it) }
                                    "### RECURRING ###" ->
                                        currentLine?.let { importRecurringTransaction(it) }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DataExporter", "Error importing data", e)
                throw e
            }
        }
    }

    private suspend fun importTransaction(line: String) {
        try {
            val parts = line.split(",")
            if (parts.size >= 4) {
                // Check for duplicate before inserting
                val existingTransaction = db.transactionDao().getTransactionForDateAndDesc(
                    date = LocalDate.parse(parts[0]),
                    description = parts[3],
                    amount = parts[1].toDouble()
                )

                // Only insert if no duplicate exists
                if (existingTransaction == null) {
                    val categoryType = if (parts[1].toDouble() >= 0)
                        Category.TransactionType.INCOME
                    else
                        Category.TransactionType.EXPENSE

                    val category = getOrCreateCategory(parts[2], categoryType)
                    val transaction = Transaction(
                        date = LocalDate.parse(parts[0]),
                        amount = parts[1].toDouble(),
                        categoryId = category.id,
                        description = parts[3]
                    )
                    db.transactionDao().insertTransaction(transaction)
                }
            }
        } catch (e: Exception) {
            Log.e("DataExporter", "Error importing transaction: $line", e)
        }
    }

    private suspend fun importRecurringTransaction(line: String) {
        try {
            val parts = line.split(",")
            if (parts.size >= 6) {
                // Check for duplicate using name and start date
                val existingRecurring = db.recurringTransactionDao()
                    .getRecurringByNameAndStartDate(
                        name = parts[0],
                        startDate = LocalDate.parse(parts[4])
                    )

                // Only insert if no duplicate exists
                if (existingRecurring == null) {
                    val categoryType = if (parts[1].toDouble() >= 0)
                        Category.TransactionType.INCOME
                    else
                        Category.TransactionType.EXPENSE

                    val category = getOrCreateCategory(parts[2], categoryType)
                    val recurring = RecurringTransaction(
                        name = parts[0],
                        amount = parts[1].toDouble(),
                        categoryId = category.id,
                        daysOfMonth = parts[3],
                        startDate = LocalDate.parse(parts[4]),
                        description = parts[5]
                    )
                    db.recurringTransactionDao().insertRecurringTransaction(recurring)
                }
            }
        } catch (e: Exception) {
            Log.e("DataExporter", "Error importing recurring transaction: $line", e)
        }
    }

    private suspend fun getOrCreateCategory(name: String, type: Category.TransactionType): Category {
        return try {
            val categories = db.categoryDao().getAllCategories()
            categories.find { it.name == name } ?: run {
                val category = Category(
                    name = name,
                    type = type,
                    emoji = "📝"  // Default emoji
                )
                val newCategory = db.categoryDao().insertCategory(category)
                category.copy(id = newCategory.toInt())
            }
        } catch (e: Exception) {
            Log.e("DataExporter", "Error getting/creating category: $name", e)
            throw e
        }
    }
}