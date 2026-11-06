package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.Gamification
import com.fittakip.app.domain.Stats
import com.fittakip.app.domain.Totals
import kotlin.math.roundToInt

@Composable
fun BackHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

private val MOODS = listOf("😫", "😕", "😐", "🙂", "😄")

// ---------------- Günlük check-in ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val habits by vm.habits.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = vm.today
    fun value(type: String) = habits.lastOrNull { it.epochDay == today && it.type == type }?.value
    val noteToday = habits.lastOrNull { it.epochDay == today && it.type == "NOTE" }?.text ?: ""
    var note by rememberSaveable(noteToday) { mutableStateOf(noteToday) }
    var steps by rememberSaveable { mutableStateOf(value("STEPS")?.toInt()?.toString() ?: "") }
    var newSupp by rememberSaveable { mutableStateOf("") }
    val takenSupps = habits.filter { it.epochDay == today && it.type == "SUPP" }.map { it.text }.toSet()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackHeader("Günlük check-in", onBack) }

        item {
            SectionCard("Uyku", icon = Icons.Rounded.Bedtime) {
                Stepper(value("SLEEP")?.clean() ?: "7", { v -> v.toNum()?.let { if (it in 0.0..16.0) vm.setHabit("SLEEP", it) } }, "Dün gece kaç saat uyudun?", 0.5)
                Text("Kalitesi", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { q ->
                        SelectCard(value("SLEEP_Q")?.toInt() == q, { vm.setHabit("SLEEP_Q", q.toDouble()) }, "★".repeat(q), Modifier.weight(1f))
                    }
                }
                val week = (6 downTo 0).map { today - it }
                val vals = week.map { d -> habits.lastOrNull { it.epochDay == d && it.type == "SLEEP" }?.value ?: 0.0 }
                if (vals.any { it > 0 }) {
                    BarChart(vals, week.map { shortDayName(it) }, FitColors.Blue, 8.0)
                    HintText("Son 7 gün · turuncu çizgi 8 saat")
                }
            }
        }

        item {
            SectionCard("Ruh hali ve enerji", icon = Icons.Rounded.Mood) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MOODS.forEachIndexed { i, e ->
                        SelectCard(value("MOOD")?.toInt() == i + 1, { vm.setHabit("MOOD", (i + 1).toDouble()) }, e, Modifier.weight(1f))
                    }
                }
                Text("Enerji", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { q ->
                        SelectCard(value("ENERGY")?.toInt() == q, { vm.setHabit("ENERGY", q.toDouble()) }, "⚡".repeat(q.coerceAtMost(3)) + if (q > 3) "+" else "", Modifier.weight(1f), "$q")
                    }
                }
                val moods = (6 downTo 0).map { d -> habits.lastOrNull { it.epochDay == today - d && it.type == "MOOD" }?.value?.toInt() }
                if (moods.any { it != null }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        moods.forEachIndexed { i, m ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m?.let { MOODS[it - 1] } ?: "·")
                                HintText(shortDayName(today - 6 + i))
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Adım", icon = Icons.Rounded.DirectionsWalk) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField(steps, { steps = it }, "Bugünkü adım", Modifier.weight(1f))
                    Button(onClick = { steps.toNum()?.let { vm.setHabit("STEPS", it) } }, enabled = steps.toNum() != null) { Text("Kaydet") }
                }
                value("STEPS")?.let {
                    ProgressBar((it / 10_000).toFloat(), FitColors.Lime)
                    HintText("${it.toInt()} / 10.000 adım · ≈ ${(it * 0.04).roundToInt()} kcal yaktın")
                }
                HintText("Telefonunun sağlık uygulamasındaki adım sayısını buraya gir.")
            }
        }

        item {
            SectionCard("Kafein", icon = Icons.Rounded.Coffee) {
                val cups = value("CAFFEINE")?.toInt() ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☕".repeat(cups.coerceAtMost(8)).ifEmpty { "Bugün kahve/çay yok" }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.addCaffeine(-1) }, enabled = cups > 0) { Text("−", style = MaterialTheme.typography.headlineMedium) }
                    IconButton(onClick = { vm.addCaffeine(1) }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                }
                HintText(
                    when {
                        cups >= 5 -> "Günde ~400 mg üstü kafein uykunu bozabilir. Öğleden sonra azaltmayı dene."
                        else -> "$cups fincan · ≈ ${cups * 80} mg kafein"
                    }
                )
            }
        }

        item {
            SectionCard("Takviyeler", icon = Icons.Rounded.Medication) {
                if (settings.supplements.isEmpty()) HintText("Listende takviye yok. Aşağıdan ekleyebilirsin.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.supplements.sorted().forEach { s ->
                        val on = s in takenSupps
                        Pill(if (on) "✓ $s" else s, if (on) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { vm.toggleSupplement(s) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput(newSupp, { newSupp = it }, "Yeni takviye", Modifier.weight(1f))
                    TextButton(onClick = {
                        val n = newSupp.trim()
                        if (n.isNotEmpty()) vm.updateSettings { it.copy(supplements = it.supplements + n) }
                        newSupp = ""
                    }) { Text("Ekle") }
                }
                if (settings.supplements.isNotEmpty()) HintText("Çıkarmak için Ayarlar > Takviye listesi.")
            }
        }

        item {
            SectionCard("Günlük not", icon = Icons.Rounded.EditNote) {
                TextInput(note, { note = it }, "Bugün nasıl geçti? Ne hissettin?", singleLine = false)
                PrimaryButton("Notu kaydet", onClick = { vm.setHabit("NOTE", 1.0, note.trim()) }, enabled = note.isNotBlank() && note != noteToday)
                val past = habits.filter { it.type == "NOTE" && it.epochDay < today }.takeLast(3).reversed()
                past.forEach { HintText("${dayLabel(it.epochDay)}: ${it.text}") }
            }
        }
    }
}

