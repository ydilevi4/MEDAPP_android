package com.medapp.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.domain.usecase.MarkIntakeMissedUseCase
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabaseProvider.get(applicationContext)
        val intakeDao = database.intakeDao()
        val notificationManager = ReminderNotificationManager(applicationContext)
        val markIntakeMissedUseCase = MarkIntakeMissedUseCase(database)

        return runCatching {
            notificationManager.ensureChannel()

            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val nowInstant = Instant.ofEpochMilli(now)
            val horizonStart = nowInstant.minus(7, ChronoUnit.DAYS).toEpochMilli()
            val horizonEnd = nowInstant.plus(90, ChronoUnit.DAYS).toEpochMilli()

            val overdue = intakeDao.getOverduePlannedIntakes(
                nowMillis = now,
                horizonStart = horizonStart,
                horizonEnd = horizonEnd
            )

            val latestPerMedicine = overdue
                .groupBy { it.medicineId }
                .mapNotNull { (_, items) -> items.maxByOrNull { it.plannedAt } }

            val idsToMiss = mutableSetOf<String>()
            overdue.groupBy { it.medicineId }.forEach { (_, items) ->
                val latest = items.maxByOrNull { it.plannedAt } ?: return@forEach
                items.filter { it.intakeId != latest.intakeId }.forEach { idsToMiss += it.intakeId }
            }

            for (item in latestPerMedicine) {
                val next = intakeDao.getNextIntakeForMedicine(item.medicineId, item.plannedAt)
                if (next != null && next.plannedAt <= now) {
                    idsToMiss += item.intakeId
                }
            }

            idsToMiss.forEach { intakeId ->
                markIntakeMissedUseCase(intakeId)
            }

            val refreshedOverdue = intakeDao.getOverduePlannedIntakes(
                nowMillis = now,
                horizonStart = horizonStart,
                horizonEnd = horizonEnd
            )
            val notifyItems = refreshedOverdue
                .groupBy { it.medicineId }
                .mapNotNull { (_, items) -> items.maxByOrNull { it.plannedAt } }
                .sortedBy { it.plannedAt }

            notificationManager.showOverdueNotification(notifyItems)
            Result.success()
        }.getOrElse { throwable ->
            Log.e("ReminderWorker", "Failed while checking reminders", throwable)
            Result.retry()
        }
    }
}
