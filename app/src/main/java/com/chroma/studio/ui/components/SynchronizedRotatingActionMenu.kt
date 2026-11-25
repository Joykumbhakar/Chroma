package com.chroma.studio.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chroma.studio.chat.ui.components.GlassGradient
import com.chroma.studio.chat.ui.components.GlassStyle
import com.chroma.studio.chat.ui.components.createGlassAgsl
import com.chroma.studio.chat.ui.components.drawGlassTile
import com.chroma.studio.chat.ui.components.glyphFlashVideo
import com.chroma.studio.chat.ui.components.glyphPhotos
import com.chroma.studio.chat.ui.components.glyphStroke
import com.chroma.studio.chat.ui.components.withGlyphShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Exact animation constants:
 * --spin-angle: 135deg;
 * --anim-duration: 540ms;
 * --spring-easing: cubic-bezier(0.19, 1, 0.22, 1.16);
 * --orbit-radius: 82px;
 */
private const val SPIN_ANGLE_DEG = 135f
private const val ANIM_DURATION_MS = 540
private val ORBIT_RADIUS_DP = 82.dp
private val TRIGGER_BUTTON_SIZE_DP = 56.dp
private val ACTION_NODE_SIZE_DP = 48.dp
private val GLASS_HALO_BASE_SIZE_DP = 64.dp

private val SpringOvershootEasing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.16f)
private val SmoothOpacityEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
private val PressFeedbackEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Identifies the 5 main creation editors:
 * 1. Chroma Studio Gradient Studio
 * 2. Flash Final Cut Video Editor
 * 3. Photo Editor for Post
 * 4. Story Editor
 * 5. AI Generator
 */
enum class ActionMenuType(val label: String) {
    GRADIENTS("Gradient Studio"),
    FLASH("Flash Video"),
    IMAGE_EDITOR("Photo Editor"),
    STORY("Story Editor"),
    AI_GENERATOR("AI Generator")
}

private fun getGradientForAction(type: ActionMenuType): GlassGradient {
    return when (type) {
        ActionMenuType.GRADIENTS -> GlassGradient(Color(0xFF8B5CF6), Color(0xFF6366F1))
        ActionMenuType.FLASH -> GlassGradient(Color(0xFFFF007A), Color(0xFFFF5E00))
        ActionMenuType.IMAGE_EDITOR -> GlassGradient(Color(0xFF38BDF8), Color(0xFF0381FE))
        ActionMenuType.STORY -> GlassGradient(Color(0xFFFFB83D), Color(0xFFF8651B))
        ActionMenuType.AI_GENERATOR -> GlassGradient(Color(0xFF2DD4BF), Color(0xFF0D9488))
    }
}

/**
 * Synchronized Rotating Action Menu with liquid-glass 3D squircle icons.
 * Symmetrically presents the 5 main editors across an upper radial arc.
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

    val spinRotation by animateFloatAsState(
        targetValue = if (isOpen) SPIN_ANGLE_DEG else 0f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            easing = SpringOvershootEasing
        ),
        label = "SynchronizedSpinRotation"
    )

    val haloScale by animateFloatAsState(
        targetValue = if (isOpen) 3.45f else 0.75f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            easing = SpringOvershootEasing
        ),
        label = "GlassHaloScale"
    )

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
    val arcSpanDeg = 180f
    val stepDeg = arcSpanDeg / (totalNodes - 1) // 45° step for 5 nodes

    val glassStyle = remember {
        GlassStyle(
            tilt = 30f,
            roundness = 0.6f,
            refraction = 0.8f,
            bevel = 0.6f,
            frost = 0.2f,
            shine = 0.85f,
            shade = 0.22f,
            tint = Color(0xFFDFDFDF),
            tintOpacity = 0.2f
        )
    }
    val agsl = remember { createGlassAgsl() }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Frosted Glass Halo Plate
        GlassHaloPlate(
            scale = haloScale,
            alpha = haloAlpha
        )

        // 2. Synchronized Carrier for the 5 Liquid Squircle Action Nodes
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            actions.forEachIndexed { index, actionType ->
                val finalAngleDeg = -180f + index * stepDeg
                val compensatedAngleDeg = finalAngleDeg - SPIN_ANGLE_DEG
                val rad = (compensatedAngleDeg * PI / 180.0).toFloat()

                val targetTx = orbitRadiusPx * cos(rad)
                val targetTy = orbitRadiusPx * sin(rad)

                val staggerDelayMs = if (isOpen) {
                    index * 14
                } else {
                    (totalNodes - 1 - index) * 10
                }

                OrbitalActionNode(
                    actionType = actionType,
                    isOpen = isOpen,
                    targetTx = targetTx,
                    targetTy = targetTy,
                    spinRotation = spinRotation,
                    staggerDelayMs = staggerDelayMs,
                    agsl = agsl,
                    glassStyle = glassStyle,
                    onSelect = {
                        onActionSelected(actionType)
                        onClose()
                    }
                )
            }
        }

        // 3. Central Obsidian Circle '+' Trigger Button
        MainTriggerButton(
            isOpen = isOpen,
            rotationDeg = spinRotation,
            onClick = onToggle
        )
    }
}

/**
 * Renders the expanding glass halo backdrop behind the active menu.
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
        val strokeWidthPx = (1.dp.toPx() / scale.coerceAtLeast(1f))

        // Radial inner fill at (50%, 35%)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xADFFFFFF),
                    0.65f to Color(0x47FFFFFF),
                    1.00f to Color(0x1FFFFFFF)
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
                    0.00f to Color(0xD9FFFFFF),
                    0.50f to Color(0x4DCBD5E1),
                    1.00f to Color(0x8CFFFFFF)
                )
            ),
            radius = radius - strokeWidthPx / 2f,
            style = Stroke(width = strokeWidthPx)
        )
    }
}

/**
 * Individual action button node inside the rotating orbital carrier.
 * Renders the dual-layer liquid glass squircle with white glyph floating above.
 */
