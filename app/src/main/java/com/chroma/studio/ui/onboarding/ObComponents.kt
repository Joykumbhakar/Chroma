package com.chroma.studio.ui.onboarding

import androidx.compose.ui.res.painterResource
import com.chroma.studio.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun ObScreen(background: Brush = ObColors.FlowBackground, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(), content = content)
    }
}

@Composable
fun ObGlassCard(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(16.dp), content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.clip(shape)
            .background(Brush.linearGradient(listOf(ObColors.GlassTop, ObColors.GlassBottom)))
            .border(1.dp, ObColors.GlassBorder, shape),
        content = content
    )
}

@Composable
fun ObIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x1FFFFFFF))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun ObProgressRail(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350, easing = FastOutSlowInEasing), label = "rail")
    Box(
        modifier = modifier
            .width(130.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun ObSegmentedProgress(step: Int, totalSteps: Int = 4, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val active = i <= step
            val animColor by animateColorAsState(
                targetValue = if (active) Color.White else Color(0x35FFFFFF),
                animationSpec = tween(300),
                label = "segColor_$i"
            )
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(animColor)
            )
        }
    }
}

@Composable
fun ObNavBar(
    onBack: () -> Unit,
    step: Int,
    totalSteps: Int = 4,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ObIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        ObSegmentedProgress(step = step, totalSteps = totalSteps)
        Box(Modifier.widthIn(min = 38.dp), contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

@Composable
fun ObNavBar(onBack: () -> Unit, progress: Float, trailing: @Composable () -> Unit = {}) {
    val step = when {
        progress <= 0.25f -> 1
        progress <= 0.50f -> 2
        progress <= 0.75f -> 3
        else -> 4
    }
    ObNavBar(onBack = onBack, step = step, totalSteps = 4, trailing = trailing)
}

/** Infinite-transition Neon Green wave animated gradient brush (#00FF87 -> #60EFA0 -> #A3FF2E -> #00DF81) */
@Composable
fun rememberNeonGreenWaveBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "neonGreenWaveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neonGreenWavePhase"
    )
    val colors = ObColors.NeonGreenWaveColors
    return remember(phase) {
        val spread = 720f
        val offset = phase * spread
        Brush.linearGradient(
            colors = colors,
            start = Offset(offset, 0f),
            end = Offset(offset + spread, spread * 0.45f),
            tileMode = TileMode.Repeated
        )
    }
}

/** Composable for headline texts with Neon Green wave animated gradient */
@Composable
fun NeonWaveText(
    text: String,
    style: TextStyle = ObType.Title,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val waveBrush = rememberNeonGreenWaveBrush()
    Text(
        text = text,
        style = style.copy(brush = waveBrush),
        modifier = modifier,
        textAlign = textAlign,
        fontFamily = fontFamily,
        fontSize = fontSize
    )
}

@Composable
fun ObBadge(text: String, color: Color = ObColors.NeonGreen, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.5.dp)
    )
}

@Composable
fun ObHeader(
    firstLine: String,
    highlightLine: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val waveBrush = rememberNeonGreenWaveBrush()
    Column(modifier.padding(horizontal = 22.dp)) {
        Text(
            buildAnnotatedString {
                append(firstLine)
                append("\n")
                withStyle(SpanStyle(brush = waveBrush)) {
                    append(highlightLine)
                }
            },
            style = ObType.Title
        )
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = ObType.Body)
    }
}

@Composable
fun ObHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val lines = title.split("\n", limit = 2)
    if (lines.size == 2) {
        ObHeader(firstLine = lines[0], highlightLine = lines[1], subtitle = subtitle, modifier = modifier)
    } else {
        val waveBrush = rememberNeonGreenWaveBrush()
        Column(modifier.padding(horizontal = 22.dp)) {
            Text(title, style = ObType.Title.copy(brush = waveBrush))
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = ObType.Body)
        }
    }
}

/** Enhanced Card Window Pop-up with liquid glass styling, specular border, and spring animation */
@Composable
fun ObPopupCardWindow(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.88f
                ) + fadeIn(tween(260)),
                exit = scaleOut(tween(180), targetScale = 0.92f) + fadeOut(tween(150))
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xF20F1638),
                                    Color(0xFA090D24)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x9900FF87),
                                        Color(0x4060EFA0),
                                        Color(0x22FFFFFF),
                                        Color(0x0FFFFFFF)
                                    )
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .shadow(28.dp, RoundedCornerShape(26.dp), spotColor = Color(0x6000FF87))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                }
            }
        }
    }
}

/** White pill. Also used as the Google button via [leading]. */
@Composable
fun ObPrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null, trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowForward, height: Dp = 52.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(0.5f, 400f), label = "press")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "alpha")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .shadow(if (enabled) 12.dp else 0.dp, CircleShape, ambientColor = Color(0x60000000), spotColor = Color(0x80000000))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF090C22), strokeWidth = 2.dp)
        } else {
            leading?.let { it(); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color(0xFF090C22), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            trailingIcon?.let { Spacer(Modifier.width(6.dp)); Icon(it, null, tint = Color(0xFF090C22), modifier = Modifier.size(18.dp)) }
        }
    }
}


