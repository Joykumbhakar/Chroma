package com.chroma.studio.chat.ui.components

import android.webkit.JavascriptInterface
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.VoiceGlobeStyleType
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 1. PALETTE DEFINITIONS & THEME MAPPER
// ============================================================================

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
    val waveColor: Color,
    val specularGlassHighlight: Color = Color(0x40FFFFFF),
    val chromaticRimColors: List<Color> = listOf(
        Color(0xD9FFFFFF),
        Color(0x9938BDF8),
        Color(0x80818CF8),
        Color(0x73C084FC),
        Color(0x9938BDF8),
        Color(0xD9FFFFFF)
    )
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
        VoiceGlobeStyleType.STRANDS -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFF3569FA),
            orbCoreStops = listOf(
                0.00f to Color(0xFF93F1FF),
                0.38f to Color(0xFF3569FA),
                0.74f to Color(0xFF193D9B),
                1.00f to Color(0xFF050B24)
            ),
            c1Inner = Color(0xFFFFA24C),
            c1Outer = Color(0xFFD95B56),
            c2Inner = Color.White,
            c2Outer = Color(0xFFFFE0B3),
            c3Inner = Color(0xFF93F1FF),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF3569FA),
            c4Outer = Color(0xFF193D9B),
            causticColor = Color(0xFF93F1FF),
            waveColor = Color(0xFF3569FA)
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
        VoiceGlobeStyleType.CHROMA_BLOB -> OpticalVoiceGlobePalette(
            accentColor = Color(0xFFFF00A7),
            orbCoreStops = listOf(
                0.00f to Color(0xFFFF50A1),
                0.38f to Color(0xFFFF00A7),
                0.74f to Color(0xFF8800CC),
                1.00f to Color(0xFF004DFF)
            ),
            c1Inner = Color(0xFFFF50A1),
            c1Outer = Color(0xFFFF6700),
            c2Inner = Color.White,
            c2Outer = Color(0xFFFFCCE8),
            c3Inner = Color(0xFFFF00A7),
            c3Outer = Color.Transparent,
            c4Inner = Color(0xFF004DFF),
            c4Outer = Color(0xFF001A99),
            causticColor = Color(0xFFFF50A1),
            waveColor = Color(0xFFFF00A7)
        )
    }
}

// ============================================================================
// 2. MAIN VOICE BUBBLE CARD (With Apple visionOS Specular Rim & 3D Touch Tilt)
// ============================================================================

/**
 * 100% Native Jetpack Compose Optical 3D Liquid Glass Voice Bubble Card.
 * Replicates the Apple VisionOS design with interactive 3D parallax tilt,
 * voice-reactive acoustic resonance, specular glass reflections, and smooth spring physics.
 */
