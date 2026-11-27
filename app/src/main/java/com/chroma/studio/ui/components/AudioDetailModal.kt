package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.chroma.studio.data.CatalogMusicTrack
import com.chroma.studio.data.MusicRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.X

private sealed interface AudioUsageGridItem {
    val id: String
    val thumbnailUrl: String
    val isFlash: Boolean
    val label: String

    data class FlashEntry(val video: FlashVideo) : AudioUsageGridItem {
        override val id: String = "flash_${video.id}"
        override val thumbnailUrl: String = video.thumbnailUrl
        override val isFlash: Boolean = true
        override val label: String = "${video.likesCount}"
    }

    data class PostEntry(val post: DiscoverPost) : AudioUsageGridItem {
        override val id: String = "post_${post.id}"
        override val thumbnailUrl: String =
            post.imageUrl ?: post.imageUrls.firstOrNull() ?: post.thumbnailUrl.orEmpty()
        override val isFlash: Boolean = false
        override val label: String = "${post.likesCount}"
    }
}

private fun formatTrackSec(sec: Int): String {
    val safe = sec.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}

/**
 * Instagram-style Audio Details Page / Sheet: displays album art, artist/album metadata,
 * description, trending & best-part window, live audio preview, bookmark toggle, "Use Audio" action,
 * and a 3-column grid of all Flash Videos and Posts using this same music track.
 */
@Composable
fun AudioDetailModal(
    trackId: String?,
    fallbackTitle: String? = null,
    fallbackArtist: String? = null,
    fallbackCoverUrl: String? = null,
    fallbackAudioUrl: String? = null,
    onDismiss: () -> Unit,
    onUseAudio: ((CatalogMusicTrack) -> Unit)? = null,
    onFlashClick: ((FlashVideo) -> Unit)? = null,
    onPostClick: ((DiscoverPost) -> Unit)? = null
) {
    val cleanTrackId = trackId?.takeIf { it.isNotBlank() } ?: return
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val repo = remember { MusicRepository.getInstance() }

    var track by remember(cleanTrackId) { mutableStateOf<CatalogMusicTrack?>(null) }
    var flashes by remember(cleanTrackId) { mutableStateOf<List<FlashVideo>>(emptyList()) }
    var posts by remember(cleanTrackId) { mutableStateOf<List<DiscoverPost>>(emptyList()) }
    var isLoading by remember(cleanTrackId) { mutableStateOf(true) }
    var isPlaying by remember(cleanTrackId) { mutableStateOf(false) }
    var isSaved by remember(cleanTrackId) { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                exoPlayer.stop()
                exoPlayer.release()
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(cleanTrackId) {
        isLoading = true
        val fetchedTrack = repo.fetchTrackById(context, cleanTrackId)
        val (usedFlashes, usedPosts) = repo.fetchMediaUsingTrack(cleanTrackId)
        track = fetchedTrack
        isSaved = fetchedTrack?.saved ?: (cleanTrackId in repo.getSavedTrackIds(context))
        flashes = usedFlashes
        posts = usedPosts
        isLoading = false
    }

    val effectiveTitle = track?.title ?: fallbackTitle.orEmpty().ifBlank { "Music Track" }
    val effectiveArtist = track?.artist ?: fallbackArtist.orEmpty().ifBlank { "Oscellion Audio" }
    val effectiveCover = track?.coverUrl?.takeIf { it.isNotBlank() } ?: fallbackCoverUrl.orEmpty()
    val effectiveAudio = track?.audioUrl?.takeIf { it.isNotBlank() } ?: fallbackAudioUrl.orEmpty()

    val gridItems = remember(flashes, posts) {
        flashes.map { AudioUsageGridItem.FlashEntry(it) } + posts.map { AudioUsageGridItem.PostEntry(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            color = colors.bg,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Audio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = repo.toggleSavedTrack(context, cleanTrackId)
                                isSaved = next
                                ToastManager.showToast(if (next) "Saved audio" else "Removed from Saved")
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Bookmark,
                                contentDescription = "Save audio",
                                tint = if (isSaved) Color(0xFF0381FE) else colors.textMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = colors.textMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Track Hero Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.glassBg)
                                .border(0.75.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                                .clickable(enabled = effectiveAudio.isNotBlank()) {
                                    if (isPlaying) {
                                        try { exoPlayer.pause() } catch (_: Throwable) {}
                                        isPlaying = false
                                    } else {
                                        try {
                                            exoPlayer.setMediaItem(MediaItem.fromUri(effectiveAudio))
                                            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                                            exoPlayer.prepare()
                                            val startSec = track?.bestPartStartSec ?: 0
                                            exoPlayer.seekTo(startSec * 1000L)
                                            exoPlayer.play()
                                            isPlaying = true
                                        } catch (_: Throwable) {
                                            isPlaying = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (effectiveCover.isNotBlank()) {
                                AsyncImage(
                                    model = effectiveCover,
                                    contentDescription = effectiveTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Lucide.Music,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            if (effectiveAudio.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.56f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = effectiveTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (track?.isTrending == true) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9F0A).copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Flame,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9F0A),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Trending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF9F0A)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = buildString {
                                    append(effectiveArtist)
                                    val alb = track?.album.orEmpty()
                                    if (alb.isNotBlank()) append(" • $alb")
                                },
                                fontSize = 13.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    val t = track
                                    if (t != null) {
                                        append("${t.category} • ${formatTrackSec(t.durationSec)} • ")
                                    }
                                    append("${gridItems.size} posts & Flashes")
                                },
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    track?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            fontSize = 12.5.sp,
                            color = colors.textMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    track?.let { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9F0A).copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9F0A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Best part: ${formatTrackSec(t.bestPartStartSec)}–${formatTrackSec(t.bestPartEndSec)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF9F0A)
                                )
                            }
                        }
                    }

                    if (onUseAudio != null && track != null && track?.status == "active") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE))))
                                .clickable {
                                    onUseAudio(track!!)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Use Audio",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.75.dp)
                        .background(colors.glassBorder)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.5.dp)
                    }
                } else if (gridItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Posts or Flash videos have used this track yet.",
                            fontSize = 13.5.sp,
                            color = colors.textMuted
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(gridItems, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.75f)
                                    .background(colors.glassBg)
                                    .clickable {
                                        when (item) {
                                            is AudioUsageGridItem.FlashEntry -> onFlashClick?.invoke(item.video)
                                            is AudioUsageGridItem.PostEntry -> onPostClick?.invoke(item.post)
                                        }
                                        onDismiss()
                                    }
                            ) {
                                if (item.thumbnailUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Icon(
                                    imageVector = if (item.isFlash) Lucide.Film else Lucide.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(15.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Play,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = item.label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
