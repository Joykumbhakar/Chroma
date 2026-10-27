package com.chroma.studio.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot


/**
 * Shared Palette and Shaders for Apple HIG 3D Empty State Graphics.
 * Replicates the ultra-clean Cupertino Blue, Sapphire Recesses, and Pearl Highlights
 * seen in [AppleTrashBinIcon].
 */
object Apple3DShaderPalette {
    val PlinthShadow = Color(0xFF001645)

    fun bodyBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF4CC9FF),
        0.22f to Color(0xFF1A96FF),
        0.52f to Color(0xFF007AFF),
        0.78f to Color(0xFF0051D5),
        1.00f to Color(0xFF002E8A),
        start = Offset(s * 0.16f, s * 0.06f),
        end = Offset(s * 0.84f, s * 0.96f)
    )

    fun cylindricalSheenBrush(s: Float) = Brush.horizontalGradient(
        0.00f to Color(0x8C80DCFF),
        0.14f to Color(0x52FFFFFF),
        0.30f to Color(0x1438B6FF),
        0.55f to Color(0x2E0040B8),
        0.82f to Color(0x5C002270),
        0.93f to Color(0x6147B8FF),
        1.00f to Color(0xA699E5FF),
        startX = 0f,
        endX = s
    )

    fun lidRimBrush(s: Float) = Brush.horizontalGradient(
        0.00f to Color(0xFF66D4FF),
        0.18f to Color(0xFFB8EEFF),
        0.50f to Color(0xFF0073F0),
        0.82f to Color(0xFF004BC7),
        1.00f to Color(0xFF75DAFF),
        startX = 0f,
        endX = s
    )

    fun sapphireRecessBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF001645),
        0.48f to Color(0xFF002B7A),
        1.00f to Color(0xFF0042B0),
        start = Offset(s * 0.2f, 0f),
        end = Offset(s * 0.8f, s)
    )

    fun sapphireBevelBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xD9001030),
        0.45f to Color(0x401A8CFF),
        0.85f to Color(0xE070D6FF),
        1.00f to Color(0xFAC8F4FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun specularStrokeBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xF0FFFFFF),
        0.35f to Color(0x9980DCFF),
        0.70f to Color(0x260051D5),
        1.00f to Color(0xC766D4FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun pearlBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFFFFFFF),
        0.55f to Color(0xFFEDF7FF),
        1.00f to Color(0xFFB0DEFF),
        start = Offset(s * 0.15f, 0f),
        end = Offset(s * 0.85f, s)
    )

    fun skyBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFE0F7FF),
        0.50f to Color(0xFF75D8FF),
        1.00f to Color(0xFF1A96FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun amberFlameBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFFFF099),
        0.50f to Color(0xFFFFB020),
        1.00f to Color(0xFFFF5500),
        start = Offset(s * 0.2f, 0f),
        end = Offset(s * 0.8f, s)
    )
}

/**
 * Shared Apple HIG Cupertino Blue Gradient Palette & Surface Brushes
 * Matches the exact 1000x1000 coordinate space and studio lighting of the Trash Bin icon.
 */
object AppleHigBluePalette {

    fun bodyBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFF56D0FF),
        0.22f to Color(0xFF1E99FF),
        0.52f to Color(0xFF007AFF),
        0.78f to Color(0xFF0051D5),
        1.00f to Color(0xFF002B82),
        start = Offset(140f * scale, 60f * scale),
        end = Offset(860f * scale, 940f * scale)
    )

    fun cylindricalSheenBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0x8C80DCFF),
        0.16f to Color(0x57FFFFFF),
        0.34f to Color(0x1438B6FF),
        0.58f to Color(0x330040B8),
        0.82f to Color(0x61002270),
        0.93f to Color(0x6147B8FF),
        1.00f to Color(0x9E99E5FF),
        start = Offset(150f * scale, 100f * scale),
        end = Offset(850f * scale, 250f * scale)
    )

    fun lidTopBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFF9BE8FF),
        0.28f to Color(0xFF42B9FF),
        0.62f to Color(0xFF007AFF),
        0.88f to Color(0xFF0051D5),
        1.00f to Color(0xFF003599),
        start = Offset(200f * scale, 160f * scale),
        end = Offset(840f * scale, 720f * scale)
    )

    fun secondaryLayerBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFF70D6FF),
        0.45f to Color(0xFF0A84FF),
        0.85f to Color(0xFF0049C2),
        1.00f to Color(0xFF00256E),
        start = Offset(180f * scale, 180f * scale),
        end = Offset(820f * scale, 820f * scale)
    )

    fun handleBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFFA8ECFF),
        0.30f to Color(0xFF3DB8FF),
        0.68f to Color(0xFF007AFF),
        1.00f to Color(0xFF0047BF),
        start = Offset(250f * scale, 150f * scale),
        end = Offset(750f * scale, 650f * scale)
    )

    fun luminousOrbBrush(center: Offset, radius: Float) = Brush.radialGradient(
        0.00f to Color(0xFFFFFFFF),
        0.36f to Color(0xFFA8ECFF),
        0.72f to Color(0xFF29A8FF),
        1.00f to Color(0xFF0055D4),
        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
        radius = radius * 1.25f
    )

    fun capsuleRecessBrush(top: Float, bottom: Float, scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFF001440),
        0.48f to Color(0xFF002975),
        1.00f to Color(0xFF003DA6),
        start = Offset(400f * scale, top * scale),
        end = Offset(600f * scale, bottom * scale)
    )

    fun capsuleBevelBrush(top: Float, bottom: Float, scale: Float) = Brush.linearGradient(
        0.00f to Color(0xD9001238),
        0.45f to Color(0x401A8CFF),
        0.85f to Color(0xE070D6FF),
        1.00f to Color(0xF5B8F0FF),
        start = Offset(350f * scale, top * scale),
        end = Offset(700f * scale, bottom * scale)
    )

    fun specularStrokeBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xF0FFFFFF),
        0.35f to Color(0x9480DCFF),
        0.70f to Color(0x240051D5),
        1.00f to Color(0xC766D4FF),
        start = Offset(160f * scale, 160f * scale),
        end = Offset(840f * scale, 840f * scale)
    )

    fun floorGlowBrush(centerX: Float, centerY: Float, radius: Float, scale: Float) = Brush.radialGradient(
        0.00f to Color(0x570055E0),
        0.55f to Color(0x1C007AFF),
        1.00f to Color(0x00007AFF),
        center = Offset(centerX * scale, centerY * scale),
        radius = radius * scale
    )

    fun DrawScope.drawSoftboxGlossStripes(
        leftTopX: Float,
        topY: Float,
        bottomY: Float,
        rightEdgeX: Float,
        s: Float
    ) {
        val wideGloss = Path().apply {
            moveTo(leftTopX * s, topY * s)
            lineTo((leftTopX + 95f) * s, topY * s)
            lineTo((leftTopX + 155f) * s, bottomY * s)
            lineTo((leftTopX + 60f) * s, bottomY * s)
            close()
        }
        drawPath(wideGloss, color = Color.White.copy(alpha = 0.20f))

        val thinGloss = Path().apply {
            moveTo((leftTopX + 24f) * s, topY * s)
            lineTo((leftTopX + 54f) * s, topY * s)
            lineTo((leftTopX + 114f) * s, bottomY * s)
            lineTo((leftTopX + 86f) * s, bottomY * s)
            close()
        }
        drawPath(thinGloss, color = Color.White.copy(alpha = 0.30f))

        val rightRimCatch = Path().apply {
            moveTo((rightEdgeX - 65f) * s, topY * s)
            lineTo((rightEdgeX - 20f) * s, topY * s)
            lineTo((rightEdgeX - 55f) * s, bottomY * s)
            lineTo((rightEdgeX - 95f) * s, bottomY * s)
            close()
        }
        drawPath(rightRimCatch, color = Color(0xFF99E5FF).copy(alpha = 0.24f))
    }
}

/**
 * Apple HIG Trending Fire Palette (Solar Yellow -> Vibrant Orange -> Crimson Red -> Deep Ruby)
 * Uses the exact same 3D lighting, anodized sheen, and recessed chamber architecture.
 */
object AppleHigFirePalette {

    fun flameBodyBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFFFFE047), // Luminous Solar Yellow Crown
        0.22f to Color(0xFFFF9F0A), // Apple HIG Warm Amber-Orange
        0.50f to Color(0xFFFF5E00), // Blazing Tangerine Orange
        0.78f to Color(0xFFFF2D20), // Apple HIG Vibrant Coral-Red
        1.00f to Color(0xFF8C0014), // Deep Ruby-Crimson Base
        start = Offset(240f * scale, 120f * scale),
        end = Offset(760f * scale, 860f * scale)
    )

    fun flameCylindricalSheenBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0x99FFF078),
        0.16f to Color(0x66FFFFFF),
        0.35f to Color(0x1AFF8800),
        0.60f to Color(0x38B80012),
        0.82f to Color(0x66590009),
        0.93f to Color(0x66FF9500),
        1.00f to Color(0xB3FFDF70),
        start = Offset(220f * scale, 180f * scale),
        end = Offset(780f * scale, 320f * scale)
    )

    fun innerCoreFlameBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFFFFFFFF), // White-Hot Tip
        0.24f to Color(0xFFFFF266), // Brilliant Lemon-Gold
        0.58f to Color(0xFFFFA00A), // Rich Golden Orange
        0.88f to Color(0xFFFF3B30), // Fiery Red-Orange Base
        1.00f to Color(0xFFC40018),
        start = Offset(440f * scale, 340f * scale),
        end = Offset(580f * scale, 820f * scale)
    )

    fun rubyRecessBrush(top: Float, bottom: Float, scale: Float) = Brush.linearGradient(
        0.00f to Color(0xFF380008),
        0.48f to Color(0xFF6E0412),
        1.00f to Color(0xFFB0151E),
        start = Offset(450f * scale, top * scale),
        end = Offset(560f * scale, bottom * scale)
    )

    fun rubyBevelBrush(top: Float, bottom: Float, scale: Float) = Brush.linearGradient(
        0.00f to Color(0xD92B0005),
        0.45f to Color(0x40FF5500),
        0.85f to Color(0xE6FFB340),
        1.00f to Color(0xFAFFF099),
        start = Offset(380f * scale, top * scale),
        end = Offset(660f * scale, bottom * scale)
    )

    fun fireSpecularStrokeBrush(scale: Float) = Brush.linearGradient(
        0.00f to Color(0xF5FFFFFF),
        0.35f to Color(0xA6FFE066),
        0.70f to Color(0x2EFF2D20),
        1.00f to Color(0xD9FFA033),
        start = Offset(220f * scale, 160f * scale),
        end = Offset(780f * scale, 840f * scale)
    )

    fun fireFloorGlowBrush(centerX: Float, centerY: Float, radius: Float, scale: Float) = Brush.radialGradient(
        0.00f to Color(0x66FF3B1F),
        0.52f to Color(0x26FF9500),
        1.00f to Color(0x00FF9500),
        center = Offset(centerX * scale, centerY * scale),
        radius = radius * scale
    )
}

/**
 * Physical Spring Animation Controller for Apple 3D Empty State Graphics.
 * Orchestrates multi-stage spring articulation (hinging lid open, floating up,
 * elastic squash & stretch bounce, and return), mimicking [AppleTrashBinIcon].
 */
@Stable
class AppleGraphicAnimationState {
    var isArticulated by mutableStateOf(false)
        private set

    val articulationProgress = Animatable(0f)
    val squashX = Animatable(1f)
    val squashY = Animatable(1f)
    val tiltAngle = Animatable(0f)

    var isAnimating by mutableStateOf(false)
        private set

    fun toggle() {
        isArticulated = !isArticulated
    }

