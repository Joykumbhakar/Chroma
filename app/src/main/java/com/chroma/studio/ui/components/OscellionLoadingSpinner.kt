package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Oscellion Brand Animated In-App Loader
 * Direct implementation of Section 10 from the official Brand Identity Dashboard:
 * - Full official vector path track at 0.08 - 0.12 opacity.
 * - Rotating animated outer trace with smooth cubic-bezier easing.
 */
@Composable
fun OscellionLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color? = null,
    innerColor: Color? = null,
    strokeWidth: Dp? = null
) {
    val isDark = isSystemInDarkTheme()
    val spinnerColor = color ?: if (isDark) Color.White else Color(0xFF1C1C1E)

    val infiniteTransition = rememberInfiniteTransition(label = "OscellionSpinner")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "osc_spin"
    )

    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Box(
        modifier = modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.minDimension / 1024f
            val calculatedStrokeWidth = (strokeWidth?.toPx()?.times(1024f / size.minDimension)) ?: 48f

            val offsetX = (size.width - 1024f * scale) / 2f
            val offsetY = (size.height - 1024f * scale) / 2f

            // 1. Faint background track
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)
                canvas.drawPath(
                    parsedPath,
                    Paint().apply {
                        this.color = spinnerColor.copy(alpha = 0.10f)
                        this.style = PaintingStyle.Stroke
                        this.strokeWidth = calculatedStrokeWidth
                        this.strokeCap = StrokeCap.Round
                        this.strokeJoin = StrokeJoin.Round
                        this.isAntiAlias = true
                    }
                )
                canvas.restore()
            }

            // 2. Animated rotating trace
            rotate(degrees = rotation, pivot = center) {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(offsetX, offsetY)
                    canvas.scale(scale, scale)
                    canvas.drawPath(
                        parsedPath,
                        Paint().apply {
                            this.color = spinnerColor
                            this.style = PaintingStyle.Stroke
                            this.strokeWidth = calculatedStrokeWidth
                            this.strokeCap = StrokeCap.Round
                            this.strokeJoin = StrokeJoin.Round
                            this.isAntiAlias = true
                        }
                    )
                    canvas.restore()
                }
            }
        }
    }
}
