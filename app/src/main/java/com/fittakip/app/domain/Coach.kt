package com.fittakip.app.domain

import com.fittakip.app.data.PlanItem
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.data.WorkoutTemplate
import com.fittakip.app.data.counted
import com.fittakip.app.data.weightStep

/**
 * Antrenman koçu: bir hareketin geçmişini "aynı antrenman" bağlamında okur.
 * Örn. Lat Pulldown Upper A'da geniş, Upper B'de dar tutuşla yapılıyorsa ikisi birbirine karışmaz.
 */
object Coach {

    data class Target(val kg: Double, val reps: Int)

    /** Bir günün setleri hangi antrenmana en çok benziyor? Belirsizse null. */
    fun templateOfDay(daySets: List<WorkoutSet>, templates: List<WorkoutTemplate>): String? {
        val done = daySets.map { it.exercise.trim().lowercase() }.toSet()
        if (done.isEmpty()) return null
        val scored = templates.map { t ->
            val hit = t.items.count { it.exercise.lowercase() in done }
            t.name to (if (t.items.isEmpty()) 0.0 else hit.toDouble() / t.items.size) * 100 + hit
        }.filter { it.second >= 2 * 100.0 / 8 }   // en az ~2 hareket eşleşmeli
            .sortedByDescending { it.second }
        if (scored.isEmpty()) return null
        if (scored.size > 1 && scored[0].second - scored[1].second < 1e-6) return null
        return scored[0].first
    }

    /**
     * [exercise] için [before] gününden önceki sayılan setler (ısınma ve drop hariç).
     * [templateName] verilirse önce o antrenmanın yapıldığı günlere bakılır; hiç yoksa tüm geçmiş döner.
     */
    fun contextHistory(
        history: List<WorkoutSet>, exercise: String, templateName: String?,
        templates: List<WorkoutTemplate>, before: Long
    ): List<WorkoutSet> {
        val past = history.filter { it.counted() && it.exercise.trim().equals(exercise.trim(), true) && it.epochDay < before }
        if (templateName == null || past.isEmpty()) return past
        val days = past.map { it.epochDay }.toSet()
        val dayTemplate = history.filter { it.epochDay in days && it.counted() }.groupBy { it.epochDay }
            .mapValues { templateOfDay(it.value, templates) }
        val same = past.filter { dayTemplate[it.epochDay] == templateName }
        return same.ifEmpty { past }
    }

    /** Bugün kutuya yazılacak ağırlık ve tekrar (koçun önerisiyle aynı). */
    fun nextTarget(past: List<WorkoutSet>, item: PlanItem?): Target? {
        val counted = past.filter { it.counted() }
        if (counted.isEmpty()) return null
        val lastDay = counted.maxOf { it.epochDay }
        val last = counted.filter { it.epochDay == lastDay }
        val w = last.maxOf { it.weightKg }
        val atW = last.filter { it.weightKg == w }
        if (item == null) return Target(w, atW.maxOf { it.reps })
        val allTop = atW.size >= item.sets && atW.all { it.reps >= item.repsMax }
        return when {
            allTop -> Target(w + weightStep(item.exercise), item.reps)
            atW.any { it.reps < item.reps } -> Target(w, item.reps)
            else -> Target(w, (atW.minOf { it.reps } + 1).coerceAtMost(item.repsMax))
        }
    }

    /**
     * Grafikler için: programında birden fazla antrenmanda geçen hareketleri (ör. Lat Pulldown hem
     * Upper A hem Upper B) "Lat Pulldown · Upper A" / "· Upper B" diye ayırır. Kayıtlar değişmez, sadece görünüm.
     */
    fun splitByContext(sets: List<WorkoutSet>, templates: List<WorkoutTemplate>): List<WorkoutSet> {
        if (templates.size < 2) return sets
        val shared = templates.flatMap { t -> t.items.map { it.exercise.lowercase() }.distinct() }
            .groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
        if (shared.isEmpty()) return sets
        val dayTemplate = sets.filter { it.counted() }.groupBy { it.epochDay }.mapValues { templateOfDay(it.value, templates) }
        return sets.map { s ->
            val key = s.exercise.trim().lowercase()
            val t = dayTemplate[s.epochDay]
            if (key in shared && t != null && templates.first { it.name == t }.items.any { it.exercise.lowercase() == key })
                s.copy(exercise = "${s.exercise.trim()} · $t") else s
        }
    }
}
