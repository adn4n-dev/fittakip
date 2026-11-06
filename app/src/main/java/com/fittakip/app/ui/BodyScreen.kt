package com.fittakip.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.fittakip.app.data.ProgressPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.BodyMeasure
import com.fittakip.app.data.BodyWeight
import com.fittakip.app.data.Profile
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Goal
import com.fittakip.app.domain.ProgressAnalyzer
import com.fittakip.app.domain.WeightTrend

@Composable
fun BodyScreen(vm: AppViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle()
    val measures by vm.measures.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile = state.profile
    val goal = Goal.from(profile?.goal)
    val trend = remember(weights, profile) { ProgressAnalyzer.analyzeWeight(weights, profile?.targetWeightKg, goal) }
    var input by rememberSaveable { mutableStateOf("") }
    var measureOpen by rememberSaveable { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    fun show(id: String) = id !in settings.hiddenSections

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Vücut", longDayLabel(vm.today)) }

        item {
            if (trend == null) {
                EmptyState(
                    Icons.Rounded.MonitorWeight, "İlk tartını gir",
                    "Birkaç gün tartıldıktan sonra trendini, haftalık değişimi ve hedefe varış tahminini göreceksin."
                )
            } else {
                HeroWeight(trend, weights, vm.today)
            }
        }

        item {
            FitCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField(input, { input = it }, "Bugünkü kilon (kg)", Modifier.weight(1f))
                    val v = input.toNum()
                    FilledIconButton(
                        onClick = { vm.addWeight(v!!); input = "" },
                        enabled = v != null && v in 30.0..300.0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) { Icon(Icons.Rounded.Check, contentDescription = "Kaydet") }
                }
                HintText("Her sabah tuvaletten sonra, kahvaltıdan önce tartıl. Günlük ±1 kg oynama normal, trende bak.")
            }
        }

        // Akıllı kalori ayarı
        trend?.let { t ->
            if (show("B_ADVICE")) item {
                FitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Lightbulb, FitColors.Orange, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(t.message, style = MaterialTheme.typography.bodyMedium)
                    }
                    val wk = t.kgPerWeek
                    val suggest = when {
                        wk == null -> 0
                        goal == Goal.CUT && wk > -0.2 -> -150
                        goal == Goal.CUT && wk < -1.0 -> 200
                        goal == Goal.BULK && wk < 0.1 -> 200
                        goal == Goal.BULK && wk > 0.6 -> -150
                        goal == Goal.MAINTAIN && wk > 0.2 -> -150
                        goal == Goal.MAINTAIN && wk < -0.2 -> 150
                        else -> 0
                    }
                    if (suggest != 0) {
                        PrimaryButton(
                            "Kalori hedefini ${if (suggest > 0) "+" else ""}$suggest kcal ayarla",
                            onClick = { vm.updateSettings { it.copy(kcalAdjust = it.kcalAdjust + suggest) } }
                        )
                    }
                    if (settings.kcalAdjust != 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HintText("Şu anki düzeltme: ${if (settings.kcalAdjust > 0) "+" else ""}${settings.kcalAdjust} kcal", Modifier.weight(1f))
                            TextButton(onClick = { vm.updateSettings { it.copy(kcalAdjust = 0) } }) { Text("Sıfırla") }
                        }
                    }
                }
            }
            if (t.history.size >= 2) {
                if (show("B_CHART")) item {
                    SectionCard("Kilo grafiği", icon = Icons.Rounded.Insights) {
                        TrendChart(t.history, t.fit, t.fitStartDay)
                    }
                }
            }
        }

        // BMI ve yağ oranı
        if (profile != null) {
            if (show("B_COMPOSITION")) item { CompositionCard(profile, weights.lastOrNull()?.kg ?: profile.weightKg, measures.lastOrNull()) }
        }

        if (show("B_PHOTOS")) item { PhotosCard(vm) }

        // Ölçüler
        if (show("B_MEASURES")) item {
            if (measureOpen) {
                MeasureForm(onClose = { measureOpen = false }) {
                    vm.addMeasure(it)
                    measureOpen = false
                }
            } else {
                MeasuresCard(measures, onAdd = { measureOpen = true }, onDelete = { vm.deleteMeasure(it) })
            }
        }

        if (weights.isNotEmpty()) {
            if (show("B_HISTORY")) item { Text("Tartı geçmişi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
        }
        val rows = weights.mapIndexed { i, w -> w to (if (i > 0) w.kg - weights[i - 1].kg else null) }.reversed().take(30)
        if (show("B_HISTORY")) items(rows, key = { it.first.id }) { (w, delta) -> WeightRow(w, delta) { vm.deleteWeight(w) } }
        item { SecondaryButton("Kartları düzenle", onClick = { picker = true }, icon = Icons.Rounded.Tune, modifier = Modifier.fillMaxWidth()) }
    }
    if (picker) CardPicker("Vücut kartları", BODY_CARDS, settings.hiddenSections, onToggle = { id, on ->
        vm.updateSettings { it.copy(hiddenSections = if (on) it.hiddenSections - id else it.hiddenSections + id) }
    }) { picker = false }
}

