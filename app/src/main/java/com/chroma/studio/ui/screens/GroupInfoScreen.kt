package com.chroma.studio.ui.screens

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
    val room = recentChats.find { it.id == roomId }
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    if (room == null || !room.isGroup) {
        onBack()
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
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(room.name ?: "") }
    var newGroupDesc by remember { mutableStateOf(room.groupDescription ?: "") }

    val settingsManager = remember { ChatSettingsManager(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
                chatViewModel.uploadGroupImage(roomId, bytes, extension)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F3F8))
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                        .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Group Avatar with Camera Edit Badge
                    Box(
                        modifier = Modifier.size(92.dp),
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
                                    .size(30.dp)
                                    .align(Alignment.BottomEnd)
                                    .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Camera, "Change Photo", tint = Color.White, modifier = Modifier.size(15.dp))
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
                                Icon(Lucide.Pencil, "Edit", tint = Color(0xFF007AFF), modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Group · ${room.participants.size} participants",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )

                    // Group Description
                    if (!room.groupDescription.isNullOrBlank() || isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0x26FFFFFF) else Color(0x0A000000))
                                .clickable(enabled = isAdmin && !isLeft && !isClosed) {
                                    newGroupDesc = room.groupDescription ?: ""
                                    showEditDescDialog = true
                                }
                                .padding(12.dp)
                        ) {
                            if (room.groupDescription.isNullOrBlank()) {
                                Text(
                                    text = "Add a group description...",
                                    color = Color(0xFF8E8E93),
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                                    .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                                    .clickable { showAddMemberDialog = true }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Lucide.UserPlus, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                                .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                                .clickable { chatViewModel.muteChat(roomId, !isMuted) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (isMuted) Lucide.BellOff else Lucide.Bell,
                                    null,
                                    tint = if (isMuted) Color(0xFFFF9F0A) else Color(0xFF007AFF),
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                                .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                                .clickable { showWallpaperPicker = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Lucide.Palette, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Wallpaper", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 3. Participants Section Header
            item {
                Text(
                    text = "${room.participants.size} MEMBERS",
                    color = Color(0xFF8E8E93),
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                        .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        room.participants.forEachIndexed { index, userId ->
                            val profile = userProfiles[userId]
                            val isMemberAdmin = room.admins.contains(userId)
                            val isMe = userId == currentUser.uid

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToProfile(userId) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IosAvatarView(
                                    name = profile?.displayName ?: "User",
                                    avatarUrl = profile?.avatarUrl ?: "",
                                    size = 46.dp,
                                    gender = profile?.gender
                                )

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMe) "${profile?.displayName ?: "You"} (You)" else profile?.displayName ?: "Member",
                                            color = Color(0xFF1C1C1E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (profile?.verified == true) {
                                            Spacer(Modifier.width(4.dp))
                                            Image(
                                                painter = painterResource(R.drawable.verification_badge),
                                                contentDescription = "Verified",
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    if (profile?.username?.isNotBlank() == true) {
                                        Text(
                                            text = "@${profile.username}",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (isMemberAdmin) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A84FF).copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Admin",
                                            color = Color(0xFF0A84FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isAdmin && !isLeft && !isClosed && !isMe) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Lucide.Menu, "Options", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            if (!isMemberAdmin) {
                                                DropdownMenuItem(
                                                    text = { Text("Make Group Admin", color = Color(0xFF1C1C1E)) },
                                                    leadingIcon = { Icon(Lucide.ShieldCheck, null, tint = Color(0xFF0A84FF)) },
                                                    onClick = {
                                                        chatViewModel.makeAdmin(room.id, userId)
                                                        expanded = false
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("Remove Admin Rights", color = Color(0xFFFF9F0A)) },
                                                    leadingIcon = { Icon(Lucide.ShieldMinus, null, tint = Color(0xFFFF9F0A)) },
                                                    onClick = {
                                                        chatViewModel.demoteAdmin(room.id, userId)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Remove from Group", color = Color(0xFFFF3B30)) },
                                                leadingIcon = { Icon(Lucide.UserMinus, null, tint = Color(0xFFFF3B30)) },
                                                onClick = {
                                                    chatViewModel.removeParticipant(room.id, userId)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (index < room.participants.size - 1) {
                                HorizontalDivider(
                                    color = Color(0x14000000),
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White)
                            .border(0.5.dp, if (colors.isDark) Color(0x2EFFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                            .clickable {
                                showLeaveGroupConfirm = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.LogOut, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Exit Group", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (colors.isDark) Color(0x33FFFFFF) else Color(0x14000000))
                        .border(0.5.dp, if (colors.isDark) Color(0x33FFFFFF) else Color(0x24000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = if (colors.isDark) Color.White else Color.Black
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (colors.isDark) Color(0x33FFFFFF) else Color(0x14000000))
                        .border(0.5.dp, if (colors.isDark) Color(0x33FFFFFF) else Color(0x24000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newGroupDesc,
                        onValueChange = { if (it.length <= 250) newGroupDesc = it },
                        singleLine = false,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = if (colors.isDark) Color.White else Color.Black
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

        val displayUsers = if (addMemberSearch.length >= 2) searchResults else availableUsers

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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (colors.isDark) Color(0x33FFFFFF) else Color(0x14000000))
                            .border(0.5.dp, if (colors.isDark) Color(0x33FFFFFF) else Color(0x24000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = addMemberSearch,
                            onValueChange = { addMemberSearch = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = if (colors.isDark) Color.White else Color.Black
                            ),
                            decorationBox = { inner ->
                                if (addMemberSearch.isEmpty()) {
                                    Text("Search users...", color = Color(0xFF8E8E93), fontSize = 14.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF0A84FF), strokeWidth = 2.dp)
                        }
                    } else if (displayUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(if (addMemberSearch.isNotEmpty()) "No users found" else "No contacts available", color = Color(0xFF8E8E93), fontSize = 14.sp)
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
                                        gender = profile.gender
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            profile.displayName.ifBlank { "User" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (colors.isDark) Color.White else Color.Black
                                        )
                                        if (profile.username.isNotBlank()) {
                                            Text(
                                                "@${profile.username}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF8E8E93),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(Lucide.UserPlus, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
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
