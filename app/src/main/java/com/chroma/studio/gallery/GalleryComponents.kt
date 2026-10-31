package com.chroma.studio.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// =============================================================================================
// Small building blocks
// =============================================================================================

@Composable
fun GalleryPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.RowScope.(contentColor: Color) -> Unit,
) {
    val c = Gallery.colors
    val fg = if (selected) c.surface else c.text
    Row(
        modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) c.text else c.chip)
            .border(1.dp, if (selected) c.text else c.hairline, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content(fg) }
}

@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    fontSize: Int = 13,
    requestFocus: Boolean = false,
    background: Color = Gallery.colors.elevated,
) {
    val c = Gallery.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) { if (requestFocus) runCatching { focus.requestFocus() } }

    Row(
        modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, c.hairline, CircleShape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, tint = c.primary, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = fontSize.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = fontSize.sp),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(c.muted.copy(alpha = 0.25f))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Clear", tint = c.muted, modifier = Modifier.size(12.dp)) }
        }
    }
}

// =============================================================================================
// Controls bar + chips + search
// =============================================================================================

@Composable
fun ControlsBar(
    filters: GalleryFilters,
    albums: List<Album>,
    columns: Int,
    onAlbum: (String) -> Unit,
    onType: (MediaTypeFilter) -> Unit,
    onSort: (SortOrder) -> Unit,
    onCycleColumns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Gallery.colors
    var albumMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                GalleryPill(onClick = { albumMenu = true }) { fg ->
                    Icon(Icons.Rounded.Folder, null, tint = c.primary, modifier = Modifier.size(14.dp))
                    Text(filters.album, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 90.dp))
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.muted, modifier = Modifier.size(14.dp))
                }
                GalleryMenu(albumMenu, { albumMenu = false }) {
                    albums.forEach { album ->
                        val sel = album.name == filters.album
                        MenuRow(album.name, sel, if (sel) Icons.Rounded.FolderOpen else Icons.Rounded.Folder, "${album.count}") {
                            onAlbum(album.name); albumMenu = false
                        }
                    }
                }
            }
            Box {
                GalleryPill(onClick = { typeMenu = true }) { fg ->
                    Icon(typeIcon(filters.type), null, tint = c.primary, modifier = Modifier.size(14.dp))
                    Text(filters.type.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.muted, modifier = Modifier.size(14.dp))
                }
                GalleryMenu(typeMenu, { typeMenu = false }) {
                    MediaTypeFilter.entries.forEach { t ->
                        MenuRow(t.label, t == filters.type, typeIcon(t)) { onType(t); typeMenu = false }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                GalleryPill(onClick = { sortMenu = true }) { fg ->
                    Icon(Icons.Rounded.SwapVert, null, tint = c.muted, modifier = Modifier.size(15.dp))
                    Text(filters.sort.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
                GalleryMenu(sortMenu, { sortMenu = false }) {
                    SortOrder.entries.forEach { s ->
                        MenuRow(s.label, s == filters.sort, sortIcon(s)) { onSort(s); sortMenu = false }
                    }
                }
            }
            GalleryPill(onClick = onCycleColumns) { fg ->
                Icon(Icons.Rounded.GridView, null, tint = c.primary, modifier = Modifier.size(14.dp))
                Text("${columns}x", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
        }
    }
}

private fun typeIcon(t: MediaTypeFilter) = when (t) {
    MediaTypeFilter.All -> Icons.Rounded.Layers
    MediaTypeFilter.Photos -> Icons.Rounded.Image
    MediaTypeFilter.Videos -> Icons.Rounded.Movie
}

private fun sortIcon(s: SortOrder) = when (s) {
    SortOrder.Newest -> Icons.Rounded.CalendarMonth
    SortOrder.Oldest -> Icons.Rounded.Schedule
    SortOrder.Duration -> Icons.Rounded.PlayArrow
    SortOrder.Name -> Icons.Rounded.Description
    SortOrder.Size -> Icons.Rounded.Download
}

@Composable
private fun GalleryMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = Gallery.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .border(1.dp, c.hairline, RoundedCornerShape(18.dp)),
        content = content,
    )
}

@Composable
private fun MenuRow(label: String, selected: Boolean, icon: ImageVector, trailing: String? = null, onClick: () -> Unit) {
    val c = Gallery.colors
    DropdownMenuItem(
        text = {
            Text(label, fontSize = 13.sp, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        },
        leadingIcon = { Icon(icon, null, tint = if (selected) c.primary else c.muted, modifier = Modifier.size(16.dp)) },
        trailingIcon = if (trailing != null) {
            { Text(trailing, fontSize = 11.sp, color = c.muted) }
        } else null,
        onClick = onClick,
    )
}

data class QuickChip(val id: String, val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun QuickChipsRow(chips: List<QuickChip>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(chips, key = { it.id }) { chip ->
            GalleryPill(onClick = chip.onClick, selected = chip.selected) { fg ->
                Icon(chip.icon, null, tint = fg, modifier = Modifier.size(12.dp))
                Text(chip.label, fontSize = 11.5.sp, fontWeight = if (chip.selected) FontWeight.Bold else FontWeight.Medium, color = fg)
            }
        }
    }
}

@Composable
fun SearchRow(visible: Boolean, query: String, onQuery: (String) -> Unit) {
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        PillTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = "Search photos, albums, monthsâ€¦",
            leadingIcon = Icons.Rounded.Search,
            requestFocus = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        )
    }
}

