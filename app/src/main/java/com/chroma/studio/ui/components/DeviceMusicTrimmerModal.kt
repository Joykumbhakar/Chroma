package com.chroma.studio.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Audio Track model fetched from Android device MediaStore or picked via file picker.
 */
data class DeviceAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    val sizeBytes: Long = 0L
) {
    val durationSec: Float get() = (durationMs / 1000f).coerceAtLeast(0.1f)
    val formattedDuration: String get() {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }
}

/**
 * Queries device storage for all music and audio files via MediaStore.
 */
fun queryDeviceAudioFiles(context: Context): List<DeviceAudioTrack> {
    val audioList = mutableListOf<DeviceAudioTrack>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DISPLAY_NAME
    )
    val selection = "(${MediaStore.Audio.Media.DURATION} >= 500 OR ${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} = 0) AND (${MediaStore.Audio.Media.SIZE} > 0 OR ${MediaStore.Audio.Media.SIZE} IS NULL)"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var title = if (!cursor.isNull(titleCol)) cursor.getString(titleCol) else null
                val displayName = if (!cursor.isNull(nameCol)) cursor.getString(nameCol) else null
                if (title.isNullOrBlank() || title.contains("unknown", ignoreCase = true)) {
                    title = displayName?.substringBeforeLast('.') ?: "Audio Track $id"
                }
                val rawArtist = if (!cursor.isNull(artistCol)) cursor.getString(artistCol) else null
                val artist = if (rawArtist.isNullOrBlank() || rawArtist.contains("unknown", ignoreCase = true)) "Device Audio" else rawArtist
                val durationMs = if (!cursor.isNull(durationCol)) cursor.getLong(durationCol) else 0L
                val sizeBytes = if (!cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                audioList.add(
                    DeviceAudioTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        durationMs = durationMs,
                        uri = contentUri,
                        sizeBytes = sizeBytes
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("DeviceMusicLoader", "Failed to query media store audio", e)
    }
    return audioList
}

/**
 * Helper to query file name and duration from content resolver and MediaMetadataRetriever
 */
fun queryAudioTrackInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "Custom Audio Track"
    var durationMs = 0L
    try {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (idx != -1) {
                    name = cursor.getString(idx) ?: name
                }
            }
        }
    } catch (_: Throwable) {}

    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        if (!extractedTitle.isNullOrBlank()) {
            name = extractedTitle
        }
        val extractedDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        if (extractedDur != null && extractedDur > 0L) {
            durationMs = extractedDur
        }
        retriever.release()
    } catch (_: Throwable) {}

    return Pair(name, if (durationMs > 0L) durationMs else 30000L)
}

/**
 * Format seconds into mm:ss or mm:ss.s
 */
fun formatAudioTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    val tenths = ((seconds - totalSec) * 10).toInt().coerceIn(0, 9)
    return String.format(Locale.US, "%02d:%02d.%d", min, sec, tenths)
}

/**
 * Generates a deterministic pseudo-waveform for an audio track based on its id/title.
 */
fun generateTrackWaveform(trackId: Long, barCount: Int = 48): List<Float> {
    val rng = Random(trackId)
    return List(barCount) {
        0.25f + rng.nextFloat() * 0.75f
    }
}

/**
 * Animated Vinyl Disc / Turntable Record Graphic.
 * Rotates smoothly when playing and glides to a stop on pause.
 * Features realistic circular micro-grooves, specular light sheen, and center label.
 */
