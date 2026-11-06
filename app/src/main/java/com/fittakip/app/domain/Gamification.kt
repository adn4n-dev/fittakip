package com.fittakip.app.domain

import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.BodyMeasure
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.CardioEntry
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.Profile
import com.fittakip.app.data.QUOTES
import com.fittakip.app.data.WaterLog
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.data.muscleOf
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs

data class AllData(
    val foods: List<FoodEntry> = emptyList(),
    val sets: List<WorkoutSet> = emptyList(),
    val weights: List<BodyWeight> = emptyList(),
    val water: List<WaterLog> = emptyList(),
    val cardio: List<CardioEntry> = emptyList(),
    val measures: List<BodyMeasure> = emptyList()
)

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val current: Double,
    val target: Double
) {
    val unlocked get() = current >= target
    val progress get() = (current / target).coerceIn(0.0, 1.0).toFloat()
}

data class Quest(val title: String, val detail: String, val done: Boolean, val xp: Int = 20)

data class WeekStats(
    val start: Long,
    val workoutDays: Int,
    val sets: Int,
    val volume: Double,
    val cardioMin: Int,
    val avgKcal: Int?,
    val avgProtein: Int?,
    val waterGoalDays: Int,
    val weightChange: Double?,
    val prs: Int,
    val muscleVolume: Map<String, Double>
)

data class GameState(
    val activeDays: Set<Long>,
    val streak: Int,
    val longestStreak: Int,
    val xp: Int,
    val level: Int,
    val levelTitle: String,
    val xpIntoLevel: Int,
    val xpForLevel: Int,
    val achievements: List<Achievement>,
    val quests: List<Quest>,
    val weekWorkoutDays: Int,
    val weekGoal: Int,
    val waterGoal: Int,
    val todayWater: Int,
    val todayKcal: Int,
    val todaySets: Int,
    val prCount: Int,
    val totalVolume: Double,
    val quote: String,
    val thisWeek: WeekStats,
    val lastWeek: WeekStats
)

object Gamification {

    // ---------- Seri ----------

    fun activeDays(d: AllData): Set<Long> =
        (d.foods.map { it.epochDay } + d.sets.map { it.epochDay } + d.weights.map { it.epochDay } +
            d.water.map { it.epochDay } + d.cardio.map { it.epochDay }).toSet()

    /** Bugün kayıt yoksa seri dünden sayılır (gün bitene kadar bozulmaz). */
    fun currentStreak(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun longestStreak(days: Set<Long>): Int {
        var best = 0
        for (d in days) {
            if (d - 1 in days) continue
            var n = 0
            var x = d
            while (x in days) { n++; x++ }
            if (n > best) best = n
        }
        return best
    }

    // ---------- Rekorlar ----------

    /** Bir hareketin günlük en iyi 1RM'i önceki tüm günlerden yüksekse = rekor. */
    fun prDays(sets: List<WorkoutSet>): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        sets.groupBy { it.exercise.trim().lowercase() }.values.forEach { list ->
            var best = 0.0
            list.groupBy { it.epochDay }.toSortedMap().forEach { (day, ds) ->
                val dayBest = ds.maxOf { Stats.epley1RM(it.weightKg, it.reps) }
                if (best > 0 && dayBest > best + 1e-6) result += day to list.first().exercise.trim()
                if (dayBest > best) best = dayBest
            }
        }
        return result
    }

    // ---------- Seviye ----------

    fun xpForLevel(level: Int): Int = 50 * level * (level - 1)

    fun levelOf(xp: Int): Int {
        var l = 1
        while (xp >= xpForLevel(l + 1)) l++
        return l
    }

    fun levelTitle(level: Int): String = when {
        level >= 20 -> "Efsane"
        level >= 16 -> "Canavar"
        level >= 12 -> "Demir Bilek"
        level >= 8 -> "Sporcu"
        level >= 5 -> "Düzenli"
        level >= 3 -> "Azimli"
        else -> "Çaylak"
    }

    // ---------- Ana hesap ----------

    fun waterGoal(p: Profile?, st: AppSettings): Int =
        if (st.waterGoalMl > 0) st.waterGoalMl else Calculator.waterGoalMl(p?.weightKg ?: 70.0)

