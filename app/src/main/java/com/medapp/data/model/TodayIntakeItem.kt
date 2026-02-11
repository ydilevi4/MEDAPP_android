package com.medapp.data.model

data class TodayIntakeItem(
    val id: String,
    val medicineName: String,
    val plannedAt: Long,
    val status: String,
    val pillCountPlanned: Double,
    val realDoseMgPlanned: Int,
    val packageIdPlanned: String
)
