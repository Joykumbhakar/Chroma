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

/**
 * Resolves all media slides for a [DiscoverPost].
 * Supports:
 * 1. Multi-image array (`imageUrls`)
 * 2. Multi-work array (`workUrls`)
 * 3. Comma-separated image URLs in `imageUrl`
 * 4. Comma-separated JSON URLs in `jsonUrl`
 * 5. Single work or image fallback
 */
fun resolvePostMediaSlides(work: DiscoverPost): List<PostMediaSlide> {
    val slides = mutableListOf<PostMediaSlide>()

    // 0. Video post (postType = "video") — highest priority
    if (work.postType == "video" && !work.videoUrl.isNullOrBlank()) {
        slides.add(PostMediaSlide.VideoSlide(work.videoUrl.trim(), work.thumbnailUrl))
        return slides
    }

    // 1. Array of images
    if (work.imageUrls.isNotEmpty()) {
        work.imageUrls.forEach { url ->
            if (url.isNotBlank()) slides.add(PostMediaSlide.ImageSlide(url.trim()))
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
                .forEach { slides.add(PostMediaSlide.ImageSlide(it)) }
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
        if (work.postType == "image" && !work.imageUrl.isNullOrBlank()) {
            slides.add(PostMediaSlide.ImageSlide(work.imageUrl.trim()))
        } else if (work.jsonUrl.isNotBlank()) {
            slides.add(PostMediaSlide.WorkSlide(work.jsonUrl.trim()))
        } else if (!work.imageUrl.isNullOrBlank()) {
            slides.add(PostMediaSlide.ImageSlide(work.imageUrl.trim()))
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
 * Shows thumbnail while buffering, plays/pauses on tap with an ExoPlayer.
 */
@Composable
fun PostVideoSlideView(
    videoUrl: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }

    val player = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_READY) isPlayerReady = true
                }
            })
        }
    }
    DisposableEffect(videoUrl) { onDispose { player.release() } }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Video player surface
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { frame ->
                    val tv = android.view.TextureView(ctx)
                    frame.addView(tv,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    player.setVideoTextureView(tv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Thumbnail poster while buffering
        if (!isPlayerReady && !thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tap-to-play/pause overlay
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (isPlaying) player.pause() else player.play() },
            contentAlignment = Alignment.Center
        ) {
            if (!isPlaying) {
                Icon(
                    imageVector = com.composables.icons.lucide.Lucide.Play,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageIndex) {
                            detectTapGestures(
                                onDoubleTap = onDoubleTap
                            )
                        },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(work.id) {
                        detectTapGestures(
                            onDoubleTap = onDoubleTap
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    singleSlide is PostMediaSlide.VideoSlide -> {
                        PostVideoSlideView(
                            videoUrl = singleSlide.videoUrl,
                            thumbnailUrl = singleSlide.thumbnailUrl,
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
