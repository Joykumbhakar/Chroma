package com.chroma.studio.chat.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.ui.components.OnlineStatusBadge
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.components.rememberOscellionMediaPicker
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Link2
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Users

/**
 * Available Contact item for Group Creation selection.
 */
data class GroupContact(
    val id: String,
    val name: String,
    val role: String,
    val avatar: String,
    val status: String = "Online",
    val isVerified: Boolean = false,
    val verifiedTone: String? = null
)

/**
 * Complete group configuration returned from the onboarding flow.
 */
data class GroupCreationConfig(
    val groupName: String,
    val groupAvatar: String,
    val members: List<GroupMember>,
    val groupDescription: String,
    val onlyAdminsCanPost: Boolean,
    val coAdminIds: List<String> = emptyList(),
    val disappearingHours: Int = 0,
    val inviteCode: String? = null,
    val isPublicGroup: Boolean = false,
    val editInfoAdminsOnly: Boolean = true,
    val welcomeMessage: String = ""
)

enum class GroupCreationStep {
    SELECT_PARTICIPANTS,
    GROUP_DETAILS
}

private enum class ParticipantFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    VERIFIED("Verified")
}

private const val MAX_GROUP_PARTICIPANTS = 50
private val DISAPPEARING_OPTIONS_HOURS = listOf(0, 24, 168, 2160)

internal fun generateGroupInviteCode(seed: Long = System.nanoTime()): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val rng = java.util.Random(seed)
    return buildString(8) {
        repeat(8) { append(alphabet[rng.nextInt(alphabet.length)]) }
    }
}

internal fun disappearingTimerLabel(hours: Int): String = when (hours) {
    24 -> "24h"
    168 -> "7d"
    2160 -> "90d"
    else -> "Off"
}

internal fun buildGroupInviteUrl(inviteCode: String): String =
    "https://oscellion.vercel.app/join/$inviteCode"

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
 * Flagship Apple HIG Multi-Step Group Creation Onboarding Modal.
 * Step 1: Select & filter participants with live presence, verification badges & horizontal strip.
 * Step 2: Configure Group Identity, Photo/Icon Studio, Co-Admins, Privacy, Disappearing Timer & Invite Link.
 */
@Composable
fun CreateGroupModal(
    currentUser: User,
    availableContacts: List<GroupContact> = emptyList(),
    onDismiss: () -> Unit,
    onDirectMessage: ((GroupContact) -> Unit)? = null,
    onCreateGroup: (GroupCreationConfig) -> Unit
) {
    val colors = LocalChatColors.current

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
            GroupCreationContent(
                currentUser = currentUser,
                availableContacts = availableContacts,
                onDismiss = onDismiss,
                onDirectMessage = onDirectMessage,
                onCreateGroup = onCreateGroup
            )
        }
    }
}

