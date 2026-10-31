package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.ReplyQuote
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.launch

const val MAX_MESSAGE_CHARACTERS = 4096

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    replyQuote: ReplyQuote?,
    onCancelReply: () -> Unit,
    isBlocked: Boolean,
    onCameraClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onFileClick: () -> Unit,
    onPollClick: () -> Unit,
    onStickerClick: () -> Unit = {},
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    onDictateClick: () -> Unit = {},
    isDictating: Boolean = false,
    dictationRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showAttachmentMenu) {
        showAttachmentMenu = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // ── Liquid-glass action menu (GlassIcons) floating over the wallpaper ──
        AnimatedVisibility(
            visible = showAttachmentMenu,
            enter = expandVertically(
                animationSpec = tween(
                    durationMillis = ACTION_MENU_ANIM_DURATION_MS,
                    easing = ActionMenuPopEasing
                )
            ) + fadeIn(animationSpec = tween(260)),
            exit = shrinkVertically(
                animationSpec = tween(
                    durationMillis = ACTION_MENU_CLOSE_DURATION_MS,
                    easing = ActionMenuSmoothOutEasing
                )
            ) + fadeOut(animationSpec = tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                GlassIconsActionMenu(
                    isOpen = showAttachmentMenu,
                    onOptionSelected = { option ->
                        showAttachmentMenu = false
                        when (option) {
                            ActionMenuOption.CAMERA -> onCameraClick()
                            ActionMenuOption.PHOTOS -> onPhotoClick()
                            ActionMenuOption.FILES -> onFileClick()
                            ActionMenuOption.POLL -> onPollClick()
                            ActionMenuOption.STICKER -> onStickerClick()
                        }
                    }
                )
            }
        }

        Surface(
            color = colors.headerBg,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

            // ── Top hairline separator ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.separator)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {

                // ── Reply Preview Banner ──────────────────────────
                AnimatedVisibility(
                    visible = replyQuote != null,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                ) {
                    if (replyQuote != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(colors.iosBlue, RoundedCornerShape(2.dp))
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Replying to ${replyQuote.author}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.iosBlue,
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = replyQuote.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onCancelReply() },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCloseX(tint = colors.textMuted, size = 13.dp)
                            }
                        }
                    }
                }

                // ── Emoji Picker ──────────────────────────────────
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) + scaleIn(initialScale = 0.92f) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f) + fadeOut()
                ) {
                    val emojiList = listOf(
                        "😀", "😂", "🙌", "🔥", "❤️", "👍",
                        "🎉", "🚀", "✨", "🤔", "👏", "💯"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF81C1C1E)
                            )
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.height(80.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(emojiList) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onInputTextChange(inputText + emoji)
                                            showEmojiPicker = false
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                // ── Main Input Row ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Synchronized Rotating Action Menu Trigger Button (Blue '+' that spins to 'x') ──
                    SynchronizedActionMenuTriggerButton(
                        isOpen = showAttachmentMenu,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAttachmentMenu = !showAttachmentMenu
                            if (showAttachmentMenu) showEmojiPicker = false
                        }
                    )

                    // Text input capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isDictating) colors.iosBlueSubtle
                                else (if (colors.isLight) Color(0xFFFFFFFF) else colors.surfaceTertiary)
                            )
                            .border(
                                if (isDictating) 1.5.dp else 0.5.dp,
                                if (isDictating) colors.iosBlue
                                else (if (colors.isLight) Color(0xFFD0D0D5) else Color(0xFF3A3A3C)),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { newText ->
                                if (showAttachmentMenu) showAttachmentMenu = false
                                if (newText.length <= MAX_MESSAGE_CHARACTERS) {
                                    onInputTextChange(newText)
                                } else {
                                    onInputTextChange(newText.take(MAX_MESSAGE_CHARACTERS))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isBlocked,
                            maxLines = 5,
                            cursorBrush = SolidColor(colors.iosBlue),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = colors.textDark,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = if (isBlocked) "You can't message this contact"
                                                   else if (isDictating) "Listening... Speak now"
                                                   else "Type a message...",
                                            fontSize = 16.sp,
                                            fontWeight = if (isDictating) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isDictating) colors.iosBlue else colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Remaining characters indicator (when message is long)
                        if (inputText.length > 3000) {
                            Text(
                                text = "${inputText.length}/$MAX_MESSAGE_CHARACTERS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (inputText.length >= MAX_MESSAGE_CHARACTERS) Color(0xFFFF3B30) else colors.textMuted
                            )
                        }

                        // Speech Dictation button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDictateClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDictating) {
                                PhosphorSparkleIcon(tint = colors.iosBlue, size = 18.dp)
                            } else {
                                PhosphorVoiceMicIcon(tint = colors.textMuted, size = 18.dp)
                            }
                        }

                        // Emoji toggle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    showEmojiPicker = !showEmojiPicker
                                    if (showEmojiPicker) showAttachmentMenu = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorSmileyIcon(
                                tint = if (showEmojiPicker) colors.iosBlue else colors.textMuted,
                                size = 20.dp
                            )
                        }
                    }

                    // Voice / Send Action Button (Animated toggle between Voice Mic and Send Arrow)
                    val hasSendContent = inputText.trim().isNotEmpty()
                    val sendBtnScale = remember { Animatable(1f) }
                    val sendBtnRotation = remember { Animatable(0f) }
                    val sendBtnScope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (hasSendContent) {
                                    sendBtnScope.launch {
                                        sendBtnScale.snapTo(0.68f)
                                        sendBtnRotation.snapTo(-22f)
                                        launch {
                                            sendBtnScale.animateTo(
                                                1f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.48f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                        launch {
                                            sendBtnRotation.animateTo(
                                                0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.52f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                    onSendClick()
                                } else {
                                    onVoiceClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = hasSendContent,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(140)))
                                    .togetherWith(scaleOut(tween(100)) + fadeOut(tween(100)))
                            },
                            label = "sendVoiceToggle"
                        ) { hasText ->
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = sendBtnScale.value
                                    scaleY = sendBtnScale.value
                                    rotationZ = sendBtnRotation.value
                                }
                            ) {
                                if (hasText) {
                                    PhosphorPaperPlaneTilt(tint = colors.iosBlue, size = 24.dp)
                                } else {
                                    PhosphorVoiceMicIcon(tint = colors.iosBlue, size = 24.dp)
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



