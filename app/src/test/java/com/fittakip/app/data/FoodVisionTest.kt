package com.fittakip.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodVisionTest {
    @Test
    fun parse_extractsJsonEvenWithExtraText() {
        val r = FoodVision.parse("""Tabii: {"items":[{"name":"Pirinç pilavı","portion":"1 tabak (~200 g)","kcal":260,"protein":5,"carb":52,"fat":4},
            {"name":"Kuru fasulye","portion":"1 kase","kcal":300.4,"protein":15,"carb":40,"fat":9},{"name":"","kcal":10}],"note":"Yağ miktarı belirsiz"}""")
        assertEquals(2, r.items.size)
        assertEquals(300, r.items[1].kcal)
        assertEquals("Yağ miktarı belirsiz", r.note)
    }

    @Test
    fun parse_emptyPlate() {
        val r = FoodVision.parse("""{"items":[],"note":"Fotoğrafta yiyecek yok"}""")
        assertTrue(r.items.isEmpty())
    }
}
