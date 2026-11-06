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

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL, "Günlük hatırlatma", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Serini korumak için akşam hatırlatması"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, title: String, text: String) {
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
            NotificationManagerCompat.from(context).notify(1, n)
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
