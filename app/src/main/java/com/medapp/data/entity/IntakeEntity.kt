package com.medapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intakes",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId"), Index("plannedAt")]
)
data class IntakeEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val plannedAt: Long,
    val status: String,
    val pillCountPlanned: Double,
    val realDoseMgPlanned: Int,
    val packageIdPlanned: String,
    val googleTaskId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