@Composable
fun GroupCreationContent(
    currentUser: User,
    availableContacts: List<GroupContact> = emptyList(),
    onDismiss: () -> Unit,
    onDirectMessage: ((GroupContact) -> Unit)? = null,
    onCreateGroup: (GroupCreationConfig) -> Unit
) {
    val colors = LocalChatColors.current
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableStateOf(GroupCreationStep.SELECT_PARTICIPANTS) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ParticipantFilter.ALL) }
    val selectedContacts = remember { mutableStateListOf<GroupContact>() }
    val coAdminIds = remember { mutableStateListOf<String>() }

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var welcomeMessage by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(defaultGroupAvatarPresets.first()) }
    var disappearingHours by remember { mutableIntStateOf(0) }
    var onlyAdminsCanPost by remember { mutableStateOf(false) }
    var editInfoAdminsOnly by remember { mutableStateOf(true) }
    var isPublicGroup by remember { mutableStateOf(false) }
    var inviteLinkEnabled by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf(generateGroupInviteCode()) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
    var defaultSuggestedContacts by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val hasUnsavedChanges = selectedContacts.isNotEmpty() ||
        groupName.isNotBlank() ||
        groupDescription.isNotBlank() ||
        welcomeMessage.isNotBlank() ||
        selectedAvatar != defaultGroupAvatarPresets.first()

    val requestCloseOrBack: () -> Unit = {
        SoundManager.playReaction()
        when {
            currentStep == GroupCreationStep.GROUP_DETAILS -> {
                currentStep = GroupCreationStep.SELECT_PARTICIPANTS
            }
            hasUnsavedChanges -> showDiscardDialog = true
            else -> onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val suggested = discoverRepo.getSuggestedCreators(limit = 40)
            defaultSuggestedContacts = suggested.filter { it.uid != currentUser.id }.map { prof ->
                val liveOnline = prof.online || OnlineStatusManager.isUserOnline(prof.uid)
                GroupContact(
                    id = prof.uid,
                    name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                    role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                    avatar = prof.avatarUrl,
                    status = if (liveOnline) "Online" else "Active on Oscellion",
                    isVerified = prof.verified,
                    verifiedTone = prof.verifiedTone
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
                    val liveOnline = prof.online || OnlineStatusManager.isUserOnline(prof.uid)
                    GroupContact(
                        id = prof.uid,
                        name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                        role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                        avatar = prof.avatarUrl,
                        status = if (liveOnline) "Online" else "Active on Oscellion",
                        isVerified = prof.verified,
                        verifiedTone = prof.verifiedTone
                    )
                }
            } catch (_: Exception) {}
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    val allBaseContacts = remember(availableContacts, defaultSuggestedContacts) {
        (availableContacts + defaultSuggestedContacts).distinctBy { it.id }
    }

    val filteredContacts = remember(searchQuery, allBaseContacts, searchResults, activeFilter) {
        val baseList = if (searchQuery.isBlank()) {
            allBaseContacts
        } else if (searchResults.isNotEmpty()) {
            val q = searchQuery.trim().lowercase()
            (searchResults + allBaseContacts.filter {
                it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
            }).distinctBy { it.id }
        } else {
            val q = searchQuery.trim().lowercase()
            allBaseContacts.filter {
                it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
            }
        }

        when (activeFilter) {
            ParticipantFilter.ALL -> baseList
            ParticipantFilter.ONLINE -> baseList.filter { it.status == "Online" || OnlineStatusManager.isUserOnline(it.id) }
            ParticipantFilter.VERIFIED -> baseList.filter { it.isVerified }
        }
    }

    if (showDiscardDialog) {
        IosAlertDialog(
            title = "Discard New Group?",
            message = "Your selected members and group settings will not be saved.",
            actions = listOf(
                IosAlertAction(
                    title = "Keep Editing",
                    isCancel = true,
                    onClick = { showDiscardDialog = false }
                ),
                IosAlertAction(
                    title = "Discard",
                    isDestructive = true,
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    }
                )
            ),
            onDismiss = { showDiscardDialog = false }
        )
    }

    BackHandler(onBack = requestCloseOrBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
                // -- Apple HIG Top Navigation Bar --------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.appBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Cancel" else "Back",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = requestCloseOrBack
                            )
                            .padding(vertical = 4.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Add Participants" else "New Group",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark
                        )
                        Text(
                            text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                                "${selectedContacts.size}/$MAX_GROUP_PARTICIPANTS selected · Step 1 of 2"
                            } else {
                                "${selectedContacts.size + 1} members · Step 2 of 2"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted
                        )
                    }

                    val isActionEnabled = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                        selectedContacts.isNotEmpty()
                    } else {
                        groupName.isNotBlank()
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS && selectedContacts.size == 1 && onDirectMessage != null) {
                            Text(
                                text = "Chat",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.iosBlue,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        onDirectMessage.invoke(selectedContacts.first())
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }

                        Text(
                            text = if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) "Next" else "Create",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActionEnabled) colors.iosBlue else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clickable(
                                    enabled = isActionEnabled,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playReaction()
                                    if (currentStep == GroupCreationStep.SELECT_PARTICIPANTS) {
                                        currentStep = GroupCreationStep.GROUP_DETAILS
                                    } else {
                                        val selectedIdSet = selectedContacts.map { it.id }.toSet()
                                        val cleanCoAdmins = coAdminIds.filter { it in selectedIdSet }.distinct()
                                        val groupMembers = listOf(
                                            GroupMember(
                                                id = currentUser.id,
                                                name = "${currentUser.name} (You)",
                                                role = "Group Admin",
                                                avatar = currentUser.avatar
                                            )
                                        ) + selectedContacts.map { contact ->
                                            GroupMember(
                                                id = contact.id,
                                                name = contact.name,
                                                role = if (contact.id in cleanCoAdmins) "Group Admin" else contact.role,
                                                avatar = contact.avatar
                                            )
                                        }
                                        onCreateGroup(
                                            GroupCreationConfig(
                                                groupName = groupName.trim(),
                                                groupAvatar = selectedAvatar,
                                                members = groupMembers,
                                                groupDescription = groupDescription.trim(),
                                                onlyAdminsCanPost = onlyAdminsCanPost,
                                                coAdminIds = cleanCoAdmins,
                                                disappearingHours = disappearingHours,
                                                inviteCode = if (inviteLinkEnabled) inviteCode else null,
                                                isPublicGroup = isPublicGroup,
                                                editInfoAdminsOnly = editInfoAdminsOnly,
                                                welcomeMessage = welcomeMessage.trim()
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Subtle 2-segment Step Progress Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.iosBlue)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (currentStep == GroupCreationStep.GROUP_DETAILS) colors.iosBlue
                                else colors.separator
                            )
                    )
                }

                HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

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
                                isSearching = isSearching,
                                activeFilter = activeFilter,
                                onFilterChanged = { activeFilter = it },
                                selectedContacts = selectedContacts,
                                filteredContacts = filteredContacts,
                                onToggleContact = { contact ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playReaction()
                                    if (selectedContacts.any { it.id == contact.id }) {
                                        selectedContacts.removeAll { it.id == contact.id }
                                        coAdminIds.removeAll { it == contact.id }
                                    } else if (selectedContacts.size < MAX_GROUP_PARTICIPANTS) {
                                        selectedContacts.add(contact)
                                    } else {
                                        ToastManager.showToast("Maximum $MAX_GROUP_PARTICIPANTS participants allowed")
                                    }
                                },
                                onSelectAllVisible = {
                                    val availableSlots = MAX_GROUP_PARTICIPANTS - selectedContacts.size
                                    if (availableSlots > 0) {
                                        val existing = selectedContacts.map { it.id }.toSet()
                                        val toAdd = filteredContacts.filter { it.id !in existing }.take(availableSlots)
                                        selectedContacts.addAll(toAdd)
                                        SoundManager.playReaction()
                                    }
                                },
                                onClearSelection = {
                                    selectedContacts.clear()
                                    coAdminIds.clear()
                                    SoundManager.playReaction()
                                },
                                onDirectMessage = onDirectMessage
                            )
                        }

                        GroupCreationStep.GROUP_DETAILS -> {
                            StepGroupDetails(
                                currentUser = currentUser,
                                groupName = groupName,
                                onGroupNameChanged = { if (it.length <= 35) groupName = it },
                                groupDescription = groupDescription,
                                onGroupDescriptionChanged = { if (it.length <= 150) groupDescription = it },
                                welcomeMessage = welcomeMessage,
                                onWelcomeMessageChanged = { if (it.length <= 120) welcomeMessage = it },
                                selectedAvatar = selectedAvatar,
                                onAvatarSelected = { selectedAvatar = it },
                                selectedContacts = selectedContacts,
                                coAdminIds = coAdminIds,
                                onToggleCoAdmin = { contactId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playReaction()
                                    if (coAdminIds.contains(contactId)) coAdminIds.remove(contactId)
                                    else coAdminIds.add(contactId)
                                },
                                onRemoveContact = { contact ->
                                    SoundManager.playReaction()
                                    selectedContacts.remove(contact)
                                    coAdminIds.removeAll { it == contact.id }
                                },
                                disappearingHours = disappearingHours,
                                onDisappearingHoursSelected = { hours ->
                                    SoundManager.playReaction()
                                    disappearingHours = hours
                                },
                                onlyAdminsCanPost = onlyAdminsCanPost,
                                onToggleOnlyAdminsCanPost = {
                                    SoundManager.playReaction()
                                    onlyAdminsCanPost = !onlyAdminsCanPost
                                },
                                editInfoAdminsOnly = editInfoAdminsOnly,
                                onToggleEditInfoAdminsOnly = {
                                    SoundManager.playReaction()
                                    editInfoAdminsOnly = !editInfoAdminsOnly
                                },
                                isPublicGroup = isPublicGroup,
                                onSetPublicGroup = { isPublic ->
                                    SoundManager.playReaction()
                                    isPublicGroup = isPublic
                                },
                                inviteLinkEnabled = inviteLinkEnabled,
                                onToggleInviteLink = {
                                    SoundManager.playReaction()
                                    inviteLinkEnabled = !inviteLinkEnabled
                                },
                                inviteCode = inviteCode,
                                onRegenerateInviteCode = {
                                    SoundManager.playReaction()
                                    inviteCode = generateGroupInviteCode()
                                    ToastManager.showToast("Invite link refreshed")
                                }
                            )
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
    isSearching: Boolean,
    activeFilter: ParticipantFilter,
    onFilterChanged: (ParticipantFilter) -> Unit,
    selectedContacts: List<GroupContact>,
    filteredContacts: List<GroupContact>,
    onToggleContact: (GroupContact) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearSelection: () -> Unit,
    onDirectMessage: ((GroupContact) -> Unit)? = null
) {
    val colors = LocalChatColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Search Input
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                                    "Search people by name or @username",
                                    fontSize = 14.5.sp,
                                    color = colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.8.dp,
                            color = colors.iosBlue
                        )
                    } else if (searchQuery.isNotEmpty()) {
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

        // Segmented Filter Bar (Flat non-pill tabs)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceSecondary)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ParticipantFilter.entries.forEach { filter ->
                    val selected = activeFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) colors.cardBg else Color.Transparent)
                            .clickable {
                                SoundManager.playReaction()
                                onFilterChanged(filter)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) colors.iosBlue else colors.textMuted
                        )
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
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedContacts, key = { it.id }) { contact ->
                            SelectedContactChip(
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

        // Contacts Section Header with Quick Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTACTS (${filteredContacts.size})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedContacts.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF3B30),
                            modifier = Modifier.clickable { onClearSelection() }
                        )
                    }
                    if (filteredContacts.isNotEmpty() && selectedContacts.size < MAX_GROUP_PARTICIPANTS) {
                        Text(
                            text = "Select All",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.iosBlue,
                            modifier = Modifier.clickable { onSelectAllVisible() }
                        )
                    }
                }
            }
        }

        if (filteredContacts.isEmpty() && !isSearching) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Users,
                        contentDescription = null,
                        tint = colors.textMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No people matching \"$searchQuery\"" else "No contacts in this filter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                }
            }
        }

        items(filteredContacts, key = { it.id }) { contact ->
            val isSelected = selectedContacts.any { it.id == contact.id }
            val liveOnline = contact.status == "Online" || OnlineStatusManager.isUserOnline(contact.id)

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
                LiquidGlassCheckbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    size = 22.dp,
                    checkedColor = colors.iosBlue,
                    uncheckedColor = colors.separatorOpaque
                )

                com.chroma.studio.ui.components.IosAvatarView(
                    name = contact.name,
                    avatarUrl = contact.avatar,
                    size = 44.dp,
                    isOnline = liveOnline
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (contact.isVerified) {
                            VerifiedBadge(
                                size = 14.dp,
                                tone = contact.verifiedTone
                            )
                        }
                    }
                    Text(
                        text = contact.role,
                        fontSize = 13.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onDirectMessage != null) {
                    Text(
                        text = "Chat",
                        color = colors.iosBlue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.iosBlue.copy(alpha = 0.10f))
                            .clickable {
                                SoundManager.playReaction()
                                onDirectMessage(contact)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                } else {
                    OnlineStatusBadge(
                        isOnline = liveOnline,
                        statusText = if (liveOnline) "Online" else contact.status,
                        compact = true
                    )
                }
            }

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 88.dp)
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
    groupDescription: String,
    onGroupDescriptionChanged: (String) -> Unit,
    welcomeMessage: String,
    onWelcomeMessageChanged: (String) -> Unit,
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    selectedContacts: List<GroupContact>,
    coAdminIds: List<String>,
    onToggleCoAdmin: (String) -> Unit,
    onRemoveContact: (GroupContact) -> Unit,
    disappearingHours: Int,
    onDisappearingHoursSelected: (Int) -> Unit,
    onlyAdminsCanPost: Boolean,
    onToggleOnlyAdminsCanPost: () -> Unit,
    editInfoAdminsOnly: Boolean,
    onToggleEditInfoAdminsOnly: () -> Unit,
    isPublicGroup: Boolean,
    onSetPublicGroup: (Boolean) -> Unit,
    inviteLinkEnabled: Boolean,
    onToggleInviteLink: () -> Unit,
    inviteCode: String,
    onRegenerateInviteCode: () -> Unit
) {
    val colors = LocalChatColors.current
    val clipboard = LocalClipboardManager.current
    var isIconStudioOpen by remember { mutableStateOf(false) }

    val devicePhotoPicker = rememberOscellionMediaPicker(
        title = "Group Photo",
        mediaFilter = GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { pickedUri ->
        SoundManager.playSend()
        onAvatarSelected(pickedUri.toString())
    }

    val quickEmojis = listOf("🚀", "🔥", "🎨", "✨", "🎮", "💻", "🎧", "☕", "🏆", "🌿")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Borderless Identity Section (Avatar + Photo Actions + Fields)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, colors.iosBlue.copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                isIconStudioOpen = true
                                SoundManager.playReaction()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        com.chroma.studio.ui.components.IosAvatarView(
                            name = groupName.ifBlank { "Group" },
                            avatarUrl = selectedAvatar,
                            size = 82.dp,
                            isGroup = true
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.iosBlue)
                            .border(1.5.dp, colors.appBg, RoundedCornerShape(7.dp))
                            .clickable {
                                SoundManager.playReaction()
                                devicePhotoPicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCameraIcon(tint = Color.White, size = 13.dp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceSecondary)
                            .clickable {
                                SoundManager.playReaction()
                                devicePhotoPicker.launch("image/*")
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Lucide.ImagePlus, contentDescription = null, tint = colors.iosBlue, modifier = Modifier.size(14.dp))
                        Text("Gallery Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textDark)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iosBlue.copy(alpha = 0.12f))
                            .clickable {
                                isIconStudioOpen = true
                                SoundManager.playReaction()
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Lucide.Palette, contentDescription = null, tint = colors.iosBlue, modifier = Modifier.size(14.dp))
                        Text("Icon Studio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.iosBlue)
                    }
                }

                // Quick Preset Avatars Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(defaultGroupAvatarPresets) { avatarUrl ->
                        val isSelected = avatarUrl == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) colors.iosBlue else Color.Transparent,
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

                // Group Name Input
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
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        ),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (groupName.isEmpty()) {
                                Text("Group Name (Required)", fontSize = 15.5.sp, color = colors.textMuted)
                            }
                            innerTextField()
                        }
                    )
                    Text("${groupName.length}/35", fontSize = 11.5.sp, color = colors.textMuted)
                }

                // Quick Emojis Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
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

                // Group Description Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BasicTextField(
                        value = groupDescription,
                        onValueChange = onGroupDescriptionChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = TextStyle(fontSize = 14.sp, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (groupDescription.isEmpty()) {
                                Text("Group Description (Optional)", fontSize = 14.sp, color = colors.textMuted)
                            }
                            innerTextField()
                        }
                    )
                    Text("${groupDescription.length}/150", fontSize = 11.5.sp, color = colors.textMuted)
                }

                // Custom Welcome Note Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = welcomeMessage,
                        onValueChange = onWelcomeMessageChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (welcomeMessage.isEmpty()) {
                                Text("Welcome Note (Optional intro message)", fontSize = 14.sp, color = colors.textMuted)
                            }
                            innerTextField()
                        }
                    )
                    Text("${welcomeMessage.length}/120", fontSize = 11.5.sp, color = colors.textMuted)
                }
            }
        }

        // 2. Group Privacy & Access Mode (Borderless surface with hairlines)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIVACY & PERMISSIONS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                ) {
                    // Private vs Public Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPublicGroup) Lucide.Globe else Lucide.Lock,
                                contentDescription = null,
                                tint = colors.iosBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Group Access",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark
                                )
                                Text(
                                    text = if (isPublicGroup) "Anyone with link can join" else "Invite-only group",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.appBg)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(false to "Private", true to "Public").forEach { (pub, label) ->
                                val selected = isPublicGroup == pub
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) Color.White else colors.textMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) colors.iosBlue else Color.Transparent)
                                        .clickable { onSetPublicGroup(pub) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Disappearing Messages Multi-Timer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.Timer, contentDescription = null, tint = colors.iosBlue, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Disappearing Messages",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark
                                )
                                Text(
                                    text = if (disappearingHours > 0) "Auto-expires after ${disappearingTimerLabel(disappearingHours)}" else "Keep message history",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.appBg)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DISAPPEARING_OPTIONS_HOURS.forEach { hours ->
                                val selected = disappearingHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) colors.iosBlue else Color.Transparent)
                                        .clickable { onDisappearingHoursSelected(hours) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = disappearingTimerLabel(hours),
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) Color.White else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Announcement Channel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleOnlyAdminsCanPost() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Lucide.ShieldCheck, contentDescription = null, tint = colors.iosBlue, modifier = Modifier.size(16.dp))
                            Column {
                                Text(
                                    text = "Announcement Channel",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark
                                )
                                Text(
                                    text = if (onlyAdminsCanPost) "Only group admins can send messages" else "All members can send messages",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                        AppleSwitch(
                            checked = onlyAdminsCanPost,
                            onCheckedChange = { onToggleOnlyAdminsCanPost() }
                        )
                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Edit Info Admins Only
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleEditInfoAdminsOnly() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restrict Info Edits",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textDark
                            )
                            Text(
                                text = if (editInfoAdminsOnly) "Only admins can edit group name & photo" else "Any member can edit group info",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        AppleSwitch(
                            checked = editInfoAdminsOnly,
                            onCheckedChange = { onToggleEditInfoAdminsOnly() }
                        )
                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Invite Link Toggle + Live Copyable Link
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleInviteLink() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Lucide.Link2, contentDescription = null, tint = colors.iosBlue, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = "Group Invite Link",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textDark
                                    )
                                    Text(
                                        text = if (inviteLinkEnabled) "Share link to let friends join directly" else "Disabled",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            AppleSwitch(
                                checked = inviteLinkEnabled,
                                onCheckedChange = { onToggleInviteLink() }
                            )
                        }

                        if (inviteLinkEnabled) {
                            val inviteUrl = buildGroupInviteUrl(inviteCode)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.appBg)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = inviteUrl,
                                    fontSize = 12.sp,
                                    color = colors.iosBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Lucide.RefreshCw,
                                    contentDescription = "New code",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { onRegenerateInviteCode() }
                                )
                                Icon(
                                    imageVector = Lucide.Copy,
                                    contentDescription = "Copy invite link",
                                    tint = colors.iosBlue,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable {
                                            clipboard.setText(AnnotatedString(inviteUrl))
                                            SoundManager.playReaction()
                                            ToastManager.showToast("Invite link copied")
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Members & Co-Admin Assignment
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMBERS (${selectedContacts.size + 1})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${coAdminIds.size} co-admin${if (coAdminIds.size == 1) "" else "s"}",
                    fontSize = 11.5.sp,
                    color = colors.iosBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Creator Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceSecondary)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.chroma.studio.ui.components.IosAvatarView(
                    name = currentUser.name,
                    avatarUrl = currentUser.avatar,
                    size = 38.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentUser.name} (You)",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                    Text(
                        text = "Group Creator",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Text(
                    text = "Owner",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.iosBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.iosBlueSubtle)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Added Members with Co-Admin Toggle
        items(selectedContacts, key = { it.id }) { contact ->
            val isCoAdmin = contact.id in coAdminIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.chroma.studio.ui.components.IosAvatarView(
                    name = contact.name,
                    avatarUrl = contact.avatar,
                    size = 38.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (contact.isVerified) {
                            VerifiedBadge(size = 13.dp, tone = contact.verifiedTone)
                        }
                    }
                    Text(
                        text = if (isCoAdmin) "Co-Admin" else contact.role,
                        fontSize = 12.sp,
                        color = if (isCoAdmin) colors.iosBlue else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isCoAdmin) "Admin" else "Make Admin",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCoAdmin) Color.White else colors.iosBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCoAdmin) colors.iosBlue else colors.iosBlueSubtle)
                        .clickable { onToggleCoAdmin(contact.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onRemoveContact(contact) },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCloseX(tint = colors.textMuted, size = 14.dp)
                }
            }
            HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
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

// -- Selected Contact Compact Tile on Shelf ----------------------------

@Composable
private fun SelectedContactChip(
    contact: GroupContact,
    onRemove: () -> Unit
) {
    val colors = LocalChatColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(54.dp)
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            com.chroma.studio.ui.components.IosAvatarView(
                name = contact.name,
                avatarUrl = contact.avatar,
                size = 46.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, colors.iosBlue, CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(colors.cardBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                PhosphorCloseX(tint = colors.textMuted, size = 10.dp)
            }
        }

        Text(
            text = contact.name.split(" ").firstOrNull() ?: contact.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
