package com.chroma.studio.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.CloudinaryClient
import com.chroma.studio.data.FlashRepository
import com.chroma.studio.model.FlashDraft
import com.chroma.studio.model.FlashMediaType
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.utils.VideoProcessor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class FlashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashRepository()
    private val prefs = application.getSharedPreferences("flash_drafts", Context.MODE_PRIVATE)

    // ── Feed state ───────────────────────────────────────────────────────────

    private val _flashFeed = MutableStateFlow<List<FlashVideo>>(emptyList())
    val flashFeed: StateFlow<List<FlashVideo>> = _flashFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Liked set ────────────────────────────────────────────────────────────

    private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    // ── Upload state ─────────────────────────────────────────────────────────

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    // ── Drafts ───────────────────────────────────────────────────────────────

    private val _drafts = MutableStateFlow<List<FlashDraft>>(emptyList())
    val drafts: StateFlow<List<FlashDraft>> = _drafts.asStateFlow()

    // ── Pagination cursor ────────────────────────────────────────────────────

    private var lastTimestamp: Long? = null

    init {
        loadFeed()
        loadDrafts()
        syncLikedIds()
    }

    // ── Feed ─────────────────────────────────────────────────────────────────

    fun addOptimisticFlash(flash: FlashVideo) {
        val current = _flashFeed.value
        _flashFeed.value = listOf(flash) + current.filter { it.id != flash.id }
    }

    fun removeOptimisticFlash(flashId: String) {
        _flashFeed.value = _flashFeed.value.filter { it.id != flashId }
    }

    fun ensureFlashInFeed(videoId: String, post: com.chroma.studio.model.DiscoverPost? = null) {
        val current = _flashFeed.value
        if (current.any { it.id == videoId }) return

        if (post != null) {
            val video = FlashVideo(
                id = post.id,
                creatorId = post.creatorId,
                authorUsername = post.authorUsername,
                authorDisplayName = post.authorDisplayName,
                authorAvatarUrl = post.authorAvatarUrl,
                authorVerified = post.authorVerified,
                videoUrl = post.videoUrl ?: "",
                thumbnailUrl = post.thumbnailUrl ?: post.imageUrl ?: "",
                caption = post.description.ifBlank { post.title },
                hashtags = post.tags,
                mentions = post.mentions,
                likesCount = post.likesCount,
                likedBy = post.likedBy,
                commentCount = post.effectiveCommentsCount,
                remixCount = post.remixCount,
                remixedBy = post.remixedBy,
                shareCount = post.shareCount,
                sharedBy = post.sharedBy,
                saveCount = post.saveCount,
                savedBy = post.savedBy,
                remixedFromPostId = post.remixedFromPostId,
                remixedFromAuthorUsername = post.remixedFromAuthorUsername,
                timestamp = post.timestamp,
                hideLikesCount = post.hideLikesCount,
                turnOffComments = post.turnOffComments,
                turnOffRemix = post.turnOffRemix
            )
            _flashFeed.value = listOf(video) + current
            syncLikedIds()
        }
    }

    fun loadFeed(isRefresh: Boolean = false) {
        if (_isLoading.value || _isRefreshing.value) return
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
                lastTimestamp = null
            } else {
                _isLoading.value = true
            }
            try {
                val items = repository.getFlashFeed(limit = 12, lastTimestamp = if (isRefresh) null else lastTimestamp)
                val pendingOptimistic = _flashFeed.value.filter { it.id.startsWith("temp_") }
                if (isRefresh || lastTimestamp == null) {
                    _flashFeed.value = (pendingOptimistic + items).distinctBy { it.id }
                } else {
                    _flashFeed.value = (_flashFeed.value + items).distinctBy { it.id }
                }
                lastTimestamp = items.lastOrNull()?.timestamp
                syncLikedIds()
            } catch (e: Exception) {
                Log.e("FlashViewModel", "loadFeed error", e)
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreFeed() {
        if (_isLoadingMore.value || lastTimestamp == null) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val items = repository.getFlashFeed(limit = 12, lastTimestamp = lastTimestamp)
                _flashFeed.value = _flashFeed.value + items
                lastTimestamp = items.lastOrNull()?.timestamp ?: lastTimestamp
                syncLikedIds()
            } catch (e: Exception) {
                Log.e("FlashViewModel", "loadMoreFeed error", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // ── Interactions ─────────────────────────────────────────────────────────

    private val mutationCoordinator = com.chroma.studio.data.mutation.OptimisticMutationCoordinator(viewModelScope)

    fun toggleLike(videoId: String, forceLike: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val isLikedNow = _likedVideoIds.value.contains(videoId)
        if (forceLike && isLikedNow) return

        val targetLiked = if (forceLike) true else !isLikedNow
        val prevLikedSet = _likedVideoIds.value
        val prevFeed = _flashFeed.value

        mutationCoordinator.mutate(
            key = "flash_like:$videoId",
            desiredState = targetLiked,
            onOptimisticApply = { nextLiked ->
                val newLiked = _likedVideoIds.value.toMutableSet()
                if (nextLiked) newLiked.add(videoId) else newLiked.remove(videoId)
                _likedVideoIds.value = newLiked

                _flashFeed.value = _flashFeed.value.map { v ->
                    if (v.id == videoId) {
                        val newLikedBy = if (nextLiked) v.likedBy + uid else v.likedBy - uid
                        v.copy(
                            likesCount = maxOf(0, v.likesCount + if (nextLiked) 1 else -1),
                            likedBy = newLikedBy.distinct()
                        )
                    } else v
                }
            },
            performServerSync = { nextLiked ->
                repository.setFlashLike(videoId, uid, nextLiked)
            },
            onRollback = {
                _likedVideoIds.value = prevLikedSet
                _flashFeed.value = prevFeed
            },
            onError = {
                com.chroma.studio.ui.components.ToastManager.showToast("Could not update like. Please try again.")
            }
        )
    }

    fun shareFlash(videoId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        _flashFeed.value = _flashFeed.value.map { v ->
            if (v.id == videoId && !v.sharedBy.contains(uid)) {
                v.copy(shareCount = v.shareCount + 1, sharedBy = v.sharedBy + uid)
            } else v
        }
        
        viewModelScope.launch {
            repository.incrementShare(videoId, uid)
        }
    }

    fun toggleSave(videoId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentVideo = _flashFeed.value.firstOrNull { it.id == videoId } ?: return
        val isSavedNow = currentVideo.savedBy.contains(uid)
        val targetSaved = !isSavedNow
        val prevFeed = _flashFeed.value

        mutationCoordinator.mutate(
            key = "flash_save:$videoId",
            desiredState = targetSaved,
            onOptimisticApply = { nextSaved ->
                _flashFeed.value = _flashFeed.value.map { v ->
                    if (v.id == videoId) {
                        val newSavedBy = if (nextSaved) v.savedBy + uid else v.savedBy - uid
                        v.copy(
                            saveCount = maxOf(0, v.saveCount + if (nextSaved) 1 else -1),
                            savedBy = newSavedBy.distinct()
                        )
                    } else v
                }
            },
            performServerSync = { nextSaved ->
                repository.setFlashSave(videoId, uid, nextSaved)
            },
            onRollback = {
                _flashFeed.value = prevFeed
            },
            onError = {
                com.chroma.studio.ui.components.ToastManager.showToast("Could not update saved post. Please try again.")
            }
        )
    }

    private fun syncLikedIds() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val liked = _flashFeed.value
            .filter { uid in it.likedBy }
            .map { it.id }
            .toSet()
        _likedVideoIds.value = liked
    }

    // ── Publish ──────────────────────────────────────────────────────────────

    /**
     * Full publish pipeline:
     * 1. Trim video to [trimStartMs]..[trimEndMs]
     * 2. Compress video (MediaCodec)
     * 3. Upload video to Cloudinary
     * 4. Upload thumbnail to Cloudinary
     * 5. Write Firestore document
     */
    fun publishFlash(
        context: Context,
        mediaUri: Uri,
        mediaType: FlashMediaType,
        trimStartMs: Long,
        trimEndMs: Long,
        thumbnailBitmap: Bitmap?,
        caption: String,
        hashtags: List<String>,
        mentions: List<String>,
        filterName: String,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        hue: Float = 0f,
        imageUris: List<Uri> = emptyList(),
        durationPerSlideMs: Long = 3000L,
        remixedFromPostId: String? = null,
        remixedFromAuthorUsername: String? = null,
        hideLikesCount: Boolean = false,
        turnOffComments: Boolean = false,
        turnOffRemix: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            _uploadError.value = null
            CloudinaryClient.resetCircuitBreaker()
            com.chroma.studio.data.FlashUploadManager.updateProgress(0.05f, "Preparing video…")

            try {
                // Step 1 & 2: Process video
                val processedFile = when (mediaType) {
                    FlashMediaType.VIDEO -> {
                        _uploadProgress.value = 0.15f
                        com.chroma.studio.data.FlashUploadManager.updateProgress(0.15f, "Preparing video…")
                        val videoDur = VideoProcessor.getVideoDurationMs(context, mediaUri)
                        val shouldTrim = trimStartMs > 100L || (trimEndMs > 0L && videoDur > 0L && trimEndMs < videoDur - 100L)
                        val trimmed = if (shouldTrim) {
                            com.chroma.studio.data.FlashUploadManager.updateProgress(0.25f, "Trimming video clip…")
                            val res = try {
                                VideoProcessor.trimVideo(context, mediaUri, trimStartMs, trimEndMs)
                            } catch (e: Exception) {
                                Log.w("FlashViewModel", "Trimming failed, falling back to source video", e)
                                null
                            }
                            res ?: if (mediaUri.scheme == "file") {
                                java.io.File(mediaUri.path ?: "")
                            } else {
                                val tempFile = java.io.File(context.cacheDir, "ready_flash_${System.currentTimeMillis()}.mp4")
                                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                                    java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                                }
                                tempFile
                            }
                        } else {
                            if (mediaUri.scheme == "file") {
                                java.io.File(mediaUri.path ?: "")
                            } else {
                                val tempFile = java.io.File(context.cacheDir, "ready_flash_${System.currentTimeMillis()}.mp4")
                                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                                    java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                                }
                                tempFile
                            }
                        }
                        _uploadProgress.value = 0.45f
                        com.chroma.studio.data.FlashUploadManager.updateProgress(0.45f, "Video ready for upload")
                        trimmed
                    }
                    FlashMediaType.SLIDESHOW -> {
                        _uploadProgress.value = 0.2f
                        com.chroma.studio.data.FlashUploadManager.updateProgress(0.2f, "Creating slideshow…")
                        VideoProcessor.createSlideshow(context, imageUris, durationPerSlideMs)
                            ?: throw Exception("Slideshow creation failed")
                    }
                    FlashMediaType.PHOTO -> {
                        // Single photo: create 3-second slideshow
                        _uploadProgress.value = 0.2f
                        com.chroma.studio.data.FlashUploadManager.updateProgress(0.2f, "Converting photo to video…")
                        VideoProcessor.createSlideshow(context, listOf(mediaUri), 3000L)
                            ?: throw Exception("Photo-to-video conversion failed")
                    }
                }

                // Step 3: Upload video directly via streaming
                _uploadProgress.value = 0.50f
                com.chroma.studio.data.FlashUploadManager.updateProgress(0.50f, "Uploading video to Cloudinary…")
                var videoUrl = CloudinaryClient.uploadVideoFile(
                    file = processedFile,
                    folder = "flash_videos",
                    onProgress = { p ->
                        val prog = 0.50f + p * 0.35f
                        _uploadProgress.value = prog
                        com.chroma.studio.data.FlashUploadManager.updateProgress(prog, "Uploading video… ${(p * 100).toInt()}%")
                    }
                ) ?: throw Exception("Video upload to Cloudinary failed. Please check your network or Cloudinary preset.")
                
                // Apply filter transformation to delivery URL
                videoUrl = CloudinaryClient.applyTransformation(videoUrl, filterName, brightness, contrast, saturation, hue)

                // Step 4: Upload thumbnail
                _uploadProgress.value = 0.88f
                com.chroma.studio.data.FlashUploadManager.updateProgress(0.88f, "Uploading cover thumbnail…")
                val thumbUrl = if (thumbnailBitmap != null) {
                    val baos = ByteArrayOutputStream()
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    CloudinaryClient.uploadFlashThumbnail(baos.toByteArray()) ?: ""
                } else {
                    val extracted = VideoProcessor.extractThumbnail(context, Uri.fromFile(processedFile), trimStartMs.coerceAtLeast(0L))
                    if (extracted != null) {
                        val baos = ByteArrayOutputStream()
                        extracted.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        CloudinaryClient.uploadFlashThumbnail(baos.toByteArray()) ?: ""
                    } else ""
                }

                // Step 5: Write Firestore document
                _uploadProgress.value = 0.95f
                com.chroma.studio.data.FlashUploadManager.updateProgress(0.95f, "Publishing to Community…")
                val duration = if (trimEndMs > trimStartMs) trimEndMs - trimStartMs else VideoProcessor.getVideoDurationMs(context, Uri.fromFile(processedFile))
                repository.publishFlash(
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbUrl,
                    caption = caption,
                    hashtags = hashtags,
                    mentions = mentions,
                    duration = duration.coerceIn(1_000L, 30_000L),
                    remixedFromPostId = remixedFromPostId,
                    remixedFromAuthorUsername = remixedFromAuthorUsername,
                    hideLikesCount = hideLikesCount,
                    turnOffComments = turnOffComments,
                    turnOffRemix = turnOffRemix
                )

                // Cleanup temp files
                withContext(Dispatchers.IO) { processedFile.delete() }

                _uploadProgress.value = 1f
                com.chroma.studio.data.FlashUploadManager.setSuccess("Posted! 🎉")
                loadFeed(isRefresh = true)
                
                try {
                    withContext(Dispatchers.Main) { onSuccess() }
                } catch (navEx: Exception) {
                    if (navEx is kotlinx.coroutines.CancellationException) throw navEx
                    Log.e("FlashViewModel", "Navigation/Success callback error", navEx)
                    // Still successful upload, just callback failed
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FlashViewModel", "publishFlash error", e)
                val errMsg = e.message ?: "Upload failed: ${e.javaClass.simpleName}"
                _uploadError.value = errMsg
                com.chroma.studio.data.FlashUploadManager.setFailed(errMsg)
                withContext(Dispatchers.Main) { onError(errMsg) }
            } finally {
                _isUploading.value = false
            }
        }
    }

    // ── Drafts (JSON persisted in SharedPreferences) ─────────────────────────

    fun saveDraft(draft: FlashDraft) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadDraftsFromPrefs().toMutableList()
            list.removeAll { it.id == draft.id }
            list.add(0, draft)
            saveDraftsToPrefs(list)
            withContext(Dispatchers.Main) { _drafts.value = list }
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadDraftsFromPrefs().toMutableList()
            list.removeAll { it.id == draftId }
            saveDraftsToPrefs(list)
            withContext(Dispatchers.Main) { _drafts.value = list }
        }
    }

    private fun loadDrafts() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadDraftsFromPrefs()
            withContext(Dispatchers.Main) { _drafts.value = list }
        }
    }

    private fun loadDraftsFromPrefs(): List<FlashDraft> {
        val json = prefs.getString("drafts_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                FlashDraft(
                    id = obj.optString("id"),
                    localUri = obj.optString("localUri"),
                    mediaType = FlashMediaType.valueOf(obj.optString("mediaType", "VIDEO")),
                    thumbnailUri = obj.optString("thumbnailUri"),
                    caption = obj.optString("caption"),
                    trimStartMs = obj.optLong("trimStartMs"),
                    trimEndMs = obj.optLong("trimEndMs"),
                    filterName = obj.optString("filterName", "None"),
                    brightness = obj.optDouble("brightness", 0.0).toFloat(),
                    contrast = obj.optDouble("contrast", 1.0).toFloat(),
                    saturation = obj.optDouble("saturation", 1.0).toFloat(),
                    hue = obj.optDouble("hue", 0.0).toFloat(),
                    createdAt = obj.optLong("createdAt")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDraftsToPrefs(drafts: List<FlashDraft>) {
        val arr = JSONArray()
        drafts.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id)
                put("localUri", d.localUri)
                put("mediaType", d.mediaType.name)
                put("thumbnailUri", d.thumbnailUri)
                put("caption", d.caption)
                put("trimStartMs", d.trimStartMs)
                put("trimEndMs", d.trimEndMs)
                put("filterName", d.filterName)
                put("brightness", d.brightness.toDouble())
                put("contrast", d.contrast.toDouble())
                put("saturation", d.saturation.toDouble())
                put("hue", d.hue.toDouble())
                put("createdAt", d.createdAt)
            })
        }
        prefs.edit().putString("drafts_json", arr.toString()).apply()
    }

    // ── Remix Helper ─────────────────────────────────────────────────────────

    fun downloadVideoForRemix(context: Context, videoUrl: String, onProgress: (Float) -> Unit, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onProgress(0.1f) }
                val url = java.net.URL(videoUrl)
                val connection = url.openConnection()
                connection.connect()
                val fileLength = connection.contentLength
                val input = java.io.BufferedInputStream(url.openStream(), 8192)
                val outFile = java.io.File(context.cacheDir, "remix_temp_${System.currentTimeMillis()}.mp4")
                val output = java.io.FileOutputStream(outFile)

                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        withContext(Dispatchers.Main) {
                            onProgress(0.1f + (total.toFloat() / fileLength.toFloat()) * 0.9f)
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()
                
                withContext(Dispatchers.Main) {
                    onComplete(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile))
                }
            } catch (e: Exception) {
                Log.e("FlashViewModel", "Error downloading remix video", e)
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    // ── Delete Flash Video ───────────────────────────────────────────────────

    fun deleteFlash(videoId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val success = repository.deleteFlash(videoId)
                if (success) {
                    _flashFeed.value = _flashFeed.value.filter { it.id != videoId }
                }
                withContext(Dispatchers.Main) {
                    onResult?.invoke(success)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FlashViewModel", "deleteFlash error", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false)
                }
            }
        }
    }

    fun incrementCommentCount(videoId: String, delta: Int = 1) {
        _flashFeed.value = _flashFeed.value.map {
            if (it.id == videoId) it.copy(commentCount = maxOf(0, it.commentCount + delta)) else it
        }
    }

    fun clearUploadError() { _uploadError.value = null }
}
