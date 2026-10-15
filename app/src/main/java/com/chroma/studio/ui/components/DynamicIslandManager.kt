package com.chroma.studio.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed hierarchy of all Dynamic Island / Notch in-app notifications.
 */
sealed class DynamicIslandEvent {
    data class ChatMessage(
        val roomId: String,
        val senderName: String,
        val messageText: String,
        val avatarUrl: String = "",
        val timeText: String = "now",
        val onReply: ((String) -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Like(
        val userName: String,
        val userAvatar: String = "",
        val targetTitle: String = "your post",
        val targetThumbnail: String = "",
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Follow(
        val userId: String,
        val userName: String,
        val userAvatar: String = "",
        val onFollowBack: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class RemixOrDownload(
        val userName: String,
        val userAvatar: String = "",
        val workTitle: String = "Artwork",
        val isRemix: Boolean = true,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Status(
        val title: String,
        val message: String = "",
        val icon: ImageVector? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
        val durationMs: Long = 4000L
    ) : DynamicIslandEvent()
}

/**
 * Singleton manager to post dynamic notifications into the Apple Dynamic Island HUD.
 */
object DynamicIslandManager {
    private val _events = MutableSharedFlow<DynamicIslandEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun post(event: DynamicIslandEvent) {
        _events.tryEmit(event)
    }

    fun showChatMessage(
        roomId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        timeText: String = "now",
        onReply: ((String) -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.ChatMessage(
                roomId = roomId,
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
                timeText = timeText,
                onReply = onReply,
                onClick = onClick
            )
        )
    }

    fun showLike(
        userName: String,
        userAvatar: String = "",
        targetTitle: String = "your post",
        targetThumbnail: String = "",
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Like(
                userName = userName,
                userAvatar = userAvatar,
                targetTitle = targetTitle,
                targetThumbnail = targetThumbnail,
                onClick = onClick
            )
        )
    }

    fun showFollow(
        userId: String,
        userName: String,
        userAvatar: String = "",
        onFollowBack: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Follow(
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                onFollowBack = onFollowBack,
                onClick = onClick
            )
        )
    }

    fun showRemix(
        userName: String,
        userAvatar: String = "",
        workTitle: String = "Artwork",
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.RemixOrDownload(
                userName = userName,
                userAvatar = userAvatar,
                workTitle = workTitle,
                isRemix = true,
                onClick = onClick
            )
        )
    }

    fun showStatus(
        title: String,
        message: String = "",
        icon: ImageVector? = null,
        actionLabel: String? = null,
        durationMs: Long = 4000L,
        onAction: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Status(
                title = title,
                message = message,
                icon = icon,
                actionLabel = actionLabel,
                durationMs = durationMs,
                onAction = onAction
            )
        )
    }
}
