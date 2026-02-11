package com.medapp.domain.usecase

import androidx.room.withTransaction
import com.medapp.data.db.AppDatabase

class MarkIntakeCompletedUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(intakeId: String) {
        database.withTransaction {
            val intake = database.intakeDao().getById(intakeId) ?: return@withTransaction
            if (intake.status != "PLANNED") return@withTransaction

            val currentPackage = database.pillPackageDao().getById(intake.packageIdPlanned) ?: return@withTransaction
            val now = System.currentTimeMillis()

            database.intakeDao().update(
                intake.copy(status = "COMPLETED", updatedAt = now)
            )
            database.pillPackageDao().update(
                currentPackage.copy(
                    pillsRemaining = (currentPackage.pillsRemaining - intake.pillCountPlanned).coerceAtLeast(0.0),
                    updatedAt = now
                )
            )
        }
    }
}
