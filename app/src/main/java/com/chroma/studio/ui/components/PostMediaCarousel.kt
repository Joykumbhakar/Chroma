@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.graphicsLayer
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.composables.icons.lucide.Film
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.ui.theme.OscellionPalette
import com.chroma.studio.ui.screens.LayerCache
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Zap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Type-safe media slide descriptor for community posts supporting multi-image and multi-work feeds.
 */
sealed class PostMediaSlide {
    data class ImageSlide(val url: String) : PostMediaSlide()
    data class WorkSlide(val jsonUrl: String) : PostMediaSlide()
    data class VideoSlide(val videoUrl: String, val thumbnailUrl: String? = null) : PostMediaSlide()
}

fun isVideoMediaUrl(url: String): Boolean {
    val lower = url.lowercase().substringBefore('?')
    return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") ||
           lower.endsWith(".m4v") || lower.endsWith(".3gp") || lower.contains("/video/upload/") ||
           lower.contains("flash_video") || lower.contains("video_upload")
}

fun isVideoPost(work: DiscoverPost): Boolean {
    if (work.postType.equals("video", ignoreCase = true) ||
        work.postType.equals("flash", ignoreCase = true) ||
        !work.videoUrl.isNullOrBlank()) {
        return true
    }
    if (work.imageUrls.any { isVideoMediaUrl(it) }) return true
    if (!work.imageUrl.isNullOrBlank() && work.imageUrl.split(",").any { isVideoMediaUrl(it.trim()) }) return true
    return false
}

fun extractVideoUrlFromPost(work: DiscoverPost): String? {
    if (!work.videoUrl.isNullOrBlank()) return work.videoUrl.trim()
    if (work.imageUrls.isNotEmpty()) {
        val match = work.imageUrls.firstOrNull { isVideoMediaUrl(it) }
        if (!match.isNullOrBlank()) return match.trim()
    }
    if (!work.imageUrl.isNullOrBlank()) {
        val match = work.imageUrl.split(",").map { it.trim() }.firstOrNull { isVideoMediaUrl(it) }
        if (!match.isNullOrBlank()) return match
        if (work.postType.equals("video", ignoreCase = true) || work.postType.equals("flash", ignoreCase = true)) {
            return work.imageUrl.trim()
        }
    }
    return null
}

/**
 * Resolves all media slides for a [DiscoverPost].
 * Supports:
 * 1. Single and multi-video Flash posts
 * 2. Mixed video and image carousels
 * 3. Multi-image array (`imageUrls`)
 * 4. Multi-work array (`workUrls`)
 * 5. Comma-separated media fallbacks
 */
fun resolvePostMediaSlides(work: DiscoverPost): List<PostMediaSlide> {
    val slides = mutableListOf<PostMediaSlide>()

    // 0. Explicit Video post (postType = "video" or "flash", or explicit videoUrl)
    val isExplicitVideo = work.postType.equals("video", ignoreCase = true) ||
                          work.postType.equals("flash", ignoreCase = true) ||
                          !work.videoUrl.isNullOrBlank()

    if (isExplicitVideo && !work.videoUrl.isNullOrBlank()) {
        slides.add(PostMediaSlide.VideoSlide(work.videoUrl.trim(), work.thumbnailUrl))
        return slides
    }

    // 1. Array of images/videos (supports multi-video or mixed media carousels from editor)
    if (work.imageUrls.isNotEmpty()) {
        work.imageUrls.forEach { url ->
            val trimmed = url.trim()
            if (trimmed.isNotBlank()) {
                if (isVideoMediaUrl(trimmed)) {
                    slides.add(PostMediaSlide.VideoSlide(trimmed, work.thumbnailUrl))
                } else {
                    slides.add(PostMediaSlide.ImageSlide(trimmed))
                }
            }
        }
    }

    // 2. Array of work JSON URLs
    if (work.workUrls.isNotEmpty()) {
        work.workUrls.forEach { jsonUrl ->
            if (jsonUrl.isNotBlank()) slides.add(PostMediaSlide.WorkSlide(jsonUrl.trim()))
        }
    }

    // 3. Comma-separated imageUrl string fallback
    if (slides.isEmpty() && !work.imageUrl.isNullOrBlank()) {
        if (work.imageUrl.contains(",")) {
            work.imageUrl.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { url ->
                    if (isVideoMediaUrl(url)) {
                        slides.add(PostMediaSlide.VideoSlide(url, work.thumbnailUrl))
                    } else {
                        slides.add(PostMediaSlide.ImageSlide(url))
                    }
                }
        }
    }

    // 4. Comma-separated jsonUrl string fallback
    if (slides.isEmpty() && work.jsonUrl.isNotBlank() && work.jsonUrl.contains(",")) {
        work.jsonUrl.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { slides.add(PostMediaSlide.WorkSlide(it)) }
    }

    // 5. Single media fallback
    if (slides.isEmpty()) {
        if (!work.videoUrl.isNullOrBlank()) {
            slides.add(PostMediaSlide.VideoSlide(work.videoUrl.trim(), work.thumbnailUrl))
        } else if (!work.imageUrl.isNullOrBlank()) {
            val url = work.imageUrl.trim()
            if (isVideoMediaUrl(url)) {
                slides.add(PostMediaSlide.VideoSlide(url, work.thumbnailUrl))
            } else {
                slides.add(PostMediaSlide.ImageSlide(url))
            }
        } else if (work.jsonUrl.isNotBlank()) {
            slides.add(PostMediaSlide.WorkSlide(work.jsonUrl.trim()))
        }
    }

    return slides
}

