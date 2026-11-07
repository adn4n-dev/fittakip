package com.fittakip.app.data

import java.util.Locale

private val TR = Locale.forLanguageTag("tr")
fun String.trLower(): String = lowercase(TR)

// ---------------- Yemek veritabanı ----------------

/** Porsiyon başına değerler (yaklaşık). */
data class FoodItem(val name: String, val portion: String, val kcal: Int, val p: Double, val c: Double, val f: Double)

val FOOD_DB = listOf(
    // Kahvaltılık
    FoodItem("Haşlanmış yumurta", "1 adet", 78, 6.3, 0.6, 5.3),
    FoodItem("Omlet", "2 yumurta", 190, 13.0, 1.0, 15.0),
    FoodItem("Menemen", "1 porsiyon", 250, 12.0, 10.0, 18.0),
    FoodItem("Beyaz peynir", "30 g", 80, 5.0, 0.5, 6.5),
    FoodItem("Kaşar peyniri", "30 g", 110, 7.5, 0.5, 8.5),
    FoodItem("Lor peyniri", "100 g", 100, 13.0, 3.0, 4.0),
    FoodItem("Siyah zeytin", "5 adet", 45, 0.3, 1.0, 4.5),
    FoodItem("Domates", "1 orta", 22, 1.0, 4.8, 0.2),
    FoodItem("Salatalık", "1 orta", 30, 1.3, 6.0, 0.2),
    FoodItem("Beyaz ekmek", "1 dilim", 66, 2.2, 12.5, 0.8),
    FoodItem("Tam buğday ekmeği", "1 dilim", 62, 3.0, 11.0, 1.0),
    FoodItem("Simit", "1 adet", 300, 9.0, 52.0, 7.0),
    FoodItem("Peynirli poğaça", "1 adet", 250, 6.0, 28.0, 13.0),
    FoodItem("Bal", "1 tatlı kaşığı", 30, 0.0, 8.0, 0.0),
    FoodItem("Fıstık ezmesi", "1 yemek kaşığı", 95, 4.0, 3.0, 8.0),
    FoodItem("Yulaf ezmesi", "50 g", 190, 6.5, 33.0, 3.5),
    FoodItem("Süt (yarım yağlı)", "1 su bardağı", 92, 6.6, 9.6, 3.0),
    FoodItem("Yoğurt", "1 kase (200 g)", 122, 7.0, 9.0, 6.5),
    FoodItem("Süzme yoğurt", "150 g", 145, 15.0, 6.0, 7.0),
    FoodItem("Ayran", "1 bardak", 76, 4.0, 6.0, 3.6),
    FoodItem("Kefir", "1 bardak", 120, 7.0, 9.0, 6.0),
    FoodItem("Avokado", "yarım", 160, 2.0, 8.5, 14.7),
    // Meyve, kuruyemiş
    FoodItem("Muz", "1 orta", 105, 1.3, 27.0, 0.4),
    FoodItem("Elma", "1 orta", 95, 0.5, 25.0, 0.3),
    FoodItem("Portakal", "1 orta", 62, 1.2, 15.0, 0.2),
    FoodItem("Çilek", "1 kase", 48, 1.0, 11.5, 0.5),
    FoodItem("Üzüm", "1 kase", 104, 1.1, 27.0, 0.2),
    FoodItem("Karpuz", "1 dilim", 85, 1.7, 21.0, 0.4),
    FoodItem("Ceviz", "30 g", 196, 4.6, 4.0, 19.5),
    FoodItem("Badem", "30 g", 173, 6.4, 6.5, 15.0),
    FoodItem("Fındık", "30 g", 188, 4.5, 5.0, 18.0),
    FoodItem("Kuru kayısı", "5 adet", 85, 1.0, 20.0, 0.2),
    FoodItem("Hurma", "3 adet", 200, 1.8, 54.0, 0.1),
    // Et, tavuk, balık
    FoodItem("Tavuk göğsü (ızgara)", "150 g", 248, 46.0, 0.0, 5.4),
    FoodItem("Tavuk but (fırın)", "150 g", 300, 38.0, 0.0, 16.0),
    FoodItem("Izgara köfte", "4 adet", 290, 22.0, 4.0, 21.0),
    FoodItem("Biftek", "150 g", 330, 40.0, 0.0, 18.0),
    FoodItem("Somon (fırın)", "150 g", 310, 33.0, 0.0, 19.0),
    FoodItem("Ton balığı (suda)", "1 kutu", 130, 29.0, 0.0, 1.0),
    FoodItem("Hamsi tava", "1 porsiyon", 350, 22.0, 10.0, 25.0),
    FoodItem("Hindi füme", "50 g", 55, 9.0, 1.0, 1.5),
    // Ana yemekler
    FoodItem("Pirinç pilavı", "1 porsiyon", 230, 4.0, 42.0, 5.0),
    FoodItem("Bulgur pilavı", "1 porsiyon", 190, 5.0, 35.0, 4.0),
    FoodItem("Makarna", "1 porsiyon", 220, 8.0, 43.0, 1.3),
    FoodItem("Kuru fasulye", "1 porsiyon", 300, 15.0, 40.0, 9.0),
    FoodItem("Nohut yemeği", "1 porsiyon", 290, 13.0, 38.0, 9.0),
    FoodItem("Karnıyarık", "1 porsiyon", 350, 12.0, 18.0, 26.0),
    FoodItem("İmam bayıldı", "1 porsiyon", 280, 3.0, 18.0, 22.0),
    FoodItem("Zeytinyağlı taze fasulye", "1 porsiyon", 160, 4.0, 15.0, 10.0),
    FoodItem("Ispanak yemeği", "1 porsiyon", 140, 7.0, 10.0, 8.0),
    FoodItem("Mantı", "1 porsiyon", 520, 22.0, 60.0, 21.0),
    FoodItem("Haşlanmış patates", "1 orta", 160, 4.0, 37.0, 0.2),
    FoodItem("Çoban salata", "1 porsiyon", 90, 2.0, 8.0, 6.0),
    // Çorbalar
    FoodItem("Mercimek çorbası", "1 kase", 150, 8.0, 22.0, 4.0),
    FoodItem("Ezogelin çorbası", "1 kase", 150, 6.0, 22.0, 4.5),
    FoodItem("Yayla çorbası", "1 kase", 140, 6.0, 16.0, 6.0),
    FoodItem("Tavuk çorbası", "1 kase", 110, 8.0, 12.0, 3.0),
    // Dışarıda
    FoodItem("Lahmacun", "1 adet", 270, 12.0, 35.0, 9.0),
    FoodItem("Kıymalı pide", "1 porsiyon", 650, 30.0, 75.0, 25.0),
    FoodItem("Tavuk döner dürüm", "1 adet", 550, 32.0, 55.0, 22.0),
    FoodItem("Et döner porsiyon", "1 porsiyon", 600, 35.0, 25.0, 40.0),
    FoodItem("İskender", "1 porsiyon", 850, 40.0, 55.0, 50.0),
    FoodItem("Adana kebap", "1 porsiyon", 650, 35.0, 30.0, 42.0),
    FoodItem("Çiğ köfte dürüm", "1 adet", 330, 9.0, 62.0, 5.0),
    FoodItem("Mercimek köftesi", "4 adet", 220, 9.0, 38.0, 4.0),
    FoodItem("Sigara böreği", "3 adet", 300, 9.0, 24.0, 19.0),
    FoodItem("Su böreği", "1 dilim", 320, 12.0, 30.0, 17.0),
    FoodItem("Peynirli gözleme", "1 adet", 400, 15.0, 45.0, 17.0),
    FoodItem("Hamburger", "1 adet", 550, 28.0, 45.0, 28.0),
    FoodItem("Pizza", "2 dilim", 570, 24.0, 66.0, 22.0),
    FoodItem("Patates kızartması", "orta boy", 365, 4.0, 48.0, 17.0),
    // Tatlı ve içecek
    FoodItem("Baklava", "2 dilim", 330, 5.0, 37.0, 19.0),
    FoodItem("Sütlaç", "1 kase", 250, 7.0, 42.0, 6.0),
    FoodItem("Künefe", "1 porsiyon", 550, 12.0, 60.0, 30.0),
    FoodItem("Dondurma", "2 top", 270, 4.5, 32.0, 14.0),
    FoodItem("Sütlü çikolata", "30 g", 160, 2.3, 17.0, 9.0),
    FoodItem("Türk kahvesi (şekersiz)", "1 fincan", 5, 0.3, 0.6, 0.2),
    FoodItem("Çay (1 şekerli)", "1 bardak", 20, 0.0, 5.0, 0.0),
    FoodItem("Kola", "330 ml", 140, 0.0, 35.0, 0.0),
    FoodItem("Portakal suyu", "1 bardak", 90, 1.4, 21.0, 0.4),
    // Sporcu
    FoodItem("Whey protein", "1 ölçek", 120, 24.0, 3.0, 1.5),
    FoodItem("Pirinç patlağı", "1 adet", 35, 0.7, 7.3, 0.3),
    FoodItem("Protein bar", "1 adet", 200, 20.0, 20.0, 7.0)
)

