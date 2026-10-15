package com.chroma.studio.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val LocalOscellionColors = compositionLocalOf { LightPalette }

/**
 * body { transition: background-color 0.4s ease; } -> animate every color swap the
 * same way the CSS custom properties cross-fade when `.dark` is toggled on <body>.
 */
@Composable
fun OscellionTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val target = if (darkTheme) DarkPalette else LightPalette
    val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 400)

    val animated = OscellionPalette(
        bg = animateColorAsState(target.bg, animSpec, label = "bg").value,
        gridLine = animateColorAsState(target.gridLine, animSpec, label = "grid").value,
        glassBg = animateColorAsState(target.glassBg, animSpec, label = "glassBg").value,
        glassBgHover = animateColorAsState(target.glassBgHover, animSpec, label = "glassBgHover").value,
        glassBorder = animateColorAsState(target.glassBorder, animSpec, label = "glassBorder").value,
        primary = animateColorAsState(target.primary, animSpec, label = "primary").value,
        primaryHover = animateColorAsState(target.primaryHover, animSpec, label = "primaryHover").value,
        onPrimary = target.onPrimary,
        textMain = animateColorAsState(target.textMain, animSpec, label = "textMain").value,
        textMuted = animateColorAsState(target.textMuted, animSpec, label = "textMuted").value,
        success = target.success,
        error = target.error,
        warning = target.warning,
        isDark = darkTheme
    )

    val materialColorScheme = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = animated.primary,
            onPrimary = animated.onPrimary,
            background = animated.bg,
            surface = animated.bg,
            onBackground = animated.textMain,
            onSurface = animated.textMain,
            secondary = animated.primary,
            tertiary = animated.primary
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = animated.primary,
            onPrimary = animated.onPrimary,
            background = animated.bg,
            surface = animated.bg,
            onBackground = animated.textMain,
            onSurface = animated.textMain,
            secondary = animated.primary,
            tertiary = animated.primary
        )
    }

    CompositionLocalProvider(LocalOscellionColors provides animated) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = materialColorScheme
        ) {
            content()
        }
    }
}

// --font: 'Inter' ; --mono: 'JetBrains Mono'
// Drop matching .ttf files into res/font/ as inter_*.ttf / jetbrains_mono_*.ttf and
// wire them up here to get pixel-identical typography. Falls back to system sans/mono.
val InterFontFamily = FontFamily.SansSerif
val MonoFontFamily = FontFamily.Monospace
