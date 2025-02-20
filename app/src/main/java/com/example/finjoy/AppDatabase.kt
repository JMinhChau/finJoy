package com.example.finjoy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class,
        Category::class,
        RecurringTransaction::class,
        RecurringTransactionHistory::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun recurringTransactionHistoryDao(): RecurringTransactionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val TAG = "AppDatabase"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transactions"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        categoryId INTEGER NOT NULL,
                        daysOfMonth TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        description TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY (categoryId) REFERENCES categories (id)
                            ON DELETE RESTRICT
                    )
                """)

                // Copy data from old table to new table if it exists
                database.execSQL("""
                    INSERT INTO recurring_transactions (
                        id, name, amount, categoryId, daysOfMonth, 
                        startDate, isActive, description
                    )
                    SELECT id, name, amount, categoryId, daysOfMonth,
                           startDate, isActive, description 
                    FROM fixed_costs
                """)

                // Drop old table
                database.execSQL("DROP TABLE IF EXISTS fixed_costs")

                // Create indices for better performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recurring_transactions_categoryId ON recurring_transactions(categoryId)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_transaction_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recurringTransactionId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        startDate TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY (recurringTransactionId) 
                            REFERENCES recurring_transactions (id)
                            ON DELETE CASCADE
                    )
                """)

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recurring_history_transaction_id " +
                            "ON recurring_transaction_history(recurringTransactionId)"
                )
            }
        }
    }
}