package com.fittakip.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.ExerciseProgress
import com.fittakip.app.domain.ProgressAnalyzer
import com.fittakip.app.domain.Verdict

@Composable
private fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.IMPROVING -> FitColors.Lime
    Verdict.PLATEAU -> FitColors.Orange
    Verdict.DECLINING -> FitColors.Red
    Verdict.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun ProgressScreen(vm: AppViewModel, onOpenExercise: (String) -> Unit = {}) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val game by vm.game.collectAsStateWithLifecycle()
    val bwNow = vm.profile.collectAsStateWithLifecycle().value.profile?.weightKg ?: 0.0
    val progress = remember(sets, bwNow) { ProgressAnalyzer.exercises(sets, bwNow) }
    val intensity = remember(sets) { sets.groupBy { it.epochDay }.mapValues { it.value.size } }
    val state by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Gelişim", "Son 8 hafta") }

        if (progress.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Insights,
                    "Henüz analiz yok",
                    "Antrenman sekmesinden en az 3 farklı günde set girince her hareket için gelişim tahmini burada çıkar."
                )
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CountCard("Gelişen", progress.count { it.verdict == Verdict.IMPROVING }, FitColors.Lime, Modifier.weight(1f))
                    CountCard("Plato", progress.count { it.verdict == Verdict.PLATEAU }, FitColors.Orange, Modifier.weight(1f))
                    CountCard("Düşen", progress.count { it.verdict == Verdict.DECLINING }, FitColors.Red, Modifier.weight(1f))
                }
            }
            item {
                SectionCard("Antrenman takvimi", icon = Icons.Rounded.Insights) {
                    Heatmap(intensity, vm.today)
                    HintText("Son 12 hafta. Her kare bir gün, renk o günkü set sayısını gösterir.")
                }
            }
            game?.let { g ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CountCard("Toplam rekor", g.prCount, FitColors.Orange, Modifier.weight(1f))
                        CountCard("Ton kaldırdın", (g.totalVolume / 1000).toInt(), FitColors.Lime, Modifier.weight(1f))
                    }
                }
            }
            // Deload önerisi
            val stuck = progress.count { it.verdict == Verdict.PLATEAU || it.verdict == Verdict.DECLINING }
            if (progress.count { it.fit != null } >= 3 && stuck >= (progress.count { it.fit != null } + 1) / 2) {
                item {
                    SectionCard("Deload haftası zamanı olabilir 🛌") {
                        Text("Hareketlerinin çoğu platoda ya da düşüşte ($stuck / ${progress.count { it.fit != null }}).", style = MaterialTheme.typography.bodyMedium)
                        HintText("Bir hafta ağırlıkları %40-50 azalt, set sayısını yarıya indir. Vücut toparlanınca genelde rekorlar geri gelir.")
                    }
                }
            }
            game?.let { g ->
                val ms = g.thisWeek.muscleSets
                if (ms.isNotEmpty()) {
                    item {
                        SectionCard("Haftalık set hedefi (kas başına 10-20)") {
                            com.fittakip.app.data.MUSCLES.forEach { m ->
                                val n = ms[m] ?: 0
                                val color = when { n >= 10 -> FitColors.Lime; n >= 5 -> FitColors.Orange; else -> MaterialTheme.colorScheme.onSurfaceVariant }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row { Text(m, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Text("$n set", color = color, style = MaterialTheme.typography.labelLarge) }
                                    ProgressBar(n / 20f, color, 6.dp)
                                }
                            }
                            HintText("Isınma setleri sayılmaz. 10 setin altı kas gelişimi için genelde az kalır.")
                        }
                    }
                }
            }
            item {
                SectionCard("Kişisel rekor tablosu 🏆") {
                    progress.sortedByDescending { it.best }.forEach { e ->
                        Row(Modifier.fillMaxWidth().clickable { onOpenExercise(e.exercise) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(e.exercise, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${e.best.fmt()} kg", style = MaterialTheme.typography.titleSmall)
                            Text("  ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HintText("Detay, tüm seanslar ve rekor günleri için harekete dokun.")
                }
            }
            items(progress, key = { it.exercise.lowercase() }) { ExerciseCard(it) { onOpenExercise(it.exercise) } }
        }
        item { HowItWorks() }
    }
}

@Composable
private fun CountCard(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp)) {
            Text("$count", style = MaterialTheme.typography.headlineMedium, color = color)
            HintText(label)
        }
    }
}

@Composable
private fun ExerciseCard(p: ExerciseProgress, onOpen: () -> Unit) {
    val color = verdictColor(p.verdict)
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
            Text("${p.exercise} ›", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Pill(p.verdict.short, color)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(p.best.fmt(), style = MaterialTheme.typography.headlineLarge)
            Text(
                " kg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            HintText("en iyi tahmini 1RM", Modifier.padding(bottom = 6.dp))
        }

        val fit = p.fit
        if (fit != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Haftalık", "%${p.weeklyChangePct.signed()}", Modifier.weight(1f), color)
                MetricTile("4 hafta sonra", "${(p.projected4Weeks ?: p.best).fmt()} kg", Modifier.weight(1f))
                MetricTile("Güven", p.confidence, Modifier.weight(1f))
            }
        } else {
            HintText(p.verdict.label)
        }

        TrendChart(p.history, fit, p.fitStartDay)

        if (p.verdict == Verdict.PLATEAU) {
            HintText("💡 Tekrar aralığını değiştir, bir deload haftası dene ya da uyku, protein ve kaloriyi kontrol et.")
        }
    }
}

@Composable
private fun HowItWorks() {
    var open by rememberSaveable { mutableStateOf(false) }
    FitCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Rounded.Info, FitColors.Blue, 32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Tahmin nasıl yapılıyor?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(open) {
            HintText(
                "Her antrenman gününde o hareketteki en iyi setinden tahmini 1RM hesaplanır " +
                    "(Epley: kg × (1 + tekrar / 30)). Böylece 80×8 ile 90×5'i karşılaştırabiliyoruz. " +
                    "Son 8 haftaya doğrusal trend çizilir: haftada %0,5'ten hızlı artış gelişim, " +
                    "−%0,5'ten hızlı azalma düşüş, arası plato sayılır. Turuncu kesikli çizgi 4 hafta sonrası tahmini. " +
                    "Güven, verinin trende ne kadar oturduğunu (R²) ve gün sayısını gösterir."
            )
        }
    }
}
