package com.chroma.studio.chat.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.ui.window.*
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import com.chroma.studio.ui.theme.InterFontFamily
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeState

@Composable
fun LiquidGlassKnob(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    scale: Float,
    baseAlpha: Float,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    isToggleKnob: Boolean = false
) {
    val knobShape = RoundedCornerShape(50.dp)
    Box(
        modifier = modifier
            .size(width, height)
            .scale(scale)
            .shadow(
                elevation = if (isToggleKnob) 1.5.dp else 2.dp,
                shape = knobShape,
                spotColor = Color.Black.copy(alpha = if (isToggleKnob) 0.12f else 0.20f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(knobShape)
            .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = knobShape) else Modifier)
            .background(
                if (isToggleKnob) {
                    Color.White.copy(alpha = baseAlpha.coerceIn(0.10f, 0.95f))
                } else {
                    Color.White.copy(alpha = baseAlpha.coerceAtLeast(0.90f))
                }
            )
            .border(
                0.5.dp,
                if (isToggleKnob) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f),
                knobShape
            )
    ) {
        // Inset specular glow layer: inset 0 1px 2px rgba(255, 255, 255, 0.8)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.95f),
                        0.4f to Color.White.copy(alpha = 0.40f),
                        1.0f to Color.Black.copy(alpha = 0.10f)
                    ),
                    shape = knobShape
                )
        )

        // Top curved glossy reflection: height 48%, border-radius: 12px 12px 28px 28px / 10px 10px 6px 6px
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 2.dp, end = 2.dp, top = 1.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.95f),
                        1.0f to Color.White.copy(alpha = 0.10f)
                    )
                )
        )

        // Bottom ambient light rim: height 35%, border-radius: 28px 28px 12px 12px / 6px 6px 10px 10px
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 2.dp, end = 2.dp, bottom = 1.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to Color.White.copy(alpha = 0.85f)
                    )
                )
        )
    }
}

@Composable
fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF34C759),
    inactiveTrackColor: Color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF39393D) else Color(0xFFE5E5EA),
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val trackColor by animateColorAsState(
        if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "trackColor"
    )

    // Exact CSS dimensions & translations:
    // .toggle-wrapper: 44px x 22px
    // .toggle-knob: top 2px, left 2px, width 24px (28px on active), height 18px
    // unchecked: translateX(0)
    // unchecked + active: translateX(-3px), scale(1.26), width 28px
    // checked: translateX(16px) -> knob starts at 2px + 16px = 18px
    // checked + active: translateX(15px), scale(1.26), width 28px
    val targetTranslation = if (!checked && isPressed) -1f
                            else if (!checked) 2f
                            else if (checked && isPressed) 15f
                            else 18f

    val targetWidth = if (isPressed) 28.dp else 24.dp
    val targetScale = if (isPressed) 1.26f else 1.0f

    val jellySpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.42f, stiffness = 550f)
    val jellySpringDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.42f, stiffness = 550f)

    val thumbTranslationX by animateFloatAsState(targetTranslation, animationSpec = jellySpring, label = "tx")
    val thumbWidth by animateDpAsState(targetWidth, animationSpec = jellySpringDp, label = "tw")
    val thumbScale by animateFloatAsState(targetScale, animationSpec = jellySpring, label = "ts")

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .width(44.dp)
            .height(22.dp)
            .pointerInput(checked) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onCheckedChange(!checked)
                    }
                )
            }
    ) {
        // Track: 44x22 rounded pill with inset shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(100.dp))
                .background(trackColor)
                .border(
                    0.5.dp,
                    if (checked) Color(0x33000000) else Color(0x14000000),
                    RoundedCornerShape(100.dp)
                )
        ) {
            // Inset top shadow: inset 0 2px 5px rgba(0, 0, 0, 0.15)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = if (checked) 0.18f else 0.12f), Color.Transparent)
                        )
                    )
            )
        }

        // Knob: 24x18 (28x18 active), top: 2px, left offset: thumbTranslationX
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 2.dp, bottom = 2.dp)
                .offset(x = thumbTranslationX.dp)
                .size(width = thumbWidth, height = 18.dp)
        ) {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 18.dp,
                scale = thumbScale,
                baseAlpha = 0.90f,
                isToggleKnob = true,
                hazeState = hazeState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    gradientColors: List<Color>? = null,
    trackHeight: Dp = 6.dp
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isInteracting) {
        if (isInteracting) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    // Exact CSS dimensions: 24px normal, 28px active, scale 1.15
    val thumbWidth by animateDpAsState(
        targetValue = if (isInteracting) 28.dp else 24.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "tw"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.15f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "ts"
    )

    val isDark = isSystemInDarkTheme()
    val inactiveTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    val activeGrad = gradientColors ?: listOf(Color(0xFF0A84FF), Color(0xFF0071E3))

    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        colors = SliderDefaults.colors(
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            thumbColor = Color.Transparent
        ),
        track = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
            } else 0f

            val trackShape = RoundedCornerShape(100.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(trackShape)
                    .background(inactiveTrackColor)
                    .border(0.5.dp, Color.Black.copy(alpha = if (isDark) 0.15f else 0.05f), trackShape)
            ) {
                // Inset track shadow: inset 0 2px 5px rgba(0, 0, 0, 0.15)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                if (fraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(trackShape)
                            .background(Brush.horizontalGradient(activeGrad))
                    )
                }
            }
        },
        thumb = {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 18.dp,
                scale = thumbScale,
                baseAlpha = 0.90f,
                isToggleKnob = false,
                hazeState = hazeState
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleJellySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = Color(0xFF007AFF),
    inactiveColor: Color = Color(0xFFE5E5EA),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isActive = isPressed || isDragged

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isActive) 1.25f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.4f, // bouncy jelly effect
            stiffness = 500f
        ),
        label = "sliderThumbScale"
    )

    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        interactionSource = interactionSource,
        modifier = modifier,
        colors = androidx.compose.material3.SliderDefaults.colors(
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            thumbColor = Color.Transparent
        ),
        track = { sliderState ->
            val fraction = (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(inactiveColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                        .background(activeColor)
                )
            }
        },
        thumb = {
            val thumbWidth = 24.dp
            val thumbHeight = 20.dp
            
            Box(
                modifier = Modifier
                    .size(width = thumbWidth, height = thumbHeight)
                    .scale(scale)
                    .shadow(
                        elevation = 3.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .border(
                        width = 0.5.dp, 
                        color = Color.Black.copy(alpha = 0.15f), 
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                    )
            )
        }
    )
}

