package com.fittakip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.GameState
import java.time.LocalTime

@Composable
fun TodayScreen(vm: AppViewModel, onTab: (Int) -> Unit, onOpen: (Sub) -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val g = game

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenHeader("$greeting 👋", longDayLabel(vm.today)) }
                IconButton(onClick = { onOpen(Sub.ACHIEVEMENTS) }) { Icon(Icons.Rounded.EmojiEvents, "Başarımlar") }
                IconButton(onClick = { onOpen(Sub.REPORT) }) { Icon(Icons.Rounded.Assessment, "Haftalık rapor") }
                IconButton(onClick = { onOpen(Sub.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Ayarlar") }
            }
        }
        if (g == null) return@LazyColumn

        item { StreakCard(g, vm.today) }
        item { LevelCard(g) { onOpen(Sub.ACHIEVEMENTS) } }
        item { QuestsCard(g) }
        item {
            val p = state.profile
            val target = p?.let { Calculator.calculate(it, settings.kcalAdjust).target }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile(
                    "Kalori", if (target != null) "${(target - g.todayKcal).coerceAtLeast(0)}" else "${g.todayKcal}",
                    if (target != null) "kcal kaldı" else "kcal", FitColors.Lime, Modifier.weight(1f)
                ) { onTab(1) }
                QuickTile("Su", (g.todayWater / 1000.0).fmt(1), "/ ${(g.waterGoal / 1000.0).fmt(1)} L", FitColors.Blue, Modifier.weight(1f)) { onTab(1) }
                QuickTile("Set", "${g.todaySets}", "bugün", FitColors.Orange, Modifier.weight(1f)) { onTab(2) }
            }
        }
        item {
            FitCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Haftalık antrenman hedefi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${g.weekWorkoutDays} / ${g.weekGoal}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (g.weekWorkoutDays >= g.weekGoal) FitColors.Lime else MaterialTheme.colorScheme.onSurface
                    )
                }
                ProgressBar(g.weekWorkoutDays.toFloat() / g.weekGoal.coerceAtLeast(1), FitColors.Lime, 10.dp)
                HintText(
                    if (g.weekWorkoutDays >= g.weekGoal) "Bu haftanın hedefi tamam! Ekstra her gün bonus." else
                        "Hedefe ${g.weekGoal - g.weekWorkoutDays} antrenman kaldı."
                )
            }
        }
        val recent = g.achievements.filter { it.unlocked }.takeLast(6).reversed()
        if (recent.isNotEmpty()) {
            item {
                SectionCard("Son rozetlerin", action = { TextButton(onClick = { onOpen(Sub.ACHIEVEMENTS) }) { Text("Tümü") } }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recent) { a ->
                            Column(
                                Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(a.emoji, fontSize = 28.sp)
                                Text(a.title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
        item {
            FitCard {
                Row {
                    Icon(Icons.Rounded.FormatQuote, null, tint = FitColors.Orange, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(g.quote, style = MaterialTheme.typography.titleMedium)
                        HintText("Günün sözü")
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(g: GameState, today: Long) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 44.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${g.streak}", style = MaterialTheme.typography.displayMedium)
                    Text(
                        " günlük seri", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                HintText(
                    when {
                        today in g.activeDays -> "Bugün kaydını yaptın, seri güvende ✅"
                        g.streak > 0 -> "Bugün bir şey kaydet, serin bozulmasın!"
                        else -> "Bugün bir kayıt gir ve yeni seri başlat."
                    }
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${g.longestStreak}", style = MaterialTheme.typography.titleLarge)
                HintText("en uzun")
            }
        }
        // Bu haftanın günleri
        val ws = com.fittakip.app.domain.Gamification.weekStart(today)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val day = ws + i
                val active = day in g.activeDays
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    active -> FitColors.Orange
                                    day == today -> FitColors.Orange.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) Text("🔥", fontSize = 14.sp)
                    }
                    Text(
                        shortDayName(day), style = MaterialTheme.typography.labelSmall,
                        color = if (day == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(g: GameState, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FitColors.Lime),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${g.level}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.levelTitle, style = MaterialTheme.typography.titleMedium)
                    HintText("Seviye ${g.level} · toplam ${g.xp} XP")
                }
                Text("${g.xpIntoLevel} / ${g.xpForLevel}", style = MaterialTheme.typography.labelLarge, color = FitColors.Lime)
            }
            ProgressBar(g.xpIntoLevel.toFloat() / g.xpForLevel.coerceAtLeast(1), FitColors.Lime, 10.dp)
            HintText("Her kayıt XP kazandırır: yemek 5, set 3, tartı 10, antrenman günü 25, rekor 40, görev 20.")
        }
    }
}

@Composable
private fun QuestsCard(g: GameState) {
    val done = g.quests.count { it.done }
    SectionCard("Günlük görevler", action = { Pill("$done / ${g.quests.size}", if (done == g.quests.size) FitColors.Lime else FitColors.Orange) }) {
        g.quests.forEach { q ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (q.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                    tint = if (q.done) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(q.title, style = MaterialTheme.typography.titleSmall, color = if (q.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    HintText(q.detail)
                }
                Text("+${q.xp} XP", style = MaterialTheme.typography.labelMedium, color = if (q.done) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (done == g.quests.size) HintText("Bugünün tüm görevleri tamam 🎉")
    }
}

@Composable
private fun QuickTile(label: String, value: String, unit: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            HintText(unit)
        }
    }
}
