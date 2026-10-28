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
    val knobShape = RoundedCornerShape(100.dp)
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(width, height)
            .scale(scale)
            .clip(knobShape)
            .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = knobShape) else Modifier)
            .background(Color.White)
            .border(
                0.5.dp,
                if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.08f),
                knobShape
            )
    ) {
        // Pristine Apple hairline top specular highlight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 2.dp, vertical = 0.5.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.2f)
                        )
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
    val isDark = isSystemInDarkTheme()
    val trackColor by animateColorAsState(
        if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "trackColor"
    )

    // Authentic iOS UISwitch proportions:
    // Track: 46dp width x 26dp height (pill shape)
    // Knob: 22dp height x 22dp width (when pressed stretches to 26dp with fluid Apple spring)
    // Inactive: X = 2dp
    // Active (checked): X = 22dp (46 - 22 - 2)
    val targetTranslation = when {
        !checked && isPressed -> 2f
        !checked -> 2f
        checked && isPressed -> 18f
        else -> 22f
    }

    val targetWidth = if (isPressed) 26.dp else 22.dp

    val jellySpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.65f, stiffness = 600f)
    val jellySpringDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.65f, stiffness = 600f)

    val thumbTranslationX by animateFloatAsState(targetTranslation, animationSpec = jellySpring, label = "tx")
    val thumbWidth by animateDpAsState(targetWidth, animationSpec = jellySpringDp, label = "tw")

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .width(46.dp)
            .height(26.dp)
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
        // Track: Clean Apple flat pill, zero shadows
        val trackShape = RoundedCornerShape(100.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(trackShape)
                .background(trackColor)
                .border(
                    0.5.dp,
                    if (checked) Color(0x1F000000) else if (isDark) Color(0x33FFFFFF) else Color(0x18000000),
                    trackShape
                )
        )

        // Knob: Clean white Apple knob, zero shadows
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(vertical = 2.dp)
                .offset(x = thumbTranslationX.dp)
                .size(width = thumbWidth, height = 22.dp)
        ) {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 22.dp,
                scale = 1.0f,
                baseAlpha = 1.0f,
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
    trackHeight: Dp = 4.dp
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

    // Authentic iOS UISlider: 24dp circular white thumb, smooth slight scale on drag
    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "ts"
    )

    val isDark = isSystemInDarkTheme()
    val inactiveTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    val activeGrad = gradientColors ?: listOf(Color(0xFF007AFF), Color(0xFF0A84FF))

    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
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
                    .border(0.5.dp, if (isDark) Color(0x22FFFFFF) else Color(0x10000000), trackShape)
            ) {
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
                width = 24.dp,
                height = 24.dp,
                scale = thumbScale,
                baseAlpha = 1.0f,
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
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.65f,
            stiffness = 600f
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
            val trackShape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(trackShape)
                    .background(inactiveColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                        .clip(trackShape)
                        .background(activeColor)
                )
            }
        },
        thumb = {
            val thumbWidth = 24.dp
            val thumbHeight = 24.dp
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val knobShape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp)
            
            Box(
                modifier = Modifier
                    .size(width = thumbWidth, height = thumbHeight)
                    .scale(scale)
                    .clip(knobShape)
                    .background(Color.White)
                    .border(
                        width = 0.5.dp, 
                        color = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.08f), 
                        shape = knobShape
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
