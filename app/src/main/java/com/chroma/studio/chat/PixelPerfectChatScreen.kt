package com.chroma.studio.pixelperfectchat

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.composables.icons.lucide.*
import com.chroma.studio.chat.ui.components.VoiceRecorderCard

/**
 * ============================================================================
 * 1. PIXEL-PERFECT THEME TOKENS (Matches CSS :root and html.dark variables)
 * ============================================================================
 */
@Immutable
data class ChatThemeColors(
    val isDark: Boolean,
    val iosBlue: Color = Color(0xFF0078FE),
    val iosBlueHover: Color = Color(0xFF0066D6),
    val chatBg: Color,
    val appBg: Color,
    val headerBg: Color,
    val bubbleGray: Color,
    val textDark: Color,
    val textMuted: Color,
    val inputBg: Color,
    val borderSubtle: Color,
    val onlineGreen: Color = Color(0xFF22C55E)
)

val LightChatColors = ChatThemeColors(
    isDark = false,
    chatBg = Color(0xFFE5E5E5),
    appBg = Color(0xFFFFFFFF),
    headerBg = Color(0xDBFFFFFF), // rgba(255, 255, 255, 0.86)
    bubbleGray = Color(0xFFF2F4F7),
    textDark = Color(0xFF111827),
    textMuted = Color(0xFF64748B),
    inputBg = Color(0xFFF4F6F8),
    borderSubtle = Color(0x0F000000) // rgba(0, 0, 0, 0.06)
)

val DarkChatColors = ChatThemeColors(
    isDark = true,
    chatBg = Color(0xFF030712),
    appBg = Color(0xFF0F172A),
    headerBg = Color(0xDB0F172A), // rgba(15, 23, 42, 0.86)
    bubbleGray = Color(0xFF1E293B),
    textDark = Color(0xFFF8FAFC),
    textMuted = Color(0xFF94A3B8),
    inputBg = Color(0xFF1E293B),
    borderSubtle = Color(0x14FFFFFF) // rgba(255, 255, 255, 0.08)
)

val LocalChatColors = staticCompositionLocalOf { LightChatColors }

/**
 * ============================================================================
 * 2. DATA MODELS FOR MESSAGES, MEDIA ATTACHMENTS & REACTIONS
 * ============================================================================
 */
enum class MessageDirection { INCOMING, OUTGOING }

sealed class MessageAttachment {
    data class ImageAttachment(val urlOrLabel: String, val previewColor: Color = Color(0xFF3B82F6)) : MessageAttachment()
    data class DocumentAttachment(val fileName: String, val sizeKb: Int) : MessageAttachment()
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val direction: MessageDirection,
    val timestamp: String,
    val isRead: Boolean = true,
    val quotedText: String? = null,
    val attachment: MessageAttachment? = null,
    val reactions: List<String> = emptyList()
)

/**
 * ============================================================================
 * 3. MAIN CHAT SCREEN COMPOSABLE
 * ============================================================================
 */
