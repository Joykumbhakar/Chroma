package com.chroma.studio.chat.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Represents the available 3D action options in the synchronized rotating menu.
 */
enum class ActionMenuOption(
    val label: String,
    val bgStops: List<Color>,
    val borderStops: List<Color>,
    val shadowColor: Color
) {
    CAMERA(
        label = "Camera",
        bgStops = listOf(
            Color(0xFAFFFFFF),
            Color(0xF0EEF2FF),
            Color(0xE6E0E7FF)
        ),
        borderStops = listOf(
            Color(0xFFFFFFFF),
            Color(0x94A5B4FC),
            Color(0x576366F1)
        ),
        shadowColor = Color(0x294F46E5)
    ),
    PHOTOS(
        label = "Photos & Gallery",
        bgStops = listOf(
            Color(0xFAFFFFFF),
            Color(0xF0F0F9FF),
            Color(0xE6E0F2FE)
        ),
        borderStops = listOf(
            Color(0xFFFFFFFF),
            Color(0x947DD3FC),
            Color(0x570EA5E9)
        ),
        shadowColor = Color(0x290284C7)
    ),
    FILES(
        label = "Files",
        bgStops = listOf(
            Color(0xFAFFFFFF),
            Color(0xF0F0FDFA),
            Color(0xE0CCFBF1)
        ),
        borderStops = listOf(
            Color(0xFFFFFFFF),
            Color(0x945EEAD4),
            Color(0x5714B8A6)
        ),
        shadowColor = Color(0x290D9488)
    ),
    POLL(
        label = "Poll",
        bgStops = listOf(
            Color(0xFAFFFFFF),
            Color(0xF0F5F3FF),
            Color(0xE6EDE9FE)
        ),
        borderStops = listOf(
            Color(0xFFFFFFFF),
            Color(0x99C4B5FD),
            Color(0x578B5CF6)
        ),
        shadowColor = Color(0x297C3AED)
    )
}

// Exact physics & layout constants matching the web component
const val ACTION_MENU_SPIN_ANGLE = 135f
const val ACTION_MENU_ANIM_DURATION_MS = 500
const val ACTION_MENU_CLOSE_DURATION_MS = 380
val ACTION_MENU_LINE_OFFSET_Y = (-60).dp
val ACTION_MENU_LINE_START_X = 14.dp
val ACTION_MENU_LINE_STEP_X = 52.dp
val ACTION_MENU_HALO_OFFSET_X = 92.dp

// Spring-like overshoot cubic-bezier(0.22, 1.38, 0.32, 1.0)
val ActionMenuPopEasing = CubicBezierEasing(0.22f, 1.38f, 0.32f, 1.0f)

// Smooth deceleration cubic-bezier(0.16, 1.0, 0.3, 1.0)
val ActionMenuSmoothOutEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

/**
 * Reusable Synchronized Rotating Action Menu component with expanding glass pill halo,
 * staggered 3D option nodes, and rotating blue trigger button.
 */
/**
 * Synchronized Rotating Action Menu Nodes Container (4 Floating 3D Action Nodes).
 * Renders cleanly above the input bar directly over the wallpaper without any background plate.
 */
@Composable
fun SynchronizedRotatingActionNodes(
    isOpen: Boolean,
    onOptionSelected: (ActionMenuOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { ActionMenuOption.entries }

    Row(
        modifier = modifier
            .wrapContentSize()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            StaggeredActionNodeItem(
                option = option,
                index = index,
                totalCount = options.size,
                isOpen = isOpen,
                onSelect = { onOptionSelected(option) }
            )
        }
    }
}

/**
 * Reusable Synchronized Rotating Action Menu component.
 */
@Composable
fun SynchronizedRotatingActionMenu(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onOptionSelected: (ActionMenuOption) -> Unit,
    modifier: Modifier = Modifier
) {
    SynchronizedActionMenuTriggerButton(
        isOpen = isOpen,
        onClick = onToggle,
        modifier = modifier
    )
}

