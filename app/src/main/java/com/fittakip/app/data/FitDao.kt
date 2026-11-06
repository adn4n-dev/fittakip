package com.fittakip.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FitDao {

    // Profil
    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<Profile?>

    @Upsert
    suspend fun saveProfile(profile: Profile)

    // Yemek
    @Query("SELECT * FROM food ORDER BY epochDay DESC, id DESC")
    fun allFoods(): Flow<List<FoodEntry>>

    @Insert
    suspend fun addFood(entry: FoodEntry)

    @Insert
    suspend fun addFoods(entries: List<FoodEntry>)

    @Delete
    suspend fun deleteFood(entry: FoodEntry)

    // Antrenman
    @Query("SELECT * FROM workout_set ORDER BY epochDay DESC, id DESC")
    fun allSets(): Flow<List<WorkoutSet>>

    @Insert
    suspend fun addSet(set: WorkoutSet)

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    // Tartı
    @Query("SELECT * FROM body_weight ORDER BY epochDay ASC, id ASC")
    fun weights(): Flow<List<BodyWeight>>

    @Insert
    suspend fun addWeight(weight: BodyWeight)

    @Delete
    suspend fun deleteWeight(weight: BodyWeight)

    // Su
    @Query("SELECT * FROM water ORDER BY epochDay ASC, id ASC")
    fun water(): Flow<List<WaterLog>>

    @Insert
    suspend fun addWater(w: WaterLog)

    @Delete
    suspend fun deleteWater(w: WaterLog)

    // Kardiyo
    @Query("SELECT * FROM cardio ORDER BY epochDay DESC, id DESC")
    fun cardio(): Flow<List<CardioEntry>>

    @Insert
    suspend fun addCardio(c: CardioEntry)

    @Delete
    suspend fun deleteCardio(c: CardioEntry)

    // Ölçüler
    @Query("SELECT * FROM measure ORDER BY epochDay ASC, id ASC")
    fun measures(): Flow<List<BodyMeasure>>

    @Insert
    suspend fun addMeasure(m: BodyMeasure)

    @Delete
    suspend fun deleteMeasure(m: BodyMeasure)

    @androidx.room.Update
    suspend fun updateFood(entry: FoodEntry)

    // Alışkanlıklar
    @Query("SELECT * FROM habit ORDER BY epochDay ASC, id ASC")
    fun habits(): Flow<List<HabitLog>>

    @Insert
    suspend fun addHabit(h: HabitLog)

    @Query("DELETE FROM habit WHERE epochDay = :day AND type = :type")
    suspend fun clearHabit(day: Long, type: String)

    @Query("DELETE FROM habit WHERE epochDay = :day AND type = :type AND text = :text")
    suspend fun clearHabitText(day: Long, type: String, text: String)

    // Oruç
    @Query("SELECT * FROM fast ORDER BY start DESC")
    fun fasts(): Flow<List<FastEntry>>

    @Insert
    suspend fun addFast(f: FastEntry)

    @androidx.room.Update
    suspend fun updateFast(f: FastEntry)

    @Delete
    suspend fun deleteFast(f: FastEntry)

    // Oturumlar
    @Query("SELECT * FROM session ORDER BY start DESC")
    fun sessions(): Flow<List<WorkoutSession>>

    @Insert
    suspend fun addSession(s: WorkoutSession)

    // Kendi yemeklerim
    @Query("SELECT * FROM custom_food ORDER BY name")
    fun customFoods(): Flow<List<CustomFood>>

    @Insert
    suspend fun addCustomFood(f: CustomFood)

    @Delete
    suspend fun deleteCustomFood(f: CustomFood)

    // Fotoğraflar
    @Query("SELECT * FROM photo ORDER BY epochDay ASC, id ASC")
    fun photos(): Flow<List<ProgressPhoto>>

    @Insert
    suspend fun addPhoto(p: ProgressPhoto)

    @Delete
    suspend fun deletePhoto(p: ProgressPhoto)

    // Widget için
    @Query("SELECT COALESCE(SUM(kcal), 0) FROM food WHERE epochDay = :day")
    suspend fun kcalOn(day: Long): Int

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water WHERE epochDay = :day")
    suspend fun waterOn(day: Long): Int

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileNow(): Profile?

    // Bildirim için: kayıt girilen günler
    @Query(
        "SELECT epochDay FROM workout_set UNION SELECT epochDay FROM food UNION " +
            "SELECT epochDay FROM body_weight UNION SELECT epochDay FROM water UNION SELECT epochDay FROM cardio"
    )
    suspend fun activeDays(): List<Long>

    // Yedekleme
    @Query("SELECT * FROM profile") suspend fun dumpProfile(): List<Profile>
    @Query("SELECT * FROM food") suspend fun dumpFoods(): List<FoodEntry>
    @Query("SELECT * FROM workout_set") suspend fun dumpSets(): List<WorkoutSet>
    @Query("SELECT * FROM body_weight") suspend fun dumpWeights(): List<BodyWeight>
    @Query("SELECT * FROM water") suspend fun dumpWater(): List<WaterLog>
    @Query("SELECT * FROM cardio") suspend fun dumpCardio(): List<CardioEntry>
    @Query("SELECT * FROM measure") suspend fun dumpMeasures(): List<BodyMeasure>

    @Insert suspend fun insertSets(list: List<WorkoutSet>)
    @Insert suspend fun insertWeights(list: List<BodyWeight>)
    @Insert suspend fun insertWater(list: List<WaterLog>)
    @Insert suspend fun insertCardio(list: List<CardioEntry>)
    @Insert suspend fun insertMeasures(list: List<BodyMeasure>)

    @Query("SELECT * FROM habit") suspend fun dumpHabits(): List<HabitLog>
    @Query("SELECT * FROM fast") suspend fun dumpFasts(): List<FastEntry>
    @Query("SELECT * FROM session") suspend fun dumpSessions(): List<WorkoutSession>
    @Query("SELECT * FROM custom_food") suspend fun dumpCustomFoods(): List<CustomFood>
    @Insert suspend fun insertHabits(list: List<HabitLog>)
    @Insert suspend fun insertFasts(list: List<FastEntry>)
    @Insert suspend fun insertSessions(list: List<WorkoutSession>)
    @Insert suspend fun insertCustomFoods(list: List<CustomFood>)
    @Query("DELETE FROM habit") suspend fun clearHabits()
    @Query("DELETE FROM fast") suspend fun clearFasts()
    @Query("DELETE FROM session") suspend fun clearSessions()
    @Query("DELETE FROM custom_food") suspend fun clearCustomFoods()
    @Query("DELETE FROM photo") suspend fun clearPhotos()

    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Query("DELETE FROM food") suspend fun clearFoods()
    @Query("DELETE FROM workout_set") suspend fun clearSets()
    @Query("DELETE FROM body_weight") suspend fun clearWeights()
    @Query("DELETE FROM water") suspend fun clearWater()
    @Query("DELETE FROM cardio") suspend fun clearCardio()
    @Query("DELETE FROM measure") suspend fun clearMeasures()
}
