package com.fittakip.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Open Food Facts: milyonlarca ürünün açık veritabanı (Türkiye'de satılan on binlerce ürün dahil).
 * Önce Türkiye'de satılan ürünlerde arar, az sonuç çıkarsa tüm dünyada.
 * Arama hız sınırı dakikada 10 istek olduğu için sonuçlar bellekte saklanır.
 */
object OnlineFoods {
    private const val UA = "FitTakip/1.5 (Android; github.com/adn4n-dev/fittakip)"
    private const val FIELDS = "code,product_name,product_name_tr,brands,serving_size,serving_quantity,nutriments"
    private val cache = object : LinkedHashMap<String, List<FoodItem>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FoodItem>>?) = size > 40
    }

    class OnlineException(message: String) : Exception(message)

    suspend fun search(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        synchronized(cache) { cache[q] }?.let { return@withContext it }
        val enc = URLEncoder.encode(q, "UTF-8")
        fun url(host: String) = "https://$host/cgi/search.pl?search_terms=$enc&search_simple=1&action=process&json=1&page_size=40&fields=$FIELDS"
        val tr = parseList(get(url("tr.openfoodfacts.org")))
        val all = if (tr.size >= 8) tr else (tr + parseList(get(url("world.openfoodfacts.org")))).distinctBy { it.name.lowercase() to it.kcal }
        synchronized(cache) { cache[q] = all }
        all
    }

    /** Barkoddan ürün; veritabanında yoksa null. */
    suspend fun byBarcode(code: String): FoodItem? = withContext(Dispatchers.IO) {
        val body = get("https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(code, "UTF-8")}.json?fields=$FIELDS")
        val o = JSONObject(body)
        if (o.optInt("status") != 1) null else o.optJSONObject("product")?.let { parse(it) }
    }

    private fun parseList(body: String): List<FoodItem> {
        val arr = JSONObject(body).optJSONArray("products") ?: return emptyList()
        // Veritabanında aynı ürün birden çok kez kayıtlı olabiliyor: ad + kaloriye göre tekilleştir
        return (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it)) }.distinctBy { it.name.lowercase() to it.kcal }
    }

    /** Porsiyon bilgisi varsa porsiyon başına, yoksa 100 g başına değerler. */
    fun parse(p: JSONObject): FoodItem? {
        val n = p.optJSONObject("nutriments") ?: return null
        val name = p.optString("product_name_tr").ifBlank { p.optString("product_name") }.trim()
        if (name.isBlank()) return null
        val kcal100 = n.num("energy-kcal_100g") ?: n.num("energy_100g")?.let { it / 4.184 } ?: return null
        if (kcal100 <= 0 || kcal100 > 950) return null
        val prot = n.num("proteins_100g") ?: 0.0
        val carb = n.num("carbohydrates_100g") ?: 0.0
        val fat = n.num("fat_100g") ?: 0.0
        val brand = p.optString("brands").split(',').firstOrNull()?.trim().orEmpty()
        val serving = p.optString("serving_quantity").toDoubleOrNull()?.takeIf { it in 1.0..1500.0 }
        val f = (serving ?: 100.0) / 100.0
        val label = if (brand.isNotBlank() && !name.contains(brand, true)) "$name · $brand" else name
        val portion = if (serving != null) "1 porsiyon (${serving.roundToInt()} g)" else "100 g"
        return FoodItem(label, portion, (kcal100 * f).roundToInt(), prot * f, carb * f, fat * f)
    }

    private fun JSONObject.num(key: String): Double? = if (has(key)) optDouble(key).takeIf { !it.isNaN() } else null

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 20_000
            c.setRequestProperty("User-Agent", UA)
            val code = c.responseCode
            if (code == 429) throw OnlineException("Çok hızlı arandı, birkaç saniye sonra tekrar dene.")
            if (code !in 200..299) throw OnlineException("Yemek veritabanına ulaşılamadı ($code).")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
