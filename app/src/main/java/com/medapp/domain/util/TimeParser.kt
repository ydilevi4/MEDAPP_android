package com.medapp.domain.util

import java.time.LocalTime
import java.util.logging.Logger

object TimeParser {
    private val logger = Logger.getLogger(TimeParser::class.java.name)
    private val timeRegex = Regex("^\\d{2}:\\d{2}$")

    fun normalizeTimeInput(value: String): String {
        return if (value == "24:00") "00:00" else value
    }

    fun isValidTime(value: String): Boolean {
        val normalized = normalizeTimeInput(value)
        if (!timeRegex.matches(normalized)) return false
        val parts = normalized.split(":")
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false
        return hour in 0..23 && minute in 0..59
    }

    fun parseLocalTimeSafe(value: String?, fallback: LocalTime): LocalTime {
        if (value == null) return fallback
        val normalized = normalizeTimeInput(value)
        if (!isValidTime(normalized)) {
            logger.warning("Invalid time '$value'. Falling back to $fallback")
            return fallback
        }
        val parts = normalized.split(":")
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }
}

