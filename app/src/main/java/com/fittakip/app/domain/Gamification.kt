package com.fittakip.app.domain

import com.fittakip.app.data.counted
import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.BodyMeasure
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.CHALLENGES
import com.fittakip.app.data.CardioEntry
import com.fittakip.app.data.ChallengeKind
import com.fittakip.app.data.CustomFood
import com.fittakip.app.data.FastEntry
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.HabitLog
import com.fittakip.app.data.Profile
import com.fittakip.app.data.ProgressPhoto
import com.fittakip.app.data.QUOTES
import com.fittakip.app.data.WaterLog
import com.fittakip.app.data.WorkoutSession
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
    val measures: List<BodyMeasure> = emptyList(),
    val habits: List<HabitLog> = emptyList(),
    val fasts: List<FastEntry> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val customFoods: List<CustomFood> = emptyList(),
    val photos: List<ProgressPhoto> = emptyList()
) {
    /** Isınma setleri hacme ve rekorlara sayılmaz. */
    val workSets: List<WorkoutSet> get() = sets.filter { it.counted() }
}

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

data class Challenge(val title: String, val current: Double, val target: Double, val unit: String, val xp: Int = 150) {
    val done get() = current >= target
    val progress get() = (current / target).coerceIn(0.0, 1.0).toFloat()
}

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
    val muscleVolume: Map<String, Double>,
    val muscleSets: Map<String, Int>,
    val avgSleep: Double?
)

data class Totals(
    val activeDays: Int,
    val workoutDays: Int,
    val sets: Int,
    val volume: Double,
    val foods: Int,
    val waterL: Double,
    val cardioMin: Int,
    val fasts: Int,
    val longestFastH: Double,
    val prs: Int,
    val avgSleep: Double?,
    val sessions: Int,
    val avgSessionMin: Int?
)

data class GameState(
    val activeDays: Set<Long>,
    val streakDays: Set<Long>,
    val thisWeekTrainDays: Set<Long>,
    val streak: Int,
    val longestStreak: Int,
    val freezes: Int,
    val xp: Int,
    val level: Int,
    val levelTitle: String,
    val xpIntoLevel: Int,
    val xpForLevel: Int,
    val achievements: List<Achievement>,
    val quests: List<Quest>,
    val challenge: Challenge,
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
    val lastWeek: WeekStats,
    val insights: List<String>,
    val allTime: Totals,
    val thisMonth: Totals,
    val checkinDone: Int,
    val checkinTotal: Int
)

object Gamification {

    // ---------- Seri ----------

    /** Seri için sayılan günler: yemek, set, tartı veya kardiyo. Sadece su/alışkanlık seriyi sürdürmez. */
    fun streakDays(d: AllData): Set<Long> =
        (d.foods.map { it.epochDay } + d.sets.map { it.epochDay } + d.weights.map { it.epochDay } +
            d.cardio.map { it.epochDay }).toSet()

