package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Direct port and implementation of the Samsung One UI Design Guidelines color palette (Pages 62-63, 86).
 * Incorporates professional, accessible, and high-contrast color tokens for Light and Dark modes.
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
    val colorControlActive: Color = Color(0xFF3E91FF)
)

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
    colorControlActive = Color(0xFF3E91FF)
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
    colorControlActive = Color(0xFF3E91FF)
)

// Radii (--radius-sm/md/lg/pill) as dp values, used across components
object OscellionRadius {
    const val sm = 8
    const val md = 16
    const val lg = 24
    const val pill = 9999
}
