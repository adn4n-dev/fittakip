package com.fittakip.app

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

/**
 * Çökme kaydı: uygulama çökerse hata telefona yazılır, hiçbir yere gönderilmez.
 * Bir sonraki açılışta kullanıcıya "paylaşmak ister misin?" diye sorulur.
 */
class FitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Önceki sürümde indirilmiş olabilecek 2,6 GB'lık telefon modelini temizle (artık Gemini kullanılıyor)
        runCatching {
            val st = com.fittakip.app.data.SettingsStore(this)
            val id = st.state.value.modelDownloadId
            if (id != 0L) getSystemService(android.app.DownloadManager::class.java).remove(id)
            if (id != 0L) st.update { it.copy(modelDownloadId = 0) }
            File(getExternalFilesDir(null), "gemma-4-E2B-it.litertlm").delete()
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { CrashLog.write(this, error) }
            previous?.uncaughtException(thread, error)
        }
    }
}

object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun write(context: Context, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        file(context).writeText(
            "FitTakip ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                "Zaman: ${LocalDateTime.now()}\n" +
                "Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n\n" +
                sw.toString().take(12_000)
        )
    }

    fun read(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) { file(context).delete() }
}