@Composable
fun VoiceRecorderCard(
    recordingDurationSec: Int,
    statusText: String = "Listening...",
    globeStyle: VoiceGlobeStyleType = VoiceGlobeStyleType.ORIGINAL_BLUE,
    voiceAmplitude: Float = 0f,
    onCancel: () -> Unit,
    onTranscribe: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val isDark = !colors.isLight
    val palette = remember(globeStyle, isDark) { getOpticalVoiceGlobePalette(globeStyle, isDark) }
    val cardShape = RoundedCornerShape(40.dp)
    val coroutineScope = rememberCoroutineScope()

    // 3D Interactive Tilt Parallax when touching and dragging across the voice card
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(width = 304.dp, height = 312.dp)
            .graphicsLayer {
                rotationY = tiltX.value * 7f
                rotationX = -tiltY.value * 7f
                cameraDistance = 16f * density
            }
            .shadow(
                elevation = 28.dp,
                shape = cardShape,
                ambientColor = if (isDark) Color(0xA6000000) else Color(0x2E0F172A),
                spotColor = if (isDark) palette.accentColor.copy(alpha = 0.28f) else palette.accentColor.copy(alpha = 0.20f)
            )
            .clip(cardShape)
            .background(if (isDark) Color(0xF20F172A) else Color(0xF5FFFFFF))
            // Apple visionOS dual-tone specular glass border
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF),
                        if (isDark) Color(0x14FFFFFF) else Color(0x140F172A),
                        palette.accentColor.copy(alpha = 0.20f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 600f)
                ),
                shape = cardShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch { tiltX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
                        coroutineScope.launch { tiltY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
                    },
                    onDragCancel = {
                        coroutineScope.launch { tiltX.animateTo(0f, spring()) }
                        coroutineScope.launch { tiltY.animateTo(0f, spring()) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nextX = (tiltX.value + dragAmount.x / 120f).coerceIn(-1f, 1f)
                        val nextY = (tiltY.value + dragAmount.y / 120f).coerceIn(-1f, 1f)
                        coroutineScope.launch { tiltX.snapTo(nextX) }
                        coroutineScope.launch { tiltY.snapTo(nextY) }
                    }
                )
            }
            .padding(20.dp)
    ) {
        // Center 3D Liquid Glass Voice Globe (250dp container centered at 48% Y)
        if (globeStyle == VoiceGlobeStyleType.STRANDS) {
            StrandsVoiceGlobe(
                level = voiceAmplitude,
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset(y = (-4).dp),
                params = StrandsParams(glassSize = 0.85f, lightMode = !isDark)
            )
        } else if (globeStyle == VoiceGlobeStyleType.CHROMA_BLOB) {
            AIBlobVoiceGlobe(
                level = voiceAmplitude,
                listening = true,
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset(y = (-4).dp),
                params = AIBlobParams()
            )
        } else {
            LiquidGlassVoiceOrb(
                palette = palette,
                voiceAmplitude = voiceAmplitude,
                parallaxOffset = Offset(tiltX.value, tiltY.value),
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset(y = (-4).dp)
            )
        }

        // Overlay Column for Header Status Pill & Action Buttons
        Column(
            modifier = Modifier.fillMaxSize(),
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

            // Bottom Action Buttons: Cancel | Text | Send
            VoiceActionsRow(
                palette = palette,
                isDark = isDark,
                onCancel = onCancel,
                onTranscribe = onTranscribe,
                onSend = onSend
            )
        }
    }
}

// ============================================================================
// 3. OPTICAL 3D LIQUID GLASS GLOBE (Optics & Physics Engine)
// ============================================================================

/**
 * 3D Optical Liquid Glass Sphere:
 * - Dual expanding sonar wave rings with voice boost
 * - Breathing outer scale coupled with voice amplitude
 * - 4 rotating & morphing internal plasma gradients
 * - Apple VisionOS specular 3D inner glass lens highlight with touch parallax
 * - Refracted counter-bounce caustic exit
 * - Caustic bottom rim glow + crisp top white glass rim reflection
 * - Center 5-bar voice equalizer with formant sensitivity weights
 */
