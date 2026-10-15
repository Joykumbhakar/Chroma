package com.chroma.studio.chat.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "chatapp_settings_prefs"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_ACCENT_COLOR = "pref_accent_color"
        private const val KEY_WALLPAPER_TYPE = "pref_wallpaper_type"
        private const val KEY_WALLPAPER_DIMMING = "pref_wallpaper_dimming"
        private const val KEY_BUBBLE_STYLE = "pref_bubble_style"
        private const val KEY_GLOBE_STYLE = "pref_globe_style"
        private const val KEY_ANIMATION_STYLE = "pref_animation_style"
        private const val KEY_CORNER_CUSTOM_ENABLED = "pref_corner_custom_enabled"
        private const val KEY_CORNER_TOP_START = "pref_corner_top_start"
        private const val KEY_CORNER_TOP_END = "pref_corner_top_end"
        private const val KEY_CORNER_BOTTOM_START = "pref_corner_bottom_start"
        private const val KEY_CORNER_BOTTOM_END = "pref_corner_bottom_end"
        private const val KEY_POINTY_STYLE = "pref_pointy_style"
        private const val KEY_CORNER_APPLY_BOTH = "pref_corner_apply_both"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var accentColor: AccentColorType
        get() {
            val name = prefs.getString(KEY_ACCENT_COLOR, AccentColorType.BLUE.name)
            return runCatching { AccentColorType.valueOf(name ?: "") }.getOrDefault(AccentColorType.BLUE)
        }
        set(value) = prefs.edit().putString(KEY_ACCENT_COLOR, value.name).apply()

    var wallpaperType: WallpaperType
        get() {
            val name = prefs.getString(KEY_WALLPAPER_TYPE, WallpaperType.WALLPAPER_HD_1.name)
            val parsed = runCatching { WallpaperType.valueOf(name ?: "") }.getOrDefault(WallpaperType.WALLPAPER_HD_1)
            return if (parsed == WallpaperType.DEFAULT) WallpaperType.WALLPAPER_HD_1 else parsed
        }
        set(value) = prefs.edit().putString(KEY_WALLPAPER_TYPE, value.name).apply()

    var wallpaperDimming: Float
        get() = prefs.getFloat(KEY_WALLPAPER_DIMMING, 0.15f)
        set(value) = prefs.edit().putFloat(KEY_WALLPAPER_DIMMING, value).apply()

    var bubbleStyle: BubbleStyleType
        get() {
            val name = prefs.getString(KEY_BUBBLE_STYLE, BubbleStyleType.ROUNDED.name)
            return runCatching { BubbleStyleType.valueOf(name ?: "") }.getOrDefault(BubbleStyleType.ROUNDED)
        }
        set(value) = prefs.edit().putString(KEY_BUBBLE_STYLE, value.name).apply()

    var globeStyle: VoiceGlobeStyleType
        get() {
            val name = prefs.getString(KEY_GLOBE_STYLE, VoiceGlobeStyleType.ORIGINAL_BLUE.name)
            return runCatching { VoiceGlobeStyleType.valueOf(name ?: "") }.getOrDefault(VoiceGlobeStyleType.ORIGINAL_BLUE)
        }
        set(value) = prefs.edit().putString(KEY_GLOBE_STYLE, value.name).apply()

    var animationStyle: BubbleAnimationStyle
        get() {
            val name = prefs.getString(KEY_ANIMATION_STYLE, BubbleAnimationStyle.POP_ZOOM.name)
            return runCatching { BubbleAnimationStyle.valueOf(name ?: "") }.getOrDefault(BubbleAnimationStyle.POP_ZOOM)
        }
        set(value) = prefs.edit().putString(KEY_ANIMATION_STYLE, value.name).apply()

    var customCorners: CustomCornerConfig
        get() {
            val enabled = prefs.getBoolean(KEY_CORNER_CUSTOM_ENABLED, false)
            val topStart = prefs.getFloat(KEY_CORNER_TOP_START, 18f)
            val topEnd = prefs.getFloat(KEY_CORNER_TOP_END, 18f)
            val bottomStart = prefs.getFloat(KEY_CORNER_BOTTOM_START, 18f)
            val bottomEnd = prefs.getFloat(KEY_CORNER_BOTTOM_END, 4f)
            val pointyName = prefs.getString(KEY_POINTY_STYLE, PointyCornerStyle.SUBTLE_ROUNDED.name)
            val pointy = runCatching { PointyCornerStyle.valueOf(pointyName ?: "") }.getOrDefault(PointyCornerStyle.SUBTLE_ROUNDED)
            val applyBoth = prefs.getBoolean(KEY_CORNER_APPLY_BOTH, true)
            return CustomCornerConfig(
                customEnabled = enabled,
                topStart = topStart,
                topEnd = topEnd,
                bottomStart = bottomStart,
                bottomEnd = bottomEnd,
                pointyCornerStyle = pointy,
                applyToBoth = applyBoth
            )
        }
        set(value) {
            prefs.edit()
                .putBoolean(KEY_CORNER_CUSTOM_ENABLED, value.customEnabled)
                .putFloat(KEY_CORNER_TOP_START, value.topStart)
                .putFloat(KEY_CORNER_TOP_END, value.topEnd)
                .putFloat(KEY_CORNER_BOTTOM_START, value.bottomStart)
                .putFloat(KEY_CORNER_BOTTOM_END, value.bottomEnd)
                .putString(KEY_POINTY_STYLE, value.pointyCornerStyle.name)
                .putBoolean(KEY_CORNER_APPLY_BOTH, value.applyToBoth)
                .apply()
        }
}

