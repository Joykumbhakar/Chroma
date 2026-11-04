package com.chroma.studio.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chroma.studio.chat.sound.SoundManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================
// APPLE BLUE GLASS TOGGLE DESIGN TOKENS (Matched 1:1 with Reference File)
// ============================================================================

val ToggleStageCenter = Color(0xFFDBEAFE)
val ToggleStageMid = Color(0xFFEFF6FF)
val ToggleStageEdge = Color(0xFFE2E8F0)

// ON State: Clean, Luminous Apple iOS Blue Liquid Glass Stops
val OnGlassTop = Color(0xDC3882F6)
val OnGlassUpperMid = Color(0xD62970EC)
val OnGlassLowerMid = Color(0xDC1E5CE0)
val OnGlassBottom = Color(0xE31549C6)
val OnSapphireCrown = Color(0x807CC0FF)
val OnBottomCaustic = Color(0x73529BFF)
val OnContactShadow = Color(0x360E369E)
val OnLowerRefraction = Color(0x8C99CCFF)
val OnInnerBottomTint = Color(0x4D0E369E)

// OFF State: Frosted Translucent Cool Slate Glass Stops
val OffGlassTop = Color(0x8F94A3B8)
val OffGlassUpperMid = Color(0x8A7D8FA8)
val OffGlassLowerMid = Color(0x9464748B)
val OffGlassBottom = Color(0xA3475569)
val OffCrown = Color(0x61E2E8F0)
val OffBottomCaustic = Color(0x47BACCE2)
val OffContactShadow = Color(0x2E1E293B)
val OffLowerRefraction = Color(0x73CBD5E1)
val OffInnerBottomTint = Color(0x38334155)

// Shared Specular & Thumb Tokens
val TrackSpecularTop = Color(0xF2FFFFFF)
val TrackUpperSheen = Color(0x52FFFFFF)
val ThumbDropShadowPrimary = Color(0x420E369E)
val ThumbInnerRefraction = Color(0x6B87BCFF)

// Spring Physics Constants
private const val TOGGLE_STIFFNESS = 280f
private const val TOGGLE_DAMPING = 20f
private const val HOLD_STIFFNESS = 320f
private const val HOLD_DAMPING = 22f
private const val PRESS_STIFFNESS = 340f
private const val PRESS_DAMPING = 20f

/**
 * Full-screen preview container centering the Apple-style Blue Glass Toggle.
 */
@Composable
fun AppleGlassToggleScreen() {
    var checked by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(ToggleStageCenter, ToggleStageMid, ToggleStageEdge),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AppleGlassToggle(
            checked = checked,
            onCheckedChange = { checked = it },
            trackWidth = 148.dp,
            trackHeight = 76.dp,
            showIoLabels = true
        )
    }
}

/**
 * Apple-style Blue Liquid Glass Toggle Switch with:
 * - Zero outer side glow (tight ground-contact shadow only)
 * - Clean luminous iOS Blue liquid glass shaders (`#3882F6` -> `#1549C6`)
 * - ODE spring physics with velocity-based track squash & stretch
 * - Authentic iOS liquid thumb stretch (widens into a pill on press/drag or high velocity,
 *   and settles back into a strict 1:1 circle at rest)
 * - Dynamic specular shimmer sweep and etched frosted glass `I` / `O` micro-glyphs
 * - Universal dynamic scaling across standard setting rows (52dp x 31dp) and showcase sizes (148dp x 76dp)
 */
