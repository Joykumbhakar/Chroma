package com.chroma.studio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.chroma.studio.ui.components.FlatDoubleTapHeartOverlay
import com.chroma.studio.ui.components.rememberDoubleTapLikeState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.IosAvatarView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.chroma.studio.model.FlashDraft
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.components.IosAlertDialog
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import com.chroma.studio.ui.components.ShareFlashToChatSheet
import com.google.firebase.auth.FirebaseAuth
import com.chroma.studio.utils.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlashFeedScreen(
    flashVm: FlashViewModel,
    commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel,
    onNavigateToOldHome: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToFlashPicker: () -> Unit,
    onRemixRequested: (Uri, String, String) -> Unit,
    onRequireLogin: () -> Unit,
    emptyContent: (@Composable () -> Unit)? = null,
    initialVideoId: String? = null,
    onRemixVideo: ((FlashVideo) -> Unit)? = null,
    chatViewModel: com.chroma.studio.viewmodel.ChatViewModel? = null,
    onNavigateToPost: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val effectiveChatVm = chatViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.ChatViewModel>()

    val flashFeed by flashVm.flashFeed.collectAsState()
    val isLoading by flashVm.isLoading.collectAsState()
    val isRefreshing by flashVm.isRefreshing.collectAsState()
    val likedVideoIds by flashVm.likedVideoIds.collectAsState()
    val drafts by flashVm.drafts.collectAsState()
    val followingUserIds by com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds.collectAsState()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    var selectedTab by remember { mutableStateOf("For You") }
    var shareVideoTarget by remember { mutableStateOf<FlashVideo?>(null) }
    val hiddenVideoIds = remember { mutableStateListOf<String>() }

    fun requireSignedIn(): Boolean {
        val signedIn = FirebaseAuth.getInstance().currentUser != null &&
            !com.chroma.studio.data.SavedAccountManager.isGuest(context)
        if (!signedIn) onRequireLogin()
        return signedIn
    }

    val displayedFeed = remember(flashFeed, selectedTab, followingUserIds, currentUid, hiddenVideoIds.size) {
        val visible = flashFeed.filterNot { it.id in hiddenVideoIds }
        if (selectedTab == "Following") {
            visible.filter { v ->
                v.creatorId == currentUid ||
                    v.creatorId in followingUserIds ||
                    com.chroma.studio.data.mutation.FollowingLocalStorageManager.isFollowing(v.creatorId)
            }
        } else {
            visible
        }
    }

    val initialPage = remember(initialVideoId, displayedFeed) {
        if (!initialVideoId.isNullOrBlank()) {
            val idx = displayedFeed.indexOfFirst { it.id == initialVideoId }
            if (idx >= 0) idx else 0
        } else {
            0
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { displayedFeed.size.coerceAtLeast(1) }
    )

    BackHandler {
        onNavigateToOldHome()
    }

    LaunchedEffect(initialVideoId) {
        if (!initialVideoId.isNullOrBlank()) {
            flashVm.ensureFlashInFeed(initialVideoId)
        }
    }

    LaunchedEffect(initialVideoId, displayedFeed) {
        if (!initialVideoId.isNullOrBlank() && displayedFeed.isNotEmpty()) {
            val idx = displayedFeed.indexOfFirst { it.id == initialVideoId }
            if (idx >= 0 && pagerState.currentPage != idx) {
                pagerState.scrollToPage(idx)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (displayedFeed.isNotEmpty() && pagerState.currentPage >= displayedFeed.size) {
            pagerState.scrollToPage(0)
        }
    }

    // ExoPlayer Pool of 3 recycled players using CacheDataSource with proper AudioAttributes and AudioFocus
    val exoPlayers = remember {
        val cache = com.chroma.studio.utils.MediaCacheManager.getInstance(context)
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val poolSize = if (com.chroma.studio.utils.DevicePerformanceManager.isLowEndDevice(context)) 2 else 3
        List(poolSize) {
            androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setLoadControl(com.chroma.studio.utils.DevicePerformanceManager.createOptimizedLoadControl(context))
                .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory))
                .setAudioAttributes(audioAttributes, true)
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                    volume = 1f
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayers.forEach { it.release() } }
    }

    // Drafts bottom sheet
    var showDraftSheet by remember { mutableStateOf(false) }
    var draftToDelete by remember { mutableStateOf<FlashDraft?>(null) }

    // Global mute state
    var isMuted by remember { mutableStateOf(false) }

    // Sync all pooled players with global mute state
    LaunchedEffect(isMuted) {
        exoPlayers.forEach { it.volume = if (isMuted) 0f else 1f }
    }

    // Load more when near end and Trigger AOT Prefetching
    LaunchedEffect(pagerState.currentPage, displayedFeed.size) {
        if (selectedTab == "For You" && pagerState.currentPage >= displayedFeed.size - 3) {
            flashVm.loadMoreFeed()
        }

        // AOT Prefetching for the next 2-3 videos
        for (i in 1..3) {
            val idx = pagerState.currentPage + i
            if (idx < displayedFeed.size) {
                val url = displayedFeed[idx].videoUrl
                launch(Dispatchers.IO) {
                    com.chroma.studio.utils.VideoPrefetcher.prefetchVideo(context, url)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading && flashFeed.isEmpty()) {
            FlashFeedSkeleton()
        } else if (displayedFeed.isEmpty()) {
            if (selectedTab == "Following" && flashFeed.isNotEmpty()) {
                FollowingFlashEmptyState(
                    onExploreForYou = { selectedTab = "For You" },
                    onRefresh = { flashVm.loadFeed(isRefresh = true) }
                )
            } else if (emptyContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(if (colors.isDark) Color(0xFF09090B) else Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.Gray.copy(alpha = 0.35f))
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            emptyContent()
                        }
                    }
                }
            } else {
                FlashEmptyState(
                    onUpload = {
                        if (requireSignedIn()) onNavigateToFlashPicker()
                    }
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                key = { page -> displayedFeed.getOrNull(page)?.id ?: page },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = displayedFeed.getOrNull(page)
                if (video != null) {
                    FlashVideoItem(
                        player = exoPlayers[page % exoPlayers.size],
                        video = video,
                        isActive = pagerState.currentPage == page && shareVideoTarget == null,
                        isLiked = video.id in likedVideoIds,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        onRequireLogin = onRequireLogin,
                        onLike = {
                            if (requireSignedIn()) {
                                flashVm.toggleLike(video.id)
                            }
                        },
                        onShare = {
                            shareVideoTarget = video
                        },
                        onAuthorClick = { onNavigateToProfile(video.creatorId) },
                        onHideVideo = {
                            hiddenVideoIds.add(video.id)
                            ToastManager.showToast("Hidden from your Flash feed")
                        },
                        flashVm = flashVm,
                        commentVm = commentVm,
                        onRemixRequested = onRemixRequested,
                        onNavigateToFlashPicker = {
                            if (requireSignedIn()) onNavigateToFlashPicker()
                        },
                        onNavigateToPost = onNavigateToPost,
                        onRemixVideo = onRemixVideo
                    )
                }
            }
        }

        // ── Top Studio Header (For You / Following + Clean Controls) ──────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.68f),
                            Color.Black.copy(alpha = 0.24f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToOldHome() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Following", "For You").forEach { tab ->
                    val selected = selectedTab == tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.56f),
                            fontSize = 15.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 20.dp else 0.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (drafts.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDraftSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.FileVideo, "Drafts", tint = Color.White, modifier = Modifier.size(19.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0381FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${drafts.size}", fontSize = 8.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable {
                            if (requireSignedIn()) onNavigateToFlashPicker()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Camera,
                        contentDescription = "Create Flash",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Refresh indicator
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                OscellionLoadingSpinner(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }

    // Drafts bottom sheet
    if (showDraftSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDraftSheet = false },
            containerColor = if (colors.isDark) Color(0xFF13131A) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.cardBorder) },
            windowInsets = WindowInsets(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                Text(
                    "Drafts",
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                drafts.forEach { draft ->
                    DraftItem(
                        draft = draft,
                        onResume = {
                            showDraftSheet = false
                            if (requireSignedIn()) onNavigateToFlashPicker()
                        },
                        onDelete = { draftToDelete = draft }
                    )
                }
            }
        }
    }

    if (draftToDelete != null) {
        val draft = draftToDelete!!
        IosAlertDialog(
            title = "Delete Draft?",
            message = "Are you sure you want to delete this Flash video draft? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                val toDelete = draft
                draftToDelete = null
                flashVm.deleteDraft(toDelete.id)
                ToastManager.showToast("Draft deleted")
            },
            dismissText = "Cancel",
            onDismiss = { draftToDelete = null }
        )
    }

    shareVideoTarget?.let { videoToShare ->
        ShareFlashToChatSheet(
            flashVideo = videoToShare,
            chatViewModel = effectiveChatVm,
            onDismissRequest = { shareVideoTarget = null },
            onExternalShare = {
                flashVm.shareFlash(videoToShare.id)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this Flash on Oscellion: https://oscellion.app/flash/${videoToShare.id}")
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
            },
            onShareSuccess = {
                flashVm.shareFlash(videoToShare.id)
            }
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FlashVideoItem(
    player: androidx.media3.exoplayer.ExoPlayer,
    video: FlashVideo,
    isActive: Boolean,
    isLiked: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onAuthorClick: () -> Unit,
    onHideVideo: () -> Unit,
    flashVm: FlashViewModel,
    commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel,
    onRemixRequested: (Uri, String, String) -> Unit,
    onNavigateToFlashPicker: () -> Unit = {},
    onNavigateToPost: ((String) -> Unit)? = null,
    onRemixVideo: ((FlashVideo) -> Unit)? = null,
    onRequireLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }

    val followingUserIds by com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds.collectAsState()
    val isFollowing = followingUserIds.contains(video.creatorId) || com.chroma.studio.data.mutation.FollowingLocalStorageManager.isFollowing(video.creatorId)
    val isSaved = auth.currentUser?.uid?.let { video.savedBy.contains(it) } ?: false
    var showComments by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }

    val currentUserAvatar = remember(context, auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        val saved = uid?.let { u -> com.chroma.studio.data.SavedAccountManager.getSavedAccounts(context).firstOrNull { it.uid == u } }
        saved?.avatarUrl?.takeIf { it.isNotBlank() } ?: auth.currentUser?.photoUrl?.toString().orEmpty()
    }
    val currentUserName = remember(context, auth.currentUser?.uid) {
        auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "You"
    }

    fun requireSignedIn(): Boolean {
        val signedIn = auth.currentUser != null && !com.chroma.studio.data.SavedAccountManager.isGuest(context)
        if (!signedIn) onRequireLogin()
        return signedIn
    }

    var liveCreatorProfile by remember(video.creatorId) { mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
    val resolvedCreatorAvatar = liveCreatorProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: video.authorAvatarUrl
    val resolvedCreatorDisplayName = liveCreatorProfile?.displayName?.takeIf { it.isNotBlank() } ?: video.authorDisplayName.ifBlank { video.authorUsername }
    val resolvedCreatorUsername = liveCreatorProfile?.username?.takeIf { it.isNotBlank() } ?: video.authorUsername
    val resolvedCreatorVerified = liveCreatorProfile?.verified ?: video.authorVerified
    val resolvedCreatorVerifiedTone = liveCreatorProfile?.verifiedTone ?: video.authorVerifiedTone

    LaunchedEffect(video.creatorId) {
        if (video.creatorId.isNotBlank()) {
            liveCreatorProfile = discoverRepo.getUserProfile(video.creatorId)
        }
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank() && currentUid != video.creatorId && !com.chroma.studio.data.mutation.OptimisticFollowManager.isFollowing(video.creatorId)) {
            val serverFollowing = discoverRepo.checkIsFollowing(currentUid, video.creatorId)
            if (serverFollowing) {
                com.chroma.studio.data.mutation.OptimisticFollowManager.seedFollowing(setOf(video.creatorId), currentUid)
            } else if (!com.chroma.studio.data.mutation.OptimisticFollowManager.hasPendingMutation(video.creatorId, currentUid)) {
                com.chroma.studio.data.mutation.OptimisticFollowManager.removeFollowing(video.creatorId, currentUid)
            }
        }
    }

    val colors = LocalOscellionColors.current
    val doubleTapLikeState = rememberDoubleTapLikeState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPlaying by remember { mutableStateOf(false) }
    var userPaused by remember(video.id, isActive) { mutableStateOf(false) }
    var overlayShowsPauseIcon by remember { mutableStateOf(false) }
    var showPlayPauseOverlay by remember { mutableStateOf(false) }
    var is2xSpeed by remember(video.id, isActive) { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val currentUserUid = auth.currentUser?.uid
    val isOwner = currentUserUid == video.creatorId
    var localCollabs by remember(video.id, video.collaborators) { mutableStateOf(video.collaborators) }
    var localPendingCollabIds by remember(video.id, video.pendingCollaboratorIds) { mutableStateOf(video.pendingCollaboratorIds) }
    var localAcceptedCollabIds by remember(video.id, video.collaboratorIds) { mutableStateOf(video.collaboratorIds) }
    var isRespondingCollab by remember(video.id) { mutableStateOf(false) }
    var showCollabSheet by remember(video.id) { mutableStateOf(false) }
    var showStopSharingConfirm by remember(video.id) { mutableStateOf(false) }
    var showManageCollabPicker by remember(video.id) { mutableStateOf(false) }
    var showAudioDetailModal by remember(video.id) { mutableStateOf(false) }
    val acceptedCollaborators = remember(localCollabs, localAcceptedCollabIds) {
        com.chroma.studio.model.resolveAcceptedCollaborators(localCollabs, localAcceptedCollabIds)
    }
    val isPendingInvitee = !currentUserUid.isNullOrBlank() && localPendingCollabIds.contains(currentUserUid)
    val isAcceptedCollaborator = !currentUserUid.isNullOrBlank() && localAcceptedCollabIds.contains(currentUserUid)
    var currentPosition by remember(video.id) { mutableLongStateOf(0L) }
    var videoDuration by remember(video.id) { mutableLongStateOf(video.duration.coerceAtLeast(1L)) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var scrubberWidthPx by remember { mutableFloatStateOf(1f) }

    var isPlayerReady by remember(video.videoUrl, player) { mutableStateOf(false) }
    var isBuffering by remember(video.videoUrl, player) { mutableStateOf(false) }
    var showBufferingSpinner by remember { mutableStateOf(false) }
    var hasPlaybackError by remember(video.videoUrl, player) { mutableStateOf(false) }
    var videoRatio by remember(video.videoUrl) { mutableFloatStateOf(9f / 16f) }
    var attachedTextureView by remember { mutableStateOf<android.view.TextureView?>(null) }

    LaunchedEffect(isBuffering, isActive, isPlayerReady, hasPlaybackError) {
        if (isBuffering && isActive && !isPlayerReady && !hasPlaybackError) {
            delay(350)
            showBufferingSpinner = true
        } else {
            showBufferingSpinner = false
        }
    }

    // Prepare the pooled player cleanly only when its URI differs or needs preparation
    LaunchedEffect(video.videoUrl, player, isActive) {
        if (isActive || player.currentMediaItem?.localConfiguration?.uri?.toString() != video.videoUrl) {
            val loadedUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (loadedUri != video.videoUrl) {
                isPlayerReady = false
                hasPlaybackError = false
                player.setMediaItem(androidx.media3.common.MediaItem.fromUri(video.videoUrl))
                player.prepare()
            } else if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                hasPlaybackError = false
                player.prepare()
            }
            if (isActive) {
                attachedTextureView?.let { tv ->
                    player.setVideoTextureView(tv)
                }
            }
        }
    }

    // Track app lifecycle to pause video when app goes to background
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var lifecycleEvent by remember { mutableStateOf(androidx.lifecycle.Lifecycle.Event.ON_ANY) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            lifecycleEvent = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Sync mute cleanly without resetting user pause state
    LaunchedEffect(isMuted, player) {
        player.volume = if (isMuted) 0f else 1f
    }

    // Sync 2x speed cleanly
    LaunchedEffect(is2xSpeed, isActive, player) {
        player.setPlaybackSpeed(if (isActive && is2xSpeed) 2.0f else 1.0f)
    }

    val isLifecycleActive = lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_RESUME ||
        lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_START ||
        lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_ANY

    val flashMusicUrl = remember(video.musicAudioUrl) {
        com.chroma.studio.data.normalizeMusicStreamUrl(video.musicAudioUrl)
    }
    val needsSyncedFlashMusic = !video.musicMixedInVideo && flashMusicUrl.isNotBlank()
    val flashMusicPlayer = remember(video.id) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            volume = 1f
        }
    }
    DisposableEffect(flashMusicPlayer) {
        onDispose {
            try {
                flashMusicPlayer.stop()
                flashMusicPlayer.release()
            } catch (_: Throwable) {}
        }
    }
    LaunchedEffect(video.id, flashMusicUrl, needsSyncedFlashMusic, video.musicStartSec, isActive) {
        if (isActive && needsSyncedFlashMusic) {
            try {
                if (flashMusicPlayer.currentMediaItem?.localConfiguration?.uri?.toString() != flashMusicUrl) {
                    flashMusicPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(flashMusicUrl))
                    flashMusicPlayer.prepare()
                }
                flashMusicPlayer.seekTo((video.musicStartSec * 1000L).coerceAtLeast(0L))
            } catch (_: Throwable) {}
        } else {
            try {
                flashMusicPlayer.pause()
            } catch (_: Throwable) {}
        }
    }
    LaunchedEffect(isActive, needsSyncedFlashMusic, userPaused, isMuted, isLifecycleActive, video.musicStartSec, video.musicDurationSec) {
        if (isActive && needsSyncedFlashMusic && !userPaused && !isMuted && isLifecycleActive) {
            try {
                flashMusicPlayer.volume = 1f
                flashMusicPlayer.playWhenReady = true
                if (!flashMusicPlayer.isPlaying) flashMusicPlayer.play()
                val durSec = video.musicDurationSec
                if (durSec > 0) {
                    val startMs = (video.musicStartSec * 1000L).coerceAtLeast(0L)
                    val endMs = startMs + durSec * 1000L
                    while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
                        delay(250L)
                        if (flashMusicPlayer.isPlaying && flashMusicPlayer.currentPosition >= endMs) {
                            flashMusicPlayer.seekTo(startMs)
                        }
                    }
                }
            } catch (_: Throwable) {}
        } else {
            try {
                flashMusicPlayer.playWhenReady = false
                if (flashMusicPlayer.isPlaying) flashMusicPlayer.pause()
            } catch (_: Throwable) {}
        }
    }

    // Play/Pause logic respecting active pager item, lifecycle, and explicit user pause
    LaunchedEffect(isActive, lifecycleEvent, userPaused, player) {
        val canRunLifecycle = lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_RESUME ||
            lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_START ||
            lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_ANY
        if (isActive && !userPaused && canRunLifecycle) {
            attachedTextureView?.let { player.setVideoTextureView(it) }
            player.volume = if (isMuted) 0f else 1f
            player.play()
        } else {
            player.pause()
        }
    }

    // Attach listener cleanly for the active item/player pair
    DisposableEffect(player, video.videoUrl, isActive) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    isPlayerReady = true
                    hasPlaybackError = false
                }
            }
            override fun onRenderedFirstFrame() {
                isPlayerReady = true
                isBuffering = false
                hasPlaybackError = false
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasPlaybackError = true
                isBuffering = false
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoRatio = (videoSize.width.toFloat() / videoSize.height.toFloat()) * videoSize.pixelWidthHeightRatio
                }
            }
        }
        player.addListener(listener)
        if (isActive && player.playbackState == androidx.media3.common.Player.STATE_READY) {
            isPlayerReady = true
            isPlaying = player.isPlaying
        }
        onDispose {
            player.removeListener(listener)
            if (isActive) {
                player.setPlaybackSpeed(1.0f)
                player.pause()
            }
        }
    }

    LaunchedEffect(player, isPlaying, isActive, isScrubbing) {
        while (isActive && (isPlaying || isScrubbing)) {
            if (!isScrubbing) {
                currentPosition = player.currentPosition
                videoDuration = player.duration.coerceAtLeast(1L)
            }
            delay(50)
        }
    }

    val playPauseScale by animateFloatAsState(
        targetValue = if (showPlayPauseOverlay) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playPauseScale"
    )
    val playPauseAlpha by animateFloatAsState(
        targetValue = if (showPlayPauseOverlay) 1f else 0f,
        animationSpec = tween(260),
        label = "playPauseAlpha"
    )

    LaunchedEffect(showPlayPauseOverlay) {
        if (showPlayPauseOverlay) {
            delay(550)
            showPlayPauseOverlay = false
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "likeScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id, isActive) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        doubleTapLikeState.trigger(offset)
                        if (requireSignedIn()) {
                            flashVm.toggleLike(video.id, forceLike = true)
                        }
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onLongPress = {
                        if (isActive && isPlaying) {
                            is2xSpeed = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (is2xSpeed) {
                            is2xSpeed = false
                        }
                    },
                    onTap = {
                        val nextPaused = !userPaused
                        userPaused = nextPaused
                        overlayShowsPauseIcon = nextPaused
                        if (nextPaused) {
                            player.pause()
                        } else {
                            attachedTextureView?.let { player.setVideoTextureView(it) }
                            player.play()
                        }
                        showPlayPauseOverlay = true
                    }
                )
            }
    ) {
        // Video player (Full Screen Crop/Fill)
        AndroidView(
            factory = { ctx ->
                val frameLayout = androidx.media3.ui.AspectRatioFrameLayout(ctx)
                frameLayout.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                val textureView = android.view.TextureView(ctx)
                frameLayout.addView(
                    textureView,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                frameLayout.tag = textureView
                frameLayout
            },
            update = { view ->
                val frameLayout = view as androidx.media3.ui.AspectRatioFrameLayout
                val textureView = frameLayout.tag as android.view.TextureView
                attachedTextureView = textureView
                if (isActive) {
                    player.setVideoTextureView(textureView)
                }
                frameLayout.setAspectRatio(videoRatio)
            },
            onRelease = { view ->
                val frameLayout = view as? androidx.media3.ui.AspectRatioFrameLayout
                val textureView = frameLayout?.tag as? android.view.TextureView
                if (textureView != null) {
                    player.clearVideoTextureView(textureView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Thumbnail overlay until first rendered frame
        androidx.compose.animation.AnimatedVisibility(
            visible = !isPlayerReady,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            if (video.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom dark gradient overlay for crystal-clear readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Buffering spinner (only when stalled >350ms)
        if (showBufferingSpinner) {
            OscellionLoadingSpinner(
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
            )
        }

        // Error retry pill
        if (hasPlaybackError) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable {
                        hasPlaybackError = false
                        isPlayerReady = false
                        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(video.videoUrl))
                        player.prepare()
                        player.play()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Lucide.RefreshCw, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                Text(
                    text = "Video unavailable · Tap to retry",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Pending Collaborator Invite Banner
        if (isPendingInvitee && currentUserUid != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
            ) {
                com.chroma.studio.ui.components.PendingCollabReviewStrip(
                    creatorHandle = resolvedCreatorUsername,
                    isProcessing = isRespondingCollab,
                    darkSurface = true,
                    onAccept = {
                        isRespondingCollab = true
                        coroutineScope.launch {
                            val ok = discoverRepo.respondToCollabInvite(video.id, accept = true)
                            if (ok) {
                                val nextState = com.chroma.studio.model.applyCollabInviteResponse(
                                    participants = localCollabs,
                                    pendingIds = localPendingCollabIds,
                                    acceptedIds = localAcceptedCollabIds,
                                    responderUid = currentUserUid,
                                    accept = true
                                )
                                localCollabs = nextState.collaborators
                                localPendingCollabIds = nextState.pendingCollaboratorIds
                                localAcceptedCollabIds = nextState.collaboratorIds
                                ToastManager.showToast("Joined collaboration!")
                            } else {
                                ToastManager.showToast("Could not accept invite")
                            }
                            isRespondingCollab = false
                        }
                    },
                    onDecline = {
                        isRespondingCollab = true
                        coroutineScope.launch {
                            val ok = discoverRepo.respondToCollabInvite(video.id, accept = false)
                            if (ok) {
                                val nextState = com.chroma.studio.model.applyCollabInviteResponse(
                                    participants = localCollabs,
                                    pendingIds = localPendingCollabIds,
                                    acceptedIds = localAcceptedCollabIds,
                                    responderUid = currentUserUid,
                                    accept = false
                                )
                                localCollabs = nextState.collaborators
                                localPendingCollabIds = nextState.pendingCollaboratorIds
                                localAcceptedCollabIds = nextState.collaboratorIds
                                ToastManager.showToast("Invite declined")
                            }
                            isRespondingCollab = false
                        }
                    }
                )
            }
        }

        // 2x Speed indicator during long-press
        AnimatedVisibility(
            visible = is2xSpeed,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Lucide.Zap, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(
                    text = "2x Speed",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center Play / Pause Animated Feedback
        if (showPlayPauseOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .scale(playPauseScale)
                    .graphicsLayer { alpha = playPauseAlpha },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (overlayShowsPauseIcon) Lucide.Pause else Lucide.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Double-tap heart animation
        FlatDoubleTapHeartOverlay(
            state = doubleTapLikeState,
            modifier = Modifier.fillMaxSize(),
            heartSize = 110.dp
        )

        val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val overlayAlpha by animateFloatAsState(
            targetValue = if (isScrubbing || is2xSpeed) 0.25f else 1f,
            animationSpec = tween(180),
            label = "hudDimAlpha"
        )

        // ── Right-Side Vertical Action Column ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 74.dp + navBarBottomDp)
                .graphicsLayer { alpha = overlayAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReelsActionButton(
                icon = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                tint = if (isLiked) Color(0xFFF43F5E) else Color.White,
                label = if (video.hideLikesCount) "" else formatCount(maxOf(video.likesCount, video.likedBy.size)),
                scale = likeScale,
                onClick = onLike
            )

            if (!video.turnOffComments) {
                ReelsActionButton(
                    icon = Lucide.MessageCircle,
                    tint = Color.White,
                    label = formatCount(video.commentCount),
                    onClick = {
                        if (requireSignedIn()) {
                            showComments = true
                        }
                    }
                )
            }

            if (!video.turnOffRemix) {
                ReelsActionButton(
                    icon = Lucide.HeartHandshake,
                    tint = Color.White,
                    label = formatCount(video.remixCount),
                    onClick = {
                        if (!requireSignedIn()) return@ReelsActionButton
                        if (onRemixVideo != null) {
                            onRemixVideo(video)
                        } else {
                            ToastManager.showToast("Preparing remix…")
                            flashVm.downloadVideoForRemix(
                                context = context,
                                videoUrl = video.videoUrl,
                                onProgress = { },
                                onComplete = { uri ->
                                    if (uri != null) {
                                        onRemixRequested(uri, video.id, video.authorUsername)
                                    } else {
                                        ToastManager.showToast("Failed to download video")
                                    }
                                }
                            )
                        }
                    }
                )
            }

            ReelsActionButton(
                icon = Lucide.Send,
                tint = Color.White,
                label = formatCount(video.shareCount),
                onClick = onShare
            )

            ReelsActionButton(
                icon = Lucide.Bookmark,
                tint = if (isSaved) Color(0xFFFBBF24) else Color.White,
                label = formatCount(video.saveCount),
                onClick = {
                    if (requireSignedIn()) flashVm.toggleSave(video.id)
                }
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { showMoreOptionsSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Ellipsis,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Audio Disc Squircle (Borderless, clean contrast — tappable when curated track is attached)
            val hasAttachedTrack = !video.musicTrackId.isNullOrBlank() || !video.musicTitle.isNullOrBlank()
            val discImageUrl = video.musicCoverUrl?.takeIf { it.isNotBlank() } ?: resolvedCreatorAvatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF1B1B22))
                    .clickable(enabled = hasAttachedTrack) { showAudioDetailModal = true }
                    .rotate(if (isPlaying && isActive) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                if (discImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = discImageUrl,
                        contentDescription = "Audio Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Lucide.Music,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Bottom-Left Creator & Caption Info ────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp, bottom = 70.dp + navBarBottomDp)
                .graphicsLayer { alpha = overlayAlpha }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val firstCollab = acceptedCollaborators.firstOrNull()
                if (firstCollab != null) {
                    com.chroma.studio.ui.components.CollabStackedAvatars(
                        primaryName = resolvedCreatorDisplayName.ifBlank { resolvedCreatorUsername },
                        primaryAvatarUrl = resolvedCreatorAvatar,
                        secondaryName = firstCollab.displayName.ifBlank { firstCollab.username },
                        secondaryAvatarUrl = firstCollab.avatarUrl,
                        cutoutRingColor = Color(0xFF141418),
                        size = 32.dp,
                        onClick = { showCollabSheet = true }
                    )
                } else {
                    IosAvatarView(
                        name = resolvedCreatorDisplayName.ifBlank { resolvedCreatorUsername },
                        avatarUrl = resolvedCreatorAvatar,
                        size = 36.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onAuthorClick() }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable {
                            if (acceptedCollaborators.isNotEmpty()) showCollabSheet = true else onAuthorClick()
                        }
                ) {
                    if (acceptedCollaborators.isNotEmpty()) {
                        com.chroma.studio.ui.components.CollabCoAuthorTitleRow(
                            primaryLabel = resolvedCreatorUsername.ifBlank { resolvedCreatorDisplayName.ifBlank { "Creator" } },
                            primaryVerified = resolvedCreatorVerified,
                            primaryVerifiedTone = resolvedCreatorVerifiedTone,
                            acceptedCollaborators = acceptedCollaborators,
                            textColor = Color.White,
                            mutedColor = Color.White.copy(alpha = 0.80f),
                            fontSize = 14.sp,
                            onPrimaryClick = onAuthorClick,
                            onCollaboratorClick = { showCollabSheet = true }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = resolvedCreatorDisplayName.ifBlank { resolvedCreatorUsername.ifBlank { "Creator" } },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.75f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                            if (resolvedCreatorVerified) {
                                VerifiedBadge(
                                    size = 13.dp,
                                    tone = resolvedCreatorVerifiedTone
                                )
                            }
                        }
                        Text(
                            text = "@${resolvedCreatorUsername.ifBlank { "creator" }}",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (auth.currentUser?.uid != video.creatorId && !isFollowing) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0381FE))
                            .clickable {
                                if (!requireSignedIn()) return@clickable
                                val currentUid = auth.currentUser?.uid ?: return@clickable
                                com.chroma.studio.data.mutation.OptimisticFollowManager.toggleFollow(
                                    currentUserId = currentUid,
                                    targetUserId = video.creatorId,
                                    baseFollowing = false
                                )
                            }
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Follow",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(9.dp))

            val hasCuratedTrack = !video.musicTrackId.isNullOrBlank() || !video.musicTitle.isNullOrBlank()
            val audioLabelText = if (!video.musicTitle.isNullOrBlank()) {
                val artistLabel = video.musicArtist?.takeIf { it.isNotBlank() } ?: "Chroma Music"
                "${video.musicTitle} · $artistLabel"
            } else {
                "${video.authorDisplayName.ifBlank { video.authorUsername.ifBlank { "Creator" } }} · Original audio"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(enabled = hasCuratedTrack) { showAudioDetailModal = true }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Audio",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = audioLabelText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasCuratedTrack) {
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (video.caption.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = video.caption,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    lineHeight = 18.5.sp,
                    maxLines = if (isCaptionExpanded) 6 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
                )
            }

            if (video.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = video.hashtags.take(5).joinToString(" ") { "#$it" },
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Bottom Comment Prompt + Scrubbable Progress Rail ──────────────────
        val playbackFraction = if (videoDuration > 0L) {
            (currentPosition.toFloat() / videoDuration.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val shownProgress = if (isScrubbing) scrubFraction else playbackFraction

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            if (isScrubbing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scrubMs = (scrubFraction * videoDuration).toLong()
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${formatFlashTime(scrubMs)} / ${formatFlashTime(videoDuration)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (video.turnOffComments) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.44f)
                        )
                        .clickable(enabled = !video.turnOffComments) {
                            if (requireSignedIn()) {
                                showComments = true
                            }
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!video.turnOffComments) {
                        IosAvatarView(
                            name = currentUserName,
                            avatarUrl = currentUserAvatar,
                            size = 24.dp,
                            modifier = Modifier.clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (video.turnOffComments) "Comments are turned off" else "Add a comment…",
                        color = Color.White.copy(alpha = if (video.turnOffComments) 0.45f else 0.72f),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!video.turnOffComments) {
                        Icon(
                            imageVector = Lucide.MessageCircle,
                            contentDescription = "Comments",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Interactive Scrubbable Progress Bar
            val barHeight by animateDpAsState(
                targetValue = if (isScrubbing) 5.dp else 2.5.dp,
                animationSpec = tween(140),
                label = "scrubberHeight"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .onSizeChanged { size -> scrubberWidthPx = size.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(video.id, videoDuration) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubFraction = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                scrubFraction = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                val targetMs = (scrubFraction * videoDuration).toLong()
                                player.seekTo(targetMs)
                                currentPosition = targetMs
                                isScrubbing = false
                            },
                            onDragCancel = {
                                isScrubbing = false
                            }
                        )
                    },
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(shownProgress)
                            .fillMaxHeight()
                            .background(if (isScrubbing) Color(0xFF38BDF8) else Color.White)
                    )
                }
            }
        }

        // Comments Bottom Sheet
        if (showComments) {
            FlashCommentsSheet(
                video = video,
                commentVm = commentVm,
                onDismiss = { showComments = false },
                onRequireLogin = onRequireLogin,
                onCommentCountChanged = { delta ->
                    flashVm.incrementCommentCount(video.id, delta)
                },
                onReplyWithFlash = {
                    showComments = false
                    onNavigateToFlashPicker()
                }
            )
        }

        // More Options Bottom Sheet
        if (showMoreOptionsSheet) {
            FlashMoreOptionsSheet(
                video = video,
                isOwner = isOwner,
                isAcceptedCollaborator = isAcceptedCollaborator,
                onDismiss = { showMoreOptionsSheet = false },
                onDeleteClick = {
                    showMoreOptionsSheet = false
                    showDeleteConfirmDialog = true
                },
                onManageCollaboratorsClick = {
                    showMoreOptionsSheet = false
                    showManageCollabPicker = true
                },
                onStopSharingClick = {
                    showMoreOptionsSheet = false
                    showStopSharingConfirm = true
                },
                onShareClick = {
                    showMoreOptionsSheet = false
                    onShare()
                },
                onNotInterestedClick = {
                    showMoreOptionsSheet = false
                    onHideVideo()
                },
                onDownloadClick = {
                    showMoreOptionsSheet = false
                    ToastManager.showToast("Downloading video to gallery…")
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val request = okhttp3.Request.Builder().url(video.videoUrl).build()
                            val response = okhttp3.OkHttpClient().newCall(request).execute()
                            val body = response.body
                            if (response.isSuccessful && body != null) {
                                val tempFile = java.io.File(context.cacheDir, "flash_dl_${System.currentTimeMillis()}.mp4")
                                tempFile.outputStream().use { out ->
                                    body.byteStream().copyTo(out)
                                }
                                val savedUri = ExportHelper.saveVideoToGallery(context, tempFile)
                                withContext(Dispatchers.Main) {
                                    if (savedUri != null) {
                                        ToastManager.showToast("Saved to gallery")
                                    } else {
                                        ToastManager.showToast("Failed to save to gallery")
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    ToastManager.showToast("Failed to download video")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                ToastManager.showToast("Download failed: ${e.localizedMessage}")
                            }
                        }
                    }
                }
            )
        }

        if (showDeleteConfirmDialog) {
            IosAlertDialog(
                title = "Delete Flash Video?",
                message = "Are you sure you want to permanently delete this Flash video? This action cannot be undone.",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    showDeleteConfirmDialog = false
                    flashVm.deleteFlash(video.id) { success ->
                        if (success) {
                            ToastManager.showToast("Flash video deleted")
                        } else {
                            ToastManager.showToast("Failed to delete video")
                        }
                    }
                },
                dismissText = "Cancel",
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }

        if (showCollabSheet) {
            com.chroma.studio.ui.components.CollaboratorsDetailSheet(
                creatorUid = video.creatorId,
                creatorDisplayName = resolvedCreatorDisplayName.ifBlank { resolvedCreatorUsername },
                creatorUsername = resolvedCreatorUsername,
                creatorAvatarUrl = resolvedCreatorAvatar,
                creatorVerified = resolvedCreatorVerified,
                creatorVerifiedTone = resolvedCreatorVerifiedTone,
                collaborators = acceptedCollaborators,
                isCurrentUserCollaborator = isAcceptedCollaborator,
                onStopSharing = { showStopSharingConfirm = true },
                onAuthorClick = { uid ->
                    if (uid == video.creatorId) onAuthorClick()
                    else com.chroma.studio.ui.components.ToastManager.showToast("Viewing collaborator profile")
                },
                onDismiss = { showCollabSheet = false }
            )
        }

        if (showStopSharingConfirm && currentUserUid != null) {
            com.chroma.studio.ui.components.IosAlertDialog(
                title = "Stop Sharing Flash?",
                message = "This removes you as a collaborator and hides this Flash from your profile while keeping @${resolvedCreatorUsername}'s original video intact.",
                confirmText = "Stop Sharing",
                isDestructive = true,
                onConfirm = {
                    showStopSharingConfirm = false
                    coroutineScope.launch {
                        val ok = discoverRepo.leaveCollab(video.id)
                        if (ok) {
                            val nextState = com.chroma.studio.model.removeCollabParticipant(
                                participants = localCollabs,
                                pendingIds = localPendingCollabIds,
                                acceptedIds = localAcceptedCollabIds,
                                targetUid = currentUserUid
                            )
                            localCollabs = nextState.collaborators
                            localAcceptedCollabIds = nextState.collaboratorIds
                            ToastManager.showToast("Removed from your profile")
                        } else {
                            ToastManager.showToast("Could not update collaboration")
                        }
                    }
                },
                onDismiss = { showStopSharingConfirm = false }
            )
        }

        if (showManageCollabPicker && isOwner && currentUserUid != null) {
            com.chroma.studio.ui.components.CollabPickerModalSheet(
                currentUserUid = currentUserUid,
                initialSelection = localCollabs,
                repository = discoverRepo,
                onDone = { picked ->
                    showManageCollabPicker = false
                    val pickedUids = picked.map { it.uid }.toSet()
                    val existingUids = localCollabs.map { it.uid }.toSet()
                    val added = picked.filter { it.uid !in existingUids }
                    val removed = localCollabs.filter { it.uid !in pickedUids }.map { it.uid }
                    if (added.isNotEmpty() || removed.isNotEmpty()) {
                        coroutineScope.launch {
                            val ok = discoverRepo.updateCollaboratorsAsCreator(
                                postId = video.id,
                                addedCollaborators = added,
                                removedUids = removed
                            )
                            if (ok) {
                                ToastManager.showToast("Collaborators updated")
                                flashVm.loadFeed(isRefresh = true)
                            } else {
                                ToastManager.showToast("Failed to update collaborators")
                            }
                        }
                    }
                },
                onDismiss = { showManageCollabPicker = false }
            )
        }

        if (showAudioDetailModal && !video.musicTrackId.isNullOrBlank()) {
            com.chroma.studio.ui.components.AudioDetailModal(
                trackId = video.musicTrackId,
                fallbackTitle = video.musicTitle,
                fallbackArtist = video.musicArtist,
                fallbackCoverUrl = video.musicCoverUrl,
                fallbackAudioUrl = video.musicAudioUrl,
                onDismiss = { showAudioDetailModal = false },
                onUseAudio = { track ->
                    showAudioDetailModal = false
                    if (requireSignedIn()) {
                        com.chroma.studio.data.PendingMusicHandoff.set(
                            com.chroma.studio.data.AttachedMusicSelection(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                coverUrl = track.coverUrl,
                                audioUrl = track.audioUrl,
                                startSec = track.bestPartStartSec,
                                durationSec = (track.bestPartEndSec - track.bestPartStartSec).coerceIn(5, maxOf(5, track.durationSec)),
                                totalTrackSec = track.durationSec,
                                showLyricsSticker = true
                            )
                        )
                        onNavigateToFlashPicker()
                    }
                },
                onFlashClick = { clickedFlash ->
                    showAudioDetailModal = false
                    flashVm.ensureFlashInFeed(clickedFlash.id, null)
                },
                onPostClick = { clickedPost ->
                    showAudioDetailModal = false
                    onNavigateToPost?.invoke(clickedPost.id)
                }
            )
        }
    }
}

private fun formatFlashTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "$min:${sec.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashMoreOptionsSheet(
    video: FlashVideo,
    isOwner: Boolean,
    isAcceptedCollaborator: Boolean = false,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onManageCollaboratorsClick: () -> Unit = {},
    onStopSharingClick: () -> Unit = {},
    onShareClick: () -> Unit,
    onNotInterestedClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBg = if (colors.isDark) Color(0xFF13131A) else Color.White
    val iconChipBg = if (colors.isDark) Color(0xFF1E1E28) else Color(0xFFEEEEF3)
    val dangerBg = Color(0xFFF43F5E).copy(alpha = 0.12f)
    val dangerColor = Color(0xFFF43F5E)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.textMuted.copy(alpha = 0.45f))
                )
            }

            Text(
                text = "Flash options",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            @Composable
            fun OptionRow(
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                label: String,
                danger: Boolean = false,
                onClick: () -> Unit
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (danger) dangerColor else colors.textMain,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = label,
                        color = if (danger) dangerColor else colors.textMain,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            OptionRow(Lucide.Share2, "Share Flash", onClick = onShareClick)

            OptionRow(Lucide.Link, "Copy link") {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Flash Video", "https://oscellion.app/flash/${video.id}")
                clipboard.setPrimaryClip(clip)
                ToastManager.showToast("Link copied to clipboard")
                onDismiss()
            }

            OptionRow(Lucide.Download, "Save to device", onClick = onDownloadClick)

            if (isOwner) {
                OptionRow(Lucide.Users, "Manage Collaborators", onClick = onManageCollaboratorsClick)
                OptionRow(Lucide.Trash2, "Delete Flash video", danger = true, onClick = onDeleteClick)
            } else {
                if (isAcceptedCollaborator) {
                    OptionRow(Lucide.UserMinus, "Stop sharing", danger = true, onClick = onStopSharingClick)
                }
                OptionRow(Lucide.EyeOff, "Not interested", onClick = onNotInterestedClick)
                OptionRow(Lucide.Flag, "Report video", danger = true) {
                    val reporterUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (!reporterUid.isNullOrBlank()) {
                        val reportData = mapOf(
                            "reporterId" to reporterUid,
                            "targetId" to video.id,
                            "targetType" to "flash_video",
                            "targetAuthorId" to video.creatorId,
                            "reason" to "Inappropriate content",
                            "timestamp" to System.currentTimeMillis()
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("reports")
                            .add(reportData)
                    }
                    ToastManager.showToast("Report submitted. Thank you.")
                    onDismiss()
                }
            }

            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconChipBg)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReelsActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    scale: Float = 1f,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        if (label.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
private fun FlashEmptyState(onUpload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Lucide.Film,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text("No Flash Videos Yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Share a short video or remix from the studio.",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0381FE))
                .clickable(onClick = onUpload)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Lucide.Plus, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("Create Flash", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FollowingFlashEmptyState(
    onExploreForYou: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Lucide.Users,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text("No Following Flash Yet", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Follow creators in For You to see their Flash videos here.",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0381FE))
                    .clickable(onClick = onExploreForYou)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Text("Explore For You", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E28))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text("Refresh", color = Color.White.copy(alpha = 0.85f), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DraftItem(draft: FlashDraft, onResume: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardSurfaceElevated)
            .clickable { onResume() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            if (draft.thumbnailUri.isNotBlank()) {
                AsyncImage(
                    model = draft.thumbnailUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Lucide.FileVideo, null, tint = colors.textMuted, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                draft.mediaType.name.lowercase().replaceFirstChar { it.uppercase() },
                color = colors.textMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                draft.caption.ifBlank { "No caption" },
                color = colors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Lucide.Trash2, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
}

internal fun formatCount(rawCount: Int): String {
    val count = maxOf(0, rawCount)
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0).trimEnd('0').trimEnd('.')
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0).trimEnd('0').trimEnd('.')
        else -> count.toString()
    }
}

@Composable
fun FlashCommentsSheet(
    video: FlashVideo,
    commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel,
    onDismiss: () -> Unit,
    onCommentCountChanged: (Int) -> Unit = {},
    onReplyWithFlash: (String?) -> Unit,
    onRequireLogin: () -> Unit = {}
) {
    com.chroma.studio.ui.components.YouTubeCommentsSheet(
        targetId = video.id,
        targetCommentCount = video.commentCount,
        creatorId = video.creatorId,
        creatorUsername = video.authorUsername,
        creatorAvatarUrl = video.authorAvatarUrl,
        creatorVerified = video.authorVerified,
        creatorVerifiedTone = video.authorVerifiedTone,
        postCaption = video.caption,
        postThumbnailUrl = video.thumbnailUrl,
        turnOffComments = video.turnOffComments,
        commentVm = commentVm,
        onDismiss = onDismiss,
        onCommentCountChanged = onCommentCountChanged,
        onReplyWithFlash = onReplyWithFlash,
        onRequireLogin = onRequireLogin
    )
}

fun Modifier.flashShimmerEffect(): Modifier = composed {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 2000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            ),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
        )
    )
}

@Composable
private fun FlashFeedSkeleton() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Mock Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).flashShimmerEffect())
            Box(Modifier.width(120.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).flashShimmerEffect())
            Box(Modifier.size(24.dp).clip(CircleShape).flashShimmerEffect())
        }

        // Mock Bottom Info
        val skeletonNavBarDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 64.dp + skeletonNavBarDp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).flashShimmerEffect())
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).flashShimmerEffect())
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(220.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).flashShimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(160.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).flashShimmerEffect())
        }

        // Mock Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 68.dp + skeletonNavBarDp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(32.dp).clip(CircleShape).flashShimmerEffect())
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(24.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).flashShimmerEffect())
                }
            }
        }

        // Mock Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.BottomCenter)
                .flashShimmerEffect()
        )
    }
}
