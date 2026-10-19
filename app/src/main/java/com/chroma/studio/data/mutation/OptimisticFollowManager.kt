package com.chroma.studio.data.mutation

import com.chroma.studio.data.DiscoverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single Source of Truth for Follow / Unfollow across the entire application.
 * Ensures that toggling follow in UserProfileScreen, UserListScreen, or Creator modals
 * instantaneously updates buttons and counts everywhere, coalesces rapid taps,
 * and rolls back gracefully on error.
 */
object OptimisticFollowManager {
    private val coordinator = OptimisticMutationCoordinator()

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

    fun seedFollowing(userIds: Set<String>) {
        _followingUserIds.value = _followingUserIds.value + userIds
    }

    fun isFollowing(userId: String): Boolean {
        return _followingUserIds.value.contains(userId)
    }

    fun getFollowerDelta(userId: String): Int {
        return _followerCountDeltas.value[userId] ?: 0
    }

    fun toggleFollow(
        currentUserId: String,
        targetUserId: String,
        baseFollowing: Boolean? = null,
        onError: ((Throwable?) -> Unit)? = null
    ) {
        if (currentUserId.isBlank() || targetUserId.isBlank() || currentUserId == targetUserId) return

        val currentlyFollowing = baseFollowing ?: _followingUserIds.value.contains(targetUserId)
        val desiredFollowing = !currentlyFollowing
        val mutationKey = "follow:$currentUserId:$targetUserId"

        val prevFollowingSet = _followingUserIds.value
        val prevDeltas = _followerCountDeltas.value

        coordinator.mutate(
            key = mutationKey,
            desiredState = desiredFollowing,
            onOptimisticApply = { nextFollowing ->
                // Immediate update across all observing screens
                if (nextFollowing) {
                    _followingUserIds.value = _followingUserIds.value + targetUserId
                    val currentDelta = _followerCountDeltas.value[targetUserId] ?: 0
                    _followerCountDeltas.value = _followerCountDeltas.value + (targetUserId to (currentDelta + 1))
                } else {
                    _followingUserIds.value = _followingUserIds.value - targetUserId
                    val currentDelta = _followerCountDeltas.value[targetUserId] ?: 0
                    _followerCountDeltas.value = _followerCountDeltas.value + (targetUserId to (currentDelta - 1))
                }
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
                _followerCountDeltas.value = prevDeltas
            },
            onError = onError
        )
    }
}
