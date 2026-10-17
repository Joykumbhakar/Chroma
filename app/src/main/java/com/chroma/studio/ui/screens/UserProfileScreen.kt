@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens
import android.graphics.Bitmap
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.isSystemInDarkTheme
import com.chroma.studio.ui.components.AppleStatusBlueGradient

import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.AppleWorksGraphic
import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.AppleRemixGraphic
import com.chroma.studio.ui.components.AppleHeartHandshakeGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.AiWaveSphereVector
// Apple HIG 3D Empty States
import com.chroma.studio.ui.components.ChromaPillVariant
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getProfileRoleIcon(role: String?): ImageVector {
    if (role.isNullOrBlank()) return AiWaveSphereVector
    return when (role.trim().lowercase()) {
        "developer" -> Lucide.Terminal
        "designer" -> Lucide.Palette
        "web designer" -> Lucide.Monitor
        "frontend developer" -> Lucide.LayoutTemplate
        "android developer" -> Lucide.Smartphone
        "ios developer" -> Lucide.Smartphone
        "content creator" -> Lucide.Video
        "doctor" -> Lucide.Activity
        "engineer" -> Lucide.Wrench
        "advocate" -> Lucide.BookOpen
        "ca" -> Lucide.Briefcase
        else -> AiWaveSphereVector
    }
}

internal fun isFieldVisible(key: String, visibility: Map<String, String>?, isCurrentUser: Boolean, isFollowing: Boolean): Boolean {
    if (isCurrentUser) return true
    val defaultLevel = when (key) {
        "dob", "gender", "location" -> "private"
        else -> "public"
    }
    val level = visibility?.get(key) ?: defaultLevel
    return when (level) {
        "private" -> false
        "followers" -> isFollowing
        "public" -> true
        else -> false
    }
}

