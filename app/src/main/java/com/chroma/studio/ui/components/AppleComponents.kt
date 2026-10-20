package com.chroma.studio.ui.components
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LiquidGlassKnob(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    scale: Float,
    baseAlpha: Float,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width, height)
            .scale(scale)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.22f),
                ambientColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(CircleShape)
            .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = CircleShape) else Modifier)
            .background(Color.White.copy(alpha = baseAlpha.coerceAtLeast(0.96f)))
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 2.dp, end = 2.dp, top = 1.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.95f),
                        1.0f to Color.White.copy(alpha = 0.1f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 2.dp, end = 2.dp, bottom = 1.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(50.dp))
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
    activeTrackColor: Color = Color(0xFF0A84FF),
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

    val targetTranslation = if (checked && isPressed) 15f
                            else if (checked) 16f
                            else if (isPressed) -3f
                            else 0f

    val targetWidth = if (isPressed) 28.dp else 24.dp
    // Match exact liquid active scale
    val targetScale = if (isPressed) 1.26f else 1f

    val jellySpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.5f, stiffness = 600f)
    val jellySpringDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.5f, stiffness = 600f)

    val thumbTranslationX by animateFloatAsState(targetTranslation, animationSpec = jellySpring, label = "tx")
    val thumbWidth by animateDpAsState(targetWidth, animationSpec = jellySpringDp, label = "tw")
    val thumbScale by animateFloatAsState(targetScale, animationSpec = jellySpring, label = "ts")

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .width(44.dp)
            .height(22.dp)
            // NO clip on outer Box â€” lets the knob visually overflow the track border on press
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
        // Clipped pill-shaped track background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(100.dp))
                .background(trackColor)
                .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
        )
        // Knob â€” unclipped so it bleeds outside the track on press (iOS-authentic overflow)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                .offset(x = thumbTranslationX.dp)
                .size(width = thumbWidth, height = 18.dp)
        ) {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 18.dp,
                scale = thumbScale,
                baseAlpha = 0.1f,
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
    trackHeight: Dp = 6.5.dp
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

    val thumbWidth by animateDpAsState(
        targetValue = if (isInteracting) 28.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "tw"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "ts"
    )

    val isDark = isSystemInDarkTheme()
    val inactiveTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val trackBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
    val activeGrad = gradientColors ?: listOf(Color(0xFF2997FF), Color(0xFF0071E3))

    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth().height(24.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(inactiveTrackColor)
                    .border(0.5.dp, trackBorderColor, CircleShape)
            ) {
                if (fraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(CircleShape)
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
                baseAlpha = 1.0f,
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

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 500f
        ),
        label = "jellyScale"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        interactionSource = interactionSource,
        modifier = modifier,
        colors = SliderDefaults.colors(
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            thumbColor = Color.Transparent
        ),
        track = { sliderState ->
            val fraction = (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
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
            val thumbHeight = 18.dp
            
            Box(
                modifier = Modifier
                    .size(width = thumbWidth, height = thumbHeight)
                    .scale(scale)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = Color.Black.copy(alpha = 0.3f),
                        ambientColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .background(Color.White, shape = RoundedCornerShape(50))
                    .border(
                        width = 0.5.dp, 
                        color = Color.Black.copy(alpha = 0.15f), 
                        shape = RoundedCornerShape(50)
                    )
            )
        }
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
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    
    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 1.1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(250, easing = LinearOutSlowInEasing),
        label = "dialogAlpha"
    )

    val dialogBg = if (isSystemInDarkTheme) Color(0xEB2C2C2E) else Color(0xEBFFFFFF)
    val titleColor = if (isSystemInDarkTheme) Color.White else Color.Black
    val dividerColor = if (isSystemInDarkTheme) Color(0x40545458) else Color(0x203C3C43)
    val blueColor = if (isSystemInDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val redColor = if (isSystemInDarkTheme) Color(0xFFFF453A) else Color(0xFFFF3B30)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

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
                        VerticalDivider(color = dividerColor, thickness = 0.5.dp)
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
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
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
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE61C1C1E))
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// AUTHENTIC iOS / macOS POPUP DIALOGS, ACTION SHEETS, MENUS & DYNAMIC ISLAND
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Data model for iOS Action Sheet item
 */
data class IosActionSheetItem(
    val title: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Data model for iOS Context Menu item
 */
data class IosContextMenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Standard iOS-style Alert Dialog matching Apple Human Interface Guidelines:
 * - 270dp width, 14dp rounded corners, frosted translucent surface
 * - 0.5dp hairline dividers
 * - 44dp button heights
 * - Standard 2-button (horizontal), Single-button, Destructive (stacked vertical), or Custom content
 */
@Composable
fun IosAlertDialog(
    title: String,
    message: String? = null,
    confirmText: String = "OK",
    onConfirm: () -> Unit,
    dismissText: String? = "Cancel",
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    customContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val dialogBg = if (isDark) Color(0xF22C2C2E) else Color(0xF2FFFFFF)
    val titleColor = if (isDark) Color.White else Color.Black
    val messageColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
    val dividerColor = if (isDark) Color(0x36545458) else Color(0x363C3C43)
    val blueColor = Color(0xFF007AFF)
    val redColor = Color(0xFFFF3B30)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val scaleAnim = remember { Animatable(0.95f) }
        val alphaAnim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                scaleAnim.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                alphaAnim.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .shadow(24.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Content Area
                Column(
                    modifier = Modifier.padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    if (!message.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message,
                            color = messageColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                    if (customContent != null) {
                        Spacer(Modifier.height(14.dp))
                        customContent()
                    }
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Actions Area
                if (isDestructive) {
                    // Stacked Actions for Destructive (Destructive action top, Cancel bottom)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { onConfirm() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = redColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        }
                        if (dismissText != null) {
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dismissText,
                                    color = blueColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                } else if (dismissText != null) {
                    // Standard 2-Button Horizontal Layout
                    Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dismissText,
                                color = blueColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        }
                        VerticalDivider(color = dividerColor, thickness = 0.5.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onConfirm() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = blueColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                } else {
                    // Single Button Dismiss Alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
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

/**
 * Authentic iOS Action Sheet with separate options container and detached cancel button group
 */
@Composable
fun IosActionSheet(
    title: String? = null,
    items: List<IosActionSheetItem>,
    cancelText: String = "Cancel",
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val groupBg = if (isDark) Color(0xF02C2C2E) else Color(0xF0FFFFFF)
    val cancelBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val dividerColor = if (isDark) Color(0x36545458) else Color(0x363C3C43)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val slideAnim = remember { Animatable(100f) }
        val alphaAnim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                slideAnim.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                alphaAnim.animateTo(1f, tween(200, easing = LinearOutSlowInEasing))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .graphicsLayer {
                        translationY = slideAnim.value
                        alpha = alphaAnim.value
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Options Group
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(groupBg)
                ) {
                    if (!title.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = Color(0xFF8E8E93),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                    }

                    items.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clickable {
                                    onDismiss()
                                    item.onClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.title,
                                color = if (item.isDestructive) Color(0xFFFF3B30) else Color(0xFF007AFF),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        }
                        if (index < items.lastIndex) {
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Detached Cancel Group
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(cancelBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        color = Color(0xFF007AFF),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Authentic iOS Context Menu Card with trailing icons and hairline dividers
 */
@Composable
fun IosContextMenuModal(
    items: List<IosContextMenuItem>,
    onDismiss: () -> Unit,
    headerContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val menuBg = if (isDark) Color(0xF21E1E1E) else Color(0xF2FFFFFF)
    val dividerColor = if (isDark) Color(0x36545458) else Color(0x363C3C43)
    val textColor = if (isDark) Color.White else Color.Black

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val scaleAnim = remember { Animatable(0.6f) }
        val alphaAnim = remember { Animatable(0f) }
        val slideAnim = remember { Animatable(24f) }

        LaunchedEffect(Unit) {
            launch {
                scaleAnim.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                alphaAnim.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
            }
            launch {
                slideAnim.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                        translationY = slideAnim.value
                    }
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                if (headerContent != null) {
                    headerContent()
                    Spacer(Modifier.height(14.dp))
                }

                // Inset Grouped Context Menu Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(menuBg)
                        .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    item.onClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.title,
                                color = if (item.isDestructive) Color(0xFFFF3B30) else textColor,
                                fontSize = 17.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = if (item.isDestructive) FontWeight.Medium else FontWeight.Normal
                            )
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.title,
                                    tint = if (item.isDestructive) Color(0xFFFF3B30) else (if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                headerContent?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    it()
                }
            }
        }
    }
}

/**
 * Multi-event Apple Dynamic Island & Notch Island In-App Notification Host:
 * - Subscribes to DynamicIslandManager.events (Messages, Likes, Follows, Remixes, Status Toasts)
 * - Fluid Apple spring physics with sensor hole illusion and adaptive heights
 * - Interactive inline quick-reply, follow-back, and tap-to-navigate actions
 * - Supports floating Dynamic Island and top-attached Notch modes
 */
@Composable
fun AppleDynamicWaveform(tint: Color = Color(0xFF34C759)) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "h3"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp).padding(end = 4.dp)
    ) {
        Box(Modifier.width(2.5.dp).height(h1.dp).clip(CircleShape).background(tint))
        Box(Modifier.width(2.5.dp).height(h2.dp).clip(CircleShape).background(tint))
        Box(Modifier.width(2.5.dp).height(h3.dp).clip(CircleShape).background(tint))
    }
}

/**
 * Advanced Apple iOS Dynamic Island & Notch Island In-App Notification Host:
 * - Fluid Apple spring physics with morphing pill geometry and TrueDepth sensor hole
 * - Starts in non-intrusive compact pill mode to prevent covering headers or blocking the screen
 * - Tap to expand into rich interactive card with quick actions
 * - Instant swipe-up to dismiss gesture with elastic resistance and tactile haptics
 * - Auto-dismiss pause when user is actively composing a reply
 * - Authentic Apple thin glossy 3D border (0.8dp specular reflection gradient)
 * - Deep OLED obsidian background with top specular rim highlight
 */
@Composable
fun IosDynamicIslandHost(
    modifier: Modifier = Modifier,
    isNotchMode: Boolean = false
) {
    if (DynamicIslandManager.isSuppressed) return

    var currentEvent by remember { mutableStateOf<DynamicIslandEvent?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
        label = "islandOffsetY"
    )
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        DynamicIslandManager.events.collect { event ->
            currentEvent = event
            isExpanded = false // Never aggressively auto-expand by default
            replyText = ""
            offsetY = 0f
            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
        }
    }

    LaunchedEffect(currentEvent, isExpanded, replyText.isEmpty()) {
        val event = currentEvent ?: return@LaunchedEffect
        if (isExpanded) {
            // Expanded mode: do NOT dismiss if user is actively composing a reply
            if (replyText.isNotEmpty()) {
                return@LaunchedEffect
            }
            // Gentle timeout for expanded card if untouched
            delay(7500L)
            isExpanded = false
            delay(280L)
            if (currentEvent == event && !isExpanded) {
                currentEvent = null
            }
        } else {
            // Compact pill mode: polite, non-annoying glance timer
            val duration = when (event) {
                is DynamicIslandEvent.Status -> (event.durationMs).coerceIn(2000L, 2600L)
                is DynamicIslandEvent.ChatMessage -> 3600L
                else -> 3200L
            }
            delay(duration)
            if (currentEvent == event && !isExpanded) {
                currentEvent = null
            }
        }
    }

    currentEvent?.let { event ->
        val islandWidth by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 356.dp
                    is DynamicIslandEvent.Follow -> 340.dp
                    is DynamicIslandEvent.Like -> 330.dp
                    is DynamicIslandEvent.RemixOrDownload -> 330.dp
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null || event.message.isNotBlank()) 340.dp else 296.dp
                }
            } else {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 260.dp
                    is DynamicIslandEvent.Follow -> 240.dp
                    is DynamicIslandEvent.Like -> 230.dp
                    is DynamicIslandEvent.RemixOrDownload -> 240.dp
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null) 240.dp else if (event.title.length > 20) 250.dp else 210.dp
                }
            },
            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
            label = "islandWidth"
        )
        val islandHeight by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 122.dp
                    is DynamicIslandEvent.Follow -> 64.dp
                    is DynamicIslandEvent.Like -> 60.dp
                    is DynamicIslandEvent.RemixOrDownload -> 60.dp
                    is DynamicIslandEvent.Status -> if (event.message.isNotBlank()) 64.dp else 52.dp
                }
            } else 38.dp,
            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
            label = "islandHeight"
        )
        val islandCorner by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 32.dp
                    is DynamicIslandEvent.Follow -> 28.dp
                    is DynamicIslandEvent.Like -> 28.dp
                    is DynamicIslandEvent.RemixOrDownload -> 28.dp
                    is DynamicIslandEvent.Status -> 26.dp
                }
            } else 19.dp,
            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
            label = "islandCorner"
        )

        val shape = if (isNotchMode) {
            RoundedCornerShape(bottomStart = islandCorner, bottomEnd = islandCorner, topStart = 0.dp, topEnd = 0.dp)
        } else {
            RoundedCornerShape(islandCorner)
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (!isNotchMode) Modifier.statusBarsPadding().padding(top = 6.dp) else Modifier)
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .zIndex(999f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(width = islandWidth, height = islandHeight)
                    .shadow(
                        elevation = if (isExpanded) 14.dp else 8.dp,
                        shape = shape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = 0.50f),
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(shape)
                    .background(Color(0xFF000000)) // 100% opaque base ensures nothing behind bleeds through
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF161616), // Solid deep charcoal black (100% opaque)
                                Color(0xFF000000)  // Pure darkest pitch black
                            )
                        )
                    )
                    .border(
                        width = 1.25.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.65f), // Crisp top specular glass gleam
                                Color.White.copy(alpha = 0.35f), // Upper curve glass refraction
                                Color.White.copy(alpha = 0.20f), // Side glass bezel (clearly visible)
                                Color.White.copy(alpha = 0.28f), // Lower bezel transition
                                Color.White.copy(alpha = 0.45f)  // Bottom upward bounce reflection
                            )
                        ),
                        shape = shape
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount < 0 || offsetY < 0) {
                                    change.consume()
                                    offsetY = (offsetY + dragAmount).coerceAtLeast(-100f)
                                }
                            },
                            onDragEnd = {
                                if (offsetY < -12f) {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    coroutineScope.launch {
                                        currentEvent = null
                                        isExpanded = false
                                        offsetY = 0f
                                    }
                                } else {
                                    offsetY = 0f
                                }
                            },
                            onDragCancel = {
                                offsetY = 0f
                            }
                        )
                    }
                    .clickable(enabled = !isExpanded) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                        isExpanded = true
                    }
                    .padding(if (isExpanded) 12.dp else 6.dp)
            ) {
                if (!isExpanded) {
                    // COMPACT PILL PRESENTATION
                    when (event) {
                        is DynamicIslandEvent.ChatMessage -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    if (event.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(event.avatarUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF34C759)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.MessageSquare, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${event.senderName}: ${event.messageText}",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                AppleDynamicWaveform(tint = Color(0xFF34C759))
                            }
                        }

                        is DynamicIslandEvent.Status -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    val iconBg = when {
                                        event.title.contains("copied", ignoreCase = true) || event.title.contains("saved", ignoreCase = true) || event.title.contains("success", ignoreCase = true) || event.title.contains("updated", ignoreCase = true) -> Color(0xFF34C759)
                                        event.title.contains("delete", ignoreCase = true) || event.title.contains("remove", ignoreCase = true) || event.title.contains("error", ignoreCase = true) || event.title.contains("failed", ignoreCase = true) -> Color(0xFFFF3B30)
                                        event.title.contains("like", ignoreCase = true) -> Color(0xFFFF2D55)
                                        event.title.contains("follow", ignoreCase = true) -> Color(0xFF007AFF)
                                        else -> Color(0xFF5856D6)
                                    }
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(event.icon ?: Lucide.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = event.title,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (event.actionLabel != null) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF007AFF))
                                            .clickable {
                                                event.onAction?.invoke()
                                                currentEvent = null
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(event.actionLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C2C2E))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    )
                                }
                            }
                        }

                        is DynamicIslandEvent.Like -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFF2D55)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Heart, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${event.userName} liked your post",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2D55)))
                            }
                        }

                        is DynamicIslandEvent.Follow -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.UserPlus, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${event.userName} followed you",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                            }
                        }

                        is DynamicIslandEvent.RemixOrDownload -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFF9500)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (event.isRemix) {
                                            AiWaveSphereIcon(modifier = Modifier.size(15.dp))
                                        } else {
                                            Icon(Lucide.Download, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (event.isRemix) "${event.userName} remixed" else "${event.userName} downloaded",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9500)))
                            }
                        }
                    }
                } else {
                    // EXPANDED INTERACTIVE VIEW
                    when (event) {
                        is DynamicIslandEvent.ChatMessage -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (event.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(event.avatarUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF34C759)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.MessageSquare, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = event.senderName,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = event.timeText,
                                                color = Color(0xFF8E8E93),
                                                fontSize = 12.sp,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                        Text(
                                            text = event.messageText,
                                            color = Color(0xFFD1D1D6),
                                            fontSize = 13.sp,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Close button
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                isExpanded = false
                                                currentEvent = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF2C2C2E))
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = replyText,
                                            onValueChange = { replyText = it },
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontFamily = InterFontFamily
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { innerTextField ->
                                                if (replyText.isEmpty()) {
                                                    Text(
                                                        text = "iMessage reply...",
                                                        color = Color(0xFF8E8E93),
                                                        fontSize = 13.5.sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF007AFF))
                                            .clickable {
                                                if (replyText.isNotBlank()) {
                                                    event.onReply?.invoke(replyText)
                                                    replyText = ""
                                                } else {
                                                    event.onClick?.invoke()
                                                }
                                                isExpanded = false
                                                currentEvent = null
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (replyText.isNotBlank()) "Send" else "Open",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Like -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF2D55)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Heart, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = event.userName,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "liked ${event.targetTitle}",
                                            color = Color(0xFFD1D1D6),
                                            fontSize = 12.sp,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(0x33FF2D55))
                                            .border(1.dp, Color(0xFFFF2D55).copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                                            .clickable {
                                                event.onClick?.invoke()
                                                currentEvent = null
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "View",
                                            color = Color(0xFFFF375F),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                isExpanded = false
                                                currentEvent = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Follow -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (event.userAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(event.userAvatar).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF007AFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.UserPlus, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = event.userName,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "started following you",
                                            color = Color(0xFFD1D1D6),
                                            fontSize = 12.sp,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.onFollowBack != null) {
                                        Box(
                                            modifier = Modifier
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(Color(0xFF007AFF))
                                                .clickable {
                                                    event.onFollowBack.invoke()
                                                    currentEvent = null
                                                }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Follow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(0xFF2C2C2E))
                                            .clickable {
                                                event.onClick?.invoke()
                                                currentEvent = null
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("View", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                isExpanded = false
                                                currentEvent = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.RemixOrDownload -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFFF9500)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (event.isRemix) {
                                            AiWaveSphereIcon(modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Lucide.Download, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = event.userName,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (event.isRemix) "remixed ${event.workTitle}" else "downloaded ${event.workTitle}",
                                            color = Color(0xFFD1D1D6),
                                            fontSize = 12.sp,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(0x33FF9500))
                                            .border(1.dp, Color(0xFFFF9500).copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                                            .clickable {
                                                event.onClick?.invoke()
                                                currentEvent = null
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("View", color = Color(0xFFFF9F0A), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                isExpanded = false
                                                currentEvent = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Status -> {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconBg = when {
                                        event.title.contains("copied", ignoreCase = true) || event.title.contains("saved", ignoreCase = true) || event.title.contains("success", ignoreCase = true) || event.title.contains("updated", ignoreCase = true) -> Color(0xFF34C759)
                                        event.title.contains("delete", ignoreCase = true) || event.title.contains("remove", ignoreCase = true) || event.title.contains("error", ignoreCase = true) || event.title.contains("failed", ignoreCase = true) -> Color(0xFFFF3B30)
                                        event.title.contains("like", ignoreCase = true) -> Color(0xFFFF2D55)
                                        event.title.contains("follow", ignoreCase = true) -> Color(0xFF007AFF)
                                        else -> Color(0xFF5856D6)
                                    }

                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape).background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(event.icon ?: Lucide.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = event.title,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (event.message.isNotBlank()) {
                                            Text(
                                                text = event.message,
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 11.5.sp,
                                                fontFamily = InterFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (event.actionLabel != null) {
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF007AFF))
                                                .clickable {
                                                    event.onAction?.invoke()
                                                    currentEvent = null
                                                }
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(event.actionLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                isExpanded = false
                                                currentEvent = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosDynamicIsland(
    isVisible: Boolean,
    senderName: String,
    messageText: String,
    avatarUrl: String = "",
    timeText: String = "now",
    onReplySend: (String) -> Unit,
    onReadClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            DynamicIslandManager.showChatMessage(
                roomId = "",
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
                timeText = timeText,
                onReply = onReplySend,
                onClick = onReadClick
            )
        }
    }
}
@Composable
fun InstagramActionOverlay(
    emojis: List<String> = listOf("❤️", "😂", "😮", "😢", "😡", "👍"),
    menuItems: List<IosContextMenuItem>,
    onReact: (String) -> Unit,
    onPlusClick: () -> Unit,
    onDismiss: () -> Unit,
    bounds: androidx.compose.ui.geometry.Rect,
    isOutgoing: Boolean,
    clonedContent: @Composable () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val scaleAnim = remember { Animatable(0.8f) }
        val alphaAnim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
            launch { alphaAnim.animateTo(1f, tween(150)) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val topPx = bounds.top
            val bottomPx = bounds.bottom
            val leftPx = bounds.left
            val rightPx = bounds.right

            val topDp = with(density) { topPx.toDp() }
            val bottomDp = with(density) { bottomPx.toDp() }
            val leftDp = with(density) { leftPx.toDp() }
            val rightDp = with(density) { rightPx.toDp() }
            val widthDp = with(density) { bounds.width.toDp() }
            val heightDp = with(density) { bounds.height.toDp() }
            
            // Adjust to prevent going off-screen
            val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
            
            var adjustedTop = topDp
            var menuTop = bottomDp + 8.dp
            var reactionBottom = topDp - 8.dp
            
            // Layout logic
            if (reactionBottom < 60.dp) {
                // Not enough space at top, push everything down
                val pushDown = 60.dp - reactionBottom
                adjustedTop += pushDown
                reactionBottom += pushDown
                menuTop += pushDown
            }
            if (menuTop + 300.dp > screenHeightDp) { // Estimate menu height
                // Not enough space at bottom, push everything up
                val pushUp = (menuTop + 300.dp) - screenHeightDp
                adjustedTop -= pushUp
                reactionBottom -= pushUp
                menuTop -= pushUp
            }
            
            // The cloned message
            Box(
                modifier = Modifier
                    .offset(x = leftDp, y = adjustedTop)
                    .width(widthDp)
                    .height(heightDp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        transformOrigin = TransformOrigin(if (isOutgoing) 1f else 0f, 0.5f)
                    }
            ) {
                clonedContent()
            }
            
            // Reaction Pill
            Row(
                modifier = Modifier
                    .offset(y = reactionBottom - 48.dp) // align bottom of pill to reactionBottom
                    .align(if (isOutgoing) Alignment.TopEnd else Alignment.TopStart)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                        transformOrigin = TransformOrigin(if (isOutgoing) 1f else 0f, 1f)
                    }
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                emojis.forEachIndexed { index, emoji ->
                    val emojiScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(index * 30L)
                        emojiScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 800f))
                    }
                    Text(
                        text = emoji,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = emojiScale.value
                                scaleY = emojiScale.value
                            }
                            .clickable { onReact(emoji) }
                            .padding(4.dp)
                    )
                }
                
                // Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (isDark) Color.DarkGray else Color.LightGray))
                
                // Plus
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "More",
                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.size(20.dp).clickable { onPlusClick() }.padding(2.dp)
                )
            }
            
            // Context Menu
            Column(
                modifier = Modifier
                    .offset(y = menuTop)
                    .align(if (isOutgoing) Alignment.TopEnd else Alignment.TopStart)
                    .padding(horizontal = 16.dp)
                    .width(220.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                        transformOrigin = TransformOrigin(if (isOutgoing) 1f else 0f, 0f)
                    }
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color.White)
                    .padding(vertical = 4.dp)
            ) {
                menuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = if (item.isDestructive) Color.Red else (if (isDark) Color.White else Color.Black),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        item.icon?.let { icon -> Icon(imageVector = icon, contentDescription = item.title, tint = if (item.isDestructive) Color.Red else (if (isDark) Color.White else Color.Black), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}



