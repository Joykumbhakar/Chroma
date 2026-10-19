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
                _works.value = recentWorks

                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    coroutineScope {
                        val likedAsync = async {
                            val likedSet = mutableSetOf<String>()
                            val results = recentWorks.map { work ->
                                async { if (repository.hasLiked(work.id, currentUser.uid)) work.id else null }
                            }.awaitAll()
                            likedSet.addAll(results.filterNotNull())
                            likedSet
                        }
                        val savedAsync = async {
                            repository.getSavedWorkIds(currentUser.uid)
                        }
                        _likedWorkIds.value = likedAsync.await()
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
                _trendingWorks.value = repository.getTrendingWorks(5)
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
                    if (currentUser != null) {
                        val likedSet = _likedWorkIds.value.toMutableSet()
                        val results = newWorks.map { work ->
                            async { if (repository.hasLiked(work.id, currentUser.uid)) work.id else null }
                        }.awaitAll()
                        likedSet.addAll(results.filterNotNull())
                        _likedWorkIds.value = likedSet
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
                    if (it.id == workId) it.copy(likesCount = maxOf(0, it.likesCount + if (nextLiked) 1 else -1)) else it
                }
                _trendingWorks.value = _trendingWorks.value.map {
                    if (it.id == workId) it.copy(likesCount = maxOf(0, it.likesCount + if (nextLiked) 1 else -1)) else it
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
        
        _works.value = _works.value.map {
            if (it.id == workId && !it.remixedBy.contains(uid)) {
                it.copy(remixCount = it.remixCount + 1, remixedBy = it.remixedBy + uid)
            } else it
        }
        _trendingWorks.value = _trendingWorks.value.map {
            if (it.id == workId && !it.remixedBy.contains(uid)) {
                it.copy(remixCount = it.remixCount + 1, remixedBy = it.remixedBy + uid)
            } else it
        }
        viewModelScope.launch {
            repository.incrementRemixCount(workId, uid)
        }
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
            repository.incrementShareCount(workId, uid)
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
            repository.incrementDownloadCount(workId, uid)
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
        viewModelScope.launch {
            try {
                repository.archivePost(workId, true)
                _works.value = _works.value.filter { it.id != workId }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun unarchiveWork(workId: String, creatorId: String) {
        viewModelScope.launch {
            try {
                repository.archivePost(workId, false)
                _archivedWorks.value = _archivedWorks.value.filter { it.id != workId }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
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

    fun recordShare(workId: String, userId: String) {
        trackShare(workId)
        viewModelScope.launch {
            repository.recordShare(workId, userId)
        }
    }
}
