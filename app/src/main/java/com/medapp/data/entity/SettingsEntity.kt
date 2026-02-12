package com.medapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val wakeTime: String,
    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String,
    val sleepTime: String,
    val equalDistanceRule: String,
    val lowStockWarningEnabled: Boolean,
    val lowStockWarningDays: Int,
    val language: String,
    val googleAccountEmail: String? = null,
    val googleTasksListId: String? = null,
    val googleAuthConnectedAt: Long? = null
) {
    companion object {
        const val SINGLETON_ID = "singleton"
    }
}
