package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatAdapters
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.chat.data.PollData
import com.chroma.studio.chat.data.PollVoter
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.AppleChatGraphic
import com.chroma.studio.ui.components.AppleEmptyStateView
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.viewmodel.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun toggleOrUpdateReactionHelper(msg: ChatMessage, emoji: String): ChatMessage {
    val prevReaction = msg.userReaction
    val currentReactions = msg.reactions.toMutableMap()

    if (prevReaction == emoji) {
        val count = (currentReactions[emoji] ?: 1) - 1
        if (count <= 0) {
            currentReactions.remove(emoji)
        } else {
            currentReactions[emoji] = count
        }
        return msg.copy(
            reactions = currentReactions.filterValues { it > 0 },
            userReaction = null
        )
    } else {
        if (prevReaction != null) {
            val count = (currentReactions[prevReaction] ?: 1) - 1
            if (count <= 0) {
                currentReactions.remove(prevReaction)
            } else {
                currentReactions[prevReaction] = count
            }
        }
        val currentCount = currentReactions[emoji] ?: 0
        currentReactions[emoji] = currentCount + 1
        return msg.copy(
            reactions = currentReactions,
            userReaction = emoji
        )
    }
}


fun addOrEnsureReactionHelper(msg: ChatMessage, emoji: String): ChatMessage {
    val prevReaction = msg.userReaction
    if (prevReaction == emoji) {
        return msg // Already reacted with this emoji, keep it! Never remove or dislike.
    }
    val currentReactions = msg.reactions.toMutableMap()
    if (prevReaction != null) {
        val count = (currentReactions[prevReaction] ?: 1) - 1
        if (count <= 0) {
            currentReactions.remove(prevReaction)
        } else {
            currentReactions[prevReaction] = count
        }
    }
    val currentCount = currentReactions[emoji] ?: 0
    currentReactions[emoji] = currentCount + 1
    return msg.copy(
        reactions = currentReactions,
        userReaction = emoji
    )
}

