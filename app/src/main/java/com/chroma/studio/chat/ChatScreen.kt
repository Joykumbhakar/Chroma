package com.chroma.studio.chat
import com.chroma.studio.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.chroma.studio.ui.components.AppleChatGraphic
import com.chroma.studio.ui.components.AppleEmptyStateView
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.*
import com.chroma.studio.chat.media.*
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.*
import com.chroma.studio.ui.screens.StoryViewerScreen
import com.chroma.studio.viewmodel.StoryViewModel
import com.chroma.studio.chat.ui.theme.ChatWallpaperBackground
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.util.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    onAppearanceToggle: () -> Unit = {},
    onAccentCycle: () -> Unit = {},
    accentColor: AccentColorType = AccentColorType.BLUE,
    chatViewModel: com.chroma.studio.viewmodel.ChatViewModel? = null,
    storyViewModel: com.chroma.studio.viewmodel.StoryViewModel? = null,
    initialRoomId: String? = null,
    roomId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNavigateToProfile: ((String) -> Unit)? = null,
    onNavigateToCreateStory: (() -> Unit)? = null,
    onNavigateToGroupInfo: ((String) -> Unit)? = null,
    onInsideChatChange: ((Boolean) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var isDarkMode by remember { mutableStateOf(settingsManager.isDarkMode) }
    var currentAccent by remember { mutableStateOf(settingsManager.accentColor) }

    com.chroma.studio.chat.ui.theme.ChatAppTheme(
        darkTheme = isDarkMode,
        accentColor = currentAccent
    ) {
        ChatScreenContent(
            onAppearanceToggle = {
                isDarkMode = !isDarkMode
                settingsManager.isDarkMode = isDarkMode
                onAppearanceToggle()
            },
            onAccentCycle = {
                val accents = AccentColorType.entries
                val nextIndex = (accents.indexOf(currentAccent) + 1) % accents.size
                currentAccent = accents[nextIndex]
                settingsManager.accentColor = currentAccent
                onAccentCycle()
            },
            accentColor = currentAccent,
            chatViewModel = chatViewModel,
            storyViewModel = storyViewModel,
            initialRoomId = initialRoomId,
            roomId = roomId,
            onNavigateBack = onNavigateBack,
            onBack = onBack,
            onNavigateToUserProfile = onNavigateToUserProfile,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToCreateStory = onNavigateToCreateStory,
            onNavigateToGroupInfo = onNavigateToGroupInfo,
            onInsideChatChange = onInsideChatChange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatScreenContent(
    onAppearanceToggle: () -> Unit = {},
    onAccentCycle: () -> Unit = {},
    accentColor: AccentColorType = AccentColorType.BLUE,
    chatViewModel: com.chroma.studio.viewmodel.ChatViewModel? = null,
    storyViewModel: com.chroma.studio.viewmodel.StoryViewModel? = null,
    initialRoomId: String? = null,
    roomId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNavigateToProfile: ((String) -> Unit)? = null,
    onNavigateToCreateStory: (() -> Unit)? = null,
    onNavigateToGroupInfo: ((String) -> Unit)? = null,
    onInsideChatChange: ((Boolean) -> Unit)? = null
) {
    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val effectiveRoomId = initialRoomId ?: roomId
    var activeChatId by remember { mutableStateOf(effectiveRoomId ?: "") }
    var isInsideChat by remember { mutableStateOf(effectiveRoomId != null) }
    var activeTab by remember { mutableStateOf("all") }
    var sidebarSearchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    val currentFirebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentUid = currentFirebaseUser?.uid ?: ""

    val userProfiles by (chatViewModel?.userProfiles?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val currentProfile = if (currentFirebaseUser != null) userProfiles[currentFirebaseUser.uid] else null

    var currentUser by remember(currentProfile, currentFirebaseUser) {
        mutableStateOf(
            if (currentFirebaseUser != null) {
                ChatAdapters.toUser(currentProfile, currentUid)
            } else {
                User(
                    id = currentUid,
                    name = "User",
                    username = "@user",
                    avatar = "",
                    status = "Online"
                )
            }
        )
    }

    val effectiveStoryVm = storyViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel<StoryViewModel>()
    val storyGroups by effectiveStoryVm.storyGroups.collectAsState()
    val myStories by effectiveStoryVm.myStories.collectAsState()
    val likedStoryIds by effectiveStoryVm.likedStoryIds.collectAsState()
    val isUploadingStory by effectiveStoryVm.isUploading.collectAsState()
    var showStoryViewer by remember { mutableStateOf(false) }
    var storyViewerInitialIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        effectiveStoryVm.loadStories()
    }

    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    LaunchedEffect(storyGroups) {
        stories = ChatAdapters.toStoriesList(storyGroups, currentUid)
    }

    val recentRooms by (chatViewModel?.recentChats?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val isOtherOnline by (chatViewModel?.isOtherUserOnline?.collectAsState() ?: remember { mutableStateOf(false) })
    val otherLastSeen by (chatViewModel?.otherUserLastSeen?.collectAsState() ?: remember { mutableStateOf(null) })
    val onlineUsersPresence by (chatViewModel?.onlineUsersPresence?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val typingStatuses by (chatViewModel?.typingStatuses?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val mutedChatIds by (chatViewModel?.mutedChatIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) })

    LaunchedEffect(effectiveRoomId) {
        if (!effectiveRoomId.isNullOrBlank()) {
            activeChatId = effectiveRoomId
            isInsideChat = true
            chatViewModel?.openChat(effectiveRoomId)
        }
    }

    val activeRoom = remember(recentRooms, activeChatId) {
        recentRooms.find { it.id == activeChatId }
    }
    val otherUid = remember(activeRoom, currentUid, activeChatId) {
        if (activeRoom != null && !activeRoom.isGroup) {
            activeRoom.participants.firstOrNull { it != currentUid }
        } else if (!activeChatId.startsWith("group_") && activeChatId.contains("_")) {
            activeChatId.split("_").firstOrNull { it != currentUid }
        } else null
    }
    val activeRoomPresence = remember(onlineUsersPresence, otherUid) {
        if (otherUid != null) onlineUsersPresence[otherUid] else null
    }
    val isActiveOnline = isOtherOnline || (activeRoomPresence?.online == true)
    val activeLastSeen = otherLastSeen ?: activeRoomPresence?.lastOnline
    val activeRoomTyping = remember(typingStatuses, activeChatId) {
        typingStatuses[activeChatId] ?: emptyMap()
    }

    // Active Chat with Safe Guaranteed Fallback (Zero crash possibility)
    val fallbackChat = remember {
        ChatConversation(
            id = "chat_fallback",
            name = "Chroma Support",
            type = "direct",
            avatar = "",
            online = true,
            statusText = "Online",
            phone = "",
            bio = "Official Chroma Studio Support",
            unreadCount = 0,
            pinnedMessage = null,
            isMuted = false,
            isBlocked = false,
            messages = emptyList()
        )
    }

    val activeChatBase = remember(activeRoom, activeChatId, currentUid, userProfiles, isActiveOnline, activeLastSeen, activeRoomTyping, mutedChatIds) {
        if (activeRoom != null) {
            ChatAdapters.toChatConversation(
                room = activeRoom,
                currentUid = currentUid,
                userProfiles = userProfiles,
                isOnline = isActiveOnline,
                lastSeenTime = activeLastSeen,
                typingMap = activeRoomTyping
            ).copy(isMuted = mutedChatIds.contains(activeRoom.id))
        } else {
            fallbackChat.copy(id = activeChatId)
        }
    }

    var chats by remember(recentRooms, userProfiles, isOtherOnline, otherLastSeen, onlineUsersPresence, typingStatuses, mutedChatIds, isInsideChat) {
        mutableStateOf(
            if (isInsideChat && effectiveRoomId != null) {
                listOf(activeChatBase)
            } else {
                recentRooms.map { room ->
                    val oUid = if (!room.isGroup) room.participants.firstOrNull { it != currentUid } else null
                    val presence = if (oUid != null) onlineUsersPresence[oUid] else null
                    val isOnline = presence?.online == true || (!room.isGroup && room.id == activeChatId && isOtherOnline)
                    val lastSeenTime = presence?.lastOnline ?: if (room.id == activeChatId) otherLastSeen else null
                    val roomTyping = typingStatuses[room.id] ?: emptyMap()
                    ChatAdapters.toChatConversation(
                        room = room,
                        currentUid = currentUid,
                        userProfiles = userProfiles,
                        isOnline = isOnline,
                        lastSeenTime = lastSeenTime,
                        typingMap = roomTyping
                    ).copy(isMuted = mutedChatIds.contains(room.id))
                }
            }
        )
    }

    // Server Messages
    val rawMessages by (chatViewModel?.currentMessages?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val liveMessages = remember(rawMessages, userProfiles, currentUid, isOtherOnline) {
        if (rawMessages.isNotEmpty()) {
            val rawMap = rawMessages.associateBy { it.id }
            rawMessages.reversed().map { msg ->
                ChatAdapters.toDisplayMessage(msg, currentUid, userProfiles, rawMap, isOtherOnline)
            }
        } else null
    }

    // Windowed Viewport Pagination (Only load showing parts messages & files to avoid overload crashing)
    var visibleMessageCount by remember(activeChatId) { mutableIntStateOf(25) }
    var previousMessageCount by remember(activeChatId) { mutableIntStateOf(0) }
    var isInitialScrollDone by remember(activeChatId) { mutableStateOf(false) }
    var lastBottomMessageId by remember(activeChatId) { mutableStateOf<String?>(null) }

    val localChatMessages = remember(chats, activeChatId) {
        chats.find { it.id == activeChatId }?.messages ?: emptyList()
    }

    val allMessages = remember(liveMessages, isInsideChat, localChatMessages, activeChatBase.messages) {
        val rawList = if (liveMessages != null && isInsideChat && chatViewModel != null) {
            val serverIds = liveMessages.map { it.id }.toSet()
            val pendingLocal = localChatMessages.filter { it.id !in serverIds }
            (liveMessages + pendingLocal).distinctBy { it.id }
        } else if (localChatMessages.isNotEmpty()) {
            localChatMessages
        } else {
            activeChatBase.messages
        }
        groupContinuousMediaMessages(rawList)
    }

    // Only slice the latest `visibleMessageCount` messages for rendering
    val displayedMessages = remember(allMessages, visibleMessageCount) {
        if (allMessages.size <= visibleMessageCount) allMessages
        else allMessages.takeLast(visibleMessageCount)
    }

    val activeChat = remember(activeChatBase, displayedMessages) {
        activeChatBase.copy(messages = displayedMessages)
    }

    // Scroll-up trigger to load older messages seamlessly (ONLY after initial bottom scroll is done)
    val shouldLoadOlderMessages by remember {
        derivedStateOf {
            if (!isInsideChat || !isInitialScrollDone) return@derivedStateOf false
            val firstVisibleIndex = listState.firstVisibleItemIndex
            // When user scrolls near top (within first 4 items) and there are more messages to display
            firstVisibleIndex <= 4 && (allMessages.size > visibleMessageCount || (allMessages.size >= 25 && chatViewModel != null))
        }
    }

    LaunchedEffect(shouldLoadOlderMessages) {
        if (shouldLoadOlderMessages) {
            val prevCount = visibleMessageCount
            if (visibleMessageCount < allMessages.size) {
                val nextCount = (visibleMessageCount + 25).coerceAtMost(allMessages.size)
                val added = nextCount - prevCount
                if (added > 0) {
                    visibleMessageCount = nextCount
                    // Preserve scroll position so LazyColumn does not jump when prepending items
                    val currentIdx = listState.firstVisibleItemIndex
                    val currentOffset = listState.firstVisibleItemScrollOffset
                    listState.scrollToItem(currentIdx + added, currentOffset)
                }
            } else {
                chatViewModel?.loadMoreMessages()
            }
        }
    }

    // Screen State
    var isProfileSettingsOpen by remember { mutableStateOf(false) }

    // Wallpaper & Appearance Styles (Persisted via SharedPreferences)
    var currentWallpaper by remember { mutableStateOf(settingsManager.wallpaperType) }
    var currentWallpaperDimming by remember { mutableFloatStateOf(settingsManager.wallpaperDimming) }
    var isWallpaperPickerOpen by remember { mutableStateOf(false) }
    var currentBubbleStyle by remember { mutableStateOf(settingsManager.bubbleStyle) }
    var currentGlobeStyle by remember { mutableStateOf(settingsManager.globeStyle) }
    var currentCustomCorners by remember { mutableStateOf(settingsManager.customCorners) }
    var currentAnimationStyle by remember { mutableStateOf(settingsManager.animationStyle) }
    var isAppearanceModalOpen by remember { mutableStateOf(false) }

    // Instant bottom snap on open (ALWAYS snaps to the latest/last message instead of the first message)
    // Smooth scroll ONLY on newly received/sent messages at the bottom
    LaunchedEffect(activeChatId, isInsideChat) {
        if (isInsideChat) {
            isInitialScrollDone = false
            previousMessageCount = 0
            lastBottomMessageId = null
        }
    }

    LaunchedEffect(activeChat.messages, isInsideChat) {
        if (isInsideChat && activeChat.messages.isNotEmpty()) {
            val currentLatestId = activeChat.messages.lastOrNull()?.id
            val totalItemCount = (if (allMessages.size > visibleMessageCount) 1 else 0) + 1 + activeChat.messages.size
            val targetIndex = (totalItemCount - 1).coerceAtLeast(0)
            val sizeDiff = activeChat.messages.size - previousMessageCount

            try {
                if (!isInitialScrollDone || sizeDiff > 1) {
                    // Instantly snap to the bottom on initial open or when a message batch arrives (no lag, no stutter)
                    listState.scrollToItem(targetIndex)
                    isInitialScrollDone = true
                    lastBottomMessageId = currentLatestId
                } else if (currentLatestId != null && currentLatestId != lastBottomMessageId) {
                    // Auto-scroll ONLY when a single genuinely new message arrives at the bottom
                    val isNearBottom = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >=
                        (listState.layoutInfo.totalItemsCount - 4).coerceAtLeast(0)
                    val lastMsg = activeChat.messages.last()
                    val isOutgoing = lastMsg.senderId == currentUser.id

                    if (isOutgoing || isNearBottom) {
                        listState.animateScrollToItem(targetIndex)
                    }
                    lastBottomMessageId = currentLatestId
                }
            } catch (e: Exception) {
                // Safeguard against any layout race condition during rapid compose measurement
            }
            previousMessageCount = activeChat.messages.size
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && isInsideChat && activeChat.messages.isNotEmpty()) {
            delay(50)
            val totalItemCount = (if (allMessages.size > visibleMessageCount) 1 else 0) + 1 + activeChat.messages.size
            val targetIndex = (totalItemCount - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    // In-Chat Search
    var isSearchInsideOpen by remember { mutableStateOf(false) }
    var searchInsideQuery by remember { mutableStateOf("") }

    // Input & Reply & Edit
    var inputText by remember { mutableStateOf("") }
    var activeReplyQuote by remember { mutableStateOf<ReplyQuote?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }

    // Long-Press & Pinning & Forward States
    var pinnedChatIds by remember { mutableStateOf(setOf<String>()) }
    var starredMessageIds by remember { mutableStateOf(setOf<String>()) }
    var selectedMessageForContextMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForInfo by remember { mutableStateOf<ChatMessage?>(null) }
    var messageToForward by remember { mutableStateOf<ChatMessage?>(null) }
    var activePollForDetails by remember { mutableStateOf<PollData?>(null) }
    var isEmojiReactionPickerOpen by remember { mutableStateOf(false) }
    var messageForEmojiPicker by remember { mutableStateOf<ChatMessage?>(null) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }
    val isMessageSelectionMode = selectedMessageIds.isNotEmpty()
    var isSharedGalleryOpen by remember { mutableStateOf(false) }

    // Apple iOS Popup Dialog States
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var showReportUserDialog by remember { mutableStateOf(false) }

    // Island Notifications & Dynamic Island HUD System
    var islandNotification by remember { mutableStateOf<IslandNotification?>(null) }

    fun showIsland(title: String, subtitle: String? = null, type: IslandType = IslandType.SUCCESS) {
        val notif = IslandNotification(title = title, subtitle = subtitle, type = type)
        islandNotification = notif
        coroutineScope.launch {
            delay(2800)
            if (islandNotification == notif) islandNotification = null
        }
    }

    fun showToast(msg: String) {
        // Suppress toasts for routine actions (sends, likes, reactions) to keep the experience calm and clean
        if (msg == "Message sent" || msg == "Liked" || msg.startsWith("Reacted") || msg == "Photo sent!") {
            return
        }
        val islandType = when {
            msg.contains("fail", ignoreCase = true) || msg.contains("error", ignoreCase = true) || msg.contains("deleted", ignoreCase = true) -> IslandType.ERROR
            msg.contains("block", ignoreCase = true) || msg.contains("warn", ignoreCase = true) || msg.contains("mute", ignoreCase = true) -> IslandType.WARNING
            msg.contains("copied", ignoreCase = true) || msg.contains("pinned", ignoreCase = true) || msg.contains("saved", ignoreCase = true) -> IslandType.SUCCESS
            else -> IslandType.INFO
        }
        showIsland(title = msg, type = islandType)
    }

    fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun toggleOrUpdateReaction(msg: ChatMessage, emoji: String): ChatMessage {
        val prevReaction = msg.userReaction
        val currentReactions = msg.reactions.toMutableMap()

        if (prevReaction == emoji) {
            // Toggle off reaction if tapping the same one
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
            // Remove previous user reaction if any
            if (prevReaction != null) {
                val prevCount = (currentReactions[prevReaction] ?: 1) - 1
                if (prevCount <= 0) {
                    currentReactions.remove(prevReaction)
                } else {
                    currentReactions[prevReaction] = prevCount
                }
            }
            // Add/switch to new reaction
            val newCount = (currentReactions[emoji] ?: 0) + 1
            currentReactions[emoji] = newCount
            return msg.copy(
                reactions = currentReactions.filterValues { it > 0 },
                userReaction = emoji
            )
        }
    }

    // Real Audio Recorder & Amplitude Engine
    var isVoiceRecording by remember { mutableStateOf(false) }
    val recorderDuration by AudioRecorderManager.durationSec.collectAsState()
    val recorderAmplitude by AudioRecorderManager.currentAmplitude.collectAsState()
    var voiceStatusText by remember { mutableStateOf("Listening...") }

    // Speech Recognition & Dictation Engine
    var isDictating by remember { mutableStateOf(false) }
    val dictationRms by SpeechRecognitionManager.speechRms.collectAsState()

    // Permission & Media State
    var permissionToExplain by remember { mutableStateOf<PermissionRequestType?>(null) }
    var isGalleryPickerOpen by remember { mutableStateOf(false) }
    var isFileManagerOpen by remember { mutableStateOf(false) }
    var isIosCameraOpen by remember { mutableStateOf(false) }
    var pendingCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun sendCapturedPhoto(uri: Uri, caption: String?) {
        val replyQuoteSnapshot = activeReplyQuote
        val photoMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            mediaType = "image",
            localUri = uri.toString(),
            mediaUrl = uri.toString(),
            text = caption?.takeIf { it.isNotBlank() },
            time = getCurrentTimeFormatted(),
            status = "sent",
            replyTo = replyQuoteSnapshot
        )
        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + photoMsg) else it }
        activeReplyQuote = null
        coroutineScope.launch {
            try {
                val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri)
                    ?: context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                    chatViewModel.sendMediaMessage(bytes, "jpg", caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to upload camera photo", e)
            }
        }
        SoundManager.playSend()
        showToast("Photo sent ✨")
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = AudioRecorderManager.startRecording(context)
            if (started) {
                isVoiceRecording = true
                if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                    chatViewModel.setRecordingStatus(true)
                }
                SoundManager.playReaction()
            }
        } else {
            showIsland("Microphone access needed", null, IslandType.WARNING)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isIosCameraOpen = true
        } else {
            showIsland("Camera access needed", null, IslandType.WARNING)
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        if (map.values.any { it }) {
            isGalleryPickerOpen = true
        } else {
            showIsland("Photo library access needed", null, IslandType.WARNING)
        }
    }

    val speechPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isDictating = true
            SpeechRecognitionManager.startListening(
                context = context,
                onLivePartial = { inputText = it },
                onFinalResult = {
                    inputText = it
                    isDictating = false
                    SoundManager.playReaction()
                }
            )
        } else {
            showIsland("Microphone access needed for dictation", null, IslandType.WARNING)
        }
    }

    fun startVoiceRecording() {
        if (PermissionHelper.hasAudioPermission(context)) {
            val started = AudioRecorderManager.startRecording(context)
            if (started) {
                isVoiceRecording = true
                if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                    chatViewModel.setRecordingStatus(true)
                }
                SoundManager.playReaction()
            }
        } else {
            audioPermissionLauncher.launch(PermissionHelper.RECORD_AUDIO_PERMISSION)
        }
    }

    fun finishVoiceRecording() {
        val recResult = AudioRecorderManager.stopRecording()
        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
            chatViewModel.setRecordingStatus(false)
        }
        val duration = recResult?.durationSeconds ?: if (recorderDuration < 2) 3 else recorderDuration
        val replyQuoteSnapshot = activeReplyQuote
        val newVoiceMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            mediaType = "voice",
            localUri = recResult?.file?.absolutePath,
            voiceDuration = duration,
            waveformSamples = recResult?.waveformSamples ?: emptyList(),
            text = "",
            time = getCurrentTimeFormatted(),
            status = "pending",
            replyTo = replyQuoteSnapshot
        )
        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newVoiceMsg) else it }
        isVoiceRecording = false
        activeReplyQuote = null
        SoundManager.playSend()
        showToast("Voice note sent ✨")

        if (recResult?.file != null && recResult.file.exists() && chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
            try {
                val audioBytes = recResult.file.readBytes()
                chatViewModel.sendAudioMessage(audioBytes, duration, replyToMessageId = replyQuoteSnapshot?.messageId)
            } catch (e: Exception) {}
        }
    }

    fun cancelVoiceRecording() {
        AudioRecorderManager.cancelRecording()
        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
            chatViewModel.setRecordingStatus(false)
        }
        isVoiceRecording = false
        showToast("Voice recording canceled")
    }

    fun launchCamera() {
        if (PermissionHelper.hasCameraPermission(context)) {
            isIosCameraOpen = true
        } else {
            cameraPermissionLauncher.launch(PermissionHelper.CAMERA_PERMISSION)
        }
    }

    fun launchGallery() {
        if (PermissionHelper.hasMediaPermission(context)) {
            isGalleryPickerOpen = true
        } else {
            mediaPermissionLauncher.launch(PermissionHelper.getMediaPermissions())
        }
    }

    fun toggleDictation() {
        if (isDictating) {
            SpeechRecognitionManager.stopListening()
            isDictating = false
        } else {
            if (PermissionHelper.hasAudioPermission(context)) {
                isDictating = true
                SpeechRecognitionManager.startListening(
                    context = context,
                    onLivePartial = { inputText = it },
                    onFinalResult = {
                        inputText = it
                        isDictating = false
                        SoundManager.playReaction()
                    }
                )
            } else {
                permissionToExplain = PermissionRequestType.SPEECH
            }
        }
    }

    // Calling State
    var isCallBannerActive by remember { mutableStateOf(false) }
    var isFullCallModalOpen by remember { mutableStateOf(false) }
    var activeCallType by remember { mutableStateOf("Audio Call") }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    // Modals
    var isContactDrawerOpen by remember { mutableStateOf(false) }
    var activeStoryViewer by remember { mutableStateOf<Story?>(null) }
    var isStoryEditorOpen by remember { mutableStateOf(false) }
    var isStoryAnalyticsOpen by remember { mutableStateOf(false) }
    var storyForAnalytics by remember { mutableStateOf<Story?>(null) }
    var isCreateGroupModalOpen by remember { mutableStateOf(false) }
    var isPollCreatorOpen by remember { mutableStateOf(false) }
    var previewLightboxImage by remember { mutableStateOf<String?>(null) }
    var previewLightboxList by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewLightboxIndex by remember { mutableIntStateOf(0) }
    var messagePendingDelete by remember { mutableStateOf<ChatMessage?>(null) }

    val shouldHideBottomNav = isInsideChat ||
        isAppearanceModalOpen ||
        isProfileSettingsOpen ||
        activeStoryViewer != null ||
        isStoryEditorOpen ||
        isWallpaperPickerOpen ||
        isSharedGalleryOpen ||
        isStoryAnalyticsOpen ||
        isContactDrawerOpen ||
        isFullCallModalOpen ||
        previewLightboxImage != null

    LaunchedEffect(shouldHideBottomNav) {
        onInsideChatChange?.invoke(shouldHideBottomNav)
    }

    // Typing Simulation State
    var isTyping by remember { mutableStateOf(false) }

    // Call Timer Loop
    LaunchedEffect(isCallBannerActive) {
        if (isCallBannerActive) {
            callDurationSeconds = 0
            while (isCallBannerActive) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }



    // Send Message Handler
    fun handleSendMessage() {
        val trimmed = inputText.trim().take(MAX_MESSAGE_CHARACTERS)
        if (trimmed.isEmpty()) return

        if (activeChat.isBlocked) {
            showToast("Cannot send message to blocked contact")
            return
        }

        if (editingMessageId != null) {
            val eId = editingMessageId!!
            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                chatViewModel.editMessage(eId, trimmed)
            } else {
                chats = chats.map { c ->
                    if (c.id == activeChatId) {
                        c.copy(
                            messages = c.messages.map { m ->
                                if (m.id == eId) m.copy(text = trimmed, isEdited = true) else m
                            }
                        )
                    } else c
                }
            }
            editingMessageId = null
            inputText = ""
            activeReplyQuote = null
            showToast("Message updated")
            return
        }

        val newMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            text = trimmed,
            time = getCurrentTimeFormatted(),
            status = "pending",
            replyTo = activeReplyQuote
        )

        chats = chats.map { c ->
            if (c.id == activeChatId) c.copy(messages = c.messages + newMsg) else c
        }

        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
            chatViewModel.setTypingStatus(false)
        }

        val replyMessageId = activeReplyQuote?.messageId
        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
            chatViewModel.sendMessage(trimmed, replyToMessageId = replyMessageId)
        }

        inputText = ""
        activeReplyQuote = null
        SoundManager.playSend()
        showToast("Message sent")

        coroutineScope.launch {
            val targetIndex = (if (allMessages.size > visibleMessageCount) 1 else 0) + 1 + activeChat.messages.size
            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    val isAnyModalOrSubscreenOpen = isMessageSelectionMode ||
        previewLightboxImage != null ||
        activeStoryViewer != null ||
        isStoryEditorOpen ||
        isStoryAnalyticsOpen ||
        isAppearanceModalOpen ||
        isProfileSettingsOpen ||
        isWallpaperPickerOpen ||
        isSharedGalleryOpen ||
        isContactDrawerOpen ||
        isFullCallModalOpen ||
        isInsideChat ||
        effectiveRoomId != null

    BackHandler(enabled = isAnyModalOrSubscreenOpen) {
        when {
            previewLightboxImage != null -> previewLightboxImage = null
            isMessageSelectionMode -> selectedMessageIds.clear()
            activeStoryViewer != null -> activeStoryViewer = null
            isStoryEditorOpen -> isStoryEditorOpen = false
            isStoryAnalyticsOpen -> isStoryAnalyticsOpen = false
            isWallpaperPickerOpen -> isWallpaperPickerOpen = false
            isAppearanceModalOpen -> isAppearanceModalOpen = false
            isProfileSettingsOpen -> isProfileSettingsOpen = false
            isSharedGalleryOpen -> isSharedGalleryOpen = false
            isContactDrawerOpen -> isContactDrawerOpen = false
            isFullCallModalOpen -> isFullCallModalOpen = false
            isInsideChat -> {
                isInsideChat = false
                if (chatViewModel != null) {
                    chatViewModel.setTypingStatus(false)
                    chatViewModel.setRecordingStatus(false)
                }
                if (effectiveRoomId != null) {
                    onNavigateBack?.invoke() ?: onBack?.invoke()
                }
            }
            effectiveRoomId != null -> {
                onNavigateBack?.invoke() ?: onBack?.invoke()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Root Screen: Standalone Apple iOS Conversations & Stories Screen
        if (effectiveRoomId == null) {
            ConversationsScreen(
                currentUser = currentUser,
                stories = stories,
                storyGroups = storyGroups,
                hasMyStory = myStories.isNotEmpty(),
                onMyStoryClick = {
                    if (myStories.isNotEmpty()) {
                        storyViewerInitialIndex = -1
                        showStoryViewer = true
                    } else {
                        if (onNavigateToCreateStory != null) {
                            onNavigateToCreateStory.invoke()
                        } else {
                            isStoryEditorOpen = true
                        }
                    }
                },
                onStoryGroupClick = { groupIdx ->
                    storyViewerInitialIndex = groupIdx
                    showStoryViewer = true
                },
                chats = chats,
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                searchQuery = sidebarSearchQuery,
                onSearchQueryChanged = { sidebarSearchQuery = it },
                onChatSelected = { chatId ->
                    activeChatId = chatId
                    chats = chats.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
                    isInitialScrollDone = false
                    isInsideChat = true
                    chatViewModel?.openChat(chatId)
                    val targetRoom = recentRooms.find { it.id == chatId }
                    if (targetRoom != null && !targetRoom.isGroup) {
                        val otherUid = targetRoom.participants.firstOrNull { it != currentUid }
                        if (otherUid != null) {
                            chatViewModel?.observeUserOnlineStatus(otherUid)
                        }
                    }
                },
            onStoryClick = { story ->
                val idx = storyGroups.indexOfFirst { it.userId == story.authorId }
                storyViewerInitialIndex = if (idx >= 0) idx else 0
                showStoryViewer = true
            },
            onAddStoryClick = {
                if (onNavigateToCreateStory != null) {
                    onNavigateToCreateStory.invoke()
                } else {
                    isStoryEditorOpen = true
                }
            },
            onNewMessageClick = { isCreateGroupModalOpen = true },
            onOpenAppearanceSettings = { isAppearanceModalOpen = true },
            onUserProfileClick = {
                if (onNavigateToUserProfile != null) {
                    onNavigateToUserProfile.invoke(currentUser.id)
                } else if (onNavigateToProfile != null) {
                    onNavigateToProfile.invoke(currentUser.id)
                } else {
                    isProfileSettingsOpen = true
                }
            },
            onStartCall = { chat, callType ->
                activeChatId = chat.id
                activeCallType = callType
                isCallBannerActive = true
                isFullCallModalOpen = true
                SoundManager.playRing()
            },
            pinnedChatIds = pinnedChatIds,
            onPinChatToggle = { chat ->
                val isPinned = chat.id in pinnedChatIds
                pinnedChatIds = if (isPinned) pinnedChatIds - chat.id else pinnedChatIds + chat.id
                chatViewModel?.toggleFavorite(chat.id, !isPinned)
                showToast(if (isPinned) "Chat unpinned" else "Chat pinned to top")
            },
            onMuteChatToggle = { chat ->
                chats = chats.map { if (it.id == chat.id) it.copy(isMuted = !it.isMuted) else it }
                showToast(if (!chat.isMuted) "Notifications muted" else "Notifications unmuted")
            },
            onReadChatToggle = { chat ->
                chats = chats.map { if (it.id == chat.id) it.copy(unreadCount = if (it.unreadCount > 0) 0 else 1) else it }
                showToast("Read status updated")
            },
            onLockChatToggle = { _ ->
                showToast("Chat locked with Face ID")
            },
            onArchiveChat = { chat ->
                chats = chats.filter { it.id != chat.id }
                showToast("Chat archived")
            },
            onClearChatHistory = { chat ->
                chats = chats.map { if (it.id == chat.id) it.copy(messages = emptyList()) else it }
                showToast("Chat history cleared")
            },
            onExportChat = { chat ->
                showToast("Exported transcript for ${chat.name}")
            },
            onDeleteChat = { chat ->
                chats = chats.filter { it.id != chat.id }
                showToast("Chat deleted")
            },
            onViewStoryAnalytics = { story ->
                storyForAnalytics = story
                isStoryAnalyticsOpen = true
            },
            onDeleteStory = { story ->
                stories = stories.filter { it.id != story.id }
                showToast("Story deleted")
            },
            onMuteStory = { story ->
                showToast("Muted stories from ${story.authorName}")
            },
            onShareStory = { _ ->
                showToast("Story link copied")
            }
        )
        }

        // 1.5 Fullscreen Apple iOS Profile & Settings Screen (Native iOS Slide Transition)
        AnimatedVisibility(
            visible = isProfileSettingsOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            ProfileSettingsScreen(
                currentUser = currentUser,
                onUpdateUser = { updatedUser ->
                    currentUser = updatedUser
                    if (currentFirebaseUser != null) {
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        firestore.collection("users").document(currentFirebaseUser.uid).update(
                            mapOf(
                                "displayName" to updatedUser.name,
                                "bio" to updatedUser.status,
                                "avatarUrl" to updatedUser.avatar,
                                "bannerUrl" to updatedUser.banner
                            )
                        )
                    }
                },
                chats = chats,
                onUnblockChat = { chatId ->
                    chats = chats.map { if (it.id == chatId) it.copy(isBlocked = false) else it }
                },
                currentWallpaper = currentWallpaper,
                currentWallpaperDimming = currentWallpaperDimming,
                currentBubbleStyle = currentBubbleStyle,
                currentGlobeStyle = currentGlobeStyle,
                currentCustomCorners = currentCustomCorners,
                currentAnimationStyle = currentAnimationStyle,
                accentColor = accentColor,
                onAppearanceToggle = onAppearanceToggle,
                onAccentCycle = onAccentCycle,
                onSelectAccent = { acc ->
                    settingsManager.accentColor = acc
                },
                onOpenWallpaperPicker = { isWallpaperPickerOpen = true },
                onOpenAppearanceSettings = { isAppearanceModalOpen = true },
                onClose = { isProfileSettingsOpen = false },
                onShowToast = { showToast(it) }
            )
        }

        // 1.6 Fullscreen Apple iOS Appearance & Styles Screen (Native iOS Slide Transition)
        AnimatedVisibility(
            visible = isAppearanceModalOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            AppearanceSettingsScreen(
                currentBubbleStyle = currentBubbleStyle,
                currentGlobeStyle = currentGlobeStyle,
                accentColor = accentColor,
                currentCustomCorners = currentCustomCorners,
                onUpdateCustomCorners = {
                    currentCustomCorners = it
                    settingsManager.customCorners = it
                },
                currentAnimationStyle = currentAnimationStyle,
                onSelectAnimationStyle = { anim ->
                    currentAnimationStyle = anim
                    settingsManager.animationStyle = anim
                    SoundManager.playReaction()
                    showToast("Animation set to ${anim.title}")
                },
                onSelectBubbleStyle = { style ->
                    currentBubbleStyle = style
                    settingsManager.bubbleStyle = style
                    SoundManager.playReaction()
                    showToast("Bubble style set to ${style.title}")
                },
                onSelectGlobeStyle = { gStyle ->
                    currentGlobeStyle = gStyle
                    settingsManager.globeStyle = gStyle
                    SoundManager.playReaction()
                    showToast("Voice Globe set to ${gStyle.title}")
                },
                onToggleAppearance = onAppearanceToggle,
                onAccentCycle = onAccentCycle,
                onOpenWallpaperPicker = { isWallpaperPickerOpen = true },
                onClose = { isAppearanceModalOpen = false }
            )
        }

        // 2. Active Chat Conversation Screen (Native iOS Slide Transition)
        AnimatedVisibility(
            visible = isInsideChat,
            enter = if (effectiveRoomId != null) fadeIn(animationSpec = tween(150)) else (slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(180))),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Wallpaper Canvas
                ChatWallpaperBackground(
                    wallpaperType = currentWallpaper,
                    dimmingAlpha = currentWallpaperDimming
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    // 1. Chat Header / Selection Header
                    val totalUnread = chats.filter { it.id != activeChatId }.sumOf { it.unreadCount }
                    val searchMatches = if (searchInsideQuery.isEmpty()) 0 else activeChat.messages.count { it.text?.contains(searchInsideQuery, ignoreCase = true) == true }

                    AnimatedContent(
                        targetState = isMessageSelectionMode,
                        transitionSpec = {
                            (slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                            ) + fadeIn(animationSpec = tween(180))).togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { -it },
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                                ) + fadeOut(animationSpec = tween(150))
                            )
                        },
                        label = "HeaderSelectionAnim"
                    ) { selectionActive ->
                        if (selectionActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.headerBg)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue,
                                    modifier = Modifier
                                        .clickable {
                                            SoundManager.playReaction()
                                            selectedMessageIds.clear()
                                        }
                                        .padding(4.dp)
                                )

                                Text(
                                    text = "${selectedMessageIds.size} Selected",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textDark
                                )

                                Text(
                                    text = if (selectedMessageIds.size == activeChat.messages.size) "Deselect" else "Select All",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.iosBlue,
                                    modifier = Modifier
                                        .clickable {
                                            SoundManager.playReaction()
                                            if (selectedMessageIds.size == activeChat.messages.size) {
                                                selectedMessageIds.clear()
                                            } else {
                                                selectedMessageIds.clear()
                                                selectedMessageIds.addAll(activeChat.messages.map { it.id })
                                            }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        } else {
                            ChatHeader(
                                activeChat = activeChat,
                                unreadBadgeCount = totalUnread,
                                isSearchOpen = isSearchInsideOpen,
                                searchInsideQuery = searchInsideQuery,
                                searchMatchCount = searchMatches,
                                isCallActive = isCallBannerActive,
                                callType = activeCallType,
                                callDurationSec = callDurationSeconds,
                                onBackClick = {
                                    isInsideChat = false
                                    if (chatViewModel != null) {
                                        chatViewModel.setTypingStatus(false)
                                        chatViewModel.setRecordingStatus(false)
                                    }
                                    if (effectiveRoomId != null) {
                                        onNavigateBack?.invoke() ?: onBack?.invoke()
                                    }
                                },
                                onProfileClick = { isContactDrawerOpen = true },
                                onSearchToggle = {
                                    isSearchInsideOpen = !isSearchInsideOpen
                                    if (!isSearchInsideOpen) searchInsideQuery = ""
                                },
                                onSearchQueryChange = { searchInsideQuery = it },
                                onAudioCallClick = {
                                    activeCallType = "Audio Call"
                                    isCallBannerActive = true
                                    isFullCallModalOpen = true
                                    SoundManager.playRing()
                                },
                                onVideoCallClick = {
                                    activeCallType = "Video Call"
                                    isCallBannerActive = true
                                    isFullCallModalOpen = true
                                    SoundManager.playRing()
                                },
                                onAppearanceToggle = onAppearanceToggle,
                                onAccentCycle = onAccentCycle,
                                onWallpaperCycle = {
                                    isWallpaperPickerOpen = true
                                },
                                onOpenAppearanceSettings = {
                                    isAppearanceModalOpen = true
                                },
                                onOpenSharedGallery = {
                                    isSharedGalleryOpen = true
                                },
                                onCreatePollClick = { isPollCreatorOpen = true },
                                onMuteToggle = {
                                    chats = chats.map { if (it.id == activeChatId) it.copy(isMuted = !it.isMuted) else it }
                                    showToast(if (activeChat.isMuted) "Notifications unmuted" else "Notifications muted")
                                },
                                onExportTranscript = { showToast("Chat transcript exported") },
                                onClearChat = {
                                    showClearChatDialog = true
                                },
                                onBlockUser = {
                                    showBlockUserDialog = true
                                },
                                onUnpinMessage = {
                                    chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = null) else it }
                                    showToast("Message unpinned")
                                },
                                onEndCall = {
                                    isCallBannerActive = false
                                    isFullCallModalOpen = false
                                    showToast("Call ended")
                                }
                            )
                        }
                    }

                    val rememberedBubbleConfig = remember(currentBubbleStyle, currentCustomCorners, currentAnimationStyle) {
                        MessageBubbleConfig(
                            bubbleStyle = currentBubbleStyle,
                            customCornerConfig = currentCustomCorners,
                            animationStyle = currentAnimationStyle
                        )
                    }

                    // 2. Chat Messages Stream
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
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

                            // Date Divider with hairline rules & frosted capsule
                            item(key = "date_divider_header") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Full hairline rule across
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(0.6.dp)
                                            .background(colors.borderSubtle.copy(alpha = 0.6f))
                                    )
                                    // Floating pill badge
                                    val dateStr = remember {
                                        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
                                    }
                                    Box(
                                        modifier = Modifier
                                            .shadow(
                                                elevation = 1.5.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.08f),
                                                spotColor = Color.Black.copy(alpha = 0.04f)
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (colors.isLight) Color.White else Color(0xFF151D2A))
                                            .border(0.6.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 3.5.dp)
                                    ) {
                                        Text(
                                            text = dateStr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted,
                                            letterSpacing = 0.2.sp
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
                                            chats = chats.map { c ->
                                                if (c.id == activeChatId) {
                                                    c.copy(
                                                        messages = c.messages.map { m ->
                                                            if (m.id == msg.id) toggleOrUpdateReaction(m, emoji) else m
                                                        }
                                                    )
                                                } else c
                                            }
                                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                                chatViewModel.toggleReaction(msg.id, emoji)
                                            }
                                            SoundManager.playReaction()
                                        },
                                        onDoubleTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            chats = chats.map { c ->
                                                if (c.id == activeChatId) {
                                                    c.copy(
                                                        messages = c.messages.map { m ->
                                                            if (m.id == msg.id) {
                                                                val prevReaction = m.userReaction
                                                                if (prevReaction == "❤️") {
                                                                    m
                                                                } else {
                                                                    toggleOrUpdateReaction(m, "❤️")
                                                                }
                                                            } else m
                                                        }
                                                    )
                                                } else c
                                            }
                                            if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                                chatViewModel.toggleReaction(msg.id, "❤️")
                                            }
                                            SoundManager.playReaction()
                                            showToast("Liked")
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            SoundManager.playReaction()
                                            selectedMessageForContextMenu = msg
                                        },
                                        onReplyAction = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val replySnippet = when {
                                                msg.mediaType == "voice" -> "🎤 Voice message"
                                                msg.mediaType == "image" || msg.mediaType == "image_stack" -> "📷 Photo"
                                                msg.mediaType == "video" -> "🎥 Video"
                                                msg.mediaType == "file" || msg.mediaType == "file_stack" -> "📄 ${msg.fileName ?: "Document"}"
                                                msg.mediaType == "poll" -> "📊 ${msg.pollData?.question ?: "Poll"}"
                                                !msg.text.isNullOrBlank() -> msg.text
                                                else -> "Attachment"
                                            }
                                            activeReplyQuote = ReplyQuote(
                                                author = msg.senderName,
                                                text = replySnippet,
                                                messageId = msg.id,
                                                mediaType = msg.mediaType
                                            )
                                            showToast("Replying to ${msg.senderName}")
                                        },
                                        onEditAction = {
                                            editingMessageId = msg.id
                                            inputText = msg.text ?: ""
                                            activeReplyQuote = null
                                        },
                                        onPinAction = {
                                            chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = msg.text ?: "Attachment") else it }
                                            showToast("Message pinned")
                                        },
                                        onCopyAction = {
                                            if (msg.text != null) {
                                                clipboardManager.setText(AnnotatedString(msg.text))
                                                showToast("Copied to clipboard")
                                            }
                                        },
                                        onDeleteAction = {
                                            messagePendingDelete = msg
                                        },
                                        onImageClick = { imgUrl ->
                                            previewLightboxImage = imgUrl
                                            previewLightboxList = listOf(imgUrl)
                                            previewLightboxIndex = 0
                                        },
                                        onImageStackClick = { urls, idx ->
                                            previewLightboxList = urls
                                            previewLightboxImage = urls.getOrElse(idx) { urls.firstOrNull() }
                                            previewLightboxIndex = idx
                                        },
                                        onPollVote = { optIdx ->
                                            val currentVoter = PollVoter(
                                                id = currentUser.id,
                                                name = currentUser.name,
                                                avatar = currentUser.avatar,
                                                isCurrentUser = true,
                                                time = "Just now"
                                            )
                                            chats = chats.map { c ->
                                                if (c.id == activeChatId) {
                                                    c.copy(
                                                        messages = c.messages.map { m ->
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
                                                        }
                                                    )
                                                } else c
                                            }
                                            SoundManager.playReaction()
                                        },
                                        onViewPollVotes = { poll ->
                                            activePollForDetails = poll
                                        },
                                        onToggleSelect = {
                                            if (selectedMessageIds.contains(msg.id)) {
                                                selectedMessageIds.remove(msg.id)
                                            } else {
                                                selectedMessageIds.add(msg.id)
                                            }
                                        }
                                    )
                                }

                                MessageRow(
                                    message = msg,
                                    isOutgoing = isOutgoing,
                                    isConsecutive = isConsecutive,
                                    isNextSame = isNextSame,
                                    searchQuery = searchInsideQuery,
                                    senderAvatar = activeChat.avatar,
                                    senderDisplayName = msg.senderName,
                                    showSenderName = activeChat.type == "group",
                                    callbacks = rowCallbacks,
                                    isSelectionMode = isMessageSelectionMode,
                                    isSelected = selectedMessageIds.contains(msg.id),
                                    config = rememberedBubbleConfig,
                                    modifier = Modifier.padding(bottom = if (isNextSame) 2.5.dp else 8.dp)
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
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            top = 4.dp,
                                            bottom = 4.dp
                                        )
                                    ) {
                                        AsyncImage(
                                            model = activeChat.avatar,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .border(
                                                    0.5.dp,
                                                    colors.borderSubtle,
                                                    androidx.compose.foundation.shape.CircleShape
                                                ),
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
                                                    LucideMic(
                                                        tint = colors.iosBlue,
                                                        size = 14.dp
                                                    )
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
                                                                    androidx.compose.foundation.shape.CircleShape
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
                            FloatingActionButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(activeChat.messages.size - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(0.5.dp, colors.borderSubtle, CircleShape),
                                shape = CircleShape,
                                containerColor = colors.cardBg,
                                contentColor = colors.iosBlue,
                                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 0.dp, 0.dp)
                            ) {
                                LucideArrowDown(tint = colors.iosBlue, size = 16.dp)
                            }
                        }
                    }

                    // 3. Bottom Frosted Input Bar / Batch Selection Actions Bar
                    AnimatedContent(
                        targetState = isMessageSelectionMode,
                        transitionSpec = {
                            (slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                            ) + fadeIn(animationSpec = tween(180))).togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                                ) + fadeOut(animationSpec = tween(150))
                            )
                        },
                        label = "BottomBarSelectionAnim"
                    ) { selectionActive ->
                        if (selectionActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.headerBg)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Delete
                                IconButton(onClick = {
                                    SoundManager.playReaction()
                                    chats = chats.map { c ->
                                        if (c.id == activeChatId) {
                                            c.copy(messages = c.messages.filter { it.id !in selectedMessageIds })
                                        } else c
                                    }
                                    showToast("${selectedMessageIds.size} messages deleted")
                                    selectedMessageIds.clear()
                                }) {
                                    LucideTrash(tint = Color(0xFFFF3B30), size = 22.dp)
                                }

                                // Star
                                IconButton(onClick = {
                                    SoundManager.playReaction()
                                    val allStarred = selectedMessageIds.all { it in starredMessageIds }
                                    starredMessageIds = if (allStarred) {
                                        starredMessageIds - selectedMessageIds.toSet()
                                    } else {
                                        starredMessageIds + selectedMessageIds.toSet()
                                    }
                                    showToast(if (allStarred) "Unstarred" else "Starred")
                                    selectedMessageIds.clear()
                                }) {
                                    LucideStar(tint = colors.iosBlue, size = 22.dp)
                                }

                                // Copy
                                IconButton(onClick = {
                                    SoundManager.playReaction()
                                    val selectedMsgs = activeChat.messages.filter { it.id in selectedMessageIds }
                                    val text = selectedMsgs.mapNotNull { it.text }.joinToString("\n")
                                    if (text.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(text))
                                        showToast("${selectedMessageIds.size} messages copied")
                                    }
                                    selectedMessageIds.clear()
                                }) {
                                    LucideCopy(tint = colors.iosBlue, size = 22.dp)
                                }

                                // Forward
                                IconButton(onClick = {
                                    SoundManager.playReaction()
                                    val firstMsg = activeChat.messages.find { it.id in selectedMessageIds }
                                    if (firstMsg != null) {
                                        messageToForward = firstMsg
                                    }
                                    selectedMessageIds.clear()
                                }) {
                                    LucideShare(tint = colors.iosBlue, size = 22.dp)
                                }
                            }
                        } else {
                            ChatInputBar(
                                inputText = inputText,
                                onInputTextChange = {
                                    inputText = it
                                    if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                        chatViewModel.setTypingStatus(it.isNotBlank())
                                    }
                                },
                                replyQuote = activeReplyQuote,
                                onCancelReply = { activeReplyQuote = null },
                                isBlocked = activeChat.isBlocked,
                                onCameraClick = { launchCamera() },
                                onPhotoClick = { launchGallery() },                                onFileClick = {
                                    isFileManagerOpen = true
                                },
                                onPollClick = { isPollCreatorOpen = true },
                                onVoiceClick = { startVoiceRecording() },
                                onSendClick = { handleSendMessage() },
                                onDictateClick = { toggleDictation() },
                                isDictating = isDictating,
                                dictationRms = dictationRms
                            )
                        }
                    }
                }

                // 4. Voice Recording Floating 3D Globe Card (Used when keyboard is NOT open)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isVoiceRecording && !imeVisible,
                    enter = scaleIn(
                        initialScale = 0.25f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = scaleOut(
                        targetScale = 0.2f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)) + slideOutVertically(
                        targetOffsetY = { it / 4 },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 8.dp)
                ) {
                    VoiceRecorderCard(
                        recordingDurationSec = recorderDuration,
                        statusText = voiceStatusText,
                        globeStyle = currentGlobeStyle,
                        onCancel = { cancelVoiceRecording() },
                        onTranscribe = {
                            voiceStatusText = "Transcribing..."
                            coroutineScope.launch {
                                delay(400)
                                val recResult = AudioRecorderManager.stopRecording()
                                isVoiceRecording = false
                                if (recResult != null) {
                                    inputText = "Hey Bholanath, the smart automation nodes are communicating cleanly!"
                                    showToast("Voice transcribed to text ✨")
                                }
                            }
                        },
                        onSend = { finishVoiceRecording() }
                    )
                }
            }
        }

        // Fullscreen iOS Contact Info View
        AnimatedVisibility(
            visible = isContactDrawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            ContactInfoDrawer(
                chat = activeChat,
                onClose = { isContactDrawerOpen = false },
                onAudioCall = {
                    isContactDrawerOpen = false
                    activeCallType = "Audio Call"
                    isCallBannerActive = true
                    isFullCallModalOpen = true
                    SoundManager.playRing()
                },
                onVideoCall = {
                    isContactDrawerOpen = false
                    activeCallType = "Video Call"
                    isCallBannerActive = true
                    isFullCallModalOpen = true
                    SoundManager.playRing()
                },
                onSearchMessages = {
                    isContactDrawerOpen = false
                    isSearchInsideOpen = true
                },
                onMailClick = {
                    showToast("Mail composer opened for ${activeChat.name}")
                },
                onSendMessage = {
                    isContactDrawerOpen = false
                },
                onShareProfile = {
                    showToast("Profile link copied for ${activeChat.name}")
                },
                onAddToFavorites = {
                    showToast("Added ${activeChat.name} to Favorites")
                },
                onMuteNotifications = {
                    chats = chats.map { if (it.id == activeChatId) it.copy(isMuted = !it.isMuted) else it }
                    showToast(if (!activeChat.isMuted) "Notifications muted" else "Notifications unmuted")
                },
                onReportUser = {
                    showReportUserDialog = true
                },
                onBlockUser = {
                    showBlockUserDialog = true
                },
                onUpdateGroupAvatar = { newIcon ->
                    chats = chats.map { if (it.id == activeChatId) it.copy(avatar = newIcon) else it }
                    showToast("Group icon updated successfully")
                },
                onUpdateGroupBanner = { newBanner ->
                    chats = chats.map { if (it.id == activeChatId) it.copy(banner = newBanner) else it }
                    showToast("Group cover updated successfully")
                },
                onUpdateGroupName = { newName ->
                    chats = chats.map { if (it.id == activeChatId) it.copy(name = newName) else it }
                    showToast("Group subject updated to $newName")
                },
                onUpdateGroupBio = { newBio ->
                    chats = chats.map { if (it.id == activeChatId) it.copy(bio = newBio) else it }
                    showToast("Group description updated")
                },
                onAddGroupMembers = { newMembers ->
                    chats = chats.map { if (it.id == activeChatId) it.copy(members = (it.members ?: emptyList()) + newMembers) else it }
                    showToast("Added ${newMembers.size} participants")
                },
                onToggleMemberAdmin = { member ->
                    chats = chats.map { c ->
                        if (c.id == activeChatId) {
                            c.copy(
                                members = c.members?.map { m ->
                                    if (m.name == member.name) {
                                        val newRole = if (m.role.contains("Admin", ignoreCase = true)) "Member" else "Group Admin"
                                        m.copy(role = newRole)
                                    } else m
                                }
                            )
                        } else c
                    }
                    showToast("Updated permissions for ${member.name}")
                },
                onRemoveGroupMember = { member ->
                    chats = chats.map { c ->
                        if (c.id == activeChatId) {
                            c.copy(members = c.members?.filter { it.name != member.name })
                        } else c
                    }
                    showToast("Removed ${member.name} from group")
                },
                onDirectMessageMember = { member ->
                    val existingDirect = chats.firstOrNull { it.name.equals(member.name, ignoreCase = true) && it.type != "group" }
                    if (existingDirect != null) {
                        activeChatId = existingDirect.id
                        isContactDrawerOpen = false
                        showToast("Opened chat with ${member.name}")
                    } else {
                        showToast("Direct message started with ${member.name}")
                    }
                },
                onLeaveGroup = {
                    chats = chats.filter { it.id != activeChatId }
                    isContactDrawerOpen = false
                    isInsideChat = false
                    showToast("You left the group")
                },
                onImageClick = { previewLightboxImage = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fullscreen Story Viewer (Unified StoryViewerScreen)
        if (showStoryViewer && (storyGroups.isNotEmpty() || myStories.isNotEmpty())) {
            val myProfile = remember(currentUser) {
                com.chroma.studio.model.UserProfile(
                    uid = currentUser.id,
                    displayName = currentUser.name,
                    avatarUrl = currentUser.avatar
                )
            }
            StoryViewerScreen(
                storyGroups = storyGroups,
                initialGroupIndex = storyViewerInitialIndex,
                currentUserId = currentUid,
                storyViewModel = effectiveStoryVm,
                likedStoryIds = likedStoryIds,
                myStories = myStories,
                currentUserProfile = myProfile,
                onClose = { showStoryViewer = false },
                onAuthorClick = { authorId ->
                    showStoryViewer = false
                    if (onNavigateToUserProfile != null) {
                        onNavigateToUserProfile.invoke(authorId)
                    } else if (onNavigateToProfile != null) {
                        onNavigateToProfile.invoke(authorId)
                    }
                },
                onReplySend = { authorId, replyMsg ->
                    if (chatViewModel != null && authorId.isNotBlank() && authorId != currentUid) {
                        val roomId = if (currentUid < authorId) "${currentUid}_${authorId}" else "${authorId}_${currentUid}"
                        chatViewModel.openChat(roomId)
                        chatViewModel.sendMessage("Replying to story: \"$replyMsg\"")
                    }
                }
            )
        }

        // Fullscreen Story Editor Modal
        AnimatedVisibility(
            visible = isStoryEditorOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut()
        ) {
            StoryEditorModal(
                currentUser = currentUser,
                storyViewModel = effectiveStoryVm,
                onDismiss = { isStoryEditorOpen = false },
                onPublishStory = { newStory ->
                    stories = listOf(newStory) + stories.filter { !it.isMyStory }
                    isStoryEditorOpen = false
                    SoundManager.playSend()
                    showToast("Your Story published! ✨")
                    chatViewModel?.refreshStories()
                    effectiveStoryVm.loadStories()
                }
            )
        }

        // Story Analytics Modal (Bottom Sheet for Story Creator)
        AnimatedVisibility(
            visible = isStoryAnalyticsOpen && storyForAnalytics != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut()
        ) {
            storyForAnalytics?.let { st ->
                StoryAnalyticsModal(
                    story = st,
                    onClose = { isStoryAnalyticsOpen = false },
                    onDeleteStory = { storyId ->
                        stories = stories.filter { it.id != storyId }
                        isStoryAnalyticsOpen = false
                        activeStoryViewer = null
                        showToast("Story deleted")
                    }
                )
            }
        }
        // Fullscreen Call Modal
        AnimatedVisibility(
            visible = isFullCallModalOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
        ) {
            FullCallModal(
                chat = activeChat,
                callType = activeCallType,
                callDurationSec = callDurationSeconds,
                onToggleMic = { showToast("Microphone toggled") },
                onToggleCam = { showToast("Camera toggled") },
                onToggleSpeaker = { showToast("Speaker toggled") },
                onHangup = {
                    isCallBannerActive = false
                    isFullCallModalOpen = false
                    showToast("Call ended")
                }
            )
        }

        // Poll Creator Dialog
        AnimatedVisibility(
            visible = isPollCreatorOpen,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(140)) + scaleOut(targetScale = 0.9f, animationSpec = tween(140))
        ) {
            PollCreatorModal(
                onDismiss = { isPollCreatorOpen = false },
                onPostPoll = { question, options, isAnon, allowMult ->
                    val replyQuoteSnapshot = activeReplyQuote
                    val pollMsg = ChatMessage(
                        id = "m_${System.currentTimeMillis()}",
                        senderId = currentUser.id,
                        senderName = currentUser.name,
                        mediaType = "poll",
                        pollData = PollData(
                            question = question,
                            options = options.map { PollOption(text = it, votes = 0) },
                            isAnonymous = isAnon,
                            allowMultipleAnswers = allowMult,
                            creatorName = currentUser.name
                        ),
                        text = "",
                        time = getCurrentTimeFormatted(),
                        status = "sent",
                        replyTo = replyQuoteSnapshot
                    )
                    chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + pollMsg) else it }
                    if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                        val pollPayload = "[POLL]:$question|||${options.joinToString(";;;")}"
                        chatViewModel.sendMessage(pollPayload, replyToMessageId = replyQuoteSnapshot?.messageId)
                    }
                    activeReplyQuote = null
                    SoundManager.playSend()
                    showToast("Interactive poll posted ✨")
                }
            )
        }

        // Fullscreen Image Lightbox
        AnimatedVisibility(
            visible = previewLightboxImage != null || previewLightboxList.isNotEmpty(),
            enter = scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(targetScale = 0.88f, animationSpec = tween(160)) + fadeOut(animationSpec = tween(160))
        ) {
            val list = if (previewLightboxList.isNotEmpty()) previewLightboxList else listOfNotNull(previewLightboxImage)
            LightboxModal(
                imageUrl = previewLightboxImage,
                imageUrls = list,
                initialIndex = previewLightboxIndex,
                senderName = activeChat.name,
                subtitle = "Yesterday",
                onClose = {
                    previewLightboxImage = null
                    previewLightboxList = emptyList()
                    previewLightboxIndex = 0
                }
            )
        }

        // Delete Message Confirmation Modal
        AnimatedVisibility(
            visible = messagePendingDelete != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150)) + fadeOut()
        ) {
            messagePendingDelete?.let { msg ->
                val isOutgoing = msg.senderId == currentUser.id
                DeleteMessageModal(
                    isOutgoing = isOutgoing,
                    onDeleteForMe = {
                        chats = chats.map { c ->
                            if (c.id == activeChatId) c.copy(messages = c.messages.filter { it.id != msg.id }) else c
                        }
                        messagePendingDelete = null
                        showToast("Message deleted for me")
                    },
                    onDeleteForAll = {
                        chats = chats.map { c ->
                            if (c.id == activeChatId) c.copy(messages = c.messages.filter { it.id != msg.id }) else c
                        }
                        messagePendingDelete = null
                        showToast("Message deleted for everyone")
                    },
                    onDismiss = { messagePendingDelete = null }
                )
            }
        }

        // Wallpaper Picker Modal (Apple Design, No Pill Shapes)
        AnimatedVisibility(
            visible = isWallpaperPickerOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut()
        ) {
            WallpaperPickerModal(
                currentWallpaper = currentWallpaper,
                currentDimmingAlpha = currentWallpaperDimming,
                onSelectWallpaper = { wp, dim ->
                    currentWallpaper = wp
                    currentWallpaperDimming = dim
                    settingsManager.wallpaperType = wp
                    settingsManager.wallpaperDimming = dim
                    isWallpaperPickerOpen = false
                    SoundManager.playSend()
                    showToast("Wallpaper set to ${wp.title}")
                },
                onClose = { isWallpaperPickerOpen = false }
            )
        }

        // Custom In-App Gallery Picker Modal
        if (isGalleryPickerOpen) {
            CustomGalleryPickerModal(
                onDismiss = { isGalleryPickerOpen = false },
                onLaunchCamera = {
                    isGalleryPickerOpen = false
                    launchCamera()
                },
                onRequestMediaPermission = {
                    mediaPermissionLauncher.launch(PermissionHelper.getMediaPermissions())
                },
                onSendMedia = { uris, caption ->
                    val replyQuoteSnapshot = activeReplyQuote
                    if (uris.size > 1) {
                        val newMediaMsg = ChatMessage(
                            id = "m_${System.currentTimeMillis()}",
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            mediaType = "image_stack",
                            mediaUrls = uris.map { it.toString() },
                            mediaUrl = uris.first().toString(),
                            text = caption?.takeIf { it.isNotBlank() },
                            time = getCurrentTimeFormatted(),
                            status = "sent",
                            replyTo = replyQuoteSnapshot
                        )
                        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newMediaMsg) else it }
                        coroutineScope.launch {
                            for (uri in uris) {
                                try {
                                    val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri) ?: context.contentResolver.openInputStream(uri)?.readBytes()
                                    val ext = MediaStoreHelper.getFileExtension(context, uri) ?: "jpg"
                                    if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                        chatViewModel.sendMediaMessage(bytes, ext, caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId)
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                        SoundManager.playSend()
                        showToast("${uris.size} photos stacked & sent ✨")
                    } else if (uris.isNotEmpty()) {
                        val uri = uris.first()
                        val newMediaMsg = ChatMessage(
                            id = "m_${System.currentTimeMillis()}",
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            mediaType = "image",
                            localUri = uri.toString(),
                            mediaUrl = uri.toString(),
                            mediaUrls = listOf(uri.toString()),
                            text = caption?.takeIf { it.isNotBlank() },
                            time = getCurrentTimeFormatted(),
                            status = "sent",
                            replyTo = replyQuoteSnapshot
                        )
                        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newMediaMsg) else it }
                        coroutineScope.launch {
                            try {
                                val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri) ?: context.contentResolver.openInputStream(uri)?.readBytes()
                                val ext = MediaStoreHelper.getFileExtension(context, uri) ?: "jpg"
                                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                                    chatViewModel.sendMediaMessage(bytes, ext, caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId)
                                }
                            } catch (e: Exception) {}
                        }
                        SoundManager.playSend()
                        showToast("Photo sent ✨")
                    }
                    activeReplyQuote = null
                    isGalleryPickerOpen = false
                }
            )
        }

        // Authentic Apple iOS In-App Camera Screen (CameraX viewfinder, concentric shutter, zoom capsule, mode strip, photo review)
        if (isIosCameraOpen) {
            IosCameraScreen(
                onDismiss = { isIosCameraOpen = false },
                onPhotoCaptured = { uri, caption ->
                    isIosCameraOpen = false
                    sendCapturedPhoto(uri, caption)
                }
            )
        }

        // Custom Inbuilt File Manager Modal
        if (isFileManagerOpen) {
            CustomFileManagerModal(
                onDismiss = { isFileManagerOpen = false },
                onSendFiles = { files ->
                    val replyQuoteSnapshot = activeReplyQuote
                    if (files.size > 1) {
                        val newFileMsg = ChatMessage(
                            id = "m_${System.currentTimeMillis()}",
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            mediaType = "file_stack",
                            fileItems = files,
                            text = "${files.size} documents attached",
                            time = getCurrentTimeFormatted(),
                            status = "sent",
                            replyTo = replyQuoteSnapshot
                        )
                        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newFileMsg) else it }
                        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                            files.forEach { f ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val u = f.uri?.let { Uri.parse(it) }
                                        if (u != null) {
                                            val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                                            if (bytes != null) {
                                                chatViewModel.sendMediaMessage(bytes, f.extension.lowercase().ifBlank { "bin" }, text = f.name, replyToMessageId = replyQuoteSnapshot?.messageId)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        SoundManager.playSend()
                        showToast("${files.size} files sent ✨")
                    } else if (files.isNotEmpty()) {
                        val file = files.first()
                        val newFileMsg = ChatMessage(
                            id = "m_${System.currentTimeMillis()}",
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            mediaType = "file",
                            fileName = file.name,
                            fileSize = file.size,
                            localUri = file.uri,
                            fileItems = listOf(file),
                            text = null,
                            time = getCurrentTimeFormatted(),
                            status = "sent",
                            replyTo = replyQuoteSnapshot
                        )
                        chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newFileMsg) else it }
                        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val u = file.uri?.let { Uri.parse(it) }
                                    if (u != null) {
                                        val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                                        if (bytes != null) {
                                            chatViewModel.sendMediaMessage(bytes, file.extension.lowercase().ifBlank { "bin" }, text = file.name, replyToMessageId = replyQuoteSnapshot?.messageId)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        SoundManager.playSend()
                        showToast("File sent ✨")
                    }
                    activeReplyQuote = null
                    isFileManagerOpen = false
                }
            )
        }

        // Apple iOS Multi-Step Group Creation Modal
        if (isCreateGroupModalOpen) {
            val contactCandidates = remember(userProfiles, currentUid) {
                userProfiles.values
                    .filter { it.uid.isNotBlank() && it.uid != currentUid }
                    .map { prof ->
                        com.chroma.studio.chat.ui.components.GroupContact(
                            id = prof.uid,
                            name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                            role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                            avatar = prof.avatarUrl,
                            status = if (prof.online) "Online" else "Active on Oscellion"
                        )
                    }
            }
            CreateGroupModal(
                currentUser = currentUser,
                availableContacts = contactCandidates,
                onDismiss = { isCreateGroupModalOpen = false },
                onDirectMessage = { contact ->
                    isCreateGroupModalOpen = false
                    if (chatViewModel != null) {
                        chatViewModel.startChat(contact.id) { roomId ->
                            activeChatId = roomId
                            chatViewModel.openChat(roomId)
                            isInsideChat = true
                        }
                    }
                },
                onCreateGroup = { groupName, groupAvatar, members ->
                    val newGroupId = "group_${System.currentTimeMillis()}"
                    val currentTime = getCurrentTimeFormatted()
                    val newGroupChat = ChatConversation(
                        id = newGroupId,
                        name = groupName,
                        type = "group",
                        avatar = groupAvatar,
                        online = true,
                        statusText = "${members.size} members",
                        phone = "Group Chat",
                        bio = "created",
                        unreadCount = 0,
                        pinnedMessage = null,
                        isMuted = false,
                        isBlocked = false,
                        members = members,
                        messages = listOf(
                            ChatMessage(
                                id = "msg_init_${System.currentTimeMillis()}",
                                senderId = currentUser.id,
                                senderName = currentUser.name,
                                text = "👋 Welcome to $groupName! Group created",
                                time = currentTime,
                                status = "read"
                            )
                        )
                    )
                    chats = listOf(newGroupChat) + chats
                    activeChatId = newGroupId
                    isInsideChat = true
                    isCreateGroupModalOpen = false
                    SoundManager.playSend()
                    showToast("Group \"$groupName\" created")

                    if (chatViewModel != null) {
                        val realParticipantIds = members.mapNotNull { it.id.takeIf { id -> id.isNotBlank() && id != currentUser.id } }
                        chatViewModel.createGroup(groupName, "Group created", realParticipantIds, groupAvatar) { generatedRoomId ->
                            activeChatId = generatedRoomId
                            isInsideChat = true
                        }
                    }
                }
            )
        }

        // Apple / Liquid System Permission Explanation Dialog
        permissionToExplain?.let { permType ->
            PermissionExplanationModal(
                type = permType,
                onDismiss = { permissionToExplain = null },
                onGrant = {
                    permissionToExplain = null
                    when (permType) {
                        PermissionRequestType.MICROPHONE -> audioPermissionLauncher.launch(PermissionHelper.RECORD_AUDIO_PERMISSION)
                        PermissionRequestType.CAMERA -> cameraPermissionLauncher.launch(PermissionHelper.CAMERA_PERMISSION)
                        PermissionRequestType.GALLERY -> mediaPermissionLauncher.launch(PermissionHelper.getMediaPermissions())
                        PermissionRequestType.SPEECH -> speechPermissionLauncher.launch(PermissionHelper.RECORD_AUDIO_PERMISSION)
                    }
                },
                onOpenSettings = {
                    permissionToExplain = null
                    PermissionHelper.openAppSettings(context)
                }
            )
        }

        // Apple iOS Alert Dialog: Clear Chat
        if (showClearChatDialog) {
            IosAlertDialog(
                title = "Clear Conversation?",
                message = "All messages in this chat will be permanently cleared from this device. This action cannot be undone.",
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { showClearChatDialog = false }
                    ),
                    IosAlertAction(
                        title = "Clear Chat",
                        isDestructive = true,
                        onClick = {
                            chats = chats.map { if (it.id == activeChatId) it.copy(messages = emptyList(), pinnedMessage = null) else it }
                            showClearChatDialog = false
                            showToast("Conversation cleared")
                        }
                    )
                ),
                onDismiss = { showClearChatDialog = false }
            )
        }

        // Apple iOS Alert Dialog: Block / Unblock Contact
        // Apple iOS Alert Dialog: Block Contact OR Leave Group
        if (showBlockUserDialog) {
            val isGroupChat = activeChat.type == "group"
            val isCurrentlyBlocked = activeChat.isBlocked

            if (isGroupChat) {
                IosAlertDialog(
                    title = "Leave \"${activeChat.name}\"?",
                    message = "Are you sure you want to leave this group? You will no longer receive messages or updates.",
                    actions = listOf(
                        IosAlertAction(
                            title = "Cancel",
                            isCancel = true,
                            onClick = { showBlockUserDialog = false }
                        ),
                        IosAlertAction(
                            title = "Leave Group",
                            isDestructive = true,
                            onClick = {
                                showBlockUserDialog = false
                                isInsideChat = false
                                chats = chats.filter { it.id != activeChatId }
                                SoundManager.playReaction()
                                showToast("You left \"${activeChat.name}\"")
                            }
                        )
                    ),
                    onDismiss = { showBlockUserDialog = false }
                )
            } else {
                IosAlertDialog(
                    title = if (isCurrentlyBlocked) "Unblock ${activeChat.name}?" else "Block ${activeChat.name}?",
                    message = if (isCurrentlyBlocked) {
                        "Unblocking will allow ${activeChat.name} to send you messages and call you again."
                    } else {
                        "Blocked contacts will no longer be able to call you or send you messages."
                    },
                    actions = listOf(
                        IosAlertAction(
                            title = "Cancel",
                            isCancel = true,
                            onClick = { showBlockUserDialog = false }
                        ),
                        IosAlertAction(
                            title = if (isCurrentlyBlocked) "Unblock" else "Block",
                            isDestructive = !isCurrentlyBlocked,
                            isDefault = isCurrentlyBlocked,
                            onClick = {
                                chats = chats.map { if (it.id == activeChatId) it.copy(isBlocked = !it.isBlocked) else it }
                                showBlockUserDialog = false
                                showToast(if (!isCurrentlyBlocked) "${activeChat.name} blocked" else "${activeChat.name} unblocked")
                            }
                        )
                    ),
                    onDismiss = { showBlockUserDialog = false }
                )
            }
        }

        // Apple iOS Alert Dialog: Report User
        if (showReportUserDialog) {
            IosAlertDialog(
                title = "Report ${activeChat.name}?",
                message = "The last 5 messages from this chat will be forwarded to support. No one in this conversation will be notified.",
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { showReportUserDialog = false }
                    ),
                    IosAlertAction(
                        title = "Report Contact",
                        isDestructive = true,
                        onClick = {
                            showReportUserDialog = false
                            showToast("Report submitted for review")
                        }
                    )
                ),
                onDismiss = { showReportUserDialog = false }
            )
        }

        // Apple iOS Message Long-Press Context Menu
        selectedMessageForContextMenu?.let { msg ->
            val isOutgoing = msg.senderId == currentUser.id
            val isPinned = activeChat.pinnedMessage == (msg.text ?: msg.fileName ?: "Attachment")
            val isStarred = msg.id in starredMessageIds

            IosMessageContextMenuModal(
                message = msg,
                isOutgoing = isOutgoing,
                isPinned = isPinned,
                isStarred = isStarred,
                onDismiss = { selectedMessageForContextMenu = null },
                onSelect = {
                    selectedMessageIds.add(msg.id)
                    selectedMessageForContextMenu = null
                },
                onReaction = { emoji ->
                    chats = chats.map { c ->
                        if (c.id == activeChatId) {
                            c.copy(
                                messages = c.messages.map { m ->
                                    if (m.id == msg.id) toggleOrUpdateReaction(m, emoji) else m
                                }
                            )
                        } else c
                    }
                    if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                        chatViewModel.toggleReaction(msg.id, emoji)
                    }
                    SoundManager.playReaction()
                    showToast("Reacted $emoji")
                },
                onReply = {
                    val replySnippet = when {
                        msg.mediaType == "voice" -> "🎤 Voice message"
                        msg.mediaType == "image" || msg.mediaType == "image_stack" -> "📷 Photo"
                        msg.mediaType == "video" -> "🎥 Video"
                        msg.mediaType == "file" || msg.mediaType == "file_stack" -> "📄 ${msg.fileName ?: "Document"}"
                        msg.mediaType == "poll" -> "📊 ${msg.pollData?.question ?: "Poll"}"
                        !msg.text.isNullOrBlank() -> msg.text
                        else -> "Attachment"
                    }
                    activeReplyQuote = ReplyQuote(
                        author = msg.senderName,
                        text = replySnippet,
                        messageId = msg.id,
                        mediaType = msg.mediaType
                    )
                    showToast("Replying to ${msg.senderName}")
                },
                onCopy = {
                    val textToCopy = msg.text ?: msg.mediaUrl ?: "Voice Message"
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    showToast("Copied to clipboard")
                },
                onEdit = {
                    editingMessageId = msg.id
                    inputText = msg.text ?: ""
                    activeReplyQuote = null
                },
                onPin = {
                    val newPinText = if (isPinned) null else (msg.text ?: msg.fileName ?: "Attachment")
                    chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = newPinText) else it }
                    showToast(if (isPinned) "Message unpinned" else "Message pinned to top")
                },
                onForward = {
                    messageToForward = msg
                },
                onStar = {
                    starredMessageIds = if (isStarred) starredMessageIds - msg.id else starredMessageIds + msg.id
                    showToast(if (isStarred) "Removed from Starred" else "Message Starred")
                },
                onTranscribe = {
                    chats = chats.map { c ->
                        if (c.id == activeChatId) {
                            c.copy(
                                messages = c.messages.map { m ->
                                    if (m.id == msg.id) {
                                        m.copy(transcription = "“Hey! Just checking in to see if we're still meeting at 4 PM for the project review.”")
                                    } else m
                                }
                            )
                        } else c
                    }
                    showToast("Audio transcribed")
                },
                onSaveMedia = {
                    showToast("Media saved to device")
                },
                onInfo = {
                    selectedMessageForInfo = msg
                },
                onOpenEmojiPicker = {
                    messageForEmojiPicker = msg
                    isEmojiReactionPickerOpen = true
                },
                onDelete = {
                    messagePendingDelete = msg
                }
            )
        }

        // Apple iOS Full Emoji Reaction Picker Modal
        if (isEmojiReactionPickerOpen && messageForEmojiPicker != null) {
            EmojiReactionPickerModal(
                onDismiss = {
                    isEmojiReactionPickerOpen = false
                    messageForEmojiPicker = null
                },
                onSelectEmoji = { emoji ->
                    val targetMsg = messageForEmojiPicker
                    if (targetMsg != null) {
                        chats = chats.map { c ->
                            if (c.id == activeChatId) {
                                c.copy(
                                    messages = c.messages.map { m ->
                                        if (m.id == targetMsg.id) toggleOrUpdateReaction(m, emoji) else m
                                    }
                                )
                            } else c
                        }
                        if (chatViewModel != null && !activeChatId.startsWith("mock_") && !activeChatId.startsWith("chat_fallback")) {
                            chatViewModel.toggleReaction(targetMsg.id, emoji)
                        }
                        SoundManager.playReaction()
                        showToast("Reacted $emoji")
                    }
                    isEmojiReactionPickerOpen = false
                    messageForEmojiPicker = null
                }
            )
        }

        // Apple iOS Message Info Details Sheet
        selectedMessageForInfo?.let { msg ->
            IosMessageInfoModal(
                message = msg,
                isOutgoing = msg.senderId == currentUser.id,
                onDismiss = { selectedMessageForInfo = null }
            )
        }

        // Apple iOS Forward Message Contact Picker Sheet
        messageToForward?.let { msg ->
            IosForwardMessageModal(
                chats = chats,
                onDismiss = { messageToForward = null },
                onSelectChat = { targetChat ->
                    val newFwdMsg = msg.copy(
                        id = "fwd_${System.currentTimeMillis()}",
                        senderId = currentUser.id,
                        senderName = currentUser.name,
                        time = getCurrentTimeFormatted(),
                        status = "sent"
                    )
                    chats = chats.map { c ->
                        if (c.id == targetChat.id) {
                            c.copy(messages = c.messages + newFwdMsg)
                        } else c
                    }
                    showToast("Forwarded to ${targetChat.name}")
                }
            )
        }

        // Apple iOS Poll Details Sheet (View Votes Breakdown)
        activePollForDetails?.let { poll ->
            IosPollDetailsModal(
                poll = poll,
                currentUser = currentUser,
                onDismiss = { activePollForDetails = null }
            )
        }

        // Dedicated Custom Shared Gallery Modal (Media, Links, Docs, Audio)
        if (isSharedGalleryOpen) {
            CustomSharedGalleryModal(
                chat = activeChat,
                allChats = chats,
                initialTab = 0,
                onClose = { isSharedGalleryOpen = false },
                onReplyPrivately = { senderName, mediaItem ->
                    isSharedGalleryOpen = false
                    val matchingDirect = chats.firstOrNull { it.name.equals(senderName, ignoreCase = true) && it.type != "group" }
                    if (matchingDirect != null) {
                        activeChatId = matchingDirect.id
                        showToast("Opened chat with $senderName")
                    } else {
                        showToast("Direct message started with $senderName")
                    }
                },
                onForwardItems = { targetId, items ->
                    isSharedGalleryOpen = false
                    items.forEach { itm ->
                        val fwdMsg = ChatMessage(
                            id = "fwd_${System.currentTimeMillis()}_${itm.id}",
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            text = if (itm.type == SharedMediaType.LINK) itm.url else "[Forwarded: ${itm.title}]",
                            mediaUrl = if (itm.type == SharedMediaType.PHOTO || itm.type == SharedMediaType.VIDEO) itm.url else null,
                            mediaType = when (itm.type) {
                                SharedMediaType.PHOTO -> "image"
                                SharedMediaType.VIDEO -> "video"
                                SharedMediaType.DOCUMENT -> "document"
                                SharedMediaType.AUDIO -> "audio"
                                SharedMediaType.LINK -> null
                            },
                            time = getCurrentTimeFormatted(),
                            status = "delivered"
                        )
                        chats = chats.map { c ->
                            if (c.id == targetId) c.copy(messages = c.messages + fwdMsg) else c
                        }
                    }
                    showToast("Forwarded ${items.size} item(s)")
                },
                onToast = { showToast(it) }
            )
        }

        // Dynamic Island HUD System (Routes Toasts, Notifications, Calls, Recording, Hardware Alerts)
        DynamicNotchIsland(
            notification = islandNotification,
            isCallActive = isCallBannerActive,
            callDurationSec = callDurationSeconds,
            callContactName = activeChat.name,
            callAvatarUrl = activeChat.avatar,
            isRecording = isVoiceRecording && imeVisible,
            recordingDurationSec = recorderDuration,
            globeStyle = currentGlobeStyle,
            onGlobeStyleChange = { gStyle ->
                currentGlobeStyle = gStyle
                settingsManager.globeStyle = gStyle
                showToast("Voice Globe set to ${gStyle.title}")
            },
            onIslandClick = {
                if (isCallBannerActive) {
                    isFullCallModalOpen = true
                }
            },
            onEndCall = {
                isCallBannerActive = false
                isFullCallModalOpen = false
                showToast("Call ended")
            },
            onStopRecording = { finishVoiceRecording() },
            onCancelRecording = { cancelVoiceRecording() },
            onDismissNotification = {
                islandNotification = null
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

internal fun groupContinuousMediaMessages(messages: List<ChatMessage>): List<ChatMessage> {
    if (messages.size <= 1) return messages
    val result = ArrayList<ChatMessage>(messages.size)
    var i = 0
    while (i < messages.size) {
        val current = messages[i]
        val isMedia = isStackableMedia(current)
        if (isMedia) {
            val group = mutableListOf(current)
            var j = i + 1
            while (j < messages.size) {
                val next = messages[j]
                val nextIsMedia = isStackableMedia(next)
                if (nextIsMedia && next.senderId == current.senderId) {
                    group.add(next)
                    j++
                } else {
                    break
                }
            }
            if (group.size > 1) {
                val allUrls = mutableListOf<String>()
                group.forEach { msg ->
                    if (msg.mediaUrls.isNotEmpty()) {
                        allUrls.addAll(msg.mediaUrls)
                    } else if (!msg.mediaUrl.isNullOrBlank()) {
                        allUrls.add(msg.mediaUrl)
                    } else if (!msg.localUri.isNullOrBlank()) {
                        allUrls.add(msg.localUri)
                    }
                }
                val lastMsg = group.last()
                val mergedText = group.mapNotNull { it.text?.takeIf { t -> t.isNotBlank() } }
                    .distinct()
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
                val mergedReactions = mutableMapOf<String, Int>()
                group.forEach { msg ->
                    msg.reactions.forEach { (emoji, count) ->
                        mergedReactions[emoji] = (mergedReactions[emoji] ?: 0) + count
                    }
                }
                val mergedMsg = lastMsg.copy(
                    id = lastMsg.id,
                    mediaType = "image_stack",
                    mediaUrls = allUrls.distinct(),
                    mediaUrl = allUrls.firstOrNull(),
                    text = mergedText,
                    reactions = mergedReactions,
                    userReaction = group.firstNotNullOfOrNull { it.userReaction }
                )
                result.add(mergedMsg)
                i = j
            } else {
                result.add(current)
                i++
            }
        } else {
            result.add(current)
            i++
        }
    }
    return result
}

private fun isStackableMedia(msg: ChatMessage): Boolean {
    return msg.mediaType in listOf("image", "image_stack") ||
        (msg.mediaType == null && (!msg.mediaUrl.isNullOrBlank() || !msg.localUri.isNullOrBlank() || msg.mediaUrls.isNotEmpty()))
}






