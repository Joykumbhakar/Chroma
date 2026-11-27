package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

private const val SAFE_MAPPER_TAG = "FirestoreSafeMapper"

private fun Any?.asLongCompat(default: Long = 0L): Long = when (this) {
    is Number -> this.toLong()
    is Timestamp -> this.toDate().time
    is String -> this.toLongOrNull() ?: default
    else -> default
}

private fun Any?.asNullableLongCompat(): Long? = when (this) {
    is Number -> this.toLong()
    is Timestamp -> this.toDate().time
    is String -> this.toLongOrNull()
    else -> null
}

private fun Any?.asIntCompat(default: Int = 0): Int = when (this) {
    is Number -> this.toInt()
    is String -> this.toIntOrNull() ?: default
    else -> default
}

private fun Any?.asBooleanCompat(default: Boolean = false): Boolean = when (this) {
    is Boolean -> this
    is String -> this.equals("true", ignoreCase = true)
    is Number -> this.toInt() != 0
    else -> default
}

private fun Any?.asStringListCompat(): List<String> = when (this) {
    is Iterable<*> -> this.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
    is Array<*> -> this.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
    is String -> if (this.isNotBlank()) listOf(this) else emptyList()
    else -> emptyList()
}

private fun Any?.asStringMapCompat(): Map<String, String> = when (this) {
    is Map<*, *> -> entries.mapNotNull { (k, v) ->
        val key = k?.toString() ?: return@mapNotNull null
        val value = v?.toString() ?: return@mapNotNull null
        key to value
    }.toMap()
    else -> emptyMap()
}

private fun Any?.asIntMapCompat(): Map<String, Int> = when (this) {
    is Map<*, *> -> entries.mapNotNull { (k, v) ->
        val key = k?.toString() ?: return@mapNotNull null
        key to v.asIntCompat(0)
    }.toMap()
    else -> emptyMap()
}

private fun Any?.asLongMapCompat(): Map<String, Long> = when (this) {
    is Map<*, *> -> entries.mapNotNull { (k, v) ->
        val key = k?.toString() ?: return@mapNotNull null
        key to v.asLongCompat(0L)
    }.toMap()
    else -> emptyMap()
}

/**
 * Safely deserializes a [ChatRoom] from a Firestore [DocumentSnapshot].
 * Protects against cross-platform schema differences (e.g. Web `pinnedBy: List<String>`
 * vs legacy Android `pinnedBy: String?`, `Timestamp` vs `Long`, etc.) so a single
 * mismatched field never crashes the UI or listener threads.
 */
fun DocumentSnapshot.toChatRoomSafe(): ChatRoom? {
    if (!exists()) return null
    try {
        val parsed = toObject(ChatRoom::class.java)
        if (parsed != null) {
            return if (parsed.id.isBlank()) parsed.copy(id = id) else parsed
        }
    } catch (t: Throwable) {
        Log.w(SAFE_MAPPER_TAG, "Fallback manual parsing for ChatRoom($id): ${t.message}")
    }

    val d = data ?: return null
    return try {
        ChatRoom(
            id = (d["id"] as? String)?.takeIf { it.isNotBlank() } ?: id,
            name = d["name"] as? String,
            isGroup = (d["isGroup"] ?: d["group"]).asBooleanCompat(false),
            avatarUrl = d["avatarUrl"] as? String,
            participants = d["participants"].asStringListCompat(),
            admins = d["admins"].asStringListCompat(),
            createdBy = d["createdBy"] as? String,
            groupDescription = d["groupDescription"] as? String,
            lastMessage = (d["lastMessage"] as? String) ?: "",
            lastSender = d["lastSender"] as? String,
            lastMessageTime = d["lastMessageTime"].asLongCompat(0L),
            lastMessageId = d["lastMessageId"] as? String,
            lastMessageIsRead = (d["lastMessageIsRead"] ?: d["read"]).asBooleanCompat(false),
            lastMessageIsDelivered = (d["lastMessageIsDelivered"] ?: d["delivered"]).asBooleanCompat(false),
            unreadCount = d["unreadCount"].asIntMapCompat(),
            favoritedBy = d["favoritedBy"].asStringListCompat(),
            archivedBy = d["archivedBy"].asStringListCompat(),
            deletedBy = d["deletedBy"].asStringListCompat(),
            deletedAt = d["deletedAt"].asLongMapCompat(),
            pinnedMessageId = d["pinnedMessageId"] as? String,
            pinnedMessageText = d["pinnedMessageText"] as? String,
            pinnedBy = d["pinnedBy"],
            pinnedMessageBy = (d["pinnedMessageBy"] as? String) ?: (d["pinnedBy"] as? String),
            pinnedAt = d["pinnedAt"].asLongCompat(0L),
            onlyAdminsCanPost = d["onlyAdminsCanPost"].asBooleanCompat(false),
            disappearingHours = (d["disappearingHours"] as? Number)?.toInt() ?: 0,
            inviteCode = d["inviteCode"] as? String,
            isPublicGroup = d["isPublicGroup"].asBooleanCompat(false),
            editInfoAdminsOnly = d["editInfoAdminsOnly"]?.asBooleanCompat(true) ?: true
        )
    } catch (t: Throwable) {
        Log.e(SAFE_MAPPER_TAG, "Failed fallback parsing ChatRoom($id)", t)
        null
    }
}