    private fun questsFor(day: Long, d: AllData, waterGoal: Int, proteinTarget: Int?): List<Quest> {
        val foods = d.foods.filter { it.epochDay == day }
        val water = d.water.filter { it.epochDay == day }.sumOf { it.ml }
        val moved = d.sets.any { it.epochDay == day } || d.cardio.any { it.epochDay == day }
        val protein = foods.sumOf { it.protein }
        val list = mutableListOf(
            Quest("Öğünlerini kaydet", "En az 3 yemek gir (${foods.size}/3)", foods.size >= 3),
            Quest("Su hedefini tamamla", "${water} / $waterGoal ml", water >= waterGoal),
            Quest("Hareket et", "Bir set ya da kardiyo kaydet", moved)
        )
        if (proteinTarget != null) {
            list += Quest("Protein hedefi", "${protein.toInt()} / $proteinTarget g", protein >= proteinTarget * 0.9)
        }
        return list
    }

    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

    fun weekStats(d: AllData, start: Long, waterGoal: Int, prs: List<Pair<Long, String>>): WeekStats {
        val range = start..(start + 6)
        val sets = d.sets.filter { it.epochDay in range }
        val foodDays = d.foods.filter { it.epochDay in range }.groupBy { it.epochDay }
        val waterByDay = d.water.filter { it.epochDay in range }.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.ml } }
        val weights = d.weights.filter { it.epochDay in range }.sortedBy { it.epochDay }
        val muscle = sets.groupBy { muscleOf(it.exercise) }.mapValues { e -> e.value.sumOf { it.weightKg * it.reps } }
        return WeekStats(
            start = start,
            workoutDays = sets.map { it.epochDay }.toSet().size,
            sets = sets.size,
            volume = sets.sumOf { it.weightKg * it.reps },
            cardioMin = d.cardio.filter { it.epochDay in range }.sumOf { it.minutes },
            avgKcal = if (foodDays.isEmpty()) null else foodDays.values.map { l -> l.sumOf { it.kcal } }.average().toInt(),
            avgProtein = if (foodDays.isEmpty()) null else foodDays.values.map { l -> l.sumOf { it.protein } }.average().toInt(),
            waterGoalDays = waterByDay.values.count { it >= waterGoal },
            weightChange = if (weights.size >= 2) weights.last().kg - weights.first().kg else null,
            prs = prs.count { it.first in range },
            muscleVolume = muscle
        )
    }

    fun compute(d: AllData, p: Profile?, st: AppSettings, today: Long): GameState {
        val days = activeDays(d)
        val streak = currentStreak(days, today)
        val longest = maxOf(longestStreak(days), streak)
        val prs = prDays(d.sets)
        val workoutDays = d.sets.map { it.epochDay }.toSet()
        val waterGoal = waterGoal(p, st)
        val proteinTarget = p?.let { Calculator.calculate(it, st.kcalAdjust).proteinG }
        val totalVolume = d.sets.sumOf { it.weightKg * it.reps }

        // Geçmiş günlerdeki tamamlanan görevler de XP getirir
        val questXp = days.sumOf { day -> questsFor(day, d, waterGoal, proteinTarget).filter { it.done }.sumOf { it.xp } }
        val xp = d.foods.size * 5 + d.sets.size * 3 + d.weights.size * 10 + d.water.size * 2 +
            d.cardio.size * 15 + d.measures.size * 10 + workoutDays.size * 25 + prs.size * 40 + questXp
        val level = levelOf(xp)

        val waterByDay = d.water.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.ml } }
        val waterGoalDays = waterByDay.values.count { it >= waterGoal }
        val proteinDays = if (proteinTarget == null) 0 else
            d.foods.groupBy { it.epochDay }.values.count { l -> l.sumOf { it.protein } >= proteinTarget * 0.9 }

        val sortedW = d.weights.sortedBy { it.epochDay }
        val startW = sortedW.firstOrNull()?.kg
        val lastW = sortedW.lastOrNull()?.kg
        val target = p?.targetWeightKg
        val towardGoal = if (startW != null && lastW != null && target != null) {
            if (target < startW) startW - lastW else lastW - startW
        } else 0.0
        val benchBest = d.sets.filter { it.exercise.trim().equals("bench press", true) }
            .maxOfOrNull { Stats.epley1RM(it.weightKg, it.reps) } ?: 0.0
        val bw = lastW ?: p?.weightKg ?: 999.0

        val achievements = listOf(
            Achievement("first_food", "🍽️", "İlk lokma", "İlk yemeğini kaydet", d.foods.size.toDouble(), 1.0),
            Achievement("food_100", "📒", "Kayıt ustası", "100 yemek kaydı", d.foods.size.toDouble(), 100.0),
            Achievement("first_set", "🏋️", "İlk set", "İlk setini kaydet", d.sets.size.toDouble(), 1.0),
            Achievement("sets_100", "💯", "Yüzlük", "100 set tamamla", d.sets.size.toDouble(), 100.0),
            Achievement("sets_500", "⛓️", "Demir aşığı", "500 set tamamla", d.sets.size.toDouble(), 500.0),
            Achievement("workouts_10", "🔟", "On antrenman", "10 farklı günde antrenman", workoutDays.size.toDouble(), 10.0),
            Achievement("workouts_50", "🏅", "Elli antrenman", "50 farklı günde antrenman", workoutDays.size.toDouble(), 50.0),
            Achievement("streak_3", "🔥", "Isınma turu", "3 günlük seri", longest.toDouble(), 3.0),
            Achievement("streak_7", "📅", "Bir hafta", "7 günlük seri", longest.toDouble(), 7.0),
            Achievement("streak_30", "🌋", "Alışkanlık", "30 günlük seri", longest.toDouble(), 30.0),
            Achievement("pr_1", "🏆", "İlk rekor", "Bir harekette kişisel rekor kır", prs.size.toDouble(), 1.0),
            Achievement("pr_10", "👑", "Rekor avcısı", "10 kişisel rekor", prs.size.toDouble(), 10.0),
            Achievement("vol_10t", "🪨", "10 ton", "Toplam 10.000 kg kaldır", totalVolume, 10_000.0),
            Achievement("vol_100t", "🏔️", "100 ton", "Toplam 100.000 kg kaldır", totalVolume, 100_000.0),
            Achievement("water_1", "💧", "Su gibi", "Bir gün su hedefini tamamla", waterGoalDays.toDouble(), 1.0),
            Achievement("water_7", "🌊", "Nemli hafta", "7 gün su hedefi", waterGoalDays.toDouble(), 7.0),
            Achievement("protein_7", "🥩", "Protein makinesi", "7 gün protein hedefi", proteinDays.toDouble(), 7.0),
            Achievement("weigh_7", "⚖️", "Tartı dostu", "7 kez tartıl", d.weights.size.toDouble(), 7.0),
            Achievement("kg_1", "📉", "İlk kilo", "Hedefe doğru 1 kg", towardGoal, 1.0),
            Achievement("kg_5", "🎯", "Beş kilo", "Hedefe doğru 5 kg", towardGoal, 5.0),
            Achievement(
                "goal", "🥇", "Hedefte", "Hedef kilona ulaş",
                if (lastW != null && target != null && abs(lastW - target) < 0.5) 1.0 else 0.0, 1.0
            ),
            Achievement("cardio_5", "🏃", "Kardiyo kralı", "5 kardiyo seansı", d.cardio.size.toDouble(), 5.0),
            Achievement("measure_1", "📏", "Mezura", "İlk vücut ölçünü gir", d.measures.size.toDouble(), 1.0),
            Achievement("bench_bw", "💪", "Kendi ağırlığın", "Bench 1RM'in vücut ağırlığına ulaşsın", benchBest, bw),
            Achievement("level_10", "⭐", "Seviye 10", "10. seviyeye çık", level.toDouble(), 10.0)
        )

        val ws = weekStart(today)
        return GameState(
            activeDays = days,
            streak = streak,
            longestStreak = longest,
            xp = xp,
            level = level,
            levelTitle = levelTitle(level),
            xpIntoLevel = xp - xpForLevel(level),
            xpForLevel = xpForLevel(level + 1) - xpForLevel(level),
            achievements = achievements,
            quests = questsFor(today, d, waterGoal, proteinTarget),
            weekWorkoutDays = workoutDays.count { it in ws..(ws + 6) } +
                d.cardio.map { it.epochDay }.filter { it in ws..(ws + 6) && it !in workoutDays }.toSet().size,
            weekGoal = st.weeklyWorkoutGoal,
            waterGoal = waterGoal,
            todayWater = waterByDay[today] ?: 0,
            todayKcal = d.foods.filter { it.epochDay == today }.sumOf { it.kcal },
            todaySets = d.sets.count { it.epochDay == today },
            prCount = prs.size,
            totalVolume = totalVolume,
            quote = QUOTES[(today % QUOTES.size).toInt()],
            thisWeek = weekStats(d, ws, waterGoal, prs),
            lastWeek = weekStats(d, ws - 7, waterGoal, prs)
        )
    }
}
