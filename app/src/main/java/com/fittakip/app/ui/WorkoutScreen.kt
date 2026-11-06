package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.CARDIO_TYPES
import com.fittakip.app.data.CardioEntry
import com.fittakip.app.data.EXERCISES
import com.fittakip.app.data.TEMPLATES
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.data.muscleOf
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Stats
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val cardio by vm.cardio.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val restEnd by vm.restEnd.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()

    val names = remember(sets) {
        (sets.map { it.exercise.trim() } + EXERCISES.map { it.name }).distinctBy { it.lowercase() }
    }

    var exercise by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("20") }
    var reps by rememberSaveable { mutableStateOf("8") }
    var note by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<WorkoutSet?>(null) }
    var tools by rememberSaveable { mutableStateOf(false) }
    var cardioOpen by rememberSaveable { mutableStateOf(false) }
    var showAllHistory by rememberSaveable { mutableStateOf(false) }

    fun pick(name: String) {
        exercise = name
        // Son yapılan değerlerle otomatik doldur
        sets.firstOrNull { it.exercise.trim().equals(name, true) }?.let {
            kg = it.weightKg.clean()
            reps = it.reps.toString()
        }
    }

    val template = TEMPLATES.firstOrNull { it.name == settings.activeTemplate }
    val doneToday = sets.filter { it.epochDay == vm.today }.map { it.exercise.trim().lowercase() }.toSet()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenHeader("Antrenman", longDayLabel(vm.today)) }
                IconButton(onClick = { tools = true }) { Icon(Icons.Rounded.Calculate, "Araçlar") }
            }
        }

        restEnd?.let { end -> item(key = "rest") { RestTimer(end, vm) } }

        // Şablon
        item {
            SectionCard("Bugünün planı", icon = Icons.Rounded.ListAlt, action = if (template != null) {
                { IconButton(onClick = { vm.updateSettings { it.copy(activeTemplate = "") } }) { Icon(Icons.Rounded.Close, "Planı kapat") } }
            } else null) {
                if (template == null) {
                    HintText("Hazır bir program seç; hareketler sırayla önüne gelsin.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TEMPLATES.forEach { t ->
                            Pill(t.name, FitColors.Lime, onClick = {
                                vm.updateSettings { it.copy(activeTemplate = t.name) }
                                t.exercises.firstOrNull { it.lowercase() !in doneToday }?.let { pick(it) }
                            })
                        }
                    }
                } else {
                    val done = template.exercises.count { it.lowercase() in doneToday }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(template.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Pill("$done / ${template.exercises.size}", if (done == template.exercises.size) FitColors.Lime else FitColors.Orange)
                    }
                    ProgressBar(done.toFloat() / template.exercises.size, FitColors.Lime)
                    template.exercises.forEach { ex ->
                        val isDone = ex.lowercase() in doneToday
                        Surface(
                            onClick = { pick(ex) }, shape = RoundedCornerShape(14.dp),
                            color = if (exercise.equals(ex, true)) FitColors.Lime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isDone) "✅" else "⬜")
                                Spacer(Modifier.width(10.dp))
                                Text(ex, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                HintText(muscleOf(ex))
                            }
                        }
                    }
                }
            }
        }

        // Set ekle
        item {
            SectionCard("Set ekle", icon = Icons.Rounded.FitnessCenter) {
                TextInput(exercise, { exercise = it }, "Hareket (ör. Bench Press)")
                val q = exercise.trim()
                val suggestions = if (q.isEmpty()) names.take(8) else names.filter {
                    it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true)
                }.take(8)
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s -> Pill(s, MaterialTheme.colorScheme.tertiary, onClick = { pick(s) }) }
                    }
                }
                if (q.isNotEmpty()) {
                    Calculator.suggestion(sets, q, vm.today)?.let { tip ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Lightbulb, FitColors.Orange, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stepper(kg, { kg = it }, "Ağırlık (kg)", 2.5, Modifier.weight(1f))
                    Stepper(reps, { reps = it }, "Tekrar", 1.0, Modifier.weight(1f))
                }
                TextInput(note, { note = it }, "Not (ops.): form, RPE, his…", singleLine = false)

                val w = kg.toNum()
                val r = reps.toNum()?.toInt()
                val valid = exercise.isNotBlank() && w != null && w >= 0 && r != null && r in 1..100
                if (valid) {
                    val e = Stats.epley1RM(w!!, r!!)
                    val best = sets.filter { it.exercise.trim().equals(q, true) }.maxOfOrNull { Stats.epley1RM(it.weightKg, it.reps) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Tahmini 1RM ≈ ${e.fmt()} kg", MaterialTheme.colorScheme.primary)
                        if (best != null && e > best) Pill("🏆 Rekor olur!", FitColors.Orange)
                    }
                }
                PrimaryButton(
                    "Seti kaydet",
                    onClick = {
                        vm.addSet(exercise.trim(), w!!, r!!, note.trim())
                        note = ""
                    },
                    enabled = valid,
                    icon = Icons.Rounded.Check
                )
                HintText("Kaydedince ${settings.restSeconds} sn dinlenme sayacı başlar. Silmek için geçmişteki sete dokun.")
            }
        }

        // Kardiyo
        item {
            if (cardioOpen) {
                CardioCard(weightKg = state.profile?.weightKg ?: 75.0, onClose = { cardioOpen = false }) { t, m, k ->
                    vm.addCardio(t, m, k)
                    cardioOpen = false
                }
            } else {
                SecondaryButton("Kardiyo ekle", onClick = { cardioOpen = true }, icon = Icons.Rounded.DirectionsRun, modifier = Modifier.fillMaxWidth())
            }
        }

        if (sets.isEmpty() && cardio.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.FitnessCenter, "İlk setini gir",
                    "Bir plan seç ya da hareket adını yaz. Girdiğin setler burada güne göre listelenir."
                )
            }
        }

        val days = (sets.map { it.epochDay } + cardio.map { it.epochDay }).toSet().sortedDescending()
        val shown = if (showAllHistory) days else days.take(7)
        shown.forEach { day ->
            item(key = "day-$day") {
                DayCard(
                    title = if (day == vm.today) "Bugün" else dayLabel(day),
                    sets = sets.filter { it.epochDay == day },
                    cardio = cardio.filter { it.epochDay == day },
                    onSetClick = { toDelete = it },
                    onCardioDelete = { vm.deleteCardio(it) }
                )
            }
        }
        if (days.size > 7 && !showAllHistory) {
            item { TextButton(onClick = { showAllHistory = true }, modifier = Modifier.fillMaxWidth()) { Text("Tüm geçmişi göster (${days.size} gün)") } }
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Seti sil?") },
            text = { Text("${s.exercise} · ${s.weightKg.clean()} kg × ${s.reps}" + if (s.note.isNotBlank()) "\n📝 ${s.note}" else "") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSet(s); toDelete = null }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (tools) ToolsSheet(initialKg = kg.toNum() ?: 60.0) { tools = false }
}

