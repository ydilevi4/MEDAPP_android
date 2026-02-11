package com.medapp.domain.usecase

import androidx.room.withTransaction
import com.medapp.data.db.AppDatabase

class MarkIntakeMissedUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(intakeId: String) {
        database.withTransaction {
            val intake = database.intakeDao().getById(intakeId) ?: return@withTransaction
            if (intake.status != "PLANNED") return@withTransaction
            database.intakeDao().updateStatusById(
                intakeId = intake.id,
                status = "MISSED",
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
