package com.fittakip.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tek satırlık kullanıcı profili (id her zaman 1). */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val male: Boolean = true,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: Double,
    val goal: String,            // Goal enum adı: CUT / MAINTAIN / BULK
    val targetWeightKg: Double? = null
)

/** Yemek günlüğü kaydı. Tarihler LocalDate.toEpochDay() olarak tutulur. */
@Entity(tableName = "food")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val name: String,
    val kcal: Int,
    val protein: Double = 0.0,
    val carb: Double = 0.0,
    val fat: Double = 0.0,
    @ColumnInfo(defaultValue = "SNACK") val meal: String = "SNACK"
)

/** Antrenman notu: bir set = hareket + ağırlık + tekrar + serbest not. */
@Entity(tableName = "workout_set")
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exercise: String,
    val weightKg: Double,
    val reps: Int,
    val note: String = ""
)

/** Vücut ağırlığı (tartı) kaydı. */
@Entity(tableName = "body_weight")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kg: Double
)

/** İçilen su (ml). */
@Entity(tableName = "water")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val ml: Int
)

/** Kardiyo oturumu. */
@Entity(tableName = "cardio")
data class CardioEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val type: String,
    val minutes: Int,
    val kcal: Int
)

/** Mezura ölçüleri (cm). Girilmeyenler null. */
@Entity(tableName = "measure")
data class BodyMeasure(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val waist: Double? = null,
    val neck: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val hip: Double? = null
)
