package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.domain.Goal
import com.fittakip.app.domain.ProgressAnalyzer
import com.fittakip.app.domain.WeightTrend

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Kilo", longDayLabel(vm.today)) }

        item {
            if (trend == null) {
                EmptyState(
                    Icons.Rounded.MonitorWeight,
                    "İlk tartını gir",
                    "Birkaç gün tartıldıktan sonra trendini, haftalık değişimi ve hedefe varış tahminini göreceksin."
                )
            } else {
                HeroWeight(trend, weights, vm.today)
            }
        }

        item {
            FitCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField(input, { input = it }, "Bugünkü kilon (kg)", Modifier.weight(1f))
                    val v = input.toNum()
                    FilledIconButton(
                        onClick = { vm.addWeight(v!!); input = "" },
                        enabled = v != null && v in 30.0..300.0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) { Icon(Icons.Rounded.Check, contentDescription = "Kaydet") }
                }
                HintText("Her sabah tuvaletten sonra, kahvaltıdan önce tartıl. Günlük ±1 kg oynama normal, trende bak.")
            }
        }

        trend?.let { t ->
            item {
                FitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Lightbulb, FitColors.Orange, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(t.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (t.history.size >= 2) {
                item {
                    SectionCard("Grafik", icon = Icons.Rounded.Insights) {
                        TrendChart(t.history, t.fit, t.fitStartDay)
                    }
                }
            }
        }

        if (weights.isNotEmpty()) {
            item { Text("Geçmiş", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
        }
        val rows = weights.mapIndexed { i, w -> w to (if (i > 0) w.kg - weights[i - 1].kg else null) }.reversed()
        items(rows, key = { it.first.id }) { (w, delta) -> WeightRow(w, delta) { vm.deleteWeight(w) } }
    }
}

@Composable
private fun HeroWeight(t: WeightTrend, weights: List<BodyWeight>, today: Long) {
    FitCard {
        HintText("Trend kilo (günlük dalgalanma süzülmüş)")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.smoothed.fmt(), style = MaterialTheme.typography.displayMedium)
            Text(
                " kg",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            t.kgPerWeek?.let {
                Pill("${it.signed(2)} kg/hafta", if (it < 0) FitColors.Blue else FitColors.Orange, Modifier.padding(bottom = 10.dp))
            }
        }
        HintText("Son tartı: ${t.current.fmt()} kg")

        val target = t.target
        val start = weights.firstOrNull()?.kg
        if (target != null && start != null && start != target) {
            val progress = ((start - t.smoothed) / (start - target)).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    HintText("Başlangıç ${start.clean()} kg", Modifier.weight(1f))
                    Text(
                        "%${(progress * 100).toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    HintText("Hedef ${target.clean()} kg")
                }
                ProgressBar(progress, MaterialTheme.colorScheme.primary, 10.dp)
            }
            t.etaDays?.let { d ->
                MetricTile(
                    "Bu tempoyla tahmini varış",
                    "${dayLabel(today + d, withYear = true)} · ≈ ${(d / 7.0).fmt(0)} hafta",
                    Modifier.fillMaxWidth(),
                    MaterialTheme.colorScheme.primary
                )
            }
        } else if (target == null) {
            HintText("Kalori sekmesinde profiline hedef kilo eklersen ilerleme çubuğu ve varış tarihi de çıkar.")
        }
    }
}

@Composable
private fun WeightRow(w: BodyWeight, delta: Double?, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${w.kg.clean()} kg", style = MaterialTheme.typography.titleSmall)
                HintText(dayLabel(w.epochDay, withYear = true))
            }
            delta?.let {
                Text(
                    it.signed(1),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        it < 0 -> FitColors.Blue
                        it > 0 -> FitColors.Orange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
