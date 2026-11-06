package com.fittakip.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittakip.app.domain.LinearFit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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

fun longDayLabel(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMMM EEEE", TR))

// ---------- Yerleşim ----------

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        subtitle?.let {
            Text(
                it.uppercase(TR),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        }
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                IconBadge(icon)
                Spacer(Modifier.width(12.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        content()
    }
}

@Composable
fun IconBadge(icon: ImageVector, color: Color = MaterialTheme.colorScheme.primary, size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String) {
    FitCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconBadge(icon, size = 56.dp)
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    var m = modifier
        .clip(shape)
        .background(color.copy(alpha = 0.16f))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Text(
        text,
        modifier = m.padding(horizontal = 12.dp, vertical = 6.dp),
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

// ---------- Girdiler ----------

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}

@Composable
fun NumField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/** − [değer] + şeklinde, spor salonunda tek elle kullanılabilen sayı girişi. */
@Composable
fun Stepper(value: String, onChange: (String) -> Unit, label: String, step: Double, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onChange(max(0.0, (value.toNum() ?: 0.0) - step).clean()) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "Azalt")
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onChange(((value.toNum() ?: 0.0) + step).clean()) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Artır")
            }
        }
    }
}

@Composable
fun SelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, if (selected) accent else Color.Transparent)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

// ---------- Göstergeler ----------

@Composable
fun CalorieRing(eaten: Int, target: Int, modifier: Modifier = Modifier) {
    val remaining = target - eaten
    val over = remaining < 0
    val progress by animateFloatAsState(
        if (target > 0) (eaten.toFloat() / target).coerceIn(0f, 1f) else 0f,
        label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(modifier.size(156.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = color, startAngle = 135f, sweepAngle = 270f * progress, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${abs(remaining)}", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (over) "kcal fazla" else "kcal kaldı",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MacroBar(label: String, eaten: Double, target: Int, color: Color) {
    val progress by animateFloatAsState(
        if (target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f,
        label = "macro"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${eaten.roundToInt()} / $target g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ProgressBar(progress, color)
    }
}

@Composable
fun ProgressBar(progress: Float, color: Color, height: Dp = 8.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/**
 * Dolu çizgi + altında degrade: gerçek veriler.
 * Turuncu kesikli çizgi: regresyon trendi, [projectDays] gün ileriye uzatılmış tahmin.
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
    val trendColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotFill = MaterialTheme.colorScheme.surfaceContainer

    val minDay = points.first().first
    val lastDay = points.last().first
    val hasTrend = fit != null && fitStartDay != null
    val endDay = if (hasTrend) lastDay + projectDays else lastDay
    val trendStart = if (hasTrend) fitStartDay!! to fit!!.at(0.0) else null
    val trendEnd = if (hasTrend) endDay to fit!!.at((endDay - fitStartDay!!).toDouble()) else null

    val ys = points.map { it.second } + listOfNotNull(trendStart?.second, trendEnd?.second)
    val rawMin = ys.min()
    val rawMax = ys.max()
    val pad = ((rawMax - rawMin) * 0.15).takeIf { it > 0 } ?: 1.0
    val lo = rawMin - pad
    val hi = rawMax + pad

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(vertical = 6.dp)
        ) {
            val spanX = (endDay - minDay).coerceAtLeast(1).toFloat()
            fun px(day: Long): Float = (day - minDay) / spanX * size.width
            fun py(v: Double): Float = (size.height * (1 - (v - lo) / (hi - lo))).toFloat()

            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(
                    gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
            }

            val line = Path()
            val fill = Path()
            points.forEachIndexed { i, (d, v) ->
                if (i == 0) {
                    line.moveTo(px(d), py(v))
                    fill.moveTo(px(d), size.height)
                    fill.lineTo(px(d), py(v))
                } else {
                    line.lineTo(px(d), py(v))
                    fill.lineTo(px(d), py(v))
                }
            }
            fill.lineTo(px(lastDay), size.height)
            fill.close()
            drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)))

            if (trendStart != null && trendEnd != null) {
                drawLine(
                    trendColor,
                    Offset(px(trendStart.first), py(trendStart.second)),
                    Offset(px(trendEnd.first), py(trendEnd.second)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                )
                drawCircle(trendColor, radius = 5.dp.toPx(), center = Offset(px(trendEnd.first), py(trendEnd.second)))
            }

            drawPath(
                line, lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEach { (d, v) ->
                val c = Offset(px(d), py(v))
                drawCircle(lineColor, radius = 5.dp.toPx(), center = c)
                drawCircle(dotFill, radius = 2.5.dp.toPx(), center = c)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HintText(dayLabel(minDay))
            HintText("${rawMin.fmt()} – ${rawMax.fmt()} $unit")
            HintText(dayLabel(endDay))
        }
        if (hasTrend) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 14.dp, height = 3.dp)
                        .background(trendColor)
                )
                Spacer(Modifier.width(6.dp))
                HintText("Trend ve 4 haftalık tahmin")
            }
        }
    }
}
