@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens
import android.graphics.Bitmap
import com.chroma.studio.ui.components.OneUiSettingsGroup
import com.chroma.studio.ui.components.OneUiSettingsNavRow
import com.chroma.studio.ui.components.OneUiIconBadge
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.components.ProfileCropTarget
import com.chroma.studio.ui.components.ProfileMediaEditorModal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.isSystemInDarkTheme
import com.chroma.studio.ui.components.AppleStatusBlueGradient
import com.chroma.studio.ui.components.StoryCreationBottomSheet
import com.chroma.studio.ui.components.ShareStoryAudienceSheet
import com.chroma.studio.ui.components.OnlyFriendsManagementSheet

import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.AppleWorksGraphic
import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.AppleRemixGraphic
import com.chroma.studio.ui.components.AppleHeartHandshakeGraphic
import com.chroma.studio.ui.components.AppleLockGraphic
import com.chroma.studio.ui.components.AppleSavedWorksGraphic
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.chroma.studio.ui.components.IosAvatarView
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
import com.google.firebase.firestore.FieldValue
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
    onLogout: (() -> Unit)? = null,
    onDownloadWork: (DiscoverPost) -> Unit,
    onRemixWork: (DiscoverPost) -> Unit,
    onNavigateToOriginal: (String, String) -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)? = null,
    onNavigateToChat: ((String) -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCreateStory: (() -> Unit)? = null,
    onStoryViewerChange: ((Boolean) -> Unit)? = null,
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
    var showStoryCreationSheet by remember { mutableStateOf(false) }
    var showStoryGalleryPicker by remember { mutableStateOf(false) }
    var showStoryCamera by remember { mutableStateOf(false) }
    var editingStoryImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadStoryUri by remember { mutableStateOf<Uri?>(null) }
    var showShareStoryAudienceSheet by remember { mutableStateOf(false) }
    var showOnlyFriendsManagementSheet by remember { mutableStateOf(false) }
    var storyViewerInitialIndex by remember { mutableIntStateOf(0) }

    val shouldHideBottomNav = showStoryViewer || showStoryCreationSheet || showStoryGalleryPicker || showStoryCamera || (editingStoryImageUri != null) || showShareStoryAudienceSheet || showOnlyFriendsManagementSheet
    LaunchedEffect(shouldHideBottomNav) {
        onStoryViewerChange?.invoke(shouldHideBottomNav)
    }

    var works by remember { mutableStateOf<List<DiscoverPost>>(emptyList()) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    val globalFollowingIds by com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds.collectAsState()
    val followerDeltas by com.chroma.studio.data.mutation.OptimisticFollowManager.followerCountDeltas.collectAsState()
    val isFollowingEffective = globalFollowingIds.contains(userId) || isFollowing
    val effectiveFollowerCount = maxOf(0, (profile?.followersCount ?: 0) + (followerDeltas[userId] ?: 0))
    val effectiveProfile = profile?.copy(followersCount = effectiveFollowerCount)
    var mutualFollowers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val likedWorkIds by discoverVm.likedWorkIds.collectAsState()
    val savedWorkIds by discoverVm.savedWorkIds.collectAsState()

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
    var showOtherUserOptionsSheet by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportUserSheet by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("Spam, Scam, or Fraud") }
    var isMutedStories by remember { mutableStateOf(false) }
    var savedWorks by remember { mutableStateOf<List<DiscoverPost>>(emptyList()) }
    var isSavedLoading by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var pendingEditorUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEditorTarget by remember { mutableStateOf(ProfileCropTarget.AVATAR) }

    val bannerGalleryLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingEditorUri = uri
            pendingEditorTarget = ProfileCropTarget.BANNER
        }
    }

    val avatarGalleryLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingEditorUri = uri
            pendingEditorTarget = ProfileCropTarget.AVATAR
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

        if (isCurrentUser && currentUserId != null) {
            launch {
                isSavedLoading = true
                try {
                    savedWorks = discoverVm.repository.getSavedWorks(currentUserId)
                } catch (_: Exception) {}
                isSavedLoading = false
            }
        }

        if (currentUserId != null && currentUserId != userId) {
            launch {
                val serverFollowing = discoverVm.repository.checkIsFollowing(currentUserId, userId)
                isFollowing = serverFollowing
                if (serverFollowing) {
                    com.chroma.studio.data.mutation.OptimisticFollowManager.seedFollowing(setOf(userId))
                }
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

    val isRestricted = profile?.isPrivateAccount == true && !isCurrentUser && !isFollowingEffective

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
    val profileTabIcons = if (isCurrentUser) listOf(Lucide.LayoutGrid, Lucide.Image, Lucide.Zap, Lucide.HeartHandshake, Lucide.Bookmark) else listOf(Lucide.LayoutGrid, Lucide.Image, Lucide.Zap, Lucide.HeartHandshake)

    val scrollProgress by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) 1f
            else (gridState.firstVisibleItemScrollOffset.toFloat() / 140f).coerceIn(0f, 1f)
        }
    }
    val isScrolled by remember {
        derivedStateOf { scrollProgress > 0.45f }
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
                    onPostsClick = {
                        selectedTabIndex = 0
                        scope.launch { gridState.animateScrollToItem(0) }
                    },
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
                    profile = effectiveProfile,
                    isCurrentUser = isCurrentUser,
                    isFollowing = isFollowingEffective,
                    mutualFollowers = mutualFollowers,
                    userId = userId,
                    onShowShareCard = { showShareCardModal = true },
                    onNavigateToUserList = onNavigateToUserList,
                    onNavigateToProfile = { targetUid -> onNavigateToOriginal(targetUid, "") }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                UserProfileActionsStoriesSection(
                    isCurrentUser = isCurrentUser,
                    isFollowing = isFollowingEffective,
                    userId = userId,
                    currentUserId = currentUserId,
                    profile = effectiveProfile,
                    filteredStories = filteredStories,
                    hasMyStory = isCurrentUser && (myStories.isNotEmpty() || filteredStories.isNotEmpty()),
                    profileTabIcons = profileTabIcons,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onToggleFollow = {
                        if (currentUserId == null) onRequireLogin()
                        else {
                            discoverVm.toggleFollow(userId, currentUserId, baseFollowing = isFollowingEffective)
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
                            showStoryCreationSheet = true
                        }
                    },
                    onAddStoryClick = {
                        showStoryCreationSheet = true
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
                val lockGraphicState = rememberAppleGraphicAnimationState()
                AppleEmptyTabState(
                    graphic = {
                        AppleLockGraphic(
                            modifier = Modifier.size(150.dp),
                            state = lockGraphicState,
                            enableClickToAnimate = true
                        )
                    },
                    title = "This Account is Private",
                    subtitle = "Follow this account to see their works, posts, and videos."
                )
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
                                        modifier = Modifier.size(130.dp),
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
                4 -> {
                    // Saved / Bookmarks Tab (Only visible to account owner)
                    if (isSavedLoading) {
                        items(6) {
                            Box(modifier = Modifier.aspectRatio(1f).clip(RectangleShape).shimmerEffect())
                        }
                    } else if (savedWorks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val savedGraphicState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    AppleSavedWorksGraphic(
                                        modifier = Modifier.size(150.dp),
                                        state = savedGraphicState,
                                        enableClickToAnimate = true
                                    )
                                },
                                title = "No Saved Works",
                                subtitle = "Works and posts you bookmark will appear here."
                            )
                        }
                    } else {
                        items(savedWorks.size, key = { savedWorks[it].id }) { index ->
                            val work = savedWorks[index]
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

    // Samsung One UI 7.0 Translucent Navigation Bar
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val topBarBg = if (isSysDark) Color(0xFF000000).copy(alpha = (scrollProgress * 0.94f).coerceIn(0f, 0.96f))
                   else Color(0xFFF2F4F7).copy(alpha = (scrollProgress * 0.96f).coerceIn(0f, 0.98f))
    val topBarBorder = if (isSysDark) Color(0xFF1F2430).copy(alpha = scrollProgress)
                       else Color(0xFFE2E8F0).copy(alpha = scrollProgress)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(topBarBg)
            .border(width = 0.5.dp, color = topBarBorder)
            .padding(top = statusBarTop + 4.dp, bottom = 8.dp, start = 14.dp, end = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val topIconBg = Color.Black.copy(alpha = 0.22f * (1f - scrollProgress))

            // Back button
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(topIconBg)
                        .clickable {
                            com.chroma.studio.chat.sound.SoundManager.playReaction()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = if (scrollProgress > 0.4f) (if (isSysDark) Color.White else Color(0xFF0F172A)) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(38.dp))
            }

            // Collapsed title when scrolled (smooth One UI typography slide & fade)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (scrollProgress > 0.08f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-1).dp),
                        modifier = Modifier.graphicsLayer {
                            alpha = ((scrollProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                            translationY = (1f - scrollProgress) * 8f
                        }
                    ) {
                        Text(
                            text = profile?.displayName ?: "",
                            color = if (isSysDark) Color.White else Color(0xFF0F172A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.5.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${profile?.username ?: ""}",
                            color = if (isSysDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.5.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Menu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCurrentUser) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(topIconBg)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                showBannerLibrarySheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isScrolled) {
                            Icon(
                                Lucide.Pencil,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(17.dp)
                                    .offset(x = 0.5.dp, y = 0.75.dp)
                            )
                        }
                        Icon(
                            Lucide.Pencil,
                            contentDescription = "Edit Banner",
                            tint = if (isScrolled) (if (isSysDark) Color.White else Color(0xFF0F172A)) else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(topIconBg)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                showSettingsBottomSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isScrolled) {
                            Icon(
                                Lucide.Settings,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(17.dp)
                                    .offset(x = 0.5.dp, y = 0.75.dp)
                            )
                        }
                        Icon(
                            Lucide.Settings,
                            contentDescription = "Settings",
                            tint = if (isScrolled) (if (isSysDark) Color.White else Color(0xFF0F172A)) else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(topIconBg)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                showShareCardModal = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.Share2,
                            contentDescription = "Share Card",
                            tint = if (isScrolled) (if (isSysDark) Color.White else Color(0xFF0F172A)) else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(topIconBg)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                showOtherUserOptionsSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.EllipsisVertical,
                            contentDescription = "More Options",
                            tint = if (isScrolled) (if (isSysDark) Color.White else Color(0xFF0F172A)) else Color.White,
                            modifier = Modifier.size(17.dp)
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
            val activeList = remember(selectedTabIndex, normalWorks, imagePosts, remixWorks, savedWorks, works) {
                when (selectedTabIndex) {
                    0 -> normalWorks
                    1 -> imagePosts
                    3 -> remixWorks
                    4 -> savedWorks
                    else -> works
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
                            isSaved = savedWorkIds.contains(work.id),
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
                                if (currentUserId != null) {
                                    discoverVm.trackDownload(work.id)
                                    onDownloadWork(work)
                                } else onRequireLogin()
                            },
                            onRemixWork = {
                                if (currentUserId != null) {
                                    discoverVm.trackRemix(work.id)
                                    onRemixWork(work)
                                } else onRequireLogin()
                            },
                            onRemixClick = {
                                if (currentUserId != null) {
                                    discoverVm.trackRemix(work.id)
                                    onRemixWork(work)
                                } else onRequireLogin()
                            },
                            onSaveClick = {
                                if (currentUserId != null) {
                                    discoverVm.toggleSave(work.id)
                                    if (selectedTabIndex == 4) {
                                        savedWorks = savedWorks.filter { it.id != work.id }
                                    }
                                } else onRequireLogin()
                            },
                            onShareClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out '${work.title}' by @${work.authorUsername} on Oscellion: https://oscellion.vercel.app/post/${work.id}")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Post"))
                                if (currentUserId != null) {
                                    discoverVm.trackShare(work.id)
                                }
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
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Profile Media Crop & Transform Editor Modal
    if (pendingEditorUri != null) {
        ProfileMediaEditorModal(
            imageUri = pendingEditorUri!!,
            target = pendingEditorTarget,
            onDismiss = { pendingEditorUri = null },
            onCropSuccess = { croppedUri ->
                val targetMode = pendingEditorTarget
                pendingEditorUri = null
                if (targetMode == ProfileCropTarget.AVATAR) {
                    scope.launch {
                        try {
                            isUploadingAvatar = true
                            val uid = currentUserId
                            if (uid != null) {
                                val url = com.chroma.studio.data.SupabaseMediaStorage.uploadAvatarFromUri(context, uid, croppedUri)
                                if (url != null) {
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid)
                                        .update("avatarUrl", url)
                                        .await()
                                    profile = profile?.copy(avatarUrl = url)
                                    ToastManager.showToast("Avatar updated successfully")
                                } else {
                                    ToastManager.showToast("Failed to upload avatar")
                                }
                            }
                        } catch (e: Exception) {
                            ToastManager.showToast("Error updating avatar: ${e.message ?: "Failed"}")
                        } finally {
                            isUploadingAvatar = false
                        }
                    }
                } else {
                    scope.launch {
                        try {
                            isUploadingBanner = true
                            val uid = currentUserId
                            if (uid != null) {
                                val url = com.chroma.studio.data.SupabaseMediaStorage.uploadBannerFromUri(context, uid, croppedUri)
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
                            ToastManager.showToast("Error updating banner: ${e.message ?: "Failed"}")
                        } finally {
                            isUploadingBanner = false
                            showBannerLibrarySheet = false
                        }
                    }
                }
            }
        )
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
            val cleanAvatar = profile?.avatarUrl.orEmpty().trim()
            val presetRes = remember(cleanAvatar) { getAvatarPresetDrawableRes(cleanAvatar) }
            val isCustomPhoto = cleanAvatar.isNotBlank() && !cleanAvatar.contains("default_avatar") && presetRes == null

            if (presetRes != null) {
                Image(
                    painter = painterResource(id = presetRes),
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (isCustomPhoto) {
                AsyncImage(
                    model = cleanAvatar,
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            } else {
                IosAvatarView(
                    name = profile?.displayName ?: profile?.username ?: "",
                    avatarUrl = "",
                    size = 280.dp,
                    gender = profile?.gender,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(1f)
                )
            }

            IconButton(
                onClick = { showAvatarOverlay = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarTop + 8.dp, start = 8.dp)
            ) {
                Icon(Lucide.ArrowLeft, contentDescription = "Close", tint = Color.White)
            }

            if (isCurrentUser) {
                ChromaPillButton(
                    onClick = {
                        showAvatarOverlay = false
                        avatarGalleryLauncher.launch("image/*")
                    },
                    variant = ChromaPillVariant.Blue,
                    enabled = !isUploadingAvatar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp)
                ) {
                    Icon(Lucide.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isUploadingAvatar) "Updating..." else "Change Avatar Photo",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
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

    // Story Creation Options Bottom Sheet
    if (showStoryCreationSheet) {
        StoryCreationBottomSheet(
            onDismissRequest = { showStoryCreationSheet = false },
            onChooseFromGallery = {
                showStoryCreationSheet = false
                showStoryGalleryPicker = true
            },
            onTextStory = {
                showStoryCreationSheet = false
                onNavigateToCreateStory?.invoke()
            }
        )
    }

    // Unified In-App Oscellion Gallery for Stories
    if (showStoryGalleryPicker) {
        com.chroma.studio.chat.ui.components.CustomGalleryPickerModal(
            onDismiss = { showStoryGalleryPicker = false },
            onLaunchCamera = {
                showStoryGalleryPicker = false
                showStoryCamera = true
            },
            onRequestMediaPermission = {},
            onSendMedia = { uris, _ ->
                showStoryGalleryPicker = false
                val pickedUri = uris.firstOrNull()
                if (pickedUri != null) {
                    editingStoryImageUri = pickedUri
                }
            }
        )
    }

    // Professional In-App Camera for Stories
    if (showStoryCamera) {
        com.chroma.studio.chat.ui.components.IosCameraScreen(
            onDismiss = { showStoryCamera = false },
            onPhotoCaptured = { uri, _ ->
                showStoryCamera = false
                editingStoryImageUri = uri
            },
            onVideoCaptured = { uri, _ ->
                showStoryCamera = false
                pendingUploadStoryUri = uri
                showShareStoryAudienceSheet = true
            }
        )
    }

    // Story Image Editor
    if (editingStoryImageUri != null) {
        FlashPhotoEditorScreen(
            photoUris = listOf(editingStoryImageUri!!),
            title = "Edit Story",
            initialAspectRatio = PhotoAspectRatio.STORY_9_16,
            nextButtonLabel = "Share Story",
            onExportUris = { editedUris ->
                val finalUri = editedUris.firstOrNull() ?: editingStoryImageUri!!
                pendingUploadStoryUri = finalUri
                showShareStoryAudienceSheet = true
            },
            onBack = {
                editingStoryImageUri = null
            }
        )
    }

    // Instagram-style Audience Selector for Story
    if (showShareStoryAudienceSheet && pendingUploadStoryUri != null) {
        val onlyFriends by storyVm.onlyFriendsIds.collectAsState()
        ShareStoryAudienceSheet(
            onDismissRequest = {
                showShareStoryAudienceSheet = false
            },
            onSharePublic = {
                showShareStoryAudienceSheet = false
                val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                pendingUploadStoryUri = null
                editingStoryImageUri = null
                storyVm.uploadStory(context, uri, audienceType = "public")
                com.chroma.studio.ui.components.ToastManager.showToast("Sharing your story... 🌟")
            },
            onShareOnlyFriends = {
                showShareStoryAudienceSheet = false
                val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                pendingUploadStoryUri = null
                editingStoryImageUri = null
                storyVm.uploadStory(context, uri, audienceType = "only_friends", customAudienceIds = onlyFriends)
                com.chroma.studio.ui.components.ToastManager.showToast("Shared with Only Friend! ⭐")
            },
            onEditOnlyFriends = {
                showOnlyFriendsManagementSheet = true
            },
            onlyFriendsCount = onlyFriends.size
        )
    }

    // Only Friend List Management Sheet
    if (showOnlyFriendsManagementSheet) {
        OnlyFriendsManagementSheet(
            storyViewModel = storyVm,
            onDismissRequest = { showOnlyFriendsManagementSheet = false }
        )
    }

    androidx.activity.compose.BackHandler(enabled = showStoryCamera) {
        showStoryCamera = false
    }
    androidx.activity.compose.BackHandler(enabled = editingStoryImageUri != null) {
        editingStoryImageUri = null
    }
    androidx.activity.compose.BackHandler(enabled = showStoryGalleryPicker) {
        showStoryGalleryPicker = false
    }
    androidx.activity.compose.BackHandler(enabled = showStoryCreationSheet) {
        showStoryCreationSheet = false
    }
    androidx.activity.compose.BackHandler(enabled = showStoryViewer) {
        showStoryViewer = false
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

    // Samsung One UI 7.0 Settings Bottom Sheet
    if (showSettingsBottomSheet) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            containerColor = if (isDark) Color(0xFF0F1117) else Color(0xFFF2F4F7),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Options & Settings",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                    IconButton(
                        onClick = { showSettingsBottomSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }

                OneUiSettingsGroup(title = "Account & Identity") {
                    OneUiSettingsNavRow(
                        title = "Account Settings",
                        subtitle = "Manage privacy, security & alerts",
                        icon = Lucide.UserCog,
                        iconColor = Color(0xFF2563EB),
                        onClick = {
                            showSettingsBottomSheet = false
                            onNavigateToSettings()
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Edit Identity & Avatar",
                        subtitle = "Change name, avatars, and bio",
                        icon = Lucide.UserCheck,
                        iconColor = Color(0xFF6366F1),
                        onClick = {
                            showSettingsBottomSheet = false
                            onNavigateToEditProfile()
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Profile Pass Card",
                        subtitle = "Share your scannable digital card",
                        icon = Lucide.QrCode,
                        iconColor = Color(0xFF10B981),
                        onClick = {
                            showSettingsBottomSheet = false
                            showShareCardModal = true
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Only Friend List",
                        subtitle = "Manage close friends (${storyVm.onlyFriendsIds.collectAsState().value.size})",
                        icon = Lucide.Star,
                        iconColor = Color(0xFFF59E0B),
                        onClick = {
                            showSettingsBottomSheet = false
                            showOnlyFriendsManagementSheet = true
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Saved Collection",
                        subtitle = "View bookmarked creations",
                        icon = Lucide.Bookmark,
                        iconColor = Color(0xFF8B5CF6),
                        onClick = {
                            showSettingsBottomSheet = false
                            selectedTabIndex = 4
                        }
                    )
                }

                OneUiSettingsGroup(title = "Share & Connect") {
                    OneUiSettingsNavRow(
                        title = "Share Profile",
                        subtitle = "Share your profile link with others",
                        icon = Lucide.Share2,
                        iconColor = Color(0xFF06B6D4),
                        onClick = {
                            showSettingsBottomSheet = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out my profile on Oscellion: https://oscellion.vercel.app/u/${profile?.username ?: ""}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Copy Profile Link",
                        subtitle = "oscellion.vercel.app/u/${profile?.username ?: ""}",
                        icon = Lucide.Copy,
                        iconColor = Color(0xFF64748B),
                        onClick = {
                            showSettingsBottomSheet = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("profile_link", "https://oscellion.vercel.app/u/${profile?.username ?: ""}"))
                            ToastManager.showToast("Profile link copied")
                        }
                    )
                }

                OneUiSettingsGroup(title = "Session") {
                    OneUiSettingsNavRow(
                        title = "Sign Out",
                        subtitle = "Log out from this device",
                        icon = Lucide.LogOut,
                        iconColor = Color(0xFFEF4444),
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

    // Samsung One UI 7.0 Sign Out Confirmation Dialog
    if (showSignOutConfirm) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = if (isDark) Color(0xFF12141A) else Color.White,
            tonalElevation = 6.dp,
            icon = {
                OneUiIconBadge(
                    icon = Lucide.LogOut,
                    iconColor = Color(0xFFEF4444)
                )
            },
            title = {
                Text(
                    text = "Sign Out",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out from this device?",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        if (onLogout != null) {
                            onLogout()
                        } else {
                            onRequireLogin()
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutConfirm = false },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2E384D) else Color(0xFFCBD5E1))
                ) {
                    Text("Cancel", fontFamily = InterFontFamily, color = if (isDark) Color.White else Color(0xFF0F172A))
                }
            }
        )
    }

    // Samsung One UI 7.0 Other User Options Bottom Sheet
    if (showOtherUserOptionsSheet) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        ModalBottomSheet(
            onDismissRequest = { showOtherUserOptionsSheet = false },
            containerColor = if (isDark) Color(0xFF0F1117) else Color(0xFFF2F4F7),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Profile Options",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                    IconButton(
                        onClick = { showOtherUserOptionsSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }

                OneUiSettingsGroup(title = "Actions") {
                    OneUiSettingsNavRow(
                        title = "Share Profile",
                        subtitle = "Share @${profile?.username ?: ""} with others",
                        icon = Lucide.Share2,
                        iconColor = Color(0xFF2563EB),
                        onClick = {
                            showOtherUserOptionsSheet = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out @${profile?.username ?: ""} on Oscellion: https://oscellion.vercel.app/u/${profile?.username ?: ""}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Copy Profile Link",
                        subtitle = "oscellion.vercel.app/u/${profile?.username ?: ""}",
                        icon = Lucide.Copy,
                        iconColor = Color(0xFF64748B),
                        onClick = {
                            showOtherUserOptionsSheet = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("profile_link", "https://oscellion.vercel.app/u/${profile?.username ?: ""}"))
                            ToastManager.showToast("Profile link copied")
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Profile Pass Card",
                        subtitle = "View scannable digital card",
                        icon = Lucide.QrCode,
                        iconColor = Color(0xFF10B981),
                        onClick = {
                            showOtherUserOptionsSheet = false
                            showShareCardModal = true
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Direct Message",
                        subtitle = "Open conversation with @${profile?.username ?: ""}",
                        icon = Lucide.MessageSquare,
                        iconColor = Color(0xFF06B6D4),
                        onClick = {
                            showOtherUserOptionsSheet = false
                            onNavigateToChat?.invoke(userId)
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = if (isMutedStories) "Unmute Stories" else "Mute Stories",
                        subtitle = if (isMutedStories) "Show stories in feed" else "Hide stories from feed",
                        icon = if (isMutedStories) Lucide.Bell else Lucide.BellOff,
                        iconColor = Color(0xFFF59E0B),
                        onClick = {
                            showOtherUserOptionsSheet = false
                            isMutedStories = !isMutedStories
                            ToastManager.showToast(if (isMutedStories) "Stories from @${profile?.username ?: ""} muted" else "Stories unmuted")
                        }
                    )
                }

                OneUiSettingsGroup(title = "Safety & Reporting") {
                    OneUiSettingsNavRow(
                        title = "Block User",
                        subtitle = "Prevent @${profile?.username ?: ""} from contacting you",
                        icon = Lucide.Ban,
                        iconColor = Color(0xFFEF4444),
                        isDestructive = true,
                        onClick = {
                            showOtherUserOptionsSheet = false
                            showBlockConfirmDialog = true
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    OneUiSettingsNavRow(
                        title = "Report Account",
                        subtitle = "Report spam, abuse, or safety violation",
                        icon = Lucide.Flag,
                        iconColor = Color(0xFFEF4444),
                        isDestructive = true,
                        onClick = {
                            showOtherUserOptionsSheet = false
                            showReportUserSheet = true
                        }
                    )
                }
            }
        }
    }

    // Samsung One UI 7.0 Block Confirmation Dialog
    if (showBlockConfirmDialog) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = if (isDark) Color(0xFF12141A) else Color.White,
            tonalElevation = 6.dp,
            icon = {
                OneUiIconBadge(
                    icon = Lucide.Ban,
                    iconColor = Color(0xFFEF4444)
                )
            },
            title = {
                Text(
                    text = "Block @${profile?.username ?: "User"}?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "They will not be able to message you or view your profile and posts. They will not be notified that you blocked them.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        val uid = currentUserId ?: return@Button
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .update("blockedUserIds", FieldValue.arrayUnion(userId)).await()
                                ToastManager.showToast("@${profile?.username ?: ""} has been blocked")
                                isFollowing = false
                            } catch (e: Exception) {
                                ToastManager.showToast("Failed to block user")
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Block", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBlockConfirmDialog = false },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2E384D) else Color(0xFFCBD5E1))
                ) {
                    Text("Cancel", fontFamily = InterFontFamily, color = if (isDark) Color.White else Color(0xFF0F172A))
                }
            }
        )
    }

    // Samsung One UI 7.0 Report User Bottom Sheet
    if (showReportUserSheet) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        ModalBottomSheet(
            onDismissRequest = { showReportUserSheet = false },
            containerColor = if (isDark) Color(0xFF0F1117) else Color(0xFFF2F2F7),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Report @${profile?.username ?: ""}",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            "Why are you reporting this account?",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    IconButton(
                        onClick = { showReportUserSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }

                val reportReasons = listOf(
                    "Spam, Scam, or Fraud",
                    "Harassment, Bullying, or Hate Speech",
                    "Inappropriate or Sensitive Media",
                    "Impersonation or False Identity",
                    "Violence or Dangerous Activity"
                )

                OneUiSettingsGroup(title = "Report Reason") {
                    reportReasons.forEachIndexed { idx, reason ->
                        val isSelected = selectedReportReason == reason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportReason = reason }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reason,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReportReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF2563EB),
                                    unselectedColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                                )
                            )
                        }
                        if (idx < reportReasons.lastIndex) {
                            HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                    }
                }

                Button(
                    onClick = {
                        showReportUserSheet = false
                        val uid = currentUserId ?: "anonymous"
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance().collection("reports").add(
                                    mapOf(
                                        "reporterId" to uid,
                                        "reportedUserId" to userId,
                                        "reportedUsername" to (profile?.username ?: ""),
                                        "reason" to selectedReportReason,
                                        "type" to "user_profile",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                ).await()
                                ToastManager.showToast("Report submitted. Thank you for keeping Chroma safe.")
                            } catch (_: Exception) {
                                ToastManager.showToast("Failed to submit report")
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Submit Report", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                }
            }
        }
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
                                            Icon(
                                                Lucide.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(18.dp)
                                            )
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
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val photosState = rememberAppleGraphicAnimationState()
                                AppleEmptyTabState(
                                    graphic = {
                                        ApplePhotosGraphic(
                                            modifier = Modifier.size(90.dp),
                                            state = photosState,
                                            enableClickToAnimate = true
                                        )
                                    },
                                    title = "No Image Works",
                                    subtitle = "Publish a work with an image to set it as banner."
                                )
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
                                                 pendingEditorUri = Uri.parse(workImg)
                                                 pendingEditorTarget = ProfileCropTarget.BANNER
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
                                                Icon(
                                                    Lucide.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF007AFF),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(18.dp)
                                                )
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
            .clip(RoundedCornerShape(9999.dp))
            .background(colors.bg.copy(alpha = 0.5f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(iconGradient))
                .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
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
            Text(title, color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
            .padding(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (graphic != null) {
            graphic()
        } else if (icon != null) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
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
    val colors = LocalOscellionColors.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RectangleShape)
            .background(colors.glassBgHover)
            .border(0.5.dp, colors.glassBorder, RectangleShape)
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = colors.textMuted.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Top Reel Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "REEL",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    letterSpacing = 0.5.sp
                )
            }

            if (isOwner) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
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
                        .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassBg)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Flash", color = colors.error, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(Lucide.Trash2, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
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
