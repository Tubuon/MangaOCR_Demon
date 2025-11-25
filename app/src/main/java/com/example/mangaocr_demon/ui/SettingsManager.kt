package com.example.mangaocr_demon.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.example.mangaocr_demon.R

class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SEPIA = "sepia"

        const val MODE_HORIZONTAL = "horizontal"
        const val MODE_VERTICAL = "vertical"
        const val MODE_WEBTOON = "webtoon"
    }

    var theme: String
        get() = prefs.getString("theme", THEME_LIGHT) ?: THEME_LIGHT
        set(value) = prefs.edit().putString("theme", value).apply()

    var readingMode: String
        get() = prefs.getString("reading_mode", MODE_HORIZONTAL) ?: MODE_HORIZONTAL
        set(value) = prefs.edit().putString("reading_mode", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var brightness: Int
        get() = prefs.getInt("brightness", -1)
        set(value) = prefs.edit().putInt("brightness", value).apply()

    // ✅ LẤY MÀU NỀN THEO THEME
    fun getThemeBackgroundColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.background_dark)
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.background_sepia)
            else -> ContextCompat.getColor(context, R.color.background_light)
        }
    }

    // ✅ LẤY MÀU CHỮ THEO THEME
    fun getThemeTextColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.text_primary_dark)
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.text_primary_sepia)
            else -> ContextCompat.getColor(context, R.color.text_primary_light)
        }
    }

    // ✅ LẤY MÀU CHỮ PHỤ (secondary text)
    fun getThemeSecondaryTextColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.text_secondary_dark)
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.text_secondary_sepia)
            else -> ContextCompat.getColor(context, R.color.text_secondary_light)
        }
    }

    // ✅ LẤY MÀU SURFACE (cho cards, containers)
    fun getThemeSurfaceColor(): Int {
        return when (theme) {
            THEME_DARK -> ContextCompat.getColor(context, R.color.surface_dark)
            THEME_SEPIA -> ContextCompat.getColor(context, R.color.surface_sepia)
            else -> ContextCompat.getColor(context, R.color.surface_light)
        }
    }
}