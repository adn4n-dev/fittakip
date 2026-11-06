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
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    if (!settings.onboarded) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.systemBarsPadding()) {
                OnboardingScreen {
                    vm.updateSettings { it.copy(onboarded = true) }
                    askNotifications()
                }
            }
        }
        return
    }

    val openExercise: (String) -> Unit = { name -> vm.detailExercise.value = name; sub = Sub.EXERCISE }
    BackHandler(enabled = sub != null) { sub = null }
    BackHandler(enabled = sub == null && tab != 0) { tab = 0 }

    val tabs = listOf(
        "Bugün" to Icons.Rounded.Home,
        "Beslenme" to Icons.Rounded.Restaurant,
        "Antrenman" to Icons.Rounded.FitnessCenter,
        "Gelişim" to Icons.Rounded.Insights,
        "Vücut" to Icons.Rounded.Accessibility
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (sub == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = {
                                if (tab != i) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tab = i
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
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
                    Sub.SETTINGS -> SettingsScreen(vm, onBack = { sub = null }, onRequestNotifications = askNotifications)
                    Sub.HABITS -> HabitsScreen(vm) { sub = null }
                    Sub.STATS -> StatsScreen(vm) { sub = null }
                    Sub.EXERCISE -> ExerciseDetailScreen(vm) { sub = null }
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

    celebrations.firstOrNull()?.let { c ->
        CelebrationDialog(c) { vm.dismissCelebration() }
    }
}
