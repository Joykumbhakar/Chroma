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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Apple Human Interface Guidelines (HIG) Professional Blue Gradient Palette.
 * Crafted to deliver a clean, precision-anodized Cupertino aesthetic.
 */
object AppleBluePalette {
    val KeyLightCyan = Color(0xFF5AC8FA)
    val HighlightSky = Color(0xFF4CC9FF)
    val ElectricAzure = Color(0xFF1A96FF)
    val SystemBlue = Color(0xFF007AFF)
    val RoyalCobalt = Color(0xFF0051D5)
    val DeepSapphire = Color(0xFF002E8A)
    val MidnightCavity = Color(0xFF001238)
    val RecessedTop = Color(0xFF001B52)
    val RecessedBottom = Color(0xFF003DA6)
    val SpecularWhite = Color(0xFFFFFFFF)
    val SpecularIce = Color(0xFFB8EEFF)
}

/**
 * State controller for triggering multi-stage physics animations on [AppleTrashBinIcon].
 *
 * Call [triggerDeleteAnimation] from any coroutine scope (e.g. button click, swipe-to-delete)
 * to orchestrate the lid opening, document card drop-in, and elastic bin squash.
 */
@Stable
class AppleTrashState {
    var isLidOpen by mutableStateOf(false)
        private set

    val documentProgress = Animatable(0f)
    val squashX = Animatable(1f)
    val squashY = Animatable(1f)
    val shakeAngle = Animatable(0f)

    var isAnimating by mutableStateOf(false)
        private set

    /**
     * Toggles the lid open or closed using spring physics.
     */
    fun toggleLid() {
        isLidOpen = !isLidOpen
    }

    /**
     * Runs the signature Apple Delete animation:
     * 1. Hinges lid open with medium-bouncy spring
     * 2. Arcs a document card into the open bin cavity
     * 3. Triggers an elastic squash-and-stretch impact on the bin body
     * 4. Closes the lid smoothly
     */
    suspend fun triggerDeleteAnimation() {
        if (isAnimating) return
        isAnimating = true
        val wasOpen = isLidOpen

        coroutineScope {
            // Step 1: Pivot lid open
            isLidOpen = true
            documentProgress.snapTo(0f)

            // Step 2: Arc document into the bin mouth
            launch {
                documentProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
                )
                documentProgress.snapTo(0f)
            }

            // Step 3: Trigger elastic squash & rebound when document lands inside
            launch {
                delay(410)
                launch {
                    squashX.animateTo(1.065f, tween(110))
                    squashX.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    squashY.animateTo(0.935f, tween(110))
                    squashY.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }

            // Step 4: Close lid after item drops in
            delay(650)
            if (!wasOpen) {
                isLidOpen = false
            }
            delay(250)
            isAnimating = false
        }
    }

