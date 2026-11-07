package com.fittakip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittakip.app.auth.GoogleAuth
import com.fittakip.app.auth.GoogleUser
import com.fittakip.app.auth.SignInResult
import com.fittakip.app.data.Equipment
import com.fittakip.app.data.Profile
import com.fittakip.app.data.TEMPLATES
import com.fittakip.app.data.WEEKDAY_NAMES
import com.fittakip.app.data.WEEK_PRESETS
import com.fittakip.app.data.exerciseDef
import com.fittakip.app.domain.ACTIVITY_LEVELS
import com.fittakip.app.domain.Calculator
import com.fittakip.app.domain.Goal
import com.fittakip.app.domain.OnboardingLogic
import kotlinx.coroutines.launch

private const val STEPS = 5   // 0 giriş · 1 hakkında · 2 program · 3 salon · 4 özet

/**
 * İlk açılış sihirbazı: Google ile giriş → kullanıcı bilgileri → antrenman programı → salon → özet.
 * [onDone] true dönerse kullanıcı programını kendisi girmek istiyor demektir (Program ekranı açılır).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(vm: AppViewModel, onDone: (openProgram: Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }

    // Google
    var gEmail by rememberSaveable { mutableStateOf("") }
    var gFull by rememberSaveable { mutableStateOf("") }
    var gFirst by rememberSaveable { mutableStateOf("") }
    var signingIn by remember { mutableStateOf(false) }
    var signInError by rememberSaveable { mutableStateOf<String?>(null) }

    // Hakkında
    var name by rememberSaveable { mutableStateOf("") }
    var male by rememberSaveable { mutableStateOf(true) }
    var age by rememberSaveable { mutableStateOf("25") }
    var height by rememberSaveable { mutableStateOf("175") }
    var weight by rememberSaveable { mutableStateOf("75") }
    var target by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf(Goal.CUT.name) }
    var activity by rememberSaveable { mutableDoubleStateOf(1.55) }
    var lifeStage by rememberSaveable { mutableStateOf("") }
    var health by rememberSaveable { mutableStateOf(false) }

    // Program
    var level by rememberSaveable { mutableIntStateOf(1) }
    var days by rememberSaveable { mutableStateOf(setOf(0, 2, 4)) }
    var presetIdx by rememberSaveable { mutableIntStateOf(-1) }   // -1 = önerilen, -2 = kendim gireceğim
    var rotation by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var unavailable by rememberSaveable { mutableStateOf(setOf<String>()) }

    val recommended = OnboardingLogic.recommend(days.size, level)
    val preset = when (presetIdx) { -1 -> recommended; -2 -> null; else -> WEEK_PRESETS[presetIdx] }
    val rot = rotation ?: (preset?.let { OnboardingLogic.suggestRotation(it, days) } ?: false)
    val plan = preset?.let { OnboardingLogic.buildPlan(it, days, rot) } ?: List(7) { "" }

    fun signIn() {
        signingIn = true; signInError = null
        scope.launch {
            when (val r = GoogleAuth.signIn(context)) {
                is SignInResult.Success -> {
                    gEmail = r.user.email; gFull = r.user.fullName; gFirst = r.user.firstName
                    if (name.isBlank()) name = r.user.firstName
                    step = 1
                }
                SignInResult.Cancelled -> Unit
                is SignInResult.Error -> signInError = r.message
            }
            signingIn = false
        }
    }

    fun finish() {
        val p = Profile(
            male = male,
            age = age.toNum()?.toInt()?.coerceIn(12, 99) ?: 25,
            heightCm = height.toNum()?.coerceIn(120.0, 230.0) ?: 175.0,
            weightKg = weight.toNum()?.coerceIn(30.0, 300.0) ?: 75.0,
            activity = activity,
            goal = goal,
            targetWeightKg = target.toNum()
        )
        val google = if (gEmail.isNotBlank()) GoogleUser(gFirst, gFull, gEmail) else null
        vm.completeOnboarding(name, google, p, plan, rot, unavailable, days.size, if (male) "" else lifeStage, health)
        onDone(preset == null)
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize()) {
        if (step > 0) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step-- }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
                Row(Modifier.weight(1f).padding(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1 until STEPS) Box(
                        Modifier.weight(1f).height(5.dp).clip(CircleShape)
                            .background(if (i <= step) FitColors.Lime else Color.White.copy(alpha = 0.10f))
                    )
                }
            }
        }
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f), label = "onb") { s ->
            when (s) {
                0 -> WelcomeStep(signingIn, signInError, onGoogle = ::signIn, onSkip = { step = 1 })
                1 -> StepScaffold(
                    title = if (name.isNotBlank()) "Merhaba $name 👋" else "Seni tanıyalım",
                    subtitle = "Kalori hedefin ve ağırlık önerilerin bu bilgilere göre hesaplanır.",
                    button = "Devam", enabled = name.isNotBlank() && weight.toNum() != null && height.toNum() != null && age.toNum() != null,
                    onNext = { step = 2 }
                ) {
                    if (gEmail.isNotBlank()) Pill("✓ $gEmail", FitColors.Lime)
                    TextInput(name, { name = it }, "Adın")
                    Label("Cinsiyet")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectCard(male, { male = true; lifeStage = "" }, "Erkek", Modifier.weight(1f))
                        SelectCard(!male, { male = false }, "Kadın", Modifier.weight(1f))
                    }
                    if (!male) LifeStagePicker(lifeStage) { lifeStage = it; if (it.isNotEmpty() && goal == Goal.CUT.name) goal = Goal.MAINTAIN.name }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stepper(age, { age = it }, "Yaş", 1.0, Modifier.weight(1f))
                        Stepper(height, { height = it }, "Boy (cm)", 1.0, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stepper(weight, { weight = it }, "Kilo (kg)", 0.5, Modifier.weight(1f))
                        Stepper(target, { target = it }, "Hedef kilo", 0.5, Modifier.weight(1f))
                    }
                    Label("Hedefin")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Goal.entries.forEach { g ->
                            val blocked = g == Goal.CUT && !male && lifeStage.isNotEmpty()
                            SelectCard(goal == g.name, { if (!blocked) goal = g.name }, g.label, Modifier.weight(1f), if (blocked) "kapalı" else null)
                        }
                    }
                    Label("Gün içi hareketin")
                    ACTIVITY_LEVELS.forEach { a ->
                        SelectCard(activity == a.factor, { activity = a.factor }, a.label, Modifier.fillMaxWidth())
                    }
                    Label("Sağlık")
                    HealthToggle(health) { health = it }
                    SafetyNotice(Calculator.safetyWarnings(male, height.toNum(), weight.toNum(), target.toNum(), Goal.valueOf(goal), if (male) "" else lifeStage, health))
                }
                2 -> StepScaffold(
                    title = "Antrenman programın",
                    subtitle = "Hangi günler spora gidiyorsun? Her gün sana o günün görevini söyleyeceğim.",
                    button = "Devam", enabled = days.isNotEmpty(), onNext = { step = 3 }
                ) {
                    Label("Seviyen")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Yeni başladım" to "0-6 ay", "Orta" to "6 ay - 2 yıl", "İleri" to "2 yıl +").forEachIndexed { i, (t, sub) ->
                            SelectCard(level == i, { level = i }, t, Modifier.weight(1f), sub)
                        }
                    }
                    Label("Spor günlerin · haftada ${days.size} gün")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WEEKDAY_NAMES.forEachIndexed { i, d ->
                            val on = i in days
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(if (on) FitColors.Lime.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (on) FitColors.Lime else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { days = if (on) days - i else days + i; rotation = null }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d.take(2), color = if (on) FitColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Label("Program")
                    ProgramOption(
                        selected = presetIdx == -1, title = "⭐ Önerilen: ${recommended.name}",
                        desc = recommended.desc + " · seviyene ve gün sayına göre", onClick = { presetIdx = -1; rotation = null }
                    )
                    WEEK_PRESETS.forEachIndexed { i, p ->
                        if (p != recommended) ProgramOption(presetIdx == i, p.name, p.desc) { presetIdx = i; rotation = null }
                    }
                    ProgramOption(presetIdx == -2, "✍️ Kendi programımı gireceğim", "Kurulumdan sonra antrenmanlarını hareket hareket yazarsın.") { presetIdx = -2 }

                    if (preset != null) {
                        Label("Bir gün kaçırırsan?")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectCard(!rot, { rotation = false }, "📅 Günlere bağlı", Modifier.weight(1f), "Pazartesi hep aynı antrenman")
                            SelectCard(rot, { rotation = true }, "🔁 Sırayla", Modifier.weight(1f), "Kaçırınca sıra kaymaz")
                        }
                        if (!rot && days.size != OnboardingLogic.workouts(preset).size) {
                            HintText("Bu program ${OnboardingLogic.workouts(preset).size} antrenmanlık; ${days.size} gün seçtin. Antrenmanlar seçtiğin günlere sırayla dağıtılır. Sırayla modu bu durumda daha iyi olabilir.")
                        }
                        PlanPreview(plan)
                    }
                }
                3 -> {
                    val machines = preset?.let { p ->
                        OnboardingLogic.workouts(p).distinct()
                            .flatMap { n -> (vm.settings.value.allTemplates.firstOrNull { it.name == n } ?: TEMPLATES.firstOrNull { it.name == n })?.items.orEmpty() }
                            .map { it.exercise }.distinct()
                            .filter { exerciseDef(it)?.equipment in setOf(Equipment.MACHINE, Equipment.CABLE) }
                    }.orEmpty()
                    StepScaffold(
                        title = "Salonunda olmayan var mı?",
                        subtitle = if (machines.isEmpty()) "Programını sonra gireceğin için bu adımı geçebilirsin. Salonunu daha sonra Antrenman > Kütüphane > Salonumu düzenle'den ayarlayabilirsin."
                        else "Salonunda olmayan makinelere dokun. O gün sana yerine geçecek hareketi önereyim.",
                        button = "Devam", onNext = { step = 4 }
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            machines.forEach { m ->
                                val off = m in unavailable
                                Pill(
                                    (if (off) "✖ " else "✓ ") + m,
                                    if (off) MaterialTheme.colorScheme.error else FitColors.Lime,
                                    onClick = { unavailable = if (off) unavailable - m else unavailable + m }
                                )
                            }
                        }
                        if (machines.isNotEmpty()) HintText("${machines.size - unavailable.count { it in machines }}/${machines.size} makine var olarak işaretli.")
                    }
                }
                else -> {
                    val p = Profile(
                        male = male, age = age.toNum()?.toInt() ?: 25, heightCm = height.toNum() ?: 175.0,
                        weightKg = weight.toNum() ?: 75.0, activity = activity, goal = goal, targetWeightKg = target.toNum()
                    )
                    val r = Calculator.calculate(p, vm.settings.value.copy(lifeStage = if (male) "" else lifeStage))
                    val warnings = Calculator.safetyWarnings(male, p.heightCm, p.weightKg, p.targetWeightKg, Goal.valueOf(goal), if (male) "" else lifeStage, health)
                    StepScaffold(
                        title = "Hazırsın${if (name.isNotBlank()) ", $name" else ""}! 🎉",
                        subtitle = "Her şeyi sonra Ayarlar'dan ve Program ekranından değiştirebilirsin.",
                        button = if (preset == null) "Programımı girmeye geç" else "Başlayalım", onNext = ::finish
                    ) {
                        GlowCard {
                            val dark = Color(0xFF0E1200)
                            Text("GÜNLÜK HEDEFİN", color = dark.copy(alpha = 0.65f), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                            Text("${r.target} kcal", color = dark, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp))
                            Text("Protein ${r.proteinG} g · Yağ ${r.fatG} g · Karbonhidrat ${r.carbG} g", color = dark, style = MaterialTheme.typography.titleSmall)
                        }
                        SafetyNotice(warnings)
                        HintText("Bu hesaplar genel tahmindir, tıbbi tavsiye değildir.")
                        if (preset != null) {
                            Label(if (rot) "Antrenman sıran" else "Haftan")
                            PlanPreview(plan)
                            if (rot) HintText("İstediğin gün git; uygulama her seferinde sıradaki antrenmanı verir. Hedefin haftada ${days.size} antrenman.")
                        }
                        if (gEmail.isNotBlank()) HintText("Google hesabın: $gEmail. Verilerin telefonunda kalır, hiçbir sunucuya gönderilmez.")
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(signingIn: Boolean, error: String?, onGoogle: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("FitTakip", style = MaterialTheme.typography.displayMedium, color = FitColors.Lime)
        Text("Her gün biraz daha iyi.", style = MaterialTheme.typography.titleLarge)
        listOf(
            "🗓️" to "Her gün ne yapacağını bil" to "Pazartesi görevin hazır, salı off day. Kaçırınca kaydır.",
            "🏋️" to "Ağırlık koçu" to "Geçen seferden ne kaldırdın, bugün ne denemelisin.",
            "🍽️" to "Kalori ve su" to "Hedefine göre günlük kalori ve makro.",
            "📈" to "Gelişim tahmini" to "Güç ve kilo trendin, hedefe ne zaman varacağın."
        ).forEach { (head, body) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(head.first, fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(head.second, style = MaterialTheme.typography.titleMedium)
                    HintText(body)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onGoogle, enabled = !signingIn,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (signingIn) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color(0xFF1F1F1F))
            else {
                GoogleG()
                Spacer(Modifier.width(12.dp))
                Text("Google ile devam et", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Google olmadan devam et") }
        HintText("Google ile adını ve e-postanı alırım. Verilerin telefonunda kalır.", Modifier.fillMaxWidth())
    }
}

/** Renkli "G" harfi (logo dosyası olmadan). */
@Composable
private fun GoogleG() {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFFDADCE0), CircleShape), contentAlignment = Alignment.Center) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun StepScaffold(
    title: String, subtitle: String, button: String, enabled: Boolean = true,
    onNext: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    HintText(subtitle)
                    Spacer(Modifier.height(4.dp))
                    content()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        PrimaryButton(button, onClick = onNext, enabled = enabled, icon = Icons.AutoMirrored.Rounded.ArrowForward,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ProgramOption(selected: Boolean, title: String, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        color = if (selected) FitColors.Lime.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) FitColors.Lime else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) FitColors.Lime else MaterialTheme.colorScheme.onSurface)
                HintText(desc)
            }
            if (selected) Icon(Icons.Rounded.Check, null, tint = FitColors.Lime)
        }
    }
}

@Composable
private fun PlanPreview(plan: List<String>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.04f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        plan.forEachIndexed { i, p ->
            Row {
                Text(WEEKDAY_NAMES[i], modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (p) { com.fittakip.app.data.OFF_DAY -> "Off day 😌"; "" -> "—"; else -> "$p 💪" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = if (p.isNotEmpty() && p != com.fittakip.app.data.OFF_DAY) FontWeight.Bold else FontWeight.Normal,
                    color = if (p.isNotEmpty() && p != com.fittakip.app.data.OFF_DAY) FitColors.Lime else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
