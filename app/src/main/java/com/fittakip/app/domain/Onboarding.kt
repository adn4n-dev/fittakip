package com.fittakip.app.domain

import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.WEEK_PRESETS
import com.fittakip.app.data.WeekPreset

/** Kurulum sihirbazının karar mantığı (ekrandan bağımsız, test edilebilir). */
object OnboardingLogic {

    /** Seviye: 0 yeni başlayan, 1 orta, 2 ileri. Haftalık gün sayısına göre en uygun hazır program. */
    fun recommend(daysPerWeek: Int, level: Int): WeekPreset {
        fun byName(prefix: String) = WEEK_PRESETS.first { it.name.startsWith(prefix) }
        return when {
            daysPerWeek <= 3 && level == 0 -> byName("Full Body")
            daysPerWeek <= 3 -> WEEK_PRESETS.first()          // Upper A / Lower / Upper B
            daysPerWeek == 4 -> byName("Üst / Alt")
            level == 0 -> byName("Üst / Alt")
            else -> byName("PPL · 6")
        }
    }

    fun workouts(preset: WeekPreset): List<String> = preset.days.filter { it.isNotEmpty() && it != OFF_DAY }

    /** Seçilen günler programın antrenman sayısıyla aynıysa günlere bağlı, değilse sırayla modu daha uygun. */
    fun suggestRotation(preset: WeekPreset, selectedDays: Set<Int>): Boolean =
        selectedDays.isNotEmpty() && selectedDays.size != workouts(preset).size

    /**
     * Haftalık planı kurar. Günlere bağlı modda antrenmanlar seçilen günlere sırayla yerleşir;
     * sırayla modda sıra önemli olduğu için programın tüm antrenmanları korunur.
     */
    fun buildPlan(preset: WeekPreset, selectedDays: Set<Int>, rotation: Boolean): List<String> {
        val seq = workouts(preset)
        if (seq.isEmpty() || selectedDays.isEmpty()) return preset.days
        if (rotation && selectedDays.size < seq.size) return preset.days
        val sorted = selectedDays.sorted()
        return (0..6).map { d ->
            val i = sorted.indexOf(d)
            if (i < 0) OFF_DAY else seq[i % seq.size]
        }
    }

    /** Programın varsayılan antrenman günleri (Pazartesi = 0). */
    fun defaultDays(preset: WeekPreset): Set<Int> =
        preset.days.mapIndexedNotNull { i, d -> if (d.isNotEmpty() && d != OFF_DAY) i else null }.toSet()
}
