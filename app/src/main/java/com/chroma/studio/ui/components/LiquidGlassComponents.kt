package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Ellipsis

/**
 * Core Liquid Glass Design System Tokens
 * Reference: e:\liquid_glass_design_system.html & e:\apple_hig_3d_graphic_library.html
 */
object LiquidGlassTokens {
    val CanvasBg = Color(0xFFF4F7FB)
    val SurfaceCard = Color(0xFFFFFFFF)
    val BorderFlat = Color(0xFFDFE8F3)
    val AccentBlue = Color(0xFF007AFF)
    val AppleSystemBlue = Color(0xFF007AFF)
    val AppleSystemBlueActive = Color(0xFF0066D6)
    val AppleSystemBlueHover = Color(0xFF0071EB)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)

    val PrimaryBlueGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF4DA6FF),
            Color(0xFF0A84FF),
            Color(0xFF0051D5)
        )
    )

    // Exact Apple Precision Specular Glass Border (168deg multi-angle specular rim)
    val AppleSpecularGlassBorder = Brush.linearGradient(
        0.00f to Color(1f, 1f, 1f, 0.95f),
        0.24f to Color(1f, 1f, 1f, 0.52f),
        0.50f to Color(1f, 1f, 1f, 0.14f),
        0.76f to Color(0.745f, 0.902f, 1f, 0.32f),
        1.00f to Color(1f, 1f, 1f, 0.78f),
        start = Offset.Zero,
        end = Offset(120f, 480f)
    )

    val SurfaceSpecularGlassBorder = Brush.linearGradient(
        0.00f to Color(1f, 1f, 1f, 0.85f),
        0.24f to Color(1f, 1f, 1f, 0.45f),
        0.50f to Color(1f, 1f, 1f, 0.12f),
        0.76f to Color(1f, 1f, 1f, 0.28f),
        1.00f to Color(1f, 1f, 1f, 0.70f),
        start = Offset.Zero,
        end = Offset(120f, 480f)
    )

    val PrimarySpecularBorder = AppleSpecularGlassBorder

    val SecondaryWhiteGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8F9FA),
            Color(0xFFF1F3F5)
        )
    )

    val SecondarySpecularBorder = SolidColor(Color.Black)

    val CardCornerRadius = 22.dp
    val PillCornerRadius = 9999.dp
    val DialogCornerRadius = 24.dp
}

/**
 * Liquid Glass Card (ds-card)
 * Pure white card surface, crisp 1.dp border #DFE8F3, corner radius 22.dp, zero heavy shadow.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LiquidGlassTokens.SurfaceCard,
    borderColor: Color = LiquidGlassTokens.BorderFlat,
    cornerRadius: Dp = LiquidGlassTokens.CardCornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Liquid Glass Primary Button (btn-primary)
 * Apple System Blue (#007AFF) core + Thin and Elegant Specular Glass Hairline Border (0.5dp)
 * 12.dp Apple squircle shape with spring press physics (0.965f).
 */
@Composable
fun LiquidGlassPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit
) {
    val height = if (isSmall) 34.dp else 42.dp
    val horizontalPadding = if (isSmall) 14.dp else 20.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFF94A3B8)
            isPressed -> Color(0xFF0066D6)
            else -> Color(0xFF007AFF)
        },
        animationSpec = tween(140),
        label = "btnBgColor"
    )

    // Thin and elegant Apple specular glass border (0.5dp hairline)
    val thinAppleBorder = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(bgColor)
            .border(
                width = 0.5.dp,
                brush = if (enabled) thinAppleBorder else SolidColor(Color.White.copy(alpha = 0.12f)),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
fun LiquidGlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    shape: Shape = CircleShape,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    LiquidGlassPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSmall = isSmall,
        shape = shape
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = if (isSmall) 12.5.sp else 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).sp
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
    }
}

/**
 * Liquid Glass Secondary Button (btn-secondary)
 * Apple Frosted Translucent Surface + Thin and Elegant Specular Glass Hairline Border (0.5dp).
 */
