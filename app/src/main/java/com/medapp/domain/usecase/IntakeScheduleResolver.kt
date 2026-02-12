package com.medapp.domain.usecase

import com.medapp.data.entity.SettingsEntity
import com.medapp.domain.util.TimeParser
import java.time.LocalTime

object IntakeScheduleResolver {
    private const val CUSTOM_PREFIX = "CUSTOM_TIME:"

    fun resolveAnchorTime(anchor: String, settings: SettingsEntity): LocalTime? {
        val wake = TimeParser.parseLocalTimeSafe(settings.wakeTime, LocalTime.of(7, 0))
        val breakfast = TimeParser.parseLocalTimeSafe(settings.breakfastTime, LocalTime.of(8, 0))
        val lunch = TimeParser.parseLocalTimeSafe(settings.lunchTime, LocalTime.of(13, 0))
        val dinner = TimeParser.parseLocalTimeSafe(settings.dinnerTime, LocalTime.of(19, 0))
        val sleep = TimeParser.parseLocalTimeSafe(settings.sleepTime, LocalTime.of(23, 0))

        return when {
            anchor.startsWith(CUSTOM_PREFIX) -> TimeParser.parseLocalTime(anchor.removePrefix(CUSTOM_PREFIX))
            anchor == "AFTER_WAKE" -> wake
            anchor == "BEFORE_BREAKFAST" -> breakfast.minusMinutes(30)
            anchor == "BREAKFAST_TIME" -> breakfast
            anchor == "BEFORE_LUNCH" -> lunch.minusMinutes(30)
            anchor == "LUNCH_TIME" -> lunch
            anchor == "BEFORE_DINNER" -> dinner.minusMinutes(30)
            anchor == "DINNER_TIME" -> dinner
            anchor == "BEFORE_SLEEP" -> sleep
            else -> null
        }
    }

    fun customAnchor(time: String): String = "$CUSTOM_PREFIX${TimeParser.normalizeTimeInput(time)}"
}
