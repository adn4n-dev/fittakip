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
import com.fittakip.app.domain.GameState
import kotlinx.coroutines.delay

@Composable
fun CheckinCard(g: GameState, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🧘", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Günlük check-in", style = MaterialTheme.typography.titleMedium)
                HintText("Uyku, ruh hali, adım, takviye ve not · ${g.checkinDone}/${g.checkinTotal}")
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
