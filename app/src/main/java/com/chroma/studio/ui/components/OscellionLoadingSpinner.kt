package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlin.math.*

private const val RADIUS = 420f
private const val CIRCUMFERENCE = 2638.9378f // 2 * PI * 420
private const val TICK_LENGTH = 910.75f
private const val PORTAL_START_DEG = 339.87f // -20.13°
private const val OPEN_RING_SWEEP_DEG = 332f // 28° open gap with rounded caps
private const val OPEN_RING_DASH = 2433.69f // (332 / 360) * CIRCUMFERENCE

private enum class LoaderPhase {
    LOADING,
    MORPH_TO_CHECK,
    CHECKED,
    MORPH_TO_LOAD
}

/**
 * Oscellion — Minimal Flat Loader & Overflowed Checkmark Morph.
 * Exact Compose implementation of:
 * e:\Design & 3D Assets\oscellion_loader_to_overflowed_checkmark_morph.html
 *
 * Visual Components:
 * 1. Flat subtle background track (circle r=420, stroke=44, color=#1F1F23)
 * 2. Flat pure white rotating arc with Material/Apple breathing ease
 * 3. Extruded 3D Oscellion emblem in the center with 1:1 synchronized rotation & dynamic lighting
 * 4. Overflowed Checkmark Morph: Arc smoothly glides into 332° open ring framing the tick portal,
 *    3D emblem twists and dissolves into tick pivot, tick sweeps kinetically past the rim,
 *    followed by tactile micro-pop and concentric echo pulse!
 */
