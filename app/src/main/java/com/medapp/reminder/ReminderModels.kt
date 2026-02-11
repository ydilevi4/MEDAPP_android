package com.medapp.reminder

data class OverdueIntakeNotificationItem(
    val intakeId: String,
    val medicineId: String,
    val medicineName: String,
    val plannedAt: Long
)
