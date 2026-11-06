package com.fittakip.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val onboarded: Boolean = false,
    val remindersOn: Boolean = true,
    val reminderHour: Int = 20,
    val waterGoalMl: Int = 0,          // 0 = kiloya göre otomatik
    val weeklyWorkoutGoal: Int = 4,
    val kcalAdjust: Int = 0,           // kalori hedefine elle/otomatik düzeltme
    val restSeconds: Int = 90,
    val activeTemplate: String = "",
    val seenInit: Boolean = false,
    val seenAchievements: Set<String> = emptySet(),
    val lastLevel: Int = 1
)

/** Küçük ayarlar SharedPreferences'ta; değişiklikler StateFlow ile ekrana akar. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fit_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state

    private fun read() = AppSettings(
        onboarded = prefs.getBoolean("onboarded", false),
        remindersOn = prefs.getBoolean("remindersOn", true),
        reminderHour = prefs.getInt("reminderHour", 20),
        waterGoalMl = prefs.getInt("waterGoalMl", 0),
        weeklyWorkoutGoal = prefs.getInt("weeklyWorkoutGoal", 4),
        kcalAdjust = prefs.getInt("kcalAdjust", 0),
        restSeconds = prefs.getInt("restSeconds", 90),
        activeTemplate = prefs.getString("activeTemplate", "") ?: "",
        seenInit = prefs.getBoolean("seenInit", false),
        seenAchievements = prefs.getStringSet("seenAchievements", emptySet())?.toSet() ?: emptySet(),
        lastLevel = prefs.getInt("lastLevel", 1)
    )

    @Synchronized
    fun update(change: (AppSettings) -> AppSettings) {
        val s = change(_state.value)
        prefs.edit()
            .putBoolean("onboarded", s.onboarded)
            .putBoolean("remindersOn", s.remindersOn)
            .putInt("reminderHour", s.reminderHour)
            .putInt("waterGoalMl", s.waterGoalMl)
            .putInt("weeklyWorkoutGoal", s.weeklyWorkoutGoal)
            .putInt("kcalAdjust", s.kcalAdjust)
            .putInt("restSeconds", s.restSeconds)
            .putString("activeTemplate", s.activeTemplate)
            .putBoolean("seenInit", s.seenInit)
            .putStringSet("seenAchievements", HashSet(s.seenAchievements))
            .putInt("lastLevel", s.lastLevel)
            .apply()
        _state.value = s
    }
}
