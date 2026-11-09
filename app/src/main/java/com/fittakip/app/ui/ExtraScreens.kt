package com.fittakip.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.AccountCircle
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.domain.WeekStats
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

// ---------------- Başarımlar ----------------

@Composable
fun AchievementsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val g = game ?: return
    val unlocked = g.achievements.count { it.unlocked }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) { SubHeader("Başarımlar", onBack) }
        item(span = { GridItemSpan(2) }) {
            FitCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$unlocked / ${g.achievements.size}", style = MaterialTheme.typography.headlineLarge)
                        HintText("rozet açıldı")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Seviye ${g.level}", style = MaterialTheme.typography.titleLarge, color = FitColors.Lime)
                        HintText(g.levelTitle)
                    }
                }
                ProgressBar(unlocked.toFloat() / g.achievements.size, FitColors.Lime, 10.dp)
            }
        }
        items(g.achievements.sortedByDescending { if (it.unlocked) 2f else it.progress }, key = { it.id }) { a ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    Modifier
                        .padding(14.dp)
                        .alpha(if (a.unlocked) 1f else 0.75f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (a.unlocked) FitColors.Lime.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (a.unlocked) a.emoji else "🔒", fontSize = 28.sp)
                    }
                    Text(a.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                    Text(
                        a.desc, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2
                    )
                    if (!a.unlocked) {
                        ProgressBar(a.progress, FitColors.Orange, 6.dp)
                        HintText("%${(a.progress * 100).roundToInt()}")
                    } else {
                        Pill("Açıldı", FitColors.Lime)
                    }
                }
            }
        }
    }
}

// ---------------- Haftalık rapor ----------------

@Composable
fun WeeklyReportScreen(vm: AppViewModel, onBack: () -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    val g = game ?: return
    val w = g.thisWeek
    val l = g.lastWeek
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SubHeader("Haftalık rapor", onBack) }
                IconButton(onClick = { vm.share(vm.weekShareText()) }) { Icon(Icons.Rounded.Share, "Paylaş") }
            }
        }
        item {
            FitCard {
                HintText("${dayLabel(w.start)} – ${dayLabel(w.start + 6)} · geçen haftayla karşılaştırma")
                Text(verdict(w, l), style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            FitCard {
                CompareRow("Antrenman günü", w.workoutDays.toDouble(), l.workoutDays.toDouble(), "gün", higherIsBetter = true)
                CompareRow("Set", w.sets.toDouble(), l.sets.toDouble(), "", higherIsBetter = true)
                CompareRow("Toplam hacim", w.volume, l.volume, "kg", higherIsBetter = true)
                CompareRow("Kardiyo", w.cardioMin.toDouble(), l.cardioMin.toDouble(), "dk", higherIsBetter = true)
                CompareRow("Kişisel rekor", w.prs.toDouble(), l.prs.toDouble(), "", higherIsBetter = true)
                CompareRow("Su hedefi tutan gün", w.waterGoalDays.toDouble(), l.waterGoalDays.toDouble(), "gün", higherIsBetter = true)
                CompareRow("Ortalama kalori", w.avgKcal?.toDouble(), l.avgKcal?.toDouble(), "kcal", higherIsBetter = null)
                CompareRow("Ortalama protein", w.avgProtein?.toDouble(), l.avgProtein?.toDouble(), "g", higherIsBetter = true)
                CompareRow("Kilo değişimi", w.weightChange, l.weightChange, "kg", higherIsBetter = null, signedValue = true)
            }
        }
        if (w.muscleVolume.isNotEmpty()) {
            item {
                SectionCard("Kas gruplarına hacim") {
                    HBars(w.muscleVolume.toList().sortedByDescending { it.second }, FitColors.Blue, "kg")
                }
            }
        }
        item { HintText("Rapor her pazartesi sıfırlanır. Geçen haftayı geçmek için küçük bir hedef koy: bir antrenman, birkaç set ya da her gün su hedefi.") }
    }
}