@Composable
private fun StaggeredActionNodeItem(
    option: ActionMenuOption,
    index: Int,
    totalCount: Int,
    isOpen: Boolean,
    onSelect: () -> Unit
) {
    val transformAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }
    val selectPulseAnim = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    // Coordinate staggered open and close animations matching the CSS transitions
    LaunchedEffect(isOpen) {
        val staggerDelay = if (isOpen) {
            index * 42L
        } else {
            (totalCount - 1 - index) * 25L
        }

        launch {
            delay(staggerDelay)
            transformAnim.animateTo(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isOpen) ACTION_MENU_ANIM_DURATION_MS else ACTION_MENU_CLOSE_DURATION_MS,
                    easing = if (isOpen) ActionMenuPopEasing else ActionMenuSmoothOutEasing
                )
            )
        }

        launch {
            delay(staggerDelay)
            alphaAnim.animateTo(
                targetValue = if (isOpen) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isOpen) 260 else 220,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "OptionPressScale"
    )

    val currentProgress = transformAnim.value
    val baseScale = 0.18f + (0.82f * currentProgress)
    val combinedScale = baseScale * pressScale * selectPulseAnim.value

    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = alphaAnim.value
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = option.shadowColor,
                spotColor = option.shadowColor
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = option.bgStops,
                    center = Offset(28f, 22f),
                    radius = 110f
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = option.borderStops,
                    start = Offset(0f, 0f),
                    end = Offset(120f, 120f)
                ),
                shape = CircleShape
            )
            .clickable(
                enabled = isOpen,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onSelect()
                }
            )
            .semantics {
                contentDescription = option.label
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Specular Sheen & Glass Rim Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top specular sheen
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xC7FFFFFF),
                        Color(0x29FFFFFF),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.7f, size.height * 0.7f)
                )
            )
            // 1dp inner white glass rim
            drawCircle(
                color = Color(0xD9FFFFFF),
                radius = (size.minDimension / 2f) - 1.5.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Custom 3D Vector Graphic Inside Option Button
        Canvas(modifier = Modifier.size(22.dp)) {
            when (option) {
                ActionMenuOption.CAMERA -> drawCamera3DGraphic()
                ActionMenuOption.PHOTOS -> drawPhotos3DGraphic()
                ActionMenuOption.FILES -> drawFiles3DGraphic()
                ActionMenuOption.POLL -> drawPoll3DGraphic()
            }
        }
    }
}

