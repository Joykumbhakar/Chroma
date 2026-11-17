package com.chroma.studio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.data.DiscoverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class DiscoverViewModel : ViewModel() {
    val repository = DiscoverRepository()

    companion object {
        val SAMPLE_PLACEHOLDER_POSTS = emptyList<DiscoverPost>()
    }

    private val _works = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val works: StateFlow<List<DiscoverPost>> = _works.asStateFlow()

    private val _trendingWorks = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val trendingWorks: StateFlow<List<DiscoverPost>> = _trendingWorks.asStateFlow()

    private val _archivedWorks = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val archivedWorks: StateFlow<List<DiscoverPost>> = _archivedWorks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _likedWorkIds = MutableStateFlow<Set<String>>(emptySet())
    val likedWorkIds: StateFlow<Set<String>> = _likedWorkIds.asStateFlow()

    private val _savedWorkIds = MutableStateFlow<Set<String>>(emptySet())
    val savedWorkIds: StateFlow<Set<String>> = _savedWorkIds.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    private var hasMoreWorks = true

    init {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val cached = getCachedLikedWorkIds(currentUid)
            if (cached.isNotEmpty()) {
                _likedWorkIds.value = cached
            }
        }
        loadWorks()
        loadTrending()
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                refreshUserInteractions(uid)
            } else {
                resetUserInteractions()
            }
        }
    }

    private fun getCachedLikedWorkIds(userId: String): Set<String> {
        return try {
            val ctx = com.chroma.studio.ChromaApp.instance
            val prefs = ctx.getSharedPreferences("chroma_community_likes_cache", android.content.Context.MODE_PRIVATE)
            (prefs.getStringSet("cached_liked_works_$userId", emptySet()) ?: emptySet()).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveCachedLikedWorkIds(userId: String, likedIds: Set<String>) {
        try {
            val ctx = com.chroma.studio.ChromaApp.instance
            val prefs = ctx.getSharedPreferences("chroma_community_likes_cache", android.content.Context.MODE_PRIVATE)
            prefs.edit().putStringSet("cached_liked_works_$userId", HashSet(likedIds)).apply()
        } catch (_: Exception) {}
    }

    /**
     * Reconciles authoritative server likes with any currently in-flight local optimistic like mutations.
     * Ensures stale cached likes are properly removed when unliked, while never clobbering active user taps.
     */
    private fun reconcileLikedIdsWithInFlight(serverTruth: Set<String>): Set<String> {
        val result = serverTruth.toMutableSet()
        val allKnownIds = result + _likedWorkIds.value + _works.value.map { it.id }
        for (id in allKnownIds) {
            val key = "like:$id"
            if (mutationCoordinator.isInFlight(key)) {
                val desired = mutationCoordinator.getDesiredState<Boolean>(key)
                if (desired == true) result.add(id)
                else if (desired == false) result.remove(id)
            }
        }
        return result
    }

    private fun applyLikeStateToPost(post: DiscoverPost, uid: String, liked: Boolean): DiscoverPost {
        val wasLikedInPost = post.likedBy.contains(uid)
        val newLikedBy = if (liked) (post.likedBy + uid).distinct() else (post.likedBy - uid)
        val baseLikes = maxOf(post.likesCount, post.likedBy.size)
        val newLikesCount = when {
            liked && !wasLikedInPost -> maxOf(baseLikes + 1, newLikedBy.size)
            !liked && wasLikedInPost -> maxOf(0, baseLikes - 1)
            else -> maxOf(baseLikes, newLikedBy.size)
        }
        return post.copy(
            likesCount = newLikesCount,
            likedBy = newLikedBy
        )
    }

    fun resetUserInteractions() {
        _likedWorkIds.value = emptySet()
        _savedWorkIds.value = emptySet()
        com.chroma.studio.data.mutation.OptimisticFollowManager.reset()
        com.chroma.studio.data.mutation.FollowingLocalStorageManager.clear()
    }

    fun refreshUserInteractions(userId: String) {
        viewModelScope.launch {
            try {
                // Force Firebase Auth token refresh so Firestore security rules have a valid token
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .currentUser?.getIdToken(true)?.await()
                } catch (_: Exception) {}

                val cached = getCachedLikedWorkIds(userId)
                val currentWorks = _works.value
                val instantLiked = currentWorks.filter { it.likedBy.contains(userId) }.map { it.id }.toSet()
                _likedWorkIds.value = cached + instantLiked

                coroutineScope {
                    val serverLikedAsync = async {
                        repository.getLikedWorkIds(userId)
                    }
                    val savedAsync = async {
                        repository.getSavedWorkIds(userId)
                    }
                    val followingAsync = async {
                        try {
                            val list = repository.getFollowing(userId)
                            if (list.isNotEmpty()) {
                                com.chroma.studio.data.mutation.FollowingLocalStorageManager.saveFollowingUsers(null, userId, list)
                            }
                            list.map { it.uid }.toSet()
                        } catch (_: Exception) {
                            emptySet()
                        }
                    }
                    val remoteLiked = serverLikedAsync.await()
                    val latestInstantLiked = _works.value.filter { it.likedBy.contains(userId) }.map { it.id }.toSet()
                    val totalLiked = reconcileLikedIdsWithInFlight(remoteLiked + latestInstantLiked)
                    _likedWorkIds.value = totalLiked
                    saveCachedLikedWorkIds(userId, totalLiked)
                    _savedWorkIds.value = savedAsync.await()
                    val followingSet = followingAsync.await()
                    if (followingSet.isNotEmpty()) {
                        com.chroma.studio.data.mutation.OptimisticFollowManager.seedFollowing(followingSet, userId)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun addOptimisticPost(post: DiscoverPost) {
        val current = _works.value
        _works.value = listOf(post) + current.filter { it.id != post.id }
    }

    fun removeOptimisticPost(postId: String) {
        _works.value = _works.value.filter { it.id != postId }
    }

    fun loadWorks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else if (_works.value.isEmpty()) {
                _isLoading.value = true
            }
            hasMoreWorks = true
            try {
                val recentWorks = repository.getRecentWorks()
                if (recentWorks.size < 10) hasMoreWorks = false
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                // Preserve any in-flight optimistic like mutations on freshly fetched works
                val reconciledRecent = if (!currentUid.isNullOrBlank()) {
                    recentWorks.map { w ->
                        val key = "like:${w.id}"
                        if (mutationCoordinator.isInFlight(key)) {
                            val desired = mutationCoordinator.getDesiredState<Boolean>(key)
                            if (desired != null) applyLikeStateToPost(w, currentUid, desired) else w
                        } else w
                    }
                } else recentWorks
                val pendingOptimistic = _works.value.filter { it.id.startsWith("temp_") }
                val combined = (pendingOptimistic + reconciledRecent).distinctBy { it.id }
                _works.value = combined

                if (!currentUid.isNullOrBlank()) {
                    val instantLiked = reconciledRecent.filter { it.likedBy.contains(currentUid) }.map { it.id }.toSet()
                    val loadedIds = reconciledRecent.map { it.id }.toSet()
                    // Reconcile loaded items immediately without resurrecting unliked posts
                    val preRemoteTruth = (_likedWorkIds.value - loadedIds) + instantLiked
                    val mergedLikes = reconcileLikedIdsWithInFlight(preRemoteTruth)
                    _likedWorkIds.value = mergedLikes
                    saveCachedLikedWorkIds(currentUid, mergedLikes)

                    coroutineScope {
                        val serverLikedAsync = async {
                            repository.getLikedWorkIds(currentUid)
                        }
                        val savedAsync = async {
                            repository.getSavedWorkIds(currentUid)
                        }
                        val remoteLiked = serverLikedAsync.await()
                        val totalLiked = reconcileLikedIdsWithInFlight(remoteLiked + instantLiked)
                        _likedWorkIds.value = totalLiked
                        saveCachedLikedWorkIds(currentUid, totalLiked)
                        _savedWorkIds.value = savedAsync.await()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val remoteTrending = repository.getTrendingWorks(30)
                val localQualifying = _works.value.filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
                val combined = (localQualifying + remoteTrending).distinctBy { it.id }
                    .filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
                    .sortedByDescending { it.likesCount }
                _trendingWorks.value = combined
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    fun loadMoreWorks() {
        if (_isLoading.value || _isLoadingMore.value || !hasMoreWorks || _isRefreshing.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val lastTimestamp = _works.value.lastOrNull()?.timestamp
                val newWorks = repository.getRecentWorks(lastTimestamp = lastTimestamp)
                if (newWorks.isEmpty()) {
                    hasMoreWorks = false
                } else {
                    if (newWorks.size < 10) hasMoreWorks = false
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val reconciledNew = if (!currentUid.isNullOrBlank()) {
                        newWorks.map { w ->
                            val key = "like:${w.id}"
                            if (mutationCoordinator.isInFlight(key)) {
                                val desired = mutationCoordinator.getDesiredState<Boolean>(key)
                                if (desired != null) applyLikeStateToPost(w, currentUid, desired) else w
                            } else w
                        }
                    } else newWorks
                    _works.value = (_works.value + reconciledNew).distinctBy { it.id }
                    if (!currentUid.isNullOrBlank()) {
                        val instantLiked = reconciledNew.filter { it.likedBy.contains(currentUid) }.map { it.id }.toSet()
                        val newIds = reconciledNew.map { it.id }.toSet()
                        val updated = reconcileLikedIdsWithInFlight((_likedWorkIds.value - newIds) + instantLiked)
                        _likedWorkIds.value = updated
                        saveCachedLikedWorkIds(currentUid, updated)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private val mutationCoordinator = com.chroma.studio.data.mutation.OptimisticMutationCoordinator(viewModelScope)

    val followingUserIds = com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds
    val followerCountDeltas = com.chroma.studio.data.mutation.OptimisticFollowManager.followerCountDeltas

    fun toggleLike(workId: String, currentUserId: String? = null, forceLike: Boolean = false) {
        val uid = if (!currentUserId.isNullOrBlank()) currentUserId else com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) return

        val isLikedNow = _likedWorkIds.value.contains(workId)

        if (forceLike && isLikedNow) {
            return // Already liked, skip toggle
        }

        val targetLiked = if (forceLike) true else !isLikedNow

        mutationCoordinator.mutate(
            key = "like:$workId",
            desiredState = targetLiked,
            onOptimisticApply = { nextLiked ->
                val newLikedIds = if (nextLiked) _likedWorkIds.value + workId else _likedWorkIds.value - workId
                _likedWorkIds.value = newLikedIds
                saveCachedLikedWorkIds(uid, newLikedIds)

                _works.value = _works.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, nextLiked) else it
                }
                _archivedWorks.value = _archivedWorks.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, nextLiked) else it
                }
                val updatedTrending = (_trendingWorks.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, nextLiked) else it
                } + _works.value).distinctBy { it.id }
                    .filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
                    .sortedByDescending { it.likesCount }
                _trendingWorks.value = updatedTrending
                _likedWorks.value = if (!nextLiked) {
                    _likedWorks.value.filter { it.id != workId }
                } else {
                    _likedWorks.value.map {
                        if (it.id == workId) applyLikeStateToPost(it, uid, true) else it
                    }
                }
            },
            performServerSync = { nextLiked ->
                repository.setLike(workId, uid, nextLiked)
            },
            onRollback = {
                // Isolated, safe per-workId rollback! Never wipes out other concurrent likes!
                val restoredLiked = !targetLiked
                val restoredLikedIds = if (restoredLiked) _likedWorkIds.value + workId else _likedWorkIds.value - workId
                _likedWorkIds.value = restoredLikedIds
                saveCachedLikedWorkIds(uid, restoredLikedIds)

                _works.value = _works.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, restoredLiked) else it
                }
                _archivedWorks.value = _archivedWorks.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, restoredLiked) else it
                }
                val updatedTrending = (_trendingWorks.value.map {
                    if (it.id == workId) applyLikeStateToPost(it, uid, restoredLiked) else it
                } + _works.value).distinctBy { it.id }
                    .filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
                    .sortedByDescending { it.likesCount }
                _trendingWorks.value = updatedTrending
            },
            onError = {
                com.chroma.studio.ui.components.ToastManager.showToast("Could not update like. Please try again.")
            }
        )
    }

    fun toggleSave(workId: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val isSavedNow = _savedWorkIds.value.contains(workId)
        val targetSaved = !isSavedNow
        val prevSavedSet = _savedWorkIds.value
        val prevWorks = _works.value
        val prevTrending = _trendingWorks.value
        val prevSavedWorks = _savedWorks.value

        mutationCoordinator.mutate(
            key = "save:$workId",
            desiredState = targetSaved,
            onOptimisticApply = { nextSaved ->
                _savedWorkIds.value = if (nextSaved) _savedWorkIds.value + workId else _savedWorkIds.value - workId
                _works.value = _works.value.map {
                    if (it.id == workId) {
                        val newSavedBy = if (nextSaved) it.savedBy + currentUser.uid else it.savedBy - currentUser.uid
                        it.copy(saveCount = maxOf(0, it.saveCount + if (nextSaved) 1 else -1), savedBy = newSavedBy)
                    } else it
                }
                _trendingWorks.value = _trendingWorks.value.map {
                    if (it.id == workId) {
                        val newSavedBy = if (nextSaved) it.savedBy + currentUser.uid else it.savedBy - currentUser.uid
                        it.copy(saveCount = maxOf(0, it.saveCount + if (nextSaved) 1 else -1), savedBy = newSavedBy)
                    } else it
                }
                if (!nextSaved) {
                    _savedWorks.value = _savedWorks.value.filter { it.id != workId }
                }
            },
            performServerSync = { nextSaved ->
                repository.setSave(workId, currentUser.uid, nextSaved)
            },
            onRollback = {
                _savedWorkIds.value = prevSavedSet
                _works.value = prevWorks
                _trendingWorks.value = prevTrending
                _savedWorks.value = prevSavedWorks
            },
            onError = {
                com.chroma.studio.ui.components.ToastManager.showToast("Could not update saved post. Please try again.")
            }
        )
    }

    fun toggleFollow(targetUserId: String, currentUserId: String? = null, baseFollowing: Boolean? = null) {
        val uid = if (!currentUserId.isNullOrBlank()) currentUserId else com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.chroma.studio.data.mutation.OptimisticFollowManager.toggleFollow(
            currentUserId = uid,
            targetUserId = targetUserId,
            baseFollowing = baseFollowing,
            onError = {
                com.chroma.studio.ui.components.ToastManager.showToast("Failed to update follow status")
            }
        )
    }

    fun repostWithRemix(
        originalPost: DiscoverPost,
        remixTitle: String,
        remixDescription: String,
        onSuccess: ((DiscoverPost) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val remixedPost = repository.repostWithRemix(
                    originalPost = originalPost,
                    remixTitle = remixTitle,
                    remixDescription = remixDescription
                )
                addOptimisticPost(remixedPost)
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (!currentUid.isNullOrBlank()) {
                    _works.value = _works.value.map {
                        if (it.id == originalPost.id && !it.remixedBy.contains(currentUid)) {
                            it.copy(
                                remixCount = it.remixCount + 1,
                                remixedBy = (it.remixedBy + currentUid).distinct()
                            )
                        } else it
                    }
                }
                onSuccess?.invoke(remixedPost)
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to repost remix")
            }
        }
    }

    fun trackRemix(workId: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentWork = _works.value.firstOrNull { it.id == workId }
        val alreadyRemixed = currentWork?.remixedBy?.contains(uid) == true
        if (alreadyRemixed) return

        val prevWorks = _works.value
        val prevTrending = _trendingWorks.value

        mutationCoordinator.mutate(
            key = "remix:$workId",
            desiredState = true,
            onOptimisticApply = {
                _works.value = _works.value.map {
                    if (it.id == workId && !it.remixedBy.contains(uid)) {
                        it.copy(
                            remixCount = it.remixCount + 1,
                            remixedBy = (it.remixedBy + uid).distinct()
                        )
                    } else it
                }
                _trendingWorks.value = _trendingWorks.value.map {
                    if (it.id == workId && !it.remixedBy.contains(uid)) {
                        it.copy(
                            remixCount = it.remixCount + 1,
                            remixedBy = (it.remixedBy + uid).distinct()
                        )
                    } else it
                }
            },
            performServerSync = {
                repository.incrementRemixCount(workId, uid)
                true
            },
            onRollback = {
                _works.value = prevWorks
                _trendingWorks.value = prevTrending
            }
        )
    }

    fun trackShare(workId: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        _works.value = _works.value.map {
            if (it.id == workId && !it.sharedBy.contains(uid)) {
                it.copy(shareCount = it.shareCount + 1, sharedBy = it.sharedBy + uid)
            } else it
        }
        _trendingWorks.value = _trendingWorks.value.map {
            if (it.id == workId && !it.sharedBy.contains(uid)) {
                it.copy(shareCount = it.shareCount + 1, sharedBy = it.sharedBy + uid)
            } else it
        }
        viewModelScope.launch {
            if (!workId.startsWith("placeholder_")) {
                repository.incrementShareCount(workId, uid)
            }
        }
    }

    fun trackDownload(workId: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        _works.value = _works.value.map {
            if (it.id == workId && !it.downloadedBy.contains(uid)) {
                it.copy(downloadCount = it.downloadCount + 1, downloadedBy = it.downloadedBy + uid)
            } else it
        }
        _trendingWorks.value = _trendingWorks.value.map {
            if (it.id == workId && !it.downloadedBy.contains(uid)) {
                it.copy(downloadCount = it.downloadCount + 1, downloadedBy = it.downloadedBy + uid)
            } else it
        }
        viewModelScope.launch {
            if (!workId.startsWith("placeholder_")) {
                repository.incrementDownloadCount(workId, uid)
            }
        }
    }

    fun updateCounterVisibility(
        postId: String,
        hideLikesCount: Boolean? = null,
        hideSharesCount: Boolean? = null,
        hideRemixesCount: Boolean? = null,
        hideDownloadsCount: Boolean? = null,
        hideSavesCount: Boolean? = null
    ) {
        _works.value = _works.value.map { post ->
            if (post.id == postId) {
                post.copy(
                    hideLikesCount = hideLikesCount ?: post.hideLikesCount,
                    hideSharesCount = hideSharesCount ?: post.hideSharesCount,
                    hideRemixesCount = hideRemixesCount ?: post.hideRemixesCount,
                    hideDownloadsCount = hideDownloadsCount ?: post.hideDownloadsCount,
                    hideSavesCount = hideSavesCount ?: post.hideSavesCount
                )
            } else post
        }
        viewModelScope.launch {
            repository.updatePostCounterVisibility(
                postId = postId,
                hideLikesCount = hideLikesCount,
                hideSharesCount = hideSharesCount,
                hideRemixesCount = hideRemixesCount,
                hideDownloadsCount = hideDownloadsCount,
                hideSavesCount = hideSavesCount
            )
        }
    }

    fun reportWork(workId: String, reason: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch { repository.reportWork(workId, currentUser.uid, reason) }
    }

    fun loadArchivedWorks(creatorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _archivedWorks.value = repository.getArchivedWorksByUser(creatorId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun archiveWork(workId: String) {
        val prevWorks = _works.value
        val targetWork = prevWorks.firstOrNull { it.id == workId } ?: return
        val prevArchived = _archivedWorks.value

        // 1. Optimistic instant local apply
        _works.value = prevWorks.filter { it.id != workId }
        _archivedWorks.value = listOf(targetWork) + prevArchived.filter { it.id != workId }

        viewModelScope.launch {
            try {
                repository.archivePost(workId, true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _works.value = prevWorks
                _archivedWorks.value = prevArchived
                com.chroma.studio.ui.components.ToastManager.showToast("Could not archive post. Please try again.")
            }
        }
    }

    fun unarchiveWork(workId: String, creatorId: String) {
        val prevArchived = _archivedWorks.value
        val targetWork = prevArchived.firstOrNull { it.id == workId } ?: return
        val prevWorks = _works.value

        // 1. Optimistic instant local apply
        _archivedWorks.value = prevArchived.filter { it.id != workId }
        _works.value = listOf(targetWork) + prevWorks.filter { it.id != workId }

        viewModelScope.launch {
            try {
                repository.archivePost(workId, false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _archivedWorks.value = prevArchived
                _works.value = prevWorks
                com.chroma.studio.ui.components.ToastManager.showToast("Could not unarchive post. Please try again.")
            }
        }
    }

    fun deleteWork(workId: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (workId.isBlank()) return

        // Optimistic UI update across all active feeds and activity history states
        _works.value = _works.value.filter { it.id != workId }
        _trendingWorks.value = _trendingWorks.value.filter { it.id != workId }
        _archivedWorks.value = _archivedWorks.value.filter { it.id != workId }
        _likedWorks.value = _likedWorks.value.filter { it.id != workId }
        _savedWorks.value = _savedWorks.value.filter { it.id != workId }
        _sharedWorks.value = _sharedWorks.value.filter { it.id != workId }
        _likedWorkIds.value = _likedWorkIds.value - workId
        _savedWorkIds.value = _savedWorkIds.value - workId

        viewModelScope.launch {
            val success = repository.deleteWork(workId)
            if (success) {
                com.chroma.studio.ui.components.ToastManager.showToast("Post deleted")
            } else {
                com.chroma.studio.ui.components.ToastManager.showToast("Failed to delete post")
                loadWorks(isRefresh = true)
            }
            onComplete?.invoke(success)
        }
    }

    fun updateWork(workId: String, newTitle: String, newDescription: String) {
        _works.value = _works.value.map {
            if (it.id == workId) it.copy(title = newTitle, description = newDescription) else it
        }
        viewModelScope.launch {
            val success = repository.updateWork(workId, newTitle, newDescription)
            if (!success) loadWorks(isRefresh = true)
        }
    }

    // ── Activity History State & Loaders ──────────────────────────────────────
    private val _likedWorks = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val likedWorks: StateFlow<List<DiscoverPost>> = _likedWorks.asStateFlow()

    private val _savedWorks = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val savedWorks: StateFlow<List<DiscoverPost>> = _savedWorks.asStateFlow()

    private val _sharedWorks = MutableStateFlow<List<DiscoverPost>>(emptyList())
    val sharedWorks: StateFlow<List<DiscoverPost>> = _sharedWorks.asStateFlow()

    private val _isActivityHistoryLoading = MutableStateFlow(false)
    val isActivityHistoryLoading: StateFlow<Boolean> = _isActivityHistoryLoading.asStateFlow()

    fun loadLikedWorks(userId: String) {
        viewModelScope.launch {
            _isActivityHistoryLoading.value = true
            try {
                val list = repository.getLikedWorks(userId)
                _likedWorks.value = list
                val currentSet = _likedWorkIds.value.toMutableSet()
                currentSet.addAll(list.map { it.id })
                _likedWorkIds.value = currentSet
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isActivityHistoryLoading.value = false
            }
        }
    }

    fun loadSavedWorks(userId: String) {
        viewModelScope.launch {
            _isActivityHistoryLoading.value = true
            try {
                val list = repository.getSavedWorks(userId)
                _savedWorks.value = list
                val currentSet = _savedWorkIds.value.toMutableSet()
                currentSet.addAll(list.map { it.id })
                _savedWorkIds.value = currentSet
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isActivityHistoryLoading.value = false
            }
        }
    }

    fun loadSharedWorks(userId: String) {
        viewModelScope.launch {
            _isActivityHistoryLoading.value = true
            try {
                _sharedWorks.value = repository.getSharedWorks(userId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                _isActivityHistoryLoading.value = false
            }
        }
    }

    suspend fun updateUserProfile(updatedProfile: com.chroma.studio.model.UserProfile): Result<Unit> {
        return repository.updateUserProfile(updatedProfile)
    }

    fun recordShare(workId: String, userId: String) {
        trackShare(workId)
        viewModelScope.launch {
            repository.recordShare(workId, userId)
        }
    }
}
