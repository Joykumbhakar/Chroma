package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ChartColumn
import com.composables.icons.lucide.Users
import androidx.compose.material3.Surface
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.ui.components.LiquidGlassRadio
import com.chroma.studio.ui.components.LiquidGlassProgressBar
import com.chroma.studio.ui.components.LiquidGlassPollOptionMeter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
import com.chroma.studio.ui.components.IosAvatarView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import java.util.regex.Pattern
import com.chroma.studio.R
import com.chroma.studio.chat.data.ChatFileItem
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.chat.data.PollData
import com.chroma.studio.chat.media.AudioPlayerManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.ui.theme.ChatColors
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.foundation.shape.CutCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import com.chroma.studio.chat.data.BubbleAnimationStyle
import com.chroma.studio.chat.data.BubbleStyleType
import com.chroma.studio.chat.data.CustomCornerConfig
import com.chroma.studio.chat.data.PointyCornerStyle

private val MARKDOWN_PATTERN: Pattern = Pattern.compile(
    "\\[([^\\]]+)\\]\\(((?:https?://|www\\.)[^\\s)]+)\\)|" + // 1, 2: Markdown link
    "((?:https?://|www\\.)[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!\\$&'()*+,;=%]+)|" + // 3: Bare URL
    "```(?:[a-zA-Z0-9_\\-]*\\r?\\n)?([\\s\\S]*?)```|" + // 4: Code block
    "`([^`\\r\\n]+)`|" + // 5: Inline code
    "\\*\\*([^\\*\\r\\n]+)\\*\\*|" + // 6: Bold
    "(?:\\*([^\\*\\r\\n]+)\\*|_([^\\_\\r\\n]+)_)|" + // 7, 8: Italic
    "(?:~~([^~\\r\\n]+)~~|~([^~\\r\\n]+)~)|" + // 9, 10: Strikethrough
    "\\|\\|([\\s\\S]*?)\\|\\||" + // 11: Spoiler
    "(?:(?<=^|[\\s(]))@([a-zA-Z0-9_]{2,30})\\b|" + // 12: Mention
    "(?:(?<=^|[\\s(]))#([a-zA-Z0-9_]{2,30})\\b" // 13: Hashtag
)

fun parseMarkdownToAnnotatedString(
    text: String,
    visual: BubbleVisualConfig,
    isOutgoing: Boolean = false,
    searchQuery: String = "",
    revealedSpoilers: Set<String> = emptySet()
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val matcher = MARKDOWN_PATTERN.matcher(text)
        var lastEnd = 0
        var spoilerIndex = 0

        fun appendPlainWithSearch(plainText: String) {
            if (plainText.isEmpty()) return
            if (searchQuery.isNotBlank() && plainText.contains(searchQuery, ignoreCase = true)) {
                var searchStart = 0
                val lowerPlain = plainText.lowercase()
                val lowerQuery = searchQuery.lowercase()
                while (searchStart < plainText.length) {
                    val foundIdx = lowerPlain.indexOf(lowerQuery, searchStart)
                    if (foundIdx == -1) {
                        withStyle(SpanStyle(color = visual.textColor, fontWeight = visual.fontWeight)) {
                            append(plainText.substring(searchStart))
                        }
                        break
                    }
                    if (foundIdx > searchStart) {
                        withStyle(SpanStyle(color = visual.textColor, fontWeight = visual.fontWeight)) {
                            append(plainText.substring(searchStart, foundIdx))
                        }
                    }
                    withStyle(
                        SpanStyle(
                            background = Color(0xFFFACC15),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(plainText.substring(foundIdx, foundIdx + searchQuery.length))
                    }
                    searchStart = foundIdx + searchQuery.length
                }
            } else {
                withStyle(SpanStyle(color = visual.textColor, fontWeight = visual.fontWeight)) {
                    append(plainText)
                }
            }
        }

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastEnd) {
                appendPlainWithSearch(text.substring(lastEnd, start))
            }

            when {
                // 1 & 2: Markdown Link [text](url)
                matcher.group(1) != null && matcher.group(2) != null -> {
                    val linkText = matcher.group(1)!!
                    val url = matcher.group(2)!!
                    val tagStart = length
                    withStyle(
                        SpanStyle(
                            color = visual.mentionColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                    addStringAnnotation(tag = "URL", annotation = url, start = tagStart, end = length)
                }

                // 3: Bare URL
                matcher.group(3) != null -> {
                    val url = matcher.group(3)!!
                    val tagStart = length
                    withStyle(
                        SpanStyle(
                            color = visual.mentionColor,
                            fontWeight = FontWeight.Normal,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(url)
                    }
                    addStringAnnotation(tag = "URL", annotation = url, start = tagStart, end = length)
                }

                // 4: Code Block
                matcher.group(4) != null -> {
                    val codeContent = matcher.group(4)!!
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            background = if (isOutgoing) Color(0x38000000) else Color(0x18000000),
                            color = visual.textColor
                        )
                    ) {
                        append("\n" + codeContent.trim() + "\n")
                    }
                }

                // 5: Inline Code
                matcher.group(5) != null -> {
                    val codeText = matcher.group(5)!!
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = if (isOutgoing) Color(0x38000000) else Color(0x18000000),
                            color = visual.textColor,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" $codeText ")
                    }
                }

                // 6: Bold **text**
                matcher.group(6) != null -> {
                    val boldText = matcher.group(6)!!
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = visual.textColor)) {
                        append(boldText)
                    }
                }

                // 7 or 8: Italic *text* or _text_
                matcher.group(7) != null || matcher.group(8) != null -> {
                    val italicText = matcher.group(7) ?: matcher.group(8)!!
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = visual.textColor)) {
                        append(italicText)
                    }
                }

                // 9 or 10: Strikethrough ~~text~~ or ~text~
                matcher.group(9) != null || matcher.group(10) != null -> {
                    val strikeText = matcher.group(9) ?: matcher.group(10)!!
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = visual.textColor.copy(alpha = 0.85f)
                        )
                    ) {
                        append(strikeText)
                    }
                }

                // 11: Spoiler ||text||
                matcher.group(11) != null -> {
                    val spoilerText = matcher.group(11)!!
                    val spoilerId = "spoiler_${spoilerIndex++}"
                    val isRevealed = revealedSpoilers.contains(spoilerId)
                    val tagStart = length
                    if (isRevealed) {
                        withStyle(
                            SpanStyle(
                                background = if (isOutgoing) Color(0x2EFFFFFF) else Color(0x22000000),
                                color = visual.textColor
                            )
                        ) {
                            append(spoilerText)
                        }
                    } else {
                        withStyle(
                            SpanStyle(
                                background = if (isOutgoing) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.6f),
                                color = Color.Transparent
                            )
                        ) {
                            append(spoilerText)
                        }
                    }
                    addStringAnnotation(tag = "SPOILER", annotation = spoilerId, start = tagStart, end = length)
                }

                // 12: Mention @user
                matcher.group(12) != null -> {
                    val username = matcher.group(12)!!
                    withStyle(
                        SpanStyle(
                            color = visual.mentionColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("@$username")
                    }
                }

                // 13: Hashtag #tag
                matcher.group(13) != null -> {
                    val tag = matcher.group(13)!!
                    withStyle(
                        SpanStyle(
                            color = visual.mentionColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("#$tag")
                    }
                }

                else -> {
                    appendPlainWithSearch(text.substring(start, end))
                }
            }
            lastEnd = end
        }

        if (lastEnd < text.length) {
            appendPlainWithSearch(text.substring(lastEnd))
        }
    }
}

