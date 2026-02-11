package com.medapp.domain.usecase

import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.entity.SettingsEntity
import com.medapp.domain.model.LowStockMedicineItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LowStockEstimator(
    private val medicineDao: MedicineDao,
    private val pillPackageDao: PillPackageDao,
    private val intakeDao: IntakeDao
) {
    suspend fun estimate(settings: SettingsEntity): List<LowStockMedicineItem> {
        if (!settings.lowStockWarningEnabled) return emptyList()

        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val nowDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val in7 = nowDate.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val in14 = nowDate.plusDays(14).atStartOfDay(zone).toInstant().toEpochMilli()

        val medicines = medicineDao.getActiveMedicines().associateBy { it.id }
        return pillPackageDao.getAllCurrent()
            .filter { it.warnWhenEnding }
            .mapNotNull { pkg ->
                val medicine = medicines[pkg.medicineId] ?: return@mapNotNull null
                val planned7 = intakeDao.getPlannedInRangeForMedicine(medicine.id, now, in7)
                val daily7 = averageDailyConsumption(planned7, zone)
                val plannedForAvg = if (daily7 > 0.0 && planned7.isNotEmpty()) {
                    planned7
                } else {
                    intakeDao.getPlannedInRangeForMedicine(medicine.id, now, in14)
                }
                val avgDailyConsumption = averageDailyConsumption(plannedForAvg, zone)
                if (avgDailyConsumption <= 0.0) return@mapNotNull null

                val daysRemaining = pkg.pillsRemaining / avgDailyConsumption
                if (daysRemaining > settings.lowStockWarningDays) return@mapNotNull null

                LowStockMedicineItem(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    packageId = pkg.id,
                    pillsRemaining = pkg.pillsRemaining,
                    estimatedDaysRemaining = daysRemaining
                )
            }
            .sortedBy { it.estimatedDaysRemaining }
    }

    private fun averageDailyConsumption(planned: List<com.medapp.data.entity.IntakeEntity>, zone: ZoneId): Double {
        if (planned.isEmpty()) return 0.0
        val perDay = planned.groupBy {
            Instant.ofEpochMilli(it.plannedAt).atZone(zone).toLocalDate()
        }
        if (perDay.isEmpty()) return 0.0
        val total = perDay.values.sumOf { dayList -> dayList.sumOf { it.pillCountPlanned } }
        return total / perDay.size.toDouble()
    }
}
