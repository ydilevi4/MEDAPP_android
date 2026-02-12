package com.medapp.data.db

import android.content.Context
import androidx.room.Room

object AppDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "medapp.db"
            ).addMigrations(DbMigrations.MIGRATION_4_5).build().also {
                instance = it
            }
        }
    }
}
