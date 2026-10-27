package com.chroma.studio.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.chroma.studio.model.FlashDraft
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.GlassDialog
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
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
    emptyContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    val flashFeed by flashVm.flashFeed.collectAsState()
    val isLoading by flashVm.isLoading.collectAsState()
    val isRefreshing by flashVm.isRefreshing.collectAsState()
    val likedVideoIds by flashVm.likedVideoIds.collectAsState()
    val drafts by flashVm.drafts.collectAsState()

    var selectedTab by remember { mutableStateOf("Reels") }
    val pagerState = rememberPagerState(pageCount = { flashFeed.size.coerceAtLeast(1) })

    // ExoPlayer Pool of 3 recycled players using CacheDataSource
    val exoPlayers = remember {
        val cache = com.chroma.studio.utils.MediaCacheManager.getInstance(context)
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        List(3) {
            androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayers.forEach { it.release() } }
    }

    // Drafts bottom sheet
    var showDraftSheet by remember { mutableStateOf(false) }

    // Global mute state
    var isMuted by remember { mutableStateOf(false) }

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
                                putExtra(Intent.EXTRA_TEXT, "Check out this Flash on Chroma: https://chroma.studio/flash/${video.id}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        },
                        onAuthorClick = { onNavigateToProfile(video.creatorId) },
                        flashVm = flashVm,
                        commentVm = commentVm,
                        onRemixRequested = onRemixRequested
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
                        onDelete = { flashVm.deleteDraft(draft.id) }
                    )
                }
            }
        }
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
    onRemixRequested: (Uri, String, String) -> Unit
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
    LaunchedEffect(video.videoUrl) {
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(video.videoUrl))
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
    LaunchedEffect(isActive, lifecycleEvent) {
        if (isActive && (lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_RESUME || lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_ANY)) {
            player.play()
        } else {
            player.pause()
        }
    }

    val colors = LocalOscellionColors.current
    val doubleTapLikeState = rememberDoubleTapLikeState()
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var showPlayPauseOverlay by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val isOwner = auth.currentUser != null && auth.currentUser!!.uid == video.creatorId
    var currentPosition by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }

    // Sync volume with global mute state
    LaunchedEffect(isMuted) {
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
                        if (!isLiked) {
                            onLike()
                        }
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { onAuthorClick() }
                ) {
                    if (video.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = video.authorAvatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                video.authorDisplayName.firstOrNull()?.toString() ?: "?",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
                if (auth.currentUser == null || auth.currentUser!!.uid != video.creatorId) {
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
                onReplyWithFlash = {
                    showComments = false
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
                    ToastManager.showToast("Saving video to gallery...")
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog) {
            GlassDialog(
                onDismissRequest = { showDeleteConfirmDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Delete Flash Video?",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Are you sure you want to permanently delete this Flash video? This action cannot be undone.",
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LiquidGlassSecondaryButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmDialog = false },
                            isSmall = true
                        )
                        Spacer(Modifier.width(8.dp))
                        GlassTextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                flashVm.deleteFlash(video.id) { success ->
                                    if (success) {
                                        ToastManager.showToast("Flash video deleted")
                                    } else {
                                        ToastManager.showToast("Failed to delete video")
                                    }
                                }
                            }
                        ) {
                            Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
    onReplyWithFlash: (String?) -> Unit
) {
    val colors = LocalOscellionColors.current
    val comments by commentVm.comments.collectAsState()
    val isLoading by commentVm.isLoading.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(video.id) {
        commentVm.loadComments(video.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            val displayCommentCount = maxOf(video.commentCount, comments.size)
            Text(
                text = "$displayCommentCount comments",
                color = Color(0xFF1C1C1E),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    OscellionLoadingSpinner(color = colors.primary)
                }
            } else if (comments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Lucide.MessageCircle, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Be the first to comment", color = Color(0xFF6B7280))
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(comments) { comment ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            AsyncImage(
                                model = comment.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E7EB))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        comment.username,
                                        color = Color(0xFF1C1C1E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (comment.verified) {
                                        Spacer(Modifier.width(4.dp))
                                        VerifiedBadge(size = 12.dp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (comment.isPending) {
                                        Text(
                                            "Sending...",
                                            color = Color(0xFF007AFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (comment.isError) {
                                        Text(
                                            "Failed • Tap to retry",
                                            color = Color(0xFFFF3B30),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { commentVm.retryComment(comment) }
                                        )
                                    } else {
                                        Text(
                                            android.text.format.DateUtils.getRelativeTimeSpanString(
                                                comment.timestamp,
                                                System.currentTimeMillis(),
                                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                                            ).toString(),
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    comment.text,
                                    color = if (comment.isError) Color(0xFF9CA3AF) else Color(0xFF374151),
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                if (!comment.isPending && !comment.isError) {
                                    Text(
                                        "Reply",
                                        color = Color(0xFF6B7280),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            onReplyWithFlash(comment.id)
                                        }
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                                val liked = FirebaseAuth.getInstance().currentUser?.uid?.let { comment.likedBy.contains(it) } == true
                                Icon(
                                    imageVector = if (liked) ChromaIcons.HeartFilled else Lucide.Heart,
                                    contentDescription = "Like",
                                    tint = if (liked) Color(0xFFFF3B30) else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp).clickable { commentVm.toggleLike(comment.id) }
                                )
                                if (comment.likesCount > 0) {
                                    Text(
                                        comment.likesCount.toString(),
                                        color = Color(0xFF6B7280),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Row at bottom of comment sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = FirebaseAuth.getInstance().currentUser?.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE5E7EB))
                )
                Spacer(Modifier.width(12.dp))
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add comment...", color = Color(0xFF9CA3AF)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1C1C1E),
                        unfocusedTextColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )
                if (commentText.isNotBlank()) {
                    IconButton(onClick = {
                        commentVm.addComment(video.id, commentText)
                        commentText = ""
                    }) {
                        Icon(Lucide.Send, "Send", tint = Color(0xFF007AFF))
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