@Composable
private fun OrbitalActionNode(
    actionType: ActionMenuType,
    isOpen: Boolean,
    targetTx: Float,
    targetTy: Float,
    spinRotation: Float,
    staggerDelayMs: Int,
    agsl: Any?,
    glassStyle: GlassStyle,
    onSelect: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val gradient = remember(actionType) { getGradientForAction(actionType) }

    val transformProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = ANIM_DURATION_MS,
            delayMillis = staggerDelayMs,
            easing = SpringOvershootEasing
        ),
        label = "NodeTransform_${actionType.name}"
    )

    val alphaProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = staggerDelayMs,
            easing = SmoothOpacityEasing
        ),
        label = "NodeAlpha_${actionType.name}"
    )

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

    val baseScale = 0.28f + (1.0f - 0.28f) * transformProgress
    val combinedScale = baseScale * pressScale

    if (alphaProgress <= 0.001f && !isOpen) return

    val rotRad = (spinRotation * PI / 180.0).toFloat()
    val localX = targetTx * transformProgress
    val localY = targetTy * transformProgress
    val worldTx = localX * cos(rotRad) - localY * sin(rotRad)
    val worldTy = localX * sin(rotRad) + localY * cos(rotRad)

    val nodeSize = ACTION_NODE_SIZE_DP

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    worldTx.roundToInt(),
                    worldTy.roundToInt()
                )
            }
            .size(nodeSize)
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = alphaProgress.coerceIn(0f, 1f)
            }
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(100)
                    onSelect()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Dual-layer Liquid Glass Tile (Tilted Gradient Plate + Frosted Front Squircle)
        Canvas(modifier = Modifier.requiredSize(nodeSize * 2.2f)) {
            drawGlassTile(
                agsl = agsl,
                tile = nodeSize,
                gradient = gradient,
                style = glassStyle,
                enter = transformProgress,
                press = if (isPressed) 1f else 0f
            )
        }

        // 2. Crisp Vector Glyph floating on the glass with subtle shadow
        Canvas(
            modifier = Modifier
                .size(if (actionType == ActionMenuType.AI_GENERATOR) nodeSize * 0.50f else nodeSize * 0.44f)
                .graphicsLayer {
                    val e = transformProgress.coerceIn(0f, 1f)
                    val rise = (1f - ((1f - e) * (1f - e))) * 0.08f * nodeSize.toPx()
                    translationY = rise
                    alpha = alphaProgress.coerceIn(0f, 1f)
                }
        ) {
            when (actionType) {
                ActionMenuType.GRADIENTS -> glyphGradientStudio(Color.White)
                ActionMenuType.FLASH -> glyphFlashVideo(Color.White)
                ActionMenuType.IMAGE_EDITOR -> glyphPhotos(Color.White)
                ActionMenuType.STORY -> glyphStoryEditor(Color.White)
                ActionMenuType.AI_GENERATOR -> drawFluentDiamondGem(includeSoftShadow = true)
            }
        }
    }
}

/**
 * 1. Chroma Studio Gradient Studio Glyph: Artist palette with color wells.
 */
