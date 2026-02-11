package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medapp.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settingsEntity: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String = SettingsEntity.SINGLETON_ID): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    fun observeById(id: String = SettingsEntity.SINGLETON_ID): Flow<SettingsEntity?>
}
