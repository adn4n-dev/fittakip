package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.data.WorkoutTemplate
import com.fittakip.app.data.counted

/** Bir günün planı. */
sealed class DayPlan {
    data class Workout(val template: WorkoutTemplate, val rotation: Boolean) : DayPlan()
    data object Off : DayPlan()
    data object None : DayPlan()
}

/**
 * Program iki şekilde çalışır:
 * - Günlere bağlı: Pazartesi her zaman Upper A.
 * - Sırayla: antrenmanlar sırayla gelir; bir gün kaçırırsan sıra kaymaz, ertesi gün kaldığın yerden devam edersin.
 */
object PlanResolver {

    /** Sıralı moddaki antrenman sırası: haftalık plandaki antrenman günleri, sırasıyla. */
    fun order(st: AppSettings): List<WorkoutTemplate> =
        st.plan.filter { it.isNotEmpty() && it != OFF_DAY }.mapNotNull { n -> st.allTemplates.firstOrNull { it.name == n } }

    /** Bir antrenman günü, sıradaki şablonun hareketlerinin en az yarısını içeriyorsa o şablon tamamlanmış sayılır. */
    private fun matches(t: WorkoutTemplate, daySets: List<WorkoutSet>): Boolean {
        val names = daySets.map { it.exercise.trim().lowercase() }.toSet()
        val hit = t.items.map { it.exercise.lowercase() }.distinct().count { it in names }
        return hit * 2 >= t.items.map { it.exercise.lowercase() }.distinct().size
    }

    /** [day] gününden önce kaç antrenman tamamlandığına göre sıradaki şablonun indeksi. */
    fun rotationIndex(st: AppSettings, sets: List<WorkoutSet>, day: Long): Int {
        val order = order(st)
        if (order.isEmpty()) return -1
        var idx = 0
        sets.filter { it.counted() && it.epochDay >= st.rotationStart && it.epochDay < day }
            .groupBy { it.epochDay }.toSortedMap().values.forEach { ds ->
                if (matches(order[idx % order.size], ds)) idx++
            }
        return idx % order.size
    }

    fun forDay(st: AppSettings, sets: List<WorkoutSet>, day: Long): DayPlan {
        if (st.rotation) {
            val order = order(st)
            if (order.isEmpty()) return DayPlan.None
            return DayPlan.Workout(order[rotationIndex(st, sets, day)], true)
        }
        if (st.planFor(day) == OFF_DAY) return DayPlan.Off
        return st.templateFor(day)?.let { DayPlan.Workout(it, false) } ?: DayPlan.None
    }

    fun templateFor(st: AppSettings, sets: List<WorkoutSet>, day: Long): WorkoutTemplate? =
        (forDay(st, sets, day) as? DayPlan.Workout)?.template

    /** Sıralı modda bugünden sonraki antrenman. */
    fun nextAfter(st: AppSettings, sets: List<WorkoutSet>, day: Long): WorkoutTemplate? {
        val order = order(st)
        if (order.isEmpty()) return null
        return order[(rotationIndex(st, sets, day) + 1) % order.size]
    }

    /** Son antrenmandan bu yana geçen gün (bugün dahil değil). */
    fun daysSinceLastWorkout(sets: List<WorkoutSet>, day: Long): Int? =
        sets.filter { it.counted() && it.epochDay < day }.maxOfOrNull { it.epochDay }?.let { (day - it).toInt() }
}
