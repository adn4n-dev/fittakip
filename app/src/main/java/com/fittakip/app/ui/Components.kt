package com.fittakip.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.compositeOver
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
    Column(Modifier.padding(top = 10.dp, bottom = 6.dp)) {
        subtitle?.let {
            Text(
                it.uppercase(TR),
                style = MaterialTheme.typography.labelMedium,
                color = FitColors.Lime.copy(alpha = 0.85f),
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp))
    }
}

/** Cam efektli kart: hafif degrade dolgu + ince parlak kenar. */
private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .enterUp()
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1F26), Color(0xFF15171C))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))), CardShape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

/** Vurgulu kahraman kartı: tema renginden maviye degrade, koyu yazı. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(FitColors.Lime, Color(0xFF4DE1C1)),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .enterUp()
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(colors))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Ekran arkasında yavaşça nefes alan renk ışıltıları. */
@Composable
fun GlowBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val accent = FitColors.Lime
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        0.85f, 1.15f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "p"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(FitColors.Bg)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.1f, 0f), radius = size.width * 0.9f * pulse),
                    radius = size.width * 0.9f * pulse, center = Offset(size.width * 0.1f, 0f)
                )
                drawCircle(
                    Brush.radialGradient(listOf(FitColors.Blue.copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width, size.height * 0.35f), radius = size.width * 0.8f / pulse),
                    radius = size.width * 0.8f / pulse, center = Offset(size.width, size.height * 0.35f)
                )
            },
        content = content
    )
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
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
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
        .border(1.dp, color.copy(alpha = 0.22f), shape)
    if (onClick != null) {
        val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        m = Modifier.pressScale(src).then(m).clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)
    }
    Text(
        text,
        modifier = m.padding(horizontal = 14.dp, vertical = if (onClick != null) 10.dp else 6.dp),   // dokunulabilenler parmak boyunda
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
            .background(Color.White.copy(alpha = 0.05f))
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
                Icon(Icons.Rounded.Remove, contentDescription = "$label azalt")
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
                Icon(Icons.Rounded.Add, contentDescription = "$label artır")
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
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f), tween(220), label = "sbg")
    val bd by animateColorAsState(if (selected) accent else Color.White.copy(alpha = 0.06f), tween(220), label = "sbd")
    Surface(
        onClick = onClick,
        modifier = modifier.pressScale(src),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.5.dp, bd),
        interactionSource = src
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
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val brush = if (enabled) Brush.horizontalGradient(listOf(FitColors.Lime, FitColors.Lime.copy(alpha = 0.75f).compositeOver(Color(0xFF4DE1C1))))
    else Brush.horizontalGradient(listOf(FitColors.CardHigh, FitColors.CardHigh))
    Button(
        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = shape,
        interactionSource = src,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
            contentColor = Color(0xFF0E1200), disabledContentColor = FitColors.Muted
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier
            .pressScale(src)
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(brush)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        interactionSource = src,
        modifier = modifier.pressScale(src).height(52.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Göstergeler ----------

/** İç içe üç halka (kalori, protein, su) ortada kalan kalori. */
@Composable
fun TripleRing(
    kcal: Float, protein: Float, water: Float,
    centerTop: String, centerBottom: String, overColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    val a by animateFloatAsState(kcal.coerceIn(0f, 1f), tween(900), label = "k")
    val b by animateFloatAsState(protein.coerceIn(0f, 1f), tween(1000), label = "p")
    val c by animateFloatAsState(water.coerceIn(0f, 1f), tween(1100), label = "w")
    val kColor = if (overColor) FitColors.Red else FitColors.Lime
    val track = Color.White.copy(alpha = 0.06f)
    Box(modifier.size(172.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val gap = 4.dp.toPx()
            listOf(Triple(a, kColor, 0), Triple(b, FitColors.Blue, 1), Triple(c, Color(0xFF4DE1E1), 2)).forEach { (v, col, i) ->
                val inset = stroke / 2 + i * (stroke + gap)
                val tl = Offset(inset, inset)
                val sz = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(track, -90f, 360f, false, tl, sz, style = Stroke(stroke))
                // Renk geçişi halkanın başladığı yerden (saat 12) ucuna kadar: sönükten parlağa, kırılma yok.
                if (v > 0f) rotate(-90f) {
                    drawArc(
                        Brush.sweepGradient(0f to col.copy(alpha = 0.55f), v.coerceAtLeast(0.01f) to col, 1f to col),
                        0f, 360f * v, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Sayı ise sayarak değişsin (ör. kalan kalori 1450 → 1180)
            val n = centerTop.toIntOrNull()
            val counted by androidx.compose.animation.core.animateIntAsState(n ?: 0, tween(700), label = "count")
            Text(if (n != null) counted.toString() else centerTop, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(centerBottom, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 120f), label = "bar")
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth(p)
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
