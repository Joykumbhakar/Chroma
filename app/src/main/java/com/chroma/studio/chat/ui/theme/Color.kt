package com.chroma.studio.chat.ui.theme

import androidx.compose.ui.graphics.Color

// === PRIMARY ACCENT PALETTE (Samsung One UI Guide Page 62) ===
val AccentBlue = Color(0xFF0381FE)          // Official One UI Primary
val AccentBlueDark = Color(0xFF3E91FF)      // Official One UI Active Control / Dark Mode Accent
val AccentBluePressed = Color(0xFF0072DE)   // Official One UI Primary Dark (Light mode)
val AccentBlueSubtle = Color(0x1A0381FE)
val AccentBlueMedium = Color(0x330381FE)

val AccentGreen = Color(0xFF00C853)         // Samsung One UI Green (Page 63)
val AccentGreenDark = Color(0xFF10B981)
val AccentRed = Color(0xFFF53B57)           // Samsung One UI Red (Page 63)
val AccentRedDark = Color(0xFFEF4444)
val AccentOrange = Color(0xFFFF9500)
val AccentOrangeDark = Color(0xFFFF9F0A)
val AccentPurple = Color(0xFFAF52DE)
val AccentPurpleDark = Color(0xFFBF5AF2)
val AccentPink = Color(0xFFFF2D55)
val AccentTeal = Color(0xFF5AC8FA)
val AccentYellow = Color(0xFFFFCC00)

// === SEMANTIC COLORS ===
val Destructive = Color(0xFFF53B57)
val DestructiveDark = Color(0xFFEF4444)
val Success = Color(0xFF00C853)
val Warning = Color(0xFFFF9500)

// === LIGHT THEME SURFACE TOKENS (Samsung One UI Page 62) ===
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceSecondaryLight = Color(0xFFF4F7FB)
val SurfaceTertiaryLight = Color(0xFFE5E5EA)
val SurfaceElevatedLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFFAFAFA)     // Samsung One UI Light background canvas (#FAFAFA)
val ChatBackgroundLight = Color(0xFFF2F4F7)
val NavBarLight = Color(0xF0F9F9FC)
val NavBarBorderLight = Color(0x20000000)
val LabelPrimaryLight = Color(0xFF000000)   // Samsung One UI Black (#000000)
val LabelSecondaryLight = Color(0xFF71717A) // WCAG 4.5:1 compliant secondary text
val LabelTertiaryLight = Color(0xFF9D9DA3)
val LabelQuaternaryLight = Color(0xFFC4C4C9)
val SeparatorLight = Color(0x293C3C43)
val SeparatorOpaqueLight = Color(0xFFC6C6C8)
val BorderSubtleLight = Color(0x1A000000)
val BorderMediumLight = Color(0x33000000)
val BubbleIncomingLight = Color(0xFFE8E8ED)
val BubbleOutgoingLight = Color(0xFF0381FE) // Samsung One UI Primary Blue

// === DARK THEME SURFACE TOKENS (Samsung One UI Page 62) ===
val SurfaceDark = Color(0xFF17171A)        // Samsung One UI Elevated Container (#17171A)
val SurfaceSecondaryDark = Color(0xFF222226)
val SurfaceTertiaryDark = Color(0xFF2C2C2E)
val SurfaceElevatedDark = Color(0xFF17171A)
val BackgroundDark = Color(0xFF080808)     // Samsung One UI Dark background canvas (#080808)
val ChatBackgroundDark = Color(0xFF080808)
val NavBarDark = Color(0xE8121212)
val NavBarBorderDark = Color(0x33FFFFFF)
val LabelPrimaryDark = Color(0xFFFAFAFA)   // Samsung One UI White (#FAFAFA)
val LabelSecondaryDark = Color(0xFF8E8E93) // WCAG 4.5:1 compliant secondary text
val LabelTertiaryDark = Color(0xFF636366)
val LabelQuaternaryDark = Color(0xFF48484A)
val SeparatorDark = Color(0x33545458)
val SeparatorOpaqueDark = Color(0xFF38383A)
val BorderSubtleDark = Color(0x1AFFFFFF)
val BorderMediumDark = Color(0x33FFFFFF)
val BubbleIncomingDark = Color(0xFF222226)
val BubbleOutgoingDark = Color(0xFF0381FE) // Samsung One UI Primary Blue

// === GRADIENT HELPERS ===
val StoryGradientColors = listOf(
    Color(0xFF0381FE),
    Color(0xFF3E91FF),
    Color(0xFF5856D6),
    Color(0xFF0381FE)
)

// === LEGACY ALIASES (backward compatibility) ===
val IosBlue = AccentBlue
val IosBlueDark = AccentBlueDark
val IosBlueHover = AccentBluePressed
val IosBlueSubtleLight = AccentBlueSubtle
val IosBlueSubtleDark = AccentBlueSubtle
val IosGreen = AccentGreen
val IosGreenDark = AccentGreenDark
val IosIndigo = Color(0xFF5856D6)
val IosIndigoDark = Color(0xFF5E5CE6)
val IosOrange = AccentOrange
val IosOrangeDark = AccentOrangeDark
val IosPink = AccentPink
val IosPinkDark = Color(0xFFFF375F)
val IosPurple = AccentPurple
val IosPurpleDark = AccentPurpleDark
val IosRed = AccentRed
val IosRedDark = AccentRedDark
val IosTeal = AccentTeal
val IosTealDark = Color(0xFF64D2FF)
val IosYellow = AccentYellow
val IosYellowDark = Color(0xFFFFD60A)
val SystemGray1Light = Color(0xFF8E8E93)
val SystemGray2Light = Color(0xFFAEAEB2)
val SystemGray3Light = Color(0xFFC7C7CC)
val SystemGray4Light = Color(0xFFD1D1D6)
val SystemGray5Light = Color(0xFFE5E5EA)
val SystemGray6Light = Color(0xFFF2F2F7)
val SystemGray1Dark = Color(0xFF8E8E93)
val SystemGray2Dark = Color(0xFF636366)
val SystemGray3Dark = Color(0xFF48484A)
val SystemGray4Dark = Color(0xFF3A3A3C)
val SystemGray5Dark = Color(0xFF2C2C2E)
val SystemGray6Dark = Color(0xFF1C1C1E)
val LightAppBg = BackgroundLight
val LightChatBg = ChatBackgroundLight
val LightSidebarBg = SurfaceLight
val LightCardBg = SurfaceLight
val LightHeaderBg = NavBarLight
val LightBubbleGray = BubbleIncomingLight
val LightTextDark = LabelPrimaryLight
val LightTextMuted = LabelSecondaryLight
val LightInputBg = SurfaceLight
val LightBorderSubtle = BorderSubtleLight
val LightSeparator = SeparatorLight
val DarkAppBg = BackgroundDark
val DarkChatBg = ChatBackgroundDark
val DarkSidebarBg = SurfaceDark
val DarkCardBg = SurfaceDark
val DarkHeaderBg = NavBarDark
val DarkBubbleGray = BubbleIncomingDark
val DarkTextDark = LabelPrimaryDark
val DarkTextMuted = LabelSecondaryDark
val DarkInputBg = SurfaceDark
val DarkBorderSubtle = BorderSubtleDark
val DarkSeparator = SeparatorDark
val StoryGradient = StoryGradientColors
