package com.chroma.studio.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.CloudinaryClient
import com.chroma.studio.data.StoryRepository
import com.chroma.studio.data.StoryLikeSyncManager
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Saved state of a pending story upload to enable instantaneous retry
 * without having the user start from zero if upload fails.
 */
data class PendingStoryUpload(
    val uri: Uri? = null,
    val textContent: String = "",
    val textFormat: String = "",
    val backgroundColor: String = "",
    val audienceType: String = "public",
    val customAudienceIds: List<String> = emptyList(),
    val isTextStory: Boolean = false,
    val failedAt: Long = System.currentTimeMillis()
)

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

    // Preserved failed upload state for zero-friction one-tap retry
    private val _failedStoryUpload = MutableStateFlow<PendingStoryUpload?>(null)
    val failedStoryUpload: StateFlow<PendingStoryUpload?> = _failedStoryUpload.asStateFlow()

    // Liked story IDs backed by offline-first StoryLikeSyncManager with LocalStorage Cache
    val likedStoryIds: StateFlow<Set<String>> = StoryLikeSyncManager.likedStoryIds

    // Viewed story IDs (local tracking)
    private val _viewedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val viewedStoryIds: StateFlow<Set<String>> = _viewedStoryIds.asStateFlow()

    // Only Friends close friends list
    private val _onlyFriendsIds = MutableStateFlow<List<String>>(emptyList())
    val onlyFriendsIds: StateFlow<List<String>> = _onlyFriendsIds.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            StoryLikeSyncManager.initForUser(currentUid)
            loadStories()
            loadOnlyFriends()
        }

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                StoryLikeSyncManager.initForUser(uid)
                loadStories()
                loadOnlyFriends()
            } else {
                resetForLogout()
            }
        }
        try {
            auth.addAuthStateListener(authStateListener!!)
        } catch (e: Exception) {
            Log.e("StoryViewModel", "Error adding auth state listener", e)
        }
    }

    fun resetForLogout() {
        _storyGroups.value = emptyList()
        _myStories.value = emptyList()
        _onlyFriendsIds.value = emptyList()
        _isUploading.value = false
        _uploadError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let {
            try {
                auth.removeAuthStateListener(it)
            } catch (_: Exception) {}
        }
        authStateListener = null
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
                // Merge remote stories with LocalStorage cache and pending sync actions
                if (currentUid != null) {
                    StoryLikeSyncManager.mergeRemoteStories(groups, currentUid)
                }
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
                    if (currentUid != null) {
                        StoryLikeSyncManager.mergeRemoteStories(groups, currentUid)
                    }
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
                val currentUid = auth.currentUser?.uid
                if (currentUid != null && serverStories.isNotEmpty()) {
                    StoryLikeSyncManager.mergeRemoteStories(
                        listOf(UserStoryGroup(userId = currentUid, userProfile = UserProfile(uid = currentUid), stories = serverStories)),
                        currentUid
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun uploadStory(
        context: Context,
        uri: Uri,
        audienceType: String = "public",
        customAudienceIds: List<String> = emptyList(),
        textContent: String = "",
        textFormat: String = "",
        backgroundColor: String = ""
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
            textContent = textContent,
            textFormat = textFormat,
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = targetAudienceIds,
            timestamp = System.currentTimeMillis()
        )
        _myStories.value = listOf(optimisticStory) + _myStories.value.filter { it.id != tempId }
        _failedStoryUpload.value = null
        _uploadError.value = null

        viewModelScope.launch {
            _isUploading.value = true
            try {
                val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri)
                if (bytes == null || bytes.isEmpty()) {
                    _failedStoryUpload.value = PendingStoryUpload(
                        uri = uri,
                        textContent = textContent,
                        textFormat = textFormat,
                        backgroundColor = backgroundColor,
                        audienceType = audienceType,
                        customAudienceIds = targetAudienceIds,
                        isTextStory = false
                    )
                    _uploadError.value = "Failed to process image. Tap to retry."
                    return@launch
                }

                val url = CloudinaryClient.uploadImage(bytes, folder = "stories")
                if (url == null) {
                    _failedStoryUpload.value = PendingStoryUpload(
                        uri = uri,
                        textContent = textContent,
                        textFormat = textFormat,
                        backgroundColor = backgroundColor,
                        audienceType = audienceType,
                        customAudienceIds = targetAudienceIds,
                        isTextStory = false
                    )
                    _uploadError.value = "Upload failed. Tap to retry."
                    return@launch
                }
                repository.publishStory(
                    mediaUrl = url,
                    audienceType = audienceType,
                    customAudienceIds = targetAudienceIds,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor
                )
                _failedStoryUpload.value = null
                _uploadError.value = null
                _myStories.value = _myStories.value.filter { it.id != tempId }
                loadMyStories()
                loadStories()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _failedStoryUpload.value = PendingStoryUpload(
                    uri = uri,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    customAudienceIds = targetAudienceIds,
                    isTextStory = false
                )
                _uploadError.value = "Upload failed: ${e.localizedMessage ?: "Network error"}. Tap to retry."
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
        _failedStoryUpload.value = null
        _uploadError.value = null

        viewModelScope.launch {
            _isUploading.value = true
            try {
                repository.publishTextStory(textContent, textFormat, backgroundColor, audienceType, customAudienceIds)
                _failedStoryUpload.value = null
                _uploadError.value = null
                _myStories.value = _myStories.value.filter { it.id != tempId }
                loadMyStories()
                loadStories()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _failedStoryUpload.value = PendingStoryUpload(
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    customAudienceIds = customAudienceIds,
                    isTextStory = true
                )
                _uploadError.value = "Upload failed: ${e.localizedMessage ?: "Network error"}. Tap to retry."
                Log.e("StoryViewModel", "publishTextStory error", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * Resumes and re-attempts the exact story upload from the saved pending state
     * without forcing the user to re-select media or re-type from zero.
     */
    fun retryFailedUpload(context: Context) {
        val pending = _failedStoryUpload.value ?: return
        _failedStoryUpload.value = null
        _uploadError.value = null
        if (pending.isTextStory) {
            publishTextStory(
                textContent = pending.textContent,
                textFormat = pending.textFormat,
                backgroundColor = pending.backgroundColor,
                audienceType = pending.audienceType,
                customAudienceIds = pending.customAudienceIds
            )
        } else if (pending.uri != null) {
            uploadStory(
                context = context,
                uri = pending.uri,
                audienceType = pending.audienceType,
                customAudienceIds = pending.customAudienceIds,
                textContent = pending.textContent,
                textFormat = pending.textFormat,
                backgroundColor = pending.backgroundColor
            )
        }
    }

    /**
     * Clears failed upload state and discards the optimistic temporary story item.
     */
    fun clearFailedUpload() {
        _failedStoryUpload.value = null
        _uploadError.value = null
        _myStories.value = _myStories.value.filter { !it.id.startsWith("temp_") }
    }

    fun markViewed(storyId: String) {
        if (_viewedStoryIds.value.contains(storyId)) return
        _viewedStoryIds.value = _viewedStoryIds.value + storyId
        viewModelScope.launch {
            repository.markViewed(storyId)
        }
    }

    fun toggleLike(storyId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val nextLiked = StoryLikeSyncManager.toggleLike(storyId, currentUid)

        // Optimistically update _storyGroups immediately
        _storyGroups.value = _storyGroups.value.map { group ->
            val updatedStories = group.stories.map { story ->
                if (story.id == storyId) {
                    val newLikedBy = if (nextLiked) {
                        (story.likedBy + currentUid).distinct()
                    } else {
                        story.likedBy - currentUid
                    }
                    val newLikesCount = if (nextLiked) {
                        story.likes + 1
                    } else {
                        maxOf(0, story.likes - 1)
                    }
                    story.copy(likedBy = newLikedBy, likes = newLikesCount)
                } else story
            }
            group.copy(stories = updatedStories)
        }

        // Optimistically update _myStories immediately
        _myStories.value = _myStories.value.map { story ->
            if (story.id == storyId) {
                val newLikedBy = if (nextLiked) {
                    (story.likedBy + currentUid).distinct()
                } else {
                    story.likedBy - currentUid
                }
                val newLikesCount = if (nextLiked) {
                    story.likes + 1
                } else {
                    maxOf(0, story.likes - 1)
                }
                story.copy(likedBy = newLikedBy, likes = newLikesCount)
            } else story
        }
    }

    fun forceLike(storyId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (StoryLikeSyncManager.likedStoryIds.value.contains(storyId)) return
        StoryLikeSyncManager.forceLike(storyId, currentUid)

        _storyGroups.value = _storyGroups.value.map { group ->
            val updatedStories = group.stories.map { story ->
                if (story.id == storyId) {
                    val newLikedBy = (story.likedBy + currentUid).distinct()
                    story.copy(likedBy = newLikedBy, likes = story.likes + 1)
                } else story
            }
            group.copy(stories = updatedStories)
        }

        _myStories.value = _myStories.value.map { story ->
            if (story.id == storyId) {
                val newLikedBy = (story.likedBy + currentUid).distinct()
                story.copy(likedBy = newLikedBy, likes = story.likes + 1)
            } else story
        }
    }

    fun syncPendingLikes() {
        viewModelScope.launch {
            StoryLikeSyncManager.syncPendingQueue()
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

    suspend fun fetchStoryGroupForStory(storyId: String): Pair<UserStoryGroup, Story>? {
        return repository.fetchStoryGroupForStory(storyId)
    }
}