@Composable
fun LiquidGlassSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit
) {
    val height = if (isSmall) 34.dp else 42.dp
    val horizontalPadding = if (isSmall) 14.dp else 20.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "secBtnScale"
    )

    val isDark = isSystemInDarkTheme()
    val secBg = when {
        !enabled -> if (isDark) Color(0x10FFFFFF) else Color(0xFFE2E8F0)
        isPressed -> if (isDark) Color(0x30FFFFFF) else Color(0xFFE5E5EA)
        else -> if (isDark) Color(0x1CFFFFFF) else Color(0xFFF2F2F7)
    }

    // Thin and elegant Apple secondary border (0.5dp hairline)
    val thinBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.08f))
        )
    }
    val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(secBg)
            .border(
                width = 0.5.dp,
                brush = thinBorderBrush,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    shape: Shape = CircleShape,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    LiquidGlassSecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSmall = isSmall,
        shape = shape
    ) {
        val contentColor = LocalContentColor.current
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = if (isSmall) 12.5.sp else 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).sp
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
    }
}

/**
 * Liquid Glass Pill Chip (.liquid-glass-chip)
 * Apple pill shape (9999.dp) with specular glass border.
 * When selected: pure Flat Apple System Blue (#007AFF) core + 1px Specular Glass Border + scale feedback.
 * When unselected: frosted subtle surface + 0.5px crisp glass rim.
 */
@Composable
fun LiquidGlassPillChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    isSmall: Boolean = false,
    activeColor: Color = LiquidGlassTokens.AppleSystemBlue
) {
    val shape = RoundedCornerShape(LiquidGlassTokens.PillCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(140, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "chipScale"
    )

    val isDark = isSystemInDarkTheme()
    val unselectedBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val unselectedBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else unselectedBg,
        animationSpec = tween(180),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)),
        animationSpec = tween(180),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier
                        .border(0.5.dp, Color(0x8C0051D5), shape)
                        .border(1.dp, LiquidGlassTokens.AppleSpecularGlassBorder, shape)
                } else {
                    Modifier
                        .border(0.5.dp, unselectedBorder, shape)
                        .border(0.5.dp, LiquidGlassTokens.SurfaceSpecularGlassBorder, shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (isSmall) 10.dp else 14.dp,
                vertical = if (isSmall) 4.5.dp else 6.5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(if (isSmall) 12.dp else 14.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = if (isSmall) 11.5.sp else 12.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = if (isSmall) 10.sp else 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    LiquidGlassSecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSmall = isSmall
    ) {
        val contentColor = LocalContentColor.current
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = if (isSmall) 12.5.sp else 14.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
    }
}

/**
 * Liquid Glass Underlined Pill Bar (ds-pill-bar & ds-pill-item)
 * Single unified container (#F4F7FB + 1.dp #DFE8F3), thin icons + sleek underlined active indicator.
 */
@Composable
fun LiquidGlassPillBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(LiquidGlassTokens.PillCornerRadius))
            .background(LiquidGlassTokens.CanvasBg)
            .border(1.dp, LiquidGlassTokens.BorderFlat, RoundedCornerShape(LiquidGlassTokens.PillCornerRadius))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

/**
 * Individual item inside LiquidGlassPillBar with animated bottom underline bar.
 */
@Composable
fun LiquidGlassPillItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        if (isSelected) LiquidGlassTokens.AccentBlue else LiquidGlassTokens.TextSecondary,
        label = "pillTextColor"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "pillIndicatorWidth"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(LiquidGlassTokens.PillCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title,
                color = textColor,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Sleek 2.dp bottom underline
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(indicatorWidth * 0.7f)
                .clip(RoundedCornerShape(9999.dp))
                .background(if (isSelected) LiquidGlassTokens.AccentBlue else Color.Transparent)
        )
    }
}

/**
 * Liquid Glass Progress Shaders & Palette Tokens
 * Direct 1:1 match to e:\Design Assets\liquid_glass_design_system.html (Spec 4 & 11)
 */
