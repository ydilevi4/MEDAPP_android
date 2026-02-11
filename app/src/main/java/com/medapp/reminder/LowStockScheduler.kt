package com.medapp.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object LowStockScheduler {
    fun ensureScheduled(context: Context) {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextRun).toMinutes().coerceAtLeast(0)

        val periodic = PeriodicWorkRequestBuilder<LowStockWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderConstants.LOW_STOCK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }

    fun enqueueCheckOnStart(context: Context) {
        val oneTime = OneTimeWorkRequestBuilder<LowStockWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderConstants.LOW_STOCK_START_CHECK_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTime
        )
    }
}
