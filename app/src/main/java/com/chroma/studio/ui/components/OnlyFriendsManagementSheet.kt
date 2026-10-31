package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.X
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

val OnlyFriendEmerald = Color(0xFF00C853)
val OnlyFriendNeon = Color(0xFF00E676)

/**
 * Pixel-perfect Instagram-style "Only Friend" (Close Friends) list manager.
 * Allows searching followed users, selecting / deselecting with a green checkmark,
 * and saving the custom audience list to Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlyFriendsManagementSheet(
    storyViewModel: StoryViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val authUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentOnlyFriends by storyViewModel.onlyFriendsIds.collectAsState()
    var selectedIds by remember(currentOnlyFriends) { mutableStateOf(currentOnlyFriends.toSet()) }

    var candidateUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch candidate users from following & followers
    LaunchedEffect(authUid) {
        if (authUid.isNotBlank()) {
            isLoadingUsers = true
            try {
                val repo = DiscoverRepository()
                val following = repo.getFollowing(authUid)
                val followers = repo.getFollowers(authUid)
                val combined = (following + followers).distinctBy { it.uid }.filter { it.uid != authUid }
                candidateUsers = combined
            } catch (_: Exception) {
            } finally {
                isLoadingUsers = false
            }
        }
    }

    val filteredUsers = remember(candidateUsers, searchQuery, selectedIds) {
        val query = searchQuery.trim().lowercase()
        val sorted = candidateUsers.sortedByDescending { selectedIds.contains(it.uid) }
        if (query.isBlank()) {
            sorted
        } else {
            sorted.filter {
                it.username.lowercase().contains(query) ||
                it.displayName.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
        contentColor = colors.textMain,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.18f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header Row: Title with green star + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(OnlyFriendEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "Only Friend",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        letterSpacing = (-0.4).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        .clickable { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = colors.textMain,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Subtitle Description
            Text(
                text = "We don't send notifications when you edit your Only Friend list. Only people on this list can view stories shared with Only Friend.",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = colors.textMuted
            )

            Spacer(Modifier.height(14.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = "Search",
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
                        cursorBrush = SolidColor(OnlyFriendEmerald),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search people...",
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Count summary & Clear All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedIds.size} selected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )

                if (selectedIds.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF3B30),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedIds = emptySet()
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // User List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoadingUsers) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = OnlyFriendEmerald,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (filteredUsers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No people found" else "Follow people to add them to your Only Friend list",
                            color = colors.textMuted,
                            fontSize = 13.5.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            val isSelected = selectedIds.contains(user.uid)
                            OnlyFriendUserRow(
                                user = user,
                                isSelected = isSelected,
                                isDark = colors.isDark,
                                onToggle = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playReaction()
                                    selectedIds = if (isSelected) {
                                        selectedIds - user.uid
                                    } else {
                                        selectedIds + user.uid
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Save / Done Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundManager.playSend()
                    storyViewModel.saveOnlyFriendsList(selectedIds.toList())
                    ToastManager.showToast("Only Friend list updated! ⭐")
                    onDismissRequest()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnlyFriendEmerald,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Done (${selectedIds.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnlyFriendUserRow(
    user: UserProfile,
    isSelected: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            IosAvatarView(
                name = user.displayName.ifBlank { user.username },
                avatarUrl = user.avatarUrl,
                size = 46.dp,
                gender = user.gender
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((-2).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName.ifBlank { user.username.ifBlank { "User" } },
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.verified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedBadge(size = 12.dp)
                    }
                }
                if (user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Circular Selection Checkmark
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(OnlyFriendEmerald)
                    } else {
                        Modifier.border(
                            1.5.dp,
                            if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f),
                            CircleShape
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
