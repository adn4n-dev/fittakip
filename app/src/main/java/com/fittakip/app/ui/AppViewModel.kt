package com.fittakip.app.ui

import com.fittakip.app.data.counted
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittakip.app.data.*
import com.fittakip.app.domain.Achievement
import com.fittakip.app.domain.AllData
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Gamification
import com.fittakip.app.domain.GameState
import com.fittakip.app.domain.Stats
import com.fittakip.app.notify.Reminders
import com.fittakip.app.notify.RestAlarm
import com.fittakip.app.widget.FitWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.roundToInt

data class ProfileState(val loaded: Boolean, val profile: Profile?)

sealed class Celebration {
    data class PR(val exercise: String, val newBest: Double, val oldBest: Double) : Celebration()
    data class Badge(val achievement: Achievement) : Celebration()
    data class LevelUp(val level: Int, val title: String) : Celebration()
    data class Custom(val emoji: String, val title: String, val body: String) : Celebration()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    private val store = SettingsStore(app)
    val settings: StateFlow<AppSettings> = store.state
    /** Her okunuşta güncel gün: gece yarısından sonra kayıtlar doğru güne yazılır. */
    val today: Long get() = LocalDate.now().toEpochDay()

    /** Gün değişince oyun durumunu yeniden hesaplatmak için. */
    private val dayTicker: Flow<Long> = flow {
        while (true) {
            emit(LocalDate.now().toEpochDay())
            kotlinx.coroutines.delay(60_000)
        }
    }.distinctUntilChanged()

    private fun <T> Flow<T>.state(initial: T) = stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val profile: StateFlow<ProfileState> = dao.profile().map { ProfileState(true, it) }.state(ProfileState(false, null))
    val foods = dao.allFoods().state(emptyList())
    val sets = dao.allSets().state(emptyList())
    val weights = dao.weights().state(emptyList())
    val water = dao.water().state(emptyList())
    val cardio = dao.cardio().state(emptyList())
    val measures = dao.measures().state(emptyList())
    val habits = dao.habits().state(emptyList())
    val fasts = dao.fasts().state(emptyList())
    val sessions = dao.sessions().state(emptyList())
    val customFoods = dao.customFoods().state(emptyList())
    val photos = dao.photos().state(emptyList())
    val savedMeals = dao.savedMeals().state(emptyList())

    private val _day = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDay: StateFlow<Long> = _day
    fun shiftDay(delta: Int) { _day.value = (_day.value + delta).coerceAtMost(today) }

    /** Antrenman ekranında set eklenen gün (geçmişe set girmek için). */
    private val _workoutDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val workoutDay: StateFlow<Long> = _workoutDay
    fun shiftWorkoutDay(delta: Int) { _workoutDay.value = (_workoutDay.value + delta).coerceIn(today - 30, today) }

    /** Hareket detay ekranında gösterilen hareket. */
    val detailExercise = MutableStateFlow("")

    private val allData: Flow<AllData> =
        combine(dao.allFoods(), dao.allSets(), dao.weights(), dao.water(), dao.cardio()) { f, s, w, wa, c ->
            AllData(foods = f, sets = s, weights = w, water = wa, cardio = c)
        }.combine(combine(dao.measures(), dao.habits(), dao.fasts(), dao.sessions(), dao.customFoods()) { m, h, fa, se, cf ->
            AllData(measures = m, habits = h, fasts = fa, sessions = se, customFoods = cf)
        }) { a, b ->
            a.copy(measures = b.measures, habits = b.habits, fasts = b.fasts, sessions = b.sessions, customFoods = b.customFoods)
        }.combine(dao.photos()) { d, p -> d.copy(photos = p) }

    val game: StateFlow<GameState?> =
        combine(allData, dao.profile(), settings, dayTicker) { d, p, st, day -> Gamification.compute(d, p, st, day) }
            .flowOn(Dispatchers.Default)
            .state(null)

    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations
    fun dismissCelebration() { _celebrations.value = _celebrations.value.drop(1) }
    private fun celebrate(c: Celebration) { _celebrations.value = _celebrations.value + c }

