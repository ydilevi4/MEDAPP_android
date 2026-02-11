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
import com.medapp.domain.model.LowStockMedicineItem
import kotlin.math.ceil

class LowStockNotificationManager(
    private val context: Context
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            ReminderConstants.LOW_STOCK_CHANNEL_ID,
            ReminderConstants.LOW_STOCK_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    fun showLowStockNotification(items: List<LowStockMedicineItem>) {
        if (items.isEmpty()) {
            dismiss()
            return
        }

        val short = items.take(3).joinToString(", ") { it.medicineName } + if (items.size > 3) ", …" else ""
        val bigText = items.joinToString("\n") {
            "• ${it.medicineName}: ~${ceil(it.estimatedDaysRemaining)} days remaining"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(ReminderConstants.EXTRA_OPEN_TAB, ReminderConstants.TAB_LOW_STOCK)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderConstants.LOW_STOCK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medicines running low")
            .setContentText(short)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Confirm purchase", openPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ReminderConstants.LOW_STOCK_NOTIFICATION_ID, notification)
        }.onFailure {
            Log.e("LowStockNotification", "Unable to post low-stock notification", it)
        }
    }

    fun dismiss() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(ReminderConstants.LOW_STOCK_NOTIFICATION_ID)
        }.onFailure {
            Log.e("LowStockNotification", "Unable to dismiss low-stock notification", it)
        }
    }
}
