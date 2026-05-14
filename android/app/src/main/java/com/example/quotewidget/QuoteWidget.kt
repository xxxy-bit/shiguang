package com.example.quotewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews

class QuoteWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
        scheduleAutoRefresh(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        cancelAutoRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelAutoRefresh(context)
    }

    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val fontSize = WidgetPrefs.getFontSize(context)
        val textColor = WidgetPrefs.getTextColor(context)
        val bgColor = WidgetPrefs.getBgColor(context)

        views.setTextViewTextSize(R.id.quote_text, android.util.TypedValue.COMPLEX_UNIT_SP, fontSize)
        views.setTextColor(R.id.quote_text, textColor)
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        val intent = Intent(context, QuoteWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        }
        val pending = PendingIntent.getBroadcast(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.quote_text, pending)

        manager.updateAppWidget(widgetId, views)

        Thread {
            val text = QuoteApi.fetchRandomText()
            val displayText = text ?: context.getString(R.string.error)
            views.setTextViewText(R.id.quote_text, displayText)
            manager.updateAppWidget(widgetId, views)
        }.start()
    }

    private fun scheduleAutoRefresh(context: Context) {
        val minutes = WidgetPrefs.getIntervalMinutes(context)
        cancelAutoRefresh(context)
        if (minutes <= 0) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuoteWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, QuoteWidget::class.java)))
        }
        val pending = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = minutes * 60L * 1000L
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + intervalMs,
            intervalMs,
            pending
        )
    }

    private fun cancelAutoRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuoteWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pending = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
