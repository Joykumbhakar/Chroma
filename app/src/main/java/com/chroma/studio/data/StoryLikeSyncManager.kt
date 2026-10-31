package com.chroma.studio.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.chroma.studio.ChromaApp
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Data model for pending offline story like/unlike actions.
 */
data class PendingStoryLikeAction(
    val storyId: String,
    val userId: String,
    val targetLiked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Production-grade offline-first Story Like Cache & Sync Manager.
 *
 * Architecture & Guarantees:
 * 1. Optimistic UI: State updates immediately on the main thread for 60/120fps buttery-smooth feel.
 * 2. Persistent LocalStorage: Liked story IDs are cached in SharedPreferences per user so they persist
 *    across app restarts, screen navigation, and offline sessions.
 * 3. Offline Pending Queue: Likes or unlikes performed while disconnected or on high-latency/slow network
 *    are recorded in an persistent queue.
 * 4. Automatic Network Monitoring: Listens for internet connectivity callbacks via ConnectivityManager.
 *    As soon as the device connects to the internet ("perfect internet"), it drains and syncs the queue to Firestore.
 * 5. Idempotent Synchronization: Uses atomic Firestore field operations to avoid race conditions.
 * 6. Conflict-Free Merging: When fetching remote stories, pending local actions take precedence so that
 *    slow network responses never overwrite newer user choices.
 */
object StoryLikeSyncManager {
    private const val TAG = "StoryLikeSyncManager"
    private const val PREFS_NAME = "chroma_story_likes_storage"
    private const val KEY_CACHED_LIKES_PREFIX = "cached_liked_stories_"
    private const val KEY_PENDING_QUEUE_PREFIX = "pending_story_like_queue_"

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()
    private val storyRepository = StoryRepository()

    private val _likedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val likedStoryIds: StateFlow<Set<String>> = _likedStoryIds.asStateFlow()

    private var isNetworkMonitoringActive = false
    private var lastInitializedUserId: String? = null

    init {
        try {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!currentUid.isNullOrBlank()) {
                initForUser(currentUid)
            }
        } catch (e: Exception) {
            Log.w(TAG, "StoryLikeSyncManager initial init deferred", e)
        }
    }

    private fun getContext(): Context? {
        return try {
            ChromaApp.instance
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Initializes the manager for a specific user.
     * Loads their cached liked story IDs from LocalStorage immediately.
     */
    fun initForUser(userId: String) {
        if (userId.isBlank()) return
        lastInitializedUserId = userId

        val cached = getCachedLikedStoryIds(userId)
        val pending = getPendingQueue(userId)
        val pendingMap = pending.associate { it.storyId to it.targetLiked }

        val activeLiked = cached.toMutableSet()
        pendingMap.forEach { (storyId, isLiked) ->
            if (isLiked) activeLiked.add(storyId) else activeLiked.remove(storyId)
        }

        _likedStoryIds.value = activeLiked
        ensureNetworkMonitoring()

        // If there are pending actions, try syncing them in background
        if (pending.isNotEmpty()) {
            scope.launch {
                syncPendingQueue()
            }
        }
    }

    /**
     * Starts listening for network connectivity changes.
     */
    fun ensureNetworkMonitoring(context: Context? = null) {
        if (isNetworkMonitoringActive) return
        val ctx = context?.applicationContext ?: getContext() ?: return

        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network restored. Triggering pending story likes sync...")
                    scope.launch {
                        syncPendingQueue()
                    }
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        Log.d(TAG, "Network validated. Triggering pending story likes sync...")
                        scope.launch {
                            syncPendingQueue()
                        }
                    }
                }
            })
            isNetworkMonitoringActive = true
            Log.d(TAG, "Network monitoring registered successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback", e)
        }
    }

    /**
     * Toggles like on a story with optimistic UI and LocalStorage caching.
     * @return The new liked state (true if now liked, false if unliked).
     */
    fun toggleLike(storyId: String, userId: String? = null): Boolean {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return false
        if (storyId.isBlank()) return false

        ensureNetworkMonitoring()
        val currentLiked = _likedStoryIds.value.contains(storyId)
        val targetLiked = !currentLiked

        // 1. Immediate in-memory optimistic update
        val updatedSet = if (targetLiked) {
            _likedStoryIds.value + storyId
        } else {
            _likedStoryIds.value - storyId
        }
        _likedStoryIds.value = updatedSet

        // 2. Persist to LocalStorage cache immediately
        saveCachedLikedStoryIds(uid, updatedSet)

        // 3. Record in LocalStorage Pending Queue
        recordPendingAction(storyId, uid, targetLiked)

        // 4. Attempt immediate background network sync
        scope.launch {
            val success = storyRepository.setStoryLike(storyId, targetLiked)
            if (success) {
                removePendingAction(storyId, uid, targetLiked)
                Log.d(TAG, "Optimistic sync succeeded for story $storyId (target=$targetLiked)")
            } else {
                Log.d(TAG, "Optimistic sync deferred (offline/slow internet) for story $storyId (target=$targetLiked)")
            }
        }

        return targetLiked
    }

    /**
     * Forces like on a story (e.g. for double-tap gestures).
     */
    fun forceLike(storyId: String, userId: String? = null): Boolean {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return false
        if (storyId.isBlank()) return false

        if (_likedStoryIds.value.contains(storyId)) {
            return true
        }

        ensureNetworkMonitoring()
        val updatedSet = _likedStoryIds.value + storyId
        _likedStoryIds.value = updatedSet

        saveCachedLikedStoryIds(uid, updatedSet)
        recordPendingAction(storyId, uid, true)

        scope.launch {
            val success = storyRepository.setStoryLike(storyId, true)
            if (success) {
                removePendingAction(storyId, uid, true)
            }
        }
        return true
    }

    /**
     * Drains the pending queue and updates the Firestore database when internet is available.
     */
    suspend fun syncPendingQueue() = syncMutex.withLock {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withLock
        val queue = getPendingQueue(uid)
        if (queue.isEmpty()) return@withLock

        Log.d(TAG, "Draining pending story like queue: ${queue.size} items to sync.")
        val remaining = mutableListOf<PendingStoryLikeAction>()

        for (action in queue) {
            val success = storyRepository.setStoryLike(action.storyId, action.targetLiked)
            if (!success) {
                // If it fails (still no connection / high latency), keep in queue for next cycle
                remaining.add(action)
            } else {
                Log.d(TAG, "Synced pending like for story ${action.storyId} (isLiked=${action.targetLiked})")
            }
        }

        savePendingQueue(uid, remaining)
        Log.d(TAG, "Pending sync queue update complete. Remaining unsynced: ${remaining.size}")
    }

    /**
     * Merges remote story groups from Firestore into the local state.
     * Guarantees that local pending actions and cached likes are never overwritten by stale responses.
     */
    fun mergeRemoteStories(groups: List<UserStoryGroup>, userId: String? = null) {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return

        val remoteLiked = mutableSetOf<String>()
        groups.forEach { group ->
            group.stories.forEach { story ->
                if (story.likedBy.contains(uid)) {
                    remoteLiked.add(story.id)
                }
            }
        }

        val cachedLiked = getCachedLikedStoryIds(uid)
        val pendingQueue = getPendingQueue(uid)
        val pendingMap = pendingQueue.associate { it.storyId to it.targetLiked }

        // Start with combined cached + remote
        val merged = (cachedLiked + remoteLiked).toMutableSet()

        // Apply pending mutations on top
        pendingMap.forEach { (storyId, isLiked) ->
            if (isLiked) {
                merged.add(storyId)
            } else {
                merged.remove(storyId)
            }
        }

        _likedStoryIds.value = merged
        saveCachedLikedStoryIds(uid, merged)

        if (pendingQueue.isNotEmpty()) {
            scope.launch {
                syncPendingQueue()
            }
        }
    }

    // --- LocalStorage Helpers ---

    private fun getCachedLikedStoryIds(userId: String): Set<String> {
        val ctx = getContext() ?: return emptySet()
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_CACHED_LIKES_PREFIX + userId, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveCachedLikedStoryIds(userId: String, ids: Set<String>) {
        val ctx = getContext() ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_CACHED_LIKES_PREFIX + userId, ids).apply()
    }

    private fun getPendingQueue(userId: String): List<PendingStoryLikeAction> {
        val ctx = getContext() ?: return emptyList()
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_QUEUE_PREFIX + userId, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingStoryLikeAction>>() {}.type
            gson.fromJson<List<PendingStoryLikeAction>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding pending queue JSON", e)
            emptyList()
        }
    }

    private fun savePendingQueue(userId: String, queue: List<PendingStoryLikeAction>) {
        val ctx = getContext() ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val json = gson.toJson(queue)
            prefs.edit().putString(KEY_PENDING_QUEUE_PREFIX + userId, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving pending queue JSON", e)
        }
    }

    private fun recordPendingAction(storyId: String, userId: String, targetLiked: Boolean) {
        val currentQueue = getPendingQueue(userId).toMutableList()
        // Remove any prior pending action for this story
        currentQueue.removeAll { it.storyId == storyId }
        currentQueue.add(PendingStoryLikeAction(storyId, userId, targetLiked))
        savePendingQueue(userId, currentQueue)
    }

    private fun removePendingAction(storyId: String, userId: String, targetLiked: Boolean) {
        val currentQueue = getPendingQueue(userId).toMutableList()
        val modified = currentQueue.removeAll { it.storyId == storyId && it.targetLiked == targetLiked }
        if (modified) {
            savePendingQueue(userId, currentQueue)
        }
    }
}
