package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Exact color tokens matching the Apple-style beveled HTML/CSS design:
 * - Outer silver/white machined housing (#F5F5F7 -> #E5E5EA)
 * - Inner Apple System Blue (#148EFF -> #007AFF -> #0062CC)
 * - Inner Apple Pressed Blue (#0062CC -> #006BE6 -> #0071EB)
 * - Inner Apple Active Black (#3A3A3C -> #1D1D1F -> #000000)
 */
private object AppleBevelTokens {
    // Housing Colors
    val HousingTop = Color(0xFFF5F5F7)
    val HousingBottom = Color(0xFFE5E5EA)
    val HousingBorder = Color(0x1F000000) // rgba(0, 0, 0, 0.12)
    val HousingTopHighlight = Color(0xF2FFFFFF) // rgba(255, 255, 255, 0.95)
    val HousingBottomShade = Color(0x14000000) // rgba(0, 0, 0, 0.08)

    // Default Apple Blue State (#007AFF core)
    val BlueTop = Color(0xFF148EFF)
    val BlueMid = Color(0xFF007AFF)
    val BlueBottom = Color(0xFF0062CC)
    val BlueBorder = Color(0xFF0051A8)
    val BluePressedTop = Color(0xFF0062CC)
    val BluePressedMid = Color(0xFF006BE6)
    val BluePressedBottom = Color(0xFF0071EB)

    // Active / Toggled Apple Black State (#1D1D1F core)
    val BlackTop = Color(0xFF3A3A3C)
    val BlackMid = Color(0xFF1D1D1F)
    val BlackBottom = Color(0xFF000000)
    val BlackPressedTop = Color(0xFF000000)
    val BlackPressedMid = Color(0xFF141416)
    val BlackPressedBottom = Color(0xFF242426)
    val BlackBorder = Color(0xFF000000)

    // Plus Icon Gradient Stops
    val PlusTop = Color(0xFFFFFFFF)
    val PlusMid = Color(0xFFF5F5F7)
    val PlusBottom = Color(0xFFDCE1E8)
}

/**
 * An Apple-style machined bevel "+" button designed for inserting transitions between video clips.
 *
 * Improvements included:
 * 1. Smoothly animated multi-stop gradients & inset bevel highlights across Default, Pressed, and Active states.
 * 2. True crescent-shaped CSS `inset` bevel rendering via `PathOperation.Difference` geometry.
 * 3. Proportional scaling of housing padding, corner radii, and glyph geometry for any [buttonSize].
 * 4. Cached "+" vector path with multi-layered soft drop shadows and native tactile haptic feedback.
 *
 * @param isActive Whether the button is currently in its toggled/selected state (Apple Black).
 * @param onClick Callback invoked when the user taps the transition button.
 * @param modifier Optional [Modifier] for positioning the button on a video timeline seam.
 * @param buttonSize Size of the inner tactile button (defaults to 52.dp; scales proportionally for smaller timeline seams).
 * @param activeRotationDegrees Rotation angle of the "+" icon when [isActive] is true (90f for spun plus, or 45f for close "×").
 * @param enabled Whether the button is interactive.
 * @param enableHaptics Whether to trigger subtle physical haptic feedback on press and toggle.
 * @param customIcon Optional custom [ImageVector] to display instead of the default plus glyph.
 */
@Composable
fun AppleBeveledTransitionButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    activeRotationDegrees: Float = 90f,
    enabled: Boolean = true,
    enableHaptics: Boolean = true,
    customIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticFeedback = LocalHapticFeedback.current

    // Proportional geometry scale relative to the 52.dp reference specification
    val sizeScale = (buttonSize / 52.dp).coerceAtLeast(0.45f)
    val housingPadding = (4.dp * sizeScale).coerceAtLeast(2.dp)
    val innerCornerRadius = 14.dp * sizeScale
    val outerCornerRadius = innerCornerRadius + housingPadding
    val iconSize = 24.dp * sizeScale

    // Subtle tactile haptic tick when the finger depresses the physical bevel
    LaunchedEffect(isPressed) {
        if (isPressed && enabled && enableHaptics) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Physical vertical press translation (matches `.apple-bevel-housing:active { transform: translateY(0.5px) }`)
    val housingOffsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) (0.75.dp * sizeScale) else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "HousingPressOffsetY"
    )

    // Subtle elevation compression on press
    val housingElevation by animateDpAsState(
        targetValue = if (isPressed && enabled) (3.dp * sizeScale) else (8.dp * sizeScale),
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "HousingElevation"
    )

    val innerElevation by animateDpAsState(
        targetValue = if (isPressed && enabled) (1.dp * sizeScale) else (3.dp * sizeScale),
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "InnerElevation"
    )

    // "+" Icon press scale (0.94f when pressed)
    val iconScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "PlusIconScale"
    )

    // "+" Icon rotation (smoothly rotates when active)
    val iconRotation by animateFloatAsState(
        targetValue = if (isActive) activeRotationDegrees else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PlusIconRotation"
    )

    val colorSpec = tween<Color>(durationMillis = 160, easing = FastOutSlowInEasing)

    // Smoothly animated 3-stop vertical gradient colors
    val gradTop by animateColorAsState(
        targetValue = when {
            isActive && isPressed -> AppleBevelTokens.BlackPressedTop
            isActive -> AppleBevelTokens.BlackTop
            isPressed -> AppleBevelTokens.BluePressedTop
            else -> AppleBevelTokens.BlueTop
        },
        animationSpec = colorSpec,
        label = "GradTopColor"
    )

    val gradMid by animateColorAsState(
        targetValue = when {
            isActive && isPressed -> AppleBevelTokens.BlackPressedMid
            isActive -> AppleBevelTokens.BlackMid
            isPressed -> AppleBevelTokens.BluePressedMid
            else -> AppleBevelTokens.BlueMid
        },
        animationSpec = colorSpec,
        label = "GradMidColor"
    )

    val gradBottom by animateColorAsState(
        targetValue = when {
            isActive && isPressed -> AppleBevelTokens.BlackPressedBottom
            isActive -> AppleBevelTokens.BlackBottom
            isPressed -> AppleBevelTokens.BluePressedBottom
            else -> AppleBevelTokens.BlueBottom
        },
        animationSpec = colorSpec,
        label = "GradBottomColor"
    )

    // Smooth border color transition between Blue (#0051A8) and Black (#000000)
    val innerBorderColor by animateColorAsState(
        targetValue = if (isActive) AppleBevelTokens.BlackBorder else AppleBevelTokens.BlueBorder,
        animationSpec = colorSpec,
        label = "InnerBorderColor"
    )

    // Smoothly animated inset bevel colors for top specular edge, bottom shade, and side rims
    val topInsetColor by animateColorAsState(
        targetValue = when {
            isPressed && isActive -> Color.Black.copy(alpha = 0.75f)
            isPressed -> Color(0x8C001D42) // Recessed top inner shadow on press
            isActive -> Color.White.copy(alpha = 0.38f)
            else -> Color.White.copy(alpha = 0.58f)
        },
        animationSpec = colorSpec,
        label = "TopInsetColor"
    )

    val bottomInsetColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White.copy(alpha = 0.25f)
            isActive -> Color.Black.copy(alpha = 0.78f)
            else -> Color(0x73001D42) // rgba(0, 29, 66, 0.45)
        },
        animationSpec = colorSpec,
        label = "BottomInsetColor"
    )

    val sideInsetColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.Transparent
            isActive -> Color.White.copy(alpha = 0.14f)
            else -> Color.White.copy(alpha = 0.22f)
        },
        animationSpec = colorSpec,
        label = "SideInsetColor"
    )

    val topBevelDepth by animateDpAsState(
        targetValue = (if (isPressed) 2.25.dp else 1.5.dp) * sizeScale,
        animationSpec = tween(durationMillis = 140),
        label = "TopBevelDepth"
    )

    val bottomBevelDepth by animateDpAsState(
        targetValue = (if (isPressed) 1.dp else 2.dp) * sizeScale,
        animationSpec = tween(durationMillis = 140),
        label = "BottomBevelDepth"
    )

    val outerShape = RoundedCornerShape(outerCornerRadius)
    val innerShape = RoundedCornerShape(innerCornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .offset(y = housingOffsetY)
            .shadow(
                elevation = housingElevation,
                shape = outerShape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(outerShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppleBevelTokens.HousingTop,
                        AppleBevelTokens.HousingBottom
                    )
                )
            )
            // Outer housing machined inset top highlight & bottom shade
            .drawMachinedInsetBevel(
                cornerRadius = outerCornerRadius,
                topHighlightColor = AppleBevelTokens.HousingTopHighlight,
                topHighlightOffset = 1.25.dp * sizeScale,
                bottomShadeColor = AppleBevelTokens.HousingBottomShade,
                bottomShadeOffset = 1.25.dp * sizeScale,
                sideHighlightColor = Color.White.copy(alpha = 0.55f),
                borderInset = 0.75.dp * sizeScale
            )
            .border(
                width = (1.dp * sizeScale).coerceAtLeast(0.5.dp),
                color = AppleBevelTokens.HousingBorder,
                shape = outerShape
            )
            .padding(housingPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .shadow(
                    elevation = innerElevation,
                    shape = innerShape,
                    ambientColor = innerBorderColor,
                    spotColor = innerBorderColor
                )
                .clip(innerShape)
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to gradTop,
                        0.53f to gradMid,
                        1.0f to gradBottom
                    )
                )
                .drawMachinedInsetBevel(
                    cornerRadius = innerCornerRadius,
                    topHighlightColor = topInsetColor,
                    topHighlightOffset = topBevelDepth,
                    bottomShadeColor = bottomInsetColor,
                    bottomShadeOffset = bottomBevelDepth,
                    sideHighlightColor = sideInsetColor,
                    borderInset = (1.dp * sizeScale).coerceAtLeast(0.5.dp)
                )
                .border(
                    width = (1.dp * sizeScale).coerceAtLeast(0.5.dp),
                    color = innerBorderColor,
                    shape = innerShape
                )
                .semantics {
                    contentDescription = if (isActive) "Close Transition Picker" else "Add Transition"
                    stateDescription = if (isActive) "Active" else "Inactive"
                    role = Role.Button
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // Uses authentic Apple physical bevel depression instead of Material ripple
                    enabled = enabled,
                    onClick = {
                        if (enableHaptics) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (customIcon != null) {
                Icon(
                    imageVector = customIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = iconRotation
                        }
                )
            } else {
                BeveledPlusGlyph(
                    isActive = isActive,
                    isPressed = isPressed,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = iconRotation
                        }
                )
            }
        }
    }
}

