package com.medapp.domain.usecase

import androidx.room.withTransaction
import com.medapp.data.db.AppDatabase
import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.PillPackageEntity

class CreateMedicineUseCase(
    private val database: AppDatabase,
    private val ensureSettingsUseCase: EnsureSettingsUseCase
) {
    data class Params(
        val medicine: MedicineEntity,
        val pillPackage: PillPackageEntity
    )

    suspend operator fun invoke(params: Params) {
        ensureSettingsUseCase()
        database.withTransaction {
            database.medicineDao().insert(params.medicine)
            database.pillPackageDao().insert(
                params.pillPackage.copy(
                    isCurrent = true,
                    pillsRemaining = params.pillPackage.pillsInPack
                )
            )
        }
    }
}
