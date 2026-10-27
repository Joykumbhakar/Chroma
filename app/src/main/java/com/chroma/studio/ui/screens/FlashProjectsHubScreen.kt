package com.chroma.studio.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.DraftPostRepository
import com.chroma.studio.data.FlashRepository
import com.chroma.studio.model.CreatorPostDraft
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Filter modes for the 3-column Flash Projects & Drafts Hub
 */
enum class FlashHubFilter(val label: String) {
    ALL("All"),
    DRAFTS("Draft"),
    SAVED("Saved")
}

/**
 * Unified model for rendering cards in the 3*... grid layout
 */
sealed class FlashHubItem {
    abstract val id: String
    abstract val title: String
    abstract val timestamp: Long
    abstract val durationSec: Float
    abstract val previewUriOrUrl: String?
    abstract val resolution: String      // e.g. "1080p • 9:16"
    abstract val clipCount: Int          // e.g. 1 or 3
    abstract val isDraft: Boolean

    val formattedDuration: String get() {
        val totalSec = durationSec.toInt().coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)
    }

    val formattedDate: String get() {
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val pattern = if (now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)) "MMM d" else "MMM d, yyyy"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    data class Draft(
        val draft: CreatorPostDraft,
        override val id: String = draft.id,
        override val title: String = draft.title.ifBlank { "Untitled Project" },
        override val timestamp: Long = draft.timestamp,
        override val durationSec: Float = ((draft.flashTrimEndMs - draft.flashTrimStartMs).coerceAtLeast(0L) / 1000f),
        override val previewUriOrUrl: String? = draft.customCoverUri ?: draft.videoUri ?: draft.imageUris.firstOrNull(),
        override val resolution: String = if (draft.aspectRatio.isNotBlank()) "1080p • ${draft.aspectRatio}" else "1080p • 9:16",
        override val clipCount: Int = ((if (draft.videoUri != null) 1 else 0) + draft.imageUris.size).coerceAtLeast(1),
        override val isDraft: Boolean = true
    ) : FlashHubItem()

    data class Saved(
        val video: FlashVideo,
        override val id: String = video.id,
        override val title: String = video.caption.ifBlank { "Saved Flash Video" },
        override val timestamp: Long = video.timestamp,
        override val durationSec: Float = (video.duration / 1000f),
        override val previewUriOrUrl: String? = video.thumbnailUrl.ifBlank { video.videoUrl },
        override val resolution: String = "1080p • 9:16",
        override val clipCount: Int = 1,
        override val isDraft: Boolean = false
    ) : FlashHubItem()

    data class LocalExported(
        override val id: String,
        override val title: String,
        override val timestamp: Long,
        override val durationSec: Float,
        override val previewUriOrUrl: String?,
        val fileUri: Uri,
        override val resolution: String = "1080p • 9:16",
        override val clipCount: Int = 1,
        override val isDraft: Boolean = false
    ) : FlashHubItem()
}

