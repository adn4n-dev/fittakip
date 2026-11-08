package com.fittakip.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittakip.app.ui.*

class MainActivity : ComponentActivity() {
    private val shortcut = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        shortcut.value = intent.getStringExtra("shortcut")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        shortcut.value = intent?.getStringExtra("shortcut")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            FitTheme(settings.accent) {
                FitApp(vm, shortcut.value) { shortcut.value = null }
            }
        }
    }
}

@Composable
fun FitApp(vm: AppViewModel, shortcut: String?, onShortcutHandled: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val celebrations by vm.celebrations.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by rememberSaveable { mutableStateOf<Sub?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val askNotifications = {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(shortcut) {
        when (shortcut) {
            "water" -> { vm.addWater(250); vm.toast("250 ml su eklendi 💧"); tab = 1; sub = null }
            "workout" -> { tab = 2; sub = null }
            "weight" -> { tab = 4; sub = null }
        }
        if (shortcut != null) onShortcutHandled()
    }

    LaunchedEffect(message) {
        message?.let { m ->
            vm.clearMessage()
            val r = snackbar.showSnackbar(m.text, actionLabel = m.action, duration = if (m.action != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }

    if (!settings.onboarded) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.systemBarsPadding()) {
                OnboardingFlow(vm) { openProgram ->
                    tab = 0
                    sub = if (openProgram) Sub.PROGRAM else null
                    askNotifications()
                }
            }
        }
        return
    }

    val openExercise: (String) -> Unit = { name -> vm.detailExercise.value = name; sub = Sub.EXERCISE }
    BackHandler(enabled = sub != null) { sub = null }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> vm.onAppPause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> vm.onAppResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    BackHandler(enabled = sub == null && tab != 0) { tab = 0 }

    val tabs = listOf(
        "Bugün" to Icons.Rounded.Home,
        "Beslenme" to Icons.Rounded.Restaurant,
        "Antrenman" to Icons.Rounded.FitnessCenter,
        "Gelişim" to Icons.Rounded.Insights,
        "Vücut" to Icons.Rounded.Accessibility
    )

    GlowBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = FitColors.Text,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (sub == null) FloatingNav(tabs, tab) { i ->
                    if (tab != i) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tab = i
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                AnimatedContent(targetState = sub, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "sub") { s ->
                    when (s) {
                        Sub.ACHIEVEMENTS -> AchievementsScreen(vm) { sub = null }
                        Sub.REPORT -> WeeklyReportScreen(vm) { sub = null }
                        Sub.SETTINGS -> SettingsScreen(vm, onBack = { sub = null }, onRequestNotifications = askNotifications, onProgram = { sub = Sub.PROGRAM }, onStatus = { sub = Sub.STATUS })
                        Sub.HABITS -> HabitsScreen(vm) { sub = null }
                        Sub.EXERCISE -> ExerciseDetailScreen(vm) { sub = null }
                        Sub.PROGRAM -> ProgramScreen(vm) { sub = null }
                        Sub.STATUS -> StatusScreen { sub = Sub.SETTINGS }
                        null -> Crossfade(targetState = tab, label = "tab") { t ->
                            when (t) {
                                0 -> TodayScreen(vm, onTab = { tab = it }, onOpen = { sub = it })
                                1 -> NutritionScreen(vm)
                                2 -> WorkoutScreen(vm, onOpenExercise = openExercise)
                                3 -> ProgressScreen(vm, onOpenExercise = openExercise)
                                else -> BodyScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }

    celebrations.firstOrNull()?.let { c ->
        CelebrationDialog(c) { vm.dismissCelebration() }
    }

    // Son kullanımda çöktüyse: rapor telefonda, paylaşmak kullanıcının kararı.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(ctx)) }
    crash?.let { report ->
        AlertDialog(
            onDismissRequest = { CrashLog.clear(ctx); crash = null },
            title = { Text("Uygulama son kullanımda çöktü 😕") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Özür dilerim. Hatanın kaydı sadece bu telefonda duruyor, hiçbir yere gönderilmedi.")
                    Text("Paylaşırsan (WhatsApp, e-posta…) sorunu bulup düzeltmek çok kolaylaşır. Raporda kayıtların yok; sadece hatanın teknik detayı ve telefon modeli var.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "FitTakip çökme raporu")
                        putExtra(android.content.Intent.EXTRA_TEXT, report)
                    }
                    ctx.startActivity(android.content.Intent.createChooser(send, "Raporu paylaş"))
                    CrashLog.clear(ctx); crash = null
                }) { Text("Raporu paylaş") }
            },
            dismissButton = { TextButton(onClick = { CrashLog.clear(ctx); crash = null }) { Text("Sil") } }
        )
    }
}


/** Yüzen, hap şeklinde alt menü: seçili sekme vurgu renginde genişler. */
@Composable
private fun FloatingNav(tabs: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>, selected: Int, onSelect: (Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                .background(androidx.compose.ui.graphics.Color(0xF2171A20))
                .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, (label, icon) ->
                val on = i == selected
                val bg by animateColorAsState(if (on) FitColors.Lime else androidx.compose.ui.graphics.Color.Transparent, label = "bg")
                val fg by animateColorAsState(if (on) androidx.compose.ui.graphics.Color(0xFF0E1200) else FitColors.Muted, label = "fg")
                // Her sekmenin adı her zaman görünür (sadece simgeyle hangi ekran olduğu tahmin edilmesin)
                androidx.compose.foundation.layout.Column(
                    Modifier
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                    Text(
                        label, color = fg, maxLines = 1,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}
