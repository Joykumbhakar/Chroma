package com.chroma.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Holds trajectory and rotation details for each individual small blast heart.
 */
data class ParticleHeartData(
    val targetOffsetX: Float, // Target X distance in Dp
    val targetOffsetY: Float, // Target Y distance in Dp
    val targetRotation: Float // Random spin in degrees (-45f to 45f)
)

/**
 * Represents a single double-tap burst event at a specific screen coordinate.
 */
data class HeartBurst(
    val id: Long = System.nanoTime() + Random.nextLong(),
    val tapOffset: Offset, // Exact pixel coordinate of the double tap
    val mainRotation: Float, // Random tilt for the big heart (-15f to 15f)
    val particles: List<ParticleHeartData>
)

/**
 * Generates a new [HeartBurst] matching the exact math from the web implementation:
 * - Main heart tilted between -15 and +15 degrees.
 * - 6 small hearts arranged in a circle with slight angular variation and 70..130dp travel radius.
 */
fun createHeartBurst(tapOffset: Offset): HeartBurst {
    val particleCount = 6
    val particles = List(particleCount) { index ->
        // Evenly spread circular angle with random jitter (-0.25 to +0.25 radians)
        val baseAngle = (2.0 * PI / particleCount) * index
        val jitter = Random.nextDouble(-0.25, 0.25)
        val angle = baseAngle + jitter

        // Travel distance between 70dp and 130dp
        val distanceDp = 70f + Random.nextFloat() * 60f

        ParticleHeartData(
            targetOffsetX = (cos(angle) * distanceDp).toFloat(),
            targetOffsetY = (sin(angle) * distanceDp).toFloat(),
            targetRotation = Random.nextFloat() * 90f - 45f
        )
    }

    return HeartBurst(
        tapOffset = tapOffset,
        mainRotation = Random.nextFloat() * 30f - 15f,
        particles = particles
    )
}

/**
 * Recreates the exact SVG heart path and #FF2D55 fill color from the HTML version
 * without requiring any external drawable resources or icon libraries.
 */
fun createHeartImageVector(fillColor: Color = Color(0xFFFF2D55)): ImageVector {
    return ImageVector.Builder(
        name = "LikeHeart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(fillColor)) {
            // Exact path: M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z
            moveTo(12f, 21.35f)
            lineToRelative(-1.45f, -1.32f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
            lineTo(12f, 21.35f)
            close()
        }
    }.build()
}

/**
 * Runs the parallel animations for the 6 small blast hearts (600ms) and the
 * main large heart (1000ms), then triggers cleanup once finished.
 */
@Composable
fun HeartBurstAnimation(
    burst: HeartBurst,
    heartVector: ImageVector,
    mainHeartSize: Dp = 90.dp,
    onAnimationFinished: (HeartBurst) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Main heart animation states
    val mainScale = remember { Animatable(0f) }
    val mainAlpha = remember { Animatable(0f) }
    val mainOffsetYDp = remember { Animatable(0f) }

    // Blast particles progress (0f to 1f over 600ms with ease-out curve)
    val blastProgress = remember { Animatable(0f) }
    val easeOutCubic = remember { CubicBezierEasing(0.0f, 0.0f, 0.58f, 1.0f) }
    val springPopEasing = remember { CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f) }

    LaunchedEffect(burst.id) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}

        coroutineScope {
            // 1. Main Heart Scale: Pops to 1.2x at 150ms, settles to 1.0x at 300ms, shrinks to 0.9x at 1000ms
            launch {
                mainScale.animateTo(
                    targetValue = 0.9f,
                    animationSpec = keyframes {
                        durationMillis = 1000
                        0f at 0 with springPopEasing
                        1.2f at 150 with FastOutSlowInEasing
                        1.0f at 300 with LinearEasing
                        1.0f at 650 with LinearEasing
                        0.9f at 1000
                    }
                )
            }

            // 2. Main Heart Alpha: Fades in by 150ms, holds until 650ms, fades out by 1000ms
            launch {
                mainAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 1000
                        0f at 0
                        1f at 150
                        1f at 650
                        0f at 1000
                    }
                )
            }

            // 3. Main Heart Float Up: Holds Y position until 650ms, then floats up by -60dp
            launch {
                mainOffsetYDp.animateTo(
                    targetValue = -60f,
                    animationSpec = keyframes {
                        durationMillis = 1000
                        0f at 0
                        0f at 650 with FastOutSlowInEasing
                        -60f at 1000
                    }
                )
            }

            // 4. Small Hearts Blast: 600ms ease-out outward trajectory
            launch {
                blastProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = easeOutCubic
                    )
                )
            }
        }

        // Remove this burst from the screen once the 1000ms animation completes
        onAnimationFinished(burst)
    }

    val smallHeartSize = (mainHeartSize * (50f / 90f))

    val mainHalfSizePx = with(density) { (mainHeartSize / 2).toPx() }
    val smallHalfSizePx = with(density) { (smallHeartSize / 2).toPx() }

    // Render the 6 small blast hearts behind the main heart (z-index equivalent)
    val currentBlast = blastProgress.value
    if (currentBlast < 1f) {
        val particleScale = 0.8f + (0.2f - 0.8f) * currentBlast // Scales from 0.8f down to 0.2f
        val particleAlpha = 1f - currentBlast                    // Fades from 1f down to 0f

        burst.particles.forEach { particle ->
            val currentTxPx = with(density) { (particle.targetOffsetX * currentBlast).dp.toPx() }
            val currentTyPx = with(density) { (particle.targetOffsetY * currentBlast).dp.toPx() }
            val currentRotation = particle.targetRotation * currentBlast

            Image(
                imageVector = heartVector,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (burst.tapOffset.x - smallHalfSizePx + currentTxPx).roundToInt(),
                            y = (burst.tapOffset.y - smallHalfSizePx + currentTyPx).roundToInt()
                        )
                    }
                    .size(smallHeartSize)
                    .graphicsLayer {
                        scaleX = particleScale
                        scaleY = particleScale
                        alpha = particleAlpha
                        rotationZ = currentRotation
                    }
            )
        }
    }

    // Render the main 90dp heart on top
    val currentMainOffsetYPx = with(density) { mainOffsetYDp.value.dp.toPx() }
    Image(
        imageVector = heartVector,
        contentDescription = null,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (burst.tapOffset.x - mainHalfSizePx).roundToInt(),
                    y = (burst.tapOffset.y - mainHalfSizePx + currentMainOffsetYPx).roundToInt()
                )
            }
            .size(mainHeartSize)
            .graphicsLayer {
                scaleX = mainScale.value
                scaleY = mainScale.value
                alpha = mainAlpha.value
                rotationZ = burst.mainRotation
            }
    )
}

