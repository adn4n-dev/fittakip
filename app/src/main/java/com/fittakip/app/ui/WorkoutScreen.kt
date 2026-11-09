package com.fittakip.app.ui

import com.fittakip.app.data.counted
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.graphics.Color
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
    var cardioOpen by rememberSaveable { mutableStateOf(false) }
    var showAllHistory by rememberSaveable { mutableStateOf(false) }
    var warmup by rememberSaveable { mutableStateOf(false) }
    var rpe by rememberSaveable { mutableStateOf<Double?>(null) }
    var drop by rememberSaveable { mutableStateOf(false) }
    var dropKg by rememberSaveable { mutableStateOf("") }
    var dropReps by rememberSaveable { mutableStateOf("") }
    var newTemplate by rememberSaveable { mutableStateOf(false) }
    var library by rememberSaveable { mutableStateOf(false) }

    val allTemplates = settings.allTemplates
    val dayPlan = remember(settings, sets, wDay) { com.fittakip.app.domain.PlanResolver.forDay(settings, sets, wDay) }
    val planned = (dayPlan as? com.fittakip.app.domain.DayPlan.Workout)?.template
    val isOff = dayPlan is com.fittakip.app.domain.DayPlan.Off
    val template = allTemplates.firstOrNull { it.name == settings.activeTemplate } ?: planned
    val daySets = sets.filter { it.epochDay == wDay && it.counted() }
    fun doneSets(ex: String) = daySets.count { it.exercise.trim().equals(ex, true) }
    fun nextPlanExercise(after: String): String? {
        val items = template?.items ?: return null
        val i = items.indexOfFirst { it.exercise.equals(after, true) }
        return (items.drop(i + 1) + items.take(i.coerceAtLeast(0))).firstOrNull { doneSets(it.exercise) < it.sets && it.exercise != after }?.exercise
    }
    val lastToday = sets.firstOrNull { it.epochDay == wDay && it.exercise.trim().equals(exercise.trim(), true) }

    /** Bu hareketin, aynı antrenmanın (ör. Upper B) günlerindeki geçmişi. */
    fun contextPast(name: String) = com.fittakip.app.domain.Coach.contextHistory(sets, name, template?.name, allTemplates, wDay)

    fun pick(name: String) {
        exercise = name
        // Bugün bu harekette set girdiysen onun devamı; yoksa koçun önerdiği ağırlık/tekrar (ısınma ve drop hariç).
        val todayLast = sets.firstOrNull { it.epochDay == wDay && it.counted() && it.exercise.trim().equals(name, true) }
        val item = template?.items?.firstOrNull { it.exercise.equals(name, true) }
        val target = if (todayLast != null) com.fittakip.app.domain.Coach.Target(todayLast.weightKg, todayLast.reps)
        else com.fittakip.app.domain.Coach.nextTarget(contextPast(name), item)
            ?: sets.firstOrNull { it.counted() && it.exercise.trim().equals(name, true) }?.let { com.fittakip.app.domain.Coach.Target(it.weightKg, it.reps) }
        target?.let {
            kg = it.kg.clean()
            reps = it.reps.toString()
        }
    }

    var focus by rememberSaveable { mutableStateOf(false) }
    var moreOptions by rememberSaveable { mutableStateOf(false) }

    /** Spor modu: hedefi tek dokunuşla kaydet, süperset partnerine ya da sıradaki harekete geç. */
    /** Üşengeç modu: bu hareketin kalan setlerini aynı ağırlık/tekrarla tek seferde kaydet, sonrakine geç. */
    fun completeRemaining(w: Double, r: Int) {
        val name = exercise.trim()
        val cur = template?.items?.firstOrNull { it.exercise.equals(name, true) } ?: return
        val left = cur.sets - doneSets(cur.exercise)
        if (left <= 0) return
        repeat(left) { vm.addSet(name, w, r, "", false, null, null) }
        val nx = nextPlanExercise(cur.exercise)
        if (nx != null) { pick(nx); vm.toast("${cur.exercise}: $left set eklendi ✅ Sıradaki: $nx") }
        else vm.toast("${template.name} bitti 🎉 Eline sağlık!")
    }

    fun logAndAdvance(w: Double, r: Int) {
        val name = exercise.trim()
        val cur = template?.items?.firstOrNull { it.exercise.equals(name, true) }
        val group = if (cur != null && cur.superset > 0) template.items.filter { it.superset == cur.superset } else emptyList()
        val isFirst = group.size == 2 && group[0].exercise.equals(cur?.exercise, true)
        vm.addSet(name, w, r, "", false, null, if (isFirst) 15 else null)
        if (group.size == 2) {
            val partner = if (isFirst) group[1] else group[0]
            if (doneSets(partner.exercise) < partner.sets) { pick(partner.exercise); return }
        }
        if (cur != null && doneSets(cur.exercise) + 1 >= cur.sets) {
            val nx = nextPlanExercise(cur.exercise)
            if (nx != null) { pick(nx); vm.toast("${cur.exercise} tamam ✅ Sıradaki: $nx") }
            else vm.toast("${template.name} bitti 🎉 Eline sağlık!")
        }
    }

    // Antrenman ekranı açıkken ekran kararmasın (setler arası kilit açmak zorunda kalma).
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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
                    if (wDay == vm.today && done < template.exercises.size) PrimaryButton("🎯 Spor modu · tek dokunuşla set", onClick = {
                        if (template.items.none { it.exercise.equals(exercise.trim(), true) && doneSets(it.exercise) < it.sets })
                            template.items.firstOrNull { doneSets(it.exercise) < it.sets }?.let { pick(it.exercise) }
                        focus = true
                    }, modifier = Modifier.fillMaxWidth())
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
                                        com.fittakip.app.data.exerciseTr(ex)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FitColors.Lime.copy(alpha = 0.85f)) }
                                        HintText(
                                            "${muscleOf(ex)} · hedef ${item.label}" +
                                                (if (item.note.isNotBlank()) " · ${item.note}" else "")
                                        )
                                        if (item.superset > 0) {
                                            val partner = template.items.firstOrNull { it.superset == item.superset && it.exercise != ex }
                                            if (partner != null) Text("⚡ Süperset S${item.superset} · ${partner.exercise} ile", style = MaterialTheme.typography.labelSmall, color = FitColors.Orange)
                                        }
                                        if (!settings.isAvailable(ex)) {
                                            Text("⚠️ Salonunda yok olarak işaretli. Yerine:", style = MaterialTheme.typography.labelSmall, color = FitColors.Red)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                vm.alternativesFor(ex).forEach { alt ->
                                                    Pill(alt, FitColors.Lime, onClick = { vm.swapInTemplate(template.name, ex, alt); pick(alt) })
                                                }
                                            }
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
                val usable = names.filter { settings.isAvailable(it) }
                val suggestions = if (q.isEmpty()) usable.take(8) else usable.filter {
                    it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true)
                }.take(8)
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s -> Pill(s, MaterialTheme.colorScheme.tertiary, onClick = { pick(s) }) }
                    }
                }
                val planItem = template?.items?.firstOrNull { it.exercise.equals(q, true) }
                if (q.isNotEmpty()) {
                    val ctxDay = contextPast(q).maxOfOrNull { it.epochDay }
                    val prev = sets.filter { it.exercise.trim().equals(q, true) && it.epochDay < wDay && !it.warmup }
                    (ctxDay ?: prev.maxOfOrNull { it.epochDay })?.let { lastDay ->
                        val ls = prev.filter { it.epochDay == lastDay }.reversed()
                        Text(
                            "Geçen sefer (${dayLabel(lastDay)}): " + ls.joinToString(", ") { (if (it.isDrop) "↓" else "") + "${it.weightKg.clean()}×${it.reps}" },
                            style = MaterialTheme.typography.bodyMedium, color = FitColors.Blue
                        )
                    }
                    com.fittakip.app.data.exerciseTr(q)?.let { HintText("ℹ️ $q: $it") }
                    val bwKg = state.profile?.weightKg ?: 0.0
                    val plateau = remember(q, sets, template?.name) {
                        val past = contextPast(q)
                        if (past.map { it.epochDay }.distinct().size < 4) null
                        else com.fittakip.app.domain.ProgressAnalyzer.analyzeExercise(past, bwKg).takeIf {
                            it.verdict == com.fittakip.app.domain.Verdict.PLATEAU && it.history.size >= 4 &&
                                it.history.last().first - it.history.first().first >= 21
                        }
                    }
                    plateau?.let { PlateauCoach(q, kg.toNum(), onLight = { light -> kg = light.clean() }) }
                    MachineNoteRow(q, settings.machineNote(q)) { vm.setMachineNote(q, it) }
                }
                if (q.isNotEmpty()) {
                    (if (planItem != null) Calculator.rangeSuggestionFrom(contextPast(q), planItem) else Calculator.suggestion(sets, q, vm.today))?.let { tip ->
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
                    val step = com.fittakip.app.data.weightStep(q)
                    Stepper(kg, { kg = it }, "Ağırlık (kg) · ±${step.clean()}", step, Modifier.weight(1f))
                    Stepper(reps, { reps = it }, "Tekrar", 1.0, Modifier.weight(1f))
                }
                val showMore = moreOptions || warmup || rpe != null || drop || note.isNotBlank()
                Surface(
                    onClick = { moreOptions = !showMore }, shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️ Isınma, zorluk, drop set, not", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(if (showMore) "Gizle ▲" else "Aç ▼", style = MaterialTheme.typography.labelLarge, color = FitColors.Lime)
                    }
                }
                if (showMore) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = drop, onCheckedChange = {
                        drop = it
                        if (it && dropKg.isBlank()) {
                            // Varsayılan: ağırlığın ~%25 azı, aynı tekrar
                            val st = com.fittakip.app.data.weightStep(exercise)
                            dropKg = ((kg.toNum() ?: 0.0) * 0.75 / st).let { v -> kotlin.math.round(v) * st }.clean()
                            dropReps = reps
                        }
                    })
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Drop set ekle", style = MaterialTheme.typography.titleSmall)
                        HintText("Setin hemen ardından düşük ağırlıkla devam. Hedef sete sayılmaz, hacme sayılır.")
                    }
                }
                if (drop) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stepper(dropKg, { dropKg = it }, "↓ Drop ağırlık", com.fittakip.app.data.weightStep(q), Modifier.weight(1f))
                        Stepper(dropReps, { dropReps = it }, "↓ Drop tekrar", 1.0, Modifier.weight(1f))
                    }
                }
                TextInput(note, { note = it }, "Not (ops.): form, his…", singleLine = false)
                }

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
                        val dk = dropKg.toNum()
                        val dr = dropReps.toNum()?.toInt()
                        if (drop && dk != null && dr != null && dr > 0) {
                            vm.addSet(exercise.trim(), dk, dr, "", false, null, if (isFirst) 15 else null, isDrop = true)
                            drop = false
                        }
                        if (group.size == 2) {
                            val partner = if (isFirst) group[1] else group[0]
                            if (doneSets(partner.exercise) < partner.sets) pick(partner.exercise)
                            else if (doneSets(cur!!.exercise) + 1 >= cur.sets) nextPlanExercise(cur.exercise)?.let { pick(it) }
                        } else if (!warmup && cur != null && doneSets(cur.exercise) + 1 >= cur.sets) {
                            // Hedef setler bitti: programdaki sıradaki harekete geç
                            nextPlanExercise(cur.exercise)?.let {
                                pick(it)
                                vm.toast("${cur.exercise} tamam ✅ Sıradaki: $it")
                            }
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

    if (newTemplate) NewTemplateDialog(onDismiss = { newTemplate = false }) { n, ex ->
        vm.saveTemplate(n, ex)
        newTemplate = false
    }
    if (library) LibrarySheet(
        unavailable = settings.unavailable,
        onToggleAvailable = { n, on -> vm.setAvailable(n, on) },
        onDismiss = { library = false },
        onDetail = { library = false; onOpenExercise(it) }
    ) {
        pick(it)
        library = false
    }

    if (focus && template != null) {
        val item = template.items.firstOrNull { it.exercise.equals(exercise.trim(), true) }
        val past = contextPast(exercise.trim())
        val lastDay = past.maxOfOrNull { it.epochDay }
        FocusMode(
            exercise = exercise.trim(),
            item = item,
            setNo = doneSets(exercise.trim()) + 1,
            done = template.items.count { doneSets(it.exercise) >= it.sets },
            total = template.items.size,
            lastTime = lastDay?.let { d -> past.filter { it.epochDay == d }.joinToString("  ") { "${it.weightKg.clean()}×${it.reps}" } },
            kg = kg, reps = reps,
            onKg = { kg = it }, onReps = { reps = it },
            restEnd = restEnd, vm = vm,
            nextName = item?.let { nextPlanExercise(it.exercise) },
            onSkip = { item?.let { nextPlanExercise(it.exercise) }?.let { pick(it) } },
            remaining = item?.let { it.sets - doneSets(it.exercise) } ?: 0,
            onCompleteRemaining = { kg.toNum()?.let { w -> reps.toNum()?.toInt()?.let { r -> if (r > 0) completeRemaining(w, r) } } },
            onLog = { kg.toNum()?.let { w -> reps.toNum()?.toInt()?.let { r -> if (r > 0) logAndAdvance(w, r) } } },
            onClose = { focus = false }
        )
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
            ExactAlarmHint()
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
    val volume = sets.filter { it.counted() }.sumOf { it.weightKg * it.reps }
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
                        else "≈1RM ${(ordered.filter { it.counted() }.ifEmpty { ordered }).maxOf { Stats.epley1RM(it.weightKg, it.reps) }.fmt()} kg",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ordered.forEach { s ->
                        Pill(
                            (if (s.warmup) "ı " else "") + (if (s.isDrop) "↓ " else "") + "${s.weightKg.clean()} × ${s.reps}" + (s.rpe?.let { " @${it.clean()}" } ?: ""),
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


/** Android 12+ : tam zamanlı alarm izni yoksa dinlenme titreşimi gecikebilir; izin sayfasını aç. */
@Composable
fun ExactAlarmHint() {
    if (android.os.Build.VERSION.SDK_INT < 31) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val am = ctx.getSystemService(android.app.AlarmManager::class.java)
    var allowed by remember { mutableStateOf(am.canScheduleExactAlarms()) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) allowed = am.canScheduleExactAlarms()
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }
    if (allowed) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        HintText("⚠️ Telefon kilitliyken titreşim gecikebilir. Tam zamanlı alarm iznini aç.", Modifier.weight(1f))
        TextButton(onClick = {
            ctx.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + ctx.packageName))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }) { Text("İzin ver") }
    }
}


