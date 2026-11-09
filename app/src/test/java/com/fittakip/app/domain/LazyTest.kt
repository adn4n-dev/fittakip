package com.fittakip.app.domain

import com.fittakip.app.data.FOOD_DB
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyTest {
    @Test
    fun usualBreakfast_needsThreeDays() {
        val today = 100L
        val foods = (95L..99L).flatMap { d ->
            listOf(
                FoodEntry(epochDay = d, name = "Haşlanmış yumurta", kcal = 156, meal = "BREAKFAST"),
                FoodEntry(epochDay = d, name = "Beyaz peynir", kcal = 90, meal = "BREAKFAST")
            )
        } + FoodEntry(epochDay = 99, name = "Simit", kcal = 280, meal = "BREAKFAST")   // bir kez → her zamanki değil
        val u = Routine.usual(foods, Meal.BREAKFAST, today)!!
        assertEquals(setOf("Haşlanmış yumurta", "Beyaz peynir"), u.items.map { it.name }.toSet())
        assertEquals(246, u.kcal)
        assertNull(Routine.usual(foods, Meal.DINNER, today))
    }

    @Test
    fun voice_numbersAndUnits() {
        val p = VoiceParser.parse("iki yumurta, bir simit ve bir bardak çay", FOOD_DB)
        assertEquals(3, p.matches.size)
        assertEquals(2.0, p.matches[0].qty, 1e-9)
        assertTrue(p.matches[0].item.name.contains("yumurta", true))
        assertTrue(p.matches[1].item.name.contains("simit", true))
        assertTrue(p.matches[2].item.name.contains("çay", true))
    }

    @Test
    fun voice_halfAndUnknown() {
        val p = VoiceParser.parse("bir buçuk tabak pilav ile zxqv", FOOD_DB)
        assertNotNull(p.matches.firstOrNull())
        assertEquals(1.5, p.matches.first().qty, 1e-9)
        assertTrue(p.matches.first().item.name.contains("pilav", true))
        assertEquals(listOf("zxqv"), p.unknown)
    }
}
