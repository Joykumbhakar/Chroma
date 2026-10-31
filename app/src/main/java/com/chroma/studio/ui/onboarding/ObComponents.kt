package com.chroma.studio.ui.onboarding

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ObSegmentedProgress(step: Int, totalSteps: Int = 3, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    .width(42.dp)
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
    totalSteps: Int = 3,
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
        progress <= 0.35f -> 1
        progress <= 0.70f -> 2
        else -> 3
    }
    ObNavBar(onBack = onBack, step = step, totalSteps = 3, trailing = trailing)
}

@Composable
fun ObBadge(text: String, color: Color = ObColors.Lime, modifier: Modifier = Modifier) {
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
fun ObHeader(
    firstLine: String,
    highlightLine: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 22.dp)) {
        Text(
            buildAnnotatedString {
                append(firstLine)
                append("\n")
                withStyle(SpanStyle(color = ObColors.Lime)) {
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
        Column(modifier.padding(horizontal = 22.dp)) {
            Text(title, style = ObType.Title)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = ObType.Body)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Frosted glass background layer with 24.dp blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x55FFFFFF),
                            Color(0x221E2548)
                        )
                    )
                )
        )
        // Tint overlay and fine glass border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x40141A38),
                            Color(0x600C1028)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66FFFFFF),
                                Color(0x18FFFFFF)
                            )
                        )
                    ),
                    shape
                )
        )
        // Foreground crisp content
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = ObColors.Lime,
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
    val bg by animateColorAsState(if (selected) Color(0x28FFE500) else Color(0x8C141938), tween(180), label = "chipBg")
    val fg by animateColorAsState(Color.White, tween(180), label = "chipFg")
    val iconTint by animateColorAsState(if (selected) Color(0xFFFDE047) else Color(0xCCB0C2F0), tween(180), label = "chipIcon")
    val border by animateColorAsState(if (selected) Color(0xFFFDE047) else Color(0x18FFFFFF), tween(180), label = "chipBorder")
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, spring(0.45f, 500f), label = "chipScale")
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(50.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
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

@Composable
fun ObTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    leading: ImageVector? = null, leadingTint: Color = ObColors.Sky, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null, trailing: (@Composable RowScope.() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val border by animateColorAsState(
        when { isError -> ObColors.Danger; focused -> ObColors.Lime; else -> ObColors.FieldBorder }, tween(150), label = "fieldBorder"
    )
    val textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(ObColors.Field).border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leading?.let { Icon(it, null, tint = leadingTint, modifier = Modifier.size(18.dp)) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, color = ObColors.Placeholder, fontSize = 15.sp)
            if (onClick != null) {
                Text(value, style = textStyle)
            } else {
                BasicTextField(
                    value, onValueChange, fieldModifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    singleLine = true, textStyle = textStyle, cursorBrush = SolidColor(ObColors.Lime),
                    visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions
                )
            }
        }
        trailing?.invoke(this)
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
            .padding(vertical = 6.dp),
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
