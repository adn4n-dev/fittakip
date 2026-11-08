package com.fittakip.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Basılınca hafifçe içe göçer, bırakınca yaylanarak geri gelir. */
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.96f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Ekrana ilk girişte hafifçe aşağıdan yukarı kayarak belirme (≈280 ms). */
fun Modifier.enterUp(): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(280)) }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 14.dp.toPx()
    }
}

/** Yüklenirken içerik şeklinde parlayan iskelet. */
@Composable
fun Shimmer(modifier: Modifier = Modifier, height: Dp = 18.dp, widthFraction: Float = 1f) {
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        -1f, 2f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "x"
    )
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)),
                    start = Offset(x * 600f, 0f), end = Offset(x * 600f + 400f, 0f)
                )
            )
    )
}

/** Liste satırı iskeleti (yemek arama, fotoğraf analizi). */
@Composable
fun ShimmerRows(count: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        repeat(count) { i ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Shimmer(Modifier.size(40.dp), height = 40.dp, widthFraction = 1f)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Shimmer(height = 14.dp, widthFraction = if (i % 2 == 0) 0.7f else 0.55f)
                    Shimmer(height = 10.dp, widthFraction = 0.4f)
                }
            }
        }
    }
}