    fun activeDays(d: AllData): Set<Long> =
        (d.foods.map { it.epochDay } + d.sets.map { it.epochDay } + d.weights.map { it.epochDay } +
            d.water.map { it.epochDay } + d.cardio.map { it.epochDay } + d.habits.map { it.epochDay }).toSet()

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
    fun prDays(sets: List<WorkoutSet>, bw: Double = 0.0): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        sets.filter { it.counted() && !Stats.isTimed(it.exercise) }.groupBy { it.exercise.trim().lowercase() }.values.forEach { list ->
            var best = 0.0
            list.groupBy { it.epochDay }.toSortedMap().forEach { (day, ds) ->
                val dayBest = ds.maxOf { Stats.e1rm(it, bw) }
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

    // ---------- Yardımcılar ----------

    fun waterGoal(p: Profile?, st: AppSettings): Int =
        if (st.waterGoalMl > 0) st.waterGoalMl else Calculator.waterGoalMl(p?.weightKg ?: 70.0)

    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

    private fun habitValue(d: AllData, day: Long, type: String): Double? =
        d.habits.lastOrNull { it.epochDay == day && it.type == type }?.value

    /**
     * Günün görevleri: 2 sabit (öğün, su) + günün planına göre antrenman ya da aktif dinlenme
     * + her gün değişen bir bonus görev.
     */
    fun questsFor(day: Long, d: AllData, waterGoal: Int, proteinTarget: Int?, kcalTarget: Int?, st: AppSettings): List<Quest> {
        val foods = d.foods.filter { it.epochDay == day }
        val water = d.water.filter { it.epochDay == day }.sumOf { it.ml }
        val protein = foods.sumOf { it.protein }
        val kcal = foods.sumOf { it.kcal }
        val daySets = d.sets.filter { it.epochDay == day && it.counted() }
        val list = mutableListOf(
            Quest("Öğünlerini kaydet", "En az 3 yemek gir (${foods.size}/3)", foods.size >= 3),
            Quest("Su hedefini tamamla", "$water / $waterGoal ml", water >= waterGoal)
        )
        val dayPlan = PlanResolver.forDay(st, d.sets, day)
        val tpl = (dayPlan as? DayPlan.Workout)?.template
        list += when {
            dayPlan is DayPlan.Off -> {
                val steps = d.habits.lastOrNull { it.epochDay == day && it.type == "STEPS" }?.value ?: 0.0
                val cardioMin = d.cardio.filter { it.epochDay == day }.sumOf { it.minutes }
                Quest("Aktif dinlenme", "20 dk hafif kardiyo ya da 6.000 adım", cardioMin >= 20 || steps >= 6000)
            }
            tpl != null -> {
                val done = tpl.items.count { item -> daySets.count { it.exercise.trim().equals(item.exercise, true) } >= item.sets }
                Quest("${tpl.name} programını bitir", "$done / ${tpl.items.size} hareket hedef sete ulaştı", done == tpl.items.size, 30)
            }
            else -> Quest("Hareket et", "Bir set ya da kardiyo kaydet", daySets.isNotEmpty() || d.cardio.any { it.epochDay == day })
        }
        val bonusPool = buildList {
            if (proteinTarget != null) add(Quest("Protein hedefi", "${protein.toInt()} / $proteinTarget g", protein >= proteinTarget * 0.9))
            add(Quest("Tartıl", "Sabah aç karnına tartı gir", d.weights.any { it.epochDay == day }))
            if (kcalTarget != null) add(Quest("Hedefte kal", "Kalori hedefinin ±%10'u ($kcal / $kcalTarget)", kcal in (kcalTarget * 0.9).toInt()..(kcalTarget * 1.1).toInt()))
            if ("SLEEP" in st.checkinItems) add(Quest("Uykunu kaydet", "Check-in'den dünkü uykunu gir", d.habits.any { it.epochDay == day && it.type == "SLEEP" }))
        }
        if (bonusPool.isNotEmpty()) list += bonusPool[((day % bonusPool.size).toInt() + bonusPool.size) % bonusPool.size].copy(title = "Bonus: " + bonusPool[((day % bonusPool.size).toInt() + bonusPool.size) % bonusPool.size].title)
        return list
    }

    fun weekStats(d: AllData, start: Long, waterGoal: Int, prs: List<Pair<Long, String>>): WeekStats {
        val range = start..(start + 6)
        val sets = d.workSets.filter { it.epochDay in range }
        val foodDays = d.foods.filter { it.epochDay in range }.groupBy { it.epochDay }
        val waterByDay = d.water.filter { it.epochDay in range }.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.ml } }
        val weights = d.weights.filter { it.epochDay in range }.sortedBy { it.epochDay }
        val byMuscle = sets.groupBy { muscleOf(it.exercise) }
        val sleep = d.habits.filter { it.type == "SLEEP" && it.epochDay in range }.map { it.value }
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
            muscleVolume = byMuscle.mapValues { e -> e.value.sumOf { it.weightKg * it.reps } },
            muscleSets = byMuscle.mapValues { it.value.size },
            avgSleep = if (sleep.isEmpty()) null else sleep.average()
        )
    }

    private fun challengeFor(weekStart: Long, d: AllData, waterGoal: Int, proteinTarget: Int?, days: Set<Long>): Challenge {
        val def = CHALLENGES[((weekStart / 7) % CHALLENGES.size).toInt().let { if (it < 0) it + CHALLENGES.size else it }]
        val range = weekStart..(weekStart + 6)
        val current = when (def.kind) {
            ChallengeKind.WORKOUT_DAYS -> d.sets.filter { it.epochDay in range }.map { it.epochDay }.toSet().size.toDouble()
            ChallengeKind.VOLUME -> d.workSets.filter { it.epochDay in range }.sumOf { it.weightKg * it.reps }
            ChallengeKind.WATER_DAYS -> d.water.filter { it.epochDay in range }.groupBy { it.epochDay }
                .values.count { l -> l.sumOf { it.ml } >= waterGoal }.toDouble()
            ChallengeKind.CARDIO -> d.cardio.filter { it.epochDay in range }.sumOf { it.minutes }.toDouble()
            ChallengeKind.PROTEIN_DAYS -> if (proteinTarget == null) 0.0 else d.foods.filter { it.epochDay in range }
                .groupBy { it.epochDay }.values.count { l -> l.sumOf { it.protein } >= proteinTarget * 0.9 }.toDouble()
            ChallengeKind.ACTIVE_DAYS -> days.count { it in range }.toDouble()
            ChallengeKind.SLEEP_DAYS -> d.habits.count { it.type == "SLEEP" && it.epochDay in range && it.value >= 7 }.toDouble()
        }
        return Challenge(def.title, current, def.target, def.unit)
    }

    private fun totals(d: AllData, range: LongRange?, prs: List<Pair<Long, String>>, waterGoal: Int): Totals {
        fun inR(day: Long) = range == null || day in range
        val sets = d.workSets.filter { inR(it.epochDay) }
        val fasts = d.fasts.filter { it.end != null && inR(LocalDate.ofEpochDay(it.start / 86_400_000).toEpochDay()) }
        val sleep = d.habits.filter { it.type == "SLEEP" && inR(it.epochDay) }.map { it.value }
        val sessions = d.sessions.filter { inR(it.epochDay) }
        return Totals(
            activeDays = activeDays(d).count { inR(it) },
            workoutDays = d.sets.filter { inR(it.epochDay) }.map { it.epochDay }.toSet().size,
            sets = sets.size,
            volume = sets.sumOf { it.weightKg * it.reps },
            foods = d.foods.count { inR(it.epochDay) },
            waterL = d.water.filter { inR(it.epochDay) }.sumOf { it.ml } / 1000.0,
            cardioMin = d.cardio.filter { inR(it.epochDay) }.sumOf { it.minutes },
            fasts = fasts.size,
            longestFastH = fasts.maxOfOrNull { (it.end!! - it.start) / 3_600_000.0 } ?: 0.0,
            prs = prs.count { inR(it.first) },
            avgSleep = if (sleep.isEmpty()) null else sleep.average(),
            sessions = sessions.size,
            avgSessionMin = if (sessions.isEmpty()) null else sessions.map { (it.end - it.start) / 60_000 }.average().toInt()
        )
    }

    /** Verilerdeki basit ilişkiler: uyku, protein, su ve antrenman performansı. */
    fun insights(d: AllData, proteinTarget: Int?): List<String> {
        val out = mutableListOf<String>()
        val volByDay = d.workSets.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.weightKg * it.reps } }
        val sleepPrev = volByDay.keys.mapNotNull { day ->
            (d.habits.lastOrNull { it.type == "SLEEP" && (it.epochDay == day || it.epochDay == day - 1) }?.value)?.let { it to volByDay[day]!! }
        }
        val good = sleepPrev.filter { it.first >= 7 }.map { it.second }
        val bad = sleepPrev.filter { it.first < 7 }.map { it.second }
        if (good.size >= 2 && bad.size >= 2) {
            val diff = (good.average() / bad.average() - 1) * 100
            out += if (diff > 0) "7+ saat uyuduğun günlerde antrenman hacmin ortalama %${diff.toInt()} daha yüksek. 😴"
            else "Uyku süresi ile antrenman hacmin arasında belirgin bir fark görünmüyor."
        }
        val moods = d.habits.filter { it.type == "MOOD" }
        if (moods.size >= 4) {
            val trainDays = d.sets.map { it.epochDay }.toSet()
            val onTrain = moods.filter { it.epochDay in trainDays }.map { it.value }
            val off = moods.filter { it.epochDay !in trainDays }.map { it.value }
            if (onTrain.size >= 2 && off.size >= 2 && onTrain.average() > off.average() + 0.2) {
                out += "Antrenman yaptığın günlerde ruh halin daha iyi (${onTrain.average().fmt1()} / ${off.average().fmt1()}). 🙂"
            }
        }
        if (proteinTarget != null) {
            val days = d.foods.groupBy { it.epochDay }.filterKeys { it >= (d.foods.maxOfOrNull { f -> f.epochDay } ?: 0) - 13 }
            if (days.size >= 5) {
                val hit = days.values.count { l -> l.sumOf { it.protein } >= proteinTarget * 0.9 }
                out += "Son 2 haftada ${days.size} günün ${hit} tanesinde protein hedefini tutturdun."
            }
        }
        val caffeine = d.habits.filter { it.type == "CAFFEINE" }
        if (caffeine.size >= 5) out += "Günde ortalama ${caffeine.groupBy { it.epochDay }.values.map { l -> l.sumOf { it.value } }.average().fmt1()} fincan kafein alıyorsun."
        return out
    }

    private fun Double.fmt1() = String.format(java.util.Locale.forLanguageTag("tr"), "%.1f", this)

    // ---------- Ana hesap ----------

    fun compute(d: AllData, p: Profile?, st: AppSettings, today: Long): GameState {
        val realDays = activeDays(d)
        val days = streakDays(d) + st.frozen
        val streak = currentStreak(days, today)
        val longest = maxOf(longestStreak(days), streak)
        val bwNow = d.weights.maxByOrNull { it.epochDay }?.kg ?: p?.weightKg ?: 0.0
        val prs = prDays(d.sets, bwNow)
        val workoutDays = d.sets.map { it.epochDay }.toSet()
        val waterGoal = waterGoal(p, st)
        val calc = p?.let { Calculator.calculate(it, st) }
        val proteinTarget = calc?.proteinG
        val kcalTarget = calc?.target
        val totalVolume = d.workSets.sumOf { it.weightKg * it.reps }

        // Geçmiş haftaların tamamlanan meydan okumaları
        val firstDay = realDays.minOrNull() ?: today
        val challenges = generateSequence(weekStart(firstDay)) { it + 7 }.takeWhile { it <= weekStart(today) }
            .map { challengeFor(it, d, waterGoal, proteinTarget, realDays) }.toList()
        val challengesDone = challenges.count { it.done }

        val questXp = realDays.sumOf { day -> questsFor(day, d, waterGoal, proteinTarget, kcalTarget, st).filter { it.done }.sumOf { it.xp } }
        // XP şişirilemesin: su ve alışkanlıklar kayıt başına değil, gün başına sayılır; yemek günde en fazla 6 kayıt
        val waterXpDays = d.water.groupBy { it.epochDay }.values.count { l -> l.sumOf { it.ml } >= waterGoal }
        val habitDays = d.habits.map { it.epochDay }.toSet().size
        val foodXp = d.foods.groupBy { it.epochDay }.values.sumOf { minOf(it.size, 6) } * 5
        val setXp = d.workSets.groupBy { it.epochDay }.values.sumOf { minOf(it.size, 30) } * 3
        val xp = foodXp + setXp + d.weights.map { it.epochDay }.toSet().size * 10 + waterXpDays * 10 +
            d.cardio.size * 15 + d.measures.size * 10 + workoutDays.size * 25 + prs.size * 40 + questXp +
            challengesDone * 150 + habitDays * 5 + d.fasts.count { it.end != null } * 20 + d.photos.size * 15 + d.sessions.size * 10
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
        fun best(name: String) = d.workSets.filter { it.exercise.trim().equals(name, true) }
            .maxOfOrNull { Stats.epley1RM(it.weightKg, it.reps) } ?: 0.0
        val bw = lastW ?: p?.weightKg ?: 999.0
        val fastHours = d.fasts.filter { it.end != null }.maxOfOrNull { (it.end!! - it.start) / 3_600_000.0 } ?: 0.0
        val sleep7 = d.habits.count { it.type == "SLEEP" && it.value >= 7 }
        val maxSteps = d.habits.filter { it.type == "STEPS" }.maxOfOrNull { it.value } ?: 0.0
        val suppDays = d.habits.filter { it.type == "SUPP" }.groupBy { it.epochDay }.values
            .count { l -> l.map { it.text }.toSet().containsAll(st.supplements) && st.supplements.isNotEmpty() }

        // Programın tamamen uygulandığı haftalar
        val planWeeks = if (!st.hasPlan) 0 else generateSequence(weekStart(firstDay)) { it + 7 }.takeWhile { it + 6 < today + 1 }.count { ws ->
            val trainDays = (0..6).filter { st.plan[it].isNotEmpty() && st.plan[it] != com.fittakip.app.data.OFF_DAY }
            if (st.rotation) {
                // Sıralı modda: o hafta antrenman günü sayısı plandaki antrenman sayısına ulaştı mı
                trainDays.isNotEmpty() && d.workSets.filter { it.epochDay in ws..(ws + 6) }.map { it.epochDay }.toSet().size >= trainDays.size
            } else trainDays.isNotEmpty() && trainDays.all { i -> d.workSets.any { it.epochDay == ws + i } }
        }

        val achievements = listOf(
            Achievement("first_food", "🍽️", "İlk lokma", "İlk yemeğini kaydet", d.foods.size.toDouble(), 1.0),
            Achievement("food_50", "🗒️", "Defter tutan", "50 yemek kaydı", d.foods.size.toDouble(), 50.0),
            Achievement("food_100", "📒", "Kayıt ustası", "100 yemek kaydı", d.foods.size.toDouble(), 100.0),
            Achievement("food_500", "📚", "Ansiklopedi", "500 yemek kaydı", d.foods.size.toDouble(), 500.0),
            Achievement("first_set", "🏋️", "İlk set", "İlk setini kaydet", d.sets.size.toDouble(), 1.0),
            Achievement("sets_100", "💯", "Yüzlük", "100 set tamamla", d.sets.size.toDouble(), 100.0),
            Achievement("sets_250", "🔩", "Çelik", "250 set tamamla", d.sets.size.toDouble(), 250.0),
            Achievement("sets_500", "⛓️", "Demir aşığı", "500 set tamamla", d.sets.size.toDouble(), 500.0),
            Achievement("workouts_10", "🔟", "On antrenman", "10 farklı günde antrenman", workoutDays.size.toDouble(), 10.0),
            Achievement("workouts_25", "🎗️", "Yirmi beş", "25 farklı günde antrenman", workoutDays.size.toDouble(), 25.0),
            Achievement("workouts_50", "🏅", "Elli antrenman", "50 farklı günde antrenman", workoutDays.size.toDouble(), 50.0),
            Achievement("workouts_100", "🏛️", "Yüzüncü gün", "100 farklı günde antrenman", workoutDays.size.toDouble(), 100.0),
            Achievement("streak_3", "🔥", "Isınma turu", "3 günlük seri", longest.toDouble(), 3.0),
            Achievement("streak_7", "📅", "Bir hafta", "7 günlük seri", longest.toDouble(), 7.0),
            Achievement("streak_14", "🔥", "İki hafta", "14 günlük seri", longest.toDouble(), 14.0),
            Achievement("streak_30", "🌋", "Alışkanlık", "30 günlük seri", longest.toDouble(), 30.0),
            Achievement("streak_60", "☄️", "İki ay", "60 günlük seri", longest.toDouble(), 60.0),
            Achievement("streak_100", "💎", "Yüz gün", "100 günlük seri", longest.toDouble(), 100.0),
            Achievement("pr_1", "🏆", "İlk rekor", "Bir harekette kişisel rekor kır", prs.size.toDouble(), 1.0),
            Achievement("pr_5", "🥈", "Rekor serisi", "5 kişisel rekor", prs.size.toDouble(), 5.0),
            Achievement("pr_10", "👑", "Rekor avcısı", "10 kişisel rekor", prs.size.toDouble(), 10.0),
            Achievement("pr_25", "🏆", "Rekor makinesi", "25 kişisel rekor", prs.size.toDouble(), 25.0),
            Achievement("vol_10t", "🪨", "10 ton", "Toplam 10.000 kg kaldır", totalVolume, 10_000.0),
            Achievement("vol_50t", "⛰️", "50 ton", "Toplam 50.000 kg kaldır", totalVolume, 50_000.0),
            Achievement("vol_100t", "🏔️", "100 ton", "Toplam 100.000 kg kaldır", totalVolume, 100_000.0),
            Achievement("vol_250t", "🌍", "250 ton", "Toplam 250.000 kg kaldır", totalVolume, 250_000.0),
            Achievement("plan_week", "🗓️", "Planlı hafta", "Haftalık programın tüm antrenmanlarını bir haftada bitir", planWeeks.toDouble(), 1.0),
            Achievement("plan_4", "📆", "Disiplinli ay", "4 hafta programına tam uy", planWeeks.toDouble(), 4.0),
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
            Achievement("bench_bw", "💪", "Kendi ağırlığın", "Bench 1RM'in vücut ağırlığına ulaşsın", best("bench press"), bw),
            Achievement("squat_15", "🦵", "Bacak günü", "Squat 1RM'in vücut ağırlığının 1,5 katı", best("squat"), bw * 1.5),
            Achievement("dead_2", "🏗️", "Ağır sıklet", "Deadlift 1RM'in vücut ağırlığının 2 katı", best("deadlift"), bw * 2),
            Achievement("level_10", "⭐", "Seviye 10", "10. seviyeye çık", level.toDouble(), 10.0),
            Achievement("fast_16", "⏳", "Oruç ustası", "16 saatlik orucu tamamla", fastHours, 16.0),
            Achievement("sleep_7", "😴", "Uykucu", "7 gece en az 7 saat uyu", sleep7.toDouble(), 7.0),
            Achievement("steps_10k", "👟", "On bin adım", "Bir günde 10.000 adım", maxSteps, 10_000.0),
            Achievement("photo_1", "📸", "Albüm", "İlk ilerleme fotoğrafın", d.photos.size.toDouble(), 1.0),
            Achievement("mood_7", "🧠", "Kendini tanı", "7 gün ruh halini kaydet", d.habits.count { it.type == "MOOD" }.toDouble(), 7.0),
            Achievement("journal_10", "📝", "Günlükçü", "10 günlük not yaz", d.habits.count { it.type == "NOTE" }.toDouble(), 10.0),
            Achievement("challenge_1", "🎖️", "Meydan okuyan", "Bir haftalık meydan okumayı tamamla", challengesDone.toDouble(), 1.0),
            Achievement("challenge_5", "🛡️", "Savaşçı", "5 haftalık meydan okuma", challengesDone.toDouble(), 5.0),
            Achievement("freeze", "🧊", "Kurtarıcı", "Seri dondurma hakkını kullan", st.frozen.size.toDouble(), 1.0),
            Achievement("session_10", "⏱️", "Kronometre", "10 antrenmanı kronometreyle tut", d.sessions.size.toDouble(), 10.0),
            Achievement("template", "🗂️", "Programcı", "Kendi antrenman programını oluştur", st.templates.size.toDouble(), 1.0),
            Achievement("fav_5", "⭐", "Seçici", "5 yemeği favorilere ekle", st.favorites.size.toDouble(), 5.0),
            Achievement("supp_7", "💊", "Takviye düzeni", "7 gün tüm takviyelerini al", suppDays.toDouble(), 7.0),
            Achievement("custom_food", "🧑‍🍳", "Kendi tarifim", "Listene kendi yemeğini ekle", d.customFoods.size.toDouble(), 1.0)
        )

        val ws = weekStart(today)
        val monthStart = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
        val checkTypes = st.checkinItems.toList()
        val checkDone = checkTypes.count { t -> d.habits.any { it.epochDay == today && it.type == t } }

        return GameState(
            activeDays = realDays,
            streakDays = days,
            thisWeekTrainDays = workoutDays.filter { it in ws..(ws + 6) }.toSet(),
            streak = streak,
            longestStreak = longest,
            freezes = st.streakFreezes,
            xp = xp,
            level = level,
            levelTitle = levelTitle(level),
            xpIntoLevel = xp - xpForLevel(level),
            xpForLevel = xpForLevel(level + 1) - xpForLevel(level),
            achievements = achievements,
            quests = questsFor(today, d, waterGoal, proteinTarget, kcalTarget, st),
            challenge = challengeFor(ws, d, waterGoal, proteinTarget, realDays),
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
            lastWeek = weekStats(d, ws - 7, waterGoal, prs),
            insights = insights(d, proteinTarget),
            allTime = totals(d, null, prs, waterGoal),
            thisMonth = totals(d, monthStart..today, prs, waterGoal),
            checkinDone = checkDone,
            checkinTotal = checkTypes.size
        )
    }
}
