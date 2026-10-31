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

/**
 * Liquid Jelly Knob Component
 * Pixel-perfect implementation of the Liquid Jelly Knob with curved glossy reflections,
 * ambient light rim, inset specular highlights, and elastic press physics.
 */
@Composable
fun LiquidGlassKnob(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    scale: Float = 1.0f,
    baseAlpha: Float = 0.90f,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    isToggleKnob: Boolean = false
) {
    val knobShape = RoundedCornerShape(50.dp)
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Exact CSS box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2), 0 0 0 0.5px rgba(0, 0, 0, 0.1)
            .shadow(
                elevation = 2.5.dp,
                shape = knobShape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x40000000)
            )
            .clip(knobShape)
            .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = knobShape) else Modifier)
            // Exact background: rgba(255, 255, 255, 0.90) with subtle frosted backdrop
            .background(Color.White.copy(alpha = baseAlpha))
            .border(
                0.5.dp,
                if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.10f),
                knobShape
            )
    ) {
        // Top curved glossy reflection (Exact CSS ::before pseudo element)
        // height: 48%, border-radius: 12px 12px 28px 28px / 10px 10px 6px 6px
        // background: linear-gradient(to bottom, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.1) 100%)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp, start = 2.dp, end = 2.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xF2FFFFFF), // rgba(255, 255, 255, 0.95)
                        1.0f to Color(0x1AFFFFFF)  // rgba(255, 255, 255, 0.10)
                    )
                )
        )

        // Bottom ambient light rim (Exact CSS ::after pseudo element)
        // height: 35%, border-radius: 28px 28px 12px 12px / 6px 6px 10px 10px
        // background: linear-gradient(to top, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 100%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 1.dp, start = 2.dp, end = 2.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp, topStart = 6.dp, topEnd = 6.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to Color(0xD9FFFFFF) // rgba(255, 255, 255, 0.85)
                    )
                )
        )

        // Top specular hairline highlight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.35f)
                        )
                    )
                )
        )
    }
}

/**
 * Liquid Glass Toggle Switch
 * Exact match to CSS:
 * - Track: 44px x 22px, border-radius 100px
 * - Unchecked track: #E5E5EA with inset depth shadows
 * - Checked track: #34C759 with inset depth shadows
 * - Knob: 24px x 18px horizontal pill (active: 28px x 18px, scale 1.26)
 * - Translation: unchecked -> 2px, checked -> 18px (translateX: 16px)
 * - Press states: unchecked -> -1px, checked -> 15px
 */
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
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 1f, 0.5f, 1f)),
        label = "trackColor"
    )

    // Exact CSS translations & dimensions:
    // Resting unchecked: X = 2.dp
    // Resting checked: X = 18.dp (44 - 24 - 2 = 18.dp; translateX: 16px)
    // Pressed unchecked: X = -1.dp (translateX: -3px), width: 28.dp, scale: 1.26
    // Pressed checked: X = 15.dp (translateX: 15px), width: 28.dp, scale: 1.26
    val targetKnobX = when {
        !checked && isPressed -> (-1f)
        !checked -> 2f
        checked && isPressed -> 15f
        else -> 18f
    }
    val targetKnobW = if (isPressed) 28.dp else 24.dp
    val targetScale = if (isPressed) 1.26f else 1.0f

    val jellySpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.45f, stiffness = 550f)
    val jellySpringDp = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.45f, stiffness = 550f)

    val thumbTranslationX by animateFloatAsState(targetKnobX, animationSpec = jellySpring, label = "tx")
    val thumbWidth by animateDpAsState(targetKnobW, animationSpec = jellySpringDp, label = "tw")
    val thumbScale by animateFloatAsState(targetScale, animationSpec = jellySpring, label = "tscale")

    Box(
        modifier = modifier
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
        // Track: 44x22 rounded pill with inset depth shadows (inset 0 2px 5px rgba(0, 0, 0, 0.15))
        val trackShape = RoundedCornerShape(100.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(trackShape)
                .background(trackColor)
        ) {
            // Inset top shadow gradient:
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = if (checked) 0.20f else 0.15f),
                            1.0f to Color.Transparent
                        )
                    )
            )
            // Inset bottom subtle rim:
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            1.0f to Color.White.copy(alpha = if (checked) 0.12f else 0.08f)
                        )
                    )
            )
        }

        // Liquid Jelly Knob
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbTranslationX.dp, y = 0.dp)
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

