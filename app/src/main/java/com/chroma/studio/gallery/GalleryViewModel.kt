package com.chroma.studio.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max
import kotlin.math.min

data class GalleryFilters(
    val type: MediaTypeFilter = MediaTypeFilter.All,
    val album: String = ALL_ALBUMS,
    val favoritesOnly: Boolean = false,
    val sort: SortOrder = SortOrder.Newest,
    val query: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val prefs = GalleryPrefs(app)

    // ---- configuration pushed from the composable ---------------------------------------
    private var maxSelection = 10
    private val cameraTileEnabled = MutableStateFlow(true)

    fun configure(maxSelection: Int, cameraTile: Boolean) {
        this.maxSelection = maxSelection
        cameraTileEnabled.value = cameraTile
    }

    // ---- UI state -------------------------------------------------------------------------
    private val accessState = MutableStateFlow(MediaAccess.Denied)
    private val loading = MutableStateFlow(true)

    private val _filters = MutableStateFlow(GalleryFilters(sort = prefs.sort))
    val filters: StateFlow<GalleryFilters> = _filters.asStateFlow()

    private val _columns = MutableStateFlow(prefs.columns)
    val columns: StateFlow<Int> = _columns.asStateFlow()

    private val _searchVisible = MutableStateFlow(false)
    val searchVisible: StateFlow<Boolean> = _searchVisible.asStateFlow()

    private val _selection = MutableStateFlow(SelectionState())
    val selection: StateFlow<SelectionState> = _selection.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    val favorites: StateFlow<Set<String>> = prefs.favorites.asStateFlow()

    private val _events = MutableSharedFlow<GalleryEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GalleryEvent> = _events.asSharedFlow()

    private val media = accessState.flatMapLatest { access ->
        if (access == MediaAccess.Denied) {
            flowOf(emptyList<MediaItem>()).onEach { loading.value = false }
        } else {
            repository.observeMedia()
                .onStart { loading.value = true }
                .onEach { loading.value = false }
        }
    }

    private class Inputs(
        val media: List<MediaItem>,
        val favorites: Set<String>,
        val filters: GalleryFilters,
        val camera: Boolean,
    )

    /** Everything the grid needs, recomputed off the main thread whenever media/filters/favorites change. */
    val content: StateFlow<ContentState> =
        combine(media, prefs.favorites, _filters, cameraTileEnabled) { m, f, fl, cam -> Inputs(m, f, fl, cam) }
            .mapLatest { compute(it) }
            .combine(loading) { state, isLoading -> state.copy(isLoading = isLoading) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentState())

    private fun compute(input: Inputs): ContentState {
        val f = input.filters
        var list = input.media

        if (f.favoritesOnly) list = list.filter { it.key in input.favorites }
        list = when (f.type) {
            MediaTypeFilter.Photos -> list.filter { !it.isVideo }
            MediaTypeFilter.Videos -> list.filter { it.isVideo }
            MediaTypeFilter.All -> list
        }
        if (f.album != ALL_ALBUMS) list = list.filter { it.bucketName.equals(f.album, ignoreCase = true) }
        val q = f.query.trim()
        if (q.isNotEmpty()) {
            list = list.filter {
                it.name.contains(q, true) || it.bucketName.contains(q, true) || it.monthKey.contains(q, true)
            }
        }
        list = when (f.sort) {
            SortOrder.Newest -> list.sortedByDescending { it.dateMs }
            SortOrder.Oldest -> list.sortedBy { it.dateMs }
            SortOrder.Duration -> list.sortedByDescending { it.durationMs }
            SortOrder.Name -> list.sortedBy { it.name.lowercase() }
            SortOrder.Size -> list.sortedByDescending { it.sizeBytes }
        }

        val showCamera = input.camera && f.album == ALL_ALBUMS && !f.favoritesOnly &&
            f.type == MediaTypeFilter.All && q.isEmpty()
        val grouped = f.sort == SortOrder.Newest || f.sort == SortOrder.Oldest

        val entries = ArrayList<GridEntry>(list.size + 32)
        if (grouped) {
            var i = 0
            var cameraAdded = !showCamera
            while (i < list.size) {
                val month = list[i].monthKey
                var j = i
                while (j < list.size && list[j].monthKey == month) j++
                entries += GridEntry.Header(month, list.subList(i, j).map { it.key })
                if (!cameraAdded) {
                    entries += GridEntry.Camera
                    cameraAdded = true
                }
                for (k in i until j) entries += GridEntry.Media(list[k])
                i = j
            }
            if (!cameraAdded) entries += GridEntry.Camera
        } else {
            if (showCamera) entries += GridEntry.Camera
            list.forEach { entries += GridEntry.Media(it) }
        }

        val albums = input.media.groupingBy { it.bucketName }.eachCount()
            .map { Album(it.key, it.value) }
            .sortedByDescending { it.count }

        return ContentState(
            isLoading = false,
            albums = listOf(Album(ALL_ALBUMS, input.media.size)) + albums,
            visible = list,
            entries = entries,
            indexByKey = list.withIndex().associate { it.value.key to it.index },
            allByKey = input.media.associateBy { it.key },
            photoCount = list.count { !it.isVideo },
            videoCount = list.count { it.isVideo },
        )
    }

    // ---- permission -----------------------------------------------------------------------
    fun setAccess(access: MediaAccess) {
        accessState.value = access
    }

    // ---- filters --------------------------------------------------------------------------
    fun setAlbum(album: String) = _filters.update { it.copy(album = album, favoritesOnly = false) }
    fun setType(type: MediaTypeFilter) = _filters.update { it.copy(type = type) }
    fun setSort(sort: SortOrder) {
        prefs.sort = sort
        _filters.update { it.copy(sort = sort) }
    }
    fun setFavoritesOnly(on: Boolean) = _filters.update { it.copy(favoritesOnly = on) }
    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun resetFilters() {
        _filters.update { GalleryFilters(sort = it.sort) }
        _searchVisible.value = false
    }

    fun toggleSearch() {
        _searchVisible.value = !_searchVisible.value
        if (!_searchVisible.value) setQuery("")
    }
    fun closeSearch() {
        _searchVisible.value = false
        setQuery("")
    }

    fun setColumns(count: Int) {
        val c = count.coerceIn(2, 5)
        prefs.columns = c
        _columns.value = c
    }

    private inline fun MutableStateFlow<GalleryFilters>.update(block: (GalleryFilters) -> GalleryFilters) {
        value = block(value)
    }

    // ---- favourites -----------------------------------------------------------------------
    fun toggleFavorite(key: String) = prefs.toggleFavorite(key)

    // ---- selection ------------------------------------------------------------------------
    fun toggleSelection(key: String) {
        val cur = _selection.value.ids
        when {
            key in cur -> _selection.value = SelectionState(cur - key)
            cur.size >= maxSelection -> _events.tryEmit(GalleryEvent.LimitReached(maxSelection))
            else -> _selection.value = SelectionState(cur + key)
        }
    }

    fun ensureSelected(key: String) {
        val cur = _selection.value.ids
        if (cur.isEmpty()) _selection.value = SelectionState(listOf(key))
    }

    /** Select (or deselect, if all already selected) a whole month section. */
    fun toggleGroup(keys: List<String>) {
        val cur = _selection.value
        if (keys.all { it in cur.set }) {
            val drop = keys.toSet()
            _selection.value = SelectionState(cur.ids.filterNot { it in drop })
        } else {
            val room = maxSelection - cur.size
            val toAdd = keys.filter { it !in cur.set }
            _selection.value = SelectionState(cur.ids + toAdd.take(room.coerceAtLeast(0)))
            if (toAdd.size > room) _events.tryEmit(GalleryEvent.LimitReached(maxSelection))
        }
    }

    fun selectAllVisible() {
        val cur = _selection.value
        val room = maxSelection - cur.size
        val toAdd = content.value.visible.map { it.key }.filter { it !in cur.set }
        _selection.value = SelectionState(cur.ids + toAdd.take(room.coerceAtLeast(0)))
        if (toAdd.size > room) _events.tryEmit(GalleryEvent.LimitReached(maxSelection))
    }

    fun clearSelection() {
        _selection.value = SelectionState()
        _caption.value = ""
    }

    fun removeSelected(key: String) {
        _selection.value = SelectionState(_selection.value.ids - key)
    }

    fun canSelectMore(): Boolean = _selection.value.size < maxSelection

    fun setCaption(text: String) {
        _caption.value = text
    }

    fun selectedItems(): List<MediaItem> {
        val map = content.value.allByKey
        return _selection.value.ids.mapNotNull { map[it] }
    }

    // ---- drag-to-select (long-press a tile, then slide across the grid) ---------------------
    private var dragAnchor = -1
    private var dragBase: List<String> = emptyList()
    private var dragSelecting = true

    fun onDragStart(key: String) {
        val index = content.value.indexByKey[key] ?: return
        dragAnchor = index
        dragBase = _selection.value.ids
        dragSelecting = key !in dragBase
        applyDragRange(index)
    }

    fun onDragOver(key: String) {
        if (dragAnchor < 0) return
        val index = content.value.indexByKey[key] ?: return
        applyDragRange(index)
    }

    fun onDragEnd() {
        dragAnchor = -1
        dragBase = emptyList()
    }

    private fun applyDragRange(current: Int) {
        val visible = content.value.visible
        if (visible.isEmpty()) return
        val lo = min(dragAnchor, current).coerceIn(0, visible.lastIndex)
        val hi = max(dragAnchor, current).coerceIn(0, visible.lastIndex)
        val range = visible.subList(lo, hi + 1).map { it.key }
        val next = if (dragSelecting) {
            val base = dragBase.toSet()
            val merged = dragBase + range.filter { it !in base }
            if (merged.size > maxSelection) {
                _events.tryEmit(GalleryEvent.LimitReached(maxSelection))
                merged.take(maxSelection)
            } else merged
        } else {
            val drop = range.toSet()
            dragBase.filterNot { it in drop }
        }
        if (next != _selection.value.ids) _selection.value = SelectionState(next)
    }

    // ---- camera tile ----------------------------------------------------------------------
    fun createCameraUri(): Uri? = repository.createCameraUri()
    fun finalizeCameraUri(uri: Uri) = repository.finalizeCameraUri(uri)
    fun discardCameraUri(uri: Uri) = repository.discardCameraUri(uri)
}
