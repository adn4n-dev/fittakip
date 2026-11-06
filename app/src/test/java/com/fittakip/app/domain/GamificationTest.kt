package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationTest {

    @Test
    fun streak_countsFromYesterdayIfTodayEmpty() {
        val days = setOf(10L, 11L, 12L)
        assertEquals(3, Gamification.currentStreak(days, 13))
        assertEquals(3, Gamification.currentStreak(days, 12))
        assertEquals(0, Gamification.currentStreak(days, 15))
    }

    @Test
    fun longestStreak() {
        assertEquals(4, Gamification.longestStreak(setOf(1L, 2L, 5L, 6L, 7L, 8L, 10L)))
    }

    @Test
    fun levels() {
        assertEquals(1, Gamification.levelOf(0))
        assertEquals(2, Gamification.levelOf(100))
        assertEquals(3, Gamification.levelOf(300))
        assertEquals(2, Gamification.levelOf(299))
    }

    @Test
    fun prDays_countsOnlyBeatingEarlierDays() {
        val sets = listOf(
            WorkoutSet(epochDay = 1, exercise = "Bench", weightKg = 60.0, reps = 5),
            WorkoutSet(epochDay = 1, exercise = "Bench", weightKg = 70.0, reps = 5),
            WorkoutSet(epochDay = 3, exercise = "Bench", weightKg = 72.5, reps = 5),
            WorkoutSet(epochDay = 5, exercise = "Bench", weightKg = 70.0, reps = 5)
        )
        assertEquals(listOf(3L to "Bench"), Gamification.prDays(sets))
    }

    @Test
    fun compute_runsOnEmptyData() {
        val g = Gamification.compute(AllData(), null, AppSettings(), 20_000)
        assertEquals(0, g.streak)
        assertEquals(1, g.level)
        assertTrue(g.achievements.none { it.unlocked })
    }

    @Test
    fun weightStep_byEquipment() {
        assertEquals(5.0, com.fittakip.app.data.weightStep("Leg Press"), 1e-9)
        assertEquals(2.5, com.fittakip.app.data.weightStep("Triceps Pushdown"), 1e-9)
        assertEquals(2.0, com.fittakip.app.data.weightStep("Lateral Raise"), 1e-9)
    }

    @Test
    fun rotationPlanMap_matchesSingleDay() {
        val st = AppSettings(weekPlan = "Upper A;OFF;Lower;OFF;Upper B;OFF;OFF", planMode = "ROTATION", rotationStart = 0)
        val sets = com.fittakip.app.data.USER_PROGRAM[0].items.map { WorkoutSet(epochDay = 5, exercise = it.exercise, weightKg = 20.0, reps = 10) }
        val map = PlanResolver.planMap(st, sets, listOf(5L, 6L))
        assertEquals(PlanResolver.forDay(st, sets, 5), map[5L])
        assertEquals(PlanResolver.forDay(st, sets, 6), map[6L])
    }

    @Test
    fun bodyFat_needsWaistAndNeck() {
        assertNull(Calculator.bodyFatNavy(true, 180.0, null, 38.0, null))
        val bf = Calculator.bodyFatNavy(true, 180.0, 85.0, 38.0, null)!!
        assertTrue(bf in 10.0..25.0)
    }
}
