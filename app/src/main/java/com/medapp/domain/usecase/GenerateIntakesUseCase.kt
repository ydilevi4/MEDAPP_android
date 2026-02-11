package com.medapp.domain.usecase

import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.model.EqualDistanceRule
import java.time.LocalDate
import java.time.LocalTime
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
        val breakfastTime = LocalTime.parse(settings.breakfastTime)
        val tieRule = EqualDistanceRule.valueOf(settings.equalDistanceRule)
        val zone = ZoneId.systemDefault()

        val activeMedicines = medicineDao.getActiveMedicines()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(90)

        for (medicine in activeMedicines) {
            val currentPackage = pillPackageDao.getCurrentForMedicine(medicine.id) ?: continue
            val dose = doseCalculationUseCase(
                targetDoseMg = medicine.targetDoseMg,
                pillDoseMg = currentPackage.pillDoseMg,
                divisibleHalf = currentPackage.divisibleHalf,
                tieRule = tieRule
            )

            val toInsert = mutableListOf<IntakeEntity>()
            var date = startDate
            while (!date.isAfter(endDate)) {
                val plannedAt = date.atTime(breakfastTime).atZone(zone).toInstant().toEpochMilli()
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val exists = intakeDao.countForMedicineInRange(medicine.id, dayStart, dayEnd) > 0
                if (!exists) {
                    val now = System.currentTimeMillis()
                    toInsert += IntakeEntity(
                        id = UUID.randomUUID().toString(),
                        medicineId = medicine.id,
                        plannedAt = plannedAt,
                        status = "PLANNED",
                        pillCountPlanned = dose.pillCount,
                        realDoseMgPlanned = dose.realDoseMg,
                        packageIdPlanned = currentPackage.id,
                        googleTaskId = null,
                        createdAt = now,
                        updatedAt = now
                    )
                }
                date = date.plusDays(1)
            }
            if (toInsert.isNotEmpty()) {
                intakeDao.insertAll(toInsert)
            }
        }
    }
}
