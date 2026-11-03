package com.chroma.studio.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.CachePolicy
import coil.size.Scale
import coil.size.Size
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import com.chroma.studio.chat.media.GalleryMediaItem
import com.chroma.studio.chat.media.MediaStoreHelper
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter for media types in Oscellion Gallery
 */
enum class GalleryMediaTypeFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos")
}

/**
 * Sort options for media items
 */
enum class GallerySortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    DURATION("Duration"),
    NAME("Name")
}

/**
 * Presentation mode for Oscellion Gallery
 */
enum class GalleryLayoutMode {
    FULL_SCREEN,
    BOTTOM_SHEET
}

/**
 * View modes matching Samsung Native One UI Gallery:
 * - PICTURES: Chronological timeline view with date headers and group selection
 * - ALBUMS: 2-column squircle cards grid of media buckets/folders
 */
enum class GalleryViewMode(val label: String) {
    PICTURES("Pictures"),
    ALBUMS("Albums")
}

/**
 * Date-grouped timeline chunk for Samsung One UI chronological gallery
 */
data class GalleryDateGroup(
    val title: String,
    val dateKey: String,
    val items: List<GalleryMediaItem>
)

/**
 * Album card information for Samsung One UI Albums view
 */
data class GalleryAlbumInfo(
    val name: String,
    val count: Int,
    val coverUri: Uri?,
    val isVideoOnly: Boolean = false
)

/**
 * Groups media items chronologically by date:
 * - "Today"
 * - "Yesterday"
 * - "EEE, MMM d" (for this year, e.g. "Wed, Oct 28")
 * - "MMM d, yyyy" (for earlier years)
 */
fun groupMediaByDate(items: List<GalleryMediaItem>): List<GalleryDateGroup> {
    if (items.isEmpty()) return emptyList()
    val calNow = Calendar.getInstance()
    val nowYear = calNow.get(Calendar.YEAR)
    val nowDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)

    val itemCal = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val yearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val grouped = linkedMapOf<String, Pair<String, MutableList<GalleryMediaItem>>>()

    for (item in items) {
        val millis = if (item.dateAdded < 100000000000L) item.dateAdded * 1000L else item.dateAdded
        itemCal.timeInMillis = millis
        val itemYear = itemCal.get(Calendar.YEAR)
        val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

        val key = keyFormat.format(itemCal.time)
        val title = when {
            itemYear == nowYear && itemDayOfYear == nowDayOfYear -> "Today"
            itemYear == nowYear && itemDayOfYear == nowDayOfYear - 1 -> "Yesterday"
            itemYear == nowYear -> dayFormat.format(itemCal.time)
            else -> yearFormat.format(itemCal.time)
        }

        val entry = grouped.getOrPut(key) { Pair(title, mutableListOf()) }
        entry.second.add(item)
    }

    return grouped.map { (key, pair) ->
        GalleryDateGroup(
            title = pair.first,
            dateKey = key,
            items = pair.second
        )
    }
}

