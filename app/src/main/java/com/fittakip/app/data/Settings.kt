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
    val programSeeded: Boolean = false,
    val planMode: String = "WEEKDAY",        // WEEKDAY: günlere bağlı · ROTATION: sırayla
    val rotationStart: Long = 0,
    val hiddenSections: Set<String> = emptySet(),
    val unavailable: Set<String> = emptySet(),      // salonda olmayan makineler
    val machineNotes: String = "{}",                 // {"Leg Press": "koltuk 4, ayak üstte"}
    val autoBackupUri: String = "",                  // kullanıcının seçtiği klasör
    val lastAutoBackup: Long = 0,
    val weekOverrides: String = "",                  // "20400=Upper A;20401=OFF" — sadece içinde bulunulan hafta
    val userName: String = "",
    val googleEmail: String = "",                    // boş = Google ile giriş yapılmadı
    val googleFullName: String = "",
    val lifeStage: String = "",                      // "", "PREGNANT", "BREASTFEEDING" — kalori açığı uygulanmaz
    val healthCondition: Boolean = false,            // diyabet/tansiyon/kalp/tiroid vb. — doktor notu gösterilir
    val fastingMode: Boolean = false,                // oruç modu: öğünler sahur / iftar
    val driveBackupOn: Boolean = false,              // Google Drive'a günlük otomatik yedek
    val lastDriveBackup: Long = 0L,
    val anthropicKey: String = "",                   // fotoğraftan kalori için kullanıcının kendi Claude API anahtarı (yedeğe girmez)
    val modelDownloadId: Long = 0L,                  // telefondaki yapay zekâ modelinin süren indirmesi
    val mealNudges: Boolean = true,                  // öğün saatinde "her zamankini ekle" bildirimi
    val mealNotified: String = ""                    // "20400:BREAKFAST,20400:LUNCH" bugün gösterilenler (cihaza özel)
) {
    /** Bu haftaya özel gün değişiklikleri (gün → antrenman adı ya da OFF). Geçmiş haftalar otomatik yok sayılır. */
    val overrides: Map<Long, String>
        get() {
            val ws = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay()
            return weekOverrides.split(";").mapNotNull { part ->
                val d = part.substringBefore("=").toLongOrNull() ?: return@mapNotNull null
                if (d < ws || d > ws + 6) null else d to part.substringAfter("=", "")
            }.toMap()
        }

    fun withOverrides(m: Map<Long, String>): AppSettings =
        copy(weekOverrides = m.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" })

    fun isAvailable(exercise: String) = unavailable.none { it.equals(exercise.trim(), true) }

    fun machineNote(exercise: String): String =
        try { org.json.JSONObject(machineNotes).optString(exercise.trim(), "") } catch (_: Exception) { "" }

    fun withMachineNote(exercise: String, note: String): AppSettings {
        val o = try { org.json.JSONObject(machineNotes) } catch (_: Exception) { org.json.JSONObject() }
        if (note.isBlank()) o.remove(exercise.trim()) else o.put(exercise.trim(), note.trim())
        return copy(machineNotes = o.toString())
    }

    val rotation: Boolean get() = planMode == "ROTATION"

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

    /** Kendi şablonların, aynı adlı hazır şablonun yerine geçer (hazır programı düzenleyebilmek için). */
    val allTemplates: List<WorkoutTemplate>
        get() {
            val mine = templates
            return mine + TEMPLATES.filter { b -> mine.none { it.name == b.name } }
        }

    /** epochDay için plandaki şablon adı; "" plan yok, OFF_DAY dinlenme günü. */
    /** Programdaki asıl plan (bu haftanın kaydırmaları hariç). */
    /** Haftalık programda kullanılan antrenmanlar. */
    val planTemplates: List<WorkoutTemplate> get() = plan.filter { it.isNotEmpty() && it != OFF_DAY }.distinct()
        .mapNotNull { n -> allTemplates.firstOrNull { it.name == n } }

    fun basePlanFor(epochDay: Long): String = plan[java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1]

    /** O günün geçerli planı: bu hafta kaydırıldıysa kaydırılmış hali. */
    fun planFor(epochDay: Long): String = overrides[epochDay] ?: basePlanFor(epochDay)

    fun isShifted(epochDay: Long): Boolean = overrides[epochDay]?.let { it != basePlanFor(epochDay) } ?: false

    fun templateFor(epochDay: Long): WorkoutTemplate? = planFor(epochDay).let { n -> allTemplates.firstOrNull { it.name == n } }
}

/** Küçük ayarlar SharedPreferences'ta; değişiklikler StateFlow ile ekrana akar. */
class SettingsStore(context: Context) {
    companion object {
        const val PREFS = "fit_settings"
        @Volatile private var instance: SettingsStore? = null
        /** Uygulamanın tamamı (ekranlar, arka plan işleri, widget) aynı kopyayı kullanır; böylece biri diğerinin değişikliğinin üstüne yazmaz. */
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Yedekten geri yükleme sonrası ayarları diskten yeniden oku. */
    fun reload() { _state.value = read() }
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
        programSeeded = prefs.getBoolean("programSeeded", false),
        planMode = prefs.getString("planMode", "WEEKDAY") ?: "WEEKDAY",
        rotationStart = prefs.getLong("rotationStart", 0),
        hiddenSections = set("hiddenSections", emptySet()),
        unavailable = set("unavailable", emptySet()),
        machineNotes = prefs.getString("machineNotes", "{}") ?: "{}",
        autoBackupUri = prefs.getString("autoBackupUri", "") ?: "",
        lastAutoBackup = prefs.getLong("lastAutoBackup", 0),
        weekOverrides = prefs.getString("weekOverrides", "") ?: "",
        userName = prefs.getString("userName", "") ?: "",
        googleEmail = prefs.getString("googleEmail", "") ?: "",
        googleFullName = prefs.getString("googleFullName", "") ?: "",
        lifeStage = prefs.getString("lifeStage", "") ?: "",
        healthCondition = prefs.getBoolean("healthCondition", false),
        fastingMode = prefs.getBoolean("fastingMode", false),
        driveBackupOn = prefs.getBoolean("driveBackupOn", false),
        lastDriveBackup = prefs.getLong("lastDriveBackup", 0L),
        anthropicKey = prefs.getString("anthropicKey", "") ?: "",
        modelDownloadId = prefs.getLong("modelDownloadId", 0L),
        mealNudges = prefs.getBoolean("mealNudges", true),
        mealNotified = prefs.getString("mealNotified", "") ?: ""
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
            .putString("planMode", s.planMode)
            .putLong("rotationStart", s.rotationStart)
            .putStringSet("hiddenSections", HashSet(s.hiddenSections))
            .putStringSet("unavailable", HashSet(s.unavailable))
            .putString("machineNotes", s.machineNotes)
            .putString("autoBackupUri", s.autoBackupUri)
            .putLong("lastAutoBackup", s.lastAutoBackup)
            .putString("weekOverrides", s.weekOverrides)
            .putString("userName", s.userName)
            .putString("googleEmail", s.googleEmail)
            .putString("googleFullName", s.googleFullName)
            .putString("lifeStage", s.lifeStage)
            .putBoolean("healthCondition", s.healthCondition)
            .putBoolean("fastingMode", s.fastingMode)
            .putBoolean("driveBackupOn", s.driveBackupOn)
            .putLong("lastDriveBackup", s.lastDriveBackup)
            .putString("anthropicKey", s.anthropicKey)
            .putLong("modelDownloadId", s.modelDownloadId)
            .putBoolean("mealNudges", s.mealNudges)
            .putString("mealNotified", s.mealNotified)
            .apply()
        _state.value = s
    }
}
