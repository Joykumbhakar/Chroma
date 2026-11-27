package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.chroma.studio.data.AttachedMusicSelection
import com.chroma.studio.data.CatalogMusicTrack
import com.chroma.studio.data.MusicLyricLine
import com.chroma.studio.data.MusicMoodCollection
import com.chroma.studio.data.MusicRepository
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 1. APPLE HIG DESIGN TOKENS (EXACT VISUAL SPEC FROM TEMPLATE)
// ============================================================================

private data class MusicPickerColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val secondarySurface: Color,
    val elevatedSurface: Color,
    val controlTrack: Color,
    val activeThumb: Color,
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val separator: Color,
    val accent: Color,
    val switchActive: Color,
    val switchInactive: Color,
    val chorusMarker: Color,
    val coverTop: Color,
    val coverBottom: Color,
    val coverBorder: Color,
    val coverSpecular: Color,
    val waveformInactive: Color,
    val grabber: Color,
    val scrim: Color,
    val toastBg: Color,
    val toastText: Color
)

private val LightMusicPickerColors = MusicPickerColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    secondarySurface = Color(0xFFF2F2F7),
    elevatedSurface = Color(0xFFFFFFFF),
    controlTrack = Color(0x1F767680),
    activeThumb = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF000000),
    textBody = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF636366),
    textTertiary = Color(0xFF8E8E93),
    separator = Color(0x12000000),
    accent = Color(0xFF007AFF),
    switchActive = Color(0xFF34C759),
    switchInactive = Color(0xFFE5E5EA),
    chorusMarker = Color(0xFFFF9F0A),
    coverTop = Color(0xFFF9F9FB),
    coverBottom = Color(0xFFEAEAEE),
    coverBorder = Color(0x10000000),
    coverSpecular = Color(0xD9FFFFFF),
    waveformInactive = Color(0xFFC7C7CC),
    grabber = Color(0x26000000),
    scrim = Color(0x59000000),
    toastBg = Color(0xE81C1C1E),
    toastText = Color(0xFFFFFFFF)
)

private val DarkMusicPickerColors = MusicPickerColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    secondarySurface = Color(0xFF2C2C2E),
    elevatedSurface = Color(0xFF2C2C2E),
    controlTrack = Color(0x3D767680),
    activeThumb = Color(0xFF3A3A3C),
    textPrimary = Color(0xFFFFFFFF),
    textBody = Color(0xFFE5E5EA),
    textSecondary = Color(0xFFAEAEB2),
    textTertiary = Color(0xFF8E8E93),
    separator = Color(0x18FFFFFF),
    accent = Color(0xFF0A84FF),
    switchActive = Color(0xFF30D158),
    switchInactive = Color(0xFF3A3A3C),
    chorusMarker = Color(0xFFFF9F0A),
    coverTop = Color(0xFF363639),
    coverBottom = Color(0xFF242427),
    coverBorder = Color(0x1AFFFFFF),
    coverSpecular = Color(0x24FFFFFF),
    waveformInactive = Color(0xFF48484A),
    grabber = Color(0x33FFFFFF),
    scrim = Color(0x99000000),
    toastBg = Color(0xF2FFFFFF),
    toastText = Color(0xFF000000)
)

private val LocalMusicPickerColors = compositionLocalOf { LightMusicPickerColors }

@Composable
private fun rememberAnimatedMusicPickerColors(isDark: Boolean): MusicPickerColors {
    val target = if (isDark) DarkMusicPickerColors else LightMusicPickerColors
    val spec = tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)

    val background by animateColorAsState(target.background, spec, label = "bg")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val secondarySurface by animateColorAsState(target.secondarySurface, spec, label = "secSurface")
    val elevatedSurface by animateColorAsState(target.elevatedSurface, spec, label = "elevSurface")
    val controlTrack by animateColorAsState(target.controlTrack, spec, label = "ctrlTrack")
    val activeThumb by animateColorAsState(target.activeThumb, spec, label = "thumb")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textBody by animateColorAsState(target.textBody, spec, label = "textBody")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textSec")
    val separator by animateColorAsState(target.separator, spec, label = "sep")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val coverTop by animateColorAsState(target.coverTop, spec, label = "coverTop")
    val coverBottom by animateColorAsState(target.coverBottom, spec, label = "coverBottom")
    val waveformInactive by animateColorAsState(target.waveformInactive, spec, label = "wfInactive")

    return target.copy(
        background = background,
        surface = surface,
        secondarySurface = secondarySurface,
        elevatedSurface = elevatedSurface,
        controlTrack = controlTrack,
        activeThumb = activeThumb,
        textPrimary = textPrimary,
        textBody = textBody,
        textSecondary = textSecondary,
        separator = separator,
        accent = accent,
        coverTop = coverTop,
        coverBottom = coverBottom,
        waveformInactive = waveformInactive
    )
}

// ============================================================================
// 2. PICKER MODELS & LIVE FIREBASE / WAVEFORM ADAPTERS
// ============================================================================

private enum class PickerTab(val label: String) {
    FOR_YOU("For You"),
    BROWSE("Browse"),
    SAVED("Saved")
}

private data class PickerMusicTrack(
    val source: CatalogMusicTrack,
    val waveform: List<Float>
) {
    val id: String get() = source.id
    val title: String get() = source.title
    val artist: String get() = source.artist
    val album: String get() = source.album
    val coverUrl: String get() = source.coverUrl
    val audioUrl: String get() = source.audioUrl
    val durationSec: Int get() = source.durationSec.coerceAtLeast(1)
    val chorusSec: Int get() = source.bestPartStartSec.coerceIn(0, max(0, durationSec - 1))
    val bestPartLenSec: Int get() = (source.bestPartEndSec - source.bestPartStartSec).coerceIn(5, 60)
    val genre: String get() = source.category
    val collection: MusicMoodCollection get() = source.collection
    val explicit: Boolean get() = source.isExplicit
    val trending: Boolean get() = source.isTrending
    val saved: Boolean get() = source.saved
    val lyrics: List<MusicLyricLine> get() = source.lyrics
}

private data class SelectedMusicClip(
    val track: PickerMusicTrack,
    val startSec: Int,
    val durationSec: Int,
    val showLyricsSticker: Boolean
)

private fun buildDeterministicWaveform(seedIndex: Int, chorusRatio: Float): List<Float> {
    return List(44) { i ->
        val wave1 = sin((i + seedIndex * 3.1) * 0.42) * 0.32
        val wave2 = cos((i * 0.85) + seedIndex * 1.7) * 0.22
        val chorusBoost = if (abs(i / 44f - chorusRatio) < 0.14f) 0.22 else 0.0
        (0.52 + wave1 + wave2 + chorusBoost).toFloat().coerceIn(0.22f, 1.0f)
    }
}

private fun formatMusicTime(totalSec: Int): String {
    val safe = max(0, totalSec)
    val mins = safe / 60
    val secs = safe % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

private fun lyricAtTime(track: PickerMusicTrack, second: Int): String {
    if (track.lyrics.isEmpty()) {
        return "${track.artist} • ${track.title}"
    }
    var active = track.lyrics.first().text
    for (cue in track.lyrics) {
        if (second >= cue.timeSec) active = cue.text
    }
    return active
}

private fun highlightQueryMatch(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, cursor)
            if (matchIndex == -1) {
                append(text.substring(cursor))
                break
            }
            if (matchIndex > cursor) {
                append(text.substring(cursor, matchIndex))
            }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(matchIndex, matchIndex + trimmed.length))
            }
            cursor = matchIndex + trimmed.length
        }
    }
}

// ============================================================================
// 3. MODAL WRAPPER & MAIN SCREEN (USED ACROSS FLASH, POSTS, STORIES, CHATS)
// ============================================================================

/**
 * Full-screen Apple HIG Music Picker & Precision Clip Trimmer backed by Firebase `music_tracks`
 * and Music Cloudinary audio streaming. Use across Flash Video Editor, Post Creation, Stories, and Chats.
 */