/**
 * Unified, Fixed Oscellion Gallery Layout powered by Samsung One UI 7.0:
 * - Pictures vs Albums segmented view modes with 2-column squircle album covers.
 * - Chronological date timeline grouping with sticky headers and 1-tap "Select day" group toggles.
 * - Dynamic pinch-to-zoom grid density scaling (2, 3, 4, 5 columns) with instant haptic ticks.
 * - Drag-to-select with smooth auto-scroll and 120Hz performance.
 * - One UI 7 Floating Now Bar selection dock with mini thumbnail strip and Brand Blue (#0A84FF) Confirm button.
 * - Full Lightbox viewer with slide-up Samsung Details (ⓘ) sheet showing EXIF/resolution/storage path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OscellionGalleryLayout(
    layoutMode: GalleryLayoutMode = GalleryLayoutMode.FULL_SCREEN,
    title: String = "Gallery",
    badgeText: String = "Oscellion",
    initialSelectedUris: List<Uri> = emptyList(),
    maxSelection: Int = 10,
    allowedMediaFilter: GalleryMediaTypeFilter = GalleryMediaTypeFilter.ALL,
    showCameraTile: Boolean = true,
    showCaptionInput: Boolean = false,
    showAlbumsTab: Boolean = (layoutMode == GalleryLayoutMode.FULL_SCREEN),
    onVideoSelected: ((Uri) -> Unit)? = null,
    onPhotosSelected: ((List<Uri>) -> Unit)? = null,
    onSendMediaWithCaption: ((List<Uri>, String?) -> Unit)? = null,
    onLaunchCamera: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current

    // Permission state
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
    }

    // Direct native System PhotoPicker fallback (zero permission barrier)
    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxSelection.coerceAtLeast(2))
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            SoundManager.playTap(context)
            if (onVideoSelected != null && uris.size == 1) {
                val mime = context.contentResolver.getType(uris.first()) ?: ""
                if (mime.startsWith("video/")) {
                    onVideoSelected(uris.first())
                    return@rememberLauncherForActivityResult
                }
            }
            if (onSendMediaWithCaption != null) {
                onSendMediaWithCaption(uris, null)
            } else {
                onPhotosSelected?.invoke(uris)
            }
        }
    }

    // Camera launcher fallback if onLaunchCamera not provided
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            SoundManager.playReaction()
        }
    }

    // Media states
    var allMedia by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Samsung One UI View Mode state (Pictures vs Albums)
    var viewMode by remember { mutableStateOf(GalleryViewMode.PICTURES) }

    // Controls state
    var mediaTypeFilter by remember { mutableStateOf(allowedMediaFilter) }
    var selectedAlbum by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(GallerySortOrder.NEWEST) }
    var gridColumns by remember { mutableIntStateOf(3) } // Supports 2, 3, 4, 5 columns
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Multi-selection state
    val selectedUris = remember { mutableStateListOf<Uri>().apply { addAll(initialSelectedUris) } }
    var captionText by remember { mutableStateOf("") }

    // Modern Lightbox multi-item swipe & zoom state
    var lightboxInitialIndex by remember { mutableIntStateOf(0) }
    var showLightbox by remember { mutableStateOf(false) }
    var lightboxMediaList by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }

    // Load media from MediaStore
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            allMedia = withContext(Dispatchers.IO) {
                MediaStoreHelper.loadDeviceMedia(context, maxCount = 2000)
            }
            isLoading = false
        }
    }

    // Samsung One UI Albums List with cover photos and counts
    val albumsList: List<GalleryAlbumInfo> = remember(allMedia) {
        val map = linkedMapOf<String, MutableList<GalleryMediaItem>>()
        allMedia.forEach { item ->
            val bucket = item.bucketName.ifBlank { "Other" }
            map.getOrPut(bucket) { mutableListOf() }.add(item)
        }
        map.map { (name, items) ->
            GalleryAlbumInfo(
                name = name,
                count = items.size,
                coverUri = items.firstOrNull()?.uri,
                isVideoOnly = items.all { it.isVideo }
            )
        }.sortedByDescending { it.count }
    }

    // Distinct albums list with counts for dropdown
    val albumsWithCounts = remember(allMedia) {
        val counts = mutableMapOf<String, Int>()
        counts["All"] = allMedia.size
        allMedia.forEach { item ->
            val bucket = item.bucketName.ifBlank { "Other" }
            counts[bucket] = (counts[bucket] ?: 0) + 1
        }
        counts.toList().sortedByDescending { if (it.first == "All") Int.MAX_VALUE else it.second }
    }

    // Filtered & sorted media list
    val filteredMedia = remember(allMedia, mediaTypeFilter, selectedAlbum, sortOrder, searchQuery) {
        var list = allMedia

        // 1. Media Type Filter
        list = when (mediaTypeFilter) {
            GalleryMediaTypeFilter.ALL -> list
            GalleryMediaTypeFilter.PHOTOS -> list.filter { !it.isVideo }
            GalleryMediaTypeFilter.VIDEOS -> list.filter { it.isVideo }
        }

        // 2. Album Filter
        if (selectedAlbum != "All") {
            list = list.filter { it.bucketName.equals(selectedAlbum, ignoreCase = true) }
        }

        // 3. Search Query Filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.bucketName.lowercase().contains(q)
            }
        }

        // 4. Sort Order
        when (sortOrder) {
            GallerySortOrder.NEWEST -> list.sortedByDescending { it.dateAdded }
            GallerySortOrder.OLDEST -> list.sortedBy { it.dateAdded }
            GallerySortOrder.DURATION -> list.sortedByDescending { it.durationMs }
            GallerySortOrder.NAME -> list.sortedBy { it.name.lowercase() }
        }
    }

    // Chronologically grouped media list for Samsung One UI timeline
    val dateGroups = remember(filteredMedia) {
        groupMediaByDate(filteredMedia)
    }

    fun handleCategorySelect(cat: String) {
        selectedCategory = cat
        when (cat) {
            "All" -> {
                selectedAlbum = "All"
                mediaTypeFilter = GalleryMediaTypeFilter.ALL
            }
            "Camera" -> {
                val cameraAlbum = albumsWithCounts.firstOrNull {
                    it.first.contains("Camera", ignoreCase = true) || it.first.contains("DCIM", ignoreCase = true)
                }?.first ?: "Camera"
                selectedAlbum = cameraAlbum
                mediaTypeFilter = GalleryMediaTypeFilter.ALL
            }
            "Screenshots" -> {
                val ssAlbum = albumsWithCounts.firstOrNull {
                    it.first.contains("Screenshot", ignoreCase = true)
                }?.first ?: "Screenshots"
                selectedAlbum = ssAlbum
                mediaTypeFilter = GalleryMediaTypeFilter.ALL
            }
            "Videos" -> {
                selectedAlbum = "All"
                mediaTypeFilter = GalleryMediaTypeFilter.VIDEOS
            }
            "Photos" -> {
                selectedAlbum = "All"
                mediaTypeFilter = GalleryMediaTypeFilter.PHOTOS
            }
            "Downloads" -> {
                val dlAlbum = albumsWithCounts.firstOrNull {
                    it.first.contains("Download", ignoreCase = true)
                }?.first ?: "Download"
                selectedAlbum = dlAlbum
                mediaTypeFilter = GalleryMediaTypeFilter.ALL
            }
        }
    }

    fun handleSelectAll() {
        SoundManager.playTap(context)
        val remaining = maxSelection - selectedUris.size
        if (remaining > 0) {
            val toAdd = filteredMedia
                .map { it.uri }
                .filter { !selectedUris.contains(it) }
                .take(remaining)
            selectedUris.addAll(toAdd)
        }
    }

    // Layout Wrapper: Modal sheet or Fullscreen
    if (layoutMode == GalleryLayoutMode.BOTTOM_SHEET) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colors.bg)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.textMuted.copy(alpha = 0.35f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header & Controls
                GalleryTopBar(
                    title = title,
                    badgeText = "${filteredMedia.size}",
                    isBottomSheet = true,
                    isSearchVisible = isSearchVisible,
                    onToggleSearch = { isSearchVisible = !isSearchVisible },
                    onOpenSystemPicker = {
                        systemPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onClose = onClose
                )

                // Samsung One UI Segmented Tabs: Pictures vs Albums
                if (showAlbumsTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GallerySegmentedTabs(
                            selectedMode = viewMode,
                            onModeChange = {
                                SoundManager.playTap(context)
                                viewMode = it
                            }
                        )
                    }
                }

                // Active Album Filter pill if user drilled down into a specific album
                if (viewMode == GalleryViewMode.PICTURES && selectedAlbum != "All") {
                    GalleryActiveAlbumChip(
                        albumName = selectedAlbum,
                        onClear = {
                            SoundManager.playTap(context)
                            selectedAlbum = "All"
                        }
                    )
                }

                if (viewMode == GalleryViewMode.ALBUMS) {
                    GalleryAlbumsGrid(
                        albums = albumsList,
                        onSelectAlbum = { albumName ->
                            selectedAlbum = albumName
                            viewMode = GalleryViewMode.PICTURES
                        }
                    )
                } else {
                    GalleryMainBody(
                        hasPermission = hasPermission,
                        isLoading = isLoading,
                        mediaItems = filteredMedia,
                        dateGroups = dateGroups,
                        selectedUris = selectedUris,
                        maxSelection = maxSelection,
                        gridColumns = gridColumns,
                        selectedAlbum = selectedAlbum,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { handleCategorySelect(it) },
                        albumsWithCounts = albumsWithCounts,
                        mediaTypeFilter = mediaTypeFilter,
                        sortOrder = sortOrder,
                        isSearchVisible = isSearchVisible,
                        searchQuery = searchQuery,
                        showCameraTile = showCameraTile && selectedAlbum == "All" && searchQuery.isBlank(),
                        showCaptionInput = showCaptionInput,
                        captionText = captionText,
                        onCaptionChange = { captionText = it },
                        onAlbumChange = { selectedAlbum = it },
                        onTypeFilterChange = { mediaTypeFilter = it },
                        onSortOrderChange = { sortOrder = it },
                        onToggleColumns = {
                            gridColumns = when (gridColumns) {
                                2 -> 3
                                3 -> 4
                                4 -> 5
                                else -> 2
                            }
                        },
                        onSearchQueryChange = { searchQuery = it },
                        onLaunchCamera = {
                            SoundManager.playTap(context)
                            if (onLaunchCamera != null) {
                                onLaunchCamera()
                            } else {
                                val (_, uri) = MediaStoreHelper.createTempImageUri(context)
                                cameraCaptureLauncher.launch(uri)
                            }
                        },
                        onUpdateSelectedUris = { newUris: List<Uri> ->
                            selectedUris.clear()
                            selectedUris.addAll(newUris)
                        },
                        onMediaClick = { item ->
                            SoundManager.playPop()
                            if (item.isVideo && onVideoSelected != null && selectedUris.isEmpty()) {
                                onVideoSelected(item.uri)
                            } else {
                                if (selectedUris.contains(item.uri)) {
                                    selectedUris.remove(item.uri)
                                } else if (selectedUris.size < maxSelection) {
                                    selectedUris.add(item.uri)
                                }
                            }
                        },
                        onMediaLongClick = { item ->
                            SoundManager.playPop()
                            if (!selectedUris.contains(item.uri) && selectedUris.size < maxSelection) {
                                selectedUris.add(item.uri)
                            }
                        },
                        onQuickPreview = { item ->
                            SoundManager.playReaction()
                            val idx = filteredMedia.indexOf(item).coerceAtLeast(0)
                            lightboxInitialIndex = idx
                            lightboxMediaList = filteredMedia
                            showLightbox = true
                        },
                        onSelectAll = { handleSelectAll() },
                        onClearSelection = {
                            SoundManager.playTap(context)
                            selectedUris.clear()
                        },
                        onRemoveUri = { uri ->
                            SoundManager.playTap(context)
                            selectedUris.remove(uri)
                        },
                        onSubmitSelection = {
                            SoundManager.playSend(context)
                            if (onSendMediaWithCaption != null) {
                                onSendMediaWithCaption(selectedUris.toList(), captionText.ifBlank { null })
                                onClose()
                            } else {
                                onPhotosSelected?.invoke(selectedUris.toList())
                            }
                        },
                        onRequestPermission = {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        onOpenSystemPicker = {
                            systemPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }
            }
        }
    } else {
        // Full screen presentation
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
        ) {
            // Fullscreen Screen Header with Reachability & Clean Flat Icons
            ChromaScreenHeader(
                title = title,
                badgeText = null,
                showBackButton = true,
                circularBackButton = false,
                onBack = onClose,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // System Picker Icon Button
                        IconButton(
                            onClick = {
                                SoundManager.playTap(context)
                                systemPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.FolderOpen,
                                contentDescription = "System",
                                tint = colors.textMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Search Icon Button
                        IconButton(
                            onClick = {
                                SoundManager.playTap(context)
                                isSearchVisible = !isSearchVisible
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible) Color(0xFF0A84FF) else colors.textMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            // Samsung One UI Segmented Switcher (Pictures vs Albums)
            if (showAlbumsTab) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GallerySegmentedTabs(
                        selectedMode = viewMode,
                        onModeChange = {
                            SoundManager.playTap(context)
                            viewMode = it
                        }
                    )
                }
            }

            // Active Album Filter chip
            if (viewMode == GalleryViewMode.PICTURES && selectedAlbum != "All") {
                GalleryActiveAlbumChip(
                    albumName = selectedAlbum,
                    onClear = {
                        SoundManager.playTap(context)
                        selectedAlbum = "All"
                    }
                )
            }

            if (viewMode == GalleryViewMode.ALBUMS) {
                GalleryAlbumsGrid(
                    albums = albumsList,
                    onSelectAlbum = { albumName ->
                        selectedAlbum = albumName
                        viewMode = GalleryViewMode.PICTURES
                    }
                )
            } else {
                GalleryMainBody(
                    hasPermission = hasPermission,
                    isLoading = isLoading,
                    mediaItems = filteredMedia,
                    dateGroups = dateGroups,
                    selectedUris = selectedUris,
                    maxSelection = maxSelection,
                    gridColumns = gridColumns,
                    selectedAlbum = selectedAlbum,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { handleCategorySelect(it) },
                    albumsWithCounts = albumsWithCounts,
                    mediaTypeFilter = mediaTypeFilter,
                    sortOrder = sortOrder,
                    isSearchVisible = isSearchVisible,
                    searchQuery = searchQuery,
                    showCameraTile = showCameraTile && selectedAlbum == "All" && searchQuery.isBlank(),
                    showCaptionInput = showCaptionInput,
                    captionText = captionText,
                    onCaptionChange = { captionText = it },
                    onAlbumChange = { selectedAlbum = it },
                    onTypeFilterChange = { mediaTypeFilter = it },
                    onSortOrderChange = { sortOrder = it },
                    onToggleColumns = {
                        gridColumns = when (gridColumns) {
                            2 -> 3
                            3 -> 4
                            4 -> 5
                            else -> 2
                        }
                    },
                    onSearchQueryChange = { searchQuery = it },
                    onLaunchCamera = {
                        SoundManager.playTap(context)
                        if (onLaunchCamera != null) {
                            onLaunchCamera()
                        } else {
                            val (_, uri) = MediaStoreHelper.createTempImageUri(context)
                            cameraCaptureLauncher.launch(uri)
                        }
                    },
                    onUpdateSelectedUris = { newUris: List<Uri> ->
                        selectedUris.clear()
                        selectedUris.addAll(newUris)
                    },
                    onMediaClick = { item ->
                        SoundManager.playPop()
                        if (item.isVideo && onVideoSelected != null && selectedUris.isEmpty()) {
                            onVideoSelected(item.uri)
                        } else {
                            if (selectedUris.contains(item.uri)) {
                                selectedUris.remove(item.uri)
                            } else if (selectedUris.size < maxSelection) {
                                selectedUris.add(item.uri)
                            }
                        }
                    },
                    onMediaLongClick = { item ->
                        SoundManager.playPop()
                        if (!selectedUris.contains(item.uri) && selectedUris.size < maxSelection) {
                            selectedUris.add(item.uri)
                        }
                    },
                    onQuickPreview = { item ->
                        SoundManager.playReaction()
                        val idx = filteredMedia.indexOf(item).coerceAtLeast(0)
                        lightboxInitialIndex = idx
                        lightboxMediaList = filteredMedia
                        showLightbox = true
                    },
                    onSelectAll = { handleSelectAll() },
                    onClearSelection = {
                        SoundManager.playTap(context)
                        selectedUris.clear()
                    },
                    onRemoveUri = { uri ->
                        SoundManager.playTap(context)
                        selectedUris.remove(uri)
                    },
                    onSubmitSelection = {
                        SoundManager.playSend(context)
                        if (onSendMediaWithCaption != null) {
                            onSendMediaWithCaption(selectedUris.toList(), captionText.ifBlank { null })
                            onClose()
                        } else {
                            onPhotosSelected?.invoke(selectedUris.toList())
                        }
                    },
                    onRequestPermission = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(perms)
                    },
                    onOpenSystemPicker = {
                        systemPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            }
        }
    }

    // Modern Lightbox Multi-Item Swipe & Zoom Dialog with Samsung One UI Details Sheet
    if (showLightbox && lightboxMediaList.isNotEmpty()) {
        GalleryLightboxDialog(
            mediaList = lightboxMediaList,
            initialIndex = lightboxInitialIndex,
            selectedUris = selectedUris,
            maxSelection = maxSelection,
            onToggleSelect = { item ->
                SoundManager.playPop()
                if (selectedUris.contains(item.uri)) {
                    selectedUris.remove(item.uri)
                } else if (selectedUris.size < maxSelection) {
                    selectedUris.add(item.uri)
                }
            },
            onDirectSelect = { item ->
                SoundManager.playSend(context)
                if (item.isVideo && onVideoSelected != null) {
                    onVideoSelected(item.uri)
                } else {
                    if (!selectedUris.contains(item.uri)) {
                        selectedUris.add(item.uri)
                    }
                    if (onSendMediaWithCaption != null) {
                        onSendMediaWithCaption(selectedUris.toList(), captionText.ifBlank { null })
                        showLightbox = false
                        onClose()
                    } else {
                        showLightbox = false
                        onPhotosSelected?.invoke(selectedUris.toList())
                    }
                }
            },
            onDismiss = { showLightbox = false }
        )
    }
}

/**
 * Top bar for bottom-sheet mode
 */
