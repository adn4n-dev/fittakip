package com.fittakip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.ExerciseProgress
import com.fittakip.app.domain.ProgressAnalyzer
import com.fittakip.app.domain.Verdict

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val sets by vm.sets.collectAsStateWithLifecycle()
    val progress = remember(sets) { ProgressAnalyzer.exercises(sets) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("Gelişim") }
        item {
            SectionCard("Tahmin nasıl yapılıyor?") {
                HintText(
                    "Her antrenman gününde o hareketteki en iyi setinden tahmini 1RM hesaplanır " +
                        "(Epley: kg × (1 + tekrar / 30)). Böylece 80×8 ile 90×5'i karşılaştırabiliyoruz. " +
                        "Son 8 haftaya doğrusal trend çizilir: haftada %0,5'ten hızlı artış = gelişim, " +
                        "-%0,5'ten hızlı azalma = düşüş, arası = plato. Kesikli çizgi 4 hafta sonrası tahmini."
                )
            }
        }
        if (progress.isEmpty()) {
            item { HintText("Antrenman sekmesinden set girdikçe her hareket için analiz burada çıkacak.") }
        }
        items(progress, key = { it.exercise.lowercase() }) { ExerciseCard(it) }
    }
}

@Composable
private fun ExerciseCard(p: ExerciseProgress) {
    val verdictColor = when (p.verdict) {
        Verdict.IMPROVING -> Color(0xFF2E9B4F)
        Verdict.PLATEAU -> Color(0xFFE08A00)
        Verdict.DECLINING -> MaterialTheme.colorScheme.error
        Verdict.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    SectionCard(p.exercise) {
        Text(p.verdict.label, color = verdictColor, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBox("En iyi 1RM", "${p.best.fmt()} kg")
            p.fit?.let {
                StatBox("Haftalık", "%${p.weeklyChangePct.signed()}")
                p.projected4Weeks?.let { proj -> StatBox("4 hafta sonra", "${proj.fmt()} kg") }
            }
        }
        p.fit?.let { fit ->
            HintText("Tahmin güvenilirliği: ${p.confidence} (R² ${fit.r2.fmt(2)}, ${fit.n} antrenman günü)")
        }
        if (p.verdict == Verdict.PLATEAU) {
            HintText("Öneri: tekrar aralığını değiştir, deload haftası dene ya da uyku/protein/kaloriyi kontrol et.")
        }
        TrendChart(p.history, p.fit, p.fitStartDay)
    }
}
