package com.fittakip.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fittakip.app.MainActivity
import com.fittakip.app.R
import com.fittakip.app.data.AppDatabase
import com.fittakip.app.domain.Gamification
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "daily_reminder"
    const val CHANNEL = "reminders"

    fun schedule(context: Context, hour: Int) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    /** Her sabah 08:30'da günün sözü ve seri durumu. */
    fun setMorning(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork("morning"); return }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(8, 30)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val req = PeriodicWorkRequestBuilder<MorningWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork("morning", ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** 2 saatte bir, 09-21 arası, su hedefinin gerisindeysen hatırlatır. */
    fun setWater(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork("water"); return }
        val req = PeriodicWorkRequestBuilder<WaterWorker>(2, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork("water", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, "Günlük hatırlatma", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Serini korumak için akşam hatırlatması"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, title: String, text: String, id: Int = 1) {
        if (!canNotify(context)) return
        createChannel(context)
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_fit)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}

/** Akşam çalışır: bugün hiç kayıt yoksa seriyi hatırlatır. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now().toEpochDay()
        val days = AppDatabase.get(applicationContext).dao().activeDays().toSet()
        if (today in days) return Result.success()
        val streak = Gamification.currentStreak(days, today)
        if (streak > 0) {
            Reminders.show(
                applicationContext,
                "🔥 $streak günlük serin tehlikede!",
                "Bugün henüz bir şey kaydetmedin. Bir öğün, bir bardak su ya da bir set yeter; serin devam etsin."
            )
        } else {
            Reminders.show(
                applicationContext,
                "Bugün kendin için 1 dakika ayır 💪",
                "Bir kayıt gir ve yeni bir seri başlat. Küçük adımlar büyük fark yaratır."
            )
        }
        return Result.success()
    }
}


class MorningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now().toEpochDay()
        val days = AppDatabase.get(applicationContext).dao().activeDays().toSet()
        val streak = Gamification.currentStreak(days, today)
        val quote = com.fittakip.app.data.QUOTES[(today % com.fittakip.app.data.QUOTES.size).toInt()]
        val st = com.fittakip.app.data.SettingsStore(applicationContext).state.value
        val dayName = com.fittakip.app.data.WEEKDAY_NAMES[LocalDate.now().dayOfWeek.value - 1]
        val plan = st.planFor(today)
        val tpl = st.templateFor(today)
        val title = when {
            plan == com.fittakip.app.data.OFF_DAY -> "Günaydın! $dayName: off day 😌"
            tpl != null -> "Günaydın! $dayName: ${tpl.name} günü 💪"
            streak > 0 -> "Günaydın! 🔥 $streak günlük serin seni bekliyor"
            else -> "Günaydın! Bugün yeni bir başlangıç ☀️"
        }
        val body = when {
            plan == com.fittakip.app.data.OFF_DAY -> "Bugün dinlen. Hafif yürüyüş, esneme ve bol su. Kaslar dinlenirken büyür."
            tpl != null -> "Bugünkü görevin: " + tpl.items.joinToString(", ") { "${it.exercise} ${it.label}" }
            else -> quote
        }
        Reminders.show(applicationContext, title, body, 2)
        return Result.success()
    }
}

class WaterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val hour = LocalDateTime.now().hour
        if (hour !in 9..21) return Result.success()
        val dao = AppDatabase.get(applicationContext).dao()
        val st = com.fittakip.app.data.SettingsStore(applicationContext).state.value
        val goal = Gamification.waterGoal(dao.profileNow(), st)
        val drank = dao.waterOn(LocalDate.now().toEpochDay())
        // Gün içinde beklenen miktar: 09:00'da 0, 21:00'de hedefin tamamı
        val expected = goal * (hour - 9) / 12
        if (drank < expected - 250) {
            Reminders.show(applicationContext, "💧 Su içme vakti", "Bugün $drank / $goal ml içtin. Bir bardak su iyi gelir.", 3)
        }
        return Result.success()
    }
}
