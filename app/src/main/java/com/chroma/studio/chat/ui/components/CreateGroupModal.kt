package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.chat.data.GroupMember
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.data.drawableUri
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.VerifiedBadge

/**
 * Available Contact item for Group Creation selection.
 */
data class GroupContact(
    val id: String,
    val name: String,
    val role: String,
    val avatar: String,
    val status: String = "Online",
    val isVerified: Boolean = false
)

enum class GroupCreationStep {
    SELECT_PARTICIPANTS,
    GROUP_DETAILS
}

val defaultGroupAvatarPresets = listOf(
    drawableUri(R.drawable.ic_avatar_deep_purple),
    drawableUri(R.drawable.ic_avatar_corporate_navy),
    drawableUri(R.drawable.ic_avatar_emerald_focus),
    drawableUri(R.drawable.ic_avatar_gold_standard),
    drawableUri(R.drawable.ic_avatar_sapphire_night),
    drawableUri(R.drawable.ic_avatar_royal_blue),
    drawableUri(R.drawable.ic_avatar_ocean_depth),
    drawableUri(R.drawable.ic_avatar_executive_forest),
    drawableUri(R.drawable.ic_avatar_teal_pro),
    drawableUri(R.drawable.ic_avatar_burgundy_board),
    drawableUri(R.drawable.ic_avatar_midnight),
    drawableUri(R.drawable.ic_avatar_charcoal),
    drawableUri(R.drawable.ic_avatar_classic_mac)
)

/**
 * Flagship Apple iOS Multi-Step Group Creation Modal.
 * Step 1: Select & search participants with horizontal shelf.
 * Step 2: Set Group Subject, Avatar, Description, and Permissions.
 */
