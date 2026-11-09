package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Mic
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.CustomFood
import com.fittakip.app.data.FOOD_DB
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.FoodItem
import com.fittakip.app.data.Meal
import com.fittakip.app.data.Profile
import com.fittakip.app.data.trLower
import com.fittakip.app.domain.ACTIVITY_LEVELS
import com.fittakip.app.domain.CalorieResult
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Gamification
import com.fittakip.app.domain.Goal
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(vm: AppViewModel) {
    val state by vm.profile.collectAsStateWithLifecycle()
    val allFoods by vm.foods.collectAsStateWithLifecycle()
    val water by vm.water.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val customFoods by vm.customFoods.collectAsStateWithLifecycle()
    val savedMeals by vm.savedMeals.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    var saveMealOf by remember { mutableStateOf<Meal?>(null) }
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var voiceStart by rememberSaveable { mutableStateOf(false) }
    var quick by rememberSaveable { mutableStateOf(false) }
    var editFood by remember { mutableStateOf<FoodEntry?>(null) }

    val foods = remember(allFoods, day) { allFoods.filter { it.epochDay == day } }
    val dayWater = remember(water, day) { water.filter { it.epochDay == day }.sumOf { it.ml } }
    val p = state.profile
    val waterGoal = Gamification.waterGoal(p, settings)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenHeader("Beslenme", longDayLabel(day)) }
                IconButton(onClick = { vm.shiftDay(-1) }) { Icon(Icons.Rounded.ChevronLeft, "Önceki gün") }
                IconButton(onClick = { vm.shiftDay(1) }, enabled = day < vm.today) { Icon(Icons.Rounded.ChevronRight, "Sonraki gün") }
            }
        }
        if (!state.loaded) return@LazyColumn

        if (p == null || editing) {
            item {
                ProfileForm(
                    initial = p, onCancel = if (p != null) { { editing = false } } else null,
                    initialLifeStage = settings.lifeStage, initialHealth = settings.healthCondition
                ) { prof, stage, health ->
                    vm.saveProfile(prof)
                    vm.setSafety(stage, health)
                    editing = false
                }
            }
            return@LazyColumn
        }

        val result = Calculator.calculate(p, settings)
        if (day != vm.today) {
            item {
                Pill(
                    "Geçmiş gün: ${dayLabel(day, withYear = true)} · bugüne dön", FitColors.Blue,
                    onClick = { vm.shiftDay((vm.today - day).toInt()) }
                )
            }
        }
        val warnings = Calculator.safetyWarnings(p.male, p.heightCm, p.weightKg, p.targetWeightKg, Goal.from(p.goal), settings.lifeStage, settings.healthCondition)
        if (warnings.isNotEmpty()) item { SafetyNotice(warnings) }
        item { HeroCard(p, result, foods, settings.kcalAdjust, dayWater, waterGoal) { editing = true } }
        // En sık yapılan iş en üstte: yemek ekle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Yemek ekle", onClick = { sheet = true }, icon = Icons.Rounded.Add, modifier = Modifier.weight(1f))
                    // Üşengeç modu: söyle, eklesin
                    FilledTonalIconButton(
                        onClick = { voiceStart = true; sheet = true },
                        modifier = Modifier.size(56.dp), shape = RoundedCornerShape(18.dp)
                    ) { Icon(Icons.Rounded.Mic, "Sesle ekle", modifier = Modifier.size(26.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Hızlı kalori", onClick = { quick = true }, icon = Icons.Rounded.Bolt, modifier = Modifier.weight(1f))
                    SecondaryButton("Dünü kopyala", onClick = { vm.copyPreviousDay() }, icon = Icons.Rounded.ContentCopy, modifier = Modifier.weight(1f))
                }
            }
        }
        item { WaterCard(dayWater, waterGoal, { vm.addWater(it) }, { vm.undoWater() }) }

        if (foods.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Restaurant,
                    if (day == vm.today) "Bugün henüz bir şey eklemedin" else "Bu gün için kayıt yok",
                    "170+ ev yemeği ve internette milyonlarca market ürünü içinden ara, barkod okut ya da kendi değerlerini gir."
                )
            }
        }
        (Meal.choices(settings.fastingMode) + Meal.entries).distinct().forEach { m ->
            val list = foods.filter { Meal.from(it.meal) == m }
            if (list.isNotEmpty()) {
                item(key = "meal-${m.name}") {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${m.emoji} ${m.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        HintText("${list.sumOf { it.kcal }} kcal")
                        if (list.size >= 2) TextButton(onClick = { saveMealOf = m }) { Text("Öğünü kaydet") }
                    }
                }
                items(list, key = { it.id }) { f ->
                    SwipeToDelete(onDelete = { vm.deleteFood(f) }, modifier = Modifier.animateItem()) {
                        FoodRow(f, onClick = { editFood = f }) { vm.deleteFood(f) }
                    }
                }
            }
        }
        if (foods.isNotEmpty()) item { HintText("Bir kayda dokunarak porsiyonunu ya da öğününü değiştirebilirsin.") }
        item {
            Pill(
                if (settings.fastingMode) "🌙 Oruç modu açık · öğünler sahur / iftar" else "🌙 Oruç tutuyorum (sahur / iftar)",
                if (settings.fastingMode) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { vm.updateSettings { it.copy(fastingMode = !it.fastingMode) } }
            )
        }
        item {
            val adaptive = remember(allFoods, weights) { Calculator.adaptiveTdee(allFoods, weights, vm.today) }
            AdaptiveCard(adaptive, result.tdee, settings.kcalAdjust) { vm.applyAdaptiveTdee(it) }
        }

        // Öğün dağılımı
        if (foods.size >= 2) {
            item {
                SectionCard("Öğün dağılımı", icon = Icons.Rounded.PieChart) {
                    val total = foods.sumOf { it.kcal }.coerceAtLeast(1)
                    HBars(
                        Meal.entries.filter { m -> foods.any { Meal.from(it.meal) == m } }.map { m -> "${m.emoji} ${m.label} · %${foods.filter { Meal.from(it.meal) == m }.sumOf { it.kcal } * 100 / total}" to foods.filter { Meal.from(it.meal) == m }.sumOf { it.kcal }.toDouble() },
                        FitColors.Orange, "kcal"
                    )
                }
            }
        }

        // Haftalık bütçe
        item {
            val ws = Gamification.weekStart(day)
            val weekDays = (ws..(ws + 6)).toList()
            val eaten = allFoods.filter { it.epochDay in ws..minOf(day, ws + 6) }.sumOf { it.kcal }
            val budget = result.target * 7
            val daysLeft = (ws + 6 - day).toInt() + 1
            val remaining = budget - eaten
            SectionCard("Haftalık kalori bütçesi", icon = Icons.Rounded.Flag) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$eaten", style = MaterialTheme.typography.headlineMedium)
                    Text(" / $budget kcal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
                ProgressBar(eaten.toFloat() / budget, if (eaten > budget) FitColors.Red else FitColors.Lime, 10.dp)
                HintText(
                    if (remaining > 0) "Haftanın kalan $daysLeft günü için günde ortalama ${remaining / daysLeft} kcal hakkın var."
                    else "Haftalık bütçeyi ${-remaining} kcal aştın; kalan günlerde biraz hafif git."
                )
                HintText("Hafta: ${dayLabel(weekDays.first())} – ${dayLabel(weekDays.last())}")
            }
        }

        item {
            val days = (6 downTo 0).map { day - it }
            val values = days.map { d -> allFoods.filter { it.epochDay == d }.sumOf { it.kcal }.toDouble() }
            val logged = values.filter { it > 0 }
            SectionCard("Son 7 gün", icon = Icons.Rounded.LocalFireDepartment) {
                BarChart(values, days.map { shortDayName(it) }, FitColors.Lime, result.target.toDouble())
                HintText(
                    if (logged.isEmpty()) "Turuncu çizgi günlük hedefin (${result.target} kcal)."
                    else "Ortalama ${logged.average().roundToInt()} kcal · hedef ${result.target} kcal (turuncu çizgi)"
                )
            }
        }
    }

    if (sheet) {
        AddFoodSheet(
            fasting = settings.fastingMode,
            history = allFoods,
            customFoods = customFoods,
            savedMeals = savedMeals,
            onSavedMeal = { sm, meal -> vm.addSavedMeal(sm, meal) },
            onDeleteSavedMeal = { vm.deleteSavedMeal(it) },
            favorites = settings.favorites,
            onToggleFavorite = { vm.toggleFavorite(it) },
            onSaveCustom = { n, k, pr, cb, ft -> vm.saveCustomFood(n, k, pr, cb, ft) },
            onDeleteCustom = { vm.deleteCustomFood(it) },
            startVoice = voiceStart,
            today = vm.today,
            onDismiss = { sheet = false; voiceStart = false }
        ) { name, kcal, pr, cb, ft, meal -> vm.addFood(name, kcal, pr, cb, ft, meal) }
    }

    saveMealOf?.let { m ->
        var name by remember(m) { mutableStateOf("Benim ${m.label.lowercase()}m") }
        AlertDialog(
            onDismissRequest = { saveMealOf = null },
            title = { Text("Öğünü kaydet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HintText(foods.filter { Meal.from(it.meal) == m }.joinToString(", ") { it.name })
                    TextInput(name, { name = it }, "Öğün adı")
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.saveMeal(name.trim(), foods.filter { Meal.from(it.meal) == m }); saveMealOf = null }, enabled = name.isNotBlank()) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { saveMealOf = null }) { Text("Vazgeç") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (quick) QuickKcalDialog(settings.fastingMode, onDismiss = { quick = false }) { kcal, meal ->
        vm.addFood("Hızlı ekleme", kcal, 0.0, 0.0, 0.0, meal)
        quick = false
    }

    editFood?.let { f ->
        AlertDialog(
            onDismissRequest = { editFood = null },
            title = { Text(f.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HintText("${f.kcal} kcal · P ${f.protein.roundToInt()} · K ${f.carb.roundToInt()} · Y ${f.fat.roundToInt()}")
                    Text("Porsiyonu değiştir", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5, 0.75, 1.5, 2.0).forEach { x ->
                            SelectCard(false, { vm.scaleFood(f, x); editFood = null }, "${x.clean()}×", Modifier.weight(1f))
                        }
                    }
                    Text("Öğüne taşı", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Meal.choices(settings.fastingMode).forEach { m ->
                            SelectCard(Meal.from(f.meal) == m, { vm.moveFood(f, m); editFood = null }, m.emoji, Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editFood = null }) { Text("Kapat") } },
            dismissButton = { TextButton(onClick = { vm.deleteFood(f); editFood = null }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun QuickKcalDialog(fasting: Boolean, onDismiss: () -> Unit, onAdd: (Int, Meal) -> Unit) {
    var kcal by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(Meal.forHour(LocalTime.now().hour, fasting)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hızlı kalori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HintText("Ne yediğini yazmakla uğraşmak istemiyorsan sadece kaloriyi gir.")
                NumField(kcal, { kcal = it }, "Kalori", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Meal.choices(fasting).forEach { m -> SelectCard(meal == m, { meal = m }, m.emoji, Modifier.weight(1f)) }
                }
            }
        },
        confirmButton = {
            val k = kcal.toNum()?.toInt()
            TextButton(onClick = { onAdd(k!!, meal) }, enabled = k != null && k in 1..5000) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun AdaptiveCard(a: Calculator.AdaptiveTdee?, formulaTdee: Int, adjust: Int, onApply: (Int) -> Unit) {
    SectionCard("Gerçek kalori ihtiyacın", icon = Icons.Rounded.Insights) {
        if (a == null) {
            HintText("4 hafta boyunca yemeklerini ve en az haftada 2 kez kilonu kaydedersen, formül yerine senin gerçek harcamanı hesaplarım. Genelde formülden 200-400 kcal farklı çıkar.")
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${a.tdee}", style = MaterialTheme.typography.headlineLarge, color = FitColors.Lime)
                Text(" kcal/gün", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            HintText("Son ${a.loggedDays} günde ortalama ${a.avgIntake} kcal yedin, kilon haftada ${a.kgPerWeek.signed(2)} kg değişti. Formül ${formulaTdee} kcal diyordu. Güven: ${a.confidence}.")
            val newAdjust = a.tdee - formulaTdee
            if (kotlin.math.abs(newAdjust - adjust) >= 50) {
                PrimaryButton("Hedefimi buna göre ayarla", onClick = { onApply(a.tdee) })
            } else HintText("✅ Hedefin zaten gerçek harcamana göre ayarlı.")
        }
    }
}

@Composable
private fun HeroCard(p: Profile, r: CalorieResult, foods: List<FoodEntry>, adjust: Int, water: Int, waterGoal: Int, onEdit: () -> Unit) {
    val eaten = foods.sumOf { it.kcal }
    val protein = foods.sumOf { it.protein }
    val carb = foods.sumOf { it.carb }
    val fat = foods.sumOf { it.fat }

    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Günlük durum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Pill(Goal.from(p.goal).label, MaterialTheme.colorScheme.secondary)
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Profili düzenle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val left = r.target - eaten
            TripleRing(
                eaten.toFloat() / r.target, (protein / r.proteinG.coerceAtLeast(1)).toFloat(), water.toFloat() / waterGoal.coerceAtLeast(1),
                "${kotlin.math.abs(left)}", if (left >= 0) "kcal kaldı" else "kcal fazla", left < 0
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniStat(Icons.Rounded.LocalFireDepartment, "Kalori", "$eaten / ${r.target}", FitColors.Lime)
                MiniStat(Icons.Rounded.Restaurant, "Protein", "${protein.roundToInt()} / ${r.proteinG} g", FitColors.Blue)
                MiniStat(Icons.Rounded.WaterDrop, "Su", "${(water / 1000.0).fmt(1)} / ${(waterGoal / 1000.0).fmt(1)} L", Color(0xFF4DE1E1))
            }
        }
        MacroBar("Karbonhidrat", carb, r.carbG, FitColors.Lime)
        MacroBar("Yağ", fat, r.fatG, FitColors.Orange)
        val total = protein * 4 + carb * 4 + fat * 9
        if (total > 0) {
            HintText(
                "Kalori dağılımı: protein %${(protein * 4 / total * 100).roundToInt()} · " +
                    "karb %${(carb * 4 / total * 100).roundToInt()} · yağ %${(fat * 9 / total * 100).roundToInt()}"
            )
        }
        if (adjust != 0) HintText("Hedefe ${if (adjust > 0) "+" else ""}$adjust kcal düzeltme uygulandı (Vücut sekmesinden değiştirebilirsin).")
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, color, 32.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WaterCard(ml: Int, goal: Int, onAdd: (Int) -> Unit, onUndo: () -> Unit) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.WaterDrop, FitColors.Blue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Su", style = MaterialTheme.typography.titleMedium)
                HintText("$ml / $goal ml · ${ml / 250} bardak")
            }
            Text(
                "%${(ml * 100 / goal.coerceAtLeast(1)).coerceAtMost(999)}",
                style = MaterialTheme.typography.titleLarge,
                color = if (ml >= goal) FitColors.Lime else FitColors.Blue
            )
        }
        ProgressBar(ml.toFloat() / goal.coerceAtLeast(1), FitColors.Blue, 10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("+250 ml", onClick = { onAdd(250) }, modifier = Modifier.weight(1f))
            SecondaryButton("+500 ml", onClick = { onAdd(500) }, modifier = Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = ml > 0, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Undo, "Geri al")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    fasting: Boolean,
    history: List<FoodEntry>,
    customFoods: List<CustomFood>,
    savedMeals: List<com.fittakip.app.data.SavedMeal>,
    onSavedMeal: (com.fittakip.app.data.SavedMeal, Meal) -> Unit,
    onDeleteSavedMeal: (com.fittakip.app.data.SavedMeal) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSaveCustom: (String, Int, Double, Double, Double) -> Unit,
    onDeleteCustom: (CustomFood) -> Unit,
    startVoice: Boolean = false,
    today: Long = java.time.LocalDate.now().toEpochDay(),
    onDismiss: () -> Unit,
    onAdd: (String, Int, Double, Double, Double, Meal) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var meal by remember { mutableStateOf(Meal.forHour(LocalTime.now().hour, fasting)) }

    // ---- Üşengeç modu: her zamanki öğün + sesle ekleme ----
    val usual = remember(history, meal, today) { com.fittakip.app.domain.Routine.usual(history, meal, today) }
    var voiceResult by remember { mutableStateOf<com.fittakip.app.domain.VoiceParser.Parsed?>(null) }
    var voiceMsg by remember { mutableStateOf<String?>(null) }
    val voiceCatalog = remember(history, customFoods) {
        // Önce kendi geçmişin ve yemeklerin (senin porsiyonların), sonra hazır liste
        history.distinctBy { it.name.lowercase() }.map { FoodItem(it.name, "senin porsiyonun", it.kcal, it.protein, it.carb, it.fat) } +
            customFoods.map { FoodItem(it.name, it.portion, it.kcal, it.protein, it.carb, it.fat) } + FOOD_DB
    }
    val listen = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { res ->
        val heard = res.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (heard.isNullOrBlank()) { if (res.resultCode == android.app.Activity.RESULT_OK) voiceMsg = "Bir şey duyamadım, tekrar dene."; return@rememberLauncherForActivityResult }
        val parsed = com.fittakip.app.domain.VoiceParser.parse(heard, voiceCatalog)
        voiceResult = parsed
        voiceMsg = if (parsed.matches.isEmpty()) "\"$heard\" — listede eşleşen yemek bulamadım. Arama kutusuna yazmayı dene." else null
    }
    fun startListening() {
        val i = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ne yedin? Ör: iki yumurta, bir simit, bir çay")
        try { listen.launch(i) } catch (e: android.content.ActivityNotFoundException) { voiceMsg = "Telefonda ses tanıma bulunamadı (Google uygulaması gerekli)." }
    }
    LaunchedEffect(startVoice) { if (startVoice) startListening() }

    var query by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var mult by remember { mutableDoubleStateOf(1.0) }

    // Kendi yemeklerin de hazır listeyle aynı biçimde aranır
    val mine = remember(customFoods) { customFoods.map { FoodItem(it.name, it.portion, it.kcal, it.protein, it.carb, it.fat) } }
    val all = remember(mine) { (mine + FOOD_DB).distinctBy { it.name.trLower() } }
    val frequent = remember(history) {
        history.groupBy { it.name.trLower() }.values.sortedByDescending { it.size }.take(6).map { it.first() }
    }
    val favItems = remember(favorites, all) { all.filter { it.name in favorites } }
    val results = remember(query, all) {
        val q = query.trim().trLower()
        if (q.isEmpty()) FOOD_DB.take(20) else all.filter { it.name.trLower().contains(q) }
    }

    // İnternetten arama (Open Food Facts): yazmayı bırakınca 0,8 sn sonra
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var online by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var onlineState by remember { mutableStateOf("") }   // "", "loading", "done", hata mesajı
    LaunchedEffect(query) {
        val q = query.trim()
        online = emptyList()
        if (q.length < 3) { onlineState = ""; return@LaunchedEffect }
        kotlinx.coroutines.delay(800)
        onlineState = "loading"
        onlineState = try {
            online = com.fittakip.app.data.OnlineFoods.search(q).filter { o -> results.none { it.name.equals(o.name, true) } }
            "done"
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            (e as? com.fittakip.app.data.OnlineFoods.OnlineException)?.message ?: "İnternet yok; sadece telefondaki liste aranıyor."
        }
    }
    // ---- Barkod ----
    var scanMsg by remember { mutableStateOf<String?>(null) }   // arama kutusunun hemen altında görünür
    var manualBarcode by remember { mutableStateOf(false) }
    fun lookupBarcode(code: String) {
        scanMsg = "🔎 $code aranıyor…"
        scope.launch {
            scanMsg = try {
                val item = com.fittakip.app.data.OnlineFoods.byBarcode(code)
                if (item != null) { picked = item; mult = 1.0; null }
                else "Bu barkod ($code) veritabanında yok. Adıyla aramayı ya da \"Elle gir\"i dene."
            } catch (e: Exception) { "Barkod aranamadı: ${e.message ?: "internet yok"}" }
        }
    }
    fun scanBarcode() {
        val opts = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13, com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8,
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A, com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
            ).enableAutoZoom().build()
        val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, opts)
        fun start() {
            scanner.startScan()
                .addOnSuccessListener { b -> b.rawValue?.let { lookupBarcode(it) } ?: run { scanMsg = "Barkod okunamadı, tekrar dene." } }
                .addOnCanceledListener { scanMsg = null }
                .addOnFailureListener { scanMsg = "Barkod okuyucu açılamadı (${it.message}). Barkodu elle yazabilirsin." ; manualBarcode = true }
        }
        // Okuyucu Google Play Hizmetleri'nin ayrı bir modülü: telefonda yoksa önce indir.
        val installer = com.google.android.gms.common.moduleinstall.ModuleInstall.getClient(context)
        installer.areModulesAvailable(scanner)
            .addOnSuccessListener { r ->
                if (r.areModulesAvailable()) start()
                else {
                    scanMsg = "⬇️ Barkod okuyucu ilk kullanım için indiriliyor…"
                    installer.installModules(com.google.android.gms.common.moduleinstall.ModuleInstallRequest.newBuilder().addApi(scanner).build())
                        .addOnSuccessListener { scanMsg = null; start() }
                        .addOnFailureListener { scanMsg = "Barkod okuyucu indirilemedi (${it.message}). Barkodu elle yazabilirsin."; manualBarcode = true }
                }
            }
            .addOnFailureListener { start() }
    }

    // ---- Fotoğraftan kalori ----
    var photoBusy by remember { mutableStateOf(false) }
    var photoResult by remember { mutableStateOf<com.fittakip.app.data.FoodVision.Result?>(null) }
    var photoMsg by remember { mutableStateOf<String?>(null) }
    var pendingCamera by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
    var photoJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var photoSecs by remember { mutableIntStateOf(0) }
    LaunchedEffect(photoBusy) { photoSecs = 0; while (photoBusy) { kotlinx.coroutines.delay(1000); photoSecs++ } }
    fun analyze(uri: android.net.Uri) {
        photoBusy = true; photoMsg = null; photoResult = null
        photoJob = scope.launch {
            try {
                val r = com.fittakip.app.data.FoodVision.analyze(context, uri)
                if (r.items.isEmpty()) photoMsg = r.note.ifBlank { "Fotoğrafta yiyecek bulamadım." } else photoResult = r
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException)
                    photoMsg = (e as? com.fittakip.app.data.FoodVision.VisionException)?.message ?: "Hata: ${e.message ?: e.javaClass.simpleName}"
            }
            photoBusy = false
        }
    }
    val takePhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { ok ->
        val u = pendingCamera
        if (ok && u != null) analyze(u)
    }
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { u ->
        u?.let { analyze(it) }
    }
    fun openCamera() {
        val dir = java.io.File(context.cacheDir, "food").apply { mkdirs() }
        val f = java.io.File(dir, "tabak.jpg")
        val u = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", f)
        pendingCamera = u
        takePhoto.launch(u)
    }

    if (manualBarcode) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualBarcode = false },
            title = { Text("Barkodu yaz") },
            text = { NumField(code, { code = it.filter { ch -> ch.isDigit() } }, "Paketin altındaki numara (8–13 hane)", Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(enabled = code.length in 8..14, onClick = { manualBarcode = false; lookupBarcode(code) }) { Text("Ara") } },
            dismissButton = { TextButton(onClick = { manualBarcode = false }) { Text("Vazgeç") } }
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        LazyColumn(
            Modifier.fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Yemek ekle", style = MaterialTheme.typography.headlineMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Meal.choices(fasting).forEach { m -> SelectCard(meal == m, { meal = m }, m.emoji, Modifier.weight(1f), m.label) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectCard(!manual, { manual = false }, "Listeden seç", Modifier.weight(1f))
                    SelectCard(manual, { manual = true; picked = null }, "Elle gir", Modifier.weight(1f))
                }
            }

            val pk = picked
            if (manual) {
                item {
                    ManualFoodForm(onSaveCustom) { n, k, pr, cb, ft ->
                        onAdd(n, k, pr, cb, ft, meal)
                        onDismiss()
                    }
                }
            } else if (pk != null) {
                item {
                    FitCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pk.name, style = MaterialTheme.typography.titleMedium)
                                HintText("1 porsiyon = ${pk.portion}")
                            }
                            FavStar(pk.name in favorites) { onToggleFavorite(pk.name) }
                            IconButton(onClick = { picked = null }) { Icon(Icons.Rounded.Close, "Geri") }
                        }
                        Text("Porsiyon", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { m ->
                                SelectCard(mult == m, { mult = m }, "${m.clean()}×", Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Kalori", "${(pk.kcal * mult).roundToInt()}", Modifier.weight(1f), FitColors.Lime)
                            MetricTile("Protein", "${(pk.p * mult).roundToInt()} g", Modifier.weight(1f))
                            MetricTile("Karb.", "${(pk.c * mult).roundToInt()} g", Modifier.weight(1f))
                            MetricTile("Yağ", "${(pk.f * mult).roundToInt()} g", Modifier.weight(1f))
                        }
                        PrimaryButton("${meal.label} öğününe ekle", onClick = {
                            val name = if (mult == 1.0) pk.name else "${pk.name} (${mult.clean()}×)"
                            onAdd(name, (pk.kcal * mult).roundToInt(), pk.p * mult, pk.c * mult, pk.f * mult, meal)
                            onDismiss()
                        }, icon = Icons.Rounded.Add)
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Ara: tavuk, simit, Ülker, Pınar…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = { IconButton(onClick = { scanBarcode() }) { Icon(Icons.Rounded.QrCodeScanner, "Barkod okut") } },
                        singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
                usual?.let { u ->
                    if (voiceResult == null) item {
                        Surface(
                            onClick = { u.items.forEach { e -> onAdd(e.name, e.kcal, e.protein, e.carb, e.fat, meal) }; onDismiss() },
                            shape = RoundedCornerShape(18.dp), color = FitColors.Lime.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${meal.emoji} Her zamanki ${Meal.yours(meal)}", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime)
                                    HintText("${u.label} · ${u.kcal} kcal")
                                }
                                Text("✓ Ekle", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime)
                            }
                        }
                    }
                }
                item {
                    SecondaryButton("🎤 Söyle, ekleyeyim", onClick = { startListening() }, modifier = Modifier.fillMaxWidth())
                }
                voiceMsg?.let { m -> item { HintText(m) } }
                voiceResult?.let { v ->
                    if (v.matches.isNotEmpty()) item {
                        VoiceResultCard(v, meal, onChange = { voiceResult = it }, onDismiss = { voiceResult = null }) { list ->
                            list.forEach { m -> onAdd(m.item.name + if (m.qty != 1.0) " ×${m.qty.toString().removeSuffix(".0").replace('.', ',')}" else "",
                                m.kcal, m.item.p * m.qty, m.item.c * m.qty, m.item.f * m.qty, meal) }
                            onDismiss()
                        }
                    }
                }
                scanMsg?.let { m ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HintText(m, Modifier.weight(1f))
                            TextButton(onClick = { manualBarcode = true }) { Text("Elle yaz") }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("📷 Fotoğraf çek", enabled = !photoBusy, modifier = Modifier.weight(1f),
                            onClick = { openCamera() })
                        SecondaryButton("🖼️ Galeriden", enabled = !photoBusy, modifier = Modifier.weight(1f),
                            onClick = { pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    }
                }
                item { HintText("📷 Tabağının fotoğrafını çek, kalorisini Google Gemini tahmin etsin. Sadece o fotoğraf gönderilir, diğer kayıtların değil.") }
                if (photoBusy) item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Tabağına bakıyorum… $photoSecs sn", modifier = Modifier.weight(1f))
                        TextButton(onClick = { photoJob?.cancel(); photoBusy = false }) { Text("Vazgeç") }
                    }
                }
                if (photoBusy) item {
                    ShimmerRows(2)
                }
                photoMsg?.let { m -> item { Text(m, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
                photoResult?.let { r ->
                    item {
                        PhotoResultCard(r, meal, onRemove = { g -> photoResult = r.copy(items = r.items - g) }, onDismiss = { photoResult = null }) { list ->
                            list.forEach { g -> onAdd("${g.name} (fotoğraf)", g.kcal, g.p, g.c, g.f, meal) }
                            onDismiss()
                        }
                    }
                }
                if (query.isBlank()) {
                    if (savedMeals.isNotEmpty()) {
                        item { Text("🍱 Kayıtlı öğünlerin · tek dokunuşla hepsini ekle", style = MaterialTheme.typography.titleSmall) }
                        items(savedMeals, key = { "sm-" + it.id }) { sm ->
                            val arr = remember(sm.items) { org.json.JSONArray(sm.items) }
                            val names = (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).getString("n") }
                            val kcal = (0 until arr.length()).sumOf { arr.getJSONObject(it).getInt("k") }
                            PickRow(sm.name, names, kcal, null, null, onLongDelete = { onDeleteSavedMeal(sm) }) {
                                onSavedMeal(sm, meal)
                                onDismiss()
                            }
                        }
                    }
                    if (favItems.isNotEmpty()) {
                        item { Text("⭐ Favorilerin", style = MaterialTheme.typography.titleSmall) }
                        items(favItems, key = { "fav-" + it.name }) { item ->
                            PickRow(item.name, item.portion, item.kcal, true, { onToggleFavorite(item.name) }) { picked = item; mult = 1.0 }
                        }
                    }
                    if (frequent.isNotEmpty()) {
                        item { Text("Sık yediklerin · tek dokunuşla ekle", style = MaterialTheme.typography.titleSmall) }
                        items(frequent, key = { "f-" + it.id }) { f ->
                            PickRow(f.name, "P ${f.protein.roundToInt()} · K ${f.carb.roundToInt()} · Y ${f.fat.roundToInt()}", f.kcal, null, null) {
                                onAdd(f.name, f.kcal, f.protein, f.carb, f.fat, meal)
                                onDismiss()
                            }
                        }
                    }
                    if (customFoods.isNotEmpty()) {
                        item { Text("🧑‍🍳 Kendi yemeklerin", style = MaterialTheme.typography.titleSmall) }
                        items(customFoods, key = { "c-" + it.id }) { c ->
                            PickRow(c.name, c.portion, c.kcal, c.name in favorites, { onToggleFavorite(c.name) }, onLongDelete = { onDeleteCustom(c) }) {
                                picked = FoodItem(c.name, c.portion, c.kcal, c.protein, c.carb, c.fat); mult = 1.0
                            }
                        }
                    }
                    item { Text("Hazır liste", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp)) }
                }
                if (results.isEmpty()) {
                    item { HintText("\"$query\" bulunamadı. \"Elle gir\" ile kendin ekleyip listene kaydedebilirsin.") }
                }
                items(results, key = { "d-" + it.name }) { item ->
                    PickRow(item.name, item.portion, item.kcal, item.name in favorites, { onToggleFavorite(item.name) }) { picked = item; mult = 1.0 }
                }
                if (query.trim().length >= 3 || onlineState.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Text("🌐 Market ürünleri (Open Food Facts)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (onlineState == "loading") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    if (onlineState == "loading") item { ShimmerRows(3) }
                    when {
                        onlineState == "done" && online.isEmpty() -> item { HintText("İnternette de bulunamadı. Barkodu okutmayı ya da \"Elle gir\"i dene.") }
                        onlineState != "" && onlineState != "loading" && onlineState != "done" -> item { HintText(onlineState) }
                    }
                    itemsIndexed(online, key = { i, it -> "o-$i-${it.name}" }) { _, item ->
                        PickRow(item.name, item.portion, item.kcal, item.name in favorites, { onToggleFavorite(item.name) }) { picked = item; mult = 1.0 }
                    }
                    if (online.isNotEmpty()) item { HintText("Değerler üreticilerin etiketinden, Open Food Facts gönüllülerince girildi; nadiren hatalı olabilir.") }
                }
            }
        }
    }
}

