package com.fittakip.app.domain

import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.FoodItem
import com.fittakip.app.data.Meal
import java.util.Locale
import kotlin.math.roundToInt

/** "Üşengeç modu": yazmadan kayıt. Alışkanlık öğünleri ve sesli cümleyi yerel listeyle eşleştirme. */
object Routine {
    data class Usual(val meal: Meal, val items: List<FoodEntry>) {
        val kcal: Int get() = items.sumOf { it.kcal }
        val label: String get() = items.joinToString(", ") { it.name }
    }

    /**
     * Son [days] günde bu öğünde en az [minDays] farklı günde yenen yiyecekler = "her zamanki" öğün.
     * Değerler o yiyeceğin bu öğündeki en son kaydından alınır. En sık 6 yiyecek.
     */
    fun usual(foods: List<FoodEntry>, meal: Meal, today: Long, days: Int = 14, minDays: Int = 3): Usual? {
        val recent = foods.filter { Meal.from(it.meal) == meal && it.epochDay in (today - days) until today }
        if (recent.isEmpty()) return null
        val byName = recent.groupBy { it.name.trim().lowercase(Locale("tr")) }
        val picked = byName.values
            .map { list -> list.map { it.epochDay }.distinct().size to list.maxBy { it.epochDay * 100_000 + it.id } }
            .filter { it.first >= minDays }
            .sortedByDescending { it.first }
            .take(6)
            .map { it.second }
        return if (picked.isEmpty()) null else Usual(meal, picked)
    }
}

object VoiceParser {
    data class Match(val item: FoodItem, val qty: Double, val heard: String) {
        val kcal: Int get() = (item.kcal * qty).roundToInt()
    }
    data class Parsed(val matches: List<Match>, val unknown: List<String>)

    private val tr = Locale("tr")
    private val NUMBERS = mapOf(
        "yarım" to 0.5, "bir" to 1.0, "iki" to 2.0, "üç" to 3.0, "dört" to 4.0, "beş" to 5.0,
        "altı" to 6.0, "yedi" to 7.0, "sekiz" to 8.0, "dokuz" to 9.0, "on" to 10.0, "çeyrek" to 0.25
    )
    private val UNITS = setOf(
        "tane", "adet", "dilim", "bardak", "kase", "tabak", "porsiyon", "kaşık", "yemek", "tatlı", "çay", "avuç",
        "parça", "kutu", "şişe", "paket", "fincan", "kupa", "top", "kepçe"
    )
    private val FILLER = setOf("ve", "ile", "bir de", "de", "da", "yedim", "içtim", "yiyorum", "içiyorum", "biraz", "bi", "artı")

    /** Kelimeyi yumuşak karşılaştırma için sadeleştir (Türkçe küçük harf, parantez ve noktalama yok). */
    fun norm(s: String): String = s.lowercase(tr).replace(Regex("\\(.*?\\)"), " ").replace(Regex("[^a-zçğıöşü0-9 ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    /**
     * "iki yumurta, bir simit ve bir bardak çay" → [2× yumurta, 1× simit, 1× çay].
     * [catalog]: önce kullanıcının kendi geçmişi/yemekleri, sonra hazır liste (öncelik sırasıyla).
     */
    fun parse(text: String, catalog: List<FoodItem>): Parsed {
        val segments = text.lowercase(tr).split(',', '+', '\n', '.')
            .flatMap { it.split(" ve ", " ile ", " artı ") }
            .map { norm(it) }.filter { it.isNotBlank() }
        val matches = mutableListOf<Match>()
        val unknown = mutableListOf<String>()
        for (seg in segments) {
            var words = seg.split(' ').filter { it.isNotBlank() && it !in FILLER }
            var qty = 1.0
            // Miktar: "2", "iki", "bir buçuk", "yarım"
            words.firstOrNull()?.let { w ->
                val n = w.replace(',', '.').toDoubleOrNull() ?: NUMBERS[w]
                if (n != null) {
                    qty = n; words = words.drop(1)
                    if (words.firstOrNull() == "buçuk") { qty += 0.5; words = words.drop(1) }
                }
            }
            // Birim kelimesini at ("bir BARDAK çay"); ama "çay" tek başına yemek adıysa koru
            if (words.size > 1 && words.first() in UNITS) words = words.drop(1)
            if (words.size > 1 && words.first() == "kaşığı") words = words.drop(1)
            val query = words.joinToString(" ")
            if (query.isBlank()) continue
            val best = bestMatch(query, catalog)
            if (best != null) matches += Match(best, qty, seg) else unknown += seg
        }
        return Parsed(matches, unknown)
    }

    private fun stem(w: String) = if (w.length > 4) w.take(w.length - 1) else w

    /** En iyi eşleşme: tüm sorgu kelimeleri adda geçiyorsa güçlü; kısa ve öz adlar tercih edilir. */
    fun bestMatch(query: String, catalog: List<FoodItem>): FoodItem? {
        val q = query.split(' ').filter { it.length >= 2 }
        if (q.isEmpty()) return null
        var best: FoodItem? = null
        var bestScore = 0.0
        catalog.forEachIndexed { idx, item ->
            val name = norm(item.name)
            val nameWords = name.split(' ')
            val hit = q.count { qw -> nameWords.any { nw -> nw == qw || nw.startsWith(stem(qw)) || qw.startsWith(stem(nw)) } }
            if (hit == 0) return@forEachIndexed
            val coverage = hit.toDouble() / q.size
            if (coverage < 0.6) return@forEachIndexed
            // Ad ne kadar sorguya yakınsa (fazla kelime yoksa) o kadar iyi; listede önde olan (kişisel geçmiş) hafif öncelikli
            val tightness = hit.toDouble() / nameWords.size
            val score = coverage * 2 + tightness + (if (name.startsWith(q.first())) 0.3 else 0.0) - idx * 1e-5
            if (score > bestScore) { bestScore = score; best = item }
        }
        return best
    }
}
