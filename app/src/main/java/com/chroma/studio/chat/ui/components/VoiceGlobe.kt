package com.chroma.studio.chat.ui.components

import android.webkit.JavascriptInterface
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.VoiceGlobeStyleType
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Palette configuration for 3D Optical Liquid Glass Voice Bubble.
 */
@Immutable
data class OpticalVoiceGlobePalette(
    val accentColor: Color,
    val orbCoreStops: List<Pair<Float, Color>>,
    val c1Inner: Color,
    val c1Outer: Color,
    val c2Inner: Color,
    val c2Outer: Color,
    val c3Inner: Color,
    val c3Outer: Color,
    val c4Inner: Color,
    val c4Outer: Color,
    val causticColor: Color,
    val waveColor: Color
)

fun getOpticalVoiceGlobePalette(globeStyle: VoiceGlobeStyleType, isDark: Boolean): OpticalVoiceGlobePalette {
    return when (globeStyle) {
        VoiceGlobeStyleType.ORIGINAL_BLUE -> {
            if (isDark) {
                OpticalVoiceGlobePalette(
                    accentColor = Color(0xFF0078FE),
                    orbCoreStops = listOf(
                        0.00f to Color(0xFF38BDF8),
                        0.35f to Color(0xFF0284C7),
                        0.72f to Color(0xFF1D4ED8),
                        1.00f to Color(0xFF091536)
                    ),
                    c1Inner = Color(0xFF60A5FA),
                    c1Outer = Color(0xFF0078FE),
                    c2Inner = Color.White,
                    c2Outer = Color(0xFFBAE6FD),
                    c3Inner = Color(0xFF38BDF8),
                    c3Outer = Color.Transparent,
                    c4Inner = Color(0xFF1D4ED8),
                    c4Outer = Color(0xFF1E3A8A),
                    causticColor = Color(0xFF38BDF8),
                    waveColor = Color(0xFF0078FE)
                )
            } else {
                OpticalVoiceGlobePalette(
                    accentColor = Color(0xFF0078FE),
                    orbCoreStops = listOf(
                        0.00f to Color(0xFFE0F2FE),
                        0.38f to Color(0xFF60A5FA),
                        0.74f to Color(0xFF0078FE),
                        1.00f to Color(0xFF1E3A8A)
                    ),
                    c1Inner = Color(0xFF60A5FA),
                    c1Outer = Color(0xFF0078FE),
                    c2Inner = Color.White,
                    c2Outer = Color(0xFFBAE6FD),
                    c3Inner = Color(0xFF38BDF8),
                    c3Outer = Color.Transparent,
                    c4Inner = Color(0xFF1D4ED8),
                    c4Outer = Color(0xFF1E3A8A),
                    causticColor = Color(0xFF38BDF8),
                    waveColor = Color(0xFF0078FE)
                )
            }
        }
        VoiceGlobeStyleType.CHROMATIC_3D -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFF8B5CF6),
            orbCoreStops = listOf(
                0.00f to Color(0xFFE9D5FF),
                0.38f to Color(0xFFC084FC),
                0.74f to Color(0xFF8B5CF6),
                1.00f to Color(0xFF3B0764)
            ),
            c1Inner = Color(0xFFC084FC),
            c1Outer = Color(0xFF8B5CF6),
            c2Inner = Color.White,
            c2Outer = Color(0xFFF3E8FF),
            c3Inner = Color(0xFFA855F7),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF7C3AED),
            c4Outer = Color(0xFF3B0764),
            causticColor = Color(0xFFA855F7),
            waveColor = Color(0xFF8B5CF6)
        )
        VoiceGlobeStyleType.AURORA_PULSE -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFFEC4899),
            orbCoreStops = listOf(
                0.00f to Color(0xFFFCE7F3),
                0.38f to Color(0xFFF472B6),
                0.74f to Color(0xFFEC4899),
                1.00f to Color(0xFF831843)
            ),
            c1Inner = Color(0xFFF472B6),
            c1Outer = Color(0xFFEC4899),
            c2Inner = Color.White,
            c2Outer = Color(0xFFFDF2F8),
            c3Inner = Color(0xFFF43F5E),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFFDB2777),
            c4Outer = Color(0xFF831843),
            causticColor = Color(0xFFF472B6),
            waveColor = Color(0xFFEC4899)
        )
        VoiceGlobeStyleType.CYBER_GRID -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFF10B981),
            orbCoreStops = listOf(
                0.00f to Color(0xFFD1FAE5),
                0.38f to Color(0xFF34D399),
                0.74f to Color(0xFF10B981),
                1.00f to Color(0xFF064E3B)
            ),
            c1Inner = Color(0xFF34D399),
            c1Outer = Color(0xFF10B981),
            c2Inner = Color.White,
            c2Outer = Color(0xFFECFDF5),
            c3Inner = Color(0xFF2DD4BF),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF059669),
            c4Outer = Color(0xFF064E3B),
            causticColor = Color(0xFF34D399),
            waveColor = Color(0xFF10B981)
        )
        VoiceGlobeStyleType.SOLAR_FLARE -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFFF97316),
            orbCoreStops = listOf(
                0.00f to Color(0xFFFEF3C7),
                0.38f to Color(0xFFFBBF24),
                0.74f to Color(0xFFF97316),
                1.00f to Color(0xFF7C2D12)
            ),
            c1Inner = Color(0xFFFBBF24),
            c1Outer = Color(0xFFF97316),
            c2Inner = Color.White,
            c2Outer = Color(0xFFFFFBEB),
            c3Inner = Color(0xFFFB923C),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFFEA580C),
            c4Outer = Color(0xFF7C2D12),
            causticColor = Color(0xFFFBBF24),
            waveColor = Color(0xFFF97316)
        )
        VoiceGlobeStyleType.DARK_MATTER -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFF6366F1),
            orbCoreStops = listOf(
                0.00f to Color(0xFFC7D2FE),
                0.38f to Color(0xFF818CF8),
                0.74f to Color(0xFF4F46E5),
                1.00f to Color(0xFF0F172A)
            ),
            c1Inner = Color(0xFF818CF8),
            c1Outer = Color(0xFF4F46E5),
            c2Inner = Color.White,
            c2Outer = Color(0xFFEEF2FF),
            c3Inner = Color(0xFF6366F1),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF4338CA),
            c4Outer = Color(0xFF0F172A),
            causticColor = Color(0xFF818CF8),
            waveColor = Color(0xFF6366F1)
        )
        VoiceGlobeStyleType.ROSE_QUARTZ -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFFF43F5E),
            orbCoreStops = listOf(
                0.00f to Color(0xFFFFE4E6),
                0.38f to Color(0xFFFDA4AF),
                0.74f to Color(0xFFF43F5E),
                1.00f to Color(0xFF881337)
            ),
            c1Inner = Color(0xFFFDA4AF),
            c1Outer = Color(0xFFF43F5E),
            c2Inner = Color.White,
            c2Outer = Color(0xFFFFF1F2),
            c3Inner = Color(0xFFFB7185),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFFE11D48),
            c4Outer = Color(0xFF881337),
            causticColor = Color(0xFFFDA4AF),
            waveColor = Color(0xFFF43F5E)
        )
        VoiceGlobeStyleType.ELECTRIC_CYAN -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFF06B6D4),
            orbCoreStops = listOf(
                0.00f to Color(0xFFCFFAFE),
                0.38f to Color(0xFF22D3EE),
                0.74f to Color(0xFF06B6D4),
                1.00f to Color(0xFF164E63)
            ),
            c1Inner = Color(0xFF22D3EE),
            c1Outer = Color(0xFF06B6D4),
            c2Inner = Color.White,
            c2Outer = Color(0xFFECFEFF),
            c3Inner = Color(0xFF38BDF8),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF0891B2),
            c4Outer = Color(0xFF164E63),
            causticColor = Color(0xFF22D3EE),
            waveColor = Color(0xFF06B6D4)
        )
    }
}

