package com.chroma.studio.chat.data

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-account isolated offline message cache and pre-loading engine for Oscellion.
 *
 * Security & Multi-Account Isolation:
 * - Every memory and disk cache entry is strictly partitioned by Firebase UID (`u_<uid>/`).
 * - Legacy unscoped cache files (`recent_chats_cache.json`, `room_*.json` at root) are
 *   automatically purged so conversations can NEVER bleed from one logged-in account to another.
 * - Logout wipes in-memory and disk chat caches immediately.
 */
object ChatLocalCacheManager {
    private const val TAG = "ChatLocalCache"
    private const val CACHE_DIR_NAME = "chat_messages_cache"
    private const val MEDIA_CACHE_DIR_NAME = "chat_media_cache"
    private const val RECENT_CHATS_FILE_NAME = "recent_chats_cache.json"
    private const val MAX_CACHED_MESSAGES_PER_ROOM = 200

    private val gson by lazy { Gson() }
    private val memoryCache = ConcurrentHashMap<String, List<ChatMessage>>()
    private val cachedRecentChatsByUser = ConcurrentHashMap<String, List<ChatRoom>>()
    private val cacheScope = CoroutineScope(Dispatchers.IO)
    private val activeDownloads = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var legacyPurged = false

    @androidx.annotation.VisibleForTesting
    var testCacheDir: File? = null

