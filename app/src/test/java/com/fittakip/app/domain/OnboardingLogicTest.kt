package com.fittakip.app.domain

import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.WEEK_PRESETS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingLogicTest {
    private val upperLower = WEEK_PRESETS.first()

    @Test
    fun recommend_byDaysAndLevel() {
        assertTrue(OnboardingLogic.recommend(3, 0).name.startsWith("Full Body"))
        assertEquals(upperLower, OnboardingLogic.recommend(3, 1))
        assertTrue(OnboardingLogic.recommend(4, 1).name.startsWith("Üst / Alt"))
        assertTrue(OnboardingLogic.recommend(6, 2).name.startsWith("PPL · 6"))
    }

    @Test
    fun buildPlan_placesWorkoutsOnChosenDays() {
        // Salı, Perşembe, Cumartesi
        val plan = OnboardingLogic.buildPlan(upperLower, setOf(1, 3, 5), rotation = false)
        assertEquals(listOf(OFF_DAY, "Upper A", OFF_DAY, "Lower", OFF_DAY, "Upper B", OFF_DAY), plan)
        assertFalse(OnboardingLogic.suggestRotation(upperLower, setOf(1, 3, 5)))
    }

    @Test
    fun rotation_keepsAllWorkoutsWhenFewerDays() {
        val ppl6 = WEEK_PRESETS.first { it.name.startsWith("PPL · 6") }
        assertTrue(OnboardingLogic.suggestRotation(ppl6, setOf(0, 1, 2, 3, 4)))
        val plan = OnboardingLogic.buildPlan(ppl6, setOf(0, 1, 2, 3, 4), rotation = true)
        assertEquals(6, plan.count { it != OFF_DAY && it.isNotEmpty() })
    }
}
