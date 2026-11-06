package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.USER_PROGRAM
import com.fittakip.app.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanResolverTest {
    // Pzt Upper A, Çar Lower, Cum Upper B
    private val plan = "Upper A;OFF;Lower;OFF;Upper B;OFF;OFF"
    private val monday = LocalDate.of(2026, 11, 2).toEpochDay()

    private fun did(day: Long, tpl: String) =
        USER_PROGRAM.first { it.name == tpl }.items.map { WorkoutSet(epochDay = day, exercise = it.exercise, weightKg = 20.0, reps = 10) }

    @Test
    fun weekday_mode_follows_calendar() {
        val st = AppSettings(weekPlan = plan)
        assertEquals("Upper A", PlanResolver.templateFor(st, emptyList(), monday)?.name)
        assertTrue(PlanResolver.forDay(st, emptyList(), monday + 1) is DayPlan.Off)
        assertEquals("Upper B", PlanResolver.templateFor(st, emptyList(), monday + 4)?.name)
    }

    @Test
    fun rotation_does_not_skip_missed_workout() {
        val st = AppSettings(weekPlan = plan, planMode = "ROTATION", rotationStart = monday)
        // Pazartesi gidemedi; Salı Upper A yaptı
        val sets = did(monday + 1, "Upper A")
        assertEquals("Upper A", PlanResolver.templateFor(st, sets, monday + 1)?.name)
        // Ertesi gün sıradaki Lower olmalı
        assertEquals("Lower", PlanResolver.templateFor(st, sets, monday + 2)?.name)
        val more = sets + did(monday + 3, "Lower")
        assertEquals("Upper B", PlanResolver.templateFor(st, more, monday + 5)?.name)
        val all = more + did(monday + 5, "Upper B")
        assertEquals("Upper A", PlanResolver.templateFor(st, all, monday + 7)?.name)
    }

    @Test
    fun rep_range_label() {
        val lat = USER_PROGRAM.first().items.first()
        assertEquals("4×8–12", lat.label)
    }
}