@Composable
fun AppleGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 31.dp,
    showIoLabels: Boolean = false
) {
    val density = LocalDensity.current
    val view = LocalView.current

    // Geometry scaling based on track dimensions
    val scaleFactor = trackHeight.value / 76f
    val paddingDp = (trackHeight * (6f / 76f)).coerceIn(2.dp, 6.dp)
    val thumbHeightDp = trackHeight - paddingDp * 2
    val thumbBaseWidthDp = if (trackWidth <= 64.dp) thumbHeightDp else (trackWidth * (78f / 148f))
    val thumbMaxStretchDp = (trackWidth * 0.11f).coerceIn(3.5.dp, 16.dp)

    // 1. Toggle Morph Spring (0f = OFF, 1f = ON)
    var togglePos by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var toggleVel by remember { mutableFloatStateOf(0f) }
    var dragOverrideTarget by remember { mutableStateOf<Float?>(null) }
    val toggleTarget = dragOverrideTarget ?: if (checked) 1f else 0f

    // 2. Thumb Press-Hold Liquid Stretch Spring (0f = 1:1 Circle, 1f = Stretched Pill)
    var holdPos by remember { mutableFloatStateOf(0f) }
    var holdVel by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val holdTarget = if (isPressed) 1f else 0f

    // 3. Track Tactile Bounce Spring
    var pressScale by remember { mutableFloatStateOf(1f) }
    var pressVel by remember { mutableFloatStateOf(0f) }
    val pressTarget = 1f

    var impulseTrigger by remember { mutableIntStateOf(0) }

    fun triggerBounce(impulse: Float) {
        pressVel += impulse * 18f
        impulseTrigger++
    }

    // Unified 60/120fps ODE Spring Physics Loop
    LaunchedEffect(toggleTarget, holdTarget, impulseTrigger) {
        var lastTimeNanos = withFrameNanos { it }
        var isSettled = false

        while (!isSettled) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.032f)
                lastTimeNanos = nowNanos

                // 1. Toggle Position Spring ODE
                val tForce = -TOGGLE_STIFFNESS * (togglePos - toggleTarget) - TOGGLE_DAMPING * toggleVel
                toggleVel += tForce * dt
                togglePos += toggleVel * dt

                // 2. Thumb Hold Stretch Spring ODE
                val hForce = -HOLD_STIFFNESS * (holdPos - holdTarget) - HOLD_DAMPING * holdVel
                holdVel += hForce * dt
                holdPos += holdVel * dt

                // 3. Track Tactile Press Bounce Spring ODE
                val pForce = -PRESS_STIFFNESS * (pressScale - pressTarget) - PRESS_DAMPING * pressVel
                pressVel += pForce * dt
                pressScale += pressVel * dt

                val isToggleSettled = abs(togglePos - toggleTarget) < 0.001f && abs(toggleVel) < 0.005f
                val isHoldSettled = abs(holdPos - holdTarget) < 0.001f && abs(holdVel) < 0.005f
                val isPressSettled = abs(pressScale - pressTarget) < 0.001f && abs(pressVel) < 0.005f

                if (isToggleSettled && isHoldSettled && isPressSettled) {
                    togglePos = toggleTarget
                    toggleVel = 0f
                    holdPos = holdTarget
                    holdVel = 0f
                    pressScale = pressTarget
                    pressVel = 0f
                    isSettled = true
                }
            }
        }
    }

    val clampedFraction = togglePos.coerceIn(0f, 1f)

    // Track velocity-based squash and stretch
    val stretchX = (1f + abs(toggleVel) * 0.012f) * pressScale
    val squashY = (1f - abs(toggleVel) * 0.009f) * pressScale

    // Apple iOS Liquid Thumb Stretch: wider capsule knob at rest, stretches further on press/velocity
    val velocityStretch = (abs(toggleVel) * 0.14f).coerceIn(0f, 1f)
    val pillFactor = max(holdPos, velocityStretch).coerceIn(0f, 1f)
    val currentThumbWidthDp = thumbBaseWidthDp + (thumbMaxStretchDp * pillFactor)

    val maxTravelPx = with(density) {
        (trackWidth - paddingDp * 2 - currentThumbWidthDp).toPx().coerceAtLeast(0f)
    }
    val maxOvershootPx = with(density) { (2.5f * scaleFactor).dp.toPx() }
    val thumbOffsetX = (maxTravelPx * togglePos)
        .coerceIn(-maxOvershootPx, maxTravelPx + maxOvershootPx)
        .roundToInt()

    val thumbScaleY = 1f - pillFactor * 0.035f

    // Etched "I" and "O" Micro-Glyphs Spring Bloom
    val onGlyphProgress = ((clampedFraction - 0.25f) / 0.75f).coerceIn(0f, 1f)
    val offGlyphProgress = ((0.75f - clampedFraction) / 0.75f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .graphicsLayer {
                scaleX = stretchX
                scaleY = squashY
            }
            .appleGlassToggleTrackSurface(progress = clampedFraction)
            .clip(CircleShape)
            .pointerInput(checked, maxTravelPx) {
                val trackWidthPx = size.width.toFloat()
                val thumbBaseWidthPx = thumbBaseWidthDp.toPx()
                val touchSlopPx = 8.dp.toPx()
                val dragSlopPx = 3.dp.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    isPressed = true
                    var hasDragged = false
                    var isCancelled = false

                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    } catch (_: Exception) {}
                    triggerBounce(-0.06f)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val currentX = change.position.x
                        val currentY = change.position.y
                        val dx = currentX - startX
                        val dy = currentY - startY

                        // Clean cancellation if the user is scrolling a parent list vertically
                        if (!hasDragged && !isCancelled) {
                            if (abs(dy) > touchSlopPx && abs(dy) > abs(dx) * 1.3f) {
                                isCancelled = true
                                isPressed = false
                                break
                            } else if (abs(dx) > dragSlopPx) {
                                hasDragged = true
                            }
                        }

                        if (hasDragged) {
                            val travelDistancePx = (trackWidthPx - thumbBaseWidthPx).coerceAtLeast(1f)
                            val fraction = ((currentX - thumbBaseWidthPx / 2f) / travelDistancePx)
                                .coerceIn(0f, 1f)
                            dragOverrideTarget = fraction
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    isPressed = false

                    if (!isCancelled) {
                        triggerBounce(-0.09f)

                        val finalChecked = if (hasDragged) {
                            (dragOverrideTarget ?: clampedFraction) >= 0.5f
                        } else {
                            !checked
                        }
                        dragOverrideTarget = null

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val feedback = if (finalChecked) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
                                if (!view.performHapticFeedback(feedback)) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        } catch (_: Exception) {}

                        try {
                            SoundManager.playReaction()
                        } catch (_: Exception) {}

                        onCheckedChange(finalChecked)
                    }
                    dragOverrideTarget = null
                }
            }
            .semantics {
                role = Role.Switch
                contentDescription = "Apple Blue Glass Toggle"
                stateDescription = if (checked) "On" else "Off"
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // ====================================================================
        // ETCHED GLASS "I" INDICATOR (ON State - Left Side)
        // ====================================================================
        if (showIoLabels && onGlyphProgress > 0.001f) {
            val onScale = 0.55f + 0.45f * onGlyphProgress
            val barStartPadding = (trackWidth * 0.19f).coerceIn(8.dp, 28.dp)
            val barWidth = (2.dp * scaleFactor).coerceIn(1.5.dp, 6.dp)
            val barHeight = (trackHeight * 0.32f).coerceIn(8.dp, 22.dp)

            Canvas(
                modifier = Modifier
                    .padding(start = barStartPadding)
                    .size(width = barWidth, height = barHeight)
                    .graphicsLayer {
                        alpha = onGlyphProgress
                        scaleX = onScale
                        scaleY = onScale
                    }
            ) {
                val strokeW = barWidth.toPx()
                drawLine(
                    color = OnContactShadow,
                    start = Offset(size.width / 2f, 1.2.dp.toPx()),
                    end = Offset(size.width / 2f, size.height + 1.2.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }

        // ====================================================================
        // ETCHED GLASS "O" INDICATOR (OFF State - Right Side)
        // ====================================================================
        if (showIoLabels && offGlyphProgress > 0.001f) {
            val offScale = 0.55f + 0.45f * offGlyphProgress
            val ringEndPadding = (trackWidth * 0.15f).coerceIn(6.dp, 22.dp)
            val ringSize = (trackHeight * 0.28f).coerceIn(7.dp, 18.dp)

            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ringEndPadding)
                    .size(ringSize)
                    .graphicsLayer {
                        alpha = offGlyphProgress
                        scaleX = offScale
                        scaleY = offScale
                    }
            ) {
                val strokeW = (2.dp * scaleFactor).toPx().coerceIn(1.5.dp.toPx(), 2.4.dp.toPx())
                drawCircle(
                    color = OffContactShadow,
                    radius = (size.minDimension - strokeW) / 2f,
                    center = center + Offset(0f, 1.dp.toPx()),
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.80f),
                    radius = (size.minDimension - strokeW) / 2f,
                    center = center,
                    style = Stroke(width = strokeW)
                )
            }
        }

        // ====================================================================
        // SCULPTED APPLE LIQUID PEARL-GLASS THUMB KNOB
        // ====================================================================
        Box(
            modifier = Modifier
                .padding(start = paddingDp)
                .offset { IntOffset(thumbOffsetX, 0) }
                .width(currentThumbWidthDp)
                .height(thumbHeightDp)
                .graphicsLayer {
                    scaleY = thumbScaleY
                }
                .appleGlassThumbSurface(progress = clampedFraction)
        )
    }
}

