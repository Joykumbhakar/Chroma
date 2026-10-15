package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors

/**
 * Oscellion Core Signal Vector Logo
 * Exact mathematical recreation of the 3-line signal from Brand Identity & App Icon:
 * - Outer shell receives (White/Dark based on theme),
 * - Inner waves broadcast (Oscellion Blue #3B82F6 / #007AFF).
 */
@Composable
fun OscellionLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    outerColor: Color? = null,
    innerColor: Color = Color(0xFF3B82F6),
    strokeWidth: Dp? = null
) {
    val isDark = isSystemInDarkTheme()
    val resolvedOuterColor = outerColor ?: if (isDark) Color.White else Color(0xFF111827)

    Canvas(modifier = modifier.size(size)) {
        val calculatedStrokeWidth = (strokeWidth?.toPx()) ?: (this.size.minDimension * 0.085f).coerceAtLeast(1.5f)
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val scale = this.size.minDimension / 100f

        // ── 1. Outer Arc (R=40, curving left) ──────────────────────────────
        // HTML SVG: M 50 10 A 40 40 0 0 0 50 90
        drawArc(
            color = resolvedOuterColor,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - 40f * scale, center.y - 40f * scale),
            size = Size(80f * scale, 80f * scale),
            style = Stroke(width = calculatedStrokeWidth, cap = StrokeCap.Round)
        )

        // ── 2. Middle Arc (R=25, curving right) ────────────────────────────
        // HTML SVG: M 50 25 A 25 25 0 0 1 50 75
        drawArc(
            color = innerColor,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - 25f * scale, center.y - 25f * scale),
            size = Size(50f * scale, 50f * scale),
            style = Stroke(width = calculatedStrokeWidth, cap = StrokeCap.Round)
        )

        // ── 3. Inner Arc (R=10, curving right) ────────────────────────────
        // HTML SVG: M 50 40 A 10 10 0 0 1 50 60
        drawArc(
            color = innerColor,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - 10f * scale, center.y - 10f * scale),
            size = Size(20f * scale, 20f * scale),
            style = Stroke(width = calculatedStrokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Official Horizontal Lockup: Logo + "Oscellion." with vibrant blue dot
 */
@Composable
fun OscellionBrandLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    fontSize: Dp = 20.dp
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OscellionLogo(
            size = iconSize,
            outerColor = colors.textMain,
            innerColor = Color(0xFF007AFF)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oscellion",
                fontSize = fontSize.value.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = ".",
                fontSize = fontSize.value.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color(0xFF007AFF)
            )
        }
    }
}

/**
 * Official Stacked Lockup: Large Logo over "Oscellion."
 */
@Composable
fun OscellionStackedLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OscellionLogo(
            size = iconSize,
            outerColor = colors.textMain,
            innerColor = Color(0xFF007AFF)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oscellion",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = ".",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color(0xFF007AFF)
            )
        }
    }
}
