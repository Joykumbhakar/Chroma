package com.chroma.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Exact constants matching the CSS custom properties:
 * --spin-angle: 135deg;
 * --anim-duration: 540ms;
 * --spring-easing: cubic-bezier(0.19, 1, 0.22, 1.16);
 * --orbit-radius: 74px;
 */
private const val SPIN_ANGLE_DEG = 135f
private const val ANIM_DURATION_MS = 540
private val ORBIT_RADIUS_DP = 74.dp
private val TRIGGER_BUTTON_SIZE_DP = 54.dp
private val ACTION_NODE_SIZE_DP = 46.dp
private val GLASS_HALO_BASE_SIZE_DP = 60.dp

/**
 * Custom Spring CubicBezierEasing matching `cubic-bezier(0.19, 1, 0.22, 1.16)`.
 * Jetpack Compose's CubicBezierEasing permits Y control points > 1f to produce
 * the exact spring overshoot trajectory from CSS.
 */
private val SpringOvershootEasing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.16f)

/**
 * Matches `cubic-bezier(0.16, 1, 0.3, 1)` used for node opacity fade.
 */
private val SmoothOpacityEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

/**
 * Matches `cubic-bezier(0.22, 1, 0.36, 1)` used for press & selection pulse feedback.
 */
private val PressFeedbackEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Identifies each action item in the orbital menu.
 */
enum class ActionMenuType(val label: String) {
    GRADIENTS("Gradients"),
    POST("Post"),
    VIDEO("Video"),
    STORY("Story"),
    AI_GENERATOR("AI Generator")
}

/**
 * Encapsulates the exact radial surface gradient stops, linear glass border stops,
 * and SVG icon linear gradient stops for each option button.
 */
data class GlassOptionTheme(
    val fillStops: Array<Pair<Float, Color>>,
    val borderStops: Array<Pair<Float, Color>>,
    val iconGradientStops: Array<Pair<Float, Color>>
)

/**
 * Returns the exact theme specification corresponding to `.glass-indigo`, `.glass-sky`,
 * `.glass-teal`, `.glass-amber`, and `.glass-violet`.
 */
