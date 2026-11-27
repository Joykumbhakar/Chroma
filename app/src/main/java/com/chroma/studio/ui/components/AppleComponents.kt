package com.chroma.studio.ui.components
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.chroma.studio.ui.theme.ChromaIcons

import com.composables.icons.lucide.*
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val isAppInDark: Boolean
    @Composable
    get() = LocalOscellionColors.current.isDark

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
    val isDark = isAppInDark

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
/**
 * Universal Apple HIG & Claymorphic Pill Toggle Switch.
 * Powered by real-time second-order spring physics from [AppSpringPhysics.globalConfig],
 * GPU RenderNode hardware acceleration (120 FPS), instant 1-tap responsiveness,
 * fluid drag rubber-banding, tap/hold clay zoom plumping, and acoustic feedback.
 */
@Composable
fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color.Unspecified,
    inactiveTrackColor: Color = Color.Unspecified,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    trackWidth: Dp = 48.dp,
    trackHeight: Dp = 28.dp
) {
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val effectiveActiveTrack = if (activeTrackColor != Color.Unspecified) activeTrackColor else colors.primary
    val effectiveInactiveTrack = if (inactiveTrackColor != Color.Unspecified) inactiveTrackColor else colors.cardBorderSubtle

    val haptic = LocalHapticFeedback.current
    val thumbSize = (trackHeight - 4.dp).coerceAtLeast(10.dp)
    val maxOffset = trackWidth - thumbSize - 2.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) effectiveActiveTrack else effectiveInactiveTrack,
        label = "appleSwitchTrackColor",
        animationSpec = tween(220)
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 2.dp,
        label = "appleSwitchThumbOffset",
        animationSpec = tween(220)
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.18f),
                    ambientColor = Color.Black.copy(alpha = 0.10f)
                )
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
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
/**
 * Global reactive state tracking whether a user is currently clicking, tapping, or dragging
 * a slider knob in any photo, video, or editor screen.
 *
 * When active, allows surrounding background chrome, cards, divs, and unrelated components
 * to fade to transparent or dim, keeping ONLY the active slider and the media canvas clearly visible.
 */
class SliderScrubbingState {
    var isScrubbing by mutableStateOf(false)
    var activeId by mutableStateOf<String?>(null)
}

