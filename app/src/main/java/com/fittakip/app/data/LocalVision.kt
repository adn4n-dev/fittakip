package com.fittakip.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Telefonda çalışan yapay zekâ: Google'ın açık lisanslı (Apache 2.0) Gemma 4 E2B modeli, LiteRT-LM ile.
 * Bir kez indirilir (~2,6 GB, sadece Wi-Fi), sonra fotoğraflar internetsiz ve hiçbir yere gönderilmeden analiz edilir.
 */
object LocalVision {
    private const val FILE = "gemma-4-E2B-it.litertlm"
    private const val URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$FILE"
    private const val MIN_BYTES = 2_400_000_000L   // yarım kalmış indirmeyi "hazır" sayma

    sealed interface Status {
        data object NotDownloaded : Status
        data class Downloading(val done: Long, val total: Long) : Status
        data object Ready : Status
        data class Failed(val reason: String) : Status
    }

    fun modelFile(context: Context): File = File(context.getExternalFilesDir(null), FILE)

    fun isReady(context: Context): Boolean = modelFile(context).let { it.exists() && it.length() >= MIN_BYTES } &&
        SettingsStore(context).state.value.modelDownloadId == 0L

    /** İndirme durumu (DownloadManager'a sorar; bitince "hazır" işaretler). */
    fun status(context: Context): Status {
        val store = SettingsStore(context)
        val id = store.state.value.modelDownloadId
        if (id == 0L) return if (isReady(context)) Status.Ready else Status.NotDownloaded
        val dm = context.getSystemService(DownloadManager::class.java)
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) { store.update { it.copy(modelDownloadId = 0) }; return Status.NotDownloaded }
            val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (st) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    store.update { it.copy(modelDownloadId = 0) }
                    if (modelFile(context).length() >= MIN_BYTES) Status.Ready else Status.Failed("Dosya eksik indi, tekrar dene.")
                }
                DownloadManager.STATUS_FAILED -> {
                    store.update { it.copy(modelDownloadId = 0) }
                    modelFile(context).delete()
                    Status.Failed("İndirme başarısız (kod ${c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}). Wi-Fi ve boş alanı kontrol edip tekrar dene.")
                }
                else -> Status.Downloading(done, total)
            }
        }
    }

    /** Wi-Fi'da arka planda indir (uygulama kapansa da devam eder, bildirimde ilerleme görünür). */
    fun startDownload(context: Context): String? {
        val free = context.getExternalFilesDir(null)?.usableSpace ?: 0L
        if (free < 3_200_000_000L) return "Telefonda en az 3,2 GB boş alan gerekiyor (şu an ${free / 1_000_000_000} GB)."
        modelFile(context).delete()
        val req = DownloadManager.Request(Uri.parse(URL))
            .setTitle("FitTakip · telefondaki yapay zekâ")
            .setDescription("Gemma 4 modeli (2,6 GB), sadece Wi-Fi")
            .setDestinationInExternalFilesDir(context, null, FILE)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val id = context.getSystemService(DownloadManager::class.java).enqueue(req)
        SettingsStore(context).update { it.copy(modelDownloadId = id) }
        return null
    }

    fun cancelOrDelete(context: Context) {
        val store = SettingsStore(context)
        val id = store.state.value.modelDownloadId
        if (id != 0L) context.getSystemService(DownloadManager::class.java).remove(id)
        store.update { it.copy(modelDownloadId = 0) }
        release()
        modelFile(context).delete()
    }

    // ---- Çalıştırma: model belleğe bir kez yüklenir, 3 dk kullanılmazsa bırakılır ----
    private val lock = Mutex()
    private var engine: Engine? = null
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun analyze(context: Context, jpeg: ByteArray, prompt: String): String = withContext(Dispatchers.IO) {
        lock.withLock {
            idleJob?.cancel()
            val e = engine ?: createEngine(context).also { engine = it }
            val img = File(context.cacheDir, "food/local.jpg").apply { parentFile?.mkdirs(); writeBytes(jpeg) }
            val text = try {
                e.createConversation().use { conv ->
                    conv.sendMessage(Contents.of(Content.ImageFile(img.absolutePath), Content.Text(prompt))).toString()
                }
            } finally {
                idleJob = scope.launch { delay(180_000); lock.withLock { release() } }
            }
            text
        }
    }

    private fun createEngine(context: Context): Engine {
        val path = modelFile(context).absolutePath
        val cache = File(context.cacheDir, "litertlm").apply { mkdirs() }.absolutePath
        // Önce görüntü kısmını GPU'da dene; olmazsa tamamen işlemcide çalıştır.
        return runCatching {
            Engine(EngineConfig(modelPath = path, backend = Backend.CPU(), visionBackend = Backend.GPU(), cacheDir = cache)).apply { initialize() }
        }.getOrElse {
            Engine(EngineConfig(modelPath = path, backend = Backend.CPU(), visionBackend = Backend.CPU(), cacheDir = cache)).apply { initialize() }
        }
    }

    private fun release() {
        runCatching { engine?.close() }
        engine = null
    }
}
