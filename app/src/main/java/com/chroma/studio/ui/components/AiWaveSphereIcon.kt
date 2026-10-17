package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom AI Wave Sphere Graphic matching the user specification:
 * - Purple-to-violet linear gradient circular badge (#8A2BE2 -> #6E11E9 -> #4A00E0)
 * - Crisp white outer ring (radius 14 on 48-unit viewport, stroke width 2.2)
 * - Three flowing sinusoidal wave strokes clipped inside radius 13 sphere (stroke width 2.2, round cap/join)
 */
val AiWaveSphereVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "AiWaveSphere",
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 48f,
        viewportHeight = 48f
    ).apply {
        // 1. Purple Gradient Circular Background Badge
        addPath(
            pathData = PathParser().parsePathString("M 24,24 m -22,0 a 22,22 0 1,0 44,0 a 22,22 0 1,0 -44,0").toNodes(),
            fill = Brush.linearGradient(
                colors = listOf(Color(0xFF8A2BE2), Color(0xFF6E11E9), Color(0xFF4A00E0)),
                start = Offset(8f, 6f),
                end = Offset(40f, 42f)
            )
        )
        // 2. Outer White Ring
        addPath(
            pathData = PathParser().parsePathString("M 24,24 m -14,0 a 14,14 0 1,0 28,0 a 14,14 0 1,0 -28,0").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.2f
        )
        // 3. Three Flowing Wave Strokes (Clipped inside sphere)
        group(
            clipPathData = PathParser().parsePathString("M 24,24 m -13,0 a 13,13 0 1,0 26,0 a 13,13 0 1,0 -26,0").toNodes()
        ) {
            addPath(
                pathData = PathParser().parsePathString("M 8,16 C 16,24 24,8 40,16").toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = PathParser().parsePathString("M 8,24 C 16,32 24,16 40,24").toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = PathParser().parsePathString("M 8,32 C 16,40 24,24 40,32").toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

/**
 * Composable that renders the AI Wave Sphere with pixel-perfect resolution independence.
 */
@Composable
fun AiWaveSphereIcon(
    modifier: Modifier = Modifier.size(24.dp),
    contentDescription: String? = "AI"
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 48f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // 1. Purple Gradient Circular Background Badge
        val grad = Brush.linearGradient(
            colors = listOf(Color(0xFF8A2BE2), Color(0xFF6E11E9), Color(0xFF4A00E0)),
            start = Offset(cx - 16f * scale, cy - 18f * scale),
            end = Offset(cx + 16f * scale, cy + 18f * scale)
        )
        drawCircle(
            brush = grad,
            radius = 22f * scale,
            center = Offset(cx, cy)
        )

        // 2. Outer White Ring
        drawCircle(
            color = Color.White,
            radius = 14f * scale,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2f * scale)
        )

        // 3. Three Flowing Wave Strokes (Clipped inside sphere)
        clipPath(
            Path().apply {
                addOval(Rect(center = Offset(cx, cy), radius = 13f * scale))
            }
        ) {
            val stroke = Stroke(width = 2.2f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            // Wave 1 (Upper Hemisphere)
            drawPath(
                Path().apply {
                    moveTo(cx - 16f * scale, cy - 8f * scale)
                    cubicTo(cx - 8f * scale, cy, cx, cy - 16f * scale, cx + 16f * scale, cy - 8f * scale)
                },
                color = Color.White,
                style = stroke
            )
            // Wave 2 (Center Equator Wave)
            drawPath(
                Path().apply {
                    moveTo(cx - 16f * scale, cy)
                    cubicTo(cx - 8f * scale, cy + 8f * scale, cx, cy - 8f * scale, cx + 16f * scale, cy)
                },
                color = Color.White,
                style = stroke
            )
            // Wave 3 (Lower Hemisphere)
            drawPath(
                Path().apply {
                    moveTo(cx - 16f * scale, cy + 8f * scale)
                    cubicTo(cx - 8f * scale, cy + 16f * scale, cx, cy, cx + 16f * scale, cy + 8f * scale)
                },
                color = Color.White,
                style = stroke
            )
        }
    }
}
