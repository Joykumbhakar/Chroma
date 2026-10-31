package com.chroma.studio.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Default Apple Siri-inspired chromatic palette for the Strands component:
 * 1. Red (top edge)
 * 2. Red-Orange
 * 3. Amber
 * 4. Warm White core
 * 5. Cyan
 * 6. Azure Blue
 * 7. Deep Midnight Blue
 */
val DEFAULT_STRANDS_COLORS = listOf(
    Color(0xFFD95B56),
    Color(0xFFBA5046),
    Color(0xFFFFA24C),
    Color(0xFFFFE0B3),
    Color(0xFF93F1FF),
    Color(0xFF3569FA),
    Color(0xFF193D9B)
)

/**
 * Configuration parameters mirroring the React Bits <Strands /> component props.
 */
@Immutable
data class StrandsParams(
    val colors: List<Color> = DEFAULT_STRANDS_COLORS,
    val count: Int = 3,
    val speed: Float = 1.0f,
    val amplitude: Float = 1.0f,
    val waviness: Float = 1.0f,
    val energy: Float = 0.65f,
    val thickness: Float = 1.0f,
    val spread: Float = 1.0f,
    val glow: Float = 1.0f,
    val taper: Float = 1.0f,
    val intensity: Float = 1.0f,
    val saturation: Float = 1.0f,
    val hueShift: Float = 0.0f,
    val scale: Float = 1.0f,
    val glass: Boolean = true,
    val glassSize: Float = 1.0f,
    val refraction: Float = 1.0f,
    val rim: Float = 1.0f,
    val dispersion: Float = 1.0f,
    val interactive: Boolean = true,
    val clickPulse: Boolean = true,
    val intro: Boolean = true,
    val lightMode: Boolean = false,
    val opacity: Float = 1.0f
)

/**
 * Smoothstep interpolation matching GLSL smoothstep.
 */