/**
 * Draws the exact rounded "+" icon from the SVG specification with cached `PathOperation.Union`
 * geometry so zero Path allocations occur during press or rotation animations.
 *
 * Includes:
 * - Top white specular edge highlight (`0 -0.5dp`)
 * - Multi-step feathered bottom drop shadow (`0 1.5dp`)
 * - Vertical metallic-white gradient fill (`#FFFFFF` -> `#F5F5F7` -> `#DCE1E8`)
 * - Crisp inner bevel rim stroke (`rgba(255, 255, 255, 0.88)`)
 */
@Composable
private fun BeveledPlusGlyph(
    isActive: Boolean,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    val shadowBaseColor by animateColorAsState(
        targetValue = if (isActive) {
            Color.Black.copy(alpha = 0.62f)
        } else {
            Color(0x6B00193C) // rgba(0, 25, 60, 0.42)
        },
        animationSpec = tween(durationMillis = 160),
        label = "GlyphShadowColor"
    )

    val topSpecularAlpha by animateFloatAsState(
        targetValue = if (isActive || isPressed) 0.15f else 0.42f,
        animationSpec = tween(durationMillis = 140),
        label = "GlyphTopSpecularAlpha"
    )

    Spacer(
        modifier = modifier.drawWithCache {
            // Scale factor from the 24x24 SVG viewBox
            val scale = size.minDimension / 24f
            if (scale <= 0.001f || size.width <= 0f || size.height <= 0f) {
                return@drawWithCache onDrawBehind { }
            }

            // SVG geometry constants:
            // Vertical bar: x = 10.5..13.5 (width 3), y = 4.25..19.75 (height 15.5), rx = 1.5
            // Horizontal bar: x = 4.25..19.75 (width 15.5), y = 10.5..13.5 (height 3), rx = 1.5
            val vLeft = 10.5f * scale
            val vTop = 4.25f * scale
            val vWidth = 3.0f * scale
            val vHeight = 15.5f * scale

            val hLeft = 4.25f * scale
            val hTop = 10.5f * scale
            val hWidth = 15.5f * scale
            val hHeight = 3.0f * scale

            val pillRadius = CornerRadius(1.5f * scale, 1.5f * scale)

            // Pre-compute unified "+" Path once per layout size
            val verticalBar = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = vLeft,
                        top = vTop,
                        right = vLeft + vWidth,
                        bottom = vTop + vHeight,
                        cornerRadius = pillRadius
                    )
                )
            }
            val horizontalBar = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = hLeft,
                        top = hTop,
                        right = hLeft + hWidth,
                        bottom = hTop + hHeight,
                        cornerRadius = pillRadius
                    )
                )
            }

            val plusPath = Path().apply {
                op(verticalBar, horizontalBar, PathOperation.Union)
            }

            val plusGradient = Brush.verticalGradient(
                0.0f to AppleBevelTokens.PlusTop,
                0.75f to AppleBevelTokens.PlusMid,
                1.0f to AppleBevelTokens.PlusBottom,
                startY = vTop,
                endY = vTop + vHeight
            )

            val rimStroke = Stroke(
                width = 0.42f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            val shadowSpreadStroke = Stroke(
                width = 0.85f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            onDrawBehind {
                // 1. Top subtle white specular edge: drop-shadow(0 -0.5px 0px rgba(255, 255, 255, 0.4))
                if (topSpecularAlpha > 0.01f) {
                    translate(left = 0f, top = -0.5f * scale) {
                        drawPath(
                            path = plusPath,
                            color = Color.White.copy(alpha = topSpecularAlpha)
                        )
                    }
                }

                // 2. Feathered 2-pass dimensional drop shadow: drop-shadow(0 1.5px 1px ...)
                translate(left = 0f, top = 1.65f * scale) {
                    // Outer soft penumbra
                    drawPath(
                        path = plusPath,
                        color = shadowBaseColor.copy(alpha = shadowBaseColor.alpha * 0.45f),
                        style = shadowSpreadStroke
                    )
                    // Core umbra
                    drawPath(
                        path = plusPath,
                        color = shadowBaseColor
                    )
                }

                // 3. Main vertical bevel gradient fill (#FFFFFF -> #F5F5F7 -> #DCE1E8)
                drawPath(
                    path = plusPath,
                    brush = plusGradient
                )

                // 4. Crisp subtle white bevel rim stroke
                drawPath(
                    path = plusPath,
                    color = Color.White.copy(alpha = 0.88f),
                    style = rimStroke
                )
            }
        }
    )
}