@Composable
private fun FavStar(on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (on) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            if (on) "Favorilerden çıkar" else "Favorilere ekle",
            tint = if (on) FitColors.Orange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PickRow(
    title: String,
    subtitle: String,
    kcal: Int,
    favorite: Boolean?,
    onFavorite: (() -> Unit)?,
    onLongDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HintText(subtitle)
            }
            Text("$kcal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(" kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (favorite != null && onFavorite != null) FavStar(favorite, onFavorite)
            if (onLongDelete != null) {
                IconButton(onClick = onLongDelete) { Icon(Icons.Rounded.DeleteOutline, "Listemden sil", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun ManualFoodForm(onSaveCustom: (String, Int, Double, Double, Double) -> Unit, onAdd: (String, Int, Double, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var per100 by rememberSaveable { mutableStateOf(false) }
    var grams by rememberSaveable { mutableStateOf("100") }
    var save by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextInput(name, { name = it }, "Ne yedin?")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectCard(!per100, { per100 = false }, "Porsiyon değeri", Modifier.weight(1f))
            SelectCard(per100, { per100 = true }, "100 g değeri", Modifier.weight(1f), "paketin arkasından")
        }
        if (per100) NumField(grams, { grams = it }, "Kaç gram yedin?", Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(kcal, { kcal = it }, "Kalori", Modifier.weight(1f))
            NumField(protein, { protein = it }, "Protein (g)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(carb, { carb = it }, "Karb. (g)", Modifier.weight(1f))
            NumField(fat, { fat = it }, "Yağ (g)", Modifier.weight(1f))
        }
        val factor = if (per100) (grams.toNum() ?: 0.0) / 100 else 1.0
        val p = (protein.toNum() ?: 0.0) * factor
        val c = (carb.toNum() ?: 0.0) * factor
        val f = (fat.toNum() ?: 0.0) * factor
        val k = kcal.toNum()?.let { it * factor } ?: (p * 4 + c * 4 + f * 9).takeIf { it > 0 }
        if (k != null && (per100 || kcal.isBlank())) {
            Pill("Toplam: ${k.roundToInt()} kcal · P ${p.roundToInt()} · K ${c.roundToInt()} · Y ${f.roundToInt()}", MaterialTheme.colorScheme.primary)
        } else if (kcal.isBlank()) {
            HintText("Kaloriyi bilmiyorsan boş bırak, protein/karb/yağdan hesaplarım.")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = save, onCheckedChange = { save = it })
            Text("Listeme kaydet (sonra aramada çıksın)", style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            "Ekle",
            onClick = {
                if (save) onSaveCustom(name.trim(), k!!.roundToInt(), p, c, f)
                onAdd(name.trim(), k!!.roundToInt(), p, c, f)
            },
            enabled = name.isNotBlank() && k != null && k >= 0,
            icon = Icons.Rounded.Add
        )
    }
}

@Composable
private fun FoodRow(f: FoodEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Rounded.Restaurant, FitColors.Orange, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HintText("P ${f.protein.roundToInt()}g · K ${f.carb.roundToInt()}g · Y ${f.fat.roundToInt()}g")
            }
            Text("${f.kcal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(" kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfileForm(
    initial: Profile?, onCancel: (() -> Unit)?,
    initialLifeStage: String = "", initialHealth: Boolean = false,
    onSave: (Profile, String, Boolean) -> Unit
) {
    var lifeStage by rememberSaveable { mutableStateOf(initialLifeStage) }
    var health by rememberSaveable { mutableStateOf(initialHealth) }
    var male by rememberSaveable { mutableStateOf(initial?.male ?: true) }
    var age by rememberSaveable { mutableStateOf(initial?.age?.toString() ?: "") }
    var height by rememberSaveable { mutableStateOf(initial?.heightCm?.clean() ?: "") }
    var weight by rememberSaveable { mutableStateOf(initial?.weightKg?.clean() ?: "") }
    var target by rememberSaveable { mutableStateOf(initial?.targetWeightKg?.clean() ?: "") }
    var activity by rememberSaveable { mutableDoubleStateOf(initial?.activity ?: 1.55) }
    var goal by rememberSaveable { mutableStateOf(initial?.goal ?: Goal.MAINTAIN.name) }

    SectionCard(
        if (initial == null) "Önce seni tanıyalım" else "Profili düzenle",
        icon = Icons.Rounded.Person,
        action = if (onCancel != null) {
            { IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = "Kapat") } }
        } else null
    ) {
        if (initial == null) {
            HintText("Günlük kalori, makro ve su hedefini bu bilgilerle hesaplıyorum. Sonra istediğin zaman değiştirebilirsin.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectCard(male, { male = true; lifeStage = "" }, "Erkek", Modifier.weight(1f))
            SelectCard(!male, { male = false }, "Kadın", Modifier.weight(1f))
        }
        if (!male) LifeStagePicker(lifeStage) { lifeStage = it; if (it.isNotEmpty() && goal == Goal.CUT.name) goal = Goal.MAINTAIN.name }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(age, { age = it }, "Yaş", Modifier.weight(1f))
            NumField(height, { height = it }, "Boy (cm)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(weight, { weight = it }, "Kilo (kg)", Modifier.weight(1f))
            NumField(target, { target = it }, "Hedef kilo", Modifier.weight(1f))
        }

        Text("Ne kadar hareketlisin?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        ACTIVITY_LEVELS.forEach { lvl ->
            SelectCard(
                selected = activity == lvl.factor,
                onClick = { activity = lvl.factor },
                title = lvl.label.substringBefore(" ("),
                subtitle = lvl.label.substringAfter("(", "").removeSuffix(")").ifBlank { null },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Hedefin ne?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { g ->
                val (emoji, delta) = when (g) {
                    Goal.CUT -> "🔥" to "−500 kcal"
                    Goal.MAINTAIN -> "⚖️" to "±0 kcal"
                    Goal.BULK -> "💪" to "+300 kcal"
                }
                val blocked = g == Goal.CUT && !male && lifeStage.isNotEmpty()
                SelectCard(goal == g.name, { if (!blocked) goal = g.name }, "$emoji ${g.label}", Modifier.weight(1f), if (blocked) "kapalı" else delta)
            }
        }
        HealthToggle(health) { health = it }
        SafetyNotice(Calculator.safetyWarnings(male, height.toNum(), weight.toNum(), target.toNum(), Goal.from(goal), lifeStage, health))

        val a = age.toIntOrNull()
        val h = height.toNum()
        val w = weight.toNum()
        val t = target.toNum()
        val valid = a != null && a in 13..100 &&
            h != null && h in 100.0..250.0 &&
            w != null && w in 30.0..300.0 &&
            (target.isBlank() || (t != null && t in 30.0..300.0))

        PrimaryButton(
            "Kaydet ve hesapla",
            onClick = {
                onSave(
                    Profile(
                        male = male, age = a!!, heightCm = h!!, weightKg = w!!,
                        activity = activity, goal = goal, targetWeightKg = t
                    ),
                    if (male) "" else lifeStage, health
                )
            },
            enabled = valid
        )
    }
}


/** Hamilelik / emzirme seçimi (sadece kadınlarda gösterilir). */
@Composable
fun LifeStagePicker(value: String, onChange: (String) -> Unit) {
    Text("Hamile misin ya da emziriyor musun?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectCard(value.isEmpty(), { onChange("") }, "Hayır", Modifier.weight(1f))
        SelectCard(value == "PREGNANT", { onChange("PREGNANT") }, "🤰 Hamileyim", Modifier.weight(1f))
        SelectCard(value == "BREASTFEEDING", { onChange("BREASTFEEDING") }, "🍼 Emziriyorum", Modifier.weight(1f))
    }
}

/** Kronik rahatsızlık işareti. */
@Composable
fun HealthToggle(value: Boolean, onChange: (Boolean) -> Unit) {
    SelectCard(
        value, { onChange(!value) },
        (if (value) "✓ " else "") + "Bir rahatsızlığım var",
        Modifier.fillMaxWidth(),
        "Diyabet, tansiyon, kalp, tiroid ya da düzenli ilaç"
    )
}

/** Turuncu uyarı kartı. */
@Composable
fun SafetyNotice(warnings: List<String>) {
    if (warnings.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = FitColors.Orange.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, FitColors.Orange.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            warnings.forEach { w ->
                Row {
                    Text("⚠️", modifier = Modifier.padding(end = 8.dp))
                    Text(w, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


/** Fotoğraftan çıkan tahminler: istemediğini çıkar, kalanını tek dokunuşla öğüne ekle. */
@Composable
private fun PhotoResultCard(
    r: com.fittakip.app.data.FoodVision.Result, meal: Meal,
    onRemove: (com.fittakip.app.data.FoodVision.Guess) -> Unit, onDismiss: () -> Unit,
    onAddAll: (List<com.fittakip.app.data.FoodVision.Guess>) -> Unit
) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📷 Tabağında gördüklerim", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") }
        }
        r.items.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = MaterialTheme.typography.titleSmall)
                    HintText("${g.portion} · P ${g.p.roundToInt()} · K ${g.c.roundToInt()} · Y ${g.f.roundToInt()}")
                }
                Text("${g.kcal} kcal", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime)
                IconButton(onClick = { onRemove(g) }) { Icon(Icons.Rounded.Close, "${g.name} çıkar") }
            }
        }
        if (r.note.isNotBlank()) HintText("💬 ${r.note}")
        HintText("Bu bir tahmin: porsiyon gözle kestirildiği için ±%20-30 sapabilir. Ekledikten sonra kayda dokunup porsiyonu düzeltebilirsin.")
        if (r.items.isNotEmpty()) PrimaryButton("Hepsini ${meal.label} öğününe ekle · ${r.items.sumOf { it.kcal }} kcal", onClick = { onAddAll(r.items) }, icon = Icons.Rounded.Add)
    }
}


/** Sola kaydırınca sil (altta "Geri al" çıkar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
    })
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val p = state.progress
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = if (state.targetValue == SwipeToDismissBoxValue.EndToStart) 0.85f else 0.35f * p)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(Modifier.padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White)
                }
            }
        }
    ) { content() }
}


/** Sesle söylenenler: "anladıklarım", miktarı ± ile düzelt, istemediğini çıkar, tek dokunuşla ekle. */
@Composable
private fun VoiceResultCard(
    v: com.fittakip.app.domain.VoiceParser.Parsed, meal: Meal,
    onChange: (com.fittakip.app.domain.VoiceParser.Parsed) -> Unit, onDismiss: () -> Unit,
    onAddAll: (List<com.fittakip.app.domain.VoiceParser.Match>) -> Unit
) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎤 Anladıklarım", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") }
        }
        v.matches.forEachIndexed { i, m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.item.name, style = MaterialTheme.typography.titleSmall)
                    HintText("${m.item.portion} · duyduğum: \"${m.heard}\"")
                }
                IconButton(onClick = { onChange(v.copy(matches = v.matches.toMutableList().also { it[i] = m.copy(qty = (m.qty - 0.5).coerceAtLeast(0.5)) })) }) { Text("−") }
                Text(m.qty.toString().removeSuffix(".0").replace('.', ',') + "×", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { onChange(v.copy(matches = v.matches.toMutableList().also { it[i] = m.copy(qty = m.qty + 0.5) })) }) { Text("+") }
                Text("${m.kcal}", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime, modifier = Modifier.widthIn(min = 40.dp))
                IconButton(onClick = { onChange(v.copy(matches = v.matches - m)) }) { Icon(Icons.Rounded.Close, "${m.item.name} çıkar") }
            }
        }
        if (v.unknown.isNotEmpty()) HintText("Anlayamadım: " + v.unknown.joinToString(", ") { "\"$it\"" } + " — bunları arama kutusundan ekleyebilirsin.")
        if (v.matches.isNotEmpty()) PrimaryButton("Hepsini ${meal.label} öğününe ekle · ${v.matches.sumOf { it.kcal }} kcal", onClick = { onAddAll(v.matches) }, icon = Icons.Rounded.Add)
    }
}
