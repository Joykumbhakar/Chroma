package com.chroma.studio.chat.ui.theme

import com.chroma.studio.chat.data.AccentColorType
import com.chroma.studio.chat.data.WallpaperType


import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage



// ======================================================
// Design System: Spacing & Shape Tokens
// ======================================================

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = 50.dp
}

// ======================================================
// Chat Design Token System
// ======================================================

@Immutable
data class ChatColors(
    // Accent
    val iosBlue: Color,
    val iosBlueHover: Color,
    val iosBlueSubtle: Color,
    // App backgrounds
    val chatBg: Color,
    val appBg: Color,
    val sidebarBg: Color,
    val cardBg: Color,
    val headerBg: Color,
    // Surface tiers
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    // Bubbles
    val bubbleGray: Color,
    // Text
    val textDark: Color,
    val textMuted: Color,
    val textTertiary: Color,
    // Input
    val inputBg: Color,
    // Borders
    val borderSubtle: Color,
    val separator: Color,
    val separatorOpaque: Color,
    // System grays
    val systemGray5: Color,
    val systemGray6: Color,
    // Destructive
    val destructive: Color,
    val success: Color,
    val isLight: Boolean
)

val LocalChatColors = staticCompositionLocalOf<ChatColors> {
    error("No ChatColors provided")
}

@Composable
fun ChatAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColorType = AccentColorType.BLUE,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        ChatColors(
            iosBlue = accentColor.color,
            iosBlueHover = accentColor.hoverColor,
            iosBlueSubtle = accentColor.subtleColor,
            chatBg = ChatBackgroundDark,
            appBg = BackgroundDark,
            sidebarBg = SurfaceDark,
            cardBg = SurfaceDark,
            headerBg = NavBarDark,
            surfaceSecondary = SurfaceSecondaryDark,
            surfaceTertiary = SurfaceTertiaryDark,
            bubbleGray = BubbleIncomingDark,
            textDark = LabelPrimaryDark,
            textMuted = LabelSecondaryDark,
            textTertiary = LabelTertiaryDark,
            inputBg = SurfaceDark,
            borderSubtle = BorderSubtleDark,
            separator = SeparatorDark,
            separatorOpaque = SeparatorOpaqueDark,
            systemGray5 = SystemGray5Dark,
            systemGray6 = SystemGray6Dark,
            destructive = DestructiveDark,
            success = AccentGreenDark,
            isLight = false
        )
    } else {
        ChatColors(
            iosBlue = accentColor.color,
            iosBlueHover = accentColor.hoverColor,
            iosBlueSubtle = accentColor.subtleColor,
            chatBg = ChatBackgroundLight,
            appBg = BackgroundLight,
            sidebarBg = SurfaceLight,
            cardBg = SurfaceLight,
            headerBg = NavBarLight,
            surfaceSecondary = SurfaceSecondaryLight,
            surfaceTertiary = SurfaceTertiaryLight,
            bubbleGray = BubbleIncomingLight,
            textDark = LabelPrimaryLight,
            textMuted = LabelSecondaryLight,
            textTertiary = LabelTertiaryLight,
            inputBg = SurfaceLight,
            borderSubtle = BorderSubtleLight,
            separator = SeparatorLight,
            separatorOpaque = SeparatorOpaqueLight,
            systemGray5 = SystemGray5Light,
            systemGray6 = SystemGray6Light,
            destructive = Destructive,
            success = AccentGreen,
            isLight = true
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalChatColors provides colors) {
        MaterialTheme(
            typography = Typography,
            shapes = MaterialTheme.shapes.copy(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}

// ======================================================
// Wallpaper Background
// ======================================================

@Composable
fun ChatWallpaperBackground(
    wallpaperType: WallpaperType,
    dimmingAlpha: Float = 0.15f,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current

    Box(modifier = modifier.fillMaxSize().background(colors.appBg)) {
        AsyncImage(
            model = wallpaperType.drawableRes,
            contentDescription = wallpaperType.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Ambient overlay for legibility over wallpapers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (colors.isLight) Color.White.copy(alpha = dimmingAlpha)
                    else Color.Black.copy(alpha = (dimmingAlpha + 0.2f).coerceAtMost(0.85f))
                )
        )
    }
}



