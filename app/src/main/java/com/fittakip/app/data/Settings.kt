package com.fittakip.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val onboarded: Boolean = false,
    val remindersOn: Boolean = true,
    val reminderHour: Int = 20,
    val morningOn: Boolean = false,
    val waterRemindOn: Boolean = false,
    val waterGoalMl: Int = 0,          // 0 = kiloya göre otomatik
    val weeklyWorkoutGoal: Int = 4,
    val kcalAdjust: Int = 0,           // kalori hedefine düzeltme
    val proteinPerKg: Double = 0.0,    // 0 = hedefe göre otomatik
    val fatPct: Int = 25,
    val restSeconds: Int = 90,
    val activeTemplate: String = "",
    val customTemplates: String = "",  // "Ad|Hareket1;Hareket2" satır satır
    val favorites: Set<String> = emptySet(),
    val supplements: Set<String> = setOf("Kreatin", "Multivitamin", "Omega-3"),
    val accent: String = "LIME",
    val sessionStart: Long = 0,
    val streakFreezes: Int = 0,
    val frozenDays: Set<String> = emptySet(),
    val lastFreezeAward: Int = 0,
    val seenInit: Boolean = false,
    val seenAchievements: Set<String> = emptySet(),
    val lastLevel: Int = 1,
    val lastQuestCelebration: Long = 0,
    val weekPlan: String = "",                       // "Push;OFF;Pull;..." (Pazartesi'den Pazar'a)
    val hiddenCards: Set<String> = setOf("FASTING", "QUOTE", "BADGES"),
    val checkinItems: Set<String> = setOf("SLEEP"),
    val programSeeded: Boolean = false
) {
    /** 0 = Pazartesi … 6 = Pazar; boş = plan yok. */
    val plan: List<String> get() = weekPlan.split(";").let { l -> List(7) { i -> l.getOrNull(i)?.trim() ?: "" } }
    val hasPlan: Boolean get() = plan.any { it.isNotEmpty() }

    val frozen: Set<Long> get() = frozenDays.mapNotNull { it.toLongOrNull() }.toSet()

    /** Satır biçimi: "Ad|Bench Press:4x8;Squat:3x10" (eski biçimde hedef yoksa 3×10). */
    val templates: List<WorkoutTemplate>
        get() = customTemplates.lines().mapNotNull { line ->
            val name = line.substringBefore("|").trim()
            val items = line.substringAfter("|", "").split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
                val f = part.split(":")
                val ex = normalizeExercise(f[0])
                val scheme = f.getOrElse(1) { "3x10" }.lowercase().split("x")
                val reps = scheme.getOrElse(1) { "10" }.split("-")
                val r = reps.getOrNull(0)?.toIntOrNull() ?: 10
                PlanItem(
                    ex, scheme.getOrNull(0)?.toIntOrNull() ?: 3, r, reps.getOrNull(1)?.toIntOrNull() ?: r,
                    f.getOrNull(2)?.removePrefix("s")?.toIntOrNull() ?: 0, f.getOrNull(3) ?: ""
                )
            }
            if (name.isEmpty() || items.isEmpty()) null else WorkoutTemplate(name, items)
        }

    val allTemplates: List<WorkoutTemplate> get() = TEMPLATES + templates

    /** epochDay için plandaki şablon adı; "" plan yok, OFF_DAY dinlenme günü. */
    fun planFor(epochDay: Long): String = plan[java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1]

    fun templateFor(epochDay: Long): WorkoutTemplate? = planFor(epochDay).let { n -> allTemplates.firstOrNull { it.name == n } }
}

/** Küçük ayarlar SharedPreferences'ta; değişiklikler StateFlow ile ekrana akar. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fit_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state

    private fun set(key: String, def: Set<String>) = prefs.getStringSet(key, def)?.toSet() ?: def

    private fun read() = AppSettings(
        onboarded = prefs.getBoolean("onboarded", false),
        remindersOn = prefs.getBoolean("remindersOn", true),
        reminderHour = prefs.getInt("reminderHour", 20),
        morningOn = prefs.getBoolean("morningOn", false),
        waterRemindOn = prefs.getBoolean("waterRemindOn", false),
        waterGoalMl = prefs.getInt("waterGoalMl", 0),
        weeklyWorkoutGoal = prefs.getInt("weeklyWorkoutGoal", 4),
        kcalAdjust = prefs.getInt("kcalAdjust", 0),
        proteinPerKg = prefs.getFloat("proteinPerKg", 0f).toDouble(),
        fatPct = prefs.getInt("fatPct", 25),
        restSeconds = prefs.getInt("restSeconds", 90),
        activeTemplate = prefs.getString("activeTemplate", "") ?: "",
        customTemplates = prefs.getString("customTemplates", "") ?: "",
        favorites = set("favorites", emptySet()),
        supplements = set("supplements", setOf("Kreatin", "Multivitamin", "Omega-3")),
        accent = prefs.getString("accent", "LIME") ?: "LIME",
        sessionStart = prefs.getLong("sessionStart", 0),
        streakFreezes = prefs.getInt("streakFreezes", 0),
        frozenDays = set("frozenDays", emptySet()),
        lastFreezeAward = prefs.getInt("lastFreezeAward", 0),
        seenInit = prefs.getBoolean("seenInit", false),
        seenAchievements = set("seenAchievements", emptySet()),
        lastLevel = prefs.getInt("lastLevel", 1),
        lastQuestCelebration = prefs.getLong("lastQuestCelebration", 0),
        weekPlan = prefs.getString("weekPlan", "") ?: "",
        hiddenCards = set("hiddenCards", setOf("FASTING", "QUOTE", "BADGES")),
        checkinItems = set("checkinItems", setOf("SLEEP")),
        programSeeded = prefs.getBoolean("programSeeded", false)
    )

    @Synchronized
    fun update(change: (AppSettings) -> AppSettings) {
        val s = change(_state.value)
        prefs.edit()
            .putBoolean("onboarded", s.onboarded)
            .putBoolean("remindersOn", s.remindersOn)
            .putInt("reminderHour", s.reminderHour)
            .putBoolean("morningOn", s.morningOn)
            .putBoolean("waterRemindOn", s.waterRemindOn)
            .putInt("waterGoalMl", s.waterGoalMl)
            .putInt("weeklyWorkoutGoal", s.weeklyWorkoutGoal)
            .putInt("kcalAdjust", s.kcalAdjust)
            .putFloat("proteinPerKg", s.proteinPerKg.toFloat())
            .putInt("fatPct", s.fatPct)
            .putInt("restSeconds", s.restSeconds)
            .putString("activeTemplate", s.activeTemplate)
            .putString("customTemplates", s.customTemplates)
            .putStringSet("favorites", HashSet(s.favorites))
            .putStringSet("supplements", HashSet(s.supplements))
            .putString("accent", s.accent)
            .putLong("sessionStart", s.sessionStart)
            .putInt("streakFreezes", s.streakFreezes)
            .putStringSet("frozenDays", HashSet(s.frozenDays))
            .putInt("lastFreezeAward", s.lastFreezeAward)
            .putBoolean("seenInit", s.seenInit)
            .putStringSet("seenAchievements", HashSet(s.seenAchievements))
            .putInt("lastLevel", s.lastLevel)
            .putLong("lastQuestCelebration", s.lastQuestCelebration)
            .putString("weekPlan", s.weekPlan)
            .putStringSet("hiddenCards", HashSet(s.hiddenCards))
            .putStringSet("checkinItems", HashSet(s.checkinItems))
            .putBoolean("programSeeded", s.programSeeded)
            .apply()
        _state.value = s
    }
}
