package com.chroma.studio.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.data.AttachedMusicSelection
import com.chroma.studio.data.PendingMusicHandoff
import com.chroma.studio.ui.components.AudioDetailModal
import com.chroma.studio.ui.components.ToastManager
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play

@Composable
fun ChatMusicTrackBubbleCard(
    message: ChatMessage,
    isOutgoing: Boolean,
    onOpenFlash: ((String) -> Unit)? = null
) {
    val chatColors = LocalChatColors.current
    val context = LocalContext.current
    val audioUrl = message.musicAudioUrl?.takeIf { it.isNotBlank() } ?: message.mediaUrl.orEmpty()
    val trackTitle = message.musicTitle?.takeIf { it.isNotBlank() }
        ?: message.text?.substringBefore(" · ")?.takeIf { it.isNotBlank() }
        ?: "Music Track"
    val trackArtist = message.musicArtist?.takeIf { it.isNotBlank() } ?: "Chroma Music"
    val coverUrl = message.musicCoverUrl?.takeIf { it.isNotBlank() }

    var isPlaying by remember(message.id) { mutableStateOf(false) }
    var showDetailModal by remember(message.id) { mutableStateOf(false) }

    val exoPlayer = remember(message.id) {
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        ExoPlayer.Builder(context)
            .setAudioAttributes(attrs, true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(audioUrl) {
        if (audioUrl.isBlank()) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    val cardBg = if (isOutgoing) Color.White.copy(alpha = 0.16f) else chatColors.surfaceSecondary.copy(alpha = 0.78f)
    val borderColor = if (isOutgoing) Color.White.copy(alpha = 0.24f) else chatColors.borderSubtle
    val primaryText = if (isOutgoing) Color.White else chatColors.textDark
    val secondaryText = if (isOutgoing) Color.White.copy(alpha = 0.8f) else chatColors.textMuted

    Row(
        modifier = Modifier
            .widthIn(min = 216.dp, max = 280.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(0.75.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !message.musicTrackId.isNullOrBlank()) {
                if (isPlaying) {
                    exoPlayer.pause()
                }
                showDetailModal = true
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFA2D48).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = trackTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = null,
                    tint = Color(0xFFFA2D48),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trackTitle,
                color = primaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trackArtist,
                color = secondaryText,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val windowEnd = message.musicStartSec + message.musicDurationSec.coerceAtLeast(1)
            Text(
                text = "${formatChatMusicSec(message.musicStartSec)}–${formatChatMusicSec(windowEnd)} clip",
                color = secondaryText.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.width(2.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White else Color(0xFFFA2D48))
                .clickable(enabled = audioUrl.isNotBlank()) {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        if (exoPlayer.mediaItemCount == 0) {
                            exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
                            exoPlayer.prepare()
                            exoPlayer.seekTo((message.musicStartSec * 1000L).coerceAtLeast(0L))
                        }
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isOutgoing) Color(0xFFFA2D48) else Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }

    if (showDetailModal && !message.musicTrackId.isNullOrBlank()) {
        AudioDetailModal(
            trackId = message.musicTrackId,
            fallbackTitle = trackTitle,
            fallbackArtist = trackArtist,
            fallbackCoverUrl = coverUrl,
            fallbackAudioUrl = audioUrl,
            onDismiss = { showDetailModal = false },
            onUseAudio = { track ->
                showDetailModal = false
                PendingMusicHandoff.set(
                    AttachedMusicSelection(
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
                ToastManager.showToast("Selected ${track.title} — open Flash or Story to use")
            },
            onFlashClick = { clickedFlash ->
                showDetailModal = false
                onOpenFlash?.invoke(clickedFlash.id)
            }
        )
    }
}

private fun formatChatMusicSec(sec: Int): String {
    val safe = sec.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}
