package com.chroma.studio.gallery

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val ALL_ALBUMS = "All"

enum class GalleryLayoutMode { FullScreen, BottomSheet }

enum class MediaTypeFilter(val label: String) { All("All"), Photos("Photos"), Videos("Videos") }

enum class SortOrder(val label: String) {
    Newest("Newest"), Oldest("Oldest"), Duration("Duration"), Name("Name"), Size("Size")
}

/** One photo or video. [key] is unique across images + videos (MediaStore ids overlap between tables). */
@Immutable
data class MediaItem(
    val key: String,
    val uri: Uri,
    val name: String,
    val bucketName: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val dateMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val monthKey: String,
)

@Immutable
data class Album(val name: String, val count: Int)

/** Ordered selection (order = number badge shown on the tile). */
@Immutable
class SelectionState(val ids: List<String> = emptyList()) {
    val set: Set<String> = ids.toSet()
    val size: Int get() = ids.size
    fun isEmpty() = ids.isEmpty()
    fun isNotEmpty() = ids.isNotEmpty()
    /** 1-based badge number, 0 when not selected. */
    fun numberOf(key: String): Int = if (key in set) ids.indexOf(key) + 1 else 0
}

sealed interface GridEntry {
    val key: String

    data object Camera : GridEntry {
        override val key = "camera_tile"
    }

    data class Header(val title: String, val mediaKeys: List<String>) : GridEntry {
        override val key get() = "header_$title"
    }

    data class Media(val item: MediaItem) : GridEntry {
        override val key get() = item.key
    }
}

@Immutable
data class ContentState(
    val isLoading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val visible: List<MediaItem> = emptyList(),
    val entries: List<GridEntry> = emptyList(),
    val indexByKey: Map<String, Int> = emptyMap(),
    val allByKey: Map<String, MediaItem> = emptyMap(),
    val photoCount: Int = 0,
    val videoCount: Int = 0,
)

sealed interface GalleryEvent {
    data class LimitReached(val max: Int) : GalleryEvent
}

// ---------------------------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------------------------

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1L shl 10 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy â€¢ HH:mm", Locale.getDefault())

fun MediaItem.formattedDate(): String =
    Instant.ofEpochMilli(dateMs).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