private fun getThemeForAction(type: ActionMenuType): GlassOptionTheme {
    return when (type) {
        ActionMenuType.GRADIENTS -> GlassOptionTheme(
            fillStops = arrayOf(
                0.00f to Color(0xFAFFFFFF), // rgba(255, 255, 255, 0.98)
                0.52f to Color(0xEBEEF2FF), // rgba(238, 242, 255, 0.92)
                1.00f to Color(0xE0E0E7FF)  // rgba(224, 231, 255, 0.88)
            ),
            borderStops = arrayOf(
                0.00f to Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
                0.52f to Color(0x8CA5B4FC), // rgba(165, 180, 252, 0.55)
                1.00f to Color(0x526366F1)  // rgba(99, 102, 241, 0.32)
            ),
            iconGradientStops = arrayOf(
                0.00f to Color(0xFF6366F1),
                1.00f to Color(0xFF4338CA)
            )
        )

        ActionMenuType.POST -> GlassOptionTheme(
            fillStops = arrayOf(
                0.00f to Color(0xFAFFFFFF), // rgba(255, 255, 255, 0.98)
                0.52f to Color(0xEBF0F9FF), // rgba(240, 249, 255, 0.92)
                1.00f to Color(0xE0E0F2FE)  // rgba(224, 242, 254, 0.88)
            ),
            borderStops = arrayOf(
                0.00f to Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
                0.52f to Color(0x8C7DD3FC), // rgba(125, 211, 252, 0.55)
                1.00f to Color(0x520EA5E9)  // rgba(14, 165, 233, 0.32)
            ),
            iconGradientStops = arrayOf(
                0.00f to Color(0xFF0EA5E9),
                1.00f to Color(0xFF0369A1)
            )
        )

        ActionMenuType.VIDEO -> GlassOptionTheme(
            fillStops = arrayOf(
                0.00f to Color(0xFAFFFFFF), // rgba(255, 255, 255, 0.98)
                0.52f to Color(0xEBF0FDFA), // rgba(240, 253, 250, 0.92)
                1.00f to Color(0xDBCCFBF1)  // rgba(204, 251, 241, 0.86)
            ),
            borderStops = arrayOf(
                0.00f to Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
                0.52f to Color(0x8C5EEAD4), // rgba(94, 234, 212, 0.55)
                1.00f to Color(0x5214B8A6)  // rgba(20, 184, 166, 0.32)
            ),
            iconGradientStops = arrayOf(
                0.00f to Color(0xFF14B8A6),
                1.00f to Color(0xFF0F766E)
            )
        )

        ActionMenuType.STORY -> GlassOptionTheme(
            fillStops = arrayOf(
                0.00f to Color(0xFAFFFFFF), // rgba(255, 255, 255, 0.98)
                0.52f to Color(0xEBFFFBEB), // rgba(255, 251, 235, 0.92)
                1.00f to Color(0xDBFEF3C7)  // rgba(254, 243, 199, 0.86)
            ),
            borderStops = arrayOf(
                0.00f to Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
                0.52f to Color(0x94FCD34D), // rgba(252, 211, 77, 0.58)
                1.00f to Color(0x52F59E0B)  // rgba(245, 158, 11, 0.32)
            ),
            iconGradientStops = arrayOf(
                0.00f to Color(0xFFF59E0B),
                1.00f to Color(0xFFB45309)
            )
        )

        ActionMenuType.AI_GENERATOR -> GlassOptionTheme(
            fillStops = arrayOf(
                0.00f to Color(0xFAFFFFFF), // rgba(255, 255, 255, 0.98)
                0.52f to Color(0xEBF5F3FF), // rgba(245, 243, 255, 0.92)
                1.00f to Color(0xE0EDE9FE)  // rgba(237, 233, 254, 0.88)
            ),
            borderStops = arrayOf(
                0.00f to Color(0xFFFFFFFF), // rgba(255, 255, 255, 1.0)
                0.52f to Color(0x94C4B5FD), // rgba(196, 181, 253, 0.58)
                1.00f to Color(0x528B5CF6)  // rgba(139, 92, 246, 0.32)
            ),
            iconGradientStops = arrayOf(
                0.00f to Color(0xFF8B5CF6),
                0.50f to Color(0xFF6D28D9),
                1.00f to Color(0xFFEC4899)
            )
        )
    }
}

/**
 * The self-contained Synchronized Rotating Action Menu component.
 *
 * Synchronizes the 135-degree rotation of the central '+' trigger button with the
 * orbital carrier, while counter-rotating each action node by -135 degrees so icons
 * travel along a curved spiral path and arrive upright in a symmetrical upper arc.
 */
