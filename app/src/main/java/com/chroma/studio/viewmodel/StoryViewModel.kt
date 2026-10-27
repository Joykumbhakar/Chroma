package com.chroma.studio.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.CloudinaryClient
import com.chroma.studio.data.StoryRepository
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class StoryViewModel : ViewModel() {
    private val repository = StoryRepository()
    private val auth = FirebaseAuth.getInstance()

    // Feed stories (from followed users)
    private val _storyGroups = MutableStateFlow<List<UserStoryGroup>>(emptyList())
    val storyGroups: StateFlow<List<UserStoryGroup>> = _storyGroups.asStateFlow()

    // Current user's own stories
    private val _myStories = MutableStateFlow<List<Story>>(emptyList())
    val myStories: StateFlow<List<Story>> = _myStories.asStateFlow()

    // Upload state
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    // Liked story IDs (optimistic UI)
    private val _likedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val likedStoryIds: StateFlow<Set<String>> = _likedStoryIds.asStateFlow()

    // Viewed story IDs (local tracking)
    private val _viewedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val viewedStoryIds: StateFlow<Set<String>> = _viewedStoryIds.asStateFlow()

    // Only Friends close friends list
    private val _onlyFriendsIds = MutableStateFlow<List<String>>(emptyList())
    val onlyFriendsIds: StateFlow<List<String>> = _onlyFriendsIds.asStateFlow()

    init {
        loadStories()
        loadOnlyFriends()
    }

    fun loadOnlyFriends() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val list = repository.getOnlyFriends(uid)
                _onlyFriendsIds.value = list
            } catch (e: Exception) {
                Log.e("StoryViewModel", "loadOnlyFriends error", e)
            }
        }
    }

    fun toggleOnlyFriend(targetUid: String) {
        val uid = auth.currentUser?.uid ?: return
        val current = _onlyFriendsIds.value.toMutableList()
        if (current.contains(targetUid)) {
            current.remove(targetUid)
        } else {
            current.add(targetUid)
        }
        _onlyFriendsIds.value = current
        viewModelScope.launch {
            repository.saveOnlyFriends(uid, current)
        }
    }

    fun saveOnlyFriendsList(newList: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        _onlyFriendsIds.value = newList
        viewModelScope.launch {
            repository.saveOnlyFriends(uid, newList)
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            try {
                val followingIds = repository.getFollowingIds()
                val currentUid = auth.currentUser?.uid
                val allIds = if (currentUid != null) (followingIds + currentUid).distinct() else followingIds
                val groups = if (allIds.isNotEmpty()) {
                    val targeted = repository.fetchStoriesForUsers(allIds)
                    if (targeted.size < 5) {
                        val publicStories = repository.fetchRecentPublicStories()
                        (targeted + publicStories).distinctBy { it.userId }
                    } else targeted
                } else {
                    repository.fetchRecentPublicStories()
                }
                _storyGroups.value = if (currentUid != null) groups.filter { it.userId != currentUid } else groups
                // Pre-populate liked story IDs
                val myUid = currentUid ?: return@launch
                val liked = mutableSetOf<String>()
                groups.forEach { g ->
                    g.stories.forEach { s -> if (s.likedBy.contains(myUid)) liked.add(s.id) }
                }
                _likedStoryIds.value = liked
                loadMyStories()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "loadStories error", e)
            }
        }
    }

    fun loadUserStories(userId: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null && userId == currentUid) {
            loadMyStories()
            return
        }
        viewModelScope.launch {
            try {
                // Check if we already have this user's stories
                if (_storyGroups.value.any { it.userId == userId }) return@launch
                
                // Fetch just for this user
                val groups = repository.fetchStoriesForUsers(listOf(userId))
                if (groups.isNotEmpty()) {
                    val currentGroups = _storyGroups.value.toMutableList()
                    val existingIndex = currentGroups.indexOfFirst { it.userId == userId }
                    if (existingIndex != -1) {
                        currentGroups[existingIndex] = groups.first()
                    } else {
                        currentGroups.add(groups.first())
                    }
                    _storyGroups.value = currentGroups
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "loadUserStories error", e)
            }
        }
    }

    fun loadMyStories() {
        viewModelScope.launch {
            try {
                val serverStories = repository.fetchMyStories()
                val pendingStories = _myStories.value.filter { it.id.startsWith("temp_") }
                _myStories.value = (pendingStories + serverStories).distinctBy { it.id }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun uploadStory(
        context: Context,
        uri: Uri,
        audienceType: String = "public",
        customAudienceIds: List<String> = emptyList()
    ) {
        val currentUid = auth.currentUser?.uid ?: ""
        val tempId = "temp_story_${System.currentTimeMillis()}"
        val targetAudienceIds = if (audienceType == "only_friends" && customAudienceIds.isEmpty()) {
            _onlyFriendsIds.value
        } else {
            customAudienceIds
        }
        val optimisticStory = Story(
            id = tempId,
            userId = currentUid,
            mediaUrl = uri.toString(),
            audienceType = audienceType,
            customAudienceIds = targetAudienceIds,
            timestamp = System.currentTimeMillis()
        )
        _myStories.value = listOf(optimisticStory) + _myStories.value.filter { it.id != tempId }

        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            try {
                val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri)
                if (bytes == null || bytes.isEmpty()) {
                    _myStories.value = _myStories.value.filter { it.id != tempId }
                    _uploadError.value = "Failed to process image"
                    return@launch
                }

                val url = CloudinaryClient.uploadImage(bytes, folder = "stories")
                if (url == null) {
                    _myStories.value = _myStories.value.filter { it.id != tempId }
                    _uploadError.value = "Upload failed. Please try again."
                    return@launch
                }
                repository.publishStory(url, audienceType, targetAudienceIds)
                _myStories.value = _myStories.value.filter { it.id != tempId }
                loadMyStories()
                loadStories()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _myStories.value = _myStories.value.filter { it.id != tempId }
                _uploadError.value = "Error: ${e.message}"
                Log.e("StoryViewModel", "uploadStory error", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun publishTextStory(
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        audienceType: String,
        customAudienceIds: List<String>
    ) {
        val currentUid = auth.currentUser?.uid ?: ""
        val tempId = "temp_story_${System.currentTimeMillis()}"
        val optimisticStory = Story(
            id = tempId,
            userId = currentUid,
            mediaUrl = "",
            textContent = textContent,
            textFormat = textFormat,
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = customAudienceIds,
            timestamp = System.currentTimeMillis()
        )
        _myStories.value = listOf(optimisticStory) + _myStories.value.filter { it.id != tempId }

        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            try {
                repository.publishTextStory(textContent, textFormat, backgroundColor, audienceType, customAudienceIds)
                _myStories.value = _myStories.value.filter { it.id != tempId }
                loadMyStories()
                loadStories()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _myStories.value = _myStories.value.filter { it.id != tempId }
                _uploadError.value = "Error: ${e.message}"
                Log.e("StoryViewModel", "publishTextStory error", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun markViewed(storyId: String) {
        if (_viewedStoryIds.value.contains(storyId)) return
        _viewedStoryIds.value = _viewedStoryIds.value + storyId
        viewModelScope.launch {
            repository.markViewed(storyId)
        }
    }

    fun toggleLike(storyId: String) {
        val isLiked = _likedStoryIds.value.contains(storyId)
        _likedStoryIds.value = if (isLiked) _likedStoryIds.value - storyId else _likedStoryIds.value + storyId
        viewModelScope.launch {
            val result = repository.toggleLike(storyId)
            // If server result differs from optimistic, revert
            val optimistic = _likedStoryIds.value.contains(storyId)
            if (result != optimistic) {
                _likedStoryIds.value = if (result) _likedStoryIds.value + storyId else _likedStoryIds.value - storyId
            }
        }
    }

    fun deleteStory(storyId: String) {
        _myStories.value = _myStories.value.filter { it.id != storyId }
        _storyGroups.value = _storyGroups.value.mapNotNull { group ->
            val updated = group.stories.filter { it.id != storyId }
            if (updated.isEmpty()) null else group.copy(stories = updated)
        }
        viewModelScope.launch { repository.deleteStory(storyId) }
    }
}
