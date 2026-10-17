package com.chroma.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * State controller for managing double-tap-to-like animations.
 */
@Stable
class DoubleTapLikeState {
    var isTriggered by mutableStateOf(false)
        internal set

    var tapOffset by mutableStateOf<Offset?>(null)
        internal set

    var triggerToken by mutableLongStateOf(0L)
        internal set

    fun trigger(offset: Offset? = null) {
        tapOffset = offset
        triggerToken++
        isTriggered = true
    }

    internal fun finish() {
        isTriggered = false
    }
}

@Composable
fun rememberDoubleTapLikeState(): DoubleTapLikeState {
    return remember { DoubleTapLikeState() }
}

/**
 * A simple, flat, smooth, clean and professional Double-Tap to Like Heart animation.
 *
 * Features:
 * - Mathematical cubic Bézier vector heart path (sharp, flat and crisp on any resolution).
 * - Flat vibrant crimson red with a clean 2dp high-contrast white border and ambient drop shadow.
 * - Dynamic spring pop-in (scale 0 -> 1.22 -> 1.0) with subtle organic tilt.
 * - Delicate expanding shockwave ring and 6 minimal flat sparkle dots.
 * - Gentle hold followed by smooth upward drift and dissolve exit.
 * - Built-in tactile haptic feedback.
 */