private fun verdict(w: WeekStats, l: WeekStats): String = when {
    w.workoutDays == 0 && w.sets == 0 && w.avgKcal == null -> "Bu hafta henüz kayıt yok. Bugün başla, hafta senin!"
    w.workoutDays > l.workoutDays -> "Geçen haftadan daha çok antrenman yaptın. Süper gidiyorsun 🔥"
    w.volume > l.volume && l.volume > 0 -> "Hacmin arttı, kasların teşekkür ediyor 💪"
    w.workoutDays == l.workoutDays && w.workoutDays > 0 -> "Geçen haftayla aynı tempodasın. İstikrar kazandırır."
    else -> "Hafta bitmedi, geçen haftayı yakalamak için hâlâ vaktin var."
}

@Composable
private fun CompareRow(label: String, now: Double?, before: Double?, unit: String, higherIsBetter: Boolean?, signedValue: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        val text = when {
            now == null -> "—"
            signedValue -> now.signed()
            now >= 100 -> now.roundToInt().toString()
            now % 1.0 == 0.0 -> now.toLong().toString()
            else -> now.fmt()
        }
        Text("$text $unit".trim(), style = MaterialTheme.typography.titleSmall)
        if (now != null && before != null && now != before) {
            val up = now > before
            val good = higherIsBetter?.let { if (it) up else !up }
            Text(
                if (up) "  ▲" else "  ▼",
                color = when (good) {
                    true -> FitColors.Lime
                    false -> FitColors.Red
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------- Ayarlar ----------------

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onRequestNotifications: () -> Unit, onProgram: () -> Unit = {}, onStatus: () -> Unit = {}) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val state by vm.profile.collectAsStateWithLifecycle()
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportTo(it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFrom(it) }
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { vm.setAutoBackupFolder(it) } }
    val csvSets = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.exportCsv(it, "sets") } }
    val csvFoods = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.exportCsv(it, "foods") } }
    val game by vm.game.collectAsStateWithLifecycle()
    val level = game?.level ?: 1

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SubHeader("Ayarlar", onBack) }

        item {
            Surface(onClick = onStatus, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = FitColors.Lime.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🩺 Uygulama durumu", style = MaterialTheme.typography.titleSmall)
                        HintText("Barkod, market ürünleri, fotoğraf, Drive, bildirimler… hangisi çalışıyor, hangisi çalışmıyor?")
                    }
                    Text("›", style = MaterialTheme.typography.headlineSmall, color = FitColors.Lime)
                }
            }
        }
        item { AccountCard(vm, settings) }

        item {
            if (editProfile || state.profile == null) {
                ProfileForm(
                    state.profile, onCancel = { editProfile = false },
                    initialLifeStage = settings.lifeStage, initialHealth = settings.healthCondition
                ) { prof, stage, health ->
                    vm.saveProfile(prof)
                    vm.setSafety(stage, health)
                    editProfile = false
                }
            } else {
                SectionCard("Profil", icon = Icons.Rounded.Person, action = { TextButton(onClick = { editProfile = true }) { Text("Düzenle") } }) {
                    val p = state.profile!!
                    HintText("${if (p.male) "Erkek" else "Kadın"} · ${p.age} yaş · ${p.heightCm.clean()} cm · ${p.weightKg.clean()} kg" +
                        (p.targetWeightKg?.let { " · hedef ${it.clean()} kg" } ?: ""))
                }
            }
        }

        item {
            Surface(onClick = onProgram, shape = RoundedCornerShape(28.dp), color = androidx.compose.ui.graphics.Color.Transparent) {
                GlowCard {
                    Text("🗓️ Haftalık program", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color(0xFF0E1200))
                    Text("Hangi gün hangi antrenman, hangi gün off day? Dokun ve düzenle.", color = androidx.compose.ui.graphics.Color(0xFF0E1200))
                }
            }
        }

        item {
            SectionCard("Hatırlatma", icon = Icons.Rounded.Notifications) {
                ExactAlarmHint()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Seri hatırlatması", style = MaterialTheme.typography.titleSmall)
                        HintText("O gün kayıt girmediysen akşam bildirim gönderir.")
                    }
                    Switch(
                        checked = settings.remindersOn,
                        onCheckedChange = {
                            vm.setReminders(it)
                            if (it) onRequestNotifications()
                        }
                    )
                }
                if (settings.remindersOn) {
                    Stepper(
                        settings.reminderHour.toString(),
                        { v -> v.toNum()?.toInt()?.let { h -> if (h in 0..23) vm.setReminders(true, h) } },
                        "Saat (0-23)", 1.0
                    )
                    TextButton(onClick = { onRequestNotifications(); vm.testNotification() }) { Text("Deneme bildirimi gönder") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Öğün önerisi (tek dokunuşla ekle)", style = MaterialTheme.typography.titleSmall)
                        HintText("Kahvaltı, öğle ve akşam saatinde henüz girmediysen 'Her zamanki öğle yemeğin? ✓ Ekle' bildirimi; uygulamayı açmadan eklenir.")
                    }
                    Switch(checked = settings.mealNudges, onCheckedChange = { vm.setMealNudges(it); if (it) onRequestNotifications() })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sabah motivasyonu", style = MaterialTheme.typography.titleSmall)
                        HintText("Her sabah 08:30'da günün sözü ve serin.")
                    }
                    Switch(checked = settings.morningOn, onCheckedChange = { vm.setMorning(it); if (it) onRequestNotifications() })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Su hatırlatması", style = MaterialTheme.typography.titleSmall)
                        HintText("09-21 arası, hedefinin gerisindeysen 2 saatte bir.")
                    }
                    Switch(checked = settings.waterRemindOn, onCheckedChange = { vm.setWaterReminder(it); if (it) onRequestNotifications() })
                }
            }
        }

        item {
            SectionCard("Hedefler", icon = Icons.Rounded.Tune) {
                Stepper(settings.weeklyWorkoutGoal.toString(), { v ->
                    v.toNum()?.toInt()?.let { n -> if (n in 1..7) vm.updateSettings { it.copy(weeklyWorkoutGoal = n) } }
                }, "Haftalık antrenman hedefi (gün)", 1.0)
                Stepper(if (settings.waterGoalMl == 0) "0" else settings.waterGoalMl.toString(), { v ->
                    v.toNum()?.toInt()?.let { n -> if (n in 0..6000) vm.updateSettings { it.copy(waterGoalMl = n) } }
                }, "Su hedefi (ml, 0 = kiloya göre otomatik)", 250.0)
                Stepper(settings.restSeconds.toString(), { v ->
                    v.toNum()?.toInt()?.let { n -> if (n in 15..600) vm.updateSettings { it.copy(restSeconds = n) } }
                }, "Dinlenme süresi (saniye)", 15.0)
            }
        }

        item {
            SectionCard("Bugün ekranı", icon = Icons.Rounded.Tune) {
                HintText("Görmek istediğin kartları seç. Program ve seri kartı her zaman görünür.")
                TODAY_CARDS.forEach { (id, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = id !in settings.hiddenCards, onCheckedChange = { on ->
                            vm.updateSettings { it.copy(hiddenCards = if (on) it.hiddenCards - id else it.hiddenCards + id) }
                        })
                    }
                }
            }
        }

        item {
            SectionCard("Renk teması", icon = Icons.Rounded.Palette) {
                HintText("Seviye atladıkça yeni renkler açılır. Şu an seviye $level.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.fittakip.app.data.ACCENTS.forEach { a ->
                        val open = level >= a.unlockLevel
                        val c = androidx.compose.ui.graphics.Color(a.color)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (open) c else c.copy(alpha = 0.25f))
                                    .then(if (open) Modifier.clickable { vm.updateSettings { it.copy(accent = a.id) } } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (!open) "🔒" else if (settings.accent == a.id) "✓" else "", color = androidx.compose.ui.graphics.Color.Black)
                            }
                            Text(a.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            if (!open) HintText("Sv. ${a.unlockLevel}")
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Makro ayarları", icon = Icons.Rounded.Tune) {
                Stepper(
                    if (settings.proteinPerKg == 0.0) "0" else settings.proteinPerKg.clean(),
                    { v -> v.toNum()?.let { n -> if (n in 0.0..4.0) vm.updateSettings { it.copy(proteinPerKg = n) } } },
                    "Protein (g/kg, 0 = hedefe göre otomatik)", 0.1
                )
                Stepper(settings.fatPct.toString(), { v ->
                    v.toNum()?.toInt()?.let { n -> if (n in 10..60) vm.updateSettings { it.copy(fatPct = n) } }
                }, "Yağ (kalorinin yüzdesi)", 5.0)
                HintText("Kalan kalori karbonhidrata gider. Kas yapımı için 1,6-2,2 g/kg protein önerilir.")
            }
        }

        item {
            SectionCard("Takviye listesi", icon = Icons.Rounded.Medication) {
                if (settings.supplements.isEmpty()) HintText("Liste boş. Check-in ekranından ekleyebilirsin.")
                settings.supplements.sorted().forEach { sp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sp, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.updateSettings { it.copy(supplements = it.supplements - sp) } }) { Text("Çıkar") }
                    }
                }
            }
        }

        item {
            SectionCard("Veriler", icon = Icons.Rounded.Backup) {
                HintText("Tüm kayıtlarını tek bir dosyaya yedekle. Telefon değiştirirken ya da sıfırlamadan önce işine yarar.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Yedekle", onClick = { exporter.launch("fittakip-yedek-${LocalDate.now()}.json") }, icon = Icons.Rounded.Backup, modifier = Modifier.weight(1f))
                    SecondaryButton("Geri yükle", onClick = { importer.launch(arrayOf("application/json", "*/*")) }, icon = Icons.Rounded.Restore, modifier = Modifier.weight(1f))
                }
                Text("Otomatik haftalık yedek", style = MaterialTheme.typography.labelLarge)
                if (settings.autoBackupUri.isEmpty()) {
                    HintText("Bir klasör seç (ör. Google Drive içindeki bir klasör); her hafta oraya kendiliğinden yedek alırım. Telefon kaybolsa da verin gitmez.")
                    SecondaryButton("Klasör seç ve aç", onClick = { folder.launch(null) }, icon = Icons.Rounded.Backup, modifier = Modifier.fillMaxWidth())
                } else {
                    HintText(
                        "Açık ✅ " + if (settings.lastAutoBackup > 0)
                            "Son yedek: ${dayLabel(settings.lastAutoBackup / 86_400_000, true)}" else "İlk yedek bekleniyor"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Klasörü değiştir", onClick = { folder.launch(null) }, modifier = Modifier.weight(1f))
                        SecondaryButton("Kapat", onClick = { vm.setAutoBackupFolder(null) }, modifier = Modifier.weight(1f))
                    }
                }
                Text("Excel / Google Sheets için CSV", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Setler", onClick = { csvSets.launch("fittakip-setler-${LocalDate.now()}.csv") }, modifier = Modifier.weight(1f))
                    SecondaryButton("Yemekler", onClick = { csvFoods.launch("fittakip-yemekler-${LocalDate.now()}.csv") }, modifier = Modifier.weight(1f))
                }
                TextButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Tüm verileri sil", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { HintText("FitTakip ${com.fittakip.app.BuildConfig.VERSION_NAME} · Verilerin bu telefonda saklanır; Drive yedeğini açarsan sadece senin Google Drive'ına gider. Fotoğraftan kalori kullanırsan sadece o fotoğraf analiz için Google Gemini'ye gönderilir. Ana ekrana FitTakip widget'ını ekleyebilir, uygulama ikonuna basılı tutarak kısayolları kullanabilirsin.") }
    }

    var wipeDrive by remember { mutableStateOf(false) }
    val withDriveReset = rememberDriveAuth { vm.resetAll(); vm.toast("Drive'a ulaşılamadı, sadece telefondaki veriler silindi: $it") }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Her şey silinsin mi?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil, yemekler, setler, tartılar, su, kardiyo, ölçüler, fotoğraflar ve programın kalıcı olarak silinir.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = wipeDrive, onCheckedChange = { wipeDrive = it })
                        Column {
                            Text("Google Drive'daki yedekleri de sil", style = MaterialTheme.typography.titleSmall)
                            HintText("İşaretlemezsen Drive'daki yedeklerin durur, sonra geri yükleyebilirsin.")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                confirmReset = false
                if (wipeDrive) withDriveReset { vm.resetAll(it) } else vm.resetAll()
            }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