@Composable
fun SynchronizedActionMenuTriggerButton(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rotation by animateFloatAsState(
        targetValue = if (isOpen) ACTION_MENU_SPIN_ANGLE else 0f,
        animationSpec = tween(
            durationMillis = ACTION_MENU_ANIM_DURATION_MS,
            easing = ActionMenuPopEasing
        ),
        label = "MainTriggerRotation"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "MainTriggerScale"
    )

    val bgColors = if (isOpen) {
        listOf(Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF1D4ED8))
    } else {
        listOf(Color(0xFF60A5FA), Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF1D4ED8))
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (isOpen) 8.dp else 10.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF2563EB),
                spotColor = Color(0xFF2563EB)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = bgColors,
                    center = Offset(34f, 24f),
                    radius = 115f
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x99FFFFFF),
                        Color(0x4093C5FD),
                        Color(0x59FFFFFF)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = if (isOpen) "Close Action Menu" else "Open Action Menu"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Specular Glass Rim + Plus Icon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 1.5.dp.toPx()
            // Top-left inner specular highlight
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x59FFFFFF), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.6f, size.height * 0.6f)
                ),
                radius = (size.minDimension / 2f) - inset
            )
            // Inner glass border ring
            drawCircle(
                color = Color(0x40FFFFFF),
                radius = (size.minDimension / 2f) - inset,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crisp Plus ('+') Icon (20dp equivalent inside 42dp button)
            val iconScale = 20.dp.toPx() / 24f
            val centerOffset = (size.width - 20.dp.toPx()) / 2f
            val strokeWidth = 2.25f * iconScale

            // Horizontal bar (M5 12h14)
            drawLine(
                color = Color.White,
                start = Offset(centerOffset + 5f * iconScale, centerOffset + 12f * iconScale),
                end = Offset(centerOffset + 19f * iconScale, centerOffset + 12f * iconScale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Vertical bar (M12 5v14)
            drawLine(
                color = Color.White,
                start = Offset(centerOffset + 12f * iconScale, centerOffset + 5f * iconScale),
                end = Offset(centerOffset + 12f * iconScale, centerOffset + 19f * iconScale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 1. Camera: Clean 3D Optical Camera Graphic (24x24 coordinate space).
 */
private fun DrawScope.drawCamera3DGraphic() {
    val s = size.width / 24f

    // Soft Ambient Ground Shadow
    drawOval(
        color = Color(0x38312E81),
        topLeft = Offset((12f - 8.2f) * s, (20.4f - 1.4f) * s),
        size = Size(16.4f * s, 2.8f * s)
    )

    // 3D Shutter Button (Left Top)
    drawRoundRect(
        color = Color(0xFFA5B4FC),
        topLeft = Offset(4.6f * s, 5.1f * s),
        size = Size(3.2f * s, 2.0f * s),
        cornerRadius = CornerRadius(0.9f * s, 0.9f * s)
    )

    // 3D Extruded Bottom Rim
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF4338CA), Color(0xFF312E81)),
            start = Offset(2.5f * s, 7.4f * s),
            end = Offset(21.5f * s, 20.0f * s)
        ),
        topLeft = Offset(2.5f * s, 7.4f * s),
        size = Size(19.0f * s, 12.6f * s),
        cornerRadius = CornerRadius(4.2f * s, 4.2f * s)
    )

    // 3D Sculpted Top Prism & Main Camera Body
    val bodyPath = Path().apply {
        moveTo(9.1f * s, 4.3f * s)
        cubicTo(9.45f * s, 3.65f * s, 10.12f * s, 3.25f * s, 10.86f * s, 3.25f * s)
        lineTo(13.14f * s, 3.25f * s)
        cubicTo(13.88f * s, 3.65f * s, 14.55f * s, 3.65f * s, 14.9f * s, 4.3f * s)
        lineTo(15.9f * s, 6.2f * s)
        lineTo(17.3f * s, 6.2f * s)
        cubicTo(19.62f * s, 6.2f * s, 21.5f * s, 8.08f * s, 21.5f * s, 10.4f * s)
        lineTo(21.5f * s, 15.1f * s)
        cubicTo(21.5f * s, 17.42f * s, 19.62f * s, 19.3f * s, 17.3f * s, 19.3f * s)
        lineTo(6.7f * s, 19.3f * s)
        cubicTo(4.38f * s, 19.3f * s, 2.5f * s, 17.42f * s, 2.5f * s, 15.1f * s)
        lineTo(2.5f * s, 10.4f * s)
        cubicTo(2.5f * s, 8.08f * s, 4.38f * s, 6.2f * s, 6.7f * s, 6.2f * s)
        lineTo(8.1f * s, 6.2f * s)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF818CF8), Color(0xFF6366F1), Color(0xFF4338CA)),
            start = Offset(4f * s, 3.5f * s),
            end = Offset(20f * s, 19.3f * s)
        )
    )

    // Top Specular Edge Highlight
    val highlightPath = Path().apply {
        moveTo(6.8f * s, 6.65f * s)
        lineTo(17.2f * s, 6.65f * s)
        cubicTo(19.1f * s, 6.65f * s, 20.65f * s, 8.15f * s, 20.65f * s, 10.05f * s)
    }
    drawPath(
        path = highlightPath,
        color = Color(0x8CFFFFFF),
        style = Stroke(width = 0.75f * s, cap = StrokeCap.Round)
    )

    // Lens Drop Shadow on Body
    drawCircle(
        color = Color(0x521E1B4B),
        radius = 5.15f * s,
        center = Offset(12f * s, 13.45f * s)
    )

    // 3D Protruding Metallic Lens Barrel
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.White, Color(0xFFE0E7FF), Color(0xFFA5B4FC)),
            start = Offset(7f * s, 7.8f * s),
            end = Offset(17f * s, 17.8f * s)
        ),
        radius = 5.0f * s,
        center = Offset(12f * s, 12.8f * s)
    )

    // Deep 3D Optical Glass Lens
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF60A5FA), Color(0xFF3730A3), Color(0xFF1E1B4B)),
            center = Offset(10.9f * s, 11.3f * s),
            radius = 5.2f * s
        ),
        radius = 3.65f * s,
        center = Offset(12f * s, 12.8f * s)
    )

    // Optical Specular Reflections
    drawCircle(
        color = Color(0xE0FFFFFF),
        radius = 1.15f * s,
        center = Offset(10.7f * s, 11.45f * s)
    )
    drawCircle(
        color = Color(0xD993C5FD),
        radius = 0.5f * s,
        center = Offset(13.45f * s, 14.15f * s)
    )

    // Glowing Flash Indicator
    drawCircle(
        color = Color(0xFFFEF08A),
        radius = 1.05f * s,
        center = Offset(18.1f * s, 9.1f * s)
    )
}

