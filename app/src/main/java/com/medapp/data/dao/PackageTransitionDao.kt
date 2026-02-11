package com.medapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.medapp.data.entity.PackageTransitionEntity

@Dao
interface PackageTransitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transition: PackageTransitionEntity)
}
