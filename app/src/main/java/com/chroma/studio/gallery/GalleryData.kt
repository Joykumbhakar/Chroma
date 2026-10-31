package com.chroma.studio.gallery

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import android.content.ContentUris
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reads photos + videos from MediaStore and re-emits whenever the library changes
 * (new screenshot, deleted photo, partial-access grantâ€¦).
 */
class MediaRepository(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    @OptIn(FlowPreview::class)
    fun observeMedia(): Flow<List<MediaItem>> {
        val changes = callbackFlow<Unit> {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
            resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }.debounce(500)

        return merge(flowOf(Unit), changes)
            .map { loadAll() }
            .flowOn(Dispatchers.IO)
    }

    private fun loadAll(): List<MediaItem> =
        (query(isVideo = false) + query(isVideo = true)).sortedByDescending { it.dateMs }

    private fun query(isVideo: Boolean): List<MediaItem> {
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val dateTakenCol = if (isVideo) MediaStore.Video.Media.DATE_TAKEN else MediaStore.Images.Media.DATE_TAKEN
        val bucketCol = if (isVideo) MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        else MediaStore.Images.Media.BUCKET_DISPLAY_NAME

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            dateTakenCol,
            bucketCol,
        )
        if (isVideo) projection += MediaStore.Video.Media.DURATION

        val out = ArrayList<MediaItem>()
        try {
            resolver.query(
                collection,
                projection.toTypedArray(),
                "${MediaStore.MediaColumns.SIZE} > 0",
                null,
                "$dateTakenCol DESC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val iW = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val iH = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val iTaken = c.getColumnIndexOrThrow(dateTakenCol)
                val iBucket = c.getColumnIndexOrThrow(bucketCol)
                val iDur = if (isVideo) c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val taken = c.getLong(iTaken)
                    val date = if (taken > 0) taken else c.getLong(iAdded) * 1000L
                    out += MediaItem(
                        key = (if (isVideo) "v" else "i") + id,
                        uri = ContentUris.withAppendedId(collection, id),
                        name = c.getString(iName) ?: "Unknown",
                        bucketName = c.getString(iBucket) ?: "Other",
                        isVideo = isVideo,
                        durationMs = if (isVideo) c.getLong(iDur) else 0L,
                        dateMs = date,
                        sizeBytes = c.getLong(iSize),
                        width = c.getInt(iW),
                        height = c.getInt(iH),
                        mimeType = c.getString(iMime) ?: if (isVideo) "video/*" else "image/*",
                        monthKey = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(monthFormatter),
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked while loading â€“ UI will show the permission card on next resume.
        }
        return out
    }

    // ---- Camera tile support (API 29+) -----------------------------------------------------

    fun createCameraUri(): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "OSC_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun finalizeCameraUri(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        }
    }

    fun discardCameraUri(uri: Uri) {
        runCatching { resolver.delete(uri, null, null) }
    }
}

/** Tiny persisted settings: favourites, grid density and sort order. */
class GalleryPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("oscellion_gallery", Context.MODE_PRIVATE)

    val favorites = MutableStateFlow(sp.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())

    fun toggleFavorite(key: String) {
        val next = favorites.value.let { if (key in it) it - key else it + key }
        favorites.value = next
        sp.edit().putStringSet("favorites", next).apply()
    }

    var columns: Int
        get() = sp.getInt("columns", 3).coerceIn(2, 5)
        set(value) = sp.edit().putInt("columns", value).apply()

    var sort: SortOrder
        get() = runCatching { SortOrder.valueOf(sp.getString("sort", SortOrder.Newest.name)!!) }
            .getOrDefault(SortOrder.Newest)
        set(value) = sp.edit().putString("sort", value.name).apply()
}