@Composable
fun AnimatedVinylDiscGraphic(
    isPlaying: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        var lastTime = withFrameNanos { it }
        while (isPlaying) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                rotationAngle = (rotationAngle + dt * 100f) % 360f // Smooth 16 RPM rotation
            }
        }
    }

    val infiniteGlow = rememberInfiniteTransition(label = "vinylGlow")
    val glowAlpha by infiniteGlow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier.size(108.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient breathing aura glow behind vinyl
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0381FE).copy(alpha = glowAlpha),
                                Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Rotating Vinyl Record Canvas
        Canvas(
            modifier = Modifier
                .size(94.dp)
                .rotate(rotationAngle)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer Vinyl Disc body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF26262D), Color(0xFF141418), Color(0xFF0A0A0D)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer rim ring
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius - 1f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Concentric Vinyl Micro-Grooves
            val grooveRadii = listOf(0.88f, 0.80f, 0.72f, 0.64f, 0.56f, 0.48f)
            for (factor in grooveRadii) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.055f),
                    radius = radius * factor,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }

            // Light glint reflection sheen across vinyl
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.09f)
                    ),
                    center = center
                ),
                radius = radius * 0.92f,
                center = center
            )

            // Center Label Badge
            val labelRadius = radius * 0.36f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0381FE), Color(0xFF00C6FF), Color(0xFF8B5CF6)),
                    start = Offset(center.x - labelRadius, center.y - labelRadius),
                    end = Offset(center.x + labelRadius, center.y + labelRadius)
                ),
                radius = labelRadius,
                center = center
            )

            // Label silver border
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = labelRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Center Spindle Hole
            drawCircle(
                color = Color(0xFF050507),
                radius = labelRadius * 0.28f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = labelRadius * 0.28f,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )
        }
    }
}

/**
 * Dynamic Multi-Frequency Real-Time Audio Equalizer.
 * 28 reactive frequency bars bouncing smoothly with sound waves when playing.
 */
@Composable
fun AnimatedAudioEqualizer(
    isPlaying: Boolean,
    barCount: Int = 28,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eqPhase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val freq = 1.3f + (i % 6) * 0.45f
            val phase = (i * 0.32f)
            val rawFactor = (sin((time * freq + phase).toDouble()).toFloat() * 0.5f + 0.5f)
            val targetHeight = if (isPlaying) (0.22f + rawFactor * 0.78f).coerceIn(0.15f, 1f) else 0.15f

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "barHeight_$i"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFF00E5FF),
                                Color(0xFF0381FE)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Interactive Audio Waveform Timeline & Trimmer.
 * Features dual-color trim highlighting, live playhead line, and touch-to-seek scrubbing.
 */
@Composable
fun InteractiveAudioWaveform(
    waveformBars: List<Float>,
    currentPlaybackSec: Float,
    maxDuration: Float,
    trimStartSec: Float,
    trimEndSec: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val startRatio = (trimStartSec / maxDuration).coerceIn(0f, 1f)
    val endRatio = (trimEndSec / maxDuration).coerceIn(0f, 1f)
    val playRatio = (currentPlaybackSec / maxDuration).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .pointerInput(maxDuration) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(ratio * maxDuration)
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Waveform bars
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            waveformBars.forEachIndexed { index, amplitude ->
                val barProgress = index.toFloat() / (waveformBars.size - 1).coerceAtLeast(1)
                val inTrimWindow = barProgress in startRatio..endRatio

                val barColor = if (inTrimWindow) {
                    if (barProgress <= playRatio) Color(0xFF10B981) else Color(0xFF0381FE)
                } else {
                    Color.White.copy(alpha = 0.20f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                        .fillMaxHeight(amplitude.coerceIn(0.18f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }

        // Live Playhead indicator line with glow
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(playRatio.coerceIn(0.01f, 1f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(6.dp, CircleShape, spotColor = Color(0xFF0381FE))
            )
        }
    }
}

/**
 * 4-Bar Mini Jumping Equalizer for track list items
 */
@Composable
fun MiniPlayingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniEq")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniEqPhase"
    )

    Row(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0..3) {
            val freq = 1.6f + i * 0.7f
            val phase = i * 0.75f
            val factor = if (isPlaying) (sin((time * freq + phase).toDouble()).toFloat() * 0.45f + 0.55f) else 0.25f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(factor.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFF0381FE))
            )
        }
    }
}