@Composable
private fun MessageReactionDock(
    visible: Boolean,
    isOutgoing: Boolean,
    userReaction: String?,
    colors: ChatColors,
    onReactionClick: (String) -> Unit,
    onLongPress: () -> Unit,
    onReplyAction: () -> Unit,
    onCloseDock: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible,
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
                val isSelected = userReaction == emoji
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.iosBlueSubtle else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) colors.iosBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            coroutineScope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundManager.playReaction()
                                onCloseDock()
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
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playReaction()
                        onCloseDock()
                        onLongPress()
                    },
                contentAlignment = Alignment.Center
            ) {
                PhosphorPlusIcon(tint = colors.textDark, size = 18.dp)
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
                    onCloseDock()
                },
                modifier = Modifier.size(30.dp)
            ) {
                LucideReply(tint = colors.iosBlue, size = 16.dp)
            }
        }
    }
}

@Immutable
data class MessageRowCallbacks(
    val onReactionClick: (String) -> Unit = {},
    val onDoubleTap: () -> Unit = {},
    val onLongPress: () -> Unit = {},
    val onReplyAction: () -> Unit = {},
    val onEditAction: () -> Unit = {},
    val onPinAction: () -> Unit = {},
    val onCopyAction: () -> Unit = {},
    val onDeleteAction: () -> Unit = {},
    val onImageClick: (String) -> Unit = {},
    val onImageStackClick: ((List<String>, Int) -> Unit)? = null,
    val onPollVote: (Int) -> Unit = {},
    val onPollRetractVote: (() -> Unit)? = null,
    val onClosePoll: (() -> Unit)? = null,
    val onViewPollVotes: ((PollData) -> Unit)? = null,
    val onToggleSelect: () -> Unit = {},
    val onQuoteClick: ((String) -> Unit)? = null,
    val onRetry: (() -> Unit)? = null
)

@Immutable
data class MessageBubbleConfig(
    val bubbleStyle: BubbleStyleType = BubbleStyleType.CLASSIC_IOS,
    val customCornerConfig: CustomCornerConfig = CustomCornerConfig(),
    val animationStyle: BubbleAnimationStyle = BubbleAnimationStyle.POP_ZOOM
)

@Composable
private fun Modifier.messageEntrance(
    isFreshMessage: Boolean,
    animationStyle: BubbleAnimationStyle,
    isOutgoing: Boolean
): Modifier {
    if (!isFreshMessage) return this

    val animScaleX = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.POP_ZOOM -> 0.42f
                BubbleAnimationStyle.SLIDE_UP_FADE -> 1f
                BubbleAnimationStyle.ELASTIC_SPRING -> 0.25f
                BubbleAnimationStyle.FLIP_IN_3D -> 0.85f
                BubbleAnimationStyle.SLIDE_IN_SIDE -> 0.92f
                BubbleAnimationStyle.EXPAND_REVEAL -> 0.70f
                BubbleAnimationStyle.GENTLE_FADE -> 0.96f
            }
        )
    }
    val animScaleY = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.POP_ZOOM -> 0.28f
                BubbleAnimationStyle.SLIDE_UP_FADE -> 1f
                BubbleAnimationStyle.ELASTIC_SPRING -> 0.25f
                BubbleAnimationStyle.FLIP_IN_3D -> 0.85f
                BubbleAnimationStyle.SLIDE_IN_SIDE -> 0.92f
                BubbleAnimationStyle.EXPAND_REVEAL -> 0.38f
                BubbleAnimationStyle.GENTLE_FADE -> 0.96f
            }
        )
    }
    val animAlpha = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.POP_ZOOM -> 0.35f
                BubbleAnimationStyle.SLIDE_UP_FADE -> 0f
                BubbleAnimationStyle.ELASTIC_SPRING -> 0.1f
                BubbleAnimationStyle.FLIP_IN_3D -> 0f
                BubbleAnimationStyle.SLIDE_IN_SIDE -> 0f
                BubbleAnimationStyle.EXPAND_REVEAL -> 0.15f
                BubbleAnimationStyle.GENTLE_FADE -> 0f
            }
        )
    }
    val animOffsetY = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.POP_ZOOM -> 22f
                BubbleAnimationStyle.SLIDE_UP_FADE -> 40f
                BubbleAnimationStyle.ELASTIC_SPRING -> 10f
                BubbleAnimationStyle.FLIP_IN_3D -> 20f
                BubbleAnimationStyle.EXPAND_REVEAL -> 16f
                else -> 0f
            }
        )
    }
    val animOffsetX = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.SLIDE_IN_SIDE -> if (isOutgoing) 90f else -90f
                else -> 0f
            }
        )
    }
    val animRotationX = remember {
        Animatable(
            when (animationStyle) {
                BubbleAnimationStyle.FLIP_IN_3D -> 45f
                else -> 0f
            }
        )
    }

    LaunchedEffect(animationStyle) {
        when (animationStyle) {
            BubbleAnimationStyle.POP_ZOOM -> {
                animScaleX.snapTo(0.42f)
                animScaleY.snapTo(0.28f)
                animOffsetY.snapTo(22f)
                animAlpha.snapTo(0.35f)
                launch { animScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)) }
                launch { animScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
                launch { animOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)) }
                animAlpha.animateTo(1f, animationSpec = tween(140))
            }
            BubbleAnimationStyle.SLIDE_UP_FADE -> {
                animScaleX.snapTo(1f)
                animScaleY.snapTo(1f)
                animOffsetY.snapTo(40f)
                animAlpha.snapTo(0f)
                launch { animOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
                animAlpha.animateTo(1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            }
            BubbleAnimationStyle.ELASTIC_SPRING -> {
                animScaleX.snapTo(0.25f)
                animScaleY.snapTo(0.25f)
                animOffsetY.snapTo(10f)
                animAlpha.snapTo(0.1f)
                launch { animScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessLow)) }
                launch { animScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)) }
                launch { animOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                animAlpha.animateTo(1f, animationSpec = tween(120))
            }
            BubbleAnimationStyle.FLIP_IN_3D -> {
                animScaleX.snapTo(0.85f)
                animScaleY.snapTo(0.85f)
                animRotationX.snapTo(45f)
                animOffsetY.snapTo(20f)
                animAlpha.snapTo(0f)
                launch { animRotationX.animateTo(0f, animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
                launch { animScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                launch { animScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                launch { animOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                animAlpha.animateTo(1f, animationSpec = tween(180))
            }
            BubbleAnimationStyle.SLIDE_IN_SIDE -> {
                val sideOffset = if (isOutgoing) 90f else -90f
                animScaleX.snapTo(0.92f)
                animScaleY.snapTo(0.92f)
                animOffsetX.snapTo(sideOffset)
                animAlpha.snapTo(0f)
                launch { animOffsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) }
                launch { animScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                launch { animScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                animAlpha.animateTo(1f, animationSpec = tween(160))
            }
            BubbleAnimationStyle.EXPAND_REVEAL -> {
                animScaleX.snapTo(0.70f)
                animScaleY.snapTo(0.38f)
                animOffsetY.snapTo(16f)
                animAlpha.snapTo(0.15f)
                launch { animScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)) }
                launch { animScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)) }
                launch { animOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) }
                animAlpha.animateTo(1f, animationSpec = tween(150))
            }
            BubbleAnimationStyle.GENTLE_FADE -> {
                animScaleX.snapTo(0.96f)
                animScaleY.snapTo(0.96f)
                animAlpha.snapTo(0f)
                launch { animScaleX.animateTo(1f, animationSpec = tween(260, easing = LinearOutSlowInEasing)) }
                launch { animScaleY.animateTo(1f, animationSpec = tween(260, easing = LinearOutSlowInEasing)) }
                animAlpha.animateTo(1f, animationSpec = tween(240, easing = LinearOutSlowInEasing))
            }
        }
    }

    return this.graphicsLayer {
        scaleX = animScaleX.value
        scaleY = animScaleY.value
        alpha = animAlpha.value
        translationX = animOffsetX.value
        translationY = animOffsetY.value
        rotationX = animRotationX.value
        transformOrigin = TransformOrigin(if (isOutgoing) 0.98f else 0.02f, 1.0f)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageCallRow(
    message: ChatMessage,
    colors: ChatColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardBg)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
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
}