@Composable
private fun GalleryTopBar(
    title: String,
    badgeText: String,
    isBottomSheet: Boolean,
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSystemPicker: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // System Picker icon
            IconButton(
                onClick = onOpenSystemPicker,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Lucide.FolderOpen,
                    contentDescription = "System",
                    tint = colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Search icon
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Lucide.Search,
                    contentDescription = "Search",
                    tint = if (isSearchVisible) Color(0xFF0A84FF) else colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Close",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Samsung One UI Segmented Switcher: [ Pictures | Albums ]
 */
@Composable
private fun GallerySegmentedTabs(
    selectedMode: GalleryViewMode,
    onModeChange: (GalleryViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp)),
        color = colors.glassBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryViewMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                val bgAnim by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF0A84FF) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBg"
                )
                val textAnim by animateColorAsState(
                    targetValue = if (isSelected) Color.White else colors.textMuted,
                    animationSpec = tween(180),
                    label = "tabText"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgAnim)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = textAnim,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Active Album Clear Filter Chip
 */
@Composable
private fun GalleryActiveAlbumChip(
    albumName: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A84FF).copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color(0xFF0A84FF).copy(alpha = 0.35f)),
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                Lucide.Folder,
                contentDescription = null,
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = albumName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A84FF)
            )
            Icon(
                Lucide.X,
                contentDescription = "Clear",
                tint = Color(0xFF0A84FF),
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .clickable { onClear() }
            )
        }
    }
}

