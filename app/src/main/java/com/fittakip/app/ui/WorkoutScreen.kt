package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.clickable
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
fun WorkoutScreen(vm: AppViewModel, onOpenExercise: (String) -> Unit = {}) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val cardio by vm.cardio.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val restEnd by vm.restEnd.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val wDay by vm.workoutDay.collectAsStateWithLifecycle()

    val names = remember(sets) {
        (sets.map { it.exercise.trim() } + EXERCISES.map { it.name }).distinctBy { it.lowercase() }
    }

    var exercise by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("20") }
    var reps by rememberSaveable { mutableStateOf("8") }
    var note by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<WorkoutSet?>(null) }
    var tools by rememberSaveable { mutableStateOf(false) }
    var cardioOpen by rememberSaveable { mutableStateOf(false) }
    var showAllHistory by rememberSaveable { mutableStateOf(false) }
    var warmup by rememberSaveable { mutableStateOf(false) }
    var rpe by rememberSaveable { mutableStateOf<Double?>(null) }
    var newTemplate by rememberSaveable { mutableStateOf(false) }
    var library by rememberSaveable { mutableStateOf(false) }

    fun pick(name: String) {
        exercise = name
        // Son yapılan değerlerle otomatik doldur
        sets.firstOrNull { it.exercise.trim().equals(name, true) }?.let {
            kg = it.weightKg.clean()
            reps = it.reps.toString()
        }
    }

    val allTemplates = settings.allTemplates
    val planned = settings.templateFor(wDay)
    val isOff = settings.planFor(wDay) == com.fittakip.app.data.OFF_DAY
    val template = allTemplates.firstOrNull { it.name == settings.activeTemplate } ?: planned
    val daySets = sets.filter { it.epochDay == wDay && !it.warmup }
    fun doneSets(ex: String) = daySets.count { it.exercise.trim().equals(ex, true) }
    val lastToday = sets.firstOrNull { it.epochDay == wDay && it.exercise.trim().equals(exercise.trim(), true) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenHeader("Antrenman", longDayLabel(wDay)) }
                IconButton(onClick = { vm.shiftWorkoutDay(-1) }) { Icon(Icons.Rounded.ChevronLeft, "Önceki gün") }
                IconButton(onClick = { vm.shiftWorkoutDay(1) }, enabled = wDay < vm.today) { Icon(Icons.Rounded.ChevronRight, "Sonraki gün") }
                IconButton(onClick = { library = true }) { Icon(Icons.AutoMirrored.Rounded.MenuBook, "Hareket kütüphanesi") }
                IconButton(onClick = { tools = true }) { Icon(Icons.Rounded.Calculate, "Araçlar") }
            }
        }

        if (wDay != vm.today) {
            item { Pill("Geçmiş güne set ekliyorsun: ${dayLabel(wDay, true)} · bugüne dön", FitColors.Blue, onClick = { vm.shiftWorkoutDay((vm.today - wDay).toInt()) }) }
        }
        if (isOff && settings.activeTemplate.isEmpty() && wDay == vm.today) {
            item {
                FitCard {
                    Text("Bugün off day 😌", style = MaterialTheme.typography.titleLarge)
                    HintText("Programına göre bugün dinlenme günü. Yine de antrenman yapmak istersen aşağıdan bir program seçebilir ya da hafif kardiyo ekleyebilirsin.")
                }
            }
        }
        item(key = "session") { SessionCard(settings.sessionStart, vm) }
        restEnd?.let { end -> item(key = "rest") { RestTimer(end, vm) } }

        // Şablon
        item {
            SectionCard(if (template != null && template == planned) "Programdaki antrenman" else "Bugünün planı", icon = Icons.Rounded.ListAlt, action = if (template != null && settings.activeTemplate.isNotEmpty()) {
                { IconButton(onClick = { vm.updateSettings { it.copy(activeTemplate = "") } }) { Icon(Icons.Rounded.Close, "Planı kapat") } }
            } else null) {
                if (template == null) {
                    HintText("Hazır bir program seç; hareketler sırayla önüne gelsin.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("+ Kendi programın", FitColors.Orange, onClick = { newTemplate = true })
                        allTemplates.forEach { t ->
                            Pill(t.name, FitColors.Lime, onClick = {
                                vm.updateSettings { it.copy(activeTemplate = t.name) }
                                t.items.firstOrNull { doneSets(it.exercise) < it.sets }?.let { pick(it.exercise) }
                            })
                        }
                    }
                } else {
                    val done = template.items.count { doneSets(it.exercise) >= it.sets }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(template.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Pill("$done / ${template.exercises.size}", if (done == template.exercises.size) FitColors.Lime else FitColors.Orange)
                    }
                    ProgressBar(done.toFloat() / template.exercises.size, FitColors.Lime)
                    if (settings.templates.any { it.name == template.name }) {
                        TextButton(onClick = { vm.deleteTemplate(template.name) }) { Text("Bu programı sil", color = MaterialTheme.colorScheme.error) }
                    }
                    template.items.forEach { item ->
                        val ex = item.exercise
                        val n = doneSets(ex)
                        val isDone = n >= item.sets
                        Surface(
                            onClick = { pick(ex) }, shape = RoundedCornerShape(16.dp),
                            color = if (exercise.equals(ex, true)) FitColors.Lime.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isDone) "✅" else "🏋️")
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ex, style = MaterialTheme.typography.titleSmall)
                                        HintText(
                                            "${muscleOf(ex)} · hedef ${item.label}" +
                                                (if (item.note.isNotBlank()) " · ${item.note}" else "")
                                        )
                                        if (item.superset > 0) {
                                            val partner = template.items.firstOrNull { it.superset == item.superset && it.exercise != ex }
                                            if (partner != null) Text("⚡ Süperset S${item.superset} · ${partner.exercise} ile", style = MaterialTheme.typography.labelSmall, color = FitColors.Orange)
                                        }
                                    }
                                    Text("${n.coerceAtMost(item.sets)}/${item.sets}", style = MaterialTheme.typography.titleMedium, color = if (isDone) FitColors.Lime else MaterialTheme.colorScheme.onSurface)
                                }
                                ProgressBar(n.toFloat() / item.sets, if (isDone) FitColors.Lime else FitColors.Orange, 5.dp)
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
                val planItem = template?.items?.firstOrNull { it.exercise.equals(q, true) }
                if (q.isNotEmpty()) {
                    (if (planItem != null) Calculator.rangeSuggestion(sets, planItem, wDay) else Calculator.suggestion(sets, q, vm.today))?.let { tip ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Lightbulb, FitColors.Orange, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                lastToday?.let { last ->
                    SecondaryButton(
                        "Son seti tekrarla · ${last.weightKg.clean()} × ${last.reps}",
                        onClick = { vm.addSet(last.exercise, last.weightKg, last.reps, "", last.warmup, last.rpe) },
                        icon = Icons.Rounded.Replay, modifier = Modifier.fillMaxWidth()
                    )
                }
                template?.items?.firstOrNull { it.exercise.equals(exercise.trim(), true) }?.let { item ->
                    HintText("Hedef: ${item.label} · yapılan ${doneSets(item.exercise)}/${item.sets} set" + (if (item.note.isNotBlank()) " · ${item.note}" else ""))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stepper(kg, { kg = it }, "Ağırlık (kg)", 2.5, Modifier.weight(1f))
                    Stepper(reps, { reps = it }, "Tekrar", 1.0, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = warmup, onCheckedChange = { warmup = it })
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Isınma seti", style = MaterialTheme.typography.titleSmall)
                        HintText("Rekora ve hacme sayılmaz, dinlenme 45 sn")
                    }
                }
                Text("Zorluk (RPE) · 10 = hiç tekrar kalmadı", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6.0, 7.0, 8.0, 9.0, 10.0).forEach { r ->
                        SelectCard(rpe == r, { rpe = if (rpe == r) null else r }, r.clean(), Modifier.weight(1f))
                    }
                }
                TextInput(note, { note = it }, "Not (ops.): form, his…", singleLine = false)

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
                        // Süperset: A'dan sonra kısa dinlenmeyle B'ye, B'den sonra normal dinlenmeyle A'ya geç
                        val cur = template?.items?.firstOrNull { it.exercise.equals(exercise.trim(), true) }
                        val group = if (!warmup && cur != null && cur.superset > 0) template.items.filter { it.superset == cur.superset } else emptyList()
                        val isFirst = group.size == 2 && group[0].exercise.equals(cur?.exercise, true)
                        vm.addSet(exercise.trim(), w!!, r!!, note.trim(), warmup, rpe, if (isFirst) 15 else null)
                        if (group.size == 2) {
                            val partner = if (isFirst) group[1] else group[0]
                            if (doneSets(partner.exercise) < partner.sets) pick(partner.exercise)
                        }
                        note = ""
                        rpe = null
                    },
                    enabled = valid,
                    icon = Icons.Rounded.Check
                )
                HintText("Kaydedince ${settings.restSeconds} sn dinlenme sayacı başlar; telefon kilitliyken de süre dolunca titrer. Düzeltmek için geçmişteki sete dokun.")
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
                    onSetClick = { editing = it },
                    onCardioDelete = { vm.deleteCardio(it) },
                    onShare = { vm.share(vm.workoutShareText(day)) },
                    onExercise = onOpenExercise
                )
            }
        }
        if (days.size > 7 && !showAllHistory) {
            item { TextButton(onClick = { showAllHistory = true }, modifier = Modifier.fillMaxWidth()) { Text("Tüm geçmişi göster (${days.size} gün)") } }
        }
    }

    editing?.let { s -> EditSetDialog(s, onDismiss = { editing = null }, onSave = { vm.updateSet(it); editing = null }, onDelete = { vm.deleteSet(s); editing = null }) }

    if (tools) ToolsSheet(initialKg = kg.toNum() ?: 60.0) { tools = false }
    if (newTemplate) NewTemplateDialog(onDismiss = { newTemplate = false }) { n, ex ->
        vm.saveTemplate(n, ex)
        newTemplate = false
    }
    if (library) LibrarySheet(onDismiss = { library = false }, onDetail = { library = false; onOpenExercise(it) }) {
        pick(it)
        library = false
    }
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
    onCardioDelete: (CardioEntry) -> Unit,
    onShare: () -> Unit,
    onExercise: (String) -> Unit
) {
    val byExercise = sets.groupBy { it.exercise.trim() }
    val volume = sets.filter { !it.warmup }.sumOf { it.weightKg * it.reps }
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
            IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Paylaş", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        byExercise.forEach { (name, list) ->
            val ordered = list.reversed()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$name ›", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).clickable { onExercise(name) }
                    )
                    Text(
                        if (Stats.isTimed(name) || ordered.all { it.weightKg == 0.0 }) "en iyi ${ordered.maxOf { it.reps }} ${if (Stats.isTimed(name)) "sn" else "tekrar"}"
                        else "≈1RM ${(ordered.filter { !it.warmup }.ifEmpty { ordered }).maxOf { Stats.epley1RM(it.weightKg, it.reps) }.fmt()} kg",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ordered.forEach { s ->
                        Pill(
                            (if (s.warmup) "ı " else "") + "${s.weightKg.clean()} × ${s.reps}" + (s.rpe?.let { " @${it.clean()}" } ?: ""),
                            if (s.warmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            onClick = { onSetClick(s) }
                        )
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


@Composable
private fun EditSetDialog(s: WorkoutSet, onDismiss: () -> Unit, onSave: (WorkoutSet) -> Unit, onDelete: () -> Unit) {
    var kg by remember { mutableStateOf(s.weightKg.clean()) }
    var reps by remember { mutableStateOf(s.reps.toString()) }
    var warm by remember { mutableStateOf(s.warmup) }
    var rpe by remember { mutableStateOf(s.rpe) }
    var note by remember { mutableStateOf(s.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${s.exercise} · ${dayLabel(s.epochDay)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stepper(kg, { kg = it }, "Ağırlık", 2.5, Modifier.weight(1f))
                    Stepper(reps, { reps = it }, "Tekrar", 1.0, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = warm, onCheckedChange = { warm = it })
                    Text("Isınma seti")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(6.0, 7.0, 8.0, 9.0, 10.0).forEach { r -> SelectCard(rpe == r, { rpe = if (rpe == r) null else r }, r.clean(), Modifier.weight(1f)) }
                }
                TextInput(note, { note = it }, "Not")
            }
        },
        confirmButton = {
            val w = kg.toNum()
            val r = reps.toNum()?.toInt()
            TextButton(
                onClick = { onSave(s.copy(weightKg = w!!, reps = r!!, warmup = warm, rpe = rpe, note = note.trim())) },
                enabled = w != null && w >= 0 && r != null && r in 1..100
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