    suspend fun triggerBellRing() {
        if (isAnimating) return
        isAnimating = true
        coroutineScope {
            launch {
                // Bell swing sequence matching Apple HIG spring physics
                tiltAngle.animateTo(22f, tween(160, easing = FastOutSlowInEasing))
                tiltAngle.animateTo(-19f, tween(200, easing = FastOutSlowInEasing))
                tiltAngle.animateTo(12f, tween(200, easing = FastOutSlowInEasing))
                tiltAngle.animateTo(-7f, tween(180, easing = FastOutSlowInEasing))
                tiltAngle.animateTo(3f, tween(140, easing = FastOutSlowInEasing))
                tiltAngle.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            launch {
                articulationProgress.animateTo(1f, tween(180))
                articulationProgress.animateTo(0f, tween(800))
            }
            launch {
                squashX.animateTo(1.05f, tween(100))
                squashX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            launch {
                squashY.animateTo(0.95f, tween(100))
                squashY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
        isAnimating = false
    }

    suspend fun triggerArticulateAnimation() {
        if (isAnimating) return
        isAnimating = true
        val wasArticulated = isArticulated

        coroutineScope {
            // Stage 1: Smooth spring-hinge open
            isArticulated = true
            launch {
                articulationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            // Stage 2: Hold open for a tactile beat
            delay(520)

            // Stage 3: Spring snap close with physical squash and stretch impact
            launch {
                articulationProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            delay(120)
            launch {
                squashX.animateTo(1.065f, tween(100))
                squashX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                squashY.animateTo(0.935f, tween(100))
                squashY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            delay(350)
            isArticulated = wasArticulated
            isAnimating = false
        }
    }
}

@Composable
fun rememberAppleGraphicAnimationState(): AppleGraphicAnimationState {
    return remember { AppleGraphicAnimationState() }
}

// ─────────────────────────────────────────────────────────────────────────────
// BASE DRAWING SCOPE HELPERS (100x100 COORDINATE SYSTEM)
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFloorShadow(cx: Float = 50f, cy: Float = 88f, rx: Float = 34f, ry: Float = 4.5f) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF003DA6).copy(alpha = 0.28f),
                Color(0xFF007AFF).copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = rx
        ),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f)
    )
}

private fun DrawScope.drawSculptedSquircleBase(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rx: Float
) {
    val s = 100f
    drawRoundRect(
        color = Apple3DShaderPalette.PlinthShadow,
        topLeft = Offset(x, y + 2.4f),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    drawRoundRect(
        brush = Apple3DShaderPalette.bodyBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    drawRoundRect(
        brush = Apple3DShaderPalette.cylindricalSheenBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    drawRoundRect(
        brush = Apple3DShaderPalette.specularStrokeBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx),
        style = Stroke(width = 2.2f)
    )
}

private fun DrawScope.drawSapphireRecessRect(x: Float, y: Float, w: Float, h: Float, rx: Float) {
    val s = 100f
    drawRoundRect(
        brush = Apple3DShaderPalette.sapphireRecessBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    drawRoundRect(
        brush = Apple3DShaderPalette.sapphireBevelBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx),
        style = Stroke(width = 1.8f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HOME GRAPHICS ICON (Sculpted Villa + Hinged Pearl Roof & Interior Beacon)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleHomeGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val openProgress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            // 1. Floor shadow
            drawFloorShadow(50f, 88f, 35f, 4.8f)

            // 2. Interior warm sapphire beacon glow (visible when roof articulates)
            if (openProgress > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5AC8FA).copy(alpha = 0.7f * openProgress),
                            Color(0xFF007AFF).copy(alpha = 0.3f * openProgress),
                            Color.Transparent
                        ),
                        center = Offset(50f, 36f),
                        radius = 24f * openProgress
                    ),
                    radius = 24f * openProgress,
                    center = Offset(50f, 36f)
                )
            }

            // 3. Villa Main Body (Cupertino Blue with tapered gable walls)
            val villaPath = Path().apply {
                moveTo(20f, 44f)
                lineTo(50f, 21f)
                lineTo(80f, 44f)
                lineTo(80f, 74f)
                cubicTo(80f, 80f, 75f, 85f, 69f, 85f)
                lineTo(31f, 85f)
                cubicTo(25f, 85f, 20f, 80f, 20f, 74f)
                close()
            }
            drawPath(villaPath, color = Apple3DShaderPalette.PlinthShadow)
            drawPath(villaPath, brush = Apple3DShaderPalette.bodyBrush(s))
            drawPath(villaPath, brush = Apple3DShaderPalette.cylindricalSheenBrush(s))
            drawPath(villaPath, brush = Apple3DShaderPalette.specularStrokeBrush(s), style = Stroke(2.2f))

            // 4. Sapphire Recessed Arched Doorway
            drawRoundRect(
                brush = Apple3DShaderPalette.sapphireRecessBrush(s),
                topLeft = Offset(38.5f, 52f),
                size = Size(23f, 33f),
                cornerRadius = CornerRadius(11.5f, 11.5f)
            )
            drawRoundRect(
                brush = Apple3DShaderPalette.sapphireBevelBrush(s),
                topLeft = Offset(38.5f, 52f),
                size = Size(23f, 33f),
                cornerRadius = CornerRadius(11.5f, 11.5f),
                style = Stroke(1.8f)
            )

            // 5. Door Knob Beacon & Window
            drawCircle(pearl, radius = 4.2f, center = Offset(50f, 64f))
            drawCircle(Color(0xFF5AC8FA), radius = 2f, center = Offset(50f, 64f))

            // Upper attic circular window
            drawCircle(Apple3DShaderPalette.sapphireRecessBrush(s), radius = 6f, center = Offset(50f, 37f))
            drawCircle(Apple3DShaderPalette.sapphireBevelBrush(s), radius = 6f, center = Offset(50f, 37f), style = Stroke(1.2f))
            drawCircle(Color.White.copy(alpha = 0.85f), radius = 2.2f, center = Offset(50f, 37f))

            // 6. Cantilevered Pearl Roof + Chimney (Opens its top smoothly!)
            withTransform({
                translate(
                    left = -3f * openProgress,
                    top = -14f * openProgress
                )
                rotate(
                    degrees = -24f * openProgress,
                    pivot = Offset(18f, 45f)
                )
            }) {
                // Pearl Chimney
                drawRoundRect(
                    brush = pearl,
                    topLeft = Offset(67f, 17f),
                    size = Size(7.5f, 14f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    brush = Apple3DShaderPalette.lidRimBrush(s),
                    topLeft = Offset(65.5f, 15f),
                    size = Size(10.5f, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Sculpted Cantilevered Roof Peak
                val roofPath = Path().apply {
                    moveTo(12f, 45.5f)
                    lineTo(46.2f, 15.8f)
                    cubicTo(48.4f, 13.9f, 51.6f, 13.9f, 53.8f, 15.8f)
                    lineTo(88f, 45.5f)
                    cubicTo(90.2f, 47.4f, 88.8f, 51f, 85.8f, 51f)
                    lineTo(79.5f, 51f)
                    lineTo(50f, 25.5f)
                    lineTo(20.5f, 51f)
                    lineTo(14.2f, 51f)
                    cubicTo(11.2f, 51f, 9.8f, 47.4f, 12f, 45.5f)
                    close()
                }
                drawPath(roofPath, brush = pearl)
                drawPath(
                    roofPath,
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFCBE6FF)),
                        start = Offset(20f, 15f),
                        end = Offset(80f, 50f)
                    ),
                    style = Stroke(width = 1.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SETTINGS GRAPHICS ICON (8-Tooth Cupertino Gear + Rotary Bearing Core)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleSettingsGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val progress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            withTransform({
                rotate(degrees = 60f * progress, pivot = Offset(50f, 50f))
            }) {
                // 8 Gear Teeth
                val angles = listOf(0f, 45f, 90f, 135f)
                angles.forEach { angle ->
                    rotate(degrees = angle, pivot = Offset(50f, 50f)) {
                        drawRoundRect(
                            brush = Apple3DShaderPalette.lidRimBrush(s),
                            topLeft = Offset(42f, 9f),
                            size = Size(16f, 82f),
                            cornerRadius = CornerRadius(6.5f, 6.5f)
                        )
                        drawRoundRect(
                            brush = Apple3DShaderPalette.specularStrokeBrush(s),
                            topLeft = Offset(42f, 9f),
                            size = Size(16f, 82f),
                            cornerRadius = CornerRadius(6.5f, 6.5f),
                            style = Stroke(1.4f)
                        )
                    }
                }

                // Sculpted Gear Body
                drawCircle(color = Apple3DShaderPalette.PlinthShadow, radius = 31.5f, center = Offset(50f, 52.2f))
                drawCircle(brush = Apple3DShaderPalette.bodyBrush(s), radius = 31.5f, center = Offset(50f, 50f))
                drawCircle(brush = Apple3DShaderPalette.cylindricalSheenBrush(s), radius = 31.5f, center = Offset(50f, 50f))
                drawCircle(brush = Apple3DShaderPalette.specularStrokeBrush(s), radius = 31.5f, center = Offset(50f, 50f), style = Stroke(2.2f))

                // Sapphire Bearing Race
                drawCircle(brush = Apple3DShaderPalette.sapphireRecessBrush(s), radius = 21f, center = Offset(50f, 50f))
                drawCircle(brush = Apple3DShaderPalette.sapphireBevelBrush(s), radius = 21f, center = Offset(50f, 50f), style = Stroke(2.2f))

                // Central Pearl Roller Bearing Core (Expands with spring physics)
                val bearingRadius = 13f + 1.8f * progress
                drawCircle(brush = pearl, radius = bearingRadius, center = Offset(50f, 50f))
                drawCircle(color = Color.White, radius = bearingRadius, center = Offset(50f, 50f), style = Stroke(1.4f))

                // Inner Sapphire Center Pin
                drawCircle(brush = Apple3DShaderPalette.bodyBrush(s), radius = 5.5f, center = Offset(50f, 50f))
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 1.8f, center = Offset(48.5f, 48.5f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. PHOTOS / IMAGE GRAPHICS ICON (Layered Photo Card + Rising Sun & Peaks)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ApplePhotosGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val progress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            // Back angled photo frame
            rotate(degrees = 7f, pivot = Offset(52f, 43f)) {
                drawRoundRect(
                    brush = Apple3DShaderPalette.sapphireRecessBrush(s),
                    topLeft = Offset(21f, 13f),
                    size = Size(62f, 60f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    brush = Apple3DShaderPalette.specularStrokeBrush(s),
                    topLeft = Offset(21f, 13f),
                    size = Size(62f, 60f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(2f)
                )
            }

            // Foreground Photo Card Frame
            drawSculptedSquircleBase(12f, 18f, 66f, 62f, 15f)

            // Sapphire Recessed Landscape Window
            drawSapphireRecessRect(18f, 24f, 54f, 50f, 10f)

            // Inner Window Clip Path with smooth 10f rounded corners (including bottom right)
            val windowClipPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(18f, 24f, 72f, 74f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                )
            }

            clipPath(windowClipPath) {
                // Articulated Photo Landscape (Slides up & tilts gracefully!)
                withTransform({
                    translate(top = -9f * progress)
                    rotate(degrees = -5f * progress, pivot = Offset(50f, 50f))
                }) {
                    // Radiant Pearl Sun
                    drawCircle(pearl, radius = 7.5f, center = Offset(33f, 37f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(33f, 37f),
                            radius = 12f
                        ),
                        radius = 12f,
                        center = Offset(33f, 37f)
                    )

                    // Background Emerald/Sky Mountain
                    val backPeak = Path().apply {
                        moveTo(37f, 74f)
                        lineTo(55f, 46f)
                        cubicTo(57f, 43f, 60.5f, 43f, 62.5f, 46f)
                        lineTo(72f, 61f)
                        lineTo(72f, 64f)
                        cubicTo(72f, 70f, 68f, 74f, 62f, 74f)
                        close()
                    }
                    drawPath(backPeak, brush = sky)

                    // Foreground Pearl Mountain
                    val frontPeak = Path().apply {
                        moveTo(18f, 64f)
                        lineTo(33f, 44f)
                        cubicTo(35.2f, 41.2f, 39f, 41.2f, 41.2f, 44f)
                        lineTo(64f, 74f)
                        lineTo(27f, 74f)
                        cubicTo(22f, 74f, 18f, 70f, 18f, 65f)
                        close()
                    }
                    drawPath(frontPeak, brush = pearl)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. WORKS / CANVAS PALETTE GRAPHICS ICON (Apple HIG 3D Lucide Palette & Sculpted Pen)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents each CNC-milled recessed paint well on the Lucide palette in 1000x1000 master space.
 */
data class PaintWellSpec(
    val index: Int,
    val name: String,
    val center: Offset,
    val highlightColor: Color,
    val midColor: Color,
    val deepColor: Color,
    val abyssColor: Color,
    val causticRimColor: Color,
    val dipDelta: Offset
)

val AppleSystemPaintWells = listOf(
    PaintWellSpec(
        index = 0,
        name = "System Cyan",
        center = Offset(242f, 556f),
        highlightColor = Color(0xFFE0F7FF),
        midColor = Color(0xFF38BDF8),
        deepColor = Color(0xFF007AFF),
        abyssColor = Color(0xFF0040A8),
        causticRimColor = Color(0xFFBAE6FD),
        dipDelta = Offset(-105f, -18f)
    ),
    PaintWellSpec(
        index = 1,
        name = "System Indigo",
        center = Offset(288f, 382f),
        highlightColor = Color(0xFFEDE9FE),
        midColor = Color(0xFF818CF8),
        deepColor = Color(0xFF5856D6),
        abyssColor = Color(0xFF2E1065),
        causticRimColor = Color(0xFFDDD6FE),
        dipDelta = Offset(-116f, -118f)
    ),
    PaintWellSpec(
        index = 2,
        name = "System Coral",
        center = Offset(452f, 284f),
        highlightColor = Color(0xFFFFFBEB),
        midColor = Color(0xFFFB923C),
        deepColor = Color(0xFFFF2D55),
        abyssColor = Color(0xFF881337),
        causticRimColor = Color(0xFFFED7AA),
        dipDelta = Offset(-22f, -168f)
    ),
    PaintWellSpec(
        index = 3,
        name = "System Mint",
        center = Offset(636f, 332f),
        highlightColor = Color(0xFFECFDF5),
        midColor = Color(0xFF34D399),
        deepColor = Color(0xFF059669),
        abyssColor = Color(0xFF064E3B),
        causticRimColor = Color(0xFFA7F3D0),
        dipDelta = Offset(82f, -138f)
    )
)

enum class AppleBlueFinish(
    val label: String,
    val bodyStops: List<Pair<Float, Color>>,
    val extrusionStops: List<Pair<Float, Color>>,
    val crownStops: List<Pair<Float, Color>>
) {
    Cupertino(
        label = "Cupertino Blue",
        bodyStops = listOf(
            0.00f to Color(0xFF56D0FF),
            0.20f to Color(0xFF1E9BFF),
            0.50f to Color(0xFF007AFF),
            0.78f to Color(0xFF0051D5),
            1.00f to Color(0xFF002B82)
        ),
        extrusionStops = listOf(
            0.00f to Color(0xFF0051D5),
            0.35f to Color(0xFF003594),
            0.72f to Color(0xFF001F5C),
            1.00f to Color(0xFF001238)
        ),
        crownStops = listOf(
            0.00f to Color(0xFFA6ECFF),
            0.32f to Color(0xFF42B9FF),
            0.70f to Color(0xFF007AFF),
            1.00f to Color(0xFF0045B8)
        )
    ),
    PacificPro(
        label = "Pacific Pro",
        bodyStops = listOf(
            0.00f to Color(0xFF67E8F9),
            0.22f to Color(0xFF0EA5E9),
            0.52f to Color(0xFF0284C7),
            0.80f to Color(0xFF0369A1),
            1.00f to Color(0xFF0F172A)
        ),
        extrusionStops = listOf(
            0.00f to Color(0xFF0284C7),
            0.40f to Color(0xFF075985),
            0.75f to Color(0xFF0C4A6E),
            1.00f to Color(0xFF082F49)
        ),
        crownStops = listOf(
            0.00f to Color(0xFFCFFAFE),
            0.35f to Color(0xFF38BDF8),
            0.72f to Color(0xFF0284C7),
            1.00f to Color(0xFF0369A1)
        )
    ),
    SequoiaIndigo(
        label = "Sequoia Indigo",
        bodyStops = listOf(
            0.00f to Color(0xFFA5B4FC),
            0.22f to Color(0xFF6366F1),
            0.52f to Color(0xFF4F46E5),
            0.78f to Color(0xFF3730A3),
            1.00f to Color(0xFF1E1B4B)
        ),
        extrusionStops = listOf(
            0.00f to Color(0xFF4338CA),
            0.40f to Color(0xFF312E81),
            0.75f to Color(0xFF1E1B4B),
            1.00f to Color(0xFF0F0D26)
        ),
        crownStops = listOf(
            0.00f to Color(0xFFE0E7FF),
            0.35f to Color(0xFF818CF8),
            0.72f to Color(0xFF4F46E5),
            1.00f to Color(0xFF3730A3)
        )
    )
}

/**
 * State controller managing interactive spring animations, well selection, and studio overlays.
 */
@Stable
class ApplePalettePenState(
    private val scope: CoroutineScope
) {
    var selectedWell by mutableStateOf(AppleSystemPaintWells[0])
        private set

    var isPenPoised by mutableStateOf(false)
    var isColorWells by mutableStateOf(true)
    var showSquircle by mutableStateOf(false)
    var showHigGrid by mutableStateOf(false)
    var isDarkMode by mutableStateOf(false)
    var blueFinish by mutableStateOf(AppleBlueFinish.Cupertino)

    // Continuous 0f..1f progress for the 4-stage Pen Dip & Calligraphic Draw animation
    val strokeTimeline = Animatable(0f)

    // Palette elastic squash & rebound progress (0f = rest, 1f = max squash, <0f = rebound)
    val paletteSquashX = Animatable(1f)
    val paletteSquashY = Animatable(1f)
    val paletteOffsetY = Animatable(0f)

    // Palette remix spin rotation & lift
    val remixRotation = Animatable(0f)
    val remixScale = Animatable(1f)
    val remixOffsetY = Animatable(0f)

    private var isAnimating = false

    fun selectWellAndAnimate(well: PaintWellSpec) {
        selectedWell = well
        triggerPaintStrokeAnimation()
    }

    /**
     * Executes the exact multi-stage Pen Dip -> Calligraphic Sweep -> Palette Squash Rebound sequence.
     */
    fun triggerPaintStrokeAnimation() {
        if (isAnimating) return
        isAnimating = true

        scope.launch {
            coroutineScope {
                // 1. Pen Dip, Sweep, Poised Lift, and Return (1120ms total)
                launch {
                    strokeTimeline.snapTo(0f)
                    strokeTimeline.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1120,
                            easing = CubicBezierEasing(0.32f, 1.0f, 0.56f, 1f)
                        )
                    )
                    strokeTimeline.snapTo(0f)
                }

                // 2. Palette Elastic Spring Squash & Rebound when nib dips into the paint well (240ms delay)
                launch {
                    delay(240)
                    launch {
                        paletteSquashX.animateTo(1.045f, tween(190, easing = FastOutSlowInEasing))
                        paletteSquashX.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        paletteSquashY.animateTo(0.955f, tween(190, easing = FastOutSlowInEasing))
                        paletteSquashY.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        paletteOffsetY.animateTo(11f, tween(190, easing = FastOutSlowInEasing))
                        paletteOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
            isAnimating = false
        }
    }

    /**
     * Executes the Apple Remix Spin oscillation on the palette and poised lift on the pen.
     */
    fun triggerRemixSpin() {
        if (isAnimating) return
        isAnimating = true
        val wasPoised = isPenPoised
        isPenPoised = true

        scope.launch {
            coroutineScope {
                launch {
                    remixRotation.animateTo(-7.5f, tween(160, easing = FastOutSlowInEasing))
                    remixRotation.animateTo(6.8f, tween(180, easing = FastOutSlowInEasing))
                    remixRotation.animateTo(-3.5f, tween(160, easing = FastOutSlowInEasing))
                    remixRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    remixScale.animateTo(1.032f, tween(260))
                    remixScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                }
                launch {
                    remixOffsetY.animateTo(-11f, tween(260))
                    remixOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                }
            }
            if (!wasPoised) isPenPoised = false
            isAnimating = false
        }
    }
}

@Composable
fun rememberApplePalettePenState(): ApplePalettePenState {
    val scope = rememberCoroutineScope()
    return remember { ApplePalettePenState(scope) }
}

/**
 * Master 1000x1000 Apple HIG 3D Glossy Lucide Palette & Pen Icon rendered natively on Jetpack Compose Canvas.
 */
@Composable
fun AppleLucidePaletteAndPenIcon(
    modifier: Modifier = Modifier.size(420.dp),
    state: ApplePalettePenState = rememberApplePalettePenState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // Smooth transitions for poised hover state, color well visibility, and active ink color
    val poisedFraction by animateFloatAsState(
        targetValue = if (state.isPenPoised) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PenPoisedSpring"
    )

    val colorWellsAlpha by animateFloatAsState(
        targetValue = if (state.isColorWells) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "ColorWellsAlpha"
    )

    val squircleScale by animateFloatAsState(
        targetValue = if (state.showSquircle) 1f else 0.91f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "SquircleScale"
    )

    val squircleAlpha by animateFloatAsState(
        targetValue = if (state.showSquircle) 1f else 0f,
        animationSpec = tween(350),
        label = "SquircleAlpha"
    )

    val gridAlpha by animateFloatAsState(
        targetValue = if (state.showHigGrid) 1f else 0f,
        animationSpec = tween(320),
        label = "GridAlpha"
    )

    val activeMidColor by animateColorAsState(
        targetValue = state.selectedWell.midColor,
        animationSpec = tween(300),
        label = "ActiveInkMidColor"
    )

    val activeDeepColor by animateColorAsState(
        targetValue = state.selectedWell.deepColor,
        animationSpec = tween(300),
        label = "ActiveInkDeepColor"
    )

    val finish = state.blueFinish

    val appleBodyBrush = remember(finish) {
        Brush.linearGradient(
            colorStops = finish.bodyStops.toTypedArray(),
            start = Offset(140f, 50f),
            end = Offset(860f, 950f)
        )
    }

    val paletteExtrusionBrush = remember(finish) {
        Brush.linearGradient(
            colorStops = finish.extrusionStops.toTypedArray(),
            start = Offset(80f, 100f),
            end = Offset(920f, 950f)
        )
    }

    val lidTopCrownBrush = remember(finish) {
        Brush.linearGradient(
            colorStops = finish.crownStops.toTypedArray(),
            start = Offset(-45f, -310f),
            end = Offset(45f, 100f)
        )
    }

    val cylindricalSheenBrush = remember {
        Brush.linearGradient(
            0.00f to Color(0xFF88E0FF).copy(alpha = 0.58f),
            0.13f to Color(0xFFFFFFFF).copy(alpha = 0.34f),
            0.28f to Color(0xFF38B6FF).copy(alpha = 0.06f),
            0.54f to Color(0xFF003EB3).copy(alpha = 0.18f),
            0.81f to Color(0xFF001F6B).copy(alpha = 0.38f),
            0.92f to Color(0xFF47B8FF).copy(alpha = 0.40f),
            1.00f to Color(0xFFA3E8FF).copy(alpha = 0.68f),
            start = Offset(100f, 0f),
            end = Offset(900f, 0f)
        )
    }

    val penCylindricalSheenBrush = remember {
        Brush.linearGradient(
            0.00f to Color(0xFF88E0FF).copy(alpha = 0.58f),
            0.13f to Color(0xFFFFFFFF).copy(alpha = 0.34f),
            0.28f to Color(0xFF38B6FF).copy(alpha = 0.06f),
            0.54f to Color(0xFF003EB3).copy(alpha = 0.18f),
            0.81f to Color(0xFF001F6B).copy(alpha = 0.38f),
            0.92f to Color(0xFF47B8FF).copy(alpha = 0.40f),
            1.00f to Color(0xFFA3E8FF).copy(alpha = 0.68f),
            start = Offset(-55f, 0f),
            end = Offset(55f, 0f)
        )
    }

    val capsuleRecessBrush = remember {
        Brush.linearGradient(
            0.00f to Color(0xFF00123B),
            0.46f to Color(0xFF002875),
            1.00f to Color(0xFF003EA8),
            start = Offset(-35f, -55f),
            end = Offset(35f, 55f)
        )
    }

    val capsuleBevelBrush = remember {
        Brush.linearGradient(
            0.00f to Color(0xFF000D2B).copy(alpha = 0.88f),
            0.42f to Color(0xFF1A8CFF).copy(alpha = 0.24f),
            0.82f to Color(0xFF75DAFF).copy(alpha = 0.90f),
            1.00f to Color(0xFFCCF4FF).copy(alpha = 0.98f),
            start = Offset(-55f, -55f),
            end = Offset(55f, 55f)
        )
    }

    val specularStrokeBrush = remember {
        Brush.linearGradient(
            0.00f to Color(0xFFFFFFFF).copy(alpha = 0.95f),
            0.32f to Color(0xFF88E0FF).copy(alpha = 0.62f),
            0.68f to Color(0xFF0051D5).copy(alpha = 0.14f),
            1.00f to Color(0xFF70D6FF).copy(alpha = 0.82f),
            start = Offset(116f, 184f),
            end = Offset(886f, 866f)
        )
    }

    Canvas(
        modifier = modifier.pointerInput(enableClickToAnimate) {
            if (!enableClickToAnimate) return@pointerInput
            detectTapGestures { tapOffset ->
                val minDim = kotlin.math.min(size.width, size.height).toFloat()
                val scale = minDim / 1000f
                val offsetX = (size.width - 1000f * scale) / 2f
                val offsetY = (size.height - 1000f * scale) / 2f

                // Convert pointer tap into 1000x1000 master coordinate space
                val masterPoint = Offset(
                    x = (tapOffset.x - offsetX) / scale,
                    y = (tapOffset.y - offsetY) / scale
                )

                // Check if user tapped directly inside any of the 4 CNC-milled paint wells (r = 58f)
                val tappedWell = AppleSystemPaintWells.firstOrNull { well ->
                    hypot(masterPoint.x - well.center.x, masterPoint.y - well.center.y) <= 62f
                }

                if (tappedWell != null) {
                    state.selectWellAndAnimate(tappedWell)
                } else {
                    state.triggerPaintStrokeAnimation()
                }
                onClick?.invoke()
            }
        }
    ) {
        val masterScale = size.minDimension / 1000f
        val offsetLeft = (size.width - 1000f * masterScale) / 2f
        val offsetTop = (size.height - 1000f * masterScale) / 2f

        withTransform({
            translate(left = offsetLeft, top = offsetTop)
            scale(scaleX = masterScale, scaleY = masterScale, pivot = Offset.Zero)
        }) {
            // =========================================================================
            // 0. OPTIONAL iOS / macOS APP ICON SQUIRCLE PLINTH
            // =========================================================================
            if (squircleAlpha > 0.01f) {
                withTransform({
                    scale(scaleX = squircleScale, scaleY = squircleScale, pivot = Offset(500f, 500f))
                }) {
                    drawIosSquirclePlinth(alpha = squircleAlpha)
                }
            }

            // =========================================================================
            // MAIN LUCIDE PALETTE & PEN GROUP (With Compose Spring Physics)
            // =========================================================================
            withTransform({
                translate(
                    left = 0f,
                    top = state.paletteOffsetY.value + state.remixOffsetY.value
                )
                rotate(
                    degrees = state.remixRotation.value,
                    pivot = Offset(500f, 540f)
                )
                scale(
                    scaleX = state.paletteSquashX.value * state.remixScale.value,
                    scaleY = state.paletteSquashY.value * state.remixScale.value,
                    pivot = Offset(500f, 540f)
                )
            }) {
                // 1. Ambient Blue Floor Shadow
                withTransform({
                    scale(scaleX = 1f, scaleY = 20f / 272f, pivot = Offset(482f, 898f))
                }) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to Color(0xFF0055E0).copy(alpha = 0.34f),
                            0.55f to Color(0xFF007AFF).copy(alpha = 0.11f),
                            1.00f to Color.Transparent,
                            center = Offset(482f, 898f),
                            radius = 272f
                        ),
                        radius = 272f,
                        center = Offset(482f, 898f)
                    )
                }

                // 2. Multi-Layer Apple Studio Drop Shadow under Palette Assembly
                val paletteExtrusionPath = buildLucidePalettePath(yOffset = 24f)
                val paletteTopPath = buildLucidePalettePath(yOffset = 0f)

                drawBlurredPathShadow(
                    path = paletteExtrusionPath,
                    color = Color(0xFF002B78).copy(alpha = 0.30f),
                    dx = 0f,
                    dy = 30f,
                    blurRadius = 26f
                )
                drawBlurredPathShadow(
                    path = paletteExtrusionPath,
                    color = Color(0xFF001D54).copy(alpha = 0.20f),
                    dx = 0f,
                    dy = 10f,
                    blurRadius = 10f
                )

                // 3. 3D CNC-Machined Lower Slab Extrusion Wall (24pt slab thickness)
                drawPath(
                    path = paletteExtrusionPath,
                    brush = paletteExtrusionBrush
                )

                // Bottom Extrusion Rim Specular Catch-Light
                val bottomRimCatch = Path().apply {
                    moveTo(195f, 792f)
                    cubicTo(268f, 864f, 372f, 889f, 476f, 889f)
                    cubicTo(534f, 889f, 572f, 856f, 581f, 808f)
                }
                drawPath(
                    path = bottomRimCatch,
                    color = Color(0xFF38B6FF).copy(alpha = 0.45f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // 4. Primary Clipped Top Surface of the Lucide Palette
                clipPath(path = paletteTopPath) {
                    // Primary Rich Cupertino Blue Gradient Fill
                    drawRect(
                        brush = appleBodyBrush,
                        topLeft = Offset(100f, 170f),
                        size = Size(800f, 720f)
                    )

                    // Cylindrical Metallic Anodized Reflection Pass
                    drawRect(
                        brush = cylindricalSheenBrush,
                        topLeft = Offset(100f, 170f),
                        size = Size(800f, 720f)
                    )

                    // Recessed Mixing Basin in Center-Right of Palette
                    withTransform({
                        rotate(degrees = -14f, pivot = Offset(512f, 482f))
                    }) {
                        drawBlurredOval(
                            center = Offset(512f, 482f),
                            rx = 124f,
                            ry = 84f,
                            color = Color(0xFF001B54).copy(alpha = 0.26f),
                            blurRadius = 12f
                        )
                        drawOval(
                            color = Color(0xFF70D6FF).copy(alpha = 0.25f),
                            topLeft = Offset(515f - 120f, 485f - 80f),
                            size = Size(240f, 160f),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Overhead Dome Crown Sheen
                    drawBlurredOval(
                        center = Offset(435f, 238f),
                        rx = 240f,
                        ry = 48f,
                        color = Color.White.copy(alpha = 0.28f),
                        blurRadius = 14f
                    )

                    // Diagonal Studio Softbox Ribbons (Identical Optical Signature to Trash Bin)
                    val wideRibbon = Path().apply {
                        moveTo(152f, 195f)
                        lineTo(262f, 195f)
                        lineTo(348f, 865f)
                        lineTo(238f, 865f)
                        close()
                    }
                    drawBlurredPath(
                        path = wideRibbon,
                        color = Color.White.copy(alpha = 0.19f),
                        blurRadius = 12f
                    )

                    val coreRibbon = Path().apply {
                        moveTo(192f, 195f)
                        lineTo(224f, 195f)
                        lineTo(310f, 865f)
                        lineTo(280f, 865f)
                        close()
                    }
                    drawBlurredPath(
                        path = coreRibbon,
                        color = Color.White.copy(alpha = 0.31f),
                        blurRadius = 4f
                    )

                    // Right Lobe Secondary Anodized Rim Catch
                    drawBlurredOval(
                        center = Offset(785f, 405f),
                        rx = 92f,
                        ry = 118f,
                        color = Color(0xFF99E5FF).copy(alpha = 0.24f),
                        blurRadius = 14f
                    )

                    // Bottom Continuous Curvature Cushion Highlight
                    val bottomCushionPath = Path().apply {
                        moveTo(210f, 784f)
                        cubicTo(282f, 846f, 384f, 856f, 488f, 850f)
                    }
                    drawPath(
                        path = bottomCushionPath,
                        color = Color(0xFF88E0FF).copy(alpha = 0.52f),
                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                    )

                    // Dynamic Realistic Cast Shadow from Floating Pen onto Palette Surface
                    val penPose = computePenPose(
                        t = state.strokeTimeline.value,
                        poisedFraction = poisedFraction,
                        dipDelta = state.selectedWell.dipDelta
                    )

                    withTransform({
                        translate(left = penPose.shadowTranslateX, top = penPose.shadowTranslateY)
                        rotate(degrees = penPose.shadowRotation, pivot = Offset(545f, 465f))
                        scale(
                            scaleX = penPose.shadowScale,
                            scaleY = penPose.shadowScale,
                            pivot = Offset(545f, 465f)
                        )
                    }) {
                        val surfaceShadowPath = Path().apply {
                            moveTo(466f, 538f)
                            lineTo(685f, 275f)
                            cubicTo(712f, 245f, 755f, 285f, 725f, 318f)
                            lineTo(494f, 552f)
                            close()
                        }
                        drawBlurredPath(
                            path = surfaceShadowPath,
                            color = Color(0xFF000B26).copy(alpha = penPose.shadowAlpha),
                            blurRadius = 18f
                        )
                    }
                }

                // 5. Crisp Specular Outer Diamond-Cut Chamfer on Palette Silhouette
                val outerContourOnly = buildLucidePaletteOuterContour(yOffset = 0f)
                drawPath(
                    path = outerContourOnly,
                    brush = specularStrokeBrush,
                    style = Stroke(width = 3.5f)
                )

                // 6. Recessed Ergonomic Thumbport Bevel & Inner Wall Shadow
                drawCircle(
                    color = Color(0xFF000D2B).copy(alpha = 0.70f),
                    radius = 67f,
                    center = Offset(394f, 744f),
                    style = Stroke(width = 7f)
                )
                withTransform({
                    translate(left = 394f, top = 744f)
                }) {
                    drawCircle(
                        brush = capsuleBevelBrush,
                        radius = 66f,
                        center = Offset.Zero,
                        style = Stroke(width = 4.5f)
                    )
                }

                // =========================================================================
                // 7. FOUR INTERACTIVE CNC-MILLED PAINT WELLS
                // =========================================================================
                AppleSystemPaintWells.forEach { well ->
                    val isSelected = well.index == state.selectedWell.index
                    val wellScale = if (isSelected) 1.04f else 1f

                    withTransform({
                        translate(left = well.center.x, top = well.center.y)
                        scale(scaleX = wellScale, scaleY = wellScale, pivot = Offset.Zero)
                    }) {
                        // Deep Sapphire Recessed Channel
                        drawCircle(
                            brush = capsuleRecessBrush,
                            radius = 55f,
                            center = Offset.Zero
                        )

                        // Inner Top-Left Machined Wall Shadow
                        drawArc(
                            color = Color(0xFF000A24).copy(alpha = 0.75f),
                            startAngle = 144f,
                            sweepAngle = 138f,
                            useCenter = false,
                            topLeft = Offset(-49f, -49f),
                            size = Size(98f, 98f),
                            style = Stroke(width = 8.5f, cap = StrokeCap.Round)
                        )

                        // Subsurface Liquid Acrylic Paint Dome
                        if (colorWellsAlpha > 0.01f) {
                            val domeScale = 0.75f + 0.25f * colorWellsAlpha
                            withTransform({
                                scale(scaleX = domeScale, scaleY = domeScale, pivot = Offset.Zero)
                            }) {
                                val dropletBrush = Brush.radialGradient(
                                    0.00f to well.highlightColor.copy(alpha = colorWellsAlpha),
                                    0.32f to well.midColor.copy(alpha = colorWellsAlpha),
                                    0.72f to well.deepColor.copy(alpha = colorWellsAlpha),
                                    0.92f to well.abyssColor.copy(alpha = colorWellsAlpha),
                                    1.00f to well.midColor.copy(alpha = colorWellsAlpha),
                                    center = Offset(-13f, -15f),
                                    radius = 68f
                                )
                                drawCircle(
                                    brush = dropletBrush,
                                    radius = 41f,
                                    center = Offset(0f, 2f)
                                )

                                // Subsurface Bottom-Right Caustic Glow
                                drawArc(
                                    color = well.causticRimColor.copy(alpha = 0.75f * colorWellsAlpha),
                                    startAngle = 18f,
                                    sweepAngle = 104f,
                                    useCenter = false,
                                    topLeft = Offset(-34f, -32f),
                                    size = Size(68f, 68f),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                                )

                                // Crisp Studio Softbox Specular Meniscus Highlight
                                withTransform({
                                    rotate(degrees = -32f, pivot = Offset(-14f, -14f))
                                }) {
                                    drawOval(
                                        color = Color.White.copy(alpha = 0.88f * colorWellsAlpha),
                                        topLeft = Offset(-14f - 14f, -14f - 7.5f),
                                        size = Size(28f, 15f)
                                    )
                                }
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.65f * colorWellsAlpha),
                                    radius = 3f,
                                    center = Offset(-4f, -22f)
                                )
                            }
                        }

                        // Precision Anodized Outer Bevel Rim
                        drawCircle(
                            brush = capsuleBevelBrush,
                            radius = 55f,
                            center = Offset.Zero,
                            style = Stroke(width = 4.5f)
                        )
                    }
                }

                // =========================================================================
                // 8. ACTIVE INK CONTACT GLOW & CALLIGRAPHIC STROKE RIBBON
                // =========================================================================
                val glowScale = 1f + 0.38f * poisedFraction
                val glowAlpha = 0.48f + 0.47f * poisedFraction
                withTransform({
                    scale(scaleX = glowScale, scaleY = glowScale, pivot = Offset(472f, 534f))
                }) {
                    drawBlurredOval(
                        center = Offset(472f, 534f),
                        rx = 48f,
                        ry = 26f,
                        color = activeMidColor.copy(alpha = glowAlpha),
                        blurRadius = 14f
                    )
                }

                // Animated Calligraphic Vector Stroke Ribbon (Trimmed via PathMeasure)
                val t = state.strokeTimeline.value
                if (t > 0.16f) {
                    val ribbonAlpha = when {
                        t < 0.24f -> ((t - 0.16f) / 0.08f).coerceIn(0f, 1f)
                        t < 0.74f -> 1f
                        else -> (1f - (t - 0.74f) / 0.26f).coerceIn(0f, 1f)
                    }

                    val fullRibbonPath = Path().apply {
                        moveTo(288f, 382f)
                        cubicTo(332f, 492f, 418f, 575f, 508f, 518f)
                        cubicTo(590f, 468f, 648f, 498f, 728f, 432f)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullRibbonPath, false)
                    val totalLength = pathMeasure.length

                    val endFraction = ((t - 0.16f) / 0.58f).coerceIn(0f, 1f)
                    val startFraction = if (t > 0.74f) {
                        ((t - 0.74f) / 0.26f * 0.22f).coerceIn(0f, 0.3f)
                    } else 0f

                    val trimmedRibbon = Path()
                    pathMeasure.getSegment(
                        startDistance = totalLength * startFraction,
                        stopDistance = totalLength * endFraction,
                        destination = trimmedRibbon,
                        startWithMoveTo = true
                    )

                    val ribbonBrush = Brush.linearGradient(
                        0.00f to Color.White.copy(alpha = ribbonAlpha),
                        0.38f to activeMidColor.copy(alpha = ribbonAlpha),
                        1.00f to activeDeepColor.copy(alpha = ribbonAlpha),
                        start = Offset(288f, 382f),
                        end = Offset(728f, 432f)
                    )

                    drawPath(
                        path = trimmedRibbon,
                        brush = ribbonBrush,
                        style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = trimmedRibbon,
                        color = Color.White.copy(alpha = 0.85f * ribbonAlpha),
                        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // =========================================================================
                // 9. LUCIDE SCULPTED STUDIO PEN (45.0° Optical Axis + Spring Kinematics)
                // =========================================================================
                val penPose = computePenPose(
                    t = state.strokeTimeline.value,
                    poisedFraction = poisedFraction,
                    dipDelta = state.selectedWell.dipDelta
                )

                withTransform({
                    translate(left = penPose.translateX, top = penPose.translateY)
                    rotate(degrees = penPose.rotation, pivot = Offset(640f, 360f))
                    scale(scaleX = penPose.scale, scaleY = penPose.scale, pivot = Offset(640f, 360f))
                    translate(left = 644f, top = 362f)
                    rotate(degrees = 45f, pivot = Offset.Zero)
                }) {
                    drawSculptedApplePen(
                        appleBodyBrush = appleBodyBrush,
                        lidTopCrownBrush = lidTopCrownBrush,
                        penCylindricalSheenBrush = penCylindricalSheenBrush,
                        capsuleRecessBrush = capsuleRecessBrush,
                        activeMidColor = activeMidColor
                    )
                }
            }

            // =========================================================================
            // 10. APPLE HIG 1024×1024 OPTICAL CONSTRUCTION GRID OVERLAY
            // =========================================================================
            if (gridAlpha > 0.01f) {
                drawAppleHigConstructionGrid(alpha = gridAlpha)
            }
        }
    }
}

/**
 * Renders the local-space 45° Apple Studio Pen assembly (Nib, Ferrule, Anodized Barrel, and Pocket Clip).
 */
private fun DrawScope.drawSculptedApplePen(
    appleBodyBrush: Brush,
    lidTopCrownBrush: Brush,
    penCylindricalSheenBrush: Brush,
    capsuleRecessBrush: Brush,
    activeMidColor: Color
) {
    val barrelPath = Path().apply {
        moveTo(-47f, -258f)
        cubicTo(-47f, -292f, -25f, -314f, 0f, -314f)
        cubicTo(25f, -314f, 47f, -292f, 47f, -258f)
        lineTo(42f, 86f)
        cubicTo(42f, 96f, 35f, 102f, 24f, 102f)
        lineTo(-24f, 102f)
        cubicTo(-35f, 102f, -42f, 96f, -42f, 86f)
        close()
    }

    val nibConePath = Path().apply {
        moveTo(-38f, 96f)
        lineTo(38f, 96f)
        lineTo(22f, 176f)
        cubicTo(16f, 204f, 7f, 228f, 0f, 244f)
        cubicTo(-7f, 228f, -16f, 204f, -22f, 176f)
        close()
    }

    // Floating Drop Shadow beneath Pen Assembly
    drawBlurredPathShadow(
        path = barrelPath,
        color = Color(0xFF001542).copy(alpha = 0.38f),
        dx = -16f,
        dy = 28f,
        blurRadius = 18f
    )
    drawBlurredPathShadow(
        path = nibConePath,
        color = Color(0xFF001542).copy(alpha = 0.34f),
        dx = -12f,
        dy = 22f,
        blurRadius = 14f
    )

    // ---------------- 1. PRECISION FOUNTAIN / STYLUS NIB & FERRULE ----------------
    val nibBrush = Brush.linearGradient(
        0.00f to Color(0xFFF0FAFF),
        0.24f to Color(0xFF8AE2FF),
        0.56f to Color(0xFF0A84FF),
        0.84f to Color(0xFF0044B5),
        1.00f to Color(0xFF001E5A),
        start = Offset(-38f, 100f),
        end = Offset(38f, 240f)
    )
    drawPath(path = nibConePath, brush = nibBrush)

    // Right Facet Shaded Depth on Nib Cone
    val rightNibFacet = Path().apply {
        moveTo(0f, 96f)
        lineTo(38f, 96f)
        lineTo(22f, 176f)
        cubicTo(16f, 204f, 7f, 228f, 0f, 244f)
        close()
    }
    drawPath(path = rightNibFacet, color = Color(0xFF001A57).copy(alpha = 0.34f))

    // Left Facet Specular Highlight Streak
    drawLine(
        color = Color.White.copy(alpha = 0.78f),
        start = Offset(-29f, 102f),
        end = Offset(-12f, 198f),
        strokeWidth = 5.5f,
        cap = StrokeCap.Round
    )

    // Precision Machined Ferrule Collar Band
    val ferruleBrush = Brush.linearGradient(
        0.00f to Color(0xFF9BE7FF),
        0.22f to Color(0xFFFFFFFF),
        0.52f to Color(0xFF38B2FF),
        0.82f to Color(0xFF0040B0),
        1.00f to Color(0xFF80DCFF),
        start = Offset(-41f, 88f),
        end = Offset(41f, 88f)
    )
    drawRoundRect(
        brush = ferruleBrush,
        topLeft = Offset(-41f, 88f),
        size = Size(82f, 14f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF002B80),
        topLeft = Offset(-41f, 88f),
        size = Size(82f, 14f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.5f)
    )

    // Classic Lucide Fountain Capillary Slit & Breather Hole
    drawLine(
        color = Color(0xFF001238),
        start = Offset(0f, 154f),
        end = Offset(0f, 238f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFF001238),
        radius = 8f,
        center = Offset(0f, 150f)
    )
    drawCircle(
        color = activeMidColor,
        radius = 8f,
        center = Offset(0f, 150f),
        style = Stroke(width = 2.5f)
    )

    // Dynamic Glowing Ink Tip Droplet
    drawCircle(
        color = activeMidColor,
        radius = 8f,
        center = Offset(0f, 242f)
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = Offset(-2f, 240f)
    )

    // Outer Specular Bevel on Nib Cone
    val penSpecularBrush = Brush.linearGradient(
        0.00f to Color.White.copy(alpha = 0.92f),
        0.50f to Color(0xFF88E0FF).copy(alpha = 0.55f),
        1.00f to Color(0xFF70D6FF).copy(alpha = 0.80f),
        start = Offset(-47f, -314f),
        end = Offset(47f, 244f)
    )
    drawPath(
        path = nibConePath,
        brush = penSpecularBrush,
        style = Stroke(width = 2.2f)
    )

    // ---------------- 2. UNIBODY ANODIZED PEN BARREL ----------------
    clipPath(path = barrelPath) {
        drawRect(
            brush = appleBodyBrush,
            topLeft = Offset(-55f, -320f),
            size = Size(110f, 430f)
        )
        drawRect(
            brush = penCylindricalSheenBrush,
            topLeft = Offset(-55f, -320f),
            size = Size(110f, 430f)
        )

        // Top Dome Crown Cap Surface
        drawOval(
            brush = lidTopCrownBrush,
            topLeft = Offset(-50f, -268f - 34f),
            size = Size(100f, 68f)
        )
        val crownHighlight = Path().apply {
            moveTo(-38f, -258f)
            cubicTo(-18f, -248f, 18f, -248f, 38f, -258f)
        }
        drawPath(
            path = crownHighlight,
            color = Color.White.copy(alpha = 0.65f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Recessed Sapphire Grip Channel Band
        drawRect(
            brush = capsuleRecessBrush,
            topLeft = Offset(-50f, 34f),
            size = Size(100f, 20f)
        )
        drawLine(
            color = Color(0xFF000A24).copy(alpha = 0.85f),
            start = Offset(-50f, 35f),
            end = Offset(50f, 35f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF88E0FF).copy(alpha = 0.80f),
            start = Offset(-50f, 54f),
            end = Offset(50f, 54f),
            strokeWidth = 2.2f
        )

        // Recessed Center Barrel Precision Ring
        drawRect(
            brush = capsuleRecessBrush,
            topLeft = Offset(-50f, -138f),
            size = Size(100f, 12f)
        )
        drawLine(
            color = Color(0xFF75DAFF).copy(alpha = 0.70f),
            start = Offset(-50f, -126f),
            end = Offset(50f, -126f),
            strokeWidth = 2f
        )

        // Long Vertical Studio Softbox Highlight Strip Down Left of Barrel
        drawRoundRect(
            color = Color.White.copy(alpha = 0.32f),
            topLeft = Offset(-33f, -298f),
            size = Size(14f, 390f),
            cornerRadius = CornerRadius(7f, 7f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.58f),
            topLeft = Offset(-26f, -292f),
            size = Size(5.5f, 380f),
            cornerRadius = CornerRadius(2.75f, 2.75f)
        )

        // Right Edge Rim Catch
        drawRoundRect(
            color = Color(0xFF99E5FF).copy(alpha = 0.30f),
            topLeft = Offset(25f, -288f),
            size = Size(9f, 375f),
            cornerRadius = CornerRadius(4.5f, 4.5f)
        )
    }

    // Crisp Specular Outer Bevel Contour on Pen Barrel
    drawPath(
        path = barrelPath,
        brush = penSpecularBrush,
        style = Stroke(width = 2.8f)
    )

    // ---------------- 3. SCULPTED POCKET CLIP ARCH ----------------
    // Clip Cast Shadow on Barrel
    drawRoundRect(
        color = Color(0xFF000E30).copy(alpha = 0.48f),
        topLeft = Offset(-10f, -244f),
        size = Size(24f, 154f),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Sculpted Anodized Clip Body
    drawRoundRect(
        brush = lidTopCrownBrush,
        topLeft = Offset(-12f, -252f),
        size = Size(24f, 150f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        brush = penSpecularBrush,
        topLeft = Offset(-12f, -252f),
        size = Size(24f, 150f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2.4f)
    )

    // Recessed Sapphire Slot inside Clip
    drawRoundRect(
        brush = capsuleRecessBrush,
        topLeft = Offset(-4.5f, -228f),
        size = Size(9f, 102f),
        cornerRadius = CornerRadius(4.5f, 4.5f)
    )

    // Top Specular Highlight Ridge on Clip
    drawLine(
        color = Color.White.copy(alpha = 0.88f),
        start = Offset(-2.5f, -242f),
        end = Offset(-2.5f, -114f),
        strokeWidth = 3.2f,
        cap = StrokeCap.Round
    )
}

/**
 * Builds the exact G2-continuous Lucide Palette silhouette with the EvenOdd ergonomic thumbport cutout.
 */
private fun buildLucidePalettePath(yOffset: Float): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addPath(buildLucidePaletteOuterContour(yOffset))
    // Ergonomic Thumbport Cutout at (394, 744 + yOffset) with r = 66f
    addOval(
        Rect(
            left = 328f,
            top = 678f + yOffset,
            right = 460f,
            bottom = 810f + yOffset
        )
    )
}

private fun buildLucidePaletteOuterContour(yOffset: Float): Path = Path().apply {
    moveTo(476f, 184f + yOffset)
    cubicTo(258f, 184f + yOffset, 116f, 336f + yOffset, 116f, 530f + yOffset)
    cubicTo(116f, 724f + yOffset, 268f, 866f + yOffset, 476f, 866f + yOffset)
    cubicTo(542f, 866f + yOffset, 584f, 824f + yOffset, 584f, 770f + yOffset)
    cubicTo(584f, 742f + yOffset, 570f, 718f + yOffset, 552f, 698f + yOffset)
    cubicTo(534f, 678f + yOffset, 522f, 656f + yOffset, 522f, 628f + yOffset)
    cubicTo(522f, 580f + yOffset, 560f, 544f + yOffset, 612f, 544f + yOffset)
    lineTo(696f, 544f + yOffset)
    cubicTo(810f, 544f + yOffset, 886f, 466f + yOffset, 886f, 358f + yOffset)
    cubicTo(886f, 242f + yOffset, 704f, 184f + yOffset, 476f, 184f + yOffset)
    close()
}

private data class PenPoseKinematics(
    val translateX: Float,
    val translateY: Float,
    val rotation: Float,
    val scale: Float,
    val shadowTranslateX: Float,
    val shadowTranslateY: Float,
    val shadowRotation: Float,
    val shadowScale: Float,
    val shadowAlpha: Float
)

/**
 * Computes the exact 4-keyframe Pen & Shadow kinematics matching `applePenDipAndDraw` and `pen-poised`.
 */
private fun computePenPose(
    t: Float,
    poisedFraction: Float,
    dipDelta: Offset
): PenPoseKinematics {
    if (t <= 0.001f) {
        return PenPoseKinematics(
            translateX = 26f * poisedFraction,
            translateY = -36f * poisedFraction,
            rotation = 9.5f * poisedFraction,
            scale = 1f + 0.032f * poisedFraction,
            shadowTranslateX = 34f * poisedFraction,
            shadowTranslateY = 16f * poisedFraction,
            shadowRotation = 7f * poisedFraction,
            shadowScale = 1f - 0.12f * poisedFraction,
            shadowAlpha = 0.45f - 0.21f * poisedFraction
        )
    }

    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f.coerceIn(0f, 1f)

    return when {
        // Stage 1 (0% -> 22%): Dip into active paint well
        t < 0.22f -> {
            val f = t / 0.22f
            PenPoseKinematics(
                translateX = lerp(0f, dipDelta.x, f),
                translateY = lerp(0f, dipDelta.y, f),
                rotation = lerp(0f, -13f, f),
                scale = lerp(1f, 0.97f, f),
                shadowTranslateX = lerp(0f, dipDelta.x, f),
                shadowTranslateY = lerp(0f, dipDelta.y, f),
                shadowRotation = 0f,
                shadowScale = lerp(1f, 0.92f, f),
                shadowAlpha = lerp(0.45f, 0.60f, f)
            )
        }
        // Stage 2 (22% -> 54%): Fluid calligraphic sweep across mixing basin
        t < 0.54f -> {
            val f = (t - 0.22f) / 0.32f
            PenPoseKinematics(
                translateX = lerp(dipDelta.x, -14f, f),
                translateY = lerp(dipDelta.y, 34f, f),
                rotation = lerp(-13f, 6.5f, f),
                scale = lerp(0.97f, 1.035f, f),
                shadowTranslateX = lerp(dipDelta.x, -8f, f),
                shadowTranslateY = lerp(dipDelta.y, 38f, f),
                shadowRotation = 0f,
                shadowScale = lerp(0.92f, 1.02f, f),
                shadowAlpha = lerp(0.60f, 0.50f, f)
            )
        }
        // Stage 3 (54% -> 80%): Poised follow-through lift
        t < 0.80f -> {
            val f = (t - 0.54f) / 0.26f
            PenPoseKinematics(
                translateX = lerp(-14f, 32f, f),
                translateY = lerp(34f, -26f, f),
                rotation = lerp(6.5f, 11f, f),
                scale = lerp(1.035f, 1.02f, f),
                shadowTranslateX = lerp(-8f, 38f, f),
                shadowTranslateY = lerp(38f, -12f, f),
                shadowRotation = lerp(0f, 7f, f),
                shadowScale = lerp(1.02f, 0.85f, f),
                shadowAlpha = lerp(0.50f, 0.22f, f)
            )
        }
        // Stage 4 (80% -> 100%): Settle smoothly back to 45° rest position
        else -> {
            val f = (t - 0.80f) / 0.20f
            PenPoseKinematics(
                translateX = lerp(32f, 0f, f),
                translateY = lerp(-26f, 0f, f),
                rotation = lerp(11f, 0f, f),
                scale = lerp(1.02f, 1f, f),
                shadowTranslateX = lerp(38f, 0f, f),
                shadowTranslateY = lerp(-12f, 0f, f),
                shadowRotation = lerp(7f, 0f, f),
                shadowScale = lerp(0.85f, 1f, f),
                shadowAlpha = lerp(0.22f, 0.45f, f)
            )
        }
    }
}

private fun DrawScope.drawIosSquirclePlinth(alpha: Float) {
    // 3D Plinth Bottom Lip Extrusion
    drawRoundRect(
        color = Color(0xFFB8D3FA).copy(alpha = alpha),
        topLeft = Offset(88f, 98f),
        size = Size(824f, 824f),
        cornerRadius = CornerRadius(192f, 192f)
    )
    // Main Ceramic Glass Squircle Surface
    val squircleBrush = Brush.linearGradient(
        0.00f to Color.White.copy(alpha = alpha),
        0.48f to Color(0xFFF4F8FF).copy(alpha = alpha),
        0.85f to Color(0xFFE1EDFF).copy(alpha = alpha),
        1.00f to Color(0xFFCFE2FF).copy(alpha = alpha),
        start = Offset(150f, 88f),
        end = Offset(850f, 912f)
    )
    drawRoundRect(
        brush = squircleBrush,
        topLeft = Offset(88f, 88f),
        size = Size(824f, 824f),
        cornerRadius = CornerRadius(192f, 192f)
    )
    drawRoundRect(
        color = Color(0xFF9AC2FF).copy(alpha = 0.85f * alpha),
        topLeft = Offset(88f, 88f),
        size = Size(824f, 824f),
        cornerRadius = CornerRadius(192f, 192f),
        style = Stroke(width = 3.5f)
    )
}

private fun DrawScope.drawAppleHigConstructionGrid(alpha: Float) {
    val gridBlue = Color(0xFF007AFF).copy(alpha = 0.55f * alpha)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    // Squircle Safe Area Boundary (824x824)
    drawRoundRect(
        color = gridBlue,
        topLeft = Offset(88f, 88f),
        size = Size(824f, 824f),
        cornerRadius = CornerRadius(192f, 192f),
        style = Stroke(width = 1.8f, pathEffect = dashEffect)
    )

    // Golden Ratio Concentric Keyline Circles
    drawCircle(color = gridBlue, radius = 384f, center = Offset(500f, 500f), style = Stroke(width = 1.4f))
    drawCircle(color = gridBlue, radius = 272f, center = Offset(500f, 500f), style = Stroke(width = 1.4f))
    drawCircle(
        color = gridBlue,
        radius = 168f,
        center = Offset(500f, 500f),
        style = Stroke(width = 1.4f, pathEffect = dashEffect)
    )

    // Orthogonal & Diagonal Optical Axes
    drawLine(color = gridBlue, start = Offset(88f, 500f), end = Offset(912f, 500f), strokeWidth = 1.2f)
    drawLine(color = gridBlue, start = Offset(500f, 88f), end = Offset(500f, 912f), strokeWidth = 1.2f)
    drawLine(color = gridBlue, start = Offset(150f, 150f), end = Offset(850f, 850f), strokeWidth = 1.2f)

    // 45.0° Pen Trajectory Axis (Magenta Keyline)
    drawLine(
        color = Color(0xFFFF2D55).copy(alpha = 0.80f * alpha),
        start = Offset(260f, 746f),
        end = Offset(860f, 146f),
        strokeWidth = 2f,
        pathEffect = dashEffect
    )

    // Paint Well Center Crosshairs
    val crosshairColor = Color(0xFF00C7BE).copy(alpha = 0.90f * alpha)
    AppleSystemPaintWells.forEach { well ->
        val c = well.center
        drawLine(crosshairColor, Offset(c.x - 16f, c.y), Offset(c.x + 16f, c.y), strokeWidth = 2f)
        drawLine(crosshairColor, Offset(c.x, c.y - 16f), Offset(c.x, c.y + 16f), strokeWidth = 2f)
    }
}

/**
 * Native Android Canvas BlurMaskFilter helpers so softbox highlights and drop shadows match SVG filters.
 */
private fun DrawScope.drawBlurredPathShadow(
    path: Path,
    color: Color,
    dx: Float,
    dy: Float,
    blurRadius: Float
) {
    drawIntoCanvas { canvas ->
        val frameworkPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
        canvas.save()
        canvas.translate(dx, dy)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), frameworkPaint)
        canvas.restore()
    }
}

private fun DrawScope.drawBlurredPath(
    path: Path,
    color: Color,
    blurRadius: Float
) {
    drawBlurredPathShadow(path = path, color = color, dx = 0f, dy = 0f, blurRadius = blurRadius)
}

private fun DrawScope.drawBlurredOval(
    center: Offset,
    rx: Float,
    ry: Float,
    color: Color,
    blurRadius: Float
) {
    val ovalPath = Path().apply {
        addOval(Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry))
    }
    drawBlurredPath(path = ovalPath, color = color, blurRadius = blurRadius)
}

@Composable
fun AppleWorksGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val paletteState = rememberApplePalettePenState()

    LaunchedEffect(state.isAnimating) {
        if (state.isAnimating) {
            paletteState.triggerPaintStrokeAnimation()
        }
    }

    AppleLucidePaletteAndPenIcon(
        modifier = modifier,
        state = paletteState,
        enableClickToAnimate = enableClickToAnimate,
        onClick = {
            scope.launch {
                state.triggerArticulateAnimation()
            }
            onClick?.invoke()
        }
    )
}

@Composable
fun AppleCanvasPaletteGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    AppleWorksGraphic(
        modifier = modifier,
        state = state,
        enableClickToAnimate = enableClickToAnimate,
        onClick = onClick
    )
}

@Composable
fun AppleCanvasPaletteIcon(
    modifier: Modifier = Modifier.size(140.dp),
    onClick: (() -> Unit)? = null
) {
    AppleWorksGraphic(
        modifier = modifier,
        enableClickToAnimate = true,
        onClick = onClick
    )
}

/**
 * Complete Interactive Apple HIG Senior Design Studio Screen with Frosted Glass Controller Dock.
 */
@Composable
fun AppleLucidePaletteStudioScreen() {
    val state = rememberApplePalettePenState()

    val bgBrush = if (state.isDarkMode) {
        Brush.radialGradient(
            0.0f to Color(0xFF0E1C3E),
            0.55f to Color(0xFF070C1A),
            1.0f to Color(0xFF03060E)
        )
    } else {
        Brush.radialGradient(
            0.0f to Color(0xFFFFFFFF),
            0.55f to Color(0xFFEBF3FF),
            1.0f to Color(0xFFDAE6F8)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = bgBrush)
            .padding(16.dp)
    ) {
        // Top Studio Header Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (state.isDarkMode) Color(0xCC0D1426) else Color(0xCCFFFFFF)
                    )
                    .border(1.dp, Color(0x33007AFF), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(state.selectedWell.midColor)
                )
                Text(
                    text = "APPLE HIG ICON STUDIO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isDarkMode) Color.White else Color(0xFF1E293B)
                )
                Text(
                    text = state.selectedWell.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF)
                )
            }
        }

        // Center Interactive 3D Palette & Pen Canvas
        AppleLucidePaletteAndPenIcon(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp),
            state = state
        )

        // Bottom Frosted Glass Controller Dock
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tip: Tap any of the 4 paint wells on the palette to dip & switch ink color",
                fontSize = 11.sp,
                color = if (state.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (state.isDarkMode) Color(0xD90D1426) else Color(0xD9FFFFFF)
                    )
                    .border(1.dp, Color(0x26007AFF), RoundedCornerShape(18.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioActionButton(
                    label = "Animate Stroke",
                    isPrimary = true,
                    isActive = false,
                    isDark = state.isDarkMode,
                    onClick = { state.triggerPaintStrokeAnimation() }
                )
                StudioActionButton(
                    label = "Remix Spin",
                    isPrimary = false,
                    isActive = false,
                    isDark = state.isDarkMode,
                    onClick = { state.triggerRemixSpin() }
                )
                StudioActionButton(
                    label = "Pen Hover",
                    isPrimary = false,
                    isActive = state.isPenPoised,
                    isDark = state.isDarkMode,
                    onClick = { state.isPenPoised = !state.isPenPoised }
                )
                StudioActionButton(
                    label = "Color Wells",
                    isPrimary = false,
                    isActive = state.isColorWells,
                    isDark = state.isDarkMode,
                    onClick = { state.isColorWells = !state.isColorWells }
                )
                StudioActionButton(
                    label = "HIG Grid",
                    isPrimary = false,
                    isActive = state.showHigGrid,
                    isDark = state.isDarkMode,
                    onClick = { state.showHigGrid = !state.showHigGrid }
                )
                StudioActionButton(
                    label = "Squircle",
                    isPrimary = false,
                    isActive = state.showSquircle,
                    isDark = state.isDarkMode,
                    onClick = { state.showSquircle = !state.showSquircle }
                )
                StudioActionButton(
                    label = "Dark",
                    isPrimary = false,
                    isActive = state.isDarkMode,
                    isDark = state.isDarkMode,
                    onClick = { state.isDarkMode = !state.isDarkMode }
                )
            }
        }
    }
}

@Composable
private fun StudioActionButton(
    label: String,
    isPrimary: Boolean,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isPrimary -> Color(0xFF007AFF)
        isActive -> Color(0x24007AFF)
        else -> Color.Transparent
    }
    val textColor = when {
        isPrimary -> Color.White
        isActive -> Color(0xFF007AFF)
        isDark -> Color(0xFFE2E8F0)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isPrimary || isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 5. FLASH (VIDEOS) GRAPHICS ICON (Director's Clapperboard + Hinged Clapper Arm)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleFlashGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val progress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            // 1. Clapperboard Main Slate Body
            drawSculptedSquircleBase(12f, 36f, 76f, 50f, 13f)

            // 2. Sapphire Recessed Video Viewport
            drawSapphireRecessRect(18f, 42f, 64f, 38f, 9f)

            // 3. Central Glowing Video Play Glyph (Rounded Apple HIG Play Triangle)
            val playPath = Path().apply {
                moveTo(44.5f, 55.0f)
                lineTo(44.5f, 67.0f)
                quadraticBezierTo(44.5f, 70.0f, 47.0f, 68.6f)
                lineTo(56.6f, 62.7f)
                cubicTo(59.2f, 61.8f, 59.2f, 60.2f, 56.6f, 59.3f)
                lineTo(47.0f, 53.4f)
                quadraticBezierTo(44.5f, 52.0f, 44.5f, 55.0f)
                close()
            }

            // Soft tactile drop shadow under play glyph
            val playShadowPath = Path().apply {
                moveTo(44.5f, 56.2f)
                lineTo(44.5f, 68.2f)
                quadraticBezierTo(44.5f, 71.2f, 47.0f, 69.8f)
                lineTo(56.6f, 63.9f)
                cubicTo(59.2f, 63.0f, 59.2f, 61.4f, 56.6f, 60.5f)
                lineTo(47.0f, 54.6f)
                quadraticBezierTo(44.5f, 53.2f, 44.5f, 56.2f)
                close()
            }
            drawPath(playShadowPath, color = Color(0x55001C48))

            // Luminous Pearl Play Glyph Body
            drawPath(playPath, brush = pearl)

            // Overhead glossy sheen overlay
            drawPath(
                playPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.Transparent),
                    startY = 52f,
                    endY = 70f
                )
            )

            // Specular border stroke with rounded joins
            drawPath(
                playPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0.65f)),
                    start = Offset(44.5f, 52f),
                    end = Offset(59.8f, 70f)
                ),
                style = Stroke(1.3f, join = StrokeJoin.Round)
            )

            // 4. Film Perforation Capsules (Bottom of Slate)
            val perfOffsets = listOf(24f, 37f, 50f, 63f)
            perfOffsets.forEach { px ->
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(px, 73f),
                    size = Size(7f, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // 5. Articulated Clapperboard Striped Top Arm (Claps open and shut with spring physics!)
            withTransform({
                translate(
                    top = -4f * progress
                )
                rotate(
                    degrees = -30f * progress,
                    pivot = Offset(14f, 34f)
                )
            }) {
                val clapperRect = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(10f, 21f, 90f, 35f),
                            cornerRadius = CornerRadius(6.5f, 6.5f)
                        )
                    )
                }

                // Arm Base
                drawPath(clapperRect, brush = Apple3DShaderPalette.bodyBrush(s))
                drawPath(clapperRect, brush = Apple3DShaderPalette.cylindricalSheenBrush(s))

                // Diagonal Pearl Hazard Stripes
                clipPath(clapperRect) {
                    val stripePositions = listOf(14f, 32f, 50f, 68f, 86f)
                    stripePositions.forEach { sx ->
                        val stripePath = Path().apply {
                            moveTo(sx, 18f)
                            lineTo(sx + 10f, 18f)
                            lineTo(sx - 2f, 38f)
                            lineTo(sx - 12f, 38f)
                            close()
                        }
                        drawPath(stripePath, brush = pearl)
                    }
                }

                // Outer Specular Stroke & Left Hinge Pin
                drawPath(clapperRect, brush = Apple3DShaderPalette.specularStrokeBrush(s), style = Stroke(1.8f))
                drawCircle(color = Color.White, radius = 3.2f, center = Offset(15f, 28f))
                drawCircle(color = Color(0xFF0051D5), radius = 1.6f, center = Offset(15f, 28f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. REMIX GRAPHICS ICON (Dual Orbital Co-Creation Nodes + Revolving Arcs)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleRemixGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val progress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            // Dual Orbital Nodes that smoothly rotate & expand outwards
            withTransform({
                rotate(degrees = -36f * progress, pivot = Offset(50f, 50f))
                scale(1f + 0.08f * progress, 1f + 0.08f * progress, pivot = Offset(50f, 50f))
            }) {
                // Primary Node Squircle (Left)
                drawSculptedSquircleBase(12f, 26f, 38f, 38f, 15f)
                drawSapphireRecessRect(18f, 32f, 26f, 26f, 10f)

                // Secondary Node Squircle (Right)
                drawSculptedSquircleBase(50f, 36f, 38f, 38f, 15f)
                drawSapphireRecessRect(56f, 42f, 26f, 26f, 10f)

                // Upper Intertwining Orbital Arc (Left to Right)
                drawArc(
                    brush = pearl,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(22f, 16f),
                    size = Size(56f, 48f),
                    style = Stroke(width = 5.2f, cap = StrokeCap.Round)
                )

                // Lower Intertwining Orbital Arc (Right to Left)
                drawArc(
                    brush = sky,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(22f, 36f),
                    size = Size(56f, 48f),
                    style = Stroke(width = 5.2f, cap = StrokeCap.Round)
                )

                // Directional Arrow Heads on Arcs
                val topArrow = Path().apply {
                    moveTo(74f, 34f)
                    lineTo(82f, 37f)
                    lineTo(77f, 44f)
                    close()
                }
                drawPath(topArrow, brush = pearl)

                val bottomArrow = Path().apply {
                    moveTo(26f, 66f)
                    lineTo(18f, 63f)
                    lineTo(23f, 56f)
                    close()
                }
                drawPath(bottomArrow, brush = sky)

                // Central Brilliant Sparkle Core (Intersection of Remix)
                val sparkRadius = 6f + 2f * progress
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFF80DCFF), Color.Transparent),
                        center = Offset(50f, 50f),
                        radius = sparkRadius * 2f
                    ),
                    radius = sparkRadius * 2f,
                    center = Offset(50f, 50f)
                )
                drawCircle(color = Color.White, radius = sparkRadius, center = Offset(50f, 50f))
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 7. MESSAGE / CHAT GRAPHICS ICON (Volumetric Chat Bubbles + Floating Speech Crest)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleChatGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val progress = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            // 1. Primary Bottom Message Bubble with Tail
            val primaryBubble = Path().apply {
                moveTo(24f, 38f)
                lineTo(76f, 38f)
                cubicTo(82f, 38f, 86f, 42f, 86f, 48f)
                lineTo(86f, 72f)
                cubicTo(86f, 78f, 82f, 82f, 76f, 82f)
                lineTo(34f, 82f)
                lineTo(20f, 90f) // Speech Tail
                lineTo(22f, 82f)
                lineTo(18f, 82f)
                cubicTo(12f, 82f, 8f, 78f, 8f, 72f)
                lineTo(8f, 48f)
                cubicTo(8f, 42f, 12f, 38f, 18f, 38f)
                close()
            }

            drawPath(primaryBubble, color = Apple3DShaderPalette.PlinthShadow)
            drawPath(primaryBubble, brush = Apple3DShaderPalette.bodyBrush(s))
            drawPath(primaryBubble, brush = Apple3DShaderPalette.cylindricalSheenBrush(s))
            drawPath(primaryBubble, brush = Apple3DShaderPalette.specularStrokeBrush(s), style = Stroke(2.2f))

            // 2. Sapphire Recessed Text Container
            val recessBubble = Path().apply {
                moveTo(22f, 44f)
                lineTo(72f, 44f)
                cubicTo(76f, 44f, 80f, 47f, 80f, 51f)
                lineTo(80f, 69f)
                cubicTo(80f, 73f, 76f, 76f, 72f, 76f)
                lineTo(32f, 76f)
                lineTo(24f, 80f)
                lineTo(25f, 76f)
                lineTo(18f, 76f)
                cubicTo(14f, 76f, 14f, 73f, 14f, 69f)
                lineTo(14f, 51f)
                cubicTo(14f, 47f, 18f, 44f, 22f, 44f)
                close()
            }
            drawPath(recessBubble, brush = Apple3DShaderPalette.sapphireRecessBrush(s))
            drawPath(recessBubble, brush = Apple3DShaderPalette.sapphireBevelBrush(s), style = Stroke(1.8f))

            // 3. Message Preview Typing Bars (Inside Sapphire Recess)
            drawRoundRect(
                brush = sky,
                topLeft = Offset(20f, 52f),
                size = Size(38f, 4.5f),
                cornerRadius = CornerRadius(2.25f, 2.25f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF80DCFF), Color(0xFF38B6FF))),
                topLeft = Offset(20f, 61f),
                size = Size(26f, 4.5f),
                cornerRadius = CornerRadius(2.25f, 2.25f)
            )

            // 4. Secondary Floating Response Bubble / Message Crest (Opens and floats upward with spring physics!)
            withTransform({
                translate(
                    left = 4f * progress,
                    top = -12f * progress
                )
                rotate(
                    degrees = -20f * progress,
                    pivot = Offset(32f, 36f)
                )
            }) {
                val floatingBubble = Path().apply {
                    moveTo(34f, 15f)
                    lineTo(82f, 15f)
                    cubicTo(88f, 15f, 92f, 19f, 92f, 25f)
                    lineTo(92f, 45f)
                    cubicTo(92f, 51f, 88f, 55f, 82f, 55f)
                    lineTo(74f, 55f)
                    lineTo(76f, 61f) // Floating bubble tail
                    lineTo(66f, 55f)
                    lineTo(34f, 55f)
                    cubicTo(28f, 55f, 24f, 51f, 24f, 45f)
                    lineTo(24f, 25f)
                    cubicTo(24f, 19f, 28f, 15f, 34f, 15f)
                    close()
                }

                drawPath(floatingBubble, brush = pearl)
                drawPath(
                    floatingBubble,
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFF80DCFF).copy(alpha = 0.5f)),
                        start = Offset(24f, 15f),
                        end = Offset(92f, 55f)
                    ),
                    style = Stroke(1.8f)
                )

                // 3 Pearl-Azure Typing Dots inside Floating Bubble
                val dotPositions = listOf(44f, 58f, 72f)
                dotPositions.forEachIndexed { i, dx ->
                    drawCircle(
                        brush = Apple3DShaderPalette.bodyBrush(s),
                        radius = 3.6f,
                        center = Offset(dx, 35f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 1.4f,
                        center = Offset(dx - 1f, 34f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7B. LOCK / PRIVATE ACCOUNT GRAPHICS ICON (Sculpted Padlock + Anodized Steel Shackle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleLockGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val articulate = state.articulationProgress.value

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = 100f
        val pearl = Apple3DShaderPalette.pearlBrush(s)
        val sky = Apple3DShaderPalette.skyBrush(s)
        val scaleFactor = size.minDimension / 100f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            scale(state.squashX.value, state.squashY.value, pivot = Offset(50f, 85f))
        }) {
            // 1. Floor shadow
            drawFloorShadow(50f, 88f, 36f, 4.8f)

            // 2. Anodized Steel Arched Shackle (Unlocks/Slides up during articulation)
            withTransform({
                translate(top = -10f * articulate)
            }) {
                // Shackle back shadow
                val shackleShadowPath = Path().apply {
                    moveTo(33f, 48f)
                    lineTo(33f, 32f)
                    cubicTo(33f, 18f, 67f, 18f, 67f, 32f)
                    lineTo(67f, 48f)
                }
                drawPath(
                    shackleShadowPath,
                    color = Color.Black.copy(alpha = 0.22f),
                    style = Stroke(width = 8.5f, cap = StrokeCap.Round)
                )

                // Shackle pearl metallic body
                drawPath(
                    shackleShadowPath,
                    brush = pearl,
                    style = Stroke(width = 7.5f, cap = StrokeCap.Round)
                )

                // Shackle specular highlight
                val shackleHighlight = Path().apply {
                    moveTo(35f, 32f)
                    cubicTo(35f, 21f, 65f, 21f, 65f, 32f)
                }
                drawPath(
                    shackleHighlight,
                    brush = Apple3DShaderPalette.specularStrokeBrush(s),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
            }

            // 3. Sculpted Squircle Padlock Body (Cupertino Blue)
            drawSculptedSquircleBase(22f, 40f, 56f, 45f, 14f)

            // 4. Sapphire Recessed Keyhole Chamber
            drawSapphireRecessRect(38f, 49f, 24f, 27f, 8f)

            // 5. Keyhole (Circle top + tapered slot)
            val keyholePath = Path().apply {
                addOval(Rect(center = Offset(50f, 58f), radius = 4.2f))
                moveTo(48.2f, 59.5f)
                lineTo(47f, 68f)
                cubicTo(47f, 69.5f, 53f, 69.5f, 53f, 68f)
                lineTo(51.8f, 59.5f)
                close()
            }
            drawPath(keyholePath, brush = sky)
            drawPath(keyholePath, brush = Apple3DShaderPalette.specularStrokeBrush(s), style = Stroke(1.2f))

            // Luminous core beacon in keyhole
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(50f, 58f),
                    radius = 4.5f
                ),
                radius = 4.5f,
                center = Offset(50f, 58f)
            )

            // Cylindrical metallic sheen across padlock body
            val bodyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(22f, 40f, 78f, 85f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                )
            }
            clipPath(bodyPath) {
                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(24f, 40f, 78f, 85f, s / 1000f)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. HEART-HANDSHAKE GRAPHICS ICON (Sculpted 3D Cantilevered Handshake + 3-Pump Animation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleHeartHandshakeGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val pumpScaleX = remember { Animatable(1f) }
    val pumpScaleY = remember { Animatable(1f) }
    val pumpTranslateY = remember { Animatable(0f) }
    val upperHandAngle = remember { Animatable(0f) }
    val fingerShift = remember { Animatable(0f) }

    fun triggerHandshake() {
        scope.launch {
            // Anticipation open
            launch { upperHandAngle.animateTo(-9f, tween(150, easing = FastOutSlowInEasing)) }
            launch { fingerShift.animateTo(9f, tween(150, easing = FastOutSlowInEasing)) }
            pumpScaleX.animateTo(0.972f, tween(150))

            // Pump 1 (Downward clasp)
            launch { upperHandAngle.animateTo(4.0f, tween(170)) }
            launch { fingerShift.animateTo(-6f, tween(170)) }
            launch { pumpTranslateY.animateTo(15f, tween(170)) }
            launch { pumpScaleX.animateTo(1.045f, tween(170)) }
            pumpScaleY.animateTo(0.955f, tween(170))

            // Pump 2 (Upward rebound)
            launch { upperHandAngle.animateTo(-4.8f, tween(170)) }
            launch { fingerShift.animateTo(4.5f, tween(170)) }
            launch { pumpTranslateY.animateTo(-11f, tween(170)) }
            launch { pumpScaleX.animateTo(0.985f, tween(170)) }
            pumpScaleY.animateTo(1.02f, tween(170))

            // Pump 3 (Downward)
            launch { upperHandAngle.animateTo(2.6f, tween(170)) }
            launch { fingerShift.animateTo(-3f, tween(170)) }
            launch { pumpTranslateY.animateTo(9f, tween(170)) }
            launch { pumpScaleX.animateTo(1.025f, tween(170)) }
            pumpScaleY.animateTo(0.978f, tween(170))

            // Settle with Compose Spring
            launch { upperHandAngle.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { fingerShift.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { pumpTranslateY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { pumpScaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            pumpScaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    LaunchedEffect(state.isAnimating) {
        if (state.isAnimating) {
            triggerHandshake()
        }
    }

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            triggerHandshake()
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = size.minDimension / 1000f

        // Ambient Floor Shadow
        drawOval(
            brush = AppleHigBluePalette.floorGlowBrush(500f, 868f, 225f, s),
            topLeft = Offset(275f * s, 848f * s),
            size = Size(450f * s, 38f * s)
        )

        withTransform({
            translate(top = pumpTranslateY.value * s)
            scale(pumpScaleX.value, pumpScaleY.value, pivot = Offset(500f * s, 790f * s))
        }) {
            val heartPath = Path().apply {
                moveTo(500f * s, 816f * s)
                cubicTo(476f * s, 816f * s, 452f * s, 804f * s, 430f * s, 782f * s)
                lineTo(222f * s, 574f * s)
                cubicTo(150f * s, 502f * s, 128f * s, 430f * s, 128f * s, 354f * s)
                cubicTo(128f * s, 232f * s, 224f * s, 142f * s, 344f * s, 142f * s)
                cubicTo(412f * s, 142f * s, 464f * s, 172f * s, 500f * s, 218f * s)
                cubicTo(536f * s, 172f * s, 588f * s, 142f * s, 656f * s, 142f * s)
                cubicTo(776f * s, 142f * s, 872f * s, 232f * s, 872f * s, 354f * s)
                cubicTo(872f * s, 430f * s, 850f * s, 502f * s, 778f * s, 574f * s)
                lineTo(570f * s, 782f * s)
                cubicTo(548f * s, 804f * s, 524f * s, 816f * s, 500f * s, 816f * s)
                close()
            }

            clipPath(heartPath) {
                // 1. Main Cupertino Blue Heart Body + Cylindrical Sheen
                drawRect(
                    brush = AppleHigBluePalette.bodyBrush(s),
                    topLeft = Offset(120f * s, 135f * s),
                    size = Size(760f * s, 690f * s)
                )
                drawRect(
                    brush = AppleHigBluePalette.cylindricalSheenBrush(s),
                    topLeft = Offset(120f * s, 135f * s),
                    size = Size(760f * s, 690f * s)
                )

                // Left Lobe Overhead Dome Highlight
                drawOval(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(200f * s, 175f * s),
                    size = Size(220f * s, 88f * s)
                )

                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(175f, 145f, 805f, 815f, s)
                }

                // 2. Sculpted Lower-Right Interlocking Finger Capsules
                withTransform({
                    translate(left = fingerShift.value * s, top = fingerShift.value * s)
                }) {
                    val fingerCapsules = listOf(
                        Triple(672f, 562f, 162f),
                        Triple(596f, 638f, 156f),
                        Triple(520f, 712f, 138f)
                    )
                    fingerCapsules.forEach { (cx, cy, height) ->
                        withTransform({
                            rotate(-45f, pivot = Offset(cx * s, cy * s))
                        }) {
                            val topLeft = Offset((cx - 24f) * s, (cy - height / 2f) * s)
                            val capSize = Size(48f * s, height * s)
                            val rad = CornerRadius(24f * s, 24f * s)

                            drawRoundRect(
                                brush = AppleHigBluePalette.capsuleRecessBrush(cy - height / 2f, cy + height / 2f, s),
                                topLeft = topLeft,
                                size = capSize,
                                cornerRadius = rad
                            )
                            drawRoundRect(
                                brush = AppleHigBluePalette.capsuleBevelBrush(cy - height / 2f, cy + height / 2f, s),
                                topLeft = topLeft,
                                size = capSize,
                                cornerRadius = rad,
                                style = Stroke(width = 4.5f * s)
                            )
                        }
                    }
                }

                // 3. Cantilevered 3D Upper-Right Hand & Thumb Layer
                withTransform({
                    rotate(upperHandAngle.value, pivot = Offset(650f * s, 340f * s))
                }) {
                    val upperHandPath = Path().apply {
                        moveTo(500f * s, 218f * s)
                        lineTo(378f * s, 340f * s)
                        cubicTo(340f * s, 378f * s, 340f * s, 438f * s, 378f * s, 476f * s)
                        cubicTo(416f * s, 514f * s, 478f * s, 514f * s, 518f * s, 476f * s)
                        lineTo(584f * s, 412f * s)
                        cubicTo(618f * s, 380f * s, 672f * s, 380f * s, 706f * s, 412f * s)
                        lineTo(825f * s, 524f * s)
                        lineTo(890f * s, 350f * s)
                        lineTo(890f * s, 130f * s)
                        lineTo(500f * s, 130f * s)
                        close()
                    }

                    val seamCurvePath = Path().apply {
                        moveTo(500f * s, 218f * s)
                        lineTo(378f * s, 340f * s)
                        cubicTo(340f * s, 378f * s, 340f * s, 438f * s, 378f * s, 476f * s)
                        cubicTo(416f * s, 514f * s, 478f * s, 514f * s, 518f * s, 476f * s)
                        lineTo(584f * s, 412f * s)
                        cubicTo(618f * s, 380f * s, 672f * s, 380f * s, 706f * s, 412f * s)
                        lineTo(825f * s, 524f * s)
                    }

                    // Recessed Deep Sapphire Clasp Shadow Groove Under the Thumb & Hand
                    drawPath(
                        path = seamCurvePath,
                        brush = AppleHigBluePalette.capsuleBevelBrush(220f, 530f, s),
                        style = Stroke(width = 44f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = seamCurvePath,
                        brush = AppleHigBluePalette.capsuleRecessBrush(220f, 530f, s),
                        style = Stroke(width = 34f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Elevated Upper-Right Hand & Thumb Surface
                    clipPath(upperHandPath) {
                        drawRect(
                            brush = AppleHigBluePalette.lidTopBrush(s),
                            topLeft = Offset(330f * s, 130f * s),
                            size = Size(560f * s, 410f * s)
                        )
                        // Soft Overhead Crown Highlight
                        drawOval(
                            color = Color.White.copy(alpha = 0.38f),
                            topLeft = Offset(545f * s, 172f * s),
                            size = Size(235f * s, 46f * s)
                        )
                        // Sculpted Thumb Pad Knuckle Catch-Light
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = 34f * s,
                            center = Offset(445f * s, 412f * s)
                        )
                    }

                    // Crisp White Specular Bevel Along the Sculpted Thumb Edge
                    drawPath(
                        path = seamCurvePath,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Outer Specular Bevel Contour
            drawPath(
                path = heartPath,
                brush = AppleHigBluePalette.specularStrokeBrush(s),
                style = Stroke(width = 3.4f * s)
            )
        }
    }
}

@Composable
fun AppleHeartHandshakeIcon(
    modifier: Modifier = Modifier.size(140.dp),
    onClick: (() -> Unit)? = null
) {
    AppleHeartHandshakeGraphic(
        modifier = modifier,
        enableClickToAnimate = true,
        onClick = onClick
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. TRENDING FIRE GRAPHICS ICON (Lucide 'flame' • Solar Red-Orange-Yellow 3D)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleTrendingFireGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val innerFlameAngle = remember { Animatable(0f) }
    val innerFlameScale = remember { Animatable(1f) }
    val sparkShiftY = remember { Animatable(0f) }

    fun triggerFireBlaze() {
        scope.launch {
            // Anticipation crouch
            launch { scaleX.animateTo(1.06f, tween(140, easing = FastOutSlowInEasing)) }
            launch { scaleY.animateTo(0.93f, tween(140, easing = FastOutSlowInEasing)) }
            innerFlameAngle.animateTo(-7f, tween(140))

            // Upward Blaze Surge 1
            launch { scaleX.animateTo(0.95f, tween(175)) }
            launch { scaleY.animateTo(1.075f, tween(175)) }
            launch { innerFlameScale.animateTo(1.12f, tween(175)) }
            launch { sparkShiftY.animateTo(-26f, tween(175)) }
            innerFlameAngle.animateTo(6.5f, tween(175))

            // Secondary Flicker
            launch { scaleX.animateTo(1.03f, tween(160)) }
            launch { scaleY.animateTo(0.97f, tween(160)) }
            launch { innerFlameScale.animateTo(0.96f, tween(160)) }
            innerFlameAngle.animateTo(-4.5f, tween(160))

            // Spring settle
            launch { scaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { scaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { innerFlameScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { sparkShiftY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            innerFlameAngle.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    LaunchedEffect(state.isAnimating) {
        if (state.isAnimating) {
            triggerFireBlaze()
        }
    }

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            triggerFireBlaze()
            scope.launch {
                state.triggerArticulateAnimation()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val s = size.minDimension / 1000f

        // Warm Crimson-Orange Ambient Floor Glow
        drawOval(
            brush = AppleHigFirePalette.fireFloorGlowBrush(500f, 868f, 230f, s),
            topLeft = Offset(270f * s, 848f * s),
            size = Size(460f * s, 38f * s)
        )

        withTransform({
            scale(scaleX.value, scaleY.value, pivot = Offset(500f * s, 830f * s))
        }) {
            // Exact Lucide 'flame' Outer Silhouette Mapped to 1000x1000 Space
            val outerFlamePath = Path().apply {
                moveTo(381f * s, 575f * s)
                cubicTo(428f * s, 575f * s, 466f * s, 537f * s, 466f * s, 490f * s)
                cubicTo(466f * s, 443.1f * s, 449f * s, 422f * s, 432f * s, 388f * s)
                cubicTo(395.5f * s, 315.1f * s, 424.4f * s, 250.2f * s, 500f * s, 168f * s)
                cubicTo(522f * s, 258f * s, 572f * s, 342f * s, 640f * s, 398f * s)
                cubicTo(708f * s, 454f * s, 742f * s, 520f * s, 742f * s, 592f * s)
                cubicTo(742f * s, 725.6f * s, 633.6f * s, 834f * s, 500f * s, 834f * s)
                cubicTo(366.4f * s, 834f * s, 258f * s, 725.6f * s, 258f * s, 592f * s)
                cubicTo(258f * s, 550f * s, 274f * s, 510f * s, 296f * s, 484f * s)
                cubicTo(296f * s, 534f * s, 334f * s, 575f * s, 381f * s, 575f * s)
                close()
            }

            clipPath(outerFlamePath) {
                // 1. Main Solar Yellow -> Orange -> Red -> Ruby Body Gradient
                drawRect(
                    brush = AppleHigFirePalette.flameBodyBrush(s),
                    topLeft = Offset(240f * s, 150f * s),
                    size = Size(520f * s, 700f * s)
                )
                drawRect(
                    brush = AppleHigFirePalette.flameCylindricalSheenBrush(s),
                    topLeft = Offset(240f * s, 150f * s),
                    size = Size(520f * s, 700f * s)
                )

                // Studio Softbox Gloss Stripes in Warm Gold
                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(285f, 165f, 825f, 725f, s)
                }

                // 2. Recessed Deep-Ruby Thermal Core Chamber (Matches Trash Bin Capsule Recesses)
                val chamberPath = Path().apply {
                    moveTo(512f * s, 395f * s)
                    cubicTo(558f * s, 455f * s, 632f * s, 525f * s, 632f * s, 622f * s)
                    cubicTo(632f * s, 704f * s, 572f * s, 766f * s, 498f * s, 766f * s)
                    cubicTo(424f * s, 766f * s, 366f * s, 706f * s, 366f * s, 628f * s)
                    cubicTo(366f * s, 572f * s, 402f * s, 528f * s, 444f * s, 492f * s)
                    cubicTo(476f * s, 464f * s, 498f * s, 432f * s, 512f * s, 395f * s)
                    close()
                }

                drawPath(
                    path = chamberPath,
                    brush = AppleHigFirePalette.rubyRecessBrush(395f, 766f, s)
                )
                drawPath(
                    path = chamberPath,
                    brush = AppleHigFirePalette.rubyBevelBrush(395f, 766f, s),
                    style = Stroke(width = 6f * s, join = StrokeJoin.Round)
                )

                // 3. Elevated 3D Golden-Yellow Inner Flame Core (Cantilevered with Spring Flicker)
                withTransform({
                    rotate(innerFlameAngle.value, pivot = Offset(500f * s, 750f * s))
                    scale(innerFlameScale.value, innerFlameScale.value, pivot = Offset(500f * s, 750f * s))
                }) {
                    val innerCorePath = Path().apply {
                        moveTo(516f * s, 455f * s)
                        cubicTo(552f * s, 505f * s, 594f * s, 558f * s, 594f * s, 634f * s)
                        cubicTo(594f * s, 694f * s, 552f * s, 742f * s, 498f * s, 742f * s)
                        cubicTo(444f * s, 742f * s, 404f * s, 696f * s, 404f * s, 638f * s)
                        cubicTo(404f * s, 592f * s, 434f * s, 556f * s, 468f * s, 524f * s)
                        cubicTo(492f * s, 502f * s, 508f * s, 480f * s, 516f * s, 455f * s)
                        close()
                    }

                    // Warm Ruby Contact Drop Shadow Under Elevated Inner Flame
                    drawPath(
                        path = innerCorePath,
                        color = Color(0x882B0005),
                        style = Stroke(width = 14f * s)
                    )

                    drawPath(
                        path = innerCorePath,
                        brush = AppleHigFirePalette.innerCoreFlameBrush(s)
                    )

                    // White-Hot Inner Core Specular Highlight
                    drawOval(
                        color = Color.White.copy(alpha = 0.68f),
                        topLeft = Offset(468f * s, 575f * s),
                        size = Size(56f * s, 96f * s)
                    )

                    drawPath(
                        path = innerCorePath,
                        color = Color.White.copy(alpha = 0.88f),
                        style = Stroke(width = 4f * s, join = StrokeJoin.Round)
                    )
                }

                // 4. Rising 3D Solar Spark Orb in Upper Flame Crown
                withTransform({
                    translate(top = sparkShiftY.value * s)
                }) {
                    val sparkCenter = Offset(516f * s, 322f * s)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 18f * s,
                        center = sparkCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFE047).copy(alpha = 0.45f),
                        radius = 32f * s,
                        center = sparkCenter
                    )
                }
            }

            // Outer 1.5px Specular Bevel Contour
            drawPath(
                path = outerFlamePath,
                brush = AppleHigFirePalette.fireSpecularStrokeBrush(s),
                style = Stroke(width = 3.4f * s, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun AppleTrendingFireIcon(
    modifier: Modifier = Modifier.size(140.dp),
    onClick: (() -> Unit)? = null
) {
    AppleTrendingFireGraphic(
        modifier = modifier,
        enableClickToAnimate = true,
        onClick = onClick
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. NEW PHOTOS ICON (Sculpted 3D Frame + Recessed Sky Well + Sun & Peaks)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ApplePhotosIcon(
    modifier: Modifier = Modifier.size(140.dp),
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val innerShiftY = remember { Animatable(0f) }

    fun triggerBounce() {
        scope.launch {
            launch { innerShiftY.animateTo(-20f, tween(180, easing = FastOutSlowInEasing)) }
            launch { scaleX.animateTo(1.055f, tween(180)) }
            scaleY.animateTo(0.945f, tween(180))

            launch { innerShiftY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { scaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            scaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    Canvas(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            triggerBounce()
            onClick?.invoke()
        }
    ) {
        val s = size.minDimension / 1000f

        // Ambient Floor Shadow
        drawOval(
            brush = AppleHigBluePalette.floorGlowBrush(500f, 868f, 225f, s),
            topLeft = Offset(275f * s, 850f * s),
            size = Size(450f * s, 36f * s)
        )

        withTransform({
            scale(scaleX.value, scaleY.value, pivot = Offset(500f * s, 830f * s))
        }) {
            // Subtle Peeking Secondary Gallery Card for 3D Depth
            withTransform({
                rotate(-6f, pivot = Offset(500f * s, 500f * s))
                translate(left = -16f * s, top = -12f * s)
            }) {
                drawRoundRect(
                    brush = AppleHigBluePalette.secondaryLayerBrush(s),
                    topLeft = Offset(190f * s, 190f * s),
                    size = Size(620f * s, 620f * s),
                    cornerRadius = CornerRadius(110f * s, 110f * s)
                )
            }

            val framePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(170f * s, 170f * s, 830f * s, 830f * s),
                        cornerRadius = CornerRadius(118f * s, 118f * s)
                    )
                )
            }

            clipPath(framePath) {
                // 1. Main Cupertino Blue Frame Body + Cylindrical Metallic Sheen
                drawRect(
                    brush = AppleHigBluePalette.bodyBrush(s),
                    topLeft = Offset(170f * s, 170f * s),
                    size = Size(660f * s, 660f * s)
                )
                drawRect(
                    brush = AppleHigBluePalette.cylindricalSheenBrush(s),
                    topLeft = Offset(170f * s, 170f * s),
                    size = Size(660f * s, 660f * s)
                )

                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(195f, 170f, 830f, 810f, s)
                }

                // 2. Recessed Deep-Sapphire Inner Sky Viewport
                val viewportRect = Rect(236f * s, 236f * s, 764f * s, 764f * s)
                val viewportPath = Path().apply {
                    addRoundRect(RoundRect(viewportRect, CornerRadius(78f * s, 78f * s)))
                }

                drawRoundRect(
                    brush = AppleHigBluePalette.capsuleRecessBrush(236f, 764f, s),
                    topLeft = Offset(236f * s, 236f * s),
                    size = Size(528f * s, 528f * s),
                    cornerRadius = CornerRadius(78f * s, 78f * s)
                )

                clipPath(viewportPath) {
                    withTransform({
                        translate(top = innerShiftY.value * s)
                    }) {
                        // 3. Elevated 3D Luminous Sun Orb with Coronal Glow
                        val sunCenter = Offset(374f * s, 372f * s)
                        drawCircle(
                            color = Color(0x4056D0FF),
                            radius = 76f * s,
                            center = sunCenter
                        )
                        drawCircle(
                            color = Color(0x88000D2B),
                            radius = 60f * s,
                            center = Offset(378f * s, 384f * s)
                        )
                        drawCircle(
                            brush = AppleHigBluePalette.luminousOrbBrush(sunCenter, 58f * s),
                            radius = 58f * s,
                            center = sunCenter
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 58f * s,
                            center = sunCenter,
                            style = Stroke(width = 3.2f * s)
                        )

                        // 4. Secondary Sculpted Left Mountain Peak
                        val backPeak = Path().apply {
                            moveTo(220f * s, 770f * s)
                            lineTo(382f * s, 546f * s)
                            cubicTo(406f * s, 516f * s, 444f * s, 516f * s, 468f * s, 546f * s)
                            lineTo(640f * s, 770f * s)
                            close()
                        }
                        drawPath(backPeak, brush = AppleHigBluePalette.secondaryLayerBrush(s))

                        // 5. Primary Sculpted Right Mountain Peak (Lucide 'image' Slope)
                        val frontPeak = Path().apply {
                            moveTo(306f * s, 770f * s)
                            lineTo(582f * s, 482f * s)
                            cubicTo(612f * s, 450f * s, 658f * s, 450f * s, 688f * s, 482f * s)
                            lineTo(780f * s, 578f * s)
                            lineTo(780f * s, 770f * s)
                            close()
                        }
                        drawPath(frontPeak, brush = AppleHigBluePalette.lidTopBrush(s))

                        // Specular Ridge Highlight Along Primary Mountain Crest
                        val frontCrest = Path().apply {
                            moveTo(314f * s, 762f * s)
                            lineTo(584f * s, 484f * s)
                            cubicTo(612f * s, 454f * s, 658f * s, 454f * s, 686f * s, 484f * s)
                            lineTo(768f * s, 568f * s)
                        }
                        drawPath(
                            path = frontCrest,
                            color = Color.White.copy(alpha = 0.88f),
                            style = Stroke(width = 5f * s, cap = StrokeCap.Round)
                        )
                    }
                }

                // Anodized Outer Bevel Rim Around Inner Viewport
                drawRoundRect(
                    brush = AppleHigBluePalette.capsuleBevelBrush(236f, 764f, s),
                    topLeft = Offset(236f * s, 236f * s),
                    size = Size(528f * s, 528f * s),
                    cornerRadius = CornerRadius(78f * s, 78f * s),
                    style = Stroke(width = 5f * s)
                )
            }

            // Outer Frame Specular Bevel Contour
            drawPath(
                path = framePath,
                brush = AppleHigBluePalette.specularStrokeBrush(s),
                style = Stroke(width = 3.4f * s)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. NEW VIDEOS ICON (Sculpted Camera Body + Recessed Play Chamber + Optical Wing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleVideosIcon(
    modifier: Modifier = Modifier.size(140.dp),
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val wingAngle = remember { Animatable(0f) }
    val wingShiftX = remember { Animatable(0f) }

    fun triggerVideoSpring() {
        scope.launch {
            launch { wingAngle.animateTo(-7.5f, tween(180, easing = FastOutSlowInEasing)) }
            launch { wingShiftX.animateTo(20f, tween(180, easing = FastOutSlowInEasing)) }
            launch { scaleX.animateTo(1.055f, tween(180)) }
            scaleY.animateTo(0.945f, tween(180))

            launch { wingAngle.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { wingShiftX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { scaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            scaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    Canvas(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            triggerVideoSpring()
            onClick?.invoke()
        }
    ) {
        val s = size.minDimension / 1000f

        // Ambient Floor Shadow
        drawOval(
            brush = AppleHigBluePalette.floorGlowBrush(500f, 836f, 235f, s),
            topLeft = Offset(265f * s, 818f * s),
            size = Size(470f * s, 36f * s)
        )

        withTransform({
            scale(scaleX.value, scaleY.value, pivot = Offset(480f * s, 720f * s))
        }) {
            // 1. Cantilevered Right Optical Wing
            withTransform({
                translate(left = wingShiftX.value * s)
                rotate(wingAngle.value, pivot = Offset(644f * s, 500f * s))
            }) {
                val wingPath = Path().apply {
                    moveTo(634f * s, 440f * s)
                    lineTo(824f * s, 334f * s)
                    cubicTo(844f * s, 323f * s, 864f * s, 336f * s, 864f * s, 358f * s)
                    lineTo(864f * s, 642f * s)
                    cubicTo(864f * s, 664f * s, 844f * s, 677f * s, 824f * s, 666f * s)
                    lineTo(634f * s, 560f * s)
                    close()
                }

                clipPath(wingPath) {
                    drawRect(
                        brush = AppleHigBluePalette.lidTopBrush(s),
                        topLeft = Offset(630f * s, 320f * s),
                        size = Size(240f * s, 360f * s)
                    )
                    // Recessed Sapphire Neck Collar Groove
                    drawRect(
                        brush = AppleHigBluePalette.capsuleRecessBrush(420f, 580f, s),
                        topLeft = Offset(634f * s, 420f * s),
                        size = Size(32f * s, 160f * s)
                    )
                    // Recessed Vertical Optical Aperture Capsule on Wing
                    drawRoundRect(
                        brush = AppleHigBluePalette.capsuleRecessBrush(385f, 615f, s),
                        topLeft = Offset(792f * s, 385f * s),
                        size = Size(34f * s, 230f * s),
                        cornerRadius = CornerRadius(17f * s, 17f * s)
                    )
                    drawRoundRect(
                        brush = AppleHigBluePalette.capsuleBevelBrush(385f, 615f, s),
                        topLeft = Offset(792f * s, 385f * s),
                        size = Size(34f * s, 230f * s),
                        cornerRadius = CornerRadius(17f * s, 17f * s),
                        style = Stroke(width = 3.5f * s)
                    )
                    // Top Bevel Specular Highlight
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(656f * s, 444f * s),
                        end = Offset(836f * s, 344f * s),
                        strokeWidth = 5.5f * s,
                        cap = StrokeCap.Round
                    )
                }

                drawPath(
                    path = wingPath,
                    brush = AppleHigBluePalette.specularStrokeBrush(s),
                    style = Stroke(width = 3f * s)
                )
            }

            // 2. Main Sculpted Camera Body
            val bodyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(142f * s, 280f * s, 648f * s, 720f * s),
                        cornerRadius = CornerRadius(92f * s, 92f * s)
                    )
                )
            }

            clipPath(bodyPath) {
                drawRect(
                    brush = AppleHigBluePalette.bodyBrush(s),
                    topLeft = Offset(142f * s, 280f * s),
                    size = Size(506f * s, 440f * s)
                )
                drawRect(
                    brush = AppleHigBluePalette.cylindricalSheenBrush(s),
                    topLeft = Offset(142f * s, 280f * s),
                    size = Size(506f * s, 440f * s)
                )
                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(170f, 280f, 720f, 630f, s)
                }

                // Top Recessed Sapphire Recording Tally Groove + Luminous Indicator Dot
                drawRoundRect(
                    brush = AppleHigBluePalette.capsuleRecessBrush(322f, 354f, s),
                    topLeft = Offset(206f * s, 322f * s),
                    size = Size(100f * s, 32f * s),
                    cornerRadius = CornerRadius(16f * s, 16f * s)
                )
                drawRoundRect(
                    brush = AppleHigBluePalette.capsuleBevelBrush(322f, 354f, s),
                    topLeft = Offset(206f * s, 322f * s),
                    size = Size(100f * s, 32f * s),
                    cornerRadius = CornerRadius(16f * s, 16f * s),
                    style = Stroke(width = 3f * s)
                )
                drawCircle(
                    color = Color(0xFF70D6FF),
                    radius = 7f * s,
                    center = Offset(228f * s, 338f * s)
                )

                // Center Recessed Sapphire Optical Chamber + Sculpted 3D Play Triangle
                val chamberCenter = Offset(395f * s, 508f * s)
                drawCircle(
                    brush = AppleHigBluePalette.capsuleRecessBrush(396f, 620f, s),
                    radius = 110f * s,
                    center = chamberCenter
                )
                drawCircle(
                    brush = AppleHigBluePalette.capsuleBevelBrush(396f, 620f, s),
                    radius = 110f * s,
                    center = chamberCenter,
                    style = Stroke(width = 5f * s)
                )

                val playPath = Path().apply {
                    moveTo(362f * s, 450f * s)
                    lineTo(460f * s, 508f * s)
                    lineTo(362f * s, 566f * s)
                    close()
                }
                drawPath(playPath, brush = AppleHigBluePalette.lidTopBrush(s))
                drawPath(
                    path = playPath,
                    color = Color.White.copy(alpha = 0.88f),
                    style = Stroke(width = 5.5f * s, join = StrokeJoin.Round)
                )
            }

            drawPath(
                path = bodyPath,
                brush = AppleHigBluePalette.specularStrokeBrush(s),
                style = Stroke(width = 3.4f * s)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. APPLE HIG 3D NOTIFICATION GRAPHIC (Cupertino Blue Dome + Acoustic Sonic Rings + Checkmark Jewel)
// Exact match to e:\Design Assets\apple_hig_3d_notification_empty_state_studio.html
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleNotificationGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    showSonicWaves: Boolean = true,
    showCheckBadge: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerBellRing()
                onClick?.invoke()
            }
        }
    } else Modifier

    Canvas(modifier = modifier.then(clickModifier)) {
        val scale = size.minDimension / 1000f
        val progress = state.articulationProgress.value
        val bellAngle = state.tiltAngle.value

        withTransform({
            scale(state.squashX.value, state.squashY.value, pivot = Offset(500f * scale, 876f * scale))
        }) {
            // 1. Ambient Floor Glow
            drawOval(
                brush = Brush.radialGradient(
                    0.00f to Color(0x520055E0),
                    0.55f to Color(0x1A007AFF),
                    1.00f to Color(0x00007AFF),
                    center = Offset(500f * scale, 876f * scale),
                    radius = 225f * scale
                ),
                topLeft = Offset((500f - 225f) * scale, (876f - 18f) * scale),
                size = Size(450f * scale, 36f * scale)
            )

            // 2. Concentric Acoustic Sonic Rings (Left & Right Empty State Aura)
            if (showSonicWaves) {
                val waveAlpha = 0.55f + 0.45f * progress
                val waveScale = 1f + 0.06f * progress
                val waveBrush = Brush.verticalGradient(
                    0.00f to Color(0x3370D6FF),
                    0.50f to Color(0xC01A96FF),
                    1.00f to Color(0x330051D5),
                    startY = 300f * scale,
                    endY = 620f * scale
                )

                // Left Acoustic Waves
                withTransform({
                    scale(waveScale, waveScale, pivot = Offset(240f * scale, 460f * scale))
                }) {
                    val leftWave1 = Path().apply {
                        moveTo(192f * scale, 365f * scale)
                        cubicTo(148f * scale, 415f * scale, 148f * scale, 505f * scale, 192f * scale, 555f * scale)
                    }
                    drawPath(leftWave1, waveBrush, alpha = waveAlpha, style = Stroke(width = 14f * scale, cap = StrokeCap.Round))

                    val leftWave2 = Path().apply {
                        moveTo(138f * scale, 320f * scale)
                        cubicTo(78f * scale, 395f * scale, 78f * scale, 525f * scale, 138f * scale, 600f * scale)
                    }
                    drawPath(leftWave2, waveBrush, alpha = waveAlpha * 0.65f, style = Stroke(width = 10f * scale, cap = StrokeCap.Round))
                }

                // Right Acoustic Waves
                withTransform({
                    scale(waveScale, waveScale, pivot = Offset(760f * scale, 460f * scale))
                }) {
                    val rightWave1 = Path().apply {
                        moveTo(808f * scale, 365f * scale)
                        cubicTo(852f * scale, 415f * scale, 852f * scale, 505f * scale, 808f * scale, 555f * scale)
                    }
                    drawPath(rightWave1, waveBrush, alpha = waveAlpha, style = Stroke(width = 14f * scale, cap = StrokeCap.Round))

                    val rightWave2 = Path().apply {
                        moveTo(862f * scale, 320f * scale)
                        cubicTo(922f * scale, 395f * scale, 922f * scale, 525f * scale, 862f * scale, 600f * scale)
                    }
                    drawPath(rightWave2, waveBrush, alpha = waveAlpha * 0.65f, style = Stroke(width = 10f * scale, cap = StrokeCap.Round))
                }
            }

            // 3. Swinging Bell Assembly (Pivots from top hinge at 500, 180)
            withTransform({
                rotate(degrees = bellAngle, pivot = Offset(500f * scale, 180f * scale))
            }) {
                // A. Sculpted Top Suspension Crown Loop
                val crownLoopPath = Path().apply {
                    moveTo(424f * scale, 242f * scale)
                    lineTo(429f * scale, 162f * scale)
                    cubicTo(432f * scale, 132f * scale, 454f * scale, 114f * scale, 486f * scale, 114f * scale)
                    lineTo(514f * scale, 114f * scale)
                    cubicTo(546f * scale, 114f * scale, 568f * scale, 132f * scale, 571f * scale, 162f * scale)
                    lineTo(576f * scale, 242f * scale)
                    lineTo(534f * scale, 242f * scale)
                    lineTo(530f * scale, 174f * scale)
                    cubicTo(529f * scale, 160f * scale, 520f * scale, 152f * scale, 506f * scale, 152f * scale)
                    lineTo(494f * scale, 152f * scale)
                    cubicTo(480f * scale, 152f * scale, 471f * scale, 160f * scale, 470f * scale, 174f * scale)
                    lineTo(466f * scale, 242f * scale)
                    close()
                }

                val crownBrush = Brush.linearGradient(
                    0.00f to Color(0xFF94E5FF),
                    0.28f to Color(0xFF3DB8FF),
                    0.65f to Color(0xFF007AFF),
                    1.00f to Color(0xFF0047BF),
                    start = Offset(415f * scale, 108f * scale),
                    end = Offset(585f * scale, 248f * scale)
                )
                drawPath(crownLoopPath, crownBrush)

                // Inner arch shaded underside
                val innerArchShade = Path().apply {
                    moveTo(464f * scale, 242f * scale)
                    lineTo(468f * scale, 172f * scale)
                    cubicTo(470f * scale, 156f * scale, 480f * scale, 148f * scale, 494f * scale, 148f * scale)
                    lineTo(506f * scale, 148f * scale)
                    cubicTo(520f * scale, 148f * scale, 530f * scale, 156f * scale, 532f * scale, 172f * scale)
                    lineTo(536f * scale, 242f * scale)
                    lineTo(522f * scale, 242f * scale)
                    lineTo(519f * scale, 176f * scale)
                    cubicTo(518f * scale, 166f * scale, 512f * scale, 160f * scale, 502f * scale, 160f * scale)
                    lineTo(498f * scale, 160f * scale)
                    cubicTo(488f * scale, 160f * scale, 482f * scale, 166f * scale, 481f * scale, 176f * scale)
                    lineTo(478f * scale, 242f * scale)
                    close()
                }
                drawPath(innerArchShade, Color(0xFF002670))

                // Top crown specular ridge
                val crownRidge = Path().apply {
                    moveTo(454f * scale, 128f * scale)
                    cubicTo(478f * scale, 120f * scale, 522f * scale, 120f * scale, 546f * scale, 128f * scale)
                }
                drawPath(crownRidge, Color.White.copy(alpha = 0.85f), style = Stroke(width = 6.5f * scale, cap = StrokeCap.Round))

                // B. Continuous-Curvature Bell Dome Body
                val bellDomePath = Path().apply {
                    moveTo(500f * scale, 222f * scale)
                    cubicTo(608f * scale, 222f * scale, 682f * scale, 288f * scale, 698f * scale, 398f * scale)
                    cubicTo(710f * scale, 482f * scale, 722f * scale, 566f * scale, 766f * scale, 648f * scale)
                    lineTo(234f * scale, 648f * scale)
                    cubicTo(278f * scale, 566f * scale, 290f * scale, 482f * scale, 302f * scale, 398f * scale)
                    cubicTo(318f * scale, 288f * scale, 392f * scale, 222f * scale, 500f * scale, 222f * scale)
                    close()
                }

                val domeBrush = Brush.linearGradient(
                    0.00f to Color(0xFF4CC9FF),
                    0.22f to Color(0xFF1A96FF),
                    0.52f to Color(0xFF007AFF),
                    0.78f to Color(0xFF0051D5),
                    1.00f to Color(0xFF002E8A),
                    start = Offset(234f * scale, 222f * scale),
                    end = Offset(766f * scale, 648f * scale)
                )

                val sheenBrush = Brush.horizontalGradient(
                    0.00f to Color(0x8C80DCFF),
                    0.14f to Color(0x52FFFFFF),
                    0.30f to Color(0x1438B6FF),
                    0.55f to Color(0x2E0040B8),
                    0.82f to Color(0x5C002270),
                    0.93f to Color(0x6147B8FF),
                    1.00f to Color(0xA699E5FF),
                    startX = 234f * scale,
                    endX = 766f * scale
                )

                drawPath(bellDomePath, domeBrush)
                drawPath(bellDomePath, sheenBrush)

                // Diagonal softbox gloss highlight on left dome curve
                val glossStripe = Path().apply {
                    moveTo(332f * scale, 245f * scale)
                    cubicTo(365f * scale, 232f * scale, 405f * scale, 228f * scale, 430f * scale, 228f * scale)
                    cubicTo(392f * scale, 340f * scale, 368f * scale, 485f * scale, 326f * scale, 642f * scale)
                    lineTo(254f * scale, 642f * scale)
                    cubicTo(294f * scale, 505f * scale, 306f * scale, 355f * scale, 332f * scale, 245f * scale)
                    close()
                }
                drawPath(glossStripe, Color.White.copy(alpha = 0.24f))

                // Dome Outer Specular Contour
                val specularBrush = Brush.linearGradient(
                    0.00f to Color(0xE6FFFFFF),
                    0.35f to Color(0x8C80DCFF),
                    0.70f to Color(0x1A0051D5),
                    1.00f to Color(0xB366D4FF),
                    start = Offset(234f * scale, 222f * scale),
                    end = Offset(766f * scale, 648f * scale)
                )
                drawPath(bellDomePath, specularBrush, style = Stroke(width = 3f * scale))

                // C. Three Precision-Milled Acoustic Sapphire Channels
                val capsuleRecess = Brush.linearGradient(
                    0.00f to Color(0xFF001B52),
                    0.45f to Color(0xFF002D80),
                    1.00f to Color(0xFF003DA6),
                    start = Offset(350f * scale, 330f * scale),
                    end = Offset(650f * scale, 590f * scale)
                )
                val capsuleBevel = Brush.linearGradient(
                    0.00f to Color(0xCC001238),
                    0.45f to Color(0x331A8CFF),
                    0.85f to Color(0xD970D6FF),
                    1.00f to Color(0xF2B8F0FF),
                    start = Offset(350f * scale, 330f * scale),
                    end = Offset(650f * scale, 590f * scale)
                )

                // Left Capsule (Tilted 7.5 deg)
                withTransform({
                    rotate(7.5f, pivot = Offset(384f * scale, 468f * scale))
                }) {
                    drawRoundRect(
                        brush = capsuleRecess,
                        topLeft = Offset(354f * scale, 348f * scale),
                        size = Size(60f * scale, 240f * scale),
                        cornerRadius = CornerRadius(30f * scale, 30f * scale)
                    )
                    drawRoundRect(
                        brush = capsuleBevel,
                        topLeft = Offset(354f * scale, 348f * scale),
                        size = Size(60f * scale, 240f * scale),
                        cornerRadius = CornerRadius(30f * scale, 30f * scale),
                        style = Stroke(width = 3.5f * scale)
                    )
                }

                // Center Capsule (Vertical)
                drawRoundRect(
                    brush = capsuleRecess,
                    topLeft = Offset(470f * scale, 334f * scale),
                    size = Size(60f * scale, 256f * scale),
                    cornerRadius = CornerRadius(30f * scale, 30f * scale)
                )
                drawRoundRect(
                    brush = capsuleBevel,
                    topLeft = Offset(470f * scale, 334f * scale),
                    size = Size(60f * scale, 256f * scale),
                    cornerRadius = CornerRadius(30f * scale, 30f * scale),
                    style = Stroke(width = 3.5f * scale)
                )

                // Right Capsule (Tilted -7.5 deg)
                withTransform({
                    rotate(-7.5f, pivot = Offset(616f * scale, 468f * scale))
                }) {
                    drawRoundRect(
                        brush = capsuleRecess,
                        topLeft = Offset(586f * scale, 348f * scale),
                        size = Size(60f * scale, 240f * scale),
                        cornerRadius = CornerRadius(30f * scale, 30f * scale)
                    )
                    drawRoundRect(
                        brush = capsuleBevel,
                        topLeft = Offset(586f * scale, 348f * scale),
                        size = Size(60f * scale, 240f * scale),
                        cornerRadius = CornerRadius(30f * scale, 30f * scale),
                        style = Stroke(width = 3.5f * scale)
                    )
                }

                // D. Underside Acoustic Cavity
                drawOval(
                    color = Color(0xFF001238),
                    topLeft = Offset((500f - 284f) * scale, (722f - 30f) * scale),
                    size = Size(568f * scale, 60f * scale)
                )
                drawOval(
                    color = Color(0xFF0051D5),
                    topLeft = Offset((500f - 284f) * scale, (722f - 30f) * scale),
                    size = Size(568f * scale, 60f * scale),
                    style = Stroke(width = 3.5f * scale)
                )

                // E. Swinging 3D Clapper Pendulum (Opposite phase swing)
                val clapperAngle = -1.25f * bellAngle
                withTransform({
                    rotate(clapperAngle, pivot = Offset(500f * scale, 680f * scale))
                }) {
                    // Clapper Stem
                    drawRoundRect(
                        color = Color(0xFF002D80),
                        topLeft = Offset(486f * scale, 680f * scale),
                        size = Size(28f * scale, 48f * scale),
                        cornerRadius = CornerRadius(14f * scale, 14f * scale)
                    )
                    // Clapper 3D Sphere Core
                    val clapperSphereBrush = Brush.radialGradient(
                        0.00f to Color(0xFFB8F0FF),
                        0.22f to Color(0xFF4CC9FF),
                        0.55f to Color(0xFF007AFF),
                        0.84f to Color(0xFF003DA6),
                        1.00f to Color(0xFF001B52),
                        center = Offset((500f - 56f * 0.32f) * scale, (764f - 56f * 0.32f) * scale),
                        radius = 56f * 1.4f * scale
                    )
                    drawCircle(clapperSphereBrush, radius = 56f * scale, center = Offset(500f * scale, 764f * scale))
                    drawCircle(specularBrush, radius = 54.5f * scale, center = Offset(500f * scale, 764f * scale), style = Stroke(width = 2.5f * scale))

                    // Specular hotspot
                    drawOval(
                        color = Color.White.copy(alpha = 0.88f),
                        topLeft = Offset((480f - 20f) * scale, (744f - 11f) * scale),
                        size = Size(40f * scale, 22f * scale)
                    )
                }

                // F. Cantilevered Flared Bell Rim Lip
                val bellRimPath = Path().apply {
                    moveTo(222f * scale, 632f * scale)
                    cubicTo(340f * scale, 624f * scale, 660f * scale, 624f * scale, 778f * scale, 632f * scale)
                    cubicTo(812f * scale, 635f * scale, 832f * scale, 654f * scale, 834f * scale, 678f * scale)
                    cubicTo(836f * scale, 704f * scale, 814f * scale, 724f * scale, 768f * scale, 728f * scale)
                    cubicTo(640f * scale, 738f * scale, 360f * scale, 738f * scale, 232f * scale, 728f * scale)
                    cubicTo(186f * scale, 724f * scale, 164f * scale, 704f * scale, 166f * scale, 678f * scale)
                    cubicTo(168f * scale, 654f * scale, 188f * scale, 635f * scale, 222f * scale, 632f * scale)
                    close()
                }

                val rimFrontBrush = Brush.horizontalGradient(
                    0.00f to Color(0xFF66D4FF),
                    0.14f to Color(0xFFB8EEFF),
                    0.30f to Color(0xFF29A3FF),
                    0.55f to Color(0xFF0073F0),
                    0.82f to Color(0xFF004BC7),
                    0.93f to Color(0xFF38B2FF),
                    1.00f to Color(0xFF75DAFF),
                    startX = 160f * scale,
                    endX = 840f * scale
                )

                drawPath(bellRimPath, rimFrontBrush)

                // Top rim crown surface
                val rimTopSurface = Path().apply {
                    moveTo(168f * scale, 666f * scale)
                    cubicTo(180f * scale, 642f * scale, 202f * scale, 632f * scale, 240f * scale, 629f * scale)
                    cubicTo(355f * scale, 621f * scale, 645f * scale, 621f * scale, 760f * scale, 629f * scale)
                    cubicTo(798f * scale, 632f * scale, 820f * scale, 642f * scale, 832f * scale, 666f * scale)
                    cubicTo(740f * scale, 678f * scale, 620f * scale, 682f * scale, 500f * scale, 682f * scale)
                    cubicTo(380f * scale, 682f * scale, 260f * scale, 678f * scale, 168f * scale, 666f * scale)
                    close()
                }
                val rimTopBrush = Brush.linearGradient(
                    0.00f to Color(0xFF8AE2FF),
                    0.35f to Color(0xFF38B2FF),
                    0.72f to Color(0xFF007AFF),
                    1.00f to Color(0xFF0051D5),
                    start = Offset(150f * scale, 620f * scale),
                    end = Offset(850f * scale, 682f * scale)
                )
                drawPath(rimTopSurface, rimTopBrush)

                // Specular ridge along rim bevel
                val rimRidge = Path().apply {
                    moveTo(186f * scale, 664f * scale)
                    cubicTo(310f * scale, 678f * scale, 690f * scale, 678f * scale, 814f * scale, 664f * scale)
                }
                drawPath(rimRidge, Color.White.copy(alpha = 0.85f), style = Stroke(width = 6f * scale, cap = StrokeCap.Round))
                drawPath(bellRimPath, specularBrush, style = Stroke(width = 2.8f * scale))

                // G. Floating 3D "All Caught Up" Checkmark Jewel Badge
                if (showCheckBadge) {
                    val badgeScale = 1f + 0.15f * progress
                    withTransform({
                        scale(badgeScale, badgeScale, pivot = Offset(710f * scale, 638f * scale))
                    }) {
                        // Outer jewel ring
                        val checkBadgeBrush = Brush.linearGradient(
                            0.00f to Color(0xFF70E1FF),
                            0.35f to Color(0xFF1AA3FF),
                            0.75f to Color(0xFF0066E6),
                            1.00f to Color(0xFF00378F),
                            start = Offset(632f * scale, 560f * scale),
                            end = Offset(788f * scale, 716f * scale)
                        )
                        drawCircle(checkBadgeBrush, radius = 78f * scale, center = Offset(710f * scale, 638f * scale))

                        // Recessed sapphire well
                        drawCircle(capsuleRecess, radius = 62f * scale, center = Offset(710f * scale, 638f * scale))
                        drawCircle(capsuleBevel, radius = 62f * scale, center = Offset(710f * scale, 638f * scale), style = Stroke(width = 3f * scale))

                        // Specular highlight on lens
                        drawOval(
                            color = Color.White.copy(alpha = 0.38f),
                            topLeft = Offset((688f - 36f) * scale, (602f - 16f) * scale),
                            size = Size(72f * scale, 32f * scale)
                        )

                        // 3D White Specular Checkmark
                        val checkPath = Path().apply {
                            moveTo(682f * scale, 640f * scale)
                            lineTo(702f * scale, 660f * scale)
                            lineTo(742f * scale, 616f * scale)
                        }
                        drawPath(
                            checkPath,
                            Color.White,
                            style = Stroke(width = 14f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Outer specular rim
                        drawCircle(specularBrush, radius = 78f * scale, center = Offset(710f * scale, 638f * scale), style = Stroke(width = 3f * scale))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UNIFIED APPLE EMPTY STATE VIEW COMPONENT (Matches TrashScreen Spec)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleEmptyStateView(
    graphic: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    actionButtonIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        graphic()

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            color = colors.textMain,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = colors.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionButtonText != null && onActionClick != null) {
            Spacer(Modifier.height(22.dp))
            LiquidGlassPrimaryButton(
                text = actionButtonText,
                leadingIcon = actionButtonIcon,
                onClick = onActionClick
            )
        }
    }
}
