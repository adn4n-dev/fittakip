package com.fittakip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
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
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }

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
                ProfileForm(initial = p, onCancel = if (p != null) { { editing = false } } else null) {
                    vm.saveProfile(it)
                    editing = false
                }
            }
            return@LazyColumn
        }

        val result = Calculator.calculate(p, settings.kcalAdjust)
        if (day != vm.today) {
            item {
                Pill(
                    "Geçmiş gün: ${dayLabel(day, withYear = true)} · bugüne dön", FitColors.Blue,
                    onClick = { vm.shiftDay((vm.today - day).toInt()) }
                )
            }
        }
        item { HeroCard(p, result, foods, settings.kcalAdjust) { editing = true } }
        item { WaterCard(dayWater, waterGoal, { vm.addWater(it) }, { vm.undoWater() }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Yemek ekle", onClick = { sheet = true }, icon = Icons.Rounded.Add, modifier = Modifier.weight(1f))
                SecondaryButton("Dünü kopyala", onClick = { vm.copyPreviousDay() }, icon = Icons.Rounded.ContentCopy)
            }
        }

        if (foods.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Restaurant,
                    if (day == vm.today) "Bugün henüz bir şey eklemedin" else "Bu gün için kayıt yok",
                    "80'den fazla hazır yemek içinden ara ya da kendi değerlerini gir."
                )
            }
        }
        Meal.entries.forEach { m ->
            val list = foods.filter { Meal.from(it.meal) == m }
            if (list.isNotEmpty()) {
                item(key = "meal-${m.name}") {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${m.emoji} ${m.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        HintText("${list.sumOf { it.kcal }} kcal")
                    }
                }
                items(list, key = { it.id }) { f -> FoodRow(f) { vm.deleteFood(f) } }
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
        AddFoodSheet(allFoods, onDismiss = { sheet = false }) { name, kcal, pr, cb, ft, meal ->
            vm.addFood(name, kcal, pr, cb, ft, meal)
        }
    }
}

@Composable
private fun HeroCard(p: Profile, r: CalorieResult, foods: List<FoodEntry>, adjust: Int, onEdit: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CalorieRing(eaten, r.target)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniStat(Icons.Rounded.Flag, "Hedef", "${r.target} kcal", FitColors.Lime)
                MiniStat(Icons.Rounded.Restaurant, "Yenen", "$eaten kcal", FitColors.Orange)
                MiniStat(Icons.Rounded.LocalFireDepartment, "Günlük harcama", "${r.tdee} kcal", FitColors.Blue)
            }
        }
        MacroBar("Protein", protein, r.proteinG, FitColors.Blue)
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
                Icon(Icons.Rounded.Undo, "Geri al")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    history: List<FoodEntry>,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Double, Double, Double, Meal) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var meal by remember { mutableStateOf(Meal.forHour(LocalTime.now().hour)) }
    var query by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var mult by remember { mutableDoubleStateOf(1.0) }
    val frequent = remember(history) {
        history.groupBy { it.name.trLower() }.values.sortedByDescending { it.size }.take(8).map { it.first() }
    }
    val results = remember(query) {
        val q = query.trim().trLower()
        if (q.isEmpty()) FOOD_DB.take(20) else FOOD_DB.filter { it.name.trLower().contains(q) }
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
                    Meal.entries.forEach { m ->
                        SelectCard(meal == m, { meal = m }, "${m.emoji}", Modifier.weight(1f), m.label)
                    }
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
                    ManualFoodForm { n, k, pr, cb, ft ->
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
                        placeholder = { Text("Ara: tavuk, simit, mercimek…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isBlank() && frequent.isNotEmpty()) {
                    item { Text("Sık yediklerin · tek dokunuşla ekle", style = MaterialTheme.typography.titleSmall) }
                    items(frequent, key = { "f-" + it.id }) { f ->
                        PickRow(f.name, "P ${f.protein.roundToInt()} · K ${f.carb.roundToInt()} · Y ${f.fat.roundToInt()}", f.kcal) {
                            onAdd(f.name, f.kcal, f.protein, f.carb, f.fat, meal)
                            onDismiss()
                        }
                    }
                    item { Text("Hazır liste", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp)) }
                }
                if (results.isEmpty()) {
                    item { HintText("\"$query\" bulunamadı. \"Elle gir\" ile kendin ekleyebilirsin.") }
                }
                items(results, key = { "d-" + it.name }) { item ->
                    PickRow(item.name, item.portion, item.kcal) { picked = item; mult = 1.0 }
                }
            }
        }
    }
}

@Composable
private fun PickRow(title: String, subtitle: String, kcal: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HintText(subtitle)
            }
            Text("$kcal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(" kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ManualFoodForm(onAdd: (String, Int, Double, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextInput(name, { name = it }, "Ne yedin?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(kcal, { kcal = it }, "Kalori", Modifier.weight(1f))
            NumField(protein, { protein = it }, "Protein (g)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(carb, { carb = it }, "Karb. (g)", Modifier.weight(1f))
            NumField(fat, { fat = it }, "Yağ (g)", Modifier.weight(1f))
        }
        val p = protein.toNum() ?: 0.0
        val c = carb.toNum() ?: 0.0
        val f = fat.toNum() ?: 0.0
        val k = kcal.toNum() ?: (p * 4 + c * 4 + f * 9).takeIf { it > 0 }
        if (kcal.isBlank() && k != null) {
            Pill("Makrolardan: ${k.roundToInt()} kcal", MaterialTheme.colorScheme.primary)
        } else if (kcal.isBlank()) {
            HintText("Kaloriyi bilmiyorsan boş bırak, protein/karb/yağdan hesaplarım.")
        }
        PrimaryButton(
            "Ekle",
            onClick = { onAdd(name.trim(), k!!.roundToInt(), p, c, f) },
            enabled = name.isNotBlank() && k != null && k >= 0,
            icon = Icons.Rounded.Add
        )
    }
}

@Composable
private fun FoodRow(f: FoodEntry, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
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
fun ProfileForm(initial: Profile?, onCancel: (() -> Unit)?, onSave: (Profile) -> Unit) {
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
            SelectCard(male, { male = true }, "Erkek", Modifier.weight(1f))
            SelectCard(!male, { male = false }, "Kadın", Modifier.weight(1f))
        }
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
                SelectCard(goal == g.name, { goal = g.name }, "$emoji ${g.label}", Modifier.weight(1f), delta)
            }
        }

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
                    )
                )
            },
            enabled = valid
        )
    }
}