@Composable
fun CreateGroupModal(
    currentUser: User,
    availableContacts: List<GroupContact> = emptyList(),
    onDismiss: () -> Unit,
    onDirectMessage: ((GroupContact) -> Unit)? = null,
    onCreateGroup: (groupName: String, groupAvatar: String, members: List<GroupMember>) -> Unit
) {
    val colors = LocalChatColors.current

    var currentStep by remember { mutableStateOf(GroupCreationStep.SELECT_PARTICIPANTS) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedContacts = remember { mutableStateListOf<GroupContact>() }

    var groupName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(defaultGroupAvatarPresets.first()) }
    var disappearingMessagesEnabled by remember { mutableStateOf(false) }
    var onlyAdminsCanPost by remember { mutableStateOf(false) }

    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
    var defaultSuggestedContacts by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val suggested = discoverRepo.getSuggestedCreators(limit = 40)
            defaultSuggestedContacts = suggested.filter { it.uid != currentUser.id }.map { prof ->
                GroupContact(
                    id = prof.uid,
                    name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                    role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                    avatar = prof.avatarUrl,
                    status = if (prof.online) "Online" else "Active on Oscellion"
                )
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            isSearching = true
            try {
                val found = discoverRepo.searchUsers(q)
                searchResults = found.filter { it.uid != currentUser.id }.map { prof ->
                    GroupContact(
                        id = prof.uid,
                        name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                        role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                        avatar = prof.avatarUrl,
                        status = if (prof.online) "Online" else "Active on Oscellion"
                    )
                }
            } catch (_: Exception) {}
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    val allBaseContacts = remember(availableContacts, defaultSuggestedContacts) {
        (availableContacts + defaultSuggestedContacts).distinctBy { it.id }
    }

    val filteredContacts = remember(searchQuery, allBaseContacts, searchResults) {
        if (searchQuery.isBlank()) allBaseContacts
        else if (searchResults.isNotEmpty()) {
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
                // -- iOS Navigation Bar -----------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.appBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Action (Cancel or Back)
                    Text(
                        text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Cancel" else "Back",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                if (currentStep == GroupCreationStep.GROUP_DETAILS) {
                                    currentStep = GroupCreationStep.SELECT_PARTICIPANTS
                                } else {
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 4.dp)
                    )

                    // Navigation Center Title & Subtitle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Add Participants" else "New Group",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark
                        )
                        if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                            Text(
                                text = "${selectedContacts.size}/50 selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textMuted
                            )
                        }
                    }

                    // Right Action (Next or Create)
                    val isActionEnabled = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                        selectedContacts.isNotEmpty()
                    } else {
                        groupName.isNotBlank()
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS && selectedContacts.size == 1 && onDirectMessage != null) {
                            Text(
                                text = "Direct Chat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.iosBlue,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onDirectMessage.invoke(selectedContacts.first())
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Text(
                            text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Next" else "Create",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActionEnabled) colors.iosBlue else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clickable(
                                    enabled = isActionEnabled,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                                        currentStep = GroupCreationStep.GROUP_DETAILS
                                    } else {
                                        val groupMembers = listOf(
                                            GroupMember(
                                                id = currentUser.id,
                                                name = "${currentUser.name} (You)",
                                                role = "Group Admin",
                                                avatar = currentUser.avatar
                                            )
                                        ) + selectedContacts.map {
                                            GroupMember(
                                                id = it.id,
                                                name = it.name,
                                                role = it.role,
                                                avatar = it.avatar
                                            )
                                        }
                                        onCreateGroup(groupName.trim(), selectedAvatar, groupMembers)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                // -- Step Content Animation -------------------------------
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState == GroupCreationStep.GROUP_DETAILS) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "GroupCreationStepTransition",
                    modifier = Modifier.fillMaxSize()
                ) { step ->
                    when (step) {
                        GroupCreationStep.SELECT_PARTICIPANTS -> {
                            StepSelectParticipants(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { searchQuery = it },
                                selectedContacts = selectedContacts,
                                filteredContacts = filteredContacts,
                                onToggleContact = { contact ->
                                    SoundManager.playReaction()
                                    if (selectedContacts.any { it.id == contact.id }) {
                                        selectedContacts.removeAll { it.id == contact.id }
                                    } else {
                                        selectedContacts.add(contact)
                                    }
                                },
                                onDirectMessage = onDirectMessage
                            )
                        }

                        GroupCreationStep.GROUP_DETAILS -> {
                            StepGroupDetails(
                                currentUser = currentUser,
                                groupName = groupName,
                                onGroupNameChanged = { if (it.length <= 35) groupName = it },
                                selectedAvatar = selectedAvatar,
                                onAvatarSelected = { selectedAvatar = it },
                                selectedContacts = selectedContacts,
                                onRemoveContact = { contact ->
                                    SoundManager.playReaction()
                                    selectedContacts.remove(contact)
                                },
                                disappearingMessagesEnabled = disappearingMessagesEnabled,
                                onToggleDisappearingMessages = { disappearingMessagesEnabled = !disappearingMessagesEnabled },
                                onlyAdminsCanPost = onlyAdminsCanPost,
                                onToggleOnlyAdminsCanPost = { onlyAdminsCanPost = !onlyAdminsCanPost }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -- STEP 1: Select Participants Screen ------------------------------

@Composable
private fun StepSelectParticipants(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedContacts: List<GroupContact>,
    filteredContacts: List<GroupContact>,
    onToggleContact: (GroupContact) -> Unit,
    onDirectMessage: ((GroupContact) -> Unit)? = null
) {
    val colors = LocalChatColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // iOS Search Field
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhosphorMagnifyingGlass(tint = colors.textMuted, size = 16.dp)
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
                                    "Search by name or role",
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
                                .size(24.dp)
                                .clickable { onSearchQueryChanged("") },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
                        }
                    }
                }
            }
        }

        // Horizontal Selected Participants Shelf
        if (selectedContacts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedContacts, key = { it.id }) { contact ->
                            SelectedContactPill(
                                contact = contact,
                                onRemove = { onToggleContact(contact) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        color = colors.separator,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        // Section Title: ALL CONTACTS
        item {
            Text(
                text = "CONTACTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
            )
        }

        // Contact Rows
        items(filteredContacts, key = { it.id }) { contact ->
            val isSelected = selectedContacts.any { it.id == contact.id }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleContact(contact) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selection Checkbox (Thin Box & Extended Tick)
                LiquidGlassCheckbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    size = 22.dp,
                    checkedColor = colors.iosBlue,
                    uncheckedColor = colors.separatorOpaque
                )

                // Avatar
                com.chroma.studio.ui.components.IosAvatarView(
                    name = contact.name,
                    avatarUrl = contact.avatar,
                    size = 44.dp
                )

                // Name & Role Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((-2).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
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
                        lineHeight = 15.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onDirectMessage != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.iosBlue.copy(alpha = 0.12f))
                            .clickable {
                                SoundManager.playReaction()
                                onDirectMessage(contact)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chat",
                            color = colors.iosBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = contact.status,
                        fontSize = 12.5.sp,
                        color = if (contact.status == "Online") colors.success else colors.textMuted
                    )
                }
            }

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 90.dp)
            )
        }
    }
}

// -- STEP 2: Group Details Screen ------------------------------------