/**
 * Animated Apple HIG & Instagram style scroll dots pagination indicator.
 * - Active dot expands to 16dp pill in vibrant iOS accent blue (#007AFF) with spring physics.
 * - Inactive dots render as 6dp translucent circles.
 * - When total dots exceed 5, a sliding window is applied with edge dots scaled to 4dp to hint at remaining slides.
 * - Dots are directly clickable to smoothly jump to any slide.
 */
@Composable
fun PostScrollDotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    isDark: Boolean,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalDots <= 1) return

    val maxVisibleDots = 5
    val windowStart = if (totalDots <= maxVisibleDots) {
        0
    } else {
        (selectedIndex - (maxVisibleDots / 2)).coerceIn(0, totalDots - maxVisibleDots)
    }
    val windowEnd = (windowStart + maxVisibleDots).coerceAtMost(totalDots)

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in windowStart until windowEnd) {
            val isSelected = i == selectedIndex
            val isEdge = totalDots > maxVisibleDots && (
                (i == windowStart && windowStart > 0) ||
                (i == windowEnd - 1 && windowEnd < totalDots)
            )

            val targetWidth = when {
                isSelected -> 16.dp
                isEdge -> 4.dp
                else -> 6.dp
            }
            val targetHeight = when {
                isEdge -> 4.dp
                else -> 6.dp
            }

            val dotWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
                label = "dotWidth_$i"
            )
            val dotHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
                label = "dotHeight_$i"
            )

            val baseInactiveColor = if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.20f)
            val edgeInactiveColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f)

            val dotColor by animateColorAsState(
                targetValue = when {
                    isSelected -> Color(0xFF007AFF)
                    isEdge -> edgeInactiveColor
                    else -> baseInactiveColor
                },
                animationSpec = tween(durationMillis = 200),
                label = "dotColor_$i"
            )

            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = dotHeight)
                    .clip(CircleShape)
                    .background(dotColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDotClick(i)
                    }
            )
        }
    }
}

/**
 * Dedicated renderer for a work JSON slide with LRU caching.
 */
@Composable
fun PostWorkSlideView(
    jsonUrl: String,
    repository: WorkRepository,
    colors: OscellionPalette = LocalOscellionColors.current,
    modifier: Modifier = Modifier
) {
    var layers by remember(jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(jsonUrl)) }

    LaunchedEffect(jsonUrl) {
        if (layers == null && jsonUrl.isNotBlank()) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(jsonUrl)
                LayerCache.put(jsonUrl, parsed)
                parsed
            }
        }
    }

    if (layers != null && layers!!.isNotEmpty()) {
        CanvasPreview(
            layers = layers!!,
            shape = "full",
            borderColor = Color.Transparent,
            isStatic = true,
            timeMillisOverride = 5000L,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                            if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AiWaveSphereIcon(modifier = Modifier.size(44.dp))
        }
    }
}

/**
 * Renders a video slide for community feed posts inline.
 * - Tap anywhere on video to Play/Pause.
 * - Clean solid white Play icon with NO background overlay when paused.
 * - Floating audio Mute/Unmute button in the bottom-right corner.
 * - Slim progress bar at bottom showing playback position.
 * - Automatic looping with repeat mode.
 * - Lifecycle-aware: pauses when app goes to background.
 * - Double tap forwards to post double-tap like.
 */
