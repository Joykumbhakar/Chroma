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
        loadWorks()
        loadTrending()
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                refreshUserInteractions(uid)
            }
        }
    }

    fun refreshUserInteractions(userId: String) {
        viewModelScope.launch {
            try {
                val currentWorks = _works.value
                val instantLiked = currentWorks.filter { it.likedBy.contains(userId) }.map { it.id }.toSet()
                _likedWorkIds.value = _likedWorkIds.value + instantLiked

                coroutineScope {
                    val likedAsync = async {
                        val unconfirmedWorks = currentWorks.filter { !it.likedBy.contains(userId) }
                        val extraLikes = unconfirmedWorks.map { work ->
                            async { if (repository.hasLiked(work.id, userId)) work.id else null }
                        }.awaitAll().filterNotNull()
                        instantLiked + extraLikes
                    }
                    val savedAsync = async {
                        repository.getSavedWorkIds(userId)
                    }
                    _likedWorkIds.value = _likedWorkIds.value + likedAsync.await()
                    _savedWorkIds.value = savedAsync.await()
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
                val pendingOptimistic = _works.value.filter { it.id.startsWith("temp_") }
                val combined = (pendingOptimistic + recentWorks).distinctBy { it.id }
                _works.value = combined

                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val currentUid = currentUser?.uid
                if (!currentUid.isNullOrBlank()) {
                    val instantLiked = recentWorks.filter { it.likedBy.contains(currentUid) }.map { it.id }.toSet()
                    _likedWorkIds.value = _likedWorkIds.value + instantLiked

                    coroutineScope {
                        val likedAsync = async {
                            val unconfirmedWorks = recentWorks.filter { !it.likedBy.contains(currentUid) }
                            val extraLikes = unconfirmedWorks.map { work ->
                                async { if (repository.hasLiked(work.id, currentUid)) work.id else null }
                            }.awaitAll().filterNotNull()
                            instantLiked + extraLikes
                        }
                        val savedAsync = async {
                            repository.getSavedWorkIds(currentUid)
                        }
                        _likedWorkIds.value = _likedWorkIds.value + likedAsync.await()
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
                val remoteTrending = repository.getTrendingWorks(5)
                val pendingOptimistic = _works.value.filter { it.id.startsWith("temp_") }
                val combined = (pendingOptimistic + remoteTrending).distinctBy { it.id }
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
                    _works.value = _works.value + newWorks
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val currentUid = currentUser?.uid
                    if (!currentUid.isNullOrBlank()) {
                        val instantLiked = newWorks.filter { it.likedBy.contains(currentUid) }.map { it.id }.toSet()
                        _likedWorkIds.value = _likedWorkIds.value + instantLiked

                        val unconfirmedWorks = newWorks.filter { !it.likedBy.contains(currentUid) }
                        if (unconfirmedWorks.isNotEmpty()) {
                            val results = unconfirmedWorks.map { work ->
                                async { if (repository.hasLiked(work.id, currentUid)) work.id else null }
                            }.awaitAll()
                            _likedWorkIds.value = _likedWorkIds.value + results.filterNotNull()
                        }
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
        val prevLikedSet = _likedWorkIds.value
        val prevWorks = _works.value
        val prevTrending = _trendingWorks.value
        val prevLikedWorks = _likedWorks.value

        mutationCoordinator.mutate(
            key = "like:$workId",
            desiredState = targetLiked,
            onOptimisticApply = { nextLiked ->
                _likedWorkIds.value = if (nextLiked) _likedWorkIds.value + workId else _likedWorkIds.value - workId
                _works.value = _works.value.map {
                    if (it.id == workId) {
                        val newLikedBy = if (nextLiked) (it.likedBy + uid).distinct() else (it.likedBy - uid)
                        it.copy(
                            likesCount = maxOf(0, it.likesCount + if (nextLiked) 1 else -1),
                            likedBy = newLikedBy
                        )
                    } else it
                }
                _trendingWorks.value = _trendingWorks.value.map {
                    if (it.id == workId) {
                        val newLikedBy = if (nextLiked) (it.likedBy + uid).distinct() else (it.likedBy - uid)
                        it.copy(
                            likesCount = maxOf(0, it.likesCount + if (nextLiked) 1 else -1),
                            likedBy = newLikedBy
                        )
                    } else it
                }
                if (!nextLiked) {
                    _likedWorks.value = _likedWorks.value.filter { it.id != workId }
                }
            },
            performServerSync = { nextLiked ->
                repository.setLike(workId, uid, nextLiked)
            },
            onRollback = {
                _likedWorkIds.value = prevLikedSet
                _works.value = prevWorks
                _trendingWorks.value = prevTrending
                _likedWorks.value = prevLikedWorks
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

    fun deleteWork(workId: String) {
        _works.value = _works.value.filter { it.id != workId }
        viewModelScope.launch {
            val success = repository.deleteWork(workId)
            if (!success) loadWorks(isRefresh = true)
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
