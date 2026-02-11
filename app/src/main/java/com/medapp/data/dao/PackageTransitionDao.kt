package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medapp.data.entity.PackageTransitionEntity

@Dao
interface PackageTransitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transition: PackageTransitionEntity)

    @Update
    suspend fun update(transition: PackageTransitionEntity)

    @Query("SELECT * FROM package_transitions WHERE medicineId = :medicineId LIMIT 1")
    suspend fun getForMedicine(medicineId: String): PackageTransitionEntity?

    @Query("DELETE FROM package_transitions WHERE medicineId = :medicineId")
    suspend fun deleteForMedicine(medicineId: String)
}
