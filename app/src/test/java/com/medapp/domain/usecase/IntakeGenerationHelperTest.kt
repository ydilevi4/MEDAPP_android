package com.medapp.domain.usecase

import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.SettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class IntakeGenerationHelperTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val settings = SettingsEntity(
        wakeTime = "07:00",
        breakfastTime = "08:00",
        lunchTime = "13:00",
        dinnerTime = "19:00",
        sleepTime = "23:00",
        equalDistanceRule = "PREFER_HIGHER",
        lowStockWarningEnabled = true,
        lowStockWarningDays = 3,
        language = "EN"
    )

    @Test
    fun `food sleep before meal anchor uses minus 30 minutes`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val medicine = medicine(
            createdAt = startDate,
            scheduleType = "FOOD_SLEEP",
            anchorsJson = "[\"BEFORE_BREAKFAST\"]",
            durationType = "DAYS",
            durationDays = 1
        )

        val generated = IntakeGenerationHelper.generatePlannedAtMillis(
            IntakeGenerationHelper.GeneratePlanParams(
                medicine = medicine,
                settings = settings,
                now = LocalDateTime.of(2026, 1, 1, 0, 0),
                horizonDays = 2,
                zoneId = zone,
                pillCountPerIntake = 1.0,
                alreadyPlannedPills = 0.0,
                existingPlannedAtMillis = emptySet()
            )
        )

        val expectedMillis = LocalDateTime.of(2026, 1, 1, 7, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(listOf(expectedMillis), generated)
    }

    @Test
    fun `every n hours generates continuous times`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val medicine = medicine(
            createdAt = startDate,
            scheduleType = "EVERY_N_HOURS",
            intervalHours = 8,
            firstDoseTime = "06:00",
            durationType = "DAYS",
            durationDays = 2
        )

        val generated = IntakeGenerationHelper.generatePlannedAtMillis(
            IntakeGenerationHelper.GeneratePlanParams(
                medicine = medicine,
                settings = settings,
                now = LocalDateTime.of(2026, 1, 1, 0, 0),
                horizonDays = 2,
                zoneId = zone,
                pillCountPerIntake = 1.0,
                alreadyPlannedPills = 0.0,
                existingPlannedAtMillis = emptySet()
            )
        )

        val expectedTimes = listOf("2026-01-01T06:00", "2026-01-01T14:00", "2026-01-01T22:00", "2026-01-02T06:00", "2026-01-02T14:00", "2026-01-02T22:00")
            .map { LocalDateTime.parse(it).atZone(zone).toInstant().toEpochMilli() }
        assertEquals(expectedTimes, generated)
    }



    @Test
    fun `pills count does not stop early when existing planned slots are present`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val medicine = medicine(
            createdAt = startDate,
            scheduleType = "EVERY_N_HOURS",
            intervalHours = 8,
            firstDoseTime = "06:00",
            durationType = "PILLS_COUNT",
            totalPillsToTake = 8.0
        )

        val existingMillis = listOf(
            LocalDateTime.of(2026, 1, 1, 6, 0),
            LocalDateTime.of(2026, 1, 1, 14, 0)
        ).map { it.atZone(zone).toInstant().toEpochMilli() }

        val generated = IntakeGenerationHelper.generatePlannedAtMillis(
            IntakeGenerationHelper.GeneratePlanParams(
                medicine = medicine,
                settings = settings,
                now = LocalDateTime.of(2026, 1, 1, 0, 0),
                horizonDays = 2,
                zoneId = zone,
                pillCountPerIntake = 2.0,
                alreadyPlannedPills = 4.0,
                existingPlannedAtMillis = existingMillis.toSet()
            )
        )

        val expectedTimes = listOf(
            LocalDateTime.of(2026, 1, 1, 22, 0),
            LocalDateTime.of(2026, 1, 2, 6, 0)
        ).map { it.atZone(zone).toInstant().toEpochMilli() }

        assertEquals(expectedTimes, generated)
    }

    @Test
    fun `courses apply taking and rest days`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val medicine = medicine(
            createdAt = startDate,
            scheduleType = "FOOD_SLEEP",
            anchorsJson = "[\"BREAKFAST_TIME\"]",
            durationType = "COURSES",
            courseDays = 2,
            restDays = 1,
            cyclesCount = 2
        )

        val generated = IntakeGenerationHelper.generatePlannedAtMillis(
            IntakeGenerationHelper.GeneratePlanParams(
                medicine = medicine,
                settings = settings,
                now = LocalDateTime.of(2026, 1, 1, 0, 0),
                horizonDays = 10,
                zoneId = zone,
                pillCountPerIntake = 1.0,
                alreadyPlannedPills = 0.0,
                existingPlannedAtMillis = emptySet()
            )
        )

        val generatedDates = generated.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 5)
            ),
            generatedDates
        )
        assertTrue(generatedDates.none { it == LocalDate.of(2026, 1, 3) })
    }

    private fun medicine(
        createdAt: LocalDate,
        scheduleType: String,
        anchorsJson: String = "[]",
        intervalHours: Int? = null,
        firstDoseTime: String? = null,
        durationType: String,
        durationDays: Int? = null,
        courseDays: Int? = null,
        restDays: Int? = null,
        cyclesCount: Int? = null,
        totalPillsToTake: Double? = null
    ): MedicineEntity {
        val createdAtMillis = LocalDateTime.of(createdAt, LocalTime.MIDNIGHT).atZone(zone).toInstant().toEpochMilli()
        return MedicineEntity(
            id = "m1",
            name = "Medicine",
            targetDoseMg = 100,
            scheduleType = scheduleType,
            anchorsJson = anchorsJson,
            intervalHours = intervalHours,
            firstDoseTime = firstDoseTime,
            durationType = durationType,
            durationDays = durationDays,
            totalPillsToTake = totalPillsToTake,
            courseDays = courseDays,
            restDays = restDays,
            cyclesCount = cyclesCount,
            isActive = true,
            createdAt = createdAtMillis,
            updatedAt = createdAtMillis
        )
    }
}
