package com.example.chromastudio.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class ChromaColors(
    val gridLine: Color,
    val glassBg: Color,
    val glassBgHover: Color,
    val glassBorder: Color,
    val primaryHover: Color,
    val textMuted: Color,
    val success: Color,
    val error: Color,
    val warning: Color
)

val LocalChromaColors = staticCompositionLocalOf<ChromaColors> {
    error("No ChromaColors provided")
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBgColor,
    surface = DarkBgColor,
    onBackground = DarkTextMain,
    onSurface = DarkTextMain,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBgColor,
    surface = LightBgColor,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
)

@Composable
fun ChromaStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is set to false to respect exact Chroma colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val chromaColors = if (darkTheme) {
        ChromaColors(
            gridLine = DarkGridLine,
            glassBg = DarkGlassBg,
            glassBgHover = DarkGlassBgHover,
            glassBorder = DarkGlassBorder,
            primaryHover = DarkPrimaryHover,
            textMuted = DarkTextMuted,
            success = SuccessColor,
            error = ErrorColor,
            warning = WarningColor
        )
    } else {
        ChromaColors(
            gridLine = LightGridLine,
            glassBg = LightGlassBg,
            glassBgHover = LightGlassBgHover,
            glassBorder = LightGlassBorder,
            primaryHover = LightPrimaryHover,
            textMuted = LightTextMuted,
            success = SuccessColor,
            error = ErrorColor,
            warning = WarningColor
        )
    }

    CompositionLocalProvider(LocalChromaColors provides chromaColors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
