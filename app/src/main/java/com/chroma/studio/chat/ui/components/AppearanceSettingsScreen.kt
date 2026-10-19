package com.chroma.studio.chat.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.*
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.ChatColors
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.AiWaveSphereIcon
import com.composables.icons.lucide.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Apple iOS 18 Design System — Inset Grouped Appearance Settings.
 * Uses Lucide icons with Apple-minimalist squircle icon backgrounds.
 * Authentic AppleSlider (LiquidGlassKnob) and AppleSwitch with jelly spring physics.
 */
enum class AppearanceSubScreen {
    MAIN,
    BUBBLE_STYLES,
    CORNER_GEOMETRY,
    ANIMATIONS,
    VOICE_GLOBE,
    THEME_WALLPAPERS
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  APPLE SYSTEM GRADIENTS (Authentic iOS 18 clean & professional color ramps)
// ─────────────────────────────────────────────────────────────────────────────

object AppleGradients {
    // 🌌 Universal Signature Programmatic Gradient: Layered #f8fafc base with TL_BR linear gradient (#FF006FFF to #FF111C42)
    val Universal = Brush.linearGradient(
        colors = listOf(Color(0xFF006FFF), Color(0xFF111C42)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    val UniversalHorizontal = Brush.horizontalGradient(
        colors = listOf(Color(0xFF006FFF), Color(0xFF111C42))
    )

    // 🍏 Apple System Blue (iMessage / App Store / Safari)
    val Blue = Brush.verticalGradient(listOf(Color(0xFF2997FF), Color(0xFF0071E3)))
    val BlueHorizontal = Brush.horizontalGradient(listOf(Color(0xFF2997FF), Color(0xFF0071E3)))

    // 🍏 Apple System Orange (Clock / Sliders / Reminders)
    val Orange = Brush.verticalGradient(listOf(Color(0xFFFF9F0A), Color(0xFFFF6900)))
    val OrangeHorizontal = Brush.horizontalGradient(listOf(Color(0xFFFF9F0A), Color(0xFFFF6900)))

    // 🍏 Apple System Purple / Indigo (Shortcuts / Podcasts)
    val Purple = Brush.verticalGradient(listOf(Color(0xFF5E5CE6), Color(0xFF4845D2)))
    val PurpleHorizontal = Brush.horizontalGradient(listOf(Color(0xFF5E5CE6), Color(0xFF4845D2)))

    // 🍏 Apple System Green (Messages / Phone / Fitness)
    val Green = Brush.verticalGradient(listOf(Color(0xFF30D158), Color(0xFF248A3D)))
    val GreenHorizontal = Brush.horizontalGradient(listOf(Color(0xFF30D158), Color(0xFF248A3D)))

    // 🍏 Apple System Teal / Cyan (AirDrop / Weather)
    val Teal = Brush.verticalGradient(listOf(Color(0xFF64D2FF), Color(0xFF0091D2)))
    val TealHorizontal = Brush.horizontalGradient(listOf(Color(0xFF64D2FF), Color(0xFF0091D2)))

    // 🍏 Apple System Pink (Music / Health)
    val Pink = Brush.verticalGradient(listOf(Color(0xFFFF375F), Color(0xFFD70035)))

    // 🍏 Apple System Red (Destructive / Reset)
    val Red = Brush.verticalGradient(listOf(Color(0xFFFF453A), Color(0xFFD70015)))

    // 🍏 Apple System Slate / Dark Charcoal (Display / Dark Appearance)
    val Slate = Brush.verticalGradient(listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E)))

