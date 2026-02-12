package com.medapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PackageTransitionDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.dao.SettingsDao
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.PackageTransitionEntity
import com.medapp.data.entity.PillPackageEntity
import com.medapp.data.entity.SettingsEntity

@Database(
    entities = [
        MedicineEntity::class,
        PillPackageEntity::class,
        IntakeEntity::class,
        SettingsEntity::class,
        PackageTransitionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun pillPackageDao(): PillPackageDao
    abstract fun intakeDao(): IntakeDao
    abstract fun settingsDao(): SettingsDao
    abstract fun packageTransitionDao(): PackageTransitionDao
}
