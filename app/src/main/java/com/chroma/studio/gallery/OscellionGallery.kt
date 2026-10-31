package com.chroma.studio.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drop-in media picker / gallery.
 *
 * ```
 * OscellionGallery(
 *     mode = GalleryLayoutMode.BottomSheet,
 *     maxSelection = 10,
 *     onConfirm = { items, caption -> /* send items */ },
 *     onClose = { /* hide gallery */ },
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OscellionGallery(
    modifier: Modifier = Modifier,
    mode: GalleryLayoutMode = GalleryLayoutMode.FullScreen,
    maxSelection: Int = 10,
    showCaptionInput: Boolean = true,
    showCameraTile: Boolean = true,
    title: String = "Gallery",
    darkTheme: Boolean = isSystemInDarkTheme(),
    soundEffects: Boolean = false,
    onConfirm: (items: List<MediaItem>, caption: String) -> Unit,
    onClose: () -> Unit,
    onSystemPicked: (List<Uri>) -> Unit = {},
    viewModel: GalleryViewModel = viewModel(),
) {
    OscellionTheme(darkTheme) {
        val colors = Gallery.colors
        when (mode) {
            GalleryLayoutMode.FullScreen -> Box(modifier.fillMaxSize().background(colors.surface)) {
                GalleryContent(
                    vm = viewModel, isSheet = false, maxSelection = maxSelection, showCaptionInput = showCaptionInput,
                    showCameraTile = showCameraTile, title = title, soundEffects = soundEffects,
                    onConfirm = onConfirm, onClose = onClose, onSystemPicked = onSystemPicked,
                )
            }

            GalleryLayoutMode.BottomSheet -> ModalBottomSheet(
                onDismissRequest = onClose,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                Box(modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                    GalleryContent(
                        vm = viewModel, isSheet = true, maxSelection = maxSelection, showCaptionInput = showCaptionInput,
                        showCameraTile = showCameraTile, title = title, soundEffects = soundEffects,
                        onConfirm = onConfirm, onClose = onClose, onSystemPicked = onSystemPicked,
                    )
                }
            }
        }
    }
}

@Composable
fun rememberGalleryImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
            .crossfade(true)
            .build()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun GalleryContent(
    vm: GalleryViewModel,
    isSheet: Boolean,
    maxSelection: Int,
    showCaptionInput: Boolean,
    showCameraTile: Boolean,
    title: String,
    soundEffects: Boolean,
    onConfirm: (List<MediaItem>, String) -> Unit,
    onClose: () -> Unit,
    onSystemPicked: (List<Uri>) -> Unit,
) {
    val c = Gallery.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val imageLoader = rememberGalleryImageLoader()

    // ---- state ---------------------------------------------------------------------------------
    val access = rememberMediaAccessController()
    LaunchedEffect(access.access) { vm.setAccess(access.access) }
    val cameraSupported = showCameraTile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    SideEffect { vm.configure(maxSelection, cameraSupported) }

    val content by vm.content.collectAsState()
    val selection by vm.selection.collectAsState()
    val filters by vm.filters.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val columns by vm.columns.collectAsState()
    val searchVisible by vm.searchVisible.collectAsState()
    val caption by vm.caption.collectAsState()

    var lightboxIndex by remember { mutableIntStateOf(-1) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is GalleryEvent.LimitReached -> {
                    toast = "You can select up to ${event.max} items"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2200); toast = null } }

    BackHandler(enabled = searchVisible || selection.isNotEmpty()) {
        if (searchVisible) vm.closeSearch() else vm.clearSelection()
    }

    // ---- launchers -----------------------------------------------------------------------------
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxSelection.coerceAtLeast(2)),
    ) { uris -> if (uris.isNotEmpty()) onSystemPicked(uris) }
    val openSystemPicker = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pendingCameraUri?.let { uri -> if (ok) vm.finalizeCameraUri(uri) else vm.discardCameraUri(uri) }
        pendingCameraUri = null
    }
    val launchCamera = {
        vm.createCameraUri()?.let { uri ->
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
        Unit
    }

    // ---- helpers -------------------------------------------------------------------------------
    fun tick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (soundEffects) view.playSoundEffect(SoundEffectConstants.CLICK)
    }
    fun confirm() {
        val items = vm.selectedItems()
        if (items.isNotEmpty()) onConfirm(items, if (showCaptionInput) caption else "")
    }

    // ---- header coordinator --------------------------------------------------------------------
    val density = LocalDensity.current
    val expandedHeight = 136.dp
    val headerState = remember(density) { CollapsingHeaderState(with(density) { expandedHeight.toPx() }) }

    val baseTitle = when {
        filters.favoritesOnly -> "Favorites"
        filters.album != ALL_ALBUMS -> filters.album
        else -> title
    }
    val subtitle = if (selection.isNotEmpty()) {
        val bytes = selection.ids.sumOf { content.allByKey[it]?.sizeBytes ?: 0L }
        "${formatSize(bytes)} total â€¢ Tap or drag to adjust"
    } else {
        buildList {
            if (content.photoCount > 0) add("${content.photoCount} photo${if (content.photoCount > 1) "s" else ""}")
            if (content.videoCount > 0) add("${content.videoCount} video${if (content.videoCount > 1) "s" else ""}")
        }.joinToString("  â€¢  ").ifEmpty { "No media items" }
    }

    Box(Modifier.fillMaxSize()) {
        if (access.access == MediaAccess.Denied) {
            Column(Modifier.fillMaxSize().then(if (!isSheet) Modifier.statusBarsPadding() else Modifier)) {
                PermissionCard(onAllow = access.request, onSystemPicker = openSystemPicker)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(headerState.connection)
                    .then(if (!isSheet) Modifier.statusBarsPadding() else Modifier),
            ) {
                GalleryHeader(
                    state = headerState,
                    expandedHeight = expandedHeight,
                    title = if (selection.isNotEmpty()) "${selection.size} selected" else baseTitle,
                    subtitle = subtitle,
                    countBadge = content.visible.size,
                    isSheet = isSheet,
                    onBack = onClose,
                    onToggleCollapse = { headerState.toggle() },
                    onPicker = openSystemPicker,
                    onSearch = vm::toggleSearch,
                )

                if (access.access == MediaAccess.Partial) PartialAccessBanner(onManage = access.request)

                ControlsBar(
                    filters = filters,
                    albums = content.albums,
                    columns = columns,
                    onAlbum = vm::setAlbum,
                    onType = vm::setType,
                    onSort = vm::setSort,
                    onCycleColumns = { vm.setColumns(if (columns >= 5) 2 else columns + 1) },
                )

                QuickChipsRow(
                    buildQuickChips(filters, content.albums, vm),
                    modifier = Modifier.height(36.dp),
                )

                SearchRow(searchVisible, filters.query, vm::setQuery)

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val gridState = rememberLazyGridState()

                    when {
                        content.isLoading -> LoadingState()
                        content.entries.none { it is GridEntry.Media } && content.entries.none { it is GridEntry.Camera } ->
                            EmptyState(onReset = vm::resetFilters)
                        else -> MediaGrid(
                            gridState = gridState,
                            content = content,
                            selection = selection,
                            favorites = favorites,
                            columns = columns,
                            imageLoader = imageLoader,
                            hasDock = selection.isNotEmpty(),
                            showCaption = showCaptionInput,
                            vm = vm,
                            onTileClick = { key -> vm.toggleSelection(key); tick() },
                            onPreview = { key -> lightboxIndex = content.indexByKey[key] ?: -1 },
                            onCamera = launchCamera,
                            onColumns = vm::setColumns,
                            haptic = { haptic.performHapticFeedback(it) },
                        )
                    }

                    FastScroller(gridState, content.entries)

                    SelectionDock(
                        visible = selection.isNotEmpty(),
                        selection = selection,
                        allByKey = content.allByKey,
                        maxSelection = maxSelection,
                        showCaption = showCaptionInput,
                        caption = caption,
                        imageLoader = imageLoader,
                        onCaption = vm::setCaption,
                        onSelectAll = vm::selectAllVisible,
                        onClear = vm::clearSelection,
                        onRemove = vm::removeSelected,
                        onConfirm = ::confirm,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        GalleryToast(toast, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }

    // ---- lightbox ------------------------------------------------------------------------------
    if (lightboxIndex in content.visible.indices) {
        GalleryLightbox(
            items = content.visible,
            startIndex = lightboxIndex,
            selection = selection,
            favorites = favorites,
            imageLoader = imageLoader,
            onToggleSelect = { vm.toggleSelection(it.key); tick() },
            onToggleFavorite = { vm.toggleFavorite(it.key); tick() },
            onShare = { shareMedia(context, it) },
            onNext = { item ->
                vm.ensureSelected(item.key)
                lightboxIndex = -1
                confirm()
            },
            onDismiss = { lightboxIndex = -1 },
        )
    }
}

// =================================================================================================
// Grid with drag-select, edge auto-scroll and pinch-to-change-density
// =================================================================================================

@Composable
private fun MediaGrid(
    gridState: LazyGridState,
    content: ContentState,
    selection: SelectionState,
    favorites: Set<String>,
    columns: Int,
    imageLoader: ImageLoader,
    hasDock: Boolean,
    showCaption: Boolean,
    vm: GalleryViewModel,
    onTileClick: (String) -> Unit,
    onPreview: (String) -> Unit,
    onCamera: () -> Unit,
    onColumns: (Int) -> Unit,
    haptic: (HapticFeedbackType) -> Unit,
) {
    val density = LocalDensity.current
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    val currentColumns by rememberUpdatedState(columns)
    val currentOnColumns by rememberUpdatedState(onColumns)

    // Edge auto-scroll while dragging a selection.
    LaunchedEffect(dragPos != null) {
        val edge = with(density) { 72.dp.toPx() }
        while (dragPos != null) {
            val p = dragPos ?: break
            val h = gridState.layoutInfo.viewportSize.height
            val delta = when {
                p.y < edge -> -((edge - p.y) / edge) * 42f
                p.y > h - edge -> ((p.y - (h - edge)) / edge) * 42f
                else -> 0f
            }
            if (delta != 0f) {
                gridState.scrollBy(delta)
                gridState.mediaKeyAt(p)?.let(vm::onDragOver)
            }
            delay(16)
        }
    }

    val bottomPad = if (hasDock) (if (showCaption) 250.dp else 200.dp) else 32.dp

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier
            .fillMaxSize()
            // Pinch with two fingers to change the grid density (2â€“5 columns).
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var zoom = 1f
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size >= 2) {
                            zoom *= event.calculateZoom()
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    when {
                        zoom > 1.3f -> currentOnColumns(currentColumns - 1)
                        zoom < 0.75f -> currentOnColumns(currentColumns + 1)
                    }
                }
            }
            // Long-press a tile, then slide across others to select (or deselect) a range.
            .pointerInput(Unit) {
                var active = false
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val key = gridState.mediaKeyAt(offset)
                        if (key != null) {
                            active = true
                            vm.onDragStart(key)
                            dragPos = offset
                            haptic(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, _ ->
                        if (active) {
                            change.consume()
                            dragPos = change.position
                            gridState.mediaKeyAt(change.position)?.let(vm::onDragOver)
                        }
                    },
                    onDragEnd = { active = false; dragPos = null; vm.onDragEnd() },
                    onDragCancel = { active = false; dragPos = null; vm.onDragEnd() },
                )
            },
    ) {
        items(
            items = content.entries,
            key = { it.key },
            span = { entry -> if (entry is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            contentType = { it::class },
        ) { entry ->
            when (entry) {
                is GridEntry.Header -> SectionHeader(entry.title, entry.mediaKeys, selection) { vm.toggleGroup(entry.mediaKeys) }
                GridEntry.Camera -> CameraTile(onCamera)
                is GridEntry.Media -> MediaTile(
                    item = entry.item,
                    number = selection.numberOf(entry.item.key),
                    anySelected = selection.isNotEmpty(),
                    favorite = entry.item.key in favorites,
                    showPreviewButton = columns <= 3,
                    imageLoader = imageLoader,
                    onClick = { onTileClick(entry.item.key) },
                    onPreview = { onPreview(entry.item.key) },
                )
            }
        }
    }
}

/** Returns the media key of the tile under [pos] (grid-local coordinates), or null for headers/camera/gaps. */
private fun LazyGridState.mediaKeyAt(pos: Offset): String? {
    val info = layoutInfo.visibleItemsInfo.firstOrNull {
        pos.x >= it.offset.x && pos.x < it.offset.x + it.size.width &&
            pos.y >= it.offset.y && pos.y < it.offset.y + it.size.height
    } ?: return null
    val key = info.key as? String ?: return null
    return if (key.startsWith("header_") || key == "camera_tile") null else key
}

private fun buildQuickChips(filters: GalleryFilters, albums: List<Album>, vm: GalleryViewModel): List<QuickChip> {
    val noExtras = filters.type == MediaTypeFilter.All && filters.album == ALL_ALBUMS && !filters.favoritesOnly
    val chips = mutableListOf(
        QuickChip("all", "All", Icons.Rounded.Layers, noExtras) {
            vm.setType(MediaTypeFilter.All); vm.setAlbum(ALL_ALBUMS); vm.setFavoritesOnly(false)
        },
        QuickChip("fav", "Favorites", Icons.Rounded.Favorite, filters.favoritesOnly) {
            vm.setAlbum(ALL_ALBUMS); vm.setType(MediaTypeFilter.All); vm.setFavoritesOnly(true)
        },
        QuickChip("videos", "Videos", Icons.Rounded.Movie, filters.type == MediaTypeFilter.Videos && filters.album == ALL_ALBUMS && !filters.favoritesOnly) {
            vm.setAlbum(ALL_ALBUMS); vm.setFavoritesOnly(false); vm.setType(MediaTypeFilter.Videos)
        },
        QuickChip("photos", "Photos", Icons.Rounded.Image, filters.type == MediaTypeFilter.Photos && filters.album == ALL_ALBUMS && !filters.favoritesOnly) {
            vm.setAlbum(ALL_ALBUMS); vm.setFavoritesOnly(false); vm.setType(MediaTypeFilter.Photos)
        },
    )
    // Top real albums on this device (Camera, Screenshots, Download, WhatsAppâ€¦) become one-tap chips.
    albums.filter { it.name != ALL_ALBUMS }.take(4).forEach { album ->
        val icon = if (album.name.equals("Camera", true)) Icons.Rounded.CameraAlt else Icons.Rounded.Folder
        chips += QuickChip("album_${album.name}", album.name, icon, filters.album == album.name) {
            vm.setFavoritesOnly(false); vm.setAlbum(album.name)
        }
    }
    return chips
}

private fun shareMedia(context: android.content.Context, item: MediaItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null)
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
