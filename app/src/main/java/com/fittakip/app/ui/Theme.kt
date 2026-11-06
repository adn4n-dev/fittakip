package com.fittakip.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fittakip.app.data.ACCENTS

object FitColors {
    val Bg = Color(0xFF0E0F12)
    val Card = Color(0xFF17191E)
    val CardHigh = Color(0xFF22252C)
    val Line = Color(0xFF2C2F37)

    /** Vurgu rengi temaya göre değişir; okuyan her ekran otomatik yenilenir. */
    var Lime by mutableStateOf(Color(0xFFC6F432))

    val Orange = Color(0xFFFF8A50)
    val Blue = Color(0xFF5AB0FF)
    val Red = Color(0xFFFF5C6C)
    val Text = Color(0xFFF2F3F5)
    val Muted = Color(0xFF8D929C)
}

private val base = Typography()
private val typography = base.copy(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun FitTheme(accentId: String = "LIME", content: @Composable () -> Unit) {
    val accent = Color(ACCENTS.firstOrNull { it.id == accentId }?.color ?: 0xFFC6F432)
    androidx.compose.runtime.SideEffect { if (FitColors.Lime != accent) FitColors.Lime = accent }
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF151A00),
        secondary = FitColors.Orange,
        onSecondary = Color(0xFF2A0E00),
        tertiary = FitColors.Blue,
        onTertiary = Color(0xFF001A33),
        background = FitColors.Bg,
        onBackground = FitColors.Text,
        surface = FitColors.Bg,
        onSurface = FitColors.Text,
        surfaceVariant = FitColors.CardHigh,
        onSurfaceVariant = FitColors.Muted,
        surfaceContainerLowest = FitColors.Bg,
        surfaceContainerLow = FitColors.Card,
        surfaceContainer = FitColors.Card,
        surfaceContainerHigh = FitColors.CardHigh,
        surfaceContainerHighest = FitColors.CardHigh,
        secondaryContainer = accent.copy(alpha = 0.18f),
        onSecondaryContainer = accent,
        outline = FitColors.Line,
        outlineVariant = FitColors.Line,
        error = FitColors.Red
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
