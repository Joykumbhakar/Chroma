package com.chroma.studio.data

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class UploadStatus {
    IDLE,
    UPLOADING,
    SUCCESS,
    FAILED
}

/**
 * Represents an active or completed background upload task for Flash Videos or Community Posts.
 * Tracks progress, video thumbnail, status, and failure metadata for Instagram-like UI/UX.
 */
data class FlashUploadTask(
    val id: String = UUID.randomUUID().toString(),
    val thumbnailBitmap: Bitmap? = null,
    val title: String = "",
    val progress: Float = 0f,
    val statusText: String = "Posting video to Community…",
    val status: UploadStatus = UploadStatus.IDLE,
    val errorMessage: String? = null,
    val retryAction: (() -> Unit)? = null,
    val draftId: String? = null
)

/**
 * Singleton manager coordinating Instagram-style background uploads for Flash Videos and Posts.
 * Enables immediate user redirection to the Community tab while monitoring progress,
 * handling real-time retry, and preserving local drafts.
 */
object FlashUploadManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTask = MutableStateFlow<FlashUploadTask?>(null)
    val currentTask: StateFlow<FlashUploadTask?> = _currentTask.asStateFlow()

    fun startUpload(
        thumbnailBitmap: Bitmap?,
        title: String,
        statusText: String = "Posting video to Community…",
        draftId: String? = null,
        retryAction: (() -> Unit)? = null
    ): FlashUploadTask {
        val task = FlashUploadTask(
            thumbnailBitmap = thumbnailBitmap,
            title = title,
            progress = 0.05f,
            statusText = statusText,
            status = UploadStatus.UPLOADING,
            retryAction = retryAction,
            draftId = draftId
        )
        _currentTask.value = task
        return task
    }

    fun updateProgress(progress: Float, statusText: String? = null) {
        val current = _currentTask.value ?: return
        if (current.status != UploadStatus.UPLOADING) return
        _currentTask.value = current.copy(
            progress = progress.coerceIn(0f, 1f),
            statusText = statusText ?: current.statusText
        )
    }

    fun setSuccess(statusText: String = "Posted! 🎉") {
        val current = _currentTask.value ?: return
        _currentTask.value = current.copy(
            progress = 1f,
            statusText = statusText,
            status = UploadStatus.SUCCESS,
            errorMessage = null
        )

        // Automatically dismiss the success HUD banner after 3 seconds
        scope.launch {
            delay(3000)
            if (_currentTask.value?.status == UploadStatus.SUCCESS) {
                _currentTask.value = null
            }
        }
    }

    fun setFailed(cleanErrorMessage: String, retryAction: (() -> Unit)? = null) {
        val current = _currentTask.value ?: return
        val sanitizedError = cleanErrorMessage
            .removePrefix("java.lang.Exception: ")
            .removePrefix("Exception: ")
            .trim()
            .ifBlank { "Network connection issue or upload error" }

        _currentTask.value = current.copy(
            status = UploadStatus.FAILED,
            errorMessage = sanitizedError,
            retryAction = retryAction ?: current.retryAction
        )
    }

    fun retry() {
        val task = _currentTask.value ?: return
        val action = task.retryAction ?: return
        _currentTask.value = task.copy(
            status = UploadStatus.UPLOADING,
            progress = 0.05f,
            statusText = "Re-uploading video…",
            errorMessage = null
        )
        action.invoke()
    }

    fun dismiss() {
        _currentTask.value = null
    }
}