    private val _restEnd = MutableStateFlow<Long?>(null)
    val restEnd: StateFlow<Long?> = _restEnd
    fun startRest(seconds: Int = settings.value.restSeconds) {
        val end = System.currentTimeMillis() + seconds * 1000L
        _restEnd.value = end
        RestAlarm.schedule(getApplication(), end)
    }
    fun addRest(seconds: Int) {
        val end = (_restEnd.value ?: System.currentTimeMillis()) + seconds * 1000L
        _restEnd.value = end
        RestAlarm.schedule(getApplication(), end)
    }
    fun stopRest() { _restEnd.value = null; RestAlarm.cancel(getApplication()) }
    fun restFinished() { _restEnd.value = null; RestAlarm.cancel(getApplication()); vibrate(600) }

    /** Ekranın altında görünen mesaj; isteğe bağlı "Geri al" eylemiyle. */
    data class UiMessage(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message
    fun clearMessage() { _message.value = null }
    fun toast(m: String) { _message.value = UiMessage(m) }
    private var _msgText: String?
        get() = _message.value?.text
        set(v) { _message.value = v?.let { UiMessage(it) } }

    init {
        // Gün değişince, "bugün"e bakan ekranlar yeni güne geçsin (geçmiş güne bakılıyorsa dokunma)
        viewModelScope.launch {
            var last = LocalDate.now().toEpochDay()
            dayTicker.collect { d ->
                if (d != last) {
                    if (_day.value == last) _day.value = d
                    if (_workoutDay.value == last) _workoutDay.value = d
                    last = d
                }
            }
        }
        viewModelScope.launch {
            game.filterNotNull().collect { g ->
                val st = settings.value
                FitWidget.save(getApplication(), g.streak, kcalLeft(g), g.todayWater, g.waterGoal)

                // Seri dondurma: dün boşsa ve hak varsa seriyi koru
                val yesterday = today - 1
                val effective = g.streakDays
                if (st.streakFreezes > 0 && yesterday !in effective && (today - 2) in effective) {
                    store.update {
                        it.copy(streakFreezes = it.streakFreezes - 1, frozenDays = it.frozenDays + yesterday.toString())
                    }
                    celebrate(Celebration.Custom("🧊", "Serin kurtarıldı!", "Dün kayıt yoktu, bir seri dondurma hakkın kullanıldı."))
                    return@collect
                }
                // Her 7 günlük seride 1 hak (en fazla 2)
                if (g.streak >= 7 && g.streak % 7 == 0 && g.streak > st.lastFreezeAward) {
                    store.update { it.copy(streakFreezes = (it.streakFreezes + 1).coerceAtMost(2), lastFreezeAward = g.streak) }
                    if (st.seenInit) celebrate(Celebration.Custom("🧊", "Seri dondurma kazandın", "${g.streak} günlük seri! Bir gün kaçırırsan serin bozulmayacak."))
                }

                val unlocked = g.achievements.filter { it.unlocked }.map { it.id }.toSet()
                if (!st.seenInit) {
                    store.update { it.copy(seenAchievements = unlocked, lastLevel = g.level, seenInit = true, lastQuestCelebration = today) }
                    return@collect
                }
                val fresh = unlocked - st.seenAchievements
                if (fresh.isNotEmpty()) {
                    g.achievements.filter { it.id in fresh }.forEach { celebrate(Celebration.Badge(it)) }
                    store.update { it.copy(seenAchievements = it.seenAchievements + fresh) }
                }
                if (g.level > st.lastLevel) {
                    celebrate(Celebration.LevelUp(g.level, g.levelTitle))
                    ACCENTS.firstOrNull { it.unlockLevel in (st.lastLevel + 1)..g.level && it.unlockLevel > 1 }?.let {
                        celebrate(Celebration.Custom("🎨", "Yeni tema açıldı", "\"${it.name}\" rengini Ayarlar'dan seçebilirsin."))
                    }
                    store.update { it.copy(lastLevel = g.level) }
                }
                if (g.quests.all { it.done } && st.lastQuestCelebration != today) {
                    celebrate(Celebration.Custom("✅", "Günün tüm görevleri tamam!", "+${g.quests.sumOf { it.xp }} XP kazandın. Yarın görüşürüz."))
                    store.update { it.copy(lastQuestCelebration = today) }
                }
            }
        }
        // Kullanıcının kendi programı: henüz plan kurulmadıysa bir kez otomatik yerleştir
        if (!settings.value.programSeeded) {
            store.update {
                if (it.hasPlan) it.copy(programSeeded = true)
                else it.copy(weekPlan = WEEK_PRESETS.first().days.joinToString(";"), programSeeded = true)
            }
        }
        val st = settings.value
        if (st.remindersOn) Reminders.schedule(app, st.reminderHour)
        Reminders.setMorning(app, st.morningOn)
        Reminders.setWater(app, st.waterRemindOn)
    }

    private fun kcalLeft(g: GameState): Int? = profile.value.profile?.let { Calculator.calculate(it, settings.value).target - g.todayKcal }

    // ---------- Profil ve ayarlar ----------

    fun saveProfile(p: Profile) = viewModelScope.launch { dao.saveProfile(p) }
    fun updateSettings(change: (AppSettings) -> AppSettings) = store.update(change)

    fun setReminders(on: Boolean, hour: Int = settings.value.reminderHour) {
        store.update { it.copy(remindersOn = on, reminderHour = hour) }
        if (on) Reminders.schedule(getApplication(), hour) else Reminders.cancel(getApplication())
    }

    fun setMorning(on: Boolean) { store.update { it.copy(morningOn = on) }; Reminders.setMorning(getApplication(), on) }
    fun setWaterReminder(on: Boolean) { store.update { it.copy(waterRemindOn = on) }; Reminders.setWater(getApplication(), on) }

    fun testNotification() = Reminders.show(getApplication(), "FitTakip 💪", "Bildirimler çalışıyor. Akşam serini hatırlatacağım.")

    // ---------- Yemek ----------

    fun addFood(name: String, kcal: Int, protein: Double, carb: Double, fat: Double, meal: Meal) =
        viewModelScope.launch {
            dao.addFood(FoodEntry(epochDay = _day.value, name = name, kcal = kcal, protein = protein, carb = carb, fat = fat, meal = meal.name))
        }

    fun deleteFood(entry: FoodEntry) = viewModelScope.launch {
        dao.deleteFood(entry)
        _message.value = UiMessage("\"${entry.name}\" silindi", "Geri al") { viewModelScope.launch { dao.insertFood(entry) } }
    }

    // ---------- Kayıtlı öğünler ----------

    fun saveMeal(name: String, items: List<FoodEntry>) = viewModelScope.launch {
        val arr = org.json.JSONArray()
        items.forEach {
            arr.put(org.json.JSONObject().put("n", it.name).put("k", it.kcal).put("p", it.protein).put("c", it.carb).put("f", it.fat))
        }
        dao.addSavedMeal(SavedMeal(name = name, items = arr.toString()))
        _msgText = "\"$name\" kaydedildi. Yemek eklerken tek dokunuşla ekleyebilirsin."
    }

    fun addSavedMeal(m: SavedMeal, meal: Meal) = viewModelScope.launch {
        val arr = org.json.JSONArray(m.items)
        val list = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            FoodEntry(
                epochDay = _day.value, name = o.getString("n"), kcal = o.getInt("k"),
                protein = o.optDouble("p", 0.0), carb = o.optDouble("c", 0.0), fat = o.optDouble("f", 0.0), meal = meal.name
            )
        }
        dao.addFoods(list)
        _msgText = "${m.name}: ${list.size} yemek eklendi (${list.sumOf { it.kcal }} kcal)"
    }

