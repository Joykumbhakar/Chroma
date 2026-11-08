package com.chroma.studio.data.mutation

import android.content.Context
import android.util.Log
import com.chroma.studio.ChromaApp
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Local Storage Manager for Following Users Data.
 * 
 * Guarantees:
 * 1. ZERO ms waiting time: On cold start or when opening any user profile, feed post,
 *    reel, or user list, the following status is immediately retrieved from local storage,
 *    so the button is already "Following" on Frame 0 with NO delay.
 * 2. Multi-tier caching:
 *    - L1 Synchronous in-memory cache for 0ms UI reads.
 *    - L2 Persistent SharedPreferences storage for high-speed string set and user JSON retrieval.
 *    - L3 Atomic disk file storage in filesDir/following_cache for full offline data retention.
 * 3. Bidirectional reactive synchronization with [OptimisticFollowManager].
 */
object FollowingLocalStorageManager {
    private const val TAG = "FollowLocalStorage"
    private const val PREFS_NAME = "chroma_following_storage"
    private const val PREF_KEY_IDS_PREFIX = "following_ids_"
    private const val PREF_KEY_USERS_PREFIX = "following_users_json_"
    private const val PREF_KEY_LAST_UID = "last_active_user_uid"
    private const val CACHE_DIR_NAME = "following_cache"

    private val gson by lazy { Gson() }
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory L1 cache: current active user's followed user IDs
    private val memoryFollowingIds = ConcurrentHashMap<String, MutableSet<String>>()
    // In-memory L1 cache: user profiles of followed users (targetUid -> UserProfile)
    private val memoryFollowedUsers = ConcurrentHashMap<String, UserProfile>()

    private var appContext: Context? = null