/**
 * 2. Photos / Gallery: Clean 3D Layered Landscape Graphic (24x24 coordinate space).
 */
private fun DrawScope.drawPhotos3DGraphic() {
    val s = size.width / 24f

    // Soft Ambient Ground Shadow
    drawOval(
        color = Color(0x380369A1),
        topLeft = Offset((12f - 7.8f) * s, (20.6f - 1.3f) * s),
        size = Size(15.6f * s, 2.6f * s)
    )

    // Back Tilted 3D Photo Card for Depth
    rotate(degrees = 7f, pivot = Offset(12.4f * s, 10.8f * s)) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xA67DD3FC), Color(0xA60284C7)),
                start = Offset(4.8f * s, 3.2f * s),
                end = Offset(20.0f * s, 18.4f * s)
            ),
            topLeft = Offset(4.8f * s, 3.2f * s),
            size = Size(15.2f * s, 15.2f * s),
            cornerRadius = CornerRadius(3.6f * s, 3.6f * s)
        )
    }

    // Front 3D Extruded Base
    drawRoundRect(
        color = Color(0xFF0284C7),
        topLeft = Offset(3.2f * s, 4.8f * s),
        size = Size(16.4f * s, 15.2f * s),
        cornerRadius = CornerRadius(3.8f * s, 3.8f * s)
    )

    // Front Glossy 3D Sky Frame
    val frameRect = RoundRect(
        rect = Rect(Offset(3.2f * s, 4.1f * s), Size(16.4f * s, 15.2f * s)),
        cornerRadius = CornerRadius(3.8f * s, 3.8f * s)
    )
    val frameClipPath = Path().apply { addRoundRect(frameRect) }

    drawPath(
        path = frameClipPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC)),
            start = Offset(6f * s, 4.1f * s),
            end = Offset(16f * s, 19.3f * s)
        )
    )

    // Top Inner Rim Highlight
    drawRoundRect(
        color = Color(0xB3FFFFFF),
        topLeft = Offset(3.6f * s, 4.5f * s),
        size = Size(15.6f * s, 14.4f * s),
        cornerRadius = CornerRadius(3.4f * s, 3.4f * s),
        style = Stroke(width = 0.75f * s)
    )

    // 3D Glowing Sun Orb
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF08A), Color(0xFFFBBF24), Color(0xFFEA580C)),
            center = Offset(7.5f * s, 8.1f * s),
            radius = 3.2f * s
        ),
        radius = 2.25f * s,
        center = Offset(8.2f * s, 8.8f * s)
    )

    // Clip Mountains cleanly inside the 3D Sky Frame
    withTransform({ clipPath(frameClipPath) }) {
        // Far 3D Mountain Peak
        val farPeak = Path().apply {
            moveTo(9.8f * s, 19.3f * s)
            lineTo(14.9f * s, 11.4f * s)
            cubicTo(15.45f * s, 10.55f * s, 16.68f * s, 10.55f * s, 17.23f * s, 11.4f * s)
            lineTo(19.6f * s, 15.1f * s)
            lineTo(19.6f * s, 19.3f * s)
            close()
        }
        drawPath(
            path = farPeak,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF0369A1)),
                start = Offset(13f * s, 10.8f * s),
                end = Offset(18f * s, 19.3f * s)
            )
        )

        // Near 3D Sculpted Mountain Peak
        val nearPeak = Path().apply {
            moveTo(3.2f * s, 16.0f * s)
            lineTo(7.5f * s, 10.6f * s)
            cubicTo(8.12f * s, 9.82f * s, 9.28f * s, 9.82f * s, 9.9f * s, 10.6f * s)
            lineTo(16.8f * s, 19.3f * s)
            lineTo(3.2f * s, 19.3f * s)
            close()
        }
        drawPath(
            path = nearPeak,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF075985)),
                start = Offset(6f * s, 10f * s),
                end = Offset(14f * s, 19.3f * s)
            )
        )
    }
}