@Composable
fun FlatDoubleTapHeartAnimation(
    isTriggered: Boolean,
    modifier: Modifier = Modifier,
    tapOffset: Offset? = null,
    heartColor: Color = Color(0xFFFF2D55), // Apple System Pink/Red
    heartSize: Dp = 105.dp,
    enableHaptics: Boolean = true,
    rotationDegrees: Float = -6f,
    onAnimationFinished: () -> Unit = {}
) {
    if (!isTriggered) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val heartSizePx = with(density) { heartSize.toPx() }
    val maxUpwardDriftPx = with(density) { 26.dp.toPx() }
    val ringMaxRadiusPx = heartSizePx * 0.72f
    val particleMaxDistPx = heartSizePx * 0.65f

    // Animation drivers
    val scaleAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }
    val driftAnim = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(0f) }

    LaunchedEffect(isTriggered) {
        if (!isTriggered) return@LaunchedEffect

        if (enableHaptics) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }

        // Reset
        scaleAnim.snapTo(0f)
        alphaAnim.snapTo(0f)
        driftAnim.snapTo(0f)
        ringProgress.snapTo(0.2f)
        ringAlpha.snapTo(0.85f)
        particleProgress.snapTo(0.2f)
        particleAlpha.snapTo(1f)

        // Phase 1: Rapid spring pop-in & particle burst
        launch {
            scaleAnim.animateTo(
                targetValue = 1.22f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(1f, tween(110, easing = LinearEasing))
        }
        launch {
            ringProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        }
        launch {
            ringAlpha.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
        }
        launch {
            particleProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            particleAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }

        // Phase 2: Gentle hold
        delay(480)

        // Phase 3: Smooth float upward & dissolve
        launch {
            driftAnim.animateTo(maxUpwardDriftPx, tween(260, easing = FastOutSlowInEasing))
        }
        launch {
            scaleAnim.animateTo(1.12f, tween(260, easing = FastOutSlowInEasing))
        }
        launch {
            alphaAnim.animateTo(0f, tween(260, easing = LinearEasing))
        }

        delay(270)
        onAnimationFinished()
    }

    val heartPath = remember(heartSizePx) { createCenteredHeartPath(heartSizePx) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentAlpha = alphaAnim.value
        if (currentAlpha <= 0.001f) return@Canvas

        val w = size.width
        val h = size.height
        val centerPos = if (tapOffset != null) {
            val half = heartSizePx / 2f
            Offset(
                x = tapOffset.x.coerceIn(half + 16f, w - half - 16f),
                y = tapOffset.y.coerceIn(half + 16f, h - half - 16f)
            )
        } else {
            Offset(w / 2f, h / 2f)
        }

        val currentScale = scaleAnim.value
        val currentDrift = driftAnim.value

        // 1. Clean Shockwave Ring
        val rAlpha = ringAlpha.value * currentAlpha
        if (rAlpha > 0.005f) {
            val radius = ringMaxRadiusPx * ringProgress.value
            drawCircle(
                color = Color.White.copy(alpha = rAlpha * 0.9f),
                radius = radius,
                center = centerPos,
                style = Stroke(width = 2.2.dp.toPx())
            )
            drawCircle(
                color = heartColor.copy(alpha = rAlpha * 0.45f),
                radius = radius * 1.06f,
                center = centerPos,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // 2. Minimal Flat Burst Dots (6 geometric particles)
        val pAlpha = particleAlpha.value * currentAlpha
        if (pAlpha > 0.005f) {
            val pDist = particleMaxDistPx * particleProgress.value
            val angles = floatArrayOf(30f, 90f, 150f, 210f, 270f, 330f)
            for (ang in angles) {
                val rad = Math.toRadians(ang.toDouble()).toFloat()
                val px = centerPos.x + cos(rad) * pDist
                val py = centerPos.y + sin(rad) * pDist
                val dotRadius = (3.5.dp.toPx() * (1f - particleProgress.value * 0.45f)).coerceAtLeast(1f)

                // White dot with slight vibrant border
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = heartColor.copy(alpha = pAlpha * 0.7f),
                    radius = dotRadius * 1.35f,
                    center = Offset(px, py),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }

        // 3. Flat Heart Drawing (Centered, Scaled, Tilted, Shifted)
        val animatedCenterY = centerPos.y - currentDrift

        translate(left = centerPos.x, top = animatedCenterY) {
            rotate(degrees = rotationDegrees, pivot = Offset.Zero) {
                scale(scale = currentScale, pivot = Offset.Zero) {
                    // Soft ambient shadow
                    translate(top = 3.dp.toPx()) {
                        drawPath(
                            path = heartPath,
                            color = Color.Black.copy(alpha = 0.22f * currentAlpha)
                        )
                    }

                    // Flat solid heart fill
                    drawPath(
                        path = heartPath,
                        color = heartColor.copy(alpha = currentAlpha)
                    )

                    // Crisp flat high-contrast white border
                    drawPath(
                        path = heartPath,
                        color = Color.White.copy(alpha = 0.95f * currentAlpha),
                        style = Stroke(
                            width = 2.2.dp.toPx() / currentScale.coerceAtLeast(0.1f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Overlay wrapper that binds to [DoubleTapLikeState].
 */
@Composable
fun FlatDoubleTapHeartOverlay(
    state: DoubleTapLikeState,
    modifier: Modifier = Modifier,
    heartColor: Color = Color(0xFFFF2D55),
    heartSize: Dp = 105.dp
) {
    FlatDoubleTapHeartAnimation(
        isTriggered = state.isTriggered,
        tapOffset = state.tapOffset,
        heartColor = heartColor,
        heartSize = heartSize,
        modifier = modifier,
        onAnimationFinished = { state.finish() }
    )
}

/**
 * All-in-one container that handles double-tap gesture detection,
 * triggers haptic feedback and displays the clean flat heart animation.
 */
@Composable
fun DoubleTapLikeContainer(
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    onSingleTap: (() -> Unit)? = null,
    heartColor: Color = Color(0xFFFF2D55),
    heartSize: Dp = 105.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberDoubleTapLikeState()

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onSingleTap?.invoke() },
                onDoubleTap = { offset ->
                    state.trigger(offset)
                    onDoubleTap(offset)
                }
            )
        }
    ) {
        content()
        FlatDoubleTapHeartOverlay(
            state = state,
            modifier = Modifier.fillMaxSize(),
            heartColor = heartColor,
            heartSize = heartSize
        )
    }
}

/**
 * Canonical mathematical heart path centered at Offset.Zero.
 */
private fun createCenteredHeartPath(sizePx: Float): Path {
    val s = sizePx / 24f
    return Path().apply {
        // Bottom tip
        moveTo(0f, 9.35f * s)
        lineTo(-1.45f * s, 8.03f * s)
        cubicTo(-6.6f * s, 3.36f * s, -10f * s, 0.28f * s, -10f * s, -3.5f * s)
        cubicTo(-10f * s, -6.58f * s, -7.58f * s, -9f * s, -4.5f * s, -9f * s)
        cubicTo(-2.76f * s, -9f * s, -1.09f * s, -8.19f * s, 0f, -6.91f * s)
        cubicTo(1.09f * s, -8.19f * s, 2.76f * s, -9f * s, 4.5f * s, -9f * s)
        cubicTo(7.58f * s, -9f * s, 10f * s, -6.58f * s, 10f * s, -3.5f * s)
        cubicTo(10f * s, 0.28f * s, 6.6f * s, 3.36f * s, 1.45f * s, 8.04f * s)
        lineTo(0f, 9.35f * s)
        close()
    }
}
