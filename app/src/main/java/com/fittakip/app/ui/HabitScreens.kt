package com.fittakip.app.ui

import com.fittakip.app.data.counted
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
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

val CHECKIN_OPTIONS = listOf(
    "SLEEP" to "😴 Uyku", "MOOD" to "🙂 Ruh hali", "STEPS" to "👟 Adım",
    "SUPP" to "💊 Takviye", "NOTE" to "📝 Not"
)

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackHeader("Günlük check-in", onBack) }
        item {
            FitCard {
                Text("Neleri takip etmek istersin?", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHECKIN_OPTIONS.forEach { (key, label) ->
                        val on = key in settings.checkinItems
                        Pill(if (on) "✓ $label" else label, if (on) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = {
                            vm.updateSettings { st -> st.copy(checkinItems = if (on) st.checkinItems - key else st.checkinItems + key) }
                        })
                    }
                }
                HintText("Az ama düzenli kayıt, çok ama yarım kalan kayıttan iyidir. Başlangıç için sadece uykuyu öneririm.")
            }
        }

        if ("SLEEP" in settings.checkinItems) item {
            SectionCard("Uyku", icon = Icons.Rounded.Bedtime) {
                Stepper(value("SLEEP")?.clean() ?: "7", { v -> v.toNum()?.let { if (it in 0.0..16.0) vm.setHabit("SLEEP", it) } }, "Dün gece kaç saat uyudun?", 0.5)
                val week = (6 downTo 0).map { today - it }
                val vals = week.map { d -> habits.lastOrNull { it.epochDay == d && it.type == "SLEEP" }?.value ?: 0.0 }
                if (vals.any { it > 0 }) {
                    BarChart(vals, week.map { shortDayName(it) }, FitColors.Blue, 8.0)
                    HintText("Son 7 gün · turuncu çizgi 8 saat")
                }
            }
        }

        if ("MOOD" in settings.checkinItems) item {
            SectionCard("Ruh hali", icon = Icons.Rounded.Mood) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MOODS.forEachIndexed { i, e ->
                        SelectCard(value("MOOD")?.toInt() == i + 1, { vm.setHabit("MOOD", (i + 1).toDouble()) }, e, Modifier.weight(1f))
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

        if ("STEPS" in settings.checkinItems) item {
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

        if ("SUPP" in settings.checkinItems) item {
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

        if ("NOTE" in settings.checkinItems) item {
            SectionCard("Günlük not", icon = Icons.Rounded.EditNote) {
                TextInput(note, { note = it }, "Bugün nasıl geçti? Ne hissettin?", singleLine = false)
                PrimaryButton("Notu kaydet", onClick = { vm.setHabit("NOTE", 1.0, note.trim()) }, enabled = note.isNotBlank() && note != noteToday)
                val past = habits.filter { it.type == "NOTE" && it.epochDay < today }.takeLast(3).reversed()
                past.forEach { HintText("${dayLabel(it.epochDay)}: ${it.text}") }
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
    val work = list.filter { it.counted() }
    val bw = state.profile?.weightKg ?: 0.0
    val progress = remember(work, bw) { if (work.isEmpty()) null else com.fittakip.app.domain.ProgressAnalyzer.analyzeExercise(work, bw) }
    val prs = remember(sets, name, bw) { Gamification.prDays(sets, bw).filter { it.second.equals(name, true) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                com.fittakip.app.data.FORM_TIPS[com.fittakip.app.data.EXERCISES.firstOrNull { it.name.equals(name, true) }?.name ?: name]?.let {
                    HintText("💡 Form: $it")
                }
            }
        }
        item {
            var range by remember { mutableIntStateOf(0) }
            val days = listOf(0, 30, 90, 180)
            SectionCard("Gelişim") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Tümü", "1 ay", "3 ay", "6 ay").forEachIndexed { i, l -> SelectCard(range == i, { range = i }, l, Modifier.weight(1f)) }
                }
                val h = if (range == 0) prog.history else prog.history.filter { it.first >= vm.today - days[range] }
                if (h.size >= 2) {
                    TrendChart(h, if (range == 0 || range == 2) prog.fit else null, prog.fitStartDay)
                    val change = h.last().second - h.first().second
                    HintText("Bu aralıkta tahmini 1RM: ${h.first().second.fmt()} → ${h.last().second.fmt()} kg (${change.signed()} kg, %${(change / h.first().second * 100).signed()})")
                } else HintText("Bu aralıkta yeterli kayıt yok.")
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