@Composable
fun StoryMusicPickerModal(
    isOpen: Boolean,
    initialTrackId: String? = null,
    headerTitle: String = "Add Music",
    onDismiss: () -> Unit,
    onClipConfirmed: (AttachedMusicSelection) -> Unit
) {
    if (!isOpen) return
    val colors = LocalOscellionColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        StoryMusicPickerScreen(
            modifier = Modifier.fillMaxSize(),
            initialDarkTheme = colors.isDark,
            initialTrackId = initialTrackId,
            headerTitle = headerTitle,
            onCancel = onDismiss,
            onClipConfirmed = { selection ->
                onClipConfirmed(selection)
                onDismiss()
            }
        )
    }
}

@Composable
fun StoryMusicPickerScreen(
    modifier: Modifier = Modifier,
    initialDarkTheme: Boolean = LocalOscellionColors.current.isDark,
    initialTrackId: String? = null,
    headerTitle: String = "Add Music",
    onCancel: () -> Unit = {},
    onClipConfirmed: (AttachedMusicSelection) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val repo = remember { MusicRepository.getInstance() }

    var isDark by remember { mutableStateOf(initialDarkTheme) }
    val colors = rememberAnimatedMusicPickerColors(isDark = isDark)

    val tracks = remember { mutableStateListOf<PickerMusicTrack>() }
    var isCatalogLoading by remember { mutableStateOf(true) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogReloadNonce by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableStateOf(PickerTab.FOR_YOU) }
    var activeGenre by remember { mutableStateOf("All") }
    var activeCollection by remember { mutableStateOf<MusicMoodCollection?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Playback, Trimmer & Applied Selection State
    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedTrackForTrimmer by remember { mutableStateOf<PickerMusicTrack?>(null) }
    var appliedClip by remember { mutableStateOf<SelectedMusicClip?>(null) }
    var clipStartSec by remember { mutableIntStateOf(0) }
    var clipDurationSec by remember { mutableIntStateOf(15) }
    var showLyricsSticker by remember { mutableStateOf(true) }
    var playheadProgress by remember { mutableFloatStateOf(0f) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    fun showToast(message: String) {
        toastJob?.cancel()
        toastMessage = message
        toastJob = scope.launch {
            delay(1900L)
            toastMessage = null
        }
    }

    // Real Media3 ExoPlayer backed by Music Cloudinary URLs
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

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
                playheadProgress = 0f
                showToast("Could not stream preview (${error.errorCodeName})")
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (playingTrackId != null) {
                    isPlaying = playing
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            try {
                exoPlayer.stop()
                exoPlayer.release()
            } catch (_: Throwable) {}
        }
    }

    fun startPlayback(track: PickerMusicTrack, startFromSec: Int = clipStartSec) {
        val streamUrl = com.chroma.studio.data.normalizeMusicStreamUrl(track.audioUrl)
        if (streamUrl.isBlank()) {
            showToast("Preview stream unavailable for this track")
            return
        }
        playingTrackId = track.id
        isPlaying = true
        playheadProgress = 0f
        try {
            val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentUri != streamUrl) {
                exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.prepare()
            } else if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.seekTo((startFromSec.coerceIn(0, track.durationSec)) * 1000L)
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } catch (_: Throwable) {
            isPlaying = false
            showToast("Failed to start audio preview")
        }
    }

    fun stopPlayback(clearTrack: Boolean = false) {
        try {
            exoPlayer.pause()
            if (clearTrack) exoPlayer.stop()
        } catch (_: Throwable) {}
        isPlaying = false
        if (clearTrack) {
            playingTrackId = null
            playheadProgress = 0f
        }
    }

    fun togglePlayback(track: PickerMusicTrack) {
        if (playingTrackId == track.id && isPlaying) {
            stopPlayback(clearTrack = false)
        } else {
            val startAt = if (selectedTrackForTrimmer?.id == track.id) clipStartSec else track.chorusSec
            startPlayback(track, startAt)
        }
    }

    fun openTrimmer(track: PickerMusicTrack) {
        focusManager.clearFocus()
        keyboardController?.hide()
        selectedTrackForTrimmer = track
        if (appliedClip?.track?.id == track.id) {
            clipDurationSec = appliedClip!!.durationSec
            clipStartSec = appliedClip!!.startSec
            showLyricsSticker = appliedClip!!.showLyricsSticker
        } else {
            val defaultDur = track.bestPartLenSec.coerceIn(5, min(60, track.durationSec))
            clipDurationSec = when {
                defaultDur <= 15 -> 15
                defaultDur <= 30 -> 30
                else -> min(60, track.durationSec)
            }.coerceAtMost(track.durationSec)
            val maxStart = max(0, track.durationSec - clipDurationSec)
            clipStartSec = track.chorusSec.coerceIn(0, maxStart)
        }
        playheadProgress = 0f
        if (playingTrackId != track.id || !isPlaying) {
            startPlayback(track, clipStartSec)
        }
    }

    // Load live catalog from Firebase Firestore
    LaunchedEffect(catalogReloadNonce) {
        isCatalogLoading = true
        catalogError = null
        val result = repo.fetchActiveCatalogResult(context)
        tracks.clear()
        tracks.addAll(
            result.tracks.mapIndexed { idx, t ->
                val ratio = t.bestPartStartSec.toFloat() / max(1, t.durationSec).toFloat()
                PickerMusicTrack(
                    source = t,
                    waveform = buildDeterministicWaveform(idx, ratio)
                )
            }
        )
        catalogError = result.errorMessage
        isCatalogLoading = false
        if (!initialTrackId.isNullOrBlank()) {
            tracks.firstOrNull { it.id == initialTrackId }?.let { openTrimmer(it) }
        }
    }

    // Debounced seek when dragging scrubber/dial
    LaunchedEffect(clipStartSec, selectedTrackForTrimmer?.id, isPlaying) {
        val activeTrack = selectedTrackForTrimmer ?: return@LaunchedEffect
        if (!isPlaying || playingTrackId != activeTrack.id || activeTrack.audioUrl.isBlank()) return@LaunchedEffect
        delay(180L)
        try {
            exoPlayer.seekTo(clipStartSec * 1000L)
        } catch (_: Throwable) {}
    }

    // Sync playhead progress with live ExoPlayer playback position and loop clip window cleanly
    LaunchedEffect(isPlaying, playingTrackId, selectedTrackForTrimmer?.id, clipStartSec, clipDurationSec) {
        if (!isPlaying) return@LaunchedEffect
        val stepMs = 50L
        while (isActive && isPlaying) {
            delay(stepMs)
            val active = selectedTrackForTrimmer ?: tracks.firstOrNull { it.id == playingTrackId }
            val startSec = if (selectedTrackForTrimmer != null) clipStartSec else (active?.chorusSec ?: 0)
            val loopDur = if (selectedTrackForTrimmer != null) clipDurationSec else min(20, active?.durationSec ?: 20)
            val safeLoopDurMs = max(1, loopDur) * 1000L

            if (exoPlayer.playbackState == Player.STATE_READY) {
                val relMs = (exoPlayer.currentPosition - startSec * 1000L).coerceAtLeast(0L)
                val next = (relMs.toFloat() / safeLoopDurMs.toFloat())
                if (next >= 1f) {
                    playheadProgress = 0f
                    if (active != null && active.audioUrl.isNotBlank()) {
                        try { exoPlayer.seekTo(startSec * 1000L) } catch (_: Throwable) {}
                    }
                } else {
                    playheadProgress = next.coerceIn(0f, 1f)
                }
            }
        }
    }

    val dynamicGenres = remember(tracks.toList()) {
        val base = linkedSetOf("All", "Chill", "Upbeat", "Cinematic", "Acoustic")
        tracks.forEach { if (it.genre.isNotBlank()) base.add(it.genre) }
        base.map { g ->
            g to when (g) {
                "All" -> "All"
                "Chill" -> "Chill & Lo-Fi"
                else -> g
            }
        }
    }

    val filteredTracks = remember(tracks.toList(), activeTab, activeGenre, activeCollection, searchQuery) {
        tracks.filter { track ->
            if (activeTab == PickerTab.SAVED && !track.saved) return@filter false
            if (activeCollection != null && track.collection != activeCollection) return@filter false
            if (activeGenre != "All" && !track.genre.equals(activeGenre, ignoreCase = true)) return@filter false
            val q = searchQuery.trim()
            if (q.isNotEmpty()) {
                val matches = track.title.contains(q, ignoreCase = true) ||
                    track.artist.contains(q, ignoreCase = true) ||
                    track.album.contains(q, ignoreCase = true) ||
                    track.genre.contains(q, ignoreCase = true)
                if (!matches) return@filter false
            }
            true
        }
    }

    val savedCount = tracks.count { it.saved }
    val playingTrack = tracks.find { it.id == playingTrackId }
    val collectionTrackCounts = remember(tracks.toList()) {
        MusicMoodCollection.entries.associateWith { col -> tracks.count { it.collection == col } }
    }

    val sectionHeading = when {
        searchQuery.isNotBlank() -> "Top Results"
        activeCollection != null -> activeCollection!!.title
        activeTab == PickerTab.SAVED -> "Saved Tracks"
        activeTab == PickerTab.BROWSE -> if (activeGenre == "All") "Popular Right Now" else "$activeGenre Selection"
        else -> "Trending & Recommended"
    }

    val hasActiveFilters = searchQuery.isNotBlank() || activeGenre != "All" || activeCollection != null

    CompositionLocalProvider(LocalMusicPickerColors provides colors) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.surface)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PickerHeader(
                    title = headerTitle,
                    searchQuery = searchQuery,
                    activeTab = activeTab,
                    savedCount = savedCount,
                    isDark = isDark,
                    onCancel = {
                        stopPlayback(clearTrack = true)
                        appliedClip = null
                        onCancel()
                    },
                    onToggleTheme = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isDark = !isDark
                    },
                    onSearchChange = { searchQuery = it },
                    onTabSelected = { tab ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        activeTab = tab
                        activeCollection = null
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = if (playingTrack != null) 112.dp else 32.dp)
                ) {
                    if (activeTab != PickerTab.SAVED) {
                        item(key = "genres") {
                            GenreFilterStrip(
                                genreFilters = dynamicGenres,
                                activeGenre = activeGenre,
                                onSelectGenre = { genre ->
                                    activeGenre = genre
                                    activeCollection = null
                                }
                            )
                        }
                    }

                    if (activeTab == PickerTab.BROWSE) {
                        item(key = "moods") {
                            CuratedMoodsGrid(
                                activeCollection = activeCollection,
                                trackCounts = collectionTrackCounts,
                                onSelectCollection = { col ->
                                    activeCollection = if (activeCollection == col) null else col
                                },
                                onClearCollection = { activeCollection = null }
                            )
                        }
                    }

                    item(key = "section_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sectionHeading,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "${filteredTracks.size} ${if (filteredTracks.size == 1) "song" else "songs"}",
                                fontSize = 12.sp,
                                color = colors.textTertiary
                            )
                        }
                    }

                    if (isCatalogLoading) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.accent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else if (filteredTracks.isEmpty()) {
                        item(key = "empty") {
                            EmptySongsState(
                                isSavedEmpty = activeTab == PickerTab.SAVED && searchQuery.isBlank(),
                                errorMessage = if (activeTab != PickerTab.SAVED && tracks.isEmpty()) catalogError else null,
                                canResetFilters = hasActiveFilters,
                                onResetFilters = {
                                    searchQuery = ""
                                    activeGenre = "All"
                                    activeCollection = null
                                },
                                onRetryCatalog = { catalogReloadNonce += 1 }
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredTracks,
                            key = { _, item -> item.id }
                        ) { index, track ->
                            val isCurrentlyPlaying = playingTrackId == track.id && isPlaying
                            val trackAppliedClip = appliedClip?.takeIf { it.track.id == track.id }
                            SongRowItem(
                                track = track,
                                searchQuery = searchQuery,
                                isPlaying = isCurrentlyPlaying,
                                appliedClip = trackAppliedClip,
                                showDivider = index < filteredTracks.lastIndex,
                                onRowClick = { openTrimmer(track) },
                                onPlayClick = { togglePlayback(track) },
                                onBookmarkClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val nowSaved = repo.toggleSavedTrack(context, track.id)
                                    val idx = tracks.indexOfFirst { it.id == track.id }
                                    if (idx >= 0) {
                                        tracks[idx] = tracks[idx].copy(
                                            source = tracks[idx].source.copy(saved = nowSaved)
                                        )
                                    }
                                    showToast(if (nowSaved) "Saved to Library" else "Removed from Saved")
                                }
                            )
                        }
                    }
                }
            }

            // Floating Mini Player Bar
            AnimatedVisibility(
                visible = playingTrack != null && selectedTrackForTrimmer == null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                playingTrack?.let { track ->
                    FloatingMiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        progress = playheadProgress,
                        onOpenTrimmer = { openTrimmer(track) },
                        onTogglePlay = { togglePlayback(track) },
                        onDismissPlayer = { stopPlayback(clearTrack = true) }
                    )
                }
            }

            // Trimmer Sheet Scrim
            AnimatedVisibility(
                visible = selectedTrackForTrimmer != null,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedTrackForTrimmer = null }
                )
            }

            // Precision Clip Trimmer Bottom Sheet
            AnimatedVisibility(
                visible = selectedTrackForTrimmer != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedTrackForTrimmer?.let { track ->
                    ClipTrimmerBottomSheet(
                        track = track,
                        clipStartSec = clipStartSec,
                        clipDurationSec = clipDurationSec,
                        isPlaying = isPlaying && playingTrackId == track.id,
                        playheadProgress = playheadProgress,
                        showLyricsSticker = showLyricsSticker,
                        onCancel = { selectedTrackForTrimmer = null },
                        onDone = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val confirmed = SelectedMusicClip(
                                track = track,
                                startSec = clipStartSec,
                                durationSec = clipDurationSec,
                                showLyricsSticker = showLyricsSticker
                            )
                            appliedClip = confirmed
                            selectedTrackForTrimmer = null
                            stopPlayback(clearTrack = true)
                            onClipConfirmed(
                                AttachedMusicSelection(
                                    trackId = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    album = track.album,
                                    coverUrl = track.coverUrl,
                                    audioUrl = track.audioUrl,
                                    startSec = clipStartSec,
                                    durationSec = clipDurationSec,
                                    totalTrackSec = track.durationSec,
                                    showLyricsSticker = showLyricsSticker
                                )
                            )
                        },
                        onTogglePlay = { togglePlayback(track) },
                        onLyricsToggle = { showLyricsSticker = it },
                        onDurationPresetSelect = { dur ->
                            val effective = dur.coerceAtMost(track.durationSec)
                            clipDurationSec = effective
                            val maxStart = max(0, track.durationSec - effective)
                            clipStartSec = clipStartSec.coerceIn(0, maxStart)
                            playheadProgress = 0f
                        },
                        onClipWindowChange = { newStart, newDur ->
                            clipDurationSec = newDur
                            clipStartSec = newStart
                            playheadProgress = 0f
                        },
                        onJumpToChorus = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val maxStart = max(0, track.durationSec - clipDurationSec)
                            clipStartSec = min(maxStart, track.chorusSec)
                            playheadProgress = 0f
                            showToast("Best part (${formatMusicTime(clipStartSec)})")
                        }
                    )
                }
            }

            // Floating Toast Pill
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f, animationSpec = tween(180)),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.toastBg,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = toastMessage.orEmpty(),
                        color = colors.toastText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. TOP HEADER, SEARCH BAR & SLIDING SEGMENTED CONTROL