@Composable
private fun CompositionCard(p: Profile, kg: Double, m: BodyMeasure?) {
    val bmi = Calculator.bmi(kg, p.heightCm)
    val bf = Calculator.bodyFatNavy(p.male, p.heightCm, m?.waist, m?.neck, m?.hip)
    val bmiColor = when {
        bmi < 18.5 -> FitColors.Blue
        bmi < 25 -> FitColors.Lime
        bmi < 30 -> FitColors.Orange
        else -> FitColors.Red
    }
    SectionCard("Vücut kompozisyonu", icon = Icons.Rounded.MonitorWeight) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("BMI", "${bmi.fmt()} · ${Calculator.bmiCategory(bmi)}", Modifier.weight(1f), bmiColor)
            MetricTile(
                "Yağ oranı (tahmini)",
                if (bf != null) "%${bf.fmt()}" else "Ölçü gir",
                Modifier.weight(1f),
                if (bf != null) FitColors.Orange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (bf != null) {
            val lean = kg * (1 - bf / 100)
            HintText("Yağsız kütle ≈ ${lean.fmt()} kg · yağ kütlesi ≈ ${(kg - lean).fmt()} kg")
        } else {
            HintText("Yağ oranı için bel ve boyun ölçüsü gir${if (!p.male) " (kadınlarda kalça da)" else ""}. ABD Donanması formülü kullanılır.")
        }
        val (lo, hi) = Calculator.idealWeightRange(p.heightCm)
        val whr = Calculator.waistHipRatio(m?.waist, m?.hip)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Sağlıklı kilo aralığı", "${lo.fmt(0)}–${hi.fmt(0)} kg", Modifier.weight(1f))
            MetricTile(
                "Bel / kalça oranı",
                if (whr != null) "${whr.fmt(2)} · ${Calculator.whrRisk(whr, p.male)}" else "Ölçü gir",
                Modifier.weight(1f),
                if (whr != null && Calculator.whrRisk(whr, p.male) == "Yüksek risk") FitColors.Red else MaterialTheme.colorScheme.onSurface
            )
        }
        HintText("BMI kaslı kişilerde yanıltıcı olabilir; yağ oranı ve bel ölçüsü daha iyi gösterge.")
    }
}

