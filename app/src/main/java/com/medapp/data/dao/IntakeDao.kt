package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.model.TodayIntakeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(intakes: List<IntakeEntity>)

    @Update
    suspend fun update(intakeEntity: IntakeEntity)

    @Query("SELECT * FROM intakes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IntakeEntity?

    @Query(
        """
        SELECT i.id, m.name AS medicineName, i.plannedAt, i.status, i.pillCountPlanned, i.realDoseMgPlanned, i.packageIdPlanned
        FROM intakes i
        INNER JOIN medicines m ON m.id = i.medicineId
        WHERE i.plannedAt BETWEEN :dayStart AND :dayEnd
        ORDER BY i.plannedAt ASC
        """
    )
    fun observeTodayIntakes(dayStart: Long, dayEnd: Long): Flow<List<TodayIntakeItem>>

    @Query("SELECT COUNT(*) FROM intakes WHERE medicineId = :medicineId AND plannedAt BETWEEN :dayStart AND :dayEnd")
    suspend fun countForMedicineInRange(medicineId: String, dayStart: Long, dayEnd: Long): Int

    @Query("SELECT plannedAt FROM intakes WHERE medicineId = :medicineId AND plannedAt BETWEEN :fromMillis AND :toMillis")
    suspend fun getPlannedAtInRange(medicineId: String, fromMillis: Long, toMillis: Long): List<Long>

    @Query("SELECT COALESCE(SUM(pillCountPlanned), 0) FROM intakes WHERE medicineId = :medicineId")
    suspend fun getPlannedPillsSum(medicineId: String): Double
}
