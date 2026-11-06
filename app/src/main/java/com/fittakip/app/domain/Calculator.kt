package com.fittakip.app.domain

import com.fittakip.app.data.Profile
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

    fun calculate(male: Boolean, age: Int, cm: Double, kg: Double, activity: Double, goal: Goal): CalorieResult {
        val bmr = bmr(male, kg, cm, age)
        val tdee = bmr * activity
        // Diyette çok düşmesin diye güvenli taban
        val floor = if (male) 1500.0 else 1200.0
        val target = max(tdee + goal.kcalDelta, floor)

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

    fun calculate(p: Profile): CalorieResult =
        calculate(p.male, p.age, p.heightCm, p.weightKg, p.activity, Goal.from(p.goal))
}
