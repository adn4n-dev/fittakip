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
    val lastQuestCelebration: Long = 0
) {
    val frozen: Set<Long> get() = frozenDays.mapNotNull { it.toLongOrNull() }.toSet()

    val templates: List<WorkoutTemplate>
        get() = customTemplates.lines().mapNotNull { line ->
            val name = line.substringBefore("|").trim()
            val ex = line.substringAfter("|", "").split(";").map { it.trim() }.filter { it.isNotEmpty() }
            if (name.isEmpty() || ex.isEmpty()) null else WorkoutTemplate(name, ex)
        }
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
        lastQuestCelebration = prefs.getLong("lastQuestCelebration", 0)
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
            .apply()
        _state.value = s
    }
}
