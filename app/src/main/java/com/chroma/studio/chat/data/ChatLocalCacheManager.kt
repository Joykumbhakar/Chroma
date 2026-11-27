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
 * WhatsApp-grade Frame-0 Architecture:
 * - L1 RAM: `memoryCache`, `cachedRecentChatsByUser`, and `decryptedTextMemoryCache`
 *   for 0ms UI state hydration.
 * - L2 Disk: WAL-mode SQLite (`ChatSQLiteStore`) supporting unlimited local history,
 *   single-row upserts, paged older message queries, and persisted E2EE plaintexts.
 * - Fallback: Atomic per-account JSON files under `u_<uid>/` for legacy migration and
 *   bare-JVM unit tests (`testCacheDir != null`).
 *
 * Security & Multi-Account Isolation:
 * - Every memory, SQLite, and JSON cache entry is strictly partitioned by Firebase UID.
 * - Logout wipes L1 memory, SQLite tables, and disk directories immediately.
 */
object ChatLocalCacheManager {
    private const val TAG = "ChatLocalCache"
    private const val CACHE_DIR_NAME = "chat_messages_cache"
    private const val MEDIA_CACHE_DIR_NAME = "chat_media_cache"
    private const val RECENT_CHATS_FILE_NAME = "recent_chats_cache.json"
    private const val DECRYPTED_TEXTS_FILE_NAME = "decrypted_texts_cache.json"
    const val DEFAULT_MEMORY_PAGE_SIZE = 60
    private const val MAX_JSON_FALLBACK_MESSAGES = 2000

    private val gson by lazy { Gson() }
    private val memoryCache = ConcurrentHashMap<String, List<ChatMessage>>()
    private val cachedRecentChatsByUser = ConcurrentHashMap<String, List<ChatRoom>>()
    private val decryptedTextMemoryCache = ConcurrentHashMap<String, String>()
    private val cacheScope = CoroutineScope(Dispatchers.IO)
    private val activeDownloads = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var legacyPurged = false

    @androidx.annotation.VisibleForTesting
    var testCacheDir: File? = null

    internal fun isTestEnvironment(): Boolean = testCacheDir != null

