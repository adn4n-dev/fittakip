package com.fittakip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("Kalori") }
        if (!state.loaded) return@LazyColumn

        val p = state.profile
        if (p == null || editing) {
            item {
                ProfileForm(p) {
                    vm.saveProfile(it)
                    editing = false
                }
            }
        } else {
            val result = Calculator.calculate(p)
            item { SummaryCard(p, result, foods) { editing = true } }
            item { AddFoodCard { name, kcal, pr, cb, ft -> vm.addFood(name, kcal, pr, cb, ft) } }
            if (foods.isNotEmpty()) item { Text("Bugün yediklerin", style = MaterialTheme.typography.titleSmall) }
            items(foods, key = { it.id }) { f -> FoodRow(f) { vm.deleteFood(f) } }
        }
    }
}

@Composable
private fun ProfileForm(initial: Profile?, onSave: (Profile) -> Unit) {
    var male by rememberSaveable { mutableStateOf(initial?.male ?: true) }
    var age by rememberSaveable { mutableStateOf(initial?.age?.toString() ?: "") }
    var height by rememberSaveable { mutableStateOf(initial?.heightCm?.clean() ?: "") }
    var weight by rememberSaveable { mutableStateOf(initial?.weightKg?.clean() ?: "") }
    var target by rememberSaveable { mutableStateOf(initial?.targetWeightKg?.clean() ?: "") }
    var activity by rememberSaveable { mutableDoubleStateOf(initial?.activity ?: 1.55) }
    var goal by rememberSaveable { mutableStateOf(initial?.goal ?: Goal.MAINTAIN.name) }

    SectionCard(if (initial == null) "Önce seni tanıyalım" else "Profili düzenle") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = male, onClick = { male = true }, label = { Text("Erkek") })
            FilterChip(selected = !male, onClick = { male = false }, label = { Text("Kadın") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(age, { age = it }, "Yaş", Modifier.weight(1f))
            NumField(height, { height = it }, "Boy (cm)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(weight, { weight = it }, "Kilo (kg)", Modifier.weight(1f))
            NumField(target, { target = it }, "Hedef kilo (ops.)", Modifier.weight(1f))
        }

        Text("Aktivite seviyesi", style = MaterialTheme.typography.labelLarge)
        ACTIVITY_LEVELS.forEach { lvl ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { activity = lvl.factor },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = activity == lvl.factor, onClick = { activity = lvl.factor })
                Text(lvl.label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Hedefin", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { g ->
                FilterChip(selected = goal == g.name, onClick = { goal = g.name }, label = { Text(g.label) })
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

        Button(
            onClick = {
                onSave(
                    Profile(
                        male = male, age = a!!, heightCm = h!!, weightKg = w!!,
                        activity = activity, goal = goal, targetWeightKg = t
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Kaydet ve hesapla") }
    }
}

@Composable
private fun SummaryCard(p: Profile, r: CalorieResult, foods: List<FoodEntry>, onEdit: () -> Unit) {
    val eaten = foods.sumOf { it.kcal }
    val protein = foods.sumOf { it.protein }
    val carb = foods.sumOf { it.carb }
    val fat = foods.sumOf { it.fat }
    val remaining = r.target - eaten

    SectionCard("Bugün") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBox("Hedef", "${r.target}")
            StatBox("Yenen", "$eaten")
            StatBox(if (remaining >= 0) "Kalan" else "Fazla", "${kotlin.math.abs(remaining)}")
        }
        LinearProgressIndicator(
            progress = { (eaten.toFloat() / r.target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        MacroLine("Protein", protein, r.proteinG)
        MacroLine("Karbonhidrat", carb, r.carbG)
        MacroLine("Yağ", fat, r.fatG)
        HintText(
            "BMR ${r.bmr} kcal · Günlük harcama (TDEE) ${r.tdee} kcal · " +
                "Hedef: ${Goal.from(p.goal).label} · ${p.weightKg.clean()} kg"
        )
        TextButton(onClick = onEdit) { Text("Profili düzenle") }
    }
}

@Composable
private fun MacroLine(label: String, eaten: Double, target: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${eaten.roundToInt()} / $target g", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddFoodCard(onAdd: (String, Int, Double, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }

    SectionCard("Yemek ekle") {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Ne yedin?") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(kcal, { kcal = it }, "Kalori (kcal)", Modifier.weight(1f))
            NumField(protein, { protein = it }, "Protein (g)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(carb, { carb = it }, "Karb. (g)", Modifier.weight(1f))
            NumField(fat, { fat = it }, "Yağ (g)", Modifier.weight(1f))
        }

        val p = protein.toNum() ?: 0.0
        val c = carb.toNum() ?: 0.0
        val f = fat.toNum() ?: 0.0
        // Kalori boş bırakılırsa makrolardan hesaplanır (4/4/9)
        val k = kcal.toNum() ?: (p * 4 + c * 4 + f * 9).takeIf { it > 0 }
        if (kcal.isBlank() && k != null) HintText("Makrolardan hesaplanan: ${k.roundToInt()} kcal")

        Button(
            onClick = {
                onAdd(name.trim(), k!!.roundToInt(), p, c, f)
                name = ""; kcal = ""; protein = ""; carb = ""; fat = ""
            },
            enabled = name.isNotBlank() && k != null && k >= 0,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ekle") }
    }
}

@Composable
private fun FoodRow(f: FoodEntry, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(f.name) },
        supportingContent = {
            Text("P ${f.protein.roundToInt()}g · K ${f.carb.roundToInt()}g · Y ${f.fat.roundToInt()}g")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${f.kcal} kcal")
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Sil") }
            }
        }
    )
}
