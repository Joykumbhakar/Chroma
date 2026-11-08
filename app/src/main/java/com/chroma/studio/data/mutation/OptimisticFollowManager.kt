package com.chroma.studio.data.mutation

import android.content.Context
import android.util.Log
import com.chroma.studio.data.DiscoverRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single Source of Truth for Follow / Unfollow across the entire application.
 * Backed by [FollowingLocalStorageManager] for 0ms cold-start local persistence
 * so users never have to wait for the "Follow" button to turn to "Following".
 */
object OptimisticFollowManager {
    private const val TAG = "OptimisticFollowMgr"

    private val coordinator = OptimisticMutationCoordinator()

    private var appContext: Context? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var snapshotListenerRegistration: ListenerRegistration? = null
    private var activeUserId: String? = null

    /**
     * Optional custom sync handler for testing or custom sync logic.
     * If null, falls back to lazy instantiation of DiscoverRepository.
     */
    var followSyncHandler: (suspend (currentUserId: String, targetUserId: String, targetFollowing: Boolean) -> Boolean)? = null

    // Global set of userIds that the current logged-in user is following
    private val _followingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followingUserIds: StateFlow<Set<String>> = _followingUserIds.asStateFlow()

    // Follower count deltas relative to base server counts (targetUserId -> delta)
    private val _followerCountDeltas = MutableStateFlow<Map<String, Int>>(emptyMap())
    val followerCountDeltas: StateFlow<Map<String, Int>> = _followerCountDeltas.asStateFlow()

    /**
     * Initializes the follow manager with an application context, preloads local storage,
     * and attaches an auth state listener to maintain real-time Firestore synchronization.
     */
    fun initialize(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
            FollowingLocalStorageManager.preload(context)
        }
        try {
            val auth = FirebaseAuth.getInstance()
            if (authStateListener == null) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid
                    if (!uid.isNullOrBlank()) {
                        setupFollowingListener(uid)
                    } else {
                        cleanupFollowingListener()
                        reset()
                    }
                }
                authStateListener = listener
                auth.addAuthStateListener(listener)
            }
            val currentUid = auth.currentUser?.uid ?: FollowingLocalStorageManager.getActiveUid(appContext)
            if (!currentUid.isNullOrBlank() && activeUserId != currentUid) {
                setupFollowingListener(currentUid)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auth state listener initialization bypassed/deferred: ${e.message}")
        }
    }

    private fun setupFollowingListener(uid: String) {
        if (activeUserId == uid && snapshotListenerRegistration != null) return
        cleanupFollowingListener()
        activeUserId = uid

        // 1. Instant 0ms load from local storage to eliminate button lag completely
        val storedIds = FollowingLocalStorageManager.getFollowingIds(appContext, uid)
        if (storedIds.isNotEmpty()) {
            _followingUserIds.value = _followingUserIds.value + storedIds
        }

        // 2. Real-time Firestore snapshot listener on users/{uid}/following
        try {
            val db = FirebaseFirestore.getInstance()
            val followingRef = db.collection("users").document(uid).collection("following")
            snapshotListenerRegistration = followingRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to following for $uid: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val serverIds = snapshot.documents.map { it.id }.toSet()
                    _followingUserIds.value = serverIds
                    FollowingLocalStorageManager.saveFollowingIds(appContext, uid, serverIds)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach following snapshot listener: ${e.message}")
        }
    }

    fun cleanupFollowingListener() {
        try {
            snapshotListenerRegistration?.remove()
        } catch (_: Exception) {}
        snapshotListenerRegistration = null
        activeUserId = null
    }

    fun seedFollowing(userIds: Set<String>, currentUserId: String? = null) {
        val updated = _followingUserIds.value + userIds
        _followingUserIds.value = updated
        val uid = currentUserId ?: activeUserId ?: FollowingLocalStorageManager.getActiveUid(appContext)
        if (!uid.isNullOrBlank()) {
            FollowingLocalStorageManager.saveFollowingIds(appContext, uid, updated)
        }
    }

    fun removeFollowing(userId: String, currentUserId: String? = null) {
        val updated = _followingUserIds.value - userId
        _followingUserIds.value = updated
        val uid = currentUserId ?: activeUserId ?: FollowingLocalStorageManager.getActiveUid(appContext)
        if (!uid.isNullOrBlank()) {
            FollowingLocalStorageManager.removeFollowing(appContext, uid, userId)
        }
    }

    fun hasPendingMutation(targetUserId: String, currentUserId: String? = null): Boolean {
        val uid = currentUserId ?: activeUserId ?: try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) { null } ?: return false
        return coordinator.getDesiredState<Boolean>("follow:$uid:$targetUserId") != null
    }

    fun isFollowing(userId: String): Boolean {
        return _followingUserIds.value.contains(userId) || FollowingLocalStorageManager.isFollowing(userId, appContext, activeUserId)
    }

    fun getFollowerDelta(userId: String): Int {
        return _followerCountDeltas.value[userId] ?: 0
    }

    fun reset() {
        _followingUserIds.value = emptySet()
        _followerCountDeltas.value = emptyMap()
    }

    fun toggleFollow(
        currentUserId: String,
        targetUserId: String,
        baseFollowing: Boolean? = null,
        onError: ((Throwable?) -> Unit)? = null
    ) {
        if (currentUserId.isBlank() || targetUserId.isBlank() || currentUserId == targetUserId) return

        val currentlyFollowing = baseFollowing ?: isFollowing(targetUserId)
        val desiredFollowing = !currentlyFollowing
        val mutationKey = "follow:$currentUserId:$targetUserId"

        val prevFollowingSet = _followingUserIds.value
        val prevDeltas = _followerCountDeltas.value

        coordinator.mutate(
            key = mutationKey,
            desiredState = desiredFollowing,
            onOptimisticApply = { nextFollowing ->
                // Immediate update across all observing screens and local storage (0ms)
                val newSet = if (nextFollowing) {
                    FollowingLocalStorageManager.addFollowing(appContext, currentUserId, targetUserId)
                    _followingUserIds.value + targetUserId
                } else {
                    FollowingLocalStorageManager.removeFollowing(appContext, currentUserId, targetUserId)
                    _followingUserIds.value - targetUserId
                }
                _followingUserIds.value = newSet

                val currentDelta = _followerCountDeltas.value[targetUserId] ?: 0
                val deltaChange = if (nextFollowing) 1 else -1
                _followerCountDeltas.value = _followerCountDeltas.value + (targetUserId to (currentDelta + deltaChange))
            },
            performServerSync = { targetFollowing ->
                val handler = followSyncHandler
                if (handler != null) {
                    handler(currentUserId, targetUserId, targetFollowing)
                } else {
                    DiscoverRepository().setFollow(currentUserId, targetUserId, targetFollowing)
                }
            },
            onRollback = {
                _followingUserIds.value = prevFollowingSet
                FollowingLocalStorageManager.saveFollowingIds(appContext, currentUserId, prevFollowingSet)
                _followerCountDeltas.value = prevDeltas
            },
            onError = onError
        )
    }
}
