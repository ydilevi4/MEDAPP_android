package com.medapp.domain.usecase

import com.medapp.integration.google.GoogleTaskMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleTaskSyncHelpersTest {

    @Test
    fun `extract marker from notes`() {
        val notes = "MPT_INTAKE_ID:intake-123\nDose: 20 mg"

        assertEquals("intake-123", GoogleTaskMarker.extractIntakeId(notes))
    }

    @Test
    fun `return null when marker is missing`() {
        assertNull(GoogleTaskMarker.extractIntakeId("Dose: 20 mg"))
    }

    @Test
    fun `completion accounting clamps negative`() {
        val accounting = MarkIntakeCompletedUseCase.completionAccounting(
            targetPackageId = "pkg",
            oldPackageId = "pkg",
            plannedPillCount = -1.0
        )

        assertEquals(0.0, accounting.pillDelta, 0.0)
        assertEquals(true, accounting.shouldUpdateOldConsumed)
    }
}
