package com.chroma.studio.gallery

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Matte iOS-style palette (no glow, hairline borders) â€“ same tokens as the HTML prototype. */
@Immutable
class GalleryColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val hairline: Color,
    val chip: Color,
    val dock: Color,
    val isDark: Boolean,
)

private val LightColors = GalleryColors(
    background = Color(0xFFEFEFF4),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFF2F2F7),
    text = Color(0xFF000000),
    muted = Color(0xFF6C6C70),
    primary = Color(0xFF007AFF),
    hairline = Color(0x17000000),
    chip = Color(0xFFF2F2F7),
    dock = Color(0xF5FFFFFF),
    isDark = false,
)

private val DarkColors = GalleryColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0B0D),
    elevated = Color(0xFF1C1C1E),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFF8E8E93),
    primary = Color(0xFF0A84FF),
    hairline = Color(0x1FFFFFFF),
    chip = Color(0xFF1C1C1E),
    dock = Color(0xF5161618),
    isDark = true,
)

private val LocalGalleryColors = staticCompositionLocalOf { LightColors }

object Gallery {
    val colors: GalleryColors
        @Composable @ReadOnlyComposable get() = LocalGalleryColors.current
}

@Composable
fun OscellionTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(primary = c.primary, background = c.background, surface = c.surface, onSurface = c.text)
    } else {
        lightColorScheme(primary = c.primary, background = c.background, surface = c.surface, onSurface = c.text)
    }
    CompositionLocalProvider(LocalGalleryColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
