package com.chroma.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.FlashRepository
import com.chroma.studio.model.FlashComment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlashCommentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashRepository()
    private val auth = FirebaseAuth.getInstance()

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
                _comments.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addComment(videoId: String, text: String, replyToId: String? = null, replyFlashUrl: String? = null) {
        if (text.isBlank() && replyFlashUrl == null) return
        viewModelScope.launch {
            try {
                val newComment = repository.addComment(videoId, text, replyToId, replyFlashUrl)
                _comments.value = _comments.value + newComment
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleLike(commentId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val nowLiked = repository.toggleCommentLike(commentId, userId)
                _comments.value = _comments.value.map { c ->
                    if (c.id == commentId) {
                        val newLikedBy = if (nowLiked) c.likedBy + userId else c.likedBy - userId
                        c.copy(likedBy = newLikedBy, likesCount = newLikedBy.size)
                    } else {
                        c
                    }
                }
            } catch (e: Exception) {
                // error
            }
        }
    }
}
