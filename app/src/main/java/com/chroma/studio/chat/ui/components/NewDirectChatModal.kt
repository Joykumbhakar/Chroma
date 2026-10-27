package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.VerifiedBadge
import kotlinx.coroutines.delay

/**
 * Direct 1-on-1 User Chat Selection Modal (New Message).
 *
 * Allows users to search the entire community by name or @username,
 * pick any user, and immediately start a direct 1-on-1 chat.
 * Strictly adheres to clean Oscellion design without circular/pill icon containers.
 */
@Composable
fun NewDirectChatModal(
    currentUser: User,
    availableContacts: List<GroupContact> = emptyList(),
    onDismiss: () -> Unit,
    onSelectUser: (userId: String) -> Unit,
    onOpenCreateGroup: () -> Unit
) {
    val colors = LocalChatColors.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    val discoverRepo = remember { DiscoverRepository() }

    var suggestedUsers by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Load initial suggested creators from DiscoverRepository
    LaunchedEffect(Unit) {
        try {
            val suggested = discoverRepo.getSuggestedCreators(limit = 40)
            suggestedUsers = suggested.filter { it.uid != currentUser.id }.map { prof ->
                GroupContact(
                    id = prof.uid,
                    name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                    role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                    avatar = prof.avatarUrl,
                    status = if (prof.online) "Online" else "Active on Oscellion",
                    isVerified = prof.verified
                )
            }
        } catch (_: Exception) {}
    }

    // Debounced search query
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            isSearching = true
            delay(280L)
            try {
                val cleanQuery = q.removePrefix("@")
                val results = discoverRepo.searchUsers(cleanQuery)
                searchResults = results.filter { it.uid != currentUser.id }.map { prof ->
                    GroupContact(
                        id = prof.uid,
                        name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                        role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                        avatar = prof.avatarUrl,
                        status = if (prof.online) "Online" else "Active on Oscellion",
                        isVerified = prof.verified
                    )
                }
            } catch (_: Exception) {
                searchResults = emptyList()
            }
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    val allBaseContacts = remember(availableContacts, suggestedUsers) {
        (availableContacts + suggestedUsers).distinctBy { it.id }
    }

    val displayList = remember(searchQuery, allBaseContacts, searchResults) {
        if (searchQuery.isBlank()) {
            allBaseContacts
        } else if (searchResults.isNotEmpty()) {
            (searchResults + allBaseContacts.filter {
                val q = searchQuery.trim().lowercase()
                it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
            }).distinctBy { it.id }
        } else {
            val q = searchQuery.trim().lowercase()
            allBaseContacts.filter {
                it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBg),
            color = colors.appBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // ── Top Navigation Bar ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.appBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onDismiss()
                            }
                            .padding(vertical = 4.dp)
                    )

                    Text(
                        text = "New Message",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )

                    // Spacer for visual symmetry
                    Spacer(Modifier.width(48.dp))
                }

                // ── Search Input Field ────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceSecondary)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhosphorMagnifyingGlass(tint = colors.textMuted, size = 16.dp)

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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
                                        text = "Search name or @username",
                                        fontSize = 15.sp,
                                        color = colors.textMuted
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = colors.iosBlue,
                                strokeWidth = 1.5.dp
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "✕",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { searchQuery = "" }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.separator,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // ── Contact List / User Selection ────────────────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Option 1: New Group (clean icon without pill/circle background)
                    if (searchQuery.isBlank()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onOpenCreateGroup()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Users,
                                    contentDescription = "New Group",
                                    tint = colors.iosBlue,
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "New Group",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.iosBlue
                                    )
                                    Text(
                                        text = "Chat with multiple people",
                                        fontSize = 13.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = colors.separator,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 54.dp)
                            )
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            text = if (searchQuery.isBlank()) "SUGGESTED PEOPLE" else "SEARCH RESULTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                        )
                    }

                    // User rows
                    if (displayList.isEmpty() && !isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No users found" else "No matching users for \"$searchQuery\"",
                                    fontSize = 14.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    } else {
                        items(displayList, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        focusManager.clearFocus()
                                        onSelectUser(contact.id)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Contact Avatar with Online Dot
                                Box(modifier = Modifier.size(46.dp)) {
                                    AsyncImage(
                                        model = contact.avatar,
                                        contentDescription = contact.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(0.5.dp, colors.borderSubtle, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (contact.status == "Online") {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(colors.success, CircleShape)
                                                .border(2.dp, colors.appBg, CircleShape)
                                        )
                                    }
                                }

                                // User Details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (contact.isVerified) {
                                            VerifiedBadge(size = 14.dp)
                                        }
                                    }

                                    Text(
                                        text = contact.role,
                                        fontSize = 13.5.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Online / Status Indicator text
                                Text(
                                    text = contact.status,
                                    fontSize = 12.sp,
                                    color = if (contact.status == "Online") colors.success else colors.textMuted
                                )
                            }

                            HorizontalDivider(
                                color = colors.separator,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 74.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
