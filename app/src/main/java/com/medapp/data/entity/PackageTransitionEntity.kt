package com.medapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "package_transitions",
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
data class PackageTransitionEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val oldPackageId: String,
    val newPackageId: String,
    val oldPillsLeft: Double,
    val oldPillsConsumed: Double,
    val createdAt: Long
)
