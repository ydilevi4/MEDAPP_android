package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medapp.data.entity.MedicineEntity
import com.medapp.data.model.MedicineListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MedicineEntity)

    @Query("SELECT id, name, targetDoseMg, isActive FROM medicines ORDER BY createdAt DESC")
    fun observeMedicines(): Flow<List<MedicineListItem>>

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getActiveMedicines(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MedicineEntity?
}
