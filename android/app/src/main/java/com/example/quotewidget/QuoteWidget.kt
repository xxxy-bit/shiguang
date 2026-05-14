package com.example.quotewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
    }

    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Apply custom styles
        val fontSize = WidgetPrefs.getFontSize(context)
        val textColor = WidgetPrefs.getTextColor(context)
        val bgColor = WidgetPrefs.getBgColor(context)

        views.setTextViewTextSize(R.id.quote_text, android.util.TypedValue.COMPLEX_UNIT_SP, fontSize)
        views.setTextColor(R.id.quote_text, textColor)
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        // Tap to refresh
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

        // Fetch quote
        Thread {
            val text = QuoteApi.fetchRandomText()
            val displayText = text ?: context.getString(R.string.error)
            views.setTextViewText(R.id.quote_text, displayText)
            manager.updateAppWidget(widgetId, views)
        }.start()
    }
}
