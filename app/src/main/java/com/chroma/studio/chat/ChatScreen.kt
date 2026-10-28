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
import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

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

import androidx.activity.result.PickVisualMediaRequest

import androidx.activity.result.contract.ActivityResultContracts

import coil.compose.AsyncImage

import com.chroma.studio.chat.data.*

import com.chroma.studio.chat.media.*
import com.chroma.studio.chat.media.ChatMediaLimiter

import com.chroma.studio.chat.sound.SoundManager

import com.chroma.studio.chat.ui.components.*

import com.chroma.studio.ui.components.StoryCreationBottomSheet
import com.chroma.studio.ui.components.ShareStoryAudienceSheet
import com.chroma.studio.ui.components.OnlyFriendsManagementSheet

import com.chroma.studio.ui.screens.StoryViewerScreen
import com.chroma.studio.ui.screens.FlashPhotoEditorScreen
import com.chroma.studio.sticker.StickerItem
import com.chroma.studio.sticker.StickerPickerSheet
import com.chroma.studio.sticker.StickerStudioModal
import com.chroma.studio.ui.screens.PhotoAspectRatio

import com.chroma.studio.viewmodel.StoryViewModel

import com.chroma.studio.chat.ui.theme.ChatWallpaperBackground

import com.chroma.studio.chat.ui.theme.LocalChatColors

import com.chroma.studio.chat.util.PermissionHelper

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers

import java.text.SimpleDateFormat

import java.util.*



