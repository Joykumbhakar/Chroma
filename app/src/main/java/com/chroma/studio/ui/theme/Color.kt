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
    bg = Color(0xFFF4F7FB),           // --ds-bg-canvas
    gridLine = Color(0x0F000000),     // subtle grid line
    glassBg = Color(0xFFFFFFFF),      // --ds-surface-card (Solid white pristine panel)
    glassBgHover = Color(0xFFF4F7FB), // hover panel
    glassBorder = Color(0xFFDFE8F3),  // --ds-border-flat
    primary = Color(0xFF0A84FF),      // --ds-accent-blue (Cupertino System Blue)
    primaryHover = Color(0xFF0051D5), // deep blue
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFF0F172A),     // --ds-text-primary (Slate 900)
    textMuted = Color(0xFF64748B),    // --ds-text-secondary (Slate 500)
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    isDark = false
)

// ---- body.dark ----
val DarkPalette = OscellionPalette(
    bg = Color(0xFF030303),           // #030303
    gridLine = Color(0x05FFFFFF),     // rgba(255,255,255,0.02)
    glassBg = Color(0xFF0A0A0A),      // #0A0A0A (Panel background)
    glassBgHover = Color(0xFF141414), // slightly lighter panel
    glassBorder = Color(0x14FFFFFF),  // rgba(255, 255, 255, 0.08)
    primary = Color(0xFF3B82F6),      // #3B82F6 (Brand blue)
    primaryHover = Color(0xFF2563EB), // Tailwind blue-600
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFFF9FAFB),
    textMuted = Color(0xFF9CA3AF),
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
