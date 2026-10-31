package com.chroma.studio.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-screen viewer: swipe between items, pinch / double-tap zoom, swipe-down to dismiss,
 * inline video playback, info card, favourite, select, share and a synced filmstrip.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GalleryLightbox(
    items: List<MediaItem>,
    startIndex: Int,
    selection: SelectionState,
    favorites: Set<String>,
    imageLoader: ImageLoader,
    onToggleSelect: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onNext: (MediaItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var infoVisible by remember { mutableStateOf(false) }
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 120.dp.toPx() }

    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
    val isSelected = current.key in selection.set
    val isFavorite = current.key in favorites

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val bgAlpha = (1f - abs(dragY.value) / (dismissThreshold * 3f)).coerceIn(0.15f, 1f)

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.97f * bgAlpha))) {

            // ---- pager (swipe-down-to-dismiss lives here) -----------------------------------
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragY.value.roundToInt()) }
                    .pointerInput(zoomed) {
                        if (!zoomed) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(dragY.value) > dismissThreshold) onDismiss() else dragY.animateTo(0f, spring())
                                    }
                                },
                                onDragCancel = { scope.launch { dragY.animateTo(0f, spring()) } },
                            ) { change, amount ->
                                change.consume()
                                scope.launch { dragY.snapTo(dragY.value + amount) }
                            }
                        }
                    },
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !zoomed,
                    key = { items[it].key },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val item = items[page]
                    val isCurrent = pagerState.currentPage == page
                    if (item.isVideo) {
                        VideoPage(item, isCurrent, imageLoader) { chromeVisible = !chromeVisible }
                    } else {
                        ZoomableImage(
                            item = item,
                            isCurrent = isCurrent,
                            imageLoader = imageLoader,
                            onZoomChanged = { if (isCurrent) zoomed = it },
                            onTap = { chromeVisible = !chromeVisible },
                        )
                    }
                }
            }

            val chromeShown = chromeVisible && dragY.value == 0f

            // ---- top bar ---------------------------------------------------------------------
            AnimatedVisibility(
                chromeShown, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("${pagerState.currentPage + 1} of ${items.size}", color = Color.White.copy(alpha = 0.65f), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Text(current.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    LightboxIcon(Icons.Rounded.Info, "Details", active = infoVisible) { infoVisible = !infoVisible }
                    LightboxIcon(Icons.Rounded.Share, "Share") { onShare(current) }
                    LightboxIcon(Icons.Rounded.Close, "Close", onClick = onDismiss)
                }
            }

            // ---- bottom area -------------------------------------------------------------------
            AnimatedVisibility(
                chromeShown, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedVisibility(infoVisible) { InfoCard(current) }

                    Filmstrip(items, pagerState.currentPage, selection, imageLoader) { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DarkPill(onClick = { onToggleFavorite(current) }) {
                                Icon(
                                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    if (isFavorite) "Remove favourite" else "Add favourite",
                                    tint = if (isFavorite) Color(0xFFFF3B30) else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            DarkPill(onClick = { onToggleSelect(current) }, highlighted = isSelected) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    if (isSelected) "Selected (${selection.numberOf(current.key)})" else "Select",
                                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Row(
                            Modifier.height(40.dp).clip(CircleShape).background(Gallery.colors.primary)
                                .clickable { onNext(current) }.padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Next", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun LightboxIcon(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(if (active) Color.White.copy(alpha = 0.18f) else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun DarkPill(onClick: () -> Unit, highlighted: Boolean = false, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val primary = Gallery.colors.primary
    Row(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (highlighted) primary else Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun InfoCard(item: MediaItem) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C1C1E)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Media Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(formatSize(item.sizeBytes), color = Gallery.colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        InfoLine(item.formattedDate(), if (item.width > 0) "${item.width} x ${item.height}" else "—")
        InfoLine("Album: ${item.bucketName}", item.mimeType.substringAfter('/').uppercase() +
            if (item.isVideo) " • ${formatDuration(item.durationMs)}" else "")
    }
}

@Composable
private fun InfoLine(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, color = Color.White.copy(alpha = 0.75f), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(right, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}

@Composable
private fun Filmstrip(
    items: List<MediaItem>,
    currentPage: Int,
    selection: SelectionState,
    imageLoader: ImageLoader,
    onSelectPage: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val itemPx = with(LocalDensity.current) { 48.dp.toPx() }

    LaunchedEffect(currentPage) {
        val center = (listState.layoutInfo.viewportSize.width / 2f - itemPx / 2f).roundToInt()
        listState.animateScrollToItem(currentPage, -center)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(items, key = { _, it -> it.key }) { index, item ->
            val active = index == currentPage
            val selected = item.key in selection.set
            Box(
                Modifier.size(48.dp)
                    .graphicsLayer { alpha = if (active) 1f else 0.55f }
                    .then(if (active) Modifier.border(2.dp, Color.White) else Modifier)
                    .clickable { onSelectPage(index) },
            ) {
                AsyncImage(
                    model = thumbRequest(item, 160), imageLoader = imageLoader, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
                if (selected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(2.dp).size(14.dp).clip(CircleShape).background(Gallery.colors.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(9.dp)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Zoomable image: pinch, pan, double-tap. Single-finger drags at 1x are left alone so the pager
// can swipe and the dismiss gesture can work.
// ---------------------------------------------------------------------------------------------

@Composable
private fun ZoomableImage(
    item: MediaItem,
    isCurrent: Boolean,
    imageLoader: ImageLoader,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1f) / 2f
        val maxY = size.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) { scale = 1f; offset = Offset.Zero }
    }
    LaunchedEffect(scale > 1.02f) { onZoomChanged(scale > 1.02f) }

    val request = remember(item.uri) {
        ImageRequest.Builder(context).data(item.uri).size(2048).crossfade(true).build()
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        scope.launch {
                            val from = scale
                            val fromOffset = offset
                            val target = if (from > 1.05f) 1f else 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val targetOffset = if (target == 1f) Offset.Zero else clamp((center - tap) * (target - 1f), target)
                            animate(0f, 1f, animationSpec = spring(stiffness = 400f)) { t, _ ->
                                scale = from + (target - from) * t
                                offset = fromOffset + (targetOffset - fromOffset) * t
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val multiTouch = event.changes.size > 1
                        if (multiTouch || scale > 1.01f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            if (centroid != Offset.Unspecified) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val focus = centroid - c
                                val raw = focus - ((focus - offset) / scale) * newScale + pan
                                offset = clamp(raw, newScale)
                            } else {
                                offset = clamp(offset + pan, newScale)
                            }
                            scale = newScale
                            if (newScale <= 1.01f && !multiTouch) offset = Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale < 1.05f) { scale = 1f; offset = Offset.Zero }
                }
            }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request, imageLoader = imageLoader, contentDescription = item.name,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Video page: poster + play button until the page is current, then Media3 takes over.
// ---------------------------------------------------------------------------------------------

@Composable
private fun VideoPage(item: MediaItem, isCurrent: Boolean, imageLoader: ImageLoader, onTap: () -> Unit) {
    val context = LocalContext.current
    var started by remember(item.key) { mutableStateOf(false) }

    LaunchedEffect(isCurrent) { if (!isCurrent) started = false }

    Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onTap() }, contentAlignment = Alignment.Center) {
        if (isCurrent && started) {
            val player = remember(item.uri) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(ExoMediaItem.fromUri(item.uri))
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = thumbRequest(item, 1080), imageLoader = imageLoader, contentDescription = item.name,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable { started = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, "Play video", tint = Color.White, modifier = Modifier.size(32.dp)) }
        }
    }
}
