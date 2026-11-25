package com.chroma.studio.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        val isVerified: Boolean = false,
        val onReply: ((String) -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onQuickReact: ((String) -> Unit)? = null,
        val onMute: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Like(
        val userName: String,
        val userAvatar: String = "",
        val targetTitle: String = "your post",
        val targetThumbnail: String = "",
        val isVerified: Boolean = false,
        val onViewProfile: (() -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Follow(
        val userId: String,
        val userName: String,
        val userAvatar: String = "",
        val isVerified: Boolean = false,
        val isFollowRequest: Boolean = false,
        val onAccept: (() -> Unit)? = null,
        val onDecline: (() -> Unit)? = null,
        val onFollowBack: (() -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class RemixOrDownload(
        val userName: String,
        val userAvatar: String = "",
        val workTitle: String = "Artwork",
        val isRemix: Boolean = true,
        val isVerified: Boolean = false,
        val workThumbnail: String = "",
        val onViewProfile: (() -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Comment(
        val userName: String,
        val userAvatar: String = "",
        val commentText: String = "",
        val targetTitle: String = "your post",
        val targetThumbnail: String = "",
        val isVerified: Boolean = false,
        val isMention: Boolean = false,
        val onReply: ((String) -> Unit)? = null,
        val onViewProfile: (() -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class StoryReaction(
        val userName: String,
        val userAvatar: String = "",
        val reactionEmoji: String = "❤️",
        val storyThumbnail: String = "",
        val isVerified: Boolean = false,
        val replyText: String = "",
        val onReply: ((String) -> Unit)? = null,
        val onViewProfile: (() -> Unit)? = null,
        val onMarkAsRead: (() -> Unit)? = null,
        val onClick: (() -> Unit)? = null
    ) : DynamicIslandEvent()

    data class Status(
        val title: String,
        val message: String = "",
        val icon: ImageVector? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
        val durationMs: Long = 4000L,
        val secondaryActionLabel: String? = null,
        val onSecondaryAction: (() -> Unit)? = null,
        val onDismiss: (() -> Unit)? = null
    ) : DynamicIslandEvent()
}

/**
 * Singleton manager to post dynamic notifications into the Apple Dynamic Island HUD.
 */
object DynamicIslandManager {
    var isSuppressed by mutableStateOf(false)

    // The chat room ID currently active and visible to user (null when browsing Discover, Feed, Home, etc.)
    var currentlyActiveRoomId: String? by mutableStateOf(null)

    private val _events = MutableSharedFlow<DynamicIslandEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun post(event: DynamicIslandEvent) {
        if (isSuppressed) return
        _events.tryEmit(event)
    }

    fun showChatMessage(
        roomId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        timeText: String = "now",
        isVerified: Boolean = false,
        onReply: ((String) -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onQuickReact: ((String) -> Unit)? = null,
        onMute: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        // Strict requirement: DO NOT show Dynamic Island if user is already inside this specific chat!
        if (roomId.isNotBlank() && currentlyActiveRoomId == roomId) {
            return
        }

        post(
            DynamicIslandEvent.ChatMessage(
                roomId = roomId,
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
                timeText = timeText,
                isVerified = isVerified,
                onReply = onReply,
                onMarkAsRead = onMarkAsRead,
                onQuickReact = onQuickReact,
                onMute = onMute,
                onClick = onClick
            )
        )
    }

    fun showLike(
        userName: String,
        userAvatar: String = "",
        targetTitle: String = "your post",
        targetThumbnail: String = "",
        isVerified: Boolean = false,
        onViewProfile: (() -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Like(
                userName = userName,
                userAvatar = userAvatar,
                targetTitle = targetTitle,
                targetThumbnail = targetThumbnail,
                isVerified = isVerified,
                onViewProfile = onViewProfile,
                onMarkAsRead = onMarkAsRead,
                onClick = onClick
            )
        )
    }

    fun showFollow(
        userId: String,
        userName: String,
        userAvatar: String = "",
        isVerified: Boolean = false,
        isFollowRequest: Boolean = false,
        onAccept: (() -> Unit)? = null,
        onDecline: (() -> Unit)? = null,
        onFollowBack: (() -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Follow(
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                isVerified = isVerified,
                isFollowRequest = isFollowRequest,
                onAccept = onAccept,
                onDecline = onDecline,
                onFollowBack = onFollowBack,
                onMarkAsRead = onMarkAsRead,
                onClick = onClick
            )
        )
    }

    fun showRemix(
        userName: String,
        userAvatar: String = "",
        workTitle: String = "Artwork",
        isRemix: Boolean = true,
        isVerified: Boolean = false,
        workThumbnail: String = "",
        onViewProfile: (() -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.RemixOrDownload(
                userName = userName,
                userAvatar = userAvatar,
                workTitle = workTitle,
                isRemix = isRemix,
                isVerified = isVerified,
                workThumbnail = workThumbnail,
                onViewProfile = onViewProfile,
                onMarkAsRead = onMarkAsRead,
                onClick = onClick
            )
        )
    }

    fun showComment(
        userName: String,
        userAvatar: String = "",
        commentText: String = "",
        targetTitle: String = "your post",
        targetThumbnail: String = "",
        isVerified: Boolean = false,
        isMention: Boolean = false,
        onReply: ((String) -> Unit)? = null,
        onViewProfile: (() -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Comment(
                userName = userName,
                userAvatar = userAvatar,
                commentText = commentText,
                targetTitle = targetTitle,
                targetThumbnail = targetThumbnail,
                isVerified = isVerified,
                isMention = isMention,
                onReply = onReply,
                onViewProfile = onViewProfile,
                onMarkAsRead = onMarkAsRead,
                onClick = onClick
            )
        )
    }

    fun showStoryReaction(
        userName: String,
        userAvatar: String = "",
        reactionEmoji: String = "❤️",
        storyThumbnail: String = "",
        isVerified: Boolean = false,
        replyText: String = "",
        onReply: ((String) -> Unit)? = null,
        onViewProfile: (() -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.StoryReaction(
                userName = userName,
                userAvatar = userAvatar,
                reactionEmoji = reactionEmoji,
                storyThumbnail = storyThumbnail,
                isVerified = isVerified,
                replyText = replyText,
                onReply = onReply,
                onViewProfile = onViewProfile,
                onMarkAsRead = onMarkAsRead,
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
        secondaryActionLabel: String? = null,
        onSecondaryAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        onAction: (() -> Unit)? = null
    ) {
        post(
            DynamicIslandEvent.Status(
                title = title,
                message = message,
                icon = icon,
                actionLabel = actionLabel,
                durationMs = durationMs,
                secondaryActionLabel = secondaryActionLabel,
                onSecondaryAction = onSecondaryAction,
                onDismiss = onDismiss,
                onAction = onAction
            )
        )
    }
}
