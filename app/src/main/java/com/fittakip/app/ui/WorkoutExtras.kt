package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittakip.app.data.EXERCISES
import com.fittakip.app.data.FORM_TIPS
import com.fittakip.app.data.MUSCLES
import com.fittakip.app.data.Equipment
import com.fittakip.app.data.PlanItem
import com.fittakip.app.data.WorkoutTemplate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay

/** Antrenman kronometresi: başlat, süre akar, bitirince oturum kaydedilir. */
@Composable
fun SessionCard(start: Long, vm: AppViewModel) {
    if (start == 0L) {
        SecondaryButton("Antrenmanı başlat (kronometre)", onClick = { vm.startSession() }, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
        return
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val sec = ((now - start) / 1000).coerceAtLeast(0)
    Surface(shape = RoundedCornerShape(28.dp), color = FitColors.Blue.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = FitColors.Blue)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Antrenman sürüyor", style = MaterialTheme.typography.titleSmall)
                Text(
                    "%d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60),
                    style = MaterialTheme.typography.headlineMedium, color = FitColors.Blue, fontWeight = FontWeight.Black
                )
            }
            TextButton(onClick = { vm.cancelSession() }) { Text("İptal") }
            FilledIconButton(onClick = { vm.finishSession() }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Rounded.Stop, "Bitir") }
        }
    }
}

/** Set × tekrar seçenekleri (min–max). Dokundukça sıradakine geçer. */
private val SCHEMES = listOf(
    Triple(3, 10, 10), Triple(3, 8, 12), Triple(4, 8, 12), Triple(3, 6, 10), Triple(4, 6, 10), Triple(3, 8, 10),
    Triple(3, 10, 12), Triple(3, 12, 15), Triple(4, 12, 15), Triple(3, 15, 15), Triple(5, 5, 5), Triple(4, 6, 8)
)

