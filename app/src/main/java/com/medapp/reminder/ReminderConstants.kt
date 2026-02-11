package com.medapp.reminder

object ReminderConstants {
    const val CHANNEL_ID = "mpt_reminders"
    const val CHANNEL_NAME = "Medication reminders"
    const val UNIQUE_WORK_NAME = "reminder_worker"

    const val LOW_STOCK_CHANNEL_ID = "mpt_low_stock"
    const val LOW_STOCK_CHANNEL_NAME = "Low stock warnings"
    const val LOW_STOCK_WORK_NAME = "low_stock_worker"
    const val LOW_STOCK_START_CHECK_WORK_NAME = "low_stock_check_on_start"

    const val NOTIFICATION_ID = 1001
    const val LOW_STOCK_NOTIFICATION_ID = 1002

    const val ACTION_MARK_TAKEN = "com.medapp.reminder.ACTION_MARK_TAKEN"
    const val EXTRA_INTAKE_ID = "extra_intake_id"
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val TAB_TODAY = "today"
    const val TAB_LOW_STOCK = "low_stock"
}