/**
 * Samsung One UI Date Group Header with 1-tap "Select day" group checkmark
 */
@Composable
private fun GalleryDateGroupHeader(
    group: GalleryDateGroup,
    selectedCountInGroup: Int,
    totalCountInGroup: Int,
    onToggleSelectGroup: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    val allSelected = selectedCountInGroup == totalCountInGroup && totalCountInGroup > 0
    val someSelected = selectedCountInGroup > 0 && !allSelected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = group.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
            Text(
                text = "${group.items.size} ${if (group.items.size == 1) "item" else "items"}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textMuted
            )
        }

        // Samsung 1-tap "Select day" checkmark circle
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (allSelected) Color(0xFF0A84FF)
                    else if (someSelected) Color(0xFF0A84FF).copy(alpha = 0.25f)
                    else colors.glassBg
                )
                .border(
                    width = if (allSelected || someSelected) 1.5.dp else 1.dp,
                    color = if (allSelected || someSelected) Color(0xFF0A84FF) else colors.textMuted.copy(alpha = 0.45f),
                    shape = CircleShape
                )
                .clickable {
                    onToggleSelectGroup(!allSelected)
                },
            contentAlignment = Alignment.Center
        ) {
            if (allSelected) {
                Icon(
                    Lucide.Check,
                    contentDescription = "Select day",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            } else if (someSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A84FF))
                )
            }
        }
    }
}

/**
 * Samsung One UI Albums Grid View (2-column squircle cards with cover, count badge, and titles)
 */
