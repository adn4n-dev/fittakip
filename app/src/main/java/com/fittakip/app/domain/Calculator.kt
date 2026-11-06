package com.fittakip.app.domain

import com.fittakip.app.data.Profile
import com.fittakip.app.data.WorkoutSet
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

enum class Goal(val label: String, val kcalDelta: Int) {
    CUT("Yağ yak", -500),
    MAINTAIN("Koru", 0),
    BULK("Kas kazan", 300);

    companion object {
        fun from(name: String?): Goal = entries.firstOrNull { it.name == name } ?: MAINTAIN
    }
}

data class ActivityLevel(val factor: Double, val label: String)

val ACTIVITY_LEVELS = listOf(
    ActivityLevel(1.2, "Hareketsiz (masa başı, spor yok)"),
    ActivityLevel(1.375, "Az aktif (haftada 1-3 antrenman)"),
    ActivityLevel(1.55, "Orta aktif (haftada 3-5 antrenman)"),
    ActivityLevel(1.725, "Çok aktif (haftada 6-7 antrenman)"),
    ActivityLevel(1.9, "Aşırı aktif (fiziksel iş + antrenman)")
)

data class CalorieResult(
    val bmr: Int,
    val tdee: Int,
    val target: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int
)

object Calculator {

    /** Mifflin-St Jeor bazal metabolizma hızı (kcal/gün). */
    fun bmr(male: Boolean, kg: Double, cm: Double, age: Int): Double =
        10 * kg + 6.25 * cm - 5 * age + (if (male) 5 else -161)

    fun calculate(
        male: Boolean, age: Int, cm: Double, kg: Double, activity: Double, goal: Goal, adjust: Int = 0
    ): CalorieResult {
        val bmr = bmr(male, kg, cm, age)
        val tdee = bmr * activity
        val floor = if (male) 1500.0 else 1200.0
        val target = max(tdee + goal.kcalDelta + adjust, floor)

        val proteinPerKg = if (goal == Goal.CUT) 2.2 else 1.8
        val protein = kg * proteinPerKg
        val fat = target * 0.25 / 9
        val carb = max(0.0, (target - protein * 4 - fat * 9) / 4)

        return CalorieResult(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            target = target.roundToInt(),
            proteinG = protein.roundToInt(),
            fatG = fat.roundToInt(),
            carbG = carb.roundToInt()
        )
    }

    fun calculate(p: Profile, adjust: Int = 0): CalorieResult =
        calculate(p.male, p.age, p.heightCm, p.weightKg, p.activity, Goal.from(p.goal), adjust)

    /** Kilo başına 35 ml, 250 ml'ye yuvarlanmış. */
    fun waterGoalMl(kg: Double): Int = ((kg * 35 / 250).roundToInt() * 250).coerceIn(1500, 5000)

    fun bmi(kg: Double, cm: Double): Double = kg / ((cm / 100) * (cm / 100))

    fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Zayıf"
        bmi < 25 -> "Normal"
        bmi < 30 -> "Fazla kilolu"
        else -> "Obez"
    }

    /** ABD Donanması formülü ile vücut yağ oranı (%). Kadınlarda kalça da gerekir. */
    fun bodyFatNavy(male: Boolean, heightCm: Double, waist: Double?, neck: Double?, hip: Double?): Double? {
        if (waist == null || neck == null) return null
        return if (male) {
            if (waist <= neck) null
            else 495 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(heightCm)) - 450
        } else {
            if (hip == null || waist + hip <= neck) null
            else 495 / (1.29579 - 0.35004 * log10(waist + hip - neck) + 0.22100 * log10(heightCm)) - 450
        }?.takeIf { it in 2.0..60.0 }
    }

    fun cardioKcal(met: Double, kg: Double, minutes: Int): Int = (met * kg * minutes / 60).roundToInt()

    /** Barın her iki yanına takılacak plakalar. */
    fun plates(total: Double, bar: Double = 20.0): Pair<List<Double>, Double> {
        var perSide = (total - bar) / 2
        if (perSide <= 0) return emptyList<Double>() to 0.0
        val result = mutableListOf<Double>()
        for (p in listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)) {
            while (perSide + 1e-9 >= p) {
                result += p
                perSide -= p
            }
        }
        return result to perSide * 2
    }

    /** Geçen seansa göre bu seans için ağırlık/tekrar önerisi (progressive overload). */
    fun suggestion(history: List<WorkoutSet>, exercise: String, today: Long): String? {
        val past = history.filter { it.exercise.equals(exercise.trim(), true) && it.epochDay < today }
        if (past.isEmpty()) return null
        val lastDay = past.maxOf { it.epochDay }
        val best = past.filter { it.epochDay == lastDay }.maxBy { Stats.epley1RM(it.weightKg, it.reps) }
        val w = best.weightKg
        return when {
            best.reps >= 10 -> "Geçen sefer ${fmtKg(w)}×${best.reps}. Bu sefer ${fmtKg(w + 2.5)} kg ile 8 tekrar dene."
            best.reps >= 6 -> "Geçen sefer ${fmtKg(w)}×${best.reps}. Bu sefer ${fmtKg(w)} kg ile ${best.reps + 1} tekrar dene."
            else -> "Geçen sefer ${fmtKg(w)}×${best.reps}. Aynı ağırlıkla bir tekrar fazlasını hedefle."
        }
    }

    private fun fmtKg(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString().replace('.', ',')
}