// ============================================================================
// CUSTOM APPLE GLASS TRACK & THUMB RENDERERS (Zero-Allocation drawWithCache)
// ============================================================================

/**
 * Renders the Apple Blue Glass Track surface with zero outer side glow:
 * - Smoothly interpolates between Frosted Slate Glass (0f) and Clean Apple iOS Blue Glass (1f)
 * - Tight under-track ground contact shadow inset horizontally
 * - Top radial crown + bottom caustic lens glow + dynamic specular shimmer sweep
 * - True boolean `Path.Op.DIFFERENCE` inner specular rims and curved top meniscus glaze
 */
fun Modifier.appleGlassToggleTrackSurface(
    progress: Float
): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height
    val radiusPx = min(w, h) / 2f
    val t = progress.coerceIn(0f, 1f)

    // Interpolated Track Colors
    val topColor = lerp(OffGlassTop, OnGlassTop, t)
    val uMidColor = lerp(OffGlassUpperMid, OnGlassUpperMid, t)
    val lMidColor = lerp(OffGlassLowerMid, OnGlassLowerMid, t)
    val botColor = lerp(OffGlassBottom, OnGlassBottom, t)
    val crownColor = lerp(OffCrown, OnSapphireCrown, t)
    val causticColor = lerp(OffBottomCaustic, OnBottomCaustic, t)
    val shadowColor = lerp(OffContactShadow, OnContactShadow, t)
    val lowerRimColor = lerp(OffLowerRefraction, OnLowerRefraction, t)
    val innerTintColor = lerp(OffInnerBottomTint, OnInnerBottomTint, t)

    // Tight under-track ground contact shadow
    val shadowInsetX = (w * 0.068f).coerceIn(2.dp.toPx(), 10.dp.toPx())
    val shadowOffsetY = (h * 0.105f).coerceIn(2.dp.toPx(), 8.dp.toPx())
    val shadowBlur = (h * 0.092f).coerceIn(2.dp.toPx(), 7.dp.toPx())

    val contactShadowPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = shadowColor.toArgb()
        maskFilter = BlurMaskFilter(shadowBlur, BlurMaskFilter.Blur.NORMAL)
    }

    val pillRect = RectF(0f, 0f, w, h)
    val pillPath = NativePath().apply {
        addRoundRect(pillRect, radiusPx, radiusPx, NativePath.Direction.CW)
    }

    val linearTrackPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                topColor.toArgb(),
                uMidColor.toArgb(),
                lMidColor.toArgb(),
                botColor.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    val radialCrownPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = RadialGradient(
            w * 0.5f,
            0f,
            max(w * 0.68f, h),
            intArrayOf(
                crownColor.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.78f),
            Shader.TileMode.CLAMP
        )
    }

    val bottomCausticPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = RadialGradient(
            w * 0.5f,
            h * 1.05f,
            max(w * 0.55f, h * 0.75f),
            intArrayOf(
                causticColor.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.72f),
            Shader.TileMode.CLAMP
        )
    }

    // Dynamic Specular Shimmer Sweep driven by toggle progress
    val shimmerCenterX = w * (0.18f + 0.64f * t)
    val shimmerWidth = max(w * 0.42f, 20.dp.toPx())
    val shimmerPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            shimmerCenterX - shimmerWidth, 0f,
            shimmerCenterX + shimmerWidth, h,
            intArrayOf(
                Color.Transparent.toArgb(),
                Color(0x2BFFFFFF).toArgb(),
                Color(0x4780B8FF).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Pre-computed Boolean Difference Paths for True Inset Refractions
    val pad = 30.dp.toPx()
    val outerBoundsPath = NativePath().apply {
        addRect(-pad, -pad, w + pad, h + pad, NativePath.Direction.CW)
    }

    val bottomDepthInset = NativePath().apply {
        val hole = NativePath().apply {
            val depthShift = (h * 0.13f).coerceIn(3.dp.toPx(), 10.dp.toPx())
            addRoundRect(
                RectF(0f, -depthShift, w, h - depthShift),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val upperSheenInset = NativePath().apply {
        val hole = NativePath().apply {
            val sheenShift = (h * 0.18f).coerceIn(4.dp.toPx(), 14.dp.toPx())
            addRoundRect(
                RectF(0f, sheenShift, w, h + sheenShift),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val topRimInset = NativePath().apply {
        val hole = NativePath().apply {
            val rimShift = (h * 0.02f).coerceIn(1.dp.toPx(), 1.5.dp.toPx())
            addRoundRect(
                RectF(0f, rimShift, w, h + rimShift),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val bottomRimInset = NativePath().apply {
        val hole = NativePath().apply {
            val rimShift = (h * 0.024f).coerceIn(1.dp.toPx(), 1.8.dp.toPx())
            addRoundRect(
                RectF(0f, -rimShift, w, h - rimShift),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val innerUpperSheenPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = TrackUpperSheen.toArgb()
        maskFilter = BlurMaskFilter((h * 0.18f).coerceIn(4.dp.toPx(), 14.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }

    val innerBottomTintPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = innerTintColor.toArgb()
        maskFilter = BlurMaskFilter((h * 0.16f).coerceIn(4.dp.toPx(), 12.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }

    val topSpecularPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = TrackSpecularTop.toArgb()
        maskFilter = BlurMaskFilter(1.2.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val bottomRimPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = lowerRimColor.toArgb()
        maskFilter = BlurMaskFilter(1.5.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val glossInset = (h * 0.016f).coerceIn(0.8.dp.toPx(), 1.2.dp.toPx())
    val glossPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            0f, glossInset, 0f, h * 0.48f,
            intArrayOf(
                Color(0x52FFFFFF).toArgb(),
                Color(0x14FFFFFF).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    val strokeWidthPx = (h * 0.016f).coerceIn(0.7.dp.toPx(), 1.2.dp.toPx())
    val halfStroke = strokeWidthPx / 2f
    val borderPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.STROKE
        strokeWidth = strokeWidthPx
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color(0xD9FFFFFF).toArgb(),
                Color(0x47FFFFFF).toArgb(),
                lowerRimColor.toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    onDrawWithContent {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Tight Under-Track Contact Shadow (Zero outer side glow)
            nativeCanvas.drawRoundRect(
                shadowInsetX,
                shadowOffsetY + shadowInsetX * 0.5f,
                w - shadowInsetX,
                h + shadowOffsetY,
                radiusPx,
                radiusPx,
                contactShadowPaint
            )

            // 2. Translucent Apple Glass Gradient Body
            nativeCanvas.drawPath(pillPath, linearTrackPaint)

            // 3. Top Sky-Blue Crown & Bottom Caustic Glow
            nativeCanvas.drawPath(pillPath, radialCrownPaint)
            nativeCanvas.drawPath(pillPath, bottomCausticPaint)

            // 4. Clipped Internal Glass Refractions, Shimmer & Meniscus Glaze
            nativeCanvas.save()
            nativeCanvas.clipPath(pillPath)

            nativeCanvas.drawPath(pillPath, shimmerPaint)
            nativeCanvas.drawPath(bottomDepthInset, innerBottomTintPaint)
            nativeCanvas.drawPath(upperSheenInset, innerUpperSheenPaint)

            nativeCanvas.drawRoundRect(
                glossInset,
                glossInset,
                w - glossInset,
                h - glossInset,
                max(0f, radiusPx - glossInset),
                max(0f, radiusPx - glossInset),
                glossPaint
            )

            nativeCanvas.drawPath(topRimInset, topSpecularPaint)
            nativeCanvas.drawPath(bottomRimInset, bottomRimPaint)

            nativeCanvas.restore()

            // 5. Borderless clean glass capsule track
        }

        // 6. Draw Glyphs & Liquid Thumb
        drawContent()
    }
}

/**
 * Renders the sculpted Apple Pearl-Glass Thumb Knob with:
 * - Soft downward elevation shadow inside the track
 * - Luminous pearl-white to ice-blue radial dome fill
 * - Subtle lower sky-blue refraction picked up from the blue glass track
 * - Crisp top specular dome highlight
 */
fun Modifier.appleGlassThumbSurface(
    progress: Float
): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height
    val radiusPx = min(w, h) / 2f
    val t = progress.coerceIn(0f, 1f)

    val thumbShadowColor = lerp(Color(0x330F172A), ThumbDropShadowPrimary, t)
    val thumbShadowPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = thumbShadowColor.toArgb()
        maskFilter = BlurMaskFilter((h * 0.09f).coerceIn(2.dp.toPx(), 6.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }

    val thumbFillPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.White.toArgb(),
                Color(0xF5F6FAFF).toArgb(),
                lerp(Color(0xFFE2E8F0), Color(0xFFE4F0FF), t).toArgb()
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    val lowerRefractionColor = lerp(Color(0x4094A3B8), ThumbInnerRefraction, t)
    val lowerRefractionPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = RadialGradient(
            w * 0.5f,
            h * 0.98f,
            max(w, h) * 0.65f,
            intArrayOf(
                lowerRefractionColor.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.75f),
            Shader.TileMode.CLAMP
        )
    }

    val topGlossInset = 1.5.dp.toPx()
    val topGlossBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        ),
        startY = topGlossInset,
        endY = h * 0.52f
    )

    val borderStrokePx = 1.dp.toPx()
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White,
            Color.White.copy(alpha = 0.78f)
        )
    )

    onDrawWithContent {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Tight downward thumb elevation shadow
            nativeCanvas.drawRoundRect(
                2.dp.toPx(),
                3.dp.toPx(),
                w - 2.dp.toPx(),
                h + 2.5.dp.toPx(),
                radiusPx,
                radiusPx,
                thumbShadowPaint
            )

            // 2. Pearl-Glass Thumb Body
            nativeCanvas.drawRoundRect(0f, 0f, w, h, radiusPx, radiusPx, thumbFillPaint)

            // 3. Subtle Bottom Blue Glass Refraction
            nativeCanvas.drawRoundRect(0f, 0f, w, h, radiusPx, radiusPx, lowerRefractionPaint)
        }

        // 4. Top Dome Specular Glaze
        drawRoundRect(
            brush = topGlossBrush,
            topLeft = Offset(topGlossInset, topGlossInset),
            size = androidx.compose.ui.geometry.Size(w - topGlossInset * 2, h - topGlossInset * 2),
            cornerRadius = CornerRadius(max(0f, radiusPx - topGlossInset))
        )

        drawContent()
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 732)
@Composable
private fun AppleGlassToggleScreenPreview() {
    AppleGlassToggleScreen()
}
