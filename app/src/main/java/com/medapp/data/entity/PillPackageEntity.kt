package com.medapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pill_packages",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId")]
)
data class PillPackageEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val pillDoseMg: Int,
    val divisibleHalf: Boolean,
    val pillsInPack: Double,
    val pillsRemaining: Double,
    val purchaseLink: String?,
    val warnWhenEnding: Boolean,
    val isCurrent: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
