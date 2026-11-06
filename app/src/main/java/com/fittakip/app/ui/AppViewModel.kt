package com.fittakip.app.ui

import android.app.Application
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
import com.fittakip.app.domain.Gamification
import com.fittakip.app.domain.GameState
import com.fittakip.app.domain.Stats
import com.fittakip.app.notify.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class ProfileState(val loaded: Boolean, val profile: Profile?)

sealed class Celebration {
    data class PR(val exercise: String, val newBest: Double, val oldBest: Double) : Celebration()
    data class Badge(val achievement: Achievement) : Celebration()
    data class LevelUp(val level: Int, val title: String) : Celebration()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    private val store = SettingsStore(app)
    val settings: StateFlow<AppSettings> = store.state
    val today: Long = LocalDate.now().toEpochDay()

    private fun <T> Flow<T>.state(initial: T) = stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val profile: StateFlow<ProfileState> = dao.profile().map { ProfileState(true, it) }.state(ProfileState(false, null))
    val foods = dao.allFoods().state(emptyList())
    val sets = dao.allSets().state(emptyList())
    val weights = dao.weights().state(emptyList())
    val water = dao.water().state(emptyList())
    val cardio = dao.cardio().state(emptyList())
    val measures = dao.measures().state(emptyList())

    // Beslenme ekranında görüntülenen gün
    private val _day = MutableStateFlow(today)
    val selectedDay: StateFlow<Long> = _day
    fun shiftDay(delta: Int) { _day.value = (_day.value + delta).coerceAtMost(today) }

    // Veritabanından doğrudan (ilk değer boş liste değil, gerçek veri)
    private val allData: Flow<AllData> =
        combine(dao.allFoods(), dao.allSets(), dao.weights(), dao.water(), dao.cardio()) { f, s, w, wa, c ->
            AllData(f, s, w, wa, c)
        }.combine(dao.measures()) { d, m -> d.copy(measures = m) }

    val game: StateFlow<GameState?> =
        combine(allData, dao.profile(), settings) { d, p, st -> Gamification.compute(d, p, st, today) }
            .flowOn(Dispatchers.Default)
            .state(null)

    // Kutlama kuyruğu: rekor, rozet, seviye
    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations
    fun dismissCelebration() { _celebrations.value = _celebrations.value.drop(1) }
    private fun celebrate(c: Celebration) { _celebrations.value = _celebrations.value + c }

    // Dinlenme sayacı (bitiş zamanı, ms)
    private val _restEnd = MutableStateFlow<Long?>(null)
    val restEnd: StateFlow<Long?> = _restEnd
    fun startRest(seconds: Int = settings.value.restSeconds) { _restEnd.value = System.currentTimeMillis() + seconds * 1000L }
    fun addRest(seconds: Int) { _restEnd.value = (_restEnd.value ?: System.currentTimeMillis()) + seconds * 1000L }
    fun stopRest() { _restEnd.value = null }
    fun restFinished() { _restEnd.value = null; vibrate(600) }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun clearMessage() { _message.value = null }

    init {
        // Yeni açılan rozet ve seviyeleri yakala
        viewModelScope.launch {
            game.filterNotNull().collect { g ->
                val st = settings.value
                val unlocked = g.achievements.filter { it.unlocked }.map { it.id }.toSet()
                if (!st.seenInit) {
                    store.update { it.copy(seenAchievements = unlocked, lastLevel = g.level, seenInit = true) }
                    return@collect
                }
                val fresh = unlocked - st.seenAchievements
                if (fresh.isNotEmpty()) {
                    g.achievements.filter { it.id in fresh }.forEach { celebrate(Celebration.Badge(it)) }
                    store.update { it.copy(seenAchievements = it.seenAchievements + fresh) }
                }
                if (g.level > st.lastLevel) {
                    celebrate(Celebration.LevelUp(g.level, g.levelTitle))
                    store.update { it.copy(lastLevel = g.level) }
                }
            }
        }
        val st = settings.value
        if (st.remindersOn) Reminders.schedule(app, st.reminderHour)
    }

    // ---------- Profil ve ayarlar ----------

    fun saveProfile(p: Profile) = viewModelScope.launch { dao.saveProfile(p) }
    fun updateSettings(change: (AppSettings) -> AppSettings) = store.update(change)