private fun DrawScope.glyphGradientStudio(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    val palette = Path().apply {
        moveTo(12f * k, 3f * k)
        cubicTo(7.03f * k, 3f * k, 3f * k, 7.03f * k, 3f * k, 12f * k)
        cubicTo(3f * k, 16.97f * k, 7.03f * k, 21f * k, 12f * k, 21f * k)
        cubicTo(12.83f * k, 21f * k, 13.5f * k, 20.33f * k, 13.5f * k, 19.5f * k)
        cubicTo(13.5f * k, 19.11f * k, 13.34f * k, 18.76f * k, 13.09f * k, 18.5f * k)
        cubicTo(12.84f * k, 18.24f * k, 12.68f * k, 17.89f * k, 12.68f * k, 17.5f * k)
        cubicTo(12.68f * k, 16.67f * k, 13.35f * k, 16f * k, 14.18f * k, 16f * k)
        lineTo(16f * k, 16f * k)
        cubicTo(18.76f * k, 16f * k, 21f * k, 13.76f * k, 21f * k, 11f * k)
        cubicTo(21f * k, 6.58f * k, 16.97f * k, 3f * k, 12f * k, 3f * k)
        close()
    }
    drawPath(palette, color = c, style = st)
    drawCircle(color = c, radius = 1.3f * k, center = Offset(7.5f * k, 11.5f * k))
    drawCircle(color = c, radius = 1.3f * k, center = Offset(9.5f * k, 7.5f * k))
    drawCircle(color = c, radius = 1.3f * k, center = Offset(14.5f * k, 7.5f * k))
    drawCircle(color = c, radius = 1.3f * k, center = Offset(16.8f * k, 11.5f * k))
}

/**
 * 4. Story Editor Glyph: Segmented circular story ring with centered plus.
 */
private fun DrawScope.glyphStoryEditor(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    val radius = 8.5f * k
    val center = Offset(12f * k, 12f * k)
    drawArc(color = c, startAngle = -60f, sweepAngle = 80f, useCenter = false, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2), style = st)
    drawArc(color = c, startAngle = 60f, sweepAngle = 80f, useCenter = false, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2), style = st)
    drawArc(color = c, startAngle = 180f, sweepAngle = 80f, useCenter = false, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2), style = st)
    drawLine(color = c, start = Offset(12f * k, 8.5f * k), end = Offset(12f * k, 15.5f * k), strokeWidth = st.width, cap = StrokeCap.Round)
    drawLine(color = c, start = Offset(8.5f * k, 12f * k), end = Offset(15.5f * k, 12f * k), strokeWidth = st.width, cap = StrokeCap.Round)
}

/**
 * Central 56.dp obsidian trigger button with specular glass rim, press scale feedback,
 * and 135-degree spin morphing '+' into '×'.
 */
@Composable
private fun MainTriggerButton(
    isOpen: Boolean,
    rotationDeg: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(
            durationMillis = 160,
            easing = LinearOutSlowInEasing
        ),
        label = "MainTriggerPressScale"
    )

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

            val stop0 = lerp(Color(0xFF334155), Color(0xFF1E293B), openColorFraction)
            val stop1 = lerp(Color(0xFF1E293B), Color(0xFF0F172A), openColorFraction)
            val stop2 = lerp(Color(0xFF0F172A), Color(0xFF080C16), openColorFraction)
            val stop3 = lerp(Color(0xFF090D16), Color(0xFF060911), openColorFraction)

            // 1. Obsidian Radial Fill
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

            // 3. Inner Specular Glass Rim
            val innerRadius = radius - borderWidth * 1.5f
            drawCircle(
                brush = cssLinearGradient(
                    angleDeg = 160f,
                    size = size,
                    colorStops = arrayOf(
                        0.00f to Color(0x29FFFFFF),
                        0.45f to Color.Transparent
                    )
                ),
                radius = innerRadius
            )

            drawCircle(
                color = Color(0x1AFFFFFF),
                radius = innerRadius,
                style = Stroke(width = borderWidth)
            )

            // 4. Crisp White '+' Icon
            val iconSizePx = 22.dp.toPx()
            val iconOffset = (size.minDimension - iconSizePx) / 2f
            val scaleFactor = iconSizePx / 24f
            val plusStrokeWidth = 2.25f * scaleFactor

            drawLine(
                color = Color.White,
                start = Offset(iconOffset + 5f * scaleFactor, iconOffset + 12f * scaleFactor),
                end = Offset(iconOffset + 19f * scaleFactor, iconOffset + 12f * scaleFactor),
                strokeWidth = plusStrokeWidth,
                cap = StrokeCap.Round
            )
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