/**
 * 3. Files: Clean 3D Sculpted Document Stack Graphic (24x24 coordinate space).
 */
private fun DrawScope.drawFiles3DGraphic() {
    val s = size.width / 24f

    // Soft Ambient Ground Shadow
    drawOval(
        color = Color(0x3D0F766E),
        topLeft = Offset((12f - 7.2f) * s, (20.7f - 1.3f) * s),
        size = Size(14.4f * s, 2.6f * s)
    )

    // Back 3D Folder Backing Sheet
    rotate(degrees = -5f, pivot = Offset(11.1f * s, 12.2f * s)) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF14B8A6), Color(0xFF0F766E)),
                start = Offset(4.5f * s, 4.2f * s),
                end = Offset(17.7f * s, 20.2f * s)
            ),
            topLeft = Offset(4.5f * s, 4.2f * s),
            size = Size(13.2f * s, 16.0f * s),
            cornerRadius = CornerRadius(3.0f * s, 3.0f * s)
        )
    }

    // Helper function for the sculpted front document sheet path
    fun createDocPath(offsetY: Float = 0f): Path = Path().apply {
        val dy = offsetY * s
        moveTo(6.3f * s, 4.3f * s + dy)
        cubicTo(6.3f * s, 3.3f * s + dy, 7.1f * s, 2.5f * s + dy, 8.1f * s, 2.5f * s + dy)
        lineTo(14.2f * s, 2.5f * s + dy)
        lineTo(19.3f * s, 7.6f * s + dy)
        lineTo(19.3f * s, 18.4f * s + dy)
        cubicTo(19.3f * s, 19.4f * s + dy, 18.5f * s, 20.2f * s + dy, 17.5f * s, 20.2f * s + dy)
        lineTo(8.1f * s, 20.2f * s + dy)
        cubicTo(7.1f * s, 20.2f * s + dy, 6.3f * s, 19.4f * s + dy, 6.3f * s, 18.4f * s + dy)
        close()
    }

    // Front 3D Document Drop Shadow
    drawPath(
        path = createDocPath(offsetY = 0.6f),
        color = Color(0x590F766E)
    )

    // Front 3D Sculpted Document Page
    drawPath(
        path = createDocPath(offsetY = 0f),
        brush = Brush.linearGradient(
            colors = listOf(Color.White, Color(0xFFCCFBF1), Color(0xFF5EEAD4)),
            start = Offset(7.5f * s, 2.5f * s),
            end = Offset(18.5f * s, 20.2f * s)
        )
    )

    // Diagonal Cast Shadow Under 3D Corner Flap
    val flapShadowPath = Path().apply {
        moveTo(14.2f * s, 6.4f * s)
        lineTo(19.3f * s, 7.6f * s)
        lineTo(14.2f * s, 9.2f * s)
        close()
    }
    drawPath(path = flapShadowPath, color = Color(0x380F766E))

    // 3D Folded Corner Flap
    val flapPath = Path().apply {
        moveTo(14.2f * s, 2.5f * s)
        lineTo(19.3f * s, 7.6f * s)
        lineTo(15.8f * s, 7.6f * s)
        cubicTo(14.92f * s, 7.6f * s, 14.2f * s, 6.88f * s, 14.2f * s, 6.0f * s)
        close()
    }
    drawPath(
        path = flapPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF0D9488), Color(0xFF2DD4BF), Color(0xFF99F6E4)),
            start = Offset(14.2f * s, 7.6f * s),
            end = Offset(19.3f * s, 2.5f * s)
        )
    )

    // Tactile 3D Raised Content Bars
    val barRadius = CornerRadius(0.82f * s, 0.82f * s)
    drawRoundRect(
        color = Color(0xFF0D9488),
        topLeft = Offset(8.8f * s, 9.6f * s),
        size = Size(4.6f * s, 1.65f * s),
        cornerRadius = barRadius
    )
    drawRoundRect(
        color = Color(0xFF14B8A6),
        topLeft = Offset(8.8f * s, 12.7f * s),
        size = Size(7.8f * s, 1.65f * s),
        cornerRadius = barRadius
    )
    drawRoundRect(
        color = Color(0xFF2DD4BF),
        topLeft = Offset(8.8f * s, 15.8f * s),
        size = Size(6.0f * s, 1.65f * s),
        cornerRadius = barRadius
    )
}