// ---------------- Karşılama ----------------

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val items = listOf(
        "🔥" to "Seri ve görevler" to "Her gün küçük bir kayıt gir, serini büyüt, günlük görevlerle XP topla.",
        "🍽️" to "Akıllı beslenme" to "80'den fazla hazır yemek, su takibi, öğünler ve kişisel kalori hedefi.",
        "🏋️" to "Antrenman koçu" to "Hazır programlar, ağırlık önerisi, dinlenme sayacı ve rekor kutlamaları.",
        "📈" to "Gelişim tahmini" to "Güç ve kilo trendin, hedefe varış tarihin ve haftalık raporun."
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("FitTakip", style = MaterialTheme.typography.displayMedium, color = FitColors.Lime)
        Text("Her gün biraz daha iyi.", style = MaterialTheme.typography.titleLarge)
        items.forEach { (head, body) ->
            val (emoji, title) = head
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    HintText(body)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Başlayalım", onClick = onStart)
    }
}


/** Hesap: Google bağlantısı, ad, kurulumu yeniden çalıştırma. */
@Composable
private fun AccountCard(vm: AppViewModel, settings: com.fittakip.app.data.AppSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf(false) }
    var name by remember(settings.userName) { mutableStateOf(settings.userName) }
    var confirmRedo by remember { mutableStateOf(false) }

    SectionCard("Hesap", icon = Icons.Rounded.AccountCircle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(FitColors.Lime.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text((settings.userName.ifBlank { settings.googleFullName }.firstOrNull() ?: '?').uppercase(), color = FitColors.Lime, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(settings.userName.ifBlank { "İsimsiz" }, style = MaterialTheme.typography.titleMedium)
                HintText(if (settings.googleEmail.isNotBlank()) "Google: ${settings.googleEmail}" else "Google hesabı bağlı değil")
            }
            TextButton(onClick = { editName = !editName }) { Text("Ad") }
        }
        if (editName) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(name, { name = it }, "Adın", Modifier.weight(1f))
                TextButton(onClick = { vm.setUserName(name); editName = false }) { Text("Kaydet") }
            }
        }
        if (settings.googleEmail.isBlank()) {
            SecondaryButton(if (busy) "Bağlanıyor…" else "Google ile giriş yap", enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                busy = true; error = null
                scope.launch {
                    when (val r = com.fittakip.app.auth.GoogleAuth.signIn(context)) {
                        is com.fittakip.app.auth.SignInResult.Success -> vm.linkGoogle(r.user)
                        is com.fittakip.app.auth.SignInResult.Error -> error = r.message
                        else -> Unit
                    }
                    busy = false
                }
            })
        } else {
            TextButton(onClick = { scope.launch { com.fittakip.app.auth.GoogleAuth.signOut(context); vm.unlinkGoogle() } }) {
                Text("Google hesabının bağlantısını kaldır", color = MaterialTheme.colorScheme.error)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        DriveCard(vm)
        TextButton(onClick = { confirmRedo = true }) { Text("Kurulum sihirbazını yeniden başlat") }
    }
    if (confirmRedo) AlertDialog(
        onDismissRequest = { confirmRedo = false },
        title = { Text("Kurulumu yeniden yap?") },
        text = { Text("Bilgilerini ve programını baştan soracağım. Kayıtların (yemek, set, tartı) silinmez; haftalık programın seçtiğinle değişir.") },
        confirmButton = { TextButton(onClick = { confirmRedo = false; vm.updateSettings { it.copy(onboarded = false) } }) { Text("Başlat") } },
        dismissButton = { TextButton(onClick = { confirmRedo = false }) { Text("Vazgeç") } }
    )
}
