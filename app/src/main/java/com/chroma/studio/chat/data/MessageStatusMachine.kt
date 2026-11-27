package com.chroma.studio.chat.data

import com.chroma.studio.model.ChatMessage as DomainChatMessage
import com.chroma.studio.model.ChatRoom

/**
 * Canonical monotonic delivery states for outgoing messages.
 *
 * Ordinal rank enforces one-way server progress (`PENDING -> SENT -> DELIVERED -> READ`).
 * `ERROR` is a terminal local-failure state that only transitions back to `PENDING` via explicit retry.
 */
enum class MessageDeliveryStatus(val wireValue: String, val rank: Int) {
    ERROR("error", -1),
    PENDING("pending", 0),
    SENT("sent", 1),
    DELIVERED("delivered", 2),
    READ("read", 3);

    companion object {
        fun fromWire(value: String?): MessageDeliveryStatus {
            return when (value?.trim()?.lowercase()) {
                "error", "failed" -> ERROR
                "pending" -> PENDING
                "delivered" -> DELIVERED
                "read", "seen" -> READ
                else -> SENT
            }
        }
    }
}

enum class SendFailureReason {
    NETWORK_OFFLINE,
    MEDIA_UPLOAD_FAILED,
    PERMISSION_DENIED,
    ROOM_NOT_FOUND,
    UNAUTHENTICATED,
    UNKNOWN
}

sealed class SendMessageResult {
    data object Success : SendMessageResult()
    data class Failure(
        val reason: SendFailureReason,
        val userMessage: String,
        val isRetryable: Boolean = true
    ) : SendMessageResult()
}

data class OutboxRoomStatus(
    val messageId: String,
    val status: MessageDeliveryStatus,
    val errorReason: String? = null
)

enum class FcmPushKind {
    CHAT_MESSAGE,
    SOCIAL_ACTIVITY,
    APP_UPDATE
}

/**
 * Classifies incoming FCM data maps accurately so Likes, Follows, Comments, Remixes,
 * and System notifications are never mistakenly rendered as Chat Messages with
 * "Reply" / "Mark as read" buttons.
 */
object FcmPayloadClassifier {
    private val socialExplicitTypes = setOf(
        "LIKE",
        "LIKE_COMMENT",
        "COMMENT",
        "REPLY",
        "MENTION",
        "FOLLOW",
        "FOLLOW_REQUEST",
        "REMIX",
        "STORY_LIKE",
        "STORY_REACTION",
        "STORY_REPLY",
        "PROJECT_COLLAB",
        "PROJECT_EXPORT",
        "SYSTEM_DROP"
    )

    fun classify(data: Map<String, String>): FcmPushKind {
        val rawNotifType = (data["notificationType"] ?: data["type"] ?: "").trim().uppercase()
        if (rawNotifType == "APP_UPDATE") return FcmPushKind.APP_UPDATE
        if (rawNotifType == "MESSAGE" || rawNotifType == "CHAT_MESSAGE" || rawNotifType == "CHAT") {
            return FcmPushKind.CHAT_MESSAGE
        }
        if (rawNotifType in socialExplicitTypes) {
            return FcmPushKind.SOCIAL_ACTIVITY
        }

        val rawMessageType = (data["messageType"] ?: data["message_type"] ?: "").trim().uppercase()
        if (rawMessageType in socialExplicitTypes) {
            return FcmPushKind.SOCIAL_ACTIVITY
        }

        val roomId = (data["roomId"] ?: data["room_id"] ?: "").trim()
        val messageId = (data["messageId"] ?: "").trim()
        val isExplicitNonChatRoomPrefix = roomId.startsWith("post_", ignoreCase = true) ||
            roomId.startsWith("flash_", ignoreCase = true) ||
            roomId.startsWith("collab", ignoreCase = true)
        val looksLikeChatRoom = !isExplicitNonChatRoomPrefix &&
            (roomId.startsWith("group_") || roomId.contains("_"))
        if (looksLikeChatRoom || messageId.isNotBlank()) {
            return FcmPushKind.CHAT_MESSAGE
        }

        return FcmPushKind.SOCIAL_ACTIVITY
    }
}