    /**
     * Runs an "Empty Trash" harmonic shake with lid flutter.
     */
    suspend fun triggerEmptyShake() {
        if (isAnimating) return
        isAnimating = true
        val wasOpen = isLidOpen
        isLidOpen = true

        val keyframes = listOf(-7.5f, 7f, -5f, 3.5f, -1.5f, 0f)
        for (angle in keyframes) {
            shakeAngle.animateTo(
                targetValue = angle,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }

        if (!wasOpen) isLidOpen = false
        isAnimating = false
    }
}

@Composable
fun rememberAppleTrashState(): AppleTrashState {
    return remember { AppleTrashState() }
}

/**
 * Clean Apple-redesigned Trash Bin Graphic rendered 100% natively in Jetpack Compose [Canvas].
 *
 * Features:
 * - Zero external SVG/XML drawable dependencies (scales losslessly from 24.dp to 512.dp)
 * - Multi-stop Cupertino Blue gradients with anodized specular bevels
 * - Three precision-milled vertical capsule flutes that follow the tapered perspective
 * - Built-in spring physics for lid hinging, document toss, and elastic impact rebound
 */
@Composable
fun AppleTrashBinIcon(
    modifier: Modifier = Modifier.size(160.dp),
    state: AppleTrashState = rememberAppleTrashState(),
    showSquircleBackdrop: Boolean = false,
    enableClickToAnimate: Boolean = true,
    onDeleteComplete: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    // Spring-driven lid rotation (-36 degrees when open)
    val lidRotation by animateFloatAsState(
        targetValue = if (state.isLidOpen) -36f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LidSpringRotation"
    )

    // Spring-driven lid lift offset
    val lidLiftProgress by animateFloatAsState(
        targetValue = if (state.isLidOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LidLiftProgress"
    )

    val clickModifier = if (enableClickToAnimate) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            scope.launch {
                state.triggerDeleteAnimation()
                onDeleteComplete?.invoke()
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(clickModifier)) {
        // Normalize all drawing to a 1000x1000 virtual studio viewport
        val scaleFactor = size.minDimension / 1000f

        withTransform({
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
            // Apply elastic squash & stretch anchored at the bottom base of the bin (500, 868)
            scale(
                scaleX = state.squashX.value,
                scaleY = state.squashY.value,
                pivot = Offset(500f, 868f)
            )
            // Apply harmonic shake rotation anchored at center of mass (500, 620)
            rotate(
                degrees = state.shakeAngle.value,
                pivot = Offset(500f, 620f)
            )
        }) {
            // Optional iOS App Icon Squircle Plinth
            if (showSquircleBackdrop) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFF0F6FF), Color(0xFFDCEAFF)),
                        start = Offset(150f, 50f),
                        end = Offset(850f, 950f)
                    ),
                    topLeft = Offset(90f, 90f),
                    size = Size(820f, 820f),
                    cornerRadius = CornerRadius(190f, 190f)
                )
                drawRoundRect(
                    color = Color(0xFFCFE2FF),
                    topLeft = Offset(90f, 90f),
                    size = Size(820f, 820f),
                    cornerRadius = CornerRadius(190f, 190f),
                    style = Stroke(width = 3f)
                )
            }

            // 1. Ambient Blue Studio Floor Glow
            drawFloorShadow()

            // 2. Animated Document Projectile (Visible during triggerDeleteAnimation)
            val docT = state.documentProgress.value
            if (docT > 0.01f && docT < 0.99f) {
                drawTossedDocument(progress = docT)
            }

            // 3. Open Interior Sapphire Cavity (Revealed as lid opens)
            if (lidLiftProgress > 0.02f) {
                drawInteriorCavity(openFraction = lidLiftProgress)
            }

            // 4. Tapered Cupertino Blue Bin Body + 3 Recessed Capsule Channels
            drawTaperedBinBody()

            // 5. Pivoting Cantilevered Lid & Sculpted Top Handle
            withTransform({
                translate(
                    left = -16f * lidLiftProgress,
                    top = -26f * lidLiftProgress
                )
                rotate(
                    degrees = lidRotation,
                    pivot = Offset(240f, 350f)
                )
            }) {
                drawSculptedLidAndHandle()
            }
        }
    }
}

private fun DrawScope.drawFloorShadow() {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                AppleBluePalette.RoyalCobalt.copy(alpha = 0.28f),
                AppleBluePalette.SystemBlue.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(500f, 882f),
            radius = 220f
        ),
        topLeft = Offset(280f, 860f),
        size = Size(440f, 44f)
    )
}