/**
 * 100% Native Jetpack Compose 3D Liquid Glass Voice Bubble Card.
 * Replaces the old WebView implementation to guarantee zero lag, zero crashes,
 * and buttery smooth 60fps animations across all Android devices.
 */
@Composable
fun VoiceRecorderCard(
    recordingDurationSec: Int,
    statusText: String = "Listening...",
    globeStyle: VoiceGlobeStyleType = VoiceGlobeStyleType.ORIGINAL_BLUE,
    onCancel: () -> Unit,
    onTranscribe: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val isDark = !colors.isLight
    val palette = remember(globeStyle, isDark) { getOpticalVoiceGlobePalette(globeStyle, isDark) }

    Surface(
        modifier = modifier
            .size(width = 304.dp, height = 312.dp),
        shape = RoundedCornerShape(40.dp),
        color = if (isDark) Color(0xF51C1C1E) else Color(0xF5FFFFFF),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F0F172A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Center 3D Liquid Glass Orb & Expanding Sonar Waves
            LiquidGlassVoiceOrb(
                palette = palette,
                modifier = Modifier
                    .size(240.dp)
                    .offset(y = (-4).dp)
            )

            // Foreground Header & Action Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Status Badge (Blinking Dot + Shimmering Text + Monospace Timer)
                VoiceHeaderBadge(
                    palette = palette,
                    statusText = statusText,
                    elapsedSeconds = recordingDurationSec,
                    isDark = isDark
                )

                // Bottom Action Buttons: Cancel, Text (Transcribe), Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    VoiceActionPillButton(
                        text = "Cancel",
                        icon = Lucide.X,
                        contentColor = colors.textMuted,
                        backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0x2494A3B8),
                        onClick = {
                            SoundManager.playReaction()
                            onCancel()
                        }
                    )

                    // Transcribe ("Text") Button
                    VoiceActionPillButton(
                        text = "Text",
                        icon = Lucide.Type,
                        contentColor = palette.accentColor,
                        backgroundColor = palette.accentColor.copy(alpha = 0.12f),
                        onClick = {
                            SoundManager.playReaction()
                            onTranscribe()
                        }
                    )

                    // Send Button
                    VoiceActionPillButton(
                        text = "Send",
                        icon = Lucide.Send,
                        contentColor = Color.White,
                        backgroundColor = palette.accentColor,
                        hasShadow = false,
                        onClick = {
                            SoundManager.playSend()
                            onSend()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Top Recording Status Pill with blinking recording dot, shimmering status title, and timer badge.
 */
@Composable
fun VoiceHeaderBadge(
    palette: OpticalVoiceGlobePalette,
    statusText: String,
    elapsedSeconds: Int,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerAnimations")

    // Blinking recording dot animation (@keyframes recBlink)
    val dotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recBlink"
    )
    val dotAlpha = 1f - (dotProgress * 0.65f)
    val dotScale = 1f - (dotProgress * 0.18f)

    // Shimmering status title animation (@keyframes text-light)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerText"
    )

    val formattedTime = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60
        val secs = (elapsedSeconds % 60).toString().padStart(2, '0')
        "$mins:$secs"
    }

    val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .background(Color.Transparent)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Blinking Blue Dot (.voice-rec-dot)
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(dotScale)
                .graphicsLayer { alpha = dotAlpha }
                .background(palette.accentColor, CircleShape)
        )

        // Shimmering Status Text (.voice-status-title)
        val shimmerBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to primaryTextColor,
                0.35f to primaryTextColor,
                0.45f to palette.accentColor,
                0.50f to Color.White,
                0.55f to palette.accentColor,
                0.65f to primaryTextColor,
                1.00f to primaryTextColor
            ),
            start = Offset(shimmerOffset - 400f, 0f),
            end = Offset(shimmerOffset, 120f),
            tileMode = TileMode.Repeated
        )

        Text(
            text = statusText,
            style = TextStyle(
                brush = shimmerBrush,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp
            )
        )

        // Timer Badge (.voice-timer-badge)
        Text(
            text = formattedTime,
            color = palette.accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/**
 * 3D Optical Liquid Glass Sphere:
 * - Dual expanding sonar wave rings (`::before`, `::after`)
 * - Breathing outer scale (`@keyframes circle1`)
 * - 4 rotating & morphing internal plasma gradients (`.c1`, `.c2`, `.c3`, `.c4`)
 * - Specular 3D inner glass lens highlight (`.glass`)
 * - Caustic bottom rim glow + crisp top white glass rim reflection
 * - Center 5-bar voice equalizer (`.orb-eq`)
 */
@Composable
fun LiquidGlassVoiceOrb(
    palette: OpticalVoiceGlobePalette,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPhysics")

    // Outer breathing scale (@keyframes circle1: 0.97 <-> 1.02 over 2.5s)
    val orbBreathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1Breathing"
    )

    // Wave Ring 1 (2.1s infinite)
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    // Wave Ring 2 (delayed phase by 0.333f ~ 0.7s)
    val wave2Progress = (wave1Progress + 0.333f) % 1.0f

    // 4 Plasma Blobs continuous rotation phases (@keyframes ai1, ai2, ai3, ai4)
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "ai1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5800, easing = LinearEasing)),
        label = "ai2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "ai3"
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "ai4"
    )

    Box(
        modifier = modifier.scale(orbBreathingScale),
        contentAlignment = Alignment.Center
    ) {
        // 1. Expanding Sonar Wave Rings (.ai::before, .ai::after)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = (size.minDimension * 0.52f) / 2f

            listOf(wave1Progress, wave2Progress).forEach { progress ->
                val currentScale = 0.98f + (1.56f - 0.98f) * progress
                val currentAlpha = (1f - progress) * 0.85f
                val ringRadius = baseRadius * currentScale

                // Soft outer glow
                drawCircle(
                    color = palette.waveColor.copy(alpha = currentAlpha * 0.25f),
                    radius = ringRadius,
                    style = Stroke(width = 8.dp.toPx())
                )
                // Crisp 1.5dp ring border
                drawCircle(
                    color = palette.waveColor.copy(alpha = currentAlpha * 0.42f),
                    radius = ringRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // 2. Main 120.dp Core Orb Container (.ai .container + .ai .glass)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // A. Base Core Radial Gradient (--orb-core at 30% 25%)
                val focalOffset = Offset(size.width * 0.30f, size.height * 0.25f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = palette.orbCoreStops.toTypedArray(),
                        center = focalOffset,
                        radius = radius * 1.55f
                    ),
                    radius = radius,
                    center = center
                )

                // Helper to draw the 4 animated liquid plasma blobs (.c1, .c2, .c3, .c4)
                fun drawPlasmaBlob(
                    phase: Float,
                    startAngleDeg: Float,
                    orbitFactor: Float,
                    baseDiameterRatio: Float,
                    scales: FloatArray,
                    alphas: FloatArray,
                    innerColor: Color,
                    outerColor: Color
                ) {
                    val angleRad = ((startAngleDeg + phase * 360f) * PI / 180.0).toFloat()
                    val scale = interpolateKeyframe(phase, scales)
                    val alpha = interpolateKeyframe(phase, alphas)
                    val blobRadius = (size.minDimension * baseDiameterRatio * 0.5f) * scale
                    val orbitDist = blobRadius * orbitFactor

                    val blobCenter = Offset(
                        x = center.x + cos(angleRad) * orbitDist,
                        y = center.y + sin(angleRad) * orbitDist
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                innerColor.copy(alpha = alpha),
                                outerColor.copy(alpha = alpha * 0.65f),
                                Color.Transparent
                            ),
                            center = blobCenter,
                            radius = blobRadius.coerceAtLeast(1f)
                        ),
                        radius = blobRadius.coerceAtLeast(1f),
                        center = blobCenter
                    )
                }

                // .c4 Deep Base Plasma
                drawPlasmaBlob(
                    phase = phase4,
                    startAngleDeg = 270f,
                    orbitFactor = 0.40f,
                    baseDiameterRatio = 1.28f,
                    scales = floatArrayOf(1.0f, 0.8f, 1.5f, 0.95f, 1.0f),
                    alphas = floatArrayOf(0.95f, 0.65f, 0.6f, 0.9f, 0.95f),
                    innerColor = palette.c4Inner,
                    outerColor = palette.c4Outer
                )

                // .c1 Vibrant Main Accent Plasma
                drawPlasmaBlob(
                    phase = phase1,
                    startAngleDeg = 0f,
                    orbitFactor = 0.45f,
                    baseDiameterRatio = 1.36f,
                    scales = floatArrayOf(0.95f, 1.65f, 0.8f, 1.3f, 0.95f),
                    alphas = floatArrayOf(0.5f, 1.0f, 0.55f, 1.0f, 0.5f),
                    innerColor = palette.c1Inner,
                    outerColor = palette.c1Outer
                )

                // .c2 Bright White/Ice Highlight Plasma
                drawPlasmaBlob(
                    phase = phase2,
                    startAngleDeg = 90f,
                    orbitFactor = 0.42f,
                    baseDiameterRatio = 0.76f,
                    scales = floatArrayOf(0.65f, 1.55f, 1.05f, 0.85f, 0.65f),
                    alphas = floatArrayOf(0.9f, 0.35f, 0.8f, 0.4f, 0.9f),
                    innerColor = palette.c2Inner,
                    outerColor = palette.c2Outer
                )

                // .c3 Glow Plasma
                drawPlasmaBlob(
                    phase = phase3,
                    startAngleDeg = 180f,
                    orbitFactor = 0.44f,
                    baseDiameterRatio = 1.12f,
                    scales = floatArrayOf(0.85f, 1.45f, 0.7f, 1.3f, 0.85f),
                    alphas = floatArrayOf(0.85f, 0.6f, 0.5f, 0.85f, 0.85f),
                    innerColor = palette.c3Inner,
                    outerColor = palette.c3Outer
                )

                // B. Optical 3D Glass Lens Specular Highlight (.ai .glass)
                val specularCenter = Offset(size.width * 0.32f, size.height * 0.24f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.88f),
                            0.55f to Color.White.copy(alpha = 0.14f),
                            0.75f to Color.Transparent
                        ),
                        center = specularCenter,
                        radius = 62.dp.toPx()
                    ),
                    radius = radius,
                    center = center
                )

                // C. Bottom Caustic Rim Glow
                drawCircle(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to palette.causticColor.copy(alpha = 0.48f)
                        )
                    ),
                    radius = radius,
                    center = center
                )

                // D. Crisp Top White Glass Rim Reflection
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.25f),
                            palette.causticColor.copy(alpha = 0.35f)
                        )
                    ),
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Center 5-Bar Animated Voice Equalizer (.orb-eq)
            OrbEqualizerBars()
        }
    }
}

