package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.chroma.studio.chat.sound.SoundManager
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.chat.data.ChatFileItem
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.chat.data.PollData
import com.chroma.studio.chat.media.AudioPlayerManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CutCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import com.chroma.studio.chat.data.BubbleAnimationStyle
import com.chroma.studio.chat.data.BubbleStyleType
import com.chroma.studio.chat.data.CustomCornerConfig
import com.chroma.studio.chat.data.PointyCornerStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    searchQuery: String,
    senderAvatar: String,
    senderDisplayName: String?,
    showSenderName: Boolean,
    onReactionClick: (String) -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onReplyAction: () -> Unit,
    onEditAction: () -> Unit,
    onPinAction: () -> Unit,
    onCopyAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onImageClick: (String) -> Unit,
    onImageStackClick: ((List<String>, Int) -> Unit)? = null,
    onPollVote: (Int) -> Unit,
    onViewPollVotes: ((PollData) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    bubbleStyle: BubbleStyleType = BubbleStyleType.CLASSIC_IOS,
    customCornerConfig: CustomCornerConfig = CustomCornerConfig(),
    animationStyle: BubbleAnimationStyle = BubbleAnimationStyle.POP_ZOOM,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    var showReactionDock by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    // Only animate entrance for newly sent or received messages (< 4 sec)
    // Historical messages on open render instantly at scale=1, alpha=1 to eliminate lag and prevent OOM
    val isFreshMessage = remember(message.id, message.timestamp) {
        val now = System.currentTimeMillis()
        message.timestamp > 0L && (now - message.timestamp) < 4000L
    }

    // Message Bubble Entrance Animation Engine
    val animScale = remember { Animatable(if (!isFreshMessage) 1f else if (animationStyle == BubbleAnimationStyle.POP_ZOOM) 0.45f else if (animationStyle == BubbleAnimationStyle.ELASTIC_SPRING) 0.2f else if (animationStyle == BubbleAnimationStyle.EXPAND_REVEAL) 0.7f else 1f) }
    val animAlpha = remember { Animatable(if (!isFreshMessage) 1f else if (animationStyle == BubbleAnimationStyle.POP_ZOOM) 0.4f else 0f) }
    val animOffsetY = remember { Animatable(if (!isFreshMessage) 0f else if (animationStyle == BubbleAnimationStyle.SLIDE_UP_FADE) 35f else if (animationStyle == BubbleAnimationStyle.EXPAND_REVEAL) 15f else 0f) }
    val animOffsetX = remember { Animatable(if (!isFreshMessage) 0f else if (animationStyle == BubbleAnimationStyle.SLIDE_IN_SIDE) (if (isOutgoing) 70f else -70f) else 0f) }
    val animRotationX = remember { Animatable(if (!isFreshMessage) 0f else if (animationStyle == BubbleAnimationStyle.FLIP_IN_3D) 35f else 0f) }

    LaunchedEffect(message.id, animationStyle) {
        if (!isFreshMessage) return@LaunchedEffect
        when (animationStyle) {
            BubbleAnimationStyle.POP_ZOOM -> {
                animScale.snapTo(0.45f)
                animAlpha.snapTo(0.4f)
                animScale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)
                )
                animAlpha.animateTo(1f, animationSpec = tween(150))
            }
            BubbleAnimationStyle.SLIDE_UP_FADE -> {
                animOffsetY.snapTo(35f)
                animAlpha.snapTo(0f)
                animOffsetY.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                )
                animAlpha.animateTo(1f, animationSpec = tween(200))
            }
            BubbleAnimationStyle.ELASTIC_SPRING -> {
                animScale.snapTo(0.2f)
                animAlpha.snapTo(0f)
                animScale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessLow)
                )
                animAlpha.animateTo(1f, animationSpec = tween(120))
            }
            BubbleAnimationStyle.FLIP_IN_3D -> {
                animRotationX.snapTo(35f)
                animAlpha.snapTo(0f)
                animRotationX.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
                )
                animAlpha.animateTo(1f, animationSpec = tween(220))
            }
            BubbleAnimationStyle.SLIDE_IN_SIDE -> {
                animOffsetX.snapTo(if (isOutgoing) 70f else -70f)
                animAlpha.snapTo(0f)
                animOffsetX.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                )
                animAlpha.animateTo(1f, animationSpec = tween(180))
            }
            BubbleAnimationStyle.EXPAND_REVEAL -> {
                animScale.snapTo(0.7f)
                animOffsetY.snapTo(15f)
                animAlpha.snapTo(0f)
                animScale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                )
                animOffsetY.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                )
                animAlpha.animateTo(1f, animationSpec = tween(160))
            }
            BubbleAnimationStyle.GENTLE_FADE -> {
                animAlpha.snapTo(0f)
                animAlpha.animateTo(1f, animationSpec = tween(250, easing = LinearOutSlowInEasing))
            }
        }
    }

    // Floating heart scale animation for double tap
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(1f) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            heartScale.snapTo(0.2f)
            heartAlpha.snapTo(1f)
            heartScale.animateTo(1.35f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            heartAlpha.animateTo(0f, animationSpec = tween(300))
            showHeartBurst = false
        }
    }

    // System message (e.g. Call ended)
    if (message.mediaType == "call") {
        Box(
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.cardBg)
                    .border(0.5.dp, colors.borderSubtle, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                LucidePhone(tint = colors.iosBlue, size = 13.dp)
                Text(
                    text = "${message.callType ?: "Audio"} call ended • ${message.callDuration ?: "0m"}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted
                )
            }
        }
        return
    }

    val rowBgColor by animateColorAsState(
        targetValue = if (isSelected) colors.iosBlue.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rowBg"
    )
    val animatedStartPadding by animateDpAsState(
        targetValue = if (isSelectionMode) 12.dp else (if (isOutgoing) 44.dp else 8.dp),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "startPad"
    )
    val animatedEndPadding by animateDpAsState(
        targetValue = if (isOutgoing) 8.dp else (if (isSelectionMode) 8.dp else 44.dp),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "endPad"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBgColor)
            .clickable(enabled = isSelectionMode) {
                onToggleSelect()
            }
            .padding(
                start = animatedStartPadding,
                end = animatedEndPadding,
                top = if (isConsecutive) 2.dp else 6.dp,
                bottom = if (message.reactions.isNotEmpty()) 4.dp else 2.dp
            ),
        verticalAlignment = if (isSelectionMode) Alignment.CenterVertically else Alignment.Bottom
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = expandHorizontally(
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
                expandFrom = Alignment.Start
            ) + fadeIn(animationSpec = tween(180)),
            exit = shrinkHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                shrinkTowards = Alignment.Start
            ) + fadeOut(animationSpec = tween(150))
        ) {
            val checkScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "checkScale"
            )
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.iosBlue else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) Color.Transparent else colors.textMuted.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable {
                        onToggleSelect()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected || checkScale > 0.05f) {
                    Box(modifier = Modifier.graphicsLayer { scaleX = checkScale; scaleY = checkScale }) {
                        LucideCheck(tint = Color.White, size = 13.dp)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f, fill = isSelectionMode),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            // Apple iOS Reaction Dock (0.5s Hold Pop-up IN, Reacted Pop-up OUT)
            AnimatedVisibility(
                visible = showReactionDock,
                enter = scaleIn(
                    initialScale = 0.35f,
                    transformOrigin = TransformOrigin(if (isOutgoing) 0.85f else 0.15f, 1f),
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f)
                ) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f)
                ) + fadeIn(animationSpec = tween(130)),
                exit = scaleOut(
                    targetScale = 0.35f,
                    transformOrigin = TransformOrigin(if (isOutgoing) 0.85f else 0.15f, 1f),
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 480f)
                ) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(110)
                ) + fadeOut(animationSpec = tween(110))
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = Color.Black.copy(alpha = 0.25f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF82C2C2E))
                        .border(
                            0.5.dp,
                            if (colors.isLight) Color(0x22000000) else Color(0x38FFFFFF),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val quickEmojis = listOf("❤️", "👍", "👎", "😂", "‼️", "🔥")
                    quickEmojis.forEach { emoji ->
                        val isSelected = message.userReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.iosBlueSubtle else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) colors.iosBlue else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        SoundManager.playReaction()
                                        showReactionDock = false // Triggers animated scaleOut + fadeOut pop-up OUT
                                        kotlinx.coroutines.delay(120L)
                                        onReactionClick(emoji)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (emoji == "❤️") {
                                Image(
                                    painter = painterResource(id = R.drawable.heart_like),
                                    contentDescription = "Heart",
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    // '+' Icon right next to reactions to open Message Info / Context Menu
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (colors.isLight) Color(0x14000000) else Color(0x28FFFFFF))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundManager.playReaction()
                                showReactionDock = false
                                onLongPress()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorPlusIcon(tint = colors.textDark, size = 16.dp)
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(18.dp)
                            .background(colors.separator)
                    )

                    IconButton(
                        onClick = {
                            onReplyAction()
                            showReactionDock = false
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        LucideReply(tint = colors.iosBlue, size = 16.dp)
                    }
                }
            }

        // Swipe-to-reply Drag Row
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Reply icon revealed on swipe
            if (dragOffset.value != 0f) {
                val isSwipingLeft = dragOffset.value < 0
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (isSwipingLeft) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.iosBlue.copy(alpha = (kotlin.math.abs(dragOffset.value) / 120f).coerceIn(0.2f, 1f))),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideReply(tint = Color.White, size = 15.dp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (kotlin.math.abs(dragOffset.value) > 75f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onReplyAction()
                                }
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, animationSpec = spring(dampingRatio = 0.75f))
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val target = (dragOffset.value + dragAmount * 0.65f).coerceIn(-140f, 140f)
                                coroutineScope.launch {
                                    dragOffset.snapTo(target)
                                }
                            }
                        )
                    },
                horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Incoming Avatar (Full Circular Shape)
                if (!isOutgoing) {
                    if (!isConsecutive) {
                        AsyncImage(
                            model = senderAvatar,
                            contentDescription = "Sender",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Spacer(Modifier.width(36.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Message Bubble Container
                val isVoiceOnly = message.mediaType == "voice" && message.text.isNullOrEmpty()
                val isMediaOnly = (message.mediaType in listOf("image", "image_stack")) && message.text.isNullOrEmpty() && message.replyTo == null
                Column(
                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
                    modifier = if (isMediaOnly) Modifier.width(280.dp) else if (isVoiceOnly) Modifier.widthIn(max = 245.dp) else Modifier.widthIn(max = 295.dp)
                ) {
                    // Sender name in group chat
                    if (!isOutgoing && showSenderName && !isConsecutive && senderDisplayName != null) {
                        Text(
                            text = senderDisplayName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    // Resolve visual styling config for the selected BubbleStyleType & custom corners
                    val visual = remember(bubbleStyle, customCornerConfig, isOutgoing, isConsecutive, colors.isLight, colors.iosBlue) {
                        resolveBubbleVisual(bubbleStyle, isOutgoing, isConsecutive, colors, customCornerConfig)
                    }

                    Box(
                        contentAlignment = if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart
                    ) {
                        val bubbleBackground = if (isMediaOnly) SolidColor(Color.Transparent) else visual.background
                        val bubbleBorderWidth = if (isMediaOnly) 0.5.dp else visual.borderWidth
                        val bubbleBorderColor = if (isMediaOnly) (if (colors.isLight) Color(0x18000000) else Color(0x2EFFFFFF)) else visual.borderColor

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = animScale.value
                                    scaleY = animScale.value
                                    alpha = animAlpha.value
                                    translationX = animOffsetX.value
                                    translationY = animOffsetY.value
                                    rotationX = animRotationX.value
                                    transformOrigin = TransformOrigin(if (isOutgoing) 0.9f else 0.1f, 0.5f)
                                }
                                .then(
                                    if (visual.elevation > 0.dp && !isMediaOnly) {
                                        Modifier.shadow(
                                            elevation = visual.elevation,
                                            shape = visual.shape,
                                            ambientColor = visual.ambientColor,
                                            spotColor = visual.spotColor,
                                            clip = false
                                        )
                                    } else Modifier
                                )
                                .clip(visual.shape)
                                .background(brush = bubbleBackground)
                                .then(
                                    if (visual.borderBrush != null && !isMediaOnly) {
                                        Modifier.border(bubbleBorderWidth, visual.borderBrush, visual.shape)
                                    } else {
                                        Modifier.border(bubbleBorderWidth, bubbleBorderColor, visual.shape)
                                    }
                                )
                                .pointerInput(message.id, isSelectionMode) {
                                    if (isSelectionMode) {
                                        detectTapGestures(
                                            onTap = { onToggleSelect() }
                                        )
                                    } else {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showHeartBurst = true
                                                onDoubleTap()
                                            },
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                SoundManager.playReaction()
                                                showReactionDock = true
                                            },
                                            onTap = {
                                                if (showReactionDock) {
                                                    showReactionDock = false
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                        // Glossy top specular light gradient highlight
                        if (visual.hasTopSpecular && !isMediaOnly) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(visual.shape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                if (isOutgoing) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.40f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = 32f
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier.padding(
                                horizontal = if (isMediaOnly) 0.dp else if (isVoiceOnly) 8.dp else 11.dp,
                                vertical = if (isMediaOnly) 0.dp else if (isVoiceOnly) 5.dp else 7.dp
                            )
                        ) {
                            // 1. Reply Quote Header
                            if (message.replyTo != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isOutgoing) Color.Black.copy(alpha = 0.18f) else Color(0x12000000))
                                        .border(
                                            width = 1.dp,
                                            color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else colors.iosBlue,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = message.replyTo.author,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutgoing) Color.White else colors.iosBlue
                                        )
                                        Text(
                                            text = message.replyTo.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else colors.textDark,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // 2. Media Content
                            when (message.mediaType) {
                                "image", "image_stack" -> {
                                    val urls = if (message.mediaUrls.isNotEmpty()) message.mediaUrls else listOfNotNull(message.mediaUrl)
                                    if (urls.size > 1) {
                                        MediaStackGrid(
                                            mediaUrls = urls,
                                            isOutgoing = isOutgoing,
                                            isMediaOnly = isMediaOnly,
                                            time = message.time,
                                            status = message.status,
                                            onImageClick = { idx ->
                                                if (onImageStackClick != null) onImageStackClick(urls, idx)
                                                else onImageClick(urls.getOrElse(idx) { urls.first() })
                                            }
                                        )
                                        if (!isMediaOnly) Spacer(Modifier.height(4.dp))
                                    } else if (urls.isNotEmpty()) {
                                        val singleUrl = urls.first()
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            AsyncImage(
                                                model = singleUrl,
                                                contentDescription = "Media",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 260.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .border(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.2f) else colors.borderSubtle, RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        if (onImageStackClick != null) onImageStackClick(urls, 0)
                                                        else onImageClick(singleUrl)
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isMediaOnly) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(end = 6.dp, bottom = 6.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.Black.copy(alpha = 0.55f))
                                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(
                                                            text = message.time,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color.White
                                                        )
                                                        if (isOutgoing) {
                                                            MessageStatusTick(
                                                                status = message.status,
                                                                isOutgoing = true,
                                                                normalTint = Color.White.copy(alpha = 0.9f),
                                                                readTint = Color(0xFF38BDF8),
                                                                pendingTint = Color.White.copy(alpha = 0.75f),
                                                                size = 11.5.dp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (!isMediaOnly) Spacer(Modifier.height(4.dp))
                                    }
                                }
                                "voice" -> {
                                    VoiceNotePlayer(
                                        messageId = message.id,
                                        audioPath = message.localUri ?: message.mediaUrl ?: "",
                                        waveformSamples = message.waveformSamples,
                                        durationSec = message.voiceDuration,
                                        isOutgoing = isOutgoing,
                                        time = message.time,
                                        status = message.status,
                                        isEdited = message.isEdited,
                                        timeColor = visual.timeColor,
                                        fontFamily = visual.fontFamily,
                                        showIntegratedMetadata = isVoiceOnly
                                    )

                                    if (message.transcription != null) {
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isOutgoing) Color.White.copy(alpha = 0.18f) else colors.surfaceSecondary)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                PhosphorSparkleIcon(tint = if (isOutgoing) Color.White.copy(alpha = 0.9f) else colors.iosBlue, size = 13.dp)
                                                Text(
                                                    text = message.transcription,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isOutgoing) Color.White.copy(alpha = 0.95f) else colors.textDark,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                "poll" -> {
                                    if (message.pollData != null) {
                                        PollCard(
                                            poll = message.pollData,
                                            isOutgoing = isOutgoing,
                                            onVote = onPollVote,
                                            onViewVotes = { onViewPollVotes?.invoke(message.pollData) }
                                        )
                                    }
                                }
                                "location" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isOutgoing) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.04f))
                                            .border(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.25f) else colors.borderSubtle, RoundedCornerShape(14.dp))
                                            .padding(10.dp)
                                        ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFF43F5E).copy(alpha = 0.18f), CircleShape)
                                                .border(0.5.dp, Color(0xFFF43F5E).copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LucideMapPin(tint = Color(0xFFF43F5E), size = 18.dp)
                                        }
                                        Column {
                                            Text(
                                                text = message.locationTitle ?: "Live Location",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOutgoing) Color.White else colors.textDark
                                            )
                                            Text(
                                                text = "Tap to open map",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else colors.textMuted
                                            )
                                        }
                                    }
                                }
                                "file", "file_stack" -> {
                                    if (message.fileItems.isNotEmpty()) {
                                        FileStackCluster(
                                            fileItems = message.fileItems,
                                            isOutgoing = isOutgoing
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isOutgoing) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.04f))
                                                .border(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.25f) else colors.borderSubtle, RoundedCornerShape(14.dp))
                                                .padding(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(colors.iosBlueSubtle, CircleShape)
                                                    .border(0.5.dp, colors.iosBlue.copy(alpha = 0.25f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LucideFileText(tint = colors.iosBlue, size = 18.dp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = message.fileName ?: "Document.pdf",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOutgoing) Color.White else colors.textDark,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${message.fileSize ?: "1.2 MB"} • Document",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else colors.textMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Message Text with Mentions & Search Highlight
                            if (!message.text.isNullOrEmpty()) {
                                var isExpanded by remember(message.id) { mutableStateOf(false) }
                                var isLongMessage by remember(message.id) { mutableStateOf(false) }

                                val annotatedText = remember(message.text, searchQuery, visual) {
                                    buildAnnotatedString {
                                        val text = message.text
                                        val words = text.split(" ")
                                        words.forEachIndexed { index, word ->
                                            if (word.startsWith("@")) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = visual.mentionColor,
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = TextDecoration.Underline
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else if (searchQuery.isNotEmpty() && word.contains(searchQuery, ignoreCase = true)) {
                                                withStyle(
                                                    SpanStyle(
                                                        background = Color(0xFFFACC15),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            } else {
                                                withStyle(
                                                    SpanStyle(
                                                        color = visual.textColor,
                                                        fontWeight = visual.fontWeight
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                            }
                                            if (index < words.size - 1) append(" ")
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.animateContentSize(
                                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    Text(
                                        text = annotatedText,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                        letterSpacing = (-0.01).sp,
                                        fontFamily = visual.fontFamily,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 100,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { layoutResult ->
                                            if (!isExpanded && (layoutResult.lineCount >= 100 || layoutResult.hasVisualOverflow)) {
                                                isLongMessage = true
                                            }
                                        }
                                    )

                                    if (isLongMessage) {
                                        Text(
                                            text = if (isExpanded) "Show less" else "....read more",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutgoing) Color.White.copy(alpha = 0.95f) else colors.iosBlue,
                                            modifier = Modifier
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    isExpanded = !isExpanded
                                                    SoundManager.playPop()
                                                }
                                                .padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                // 4. Timestamp & Read Receipts (Crisp iOS status tick)
                                if (!isVoiceOnly && !isMediaOnly) {
                                    Row(
                                        modifier = Modifier
                                            .align(if (isOutgoing) Alignment.End else Alignment.Start)
                                            .padding(top = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "${message.time}${if (message.isEdited) " • edited" else ""}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else visual.timeColor,
                                            fontFamily = visual.fontFamily
                                        )
                                        if (isOutgoing) {
                                            MessageStatusTick(
                                                status = message.status,
                                                isOutgoing = true,
                                                normalTint = Color.White.copy(alpha = 0.85f),
                                                readTint = Color(0xFF60A5FA),
                                                pendingTint = Color.White.copy(alpha = 0.75f),
                                                size = 12.dp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 4. Timestamp for messages without text
                                if (!isVoiceOnly && !isMediaOnly) {
                                    Row(
                                        modifier = Modifier
                                            .align(if (isOutgoing) Alignment.End else Alignment.Start)
                                            .padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "${message.time}${if (message.isEdited) " • edited" else ""}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = visual.timeColor,
                                            fontFamily = visual.fontFamily
                                        )
                                        if (isOutgoing) {
                                            MessageStatusTick(
                                                status = message.status,
                                                isOutgoing = true,
                                                normalTint = visual.timeColor,
                                                readTint = Color(0xFF38BDF8),
                                                pendingTint = visual.timeColor.copy(alpha = 0.75f),
                                                size = 12.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating heart pop effect on double-tap (UNCLIPPED, always in front & never cut off)
                    if (heartScale.value > 0.05f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(100f)
                                .graphicsLayer {
                                    scaleX = heartScale.value
                                    scaleY = heartScale.value
                                    alpha = heartAlpha.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.heart_like),
                                contentDescription = "Heart Like",
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    // 5. Reaction Tags (Clean Apple frosted capsule pill, anchored snugly to bubble)
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart)
                                .offset(
                                    y = 4.dp,
                                    x = if (isOutgoing) (-4).dp else 4.dp
                                )
                                .zIndex(60f)
                                .animateContentSize(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            message.reactions.forEach { (emoji, count) ->
                                val isMyReaction = message.userReaction == emoji
                                val badgeBg = if (colors.isLight) Color.White else Color(0xFF2C2C2E)
                                val badgeBorderColor = if (isMyReaction && colors.isLight) Color(0x33007AFF) else (if (colors.isLight) Color(0x22000000) else Color(0x38FFFFFF))
                                val badgeBorderWidth = 0.5.dp

                                Row(
                                    modifier = Modifier
                                        .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.12f))
                                        .clip(CircleShape)
                                        .background(badgeBg)
                                        .border(badgeBorderWidth, badgeBorderColor, CircleShape)
                                        .clickable {
                                            onReactionClick(emoji)
                                        }
                                        .padding(
                                            horizontal = if (count > 1) 6.dp else 4.5.dp,
                                            vertical = 1.5.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 11.sp
                                    )
                                    if (count > 1) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isMyReaction) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isMyReaction) colors.iosBlue else colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    }
}
}
}

@Composable
fun VoiceNotePlayer(
    messageId: String = "",
    audioPath: String = "",
    waveformSamples: List<Float> = emptyList(),
    durationSec: Int,
    isOutgoing: Boolean,
    time: String = "",
    status: String = "sent",
    isEdited: Boolean = false,
    timeColor: Color = Color.White.copy(alpha = 0.85f),
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    showIntegratedMetadata: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalChatColors.current

    val currentPlayingId by AudioPlayerManager.currentPlayingId.collectAsState()
    val isGlobalPlaying by AudioPlayerManager.isPlaying.collectAsState()
    val playbackProgress by AudioPlayerManager.playbackProgress.collectAsState()
    val playbackSpeed by AudioPlayerManager.playbackSpeed.collectAsState()
    val currentPositionMs by AudioPlayerManager.currentPositionMs.collectAsState()
    val totalDurationMs by AudioPlayerManager.totalDurationMs.collectAsState()

    val isThisPlaying = isGlobalPlaying && currentPlayingId == messageId
    val isThisActive = currentPlayingId == messageId

    val fallbackDuration = if (durationSec > 0) durationSec else 3
    val effectiveDuration = if (isThisActive && totalDurationMs > 0) (totalDurationMs / 1000).coerceAtLeast(1) else fallbackDuration
    val elapsedSec = if (isThisActive) (currentPositionMs / 1000) else 0
    val displaySec = if (isThisPlaying) (effectiveDuration - elapsedSec).coerceAtLeast(0) else effectiveDuration
    val timerStr = "0:${displaySec.toString().padStart(2, '0')}"

    val speedStr = when (playbackSpeed) {
        1.5f -> "1.5x"
        2.0f -> "2x"
        else -> "1x"
    }

    Row(
        modifier = Modifier
            .widthIn(min = 205.dp, max = 230.dp)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play/Pause Button (Enhanced Circular Button)
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White else colors.iosBlue)
                .clickable {
                    AudioPlayerManager.play(messageId, audioPath, context)
                },
            contentAlignment = Alignment.Center
        ) {
            if (isThisPlaying) {
                LucidePause(
                    tint = if (isOutgoing) colors.iosBlue else Color.White,
                    size = 17.dp
                )
            } else {
                LucidePlay(
                    tint = if (isOutgoing) colors.iosBlue else Color.White,
                    size = 17.dp,
                    modifier = Modifier.offset(x = 1.dp) // Optical center correction
                )
            }
        }

        // Waveform Bars & Info Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.5.dp)
        ) {
            // Waveform Vertical Bars (28 dynamic speech waveform bars)
            val defaultHeights = remember {
                listOf(
                    22, 38, 65, 95, 70, 40, 85, 100, 75, 45, 90, 80, 55, 30,
                    65, 95, 100, 70, 40, 85, 95, 60, 35, 75, 90, 65, 40, 22
                )
            }
            val barHeights = remember(waveformSamples) {
                if (waveformSamples.size >= 8) {
                    val targetCount = 28
                    val maxSample = (waveformSamples.maxOrNull() ?: 1f).coerceAtLeast(0.12f)
                    (0 until targetCount).map { i ->
                        val sampleIdx = ((i.toFloat() / targetCount.toFloat()) * waveformSamples.size).toInt().coerceIn(0, waveformSamples.lastIndex)
                        val normalized = (waveformSamples[sampleIdx] / maxSample).coerceIn(0.18f, 1.0f)
                        (normalized * 100).toInt()
                    }
                } else {
                    defaultHeights
                }
            }
            val activeProgress = if (isThisActive) playbackProgress else 0f

            Row(
                modifier = Modifier
                    .height(26.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                barHeights.forEachIndexed { index, h ->
                    val barProgress = index.toFloat() / barHeights.size.toFloat()
                    val isPast = isThisActive && barProgress <= activeProgress

                    val barScale = if (isThisPlaying) {
                        val infinite = rememberInfiniteTransition(label = "vn$index")
                        val livePulse by infinite.animateFloat(
                            initialValue = 0.65f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(360, delayMillis = (index % 6) * 55, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "barScale"
                        )
                        livePulse
                    } else 1f

                    val barColor = when {
                        isOutgoing -> {
                            if (isThisActive) {
                                if (isPast) Color.White else Color.White.copy(alpha = 0.38f)
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                        }
                        else -> {
                            if (isThisActive) {
                                if (isPast) colors.iosBlue else colors.iosBlue.copy(alpha = 0.30f)
                            } else {
                                colors.iosBlue.copy(alpha = 0.85f)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(((h / 100f) * barScale).coerceIn(0.18f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                            .clickable {
                                if (isThisActive) {
                                    AudioPlayerManager.seekTo(barProgress)
                                } else {
                                    AudioPlayerManager.play(messageId, audioPath, context)
                                    AudioPlayerManager.seekTo(barProgress)
                                }
                            }
                    )
                }
            }

            // Duration & Speed & Integrated Timestamp Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration
                Text(
                    text = timerStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutgoing) Color.White else colors.textDark,
                    fontFamily = fontFamily
                )

                Spacer(Modifier.width(6.dp))

                // Speed Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.surfaceSecondary)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            AudioPlayerManager.cycleSpeed()
                        }
                        .padding(horizontal = 5.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = speedStr,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutgoing) Color.White else colors.iosBlue,
                        fontFamily = fontFamily
                    )
                }

                if (showIntegratedMetadata && time.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "$time${if (isEdited) " • edited" else ""}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = timeColor,
                            fontFamily = fontFamily
                        )
                        if (isOutgoing) {
                            MessageStatusTick(
                                status = status,
                                isOutgoing = true,
                                normalTint = timeColor,
                                readTint = Color(0xFF38BDF8),
                                pendingTint = timeColor.copy(alpha = 0.75f),
                                size = 11.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollCard(
    poll: PollData,
    isOutgoing: Boolean,
    onVote: (Int) -> Unit,
    onViewVotes: (() -> Unit)? = null
) {
    val colors = LocalChatColors.current
    val totalVotes = poll.options.sumOf { it.votes }
    val maxVotes = poll.options.maxOfOrNull { it.votes } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Poll Header Indicator (Icon + "Poll")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LucideBarChart2(
                tint = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted,
                size = 14.dp
            )
            Text(
                text = "Poll",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted
            )
        }

        // Poll Question
        Text(
            text = poll.question,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
            color = if (isOutgoing) Color.White else colors.textDark
        )

        // Subtitle
        Text(
            text = if (poll.allowMultipleAnswers) "Select one or more options" else "Select one option",
            fontSize = 12.sp,
            color = if (isOutgoing) Color.White.copy(alpha = 0.65f) else colors.textMuted
        )

        // Options List
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            poll.options.forEachIndexed { index, option ->
                val percentage = if (totalVotes == 0) 0f else (option.votes.toFloat() / totalVotes)
                val animatedProgress by animateFloatAsState(
                    targetValue = percentage,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                    label = "pollProgress_${index}"
                )
                val isSelected = option.isUserVoted

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            com.chroma.studio.chat.sound.SoundManager.playReaction()
                            onVote(index)
                        }
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Checkbox circle + Option Text
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Checkbox circle (20dp)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            if (isOutgoing) Color.White else Color(0xFF34C759)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.5.dp,
                                        color = if (isSelected) {
                                            Color.Transparent
                                        } else {
                                            if (isOutgoing) Color.White.copy(alpha = 0.5f) else Color(0xFFC7C7CC)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    LucideCheck(
                                        tint = if (isOutgoing) colors.iosBlue else Color.White,
                                        size = 12.dp
                                    )
                                }
                            }

                            Text(
                                text = option.text,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isOutgoing) Color.White else colors.textDark
                            )
                        }

                        // Right: Voter Avatars + Vote Count
                        if (option.votes > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Overlapping mini avatars
                                if (option.voters.isNotEmpty()) {
                                    val visibleVoters = option.voters.take(3)
                                    Row(
                                        modifier = Modifier.padding(end = 2.dp)
                                    ) {
                                        visibleVoters.forEachIndexed { voterIdx, voter ->
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (-6 * voterIdx).dp)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        1.dp,
                                                        if (isOutgoing) colors.iosBlue else colors.cardBg,
                                                        CircleShape
                                                    )
                                                    .background(colors.surfaceSecondary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (voter.avatar.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = voter.avatar,
                                                        contentDescription = voter.name,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = voter.name.take(1).uppercase(),
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.iosBlue
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "${option.votes}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else colors.textMuted
                                )
                            }
                        }
                    }

                    // Progress bar (3dp height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (isOutgoing) Color.White.copy(alpha = 0.2f) else colors.separator.copy(alpha = 0.15f)
                            )
                    ) {
                        if (animatedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        if (isOutgoing) {
                                            Color.White.copy(alpha = 0.9f)
                                        } else {
                                            if (isSelected) Color(0xFF34C759) else colors.iosBlue
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Divider & "View Votes" Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .height(0.5.dp)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.25f) else colors.separator)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = onViewVotes != null) {
                    onViewVotes?.invoke()
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "View Votes",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOutgoing) Color.White else colors.iosBlue
            )
        }
    }
}

data class BubbleVisualConfig(
    val shape: androidx.compose.ui.graphics.Shape,
    val background: Brush,
    val borderWidth: androidx.compose.ui.unit.Dp,
    val borderColor: Color,
    val borderBrush: Brush? = null,
    val elevation: androidx.compose.ui.unit.Dp,
    val ambientColor: Color,
    val spotColor: Color,
    val textColor: Color,
    val mentionColor: Color,
    val timeColor: Color,
    val fontFamily: FontFamily?,
    val fontWeight: FontWeight,
    val hasTopSpecular: Boolean
)

fun resolveBubbleVisual(
    style: BubbleStyleType,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    colors: com.chroma.studio.chat.ui.theme.ChatColors,
    customCorners: CustomCornerConfig = CustomCornerConfig()
): BubbleVisualConfig {
    val base = resolveBaseBubbleVisual(style, isOutgoing, isConsecutive, colors)
    if (!customCorners.customEnabled) {
        return base
    }

    val tStart = customCorners.topStart.dp
    val tEnd = customCorners.topEnd.dp
    val bStart = customCorners.bottomStart.dp
    val bEnd = customCorners.bottomEnd.dp

    val customShape: androidx.compose.ui.graphics.Shape = when (customCorners.pointyCornerStyle) {
        PointyCornerStyle.CHAMFERED_CUT -> {
            if (isOutgoing) {
                CutCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = 2.dp)
            } else {
                CutCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = 2.dp)
            }
        }
        PointyCornerStyle.SHARP_ZERO -> {
            if (isOutgoing) {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = 0.dp)
            } else {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = 0.dp)
            }
        }
        PointyCornerStyle.SUBTLE_ROUNDED -> {
            if (isOutgoing) {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = 2.dp)
            } else {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = 2.dp)
            }
        }
        PointyCornerStyle.ROUNDED_MATCH, PointyCornerStyle.NONE -> {
            if (isOutgoing) {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = bEnd)
            } else {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = bStart)
            }
        }
    }

    return base.copy(shape = customShape)
}

private fun resolveBaseBubbleVisual(
    style: BubbleStyleType,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    colors: com.chroma.studio.chat.ui.theme.ChatColors
): BubbleVisualConfig {
    val isLight = colors.isLight

    return when (style) {
        BubbleStyleType.ROUNDED -> {
            val shape = if (isOutgoing) {
                if (isConsecutive) RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
                else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                if (isConsecutive) RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
                else RoundedCornerShape(20.dp)
            }
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.verticalGradient(listOf(Color(0xFF007AFF), Color(0xFF0066D6))),
                    borderWidth = 0.85.dp,
                    borderColor = Color.White.copy(alpha = 0.35f),
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.08f)
                        )
                    ),
                    elevation = 1.5.dp,
                    ambientColor = Color(0x22007AFF),
                    spotColor = Color(0x35007AFF),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.82f),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFE9E9EB) else Color(0xFF26252A)),
                    borderWidth = 0.85.dp,
                    borderColor = if (isLight) Color(0x22000000) else Color(0x35FFFFFF),
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.32f),
                            if (isLight) Color(0x18000000) else Color(0x28FFFFFF)
                        )
                    ),
                    elevation = 1.dp,
                    ambientColor = Color(0x0C000000),
                    spotColor = Color(0x12000000),
                    textColor = colors.textDark,
                    mentionColor = colors.iosBlue,
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.CLASSIC_IOS -> {
            val shape = if (isOutgoing) {
                if (isConsecutive) RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
                else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                if (isConsecutive) RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
                else RoundedCornerShape(20.dp)
            }
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(colors.iosBlue, colors.iosBlueHover)),
                    borderWidth = 0.85.dp,
                    borderColor = Color.White.copy(alpha = 0.28f),
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.60f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.08f)
                        )
                    ),
                    elevation = 2.dp,
                    ambientColor = Color(0x200078FE),
                    spotColor = Color(0x300078FE),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(colors.bubbleGray),
                    borderWidth = 0.85.dp,
                    borderColor = colors.borderSubtle,
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            if (isLight) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.25f),
                            colors.borderSubtle
                        )
                    ),
                    elevation = 1.dp,
                    ambientColor = Color(0x0A000000),
                    spotColor = Color(0x10000000),
                    textColor = colors.textDark,
                    mentionColor = colors.iosBlue,
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.WHATSAPP -> {
            val shape = if (isOutgoing) {
                if (isConsecutive) RoundedCornerShape(topStart = 16.dp, topEnd = 5.dp, bottomStart = 16.dp, bottomEnd = 5.dp)
                else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                if (isConsecutive) RoundedCornerShape(topStart = 5.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 5.dp)
                else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
            }
            if (isOutgoing) {
                val bg = if (isLight) Color(0xFFD9FDD3) else Color(0xFF005C4B)
                val txt = if (isLight) Color(0xFF111B21) else Color(0xFFE9EDEF)
                val time = if (isLight) Color(0xFF667781) else Color(0xFF8696A0)
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(bg),
                    borderWidth = 0.5.dp,
                    borderColor = bg.copy(alpha = 0.8f),
                    elevation = 1.dp,
                    ambientColor = Color(0x10000000),
                    spotColor = Color(0x15000000),
                    textColor = txt,
                    mentionColor = Color(0xFF027EB5),
                    timeColor = time,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            } else {
                val bg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF202C33)
                val txt = if (isLight) Color(0xFF111B21) else Color(0xFFE9EDEF)
                val time = if (isLight) Color(0xFF667781) else Color(0xFF8696A0)
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(bg),
                    borderWidth = 0.5.dp,
                    borderColor = colors.borderSubtle,
                    elevation = 1.dp,
                    ambientColor = Color(0x10000000),
                    spotColor = Color(0x15000000),
                    textColor = txt,
                    mentionColor = colors.iosBlue,
                    timeColor = time,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.MESSENGER -> {
            val shape = RoundedCornerShape(18.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(Color(0xFF0A7CFF), Color(0xFF0066FE), Color(0xFF004BE4))),
                    borderWidth = 0.75.dp,
                    borderColor = Color.White.copy(alpha = 0.25f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.50f), Color.Transparent)),
                    elevation = 2.dp,
                    ambientColor = Color(0x200072FF),
                    spotColor = Color(0x300072FF),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            } else {
                val bg = if (isLight) Color(0xFFF0F2F5) else Color(0xFF303030)
                val txt = if (isLight) Color(0xFF050505) else Color(0xFFF0F2F5)
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(bg),
                    borderWidth = 0.5.dp,
                    borderColor = colors.borderSubtle,
                    elevation = 1.dp,
                    ambientColor = Color(0x0A000000),
                    spotColor = Color(0x10000000),
                    textColor = txt,
                    mentionColor = Color(0xFF0084FF),
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.NEO_BRUTALISM -> {
            val shape = RoundedCornerShape(14.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFFEF08A) else Color(0xFFFACC15)),
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFF1E293B),
                    elevation = 3.dp,
                    ambientColor = Color(0xFF0F172A),
                    spotColor = Color(0xFF0F172A),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFF0F172A),
                    timeColor = Color(0xFF475569),
                    fontFamily = null,
                    fontWeight = FontWeight.Bold,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E293B)),
                    borderWidth = 1.5.dp,
                    borderColor = if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    elevation = 3.dp,
                    ambientColor = Color(0xFF0F172A),
                    spotColor = Color(0xFF0F172A),
                    textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    mentionColor = colors.iosBlue,
                    timeColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                    fontFamily = null,
                    fontWeight = FontWeight.Bold,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.GLASSMORPHISM -> {
            val shape = RoundedCornerShape(18.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(colors.iosBlue.copy(alpha = 0.55f)),
                    borderWidth = 0.85.dp,
                    borderColor = Color.White.copy(alpha = 0.45f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.15f))),
                    elevation = 2.dp,
                    ambientColor = Color(0x15000000),
                    spotColor = Color(0x200078FE),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.14f)),
                    borderWidth = 0.85.dp,
                    borderColor = if (isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f),
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    elevation = 2.dp,
                    ambientColor = Color(0x0A000000),
                    spotColor = Color(0x15000000),
                    textColor = colors.textDark,
                    mentionColor = colors.iosBlue,
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.MINIMAL_OUTLINED -> {
            val shape = RoundedCornerShape(18.dp)
            if (isOutgoing) {
                val bg = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                val txt = if (isLight) Color.White else Color(0xFF0F172A)
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(bg),
                    borderWidth = 0.75.dp,
                    borderColor = bg,
                    elevation = 0.dp,
                    ambientColor = Color.Transparent,
                    spotColor = Color.Transparent,
                    textColor = txt,
                    mentionColor = if (isLight) Color(0xFF93C5FD) else Color(0xFF2563EB),
                    timeColor = if (isLight) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color.Transparent),
                    borderWidth = 1.dp,
                    borderColor = colors.borderSubtle.copy(alpha = 0.9f),
                    elevation = 0.dp,
                    ambientColor = Color.Transparent,
                    spotColor = Color.Transparent,
                    textColor = colors.textDark,
                    mentionColor = colors.iosBlue,
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.NEUMORPHISM -> {
            val shape = RoundedCornerShape(18.dp)
            val bg = if (isLight) Color(0xFFEEF2F6) else Color(0xFF1E242C)
            BubbleVisualConfig(
                shape = shape,
                background = SolidColor(bg),
                borderWidth = 0.85.dp,
                borderColor = if (isLight) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f),
                borderBrush = Brush.verticalGradient(
                    listOf(
                        if (isLight) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
                        if (isLight) Color(0x18000000) else Color(0x35000000)
                    )
                ),
                elevation = 3.dp,
                ambientColor = if (isLight) Color(0x25A0B0C0) else Color(0x25000000),
                spotColor = if (isLight) Color(0x25A0B0C0) else Color(0x25000000),
                textColor = if (isOutgoing) colors.iosBlue else colors.textDark,
                mentionColor = colors.iosBlue,
                timeColor = colors.textMuted,
                fontFamily = null,
                fontWeight = FontWeight.Medium,
                hasTopSpecular = true
            )
        }

        BubbleStyleType.CYBERPUNK -> {
            val shape = RoundedCornerShape(14.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0B0F19)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF00F3FF),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFF00F3FF), Color(0xFF0284C7))),
                    elevation = 3.dp,
                    ambientColor = Color(0x3000F3FF),
                    spotColor = Color(0x4000F3FF),
                    textColor = Color(0xFFE0F2FE),
                    mentionColor = Color(0xFF38BDF8),
                    timeColor = Color(0xFF38BDF8).copy(alpha = 0.8f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0B0F19)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFFF43F5E),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))),
                    elevation = 3.dp,
                    ambientColor = Color(0x30F43F5E),
                    spotColor = Color(0x40F43F5E),
                    textColor = Color(0xFFFFE4E6),
                    mentionColor = Color(0xFFFB7185),
                    timeColor = Color(0xFFFB7185).copy(alpha = 0.8f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.RETRO_PIXEL -> {
            val shape = RoundedCornerShape(10.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFDC2626)),
                    borderWidth = 1.25.dp,
                    borderColor = Color(0xFF7F1D1D),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFF7F1D1D))),
                    elevation = 2.dp,
                    ambientColor = Color(0x30000000),
                    spotColor = Color(0x40000000),
                    textColor = Color.White,
                    mentionColor = Color(0xFFFDE047),
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B)),
                    borderWidth = 1.25.dp,
                    borderColor = if (isLight) Color(0xFF94A3B8) else Color(0xFF475569),
                    elevation = 2.dp,
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x30000000),
                    textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    mentionColor = Color(0xFF0284C7),
                    timeColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.TEAM_CHAT -> {
            val shape = RoundedCornerShape(16.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF313338)),
                    borderWidth = 0.5.dp,
                    borderColor = Color(0xFF4E5058),
                    elevation = 1.dp,
                    ambientColor = Color(0x10000000),
                    spotColor = Color(0x20000000),
                    textColor = Color(0xFFF2F3F5),
                    mentionColor = Color(0xFF5865F2),
                    timeColor = Color(0xFF949BA4),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF2B2D31)),
                    borderWidth = 0.5.dp,
                    borderColor = Color(0xFF383A40),
                    elevation = 1.dp,
                    ambientColor = Color(0x10000000),
                    spotColor = Color(0x20000000),
                    textColor = Color(0xFFDBDEE1),
                    mentionColor = Color(0xFF5865F2),
                    timeColor = Color(0xFF949BA4),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.CLAYMORPHISM -> {
            val shape = RoundedCornerShape(20.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF4F46E5)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF818CF8),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color(0xFF4338CA))),
                    elevation = 5.dp,
                    ambientColor = Color(0x304F46E5),
                    spotColor = Color(0x404F46E5),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = true
                )
            } else {
                val bg = if (isLight) Color(0xFFF1F5F9) else Color(0xFF334155)
                val txt = if (isLight) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                val border = if (isLight) Color.White else Color(0xFF64748B)
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(bg),
                    borderWidth = 1.dp,
                    borderColor = border,
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), if (isLight) Color(0x18000000) else Color(0x35000000))),
                    elevation = 4.dp,
                    ambientColor = Color(0x15000000),
                    spotColor = Color(0x20000000),
                    textColor = txt,
                    mentionColor = colors.iosBlue,
                    timeColor = colors.textMuted,
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.METRO_UI -> {
            val shape = RoundedCornerShape(14.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0078D4)),
                    borderWidth = 0.75.dp,
                    borderColor = Color.White.copy(alpha = 0.35f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)),
                    elevation = 2.dp,
                    ambientColor = Color(0x200078D4),
                    spotColor = Color(0x300078D4),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.SemiBold,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFF3F3F3) else Color(0xFF2B2B2B)),
                    borderWidth = 0.75.dp,
                    borderColor = if (isLight) Color(0x18000000) else Color(0x28FFFFFF),
                    elevation = 1.dp,
                    ambientColor = Color(0x0A000000),
                    spotColor = Color(0x15000000),
                    textColor = if (isLight) Color(0xFF1E1E1E) else Color(0xFFF1F1F1),
                    mentionColor = Color(0xFF0078D4),
                    timeColor = if (isLight) Color(0xFF767676) else Color(0xFF9E9E9E),
                    fontFamily = null,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.HOLOGRAPHIC -> {
            val shape = RoundedCornerShape(18.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFEDE9FE), Color(0xFFE0E7FF))),
                    borderWidth = 0.85.dp,
                    borderColor = Color.White.copy(alpha = 0.85f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.40f), Color(0x15000000))),
                    elevation = 2.5.dp,
                    ambientColor = Color(0x186366F1),
                    spotColor = Color(0x256366F1),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFF4338CA),
                    timeColor = Color(0xFF475569),
                    fontFamily = null,
                    fontWeight = FontWeight.SemiBold,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEDE9FE), Color(0xFFE0F2FE))),
                    borderWidth = 0.85.dp,
                    borderColor = Color.White.copy(alpha = 0.85f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.30f))),
                    elevation = 2.dp,
                    ambientColor = Color(0x120284C7),
                    spotColor = Color(0x186366F1),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFF4338CA),
                    timeColor = Color(0xFF475569),
                    fontFamily = null,
                    fontWeight = FontWeight.SemiBold,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.SKETCHBOOK -> {
            val shape = RoundedCornerShape(16.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFFEF3C7)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF475569),
                    elevation = 2.dp,
                    ambientColor = Color(0x18000000),
                    spotColor = Color(0x20000000),
                    textColor = Color(0xFF1E293B),
                    mentionColor = Color(0xFFB45309),
                    timeColor = Color(0xFF64748B),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E293B)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF475569),
                    elevation = 2.dp,
                    ambientColor = Color(0x18000000),
                    spotColor = Color(0x20000000),
                    textColor = if (isLight) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    mentionColor = Color(0xFF2563EB),
                    timeColor = Color(0xFF64748B),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.NEON_NIGHTS -> {
            val shape = RoundedCornerShape(16.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0A0A10)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFFA855F7),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFFC084FC), Color(0xFF7E22CE))),
                    elevation = 3.dp,
                    ambientColor = Color(0x25A855F7),
                    spotColor = Color(0x35A855F7),
                    textColor = Color(0xFFF3E8FF),
                    mentionColor = Color(0xFFC084FC),
                    timeColor = Color(0xFFA855F7).copy(alpha = 0.8f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0A0A10)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF38BDF8),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFF0284C7))),
                    elevation = 3.dp,
                    ambientColor = Color(0x2538BDF8),
                    spotColor = Color(0x3538BDF8),
                    textColor = Color(0xFFE0F2FE),
                    mentionColor = Color(0xFF38BDF8),
                    timeColor = Color(0xFF38BDF8).copy(alpha = 0.8f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.WIN95 -> {
            val shape = RoundedCornerShape(12.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFFFFBEB)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF94A3B8),
                    borderBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFF64748B))),
                    elevation = 2.dp,
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x25000000),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFF000080),
                    timeColor = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFE2E8F0)),
                    borderWidth = 1.dp,
                    borderColor = Color(0xFF94A3B8),
                    borderBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFF64748B))),
                    elevation = 2.dp,
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x25000000),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFF000080),
                    timeColor = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.COMIC_BOOK -> {
            val shape = RoundedCornerShape(16.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFE11D48)),
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFF0F172A),
                    elevation = 3.dp,
                    ambientColor = Color(0xFF0F172A),
                    spotColor = Color(0xFF0F172A),
                    textColor = Color.White,
                    mentionColor = Color(0xFFFEF08A),
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.ExtraBold,
                    hasTopSpecular = false
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFFFFFBEB)),
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFF0F172A),
                    elevation = 3.dp,
                    ambientColor = Color(0xFF0F172A),
                    spotColor = Color(0xFF0F172A),
                    textColor = Color(0xFF0F172A),
                    mentionColor = Color(0xFFE11D48),
                    timeColor = Color(0xFF475569),
                    fontFamily = null,
                    fontWeight = FontWeight.ExtraBold,
                    hasTopSpecular = false
                )
            }
        }

        BubbleStyleType.TERMINAL -> {
            val shape = RoundedCornerShape(14.dp)
            BubbleVisualConfig(
                shape = shape,
                background = SolidColor(Color(0xFF090D12)),
                borderWidth = 0.85.dp,
                borderColor = if (isOutgoing) Color(0xFF10B981) else Color(0xFF059669),
                elevation = 2.dp,
                ambientColor = Color(0x1810B981),
                spotColor = Color(0x2510B981),
                textColor = if (isOutgoing) Color(0xFF34D399) else Color(0xFF6EE7B7),
                mentionColor = Color(0xFFA7F3D0),
                timeColor = Color(0xFF059669),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                hasTopSpecular = false
            )
        }

        BubbleStyleType.PRESTIGE -> {
            val shape = RoundedCornerShape(18.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF0F291E)),
                    borderWidth = 0.85.dp,
                    borderColor = Color(0xFFD4AF37),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFB45309))),
                    elevation = 3.dp,
                    ambientColor = Color(0x25D4AF37),
                    spotColor = Color(0x35D4AF37),
                    textColor = Color(0xFFF9F6EE),
                    mentionColor = Color(0xFFD4AF37),
                    timeColor = Color(0xFFD4AF37).copy(alpha = 0.85f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = SolidColor(Color(0xFF18181B)),
                    borderWidth = 0.85.dp,
                    borderColor = Color(0xFFC5A059),
                    borderBrush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFF78350F))),
                    elevation = 2.dp,
                    ambientColor = Color(0x18C5A059),
                    spotColor = Color(0x25C5A059),
                    textColor = Color(0xFFF4F4F5),
                    mentionColor = Color(0xFFC5A059),
                    timeColor = Color(0xFFC5A059).copy(alpha = 0.85f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    hasTopSpecular = true
                )
            }
        }

        BubbleStyleType.ORGANIC_BLOB -> {
            val shape = if (isOutgoing) RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 14.dp)
            else RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 20.dp)
            if (isOutgoing) {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF881337))),
                    borderWidth = 0.75.dp,
                    borderColor = Color.White.copy(alpha = 0.25f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)),
                    elevation = 3.dp,
                    ambientColor = Color(0x25BE123C),
                    spotColor = Color(0x35BE123C),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = true
                )
            } else {
                BubbleVisualConfig(
                    shape = shape,
                    background = Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF5B21B6), Color(0xFF6D28D9))),
                    borderWidth = 0.75.dp,
                    borderColor = Color.White.copy(alpha = 0.25f),
                    borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)),
                    elevation = 3.dp,
                    ambientColor = Color(0x255B21B6),
                    spotColor = Color(0x355B21B6),
                    textColor = Color.White,
                    mentionColor = Color.White,
                    timeColor = Color.White.copy(alpha = 0.85f),
                    fontFamily = null,
                    fontWeight = FontWeight.Medium,
                    hasTopSpecular = true
                )
            }
        }
    }
}