/**
 * Custom [Modifier] that replicates authentic CSS `inset` box-shadows for machined Apple bevels.
 *
 * Uses `PathOperation.Difference` between the inner contour and a vertically shifted rounded rect
 * so the top specular highlight (`inset 0 1.5px`) and bottom shade (`inset 0 -2px`) naturally taper
 * along the rounded corners instead of looking like a flat uniform border.
 */
private fun Modifier.drawMachinedInsetBevel(
    cornerRadius: Dp,
    topHighlightColor: Color,
    topHighlightOffset: Dp,
    bottomShadeColor: Color,
    bottomShadeOffset: Dp,
    sideHighlightColor: Color,
    borderInset: Dp
): Modifier = this.drawWithCache {
    val insetPx = borderInset.toPx()
    if (size.width <= insetPx * 2f || size.height <= insetPx * 2f) {
        return@drawWithCache onDrawWithContent { drawContent() }
    }

    val innerRadiusPx = (cornerRadius.toPx() - insetPx).coerceAtLeast(1f)
    val topOffsetPx = topHighlightOffset.toPx()
    val bottomOffsetPx = bottomShadeOffset.toPx()

    val innerRect = Rect(
        left = insetPx,
        top = insetPx,
        right = size.width - insetPx,
        bottom = size.height - insetPx
    )
    val baseRoundRect = RoundRect(
        rect = innerRect,
        cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx)
    )

    val basePath = Path().apply { addRoundRect(baseRoundRect) }

    // Top crescent highlight path: Base minus Base shifted down by topOffsetPx
    val shiftedDownPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = innerRect.translate(translateX = 0f, translateY = topOffsetPx),
                cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx)
            )
        )
    }
    val topCrescentPath = Path().apply {
        op(basePath, shiftedDownPath, PathOperation.Difference)
    }

    // Soft secondary feather for top highlight
    val shiftedDownSoftPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = innerRect.translate(translateX = 0f, translateY = topOffsetPx * 1.65f),
                cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx)
            )
        )
    }
    val topFeatherPath = Path().apply {
        op(basePath, shiftedDownSoftPath, PathOperation.Difference)
    }

    // Bottom crescent shade path: Base minus Base shifted up by bottomOffsetPx
    val shiftedUpPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = innerRect.translate(translateX = 0f, translateY = -bottomOffsetPx),
                cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx)
            )
        )
    }
    val bottomCrescentPath = Path().apply {
        op(basePath, shiftedUpPath, PathOperation.Difference)
    }

    // Soft secondary feather for bottom shade
    val shiftedUpSoftPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = innerRect.translate(translateX = 0f, translateY = -bottomOffsetPx * 1.65f),
                cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx)
            )
        )
    }
    val bottomFeatherPath = Path().apply {
        op(basePath, shiftedUpSoftPath, PathOperation.Difference)
    }

    val sideStrokeWidth = 0.85.dp.toPx()

    onDrawWithContent {
        drawContent()

        clipPath(basePath) {
            // 1. Subtle left & right vertical rim highlight
            if (sideHighlightColor != Color.Transparent) {
                drawRoundRect(
                    color = sideHighlightColor,
                    topLeft = Offset(innerRect.left, innerRect.top),
                    size = Size(innerRect.width, innerRect.height),
                    cornerRadius = CornerRadius(innerRadiusPx, innerRadiusPx),
                    style = Stroke(width = sideStrokeWidth)
                )
            }

            // 2. Top machined specular crescent + soft feather
            drawPath(
                path = topFeatherPath,
                color = topHighlightColor.copy(alpha = topHighlightColor.alpha * 0.35f)
            )
            drawPath(
                path = topCrescentPath,
                color = topHighlightColor
            )

            // 3. Bottom machined bevel shade crescent + soft feather
            drawPath(
                path = bottomFeatherPath,
                color = bottomShadeColor.copy(alpha = bottomShadeColor.alpha * 0.4f)
            )
            drawPath(
                path = bottomCrescentPath,
                color = bottomShadeColor
            )
        }
    }
}