private fun sstep(a: Float, b: Float, x: Float): Float {
    if (a == b) return if (x >= a) 1f else 0f
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Softplus approximation matching GLSL softp().
 */
private fun softp(z: Float): Float {
    return 0.03f * ln(1f + exp((z / 0.03f).coerceIn(-60f, 60f)))
}

/**
 * Natural conversational speech rhythm generator matching React Bits speech(t).
 */
private fun speech(t: Float): Float {
    val phrase = sstep(-0.8f, -0.3f, 0.6f * sin(t * 2.7f) + 0.5f * sin(t * 1.53f + 1.2f))
    val syllable = max(0f, sin(t * 17f + 1.6f * sin(t * 5.3f)))
    val stress = 0.6f + 0.4f * sin(t * 3.7f + 0.7f)
    return phrase * (0.35f + 0.65f * syllable) * (0.25f + stress)
}

/**
 * High-performance native Jetpack Compose implementation of the React Bits <Strands /> component.
 *
 * Simulates Siri-inspired flowing liquid chromatic strands with glass orb refraction,
 * acoustic reactivity, and interactive touch response.
 */
@Composable
fun StrandsVoiceGlobe(
    modifier: Modifier = Modifier,
    level: Float? = null,
    listening: Boolean = false,
    params: StrandsParams = StrandsParams(),
    onClick: (() -> Unit)? = null
) {
    // Animation state variables
    var clock by remember { mutableFloatStateOf(0f) }
    var pace by remember { mutableFloatStateOf(0f) }
    var activity by remember { mutableFloatStateOf(params.energy) }
    var hover by remember { mutableFloatStateOf(0f) }
    var hoverTarget by remember { mutableFloatStateOf(0f) }
    var burst by remember { mutableFloatStateOf(0f) }
    var leanX by remember { mutableFloatStateOf(0f) }
    var leanY by remember { mutableFloatStateOf(0f) }
    var leanTargetX by remember { mutableFloatStateOf(0f) }
    var leanTargetY by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableFloatStateOf(0f) }
    var pulseV by remember { mutableFloatStateOf(0f) }
    var speechT by remember { mutableFloatStateOf(0f) }
    var introProgress by remember { mutableFloatStateOf(if (params.intro) 0f else 1f) }

    // Sonar wave progress for audio reactivity feedback
    val infiniteTransition = rememberInfiniteTransition(label = "sonarWaves")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar1"
    )
    val sonarWave2 = (sonarWave1 + 0.5f) % 1.0f

    // Frame-by-frame physics loop matching the React Bits simulation
    LaunchedEffect(params.speed, params.energy, listening, level) {
        var lastNanos = 0L
        var introElapsed = 0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos == 0L) {
                    lastNanos = nowNanos
                    return@withFrameNanos
                }
                val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = nowNanos

                // 1. Intro unfold
                if (params.intro && introProgress < 1f) {
                    introElapsed += dt
                    val t = (introElapsed / 1.4f).coerceIn(0f, 1f)
                    introProgress = 1f - (1f - t).pow(3f)
                } else {
                    introProgress = 1f
                }

                // 2. Hover and Lean smoothing
                val hoverSpeed = if (hoverTarget > hover) 0.25f else 0.6f
                hover += (hoverTarget - hover) * (1f - exp(-dt / hoverSpeed))
                leanX += (leanTargetX - leanX) * (1f - exp(-dt / 0.35f))
                leanY += (leanTargetY - leanY) * (1f - exp(-dt / 0.35f))

                // 3. Click pulse spring dynamics
                pulseV += (-170f * pulse - 14f * pulseV) * dt
                pulse += pulseV * dt
                burst *= exp(-dt / 0.45f)

                // 4. Voice / Speech activity
                if (listening && level == null) {
                    speechT += dt
                }
                val voice = when {
                    level != null -> level.coerceIn(0f, 1f)
                    listening -> speech(speechT)
                    else -> 0f
                }
                val targetActivity = min(1.2f, max(params.energy.coerceIn(0f, 1f), voice) + hover * 0.18f + burst)
                val actSpeed = if (targetActivity > activity) 0.12f else 0.4f
                activity += (targetActivity - activity) * (1f - exp(-dt / actSpeed))

                // 5. Pace and Clock
                pace += (min(1f, activity) - pace) * (1f - exp(-dt / 0.8f))
                clock += dt * params.speed * (0.6f + 1.6f * pace)
            }
        }
    }

    // Touch gesture input
    val touchModifier = if (params.interactive || params.clickPulse || onClick != null) {
        Modifier.pointerInput(params.interactive, params.clickPulse) {
            detectTapGestures(
                onTap = {
                    if (params.clickPulse) {
                        pulseV += 5f
                        burst = min(1.2f, burst + 0.9f)
                    }
                    onClick?.invoke()
                }
            )
        }.pointerInput(params.interactive) {
            if (params.interactive) {
                detectDragGestures(
                    onDragEnd = {
                        hoverTarget = 0f
                        leanTargetX = 0f
                        leanTargetY = 0f
                    },
                    onDragCancel = {
                        hoverTarget = 0f
                        leanTargetX = 0f
                        leanTargetY = 0f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val halfMin = min(cx, cy).coerceAtLeast(1f)
                        val nx = (pos.x - cx) / halfMin
                        val ny = (pos.y - cy) / halfMin
                        val dist = hypot(nx, ny)
                        hoverTarget = (1.6f - dist).coerceIn(0f, 1f)
                        leanTargetX = nx.coerceIn(-1.2f, 1.2f) * 0.8f
                        leanTargetY = (-ny).coerceIn(-1.2f, 1.2f) * 0.8f
                    }
                )
            }
        }
    } else Modifier

    Box(
        modifier = modifier.then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val minDim = min(width, height)
            val center = Offset(width / 2f, height / 2f)

            // Sphere radius matching React Bits: 0.42 * glassSize
            val sphereRadius = (minDim / 2f) * 0.84f * params.glassSize * (1f + 0.03f * pulse)

            // ─────────────────────────────────────────────────────────────
            // 0. Ultrasonic Sonar Acoustic Rings (Active when voice is lively)
            // ─────────────────────────────────────────────────────────────
            if (activity > 0.05f) {
                val waveAlphaBase = (activity * 0.45f).coerceIn(0f, 0.45f)
                val ringColor = if (params.lightMode) Color(0xFF3569FA) else Color(0xFF93F1FF)

                fun drawSonarWave(progress: Float) {
                    val waveR = sphereRadius + progress * (sphereRadius * 0.42f)
                    val alpha = (1f - progress) * waveAlphaBase
                    if (alpha > 0.01f) {
                        drawCircle(
                            color = ringColor.copy(alpha = alpha),
                            radius = waveR,
                            center = center,
                            style = Stroke(width = (2.2f * (1f - progress)).coerceAtLeast(1f).dp.toPx())
                        )
                    }
                }

                drawSonarWave(sonarWave1)
                drawSonarWave(sonarWave2)
            }

            // Path for clipping the glass sphere
            val spherePath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center, sphereRadius))
            }

            // ─────────────────────────────────────────────────────────────
            // 1. Glass Sphere Background (Dark Obsidian or Frosted Light)
            // ─────────────────────────────────────────────────────────────
            if (params.glass) {
                if (params.lightMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF1F5F9),
                                Color(0xFFE2E8F0)
                            ),
                            center = center,
                            radius = sphereRadius
                        ),
                        radius = sphereRadius,
                        center = center
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F172E),
                                Color(0xFF070B18),
                                Color(0xFF03050B)
                            ),
                            center = center,
                            radius = sphereRadius
                        ),
                        radius = sphereRadius,
                        center = center
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 2. Liquid Chromatic Strands inside the Sphere
            // ─────────────────────────────────────────────────────────────
            clipPath(spherePath) {
                // Volumetric Caustic Glow Bloom behind the ribbons
                val bloomAlpha = (0.35f * params.glow * params.intensity * introProgress).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFA24C).copy(alpha = bloomAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = center + Offset(0f, -sphereRadius * 0.25f),
                        radius = sphereRadius * 0.85f
                    ),
                    radius = sphereRadius * 0.85f,
                    center = center + Offset(0f, -sphereRadius * 0.25f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3569FA).copy(alpha = bloomAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center + Offset(0f, sphereRadius * 0.25f),
                        radius = sphereRadius * 0.85f
                    ),
                    radius = sphereRadius * 0.85f,
                    center = center + Offset(0f, sphereRadius * 0.25f)
                )

                // Physics Wave Parameters
                val c = clock
                val sway = 0.75f + 0.45f * pace
                val motionX = (0.26f * sin(0.53f * c) + 0.1f * sin(1.19f * c + 1.3f)) * sway
                val motionY = 1f + (0.06f * sin(0.71f * c) + 0.035f * sin(1.37f * c + 0.4f)) * sway
                val motionZ = 0.06f * sin(0.41f * c + 2f) * sway

                val ampLevel = if (activity < 0.65f) {
                    0.25f + 0.75f * sstep(0f, 0.65f, activity)
                } else {
                    1.0f + 0.12f * ((activity - 0.65f) / 0.35f)
                }
                val grow = ((ampLevel - 0.25f) / 0.75f).coerceIn(0f, 1.25f)
                val a = params.amplitude
                val taper = max(params.taper, 0.05f)
                val bx = 0.0437f + motionZ + leanX * 0.08f
                val k = 1.0f * params.waviness

                // Number of horizontal evaluation samples across [-1, 1]
                val samples = 48
                val topPoints = ArrayList<Offset>(samples)
                val midTopPoints = ArrayList<Offset>(samples)
                val axisPoints = ArrayList<Offset>(samples)
                val midBottomPoints = ArrayList<Offset>(samples)
                val bottomPoints = ArrayList<Offset>(samples)

                for (i in 0 until samples) {
                    val normX = -1.05f + 2.1f * (i.toFloat() / (samples - 1))
                    val x = normX / max(0.05f, params.scale)

                    // axis(x) polynomial from React Bits
                    val ya = -0.2318f + leanY * 0.06f + (-0.2394f + leanX * 0.1f) * x +
                            0.2815f * a * x * x + 0.3368f * a * x * x * x

                    // bump & height envelopes
                    val bump = exp(-((x - bx) / (0.6879f * taper)).pow(2f))
                    val hW = 0.02f + softp(-0.12f + 0.5324f * a * grow * motionY * bump - 0.02f)

                    val cphi = cos(0.5f * PI.toFloat() + k * (x + 0.0664f) + motionX)
                    val envC = (0.0886f + 0.3f * exp(-((x - 0.0634f) / 0.6873f).pow(2f))) * params.spread * grow
                    val hC = -envC * tanh(cphi / 0.6f)

                    val screenX = center.x + normX * sphereRadius
                    val screenYAxis = center.y - ya * sphereRadius
                    val screenYTop = center.y - (ya + hW * 1.35f) * sphereRadius
                    val screenYMidTop = center.y - (ya + hW * 0.65f) * sphereRadius
                    val screenYMidBottom = center.y - (ya + hC * 0.65f) * sphereRadius
                    val screenYBottom = center.y - (ya + hC * 1.35f) * sphereRadius

                    topPoints.add(Offset(screenX, screenYTop))
                    midTopPoints.add(Offset(screenX, screenYMidTop))
                    axisPoints.add(Offset(screenX, screenYAxis))
                    midBottomPoints.add(Offset(screenX, screenYMidBottom))
                    bottomPoints.add(Offset(screenX, screenYBottom))
                }

                // ─────────────────────────────────────────────────────────
                // 2A. Lower Cool Liquid Ribbon (Cyan -> Azure -> Deep Navy)
                // ─────────────────────────────────────────────────────────
                val coolPath = Path().apply {
                    if (axisPoints.isNotEmpty()) {
                        moveTo(axisPoints[0].x, axisPoints[0].y)
                        for (i in 1 until samples) {
                            lineTo(axisPoints[i].x, axisPoints[i].y)
                        }
                        for (i in (samples - 1) downTo 0) {
                            lineTo(bottomPoints[i].x, bottomPoints[i].y)
                        }
                        close()
                    }
                }

                val coolGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF93F1FF).copy(alpha = 0.95f * introProgress),
                        Color(0xFF3569FA).copy(alpha = 0.85f * introProgress),
                        Color(0xFF193D9B).copy(alpha = 0.55f * introProgress),
                        Color(0xFF0A153A).copy(alpha = 0.1f * introProgress)
                    ),
                    startY = center.y - sphereRadius * 0.3f,
                    endY = center.y + sphereRadius * 0.8f
                )
                drawPath(path = coolPath, brush = coolGradient)

                // Cool ridge fibers
                val coolRidge1 = Path().apply {
                    if (bottomPoints.isNotEmpty()) {
                        moveTo(bottomPoints[0].x, bottomPoints[0].y)
                        for (i in 1 until samples) lineTo(bottomPoints[i].x, bottomPoints[i].y)
                    }
                }
                val coolRidge2 = Path().apply {
                    if (midBottomPoints.isNotEmpty()) {
                        moveTo(midBottomPoints[0].x, midBottomPoints[0].y)
                        for (i in 1 until samples) lineTo(midBottomPoints[i].x, midBottomPoints[i].y)
                    }
                }
                drawPath(
                    path = coolRidge1,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF3569FA), Color(0xFF93F1FF), Color.Transparent),
                        startX = center.x - sphereRadius,
                        endX = center.x + sphereRadius
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawPath(
                    path = coolRidge2,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF93F1FF), Color.White, Color.Transparent),
                        startX = center.x - sphereRadius,
                        endX = center.x + sphereRadius
                    ),
                    style = Stroke(width = 2.4.dp.toPx())
                )

                // ─────────────────────────────────────────────────────────
                // 2B. Upper Warm Liquid Ribbon (Warm White -> Amber -> Red)
                // ─────────────────────────────────────────────────────────
                val warmPath = Path().apply {
                    if (axisPoints.isNotEmpty()) {
                        moveTo(axisPoints[0].x, axisPoints[0].y)
                        for (i in 1 until samples) {
                            lineTo(axisPoints[i].x, axisPoints[i].y)
                        }
                        for (i in (samples - 1) downTo 0) {
                            lineTo(topPoints[i].x, topPoints[i].y)
                        }
                        close()
                    }
                }

                val warmGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD95B56).copy(alpha = 0.90f * introProgress),
                        Color(0xFFBA5046).copy(alpha = 0.85f * introProgress),
                        Color(0xFFFFA24C).copy(alpha = 0.88f * introProgress),
                        Color(0xFFFFE0B3).copy(alpha = 0.95f * introProgress)
                    ),
                    startY = center.y - sphereRadius * 0.8f,
                    endY = center.y + sphereRadius * 0.2f
                )
                drawPath(path = warmPath, brush = warmGradient)

                // Warm ridge fibers (3 ridges if count == 3)
                val warmRidgeTop = Path().apply {
                    if (topPoints.isNotEmpty()) {
                        moveTo(topPoints[0].x, topPoints[0].y)
                        for (i in 1 until samples) lineTo(topPoints[i].x, topPoints[i].y)
                    }
                }
                val warmRidgeMid = Path().apply {
                    if (midTopPoints.isNotEmpty()) {
                        moveTo(midTopPoints[0].x, midTopPoints[0].y)
                        for (i in 1 until samples) lineTo(midTopPoints[i].x, midTopPoints[i].y)
                    }
                }

                drawPath(
                    path = warmRidgeTop,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFBA5046), Color(0xFFD95B56), Color.Transparent),
                        startX = center.x - sphereRadius,
                        endX = center.x + sphereRadius
                    ),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                if (params.count >= 2) {
                    drawPath(
                        path = warmRidgeMid,
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFFFA24C), Color(0xFFFFE0B3), Color.Transparent),
                            startX = center.x - sphereRadius,
                            endX = center.x + sphereRadius
                        ),
                        style = Stroke(width = 2.4.dp.toPx())
                    )
                }

                // Center core baseline fiber
                val coreFiberPath = Path().apply {
                    if (axisPoints.isNotEmpty()) {
                        moveTo(axisPoints[0].x, axisPoints[0].y)
                        for (i in 1 until samples) lineTo(axisPoints[i].x, axisPoints[i].y)
                    }
                }
                drawPath(
                    path = coreFiberPath,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White, Color(0xFFE8FFFB), Color.Transparent),
                        startX = center.x - sphereRadius * 0.8f,
                        endX = center.x + sphereRadius * 0.8f
                    ),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // ─────────────────────────────────────────────────────────
                // 2C. Luminous Mint / White Hotspot Nexus
                // ─────────────────────────────────────────────────────────
                val nx = -0.0664f - motionX / k + 0.2168f
                val ny = -0.2318f + leanY * 0.06f + (-0.2394f + leanX * 0.1f) * nx +
                        0.2815f * a * nx * nx + 0.3368f * a * nx * nx * nx + 0.0333f
                val nexusCenter = Offset(center.x + nx * sphereRadius, center.y - ny * sphereRadius)
                val nexusRadius = (0.24f + 0.16f * grow) * sphereRadius * params.glow

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f * introProgress),
                            Color(0xFFE8FFFB).copy(alpha = 0.85f * introProgress),
                            Color(0xFF93F1FF).copy(alpha = 0.45f * introProgress),
                            Color.Transparent
                        ),
                        center = nexusCenter,
                        radius = nexusRadius
                    ),
                    radius = nexusRadius,
                    center = nexusCenter
                )

                // ─────────────────────────────────────────────────────────
                // 3. Inner Glass Refraction & Chromatic Fringe Rim
                // ─────────────────────────────────────────────────────────
                if (params.glass) {
                    val chromaticRimBrush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0x80FFA24C), // Amber
                            Color(0x60D95B56), // Red
                            Color(0x7093F1FF), // Cyan
                            Color(0x853569FA), // Blue
                            Color(0x50193D9B), // Deep Blue
                            Color(0x80FFA24C)  // Amber wrap
                        ),
                        center = center
                    )
                    drawCircle(
                        brush = chromaticRimBrush,
                        radius = sphereRadius - 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = (4f * params.refraction).dp.toPx())
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. Apple Specular 3D Curved Glass Crescent Reflections
            // ─────────────────────────────────────────────────────────────
            if (params.glass) {
                val rimFactor = params.rim.coerceIn(0.2f, 2.0f)
                val glassRect = androidx.compose.ui.geometry.Rect(center, sphereRadius - 1.5f.dp.toPx())

                // Primary top-left specular glass arc (Apple VisionOS look)
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f * rimFactor),
                            Color.White.copy(alpha = 0.15f * rimFactor),
                            Color.Transparent
                        ),
                        start = center - Offset(sphereRadius, sphereRadius),
                        end = center
                    ),
                    startAngle = 110f,
                    sweepAngle = 72f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Secondary soft highlight arc at bottom-right
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x6093F1FF),
                            Color.Transparent
                        ),
                        start = center + Offset(sphereRadius, sphereRadius),
                        end = center
                    ),
                    startAngle = 295f,
                    sweepAngle = 50f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Outer crisp glass boundary stroke
                val outerStrokeBrush = if (params.lightMode) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xB3FFFFFF),
                            Color(0x40CBD5E1),
                            Color(0x6694A3B8)
                        ),
                        start = center - Offset(sphereRadius, sphereRadius),
                        end = center + Offset(sphereRadius, sphereRadius)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color(0x263569FA),
                            Color(0x33FFA24C),
                            Color(0x1AFFFFFF)
                        ),
                        start = center - Offset(sphereRadius, sphereRadius),
                        end = center + Offset(sphereRadius, sphereRadius)
                    )
                }

                drawCircle(
                    brush = outerStrokeBrush,
                    radius = sphereRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }
}
