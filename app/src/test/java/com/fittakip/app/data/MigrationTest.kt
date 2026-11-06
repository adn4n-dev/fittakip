package com.fittakip.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * İlk sürümü (v1) kullanan birinin telefonunu taklit eder: v1 şemasıyla veritabanı kurar,
 * içine veri koyar, sonra uygulamanın güncel sürümüyle açar. Room tüm geçişleri çalıştırıp
 * şemayı doğrular; şema uyuşmazsa test patlar. Ardından verilerin korunduğunu kontrol eder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @After
    fun cleanup() {
        ctx.deleteDatabase(name)
    }

    private fun createV1() {
        val file = ctx.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `male` INTEGER NOT NULL, `age` INTEGER NOT NULL, `heightCm` REAL NOT NULL, `weightKg` REAL NOT NULL, `activity` REAL NOT NULL, `goal` TEXT NOT NULL, `targetWeightKg` REAL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `food` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `name` TEXT NOT NULL, `kcal` INTEGER NOT NULL, `protein` REAL NOT NULL, `carb` REAL NOT NULL, `fat` REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_set` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `exercise` TEXT NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `note` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `body_weight` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `kg` REAL NOT NULL)")
        db.execSQL("INSERT INTO profile VALUES (1, 1, 25, 178.0, 82.0, 1.55, 'CUT', 76.0)")
        db.execSQL("INSERT INTO food (epochDay, name, kcal, protein, carb, fat) VALUES (20000, 'Yulaf', 190, 6.5, 33, 3.5)")
        db.execSQL("INSERT INTO workout_set (epochDay, exercise, weightKg, reps, note) VALUES (20000, 'Bench Press', 80, 8, 'iyi')")
        db.execSQL("INSERT INTO workout_set (epochDay, exercise, weightKg, reps, note) VALUES (20001, 'Barfiks', 0, 10, '')")
        db.execSQL("INSERT INTO body_weight (epochDay, kg) VALUES (20000, 82.0)")
        db.version = 1
        db.close()
    }

    @Test
    fun v1_to_latest_keepsData() = runBlocking {
        createV1()
        val room = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        val dao = room.dao()

        val sets = dao.dumpSets()
        assertEquals(2, sets.size)
        assertTrue("Barfiks Pull-up'a çevrilmeli", sets.any { it.exercise == "Pull-up" })
        assertFalse(sets.any { it.warmup || it.isDrop })
        assertEquals("iyi", sets.first { it.exercise == "Bench Press" }.note)

        val foods = dao.dumpFoods()
        assertEquals(1, foods.size)
        assertEquals("SNACK", foods.first().meal)

        assertEquals(82.0, dao.dumpWeights().first().kg, 1e-9)
        assertEquals("CUT", dao.dumpProfile().first().goal)

        // Yeni tablolar çalışıyor mu
        dao.addHabit(HabitLog(epochDay = 20002, type = "SLEEP", value = 7.5))
        dao.addSavedMeal(SavedMeal(name = "Kahvaltı", items = "[]"))
        assertEquals(1, dao.dumpHabits().size)
        assertEquals(1, dao.dumpSavedMeals().size)
        room.close()
    }

    @Test
    fun freshInstall_opens() = runBlocking {
        val room = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        room.dao().addSet(WorkoutSet(epochDay = 1, exercise = "Squat", weightKg = 100.0, reps = 5, isDrop = true))
        assertTrue(room.dao().dumpSets().first().isDrop)
        room.close()
    }
}
