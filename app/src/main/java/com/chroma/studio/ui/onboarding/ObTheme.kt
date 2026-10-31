package com.chroma.studio.ui.onboarding

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Deep-tone palette used by every onboarding screen matching reference. */
object ObColors {
    val Abyss = Color(0xFF060714)
    val Ink = Color(0xFF090C22)
    val Night = Color(0xFF0D1230)
    val Deep = Color(0xFF131942)
    val Cobalt = Color(0xFF182258)
    val Royal = Color(0xFF1B2664)

    val Lime = Color(0xFFC8F535)
    val OnLime = Color(0xFF080B1E)
    val Cyan = Color(0xFF7DF9FF)
    val Amber = Color(0xFFFBBF24)
    val Danger = Color(0xFFFF5271)
    val Sky = Color(0xFF8AB4FF)

    val TextMuted = Color(0xFFBAC7E8)
    val TextFaint = Color(0xFF8F9FCA)
    val Placeholder = Color(0x80BAC7E8)

    val GlassTop = Color(0x8C161C3E)
    val GlassBottom = Color(0xB30C102A)
    val GlassBorder = Color(0x24FFFFFF)
    val Field = Color(0x99111636)
    val FieldBorder = Color(0x2EFFFFFF)
    val ToggleOn = Color(0xFFC8F535)
    val ToggleOff = Color(0x38FFFFFF)

    /** Screens 2–5: deep midnight navy vertical gradient. */
    val FlowBackground = Brush.verticalGradient(
        0.00f to Color(0xFF070817),
        0.22f to Color(0xFF0C102C),
        0.50f to Color(0xFF121840),
        0.80f to Color(0xFF182154),
        1.00f to Color(0xFF151D4C)
    )
}

object ObType {
    private val Display = FontFamily.SansSerif

    val Hero = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.9).sp,
        color = Color.White
    )
    val Title = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
        color = Color.White
    )
    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ObColors.TextMuted
    )
    val SectionHeader = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp,
        color = Color(0x99FFFFFF)
    )
    val Label = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Color.White
    )
    val Caption = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = ObColors.TextFaint
    )
}