object LiquidGlassProgressGradients {
    // Primary Cupertino Blue Fill (Component 09 & 11)
    val PrimaryBlue = Brush.horizontalGradient(
        listOf(
            Color(0xFF4DA6FF),
            Color(0xFF0A84FF),
            Color(0xFF0051D5)
        )
    )
    // Poll 3D Shader 1 (Violet)
    val PollViolet = Brush.horizontalGradient(
        0.00f to Color(0xFFC4B5FD),
        0.45f to Color(0xFF8B5CF6),
        1.00f to Color(0xFF5B21B6)
    )
    // Poll 3D Shader 2 (Indigo)
    val PollIndigo = Brush.horizontalGradient(
        0.00f to Color(0xFFA78BFA),
        0.45f to Color(0xFF7C3AED),
        1.00f to Color(0xFF4C1D95)
    )
    // Poll 3D Shader 3 (Pink / Rose)
    val PollPink = Brush.horizontalGradient(
        0.00f to Color(0xFFF472B6),
        0.48f to Color(0xFFEC4899),
        1.00f to Color(0xFF9D174D)
    )
    // Success Winner Green
    val WinnerGreen = Brush.horizontalGradient(
        0.00f to Color(0xFF34C759),
        0.55f to Color(0xFF30D158),
        1.00f to Color(0xFF248A3D)
    )
    // Outgoing Message Translucent Glass Fill
    val OutgoingGlass = Brush.horizontalGradient(
        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.65f))
    )
}

/**
 * Thin Box & Extended Tick Checkbox (.ds-checkbox)
 * Exact mathematical match to e:\Design Assets\liquid_glass_design_system.html (Component 06A & Spec 8)
 *
 * SVG Geometry:
 * - ViewBox: 24 x 24
 * - Thin Box: d="M 19 8.5 V 17 A 4 4 0 0 1 15 21 H 7 A 4 4 0 0 1 3 17 V 9 A 4 4 0 0 1 7 5 H 15.5"
 *   Stroke 1.75dp, stroke-linecap: round, stroke-linejoin: round.
 *   Unchecked: #94A3B8 (or dark mode theme equivalent), full continuous box contour.
 *   Checked: #0A84FF (or custom checkedColor), smoothly animated dash opening at top-right corner.
 * - Sweeping Overhanging / Extended Tick: d="M 7.2 12.6 L 11.0 16.8 L 21.4 4.2"
 *   Stroke 1.85dp, stroke-linecap: round, stroke-linejoin: round, checkedColor (#0A84FF).
 *   Shoots through the open corner and extends beyond the boundary to (21.4, 4.2).
 */