@Composable
fun PixelPerfectChatScreen(
    useWebViewForVoiceOrb: Boolean = false // Set true to render VoiceOrbComponent.html inside a WebView
) {
    var isDarkMode by remember { mutableStateOf(true) }
    val colors = if (isDarkMode) DarkChatColors else LightChatColors

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Conversation State (Exact 6 initial messages from ChatScreen.html)
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "Hey! How are you doing?",
                    direction = MessageDirection.INCOMING,
                    timestamp = "10:24 AM"
                ),
                ChatMessage(
                    text = "I'm good! Just working on some new ideas. How about you?",
                    direction = MessageDirection.OUTGOING,
                    timestamp = "10:26 AM"
                ),
                ChatMessage(
                    text = "That's great! I'm also working on the home automation project with Jasica.",
                    direction = MessageDirection.INCOMING,
                    timestamp = "10:28 AM"
                ),
                ChatMessage(
                    text = "Nice! Can you share the latest update?",
                    direction = MessageDirection.OUTGOING,
                    timestamp = "10:30 AM"
                ),
                ChatMessage(
                    text = "Sure! I'll send you the details in a moment.",
                    direction = MessageDirection.INCOMING,
                    timestamp = "10:32 AM"
                ),
                ChatMessage(
                    text = "Perfect. Thanks!",
                    direction = MessageDirection.OUTGOING,
                    timestamp = "10:33 AM"
                )
            )
        )
    }

    // Interactive UI States
    var inputText by remember { mutableStateOf("") }
    var activeReplyQuote by remember { mutableStateOf<String?>(null) }
    var isFriendTyping by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showSearchDrawer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showEmojiPopover by remember { mutableStateOf(false) }
    var showContactDrawer by remember { mutableStateOf(false) }
    var activeMessageActionId by remember { mutableStateOf<String?>(null) }
    var lightboxTitle by remember { mutableStateOf<String?>(null) }

    // Voice Recording Orb State
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf("Listening...") }

    // Active Audio/Video Call State
    var activeCallType by remember { mutableStateOf<String?>(null) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var isMicMuted by remember { mutableStateOf(false) }

    // Floating Toast Pill State
    var toastMessage by remember { mutableStateOf<String?>(null) }

    fun triggerToast(msg: String) {
        toastMessage = msg
    }

    // Auto-dismiss Toast after 2.2 seconds
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    // Live Call Timer Loop
    LaunchedEffect(activeCallType) {
        if (activeCallType != null) {
            callSeconds = 0
            while (activeCallType != null) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Voice Recording Simulation (Listening -> Transcribing -> Populates Input)
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceStatusText = "Listening..."
            delay(2800)
            if (isRecordingVoice) {
                voiceStatusText = "Transcribing..."
                delay(1100)
                if (isRecordingVoice) {
                    isRecordingVoice = false
                    inputText = "Here is the latest update on the project."
                }
            }
        } else {
            voiceStatusText = "Canceled"
        }
    }

    // Friend Reply Simulation Pool
    val friendReplies = remember {
        listOf(
            "That sounds amazing! Tell me more.",
            "I completely agree.",
            "Interesting perspective.",
            "Let me look into that for a second...",
            "Wow, I didn't know that!",
            "Got it. I'll add that to the project notes.",
            "Haha, that's exactly what I was thinking.",
            "Could you clarify what you mean by that?"
        )
    }

    fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }

    fun simulateFriendReply() {
        if (isBlocked) return
        coroutineScope.launch {
            delay(500)
            isFriendTyping = true
            listState.animateScrollToItem(messages.size + 1)
            delay((1400L..2600L).random())
            isFriendTyping = false
            messages = messages + ChatMessage(
                text = friendReplies.random(),
                direction = MessageDirection.INCOMING,
                timestamp = getCurrentTimeFormatted()
            )
            delay(80)
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    fun sendMessage(
        text: String = inputText.trim(),
        attachment: MessageAttachment? = null
    ) {
        if (text.isEmpty() && attachment == null) return
        val newMsg = ChatMessage(
            text = text,
            direction = MessageDirection.OUTGOING,
            timestamp = getCurrentTimeFormatted(),
            isRead = false,
            quotedText = activeReplyQuote,
            attachment = attachment
        )
        messages = messages + newMsg
        inputText = ""
        activeReplyQuote = null
        showEmojiPopover = false

        // Transition single check (Sent) -> double check (Read) after 650ms
        coroutineScope.launch {
            delay(80)
            listState.animateScrollToItem(messages.size + 1)
            delay(650)
            messages = messages.map {
                if (it.id == newMsg.id) it.copy(isRead = true) else it
            }
        }
        simulateFriendReply()
    }

    fun addOrEnsureReaction(messageId: String, emoji: String) {
        messages = messages.map { msg ->
            if (msg.id != messageId) msg
            else {
                val updated = if (msg.reactions.contains(emoji)) msg.reactions else msg.reactions + emoji
                msg.copy(reactions = updated)
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        messages = messages.map { msg ->
            if (msg.id != messageId) msg
            else {
                val updated = if (msg.reactions.contains(emoji)) {
                    msg.reactions - emoji
                } else {
                    msg.reactions + emoji
                }
                msg.copy(reactions = updated)
            }
        }
    }

    // Derived state for Floating "Scroll to Bottom" Button
    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.isNotEmpty() && lastVisible < layoutInfo.totalItemsCount - 2
        }
    }

    CompositionLocalProvider(LocalChatColors provides colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBg)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        showDropdownMenu = false
                        showEmojiPopover = false
                        activeMessageActionId = null
                        focusManager.clearFocus()
                    })
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP STICKY GLASS HEADER
                GlassHeader(
                    userStatus = when {
                        isBlocked -> "Blocked"
                        activeCallType != null -> "In ${activeCallType!!.lowercase()}..."
                        isFriendTyping -> "typing..."
                        else -> "Online"
                    },
                    isTyping = isFriendTyping,
                    isBlocked = isBlocked,
                    isMuted = isMuted,
                    showDropdownMenu = showDropdownMenu,
                    onToggleDropdown = { showDropdownMenu = !showDropdownMenu },
                    onDismissDropdown = { showDropdownMenu = false },
                    onOpenContactDrawer = {
                        showDropdownMenu = false
                        showContactDrawer = true
                    },
                    onToggleAppearance = {
                        isDarkMode = !isDarkMode
                        triggerToast("${if (isDarkMode) "Dark" else "Light"} appearance enabled")
                    },
                    onToggleMute = {
                        isMuted = !isMuted
                        triggerToast(if (isMuted) "Notifications muted" else "Notifications unmuted")
                    },
                    onClearChat = {
                        messages = emptyList()
                        showDropdownMenu = false
                        triggerToast("Conversation cleared")
                    },
                    onToggleBlock = {
                        isBlocked = !isBlocked
                        showDropdownMenu = false
                        triggerToast(if (isBlocked) "Contact blocked" else "Contact unblocked")
                    },
                    showSearchDrawer = showSearchDrawer,
                    searchQuery = searchQuery,
                    searchMatchCount = if (searchQuery.isBlank()) 0 else messages.count {
                        it.text.contains(searchQuery, ignoreCase = true)
                    },
                    onToggleSearch = {
                        showSearchDrawer = !showSearchDrawer
                        if (!showSearchDrawer) searchQuery = ""
                    },
                    onSearchQueryChange = { searchQuery = it },
                    activeCallType = activeCallType,
                    callSeconds = callSeconds,
                    isMicMuted = isMicMuted,
                    onStartCall = { type ->
                        if (isBlocked) {
                            triggerToast("Unblock contact to start a call")
                        } else {
                            activeCallType = type
                        }
                    },
                    onToggleMicMute = {
                        isMicMuted = !isMicMuted
                        triggerToast(if (isMicMuted) "Microphone muted" else "Microphone active")
                    },
                    onEndCall = {
                        activeCallType = null
                        triggerToast("Call ended")
                    }
                )

                // 2. MAIN MESSAGE STREAM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date Separator Pill
                        item(key = "date_pill") {
                            DateSeparatorPill()
                        }

                        // Empty State
                        if (messages.isEmpty()) {
                            item(key = "empty_state") {
                                EmptyChatPlaceholder()
                            }
                        }

                        // Message Rows
                        itemsIndexed(messages, key = { _, msg -> msg.id }) { _, message ->
                            val matchesSearch = searchQuery.isBlank() ||
                                message.text.contains(searchQuery, ignoreCase = true)

                            MessageRowItem(
                                message = message,
                                dimmed = !matchesSearch,
                                showActions = activeMessageActionId == message.id,
                                onToggleActions = {
                                    activeMessageActionId =
                                        if (activeMessageActionId == message.id) null else message.id
                                },
                                onDoubleTapHeart = { addOrEnsureReaction(message.id, "❤️") },
                                onReact = { emoji ->
                                    toggleReaction(message.id, emoji)
                                    activeMessageActionId = null
                                },
                                onReply = {
                                    activeReplyQuote = message.text.ifEmpty { "Media Attachment" }
                                    activeMessageActionId = null
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    activeMessageActionId = null
                                    triggerToast("Message copied to clipboard")
                                },
                                onDelete = {
                                    messages = messages.filterNot { it.id == message.id }
                                    activeMessageActionId = null
                                    triggerToast("Message deleted")
                                },
                                onOpenImageLightbox = { label ->
                                    lightboxTitle = label
                                }
                            )
                        }

                        // Incoming Typing Indicator
                        if (isFriendTyping) {
                            item(key = "typing_indicator") {
                                IncomingTypingIndicatorRow()
                            }
                        }
                    }

                    // Floating Scroll-to-Bottom Button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 16.dp)
                    ) {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size + 1)
                                }
                            },
                            shape = CircleShape,
                            color = colors.appBg.copy(alpha = 0.95f),
                            shadowElevation = 6.dp,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Lucide.ChevronDown,
                                    contentDescription = "Scroll to bottom",
                                    tint = colors.iosBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 3. FOOTER COMPOSER & EXPANDING VOICE RECORDING ORB
                FooterComposer(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    isBlocked = isBlocked,
                    activeReplyQuote = activeReplyQuote,
                    onCancelReply = { activeReplyQuote = null },
                    showEmojiPopover = showEmojiPopover,
                    onToggleEmojiPopover = { showEmojiPopover = !showEmojiPopover },
                    onSelectEmoji = { emoji ->
                        inputText += emoji
                        showEmojiPopover = false
                    },
                    isRecordingVoice = isRecordingVoice,
                    voiceStatusText = voiceStatusText,
                    onToggleVoiceRecording = { isRecordingVoice = !isRecordingVoice },
                    onSendClick = { sendMessage() },
                    onSendPhotoAttachment = {
                        sendMessage(
                            text = "Home_Automation_Hub_v2.jpg",
                            attachment = MessageAttachment.ImageAttachment("Home_Automation_Hub_v2.jpg")
                        )
                    },
                    onSendDocumentAttachment = {
                        sendMessage(
                            text = "",
                            attachment = MessageAttachment.DocumentAttachment("Jasica_System_Specs.pdf", 428)
                        )
                    },
                    useWebViewForVoiceOrb = useWebViewForVoiceOrb
                )
            }

            // 4. FLOATING TOAST NOTIFICATION PILL
            androidx.compose.animation.AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 68.dp)
                    .zIndex(120f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xE6111827),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.BadgeCheck,
                            contentDescription = null,
                            tint = colors.onlineGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = toastMessage.orEmpty(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5. SLIDE-OVER CONTACT INFO DRAWER
            androidx.compose.animation.AnimatedVisibility(
                visible = showContactDrawer,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                ContactInfoDrawer(
                    onClose = { showContactDrawer = false },
                    onStartAudioCall = {
                        showContactDrawer = false
                        activeCallType = "Audio Call"
                    },
                    onStartVideoCall = {
                        showContactDrawer = false
                        activeCallType = "Video Call"
                    },
                    onOpenSearch = {
                        showContactDrawer = false
                        showSearchDrawer = true
                    },
                    onOpenMediaPreview = { title ->
                        lightboxTitle = title
                    }
                )
            }

            // 6. FULL-SCREEN LIGHTBOX MODAL
            if (lightboxTitle != null) {
                LightboxModal(
                    title = lightboxTitle!!,
                    onDismiss = { lightboxTitle = null }
                )
            }
        }
    }
}