private fun DrawScope.drawInteriorCavity(openFraction: Float) {
    val cavityHeight = 36f * openFraction.coerceIn(0.2f, 1.25f)
    drawOval(
        color = AppleBluePalette.MidnightCavity,
        topLeft = Offset(238f, 372f - cavityHeight / 2f),
        size = Size(524f, cavityHeight)
    )
    drawOval(
        color = AppleBluePalette.RoyalCobalt.copy(alpha = 0.7f),
        topLeft = Offset(238f, 372f - cavityHeight / 2f),
        size = Size(524f, cavityHeight),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawTossedDocument(progress: Float) {
    // Parabolic arc from top-right (740, 110) into bin center (500, 460)
    val startX = 720f
    val endX = 500f
    val currentX = startX + (endX - startX) * progress

    // Parabolic Y trajectory peaking at t = 0.35
    val arcLift = -190f * (4f * progress * (1f - progress))
    val baseLineY = 140f + (450f - 140f) * progress
    val currentY = baseLineY + arcLift

    val scale = (0.85f - 0.45f * progress).coerceAtLeast(0.3f)
    val rotation = 20f - 240f * progress
    val alpha = when {
        progress < 0.15f -> progress / 0.15f
        progress > 0.85f -> (1f - progress) / 0.15f
        else -> 1f
    }.coerceIn(0f, 1f)

    withTransform({
        translate(left = currentX, top = currentY)
        rotate(degrees = rotation, pivot = Offset.Zero)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
    }) {
        drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(-42f, -54f),
            size = Size(84f, 108f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = AppleBluePalette.ElectricAzure.copy(alpha = alpha),
            topLeft = Offset(-26f, -32f),
            size = Size(38f, 8f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = Color(0xFF94A3B8).copy(alpha = alpha * 0.65f),
            topLeft = Offset(-26f, -14f),
            size = Size(52f, 6f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFF94A3B8).copy(alpha = alpha * 0.65f),
            topLeft = Offset(-26f, 2f),
            size = Size(44f, 6f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }
}

private fun DrawScope.drawTaperedBinBody() {
    val bodyPath = Path().apply {
        moveTo(236f, 372f)
        lineTo(764f, 372f)
        cubicTo(766f, 382f, 765f, 392f, 763f, 404f)
        lineTo(716f, 778f)
        cubicTo(708f, 836f, 668f, 866f, 606f, 868f)
        cubicTo(536f, 870f, 464f, 870f, 394f, 868f)
        cubicTo(332f, 866f, 292f, 836f, 284f, 778f)
        lineTo(237f, 404f)
        cubicTo(235f, 392f, 234f, 382f, 236f, 372f)
        close()
    }

    clipPath(bodyPath) {
        // 1. Primary Apple Cupertino Blue Linear Gradient (Top-Left Key Light to Deep Sapphire)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    AppleBluePalette.HighlightSky,
                    AppleBluePalette.ElectricAzure,
                    AppleBluePalette.SystemBlue,
                    AppleBluePalette.RoyalCobalt,
                    AppleBluePalette.DeepSapphire
                ),
                start = Offset(240f, 370f),
                end = Offset(760f, 870f)
            ),
            topLeft = Offset(225f, 365f),
            size = Size(550f, 515f)
        )

        // 2. Horizontal Cylindrical Anodized Metallic Sheen
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF80DCFF).copy(alpha = 0.50f),
                    0.14f to Color.White.copy(alpha = 0.30f),
                    0.32f to Color(0xFF38B6FF).copy(alpha = 0.06f),
                    0.56f to Color(0xFF0040B8).copy(alpha = 0.18f),
                    0.82f to Color(0xFF002270).copy(alpha = 0.34f),
                    0.93f to Color(0xFF47B8FF).copy(alpha = 0.36f),
                    1.00f to Color(0xFF99E5FF).copy(alpha = 0.60f)
                ),
                startX = 235f,
                endX = 765f
            ),
            topLeft = Offset(225f, 365f),
            size = Size(550f, 515f)
        )

        // 3. Left Studio Softbox Specular Streak
        val leftGlossPath = Path().apply {
            moveTo(260f, 375f)
            lineTo(296f, 375f)
            lineTo(336f, 855f)
            lineTo(304f, 855f)
            close()
        }
        drawPath(
            path = leftGlossPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                startY = 375f,
                endY = 855f
            )
        )

        // 4. Under-Lid Neck Shadow Groove
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    AppleBluePalette.MidnightCavity.copy(alpha = 0.78f),
                    AppleBluePalette.DeepSapphire.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                startY = 372f,
                endY = 402f
            ),
            topLeft = Offset(230f, 372f),
            size = Size(540f, 30f)
        )
    }

    // 5. Outer Specular Bevel Rim around Body
    drawPath(
        path = bodyPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.78f),
                AppleBluePalette.KeyLightCyan.copy(alpha = 0.45f),
                AppleBluePalette.RoyalCobalt.copy(alpha = 0.15f),
                AppleBluePalette.KeyLightCyan.copy(alpha = 0.65f)
            ),
            start = Offset(235f, 372f),
            end = Offset(765f, 868f)
        ),
        style = Stroke(width = 3f)
    )

    // 6. Three Precision-Milled Recessed Vertical Capsule Channels
    drawRecessedCapsule(centerX = 382f, centerY = 632f, tiltDegrees = -2.2f)
    drawRecessedCapsule(centerX = 500f, centerY = 632f, tiltDegrees = 0f)
    drawRecessedCapsule(centerX = 618f, centerY = 632f, tiltDegrees = 2.2f)
}

