package com.fittakip.app.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fotoğraftan kalori tahmini: fotoğraf küçültülüp Firebase AI Logic üzerinden Gemini'ye gönderilir;
 * tabaktaki yiyecekler ve tahmini kalori/makrolar döner.
 * Bu bir TAHMİNDİR (porsiyon gözle kestirilir), kullanıcı eklemeden önce düzeltebilir.
 */
object FoodVision {

    data class Guess(val name: String, val portion: String, val kcal: Int, val p: Double, val c: Double, val f: Double)
    data class Result(val items: List<Guess>, val note: String)
    class VisionException(message: String) : Exception(message)

    val PROMPT = """
        Bu fotoğraftaki yiyecek ve içecekleri tanı ve her biri için fotoğraftaki porsiyonu gözle tahmin et.
        Türk mutfağını iyi bil (pilav, kuru fasulye, mercimek çorbası, lahmacun, simit, ayran vb.).
        Yağ ve sos gibi görünmeyen kalorileri makul ölçüde hesaba kat.
        SADECE şu JSON'u döndür, başka hiçbir şey yazma:
        {"items":[{"name":"Türkçe kısa ad","portion":"tahmini porsiyon, ör. 1 tabak (~250 g)","kcal":0,"protein":0,"carb":0,"fat":0}],
         "note":"Türkçe tek cümle: emin olmadığın şeyler ya da porsiyonu netleştirmek için ipucu"}
        kcal tam sayı; protein, carb, fat gram. Fotoğrafta yiyecek yoksa items boş dizi olsun ve note'ta bunu söyle.
    """.trimIndent()

    private const val GEMINI = "gemini-3.8-flash"

    /** Firebase AI Logic üzerinden Gemini: anahtar gerekmez, sonuç birkaç saniyede. */
    suspend fun analyze(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        val jpeg = loadScaled(context, uri) ?: throw VisionException("Fotoğraf açılamadı.")
        val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        val text = try {
            val model = Firebase.ai(backend = com.google.firebase.ai.type.GenerativeBackend.googleAI()).generativeModel(GEMINI)
            model.generateContent(com.google.firebase.ai.type.content { image(bmp); text(PROMPT) }).text.orEmpty()
        } catch (e: Exception) {
            val m = (e.message ?: "") + " " + (e.cause?.message ?: "")
            throw VisionException(
                when {
                    m.contains("App Check", true) || m.contains("PERMISSION_DENIED", true) || m.contains("403") ->
                        "Firebase erişimi reddetti. Firebase > App Check'te AI Logic için zorunluluğu kaldır."
                    m.contains("quota", true) || m.contains("429") || m.contains("RESOURCE_EXHAUSTED", true) -> "Günlük ücretsiz kota doldu, yarın tekrar dene."
                    m.contains("not been used", true) || m.contains("disabled", true) -> "Firebase'de AI Logic (Gemini Developer API) henüz açılmamış."
                    m.contains("Unable to resolve host", true) || m.contains("timeout", true) -> "İnternet yok ya da çok yavaş."
                    else -> "Fotoğraf analiz edilemedi: ${e.message ?: e.javaClass.simpleName}"
                }
            )
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


    /** En uzun kenarı 1024 px, JPEG %80; telefon yan çektiyse düzeltir. */
    private fun loadScaled(context: Context, uri: Uri, maxSide: Int = 1024): ByteArray? {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = runCatching {
            cr.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        if (rotation != 0) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val scale = maxSide.toFloat() / max(bmp.width, bmp.height)
        if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        return ByteArrayOutputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it); it.toByteArray() }
    }
}
