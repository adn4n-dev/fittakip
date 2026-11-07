package com.fittakip.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.auth.DriveSync
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Drive izni: gerekiyorsa Google'ın izin ekranını açar, sonra [action]'ı jetonla çalıştırır.
 * Dönen fonksiyonu bir butona bağla: `withDrive { token -> ... }`.
 */
@Composable
fun rememberDriveAuth(onError: (String) -> Unit): ((String) -> Unit) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val token = runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(res.data).accessToken }.getOrNull()
        val action = pending
        pending = null
        if (token != null) action?.invoke(token) else onError("Drive izni verilmedi.")
    }
    return { action ->
        scope.launch {
            try {
                val r = DriveSync.authorize(context)
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    pending = action
                    launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else r.accessToken?.let(action) ?: onError("Drive izni alınamadı.")
            } catch (e: Exception) {
                onError(
                    if (e.message?.contains("access_denied", true) == true || e.message?.contains("10:") == true)
                        "Google erişimi engelledi. Google Cloud'da kendi Gmail'ini test kullanıcısı olarak eklediğinden emin ol."
                    else "Drive izni alınamadı: ${e.message}"
                )
            }
        }
    }
}

private val stamp = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag("tr"))
fun driveTime(ms: Long): String = stamp.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

/** Ayarlar > Hesap içinde: Drive yedeği aç/kapat, şimdi yedekle, geri yükle. */
@Composable
fun DriveCard(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.driveBusy.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    var restoreToken by remember { mutableStateOf<String?>(null) }
    val withDrive = rememberDriveAuth { error = it }

    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.04f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("☁️ Google Drive yedeği", style = MaterialTheme.typography.titleSmall)
                    HintText(
                        when {
                            !settings.driveBackupOn -> "Kapalı. Açarsan her gün ve antrenmandan sonra otomatik yedeklenir."
                            settings.lastDriveBackup > 0 -> "Son yedek: ${driveTime(settings.lastDriveBackup)}"
                            else -> "Açık, ilk yedek bekleniyor."
                        }
                    )
                }
                Switch(
                    checked = settings.driveBackupOn, enabled = !busy,
                    onCheckedChange = { on -> error = null; if (on) withDrive { vm.enableDrive(it) } else vm.disableDrive() }
                )
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = FitColors.Lime)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settings.driveBackupOn) SecondaryButton("Şimdi yedekle", enabled = !busy, modifier = Modifier.weight(1f), onClick = { error = null; withDrive { vm.driveBackupNow(it) } })
                SecondaryButton("Drive'dan geri yükle", enabled = !busy, modifier = Modifier.weight(1f), onClick = { error = null; withDrive { restoreToken = it } })
            }
            HintText("Yedek, Drive'ında sadece bu uygulamanın görebildiği gizli bir klasörde durur. Son 7 günün yedeği saklanır.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
    restoreToken?.let { t -> DriveRestoreDialog(vm, t, onDismiss = { restoreToken = null }) }
}

/** Drive'daki yedekleri listeler; seçileni geri yükler. */
@Composable
fun DriveRestoreDialog(vm: AppViewModel, token: String, onDismiss: () -> Unit, onRestored: () -> Unit = {}) {
    var items by remember { mutableStateOf<List<DriveSync.RemoteBackup>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<DriveSync.RemoteBackup?>(null) }
    LaunchedEffect(token) {
        try { items = vm.driveList(token) } catch (e: Exception) { error = e.message ?: "Liste alınamadı."; items = emptyList() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drive'daki yedeklerin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    items == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Drive'a bakılıyor…")
                    }
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    items!!.isEmpty() -> HintText("Drive'da henüz FitTakip yedeği yok.")
                    else -> items!!.forEach { b ->
                        Surface(onClick = { confirm = b }, shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(b.day?.let { com.fittakip.app.ui.dayLabel(it.toEpochDay(), true) } ?: b.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                HintText("${(b.size / 1024).coerceAtLeast(1)} KB")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
    confirm?.let { b ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Bu yedek geri yüklensin mi?") },
            text = { Text("Telefondaki yemek, set, tartı kayıtların ve programın bu yedekle değiştirilir.") },
            confirmButton = { TextButton(onClick = { vm.driveRestore(token, b.id) { onRestored() }; confirm = null; onDismiss() }) { Text("Geri yükle") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Vazgeç") } }
        )
    }
}
