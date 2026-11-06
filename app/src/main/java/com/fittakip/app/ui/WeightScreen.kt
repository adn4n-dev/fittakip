package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.Goal
import com.fittakip.app.domain.ProgressAnalyzer

@Composable
fun WeightScreen(vm: AppViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val profile = state.profile
    val trend = remember(weights, profile) {
        ProgressAnalyzer.analyzeWeight(weights, profile?.targetWeightKg, Goal.from(profile?.goal))
    }
    var input by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("Kilo takibi") }

        item {
            SectionCard("Bugünün tartısı") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(input, { input = it }, "Kilo (kg)", Modifier.weight(1f))
                    val v = input.toNum()
                    Button(
                        onClick = { vm.addWeight(v!!); input = "" },
                        enabled = v != null && v in 30.0..300.0
                    ) { Text("Kaydet") }
                }
                HintText("Her sabah tuvaletten sonra, kahvaltıdan önce tartıl. Günlük ±1 kg oynama normal — trende bak.")
            }
        }

        trend?.let { t ->
            item {
                SectionCard("Trend") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBox("Son tartı", "${t.current.fmt()} kg")
                        StatBox("Trend kilo", "${t.smoothed.fmt()} kg")
                        t.kgPerWeek?.let { StatBox("Haftalık", "${it.signed(2)} kg") }
                    }
                    Text(t.message, style = MaterialTheme.typography.bodyMedium)
                    t.etaDays?.let { d ->
                        Text(
                            "Bu tempoyla ${t.target!!.clean()} kg hedefine tahmini varış: " +
                                "${dayLabel(vm.today + d, withYear = true)} (≈ ${(d / 7.0).fmt(0)} hafta)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (t.target == null) HintText("Profilde hedef kilo girersen tahmini varış tarihini de hesaplarım.")
                    TrendChart(t.history, t.fit, t.fitStartDay)
                }
            }
        }

        if (weights.isNotEmpty()) item { Text("Geçmiş", style = MaterialTheme.typography.titleSmall) }
        items(weights.reversed(), key = { it.id }) { w ->
            ListItem(
                headlineContent = { Text("${w.kg.clean()} kg") },
                supportingContent = { Text(dayLabel(w.epochDay, withYear = true)) },
                trailingContent = {
                    IconButton(onClick = { vm.deleteWeight(w) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil")
                    }
                }
            )
        }
    }
}
