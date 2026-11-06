package com.fittakip.app.domain

import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressAnalyzerTest {

    @Test
    fun linearFit_exactLine() {
        val fit = Stats.linearFit(listOf(0.0, 1.0, 2.0, 3.0), listOf(1.0, 3.0, 5.0, 7.0))!!
        assertEquals(2.0, fit.slope, 1e-9)
        assertEquals(1.0, fit.intercept, 1e-9)
        assertEquals(1.0, fit.r2, 1e-9)
    }

    @Test
    fun epley() {
        assertEquals(100.0, Stats.epley1RM(100.0, 1), 1e-9)
        assertEquals(80.0 * (1 + 8 / 30.0), Stats.epley1RM(80.0, 8), 1e-9)
    }

    @Test
    fun steadyIncrease_isImproving() {
        // Her hafta 2.5 kg artan bench (80 → 92.5)
        val sets = (0 until 6).map { w -> WorkoutSet(epochDay = 20_000L + w * 7, exercise = "Bench", weightKg = 80.0 + w * 2.5, reps = 5) }
        val p = ProgressAnalyzer.analyzeExercise(sets)
        assertEquals(Verdict.IMPROVING, p.verdict)
        assertNotNull(p.projected4Weeks)
    }

    @Test
    fun sameWeight_isPlateau() {
        val sets = (0 until 5).map { w -> WorkoutSet(epochDay = 20_000L + w * 7, exercise = "Squat", weightKg = 100.0, reps = 5) }
        assertEquals(Verdict.PLATEAU, ProgressAnalyzer.analyzeExercise(sets).verdict)
    }

    @Test
    fun twoDays_notEnoughData() {
        val sets = listOf(
            WorkoutSet(epochDay = 20_000L, exercise = "Row", weightKg = 60.0, reps = 8),
            WorkoutSet(epochDay = 20_003L, exercise = "Row", weightKg = 62.5, reps = 8)
        )
        assertEquals(Verdict.NOT_ENOUGH_DATA, ProgressAnalyzer.analyzeExercise(sets).verdict)
    }

    @Test
    fun weightLoss_givesEta() {
        // Haftada 0.5 kg veriyor, hedef 80
        val weights = (0..21).map { d -> BodyWeight(epochDay = 20_000L + d, kg = 90.0 - d * 0.5 / 7) }
        val t = ProgressAnalyzer.analyzeWeight(weights, 80.0, Goal.CUT)!!
        assertEquals(-0.5, t.kgPerWeek!!, 0.01)
        assertNotNull(t.etaDays)
    }

    @Test
    fun weightGoingWrongWay_noEta() {
        val weights = (0..10).map { d -> BodyWeight(epochDay = 20_000L + d, kg = 85.0 + d * 0.05) }
        assertNull(ProgressAnalyzer.analyzeWeight(weights, 80.0, Goal.CUT)!!.etaDays)
    }
}
