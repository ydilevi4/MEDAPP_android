package com.medapp

import android.app.Application
import androidx.work.Configuration
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.reminder.ReminderNotificationManager
import com.medapp.reminder.GoogleTasksSyncScheduler
import com.medapp.reminder.LowStockNotificationManager
import com.medapp.reminder.LowStockScheduler
import com.medapp.reminder.ReminderScheduler
import kotlinx.coroutines.runBlocking

class MedAppApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        ReminderNotificationManager(this).ensureChannel()
        LowStockNotificationManager(this).ensureChannel()
        ReminderScheduler.ensureScheduled(this)
        LowStockScheduler.ensureScheduled(this)
        GoogleTasksSyncScheduler.ensureScheduled(this)
        LowStockScheduler.enqueueCheckOnStart(this)
        val settings = runBlocking { AppDatabaseProvider.get(this@MedAppApplication).settingsDao().getById() }
        if (!settings?.googleAccountEmail.isNullOrBlank() && !settings?.googleTasksListId.isNullOrBlank()) {
            GoogleTasksSyncScheduler.enqueueOnStart(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
