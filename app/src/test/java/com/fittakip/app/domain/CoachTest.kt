package com.fittakip.app.domain

import com.fittakip.app.data.USER_PROGRAM
import com.fittakip.app.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class CoachTest {
    private val upperA = USER_PROGRAM.first { it.name == "Upper A" }
    private val upperB = USER_PROGRAM.first { it.name == "Upper B" }

    private fun day(d: Long, t: com.fittakip.app.data.WorkoutTemplate, latKg: Double) =
        t.items.map { WorkoutSet(epochDay = d, exercise = it.exercise, weightKg = if (it.exercise == "Lat Pulldown") latKg else 20.0, reps = 10) }

    @Test
    fun latPulldown_wideAndCloseDoNotMix() {
        // Pazartesi Upper A geniş tutuş 60 kg, Cuma Upper B dar tutuş 45 kg, sonra tekrar Pazartesi
        val sets = day(1, upperA, 60.0) + day(5, upperB, 45.0) + day(8, upperA, 62.5)
        val forB = Coach.contextHistory(sets, "Lat Pulldown", "Upper B", USER_PROGRAM, before = 12)
        assertEquals(setOf(45.0), forB.map { it.weightKg }.toSet())
        val forA = Coach.contextHistory(sets, "Lat Pulldown", "Upper A", USER_PROGRAM, before = 12)
        assertEquals(62.5, forA.maxOf { it.weightKg }, 1e-9)
    }

    @Test
    fun prefill_ignoresDropAndWarmup_andFollowsCoach() {
        val item = upperA.items.first { it.exercise == "Lateral Raise" }   // 4×12–15
        val past = (1..4).map { WorkoutSet(epochDay = 1, exercise = "Lateral Raise", weightKg = 10.0, reps = 15) } +
            WorkoutSet(epochDay = 1, exercise = "Lateral Raise", weightKg = 4.0, reps = 12, isDrop = true) +
            WorkoutSet(epochDay = 1, exercise = "Lateral Raise", weightKg = 6.0, reps = 15, warmup = true)
        // tüm setlerde 15 → bir adım art, aralığın altından başla
        assertEquals(Coach.Target(12.0, 12), Coach.nextTarget(past, item))
    }
}