@Composable
fun PostVideoSlideView(
    videoUrl: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onDoubleTap: ((Offset) -> Unit)? = null,
    isActivePage: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) } // Feed videos default to muted
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val player = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = false
            volume = 0f
            prepare()
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        androidx.media3.common.Player.STATE_READY -> {
                            isPlayerReady = true
                            isBuffering = false
                        }
                        androidx.media3.common.Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        else -> {
                            isBuffering = false
                        }
                    }
                }
            })
        }
    }

    // Pause playback when swiped away in pager
    LaunchedEffect(isActivePage) {
        if (!isActivePage && isPlaying) {
            player.pause()
        }
    }

    // Sync volume with mute state
    LaunchedEffect(isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }

    // Smooth progress updates while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val dur = player.duration
            if (dur > 0L) {
                currentProgress = (player.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            kotlinx.coroutines.delay(120L)
        }
    }

    // Lifecycle observer: pause on background
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(videoUrl) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        onDoubleTap?.invoke(offset)
                    },
                    onTap = {
                        if (isPlaying) {
                            player.pause()
                        } else {
                            if (player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                player.seekTo(0L)
                            }
                            player.play()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Video player surface
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { frame ->
                    val tv = android.view.TextureView(ctx)
                    frame.addView(
                        tv,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    player.setVideoTextureView(tv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Thumbnail poster while buffering / not yet ready
        if (!isPlayerReady && !thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(thumbnailUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Clean Solid White Play Icon (ONLY visible when paused, NO background overlay)
        if (!isPlaying) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = "Play Video",
                tint = Color.White,
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        shadowElevation = 16f
                    }
            )
        }

        // Buffering indicator
        if (isBuffering && isPlaying) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color.White.copy(alpha = 0.85f),
                strokeWidth = 3.dp
            )
        }

        // Floating Audio Mute/Unmute toggle button in bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 10.dp, end = 10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isMuted = !isMuted
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Bottom slim video progress bar
        if (isPlayerReady) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentProgress)
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}

/**
 * Media viewport for community feed posts supporting single-item, multi-image, and multi-work carousels.
 * Encapsulates gestures, double-tap heart bursts, counter badges, and video overlays.
 */
@Composable
fun PostMediaCarousel(
    work: DiscoverPost,
    slides: List<PostMediaSlide>,
    pagerState: PagerState,
    repository: WorkRepository,
    colors: OscellionPalette = LocalOscellionColors.current,
    doubleTapLikeState: DoubleTapLikeState,
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(if (colors.isDark) Color(0xFF101012) else Color(0xFFE5E5EA)),
        contentAlignment = Alignment.Center
    ) {
        if (slides.size > 1) {
            // Multi-media carousel via HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val slide = slides.getOrNull(pageIndex)
                val isVideoSlide = slide is PostMediaSlide.VideoSlide
                val isActive = (pagerState.currentPage == pageIndex)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!isVideoSlide) {
                                Modifier.pointerInput(pageIndex) {
                                    detectTapGestures(
                                        onDoubleTap = onDoubleTap
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (slide) {
                        is PostMediaSlide.ImageSlide -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(slide.url).crossfade(true).build(),
                                contentDescription = "${work.title} - ${pageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        is PostMediaSlide.WorkSlide -> {
                            PostWorkSlideView(
                                jsonUrl = slide.jsonUrl,
                                repository = repository,
                                colors = colors,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is PostMediaSlide.VideoSlide -> {
                            PostVideoSlideView(
                                videoUrl = slide.videoUrl,
                                thumbnailUrl = slide.thumbnailUrl,
                                onDoubleTap = onDoubleTap,
                                isActivePage = isActive,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                AiWaveSphereIcon(modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                }
            }

            // Top-right glass slide counter badge (e.g. "1/3", "2/3")
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.5.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${slides.size}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Single slide: Direct rendering without pager overhead
            val singleSlide = slides.firstOrNull()
            val isVideoSlide = singleSlide is PostMediaSlide.VideoSlide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isVideoSlide) {
                            Modifier.pointerInput(work.id) {
                                detectTapGestures(
                                    onDoubleTap = onDoubleTap
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    singleSlide is PostMediaSlide.VideoSlide -> {
                        PostVideoSlideView(
                            videoUrl = singleSlide.videoUrl,
                            thumbnailUrl = singleSlide.thumbnailUrl,
                            onDoubleTap = onDoubleTap,
                            isActivePage = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    singleSlide is PostMediaSlide.ImageSlide -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(singleSlide.url).crossfade(true).build(),
                            contentDescription = work.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    singleSlide is PostMediaSlide.WorkSlide -> {
                        PostWorkSlideView(
                            jsonUrl = singleSlide.jsonUrl,
                            repository = repository,
                            colors = colors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    work.postType == "image" && !work.imageUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(work.imageUrl).crossfade(true).build(),
                            contentDescription = work.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    work.jsonUrl.isNotBlank() -> {
                        PostWorkSlideView(
                            jsonUrl = work.jsonUrl,
                            repository = repository,
                            colors = colors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !work.imageUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(work.imageUrl).crossfade(true).build(),
                            contentDescription = work.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                                            if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AiWaveSphereIcon(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }

        // Flash Video Controls & Duration Badge (matches creator_feed_mobile_app_ui.html)
        if (work.postType == "video" || work.postType == "flash") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.Play,
                        contentDescription = "Play Flash Video",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.90f))
                        .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Lucide.Zap,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (work.aspectRatio == "16:9") "2:14" else "0:28",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // Double Tap Heart Reaction Overlay (shared across single and multi-page slides)
        FlatDoubleTapHeartOverlay(
            state = doubleTapLikeState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
