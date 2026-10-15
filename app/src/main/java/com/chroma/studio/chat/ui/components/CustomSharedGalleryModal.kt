package com.chroma.studio.chat.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.GroupMember
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.launch

enum class SharedMediaType {
    PHOTO,
    VIDEO,
    DOCUMENT,
    LINK,
    AUDIO
}

data class SharedMediaItem(
    val id: String,
    val type: SharedMediaType,
    val title: String,
    val subtitle: String,
    val url: String,
    val senderName: String,
    val senderAvatar: String,
    val senderId: String,
    val timestamp: String,
    val fileExtension: String? = null,
    val duration: String? = null,
    val thumbnailUrl: String? = null
)

/**
 * Apple iOS HIG-Compliant Custom Gallery View for Media, Links, Docs, and Audio.
 * Features:
 * - Proper Apple-style search bar
 * - Tap to view (Fullscreen photo/video viewer, doc quick look, audio player, link launcher)
 * - Long-press to trigger Multi-Selection Mode
 * - Action toolbar: Edit, Share, Forward, Reply Privately (for group participants), Save, Delete
 */
@Composable
fun CustomSharedGalleryModal(
    chat: ChatConversation,
    allChats: List<ChatConversation>,
    initialTab: Int = 0,
    onClose: () -> Unit,
    onReplyPrivately: (senderName: String, mediaItem: SharedMediaItem) -> Unit,
    onForwardItems: (targetChatId: String, items: List<SharedMediaItem>) -> Unit,
    onToast: (String) -> Unit
) {
    val colors = LocalChatColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedItemIds.isNotEmpty()

    // Active Viewers / Dialogs
    var previewMediaItem by remember { mutableStateOf<SharedMediaItem?>(null) }
    var itemForForward by remember { mutableStateOf<List<SharedMediaItem>?>(null) }
    var itemForEdit by remember { mutableStateOf<SharedMediaItem?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    // Audio Playback Simulation State
    var activeAudioPlayingId by remember { mutableStateOf<String?>(null) }

    // Default Seed Data for the Chat's Shared Content
    var sharedItems by remember {
        mutableStateOf(
            listOf(
                // Media
                SharedMediaItem(
                    id = "m_1",
                    type = SharedMediaType.PHOTO,
                    title = "Hardware Node Prototype",
                    subtitle = "1920x1080 • 1.8 MB",
                    url = "https://images.unsplash.com/photo-1558002038-1055907df827?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Bholanath Kumbhakar",
                    senderAvatar = chat.avatar,
                    senderId = "user_bholanath",
                    timestamp = "Today, 10:24 AM"
                ),
                SharedMediaItem(
                    id = "m_2",
                    type = SharedMediaType.PHOTO,
                    title = "Design Token System",
                    subtitle = "2048x1536 • 2.4 MB",
                    url = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Sarah Connor",
                    senderAvatar = chat.avatar,
                    senderId = "user_sarah",
                    timestamp = "Yesterday, 3:45 PM"
                ),
                SharedMediaItem(
                    id = "m_3",
                    type = SharedMediaType.VIDEO,
                    title = "Orb UI Animation Demo",
                    subtitle = "4K • 18.2 MB",
                    url = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Joy",
                    senderAvatar = chat.avatar,
                    senderId = "user_me",
                    timestamp = "Oct 1, 11:20 AM",
                    duration = "0:42"
                ),
                SharedMediaItem(
                    id = "m_4",
                    type = SharedMediaType.PHOTO,
                    title = "Server Cluster Rack",
                    subtitle = "3840x2160 • 4.1 MB",
                    url = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Marcus Vance",
                    senderAvatar = chat.avatar,
                    senderId = "user_marcus",
                    timestamp = "Sep 29, 6:15 PM"
                ),
                SharedMediaItem(
                    id = "m_5",
                    type = SharedMediaType.PHOTO,
                    title = "Architecture Whiteboard",
                    subtitle = "1600x1200 • 1.2 MB",
                    url = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Sarah Connor",
                    senderAvatar = chat.avatar,
                    senderId = "user_sarah",
                    timestamp = "Sep 28, 2:10 PM"
                ),
                SharedMediaItem(
                    id = "m_6",
                    type = SharedMediaType.VIDEO,
                    title = "Product Walkthrough Reel",
                    subtitle = "1080p • 12.0 MB",
                    url = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?q=80&w=1200&auto=format&fit=crop",
                    senderName = "Marcus Vance",
                    senderAvatar = chat.avatar,
                    senderId = "user_marcus",
                    timestamp = "Sep 27, 4:50 PM",
                    duration = "1:15"
                ),

                // Docs
                SharedMediaItem(
                    id = "d_1",
                    type = SharedMediaType.DOCUMENT,
                    title = "Product_Roadmap_Q4.pdf",
                    subtitle = "2.4 MB • 14 pages",
                    url = "https://oscellion.chat/docs/roadmap.pdf",
                    senderName = "Bholanath Kumbhakar",
                    senderAvatar = chat.avatar,
                    senderId = "user_bholanath",
                    timestamp = "Yesterday, 4:10 PM",
                    fileExtension = "PDF"
                ),
                SharedMediaItem(
                    id = "d_2",
                    type = SharedMediaType.DOCUMENT,
                    title = "API_Security_Audit_Report.docx",
                    subtitle = "840 KB • Word Document",
                    url = "https://oscellion.chat/docs/security.docx",
                    senderName = "Marcus Vance",
                    senderAvatar = chat.avatar,
                    senderId = "user_marcus",
                    timestamp = "Sep 30, 11:05 AM",
                    fileExtension = "DOCX"
                ),
                SharedMediaItem(
                    id = "d_3",
                    type = SharedMediaType.DOCUMENT,
                    title = "Design_System_Tokens_v3.zip",
                    subtitle = "14.8 MB • Archive",
                    url = "https://oscellion.chat/docs/tokens.zip",
                    senderName = "Sarah Connor",
                    senderAvatar = chat.avatar,
                    senderId = "user_sarah",
                    timestamp = "Sep 29, 5:30 PM",
                    fileExtension = "ZIP"
                ),
                SharedMediaItem(
                    id = "d_4",
                    type = SharedMediaType.DOCUMENT,
                    title = "Quarterly_Budget_Model.xlsx",
                    subtitle = "1.1 MB • Spreadsheet",
                    url = "https://oscellion.chat/docs/budget.xlsx",
                    senderName = "Joy",
                    senderAvatar = chat.avatar,
                    senderId = "user_me",
                    timestamp = "Sep 28, 9:15 AM",
                    fileExtension = "XLSX"
                ),

                // Links
                SharedMediaItem(
                    id = "l_1",
                    type = SharedMediaType.LINK,
                    title = "Figma - Oscellion Design System & iOS UI Kit",
                    subtitle = "figma.com/@oscellion • UI Screens & HIG Tokens",
                    url = "https://figma.com/file/OscellionDesignSystem",
                    senderName = "Sarah Connor",
                    senderAvatar = chat.avatar,
                    senderId = "user_sarah",
                    timestamp = "Today, 9:30 AM"
                ),
                SharedMediaItem(
                    id = "l_2",
                    type = SharedMediaType.LINK,
                    title = "GitHub - Oscellion Chat Core Engine v3.2",
                    subtitle = "github.com/oscellion/chat-core • Production branch",
                    url = "https://github.com/oscellion/chat-core",
                    senderName = "Marcus Vance",
                    senderAvatar = chat.avatar,
                    senderId = "user_marcus",
                    timestamp = "Yesterday, 2:15 PM"
                ),
                SharedMediaItem(
                    id = "l_3",
                    type = SharedMediaType.LINK,
                    title = "Apple Human Interface Guidelines - Spatial & iOS UI",
                    subtitle = "developer.apple.com/design/human-interface-guidelines",
                    url = "https://developer.apple.com/design/human-interface-guidelines",
                    senderName = "Bholanath Kumbhakar",
                    senderAvatar = chat.avatar,
                    senderId = "user_bholanath",
                    timestamp = "Sep 29, 1:40 PM"
                ),

                // Audio
                SharedMediaItem(
                    id = "a_1",
                    type = SharedMediaType.AUDIO,
                    title = "Voice Memo - Hardware Sensor Calibration",
                    subtitle = "0:38 • 320 kbps High Quality",
                    url = "audio_calib.m4a",
                    senderName = "Bholanath Kumbhakar",
                    senderAvatar = chat.avatar,
                    senderId = "user_bholanath",
                    timestamp = "Today, 10:28 AM",
                    duration = "0:38"
                ),
                SharedMediaItem(
                    id = "a_2",
                    type = SharedMediaType.AUDIO,
                    title = "Voice Memo - UX Review & Sound Tuning",
                    subtitle = "1:14 • 320 kbps High Quality",
                    url = "audio_ux.m4a",
                    senderName = "Sarah Connor",
                    senderAvatar = chat.avatar,
                    senderId = "user_sarah",
                    timestamp = "Yesterday, 5:12 PM",
                    duration = "1:14"
                )
            )
        )
    }

    // Filter Items by active Tab and Search Query
    val currentType = when (selectedTab) {
        0 -> SharedMediaType.PHOTO // Includes Video
        1 -> SharedMediaType.DOCUMENT
        2 -> SharedMediaType.LINK
        else -> SharedMediaType.AUDIO
    }

    val filteredItems = remember(sharedItems, selectedTab, searchQuery) {
        sharedItems.filter { item ->
            val matchesTab = if (selectedTab == 0) {
                item.type == SharedMediaType.PHOTO || item.type == SharedMediaType.VIDEO
            } else {
                item.type == currentType
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.senderName.contains(searchQuery, ignoreCase = true) ||
                    item.url.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    val mediaCount = sharedItems.count { it.type == SharedMediaType.PHOTO || it.type == SharedMediaType.VIDEO }
    val docsCount = sharedItems.count { it.type == SharedMediaType.DOCUMENT }
    val linksCount = sharedItems.count { it.type == SharedMediaType.LINK }
    val audioCount = sharedItems.count { it.type == SharedMediaType.AUDIO }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 1. Top Navigation Bar (Apple HIG Style) ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(colors.headerBg)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isSelectionMode) {
                    // Back Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playReaction()
                                onClose()
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LucideChevronLeft(tint = colors.iosBlue, size = 20.dp)
                        Text(
                            text = "Back",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.iosBlue
                        )
                    }

                    // Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Media, Links & Docs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark
                        )
                        Text(
                            text = chat.name,
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    // Select Button
                    Text(
                        text = "Select",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playReaction()
                                if (filteredItems.isNotEmpty()) {
                                    selectedItemIds.add(filteredItems.first().id)
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                } else {
                    // Selection Mode Header
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playReaction()
                                selectedItemIds.clear()
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )

                    Text(
                        text = "${selectedItemIds.size} Selected",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )

                    Text(
                        text = if (selectedItemIds.size == filteredItems.size) "Deselect All" else "Select All",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playReaction()
                                if (selectedItemIds.size == filteredItems.size) {
                                    selectedItemIds.clear()
                                } else {
                                    selectedItemIds.clear()
                                    selectedItemIds.addAll(filteredItems.map { it.id })
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

            // ── 2. Apple iOS Inset Search Bar ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LucideSearch(tint = colors.textMuted, size = 16.dp)
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = colors.textDark
                        ),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search in Media, Links, Docs...",
                                    fontSize = 15.sp,
                                    color = colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colors.textMuted.copy(alpha = 0.4f))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideX(tint = Color.White, size = 11.dp)
                        }
                    }
                }
            }

            // ── 3. Segmented Navigation Tabs ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceSecondary)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Media ($mediaCount)",
                    "Docs ($docsCount)",
                    "Links ($linksCount)",
                    "Audio ($audioCount)"
                ).forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.cardBg else Color.Transparent)
                            .clickable {
                                SoundManager.playReaction()
                                selectedTab = idx
                                selectedItemIds.clear()
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.textDark else colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 4. Main Tab Content ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredItems.isEmpty()) {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            LucideSearch(tint = colors.textMuted, size = 28.dp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Results Found" else "No Items Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching content for \"$searchQuery\"" else "Shared items in this chat will appear here.",
                            fontSize = 14.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            // ── Media 3-Column Grid ──
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    val isSelected = selectedItemIds.contains(item.id)
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.surfaceSecondary)
                                            .pointerInput(isSelectionMode) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        SoundManager.playReaction()
                                                        if (!selectedItemIds.contains(item.id)) {
                                                            selectedItemIds.add(item.id)
                                                        }
                                                    },
                                                    onTap = {
                                                        if (isSelectionMode) {
                                                            SoundManager.playReaction()
                                                            if (isSelected) selectedItemIds.remove(item.id) else selectedItemIds.add(item.id)
                                                        } else {
                                                            SoundManager.playReaction()
                                                            previewMediaItem = item
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        AsyncImage(
                                            model = item.url,
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Video Badge Overlay
                                        if (item.type == SharedMediaType.VIDEO) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                LucideVideo(tint = Color.White, size = 10.dp)
                                                Text(
                                                    text = item.duration ?: "0:30",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        // Selection Checkmark Overlay
                                        if (isSelectionMode) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(if (isSelected) colors.iosBlue.copy(alpha = 0.25f) else Color.Transparent)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.iosBlue else Color.Black.copy(alpha = 0.45f))
                                                    .border(1.5.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    LucideCheck(tint = Color.White, size = 13.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ── Documents Inset Grouped List ──
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    val isSelected = selectedItemIds.contains(item.id)
                                    val badgeColor = when (item.fileExtension) {
                                        "PDF" -> Color(0xFFFF3B30)
                                        "DOCX" -> Color(0xFF007AFF)
                                        "ZIP" -> Color(0xFFFF9500)
                                        "XLSX" -> Color(0xFF34C759)
                                        else -> colors.iosBlue
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.cardBg)
                                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                            .pointerInput(isSelectionMode) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        SoundManager.playReaction()
                                                        if (!selectedItemIds.contains(item.id)) selectedItemIds.add(item.id)
                                                    },
                                                    onTap = {
                                                        if (isSelectionMode) {
                                                            SoundManager.playReaction()
                                                            if (isSelected) selectedItemIds.remove(item.id) else selectedItemIds.add(item.id)
                                                        } else {
                                                            SoundManager.playReaction()
                                                            previewMediaItem = item
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Document Extension Badge
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(badgeColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.fileExtension ?: "DOC",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }

                                        // Document Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "${item.subtitle} • ${item.senderName}",
                                                fontSize = 12.5.sp,
                                                color = colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.timestamp,
                                                fontSize = 11.sp,
                                                color = colors.textMuted.copy(alpha = 0.7f)
                                            )
                                        }

                                        // Selection indicator or quick action
                                        if (isSelectionMode) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.iosBlue else Color.Transparent)
                                                    .border(1.5.dp, if (isSelected) colors.iosBlue else colors.borderSubtle, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) LucideCheck(tint = Color.White, size = 14.dp)
                                            }
                                        } else {
                                            LucideChevronRight(tint = colors.textMuted.copy(alpha = 0.6f), size = 18.dp)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ── Links Inset Grouped List ──
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    val isSelected = selectedItemIds.contains(item.id)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.cardBg)
                                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                            .pointerInput(isSelectionMode) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        SoundManager.playReaction()
                                                        if (!selectedItemIds.contains(item.id)) selectedItemIds.add(item.id)
                                                    },
                                                    onTap = {
                                                        if (isSelectionMode) {
                                                            SoundManager.playReaction()
                                                            if (isSelected) selectedItemIds.remove(item.id) else selectedItemIds.add(item.id)
                                                        } else {
                                                            SoundManager.playReaction()
                                                            previewMediaItem = item
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Link Globe Icon Badge
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.iosBlueSubtle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LucideLink(tint = colors.iosBlue, size = 20.dp)
                                        }

                                        // Link Details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = item.url,
                                                fontSize = 13.sp,
                                                color = colors.iosBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Shared by ${item.senderName} • ${item.timestamp}",
                                                fontSize = 11.5.sp,
                                                color = colors.textMuted
                                            )
                                        }

                                        if (isSelectionMode) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.iosBlue else Color.Transparent)
                                                    .border(1.5.dp, if (isSelected) colors.iosBlue else colors.borderSubtle, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) LucideCheck(tint = Color.White, size = 14.dp)
                                            }
                                        } else {
                                            LucideChevronRight(tint = colors.textMuted.copy(alpha = 0.6f), size = 18.dp)
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // ── Audio & Voice Memos List ──
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    val isSelected = selectedItemIds.contains(item.id)
                                    val isPlaying = activeAudioPlayingId == item.id

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.cardBg)
                                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                            .pointerInput(isSelectionMode) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        SoundManager.playReaction()
                                                        if (!selectedItemIds.contains(item.id)) selectedItemIds.add(item.id)
                                                    },
                                                    onTap = {
                                                        if (isSelectionMode) {
                                                            SoundManager.playReaction()
                                                            if (isSelected) selectedItemIds.remove(item.id) else selectedItemIds.add(item.id)
                                                        } else {
                                                            SoundManager.playReaction()
                                                            activeAudioPlayingId = if (isPlaying) null else item.id
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Play / Pause Circle
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (isPlaying) colors.iosBlue else colors.iosBlueSubtle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPlaying) {
                                                LucideMic(tint = Color.White, size = 20.dp)
                                            } else {
                                                PhosphorPlay(tint = colors.iosBlue, size = 18.dp)
                                            }
                                        }

                                        // Audio Info & Animated Equalizer Waveform
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))

                                            // Waveform Visualizer
                                            val eqTransition = rememberInfiniteTransition(label = "audioEq")
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val barDurations = listOf(220, 310, 180, 260, 340, 190, 280, 310, 240, 370, 190, 290, 210, 330, 250, 180)
                                                barDurations.forEachIndexed { i, dur ->
                                                    val animatedHeight by eqTransition.animateFloat(
                                                        initialValue = 0.2f,
                                                        targetValue = 1f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(dur, easing = FastOutSlowInEasing),
                                                            repeatMode = RepeatMode.Reverse
                                                        ),
                                                        label = "wave$i"
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight(if (isPlaying) animatedHeight else 0.35f)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(if (isPlaying) colors.iosBlue else colors.textMuted.copy(alpha = 0.4f))
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${item.duration ?: "0:30"} • ${item.senderName}",
                                                fontSize = 11.5.sp,
                                                color = colors.textMuted
                                            )
                                        }

                                        if (isSelectionMode) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.iosBlue else Color.Transparent)
                                                    .border(1.5.dp, if (isSelected) colors.iosBlue else colors.borderSubtle, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) LucideCheck(tint = Color.White, size = 14.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. Bottom Selection Action Bar (Apple HIG Toolbar) ──────────
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val selectedItemsList = sharedItems.filter { it.id in selectedItemIds }
                val canReplyPrivately = chat.type == "group" && selectedItemsList.isNotEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.headerBg)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Forward Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                SoundManager.playReaction()
                                itemForForward = selectedItemsList
                            }
                            .padding(4.dp)
                    ) {
                        LucideForward(tint = colors.iosBlue, size = 22.dp)
                        Spacer(Modifier.height(2.dp))
                        Text("Forward", fontSize = 11.sp, color = colors.iosBlue, fontWeight = FontWeight.Medium)
                    }

                    // Share Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                SoundManager.playReaction()
                                val shareText = selectedItemsList.joinToString("\n") { "${it.title}: ${it.url}" }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share items")
                                context.startActivity(shareIntent)
                                onToast("Shared ${selectedItemsList.size} items")
                                selectedItemIds.clear()
                            }
                            .padding(4.dp)
                    ) {
                        LucideShare2(tint = colors.iosBlue, size = 22.dp)
                        Spacer(Modifier.height(2.dp))
                        Text("Share", fontSize = 11.sp, color = colors.iosBlue, fontWeight = FontWeight.Medium)
                    }

                    // Reply Privately (If in Group)
                    if (canReplyPrivately) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    SoundManager.playReaction()
                                    val targetItem = selectedItemsList.first()
                                    onReplyPrivately(targetItem.senderName, targetItem)
                                    onToast("Replying privately to ${targetItem.senderName}")
                                    selectedItemIds.clear()
                                }
                                .padding(4.dp)
                        ) {
                            LucideMessageSquare(tint = colors.iosBlue, size = 22.dp)
                            Spacer(Modifier.height(2.dp))
                            Text("Reply Privately", fontSize = 11.sp, color = colors.iosBlue, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Save / Download Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                SoundManager.playReaction()
                                onToast("Saved ${selectedItemsList.size} items to device")
                                selectedItemIds.clear()
                            }
                            .padding(4.dp)
                    ) {
                        LucideDownload(tint = colors.iosBlue, size = 22.dp)
                        Spacer(Modifier.height(2.dp))
                        Text("Save", fontSize = 11.sp, color = colors.iosBlue, fontWeight = FontWeight.Medium)
                    }

                    // Delete Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                SoundManager.playReaction()
                                sharedItems = sharedItems.filter { it.id !in selectedItemIds }
                                onToast("${selectedItemIds.size} items removed")
                                selectedItemIds.clear()
                            }
                            .padding(4.dp)
                    ) {
                        LucideTrash(tint = Color(0xFFFF3B30), size = 22.dp)
                        Spacer(Modifier.height(2.dp))
                        Text("Delete", fontSize = 11.sp, color = Color(0xFFFF3B30), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── 6. Fullscreen Media Preview Modal / Quick Look Dialog ───────────
        previewMediaItem?.let { item ->
            Dialog(
                onDismissRequest = { previewMediaItem = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Preview Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        SoundManager.playReaction()
                                        previewMediaItem = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideX(tint = Color.White, size = 18.dp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.senderName} • ${item.timestamp}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        SoundManager.playReaction()
                                        itemForEdit = item
                                        editingTitle = item.title
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideEdit(tint = Color.White, size = 16.dp)
                            }
                        }

                        // Preview Main Display
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.type == SharedMediaType.PHOTO || item.type == SharedMediaType.VIDEO) {
                                AsyncImage(
                                    model = item.url,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (item.type == SharedMediaType.DOCUMENT) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .padding(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(colors.iosBlueSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.fileExtension ?: "DOC", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                    }
                                    Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                                    Text(item.subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            } else if (item.type == SharedMediaType.LINK) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .padding(32.dp)
                                ) {
                                    LucideLink(tint = colors.iosBlue, size = 48.dp)
                                    Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                                    Text(item.url, fontSize = 14.sp, color = colors.iosBlue, textAlign = TextAlign.Center)
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .padding(32.dp)
                                ) {
                                    LucideMic(tint = colors.iosBlue, size = 48.dp)
                                    Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                                    Text(item.subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // Preview Bottom Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Forward
                            IconButton(onClick = {
                                SoundManager.playReaction()
                                itemForForward = listOf(item)
                            }) {
                                LucideForward(tint = Color.White, size = 22.dp)
                            }

                            // Share
                            IconButton(onClick = {
                                SoundManager.playReaction()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}: ${item.url}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share item"))
                            }) {
                                LucideShare2(tint = Color.White, size = 22.dp)
                            }

                            // Reply Privately (if group)
                            if (chat.type == "group") {
                                IconButton(onClick = {
                                    SoundManager.playReaction()
                                    onReplyPrivately(item.senderName, item)
                                    previewMediaItem = null
                                }) {
                                    LucideMessageSquare(tint = colors.iosBlue, size = 22.dp)
                                }
                            }

                            // Save
                            IconButton(onClick = {
                                SoundManager.playReaction()
                                onToast("Saved to device gallery")
                            }) {
                                LucideDownload(tint = Color.White, size = 22.dp)
                            }
                        }
                    }
                }
            }
        }

        // ── 7. Forward Media Contact Selector Modal ─────────────────────────
        itemForForward?.let { forwardList ->
            Dialog(
                onDismissRequest = { itemForForward = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.75f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(colors.cardBg)
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Forward ${forwardList.size} item(s)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceSecondary)
                                    .clickable { itemForForward = null },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideX(tint = colors.textDark, size = 15.dp)
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "SELECT RECIPIENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allChats) { targetChat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceSecondary)
                                        .clickable {
                                            SoundManager.playSend()
                                            onForwardItems(targetChat.id, forwardList)
                                            onToast("Forwarded to ${targetChat.name}")
                                            itemForForward = null
                                            selectedItemIds.clear()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = targetChat.avatar,
                                        contentDescription = targetChat.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetChat.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textDark
                                        )
                                        Text(
                                            text = targetChat.statusText,
                                            fontSize = 12.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                    LucideForward(tint = colors.iosBlue, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 8. Edit Item Title / Caption Dialog ──────────────────────────────
        itemForEdit?.let { item ->
            Dialog(
                onDismissRequest = { itemForEdit = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBg)
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Item Name",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )

                    BasicTextField(
                        value = editingTitle,
                        onValueChange = { editingTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceSecondary)
                            .padding(12.dp),
                        textStyle = TextStyle(fontSize = 15.sp, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { itemForEdit = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSecondary)
                        ) {
                            Text("Cancel", color = colors.textDark)
                        }

                        Button(
                            onClick = {
                                if (editingTitle.isNotBlank()) {
                                    sharedItems = sharedItems.map { if (it.id == item.id) it.copy(title = editingTitle) else it }
                                    if (previewMediaItem?.id == item.id) {
                                        previewMediaItem = previewMediaItem?.copy(title = editingTitle)
                                    }
                                    onToast("Item updated")
                                }
                                itemForEdit = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