/**
 * Official Apple-style pill button following Apple Human Interface Guidelines:
 * Midnight pitch-black container, pure white Apple logo, bold white typography.
 */
@Composable
fun ObAppleButton(
    text: String = "Sign in with Apple",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 52.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(0.5f, 400f), label = "apple_press")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "apple_alpha")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .shadow(if (enabled) 10.dp else 0.dp, CircleShape, ambientColor = Color(0x60000000), spotColor = Color(0x90000000))
            .clip(CircleShape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_apple_logo),
                contentDescription = "Apple logo",
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ObSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press"
    )
    val shape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x2EFFFFFF),
                        Color(0x12FFFFFF)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0x55FFFFFF),
                            Color(0x18FFFFFF)
                        )
                    )
                ),
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = ObColors.NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun ObTopicChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(if (selected) Color(0x2800FF87) else Color(0x8C141938), tween(180), label = "chipBg")
    val fg by animateColorAsState(Color.White, tween(180), label = "chipFg")
    val iconTint by animateColorAsState(if (selected) ObColors.NeonGreen else Color(0xCCB0C2F0), tween(180), label = "chipIcon")
    val border by animateColorAsState(if (selected) ObColors.NeonGreen else Color(0x22FFFFFF), tween(180), label = "chipBorder")
    val scale by animateFloatAsState(if (selected) 1.025f else 1f, spring(0.45f, 500f), label = "chipScale")
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(52.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.8.dp else 1.dp, border, shape)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
        Text(label, color = fg, fontSize = 13.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** Animated check circle. Fills lime with a springy tick when [met] flips to true. */
@Composable
fun ObCheck(met: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val bg by animateColorAsState(if (met) ObColors.Lime else Color.Transparent, tween(200), label = "ckBg")
    val stroke by animateColorAsState(if (met) ObColors.Lime else Color(0x668AB4FF), tween(200), label = "ckStroke")
    Box(modifier.size(size).clip(CircleShape).background(bg).border(1.5.dp, stroke, CircleShape), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = met,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) { Icon(Icons.Rounded.Check, null, tint = ObColors.OnLime, modifier = Modifier.size(size * 0.7f)) }
    }
}

/**
 * Exact Uiverse.io input bar by alexruix:
 * Floating label that scales and translates to top border on focus/valid,
 * transparent background (background: none), 16.dp border-radius (1rem),
 * 1.5.dp border (#9e9e9e idle -> #1a73e8 active), #2196f3 active label,
 * with 150ms cubic-bezier(0.4, 0, 0.2, 1) transition.
 */
@Composable
fun ObTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    leading: ImageVector? = null,
    leadingTint: Color? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null,
    labelBackground: Color = Color(0xFF0E143A),
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isActive = isFocused || value.isNotEmpty()

    val easeCubic = remember { CubicBezierEasing(0.4f, 0f, 0.2f, 1f) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> ObColors.Danger
            isActive -> Color(0xFF1A73E8)
            else -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(150, easing = easeCubic),
        label = "inputBorder"
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> ObColors.Danger
            isActive -> Color(0xFF2196F3)
            else -> Color(0xFFE8E8E8)
        },
        animationSpec = tween(150, easing = easeCubic),
        label = "labelColor"
    )

    val labelOffsetY by animateDpAsState(
        targetValue = if (isActive) (-9).dp else 17.dp,
        animationSpec = tween(150, easing = easeCubic),
        label = "labelOffsetY"
    )

    val labelScale by animateFloatAsState(
        targetValue = if (isActive) 0.8f else 1.0f,
        animationSpec = tween(150, easing = easeCubic),
        label = "labelScale"
    )

    val shape = RoundedCornerShape(16.dp) // 1rem in CSS

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
    ) {
        // .input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Transparent, shape)
                .border(1.5.dp, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leading?.let {
                val iconTint = leadingTint ?: (if (isActive) Color(0xFF2196F3) else Color(0xFF9E9E9E))
                Icon(it, null, tint = iconTint, modifier = Modifier.size(19.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (onClick != null) {
                    Text(
                        text = value,
                        color = Color(0xFFF5F5F5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = fieldModifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFFF5F5F5),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFF2196F3)),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                }
            }

            trailing?.invoke(this)
        }

        // .user-label
        Box(
            modifier = Modifier
                .padding(start = if (leading != null && !isActive) 44.dp else 15.dp)
                .offset(y = labelOffsetY)
                .graphicsLayer {
                    scaleX = labelScale
                    scaleY = labelScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                .then(
                    if (isActive) {
                        Modifier
                            .background(labelBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    } else Modifier
                )
        ) {
            Text(
                text = placeholder,
                color = labelColor,
                fontSize = 16.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/** Liquid Glass Jelly toggle matching reference HTML/CSS specification. */
@Composable
fun ObJellyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF34C759)
) {
    com.chroma.studio.ui.components.AppleSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { contentDescription = label },
        activeTrackColor = activeTrackColor
    )
}

@Composable
fun ObToggleRow(
    icon: ImageVector,
    tint: Color = Color.White,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = ObType.Caption.copy(color = Color(0xB3FFFFFF), fontSize = 12.sp))
        }
        Spacer(Modifier.width(12.dp))
        ObJellyToggle(checked, onCheckedChange, title)
    }
}
