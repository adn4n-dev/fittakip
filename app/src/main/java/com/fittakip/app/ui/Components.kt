package com.fittakip.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.os.Build
import com.fittakip.app.domain.LinearFit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Tema ----------

@Composable
fun FitTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF7BD88F), tertiary = Color(0xFFFFB86B))
        else -> lightColorScheme(primary = Color(0xFF1B7F3B), tertiary = Color(0xFFB65C00))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---------- Biçimlendirme ----------

private val TR = Locale.forLanguageTag("tr")

/** Türkçe klavyedeki virgülü de kabul eder. */
fun String.toNum(): Double? = replace(',', '.').trim().toDoubleOrNull()

fun Double.fmt(decimals: Int = 1): String = String.format(TR, "%.${decimals}f", this)

fun Double.signed(decimals: Int = 1): String = (if (this > 0) "+" else "") + fmt(decimals)

/** 80.0 -> "80", 82.5 -> "82,5" */
fun Double.clean(): String =
    if (this % 1.0 == 0.0) toLong().toString() else fmt(2).trimEnd('0').trimEnd(',')

fun dayLabel(epochDay: Long, withYear: Boolean = false): String =
    LocalDate.ofEpochDay(epochDay).format(
        DateTimeFormatter.ofPattern(if (withYear) "d MMMM yyyy" else "d MMM, EEE", TR)
    )

// ---------- Ortak bileşenler ----------

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun NumField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------- Grafik ----------

/**
 * Düz çizgi: gerçek veriler. Kesikli çizgi: regresyon trendi, [projectDays] gün ileriye uzatılmış tahmin.
 */
@Composable
fun TrendChart(
    points: List<Pair<Long, Double>>,
    fit: LinearFit?,
    fitStartDay: Long?,
    projectDays: Int = 28,
    unit: String = "kg"
) {
    if (points.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    val trendColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val minDay = points.first().first
    val lastDay = points.last().first
    val hasTrend = fit != null && fitStartDay != null
    val endDay = if (hasTrend) lastDay + projectDays else lastDay
    val trendStart = if (hasTrend) fitStartDay!! to fit!!.at(0.0) else null
    val trendEnd = if (hasTrend) endDay to fit!!.at((endDay - fitStartDay!!).toDouble()) else null

    val ys = points.map { it.second } + listOfNotNull(trendStart?.second, trendEnd?.second)
    val rawMin = ys.min()
    val rawMax = ys.max()
    val pad = ((rawMax - rawMin) * 0.1).takeIf { it > 0 } ?: 1.0
    val lo = rawMin - pad
    val hi = rawMax + pad

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
        ) {
            val spanX = (endDay - minDay).coerceAtLeast(1).toFloat()
            fun px(day: Long): Float = (day - minDay) / spanX * size.width
            fun py(v: Double): Float = (size.height * (1 - (v - lo) / (hi - lo))).toFloat()

            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            val path = Path()
            points.forEachIndexed { i, (d, v) ->
                if (i == 0) path.moveTo(px(d), py(v)) else path.lineTo(px(d), py(v))
            }
            drawPath(
                path, lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEach { (d, v) -> drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(px(d), py(v))) }

            if (trendStart != null && trendEnd != null) {
                drawLine(
                    trendColor,
                    Offset(px(trendStart.first), py(trendStart.second)),
                    Offset(px(trendEnd.first), py(trendEnd.second)),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dayLabel(minDay), style = MaterialTheme.typography.labelSmall)
            Text("${rawMin.fmt()}–${rawMax.fmt()} $unit", style = MaterialTheme.typography.labelSmall)
            Text(dayLabel(endDay), style = MaterialTheme.typography.labelSmall)
        }
    }
}