@Composable
private fun MeasuresCard(list: List<BodyMeasure>, onAdd: () -> Unit, onDelete: (BodyMeasure) -> Unit) {
    SectionCard("Ölçüler (cm)", icon = Icons.Rounded.Straighten, action = { TextButton(onClick = onAdd) { Text("Ekle") } }) {
        if (list.isEmpty()) {
            HintText("Tartı her şeyi söylemez. Bel, göğüs ve kol ölçülerini ayda bir girersen yağ kaybı ve kas kazanımını ayrı görürsün.")
        } else {
            val first = list.first()
            val last = list.last()
            val fields = listOf<Triple<String, Double?, Double?>>(
                Triple("Bel", last.waist, first.waist), Triple("Boyun", last.neck, first.neck),
                Triple("Göğüs", last.chest, first.chest), Triple("Kol", last.arm, first.arm), Triple("Kalça", last.hip, first.hip)
            )
            fields.filter { it.second != null }.forEach { (label, now, start) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${now!!.clean()} cm", style = MaterialTheme.typography.titleSmall)
                    if (start != null && list.size > 1) {
                        val d = now - start
                        Text(
                            "  ${d.signed()}", style = MaterialTheme.typography.labelLarge,
                            color = if (d < 0) FitColors.Blue else if (d > 0) FitColors.Orange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                HintText("Son ölçüm: ${dayLabel(last.epochDay, withYear = true)} · ${list.size} kayıt", Modifier.weight(1f))
                TextButton(onClick = { onDelete(last) }) { Text("Sonu sil") }
            }
        }
    }
}

@Composable
private fun MeasureForm(onClose: () -> Unit, onSave: (BodyMeasure) -> Unit) {
    var waist by rememberSaveable { mutableStateOf("") }
    var neck by rememberSaveable { mutableStateOf("") }
    var chest by rememberSaveable { mutableStateOf("") }
    var arm by rememberSaveable { mutableStateOf("") }
    var hip by rememberSaveable { mutableStateOf("") }
    SectionCard("Yeni ölçüm", icon = Icons.Rounded.Straighten, action = { TextButton(onClick = onClose) { Text("Vazgeç") } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(waist, { waist = it }, "Bel", Modifier.weight(1f))
            NumField(neck, { neck = it }, "Boyun", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(chest, { chest = it }, "Göğüs", Modifier.weight(1f))
            NumField(arm, { arm = it }, "Kol", Modifier.weight(1f))
            NumField(hip, { hip = it }, "Kalça", Modifier.weight(1f))
        }
        HintText("Beli göbek hizasından, boynu gırtlağın hemen altından ölç. Boş bıraktıkların kaydedilmez.")
        val m = BodyMeasure(epochDay = 0, waist = waist.toNum(), neck = neck.toNum(), chest = chest.toNum(), arm = arm.toNum(), hip = hip.toNum())
        PrimaryButton(
            "Kaydet", onClick = { onSave(m) },
            enabled = listOf(m.waist, m.neck, m.chest, m.arm, m.hip).any { it != null && it in 10.0..250.0 },
            icon = Icons.Rounded.Check
        )
    }
}

@Composable
private fun HeroWeight(t: WeightTrend, weights: List<BodyWeight>, today: Long) {
    FitCard {
        HintText("Trend kilo (günlük dalgalanma süzülmüş)")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.smoothed.fmt(), style = MaterialTheme.typography.displayMedium)
            Text(
                " kg", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            t.kgPerWeek?.let {
                Pill("${it.signed(2)} kg/hafta", if (it < 0) FitColors.Blue else FitColors.Orange, Modifier.padding(bottom = 10.dp))
            }
        }
        HintText("Son tartı: ${t.current.fmt()} kg")

        val target = t.target
        val start = weights.firstOrNull()?.kg
        if (target != null && start != null && start != target) {
            val progress = ((start - t.smoothed) / (start - target)).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    HintText("Başlangıç ${start.clean()} kg", Modifier.weight(1f))
                    Text(
                        "%${(progress * 100).toInt()}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    HintText("Hedef ${target.clean()} kg")
                }
                ProgressBar(progress, MaterialTheme.colorScheme.primary, 10.dp)
            }
            t.etaDays?.let { d ->
                MetricTile(
                    "Bu tempoyla tahmini varış",
                    "${dayLabel(today + d, withYear = true)} · ≈ ${(d / 7.0).fmt(0)} hafta",
                    Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primary
                )
            }
        } else if (target == null) {
            HintText("Beslenme sekmesinde profiline hedef kilo eklersen ilerleme çubuğu ve varış tarihi de çıkar.")
        }
    }
}

@Composable
private fun WeightRow(w: BodyWeight, delta: Double?, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${w.kg.clean()} kg", style = MaterialTheme.typography.titleSmall)
                HintText(dayLabel(w.epochDay, withYear = true))
            }
            delta?.let {
                Text(
                    it.signed(1), style = MaterialTheme.typography.labelLarge,
                    color = when {
                        it < 0 -> FitColors.Blue
                        it > 0 -> FitColors.Orange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
private fun PhotosCard(vm: AppViewModel) {
    val photos by vm.photos.collectAsStateWithLifecycle()
    var compare by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<ProgressPhoto?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.addPhoto(it) } }
    SectionCard("İlerleme fotoğrafları", icon = Icons.Rounded.PhotoCamera, action = {
        TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Ekle") }
    }) {
        if (photos.isEmpty()) {
            HintText("Ayda bir aynı ışıkta, aynı pozda fotoğraf çek. Tartıda görünmeyen değişimi fotoğraflar gösterir. Fotoğraflar sadece bu telefonda saklanır.")
        } else {
            if (photos.size >= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectCard(!compare, { compare = false }, "Galeri", Modifier.weight(1f))
                    SelectCard(compare, { compare = true }, "Önce / sonra", Modifier.weight(1f))
                }
            }
            if (compare && photos.size >= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(photos.first(), photos.last()).forEachIndexed { i, ph ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            PhotoThumb(ph, Modifier.fillMaxWidth().aspectRatio(0.75f)) { viewing = ph }
                            HintText((if (i == 0) "Önce · " else "Sonra · ") + dayLabel(ph.epochDay, true))
                        }
                    }
                }
                val days = photos.last().epochDay - photos.first().epochDay
                HintText("Aradaki süre: $days gün")
            } else {
                photos.reversed().chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ph -> PhotoThumb(ph, Modifier.weight(1f).aspectRatio(0.75f)) { viewing = ph } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    viewing?.let { ph ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text(dayLabel(ph.epochDay, true)) },
            text = { PhotoThumb(ph, Modifier.fillMaxWidth().aspectRatio(0.75f), full = true) {} },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Kapat") } },
            dismissButton = { TextButton(onClick = { vm.deletePhoto(ph); viewing = null }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun PhotoThumb(ph: ProgressPhoto, modifier: Modifier, full: Boolean = false, onClick: () -> Unit) {
    val bmp by produceState<ImageBitmap?>(null, ph.path, full) {
        value = withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = if (full) 1 else 4 }
                BitmapFactory.decodeFile(ph.path, opts)?.asImageBitmap()
            } catch (_: Exception) { null }
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        bmp?.let { Image(it, contentDescription = "İlerleme fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: HintText("…")
    }
}