@Composable
fun SynchronizedRotatingActionMenu(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onActionSelected: (ActionMenuType) -> Unit = {}
) {
    val density = LocalDensity.current
    val orbitRadiusPx = with(density) { ORBIT_RADIUS_DP.toPx() }

    // Carrier & Main button synchronized rotation (0deg <-> 135deg)
    val spinRotation by animateFloatAsState(
        targetValue = if (isOpen) SPIN_ANGLE_DEG else 0f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            easing = SpringOvershootEasing
        ),
        label = "SynchronizedSpinRotation"
    )

    // Subtle expanding glass halo scale (0.75 <-> 3.45)
    val haloScale by animateFloatAsState(
        targetValue = if (isOpen) 3.45f else 0.75f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            easing = SpringOvershootEasing
        ),
        label = "GlassHaloScale"
    )

    // Glass halo opacity (0f <-> 1f over 320ms ease)
    val haloAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing
        ),
        label = "GlassHaloAlpha"
    )

    val actions = remember { ActionMenuType.entries }
    val totalNodes = actions.size
    val arcSpanDeg = 180f // -180deg (left) to 0deg (right)
    val stepDeg = arcSpanDeg / (totalNodes - 1)

    // Interactive 256.dp x 256.dp container matching `w-64 h-64`
    Box(
        modifier = modifier.size(256.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Subtle Frosted Glass Halo Plate
        GlassHaloPlate(
            scale = haloScale,
            alpha = haloAlpha
        )

        // 2. Synchronized Rotating Carrier
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = spinRotation
                },
            contentAlignment = Alignment.Center
        ) {
            actions.forEachIndexed { index, actionType ->
                // Pre-compute polar coordinates with -SPIN_ANGLE compensation
                val finalAngleDeg = -180f + index * stepDeg
                val compensatedAngleDeg = finalAngleDeg - SPIN_ANGLE_DEG
                val rad = (compensatedAngleDeg * PI / 180.0).toFloat()

                val targetTx = orbitRadiusPx * cos(rad)
                val targetTy = orbitRadiusPx * sin(rad)

                // Micro-stagger matching `--stagger-open: index * 12ms` and `--stagger-close: (total - 1 - index) * 8ms`
                val staggerDelayMs = if (isOpen) {
                    index * 12
                } else {
                    (totalNodes - 1 - index) * 8
                }

                OrbitalActionNode(
                    actionType = actionType,
                    isOpen = isOpen,
                    targetTx = targetTx,
                    targetTy = targetTy,
                    staggerDelayMs = staggerDelayMs,
                    onSelect = {
                        onActionSelected(actionType)
                        onClose()
                    }
                )
            }
        }

        // 3. Central Circle '+' Trigger Button
        MainTriggerButton(
            isOpen = isOpen,
            rotationDeg = spinRotation,
            onClick = onToggle
        )
    }
}

/**
 * Renders the expanding `.glass-halo` backdrop behind the active menu.
 */
@Composable
private fun GlassHaloPlate(
    scale: Float,
    alpha: Float
) {
    if (alpha <= 0.001f) return

    Canvas(
        modifier = Modifier
            .size(GLASS_HALO_BASE_SIZE_DP)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        val radius = size.minDimension / 2f
        // Keep the 1dp border crisp even when scaled up by 3.45x
        val strokeWidthPx = (1.dp.toPx() / scale.coerceAtLeast(1f))

        // Radial inner fill at (50%, 35%)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xADFFFFFF), // rgba(255, 255, 255, 0.68)
                    0.65f to Color(0x47FFFFFF), // rgba(255, 255, 255, 0.28)
                    1.00f to Color(0x1FFFFFFF)  // rgba(255, 255, 255, 0.12)
                ),
                center = Offset(size.width * 0.50f, size.height * 0.35f),
                radius = radius * 1.15f
            ),
            radius = radius - strokeWidthPx / 2f
        )

        // Thin 145deg linear glass border
        drawCircle(
            brush = cssLinearGradient(
                angleDeg = 145f,
                size = size,
                colorStops = arrayOf(
                    0.00f to Color(0xD9FFFFFF), // rgba(255, 255, 255, 0.85)
                    0.50f to Color(0x4DCBD5E1), // rgba(203, 213, 225, 0.30)
                    1.00f to Color(0x8CFFFFFF)  // rgba(255, 255, 255, 0.55)
                )
            ),
            radius = radius - strokeWidthPx / 2f,
            style = Stroke(width = strokeWidthPx)
        )
    }
}

/**
 * Individual action button node inside the rotating orbital carrier.
 *
 * Translates from `(0, 0)` to `(targetTx, targetTy)`, scales from `0.28f` to `1.0f`,
 * and maintains a local `-135deg` counter-rotation so its world angle matches the
 * central '+' button rotation and comes to rest upright (`0deg`).
 */