enum class Meal(val label: String, val emoji: String) {
    BREAKFAST("Kahvaltı", "🍳"),
    LUNCH("Öğle", "🥗"),
    DINNER("Akşam", "🍽️"),
    SNACK("Ara öğün", "🍎");

    companion object {
        fun from(name: String) = entries.firstOrNull { it.name == name } ?: SNACK
        fun forHour(h: Int) = when (h) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}

// ---------------- Hareket kütüphanesi ----------------

enum class Equipment(val label: String, val emoji: String, val step: Double) {
    MACHINE("Makine", "⚙️", 5.0),
    CABLE("Kablo", "🔗", 2.5),
    DUMBBELL("Dambıl", "🏋️", 2.0),
    BARBELL("Halter", "🏋️‍♂️", 2.5),
    BODYWEIGHT("Vücut ağırlığı", "🤸", 2.5)
}

data class ExerciseDef(val name: String, val muscle: String, val equipment: Equipment)

val MUSCLES = listOf("Göğüs", "Sırt", "Bacak", "Omuz", "Kol", "Karın")

private fun e(name: String, muscle: String, eq: Equipment) = ExerciseDef(name, muscle, eq)
private val M = Equipment.MACHINE
private val C = Equipment.CABLE
private val D = Equipment.DUMBBELL
private val B = Equipment.BARBELL
private val W = Equipment.BODYWEIGHT

val EXERCISES = listOf(
    // Göğüs
    e("Chest Press", "Göğüs", M), e("Incline Chest Press", "Göğüs", M), e("Decline Chest Press", "Göğüs", M),
    e("Pec Deck", "Göğüs", M), e("Smith Machine Bench Press", "Göğüs", M), e("Smith Machine Incline Press", "Göğüs", M),
    e("Cable Fly", "Göğüs", C), e("Low-to-High Cable Fly", "Göğüs", C), e("High-to-Low Cable Fly", "Göğüs", C),
    e("Bench Press", "Göğüs", B), e("Incline Bench Press", "Göğüs", B),
    e("Dumbbell Press", "Göğüs", D), e("Incline Dumbbell Press", "Göğüs", D), e("Dumbbell Fly", "Göğüs", D),
    e("Dips", "Göğüs", W), e("Push-up", "Göğüs", W),
    // Sırt
    e("Lat Pulldown", "Sırt", M), e("Seated Row", "Sırt", M), e("Chest Supported Row", "Sırt", M), e("T-Bar Row", "Sırt", M),
    e("Assisted Pull-up", "Sırt", M), e("Pullover Machine", "Sırt", M), e("Back Extension", "Sırt", M), e("High Row Machine", "Sırt", M),
    e("Cable Row", "Sırt", C), e("Seated Cable Row", "Sırt", C), e("Straight Arm Pulldown", "Sırt", C), e("Face Pull", "Sırt", C),
    e("Single Arm Cable Row", "Sırt", C),
    e("Deadlift", "Sırt", B), e("Barbell Row", "Sırt", B), e("Dumbbell Row", "Sırt", D), e("Pull-up", "Sırt", W),
    // Bacak
    e("Leg Press", "Bacak", M), e("Hack Squat", "Bacak", M), e("Pendulum Squat", "Bacak", M), e("Smith Machine Squat", "Bacak", M),
    e("Leg Extension", "Bacak", M), e("Leg Curl", "Bacak", M), e("Seated Leg Curl", "Bacak", M), e("Lying Leg Curl", "Bacak", M),
    e("Hip Abductor", "Bacak", M), e("Hip Adductor", "Bacak", M), e("Glute Kickback Machine", "Bacak", M),
    e("Calf Raise", "Bacak", M), e("Seated Calf Raise", "Bacak", M), e("Hip Thrust Machine", "Bacak", M),
    e("Cable Kickback", "Bacak", C), e("Cable Pull Through", "Bacak", C),
    e("Squat", "Bacak", B), e("Front Squat", "Bacak", B), e("Romanian Deadlift", "Bacak", B), e("Hip Thrust", "Bacak", B),
    e("Lunge", "Bacak", D), e("Bulgarian Split Squat", "Bacak", D),
    // Omuz
    e("Shoulder Press", "Omuz", M), e("Lateral Raise Machine", "Omuz", M), e("Rear Delt Fly", "Omuz", M),
    e("Cable Lateral Raise", "Omuz", C), e("Cable Rear Delt Fly", "Omuz", C), e("Cable Upright Row", "Omuz", C),
    e("Lateral Raise", "Omuz", D), e("Dumbbell Shoulder Press", "Omuz", D), e("Arnold Press", "Omuz", D),
    e("Overhead Press", "Omuz", B),
    // Kol
    e("Biceps Machine", "Kol", M), e("Preacher Curl Machine", "Kol", M), e("Triceps Extension Machine", "Kol", M), e("Triceps Dip Machine", "Kol", M),
    e("Biceps Cable Curl", "Kol", C), e("Cable Hammer Curl", "Kol", C), e("Triceps Pushdown", "Kol", C), e("Overhead Triceps Extension", "Kol", C),
    e("Single Arm Triceps Pushdown", "Kol", C),
    e("Dumbbell Curl", "Kol", D), e("Hammer Curl", "Kol", D), e("Incline Dumbbell Curl", "Kol", D),
    e("Barbell Curl", "Kol", B), e("Skull Crusher", "Kol", B),
    // Karın
    e("Karın Makinesi", "Karın", M), e("Rotary Torso", "Karın", M), e("Cable Crunch", "Karın", C),
    e("Plank", "Karın", W), e("Crunch", "Karın", W), e("Hanging Leg Raise", "Karın", W)
)

fun exerciseDef(name: String): ExerciseDef? = EXERCISES.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

/** Ekipmana göre ağırlık artış adımı (makine 5 kg, kablo 2,5 kg, dambıl 2 kg). */
fun weightStep(exercise: String): Double = exerciseDef(exercise)?.equipment?.step ?: 2.5

fun muscleOf(exercise: String): String =
    EXERCISES.firstOrNull { it.name.equals(exercise.trim(), ignoreCase = true) }?.muscle ?: "Diğer"

/**
 * Programdaki bir hareket. reps..repsMax tekrar aralığı (ör. 8–12),
 * superset > 0 ise aynı numaralı hareketler süperset olarak arka arkaya yapılır.
 */
data class PlanItem(
    val exercise: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val repsMax: Int = reps,
    val superset: Int = 0,
    val note: String = ""
) {
    val repsLabel get() = if (repsMax > reps) "$reps–$repsMax" else "$reps"
    val label get() = "$sets×$repsLabel"
}

data class WorkoutTemplate(val name: String, val items: List<PlanItem>) {
    val exercises: List<String> get() = items.map { it.exercise }
}

private fun t(name: String, vararg items: Triple<String, Int, Int>) =
    WorkoutTemplate(name, items.map { PlanItem(it.first, it.second, it.third) })

private fun p(ex: String, sets: Int, reps: Int, max: Int = reps, ss: Int = 0, note: String = "") = PlanItem(ex, sets, reps, max, ss, note)

/** Kullanıcının kendi programı: Upper A / Lower / Upper B. */
val USER_PROGRAM = listOf(
    WorkoutTemplate("Upper A", listOf(
        p("Lat Pulldown", 4, 8, 12, note = "geniş tutuş"),
        p("Chest Press", 3, 6, 10),
        p("Seated Row", 3, 10, ss = 1),
        p("Shoulder Press", 3, 8, 10, ss = 1),
        p("Lateral Raise", 4, 12, 15, note = "son set drop"),
        p("Triceps Pushdown", 3, 12, ss = 2),
        p("Biceps Cable Curl", 3, 12, ss = 2),
        p("Leg Curl", 3, 10, 12)
    )),
    WorkoutTemplate("Lower", listOf(
        p("Hack Squat", 4, 6, 10),
        p("Romanian Deadlift", 3, 8, 10),
        p("Leg Press", 3, 10, 12),
        p("Leg Extension", 3, 15, ss = 1),
        p("Calf Raise", 4, 12, 15, ss = 1),
        p("Lateral Raise", 3, 15, ss = 2),
        p("Rear Delt Fly", 3, 15, ss = 2),
        p("Karın Makinesi", 3, 12, 15),
        p("Plank", 3, 45, note = "saniye")
    )),
    WorkoutTemplate("Upper B", listOf(
        p("Lateral Raise", 4, 12, 15, note = "ilk hareket"),
        p("Incline Chest Press", 4, 6, 10),
        p("Lat Pulldown", 4, 10, 12, note = "dar tutuş"),
        p("Cable Row", 3, 10, ss = 1),
        p("Rear Delt Fly", 3, 15, ss = 1),
        p("Overhead Triceps Extension", 3, 12, ss = 2),
        p("Biceps Machine", 3, 10, 12, ss = 2),
        p("Leg Extension", 3, 15)
    ))
)

val TEMPLATES = USER_PROGRAM + listOf(
    t("Push", Triple("Bench Press", 4, 8), Triple("Overhead Press", 3, 8), Triple("Incline Dumbbell Press", 3, 10), Triple("Lateral Raise", 3, 15), Triple("Triceps Pushdown", 3, 12)),
    t("Pull", Triple("Deadlift", 3, 5), Triple("Pull-up", 3, 8), Triple("Barbell Row", 3, 10), Triple("Face Pull", 3, 15), Triple("Barbell Curl", 3, 12)),
    t("Legs", Triple("Squat", 4, 6), Triple("Romanian Deadlift", 3, 10), Triple("Leg Press", 3, 12), Triple("Leg Curl", 3, 12), Triple("Calf Raise", 4, 15)),
    t("Full Body", Triple("Squat", 3, 8), Triple("Bench Press", 3, 8), Triple("Barbell Row", 3, 10), Triple("Overhead Press", 3, 10), Triple("Plank", 3, 1)),
    t("Üst Vücut", Triple("Bench Press", 4, 8), Triple("Barbell Row", 4, 8), Triple("Overhead Press", 3, 10), Triple("Lat Pulldown", 3, 10), Triple("Dumbbell Curl", 3, 12), Triple("Triceps Pushdown", 3, 12)),
    t("Alt Vücut", Triple("Squat", 4, 8), Triple("Romanian Deadlift", 3, 10), Triple("Lunge", 3, 12), Triple("Leg Extension", 3, 15), Triple("Calf Raise", 4, 15))
)

/** Eski kayıtlardaki isimleri yenileriyle eşler. */
fun normalizeExercise(name: String): String = when (name.trim().lowercase()) {
    "barfiks" -> "Pull-up"
    "şınav" -> "Push-up"
    else -> name.trim()
}

// ---------------- Haftalık program ----------------

const val OFF_DAY = "OFF"

val WEEKDAY_NAMES = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

/** Hazır haftalık programlar: 7 gün, Pazartesi'den başlar. */
data class WeekPreset(val name: String, val desc: String, val days: List<String>)

val WEEK_PRESETS = listOf(
    WeekPreset("Senin programın · Upper/Lower 3 gün", "Pzt Upper A · Çar Lower · Cum Upper B", listOf("Upper A", OFF_DAY, "Lower", OFF_DAY, "Upper B", OFF_DAY, OFF_DAY)),
    WeekPreset("PPL · 6 gün", "İleri seviye, her kas haftada 2 kez", listOf("Push", "Pull", "Legs", "Push", "Pull", "Legs", OFF_DAY)),
    WeekPreset("Üst / Alt · 4 gün", "Orta seviye için ideal denge", listOf("Üst Vücut", "Alt Vücut", OFF_DAY, "Üst Vücut", "Alt Vücut", OFF_DAY, OFF_DAY)),
    WeekPreset("Full Body · 3 gün", "Yeni başlayanlar için", listOf("Full Body", OFF_DAY, "Full Body", OFF_DAY, "Full Body", OFF_DAY, OFF_DAY)),
    WeekPreset("PPL · 3 gün", "Az vakti olanlar için", listOf("Push", OFF_DAY, "Pull", OFF_DAY, "Legs", OFF_DAY, OFF_DAY))
)

val OFF_DAY_TIPS = listOf(
    "20-30 dk tempolu yürüyüş toparlanmayı hızlandırır.",
    "10 dk esneme veya foam roller ile kaslarını gevşet.",
    "Bugün en az 8 saat uyumayı hedefle.",
    "Proteini düşürme; kaslar dinlenirken büyür.",
    "Su hedefini tamamla, yarınki antrenmanın daha iyi geçer."
)

// ---------------- Kardiyo ----------------

data class CardioType(val name: String, val met: Double, val emoji: String)

val CARDIO_TYPES = listOf(
    CardioType("Tempolu yürüyüş", 4.3, "🚶"),
    CardioType("Koşu", 9.8, "🏃"),
    CardioType("Bisiklet", 7.5, "🚴"),
    CardioType("Yüzme", 7.0, "🏊"),
    CardioType("İp atlama", 11.0, "🪢"),
    CardioType("Eliptik", 5.0, "⚙️"),
    CardioType("Kürek", 7.0, "🚣"),
    CardioType("HIIT", 8.0, "⚡")
)

// ---------------- Günün sözü ----------------

val QUOTES = listOf(
    "Bugün yaptığın tek set, hiç yapmadığın yüz setten iyidir.",
    "Motivasyon başlatır, alışkanlık devam ettirir.",
    "Ayna yavaş değişir, tartı yalan söyler, trend doğruyu gösterir.",
    "Kötü bir antrenman bile atlanmış antrenmandan iyidir.",
    "Kendini dünkü hâlinle kıyasla, başkasıyla değil.",
    "Disiplin, canın istemediğinde de yapmaktır.",
    "Küçük adımlar, uzun vadede büyük farklar yaratır.",
    "Protein ye, su iç, uyu. Gerisi detay.",
    "İlerleme düz bir çizgi değildir, sadece devam et.",
    "Ağırlık yerinde saysa da sen gelmeye devam et.",
    "Seri bozulmasın, bugün bir şey kaydet.",
    "Zor olan başlamak; ilk sete bir kere giriş yap.",
    "Vücudun, alışkanlıklarının özeti.",
    "Mükemmel plan değil, uygulanan plan kazanır.",
    "Yorgunluk geçer, gurur kalır.",
    "Her tekrar, gelecekteki sana bir yatırım.",
    "Ölçmediğini geliştiremezsin.",
    "Bugün kendine verdiğin sözü tut.",
    "Hız değil, istikrar kazandırır.",
    "Dinlenmek de antrenmanın bir parçası.",
    "Bir ay sonra bugün başladığına sevineceksin.",
    "Hedef uzak görünüyorsa sadece bugüne odaklan.",
    "Rekorlar, sıkıcı tekrarların ödülüdür.",
    "Aç kalmak değil, akıllı yemek kazandırır.",
    "Kendine iyi bak; tek bir bedenin var.",
    "Plato, vücudunun yeni bir zorluk istemesidir.",
    "Her gün yüzde bir daha iyi.",
    "Bahane değil, kayıt gir.",
    "Güç, tutarlılıktan doğar.",
    "Bugün de geldin. Bu zaten kazanmak."
)

// ---------------- Form ipuçları ----------------

val FORM_TIPS = mapOf(
    "Bench Press" to "Kürek kemiklerini sıkıştır, ayakları yere bas. Bar göğsün alt kısmına insin, dirsekler gövdeye ~45°.",
    "Incline Bench Press" to "Sehpa 30-45°. Bar köprücük kemiğinin biraz altına insin, omuzları geride tut.",
    "Dumbbell Press" to "Dambılları göğüs hizasına kontrollü indir, yukarıda birbirine çarptırma.",
    "Squat" to "Ayaklar omuz genişliğinde, dizler parmak yönünde. Kalçayı geriye ver, sırtı nötr tut, en az paralele in.",
    "Front Squat" to "Dirsekler yukarıda, gövde dik. Ağırlık ayağın ortasında kalsın.",
    "Deadlift" to "Bar ayağın ortası üzerinde. Sırt düz, göğüs açık; kalça ve omuz birlikte kalksın, barı bacağa yakın tut.",
    "Romanian Deadlift" to "Dizler hafif bükük, kalçayı geriye it. Arka bacakta gerilmeyi hissedince yukarı dön.",
    "Overhead Press" to "Karın ve kalçayı sık, beli bükme. Bar yüzüne yakın yükselsin, yukarıda başını öne al.",
    "Barbell Row" to "Gövde ~45°, sırt düz. Barı göbeğe çek, dirsekleri arkaya götür, savurma.",
    "Pull-up" to "Omuzları aşağı çek, göğsü bara yaklaştır. Aşağıda kolları tam aç.",
    "Lat Pulldown" to "Barı göğsün üstüne çek, gövdeyi hafif geriye yatır, omuzları kulaklardan uzak tut.",
    "Leg Press" to "Belini sehpadan kaldırma, dizleri tam kilitleme.",
    "Lateral Raise" to "Hafif ağırlık, dirsekler hafif bükük. Omuz hizasına kadar kaldır, yavaş indir.",
    "Barbell Curl" to "Dirsekleri gövdeye sabitle, savurmadan kaldır, yavaş indir.",
    "Triceps Pushdown" to "Dirsekler sabit, kolu tam aç, yukarıda kontrollü dön.",
    "Hip Thrust" to "Sırt üstü bank kenarında, çene içeride. Yukarıda kalçayı 1 sn sık.",
    "Plank" to "Baştan topuğa düz çizgi; kalçayı düşürme, karın ve kalçayı sık.",
    "Dips" to "Omuzları aşağıda tut, dirsek 90°'ye kadar in, öne hafif eğil.",
    "Face Pull" to "İpi yüze doğru çek, elleri kulak hizasında ayır, dirsekler yukarıda.",
    "Hack Squat" to "Sırtı pede yasla, ayaklar platformun ortasında. Dizleri içe kaçırmadan derin in.",
    "Chest Press" to "Kürek kemiklerini geride tut, tutamaçlar göğüs ortası hizasında olsun.",
    "Incline Chest Press" to "Üst göğsü hedefler; omuzları kaldırma, kontrollü indir.",
    "Seated Row" to "Göğüs pede yaslı, dirsekleri arkaya çek, sonda kürek kemiklerini sık.",
    "Cable Row" to "Gövdeyi sallamadan tutamacı göbeğe çek, kürek kemiklerini birleştir.",
    "Shoulder Press" to "Sırt pede yaslı, belini bükme; yukarıda dirsekleri kilitleme.",
    "Rear Delt Fly" to "Hafif ağırlık, dirsekler hafif bükük. Arka omzu hissederek yana aç.",
    "Leg Curl" to "Kalçayı pedden kaldırma, yukarıda 1 sn sık, yavaş indir.",
    "Leg Extension" to "Üstte 1 sn sık, ağırlığı savurmadan kontrollü indir.",
    "Biceps Cable Curl" to "Dirsekler sabit, yukarıda sık, aşağıda kolu tam aç.",
    "Karın Makinesi" to "Nefes verirken karnı büzerek eğil, kalçayla çekme."
)

// ---------------- Renk temaları (seviye ile açılır) ----------------

data class AccentTheme(val id: String, val name: String, val color: Long, val unlockLevel: Int)

val ACCENTS = listOf(
    AccentTheme("LIME", "Neon", 0xFFC6F432, 1),
    AccentTheme("CYAN", "Okyanus", 0xFF4DE1E1, 3),
    AccentTheme("AMBER", "Gün batımı", 0xFFFFC145, 5),
    AccentTheme("VIOLET", "Mor ötesi", 0xFFB59CFF, 8),
    AccentTheme("PINK", "Pembe güç", 0xFFFF7AC6, 12)
)



/** Hareket adlarının kısa Türkçe açıklaması: "hangi makine, ne yapıyorsun". */
val EXERCISE_TR: Map<String, String> = mapOf(
    "Chest Press" to "oturarak göğüs itme makinesi",
    "Incline Chest Press" to "eğimli göğüs itme makinesi (üst göğüs)",
    "Decline Chest Press" to "aşağı eğimli göğüs itme makinesi (alt göğüs)",
    "Pec Deck" to "kelebek makinesi, kolları önde birleştirme",
    "Smith Machine Bench Press" to "raylı barda düz bench",
    "Smith Machine Incline Press" to "raylı barda eğimli bench",
    "Cable Fly" to "kabloyla göğüs açma-kapama",
    "Low-to-High Cable Fly" to "kablo aşağıdan yukarı göğüs açma (üst göğüs)",
    "High-to-Low Cable Fly" to "kablo yukarıdan aşağı göğüs açma (alt göğüs)",
    "Bench Press" to "sehpada barla göğüs itme",
    "Incline Bench Press" to "eğimli sehpada barla göğüs itme",
    "Dumbbell Press" to "dambılla göğüs itme",
    "Incline Dumbbell Press" to "eğimli sehpada dambılla göğüs itme",
    "Dumbbell Fly" to "dambılla göğüs açma",
    "Dips" to "paralel barda vücut ağırlığıyla aşağı-yukarı",
    "Push-up" to "şınav",
    "Lat Pulldown" to "oturarak barı göğse çekme (kanat)",
    "Seated Row" to "oturarak makinede kürek çekme",
    "Chest Supported Row" to "göğüs destekli kürek makinesi",
    "T-Bar Row" to "T-bar kürek, göğüs destekli ya da ayakta",
    "Assisted Pull-up" to "destekli barfiks makinesi",
    "Pullover Machine" to "pullover makinesi, kolları baş üstünden aşağı indirme",
    "Back Extension" to "bel ekstansiyonu (hiperekstansiyon)",
    "High Row Machine" to "yukarıdan aşağı kürek makinesi",
    "Cable Row" to "kabloyla oturarak kürek",
    "Seated Cable Row" to "kabloyla oturarak kürek",
    "Straight Arm Pulldown" to "kablo, kollar düz aşağı çekme (kanat)",
    "Face Pull" to "halatla yüze doğru çekme (arka omuz)",
    "Single Arm Cable Row" to "tek kolla kablo kürek",
    "Deadlift" to "yerden barla kaldırma",
    "Barbell Row" to "eğilerek barla kürek",
    "Dumbbell Row" to "tek kolla dambıl kürek",
    "Pull-up" to "barfiks",
    "Leg Press" to "oturarak ayakla platform itme",
    "Hack Squat" to "sırtın yaslı, makinede çömelme",
    "Pendulum Squat" to "sarkaç squat makinesi",
    "Smith Machine Squat" to "raylı barda çömelme",
    "Leg Extension" to "oturarak bacağı öne açma (ön bacak)",
    "Leg Curl" to "bacağı arkaya bükme (arka bacak)",
    "Seated Leg Curl" to "oturarak arka bacak bükme",
    "Lying Leg Curl" to "yüzüstü yatarak arka bacak bükme",
    "Hip Abductor" to "bacakları dışa açma makinesi (kalça yanı)",
    "Hip Adductor" to "bacakları içe kapama makinesi (iç bacak)",
    "Glute Kickback Machine" to "kalça geri tepme makinesi",
    "Calf Raise" to "ayakta parmak ucunda yükselme (baldır)",
    "Seated Calf Raise" to "oturarak baldır kaldırma",
    "Hip Thrust Machine" to "kalça itme makinesi",
    "Cable Kickback" to "kabloyla bacağı geriye tepme (kalça)",
    "Cable Pull Through" to "bacak arasından kabloyu öne çekme (kalça)",
    "Squat" to "barla çömelme",
    "Front Squat" to "bar önde, çömelme",
    "Romanian Deadlift" to "dizler hafif bükük, kalçadan eğilme (arka bacak)",
    "Hip Thrust" to "sırt sehpada, barla kalça itme",
    "Lunge" to "öne adım atıp çömelme",
    "Bulgarian Split Squat" to "arka ayak sehpada, tek bacak çömelme",
    "Shoulder Press" to "oturarak omuz itme makinesi",
    "Lateral Raise Machine" to "yana kol açma makinesi (yan omuz)",
    "Rear Delt Fly" to "ters kelebek, arka omuz",
    "Cable Lateral Raise" to "kabloyla yana kol açma",
    "Cable Rear Delt Fly" to "kabloyla arka omuz açma",
    "Cable Upright Row" to "kabloyu çeneye doğru dik çekme",
    "Lateral Raise" to "yana kol açma (yan omuz)",
    "Dumbbell Shoulder Press" to "dambılla omuz itme",
    "Arnold Press" to "dönerek dambıl omuz itme",
    "Overhead Press" to "ayakta barla baş üstüne itme",
    "Biceps Machine" to "pazu bükme makinesi",
    "Preacher Curl Machine" to "dirsek destekli pazu makinesi",
    "Triceps Extension Machine" to "arka kol açma makinesi",
    "Triceps Dip Machine" to "oturarak dips makinesi (arka kol)",
    "Biceps Cable Curl" to "kabloyla pazu bükme",
    "Cable Hammer Curl" to "halatla çekiç tutuş pazu",
    "Triceps Pushdown" to "kabloyu aşağı itme (arka kol)",
    "Overhead Triceps Extension" to "kablo baş üstünden arka kol açma",
    "Single Arm Triceps Pushdown" to "tek kolla kablo aşağı itme",
    "Dumbbell Curl" to "dambılla pazu bükme",
    "Hammer Curl" to "çekiç tutuş dambıl pazu",
    "Incline Dumbbell Curl" to "eğimli sehpada dambıl pazu",
    "Barbell Curl" to "barla pazu bükme",
    "Skull Crusher" to "yatarak barı alna indirme (arka kol)",
    "Karın Makinesi" to "oturarak karın bükme makinesi",
    "Rotary Torso" to "gövde döndürme makinesi (yan karın)",
    "Cable Crunch" to "dizüstü, kabloyla karın bükme",
    "Plank" to "dirseklerde düz durma",
    "Crunch" to "mekik",
    "Hanging Leg Raise" to "barda asılıp bacak kaldırma",
)

fun exerciseTr(name: String): String? = EXERCISE_TR.entries.firstOrNull { it.key.equals(name.trim(), true) }?.value
