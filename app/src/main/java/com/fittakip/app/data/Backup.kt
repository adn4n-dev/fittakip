package com.fittakip.app.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Tüm verileri tek bir JSON dosyasına yazar / geri yükler. */
object Backup {

    suspend fun export(db: AppDatabase): String {
        val d = db.dao()
        val root = JSONObject()
        root.put("app", "FitTakip")
        root.put("version", 3)
        root.put("profile", JSONArray().apply {
            d.dumpProfile().forEach {
                put(JSONObject().put("male", it.male).put("age", it.age).put("heightCm", it.heightCm)
                    .put("weightKg", it.weightKg).put("activity", it.activity).put("goal", it.goal)
                    .put("targetWeightKg", it.targetWeightKg ?: JSONObject.NULL))
            }
        })
        root.put("food", JSONArray().apply {
            d.dumpFoods().forEach {
                put(JSONObject().put("day", it.epochDay).put("name", it.name).put("kcal", it.kcal)
                    .put("p", it.protein).put("c", it.carb).put("f", it.fat).put("meal", it.meal))
            }
        })
        root.put("sets", JSONArray().apply {
            d.dumpSets().forEach {
                put(JSONObject().put("day", it.epochDay).put("ex", it.exercise).put("kg", it.weightKg)
                    .put("reps", it.reps).put("note", it.note).put("warmup", it.warmup).put("rpe", it.rpe ?: JSONObject.NULL))
            }
        })
        root.put("weights", JSONArray().apply {
            d.dumpWeights().forEach { put(JSONObject().put("day", it.epochDay).put("kg", it.kg)) }
        })
        root.put("water", JSONArray().apply {
            d.dumpWater().forEach { put(JSONObject().put("day", it.epochDay).put("ml", it.ml)) }
        })
        root.put("cardio", JSONArray().apply {
            d.dumpCardio().forEach {
                put(JSONObject().put("day", it.epochDay).put("type", it.type).put("min", it.minutes).put("kcal", it.kcal))
            }
        })
        root.put("measures", JSONArray().apply {
            d.dumpMeasures().forEach {
                put(JSONObject().put("day", it.epochDay).put("waist", it.waist ?: JSONObject.NULL)
                    .put("neck", it.neck ?: JSONObject.NULL).put("chest", it.chest ?: JSONObject.NULL)
                    .put("arm", it.arm ?: JSONObject.NULL).put("hip", it.hip ?: JSONObject.NULL))
            }
        })
        root.put("habits", JSONArray().apply {
            d.dumpHabits().forEach { put(JSONObject().put("day", it.epochDay).put("type", it.type).put("v", it.value).put("t", it.text)) }
        })
        root.put("fasts", JSONArray().apply {
            d.dumpFasts().forEach { put(JSONObject().put("start", it.start).put("end", it.end ?: JSONObject.NULL).put("goal", it.goalHours)) }
        })
        root.put("sessions", JSONArray().apply {
            d.dumpSessions().forEach { put(JSONObject().put("day", it.epochDay).put("start", it.start).put("end", it.end)) }
        })
        root.put("customFoods", JSONArray().apply {
            d.dumpCustomFoods().forEach {
                put(JSONObject().put("name", it.name).put("portion", it.portion).put("kcal", it.kcal)
                    .put("p", it.protein).put("c", it.carb).put("f", it.fat))
            }
        })
        root.put("savedMeals", JSONArray().apply {
            d.dumpSavedMeals().forEach { put(JSONObject().put("name", it.name).put("items", it.items)) }
        })
        return root.toString(2)
    }

    private fun JSONObject.optD(key: String): Double? = if (isNull(key) || !has(key)) null else getDouble(key)

