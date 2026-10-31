package com.chroma.studio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.chroma.studio.ui.components.FlatDoubleTapHeartOverlay
import com.chroma.studio.ui.components.rememberDoubleTapLikeState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.IosAvatarView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.chroma.studio.model.FlashDraft
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.GlassDialog
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.IosAlertDialog
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.chroma.studio.model.FlashComment
import com.chroma.studio.viewmodel.CommentSortOrder

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
    onRemixVideo: ((FlashVideo) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    val flashFeed by flashVm.flashFeed.collectAsState()
    val isLoading by flashVm.isLoading.collectAsState()
    val isRefreshing by flashVm.isRefreshing.collectAsState()
    val likedVideoIds by flashVm.likedVideoIds.collectAsState()
    val drafts by flashVm.drafts.collectAsState()

    var selectedTab by remember { mutableStateOf("Reels") }

    val initialPage = remember(initialVideoId, flashFeed) {
        if (!initialVideoId.isNullOrBlank()) {
            val idx = flashFeed.indexOfFirst { it.id == initialVideoId }
            if (idx >= 0) idx else 0
        } else {
            0
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { flashFeed.size.coerceAtLeast(1) }
    )

    BackHandler {
        onNavigateToOldHome()
    }

    LaunchedEffect(initialVideoId, flashFeed) {
        if (!initialVideoId.isNullOrBlank() && flashFeed.isNotEmpty()) {
            val idx = flashFeed.indexOfFirst { it.id == initialVideoId }
            if (idx >= 0 && pagerState.currentPage != idx) {
                pagerState.scrollToPage(idx)
            }
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

        List(3) {
            androidx.media3.exoplayer.ExoPlayer.Builder(context)
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
    LaunchedEffect(pagerState.currentPage, flashFeed.size) {
        if (pagerState.currentPage >= flashFeed.size - 3) {
            flashVm.loadMoreFeed()
        }

        // AOT Prefetching for the next 2-3 videos
        for (i in 1..3) {
            val idx = pagerState.currentPage + i
            if (idx < flashFeed.size) {
                val url = flashFeed[idx].videoUrl
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    com.chroma.studio.utils.VideoPrefetcher.prefetchVideo(context, url)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading && flashFeed.isEmpty()) {
            FlashFeedSkeleton()
        } else if (flashFeed.isEmpty()) {
            if (emptyContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Drag handle pill at the top of the sheet
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
                FlashEmptyState(onUpload = onNavigateToFlashPicker)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = flashFeed.getOrNull(page)
                if (video != null) {
                    FlashVideoItem(
                        player = exoPlayers[page % 3],
                        video = video,
                        isActive = pagerState.currentPage == page,
                        isLiked = video.id in likedVideoIds,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        onLike = {
                            if (FirebaseAuth.getInstance().currentUser == null) {
                                onRequireLogin()
                            } else {
                                flashVm.toggleLike(video.id)
                            }
                        },
                        onShare = {
                            flashVm.shareFlash(video.id)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out this Flash on Oscellion: https://oscellion.vercel.app/flash/${video.id}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        },
                        onAuthorClick = { onNavigateToProfile(video.creatorId) },
                        flashVm = flashVm,
                        commentVm = commentVm,
                        onRemixRequested = onRemixRequested,
                        onNavigateToFlashPicker = onNavigateToFlashPicker,
                        onRemixVideo = onRemixVideo
                    )
                }
            }
        }

        // ── Top Bar (Reels / Friends Header) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            // Left: Back button
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onNavigateToOldHome() }
            )

            // Center: Tabs (Reels / Friends)
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels",
                    color = if (selectedTab == "Reels") Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { selectedTab = "Reels" }
                )
                Text(
                    text = "Friends",
                    color = if (selectedTab == "Friends") Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { selectedTab = "Friends" }
                )
            }

            // Right: Camera / Create / Drafts button
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (drafts.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { showDraftSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.FileVideo, "Drafts", tint = Color.White, modifier = Modifier.size(18.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${drafts.size}", fontSize = 8.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Icon(
                    imageVector = Lucide.Camera,
                    contentDescription = "Upload Flash",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onNavigateToFlashPicker() }
                )
            }
        }

        // Pull to refresh indicator
        if (isRefreshing) {
            Box(Modifier.fillMaxWidth().padding(top = 100.dp).align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
                OscellionLoadingSpinner(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }

    // Drafts bottom sheet (Plain pure white)
    if (showDraftSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDraftSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB)) }
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    "Drafts",
                    color = Color(0xFF1C1C1E),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                drafts.forEach { draft ->
                    DraftItem(
                        draft = draft,
                        onResume = { showDraftSheet = false; onNavigateToFlashPicker() },
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
    flashVm: FlashViewModel,
    commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel,
    onRemixRequested: (Uri, String, String) -> Unit,
    onNavigateToFlashPicker: () -> Unit = {},
    onRemixVideo: ((FlashVideo) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }

    val followingUserIds by com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds.collectAsState()
    val isFollowing = followingUserIds.contains(video.creatorId)
    val isSaved = auth.currentUser?.uid?.let { video.savedBy.contains(it) } ?: false
    var showComments by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(video.creatorId) {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank() && currentUid != video.creatorId && !com.chroma.studio.data.mutation.OptimisticFollowManager.isFollowing(video.creatorId)) {
            val serverFollowing = discoverRepo.checkIsFollowing(currentUid, video.creatorId)
            if (serverFollowing) {
                com.chroma.studio.data.mutation.OptimisticFollowManager.seedFollowing(setOf(video.creatorId))
            }
        }
    }

    // Prepare the pooled player for this specific video
    LaunchedEffect(video.videoUrl, player) {
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(video.videoUrl))
        player.volume = if (isMuted) 0f else 1f
        player.prepare()
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

    // Play/Pause logic based on pager state and app lifecycle
    LaunchedEffect(isActive, lifecycleEvent, isMuted, player) {
        if (isActive && (lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_RESUME || lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_ANY)) {
            player.volume = if (isMuted) 0f else 1f
            player.play()
        } else {
            player.pause()
        }
    }

    val colors = LocalOscellionColors.current
    val doubleTapLikeState = rememberDoubleTapLikeState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var showPlayPauseOverlay by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val isOwner = auth.currentUser?.uid == video.creatorId
    var currentPosition by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }

    // Sync volume with global mute state and player instance
    LaunchedEffect(isMuted, player) {
        player.volume = if (isMuted) 0f else 1f
    }

    var isPlayerReady by remember { mutableStateOf(false) }
    var videoRatio by remember { mutableFloatStateOf(1f) }

    // Sync play state and track progress
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    isPlayerReady = true
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoRatio = (videoSize.width.toFloat() / videoSize.height.toFloat()) * videoSize.pixelWidthHeightRatio
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            videoDuration = player.duration.coerceAtLeast(1L)
            delay(50)
        }
    }

    val playPauseScale by animateFloatAsState(
        targetValue = if (showPlayPauseOverlay) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playPauseScale"
    )
    val playPauseAlpha by animateFloatAsState(
        targetValue = if (showPlayPauseOverlay) 1f else 0f,
        animationSpec = tween(300),
        label = "playPauseAlpha"
    )

    LaunchedEffect(showPlayPauseOverlay) {
        if (showPlayPauseOverlay) {
            delay(500)
            showPlayPauseOverlay = false
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "likeScale"
    )

    // Infinite rotation for the vinyl/audio disc
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        doubleTapLikeState.trigger(offset)
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            onLike()
                        } else {
                            // Double tap ALWAYS LIKES and NEVER DISLIKES (User must tap heart button to unlike)
                            flashVm.toggleLike(video.id, forceLike = true)
                        }
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        // silent
                    },
                    onTap = {
                        if (isPlaying) player.pause() else player.play()
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

                if (textureView.tag != player) {
                    textureView.tag = player
                    player.setVideoTextureView(textureView)
                }
                frameLayout.setAspectRatio(videoRatio)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Thumbnail image
        androidx.compose.animation.AnimatedVisibility(
            visible = !isPlayerReady,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(100))
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
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Center Volume Indicator (Pill badge)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onToggleMute() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                contentDescription = "Mute",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center Play / Pause Animated Indicator
        if (showPlayPauseOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .scale(playPauseScale)
                    .graphicsLayer { alpha = playPauseAlpha }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Lucide.Play else Lucide.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Double-tap heart animation (Simple, Flat, Smooth and Clean)
        FlatDoubleTapHeartOverlay(
            state = doubleTapLikeState,
            modifier = Modifier.fillMaxSize(),
            heartSize = 115.dp
        )

        // ── Right-Side Vertical Action Column (Reels Style) ───────────────────
        // Bottom padding = nav bar inset + comment bar (≈60dp) + breathing room
        val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 68.dp + navBarBottomDp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like (Heart icon)
            ReelsActionButton(
                icon = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                tint = if (isLiked) Color(0xFFFF3B30) else Color.White,
                label = if (video.hideLikesCount) "" else formatCount(video.likesCount),
                scale = likeScale,
                onClick = onLike
            )

            // Comments (only if comments are not turned off)
            if (!video.turnOffComments) {
                ReelsActionButton(
                    icon = Lucide.MessageCircle,
                    tint = Color.White,
                    label = formatCount(video.commentCount),
                    onClick = { showComments = true }
                )
            }

            // Remix (Heart-shaped Handshake) (only if remix is not turned off)
            if (!video.turnOffRemix) {
                ReelsActionButton(
                    icon = Lucide.HeartHandshake,
                    tint = Color.White,
                    label = formatCount(video.remixCount),
                    onClick = {
                        if (onRemixVideo != null) {
                            onRemixVideo(video)
                        } else {
                            android.widget.Toast.makeText(context, "Preparing for Remix...", android.widget.Toast.LENGTH_SHORT).show()
                            flashVm.downloadVideoForRemix(
                                context = context,
                                videoUrl = video.videoUrl,
                                onProgress = { },
                                onComplete = { uri ->
                                    if (uri != null) {
                                        onRemixRequested(uri, video.id, video.authorUsername)
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to download video.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                )
            }

            // Share / Direct Paper Plane
            ReelsActionButton(
                icon = Lucide.Send,
                tint = Color.White,
                label = formatCount(video.shareCount),
                onClick = onShare
            )

            // Bookmark / Save
            ReelsActionButton(
                icon = Lucide.Bookmark,
                tint = if (isSaved) Color(0xFFFFCC00) else Color.White,
                label = formatCount(video.saveCount),
                onClick = { flashVm.toggleSave(video.id) }
            )

            // More Options (Three horizontal dots)
            Box(
                modifier = Modifier
                    .size(38.dp)
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

            // Audio Vinyl Disc / Squircle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .rotate(if (isPlaying) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                if (video.authorAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.authorAvatarUrl,
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

        // ── Bottom-Left Info Overlay (Reels Style) ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp, bottom = 64.dp + navBarBottomDp)
        ) {
            // User Row (Avatar + @username + Verified + Follow button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular Avatar
                IosAvatarView(
                    name = video.authorDisplayName.ifBlank { video.authorUsername },
                    avatarUrl = video.authorAvatarUrl,
                    size = 36.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { onAuthorClick() }
                )

                // Username + Verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onAuthorClick() }
                ) {
                    Text(
                        text = video.authorUsername.ifBlank { "epro.grams" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                blurRadius = 4f
                            )
                        )
                    )
                    if (video.authorVerified) {
                        VerifiedBadge(size = 13.dp)
                    }
                }

                // Follow / Following Pill Button
                if (auth.currentUser?.uid != video.creatorId) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .background(if (isFollowing) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                val currentUid = auth.currentUser?.uid
                                if (!currentUid.isNullOrBlank()) {
                                    com.chroma.studio.data.mutation.OptimisticFollowManager.toggleFollow(
                                        currentUserId = currentUid,
                                        targetUserId = video.creatorId,
                                        baseFollowing = isFollowing
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Audio track row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Audio",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${video.authorDisplayName.ifBlank { "Original audio" }} • Original audio",
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Explicit Badge [E]
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(6.dp))

            // Caption
            if (video.caption.isNotBlank()) {
                Text(
                    text = video.caption,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = if (isCaptionExpanded) 6 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
                )
            }

            // Hashtags
            if (video.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = video.hashtags.take(5).joinToString(" ") { "#$it" },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Bottom Comment Input Bar (Pinned across bottom) ───────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                    .clickable { showComments = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add comment...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = Lucide.Smile,
                    contentDescription = "Emoji",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Video Progress Bar at the very bottom edge ────────────────────────
        val progress = if (videoDuration > 0) currentPosition.toFloat() / videoDuration.toFloat() else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        // Comments Bottom Sheet (Plain pure white)
        if (showComments) {
            FlashCommentsSheet(
                video = video,
                commentVm = commentVm,
                onDismiss = { showComments = false },
                onCommentCountChanged = { delta ->
                    flashVm.incrementCommentCount(video.id, delta)
                },
                onReplyWithFlash = {
                    showComments = false
                    onNavigateToFlashPicker()
                }
            )
        }

        // More Options Bottom Sheet (Plain pure white)
        if (showMoreOptionsSheet) {
            FlashMoreOptionsSheet(
                video = video,
                isOwner = isOwner,
                onDismiss = { showMoreOptionsSheet = false },
                onDeleteClick = {
                    showMoreOptionsSheet = false
                    showDeleteConfirmDialog = true
                },
                onShareClick = {
                    showMoreOptionsSheet = false
                    onShare()
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
                                        ToastManager.showToast("Saved to gallery! 🎬")
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

        // Delete confirmation dialog (Apple HIG Style)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashMoreOptionsSheet(
    video: FlashVideo,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Flash Options",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isOwner) {
                // Destructive Delete option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFEF2F2))
                        .clickable { onDeleteClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "Delete Video",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Delete Flash Video",
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Share option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShareClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.Share2,
                    contentDescription = "Share",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Share Video",
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }

            // Copy Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Flash Video", video.videoUrl)
                        clipboard.setPrimaryClip(clip)
                        ToastManager.showToast("Link copied to clipboard")
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.Link,
                    contentDescription = "Copy Link",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Copy Link",
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }

            // Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDownloadClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.Download,
                    contentDescription = "Save Video",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Save to Device",
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }

            if (!isOwner) {
                // Report option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            ToastManager.showToast("Report submitted. Thank you.")
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Flag,
                        contentDescription = "Report",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Report Video",
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                // Not Interested
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            ToastManager.showToast("We'll tune your feed recommendations.")
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.EyeOff,
                        contentDescription = "Not Interested",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Not Interested",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
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
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
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
                fontSize = 11.sp,
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
    val emptyState = rememberAppleGraphicAnimationState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppleFlashGraphic(
            modifier = Modifier.size(135.dp),
            state = emptyState,
            enableClickToAnimate = true
        )
        Spacer(Modifier.height(20.dp))
        Text("No Reels Yet", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Be the first to share a video!", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        GlassButton(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Lucide.Plus, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Create Reel", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DraftItem(draft: FlashDraft, onResume: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .clickable { onResume() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E7EB)),
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
                Icon(Lucide.FileVideo, null, tint = Color(0xFF6B7280), modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                draft.mediaType.name.lowercase().replaceFirstChar { it.uppercase() },
                color = Color(0xFF1C1C1E),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                draft.caption.ifBlank { "No caption" },
                color = Color(0xFF8E8E93),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCommentsSheet(
    video: FlashVideo,
    commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel,
    onDismiss: () -> Unit,
    onCommentCountChanged: (Int) -> Unit = {},
    onReplyWithFlash: (String?) -> Unit
) {
    val colors = LocalOscellionColors.current
    val comments by commentVm.comments.collectAsState()
    val sortOrder by commentVm.sortOrder.collectAsState()
    val isLoading by commentVm.isLoading.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<FlashComment?>(null) }
    val expandedThreads = remember { mutableStateMapOf<String, Boolean>() }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var commentPendingDelete by remember { mutableStateOf<FlashComment?>(null) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(video.id) {
        commentVm.loadComments(video.id)
    }

    // Split top-level and replies
    val repliesMap = remember(comments) {
        comments.filter { !it.replyToCommentId.isNullOrBlank() }
            .groupBy { it.replyToCommentId!! }
    }

    val topLevelComments = remember(comments, sortOrder) {
        val parents = comments.filter { it.replyToCommentId.isNullOrBlank() }
        when (sortOrder) {
            CommentSortOrder.TOP_COMMENTS -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.likesCount }
                        .thenByDescending { it.timestamp }
                )
            }
            CommentSortOrder.NEWEST_FIRST -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
            }
        }
    }

    val totalCommentCount = maxOf(video.commentCount, comments.size)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            // YouTube Comments Header: Count, Sort By Dropdown, and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F0F0F)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatCount(totalCommentCount),
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = Color(0xFF606060)
                )

                Spacer(Modifier.weight(1f))

                // Sort Dropdown Button
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.SlidersHorizontal,
                            contentDescription = "Sort comments",
                            tint = Color(0xFF0F0F0F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (sortOrder == CommentSortOrder.TOP_COMMENTS) "Top comments" else "Newest first",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F0F0F)
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (sortOrder == CommentSortOrder.TOP_COMMENTS) {
                                        Icon(Lucide.Check, null, tint = Color(0xFF065FD4), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Spacer(Modifier.width(24.dp))
                                    }
                                    Text(
                                        "Top comments",
                                        fontWeight = if (sortOrder == CommentSortOrder.TOP_COMMENTS) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            onClick = {
                                commentVm.setSortOrder(CommentSortOrder.TOP_COMMENTS)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (sortOrder == CommentSortOrder.NEWEST_FIRST) {
                                        Icon(Lucide.Check, null, tint = Color(0xFF065FD4), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Spacer(Modifier.width(24.dp))
                                    }
                                    Text(
                                        "Newest first",
                                        fontWeight = if (sortOrder == CommentSortOrder.NEWEST_FIRST) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            onClick = {
                                commentVm.setSortOrder(CommentSortOrder.NEWEST_FIRST)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Lucide.X, contentDescription = "Close", tint = Color(0xFF0F0F0F), modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(
                color = Color(0xFFE5E7EB),
                thickness = 0.5.dp
            )

            // Comment List / States
            if (isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    OscellionLoadingSpinner(color = colors.primary)
                }
            } else if (topLevelComments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Lucide.MessageCircle, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No comments yet",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Say something to start the conversation.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(topLevelComments, key = { it.id }) { comment ->
                        val childReplies = repliesMap[comment.id] ?: emptyList()
                        val isThreadExpanded = expandedThreads[comment.id] ?: false

                        YouTubeCommentCard(
                            comment = comment,
                            video = video,
                            currentUserId = currentUserId,
                            replies = childReplies,
                            isRepliesExpanded = isThreadExpanded,
                            onToggleReplies = {
                                expandedThreads[comment.id] = !isThreadExpanded
                            },
                            onReply = { target ->
                                replyingToComment = target
                                focusRequester.requestFocus()
                            },
                            onReplyWithFlash = { targetId ->
                                onReplyWithFlash(targetId)
                            },
                            onToggleLike = { commentVm.toggleLike(it) },
                            onToggleDislike = { commentVm.toggleDislike(it) },
                            onToggleHeart = { commentVm.toggleCreatorHeart(it) },
                            onTogglePin = { commentVm.togglePin(it, video.id) },
                            onDelete = { commentPendingDelete = it },
                            onRetry = { commentVm.retryComment(it) }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Bottom YouTube Comment Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Replying Indicator Bar
                if (replyingToComment != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Reply,
                            contentDescription = null,
                            tint = Color(0xFF065FD4),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Replying to @${replyingToComment?.username}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { replyingToComment = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Cancel",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Quick Reactions Emoji Bar
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickEmojis = listOf("❤️", "🔥", "👏", "😂", "😍", "🙌", "✨", "💯", "🎉", "🤯")
                    items(quickEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable { commentText += emoji }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = FirebaseAuth.getInstance().currentUser?.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                if (replyingToComment != null) "Add a reply..." else "Add a comment...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 13.5.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF0F0F0F),
                            unfocusedTextColor = Color(0xFF0F0F0F)
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        maxLines = 4
                    )
                    if (commentText.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val parentTargetId = replyingToComment?.id
                                commentVm.addComment(
                                    videoId = video.id,
                                    text = commentText.trim(),
                                    replyToId = parentTargetId
                                )
                                onCommentCountChanged(1)
                                if (parentTargetId != null) {
                                    expandedThreads[parentTargetId] = true
                                }
                                commentText = ""
                                replyingToComment = null
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF065FD4))
                        ) {
                            Icon(
                                imageVector = Lucide.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (commentPendingDelete != null) {
        val comment = commentPendingDelete!!
        IosAlertDialog(
            title = "Delete Comment?",
            message = "Are you sure you want to delete this comment? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                val toDelete = comment
                commentPendingDelete = null
                commentVm.deleteComment(toDelete)
                onCommentCountChanged(-1)
                ToastManager.showToast("Comment deleted")
            },
            dismissText = "Cancel",
            onDismiss = { commentPendingDelete = null }
        )
    }
}

@Composable
fun YouTubeCommentCard(
    comment: FlashComment,
    video: FlashVideo,
    currentUserId: String?,
    replies: List<FlashComment> = emptyList(),
    isRepliesExpanded: Boolean = false,
    onToggleReplies: () -> Unit = {},
    onReply: (FlashComment) -> Unit,
    onReplyWithFlash: (String) -> Unit = {},
    onToggleLike: (String) -> Unit,
    onToggleDislike: (String) -> Unit,
    onToggleHeart: (FlashComment) -> Unit,
    onTogglePin: (FlashComment) -> Unit,
    onDelete: (FlashComment) -> Unit,
    onRetry: (FlashComment) -> Unit,
    isChildReply: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    val isDisliked = currentUserId != null && comment.dislikedBy.contains(currentUserId)
    val isVideoAuthor = currentUserId != null && currentUserId == video.creatorId
    val isCommentAuthor = currentUserId != null && currentUserId == comment.userId
    val isCommentByVideoAuthor = comment.userId == video.creatorId

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Pinned comment indicator (YouTube Signature)
        if (comment.isPinned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = if (isChildReply) 0.dp else 44.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = ChromaIcons.PinFilled,
                    contentDescription = "Pinned",
                    tint = Color(0xFF606060),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pinned by @${video.authorUsername.ifBlank { "creator" }}",
                    color = Color(0xFF606060),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 2. Avatar + Comment Body
        Row(modifier = Modifier.fillMaxWidth()) {
            IosAvatarView(
                name = comment.displayName.ifBlank { comment.username },
                avatarUrl = comment.avatarUrl,
                size = if (isChildReply) 28.dp else 34.dp
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // User & Timestamp Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCommentByVideoAuthor) {
                        // YouTube Creator Author Badge Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF27272A),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "@${comment.username}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Creator",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "@${comment.username}",
                            color = Color(0xFF0F0F0F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    if (comment.verified) {
                        VerifiedBadge(size = 11.dp)
                        Spacer(Modifier.width(4.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    if (comment.isPending) {
                        Text(
                            text = "Sending...",
                            color = Color(0xFF065FD4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (comment.isError) {
                        Text(
                            text = "Failed • Tap to retry",
                            color = Color(0xFFFF3B30),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onRetry(comment) }
                        )
                    } else {
                        Text(
                            text = android.text.format.DateUtils.getRelativeTimeSpanString(
                                comment.timestamp,
                                System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                            ).toString(),
                            color = Color(0xFF606060),
                            fontSize = 11.sp
                        )
                    }
                }

                // Comment Text
                Spacer(Modifier.height(3.dp))
                Text(
                    text = comment.text,
                    color = if (comment.isError) Color(0xFF9CA3AF) else Color(0xFF0F0F0F),
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )

                // 3. YouTube Action Bar: ThumbsUp, ThumbsDown, Creator Heart, Reply, 3-dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Thumbs Up (Like)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleLike(comment.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) ChromaIcons.ThumbsUpFilled else Lucide.ThumbsUp,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFF0F0F0F) else Color(0xFF606060),
                            modifier = Modifier.size(15.dp)
                        )
                        if (comment.likesCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatCount(comment.likesCount),
                                fontSize = 11.5.sp,
                                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLiked) Color(0xFF0F0F0F) else Color(0xFF606060)
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Thumbs Down (Dislike)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleDislike(comment.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDisliked) ChromaIcons.ThumbsDownFilled else Lucide.ThumbsDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) Color(0xFF0F0F0F) else Color(0xFF606060),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Creator Heart
                    if (comment.isCreatorHearted) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isVideoAuthor) Modifier.clickable { onToggleHeart(comment) } else Modifier
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(20.dp)) {
                                Icon(
                                    imageVector = ChromaIcons.HeartFilled,
                                    contentDescription = "Loved by creator",
                                    tint = Color(0xFFFF0033),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.TopStart)
                                )
                                AsyncImage(
                                    model = video.authorAvatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.BottomEnd)
                                        .border(0.75.dp, Color.White, CircleShape)
                                )
                            }
                        }
                    } else if (isVideoAuthor) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleHeart(comment) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Heart,
                                contentDescription = "Heart as creator",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Reply Text Button
                    if (!comment.isPending && !comment.isError) {
                        Text(
                            text = "Reply",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF606060),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onReply(comment) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        Spacer(Modifier.width(4.dp))

                        // Video reply shortcut button
                        IconButton(
                            onClick = { onReplyWithFlash(comment.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Video,
                                contentDescription = "Reply with Flash",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // 3-dots overflow menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More actions",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (isVideoAuthor && !isChildReply) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            ChromaIcons.PinFilled,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF0F0F0F)
                                        )
                                    },
                                    text = { Text(if (comment.isPinned) "Unpin comment" else "Pin to top") },
                                    onClick = {
                                        menuExpanded = false
                                        onTogglePin(comment)
                                    }
                                )
                            }
                            if (isCommentAuthor || isVideoAuthor) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Lucide.Trash2,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFF3B30)
                                        )
                                    },
                                    text = { Text("Delete", color = Color(0xFFFF3B30)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete(comment)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Lucide.Copy,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF0F0F0F)
                                    )
                                },
                                text = { Text("Copy text") },
                                onClick = {
                                    menuExpanded = false
                                    clipboard.setText(AnnotatedString(comment.text))
                                    ToastManager.showToast("Comment copied")
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Threaded Nested Replies Accordion (YouTube Signature)
        if (replies.isNotEmpty() && !isChildReply) {
            val replyCount = replies.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 44.dp, top = 2.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleReplies() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isRepliesExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = null,
                    tint = Color(0xFF065FD4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRepliesExpanded) "Hide $replyCount ${if (replyCount == 1) "reply" else "replies"}" else "$replyCount ${if (replyCount == 1) "reply" else "replies"}",
                    color = Color(0xFF065FD4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = isRepliesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp, top = 4.dp)
                ) {
                    replies.sortedBy { it.timestamp }.forEach { childReply ->
                        YouTubeCommentCard(
                            comment = childReply,
                            video = video,
                            currentUserId = currentUserId,
                            onReply = onReply,
                            onReplyWithFlash = onReplyWithFlash,
                            onToggleLike = onToggleLike,
                            onToggleDislike = onToggleDislike,
                            onToggleHeart = onToggleHeart,
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onRetry = onRetry,
                            isChildReply = true
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
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