private fun DrawScope.drawRecessedCapsule(
    centerX: Float,
    centerY: Float,
    tiltDegrees: Float
) {
    val capsuleWidth = 68f
    val capsuleHeight = 348f
    val left = centerX - capsuleWidth / 2f
    val top = centerY - capsuleHeight / 2f
    val cornerRadius = CornerRadius(34f, 34f)

    withTransform({
        rotate(degrees = tiltDegrees, pivot = Offset(centerX, centerY))
    }) {
        val capsulePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = left + capsuleWidth,
                    bottom = top + capsuleHeight,
                    cornerRadius = cornerRadius
                )
            )
        }

        // Deep Sapphire Recessed Interior
        drawPath(
            path = capsulePath,
            brush = Brush.linearGradient(
                colors = listOf(
                    AppleBluePalette.RecessedTop,
                    Color(0xFF002D80),
                    AppleBluePalette.RecessedBottom
                ),
                start = Offset(left, top),
                end = Offset(left + capsuleWidth, top + capsuleHeight)
            )
        )

        // Inner Left/Top Depth Shadow
        clipPath(capsulePath) {
            drawRoundRect(
                color = AppleBluePalette.MidnightCavity.copy(alpha = 0.75f),
                topLeft = Offset(left + 4f, top + 4f),
                size = Size(capsuleWidth, capsuleHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = 10f)
            )
        }

        // Crisp Anodized Outer Bevel Rim Highlight
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    AppleBluePalette.MidnightCavity.copy(alpha = 0.8f),
                    AppleBluePalette.ElectricAzure.copy(alpha = 0.25f),
                    AppleBluePalette.KeyLightCyan.copy(alpha = 0.85f),
                    AppleBluePalette.SpecularIce.copy(alpha = 0.95f)
                ),
                start = Offset(left, top),
                end = Offset(left + capsuleWidth, top + capsuleHeight)
            ),
            topLeft = Offset(left, top),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = 4f)
        )
    }
}

