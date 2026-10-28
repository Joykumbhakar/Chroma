package com.chroma.studio.ui.components

import androidx.compose.runtime.Composable
import com.composables.icons.lucide.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ToastManager {
    data class ToastData(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
        val durationMs: Long = 3000L
    )

    private val _toasts = MutableSharedFlow<ToastData>(extraBufferCapacity = 5)
    val toasts = _toasts.asSharedFlow()

    fun showToast(message: String, actionLabel: String? = null, durationMs: Long = 3000L, onAction: (() -> Unit)? = null) {
        val icon = when {
            message.contains("copied", ignoreCase = true) -> Lucide.Copy
            message.contains("deleted", ignoreCase = true) || message.contains("removed", ignoreCase = true) -> Lucide.Trash2
            message.contains("error", ignoreCase = true) || message.contains("failed", ignoreCase = true) -> Lucide.Info
            message.contains("saved", ignoreCase = true) || message.contains("updated", ignoreCase = true) || message.contains("success", ignoreCase = true) -> Lucide.Check
            message.contains("like", ignoreCase = true) -> Lucide.Heart
            message.contains("follow", ignoreCase = true) -> Lucide.UserCheck
            message.contains("banner", ignoreCase = true) -> Lucide.Image
            message.contains("avatar", ignoreCase = true) -> Lucide.User
            else -> AiWaveSphereVector
        }

        _toasts.tryEmit(ToastData(message, actionLabel, onAction, durationMs))

        // Note: Routine local user actions (copied, saved, deleted) do NOT trigger the Dynamic Island HUD.
        // Dynamic Island is strictly reserved for incoming external notifications (messages, likes, follows)
        // when the user is not in that screen!
    }
}

/**
 * ChromaToastHost is replaced by IosDynamicIslandHost for unified Apple iOS Dynamic Island notifications.
 */
@Composable
fun ChromaToastHost() {
    // Replaced by IosDynamicIslandHost for all toast and status notifications
}
