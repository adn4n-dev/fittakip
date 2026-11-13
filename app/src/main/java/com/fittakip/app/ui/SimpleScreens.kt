package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.Meal
import com.fittakip.app.data.counted
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.DayPlan
import com.fittakip.app.domain.Gamification
import com.fittakip.app.domain.Karne
import com.fittakip.app.domain.PlanResolver
import com.fittakip.app.domain.ProgressAnalyzer
import com.fittakip.app.domain.Goal
import com.fittakip.app.domain.Routine
import java.time.LocalTime

/**
 * Sade mod: tek ekran, beş kart, hepsi tek dokunuş.
 * Antrenman · Kalori · Su · Tartı · Haftalık karne. Geri kalan her şey Ayarlar > "Tam uygulama"da.
 */
@Composable
fun SimpleHome(vm: AppViewModel, onWorkout: () -> Unit, onProgress: () -> Unit, onSettings: () -> Unit) {
    val st by vm.settings.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val foods by vm.foods.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val water by vm.water.collectAsStateWithLifecycle()
    val customFoods by vm.customFoods.collectAsStateWithLifecycle()
    val savedMeals by vm.savedMeals.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val p = profile.profile
    val today = vm.today
    LaunchedEffect(Unit) { vm.goToday() }

    var sheet by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf(false) }

    val target = p?.let { Calculator.calculate(it, st).target }
    val todayFoods = foods.filter { it.epochDay == today }
    val eaten = todayFoods.sumOf { it.kcal }
    val protein = todayFoods.sumOf { it.protein }.toInt()
    val proteinGoal = p?.let { Calculator.calculate(it, st).proteinG }
    val drank = water.filter { it.epochDay == today }.sumOf { it.ml }
    val waterGoal = Gamification.waterGoal(p, st)
    val meal = Meal.forHour(LocalTime.now().hour, st.fastingMode)
    val usual = remember(foods, meal, today) {
        Routine.usual(foods, meal, today)?.takeIf { todayFoods.none { f -> Meal.from(f.meal) == meal } }
    }
    val karne = remember(sets, foods, weights, p, st, today) { Karne.compute(sets, foods, weights, p, st, today) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val hi = when (LocalTime.now().hour) { in 5..11 -> "Günaydın"; in 12..17 -> "İyi günler"; else -> "İyi akşamlar" }
                    Text(if (st.userName.isNotBlank()) "$hi, ${st.userName}" else hi, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    HintText(longDayLabel(today))
                }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Ayarlar") }
            }
        }

        // 1) Antrenman
        item {
            val plan = PlanResolver.forDay(st, sets, today)
            val doneToday = sets.count { it.epochDay == today && it.counted() }
            GlowCard {
                val dark = Color(0xFF0E1200)
                when {
                    doneToday > 0 -> {
                        Text("ANTRENMAN", color = dark.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                        Text("Bugün $doneToday set ✅", color = dark, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Button(onClick = onWorkout, colors = ButtonDefaults.buttonColors(containerColor = dark, contentColor = FitColors.Lime), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("Devam et", fontWeight = FontWeight.Bold)
                        }
                    }
                    plan is DayPlan.Workout -> {
                        Text("BUGÜN", color = dark.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                        Text("${plan.template.name} 💪", color = dark, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp))
                        Text("${plan.template.items.size} hareket · ${plan.template.items.sumOf { it.sets }} set", color = dark.copy(alpha = 0.7f))
                        Button(
                            onClick = { vm.startTodayWorkout(); onWorkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = dark, contentColor = FitColors.Lime),
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text("▶  Başla", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
                    }
                    else -> {
                        Text("BUGÜN", color = dark.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                        Text("Dinlenme günü 😌", color = dark, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        val next = (1..7).firstNotNullOfOrNull { i -> (PlanResolver.forDay(st, sets, today + i) as? DayPlan.Workout)?.let { i to it.template.name } }
                        next?.let { (i, n) -> Text(if (i == 1) "Yarın: $n" else "Sıradaki: $n", color = dark.copy(alpha = 0.75f)) }
                        if (!st.hasPlan) TextButton(onClick = onSettings) { Text("Programını kur", color = dark, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // 2) Kalori
        item {
            FitCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$eaten", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(target?.let { " / $it kcal" } ?: " kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                    Spacer(Modifier.weight(1f))
                    if (proteinGoal != null) HintText("protein $protein/$proteinGoal g")
                }
                if (target != null) ProgressBar(eaten.toFloat() / target, if (eaten > target * 1.05) FitColors.Orange else FitColors.Lime)
                usual?.let { u ->
                    Surface(
                        onClick = { vm.goToday(); u.items.forEach { e -> vm.addFood(e.name, e.kcal, e.protein, e.carb, e.fat, meal) } },
                        shape = RoundedCornerShape(16.dp), color = FitColors.Lime.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${meal.emoji} Her zamanki ${Meal.yours(meal)}", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime)
                                HintText("${u.label} · ${u.kcal} kcal")
                            }
                            Text("✓ Ekle", color = FitColors.Lime, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Yemek ekle", onClick = { vm.goToday(); voice = false; sheet = true }, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { vm.goToday(); voice = true; sheet = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Rounded.Mic, "Sesle ekle", modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        // 3) Su
        item {
            FitCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("💧 ${"%.1f".format(drank / 1000.0)} / ${"%.1f".format(waterGoal / 1000.0)} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(drank.toFloat() / waterGoal, Color(0xFF6FB6FF))
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(onClick = { vm.goToday(); vm.addWater(250) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) { Text("+250 ml", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 4) Tartı
        item {
            val todayW = weights.filter { it.epochDay == today }.maxByOrNull { it.id }
            val last = weights.maxByOrNull { it.epochDay * 100_000 + it.id }?.kg ?: p?.weightKg ?: 75.0
            var kg by remember(last) { mutableDoubleStateOf(Math.round(last * 10) / 10.0) }
            val trend = remember(weights, p) { ProgressAnalyzer.analyzeWeight(weights, p?.targetWeightKg, Goal.from(p?.goal)) }
            FitCard {
                if (todayW != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚖️ Bugün ${"%.1f".format(todayW.kg)} kg ✓", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        trend?.kgPerWeek?.let { HintText((if (it >= 0) "+" else "") + "%.1f".format(it) + " kg/hafta") }
                    }
                } else {
                    Text("⚖️ Bugün tartıldın mı?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { kg = Math.round((kg - 0.1) * 10) / 10.0 }, shape = RoundedCornerShape(14.dp)) { Text("−", fontSize = 22.sp) }
                        Text("%.1f kg".format(kg), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        FilledTonalButton(onClick = { kg = Math.round((kg + 0.1) * 10) / 10.0 }, shape = RoundedCornerShape(14.dp)) { Text("+", fontSize = 22.sp) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { vm.addWeight(kg) }, shape = RoundedCornerShape(14.dp)) { Text("✓") }
                    }
                }
            }
        }

        // 5) Karne
        item {
            Surface(onClick = onProgress, shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("İLERLİYOR MUYUM?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
                    Text(karne.headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    HintText(karne.summary)
                    Text(karne.tip, style = MaterialTheme.typography.bodyMedium)
                    Text("Detay ›", color = FitColors.Lime, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (sheet) {
        AddFoodSheet(
            fasting = st.fastingMode,
            history = foods,
            customFoods = customFoods,
            savedMeals = savedMeals,
            onSavedMeal = { sm, m -> vm.addSavedMeal(sm, m) },
            onDeleteSavedMeal = { vm.deleteSavedMeal(it) },
            favorites = st.favorites,
            onToggleFavorite = { vm.toggleFavorite(it) },
            onSaveCustom = { n, k, pr, cb, ft -> vm.saveCustomFood(n, k, pr, cb, ft) },
            onDeleteCustom = { vm.deleteCustomFood(it) },
            startVoice = voice,
            today = today,
            onDismiss = { sheet = false; voice = false }
        ) { name, kcal, pr, cb, ft, m -> vm.addFood(name, kcal, pr, cb, ft, m) }
    }
}

/** Sade ilerleme: karne + kilo grafiği + programdaki hareketlerin değişimi. */
@Composable
fun SimpleProgress(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.settings.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val foods by vm.foods.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val p = profile.profile
    val today = vm.today
    val karne = remember(sets, foods, weights, p, st) { Karne.compute(sets, foods, weights, p, st, today) }
    val trend = remember(weights, p) { ProgressAnalyzer.analyzeWeight(weights, p?.targetWeightKg, Goal.from(p?.goal)) }
    val bw = weights.maxByOrNull { it.epochDay }?.kg ?: p?.weightKg ?: 0.0
    val lifts = remember(sets, st) {
        val split = com.fittakip.app.domain.Coach.splitByContext(sets, st.planTemplates)
        ProgressAnalyzer.exercises(split, bw).filter { e ->
            st.planTemplates.any { t -> t.items.any { e.exercise.startsWith(it.exercise, true) } }
        }.sortedByDescending { it.weeklyChangePct }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader("İlerliyor muyum?", onBack) }
        item {
            GlowCard {
                val dark = Color(0xFF0E1200)
                Text("SON 7 GÜN", color = dark.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                Text(karne.headline, color = dark, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(karne.summary, color = dark.copy(alpha = 0.8f))
                Text(karne.tip, color = dark, fontWeight = FontWeight.SemiBold)
            }
        }
        if (trend != null && trend.history.size >= 2) item {
            FitCard {
                Text("⚖️ Kilo", style = MaterialTheme.typography.titleMedium)
                TrendChart(trend.history.takeLast(60), trend.fit, trend.fitStartDay)
                HintText(trend.kgPerWeek?.let { "Gidiş: " + (if (it >= 0) "+" else "") + "%.2f".format(it) + " kg/hafta" } ?: "Biraz daha tartı kaydı gerekli.")
            }
        }
        item {
            FitCard {
                Text("🏋️ Hareketlerin", style = MaterialTheme.typography.titleMedium)
                if (lifts.isEmpty()) HintText("Programındaki hareketlerden birkaç antrenman girince burada gelişimin görünür.")
                lifts.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.exercise, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        val pct = e.weeklyChangePct
                        Text(
                            when { e.history.size < 3 -> "veri az"; pct >= 0.5 -> "↑ %" + "%.1f".format(pct) + "/hf"; pct <= -0.5 -> "↓ %" + "%.1f".format(-pct) + "/hf"; else -> "→ sabit" },
                            color = when { e.history.size < 3 -> MaterialTheme.colorScheme.onSurfaceVariant; pct >= 0.5 -> FitColors.Lime; pct <= -0.5 -> FitColors.Orange; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
