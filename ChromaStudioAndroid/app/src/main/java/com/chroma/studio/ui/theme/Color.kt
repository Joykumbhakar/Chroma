package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Direct port of the CSS custom properties from index.html (:root and body.dark).
 * Every hex value below matches the source file 1:1 so the app reads identically
 * to Chroma Studio in light and dark mode.
 */
data class ChromaPalette(
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
val LightPalette = ChromaPalette(
    bg = Color(0xFFF8FAFC),
    gridLine = Color(0x0F000000), // rgba(0,0,0,0.06) approx -> using 0x0F alpha (~6%)
    glassBg = Color(0x8CFFFFFF),      // rgba(255,255,255,0.55)
    glassBgHover = Color(0xCCFFFFFF), // rgba(255,255,255,0.8)
    glassBorder = Color(0x99FFFFFF),  // rgba(255,255,255,0.6)
    primary = Color(0xFF4F46E5),
    primaryHover = Color(0xFF4338CA),
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFF111827),
    textMuted = Color(0xFF4B5563),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    isDark = false
)

// ---- body.dark ----
val DarkPalette = ChromaPalette(
    bg = Color(0xFF0D1117),
    gridLine = Color(0x0DFFFFFF), // rgba(255,255,255,0.05)
    glassBg = Color(0x8C191C29),      // rgba(25,28,41,0.55)
    glassBgHover = Color(0xCC282C3E),  // rgba(40,44,62,0.8)
    glassBorder = Color(0x14FFFFFF),  // rgba(255,255,255,0.08)
    primary = Color(0xFF818CF8),
    primaryHover = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    textMain = Color(0xFFF9FAFB),
    textMuted = Color(0xFF9CA3AF),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    isDark = true
)

// Radii (--radius-sm/md/lg/pill) as dp values, used across components
object ChromaRadius {
    const val sm = 8
    const val md = 16
    const val lg = 24
    const val pill = 9999
}