/**
 * Pure JVM thread-safe event deduplicator with TTL expiration.
 * Shared across FCM pushes and Firestore listeners so the same incoming message
 * never rings or pops Dynamic Island more than once.
 */
class InAppEventDeduplicator(private val ttlMs: Long = 120_000L) {
    private val recentKeys = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun canonicalMessageKey(
        roomId: String,
        messageId: String,
        senderId: String = "",
        timestamp: Long = 0L,
        messageText: String = ""
    ): String {
        return if (messageId.isNotBlank()) {
            "msg:${roomId}:${messageId}"
        } else {
            "fp:${roomId}:${senderId}:${timestamp}:${messageText.hashCode()}"
        }
    }

    fun shouldProcess(key: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (recentKeys.size > 256) {
            val cutoff = nowMs - ttlMs
            recentKeys.entries.removeIf { it.value < cutoff }
        }
        val prev = recentKeys.putIfAbsent(key, nowMs)
        if (prev == null) return true
        return if (nowMs - prev >= ttlMs) {
            recentKeys[key] = nowMs
            true
        } else {
            false
        }
    }

    fun clear() {
        recentKeys.clear()
    }
}

/**
 * Pure deterministic message delivery/read & monotonic reconciliation engine.
 * Strictly separates user online presence from message delivery and read receipts.
 */
object MessageStatusMachine {

    /**
     * Resolves the canonical tick status for a domain [DomainChatMessage].
     * Never infers delivery from another user's online presence.
     */
    fun resolveMessageStatus(
        msg: DomainChatMessage,
        currentUid: String,
        groupParticipants: List<String> = emptyList()
    ): MessageDeliveryStatus {
        if (msg.isError) return MessageDeliveryStatus.ERROR
        if (msg.isPending) return MessageDeliveryStatus.PENDING

        val otherMembers = if (groupParticipants.isNotEmpty()) {
            groupParticipants.filter { it.isNotBlank() && it != currentUid && it != msg.senderId }
        } else {
            emptyList()
        }
        val isGroupMsg = otherMembers.size > 1 || groupParticipants.size > 2

        if (isGroupMsg && otherMembers.isNotEmpty()) {
            return when {
                otherMembers.all { msg.readBy.containsKey(it) } -> MessageDeliveryStatus.READ
                msg.readBy.keys.any { it in otherMembers } ||
                    msg.deliveredTo.keys.any { it in otherMembers } ||
                    (msg.deliveredTo.isEmpty() && msg.isDelivered) -> MessageDeliveryStatus.DELIVERED
                else -> MessageDeliveryStatus.SENT
            }
        }

        val isRead = msg.isRead || msg.readBy.keys.any { it.isNotBlank() && it != msg.senderId }
        if (isRead) return MessageDeliveryStatus.READ

        val isDelivered = msg.isDelivered || msg.deliveredTo.keys.any { it.isNotBlank() && it != msg.senderId }
        if (isDelivered) return MessageDeliveryStatus.DELIVERED

        return MessageDeliveryStatus.SENT
    }

    /**
     * Resolves the inbox row tick status for [room].
     * Strictly uses Firestore room receipt flags and explicit local outbox status —
     * NEVER infers "read" from unreadCount == 0 or "delivered" from online presence.
     */
    fun resolveRoomPreviewStatus(
        room: ChatRoom,
        currentUid: String,
        localOutboxStatus: OutboxRoomStatus? = null
    ): MessageDeliveryStatus {
        if (room.lastSender != currentUid) {
            return if ((room.unreadCount[currentUid] ?: 0) == 0) {
                MessageDeliveryStatus.READ
            } else {
                MessageDeliveryStatus.DELIVERED
            }
        }

        if (localOutboxStatus != null &&
            (localOutboxStatus.messageId == room.lastMessageId || room.lastMessageId.isNullOrBlank())
        ) {
            if (localOutboxStatus.status == MessageDeliveryStatus.ERROR) {
                return MessageDeliveryStatus.ERROR
            }
            if (localOutboxStatus.status == MessageDeliveryStatus.PENDING &&
                !room.lastMessageIsDelivered &&
                !room.lastMessageIsRead
            ) {
                return MessageDeliveryStatus.PENDING
            }
        }

        return when {
            room.lastMessageIsRead -> MessageDeliveryStatus.READ
            room.lastMessageIsDelivered -> MessageDeliveryStatus.DELIVERED
            else -> MessageDeliveryStatus.SENT
        }
    }

