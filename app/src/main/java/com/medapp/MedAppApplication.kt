package com.medapp

import android.app.Application
import androidx.work.Configuration
import com.medapp.reminder.ReminderNotificationManager
import com.medapp.reminder.ReminderScheduler

class MedAppApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        ReminderNotificationManager(this).ensureChannel()
        ReminderScheduler.ensureScheduled(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