@Composable
private fun GalleryAlbumsGrid(
    albums: List<GalleryAlbumInfo>,
    onSelectAlbum: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No albums found", color = colors.textMuted, fontSize = 14.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { it.name }
        ) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        SoundManager.playTap(context)
                        onSelectAlbum(album.name)
                    }
            ) {
                // Cover preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                ) {
                    if (album.coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(album.coverUri)
                                .size(Size(320, 320))
                                .scale(Scale.FILL)
                                .crossfade(false)
                                .allowHardware(true)
                                .build(),
                            contentDescription = album.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Folder,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Count Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (album.isVideoOnly) {
                                Icon(
                                    Lucide.Video,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = "${album.count}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Album title and subtitle
                Text(
                    text = album.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "${album.count} ${if (album.count == 1) "item" else "items"}",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * Main scrollable body containing dropdown toolbars, quick category chips, search, and the 120Hz-optimized grid
 */
@Composable
private fun GalleryMainBody(
    hasPermission: Boolean,
    isLoading: Boolean,
    mediaItems: List<GalleryMediaItem>,
    dateGroups: List<GalleryDateGroup>,
    selectedUris: List<Uri>,
    maxSelection: Int,
    gridColumns: Int,
    selectedAlbum: String,
    selectedCategory: String = "All",
    onCategorySelected: (String) -> Unit = {},
    albumsWithCounts: List<Pair<String, Int>>,
    mediaTypeFilter: GalleryMediaTypeFilter,
    sortOrder: GallerySortOrder,
    isSearchVisible: Boolean,
    searchQuery: String,
    showCameraTile: Boolean,
    showCaptionInput: Boolean,
    captionText: String,
    onCaptionChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onTypeFilterChange: (GalleryMediaTypeFilter) -> Unit,
    onSortOrderChange: (GallerySortOrder) -> Unit,
    onToggleColumns: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLaunchCamera: () -> Unit,
    onUpdateSelectedUris: (List<Uri>) -> Unit = {},
    onMediaClick: (GalleryMediaItem) -> Unit,
    onMediaLongClick: (GalleryMediaItem) -> Unit,
    onQuickPreview: (GalleryMediaItem) -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onClearSelection: () -> Unit,
    onRemoveUri: (Uri) -> Unit,
    onSubmitSelection: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    if (!hasPermission) {
        GalleryPermissionCard(
            onRequest = onRequestPermission,
            onOpenSystemPicker = onOpenSystemPicker
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dropdown Controls Toolbar
            GalleryControlsBar(
                selectedAlbum = selectedAlbum,
                albumsWithCounts = albumsWithCounts,
                mediaTypeFilter = mediaTypeFilter,
                sortOrder = sortOrder,
                gridColumns = gridColumns,
                onAlbumChange = onAlbumChange,
                onTypeFilterChange = onTypeFilterChange,
                onSortOrderChange = onSortOrderChange,
                onToggleColumns = onToggleColumns
            )

            // Quick Category Filter Chips (Camera, Screenshots, Videos, Photos, Downloads)
            GalleryQuickCategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )

            // Animated Expandable Search Input
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GallerySearchInputRow(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }

            Spacer(Modifier.height(4.dp))

            // Loading / Empty / Grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OscellionLoadingSpinner(color = colors.primary)
                }
            } else if (mediaItems.isEmpty() && !showCameraTile) {
                GalleryEmptyState(
                    onOpenSystemPicker = onOpenSystemPicker
                )
            } else {
                val gridState = rememberLazyGridState()
                var dragAnchorIndex by remember { mutableStateOf<Int?>(null) }
                var dragInitialSelectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
                var dragIsSelecting by remember { mutableStateOf(true) }
                val haptic = LocalHapticFeedback.current
                val coroutineScope = rememberCoroutineScope()

                // 120Hz Floating Month/Year Date Scrubber
                val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
                val visibleMonthYear by remember(mediaItems) {
                    derivedStateOf {
                        val actualIdx = if (showCameraTile) firstVisibleIndex - 1 else firstVisibleIndex
                        val item = mediaItems.getOrNull(actualIdx.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0)))
                        item?.monthYearKey ?: ""
                    }
                }

                val useDateGrouping = sortOrder == GallerySortOrder.NEWEST || sortOrder == GallerySortOrder.OLDEST

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 4.dp,
                            end = 4.dp,
                            top = 4.dp,
                            bottom = if (selectedUris.isNotEmpty()) 130.dp else 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(gridColumns) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom > 1.25f && gridColumns > 2) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleColumns()
                                    } else if (zoom < 0.75f && gridColumns < 5) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleColumns()
                                    }
                                }
                            }
                            .pointerInput(mediaItems, showCameraTile, maxSelection) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val hitItem = gridState.layoutInfo.visibleItemsInfo.find { info ->
                                            offset.x >= info.offset.x && offset.x <= info.offset.x + info.size.width &&
                                            offset.y >= info.offset.y && offset.y <= info.offset.y + info.size.height
                                        }
                                        if (hitItem != null) {
                                            val hitKey = hitItem.key.toString()
                                            val hitUriStr = hitKey.removePrefix("item_")
                                            val mediaIdx = mediaItems.indexOfFirst { it.uri.toString() == hitUriStr }
                                            if (mediaIdx != -1) {
                                                val item = mediaItems[mediaIdx]
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                SoundManager.playPop()
                                                dragAnchorIndex = mediaIdx
                                                dragInitialSelectedUris = selectedUris.toSet()
                                                dragIsSelecting = !selectedUris.contains(item.uri)

                                                val newSelection = if (dragIsSelecting) {
                                                    if (selectedUris.size < maxSelection && !selectedUris.contains(item.uri)) {
                                                        selectedUris + item.uri
                                                    } else {
                                                        selectedUris.toList()
                                                    }
                                                } else {
                                                    selectedUris - item.uri
                                                }
                                                onUpdateSelectedUris(newSelection)
                                            }
                                        }
                                    },
                                    onDragEnd = { dragAnchorIndex = null },
                                    onDragCancel = { dragAnchorIndex = null },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val anchor = dragAnchorIndex ?: return@detectDragGesturesAfterLongPress
                                        val offset = change.position

                                        // Auto-scroll near viewport edges
                                        val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                                        if (viewportHeight > 0f) {
                                            if (offset.y < 90f && gridState.canScrollBackward) {
                                                coroutineScope.launch { gridState.scrollBy(-30f) }
                                            } else if (offset.y > viewportHeight - 110f && gridState.canScrollForward) {
                                                coroutineScope.launch { gridState.scrollBy(30f) }
                                            }
                                        }

                                        val hitItem = gridState.layoutInfo.visibleItemsInfo.find { info ->
                                            offset.x >= info.offset.x && offset.x <= info.offset.x + info.size.width &&
                                            offset.y >= info.offset.y && offset.y <= info.offset.y + info.size.height
                                        }
                                        if (hitItem != null) {
                                            val hitKey = hitItem.key.toString()
                                            val hitUriStr = hitKey.removePrefix("item_")
                                            val currentIndex = mediaItems.indexOfFirst { it.uri.toString() == hitUriStr }
                                            if (currentIndex != -1) {
                                                val minIdx = minOf(anchor, currentIndex).coerceAtLeast(0)
                                                val maxIdx = maxOf(anchor, currentIndex).coerceAtMost(mediaItems.size - 1)
                                                val draggedUris = (minIdx..maxIdx).mapNotNull { mediaItems.getOrNull(it)?.uri }

                                                val newSet = LinkedHashSet(dragInitialSelectedUris)
                                                if (dragIsSelecting) {
                                                    for (uri in draggedUris) {
                                                        if (newSet.size < maxSelection) {
                                                            newSet.add(uri)
                                                        }
                                                    }
                                                } else {
                                                    newSet.removeAll(draggedUris.toSet())
                                                }

                                                val newSelection = newSet.toList()
                                                if (newSelection != selectedUris) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onUpdateSelectedUris(newSelection)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        // Live Camera Tile
                        if (showCameraTile) {
                            item(key = "camera_tile", contentType = "camera") {
                                GalleryCameraTile(onClick = onLaunchCamera)
                            }
                        }

                        // Chronological Date Timeline Grouping
                        if (useDateGrouping) {
                            dateGroups.forEach { group ->
                                // Date Group Header spanning full row with 1-tap "Select day" group toggle
                                item(
                                    key = "header_${group.dateKey}",
                                    span = { GridItemSpan(gridColumns) },
                                    contentType = "date_header"
                                ) {
                                    val groupSelectedCount = group.items.count { selectedUris.contains(it.uri) }
                                    GalleryDateGroupHeader(
                                        group = group,
                                        selectedCountInGroup = groupSelectedCount,
                                        totalCountInGroup = group.items.size,
                                        onToggleSelectGroup = { shouldSelect ->
                                            SoundManager.playTap(context)
                                            if (shouldSelect) {
                                                val remaining = maxSelection - selectedUris.size
                                                val toAdd = group.items
                                                    .map { it.uri }
                                                    .filter { !selectedUris.contains(it) }
                                                    .take(remaining)
                                                onUpdateSelectedUris(selectedUris + toAdd)
                                            } else {
                                                val groupUris = group.items.map { it.uri }.toSet()
                                                onUpdateSelectedUris(selectedUris.filter { !groupUris.contains(it) })
                                            }
                                        }
                                    )
                                }

                                // Media items inside this date group
                                items(
                                    items = group.items,
                                    key = { "item_${it.uri}" },
                                    contentType = { if (it.isVideo) "video" else "photo" }
                                ) { item ->
                                    val isSelected = selectedUris.contains(item.uri)
                                    val selectionIndex = if (isSelected) selectedUris.indexOf(item.uri) + 1 else null

                                    GalleryMediaCard(
                                        item = item,
                                        isSelected = isSelected,
                                        selectionIndex = selectionIndex,
                                        hasAnySelection = selectedUris.isNotEmpty(),
                                        onClick = { onMediaClick(item) },
                                        onLongClick = { onMediaLongClick(item) },
                                        onQuickPreview = { onQuickPreview(item) }
                                    )
                                }
                            }
                        } else {
                            // Flat list when sorted by Name or Duration
                            items(
                                items = mediaItems,
                                key = { "item_${it.uri}" },
                                contentType = { if (it.isVideo) "video" else "photo" }
                            ) { item ->
                                val isSelected = selectedUris.contains(item.uri)
                                val selectionIndex = if (isSelected) selectedUris.indexOf(item.uri) + 1 else null

                                GalleryMediaCard(
                                    item = item,
                                    isSelected = isSelected,
                                    selectionIndex = selectionIndex,
                                    hasAnySelection = selectedUris.isNotEmpty(),
                                    onClick = { onMediaClick(item) },
                                    onLongClick = { onMediaLongClick(item) },
                                    onQuickPreview = { onQuickPreview(item) }
                                )
                            }
                        }
                    }

                    // 120Hz Floating Month/Year Date Scrubber
                    androidx.compose.animation.AnimatedVisibility(
                        visible = gridState.isScrollInProgress && visibleMonthYear.isNotBlank(),
                        enter = fadeIn(animationSpec = tween(100)) + scaleIn(initialScale = 0.88f),
                        exit = fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.5.dp)
                            ) {
                                Icon(
                                    Lucide.Calendar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = visibleMonthYear,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Samsung One UI 7 Floating Now Bar / Selection Dock (at Bottom)
        AnimatedVisibility(
            visible = selectedUris.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GalleryFloatingSelectionDock(
                selectedUris = selectedUris,
                maxSelection = maxSelection,
                showCaptionInput = showCaptionInput,
                captionText = captionText,
                onCaptionChange = onCaptionChange,
                onSelectAll = onSelectAll,
                onClear = onClearSelection,
                onRemoveUri = onRemoveUri,
                onSubmit = onSubmitSelection
            )
        }
    }
}

