package com.chroma.studio.chat.data
import com.chroma.studio.R

import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter bridging ChromaStudio backend models (ChatRoom, ChatMessage, UserProfile, UserStoryGroup)
 * with the Apple iOS 18 Chat Presentation Layer.
 */
object ChatAdapters {

    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val storyTimeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatSmartTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = java.util.Calendar.getInstance()
        val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                      now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
        if (isToday) {
            return timeFormatter.format(Date(timestamp))
        }

        val yesterday = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                          yesterday.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
        if (isYesterday) {
            return "Yesterday"
        }

        val daysDiff = (now.timeInMillis - timestamp) / (24 * 60 * 60 * 1000)
        if (daysDiff in 2..6) {
            return SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        }

        return SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestamp))
    }

    fun toUser(profile: UserProfile?, currentUid: String): User {
        return User(
            id = profile?.uid ?: currentUid,
            name = profile?.displayName?.takeIf { it.isNotBlank() } ?: profile?.username?.takeIf { it.isNotBlank() } ?: "Oscellion User",
            username = if (profile?.username?.isNotBlank() == true) "@${profile.username}" else "@user",
            avatar = profile?.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy),
            banner = profile?.bannerUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_banner_joy),
            status = if (profile?.online == true) "Online" else "Active on Oscellion",
            isVerified = profile?.verified ?: false
        )
    }

    fun toDisplayMessage(
        msg: com.chroma.studio.model.ChatMessage,
        currentUid: String,
        userProfiles: Map<String, UserProfile>,
        allMessagesMap: Map<String, com.chroma.studio.model.ChatMessage> = emptyMap(),
        isOtherOnline: Boolean = false
    ): ChatMessage {
        val isMe = msg.senderId == currentUid
        val senderProfile = userProfiles[msg.senderId]
        val senderName = if (isMe) "You" else (senderProfile?.displayName?.takeIf { it.isNotBlank() } ?: senderProfile?.username?.takeIf { it.isNotBlank() } ?: "User")

        val timeStr = timeFormatter.format(Date(msg.timestamp))

        val statusStr = when {
            msg.isPending -> "pending"
            msg.isError -> "error"
            msg.isRead -> "read"
            isOtherOnline -> "delivered"
            else -> "sent"
        }

        // Aggregate reactions: emoji -> count
        val reactionCounts = mutableMapOf<String, Int>()
        var myReaction: String? = null
        msg.reactions.forEach { (uid, emoji) ->
            if (emoji.isNotBlank()) {
                reactionCounts[emoji] = (reactionCounts[emoji] ?: 0) + 1
                if (uid == currentUid) {
                    myReaction = emoji
                }
            }
        }

        val mappedMediaType = when (msg.messageType.lowercase()) {
            "audio", "voice" -> "voice"
            "image" -> "image"
            "video" -> "video"
            "call" -> "call"
            "file" -> "file"
            else -> if (msg.mediaUrl != null || msg.imageUrl != null) "image" else null
        }

        val replyToQuote = msg.replyToMessageId?.let { parentId ->
            val parent = allMessagesMap[parentId]
            if (parent != null) {
                val parentIsMe = parent.senderId == currentUid
                val parentProfile = userProfiles[parent.senderId]
                val parentAuthor = if (parentIsMe) "You" else (parentProfile?.displayName?.takeIf { it.isNotBlank() } ?: parentProfile?.username?.takeIf { it.isNotBlank() } ?: "User")
                val parentText = when {
                    parent.messageType.equals("audio", ignoreCase = true) || parent.messageType.equals("voice", ignoreCase = true) -> "🎤 Voice message"
                    parent.messageType.equals("image", ignoreCase = true) -> "📷 Photo"
                    parent.messageType.equals("video", ignoreCase = true) -> "🎥 Video"
                    parent.messageType.equals("file", ignoreCase = true) -> "📄 Document"
                    parent.text.isNotBlank() -> parent.text
                    else -> "Message"
                }
                ReplyQuote(
                    author = parentAuthor,
                    text = parentText,
                    messageId = parent.id,
                    mediaType = parent.messageType
                )
            } else {
                null
            }
        }

        return ChatMessage(
            id = msg.id,
            senderId = msg.senderId,
            senderName = senderName,
            text = if (mappedMediaType == "voice") null else msg.text.takeIf { it.isNotBlank() },
            mediaType = mappedMediaType,
            mediaUrl = msg.imageUrl ?: msg.mediaUrl,
            mediaUrls = listOfNotNull(msg.imageUrl ?: msg.mediaUrl),
            localUri = if (mappedMediaType == "voice") (msg.imageUrl ?: msg.mediaUrl) else null,
            fileName = if (mappedMediaType == "file") "Document" else null,
            fileSize = null,
            fileItems = emptyList(),
            voiceDuration = msg.durationSeconds ?: 4,
            waveformSamples = if (mappedMediaType == "voice") listOf(
                0.22f, 0.38f, 0.65f, 0.95f, 0.70f, 0.40f, 0.85f, 1.00f, 0.75f, 0.45f,
                0.90f, 0.80f, 0.55f, 0.30f, 0.65f, 0.95f, 1.00f, 0.70f, 0.40f, 0.85f,
                0.95f, 0.60f, 0.35f, 0.75f, 0.90f, 0.65f, 0.40f, 0.22f
            ) else emptyList(),
            transcription = null,
            isTranscribing = false,
            pollData = null,
            locationTitle = null,
            callDuration = null,
            callType = null,
            time = timeStr,
            status = statusStr,
            reactions = reactionCounts,
            userReaction = myReaction,
            replyTo = replyToQuote,
            isEdited = false
        )
    }

    fun toChatConversation(
        room: ChatRoom,
        currentUid: String,
        userProfiles: Map<String, UserProfile>,
        isOnline: Boolean = false,
        lastSeenTime: Long? = null,
        typingMap: Map<String, String> = emptyMap(),
        messages: List<ChatMessage> = emptyList()
    ): ChatConversation {
        val otherUid = if (room.isGroup) null else room.participants.firstOrNull { it != currentUid }
        val otherProfile = if (otherUid != null) userProfiles[otherUid] else null

        val chatName = if (room.isGroup) {
            room.name ?: "Oscellion Group"
        } else {
            otherProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: otherProfile?.username?.takeIf { it.isNotBlank() }
                ?: room.name
                ?: "Oscellion User"
        }

        val avatar = if (room.isGroup) {
            room.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy)
        } else {
            otherProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: room.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy)
        }

        val banner = otherProfile?.bannerUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_banner_joy)

        val unread = room.unreadCount[currentUid] ?: 0

        val otherTyping = typingMap.filterKeys { it != currentUid }
        val activeUid = otherTyping.keys.firstOrNull()
        val activeActivity = if (activeUid != null) otherTyping[activeUid] else null
        val activeUserProfile = if (activeUid != null) userProfiles[activeUid] else null
        val activeUserName = if (activeUserProfile != null) {
            activeUserProfile.displayName.takeIf { it.isNotBlank() }
                ?: activeUserProfile.username.takeIf { it.isNotBlank() }
                ?: "Someone"
        } else "Someone"

        val statusText = when {
            activeActivity == "recording" -> {
                if (room.isGroup) "$activeUserName is recording audio..." else "recording audio..."
            }
            activeActivity == "typing" -> {
                if (room.isGroup) "$activeUserName is typing..." else "typing..."
            }
            isOnline -> "Online"
            lastSeenTime != null && lastSeenTime > 0 -> "Last seen ${formatSmartTimestamp(lastSeenTime)}"
            else -> if (room.isGroup) "${room.participants.size} members" else "Offline"
        }

        val membersList = if (room.isGroup) {
            room.participants.map { uid ->
                val memberProf = userProfiles[uid]
                GroupMember(
                    name = if (uid == currentUid) "You" else (memberProf?.displayName ?: memberProf?.username ?: "Member"),
                    role = if (room.admins.contains(uid)) "Admin" else "Member",
                    avatar = memberProf?.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy)
                )
            }
        } else null

        val resolvedMessages = if (messages.isNotEmpty()) {
            messages
        } else if (room.lastMessage.isNotBlank() || room.lastMessageTime > 0L) {
            val lastSenderProf = if (room.lastSender != null) userProfiles[room.lastSender] else null
            val senderName = if (room.lastSender == currentUid) "You" else (lastSenderProf?.displayName ?: lastSenderProf?.username ?: chatName)
            val otherUnread = if (otherUid != null) room.unreadCount[otherUid] ?: 0 else 0
            val computedStatus = when {
                room.lastSender != currentUid -> "delivered"
                otherUnread == 0 -> "read"
                isOnline -> "delivered"
                else -> "sent"
            }
            listOf(
                ChatMessage(
                    id = "last_${room.id}",
                    senderId = room.lastSender ?: "",
                    senderName = senderName,
                    text = room.lastMessage.ifBlank { null },
                    time = formatSmartTimestamp(room.lastMessageTime),
                    status = computedStatus
                )
            )
        } else {
            emptyList()
        }

        val isVerified = if (room.isGroup) false else (otherProfile?.verified ?: false)

        return ChatConversation(
            id = room.id,
            name = chatName,
            type = if (room.isGroup) "group" else "direct",
            avatar = avatar,
            banner = banner,
            online = isOnline,
            statusText = statusText,
            phone = otherProfile?.socialLinks?.get("phone") ?: "",
            bio = otherProfile?.bio ?: room.groupDescription ?: "",
            unreadCount = unread,
            pinnedMessage = null,
            isMuted = false,
            isBlocked = false,
            isVerified = isVerified,
            members = membersList,
            messages = resolvedMessages
        )
    }

    fun toStoriesList(
        storyGroups: List<UserStoryGroup>,
        currentUid: String
    ): List<Story> {
        val result = mutableListOf<Story>()
        for (group in storyGroups) {
            val prof = group.userProfile
            val authorName = if (group.userId == currentUid) "Your Story" else (prof.displayName.takeIf { it.isNotBlank() } ?: prof.username.takeIf { it.isNotBlank() } ?: "User")
            val avatar = prof.avatarUrl.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy)

            for (s in group.stories) {
                result.add(
                    Story(
                        id = s.id,
                        authorName = authorName,
                        avatar = avatar,
                        timeAgo = storyTimeFormatter.format(Date(s.timestamp)),
                        image = s.mediaUrl.takeIf { it.isNotBlank() },
                        caption = s.textContent,
                        seen = group.allSeen,
                        isMyStory = group.userId == currentUid,
                        viewCount = s.views,
                        likesCount = s.likes,
                        viewers = emptyList()
                    )
                )
            }
        }
        return result
    }
}