@Composable
private fun MessageSelectionBox(
    isSelectionMode: Boolean,
    isSelected: Boolean,
    colors: ChatColors,
    onToggleSelect: () -> Unit
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
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) com.chroma.studio.ui.components.LiquidGlassTokens.AppleSystemBlue else Color.Transparent)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = if (isSelected) Color.Transparent else colors.textMuted.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected || checkScale > 0.05f) {
                Box(modifier = Modifier.graphicsLayer { scaleX = checkScale; scaleY = checkScale }) {
                    LucideCheck(tint = Color.White, size = 13.dp)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MessageSwipeReplyIndicator(
    dragOffset: Float,
    colors: ChatColors
) {
    if (dragOffset != 0f) {
        val isSwipingLeft = dragOffset < 0
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp),
            contentAlignment = if (isSwipingLeft) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                LucideReply(
                    tint = colors.iosBlue.copy(alpha = (kotlin.math.abs(dragOffset) / 120f).coerceIn(0.2f, 1f)),
                    size = 20.dp
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MessageHeartBurstEffect(
    isActive: Boolean,
    scale: Float,
    alpha: Float
) {
    if (isActive) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(100f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
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
}

@Composable
private fun MessageReactionsPills(
    reactions: Map<String, Int>,
    userReaction: String?,
    isOutgoing: Boolean,
    colors: ChatColors,
    onReactionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .offset(
                y = (-9).dp,
                x = if (isOutgoing) (-4).dp else 4.dp
            )
            .zIndex(60f)
            .animateContentSize(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        reactions.forEach { (emoji, count) ->
            val isMyReaction = userReaction == emoji
            val badgeBg = if (colors.isLight) Color.White else Color(0xFF2C2C2E)
            val badgeBorderColor = if (isMyReaction && colors.isLight) Color(0x33007AFF) else (if (colors.isLight) Color(0x22000000) else Color(0x38FFFFFF))
            val badgeBorderWidth = 0.5.dp

            Row(
                modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .border(badgeBorderWidth, badgeBorderColor, RoundedCornerShape(8.dp))
                    .clickable { onReactionClick(emoji) }
                    .padding(
                        horizontal = if (count > 1) 6.dp else 4.5.dp,
                        vertical = 1.5.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                Text(text = emoji, fontSize = 11.sp)
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

@Composable
private fun MessageBubbleCard(
    message: ChatMessage,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    isNextSame: Boolean,
    searchQuery: String,
    senderDisplayName: String?,
    showSenderName: Boolean,
    callbacks: MessageRowCallbacks,
    isSelectionMode: Boolean,
    isHighlighted: Boolean,
    config: MessageBubbleConfig,
    colors: ChatColors,
    isFreshMessage: Boolean,
    showReactionDock: Boolean,
    onOpenReactionDock: () -> Unit,
    onCloseReactionDock: () -> Unit,
    onTriggerHeartBurst: () -> Unit,
    heartScale: Float,
    heartAlpha: Float,
    isHeartBurstActive: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val bubbleStyle = config.bubbleStyle
    val customCornerConfig = config.customCornerConfig
    val animationStyle = config.animationStyle

    val isVoiceOnly = message.mediaType == "voice" && message.text.isNullOrEmpty()
    val isMediaOnly = (message.mediaType in listOf("image", "image_stack", "sticker")) && message.text.isNullOrEmpty() && message.replyTo == null

    val highlightAnim = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightAnim.snapTo(0.40f)
            highlightAnim.animateTo(0f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
        }
    }

    Column(
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
        modifier = if (isMediaOnly) Modifier.width(280.dp) else if (isVoiceOnly) Modifier.widthIn(max = 245.dp) else Modifier.widthIn(max = 295.dp)
    ) {
        if (!isOutgoing && showSenderName && !isConsecutive && senderDisplayName != null) {
            Text(
                text = senderDisplayName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        val visual = remember(bubbleStyle, customCornerConfig, isOutgoing, isConsecutive, isNextSame, colors.isLight, colors.iosBlue) {
            resolveBubbleVisual(bubbleStyle, isOutgoing, isConsecutive, isNextSame, colors, customCornerConfig)
        }

        Box(
            contentAlignment = if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            val bubbleBackground = if (isMediaOnly) SolidColor(Color.Transparent) else visual.background
            val bubbleBorderWidth = if (isMediaOnly) 0.5.dp else visual.borderWidth
            val bubbleBorderColor = if (isMediaOnly) (if (colors.isLight) Color(0x18000000) else Color(0x2EFFFFFF)) else visual.borderColor
            val effectiveShape = if (isMediaOnly) RoundedCornerShape(18.dp) else visual.shape

            Box(
                modifier = Modifier
                    .messageEntrance(
                        isFreshMessage = isFreshMessage,
                        animationStyle = animationStyle,
                        isOutgoing = isOutgoing
                    )
                    .then(
                        if (visual.elevation > 0.dp && !isMediaOnly) {
                            Modifier.shadow(
                                elevation = visual.elevation,
                                shape = effectiveShape,
                                ambientColor = visual.ambientColor,
                                spotColor = visual.spotColor,
                                clip = false
                            )
                        } else Modifier
                    )
                    .clip(effectiveShape)
                    .background(brush = bubbleBackground, shape = effectiveShape)
                    .then(
                        if (visual.borderBrush != null && !isMediaOnly) {
                            Modifier.border(bubbleBorderWidth, visual.borderBrush, effectiveShape)
                        } else {
                            Modifier.border(bubbleBorderWidth, bubbleBorderColor, effectiveShape)
                        }
                    )
                    .pointerInput(message.id, isSelectionMode) {
                        if (isSelectionMode) {
                            detectTapGestures(onTap = { callbacks.onToggleSelect() })
                        } else {
                            detectTapGestures(
                                onDoubleTap = { onTriggerHeartBurst() },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    SoundManager.playReaction()
                                    onOpenReactionDock()
                                },
                                onTap = {
                                    if (message.status == "error") {
                                        callbacks.onRetry?.invoke()
                                    } else if (showReactionDock) {
                                        onCloseReactionDock()
                                    }
                                }
                            )
                        }
                    }
            ) {
                if (highlightAnim.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(effectiveShape)
                            .background(colors.iosBlue.copy(alpha = highlightAnim.value))
                    )
                }

                if (visual.hasTopSpecular && !isMediaOnly) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(effectiveShape)
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

                val hasTail = !isNextSame && !isMediaOnly && (bubbleStyle == BubbleStyleType.CLASSIC_IOS || bubbleStyle == BubbleStyleType.ROUNDED)
                MessageBubbleInnerContent(
                    message = message,
                    isOutgoing = isOutgoing,
                    visual = visual,
                    isVoiceOnly = isVoiceOnly,
                    isMediaOnly = isMediaOnly,
                    searchQuery = searchQuery,
                    isSelectionMode = isSelectionMode,
                    colors = colors,
                    showReactionDock = showReactionDock,
                    hasTail = hasTail,
                    onToggleSelect = callbacks.onToggleSelect,
                    onCloseReactionDock = onCloseReactionDock,
                    onOpenReactionDock = onOpenReactionDock,
                    onTriggerHeartBurst = onTriggerHeartBurst,
                    onImageClick = callbacks.onImageClick,
                    onImageStackClick = callbacks.onImageStackClick,
                    onPollVote = callbacks.onPollVote,
                    onPollRetractVote = callbacks.onPollRetractVote,
                    onClosePoll = callbacks.onClosePoll,
                    onViewPollVotes = callbacks.onViewPollVotes,
                    onQuoteClick = callbacks.onQuoteClick,
                    onRetry = callbacks.onRetry
                )
            }

            MessageHeartBurstEffect(
                isActive = isHeartBurstActive,
                scale = heartScale,
                alpha = heartAlpha
            )

            if (message.reactions.isNotEmpty()) {
                Box(modifier = Modifier.align(if (isOutgoing) Alignment.TopStart else Alignment.TopEnd)) {
                    MessageReactionsPills(
                        reactions = message.reactions,
                        userReaction = message.userReaction,
                        isOutgoing = isOutgoing,
                        colors = colors,
                        onReactionClick = callbacks.onReactionClick
                    )
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
fun MessageRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    isNextSame: Boolean = false,
    searchQuery: String = "",
    senderAvatar: String = "",
    senderDisplayName: String? = null,
    showSenderName: Boolean = false,
    callbacks: MessageRowCallbacks = MessageRowCallbacks(),
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    config: MessageBubbleConfig = MessageBubbleConfig(),
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    if (message.mediaType == "call") {
        MessageCallRow(message = message, colors = colors, modifier = modifier)
        return
    }

    val haptic = LocalHapticFeedback.current
    var showReactionDock by remember { mutableStateOf(false) }
    var isHeartBurstActive by remember { mutableStateOf(false) }
    var heartJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val heartScale = remember { Animatable(0.2f) }
    val heartAlpha = remember { Animatable(1f) }

    fun triggerHeartBurst() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        heartJob?.cancel()
        heartJob = coroutineScope.launch {
            try {
                isHeartBurstActive = true
                heartScale.snapTo(0.2f)
                heartAlpha.snapTo(1f)
                launch {
                    heartScale.animateTo(1.35f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
                }
                heartAlpha.animateTo(0f, animationSpec = tween(300))
            } finally {
                isHeartBurstActive = false
            }
        }
        callbacks.onDoubleTap()
    }

    val isFreshMessage = remember(message.id, message.timestamp) {
        val now = System.currentTimeMillis()
        message.timestamp > 0L && (now - message.timestamp) < 4000L
    }

    val rowBgColor = if (isSelectionMode && isSelected) colors.iosBlue.copy(alpha = 0.12f) else Color.Transparent
    val animatedStartPadding = if (isSelectionMode) 12.dp else (if (isOutgoing) 44.dp else 8.dp)
    val animatedEndPadding = if (isOutgoing) 8.dp else (if (isSelectionMode) 8.dp else 44.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBgColor)
            .clickable(enabled = isSelectionMode) { callbacks.onToggleSelect() }
            .padding(
                start = animatedStartPadding,
                end = animatedEndPadding,
                top = if (message.reactions.isNotEmpty()) 10.dp else (if (isConsecutive) 1.5.dp else 4.dp),
                bottom = 0.dp
            ),
        verticalAlignment = if (isSelectionMode) Alignment.CenterVertically else Alignment.Bottom
    ) {
        MessageSelectionBox(
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            colors = colors,
            onToggleSelect = callbacks.onToggleSelect
        )

        Column(
            modifier = Modifier.weight(1f, fill = isSelectionMode),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            MessageReactionDock(
                visible = showReactionDock,
                isOutgoing = isOutgoing,
                userReaction = message.userReaction,
                colors = colors,
                onReactionClick = callbacks.onReactionClick,
                onLongPress = callbacks.onLongPress,
                onReplyAction = callbacks.onReplyAction,
                onCloseDock = { showReactionDock = false }
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                MessageSwipeReplyIndicator(dragOffset = dragOffset.value, colors = colors)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(dragOffset.value) > 75f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        callbacks.onReplyAction()
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
                                    coroutineScope.launch { dragOffset.snapTo(target) }
                                }
                            )
                        },
                    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isOutgoing) {
                        if (!isNextSame) {
                            IosAvatarView(
                                name = senderDisplayName ?: "",
                                avatarUrl = senderAvatar,
                                size = 32.dp,
                                isOnline = false,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        } else {
                            Spacer(Modifier.width(32.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    MessageBubbleCard(
                        message = message,
                        isOutgoing = isOutgoing,
                        isConsecutive = isConsecutive,
                        isNextSame = isNextSame,
                        searchQuery = searchQuery,
                        senderDisplayName = senderDisplayName,
                        showSenderName = showSenderName,
                        callbacks = callbacks,
                        isSelectionMode = isSelectionMode,
                        isHighlighted = isHighlighted,
                        config = config,
                        colors = colors,
                        isFreshMessage = isFreshMessage,
                        showReactionDock = showReactionDock,
                        onOpenReactionDock = { showReactionDock = true },
                        onCloseReactionDock = { showReactionDock = false },
                        onTriggerHeartBurst = { triggerHeartBurst() },
                        heartScale = heartScale.value,
                        heartAlpha = heartAlpha.value,
                        isHeartBurstActive = isHeartBurstActive
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleInnerContent(
    message: ChatMessage,
    isOutgoing: Boolean,
    visual: BubbleVisualConfig,
    isVoiceOnly: Boolean,
    isMediaOnly: Boolean,
    searchQuery: String,
    isSelectionMode: Boolean,
    colors: ChatColors,
    showReactionDock: Boolean,
    hasTail: Boolean,
    onToggleSelect: () -> Unit,
    onCloseReactionDock: () -> Unit,
    onOpenReactionDock: () -> Unit,
    onTriggerHeartBurst: () -> Unit,
    onImageClick: (String) -> Unit,
    onImageStackClick: ((List<String>, Int) -> Unit)?,
    onPollVote: (Int) -> Unit,
    onPollRetractVote: (() -> Unit)? = null,
    onClosePoll: (() -> Unit)? = null,
    onViewPollVotes: ((PollData) -> Unit)?,
    onQuoteClick: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(
            start = if (isMediaOnly) 0.dp else if (isVoiceOnly) 8.dp else (if (!isOutgoing && hasTail) 16.dp else 11.dp),
            end = if (isMediaOnly) 0.dp else if (isVoiceOnly) 8.dp else (if (isOutgoing && hasTail) 16.dp else 11.dp),
            top = if (isMediaOnly) 0.dp else if (isVoiceOnly) 5.dp else 7.dp,
            bottom = if (isMediaOnly) 0.dp else if (isVoiceOnly) 5.dp else 7.dp
        )
    ) {
        // Forwarded Header
        if (message.isForwarded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "↪ Forwarded",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 1. Reply Quote Header (Apple frosted card with vertical accent indicator, interactive tap-to-scroll)
        if (message.replyTo != null) {
            val quotedMsgId = message.replyTo.messageId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onQuoteClick != null && !quotedMsgId.isNullOrBlank()) {
                        quotedMsgId?.let { onQuoteClick?.invoke(it) }
                    }
                    .background(
                        if (isOutgoing) Color.White.copy(alpha = 0.18f)
                        else if (colors.isLight) Color(0x0C000000)
                        else Color(0x18FFFFFF)
                    )
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (isOutgoing) Color.White else colors.iosBlue)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. Media Content
        when (message.mediaType) {
            "sticker" -> {
                val stickerUrl = message.localUri ?: message.mediaUrl
                if (!stickerUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = stickerUrl,
                            contentDescription = "Sticker",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onImageClick(stickerUrl) },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
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
                    showIntegratedMetadata = isVoiceOnly,
                    onDoubleTap = { onTriggerHeartBurst() }
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
                        onRetractVote = onPollRetractVote,
                        onClosePoll = onClosePoll,
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
                            .background(Color(0xFFF43F5E).copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color(0xFFF43F5E).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isOutgoing) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.04f))
                            .border(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.25f) else colors.borderSubtle, RoundedCornerShape(14.dp))
                            .clickable {
                                openChatFileIntent(context, message.localUri ?: message.mediaUrl, message.fileName)
                            }
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(colors.iosBlueSubtle, RoundedCornerShape(10.dp))
                                .border(0.5.dp, colors.iosBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
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

        // 3. Message Text with Markdown, Links, Formatted Text, Spoilers & Mentions
        if (!message.text.isNullOrEmpty()) {
            val uriHandler = LocalUriHandler.current
            var isExpanded by remember(message.id) { mutableStateOf(false) }
            var isLongMessage by remember(message.id) { mutableStateOf(false) }
            var revealedSpoilers by remember(message.id) { mutableStateOf(setOf<String>()) }
            var textLayoutResult by remember(message.id) { mutableStateOf<TextLayoutResult?>(null) }

            val annotatedText = remember(message.text, searchQuery, visual, isOutgoing, revealedSpoilers) {
                parseMarkdownToAnnotatedString(
                    text = message.text,
                    visual = visual,
                    isOutgoing = isOutgoing,
                    searchQuery = searchQuery,
                    revealedSpoilers = revealedSpoilers
                )
            }

            val textGestureModifier = Modifier.pointerInput(annotatedText, isSelectionMode) {
                detectTapGestures(
                    onTap = { pos ->
                        val offset = textLayoutResult?.getOffsetForPosition(pos)
                        if (offset != null) {
                            val urlAnnotations = annotatedText.getStringAnnotations("URL", offset, offset)
                            val url = urlAnnotations.firstOrNull()?.item
                            if (url != null) {
                                try {
                                    val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        "https://$url"
                                    } else {
                                        url
                                    }
                                    uriHandler.openUri(fullUrl)
                                    return@detectTapGestures
                                } catch (e: Exception) {
                                    // ignore malformed uri
                                }
                            }
                            val spoilerAnnotations = annotatedText.getStringAnnotations("SPOILER", offset, offset)
                            val spoilerId = spoilerAnnotations.firstOrNull()?.item
                            if (spoilerId != null) {
                                revealedSpoilers = revealedSpoilers + spoilerId
                                SoundManager.playPop()
                                return@detectTapGestures
                            }
                        }
                        if (isSelectionMode) {
                            onToggleSelect()
                        } else if (showReactionDock) {
                            onCloseReactionDock()
                        }
                    },
                    onDoubleTap = {
                        if (!isSelectionMode) {
                            onTriggerHeartBurst()
                        }
                    },
                    onLongPress = {
                        if (!isSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                            onOpenReactionDock()
                        }
                    }
                )
            }

            val isShortSingleLine = remember(message.text, message.replyTo, message.mediaType) {
                message.replyTo == null &&
                (message.mediaType.isNullOrEmpty() || message.mediaType == "text") &&
                !message.text.contains('\n') &&
                message.text.length <= 26 &&
                !message.text.contains("```")
            }

            if (isShortSingleLine) {
                // Compact Single-Line Capsule (Text & Timestamp inline, eliminating tombstone dome)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    Text(
                        text = annotatedText,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.01).sp,
                        fontFamily = visual.fontFamily,
                        modifier = textGestureModifier,
                        onTextLayout = { textLayoutResult = it }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(bottom = 1.5.dp)
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
                                size = 11.5.dp
                            )
                        }
                    }
                }
            } else {
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
                        modifier = textGestureModifier,
                        onTextLayout = { layoutResult ->
                            textLayoutResult = layoutResult
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
            }

            // 4. Timestamp & Read Receipts (Crisp iOS status tick)
            if (!isShortSingleLine && !isVoiceOnly && !isMediaOnly) {
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
    showIntegratedMetadata: Boolean = true,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalChatColors.current

    val currentPlayingId by AudioPlayerManager.currentPlayingId.collectAsState()
    val isGlobalPlaying by AudioPlayerManager.isPlaying.collectAsState()
    val isThisActive = currentPlayingId == messageId
    val isThisPlaying = isGlobalPlaying && isThisActive

    val playbackProgress by if (isThisActive) AudioPlayerManager.playbackProgress.collectAsState() else remember { mutableFloatStateOf(0f) }
    val playbackSpeed by if (isThisActive) AudioPlayerManager.playbackSpeed.collectAsState() else remember { mutableFloatStateOf(1f) }
    val currentPositionMs by if (isThisActive) AudioPlayerManager.currentPositionMs.collectAsState() else remember { mutableIntStateOf(0) }
    val totalDurationMs by if (isThisActive) AudioPlayerManager.totalDurationMs.collectAsState() else remember { mutableIntStateOf(0) }

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
        // Play/Pause Button (Apple style glass border Clean Gradient Circle)
        val playBtnBg = if (isOutgoing) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.16f)
                )
            )
        } else {
            if (colors.isLight) {
                Brush.linearGradient(
                    listOf(
                        colors.iosBlue.copy(alpha = 0.18f),
                        colors.iosBlue.copy(alpha = 0.08f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF3A3A3C),
                        Color(0xFF2C2C2E)
                    )
                )
            }
        }
        val playBtnBorder = if (isOutgoing) {
            Color.White.copy(alpha = 0.38f)
        } else {
            if (colors.isLight) colors.iosBlue.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.20f)
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(playBtnBg)
                .border(0.75.dp, playBtnBorder, CircleShape)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    AudioPlayerManager.play(messageId, audioPath, context)
                },
            contentAlignment = Alignment.Center
        ) {
            if (isThisPlaying) {
                LucidePause(
                    tint = if (isOutgoing) Color.White else colors.iosBlue,
                    size = 18.dp
                )
            } else {
                LucidePlay(
                    tint = if (isOutgoing) Color.White else colors.iosBlue,
                    size = 18.dp,
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

            val pulseProgress by if (isThisPlaying) {
                val pulseTransition = rememberInfiniteTransition(label = "voicePulse")
                pulseTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseProgress"
                )
            } else {
                remember { mutableFloatStateOf(0f) }
            }

            Canvas(
                modifier = Modifier
                    .height(26.dp)
                    .fillMaxWidth()
                    .pointerInput(messageId, isThisActive) {
                        detectTapGestures(
                            onTap = { offset ->
                                val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                if (isThisActive) {
                                    AudioPlayerManager.seekTo(progress)
                                } else {
                                    AudioPlayerManager.play(messageId, audioPath, context)
                                    AudioPlayerManager.seekTo(progress)
                                }
                            },
                            onDoubleTap = {
                                onDoubleTap?.invoke()
                            }
                        )
                    }
            ) {
                val totalBars = barHeights.size
                val spacingPx = 2.dp.toPx()
                val totalSpacing = spacingPx * (totalBars - 1)
                val barWidth = (size.width - totalSpacing) / totalBars
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())

                for (i in 0 until totalBars) {
                    val h = barHeights[i]
                    val barProgress = i.toFloat() / totalBars.toFloat()
                    val isPast = isThisActive && barProgress <= activeProgress

                    val pulseScale = if (isThisPlaying) {
                        val phase = (pulseProgress + (i % 6) / 6f) % 1f
                        0.75f + 0.35f * sin(phase * 2 * PI.toFloat())
                    } else 1f

                    val barHeightNorm = ((h / 100f) * pulseScale).coerceIn(0.18f, 1f)
                    val barPxHeight = size.height * barHeightNorm
                    val left = i * (barWidth + spacingPx)
                    val top = (size.height - barPxHeight) / 2f

                    val color = when {
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

                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barPxHeight),
                        cornerRadius = cornerRadius
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

                // Speed Indicator Pill (Apple 9999.dp Pill with Glass Border)
                val speedPillShape = RoundedCornerShape(9999.dp)
                Box(
                    modifier = Modifier
                        .clip(speedPillShape)
                        .background(if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.surfaceSecondary)
                        .border(
                            0.5.dp,
                            if (isOutgoing) Color.White.copy(alpha = 0.45f) else colors.iosBlue.copy(alpha = 0.25f),
                            speedPillShape
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            AudioPlayerManager.cycleSpeed()
                        }
                        .padding(horizontal = 7.dp, vertical = 2.dp),
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
    onRetractVote: (() -> Unit)? = null,
    onClosePoll: (() -> Unit)? = null,
    onViewVotes: (() -> Unit)? = null
) {
    val colors = LocalChatColors.current
    val totalVotes = poll.options.sumOf { it.votes }
    val maxVotes = poll.options.maxOfOrNull { it.votes } ?: 0
    val hasVoted = poll.options.any { it.isUserVoted }
    var showEndPollConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Poll Header Row: Category Badge + Status Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    poll.isClosed -> if (isOutgoing) Color.White.copy(alpha = 0.22f) else Color(0xFFFF9500).copy(alpha = 0.14f)
                    poll.isAnonymous -> if (isOutgoing) Color.White.copy(alpha = 0.22f) else Color(0xFF8B5CF6).copy(alpha = 0.14f)
                    else -> if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.iosBlue.copy(alpha = 0.14f)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (poll.isClosed) {
                        LucideLock(
                            tint = if (isOutgoing) Color.White else Color(0xFFFF9500),
                            size = 12.dp
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.ChartColumn,
                            contentDescription = null,
                            tint = if (isOutgoing) Color.White else (if (poll.isAnonymous) Color(0xFF8B5CF6) else colors.iosBlue),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = when {
                            poll.isClosed -> "FINAL RESULTS"
                            poll.isAnonymous -> "ANONYMOUS"
                            else -> "POLL"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = when {
                            poll.isClosed -> if (isOutgoing) Color.White else Color(0xFFFF9500)
                            poll.isAnonymous -> if (isOutgoing) Color.White else Color(0xFF8B5CF6)
                            else -> if (isOutgoing) Color.White else colors.iosBlue
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (!poll.isClosed) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOutgoing) Color.White.copy(alpha = 0.9f) else Color(0xFF34C759))
                    )
                }
                Text(
                    text = when {
                        poll.isClosed -> "Voting ended"
                        poll.allowMultipleAnswers -> "Select multiple"
                        else -> "Select one"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted
                )
            }
        }

        // 2. Poll Question
        Text(
            text = poll.question,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 21.sp,
            color = if (isOutgoing) Color.White else colors.textDark
        )

        // 3. Modern Fill-Meter Option Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            poll.options.forEachIndexed { index, option ->
                val percentage = if (totalVotes == 0) 0f else (option.votes.toFloat() / totalVotes)
                val animatedProgress by animateFloatAsState(
                    targetValue = if (hasVoted || poll.isClosed) percentage else 0f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "poll_fill_$index"
                )
                val isSelected = option.isUserVoted
                val isLeading = maxVotes > 0 && option.votes == maxVotes && totalVotes > 0
                val isWinner = poll.isClosed && isLeading
                val tileShape = RoundedCornerShape(13.dp)

                val baseBg = if (isOutgoing) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E).copy(alpha = 0.6f)
                }

                val borderColor = when {
                    isWinner -> Color(0xFF34C759).copy(alpha = 0.75f)
                    isSelected -> if (isOutgoing) Color.White else colors.iosBlue
                    else -> if (isOutgoing) Color.White.copy(alpha = 0.15f) else colors.borderSubtle
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(tileShape)
                        .background(baseBg)
                        .border(
                            width = if (isSelected || isWinner) 1.5.dp else 0.5.dp,
                            color = borderColor,
                            shape = tileShape
                        )
                        .clickable(enabled = !poll.isClosed) {
                            SoundManager.playReaction()
                            onVote(index)
                        }
                ) {
                    // Animated Fill Layer
                    if (hasVoted || poll.isClosed) {
                        val fillBrush = when {
                            isWinner -> Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF34C759).copy(alpha = if (isOutgoing) 0.40f else 0.28f),
                                    Color(0xFF30D158).copy(alpha = if (isOutgoing) 0.25f else 0.16f)
                                )
                            )
                            isSelected -> Brush.horizontalGradient(
                                if (isOutgoing) listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.22f)
                                ) else listOf(
                                    colors.iosBlue.copy(alpha = 0.24f),
                                    colors.iosBlue.copy(alpha = 0.14f)
                                )
                            )
                            else -> Brush.horizontalGradient(
                                if (isOutgoing) listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.12f)
                                ) else listOf(
                                    (if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF3A3A3C)).copy(alpha = 0.7f),
                                    (if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF3A3A3C)).copy(alpha = 0.5f)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .matchParentSize()
                                .background(fillBrush)
                        )
                    }

                    // Content Layer: Selection control + Option Text + Right side stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Checkbox/Radio + Option Text
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!poll.isClosed) {
                                if (poll.allowMultipleAnswers) {
                                    LiquidGlassCheckbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            SoundManager.playReaction()
                                            onVote(index)
                                        },
                                        size = 19.dp
                                    )
                                } else {
                                    LiquidGlassRadio(
                                        selected = isSelected,
                                        onClick = {
                                            SoundManager.playReaction()
                                            onVote(index)
                                        },
                                        size = 19.dp
                                    )
                                }
                            } else {
                                if (isWinner) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34C759)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LucideCheck(tint = Color.White, size = 13.dp)
                                    }
                                } else if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isOutgoing) Color.White else colors.iosBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LucideCheck(
                                            tint = if (isOutgoing) colors.iosBlue else Color.White,
                                            size = 13.dp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, if (isOutgoing) Color.White.copy(alpha = 0.35f) else colors.borderSubtle, CircleShape)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = option.text,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isLeading) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isOutgoing) Color.White else colors.textDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isWinner) {
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = Color(0xFF34C759).copy(alpha = if (isOutgoing) 0.35f else 0.16f),
                                            border = BorderStroke(0.5.dp, Color(0xFF34C759).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🏆 WINNER",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOutgoing) Color.White else Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right: Voters Avatars + Percentage / Vote count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!poll.isAnonymous && option.voters.isNotEmpty()) {
                                val visibleVoters = option.voters.take(3)
                                Row(modifier = Modifier.padding(end = 2.dp)) {
                                    visibleVoters.forEachIndexed { voterIdx, voter ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (-6 * voterIdx).dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.dp,
                                                    if (isOutgoing) colors.iosBlue else Color.White,
                                                    CircleShape
                                                )
                                                .background(colors.surfaceSecondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IosAvatarView(
                                                name = voter.name,
                                                avatarUrl = voter.avatar,
                                                size = 18.dp,
                                                isOnline = false
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasVoted || poll.isClosed) {
                                val pct = (percentage * 100f).roundToInt()
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isLeading) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isOutgoing) Color.White else (
                                            if (isWinner) Color(0xFF2E7D32)
                                            else if (isSelected) colors.iosBlue
                                            else colors.textDark
                                        )
                                    )
                                    if (option.votes > 0) {
                                        Text(
                                            text = "${option.votes} vote${if (option.votes == 1) "" else "s"}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else colors.textMuted
                                        )
                                    }
                                }
                            } else if (option.votes > 0) {
                                Text(
                                    text = "${option.votes} vote${if (option.votes == 1) "" else "s"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Bottom Stats & Action Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Lucide.Users,
                    contentDescription = null,
                    tint = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = when {
                        totalVotes == 0 -> "No votes yet"
                        totalVotes == 1 -> "1 vote"
                        else -> "$totalVotes votes"
                    } + if (poll.isClosed) " • Closed" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textMuted
                )
            }

            // Right Actions: Retract Vote + End Poll (creator) + View Votes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Retract Vote Button
                if (hasVoted && !poll.isClosed && onRetractVote != null) {
                    val pillShape = RoundedCornerShape(9999.dp)
                    Surface(
                        shape = pillShape,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else Color(0xFFFF3B30).copy(alpha = 0.10f),
                        border = BorderStroke(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.35f) else Color(0xFFFF3B30).copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            SoundManager.playReaction()
                            onRetractVote()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
                        ) {
                            Text(
                                text = "Retract",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOutgoing) Color.White else Color(0xFFFF3B30)
                            )
                        }
                    }
                }

                // End Poll Button (for poll creator if poll is open)
                if (isOutgoing && !poll.isClosed && onClosePoll != null) {
                    val pillShape = RoundedCornerShape(9999.dp)
                    Surface(
                        shape = pillShape,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else colors.surfaceSecondary,
                        border = BorderStroke(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.35f) else colors.borderSubtle),
                        modifier = Modifier.clickable {
                            SoundManager.playReaction()
                            showEndPollConfirmDialog = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
                        ) {
                            Text(
                                text = "End Poll",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOutgoing) Color.White else colors.textDark
                            )
                        }
                    }
                }

                // "View Votes" Apple Specular Glass Pill Button
                val viewVotesShape = RoundedCornerShape(9999.dp)
                Surface(
                    shape = viewVotesShape,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.iosBlue.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, if (isOutgoing) Color.White.copy(alpha = 0.45f) else colors.iosBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(enabled = onViewVotes != null) {
                        SoundManager.playReaction()
                        onViewVotes?.invoke()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.5.dp)
                    ) {
                        Text(
                            text = "View Votes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOutgoing) Color.White else colors.iosBlue
                        )
                        LucideChevronRight(
                            tint = if (isOutgoing) Color.White else colors.iosBlue,
                            size = 13.dp
                        )
                    }
                }
            }
        }
    }

    if (showEndPollConfirmDialog) {
        IosAlertDialog(
            title = "End Poll?",
            message = "Voting will be closed and final results will be locked for all participants.",
            actions = listOf(
                IosAlertAction(
                    title = "Cancel",
                    isCancel = true,
                    onClick = { showEndPollConfirmDialog = false }
                ),
                IosAlertAction(
                    title = "End Poll",
                    isDestructive = true,
                    onClick = {
                        showEndPollConfirmDialog = false
                        onClosePoll?.invoke()
                    }
                )
            ),
            onDismiss = { showEndPollConfirmDialog = false }
        )
    }
}