val LocalSliderScrubbingState = compositionLocalOf { SliderScrubbingState() }

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
    onInteractingChange: ((Boolean) -> Unit)? = null,
    sliderId: String? = null
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrubbingState = LocalSliderScrubbingState.current
    val actualId = remember(sliderId) { sliderId ?: java.util.UUID.randomUUID().toString() }

    LaunchedEffect(isInteracting) {
        onInteractingChange?.invoke(isInteracting)
        if (isInteracting) {
            val wasAlreadyActive = scrubbingState.isScrubbing && scrubbingState.activeId == actualId
            scrubbingState.activeId = actualId
            scrubbingState.isScrubbing = true
            if (!wasAlreadyActive) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        } else {
            // Brief debounce avoids 1-frame flicker during PressInteraction -> DragInteraction handoff
            kotlinx.coroutines.delay(45L)
            if (scrubbingState.activeId == actualId) {
                scrubbingState.isScrubbing = false
                scrubbingState.activeId = null
            }
        }
    }

    DisposableEffect(actualId) {
        onDispose {
            if (scrubbingState.activeId == actualId) {
                scrubbingState.isScrubbing = false
                scrubbingState.activeId = null
            }
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

    val isDark = isAppInDark
    val inactiveTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    val activeGrad = gradientColors ?: listOf(Color(0xFF0381FE), Color(0xFF0381FE))

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
    activeColor: Color = Color(0xFF0381FE),
    inactiveColor: Color = Color(0xFFE5E5EA),
    modifier: Modifier = Modifier
) {
    AppleSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
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
    val isDark = isAppInDark
    
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

    val dialogBg = if (isDark) Color(0xEB2C2C2E) else Color(0xEBFFFFFF)
    val titleColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color(0x40545458) else Color(0x203C3C43)
    val blueColor = if (isDark) Color(0xFF0381FE) else Color(0xFF0381FE)
    val redColor = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)

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
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    dismissText: String? = "Cancel",
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    customContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val haptic = LocalHapticFeedback.current

    val dialogBg = if (isDark) Color(0xF52C2C2E) else Color(0xF8FFFFFF)
    val titleColor = if (isDark) Color.White else Color.Black
    val messageColor = if (isDark) Color.White.copy(alpha = 0.78f) else Color.Black.copy(alpha = 0.75f)
    val dividerColor = if (isDark) Color(0x36545458) else Color(0x2E3C3C43)
    val hairlineBorder = if (isDark) Color(0x28FFFFFF) else Color(0x16000000)
    val pressedTint = if (isDark) Color(0x1AFFFFFF) else Color(0x12000000)
    val blueColor = Color(0xFF0381FE)
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
                .background(Color.Black.copy(alpha = 0.42f * alphaAnim.value))
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(276.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogBg)
                    .border(0.5.dp, hairlineBorder, RoundedCornerShape(14.dp))
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
                    val confirmInteraction = remember { MutableInteractionSource() }
                    val confirmPressed by confirmInteraction.collectIsPressedAsState()
                    val dismissInteraction = remember { MutableInteractionSource() }
                    val dismissPressed by dismissInteraction.collectIsPressedAsState()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .background(if (confirmPressed && confirmEnabled) pressedTint else Color.Transparent)
                                .clickable(
                                    interactionSource = confirmInteraction,
                                    indication = null,
                                    enabled = confirmEnabled
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onConfirm()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = if (confirmEnabled) redColor else redColor.copy(alpha = 0.38f),
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                        if (dismissText != null) {
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp)
                                    .background(if (dismissPressed) pressedTint else Color.Transparent)
                                    .clickable(
                                        interactionSource = dismissInteraction,
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dismissText,
                                    color = blueColor,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                } else if (dismissText != null) {
                    // Standard 2-Button Horizontal Layout
                    val dismissInteraction = remember { MutableInteractionSource() }
                    val dismissPressed by dismissInteraction.collectIsPressedAsState()
                    val confirmInteraction = remember { MutableInteractionSource() }
                    val confirmPressed by confirmInteraction.collectIsPressedAsState()
                    Row(modifier = Modifier.fillMaxWidth().height(45.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (dismissPressed) pressedTint else Color.Transparent)
                                .clickable(
                                    interactionSource = dismissInteraction,
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dismissText,
                                color = blueColor,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        }
                        VerticalDivider(color = dividerColor, thickness = 0.5.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (confirmPressed && confirmEnabled) pressedTint else Color.Transparent)
                                .clickable(
                                    interactionSource = confirmInteraction,
                                    indication = null,
                                    enabled = confirmEnabled
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onConfirm()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = if (confirmEnabled) blueColor else blueColor.copy(alpha = 0.35f),
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                } else {
                    // Single Button Dismiss Alert
                    val confirmInteraction = remember { MutableInteractionSource() }
                    val confirmPressed by confirmInteraction.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .background(if (confirmPressed && confirmEnabled) pressedTint else Color.Transparent)
                            .clickable(
                                interactionSource = confirmInteraction,
                                indication = null,
                                enabled = confirmEnabled
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onConfirm()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
                            color = if (confirmEnabled) blueColor else blueColor.copy(alpha = 0.35f),
                            fontSize = 16.5.sp,
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
                                color = if (item.isDestructive) Color(0xFFFF3B30) else Color(0xFF0381FE),
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
                        color = Color(0xFF0381FE),
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
    val activePresentation by DynamicIslandManager.currentPresentation.collectAsState()
    val isSuppressed = DynamicIslandManager.isSuppressed
    var renderedPresentation by remember { mutableStateOf<DynamicIslandPresentation?>(null) }

    var isExpanded by remember { mutableStateOf(false) }
    var isComposingReply by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var didFollowBack by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val dragOffsetY = remember { Animatable(0f) }
    val dragOffsetX = remember { Animatable(0f) }

    // Smooth single-stage notch-anchored spring physics (0.5f, 0f)
    val visibilityAlpha = remember { Animatable(0f) }
    val jumpOffsetY = remember { Animatable(-28f) }
    val jellyScaleX = remember { Animatable(0.68f) }
    val jellyScaleY = remember { Animatable(0.45f) }

    var dismissJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastChimedInstanceId by remember { mutableLongStateOf(-1L) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            DynamicIslandManager.setUserInteracting(false)
        }
    }

    LaunchedEffect(isExpanded, isComposingReply, replyText, isDragging, renderedPresentation?.instanceId) {
        val active = renderedPresentation != null && !isSuppressed
        DynamicIslandManager.setUserInteracting(
            active && (isExpanded || isComposingReply || replyText.isNotBlank() || isDragging)
        )
    }

    suspend fun animateOutAndClear(targetInstanceId: Long, invokeEventDismiss: Boolean = false) {
        val current = renderedPresentation
        if (current == null || current.instanceId != targetInstanceId) return
        if (invokeEventDismiss) {
            (current.event as? DynamicIslandEvent.Status)?.onDismiss?.invoke()
        }
        try {
            kotlinx.coroutines.coroutineScope {
                launch { visibilityAlpha.animateTo(0f, tween(150, easing = FastOutLinearInEasing)) }
                launch { jumpOffsetY.animateTo(-28f, tween(160, easing = FastOutLinearInEasing)) }
                launch { jellyScaleX.animateTo(0.64f, tween(160, easing = FastOutLinearInEasing)) }
                launch { jellyScaleY.animateTo(0.42f, tween(160, easing = FastOutLinearInEasing)) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {}

        // Guard against clearing a newer notification if one arrived mid-dismiss
        if (renderedPresentation?.instanceId != targetInstanceId) return

        DynamicIslandManager.setUserInteracting(false)
        DynamicIslandManager.dismissCurrent(expectedInstanceId = targetInstanceId)
        val promoted = DynamicIslandManager.currentPresentation.value
        if (promoted == null) {
            renderedPresentation = null
            isExpanded = false
            isComposingReply = false
            replyText = ""
            didFollowBack = false
            isDragging = false
            dragOffsetY.snapTo(0f)
            dragOffsetX.snapTo(0f)
        }
    }

    fun dismissIsland(invokeEventDismiss: Boolean = false) {
        val targetId = renderedPresentation?.instanceId ?: return
        if (dismissJob?.isActive == true) return
        dismissJob = coroutineScope.launch {
            animateOutAndClear(targetInstanceId = targetId, invokeEventDismiss = invokeEventDismiss)
        }
    }

    LaunchedEffect(activePresentation?.instanceId, isSuppressed) {
        val next = activePresentation
        if (isSuppressed || next == null) {
            if (renderedPresentation != null && (dismissJob == null || dismissJob?.isActive == false)) {
                val curId = renderedPresentation?.instanceId
                if (curId != null) {
                    animateOutAndClear(targetInstanceId = curId, invokeEventDismiss = false)
                }
            }
            return@LaunchedEffect
        }

        dismissJob?.cancel()
        dismissJob = null

        val wasAlreadyVisible = renderedPresentation != null && visibilityAlpha.value > 0.25f
        renderedPresentation = next
        isExpanded = false
        isComposingReply = false
        replyText = ""
        didFollowBack = false
        isDragging = false
        dragOffsetY.snapTo(0f)
        dragOffsetX.snapTo(0f)

        // Play notification chime strictly once per unique presentation instanceId
        if (next.event !is DynamicIslandEvent.Status &&
            next.event !is DynamicIslandEvent.IncomingCall &&
            lastChimedInstanceId != next.instanceId
        ) {
            lastChimedInstanceId = next.instanceId
            try {
                com.chroma.studio.chat.sound.SoundManager.playDynamicIsland(context)
            } catch (_: Throwable) {}
            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
        }

        if (!wasAlreadyVisible) {
            visibilityAlpha.snapTo(0f)
            jumpOffsetY.snapTo(-28f)
            jellyScaleX.snapTo(0.68f)
            jellyScaleY.snapTo(0.46f)
        } else {
            // Subtle morph pulse when promoted directly from queue
            jellyScaleX.snapTo(0.92f)
            jellyScaleY.snapTo(1.06f)
        }

        kotlinx.coroutines.coroutineScope {
            launch { visibilityAlpha.animateTo(1f, tween(170, easing = FastOutSlowInEasing)) }
            launch {
                jumpOffsetY.animateTo(
                    0f,
                    spring(dampingRatio = 0.62f, stiffness = 460f)
                )
            }
            launch {
                jellyScaleX.animateTo(
                    1f,
                    spring(dampingRatio = 0.56f, stiffness = 380f)
                )
            }
            launch {
                jellyScaleY.animateTo(
                    1f,
                    spring(dampingRatio = 0.54f, stiffness = 360f)
                )
            }
        }
    }

    LaunchedEffect(isExpanded) {
        if (renderedPresentation == null) return@LaunchedEffect
        if (isExpanded) {
            kotlinx.coroutines.coroutineScope {
                launch {
                    jumpOffsetY.animateTo(4f, tween(70, easing = FastOutSlowInEasing))
                    jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.60f, stiffness = 420f))
                }
                launch {
                    jellyScaleX.animateTo(0.96f, tween(65, easing = FastOutSlowInEasing))
                    jellyScaleX.animateTo(1f, spring(dampingRatio = 0.56f, stiffness = 380f))
                }
                launch {
                    jellyScaleY.animateTo(1.06f, tween(65, easing = FastOutSlowInEasing))
                    jellyScaleY.animateTo(1f, spring(dampingRatio = 0.56f, stiffness = 380f))
                }
            }
        } else {
            isComposingReply = false
            kotlinx.coroutines.coroutineScope {
                launch {
                    jellyScaleX.animateTo(1.04f, tween(60, easing = FastOutSlowInEasing))
                    jellyScaleX.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                }
                launch {
                    jellyScaleY.animateTo(0.93f, tween(60, easing = FastOutSlowInEasing))
                    jellyScaleY.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                }
            }
        }
    }

    LaunchedEffect(renderedPresentation?.instanceId, isExpanded, isComposingReply, replyText.isEmpty(), didFollowBack, isDragging) {
        val presentation = renderedPresentation ?: return@LaunchedEffect
        val event = presentation.event
        if (isDragging) return@LaunchedEffect

        if (event is DynamicIslandEvent.IncomingCall) {
            // Live incoming call banner stays docked until answered, declined, expanded, or cancelled
            return@LaunchedEffect
        }

        if (isExpanded) {
            // Do NOT auto-dismiss while user is typing or focused on inline reply
            if (isComposingReply || replyText.isNotEmpty()) {
                return@LaunchedEffect
            }
            delay(8500L)
            isExpanded = false
            delay(260L)
            if (renderedPresentation?.instanceId == presentation.instanceId && !isExpanded && !isDragging) {
                animateOutAndClear(targetInstanceId = presentation.instanceId, invokeEventDismiss = false)
            }
        } else {
            val duration = when (event) {
                is DynamicIslandEvent.Status -> (event.durationMs).coerceIn(2200L, 4800L)
                is DynamicIslandEvent.ChatMessage -> 4800L
                is DynamicIslandEvent.Follow -> if (event.isFollowRequest) 5600L else 4200L
                is DynamicIslandEvent.Comment -> 4800L
                is DynamicIslandEvent.StoryReaction -> 4400L
                is DynamicIslandEvent.Like -> 3600L
                is DynamicIslandEvent.RemixOrDownload -> 3800L
            }
            delay(duration)
            if (renderedPresentation?.instanceId == presentation.instanceId && !isExpanded && !isDragging) {
                animateOutAndClear(targetInstanceId = presentation.instanceId, invokeEventDismiss = false)
            }
        }
    }

    val currentPresentationSnapshot = renderedPresentation ?: return
    val event = currentPresentationSnapshot.event
    run {
        val islandWidth by animateDpAsState(
            targetValue = if (isExpanded) {
                when (event) {
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null || event.secondaryActionLabel != null || event.message.isNotBlank()) 366.dp else 320.dp
                    else -> 366.dp
                }
            } else {
                when (event) {
                    is DynamicIslandEvent.Status -> if (event.actionLabel != null) 316.dp else if (event.title.length > 22) 292.dp else 252.dp
                    is DynamicIslandEvent.IncomingCall -> 364.dp
                    is DynamicIslandEvent.ChatMessage -> 344.dp
                    is DynamicIslandEvent.Follow -> 336.dp
                    is DynamicIslandEvent.Comment -> 336.dp
                    is DynamicIslandEvent.StoryReaction -> 330.dp
                    is DynamicIslandEvent.Like -> 320.dp
                    is DynamicIslandEvent.RemixOrDownload -> 326.dp
                }
            },
            animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
            label = "islandWidth"
        )
        val islandCorner by animateDpAsState(
            targetValue = if (event is DynamicIslandEvent.IncomingCall) 32.dp else if (isExpanded) 28.dp else 24.dp,
            animationSpec = spring(dampingRatio = 0.66f, stiffness = Spring.StiffnessMediumLow),
            label = "islandCorner"
        )
        val islandElevation by animateDpAsState(
            targetValue = if (isExpanded) 18.dp else 10.dp,
            animationSpec = tween(200),
            label = "islandElevation"
        )

        val shape = RoundedCornerShape(islandCorner)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                .offset { IntOffset(dragOffsetX.value.roundToInt(), dragOffsetY.value.roundToInt()) }
                .zIndex(999f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = visibilityAlpha.value.coerceIn(0f, 1f)
                        scaleX = jellyScaleX.value
                        scaleY = jellyScaleY.value
                        translationY = jumpOffsetY.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .widthIn(max = islandWidth)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .defaultMinSize(minHeight = if (isExpanded) 78.dp else 46.dp)
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                    )
                    .shadow(
                        elevation = islandElevation,
                        shape = shape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = 0.50f),
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(shape)
                    .background(Color(0xFF000000))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A1A1D),
                                Color(0xFF0A0A0C),
                                Color(0xFF000000)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.58f),
                                Color.White.copy(alpha = 0.26f),
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.32f)
                            )
                        ),
                        shape = shape
                    )
                    .pointerInput(currentPresentationSnapshot.instanceId, isExpanded, isComposingReply) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    // Rubber-band vertical drag: allow pulling down to expand, up to collapse/dismiss
                                    val nextY = dragOffsetY.value + dragAmount.y * 0.72f
                                    dragOffsetY.snapTo(nextY.coerceIn(-150f, 52f))
                                    val nextX = dragOffsetX.value + dragAmount.x * 0.75f
                                    dragOffsetX.snapTo(nextX.coerceIn(-180f, 180f))
                                }
                            },
                            onDragEnd = {
                                val dy = dragOffsetY.value
                                val dx = dragOffsetX.value
                                isDragging = false
                                when {
                                    !isExpanded && dy > 26f -> {
                                        // Pull down to smoothly expand Dynamic Island
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                        isExpanded = true
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                                        }
                                    }
                                    isExpanded && !isComposingReply && dy < -32f && dy > -96f -> {
                                        // Swipe up gently to collapse expanded card back into compact pill
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                        isExpanded = false
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = 420f))
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = 420f))
                                        }
                                    }
                                    dy < -30f || abs(dx) > 70f -> {
                                        dismissIsland(invokeEventDismiss = true)
                                    }
                                    else -> {
                                        coroutineScope.launch {
                                            launch { dragOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                            launch { dragOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    launch { dragOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                    launch { dragOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                }
                            }
                        )
                    }
                    .clickable(enabled = !isExpanded) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                        if (event is DynamicIslandEvent.IncomingCall) {
                            event.onExpand()
                        } else {
                            isExpanded = true
                        }
                    }
                    .padding(horizontal = if (isExpanded) 13.dp else 8.dp, vertical = if (isExpanded) 12.dp else 6.dp)
            ) {
                AnimatedContent(
                    targetState = Triple(currentPresentationSnapshot.instanceId, isExpanded, isComposingReply),
                    transitionSpec = {
                        (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(180, easing = FastOutSlowInEasing)))
                            .togetherWith(fadeOut(tween(110, easing = FastOutLinearInEasing)))
                    },
                    label = "islandInnerMorph"
                ) { (_, expandedState, _) ->
                if (!expandedState) {
                    // COMPACT PILL ALERT WITH QUICK ACTION & DISMISS BUTTONS
                    when (event) {
                        is DynamicIslandEvent.IncomingCall -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IosAvatarView(
                                    name = event.callerName,
                                    avatarUrl = event.callerAvatar,
                                    size = 38.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = if (event.isVideo) "FaceTime Video" else "Oscellion Audio",
                                        color = if (event.isVideo) Color(0xFF38BDF8) else Color(0xFF34C759),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = event.callerName.ifBlank { "Oscellion User" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (event.isVerified) {
                                            com.chroma.studio.webrtc.VerifiedBadgeSeal(
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                        .clickable {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                                            event.onDecline()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.chroma.studio.webrtc.HandsetIcon(
                                        modifier = Modifier.size(18.dp),
                                        rotationDegrees = 135f
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759))
                                        .clickable {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                                            event.onAccept()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (event.isVideo) Lucide.Video else Lucide.Phone,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        is DynamicIslandEvent.ChatMessage -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IosAvatarView(
                                    name = event.senderName,
                                    avatarUrl = event.avatarUrl,
                                    size = 28.dp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${event.senderName}: ${event.messageText}",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                IslandActionPill(
                                    label = "Reply",
                                    containerColor = LiquidGlassTokens.AppleSystemBlue,
                                    textColor = Color.White,
                                    height = 26.dp,
                                    horizontalPadding = 10.dp,
                                    onClick = {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                        isExpanded = true
                                        isComposingReply = true
                                    }
                                )
                                IslandCloseButton(
                                    size = 24.dp,
                                    onClick = { dismissIsland() }
                                )
                            }
                        }

                        is DynamicIslandEvent.Status -> {
                            val iconTint = statusIconTint(event.title)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(event.icon ?: Lucide.Check, null, tint = iconTint, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = event.title,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (event.actionLabel != null) {
                                    IslandActionPill(
                                        label = event.actionLabel,
                                        containerColor = LiquidGlassTokens.AppleSystemBlue,
                                        textColor = Color.White,
                                        height = 26.dp,
                                        horizontalPadding = 10.dp,
                                        onClick = {
                                            event.onAction?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                }
                                IslandCloseButton(
                                    size = 24.dp,
                                    onClick = { dismissIsland(invokeEventDismiss = true) }
                                )
                            }
                        }

                        is DynamicIslandEvent.Like -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IslandBadgeAvatar(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    size = 28.dp,
                                    badgeBg = Color(0xFFFF2D55),
                                    badgeIcon = ChromaIcons.HeartFilled
                                )
                                Text(
                                    text = "${event.userName} liked ${event.targetTitle}",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IslandActionPill(
                                    label = "View",
                                    containerColor = Color(0x38FF2D55),
                                    borderColor = Color(0xFFFF2D55).copy(alpha = 0.55f),
                                    textColor = Color(0xFFFF4F70),
                                    height = 26.dp,
                                    horizontalPadding = 10.dp,
                                    onClick = {
                                        event.onClick?.invoke()
                                        dismissIsland()
                                    }
                                )
                                IslandCloseButton(size = 24.dp, onClick = { dismissIsland() })
                            }
                        }

                        is DynamicIslandEvent.Follow -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IslandBadgeAvatar(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    size = 28.dp,
                                    badgeBg = Color(0xFF0381FE),
                                    badgeIcon = Lucide.UserPlus
                                )
                                Text(
                                    text = if (event.isFollowRequest) "${event.userName} wants to follow" else "${event.userName} followed you",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                when {
                                    event.isFollowRequest && event.onAccept != null -> {
                                        IslandActionPill(
                                            label = "Accept",
                                            containerColor = LiquidGlassTokens.AppleSystemBlue,
                                            textColor = Color.White,
                                            height = 26.dp,
                                            horizontalPadding = 10.dp,
                                            onClick = {
                                                event.onAccept.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    event.onFollowBack != null -> {
                                        IslandActionPill(
                                            label = if (didFollowBack) "Following" else "Follow",
                                            containerColor = if (didFollowBack) Color(0xFF2C2C2E) else LiquidGlassTokens.AppleSystemBlue,
                                            textColor = if (didFollowBack) Color(0xFF34C759) else Color.White,
                                            height = 26.dp,
                                            horizontalPadding = 10.dp,
                                            onClick = {
                                                if (!didFollowBack) {
                                                    event.onFollowBack.invoke()
                                                    didFollowBack = true
                                                }
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    else -> {
                                        IslandActionPill(
                                            label = "View",
                                            containerColor = Color(0xFF2C2C2E),
                                            textColor = Color.White,
                                            height = 26.dp,
                                            horizontalPadding = 10.dp,
                                            onClick = {
                                                event.onClick?.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                                IslandCloseButton(size = 24.dp, onClick = { dismissIsland() })
                            }
                        }

                        is DynamicIslandEvent.RemixOrDownload -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (event.userAvatar.isNotBlank()) {
                                    IslandBadgeAvatar(
                                        name = event.userName,
                                        avatarUrl = event.userAvatar,
                                        size = 28.dp,
                                        badgeBg = Color(0xFFFF9500),
                                        badgeIcon = if (event.isRemix) Lucide.Sparkles else Lucide.Download
                                    )
                                } else if (event.isRemix) {
                                    AiWaveSphereIcon(modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(Lucide.Download, null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = if (event.isRemix) "${event.userName} remixed ${event.workTitle}" else "${event.userName} downloaded ${event.workTitle}",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IslandActionPill(
                                    label = "View",
                                    containerColor = Color(0x38FF9500),
                                    borderColor = Color(0xFFFF9500).copy(alpha = 0.55f),
                                    textColor = Color(0xFFFF9F0A),
                                    height = 26.dp,
                                    horizontalPadding = 10.dp,
                                    onClick = {
                                        event.onClick?.invoke()
                                        dismissIsland()
                                    }
                                )
                                IslandCloseButton(size = 24.dp, onClick = { dismissIsland() })
                            }
                        }

                        is DynamicIslandEvent.Comment -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IslandBadgeAvatar(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    size = 28.dp,
                                    badgeBg = Color(0xFF0EA5E9),
                                    badgeIcon = Lucide.MessageCircle
                                )
                                Text(
                                    text = "${event.userName}: ${event.commentText.ifBlank { "commented on ${event.targetTitle}" }}",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IslandActionPill(
                                    label = if (event.onReply != null) "Reply" else "View",
                                    containerColor = Color(0x3838BDF8),
                                    borderColor = Color(0xFF38BDF8).copy(alpha = 0.55f),
                                    textColor = Color(0xFF38BDF8),
                                    height = 26.dp,
                                    horizontalPadding = 10.dp,
                                    onClick = {
                                        if (event.onReply != null) {
                                            isExpanded = true
                                            isComposingReply = true
                                        } else {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    }
                                )
                                IslandCloseButton(size = 24.dp, onClick = { dismissIsland() })
                            }
                        }

                        is DynamicIslandEvent.StoryReaction -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IosAvatarView(
                                    name = event.userName,
                                    avatarUrl = event.avatarUrlOrFallback(),
                                    size = 28.dp
                                )
                                Text(
                                    text = if (event.replyText.isNotBlank()) "${event.userName}: ${event.replyText}" else "${event.userName} reacted ${event.reactionEmoji}",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IslandActionPill(
                                    label = if (event.replyText.isNotBlank() && event.onReply != null) "Reply" else "View",
                                    containerColor = Color(0x38FF2D55),
                                    borderColor = Color(0xFFFF2D55).copy(alpha = 0.55f),
                                    textColor = Color(0xFFFF4F70),
                                    height = 26.dp,
                                    horizontalPadding = 10.dp,
                                    onClick = {
                                        if (event.replyText.isNotBlank() && event.onReply != null) {
                                            isExpanded = true
                                            isComposingReply = true
                                        } else {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    }
                                )
                                IslandCloseButton(size = 24.dp, onClick = { dismissIsland() })
                            }
                        }
                    }
                } else {
                    // EXPANDED INTERACTIVE ALERT WITH FULL ACTION BUTTONS
                    when (event) {
                        is DynamicIslandEvent.IncomingCall -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IosAvatarView(
                                    name = event.callerName,
                                    avatarUrl = event.callerAvatar,
                                    size = 38.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = if (event.isVideo) "FaceTime Video" else "Oscellion Audio",
                                        color = if (event.isVideo) Color(0xFF38BDF8) else Color(0xFF34C759),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = event.callerName.ifBlank { "Oscellion User" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (event.isVerified) {
                                            com.chroma.studio.webrtc.VerifiedBadgeSeal(
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                        .clickable {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                                            event.onDecline()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.chroma.studio.webrtc.HandsetIcon(
                                        modifier = Modifier.size(18.dp),
                                        rotationDegrees = 135f
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759))
                                        .clickable {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                                            event.onAccept()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (event.isVideo) Lucide.Video else Lucide.Phone,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        is DynamicIslandEvent.ChatMessage -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.senderName,
                                    avatarUrl = event.avatarUrl,
                                    isVerified = event.isVerified,
                                    subtitle = event.messageText,
                                    metaLabel = event.timeText,
                                    badgeBg = Color(0xFF34C759),
                                    badgeIcon = Lucide.MessageCircle,
                                    onClose = { dismissIsland() }
                                )

                                // Quick Emoji Reactions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("👍", "❤️", "😂", "🔥", "🙌").forEach { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(9999.dp))
                                                .background(Color(0xFF232326))
                                                .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(9999.dp))
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

                                // Inline Reply Composer
                                IslandInlineReplyRow(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = "Reply to ${event.senderName}...",
                                    onSubmit = {
                                        if (replyText.isNotBlank()) {
                                            event.onReply?.invoke(replyText.trim())
                                            replyText = ""
                                            dismissIsland()
                                        }
                                    }
                                )

                                // Required Chat Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.onMarkAsRead != null) {
                                        IslandActionPill(
                                            label = "Mark Read",
                                            icon = Lucide.Check,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color(0xFFE5E5EA),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}
                                                event.onMarkAsRead.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    if (event.onMute != null) {
                                        IslandActionPill(
                                            label = "Mute",
                                            icon = Lucide.BellOff,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color(0xFFD1D1D6),
                                            onClick = {
                                                event.onMute.invoke()
                                                ToastManager.showToast("Muted ${event.senderName}")
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    IslandActionPill(
                                        label = "Open Chat",
                                        icon = Lucide.ArrowUpRight,
                                        containerColor = LiquidGlassTokens.AppleSystemBlue,
                                        textColor = Color.White,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                }
                            }
                        }

                        is DynamicIslandEvent.Like -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    isVerified = event.isVerified,
                                    subtitle = "liked ${event.targetTitle}",
                                    thumbnailUrl = event.targetThumbnail,
                                    badgeBg = Color(0xFFFF2D55),
                                    badgeIcon = ChromaIcons.HeartFilled,
                                    onClose = { dismissIsland() }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IslandActionPill(
                                        label = "View Post",
                                        icon = Lucide.ExternalLink,
                                        containerColor = Color(0x33FF2D55),
                                        borderColor = Color(0xFFFF2D55).copy(alpha = 0.55f),
                                        textColor = Color(0xFFFF4F70),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                    if (event.onViewProfile != null) {
                                        IslandActionPill(
                                            label = "Profile",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                event.onViewProfile.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Follow -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    isVerified = event.isVerified,
                                    subtitle = if (event.isFollowRequest) "requested to follow you" else "started following you",
                                    badgeBg = Color(0xFF0381FE),
                                    badgeIcon = Lucide.UserPlus,
                                    onClose = { dismissIsland() }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.isFollowRequest) {
                                        if (event.onAccept != null) {
                                            IslandActionPill(
                                                label = "Accept",
                                                icon = Lucide.Check,
                                                containerColor = LiquidGlassTokens.AppleSystemBlue,
                                                textColor = Color.White,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    event.onAccept.invoke()
                                                    dismissIsland()
                                                }
                                            )
                                        }
                                        if (event.onDecline != null) {
                                            IslandActionPill(
                                                label = "Decline",
                                                icon = Lucide.X,
                                                containerColor = Color(0x33FF3B30),
                                                borderColor = Color(0xFFFF3B30).copy(alpha = 0.50f),
                                                textColor = Color(0xFFFF6961),
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    event.onDecline.invoke()
                                                    dismissIsland()
                                                }
                                            )
                                        }
                                        IslandActionPill(
                                            label = "Profile",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            onClick = {
                                                event.onClick?.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    } else {
                                        if (event.onFollowBack != null) {
                                            IslandActionPill(
                                                label = if (didFollowBack) "Following" else "Follow Back",
                                                icon = if (didFollowBack) Lucide.UserCheck else Lucide.UserPlus,
                                                containerColor = if (didFollowBack) Color(0x2834C759) else LiquidGlassTokens.AppleSystemBlue,
                                                borderColor = if (didFollowBack) Color(0xFF34C759).copy(alpha = 0.5f) else Color.Transparent,
                                                textColor = if (didFollowBack) Color(0xFF34C759) else Color.White,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    if (!didFollowBack) {
                                                        event.onFollowBack.invoke()
                                                        didFollowBack = true
                                                    } else {
                                                        dismissIsland()
                                                    }
                                                }
                                            )
                                        }
                                        IslandActionPill(
                                            label = "View Profile",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                event.onClick?.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.RemixOrDownload -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    isVerified = event.isVerified,
                                    subtitle = if (event.isRemix) "remixed ${event.workTitle}" else "downloaded ${event.workTitle}",
                                    thumbnailUrl = event.workThumbnail,
                                    badgeBg = Color(0xFFFF9500),
                                    badgeIcon = if (event.isRemix) Lucide.Sparkles else Lucide.Download,
                                    onClose = { dismissIsland() }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IslandActionPill(
                                        label = if (event.isRemix) "View Remix" else "View Work",
                                        icon = Lucide.ExternalLink,
                                        containerColor = Color(0x33FF9500),
                                        borderColor = Color(0xFFFF9500).copy(alpha = 0.55f),
                                        textColor = Color(0xFFFF9F0A),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                    if (event.onViewProfile != null) {
                                        IslandActionPill(
                                            label = "Creator",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                event.onViewProfile.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Comment -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.userName,
                                    avatarUrl = event.userAvatar,
                                    isVerified = event.isVerified,
                                    subtitle = event.commentText.ifBlank { "commented on ${event.targetTitle}" },
                                    metaLabel = if (event.isMention) "Mention" else "Comment",
                                    thumbnailUrl = event.targetThumbnail,
                                    badgeBg = Color(0xFF0EA5E9),
                                    badgeIcon = Lucide.MessageCircle,
                                    onClose = { dismissIsland() }
                                )

                                if (isComposingReply && event.onReply != null) {
                                    IslandInlineReplyRow(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = "Reply to ${event.userName}...",
                                        onSubmit = {
                                            if (replyText.isNotBlank()) {
                                                event.onReply.invoke(replyText.trim())
                                                replyText = ""
                                                isComposingReply = false
                                                dismissIsland()
                                            }
                                        },
                                        onCancel = {
                                            isComposingReply = false
                                            replyText = ""
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.onReply != null && !isComposingReply) {
                                        IslandActionPill(
                                            label = "Reply",
                                            icon = Lucide.CornerUpLeft,
                                            containerColor = LiquidGlassTokens.AppleSystemBlue,
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = { isComposingReply = true }
                                        )
                                    }
                                    IslandActionPill(
                                        label = "View Post",
                                        icon = Lucide.ExternalLink,
                                        containerColor = Color(0x3338BDF8),
                                        borderColor = Color(0xFF38BDF8).copy(alpha = 0.55f),
                                        textColor = Color(0xFF38BDF8),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                    if (event.onViewProfile != null) {
                                        IslandActionPill(
                                            label = "Profile",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            onClick = {
                                                event.onViewProfile.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.StoryReaction -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IslandAlertHeader(
                                    name = event.userName,
                                    avatarUrl = event.avatarUrlOrFallback(),
                                    isVerified = event.isVerified,
                                    subtitle = if (event.replyText.isNotBlank()) "replied: ${event.replyText}" else "reacted ${event.reactionEmoji} to your story",
                                    thumbnailUrl = event.storyThumbnail,
                                    badgeBg = Color(0xFFFF2D55),
                                    badgeIcon = ChromaIcons.HeartFilled,
                                    onClose = { dismissIsland() }
                                )

                                if (isComposingReply && event.onReply != null) {
                                    IslandInlineReplyRow(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = "Message ${event.userName}...",
                                        onSubmit = {
                                            if (replyText.isNotBlank()) {
                                                event.onReply.invoke(replyText.trim())
                                                replyText = ""
                                                isComposingReply = false
                                                dismissIsland()
                                            }
                                        },
                                        onCancel = {
                                            isComposingReply = false
                                            replyText = ""
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.onReply != null && !isComposingReply) {
                                        IslandActionPill(
                                            label = "Reply",
                                            icon = Lucide.MessageCircle,
                                            containerColor = LiquidGlassTokens.AppleSystemBlue,
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = { isComposingReply = true }
                                        )
                                    }
                                    IslandActionPill(
                                        label = "View Story",
                                        icon = Lucide.ExternalLink,
                                        containerColor = Color(0x33FF2D55),
                                        borderColor = Color(0xFFFF2D55).copy(alpha = 0.55f),
                                        textColor = Color(0xFFFF4F70),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            event.onClick?.invoke()
                                            dismissIsland()
                                        }
                                    )
                                    if (event.onViewProfile != null) {
                                        IslandActionPill(
                                            label = "Profile",
                                            icon = Lucide.User,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color.White,
                                            onClick = {
                                                event.onViewProfile.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is DynamicIslandEvent.Status -> {
                            val iconTint = statusIconTint(event.title)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(event.icon ?: Lucide.Check, null, tint = iconTint, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (event.message.isNotBlank()) {
                                            Text(
                                                text = event.message,
                                                color = Color(0xFFD1D1D6),
                                                fontSize = 12.sp,
                                                fontFamily = InterFontFamily,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IslandCloseButton(
                                        size = 28.dp,
                                        onClick = { dismissIsland(invokeEventDismiss = true) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (event.actionLabel != null) {
                                        IslandActionPill(
                                            label = event.actionLabel,
                                            icon = Lucide.ExternalLink,
                                            containerColor = LiquidGlassTokens.AppleSystemBlue,
                                            textColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                event.onAction?.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    if (event.secondaryActionLabel != null) {
                                        IslandActionPill(
                                            label = event.secondaryActionLabel,
                                            icon = Lucide.Check,
                                            containerColor = Color(0xFF252529),
                                            borderColor = Color.White.copy(alpha = 0.12f),
                                            textColor = Color(0xFFE5E5EA),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                event.onSecondaryAction?.invoke()
                                                dismissIsland()
                                            }
                                        )
                                    }
                                    IslandActionPill(
                                        label = "Dismiss",
                                        containerColor = Color(0xFF242426),
                                        borderColor = Color.White.copy(alpha = 0.10f),
                                        textColor = Color(0xFFA1A1AA),
                                        modifier = if (event.actionLabel == null && event.secondaryActionLabel == null) Modifier.weight(1f) else Modifier,
                                        onClick = { dismissIsland(invokeEventDismiss = true) }
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
}

private fun statusIconTint(title: String): Color = when {
    title.contains("copied", ignoreCase = true) ||
        title.contains("saved", ignoreCase = true) ||
        title.contains("success", ignoreCase = true) ||
        title.contains("updated", ignoreCase = true) ||
        title.contains("posted", ignoreCase = true) ||
        title.contains("approved", ignoreCase = true) -> Color(0xFF34C759)
    title.contains("delete", ignoreCase = true) ||
        title.contains("remove", ignoreCase = true) ||
        title.contains("error", ignoreCase = true) ||
        title.contains("failed", ignoreCase = true) -> Color(0xFFFF3B30)
    title.contains("like", ignoreCase = true) -> Color(0xFFFF2D55)
    title.contains("follow", ignoreCase = true) -> Color(0xFF0381FE)
    else -> Color(0xFF5856D6)
}

private fun DynamicIslandEvent.StoryReaction.avatarUrlOrFallback(): String = userAvatar

@Composable
private fun IslandCloseButton(
    size: Dp = 26.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Lucide.X,
            contentDescription = "Dismiss",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size((size.value * 0.54f).dp)
        )
    }
}

@Composable
private fun IslandBadgeAvatar(
    name: String,
    avatarUrl: String,
    size: Dp,
    badgeBg: Color,
    badgeIcon: ImageVector
) {
    Box(modifier = Modifier.size(size)) {
        IosAvatarView(
            name = name,
            avatarUrl = avatarUrl,
            size = size
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((size.value * 0.44f).coerceAtLeast(12f).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeBg,
                modifier = Modifier.size((size.value * 0.38f).coerceAtLeast(10f).dp)
            )
        }
    }
}

@Composable
private fun IslandAlertHeader(
    name: String,
    avatarUrl: String,
    isVerified: Boolean,
    subtitle: String,
    badgeBg: Color,
    badgeIcon: ImageVector,
    metaLabel: String? = null,
    thumbnailUrl: String = "",
    verifiedTone: String? = null,
    userId: String? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IslandBadgeAvatar(
            name = name,
            avatarUrl = avatarUrl,
            size = 38.dp,
            badgeBg = badgeBg,
            badgeIcon = badgeIcon
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isVerified) {
                    VerifiedBadge(size = 13.dp, tone = verifiedTone, userId = userId)
                }
                if (!metaLabel.isNullOrBlank()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = metaLabel,
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = subtitle,
                color = Color(0xFFD1D1D6),
                fontSize = 12.5.sp,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(thumbnailUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )
        }

        IslandCloseButton(size = 28.dp, onClick = onClose)
    }
}

@Composable
private fun IslandActionPill(
    label: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = Color.Transparent,
    height: Dp = 32.dp,
    horizontalPadding: Dp = 10.dp,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(9999.dp)
    Row(
        modifier = modifier
            .height(height)
            .clip(pillShape)
            .background(containerColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(0.8.dp, borderColor, pillShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IslandInlineReplyRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
    onCancel: (() -> Unit)? = null,
    autoFocus: Boolean = true
) {
    val pillShape = RoundedCornerShape(9999.dp)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(90L)
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(pillShape)
                .background(Color(0xFF232326))
                .border(0.8.dp, Color.White.copy(alpha = 0.14f), pillShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontFamily = InterFontFamily
                ),
                singleLine = true,
                cursorBrush = SolidColor(LiquidGlassTokens.AppleSystemBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
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

        if (value.isNotBlank()) {
            IslandActionPill(
                label = "Send",
                icon = Lucide.Send,
                containerColor = LiquidGlassTokens.AppleSystemBlue,
                textColor = Color.White,
                height = 34.dp,
                horizontalPadding = 12.dp,
                onClick = onSubmit
            )
        } else if (onCancel != null) {
            IslandActionPill(
                label = "Cancel",
                containerColor = Color(0xFF252529),
                borderColor = Color.White.copy(alpha = 0.12f),
                textColor = Color(0xFFD1D1D6),
                height = 34.dp,
                horizontalPadding = 10.dp,
                onClick = onCancel
            )
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



