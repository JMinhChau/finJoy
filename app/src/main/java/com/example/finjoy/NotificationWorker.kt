package com.example.finjoy

import android.content.Context
import android.util.Log
import androidx.work.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            "settings", Context.MODE_PRIVATE
        )
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        if (notificationsEnabled) {
            val message = inputData.getString("notification_message")
                ?: "Don't forget to log your daily transactions!"
            NotificationHelper(applicationContext).showNotification(message)
        }

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Cancel any existing work
            workManager.cancelAllWorkByTag("daily_notification")

            // For testing - show notification after 15 seconds
            scheduleTestNotification(context)

            // Schedule regular notifications
            scheduleNotification(context, 9, 0,  // 9 AM
                "Good morning! Have a great day ahead. Don't forget to track your expenses!")
            scheduleNotification(context, 12, 0,  // 12 PM (noon) - Fixed from 11 PM
                "Time for a midday check - caught all your transactions so far?")
            scheduleNotification(context, 20, 0,  // 8 PM
                "Evening reminder: Have you logged all your transactions for today?")
        }

        private fun scheduleTestNotification(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val inputData = Data.Builder()
                .putString("notification_message", "Track Your Expenses")
                .build()

            val testRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(15, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("test_notification")
                .build()

            workManager.enqueue(testRequest)
            Log.d("NotificationWorker", "Scheduled test notification for 15s from now")
        }

        private fun scheduleNotification(context: Context, hour: Int, minute: Int, message: String) {
            val workManager = WorkManager.getInstance(context)

            // Calculate initial delay
            val currentDateTime = LocalDateTime.now()
            var scheduledTime = LocalDateTime.of(
                currentDateTime.toLocalDate(),
                LocalTime.of(hour, minute)
            )

            // If the time has already passed today, schedule for tomorrow
            if (currentDateTime.isAfter(scheduledTime)) {
                scheduledTime = scheduledTime.plusDays(1)
            }

            val delayInMinutes = java.time.Duration.between(
                currentDateTime,
                scheduledTime
            ).toMinutes()

            // Create input data with the specific message
            val inputData = Data.Builder()
                .putString("notification_message", message)
                .build()

            // Schedule daily repeating notification
            val dailyWork = PeriodicWorkRequestBuilder<NotificationWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("daily_notification")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "notification_${hour}_${minute}",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWork
            )

            Log.d("NotificationWorker",
                "Scheduled notification for $hour:${String.format("%02d", minute)} with message: $message")
        }
    }
}