/** Makine ayarı notu: koltuk yüksekliği, pim, tutuş gibi. */
@Composable
private fun MachineNoteRow(exercise: String, note: String, onSave: (String) -> Unit) {
    var editing by remember(exercise) { mutableStateOf(false) }
    var text by remember(exercise, note) { mutableStateOf(note) }
    if (editing) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(text, { text = it }, "Makine ayarı (ör. koltuk 4, pim 3)", Modifier.weight(1f))
            TextButton(onClick = { onSave(text); editing = false }) { Text("Kaydet") }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { editing = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (note.isBlank()) "⚙️ Makine ayarı ekle (koltuk, pim, tutuş…)" else "⚙️ Ayar: $note",
                style = MaterialTheme.typography.bodyMedium,
                color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else FitColors.Orange,
                modifier = Modifier.weight(1f)
            )
            Text("Düzenle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


/** Tam ekran, sade antrenman: kocaman hedef, tek dokunuşla "Set tamam", büyük dinlenme sayacı. */
@Composable
private fun FocusMode(
    exercise: String, item: com.fittakip.app.data.PlanItem?, setNo: Int, done: Int, total: Int, lastTime: String?,
    kg: String, reps: String, onKg: (String) -> Unit, onReps: (String) -> Unit,
    restEnd: Long?, vm: AppViewModel, nextName: String?,
    onSkip: () -> Unit, onLog: () -> Unit, onClose: () -> Unit,
    remaining: Int = 0, onCompleteRemaining: () -> Unit = {}
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) { view.keepScreenOn = true; onDispose { } }
        val step = com.fittakip.app.data.weightStep(exercise)
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(restEnd) {
            while (restEnd != null && System.currentTimeMillis() < restEnd) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(250) }
            now = System.currentTimeMillis()
        }
        val left = restEnd?.let { ((it - now) / 1000).coerceAtLeast(0) } ?: 0
        // Set kaydedilince ortada büyük ✓ patlar ve kaybolur, telefon titrer
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val burst = remember { androidx.compose.animation.core.Animatable(0f) }
        var burstKey by remember { mutableIntStateOf(0) }
        LaunchedEffect(burstKey) {
            if (burstKey == 0) return@LaunchedEffect
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            burst.snapTo(0f)
            burst.animateTo(1f, androidx.compose.animation.core.tween(750))
        }
        val resting = restEnd != null && left > 0
        Surface(color = Color(0xFF07090C), modifier = Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hareket ${done + 1}/$total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Kapat") }
                }
                Text(exercise, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, maxLines = 2)
                com.fittakip.app.data.exerciseTr(exercise)?.let { HintText(it) }
                Text(
                    "Set $setNo" + (item?.let { " / ${it.sets} · hedef ${it.repsLabel}" } ?: "") + (if (item?.note?.isNotBlank() == true) " · ${item.note}" else ""),
                    style = MaterialTheme.typography.titleMedium, color = FitColors.Lime
                )
                lastTime?.let { HintText("Geçen sefer: $it") }

                Spacer(Modifier.weight(1f))
                if (resting) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DİNLEN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 3.sp)
                        Text("%d:%02d".format(left / 60, left % 60), fontSize = 88.sp, fontWeight = FontWeight.Black, color = FitColors.Lime,
                            modifier = Modifier.semantics { contentDescription = "Dinlenme, $left saniye kaldı" })
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("+30 sn", onClick = { vm.addRest(30) })
                            SecondaryButton("Atla", onClick = { vm.stopRest() })
                        }
                    }
                } else {
                    BigAdjust("kg", kg, onMinus = { onKg(((kg.toNum() ?: 0.0) - step).coerceAtLeast(0.0).clean()) }, onPlus = { onKg(((kg.toNum() ?: 0.0) + step).clean()) })
                    BigAdjust("tekrar", reps, onMinus = { onReps(((reps.toNum()?.toInt() ?: 0) - 1).coerceAtLeast(0).toString()) }, onPlus = { onReps(((reps.toNum()?.toInt() ?: 0) + 1).toString()) })
                }
                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { burstKey++; onLog() }, enabled = !resting && (reps.toNum() ?: 0.0) > 0,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FitColors.Lime, contentColor = Color(0xFF0E1200)),
                    modifier = Modifier.fillMaxWidth().height(84.dp)
                ) {
                    Text(if (resting) "Dinleniyorsun…" else "✓  ${kg.toNum()?.clean() ?: kg} × $reps  ·  Set tamam", fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HintText(nextName?.let { "Sonraki: $it" } ?: "Son hareket", Modifier.weight(1f))
                    if (nextName != null) TextButton(onClick = onSkip) { Text("Bu hareketi geç →") }
                }
                if (remaining >= 2 && !resting) Row {
                    TextButton(onClick = { burstKey++; onCompleteRemaining() }, modifier = Modifier.fillMaxWidth()) {
                        Text("✓✓ Kalan $remaining seti ${kg.toNum()?.clean() ?: kg} × $reps ile tamamla", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (burst.value in 0.01f..0.99f) {
                val v = burst.value
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "✓", fontSize = 140.sp, fontWeight = FontWeight.Black, color = FitColors.Lime,
                        modifier = Modifier.graphicsLayer {
                            val s = 0.6f + 0.6f * kotlin.math.min(1f, v * 3f)
                            scaleX = s; scaleY = s
                            alpha = if (v < 0.4f) 1f else 1f - (v - 0.4f) / 0.6f
                        }
                    )
                }
            }
          }
        }
    }
}