@Composable
fun OscellionMorphLoader(
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    color: Color = Color.White,
    trackColor: Color = Color(0xFF1F1F23),
    checkColor: Color = Color.White,
    spinDurationMs: Int = 2800,
    morphToCheckMs: Int = 920,
    morphToLoadMs: Int = 650,
    onCompleted: (() -> Unit)? = null
) {
    var phase by remember { mutableStateOf(if (isCompleted) LoaderPhase.CHECKED else LoaderPhase.LOADING) }
    var phaseElapsedMs by remember { mutableStateOf(0f) }
    var loadProgress by remember { mutableStateOf(0f) }

    // Snapshot state for zero-jump morph initiation
    var snapArcStartDeg by remember { mutableStateOf(0f) }
    var snapArcSweepDeg by remember { mutableStateOf(30f) }
    var snapLogoYDeg by remember { mutableStateOf(0f) }
    var snapTargetStartDeg by remember { mutableStateOf(PORTAL_START_DEG) }
    var snapTargetLogoYDeg by remember { mutableStateOf(360f) }

    // React to external isCompleted state changes
    LaunchedEffect(isCompleted) {
        if (isCompleted && (phase == LoaderPhase.LOADING || phase == LoaderPhase.MORPH_TO_LOAD)) {
            // Capture exact live loading geometry
            val live = getLoadingArcGeometry(loadProgress)
            snapArcStartDeg = live.startDeg
            snapArcSweepDeg = live.sweepDeg
            snapLogoYDeg = live.logoYDeg

            var deltaDeg = ((PORTAL_START_DEG - live.startDeg) % 360f + 360f) % 360f
            if (deltaDeg < 75f) deltaDeg += 360f
            snapTargetStartDeg = live.startDeg + deltaDeg
            snapTargetLogoYDeg = 360f

            phase = LoaderPhase.MORPH_TO_CHECK
            phaseElapsedMs = 0f
        } else if (!isCompleted && (phase == LoaderPhase.CHECKED || phase == LoaderPhase.MORPH_TO_CHECK)) {
            phase = LoaderPhase.MORPH_TO_LOAD
            phaseElapsedMs = 0f
        }
    }

    // High precision frame-based master clock
    var lastFrameTimeNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(phase) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dtMs = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000f).coerceAtMost(50f)

                    when (phase) {
                        LoaderPhase.LOADING -> {
                            loadProgress += dtMs / spinDurationMs.toFloat()
                        }
                        LoaderPhase.MORPH_TO_CHECK -> {
                            phaseElapsedMs += dtMs
                            if (phaseElapsedMs >= morphToCheckMs) {
                                phase = LoaderPhase.CHECKED
                                phaseElapsedMs = 0f
                                onCompleted?.invoke()
                            }
                        }
                        LoaderPhase.CHECKED -> {
                            phaseElapsedMs += dtMs
                        }
                        LoaderPhase.MORPH_TO_LOAD -> {
                            phaseElapsedMs += dtMs
                            if (phaseElapsedMs >= morphToLoadMs) {
                                phase = LoaderPhase.LOADING
                                loadProgress = 0f
                                phaseElapsedMs = 0f
                            }
                        }
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    // Parsed official Oscellion brand mark geometry
    val emblemPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    // Parsed overflowed checkmark tick geometry
    val tickPath = remember {
        PathParser().parsePathString("M 308 508 L 468 668 L 952 184").toPath()
    }

    // Current rendering attributes derived from phase & clocks
    val liveGeometry = getLoadingArcGeometry(loadProgress)

    val currentStartDeg: Float
    val currentSweepDeg: Float
    val currentRingColor: Color
    val trackSweepDeg: Float
    val trackOpacity: Float

    val logoYDeg: Float
    val logoZDeg: Float
    val logoScale: Float
    val logoTx: Float
    val logoTy: Float
    val logoAlpha: Float

    val tickDrawProgress: Float
    val tickScale: Float
    val tickAlpha: Float

    val masterScale: Float
    val echoScale: Float
    val echoAlpha: Float

    when (phase) {
        LoaderPhase.LOADING -> {
            currentStartDeg = liveGeometry.startDeg
            currentSweepDeg = liveGeometry.sweepDeg
            currentRingColor = color
            trackSweepDeg = 360f
            trackOpacity = 1f

            logoYDeg = liveGeometry.logoYDeg
            logoZDeg = 0f
            logoScale = 1f
            logoTx = 0f
            logoTy = 0f
            logoAlpha = 1f

            tickDrawProgress = 0f
            tickScale = 0.9f
            tickAlpha = 0f

            masterScale = 1f
            echoScale = 1f
            echoAlpha = 0f
        }

        LoaderPhase.MORPH_TO_CHECK -> {
            val t = (phaseElapsedMs / morphToCheckMs.toFloat()).coerceIn(0f, 1f)

            // 1. Outer Ring Morph (0.00 -> 0.62)
            val tRing = easeOutQuart((t / 0.62f).coerceIn(0f, 1f))
            currentStartDeg = lerp(snapArcStartDeg, snapTargetStartDeg, tRing)
            currentSweepDeg = lerp(snapArcSweepDeg, OPEN_RING_SWEEP_DEG, tRing)
            val colorT = easeInOutCubic(((t - 0.12f) / 0.55f).coerceIn(0f, 1f))
            currentRingColor = lerpColor(color, checkColor, colorT)

            // Background track opening portal & receding
            val tTrack = easeOutCubic((t / 0.48f).coerceIn(0f, 1f))
            trackSweepDeg = lerp(360f, OPEN_RING_SWEEP_DEG, tTrack)
            trackOpacity = lerp(1f, 0f, easeInCubic(tTrack))

            // 2. 3D Oscellion Emblem Kinetic Morph-Out (0.00 -> 0.42)
            val tLogo = (t / 0.42f).coerceIn(0f, 1f)
            val tLogoEase = easeInOutCubic(tLogo)
            logoYDeg = lerp(snapLogoYDeg, snapTargetLogoYDeg, easeOutCubic(tLogo))
            logoZDeg = lerp(0f, -34f, easeInCubic(tLogo))
            logoScale = lerp(1f, 0.28f, easeInCubic(tLogo))
            logoTx = lerp(0f, -30f, tLogoEase)
            logoTy = lerp(0f, 46f, tLogoEase)
            logoAlpha = 1f - easeInCubic(((tLogo - 0.18f) / 0.82f).coerceIn(0f, 1f))

            // 3. Overflowed Checkmark Kinetic Draw (0.28 -> 0.86)
            val tTickRaw = ((t - 0.28f) / 0.58f).coerceIn(0f, 1f)
            tickDrawProgress = easeOutExpo(tTickRaw)
            tickAlpha = (tTickRaw * 5f).coerceIn(0f, 1f)
            tickScale = if (tTickRaw < 0.72f) {
                lerp(0.88f, 1.035f, easeOutCubic(tTickRaw / 0.72f))
            } else {
                lerp(1.035f, 1.0f, easeInOutCubic((tTickRaw - 0.72f) / 0.28f))
            }

            // 4. Tactile Micro-Pop & Echo Pulse (0.58 -> 1.00)
            val tPop = ((t - 0.58f) / 0.42f).coerceIn(0f, 1f)
            masterScale = 1f + sin(tPop * Math.PI.toFloat()) * 0.032f * (1f - tPop * 0.35f)

            if (t > 0.55f) {
                val tEcho = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                echoScale = lerp(1.0f, 1.14f, easeOutCubic(tEcho))
                echoAlpha = lerp(0.32f, 0f, easeOutCubic(tEcho))
            } else {
                echoScale = 1.0f
                echoAlpha = 0f
            }
        }

        LoaderPhase.CHECKED -> {
            currentStartDeg = PORTAL_START_DEG
            currentSweepDeg = OPEN_RING_SWEEP_DEG
            currentRingColor = checkColor
            trackSweepDeg = OPEN_RING_SWEEP_DEG
            trackOpacity = 0f

            logoYDeg = 0f
            logoZDeg = 0f
            logoScale = 0.28f
            logoTx = 0f
            logoTy = 0f
            logoAlpha = 0f

            tickDrawProgress = 1f
            tickScale = 1f
            tickAlpha = 1f

            masterScale = 1f
            echoScale = 1f
            echoAlpha = 0f
        }

        LoaderPhase.MORPH_TO_LOAD -> {
            val t = (phaseElapsedMs / morphToLoadMs.toFloat()).coerceIn(0f, 1f)
            currentRingColor = lerpColor(checkColor, color, easeInOutCubic(t))

            // Checkmark cleanly retracts back into center
            val tTickOut = (t / 0.45f).coerceIn(0f, 1f)
            tickDrawProgress = 1f - easeInCubic(tTickOut)
            tickScale = lerp(1f, 0.82f, tTickOut)
            tickAlpha = 1f - easeInCubic(tTickOut)

            // Open ring contracts smoothly into the starting loading arc
            val tRingBack = easeInOutCubic(((t - 0.1f) / 0.9f).coerceIn(0f, 1f))
            currentStartDeg = lerp(PORTAL_START_DEG, 360f, tRingBack)
            currentSweepDeg = lerp(OPEN_RING_SWEEP_DEG, (220f / CIRCUMFERENCE) * 360f, tRingBack)

            trackSweepDeg = lerp(OPEN_RING_SWEEP_DEG, 360f, tRingBack)
            trackOpacity = tRingBack

            // 3D Emblem blooms back out into center
            val tLogoIn = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val tLogoEase = easeOutCubic(tLogoIn)
            logoYDeg = lerp(-40f, 0f, tLogoEase)
            logoZDeg = lerp(24f, 0f, tLogoEase)
            logoScale = lerp(0.35f, 1f, tLogoEase)
            logoTx = 0f
            logoTy = 0f
            logoAlpha = tLogoEase

            masterScale = 1f
            echoScale = 1f
            echoAlpha = 0f
        }
    }

    Box(
        modifier = modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.minDimension / 1024f
            val offsetX = (size.width - 1024f * scale) / 2f
            val offsetY = (size.height - 1024f * scale) / 2f

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)

                // Master scale for tactile micro-pop
                if (masterScale != 1f) {
                    canvas.scale(masterScale, masterScale, 512f, 512f)
                }

                // ─── 0. OPEN-RING ECHO PULSE ─────────────────────────
                if (echoAlpha > 0f) {
                    val echoRadius = RADIUS * echoScale
                    drawArc(
                        color = currentRingColor.copy(alpha = echoAlpha),
                        startAngle = PORTAL_START_DEG,
                        sweepAngle = OPEN_RING_SWEEP_DEG,
                        useCenter = false,
                        topLeft = Offset(512f - echoRadius, 512f - echoRadius),
                        size = Size(echoRadius * 2f, echoRadius * 2f),
                        style = Stroke(width = 24f, cap = StrokeCap.Round)
                    )
                }

                // ─── 1. FLAT SUBTLE BACKGROUND TRACK ─────────────────
                if (trackOpacity > 0f) {
                    drawArc(
                        color = trackColor.copy(alpha = trackOpacity),
                        startAngle = PORTAL_START_DEG,
                        sweepAngle = trackSweepDeg,
                        useCenter = false,
                        topLeft = Offset(512f - RADIUS, 512f - RADIUS),
                        size = Size(RADIUS * 2f, RADIUS * 2f),
                        style = Stroke(width = 44f, cap = StrokeCap.Round)
                    )
                }

                // ─── 2. ACTIVE ROTATING ARC / OPEN PORTAL RING ───────
                drawArc(
                    color = currentRingColor,
                    startAngle = currentStartDeg,
                    sweepAngle = currentSweepDeg,
                    useCenter = false,
                    topLeft = Offset(512f - RADIUS, 512f - RADIUS),
                    size = Size(RADIUS * 2f, RADIUS * 2f),
                    style = Stroke(width = 44f, cap = StrokeCap.Round)
                )

                // ─── 3. EXTRUDED 3D OSCELLION INNER EMBLEM ────────────
                if (logoAlpha > 0f && logoScale > 0.01f) {
                    canvas.save()
                    canvas.translate(logoTx, logoTy)
                    if (logoZDeg != 0f) {
                        canvas.rotate(logoZDeg, 512f, 512f)
                    }

                    // 3D Coin Rotation via horizontal cosine scaling & physical thickness slices
                    val rad = Math.toRadians(logoYDeg.toDouble()).toFloat()
                    val cosY = cos(rad)
                    val sinY = sin(rad)

                    val emblemScaleFactor = 0.56f * logoScale
                    val frontFaceColor = getLogoShading(logoYDeg).copy(alpha = logoAlpha)

                    // Draw depth thickness slices when angled
                    if (abs(cosY) < 0.98f) {
                        val depthLevels = listOf(
                            -26f to Color(0xFFD4D4D8),
                            -19.5f to Color(0xFF52525B),
                            -13f to Color(0xFF71717A),
                            -6.5f to Color(0xFFA1A1AA)
                        )
                        for ((depthZ, sliceColor) in depthLevels) {
                            val sliceShiftX = sinY * depthZ * 0.45f
                            canvas.save()
                            canvas.translate(512f + sliceShiftX, 512f)
                            canvas.scale(cosY * emblemScaleFactor, emblemScaleFactor)
                            canvas.translate(-492f, -512f)
                            canvas.drawPath(
                                emblemPath,
                                Paint().apply {
                                    isAntiAlias = true
                                    style = PaintingStyle.Fill
                                    this.color = sliceColor.copy(alpha = logoAlpha * 0.9f)
                                }
                            )
                            canvas.restore()
                        }
                    }

                    // Front face
                    canvas.save()
                    canvas.translate(512f, 512f)
                    canvas.scale(cosY * emblemScaleFactor, emblemScaleFactor)
                    canvas.translate(-492f, -512f)
                    canvas.drawPath(
                        emblemPath,
                        Paint().apply {
                            isAntiAlias = true
                            style = PaintingStyle.Fill
                            this.color = frontFaceColor
                        }
                    )
                    canvas.restore()

                    canvas.restore()
                }

                // ─── 4. OVERFLOWED CHECKMARK TICK ────────────────────
                if (tickAlpha > 0f && tickDrawProgress > 0f) {
                    canvas.save()
                    if (tickScale != 1f) {
                        canvas.scale(tickScale, tickScale, 512f, 512f)
                    }

                    val tickPaint = Paint().apply {
                        isAntiAlias = true
                        style = PaintingStyle.Stroke
                        strokeWidth = 54f
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        this.color = currentRingColor.copy(alpha = tickAlpha)
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(TICK_LENGTH, TICK_LENGTH),
                            TICK_LENGTH * (1f - tickDrawProgress)
                        )
                    }
                    canvas.drawPath(tickPath, tickPaint)
                    canvas.restore()
                }

                canvas.restore()
            }
        }
    }
}

