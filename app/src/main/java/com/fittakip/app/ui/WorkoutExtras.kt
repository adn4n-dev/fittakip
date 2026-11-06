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
import com.fittakip.app.data.PlanItem
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

private val SCHEMES = listOf(3 to 10, 4 to 8, 5 to 5, 3 to 12, 4 to 6, 3 to 15)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTemplateDialog(onDismiss: () -> Unit, onSave: (String, List<PlanItem>) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf(listOf<PlanItem>()) }
    var muscle by remember { mutableStateOf(MUSCLES.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kendi antrenmanın") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextInput(name, { name = it }, "Ad (ör. Göğüs-Kol)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MUSCLES.forEach { m -> Pill(m, if (m == muscle) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = m }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EXERCISES.filter { it.muscle == muscle }.forEach { e ->
                        val on = chosen.any { it.exercise == e.name }
                        Pill(if (on) "✓ ${e.name}" else "+ ${e.name}", if (on) FitColors.Orange else MaterialTheme.colorScheme.onSurface, onClick = {
                            chosen = if (on) chosen.filter { it.exercise != e.name } else chosen + PlanItem(e.name, 3, 10)
                        })
                    }
                }
                if (chosen.isEmpty()) HintText("Hareketleri sırayla seç. Set × tekrar hedefini sonra dokunarak değiştirebilirsin.")
                chosen.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${item.exercise}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Pill(item.label, FitColors.Lime, onClick = {
                            val idx = SCHEMES.indexOfFirst { it.first == item.sets && it.second == item.reps }
                            val next = SCHEMES[(idx + 1) % SCHEMES.size]
                            chosen = chosen.toMutableList().also { l -> l[i] = item.copy(sets = next.first, reps = next.second) }
                        })
                    }
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
fun LibrarySheet(onDismiss: () -> Unit, onDetail: (String) -> Unit, onPick: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var muscle by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        LazyColumn(
            Modifier.fillMaxHeight(0.9f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Hareket kütüphanesi", style = MaterialTheme.typography.headlineMedium) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Tümü", if (muscle == null) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = null })
                    MUSCLES.forEach { m -> Pill(m, if (m == muscle) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { muscle = m }) }
                }
            }
            items(EXERCISES.filter { muscle == null || it.muscle == muscle }, key = { it.name }) { e ->
                Surface(onClick = { onPick(e.name) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(e.name, style = MaterialTheme.typography.titleSmall)
                            HintText(e.muscle)
                            FORM_TIPS[e.name]?.let { HintText("💡 $it") }
                        }
                        IconButton(onClick = { onDetail(e.name) }) { Icon(Icons.Rounded.Info, "Geçmiş ve detay", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
