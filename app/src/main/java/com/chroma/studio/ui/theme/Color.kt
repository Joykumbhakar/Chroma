package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Direct port and implementation of the Samsung One UI & Apple Liquid Glass Design Guidelines.
 * Incorporates professional, accessible, high-contrast and centralized design tokens for the entire app.
 * Modifying any token here propagates immediately to every component, screen, and feature.
 */
data class OscellionPalette(
    val bg: Color,
    val gridLine: Color,

    val glassBg: Color,
    val glassBgHover: Color,
    val glassBorder: Color,
    // glass-shadow is emulated via elevation/shadow color in components

    val primary: Color,
    val primaryHover: Color,
    val onPrimary: Color,

    val textMain: Color,
    val textMuted: Color,

    val success: Color,
    val error: Color,
    val warning: Color,

    val isDark: Boolean,

    // Official Samsung One UI Semantic Tokens (Guide Page 62)
    val primaryDark: Color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
    val colorControlActive: Color = Color(0xFF3E91FF),

    // Unified Real-Time Online Presence Tokens
    val onlineStatus: Color = if (isDark) Color(0xFF30D158) else Color(0xFF34C759),
    val offlineStatus: Color = if (isDark) Color(0xFF8E8E93) else Color(0xFFA1A1AA),
    val statusRingBorder: Color = if (isDark) Color(0xFF141416) else Color(0xFFFFFFFF),

    // Unified Apple HIG Surface Tokens (Borderless, Subtle Hierarchy)
    val cardSurface: Color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
    val cardSurfaceElevated: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
    val cardBorder: Color = Color.Transparent,
    val cardBorderSubtle: Color = if (isDark) Color(0x1EFFFFFF) else Color(0x14000000),

    // Unified Apple HIG Shape Radii Tokens (Crisp Rounded Rects, Zero Pills)
    val cardCornerRadius: Dp = 12.dp,
    val buttonCornerRadius: Dp = 10.dp,
    val chipCornerRadius: Dp = 8.dp,
    val pillCornerRadius: Dp = 10.dp,
    val sheetCornerRadius: Dp = 20.dp,

    // Input & Search Field Tokens (Borderless System Fill)
    val inputBg: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFEFEFF4),
    val inputBorder: Color = Color.Transparent,

    // Avatar Styling Tokens
    val avatarBorder: Color = if (isDark) Color(0x22FFFFFF) else Color(0x12000000)
) {
    val primaryGradient: Brush
        get() = Brush.verticalGradient(
            listOf(
                primary,
                primary
            )
        )
}

// ---- Apple HIG Light Palette ----
val LightPalette = OscellionPalette(
    bg = Color(0xFFFFFFFF),           // Clean iOS Pure White Canvas
    gridLine = Color(0x08000000),     // Ultra-subtle hairline grid
    glassBg = Color(0xFFF2F2F7),      // iOS SystemGray6 surface (borderless)
    glassBgHover = Color(0xFFE5E5EA), // iOS SystemGray5 hover/active surface
    glassBorder = Color(0x14000000),  // Hairline separator (#E5E5EA equivalent)
    primary = Color(0xFF007AFF),      // Official Apple System Blue
    primaryHover = Color(0xFF0066D6),
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFF000000),
    textMuted = Color(0xFF6C6C70),    // iOS Secondary Label
    success = Color(0xFF34C759),      // Apple System Green
    error = Color(0xFFFF3B30),        // Apple System Red
    warning = Color(0xFFFF9500),      // Apple System Orange
    isDark = false,
    primaryDark = Color(0xFF0051D5),
    colorControlActive = Color(0xFF007AFF),
    onlineStatus = Color(0xFF34C759),
    offlineStatus = Color(0xFFAEAEB2),
    statusRingBorder = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFF2F2F7),
    cardSurfaceElevated = Color(0xFFFFFFFF),
    cardBorder = Color.Transparent,
    cardBorderSubtle = Color(0x14000000),
    cardCornerRadius = 12.dp,
    buttonCornerRadius = 10.dp,
    chipCornerRadius = 8.dp,
    pillCornerRadius = 10.dp,
    sheetCornerRadius = 20.dp,
    inputBg = Color(0xFFEFEFF4),
    inputBorder = Color.Transparent,
    avatarBorder = Color(0x12000000)
)

// ---- Apple HIG Dark Palette ----
val DarkPalette = OscellionPalette(
    bg = Color(0xFF000000),           // Clean iOS Pure Black Canvas
    gridLine = Color(0x08FFFFFF),     // Subtle dark hairline
    glassBg = Color(0xFF1C1C1E),      // iOS Dark SystemGray6 surface (borderless)
    glassBgHover = Color(0xFF2C2C2E), // Elevated surface hover
    glassBorder = Color(0x1EFFFFFF),  // Hairline dark separator
    primary = Color(0xFF0A84FF),      // Official Apple Dark System Blue
    primaryHover = Color(0xFF409CFF),
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFFFFFFFF),
    textMuted = Color(0xFF8E8E93),    // iOS Dark Secondary Label
    success = Color(0xFF30D158),
    error = Color(0xFFFF453A),
    warning = Color(0xFFFF9F0A),
    isDark = true,
    primaryDark = Color(0xFF409CFF),
    colorControlActive = Color(0xFF0A84FF),
    onlineStatus = Color(0xFF30D158),
    offlineStatus = Color(0xFF8E8E93),
    statusRingBorder = Color(0xFF1C1C1E),
    cardSurface = Color(0xFF1C1C1E),
    cardSurfaceElevated = Color(0xFF2C2C2E),
    cardBorder = Color.Transparent,
    cardBorderSubtle = Color(0x1EFFFFFF),
    cardCornerRadius = 12.dp,
    buttonCornerRadius = 10.dp,
    chipCornerRadius = 8.dp,
    pillCornerRadius = 10.dp,
    sheetCornerRadius = 20.dp,
    inputBg = Color(0xFF2C2C2E),
    inputBorder = Color.Transparent,
    avatarBorder = Color(0x22FFFFFF)
)

// Radii (--radius-sm/md/lg) as dp values, used across components (no full-pill defaults)
object OscellionRadius {
    const val sm = 6
    const val md = 10
    const val lg = 14
    const val pill = 10
}