@Composable
fun LiquidGlassVoiceOrb(
    palette: OpticalVoiceGlobePalette,
    voiceAmplitude: Float = 0f,
    parallaxOffset: Offset = Offset.Zero,
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

    val effectiveSphereScale = (orbBreathingScale + voiceAmplitude * 0.065f)

    // Wave Ring 1 (2.1s infinite)
    val sonarEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = sonarEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
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
        modifier = modifier.scale(effectiveSphereScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)

            val coreDiameter = canvasSize * 0.50f
            val coreRadius = coreDiameter / 2f

            // 3D Parallax offsets
            val parallaxPx = 10.dp.toPx()
            val coreParallax = Offset(
                x = -parallaxOffset.x * parallaxPx * 0.65f,
                y = -parallaxOffset.y * parallaxPx * 0.65f
            )
            val specularParallax = Offset(
                x = parallaxOffset.x * parallaxPx * 1.15f,
                y = parallaxOffset.y * parallaxPx * 1.15f
            )

            // ----------------------------------------------------------------
            // A. DUAL SONAR WAVE RINGS (.ai::before, .ai::after)
            // ----------------------------------------------------------------
            val baseWaveRadius = (canvasSize * 0.52f) / 2f
            drawSonarWaveRing(
                center = center,
                baseRadius = baseWaveRadius,
                progress = wave1Progress,
                waveColor = palette.waveColor,
                voiceBoost = voiceAmplitude
            )
            drawSonarWaveRing(
                center = center,
                baseRadius = baseWaveRadius,
                progress = wave2Progress,
                waveColor = palette.waveColor,
                voiceBoost = voiceAmplitude
            )

            // ----------------------------------------------------------------
            // B. SOFT AMBIENT CAUSTIC GLOW UNDER THE SPHERE
            // ----------------------------------------------------------------
            val glowRadius = coreRadius * (1.36f + voiceAmplitude * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentColor.copy(alpha = 0.35f + voiceAmplitude * 0.20f),
                        palette.causticColor.copy(alpha = 0.14f + voiceAmplitude * 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(center.x, center.y + 12.dp.toPx()),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(center.x, center.y + 12.dp.toPx())
            )

            // ----------------------------------------------------------------
            // C. SPHERE CORE & ORBITING LIQUID BLOBS (.ai .container)
            // ----------------------------------------------------------------
            val spherePath = Path().apply {
                addOval(Rect(center = center, radius = coreRadius))
            }

            clipPath(spherePath) {
                // 1. Base Core Radial Gradient at (30%, 25%) shifted by 3D parallax
                val coreFocalOffset = Offset(
                    x = (center.x - coreRadius) + coreDiameter * 0.30f + coreParallax.x,
                    y = (center.y - coreRadius) + coreDiameter * 0.25f + coreParallax.y
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = palette.orbCoreStops.toTypedArray(),
                        center = coreFocalOffset,
                        radius = coreDiameter * 0.95f
                    ),
                    radius = coreRadius,
                    center = center
                )

                // 2. Orbiting Liquid Blobs (c4, c1, c2, c3)
                val unitScale = (canvasSize / 250f) * (1f + voiceAmplitude * 0.16f)
                val orbitCenter = center + coreParallax * 0.5f

                // Blob c4: Deep Base Plasma
                val c4State = evaluateKeyframes(
                    phase = phase4,
                    startAngleDeg = 270f,
                    translateFraction = 0.40f,
                    scales = floatArrayOf(1.00f, 0.80f, 1.50f, 0.95f, 1.00f),
                    alphas = floatArrayOf(0.95f, 0.65f, 0.60f, 0.90f, 0.95f)
                )
                drawBlurredOrbitalBlob(
                    sphereCenter = orbitCenter,
                    baseDiameter = 160f * unitScale,
                    state = c4State,
                    innerColor = palette.c4Inner,
                    outerColor = palette.c4Outer
                )

                // Blob c1: Vibrant Main Accent Plasma
                val c1State = evaluateKeyframes(
                    phase = phase1,
                    startAngleDeg = 0f,
                    translateFraction = 0.45f,
                    scales = floatArrayOf(0.95f, 1.65f, 0.80f, 1.30f, 0.95f),
                    alphas = floatArrayOf(0.50f, 1.00f, 0.55f, 1.00f, 0.50f)
                )
                drawBlurredOrbitalBlob(
                    sphereCenter = orbitCenter,
                    baseDiameter = 170f * unitScale,
                    state = c1State,
                    innerColor = palette.c1Inner,
                    outerColor = palette.c1Outer
                )

                // Blob c2: Bright Highlight Plasma
                val c2State = evaluateKeyframes(
                    phase = phase2,
                    startAngleDeg = 90f,
                    translateFraction = 0.42f,
                    scales = floatArrayOf(0.65f, 1.55f, 1.05f, 0.85f, 0.65f),
                    alphas = floatArrayOf(0.90f, 0.35f, 0.80f, 0.40f, 0.90f)
                )
                drawBlurredOrbitalBlob(
                    sphereCenter = orbitCenter,
                    baseDiameter = 95f * unitScale,
                    state = c2State,
                    innerColor = palette.c2Inner,
                    outerColor = palette.c2Outer
                )

                // Blob c3: Glow Plasma
                val c3State = evaluateKeyframes(
                    phase = phase3,
                    startAngleDeg = 180f,
                    translateFraction = 0.44f,
                    scales = floatArrayOf(0.85f, 1.45f, 0.70f, 1.30f, 0.85f),
                    alphas = floatArrayOf(0.85f, 0.60f, 0.50f, 0.85f, 0.85f)
                )
                drawBlurredOrbitalBlob(
                    sphereCenter = orbitCenter,
                    baseDiameter = 140f * unitScale,
                    state = c3State,
                    innerColor = palette.c3Inner,
                    outerColor = palette.c3Outer
                )

                // 3. Subsurface Iridescent Violet-Cyan Glow
                if (voiceAmplitude > 0.08f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFA855F7).copy(alpha = 0.28f * voiceAmplitude),
                                Color(0xFF38BDF8).copy(alpha = 0.18f * voiceAmplitude),
                                Color.Transparent
                            ),
                            center = center,
                            radius = coreRadius * 0.75f
                        ),
                        radius = coreRadius * 0.75f,
                        center = center,
                        blendMode = BlendMode.Plus
                    )
                }

                // 4. Inner Deep Vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.00f to Color(0x5900328C)
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )
            }

            // ----------------------------------------------------------------
            // D. OPTICAL 3D GLASS SHELL (.ai .glass)
            // ----------------------------------------------------------------
            val glassRadius = coreRadius + 2.dp.toPx()
            val glassDiameter = glassRadius * 2f
            val glassPath = Path().apply {
                addOval(Rect(center = center, radius = glassRadius))
            }

            clipPath(glassPath) {
                // 1. Primary Optical Specular Reflection at (32%, 24%)
                val specularCenter = Offset(
                    x = (center.x - glassRadius) + glassDiameter * 0.32f + specularParallax.x,
                    y = (center.y - glassRadius) + glassDiameter * 0.24f + specularParallax.y
                )
                val specularRadius = 62.dp.toPx() * 0.78f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xE0FFFFFF),
                            0.55f to Color(0x1FFFFFFF),
                            0.78f to Color.Transparent,
                            1.00f to Color.Transparent
                        ),
                        center = specularCenter,
                        radius = specularRadius
                    ),
                    radius = glassRadius,
                    center = center
                )

                // 2. Secondary Counter-Bounce Caustic at (72%, 78%)
                val counterBounceCenter = Offset(
                    x = (center.x - glassRadius) + glassDiameter * 0.72f - specularParallax.x * 0.6f,
                    y = (center.y - glassRadius) + glassDiameter * 0.78f - specularParallax.y * 0.6f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x59BAE6FD),
                            0.45f to Color(0x1F38BDF8),
                            1.00f to Color.Transparent
                        ),
                        center = counterBounceCenter,
                        radius = glassRadius * 0.52f
                    ),
                    radius = glassRadius,
                    center = center
                )

                // 3. Bottom Caustic Rim Glow
                val bottomCausticCenter = Offset(
                    x = center.x,
                    y = center.y + glassRadius * 0.78f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to palette.causticColor.copy(alpha = 0.42f),
                            0.55f to palette.causticColor.copy(alpha = 0.15f),
                            1.00f to Color.Transparent
                        ),
                        center = bottomCausticCenter,
                        radius = glassRadius * 0.85f
                    ),
                    radius = glassRadius,
                    center = center
                )

                // 4. Top Inner White Rim Reflection
                drawCircle(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xEBFFFFFF),
                            0.22f to Color(0x47FFFFFF),
                            0.50f to Color(0x1A818CF8),
                            0.82f to palette.causticColor.copy(alpha = 0.35f),
                            1.00f to palette.causticColor.copy(alpha = 0.55f)
                        ),
                        start = Offset(center.x + specularParallax.x * 0.4f, center.y - glassRadius),
                        end = Offset(center.x - specularParallax.x * 0.4f, center.y + glassRadius)
                    ),
                    radius = glassRadius - 1.2.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }

        // Center 5-Bar Voice Equalizer
        OrbEqualizerBars(voiceAmplitude = voiceAmplitude)
    }
}

