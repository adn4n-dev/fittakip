package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.Stats

@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val names = remember(sets) { sets.map { it.exercise.trim() }.distinctBy { it.lowercase() } }

    var exercise by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("Antrenman notları") }

        item {
            SectionCard("Set ekle") {
                OutlinedTextField(
                    value = exercise, onValueChange = { exercise = it },
                    label = { Text("Hareket (ör. Bench Press)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                val suggestions = names.filter {
                    exercise.isBlank() || (it.contains(exercise.trim(), ignoreCase = true) &&
                        !it.equals(exercise.trim(), ignoreCase = true))
                }.take(10)
                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { s ->
                            AssistChip(onClick = { exercise = s }, label = { Text(s) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(kg, { kg = it }, "Ağırlık (kg)", Modifier.weight(1f))
                    NumField(reps, { reps = it }, "Tekrar", Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Not (ops.): form, RPE, his…") },
                    modifier = Modifier.fillMaxWidth()
                )

                val w = kg.toNum()
                val r = reps.trim().toIntOrNull()
                val valid = exercise.isNotBlank() && w != null && w >= 0 && r != null && r in 1..100
                if (valid) HintText("Tahmini 1RM: ${Stats.epley1RM(w!!, r!!).fmt()} kg")

                Button(
                    onClick = {
                        vm.addSet(exercise.trim(), w!!, r!!, note.trim())
                        note = ""
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Seti kaydet") }
                HintText("Hareket ve ağırlık kayıttan sonra silinmez; aynı seti tek dokunuşla tekrar girebilirsin.")
            }
        }

        if (sets.isEmpty()) {
            item { HintText("Henüz set yok. İlk setini girince geçmişin burada, analizi Gelişim sekmesinde görünecek.") }
        }

        sets.groupBy { it.epochDay }.forEach { (day, list) ->
            item(key = "day-$day") {
                Text(
                    if (day == vm.today) "Bugün" else dayLabel(day),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(list, key = { it.id }) { s ->
                ListItem(
                    headlineContent = { Text("${s.exercise} — ${s.weightKg.clean()} kg × ${s.reps}") },
                    supportingContent = {
                        Text(buildString {
                            append("≈1RM ${Stats.epley1RM(s.weightKg, s.reps).fmt()} kg")
                            if (s.note.isNotBlank()) append(" · ${s.note}")
                        })
                    },
                    trailingContent = {
                        IconButton(onClick = { vm.deleteSet(s) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                )
            }
        }
    }
}