/**
 * Modern 1-Tap Quick Category Filter Chips Bar
 */
@Composable
private fun GalleryQuickCategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val categories = remember {
        listOf(
            "All" to Lucide.Layers,
            "Camera" to Lucide.Camera,
            "Screenshots" to Lucide.Smartphone,
            "Videos" to Lucide.Film,
            "Photos" to Lucide.Image,
            "Downloads" to Lucide.Download
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories) { (name, icon) ->
            val isSelected = selectedCategory == name
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipScale"
            )

            Row(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color(0xFF0A84FF)
                        else if (colors.isDark) Color(0x1FFFFFFF)
                        else Color(0x0C000000)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onCategorySelected(name) }
                    .padding(horizontal = 9.dp, vertical = 4.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else colors.textMain,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else colors.textMain
                )
            }
        }
    }
}

/**
 * Clean Dropdown Controls Toolbar with Grid Density and Sort Selectors
 */
@Composable
private fun GalleryControlsBar(
    selectedAlbum: String,
    albumsWithCounts: List<Pair<String, Int>>,
    mediaTypeFilter: GalleryMediaTypeFilter,
    sortOrder: GallerySortOrder,
    gridColumns: Int,
    onAlbumChange: (String) -> Unit,
    onTypeFilterChange: (GalleryMediaTypeFilter) -> Unit,
    onSortOrderChange: (GallerySortOrder) -> Unit,
    onToggleColumns: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var albumMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Albums & Media Type Dropdowns
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Album Dropdown Menu
            Box {
                Row(
                    modifier = Modifier
                        .clickable { albumMenuExpanded = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Lucide.Folder,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = selectedAlbum.take(12),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Lucide.ChevronDown,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }

                DropdownMenu(
                    expanded = albumMenuExpanded,
                    onDismissRequest = { albumMenuExpanded = false },
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                ) {
                    albumsWithCounts.forEach { (album, count) ->
                        val isSelected = album == selectedAlbum
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (isSelected) Lucide.FolderOpen else Lucide.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF0A84FF) else colors.textMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = album,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0A84FF) else colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                }
                            },
                            onClick = {
                                onAlbumChange(album)
                                albumMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Media Type Filter Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clickable { filterMenuExpanded = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val filterIcon = when (mediaTypeFilter) {
                        GalleryMediaTypeFilter.ALL -> Lucide.Layers
                        GalleryMediaTypeFilter.PHOTOS -> Lucide.Image
                        GalleryMediaTypeFilter.VIDEOS -> Lucide.Film
                    }
                    Icon(
                        filterIcon,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = mediaTypeFilter.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    Icon(
                        Lucide.ChevronDown,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                ) {
                    GalleryMediaTypeFilter.values().forEach { filter ->
                        val isSelected = filter == mediaTypeFilter
                        val icon = when (filter) {
                            GalleryMediaTypeFilter.ALL -> Lucide.Layers
                            GalleryMediaTypeFilter.PHOTOS -> Lucide.Image
                            GalleryMediaTypeFilter.VIDEOS -> Lucide.Film
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF0A84FF) else colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = filter.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0A84FF) else colors.textMain
                                    )
                                }
                            },
                            onClick = {
                                onTypeFilterChange(filter)
                                filterMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Right side: Sort Dropdown & Column Density Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 3. Sort Order Dropdown
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Lucide.ArrowUpDown,
                        contentDescription = "Sort",
                        tint = colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                ) {
                    GallerySortOrder.values().forEach { sort ->
                        val isSelected = sort == sortOrder
                        val icon = when (sort) {
                            GallerySortOrder.NEWEST -> Lucide.Calendar
                            GallerySortOrder.OLDEST -> Lucide.Clock
                            GallerySortOrder.DURATION -> Lucide.Play
                            GallerySortOrder.NAME -> Lucide.FileText
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF0A84FF) else colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = sort.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0A84FF) else colors.textMain
                                    )
                                }
                            },
                            onClick = {
                                onSortOrderChange(sort)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 4. Grid Density Toggle
            IconButton(
                onClick = onToggleColumns,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    when (gridColumns) {
                        2 -> Lucide.Columns2
                        3 -> Lucide.Grid3x3
                        else -> Lucide.LayoutGrid
                    },
                    contentDescription = "Grid density: $gridColumns columns",
                    tint = colors.textMain,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Expandable search bar
 */
@Composable
private fun GallerySearchInputRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassBg)
            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Lucide.Search,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(14.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF0A84FF)),
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = colors.textMain
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Search media by name or folder...",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Clear",
                    tint = colors.textMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Camera tile slot at position 0 in the media grid
 */