/**
 * Instagram / Apple iOS style Images Stack Layout for multi-photo messages (2+ images).
 * Features:
 * - Instagram Physical Card Stack: Layered cards with subtle 3D tilt, rotation, and spring physics
 * - Interactive swipe/tap gesture to cycle through photos in the stack
 * - Instagram Top-Right counter pill: Layer stack icon + "1/N" counter
 * - Instagram animated dot indicators: Active pill and inactive translucent dots
 * - Double-tap animated heart pop reaction (Instagram style)
 * - View mode toggle: Quick switch between Instagram "📚 Stack" and "⊞ Grid"
 * - High-res Lightbox integration on tap
 * - Floating glass timestamp & status checkmarks
 */
@Composable
fun MediaStackGrid(
    mediaUrls: List<String>,
    isOutgoing: Boolean,
    isMediaOnly: Boolean = false,
    time: String = "",
    status: String = "sent",
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = mediaUrls.size
    if (count == 0) return

    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var viewMode by remember { mutableStateOf(if (count > 1) "grid" else "single") }
    var activeIndex by remember { mutableStateOf(0) }
    var showHeartPop by remember { mutableStateOf(false) }

    // Double tap heart pop animation
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartPop) 1.25f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "heartScale",
        finishedListener = { if (showHeartPop) showHeartPop = false }
    )
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeartPop) 1f else 0f,
        animationSpec = tween(durationMillis = if (showHeartPop) 150 else 300),
        label = "heartAlpha"
    )

    // Dedicated Clean Single Image Layout (No 3D stack, No 1/1 badge)
    if (count == 1) {
        val singleUrl = mediaUrls.first()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(
                    if (!isMediaOnly) RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 6.dp,
                        bottomEnd = 6.dp
                    ) else RoundedCornerShape(16.dp)
                )
                .background(Color(0xFF1C1C1E))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            showHeartPop = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                        },
                        onTap = { onImageClick(0) }
                    )
                }
        ) {
            AsyncImage(
                model = singleUrl,
                contentDescription = "Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 280.dp),
                contentScale = ContentScale.Crop
            )

            // Double-tap heart animation
            if (heartAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                            alpha = heartAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "❤️", fontSize = 62.sp)
                }
            }

            // Overlay Floating Timestamp Badge
            if (isMediaOnly && time.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = time,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        if (isOutgoing) {
                            MessageStatusTick(
                                status = status,
                                isOutgoing = true,
                                normalTint = Color.White.copy(alpha = 0.9f),
                                readTint = Color(0xFF38BDF8),
                                pendingTint = Color.White.copy(alpha = 0.75f),
                                size = 11.dp
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                if (!isMediaOnly) RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 6.dp,
                    bottomEnd = 6.dp
                ) else RoundedCornerShape(16.dp)
            )
    ) {
        if (viewMode == "stack") {
            // ==========================================
            // INSTAGRAM INTERACTIVE CARD STACK
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Render up to 3 cards in reverse order (bottom layer to top layer)
                val maxVisibleCards = minOf(3, count)
                for (depth in (maxVisibleCards - 1) downTo 0) {
                    val cardIdx = (activeIndex + depth) % count
                    val cardUrl = mediaUrls[cardIdx]

                    val rotationTarget = when (depth) {
                        0 -> 0f
                        1 -> if (cardIdx % 2 == 0) -3.2f else 3.2f
                        else -> if (cardIdx % 2 == 0) 4.5f else -4.5f
                    }
                    val scaleTarget = when (depth) {
                        0 -> 1.0f
                        1 -> 0.94f
                        else -> 0.88f
                    }
                    val yOffsetTarget = when (depth) {
                        0 -> 0.dp
                        1 -> (-6).dp
                        else -> (-12).dp
                    }
                    val xOffsetTarget = when (depth) {
                        0 -> 0.dp
                        1 -> if (cardIdx % 2 == 0) (-5).dp else 5.dp
                        else -> if (cardIdx % 2 == 0) 6.dp else (-6).dp
                    }

                    val animatedRotation by animateFloatAsState(
                        targetValue = rotationTarget,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                        label = "cardRot_$depth"
                    )
                    val animatedScale by animateFloatAsState(
                        targetValue = scaleTarget,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                        label = "cardScale_$depth"
                    )
                    val animatedYOffset by animateDpAsState(
                        targetValue = yOffsetTarget,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                        label = "cardY_$depth"
                    )
                    val animatedXOffset by animateDpAsState(
                        targetValue = xOffsetTarget,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                        label = "cardX_$depth"
                    )

                    val cardShape = RoundedCornerShape(16.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = animatedXOffset, y = animatedYOffset)
                            .graphicsLayer {
                                rotationZ = animatedRotation
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                            .shadow(
                                elevation = if (depth == 0) 10.dp else 4.dp,
                                shape = cardShape,
                                spotColor = Color.Black.copy(alpha = 0.35f),
                                ambientColor = Color.Black.copy(alpha = 0.20f)
                            )
                            .clip(cardShape)
                            .background(Color(0xFF1C1C1E))
                            .border(
                                width = 0.85.dp,
                                color = if (depth == 0) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f),
                                shape = cardShape
                            )
                            .then(
                                if (depth == 0) {
                                    Modifier
                                        .pointerInput(activeIndex) {
                                            var accumulatedDrag = 0f
                                            detectHorizontalDragGestures(
                                                onDragStart = { accumulatedDrag = 0f },
                                                onDragEnd = { accumulatedDrag = 0f },
                                                onDragCancel = { accumulatedDrag = 0f },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    accumulatedDrag += dragAmount
                                                    if (accumulatedDrag < -40f) {
                                                        change.consume()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        SoundManager.playReaction()
                                                        activeIndex = (activeIndex + 1) % count
                                                        accumulatedDrag = 0f
                                                    } else if (accumulatedDrag > 40f) {
                                                        change.consume()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        SoundManager.playReaction()
                                                        activeIndex = (activeIndex - 1 + count) % count
                                                        accumulatedDrag = 0f
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(activeIndex) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    showHeartPop = true
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    SoundManager.playReaction()
                                                },
                                                onTap = {
                                                    onImageClick(activeIndex)
                                                }
                                            )
                                        }
                                } else Modifier
                            )
                    ) {
                        AsyncImage(
                            model = cardUrl,
                            contentDescription = "Photo ${cardIdx + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle darkening on background cards to enhance 3D card depth
                        if (depth > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = if (depth == 1) 0.18f else 0.35f))
                            )
                        }
                    }
                }

                // Instagram Double-Tap Popping Heart Animation
                if (heartAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                                alpha = heartAlpha
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "❤️",
                            fontSize = 62.sp
                        )
                    }
                }

                // Top-Right: Instagram Stack Badge (Icon + "1/N" counter)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable {
                            // Tap stack counter to cycle to next photo
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playReaction()
                            activeIndex = (activeIndex + 1) % count
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Stacked dual-rectangle icon (Instagram Photo Stack)
                        Box(modifier = Modifier.size(11.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .offset(x = 2.dp, y = (-1).dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .offset(x = 0.dp, y = 2.dp)
                                    .background(Color.Black)
                                    .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                        Text(
                            text = "${activeIndex + 1}/$count",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Top-Left: View Mode Switcher ("📚 Stack" / "⊞ Grid")
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewMode = "grid"
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⊞ Grid",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }

                // Bottom-Center: Instagram animated dot indicators (● ○ ○)
                if (count in 2..8) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.50f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (dotIdx in 0 until count) {
                            val isActive = dotIdx == activeIndex
                            Box(
                                modifier = Modifier
                                    .animateContentSize()
                                    .size(width = if (isActive) 14.dp else 5.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.45f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeIndex = dotIdx
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // COLLAGE GRID MODE (With Stack Switcher)
            // ==========================================
            val gap = 2.5.dp
            Box(modifier = Modifier.fillMaxWidth()) {
                when {
                    count == 2 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            AsyncImage(
                                model = mediaUrls[0],
                                contentDescription = "Photo 1",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onImageClick(0) },
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = mediaUrls[1],
                                contentDescription = "Photo 2",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onImageClick(1) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    count == 3 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            AsyncImage(
                                model = mediaUrls[0],
                                contentDescription = "Photo 1",
                                modifier = Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight()
                                    .clickable { onImageClick(0) },
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                AsyncImage(
                                    model = mediaUrls[1],
                                    contentDescription = "Photo 2",
                                    modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable { onImageClick(1) },
                                    contentScale = ContentScale.Crop
                                )
                                AsyncImage(
                                    model = mediaUrls[2],
                                    contentDescription = "Photo 3",
                                    modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable { onImageClick(2) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    else -> { // count >= 4
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp),
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                AsyncImage(
                                    model = mediaUrls[0],
                                    contentDescription = "Photo 1",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onImageClick(0) },
                                    contentScale = ContentScale.Crop
                                )
                                AsyncImage(
                                    model = mediaUrls[1],
                                    contentDescription = "Photo 2",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onImageClick(1) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp),
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                AsyncImage(
                                    model = mediaUrls[2],
                                    contentDescription = "Photo 3",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onImageClick(2) },
                                    contentScale = ContentScale.Crop
                                )
                                val isOverflown = count > 4
                                val remainingCount = count - 4
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onImageClick(3) }
                                ) {
                                    AsyncImage(
                                        model = mediaUrls[3],
                                        contentDescription = "Photo 4",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isOverflown) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.58f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+$remainingCount",
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.5).sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top-Left: Switch back to Instagram Stack Mode
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewMode = "stack"
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📚 Stack",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
            }
        }

        // Overlay Floating Timestamp Badge (Apple / Telegram style)
        if (isMediaOnly && time.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = time,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    if (isOutgoing) {
                        MessageStatusTick(
                            status = status,
                            isOutgoing = true,
                            normalTint = Color.White.copy(alpha = 0.9f),
                            readTint = Color(0xFF38BDF8),
                            pendingTint = Color.White.copy(alpha = 0.75f),
                            size = 11.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple iOS style File Stack Cluster for multi-document attachments.
 */
@Composable
fun FileStackCluster(
    fileItems: List<ChatFileItem>,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isOutgoing) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.04f))
            .border(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.25f) else colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stack Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhosphorFileIcon(
                    tint = if (isOutgoing) Color.White else colors.iosBlue,
                    size = 14.dp
                )
                Text(
                    text = "${fileItems.size} Files Attached",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutgoing) Color.White else colors.textDark
                )
            }
            Text(
                text = "Tap to open",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted
            )
        }

        HorizontalDivider(
            color = if (isOutgoing) Color.White.copy(alpha = 0.15f) else colors.borderSubtle,
            thickness = 0.5.dp
        )

        // Individual File Rows
        fileItems.forEachIndexed { index, file ->
            val ext = (file.extension.ifEmpty { file.name.substringAfterLast(".", "FILE") }).uppercase()
            val badgeColor = when (ext) {
                "PDF" -> Color(0xFFFF3B30)
                "DOC", "DOCX" -> Color(0xFF007AFF)
                "XLS", "XLSX", "CSV" -> Color(0xFF34C759)
                "ZIP", "RAR", "7Z" -> Color(0xFFAF52DE)
                "APK" -> Color(0xFFFF9500)
                else -> colors.iosBlue
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { /* preview or open */ }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(badgeColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ext.take(4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOutgoing) Color.White else colors.textDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = file.size,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted
                    )
                }
                LucideDownload(
                    tint = if (isOutgoing) Color.White.copy(alpha = 0.85f) else colors.textMuted,
                    size = 15.dp
                )
            }

            if (index < fileItems.size - 1) {
                HorizontalDivider(
                    color = if (isOutgoing) Color.White.copy(alpha = 0.1f) else colors.borderSubtle.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}


