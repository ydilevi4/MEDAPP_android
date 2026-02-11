package com.medapp.domain.usecase

import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.model.EqualDistanceRule
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class GenerateIntakesUseCase(
    private val medicineDao: MedicineDao,
    private val intakeDao: IntakeDao,
    private val pillPackageDao: PillPackageDao,
    private val doseCalculationUseCase: DoseCalculationUseCase,
    private val ensureSettingsUseCase: EnsureSettingsUseCase
) {
    suspend operator fun invoke() {
        val settings = ensureSettingsUseCase()
        val tieRule = EqualDistanceRule.valueOf(settings.equalDistanceRule)
        val zone = ZoneId.systemDefault()
        val nowDateTime = LocalDateTime.now(zone)
        val horizonEndDateTime = nowDateTime.plusDays(HORIZON_DAYS)
        val fromMillis = nowDateTime.atZone(zone).toInstant().toEpochMilli()
        val toMillis = horizonEndDateTime.atZone(zone).toInstant().toEpochMilli()

        val activeMedicines = medicineDao.getActiveMedicines()

        for (medicine in activeMedicines) {
            val currentPackage = pillPackageDao.getCurrentForMedicine(medicine.id) ?: continue
            val dose = doseCalculationUseCase(
                targetDoseMg = medicine.targetDoseMg,
                pillDoseMg = currentPackage.pillDoseMg,
                divisibleHalf = currentPackage.divisibleHalf,
                tieRule = tieRule
            )

            val existingPlannedAt = intakeDao.getPlannedAtInRange(medicine.id, fromMillis, toMillis).toSet()
            val alreadyPlannedPills = intakeDao.getPlannedPillsSum(medicine.id)
            val generatedMillis = IntakeGenerationHelper.generatePlannedAtMillis(
                IntakeGenerationHelper.GeneratePlanParams(
                    medicine = medicine,
                    settings = settings,
                    now = nowDateTime,
                    horizonDays = HORIZON_DAYS,
                    zoneId = zone,
                    pillCountPerIntake = dose.pillCount,
                    alreadyPlannedPills = alreadyPlannedPills,
                    existingPlannedAtMillis = existingPlannedAt
                )
            )

            if (generatedMillis.isEmpty()) continue

            val createdAt = System.currentTimeMillis()
            val toInsert = generatedMillis.map { plannedAt ->
                IntakeEntity(
                    id = UUID.randomUUID().toString(),
                    medicineId = medicine.id,
                    plannedAt = plannedAt,
                    status = "PLANNED",
                    pillCountPlanned = dose.pillCount,
                    realDoseMgPlanned = dose.realDoseMg,
                    packageIdPlanned = currentPackage.id,
                    googleTaskId = null,
                    createdAt = createdAt,
                    updatedAt = createdAt
                )
            }
            intakeDao.insertAll(toInsert)
        }
    }

    private companion object {
        const val HORIZON_DAYS = 90L
    }
}
