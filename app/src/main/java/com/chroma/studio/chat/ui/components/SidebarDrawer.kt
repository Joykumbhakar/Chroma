package com.chroma.studio.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.ui.theme.StoryGradient

@Composable
fun SidebarDrawer(
    currentUser: User,
    stories: List<Story>,
    chats: List<ChatConversation>,
    activeChatId: String,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onChatSelected: (String) -> Unit,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    onOpenStoryAnalytics: (Story) -> Unit = {},
    onNewMessageClick: () -> Unit,
    onCloseSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(colors.sidebarBg)
            .border(
                width = 0.5.dp,
                color = colors.separator
            )
    ) {

        // ── 1. Header: User Profile ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar with online badge
                Box(modifier = Modifier.size(38.dp)) {
                    AsyncImage(
                        model = currentUser.avatar,
                        contentDescription = "My profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.5.dp, colors.iosBlue.copy(alpha = 0.5f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.BottomEnd)
                            .background(colors.success, CircleShape)
                            .border(2.dp, colors.sidebarBg, CircleShape)
                    )
                }
                // Name + status
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = currentUser.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark,
                        lineHeight = 18.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        BlinkingDot(colors.success)
                        Text(
                            text = "Online",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.success,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Action icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNewMessageClick, modifier = Modifier.size(36.dp)) {
                    LucideEdit(tint = colors.iosBlue, size = 18.dp)
                }
                IconButton(onClick = onCloseSidebar, modifier = Modifier.size(36.dp)) {
                    PhosphorCloseX(tint = colors.textMuted, size = 16.dp)
                }
            }
        }

        // Hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )

        // ── 2. Stories Carousel ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSecondary.copy(alpha = 0.5f))
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "+ Add Story",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.iosBlue,
                    modifier = Modifier.clickable { onAddStoryClick() }
                )
            }

            Spacer(Modifier.height(10.dp))

            val myStory = stories.find { it.isMyStory }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // My Story
                item {
                    StoryItem(
                        avatarUrl = currentUser.avatar,
                        name = "Your Story",
                        hasNewStory = myStory != null,
                        isMyStory = true,
                        seen = myStory?.seen ?: false,
                        onClick = {
                            if (myStory != null) onStoryClick(myStory) else onAddStoryClick()
                        }
                    )
                }
                // Friends' stories
                items(stories.filter { !it.isMyStory }) { story ->
                    StoryItem(
                        avatarUrl = story.avatar,
                        name = story.authorName.split(" ").firstOrNull() ?: "",
                        hasNewStory = true,
                        isMyStory = false,
                        seen = story.seen,
                        onClick = { onStoryClick(story) }
                    )
                }
            }
        }

        // Hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )

        // ── 3. Search + Filter Tabs ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceSecondary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhosphorMagnifyingGlass(tint = colors.textMuted, size = 14.dp)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = colors.textDark,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(colors.iosBlue),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search chats & people",
                                fontSize = 14.sp,
                                color = colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSearchQueryChanged("") },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCloseX(tint = colors.textMuted, size = 13.dp)
                    }
                }
            }

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    "all" to "All",
                    "unread" to "Unread",
                    "direct" to "Direct",
                    "group" to "Groups"
                )
                tabs.forEach { (key, label) ->
                    val isSelected = activeTab == key
                    FilterChip(
                        label = label,
                        selected = isSelected,
                        onClick = { onTabSelected(key) }
                    )
                }
            }
        }

        // Hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )

        // ── 4. Chat List ────────────────────────────────────────────
        val filteredChats = chats.filter { c ->
            val matchTab = when (activeTab) {
                "unread" -> c.unreadCount > 0
                "direct" -> c.type == "direct"
                "group" -> c.type == "group"
                else -> true
            }
            val query = searchQuery.lowercase()
            val matchSearch = query.isEmpty() ||
                c.name.lowercase().contains(query) ||
                c.messages.any { it.text?.lowercase()?.contains(query) == true }
            matchTab && matchSearch
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredChats) { chat ->
                ChatListRow(
                    chat = chat,
                    isActive = chat.id == activeChatId,
                    onClick = { onChatSelected(chat.id) }
                )
            }
        }

        // ── 5. Footer ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSecondary.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LucideLock(tint = colors.success, size = 12.dp)
                Text(
                    text = "End-to-End Encrypted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }
            Text(
                text = "v4.2 Premium",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Story Item ───────────────────────────────────────────────

@Composable
private fun StoryItem(
    avatarUrl: String,
    name: String,
    hasNewStory: Boolean,
    isMyStory: Boolean,
    seen: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .width(56.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            // Story ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        when {
                            (isMyStory && hasNewStory) || (!isMyStory && !seen) ->
                                Brush.sweepGradient(StoryGradient)
                            else ->
                                SolidColor(if (!colors.isLight) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))
                        }
                    )
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(1.5.dp, colors.sidebarBg, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            // My story: plus badge
            if (isMyStory && !hasNewStory) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Add Story",
                    tint = colors.iosBlue,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                )
            }
        }

        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ── Filter Chip ─────────────────────────────────────────────

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(
                if (selected) colors.iosBlue
                else colors.surfaceSecondary
            )
            .border(
                0.5.dp,
                if (selected) Color.Transparent else colors.borderSubtle,
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else colors.textMuted
        )
    }
}

// ── Chat List Row ────────────────────────────────────────────

@Composable
private fun ChatListRow(
    chat: ChatConversation,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    val lastMsg = chat.messages.lastOrNull()
    val lastMsgPreview = when {
        lastMsg?.text != null -> lastMsg.text
        lastMsg?.mediaType != null -> "[${lastMsg.mediaType.replaceFirstChar { it.uppercase() }}]"
        else -> "No messages yet"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive) colors.iosBlueSubtle else Color.Transparent
            )
            .border(
                width = if (isActive) 0.5.dp else 0.dp,
                color = if (isActive) colors.iosBlue.copy(alpha = 0.25f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar
        Box(modifier = Modifier.size(44.dp)) {
            AsyncImage(
                model = chat.avatar,
                contentDescription = chat.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(0.5.dp, colors.borderSubtle, CircleShape),
                contentScale = ContentScale.Crop
            )
            if (chat.online) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(colors.success, CircleShape)
                        .border(2.dp, colors.sidebarBg, CircleShape)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = chat.name,
                fontSize = 14.sp,
                fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold
                             else FontWeight.Medium,
                color = if (isActive) colors.iosBlue else colors.textDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = (if (chat.type == "group" && lastMsg != null && lastMsg.mediaType != "call")
                           "${lastMsg.senderName}: " else "") + lastMsgPreview,
                fontSize = 13.sp,
                color = if (isActive) colors.iosBlue.copy(alpha = 0.8f)
                        else colors.textMuted,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Medium
                             else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right Meta Column
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(
                text = lastMsg?.time ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = if (chat.unreadCount > 0) colors.iosBlue
                        else colors.textMuted
            )

            // Unread badge or muted icon
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .widthIn(min = 18.dp)
                        .background(
                            if (chat.isMuted) colors.textMuted
                            else colors.iosBlue,
                            CircleShape
                        )
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+"
                               else chat.unreadCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else if (chat.isMuted) {
                LucideBellOff(tint = colors.textMuted, size = 13.dp)
            }
        }
    }
}

