package com.medapp.ui.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.PillPackageEntity
import com.medapp.data.model.EqualDistanceRule
import com.medapp.data.util.AnchorJsonHelper
import com.medapp.domain.usecase.CreateMedicineUseCase
import com.medapp.domain.usecase.DoseCalculationUseCase
import com.medapp.domain.usecase.EnsureSettingsUseCase
import com.medapp.domain.usecase.GenerateIntakesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MedicinesViewModel(
    private val createMedicineUseCase: CreateMedicineUseCase,
    private val generateIntakesUseCase: GenerateIntakesUseCase,
    private val ensureSettingsUseCase: EnsureSettingsUseCase,
    private val doseCalculationUseCase: DoseCalculationUseCase,
    dao: com.medapp.data.dao.MedicineDao
) : ViewModel() {

    val medicines = dao.observeMedicines().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun calculateDose(targetDoseMg: Int, pillDoseMg: Int, divisibleHalf: Boolean, tieRule: EqualDistanceRule): DoseCalculationUseCase.Result {
        return doseCalculationUseCase(targetDoseMg, pillDoseMg, divisibleHalf, tieRule)
    }

    fun createMedicine(input: WizardInput, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (input.name.isBlank() || input.targetDoseMg <= 0 || input.pillDoseMg <= 0 || input.pillsInPack <= 0) {
                    return@launch
                }

                val now = System.currentTimeMillis()
                val medicineId = UUID.randomUUID().toString()
                val packageId = UUID.randomUUID().toString()

                createMedicineUseCase(
                    CreateMedicineUseCase.Params(
                        medicine = MedicineEntity(
                            id = medicineId,
                            name = input.name,
                            targetDoseMg = input.targetDoseMg,
                            scheduleType = input.scheduleType,
                            intakesPerDay = input.intakesPerDay.coerceIn(1, 6),
                            anchorsJson = AnchorJsonHelper.encode(input.anchors),
                            intervalHours = input.intervalHours,
                            firstDoseTime = input.firstDoseTime,
                            durationType = input.durationType,
                            durationDays = input.durationDays,
                            totalPillsToTake = input.totalPillsToTake,
                            courseDays = input.courseDays,
                            restDays = input.restDays,
                            cyclesCount = input.cyclesCount,
                            isActive = true,
                            createdAt = now,
                            updatedAt = now
                        ),
                        pillPackage = PillPackageEntity(
                            id = packageId,
                            medicineId = medicineId,
                            pillDoseMg = input.pillDoseMg,
                            divisibleHalf = input.divisibleHalf,
                            pillsInPack = input.pillsInPack,
                            pillsRemaining = input.pillsInPack,
                            purchaseLink = input.purchaseLink.takeIf { it.isNotBlank() },
                            warnWhenEnding = input.warnWhenEnding,
                            isCurrent = true,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                )
                generateIntakesUseCase()
                onDone()
            } finally {
                _isSaving.value = false
            }
        }
    }

    suspend fun loadTieRule(): EqualDistanceRule {
        val settings = ensureSettingsUseCase()
        return EqualDistanceRule.valueOf(settings.equalDistanceRule)
    }

    data class WizardInput(
        val name: String,
        val targetDoseMg: Int,
        val pillDoseMg: Int,
        val divisibleHalf: Boolean,
        val pillsInPack: Double,
        val purchaseLink: String,
        val warnWhenEnding: Boolean,
        val scheduleType: String,
        val intakesPerDay: Int,
        val anchors: List<String>,
        val intervalHours: Int?,
        val firstDoseTime: String?,
        val durationType: String,
        val durationDays: Int?,
        val totalPillsToTake: Double?,
        val courseDays: Int?,
        val restDays: Int?,
        val cyclesCount: Int?
    )

    class Factory(
        private val createMedicineUseCase: CreateMedicineUseCase,
        private val generateIntakesUseCase: GenerateIntakesUseCase,
        private val ensureSettingsUseCase: EnsureSettingsUseCase,
        private val doseCalculationUseCase: DoseCalculationUseCase,
        private val dao: com.medapp.data.dao.MedicineDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicinesViewModel(createMedicineUseCase, generateIntakesUseCase, ensureSettingsUseCase, doseCalculationUseCase, dao) as T
        }
    }
}
