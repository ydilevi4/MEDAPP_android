package com.medapp.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.domain.usecase.MarkIntakeCompletedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class MarkTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val intakeId = intent?.getStringExtra(ReminderConstants.EXTRA_INTAKE_ID)
        if (intakeId.isNullOrBlank()) {
            Log.w("MarkTakenReceiver", "Missing intake id")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val database = AppDatabaseProvider.get(context.applicationContext)
                val completeUseCase = MarkIntakeCompletedUseCase(database)
                completeUseCase(intakeId)

                val now = System.currentTimeMillis()
                val horizonStart = Instant.ofEpochMilli(now).minus(7, ChronoUnit.DAYS).toEpochMilli()
                val horizonEnd = Instant.ofEpochMilli(now).plus(90, ChronoUnit.DAYS).toEpochMilli()
                val overdue = database.intakeDao().getOverduePlannedIntakes(now, horizonStart, horizonEnd)
                    .groupBy { it.medicineId }
                    .mapNotNull { (_, items) -> items.maxByOrNull { it.plannedAt } }
                    .sortedBy { it.plannedAt }

                val notificationManager = ReminderNotificationManager(context.applicationContext)
                notificationManager.showOverdueNotification(overdue)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Marked as taken", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Log.e("MarkTakenReceiver", "Failed to mark intake taken", it)
            }
            pendingResult.finish()
        }
    }
}