    fun deleteSavedMeal(m: SavedMeal) = viewModelScope.launch { dao.deleteSavedMeal(m) }

    /** Kaydı porsiyon çarpanıyla günceller. */
    fun scaleFood(entry: FoodEntry, factor: Double) = viewModelScope.launch {
        dao.updateFood(
            entry.copy(
                kcal = (entry.kcal * factor).roundToInt(), protein = entry.protein * factor,
                carb = entry.carb * factor, fat = entry.fat * factor
            )
        )
    }

    fun moveFood(entry: FoodEntry, meal: Meal) = viewModelScope.launch { dao.updateFood(entry.copy(meal = meal.name)) }

    fun copyPreviousDay() = viewModelScope.launch {
        val target = _day.value
        val prev = foods.value.filter { it.epochDay == target - 1 }
        if (prev.isEmpty()) {
            _msgText = "Önceki günde kopyalanacak yemek yok."
        } else {
            dao.addFoods(prev.map { it.copy(id = 0, epochDay = target) })
            _msgText = "${prev.size} yemek kopyalandı."
        }
    }

    fun toggleFavorite(name: String) = store.update {
        it.copy(favorites = if (name in it.favorites) it.favorites - name else it.favorites + name)
    }

    fun saveCustomFood(name: String, kcal: Int, p: Double, c: Double, f: Double) = viewModelScope.launch {
        if (customFoods.value.none { it.name.equals(name, true) }) {
            dao.addCustomFood(CustomFood(name = name, portion = "1 porsiyon", kcal = kcal, protein = p, carb = c, fat = f))
        }
    }

