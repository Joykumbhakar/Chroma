package com.chroma.studio.chat.ui.components

import com.chroma.studio.ui.components.VerifiedBadge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.chroma.studio.ui.components.AppleChatGraphic
import com.chroma.studio.ui.components.AppleEmptyStateView
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.ui.theme.StoryGradient
import com.chroma.studio.ui.components.getAvatarPresetDrawableRes
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.chroma.studio.ui.components.StoriesSection
import com.chroma.studio.model.UserStoryGroup

/**
 * Flagship Apple iOS 18 Conversations & Stories Screen.
 * Implements native SF Pro typography scale, iOS Large Title navigation,
 * native UISegmentedControl, crisp message status ticks (sent, delivered, seen),
 * and pixel-perfect search bar with inset hairline dividers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(
    currentUser: User,
    stories: List<Story> = emptyList(),
    storyGroups: List<UserStoryGroup> = emptyList(),
    hasMyStory: Boolean = false,
    onMyStoryClick: (() -> Unit)? = null,
    onStoryGroupClick: ((Int) -> Unit)? = null,
    chats: List<ChatConversation>,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onChatSelected: (String) -> Unit,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    onNewMessageClick: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onUserProfileClick: () -> Unit = {},
    onStartCall: (ChatConversation, String) -> Unit = { _, _ -> },
    pinnedChatIds: Set<String> = emptySet(),
    lockedChatIds: Set<String> = emptySet(),
    onPinChatToggle: (ChatConversation) -> Unit = {},
    onMuteChatToggle: (ChatConversation) -> Unit = {},
    onReadChatToggle: (ChatConversation) -> Unit = {},
    onLockChatToggle: (ChatConversation) -> Unit = {},
    onArchiveChat: (ChatConversation) -> Unit = {},
    onClearChatHistory: (ChatConversation) -> Unit = {},
    onExportChat: (ChatConversation) -> Unit = {},
    onDeleteChat: (ChatConversation) -> Unit = {},
    onViewStoryAnalytics: (Story) -> Unit = {},
    onDeleteStory: (Story) -> Unit = {},
    onMuteStory: (Story) -> Unit = {},
    onShareStory: (Story) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current

    // Context Menu States
    var selectedChatForContextMenu by remember { mutableStateOf<ChatConversation?>(null) }
    var selectedStoryForContextMenu by remember { mutableStateOf<Story?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val selectedChatIds = remember { mutableStateListOf<String>() }

    val totalUnread = remember(chats) { chats.sumOf { it.unreadCount } }

    val filteredChats = remember(chats, activeTab, searchQuery, pinnedChatIds) {
        val filtered = chats.filter { c ->
            val matchTab = when (activeTab) {
                "unread" -> c.unreadCount > 0
                "direct" -> c.type == "direct"
                "group" -> c.type == "group"
                "calls" -> true // Handled specifically in calls view or filtered
                else -> true
            }
            val query = searchQuery.trim().lowercase()
            val matchSearch = query.isEmpty() ||
                c.name.lowercase().contains(query) ||
                c.messages.any { it.text?.lowercase()?.contains(query) == true }
            matchTab && matchSearch
        }
        filtered.sortedWith(compareByDescending<ChatConversation> { it.id in pinnedChatIds })
    }

    Scaffold(
        containerColor = colors.appBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.appBg)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            ) {
                // ── iOS Navigation Bar Row: Edit/Done (Left) & Actions/Count (Right) ────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Text(
                            text = "Done",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.iosBlue,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    isSelectionMode = false
                                    selectedChatIds.clear()
                                }
                                .padding(vertical = 4.dp)
                        )

                        Text(
                            text = "${selectedChatIds.size} Selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark
                        )

                        Text(
                            text = if (selectedChatIds.size == filteredChats.size) "Deselect" else "Select All",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.iosBlue,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    if (selectedChatIds.size == filteredChats.size) {
                                        selectedChatIds.clear()
                                    } else {
                                        selectedChatIds.clear()
                                        selectedChatIds.addAll(filteredChats.map { it.id })
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        // "Chats" Title moved UP to top left
                        Text(
                            text = "Chats",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark,
                            letterSpacing = (-0.5).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search Button beside colors palette icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        isSearchExpanded = !isSearchExpanded
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideSearch(tint = if (isSearchExpanded || searchQuery.isNotEmpty()) colors.iosBlue else colors.textDark, size = 20.dp)
                            }

                            // Appearance Theme Button (Palette)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onOpenAppearanceSettings()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucidePalette(tint = colors.iosBlue, size = 20.dp)
                            }

                            // Compose New Message Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onNewMessageClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideEdit(tint = colors.iosBlue, size = 20.dp)
                            }

                            // Current User Profile Avatar (Actual Avatar, Zero Letters)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onUserProfileClick()
                                    }
                            ) {
                                ChatAvatar(
                                    avatarUrl = currentUser.avatar,
                                    name = currentUser.name,
                                    size = 36.dp,
                                    showOnline = true,
                                    isOnline = true
                                )
                            }
                        }
                    }
                }

                // Expandable Search Bar (revealed when search button clicked or query is present)
                AnimatedVisibility(
                    visible = isSearchExpanded || searchQuery.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (colors.isLight) Color(0xFF767680).copy(alpha = 0.12f)
                                else Color(0xFF767680).copy(alpha = 0.24f)
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LucideSearch(tint = colors.textMuted, size = 16.dp)

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChanged,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = colors.textDark,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(colors.iosBlue),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search conversations...",
                                            fontSize = 15.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "✕",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onSearchQueryChanged("") }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.headerBg)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mark as Read / Unread
                    TextButton(onClick = {
                        SoundManager.playReaction()
                        selectedChatIds.forEach { id ->
                            chats.find { it.id == id }?.let { onReadChatToggle(it) }
                        }
                        isSelectionMode = false
                        selectedChatIds.clear()
                    }) {
                        Text("Read", color = colors.iosBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    // Mute / Unmute
                    TextButton(onClick = {
                        SoundManager.playReaction()
                        selectedChatIds.forEach { id ->
                            chats.find { it.id == id }?.let { onMuteChatToggle(it) }
                        }
                        isSelectionMode = false
                        selectedChatIds.clear()
                    }) {
                        Text("Mute", color = colors.iosBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    // Pin / Unpin
                    TextButton(onClick = {
                        SoundManager.playReaction()
                        selectedChatIds.forEach { id ->
                            chats.find { it.id == id }?.let { onPinChatToggle(it) }
                        }
                        isSelectionMode = false
                        selectedChatIds.clear()
                    }) {
                        Text("Pin", color = colors.iosBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    // Archive
                    TextButton(onClick = {
                        SoundManager.playReaction()
                        selectedChatIds.forEach { id ->
                            chats.find { it.id == id }?.let { onArchiveChat(it) }
                        }
                        isSelectionMode = false
                        selectedChatIds.clear()
                    }) {
                        Text("Archive", color = colors.iosBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    // Delete
                    TextButton(onClick = {
                        SoundManager.playReaction()
                        selectedChatIds.forEach { id ->
                            chats.find { it.id == id }?.let { onDeleteChat(it) }
                        }
                        isSelectionMode = false
                        selectedChatIds.clear()
                    }) {
                        Text("Delete", color = Color(0xFFFF3B30), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── 1. Stories Carousel (Unified Across All Tabs) ───────────
            item(key = "conversations_stories_carousel") {
                StoriesSection(
                    stories = storyGroups,
                    isLoading = false,
                    hasMyStory = hasMyStory,
                    myStoryAvatarUrl = currentUser.avatar,
                    onMyStoryClick = {
                        SoundManager.playReaction()
                        if (hasMyStory && onMyStoryClick != null) {
                            onMyStoryClick()
                        } else {
                            onAddStoryClick()
                        }
                    },
                    onAddStoryClick = {
                        SoundManager.playReaction()
                        onAddStoryClick()
                    },
                    onStoryClick = { groupIdx ->
                        SoundManager.playReaction()
                        onStoryGroupClick?.invoke(groupIdx)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // ── 3. Clean Underline Tab Navigation ──────────────────────
            item {
                UnderlineTabNavigation(
                    tabs = listOf(
                        "all" to "All",
                        "unread" to "Unread",
                        "direct" to "Direct",
                        "group" to "Groups",
                        "calls" to "Calls"
                    ),
                    selectedTab = activeTab,
                    unreadCount = remember(chats) { chats.count { it.unreadCount > 0 } },
                    onTabSelected = { tab ->
                        SoundManager.playReaction()
                        onTabSelected(tab)
                    }
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── 3.5 Calls List View (when Calls tab is selected) ────────
            if (activeTab == "calls") {
                item {
                    Text(
                        text = "RECENT CALLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                items(chats.filter { it.type == "direct" }, key = { "call_${it.id}" }) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onChatSelected(chat.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar
                            ChatAvatar(
                                avatarUrl = chat.avatar,
                                name = chat.name,
                                size = 48.dp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = chat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textDark
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LucidePhoneIncoming(tint = colors.success, size = 12.dp)
                                    Text(
                                        text = "Incoming • 12m 34s",
                                        fontSize = 13.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }

                        // Call Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onStartCall(chat, "Audio Call")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucidePhone(tint = colors.iosBlue, size = 18.dp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onStartCall(chat, "Video Call")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideVideo(tint = colors.iosBlue, size = 18.dp)
                            }
                        }
                    }

                    HorizontalDivider(
                        color = colors.separator,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 78.dp)
                    )
                }
            }

            // ── 3.6 Quick "New Group" Action (when Groups tab is selected) ──
            if (activeTab == "group") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onNewMessageClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LucideEdit(tint = colors.iosBlue, size = 24.dp)
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "New Group",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.iosBlue
                            )
                            Text(
                                text = "Create a group with multiple contacts",
                                fontSize = 13.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.separator,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 78.dp)
                    )
                }
            }

            // ── 4. Chat Conversation Rows (when not in Calls tab) ──────────
            if (activeTab != "calls") {
                if (filteredChats.isEmpty()) {
                    item {
                        val chatState = rememberAppleGraphicAnimationState()
                        AppleEmptyStateView(
                            graphic = {
                                AppleChatGraphic(
                                    modifier = Modifier.size(150.dp),
                                    state = chatState,
                                    enableClickToAnimate = true
                                )
                            },
                            title = "No Conversations Found",
                            subtitle = if (searchQuery.isNotEmpty()) "No results matching “$searchQuery”." else "Start a new conversation to get connected with artists.",
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
                        )
                    }
                } else {
                    items(filteredChats, key = { it.id }) { chat ->
                        IosConversationListRow(
                            chat = chat,
                            currentUserId = currentUser.id,
                            isPinned = chat.id in pinnedChatIds,
                            isLocked = chat.id in lockedChatIds,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedChatIds.contains(chat.id),
                            onToggleSelect = {
                                if (selectedChatIds.contains(chat.id)) {
                                    selectedChatIds.remove(chat.id)
                                } else {
                                    selectedChatIds.add(chat.id)
                                }
                            },
                            onClick = {
                                SoundManager.playReaction()
                                onChatSelected(chat.id)
                            },
                            onLongClick = {
                                selectedChatForContextMenu = chat
                            },
                            onPinToggle = { onPinChatToggle(chat) },
                            onMuteToggle = { onMuteChatToggle(chat) },
                            onDelete = { onDeleteChat(chat) }
                        )
                    }
                }
            }

            // ── 5. Bottom Scroll Clearance Spacer ─────────────────────────
            item {
                Spacer(Modifier.height(84.dp))
            }
        }

        // Long-Press Context Menu for Conversation List Item
        selectedChatForContextMenu?.let { chat ->
            IosConversationContextMenuModal(
                chat = chat,
                isPinned = chat.id in pinnedChatIds,
                onDismiss = { selectedChatForContextMenu = null },
                onSelect = {
                    isSelectionMode = true
                    selectedChatIds.add(chat.id)
                },
                onPinToggle = { onPinChatToggle(chat) },
                onMuteToggle = { onMuteChatToggle(chat) },
                onReadToggle = { onReadChatToggle(chat) },
                onLockToggle = { onLockChatToggle(chat) },
                onArchive = { onArchiveChat(chat) },
                onClearHistory = { onClearChatHistory(chat) },
                onExportChat = { onExportChat(chat) },
                onDeleteChat = { onDeleteChat(chat) }
            )
        }

        // Long-Press Context Menu for Story Item
        selectedStoryForContextMenu?.let { story ->
            IosStoryContextMenuModal(
                story = story,
                isMyStory = story.isMyStory,
                onDismiss = { selectedStoryForContextMenu = null },
                onViewAnalytics = { onViewStoryAnalytics(story) },
                onAddNewStory = { onAddStoryClick() },
                onMuteStories = { onMuteStory(story) },
                onShareStory = { onShareStory(story) },
                onDeleteStory = { onDeleteStory(story) }
            )
        }
    }
}

// ── Supporting Apple iOS Screen Components ──────────────────────────

/**
 * Strips raw markdown tags and slash command tags for clean preview display.
 */
