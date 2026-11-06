package com.fittakip.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Basit sütun grafiği; [target] verilirse kesikli hedef çizgisi çizer. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, color: Color, target: Double? = null, highlightLast: Boolean = true) {
    if (values.isEmpty()) return
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val targetColor = MaterialTheme.colorScheme.secondary
    val maxV = maxOf(values.maxOrNull() ?: 0.0, target ?: 0.0).takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val n = values.size
            val slot = size.width / n
            val barW = slot * 0.56f
            values.forEachIndexed { i, v ->
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(10f, 10f))
                val h = (v / maxV * size.height).toFloat()
                if (h > 0f) {
                    val c = if (highlightLast && i == n - 1) color else color.copy(alpha = 0.55f)
                    drawRoundRect(c, Offset(x, size.height - h), Size(barW, h), CornerRadius(10f, 10f))
                }
            }
            if (target != null) {
                val y = (size.height - target / maxV * size.height).toFloat()
                drawLine(
                    targetColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun shortDayName(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("tr"))

/** Son [weeks] haftanın antrenman yoğunluğu (GitHub tarzı). */
@Composable
fun Heatmap(intensity: Map<Long, Int>, today: Long, weeks: Int = 12) {
    val lime = FitColors.Lime
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    val start = LocalDate.ofEpochDay(today).with(java.time.DayOfWeek.MONDAY).toEpochDay() - (weeks - 1) * 7
    val maxI = (intensity.values.maxOrNull() ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            for (w in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    for (d in 0 until 7) {
                        val day = start + w * 7 + d
                        val v = intensity[day] ?: 0
                        val color = when {
                            day > today -> Color.Transparent
                            v == 0 -> empty
                            else -> lime.copy(alpha = 0.3f + 0.7f * v / maxI)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HintText("Az")
            listOf(0.3f, 0.55f, 0.8f, 1f).forEach {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(lime.copy(alpha = it))
                )
            }
            HintText("Çok")
        }
    }
}

/** Yatay çubuklar: etiket + değer. */
@Composable
fun HBars(items: List<Pair<String, Double>>, color: Color, unit: String) {
    val maxV = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, v) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${v.toLong()} $unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProgressBar((v / maxV).toFloat(), color)
            }
        }
    }
}
