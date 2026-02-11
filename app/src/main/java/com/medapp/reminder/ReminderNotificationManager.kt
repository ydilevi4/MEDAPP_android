package com.medapp.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medapp.MainActivity
import com.medapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderNotificationManager(
    private val context: Context
) {
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            ReminderConstants.CHANNEL_ID,
            ReminderConstants.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    fun showOverdueNotification(items: List<OverdueIntakeNotificationItem>) {
        if (items.isEmpty()) {
            dismiss()
            return
        }

        val summary = items.take(3).joinToString(separator = ", ") { item ->
            "${item.medicineName} (${formatTime(item.plannedAt)})"
        }
        val bigText = items.joinToString(separator = "\n") { item ->
            "• ${item.medicineName} (${formatTime(item.plannedAt)})"
        }

        val earliest = items.first()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(ReminderConstants.EXTRA_OPEN_TAB, ReminderConstants.TAB_TODAY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionIntent = Intent(context, MarkTakenReceiver::class.java).apply {
            action = ReminderConstants.ACTION_MARK_TAKEN
            putExtra(ReminderConstants.EXTRA_INTAKE_ID, earliest.intakeId)
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to take your medicine")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Taken (next)", actionPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ReminderConstants.NOTIFICATION_ID, notification)
        }.onFailure {
            Log.e("ReminderNotification", "Unable to post notification", it)
        }
    }

    fun dismiss() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(ReminderConstants.NOTIFICATION_ID)
        }.onFailure {
            Log.e("ReminderNotification", "Unable to dismiss notification", it)
        }
    }

    private fun formatTime(timeMillis: Long): String = timeFormatter.format(Date(timeMillis))
}