/**
 * Screen opened when clicking + > Flash in the Community tab.
 * Renders a 3-column grid layout where creators see their Drafted and Saved Flash videos,
 * with a prominent "Import" button to add multiple videos/photos to create or edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashProjectsHubScreen(
    onBack: () -> Unit,
    onImportMedia: (List<Uri>) -> Unit,
    onOpenDraft: (CreatorPostDraft) -> Unit,
    onOpenSavedVideo: (Uri) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val coroutineScope = rememberCoroutineScope()
    val draftRepo = remember { DraftPostRepository(context) }
    val drafts by draftRepo.draftsFlow.collectAsState()
    val videoDecoderFactory = remember { VideoFrameDecoder.Factory() }

    var selectedFilter by remember { mutableStateOf(FlashHubFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<FlashHubItem?>(null) }
    var itemToRename by remember { mutableStateOf<FlashHubItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var userFlashes by remember { mutableStateOf<List<FlashVideo>>(emptyList()) }
    var localExportedVideos by remember { mutableStateOf<List<FlashHubItem.LocalExported>>(emptyList()) }
    var isLoadingSaved by remember { mutableStateOf(true) }

    // Multi-media picker: allows selecting multiple videos and photos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            SoundManager.playTap(context)
            ToastManager.showToast("${uris.size} media clip(s) selected")
            onImportMedia(uris)
        }
    }

    // Load saved / published flash videos from Firestore and local cache
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (!currentUid.isNullOrBlank()) {
                    val repo = FlashRepository()
                    val fetched = repo.getFlashesByUser(currentUid)
                    withContext(Dispatchers.Main) {
                        userFlashes = fetched
                    }
                }
            } catch (_: Exception) {}

            // Inspect local cache directory for any exported videos
            try {
                val cacheDir = context.cacheDir
                val exportFiles = cacheDir.listFiles { file ->
                    file.isFile && file.name.startsWith("pro_export_") && file.name.endsWith(".mp4")
                }?.sortedByDescending { it.lastModified() } ?: emptyList()

                val localItems = exportFiles.map { file ->
                    val uri = Uri.fromFile(file)
                    var durSec = 15f
                    var resStr = "1080p • 9:16"
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        if (ms != null && ms > 0L) durSec = ms / 1000f
                        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                        if (w != null && h != null && w > 0 && h > 0) {
                            val aspect = if (w > h) "16:9" else if (w == h) "1:1" else "9:16"
                            val quality = if (w >= 2160 || h >= 2160) "4K" else if (w >= 1080 || h >= 1080) "1080p" else "720p"
                            resStr = "$quality • $aspect"
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { retriever.release() } catch (_: Throwable) {}
                    }

                    FlashHubItem.LocalExported(
                        id = file.name,
                        title = file.nameWithoutExtension.replace("pro_export_", "Export "),
                        timestamp = file.lastModified(),
                        durationSec = durSec,
                        previewUriOrUrl = uri.toString(),
                        fileUri = uri,
                        resolution = resStr,
                        clipCount = 1
                    )
                }
                withContext(Dispatchers.Main) {
                    localExportedVideos = localItems
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                isLoadingSaved = false
            }
        }
    }

    // Filtered flash drafts (where mode is flash or has video)
    val flashDraftItems = remember(drafts) {
        drafts.filter { it.mode.equals("flash", ignoreCase = true) || it.videoUri != null }
            .map { FlashHubItem.Draft(it) }
    }

    val savedItems = remember(userFlashes, localExportedVideos) {
        val remoteItems = userFlashes.map { FlashHubItem.Saved(it) }
        remoteItems + localExportedVideos
    }

    val displayItems = remember(selectedFilter, flashDraftItems, savedItems) {
        when (selectedFilter) {
            FlashHubFilter.ALL -> (flashDraftItems + savedItems).sortedByDescending { it.timestamp }
            FlashHubFilter.DRAFTS -> flashDraftItems.sortedByDescending { it.timestamp }
            FlashHubFilter.SAVED -> savedItems.sortedByDescending { it.timestamp }
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ══════════════════════════════════════════════════════════════════
            // TOP BAR: Back Button + Flash Studio Title + Prominent "Import" Button
            // ══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.glassBg)
                    .border(width = 0.5.dp, color = colors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + Title & Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                SoundManager.playTap(context)
                                onBack()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "Back",
                                tint = colors.textMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Flash",
                                    color = colors.textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Studio",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                            Text(
                                text = "${displayItems.size} projects & drafts",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Right: Primary "+ New Video" Button
                    Button(
                        onClick = {
                            SoundManager.playTap(context)
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Video,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "New Video",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)

            // ══════════════════════════════════════════════════════════════════
            // FILTER TABS: All / Draft / Saved
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlashHubFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF007AFF)
                                else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF007AFF) else if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                SoundManager.playTap(context)
                                selectedFilter = filter
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.White else colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ══════════════════════════════════════════════════════════════════
            // 3*... GRID LAYOUT: 3 Fixed Columns of Flash Videos & Drafts
            // ══════════════════════════════════════════════════════════════════
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (displayItems.isEmpty()) {
                    // Empty State Span with Apple Flash Video Graphic (Same as User Profile) & Primary Action Fallback
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val flashGraphicState = rememberAppleGraphicAnimationState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AppleFlashGraphic(
                                    modifier = Modifier.size(150.dp),
                                    state = flashGraphicState,
                                    enableClickToAnimate = true
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (selectedFilter == FlashHubFilter.DRAFTS) "No Drafted Videos"
                                    else if (selectedFilter == FlashHubFilter.SAVED) "No Saved Flash Videos"
                                    else "No Flash Videos Found",
                                    color = colors.textMain,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Tap '+ New Video' to pick clips from your gallery or record a video and begin editing.",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        SoundManager.playTap(context)
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Video,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Pick or Record Video",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = displayItems,
                        key = { it.id }
                    ) { item ->
                        FlashGridCard(
                            item = item,
                            videoDecoderFactory = videoDecoderFactory,
                            onClick = {
                                SoundManager.playTap(context)
                                when (item) {
                                    is FlashHubItem.Draft -> onOpenDraft(item.draft)
                                    is FlashHubItem.Saved -> {
                                        val uri = Uri.parse(item.video.videoUrl)
                                        onOpenSavedVideo(uri)
                                    }
                                    is FlashHubItem.LocalExported -> {
                                        onOpenSavedVideo(item.fileUri)
                                    }
                                }
                            },
                            onRename = {
                                itemToRename = item
                                renameText = item.title
                            },
                            onDuplicate = {
                                coroutineScope.launch {
                                    when (item) {
                                        is FlashHubItem.Draft -> {
                                            val copy = draftRepo.duplicateDraft(item.id)
                                            if (copy != null) {
                                                ToastManager.showToast("Draft duplicated: ${copy.title}")
                                            }
                                        }
                                        is FlashHubItem.LocalExported -> {
                                            try {
                                                val src = File(item.fileUri.path ?: "")
                                                if (src.exists()) {
                                                    val copyName = "pro_export_${System.currentTimeMillis()}.mp4"
                                                    val dest = File(src.parentFile, copyName)
                                                    src.copyTo(dest, overwrite = true)
                                                    val newUri = Uri.fromFile(dest)
                                                    val copyItem = item.copy(
                                                        id = dest.name,
                                                        title = "${item.title} (Copy)",
                                                        timestamp = System.currentTimeMillis(),
                                                        fileUri = newUri,
                                                        previewUriOrUrl = newUri.toString()
                                                    )
                                                    localExportedVideos = listOf(copyItem) + localExportedVideos
                                                    ToastManager.showToast("Export duplicated")
                                                }
                                            } catch (_: Exception) {
                                                ToastManager.showToast("Duplication failed")
                                            }
                                        }
                                        is FlashHubItem.Saved -> {
                                            val newDraft = CreatorPostDraft(
                                                id = UUID.randomUUID().toString(),
                                                mode = "flash",
                                                title = "${item.title} (Copy)",
                                                videoUri = item.video.videoUrl,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            draftRepo.saveDraft(newDraft)
                                            ToastManager.showToast("Saved to Drafts")
                                        }
                                    }
                                }
                            },
                            onExport = {
                                try {
                                    when (item) {
                                        is FlashHubItem.LocalExported -> {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, item.fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                        }
                                        is FlashHubItem.Draft -> {
                                            val videoUri = item.draft.videoUri?.let { Uri.parse(it) }
                                            val imageUri = item.draft.imageUris.firstOrNull()?.let { Uri.parse(it) }
                                            val streamUri = videoUri ?: imageUri
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                if (streamUri != null) {
                                                    type = if (videoUri != null) "video/*" else "image/*"
                                                    putExtra(Intent.EXTRA_STREAM, streamUri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                } else {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "ChromaStudio Project: ${item.title}")
                                                }
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Project"))
                                        }
                                        is FlashHubItem.Saved -> {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Watch my Flash video: ${item.video.videoUrl}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Flash Video"))
                                        }
                                    }
                                } catch (_: Exception) {
                                    ToastManager.showToast("Unable to share media")
                                }
                            },
                            onDelete = {
                                itemToDelete = item
                            }
                        )
                    }
                }
            }
        }

        // Rename Dialog — Apple HIG Style Outlined Dialog
        if (itemToRename != null) {
            val target = itemToRename!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { itemToRename = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { itemToRename = null },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(0.88f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Rename Project",
                                color = colors.textMain,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )

                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.glassBorder,
                                    focusedTextColor = colors.textMain,
                                    unfocusedTextColor = colors.textMain,
                                    cursorColor = colors.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { itemToRename = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", color = colors.textMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        val newTitle = renameText.trim()
                                        if (newTitle.isNotEmpty()) {
                                            val current = target
                                            itemToRename = null
                                            coroutineScope.launch {
                                                when (current) {
                                                    is FlashHubItem.Draft -> {
                                                        draftRepo.renameDraft(current.id, newTitle)
                                                        ToastManager.showToast("Project renamed")
                                                    }
                                                    is FlashHubItem.LocalExported -> {
                                                        try {
                                                            val oldFile = File(current.fileUri.path ?: "")
                                                            val sanitized = newTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                                            val newFile = File(oldFile.parentFile, "pro_export_${sanitized}.mp4")
                                                            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                                                                localExportedVideos = localExportedVideos.map {
                                                                    if (it.id == current.id) {
                                                                        it.copy(
                                                                            id = newFile.name,
                                                                            title = newTitle,
                                                                            fileUri = Uri.fromFile(newFile),
                                                                            previewUriOrUrl = Uri.fromFile(newFile).toString()
                                                                        )
                                                                    } else it
                                                                }
                                                                ToastManager.showToast("Export renamed")
                                                            } else {
                                                                ToastManager.showToast("Project renamed")
                                                            }
                                                        } catch (_: Exception) {
                                                            ToastManager.showToast("Project renamed")
                                                        }
                                                    }
                                                    is FlashHubItem.Saved -> {
                                                        ToastManager.showToast("Saved title updated")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog — Apple HIG Style matching TrashScreen
        if (itemToDelete != null) {
            val item = itemToDelete!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { itemToDelete = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { itemToDelete = null },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}) {
                        com.chroma.studio.ui.graphics3d.AppleHigDialogCard(
                            graphicId = com.chroma.studio.ui.graphics3d.Apple3DGraphicId.TRASH_BIN,
                            title = if (item.isDraft) "Delete Draft?" else "Delete Video?",
                            description = "This action cannot be undone.",
                            cancelText = "Cancel",
                            confirmText = "Delete",
                            onCancel = { itemToDelete = null },
                            onConfirm = {
                                val target = item
                                itemToDelete = null
                                coroutineScope.launch {
                                    when (target) {
                                        is FlashHubItem.Draft -> {
                                            draftRepo.deleteDraft(target.id)
                                            ToastManager.showToast("Draft deleted")
                                        }
                                        is FlashHubItem.LocalExported -> {
                                            try {
                                                File(target.fileUri.path ?: "").delete()
                                                localExportedVideos = localExportedVideos.filter { it.id != target.id }
                                                ToastManager.showToast("Exported video deleted")
                                            } catch (_: Exception) {}
                                        }
                                        is FlashHubItem.Saved -> {
                                            try {
                                                FlashRepository().deleteFlash(target.id)
                                                userFlashes = userFlashes.filter { it.id != target.id }
                                                ToastManager.showToast("Video deleted")
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Column Grid Card for a Drafted or Saved Flash Video with rich metadata & Apple HIG action menu
 */