@Composable
fun UserProfileScreen(
    userId: String,
    initialPostId: String? = null,
    repository: WorkRepository,
    chatViewModel: com.chroma.studio.viewmodel.ChatViewModel,
    storyViewModel: com.chroma.studio.viewmodel.StoryViewModel? = null,
    showBackButton: Boolean = true,
    onRequireLogin: () -> Unit,
    onDownloadWork: (DiscoverPost) -> Unit,
    onRemixWork: (DiscoverPost) -> Unit,
    onNavigateToOriginal: (String, String) -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)? = null,
    onNavigateToChat: ((String) -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val discoverVm = viewModel<DiscoverViewModel>()
    val storyVm = storyViewModel ?: viewModel<StoryViewModel>()
    val allStoryGroups by storyVm.storyGroups.collectAsState()
    val myStories by storyVm.myStories.collectAsState()
    val likedStoryIds by storyVm.likedStoryIds.collectAsState()
    val isUploadingStory by storyVm.isUploading.collectAsState()
    var showStoryViewer by remember { mutableStateOf(false) }
    var storyViewerInitialIndex by remember { mutableIntStateOf(0) }

    var works by remember { mutableStateOf<List<DiscoverPost>>(emptyList()) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var mutualFollowers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val likedWorkIds by discoverVm.likedWorkIds.collectAsState()

    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isCurrentUser = currentUserId == userId
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { storyVm.uploadStory(context, it) }
    }

    var isWorksLoading by remember { mutableStateOf(true) }
    var userFlashes by remember { mutableStateOf<List<FlashVideo>>(emptyList()) }
    var isFlashesLoading by remember { mutableStateOf(true) }
    var flashToDelete by remember { mutableStateOf<FlashVideo?>(null) }

    var showShareCardModal by remember { mutableStateOf(false) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var showBannerLibrarySheet by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }

    val cropBannerLauncher = rememberLauncherForActivityResult(com.canhub.cropper.CropImageContract()) { result ->
        if (result.isSuccessful && result.uriContent != null) {
            val croppedUri = result.uriContent!!
            scope.launch {
                try {
                    isUploadingBanner = true
                    val bytes = com.chroma.studio.utils.compressImageToUnder50KB(context, croppedUri)
                    val uid = currentUserId
                    if (bytes != null && uid != null) {
                        val url = uploadBannerToSupabase(uid, bytes)
                        if (url != null) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("bannerUrl", url)
                                .await()
                            profile = profile?.copy(bannerUrl = url)
                            ToastManager.showToast("Banner updated successfully")
                        } else {
                            ToastManager.showToast("Failed to upload banner image")
                        }
                    }
                } catch (e: Exception) {
                    ToastManager.showToast("Error updating banner: ")
                } finally {
                    isUploadingBanner = false
                    showBannerLibrarySheet = false
                }
            }
        }
    }

    val bannerGalleryLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            cropBannerLauncher.launch(
                com.canhub.cropper.CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = com.canhub.cropper.CropImageOptions(
                        guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                        fixAspectRatio = true,
                        aspectRatioX = 3,
                        aspectRatioY = 1,
                        outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 90
                    )
                )
            )
        }
    }

    LaunchedEffect(userId) {
        launch {
            isWorksLoading = true
            works = discoverVm.repository.getWorksByUser(userId)
            isWorksLoading = false
        }

        launch {
            isFlashesLoading = true
            val flashRepo = com.chroma.studio.data.FlashRepository()
            userFlashes = flashRepo.getFlashesByUser(userId)
            isFlashesLoading = false
        }

        launch {
            storyVm.loadUserStories(userId)
        }

        if (currentUserId != null && currentUserId != userId) {
            launch {
                isFollowing = discoverVm.repository.checkIsFollowing(currentUserId, userId)
            }
            launch {
                mutualFollowers = discoverVm.repository.getMutualFollowers(currentUserId, userId)
            }
        }

        discoverVm.repository.getUserProfileFlow(userId).collect { newProfile ->
            profile = newProfile
            isLoading = false
        }
    }

    val isRestricted = profile?.isPrivateAccount == true && !isCurrentUser && !isFollowing

    if (isLoading) {
        SkeletonUserProfile(onBack = { if (showBackButton) onBack() })
        return
    }

    var isBioExpanded by remember { mutableStateOf(false) }
    var hasBioOverflow by remember { mutableStateOf(false) }
    var selectedPostIndex by remember { mutableStateOf<Int?>(null) }
    var showAvatarOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(isWorksLoading) {
        if (!isWorksLoading && initialPostId != null && works.isNotEmpty()) {
            val idx = works.indexOfFirst { it.id == initialPostId }
            if (idx >= 0) {
                selectedPostIndex = idx
            }
        }
    }

    val gridState = rememberLazyGridState()

    val normalWorks = remember(works) { works.filter { it.remixedFromPostId == null && it.postType == "work" } }
    val imagePosts = remember(works) { works.filter { it.postType == "image" } }
    val flashPosts = userFlashes
    val remixWorks = remember(works) { works.filter { it.remixedFromPostId != null } }
    val profileTabIcons = listOf(Lucide.LayoutGrid, Lucide.Image, Lucide.Zap, Lucide.HeartHandshake)

    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 80
        }
    }

    val filteredStories = remember(allStoryGroups, userId) { allStoryGroups.filter { it.userId == userId } }
    val hasActiveStories = remember(isCurrentUser, myStories, filteredStories) {
        if (isCurrentUser) myStories.isNotEmpty() || filteredStories.isNotEmpty()
        else filteredStories.isNotEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Unified 3-Column Grid for entire screen: eliminates nested scrolling & lag
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            // Header Content spans all 3 columns
            item(span = { GridItemSpan(maxLineSpan) }) {
                UserProfileHeroSection(
                    profile = profile,
                    hasActiveStories = hasActiveStories,
                    worksCount = works.size + flashPosts.size,
                    isRestricted = isRestricted,
                    userId = userId,
                    onAvatarClick = {
                        if (hasActiveStories) {
                            if (isCurrentUser) {
                                storyViewerInitialIndex = -1
                                showStoryViewer = true
                            } else {
                                val groupIdx = allStoryGroups.indexOfFirst { it.userId == userId }
                                storyViewerInitialIndex = if (groupIdx >= 0) groupIdx else 0
                                showStoryViewer = true
                            }
                        } else {
                            showAvatarOverlay = true
                        }
                    },
                    onNavigateToUserList = onNavigateToUserList
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                UserProfileBioDetailsSection(
                    profile = profile,
                    isCurrentUser = isCurrentUser,
                    isFollowing = isFollowing,
                    mutualFollowers = mutualFollowers,
                    userId = userId,
                    onShowShareCard = { showShareCardModal = true },
                    onNavigateToUserList = onNavigateToUserList
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                UserProfileActionsStoriesSection(
                    isCurrentUser = isCurrentUser,
                    isFollowing = isFollowing,
                    userId = userId,
                    currentUserId = currentUserId,
                    profile = profile,
                    filteredStories = filteredStories,
                    hasMyStory = isCurrentUser && (myStories.isNotEmpty() || filteredStories.isNotEmpty()),
                    profileTabIcons = profileTabIcons,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onToggleFollow = {
                        if (currentUserId == null) onRequireLogin()
                        else {
                            val prevFollowing = isFollowing
                            val prevProfile = profile
                            isFollowing = !isFollowing
                            profile = profile?.copy(followersCount = (profile?.followersCount ?: 0) + if (isFollowing) 1 else -1)
                            scope.launch {
                                val success = discoverVm.repository.toggleFollow(currentUserId, userId)
                                if (!success) {
                                    isFollowing = prevFollowing
                                    profile = prevProfile
                                    ToastManager.showToast("Failed to update follow status")
                                }
                            }
                        }
                    },
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onShowShareCard = { showShareCardModal = true },
                    onNavigateToChat = onNavigateToChat,
                    onMyStoryClick = {
                        if (myStories.isNotEmpty() || filteredStories.isNotEmpty()) {
                            storyViewerInitialIndex = if (isCurrentUser) -1 else 0
                            showStoryViewer = true
                        } else {
                            storyMediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    onAddStoryClick = {
                        storyMediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onStoryClick = { groupIdx ->
                        val targetGroup = filteredStories.getOrNull(groupIdx)
                        val fullIdx = if (targetGroup != null) allStoryGroups.indexOfFirst { it.userId == targetGroup.userId } else groupIdx
                        storyViewerInitialIndex = if (fullIdx >= 0) fullIdx else 0
                        showStoryViewer = true
                    },
                    storyVm = storyVm
                )
            }

        // Tab Content Items rendered directly into LazyVerticalGrid
        if (isRestricted) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 60.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Lock, contentDescription = "Private Account", tint = Color(0xFF007AFF), modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "This Account is Private",
                        color = colors.textMain,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Follow this account to see their works, posts, and videos.",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (selectedTabIndex) {
                0 -> {
                    // Works / Creations Tab
                    if (isWorksLoading) {
                        items(6) {
                            Box(modifier = Modifier.aspectRatio(1f).clip(RectangleShape).shimmerEffect())
                        }
                    } else if (normalWorks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val worksGraphicState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    AppleWorksGraphic(
                                        modifier = Modifier.size(150.dp),
                                        state = worksGraphicState,
                                        enableClickToAnimate = true
                                    )
                                },
                                title = "No Works Yet",
                                subtitle = "Creations will appear here once published."
                            )
                        }
                    } else {
                        items(normalWorks.size, key = { normalWorks[it].id }) { index ->
                            val work = normalWorks[index]
                            ProfileWorkCard(
                                work = work,
                                isLiked = likedWorkIds.contains(work.id),
                                repository = repository,
                                onClick = { selectedPostIndex = index }
                            )
                        }
                    }
                }
                1 -> {
                    // Photos Tab (Images)
                    if (isWorksLoading) {
                        items(6) {
                            Box(modifier = Modifier.aspectRatio(1f).clip(RectangleShape).shimmerEffect())
                        }
                    } else if (imagePosts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val photosGraphicState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    ApplePhotosGraphic(
                                        modifier = Modifier.size(150.dp),
                                        state = photosGraphicState,
                                        enableClickToAnimate = true
                                    )
                                },
                                title = "No Photos Yet",
                                subtitle = "Photo posts will appear here once shared."
                            )
                        }
                    } else {
                        items(imagePosts.size, key = { imagePosts[it].id }) { index ->
                            val work = imagePosts[index]
                            ProfileWorkCard(
                                work = work,
                                isLiked = likedWorkIds.contains(work.id),
                                repository = repository,
                                onClick = { selectedPostIndex = index }
                            )
                        }
                    }
                }
                2 -> {
                    // Flash Videos Tab
                    if (isFlashesLoading) {
                        items(6) {
                            Box(modifier = Modifier.aspectRatio(9f / 16f).clip(RectangleShape).shimmerEffect())
                        }
                    } else if (flashPosts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val flashGraphicState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    AppleFlashGraphic(
                                        modifier = Modifier.size(150.dp),
                                        state = flashGraphicState,
                                        enableClickToAnimate = true
                                    )
                                },
                                title = "No Flash Videos",
                                subtitle = "Short video moments will appear here."
                            )
                        }
                    } else {
                        items(flashPosts.size, key = { flashPosts[it].id }) { index ->
                            val flash = flashPosts[index]
                            ProfileFlashCard(
                                flash = flash,
                                isOwner = isCurrentUser,
                                onDeleteClick = { flashToDelete = flash }
                            )
                        }
                    }
                }
                3 -> {
                    // Remixes Tab
                    if (isWorksLoading) {
                        items(6) {
                            Box(modifier = Modifier.aspectRatio(1f).clip(RectangleShape).shimmerEffect())
                        }
                    } else if (remixWorks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val remixGraphicState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    AppleHeartHandshakeGraphic(
                                        modifier = Modifier.size(150.dp),
                                        state = remixGraphicState,
                                        enableClickToAnimate = true
                                    )
                                },
                                title = "No Remixes",
                                subtitle = "Remixed co-creations will appear here."
                            )
                        }
                    } else {
                        items(remixWorks.size, key = { remixWorks[it].id }) { index ->
                            val work = remixWorks[index]
                            ProfileWorkCard(
                                work = work,
                                isLiked = likedWorkIds.contains(work.id),
                                repository = repository,
                                onClick = { selectedPostIndex = index }
                            )
                        }
                    }
                }
            }
        }
    }

    // Top Translucent Frosted Glass Navigation Bar
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(
                if (isScrolled) colors.bg else Color.Transparent
            )
            .border(
                width = if (isScrolled) 0.5.dp else 0.dp,
                color = if (isScrolled) colors.glassBorder else Color.Transparent
            )
            .padding(top = statusBarTop + 6.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = if (isScrolled) colors.textMain else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(38.dp))
            }

            // Collapsed title when scrolled
            AnimatedVisibility(
                visible = isScrolled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = profile?.displayName ?: "",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "@${profile?.username ?: ""}",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Action Menu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCurrentUser) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { showBannerLibrarySheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.Pencil,
                            contentDescription = "Edit Banner",
                            tint = if (isScrolled) colors.textMain else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            
                            .clickable { showSettingsBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.Settings,
                            contentDescription = "Settings",
                            tint = if (isScrolled) colors.textMain else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            
                            .clickable { showShareCardModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.Share2,
                            contentDescription = "Share",
                            tint = if (isScrolled) colors.textMain else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Intercept back button when fullscreen post viewer is open
    androidx.activity.compose.BackHandler(enabled = selectedPostIndex != null) {
        selectedPostIndex = null
    }

    // Full Screen Feed Overlay
    AnimatedVisibility(
        visible = selectedPostIndex != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        if (selectedPostIndex != null) {
            val activeList = remember(selectedTabIndex, works) {
                when (selectedTabIndex) {
                    0 -> works
                    1 -> normalWorks
                    2 -> imagePosts
                    4 -> remixWorks
                    else -> emptyList()
                }
            }

            val feedListState = rememberLazyListState(initialFirstVisibleItemIndex = selectedPostIndex!!)
            Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
                LazyColumn(
                    state = feedListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = statusBarTop + 56.dp, bottom = 24.dp)
                ) {
                    items(activeList) { work ->
                        FeedPostCard(
                            work = work,
                            isLiked = likedWorkIds.contains(work.id),
                            repository = repository,
                            onRequireLogin = onRequireLogin,
                            onLikeClick = {
                                if (currentUserId != null) {
                                    discoverVm.toggleLike(work.id, currentUserId)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onAuthorClick = { _ -> },
                            onDownloadClick = {
                                if (currentUserId != null) onDownloadWork(work) else onRequireLogin()
                            },
                            onRemixWork = {
                                if (currentUserId != null) onRemixWork(work) else onRequireLogin()
                            },
                            onArchiveWork = { workId -> discoverVm.archiveWork(workId) },
                            onDeleteWork = { workId ->
                                discoverVm.deleteWork(workId)
                                works = works.filter { it.id != workId }
                                selectedPostIndex = null
                                ToastManager.showToast("Post deleted")
                            },
                            onEditWork = { workId, title, desc -> discoverVm.updateWork(workId, title, desc) },
                            onNavigateToOriginal = onNavigateToOriginal
                        )
                    }
                }

                IconButton(
                    onClick = { selectedPostIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = statusBarTop + 8.dp, start = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Avatar Fullscreen Preview Overlay
    AnimatedVisibility(
        visible = showAvatarOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable { showAvatarOverlay = false },
            contentAlignment = Alignment.Center
        ) {
            val presetRes = remember(profile?.avatarUrl) { getAvatarPresetDrawableRes(profile?.avatarUrl) }
            val defaultAvatarRes = remember(profile?.gender) { getDefaultAvatarDrawableRes(profile?.gender) }
            val resolvedDrawable = presetRes ?: if (profile?.avatarUrl.isNullOrBlank()) defaultAvatarRes else null

            if (resolvedDrawable != null) {
                Image(
                    painter = painterResource(id = resolvedDrawable),
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (!profile?.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile?.avatarUrl,
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.default_avatar_apple),
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            IconButton(
                onClick = { showAvatarOverlay = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarTop + 8.dp, start = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Lucide.ArrowLeft, contentDescription = "Close", tint = Color.White)
            }
        }
    }

    // Story Viewer Overlay
    if (showStoryViewer && (allStoryGroups.isNotEmpty() || filteredStories.isNotEmpty() || myStories.isNotEmpty())) {
        val viewerGroups = remember(allStoryGroups, filteredStories) {
            if (filteredStories.isNotEmpty() && !allStoryGroups.any { it.userId == userId }) {
                filteredStories + allStoryGroups
            } else if (allStoryGroups.isNotEmpty()) {
                allStoryGroups
            } else {
                filteredStories
            }
        }
        StoryViewerScreen(
            storyGroups = viewerGroups,
            initialGroupIndex = storyViewerInitialIndex,
            currentUserId = currentUserId ?: "",
            storyViewModel = storyVm,
            likedStoryIds = likedStoryIds,
            myStories = myStories,
            currentUserProfile = if (isCurrentUser) profile else null,
            onClose = { showStoryViewer = false },
            onAuthorClick = { clickedUid ->
                showStoryViewer = false
                if (clickedUid != userId) {
                    onNavigateToUserList?.invoke(clickedUid, "followers")
                }
            }
        )
    }

    // Apple ID / NameDrop Share Card Bottom Sheet
    if (showShareCardModal && profile != null) {
        AppleProfileCardSheet(
            profile = profile!!,
            userId = userId,
            worksCount = works.size,
            onDismiss = { showShareCardModal = false }
        )
    }

    // Apple Settings Bottom Sheet
    if (showSettingsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF141416) else Color(0xFFF2F2F7),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Options & Settings",
                        color = colors.textMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showSettingsBottomSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(16.dp))
                    }
                }

                AppleSettingsGroup {
                    AppleSettingNavRow(
                        title = "Account Settings",
                        subtitle = "Manage privacy, security & alerts",
                        icon = Lucide.UserCog,
                        onClick = {
                            showSettingsBottomSheet = false
                            onNavigateToSettings()
                        }
                    )
                    AppleSettingDivider()
                    AppleSettingNavRow(
                        title = "Edit Identity & Avatar",
                        subtitle = "Change name, avatars, and bio",
                        icon = Lucide.UserCheck,
                        onClick = {
                            showSettingsBottomSheet = false
                            onNavigateToEditProfile()
                        }
                    )
                    AppleSettingDivider()
                    AppleSettingNavRow(
                        title = "Profile Pass Card",
                        subtitle = "Share your scannable digital card",
                        icon = Lucide.QrCode,
                        onClick = {
                            showSettingsBottomSheet = false
                            showShareCardModal = true
                        }
                    )
                    AppleSettingDivider()
                    AppleSettingNavRow(
                        title = "Sign Out",
                        subtitle = "Log out from this device",
                        icon = Lucide.LogOut,
                        isDestructive = true,
                        showChevron = false,
                        onClick = {
                            showSettingsBottomSheet = false
                            showSignOutConfirm = true
                        }
                    )
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirm) {
        IosAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to log out from this device?",
            confirmText = "Sign Out",
            onConfirm = {
                showSignOutConfirm = false
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                onRequireLogin()
            },
            dismissText = "Cancel",
            onDismiss = { showSignOutConfirm = false },
            isDestructive = true
        )
    }

    // Banner Library Modal Bottom Sheet
    if (showBannerLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!isUploadingBanner) showBannerLibrarySheet = false },
            containerColor = colors.bg,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            var libraryTab by remember { mutableIntStateOf(0) }
            val myWorksWithImages = remember(works) { works.filter { !it.imageUrl.isNullOrBlank() } }
            val wavePresets = remember { AllBannerPresets.filter { it.id.startsWith("preset:wave_") } }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Choose Profile Banner",
                            color = colors.textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Select a vector preset, published work, or upload an image",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isUploadingBanner) {
                        OscellionLoadingSpinner(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = { showBannerLibrarySheet = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action Buttons Row: Upload from Device & Remove Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChromaPillButton(
                        onClick = { bannerGalleryLauncher.launch("image/*") },
                        variant = ChromaPillVariant.Blue,
                        modifier = Modifier.weight(1f).height(40.dp),
                        enabled = !isUploadingBanner
                    ) {
                        Icon(Lucide.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload from Device", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }

                    if (!profile?.bannerUrl.isNullOrBlank()) {
                        ChromaPillButton(
                            onClick = {
                                val uid = currentUserId ?: return@ChromaPillButton
                                scope.launch {
                                    try {
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).update("bannerUrl", "").await()
                                        profile = profile?.copy(bannerUrl = "")
                                        ToastManager.showToast("Banner removed")
                                    } catch (e: Exception) {
                                        ToastManager.showToast("Failed to remove banner")
                                    }
                                    showBannerLibrarySheet = false
                                }
                            },
                            variant = ChromaPillVariant.Rose,
                            modifier = Modifier.height(40.dp),
                            enabled = !isUploadingBanner
                        ) {
                            Icon(Lucide.Trash2, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tab Switchers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                        .padding(3.dp)
                ) {
                    listOf("Waves (${wavePresets.size})", "Work (${myWorksWithImages.size})").forEachIndexed { index, title ->
                        val isSelected = libraryTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
                                .clickable { libraryTab = index }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                color = if (isSelected) Color.White else colors.textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                when (libraryTab) {
                    0 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(wavePresets, key = { it.id }) { preset ->
                                val isSelected = profile?.bannerUrl == preset.id
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.75.dp,
                                            color = if (isSelected) Color(0xFF007AFF) else colors.glassBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val uid = currentUserId ?: return@clickable
                                            scope.launch {
                                                try {
                                                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).update("bannerUrl", preset.id).await()
                                                    profile = profile?.copy(bannerUrl = preset.id)
                                                    ToastManager.showToast("Banner set to ")
                                                } catch (e: Exception) {
                                                    ToastManager.showToast("Failed to update banner")
                                                }
                                                showBannerLibrarySheet = false
                                            }
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                        Image(
                                            painter = painterResource(id = preset.drawableRes),
                                            contentDescription = preset.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF007AFF))
                                                    .align(Alignment.TopEnd),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF9F9F9))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(preset.name, color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (myWorksWithImages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Lucide.Image, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No image works published yet", color = colors.textMuted, fontSize = 12.sp)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(myWorksWithImages, key = { it.id }) { work ->
                                    val isSelected = profile?.bannerUrl == work.imageUrl
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.75.dp,
                                                color = if (isSelected) Color(0xFF007AFF) else colors.glassBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                val uid = currentUserId ?: return@clickable
                                                val workImg = work.imageUrl ?: return@clickable
                                                cropBannerLauncher.launch(
                                                    com.canhub.cropper.CropImageContractOptions(
                                                        uri = Uri.parse(workImg),
                                                        cropImageOptions = com.canhub.cropper.CropImageOptions(
                                                            guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                                                            fixAspectRatio = true,
                                                            aspectRatioX = 3,
                                                            aspectRatioY = 1,
                                                            outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
                                                            outputCompressQuality = 90
                                                        )
                                                    )
                                                )
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                            AsyncImage(
                                                model = work.imageUrl,
                                                contentDescription = work.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF007AFF))
                                                        .align(Alignment.TopEnd),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF9F9F9))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(work.title.ifBlank { "Creation" }, color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // Delete Flash Video Dialog
    if (flashToDelete != null) {
        val f = flashToDelete!!
        IosAlertDialog(
            title = "Delete Flash Video?",
            message = "Are you sure you want to permanently delete this Flash video? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                flashToDelete = null
                scope.launch {
                    val flashRepo = com.chroma.studio.data.FlashRepository()
                    val success = flashRepo.deleteFlash(f.id)
                    if (success) {
                        userFlashes = userFlashes.filter { it.id != f.id }
                        ToastManager.showToast("Flash video deleted")
                    } else {
                        ToastManager.showToast("Failed to delete video")
                    }
                }
            },
            dismissText = "Cancel",
            onDismiss = { flashToDelete = null },
            isDestructive = true
        )
    }
}
}

@Composable
fun AppleDetailTile(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconGradient: List<Color>,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bg.copy(alpha = 0.5f))
            .border(0.5.dp, colors.glassBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(iconGradient))
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = label.uppercase(),
                color = Color(0xFF8E8E93),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
            Text(
                text = value,
                color = colors.textMain,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppleHorizontalDetailChip(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconGradient: List<Color>,
    value: String
) {
    val colors = LocalOscellionColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(colors.bg.copy(alpha = 0.5f))
            .border(0.5.dp, colors.glassBorder.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.linearGradient(iconGradient))
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Text(
            text = value,
            color = colors.textMain,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AppleDetailRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    value: String
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(iconGradient))
                    .border(0.75.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Text(title, color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(value, color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppleSettingSheetItem(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(iconGradient))
                .border(0.75.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = titleColor ?: colors.textMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp
            )
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
    }
}



@Composable
fun AppleEmptyTabState(
    graphic: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    title: String,
    subtitle: String
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (graphic != null) {
            graphic()
        } else if (icon != null) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileFlashCard(
    flash: FlashVideo,
    isOwner: Boolean,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RectangleShape)
            .background(Color(0xFF1E1E1E))
    ) {
        if (flash.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = flash.thumbnailUrl,
                contentDescription = flash.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF262626)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Play, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCount(flash.likesCount),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isOwner) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF21C1C1E))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Flash", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(Lucide.Trash2, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileWorkCard(
    work: DiscoverPost,
    isLiked: Boolean,
    repository: WorkRepository,
    onClick: () -> Unit
) {
    var layers by remember(work.jsonUrl) { mutableStateOf<List<com.chroma.studio.model.GradientLayer>?>(LayerCache.get(work.jsonUrl)) }

    LaunchedEffect(work.jsonUrl) {
        if (layers == null) {
            layers = withContext(Dispatchers.IO) {
                repository.deserializeLayers(work.jsonUrl).also { LayerCache.put(work.jsonUrl, it) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RectangleShape)
            .background(Color.Gray.copy(alpha = 0.15f))
            .clickable { onClick() }
    ) {
        if (work.postType == "image" && !work.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = work.imageUrl,
                contentDescription = work.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (layers != null) {
            CanvasPreview(layers = layers!!, shape = "full", borderColor = Color.Transparent)
        }

        if (work.likesCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                    contentDescription = null,
                    tint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = work.likesCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (work.remixCount > 0) {
            Icon(
                Lucide.HeartHandshake,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
fun SkeletonUserProfile(onBack: () -> Unit) {
    val colors = LocalOscellionColors.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = colors.textMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colors.bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                SkeletonBanner()
                Row(
                    modifier = Modifier.padding(top = 118.dp, start = 16.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(colors.bg)
                                .border(4.dp, colors.bg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(84.dp).clip(CircleShape).shimmerEffect())
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.height(36.dp).width(100.dp).clip(RoundedCornerShape(18.dp)).shimmerEffect())
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.height(24.dp).width(150.dp).shimmerEffect())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.height(16.dp).width(100.dp).shimmerEffect())
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(16.dp).width(250.dp).shimmerEffect())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.height(16.dp).width(200.dp).shimmerEffect())
            }
        }
    }
}

@Composable
fun SkeletonBanner() {
    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
}

@Composable
fun SkeletonWorkCard() {
    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer_anim"
    )
    val colors = LocalOscellionColors.current

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                colors.glassBgHover.copy(alpha = 0.2f),
                colors.glassBgHover.copy(alpha = 0.6f),
                colors.glassBgHover.copy(alpha = 0.2f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}
