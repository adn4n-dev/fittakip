package com.fittakip.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)   // org.json gerçek uygulaması için
@Config(sdk = [34])
class OnlineFoodsTest {
    @Test
    fun parse_usesServingWhenAvailable() {
        val p = JSONObject("""{"product_name":"Ayran","brands":"Sütaş,X","serving_quantity":"200",
            "nutriments":{"energy-kcal_100g":38,"proteins_100g":1.7,"carbohydrates_100g":2.8,"fat_100g":2.0}}""")
        val f = OnlineFoods.parse(p)!!
        assertEquals("Ayran · Sütaş", f.name)
        assertEquals("1 porsiyon (200 g)", f.portion)
        assertEquals(76, f.kcal)
        assertEquals(3.4, f.p, 1e-9)
    }

    @Test
    fun parse_fallsBackTo100gAndKj() {
        val p = JSONObject("""{"product_name":"Bisküvi","nutriments":{"energy_100g":2092}}""")
        val f = OnlineFoods.parse(p)!!
        assertEquals("100 g", f.portion)
        assertEquals(500, f.kcal)
    }

    @Test
    fun parse_rejectsMissingData() {
        assertNull(OnlineFoods.parse(JSONObject("""{"product_name":"","nutriments":{"energy-kcal_100g":100}}""")))
        assertNull(OnlineFoods.parse(JSONObject("""{"product_name":"X","nutriments":{}}""")))
    }
}