@Composable
private fun BigAdjust(unit: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = onMinus, shape = RoundedCornerShape(22.dp), modifier = Modifier.size(76.dp).semantics { contentDescription = "$unit azalt" }, contentPadding = PaddingValues(0.dp)) {
            Text("−", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
            // Rakam artınca yukarı, azalınca aşağı kayar (sayaç gibi)
            androidx.compose.animation.AnimatedContent(
                targetState = value.ifBlank { "0" },
                transitionSpec = {
                    val up = (targetState.replace(',', '.').toDoubleOrNull() ?: 0.0) > (initialState.replace(',', '.').toDoubleOrNull() ?: 0.0)
                    (androidx.compose.animation.slideInVertically { h -> if (up) h else -h } + androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutVertically { h -> if (up) -h else h } + androidx.compose.animation.fadeOut())
                },
                label = "num"
            ) { v -> Text(v, fontSize = 64.sp, fontWeight = FontWeight.Black) }
            Text(unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onPlus, shape = RoundedCornerShape(22.dp), modifier = Modifier.size(76.dp).semantics { contentDescription = "$unit artır" }, contentPadding = PaddingValues(0.dp)) {
            Text("+", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
    }
}


/** Plato görülünce somut öneri (ağırlığı kendisi değiştirmez; kullanıcı isterse uygular). */
@Composable
private fun PlateauCoach(exercise: String, currentKg: Double?, onLight: (Double) -> Unit) {
    val step = com.fittakip.app.data.weightStep(exercise)
    val light = currentKg?.let { (Math.round(it * 0.9 / step) * step).coerceAtLeast(0.0) }
    Surface(
        shape = RoundedCornerShape(18.dp), color = FitColors.Orange.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, FitColors.Orange.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🧭 3+ haftadır yerinde sayıyorsun", style = MaterialTheme.typography.titleSmall, color = FitColors.Orange)
            Text("Koçun önerisi:", style = MaterialTheme.typography.bodyMedium)
            Text("1. Bu hafta hafif hafta yap: ağırlığı %10 azalt" + (light?.let { " (${it.clean()} kg)" } ?: "") + ", her seti temiz ve tam tekrarla bitir.", style = MaterialTheme.typography.bodySmall)
            Text("2. Sonraki hafta tekrar aralığını değiştir (ör. 8–12 yerine 6–8 daha ağır) ya da tutuşu/makineyi değiştir.", style = MaterialTheme.typography.bodySmall)
            Text("3. Uyku ve proteini kontrol et; plato çoğu zaman toparlanma eksikliğidir.", style = MaterialTheme.typography.bodySmall)
            HintText("Ağrı ya da eklem rahatsızlığı varsa zorlama, ağırlığı düşür.")
            if (light != null && light > 0) TextButton(onClick = { onLight(light) }) { Text("Bugün hafif hafta: ${light.clean()} kg yaz") }
        }
    }
}
