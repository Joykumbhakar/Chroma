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
    val AccentBlue = Color(0xFF0A84FF)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)

    val PrimaryBlueGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF4DA6FF),
            Color(0xFF0A84FF),
            Color(0xFF0051D5)
        )
    )

    val PrimarySpecularBorder = Brush.linearGradient(
        0.00f to Color(0xEBFFFFFF),
        0.50f to Color(0xA67CC4FF),
        1.00f to Color(0xD9E0F2FE),
        start = Offset.Zero,
        end = Offset.Infinite
    )

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
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
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
 * Full 9999.dp Pill, Blue gradient (#4DA6FF -> #0A84FF -> #0051D5), specular glass border sheen.
 */
@Composable
fun LiquidGlassPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val height = if (isSmall) 32.dp else 42.dp
    val horizontalPadding = if (isSmall) 14.dp else 22.dp
    val shape = RoundedCornerShape(LiquidGlassTokens.PillCornerRadius)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(if (enabled) LiquidGlassTokens.PrimaryBlueGradient else Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B))))
            .border(1.5.dp, if (enabled) LiquidGlassTokens.PrimarySpecularBorder else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)), shape)
            .clickable(enabled = enabled, onClick = onClick)
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
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    LiquidGlassPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSmall = isSmall
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
            fontWeight = FontWeight.SemiBold
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
 * Full 9999.dp Pill, Pure white gradient surface, thin solid black border, text & icon Color.Black.
 */
@Composable
fun LiquidGlassSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val height = if (isSmall) 32.dp else 42.dp
    val horizontalPadding = if (isSmall) 14.dp else 22.dp
    val shape = RoundedCornerShape(LiquidGlassTokens.PillCornerRadius)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(if (enabled) LiquidGlassTokens.SecondaryWhiteGradient else Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))))
            .border(1.dp, if (enabled) LiquidGlassTokens.SecondarySpecularBorder else Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.Black
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
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    LiquidGlassSecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isSmall = isSmall
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(if (isSmall) 14.dp else 16.dp)
            )
        }
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (isSmall) 12.5.sp else 14.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Color.Black,
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
 * Liquid Glass Checkbox (Thin box with overhanging tick mark)
 * Strict Spec: Thin 1.75dp stroke box, animated 1.85dp tick mark in Accent Blue.
 */
@Composable
fun LiquidGlassCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "checkboxTick"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.width
            val strokeW = 1.75.dp.toPx()
            val cornerR = 5.dp.toPx()

            // Box outline
            drawRoundRect(
                color = if (checked) Color(0xFF0A84FF) else Color(0xFF94A3B8),
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(s - strokeW, s - strokeW),
                cornerRadius = CornerRadius(cornerR, cornerR),
                style = Stroke(width = strokeW)
            )

            // Overhanging Tick Mark
            if (checkProgress > 0.05f) {
                val tickPath = Path().apply {
                    val p1 = Offset(s * 0.25f, s * 0.52f)
                    val p2 = Offset(s * 0.44f, s * 0.72f)
                    val p3 = Offset(s * 0.82f, s * 0.30f)

                    moveTo(p1.x, p1.y)
                    if (checkProgress < 0.45f) {
                        val seg1 = checkProgress / 0.45f
                        lineTo(p1.x + (p2.x - p1.x) * seg1, p1.y + (p2.y - p1.y) * seg1)
                    } else {
                        lineTo(p2.x, p2.y)
                        val seg2 = ((checkProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        lineTo(p2.x + (p3.x - p2.x) * seg2, p2.y + (p3.y - p2.y) * seg2)
                    }
                }
                drawPath(
                    path = tickPath,
                    color = Color(0xFF0A84FF),
                    style = Stroke(
                        width = 2.0.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
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
 * Strict Spec from e:\liquid_glass_design_system.html (Component 09 & 11):
 * - Track: Recessed pill, height 6dp-8dp, RoundedCornerShape(9999.dp), #E5E5EA (light) / #2C2C2E (dark),
 *   with subtle inset border / shadow.
 * - Fill: RoundedCornerShape(9999.dp), horizontal gradient [#4DA6FF -> #0A84FF -> #0051D5],
 *   smooth animated fraction.
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
    val resolvedFill = fillBrush ?: Brush.horizontalGradient(
        listOf(
            Color(0xFF4DA6FF),
            Color(0xFF0A84FF),
            Color(0xFF0051D5)
        )
    )

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
                .clip(RoundedCornerShape(9999.dp))
                .background(resolvedTrack)
                .border(
                    0.5.dp,
                    if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
                    RoundedCornerShape(9999.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .graphicsLayer {
                        translationX = size.width * offsetFraction
                    }
                    .clip(RoundedCornerShape(9999.dp))
                    .background(resolvedFill)
            )
        }
    } else {
        val target = progress.coerceIn(0f, 1f)
        val animatedValue by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "liquidProgressFill"
        )
        val currentFill = if (animated) animatedValue else target

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(9999.dp))
                .background(resolvedTrack)
                .border(
                    0.5.dp,
                    if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
                    RoundedCornerShape(9999.dp)
                )
        ) {
            if (currentFill > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentFill)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(resolvedFill)
                )
            }
        }
    }
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
    inactiveTrackColor: Color = Color(0xFFE5E5EA)
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


