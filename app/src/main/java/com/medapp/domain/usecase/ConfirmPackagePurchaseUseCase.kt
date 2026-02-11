package com.medapp.domain.usecase

import androidx.room.withTransaction
import com.medapp.data.db.AppDatabase
import com.medapp.data.entity.PackageTransitionEntity
import com.medapp.data.entity.PillPackageEntity
import java.util.UUID

class ConfirmPackagePurchaseUseCase(
    private val database: AppDatabase,
    private val generateIntakesUseCase: GenerateIntakesUseCase
) {
    data class Params(
        val medicineId: String,
        val oldPillsLeft: Double,
        val pillDoseMg: Int,
        val divisibleHalf: Boolean,
        val pillsInPack: Double,
        val purchaseLink: String?,
        val warnWhenEnding: Boolean
    )

    suspend operator fun invoke(params: Params) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            val previousCurrent = database.pillPackageDao().getCurrentForMedicine(params.medicineId) ?: return@withTransaction
            database.pillPackageDao().update(
                previousCurrent.copy(
                    isCurrent = false,
                    pillsRemaining = params.oldPillsLeft.coerceAtLeast(0.0),
                    updatedAt = now
                )
            )

            val newPackage = PillPackageEntity(
                id = UUID.randomUUID().toString(),
                medicineId = params.medicineId,
                pillDoseMg = params.pillDoseMg,
                divisibleHalf = params.divisibleHalf,
                pillsInPack = params.pillsInPack,
                pillsRemaining = params.pillsInPack,
                purchaseLink = params.purchaseLink,
                warnWhenEnding = params.warnWhenEnding,
                isCurrent = true,
                createdAt = now,
                updatedAt = now
            )
            database.pillPackageDao().insert(newPackage)

            val existingTransition = database.packageTransitionDao().getForMedicine(params.medicineId)
            val transition = PackageTransitionEntity(
                id = existingTransition?.id ?: UUID.randomUUID().toString(),
                medicineId = params.medicineId,
                oldPackageId = previousCurrent.id,
                newPackageId = newPackage.id,
                oldPillsLeft = params.oldPillsLeft.coerceAtLeast(0.0),
                oldPillsConsumed = 0.0,
                createdAt = existingTransition?.createdAt ?: now
            )
            database.packageTransitionDao().insert(transition)
        }

        generateIntakesUseCase.refreshFuturePlanned()
    }
}