// ---------------- Tüm zamanlar istatistikleri ----------------

@Composable
fun StatsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val g = game ?: return
    var month by rememberSaveable { mutableStateOf(false) }
    val t = if (month) g.thisMonth else g.allTime
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader("İstatistikler", onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectCard(!month, { month = false }, "Tüm zamanlar", Modifier.weight(1f))
                SelectCard(month, { month = true }, "Bu ay", Modifier.weight(1f))
            }
        }
        item { StatsGrid(t, g.xp, g.longestStreak) }
    }
}

@Composable
private fun StatsGrid(t: Totals, xp: Int, longest: Int) {
    val rows = listOf(
        "Aktif gün" to "${t.activeDays}",
        "Antrenman günü" to "${t.workoutDays}",
        "Set" to "${t.sets}",
        "Kaldırılan" to "${(t.volume / 1000).fmt()} ton",
        "Kişisel rekor" to "${t.prs}",
        "Yemek kaydı" to "${t.foods}",
        "İçilen su" to "${t.waterL.fmt()} L",
        "Kardiyo" to "${t.cardioMin} dk",
        "Oruç" to "${t.fasts} · en uzun ${t.longestFastH.fmt()} sa",
        "Ortalama uyku" to (t.avgSleep?.let { "${it.fmt()} sa" } ?: "—"),
        "Kronometreli antrenman" to "${t.sessions}" + (t.avgSessionMin?.let { " · ort. $it dk" } ?: ""),
        "En uzun seri" to "$longest gün",
        "Toplam XP" to "$xp"
    )
    FitCard {
        rows.forEach { (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(k, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(v, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

// ---------------- Hareket detayı ----------------

@Composable
fun ExerciseDetailScreen(vm: AppViewModel, onBack: () -> Unit) {
    val name by vm.detailExercise.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val list = remember(sets, name) { sets.filter { it.exercise.trim().equals(name, true) } }
    val work = list.filter { !it.warmup }
    val progress = remember(work) { if (work.isEmpty()) null else com.fittakip.app.domain.ProgressAnalyzer.analyzeExercise(work) }
    val prs = remember(sets, name) { Gamification.prDays(sets).filter { it.second.equals(name, true) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            BackHeader(name, onBack, action = {
                IconButton(onClick = { vm.share(vm.workoutShareText(vm.today)) }) { Icon(Icons.Rounded.Share, "Paylaş") }
            })
        }
        if (work.isEmpty()) {
            item { HintText("Bu hareket için henüz set yok.") }
            return@LazyColumn
        }
        val prog = progress ?: return@LazyColumn
        val heaviest = work.maxBy { it.weightKg }
        val mostReps = work.maxBy { it.reps }
        val bestSession = work.groupBy { it.epochDay }.maxBy { e -> e.value.sumOf { it.weightKg * it.reps } }
        item {
            FitCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("Tahmini 1RM", "${prog.best.fmt()} kg", Modifier.weight(1f), FitColors.Lime)
                    MetricTile("En ağır", "${heaviest.weightKg.clean()} × ${heaviest.reps}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("En çok tekrar", "${mostReps.reps} @ ${mostReps.weightKg.clean()}", Modifier.weight(1f))
                    MetricTile("En iyi seans", "${bestSession.value.sumOf { it.weightKg * it.reps }.roundToInt()} kg", Modifier.weight(1f))
                }
                HintText("${work.map { it.epochDay }.toSet().size} antrenman · ${work.size} set · ${prs.size} rekor")
                state.profile?.let { p ->
                    com.fittakip.app.domain.Calculator.strengthLevel(name, prog.best, p.weightKg, p.male)?.let { (lvl, f) ->
                        Text("Güç seviyesi: $lvl", style = MaterialTheme.typography.titleSmall, color = FitColors.Lime)
                        ProgressBar(f, FitColors.Lime)
                        HintText("Vücut ağırlığının ${(prog.best / p.weightKg).fmt(2)} katı")
                    }
                }
                com.fittakip.app.data.FORM_TIPS[com.fittakip.app.data.EXERCISES.firstOrNull { it.name.equals(name, true) }?.name ?: name]?.let {
                    HintText("💡 Form: $it")
                }
            }
        }
        item {
            SectionCard("Gelişim") {
                TrendChart(prog.history, prog.fit, prog.fitStartDay)
            }
        }
        if (prs.isNotEmpty()) {
            item {
                SectionCard("Rekor günleri 🏆") {
                    prs.reversed().take(8).forEach { (d, _) ->
                        val best = work.filter { it.epochDay == d }.maxOf { Stats.epley1RM(it.weightKg, it.reps) }
                        Row { Text(dayLabel(d, true), Modifier.weight(1f)); Text("${best.fmt()} kg", color = FitColors.Orange) }
                    }
                }
            }
        }
        item {
            SectionCard("Tüm seanslar") {
                list.groupBy { it.epochDay }.toSortedMap(compareByDescending { it }).forEach { (d, l) ->
                    Column {
                        Text(dayLabel(d, true), style = MaterialTheme.typography.titleSmall)
                        HintText(l.reversed().joinToString("  ·  ") {
                            (if (it.warmup) "ı " else "") + "${it.weightKg.clean()}×${it.reps}" + (it.rpe?.let { r -> " @${r.clean()}" } ?: "")
                        })
                    }
                }
            }
        }
    }
}
