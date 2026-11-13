package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.Profile
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.data.counted
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Haftalık karne: "İlerliyor muyum?" sorusuna tek cümlelik cevap + yapılacak tek şey.
 * Son 7 gün (bugün dahil) üzerinden.
 */
data class Karne(
    val workoutsDone: Int,
    val workoutsPlanned: Int,
    val kcalDaysLogged: Int,
    val kcalDaysOnTarget: Int,
    val strengthChangePct: Double?,   // programdaki hareketlerin ortalama güç değişimi (son 2 hafta vs önceki 4 hafta)
    val improvedLifts: List<String>,
    val kgPerWeek: Double?,
    val progressing: Boolean?,         // null = yeterli veri yok
    val headline: String,
    val tip: String
) {
    val summary: String get() = buildList {
        add("$workoutsDone/$workoutsPlanned antrenman")
        if (kcalDaysLogged > 0) add("kalori $kcalDaysOnTarget/$kcalDaysLogged gün hedefte")
        strengthChangePct?.let { add("güç " + (if (it >= 0) "+" else "") + "%" + "%.1f".format(it)) }
        kgPerWeek?.let { add((if (it >= 0) "+" else "") + "%.1f".format(it) + " kg/hafta") }
    }.joinToString(" · ")

    companion object {
        fun compute(
            sets: List<WorkoutSet>, foods: List<FoodEntry>, weights: List<BodyWeight>,
            profile: Profile?, st: AppSettings, today: Long
        ): Karne {
            val from = today - 6
            val bw = weights.maxByOrNull { it.epochDay }?.kg ?: profile?.weightKg ?: 0.0

            val done = sets.filter { it.counted() && it.epochDay in from..today }.map { it.epochDay }.distinct().size
            val planned = if (st.rotation) st.weeklyWorkoutGoal
            else st.plan.count { it.isNotEmpty() && it != OFF_DAY }.takeIf { it > 0 } ?: st.weeklyWorkoutGoal

            // Kalori: bugün hariç, kayıt girilmiş (≥ 500 kcal) günler; hedefin ±%10'u "hedefte"
            val target = profile?.let { Calculator.calculate(it, st).target }
            val dayKcal = foods.filter { it.epochDay in from until today }.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.kcal } }
                .filterValues { it >= 500 }
            val onTarget = if (target == null) 0 else dayKcal.values.count { abs(it - target) <= target * 0.10 }

            // Güç: programdaki her hareket için son 14 gün en iyi 1RM vs 15-42 gün önceki en iyi 1RM
            val lifts = st.planTemplates.flatMap { t -> t.items.map { it.exercise } }.distinct()
            val changes = lifts.mapNotNull { ex ->
                val mine = sets.filter { it.counted() && it.exercise.trim().equals(ex, true) && it.reps > 0 }
                val recent = mine.filter { it.epochDay in (today - 13)..today }.maxOfOrNull { Stats.e1rm(it, bw) }
                val before = mine.filter { it.epochDay in (today - 42)..(today - 14) }.maxOfOrNull { Stats.e1rm(it, bw) }
                if (recent == null || before == null || before <= 0) null else ex to (recent - before) / before * 100
            }
            val strength = if (changes.isEmpty()) null else changes.map { it.second }.average()
            val improved = changes.filter { it.second >= 2.0 }.sortedByDescending { it.second }.map { it.first }

            val goal = Goal.from(profile?.goal)
            val kgWeek = ProgressAnalyzer.analyzeWeight(weights, profile?.targetWeightKg, goal)?.kgPerWeek
            val weightOk = when {
                kgWeek == null -> null
                goal == Goal.CUT -> kgWeek <= -0.1
                goal == Goal.BULK -> kgWeek >= 0.05
                else -> abs(kgWeek) < 0.3
            }
            val strengthOk = strength?.let { it >= 1.0 }

            val progressing = when {
                strengthOk == null && weightOk == null -> null
                else -> (strengthOk == true) || (weightOk == true && strengthOk != false)
            }
            val headline = when {
                done == 0 && dayKcal.isEmpty() -> "Bu hafta kayıt yok"
                progressing == null -> "Henüz karar vermek için veri az"
                progressing -> "✅ Evet, ilerliyorsun"
                else -> "⚠️ Yerinde sayıyorsun"
            }
            val tip = when {
                done < planned -> "Bu hafta ${planned - done} antrenman eksik; önce onu tamamla."
                strengthOk == false -> "Ağırlıklar artmıyor: setlerde kutuya gelen koç önerisini dene."
                dayKcal.size < 4 -> "Kalori kaydı az; her zamanki öğünü tek dokunuşla eklemek yeter."
                weightOk == false && goal == Goal.CUT -> "Kilo inmiyor: günlük hedefin ~150 kcal altında kal."
                weightOk == false && goal == Goal.BULK -> "Kilo artmıyor: günde ~200 kcal daha ye."
                improved.isNotEmpty() -> "En çok gelişen: ${improved.take(2).joinToString(", ")}. Böyle devam! 💪"
                else -> "Böyle devam! 💪"
            }
            return Karne(done, planned, dayKcal.size, onTarget, strength?.let { (it * 10).roundToInt() / 10.0 }, improved, kgWeek, progressing, headline, tip)
        }
    }
}