/**
 * Backwards-compatible drop-in replacement for OscellionLoadingSpinner.
 * Uses the exact flat loader & overflowed checkmark morph system.
 */
@Composable
fun OscellionLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color? = null,
    innerColor: Color? = null,
    strokeWidth: Dp? = null,
    isCompleted: Boolean = false,
    checkColor: Color = Color.White,
    onCompleted: (() -> Unit)? = null
) {
    val isDark = LocalOscellionColors.current.isDark
    val spinnerColor = color ?: if (isDark) Color.White else Color(0xFF1C1C1E)

    OscellionMorphLoader(
        modifier = modifier,
        isCompleted = isCompleted,
        color = spinnerColor,
        checkColor = checkColor,
        onCompleted = onCompleted
    )
}

// ─── MATH & INTERPOLATION HELPERS ──────────────────────────────────────

private class ArcGeometry(
    val startDeg: Float,
    val sweepDeg: Float,
    val logoYDeg: Float
)

private fun getLoadingArcGeometry(cycleProgress: Float): ArcGeometry {
    val u = ((cycleProgress % 1f) + 1f) % 1f
    val rotDeg = u * 360f

    val dashLen: Float
    val dashOffset: Float
    if (u < 0.5f) {
        val k = easeInOutSine(u * 2f)
        dashLen = lerp(220f, 1550f, k)
        dashOffset = lerp(0f, -520f, k)
    } else {
        val k = easeInOutSine((u - 0.5f) * 2f)
        dashLen = lerp(1550f, 220f, k)
        dashOffset = lerp(-520f, -CIRCUMFERENCE, k)
    }

    val offsetDeg = (-dashOffset / CIRCUMFERENCE) * 360f
    val startDeg = (rotDeg + offsetDeg) % 360f
    val sweepDeg = (dashLen / CIRCUMFERENCE) * 360f

    return ArcGeometry(
        startDeg = startDeg,
        sweepDeg = sweepDeg,
        logoYDeg = u * 360f
    )
}

