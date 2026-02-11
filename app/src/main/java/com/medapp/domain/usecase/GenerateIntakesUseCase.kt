package com.medapp.domain.usecase

import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PackageTransitionDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.entity.PillPackageEntity
import com.medapp.data.model.EqualDistanceRule
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class GenerateIntakesUseCase(
    private val medicineDao: MedicineDao,
    private val intakeDao: IntakeDao,
    private val pillPackageDao: PillPackageDao,
    private val packageTransitionDao: PackageTransitionDao,
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
            val transition = packageTransitionDao.getForMedicine(medicine.id)
            val oldPackage = transition?.let { pillPackageDao.getById(it.oldPackageId) }

            val existingPlannedAt = intakeDao.getPlannedAtInRange(medicine.id, fromMillis, toMillis).toSet()
            val alreadyPlannedPills = intakeDao.getPlannedPillsSum(medicine.id)
            val generatedMillis = IntakeGenerationHelper.generatePlannedAtMillis(
                IntakeGenerationHelper.GeneratePlanParams(
                    medicine = medicine,
                    settings = settings,
                    now = nowDateTime,
                    horizonDays = HORIZON_DAYS,
                    zoneId = zone,
                    pillCountPerIntake = doseFor(medicine.targetDoseMg, currentPackage, tieRule).pillCount,
                    alreadyPlannedPills = alreadyPlannedPills,
                    existingPlannedAtMillis = existingPlannedAt
                )
            ).sorted()

            if (generatedMillis.isEmpty()) continue

            val createdAt = System.currentTimeMillis()
            var oldPillsRemainingForPlan = (transition?.oldPillsLeft ?: 0.0) - (transition?.oldPillsConsumed ?: 0.0)

            val toInsert = generatedMillis.map { plannedAt ->
                val oldDose = oldPackage?.let { doseFor(medicine.targetDoseMg, it, tieRule) }
                val canUseOldPackage = oldPackage != null && oldDose != null && oldPillsRemainingForPlan + 1e-9 >= oldDose.pillCount
                val packageForIntake = if (canUseOldPackage) oldPackage else currentPackage
                val dose = if (canUseOldPackage) oldDose!! else doseFor(medicine.targetDoseMg, currentPackage, tieRule)
                if (canUseOldPackage) {
                    oldPillsRemainingForPlan = (oldPillsRemainingForPlan - dose.pillCount).coerceAtLeast(0.0)
                }
                IntakeEntity(
                    id = UUID.randomUUID().toString(),
                    medicineId = medicine.id,
                    plannedAt = plannedAt,
                    status = "PLANNED",
                    pillCountPlanned = dose.pillCount,
                    realDoseMgPlanned = dose.realDoseMg,
                    packageIdPlanned = packageForIntake.id,
                    googleTaskId = null,
                    createdAt = createdAt,
                    updatedAt = createdAt
                )
            }
            intakeDao.insertAll(toInsert)
        }
    }

    suspend fun refreshFuturePlanned() {
        val nowMillis = System.currentTimeMillis()
        intakeDao.deletePlannedFrom(nowMillis)
        invoke()
    }

    private fun doseFor(targetDoseMg: Int, pkg: PillPackageEntity, tieRule: EqualDistanceRule): DoseCalculationUseCase.Result {
        return doseCalculationUseCase(
            targetDoseMg = targetDoseMg,
            pillDoseMg = pkg.pillDoseMg,
            divisibleHalf = pkg.divisibleHalf,
            tieRule = tieRule
        )
    }

    private companion object {
        const val HORIZON_DAYS = 90L
    }
}