    fun deleteCustomFood(f: CustomFood) = viewModelScope.launch { dao.deleteCustomFood(f) }

    // ---------- Su ----------

    fun addWater(ml: Int) = viewModelScope.launch { dao.addWater(WaterLog(epochDay = _day.value, ml = ml)) }
    fun undoWater() = viewModelScope.launch {
        water.value.lastOrNull { it.epochDay == _day.value }?.let { dao.deleteWater(it) }
    }

    // ---------- Antrenman ----------

    fun addSet(
        exercise: String, kg: Double, reps: Int, note: String, warmup: Boolean = false, rpe: Double? = null,
        restOverride: Int? = null, isDrop: Boolean = false
    ) = viewModelScope.launch {
        val name = normalizeExercise(exercise)
        val day = _workoutDay.value
        val bw = profile.value.profile?.weightKg ?: 0.0
        val previous = sets.value.filter { it.counted() && it.exercise.trim().equals(name, true) }
        val oldBest = previous.maxOfOrNull { Stats.e1rm(it, bw) } ?: 0.0
        val hadEarlierDay = previous.any { it.epochDay < day }
        val newBest = Stats.epley1RM(Stats.load(name, kg, bw), reps)
        dao.addSet(WorkoutSet(epochDay = day, exercise = name, weightKg = kg, reps = reps, note = note, warmup = warmup, rpe = rpe, isDrop = isDrop))
        if (day != today) { _msgText = "Set ${dayLabelShort(day)} gününe eklendi."; return@launch }
        if (!warmup && !isDrop && !Stats.isTimed(name) && hadEarlierDay && newBest > oldBest + 1e-6) {
            celebrate(Celebration.PR(name, newBest, oldBest))
            vibrate(250)
        }
        startRest(restOverride ?: if (warmup) 45 else settings.value.restSeconds)
    }

    fun deleteSet(set: WorkoutSet) = viewModelScope.launch {
        dao.deleteSet(set)
        _message.value = UiMessage("${set.exercise} ${trimNum(set.weightKg)}×${set.reps} silindi", "Geri al") { viewModelScope.launch { dao.insertSet(set) } }
    }