/**
 * Settings App Range Slider with Liquid Jelly Knob
 * Exact match to CSS:
 * - Track height: 6px, background: linear-gradient(to right, #0a84ff, #e5e5ea)
 * - Track inset shadow: inset 0 2px 5px rgba(0, 0, 0, 0.15)
 * - Thumb: 24px x 18px horizontal pill capsule with Liquid Jelly optics
 * - Active state: width: 28px, transform: scale(1.15)
 */
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
    trackHeight: Dp = 6.dp,
    onValueChangeFinished: (() -> Unit)? = null,
    onInteractingChange: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isInteracting) {
        onInteractingChange?.invoke(isInteracting)
        if (isInteracting) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    // Exact CSS active press state: width: 28px, transform: scale(1.15)
    val thumbWidth by animateDpAsState(
        targetValue = if (isInteracting) 28.dp else 24.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "tsW"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.15f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "tsScale"
    )

    val isDark = isSystemInDarkTheme()
    val inactiveTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    val activeGrad = gradientColors ?: listOf(Color(0xFF0A84FF), Color(0xFF0A84FF))

    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
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
            ) {
                // Inactive track inset shadow: inset 0 2px 5px rgba(0, 0, 0, 0.15)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.15f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Active track gradient (#0a84ff)
                if (fraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(trackShape)
                            .background(Brush.horizontalGradient(activeGrad))
                    ) {
                        // Active track inset shadow:
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.15f),
                                        1.0f to Color.Transparent
                                    )
                                )
                        )
                    }
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
    activeColor: Color = Color(0xFF0A84FF),
    inactiveColor: Color = Color(0xFFE5E5EA),
    modifier: Modifier = Modifier
) {
    AppleSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        gradientColors = listOf(activeColor, activeColor),
        trackHeight = 6.dp,
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
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "islandOffsetY"
    )

    // Spring, Jump and Jelly Animatable physics states anchored to top edge (0.5f, 0f)
    val jumpOffsetY = remember { Animatable(0f) }
    val jellyScaleX = remember { Animatable(1f) }
    val jellyScaleY = remember { Animatable(1f) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    suspend fun performDismiss() {
        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
        coroutineScope.launch {
            launch { jumpOffsetY.animateTo(-35f, tween(160, easing = FastOutLinearInEasing)) }
            launch { jellyScaleX.animateTo(0.68f, tween(160, easing = FastOutLinearInEasing)) }
            launch { jellyScaleY.animateTo(0.18f, tween(160, easing = FastOutLinearInEasing)) }
        }
        delay(170L)
        currentEvent = null
        isExpanded = false
        offsetY = 0f
        offsetX = 0f
        jumpOffsetY.snapTo(0f)
        jellyScaleX.snapTo(1f)
        jellyScaleY.snapTo(1f)
    }

    fun dismissIsland() {
        coroutineScope.launch {
            performDismiss()
        }
    }

    LaunchedEffect(Unit) {
        DynamicIslandManager.events.collect { event ->
            currentEvent = event
            isExpanded = false // Never aggressively auto-expand by default
            replyText = ""
            offsetY = 0f
            offsetX = 0f

            // Play Dynamic Island notification sound effect
            try {
                com.chroma.studio.chat.sound.SoundManager.playDynamicIsland(context)
            } catch (_: Throwable) {}

            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}

            // Spring, Jump and Jelly elastic entrance physics anchored to top notch
            coroutineScope.launch {
                jumpOffsetY.snapTo(-35f)
                jellyScaleX.snapTo(0.86f)
                jellyScaleY.snapTo(1.24f)

                launch {
                    jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleX.animateTo(1.09f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                    jellyScaleX.animateTo(0.97f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleX.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleY.animateTo(0.92f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                    jellyScaleY.animateTo(1.03f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleY.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }

    LaunchedEffect(isExpanded) {
        if (currentEvent == null) return@LaunchedEffect
        if (isExpanded) {
            // EXPAND: Spring, Jump & Jelly stretch downwards from top edge
            coroutineScope.launch {
                launch {
                    jumpOffsetY.animateTo(10f, tween(60, easing = FastOutSlowInEasing))
                    jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleX.animateTo(0.93f, tween(60, easing = FastOutSlowInEasing))
                    jellyScaleX.animateTo(1.05f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleX.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleY.animateTo(1.12f, tween(60, easing = FastOutSlowInEasing))
                    jellyScaleY.animateTo(0.96f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleY.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                }
            }
        } else {
            // COMPRESS: Recoil up into notch with jelly squash
            coroutineScope.launch {
                launch {
                    jumpOffsetY.animateTo(-6f, tween(50, easing = FastOutSlowInEasing))
                    jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleX.animateTo(1.07f, tween(50, easing = FastOutSlowInEasing))
                    jellyScaleX.animateTo(0.97f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleX.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    jellyScaleY.animateTo(0.89f, tween(50, easing = FastOutSlowInEasing))
                    jellyScaleY.animateTo(1.03f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                    jellyScaleY.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }

    LaunchedEffect(currentEvent, isExpanded, replyText.isEmpty()) {
        val event = currentEvent ?: return@LaunchedEffect
        if (isExpanded) {
            // Expanded mode: do NOT dismiss if user is actively composing a reply
            if (replyText.isNotEmpty()) {
                return@LaunchedEffect
            }
            // Polite timeout for expanded card if untouched (avoids blocking screen)
            delay(4500L)
            isExpanded = false
            delay(240L)
            if (currentEvent == event && !isExpanded) {
                performDismiss()
            }
        } else {
            // Compact pill mode: perfectly tuned glance timer (snappy & non-intrusive)
            val duration = when (event) {
                is DynamicIslandEvent.Status -> (event.durationMs).coerceIn(1800L, 2400L)
                is DynamicIslandEvent.ChatMessage -> 3200L
                is DynamicIslandEvent.Like -> 2000L
                is DynamicIslandEvent.Follow -> 2200L
                is DynamicIslandEvent.RemixOrDownload -> 2000L
                else -> 2200L
            }
            delay(duration)
            if (currentEvent == event && !isExpanded) {
                performDismiss()
            }
        }
    }

    currentEvent?.let { event ->
        val islandWidth by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 360.dp
                    is DynamicIslandEvent.Follow -> 348.dp
                    is DynamicIslandEvent.Like -> 340.dp
                    is DynamicIslandEvent.RemixOrDownload -> 340.dp
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null || event.message.isNotBlank()) 348.dp else 300.dp
                }
            } else {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 270.dp
                    is DynamicIslandEvent.Follow -> 250.dp
                    is DynamicIslandEvent.Like -> 240.dp
                    is DynamicIslandEvent.RemixOrDownload -> 250.dp
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null) 250.dp else if (event.title.length > 20) 260.dp else 220.dp
                }
            },
            animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
            label = "islandWidth"
        )
        val islandHeight by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.ChatMessage -> 180.dp
                    is DynamicIslandEvent.Follow -> 72.dp
                    is DynamicIslandEvent.Like -> 68.dp
                    is DynamicIslandEvent.RemixOrDownload -> 68.dp
                    is DynamicIslandEvent.Status -> if (event.message.isNotBlank()) 72.dp else 56.dp
                }
            } else 44.dp,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
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
            } else 22.dp,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
            label = "islandCorner"
        )

        val shape = RoundedCornerShape(islandCorner)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .offset { IntOffset(offsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .zIndex(999f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = jellyScaleX.value
                        scaleY = jellyScaleY.value
                        translationY = jumpOffsetY.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
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
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y < 0) {
                                    offsetY = (offsetY + dragAmount.y).coerceAtLeast(-120f)
                                }
                                offsetX = (offsetX + dragAmount.x).coerceIn(-180f, 180f)
                            },
                            onDragEnd = {
                                if (offsetY < -10f || kotlin.math.abs(offsetX) > 35f) {
                                    dismissIsland()
                                } else {
                                    offsetY = 0f
                                    offsetX = 0f
                                }
                            },
                            onDragCancel = {
                                offsetY = 0f
                                offsetX = 0f
                            }
                        )
                    }
                    .clickable(enabled = !isExpanded) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                        isExpanded = true
                    }
                    .padding(if (isExpanded) 12.dp else 6.dp)
            ) {
                if (!isExpanded) {
                    // COMPACT NOTCH PRESENTATION (Clean icons without pill/capsule/circle backgrounds)
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
                                    IosAvatarView(
                                        name = event.senderName,
                                        avatarUrl = event.avatarUrl,
                                        size = 24.dp
                                    )
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
                                    val iconTint = when {
                                        event.title.contains("copied", ignoreCase = true) || event.title.contains("saved", ignoreCase = true) || event.title.contains("success", ignoreCase = true) || event.title.contains("updated", ignoreCase = true) -> Color(0xFF34C759)
                                        event.title.contains("delete", ignoreCase = true) || event.title.contains("remove", ignoreCase = true) || event.title.contains("error", ignoreCase = true) || event.title.contains("failed", ignoreCase = true) -> Color(0xFFFF3B30)
                                        event.title.contains("like", ignoreCase = true) -> Color(0xFFFF2D55)
                                        event.title.contains("follow", ignoreCase = true) -> Color(0xFF007AFF)
                                        else -> Color(0xFF5856D6)
                                    }
                                    Icon(event.icon ?: Lucide.Check, null, tint = iconTint, modifier = Modifier.size(18.dp))
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
                                    val islandActionShape = RoundedCornerShape(9999.dp)
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .clip(islandActionShape)
                                            .background(com.chroma.studio.ui.components.LiquidGlassTokens.AppleSystemBlue)
                                            .border(0.5.dp, Color(0x8C0051D5), islandActionShape)
                                            .border(1.dp, com.chroma.studio.ui.components.LiquidGlassTokens.AppleSpecularGlassBorder, islandActionShape)
                                            .clickable {
                                                event.onAction?.invoke()
                                                dismissIsland()
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(event.actionLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
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
                                    Icon(Lucide.Heart, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
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
                                Icon(Lucide.Heart, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(13.dp))
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
                                    Icon(Lucide.UserPlus, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
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
                                Icon(Lucide.UserPlus, null, tint = Color(0xFF007AFF), modifier = Modifier.size(13.dp))
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
                                    if (event.isRemix) {
                                        AiWaveSphereIcon(modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Lucide.Download, null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
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
                                Icon(Lucide.Download, null, tint = Color(0xFFFF9500), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                } else {
                    // EXPANDED INTERACTIVE NOTCH VIEW (Zero pill/capsule/circle backgrounds on icons)
                    when (event) {
                        is DynamicIslandEvent.ChatMessage -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Header: Avatar + Sender Name + Time + Dismiss X
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IosAvatarView(
                                        name = event.senderName,
                                        avatarUrl = event.avatarUrl,
                                        size = 34.dp
                                    )

                                    Spacer(Modifier.width(9.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = event.senderName,
                                                color = Color.White,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = event.timeText,
                                                color = Color(0xFF8E8E93),
                                                fontSize = 11.5.sp,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                        Text(
                                            text = event.messageText,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.5.sp,
                                            fontFamily = InterFontFamily,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Close button
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Dismiss",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { dismissIsland() }
                                    )
                                }

                                // 2. Quick Emoji Reactions (Instant reply without leaving current screen)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("👍", "❤️", "😂", "🔥", "🙌").forEach { emoji ->
                                        val emojiPillShape = RoundedCornerShape(9999.dp)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(emojiPillShape)
                                                .background(Color(0xFF2C2C2E).copy(alpha = 0.85f))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), emojiPillShape)
                                                .clickable {
                                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                                    if (event.onQuickReact != null) {
                                                        event.onQuickReact.invoke(emoji)
                                                    } else {
                                                        event.onReply?.invoke(emoji)
                                                    }
                                                    dismissIsland()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 14.sp)
                                        }
                                    }
                                }

                                // 3. Quick Action Row: Inline Reply TextField + Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val replyPillShape = RoundedCornerShape(9999.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(replyPillShape)
                                            .background(Color(0xFF242426))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), replyPillShape)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = replyText,
                                            onValueChange = { replyText = it },
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontFamily = InterFontFamily
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { innerTextField ->
                                                if (replyText.isEmpty()) {
                                                    Text(
                                                        text = "Reply to ${event.senderName}...",
                                                        color = Color(0xFF8E8E93),
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFontFamily,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }

                                    if (replyText.isNotBlank()) {
                                        // Send button with AppleSpecularGlassBorder
                                        val sendPillShape = RoundedCornerShape(9999.dp)
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clip(sendPillShape)
                                                .background(com.chroma.studio.ui.components.LiquidGlassTokens.AppleSystemBlue)
                                                .border(0.5.dp, Color(0x8C0051D5), sendPillShape)
                                                .border(1.dp, com.chroma.studio.ui.components.LiquidGlassTokens.AppleSpecularGlassBorder, sendPillShape)
                                                .clickable {
                                                    event.onReply?.invoke(replyText)
                                                    replyText = ""
                                                    dismissIsland()
                                                }
                                                .padding(horizontal = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Send",
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    } else {
                                        // "Mark Read" pill
                                        if (event.onMarkAsRead != null) {
                                            val markReadShape = RoundedCornerShape(9999.dp)
                                            Box(
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(markReadShape)
                                                    .background(Color(0xFF2C2C2E))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), markReadShape)
                                                    .clickable {
                                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                                        event.onMarkAsRead.invoke()
                                                        dismissIsland()
                                                    }
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Read",
                                                    color = Color(0xFFD1D1D6),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = InterFontFamily
                                                )
                                            }
                                        }

                                        // "Open" chat button
                                        val openPillShape = RoundedCornerShape(9999.dp)
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clip(openPillShape)
                                                .background(com.chroma.studio.ui.components.LiquidGlassTokens.AppleSystemBlue)
                                                .border(0.5.dp, Color(0x8C0051D5), openPillShape)
                                                .border(1.dp, com.chroma.studio.ui.components.LiquidGlassTokens.AppleSpecularGlassBorder, openPillShape)
                                                .clickable {
                                                    event.onClick?.invoke()
                                                    dismissIsland()
                                                }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Open",
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily
                                            )
                                        }
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
                                    Icon(Lucide.Heart, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(26.dp))

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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33FF2D55))
                                            .border(1.dp, Color(0xFFFF2D55).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                event.onClick?.invoke()
                                                dismissIsland()
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
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { dismissIsland() }
                                    )
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
                                        Icon(Lucide.UserPlus, null, tint = Color(0xFF007AFF), modifier = Modifier.size(26.dp))
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
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF007AFF))
                                                .clickable {
                                                    event.onFollowBack.invoke()
                                                    dismissIsland()
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2C2C2E))
                                            .clickable {
                                                event.onClick?.invoke()
                                                dismissIsland()
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("View", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                                    }
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { dismissIsland() }
                                    )
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
                                    if (event.isRemix) {
                                        AiWaveSphereIcon(modifier = Modifier.size(26.dp))
                                    } else {
                                        Icon(Lucide.Download, null, tint = Color(0xFFFF9500), modifier = Modifier.size(26.dp))
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33FF9500))
                                            .border(1.dp, Color(0xFFFF9500).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                event.onClick?.invoke()
                                                dismissIsland()
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("View", color = Color(0xFFFF9F0A), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { dismissIsland() }
                                    )
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

                                    Icon(event.icon ?: Lucide.Check, null, tint = iconBg, modifier = Modifier.size(26.dp))

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
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF007AFF))
                                                .clickable {
                                                    event.onAction?.invoke()
                                                    dismissIsland()
                                                }
                                                .padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(event.actionLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        }
                                    }
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { dismissIsland() }
                                    )
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