/**
 * State controller for managing concurrent double-tap-to-like heart burst animations.
 */
@Stable
class DoubleTapLikeState {
    val activeBursts = mutableStateListOf<HeartBurst>()

    val isTriggered: Boolean
        get() = activeBursts.isNotEmpty()

    val tapOffset: Offset?
        get() = activeBursts.lastOrNull()?.tapOffset

    fun trigger(offset: Offset? = null) {
        val tap = offset ?: Offset.Unspecified
        activeBursts.add(createHeartBurst(tap))
    }

    internal fun finish(burst: HeartBurst) {
        activeBursts.remove(burst)
    }

    fun clear() {
        activeBursts.clear()
    }
}

@Composable
fun rememberDoubleTapLikeState(): DoubleTapLikeState {
    return remember { DoubleTapLikeState() }
}

/**
 * Overlay wrapper that binds to [DoubleTapLikeState] and renders concurrent heart bursts.
 */
@Composable
fun FlatDoubleTapHeartOverlay(
    state: DoubleTapLikeState,
    modifier: Modifier = Modifier,
    heartColor: Color = Color(0xFFFF2D55),
    heartSize: Dp = 90.dp
) {
    val heartVector = remember(heartColor) { createHeartImageVector(heartColor) }

    BoxWithConstraints(modifier = modifier) {
        val fallbackCenter = remember(constraints.maxWidth, constraints.maxHeight) {
            Offset(
                x = (constraints.maxWidth / 2).toFloat().coerceAtLeast(0f),
                y = (constraints.maxHeight / 2).toFloat().coerceAtLeast(0f)
            )
        }

        state.activeBursts.forEach { burst ->
            val resolvedBurst = remember(burst.id, fallbackCenter) {
                if (burst.tapOffset == Offset.Unspecified || (burst.tapOffset.x == 0f && burst.tapOffset.y == 0f)) {
                    burst.copy(tapOffset = fallbackCenter)
                } else {
                    burst
                }
            }

            key(burst.id) {
                HeartBurstAnimation(
                    burst = resolvedBurst,
                    heartVector = heartVector,
                    mainHeartSize = heartSize,
                    onAnimationFinished = { completedBurst ->
                        state.finish(completedBurst)
                    }
                )
            }
        }
    }
}

/**
 * Backward-compatible single-burst trigger composable.
 */
@Composable
fun FlatDoubleTapHeartAnimation(
    isTriggered: Boolean,
    modifier: Modifier = Modifier,
    tapOffset: Offset? = null,
    heartColor: Color = Color(0xFFFF2D55),
    heartSize: Dp = 90.dp,
    enableHaptics: Boolean = true,
    rotationDegrees: Float = 0f,
    onAnimationFinished: () -> Unit = {}
) {
    if (!isTriggered) return
    val heartVector = remember(heartColor) { createHeartImageVector(heartColor) }
    val burst = remember(tapOffset) {
        createHeartBurst(tapOffset ?: Offset.Zero).copy(mainRotation = rotationDegrees)
    }
    HeartBurstAnimation(
        burst = burst,
        heartVector = heartVector,
        mainHeartSize = heartSize,
        onAnimationFinished = { onAnimationFinished() }
    )
}

/**
 * All-in-one container that handles double-tap gesture detection,
 * triggers haptic feedback and displays the clean heart burst animation.
 */
@Composable
fun DoubleTapLikeContainer(
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    onSingleTap: (() -> Unit)? = null,
    heartColor: Color = Color(0xFFFF2D55),
    heartSize: Dp = 90.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberDoubleTapLikeState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onSingleTap?.invoke() },
                onDoubleTap = { offset ->
                    state.trigger(offset)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    com.chroma.studio.chat.sound.SoundManager.playReaction()
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
