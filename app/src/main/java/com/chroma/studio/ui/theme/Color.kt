package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Direct port of the CSS custom properties from index.html (:root and body.dark).
 * Every hex value below matches the source file 1:1 so the app reads identically
 * to Oscellion in light and dark mode.
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

    val isDark: Boolean
)

// ---- :root (light) ----
val LightPalette = OscellionPalette(
    bg = Color(0xFFF2F4F7),           // Samsung One UI Light background canvas
    gridLine = Color(0x0A000000),     // subtle grid line
    glassBg = Color(0xFFFFFFFF),      // Solid white pristine One UI card
    glassBgHover = Color(0xFFF4F7FB), // hover panel
    glassBorder = Color(0x0D000000),  // subtle card border
    primary = Color(0xFF0A84FF),      // Unified brand blue
    primaryHover = Color(0xFF0066E0), // deep blue
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFF0A0A0C),     // Slate 950 deep legible text
    textMuted = Color(0xFF71717A),    // Slate 500 secondary text
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    isDark = false
)

// ---- body.dark ----
val DarkPalette = OscellionPalette(
    bg = Color(0xFF000000),           // Samsung One UI pure OLED black canvas
    gridLine = Color(0x08FFFFFF),     // subtle dark grid
    glassBg = Color(0xFF17171A),      // One UI elevated dark container card
    glassBgHover = Color(0xFF222226), // slightly lighter elevated surface
    glassBorder = Color(0x18FFFFFF),  // rgba(255, 255, 255, 0.09)
    primary = Color(0xFF0A84FF),      // Unified brand blue
    primaryHover = Color(0xFF0066E0), // deep blue
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFFF2F2F7),     // Crisp One UI primary white text
    textMuted = Color(0xFF8E8E93),    // Refined secondary muted text
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    isDark = true
)

// Radii (--radius-sm/md/lg/pill) as dp values, used across components
object OscellionRadius {
    const val sm = 8
    const val md = 16
    const val lg = 24
    const val pill = 9999
}
