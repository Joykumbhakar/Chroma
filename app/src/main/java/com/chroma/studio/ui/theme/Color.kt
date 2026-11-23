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

    // Unified Card & Surface Tokens
    val cardSurface: Color = if (isDark) Color(0xFF16181F) else Color(0xFFFFFFFF),
    val cardSurfaceElevated: Color = if (isDark) Color(0xFF1E222D) else Color(0xFFF8FAFC),
    val cardBorder: Color = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0),
    val cardBorderSubtle: Color = if (isDark) Color(0x12FFFFFF) else Color(0x0F000000),

    // Unified Shape Radii Tokens
    val cardCornerRadius: Dp = 22.dp,
    val buttonCornerRadius: Dp = 14.dp,
    val chipCornerRadius: Dp = 10.dp,
    val pillCornerRadius: Dp = 9999.dp,
    val sheetCornerRadius: Dp = 26.dp,

    // Input & Search Field Tokens
    val inputBg: Color = if (isDark) Color(0xFF1C1F26) else Color(0xFFF1F5F9),
    val inputBorder: Color = if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0),

    // Avatar Styling Tokens
    val avatarBorder: Color = if (isDark) Color(0x30FFFFFF) else Color(0x18000000)
) {
    val primaryGradient: Brush
        get() = Brush.verticalGradient(
            listOf(
                primaryHover,
                primary,
                primaryDark
            )
        )
}

// ---- Samsung One UI Light Theme (Page 62) ----
val LightPalette = OscellionPalette(
    bg = Color(0xFFFAFAFA),           // Samsung One UI Light background canvas (#FAFAFA)
    gridLine = Color(0x0A000000),     // Subtle grid line
    glassBg = Color(0xFFFFFFFF),      // Pure white One UI Focus Block (#FFFFFF)
    glassBgHover = Color(0xFFF4F7FB), // Hover container
    glassBorder = Color(0x0D000000),  // Subtle card border
    primary = Color(0xFF0381FE),      // Official One UI Primary Blue (#0381FE)
    primaryHover = Color(0xFF0072DE), // Official One UI Primary Dark Light (#0072DE)
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFF000000),     // Official One UI Black text (#000000)
    textMuted = Color(0xFF71717A),    // Slate 500 secondary text (WCAG 4.5:1 compliant)
    success = Color(0xFF00C853),      // Official One UI Green (Page 63)
    error = Color(0xFFF53B57),        // Official One UI Red (Page 63)
    warning = Color(0xFFF59E0B),
    isDark = false,
    primaryDark = Color(0xFF0072DE),
    colorControlActive = Color(0xFF3E91FF),
    onlineStatus = Color(0xFF34C759),
    offlineStatus = Color(0xFFA1A1AA),
    statusRingBorder = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF),
    cardSurfaceElevated = Color(0xFFF8FAFC),
    cardBorder = Color(0xFFE2E8F0),
    cardBorderSubtle = Color(0x0F000000),
    cardCornerRadius = 22.dp,
    buttonCornerRadius = 14.dp,
    chipCornerRadius = 10.dp,
    pillCornerRadius = 9999.dp,
    sheetCornerRadius = 26.dp,
    inputBg = Color(0xFFF1F5F9),
    inputBorder = Color(0xFFE2E8F0),
    avatarBorder = Color(0x18000000)
)

// ---- Samsung One UI Dark Theme (Page 62) ----
val DarkPalette = OscellionPalette(
    bg = Color(0xFF080808),           // Samsung One UI Dark background canvas (#080808)
    gridLine = Color(0x08FFFFFF),     // Subtle dark grid
    glassBg = Color(0xFF17171A),      // One UI elevated dark focus block container (#17171A)
    glassBgHover = Color(0xFF222226), // Elevated surface hover
    glassBorder = Color(0x18FFFFFF),  // Subtle card border
    primary = Color(0xFF0381FE),      // Official One UI Primary Blue (#0381FE)
    primaryHover = Color(0xFF3E91FF), // Official One UI Primary Dark Dark (#3E91FF)
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFFFAFAFA),     // Official One UI White text (#FAFAFA)
    textMuted = Color(0xFF8E8E93),    // Refined secondary muted text (WCAG 4.5:1 compliant)
    success = Color(0xFF10B981),      // Official One UI Green
    error = Color(0xFFEF4444),        // Official One UI Red
    warning = Color(0xFFF59E0B),
    isDark = true,
    primaryDark = Color(0xFF3E91FF),
    colorControlActive = Color(0xFF3E91FF),
    onlineStatus = Color(0xFF30D158),
    offlineStatus = Color(0xFF8E8E93),
    statusRingBorder = Color(0xFF141416),
    cardSurface = Color(0xFF16181F),
    cardSurfaceElevated = Color(0xFF1E222D),
    cardBorder = Color(0x1FFFFFFF),
    cardBorderSubtle = Color(0x12FFFFFF),
    cardCornerRadius = 22.dp,
    buttonCornerRadius = 14.dp,
    chipCornerRadius = 10.dp,
    pillCornerRadius = 9999.dp,
    sheetCornerRadius = 26.dp,
    inputBg = Color(0xFF1C1F26),
    inputBorder = Color(0x28FFFFFF),
    avatarBorder = Color(0x30FFFFFF)
)

// Radii (--radius-sm/md/lg/pill) as dp values, used across components
object OscellionRadius {
    const val sm = 8
    const val md = 16
    const val lg = 24
    const val pill = 9999
}
