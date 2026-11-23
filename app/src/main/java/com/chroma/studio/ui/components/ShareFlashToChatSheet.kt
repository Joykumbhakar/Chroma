package com.chroma.studio.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChatViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth

/**
 * Instagram-style "Share to Chat" modal bottom sheet.
 * Allows instant 1-tap dispatch of Flash videos to recent direct and group chats,
 * multi-sharing to multiple friends, search filtering, and external link copying.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFlashToChatSheet(
    flashVideo: FlashVideo,
    chatViewModel: ChatViewModel,
    onDismissRequest: () -> Unit,
    onExternalShare: () -> Unit,
    onShareSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val recentRooms by chatViewModel.recentChats.collectAsState()
    val userProfiles by chatViewModel.userProfiles.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val sentRoomIds = remember { mutableStateListOf<String>() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filtered rooms: active direct and group rooms matching search
    val filteredRooms = remember(recentRooms, searchQuery, userProfiles, currentUserId) {
        val nonDeleted = recentRooms.filter { room ->
            !room.deletedBy.contains(currentUserId)
        }
        if (searchQuery.isBlank()) {
            nonDeleted
        } else {
            val q = searchQuery.trim().lowercase()
            nonDeleted.filter { room ->
                if (room.isGroup) {
                    (room.name ?: "Group").lowercase().contains(q)
                } else {
                    val otherUid = room.participants.firstOrNull { it != currentUserId }
                    val prof = otherUid?.let { userProfiles[it] }
                    (prof?.displayName?.lowercase()?.contains(q) == true) ||
                    (prof?.username?.lowercase()?.contains(q) == true) ||
                    (room.name?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = if (colors.isDark) Color(0xFF161618) else Color(0xFFF9F9FB),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.20f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            // ── 1. Video Preview Card Header ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .studioBevel(cornerRadius = 14.dp, borderWidth = 0.8.dp)
                    .background(if (colors.isDark) Color(0xFF222226) else Color(0xFFEFEFF3))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 9:16 Video Mini-Poster
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(64.dp)
                        .studioBevel(cornerRadius = 8.dp, borderWidth = 0.6.dp)
                        .background(Color.Black)
                ) {
                    if (flashVideo.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = flashVideo.thumbnailUrl,
                            contentDescription = "Flash thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp).offset(x = 0.5.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Send Flash to...",
                            color = colors.textMain,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Lucide.Send,
                            contentDescription = null,
                            tint = Color(0xFF0381FE),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${flashVideo.authorUsername.ifBlank { "creator" }}",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (flashVideo.authorVerified) {
                            VerifiedBadge(size = 11.dp)
                        }
                    }

                    if (flashVideo.caption.isNotBlank()) {
                        Text(
                            text = flashVideo.caption,
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 2. Search Input Field ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .studioBevel(cornerRadius = 12.dp, borderWidth = 0.8.dp)
                    .background(if (colors.isDark) Color(0xFF1E1E22) else Color(0xFFE8E8ED))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFF0381FE)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search direct messages & groups...",
                                    color = colors.textMuted,
                                    fontSize = 13.5.sp
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
                                .background(colors.textMuted.copy(alpha = 0.25f))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Clear search",
                                tint = colors.textMain,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 3. Recipients List (Direct & Group Conversations) ───────────────
            Text(
                text = if (searchQuery.isBlank()) "Recent Conversations" else "Results",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            if (filteredRooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.MessageSquare,
                            contentDescription = null,
                            tint = colors.textMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No recent chats found" else "No matching conversations",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRooms, key = { it.id }) { room ->
                        val isSent = sentRoomIds.contains(room.id)
                        ShareRoomItem(
                            room = room,
                            currentUserId = currentUserId,
                            userProfiles = userProfiles,
                            isSent = isSent,
                            onSendClick = {
                                if (!isSent) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sentRoomIds.add(room.id)
                                    chatViewModel.sendFlashVideo(
                                        roomId = room.id,
                                        flashVideo = flashVideo
                                    )
                                    onShareSuccess()
                                    ToastManager.showToast("Sent to ${getRoomDisplayName(room, currentUserId, userProfiles)}")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4. Quick Action Buttons Row (External Share / Copy Link) ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy Link Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .studioBevel(cornerRadius = 14.dp, borderWidth = 0.8.dp)
                        .background(if (colors.isDark) Color(0xFF222226) else Color(0xFFE8E8ED))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Flash Link", "https://oscellion.vercel.app/flash/${flashVideo.id}")
                            clipboard.setPrimaryClip(clip)
                            ToastManager.showToast("Link copied to clipboard!")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Link,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Copy link",
                            color = colors.textMain,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Share to Other Apps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .studioBevel(cornerRadius = 14.dp, borderWidth = 0.8.dp)
                        .background(if (colors.isDark) Color(0xFF222226) else Color(0xFFE8E8ED))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismissRequest()
                            onExternalShare()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Share2,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Share via...",
                            color = colors.textMain,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareRoomItem(
    room: ChatRoom,
    currentUserId: String,
    userProfiles: Map<String, com.chroma.studio.model.UserProfile>,
    isSent: Boolean,
    onSendClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val displayName = getRoomDisplayName(room, currentUserId, userProfiles)
    val avatarUrl = getRoomAvatarUrl(room, currentUserId, userProfiles)
    val isVerified = if (!room.isGroup) {
        val otherUid = room.participants.firstOrNull { it != currentUserId }
        otherUid?.let { userProfiles[it]?.verified } == true
    } else false

    val subtitle = if (room.isGroup) {
        "${room.participants.size} members"
    } else {
        val otherUid = room.participants.firstOrNull { it != currentUserId }
        val prof = otherUid?.let { userProfiles[it] }
        if (!prof?.username.isNullOrBlank()) "@${prof?.username}" else "Direct Message"
    }

    val buttonBg by animateColorAsState(
        targetValue = if (isSent) Color(0xFF10B981) else Color(0xFF0381FE),
        label = "SendButtonBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (colors.isDark) Color(0xFF1C1C1F) else Color(0xFFF2F2F7))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar with verified badge
            Box(modifier = Modifier.size(40.dp)) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (room.isGroup) Color(0xFF0072DE) else Color(0xFF0381FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (room.isGroup) {
                            Icon(
                                imageVector = Lucide.Users,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVerified) {
                        VerifiedBadge(size = 11.dp)
                    }
                }

                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 1-Tap Animated Send / Sent Button
        Box(
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 68.dp)
                .studioBevel(cornerRadius = 16.dp, borderWidth = 0.6.dp)
                .background(buttonBg)
                .clickable(enabled = !isSent) { onSendClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isSent) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "Sent",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Sent",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Send",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getRoomDisplayName(
    room: ChatRoom,
    currentUserId: String,
    userProfiles: Map<String, com.chroma.studio.model.UserProfile>
): String {
    return if (room.isGroup) {
        room.name?.takeIf { it.isNotBlank() } ?: "Group"
    } else {
        val otherUid = room.participants.firstOrNull { it != currentUserId }
        val prof = otherUid?.let { userProfiles[it] }
        prof?.displayName?.takeIf { it.isNotBlank() }
            ?: prof?.username?.takeIf { it.isNotBlank() }
            ?: room.name?.takeIf { it.isNotBlank() }
            ?: "Direct Message"
    }
}

private fun getRoomAvatarUrl(
    room: ChatRoom,
    currentUserId: String,
    userProfiles: Map<String, com.chroma.studio.model.UserProfile>
): String? {
    return if (room.isGroup) {
        room.avatarUrl
    } else {
        val otherUid = room.participants.firstOrNull { it != currentUserId }
        userProfiles[otherUid]?.avatarUrl
    }
}
