package com.chroma.studio.ui.screens

import android.content.Context
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
    abstract val isDraft: Boolean

    data class Draft(
        val draft: CreatorPostDraft,
        override val id: String = draft.id,
        override val title: String = draft.title.ifBlank { "Untitled Draft" },
        override val timestamp: Long = draft.timestamp,
        override val durationSec: Float = ((draft.flashTrimEndMs - draft.flashTrimStartMs).coerceAtLeast(0L) / 1000f),
        override val previewUriOrUrl: String? = draft.videoUri ?: draft.imageUris.firstOrNull(),
        override val isDraft: Boolean = true
    ) : FlashHubItem()

    data class Saved(
        val video: FlashVideo,
        override val id: String = video.id,
        override val title: String = video.caption.ifBlank { "Saved Flash Video" },
        override val timestamp: Long = video.timestamp,
        override val durationSec: Float = (video.duration / 1000f),
        override val previewUriOrUrl: String? = video.thumbnailUrl.ifBlank { video.videoUrl },
        override val isDraft: Boolean = false
    ) : FlashHubItem()

    data class LocalExported(
        override val id: String,
        override val title: String,
        override val timestamp: Long,
        override val durationSec: Float,
        override val previewUriOrUrl: String?,
        val fileUri: Uri,
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

    var selectedFilter by remember { mutableStateOf(FlashHubFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<FlashHubItem?>(null) }
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
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        if (ms != null && ms > 0L) durSec = ms / 1000f
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
                        fileUri = uri
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

                    // Right: Primary "Import" Button (Supports multiple videos and photos)
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Import",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // FILTER TABS: All / Draft / Saved
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlashHubFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFF007AFF)
                                else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF007AFF) else if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(20.dp)
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
                // First Tile: Quick "+ Import" Card to make a new video
                item(key = "import_tile") {
                    ImportMediaTile(
                        onClick = {
                            SoundManager.playTap(context)
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }

                if (displayItems.isEmpty()) {
                    // Empty State Span with 3D Video Graphics
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        var isGraphicOpen by remember { mutableStateOf(false) }
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
                                com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon(
                                    id = com.chroma.studio.ui.graphics3d.Apple3DGraphicId.FLASH_VIDEO,
                                    isOpen = isGraphicOpen,
                                    modifier = Modifier.size(136.dp),
                                    onClick = { isGraphicOpen = !isGraphicOpen }
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
                                    text = "Tap 'Import' to add multiple videos & photos and begin editing.",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        SoundManager.playTap(context)
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF007AFF),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Plus,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Import Media",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
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
                            onDelete = {
                                itemToDelete = item
                            }
                        )
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
 * First Tile in 3-column Grid: Styled "+ Import" card to add multiple media
 */
@Composable
private fun ImportMediaTile(
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primary.copy(alpha = 0.18f),
                        colors.glassBg.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = colors.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Import Media",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Import",
                color = colors.textMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Videos & Photos",
                color = colors.textMuted,
                fontSize = 9.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 3-Column Grid Card for a Drafted or Saved Flash Video
 */
@Composable
private fun FlashGridCard(
    item: FlashHubItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current

    val formattedDuration = remember(item.durationSec) {
        if (item.durationSec > 0f) {
            val totalSec = item.durationSec.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            String.format("%d:%02d", m, s)
        } else {
            ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141416))
            .border(
                width = 0.6.dp,
                color = colors.glassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Thumbnail Image / Video Frame
        if (!item.previewUriOrUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.previewUriOrUrl)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .videoFrameMillis(800)
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

        // Top Badges Row: Draft/Saved Status Pill & Delete Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Pill
            if (item.isDraft) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DRAFT",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SAVED",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Quick Delete Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Bottom Details: Title & Duration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (formattedDuration.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