@Immutable
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

class AppleMessageBubbleShape(
    val isOutgoing: Boolean,
    val hasTail: Boolean = true,
    val isConsecutive: Boolean = false,
    val isNextSame: Boolean = false,
    val cornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    val groupedCornerRadius: androidx.compose.ui.unit.Dp = 7.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(Rect.Zero)

        val maxR = with(density) { cornerRadius.toPx() }
        val smallR = with(density) { groupedCornerRadius.toPx() }
        val effectiveOutgoing = if (layoutDirection == LayoutDirection.Rtl) !isOutgoing else isOutgoing
        val shouldDrawTail = hasTail && !isNextSame

        val baseR = min(maxR, min(w / 2f, h / 2f))
        val pinchedR = min(smallR, baseR)

        // Determine individual corner radii based on burst grouping
        val topLeftR = if (!effectiveOutgoing && isConsecutive) pinchedR else baseR
        val bottomLeftR = if (!effectiveOutgoing && isNextSame) pinchedR else baseR
        val topRightR = if (effectiveOutgoing && isConsecutive) pinchedR else baseR
        val bottomRightR = if (effectiveOutgoing && isNextSame) pinchedR else baseR

        val scale = baseR / 20f
        val tailStart = maxOf(baseR, h - 18f * scale)

        val path = Path().apply {
            if (!shouldDrawTail) {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, w, h),
                        topLeft = CornerRadius(topLeftR, topLeftR),
                        topRight = CornerRadius(topRightR, topRightR),
                        bottomRight = CornerRadius(bottomRightR, bottomRightR),
                        bottomLeft = CornerRadius(bottomLeftR, bottomLeftR)
                    )
                )
                return@apply
            }

            if (effectiveOutgoing) {
                // Top-left to top-right
                moveTo(topLeftR, 0f)
                lineTo(w - topRightR, 0f)
                arcTo(Rect(w - 2 * topRightR, 0f, w, 2 * topRightR), -90f, 90f, false)

                // Right edge down to tail start
                lineTo(w, tailStart)

                // Sweeping iMessage bottom-right tail tip & inner notch
                cubicTo(
                    w, h - 7.5f * scale,
                    w + 2.2f * scale, h - 2.0f * scale,
                    w + 6.5f * scale, h - 0.3f * scale
                )
                cubicTo(
                    w + 1.4f * scale, h + 0.6f * scale,
                    w - 2.8f * scale, h - 0.8f * scale,
                    w - 5.8f * scale, h - 4.0f * scale
                )
                cubicTo(
                    w - 8.4f * scale, h - 1.2f * scale,
                    w - 11.8f * scale, h,
                    w - 16.0f * scale, h
                )

                // Bottom edge & left corners
                lineTo(bottomLeftR, h)
                arcTo(Rect(0f, h - 2 * bottomLeftR, 2 * bottomLeftR, h), 90f, 90f, false)
                lineTo(0f, topLeftR)
                arcTo(Rect(0f, 0f, 2 * topLeftR, 2 * topLeftR), 180f, 90f, false)
                close()
            } else {
                // Top-left to bottom-right
                moveTo(topLeftR, 0f)
                lineTo(w - topRightR, 0f)
                arcTo(Rect(w - 2 * topRightR, 0f, w, 2 * topRightR), -90f, 90f, false)
                lineTo(w, h - bottomRightR)
                arcTo(Rect(w - 2 * bottomRightR, h - 2 * bottomRightR, w, h), 0f, 90f, false)

                // Bottom edge to left tail notch
                lineTo(16.0f * scale, h)

                // Sweeping iMessage bottom-left tail tip & outer curve
                cubicTo(
                    11.8f * scale, h,
                    8.4f * scale, h - 1.2f * scale,
                    5.8f * scale, h - 4.0f * scale
                )
                cubicTo(
                    2.8f * scale, h - 0.8f * scale,
                    -1.4f * scale, h + 0.6f * scale,
                    -6.5f * scale, h - 0.3f * scale
                )
                cubicTo(
                    -2.2f * scale, h - 2.0f * scale,
                    0f, h - 7.5f * scale,
                    0f, tailStart
                )

                // Left edge up to top-left corner
                lineTo(0f, topLeftR)
                arcTo(Rect(0f, 0f, 2 * topLeftR, 2 * topLeftR), 180f, 90f, false)
                close()
            }
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppleMessageBubbleShape) return false
        return isOutgoing == other.isOutgoing &&
                hasTail == other.hasTail &&
                isConsecutive == other.isConsecutive &&
                isNextSame == other.isNextSame &&
                cornerRadius == other.cornerRadius &&
                groupedCornerRadius == other.groupedCornerRadius
    }

    override fun hashCode(): Int {
        var result = isOutgoing.hashCode()
        result = 31 * result + hasTail.hashCode()
        result = 31 * result + isConsecutive.hashCode()
        result = 31 * result + isNextSame.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + groupedCornerRadius.hashCode()
        return result
    }
}