    // 🍏 Apple System Silver / Titanium (Milled Metal / Premium)
    val Titanium = Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFF8E8E93)))

    // 🍏 Apple Ultra-Gloss Top Rim (Specular glass bevel)
    val SpecularRim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  LIQUID GLASS KNOB  (Authentic tactile thumb for AppleSlider & AppleSwitch)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassKnob(
    width: Dp,
    height: Dp,
    scale: Float = 1f,
    baseAlpha: Float = 1.0f,
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
            .background(Color.White.copy(alpha = baseAlpha.coerceAtLeast(0.96f)))
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
    ) {
        // Specular top gloss highlight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.95f),
                        1f to Color.White.copy(alpha = 0.0f)
                    )
                )
        )
        // Micro-fine specular inner ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(CircleShape)
                .border(
                    0.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    CircleShape
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  APPLE SWITCH  (iOS-authentic proportions, smooth toggle & haptic feedback)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF0A84FF),
    inactiveTrackColor: Color = if (isSystemInDarkTheme()) Color(0xFF39393D) else Color(0xFFE5E5EA)
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = tween(240),
        label = "trackColor"
    )
    val targetTranslation = when {
        checked && isPressed -> 18f
        checked             -> 20f
        isPressed           -> -2f
        else                -> 0f
    }
    val jellySpring   = spring<Float>(dampingRatio = 0.45f, stiffness = 600f)
    val jellySpringDp = spring<Dp>(dampingRatio = 0.45f, stiffness = 600f)

    val thumbTranslationX by animateFloatAsState(targetTranslation, animationSpec = jellySpring, label = "tx")
    val thumbWidth        by animateDpAsState(if (isPressed) 28.dp else 24.dp, animationSpec = jellySpringDp, label = "tw")

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .pointerInput(checked) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onCheckedChange(!checked) }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(trackColor)
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp)
                .offset(x = thumbTranslationX.dp)
        ) {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 25.dp,
                baseAlpha = 1.0f
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  APPLE SLIDER  (Rock-solid Material 3 engine, custom Apple track & thumb)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    activeBrush: Brush? = null,
    isLight: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isInteracting) {
        if (isInteracting) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val thumbWidth by animateDpAsState(
        targetValue = if (isInteracting) 28.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "thumbWidth"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "thumbScale"
    )

    val inactiveTrackColor = if (isLight) Color(0xFFE5E5EA) else Color(0xFF38383A)
    val defaultActiveBrush = AppleGradients.BlueHorizontal

    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
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
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(inactiveTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(CircleShape)
                        .background(activeBrush ?: defaultActiveBrush)
                )
            }
        },
        thumb = {
            LiquidGlassKnob(
                width = thumbWidth,
                height = 18.dp,
                scale = thumbScale,
                baseAlpha = 1.0f
            )
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  iOS BACK HEADER  (< Settings / < Back  +  centered title  +  Done)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IosSubScreenHeader(
    title: String,
    backLabel: String = "Settings",
    onBack: () -> Unit,
    onDone: (() -> Unit)? = null,
    isLight: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { SoundManager.playReaction(); onBack() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Lucide.ChevronLeft,
                contentDescription = "Back",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = backLabel,
                fontSize = 17.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF007AFF)
            )
        }

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = if (isLight) Color.Black else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (onDone != null) {
            Text(
                text = "Done",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color(0xFF007AFF),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { SoundManager.playReaction(); onDone() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            Spacer(Modifier.width(56.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  LIVE BUBBLE PREVIEW CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveBubblePreviewCard(
    bubbleStyle: BubbleStyleType,
    customCorners: CustomCornerConfig,
    colors: ChatColors,
    modifier: Modifier = Modifier,
    animationStyle: BubbleAnimationStyle = BubbleAnimationStyle.POP_ZOOM,
    animKey: Int = 0,
    testButtonText: String? = null,
    onTestClick: (() -> Unit)? = null
) {
    val outVisual = resolveBubbleVisual(bubbleStyle, isOutgoing = true,  isConsecutive = false, colors = colors, customCorners = customCorners)
    val inVisual  = resolveBubbleVisual(bubbleStyle, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = customCorners)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (colors.isLight) Color(0xFFE5E5EA).copy(alpha = 0.5f) else Color(0xFF1C1C1E).copy(alpha = 0.7f))
            .border(0.5.dp, if (colors.isLight) Color(0xFFD1D1D6) else Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = Color(0xFF8E8E93), letterSpacing = 0.5.sp)
            Text(bubbleStyle.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF007AFF))
        }

        // Incoming
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .shadow(inVisual.elevation, inVisual.shape)
                    .clip(inVisual.shape)
                    .background(brush = inVisual.background)
                    .border(inVisual.borderWidth, inVisual.borderColor, inVisual.shape)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Column {
                    Text("Hey! How does this style look? ✨", fontSize = 14.5.sp, fontFamily = InterFontFamily, color = inVisual.textColor)
                    Spacer(Modifier.height(3.dp))
                    Text("10:41 AM", fontSize = 10.5.sp, fontFamily = InterFontFamily, color = inVisual.timeColor, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        // Outgoing with Interactive Live Animation Replay
        val previewScaleX = remember { Animatable(1f) }
        val previewScaleY = remember { Animatable(1f) }
        val previewAlpha = remember { Animatable(1f) }
        val previewOffsetY = remember { Animatable(0f) }
        val previewOffsetX = remember { Animatable(0f) }
        val previewRotationX = remember { Animatable(0f) }

        LaunchedEffect(animKey, animationStyle) {
            if (animKey == 0) return@LaunchedEffect
            when (animationStyle) {
                BubbleAnimationStyle.POP_ZOOM -> {
                    previewScaleX.snapTo(0.42f)
                    previewScaleY.snapTo(0.28f)
                    previewOffsetY.snapTo(22f)
                    previewAlpha.snapTo(0.35f)
                    launch { previewScaleX.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)) }
                    launch { previewScaleY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
                    launch { previewOffsetY.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)) }
                    previewAlpha.animateTo(1f, tween(140))
                }
                BubbleAnimationStyle.SLIDE_UP_FADE -> {
                    previewScaleX.snapTo(1f)
                    previewScaleY.snapTo(1f)
                    previewOffsetY.snapTo(40f)
                    previewAlpha.snapTo(0f)
                    launch { previewOffsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
                    previewAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }
                BubbleAnimationStyle.ELASTIC_SPRING -> {
                    previewScaleX.snapTo(0.25f)
                    previewScaleY.snapTo(0.25f)
                    previewOffsetY.snapTo(10f)
                    previewAlpha.snapTo(0.1f)
                    launch { previewScaleX.animateTo(1f, spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessLow)) }
                    launch { previewScaleY.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)) }
                    launch { previewOffsetY.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                    previewAlpha.animateTo(1f, tween(120))
                }
                BubbleAnimationStyle.FLIP_IN_3D -> {
                    previewScaleX.snapTo(0.85f)
                    previewScaleY.snapTo(0.85f)
                    previewRotationX.snapTo(45f)
                    previewOffsetY.snapTo(20f)
                    previewAlpha.snapTo(0f)
                    launch { previewRotationX.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
                    launch { previewScaleX.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                    launch { previewScaleY.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                    launch { previewOffsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                    previewAlpha.animateTo(1f, tween(180))
                }
                BubbleAnimationStyle.SLIDE_IN_SIDE -> {
                    previewScaleX.snapTo(0.92f)
                    previewScaleY.snapTo(0.92f)
                    previewOffsetX.snapTo(90f)
                    previewAlpha.snapTo(0f)
                    launch { previewOffsetX.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) }
                    launch { previewScaleX.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                    launch { previewScaleY.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                    previewAlpha.animateTo(1f, tween(160))
                }
                BubbleAnimationStyle.EXPAND_REVEAL -> {
                    previewScaleX.snapTo(0.70f)
                    previewScaleY.snapTo(0.38f)
                    previewOffsetY.snapTo(16f)
                    previewAlpha.snapTo(0.15f)
                    launch { previewScaleX.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)) }
                    launch { previewScaleY.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)) }
                    launch { previewOffsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                    previewAlpha.animateTo(1f, tween(150))
                }
                BubbleAnimationStyle.GENTLE_FADE -> {
                    previewScaleX.snapTo(0.96f)
                    previewScaleY.snapTo(0.96f)
                    previewAlpha.snapTo(0f)
                    launch { previewScaleX.animateTo(1f, tween(260, easing = LinearOutSlowInEasing)) }
                    launch { previewScaleY.animateTo(1f, tween(260, easing = LinearOutSlowInEasing)) }
                    previewAlpha.animateTo(1f, tween(240, easing = LinearOutSlowInEasing))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = previewScaleX.value
                        scaleY = previewScaleY.value
                        alpha = previewAlpha.value
                        translationX = previewOffsetX.value
                        translationY = previewOffsetY.value
                        rotationX = previewRotationX.value
                        transformOrigin = TransformOrigin(0.98f, 1.0f)
                    }
                    .widthIn(max = 240.dp)
                    .shadow(outVisual.elevation, outVisual.shape)
                    .clip(outVisual.shape)
                    .background(brush = outVisual.background)
                    .border(outVisual.borderWidth, outVisual.borderColor, outVisual.shape)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Column {
                    Text("Clean, responsive & ultra fast! 🚀", fontSize = 14.5.sp, fontFamily = InterFontFamily, color = outVisual.textColor)
                    Spacer(Modifier.height(3.dp))
                    Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("10:42 AM", fontSize = 10.5.sp, fontFamily = InterFontFamily, color = outVisual.timeColor)
                        Icon(Lucide.ChevronsRight, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        if (testButtonText != null && onTestClick != null) {
            LiquidGlassSecondaryButton(
                onClick = onTestClick,
                isSmall = true,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(testButtonText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = Color.Black)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  MAIN APPEARANCE SECTIONS (Root screen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainAppearanceSections(
    currentBubbleStyle: BubbleStyleType,
    currentGlobeStyle: VoiceGlobeStyleType,
    accentColor: AccentColorType,
    currentCustomCorners: CustomCornerConfig,
    currentAnimationStyle: BubbleAnimationStyle,
    colors: ChatColors,
    onNavigateToSubScreen: (AppearanceSubScreen) -> Unit,
    onToggleAppearance: () -> Unit,
    onAccentCycle: () -> Unit,
    onResetDefaults: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {

        item {
            LiveBubblePreviewCard(bubbleStyle = currentBubbleStyle, customCorners = currentCustomCorners, colors = colors)
        }

        // ── CHAT STYLING ──────────────────────────────────────────────────────
        item {
            IosSectionHeader("CHAT STYLING")
            IosGroupedCard(isLight = colors.isLight) {

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF007AFF),
                    icon = { Icon(Lucide.MessageCircle, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp)) },
                    title = "Bubble Styles",
                    subtitle = "${currentBubbleStyle.title} • ${currentBubbleStyle.category}",
                    onClick = { SoundManager.playReaction(); onNavigateToSubScreen(AppearanceSubScreen.BUBBLE_STYLES) },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
                IosRowDivider(isLight = colors.isLight)

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFFFF9500),
                    icon = { Icon(Lucide.SlidersHorizontal, null, tint = Color(0xFFFF9500), modifier = Modifier.size(17.dp)) },
                    title = "Corners & Geometry",
                    subtitle = if (currentCustomCorners.customEnabled) "Custom (${currentCustomCorners.topStart.toInt()}dp)" else "Preset defaults",
                    onClick = { SoundManager.playReaction(); onNavigateToSubScreen(AppearanceSubScreen.CORNER_GEOMETRY) },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
                IosRowDivider(isLight = colors.isLight)

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFFAF52DE),
                    icon = { AiWaveSphereIcon(modifier = Modifier.size(18.dp)) },
                    title = "Animations & Physics",
                    subtitle = currentAnimationStyle.title,
                    onClick = { SoundManager.playReaction(); onNavigateToSubScreen(AppearanceSubScreen.ANIMATIONS) },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
                IosRowDivider(isLight = colors.isLight)

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF00C7BE),
                    icon = { Icon(Lucide.Mic, null, tint = Color(0xFF00C7BE), modifier = Modifier.size(17.dp)) },
                    title = "Voice Globe Style",
                    subtitle = currentGlobeStyle.title,
                    onClick = { SoundManager.playReaction(); onNavigateToSubScreen(AppearanceSubScreen.VOICE_GLOBE) },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // ── THEME & DISPLAY ───────────────────────────────────────────────────
        item {
            IosSectionHeader("THEME & DISPLAY")
            IosGroupedCard(isLight = colors.isLight) {

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF34C759),
                    icon = { Icon(Lucide.Image, null, tint = Color(0xFF34C759), modifier = Modifier.size(17.dp)) },
                    title = "Wallpapers & Themes",
                    subtitle = "Backgrounds, accents & dark mode",
                    onClick = { SoundManager.playReaction(); onNavigateToSubScreen(AppearanceSubScreen.THEME_WALLPAPERS) },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
                IosRowDivider(isLight = colors.isLight)

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF8E8E93),
                    icon = { Icon(Lucide.Moon, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(17.dp)) },
                    title = "Dark Appearance",
                    subtitle = if (colors.isLight) "Light Mode active" else "Dark Mode active",
                    trailing = { AppleSwitch(checked = !colors.isLight, onCheckedChange = { onToggleAppearance() }) }
                )
                IosRowDivider(isLight = colors.isLight)

                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFFFF2D55),
                    icon = { Icon(Lucide.Palette, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(17.dp)) },
                    title = "Accent Color",
                    subtitle = accentColor.title,
                    onClick = { SoundManager.playReaction(); onAccentCycle() },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.color)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }

        // ── RESET ─────────────────────────────────────────────────────────────
        item {
            IosSectionHeader("RESET & MAINTENANCE")
            IosGroupedCard(isLight = colors.isLight) {
                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFFFF3B30),
                    icon = { Icon(Lucide.RotateCcw, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(17.dp)) },
                    title = "Reset Appearance Defaults",
                    subtitle = "Restore system theme and classic bubbles",
                    onClick = onResetDefaults,
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  BUBBLE STYLES SUB-SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BubbleStylesSubScreen(
    currentBubbleStyle: BubbleStyleType,
    onSelectBubbleStyle: (BubbleStyleType) -> Unit,
    colors: ChatColors,
    customCorners: CustomCornerConfig
) {
    val haptic = LocalHapticFeedback.current
    var bubbleCategoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Modern UI", "Retro & Geek", "Playful & Art", "Bold & 3D")
    val filtered   = remember(bubbleCategoryFilter) {
        if (bubbleCategoryFilter == "All") BubbleStyleType.values().toList()
        else BubbleStyleType.values().filter { it.category.equals(bubbleCategoryFilter, ignoreCase = true) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            LiveBubblePreviewCard(bubbleStyle = currentBubbleStyle, customCorners = customCorners, colors = colors)
        }

        // Category chips
        item {
            IosSectionHeader("CATEGORY FILTER")
            IosGroupedCard(isLight = colors.isLight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = bubbleCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF007AFF) else (if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)))
                                .clickable { SoundManager.playReaction(); bubbleCategoryFilter = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = InterFontFamily,
                                color = if (selected) Color.White else if (colors.isLight) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                            )
                        }
                    }
                }
            }
        }

        // Style list
        item {
            IosSectionHeader("AVAILABLE STYLES (${filtered.size})")
            IosGroupedCard(isLight = colors.isLight) {
                filtered.forEachIndexed { idx, style ->
                    val isSelected = style == currentBubbleStyle
                    val outV = resolveBubbleVisual(style, isOutgoing = true,  isConsecutive = false, colors = colors, customCorners = customCorners)
                    val inV  = resolveBubbleVisual(style, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = customCorners)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                SoundManager.playReaction()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectBubbleStyle(style)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IosSquircleIcon(color = Color(0xFF007AFF)) {
                                Icon(Lucide.MessageCircle, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = if (colors.isLight) Color.Black else Color.White)
                                Spacer(Modifier.height(1.dp))
                                Text(style.description, fontSize = 12.5.sp, fontFamily = InterFontFamily, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (isSelected) Icon(Lucide.Check, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        // Mini preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF141416))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(inV.shape)
                                    .background(brush = inV.background)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text(style.incomingPreview, fontSize = 11.5.sp, fontFamily = InterFontFamily, color = inV.textColor) }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(outV.shape)
                                    .background(brush = outV.background)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text(style.outgoingPreview, fontSize = 11.5.sp, fontFamily = InterFontFamily, color = outV.textColor) }
                        }
                    }
                    if (idx < filtered.size - 1) IosRowDivider(isLight = colors.isLight)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  CORNER GEOMETRY SUB-SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CornerGeometrySubScreen(
    currentBubbleStyle: BubbleStyleType,
    currentCustomCorners: CustomCornerConfig,
    onUpdateCustomCorners: (CustomCornerConfig) -> Unit,
    colors: ChatColors
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            LiveBubblePreviewCard(bubbleStyle = currentBubbleStyle, customCorners = currentCustomCorners, colors = colors)
        }

        item {
            IosSectionHeader("GENERAL GEOMETRY")
            IosGroupedCard(isLight = colors.isLight) {
                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF007AFF),
                    icon = { Icon(Lucide.Square, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp)) },
                    title = "Enable Custom Corners",
                    subtitle = if (currentCustomCorners.customEnabled) "Custom parameters active" else "Using preset style geometry",
                    trailing = {
                        AppleSwitch(
                            checked = currentCustomCorners.customEnabled,
                            onCheckedChange = { SoundManager.playReaction(); onUpdateCustomCorners(currentCustomCorners.copy(customEnabled = it)) }
                        )
                    }
                )
            }
        }

        if (currentCustomCorners.customEnabled) {
            item {
                IosSectionHeader("RADII & TAPER")
                IosGroupedCard(isLight = colors.isLight) {

                    IosSliderRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFF007AFF),
                        icon = { Icon(Lucide.CornerLeftUp, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp)) },
                        title = "Top Radius",
                        valueDisplay = "${currentCustomCorners.topStart.toInt()} dp",
                        value = currentCustomCorners.topStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(topStart = it, topEnd = it)) },
                        valueRange = 0f..40f
                    )
                    IosRowDivider(isLight = colors.isLight)

                    IosSliderRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFF007AFF),
                        icon = { Icon(Lucide.CornerLeftDown, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp)) },
                        title = "Bottom Radius",
                        valueDisplay = "${currentCustomCorners.bottomStart.toInt()} dp",
                        value = currentCustomCorners.bottomStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomStart = it)) },
                        valueRange = 0f..40f
                    )
                    IosRowDivider(isLight = colors.isLight)

                    IosSliderRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFFFF9500),
                        icon = { Icon(Lucide.Play, null, tint = Color(0xFFFF9500), modifier = Modifier.size(17.dp)) },
                        title = "Pointy Corner Depth",
                        valueDisplay = "${currentCustomCorners.bottomEnd.toInt()} dp",
                        value = currentCustomCorners.bottomEnd,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomEnd = it)) },
                        valueRange = 0f..20f
                    )
                    IosRowDivider(isLight = colors.isLight)

                    IosSettingsRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFF34C759),
                        icon = { Icon(Lucide.ArrowLeftRight, null, tint = Color(0xFF34C759), modifier = Modifier.size(17.dp)) },
                        title = "Apply To Both Sides",
                        subtitle = if (currentCustomCorners.applyToBoth) "Both incoming & outgoing" else "Outgoing only",
                        trailing = {
                            AppleSwitch(
                                checked = currentCustomCorners.applyToBoth,
                                onCheckedChange = { SoundManager.playReaction(); onUpdateCustomCorners(currentCustomCorners.copy(applyToBoth = it)) }
                            )
                        }
                    )
                    IosRowDivider(isLight = colors.isLight)

                    IosSettingsRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFFFF3B30),
                        icon = { Icon(Lucide.RotateCcw, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(17.dp)) },
                        title = "Reset Corner Defaults",
                        subtitle = "Restore 10dp radius, 3dp pointy notch",
                        onClick = { SoundManager.playReaction(); onUpdateCustomCorners(CustomCornerConfig(customEnabled = true)) },
                        trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            item {
                IosSectionHeader("POINTY CORNER SHAPE")
                IosGroupedCard(isLight = colors.isLight) {
                    PointyCornerStyle.values().forEachIndexed { idx, style ->
                        val isSelected = style == currentCustomCorners.pointyCornerStyle
                        IosSettingsRow(
                            isLight = colors.isLight,
                            iconColor = Color(0xFF007AFF),
                            icon = { Icon(Lucide.Share2, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp)) },
                            title = style.title,
                            subtitle = style.description,
                            onClick = { SoundManager.playReaction(); onUpdateCustomCorners(currentCustomCorners.copy(pointyCornerStyle = style)) },
                            trailing = { if (isSelected) Icon(Lucide.Check, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp)) }
                        )
                        if (idx < PointyCornerStyle.values().size - 1) IosRowDivider(isLight = colors.isLight)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  ANIMATIONS SUB-SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimationsSubScreen(
    currentBubbleStyle: BubbleStyleType,
    currentAnimationStyle: BubbleAnimationStyle,
    onSelectAnimationStyle: (BubbleAnimationStyle) -> Unit,
    colors: ChatColors,
    customCorners: CustomCornerConfig
) {
    val haptic = LocalHapticFeedback.current
    var animTestKey by remember { mutableIntStateOf(0) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            LiveBubblePreviewCard(
                bubbleStyle = currentBubbleStyle,
                customCorners = customCorners,
                colors = colors,
                animationStyle = currentAnimationStyle,
                animKey = animTestKey,
                testButtonText = "Tap to Replay Entrance Animation 🔄",
                onTestClick = {
                    SoundManager.playReaction()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    animTestKey++
                }
            )
        }

        item {
            IosSectionHeader("BUBBLE ENTRANCE ANIMATIONS")
            IosGroupedCard(isLight = colors.isLight) {
                BubbleAnimationStyle.values().forEachIndexed { idx, anim ->
                    val isSelected = anim == currentAnimationStyle
                    IosSettingsRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFFAF52DE),
                        icon = { Icon(Lucide.Zap, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(17.dp)) },
                        title = anim.title,
                        subtitle = anim.description,
                        onClick = {
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectAnimationStyle(anim)
                            animTestKey++
                        },
                        trailing = { if (isSelected) Icon(Lucide.Check, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp)) }
                    )
                    if (idx < BubbleAnimationStyle.values().size - 1) IosRowDivider(isLight = colors.isLight)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  VOICE GLOBE SUB-SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VoiceGlobeSubScreen(
    currentGlobeStyle: VoiceGlobeStyleType,
    onSelectGlobeStyle: (VoiceGlobeStyleType) -> Unit,
    colors: ChatColors
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (colors.isLight) Color(0xFFE5E5EA).copy(alpha = 0.5f) else Color(0xFF1C1C1E).copy(alpha = 0.7f))
                    .border(0.5.dp, if (colors.isLight) Color(0xFFD1D1D6) else Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DynamicIslandVoiceOrb(style = currentGlobeStyle, size = 96.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(currentGlobeStyle.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = if (colors.isLight) Color.Black else Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text(currentGlobeStyle.description, fontSize = 13.sp, fontFamily = InterFontFamily, color = Color(0xFF8E8E93))
                }
            }
        }

        item {
            IosSectionHeader("VOICE GLOBE STYLES (${VoiceGlobeStyleType.values().size})")
            IosGroupedCard(isLight = colors.isLight) {
                VoiceGlobeStyleType.values().forEachIndexed { idx, globe ->
                    val isSelected = globe == currentGlobeStyle
                    IosSettingsRow(
                        isLight = colors.isLight,
                        iconColor = Color(0xFF00C7BE),
                        icon = { Icon(Lucide.Activity, null, tint = Color(0xFF00C7BE), modifier = Modifier.size(17.dp)) },
                        title = globe.title,
                        subtitle = globe.description,
                        onClick = {
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectGlobeStyle(globe)
                        },
                        trailing = { if (isSelected) Icon(Lucide.Check, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp)) }
                    )
                    if (idx < VoiceGlobeStyleType.values().size - 1) IosRowDivider(isLight = colors.isLight)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  THEME & WALLPAPERS SUB-SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeWallpapersSubScreen(
    accentColor: AccentColorType,
    colors: ChatColors,
    onToggleAppearance: () -> Unit,
    onAccentCycle: () -> Unit,
    onOpenWallpaperPicker: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            IosSectionHeader("CHAT WALLPAPERS")
            IosGroupedCard(isLight = colors.isLight) {
                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF34C759),
                    icon = { Icon(Lucide.FolderPlus, null, tint = Color(0xFF34C759), modifier = Modifier.size(17.dp)) },
                    title = "Choose Chat Wallpaper",
                    subtitle = "Gallery images or gradient presets",
                    onClick = { SoundManager.playReaction(); onOpenWallpaperPicker() },
                    trailing = { Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp)) }
                )
            }
        }

        item {
            IosSectionHeader("APPEARANCE MODE")
            IosGroupedCard(isLight = colors.isLight) {
                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = Color(0xFF8E8E93),
                    icon = { Icon(Lucide.Moon, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(17.dp)) },
                    title = "Dark Appearance",
                    subtitle = if (colors.isLight) "Light Mode active" else "Dark Mode active",
                    trailing = { AppleSwitch(checked = !colors.isLight, onCheckedChange = { onToggleAppearance() }) }
                )
            }
        }

        item {
            IosSectionHeader("ACCENT COLOR PRESETS")
            IosGroupedCard(isLight = colors.isLight) {
                IosSettingsRow(
                    isLight = colors.isLight,
                    iconColor = accentColor.color,
                    icon = { Icon(Lucide.Palette, null, tint = accentColor.color, modifier = Modifier.size(17.dp)) },
                    title = "Current Accent",
                    subtitle = "${accentColor.title} — tap to cycle",
                    onClick = { SoundManager.playReaction(); onAccentCycle() },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(accentColor.color).border(1.5.dp, Color.White, CircleShape))
                            Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  ROOT APPEARANCE SETTINGS SCREEN  (Scaffold + AnimatedContent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppearanceSettingsScreen(
    currentBubbleStyle: BubbleStyleType,
    currentGlobeStyle: VoiceGlobeStyleType,
    accentColor: AccentColorType,
    currentCustomCorners: CustomCornerConfig,
    onUpdateCustomCorners: (CustomCornerConfig) -> Unit,
    currentAnimationStyle: BubbleAnimationStyle,
    onSelectAnimationStyle: (BubbleAnimationStyle) -> Unit,
    onSelectBubbleStyle: (BubbleStyleType) -> Unit,
    onSelectGlobeStyle: (VoiceGlobeStyleType) -> Unit,
    onToggleAppearance: () -> Unit,
    onAccentCycle: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    var currentSubScreen by remember { mutableStateOf(AppearanceSubScreen.MAIN) }

    BackHandler(enabled = currentSubScreen != AppearanceSubScreen.MAIN) {
        currentSubScreen = AppearanceSubScreen.MAIN
    }

    val screenBg = if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF000000)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                if (currentSubScreen == AppearanceSubScreen.MAIN) {
                    // ── Apple-style large title bar ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Apple Settings gear squircle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(9.dp),
                                        spotColor = Color.Black.copy(alpha = 0.12f),
                                        ambientColor = Color.Black.copy(alpha = 0.06f)
                                    )
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Settings,
                                    contentDescription = "Settings",
                                    tint = if (colors.isLight) Color(0xFF1C1C1E) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Settings",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = if (colors.isLight) Color.Black else Color.White,
                                letterSpacing = (-0.6).sp
                            )
                        }
                        Text(
                            text = "Done",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF007AFF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { SoundManager.playReaction(); onClose() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    val subTitle = when (currentSubScreen) {
                        AppearanceSubScreen.BUBBLE_STYLES    -> "Bubble Styles"
                        AppearanceSubScreen.CORNER_GEOMETRY  -> "Corners & Geometry"
                        AppearanceSubScreen.ANIMATIONS       -> "Animations"
                        AppearanceSubScreen.VOICE_GLOBE      -> "Voice Globe"
                        AppearanceSubScreen.THEME_WALLPAPERS -> "Wallpapers & Theme"
                        else                                 -> "Settings"
                    }
                    IosSubScreenHeader(
                        title = subTitle,
                        backLabel = "Settings",
                        onBack = { currentSubScreen = AppearanceSubScreen.MAIN },
                        onDone = onClose,
                        isLight = colors.isLight
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = currentSubScreen,
                transitionSpec = {
                    if (targetState == AppearanceSubScreen.MAIN)
                        (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300)))
                            .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(250)))
                    else
                        (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300)))
                            .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(250)))
                },
                label = "AppearanceSubScreenTransition"
            ) { subScreen ->
                when (subScreen) {
                    AppearanceSubScreen.MAIN -> MainAppearanceSections(
                        currentBubbleStyle    = currentBubbleStyle,
                        currentGlobeStyle     = currentGlobeStyle,
                        accentColor           = accentColor,
                        currentCustomCorners  = currentCustomCorners,
                        currentAnimationStyle = currentAnimationStyle,
                        colors                = colors,
                        onNavigateToSubScreen = { currentSubScreen = it },
                        onToggleAppearance    = onToggleAppearance,
                        onAccentCycle         = onAccentCycle,
                        onResetDefaults       = {
                            SoundManager.playReaction()
                            onSelectBubbleStyle(BubbleStyleType.ROUNDED)
                            onSelectGlobeStyle(VoiceGlobeStyleType.ORIGINAL_BLUE)
                            onSelectAnimationStyle(BubbleAnimationStyle.POP_ZOOM)
                            onUpdateCustomCorners(CustomCornerConfig())
                        }
                    )
                    AppearanceSubScreen.BUBBLE_STYLES -> BubbleStylesSubScreen(
                        currentBubbleStyle  = currentBubbleStyle,
                        onSelectBubbleStyle = onSelectBubbleStyle,
                        colors              = colors,
                        customCorners       = currentCustomCorners
                    )
                    AppearanceSubScreen.CORNER_GEOMETRY -> CornerGeometrySubScreen(
                        currentBubbleStyle   = currentBubbleStyle,
                        currentCustomCorners = currentCustomCorners,
                        onUpdateCustomCorners = onUpdateCustomCorners,
                        colors               = colors
                    )
                    AppearanceSubScreen.ANIMATIONS -> AnimationsSubScreen(
                        currentBubbleStyle    = currentBubbleStyle,
                        currentAnimationStyle = currentAnimationStyle,
                        onSelectAnimationStyle = onSelectAnimationStyle,
                        colors                = colors,
                        customCorners         = currentCustomCorners
                    )
                    AppearanceSubScreen.VOICE_GLOBE -> VoiceGlobeSubScreen(
                        currentGlobeStyle = currentGlobeStyle,
                        onSelectGlobeStyle = onSelectGlobeStyle,
                        colors            = colors
                    )
                    AppearanceSubScreen.THEME_WALLPAPERS -> ThemeWallpapersSubScreen(
                        accentColor          = accentColor,
                        colors               = colors,
                        onToggleAppearance   = onToggleAppearance,
                        onAccentCycle        = onAccentCycle,
                        onOpenWallpaperPicker = onOpenWallpaperPicker
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  REUSABLE INSET GROUPED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IosSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = InterFontFamily,
        color = Color(0xFF8E8E93),
        letterSpacing = 0.4.sp,
        modifier = modifier.padding(start = 28.dp, top = 20.dp, bottom = 7.dp)
    )
}

@Composable
fun IosGroupedCard(
    isLight: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg      = if (isLight) Color.White else Color(0xFF1C1C1E)
    val borderColor = if (isLight) Color(0xFFE5E5EA).copy(alpha = 0.8f) else Color(0xFF2C2C2E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
fun IosRowDivider(isLight: Boolean, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
        color = if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)
    )
}

/**
 * Apple-style 30dp squircle icon background with thin-outlined Lucide icon.
 * Uses soft translucent tinted squircle background (color.copy(alpha = 0.14f))
 * paired with a vibrant vector Lucide icon in matching color.
 */
@Composable
fun IosSquircleIcon(
    color: Color = Color(0xFF007AFF),
    gradient: Brush? = null,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.5.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) { icon() }
}