// ============================================================================
// 4. 5-BAR VOICE-REACTIVE EQUALIZER (.orb-eq)
// ============================================================================

@Composable
fun OrbEqualizerBars(voiceAmplitude: Float = 0f) {
    val barSpecs = remember {
        listOf(
            Triple(9.dp, 0, 0.65f),
            Triple(18.dp, 150, 0.90f),
            Triple(24.dp, 300, 1.15f),
            Triple(15.dp, 120, 0.85f),
            Triple(10.dp, 250, 0.60f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orbEqPulse")

    Row(
        modifier = Modifier.height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barSpecs.forEachIndexed { index, (baseHeight, delayMs, formantWeight) ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 850,
                        delayMillis = delayMs,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eqBar_$index"
            )

            val baseScaleY = lerpFloat(0.45f, 1.15f, progress)
            val voiceScaleBoost = voiceAmplitude * formantWeight * 0.32f
            val finalScaleY = (baseScaleY + voiceScaleBoost).coerceIn(0.38f, 1.38f)
            val alpha = lerpFloat(0.75f, 1.00f, progress)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeight * finalScaleY)
                    .shadow(
                        elevation = 5.dp,
                        shape = CircleShape,
                        ambientColor = Color.White,
                        spotColor = Color.White
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f * alpha))
            )
        }
    }
}

