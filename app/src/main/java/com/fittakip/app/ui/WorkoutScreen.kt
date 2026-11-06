package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.domain.Stats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val names = remember(sets) { sets.map { it.exercise.trim() }.distinctBy { it.lowercase() } }

    var exercise by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("20") }
    var reps by rememberSaveable { mutableStateOf("8") }
    var note by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<WorkoutSet?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Antrenman", longDayLabel(vm.today)) }

        item {
            SectionCard("Set ekle", icon = Icons.Rounded.FitnessCenter) {
                TextInput(exercise, { exercise = it }, "Hareket (ör. Bench Press)")
                val suggestions = names.filter {
                    exercise.isBlank() || (it.contains(exercise.trim(), ignoreCase = true) &&
                        !it.equals(exercise.trim(), ignoreCase = true))
                }.take(10)
                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { s ->
                            Pill(s, MaterialTheme.colorScheme.tertiary, onClick = { exercise = s })
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
                    Pill("Tahmini 1RM ≈ ${Stats.epley1RM(w!!, r!!).fmt()} kg", MaterialTheme.colorScheme.primary)
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
                HintText("Hareket ve ağırlık kayıttan sonra kalır; aynı seti tek dokunuşla tekrar girersin. Silmek için geçmişteki sete dokun.")
            }
        }

        if (sets.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.FitnessCenter,
                    "İlk setini gir",
                    "Girdiğin setler burada güne göre listelenir, gelişim analizi de Gelişim sekmesinde çıkar."
                )
            }
        }

        sets.groupBy { it.epochDay }.forEach { (day, daySets) ->
            item(key = "day-$day") {
                DayCard(
                    title = if (day == vm.today) "Bugün" else dayLabel(day),
                    sets = daySets,
                    onSetClick = { toDelete = it }
                )
            }
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Seti sil?") },
            text = {
                Text(
                    "${s.exercise} · ${s.weightKg.clean()} kg × ${s.reps}" +
                        if (s.note.isNotBlank()) "\n📝 ${s.note}" else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteSet(s); toDelete = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(title: String, sets: List<WorkoutSet>, onSetClick: (WorkoutSet) -> Unit) {
    val byExercise = sets.groupBy { it.exercise.trim() }
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            HintText("${byExercise.size} hareket · ${sets.size} set")
        }
        byExercise.forEach { (name, list) ->
            val ordered = list.reversed() // girildiği sırayla
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "≈1RM ${ordered.maxOf { Stats.epley1RM(it.weightKg, it.reps) }.fmt()} kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ordered.forEach { s ->
                        Pill(
                            "${s.weightKg.clean()} × ${s.reps}",
                            MaterialTheme.colorScheme.onSurface,
                            onClick = { onSetClick(s) }
                        )
                    }
                }
                ordered.filter { it.note.isNotBlank() }.forEach { s ->
                    HintText("📝 ${s.weightKg.clean()}×${s.reps}: ${s.note}")
                }
            }
        }
    }
}