/**
 * Primary Preview matching the plain white background showcase.
 * Click the button in Android Studio's Interactive Preview mode to test the tactile press
 * and toggle between Apple System Blue (#007AFF) and Apple Black (#1D1D1F).
 */
@Preview(
    name = "1. Isolated Button (Plain White)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 220,
    heightDp = 220
)
@Composable
fun AppleBeveledTransitionButtonPreview() {
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        AppleBeveledTransitionButton(
            isActive = isActive,
            onClick = { isActive = !isActive }
        )
    }
}

/**
 * Side-by-side preview comparing Default Blue (52.dp), Active Black (52.dp),
 * and a Compact Timeline Seam size (36.dp) to verify proportional bevel scaling.
 */
@Preview(
    name = "2. States & Proportional Sizes",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 320,
    heightDp = 160
)
@Composable
fun AppleBeveledTransitionButtonStatesPreview() {
    var firstActive by remember { mutableStateOf(false) }
    var secondActive by remember { mutableStateOf(true) }
    var compactActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Standard 52.dp Default (Apple Blue)
            AppleBeveledTransitionButton(
                isActive = firstActive,
                onClick = { firstActive = !firstActive },
                buttonSize = 52.dp
            )

            // Standard 52.dp Active (Apple Black with 45° close "×" rotation option)
            AppleBeveledTransitionButton(
                isActive = secondActive,
                onClick = { secondActive = !secondActive },
                buttonSize = 52.dp,
                activeRotationDegrees = 45f
            )

            // Compact 36.dp Timeline Track Size
            AppleBeveledTransitionButton(
                isActive = compactActive,
                onClick = { compactActive = !compactActive },
                buttonSize = 36.dp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