// ============================================================================
// 5. TOP STATUS BADGE (Blinking Dot + Shimmer Text + Monospace Timer)
// ============================================================================

@Composable
fun VoiceHeaderBadge(
    palette: OpticalVoiceGlobePalette,
    statusText: String,
    elapsedSeconds: Int,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerStatusAnim")

    val recBlinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recBlink"
    )
    val dotAlpha = lerpFloat(1f, 0.35f, recBlinkProgress)
    val dotScale = lerpFloat(1f, 0.82f, recBlinkProgress)

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -220f,
        targetValue = 420f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerText"
    )

    val formattedTimer = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60
        val secs = (elapsedSeconds % 60).toString().padStart(2, '0')
        "$mins:$secs"
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDark) Color(0x1F94A3B8) else Color(0x1494A3B8))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x120F172A),
                CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Blinking Recording Dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(dotScale)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = palette.accentColor,
                    spotColor = palette.accentColor
                )
                .clip(CircleShape)
                .background(palette.accentColor.copy(alpha = dotAlpha))
        )

        // Shimmering Status Title
        val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val shimmerBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to primaryTextColor,
                0.32f to primaryTextColor,
                0.45f to palette.accentColor,
                0.50f to Color(0xFF38BDF8),
                0.55f to palette.accentColor,
                0.68f to primaryTextColor,
                1.00f to primaryTextColor
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 260f, 140f)
        )

        Text(
            text = statusText,
            style = TextStyle(
                brush = shimmerBrush,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp
            )
        )

        // Tabular Timer Badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = formattedTimer,
                color = palette.accentColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ============================================================================
// 6. BOTTOM ACTION BUTTONS (Cancel | Text | Send)
// ============================================================================

