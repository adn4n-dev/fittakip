package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.Profile
import com.fittakip.app.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KarneTest {
    private val st = AppSettings(weekPlan = "Upper A;OFF;Lower;OFF;Upper B;OFF;OFF")
    private val p = Profile(male = true, age = 25, heightCm = 178.0, weightKg = 82.0, activity = 1.55, goal = "CUT", targetWeightKg = 76.0)

    @Test
    fun noData() {
        val k = Karne.compute(emptyList(), emptyList(), emptyList(), p, st, 100)
        assertEquals("Bu hafta kayıt yok", k.headline)
        assertEquals(3, k.workoutsPlanned)
    }

    @Test
    fun strongerAndLighter_isProgressing() {
        // 4 hafta önce Lat Pulldown 60×8, bu hafta 65×8; kilo haftada ~0,5 iniyor
        val sets = listOf(
            WorkoutSet(epochDay = 70, exercise = "Lat Pulldown", weightKg = 60.0, reps = 8),
            WorkoutSet(epochDay = 96, exercise = "Lat Pulldown", weightKg = 65.0, reps = 8),
            WorkoutSet(epochDay = 98, exercise = "Hack Squat", weightKg = 100.0, reps = 8),
            WorkoutSet(epochDay = 100, exercise = "Lateral Raise", weightKg = 10.0, reps = 12)
        )
        val weights = (72L..100L step 4).mapIndexed { i, d -> BodyWeight(epochDay = d, kg = 84.0 - i * 0.3) }
        val foods = (94L..99L).map { FoodEntry(epochDay = it, name = "x", kcal = 2100) }
        val k = Karne.compute(sets, foods, weights, p, st, 100)
        assertEquals(3, k.workoutsDone)
        assertTrue(k.progressing == true)
        assertTrue(k.headline.startsWith("✅"))
        assertTrue(k.improvedLifts.contains("Lat Pulldown"))
    }
}