@Composable
private fun OrbitalActionNode(
    actionType: ActionMenuType,
    isOpen: Boolean,
    targetTx: Float,
    targetTy: Float,
    staggerDelayMs: Int,
    onSelect: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val theme = remember(actionType) { getThemeForAction(actionType) }

    // Progress animatable for transform (translation & scale) with spring overshoot
    val transformProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            delayMillis = staggerDelayMs,
            easing = SpringOvershootEasing
        ),
        label = "NodeTransform_${actionType.name}"
    )

    // Opacity animatable (300ms cubic-bezier(0.16, 1, 0.3, 1))
    val alphaProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = staggerDelayMs,
            easing = SmoothOpacityEasing
        ),
        label = "NodeAlpha_${actionType.name}"
    )

    // Interactive press scale (0.9f when active)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = PressFeedbackEasing
        ),
        label = "NodePressScale_${actionType.name}"
    )

    // Selection pulse animation matching `@keyframes selectPulse` (1 -> 1.16 at 45% -> 0.92 at 100%)
    val pulseScale = remember { Animatable(1f) }

    // Interpolate scale from 0.28f (closed) to 1.0f (open)
    val baseScale = 0.28f + (1.0f - 0.28f) * transformProgress
    val combinedScale = baseScale * pressScale * pulseScale.value

    if (alphaProgress <= 0.001f && !isOpen) return

    Box(
        modifier = Modifier
            .size(ACTION_NODE_SIZE_DP)
            .graphicsLayer {
                translationX = targetTx * transformProgress
                translationY = targetTy * transformProgress
                scaleX = combinedScale
                scaleY = combinedScale
                // Counter-rotate by -SPIN_ANGLE_DEG so the icon stays upright when carrier is at +SPIN_ANGLE_DEG
                rotationZ = -SPIN_ANGLE_DEG
                alpha = alphaProgress.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .semantics {
                contentDescription = actionType.label
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isOpen
            ) {
                scope.launch {
                    // Trigger the 320ms keyframe selection pulse
                    launch {
                        pulseScale.snapTo(1f)
                        pulseScale.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 320
                                1.00f at 0 using PressFeedbackEasing
                                1.16f at 144 using PressFeedbackEasing // 45% of 320ms
                                0.92f at 320
                            }
                        )
                        pulseScale.snapTo(1f)
                    }
                    // Matches the 120ms timeout before closing the menu in JS
                    delay(120)
                    onSelect()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val borderStroke = 1.dp.toPx()

            // 1. Bespoke Radial Gradient Surface (120% radius at 25% 20%)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = theme.fillStops,
                    center = Offset(size.width * 0.25f, size.height * 0.20f),
                    radius = size.minDimension * 1.20f
                ),
                radius = radius - borderStroke / 2f
            )

            // 2. Top Specular Sheen (.option-btn::before: 165deg white 0.75 -> 0.15 at 45% -> transparent at 65%)
            drawCircle(
                brush = cssLinearGradient(
                    angleDeg = 165f,
                    size = size,
                    colorStops = arrayOf(
                        0.00f to Color(0xBFFFFFFF), // rgba(255, 255, 255, 0.75)
                        0.45f to Color(0x26FFFFFF), // rgba(255, 255, 255, 0.15)
                        0.65f to Color.Transparent
                    )
                ),
                radius = radius - borderStroke / 2f
            )

            // 3. Outer 135deg Tinted Glass Border (1dp)
            drawCircle(
                brush = cssLinearGradient(
                    angleDeg = 135f,
                    size = size,
                    colorStops = theme.borderStops
                ),
                radius = radius - borderStroke / 2f,
                style = Stroke(width = borderStroke)
            )

            // 4. Inner Glass Rim (.option-btn::after: inset 1px border rgba(255, 255, 255, 0.82))
            drawCircle(
                color = Color(0xD1FFFFFF),
                radius = radius - borderStroke * 1.5f,
                style = Stroke(width = borderStroke)
            )
        }

        // 5. Custom Gradient SVG Iconography
        ActionNodeIcon(
            actionType = actionType,
            iconGradientStops = theme.iconGradientStops
        )
    }
}

