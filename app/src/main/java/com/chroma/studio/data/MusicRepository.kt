package com.chroma.studio.data

import android.content.Context
import android.util.Log
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.FlashVideo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class MusicMoodCollection(val id: String, val title: String) {
    MORNING_COFFEE("MORNING_COFFEE", "Morning Coffee"),
    GOLDEN_HOUR("GOLDEN_HOUR", "Golden Hour"),
    CITY_NIGHTS("CITY_NIGHTS", "City Nights"),
    WEEKEND_DRIVE("WEEKEND_DRIVE", "Weekend Drive");

    companion object {
        fun fromRaw(raw: String?): MusicMoodCollection {
            val clean = raw?.trim().orEmpty()
            return entries.firstOrNull {
                it.id.equals(clean, ignoreCase = true) ||
                    it.title.equals(clean, ignoreCase = true)
            } ?: CITY_NIGHTS
        }
    }
}

data class MusicLyricLine(
    val timeSec: Int = 0,
    val text: String = ""
)

data class CatalogMusicTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val description: String = "",
    val category: String = "Chill",
    val collection: MusicMoodCollection = MusicMoodCollection.CITY_NIGHTS,
    val coverUrl: String = "",
    val audioUrl: String = "",
    val durationSec: Int = 30,
    val bestPartStartSec: Int = 0,
    val bestPartEndSec: Int = 20,
    val isTrending: Boolean = false,
    val isExplicit: Boolean = false,
    val isFeatured: Boolean = false,
    val status: String = "active",
    val useCount: Int = 0,
    val lyrics: List<MusicLyricLine> = emptyList(),
    val saved: Boolean = false,
    val createdAt: Long = 0L
)

/**
 * Pure selection snapshot emitted by [StoryMusicPickerModal] across Flash, Posts, Stories, and Chat.
 */
data class AttachedMusicSelection(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val audioUrl: String,
    val startSec: Int,
    val durationSec: Int,
    val totalTrackSec: Int,
    val showLyricsSticker: Boolean = true
)

fun CatalogMusicTrack.toAttachedSelection(): AttachedMusicSelection {
    val safeStart = bestPartStartSec.coerceIn(0, maxOf(0, durationSec - 1))
    val clipLen = (bestPartEndSec - safeStart).coerceIn(5, maxOf(5, durationSec))
    return AttachedMusicSelection(
        trackId = id,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        startSec = safeStart,
        durationSec = clipLen,
        totalTrackSec = durationSec,
        showLyricsSticker = true
    )
}

/**
 * Lightweight in-memory handoff for Instagram-style "Use Audio" across AudioDetailModal -> Studio / Flash creation.
 */
object PendingMusicHandoff {
    @Volatile
    private var pending: AttachedMusicSelection? = null

    fun set(selection: AttachedMusicSelection?) {
        pending = selection
    }

    fun peek(): AttachedMusicSelection? = pending

    fun consume(): AttachedMusicSelection? {
        val value = pending
        pending = null
        return value
    }
}

/**
 * Firebase Firestore repository for Music Library tracks (backed by Music Cloudinary audio streams).
 * All track metadata lives in `music_tracks/{trackId}`; per-user bookmarks persist locally in SharedPrefs.
 */
class MusicRepository {

