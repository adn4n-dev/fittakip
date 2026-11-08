package com.fittakip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.launch
import com.fittakip.app.MainActivity
import com.fittakip.app.R

/** Ana ekran widget'ı: seri, kalan kalori ve su. Değerleri uygulama her güncellemede kaydeder. */
class FitWidget : AppWidgetProvider() {

    /** Uygulama kapalıyken de (ör. sabah) veritabanından taze değerleri hesaplar. */
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val dao = com.fittakip.app.data.AppDatabase.get(context).dao()
                val st = com.fittakip.app.data.SettingsStore.get(context).state.value
                val today = java.time.LocalDate.now().toEpochDay()
                val days = dao.streakDays().toSet() + st.frozen
                val streak = com.fittakip.app.domain.Gamification.currentStreak(days, today)
                val p = dao.profileNow()
                val left = p?.let { com.fittakip.app.domain.Calculator.calculate(it, st).target - dao.kcalOn(today) }
                save(context, streak, left, dao.waterOn(today), com.fittakip.app.domain.Gamification.waterGoal(p, st))
            } catch (_: Exception) {
                ids.forEach { manager.updateAppWidget(it, views(context)) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val PREFS = "fit_widget"

        fun save(context: Context, streak: Int, kcalLeft: Int?, water: Int, waterGoal: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("streak", streak)
                .putInt("kcal", kcalLeft ?: Int.MIN_VALUE)
                .putInt("water", water)
                .putInt("waterGoal", waterGoal)
                .apply()
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FitWidget::class.java))
            ids.forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val kcal = p.getInt("kcal", Int.MIN_VALUE)
            val v = RemoteViews(context.packageName, R.layout.widget_fit)
            v.setTextViewText(R.id.w_streak, "🔥 ${p.getInt("streak", 0)}")
            v.setTextViewText(
                R.id.w_kcal,
                when {
                    kcal == Int.MIN_VALUE -> "Profil yok"
                    kcal >= 0 -> "$kcal kcal kaldı"
                    else -> "${-kcal} kcal fazla"
                }
            )
            v.setTextViewText(R.id.w_water, "💧 ${p.getInt("water", 0)} / ${p.getInt("waterGoal", 0)} ml")
            val intent = PendingIntent.getActivity(
                context, 10, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_root, intent)
            return v
        }
    }
}