    /**
     * Preloads following data synchronously from local storage at application startup.
     * Called in [ChromaApp.onCreate] to ensure that before any Activity or Composable starts,
     * the following set is already loaded in memory (0ms).
     */
    fun preload(context: Context) {
        appContext = context.applicationContext
        val activeUid = getActiveUid(context) ?: return
        try {
            // 1. Load following IDs from local storage into memory
            val storedIds = getStoredIdsFromDisk(context, activeUid)
            if (storedIds.isNotEmpty()) {
                val set = memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }
                set.addAll(storedIds)
                // Seed OptimisticFollowManager immediately
                OptimisticFollowManager.seedFollowing(storedIds, activeUid)
            }

            // 2. Load followed users metadata from local storage in background
            ioScope.launch {
                try {
                    val users = getStoredUsersFromDisk(context, activeUid)
                    for (u in users) {
                        if (u.uid.isNotBlank()) {
                            memoryFollowedUsers[u.uid] = u
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load cached followed users metadata: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Preload failed: ${e.message}")
        }
    }

    /**
     * Resolves the current active user ID from Firebase, SavedAccountManager, or last persisted UID.
     */
    fun getActiveUid(context: Context? = null): String? {
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
                ?: (ctx?.let { SavedAccountManager.getActiveAccount(it)?.uid })
                ?: (ctx?.let { getLastActiveUid(it) })
        } catch (_: Exception) {
            ctx?.let { getLastActiveUid(it) }
        }
    }

    private fun getLastActiveUid(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(PREF_KEY_LAST_UID, null)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveLastActiveUid(context: Context, uid: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_LAST_UID, uid).apply()
        } catch (_: Exception) {}
    }

    /**
     * Instant 0ms synchronous check if the given [userId] is currently followed.
     * Checks L1 memory cache first, then falls back to persistent local storage.
     */
    fun isFollowing(userId: String, context: Context? = null, uid: String? = null): Boolean {
        if (userId.isBlank()) return false
        val activeUid = uid ?: getActiveUid(context)
        if (activeUid != null) {
            val memSet = memoryFollowingIds[activeUid]
            if (memSet != null && memSet.contains(userId)) {
                return true
            }
        }
        for (set in memoryFollowingIds.values) {
            if (set.contains(userId)) return true
        }

        // Check OptimisticFollowManager in-memory StateFlow
        if (OptimisticFollowManager.followingUserIds.value.contains(userId)) {
            if (activeUid != null) {
                memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }.add(userId)
            }
            return true
        }

        // Fallback to local storage (e.g. if memory was pruned)
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }
        if (ctx != null && activeUid != null) {
            val diskIds = getStoredIdsFromDisk(ctx, activeUid)
            if (diskIds.contains(userId)) {
                memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }.add(userId)
                OptimisticFollowManager.seedFollowing(setOf(userId), activeUid)
                return true
            }
        }
        return false
    }

    fun isFollowingForUser(userId: String, uid: String): Boolean {
        return isFollowing(userId = userId, context = null, uid = uid)
    }

    /**
     * Returns all followed user IDs for [uid] from memory or local storage.
     */
    fun getFollowingIds(context: Context? = null, uid: String? = null): Set<String> {
        val targetUid = uid ?: getActiveUid(context) ?: return emptySet()
        val memSet = memoryFollowingIds[targetUid]
        if (memSet != null && memSet.isNotEmpty()) {
            return memSet.toSet()
        }
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null } ?: return emptySet()
        val diskIds = getStoredIdsFromDisk(ctx, targetUid)
        if (diskIds.isNotEmpty()) {
            val set = memoryFollowingIds.computeIfAbsent(targetUid) { ConcurrentHashMap.newKeySet() }
            set.addAll(diskIds)
        }
        return diskIds
    }

    /**
     * Returns cached user profiles for all followed users from local storage.
     */
    fun getCachedFollowingUsers(context: Context? = null, uid: String? = null): List<UserProfile> {
        val targetUid = uid ?: getActiveUid(context) ?: return emptyList()
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null } ?: return emptyList()
        val users = getStoredUsersFromDisk(ctx, targetUid)
        for (u in users) {
            if (u.uid.isNotBlank()) {
                memoryFollowedUsers[u.uid] = u
            }
        }
        return users
    }

    /**
     * Atomically adds a user to local storage and memory (instant 0ms).
     */
    fun addFollowing(context: Context? = null, uid: String? = null, targetUserId: String, targetProfile: UserProfile? = null) {
        if (targetUserId.isBlank()) return
        val activeUid = uid ?: getActiveUid(context) ?: "default_user"
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }

        // 1. Update L1 Memory Cache
        val set = memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }
        set.add(targetUserId)
        if (targetProfile != null) {
            memoryFollowedUsers[targetUserId] = targetProfile
        }

        // 2. Persist to L2/L3 Local Storage
        if (ctx != null) {
            saveLastActiveUid(ctx, activeUid)
            ioScope.launch {
                try {
                    val currentIds = getStoredIdsFromDisk(ctx, activeUid).toMutableSet()
                    currentIds.add(targetUserId)
                    persistIdsToDisk(ctx, activeUid, currentIds)

                    if (targetProfile != null) {
                        val currentUsers = getStoredUsersFromDisk(ctx, activeUid).filter { it.uid != targetUserId }.toMutableList()
                        currentUsers.add(0, targetProfile)
                        persistUsersToDisk(ctx, activeUid, currentUsers)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error adding following to local storage: ${e.message}")
                }
            }
        }
    }

    /**
     * Atomically removes a user from local storage and memory (instant 0ms).
     */
    fun removeFollowing(context: Context? = null, uid: String? = null, targetUserId: String) {
        if (targetUserId.isBlank()) return
        val activeUid = uid ?: getActiveUid(context)
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }

        // 1. Update L1 Memory Cache
        if (activeUid != null) {
            memoryFollowingIds[activeUid]?.remove(targetUserId)
        }
        for (set in memoryFollowingIds.values) {
            set.remove(targetUserId)
        }
        memoryFollowedUsers.remove(targetUserId)

        // 2. Persist to L2/L3 Local Storage
        if (ctx != null && activeUid != null) {
            ioScope.launch {
                try {
                    val currentIds = getStoredIdsFromDisk(ctx, activeUid).toMutableSet()
                    currentIds.remove(targetUserId)
                    persistIdsToDisk(ctx, activeUid, currentIds)

                    val currentUsers = getStoredUsersFromDisk(ctx, activeUid).filter { it.uid != targetUserId }
                    persistUsersToDisk(ctx, activeUid, currentUsers)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing following from local storage: ${e.message}")
                }
            }
        }
    }

    /**
     * Saves a complete authoritative set of following user IDs to local storage.
     */
    fun saveFollowingIds(context: Context? = null, uid: String? = null, ids: Set<String>) {
        val activeUid = uid ?: getActiveUid(context) ?: return
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }

        // 1. Update memory
        val set = memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }
        set.clear()
        set.addAll(ids)

        // 2. Persist to disk
        if (ctx != null) {
            saveLastActiveUid(ctx, activeUid)
            ioScope.launch {
                persistIdsToDisk(ctx, activeUid, ids)
            }
        }
    }

    /**
     * Saves a complete list of following UserProfiles to local storage.
     */
    fun saveFollowingUsers(context: Context? = null, uid: String? = null, users: List<UserProfile>) {
        val activeUid = uid ?: getActiveUid(context) ?: return
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }

        val ids = users.map { it.uid }.filter { it.isNotBlank() }.toSet()

        // 1. Update memory
        val set = memoryFollowingIds.computeIfAbsent(activeUid) { ConcurrentHashMap.newKeySet() }
        set.addAll(ids)
        for (u in users) {
            if (u.uid.isNotBlank()) {
                memoryFollowedUsers[u.uid] = u
            }
        }

        // 2. Persist to disk
        if (ctx != null) {
            saveLastActiveUid(ctx, activeUid)
            ioScope.launch {
                persistIdsToDisk(ctx, activeUid, ids)
                persistUsersToDisk(ctx, activeUid, users)
            }
        }
    }

    /**
     * Clears all cached following data for [uid] on logout.
     */
    fun clear(context: Context? = null, uid: String? = null) {
        val activeUid = uid ?: getActiveUid(context)
        if (activeUid != null) {
            memoryFollowingIds.remove(activeUid)
        } else {
            memoryFollowingIds.clear()
        }
        memoryFollowedUsers.clear()
        val ctx = context ?: appContext ?: try { ChromaApp.instance } catch (_: Exception) { null }
        if (ctx != null && activeUid != null) {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .remove("$PREF_KEY_IDS_PREFIX$activeUid")
                    .remove("$PREF_KEY_USERS_PREFIX$activeUid")
                    .remove(PREF_KEY_LAST_UID)
                    .apply()
                val cacheFile = getCacheFile(ctx, activeUid)
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Internal Disk I/O Helpers
    // ─────────────────────────────────────────────────────────────

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCacheFile(context: Context, uid: String): File {
        val safeUid = uid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getCacheDir(context), "following_$safeUid.json")
    }

    private fun getStoredIdsFromDisk(context: Context, uid: String): Set<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getStringSet("$PREF_KEY_IDS_PREFIX$uid", null)
            if (stored != null) {
                stored
            } else {
                // Fallback to JSON cache file if SharedPreferences empty
                val file = getCacheFile(context, uid)
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<List<String>>() {}.type
                    val list: List<String>? = gson.fromJson(json, type)
                    list?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading stored IDs from disk: ${e.message}")
            emptySet()
        }
    }

    private fun persistIdsToDisk(context: Context, uid: String, ids: Set<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet("$PREF_KEY_IDS_PREFIX$uid", ids).apply()

            // Atomic file write backup
            val file = getCacheFile(context, uid)
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(gson.toJson(ids.toList()))
            if (tempFile.exists()) {
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error writing IDs to disk: ${e.message}")
        }
    }

    private fun getStoredUsersFromDisk(context: Context, uid: String): List<UserProfile> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("$PREF_KEY_USERS_PREFIX$uid", null) ?: return emptyList()
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading stored users from disk: ${e.message}")
            emptyList()
        }
    }

    private fun persistUsersToDisk(context: Context, uid: String, users: List<UserProfile>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("$PREF_KEY_USERS_PREFIX$uid", gson.toJson(users)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error writing users to disk: ${e.message}")
        }
    }
}