// =============================================================================================
// Grid items
// =============================================================================================

@Composable
fun SectionHeader(title: String, keys: List<String>, selection: SelectionState, onToggle: () -> Unit) {
    val c = Gallery.colors
    val allSelected = keys.isNotEmpty() && keys.all { it in selection.set }
    Row(
        Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.text)
        Text(
            if (allSelected) "Deselect" else "Select",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.primary,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onToggle).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun CameraTile(onClick: () -> Unit) {
    val c = Gallery.colors
    Column(
        Modifier.aspectRatio(1f).background(c.elevated).clickable(onClick = onClick)
            .semantics { contentDescription = "Open camera" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(c.surface).border(1.dp, c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.CameraAlt, null, tint = c.text, modifier = Modifier.size(18.dp)) }
        Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun thumbRequest(item: MediaItem, sizePx: Int = 360): ImageRequest {
    val context = LocalContext.current
    return remember(item.uri, sizePx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(sizePx)
            .videoFrameMillis(1000)
            .crossfade(true)
            .build()
    }
}

@Composable
fun MediaTile(
    item: MediaItem,
    number: Int,
    anySelected: Boolean,
    favorite: Boolean,
    showPreviewButton: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onPreview: () -> Unit,
) {
    val c = Gallery.colors
    val selected = number > 0
    val scale by animateFloatAsState(
        if (selected) 0.86f else 1f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "tileScale",
    )

    Box(
        Modifier
            .aspectRatio(1f)
            .background(c.elevated)
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = (if (item.isVideo) "Video " else "Photo ") + item.name
            },
    ) {
        AsyncImage(
            model = thumbRequest(item),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
        )

        if (favorite) {
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
        }

        if (item.isVideo) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Text(formatDuration(item.durationMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (showPreviewButton) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onPreview),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.OpenInFull, "Preview", tint = Color.White, modifier = Modifier.size(12.dp)) }
        }

        if (selected) {
            Box(Modifier.fillMaxSize().border(2.5.dp, c.primary).background(Color.Black.copy(alpha = 0.12f)))
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(c.primary)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$number", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        } else if (anySelected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)).border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
            )
        }
    }
}

// =============================================================================================
// Floating multi-select dock
// =============================================================================================

