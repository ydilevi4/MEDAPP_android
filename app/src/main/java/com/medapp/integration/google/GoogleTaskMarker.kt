package com.medapp.integration.google

object GoogleTaskMarker {
    private const val PREFIX = "MPT_INTAKE_ID:"

    fun markerForIntakeId(intakeId: String): String = "$PREFIX$intakeId"

    fun extractIntakeId(notes: String?): String? {
        if (notes.isNullOrBlank()) return null
        return notes
            .lineSequence()
            .map { it.trim() }
            .firstNotNullOfOrNull { line ->
                if (!line.startsWith(PREFIX)) return@firstNotNullOfOrNull null
                line.removePrefix(PREFIX).takeIf { it.isNotBlank() }
            }
    }
}
