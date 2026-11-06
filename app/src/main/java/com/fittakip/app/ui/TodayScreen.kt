package com.fittakip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.AppSettings
import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.OFF_DAY_TIPS
import com.fittakip.app.data.WEEKDAY_NAMES
import com.fittakip.app.data.WorkoutSet
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.GameState
import com.fittakip.app.domain.Gamification
import java.time.LocalDate
import java.time.LocalTime

/** Bugün ekranındaki gizlenebilir kartlar. */
val TODAY_CARDS = listOf(
    "LEVEL" to "Seviye ve XP",
    "QUESTS" to "Günlük görevler",
    "CHALLENGE" to "Haftalık meydan okuma",
    "QUICK" to "Hızlı özet",
    "CHECKIN" to "Günlük check-in",
    "FASTING" to "Aralıklı oruç",
    "INSIGHTS" to "İçgörüler",
    "BADGES" to "Son rozetler",
    "QUOTE" to "Günün sözü"
)

@Composable
fun TodayScreen(vm: AppViewModel, onTab: (Int) -> Unit, onOpen: (Sub) -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val g = game
    val today = vm.today
    fun shown(id: String) = id !in settings.hiddenCards

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
                Box(Modifier.weight(1f)) { ScreenHeader(greeting, longDayLabel(today)) }
                IconButton(onClick = { onOpen(Sub.ACHIEVEMENTS) }) { Icon(Icons.Rounded.EmojiEvents, "Başarımlar") }
                IconButton(onClick = { onOpen(Sub.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Ayarlar") }
            }
        }
        if (g == null) return@LazyColumn

        item { PlanHero(settings, sets, today, onStart = { vm.startTodayWorkout(); onTab(2) }, onSetup = { onOpen(Sub.PROGRAM) }) }
        item { WeekStrip(settings, g, today) { onOpen(Sub.PROGRAM) } }
        item { StreakCard(g, today) }
        if (shown("QUESTS")) item { QuestsCard(g) }
        if (shown("QUICK")) item {
            val p = state.profile
            val target = p?.let { Calculator.calculate(it, settings).target }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile("Kalori", if (target != null) "${(target - g.todayKcal).coerceAtLeast(0)}" else "${g.todayKcal}",
                    if (target != null) "kcal kaldı" else "kcal", FitColors.Lime, Modifier.weight(1f)) { onTab(1) }
                QuickTile("Su", (g.todayWater / 1000.0).fmt(1), "/ ${(g.waterGoal / 1000.0).fmt(1)} L", FitColors.Blue, Modifier.weight(1f)) { onTab(1) }
                QuickTile("Set", "${g.todaySets}", "bugün", FitColors.Orange, Modifier.weight(1f)) { onTab(2) }
            }
        }
        if (shown("LEVEL")) item { LevelCard(g) { onOpen(Sub.ACHIEVEMENTS) } }
        if (shown("CHALLENGE")) item { ChallengeCard(g) }
        if (shown("CHECKIN") && settings.checkinItems.isNotEmpty()) item { CheckinCard(g) { onOpen(Sub.HABITS) } }
        if (shown("FASTING")) item { FastingCard(vm) }
        if (shown("INSIGHTS") && g.insights.isNotEmpty()) item { InsightsCard(g) }
        if (shown("BADGES")) {
            val recent = g.achievements.filter { it.unlocked }.takeLast(6).reversed()
            if (recent.isNotEmpty()) item { RecentBadges(recent.map { it.emoji to it.title }) { onOpen(Sub.ACHIEVEMENTS) } }
        }
        if (shown("QUOTE")) item {
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Haftalık rapor", onClick = { onOpen(Sub.REPORT) }, icon = Icons.Rounded.Assessment, modifier = Modifier.weight(1f))
                SecondaryButton("İstatistik", onClick = { onOpen(Sub.STATS) }, icon = Icons.Rounded.BarChart, modifier = Modifier.weight(1f))
            }
        }
        item { HintText("Bu ekranda hangi kartların görüneceğini Ayarlar > Bugün ekranı'ndan seçebilirsin.", Modifier.fillMaxWidth()) }
    }
}

