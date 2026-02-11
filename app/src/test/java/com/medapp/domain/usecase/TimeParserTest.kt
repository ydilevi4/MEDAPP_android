package com.medapp.domain.usecase

import com.medapp.domain.util.TimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimeParserTest {
    @Test
    fun `normalizes 24_00 to midnight and parses safely`() {
        val parsed = TimeParser.parseLocalTimeSafe("24:00", LocalTime.NOON)
        assertEquals(LocalTime.MIDNIGHT, parsed)
    }

    @Test
    fun `invalid time falls back without throwing`() {
        val parsed = TimeParser.parseLocalTimeSafe("99:99", LocalTime.of(8, 15))
        assertEquals(LocalTime.of(8, 15), parsed)
        assertTrue(!TimeParser.isValidTime("99:99"))
    }
}