    fun setReminders(on: Boolean, hour: Int = settings.value.reminderHour) {
        store.update { it.copy(remindersOn = on, reminderHour = hour) }
        if (on) Reminders.schedule(getApplication(), hour) else Reminders.cancel(getApplication())
    }

    fun testNotification() = Reminders.show(getApplication(), "FitTakip 💪", "Bildirimler çalışıyor. Akşam serini hatırlatacağım.")

    // ---------- Yemek ----------

    fun addFood(name: String, kcal: Int, protein: Double, carb: Double, fat: Double, meal: Meal) =
        viewModelScope.launch {
            dao.addFood(FoodEntry(epochDay = _day.value, name = name, kcal = kcal, protein = protein, carb = carb, fat = fat, meal = meal.name))
        }

    fun deleteFood(entry: FoodEntry) = viewModelScope.launch { dao.deleteFood(entry) }

    fun copyPreviousDay() = viewModelScope.launch {
        val target = _day.value
        val prev = foods.value.filter { it.epochDay == target - 1 }
        if (prev.isEmpty()) {
            _message.value = "Önceki günde kopyalanacak yemek yok."
        } else {
            dao.addFoods(prev.map { it.copy(id = 0, epochDay = target) })
            _message.value = "${prev.size} yemek kopyalandı."
        }
    }

    // ---------- Su ----------

    fun addWater(ml: Int) = viewModelScope.launch { dao.addWater(WaterLog(epochDay = _day.value, ml = ml)) }
    fun undoWater() = viewModelScope.launch {
        water.value.lastOrNull { it.epochDay == _day.value }?.let { dao.deleteWater(it) }
    }

    // ---------- Antrenman ----------

    fun addSet(exercise: String, kg: Double, reps: Int, note: String) = viewModelScope.launch {
        val name = exercise.trim()
        val previous = sets.value.filter { it.exercise.trim().equals(name, true) }
        val oldBest = previous.maxOfOrNull { Stats.epley1RM(it.weightKg, it.reps) } ?: 0.0
        val hadEarlierDay = previous.any { it.epochDay < today }
        val newBest = Stats.epley1RM(kg, reps)
        dao.addSet(WorkoutSet(epochDay = today, exercise = name, weightKg = kg, reps = reps, note = note))
        if (hadEarlierDay && newBest > oldBest + 1e-6) {
            celebrate(Celebration.PR(name, newBest, oldBest))
            vibrate(250)
        }
        startRest()
    }

    fun deleteSet(set: WorkoutSet) = viewModelScope.launch { dao.deleteSet(set) }

    fun addCardio(type: String, minutes: Int, kcal: Int) = viewModelScope.launch {
        dao.addCardio(CardioEntry(epochDay = today, type = type, minutes = minutes, kcal = kcal))
    }

    fun deleteCardio(c: CardioEntry) = viewModelScope.launch { dao.deleteCardio(c) }

    // ---------- Vücut ----------

    /** Tartı eklenince profildeki kilo da güncellenir, kalori hedefi kendini yeniler. */
    fun addWeight(kg: Double) = viewModelScope.launch {
        dao.addWeight(BodyWeight(epochDay = today, kg = kg))
        profile.value.profile?.let { dao.saveProfile(it.copy(weightKg = kg)) }
    }

    fun deleteWeight(w: BodyWeight) = viewModelScope.launch { dao.deleteWeight(w) }

    fun addMeasure(m: BodyMeasure) = viewModelScope.launch { dao.addMeasure(m.copy(epochDay = today)) }
    fun deleteMeasure(m: BodyMeasure) = viewModelScope.launch { dao.deleteMeasure(m) }

    // ---------- Yedekleme ----------

    fun exportTo(uri: Uri) = viewModelScope.launch {
        try {
            val json = Backup.export(db)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            _message.value = "Yedek kaydedildi."
        } catch (e: Exception) {
            _message.value = "Yedek kaydedilemedi: ${e.message}"
        }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        try {
            val json = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            } ?: error("Dosya okunamadı.")
            Backup.import(db, json)
            store.update { it.copy(seenInit = false) }
            _message.value = "Yedek geri yüklendi."
        } catch (e: Exception) {
            _message.value = "Geri yüklenemedi: ${e.message}"
        }
    }

    fun resetAll() = viewModelScope.launch {
        Backup.clearAll(dao)
        store.update { AppSettings(onboarded = true) }
        _message.value = "Tüm veriler silindi."
    }

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
