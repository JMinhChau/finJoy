package com.example.finjoy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.example.finjoy.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf("android.permission.POST_NOTIFICATIONS"),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        // Initialize database
        db = AppDatabase.getInstance(applicationContext)

        // Setup Navigation Controller with the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect Bottom Navigation with Navigation Controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Initialize default categories if needed
        setupDefaultCategories()

        // Setup fixed costs automatic processing
        setupRecurringTransactionWorker()

        // Check notifications - default to true
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notifications_enabled", true)) {
            NotificationWorker.schedule(applicationContext)
        }
    }

    private fun setupRecurringTransactionWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            15, TimeUnit.MINUTES // Check every 15 minutes
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "RecurringTransactionWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

        // Also run once immediately
        val oneTimeWork = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueue(oneTimeWork)
    }

    private fun setupDefaultCategories() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()

            if (categories.isEmpty()) {
                val defaultCategories = listOf(
                    Category(id = 1, name = "Food", type = Category.TransactionType.EXPENSE, emoji = "🍽️"),
                    Category(id = 2, name = "Transport", type = Category.TransactionType.EXPENSE, emoji = "🚗"),
                    Category(id = 3, name = "Shopping", type = Category.TransactionType.EXPENSE, emoji = "🛍️"),
                    Category(id = 4, name = "Bills", type = Category.TransactionType.EXPENSE, emoji = "📄"),
                    Category(id = 5, name = "Entertainment", type = Category.TransactionType.EXPENSE, emoji = "🎮"),
                    Category(id = 6, name = "Healthcare", type = Category.TransactionType.EXPENSE, emoji = "🏥"),
                    Category(id = 7, name = "Education", type = Category.TransactionType.EXPENSE, emoji = "📚"),
                    Category(id = 8, name = "Salary", type = Category.TransactionType.INCOME, emoji = "💰"),
                    Category(id = 9, name = "Freelance", type = Category.TransactionType.INCOME, emoji = "💼"),
                    Category(id = 10, name = "Investment", type = Category.TransactionType.INCOME, emoji = "📈")
                )
                defaultCategories.forEach { category ->
                    db.categoryDao().insertCategory(category)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    // In MainActivity.kt
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty()) {
                    when {
                        grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                            // Permission granted, schedule notifications
                            NotificationWorker.schedule(applicationContext)
                        }
                        shouldShowRequestPermissionRationale(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) -> {
                            // User denied but didn't check "Don't ask again"
                            showPermissionExplanationDialog()
                        }
                        else -> {
                            // User denied and checked "Don't ask again"
                            showSettingsDialog()
                        }
                    }

                    // Update preferences to match actual permission status
                    val isPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("notifications_enabled", isPermissionGranted)
                        .apply()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Notifications Permission")
            .setMessage("We need notification permission to remind you about logging your daily transactions. This helps you maintain accurate financial records.")
            .setPositiveButton("Try Again") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf("android.permission.POST_NOTIFICATIONS"),
                        NOTIFICATION_PERMISSION_CODE
                    )
                }
            }
            .setNegativeButton("No Thanks") { _, _ ->
                Toast.makeText(
                    this,
                    "Notifications disabled. You can enable them later in Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Notifications Disabled")
            .setMessage("To enable notifications, please grant permission in your device settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Not Now", null)
            .show()
    }
}