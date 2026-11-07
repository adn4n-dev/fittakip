package com.fittakip.app.auth

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

/**
 * Google Drive'ın gizli uygulama klasörüne (appDataFolder) yedek.
 * Bu klasörü sadece FitTakip görür; kullanıcının Drive'daki diğer dosyalarına erişim yoktur.
 * Her gün için bir dosya tutulur, son 7 gün saklanır.
 */
object DriveSync {
    const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val PREFIX = "fittakip-"
    private const val KEEP = 7

    data class RemoteBackup(val id: String, val name: String, val modified: String, val size: Long) {
        /** "fittakip-2026-11-07.json" → 2026-11-07 */
        val day: LocalDate? get() = runCatching { LocalDate.parse(name.removePrefix(PREFIX).removeSuffix(".json")) }.getOrNull()
    }

    class DriveException(message: String) : Exception(message)

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    /** İzin zaten verildiyse jeton döner; kullanıcıya sorulması gerekiyorsa sonuç hasResolution() olur. */
    suspend fun authorize(context: Context): AuthorizationResult = withContext(Dispatchers.IO) {
        Tasks.await(Identity.getAuthorizationClient(context).authorize(request()))
    }

    /** Arka plan için: ekran açmadan jeton; izin yoksa null. */
    suspend fun tokenSilently(context: Context): String? =
        runCatching { authorize(context).let { if (it.hasResolution()) null else it.accessToken } }.getOrNull()

    suspend fun list(token: String): List<RemoteBackup> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name contains '$PREFIX' and trashed = false", "UTF-8")
        val body = http(
            "GET",
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&orderBy=name%20desc&pageSize=50&fields=files(id,name,modifiedTime,size)",
            token
        )
        val arr = JSONObject(body).optJSONArray("files") ?: return@withContext emptyList()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            RemoteBackup(o.getString("id"), o.getString("name"), o.optString("modifiedTime"), o.optString("size", "0").toLongOrNull() ?: 0)
        }.sortedByDescending { it.name }
    }

    suspend fun download(token: String, id: String): String = withContext(Dispatchers.IO) {
        http("GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", token)
    }

    /** Bugünün dosyasını oluşturur ya da günceller, 7 günden eskileri siler. */
    suspend fun save(token: String, json: String, today: LocalDate = LocalDate.now()): Unit = withContext(Dispatchers.IO) {
        val name = "$PREFIX$today.json"
        val existing = list(token)
        val same = existing.firstOrNull { it.name == name }
        if (same != null) {
            http("PATCH", "https://www.googleapis.com/upload/drive/v3/files/${same.id}?uploadType=media", token, json, "application/json")
        } else {
            val boundary = "fittakip${System.currentTimeMillis()}"
            val meta = JSONObject().put("name", name).put("parents", org.json.JSONArray().put("appDataFolder"))
            val multipart = buildString {
                append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                append(meta.toString()).append("\r\n")
                append("--$boundary\r\nContent-Type: application/json\r\n\r\n")
                append(json).append("\r\n--$boundary--")
            }
            http("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart", token, multipart, "multipart/related; boundary=$boundary")
        }
        (existing.filter { it.name != name }.sortedByDescending { it.name }.drop(KEEP - 1)).forEach {
            runCatching { http("DELETE", "https://www.googleapis.com/drive/v3/files/${it.id}", token) }
        }
    }

    /** Drive'daki tüm FitTakip yedeklerini siler; silinen dosya sayısını döner. */
    suspend fun deleteAll(token: String): Int = withContext(Dispatchers.IO) {
        val all = list(token)
        all.forEach { http("DELETE", "https://www.googleapis.com/drive/v3/files/${it.id}", token) }
        all.size
    }

    private fun http(method: String, url: String, token: String, body: String? = null, type: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            if (method == "PATCH") {
                c.requestMethod = "POST"
                c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else c.requestMethod = method
            c.connectTimeout = 20_000
            c.readTimeout = 30_000
            c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", type ?: "application/json")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw DriveException(explain(code, text))
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun explain(code: Int, body: String): String = when {
        body.contains("accessNotConfigured") || body.contains("SERVICE_DISABLED") ->
            "Google Drive API bu projede kapalı. Google Cloud'da Drive API'yi etkinleştir."
        code == 401 -> "Google oturumunun süresi dolmuş. Drive yedeğini kapatıp yeniden aç."
        code == 403 -> "Drive erişimi reddedildi ($code)."
        else -> "Drive hatası ($code)."
    }
}
