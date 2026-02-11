package com.medapp.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.domain.usecase.EnsureSettingsUseCase
import com.medapp.domain.usecase.LowStockEstimator

class LowStockWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabaseProvider.get(applicationContext)
        val ensureSettingsUseCase = EnsureSettingsUseCase(db.settingsDao())
        val estimator = LowStockEstimator(
            medicineDao = db.medicineDao(),
            pillPackageDao = db.pillPackageDao(),
            intakeDao = db.intakeDao()
        )
        val notificationManager = LowStockNotificationManager(applicationContext)

        return runCatching {
            notificationManager.ensureChannel()
            val settings = ensureSettingsUseCase()
            val items = estimator.estimate(settings)
            notificationManager.showLowStockNotification(items)
            Result.success()
        }.getOrElse {
            Log.e("LowStockWorker", "Failed low-stock check", it)
            Result.retry()
        }
    }
}