    private fun resolveUserId(userId: String?): String? {
        if (!userId.isNullOrBlank()) return userId
        if (testCacheDir != null) return "test_user"
        return try {
            FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun safeSegment(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun memKey(uid: String, roomId: String): String = "$uid::$roomId"

    private fun decryptMemKey(uid: String, cacheKey: String): String = "$uid::$cacheKey"

    private fun logE(tag: String, msg: String, e: Throwable? = null) {
        if (testCacheDir != null) return
        try {
            if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg)
        } catch (_: Throwable) {}
    }

    private fun logW(tag: String, msg: String) {
        if (testCacheDir != null) return
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {}
    }

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

    private fun getDecryptedFile(context: Context, uid: String): File {
        return File(getUserCacheDir(context, uid), DECRYPTED_TEXTS_FILE_NAME)
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
        if (testCacheDir != null) return
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
        if (testCacheDir != null) return
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

    private fun readLegacyRoomMessages(context: Context, uid: String, roomId: String): List<ChatMessage> {
        return try {
            val file = getRoomFile(context, uid, roomId)
            if (!file.exists()) return emptyList()
            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson<List<ChatMessage>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            logE(TAG, "Failed to read legacy cached messages for room $roomId", e)
            emptyList()
        }
    }

    private fun writeLegacyRoomMessages(context: Context, uid: String, roomId: String, messages: List<ChatMessage>) {
        try {
            val file = getRoomFile(context, uid, roomId)
            val dir = file.parentFile
            if (dir != null && !dir.exists()) dir.mkdirs()
            val tempFile = File(dir, "${file.name}.tmp")
            val json = gson.toJson(messages.take(MAX_JSON_FALLBACK_MESSAGES))
            tempFile.writeText(json, Charsets.UTF_8)
            if (file.exists()) file.delete()
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to write fallback message cache for room $roomId", e)
        }
    }

    fun getCachedMessages(
        context: Context,
        roomId: String,
        userId: String? = null,
        limit: Int = DEFAULT_MEMORY_PAGE_SIZE
    ): List<ChatMessage> {
        val uid = resolveUserId(userId) ?: return emptyList()
        val effectiveLimit = limit.coerceAtLeast(1)
        val key = memKey(uid, roomId)

        memoryCache[key]?.let { cached ->
            if (cached.size >= effectiveLimit || isTestEnvironment()) {
                return cached.take(effectiveLimit)
            }
            // If caller wants more than what is in L1 RAM, fall through to SQLite
        }

        // L2 WAL SQLite store (device)
        val sqliteMsgs = ChatSQLiteStore.getMessages(
            context = context,
            uid = uid,
            roomId = roomId,
            limit = effectiveLimit,
            beforeTimestamp = null,
            gson = gson
        )
        if (!sqliteMsgs.isNullOrEmpty()) {
            memoryCache[key] = sqliteMsgs.take(DEFAULT_MEMORY_PAGE_SIZE)
            return sqliteMsgs
        }

        // Fallback / legacy migration from JSON file
        val legacyMsgs = readLegacyRoomMessages(context, uid, roomId)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
        if (legacyMsgs.isNotEmpty()) {
            memoryCache[key] = legacyMsgs.take(DEFAULT_MEMORY_PAGE_SIZE)
            if (ChatSQLiteStore.isAvailable(context)) {
                cacheScope.launch {
                    ChatSQLiteStore.upsertMessages(context, uid, roomId, legacyMsgs, gson)
                }
            }
            return legacyMsgs.take(effectiveLimit)
        }

        return memoryCache[key]?.take(effectiveLimit) ?: emptyList()
    }

    /**
     * Loads older locally-persisted messages strictly older than [beforeTimestamp]
     * from SQLite (or JSON fallback in tests), enabling instant scroll-up history paging
     * even in Airplane mode or before Firestore responds.
     */
    fun getOlderCachedMessages(
        context: Context,
        roomId: String,
        beforeTimestamp: Long,
        limit: Int = 40,
        userId: String? = null
    ): List<ChatMessage> {
        val uid = resolveUserId(userId) ?: return emptyList()
        val effectiveLimit = limit.coerceAtLeast(1)

        val sqliteOlder = ChatSQLiteStore.getMessages(
            context = context,
            uid = uid,
            roomId = roomId,
            limit = effectiveLimit,
            beforeTimestamp = beforeTimestamp,
            gson = gson
        )
        if (sqliteOlder != null) {
            return sqliteOlder
        }

        return readLegacyRoomMessages(context, uid, roomId)
            .asSequence()
            .filter { it.timestamp < beforeTimestamp }
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
            .take(effectiveLimit)
            .toList()
    }

    fun saveMessages(context: Context, roomId: String, messages: List<ChatMessage>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        if (messages.isEmpty()) return

        val normalized = messages
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }

        // Merge with existing L1 top window so partial updates never shrink RAM window unexpectedly
        val key = memKey(uid, roomId)
        val mergedRam = (normalized + (memoryCache[key] ?: emptyList()))
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
            .take(DEFAULT_MEMORY_PAGE_SIZE)
        memoryCache[key] = mergedRam

        val writeBlock = {
            if (ChatSQLiteStore.isAvailable(context)) {
                ChatSQLiteStore.upsertMessages(context, uid, roomId, normalized, gson)
            } else {
                val existing = readLegacyRoomMessages(context, uid, roomId)
                val mergedDisk = (normalized + existing)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
                writeLegacyRoomMessages(context, uid, roomId, mergedDisk)
            }
            cacheAllMediaForMessages(context, normalized.take(DEFAULT_MEMORY_PAGE_SIZE))
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    fun appendOrUpdateMessage(context: Context, roomId: String, message: ChatMessage, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val key = memKey(uid, roomId)
        val existingRam = memoryCache[key] ?: getCachedMessages(context, roomId, uid, DEFAULT_MEMORY_PAGE_SIZE)
        val updatedRam = (listOf(message) + existingRam.filter { it.id != message.id })
            .sortedByDescending { it.timestamp }
            .take(DEFAULT_MEMORY_PAGE_SIZE)
        memoryCache[key] = updatedRam

        val writeBlock = {
            if (ChatSQLiteStore.isAvailable(context)) {
                ChatSQLiteStore.upsertMessages(context, uid, roomId, listOf(message), gson)
            } else {
                val existingDisk = readLegacyRoomMessages(context, uid, roomId)
                val mergedDisk = (listOf(message) + existingDisk.filter { it.id != message.id })
                    .sortedByDescending { it.timestamp }
                writeLegacyRoomMessages(context, uid, roomId, mergedDisk)
            }
            cacheAllMediaForMessages(context, listOf(message))
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    fun getPersistedPlaintext(context: Context, cacheKey: String, userId: String? = null): String? {
        if (cacheKey.isBlank()) return null
        val uid = resolveUserId(userId) ?: return null
        val mKey = decryptMemKey(uid, cacheKey)
        decryptedTextMemoryCache[mKey]?.let { return it }

        val sqliteHit = ChatSQLiteStore.getPlaintext(context, uid, cacheKey)
        if (sqliteHit != null) {
            decryptedTextMemoryCache[mKey] = sqliteHit
            return sqliteHit
        }

        if (testCacheDir != null) {
            try {
                val file = getDecryptedFile(context, uid)
                if (file.exists()) {
                    val json = file.readText(Charsets.UTF_8)
                    if (json.isNotBlank()) {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val map: Map<String, String> = gson.fromJson(json, type) ?: emptyMap()
                        map.forEach { (k, v) -> decryptedTextMemoryCache[decryptMemKey(uid, k)] = v }
                        return map[cacheKey]
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun savePersistedPlaintext(context: Context, cacheKey: String, plainText: String, userId: String? = null) {
        if (cacheKey.isBlank()) return
        val uid = resolveUserId(userId) ?: return
        val mKey = decryptMemKey(uid, cacheKey)
        decryptedTextMemoryCache[mKey] = plainText

        val writeBlock = {
            if (ChatSQLiteStore.isAvailable(context)) {
                ChatSQLiteStore.savePlaintext(context, uid, cacheKey, plainText)
            } else if (testCacheDir != null) {
                try {
                    val file = getDecryptedFile(context, uid)
                    val map = if (file.exists()) {
                        val json = file.readText(Charsets.UTF_8)
                        val type = object : TypeToken<MutableMap<String, String>>() {}.type
                        gson.fromJson<MutableMap<String, String>>(json, type) ?: mutableMapOf()
                    } else {
                        mutableMapOf()
                    }
                    map[cacheKey] = plainText
                    val tempFile = File(file.parentFile, "${file.name}.tmp")
                    tempFile.writeText(gson.toJson(map), Charsets.UTF_8)
                    if (file.exists()) file.delete()
                    if (!tempFile.renameTo(file)) {
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                    }
                } catch (_: Exception) {}
            }
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    fun preloadRooms(context: Context, roomIds: List<String>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val task = {
            for (roomId in roomIds.take(15)) {
                val key = memKey(uid, roomId)
                val msgs = if (!memoryCache.containsKey(key)) {
                    getCachedMessages(context, roomId, uid, DEFAULT_MEMORY_PAGE_SIZE)
                } else {
                    memoryCache[key] ?: emptyList()
                }
                if (msgs.isNotEmpty()) {
                    cacheAllMediaForMessages(context, msgs)
                }
            }
        }
        if (testCacheDir != null) {
            task()
        } else {
            cacheScope.launch { task() }
        }
    }

    /**
     * Warms recent chats + top 15 rooms into L1 RAM cache immediately on app start
     * or account switch so tapping any conversation renders on Frame 0.
     */
    fun warmupOnStartup(context: Context, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val task = {
            val rooms = getCachedRecentChats(context, uid)
            if (rooms.isNotEmpty()) {
                preloadRooms(context, rooms.map { it.id }, uid)
            }
        }
        if (testCacheDir != null) {
            task()
        } else {
            cacheScope.launch { task() }
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

        val sqliteRooms = ChatSQLiteStore.getRecentRooms(context, uid, gson)
        if (!sqliteRooms.isNullOrEmpty()) {
            val owned = sqliteRooms.filter { uid in it.participants }
            cachedRecentChatsByUser[uid] = owned
            return owned
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
            if (ownedChats.isNotEmpty() && ChatSQLiteStore.isAvailable(context)) {
                cacheScope.launch {
                    ChatSQLiteStore.replaceUserRooms(context, uid, ownedChats, gson)
                }
            }
            ownedChats
        } catch (e: Exception) {
            logE(TAG, "Failed to load cached recent chats for user $uid", e)
            emptyList()
        }
    }

    /**
     * Persists recent chats strictly for [userId]. When [chats] is empty, deletes the
     * user's cached records so stale conversations never linger.
     */
    fun saveRecentChats(context: Context, chats: List<ChatRoom>, userId: String? = null) {
        val uid = resolveUserId(userId) ?: return
        val ownedChats = chats.filter { uid in it.participants }
        cachedRecentChatsByUser[uid] = ownedChats

        val writeBlock = {
            if (ChatSQLiteStore.isAvailable(context)) {
                ChatSQLiteStore.replaceUserRooms(context, uid, ownedChats, gson)
            } else {
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
        }

        if (testCacheDir != null) {
            writeBlock()
        } else {
            cacheScope.launch { writeBlock() }
        }
    }

    /**
     * Updates a cached conversation preview & unread badge immediately when an FCM push arrives,
     * so opening the chat list shows the new message before Firestore connects.
     */
    fun updateRoomOnIncomingPush(
        context: Context,
        roomId: String,
        senderId: String,
        lastMessagePreview: String,
        timestamp: Long,
        messageId: String? = null,
        incrementUnread: Boolean = true,
        userId: String? = null
    ) {
        val uid = resolveUserId(userId) ?: return
        if (roomId.isBlank()) return
        val current = getCachedRecentChats(context, uid).toMutableList()
        val idx = current.indexOfFirst { it.id == roomId }
        if (idx == -1) return

        val existing = current[idx]
        val updatedUnread = existing.unreadCount.toMutableMap()
        if (incrementUnread && senderId != uid) {
            updatedUnread[uid] = (updatedUnread[uid] ?: 0) + 1
        }
        val updated = existing.copy(
            lastMessage = lastMessagePreview.ifBlank { existing.lastMessage },
            lastMessageTime = maxOf(existing.lastMessageTime, timestamp),
            lastSender = senderId.ifBlank { existing.lastSender },
            lastMessageId = messageId?.takeIf { it.isNotBlank() } ?: existing.lastMessageId,
            lastMessageIsDelivered = true,
            unreadCount = updatedUnread
        )
        current.removeAt(idx)
        val sorted = (listOf(updated) + current).sortedByDescending { it.lastMessageTime }
        cachedRecentChatsByUser[uid] = sorted

        val writeBlock = {
            if (ChatSQLiteStore.isAvailable(context)) {
                ChatSQLiteStore.upsertRoom(context, uid, updated, gson)
            } else {
                saveRecentChats(context, sorted, uid)
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
        val task = {
            try {
                ChatSQLiteStore.clearRoom(context, uid, roomId)
                val file = getRoomFile(context, uid, roomId)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                logE(TAG, "Failed to delete cache for room $roomId", e)
            }
        }
        if (testCacheDir != null) {
            task()
        } else {
            cacheScope.launch { task() }
        }
    }

    /**
     * Clears in-memory L1 cache on logout or account switch.
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        cachedRecentChatsByUser.clear()
        decryptedTextMemoryCache.clear()
    }

    /**
     * Full logout cleanup: wipes in-memory cache, SQLite tables, and cached chat files on disk
     * so no conversation list, messages, or decrypted plaintexts can ever leak to another account.
     */
    fun clearAllOnLogout(context: Context) {
        clearMemoryCache()
        val wipeBlock = {
            try {
                ChatSQLiteStore.clearAll(context)
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
        val key = memKey(uid, roomId)
        val current = memoryCache[key]
        if (current != null) {
            memoryCache[key] = current.filter { it.id != messageId }
        }
        val task = {
            try {
                if (ChatSQLiteStore.isAvailable(context)) {
                    ChatSQLiteStore.removeMessage(context, uid, messageId)
                } else {
                    val existing = readLegacyRoomMessages(context, uid, roomId).filter { it.id != messageId }
                    writeLegacyRoomMessages(context, uid, roomId, existing)
                }
            } catch (e: Exception) {
                logE(TAG, "Failed to remove message $messageId from room $roomId cache", e)
            }
        }
        if (testCacheDir != null) {
            task()
        } else {
            cacheScope.launch { task() }
        }
    }
}