@Composable
fun LiquidGlassCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 22.dp,
    checkedColor: Color = Color(0xFF0A84FF),
    uncheckedColor: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val resolvedUncheckedColor = uncheckedColor ?: (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))

    val tickProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "extendedTickProgress"
    )

    val boxColor by animateColorAsState(
        targetValue = if (checked) checkedColor else resolvedUncheckedColor,
        animationSpec = tween(250),
        label = "boxColor"
    )

    val clickableModifier = if (onCheckedChange != null && enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onCheckedChange(!checked) }
        )
    } else Modifier

    Canvas(
        modifier = modifier
            .size(size)
            .then(clickableModifier)
    ) {
        val s = this.size.minDimension
        val scale = s / 24f

        val strokeBoxW = 1.75f * scale
        val strokeTickW = 1.85f * scale

        // 1. Box Contour: M 19 8.5 V 17 A 4 4 0 0 1 15 21 H 7 A 4 4 0 0 1 3 17 V 9 A 4 4 0 0 1 7 5 H 15.5
        val boxPath = Path().apply {
            moveTo(19f * scale, 8.5f * scale)
            lineTo(19f * scale, 17f * scale)
            arcTo(
                rect = Rect(11f * scale, 13f * scale, 19f * scale, 21f * scale),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(7f * scale, 21f * scale)
            arcTo(
                rect = Rect(3f * scale, 13f * scale, 11f * scale, 21f * scale),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(3f * scale, 9f * scale)
            arcTo(
                rect = Rect(3f * scale, 5f * scale, 11f * scale, 13f * scale),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(15.5f * scale, 5f * scale)
        }

        if (tickProgress > 0.01f) {
            val boxMeasure = PathMeasure()
            boxMeasure.setPath(boxPath, false)
            val boxLen = boxMeasure.length
            val openBoxPath = Path()
            // Trim ends smoothly when checked to allow the extended tick to shoot through
            val startDist = 2.8f * scale * tickProgress
            val stopDist = (boxLen - (1.2f * scale * tickProgress)).coerceAtLeast(startDist)
            boxMeasure.getSegment(startDist, stopDist, openBoxPath, true)
            drawPath(
                path = openBoxPath,
                color = boxColor,
                style = Stroke(
                    width = strokeBoxW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            drawPath(
                path = boxPath,
                color = boxColor,
                style = Stroke(
                    width = strokeBoxW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 2. Sweeping Overhanging Tick: M 7.2 12.6 L 11.0 16.8 L 21.4 4.2
        if (tickProgress > 0.01f) {
            val tickPath = Path().apply {
                moveTo(7.2f * scale, 12.6f * scale)
                lineTo(11.0f * scale, 16.8f * scale)
                lineTo(21.4f * scale, 4.2f * scale)
            }
            val tickMeasure = PathMeasure()
            tickMeasure.setPath(tickPath, false)
            val animatedTick = Path()
            tickMeasure.getSegment(0f, tickMeasure.length * tickProgress, animatedTick, true)

            drawPath(
                path = animatedTick,
                color = checkedColor,
                alpha = tickProgress.coerceIn(0f, 1f),
                style = Stroke(
                    width = strokeTickW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Precision Concentric Liquid Glass Radio Button
 * Concentric outer circle with smooth spring-scale blue inner dot.
 */
@Composable
fun LiquidGlassRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "radioDotScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = (this.size.width / 2f) - 1.5.dp.toPx()

            // Outer ring
            drawCircle(
                color = if (selected) Color(0xFF0A84FF) else Color(0xFF94A3B8),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.75.dp.toPx())
            )

            // Inner blue dot
            if (dotScale > 0.01f) {
                drawCircle(
                    brush = LiquidGlassTokens.PrimaryBlueGradient,
                    radius = (r * 0.55f) * dotScale,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Liquid Glass Input Field (ds-input)
 * Pristine #F4F7FB background, #DFE8F3 border, focus white & accent blue border.
 */
@Composable
fun LiquidGlassInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPill: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = if (isPill) RoundedCornerShape(9999.dp) else RoundedCornerShape(14.dp)
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isFocused) Color.White else LiquidGlassTokens.CanvasBg)
            .border(
                1.dp,
                if (isFocused) LiquidGlassTokens.AccentBlue else LiquidGlassTokens.BorderFlat,
                shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) LiquidGlassTokens.AccentBlue else LiquidGlassTokens.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = LiquidGlassTokens.TextPrimary,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(LiquidGlassTokens.AccentBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }

        trailingIcon?.invoke()
    }
}

/**
 * Three Dots Trigger & Panel (Liquid Glass Dots Menu)
 */
@Composable
fun LiquidGlassThreeDotsMenu(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFF4F7FB))
            .border(1.dp, Color(0xFFDFE8F3), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Lucide.Ellipsis,
            contentDescription = "Options",
            tint = Color(0xFF0A84FF),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Liquid Glass Recessed Progress Meter (.ds-progress-track & .ds-progress-fill)
 * Strict Spec from e:\Design Assets\liquid_glass_design_system.html (Component 09 & 11):
 * - Track: Recessed pill, height 6dp, RoundedCornerShape(9999.dp), #E5E5EA (light) / #2C2C2E (dark),
 *   with subtle inner inset shadow (box-shadow: inset 0 1.5px 3px rgba(0, 0, 0, 0.12)).
 * - Fill: RoundedCornerShape(9999.dp), horizontal gradient [#4DA6FF -> #0A84FF -> #0051D5],
 *   smooth animated fraction (transition: width 0.35s cubic-bezier(0.25, 1, 0.5, 1)).
 * - Supports determinate (0f..1f) and indeterminate (null or < 0f) animated shimmer.
 */
@Composable
fun LiquidGlassProgressBar(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color? = null,
    fillBrush: Brush? = null,
    animated: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val resolvedTrack = trackColor ?: (if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
    val resolvedFill = fillBrush ?: LiquidGlassProgressGradients.PrimaryBlue

    val shape = RoundedCornerShape(9999.dp)

    if (progress == null || progress < 0f) {
        // Indeterminate liquid shimmer pulse
        val infiniteTransition = rememberInfiniteTransition(label = "liquidProgressPulse")
        val offsetFraction by infiniteTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseOffset"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(resolvedTrack)
                .drawWithContent {
                    drawContent()
                    // Exact recessed inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to (if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.12f)),
                            0.45f to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    )
                }
                .border(
                    0.5.dp,
                    if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
                    shape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .graphicsLayer {
                        translationX = size.width * offsetFraction
                    }
                    .clip(shape)
                    .background(resolvedFill)
            )
        }
    } else {
        val target = progress.coerceIn(0f, 1f)
        val animatedValue by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 350f),
            label = "liquidProgressFill"
        )
        val currentFill = if (animated) animatedValue else target

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(resolvedTrack)
                .drawWithContent {
                    drawContent()
                    // Exact recessed inner shadow
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to (if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.12f)),
                            0.45f to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    )
                }
                .border(
                    0.5.dp,
                    if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
                    shape
                )
        ) {
            if (currentFill > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentFill)
                        .clip(shape)
                        .background(resolvedFill)
                )
            }
        }
    }
}

/**
 * Liquid Glass Vote Poll Option Meter
 * Dedicated poll progress meter implementing the exact poll 3D shaders and recessed styling
 * from e:\Design Assets\liquid_glass_design_system.html (Linear 3D Media/Poll & Spec 11).
 */
@Composable
fun LiquidGlassPollOptionMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    optionIndex: Int = 0,
    isWinner: Boolean = false,
    isLeading: Boolean = false,
    isOutgoing: Boolean = false,
    height: Dp = 6.dp
) {
    val fillBrush = when {
        isWinner -> LiquidGlassProgressGradients.WinnerGreen
        isOutgoing -> LiquidGlassProgressGradients.OutgoingGlass
        isLeading -> LiquidGlassProgressGradients.PrimaryBlue
        else -> when (optionIndex % 4) {
            0 -> LiquidGlassProgressGradients.PrimaryBlue
            1 -> LiquidGlassProgressGradients.PollViolet
            2 -> LiquidGlassProgressGradients.PollIndigo
            else -> LiquidGlassProgressGradients.PollPink
        }
    }

    val trackColor = if (isOutgoing) {
        Color.White.copy(alpha = 0.22f)
    } else null

    LiquidGlassProgressBar(
        progress = progress,
        modifier = modifier,
        height = height,
        trackColor = trackColor,
        fillBrush = fillBrush,
        animated = true
    )
}

/**
 * Liquid Glass Toggle Switch (Component 02)
 * Direct alias to AppleSwitch to guarantee the exact same jelly knob and track everywhere.
 */
@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF0A84FF),
    inactiveTrackColor: Color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF39393D) else Color(0xFFE5E5EA)
) {
    AppleSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor
    )
}

/**
 * Liquid Glass Bottom Sheet Drawer
 * Real Google Material 3 ModalBottomSheet with native gestures and velocity physics,
 * styled with Apple/Liquid Glass rounded top corners (24.dp), tactile drag pill, and clean surfaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
    contentColor: Color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A),
    dragHandle: @Composable (() -> Unit)? = {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 8.dp)
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f))
        )
    },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        dragHandle = dragHandle,
        content = content
    )
}