@Composable
private fun GalleryCameraTile(
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cameraTileScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassBg)
            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A84FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Camera,
                    contentDescription = "Camera",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = "Camera",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain
            )
        }
    }
}

private val GALLERY_THUMB_SIZE = Size(240, 240)

/**
 * Media card for each photo/video thumbnail in the grid.
 * Displays ordered selection badges (1, 2, 3...) in Brand Blue (#0A84FF).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryMediaCard(
    item: GalleryMediaItem,
    isSelected: Boolean,
    selectionIndex: Int?,
    hasAnySelection: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickPreview: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    // Zero-latency hardware-accelerated image request
    val imageRequest = remember(item.uri, item.isVideo) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(GALLERY_THUMB_SIZE)
            .scale(Scale.FILL)
            .apply {
                if (item.isVideo) {
                    videoFrameMillis(1000L)
                }
            }
            .crossfade(false)
            .allowRgb565(true)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey("thumb_${item.uri}")
            .diskCacheKey("thumb_${item.uri}")
            .build()
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "thumbScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) Color(0xFF0A84FF) else colors.glassBorder,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Duration Indicator (Border-less glass pill)
        if (item.isVideo) {
            val totalSec = item.durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val durationText = "%d:%02d".format(min, sec)

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    Lucide.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
                Text(
                    text = durationText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        // Selection Badge: Brand Blue (#0A84FF) with ordered number or ready-ring
        if (isSelected) {
            // Subtle blue tint wash over selected image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A84FF).copy(alpha = 0.18f))
            )

            // Numbered badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A84FF))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selectionIndex != null) {
                    Text(
                        text = "$selectionIndex",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Lucide.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        } else if (hasAnySelection) {
            // Unselected indicator in multi-selection mode
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
}

/**
 * Samsung One UI 7 Floating Multi-Select Action Dock at the bottom
 */
