package com.medapp

import android.app.Application
import androidx.work.Configuration
import com.medapp.reminder.ReminderNotificationManager
import com.medapp.reminder.LowStockNotificationManager
import com.medapp.reminder.LowStockScheduler
import com.medapp.reminder.ReminderScheduler

class MedAppApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        ReminderNotificationManager(this).ensureChannel()
        LowStockNotificationManager(this).ensureChannel()
        ReminderScheduler.ensureScheduled(this)
        LowStockScheduler.ensureScheduled(this)
        LowStockScheduler.enqueueCheckOnStart(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
