package com.medapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetDoseMg: Int,
    val scheduleType: String,
    val anchorsJson: String,
    val intervalHours: Int?,
    val firstDoseTime: String?,
    val durationType: String,
    val durationDays: Int?,
    val totalPillsToTake: Double?,
    val courseDays: Int?,
    val restDays: Int?,
    val cyclesCount: Int?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
