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
import kotlinx.coroutines.launch

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

    /**
     * Her sabah 08:30'da günün görevi. WorkManager günlük işi saatlerce kaydırabildiği için
     * tam zamanlı alarm kullanılır; alarm çalınca bildirimi gösterir ve ertesi sabahı kurar.
     */
    fun setMorning(context: Context, on: Boolean) {
        WorkManager.getInstance(context).cancelUniqueWork("morning")   // eski periyodik iş
        val am = context.getSystemService(android.app.AlarmManager::class.java)
        val pi = android.app.PendingIntent.getBroadcast(
            context, 41, android.content.Intent(context, MorningAlarmReceiver::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        if (!on) return
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(8, 30)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exact = android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi)
            else am.setWindow(android.app.AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi)
        } catch (_: SecurityException) {
            am.setWindow(android.app.AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi)
        }
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

    fun show(context: Context, title: String, text: String, id: Int = 1, actions: List<NotificationCompat.Action> = emptyList()) {
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
            .apply { actions.forEach { addAction(it) } }
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
        val st = com.fittakip.app.data.SettingsStore.get(applicationContext).state.value
        val dayName = com.fittakip.app.data.WEEKDAY_NAMES[LocalDate.now().dayOfWeek.value - 1]
        val recent = AppDatabase.get(applicationContext).dao().setsSince(minOf(st.rotationStart, today - 120))
        val dayPlan = com.fittakip.app.domain.PlanResolver.forDay(st, recent, today)
        val tpl = (dayPlan as? com.fittakip.app.domain.DayPlan.Workout)?.template
        val off = dayPlan is com.fittakip.app.domain.DayPlan.Off
        val title = when {
            off -> "Günaydın! $dayName: off day 😌"
            tpl != null && st.rotation -> "Günaydın! Sıradaki antrenmanın: ${tpl.name} 💪"
            tpl != null -> "Günaydın! $dayName: ${tpl.name} günü 💪"
            streak > 0 -> "Günaydın! 🔥 $streak günlük serin seni bekliyor"
            else -> "Günaydın! Bugün yeni bir başlangıç ☀️"
        }
        val body = when {
            off -> "Bugün dinlen. Hafif yürüyüş, esneme ve bol su. Kaslar dinlenirken büyür."
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
        val st = com.fittakip.app.data.SettingsStore.get(applicationContext).state.value
        val goal = Gamification.waterGoal(dao.profileNow(), st)
        val drank = dao.waterOn(LocalDate.now().toEpochDay())
        // Gün içinde beklenen miktar: 09:00'da 0, 21:00'de hedefin tamamı
        val expected = goal * (hour - 9) / 12
        if (drank < expected - 250) {
            Reminders.show(
                applicationContext, "💧 Su içme vakti", "Bugün $drank / $goal ml içtin. Bir bardak su iyi gelir.", 3,
                listOf(QuickLog.action(applicationContext, "+250 ml", QuickLog.ADD_WATER, 3))
            )
        }
        return Result.success()
    }
}


/** Sabah alarmı (ve telefon yeniden başlayınca alarmı tekrar kurma). */
class MorningAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        val on = com.fittakip.app.data.SettingsStore.get(context).state.value.morningOn
        if (intent.action != android.content.Intent.ACTION_BOOT_COMPLETED && on) {
            WorkManager.getInstance(context).enqueue(androidx.work.OneTimeWorkRequestBuilder<MorningWorker>().build())
        }
        Reminders.setMorning(context, on)
    }
}


/**
 * Öğün saatinde (kahvaltı 08-10, öğle 12-14, akşam 18-20) o öğün henüz girilmediyse ve
 * "her zamanki" bir öğün varsa: "Her zamanki kahvaltın? Ekle" bildirimi. Saatte bir kontrol eder, öğün başına günde bir kez.
 */
class MealNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = com.fittakip.app.data.SettingsStore.get(applicationContext)
        val st = store.state.value
        if (!st.mealNudges || st.fastingMode) return Result.success()
        val meal = when (LocalDateTime.now().hour) {
            in 8..10 -> com.fittakip.app.data.Meal.BREAKFAST
            in 12..14 -> com.fittakip.app.data.Meal.LUNCH
            in 18..20 -> com.fittakip.app.data.Meal.DINNER
            else -> return Result.success()
        }
        val today = LocalDate.now().toEpochDay()
        val key = "$today:${meal.name}"
        if (st.mealNotified.split(',').contains(key)) return Result.success()
        val foods = AppDatabase.get(applicationContext).dao().foodsSince(today - 15)
        if (foods.any { it.epochDay == today && com.fittakip.app.data.Meal.from(it.meal) == meal }) return Result.success()
        val usual = com.fittakip.app.domain.Routine.usual(foods, meal, today) ?: return Result.success()
        val id = 40 + meal.ordinal
        Reminders.show(
            applicationContext, "${meal.emoji} Her zamanki ${com.fittakip.app.data.Meal.yours(meal)}?",
            "${usual.label} · ${usual.kcal} kcal\nDokunmadan ekleyebilirsin.", id,
            listOf(QuickLog.action(applicationContext, "✓ Ekle", QuickLog.ADD_USUAL, id, meal.name))
        )
        store.update { s -> s.copy(mealNotified = (s.mealNotified.split(',').filter { it.startsWith("$today:") } + key).joinToString(",")) }
        return Result.success()
    }
}

/** Bildirim butonları: uygulamayı açmadan kayıt. */
class QuickLog : android.content.BroadcastReceiver() {
    companion object {
        const val ADD_WATER = "com.fittakip.app.ADD_WATER"
        const val ADD_USUAL = "com.fittakip.app.ADD_USUAL"

        fun action(context: Context, label: String, act: String, notifId: Int, meal: String? = null): NotificationCompat.Action {
            val i = Intent(context, QuickLog::class.java).setAction(act).putExtra("id", notifId).putExtra("meal", meal)
            val pi = PendingIntent.getBroadcast(context, notifId * 10 + act.length, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action.Builder(0, label, pi).build()
        }

        fun scheduleNudges(context: Context, on: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!on) { wm.cancelUniqueWork("meal_nudge"); return }
            wm.enqueueUniquePeriodicWork(
                "meal_nudge", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<MealNudgeWorker>(1, TimeUnit.HOURS).build()
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val id = intent.getIntExtra("id", 0)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).dao()
                val today = LocalDate.now().toEpochDay()
                when (intent.action) {
                    ADD_WATER -> {
                        dao.addWater(com.fittakip.app.data.WaterLog(epochDay = today, ml = 250))
                        Reminders.show(context, "💧 +250 ml eklendi", "Bugün toplam ${dao.waterOn(today)} ml.", id)
                    }
                    ADD_USUAL -> {
                        val meal = com.fittakip.app.data.Meal.from(intent.getStringExtra("meal") ?: "SNACK")
                        val usual = com.fittakip.app.domain.Routine.usual(dao.foodsSince(today - 15), meal, today)
                        if (usual != null) {
                            dao.addFoods(usual.items.map { it.copy(id = 0, epochDay = today, meal = meal.name) })
                            Reminders.show(context, "✓ ${meal.label} eklendi", "${usual.label} · ${usual.kcal} kcal", id)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
