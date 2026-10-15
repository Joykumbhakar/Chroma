package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.chroma.studio.R
import com.chroma.studio.ui.theme.LocalOscellionColors

@Composable
fun OscellionFluidBackground(modifier: Modifier = Modifier) {
    val colors = LocalOscellionColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_bg")

    // Breathe animation
    val breatheY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheY"
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    // Drift animation
    val driftX by infiniteTransition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = -0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )
    val driftScale by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg) // Base background color
    ) {
        // Grain overlay
        val imageBitmap = ImageBitmap.imageResource(R.drawable.noise_grain)
        val brush = ShaderBrush(ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated))
        Box(modifier = Modifier.fillMaxSize().background(brush, alpha = 0.5f))

        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Breathe
            scale(scaleX = breatheScale, scaleY = breatheScale, pivot = Offset(w / 2, h)) {
                translate(left = 0f, top = h * breatheY) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(59, 130, 246, (0.2f * 255).toInt()),
                                Color(162, 210, 255, (0.2f * 255).toInt()),
                                Color(255, 255, 255, 0)
                            ),
                            center = Offset(w / 2, h),
                            radius = maxOf(w, h)
                        ),
                        topLeft = Offset(-w / 2, h - h * 1.2f),
                        size = Size(w * 2, h * 1.2f)
                    )
                }
            }

            // Drift
            scale(scaleX = driftScale, scaleY = driftScale, pivot = Offset(w / 2, h)) {
                translate(left = w * driftX, top = h * driftY) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(59, 130, 246, (0.1f * 255).toInt()),
                                Color(180, 220, 255, (0.1f * 255).toInt()),
                                Color(255, 255, 255, 0)
                            ),
                            center = Offset(w / 2, h),
                            radius = maxOf(w, h) * 0.8f
                        ),
                        topLeft = Offset(w * 0.25f, h - h * 1.2f),
                        size = Size(w * 1.5f, h * 1.2f)
                    )
                }
            }
        }
    }
}
