package com.chroma.studio.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.math.max
import kotlin.math.min

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
        MediaStore.Audio.Media.SIZE
    )
    val selection = "${MediaStore.Audio.Media.DURATION} >= 1500"
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "Local Audio"
                val durationMs = cursor.getLong(durationCol)
                val sizeBytes = cursor.getLong(sizeCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                audioList.add(
                    DeviceAudioTrack(
                        id = id,
                        title = title,
                        artist = if (artist.contains("unknown", ignoreCase = true)) "Device Audio" else artist,
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
 * Helper to query file name from content resolver
 */
private fun queryAudioFileName(context: Context, uri: Uri): String {
    var name = "Custom Audio Track"
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
    return name
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
 * Full-featured Device Music Browser & Trimmer Modal.
 * - Loads all music from device storage
 * - Interactive player for previewing tracks
 * - Trims specific segment (start / end range)
 * - One-click apply directly to the video editor timeline
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

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadTracks()
        }
    }

    // Custom file picker fallback
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

            val trackName = queryAudioFileName(context, pickedUri)
            val track = DeviceAudioTrack(
                id = System.currentTimeMillis(),
                title = trackName,
                artist = "Imported Audio",
                durationMs = 60000L, // Will be updated on media player prepare
                uri = pickedUri
            )
            audioTracks = listOf(track) + audioTracks.filter { it.uri != pickedUri }
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
            previewPlayer.playWhenReady = true
            previewPlayer.play()
            isPlaying = true
            isPreviewingTrimOnly = false
        } catch (e: Exception) {
            Log.e("DeviceMusicModal", "Error playing preview", e)
        }
    }

    // Polling player playback position
    LaunchedEffect(selectedTrack, isPlaying) {
        while (isPlaying && selectedTrack != null) {
            val posMs = previewPlayer.currentPosition
            val trackDur = previewPlayer.duration
            if (trackDur > 0 && selectedTrack?.durationMs == 60000L) {
                // Update imported track duration if probed by player
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

    // Filter tracks by search query
    val filteredTracks = remember(audioTracks, searchQuery) {
        if (searchQuery.isBlank()) audioTracks
        else audioTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF00C6FF)),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                )
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Music,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Device Music & Audio Trimmer",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = if (videoDurationSec > 0f) "Preview, trim segment, and fit to video (${formatAudioTime(videoDurationSec)})"
                                   else "Preview, trim, and apply soundtrack",
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
                            cursorBrush = SolidColor(colors.primary),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs, artists, audio...",
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
                        Icon(Lucide.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Browse",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ================================================================
            // SECTION 2: PERMISSION BANNER (IF NOT GRANTED)
            // ================================================================
            if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2563EB).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.35f)),
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
                                text = "Allow access to automatically index songs and audio files on your phone.",
                                fontSize = 11.5.sp,
                                color = colors.textMuted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(permissionString) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    .heightIn(min = 140.dp, max = if (selectedTrack != null) 210.dp else 340.dp)
            ) {
                if (isLoadingTracks) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
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
                                        if (isSelected) colors.primary.copy(alpha = 0.12f)
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
                                    // Animated Play / Music Icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) colors.primary
                                                else colors.glassBgHover
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Lucide.Volume2 else Lucide.Play,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else colors.textMain,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.primary else colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.formattedDuration}",
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
                                            .background(colors.primary),
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
            // SECTION 4: INTEGRATED PLAYER & INTERACTIVE AUDIO TRIMMER
            // ================================================================
            if (selectedTrack != null) {
                val track = selectedTrack!!
                val maxDuration = track.durationSec.coerceAtLeast(1f)

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (colors.isDark) Color(0xFF16161A) else Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, buttonGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Track title & playback controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Playing: ${formatAudioTime(currentPlaybackSec)} / ${formatAudioTime(maxDuration)}",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play / Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Waveform Progress / Scrub Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.glassBorder)
                        ) {
                            // Active playhead
                            val progressRatio = (currentPlaybackSec / maxDuration).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio)
                                    .background(colors.primary)
                            )

                            // Trim highlight window
                            val startRatio = (trimStartSec / maxDuration).coerceIn(0f, 1f)
                            val endRatio = (trimEndSec / maxDuration).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(endRatio)
                                    .padding(start = (startRatio * 320).dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.45f))
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Trimmer Title & Active Duration Tag
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

                        Spacer(Modifier.height(8.dp))

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
                                    thumbColor = Color(0xFF007AFF),
                                    activeTrackColor = Color(0xFF007AFF),
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

                        // Quick Trim Preset Chips
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
                                    color = Color(0xFF2563EB).copy(alpha = 0.16f),
                                    border = BorderStroke(0.5.dp, Color(0xFF2563EB).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Fit Video (${videoDurationSec.toInt()}s)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
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
                                    text = "First 15s",
                                    fontSize = 11.sp,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    trimStartSec = 0f
                                    trimEndSec = min(maxDuration, 30f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.glassBgHover
                            ) {
                                Text(
                                    text = "First 30s",
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
                                    text = "Middle 30s",
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

                        // Preview Trim Button & One-Click Apply Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audition / Preview Trimmed Loop
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
                                        tint = colors.primary,
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
                                                colors = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)),
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
