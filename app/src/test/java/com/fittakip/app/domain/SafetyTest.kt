package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyTest {
    private val woman = Profile(male = false, age = 27, heightCm = 165.0, weightKg = 62.0, activity = 1.375, goal = "CUT", targetWeightKg = 58.0)

    @Test
    fun pregnancy_removesDeficit() {
        val normal = Calculator.calculate(woman, AppSettings())
        val pregnant = Calculator.calculate(woman, AppSettings(lifeStage = "PREGNANT"))
        assertEquals(pregnant.tdee, pregnant.target)
        assertTrue(pregnant.target > normal.target)
    }

    @Test
    fun lowTargetWeight_warns() {
        // 168 cm için sağlıklı alt sınır ~52 kg
        val w = Calculator.safetyWarnings(false, 168.0, 60.0, 45.0, Goal.CUT, "", false)
        assertTrue(w.any { it.contains("sağlıklı aralığın") })
    }

    @Test
    fun underweightCut_warns_andHealthyHasNone() {
        assertTrue(Calculator.safetyWarnings(false, 170.0, 50.0, null, Goal.CUT, "", false).any { it.contains("zayıf aralık") })
        assertTrue(Calculator.safetyWarnings(true, 178.0, 82.0, 76.0, Goal.CUT, "", false).isEmpty())
    }
}
