package com.fittakip.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekOverrideTest {
    private val base = AppSettings(weekPlan = "Upper A;OFF;Lower;OFF;Upper B;OFF;OFF")
    private val monday = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay()

    @Test
    fun overrideAppliesThisWeekOnly() {
        val st = base.withOverrides(mapOf(monday to OFF_DAY, monday + 1 to "Upper A"))
        assertEquals(OFF_DAY, st.planFor(monday))
        assertEquals("Upper A", st.planFor(monday + 1))
        assertTrue(st.isShifted(monday))
        // gelecek hafta normale döner
        assertEquals("Upper A", st.planFor(monday + 7))
        assertEquals(OFF_DAY, st.planFor(monday + 8))
        assertFalse(st.isShifted(monday + 7))
    }

    @Test
    fun lastWeeksOverridesExpire() {
        val st = base.withOverrides(mapOf(monday - 7 to OFF_DAY))
        assertTrue(st.overrides.isEmpty())
        assertEquals("Upper A", st.planFor(monday - 7))
    }

    @Test
    fun overrideEqualToBaseIsNotShifted() {
        val st = base.withOverrides(mapOf(monday + 2 to "Lower"))
        assertFalse(st.isShifted(monday + 2))
    }
}