/**
 * ============================================================================
 * 4. TOP NAVIGATION HEADER, SEARCH DRAWER & ACTIVE CALL BANNER
 * ============================================================================
 */
@Composable
private fun GlassHeader(
    userStatus: String,
    isTyping: Boolean,
    isBlocked: Boolean,
    isMuted: Boolean,
    showDropdownMenu: Boolean,
    onToggleDropdown: () -> Unit,
    onDismissDropdown: () -> Unit,
    onOpenContactDrawer: () -> Unit,
    onToggleAppearance: () -> Unit,
    onToggleMute: () -> Unit,
    onClearChat: () -> Unit,
    onToggleBlock: () -> Unit,
    showSearchDrawer: Boolean,
    searchQuery: String,
    searchMatchCount: Int,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    activeCallType: String?,
    callSeconds: Int,
    isMicMuted: Boolean,
    onStartCall: (String) -> Unit,
    onToggleMicMute: () -> Unit,
    onEndCall: () -> Unit
) {
    val colors = LocalChatColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.headerBg)
            .drawBehind {
                drawLine(
                    color = colors.borderSubtle,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .zIndex(50f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back Button + Profile Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = colors.textDark,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenContactDrawer() }
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    // Avatar + Pulsing Online Dot
                    Box(modifier = Modifier.size(42.dp)) {
                        AvatarCircle(size = 42.dp)
                        if (!isBlocked) {
                            val infiniteTransition = rememberInfiniteTransition(label = "online_pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1250, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .align(Alignment.BottomEnd)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(colors.onlineGreen)
                                    .border(2.dp, colors.appBg, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(verticalArrangement = Arrangement.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Bholanath Kumbhakar",
                                color = colors.textDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isMuted) {
                                Icon(
                                    imageVector = Lucide.BellOff,
                                    contentDescription = "Muted",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = userStatus,
                            color = if (isTyping) colors.iosBlue else colors.textMuted,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right: Search, Audio Call, Video Call & 3-Dot Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onToggleSearch, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = "Search",
                        tint = colors.textDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { onStartCall("Audio Call") }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Lucide.Phone,
                        contentDescription = "Voice Call",
                        tint = colors.textDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { onStartCall("Video Call") }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Lucide.Video,
                        contentDescription = "Video Call",
                        tint = colors.textDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    IconButton(onClick = onToggleDropdown, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Lucide.Ellipsis,
                            contentDescription = "More Options",
                            tint = colors.textDark,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = onDismissDropdown,
                        modifier = Modifier
                            .width(210.dp)
                                                        .clip(RoundedCornerShape(24.dp))
                            .background(colors.appBg)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Contact Info", color = colors.textDark, fontSize = 14.sp) },
                            leadingIcon = { Icon(Lucide.Info, null, tint = colors.textMuted) },
                            onClick = onOpenContactDrawer
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Appearance", color = colors.textDark, fontSize = 14.sp)
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.inputBg
                                    ) {
                                        Text(
                                            text = if (colors.isDark) "Dark" else "Light",
                                            color = colors.textMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            leadingIcon = { Icon(Lucide.Palette, null, tint = colors.textMuted) },
                            onClick = onToggleAppearance
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Mute", color = colors.textDark, fontSize = 14.sp)
                                    Text(
                                        text = if (isMuted) "Muted" else "Off",
                                        color = colors.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = { Icon(Lucide.BellOff, null, tint = colors.textMuted) },
                            onClick = onToggleMute
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = colors.textDark, fontSize = 14.sp) },
                            leadingIcon = { Icon(Lucide.Trash2, null, tint = colors.textMuted) },
                            onClick = onClearChat
                        )
                        HorizontalDivider(color = colors.borderSubtle)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBlocked) "Unblock" else "Block",
                                    color = Color(0xFFDC2626),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = { Icon(Lucide.Ban, null, tint = Color(0xFFEF4444)) },
                            onClick = onToggleBlock
                        )
                    }
                }
            }
        }

        // Expandable In-Chat Search Drawer
        AnimatedVisibility(visible = showSearchDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.inputBg,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(color = colors.textDark, fontSize = 14.sp),
                            cursorBrush = SolidColor(colors.iosBlue),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search in conversation...",
                                        color = colors.textMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "$searchMatchCount match${if (searchMatchCount == 1) "" else "es"}",
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close Search",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onToggleSearch() }
                        )
                    }
                }
            }
        }

        // Active Audio/Video Call Banner
        AnimatedVisibility(visible = activeCallType != null) {
            val mins = (callSeconds / 60).toString().padStart(2, '0')
            val secs = (callSeconds % 60).toString().padStart(2, '0')
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.iosBlue)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "${activeCallType.orEmpty()} • Bholanath Kumbhakar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "$mins:$secs",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onToggleMicMute,
                        shape = CircleShape,
                        color = if (isMicMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMicMuted) Lucide.MicOff else Lucide.Mic,
                                contentDescription = "Mute Mic",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onEndCall,
                        shape = CircleShape,
                        color = Color(0xFFEF4444)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.PhoneMissed,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "End",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ============================================================================
 * 5. MESSAGE BUBBLES & CURVED iOS MASK TAIL (Matches .bubble-left & .bubble-right)
 * ============================================================================
 */