// ============================================================================

@Composable
private fun PickerHeader(
    title: String,
    searchQuery: String,
    activeTab: PickerTab,
    savedCount: Int,
    isDark: Boolean,
    onCancel: () -> Unit,
    onToggleTheme: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTabSelected: (PickerTab) -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(top = 10.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.grabber)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Text(
                text = "Cancel",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.secondarySurface)
                    .border(0.5.dp, colors.coverBorder, CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleTheme)
                    .semantics {
                        contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
                    }
            ) {
                if (isDark) {
                    Sun3DIcon(modifier = Modifier.size(16.dp))
                } else {
                    Moon3DIcon(modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.controlTrack)
                .padding(horizontal = 10.dp)
        ) {
            Search3DIcon(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search songs, artists, or moods",
                        fontSize = 15.sp,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.textTertiary)
                        .clickable(role = Role.Button) { onSearchChange("") }
                        .semantics { contentDescription = "Clear search" }
                ) {
                    CloseCrossIcon(
                        tint = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AppleSegmentedControl(
            activeTab = activeTab,
            savedCount = savedCount,
            onTabSelected = onTabSelected
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(colors.separator)
    )
}

@Composable
private fun AppleSegmentedControl(
    activeTab: PickerTab,
    savedCount: Int,
    onTabSelected: (PickerTab) -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val tabs = PickerTab.entries
    val selectedIndex = tabs.indexOf(activeTab)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.controlTrack)
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / tabs.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "segmentThumbOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (colors.isDark) 2.dp else 3.dp,
                    shape = RoundedCornerShape(7.dp),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(7.dp))
                .background(colors.activeThumb)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == activeTab
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                    animationSpec = tween(180),
                    label = "tabTextColor"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .semantics {
                            selected = isSelected
                            contentDescription = if (tab == PickerTab.SAVED && savedCount > 0) {
                                "${tab.label}, $savedCount saved"
                            } else {
                                tab.label
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onTabSelected(tab) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    if (tab == PickerTab.SAVED && savedCount > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = savedCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. GENRE FILTER STRIP & CURATED MOODS 2x2 GRID
// ============================================================================

@Composable
private fun GenreFilterStrip(
    genreFilters: List<Pair<String, String>>,
    activeGenre: String,
    onSelectGenre: (String) -> Unit
) {
    val colors = LocalMusicPickerColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genreFilters.forEach { (key, label) ->
            val selected = activeGenre == key
            val pillBg by animateColorAsState(
                targetValue = if (selected) colors.textPrimary else colors.secondarySurface,
                animationSpec = tween(160),
                label = "genreBg"
            )
            val pillText by animateColorAsState(
                targetValue = if (selected) colors.surface else colors.textBody,
                animationSpec = tween(160),
                label = "genreText"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg)
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab) { onSelectGenre(key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = pillText
                )
            }
        }
    }
}

@Composable
private fun CuratedMoodsGrid(
    activeCollection: MusicMoodCollection?,
    trackCounts: Map<MusicMoodCollection, Int>,
    onSelectCollection: (MusicMoodCollection) -> Unit,
    onClearCollection: () -> Unit
) {
    val colors = LocalMusicPickerColors.current

    fun countLabel(col: MusicMoodCollection): String {
        val count = trackCounts[col] ?: 0
        return "$count ${if (count == 1) "Track" else "Tracks"}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CURATED MOODS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = colors.textTertiary
            )
            if (activeCollection != null) {
                Text(
                    text = "Clear",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClearCollection)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CuratedMoodCard(
                collection = MusicMoodCollection.MORNING_COFFEE,
                countLabel = countLabel(MusicMoodCollection.MORNING_COFFEE),
                selected = activeCollection == MusicMoodCollection.MORNING_COFFEE,
                onClick = { onSelectCollection(MusicMoodCollection.MORNING_COFFEE) },
                modifier = Modifier.weight(1f)
            )
            CuratedMoodCard(
                collection = MusicMoodCollection.GOLDEN_HOUR,
                countLabel = countLabel(MusicMoodCollection.GOLDEN_HOUR),
                selected = activeCollection == MusicMoodCollection.GOLDEN_HOUR,
                onClick = { onSelectCollection(MusicMoodCollection.GOLDEN_HOUR) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CuratedMoodCard(
                collection = MusicMoodCollection.CITY_NIGHTS,
                countLabel = countLabel(MusicMoodCollection.CITY_NIGHTS),
                selected = activeCollection == MusicMoodCollection.CITY_NIGHTS,
                onClick = { onSelectCollection(MusicMoodCollection.CITY_NIGHTS) },
                modifier = Modifier.weight(1f)
            )
            CuratedMoodCard(
                collection = MusicMoodCollection.WEEKEND_DRIVE,
                countLabel = countLabel(MusicMoodCollection.WEEKEND_DRIVE),
                selected = activeCollection == MusicMoodCollection.WEEKEND_DRIVE,
                onClick = { onSelectCollection(MusicMoodCollection.WEEKEND_DRIVE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CuratedMoodCard(
    collection: MusicMoodCollection,
    countLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicPickerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    val artRotation by animateFloatAsState(
        targetValue = if (selected || isPressed) 9f else 14f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "artRotation"
    )
    val artScale by animateFloatAsState(
        targetValue = if (selected || isPressed) 1.06f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "artScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.08f)
        } else {
            colors.secondarySurface
        },
        animationSpec = tween(180),
        label = "moodBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accent else Color.Transparent,
        animationSpec = tween(180),
        label = "moodBorder"
    )

    BoxWithConstraints(
        modifier = modifier
            .scale(scale)
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .semantics {
                this.selected = selected
                contentDescription = "${collection.title}, $countLabel"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val artSize = maxWidth / 3f

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 12.dp)
                .size(artSize)
                .scale(artScale)
                .rotate(artRotation)
                .clip(RoundedCornerShape(12.dp))
                .border(0.75.dp, colors.coverBorder, RoundedCornerShape(12.dp))
        ) {
            MoodEditorialIllustration(
                collection = collection,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = countLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) colors.accent else colors.textTertiary
            )
            Text(
                text = collection.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                lineHeight = 18.sp,
                color = colors.textPrimary,
                maxLines = 2,
                modifier = Modifier.padding(end = artSize * 0.72f)
            )
        }
    }
}

// ============================================================================
// 7. SONG LIST ROW, EMPTY STATE & FLOATING MINI PLAYER
// ============================================================================

@Composable
private fun SongRowItem(
    track: PickerMusicTrack,
    searchQuery: String,
    isPlaying: Boolean,
    appliedClip: SelectedMusicClip?,
    showDivider: Boolean,
    onRowClick: () -> Unit,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rowScale"
    )
    val isHighlighted = isPlaying || appliedClip != null
    val titleColor by animateColorAsState(
        targetValue = if (isHighlighted) colors.accent else colors.textPrimary,
        animationSpec = tween(180),
        label = "titleColor"
    )

    val highlightedTitle = remember(track.title, searchQuery, colors.accent) {
        highlightQueryMatch(track.title, searchQuery, colors.accent)
    }
    val highlightedArtist = remember(track.artist, searchQuery, colors.accent) {
        highlightQueryMatch(track.artist, searchQuery, colors.accent)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(rowScale)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onRowClick
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (isPlaying) "Pause ${track.title}" else "Preview ${track.title}"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onPlayClick
                    )
            ) {
                BeveledSongCover(
                    size = 48.dp,
                    cornerRadius = 12.dp,
                    coverUrl = track.coverUrl,
                    isActive = isHighlighted,
                    showEqualizer = isPlaying
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = highlightedTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.explicit) {
                        ExplicitBadge()
                    }
                    if (appliedClip != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = if (colors.isDark) 0.2f else 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${formatMusicTime(appliedClip.startSec)}–${formatMusicTime(appliedClip.startSec + appliedClip.durationSec)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accent,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (track.trending) {
                        TrendingArrowIcon(
                            tint = colors.chorusMarker,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = highlightedArtist,
                        fontSize = 13.sp,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(text = "•", fontSize = 12.sp, color = colors.textTertiary)
                    Text(
                        text = formatMusicTime(track.durationSec),
                        fontSize = 13.sp,
                        color = colors.textTertiary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBookmarkClick)
                    .semantics {
                        contentDescription = if (track.saved) "Remove ${track.title} from Saved" else "Save ${track.title}"
                    }
            ) {
                Bookmark3DIcon(
                    saved = track.saved,
                    accentColor = colors.accent,
                    outlineColor = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(colors.separator)
            )
        }
    }
}

@Composable
private fun ExplicitBadge() {
    val colors = LocalMusicPickerColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.textTertiary.copy(alpha = 0.2f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = "E",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textTertiary
        )
    }
}

@Composable
private fun EmptySongsState(
    isSavedEmpty: Boolean,
    errorMessage: String? = null,
    canResetFilters: Boolean,
    onResetFilters: () -> Unit,
    onRetryCatalog: (() -> Unit)? = null
) {
    val colors = LocalMusicPickerColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BeveledSongCover(
            size = 56.dp,
            cornerRadius = 16.dp,
            coverUrl = "",
            isActive = false,
            showEqualizer = false,
            iconSize = 28.dp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = when {
                errorMessage != null -> "Couldn't Load Music"
                isSavedEmpty -> "No Saved Music Yet"
                else -> "No Songs Found"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                errorMessage != null -> errorMessage
                isSavedEmpty -> "Bookmark songs while browsing to access them quickly here."
                else -> "No uploaded music matches your current filter or search."
            },
            fontSize = 14.sp,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (canResetFilters || errorMessage != null || (!isSavedEmpty && onRetryCatalog != null)) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.10f))
                    .clickable(role = Role.Button, onClick = {
                        if (canResetFilters) onResetFilters() else onRetryCatalog?.invoke()
                    })
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (canResetFilters) "Reset Filters" else "Refresh Library",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
private fun FloatingMiniPlayer(
    track: PickerMusicTrack,
    isPlaying: Boolean,
    progress: Float,
    onOpenTrimmer: () -> Unit,
    onTogglePlay: () -> Unit,
    onDismissPlayer: () -> Unit
) {
    val colors = LocalMusicPickerColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.elevatedSurface.copy(alpha = 0.96f),
        shadowElevation = if (colors.isDark) 14.dp else 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.75.dp, colors.coverBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTrimmer)
                ) {
                    BeveledSongCover(
                        size = 40.dp,
                        cornerRadius = 8.dp,
                        coverUrl = track.coverUrl,
                        isActive = true,
                        showEqualizer = isPlaying,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • Tap to trim",
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF3A3A3C) else colors.secondarySurface)
                        .clickable(role = Role.Button, onClick = onTogglePlay)
                        .semantics { contentDescription = if (isPlaying) "Pause preview" else "Play preview" }
                ) {
                    PlayPauseIcon(
                        isPlaying = isPlaying,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable(role = Role.Button, onClick = onOpenTrimmer)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Trim",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismissPlayer)
                        .semantics { contentDescription = "Stop preview" }
                ) {
                    CloseCrossIcon(
                        tint = colors.textTertiary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(colors.separator)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}

// ============================================================================
// 8. CLIP TRIMMER SHEET, WAVEFORM HANDLES & PRECISION NUMBER RULER
// ============================================================================

@Composable
private fun ClipTrimmerBottomSheet(
    track: PickerMusicTrack,
    clipStartSec: Int,
    clipDurationSec: Int,
    isPlaying: Boolean,
    playheadProgress: Float,
    showLyricsSticker: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onTogglePlay: () -> Unit,
    onLyricsToggle: (Boolean) -> Unit,
    onDurationPresetSelect: (Int) -> Unit,
    onClipWindowChange: (startSec: Int, durationSec: Int) -> Unit,
    onJumpToChorus: () -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var isDraggingAny by remember { mutableStateOf(false) }

    var sheetDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isSheetDragging by remember { mutableStateOf(false) }
    val animatedSheetOffsetPx by animateFloatAsState(
        targetValue = if (isSheetDragging) sheetDragOffsetPx else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sheetSwipeOffset"
    )
    val dismissThresholdPx = with(density) { 110.dp.toPx() }

    val maxStart = max(0, track.durationSec - clipDurationSec)
    val clampedStart = clipStartSec.coerceIn(0, maxStart)
    val currentLyric = remember(track, clampedStart) { "\"${lyricAtTime(track, clampedStart)}\"" }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = colors.surface,
        shadowElevation = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedSheetOffsetPx.roundToInt().coerceAtLeast(0)) }
            .border(
                width = 0.75.dp,
                color = colors.coverBorder,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isSheetDragging = true },
                            onDragCancel = {
                                isSheetDragging = false
                                sheetDragOffsetPx = 0f
                            },
                            onDragEnd = {
                                isSheetDragging = false
                                if (sheetDragOffsetPx > dismissThresholdPx) {
                                    onCancel()
                                }
                                sheetDragOffsetPx = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                sheetDragOffsetPx = (sheetDragOffsetPx + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.grabber)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.secondarySurface)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val effectivePresets = listOf(15, 30, 60)
                        effectivePresets.forEach { preset ->
                            val targetDuration = preset.coerceAtMost(track.durationSec)
                            val active = clipDurationSec == targetDuration &&
                                (preset == effectivePresets.first { it.coerceAtMost(track.durationSec) == clipDurationSec })
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) colors.activeThumb else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDurationPresetSelect(preset)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (targetDuration < preset && track.durationSec < preset) "${targetDuration}s" else "${preset}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) colors.textPrimary else colors.textTertiary
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable(role = Role.Button, onClick = onDone)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BeveledSongCover(
                    size = 56.dp,
                    cornerRadius = 12.dp,
                    coverUrl = track.coverUrl,
                    isActive = true,
                    showEqualizer = false,
                    iconSize = 26.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (track.explicit) ExplicitBadge()
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (track.album.isNotBlank()) "${track.artist} • ${track.album}" else track.artist,
                        fontSize = 14.sp,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.secondarySurface)
                        .clickable(role = Role.Button, onClick = onTogglePlay)
                        .semantics { contentDescription = if (isPlaying) "Pause preview" else "Play preview" }
                ) {
                    PlayPauseIcon(
                        isPlaying = isPlaying,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            WaveformTrimmerControl(
                track = track,
                clipStartSec = clampedStart,
                clipDurationSec = clipDurationSec,
                playheadProgress = playheadProgress,
                showDragTooltip = isDraggingAny,
                onDragStateChange = { isDraggingAny = it },
                onClipWindowChange = onClipWindowChange,
                onJumpToChorus = onJumpToChorus
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Drag handles or selection window to adjust clip",
                fontSize = 11.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepCircleButton(
    symbol: String,
    contentDesc: String,
    onClick: () -> Unit
) {
    val colors = LocalMusicPickerColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.secondarySurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = contentDesc }
    ) {
        Text(
            text = symbol,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun WaveformTrimmerControl(
    track: PickerMusicTrack,
    clipStartSec: Int,
    clipDurationSec: Int,
    playheadProgress: Float,
    showDragTooltip: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    onClipWindowChange: (Int, Int) -> Unit,
    onJumpToChorus: () -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val haptic = LocalHapticFeedback.current

    val minClipSec = min(5, track.durationSec)
    val maxClipSec = min(60, track.durationSec)

    val startRatio = (clipStartSec.toFloat() / track.durationSec.toFloat()).coerceIn(0f, 1f)
    val widthRatio = (clipDurationSec.toFloat() / track.durationSec.toFloat()).coerceIn(0f, 1f - startRatio)
    val chorusRatio = (track.chorusSec.toFloat() / track.durationSec.toFloat()).coerceIn(0.04f, 0.96f)
    val windowCenterRatio = (startRatio + widthRatio / 2f).coerceIn(0.18f, 0.82f)

    val latestTrack by rememberUpdatedState(track)
    val latestStart by rememberUpdatedState(clipStartSec)
    val latestDur by rememberUpdatedState(clipDurationSec)
    val latestOnChange by rememberUpdatedState(onClipWindowChange)
    val latestOnDragState by rememberUpdatedState(onDragStateChange)

    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var activeHandle by remember { mutableStateOf<String?>(null) }

    val leftHandleWidth by animateDpAsState(
        targetValue = if (activeHandle == "left") 14.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "leftHandleW"
    )
    val rightHandleWidth by animateDpAsState(
        targetValue = if (activeHandle == "right") 14.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rightHandleW"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val headerWidthDp = maxWidth

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMusicTime(clipStartSec),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textTertiary,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.10f))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${clipDurationSec}s clip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }

                Text(
                    text = formatMusicTime(clipStartSec + clipDurationSec),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textTertiary,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }

            val tooltipOffsetX = ((headerWidthDp * windowCenterRatio) - (headerWidthDp / 2f))
                .coerceIn(-(headerWidthDp * 0.28f), headerWidthDp * 0.28f)

            androidx.compose.animation.AnimatedVisibility(
                visible = showDragTooltip,
                enter = fadeIn(tween(130)) + scaleIn(initialScale = 0.92f, animationSpec = tween(130)),
                exit = fadeOut(tween(130)) + scaleOut(targetScale = 0.94f, animationSpec = tween(130)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = tooltipOffsetX, y = (-28).dp)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(colors.toastBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${formatMusicTime(clipStartSec)} – ${formatMusicTime(clipStartSec + clipDurationSec)} (${clipDurationSec}s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.toastText,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
        ) {
            val fullWidthDp = maxWidth
            val windowOffsetDp = fullWidthDp * startRatio
            val windowWidthDp = (fullWidthDp * widthRatio).coerceAtLeast(28.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.secondarySurface)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            val centerSec = (ratio * latestTrack.durationSec).roundToInt()
                            val maxStart = max(0, latestTrack.durationSec - latestDur)
                            val nextStart = (centerSec - latestDur / 2).coerceIn(0, maxStart)
                            latestOnChange(nextStart, latestDur)
                        }
                    }
                    .pointerInput(Unit) {
                        var initialStart = 0
                        var accumulatedPx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                initialStart = latestStart
                                accumulatedPx = 0f
                                latestOnDragState(true)
                            },
                            onDragEnd = { latestOnDragState(false) },
                            onDragCancel = { latestOnDragState(false) },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedPx += dragAmount
                                val deltaSec = ((accumulatedPx / trackWidthPx) * latestTrack.durationSec).roundToInt()
                                val maxStart = max(0, latestTrack.durationSec - latestDur)
                                val nextStart = (initialStart + deltaSec).coerceIn(0, maxStart)
                                if (nextStart != latestStart) {
                                    latestOnChange(nextStart, latestDur)
                                }
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    val count = track.waveform.size
                    val gapPx = 3.dp.toPx()
                    val barW = ((size.width - gapPx * (count - 1)) / count).coerceAtLeast(2f)
                    val centerY = size.height / 2f

                    track.waveform.forEachIndexed { idx, amp ->
                        val barPos = (idx + 0.5f) / count.toFloat()
                        val inSelection = barPos >= startRatio && barPos <= (startRatio + widthRatio)
                        val barH = (amp * size.height).coerceAtLeast(6f)
                        val leftX = idx * (barW + gapPx)
                        drawRoundRect(
                            color = if (inSelection) colors.accent else colors.waveformInactive,
                            topLeft = Offset(leftX, centerY - barH / 2f),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = fullWidthDp * chorusRatio - 12.dp, y = (-12).dp)
                    .size(24.dp)
                    .semantics { contentDescription = "Jump to Best Part" }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onJumpToChorus
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.chorusMarker)
                        .border(1.5.dp, colors.surface, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = windowOffsetDp)
                    .width(windowWidthDp)
                    .fillMaxHeight()
                    .padding(vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.13f))
                        .border(2.dp, colors.accent, RoundedCornerShape(10.dp))
                )

                val innerTravelDp = (windowWidthDp - 24.dp).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 12.dp + innerTravelDp * playheadProgress.coerceIn(0f, 1f))
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-12).dp)
                        .width(28.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            var initialStart = 0
                            var initialDur = 15
                            var accumulatedPx = 0f
                            var hitBoundary = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    initialStart = latestStart
                                    initialDur = latestDur
                                    accumulatedPx = 0f
                                    hitBoundary = false
                                    activeHandle = "left"
                                    latestOnDragState(true)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDragEnd = {
                                    activeHandle = null
                                    latestOnDragState(false)
                                },
                                onDragCancel = {
                                    activeHandle = null
                                    latestOnDragState(false)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedPx += dragAmount
                                    val deltaSec = ((accumulatedPx / trackWidthPx) * latestTrack.durationSec).roundToInt()
                                    val fixedEnd = initialStart + initialDur
                                    val minAllowedStart = max(0, fixedEnd - maxClipSec)
                                    val maxAllowedStart = max(0, fixedEnd - minClipSec)
                                    val rawStart = initialStart + deltaSec
                                    val newStart = rawStart.coerceIn(minAllowedStart, maxAllowedStart)
                                    val newDur = max(1, fixedEnd - newStart)
                                    val atLimit = rawStart < minAllowedStart || rawStart > maxAllowedStart
                                    if (atLimit && !hitBoundary) {
                                        hitBoundary = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (!atLimit) {
                                        hitBoundary = false
                                    }
                                    if (newStart != latestStart || newDur != latestDur) {
                                        latestOnChange(newStart, newDur)
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(leftHandleWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp))
                            .background(colors.accent)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f))
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp)
                        .width(28.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            var initialStart = 0
                            var initialDur = 15
                            var accumulatedPx = 0f
                            var hitBoundary = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    initialStart = latestStart
                                    initialDur = latestDur
                                    accumulatedPx = 0f
                                    hitBoundary = false
                                    activeHandle = "right"
                                    latestOnDragState(true)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDragEnd = {
                                    activeHandle = null
                                    latestOnDragState(false)
                                },
                                onDragCancel = {
                                    activeHandle = null
                                    latestOnDragState(false)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedPx += dragAmount
                                    val deltaSec = ((accumulatedPx / trackWidthPx) * latestTrack.durationSec).roundToInt()
                                    val maxAllowedDur = max(minClipSec, min(maxClipSec, latestTrack.durationSec - initialStart))
                                    val rawDur = initialDur + deltaSec
                                    val newDur = rawDur.coerceIn(minClipSec, maxAllowedDur)
                                    val atLimit = rawDur < minClipSec || rawDur > maxAllowedDur
                                    if (atLimit && !hitBoundary) {
                                        hitBoundary = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (!atLimit) {
                                        hitBoundary = false
                                    }
                                    if (newDur != latestDur) {
                                        latestOnChange(initialStart, newDur)
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(rightHandleWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp))
                            .background(colors.accent)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrecisionNumberRulerSlider(
    currentSec: Int,
    maxSec: Int,
    onSecChange: (Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val pxPerSec = with(density) { 14.dp.toPx() }
    val latestCurrent by rememberUpdatedState(currentSec)
    val latestMax by rememberUpdatedState(maxSec)
    val latestOnChange by rememberUpdatedState(onSecChange)
    val latestOnDragState by rememberUpdatedState(onDragStateChange)

    var isDragging by remember { mutableStateOf(false) }
    var dragVisualSec by remember { mutableFloatStateOf(currentSec.toFloat()) }

    val animatedSec by animateFloatAsState(
        targetValue = currentSec.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialSpringSec"
    )
    val displaySec = if (isDragging) dragVisualSec else animatedSec

    val labelStyle = remember(colors.textSecondary) {
        TextStyle(
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.secondarySurface)
            .semantics {
                contentDescription = "Start time ruler"
                stateDescription = formatMusicTime(currentSec)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val stepDelta = if (offset.x < centerX) -1 else 1
                    val next = (latestCurrent + stepDelta).coerceIn(0, latestMax)
                    if (next != latestCurrent) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestOnChange(next)
                    }
                }
            }
            .pointerInput(Unit) {
                var initialSec = 0f
                var accumulatedPx = 0f
                var lastTickedSec = -1
                detectHorizontalDragGestures(
                    onDragStart = {
                        initialSec = latestCurrent.toFloat()
                        dragVisualSec = initialSec
                        accumulatedPx = 0f
                        lastTickedSec = latestCurrent
                        isDragging = true
                        latestOnDragState(true)
                    },
                    onDragEnd = {
                        isDragging = false
                        latestOnDragState(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        latestOnDragState(false)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedPx += dragAmount
                        val continuousSec = (initialSec - accumulatedPx / pxPerSec)
                            .coerceIn(0f, latestMax.toFloat())
                        dragVisualSec = continuousSec
                        val snapped = continuousSec.roundToInt().coerceIn(0, latestMax)
                        if (snapped != latestCurrent) {
                            if (snapped % 5 == 0 && snapped != lastTickedSec) {
                                lastTickedSec = snapped
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            latestOnChange(snapped)
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.14f to Color.Black,
                            0.86f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            val centerX = size.width / 2f
            val bottomY = size.height - 6.dp.toPx()

            val visibleHalfRange = (centerX / pxPerSec).toInt() + 3
            val centerTick = displaySec.roundToInt()
            val startTick = max(0, centerTick - visibleHalfRange)
            val endTick = min(maxSec, centerTick + visibleHalfRange)

            for (sec in startTick..endTick) {
                val x = centerX + (sec - displaySec) * pxPerSec
                val distFromCenter = abs(sec - displaySec)
                val isNearNeedle = distFromCenter < 0.65f
                val isMajor = sec % 10 == 0
                val isMid = sec % 5 == 0 && !isMajor

                val tickHeight = when {
                    isMajor -> 14.dp.toPx()
                    isMid -> 10.dp.toPx()
                    else -> 6.dp.toPx()
                }
                val tickWidth = if (isMajor || isNearNeedle) 1.5.dp.toPx() else 1.dp.toPx()
                val tickColor = when {
                    isNearNeedle -> colors.accent
                    isMajor -> colors.textSecondary
                    isMid -> colors.textTertiary.copy(alpha = 0.7f)
                    else -> colors.waveformInactive
                }

                drawLine(
                    color = tickColor,
                    start = Offset(x, bottomY - tickHeight),
                    end = Offset(x, bottomY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val textLayout = textMeasurer.measure(formatMusicTime(sec), labelStyle)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            x = x - textLayout.size.width / 2f,
                            y = bottomY - tickHeight - textLayout.size.height - 3.dp.toPx()
                        )
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.accent))
            Box(modifier = Modifier.width(2.dp).weight(1f).clip(CircleShape).background(colors.accent))
        }
    }
}

@Composable
private fun AppleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalMusicPickerColors.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "switchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.switchActive else colors.switchInactive,
        animationSpec = tween(160),
        label = "switchTrack"
    )

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .semantics {
                contentDescription = "Show synced lyrics sticker"
                stateDescription = if (checked) "On" else "Off"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ============================================================================
// 9. BEVELED COVER (WITH ALBUM ART SUPPORT), EQUALIZER & 3D VECTOR ICONS
// ============================================================================

@Composable
private fun BeveledSongCover(
    size: Dp,
    cornerRadius: Dp,
    coverUrl: String,
    isActive: Boolean,
    showEqualizer: Boolean,
    iconSize: Dp = 24.dp
) {
    val colors = LocalMusicPickerColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (colors.isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(colors.coverTop, colors.coverBottom)))
            .border(0.75.dp, colors.coverBorder, RoundedCornerShape(cornerRadius))
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.coverSpecular)
            )
            MusicNote3DIcon(
                isActive = isActive,
                modifier = Modifier.size(iconSize)
            )
        }

        if (showEqualizer) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (coverUrl.isNotBlank()) Color.Black.copy(alpha = 0.42f)
                        else colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.10f)
                    )
            ) {
                AnimatedEqualizerBars(color = if (coverUrl.isNotBlank()) Color.White else colors.accent)
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 13f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 7f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(15.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(bar1.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(2.5.dp).height(bar2.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(2.5.dp).height(bar3.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun MusicNote3DIcon(isActive: Boolean, modifier: Modifier = Modifier) {
    val top = if (isActive) Color(0xFF5AC8FA) else Color(0xFFAEAEB2)
    val mid = if (isActive) Color(0xFF007AFF) else Color(0xFF7C7C82)
    val bot = if (isActive) Color(0xFF004999) else Color(0xFF48484E)

    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        drawOval(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = Offset(6.4f * sx, 20.1f * sy),
            size = Size(7.6f * sx, 1.8f * sy)
        )
        val bodyBrush = Brush.linearGradient(
            0.0f to top,
            0.52f to mid,
            1.0f to bot,
            start = Offset(3.5f * sx, 2f * sy),
            end = Offset(20f * sx, 22f * sy)
        )
        drawOval(brush = bodyBrush, topLeft = Offset(5.8f * sx, 13.0f * sy), size = Size(8.6f * sx, 7.6f * sy))
        val stemPath = Path().apply {
            moveTo(12.2f * sx, 4.1f * sy)
            lineTo(18.2f * sx, 2.7f * sy)
            cubicTo(18.7f * sx, 2.6f * sy, 19.1f * sx, 3.0f * sy, 19.1f * sx, 3.55f * sy)
            lineTo(19.1f * sx, 5.75f * sy)
            cubicTo(19.1f * sx, 6.2f * sy, 18.8f * sx, 6.55f * sy, 18.35f * sx, 6.65f * sy)
            lineTo(14.4f * sx, 7.55f * sy)
            lineTo(14.4f * sx, 16.8f * sy)
            lineTo(12.2f * sx, 16.8f * sy)
            close()
        }
        drawPath(path = stemPath, brush = bodyBrush)
        drawLine(Color.White.copy(alpha = 0.4f), Offset(12.8f * sx, 4.5f * sy), Offset(12.8f * sx, 13.0f * sy), 0.8f * sx, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.4f), Offset(13.2f * sx, 4.4f * sy), Offset(18.0f * sx, 3.3f * sy), 0.8f * sx, StrokeCap.Round)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(9.2f * sx, 15.6f * sy),
                radius = 2.4f * sx
            ),
            topLeft = Offset(7.2f * sx, 14.3f * sy),
            size = Size(3.6f * sx, 2.2f * sy)
        )
    }
}

@Composable
private fun Sun3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.22f
        val rayInner = size.minDimension * 0.34f
        val rayOuter = size.minDimension * 0.44f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE58F), Color(0xFFFFBE2E), Color(0xFFE08B00)),
                center = Offset(cx - r * 0.3f, cy - r * 0.3f),
                radius = r * 1.4f
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        for (deg in 0 until 360 step 45) {
            val rad = deg * PI / 180.0
            drawLine(
                color = Color(0xFFFFBE2E),
                start = Offset(cx + (cos(rad) * rayInner).toFloat(), cy + (sin(rad) * rayInner).toFloat()),
                end = Offset(cx + (cos(rad) * rayOuter).toFloat(), cy + (sin(rad) * rayOuter).toFloat()),
                strokeWidth = size.minDimension * 0.085f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun Moon3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(20.3f * sx, 14.4f * sy)
            cubicTo(19.0f * sx, 18.4f * sy, 15.2f * sx, 21.0f * sy, 11.1f * sx, 20.8f * sy)
            cubicTo(6.3f * sx, 20.6f * sy, 2.7f * sx, 16.5f * sy, 3.0f * sx, 11.7f * sy)
            cubicTo(3.2f * sx, 7.6f * sy, 6.2f * sx, 4.3f * sy, 9.9f * sx, 3.6f * sy)
            cubicTo(8.3f * sx, 7.1f * sy, 9.5f * sx, 11.4f * sy, 12.8f * sx, 13.5f * sy)
            cubicTo(15.2f * sx, 15.0f * sy, 18.0f * sx, 15.2f * sy, 20.3f * sx, 14.4f * sy)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                listOf(Color(0xFF7C8594), Color(0xFF484E5A), Color(0xFF22252C)),
                start = Offset(4f * sx, 4f * sy),
                end = Offset(20f * sx, 20f * sy)
            )
        )
    }
}

@Composable
private fun Search3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val rimBrush = Brush.linearGradient(listOf(Color(0xFFAEAEB2), Color(0xFF636366)), Offset.Zero, Offset(size.width, size.height))
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Color(0xFF8E8E93).copy(alpha = 0.18f)), Offset(8.5f * sx, 8.5f * sy), 7f * sx),
            radius = 6.5f * sx,
            center = Offset(10.5f * sx, 10.5f * sy)
        )
        drawCircle(brush = rimBrush, radius = 6.5f * sx, center = Offset(10.5f * sx, 10.5f * sy), style = Stroke(2.2f * sx))
        drawLine(brush = rimBrush, start = Offset(15.5f * sx, 15.5f * sy), end = Offset(20.0f * sx, 20.0f * sy), strokeWidth = 2.5f * sx, cap = StrokeCap.Round)
    }
}