private fun getLogoShading(deg: Float): Color {
    val rad = Math.toRadians(deg.toDouble()).toFloat()
    val c = cos(rad)
    val v = if (c >= 0f) {
        lerp(161f, 255f, c).roundToInt().coerceIn(0, 255)
    } else {
        lerp(161f, 228f, -c).roundToInt().coerceIn(0, 255)
    }
    return Color(v, v, min(255, v + if (c < 0.2f) 6 else 0))
}

private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
    val r = lerp(c1.red, c2.red, t)
    val g = lerp(c1.green, c2.green, t)
    val b = lerp(c1.blue, c2.blue, t)
    val a = lerp(c1.alpha, c2.alpha, t)
    return Color(r, g, b, a)
}

private fun easeInOutSine(t: Float): Float =
    -(cos(Math.PI * t.toDouble()).toFloat() - 1f) / 2f

private fun easeInOutCubic(t: Float): Float =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

private fun easeOutCubic(t: Float): Float =
    1f - (1f - t).pow(3)

private fun easeOutQuart(t: Float): Float =
    1f - (1f - t).pow(4)

private fun easeInCubic(t: Float): Float =
    t * t * t

private fun easeOutExpo(t: Float): Float =
    if (t == 1f) 1f else 1f - 2f.toDouble().pow(-10 * t.toDouble()).toFloat()

private fun lerp(a: Float, b: Float, t: Float): Float =
    a + (b - a) * t
