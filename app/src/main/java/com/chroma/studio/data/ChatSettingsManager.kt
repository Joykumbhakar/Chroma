package com.chroma.studio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ChatTheme {
    DEFAULT,    // iOS Classic Blue & Glass
    OCEAN,      // Teal & Aqua
    MONOCHROME, // Dark Slate & Minimal
    SUNSET,     // Rose & Amber
    EMERALD,    // Mint & Forest Green
    LAVENDER    // Violet & Lilac
}

enum class ChatTextSize {
    SMALL,
    NORMAL,
    LARGE,
    EXTRA_LARGE
}

enum class WallpaperType {
    AURORA,     // Animated iOS Aurora space background
    SOLID,      // WhatsApp / Apple Pastel & Dark Solid Colors
    GRADIENT,   // Apple Spatial Gradients
    DOODLE,     // WhatsApp-style subtle icon doodle pattern
    GALLERY     // Custom photo selected from phone gallery
}

enum class BubbleStyle {
    CLASSIC,    // Apple iOS default tail style
    ROUNDED,    // Modern fully rounded rectangles
    MINIMAL     // No tails, subtle margins
}

data class WallpaperConfig(
    val type: WallpaperType = WallpaperType.AURORA,
    val value: String = "",         // Hex color code, gradient key, or photo URI string
    val doodleEnabled: Boolean = false,
    val doodleOpacity: Float = 0.12f,
    val dimAmount: Float = 0.0f,     // 0.0f to 0.60f
    val blurAmount: Float = 0.0f,     // 0.0f to 25.0f
    val bubbleStyle: BubbleStyle = BubbleStyle.CLASSIC,
    val customIncomingColor: String? = null,
    val customOutgoingColor: String? = null,
    val customIncomingTextColor: String? = null,
    val customOutgoingTextColor: String? = null
)

// Pre-defined wallpaper palettes (WhatsApp & Apple Inspired)
object ChatWallpaperPresets {
    val solidColors = listOf(
        "#EFEAE2", // WhatsApp Classic Parchment
        "#E5DDD5", // WhatsApp Vintage Cream
        "#D7E6DB", // Sage Green
        "#D3E5F3", // Sky Blue
        "#E2DEF2", // Soft Lavender
        "#F4DFDF", // Rose Blush
        "#EAEAEA", // Clean Minimal Grey
        "#FFF7D6", // Soft Buttercup
        "#1E242B", // WhatsApp Dark Slate
        "#131B2E", // Midnight Blue
        "#142520", // Deep Forest Green
        "#24172E", // Deep Plum
        "#0F0F14", // Obsidian Black
        "#1C1C1E"  // Apple Dark System
    )

    val gradientPresets = listOf(
        "sunset" to listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68), Color(0xFFFF9500)),
        "ocean" to listOf(Color(0xFF00C6FF), Color(0xFF0072FF), Color(0xFF0A84FF)),
        "aurora" to listOf(Color(0xFF30D158), Color(0xFF0A84FF), Color(0xFF5E5CE6)),
        "cosmic" to listOf(Color(0xFFBF5AF2), Color(0xFFFF2D55), Color(0xFF5856D6)),
        "emerald" to listOf(Color(0xFF34C759), Color(0xFF30B0C7), Color(0xFF00C7BE)),
        "midnight" to listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFF3A3A3C)),
        "lavender" to listOf(Color(0xFFAF52DE), Color(0xFF5E5CE6), Color(0xFF64D2FF)),
        "golden" to listOf(Color(0xFFFF9F0A), Color(0xFFFFD60A), Color(0xFFFF375F))
    )
}

class ChatSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_settings_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        try {
            ChatTheme.valueOf(prefs.getString("theme", ChatTheme.DEFAULT.name) ?: ChatTheme.DEFAULT.name)
        } catch (e: Exception) {
            ChatTheme.DEFAULT
        }
    )
    val theme: StateFlow<ChatTheme> = _theme.asStateFlow()

    private val _textSize = MutableStateFlow(
        try {
            ChatTextSize.valueOf(prefs.getString("text_size", ChatTextSize.NORMAL.name) ?: ChatTextSize.NORMAL.name)
        } catch (e: Exception) {
            ChatTextSize.NORMAL
        }
    )
    val textSize: StateFlow<ChatTextSize> = _textSize.asStateFlow()

    private val _globalWallpaper = MutableStateFlow(loadWallpaper(null))
    val globalWallpaper: StateFlow<WallpaperConfig> = _globalWallpaper.asStateFlow()

    private val roomWallpaperFlows = mutableMapOf<String, MutableStateFlow<WallpaperConfig>>()

    fun setTheme(theme: ChatTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _theme.value = theme
    }

    fun setTextSize(textSize: ChatTextSize) {
        prefs.edit().putString("text_size", textSize.name).apply()
        _textSize.value = textSize
    }

    fun getWallpaperForRoom(roomId: String?): StateFlow<WallpaperConfig> {
        if (roomId == null) return globalWallpaper
        return roomWallpaperFlows.getOrPut(roomId) {
            MutableStateFlow(loadWallpaper(roomId))
        }.asStateFlow()
    }

    fun setWallpaperForRoom(config: WallpaperConfig, roomId: String?) {
        val prefix = if (roomId != null) "wp_${roomId}_" else "wp_global_"
        prefs.edit()
            .putString("${prefix}type", config.type.name)
            .putString("${prefix}value", config.value)
            .putBoolean("${prefix}doodle", config.doodleEnabled)
            .putFloat("${prefix}doodle_opacity", config.doodleOpacity)
            .putFloat("${prefix}dim", config.dimAmount)
            .putFloat("${prefix}blur", config.blurAmount)
            .putString("${prefix}bubble_style", config.bubbleStyle.name)
            .putString("${prefix}inc_color", config.customIncomingColor)
            .putString("${prefix}out_color", config.customOutgoingColor)
            .putString("${prefix}inc_txt_color", config.customIncomingTextColor)
            .putString("${prefix}out_txt_color", config.customOutgoingTextColor)
            .apply()

        if (roomId != null) {
            val flow = roomWallpaperFlows.getOrPut(roomId) { MutableStateFlow(config) }
            flow.value = config
        } else {
            _globalWallpaper.value = config
        }
    }

    fun resetWallpaper(roomId: String?) {
        if (roomId != null) {
            val prefix = "wp_${roomId}_"
            prefs.edit()
                .remove("${prefix}type")
                .remove("${prefix}value")
                .remove("${prefix}doodle")
                .remove("${prefix}doodle_opacity")
                .remove("${prefix}dim")
                .remove("${prefix}blur")
                .remove("${prefix}bubble_style")
                .remove("${prefix}inc_color")
                .remove("${prefix}out_color")
                .remove("${prefix}inc_txt_color")
                .remove("${prefix}out_txt_color")
                .apply()
            val flow = roomWallpaperFlows[roomId]
            if (flow != null) {
                flow.value = _globalWallpaper.value
            }
        } else {
            val default = WallpaperConfig(WallpaperType.AURORA)
            setWallpaperForRoom(default, null)
        }
    }

    private fun loadWallpaper(roomId: String?): WallpaperConfig {
        val prefix = if (roomId != null && prefs.contains("wp_${roomId}_type")) {
            "wp_${roomId}_"
        } else {
            "wp_global_"
        }

        val typeName = prefs.getString("${prefix}type", WallpaperType.AURORA.name) ?: WallpaperType.AURORA.name
        val type = try { WallpaperType.valueOf(typeName) } catch (e: Exception) { WallpaperType.AURORA }
        val value = prefs.getString("${prefix}value", "") ?: ""
        val doodle = prefs.getBoolean("${prefix}doodle", false)
        val doodleOpacity = prefs.getFloat("${prefix}doodle_opacity", 0.12f)
        val dim = prefs.getFloat("${prefix}dim", 0.0f)
        val blur = prefs.getFloat("${prefix}blur", 0.0f)
        val bubbleStyleStr = prefs.getString("${prefix}bubble_style", BubbleStyle.CLASSIC.name) ?: BubbleStyle.CLASSIC.name
        val bubbleStyle = try { BubbleStyle.valueOf(bubbleStyleStr) } catch(e: Exception) { BubbleStyle.CLASSIC }

        return WallpaperConfig(
            type = type,
            value = value,
            doodleEnabled = doodle,
            doodleOpacity = doodleOpacity,
            dimAmount = dim,
            blurAmount = blur,
            bubbleStyle = bubbleStyle,
            customIncomingColor = prefs.getString("${prefix}inc_color", null),
            customOutgoingColor = prefs.getString("${prefix}out_color", null),
            customIncomingTextColor = prefs.getString("${prefix}inc_txt_color", null),
            customOutgoingTextColor = prefs.getString("${prefix}out_txt_color", null)
        )
    }
}