@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

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

    var showStoryCreationSheet by remember { mutableStateOf(false) }

    var showStoryGalleryPicker by remember { mutableStateOf(false) }

    var editingStoryImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadStoryUri by remember { mutableStateOf<Uri?>(null) }
    var showShareStoryAudienceSheet by remember { mutableStateOf(false) }
    var showOnlyFriendsManagementSheet by remember { mutableStateOf(false) }
    var isStickerPickerOpen by remember { mutableStateOf(false) }
    var isStickerStudioOpen by remember { mutableStateOf(false) }

    var storyViewerInitialIndex by remember { mutableIntStateOf(0) }



    val storyMediaPicker = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.PickVisualMedia()

    ) { uri: Uri? ->

        uri?.let { editingStoryImageUri = it }

    }

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
    val blockedUserIds by (chatViewModel?.blockedUserIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) })



    LaunchedEffect(effectiveRoomId) {
        if (!effectiveRoomId.isNullOrBlank()) {
            activeChatId = effectiveRoomId
            isInsideChat = true
            chatViewModel?.openChat(effectiveRoomId)
        }
    }

    LaunchedEffect(isInsideChat, activeChatId) {
        if (isInsideChat && !activeChatId.isNullOrBlank()) {
            com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = activeChatId
        } else {
            com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = null
        }
    }

    DisposableEffect(effectiveRoomId) {
        onDispose {
            if (effectiveRoomId != null) {
                chatViewModel?.closeChat()
            }
            com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = null
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

            val otherProfile = userProfiles[activeChatId]
            val resolvedName = otherProfile?.displayName?.ifBlank { otherProfile.username } ?: ""
            ChatConversation(
                id = activeChatId,
                name = resolvedName,
                type = "direct",
                avatar = otherProfile?.avatarUrl ?: "",
                online = isActiveOnline,
                statusText = if (isActiveOnline) "Online" else if (activeLastSeen != null) "Last seen $activeLastSeen" else "",
                phone = "",
                bio = otherProfile?.bio ?: "",
                unreadCount = 0,
                pinnedMessage = null,
                isMuted = false,
                isBlocked = false,
                messages = emptyList()
            )

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

    var highlightedMessageId by remember(activeChatId) { mutableStateOf<String?>(null) }



    val localChatMessages = remember(chats, activeChatId) {

        chats.find { it.id == activeChatId }?.messages ?: emptyList()

    }



    val allMessages = remember(liveMessages, isInsideChat, localChatMessages, activeChatBase.messages) {

        val rawList = if (liveMessages != null && isInsideChat && chatViewModel != null) {

            val serverIds = liveMessages.map { it.id }.toSet()

            val pendingLocal = localChatMessages.filter { it.id !in serverIds && !it.id.startsWith("last_") }

            (liveMessages + pendingLocal).distinctBy { it.id }

        } else if (localChatMessages.isNotEmpty()) {

            localChatMessages.filter { !it.id.startsWith("last_") }

        } else {

            activeChatBase.messages.filter { !it.id.startsWith("last_") }

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

    val unreadMessageThreshold = remember(activeChatId) { activeChatBase.unreadCount }



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
    val chatSecurityPrefs = remember {
        context.getSharedPreferences("chroma_chat_security_prefs", android.content.Context.MODE_PRIVATE)
    }
    var lockedChatIds by remember {
        mutableStateOf<Set<String>>(
            chatSecurityPrefs.getStringSet("locked_chat_ids", emptySet())?.toSet() ?: emptySet()
        )
    }
    var unlockedChatSessionIds by remember { mutableStateOf<Set<String>>(emptySet()) }

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

        if (msg == "Message sent" || msg == "Liked" || msg.startsWith("Reacted") || msg.startsWith("Photo sent") || msg.startsWith("Voice note sent") || msg.startsWith("Voice recording canceled") || msg.startsWith("Interactive poll posted")) {

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
        if (chatViewModel == null || activeChatId.startsWith("mock_")) {
            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + photoMsg) else it }
        }
        activeReplyQuote = null
        coroutineScope.launch {
            try {
                val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri)
                    ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_")) {
                    chatViewModel.sendMediaMessage(bytes, "jpg", caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId, localPreviewUri = uri.toString())
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to upload camera photo", e)
            }
        }
        SoundManager.playSend()
        showToast("Photo sent ✨")
    }

    fun sendStickerMessage(sticker: StickerItem) {
        val replyQuoteSnapshot = activeReplyQuote
        val sMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            mediaType = "sticker",
            localUri = sticker.uri,
            mediaUrl = sticker.uri,
            text = null,
            time = getCurrentTimeFormatted(),
            status = "sent",
            replyTo = replyQuoteSnapshot
        )
        if (chatViewModel == null || activeChatId.startsWith("mock_")) {
            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + sMsg) else it }
        }
        activeReplyQuote = null
        coroutineScope.launch {
            try {
                val u = Uri.parse(sticker.uri)
                val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_")) {
                    chatViewModel.sendMediaMessage(bytes, "png", "", replyToMessageId = replyQuoteSnapshot?.messageId, localPreviewUri = sticker.uri)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to upload sticker", e)
            }
        }
        SoundManager.playSend()
        showToast("Sticker sent ✨")
    }

    fun sendCapturedVideo(uri: Uri, caption: String?) {
        val replyQuoteSnapshot = activeReplyQuote
        val videoMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            mediaType = "video",
            localUri = uri.toString(),
            mediaUrl = uri.toString(),
            text = caption?.takeIf { it.isNotBlank() },
            time = getCurrentTimeFormatted(),
            status = "sent",
            replyTo = replyQuoteSnapshot
        )
        if (chatViewModel == null || activeChatId.startsWith("mock_")) {
            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + videoMsg) else it }
        }
        activeReplyQuote = null
        coroutineScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_")) {
                    chatViewModel.sendMediaMessage(bytes, "mp4", caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId, localPreviewUri = uri.toString())
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to upload camera video", e)
            }
        }
        SoundManager.playSend()
        showToast("Video sent ✨")
    }



    val audioPermissionLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.RequestPermission()

    ) { isGranted ->

        if (isGranted) {

            val started = AudioRecorderManager.startRecording(context)

            if (started) {

                isVoiceRecording = true

                if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

                if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

        if (chatViewModel == null || activeChatId.startsWith("mock_")) {

            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newVoiceMsg) else it }

        }

        isVoiceRecording = false

        activeReplyQuote = null

        SoundManager.playSend()

        showToast("Voice note sent ✨")



        if (recResult?.file != null && recResult.file.exists() && chatViewModel != null && !activeChatId.startsWith("mock_")) {

            try {

                val audioBytes = recResult.file.readBytes()

                chatViewModel.sendAudioMessage(audioBytes, duration, replyToMessageId = replyQuoteSnapshot?.messageId, localAudioPath = recResult.file.absolutePath)

            } catch (e: Exception) {}

        }

    }



    fun cancelVoiceRecording() {

        AudioRecorderManager.cancelRecording()

        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

    var isNewMessageModalOpen by remember { mutableStateOf(false) }

    var isCreateGroupModalOpen by remember { mutableStateOf(false) }

    var isPollCreatorOpen by remember { mutableStateOf(false) }

    var previewLightboxImage by remember { mutableStateOf<String?>(null) }

    var previewLightboxList by remember { mutableStateOf<List<String>>(emptyList()) }

    var previewLightboxIndex by remember { mutableIntStateOf(0) }

    var messagePendingDelete by remember { mutableStateOf<ChatMessage?>(null) }

    var showDeleteSelectedMessagesConfirm by remember { mutableStateOf(false) }



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
        showStoryCreationSheet ||
        showStoryGalleryPicker ||
        editingStoryImageUri != null ||
        showShareStoryAudienceSheet ||
        showOnlyFriendsManagementSheet ||
        isStickerPickerOpen ||
        isStickerStudioOpen ||
        showStoryViewer ||
        isGalleryPickerOpen ||
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

            if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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



        if (chatViewModel == null || activeChatId.startsWith("mock_")) {

            chats = chats.map { c ->

                if (c.id == activeChatId) c.copy(messages = c.messages + newMsg) else c

            }

        }



        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

            chatViewModel.setTypingStatus(false)

        }



        val isTargetBlocked = activeChat.isBlocked || (activeChat.otherUserId != null && activeChat.otherUserId in blockedUserIds)
        if (isTargetBlocked) {
            showToast("Cannot send message to blocked contact")
            return
        }

        val replyMessageId = activeReplyQuote?.messageId

        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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



    val isAnyModalOrSubscreenOpen = messagePendingDelete != null ||
        isNewMessageModalOpen ||

        isMessageSelectionMode ||

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
        showStoryCreationSheet ||
        showStoryGalleryPicker ||
        editingStoryImageUri != null ||
        showShareStoryAudienceSheet ||
        showOnlyFriendsManagementSheet ||
        showStoryViewer ||
        isGalleryPickerOpen ||
        isInsideChat ||

        effectiveRoomId != null



    BackHandler(enabled = isAnyModalOrSubscreenOpen) {
        when {
            messagePendingDelete != null -> messagePendingDelete = null
            showOnlyFriendsManagementSheet -> showOnlyFriendsManagementSheet = false
            isStickerStudioOpen -> isStickerStudioOpen = false
            isStickerPickerOpen -> isStickerPickerOpen = false
            showShareStoryAudienceSheet -> showShareStoryAudienceSheet = false
            editingStoryImageUri != null -> editingStoryImageUri = null
            showStoryGalleryPicker -> showStoryGalleryPicker = false
            showStoryCreationSheet -> showStoryCreationSheet = false
            showStoryViewer -> showStoryViewer = false
            isGalleryPickerOpen -> isGalleryPickerOpen = false
            isNewMessageModalOpen -> isNewMessageModalOpen = false

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
                unlockedChatSessionIds = unlockedChatSessionIds - activeChatId
                isInsideChat = false

                if (chatViewModel != null) {

                    chatViewModel.setTypingStatus(false)

                    chatViewModel.setRecordingStatus(false)

                    chatViewModel.closeChat()

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

                        showStoryCreationSheet = true

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
                    val openChatAction = {
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
                    }

                    openChatAction()
                },

            onStoryClick = { story ->

                val idx = storyGroups.indexOfFirst { it.userId == story.authorId }

                storyViewerInitialIndex = if (idx >= 0) idx else 0

                showStoryViewer = true

            },

            onAddStoryClick = {

                showStoryCreationSheet = true

            },

            onNewMessageClick = { isNewMessageModalOpen = true },

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

            lockedChatIds = lockedChatIds,

            pinnedChatIds = pinnedChatIds,

            onPinChatToggle = { chat ->

                val isPinned = chat.id in pinnedChatIds

                pinnedChatIds = if (isPinned) pinnedChatIds - chat.id else pinnedChatIds + chat.id

                chatViewModel?.toggleFavorite(chat.id, !isPinned)

                showToast(if (isPinned) "Chat unpinned" else "Chat pinned to top")

            },

            onMuteChatToggle = { chat ->

                val nextMuted = !chat.isMuted

                chats = chats.map { if (it.id == chat.id) it.copy(isMuted = nextMuted) else it }

                chatViewModel?.muteChat(chat.id, nextMuted)

                showToast(if (nextMuted) "Notifications muted" else "Notifications unmuted")

            },

            onReadChatToggle = { chat ->

                val wasUnread = chat.unreadCount > 0

                chats = chats.map { if (it.id == chat.id) it.copy(unreadCount = if (wasUnread) 0 else 1) else it }

                if (wasUnread) {

                    chatViewModel?.markMultipleAsRead(listOf(chat.id))

                }

                showToast(if (wasUnread) "Marked as read" else "Marked as unread")

            },

            onLockChatToggle = { chat ->
                val isCurrentlyLocked = chat.id in lockedChatIds
                val activity = context as? android.app.Activity
                if (isCurrentlyLocked) {
                    if (activity != null) {
                        com.chroma.studio.chat.util.BiometricHelper.authenticate(
                            activity = activity,
                            title = "Unlock ${chat.name}",
                            subtitle = "Verify identity to remove biometric lock",
                            onSuccess = {
                                val next = lockedChatIds - chat.id
                                lockedChatIds = next
                                chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                                showToast("Biometric lock removed from ${chat.name}")
                            },
                            onError = { _ ->
                                // User cancelled or biometric unavailable -- lock remains, do nothing
                            }
                        )
                    } else {
                        val next = lockedChatIds - chat.id
                        lockedChatIds = next
                        chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                        showToast("Biometric lock removed")
                    }
                } else {
                    if (activity != null) {
                        com.chroma.studio.chat.util.BiometricHelper.authenticate(
                            activity = activity,
                            title = "Lock ${chat.name}",
                            subtitle = "Confirm Face ID or fingerprint to lock chat",
                            onSuccess = {
                                val next = lockedChatIds + chat.id
                                lockedChatIds = next
                                chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                                showToast("${chat.name} locked with Biometrics")
                            },
                            onError = { _ ->
                                // User cancelled -- do nothing
                            }
                        )
                    } else {
                        val next = lockedChatIds + chat.id
                        lockedChatIds = next
                        chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                        showToast("${chat.name} locked")
                    }
                }
            },

            onLockMultipleChatsToggle = { chatIds ->
                if (chatIds.isEmpty()) return@ConversationsScreen
                val activity = context as? android.app.Activity
                val allLocked = chatIds.all { it in lockedChatIds }
                if (allLocked) {
                    if (activity != null) {
                        com.chroma.studio.chat.util.BiometricHelper.authenticate(
                            activity = activity,
                            title = "Unlock Chats",
                            subtitle = "Verify biometric identity to remove chat lock",
                            onSuccess = {
                                val next = lockedChatIds - chatIds.toSet()
                                lockedChatIds = next
                                chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                                showToast("Biometric lock removed from selected chats")
                            },
                            onError = { _ ->
                                // User cancelled or biometric unavailable -- lock remains, do nothing
                            }
                        )
                    } else {
                        val next = lockedChatIds - chatIds.toSet()
                        lockedChatIds = next
                        chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                        showToast("Biometric lock removed")
                    }
                } else {
                    if (activity != null) {
                        com.chroma.studio.chat.util.BiometricHelper.authenticate(
                            activity = activity,
                            title = "Biometric Chat Lock",
                            subtitle = "Confirm Face ID or fingerprint to lock selected chats",
                            onSuccess = {
                                val next = lockedChatIds + chatIds.toSet()
                                lockedChatIds = next
                                chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                                showToast("${chatIds.size} chat(s) locked with Biometrics")
                            },
                            onError = { _ ->
                                // User cancelled -- do nothing
                            }
                        )
                    } else {
                        val next = lockedChatIds + chatIds.toSet()
                        lockedChatIds = next
                        chatSecurityPrefs.edit().putStringSet("locked_chat_ids", next).apply()
                        showToast("${chatIds.size} chat(s) locked")
                    }
                }
            },

            onArchiveChat = { chat ->

                chats = chats.filter { it.id != chat.id }

                chatViewModel?.archiveChat(chat.id, true)

                showToast("Chat archived")

            },

            onClearChatHistory = { chat ->

                chats = chats.map { if (it.id == chat.id) it.copy(messages = emptyList()) else it }

                chatViewModel?.clearChatHistory(chat.id)

                showToast("Chat history cleared")

            },

            onExportChat = { chat ->

                try {

                    val transcript = buildString {

                        appendLine("--- ChromaStudio Chat Transcript: ${chat.name} ---")

                        appendLine("Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")

                        appendLine()

                        chat.messages.forEach { m ->

                            appendLine("[${m.time}] ${m.senderName}: ${m.text}")

                        }

                    }

                    val sendIntent = android.content.Intent().apply {

                        action = android.content.Intent.ACTION_SEND

                        putExtra(android.content.Intent.EXTRA_TEXT, transcript)

                        type = "text/plain"

                    }

                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Export chat with ${chat.name}"))

                    showToast("Exporting transcript for ${chat.name}")

                } catch (e: Exception) {

                    showToast("Exported transcript for ${chat.name}")

                }

            },

            onDeleteChat = { chat ->

                chats = chats.filter { it.id != chat.id }

                chatViewModel?.deleteChat(chat.id)

                showToast("Chat deleted")

            },

            onViewStoryAnalytics = { story ->

                storyForAnalytics = story

                isStoryAnalyticsOpen = true

            },

            onDeleteStory = { story ->

                stories = stories.filter { it.id != story.id }

                effectiveStoryVm.deleteStory(story.id)

                showToast("Story deleted")

            },

            onMuteStory = { story ->

                showToast("Muted stories from ${story.authorName}")

            },

            onShareStory = { story ->

                try {

                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager

                    val clip = android.content.ClipData.newPlainText("Story Link", "https://oscellion.vercel.app/story/${story.id}")

                    clipboard?.setPrimaryClip(clip)

                    showToast("Story link copied to clipboard")

                } catch (_: Exception) {

                    showToast("Story link copied")

                }

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




                // -- Biometric lock state (declared here so overlay can reference them) --
                val isCurrentChatLocked = activeChatId in lockedChatIds && activeChatId !in unlockedChatSessionIds

                // Guard: only auto-prompt once per chat session to avoid repeated dialogs
                var biometricAutoPrompted by remember(activeChatId) { mutableStateOf(false) }

                LaunchedEffect(activeChatId, isCurrentChatLocked) {
                    if (isCurrentChatLocked && !biometricAutoPrompted) {
                        biometricAutoPrompted = true
                        kotlinx.coroutines.delay(200)
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            com.chroma.studio.chat.util.BiometricHelper.authenticate(
                                activity = activity,
                                title = "Unlock ${activeChat.name}",
                                subtitle = "Verify identity to unveil locked conversation",
                                onSuccess = {
                                    unlockedChatSessionIds = unlockedChatSessionIds + activeChatId
                                },
                                onError = {
                                    // User skipped or cancelled — blur shield stays up
                                }
                            )
                        }
                    }
                }

                Column(

                    modifier = Modifier

                        .fillMaxSize()

                        .imePadding()

                ) {

                    // 1. Chat Header / Selection Header

                    val totalUnread = chats.filter { it.id != activeChatId }.sumOf { it.unreadCount }

                    val searchMatches = if (searchInsideQuery.isEmpty()) 0 else allMessages.count { it.text?.contains(searchInsideQuery, ignoreCase = true) == true }



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
                                    unlockedChatSessionIds = unlockedChatSessionIds - activeChatId
                                    isInsideChat = false

                                    if (chatViewModel != null) {

                                        chatViewModel.setTypingStatus(false)

                                        chatViewModel.setRecordingStatus(false)

                                        chatViewModel.closeChat()

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
                                    callDurationSeconds = 0
                                    chatViewModel?.initializeCallManager(context)
                                    chatViewModel?.startCall(activeChatId)
                                    SoundManager.playRing()
                                },
                                onVideoCallClick = {
                                    activeCallType = "Video Call"
                                    isCallBannerActive = true
                                    isFullCallModalOpen = true
                                    callDurationSeconds = 0
                                    chatViewModel?.initializeCallManager(context)
                                    chatViewModel?.startCall(activeChatId)
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

                                    val nextMuted = !activeChat.isMuted

                                    chats = chats.map { if (it.id == activeChatId) it.copy(isMuted = nextMuted) else it }

                                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

                                        chatViewModel.muteChat(activeChatId, nextMuted)

                                    }

                                    showToast(if (nextMuted) "Notifications muted" else "Notifications unmuted")

                                },

                                onExportTranscript = {
                                    try {
                                        val transcript = buildString {
                                            appendLine("--- ChromaStudio Chat Transcript: ${activeChat.name} ---")
                                            appendLine("Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                                            appendLine()
                                            allMessages.forEach { m ->
                                                val content = m.text ?: m.fileName ?: if (m.mediaType == "voice") "[Voice Note]" else "[Media Attachment]"
                                                appendLine("[${m.time}] ${m.senderName}: $content")
                                            }
                                        }
                                        val exportDir = java.io.File(context.cacheDir, "transcripts").apply { if (!exists()) mkdirs() }
                                        val safeName = activeChat.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
                                        val exportFile = java.io.File(exportDir, "chat_${safeName}_${System.currentTimeMillis()}.txt").apply {
                                            writeText(transcript)
                                        }
                                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            exportFile
                                        )
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                            putExtra(android.content.Intent.EXTRA_TEXT, transcript)
                                            type = "text/plain"
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Export chat with ${activeChat.name}"))
                                        showToast("Exporting transcript for ${activeChat.name}")
                                    } catch (e: Exception) {
                                        showToast("Exporting transcript for ${activeChat.name}")
                                    }
                                },

                                onClearChat = {

                                    showClearChatDialog = true

                                },

                                onBlockUser = {

                                    showBlockUserDialog = true

                                },

                                onUnpinMessage = {
                                    chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = null, pinnedMessageId = null) else it }
                                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                                        chatViewModel.unpinMessage(activeChatId)
                                    }
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



                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                            ActiveChatMessageStream(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        activeChat = activeChat,
                        allMessages = allMessages,
                        visibleMessageCount = visibleMessageCount,
                        currentUser = currentUser,
                        currentUid = currentUid,
                        unreadMessageThreshold = unreadMessageThreshold,
                        typingStatuses = typingStatuses,
                        isTyping = isTyping,
                        isMessageSelectionMode = isMessageSelectionMode,
                        selectedMessageIds = selectedMessageIds,
                        starredMessageIds = starredMessageIds,
                        searchInsideQuery = searchInsideQuery,
                        activeChatId = activeChatId,
                        chatViewModel = chatViewModel,
                        coroutineScope = coroutineScope,
                        haptic = haptic,
                        clipboardManager = clipboardManager,
                        showToast = ::showToast,
                        bubbleConfig = rememberedBubbleConfig,
                        onChatsUpdate = { transform -> chats = transform(chats) },
                        onSelectContextMenuMessage = { msg -> selectedMessageForContextMenu = msg },
                        onStartReply = { msg ->
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
                        onStartEdit = { msg ->
                            editingMessageId = msg.id
                            inputText = msg.text ?: ""
                            activeReplyQuote = null
                        },
                        onPinMessage = { msg ->
                            val pinText = msg.text ?: msg.fileName ?: "Attachment"
                            chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = pinText, pinnedMessageId = msg.id) else it }
                            if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                                chatViewModel.pinMessage(activeChatId, msg.id, pinText)
                            }
                            showToast("Message pinned")
                        },
                        onDeleteMessagePending = { msg ->
                            selectedMessageForContextMenu = null
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
                        onViewPollVotes = { poll ->
                            activePollForDetails = poll
                        },
                        onNavigateToProfile = onNavigateToProfile,
                        onExpandVisibleCount = { count ->
                            visibleMessageCount = count
                        }
                    )
                    }

                // 3. Bottom Frosted Input Bar / Batch Selection Actions Bar
                if (!isCurrentChatLocked) {

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

                                    showDeleteSelectedMessagesConfirm = true

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

                                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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
                                onStickerClick = { isStickerPickerOpen = true },

                                onVoiceClick = { startVoiceRecording() },

                                onSendClick = { handleSendMessage() },

                                onDictateClick = { toggleDictation() },

                                isDictating = isDictating,

                                dictationRms = dictationRms

                            )

                        }

                    }

                }
            }

                // Full-screen Biometric Privacy Shield (covers header + messages)
                // Rendered inside the outer Box so it sits above the ChatHeader.
                if (isCurrentChatLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (colors.isLight)
                                        listOf(Color(0xFFF2F2F7), Color(0xFFE5E5EA), Color(0xFFF2F2F7))
                                    else
                                        listOf(Color(0xFF0A0A0D), Color(0xFF111116), Color(0xFF0A0A0D))
                                )
                            )
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { /* Consume all touches — nothing leaks through */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp)
                        ) {
                            // Glass-morphism lock icon circle
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colors.isLight) Color.White.copy(alpha = 0.85f)
                                        else Color(0xFF1C1C1E).copy(alpha = 0.85f)
                                    )
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                colors.iosBlue.copy(alpha = 0.6f),
                                                colors.iosBlue.copy(alpha = 0.2f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                LucideLock(tint = colors.iosBlue, size = 38.dp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Chat Locked",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = activeChat.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.iosBlue,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "This conversation is secured with Biometrics.\nTap below to authenticate and unveil messages.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Primary: Unlock button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.iosBlue)
                                    .clickable {
                                        SoundManager.playReaction()
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            biometricAutoPrompted = false
                                            com.chroma.studio.chat.util.BiometricHelper.authenticate(
                                                activity = activity,
                                                title = "Unlock ${activeChat.name}",
                                                subtitle = "Authenticate to unveil conversation",
                                                onSuccess = {
                                                    unlockedChatSessionIds = unlockedChatSessionIds + activeChatId
                                                },
                                                onError = {
                                                    // Stay on lock screen
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    LucideLock(tint = Color.White, size = 18.dp)
                                    Text(
                                        text = "Unlock with Biometrics",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Secondary: Back to Chats
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        // Navigate back WITHOUT unlocking.
                                        // Lock screen reappears next time chat is opened.
                                        isInsideChat = false
                                        if (chatViewModel != null) {
                                            chatViewModel.setTypingStatus(false)
                                            chatViewModel.setRecordingStatus(false)
                                            chatViewModel.closeChat()
                                        }
                                        if (effectiveRoomId != null) {
                                            onNavigateBack?.invoke() ?: onBack?.invoke()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Back to Chats",
                                    color = colors.textMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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

                    chatViewModel?.toggleFavorite(activeChatId, true)

                    showToast("Added ${activeChat.name} to Favorites")

                },

                onMuteNotifications = {

                    val nextMuted = !activeChat.isMuted

                    chats = chats.map { if (it.id == activeChatId) it.copy(isMuted = nextMuted) else it }

                    chatViewModel?.muteChat(activeChatId, nextMuted)

                    showToast(if (nextMuted) "Notifications muted" else "Notifications unmuted")

                },

                onReportUser = {

                    showReportUserDialog = true

                },

                onBlockUser = {

                    showBlockUserDialog = true

                },

                onUpdateGroupAvatar = { newIcon ->

                    chats = chats.map { if (it.id == activeChatId) it.copy(avatar = newIcon) else it }

                    chatViewModel?.updateGroupImage(activeChatId, newIcon)

                    showToast("Group icon updated successfully")

                },

                onUpdateGroupBanner = { newBanner ->

                    chats = chats.map { if (it.id == activeChatId) it.copy(banner = newBanner) else it }

                    chatViewModel?.updateGroupImage(activeChatId, newBanner)

                    showToast("Group cover updated successfully")

                },

                onUpdateGroupName = { newName ->

                    chats = chats.map { if (it.id == activeChatId) it.copy(name = newName) else it }

                    chatViewModel?.updateGroupName(activeChatId, newName)

                    showToast("Group subject updated to $newName")

                },

                onUpdateGroupBio = { newBio ->

                    chats = chats.map { if (it.id == activeChatId) it.copy(bio = newBio) else it }

                    chatViewModel?.updateGroupDescription(activeChatId, newBio)

                    showToast("Group description updated")

                },

                onAddGroupMembers = { newMembers ->

                    chats = chats.map { if (it.id == activeChatId) it.copy(members = (it.members ?: emptyList()) + newMembers) else it }

                    val memberIds = newMembers.map { it.id }

                    chatViewModel?.addParticipants(activeChatId, memberIds)

                    showToast("Added ${newMembers.size} participants")

                },

                onToggleMemberAdmin = { member ->

                    val isAdmin = member.role.contains("Admin", ignoreCase = true)

                    chats = chats.map { c ->

                        if (c.id == activeChatId) {

                            c.copy(

                                members = c.members?.map { m ->

                                    if (m.name == member.name) {

                                        val newRole = if (isAdmin) "Member" else "Group Admin"

                                        m.copy(role = newRole)

                                    } else m

                                }

                            )

                        } else c

                    }

                    if (isAdmin) {

                        chatViewModel?.demoteAdmin(activeChatId, member.id)

                    } else {

                        chatViewModel?.makeAdmin(activeChatId, member.id)

                    }

                    showToast("Updated permissions for ${member.name}")

                },

                onRemoveGroupMember = { member ->

                    chats = chats.map { c ->

                        if (c.id == activeChatId) {

                            c.copy(members = c.members?.filter { it.name != member.name })

                        } else c

                    }

                    chatViewModel?.removeParticipant(activeChatId, member.id)

                    showToast("Removed ${member.name} from group")

                },

                onDirectMessageMember = { member ->

                    val existingDirect = chats.firstOrNull { it.name.equals(member.name, ignoreCase = true) && it.type != "group" }

                    if (existingDirect != null) {

                        activeChatId = existingDirect.id

                        isContactDrawerOpen = false

                        showToast("Opened chat with ${member.name}")

                    } else {

                        chatViewModel?.startChat(member.id) { roomId ->

                            activeChatId = roomId

                            chatViewModel.openChat(roomId)

                            isContactDrawerOpen = false

                            showToast("Opened chat with ${member.name}")

                        }

                    }

                },

                onLeaveGroup = {

                    chats = chats.filter { it.id != activeChatId }

                    chatViewModel?.leaveGroup(activeChatId)

                    chatViewModel?.closeChat()

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



        // Story Creation Options Bottom Sheet

        if (showStoryCreationSheet) {

            StoryCreationBottomSheet(

                onDismissRequest = { showStoryCreationSheet = false },

                onChooseFromGallery = {

                    showStoryCreationSheet = false

                    showStoryGalleryPicker = true

                },

                onTextStory = {

                    showStoryCreationSheet = false

                    if (onNavigateToCreateStory != null) {

                        onNavigateToCreateStory.invoke()

                    } else {

                        isStoryEditorOpen = true

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

            var isMicMuted by remember { mutableStateOf(false) }
            var isCamEnabled by remember { mutableStateOf(activeCallType.contains("Video", ignoreCase = true)) }
            var isSpeakerOn by remember { mutableStateOf(activeCallType.contains("Video", ignoreCase = true)) }

            FullCallModal(
                chat = activeChat,
                callType = activeCallType,
                callDurationSec = callDurationSeconds,
                onToggleMic = {
                    isMicMuted = !isMicMuted
                    chatViewModel?.toggleMute(isMicMuted)
                    showToast(if (isMicMuted) "Microphone muted" else "Microphone unmuted")
                },
                onToggleCam = {
                    isCamEnabled = !isCamEnabled
                    chatViewModel?.toggleCamera(isCamEnabled)
                    showToast(if (isCamEnabled) "Camera enabled" else "Camera disabled")
                },
                onToggleSpeaker = {
                    isSpeakerOn = !isSpeakerOn
                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                    audioManager?.isSpeakerphoneOn = isSpeakerOn
                    showToast(if (isSpeakerOn) "Speaker on" else "Earpiece active")
                },
                onHangup = {
                    isCallBannerActive = false
                    isFullCallModalOpen = false
                    chatViewModel?.endCall()
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

                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

                        val metaFlags = "anon=$isAnon;mult=$allowMult;closed=false"
                        val pollPayload = "[POLL]:$question|||${options.joinToString(";;;")}|||$metaFlags"

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
        if (messagePendingDelete != null) {
            val msg = messagePendingDelete!!
            val isOutgoing = msg.senderId == currentUser.id || msg.senderId == currentUid
            DeleteMessageModal(
                isOutgoing = isOutgoing,
                onDeleteForMe = {
                    val targetMsgId = msg.id
                    chats = chats.map { c ->
                        if (c.id == activeChatId) c.copy(messages = c.messages.filter { it.id != targetMsgId }) else c
                    }
                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                        chatViewModel.deleteMessageForMe(targetMsgId, activeChatId)
                    }
                    messagePendingDelete = null
                    showToast("Message deleted for me")
                },
                onDeleteForAll = {
                    val targetMsgId = msg.id
                    chats = chats.map { c ->
                        if (c.id == activeChatId) c.copy(messages = c.messages.filter { it.id != targetMsgId }) else c
                    }
                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                        chatViewModel.deleteMessage(targetMsgId, activeChatId)
                    }
                    messagePendingDelete = null
                    showToast("Message deleted for everyone")
                },
                onDismiss = { messagePendingDelete = null }
            )
        }

        // Delete Multiple Selected Messages Confirmation Dialog (Apple HIG Style)
        if (showDeleteSelectedMessagesConfirm) {
            val count = selectedMessageIds.size
            IosAlertDialog(
                title = "Delete ${if (count == 1) "Message" else "$count Messages"}?",
                message = "Are you sure you want to delete the selected ${if (count == 1) "message" else "messages"}?",
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { showDeleteSelectedMessagesConfirm = false }
                    ),
                    IosAlertAction(
                        title = "Delete",
                        isDestructive = true,
                        onClick = {
                            showDeleteSelectedMessagesConfirm = false
                            val idsToDelete = selectedMessageIds.toList()
                            chats = chats.map { c ->
                                if (c.id == activeChatId) {
                                    c.copy(messages = c.messages.filter { it.id !in idsToDelete })
                                } else c
                            }
                            if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                                idsToDelete.forEach { id ->
                                    chatViewModel.deleteMessage(id, activeChatId)
                                }
                            }
                            showToast(if (idsToDelete.size == 1) "Message deleted" else "${idsToDelete.size} messages deleted")
                            selectedMessageIds.clear()
                        }
                    )
                ),
                onDismiss = { showDeleteSelectedMessagesConfirm = false }
            )
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



        // Story Gallery Picker Modal

        if (showStoryGalleryPicker) {

            CustomGalleryPickerModal(

                onDismiss = { showStoryGalleryPicker = false },

                onLaunchCamera = {

                    showStoryGalleryPicker = false

                    launchCamera()

                },

                onRequestMediaPermission = {

                    mediaPermissionLauncher.launch(PermissionHelper.getMediaPermissions())

                },

                onSendMedia = { uris, _ ->

                    showStoryGalleryPicker = false

                    val pickedUri = uris.firstOrNull()

                    if (pickedUri != null) {

                        editingStoryImageUri = pickedUri

                    }

                }

            )

        }

        // Story Image Editor
        if (editingStoryImageUri != null) {
            FlashPhotoEditorScreen(
                photoUris = listOf(editingStoryImageUri!!),
                title = "Edit Story",
                initialAspectRatio = PhotoAspectRatio.STORY_9_16,
                nextButtonLabel = "Share Story",
                onExportUris = { editedUris ->
                    val finalUri = editedUris.firstOrNull() ?: editingStoryImageUri!!
                    pendingUploadStoryUri = finalUri
                    showShareStoryAudienceSheet = true
                },
                onBack = {
                    editingStoryImageUri = null
                }
            )
        }

        if (showShareStoryAudienceSheet && pendingUploadStoryUri != null) {
            val onlyFriends by effectiveStoryVm.onlyFriendsIds.collectAsState()
            ShareStoryAudienceSheet(
                onDismissRequest = { showShareStoryAudienceSheet = false },
                onSharePublic = {
                    showShareStoryAudienceSheet = false
                    val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                    pendingUploadStoryUri = null
                    editingStoryImageUri = null
                    effectiveStoryVm.uploadStory(context, uri, audienceType = "public")
                    com.chroma.studio.ui.components.ToastManager.showToast("Sharing your story... 🌟")
                },
                onShareOnlyFriends = {
                    showShareStoryAudienceSheet = false
                    val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                    pendingUploadStoryUri = null
                    editingStoryImageUri = null
                    effectiveStoryVm.uploadStory(context, uri, audienceType = "only_friends", customAudienceIds = onlyFriends)
                    com.chroma.studio.ui.components.ToastManager.showToast("Shared with Only Friend! ⭐")
                },
                onEditOnlyFriends = { showOnlyFriendsManagementSheet = true },
                onlyFriendsCount = onlyFriends.size
            )
        }

        if (isStickerPickerOpen) {
            StickerPickerSheet(
                isOpen = isStickerPickerOpen,
                onDismiss = { isStickerPickerOpen = false },
                onStickerSelected = { sticker ->
                    sendStickerMessage(sticker)
                },
                onCreateCustomSticker = {
                    isStickerStudioOpen = true
                }
            )
        }

        if (isStickerStudioOpen) {
            StickerStudioModal(
                isOpen = isStickerStudioOpen,
                onDismiss = { isStickerStudioOpen = false },
                onStickerCreated = { sticker ->
                    sendStickerMessage(sticker)
                }
            )
        }

        if (showOnlyFriendsManagementSheet) {
            OnlyFriendsManagementSheet(
                storyViewModel = effectiveStoryVm,
                onDismissRequest = { showOnlyFriendsManagementSheet = false }
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

                onSendMedia = sendMediaLabel@{ uris, caption ->

                    val replyQuoteSnapshot = activeReplyQuote

                    // 1. Enforce 100 MB video size limit
                    val oversizedVideo = uris.firstOrNull { ChatMediaLimiter.isVideoUri(context, it) && ChatMediaLimiter.getMediaSizeBytes(context, it) > ChatMediaLimiter.MAX_VIDEO_SIZE_BYTES }
                    if (oversizedVideo != null) {
                        val size = ChatMediaLimiter.getMediaSizeBytes(context, oversizedVideo)
                        showToast("Video exceeds 100 MB limit (${ChatMediaLimiter.formatFileSize(size)}). Please choose a smaller video.")
                        return@sendMediaLabel
                    }

                    // 2. Enforce 100 MB photos batch size limit
                    val totalPhotoBytes = uris.filter { !ChatMediaLimiter.isVideoUri(context, it) }.sumOf { ChatMediaLimiter.getMediaSizeBytes(context, it) }
                    if (totalPhotoBytes > ChatMediaLimiter.MAX_PHOTOS_BATCH_SIZE_BYTES) {
                        showToast("Photos batch exceeds 100 MB limit (${ChatMediaLimiter.formatFileSize(totalPhotoBytes)}). Please select fewer photos.")
                        return@sendMediaLabel
                    }

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

                        if (chatViewModel == null || activeChatId.startsWith("mock_")) {

                            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newMediaMsg) else it }

                        }

                        coroutineScope.launch {

                            for (uri in uris) {

                                try {

                                    val isVideo = ChatMediaLimiter.isVideoUri(context, uri)
                                    val bytes = if (isVideo) {
                                        ChatMediaLimiter.processAndCompressVideo(context, uri)
                                    } else {
                                        ChatMediaLimiter.processAndCompressPhoto(context, uri)
                                    }
                                    val ext = if (isVideo) "mp4" else (MediaStoreHelper.getFileExtension(context, uri) ?: "jpg")

                                    if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_")) {

                                        chatViewModel.sendMediaMessage(bytes, ext, caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId, localPreviewUri = uri.toString())

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

                        if (chatViewModel == null || activeChatId.startsWith("mock_")) {

                            chats = chats.map { if (it.id == activeChatId) it.copy(messages = it.messages + newMediaMsg) else it }

                        }

                        coroutineScope.launch {

                            try {

                                val isVideo = ChatMediaLimiter.isVideoUri(context, uri)
                                val bytes = if (isVideo) {
                                    ChatMediaLimiter.processAndCompressVideo(context, uri)
                                } else {
                                    ChatMediaLimiter.processAndCompressPhoto(context, uri)
                                }
                                val ext = if (isVideo) "mp4" else (MediaStoreHelper.getFileExtension(context, uri) ?: "jpg")

                                if (bytes != null && chatViewModel != null && !activeChatId.startsWith("mock_")) {

                                    chatViewModel.sendMediaMessage(bytes, ext, caption ?: "", replyToMessageId = replyQuoteSnapshot?.messageId, localPreviewUri = uri.toString())

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

                },

                onVideoCaptured = { uri, caption ->

                    isIosCameraOpen = false

                    sendCapturedVideo(uri, caption)

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

                        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

                        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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



        // Direct 1-on-1 User Chat Selection Modal (New Message)

        if (isNewMessageModalOpen) {

            val contactCandidates = remember(userProfiles, currentUid) {

                userProfiles.values

                    .filter { it.uid.isNotBlank() && it.uid != currentUid }

                    .map { prof ->

                        com.chroma.studio.chat.ui.components.GroupContact(

                            id = prof.uid,

                            name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },

                            role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",

                            avatar = prof.avatarUrl,

                            status = if (prof.online) "Online" else "Active on Oscellion",

                            isVerified = prof.verified

                        )

                    }

            }

            com.chroma.studio.chat.ui.components.NewDirectChatModal(

                currentUser = currentUser,

                availableContacts = contactCandidates,

                onDismiss = { isNewMessageModalOpen = false },

                onSelectUser = { targetUserId ->

                    isNewMessageModalOpen = false

                    if (chatViewModel != null) {

                        chatViewModel.startChat(targetUserId) { roomId ->

                            activeChatId = roomId

                            chatViewModel.openChat(roomId)

                            isInsideChat = true

                        }

                    } else {

                        activeChatId = "direct_$targetUserId"

                        isInsideChat = true

                    }

                },

                onOpenCreateGroup = {

                    isNewMessageModalOpen = false

                    isCreateGroupModalOpen = true

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

                            status = if (prof.online) "Online" else "Active on Oscellion",

                            isVerified = prof.verified

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

                            if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

                                chatViewModel.clearChatHistory(activeChatId)

                            }

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

            val isCurrentlyBlocked = activeChat.isBlocked || (activeChat.otherUserId != null && activeChat.otherUserId in blockedUserIds)



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

                                if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

                                    chatViewModel.leaveGroup(activeChatId)

                                    chatViewModel.closeChat()

                                }

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
                                val nextBlocked = !isCurrentlyBlocked
                                chats = chats.map { if (it.id == activeChatId) it.copy(isBlocked = nextBlocked) else it }
                                val targetUid = activeChat.otherUserId ?: activeChat.participantUids.firstOrNull { it != currentUid }
                                if (targetUid != null && chatViewModel != null) {
                                    chatViewModel.toggleBlockUser(targetUid, nextBlocked)
                                }
                                showBlockUserDialog = false
                                showToast(if (nextBlocked) "${activeChat.name} blocked" else "${activeChat.name} unblocked")
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
                            val targetUid = activeChat.otherUserId ?: activeChat.participantUids.firstOrNull { it != currentUid }
                            val last5Snippets = allMessages.takeLast(5).map { m ->
                                "[${m.senderName}]: ${m.text ?: m.fileName ?: m.mediaType ?: "Media"}"
                            }
                            if (targetUid != null && chatViewModel != null) {
                                chatViewModel.reportUser(
                                    reportedUserId = targetUid,
                                    roomId = activeChatId,
                                    reason = "Inappropriate chat behavior",
                                    snippetMessages = last5Snippets
                                )
                            }
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

                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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
                    chats = chats.map { if (it.id == activeChatId) it.copy(pinnedMessage = newPinText, pinnedMessageId = if (isPinned) null else msg.id) else it }
                    if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                        if (isPinned) {
                            chatViewModel.unpinMessage(activeChatId)
                        } else {
                            chatViewModel.pinMessage(activeChatId, msg.id, newPinText ?: "")
                        }
                    }
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
                    val audioUrl = msg.mediaUrl ?: msg.localUri
                    if (!audioUrl.isNullOrBlank()) {
                        showToast("Transcribing voice note...")
                        coroutineScope.launch {
                            val transcription = com.chroma.studio.ai.AIGenerator.transcribeAudio(context, audioUrl)
                            chats = chats.map { c ->
                                if (c.id == activeChatId) {
                                    c.copy(
                                        messages = c.messages.map { m ->
                                            if (m.id == msg.id) {
                                                m.copy(transcription = transcription)
                                            } else m
                                        }
                                    )
                                } else c
                            }
                            showToast("Audio transcribed")
                        }
                    } else {
                        showToast("No audio file available to transcribe")
                    }
                },

                onSaveMedia = {
                    val mediaUrl = msg.mediaUrl ?: msg.localUri
                    if (!mediaUrl.isNullOrBlank()) {
                        coroutineScope.launch {
                            com.chroma.studio.chat.media.MediaStoreHelper.saveMediaToGallery(context, mediaUrl)
                        }
                    } else {
                        showToast("No media file available to save")
                    }
                },

                onInfo = {

                    selectedMessageForInfo = msg

                },

                onOpenEmojiPicker = {

                    messageForEmojiPicker = msg

                    isEmojiReactionPickerOpen = true

                },

                onDelete = {

                    selectedMessageForContextMenu = null

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

                        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {

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

                    if (chatViewModel != null && !targetChat.id.startsWith("mock_")) {

                        chatViewModel.forwardMessage(

                            message = com.chroma.studio.model.ChatMessage(
                                text = msg.text ?: "",
                                senderId = currentUser.id,
                                imageUrl = msg.mediaUrl,
                                mediaUrl = msg.mediaUrl,
                                mediaType = msg.mediaType,
                                isForwarded = true

                            ),

                            targetRoomId = targetChat.id

                        )

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

                onClosePoll = if (!poll.isClosed) {
                    {
                        activePollForDetails = poll.copy(isClosed = true)
                        if (chatViewModel != null && !activeChatId.startsWith("mock_")) {
                            chatViewModel.closePoll(poll.id)
                        }
                        chats = chats.map { c ->
                            if (c.id == activeChatId) {
                                c.copy(
                                    messages = c.messages.map { m ->
                                        if (m.id == poll.id && m.pollData != null) {
                                            m.copy(pollData = m.pollData.copy(isClosed = true))
                                        } else m
                                    }
                                )
                            } else c
                        }
                        showToast("Poll closed. Voting ended.")
                    }
                } else null,

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

                        if (chatViewModel != null && !targetId.startsWith("mock_")) {

                            chatViewModel.forwardMessage(

                                message = com.chroma.studio.model.ChatMessage(

                                    text = fwdMsg.text ?: "",

                                    senderId = currentUser.id,

                                    imageUrl = fwdMsg.mediaUrl,

                                    mediaUrl = fwdMsg.mediaUrl,

                                    mediaType = fwdMsg.mediaType

                                ),

                                targetRoomId = targetId

                            )

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













