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

class FlashCommentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashRepository()
    private val auth = FirebaseAuth.getInstance()
    private val mutationCoordinator = OptimisticMutationCoordinator(viewModelScope)

    private val _comments = MutableStateFlow<List<FlashComment>>(emptyList())
    val comments: StateFlow<List<FlashComment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = repository.getComments(videoId)
                _comments.value = items
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _comments.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addComment(videoId: String, text: String, replyToId: String? = null, replyFlashUrl: String? = null) {
        if (text.isBlank() && replyFlashUrl == null) return
        val user = auth.currentUser ?: return

        val tempId = UUID.randomUUID().toString()
        val optimisticComment = FlashComment(
            id = tempId,
            videoId = videoId,
            userId = user.uid,
            username = user.displayName ?: "User",
            displayName = user.displayName ?: "User",
            avatarUrl = user.photoUrl?.toString() ?: "",
            verified = false,
            text = text,
            timestamp = System.currentTimeMillis(),
            replyToCommentId = replyToId,
            replyFlashVideoUrl = replyFlashUrl,
            isPending = true,
            isError = false
        )

        // 1. Immediately render in UI
        _comments.value = _comments.value + optimisticComment

        // 2. Synchronize in background
        viewModelScope.launch {
            try {
                val serverComment = repository.addComment(
                    videoId = videoId,
                    text = text,
                    replyToCommentId = replyToId,
                    replyFlashVideoUrl = replyFlashUrl,
                    preGeneratedId = tempId
                )
                // 3. Reconcile with server response
                _comments.value = _comments.value.map {
                    if (it.id == tempId) serverComment.copy(isPending = false, isError = false) else it
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // 4. Mark as failed with retry action
                _comments.value = _comments.value.map {
                    if (it.id == tempId) it.copy(isPending = false, isError = true) else it
                }
                ToastManager.showToast("Couldn't post comment. Tap to retry.")
            }
        }
    }

    fun retryComment(comment: FlashComment) {
        _comments.value = _comments.value.map {
            if (it.id == comment.id) it.copy(isPending = true, isError = false) else it
        }

        viewModelScope.launch {
            try {
                val serverComment = repository.addComment(
                    videoId = comment.videoId,
                    text = comment.text,
                    replyToCommentId = comment.replyToCommentId,
                    replyFlashVideoUrl = comment.replyFlashVideoUrl,
                    preGeneratedId = comment.id
                )
                _comments.value = _comments.value.map {
                    if (it.id == comment.id) serverComment.copy(isPending = false, isError = false) else it
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _comments.value = _comments.value.map {
                    if (it.id == comment.id) it.copy(isPending = false, isError = true) else it
                }
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
                        val newLikedBy = if (nextLiked) c.likedBy + userId else c.likedBy - userId
                        c.copy(likedBy = newLikedBy.distinct(), likesCount = maxOf(0, c.likesCount + if (nextLiked) 1 else -1))
                    } else c
                }
            },
            performServerSync = { nextLiked ->
                repository.setCommentLike(commentId, userId, nextLiked)
            },
            onRollback = {
                _comments.value = prevComments
            },
            onError = {
                ToastManager.showToast("Could not update comment like.")
            }
        )
    }
}
