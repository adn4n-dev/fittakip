package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
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
import com.fittakip.app.data.FoodEntry
import com.fittakip.app.data.Profile
import com.fittakip.app.domain.ACTIVITY_LEVELS
import com.fittakip.app.domain.CalorieResult
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Goal
import kotlin.math.roundToInt

@Composable
fun CalorieScreen(vm: AppViewModel) {
    val state by vm.profile.collectAsStateWithLifecycle()
    val foods by vm.todayFoods.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Kalori", longDayLabel(vm.today)) }
        if (!state.loaded) return@LazyColumn

        val p = state.profile
        if (p == null || editing) {
            item {
                ProfileForm(
                    initial = p,
                    onCancel = if (p != null) { { editing = false } } else null
                ) {
                    vm.saveProfile(it)
                    editing = false
                }
            }
        } else {
            val result = Calculator.calculate(p)
            item { HeroCard(p, result, foods) { editing = true } }
            item {
                if (adding) {
                    AddFoodCard(onClose = { adding = false }) { name, kcal, pr, cb, ft ->
                        vm.addFood(name, kcal, pr, cb, ft)
                    }
                } else {
                    PrimaryButton("Yemek ekle", onClick = { adding = true }, icon = Icons.Rounded.Add)
                }
            }
            if (foods.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Restaurant,
                        "Bugün henüz bir şey eklemedin",
                        "Yediklerini ekledikçe kalan kalorin ve makroların burada güncellenecek."
                    )
                }
            } else {
                item {
                    Text(
                        "Bugün yediklerin",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(foods, key = { it.id }) { f -> FoodRow(f) { vm.deleteFood(f) } }
            }
        }
    }
}

@Composable
private fun HeroCard(p: Profile, r: CalorieResult, foods: List<FoodEntry>, onEdit: () -> Unit) {
    val eaten = foods.sumOf { it.kcal }
    val protein = foods.sumOf { it.protein }
    val carb = foods.sumOf { it.carb }
    val fat = foods.sumOf { it.fat }

    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bugünkü durum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
private fun ProfileForm(initial: Profile?, onCancel: (() -> Unit)?, onSave: (Profile) -> Unit) {
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
            HintText("Günlük kalori ve makro hedefini bu bilgilerle hesaplıyorum. Sonra istediğin zaman değiştirebilirsin.")
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

@Composable
private fun AddFoodCard(onClose: () -> Unit, onAdd: (String, Int, Double, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }

    SectionCard(
        "Yemek ekle",
        icon = Icons.Rounded.Restaurant,
        action = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Kapat") } }
    ) {
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
        // Kalori boş bırakılırsa makrolardan hesaplanır (4/4/9)
        val k = kcal.toNum() ?: (p * 4 + c * 4 + f * 9).takeIf { it > 0 }
        if (kcal.isBlank() && k != null) {
            Pill("Makrolardan: ${k.roundToInt()} kcal", MaterialTheme.colorScheme.primary)
        } else if (kcal.isBlank()) {
            HintText("Kaloriyi bilmiyorsan boş bırak, protein/karb/yağdan hesaplarım.")
        }

        PrimaryButton(
            "Ekle",
            onClick = {
                onAdd(name.trim(), k!!.roundToInt(), p, c, f)
                name = ""; kcal = ""; protein = ""; carb = ""; fat = ""
            },
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
