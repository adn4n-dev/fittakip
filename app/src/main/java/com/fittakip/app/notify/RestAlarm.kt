package com.fittakip.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fittakip.app.MainActivity
import com.fittakip.app.R

/**
 * Dinlenme sayacı uygulama kapalıyken de çalışsın diye:
 * geri sayan bir bildirim + süre dolunca titreşimli "Sıradaki set" bildirimi.
 */
object RestAlarm {
    private const val LIVE = "rest_live"
    private const val DONE = "rest_done"
    private const val LIVE_ID = 20
    private const val DONE_ID = 21

    private fun channels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(LIVE, "Dinlenme sayacı", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); enableVibration(false)
        })
        nm.createNotificationChannel(NotificationChannel(DONE, "Dinlenme bitti", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 400, 200, 400)
        })
    }

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 30, Intent(context, RestDoneReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 31, Intent(context, MainActivity::class.java).putExtra("shortcut", "workout"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun schedule(context: Context, endMillis: Long) {
        channels(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context)
        am.cancel(pi)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exactAllowed) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
        }
        if (!Reminders.canNotify(context)) return
        val n = NotificationCompat.Builder(context, LIVE)
            .setSmallIcon(R.drawable.ic_stat_fit)
            .setContentTitle("Dinlenme")
            .setContentText("Sıradaki sete hazırlan")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context))
            .build()
        try { NotificationManagerCompat.from(context).notify(LIVE_ID, n) } catch (_: SecurityException) {}
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        NotificationManagerCompat.from(context).cancel(LIVE_ID)
    }

    fun fire(context: Context) {
        channels(context)
        NotificationManagerCompat.from(context).cancel(LIVE_ID)
        if (!Reminders.canNotify(context)) return
        val n = NotificationCompat.Builder(context, DONE)
            .setSmallIcon(R.drawable.ic_stat_fit)
            .setContentTitle("⏱️ Dinlenme bitti")
            .setContentText("Sıradaki sete geç! 💪")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(60_000)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        try { NotificationManagerCompat.from(context).notify(DONE_ID, n) } catch (_: SecurityException) {}
    }
}

class RestDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestAlarm.fire(context)
    }
}
