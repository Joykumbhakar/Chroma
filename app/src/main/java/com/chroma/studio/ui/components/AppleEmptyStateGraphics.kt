package com.chroma.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
// 4. WORKS / CANVAS PALETTE GRAPHICS ICON (Sculpted Palette + 3D Paint Wells + Stylus Wand)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleCanvasPaletteGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val localScaleX = remember { Animatable(1f) }
    val localScaleY = remember { Animatable(1f) }
    val localWandAngle = remember { Animatable(0f) }

    fun triggerPaletteSpring() {
        scope.launch {
            launch { localWandAngle.animateTo(-16f, tween(180, easing = FastOutSlowInEasing)) }
            launch { localScaleX.animateTo(1.055f, tween(180)) }
            localScaleY.animateTo(0.945f, tween(180))

            launch { localWandAngle.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { localScaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            localScaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    LaunchedEffect(state.isAnimating) {
        if (state.isAnimating) {
            triggerPaletteSpring()
        }
    }

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            triggerPaletteSpring()
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
            brush = AppleHigBluePalette.floorGlowBrush(485f, 868f, 220f, s),
            topLeft = Offset(265f * s, 850f * s),
            size = Size(440f * s, 36f * s)
        )

        val activeScaleX = if (state.isAnimating) state.squashX.value else localScaleX.value
        val activeScaleY = if (state.isAnimating) state.squashY.value else localScaleY.value
        val activeWandAngle = if (state.isAnimating && state.articulationProgress.value > 0f) {
            -18f * state.articulationProgress.value
        } else {
            localWandAngle.value
        }

        withTransform({
            scale(activeScaleX, activeScaleY, pivot = Offset(500f * s, 840f * s))
        }) {
            val palettePath = Path().apply {
                moveTo(500f * s, 160f * s)
                cubicTo(313f * s, 160f * s, 160f * s, 313f * s, 160f * s, 500f * s)
                cubicTo(160f * s, 687f * s, 313f * s, 840f * s, 500f * s, 840f * s)
                cubicTo(531.5f * s, 840f * s, 556f * s, 814.6f * s, 556f * s, 782.6f * s)
                cubicTo(556f * s, 767.7f * s, 549.9f * s, 754.2f * s, 541.1f * s, 744.3f * s)
                cubicTo(531.3f * s, 734.5f * s, 526.2f * s, 722.2f * s, 526.2f * s, 706.1f * s)
                cubicTo(526.2f * s, 674.8f * s, 551.6f * s, 649.4f * s, 582.9f * s, 649.4f * s)
                lineTo(650.8f * s, 649.4f * s)
                cubicTo(754.5f * s, 649.4f * s, 839.7f * s, 564.3f * s, 839.7f * s, 460.5f * s)
                cubicTo(838.8f * s, 296.4f * s, 685.7f * s, 160f * s, 500f * s, 160f * s)
                close()
            }

            clipPath(palettePath) {
                // 1. Main Cupertino Blue Body + Cylindrical Metallic Sheen
                drawRect(
                    brush = AppleHigBluePalette.bodyBrush(s),
                    topLeft = Offset(150f * s, 150f * s),
                    size = Size(700f * s, 700f * s)
                )
                drawRect(
                    brush = AppleHigBluePalette.cylindricalSheenBrush(s),
                    topLeft = Offset(150f * s, 150f * s),
                    size = Size(700f * s, 700f * s)
                )

                with(AppleHigBluePalette) {
                    drawSoftboxGlossStripes(195f, 175f, 825f, 795f, s)
                }

                // 2. Four Recessed Sapphire Wells with Luminous 3D Paint Orbs
                val wells = listOf(
                    Offset(313f, 517f),
                    Offset(381f, 347f),
                    Offset(551f, 313f),
                    Offset(687f, 449f)
                )

                wells.forEach { well ->
                    val center = Offset(well.x * s, well.y * s)
                    drawCircle(
                        brush = AppleHigBluePalette.capsuleRecessBrush(well.y - 54f, well.y + 54f, s),
                        radius = 54f * s,
                        center = center
                    )
                    drawCircle(
                        brush = AppleHigBluePalette.luminousOrbBrush(center, 32f * s),
                        radius = 32f * s,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.78f),
                        radius = 9f * s,
                        center = Offset((well.x - 10f) * s, (well.y - 10f) * s)
                    )
                    drawCircle(
                        brush = AppleHigBluePalette.capsuleBevelBrush(well.y - 54f, well.y + 54f, s),
                        radius = 54f * s,
                        center = center,
                        style = Stroke(width = 4.8f * s)
                    )
                }

                // 3. Recessed Sapphire Thumb Grip Well
                val thumbWell = Offset(432f * s, 692f * s)
                drawCircle(
                    brush = AppleHigBluePalette.capsuleRecessBrush(634f, 750f, s),
                    radius = 56f * s,
                    center = thumbWell
                )
                drawCircle(
                    brush = AppleHigBluePalette.capsuleBevelBrush(634f, 750f, s),
                    radius = 56f * s,
                    center = thumbWell,
                    style = Stroke(width = 4.8f * s)
                )
            }

            // Outer Specular Bevel Contour
            drawPath(
                path = palettePath,
                brush = AppleHigBluePalette.specularStrokeBrush(s),
                style = Stroke(width = 3.4f * s)
            )

            // 4. Cantilevered 3D Sculpted Brush / Stylus Wand
            withTransform({
                rotate(activeWandAngle, pivot = Offset(610f * s, 560f * s))
            }) {
                withTransform({
                    rotate(-38f, pivot = Offset(640f * s, 530f * s))
                }) {
                    drawRoundRect(
                        color = Color(0x70001238),
                        topLeft = Offset(612f * s, 342f * s),
                        size = Size(54f * s, 320f * s),
                        cornerRadius = CornerRadius(27f * s, 27f * s)
                    )
                    drawRoundRect(
                        brush = AppleHigBluePalette.handleBrush(s),
                        topLeft = Offset(604f * s, 330f * s),
                        size = Size(52f * s, 320f * s),
                        cornerRadius = CornerRadius(26f * s, 26f * s)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.84f),
                        topLeft = Offset(604f * s, 330f * s),
                        size = Size(52f * s, 320f * s),
                        cornerRadius = CornerRadius(26f * s, 26f * s),
                        style = Stroke(width = 3f * s)
                    )
                }
            }
        }
    }
}

@Composable
fun AppleWorksGraphic(
    modifier: Modifier = Modifier.size(165.dp),
    state: AppleGraphicAnimationState = rememberAppleGraphicAnimationState(),
    enableClickToAnimate: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    AppleCanvasPaletteGraphic(
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
    AppleCanvasPaletteGraphic(
        modifier = modifier,
        enableClickToAnimate = true,
        onClick = onClick
    )
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