@Composable
fun ActiveChatMessageStream(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    activeChat: ChatConversation,
    allMessages: List<ChatMessage>,
    visibleMessageCount: Int,
    currentUser: User,
    currentUid: String,
    unreadMessageThreshold: Int,
    typingStatuses: Map<String, Map<String, String>>,
    isTyping: Boolean,
    isMessageSelectionMode: Boolean,
    selectedMessageIds: SnapshotStateList<String>,
    starredMessageIds: Set<String>,
    searchInsideQuery: String,
    activeChatId: String,
    chatViewModel: ChatViewModel?,
    coroutineScope: CoroutineScope,
    haptic: HapticFeedback,
    clipboardManager: ClipboardManager,
    showToast: (String) -> Unit,
    bubbleConfig: MessageBubbleConfig = MessageBubbleConfig(),
    onChatsUpdate: ((List<ChatConversation>) -> List<ChatConversation>) -> Unit,
    onSelectContextMenuMessage: (ChatMessage) -> Unit,
    onStartReply: (ChatMessage) -> Unit,
    onStartEdit: (ChatMessage) -> Unit,
    onPinMessage: (ChatMessage) -> Unit,
    onDeleteMessagePending: (ChatMessage) -> Unit,
    onImageClick: (String) -> Unit,
    onImageStackClick: (List<String>, Int) -> Unit,
    onViewPollVotes: (PollData) -> Unit,
    onNavigateToProfile: ((String) -> Unit)?,
    onExpandVisibleCount: (Int) -> Unit
) {
    val colors = LocalChatColors.current
    var highlightedMessageId by remember(activeChat.id) { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)
        ) {
            // Top Loading Indicator when paging in older messages
            if (allMessages.size > visibleMessageCount) {
                item(key = "top_history_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.iosBlue.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Empty State
            if (activeChat.messages.isEmpty()) {
                item {
                    val chatGraphicState = rememberAppleGraphicAnimationState()
                    AppleEmptyStateView(
                        graphic = {
                            AppleChatGraphic(
                                modifier = Modifier.size(150.dp),
                                state = chatGraphicState,
                                enableClickToAnimate = true
                            )
                        },
                        title = "Say Hello!",
                        subtitle = "Send a message, photo, or voice note to begin chatting.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                }
            }

            // Messages List
            itemsIndexed(
                items = activeChat.messages,
                key = { _, msg -> msg.id },
                contentType = { _, msg -> msg.mediaType ?: "text" }
            ) { index, msg ->
                val isOutgoing = msg.senderId == currentUser.id
                val prevMsg = if (index > 0) activeChat.messages[index - 1] else null
                val isConsecutive = prevMsg != null && prevMsg.senderId == msg.senderId && prevMsg.mediaType != "call" && (prevMsg.mediaType == msg.mediaType || (prevMsg.mediaType in listOf("text", null) && msg.mediaType in listOf("text", null)))
                val nextMsg = if (index < activeChat.messages.size - 1) activeChat.messages[index + 1] else null
                val isNextSame = nextMsg != null && nextMsg.senderId == msg.senderId && nextMsg.mediaType != "call" && (nextMsg.mediaType == msg.mediaType || (nextMsg.mediaType in listOf("text", null) && msg.mediaType in listOf("text", null)))

                val rowCallbacks = remember(msg.id, msg.userReaction, msg.pollData, msg.text, msg.mediaType, msg.senderName) {
                    MessageRowCallbacks(
                        onReactionClick = { emoji ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onChatsUpdate { currentChats ->
                                currentChats.map { c ->
                                    if (c.id == activeChatId) {
                                        c.copy(messages = c.messages.map { m ->
                                            if (m.id == msg.id) toggleOrUpdateReactionHelper(m, emoji) else m
                                        })
                                    } else c
                                }
                            }
                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                chatViewModel.toggleReaction(msg.id, emoji)
                            }
                            SoundManager.playReaction()
                        },
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onChatsUpdate { currentChats ->
                                currentChats.map { c ->
                                    if (c.id == activeChatId) {
                                        c.copy(messages = c.messages.map { m ->
                                            if (m.id == msg.id) {
                                                addOrEnsureReactionHelper(m, "❤️")
                                            } else m
                                        })
                                    } else c
                                }
                            }
                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                chatViewModel.addOrEnsureReaction(msg.id, "❤️")
                            }
                            SoundManager.playReaction()
                            showToast("Liked")
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                            onSelectContextMenuMessage(msg)
                        },
                        onReplyAction = {
                            onStartReply(msg)
                        },
                        onEditAction = {
                            onStartEdit(msg)
                        },
                        onPinAction = {
                            onPinMessage(msg)
                        },
                        onCopyAction = {
                            if (msg.text != null) {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                showToast("Copied to clipboard")
                            }
                        },
                        onDeleteAction = {
                            onDeleteMessagePending(msg)
                        },
                        onImageClick = { imgUrl ->
                            onImageClick(imgUrl)
                        },
                        onImageStackClick = { urls, idx ->
                            onImageStackClick(urls, idx)
                        },
                        onPollVote = { optIdx ->
                            SoundManager.playReaction()
                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                chatViewModel.votePoll(msg.id, optIdx, allowMultiple = msg.pollData?.allowMultipleAnswers ?: false)
                            }
                            val currentVoter = PollVoter(
                                id = currentUser.id,
                                name = currentUser.name,
                                avatar = currentUser.avatar,
                                isCurrentUser = true,
                                time = "Just now"
                            )
                            onChatsUpdate { currentChats ->
                                currentChats.map { c ->
                                    if (c.id == activeChatId) {
                                        c.copy(messages = c.messages.map { m ->
                                            if (m.id == msg.id && m.pollData != null) {
                                                val allowMultiple = m.pollData.allowMultipleAnswers
                                                val updatedOpts = m.pollData.options.mapIndexed { idx, o ->
                                                    if (idx == optIdx) {
                                                        val newVoted = !o.isUserVoted
                                                        val newVotes = if (newVoted) o.votes + 1 else (o.votes - 1).coerceAtLeast(0)
                                                        val newVoters = if (newVoted) {
                                                            o.voters.filterNot { it.id == currentUser.id } + currentVoter
                                                        } else {
                                                            o.voters.filterNot { it.id == currentUser.id }
                                                        }
                                                        o.copy(votes = newVotes, isUserVoted = newVoted, voters = newVoters)
                                                    } else if (!allowMultiple && o.isUserVoted) {
                                                        o.copy(
                                                            votes = (o.votes - 1).coerceAtLeast(0),
                                                            isUserVoted = false,
                                                            voters = o.voters.filterNot { it.id == currentUser.id }
                                                        )
                                                    } else o
                                                }
                                                m.copy(pollData = m.pollData.copy(options = updatedOpts))
                                            } else m
                                        })
                                    } else c
                                }
                            }
                            SoundManager.playReaction()
                        },
                        onPollRetractVote = {
                            SoundManager.playReaction()
                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                chatViewModel.retractPollVote(msg.id)
                            }
                            onChatsUpdate { currentChats ->
                                currentChats.map { c ->
                                    if (c.id == activeChatId) {
                                        c.copy(messages = c.messages.map { m ->
                                            if (m.id == msg.id && m.pollData != null) {
                                                val updatedOpts = m.pollData.options.map { o ->
                                                    if (o.isUserVoted) {
                                                        o.copy(
                                                            votes = (o.votes - 1).coerceAtLeast(0),
                                                            isUserVoted = false,
                                                            voters = o.voters.filterNot { it.id == currentUser.id }
                                                        )
                                                    } else o
                                                }
                                                m.copy(pollData = m.pollData.copy(options = updatedOpts))
                                            } else m
                                        })
                                    } else c
                                }
                            }
                            showToast("Vote retracted")
                        },
                        onClosePoll = {
                            SoundManager.playReaction()
                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                chatViewModel.closePoll(msg.id)
                            }
                            onChatsUpdate { currentChats ->
                                currentChats.map { c ->
                                    if (c.id == activeChatId) {
                                        c.copy(messages = c.messages.map { m ->
                                            if (m.id == msg.id && m.pollData != null) {
                                                m.copy(pollData = m.pollData.copy(isClosed = true))
                                            } else m
                                        })
                                    } else c
                                }
                            }
                            showToast("Poll closed. Voting ended.")
                        },
                        onViewPollVotes = { poll ->
                            onViewPollVotes(poll)
                        },
                        onToggleSelect = {
                            if (selectedMessageIds.contains(msg.id)) {
                                selectedMessageIds.remove(msg.id)
                            } else {
                                selectedMessageIds.add(msg.id)
                            }
                        },
                        onQuoteClick = { targetMsgId ->
                            coroutineScope.launch {
                                val targetIdx = activeChat.messages.indexOfFirst { it.id == targetMsgId }
                                if (targetIdx != -1) {
                                    val offsetHeader = (if (allMessages.size > visibleMessageCount) 1 else 0)
                                    listState.animateScrollToItem((targetIdx + offsetHeader).coerceAtLeast(0))
                                    highlightedMessageId = targetMsgId
                                    delay(1200L)
                                    highlightedMessageId = null
                                } else {
                                    val fullIdx = allMessages.indexOfFirst { it.id == targetMsgId }
                                    if (fullIdx != -1) {
                                        onExpandVisibleCount((allMessages.size - fullIdx + 10).coerceAtLeast(visibleMessageCount))
                                        delay(80L)
                                        val newIdx = activeChat.messages.indexOfFirst { it.id == targetMsgId }
                                        if (newIdx != -1) {
                                            val offsetHeader = (if (allMessages.size > visibleMessageCount) 1 else 0)
                                            listState.animateScrollToItem((newIdx + offsetHeader).coerceAtLeast(0))
                                            highlightedMessageId = targetMsgId
                                            delay(1200L)
                                            highlightedMessageId = null
                                        }
                                    } else {
                                        showToast("Original message not in current view")
                                    }
                                }
                            }
                        },

                        onRetry = {
                            if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                                chatViewModel.retryMessage(msg.id)
                            }
                        }
                    )
                }

                val unreadThresholdIndex = if (unreadMessageThreshold > 0) {
                    (activeChat.messages.size - unreadMessageThreshold).coerceAtLeast(0)
                } else -1

                if (index == unreadThresholdIndex && unreadMessageThreshold > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            colors.iosBlue.copy(alpha = 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.08f),
                                    spotColor = Color.Black.copy(alpha = 0.04f)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (colors.isLight) Color(0xF4F0F6FF)
                                    else Color(0xEE1A2538)
                                )
                                .border(
                                    0.6.dp,
                                    colors.iosBlue.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(colors.iosBlue, CircleShape)
                                )
                                Text(
                                    text = "${unreadMessageThreshold} Unread Message${if (unreadMessageThreshold > 1) "s" else ""}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }
                }

                val showDateHeader = index == 0 || (prevMsg != null && ChatAdapters.isDifferentDay(prevMsg.timestamp, msg.timestamp))
                if (showDateHeader) {
                    val dateHeaderStr = ChatAdapters.formatMessageDateHeader(msg.timestamp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 4.dp else 14.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(0.6.dp)
                                .background(colors.borderSubtle.copy(alpha = 0.45f))
                        )
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 1.5.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.06f),
                                    spotColor = Color.Black.copy(alpha = 0.03f)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isLight) Color(0xF6FFFFFF) else Color(0xEE17202D))
                                .border(0.5.dp, if (colors.isLight) Color(0x22000000) else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 13.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = dateHeaderStr,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }

                MessageRow(
                    message = msg,
                    isOutgoing = isOutgoing,
                    isConsecutive = isConsecutive,
                    isNextSame = isNextSame,
                    searchQuery = searchInsideQuery,
                    senderAvatar = activeChat.avatar,
                    showSenderName = (activeChat.type == "group") && !isOutgoing,
                    senderDisplayName = msg.senderName,
                    isSelectionMode = isMessageSelectionMode,
                    isSelected = selectedMessageIds.contains(msg.id),
                    isHighlighted = (highlightedMessageId == msg.id),
                    config = bubbleConfig,
                    callbacks = rowCallbacks,
                    modifier = Modifier.padding(vertical = if (isConsecutive) 1.5.dp else 4.dp)
                )
            }

            // Realtime Typing & Recording Activity Indicators
            val activeRoomTyping = typingStatuses[activeChat.id] ?: emptyMap()
            val otherTypingUsers = activeRoomTyping.filterKeys { it != currentUid }
            val isRemoteRecording = otherTypingUsers.values.any { it == "recording" }
            val isRemoteTyping = otherTypingUsers.values.any { it == "typing" }
            val shouldShowActivityBubble = isTyping || isRemoteTyping || isRemoteRecording

            if (shouldShowActivityBubble) {
                item {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        AsyncImage(
                            model = activeChat.avatar,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, colors.borderSubtle, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.bubbleGray)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (isRemoteRecording) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LucideMic(tint = colors.iosBlue, size = 14.dp)
                                    Text(
                                        text = "Recording audio...",
                                        fontSize = 13.sp,
                                        color = colors.iosBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(3) { index ->
                                        val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                                        val dy by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = -5f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(420, delayMillis = index * 140, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "typingBounce"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .offset(y = dy.dp)
                                                .background(
                                                    colors.textMuted.copy(alpha = 0.6f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scroll To Bottom Button (Only when scrolled significantly up)
        androidx.compose.animation.AnimatedVisibility(
            visible = listState.canScrollForward,
            enter = scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(targetScale = 0.7f) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            val totalItemCount = (if (allMessages.size > visibleMessageCount) 1 else 0) + activeChat.messages.size
                            val targetIndex = (totalItemCount - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(targetIndex)
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp), spotColor = Color.Black.copy(alpha = 0.12f))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = colors.cardBg,
                    contentColor = colors.iosBlue,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    LucideArrowDown(tint = colors.iosBlue, size = 16.dp)
                }

                if (unreadMessageThreshold > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .height(18.dp)
                            .widthIn(min = 18.dp)
                            .background(colors.iosBlue, RoundedCornerShape(9.dp))
                            .border(1.5.dp, colors.cardBg, RoundedCornerShape(9.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadMessageThreshold > 99) "99+" else unreadMessageThreshold.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