/**
 * Safely deserializes a [ChatMessage] from a Firestore [DocumentSnapshot].
 */
fun DocumentSnapshot.toChatMessageSafe(): ChatMessage? {
    if (!exists()) return null
    try {
        val parsed = toObject(ChatMessage::class.java)
        if (parsed != null) {
            return if (parsed.id.isBlank()) parsed.copy(id = id) else parsed
        }
    } catch (t: Throwable) {
        Log.w(SAFE_MAPPER_TAG, "Fallback manual parsing for ChatMessage($id): ${t.message}")
    }

    val d = data ?: return null
    return try {
        ChatMessage(
            id = (d["id"] as? String)?.takeIf { it.isNotBlank() } ?: id,
            senderId = (d["senderId"] as? String) ?: "",
            text = (d["text"] as? String) ?: "",
            timestamp = d["timestamp"].asLongCompat(System.currentTimeMillis()),
            isRead = (d["isRead"] ?: d["read"]).asBooleanCompat(false),
            isDelivered = (d["isDelivered"] ?: d["delivered"]).asBooleanCompat(false),
            imageUrl = d["imageUrl"] as? String,
            mediaUrl = d["mediaUrl"] as? String,
            mediaType = d["mediaType"] as? String,
            workId = d["workId"] as? String,
            messageType = (d["messageType"] as? String) ?: "text",
            iv = (d["iv"] as? String) ?: "",
            encryptedAesKeys = d["encryptedAesKeys"].asStringMapCompat(),
            reactions = d["reactions"].asStringMapCompat(),
            durationSeconds = (d["durationSeconds"] as? Number)?.toInt(),
            replyToMessageId = d["replyToMessageId"] as? String,
            pollVotes = d["pollVotes"].asStringMapCompat(),
            isEdited = d["isEdited"].asBooleanCompat(false),
            deletedFor = d["deletedFor"].asStringListCompat(),
            isPollClosed = d["isPollClosed"].asBooleanCompat(false),
            isForwarded = d["isForwarded"].asBooleanCompat(false),
            storyId = d["storyId"] as? String,
            storyThumbnailUrl = d["storyThumbnailUrl"] as? String,
            flashVideoId = d["flashVideoId"] as? String,
            flashVideoThumbnailUrl = d["flashVideoThumbnailUrl"] as? String,
            flashVideoUrl = d["flashVideoUrl"] as? String,
            flashVideoCaption = d["flashVideoCaption"] as? String,
            flashVideoAuthorUsername = d["flashVideoAuthorUsername"] as? String,
            flashVideoAuthorAvatarUrl = d["flashVideoAuthorAvatarUrl"] as? String,
            flashVideoDurationMs = d["flashVideoDurationMs"].asNullableLongCompat(),
            callDuration = d["callDuration"] as? String,
            callType = d["callType"] as? String,
            deliveredTo = d["deliveredTo"].asLongMapCompat(),
            readBy = d["readBy"].asLongMapCompat(),
            fontStyle = d["fontStyle"] as? String
        )
    } catch (t: Throwable) {
        Log.e(SAFE_MAPPER_TAG, "Failed fallback parsing ChatMessage($id)", t)
        null
    }
}

fun DocumentSnapshot.toUserProfileSafe(): UserProfile? {
    if (!exists()) return null
    return try {
        toObject(UserProfile::class.java)
    } catch (t: Throwable) {
        Log.w(SAFE_MAPPER_TAG, "Failed parsing UserProfile($id): ${t.message}")
        null
    }
}
