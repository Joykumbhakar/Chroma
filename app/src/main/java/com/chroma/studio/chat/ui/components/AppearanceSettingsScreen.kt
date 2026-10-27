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
 * Uses AppleSettingsHeader, AppleSettingsGroup, AppleSettingNavRow,
 * AppleSettingToggleRow, AppleSettingSliderRow, and AppleSettingDivider
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
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFDFE8F3),
                shape = RoundedCornerShape(18.dp)
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
    onResetDefaults: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
            AppleSettingsGroup(header = "Chat Styling") {
                AppleSettingNavRow(
                    title = "Bubble Styles",
                    subtitle = "${currentBubbleStyle.title} • ${currentBubbleStyle.category}",
                    icon = Lucide.MessageCircle,
                    iconColor = Color(0xFF0A84FF),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.BUBBLE_STYLES)
                    }
                )
                AppleSettingDivider()
                AppleSettingNavRow(
                    title = "Corners & Geometry",
                    subtitle = if (currentCustomCorners.customEnabled) "Custom (${currentCustomCorners.topStart.toInt()}dp)" else "Preset defaults",
                    icon = Lucide.SlidersHorizontal,
                    iconColor = Color(0xFFFF9500),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.CORNER_GEOMETRY)
                    }
                )
                AppleSettingDivider()
                AppleSettingNavRow(
                    title = "Animations & Physics",
                    subtitle = currentAnimationStyle.title,
                    icon = Lucide.Sparkles,
                    iconColor = Color(0xFFAF52DE),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.ANIMATIONS)
                    }
                )
                AppleSettingDivider()
                AppleSettingNavRow(
                    title = "Voice Globe Style",
                    subtitle = currentGlobeStyle.title,
                    icon = Lucide.Mic,
                    iconColor = Color(0xFF00C7BE),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.VOICE_GLOBE)
                    }
                )
            }
        }

        // Section 2: Theme & Display
        item {
            AppleSettingsGroup(header = "Theme & Display") {
                AppleSettingNavRow(
                    title = "Wallpapers & Themes",
                    subtitle = "Backgrounds, accents & dark mode",
                    icon = Lucide.Image,
                    iconColor = Color(0xFF34C759),
                    onClick = {
                        SoundManager.playReaction()
                        onNavigateToSubScreen(AppearanceSubScreen.THEME_WALLPAPERS)
                    }
                )
                AppleSettingDivider()
                AppleSettingToggleRow(
                    title = "Dark Appearance",
                    subtitle = if (colors.isLight) "Light Mode active" else "Dark Mode active",
                    icon = Lucide.Moon,
                    iconColor = Color(0xFF8E8E93),
                    checked = !colors.isLight,
                    onCheckedChange = { onToggleAppearance() }
                )
                AppleSettingDivider()
                AppleSettingNavRow(
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
            AppleSettingsGroup(header = "Reset & Maintenance") {
                AppleSettingNavRow(
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
            AppleSettingsGroup(header = "Category Filter") {
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
            AppleSettingsGroup(header = "Available Styles (${filtered.size})") {
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
                            AppleIconBadge(icon = Lucide.MessageCircle, iconColor = Color(0xFF0A84FF))
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
                        AppleSettingDivider(paddingStart = 50.dp)
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
            AppleSettingsGroup(header = "General Geometry") {
                AppleSettingToggleRow(
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
                AppleSettingsGroup(header = "Radii & Taper") {
                    AppleSettingSliderRow(
                        title = "Top Radius",
                        valueText = "${currentCustomCorners.topStart.toInt()} dp",
                        icon = Lucide.CornerLeftUp,
                        iconColor = Color(0xFF0A84FF),
                        value = currentCustomCorners.topStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(topStart = it, topEnd = it)) },
                        valueRange = 0f..40f
                    )
                    AppleSettingDivider()
                    AppleSettingSliderRow(
                        title = "Bottom Radius",
                        valueText = "${currentCustomCorners.bottomStart.toInt()} dp",
                        icon = Lucide.CornerLeftDown,
                        iconColor = Color(0xFF0A84FF),
                        value = currentCustomCorners.bottomStart,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomStart = it)) },
                        valueRange = 0f..40f
                    )
                    AppleSettingDivider()
                    AppleSettingSliderRow(
                        title = "Pointy Corner Depth",
                        valueText = "${currentCustomCorners.bottomEnd.toInt()} dp",
                        icon = Lucide.Play,
                        iconColor = Color(0xFFFF9500),
                        value = currentCustomCorners.bottomEnd,
                        onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomEnd = it)) },
                        valueRange = 0f..20f
                    )
                    AppleSettingDivider()
                    AppleSettingToggleRow(
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
                    AppleSettingDivider()
                    AppleSettingNavRow(
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
                AppleSettingsGroup(header = "Pointy Corner Shape") {
                    PointyCornerStyle.values().forEachIndexed { idx, style ->
                        val isSelected = style == currentCustomCorners.pointyCornerStyle
                        AppleSettingNavRow(
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
                            AppleSettingDivider()
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
            AppleSettingsGroup(header = "Bubble Entrance Animations") {
                BubbleAnimationStyle.values().forEachIndexed { idx, anim ->
                    val isSelected = anim == currentAnimationStyle
                    AppleSettingNavRow(
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
                        AppleSettingDivider()
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
            AppleSettingsGroup(header = "Voice Globe Preview") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DynamicIslandVoiceOrb(style = currentGlobeStyle, size = 96.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = currentGlobeStyle.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = if (colors.isLight) Color.Black else Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentGlobeStyle.description,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }

        item {
            AppleSettingsGroup(header = "Voice Globe Styles (${VoiceGlobeStyleType.values().size})") {
                VoiceGlobeStyleType.values().forEachIndexed { idx, globe ->
                    val isSelected = globe == currentGlobeStyle
                    AppleSettingNavRow(
                        title = globe.title,
                        subtitle = globe.description,
                        icon = Lucide.Activity,
                        iconColor = Color(0xFF00C7BE),
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
                        AppleSettingDivider()
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
            AppleSettingsGroup(header = "Chat Wallpapers") {
                AppleSettingNavRow(
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
            AppleSettingsGroup(header = "Appearance Mode") {
                AppleSettingToggleRow(
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
            AppleSettingsGroup(header = "Accent Color Presets") {
                AppleSettingNavRow(
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

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF4F7FB)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            AppleSettingsHeader(
                title = when (currentSubScreen) {
                    AppearanceSubScreen.MAIN -> "Appearance"
                    AppearanceSubScreen.BUBBLE_STYLES -> "Bubble Styles"
                    AppearanceSubScreen.CORNER_GEOMETRY -> "Corners & Geometry"
                    AppearanceSubScreen.ANIMATIONS -> "Animations"
                    AppearanceSubScreen.VOICE_GLOBE -> "Voice Globe"
                    AppearanceSubScreen.THEME_WALLPAPERS -> "Wallpapers & Theme"
                },
                useMacSettingsIcon = currentSubScreen == AppearanceSubScreen.MAIN,
                icon = if (currentSubScreen != AppearanceSubScreen.MAIN) Lucide.ChevronLeft else null,
                onBackClick = if (currentSubScreen != AppearanceSubScreen.MAIN) {
                    { currentSubScreen = AppearanceSubScreen.MAIN }
                } else null,
                doneText = "Done",
                onDoneClick = onClose
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        }
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