    fun updateSet(set: WorkoutSet) = viewModelScope.launch { dao.updateSet(set) }

    private fun dayLabelShort(day: Long) = LocalDate.ofEpochDay(day).let { "${it.dayOfMonth}.${it.monthValue}" }

    fun addCardio(type: String, minutes: Int, kcal: Int) = viewModelScope.launch {
        dao.addCardio(CardioEntry(epochDay = today, type = type, minutes = minutes, kcal = kcal))
    }

    fun deleteCardio(c: CardioEntry) = viewModelScope.launch { dao.deleteCardio(c) }

    fun startSession() = store.update { it.copy(sessionStart = System.currentTimeMillis()) }

    fun finishSession() = viewModelScope.launch {
        val start = settings.value.sessionStart
        if (start == 0L) return@launch
        val end = System.currentTimeMillis()
        store.update { it.copy(sessionStart = 0) }
        val min = ((end - start) / 60_000).toInt()
        if (min < 1) { _msgText = "Oturum 1 dakikadan kısa olduğu için kaydedilmedi."; return@launch }
        dao.addSession(WorkoutSession(epochDay = today, start = start, end = end))
        val todaySets = sets.value.filter { it.epochDay == today && it.counted() }
        val vol = todaySets.sumOf { it.weightKg * it.reps }.roundToInt()
        celebrate(Celebration.Custom("🏁", "Antrenman bitti!", "$min dakika · ${todaySets.size} set · $vol kg hacim"))
    }

    fun cancelSession() = store.update { it.copy(sessionStart = 0) }

    private fun serialize(list: List<WorkoutTemplate>) =
        list.joinToString("\n") { t ->
            t.name + "|" + t.items.joinToString(";") { i ->
                "${i.exercise}:${i.sets}x${i.reps}" + (if (i.repsMax > i.reps) "-${i.repsMax}" else "") + ":s${i.superset}:" + i.note.replace(Regex("[:;|]"), " ")
            }
        }

    fun saveTemplate(name: String, items: List<PlanItem>) = store.update {
        val others = it.templates.filter { t -> !t.name.equals(name, true) }
        it.copy(customTemplates = serialize(others + WorkoutTemplate(name.trim(), items)), activeTemplate = name.trim())
    }

    fun deleteTemplate(name: String) = store.update {
        it.copy(
            customTemplates = serialize(it.templates.filter { t -> t.name != name }),
            activeTemplate = if (it.activeTemplate == name) "" else it.activeTemplate,
            weekPlan = it.plan.joinToString(";") { d -> if (d == name) "" else d }
        )
    }

    // ---------- Haftalık program ----------

    fun setPlanDay(index: Int, value: String) = store.update {
        val p = it.plan.toMutableList()
        p[index] = value
        it.copy(weekPlan = p.joinToString(";"))
    }

    fun applyPreset(days: List<String>) = store.update { it.copy(weekPlan = days.joinToString(";")) }

    /** Programı günlere bağlı ya da sıralı çalıştır. Sıralı moda geçerken sıra bugünden başlar. */
    fun setPlanMode(rotation: Boolean) = store.update {
        it.copy(planMode = if (rotation) "ROTATION" else "WEEKDAY", rotationStart = if (rotation) today else it.rotationStart)
    }

    /** Sıralı modda sırayı baştan başlatır (ilk antrenmandan). */
    fun resetRotation() = store.update { it.copy(rotationStart = today) }
    fun clearPlan() = store.update { it.copy(weekPlan = "") }

    /** Bugünün programını açar ve kronometreyi başlatır. */
    fun startTodayWorkout() {
        val tpl = com.fittakip.app.domain.PlanResolver.templateFor(settings.value, sets.value, today) ?: return
        _workoutDay.value = today
        store.update { it.copy(activeTemplate = tpl.name, sessionStart = if (it.sessionStart == 0L) System.currentTimeMillis() else it.sessionStart) }
    }