/** Günün programı: antrenman günü, off day ya da plan yok. */
@Composable
private fun PlanHero(st: AppSettings, sets: List<WorkoutSet>, today: Long, onStart: () -> Unit, onSetup: () -> Unit) {
    val dow = LocalDate.ofEpochDay(today).dayOfWeek.value - 1
    val dayName = WEEKDAY_NAMES[dow]
    val plan = st.planFor(today)
    val tpl = st.templateFor(today)
    val dark = Color(0xFF0E1200)
    val darkMuted = Color(0xFF0E1200).copy(alpha = 0.65f)

    when {
        plan == OFF_DAY -> GlowCard(colors = listOf(Color(0xFF8E9BFF), Color(0xFF4DE1E1))) {
            Text(dayName.uppercase(), color = darkMuted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
            Text("Off day 😌", color = dark, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp))
            Text("Bugün antrenman yok. Kaslar dinlenirken büyür.", color = dark, style = MaterialTheme.typography.bodyLarge)
            OFF_DAY_TIPS.shuffled(java.util.Random(today)).take(3).forEach {
                Text("• $it", color = dark, style = MaterialTheme.typography.bodyMedium)
            }
            nextWorkout(st, today)?.let { (d, n) -> Text("Sıradaki: $d · $n", color = darkMuted, style = MaterialTheme.typography.labelLarge) }
        }
        tpl != null -> GlowCard {
            Text(dayName.uppercase(), color = darkMuted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
            Text("${tpl.name} günü 💪", color = dark, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp))
            val todaySets = sets.filter { it.epochDay == today && !it.warmup }
            val doneCount = tpl.items.count { i -> todaySets.count { it.exercise.equals(i.exercise, true) } >= i.sets }
            Text("Bugünkü görevin · $doneCount/${tpl.items.size} tamam", color = darkMuted, style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tpl.items.forEach { item ->
                    val done = todaySets.count { it.exercise.equals(item.exercise, true) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (done >= item.sets) "✅" else "◻️", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (item.superset > 0) "S${item.superset} · " else "") + item.exercise,
                                color = dark, style = MaterialTheme.typography.titleSmall
                            )
                            if (item.note.isNotBlank()) Text(item.note, color = darkMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("${done.coerceAtMost(item.sets)}/${item.sets} × ${item.repsLabel}", color = dark, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = dark, contentColor = FitColors.Lime),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(if (doneCount == 0) "Antrenmana başla" else "Antrenmana devam et", fontWeight = FontWeight.ExtraBold)
            }
        }
        else -> FitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.CalendarMonth, FitColors.Lime, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (st.hasPlan) "$dayName: plan yok" else "Haftalık programını kur", style = MaterialTheme.typography.titleLarge)
                    HintText(
                        if (st.hasPlan) "Bugüne bir antrenman ya da off day ata."
                        else "Her gün sana o günün antrenmanını ya da dinlenme gününü söyleyeyim."
                    )
                }
            }
            PrimaryButton(if (st.hasPlan) "Programı düzenle" else "Program oluştur", onClick = onSetup, icon = Icons.Rounded.CalendarMonth)
        }
    }
}

private fun nextWorkout(st: AppSettings, today: Long): Pair<String, String>? {
    for (i in 1..7) {
        val d = today + i
        val t = st.templateFor(d) ?: continue
        val name = if (i == 1) "Yarın" else WEEKDAY_NAMES[LocalDate.ofEpochDay(d).dayOfWeek.value - 1]
        return name to t.name
    }
    return null
}

/** Haftanın 7 günü: plan + o gün kayıt var mı. */
@Composable
private fun WeekStrip(st: AppSettings, g: GameState, today: Long, onClick: () -> Unit) {
    if (!st.hasPlan) return
    val ws = Gamification.weekStart(today)
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..6) {
                val day = ws + i
                val p = st.plan[i]
                val isToday = day == today
                val trained = day in g.thisWeekTrainDays
                val label = when {
                    p == OFF_DAY -> "Off"
                    p.isEmpty() -> "—"
                    else -> p.take(4)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isToday) FitColors.Lime.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                        .border(1.dp, if (isToday) FitColors.Lime else Color.Transparent, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(shortDayName(day), style = MaterialTheme.typography.labelSmall, color = if (isToday) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        when {
                            trained -> "✅"
                            p == OFF_DAY -> "😌"
                            p.isEmpty() -> "·"
                            day < today -> "✖️"
                            else -> "🏋️"
                        }, fontSize = 15.sp
                    )
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun StreakCard(g: GameState, today: Long) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(FitColors.Orange.copy(alpha = 0.45f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) { Text("🔥", fontSize = 40.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${g.streak}", style = MaterialTheme.typography.displayMedium)
                    Text(" gün seri", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                HintText(
                    when {
                        today in g.streakDays -> "Bugün kaydını yaptın, seri güvende ✅"
                        g.streak > 0 -> "Bir yemek, set ya da tartı gir; serin bozulmasın!"
                        else -> "Bugün bir kayıt gir ve yeni seri başlat."
                    }
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${g.longestStreak}", style = MaterialTheme.typography.titleLarge)
                HintText("en uzun")
                if (g.freezes > 0) Pill("🧊 ${g.freezes}", FitColors.Blue)
            }
        }
    }
}

@Composable
private fun LevelCard(g: GameState, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        FitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(FitColors.Lime, Color(0xFF4DE1C1)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${g.level}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF0E1200))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.levelTitle, style = MaterialTheme.typography.titleMedium)
                    HintText("Seviye ${g.level} · toplam ${g.xp} XP")
                }
                Text("${g.xpIntoLevel} / ${g.xpForLevel}", style = MaterialTheme.typography.labelLarge, color = FitColors.Lime)
            }
            ProgressBar(g.xpIntoLevel.toFloat() / g.xpForLevel.coerceAtLeast(1), FitColors.Lime, 10.dp)
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
    }
}

@Composable
private fun RecentBadges(items: List<Pair<String, String>>, onAll: () -> Unit) {
    SectionCard("Son rozetlerin", action = { TextButton(onClick = onAll) { Text("Tümü") } }) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { (e, t) ->
                Column(
                    Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(e, fontSize = 28.sp)
                    Text(t, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun QuickTile(label: String, value: String, unit: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.05f)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            HintText(unit)
        }
    }
}
