package com.chroma.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.FlashRepository
import com.chroma.studio.data.mutation.OptimisticMutationCoordinator
import com.chroma.studio.model.FlashComment
import com.chroma.studio.ui.components.ToastManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class CommentSortOrder {
    TOP_COMMENTS,
    NEWEST_FIRST
}

class FlashCommentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashRepository()
    private val auth = FirebaseAuth.getInstance()
    private val mutationCoordinator = OptimisticMutationCoordinator(viewModelScope)

    // Per-video comment cache to prevent cross-video flashes during vertical scrolling
    private val videoCommentsCache = ConcurrentHashMap<String, List<FlashComment>>()

    private val _currentVideoId = MutableStateFlow("")
    val currentVideoId: StateFlow<String> = _currentVideoId.asStateFlow()

    private val _comments = MutableStateFlow<List<FlashComment>>(emptyList())
    val comments: StateFlow<List<FlashComment>> = _comments.asStateFlow()

    private val _sortOrder = MutableStateFlow(CommentSortOrder.TOP_COMMENTS)
    val sortOrder: StateFlow<CommentSortOrder> = _sortOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var commentsJob: kotlinx.coroutines.Job? = null

    fun setSortOrder(order: CommentSortOrder) {
        _sortOrder.value = order
    }

    fun loadComments(videoId: String) {
        if (videoId.isBlank()) return

        commentsJob?.cancel()
        _currentVideoId.value = videoId

        // Immediate cache hit: avoid showing previous video's comments or blank state
        val cached = videoCommentsCache[videoId]
        if (cached != null) {
            _comments.value = cached
            _isLoading.value = false
        } else {
            _comments.value = emptyList()
            _isLoading.value = true
        }

        commentsJob = viewModelScope.launch {
            try {
                repository.observeComments(videoId).collect { serverComments ->
                    if (_currentVideoId.value != videoId) return@collect

                    // Keep active pending and error comments belonging specifically to this video
                    val currentList = _comments.value
                    val pending = currentList.filter { (it.isPending || it.isError) && it.videoId == videoId }
                    val serverIds = serverComments.map { it.id }.toSet()
                    val activePending = pending.filter { it.id !in serverIds }

                    val activeTopPending = activePending.filter { it.replyToCommentId.isNullOrBlank() }
                    val activeChildPending = activePending.filter { !it.replyToCommentId.isNullOrBlank() }

                    val merged = activeTopPending + serverComments + activeChildPending
                    _comments.value = merged
                    videoCommentsCache[videoId] = merged
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (_currentVideoId.value == videoId) {
                    if (videoCommentsCache[videoId] == null) {
                        _comments.value = emptyList()
                    }
                    _isLoading.value = false
                }
            }
        }
    }

    fun addComment(
        videoId: String,
        text: String,
        replyToId: String? = null,
        replyToUsername: String? = null,
        replyFlashUrl: String? = null
    ) {
        if (text.isBlank() && replyFlashUrl == null) return
        val user = auth.currentUser ?: return

        val tempId = UUID.randomUUID().toString()
        val rawHandle = user.displayName?.replace(" ", "")?.lowercase() ?: "user_${user.uid.take(5)}"
        val optimisticComment = FlashComment(
            id = tempId,
            videoId = videoId,
            userId = user.uid,
            username = rawHandle,
            displayName = user.displayName ?: rawHandle,
            avatarUrl = user.photoUrl?.toString().orEmpty(),
            verified = false,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            replyToCommentId = replyToId,
            replyToUsername = replyToUsername,
            replyFlashVideoUrl = replyFlashUrl,
            isPending = true,
            isError = false
        )

        // 1. Immediately render in UI
        if (replyToId.isNullOrBlank()) {
            // Top-level comment: prepend immediately so user sees it at the top
            _comments.value = listOf(optimisticComment) + _comments.value
        } else {
            // Reply: append and bump parent reply count optimistically
            _comments.value = _comments.value.map {
                if (it.id == replyToId) it.copy(replyCount = it.replyCount + 1) else it
            } + optimisticComment
        }
        videoCommentsCache[videoId] = _comments.value

        // 2. Synchronize in background
        viewModelScope.launch {
            try {
                val serverComment = repository.addComment(
                    videoId = videoId,
                    text = text.trim(),
                    replyToCommentId = replyToId,
                    replyToUsername = replyToUsername,
                    replyFlashVideoUrl = replyFlashUrl,
                    preGeneratedId = tempId
                )
                // 3. Reconcile with server response
                _comments.value = _comments.value.map {
                    if (it.id == tempId) serverComment.copy(isPending = false, isError = false) else it
                }
                videoCommentsCache[videoId] = _comments.value
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // 4. Mark as failed with retry action
                _comments.value = _comments.value.map {
                    if (it.id == tempId) it.copy(isPending = false, isError = true) else it
                }
                videoCommentsCache[videoId] = _comments.value
                ToastManager.showToast("Couldn't post comment. Tap to retry.")
            }
        }
    }

    fun retryComment(comment: FlashComment) {
        _comments.value = _comments.value.map {
            if (it.id == comment.id) it.copy(isPending = true, isError = false) else it
        }
        videoCommentsCache[comment.videoId] = _comments.value

        viewModelScope.launch {
            try {
                val serverComment = repository.addComment(
                    videoId = comment.videoId,
                    text = comment.text,
                    replyToCommentId = comment.replyToCommentId,
                    replyToUsername = comment.replyToUsername,
                    replyFlashVideoUrl = comment.replyFlashVideoUrl,
                    preGeneratedId = comment.id
                )
                _comments.value = _comments.value.map {
                    if (it.id == comment.id) serverComment.copy(isPending = false, isError = false) else it
                }
                videoCommentsCache[comment.videoId] = _comments.value
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _comments.value = _comments.value.map {
                    if (it.id == comment.id) it.copy(isPending = false, isError = true) else it
                }
                videoCommentsCache[comment.videoId] = _comments.value
                ToastManager.showToast("Retry failed. Please check your connection.")
            }
        }
    }

    fun toggleLike(commentId: String) {
        val userId = auth.currentUser?.uid ?: return
        val comment = _comments.value.firstOrNull { it.id == commentId } ?: return
        val isLikedNow = comment.likedBy.contains(userId)
        val targetLiked = !isLikedNow
        val prevComments = _comments.value

        mutationCoordinator.mutate(
            key = "comment_like:$commentId",
            desiredState = targetLiked,
            onOptimisticApply = { nextLiked ->
                _comments.value = _comments.value.map { c ->
                    if (c.id == commentId) {
                        val newLikedBy = if (nextLiked) (c.likedBy + userId).distinct() else c.likedBy - userId
                        val newDislikedBy = if (nextLiked) c.dislikedBy - userId else c.dislikedBy
                        c.copy(
                            likedBy = newLikedBy,
                            dislikedBy = newDislikedBy,
                            likesCount = maxOf(0, c.likesCount + if (nextLiked) 1 else -1)
                        )
                    } else c
                }
                videoCommentsCache[comment.videoId] = _comments.value
            },
            performServerSync = { nextLiked ->
                repository.setCommentLike(
                    commentId = commentId,
                    videoId = comment.videoId,
                    userId = userId,
                    targetLiked = nextLiked
                )
            },
            onRollback = {
                _comments.value = prevComments
                videoCommentsCache[comment.videoId] = prevComments
            },
            onError = {
                ToastManager.showToast("Could not update comment like.")
            }
        )
    }

    fun toggleDislike(commentId: String) {
        val userId = auth.currentUser?.uid ?: return
        val comment = _comments.value.firstOrNull { it.id == commentId } ?: return
        val isDislikedNow = comment.dislikedBy.contains(userId)
        val targetDisliked = !isDislikedNow
        val prevComments = _comments.value

        mutationCoordinator.mutate(
            key = "comment_dislike:$commentId",
            desiredState = targetDisliked,
            onOptimisticApply = { nextDisliked ->
                _comments.value = _comments.value.map { c ->
                    if (c.id == commentId) {
                        val wasLiked = c.likedBy.contains(userId)
                        val newDislikedBy = if (nextDisliked) (c.dislikedBy + userId).distinct() else c.dislikedBy - userId
                        val newLikedBy = if (nextDisliked) c.likedBy - userId else c.likedBy
                        val newLikesCount = if (nextDisliked && wasLiked) maxOf(0, c.likesCount - 1) else c.likesCount
                        c.copy(
                            dislikedBy = newDislikedBy,
                            likedBy = newLikedBy,
                            likesCount = newLikesCount
                        )
                    } else c
                }
                videoCommentsCache[comment.videoId] = _comments.value
            },
            performServerSync = { nextDisliked ->
                repository.setCommentDislike(
                    commentId = commentId,
                    videoId = comment.videoId,
                    userId = userId,
                    targetDisliked = nextDisliked
                )
            },
            onRollback = {
                _comments.value = prevComments
                videoCommentsCache[comment.videoId] = prevComments
            },
            onError = {
                ToastManager.showToast("Could not update dislike.")
            }
        )
    }

    fun togglePin(comment: FlashComment, videoId: String) {
        val targetPinned = !comment.isPinned
        val prevComments = _comments.value

        _comments.value = _comments.value.map { c ->
            if (c.id == comment.id) {
                c.copy(isPinned = targetPinned)
            } else if (targetPinned) {
                c.copy(isPinned = false)
            } else {
                c
            }
        }
        videoCommentsCache[videoId] = _comments.value

        viewModelScope.launch {
            val success = repository.setCommentPinned(comment.id, videoId, targetPinned)
            if (!success) {
                _comments.value = prevComments
                videoCommentsCache[videoId] = prevComments
                ToastManager.showToast("Failed to update pin.")
            } else {
                ToastManager.showToast(if (targetPinned) "Comment pinned" else "Comment unpinned")
            }
        }
    }

    fun toggleCreatorHeart(comment: FlashComment) {
        val targetHearted = !comment.isCreatorHearted
        val prevComments = _comments.value

        _comments.value = _comments.value.map { c ->
            if (c.id == comment.id) c.copy(isCreatorHearted = targetHearted) else c
        }
        videoCommentsCache[comment.videoId] = _comments.value

        viewModelScope.launch {
            val success = repository.setCommentCreatorHeart(
                commentId = comment.id,
                videoId = comment.videoId,
                isHearted = targetHearted
            )
            if (!success) {
                _comments.value = prevComments
                videoCommentsCache[comment.videoId] = prevComments
                ToastManager.showToast("Failed to heart comment.")
            }
        }
    }

    fun deleteComment(comment: FlashComment) {
        val prevComments = _comments.value
        // Optimistically remove comment and any nested replies, and decrement parent replyCount if replying
        _comments.value = _comments.value.filter { it.id != comment.id && it.replyToCommentId != comment.id }.map { c ->
            if (!comment.replyToCommentId.isNullOrBlank() && c.id == comment.replyToCommentId) {
                c.copy(replyCount = maxOf(0, c.replyCount - 1))
            } else {
                c
            }
        }
        videoCommentsCache[comment.videoId] = _comments.value

        viewModelScope.launch {
            val success = repository.deleteComment(
                commentId = comment.id,
                videoId = comment.videoId,
                replyToCommentId = comment.replyToCommentId
            )
            if (!success) {
                _comments.value = prevComments
                videoCommentsCache[comment.videoId] = prevComments
                ToastManager.showToast("Failed to delete comment.")
            } else {
                ToastManager.showToast("Comment deleted.")
            }
        }
    }

    fun editComment(commentId: String, newText: String) {
        val cleaned = newText.trim()
        if (cleaned.isEmpty()) return
        val target = _comments.value.firstOrNull { it.id == commentId } ?: return
        val prevComments = _comments.value

        _comments.value = _comments.value.map { c ->
            if (c.id == commentId) c.copy(text = cleaned, edited = true) else c
        }
        videoCommentsCache[target.videoId] = _comments.value

        viewModelScope.launch {
            val success = repository.editComment(
                commentId = commentId,
                videoId = target.videoId,
                newText = cleaned
            )
            if (!success) {
                _comments.value = prevComments
                videoCommentsCache[target.videoId] = prevComments
                ToastManager.showToast("Couldn't save edit.")
            }
        }
    }

    fun toggleReaction(commentId: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        val comment = _comments.value.firstOrNull { it.id == commentId } ?: return
        val existingUsers = comment.reactions[emoji].orEmpty()
        val desiredAdd = !existingUsers.contains(userId)
        val prevComments = _comments.value

        mutationCoordinator.mutate(
            key = "comment_react:$commentId:$emoji",
            desiredState = desiredAdd,
            onOptimisticApply = { shouldAdd ->
                _comments.value = _comments.value.map { c ->
                    if (c.id == commentId) {
                        val updatedMap = c.reactions.toMutableMap()
                        val list = updatedMap[emoji].orEmpty()
                        val nextList = if (shouldAdd) (list + userId).distinct() else list - userId
                        if (nextList.isEmpty()) updatedMap.remove(emoji) else updatedMap[emoji] = nextList
                        c.copy(reactions = updatedMap)
                    } else c
                }
                videoCommentsCache[comment.videoId] = _comments.value
            },
            performServerSync = { shouldAdd ->
                repository.setCommentReaction(
                    commentId = commentId,
                    videoId = comment.videoId,
                    userId = userId,
                    emoji = emoji,
                    add = shouldAdd
                )
            },
            onRollback = {
                _comments.value = prevComments
                videoCommentsCache[comment.videoId] = prevComments
            },
            onError = {
                ToastManager.showToast("Could not update reaction.")
            }
        )
    }

    fun reportComment(commentId: String, videoId: String, reason: String) {
        viewModelScope.launch {
            val success = repository.reportComment(commentId, videoId, reason)
            if (success) {
                ToastManager.showToast("Report submitted: $reason")
            } else {
                ToastManager.showToast("Couldn't submit report. Try again.")
            }
        }
    }
}