private fun sanitizeMessagePreview(rawText: String): String {
    if (rawText.startsWith("[POLL]:")) {
        val q = rawText.removePrefix("[POLL]:").substringBefore("|||").trim()
        return "Poll: $q"
    }
    return rawText
        .replace(Regex("\\[([^]]+)\\]\\([^)]+\\)"), "$1") // [label](url) -> label
        .replace(Regex("[#*_~`]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Apple iOS Story Avatar with Gradient Ring or Add Button
 */
/**
 * Universal Apple iOS Avatar with crisp vector preset resolution,
 * safe fallback to default Apple avatar artwork, and zero letter monograms.
 */
@Composable
fun ChatAvatar(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    showOnline: Boolean = false,
    isOnline: Boolean = false
) {
    val colors = LocalChatColors.current
    val cleanUrl = avatarUrl?.trim().orEmpty()
    val presetRes = remember(cleanUrl) { getAvatarPresetDrawableRes(cleanUrl) }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary)
                .border(0.5.dp, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                presetRes != null -> {
                    Image(
                        painter = painterResource(id = presetRes),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                cleanUrl.startsWith("android.resource://") -> {
                    AsyncImage(
                        model = Uri.parse(cleanUrl),
                        contentDescription = name,
                        placeholder = painterResource(id = R.drawable.default_avatar_apple),
                        error = painterResource(id = R.drawable.default_avatar_apple),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                cleanUrl.isNotBlank() -> {
                    AsyncImage(
                        model = cleanUrl,
                        contentDescription = name,
                        placeholder = painterResource(id = R.drawable.default_avatar_apple),
                        error = painterResource(id = R.drawable.default_avatar_apple),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.default_avatar_apple),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        if (showOnline && isOnline) {
            val indicatorSize = (size.value * 0.25f).coerceIn(9f, 13f).dp
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .background(colors.appBg, CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(colors.success)
            )
        }
    }
}

/**
 * Apple iOS Story Avatar with Gradient Ring or Add Button
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosStoryAvatarItem(
    avatarUrl: String,
    name: String,
    hasNewStory: Boolean,
    isMyStory: Boolean,
    seen: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val colors = LocalChatColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(64.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    SoundManager.playReaction()
                    onLongClick()
                }
            )
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            val showRing = hasNewStory
            val ringBrush = when {
                !showRing -> SolidColor(Color.Transparent)
                !seen -> Brush.sweepGradient(
                    listOf(
                        Color(0xFF007AFF),
                        Color(0xFF5856D6),
                        Color(0xFFFF2D55),
                        Color(0xFFFF9500),
                        Color(0xFF007AFF)
                    )
                )
                else -> Brush.linearGradient(
                    if (!colors.isLight) listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E))
                    else listOf(Color(0xFFD1D1D6), Color(0xFFC7C7CC))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ringBrush)
                    .padding(if (showRing) 2.5.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colors.appBg)
                        .padding(if (showRing) 2.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colors.surfaceSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatAvatar(
                            avatarUrl = avatarUrl,
                            name = name,
                            modifier = Modifier.fillMaxSize(),
                            size = 56.dp
                        )
                    }
                }
            }

            // Plus badge for My Story
            if (isMyStory && !hasNewStory) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .background(colors.appBg, CircleShape)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(colors.iosBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Text(
            text = name,
            fontSize = 11.5.sp,
            fontWeight = if (!seen && hasNewStory) FontWeight.SemiBold else FontWeight.Normal,
            color = if (!seen && hasNewStory) colors.textDark else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Clean Apple Underline Active Section Tab Navigation
 */
@Composable
private fun UnderlineTabNavigation(
    tabs: List<Pair<String, String>>,
    selectedTab: String,
    unreadCount: Int,
    onTabSelected: (String) -> Unit
) {
    val colors = LocalChatColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEach { (key, label) ->
                val isSelected = selectedTab == key
                val badge = if (key == "unread" && unreadCount > 0) unreadCount else null

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(key)
                        }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) colors.iosBlue else colors.textMuted
                        )

                        if (badge != null && badge > 0) {
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .widthIn(min = 16.dp)
                                    .background(colors.iosBlue, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (badge > 99) "99+" else badge.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .height(2.5.dp)
                                .width(22.dp)
                                .clip(RoundedCornerShape(1.25.dp))
                                .background(colors.iosBlue)
                        )
                    } else {
                        Spacer(Modifier.height(2.5.dp))
                    }
                }
            }
        }

        HorizontalDivider(
            color = colors.separator.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}

/**
 * Pixel-Perfect Apple iOS Messages Chat List Item Row
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosConversationListRow(
    chat: ChatConversation,
    currentUserId: String,
    isPinned: Boolean = false,
    isLocked: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    onMuteToggle: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val colors = LocalChatColors.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragOffset = remember { Animatable(0f) }

    val pinActionWidthPx = with(density) { 80.dp.toPx() }
    val leftActionsWidthPx = with(density) { 150.dp.toPx() }

    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode && dragOffset.value != 0f) {
            dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
        }
    }

    val lastMsg: ChatMessage? = chat.messages.lastOrNull()
    val isOutgoing = lastMsg != null && lastMsg.senderId == currentUserId

    val lastMsgPreview = when {
        isLocked -> "Locked conversation"
        lastMsg?.mediaType == "poll" || lastMsg?.text?.startsWith("[POLL]:") == true -> {
            val q = lastMsg.pollData?.question ?: lastMsg.text?.removePrefix("[POLL]:")?.substringBefore("|||")?.trim() ?: "Poll"
            "Poll: $q"
        }
        lastMsg?.text?.isNotBlank() == true -> sanitizeMessagePreview(lastMsg.text)
        lastMsg?.mediaType == "voice" -> "Voice Message (${lastMsg.voiceDuration}s)"
        lastMsg?.mediaType == "image" -> "Photo"
        lastMsg?.mediaType == "location" -> "Location shared"
        else -> "No messages yet"
    }

    val isUnread = chat.unreadCount > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // ── Background Swipe Action Buttons ─────────────────────────
        if (dragOffset.value > 0f) {
            // Pin Action Button (Revealed on Swipe Right)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(Color(0xFF0A84FF))
                        .clickable {
                            SoundManager.playReaction()
                            coroutineScope.launch {
                                dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                            }
                            onPinToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LucidePin(tint = Color.White, size = 20.dp)
                        Text(
                            text = if (isPinned) "Unpin" else "Pin",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (dragOffset.value < 0f) {
            // Mute and Delete Action Buttons (Revealed on Swipe Left)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(75.dp)
                            .background(Color(0xFFFF9500))
                            .clickable {
                                SoundManager.playReaction()
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                                }
                                onMuteToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (chat.isMuted) {
                                LucideBell(tint = Color.White, size = 20.dp)
                            } else {
                                LucideBellOff(tint = Color.White, size = 20.dp)
                            }
                            Text(
                                text = if (chat.isMuted) "Unmute" else "Mute",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Delete
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(75.dp)
                            .background(Color(0xFFFF3B30))
                            .clickable {
                                SoundManager.playReaction()
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                                }
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucideTrash(tint = Color.White, size = 20.dp)
                            Text(
                                text = "Delete",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ── Foreground Conversation Content Row ──────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .background(colors.appBg)
                .then(
                    if (!isSelectionMode) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        val current = dragOffset.value
                                        val target = when {
                                            current > pinActionWidthPx / 2f -> pinActionWidthPx
                                            current < -leftActionsWidthPx / 2f -> -leftActionsWidthPx
                                            else -> 0f
                                        }
                                        dragOffset.animateTo(
                                            target,
                                            animationSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        val newOffset = (dragOffset.value + dragAmount).coerceIn(
                                            -(leftActionsWidthPx + 40f),
                                            pinActionWidthPx + 40f
                                        )
                                        dragOffset.snapTo(newOffset)
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
                .clickable(enabled = isSelectionMode) { onToggleSelect() }
                .then(
                    if (!isSelectionMode) {
                        Modifier.combinedClickable(
                            onClick = {
                                if (dragOffset.value != 0f) {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                } else {
                                    onClick()
                                }
                            },
                            onLongClick = {
                                if (dragOffset.value != 0f) {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                } else {
                                    SoundManager.playReaction()
                                    onLongClick()
                                }
                            }
                        )
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selection Checkbox (Thin Box & Extended Tick)
                if (isSelectionMode) {
                    LiquidGlassCheckbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        size = 22.dp,
                        checkedColor = colors.iosBlue,
                        uncheckedColor = colors.textMuted.copy(alpha = 0.5f)
                    )
                }

                // Contact Avatar with Online Indicator (Actual Avatar or Vector Preset, Zero Letters)
                ChatAvatar(
                    avatarUrl = chat.avatar,
                    name = chat.name,
                    size = 52.dp,
                    showOnline = true,
                    isOnline = chat.online
                )

                // Center Column: Message Content & Contact Name
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Contact Name, Verified Badge & Pin Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = chat.name,
                            fontSize = 16.5.sp,
                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Medium,
                            color = colors.textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (chat.isVerified) {
                            VerifiedBadge(size = 15.dp)
                        }
                        if (isLocked) {
                            LucideLock(tint = colors.textMuted, size = 12.dp)
                        }
                        if (isPinned) {
                            LucidePin(tint = colors.iosBlue, size = 12.dp)
                        }
                    }

                    // Snippet & Outgoing Status Checks / Media Icon or Live Typing/Recording Indicator
                    val isActivity = chat.statusText.contains("typing", ignoreCase = true) || chat.statusText.contains("recording", ignoreCase = true)
                    val isRecording = chat.statusText.contains("recording", ignoreCase = true)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isLocked) {
                            LucideLock(tint = colors.textMuted.copy(alpha = 0.8f), size = 13.dp)
                            Text(
                                text = "Locked conversation",
                                fontSize = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = colors.textMuted.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isActivity) {
                            if (isRecording) {
                                LucideMic(tint = colors.iosBlue, size = 14.dp)
                            } else {
                                LucideEdit(tint = colors.iosBlue, size = 13.dp)
                            }
                            Text(
                                text = chat.statusText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.iosBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp
                            )
                        } else {
                            // Status Checkmark for Outgoing Messages
                            if (isOutgoing) {
                                MessageStatusTick(
                                    status = lastMsg.status,
                                    isOutgoing = true,
                                    normalTint = colors.textMuted,
                                    readTint = colors.iosBlue,
                                    pendingTint = colors.textMuted,
                                    size = 13.dp
                                )
                                Spacer(Modifier.width(2.dp))
                            }

                            // Media Type Icon
                            when (lastMsg?.mediaType) {
                                "voice" -> PhosphorWaveform(tint = if (isUnread) colors.iosBlue else colors.textMuted, size = 14.dp)
                                "image" -> PhosphorPhotoIcon(tint = if (isUnread) colors.iosBlue else colors.textMuted, size = 14.dp)
                                "poll" -> PhosphorPollIcon(tint = if (isUnread) colors.iosBlue else colors.textMuted, size = 14.dp)
                            }

                            Text(
                                text = (if (chat.type == "group" && lastMsg != null && lastMsg.senderName.isNotEmpty() && !isOutgoing)
                                           "${lastMsg.senderName.split(" ").firstOrNull() ?: lastMsg.senderName}: " else "") + lastMsgPreview,
                                fontSize = 14.5.sp,
                                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                                color = if (isUnread) colors.textDark else if (lastMsgPreview == "No messages yet") colors.textMuted.copy(alpha = 0.75f) else colors.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // Right Meta Column: Timestamp & Number Badge / Mute Indicator
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    val timeDisplay = lastMsg?.time?.ifBlank { "New" } ?: "New"
                    Text(
                        text = timeDisplay,
                        fontSize = 12.5.sp,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isUnread) colors.iosBlue else colors.textMuted
                    )

                    // Unread Count Badge or Muted Bell
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(min = 20.dp)
                                .background(
                                    if (chat.isMuted) colors.textMuted else colors.iosBlue,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    } else if (chat.isMuted) {
                        LucideBellOff(tint = colors.textMuted, size = 14.dp)
                    }
                }
            }

            // Apple Hairline divider indented to align exactly with contact text (78.dp start)
            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 78.dp)
            )
        }
    }
}
