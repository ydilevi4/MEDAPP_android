package com.medapp.domain.usecase

import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.SettingsEntity
import com.medapp.data.model.DurationType
import com.medapp.data.model.ScheduleType
import com.medapp.data.util.AnchorJsonHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object IntakeGenerationHelper {
    private val DEFAULT_FIRST_DOSE_TIME: LocalTime = LocalTime.of(8, 0)

    data class GeneratePlanParams(
        val medicine: MedicineEntity,
        val settings: SettingsEntity,
        val now: LocalDateTime,
        val horizonDays: Long,
        val zoneId: ZoneId,
        val pillCountPerIntake: Double,
        val alreadyPlannedPills: Double,
        val existingPlannedAtMillis: Set<Long>
    )

    fun generatePlannedAtMillis(params: GeneratePlanParams): List<Long> {
        val horizonEnd = params.now.plusDays(params.horizonDays)
        val startDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(params.medicine.createdAt),
            params.zoneId
        ).toLocalDate()

        var plannedPills = params.alreadyPlannedPills
        val generated = mutableListOf<Long>()

        when (ScheduleType.valueOf(params.medicine.scheduleType)) {
            ScheduleType.FOOD_SLEEP -> {
                val anchors = AnchorJsonHelper.decode(params.medicine.anchorsJson)
                if (anchors.isEmpty()) return emptyList()

                var date = maxOf(startDate, params.now.toLocalDate())
                while (!date.isAfter(horizonEnd.toLocalDate())) {
                    if (isTakingDay(params.medicine, date, startDate)) {
                        val times = anchors.mapNotNull { anchorToLocalTime(it, params.settings) }.sorted()
                        for (time in times) {
                            val plannedDateTime = LocalDateTime.of(date, time)
                            if (plannedDateTime.isBefore(params.now)) continue
                            if (plannedDateTime.isAfter(horizonEnd)) continue

                            if (shouldStopByDuration(params.medicine, date, startDate)) continue
                            if (isPillsCountExceeded(params.medicine, plannedPills, params.pillCountPerIntake)) continue

                            val millis = plannedDateTime.atZone(params.zoneId).toInstant().toEpochMilli()
                            if (!params.existingPlannedAtMillis.contains(millis)) {
                                generated += millis
                            }
                            plannedPills += params.pillCountPerIntake
                        }
                    }
                    date = date.plusDays(1)
                }
            }

            ScheduleType.EVERY_N_HOURS -> {
                val interval = params.medicine.intervalHours ?: return emptyList()
                if (interval <= 0) return emptyList()

                val firstDoseTime = params.medicine.firstDoseTime?.let(LocalTime::parse) ?: DEFAULT_FIRST_DOSE_TIME
                var cursor = LocalDateTime.of(startDate, firstDoseTime)
                while (cursor.isBefore(params.now)) {
                    cursor = cursor.plusHours(interval.toLong())
                }

                while (!cursor.isAfter(horizonEnd)) {
                    val date = cursor.toLocalDate()
                    if (isTakingDay(params.medicine, date, startDate) &&
                        !shouldStopByDuration(params.medicine, date, startDate) &&
                        !isPillsCountExceeded(params.medicine, plannedPills, params.pillCountPerIntake)
                    ) {
                        val millis = cursor.atZone(params.zoneId).toInstant().toEpochMilli()
                        if (!params.existingPlannedAtMillis.contains(millis)) {
                            generated += millis
                        }
                        plannedPills += params.pillCountPerIntake
                    }
                    cursor = cursor.plusHours(interval.toLong())
                }
            }
        }

        return generated
    }

    private fun shouldStopByDuration(medicine: MedicineEntity, date: LocalDate, startDate: LocalDate): Boolean {
        return when (DurationType.valueOf(medicine.durationType)) {
            DurationType.DAYS -> {
                val days = medicine.durationDays ?: return false
                val durationEndDate = startDate.plusDays((days - 1).coerceAtLeast(0).toLong())
                date.isAfter(durationEndDate)
            }

            DurationType.PILLS_COUNT -> false
            DurationType.COURSES -> false
        }
    }

    private fun isPillsCountExceeded(medicine: MedicineEntity, plannedPills: Double, perIntakePills: Double): Boolean {
        if (DurationType.valueOf(medicine.durationType) != DurationType.PILLS_COUNT) return false
        val total = medicine.totalPillsToTake ?: return false
        return plannedPills + perIntakePills > total + 1e-9
    }

    private fun isTakingDay(medicine: MedicineEntity, date: LocalDate, startDate: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        return when (DurationType.valueOf(medicine.durationType)) {
            DurationType.COURSES -> {
                val takeDays = medicine.courseDays ?: return false
                val restDays = medicine.restDays ?: 0
                val cycleLength = takeDays + restDays
                if (takeDays <= 0 || cycleLength <= 0) return false

                val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date).toInt()
                val cycleIndex = daysFromStart / cycleLength
                val inCycleDay = daysFromStart % cycleLength

                val cyclesCount = medicine.cyclesCount
                if (cyclesCount != null && cycleIndex >= cyclesCount) {
                    false
                } else {
                    inCycleDay < takeDays
                }
            }

            DurationType.DAYS,
            DurationType.PILLS_COUNT -> true
        }
    }

    private fun anchorToLocalTime(anchor: String, settings: SettingsEntity): LocalTime? {
        val wake = LocalTime.parse(settings.wakeTime)
        val breakfast = LocalTime.parse(settings.breakfastTime)
        val lunch = LocalTime.parse(settings.lunchTime)
        val dinner = LocalTime.parse(settings.dinnerTime)
        val sleep = LocalTime.parse(settings.sleepTime)

        return when (anchor) {
            "AFTER_WAKE" -> wake
            "BEFORE_BREAKFAST" -> breakfast.minusMinutes(30)
            "BREAKFAST_TIME" -> breakfast
            "BEFORE_LUNCH" -> lunch.minusMinutes(30)
            "LUNCH_TIME" -> lunch
            "BEFORE_DINNER" -> dinner.minusMinutes(30)
            "DINNER_TIME" -> dinner
            "BEFORE_SLEEP" -> sleep
            else -> null
        }
    }
}