    /** Gerçek harcamaya göre kalori hedefini düzeltir. */
    fun applyAdaptiveTdee(realTdee: Int) {
        val p = profile.value.profile ?: return
        val formula = Calculator.calculate(p).tdee
        store.update { it.copy(kcalAdjust = realTdee - formula) }
        _msgText = "Kalori hedefin gerçek harcamana göre güncellendi."
    }

    // ---------- Alışkanlıklar ----------

    fun setHabit(type: String, value: Double, text: String = "") = viewModelScope.launch {
        dao.clearHabit(today, type)
        dao.addHabit(HabitLog(epochDay = today, type = type, value = value, text = text))
    }

    fun addCaffeine(delta: Int) = viewModelScope.launch {
        val current = habits.value.lastOrNull { it.epochDay == today && it.type == "CAFFEINE" }?.value ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        dao.clearHabit(today, "CAFFEINE")
        if (next > 0) dao.addHabit(HabitLog(epochDay = today, type = "CAFFEINE", value = next))
    }

    fun toggleSupplement(name: String) = viewModelScope.launch {
        val has = habits.value.any { it.epochDay == today && it.type == "SUPP" && it.text == name }
        if (has) dao.clearHabitText(today, "SUPP", name)
        else dao.addHabit(HabitLog(epochDay = today, type = "SUPP", value = 1.0, text = name))
    }

    // ---------- Oruç ----------

    fun startFast(goalHours: Int) = viewModelScope.launch {
        if (fasts.value.any { it.end == null }) return@launch
        dao.addFast(FastEntry(start = System.currentTimeMillis(), goalHours = goalHours))
    }

    fun endFast() = viewModelScope.launch {
        val f = fasts.value.firstOrNull { it.end == null } ?: return@launch
        val end = System.currentTimeMillis()
        dao.updateFast(f.copy(end = end))
        val h = (end - f.start) / 3_600_000.0
        if (h >= f.goalHours) celebrate(Celebration.Custom("⏳", "Oruç tamamlandı!", "${"%.1f".format(h)} saat. Hedefin ${f.goalHours} saatti."))
        else _msgText = "Oruç ${"%.1f".format(h)} saatte bitirildi."
    }

    fun deleteFast(f: FastEntry) = viewModelScope.launch { dao.deleteFast(f) }

    // ---------- Vücut ----------

    fun addWeight(kg: Double) = viewModelScope.launch {
        dao.addWeight(BodyWeight(epochDay = today, kg = kg))
        profile.value.profile?.let { dao.saveProfile(it.copy(weightKg = kg)) }
    }

    fun deleteWeight(w: BodyWeight) = viewModelScope.launch {
        dao.deleteWeight(w)
        _message.value = UiMessage("${trimNum(w.kg)} kg tartı silindi", "Geri al") { viewModelScope.launch { dao.addWeight(w) } }
    }
    fun addMeasure(m: BodyMeasure) = viewModelScope.launch { dao.addMeasure(m.copy(epochDay = today)) }
    fun deleteMeasure(m: BodyMeasure) = viewModelScope.launch { dao.deleteMeasure(m) }

    /** Seçilen fotoğrafı küçültüp uygulama klasörüne kopyalar. */
    fun addPhoto(uri: Uri) = viewModelScope.launch {
        try {
            val path = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: error("Fotoğraf açılamadı")
                val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
                val file = File(dir, "p_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                file.absolutePath
            }
            dao.addPhoto(ProgressPhoto(epochDay = today, path = path))
        } catch (e: Exception) {
            _msgText = "Fotoğraf eklenemedi: ${e.message}"
        }
    }

    fun deletePhoto(p: ProgressPhoto) = viewModelScope.launch {
        withContext(Dispatchers.IO) { File(p.path).delete() }
        dao.deletePhoto(p)
    }

