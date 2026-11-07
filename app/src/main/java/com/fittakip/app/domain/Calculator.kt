package com.fittakip.app.domain

import com.fittakip.app.data.counted
import com.fittakip.app.data.AppSettings
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
        male: Boolean, age: Int, cm: Double, kg: Double, activity: Double, goal: Goal, adjust: Int = 0,
        proteinOverride: Double = 0.0, fatPct: Int = 25
    ): CalorieResult {
        val bmr = bmr(male, kg, cm, age)
        val tdee = bmr * activity
        val floor = if (male) 1500.0 else 1200.0
        val target = max(tdee + goal.kcalDelta + adjust, floor)

        val proteinPerKg = if (proteinOverride > 0) proteinOverride else if (goal == Goal.CUT) 2.2 else 1.8
        val protein = kg * proteinPerKg
        val fat = target * fatPct.coerceIn(10, 60) / 100.0 / 9
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

    /** Ayarlardaki kalori düzeltmesi ve makro tercihleriyle. */
    fun calculate(p: Profile, st: AppSettings): CalorieResult =
        calculate(p.male, p.age, p.heightCm, p.weightKg, p.activity, effectiveGoal(p, st.lifeStage), st.kcalAdjust, st.proteinPerKg, st.fatPct)

    /** Hamilelik ve emzirmede kalori açığı uygulanmaz: "yağ yak" seçili olsa bile koruma kalorisi verilir. */
    fun effectiveGoal(p: Profile, lifeStage: String): Goal {
        val g = Goal.from(p.goal)
        return if (!p.male && lifeStage.isNotEmpty() && g == Goal.CUT) Goal.MAINTAIN else g
    }

    fun bmiOf(kg: Double, cm: Double): Double = kg / ((cm / 100) * (cm / 100))

    /** Kullanıcıya gösterilecek güvenlik uyarıları (boşsa her şey yolunda). */
    fun safetyWarnings(
        male: Boolean, cm: Double?, kg: Double?, target: Double?, goal: Goal,
        lifeStage: String, healthCondition: Boolean
    ): List<String> = buildList {
        if (!male && lifeStage.isNotEmpty()) add(
            (if (lifeStage == "PREGNANT") "Hamilelikte" else "Emzirirken") +
                " kalori açığı uygulamıyorum; \"yağ yak\" seçsen de koruma kalorisi gösteririm. Bu dönemde kalori ihtiyacın artar, hedefini doktorunla ya da diyetisyeninle belirle."
        )
        if (cm != null && cm in 100.0..250.0) {
            val (lo, hi) = idealWeightRange(cm)
            if (kg != null && bmiOf(kg, cm) < 18.5 && goal == Goal.CUT)
                add("Vücut kitle indeksin ${"%.1f".format(bmiOf(kg, cm))}, yani zayıf aralıktasın. Kilo vermek yerine korumayı ya da kas kazanmayı öneririm.")
            if (target != null && target < lo)
                add("Hedef kilon ${fmtKg(target)} kg, boyuna göre sağlıklı aralığın (${lo.roundToInt()}–${hi.roundToInt()} kg) altında. Tahminleri gösteririm ama bu kiloya inmeni önermem.")
        }
        if (healthCondition) add("Bir rahatsızlığın olduğu için kalori, karbonhidrat ve antrenman hedeflerini doktorunla birlikte gözden geçir. Buradaki hesaplar genel tahmindir.")
    }

    /**
     * Gerçek kalori ihtiyacı: son 28 günde ortalama alınan kalori − (kilo eğimi × 7700 kcal/kg).
     * En az 14 gün yemek kaydı ve 14 güne yayılmış 6 tartı gerekir.
     */
    data class AdaptiveTdee(val tdee: Int, val avgIntake: Int, val kgPerWeek: Double, val loggedDays: Int, val confidence: String)

    fun adaptiveTdee(
        foods: List<com.fittakip.app.data.FoodEntry>,
        weights: List<com.fittakip.app.data.BodyWeight>,
        today: Long,
        window: Int = 28
    ): AdaptiveTdee? {
        val from = today - window
        val byDay = foods.filter { it.epochDay in from until today }.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.kcal } }
            .filterValues { it >= 800 } // yarım kalmış günleri sayma
        val ws = weights.filter { it.epochDay in from..today }.sortedBy { it.epochDay }
        if (byDay.size < 14 || ws.size < 6 || ws.last().epochDay - ws.first().epochDay < 14) return null
        val x0 = ws.first().epochDay
        val fit = Stats.linearFit(ws.map { (it.epochDay - x0).toDouble() }, ws.map { it.kg }) ?: return null
        val avg = byDay.values.average()
        val tdee = avg - fit.slope * 7700
        val conf = when {
            byDay.size >= 24 && ws.size >= 12 -> "Yüksek"
            byDay.size >= 18 -> "Orta"
            else -> "Düşük"
        }
        return AdaptiveTdee(tdee.roundToInt(), avg.roundToInt(), fit.slope * 7, byDay.size, conf)
    }

    /** BMI 18,5–24,9 aralığına denk gelen kilo aralığı. */
    fun idealWeightRange(cm: Double): Pair<Double, Double> {
        val m2 = (cm / 100) * (cm / 100)
        return 18.5 * m2 to 24.9 * m2
    }

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

    /** Geçen seansa göre bu seans için ağırlık/tekrar önerisi (progressive overload). */
    /**
     * Çift ilerleme (double progression): hedef aralığın üst sınırına tüm setlerde ulaştıysan ağırlık artır,
     * ulaşmadıysan aynı ağırlıkla tekrar artırmaya devam et.
     */
    fun rangeSuggestion(history: List<WorkoutSet>, item: com.fittakip.app.data.PlanItem, today: Long): String? {
        val past = history.filter { it.counted() && it.exercise.equals(item.exercise, true) && it.epochDay < today }
        if (past.isEmpty()) return "İlk sefer: ${item.label} hedefle, son tekrarlar zorlasın ama form bozulmasın."
        val lastDay = past.maxOf { it.epochDay }
        val last = past.filter { it.epochDay == lastDay }
        val w = last.maxOf { it.weightKg }
        val atW = last.filter { it.weightKg == w }
        val allTop = atW.size >= item.sets && atW.all { it.reps >= item.repsMax }
        val step = com.fittakip.app.data.weightStep(item.exercise)
        return when {
            allTop -> "Geçen sefer ${fmtKg(w)} kg ile tüm setlerde ${item.repsMax} tekrar yaptın 🎯 Bu sefer ${fmtKg(w + step)} kg'a çık, ${item.reps} tekrardan başla."
            atW.any { it.reps < item.reps } -> "Geçen sefer ${fmtKg(w)} kg'da ${item.reps} tekrarın altına düştün. Aynı ağırlıkta kal, aralığın içine gir."
            else -> "Geçen sefer ${fmtKg(w)} kg: ${atW.joinToString("/") { it.reps.toString() }}. Aynı ağırlıkla her sette 1 tekrar daha dene (hedef ${item.repsMax})."
        }
    }

    fun suggestion(history: List<WorkoutSet>, exercise: String, today: Long): String? {
        val past = history.filter { it.counted() && it.exercise.equals(exercise.trim(), true) && it.epochDay < today }
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