/**
 * 4. Poll: Clean 3D Volumetric Columns & Plinth Graphic (24x24 coordinate space).
 */
private fun DrawScope.drawPoll3DGraphic() {
    val s = size.width / 24f

    // Soft Ambient Ground Shadow
    drawOval(
        color = Color(0x3D4C1D95),
        topLeft = Offset((12f - 8.0f) * s, (20.7f - 1.3f) * s),
        size = Size(16.0f * s, 2.6f * s)
    )

    // 3D Frosted Plinth Base
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFDDD6FE), Color(0xFFA78BFA)),
            start = Offset(3.2f * s, 18.3f * s),
            end = Offset(20.8f * s, 20.5f * s)
        ),
        topLeft = Offset(3.2f * s, 18.3f * s),
        size = Size(17.6f * s, 2.2f * s),
        cornerRadius = CornerRadius(1.1f * s, 1.1f * s)
    )

    val barCorner = CornerRadius(1.6f * s, 1.6f * s)

    // Column 1 (Left - Lavender 3D Bar)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6), Color(0xFF5B21B6)),
            start = Offset(4.5f * s, 11.2f * s),
            end = Offset(8.6f * s, 19.4f * s)
        ),
        topLeft = Offset(4.5f * s, 11.2f * s),
        size = Size(4.1f * s, 8.2f * s),
        cornerRadius = barCorner
    )
    drawOval(
        color = Color(0x8CFFFFFF),
        topLeft = Offset((6.55f - 1.25f) * s, (11.95f - 0.45f) * s),
        size = Size(2.5f * s, 0.9f * s)
    )

    // Column 2 (Center Hero - Deep Royal Violet 3D Bar)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFA78BFA), Color(0xFF7C3AED), Color(0xFF4C1D95)),
            start = Offset(9.95f * s, 4.2f * s),
            end = Offset(14.05f * s, 19.4f * s)
        ),
        topLeft = Offset(9.95f * s, 4.2f * s),
        size = Size(4.1f * s, 15.2f * s),
        cornerRadius = barCorner
    )
    drawOval(
        color = Color(0xA6FFFFFF),
        topLeft = Offset((12.0f - 1.25f) * s, (4.95f - 0.45f) * s),
        size = Size(2.5f * s, 0.9f * s)
    )

    // Column 3 (Right - Vibrant Pink-Magenta 3D Bar)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFF9D174D)),
            start = Offset(15.4f * s, 8.0f * s),
            end = Offset(19.5f * s, 19.4f * s)
        ),
        topLeft = Offset(15.4f * s, 8.0f * s),
        size = Size(4.1f * s, 11.4f * s),
        cornerRadius = barCorner
    )
    drawOval(
        color = Color(0xA6FFFFFF),
        topLeft = Offset((17.45f - 1.25f) * s, (8.75f - 0.45f) * s),
        size = Size(2.5f * s, 0.9f * s)
    )
}