@Composable
private fun MessageRowItem(
    message: ChatMessage,
    dimmed: Boolean,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    onDoubleTapHeart: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onOpenImageLightbox: (String) -> Unit
) {
    val colors = LocalChatColors.current
    val isOutgoing = message.direction == MessageDirection.OUTGOING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.22f else 1f),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Outgoing Floating Action Bar (left of outgoing bubble)
        if (isOutgoing && showActions) {
            MessageActionPillBar(
                isOutgoing = true,
                onReact = onReact,
                onReply = onReply,
                onCopy = onCopy,
                onDelete = onDelete
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Incoming Avatar
        if (!isOutgoing) {
            AvatarCircle(size = 28.dp, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(min = 105.dp, max = 290.dp)
        ) {
            // Message Bubble with Custom Curved iOS Tail for Outgoing Messages
            Box(
                modifier = Modifier
                    .then(
                        if (isOutgoing) {
                            Modifier.drawBehind {
                                // Replicates .bubble-right::before & .bubble-right::after curved tail
                                val tailWidth = 8.dp.toPx()
                                val tailHeight = 20.dp.toPx()
                                val path = Path().apply {
                                    moveTo(size.width - 6.dp.toPx(), size.height)
                                    quadraticBezierTo(
                                        size.width + 2.dp.toPx(),
                                        size.height,
                                        size.width + tailWidth,
                                        size.height
                                    )
                                    quadraticBezierTo(
                                        size.width,
                                        size.height - 4.dp.toPx(),
                                        size.width,
                                        size.height - tailHeight
                                    )
                                    close()
                                }
                                drawPath(path = path, color = colors.iosBlue)
                            }
                        } else Modifier
                    )
                    .clip(
                        if (isOutgoing) {
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 4.dp,
                                bottomStart = 20.dp
                            )
                        } else {
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 20.dp,
                                bottomStart = 6.dp
                            )
                        }
                    )
                    .background(if (isOutgoing) colors.iosBlue else colors.bubbleGray)
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onTap = { onToggleActions() },
                            onDoubleTap = { onDoubleTapHeart() },
                            onLongPress = { onToggleActions() }
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column {
                    // Optional Quoted Reply Header
                    if (!message.quotedText.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .drawBehind {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.85f),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = message.quotedText,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Optional Rich Media Attachment (Image or Document)
                    when (val att = message.attachment) {
                        is MessageAttachment.ImageAttachment -> {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF93C5FD))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable { onOpenImageLightbox(att.urlOrLabel) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Lucide.Image,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to view full screen",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        is MessageAttachment.DocumentAttachment -> {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.FileText,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = att.fileName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${att.sizeKb} KB • Document",
                                        color = Color(0xFFDBEAFE),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        null -> Unit
                    }

                    // Message Text
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = if (isOutgoing) Color.White else colors.textDark,
                            fontSize = 15.sp,
                            lineHeight = 20.5.sp
                        )
                    }

                    // Timestamp & Read Receipt
                    Row(
                        modifier = Modifier
                            .align(if (isOutgoing) Alignment.End else Alignment.Start)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.timestamp,
                            color = if (isOutgoing) Color(0xFFDBEAFE) else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isOutgoing) {
                            Icon(
                                imageVector = if (message.isRead) Lucide.ChevronsRight else Lucide.Check,
                                contentDescription = if (message.isRead) "Read" else "Sent",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Reaction Pills Box
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.forEach { emoji ->
                        Surface(
                            onClick = { onReact(emoji) },
                            shape = CircleShape,
                            color = colors.appBg,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Incoming Floating Action Bar (right of incoming bubble)
        if (!isOutgoing && showActions) {
            Spacer(modifier = Modifier.width(8.dp))
            MessageActionPillBar(
                isOutgoing = false,
                onReact = onReact,
                onReply = onReply,
                onCopy = onCopy,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun MessageActionPillBar(
    isOutgoing: Boolean,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalChatColors.current
    Surface(
        shape = CircleShape,
        color = colors.appBg,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 3.dp,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "❤️",
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onReact("❤️") }
                    .padding(4.dp)
            )
            Text(
                text = "👍",
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onReact("👍") }
                    .padding(4.dp)
            )
            Icon(
                imageVector = Lucide.Reply,
                contentDescription = "Reply",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onReply() }
                    .padding(4.dp)
            )
            Icon(
                imageVector = Lucide.Copy,
                contentDescription = "Copy",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onCopy() }
                    .padding(4.dp)
            )
            if (isOutgoing) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() }
                        .padding(4.dp)
                )
            }
        }
    }
}