@Composable
private fun GalleryFloatingSelectionDock(
    selectedUris: List<Uri>,
    maxSelection: Int,
    showCaptionInput: Boolean,
    captionText: String,
    onCaptionChange: (String) -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onClear: () -> Unit,
    onRemoveUri: (Uri) -> Unit,
    onSubmit: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(20.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(22.dp),
        color = if (colors.isDark) Color(0xF01C1C1E) else Color(0xF4FFFFFF),
        border = BorderStroke(0.5.dp, colors.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Count Badge, Select All, Clear Button
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
                        Lucide.Check,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${selectedUris.size}/$maxSelection selected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onSelectAll != null && selectedUris.size < maxSelection) {
                        Text(
                            text = "Select All",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0A84FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectAll() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Lucide.Trash2,
                            contentDescription = "Clear",
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Thumbnail Strip with ordered badges and single-tap remove X
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(selectedUris) { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF0A84FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        val context = LocalContext.current
                        val dockThumbRequest = remember(uri) {
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(Size(120, 120))
                                .scale(Scale.FILL)
                                .crossfade(false)
                                .allowRgb565(true)
                                .allowHardware(true)
                                .memoryCacheKey("dock_${uri}")
                                .build()
                        }
                        AsyncImage(
                            model = dockThumbRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Order number badge on top-start
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A84FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Remove X button on top-end
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { onRemoveUri(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.X,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }

            // Optional Caption Input
            if (showCaptionInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicTextField(
                        value = captionText,
                        onValueChange = onCaptionChange,
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF0A84FF)),
                        textStyle = TextStyle(fontSize = 12.5.sp, color = colors.textMain),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (captionText.isEmpty()) {
                                Text(
                                    "Write a caption...",
                                    fontSize = 12.5.sp,
                                    color = colors.textMuted
                                )
                            }
                            inner()
                        }
                    )
                    if (captionText.isNotEmpty()) {
                        IconButton(
                            onClick = { onCaptionChange("") },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Lucide.X, null, tint = colors.textMuted, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            // Vibrant Brand Blue Action Button
            LiquidGlassPrimaryButton(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Icon(
                    Lucide.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (showCaptionInput) "Send (${selectedUris.size})" else "Confirm (${selectedUris.size})",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Slide-Up Samsung Media Details Sheet in Lightbox
 */
@Composable
private fun SamsungMediaDetailsSheet(
    item: GalleryMediaItem,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = Color(0xFF1E1E22).copy(alpha = 0.98f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Details",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // File Name
            Column {
                Text(
                    "FILE NAME",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.name,
                    fontSize = 13.5.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // Date & Time
            Column {
                Text(
                    "DATE & TIME",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.formattedDate,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Resolution, Megapixels & Size Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (item.width > 0 && item.height > 0) {
                    val mp = (item.width.toLong() * item.height.toLong()) / 1_000_000.0
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RESOLUTION",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${item.width} × ${item.height} (${"%.1f MP".format(mp)})",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FILE SIZE",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.formattedSize,
                        fontSize = 13.sp,
                        color = Color(0xFF0A84FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Storage Path
            Column {
                Text(
                    "STORAGE LOCATION",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Internal storage > DCIM > ${item.bucketName}",
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Format
            Column {
                Text(
                    "FORMAT",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (item.isVideo) "Video / MP4" else item.mimeType.substringAfterLast("/").uppercase(),
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * Modern Lightbox Multi-Item Swipe & Zoom Dialog with Details (ⓘ) sheet and Favorite heart
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryLightboxDialog(
    mediaList: List<GalleryMediaItem>,
    initialIndex: Int,
    selectedUris: List<Uri>,
    maxSelection: Int,
    onToggleSelect: (GalleryMediaItem) -> Unit,
    onDirectSelect: (GalleryMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
    ) { mediaList.size }

    var showInfoSheet by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val currentItem = mediaList.getOrNull(pagerState.currentPage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Index, Title, Favorite, Info, Share, Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${mediaList.size}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentItem?.name ?: "Media",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Favorite Heart toggle
                        IconButton(
                            onClick = {
                                SoundManager.playTap(context)
                                isFavorite = !isFavorite
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.Heart,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF2D55) else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Info Button (toggles Samsung Details sheet)
                        IconButton(
                            onClick = { showInfoSheet = !showInfoSheet },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.Info,
                                contentDescription = "Info",
                                tint = if (showInfoSheet) Color(0xFF0A84FF) else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Share Button
                        IconButton(
                            onClick = {
                                currentItem?.let { item ->
                                    coroutineScope.launch {
                                        MediaStoreHelper.shareMedia(context, item.uri.toString())
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.Share2,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Lucide.X,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // HorizontalPager for swipeable full-screen browsing
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val item = mediaList.getOrNull(page)
                    if (item != null) {
                        var zoomScale by remember { mutableFloatStateOf(1f) }
                        var zoomOffset by remember { mutableStateOf(Offset.Zero) }

                        val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                            zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4.5f)
                            if (zoomScale > 1f) {
                                zoomOffset += offsetChange
                            } else {
                                zoomOffset = Offset.Zero
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            zoomScale = if (zoomScale > 1.2f) 1f else 2.5f
                                            zoomOffset = Offset.Zero
                                        }
                                    )
                                }
                                .transformable(state = transformableState),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        translationX = zoomOffset.x
                                        translationY = zoomOffset.y
                                    }
                            )

                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Lucide.Play,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Slide-Up Samsung Media Details Sheet
                AnimatedVisibility(
                    visible = showInfoSheet && currentItem != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    currentItem?.let { item ->
                        SamsungMediaDetailsSheet(
                            item = item,
                            onDismiss = { showInfoSheet = false }
                        )
                    }
                }

                // Bottom Thumbnail Scrubber Strip
                if (mediaList.size > 1) {
                    val scrubberListState = rememberLazyListState()
                    LaunchedEffect(pagerState.currentPage) {
                        scrubberListState.animateScrollToItem(
                            (pagerState.currentPage - 2).coerceAtLeast(0)
                        )
                    }

                    LazyRow(
                        state = scrubberListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(mediaList) { idx, thumbItem ->
                            val isCurrent = idx == pagerState.currentPage
                            val isItemSel = selectedUris.contains(thumbItem.uri)
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 44.dp else 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isCurrent) 2.dp else if (isItemSel) 1.dp else 0.dp,
                                        color = if (isCurrent) Color(0xFF0A84FF) else if (isItemSel) Color(0xFF34C759) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(idx)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(thumbItem.uri)
                                        .size(Size(100, 100))
                                        .crossfade(false)
                                        .allowRgb565(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Bottom Action Row
                currentItem?.let { item ->
                    val isSelected = selectedUris.contains(item.uri)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onToggleSelect(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Lucide.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSelected) "Selected" else "Select",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { onDirectSelect(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Lucide.ArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Concise permission card
 */
@Composable
private fun GalleryPermissionCard(
    onRequest: () -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Lucide.ShieldCheck,
                contentDescription = null,
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = "Media Access",
                color = colors.textMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Grant access to browse library",
                color = colors.textMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            LiquidGlassPrimaryButton(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(
                    text = "Allow Access",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }

            OutlinedButton(
                onClick = onOpenSystemPicker,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, colors.glassBorder),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(
                    Lucide.FolderOpen,
                    contentDescription = null,
                    tint = colors.textMain,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "System",
                    color = colors.textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Empty state component
 */
@Composable
private fun GalleryEmptyState(
    onOpenSystemPicker: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Lucide.Image,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = "No Media",
                color = colors.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Try changing filters",
                color = colors.textMuted,
                fontSize = 12.5.sp
            )

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = onOpenSystemPicker,
                colors = ButtonDefaults.buttonColors(containerColor = colors.glassBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, colors.glassBorder)
            ) {
                Icon(
                    Lucide.FolderOpen,
                    contentDescription = null,
                    tint = colors.textMain,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "System",
                    color = colors.textMain,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