    // ---------- Yedekleme ve dışa aktarma ----------

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
    }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        try {
            write(uri, Backup.export(db))
            _msgText = "Yedek kaydedildi."
        } catch (e: Exception) {
            _msgText = "Yedek kaydedilemedi: ${e.message}"
        }
    }

    fun exportCsv(uri: Uri, kind: String) = viewModelScope.launch {
        try {
            val csv = if (kind == "sets") {
                "tarih;hareket;kg;tekrar;isinma;rpe;tahmini_1rm;not\n" + sets.value.sortedBy { it.epochDay }.joinToString("\n") {
                    listOf(
                        LocalDate.ofEpochDay(it.epochDay), it.exercise, it.weightKg, it.reps, if (it.warmup) 1 else 0,
                        it.rpe ?: "", "%.1f".format(Stats.epley1RM(it.weightKg, it.reps)), it.note.replace(";", ",")
                    ).joinToString(";")
                }
            } else {
                "tarih;ogun;yemek;kcal;protein;karb;yag\n" + foods.value.sortedBy { it.epochDay }.joinToString("\n") {
                    listOf(
                        LocalDate.ofEpochDay(it.epochDay), Meal.from(it.meal).label, it.name.replace(";", ","),
                        it.kcal, "%.1f".format(it.protein), "%.1f".format(it.carb), "%.1f".format(it.fat)
                    ).joinToString(";")
                }
            }
            write(uri, "﻿" + csv)
            _msgText = "CSV kaydedildi."
        } catch (e: Exception) {
            _msgText = "CSV kaydedilemedi: ${e.message}"
        }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        try {
            val json = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            } ?: error("Dosya okunamadı.")
            Backup.import(db, json)
            store.update { it.copy(seenInit = false) }
            _msgText = "Yedek geri yüklendi."
        } catch (e: Exception) {
            _msgText = "Geri yüklenemedi: ${e.message}"
        }
    }

    fun resetAll() = viewModelScope.launch {
        photos.value.forEach { withContext(Dispatchers.IO) { File(it.path).delete() } }
        Backup.clearAll(dao)
        store.update { AppSettings(onboarded = true) }
        _msgText = "Tüm veriler silindi."
    }

    // ---------- Paylaşım ----------

    fun workoutShareText(day: Long): String {
        val ds = sets.value.filter { it.epochDay == day && it.counted() }
        val c = cardio.value.filter { it.epochDay == day }
        val sb = StringBuilder("🏋️ FitTakip antrenmanım · ${LocalDate.ofEpochDay(day)}\n")
        ds.groupBy { it.exercise.trim() }.forEach { (n, l) ->
            sb.append("\n$n: ").append(l.reversed().joinToString(", ") { "${trimNum(it.weightKg)}×${it.reps}" })
        }
        c.forEach { sb.append("\n${it.type}: ${it.minutes} dk") }
        sb.append("\n\nToplam: ${ds.size} set · ${ds.sumOf { it.weightKg * it.reps }.roundToInt()} kg hacim")
        game.value?.let { sb.append(" · 🔥 ${it.streak} günlük seri") }
        return sb.toString()
    }

    fun weekShareText(): String {
        val g = game.value ?: return ""
        val w = g.thisWeek
        return "📊 FitTakip haftam\n${w.workoutDays} antrenman günü · ${w.sets} set · ${w.volume.roundToInt()} kg hacim\n" +
            "${w.cardioMin} dk kardiyo · ${w.prs} kişisel rekor\n🔥 ${g.streak} günlük seri · Seviye ${g.level} ${g.levelTitle}"
    }

    fun share(text: String) {
        val ctx = getApplication<Application>()
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        ctx.startActivity(Intent.createChooser(send, "Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun trimNum(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    // ---------- Titreşim ----------

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        val ctx = getApplication<Application>()
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        }
        try {
            v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }
}
