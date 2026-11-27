package com.chroma.studio.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.CollabParticipant
import com.chroma.studio.model.MAX_COLLAB_PARTICIPANTS
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.formatCollabHeaderAttribution
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay

/**
 * Pixel-perfect stacked overlapping avatar pair for co-authored Posts and Flashes.
 */
@Composable
fun CollabStackedAvatars(
    primaryName: String,
    primaryAvatarUrl: String,
    secondaryName: String,
    secondaryAvatarUrl: String,
    cutoutRingColor: Color,
    size: Dp = 30.dp,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(size + 18.dp)
            .height(size + 6.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .border(1.5.dp, cutoutRingColor, CircleShape)
        ) {
            IosAvatarView(
                name = primaryName,
                avatarUrl = primaryAvatarUrl,
                size = size
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(cutoutRingColor, CircleShape)
                .padding(2.dp)
        ) {
            IosAvatarView(
                name = secondaryName,
                avatarUrl = secondaryAvatarUrl,
                size = size - 4.dp
            )
        }
    }
}

/**
 * Single-line Instagram-style co-author attribution (`alice and bob` / `alice and 2 others`).
 */
@Composable
fun CollabCoAuthorTitleRow(
    primaryLabel: String,
    primaryVerified: Boolean,
    primaryVerifiedTone: String? = null,
    acceptedCollaborators: List<CollabParticipant>,
    textColor: Color,
    mutedColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.5.sp,
    onPrimaryClick: () -> Unit,
    onCollaboratorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstCollab = acceptedCollaborators.firstOrNull()
    val collabLabels = acceptedCollaborators.map { c ->
        c.username.ifBlank { c.displayName }
    }
    val attr = remember(primaryLabel, collabLabels) {
        formatCollabHeaderAttribution(primaryLabel, collabLabels)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = attr.primaryLabel,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable { onPrimaryClick() }
        )
        if (primaryVerified) {
            Spacer(Modifier.width(3.dp))
            VerifiedBadge(size = 12.dp, tone = primaryVerifiedTone)
        }
        if (attr.connector != null && attr.secondaryLabel != null) {
            Text(
                text = attr.connector,
                color = mutedColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily
            )
            Text(
                text = attr.secondaryLabel,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onCollaboratorClick() }
            )
            if (!attr.hasMultipleCollaborators && firstCollab?.verified == true) {
                Spacer(Modifier.width(3.dp))
                VerifiedBadge(size = 12.dp, tone = firstCollab.verifiedTone)
            }
        }
    }
}

/**
 * Slim top banner shown only to users who have a pending collaborator invite on this post or Flash.
 */
@Composable
fun PendingCollabReviewStrip(
    creatorHandle: String,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    darkSurface: Boolean = false
) {
    val colors = LocalOscellionColors.current
    val bgColor = if (darkSurface) Color(0xCC18181B) else colors.bg
    val textColor = if (darkSurface) Color.White else colors.textMain
    val mutedColor = if (darkSurface) Color.White.copy(alpha = 0.72f) else colors.textMuted
    val dividerColor = if (darkSurface) Color.White.copy(alpha = 0.12f) else colors.glassBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Lucide.Users,
                    contentDescription = null,
                    tint = Color(0xFF0381FE),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "@${creatorHandle.ifBlank { "creator" }} invited you to collaborate",
                    color = textColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Decline",
                    color = mutedColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.clickable(enabled = !isProcessing) { onDecline() }
                )
                Text(
                    text = if (isProcessing) "..." else "Accept",
                    color = Color(0xFF0381FE),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.clickable(enabled = !isProcessing) { onAccept() }
                )
            }
        }
        HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
    }
}

/**
 * Clean settings row for selecting collaborators inside Post and Flash creation screens.
 */