    private inline fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { f(getJSONObject(it)) }
    }

    /** Mevcut verilerin yerine yedeği koyar. Hatalı dosyada hiçbir şeyi silmez. */
    suspend fun import(db: AppDatabase, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "FitTakip") { "Bu dosya bir FitTakip yedeği değil." }
        val d = db.dao()

        val profiles = root.optJSONArray("profile").mapObjects {
            Profile(
                male = it.getBoolean("male"), age = it.getInt("age"), heightCm = it.getDouble("heightCm"),
                weightKg = it.getDouble("weightKg"), activity = it.getDouble("activity"),
                goal = it.getString("goal"), targetWeightKg = it.optD("targetWeightKg")
            )
        }
        val foods = root.optJSONArray("food").mapObjects {
            FoodEntry(epochDay = it.getLong("day"), name = it.getString("name"), kcal = it.getInt("kcal"),
                protein = it.optDouble("p", 0.0), carb = it.optDouble("c", 0.0), fat = it.optDouble("f", 0.0),
                meal = it.optString("meal", "SNACK"))
        }
        val sets = root.optJSONArray("sets").mapObjects {
            WorkoutSet(epochDay = it.getLong("day"), exercise = normalizeExercise(it.getString("ex")), weightKg = it.getDouble("kg"),
                reps = it.getInt("reps"), note = it.optString("note", ""),
                warmup = it.optBoolean("warmup", false), rpe = it.optD("rpe"))
        }
        val weights = root.optJSONArray("weights").mapObjects { BodyWeight(epochDay = it.getLong("day"), kg = it.getDouble("kg")) }
        val water = root.optJSONArray("water").mapObjects { WaterLog(epochDay = it.getLong("day"), ml = it.getInt("ml")) }
        val cardio = root.optJSONArray("cardio").mapObjects {
            CardioEntry(epochDay = it.getLong("day"), type = it.getString("type"), minutes = it.getInt("min"), kcal = it.getInt("kcal"))
        }
        val measures = root.optJSONArray("measures").mapObjects {
            BodyMeasure(epochDay = it.getLong("day"), waist = it.optD("waist"), neck = it.optD("neck"),
                chest = it.optD("chest"), arm = it.optD("arm"), hip = it.optD("hip"))
        }

        val habits = root.optJSONArray("habits").mapObjects {
            HabitLog(epochDay = it.getLong("day"), type = it.getString("type"), value = it.optDouble("v", 0.0), text = it.optString("t", ""))
        }
        val fasts = root.optJSONArray("fasts").mapObjects {
            FastEntry(start = it.getLong("start"), end = if (it.isNull("end")) null else it.getLong("end"), goalHours = it.getInt("goal"))
        }
        val sessions = root.optJSONArray("sessions").mapObjects {
            WorkoutSession(epochDay = it.getLong("day"), start = it.getLong("start"), end = it.getLong("end"))
        }
        val customFoods = root.optJSONArray("customFoods").mapObjects {
            CustomFood(name = it.getString("name"), portion = it.optString("portion", "1 porsiyon"), kcal = it.getInt("kcal"),
                protein = it.optDouble("p", 0.0), carb = it.optDouble("c", 0.0), fat = it.optDouble("f", 0.0))
        }

        val savedMeals = root.optJSONArray("savedMeals").mapObjects { SavedMeal(name = it.getString("name"), items = it.getString("items")) }

        db.withTransaction {
            clearAll(d)
            profiles.firstOrNull()?.let { d.saveProfile(it) }
            d.addFoods(foods)
            d.insertSets(sets)
            d.insertWeights(weights)
            d.insertWater(water)
            d.insertCardio(cardio)
            d.insertMeasures(measures)
            d.insertHabits(habits)
            d.insertFasts(fasts)
            d.insertSessions(sessions)
            d.insertCustomFoods(customFoods)
            d.insertSavedMeals(savedMeals)
        }
    }

    suspend fun clearAll(d: FitDao) {
        d.clearProfile(); d.clearFoods(); d.clearSets(); d.clearWeights()
        d.clearWater(); d.clearCardio(); d.clearMeasures()
        d.clearHabits(); d.clearFasts(); d.clearSessions(); d.clearCustomFoods(); d.clearPhotos(); d.clearSavedMeals()
    }
}
