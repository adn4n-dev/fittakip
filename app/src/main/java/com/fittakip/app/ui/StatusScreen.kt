package com.fittakip.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittakip.app.auth.DriveSync
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.fittakip.app.data.OnlineFoods
import com.fittakip.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Bir özelliğin durumu: ✅ çalışıyor, ⚠️ dikkat, ❌ çalışmıyor; ne yapılacağı ve varsa düzeltme butonu. */
data class Check(val title: String, val level: Int, val detail: String, val fixLabel: String? = null, val fix: ((Context) -> Unit)? = null)

private fun openAppSettings(c: Context) = c.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", c.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)

/** Tüm kontrolleri paralel çalıştırır (her biri en fazla ~25 sn). */
suspend fun runChecks(context: Context): List<Check> = coroutineScope {
    val st = SettingsStore.get(context).state.value
    val online = run {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
    val internet = Check("İnternet", if (online) 0 else 2, if (online) "Bağlı." else "İnternet yok. Market ürünleri, fotoğraf ve Drive çalışmaz; diğer her şey çalışır.")

    val off = async {
        if (!online) return@async Check("Market ürünleri (Open Food Facts)", 2, "İnternet olmadan aranamaz.")
        val r = withTimeoutOrNull(25_000) { runCatching { OnlineFoods.search("ayran") } }
        when {
            r == null -> Check("Market ürünleri (Open Food Facts)", 2, "25 saniyede cevap gelmedi; servis çok yavaş.")
            r.isSuccess && r.getOrThrow().isNotEmpty() -> Check("Market ürünleri (Open Food Facts)", 0, "Çalışıyor (deneme aramasında ${r.getOrThrow().size} ürün).")
            r.isSuccess -> Check("Market ürünleri (Open Food Facts)", 1, "Servis cevap verdi ama sonuç boş.")
            else -> Check("Market ürünleri (Open Food Facts)", 2, r.exceptionOrNull()?.message ?: "Ulaşılamadı.")
        }
    }

    val gemini = async {
        if (!online) return@async Check("Fotoğraftan kalori (Gemini)", 2, "İnternet olmadan çalışmaz.")
        suspend fun ping(level: com.google.firebase.ai.type.ThinkingLevel?): Double {
            val model = Firebase.ai(backend = com.google.firebase.ai.type.GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = com.google.firebase.ai.type.generationConfig {
                        if (level != null) thinkingConfig = com.google.firebase.ai.type.thinkingConfig { thinkingLevel = level }
                    },
                    requestOptions = com.google.firebase.ai.type.RequestOptions(timeoutInMillis = 20_000)
                )
            val t0 = System.currentTimeMillis()
            model.generateContent("Sadece 'tamam' yaz.").text.orEmpty()
            return (System.currentTimeMillis() - t0) / 1000.0
        }
        val r = withTimeoutOrNull(25_000) {
            runCatching {
                try { ping(com.google.firebase.ai.type.ThinkingLevel.LOW) } catch (e: Exception) {
                    if ((e.message ?: "").contains("thinking", true)) ping(null) else throw e
                }
            }
        }
        when {
            r == null -> Check("Fotoğraftan kalori (Gemini)", 2, "25 saniyede cevap gelmedi. Fotoğrafın takılmasının sebebi bu.")
            r.isSuccess -> Check("Fotoğraftan kalori (Gemini)", 0, "Çalışıyor (deneme cevabı ${"%.1f".format(r.getOrThrow())} sn).")
            else -> {
                val m = (r.exceptionOrNull()?.message ?: "") + " " + (r.exceptionOrNull()?.cause?.message ?: "")
                val hint = when {
                    m.contains("App Check", true) || m.contains("PERMISSION_DENIED", true) || m.contains("403") ->
                        "Firebase erişimi reddetti → Firebase > App Check > APIs > Firebase AI Logic: 'Erzwingung aufheben'."
                    m.contains("not been used", true) || m.contains("disabled", true) || m.contains("SERVICE_DISABLED", true) ->
                        "Firebase'de AI Logic (Gemini Developer API) açılmamış ya da yeni açıldı (birkaç dakika bekle)."
                    m.contains("billing", true) || m.contains("Blaze", true) -> "Firebase projesi bu model için ücretli plan istiyor."
                    m.contains("quota", true) || m.contains("429") -> "Ücretsiz kota dolmuş; yarın tekrar dene."
                    m.contains("not found", true) -> "Model bulunamadı (gemini-3.8-flash)."
                    else -> "Hata: ${m.trim().take(200)}"
                }
                Check("Fotoğraftan kalori (Gemini)", 2, hint)
            }
        }
    }

    val barcode = async {
        runCatching {
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
            val ok = com.google.android.gms.common.moduleinstall.ModuleInstall.getClient(context).areModulesAvailable(scanner).await().areModulesAvailable()
            if (ok) Check("Barkod okuyucu", 0, "Telefonda hazır.")
            else Check("Barkod okuyucu", 1, "Henüz indirilmemiş; ilk barkod okutmada Google Play Hizmetleri indirecek (internet gerekir).")
        }.getOrElse { Check("Barkod okuyucu", 2, "Google Play Hizmetleri cevap vermedi: ${it.message}. Barkodu elle yazabilirsin.") }
    }

    val drive = async {
        when {
            !st.driveBackupOn -> Check("Google Drive yedeği", 1, "Kapalı. Ayarlar > Hesap'tan açarsan telefon değişse de verilerin kaybolmaz.")
            withContext(Dispatchers.IO) { DriveSync.tokenSilently(context) } == null ->
                Check("Google Drive yedeği", 2, "Drive izni düşmüş; yedek alınamıyor. Ayarlar > Hesap'ta Drive yedeğini kapatıp yeniden aç.")
            st.lastDriveBackup == 0L -> Check("Google Drive yedeği", 1, "Açık ama henüz yedek alınmadı. 'Şimdi yedekle' ile dene.")
            System.currentTimeMillis() - st.lastDriveBackup > 3 * 86_400_000L ->
                Check("Google Drive yedeği", 1, "Son yedek ${driveTime(st.lastDriveBackup)}; 3 günden eski. 'Şimdi yedekle'yi dene.")
            else -> Check("Google Drive yedeği", 0, "Son yedek: ${driveTime(st.lastDriveBackup)}.")
        }
    }

    val notif = if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        Check("Bildirim izni", 0, "Verilmiş.")
    else Check("Bildirim izni", 2, "Kapalı: sabah görevi, su ve dinlenme bildirimleri gelmez.", "İzin ver", ::openAppSettings)

    val am = context.getSystemService(android.app.AlarmManager::class.java)
    val exact = if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) Check("Tam saatli alarm", 0, "Dinlenme sayacı ve sabah bildirimi tam zamanında çalar.")
    else Check("Tam saatli alarm", 1, "İzin yok: dinlenme bitişi ve sabah bildirimi birkaç dakika gecikebilir.", "İzin ver") { c ->
        c.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${c.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    val morning = if (st.morningOn) Check("Sabah görevi bildirimi", 0, "Her sabah 08:30'da günün programı gelir.")
    else Check("Sabah görevi bildirimi", 1, "Kapalı. Ayarlar > Hatırlatma'dan açarsan her sabah 'bugün Upper A' gibi söyler.")

    val battery = context.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    val batt = if (battery) Check("Pil kısıtlaması", 0, "Uygulama arka planda kısıtlanmıyor.")
    else Check("Pil kısıtlaması", 1, "Samsung pil tasarrufu arka plan yedeğini ve bildirimleri geciktirebilir. Uygulama > Pil > 'Kısıtlanmamış' yap.", "Ayarları aç", ::openAppSettings)

    listOf(internet, off.await(), gemini.await(), barcode.await(), drive.await(), notif, exact, morning, batt)
}

@Composable
fun StatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var checks by remember { mutableStateOf<List<Check>?>(null) }
    var run by remember { mutableIntStateOf(0) }
    LaunchedEffect(run) { checks = null; checks = runChecks(context) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Uygulama durumu", onBack) }
        item { HintText("Uygulamanın her parçası tek tek deneniyor. Bir şey garip geliyorsa önce buraya bak.") }
        val list = checks
        if (list == null) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Kontrol ediliyor… (en fazla 25 sn)")
            }
        } else {
            val bad = list.count { it.level == 2 }
            val warn = list.count { it.level == 1 }
            item {
                Text(
                    when { bad > 0 -> "❌ $bad özellik çalışmıyor" + (if (warn > 0) ", $warn uyarı" else ""); warn > 0 -> "⚠️ Her şey çalışıyor, $warn uyarı var"; else -> "✅ Her şey çalışıyor" },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(list) { c ->
                FitCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(when (c.level) { 0 -> "✅"; 1 -> "⚠️"; else -> "❌" }, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.titleSmall)
                            HintText(c.detail)
                            if (c.fix != null && c.fixLabel != null) TextButton(onClick = { c.fix.invoke(context) }) { Text(c.fixLabel) }
                        }
                    }
                }
            }
            item { SecondaryButton("Tekrar kontrol et", onClick = { run++ }, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