@Composable
fun CollabInviteActionRow(
    selectedCollaborators: List<CollabParticipant>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val summary = when {
        selectedCollaborators.isEmpty() -> "Invite"
        selectedCollaborators.size == 1 -> "@${selectedCollaborators.first().username.ifBlank { selectedCollaborators.first().displayName }}"
        else -> "${selectedCollaborators.size} invited"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Lucide.Users,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Invite collaborators",
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "Shares this post on both profiles when accepted",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = summary,
                    color = if (selectedCollaborators.isNotEmpty()) colors.primary else colors.textMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selectedCollaborators.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Searchable bottom sheet allowing creators to select up to [MAX_COLLAB_PARTICIPANTS] collaborators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollabPickerModalSheet(
    currentUserUid: String,
    initialSelection: List<CollabParticipant>,
    repository: DiscoverRepository,
    onDone: (List<CollabParticipant>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val selectedList = remember {
        mutableStateListOf<CollabParticipant>().apply { addAll(initialSelection) }
    }

    LaunchedEffect(searchQuery) {
        val clean = searchQuery.trim().removePrefix("@")
        if (clean.isEmpty()) {
            isLoading = true
            searchResults = try {
                repository.getSuggestedCreators(15).filter { it.uid != currentUserUid }
            } catch (_: Exception) {
                emptyList()
            }
            isLoading = false
        } else {
            delay(220L)
            isLoading = true
            searchResults = try {
                repository.searchUsersByUsername(clean).filter { it.uid != currentUserUid }
            } catch (_: Exception) {
                emptyList()
            }
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Invite Collaborators",
                        color = colors.textMain,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "Up to $MAX_COLLAB_PARTICIPANTS people (${selectedList.size}/$MAX_COLLAB_PARTICIPANTS)",
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        fontFamily = InterFontFamily
                    )
                }
                Text(
                    text = "Done",
                    color = colors.primary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier
                        .clickable { onDone(selectedList.toList()) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            if (selectedList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedList, key = { it.uid }) { collab ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primary.copy(alpha = 0.10f))
                                .clickable { selectedList.removeAll { it.uid == collab.uid } }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            IosAvatarView(
                                name = collab.displayName.ifBlank { collab.username },
                                avatarUrl = collab.avatarUrl,
                                size = 20.dp
                            )
                            Text(
                                text = "@${collab.username.ifBlank { collab.displayName }}",
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Remove",
                                tint = colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.inputBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Lucide.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search by @username...", color = colors.textMuted, fontSize = 13.5.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textMain, fontSize = 13.5.sp, fontFamily = InterFontFamily),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 340.dp)
                ) {
                    items(searchResults, key = { it.uid }) { user ->
                        val isSelected = selectedList.any { it.uid == user.uid }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selectedList.removeAll { it.uid == user.uid }
                                        } else if (selectedList.size < MAX_COLLAB_PARTICIPANTS) {
                                            selectedList.add(
                                                CollabParticipant(
                                                    uid = user.uid,
                                                    username = user.username,
                                                    displayName = user.displayName,
                                                    avatarUrl = user.avatarUrl,
                                                    verified = user.verified,
                                                    verifiedTone = user.verifiedTone
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                                        size = 38.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = user.displayName.ifBlank { user.username },
                                                color = colors.textMain,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.verified) {
                                                VerifiedBadge(size = 12.dp, tone = user.verifiedTone)
                                            }
                                        }
                                        Text(
                                            text = "@${user.username}",
                                            color = colors.textMuted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isSelected) Lucide.Check else Lucide.Plus,
                                    contentDescription = if (isSelected) "Selected" else "Invite",
                                    tint = if (isSelected) colors.primary else colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = colors.glassBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet showing all co-authors on a shared Post or Flash with instant profile navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsDetailSheet(
    creatorUid: String,
    creatorDisplayName: String,
    creatorUsername: String,
    creatorAvatarUrl: String,
    creatorVerified: Boolean,
    creatorVerifiedTone: String?,
    collaborators: List<CollabParticipant>,
    isCurrentUserCollaborator: Boolean,
    onStopSharing: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "In This Collaboration",
                color = colors.textMain,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)

            // Primary Creator row
            CollaboratorProfileRow(
                displayName = creatorDisplayName,
                username = creatorUsername,
                avatarUrl = creatorAvatarUrl,
                verified = creatorVerified,
                verifiedTone = creatorVerifiedTone,
                roleLabel = "Creator",
                onClick = {
                    onDismiss()
                    onAuthorClick(creatorUid)
                }
            )

            collaborators.forEach { collab ->
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colors.glassBorder,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                CollaboratorProfileRow(
                    displayName = collab.displayName.ifBlank { collab.username },
                    username = collab.username,
                    avatarUrl = collab.avatarUrl,
                    verified = collab.verified,
                    verifiedTone = collab.verifiedTone,
                    roleLabel = "Collaborator",
                    onClick = {
                        onDismiss()
                        onAuthorClick(collab.uid)
                    }
                )
            }

            if (isCurrentUserCollaborator) {
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onStopSharing()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Lucide.UserMinus, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Stop sharing",
                        color = colors.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun CollaboratorProfileRow(
    displayName: String,
    username: String,
    avatarUrl: String,
    verified: Boolean,
    verifiedTone: String?,
    roleLabel: String,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            IosAvatarView(name = displayName, avatarUrl = avatarUrl, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = displayName.ifBlank { username },
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (verified) {
                        VerifiedBadge(size = 12.dp, tone = verifiedTone)
                    }
                }
                Text(
                    text = "@${username.ifBlank { "creator" }} • $roleLabel",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
    }
}