/**
 * ============================================================================
 * 6. DYNAMIC FOOTER COMPOSER & EXPANDING VOICE RECORDING ORB
 *    Replicates the exact CSS sibling (~) transitions and 300x300dp Voice Orb!
 * ============================================================================
 */
@Composable
private fun FooterComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isBlocked: Boolean,
    activeReplyQuote: String?,
    onCancelReply: () -> Unit,
    showEmojiPopover: Boolean,
    onToggleEmojiPopover: () -> Unit,
    onSelectEmoji: (String) -> Unit,
    isRecordingVoice: Boolean,
    voiceStatusText: String,
    onToggleVoiceRecording: () -> Unit,
    onSendClick: () -> Unit,
    onSendPhotoAttachment: () -> Unit,
    onSendDocumentAttachment: () -> Unit,
    useWebViewForVoiceOrb: Boolean
) {
    val colors = LocalChatColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isInputFocused by interactionSource.collectIsFocusedAsState()

    // Matches .input-text:focus, .input-text:not(:placeholder-shown)
    val isExpandedInput = isInputFocused || inputText.isNotEmpty()

    val leftMarginDp by animateDpAsState(
        targetValue = if (isExpandedInput) 44.dp else 114.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "input_left_margin"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.appBg)
            .drawBehind {
                drawLine(
                    color = colors.borderSubtle,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contextual Reply Quote Bar
            AnimatedVisibility(visible = activeReplyQuote != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.inputBg)
                        .drawBehind {
                            drawLine(
                                color = colors.iosBlue,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 4.dp.toPx()
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to message",
                            color = colors.iosBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = activeReplyQuote.orEmpty(),
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Cancel Reply",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Emoji Picker Popover
            AnimatedVisibility(visible = showEmojiPopover && !isRecordingVoice) {
                val emojis = listOf("😀", "😂", "🙌", "🔥", "❤️", "👍", "🎉", "🚀", "✨", "🤔", "👏", "💯")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.appBg,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        emojis.chunked(6).forEach { rowEmojis ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowEmojis.forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSelectEmoji(emoji) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main Composer Row (.container-ia-chat)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // 1. Left 3 Upload Icons (Camera, Photo, File) - Hidden when input expands or voice is recording
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isExpandedInput && !isRecordingVoice,
                    enter = fadeIn() + slideInHorizontally { -it / 2 },
                    exit = fadeOut() + slideOutHorizontally { -it / 2 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ComposerIconButton(
                            icon = Lucide.Camera,
                            contentDescription = "Send Photo",
                            onClick = onSendPhotoAttachment
                        )
                        ComposerIconButton(
                            icon = Lucide.Image,
                            contentDescription = "Choose Image",
                            onClick = onSendPhotoAttachment
                        )
                        ComposerIconButton(
                            icon = Lucide.Folder,
                            contentDescription = "Attach Document",
                            onClick = onSendDocumentAttachment
                        )
                    }
                }

                // 2. Collapsed Blue "+" Button (.label-files) - Visible when input expands
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpandedInput && !isRecordingVoice,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Surface(
                        onClick = onSendDocumentAttachment,
                        shape = CircleShape,
                        color = colors.iosBlue,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "Add Attachment",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 3. Expandable Pill Text Input (.input-text)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isRecordingVoice,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f),
                    exit = fadeOut() + scaleOut(targetScale = 0.95f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = leftMarginDp)
                            .clip(CircleShape)
                            .background(if (isExpandedInput) colors.appBg else colors.inputBg)
                            .border(
                                width = 1.dp,
                                color = if (isExpandedInput) colors.iosBlue.copy(alpha = 0.35f) else colors.borderSubtle,
                                shape = CircleShape
                            )
                            .padding(start = 18.dp, end = 78.dp, top = 11.dp, bottom = 11.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputTextChange,
                            enabled = !isBlocked,
                            singleLine = true,
                            interactionSource = interactionSource,
                            textStyle = TextStyle(
                                color = colors.textDark,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colors.iosBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = if (isBlocked) "You blocked this contact" else "Type a message...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // 4. Right Emoji Trigger & Voice / Send Button inside the Pill
                if (!isRecordingVoice) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Emoji Button (.btn-emoji)
                        IconButton(
                            onClick = onToggleEmojiPopover,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.SmilePlus,
                                contentDescription = "Insert Emoji",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // Swap between Voice Waveform Button and Blue Send Arrow Button
                        if (inputText.isNotBlank()) {
                            Surface(
                                onClick = onSendClick,
                                shape = CircleShape,
                                color = colors.iosBlue,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Lucide.ArrowUp,
                                        contentDescription = "Send Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = onToggleVoiceRecording,
                                modifier = Modifier.size(36.dp)
                            ) {
                                VoiceWaveformIcon(tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // 5. EXPANDING 300x300dp VOICE RECORDING ORB CARD (.input-voice:checked ~ .label-voice)
            AnimatedVisibility(
                visible = isRecordingVoice,
                enter = fadeIn(tween(300)) + scaleIn(
                    initialScale = 0.25f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.25f)
            ) {
                VoiceRecorderCard(
                    recordingDurationSec = 1,
                    statusText = voiceStatusText,
                    onCancel = onToggleVoiceRecording,
                    onTranscribe = {
                        onInputTextChange("Here is the latest update on the project.")
                        onToggleVoiceRecording()
                    },
                    onSend = {
                        onToggleVoiceRecording()
                        onSendClick()
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * ============================================================================
 * 7. NATIVE JETPACK COMPOSE LIQUID VOICE BUBBLE / GLOBE
 *    Pixel-for-pixel recreation of CSS `.ai`, `.c1..c4`, `.glass`, and `wave`!
 * ============================================================================
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun NativeLiquidVoiceOrb(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_voice_orb")

    // 1. Subtle breathing scale on the outer 280dp orb (.ai circle1 5s ease-in-out infinite)
    val orbBreathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathing"
    )

    // 2. Ripple Wave 1 & Wave 2 (.ai::before and .ai::after 1.5s linear infinite)
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_1"
    )
    val wave2Progress = (wave1Progress + 0.27f) % 1f

    // 3. Four Orbiting Liquid Gradient Blobs (c1: 5.5s, c2: 6s, c3: 4.8s, c4: 5.2s)
    val c1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing)),
        label = "c1_orbit"
    )
    val c2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "c2_orbit"
    )
    val c3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4800, easing = LinearEasing)),
        label = "c3_orbit"
    )
    val c4Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "c4_orbit"
    )

    // 4. Shimmering Gradient Text Animation (@keyframes text-light 4s linear infinite)
    val textShimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "text_shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Liquid Globe Canvas
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(orbBreathingScale),
            contentAlignment = Alignment.Center
        ) {
            // Outer Expanding Ripple Rings (.ai::before and .ai::after)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseRadius = size.minDimension * 0.25f // 50% of 280dp = 140dp diameter -> 70dp radius

                fun drawRippleWave(progress: Float) {
                    val scale = 1f + (progress * 0.6f)
                    val alpha = when {
                        progress < 0.35f -> (progress / 0.35f)
                        progress < 0.70f -> 1f - ((progress - 0.35f) / 0.35f)
                        else -> 0f
                    }
                    if (alpha > 0.01f) {
                        // Soft blue outer glow
                        drawCircle(
                            color = Color(0xFF0078FE).copy(alpha = alpha * 0.28f),
                            radius = baseRadius * scale,
                            style = Stroke(width = 10.dp.toPx())
                        )
                        // Crisp white ring core
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.85f),
                            radius = baseRadius * scale,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                drawRippleWave(wave1Progress)
                drawRippleWave(wave2Progress)
            }

            // Inner 140dp Clipped Liquid Globe (.ai .container + .c4, .c1, .c2, .c3)
            Canvas(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDBEAFE))
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Helper to interpolate 5-point keyframe arrays (0%, 25%, 50%, 75%, 100%)
                fun interpolate5(progress: Float, v0: Float, v25: Float, v50: Float, v75: Float, v100: Float): Float {
                    val p = (progress.coerceIn(0f, 1f)) * 4f
                    val idx = p.toInt().coerceAtMost(3)
                    val frac = p - idx
                    val arr = floatArrayOf(v0, v25, v50, v75, v100)
                    return arr[idx] + (arr[idx + 1] - arr[idx]) * frac
                }

                // Blob c4: #1e40af, rotates 270deg -> 630deg
                val c4AngleRad = ((270f + c4Progress * 360f) * (PI / 180.0)).toFloat()
                val c4Scale = interpolate5(c4Progress, 1.0f, 0.7f, 1.6f, 0.9f, 1.0f)
                val c4Alpha = interpolate5(c4Progress, 1.0f, 0.85f, 0.5f, 0.8f, 1.0f)
                val c4Radius = 110.dp.toPx() * c4Scale
                val c4Offset = Offset(
                    x = center.x + cos(c4AngleRad) * (size.width * 0.45f),
                    y = center.y + sin(c4AngleRad) * (size.height * 0.45f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E40AF).copy(alpha = c4Alpha),
                            Color(0xFF1E40AF).copy(alpha = 0f)
                        ),
                        center = c4Offset,
                        radius = c4Radius.coerceAtLeast(1f)
                    ),
                    radius = c4Radius,
                    center = c4Offset
                )

                // Blob c1: radial-gradient(#60a5fa, #0078FE), rotates 0deg -> 360deg
                val c1AngleRad = ((c1Progress * 360f) * (PI / 180.0)).toFloat()
                val c1Scale = interpolate5(c1Progress, 0.9f, 1.8f, 0.7f, 1.2f, 0.9f)
                val c1Alpha = interpolate5(c1Progress, 0.3f, 1.0f, 0.4f, 1.0f, 0.3f)
                val c1Radius = 95.dp.toPx() * c1Scale
                val c1Offset = Offset(
                    x = center.x + cos(c1AngleRad) * (size.width * 0.42f),
                    y = center.y + sin(c1AngleRad) * (size.height * 0.42f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF60A5FA).copy(alpha = c1Alpha),
                            Color(0xFF0078FE).copy(alpha = c1Alpha * 0.85f),
                            Color.Transparent
                        ),
                        center = c1Offset,
                        radius = c1Radius.coerceAtLeast(1f)
                    ),
                    radius = c1Radius,
                    center = c1Offset
                )

                // Blob c2: radial-gradient(#93c5fd, #e0f2fe), rotates 90deg -> 450deg
                val c2AngleRad = ((90f + c2Progress * 360f) * (PI / 180.0)).toFloat()
                val c2Scale = interpolate5(c2Progress, 0.5f, 1.7f, 1.0f, 0.8f, 0.5f)
                val c2Alpha = interpolate5(c2Progress, 0.9f, 0.3f, 0.4f, 0.4f, 0.9f)
                val c2Radius = 60.dp.toPx() * c2Scale
                val c2Offset = Offset(
                    x = center.x + cos(c2AngleRad) * (size.width * 0.36f),
                    y = center.y + sin(c2AngleRad) * (size.height * 0.36f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF93C5FD).copy(alpha = c2Alpha),
                            Color(0xFFE0F2FE).copy(alpha = c2Alpha * 0.7f),
                            Color.Transparent
                        ),
                        center = c2Offset,
                        radius = c2Radius.coerceAtLeast(1f)
                    ),
                    radius = c2Radius,
                    center = c2Offset
                )

                // Blob c3: radial-gradient(#3b82f6, transparent), rotates 180deg -> 540deg
                val c3AngleRad = ((180f + c3Progress * 360f) * (PI / 180.0)).toFloat()
                val c3Scale = interpolate5(c3Progress, 0.8f, 1.5f, 0.6f, 1.3f, 0.8f)
                val c3Alpha = interpolate5(c3Progress, 0.8f, 0.85f, 0.4f, 0.7f, 0.8f)
                val c3Radius = 80.dp.toPx() * c3Scale
                val c3Offset = Offset(
                    x = center.x + cos(c3AngleRad) * (size.width * 0.38f),
                    y = center.y + sin(c3AngleRad) * (size.height * 0.38f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = c3Alpha),
                            Color.Transparent
                        ),
                        center = c3Offset,
                        radius = c3Radius.coerceAtLeast(1f)
                    ),
                    radius = c3Radius,
                    center = c3Offset
                )
            }

            // Frosted Glass Specular Overlay (.ai .glass: radial-gradient(75px at 70% 30%, rgba(255,255,255,0.9), transparent))
            Canvas(
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
            ) {
                val highlightCenter = Offset(size.width * 0.70f, size.height * 0.30f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = highlightCenter,
                        radius = 75.dp.toPx()
                    ),
                    radius = size.minDimension / 2f
                )
                // Inner rim glow (inset 0 0 20px rgba(255, 255, 255, 0.8))
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = (size.minDimension / 2f) - 2.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Top & Bottom Overlay Labels (.text-voice)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Animated Shimmering "Listening..." Header
            Text(
                text = statusText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.textDark,
                            colors.iosBlue,
                            Color(0xFF80BFFF),
                            colors.iosBlue,
                            colors.textDark
                        ),
                        start = Offset(textShimmerOffset - 300f, 0f),
                        end = Offset(textShimmerOffset, 120f)
                    )
                )
            )

            // Bottom "Tap to cancel" Subtitle
            Text(
                text = "Tap to cancel",
                color = colors.textMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * ============================================================================
 * 8. OPTIONAL ANDROID WEBVIEW WRAPPER FOR THE VOICE BUBBLE / GLOBE
 *    Loads `VoiceOrbComponent.html` if you ever want to run the exact CSS DOM
 *    inside Jetpack Compose!
 * ============================================================================
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewVoiceOrbComponent(
    isDark: Boolean,
    statusText: String,
    onCancelClick: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onOrbCanceled() {
                            onCancelClick()
                        }
                    },
                    "AndroidVoiceOrb"
                )
                // Can load from file:///android_asset/VoiceOrbComponent.html
                loadUrl("file:///android_asset/VoiceOrbComponent.html")
            }
        },
        update = { webView ->
            webView.evaluateJavascript(
                "if(window.updateOrbState) window.updateOrbState(${isDark}, '$statusText');",
                null
            )
        }
    )
}