    companion object {
        private const val TAG = "MusicRepository"
        private const val PREFS_NAME = "oscellion_saved_music"
        private const val KEY_SAVED_IDS = "saved_track_ids"

        @Volatile
        private var instance: MusicRepository? = null

        fun getInstance(): MusicRepository = instance ?: synchronized(this) {
            instance ?: MusicRepository().also { instance = it }
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tracksCol = db.collection("music_tracks")

    fun getSavedTrackIds(context: Context): Set<String> {
        val uid = auth.currentUser?.uid ?: "guest"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("${KEY_SAVED_IDS}_$uid", emptySet())?.toSet() ?: emptySet()
    }

    fun toggleSavedTrack(context: Context, trackId: String): Boolean {
        val uid = auth.currentUser?.uid ?: "guest"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_SAVED_IDS}_$uid"
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowSaved = if (trackId in current) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        prefs.edit().putStringSet(key, current).apply()
        return nowSaved
    }

    suspend fun fetchActiveCatalog(context: Context): List<CatalogMusicTrack> = withContext(Dispatchers.IO) {
        val savedIds = getSavedTrackIds(context)
        try {
            val snap = tracksCol
                .whereEqualTo("status", "active")
                .limit(200)
                .get()
                .await()

            snap.documents
                .mapNotNull { doc -> parseTrackDoc(doc.id, doc.data, doc.id in savedIds) }
                .sortedWith(
                    compareByDescending<CatalogMusicTrack> { it.isTrending }
                        .thenByDescending { it.isFeatured }
                        .thenByDescending { it.useCount }
                        .thenByDescending { it.createdAt }
                )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Error loading active music catalog", e)
            emptyList()
        }
    }

    suspend fun fetchTrackById(context: Context, trackId: String): CatalogMusicTrack? = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) return@withContext null
        val savedIds = getSavedTrackIds(context)
        try {
            val doc = tracksCol.document(trackId).get().await()
            if (!doc.exists()) return@withContext null
            parseTrackDoc(doc.id, doc.data, doc.id in savedIds)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Error fetching track $trackId", e)
            null
        }
    }

    suspend fun incrementTrackUsage(trackId: String?) {
        val cleanId = trackId?.trim().orEmpty()
        if (cleanId.isBlank() || cleanId.startsWith("sample_") || cleanId.matches(Regex("^t\\d+$"))) {
            // Skip synthetic fallback IDs that don't exist in Firestore
            return
        }
        withContext(Dispatchers.IO) {
            try {
                tracksCol.document(cleanId).update("useCount", FieldValue.increment(1L)).await()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Failed incrementing useCount for $cleanId", e)
            }
        }
    }

    suspend fun fetchMediaUsingTrack(trackId: String): Pair<List<FlashVideo>, List<DiscoverPost>> =
        withContext(Dispatchers.IO) {
            if (trackId.isBlank()) return@withContext emptyList<FlashVideo>() to emptyList()
            val flashes = try {
                db.collection("flash_videos")
                    .whereEqualTo("musicTrackId", trackId)
                    .limit(60)
                    .get()
                    .await()
                    .toObjects(FlashVideo::class.java)
                    .filter { !it.isArchived && !it.isTakedown }
                    .sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                emptyList()
            }

            val posts = try {
                db.collection("community_works")
                    .whereEqualTo("musicTrackId", trackId)
                    .limit(60)
                    .get()
                    .await()
                    .toObjects(DiscoverPost::class.java)
                    .filter { !it.isArchived && !it.isTakedown }
                    .sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                emptyList()
            }

            flashes to posts
        }

    private fun parseTrackDoc(
        id: String,
        data: Map<String, Any?>?,
        isSaved: Boolean
    ): CatalogMusicTrack? {
        if (data == null) return null
        val durationSec = (data["durationSec"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 30
        val bestStart = (data["bestPartStartSec"] as? Number)?.toInt()?.coerceIn(0, maxOf(0, durationSec - 1)) ?: 0
        val bestEnd = (data["bestPartEndSec"] as? Number)?.toInt()?.coerceIn(bestStart + 1, durationSec)
            ?: minOf(durationSec, bestStart + 20)
        @Suppress("UNCHECKED_CAST")
        val rawLyrics = data["lyrics"] as? List<Map<String, Any?>> ?: emptyList()
        val parsedLyrics = rawLyrics.mapNotNull { cue ->
            val text = cue["text"]?.toString()?.trim().orEmpty()
            if (text.isEmpty()) null else MusicLyricLine(
                timeSec = (cue["timeSec"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                text = text
            )
        }

        return CatalogMusicTrack(
            id = id,
            title = data["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Untitled Track",
            artist = data["artist"]?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
            album = data["album"]?.toString().orEmpty(),
            description = data["description"]?.toString().orEmpty(),
            category = data["category"]?.toString()?.takeIf { it.isNotBlank() } ?: "Chill",
            collection = MusicMoodCollection.fromRaw(data["collection"]?.toString()),
            coverUrl = data["coverUrl"]?.toString().orEmpty(),
            audioUrl = data["audioUrl"]?.toString().orEmpty(),
            durationSec = durationSec,
            bestPartStartSec = bestStart,
            bestPartEndSec = bestEnd,
            isTrending = data["isTrending"] as? Boolean ?: false,
            isExplicit = data["isExplicit"] as? Boolean ?: false,
            isFeatured = data["isFeatured"] as? Boolean ?: false,
            status = data["status"]?.toString() ?: "active",
            useCount = (data["useCount"] as? Number)?.toInt() ?: 0,
            lyrics = parsedLyrics,
            saved = isSaved,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
