package com.fittakip.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.random.Random

private data class Piece(val x: Float, val speed: Float, val size: Float, val color: Color, val spin: Float, val phase: Float)

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val colors = listOf(FitColors.Lime, FitColors.Orange, FitColors.Blue, FitColors.Red, Color.White)
    val pieces = remember {
        List(70) {
            Piece(
                Random.nextFloat(), 0.4f + Random.nextFloat() * 0.8f, 6f + Random.nextFloat() * 8f,
                colors.random(), Random.nextFloat() * 720f, Random.nextFloat()
            )
        }
    }
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "t"
    )
    Canvas(modifier) {
        pieces.forEach { p ->
            val prog = (t * p.speed + p.phase) % 1f
            val x = p.x * size.width + kotlin.math.sin((prog * 6 + p.phase * 10).toDouble()).toFloat() * 18f
            val y = prog * (size.height + 40f) - 20f
            rotate(p.spin * prog, Offset(x, y)) {
                drawRect(p.color, Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}

@Composable
fun CelebrationDialog(c: Celebration, onDismiss: () -> Unit) {
    val (emoji, title, body) = when (c) {
        is Celebration.PR -> Triple(
            "🏆", "Yeni kişisel rekor!",
            "${c.exercise}: tahmini 1RM ${c.newBest.fmt()} kg\n(önceki en iyi ${c.oldBest.fmt()} kg, +${(c.newBest - c.oldBest).fmt()} kg)"
        )
        is Celebration.Badge -> Triple(c.achievement.emoji, "Rozet kazandın!", "${c.achievement.title}\n${c.achievement.desc}")
        is Celebration.LevelUp -> Triple("⭐", "Seviye ${c.level}!", "Yeni unvanın: ${c.title}\nBöyle devam!")
        is Celebration.Custom -> Triple(c.emoji, c.title, c.body)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box {
                Confetti(Modifier.matchParentSize())
                Column(
                    Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(emoji, fontSize = 64.sp)
                    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton("Harika!", onClick = onDismiss)
                    (c as? Celebration.Custom)?.shareText?.let { text ->
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        TextButton(onClick = {
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }
                            ctx.startActivity(android.content.Intent.createChooser(send, "Antrenmanını paylaş"))
                        }) { Text("📤 Antrenmanını paylaş") }
                    }
                }
            }
        }
    }
}
