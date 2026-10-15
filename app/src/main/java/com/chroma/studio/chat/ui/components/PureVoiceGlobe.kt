package com.chroma.studio.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Exact replica of the 3D Liquid AI Globe from VoiceGlobe.html.
 *
 * Features:
 * - Concentric expanding radar waves (wave1, wave2)
 * - Breathing scale animation (circle1)
 * - Radial gradient core container matching HTML spec
 * - 4 rotating, morphing liquid plasma blobs (c1, c2, c3, c4)
 * - Glass lens specular overlay with realistic top reflection
 * - 5-bar animated audio equalizer in orb center
 */
@Composable
fun PureVoiceGlobe(
    modifier: Modifier = Modifier,
    globeSize: Dp = 150.dp
) {
    val colors = LocalChatColors.current
    val isDark = !colors.isLight

    val transition = rememberInfiniteTransition(label = "VoiceGlobeMain")

    // Breathing scale animation (circle1: 0.97 -> 1.02 over 5s)
    val breathingScale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Radar wave 1 (2.1s duration)
    val wave1Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    // Radar wave 2 (2.1s duration, delayed by 700ms)
    val wave2Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, delayMillis = 700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    // Plasma blob animations matching CSS keyframes ai1..ai4
    val blob1Angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1"
    )

    val blob2Angle by transition.animateFloat(
        initialValue = 90f,
        targetValue = 450f,
        animationSpec = infiniteRepeatable(
            animation = tween(5800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2"
    )

    val blob3Angle by transition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob3"
    )

    val blob4Angle by transition.animateFloat(
        initialValue = 270f,
        targetValue = 630f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob4"
    )

    // Core gradient definition matching CSS var(--orb-core)
    val coreColors = if (isDark) {
        listOf(
            Color(0xFF38BDF8),
            Color(0xFF0284C7),
            Color(0xFF1D4ED8),
            Color(0xFF091536)
        )
    } else {
        listOf(
            Color(0xFFE0F2FE),
            Color(0xFF60A5FA),
            Color(0xFF0078FE),
            Color(0xFF1E3A8A)
        )
    }

    val orbSize = globeSize * 0.78f

    Box(
        modifier = modifier
            .size(globeSize)
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
            },
        contentAlignment = Alignment.Center
    ) {
        // ── 1. Outer Radar Wave 1 ──────────────────────────────
        val wave1Scale = 0.98f + (wave1Progress * 0.58f)
        val wave1Alpha = (1f - wave1Progress) * 0.85f
        Box(
            modifier = Modifier
                .size(orbSize)
                .graphicsLayer {
                    scaleX = wave1Scale
                    scaleY = wave1Scale
                    alpha = wave1Alpha
                }
                .border(1.5.dp, Color(0xFF0078FE).copy(alpha = 0.38f), CircleShape)
        )

        // ── 2. Outer Radar Wave 2 ──────────────────────────────
        val wave2Scale = 0.98f + (wave2Progress * 0.58f)
        val wave2Alpha = (1f - wave2Progress) * 0.85f
        Box(
            modifier = Modifier
                .size(orbSize)
                .graphicsLayer {
                    scaleX = wave2Scale
                    scaleY = wave2Scale
                    alpha = wave2Alpha
                }
                .border(1.5.dp, Color(0xFF0078FE).copy(alpha = 0.38f), CircleShape)
        )

        // ── 3. Orb Core Inner Container ────────────────────────
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Background Plasma Canvas with 4 rotating blobs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height

                val centerOffset = Offset(canvasWidth * 0.30f, canvasHeight * 0.25f)
                val baseRadius = canvasWidth * 0.85f

                // Base radial background gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = coreColors,
                        center = centerOffset,
                        radius = baseRadius
                    )
                )

                val orbRadius = canvasWidth / 2f
                val cx = canvasWidth / 2f
                val cy = canvasHeight / 2f

                // Blob 4 (c4: #1d4ed8 -> #1e3a8a)
                val rad4 = Math.toRadians(blob4Angle.toDouble())
                val dist4 = orbRadius * 0.40f
                val bx4 = cx + (cos(rad4) * dist4).toFloat()
                val by4 = cy + (sin(rad4) * dist4).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A).copy(alpha = 0.1f)),
                        center = Offset(bx4, by4),
                        radius = orbRadius * 0.80f
                    ),
                    radius = orbRadius * 0.80f,
                    center = Offset(bx4, by4)
                )

                // Blob 1 (c1: #60a5fa -> #0078FE)
                val rad1 = Math.toRadians(blob1Angle.toDouble())
                val dist1 = orbRadius * 0.45f
                val bx1 = cx + (cos(rad1) * dist1).toFloat()
                val by1 = cy + (sin(rad1) * dist1).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF60A5FA), Color(0xFF0078FE).copy(alpha = 0.2f)),
                        center = Offset(bx1, by1),
                        radius = orbRadius * 0.75f
                    ),
                    radius = orbRadius * 0.75f,
                    center = Offset(bx1, by1)
                )

                // Blob 3 (c3: #38bdf8 -> transparent)
                val rad3 = Math.toRadians(blob3Angle.toDouble())
                val dist3 = orbRadius * 0.44f
                val bx3 = cx + (cos(rad3) * dist3).toFloat()
                val by3 = cy + (sin(rad3) * dist3).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(bx3, by3),
                        radius = orbRadius * 0.65f
                    ),
                    radius = orbRadius * 0.65f,
                    center = Offset(bx3, by3)
                )

                // Blob 2 (c2: #ffffff -> #bae6fd)
                val rad2 = Math.toRadians(blob2Angle.toDouble())
                val dist2 = orbRadius * 0.42f
                val bx2 = cx + (cos(rad2) * dist2).toFloat()
                val by2 = cy + (sin(rad2) * dist2).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFBAE6FD).copy(alpha = 0.2f)),
                        center = Offset(bx2, by2),
                        radius = orbRadius * 0.50f
                    ),
                    radius = orbRadius * 0.50f,
                    center = Offset(bx2, by2)
                )
            }

            // ── 4. Glass Lens Specular Overlay ─────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height

                val specularCenter = Offset(canvasWidth * 0.32f, canvasHeight * 0.24f)
                val specularRadius = canvasWidth * 0.55f

                // Top-left curved specular shine
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = specularCenter,
                        radius = specularRadius
                    )
                )

                // Bottom subtle cyan bounce
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF38BDF8).copy(alpha = 0.35f)
                        ),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.85f),
                        radius = canvasWidth * 0.45f
                    )
                )
            }

            // Subtle glass rim border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color(0xFF38BDF8).copy(alpha = 0.35f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // ── 5. Center 5-Bar Equalizer ──────────────────────
            OrbEqualizer()
        }
    }
}

/**
 * 5-Bar Waveform Equalizer matching .orb-eq from VoiceGlobe.html.
 */
@Composable
private fun OrbEqualizer() {
    val transition = rememberInfiniteTransition(label = "OrbEqTransition")

    val pulse1 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )

    val pulse2 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )

    val pulse3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    val pulse4 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq4"
    )

    val pulse5 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, delayMillis = 250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq5"
    )

    val heights = listOf(9.dp, 18.dp, 24.dp, 15.dp, 10.dp)
    val pulses = listOf(pulse1, pulse2, pulse3, pulse4, pulse5)

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heights[index])
                    .graphicsLayer {
                        scaleY = pulses[index]
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
            )
        }
    }
}

