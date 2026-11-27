package com.chroma.studio.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Wraps each [DynamicIslandEvent] in a unique monotonic [instanceId] so animation,
 * auto-dismiss timers, and swipe-dismiss calls are strictly scoped to one presentation
 * and can never cancel a newer event.
 */
data class DynamicIslandPresentation(
    val instanceId: Long,
    val event: DynamicIslandEvent,
    val enqueuedAtMs: Long = System.currentTimeMillis()
)

/**
 * Pure JVM, thread-safe coordinator managing Dynamic Island queueing, priority preemption,
 * interaction locks (during inline reply/drag), and generation-safe dismissal.
 */
class DynamicIslandQueueCoordinator(
    private val maxQueueSize: Int = 4
) {
    private val idGenerator = AtomicLong(0L)
    private val lock = Any()
    private val pendingQueue = ArrayDeque<DynamicIslandPresentation>()

    @Volatile
    private var userInteracting: Boolean = false

    @Volatile
    private var suppressed: Boolean = false

    private val _currentPresentation = MutableStateFlow<DynamicIslandPresentation?>(null)
    val currentPresentation: StateFlow<DynamicIslandPresentation?> = _currentPresentation.asStateFlow()

    fun enqueue(event: DynamicIslandEvent, nowMs: Long = System.currentTimeMillis()): DynamicIslandPresentation? {
        synchronized(lock) {
            if (suppressed) return null

            val presentation = DynamicIslandPresentation(
                instanceId = idGenerator.incrementAndGet(),
                event = event,
                enqueuedAtMs = nowMs
            )

            val current = _currentPresentation.value
            if (current == null) {
                _currentPresentation.value = presentation
                return presentation
            }

            // Protect expanded cards / inline reply / active finger drag from sudden clobbering
            if (userInteracting) {
                if (event is DynamicIslandEvent.Status) {
                    // Coalesce queued passive Status toasts so queue doesn't flood
                    val iter = pendingQueue.iterator()
                    while (iter.hasNext()) {
                        if (iter.next().event is DynamicIslandEvent.Status) {
                            iter.remove()
                        }
                    }
                }
                while (pendingQueue.size >= maxQueueSize) {
                    pendingQueue.removeFirst()
                }
                pendingQueue.addLast(presentation)
                return presentation
            }

            // Preempt idle Status banners immediately for incoming interactive notifications
            if (event is DynamicIslandEvent.IncomingCall) {
                val iter = pendingQueue.iterator()
                while (iter.hasNext()) {
                    if (iter.next().event is DynamicIslandEvent.IncomingCall) {
                        iter.remove()
                    }
                }
                _currentPresentation.value = presentation
                userInteracting = false
                return presentation
            }

            // Preempt idle Status banners immediately for incoming interactive notifications
            if (current.event is DynamicIslandEvent.Status || event is DynamicIslandEvent.Status) {
                if (current.event is DynamicIslandEvent.Status && event !is DynamicIslandEvent.Status) {
                    _currentPresentation.value = presentation
                    return presentation
                }
                // Consecutive idle Status replaces immediately without queueing stale toasts
                if (current.event is DynamicIslandEvent.Status && event is DynamicIslandEvent.Status) {
                    _currentPresentation.value = presentation
                    return presentation
                }
                // New Status while a non-Status alert is idle -> queue briefly behind it (coalesced)
                val iter = pendingQueue.iterator()
                while (iter.hasNext()) {
                    if (iter.next().event is DynamicIslandEvent.Status) {
                        iter.remove()
                    }
                }
                while (pendingQueue.size >= maxQueueSize) {
                    pendingQueue.removeFirst()
                }
                pendingQueue.addLast(presentation)
                return presentation
            }

            // Current is an un-interacted social/chat alert and new alert arrived -> queue instead of cutting off animation!
            while (pendingQueue.size >= maxQueueSize) {
                pendingQueue.removeFirst()
            }
            pendingQueue.addLast(presentation)
            return presentation
        }
    }

    /**
     * Dismisses the currently visible presentation ONLY if [expectedInstanceId] matches
     * (or if [expectedInstanceId] is null), then promotes the next queued presentation if any.
     */
    fun dismiss(expectedInstanceId: Long? = null): DynamicIslandPresentation? {
        synchronized(lock) {
            val current = _currentPresentation.value ?: return null
            if (expectedInstanceId != null && current.instanceId != expectedInstanceId) {
                return current
            }
            userInteracting = false
            val next = if (!suppressed && pendingQueue.isNotEmpty()) pendingQueue.removeFirst() else null
            _currentPresentation.value = next
            return next
        }
    }

    fun setUserInteracting(interacting: Boolean) {
        synchronized(lock) {
            userInteracting = interacting
        }
    }

    fun setSuppressed(isSuppressed: Boolean) {
        synchronized(lock) {
            suppressed = isSuppressed
            if (isSuppressed) {
                pendingQueue.clear()
                _currentPresentation.value = null
                userInteracting = false
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            pendingQueue.clear()
            _currentPresentation.value = null
            userInteracting = false
        }
    }

    fun pendingCount(): Int = synchronized(lock) { pendingQueue.size }
}

/**
 * Sealed hierarchy of all Dynamic Island / Notch in-app notifications.
 */
sealed class DynamicIslandEvent {
    data class ChatMessage(
        val roomId: String,
        val messageId: String = "",
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

    data class IncomingCall(
        val callId: String,
        val roomId: String,
        val callerName: String,
        val callerAvatar: String = "",
        val isVideo: Boolean = false,
        val isVerified: Boolean = false,
        val onAccept: () -> Unit,
        val onDecline: () -> Unit,
        val onExpand: () -> Unit
    ) : DynamicIslandEvent()
}

/**
 * Singleton manager to post dynamic notifications into the Apple Dynamic Island HUD.
 */
object DynamicIslandManager {
    private val coordinator = DynamicIslandQueueCoordinator(maxQueueSize = 4)

    var isSuppressed: Boolean
        get() = _isSuppressedState
        set(value) {
            _isSuppressedState = value
            coordinator.setSuppressed(value)
        }

    private var _isSuppressedState by mutableStateOf(false)

    // True when ChatScreen's live Call or Voice Recording Dynamic Island occupies the notch area.
    var isLiveActivityActive by mutableStateOf(false)

    // The chat room ID currently active and visible to user (null when browsing Discover, Feed, Home, etc.)
    var currentlyActiveRoomId: String? by mutableStateOf(null)

    val currentPresentation: StateFlow<DynamicIslandPresentation?> = coordinator.currentPresentation

    private val _events = MutableSharedFlow<DynamicIslandEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    private val recentChatEventTimes = ConcurrentHashMap<String, Long>()

    fun post(event: DynamicIslandEvent) {
        if (isSuppressed) return
        // Avoid overlapping passive Status toasts when a live Call / Voice Memo Globe island is active
        if (isLiveActivityActive && event is DynamicIslandEvent.Status) return
        coordinator.enqueue(event)
        _events.tryEmit(event)
    }

    fun dismissCurrent(expectedInstanceId: Long? = null) {
        coordinator.dismiss(expectedInstanceId)
    }

    fun setUserInteracting(interacting: Boolean) {
        coordinator.setUserInteracting(interacting)
    }

    fun showChatMessage(
        roomId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        timeText: String = "now",
        isVerified: Boolean = false,
        messageId: String = "",
        onReply: ((String) -> Unit)? = null,
        onMarkAsRead: (() -> Unit)? = null,
        onQuickReact: ((String) -> Unit)? = null,
        onMute: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        // Strict requirement: DO NOT show Dynamic Island if user is already visibly viewing this specific chat!
        if (roomId.isNotBlank() &&
            (com.chroma.studio.ChatStateManager.isChatScreenVisible(roomId) ||
                (currentlyActiveRoomId == roomId && com.chroma.studio.ChatStateManager.isChatSurfaceVisible.value))
        ) {
            return
        }

        // Secondary defense-in-depth dedup at the DynamicIslandManager boundary
        val now = System.currentTimeMillis()
        if (recentChatEventTimes.size > 128) {
            recentChatEventTimes.entries.removeIf { now - it.value > 120_000L }
        }
        val key = if (messageId.isNotBlank()) "msg:${roomId}:${messageId}" else "txt:${roomId}:${senderName}:${messageText}"
        val ttl = if (messageId.isNotBlank()) 120_000L else 4_500L
        val lastShown = recentChatEventTimes[key]
        if (lastShown != null && (now - lastShown) < ttl) {
            return
        }
        recentChatEventTimes[key] = now

        post(
            DynamicIslandEvent.ChatMessage(
                roomId = roomId,
                messageId = messageId,
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

    fun showIncomingCall(
        callId: String,
        roomId: String,
        callerName: String,
        callerAvatar: String = "",
        isVideo: Boolean = false,
        isVerified: Boolean = false,
        onAccept: () -> Unit,
        onDecline: () -> Unit,
        onExpand: () -> Unit
    ) {
        val current = currentPresentation.value
        val activeCall = current?.event as? DynamicIslandEvent.IncomingCall
        if (activeCall != null && activeCall.callId == callId) {
            return
        }
        post(
            DynamicIslandEvent.IncomingCall(
                callId = callId,
                roomId = roomId,
                callerName = callerName,
                callerAvatar = callerAvatar,
                isVideo = isVideo,
                isVerified = isVerified,
                onAccept = onAccept,
                onDecline = onDecline,
                onExpand = onExpand
            )
        )
    }

    fun dismissIncomingCall(callId: String? = null) {
        val current = currentPresentation.value ?: return
        val incoming = current.event as? DynamicIslandEvent.IncomingCall ?: return
        if (callId == null || incoming.callId == callId) {
            coordinator.dismiss(expectedInstanceId = current.instanceId)
        }
    }
}
