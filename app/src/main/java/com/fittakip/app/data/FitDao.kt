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

    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Query("DELETE FROM food") suspend fun clearFoods()
    @Query("DELETE FROM workout_set") suspend fun clearSets()
    @Query("DELETE FROM body_weight") suspend fun clearWeights()
    @Query("DELETE FROM water") suspend fun clearWater()
    @Query("DELETE FROM cardio") suspend fun clearCardio()
    @Query("DELETE FROM measure") suspend fun clearMeasures()
}
