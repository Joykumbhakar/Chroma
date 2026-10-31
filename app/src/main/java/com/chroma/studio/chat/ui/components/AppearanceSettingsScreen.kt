package com.chroma.studio.chat.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.*
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.ChatColors
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

/**
 * Apple iOS / macOS Unified Appearance Settings Screen.
 * Uses AppleSettingsHeader, OneUiSettingsGroup, OneUiSettingsNavRow,
 * OneUiSettingsToggleRow, OneUiSettingsSliderRow, and OneUiSettingsDivider
 * to provide 100% parity with AccountCenterScreen (Main Settings).
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
// 🍏  APPLE SYSTEM GRADIENTS
// ─────────────────────────────────────────────────────────────────────────────

object AppleGradients {
    val Universal = Brush.linearGradient(
        colors = listOf(Color(0xFF006FFF), Color(0xFF111C42)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    val UniversalHorizontal = Brush.horizontalGradient(
        colors = listOf(Color(0xFF006FFF), Color(0xFF111C42))
    )
    val Blue = Brush.verticalGradient(listOf(Color(0xFF2997FF), Color(0xFF0071E3)))
    val BlueHorizontal = Brush.horizontalGradient(listOf(Color(0xFF2997FF), Color(0xFF0071E3)))
    val Orange = Brush.verticalGradient(listOf(Color(0xFFFF9F0A), Color(0xFFFF6900)))
    val Purple = Brush.verticalGradient(listOf(Color(0xFF5E5CE6), Color(0xFF4845D2)))
    val Green = Brush.verticalGradient(listOf(Color(0xFF30D158), Color(0xFF248A3D)))
    val Teal = Brush.verticalGradient(listOf(Color(0xFF64D2FF), Color(0xFF0091D2)))
    val Pink = Brush.verticalGradient(listOf(Color(0xFFFF375F), Color(0xFFD70035)))
    val Red = Brush.verticalGradient(listOf(Color(0xFFFF453A), Color(0xFFD70015)))
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  LIVE VOICE BUBBLE PREVIEW
@Composable
fun LiveVoiceBubblePreview(
    isOutgoing: Boolean,
    visual: BubbleVisualConfig,
    colors: ChatColors,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.42f) }

    val defaultHeights = remember {
        listOf(
            22, 38, 65, 95, 70, 40, 85, 100, 75, 45, 90, 80, 55, 30,
            65, 95, 100, 70, 40, 85, 95, 60, 35, 75, 90, 65, 40, 22
        )
    }

    Box(
        modifier = modifier
            .widthIn(min = 210.dp, max = 240.dp)
            .shadow(visual.elevation, visual.shape)
            .clip(visual.shape)
            .background(brush = visual.background)
            .border(visual.borderWidth, visual.borderColor, visual.shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playBtnBg = if (isOutgoing) {
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.16f))
                )
            } else {
                if (colors.isLight) {
                    Brush.linearGradient(
                        listOf(colors.iosBlue.copy(alpha = 0.18f), colors.iosBlue.copy(alpha = 0.08f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E))
                    )
                }
            }
            val playBtnBorder = if (isOutgoing) Color.White.copy(alpha = 0.38f)
            else (if (colors.isLight) colors.iosBlue.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.20f))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(playBtnBg)
                    .border(0.75.dp, playBtnBorder, CircleShape)
                    .clickable {
                        SoundManager.playReaction()
                        isPlaying = !isPlaying
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    LucidePause(
                        tint = if (isOutgoing) Color.White else colors.iosBlue,
                        size = 17.dp
                    )
                } else {
                    LucidePlay(
                        tint = if (isOutgoing) Color.White else colors.iosBlue,
                        size = 17.dp,
                        modifier = Modifier.offset(x = 1.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.5.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth()
                ) {
                    val totalBars = defaultHeights.size
                    val spacingPx = 2.dp.toPx()
                    val totalSpacing = spacingPx * (totalBars - 1)
                    val barWidth = (size.width - totalSpacing) / totalBars
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())

                    for (i in 0 until totalBars) {
                        val h = defaultHeights[i]
                        val barProgress = i.toFloat() / totalBars.toFloat()
                        val isPast = barProgress <= currentProgress

                        val barHeightNorm = (h / 100f).coerceIn(0.18f, 1f)
                        val barPxHeight = size.height * barHeightNorm
                        val left = i * (barWidth + spacingPx)
                        val top = (size.height - barPxHeight) / 2f

                        val barColor = if (isOutgoing) {
                            if (isPast) Color.White else Color.White.copy(alpha = 0.40f)
                        } else {
                            if (isPast) colors.iosBlue else colors.iosBlue.copy(alpha = 0.30f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(barWidth, barPxHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaying) "0:06" else "0:14",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutgoing) Color.White else colors.textDark,
                        fontFamily = InterFontFamily
                    )

                    Spacer(Modifier.width(6.dp))

                    val speedPreviewShape = RoundedCornerShape(9999.dp)
                    Box(
                        modifier = Modifier
                            .clip(speedPreviewShape)
                            .background(if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.surfaceSecondary)
                            .border(
                                0.5.dp,
                                if (isOutgoing) Color.White.copy(alpha = 0.45f) else colors.iosBlue.copy(alpha = 0.25f),
                                speedPreviewShape
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1.5x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutgoing) Color.White else colors.iosBlue,
                            fontFamily = InterFontFamily
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "10:42 AM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = visual.timeColor,
                            fontFamily = InterFontFamily
                        )
                        if (isOutgoing) {
                            Icon(
                                imageVector = Lucide.ChevronsRight,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    val isDark = !colors.isLight
    val outVisual = resolveBubbleVisual(bubbleStyle, isOutgoing = true, isConsecutive = false, colors = colors, customCorners = customCorners)
    val inVisual = resolveBubbleVisual(bubbleStyle, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = customCorners)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFDFE8F3),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color(0xFF8E8E93),
                letterSpacing = 0.5.sp
            )
            Text(
                text = bubbleStyle.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color(0xFF0A84FF)
            )
        }

        // Incoming message bubble
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
                    Text(
                        text = "Hey! How does this style look? ✨",
                        fontSize = 14.5.sp,
                        fontFamily = InterFontFamily,
                        color = inVisual.textColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "10:41 AM",
                        fontSize = 10.5.sp,
                        fontFamily = InterFontFamily,
                        color = inVisual.timeColor,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Outgoing Voice Note bubble matching exact ChatBubble style
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LiveVoiceBubblePreview(
                isOutgoing = true,
                visual = outVisual,
                colors = colors
            )
        }

        // Outgoing message bubble with interactive animation replay
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
                    Text(
                        text = "Clean, responsive & ultra fast! 🚀",
                        fontSize = 14.5.sp,
                        fontFamily = InterFontFamily,
                        color = outVisual.textColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "10:42 AM",
                            fontSize = 10.5.sp,
                            fontFamily = InterFontFamily,
                            color = outVisual.timeColor
                        )
                        Icon(
                            imageVector = Lucide.ChevronsRight,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(12.dp)
                        )
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
                Text(
                    text = testButtonText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = if (isDark) Color.White else Color.Black
                )
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
    onResetDefaults: () -> Unit,
    gradientStyle: OneUiGradientStyle = OneUiGradientStyle.GALAXY_AI,
    onCycleGradient: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {


        // Live Preview Top Card
        item {
            LiveBubblePreviewCard(
                bubbleStyle = currentBubbleStyle,
                customCorners = currentCustomCorners,
                colors = colors
            )
        }

        // Section 1: Chat Styling
        item {
            OneUiSettingsGroup(header = "Chat Styling") {
                OneUiSettingsNavRow(
                    title = "Bubble Styles",
                    subtitle = "${currentBubbleStyle.title} • ${currentBubbleStyle.category}",
                    icon = Lucide.MessageCircle,
                    iconColor = Color(0xFF0A84FF),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.BUBBLE_STYLES)
                    }
                )
                OneUiSettingsDivider()
                OneUiSettingsNavRow(
                    title = "Corners & Geometry",
                    subtitle = if (currentCustomCorners.customEnabled) "Custom (${currentCustomCorners.topStart.toInt()}dp)" else "Preset defaults",
                    icon = Lucide.SlidersHorizontal,
                    iconColor = Color(0xFFFF9500),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.CORNER_GEOMETRY)
                    }
                )
                OneUiSettingsDivider()
                OneUiSettingsNavRow(
                    title = "Animations & Physics",
                    subtitle = currentAnimationStyle.title,
                    icon = Lucide.Sparkles,
                    iconColor = Color(0xFFAF52DE),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.ANIMATIONS)
                    }
                )
                OneUiSettingsDivider()
                OneUiSettingsNavRow(
                    title = "Voice Globe Style",
                    subtitle = currentGlobeStyle.title,
                    icon = Lucide.Mic,
                    iconGradient = currentGlobeStyle.previewGradient,
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.VOICE_GLOBE)
                    }
                )
            }
        }

        // Section 2: Theme & Display
        item {
            OneUiSettingsGroup(header = "Theme & Display") {
                OneUiSettingsNavRow(
                    title = "Wallpapers & Themes",
                    subtitle = "Backgrounds, accents & dark mode",
                    icon = Lucide.Image,
                    iconColor = Color(0xFF34C759),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.THEME_WALLPAPERS)
                    }
                )
                OneUiSettingsDivider()
                OneUiSettingsToggleRow(
                    title = "Dark Appearance",
                    subtitle = if (colors.isLight) "Light Mode active" else "Dark Mode active",
                    icon = Lucide.Moon,
                    iconColor = Color(0xFF8E8E93),
                    checked = !colors.isLight,
                    onCheckedChange = { onToggleAppearance() }
                )
                OneUiSettingsDivider()
                OneUiSettingsNavRow(
                    title = "Accent Color",
                    subtitle = accentColor.title,
                    valueText = accentColor.title,
                    valueTextColor = accentColor.color,
                    icon = Lucide.Palette,
                    iconColor = accentColor.color,
                    onClick = {
                        SoundManager.playReaction()
                        onAccentCycle()
                    }
                )
            }
        }

        // Section 3: Reset & Maintenance
        item {
            OneUiSettingsGroup(header = "Reset & Maintenance") {
                OneUiSettingsNavRow(
                    title = "Reset Appearance Defaults",
                    subtitle = "Restore system theme and classic bubbles",
                    icon = Lucide.RotateCcw,
                    iconColor = Color(0xFFFF3B30),
                    isDestructive = true,
                    onClick = onResetDefaults
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
    val filtered = remember(bubbleCategoryFilter) {
        if (bubbleCategoryFilter == "All") BubbleStyleType.values().toList()
        else BubbleStyleType.values().filter { it.category.equals(bubbleCategoryFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            LiveBubblePreviewCard(
                bubbleStyle = currentBubbleStyle,
                customCorners = customCorners,
                colors = colors
            )
        }

        // Category Filter Chips
        item {
            OneUiSettingsGroup(header = "Category Filter") {
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
                                .background(
                                    if (selected) Color(0xFF0A84FF)
                                    else if (colors.isLight) Color(0xFFE5E5EA)
                                    else Color(0xFF2C2C2E)
                                )
                                .clickable {
                                    SoundManager.playReaction()
                                    bubbleCategoryFilter = cat
                                }
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

        // Available Styles List
        item {
            OneUiSettingsGroup(header = "Available Styles (${filtered.size})") {
                filtered.forEachIndexed { idx, style ->
                    val isSelected = style == currentBubbleStyle
                    val outV = resolveBubbleVisual(style, isOutgoing = true, isConsecutive = false, colors = colors, customCorners = customCorners)
                    val inV = resolveBubbleVisual(style, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = customCorners)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OneUiIconBadge(icon = Lucide.MessageCircle, iconColor = Color(0xFF0A84FF))
                            Spacer(Modifier.width(13.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (colors.isLight) Color.Black else Color.White
                                )
                                Text(
                                    text = style.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0A84FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Mini preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 35.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isLight) Color(0xFFF4F7FB) else Color(0xFF141416))
                                .border(0.5.dp, if (colors.isLight) Color(0xFFDFE8F3) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(inV.shape)
                                    .background(brush = inV.background)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(style.incomingPreview, fontSize = 11.5.sp, color = inV.textColor)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(outV.shape)
                                    .background(brush = outV.background)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(style.outgoingPreview, fontSize = 11.5.sp, color = outV.textColor)
                            }
                        }
                    }
                    if (idx < filtered.size - 1) {
                        OneUiSettingsDivider(paddingStart = 50.dp)
                    }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            LiveBubblePreviewCard(
                bubbleStyle = currentBubbleStyle,
                customCorners = currentCustomCorners,
                colors = colors
            )
        }

        item {
            OneUiSettingsGroup(header = "General Geometry") {
                OneUiSettingsToggleRow(
                    title = "Enable Custom Corners",
                    subtitle = if (currentCustomCorners.customEnabled) "Custom parameters active" else "Using preset style geometry",
                    icon = Lucide.Square,
                    iconColor = Color(0xFF0A84FF),
                    checked = currentCustomCorners.customEnabled,
                    onCheckedChange = {
                        SoundManager.playReaction()
                        onUpdateCustomCorners(currentCustomCorners.copy(customEnabled = it))
                    }
                )
            }
        }

        if (currentCustomCorners.customEnabled) {
            item {
                OneUiSettingsGroup(header = "Radii & Taper") {
                    OneUiSettingsSliderRow(
                        title = "Top Radius",
                        valueText = "${currentCustomCorners.topStart.toInt()} dp",
                        icon = Lucide.CornerLeftUp,
                        iconColor = Color(0xFF0A84FF),
                        value = currentCustomCorners.topStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(topStart = it, topEnd = it)) },
                        valueRange = 0f..40f
                    )
                    OneUiSettingsDivider()
                    OneUiSettingsSliderRow(
                        title = "Bottom Radius",
                        valueText = "${currentCustomCorners.bottomStart.toInt()} dp",
                        icon = Lucide.CornerLeftDown,
                        iconColor = Color(0xFF0A84FF),
                        value = currentCustomCorners.bottomStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomStart = it)) },
                        valueRange = 0f..40f
                    )
                    OneUiSettingsDivider()
                    OneUiSettingsSliderRow(
                        title = "Pointy Corner Depth",
                        valueText = "${currentCustomCorners.bottomEnd.toInt()} dp",
                        icon = Lucide.Play,
                        iconColor = Color(0xFFFF9500),
                        value = currentCustomCorners.bottomEnd,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomEnd = it)) },
                        valueRange = 0f..20f
                    )
                    OneUiSettingsDivider()
                    OneUiSettingsToggleRow(
                        title = "Apply To Both Sides",
                        subtitle = if (currentCustomCorners.applyToBoth) "Both incoming & outgoing" else "Outgoing only",
                        icon = Lucide.ArrowLeftRight,
                        iconColor = Color(0xFF34C759),
                        checked = currentCustomCorners.applyToBoth,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            onUpdateCustomCorners(currentCustomCorners.copy(applyToBoth = it))
                        }
                    )
                    OneUiSettingsDivider()
                    OneUiSettingsNavRow(
                        title = "Reset Corner Defaults",
                        subtitle = "Restore 10dp radius, 3dp pointy notch",
                        icon = Lucide.RotateCcw,
                        iconColor = Color(0xFFFF3B30),
                        isDestructive = true,
                        onClick = {
                            SoundManager.playReaction()
                            onUpdateCustomCorners(CustomCornerConfig(customEnabled = true))
                        }
                    )
                }
            }

            item {
                OneUiSettingsGroup(header = "Pointy Corner Shape") {
                    PointyCornerStyle.values().forEachIndexed { idx, style ->
                        val isSelected = style == currentCustomCorners.pointyCornerStyle
                        OneUiSettingsNavRow(
                            title = style.title,
                            subtitle = style.description,
                            icon = Lucide.Share2,
                            iconColor = Color(0xFF0A84FF),
                            showChevron = false,
                            valueText = if (isSelected) "✓" else null,
                            valueTextColor = Color(0xFF0A84FF),
                            onClick = {
                                SoundManager.playReaction()
                                onUpdateCustomCorners(currentCustomCorners.copy(pointyCornerStyle = style))
                            }
                        )
                        if (idx < PointyCornerStyle.values().size - 1) {
                            OneUiSettingsDivider()
                        }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
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
            OneUiSettingsGroup(header = "Bubble Entrance Animations") {
                BubbleAnimationStyle.values().forEachIndexed { idx, anim ->
                    val isSelected = anim == currentAnimationStyle
                    OneUiSettingsNavRow(
                        title = anim.title,
                        subtitle = anim.description,
                        icon = Lucide.Zap,
                        iconColor = Color(0xFFAF52DE),
                        showChevron = false,
                        valueText = if (isSelected) "✓" else null,
                        valueTextColor = Color(0xFF0A84FF),
                        onClick = {
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectAnimationStyle(anim)
                            animTestKey++
                        }
                    )
                    if (idx < BubbleAnimationStyle.values().size - 1) {
                        OneUiSettingsDivider()
                    }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OneUiSettingsGroup(header = "3D Liquid Voice Bubble Preview") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VoiceRecorderCard(
                        recordingDurationSec = 14,
                        statusText = "Listening...",
                        globeStyle = currentGlobeStyle,
                        onCancel = {},
                        onTranscribe = {},
                        onSend = {},
                        modifier = Modifier.scale(0.92f)
                    )
                }
            }
        }

        item {
            OneUiSettingsGroup(header = "Voice Globe Styles (${VoiceGlobeStyleType.values().size})") {
                VoiceGlobeStyleType.values().forEachIndexed { idx, globe ->
                    val isSelected = globe == currentGlobeStyle
                    OneUiSettingsNavRow(
                        title = globe.title,
                        subtitle = globe.description,
                        icon = Lucide.Activity,
                        iconGradient = globe.previewGradient,
                        showChevron = false,
                        valueText = if (isSelected) "✓" else null,
                        valueTextColor = Color(0xFF0A84FF),
                        onClick = {
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectGlobeStyle(globe)
                        }
                    )
                    if (idx < VoiceGlobeStyleType.values().size - 1) {
                        OneUiSettingsDivider()
                    }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OneUiSettingsGroup(header = "Chat Wallpapers") {
                OneUiSettingsNavRow(
                    title = "Choose Chat Wallpaper",
                    subtitle = "Gallery images or gradient presets",
                    icon = Lucide.FolderPlus,
                    iconColor = Color(0xFF34C759),
                    onClick = {
                        SoundManager.playReaction()
                        onOpenWallpaperPicker()
                    }
                )
            }
        }

        item {
            OneUiSettingsGroup(header = "Appearance Mode") {
                OneUiSettingsToggleRow(
                    title = "Dark Appearance",
                    subtitle = if (colors.isLight) "Light Mode active" else "Dark Mode active",
                    icon = Lucide.Moon,
                    iconColor = Color(0xFF8E8E93),
                    checked = !colors.isLight,
                    onCheckedChange = { onToggleAppearance() }
                )
            }
        }

        item {
            OneUiSettingsGroup(header = "Accent Color Presets") {
                OneUiSettingsNavRow(
                    title = "Current Accent",
                    subtitle = "${accentColor.title} — tap to cycle",
                    icon = Lucide.Palette,
                    iconColor = accentColor.color,
                    valueText = accentColor.title,
                    valueTextColor = accentColor.color,
                    onClick = {
                        SoundManager.playReaction()
                        onAccentCycle()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 🍏  ROOT APPEARANCE SETTINGS SCREEN
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
    val isDark = !colors.isLight
    var currentSubScreen by remember { mutableStateOf(AppearanceSubScreen.MAIN) }

    BackHandler(enabled = currentSubScreen != AppearanceSubScreen.MAIN) {
        currentSubScreen = AppearanceSubScreen.MAIN
    }

    val (oneUiGradient, cycleOneUiGradient) = rememberOneUiGradientStyle()

    val screenTitle = when (currentSubScreen) {
        AppearanceSubScreen.MAIN -> "Appearance"
        AppearanceSubScreen.BUBBLE_STYLES -> "Bubble Styles"
        AppearanceSubScreen.CORNER_GEOMETRY -> "Corners & Geometry"
        AppearanceSubScreen.ANIMATIONS -> "Animations"
        AppearanceSubScreen.VOICE_GLOBE -> "Voice Globe"
        AppearanceSubScreen.THEME_WALLPAPERS -> "Wallpapers & Themes"
    }

    val screenSubtitle = when (currentSubScreen) {
        AppearanceSubScreen.MAIN -> "Customise chat bubbles, geometry and themes"
        AppearanceSubScreen.BUBBLE_STYLES -> "Choose bubble shape, outlines and styles"
        AppearanceSubScreen.CORNER_GEOMETRY -> "Fine-tune corner radius and physics"
        AppearanceSubScreen.ANIMATIONS -> "Motion curves and message physics"
        AppearanceSubScreen.VOICE_GLOBE -> "Select your voice dynamic orb theme"
        AppearanceSubScreen.THEME_WALLPAPERS -> "Backgrounds, accents & dark mode"
    }

    OneUiCollapsingHeaderLayout(
        title = screenTitle,
        subtitle = screenSubtitle,
        modifier = modifier.fillMaxSize(),
        onBack = if (currentSubScreen != AppearanceSubScreen.MAIN) {
            { currentSubScreen = AppearanceSubScreen.MAIN }
        } else onClose,
        headerActions = { compactTitleAlpha ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isDark) Color.Black.copy(0.06f) else Color.White.copy(0.10f))
                        .clickable { cycleOneUiGradient() }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (oneUiGradient == OneUiGradientStyle.SOLID) Color(0xFF007AFF)
                                    else oneUiGradient.orbColor
                                )
                        )
                        Text(
                            text = oneUiGradient.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
                        )
                    }
                }

                Text(
                    text = "Done",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (oneUiGradient == OneUiGradientStyle.SOLID) Color(0xFF007AFF) else oneUiGradient.primaryColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            SoundManager.playReaction()
                            onClose()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    ) { collapseProgress ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        currentBubbleStyle = currentBubbleStyle,
                        currentGlobeStyle = currentGlobeStyle,
                        accentColor = accentColor,
                        currentCustomCorners = currentCustomCorners,
                        currentAnimationStyle = currentAnimationStyle,
                        colors = colors,
                        onNavigateToSubScreen = { currentSubScreen = it },
                        onToggleAppearance = onToggleAppearance,
                        onAccentCycle = onAccentCycle,
                        onResetDefaults = {
                            SoundManager.playReaction()
                            onSelectBubbleStyle(BubbleStyleType.ROUNDED)
                            onSelectGlobeStyle(VoiceGlobeStyleType.ORIGINAL_BLUE)
                            onSelectAnimationStyle(BubbleAnimationStyle.POP_ZOOM)
                            onUpdateCustomCorners(CustomCornerConfig())
                        },
                        gradientStyle = oneUiGradient,
                        onCycleGradient = cycleOneUiGradient
                    )
                    AppearanceSubScreen.BUBBLE_STYLES -> BubbleStylesSubScreen(
                        currentBubbleStyle = currentBubbleStyle,
                        onSelectBubbleStyle = onSelectBubbleStyle,
                        colors = colors,
                        customCorners = currentCustomCorners
                    )
                    AppearanceSubScreen.CORNER_GEOMETRY -> CornerGeometrySubScreen(
                        currentBubbleStyle = currentBubbleStyle,
                        currentCustomCorners = currentCustomCorners,
                        onUpdateCustomCorners = onUpdateCustomCorners,
                        colors = colors
                    )
                    AppearanceSubScreen.ANIMATIONS -> AnimationsSubScreen(
                        currentBubbleStyle = currentBubbleStyle,
                        currentAnimationStyle = currentAnimationStyle,
                        onSelectAnimationStyle = onSelectAnimationStyle,
                        colors = colors,
                        customCorners = currentCustomCorners
                    )
                    AppearanceSubScreen.VOICE_GLOBE -> VoiceGlobeSubScreen(
                        currentGlobeStyle = currentGlobeStyle,
                        onSelectGlobeStyle = onSelectGlobeStyle,
                        colors = colors
                    )
                    AppearanceSubScreen.THEME_WALLPAPERS -> ThemeWallpapersSubScreen(
                        accentColor = accentColor,
                        colors = colors,
                        onToggleAppearance = onToggleAppearance,
                        onAccentCycle = onAccentCycle,
                        onOpenWallpaperPicker = onOpenWallpaperPicker
                    )
                }
            }
        }
    }
}

// 🍏  DYNAMIC ISLAND VOICE ORB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DynamicIslandVoiceOrb(
    style: VoiceGlobeStyleType,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
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
                .background(
                    Brush.sweepGradient(
                        listOf(
                            style.previewGradient.getOrElse(0) { Color.Cyan }.copy(alpha = 0.9f),
                            style.previewGradient.getOrElse(1) { Color.Blue }.copy(alpha = 0.4f),
                            style.previewGradient.getOrElse(2) { Color.Magenta }.copy(alpha = 0.9f),
                            style.previewGradient.getOrElse(0) { Color.Cyan }.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.42f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)))
        )
        Icon(
            imageVector = Lucide.Activity,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size.value * 0.32f).dp)
        )
    }
}
