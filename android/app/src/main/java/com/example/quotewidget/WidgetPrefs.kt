package com.example.quotewidget

import android.content.Context
import android.content.SharedPreferences

object WidgetPrefs {
    private const val PREFS = "widget_style"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_CORNER = "corner_radius"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFontSize(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_FONT_SIZE, 17f)

    fun setFontSize(ctx: Context, size: Float) =
        prefs(ctx).edit().putFloat(KEY_FONT_SIZE, size).apply()

    fun getTextColor(ctx: Context): Int =
        prefs(ctx).getInt(KEY_TEXT_COLOR, 0xFF333333.toInt())

    fun setTextColor(ctx: Context, color: Int) =
        prefs(ctx).edit().putInt(KEY_TEXT_COLOR, color).apply()

    fun getBgColor(ctx: Context): Int =
        prefs(ctx).getInt(KEY_BG_COLOR, 0xFFFFFFFF.toInt())

    fun setBgColor(ctx: Context, color: Int) =
        prefs(ctx).edit().putInt(KEY_BG_COLOR, color).apply()

    fun getCornerRadius(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_CORNER, 16f)

    fun setCornerRadius(ctx: Context, radius: Float) =
        prefs(ctx).edit().putFloat(KEY_CORNER, radius).apply()
}
