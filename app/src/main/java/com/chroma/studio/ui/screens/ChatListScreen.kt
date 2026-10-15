package com.chroma.studio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AppleStatusBlueGradient
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChatViewModel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    storyViewModel: StoryViewModel = viewModel(),
    onChatClick: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToCreateStory: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val haptic = LocalHapticFeedback.current
    val recentChats by chatViewModel.recentChats.collectAsState()
    val userProfiles by chatViewModel.userProfiles.collectAsState()
    val mutedChatIds by chatViewModel.mutedChatIds.collectAsState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

    val myStories by storyViewModel.myStories.collectAsState()
    val storyGroups by storyViewModel.storyGroups.collectAsState()

    // 0: All, 1: Unread, 2: Direct, 3: Groups
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Colors matching the HTML theme
    val iosBlue = Color(0xFF0078FE)
    val sidebarBg = if (isDark) Color(0xFF0F172A) else Color.White
    val borderSubtle = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textDark = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Pre-filtering logic
    val filteredChats = remember(recentChats, selectedTab, searchQuery, currentUser, userProfiles) {
        val uid = currentUser?.uid ?: ""
        var filtered = recentChats

        // Tab Filter
        filtered = when (selectedTab) {
            1 -> filtered.filter { (it.unreadCount[uid] ?: 0) > 0 }
            2 -> filtered.filter { !it.isGroup }
            3 -> filtered.filter { it.isGroup }
            else -> filtered
        }

        // Search Filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { chat ->
                val title = chat.name ?: (chat.participants.firstOrNull { it != uid }?.let { userProfiles[it]?.displayName } ?: "Unknown")
                title.contains(searchQuery, ignoreCase = true) || chat.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
        filtered
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sidebarBg)
    ) {
        // =========================================================
        // SIDEBAR HEADER (Avatar, Name, Online Dot, New Msg)
        // =========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp)) {
                    IosAvatarView(
                        name = currentUser?.displayName ?: "User",
                        avatarUrl = currentUser?.photoUrl?.toString() ?: "",
                        size = 36.dp,
                        modifier = Modifier.border(2.dp, iosBlue.copy(alpha = 0.3f), CircleShape)
                    )
                    // Online Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                            .border(2.dp, sidebarBg, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = currentUser?.displayName ?: "Alex Rivera",
                        color = textDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            IconButton(
                onClick = { onNavigateToCreateGroup() },
                modifier = Modifier
                    .size(36.dp)
                    .background(iosBlue.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Lucide.Pencil, contentDescription = "New Chat", tint = iosBlue, modifier = Modifier.size(20.dp))
            }
        }
        Divider(color = borderSubtle, thickness = 1.dp)

        // =========================================================
        // EPHEMERAL SOCIAL STORIES CAROUSEL
        // =========================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color.White.copy(0.02f) else Color.Black.copy(0.02f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORIES & UPDATES",
                    color = textMuted.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToCreateStory?.invoke() }
                ) {
                    Icon(Lucide.Plus, contentDescription = "Add Story", tint = iosBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add Story", color = iosBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // My Story Item
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateToCreateStory?.invoke() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.5.dp, if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6), CircleShape)
                                .padding(2.dp)
                        ) {
                            IosAvatarView(
                                name = currentUser?.displayName ?: "Me",
                                avatarUrl = currentUser?.photoUrl?.toString() ?: "",
                                size = 44.dp
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(14.dp)
                                    .background(iosBlue, CircleShape)
                                    .border(2.dp, sidebarBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Your Story", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                // Mock Other Stories (Replace with actual data)
                items(3) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, Brush.sweepGradient(AppleStatusBlueGradient), CircleShape)
                                .padding(2.dp)
                        ) {
                            IosAvatarView(
                                name = "Friend $index",
                                avatarUrl = "https://i.pravatar.cc/150?img=${index + 12}",
                                size = 44.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Update $index", color = textDark.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Divider(color = borderSubtle, thickness = 1.dp)

        // =========================================================
        // SEARCH & CONVERSATION TABS
        // =========================================================
        Column(modifier = Modifier.padding(12.dp)) {
            // Search Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Search, contentDescription = "Search", tint = textMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = textDark, fontSize = 13.sp),
                        modifier = Modifier.weight(1f),
                        cursorBrush = SolidColor(iosBlue),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search chats, people, groups...", color = textMuted.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear",
                            tint = textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            // Tabs
            val tabs = listOf("All", "Unread", "Direct", "Groups")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tabs.size) { index ->
                    val isSelected = selectedTab == index
                    val tabBg = if (isSelected) iosBlue else if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6)
                    val tabTextColor = if (isSelected) Color.White else textMuted
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(tabBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = index
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(tabs[index], color = tabTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // =========================================================
        // CONVERSATIONS LIST STREAM
        // =========================================================
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (filteredChats.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Lucide.MessageCircle, null, tint = textMuted.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No chats found", color = textMuted.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }
            
            items(filteredChats, key = { it.id }) { chat ->
                val uid = currentUser?.uid ?: ""
                val otherUserId = chat.participants.firstOrNull { it != uid }
                val profile = otherUserId?.let { userProfiles[it] }
                val chatTitle = chat.name ?: profile?.displayName ?: "Unknown"
                val chatAvatar = chat.avatarUrl ?: profile?.avatarUrl
                val unread = chat.unreadCount[uid] ?: 0
                val isMuted = mutedChatIds.contains(chat.id)
                val isOnline = false // Wire up to actual online presence if available

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onChatClick(chat.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar & Online Status
                    Box(modifier = Modifier.size(44.dp)) {
                        IosAvatarView(name = chatTitle, avatarUrl = chatAvatar ?: "", size = 44.dp)
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                                    .border(2.dp, sidebarBg, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Middle Content (Name, Msg Preview)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chatTitle,
                                color = textDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = smartTimeAgo(chat.lastMessageTime),
                                color = textMuted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.lastMessage.ifBlank { "Attachment" },
                                color = if (unread > 0) iosBlue else textMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (unread > 0) {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .defaultMinSize(minWidth = 18.dp)
                                        .background(iosBlue, RoundedCornerShape(9.dp))
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(unread.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (isMuted) {
                                Icon(Lucide.BellOff, contentDescription = "Muted", tint = textMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // =========================================================
        // SIDEBAR FOOTER
        // =========================================================
        Divider(color = borderSubtle, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color.White.copy(0.02f) else Color.Black.copy(0.02f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.ShieldCheck, contentDescription = "Encrypted", tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("End-to-End Encrypted", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text("v4.1", color = textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// Utility Time Formatter (copied from original to preserve logic)
fun smartTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val cal = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isToday = cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) &&
        cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    val isYesterday = cal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 &&
        cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    return when {
        isToday -> java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        isYesterday -> "Yesterday"
        diff < 7 * 24 * 60 * 60 * 1000L -> java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}