/**
 * 5-bar responsive voice equalizer pulsing inside the liquid orb.
 */
@Composable
fun OrbEqualizerBars() {
    val baseHeights = remember { listOf(9.dp, 18.dp, 24.dp, 15.dp, 10.dp) }
    val delays = remember { listOf(0, 150, 300, 120, 250) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbEq")

    Row(
        modifier = Modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        baseHeights.forEachIndexed { index, baseHeight ->
            val scaleY by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 850,
                        delayMillis = delays[index],
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eqBar$index"
            )
            val alpha = 0.7f + ((scaleY - 0.45f) / 0.7f) * 0.3f

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeight)
                    .graphicsLayer {
                        this.scaleY = scaleY
                        this.alpha = alpha.coerceIn(0.65f, 1f)
                    }
                    .shadow(4.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                    .background(Color.White.copy(alpha = 0.94f), CircleShape)
            )
        }
    }
}

/**
 * Pill button for bottom actions (Cancel, Text, Send).
 */
@Composable
fun VoiceActionPillButton(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    backgroundColor: Color,
    hasShadow: Boolean = false,
    shadowColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Linearly interpolates a keyframe array to replicate CSS `@keyframes ai1..ai4`.
 */
private fun interpolateKeyframe(phase: Float, values: FloatArray): Float {
    val scaled = (phase.coerceIn(0f, 1f) * (values.size - 1))
    val index = scaled.toInt().coerceAtMost(values.size - 2)
    val fraction = scaled - index
    return values[index] + (values[index + 1] - values[index]) * fraction
}



/**
 * Animated recording indicator dot with blinking glow (matching .voice-rec-dot).
 */
@Composable
fun VoiceRecDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "VoiceRecDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Box(
        modifier = modifier
            .size(7.dp)
            .scale(scale)
            .graphicsLayer(alpha = alpha)
            .background(color, CircleShape)
    )
}

/**
 * Legacy alias for BlinkingDot.
 */
@Composable
fun BlinkingDot(color: Color, modifier: Modifier = Modifier) {
    VoiceRecDot(color = color, modifier = modifier)
}

/**
 * Legacy Bridge compatibility stub.
 */
class VoiceGlobeBridge(
    private val onCancelCallback: () -> Unit = {},
    private val onTranscribeCallback: () -> Unit = {},
    private val onSendCallback: (Int) -> Unit = {}
) {
    @JavascriptInterface fun onCancel() { onCancelCallback() }
    @JavascriptInterface fun onTranscribe() { onTranscribeCallback() }
    @JavascriptInterface fun onSend(durationSec: Int) { onSendCallback(durationSec) }
}