fun getIosClusterShape(isOutgoing: Boolean, isConsecutive: Boolean, isNextSame: Boolean): RoundedCornerShape {
    return if (isOutgoing) {
        when {
            !isConsecutive && !isNextSame -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            !isConsecutive && isNextSame -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
            isConsecutive && isNextSame -> RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
            else -> RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        }
    } else {
        when {
            !isConsecutive && !isNextSame -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
            !isConsecutive && isNextSame -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
            isConsecutive && isNextSame -> RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
            else -> RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
        }
    }
}

fun resolveBubbleVisual(
    style: BubbleStyleType,
    isOutgoing: Boolean,
    isConsecutive: Boolean,
    isNextSame: Boolean = false,
    colors: com.chroma.studio.chat.ui.theme.ChatColors,
    customCorners: CustomCornerConfig = CustomCornerConfig()
): BubbleVisualConfig {
    val base = resolveBaseBubbleVisual(style, isOutgoing, isConsecutive, isNextSame, colors)
    if (!customCorners.customEnabled || style == BubbleStyleType.CLASSIC_IOS || style == BubbleStyleType.ROUNDED) {
        return base
    }

    val tStart = customCorners.topStart.dp
    val tEnd = customCorners.topEnd.dp
    val bStart = customCorners.bottomStart.dp
    val bEnd = customCorners.bottomEnd.dp

    val customShape: androidx.compose.ui.graphics.Shape = when (customCorners.pointyCornerStyle) {
        PointyCornerStyle.CHAMFERED_CUT -> {
            if (isOutgoing) {
                CutCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = if (isNextSame) 6.dp else 2.dp)
            } else {
                CutCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = if (isNextSame) 6.dp else 2.dp)
            }
        }
        PointyCornerStyle.SHARP_ZERO -> {
            if (isOutgoing) {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = if (isNextSame) 6.dp else 0.dp)
            } else {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = if (isNextSame) 6.dp else 0.dp)
            }
        }
        PointyCornerStyle.SUBTLE_ROUNDED -> {
            if (isOutgoing) {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomStart = bStart, bottomEnd = if (isNextSame) 6.dp else 2.dp)
            } else {
                RoundedCornerShape(topStart = tStart, topEnd = tEnd, bottomEnd = bEnd, bottomStart = if (isNextSame) 6.dp else 2.dp)
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
    isNextSame: Boolean = false,
    colors: com.chroma.studio.chat.ui.theme.ChatColors
): BubbleVisualConfig {
    val isLight = colors.isLight

    return when (style) {
        BubbleStyleType.ROUNDED -> {
            val shape = AppleMessageBubbleShape(
                isOutgoing = isOutgoing,
                hasTail = !isNextSame,
                isConsecutive = isConsecutive,
                isNextSame = isNextSame
            )
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
            val shape = AppleMessageBubbleShape(
                isOutgoing = isOutgoing,
                hasTail = !isNextSame,
                isConsecutive = isConsecutive,
                isNextSame = isNextSame
            )
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
                when {
                    !isConsecutive && !isNextSame -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                    !isConsecutive && isNextSame -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 5.dp)
                    isConsecutive && isNextSame -> RoundedCornerShape(topStart = 16.dp, topEnd = 5.dp, bottomStart = 16.dp, bottomEnd = 5.dp)
                    else -> RoundedCornerShape(topStart = 16.dp, topEnd = 5.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                }
            } else {
                when {
                    !isConsecutive && !isNextSame -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                    !isConsecutive && isNextSame -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 5.dp)
                    isConsecutive && isNextSame -> RoundedCornerShape(topStart = 5.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 5.dp)
                    else -> RoundedCornerShape(topStart = 5.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                }
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

    var viewMode by remember { mutableStateOf(if (count > 1) "stack" else "single") }
    var activeIndex by remember { mutableStateOf(0) }
    var showHeartPop by remember { mutableStateOf(false) }
    val topCardDragX = remember { Animatable(0f) }

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
            // APPLE MESSAGES INTERACTIVE CARD STACK
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
                        1 -> if (cardIdx % 2 == 0) -3.8f else 3.8f
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
                        1 -> if (cardIdx % 2 == 0) (-6).dp else 6.dp
                        else -> if (cardIdx % 2 == 0) 7.dp else (-7).dp
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

                    val cardShape = RoundedCornerShape(18.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = animatedXOffset, y = animatedYOffset)
                            .graphicsLayer {
                                translationX = if (depth == 0) topCardDragX.value else 0f
                                rotationZ = animatedRotation + if (depth == 0) (topCardDragX.value * 0.045f) else 0f
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
                                        .pointerInput(activeIndex, count) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { },
                                                onDragEnd = {
                                                    coroutineScope.launch {
                                                        if (topCardDragX.value < -70f) {
                                                            topCardDragX.animateTo(-300f, tween(140))
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            SoundManager.playReaction()
                                                            activeIndex = (activeIndex + 1) % count
                                                            topCardDragX.snapTo(0f)
                                                        } else if (topCardDragX.value > 70f) {
                                                            topCardDragX.animateTo(300f, tween(140))
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            SoundManager.playReaction()
                                                            activeIndex = (activeIndex - 1 + count) % count
                                                            topCardDragX.snapTo(0f)
                                                        } else {
                                                            topCardDragX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        topCardDragX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                                                    }
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    coroutineScope.launch {
                                                        topCardDragX.snapTo(topCardDragX.value + dragAmount)
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

                // Double-Tap Popping Heart Animation
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

                // Top-Right: Apple Messages Stack Badge (Layered Photo Icon + "1 of N" counter)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable {
                            // Tap stack counter to cycle to next photo
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playReaction()
                            activeIndex = (activeIndex + 1) % count
                        }
                        .padding(horizontal = 9.dp, vertical = 4.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Stacked dual-rectangle icon (Apple Messages Photo Stack)
                        Box(modifier = Modifier.size(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .offset(x = 2.5.dp, y = (-1.5).dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(2.5.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .offset(x = 0.dp, y = 2.dp)
                                    .background(Color.Black)
                                    .border(1.dp, Color.White, RoundedCornerShape(2.5.dp))
                            )
                        }
                        Text(
                            text = "${activeIndex + 1} of $count",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Top-Left: View Mode Switcher ("📚 Stack" / "⊞ Grid")
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewMode = "grid"
                        }
                        .padding(horizontal = 8.dp, vertical = 4.5.dp)
                ) {
                    Text(
                        text = "⊞ Grid",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }

                // Overlay Floating Timestamp Badge (Bottom End)
                if (isMediaOnly && time.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 10.dp)
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

                // Bottom-Center: Apple Messages dot indicators (● ○ ○)
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                    .clickable {
                        openChatFileIntent(context, file.uri, file.name)
                    }
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

/**
 * Helper to open an attached file or document using the system default viewer.
 */
fun openChatFileIntent(context: android.content.Context, uriString: String?, fileName: String?) {
    try {
        if (!uriString.isNullOrEmpty()) {
            val uri = android.net.Uri.parse(uriString)
            val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
            val mimeType = context.contentResolver.getType(uri) ?: when (extension) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "txt" -> "text/plain"
                "apk" -> "application/vnd.android.package-archive"
                "mp3", "wav", "m4a", "aac", "ogg" -> "audio/*"
                "mp4", "mkv", "mov" -> "video/*"
                "jpg", "jpeg", "png", "webp", "gif" -> "image/*"
                else -> "*/*"
            }
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            android.widget.Toast.makeText(context, "Opening ${fileName ?: "file"}", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No app available to open this file", android.widget.Toast.LENGTH_SHORT).show()
    }
}