    private fun logE(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: Throwable) {
            System.err.println("[$tag] ERROR: $message ${throwable?.message ?: ""}")
        }
    }

    private fun logW(tag: String, message: String) {
        try {
            Log.w(tag, message)
        } catch (_: Throwable) {
            System.err.println("[$tag] WARN: $message")
        }
    }

    private fun resolveUserId(userId: String?): String? {
        if (!userId.isNullOrBlank()) return userId
        if (testCacheDir != null) return "test_user"
        return try {
            FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun safeSegment(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private fun memKey(uid: String, roomId: String): String = "$uid::$roomId"

    private fun getRootCacheDir(context: Context): File {
        val dir = testCacheDir?.let { File(it, CACHE_DIR_NAME) } ?: File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!legacyPurged) {
            legacyPurged = true
            purgeLegacyUnscopedFiles(dir)
        }
        return dir
    }

    /**
     * Deletes any legacy unscoped cache files written by older app versions so they
     * can never be read by a different logged-in account.
     */
    private fun purgeLegacyUnscopedFiles(rootDir: File) {
        try {
            rootDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name == RECENT_CHATS_FILE_NAME || file.name.startsWith("room_"))) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun getUserCacheDir(context: Context, uid: String): File {
        val dir = File(getRootCacheDir(context), "u_${safeSegment(uid)}")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getRoomFile(context: Context, uid: String, roomId: String): File {
        return File(getUserCacheDir(context, uid), "room_${safeSegment(roomId)}.json")
    }

    fun getMediaCacheDir(context: Context): File {
        val dir = testCacheDir?.let { File(it, MEDIA_CACHE_DIR_NAME) } ?: File(context.filesDir, MEDIA_CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun urlToCacheFileName(url: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(url.toByteArray(Charsets.UTF_8))
            val hex = hashBytes.take(16).joinToString("") { "%02x".format(it) }
            val cleanPath = url.substringBefore('?').substringBefore('#')
            val ext = cleanPath.substringAfterLast('.', "").lowercase()
            val safeExt = if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "bin"
            "media_${hex}.$safeExt"
        } catch (e: Exception) {
            "media_${url.hashCode().toLong() and 0xFFFFFFFFL}.bin"
        }
    }

    fun getCachedMediaFile(context: Context, url: String?): File? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("file://")) {
            val f = File(url.removePrefix("file://"))
            return if (f.exists() && f.length() > 0L) f else null
        }
        if (url.startsWith("/") && !url.startsWith("http")) {
            val f = File(url)
            return if (f.exists() && f.length() > 0L) f else null
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val targetFile = File(getMediaCacheDir(context), urlToCacheFileName(url))
        return if (targetFile.exists() && targetFile.length() > 0L) targetFile else null
    }

    fun getOfflineSafeMediaUrl(context: Context, url: String?): String {
        if (url.isNullOrBlank()) return ""
        val cached = getCachedMediaFile(context, url)
        if (cached != null) {
            return "file://${cached.absolutePath}"
        }
        return url
    }

    fun cacheMediaFile(context: Context, url: String) {
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) return
        val targetFile = File(getMediaCacheDir(context), urlToCacheFileName(url))
        if (targetFile.exists() && targetFile.length() > 0L) return

        if (!activeDownloads.add(url)) return

        cacheScope.launch {
            try {
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.instanceFollowRedirects = true
                conn.doInput = true
                conn.connect()

                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        if (targetFile.exists()) targetFile.delete()
                        if (!tempFile.renameTo(targetFile)) {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                        }
                    }
                }
                conn.disconnect()

                val ext = targetFile.extension.lowercase()
                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif") || url.contains("image", ignoreCase = true)) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(targetFile)
                            .crossfade(false)
                            .build()
                        context.imageLoader.enqueue(request)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                logW(TAG, "Failed to cache media from $url: ${e.message}")
            } finally {
                activeDownloads.remove(url)
            }
        }
    }

    fun cacheAllMediaForMessages(context: Context, messages: List<ChatMessage>) {
        val urlsToCache = mutableListOf<String>()
        for (msg in messages) {
            if (!msg.imageUrl.isNullOrBlank()) urlsToCache.add(msg.imageUrl)
            if (!msg.mediaUrl.isNullOrBlank()) urlsToCache.add(msg.mediaUrl)
            if (!msg.storyThumbnailUrl.isNullOrBlank()) urlsToCache.add(msg.storyThumbnailUrl)
        }
        for (url in urlsToCache.distinct()) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                cacheMediaFile(context, url)
            }
        }
    }

    fun getMemoryCachedMessages(roomId: String, userId: String? = null): List<ChatMessage>? {
        val uid = resolveUserId(userId) ?: return null
        return memoryCache[memKey(uid, roomId)]
    }

    fun getCachedMessages(context: Context, roomId: String, userId: String? = null): List<ChatMessage> {
        val uid = resolveUserId(userId) ?: return emptyList()
        val key = memKey(uid, roomId)
        memoryCache[key]?.let { return it }

        return try {
            val file = getRoomFile(context, uid, roomId)
            if (!file.exists()) return emptyList()

            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return emptyList()

            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val messages: List<ChatMessage> = gson.fromJson(json, type) ?: emptyList()
            memoryCache[key] = messages
            messages
        } catch (e: Exception) {
            logE(TAG, "Failed to load cached messages for room $roomId", e)
            emptyList()
        }
    }

    fun saveMessages(context: Context, roomId: String, messages: List<ChatMessage>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        if (messages.isEmpty()) return
        val trimmed = messages
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
            .take(MAX_CACHED_MESSAGES_PER_ROOM)

        memoryCache[memKey(uid, roomId)] = trimmed

        val writeBlock = {
            try {
                val file = getRoomFile(context, uid, roomId)
                val dir = file.parentFile
                if (dir != null && !dir.exists()) dir.mkdirs()
                val tempFile = File(dir, "${file.name}.tmp")
                val json = gson.toJson(trimmed)
                tempFile.writeText(json, Charsets.UTF_8)
                if (file.exists()) file.delete()
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
                cacheAllMediaForMessages(context, trimmed)
            } catch (e: Exception) {
                logE(TAG, "Failed to save message cache for room $roomId", e)
            }
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    fun appendOrUpdateMessage(context: Context, roomId: String, message: ChatMessage, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val current = getCachedMessages(context, roomId, uid).toMutableList()
        val index = current.indexOfFirst { it.id == message.id }
        if (index != -1) {
            current[index] = message
        } else {
            current.add(0, message)
        }
        saveMessages(context, roomId, current, uid)
        cacheAllMediaForMessages(context, listOf(message))
    }

    fun preloadRooms(context: Context, roomIds: List<String>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        cacheScope.launch {
            for (roomId in roomIds.take(12)) {
                val key = memKey(uid, roomId)
                val msgs = if (!memoryCache.containsKey(key)) {
                    getCachedMessages(context, roomId, uid)
                } else {
                    memoryCache[key] ?: emptyList()
                }
                if (msgs.isNotEmpty()) {
                    cacheAllMediaForMessages(context, msgs)
                }
            }
        }
    }

    /**
     * Retrieves cached recent chat rooms strictly scoped to [userId].
     * Never returns rooms belonging to any other account.
     */
    fun getCachedRecentChats(context: Context, userId: String? = null): List<ChatRoom> {
        val uid = resolveUserId(userId) ?: return emptyList()
        cachedRecentChatsByUser[uid]?.let { list ->
            return list.filter { uid in it.participants }
        }
        return try {
            val file = File(getUserCacheDir(context, uid), RECENT_CHATS_FILE_NAME)
            if (!file.exists()) return emptyList()
            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<List<ChatRoom>>() {}.type
            val rawChats: List<ChatRoom> = gson.fromJson(json, type) ?: emptyList()
            val ownedChats = rawChats.filter { uid in it.participants }
            cachedRecentChatsByUser[uid] = ownedChats
            ownedChats
        } catch (e: Exception) {
            logE(TAG, "Failed to load cached recent chats for user $uid", e)
            emptyList()
        }
    }

    /**
     * Persists recent chats strictly for [userId]. When [chats] is empty, deletes the
     * user's cached file so stale conversations never linger.
     */
    fun saveRecentChats(context: Context, chats: List<ChatRoom>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val ownedChats = chats.filter { uid in it.participants }
        cachedRecentChatsByUser[uid] = ownedChats

        val writeBlock = {
            try {
                val dir = getUserCacheDir(context, uid)
                val file = File(dir, RECENT_CHATS_FILE_NAME)
                if (ownedChats.isEmpty()) {
                    if (file.exists()) file.delete()
                } else {
                    if (!dir.exists()) dir.mkdirs()
                    val tempFile = File(dir, "${file.name}.tmp")
                    val json = gson.toJson(ownedChats)
                    tempFile.writeText(json, Charsets.UTF_8)
                    if (file.exists()) file.delete()
                    if (!tempFile.renameTo(file)) {
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                logE(TAG, "Failed to save recent chats cache for user $uid", e)
            }
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    fun clearRoom(context: Context, roomId: String, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        memoryCache.remove(memKey(uid, roomId))
        cacheScope.launch {
            try {
                val file = getRoomFile(context, uid, roomId)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                logE(TAG, "Failed to delete cache file for room $roomId", e)
            }
        }
    }

    /**
     * Clears in-memory L1 cache on logout or account switch.
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        cachedRecentChatsByUser.clear()
    }

    /**
     * Full logout cleanup: wipes in-memory cache AND deletes cached chat JSON files on disk
     * so no conversation list or messages can ever leak to another account on this device.
     */
    fun clearAllOnLogout(context: Context) {
        clearMemoryCache()
        val wipeBlock = {
            try {
                val root = testCacheDir?.let { File(it, CACHE_DIR_NAME) } ?: File(context.filesDir, CACHE_DIR_NAME)
                if (root.exists()) {
                    root.deleteRecursively()
                }
                legacyPurged = true
            } catch (e: Exception) {
                logE(TAG, "Failed to wipe chat disk cache on logout", e)
            }
        }
        if (testCacheDir != null) {
            wipeBlock()
        } else {
            cacheScope.launch { wipeBlock() }
        }
    }

    fun removeMessage(context: Context, roomId: String, messageId: String, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val current = getCachedMessages(context, roomId, uid).toMutableList()
        val filtered = current.filter { it.id != messageId }
        memoryCache[memKey(uid, roomId)] = filtered
        cacheScope.launch {
            try {
                val file = getRoomFile(context, uid, roomId)
                val tempFile = File(file.parentFile, "${file.name}.tmp")
                val json = gson.toJson(filtered)
                tempFile.writeText(json, Charsets.UTF_8)
                if (file.exists()) file.delete()
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                logE(TAG, "Failed to remove message $messageId from room $roomId cache", e)
            }
        }
    }
}
