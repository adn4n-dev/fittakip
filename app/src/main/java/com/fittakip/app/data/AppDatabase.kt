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
        WaterLog::class, CardioEntry::class, BodyMeasure::class
    ],
    version = 2,
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

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fittakip.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
