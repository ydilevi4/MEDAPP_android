package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medapp.data.entity.PillPackageEntity

@Dao
interface PillPackageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pillPackageEntity: PillPackageEntity)

    @Update
    suspend fun update(pillPackageEntity: PillPackageEntity)

    @Query("SELECT * FROM pill_packages WHERE medicineId = :medicineId AND isCurrent = 1 LIMIT 1")
    suspend fun getCurrentForMedicine(medicineId: String): PillPackageEntity?

    @Query("SELECT * FROM pill_packages WHERE medicineId = :medicineId ORDER BY createdAt DESC")
    suspend fun getForMedicine(medicineId: String): List<PillPackageEntity>

    @Query("SELECT * FROM pill_packages WHERE isCurrent = 1")
    suspend fun getAllCurrent(): List<PillPackageEntity>

    @Query("SELECT * FROM pill_packages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PillPackageEntity?
}
