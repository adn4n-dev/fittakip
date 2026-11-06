package com.fittakip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Profile::class, FoodEntry::class, WorkoutSet::class, BodyWeight::class,
        WaterLog::class, CardioEntry::class, BodyMeasure::class,
        HabitLog::class, FastEntry::class, WorkoutSession::class, CustomFood::class, ProgressPhoto::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FitDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 → v2: öğün sütunu + su, kardiyo, ölçü tabloları. Eski veriler korunur. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food` ADD COLUMN `meal` TEXT NOT NULL DEFAULT 'SNACK'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `ml` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cardio` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `type` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `kcal` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `measure` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `waist` REAL, `neck` REAL, `chest` REAL, `arm` REAL, `hip` REAL)"
                )
            }
        }

        /** v2 → v3: ısınma/RPE sütunları + alışkanlık, oruç, oturum, özel yemek, fotoğraf tabloları. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_set` ADD COLUMN `warmup` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `workout_set` ADD COLUMN `rpe` REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `type` TEXT NOT NULL, `value` REAL NOT NULL, `text` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fast` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`start` INTEGER NOT NULL, `end` INTEGER, `goalHours` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `start` INTEGER NOT NULL, `end` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_food` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `portion` TEXT NOT NULL, `kcal` INTEGER NOT NULL, " +
                        "`protein` REAL NOT NULL, `carb` REAL NOT NULL, `fat` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `photo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `path` TEXT NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fittakip.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
