package com.medapp.ui.main

import android.content.Context
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.domain.usecase.ConfirmPackagePurchaseUseCase
import com.medapp.domain.usecase.CreateMedicineUseCase
import com.medapp.domain.usecase.DoseCalculationUseCase
import com.medapp.domain.usecase.EnsureSettingsUseCase
import com.medapp.domain.usecase.GenerateIntakesUseCase
import com.medapp.domain.usecase.MarkIntakeCompletedUseCase

class AppContainer(context: Context) {
    val database = AppDatabaseProvider.get(context)

    private val doseCalculationUseCase = DoseCalculationUseCase()
    val ensureSettingsUseCase = EnsureSettingsUseCase(database.settingsDao())
    val createMedicineUseCase = CreateMedicineUseCase(database, ensureSettingsUseCase)
    val generateIntakesUseCase = GenerateIntakesUseCase(
        medicineDao = database.medicineDao(),
        intakeDao = database.intakeDao(),
        pillPackageDao = database.pillPackageDao(),
        packageTransitionDao = database.packageTransitionDao(),
        doseCalculationUseCase = doseCalculationUseCase,
        ensureSettingsUseCase = ensureSettingsUseCase
    )
    val markIntakeCompletedUseCase = MarkIntakeCompletedUseCase(database)
    val confirmPackagePurchaseUseCase = ConfirmPackagePurchaseUseCase(database, generateIntakesUseCase)
    val doseUseCase = doseCalculationUseCase
}
