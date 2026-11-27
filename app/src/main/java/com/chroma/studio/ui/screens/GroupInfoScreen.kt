package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.VerifiedBadge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.ChatSettingsManager
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChatViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatViewModel: ChatViewModel,
    roomId: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val recentChats by chatViewModel.recentChats.collectAsState()
    val userProfiles by chatViewModel.userProfiles.collectAsState()
    var standaloneRoom by remember { mutableStateOf<com.chroma.studio.model.ChatRoom?>(null) }

    LaunchedEffect(roomId) {
        if (recentChats.none { it.id == roomId }) {
            val fetched = chatViewModel.fetchRoom(roomId)
            if (fetched != null) standaloneRoom = fetched
        }
    }

    val room = recentChats.find { it.id == roomId } ?: standaloneRoom
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    LaunchedEffect(room?.participants) {
        if (room != null && room.participants.isNotEmpty()) {
            chatViewModel.fetchProfilesForUsers(room.participants)
        }
    }

    LaunchedEffect(room) {
        if (room != null && !room.isGroup) {
            onBack()
        }
    }

    if (room == null || !room.isGroup) {
        Box(
            modifier = Modifier.fillMaxSize().background(colors.bg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(36.dp))
        }
        return
    }

    val isAdmin = room.admins.contains(currentUser.uid)
    val isLeft = !room.participants.contains(currentUser.uid)
    val isClosed = false

    val mutedChatIds by chatViewModel.mutedChatIds.collectAsState()
    val isMuted = mutedChatIds.contains(roomId)

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditDescDialog by remember { mutableStateOf(false) }
    var showLeaveGroupConfirm by remember { mutableStateOf(false) }
    var memberPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(room.name ?: "") }
    var newGroupDesc by remember { mutableStateOf(room.groupDescription ?: "") }

    LaunchedEffect(room.name) {
        newGroupName = room.name ?: ""
    }
    LaunchedEffect(room.groupDescription) {
        newGroupDesc = room.groupDescription ?: ""
    }

    val settingsManager = remember { ChatSettingsManager(context) }

    val imagePickerLauncher = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Group Photo",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
            chatViewModel.uploadGroupImage(roomId, bytes, extension)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState),
            contentPadding = PaddingValues(top = 96.dp, bottom = 40.dp)
        ) {
            // 1. Large Centered Group Avatar & Title Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(colors.cardCornerRadius))
                        .background(colors.cardSurface)
                        .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.cardCornerRadius))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Group Avatar with Camera Edit Badge - full unclipped badge
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IosAvatarView(
                            name = room.name ?: "Group",
                            avatarUrl = room.avatarUrl ?: "",
                            size = 90.dp,
                            isGroup = true
                        )

                        if (isAdmin && !isLeft && !isClosed) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(colors.cardSurfaceElevated)
                                    .border(1.5.dp, colors.bg, RoundedCornerShape(7.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Lucide.Camera,
                                    contentDescription = "Change Photo",
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Group Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = room.name ?: "Unnamed Group",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        if (isAdmin && !isLeft && !isClosed) {
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { showEditNameDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Lucide.Pencil, "Edit", tint = colors.primary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Group · ${room.participants.size} participants",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )

                    // Group Description
                    if (!room.groupDescription.isNullOrBlank() || isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(colors.chipCornerRadius))
                                .background(colors.inputBg)
                                .clickable(enabled = isAdmin && !isLeft && !isClosed) {
                                    newGroupDesc = room.groupDescription ?: ""
                                    showEditDescDialog = true
                                }
                                .padding(12.dp)
                        ) {
                            if (room.groupDescription.isNullOrBlank()) {
                                Text(
                                    text = "Add a group description...",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = room.groupDescription ?: "",
                                    color = colors.textMain,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Action Grid (Add, Mute, Wallpaper, Search)
            if (!isLeft && !isClosed) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action 1: Add Member (Admin only)
                        if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(colors.buttonCornerRadius))
                                    .background(colors.cardSurface)
                                    .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.buttonCornerRadius))
                                    .clickable { showAddMemberDialog = true }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Lucide.UserPlus, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Add", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Action 2: Mute Notifications
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp)
                                .clip(RoundedCornerShape(colors.buttonCornerRadius))
                                .background(colors.cardSurface)
                                .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.buttonCornerRadius))
                                .clickable { chatViewModel.muteChat(roomId, !isMuted) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (isMuted) Lucide.BellOff else Lucide.Bell,
                                    null,
                                    tint = if (isMuted) colors.warning else colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(if (isMuted) "Unmute" else "Mute", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Action 3: Wallpaper
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp)
                                .clip(RoundedCornerShape(colors.buttonCornerRadius))
                                .background(colors.cardSurface)
                                .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.buttonCornerRadius))
                                .clickable { showWallpaperPicker = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Lucide.Palette, null, tint = colors.primaryHover, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Wallpaper", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Announcement Channel Setting (Admin only)
            if (isAdmin && !isLeft && !isClosed) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .studioBevel(cornerRadius = colors.cardCornerRadius, borderWidth = 0.5.dp)
                            .clip(RoundedCornerShape(colors.cardCornerRadius))
                            .background(colors.cardSurface)
                            .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.cardCornerRadius))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Lucide.ShieldCheck,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Announcement Channel",
                                        color = colors.textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (room.onlyAdminsCanPost) "Only admins can send messages" else "All members can send messages",
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            AppleSwitch(
                                checked = room.onlyAdminsCanPost,
                                onCheckedChange = { nextVal ->
                                    chatViewModel.updateOnlyAdminsCanPost(room.id, nextVal)
                                }
                            )
                        }

                        HorizontalDivider(
                            color = colors.cardBorderSubtle,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Disappearing Messages Timer
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Timer, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Disappearing Messages",
                                        color = colors.textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (room.disappearingHours > 0) "Auto-expires after ${com.chroma.studio.chat.ui.components.disappearingTimerLabel(room.disappearingHours)}" else "Keep message history",
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.inputBg)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0, 24, 168, 2160).forEach { hours ->
                                    val selected = room.disappearingHours == hours
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selected) colors.primary else Color.Transparent)
                                            .clickable { chatViewModel.updateDisappearingHours(room.id, hours) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = com.chroma.studio.chat.ui.components.disappearingTimerLabel(hours),
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (selected) Color.White else colors.textMuted
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = colors.cardBorderSubtle,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Invite Link Copy & Toggle
                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Lucide.Link2, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Group Invite Link", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            if (!room.inviteCode.isNullOrBlank()) "Active link ready to share" else "Disabled",
                                            color = colors.textMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                AppleSwitch(
                                    checked = !room.inviteCode.isNullOrBlank(),
                                    onCheckedChange = { enable ->
                                        val nextCode = if (enable) com.chroma.studio.chat.ui.components.generateGroupInviteCode() else null
                                        chatViewModel.updateInviteCode(room.id, nextCode)
                                    }
                                )
                            }
                            if (!room.inviteCode.isNullOrBlank()) {
                                val url = com.chroma.studio.chat.ui.components.buildGroupInviteUrl(room.inviteCode)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.inputBg)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = url,
                                        fontSize = 12.sp,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Lucide.Copy,
                                        contentDescription = "Copy Link",
                                        tint = colors.primary,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable {
                                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(url))
                                                ToastManager.showToast("Invite link copied")
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Participants Section Header
            item {
                Text(
                    text = "${room.participants.size} MEMBERS",
                    color = colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // 4. Inset Grouped Participants List
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(colors.cardCornerRadius))
                        .background(colors.cardSurface)
                        .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.cardCornerRadius))
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        room.participants.forEachIndexed { index, userId ->
                            val profile = userProfiles[userId]
                            val isMemberAdmin = room.admins.contains(userId)
                            val isMe = userId == currentUser.uid
                            val isMemberOnline = OnlineStatusManager.isUserOnline(userId)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(colors.chipCornerRadius))
                                    .clickable { onNavigateToProfile(userId) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IosAvatarView(
                                    name = profile?.displayName ?: "User",
                                    avatarUrl = profile?.avatarUrl ?: "",
                                    size = 46.dp,
                                    isOnline = isMemberOnline,
                                    gender = profile?.gender
                                )

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMe) "${profile?.displayName ?: "You"} (You)" else profile?.displayName ?: "Member",
                                            color = colors.textMain,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (profile?.verified == true) {
                                            Spacer(Modifier.width(4.dp))
                                            VerifiedBadge(
                                                size = 13.dp,
                                                tone = profile.verifiedTone
                                            )
                                        }
                                    }
                                    if (profile?.username?.isNotBlank() == true) {
                                        Text(
                                            text = "@${profile.username}",
                                            color = colors.textMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (isMemberAdmin) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Admin",
                                            color = colors.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isAdmin && !isLeft && !isClosed && !isMe) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Lucide.Menu, "Options", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier
                                                .shadow(16.dp, RoundedCornerShape(colors.cardCornerRadius), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.15f))
                                                .clip(RoundedCornerShape(colors.cardCornerRadius))
                                                .background(colors.cardSurface)
                                                .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.cardCornerRadius))
                                        ) {
                                            if (!isMemberAdmin) {
                                                DropdownMenuItem(
                                                    text = { Text("Make Group Admin", color = colors.textMain) },
                                                    leadingIcon = { Icon(Lucide.ShieldCheck, null, tint = colors.primary) },
                                                    onClick = {
                                                        chatViewModel.makeAdmin(room.id, userId)
                                                        expanded = false
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("Remove Admin Rights", color = colors.warning) },
                                                    leadingIcon = { Icon(Lucide.ShieldMinus, null, tint = colors.warning) },
                                                    onClick = {
                                                        chatViewModel.demoteAdmin(room.id, userId)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Remove from Group", color = colors.error) },
                                                leadingIcon = { Icon(Lucide.UserMinus, null, tint = colors.error) },
                                                onClick = {
                                                    expanded = false
                                                    memberPendingRemove = userId to (profile?.displayName?.ifBlank { "this member" } ?: "this member")
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (index < room.participants.size - 1) {
                                HorizontalDivider(
                                    color = colors.cardBorderSubtle,
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Destructive Actions Section (Leave Group)
            if (!isLeft && !isClosed) {
                item {
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(colors.buttonCornerRadius))
                            .background(colors.cardSurface)
                            .border(0.5.dp, colors.cardBorder, RoundedCornerShape(colors.buttonCornerRadius))
                            .clickable {
                                showLeaveGroupConfirm = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.LogOut, null, tint = colors.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Exit Group", color = colors.error, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Standard Cupertino Navigation Header
        ChromaScreenHeader(
            title = "Oscellion",
            badgeText = "Group Info",
            showBackButton = true,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Remove Member Confirmation Dialog
    memberPendingRemove?.let { (targetUid, targetName) ->
        IosAlertDialog(
            title = "Remove Member?",
            message = "Remove $targetName from \"${room.name ?: "this group"}\"?",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                memberPendingRemove = null
                chatViewModel.removeParticipant(room.id, targetUid)
            },
            dismissText = "Cancel",
            onDismiss = { memberPendingRemove = null }
        )
    }

    // Leave Group Confirmation Dialog
    if (showLeaveGroupConfirm) {
        IosAlertDialog(
            title = "Exit Group?",
            message = "Are you sure you want to leave \"${room.name ?: "this group"}\"? You will no longer receive messages from this group.",
            confirmText = "Exit Group",
            onConfirm = {
                showLeaveGroupConfirm = false
                chatViewModel.leaveGroup(room.id)
                onBack()
            },
            dismissText = "Cancel",
            onDismiss = { showLeaveGroupConfirm = false },
            isDestructive = true
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        IosAlertDialog(
            title = "Edit Group Name",
            message = "Enter a new name for this group",
            confirmText = "Save",
            onConfirm = {
                if (newGroupName.isNotBlank() && newGroupName != room.name) {
                    chatViewModel.updateGroupName(room.id, newGroupName)
                }
                showEditNameDialog = false
            },
            dismissText = "Cancel",
            onDismiss = { showEditNameDialog = false },
            customContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(colors.chipCornerRadius))
                        .background(colors.inputBg)
                        .border(0.5.dp, colors.inputBorder, RoundedCornerShape(colors.chipCornerRadius))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = colors.textMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Edit Description Dialog
    if (showEditDescDialog) {
        IosAlertDialog(
            title = "Edit Group Description",
            message = "Enter a new description for this group",
            confirmText = "Save",
            onConfirm = {
                if (newGroupDesc != room.groupDescription) {
                    chatViewModel.updateGroupDescription(room.id, newGroupDesc)
                }
                showEditDescDialog = false
            },
            dismissText = "Cancel",
            onDismiss = { showEditDescDialog = false },
            customContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 150.dp)
                        .clip(RoundedCornerShape(colors.chipCornerRadius))
                        .background(colors.inputBg)
                        .border(0.5.dp, colors.inputBorder, RoundedCornerShape(colors.chipCornerRadius))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newGroupDesc,
                        onValueChange = { if (it.length <= 250) newGroupDesc = it },
                        singleLine = false,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = colors.textMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
        var addMemberSearch by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var searchResults by remember { mutableStateOf<List<com.chroma.studio.model.UserProfile>>(emptyList()) }

        val availableUsers = remember(recentChats, room.participants, userProfiles) {
            val users = mutableMapOf<String, com.chroma.studio.model.UserProfile>()
            recentChats.forEach { chat ->
                if (!chat.isGroup) {
                    chat.participants.forEach { pId ->
                        if (pId != currentUser.uid && !room.participants.contains(pId)) {
                            userProfiles[pId]?.let { users[pId] = it }
                        }
                    }
                }
            }
            users.values.toList()
        }

        var suggestedUsers by remember { mutableStateOf<List<com.chroma.studio.model.UserProfile>>(emptyList()) }
        LaunchedEffect(Unit) {
            try {
                suggestedUsers = discoverRepo.getSuggestedCreators(limit = 40)
                    .filter { it.uid != currentUser.uid && !room.participants.contains(it.uid) }
            } catch (_: Exception) {}
        }

        val baseUsers = remember(availableUsers, suggestedUsers, userProfiles, room.participants) {
            val allCandidates = (availableUsers + suggestedUsers + userProfiles.values.filter { it.uid.isNotBlank() })
                .filter { it.uid != currentUser.uid && !room.participants.contains(it.uid) }
            allCandidates.distinctBy { it.uid }
        }

        LaunchedEffect(addMemberSearch) {
            if (addMemberSearch.length >= 2) {
                isSearching = true
                searchResults = discoverRepo.searchUsers(addMemberSearch).filter { 
                    it.uid != currentUser.uid && !room.participants.contains(it.uid) 
                }
                isSearching = false
            } else {
                searchResults = emptyList()
            }
        }

        val displayUsers = if (addMemberSearch.length >= 2) searchResults else baseUsers

        IosAlertDialog(
            title = "Add to Group",
            message = "Search for a user by username or select from recent contacts.",
            confirmText = "Done",
            onConfirm = { showAddMemberDialog = false },
            dismissText = null,
            onDismiss = { showAddMemberDialog = false },
            customContent = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(colors.chipCornerRadius))
                            .background(colors.inputBg)
                            .border(0.5.dp, colors.inputBorder, RoundedCornerShape(colors.chipCornerRadius))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = addMemberSearch,
                            onValueChange = { addMemberSearch = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = colors.textMain
                            ),
                            decorationBox = { inner ->
                                if (addMemberSearch.isEmpty()) {
                                    Text("Search users...", color = colors.textMuted, fontSize = 14.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary, strokeWidth = 2.dp)
                        }
                    } else if (displayUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(if (addMemberSearch.isNotEmpty()) "No users found" else "No contacts available", color = colors.textMuted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            items(displayUsers, key = { it.uid }) { profile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            chatViewModel.addParticipants(room.id, listOf(profile.uid))
                                            showAddMemberDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IosAvatarView(
                                        name = profile.displayName,
                                        avatarUrl = profile.avatarUrl,
                                        size = 36.dp,
                                        isOnline = OnlineStatusManager.isUserOnline(profile.uid),
                                        gender = profile.gender
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            profile.displayName.ifBlank { "User" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colors.textMain
                                        )
                                        if (profile.username.isNotBlank()) {
                                            Text(
                                                "@${profile.username}",
                                                fontSize = 12.sp,
                                                color = colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(Lucide.UserPlus, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Wallpaper Picker Sheet
    if (showWallpaperPicker) {
        ChatWallpaperPickerSheet(
            settingsManager = settingsManager,
            roomId = roomId,
            onDismiss = { showWallpaperPicker = false }
        )
    }
}