private fun DrawScope.drawSculptedLidAndHandle() {
    // ---------------- 1. CANTILEVERED LID ----------------
    val lidPath = Path().apply {
        moveTo(325f, 256f)
        cubicTo(410f, 251f, 590f, 251f, 675f, 256f)
        cubicTo(742f, 260f, 782f, 278f, 788f, 302f)
        cubicTo(791f, 314f, 791f, 344f, 786f, 358f)
        cubicTo(781f, 369f, 755f, 374f, 690f, 377f)
        cubicTo(585f, 381f, 415f, 381f, 310f, 377f)
        cubicTo(245f, 374f, 219f, 369f, 214f, 358f)
        cubicTo(209f, 344f, 209f, 314f, 212f, 302f)
        cubicTo(218f, 278f, 258f, 260f, 325f, 256f)
        close()
    }

    clipPath(lidPath) {
        // Lid Vertical Rim Multi-Stop Metallic Blue Gradient
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF66D4FF),
                    0.12f to AppleBluePalette.SpecularIce,
                    0.28f to Color(0xFF29A3FF),
                    0.55f to AppleBluePalette.SystemBlue,
                    0.82f to Color(0xFF004BC7),
                    0.93f to Color(0xFF38B2FF),
                    1.00f to Color(0xFF75DAFF)
                ),
                startX = 210f,
                endX = 790f
            ),
            topLeft = Offset(205f, 295f),
            size = Size(590f, 90f)
        )

        // Lid Top Crown Surface
        val crownPath = Path().apply {
            moveTo(208f, 304f)
            cubicTo(218f, 276f, 262f, 258f, 325f, 255f)
            cubicTo(410f, 250f, 590f, 250f, 675f, 255f)
            cubicTo(738f, 258f, 782f, 276f, 792f, 304f)
            cubicTo(730f, 313f, 615f, 316f, 500f, 316f)
            cubicTo(385f, 316f, 270f, 313f, 208f, 304f)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8AE2FF),
                    Color(0xFF38B2FF),
                    AppleBluePalette.SystemBlue,
                    AppleBluePalette.RoyalCobalt
                ),
                start = Offset(250f, 250f),
                end = Offset(750f, 320f)
            )
        )

        // Crisp White Specular Ridge Along Lid Front Bevel
        val ridgePath = Path().apply {
            moveTo(222f, 302f)
            cubicTo(330f, 314f, 670f, 314f, 778f, 302f)
        }
        drawPath(
            path = ridgePath,
            color = Color.White.copy(alpha = 0.80f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Bottom Roll-Under Shadow on Lid Lip
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    AppleBluePalette.DeepSapphire.copy(alpha = 0.65f)
                ),
                startY = 352f,
                endY = 380f
            ),
            topLeft = Offset(205f, 352f),
            size = Size(590f, 30f)
        )
    }

    // Lid Outer Specular Rim
    drawPath(
        path = lidPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                AppleBluePalette.KeyLightCyan.copy(alpha = 0.45f),
                AppleBluePalette.RoyalCobalt.copy(alpha = 0.25f)
            ),
            start = Offset(215f, 255f),
            end = Offset(785f, 378f)
        ),
        style = Stroke(width = 2.5f)
    )

    // ---------------- 2. SCULPTED TOP HANDLE ARCH ----------------
    val handlePath = Path().apply {
        moveTo(358f, 266f)
        lineTo(365f, 178f)
        cubicTo(368f, 146f, 390f, 130f, 426f, 130f)
        lineTo(574f, 130f)
        cubicTo(610f, 130f, 632f, 146f, 635f, 178f)
        lineTo(642f, 266f)
        cubicTo(642f, 271f, 626f, 273f, 612f, 273f)
        cubicTo(598f, 273f, 586f, 271f, 586f, 266f)
        lineTo(581f, 196f)
        cubicTo(579f, 182f, 570f, 176f, 554f, 176f)
        lineTo(446f, 176f)
        cubicTo(430f, 176f, 421f, 182f, 419f, 196f)
        lineTo(414f, 266f)
        cubicTo(414f, 271f, 402f, 273f, 388f, 273f)
        cubicTo(374f, 273f, 358f, 271f, 358f, 266f)
        close()
    }

    clipPath(handlePath) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF94E5FF),
                    Color(0xFF3DB8FF),
                    AppleBluePalette.SystemBlue,
                    Color(0xFF0047BF)
                ),
                start = Offset(360f, 130f),
                end = Offset(640f, 272f)
            ),
            topLeft = Offset(350f, 125f),
            size = Size(300f, 155f)
        )

        // Top Handle Bar Specular Ridge
        val handleHighlight = Path().apply {
            moveTo(392f, 146f)
            cubicTo(425f, 138f, 575f, 138f, 608f, 146f)
        }
        drawPath(
            path = handleHighlight,
            color = Color.White.copy(alpha = 0.82f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
    }

    drawPath(
        path = handlePath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                AppleBluePalette.KeyLightCyan.copy(alpha = 0.4f),
                AppleBluePalette.DeepSapphire.copy(alpha = 0.5f)
            ),
            start = Offset(360f, 130f),
            end = Offset(640f, 270f)
        ),
        style = Stroke(width = 2.5f, join = StrokeJoin.Round)
    )
}
