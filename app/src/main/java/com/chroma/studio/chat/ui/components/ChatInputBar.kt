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

    val plusRotate by animateFloatAsState(
        targetValue = if (showAttachmentMenu) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "plusRotate"
    )

    Surface(
        color = colors.headerBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

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
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceTertiary)
                                    .clickable { onCancelReply() },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCloseX(tint = colors.textMuted, size = 11.dp)
                            }
                        }
                    }
                }

                // ── Attachment Quick Menu ─────────────────────────
                AnimatedVisibility(
                    visible = showAttachmentMenu,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(150)) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttachmentItem(
                            icon = { PhosphorCameraIcon(tint = Color.White, size = 20.dp) },
                            title = "Camera",
                            bgColor = Color(0xFF007AFF),
                            onClick = { showAttachmentMenu = false; onCameraClick() }
                        )
                        AttachmentItem(
                            icon = { PhosphorPhotoIcon(tint = Color.White, size = 20.dp) },
                            title = "Photos",
                            bgColor = Color(0xFF34C759),
                            onClick = { showAttachmentMenu = false; onPhotoClick() }
                        )
                        AttachmentItem(
                            icon = { UiverseFolderUploadIcon(tint = Color.White, size = 20.dp) },
                            title = "Files",
                            bgColor = Color(0xFFFF9500),
                            onClick = { showAttachmentMenu = false; onFileClick() }
                        )
                        AttachmentItem(
                            icon = { PhosphorPollIcon(tint = Color.White, size = 20.dp) },
                            title = "Poll",
                            bgColor = Color(0xFFAF52DE),
                            onClick = { showAttachmentMenu = false; onPollClick() }
                        )
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
                    // Attachment toggle button (Blue circular button with white plus icon)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.iosBlue)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAttachmentMenu = !showAttachmentMenu
                                if (showAttachmentMenu) showEmojiPicker = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorPlusIcon(
                            tint = Color.White,
                            size = 20.dp,
                            modifier = Modifier.graphicsLayer { rotationZ = plusRotate }
                        )
                    }

                    // Text input capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
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
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = if (isBlocked) "You can't message this contact"
                                               else if (isDictating) "Listening... Speak now"
                                               else "Type a message...",
                                        fontSize = 16.sp,
                                        fontWeight = if (isDictating) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isDictating) colors.iosBlue else colors.textMuted
                                    )
                                }
                                innerTextField()
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

                    // Voice / Send button (Blue tilted paper plane matching screenshot)
                    val hasSendContent = inputText.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (hasSendContent) {
                                    onSendClick()
                                } else {
                                    onVoiceClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorPaperPlaneTilt(tint = colors.iosBlue, size = 24.dp)
                    }
                }
            }
        }
    }
}

// ── Attachment Grid Item ──────────────────────────────────

@Composable
private fun AttachmentItem(
    icon: @Composable () -> Unit,
    title: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textDark,
            lineHeight = 13.sp
        )
    }
}