@Composable
private fun RestTimer(end: Long, vm: AppViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= end) {
                vm.restFinished()
                break
            }
            delay(250)
        }
    }
    val left = ((end - now) / 1000).coerceAtLeast(0)
    val total = vm.settings.value.restSeconds.coerceAtLeast(1)
    Surface(shape = RoundedCornerShape(24.dp), color = FitColors.Lime.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = FitColors.Lime)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dinlenme", style = MaterialTheme.typography.titleSmall)
                    HintText("Süre dolunca telefon titrer")
                }
                Text(
                    "%d:%02d".format(left / 60, left % 60),
                    style = MaterialTheme.typography.headlineMedium, color = FitColors.Lime, fontWeight = FontWeight.Black
                )
            }
            ProgressBar((left.toFloat() / total).coerceIn(0f, 1f), FitColors.Lime)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("+30 sn", onClick = { vm.addRest(30) }, modifier = Modifier.weight(1f))
                SecondaryButton("Atla", onClick = { vm.stopRest() }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardioCard(weightKg: Double, onClose: () -> Unit, onSave: (String, Int, Int) -> Unit) {
    var type by rememberSaveable { mutableStateOf(CARDIO_TYPES[0].name) }
    var minutes by rememberSaveable { mutableStateOf("30") }
    val ct = CARDIO_TYPES.first { it.name == type }
    val m = minutes.toNum()?.toInt()
    val kcal = if (m != null) Calculator.cardioKcal(ct.met, weightKg, m) else 0
    SectionCard("Kardiyo", icon = Icons.Rounded.DirectionsRun, action = {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Kapat") }
    }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CARDIO_TYPES.forEach { c ->
                Pill("${c.emoji} ${c.name}", if (c.name == type) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { type = c.name })
            }
        }
        Stepper(minutes, { minutes = it }, "Süre (dakika)", 5.0)
        Pill("≈ $kcal kcal yakılır", FitColors.Orange)
        PrimaryButton("Kaydet", onClick = { onSave(type, m!!, kcal) }, enabled = m != null && m in 1..600, icon = Icons.Rounded.Check)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(
    title: String,
    sets: List<WorkoutSet>,
    cardio: List<CardioEntry>,
    onSetClick: (WorkoutSet) -> Unit,
    onCardioDelete: (CardioEntry) -> Unit
) {
    val byExercise = sets.groupBy { it.exercise.trim() }
    val volume = sets.sumOf { it.weightKg * it.reps }
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            HintText(
                listOfNotNull(
                    if (sets.isNotEmpty()) "${sets.size} set" else null,
                    if (volume > 0) "${volume.roundToInt()} kg hacim" else null,
                    if (cardio.isNotEmpty()) "${cardio.sumOf { it.minutes }} dk kardiyo" else null
                ).joinToString(" · ")
            )
        }
        byExercise.forEach { (name, list) ->
            val ordered = list.reversed()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "≈1RM ${ordered.maxOf { Stats.epley1RM(it.weightKg, it.reps) }.fmt()} kg",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ordered.forEach { s ->
                        Pill("${s.weightKg.clean()} × ${s.reps}", MaterialTheme.colorScheme.onSurface, onClick = { onSetClick(s) })
                    }
                }
                ordered.filter { it.note.isNotBlank() }.forEach { s -> HintText("📝 ${s.weightKg.clean()}×${s.reps}: ${s.note}") }
            }
        }
        cardio.forEach { c ->
            val emoji = CARDIO_TYPES.firstOrNull { it.name == c.type }?.emoji ?: "🏃"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$emoji ${c.type}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                HintText("${c.minutes} dk · ${c.kcal} kcal")
                IconButton(onClick = { onCardioDelete(c) }) { Icon(Icons.Rounded.Close, "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsSheet(initialKg: Double, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var total by rememberSaveable { mutableStateOf(initialKg.clean()) }
    var bar by rememberSaveable { mutableStateOf("20") }
    var oneRm by rememberSaveable { mutableStateOf("100") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Araçlar", style = MaterialTheme.typography.headlineMedium) }
            item {
                SectionCard("Plaka hesaplayıcı") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stepper(total, { total = it }, "Toplam (kg)", 2.5, Modifier.weight(1f))
                        Stepper(bar, { bar = it }, "Bar (kg)", 2.5, Modifier.weight(1f))
                    }
                    val t = total.toNum()
                    val b = bar.toNum()
                    if (t != null && b != null) {
                        val (plates, rest) = Calculator.plates(t, b)
                        if (plates.isEmpty()) HintText("Toplam ağırlık bardan fazla olmalı.")
                        else {
                            Text("Her iki tarafa:", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                plates.forEach { p ->
                                    val color = when (p) {
                                        25.0 -> FitColors.Red; 20.0 -> FitColors.Blue; 15.0 -> FitColors.Orange
                                        10.0 -> FitColors.Lime; else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Pill(p.clean(), color)
                                }
                            }
                            if (rest > 0.01) HintText("${rest.fmt(2)} kg plakayla tamamlanamıyor.")
                        }
                    }
                }
            }
            item {
                SectionCard("1RM yüzde tablosu") {
                    Stepper(oneRm, { oneRm = it }, "1RM (kg)", 2.5)
                    val orm = oneRm.toNum() ?: 0.0
                    val rows = listOf(95 to 2, 90 to 3, 85 to 5, 80 to 6, 75 to 8, 70 to 10, 65 to 12, 60 to 15)
                    rows.forEach { (pct, rep) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("%$pct", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp), color = FitColors.Lime)
                            Text("${(orm * pct / 100 / 2.5).roundToInt() * 2.5} kg".replace(".0 kg", " kg").replace('.', ','),
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            HintText("≈ $rep tekrar")
                        }
                    }
                }
            }
        }
    }
}
