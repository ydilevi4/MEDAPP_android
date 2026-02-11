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

            val transitionDao = database.packageTransitionDao()
            val transition = transitionDao.getForMedicine(intake.medicineId)
            val now = System.currentTimeMillis()

            database.intakeDao().update(
                intake.copy(status = "COMPLETED", updatedAt = now)
            )

            if (transition != null && transition.oldPillsConsumed < transition.oldPillsLeft - 1e-9) {
                val oldPackage = database.pillPackageDao().getById(transition.oldPackageId)
                if (oldPackage != null) {
                    database.pillPackageDao().update(
                        oldPackage.copy(
                            pillsRemaining = (oldPackage.pillsRemaining - intake.pillCountPlanned).coerceAtLeast(0.0),
                            updatedAt = now
                        )
                    )
                }
                val updatedConsumed = (transition.oldPillsConsumed + intake.pillCountPlanned).coerceAtMost(transition.oldPillsLeft)
                transitionDao.update(transition.copy(oldPillsConsumed = updatedConsumed))
            } else {
                val currentPackage = database.pillPackageDao().getById(intake.packageIdPlanned)
                    ?: database.pillPackageDao().getCurrentForMedicine(intake.medicineId)
                    ?: return@withTransaction

                database.pillPackageDao().update(
                    currentPackage.copy(
                        pillsRemaining = (currentPackage.pillsRemaining - intake.pillCountPlanned).coerceAtLeast(0.0),
                        updatedAt = now
                    )
                )
            }
        }
    }
}