/**
 * Renders the central 54.dp obsidian '+' trigger button with its smooth open/close
 * color transition, inner specular glass rim, press scale feedback, and 135-degree spin.
 */
@Composable
private fun MainTriggerButton(
    isOpen: Boolean,
    rotationDeg: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale (0.92f when active)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(
            durationMillis = 160,
            easing = LinearOutSlowInEasing
        ),
        label = "MainTriggerPressScale"
    )

    // Smoothly transition background and border colors over 280ms when opened
    val openColorFraction by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "MainTriggerColorTransition"
    )

    Box(
        modifier = Modifier
            .size(TRIGGER_BUTTON_SIZE_DP)
            .graphicsLayer {
                rotationZ = rotationDeg
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .semantics {
                contentDescription = "Toggle Action Menu"
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val borderWidth = 1.dp.toPx()

            // Interpolate radial gradient stops between closed and open obsidian states
            val stop0 = lerp(Color(0xFF334155), Color(0xFF1E293B), openColorFraction)
            val stop1 = lerp(Color(0xFF1E293B), Color(0xFF0F172A), openColorFraction)
            val stop2 = lerp(Color(0xFF0F172A), Color(0xFF080C16), openColorFraction)
            val stop3 = lerp(Color(0xFF090D16), Color(0xFF060911), openColorFraction)

            // 1. Main Obsidian Radial Fill (110% radius at 30% 20%)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to stop0,
                        0.42f to stop1,
                        0.82f to stop2,
                        1.00f to stop3
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.20f),
                    radius = size.minDimension * 1.10f
                ),
                radius = radius - borderWidth / 2f
            )

            // 2. Outer 135deg Glass Border
            val borderStart = lerp(Color(0x85FFFFFF), Color(0x6BFFFFFF), openColorFraction)
            val borderMid = lerp(Color(0x2994A3B8), Color(0x2494A3B8), openColorFraction)
            val borderEnd = lerp(Color(0x47FFFFFF), Color(0x38FFFFFF), openColorFraction)

            drawCircle(
                brush = cssLinearGradient(
                    angleDeg = 135f,
                    size = size,
                    colorStops = arrayOf(
                        0.00f to borderStart,
                        0.50f to borderMid,
                        1.00f to borderEnd
                    )
                ),
                radius = radius - borderWidth / 2f,
                style = Stroke(width = borderWidth)
            )

            // 3. Inner Specular Glass Rim (.main-trigger-btn::after: inset 1px)
            val innerRadius = radius - borderWidth * 1.5f
            drawCircle(
                brush = cssLinearGradient(
                    angleDeg = 160f,
                    size = size,
                    colorStops = arrayOf(
                        0.00f to Color(0x29FFFFFF), // rgba(255, 255, 255, 0.16)
                        0.45f to Color.Transparent
                    )
                ),
                radius = innerRadius
            )

            drawCircle(
                color = Color(0x1AFFFFFF), // rgba(255, 255, 255, 0.10)
                radius = innerRadius,
                style = Stroke(width = borderWidth)
            )

            // 4. Crisp Plus ('+') Icon (22.dp x 22.dp, stroke-width 2.25 in 24x24 viewport)
            val iconSizePx = 22.dp.toPx()
            val iconOffset = (size.minDimension - iconSizePx) / 2f
            val scaleFactor = iconSizePx / 24f
            val plusStrokeWidth = 2.25f * scaleFactor

            // Horizontal bar: M5 12h14 -> (5, 12) to (19, 12)
            drawLine(
                color = Color.White,
                start = Offset(iconOffset + 5f * scaleFactor, iconOffset + 12f * scaleFactor),
                end = Offset(iconOffset + 19f * scaleFactor, iconOffset + 12f * scaleFactor),
                strokeWidth = plusStrokeWidth,
                cap = StrokeCap.Round
            )
            // Vertical bar: M12 5v14 -> (12, 5) to (12, 19)
            drawLine(
                color = Color.White,
                start = Offset(iconOffset + 12f * scaleFactor, iconOffset + 5f * scaleFactor),
                end = Offset(iconOffset + 12f * scaleFactor, iconOffset + 19f * scaleFactor),
                strokeWidth = plusStrokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Renders the 5 customized Lucide vector icons with their exact SVG paths, circles,
 * rounded rectangles, gradient strokes (`stroke-width="1.9"`), and translucent gradient fills.
 */
@Composable
private fun ActionNodeIcon(
    actionType: ActionMenuType,
    iconGradientStops: Array<Pair<Float, Color>>
) {
    val iconSizeDp: Dp = if (actionType == ActionMenuType.POST) 18.dp else 19.dp

    Canvas(modifier = Modifier.size(iconSizeDp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val strokeWidthPx = 1.9f * sx

        val strokeBrush = Brush.linearGradient(
            colorStops = iconGradientStops,
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        fun fillBrushWithAlpha(alphaMultiplier: Float): Brush {
            val alphaStops = iconGradientStops.map { (stop, color) ->
                stop to color.copy(alpha = color.alpha * alphaMultiplier)
            }.toTypedArray()
            return Brush.linearGradient(
                colorStops = alphaStops,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        }

        val strokeStyle = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (actionType) {
            // 1. Gradients (Customized Lucide Palette Icon)
            ActionMenuType.GRADIENTS -> {
                val palettePath = parseScaledSvgPath(
                    pathData = "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2Z",
                    sx = sx,
                    sy = sy
                )
                drawPath(path = palettePath, brush = fillBrushWithAlpha(0.14f), style = Fill)
                drawPath(path = palettePath, brush = strokeBrush, style = strokeStyle)

                val dotRadius = 1.1f * sx
                drawCircle(brush = strokeBrush, radius = dotRadius, center = Offset(13.5f * sx, 6.5f * sy))
                drawCircle(brush = strokeBrush, radius = dotRadius, center = Offset(17.5f * sx, 10.5f * sy))
                drawCircle(brush = strokeBrush, radius = dotRadius, center = Offset(8.5f * sx, 7.5f * sy))
                drawCircle(brush = strokeBrush, radius = dotRadius, center = Offset(6.5f * sx, 12.5f * sy))
            }

            // 2. Post (Customized Lucide Square-Pen Icon)
            ActionMenuType.POST -> {
                val boxPath = parseScaledSvgPath(
                    pathData = "M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7",
                    sx = sx,
                    sy = sy
                )
                val penPath = parseScaledSvgPath(
                    pathData = "M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z",
                    sx = sx,
                    sy = sy
                )
                drawPath(path = boxPath, brush = fillBrushWithAlpha(0.12f), style = Fill)
                drawPath(path = boxPath, brush = strokeBrush, style = strokeStyle)
                drawPath(path = penPath, brush = strokeBrush, style = strokeStyle)
            }

            // 3. Video (Customized Lucide Video Icon)
            ActionMenuType.VIDEO -> {
                // <rect width="14" height="12" x="2" y="6" rx="2.5" />
                val rectTopLeft = Offset(2f * sx, 6f * sy)
                val rectSize = Size(14f * sx, 12f * sy)
                val cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy)

                drawRoundRect(
                    brush = fillBrushWithAlpha(0.14f),
                    topLeft = rectTopLeft,
                    size = rectSize,
                    cornerRadius = cornerRadius,
                    style = Fill
                )
                drawRoundRect(
                    brush = strokeBrush,
                    topLeft = rectTopLeft,
                    size = rectSize,
                    cornerRadius = cornerRadius,
                    style = strokeStyle
                )

                val lensPath = parseScaledSvgPath(
                    pathData = "m16 10 4.523-2.261A.999.999 0 0 1 22 8.633v6.734a1 1 0 0 1-1.477.894L16 14v-4Z",
                    sx = sx,
                    sy = sy
                )
                drawPath(path = lensPath, brush = fillBrushWithAlpha(0.08f), style = Fill)
                drawPath(path = lensPath, brush = strokeBrush, style = strokeStyle)

                drawCircle(
                    brush = strokeBrush,
                    radius = 1.05f * sx,
                    center = Offset(6.5f * sx, 10f * sy)
                )
            }

            // 4. Story (Customized Lucide Circle-Fading-Plus Icon)
            ActionMenuType.STORY -> {
                drawCircle(
                    brush = fillBrushWithAlpha(0.14f),
                    radius = 9f * sx,
                    center = Offset(12f * sx, 12f * sy)
                )

                val storyPaths = listOf(
                    "M12 2a10 10 0 0 1 7.38 16.75",
                    "M12 8v8",
                    "M16 12H8",
                    "M2.5 8.875a10 10 0 0 0-.5 3",
                    "M2.83 16a10 10 0 0 0 2.43 3.4",
                    "M4.636 5.235a10 10 0 0 1 .891-.857",
                    "M8.644 21.42a10 10 0 0 0 7.631-.38"
                )
                storyPaths.forEach { d ->
                    val path = parseScaledSvgPath(d, sx, sy)
                    drawPath(path = path, brush = strokeBrush, style = strokeStyle)
                }
            }

            // 5. AI Generator (Customized Lucide Sparkles Icon)
            ActionMenuType.AI_GENERATOR -> {
                val mainStarPath = parseScaledSvgPath(
                    pathData = "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
                    sx = sx,
                    sy = sy
                )
                drawPath(path = mainStarPath, brush = fillBrushWithAlpha(0.16f), style = Fill)
                drawPath(path = mainStarPath, brush = strokeBrush, style = strokeStyle)

                val sparkleLines = listOf(
                    "M20 3v4",
                    "M22 5h-4",
                    "M4 17v2",
                    "M5 18H3"
                )
                sparkleLines.forEach { d ->
                    val path = parseScaledSvgPath(d, sx, sy)
                    drawPath(path = path, brush = strokeBrush, style = strokeStyle)
                }
            }
        }
    }
}

/**
 * Parses an SVG path string in a 24x24 coordinate space and scales it to the target Canvas size.
 */
private fun parseScaledSvgPath(pathData: String, sx: Float, sy: Float): Path {
    val path = PathParser().parsePathString(pathData).toPath()
    val matrix = androidx.compose.ui.graphics.Matrix()
    matrix.scale(sx, sy, 1f)
    path.transform(matrix)
    return path
}

/**
 * Computes an exact CSS-equivalent `linear-gradient(<angle>deg, ...)` brush for a given [Size].
 * In CSS, `0deg` points upward (bottom-to-top) and angles increase clockwise (`90deg` is left-to-right,
 * `135deg` is top-left to bottom-right, `165deg` is steeply angled downward).
 */
private fun DrawScope.cssLinearGradient(
    angleDeg: Float,
    size: Size,
    colorStops: Array<Pair<Float, Color>>
): Brush {
    val angleRad = ((angleDeg - 90f) * PI / 180.0).toFloat()
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    val halfW = size.width / 2f
    val halfH = size.height / 2f
    val center = Offset(halfW, halfH)

    // Exact projection length along the gradient line inside the bounding box
    val halfLength = kotlin.math.abs(halfW * cosA) + kotlin.math.abs(halfH * sinA)

    val start = Offset(
        x = center.x - cosA * halfLength,
        y = center.y - sinA * halfLength
    )
    val end = Offset(
        x = center.x + cosA * halfLength,
        y = center.y + sinA * halfLength
    )

    return Brush.linearGradient(
        colorStops = colorStops,
        start = start,
        end = end
    )
}
