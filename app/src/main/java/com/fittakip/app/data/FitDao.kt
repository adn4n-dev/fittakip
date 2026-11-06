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
    @Query("SELECT * FROM food WHERE epochDay = :day ORDER BY id DESC")
    fun foodsOn(day: Long): Flow<List<FoodEntry>>

    @Insert
    suspend fun addFood(entry: FoodEntry)

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
}
