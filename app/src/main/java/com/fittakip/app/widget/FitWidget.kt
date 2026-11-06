package com.fittakip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fittakip.app.MainActivity
import com.fittakip.app.R

/** Ana ekran widget'ı: seri, kalan kalori ve su. Değerleri uygulama her güncellemede kaydeder. */
class FitWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
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