/**
 * Enhanced, Professional Device Music Browser & Audio Trimmer Modal.
 * - Interactive Turntable Vinyl Record Graphic with smooth continuous spin.
 * - Dynamic 28-bar Real-Time Equalizer spectrum visualizer.
 * - Interactive multi-segment waveform trimmer with direct touch-to-seek.
 * - Playback rate control (0.75x, 1.0x, 1.25x, 1.5x) and mute toggle.
 * - Category length tabs (All, Short <30s, Standard 30s-2m, Long >2m).
 * - Instant search filter and file picker fallback.
 * - Loop preview with live pulsating indicator and one-click apply CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMusicTrimmerModal(
    videoDurationSec: Float = 30f,
    onAudioSelected: (uri: Uri, name: String, durationSec: Float, trimStartSec: Float, trimEndSec: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()

    // Permissions state
    val permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Audio list & search query
    var audioTracks by remember { mutableStateOf<List<DeviceAudioTrack>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("All") }
    var isLoadingTracks by remember { mutableStateOf(false) }

    fun loadTracks() {
        isLoadingTracks = true
        scope.launch {
            audioTracks = queryDeviceAudioFiles(context)
            isLoadingTracks = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            loadTracks()
        }
    }

    // Automatic lifecycle observer: re-check permission when app regains focus from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED
                if (granted != hasPermission) {
                    hasPermission = granted
                    if (granted) {
                        loadTracks()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadTracks()
        }
    }

    // Preview Player State
    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var selectedTrack by remember { mutableStateOf<DeviceAudioTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackSec by remember { mutableFloatStateOf(0f) }
    var isPreviewingTrimOnly by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    // Trimming State
    var trimStartSec by remember { mutableFloatStateOf(0f) }
    var trimEndSec by remember { mutableFloatStateOf(30f) }

    // Update trim bounds when selected track changes
    fun selectTrack(track: DeviceAudioTrack) {
        selectedTrack = track
        trimStartSec = 0f
        val defaultTrimLength = if (videoDurationSec > 0f) min(videoDurationSec, track.durationSec) else min(30f, track.durationSec)
        trimEndSec = defaultTrimLength.coerceAtLeast(1f)

        try {
            previewPlayer.stop()
            previewPlayer.setMediaItem(MediaItem.fromUri(track.uri))
            previewPlayer.prepare()
            previewPlayer.seekTo(0L)
            previewPlayer.setPlaybackSpeed(currentSpeed)
            previewPlayer.volume = if (isMuted) 0f else 1f
            previewPlayer.playWhenReady = true
            previewPlayer.play()
            isPlaying = true
            isPreviewingTrimOnly = false
        } catch (e: Exception) {
            Log.e("DeviceMusicModal", "Error playing preview", e)
        }
    }

    // Custom file picker fallback with real metadata & immediate playback
    val customFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri ->
        if (pickedUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}

            val (trackName, trackDuration) = queryAudioTrackInfo(context, pickedUri)
            val track = DeviceAudioTrack(
                id = System.currentTimeMillis(),
                title = trackName,
                artist = "Imported Audio",
                durationMs = trackDuration,
                uri = pickedUri
            )
            audioTracks = listOf(track) + audioTracks.filter { it.uri != pickedUri }
            selectTrack(track)
        }
    }

    // Polling player playback position
    LaunchedEffect(selectedTrack, isPlaying) {
        while (isPlaying && selectedTrack != null) {
            val posMs = previewPlayer.currentPosition
            val trackDur = previewPlayer.duration
            if (trackDur > 0 && selectedTrack?.durationMs == 60000L) {
                selectedTrack = selectedTrack?.copy(durationMs = trackDur)
                if (trimEndSec == 30f) {
                    trimEndSec = min(trackDur / 1000f, if (videoDurationSec > 0f) videoDurationSec else 30f)
                }
            }
            currentPlaybackSec = posMs / 1000f

            // Loop back if previewing trimmed segment
            if (isPreviewingTrimOnly) {
                val endTargetSec = trimEndSec.coerceAtLeast(trimStartSec + 0.5f)
                if (currentPlaybackSec >= endTargetSec || previewPlayer.playbackState == Player.STATE_ENDED) {
                    previewPlayer.seekTo((trimStartSec * 1000f).toLong())
                    previewPlayer.play()
                }
            } else if (previewPlayer.playbackState == Player.STATE_ENDED) {
                isPlaying = false
                currentPlaybackSec = 0f
            }
            delay(50L)
        }
    }

    // Clean up player on dismiss
    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer.stop()
                previewPlayer.release()
            } catch (_: Throwable) {}
        }
    }

    // Filter tracks by category and search query
    val filteredTracks = remember(audioTracks, searchQuery, selectedCategoryTab) {
        var list = audioTracks
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        when (selectedCategoryTab) {
            "Short (<30s)" -> list.filter { it.durationSec <= 30f }
            "Standard (30s-2m)" -> list.filter { it.durationSec in 30f..120f }
            "Long (>2m)" -> list.filter { it.durationSec > 120f }
            else -> list
        }
    }

    val buttonGlassBorder = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.65f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.textMuted.copy(alpha = 0.35f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0381FE), Color(0xFF00E5FF)),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                )
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Music,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Music Player & Studio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = if (videoDurationSec > 0f) "Preview, trim segment, and fit to video (${formatAudioTime(videoDurationSec)})"
                                   else "Browse tracks, audition loop, and apply soundtrack",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.glassBgHover)
                ) {
                    Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // ================================================================
            // SECTION 1: SEARCH & BROWSE BAR
            // ================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.glassBg,
                    border = BorderStroke(1.dp, colors.glassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF0381FE)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search tracks, artists, audio...",
                                        color = colors.textMuted,
                                        fontSize = 13.5.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // File Picker fallback button
                Surface(
                    onClick = { customFilePickerLauncher.launch("audio/*") },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.glassBg,
                    border = BorderStroke(1.dp, buttonGlassBorder),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.Folder, contentDescription = null, tint = Color(0xFF0381FE), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Browse",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Category Length Tabs
            val categoryTabs = listOf("All", "Short (<30s)", "Standard (30s-2m)", "Long (>2m)")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categoryTabs) { tab ->
                    val isSelected = selectedCategoryTab == tab
                    Surface(
                        onClick = { selectedCategoryTab = tab },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF0381FE) else colors.glassBgHover,
                        border = BorderStroke(0.5.dp, if (isSelected) Color(0xFF0381FE) else colors.glassBorder)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ================================================================
            // SECTION 2: PERMISSION BANNER (IF NOT GRANTED)
            // ================================================================
            if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0381FE).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF0381FE).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device Storage Permission Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Allow access to index songs and soundtracks from your phone.",
                                fontSize = 11.5.sp,
                                color = colors.textMuted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { permissionLauncher.launch(permissionString) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0381FE)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ================================================================
            // SECTION 3: MUSIC LIST (DEVICE TRACKS)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.glassBg,
                border = BorderStroke(1.dp, colors.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp, max = if (selectedTrack != null) 170.dp else 320.dp)
            ) {
                if (isLoadingTracks) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0381FE), modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                } else if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Lucide.Music, null, tint = colors.textMuted, modifier = Modifier.size(32.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No songs match \"$searchQuery\""
                                       else if (!hasPermission) "Grant permission to view device songs, or tap 'Browse'"
                                       else "No audio files detected in device storage",
                                fontSize = 12.5.sp,
                                color = colors.textMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isSelected = selectedTrack?.id == track.id
                            val isThisPlaying = isSelected && isPlaying

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectTrack(track) }
                                    .background(
                                        if (isSelected) Color(0xFF0381FE).copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Animated Play / Mini-Equalizer Icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFF0381FE)
                                                else colors.glassBgHover
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isThisPlaying) {
                                            MiniPlayingEqualizer(isPlaying = true)
                                        } else {
                                            Icon(
                                                imageVector = if (isSelected) Lucide.Play else Lucide.Music,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else colors.textMain,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0381FE) else colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} · ${track.formattedDuration}",
                                            fontSize = 11.5.sp,
                                            color = colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0381FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }

                            if (track != filteredTracks.last()) {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 58.dp),
                                    thickness = 0.5.dp,
                                    color = colors.glassBorder.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // ================================================================
            // SECTION 4: INTEGRATED ENHANCED MUSIC PLAYER & TRIMMER
            // ================================================================
            if (selectedTrack != null) {
                val track = selectedTrack!!
                val maxDuration = track.durationSec.coerceAtLeast(1f)
                val trackWaveform = remember(track.id) { generateTrackWaveform(track.id) }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (colors.isDark) Color(0xFF16161B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, buttonGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Top Deck: Animated Vinyl Disc + Song Details + Frequency Equalizer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Animated Rotating Vinyl Record Graphic
                            AnimatedVinylDiscGraphic(
                                isPlaying = isPlaying,
                                title = track.title
                            )

                            // Title, Time, Frequency Equalizer, and Quick Play/Pause
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color(0xFF10B981) else colors.textMuted)
                                    )
                                    Text(
                                        text = track.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = track.artist,
                                    fontSize = 12.sp,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(4.dp))

                                // Real-Time Frequency Waveform Visualizer
                                AnimatedAudioEqualizer(
                                    isPlaying = isPlaying,
                                    barCount = 20,
                                    modifier = Modifier.height(20.dp)
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "${formatAudioTime(currentPlaybackSec)} / ${formatAudioTime(maxDuration)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0381FE)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Controls Deck: Rewind 5s, Play/Pause, Forward 5s, Speed Selector, Mute
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Transport buttons: -5s, Play/Pause, +5s
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Skip Back 5s
                                IconButton(
                                    onClick = {
                                        val newPos = max(0f, currentPlaybackSec - 5f)
                                        previewPlayer.seekTo((newPos * 1000f).toLong())
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.RotateCcw,
                                        contentDescription = "Back 5s",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Main Play / Pause Capsule
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF0381FE), Color(0xFF00C6FF))
                                            )
                                        )
                                        .clickable {
                                            if (isPlaying) {
                                                previewPlayer.pause()
                                                isPlaying = false
                                            } else {
                                                previewPlayer.play()
                                                isPlaying = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Skip Forward 5s
                                IconButton(
                                    onClick = {
                                        val newPos = min(maxDuration, currentPlaybackSec + 5f)
                                        previewPlayer.seekTo((newPos * 1000f).toLong())
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.RotateCw,
                                        contentDescription = "Forward 5s",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Speed Chips + Mute Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                                speeds.forEach { spd ->
                                    val isCurrentSpd = currentSpeed == spd
                                    Surface(
                                        onClick = {
                                            currentSpeed = spd
                                            previewPlayer.setPlaybackSpeed(spd)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrentSpd) Color(0xFF0381FE) else colors.glassBgHover
                                    ) {
                                        Text(
                                            text = "${spd}x",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isCurrentSpd) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrentSpd) Color.White else colors.textMuted,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        previewPlayer.volume = if (isMuted) 0f else 1f
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                                        contentDescription = "Mute Toggle",
                                        tint = if (isMuted) Color(0xFFEF4444) else colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Interactive Multi-Segment Waveform with Touch-to-Seek
                        InteractiveAudioWaveform(
                            waveformBars = trackWaveform,
                            currentPlaybackSec = currentPlaybackSec,
                            maxDuration = maxDuration,
                            trimStartSec = trimStartSec,
                            trimEndSec = trimEndSec,
                            onSeek = { seekSec ->
                                previewPlayer.seekTo((seekSec * 1000f).toLong())
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        // Trimmer Header & Active Selection Duration Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Scissors,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Trim Audio Segment",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }

                            val selectedDuration = (trimEndSec - trimStartSec).coerceAtLeast(0.1f)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.16f),
                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${formatAudioTime(trimStartSec)} - ${formatAudioTime(trimEndSec)} (${String.format(Locale.US, "%.1fs", selectedDuration)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Start Time Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Start",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                                modifier = Modifier.width(36.dp)
                            )
                            Slider(
                                value = trimStartSec,
                                onValueChange = { newStart ->
                                    trimStartSec = min(newStart, trimEndSec - 0.5f)
                                    previewPlayer.seekTo((trimStartSec * 1000f).toLong())
                                },
                                valueRange = 0f..max(1f, maxDuration - 0.5f),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF10B981),
                                    inactiveTrackColor = colors.glassBorder
                                )
                            )
                            Text(
                                text = formatAudioTime(trimStartSec),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                modifier = Modifier.width(48.dp)
                            )
                        }

                        // End Time Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "End",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                                modifier = Modifier.width(36.dp)
                            )
                            Slider(
                                value = trimEndSec,
                                onValueChange = { newEnd ->
                                    trimEndSec = max(newEnd, trimStartSec + 0.5f)
                                },
                                valueRange = 0.5f..maxDuration,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF0381FE),
                                    activeTrackColor = Color(0xFF0381FE),
                                    inactiveTrackColor = colors.glassBorder
                                )
                            )
                            Text(
                                text = formatAudioTime(trimEndSec),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                modifier = Modifier.width(48.dp)
                            )
                        }

                        // Smart Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (videoDurationSec > 0f) {
                                Surface(
                                    onClick = {
                                        trimEndSec = min(maxDuration, trimStartSec + videoDurationSec)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0381FE).copy(alpha = 0.16f),
                                    border = BorderStroke(0.5.dp, Color(0xFF0381FE).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Fit Video (${videoDurationSec.toInt()}s)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0381FE),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    trimStartSec = 0f
                                    trimEndSec = min(maxDuration, 15f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.glassBgHover
                            ) {
                                Text(
                                    text = "Intro 15s",
                                    fontSize = 11.sp,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    val mid = maxDuration / 2f
                                    trimStartSec = max(0f, mid - 15f)
                                    trimEndSec = min(maxDuration, mid + 15f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.glassBgHover
                            ) {
                                Text(
                                    text = "Hook 30s",
                                    fontSize = 11.sp,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    trimStartSec = 0f
                                    trimEndSec = maxDuration
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.glassBgHover
                            ) {
                                Text(
                                    text = "Full Track",
                                    fontSize = 11.sp,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Preview Trim Loop Button & One-Click Apply Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audition Trimmed Segment Loop
                            Surface(
                                onClick = {
                                    isPreviewingTrimOnly = true
                                    previewPlayer.seekTo((trimStartSec * 1000f).toLong())
                                    previewPlayer.play()
                                    isPlaying = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.glassBgHover,
                                border = BorderStroke(1.dp, buttonGlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewingTrimOnly && isPlaying) Lucide.Pause else Lucide.Play,
                                        contentDescription = null,
                                        tint = Color(0xFF0381FE),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isPreviewingTrimOnly && isPlaying) "Looping Trim..." else "Preview Trim",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    )
                                }
                            }

                            // One-Click Use in Video Editor CTA
                            Surface(
                                onClick = {
                                    try {
                                        previewPlayer.stop()
                                    } catch (_: Throwable) {}
                                    onAudioSelected(
                                        track.uri,
                                        track.title,
                                        track.durationSec,
                                        trimStartSec,
                                        trimEndSec
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, buttonGlassBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF0381FE), Color(0xFF10B981)),
                                                start = Offset.Zero,
                                                end = Offset.Infinite
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Use in Video Editor",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
}