/**
 * Standard Apple Inset Grouped row:  [squircle icon] [title + subtitle]  [trailing]
 */
@Composable
fun IosSettingsRow(
    isLight: Boolean,
    iconColor: Color = Color(0xFF007AFF),
    iconBg: Brush? = null,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosSquircleIcon(color = iconColor, gradient = iconBg) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily, color = if (isLight) Color.Black else Color.White)
            if (subtitle != null) {
                Spacer(Modifier.height(1.5.dp))
                Text(subtitle, fontSize = 13.sp, fontFamily = InterFontFamily, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

/**
 * Apple Slider row:  [icon + title + value]  +  [AppleSlider with LiquidGlassKnob]
 */
@Composable
fun IosSliderRow(
    isLight: Boolean,
    iconColor: Color = Color(0xFF007AFF),
    iconBg: Brush? = null,
    activeBrush: Brush? = null,
    icon: @Composable () -> Unit,
    title: String,
    valueDisplay: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IosSquircleIcon(color = iconColor, gradient = iconBg) { icon() }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily, color = if (isLight) Color.Black else Color.White)
                Spacer(Modifier.height(1.5.dp))
                Text("Current: $valueDisplay", fontSize = 13.sp, fontFamily = InterFontFamily, color = Color(0xFF8E8E93))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 20.dp)) {
            AppleSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                activeBrush = activeBrush ?: iconBg,
                isLight = isLight,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  DYNAMIC ISLAND VOICE ORB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DynamicIslandVoiceOrb(
    style: VoiceGlobeStyleType,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")
    val rotation   by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
    val pulseScale by infiniteTransition.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = style.previewGradient))
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.78f)
                .graphicsLayer { rotationZ = rotation }
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(
                    style.previewGradient.getOrElse(0) { Color.Cyan }.copy(alpha = 0.9f),
                    style.previewGradient.getOrElse(1) { Color.Blue }.copy(alpha = 0.4f),
                    style.previewGradient.getOrElse(2) { Color.Magenta }.copy(alpha = 0.9f),
                    style.previewGradient.getOrElse(0) { Color.Cyan }.copy(alpha = 0.9f)
                )))
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.42f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)))
        )
        Icon(Lucide.Activity, null, tint = Color.White, modifier = Modifier.size((size.value * 0.32f).dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun getBubbleStyleGradient(style: BubbleStyleType): Brush = when (style.category.lowercase()) {
    "modern", "modern ui"      -> AppleGradients.Blue
    "retro", "retro & geek"    -> AppleGradients.Green
    "playful", "playful & art" -> AppleGradients.Pink
    "bold", "bold & 3d", "3d"  -> AppleGradients.Orange
    "gradients"                -> AppleGradients.Teal
    else                       -> AppleGradients.Purple
}