@Composable
private fun StepGroupDetails(
    currentUser: User,
    groupName: String,
    onGroupNameChanged: (String) -> Unit,
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    selectedContacts: List<GroupContact>,
    onRemoveContact: (GroupContact) -> Unit,
    disappearingMessagesEnabled: Boolean,
    onToggleDisappearingMessages: () -> Unit,
    onlyAdminsCanPost: Boolean,
    onToggleOnlyAdminsCanPost: () -> Unit
) {
    val colors = LocalChatColors.current
    var isIconStudioOpen by remember { mutableStateOf(false) }

    val quickEmojis = listOf("??", "??", "?", "??", "??", "??", "??", "??", "??", "???")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group Icon & Name Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBg)
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Avatar Picker with Camera Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, colors.iosBlue, CircleShape)
                        .clickable {
                            isIconStudioOpen = true
                            SoundManager.playReaction()
                        }
                ) {
                    com.chroma.studio.ui.components.IosAvatarView(
                        name = "Group",
                        avatarUrl = selectedAvatar,
                        size = 64.dp,
                        isGroup = true
                    )

                    PhosphorCameraIcon(
                        tint = colors.iosBlue,
                        size = 18.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.iosBlue.copy(alpha = 0.12f))
                            .clickable {
                                isIconStudioOpen = true
                                SoundManager.playReaction()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { LucidePalette(tint = colors.iosBlue, size = 14.dp); Text(text = "Group Icon Studio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.iosBlue) }
                    }
                }

                // Avatar Presets Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(defaultGroupAvatarPresets) { avatarUrl ->
                        val isSelected = avatarUrl == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) colors.iosBlue else colors.separatorOpaque,
                                    shape = CircleShape
                                )
                                .clickable {
                                    SoundManager.playReaction()
                                    onAvatarSelected(avatarUrl)
                                }
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar Preset",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Inset Group Name TextField
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = groupName,
                        onValueChange = onGroupNameChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        ),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (groupName.isEmpty()) {
                                Text(
                                    "Group Name (Subject)",
                                    fontSize = 16.sp,
                                    color = colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    )

                    Text(
                        text = "${groupName.length}/35",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                // Quick Emoji Suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary)
                                .clickable {
                                    if (groupName.length + emoji.length <= 35) {
                                        onGroupNameChanged(groupName + emoji)
                                        SoundManager.playReaction()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Group Settings & Permissions (iOS Inset Grouped Table)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBg)
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            ) {
                // Disappearing Messages Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDisappearingMessages() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Disappearing Messages",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        )
                        Text(
                            text = if (disappearingMessagesEnabled) "Messages expire after 24 hours" else "Off",
                            fontSize = 12.5.sp,
                            color = colors.textMuted
                        )
                    }
                    AppleSwitch(
                        checked = disappearingMessagesEnabled,
                        onCheckedChange = { onToggleDisappearingMessages() }
                    )
                }

                HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                // Only Admins Can Post
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOnlyAdminsCanPost() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Announcement Channel",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        )
                        Text(
                            text = if (onlyAdminsCanPost) "Only group admins can send messages" else "All members can send messages",
                            fontSize = 12.5.sp,
                            color = colors.textMuted
                        )
                    }
                    AppleSwitch(
                        checked = onlyAdminsCanPost,
                        onCheckedChange = { onToggleOnlyAdminsCanPost() }
                    )
                }
            }
        }

        // Participants Summary Header
        item {
            Text(
                text = "PARTICIPANTS: ${selectedContacts.size + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp
            )
        }

        // Group Creator (You - Admin)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.chroma.studio.ui.components.IosAvatarView(
                    name = currentUser.name,
                    avatarUrl = currentUser.avatar,
                    size = 40.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentUser.name} (You)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                    Text(
                        text = "Group Creator",
                        fontSize = 12.5.sp,
                        color = colors.textMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(colors.iosBlueSubtle)
                        .border(0.5.dp, colors.iosBlue.copy(alpha = 0.25f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Admin",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.iosBlue
                    )
                }
            }
        }

        // Added Participants List
        items(selectedContacts, key = { it.id }) { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.chroma.studio.ui.components.IosAvatarView(
                    name = contact.name,
                    avatarUrl = contact.avatar,
                    size = 40.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        )
                        if (contact.isVerified) {
                            VerifiedBadge(size = 13.dp)
                        }
                    }
                    Text(
                        text = contact.role,
                        fontSize = 12.5.sp,
                        color = colors.textMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRemoveContact(contact) },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCloseX(tint = colors.textMuted, size = 14.dp)
                }
            }
        }
    }

    if (isIconStudioOpen) {
        GroupIconCreatorModal(
            currentAvatar = selectedAvatar,
            initialGroupName = groupName,
            onDismiss = { isIconStudioOpen = false },
            onApplyIcon = { newIcon ->
                onAvatarSelected(newIcon)
                isIconStudioOpen = false
            }
        )
    }
}

// -- Selected Contact Avatar Pill on Shelf ----------------------------

@Composable
private fun SelectedContactPill(
    contact: GroupContact,
    onRemove: () -> Unit
) {
    val colors = LocalChatColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(56.dp)
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            com.chroma.studio.ui.components.IosAvatarView(
                name = contact.name,
                avatarUrl = contact.avatar,
                size = 48.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, colors.iosBlue, CircleShape)
            )

            // Remove 'X' Badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
            }
        }

        Text(
            text = contact.name.split(" ").firstOrNull() ?: contact.name,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



