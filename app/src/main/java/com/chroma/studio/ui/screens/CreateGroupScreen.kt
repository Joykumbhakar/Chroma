package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AuroraBackground
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.LiquidGlassProgressBar
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.ui.components.AppleHeartHandshakeGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.spatialCard
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChatViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ChromaHeaderActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit // roomId
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Step state: 0 = pick members, 1 = setup name/avatar
    var step by remember { mutableStateOf(0) }

    // Member selection
    val recentChats by chatViewModel.recentChats.collectAsState()
    val userProfiles by chatViewModel.userProfiles.collectAsState()
    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
    val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    val allUsers: List<UserProfile> = remember(recentChats, userProfiles) {
        val uids = recentChats.flatMap { it.participants }.distinct().filter { it != currentUserUid }
        uids.mapNotNull { userProfiles[it] }.filter { it.uid.isNotBlank() }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            searchResults = discoverRepo.searchUsers(searchQuery).filter { it.uid != currentUserUid }
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }
    
    val displayUsers = if (searchQuery.length >= 2) searchResults else allUsers

    var selectedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Group setup
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        groupImageUri = uri
    }

    Scaffold(
        topBar = {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = if (step == 0) "Add Members" else "New Group",
                showBackButton = true,
                onBack = {
                    if (step == 1) step = 0 else onBack()
                },
                actions = {
                    AnimatedContent(targetState = step, label = "btn") { s ->
                        if (s == 0) {
                            ChromaHeaderActionButton(
                                text = if (selectedUsers.isNotEmpty()) "Next (${selectedUsers.size})" else "Next",
                                icon = Lucide.ArrowRight,
                                enabled = selectedUsers.isNotEmpty(),
                                onClick = { step = 1 }
                            )
                        } else {
                            ChromaHeaderActionButton(
                                text = "Create",
                                icon = Lucide.Check,
                                enabled = groupName.isNotBlank() && !isCreating,
                                isLoading = isCreating,
                                accentColor = Color(0xFF30D158),
                                onClick = {
                                    isCreating = true
                                    scope.launch {
                                        val participantIds = selectedUsers.map { it.uid }
                                        var imageUrl: String? = null
                                        if (groupImageUri != null) {
                                            val stream = context.contentResolver.openInputStream(groupImageUri!!)
                                            val bytes = stream?.readBytes()
                                            stream?.close()
                                            if (bytes != null) {
                                                val ext = context.contentResolver.getType(groupImageUri!!)?.substringAfter("/") ?: "jpg"
                                                imageUrl = chatViewModel.uploadGroupImageBytes(bytes, ext)
                                            }
                                        }
                                        chatViewModel.createGroup(groupName.trim(), groupDescription.trim(), participantIds, imageUrl) { roomId ->
                                            onGroupCreated(roomId)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AuroraBackground()

            Column(modifier = Modifier.fillMaxSize()) {

                // ─── Step 0: Member Picker ──────────────────────────────────
                AnimatedContent(targetState = step, label = "steps",
                    transitionSpec = {
                        slideInHorizontally { if (targetState > initialState) it else -it } togetherWith
                            slideOutHorizontally { if (targetState > initialState) -it else it }
                    }
                ) { currentStep ->
                    if (currentStep == 0) {
                        Column(Modifier.fillMaxSize()) {
                            // Selected chips
                            AnimatedVisibility(visible = selectedUsers.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(selectedUsers, key = { it.uid }) { user ->
                                        SelectedUserChip(user = user) {
                                            selectedUsers = selectedUsers.filter { it.uid != user.uid }
                                        }
                                    }
                                }
                            }

                            // Search bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .spatialCard()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Lucide.Search, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) Text("Search people...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                                        inner()
                                    },
                                    singleLine = true
                                )
                            }

                            // User list
                            if (allUsers.isEmpty() && searchQuery.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val emptyState = rememberAppleGraphicAnimationState()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        AppleHeartHandshakeGraphic(
                                            modifier = Modifier.size(130.dp),
                                            state = emptyState,
                                            enableClickToAnimate = true
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text("No contacts yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Search by username to add members to your group", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSearching) {
                                        item {
                                            LiquidGlassProgressBar(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                        }
                                    } else if (searchQuery.length >= 2 && searchResults.isEmpty()) {
                                        item {
                                            Text("No users found", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                                        }
                                    }
                                    
                                    items(displayUsers, key = { it.uid }) { user ->
                                        val isSelected = selectedUsers.any { it.uid == user.uid }
                                        UserSelectRow(
                                            user = user,
                                            isSelected = isSelected,
                                            onClick = {
                                                selectedUsers = if (isSelected)
                                                    selectedUsers.filter { it.uid != user.uid }
                                                else selectedUsers + user
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ─── Step 1: Group Setup ────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(24.dp))

                            // Avatar picker
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0A84FF).copy(alpha = 0.4f), Color(0xFF30D158).copy(alpha = 0.4f))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (groupImageUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(groupImageUri).crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Dark overlay + edit icon on top
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Camera, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Lucide.Camera, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("Add Photo", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Group name field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .spatialCard()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Lucide.Users, null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                BasicTextField(
                                    value = groupName,
                                    onValueChange = { if (it.length <= 50) groupName = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium),
                                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                    decorationBox = { inner ->
                                        if (groupName.isEmpty()) Text("Group name", color = Color.White.copy(alpha = 0.4f), fontSize = 17.sp)
                                        inner()
                                    },
                                    singleLine = true
                                )
                                Text("${groupName.length}/50", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                                if (groupName.isNotEmpty()) {
                                    Icon(Lucide.X, null, tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp).clickable { groupName = "" })
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))

                            // Group Description field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .spatialCard()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Lucide.FileText, null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                BasicTextField(
                                    value = groupDescription,
                                    onValueChange = { if (it.length <= 250) groupDescription = it },
                                    modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                    decorationBox = { inner ->
                                        if (groupDescription.isEmpty()) Text("Group description (optional)", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                                        inner()
                                    },
                                    singleLine = false
                                )
                                Text("${groupDescription.length}/250", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
                            }

                            Spacer(Modifier.height(20.dp))

                            // Member preview chips
                            Text(
                                "${selectedUsers.size} Members",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth().spatialCard().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedUsers.forEach { user ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (user.avatarUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(user.avatarUrl).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Lucide.User, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(user.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("@${user.username}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Helper Composables ─────────────────────────────────────────────────────

@Composable
private fun UserSelectRow(user: UserProfile, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        // Avatar
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(user.avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Lucide.User, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${user.username}", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, maxLines = 1)
        }
        // Selection Checkbox
        LiquidGlassCheckbox(
            checked = isSelected,
            onCheckedChange = { onClick() },
            size = 22.dp
        )
    }
}

@Composable
private fun SelectedUserChip(user: UserProfile, onRemove: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A84FF).copy(alpha = 0.25f))
            .border(1.dp, Color(0xFF0A84FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(user.avatarUrl).crossfade(true).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Lucide.User, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(user.displayName.take(12), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Icon(Lucide.X, null, tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove))
    }
}