@Composable
fun SelectionDock(
    visible: Boolean,
    selection: SelectionState,
    allByKey: Map<String, MediaItem>,
    maxSelection: Int,
    showCaption: Boolean,
    caption: String,
    imageLoader: ImageLoader,
    onCaption: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onRemove: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Gallery.colors
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically { it } + fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(c.dock)
                .border(1.dp, c.hairline, RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Check, null, tint = c.primary, modifier = Modifier.size(16.dp))
                    Text("${selection.size}/$maxSelection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.text)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selection.size < maxSelection) {
                        Text(
                            "Select All", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = c.primary,
                            modifier = Modifier.clip(CircleShape).background(c.primary.copy(alpha = 0.12f))
                                .clickable(onClick = onSelectAll).padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClear),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.ClearAll, "Clear selection", tint = c.muted, modifier = Modifier.size(18.dp)) }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selection.ids, key = { it }) { key ->
                    val item = allByKey[key]
                    Box(Modifier.size(44.dp).background(c.elevated)) {
                        if (item != null) {
                            AsyncImage(
                                model = thumbRequest(item, 160),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(2.dp).size(16.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f)).clickable { onRemove(key) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(10.dp)) }
                    }
                }
            }

            if (showCaption) {
                PillTextField(
                    value = caption,
                    onValueChange = onCaption,
                    placeholder = "Add a captionâ€¦",
                    background = c.background,
                    fontSize = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                Modifier.fillMaxWidth().height(42.dp).clip(CircleShape).background(c.primary).clickable(onClick = onConfirm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("  Confirm (${selection.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// =============================================================================================
// Fast scroller (drag the thumb, month bubble follows)
// =============================================================================================

@Composable
fun BoxScope.FastScroller(state: LazyGridState, entries: List<GridEntry>, topInset: androidx.compose.ui.unit.Dp = 0.dp) {
    val c = Gallery.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbH = with(density) { 48.dp.toPx() }
    var trackH by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val total = entries.size

    LaunchedEffect(state.isScrollInProgress, dragging) {
        if (state.isScrollInProgress || dragging) shown = true else { delay(1200); shown = false }
    }

    val scrollable by remember(total) {
        derivedStateOf { max(0, total - state.layoutInfo.visibleItemsInfo.size) }
    }
    val fraction by remember(total) {
        derivedStateOf { if (scrollable == 0) 0f else (state.firstVisibleItemIndex.toFloat() / scrollable).coerceIn(0f, 1f) }
    }
    val bubbleLabel by remember(entries) {
        derivedStateOf {
            var i = state.firstVisibleItemIndex.coerceAtMost((entries.size - 1).coerceAtLeast(0))
            while (i >= 0 && entries.getOrNull(i) !is GridEntry.Header) i--
            (entries.getOrNull(i) as? GridEntry.Header)?.title
        }
    }

    if (total < 40) return

    Box(
        Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(36.dp)
            .padding(top = topInset)
            .onSizeChanged { trackH = it.height }
            .then(
                if (shown) Modifier.pointerInput(total) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        val span = (trackH - thumbH).coerceAtLeast(1f)
                        val f = ((change.position.y - thumbH / 2f) / span).coerceIn(0f, 1f)
                        scope.launch { state.scrollToItem((f * scrollable).roundToInt()) }
                    }
                } else Modifier
            ),
    ) {
        AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                val y = (fraction * (trackH - thumbH)).coerceAtLeast(0f)
                Box(
                    Modifier
                        .offset { IntOffset(0, y.roundToInt()) }
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                        .width(if (dragging) 10.dp else 6.dp)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (dragging) c.primary else c.muted.copy(alpha = 0.6f)),
                )
                if (dragging && bubbleLabel != null) {
                    Text(
                        bubbleLabel!!,
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .offset { IntOffset(-with(density) { 52.dp.roundToPx() }, y.roundToInt() + (thumbH / 2 - with(density) { 14.dp.toPx() }).roundToInt()) }
                            .align(Alignment.TopEnd)
                            .clip(CircleShape).background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================================
// States: loading / empty / permission / partial access / toast
// =============================================================================================

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Gallery.colors.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun EmptyState(onReset: () -> Unit, modifier: Modifier = Modifier) {
    val c = Gallery.colors
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.PhotoLibrary, null, tint = c.muted.copy(alpha = 0.7f), modifier = Modifier.size(44.dp))
        Text("No Media", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.text, modifier = Modifier.padding(top = 10.dp))
        Text("Try changing filters or the search query", fontSize = 12.5.sp, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Row(
            Modifier.padding(top = 14.dp).clip(CircleShape).background(c.elevated).border(1.dp, c.hairline, CircleShape)
                .clickable(onClick = onReset).padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Refresh, null, tint = c.text, modifier = Modifier.size(14.dp))
            Text("Reset Filters", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.text)
        }
    }
}

@Composable
fun PermissionCard(onAllow: () -> Unit, onSystemPicker: () -> Unit, modifier: Modifier = Modifier) {
    val c = Gallery.colors
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lock, null, tint = c.primary, modifier = Modifier.size(28.dp))
        }
        Text("Media Access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.text, modifier = Modifier.padding(top = 12.dp))
        Text("Allow access to browse your photos and videos.", fontSize = 13.sp, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Row(
            Modifier.padding(top = 20.dp).fillMaxWidth(0.7f).height(44.dp).clip(CircleShape).background(c.primary).clickable(onClick = onAllow),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) { Text("Allow Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth(0.7f).height(44.dp).clip(CircleShape).background(c.elevated)
                .border(1.dp, c.hairline, CircleShape).clickable(onClick = onSystemPicker),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.FolderOpen, null, tint = c.text, modifier = Modifier.size(16.dp))
            Text("  System Picker", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

/** Android 14+ â€œSelect photosâ€ mode: tell the user they only shared a subset. */
@Composable
fun PartialAccessBanner(onManage: () -> Unit) {
    val c = Gallery.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp))
            .background(c.primary.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Limited access â€“ only selected items are shown", fontSize = 12.sp, color = c.text, modifier = Modifier.weight(1f))
        Text(
            "Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.primary,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onManage).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun GalleryToast(message: String?, modifier: Modifier = Modifier) {
    val c = Gallery.colors
    AnimatedVisibility(message != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically(), modifier = modifier) {
        Row(
            Modifier.padding(top = 12.dp).clip(RoundedCornerShape(18.dp)).background(c.dock)
                .border(1.dp, c.primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Text(message.orEmpty(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.text)
        }
    }
}
