package com.fittakip.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fotoğraftan kalori tahmini: fotoğraf küçültülüp kullanıcının kendi Claude API anahtarıyla
 * Anthropic'e gönderilir; tabaktaki yiyecekler ve tahmini kalori/makrolar döner.
 * Bu bir TAHMİNDİR (porsiyon gözle kestirilir), kullanıcı eklemeden önce düzeltebilir.
 */
object FoodVision {
    private const val MODEL = "claude-sonnet-5-5"

    data class Guess(val name: String, val portion: String, val kcal: Int, val p: Double, val c: Double, val f: Double)
    data class Result(val items: List<Guess>, val note: String)
    class VisionException(message: String) : Exception(message)

    private val PROMPT = """
        Bu fotoğraftaki yiyecek ve içecekleri tanı ve her biri için fotoğraftaki porsiyonu gözle tahmin et.
        Türk mutfağını iyi bil (pilav, kuru fasulye, mercimek çorbası, lahmacun, simit, ayran vb.).
        Yağ ve sos gibi görünmeyen kalorileri makul ölçüde hesaba kat.
        SADECE şu JSON'u döndür, başka hiçbir şey yazma:
        {"items":[{"name":"Türkçe kısa ad","portion":"tahmini porsiyon, ör. 1 tabak (~250 g)","kcal":0,"protein":0,"carb":0,"fat":0}],
         "note":"Türkçe tek cümle: emin olmadığın şeyler ya da porsiyonu netleştirmek için ipucu"}
        kcal tam sayı; protein, carb, fat gram. Fotoğrafta yiyecek yoksa items boş dizi olsun ve note'ta bunu söyle.
    """.trimIndent()

    suspend fun analyze(context: Context, uri: Uri, apiKey: String): Result = withContext(Dispatchers.IO) {
        val jpeg = loadScaled(context, uri) ?: throw VisionException("Fotoğraf açılamadı.")
        val body = JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 1024)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", JSONArray()
                .put(JSONObject().put("type", "image").put("source", JSONObject()
                    .put("type", "base64").put("media_type", "image/jpeg")
                    .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))))
                .put(JSONObject().put("type", "text").put("text", PROMPT)))))
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        val text = try {
            c.requestMethod = "POST"
            c.connectTimeout = 20_000
            c.readTimeout = 90_000
            c.doOutput = true
            c.setRequestProperty("content-type", "application/json")
            c.setRequestProperty("x-api-key", apiKey.trim())
            c.setRequestProperty("anthropic-version", "2023-06-01")
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val resp = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw VisionException(explain(code, resp))
            val content = JSONObject(resp).optJSONArray("content") ?: JSONArray()
            (0 until content.length()).map { content.getJSONObject(it) }.filter { it.optString("type") == "text" }.joinToString("") { it.optString("text") }
        } finally {
            c.disconnect()
        }
        parse(text)
    }

    /** Modelin cevabındaki JSON'u ayıklar. */
    fun parse(text: String): Result {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw VisionException("Fotoğraf yorumlanamadı, tekrar dene.")
        val o = JSONObject(text.substring(start, end + 1))
        val arr = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull {
            val i = arr.getJSONObject(it)
            val name = i.optString("name").trim()
            val kcal = i.optDouble("kcal", Double.NaN)
            if (name.isBlank() || kcal.isNaN() || kcal < 0 || kcal > 5000) null
            else Guess(name, i.optString("portion"), kcal.roundToInt(), i.optDouble("protein", 0.0), i.optDouble("carb", 0.0), i.optDouble("fat", 0.0))
        }
        return Result(items, o.optString("note"))
    }

    private fun explain(code: Int, body: String): String = when {
        code == 401 -> "API anahtarı geçersiz. Ayarlar'dan kontrol et."
        body.contains("credit balance", true) -> "Anthropic hesabında kredi kalmamış (console.anthropic.com > Billing)."
        code == 429 -> "Çok sık denendi, biraz bekleyip tekrar dene."
        code == 529 || code >= 500 -> "Claude şu an yoğun, birazdan tekrar dene."
        else -> "Fotoğraf analiz edilemedi ($code)."
    }

    /** En uzun kenarı 1024 px, JPEG %80; telefon yan çektiyse düzeltir. */
    private fun loadScaled(context: Context, uri: Uri): ByteArray? {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1024) sample *= 2
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = runCatching {
            cr.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        if (rotation != 0) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val scale = 1024f / max(bmp.width, bmp.height)
        if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        return ByteArrayOutputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it); it.toByteArray() }
    }
}