/**
 * ============================================================================
 * 9. SUPPORTING UI COMPONENTS (Avatar, Typing Dots, Contact Sheet & Lightbox)
 * ============================================================================
 */
@Composable
private fun AvatarCircle(size: Dp, modifier: Modifier = Modifier, avatarUrl: String = "") {
    com.chroma.studio.ui.components.IosAvatarView(
        name = "Chroma User",
        avatarUrl = avatarUrl,
        size = size,
        isGroup = false,
        modifier = modifier
    )
}

@Composable
private fun ComposerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun VoiceWaveformIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height
        // Matches SVG path: M12 4v16 m4-13v10 M8 7v10 m12-6v2 M4 11v2
        drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.84f), stroke, cap)
        drawLine(tint, Offset(w * 0.67f, h * 0.29f), Offset(w * 0.67f, h * 0.71f), stroke, cap)
        drawLine(tint, Offset(w * 0.33f, h * 0.29f), Offset(w * 0.33f, h * 0.71f), stroke, cap)
        drawLine(tint, Offset(w * 0.84f, h * 0.45f), Offset(w * 0.84f, h * 0.55f), stroke, cap)
        drawLine(tint, Offset(w * 0.16f, h * 0.45f), Offset(w * 0.16f, h * 0.55f), stroke, cap)
    }
}

