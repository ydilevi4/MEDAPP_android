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

            val plannedPillCount = intake.pillCountPlanned.coerceAtLeast(0.0)
            val plannedPackage = database.pillPackageDao().getById(intake.packageIdPlanned)
            if (plannedPackage == null) {
                android.util.Log.w(
                    "MarkIntakeCompletedUseCase",
                    "Planned package ${intake.packageIdPlanned} not found for intake ${intake.id}; falling back to current package"
                )
            }
            val targetPackage = plannedPackage
                ?: database.pillPackageDao().getCurrentForMedicine(intake.medicineId)
                ?: return@withTransaction

            database.pillPackageDao().update(
                targetPackage.copy(
                    pillsRemaining = (targetPackage.pillsRemaining - plannedPillCount).coerceAtLeast(0.0),
                    updatedAt = now
                )
            )

            val targetAccounting = completionAccounting(
                targetPackageId = targetPackage.id,
                oldPackageId = transition?.oldPackageId,
                plannedPillCount = plannedPillCount
            )
            if (transition != null && targetAccounting.shouldUpdateOldConsumed) {
                val updatedConsumed = (transition.oldPillsConsumed + plannedPillCount).coerceAtMost(transition.oldPillsLeft)
                transitionDao.update(transition.copy(oldPillsConsumed = updatedConsumed))
            }
        }
    }


    internal data class CompletionAccounting(
        val pillDelta: Double,
        val shouldUpdateOldConsumed: Boolean
    )

    companion object {
        internal fun completionAccounting(targetPackageId: String, oldPackageId: String?, plannedPillCount: Double): CompletionAccounting {
            val pillDelta = plannedPillCount.coerceAtLeast(0.0)
            return CompletionAccounting(
                pillDelta = pillDelta,
                shouldUpdateOldConsumed = oldPackageId != null && targetPackageId == oldPackageId
            )
        }
    }
}
