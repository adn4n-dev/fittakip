package com.fittakip.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.FAST_PLANS
import com.fittakip.app.domain.GameState
import kotlinx.coroutines.delay

@Composable
fun ChallengeCard(g: GameState) {
    val c = g.challenge
    SectionCard("Haftalık meydan okuma", icon = Icons.Rounded.EmojiEvents, action = {
        Pill(if (c.done) "Tamam ✅" else "+${c.xp} XP", if (c.done) FitColors.Lime else FitColors.Orange)
    }) {
        Text(c.title, style = MaterialTheme.typography.titleMedium)
        ProgressBar(c.progress, if (c.done) FitColors.Lime else FitColors.Orange, 10.dp)
        val cur = if (c.current >= 100) "%,d".format(c.current.toLong()).replace(',', '.') else c.current.clean()
        val tgt = if (c.target >= 100) "%,d".format(c.target.toLong()).replace(',', '.') else c.target.clean()
        HintText("$cur / $tgt ${c.unit} · her pazartesi yeni bir görev gelir")
    }
}

@Composable
fun CheckinCard(g: GameState, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🧘", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Günlük check-in", style = MaterialTheme.typography.titleMedium)
                HintText("Uyku, ruh hali, enerji, adım, kafein, takviye ve not · ${g.checkinDone}/${g.checkinTotal}")
                ProgressBar(g.checkinDone.toFloat() / g.checkinTotal.coerceAtLeast(1), FitColors.Blue)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InsightsCard(g: GameState) {
    SectionCard("Senin için içgörüler", icon = Icons.Rounded.Lightbulb) {
        g.insights.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun FastingCard(vm: AppViewModel) {
    val fasts by vm.fasts.collectAsStateWithLifecycle()
    val active = fasts.firstOrNull { it.end == null }
    var plan by remember { mutableIntStateOf(16) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active?.id) {
        while (active != null) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val done = fasts.filter { it.end != null }
    SectionCard("Aralıklı oruç", icon = Icons.Rounded.Schedule) {
        if (active == null) {
            HintText("Son yemeğini yediysen orucu başlat. Hedef süre dolunca kutlama gelir.")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FAST_PLANS.forEach { h ->
                    SelectCard(plan == h, { plan = h }, "$h:${24 - h}", Modifier.weight(1f))
                }
            }
            PrimaryButton("$plan saatlik orucu başlat", onClick = { vm.startFast(plan) })
            if (done.isNotEmpty()) {
                val longest = done.maxOf { (it.end!! - it.start) / 3_600_000.0 }
                HintText("${done.size} oruç tamamlandı · en uzun ${longest.fmt()} saat · son: ${((done.first().end!! - done.first().start) / 3_600_000.0).fmt()} saat")
            }
        } else {
            val elapsedH = (now - active.start) / 3_600_000.0
            val prog = (elapsedH / active.goalHours).toFloat().coerceIn(0f, 1f)
            val ring = FitColors.Lime
            val track = MaterialTheme.colorScheme.surfaceContainerHigh
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val s = 10.dp.toPx()
                        val tl = Offset(s / 2, s / 2)
                        val sz = Size(size.width - s, size.height - s)
                        drawArc(track, -90f, 360f, false, tl, sz, style = Stroke(s))
                        drawArc(ring, -90f, 360f * prog, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
                    }
                    Text("%${(prog * 100).toInt()}", style = MaterialTheme.typography.titleMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val h = elapsedH.toInt()
                    val m = ((elapsedH - h) * 60).toInt()
                    Text("${h} sa ${m} dk", style = MaterialTheme.typography.headlineMedium)
                    HintText("Hedef ${active.goalHours} saat")
                    val left = active.goalHours - elapsedH
                    HintText(if (left > 0) "Bitmesine ${left.fmt()} saat" else "Hedef tamam! İstediğinde bitir.")
                }
            }
            SecondaryButton("Orucu bitir", onClick = { vm.endFast() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