@Composable
fun VoiceActionsRow(
    palette: OpticalVoiceGlobePalette,
    isDark: Boolean,
    onCancel: () -> Unit,
    onTranscribe: () -> Unit,
    onSend: () -> Unit
) {
    val colors = LocalChatColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Cancel Button
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isDark) Color(0x1FFFFFFF) else Color(0x1494A3B8))
                .clickable {
                    SoundManager.playReaction()
                    onCancel()
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CloseIconCanvas(color = colors.textMuted, size = 10.dp)
            Text(
                text = "Cancel",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. Text (Transcribe) Button
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.accentColor.copy(alpha = 0.12f))
                .clickable {
                    SoundManager.playReaction()
                    onTranscribe()
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Aa",
                color = palette.accentColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Text",
                color = palette.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 3. Send Button
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = palette.accentColor.copy(alpha = 0.35f),
                    spotColor = palette.accentColor.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(palette.accentColor)
                .clickable {
                    SoundManager.playSend()
                    onSend()
                }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PaperPlaneRightIconCanvas(color = Color.White, size = 11.dp)
            Text(
                text = "Send",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================================================
// 7. MATHEMATICAL & RENDERING HELPERS
// ============================================================================

data class OrbitalBlobState(
    val angleDegrees: Float,
    val translateFraction: Float,
    val scale: Float,
    val alpha: Float
)

fun evaluateKeyframes(
    phase: Float,
    startAngleDeg: Float,
    translateFraction: Float,
    scales: FloatArray,
    alphas: FloatArray
): OrbitalBlobState {
    val clamped = phase.coerceIn(0f, 1f)
    val segmentFloat = clamped * 4f
    val index = segmentFloat.toInt().coerceIn(0, 3)
    val localFraction = segmentFloat - index

    val interpolatedScale = lerpFloat(scales[index], scales[index + 1], localFraction)
    val interpolatedAlpha = lerpFloat(alphas[index], alphas[index + 1], localFraction)
    val currentAngle = startAngleDeg + (clamped * 360f)

    return OrbitalBlobState(
        angleDegrees = currentAngle,
        translateFraction = translateFraction,
        scale = interpolatedScale,
        alpha = interpolatedAlpha
    )
}

fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

fun DrawScope.drawSonarWaveRing(
    center: Offset,
    baseRadius: Float,
    progress: Float,
    waveColor: Color,
    voiceBoost: Float = 0f
) {
    val maxScale = 1.56f + (voiceBoost * 0.12f)
    val scale = lerpFloat(0.98f, maxScale, progress)
    val alpha = lerpFloat(0.85f, 0f, progress).coerceIn(0f, 1f)
    val currentRadius = baseRadius * scale

    // Soft outer glow
    drawCircle(
        color = waveColor.copy(alpha = (0.16f + voiceBoost * 0.08f) * alpha),
        radius = currentRadius,
        center = center,
        style = Stroke(width = 6.dp.toPx())
    )

    // Crisp 1.5dp sonar border
    drawCircle(
        color = waveColor.copy(alpha = (0.38f + voiceBoost * 0.12f) * alpha),
        radius = currentRadius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

fun DrawScope.drawBlurredOrbitalBlob(
    sphereCenter: Offset,
    baseDiameter: Float,
    state: OrbitalBlobState,
    innerColor: Color,
    outerColor: Color
) {
    val angleRad = Math.toRadians(state.angleDegrees.toDouble())
    val translateDist = baseDiameter * state.translateFraction
    val blobCenter = Offset(
        x = sphereCenter.x + (cos(angleRad) * translateDist).toFloat(),
        y = sphereCenter.y + (sin(angleRad) * translateDist).toFloat()
    )

    val scaledRadius = (baseDiameter / 2f) * state.scale
    val blurFeatherRadius = scaledRadius + 7.dp.toPx()

    if (blurFeatherRadius <= 1f || state.alpha <= 0.01f) return

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to innerColor.copy(alpha = innerColor.alpha * state.alpha),
                0.55f to outerColor.copy(alpha = outerColor.alpha * state.alpha * 0.88f),
                0.84f to outerColor.copy(alpha = outerColor.alpha * state.alpha * 0.35f),
                1.00f to Color.Transparent
            ),
            center = blobCenter,
            radius = blurFeatherRadius
        ),
        radius = blurFeatherRadius,
        center = blobCenter
    )
}

// ============================================================================
// 8. VECTOR CANVAS ICONS (Lightweight, zero dependency)
// ============================================================================

@Composable
fun CloseIconCanvas(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 1.8.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(this.size.width, this.size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(this.size.width, 0f),
            end = Offset(0f, this.size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PaperPlaneRightIconCanvas(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.15f)
            lineTo(w * 0.92f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.85f)
            lineTo(w * 0.28f, h * 0.5f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
        )
    }
}

// ============================================================================
// 9. BACKWARD COMPATIBILITY STUBS & ALIASES
// ============================================================================

@Composable
fun VoiceRecDot(color: Color, modifier: Modifier = Modifier) {
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

@Composable
fun BlinkingDot(color: Color, modifier: Modifier = Modifier) {
    VoiceRecDot(color = color, modifier = modifier)
}

class VoiceGlobeBridge(
    private val onCancelCallback: () -> Unit = {},
    private val onTranscribeCallback: () -> Unit = {},
    private val onSendCallback: (Int) -> Unit = {}
) {
    @JavascriptInterface fun onCancel() { onCancelCallback() }
    @JavascriptInterface fun onTranscribe() { onTranscribeCallback() }
    @JavascriptInterface fun onSend(durationSec: Int) { onSendCallback(durationSec) }
}
