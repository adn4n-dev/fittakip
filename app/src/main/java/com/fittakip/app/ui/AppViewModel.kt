package com.fittakip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittakip.app.data.AppDatabase
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.Profile
import com.fittakip.app.data.WorkoutSet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Profil henüz yüklenmediyse null, yüklendiyse Loaded(profil ya da null). */
data class ProfileState(val loaded: Boolean, val profile: Profile?)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    val today: Long = LocalDate.now().toEpochDay()

    val profile: StateFlow<ProfileState> = dao.profile()
        .map { ProfileState(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState(false, null))

    val todayFoods: StateFlow<List<FoodEntry>> = dao.foodsOn(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sets: StateFlow<List<WorkoutSet>> = dao.allSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weights: StateFlow<List<BodyWeight>> = dao.weights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveProfile(p: Profile) = viewModelScope.launch { dao.saveProfile(p) }

    fun addFood(name: String, kcal: Int, protein: Double, carb: Double, fat: Double) =
        viewModelScope.launch {
            dao.addFood(FoodEntry(epochDay = today, name = name, kcal = kcal, protein = protein, carb = carb, fat = fat))
        }

    fun deleteFood(entry: FoodEntry) = viewModelScope.launch { dao.deleteFood(entry) }

    fun addSet(exercise: String, kg: Double, reps: Int, note: String) = viewModelScope.launch {
        dao.addSet(WorkoutSet(epochDay = today, exercise = exercise, weightKg = kg, reps = reps, note = note))
    }

    fun deleteSet(set: WorkoutSet) = viewModelScope.launch { dao.deleteSet(set) }

    /** Tartı eklenince profildeki kilo da güncellenir, kalori hedefi kendini yeniler. */
    fun addWeight(kg: Double) = viewModelScope.launch {
        dao.addWeight(BodyWeight(epochDay = today, kg = kg))
        profile.value.profile?.let { dao.saveProfile(it.copy(weightKg = kg)) }
    }

    fun deleteWeight(w: BodyWeight) = viewModelScope.launch { dao.deleteWeight(w) }
}