@Composable
private fun DateSeparatorPill() {
    val colors = LocalChatColors.current
    val dateText = remember {
        SimpleDateFormat("EEE, dd MMM", Locale.US).format(Date())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = colors.appBg.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, colors.borderSubtle),
            shadowElevation = 1.dp
        ) {
            Text(
                text = dateText,
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyChatPlaceholder() {
    val colors = LocalChatColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.iosBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Circle,
                contentDescription = null,
                tint = colors.iosBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No messages yet",
            color = colors.textDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Send a message below to start chatting with Bholanath.",
            color = colors.textMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun IncomingTypingIndicatorRow() {
    val colors = LocalChatColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarCircle(size = 28.dp, modifier = Modifier.padding(bottom = 2.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            color = colors.bubbleGray
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(550, delayMillis = index * 160, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(Color(0xFF94A3B8))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactInfoDrawer(
    onClose: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMediaPreview: (String) -> Unit
) {
    val colors = LocalChatColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable { onClose() }
    ) {
        Surface(
            color = colors.appBg,
            shadowElevation = 24.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(320.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contact Info",
                            color = colors.textDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = colors.textMuted
                            )
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(96.dp)) {
                            AvatarCircle(size = 96.dp)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(colors.onlineGreen)
                                    .border(2.dp, colors.appBg, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bholanath Kumbhakar",
                            color = colors.textDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+91 98765 43210",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "\"Building hardware & home automation systems.\"",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DrawerQuickAction(
                            icon = Lucide.Phone,
                            label = "Audio",
                            onClick = onStartAudioCall,
                            modifier = Modifier.weight(1f)
                        )
                        DrawerQuickAction(
                            icon = Lucide.Video,
                            label = "Video",
                            onClick = onStartVideoCall,
                            modifier = Modifier.weight(1f)
                        )
                        DrawerQuickAction(
                            icon = Lucide.Search,
                            label = "Search",
                            onClick = onOpenSearch,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SHARED MEDIA",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to view",
                            color = colors.iosBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val sharedItems = listOf("Home Automation Prototype", "Circuit Board", "System Telemetry")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sharedItems.forEachIndexed { idx, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            when (idx) {
                                                0 -> listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))
                                                1 -> listOf(Color(0xFF0F766E), Color(0xFF2DD4BF))
                                                else -> listOf(Color(0xFF4338CA), Color(0xFF818CF8))
                                            }
                                        )
                                    )
                                    .clickable { onOpenMediaPreview(title) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Image,
                                    contentDescription = title,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = colors.borderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Direct Messages • End-to-End Encrypted",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.inputBg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.iosBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = colors.iosBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LightboxModal(
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.86f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Lucide.X, contentDescription = "Close", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF38BDF8))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Lucide.Image,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