@Composable
private fun Bookmark3DIcon(saved: Boolean, accentColor: Color, outlineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val ribbon = Path().apply {
            moveTo(6.5f * sx, 3.2f * sy)
            lineTo(17.5f * sx, 3.2f * sy)
            quadraticBezierTo(18.5f * sx, 3.2f * sy, 18.5f * sx, 4.4f * sy)
            lineTo(18.5f * sx, 20.4f * sy)
            lineTo(12.0f * sx, 16.7f * sy)
            lineTo(5.5f * sx, 20.4f * sy)
            lineTo(5.5f * sx, 4.4f * sy)
            quadraticBezierTo(5.5f * sx, 3.2f * sy, 6.5f * sx, 3.2f * sy)
            close()
        }
        if (saved) {
            drawPath(ribbon, Brush.linearGradient(listOf(Color(0xFF5AC8FA), accentColor, Color(0xFF0051A8)), Offset(5f * sx, 2f * sy), Offset(19f * sx, 21f * sy)))
            drawLine(Color.White.copy(alpha = 0.45f), Offset(7.2f * sx, 4.2f * sy), Offset(16.8f * sx, 4.2f * sy), 0.9f * sx, StrokeCap.Round)
        } else {
            drawPath(ribbon, outlineColor, style = Stroke(1.8f * sx, join = StrokeJoin.Round, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PlayPauseIcon(isPlaying: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        if (isPlaying) {
            drawRoundRect(tint, Offset(6f * sx, 5f * sy), Size(4.2f * sx, 14f * sy), CornerRadius(1.4f * sx, 1.4f * sy))
            drawRoundRect(tint, Offset(13.8f * sx, 5f * sy), Size(4.2f * sx, 14f * sy), CornerRadius(1.4f * sx, 1.4f * sy))
        } else {
            val triangle = Path().apply {
                moveTo(8.5f * sx, 5.2f * sy)
                lineTo(19.2f * sx, 12.0f * sy)
                lineTo(8.5f * sx, 18.8f * sy)
                close()
            }
            drawPath(triangle, tint)
        }
    }
}

@Composable
private fun TrendingArrowIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(3f * sx, 17f * sy)
            lineTo(9f * sx, 11f * sy)
            lineTo(13f * sx, 15f * sy)
            lineTo(21f * sx, 7f * sy)
            moveTo(14f * sx, 7f * sy)
            lineTo(21f * sx, 7f * sy)
            lineTo(21f * sx, 14f * sy)
        }
        drawPath(path, tint, style = Stroke(2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CloseCrossIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        drawLine(tint, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
        drawLine(tint, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
    }
}

@Composable
private fun MoodEditorialIllustration(collection: MusicMoodCollection, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 80f
        val sy = size.height / 80f
        when (collection) {
            MusicMoodCollection.MORNING_COFFEE -> {
                drawRect(Color(0xFFF4EBE1))
                drawCircle(Color(0xFFEBD8C3), radius = 24f * sx, center = Offset(58f * sx, 18f * sy))
                drawRect(Color(0xFFDEC8B0), Offset(0f, 56f * sy), Size(80f * sx, 24f * sy))
                drawRoundRect(Color(0xFFC49E7B), Offset(17f * sx, 55f * sy), Size(40f * sx, 4.5f * sy), CornerRadius(2.25f * sx))
                val mug = Path().apply {
                    moveTo(21f * sx, 33f * sy); lineTo(52f * sx, 33f * sy); lineTo(52f * sx, 45f * sy)
                    cubicTo(52f * sx, 51.6f * sy, 46.6f * sx, 57f * sy, 40f * sx, 57f * sy)
                    lineTo(33f * sx, 57f * sy); cubicTo(26.4f * sx, 57f * sy, 21f * sx, 51.6f * sy, 21f * sx, 45f * sy); close()
                }
                drawPath(mug, Color.White)
                drawOval(Color(0xFF7A4B2A), Offset(21f * sx, 29.8f * sy), Size(31f * sx, 6.4f * sy))
            }
            MusicMoodCollection.GOLDEN_HOUR -> {
                drawRect(Color(0xFFFBE3BE))
                drawRect(Color(0xFFF7C588), Offset(0f, 20f * sy), Size(80f * sx, 24f * sy))
                drawRect(Color(0xFFF09E6B), Offset(0f, 36f * sy), Size(80f * sx, 18f * sy))
                drawCircle(Color(0xFFFFF2D6), 15f * sx, Offset(40f * sx, 45f * sy))
                val ridge = Path().apply {
                    moveTo(0f, 54f * sy); cubicTo(14f * sx, 47f * sy, 28f * sx, 49f * sy, 42f * sx, 55f * sy)
                    cubicTo(55f * sx, 60f * sy, 68f * sx, 57f * sy, 80f * sx, 51f * sy); lineTo(80f * sx, 80f * sy); lineTo(0f, 80f * sy); close()
                }
                drawPath(ridge, Color(0xFFC96444))
            }
            MusicMoodCollection.CITY_NIGHTS -> {
                drawRect(Color(0xFF1C2336))
                drawCircle(Color(0xFFE8ECF4), 8.5f * sx, Offset(53f * sx, 20f * sy))
                drawCircle(Color(0xFF1C2336), 7.8f * sx, Offset(57f * sx, 17f * sy))
                drawRoundRect(Color(0xFF2A344E), Offset(8f * sx, 42f * sy), Size(16f * sx, 38f * sy), CornerRadius(1.5f * sx))
                drawRoundRect(Color(0xFF121726), Offset(22f * sx, 30f * sy), Size(22f * sx, 50f * sy), CornerRadius(1.5f * sx))
                drawRoundRect(Color(0xFFF2CF7E), Offset(27f * sx, 36f * sy), Size(4f * sx, 4f * sy), CornerRadius(0.8f * sx))
                drawRoundRect(Color(0xFFF2CF7E), Offset(35f * sx, 36f * sy), Size(4f * sx, 4f * sy), CornerRadius(0.8f * sx))
            }
            MusicMoodCollection.WEEKEND_DRIVE -> {
                drawRect(Color(0xFFDCEAF7))
                drawCircle(Color(0xFFFFF7E2), 8f * sx, Offset(24f * sx, 21f * sy))
                val hill = Path().apply {
                    moveTo(0f, 44f * sy); cubicTo(16f * sx, 34f * sy, 30f * sx, 34f * sy, 46f * sx, 44f * sy)
                    cubicTo(58f * sx, 51f * sy, 70f * sx, 49f * sy, 80f * sx, 42f * sy); lineTo(80f * sx, 80f * sy); lineTo(0f, 80f * sy); close()
                }
                drawPath(hill, Color(0xFF8FB3D6))
                val road = Path().apply { moveTo(40f * sx, 50f * sy); lineTo(16f * sx, 80f * sy); lineTo(64f * sx, 80f * sy); close() }
                drawPath(road, Color(0xFF2E3845))
                drawLine(Color.White, Offset(40f * sx, 54f * sy), Offset(40f * sx, 76f * sy), 2.2f * sx, StrokeCap.Round)
            }
        }
    }
}
