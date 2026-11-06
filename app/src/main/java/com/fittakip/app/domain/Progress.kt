package com.fittakip.app.domain

import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.WorkoutSet
import kotlin.math.abs

/** y = intercept + slope * x  (x = ilk günden itibaren geçen gün sayısı) */
data class LinearFit(val slope: Double, val intercept: Double, val r2: Double, val n: Int) {
    fun at(x: Double): Double = intercept + slope * x
}

object Stats {

    /** En küçük kareler ile doğrusal regresyon. */
    fun linearFit(xs: List<Double>, ys: List<Double>): LinearFit? {
        val n = xs.size
        if (n < 2 || n != ys.size) return null
        val mx = xs.average()
        val my = ys.average()
        var sxx = 0.0
        var sxy = 0.0
        var syy = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - mx
            val dy = ys[i] - my
            sxx += dx * dx
            sxy += dx * dy
            syy += dy * dy
        }
        if (sxx == 0.0) return null
        val slope = sxy / sxx
        val r2 = if (syy == 0.0) 1.0 else (sxy * sxy) / (sxx * syy)
        return LinearFit(slope, my - slope * mx, r2, n)
    }

    /** Epley formülü ile tahmini 1 tekrar maksimum. */
    fun epley1RM(kg: Double, reps: Int): Double =
        if (reps <= 1) kg else kg * (1 + reps / 30.0)
}

enum class Verdict(val label: String) {
    IMPROVING("Gelişiyorsun 📈"),
    PLATEAU("Plato – yerinde sayıyorsun"),
    DECLINING("Düşüş var 📉"),
    NOT_ENOUGH_DATA("Tahmin için en az 3 farklı antrenman günü lazım")
}

data class ExerciseProgress(
    val exercise: String,
    val history: List<Pair<Long, Double>>,   // (gün, o günün en iyi tahmini 1RM'i)
    val best: Double,
    val fit: LinearFit?,
    val fitStartDay: Long?,
    val weeklyChangePct: Double,
    val projected4Weeks: Double?,
    val verdict: Verdict,
    val confidence: String
)

data class WeightTrend(
    val history: List<Pair<Long, Double>>,   // (gün, o günün ortalama kilosu)
    val current: Double,
    val smoothed: Double,
    val kgPerWeek: Double?,
    val fit: LinearFit?,
    val fitStartDay: Long?,
    val target: Double?,
    val etaDays: Long?,
    val message: String
)

object ProgressAnalyzer {

    /** Güç trendi için son kaç günün verisi kullanılsın. */
    const val STRENGTH_WINDOW_DAYS = 56

    /** Kilo trendi için pencere (su dalgalanmasını yumuşatmak için 4 hafta). */
    const val WEIGHT_WINDOW_DAYS = 28

    /** Haftalık % değişim eşiği: üstü gelişim, altı düşüş, arası plato. */
    const val PLATEAU_THRESHOLD_PCT = 0.5

    fun exercises(sets: List<WorkoutSet>): List<ExerciseProgress> =
        sets.groupBy { it.exercise.trim().lowercase() }
            .values
            .map { analyzeExercise(it) }
            .sortedBy { it.exercise.lowercase() }

    fun analyzeExercise(sets: List<WorkoutSet>): ExerciseProgress {
        val name = sets.first().exercise.trim()
        val history = sets.groupBy { it.epochDay }
            .map { (day, daySets) -> day to daySets.maxOf { Stats.epley1RM(it.weightKg, it.reps) } }
            .sortedBy { it.first }
        val best = history.maxOf { it.second }
        val lastDay = history.last().first
        val recent = history.filter { it.first >= lastDay - STRENGTH_WINDOW_DAYS }

        val empty = ExerciseProgress(
            name, history, best, null, null, 0.0, null, Verdict.NOT_ENOUGH_DATA, "—"
        )
        if (recent.size < 3) return empty

        val x0 = recent.first().first
        val fit = Stats.linearFit(
            recent.map { (it.first - x0).toDouble() },
            recent.map { it.second }
        ) ?: return empty

        val mean = recent.map { it.second }.average()
        val weeklyPct = fit.slope * 7 / mean * 100
        val projected = fit.at((lastDay - x0 + 28).toDouble())

        val verdict = when {
            weeklyPct > PLATEAU_THRESHOLD_PCT -> Verdict.IMPROVING
            weeklyPct < -PLATEAU_THRESHOLD_PCT -> Verdict.DECLINING
            else -> Verdict.PLATEAU
        }
        val confidence = when {
            fit.r2 >= 0.6 && recent.size >= 6 -> "Yüksek"
            fit.r2 >= 0.3 -> "Orta"
            else -> "Düşük"
        }
        return ExerciseProgress(name, history, best, fit, x0, weeklyPct, projected, verdict, confidence)
    }

    fun analyzeWeight(weights: List<BodyWeight>, target: Double?, goal: Goal): WeightTrend? {
        if (weights.isEmpty()) return null

        val history = weights.groupBy { it.epochDay }
            .map { (day, list) -> day to list.map { it.kg }.average() }
            .sortedBy { it.first }

        // Üstel hareketli ortalama: günlük su/tuz dalgalanmasını bastırır
        var smoothed = history.first().second
        for (i in 1 until history.size) smoothed = 0.3 * history[i].second + 0.7 * smoothed

        val lastDay = history.last().first
        val recent = history.filter { it.first >= lastDay - WEIGHT_WINDOW_DAYS }
        val x0 = recent.first().first
        val fit = if (recent.size >= 3) {
            Stats.linearFit(recent.map { (it.first - x0).toDouble() }, recent.map { it.second })
        } else null
        val kgPerWeek = fit?.let { it.slope * 7 }

        var etaDays: Long? = null
        if (target != null && fit != null && abs(fit.slope) > 1e-4) {
            val diff = target - smoothed
            if (diff * fit.slope > 0) {
                val days = (diff / fit.slope).toLong()
                if (days in 1..3650) etaDays = days
            }
        }

        val message = when {
            target != null && abs(smoothed - target) < 0.5 -> "Hedef kilona ulaştın 🎉"
            kgPerWeek == null -> "Trend için son 4 haftada en az 3 farklı günde tartılman lazım."
            goal == Goal.CUT -> when {
                kgPerWeek < -1.0 -> "Çok hızlı veriyorsun, kas kaybı riski var. Günlük kaloriyi ~200 kcal artırmayı düşün."
                kgPerWeek <= -0.2 -> "İdeal tempoda veriyorsun 👍"
                else -> "Kilo düşmüyor. Günlük hedefi ~150-200 kcal azaltmayı ya da yemek kayıtlarını kontrol etmeyi düşün."
            }
            goal == Goal.BULK -> when {
                kgPerWeek > 0.6 -> "Çok hızlı alıyorsun, fazlası yağ olabilir. ~150 kcal azaltmayı düşün."
                kgPerWeek >= 0.1 -> "Kontrollü kilo alıyorsun 👍"
                else -> "Kilo artmıyor. Günlük ~200 kcal eklemeyi düşün."
            }
            else -> if (abs(kgPerWeek) <= 0.2) "Kilon stabil 👍"
            else if (kgPerWeek > 0) "Kilon yukarı kayıyor, kaloriyi biraz azaltabilirsin."
            else "Kilon aşağı kayıyor, kaloriyi biraz artırabilirsin."
        }

        return WeightTrend(
            history = history,
            current = history.last().second,
            smoothed = smoothed,
            kgPerWeek = kgPerWeek,
            fit = fit,
            fitStartDay = if (fit != null) x0 else null,
            target = target,
            etaDays = etaDays,
            message = message
        )
    }
}