    /**
     * Monotonically reconciles an existing cached/previous [previous] message with a newly arrived
     * [incoming] snapshot for the same message ID so receipt ticks never regress backwards.
     */
    fun mergeMonotonic(
        previous: DomainChatMessage?,
        incoming: DomainChatMessage,
        currentUid: String,
        groupParticipants: List<String> = emptyList()
    ): DomainChatMessage {
        if (previous == null) return incoming

        // Local failed state must stay ERROR until a confirmed (non-pending) server copy arrives
        // or the user explicitly retries (which replaces local with isPending = true, isError = false).
        if (previous.isError && incoming.isPending) {
            return incoming.copy(
                isPending = false,
                isError = true,
                errorReason = previous.errorReason ?: incoming.errorReason,
                uploadProgress = null
            )
        }

        // Preserve active media upload progress while local send is still uploading or pending commit.
        if (previous.isPending && previous.uploadProgress != null && incoming.isPending) {
            return incoming.copy(
                isPending = true,
                uploadProgress = previous.uploadProgress
            )
        }

        val mergedDeliveredTo = buildMap<String, Long> {
            putAll(previous.deliveredTo)
            incoming.deliveredTo.forEach { (uid, ts) ->
                val cur = this[uid]
                if (cur == null || (ts > 0L && (cur <= 0L || ts < cur))) {
                    put(uid, ts)
                }
            }
        }
        val mergedReadBy = buildMap<String, Long> {
            putAll(previous.readBy)
            incoming.readBy.forEach { (uid, ts) ->
                val cur = this[uid]
                if (cur == null || (ts > 0L && (cur <= 0L || ts < cur))) {
                    put(uid, ts)
                }
            }
        }

        val prevStatus = resolveMessageStatus(previous, currentUid, groupParticipants)
        val incStatus = resolveMessageStatus(incoming, currentUid, groupParticipants)

        // Never regress back to PENDING once server already confirmed SENT/DELIVERED/READ.
        val resolvedPending = if (prevStatus.rank >= MessageDeliveryStatus.SENT.rank) {
            false
        } else {
            incoming.isPending
        }

        val otherMembers = groupParticipants.filter { it.isNotBlank() && it != currentUid && it != incoming.senderId }
        val isGroupMsg = otherMembers.size > 1 || groupParticipants.size > 2

        val mergedRead = if (isGroupMsg && otherMembers.isNotEmpty()) {
            otherMembers.all { mergedReadBy.containsKey(it) } ||
                ((previous.isRead || incoming.isRead) && mergedReadBy.isEmpty())
        } else {
            previous.isRead || incoming.isRead || mergedReadBy.keys.any { it.isNotBlank() && it != incoming.senderId }
        }

        val mergedDelivered = mergedRead ||
            previous.isDelivered ||
            incoming.isDelivered ||
            mergedDeliveredTo.keys.any { it.isNotBlank() && it != incoming.senderId }

        val mergedUploadProgress = if (resolvedPending) {
            incoming.uploadProgress ?: previous.uploadProgress
        } else null

        val mergedError = if (incStatus.rank >= MessageDeliveryStatus.SENT.rank && !resolvedPending) {
            false
        } else {
            incoming.isError
        }

        return incoming.copy(
            isPending = resolvedPending,
            isError = mergedError,
            errorReason = if (mergedError) (incoming.errorReason ?: previous.errorReason) else null,
            uploadProgress = mergedUploadProgress,
            deliveredTo = mergedDeliveredTo,
            readBy = mergedReadBy,
            isDelivered = mergedDelivered,
            isRead = mergedRead
        )
    }
}
