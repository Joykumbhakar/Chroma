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
import com.chroma.studio.model.StoryHighlight
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
    val isHighlight: Boolean = false,
    val highlightTitle: String = "",
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

    // Upload state (WhatsApp-style real-time optimistic tracking)
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

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

    // Highlights & Archive
    private val _highlights = MutableStateFlow<List<StoryHighlight>>(emptyList())
    val highlights: StateFlow<List<StoryHighlight>> = _highlights.asStateFlow()

    private val _highlightsOwnerId = MutableStateFlow<String?>(null)
    val highlightsOwnerId: StateFlow<String?> = _highlightsOwnerId.asStateFlow()

    private val _storyArchive = MutableStateFlow<List<Story>>(emptyList())
    val storyArchive: StateFlow<List<Story>> = _storyArchive.asStateFlow()

    private val _isArchiveLoading = MutableStateFlow(false)
    val isArchiveLoading: StateFlow<Boolean> = _isArchiveLoading.asStateFlow()

    private val _repostingStoryId = MutableStateFlow<String?>(null)
    val repostingStoryId: StateFlow<String?> = _repostingStoryId.asStateFlow()

    private val _repostedSourceStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val repostedSourceStoryIds: StateFlow<Set<String>> = _repostedSourceStoryIds.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            StoryLikeSyncManager.initForUser(currentUid)
            val cachedStories = com.chroma.studio.data.OfflineDataCacheManager.getStoryGroups(
                com.chroma.studio.ChromaApp.instance,
                currentUid
            )
            if (cachedStories.isNotEmpty()) {
                _storyGroups.value = cachedStories.filter { it.userId != currentUid }
            }
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
        _uploadProgress.value = 0f
        _uploadError.value = null
        _failedStoryUpload.value = null
        _highlights.value = emptyList()
        _highlightsOwnerId.value = null
        _storyArchive.value = emptyList()
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
                if (groups.isNotEmpty() || _storyGroups.value.isEmpty()) {
                    _storyGroups.value = if (currentUid != null) groups.filter { it.userId != currentUid } else groups
                }
                if (currentUid != null && groups.isNotEmpty()) {
                    com.chroma.studio.data.OfflineDataCacheManager.saveStoryGroups(
                        com.chroma.studio.ChromaApp.instance,
                        currentUid,
                        groups
                    )
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

    fun uploadStories(
        context: Context,
        uris: List<Uri>,
        audienceType: String = "public",
        customAudienceIds: List<String> = emptyList(),
        textContent: String = "",
        textFormat: String = "",
        textFormats: List<String> = emptyList(),
        backgroundColor: String = "",
        isHighlight: Boolean = false,
        highlightTitle: String = "",
        musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null,
        slideMusicTracks: List<com.chroma.studio.data.AttachedMusicSelection?> = emptyList()
    ) {
        val indexedSlides = uris.mapIndexed { idx, uri ->
            Triple(uri, textFormats.getOrElse(idx) { textFormat }, slideMusicTracks.getOrNull(idx) ?: musicTrack)
        }.distinctBy { it.first }
        if (indexedSlides.isEmpty()) return
        if (indexedSlides.size == 1) {
            uploadStory(
                context = context,
                uri = indexedSlides.first().first,
                audienceType = audienceType,
                customAudienceIds = customAudienceIds,
                textContent = textContent,
                textFormat = indexedSlides.first().second,
                backgroundColor = backgroundColor,
                isHighlight = isHighlight,
                highlightTitle = highlightTitle,
                musicTrack = indexedSlides.first().third
            )
            return
        }

        val currentUid = auth.currentUser?.uid ?: ""
        val targetAudienceIds = if (audienceType == "only_friends" && customAudienceIds.isEmpty()) {
            _onlyFriendsIds.value
        } else {
            customAudienceIds
        }
        val baseTs = System.currentTimeMillis()
        val pendingItems = indexedSlides.mapIndexed { idx, (uri, itemFmt, slideMusic) ->
            val tempId = "temp_story_${baseTs}_$idx"
            tempId to Story(
                id = tempId,
                userId = currentUid,
                mediaUrl = uri.toString(),
                textContent = textContent,
                textFormat = itemFmt,
                backgroundColor = backgroundColor,
                audienceType = audienceType,
                customAudienceIds = targetAudienceIds,
                timestamp = baseTs + idx,
                isHighlight = isHighlight,
                highlightTitle = highlightTitle,
                expiresAt = baseTs + 86400000L,
                isUploading = true,
                uploadProgress = 0.1f,
                isFailed = false,
                musicTrackId = slideMusic?.trackId,
                musicTitle = slideMusic?.title,
                musicArtist = slideMusic?.artist,
                musicAlbum = slideMusic?.album,
                musicCoverUrl = slideMusic?.coverUrl,
                musicAudioUrl = slideMusic?.audioUrl,
                musicStartSec = slideMusic?.startSec ?: 0,
                musicDurationSec = slideMusic?.durationSec ?: 0,
                musicShowLyrics = slideMusic?.showLyricsSticker ?: false
            )
        }
        _myStories.value = pendingItems.map { it.second } + _myStories.value.filter { !it.isFailed }
        _failedStoryUpload.value = null
        _uploadError.value = null
        _uploadProgress.value = 0.1f
        _isUploading.value = true

        viewModelScope.launch {
            try {
                indexedSlides.forEachIndexed { idx, (uri, itemFmt, slideMusic) ->
                    val tempId = pendingItems[idx].first
                    performMediaStoryUpload(
                        tempId = tempId,
                        context = context,
                        uri = uri,
                        currentUid = currentUid,
                        audienceType = audienceType,
                        targetAudienceIds = targetAudienceIds,
                        textContent = textContent,
                        textFormat = itemFmt,
                        backgroundColor = backgroundColor,
                        isHighlight = isHighlight,
                        highlightTitle = highlightTitle,
                        refreshAfter = idx == indexedSlides.lastIndex,
                        musicTrack = slideMusic
                    )
                }
            } finally {
                _isUploading.value = _myStories.value.any { it.isUploading }
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
        backgroundColor: String = "",
        isHighlight: Boolean = false,
        highlightTitle: String = "",
        musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null
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
            timestamp = System.currentTimeMillis(),
            isHighlight = isHighlight,
            highlightTitle = highlightTitle,
            expiresAt = System.currentTimeMillis() + 86400000,
            isUploading = true,
            uploadProgress = 0.15f,
            isFailed = false,
            musicTrackId = musicTrack?.trackId,
            musicTitle = musicTrack?.title,
            musicArtist = musicTrack?.artist,
            musicAlbum = musicTrack?.album,
            musicCoverUrl = musicTrack?.coverUrl,
            musicAudioUrl = musicTrack?.audioUrl,
            musicStartSec = musicTrack?.startSec ?: 0,
            musicDurationSec = musicTrack?.durationSec ?: 0,
            musicShowLyrics = musicTrack?.showLyricsSticker ?: false
        )
        // Optimistically show story immediately in myStories tray & profile without dropping other active uploads
        _myStories.value = listOf(optimisticStory) + _myStories.value.filter { it.id != tempId && !it.isFailed }
        _failedStoryUpload.value = null
        _uploadError.value = null
        _uploadProgress.value = 0.15f
        _isUploading.value = true

        viewModelScope.launch {
            try {
                performMediaStoryUpload(
                    tempId = tempId,
                    context = context,
                    uri = uri,
                    currentUid = currentUid,
                    audienceType = audienceType,
                    targetAudienceIds = targetAudienceIds,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    isHighlight = isHighlight,
                    highlightTitle = highlightTitle,
                    refreshAfter = true,
                    musicTrack = musicTrack
                )
            } finally {
                _isUploading.value = _myStories.value.any { it.isUploading }
            }
        }
    }

    private suspend fun performMediaStoryUpload(
        tempId: String,
        context: Context,
        uri: Uri,
        currentUid: String,
        audienceType: String,
        targetAudienceIds: List<String>,
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        isHighlight: Boolean,
        highlightTitle: String,
        refreshAfter: Boolean,
        musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null
    ) {
        try {
            // Phase 1: Pre-process & compress image (15% -> 40%)
            updateStoryProgress(tempId, 0.35f)
            val bytes = com.chroma.studio.utils.ImageUtils.compressChatImage(context, uri)
            if (bytes == null || bytes.isEmpty()) {
                markStoryFailed(
                    tempId = tempId,
                    uri = uri,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    targetAudienceIds = targetAudienceIds,
                    isTextStory = false,
                    isHighlight = isHighlight,
                    highlightTitle = highlightTitle,
                    errorMessage = "Failed to process image. Tap to retry."
                )
                return
            }

            // Phase 2: Upload to Cloudinary CDN (40% -> 80%)
            updateStoryProgress(tempId, 0.70f)
            val url = CloudinaryClient.uploadImage(bytes, folder = "stories")
            if (url == null) {
                markStoryFailed(
                    tempId = tempId,
                    uri = uri,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    targetAudienceIds = targetAudienceIds,
                    isTextStory = false,
                    isHighlight = isHighlight,
                    highlightTitle = highlightTitle,
                    errorMessage = "Upload failed. Tap to retry."
                )
                return
            }

            // Phase 3: Publish to database (80% -> 95%)
            updateStoryProgress(tempId, 0.90f)
            repository.publishStory(
                mediaUrl = url,
                audienceType = audienceType,
                customAudienceIds = targetAudienceIds,
                textContent = textContent,
                textFormat = textFormat,
                backgroundColor = backgroundColor,
                isHighlight = isHighlight,
                highlightTitle = highlightTitle,
                musicTrack = musicTrack
            )

            // Phase 4: Succeeded! Smoothly reconcile optimistic story with server record
            _uploadProgress.value = 1.0f
            _failedStoryUpload.value = null
            _uploadError.value = null
            _myStories.value = _myStories.value.filter { it.id != tempId }
            if (refreshAfter) {
                loadMyStories()
                loadStories()
                if (isHighlight && currentUid.isNotBlank()) {
                    loadHighlights(currentUid)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            markStoryFailed(
                tempId = tempId,
                uri = uri,
                textContent = textContent,
                textFormat = textFormat,
                backgroundColor = backgroundColor,
                audienceType = audienceType,
                targetAudienceIds = targetAudienceIds,
                isTextStory = false,
                isHighlight = isHighlight,
                highlightTitle = highlightTitle,
                errorMessage = "Upload failed: ${e.localizedMessage ?: "Network error"}. Tap to retry."
            )
            Log.e("StoryViewModel", "uploadStory error", e)
        }
    }

    fun publishTextStory(
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        audienceType: String,
        customAudienceIds: List<String>,
        isHighlight: Boolean = false,
        highlightTitle: String = "",
        musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null
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
            timestamp = System.currentTimeMillis(),
            isHighlight = isHighlight,
            highlightTitle = highlightTitle,
            expiresAt = System.currentTimeMillis() + 86400000,
            isUploading = true,
            uploadProgress = 0.30f,
            isFailed = false,
            musicTrackId = musicTrack?.trackId,
            musicTitle = musicTrack?.title,
            musicArtist = musicTrack?.artist,
            musicAlbum = musicTrack?.album,
            musicCoverUrl = musicTrack?.coverUrl,
            musicAudioUrl = musicTrack?.audioUrl,
            musicStartSec = musicTrack?.startSec ?: 0,
            musicDurationSec = musicTrack?.durationSec ?: 0,
            musicShowLyrics = musicTrack?.showLyricsSticker ?: false
        )
        _myStories.value = listOf(optimisticStory) + _myStories.value.filter { !it.id.startsWith("temp_") }
        _failedStoryUpload.value = null
        _uploadError.value = null
        _uploadProgress.value = 0.30f
        _isUploading.value = true

        viewModelScope.launch {
            try {
                updateStoryProgress(tempId, 0.75f)
                repository.publishTextStory(
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    customAudienceIds = customAudienceIds,
                    isHighlight = isHighlight,
                    highlightTitle = highlightTitle,
                    musicTrack = musicTrack
                )
                _uploadProgress.value = 1.0f
                _failedStoryUpload.value = null
                _uploadError.value = null
                _myStories.value = _myStories.value.filter { it.id != tempId }
                loadMyStories()
                loadStories()
                if (isHighlight && currentUid.isNotBlank()) {
                    loadHighlights(currentUid)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                markStoryFailed(
                    tempId = tempId,
                    uri = null,
                    textContent = textContent,
                    textFormat = textFormat,
                    backgroundColor = backgroundColor,
                    audienceType = audienceType,
                    targetAudienceIds = customAudienceIds,
                    isTextStory = true,
                    isHighlight = isHighlight,
                    highlightTitle = highlightTitle,
                    errorMessage = "Upload failed: ${e.localizedMessage ?: "Network error"}. Tap to retry."
                )
                Log.e("StoryViewModel", "publishTextStory error", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    private fun updateStoryProgress(tempId: String, progress: Float) {
        _uploadProgress.value = progress
        _myStories.value = _myStories.value.map { story ->
            if (story.id == tempId) story.copy(uploadProgress = progress) else story
        }
    }

    private fun markStoryFailed(
        tempId: String,
        uri: Uri?,
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        audienceType: String,
        targetAudienceIds: List<String>,
        isTextStory: Boolean,
        isHighlight: Boolean,
        highlightTitle: String,
        errorMessage: String
    ) {
        _failedStoryUpload.value = PendingStoryUpload(
            uri = uri,
            textContent = textContent,
            textFormat = textFormat,
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = targetAudienceIds,
            isTextStory = isTextStory,
            isHighlight = isHighlight,
            highlightTitle = highlightTitle
        )
        _uploadError.value = errorMessage
        _uploadProgress.value = 0f
        _myStories.value = _myStories.value.map { story ->
            if (story.id == tempId) story.copy(isUploading = false, isFailed = true) else story
        }
    }

    /** Save an existing story to highlights so it lives forever (never expires) */
    fun saveStoryToHighlight(
        storyId: String,
        highlightTitle: String,
        emoji: String = "✨",
        coverUrl: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = repository.saveStoryToHighlight(storyId, highlightTitle, emoji, coverUrl)
                if (success) {
                    auth.currentUser?.uid?.let { loadHighlights(it) }
                    loadMyStories()
                }
                onComplete(success)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "saveStoryToHighlight error", e)
                onComplete(false)
            }
        }
    }

    /** Load immortal highlighted stories for viewing in highlight reel */
    fun loadHighlightStories(
        userId: String,
        highlightTitle: String? = null,
        onResult: (List<Story>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val list = repository.fetchHighlightStories(userId, highlightTitle)
                onResult(list)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "loadHighlightStories error", e)
                onResult(emptyList())
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
                customAudienceIds = pending.customAudienceIds,
                isHighlight = pending.isHighlight,
                highlightTitle = pending.highlightTitle
            )
        } else if (pending.uri != null) {
            uploadStory(
                context = context,
                uri = pending.uri,
                audienceType = pending.audienceType,
                customAudienceIds = pending.customAudienceIds,
                textContent = pending.textContent,
                textFormat = pending.textFormat,
                backgroundColor = pending.backgroundColor,
                isHighlight = pending.isHighlight,
                highlightTitle = pending.highlightTitle
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

    /**
     * Discards a specific pending or failed story upload.
     */
    fun discardFailedUpload(tempId: String = "") {
        _failedStoryUpload.value = null
        _uploadError.value = null
        _myStories.value = if (tempId.isNotBlank()) {
            _myStories.value.filter { it.id != tempId }
        } else {
            _myStories.value.filter { !it.id.startsWith("temp_") }
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
        _highlights.value = _highlights.value.map { hl ->
            if (storyId in hl.storyIds) hl.copy(storyIds = hl.storyIds.filter { it != storyId }) else hl
        }
        _storyArchive.value = _storyArchive.value.filter { it.id != storyId }
        viewModelScope.launch { repository.deleteStory(storyId) }
    }

    suspend fun fetchStoryGroupForStory(storyId: String): Pair<UserStoryGroup, Story>? {
        return repository.fetchStoryGroupForStory(storyId)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Story Highlights & Archive Operations
    // ────────────────────────────────────────────────────────────────────────────

    /** Load highlights for a user (sorted by updatedAt descending). */
    fun loadHighlights(userId: String) {
        viewModelScope.launch {
            try {
                val list = repository.fetchUserHighlights(userId)
                _highlights.value = list
                _highlightsOwnerId.value = userId
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "loadHighlights error", e)
            }
        }
    }

    /** Load all past and active stories for a user archive. */
    fun loadStoryArchive(userId: String) {
        viewModelScope.launch {
            _isArchiveLoading.value = true
            try {
                val archive = repository.fetchUserStoryArchive(userId)
                _storyArchive.value = archive
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "loadStoryArchive error", e)
            } finally {
                _isArchiveLoading.value = false
            }
        }
    }

    /** Add a story to an existing highlight album. */
    fun addStoryToHighlight(
        highlightId: String,
        storyId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = repository.addStoryToHighlight(highlightId, storyId)
                if (success) {
                    auth.currentUser?.uid?.let { loadHighlights(it) }
                    loadMyStories()
                }
                onComplete(success)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "addStoryToHighlight error", e)
                onComplete(false)
            }
        }
    }

    /** Create a new highlight album with a list of stories. */
    fun createHighlight(
        title: String,
        coverUrl: String,
        storyIds: List<String>,
        onComplete: (StoryHighlight?) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val created = repository.createHighlight(title, coverUrl, storyIds)
                if (created != null) {
                    auth.currentUser?.uid?.let { loadHighlights(it) }
                    loadMyStories()
                }
                onComplete(created)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "createHighlight error", e)
                onComplete(null)
            }
        }
    }

    /** Update highlight title, cover image, and story list. */
    fun updateHighlight(
        highlightId: String,
        title: String,
        coverUrl: String,
        storyIds: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = repository.updateHighlight(highlightId, title, coverUrl, storyIds)
                if (success) {
                    auth.currentUser?.uid?.let { loadHighlights(it) }
                }
                onComplete(success)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "updateHighlight error", e)
                onComplete(false)
            }
        }
    }

    /** Delete a highlight album. */
    fun deleteHighlight(
        highlightId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = repository.deleteHighlight(highlightId)
                if (success) {
                    _highlights.value = _highlights.value.filter { it.id != highlightId }
                    auth.currentUser?.uid?.let { loadHighlights(it) }
                }
                onComplete(success)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "deleteHighlight error", e)
                onComplete(false)
            }
        }
    }

    /** Fetch full Story objects for a given highlight reel. */
    fun fetchStoriesForHighlight(
        highlight: StoryHighlight,
        onResult: (List<Story>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val stories = repository.fetchStoriesByIds(highlight.storyIds)
                onResult(stories)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "fetchStoriesForHighlight error", e)
                onResult(emptyList())
            }
        }
    }

    /**
     * Reshares a story that mentioned the current user onto their own profile/story bar.
     */
    fun repostMentionedStory(
        sourceStoryId: String,
        audienceType: String = "public",
        customAudienceIds: List<String> = emptyList(),
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (sourceStoryId.isBlank() || _repostingStoryId.value == sourceStoryId) return
        _repostingStoryId.value = sourceStoryId

        viewModelScope.launch {
            try {
                val result = repository.repostMentionedStory(
                    sourceStoryId = sourceStoryId,
                    audienceType = audienceType,
                    customAudienceIds = customAudienceIds
                )
                result.fold(
                    onSuccess = { newStory ->
                        _repostedSourceStoryIds.value = _repostedSourceStoryIds.value + sourceStoryId
                        _myStories.value = listOf(newStory) + _myStories.value.filter { it.id != newStory.id }
                        loadMyStories()
                        loadStories()
                        onComplete(true, null)
                    },
                    onFailure = { err ->
                        onComplete(false, err.message ?: "Could not share story")
                    }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("StoryViewModel", "repostMentionedStory error", e)
                onComplete(false, e.localizedMessage ?: "Could not share story")
            } finally {
                if (_repostingStoryId.value == sourceStoryId) {
                    _repostingStoryId.value = null
                }
            }
        }
    }
}