/** Antrenman düzenleyici: yeni oluşturma ya da var olanı (hazır programlar dahil) değiştirme. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTemplateDialog(
    initial: WorkoutTemplate? = null,
    unavailable: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (String, List<PlanItem>) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var chosen by remember { mutableStateOf(initial?.items ?: listOf()) }
    var muscle by remember { mutableStateOf(MUSCLES.first()) }
    var adding by remember { mutableStateOf(initial == null) }
    fun update(i: Int, f: (PlanItem) -> PlanItem) { chosen = chosen.toMutableList().also { l -> l[i] = f(l[i]) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yeni antrenman" else "${initial.name} düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextInput(name, { name = it }, "Ad (ör. Upper A)")
                chosen.forEachIndexed { i, item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ${item.exercise}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (i > 0) Text("▲", Modifier.clickable { chosen = chosen.toMutableList().also { l -> val t = l[i]; l[i] = l[i - 1]; l[i - 1] = t } }.padding(6.dp))
                            if (i < chosen.size - 1) Text("▼", Modifier.clickable { chosen = chosen.toMutableList().also { l -> val t = l[i]; l[i] = l[i + 1]; l[i + 1] = t } }.padding(6.dp))
                            Text("✕", Modifier.clickable { chosen = chosen.filterIndexed { j, _ -> j != i } }.padding(6.dp), color = FitColors.Red)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pill(item.label, FitColors.Lime, onClick = {
                                val idx = SCHEMES.indexOfFirst { it.first == item.sets && it.second == item.reps && it.third == item.repsMax }
                                val n = SCHEMES[(idx + 1) % SCHEMES.size]
                                update(i) { it.copy(sets = n.first, reps = n.second, repsMax = n.third) }
                            })
                            if (i < chosen.size - 1) {
                                val linked = item.superset > 0 && chosen[i + 1].superset == item.superset
                                Pill(if (linked) "⚡ Süperset ✓" else "⚡ Alttakiyle süperset", if (linked) FitColors.Orange else MaterialTheme.colorScheme.onSurfaceVariant, onClick = {
                                    if (linked) {
                                        update(i) { it.copy(superset = 0) }; update(i + 1) { it.copy(superset = 0) }
                                    } else {
                                        val g = (chosen.maxOfOrNull { it.superset } ?: 0) + 1
                                        update(i) { it.copy(superset = g) }; update(i + 1) { it.copy(superset = g) }
                                    }
                                })
                            }
                        }
                        if (unavailable.any { it.equals(item.exercise, true) }) HintText("⚠️ Salonunda yok olarak işaretli")
                        TextInput(item.note, { v -> update(i) { it.copy(note = v.replace(Regex("[:;|]"), " ")) } }, "Not (ör. son set drop, geniş tutuş)")
                    }
                }
                if (adding) {
                    Text("Hareket ekle", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MUSCLES.forEach { m -> Pill(m, if (m == muscle) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = m }) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EXERCISES.filter { it.muscle == muscle && unavailable.none { u -> u.equals(it.name, true) } }.forEach { e ->
                            val on = chosen.any { it.exercise == e.name }
                            Pill((if (on) "✓ " else "+ ") + e.name, if (on) FitColors.Orange else MaterialTheme.colorScheme.onSurface, onClick = {
                                chosen = if (on) chosen.filter { it.exercise != e.name } else chosen + PlanItem(e.name, 3, 8, 12)
                            })
                        }
                    }
                } else {
                    TextButton(onClick = { adding = true }) { Text("+ Hareket ekle") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), chosen) }, enabled = name.isNotBlank() && chosen.isNotEmpty()) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibrarySheet(
    unavailable: Set<String>,
    onToggleAvailable: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDetail: (String) -> Unit,
    onPick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var muscle by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<Equipment?>(null) }
    var editGym by remember { mutableStateOf(false) }
    fun available(n: String) = unavailable.none { it.equals(n, true) }
    val list = EXERCISES.filter { (muscle == null || it.muscle == muscle) && (equipment == null || it.equipment == equipment) }
        .filter { editGym || available(it.name) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        LazyColumn(
            Modifier.fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hareketler", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    Pill(if (editGym) "Bitti" else "⚙️ Salonumu düzenle", if (editGym) FitColors.Lime else FitColors.Orange, onClick = { editGym = !editGym })
                }
            }
            if (editGym) item {
                HintText("Salonunda olmayan makinelerin işaretini kaldır. Öneriler ve listeler sadece salonundakileri gösterir; programındaki bir makine yoksa yerine alternatif önerilir.")
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Tüm ekipman", if (equipment == null) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { equipment = null })
                    Equipment.entries.forEach { e -> Pill("${e.emoji} ${e.label}", if (e == equipment) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { equipment = e }) }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Tüm kaslar", if (muscle == null) FitColors.Blue else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = null })
                    MUSCLES.forEach { m -> Pill(m, if (m == muscle) FitColors.Blue else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = m }) }
                }
            }
            item { HintText("${list.size} hareket" + if (!editGym && unavailable.isNotEmpty()) " · ${unavailable.size} makine salonunda yok olarak gizlendi" else "") }
            items(list, key = { it.name }) { e ->
                val on = available(e.name)
                Surface(
                    onClick = { if (editGym) onToggleAvailable(e.name, !on) else onPick(e.name) },
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = if (on) 0.05f else 0.02f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(e.name, style = MaterialTheme.typography.titleSmall, color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            HintText("${e.equipment.emoji} ${e.equipment.label} · ${e.muscle}")
                            if (!editGym) FORM_TIPS[e.name]?.let { HintText("💡 $it") }
                        }
                        if (editGym) {
                            Switch(checked = on, onCheckedChange = { onToggleAvailable(e.name, it) })
                        } else {
                            IconButton(onClick = { onDetail(e.name) }) { Icon(Icons.Rounded.Info, "Geçmiş ve detay", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}