@Composable
private fun FlashGridCard(
    item: FlashHubItem,
    videoDecoderFactory: VideoFrameDecoder.Factory,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141416))
            .border(
                width = 0.8.dp,
                color = colors.glassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Thumbnail Image / Video Frame with optimized memory/disk cache keys
        if (!item.previewUriOrUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.previewUriOrUrl)
                    .decoderFactory(videoDecoderFactory)
                    .videoFrameMillis(800)
                    .memoryCacheKey("flash_thumb_${item.id}_${item.timestamp}")
                    .diskCacheKey("flash_thumb_${item.id}_${item.timestamp}")
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Video,
                    contentDescription = null,
                    tint = colors.textMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Top Badges Row: Draft/Saved/Export Status Pill & Apple HIG Dropdown Action Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Pill
            when (item) {
                is FlashHubItem.Draft -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DRAFT",
                            color = Color.Black,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                is FlashHubItem.LocalExported -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXPORT",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                is FlashHubItem.Saved -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SAVED",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Apple HIG Action Menu Button
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            SoundManager.playTap(context)
                            showMenu = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Ellipsis,
                        contentDescription = "Project Actions",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(if (colors.isDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Lucide.Pencil, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Lucide.Copy, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export / Share", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Lucide.Share2, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                    HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Lucide.Trash2, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        // Bottom High-Contrast Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // Bottom Details: Title, Duration, Clip Count, Resolution, and Date
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Project Title
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration & Clip Count Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.formattedDuration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.formattedDuration,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${item.clipCount} ${if (item.clipCount == 1) "clip" else "clips"}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Resolution & Formatted Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.resolution,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = item.formattedDate,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}