@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppleSwitch(
        checked = checked,
        onCheckedChange = { onCheckedChange?.invoke(it) },
        modifier = modifier
    )
}

@Composable
fun LiquidJellySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    AppleJellySlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier
    )
}

@Composable
fun AppleDialog(
    title: String,
    message: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    primaryIsDestructive: Boolean = false,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    layout: String = "horizontal",
    onDismiss: () -> Unit,
    customContent: @Composable (() -> Unit)? = null
) {
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    
    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 1.1f,
        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "dialogAlpha"
    )

    val dialogBg = if (isSystemInDarkTheme) Color(0xEB2C2C2E) else Color(0xEBFFFFFF)
    val titleColor = if (isSystemInDarkTheme) Color.White else Color.Black
    val dividerColor = if (isSystemInDarkTheme) Color(0x40545458) else Color(0x203C3C43)
    val blueColor = if (isSystemInDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val redColor = if (isSystemInDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * alpha))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogBg)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(top = 18.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.4).sp,
                        lineHeight = 22.sp
                    )
                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message,
                            color = titleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                    if (customContent != null) {
                        Spacer(Modifier.height(12.dp))
                        customContent()
                    }
                }

                androidx.compose.material3.HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                if (layout == "horizontal" && secondaryButtonText != null) {
                    Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSecondaryClick?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = secondaryButtonText,
                                color = blueColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        }
                        androidx.compose.material3.VerticalDivider(color = dividerColor, thickness = 0.5.dp)
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onPrimaryClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = primaryButtonText,
                                color = if (primaryIsDestructive) redColor else blueColor,
                                fontSize = 17.sp,
                                fontWeight = if (primaryIsDestructive) FontWeight.Normal else FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(44.dp).clickable { onPrimaryClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = primaryButtonText,
                                color = if (primaryIsDestructive) redColor else blueColor,
                                fontSize = 17.sp,
                                fontWeight = if (primaryIsDestructive) FontWeight.Normal else FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                        if (secondaryButtonText != null) {
                            androidx.compose.material3.HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier.fillMaxWidth().height(44.dp).clickable { onSecondaryClick?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = secondaryButtonText,
                                    color = blueColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JasicaGraphicalDialogPanel(hazeState: dev.chrisbanes.haze.HazeState? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        val baseModifier = Modifier
            .fillMaxWidth()
            .then(if (hazeState != null) Modifier.clip(RoundedCornerShape(24.dp)).hazeChild(state = hazeState, shape = RoundedCornerShape(24.dp)) else Modifier)
            .background(if (hazeState != null) Color.Transparent else Color(0xE61C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        Box(modifier = baseModifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}
