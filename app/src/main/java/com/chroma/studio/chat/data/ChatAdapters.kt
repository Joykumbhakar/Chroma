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

    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    private val dayOfWeekFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE", Locale.getDefault())
    }

    private val monthDayYearFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    }

    val DEFAULT_WAVEFORM_SAMPLES: List<Float> = listOf(
        0.22f, 0.38f, 0.65f, 0.95f, 0.70f, 0.40f, 0.85f, 1.00f, 0.75f, 0.45f,
        0.90f, 0.80f, 0.55f, 0.30f, 0.65f, 0.95f, 1.00f, 0.70f, 0.40f, 0.85f,
        0.95f, 0.60f, 0.35f, 0.75f, 0.90f, 0.65f, 0.40f, 0.22f
    )

    private fun formatTime(timestamp: Long): String {
        return try {
            timeFormat.get()?.format(Date(timestamp)) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun formatStoryTime(timestamp: Long): String {
        return formatTime(timestamp)
    }

    fun formatSmartTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            val now = java.util.Calendar.getInstance()
            val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

            val isToday = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                          now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
            if (isToday) {
                return formatTime(timestamp)
            }

            val yesterday = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                              yesterday.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
            if (isYesterday) {
                return "Yesterday"
            }

            val daysDiff = (now.timeInMillis - timestamp) / (24 * 60 * 60 * 1000)
            if (daysDiff in 2..6) {
                return dayOfWeekFormat.get()?.format(Date(timestamp)) ?: ""
            }

            monthDayYearFormat.get()?.format(Date(timestamp)) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isDifferentDay(t1: Long, t2: Long): Boolean {
        if (t1 <= 0L || t2 <= 0L) return false
        val c1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(java.util.Calendar.YEAR) != c2.get(java.util.Calendar.YEAR) ||
               c1.get(java.util.Calendar.DAY_OF_YEAR) != c2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun formatMessageDateHeader(timestamp: Long): String {
        if (timestamp <= 0L) return "Today"
        return try {
            val now = java.util.Calendar.getInstance()
            val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

            val isToday = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                          now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
            if (isToday) return "Today"

            val yesterday = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
                              yesterday.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)
            if (isYesterday) return "Yesterday"

            val daysDiff = (now.timeInMillis - timestamp) / (24 * 60 * 60 * 1000)
            if (daysDiff in 2..6) {
                return SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp))
            }

            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        } catch (_: Exception) {
            "Today"
        }
    }

    fun toUser(profile: UserProfile?, currentUid: String): User {
        return User(
            id = profile?.uid ?: currentUid,
            name = profile?.displayName?.takeIf { it.isNotBlank() } ?: profile?.username?.takeIf { it.isNotBlank() } ?: "Oscellion User",
            username = if (profile?.username?.isNotBlank() == true) "@${profile.username}" else "@user",
            avatar = profile?.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy),
            banner = profile?.bannerUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_midnight_glow),
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

        val timeStr = formatTime(msg.timestamp)

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

        val isPoll = msg.messageType.equals("poll", ignoreCase = true) || msg.text.startsWith("[POLL]:")
        val parsedPoll = if (isPoll) {
            try {
                val payload = msg.text.removePrefix("[POLL]:")
                val parts = payload.split("|||")
                val question = parts.getOrNull(0)?.trim() ?: "Poll"
                val optionsStr = parts.getOrNull(1) ?: ""
                val rawOptions = optionsStr.split(";;;").filter { it.isNotBlank() }
                val optionsList = rawOptions.mapIndexed { optIdx, optText ->
                    val votersForOpt = msg.pollVotes.entries.filter { entry ->
                        entry.value.split(",").contains(optIdx.toString())
                    }.map { entry ->
                        val prof = userProfiles[entry.key]
                        PollVoter(
                            id = entry.key,
                            name = prof?.displayName?.takeIf { it.isNotBlank() } ?: prof?.username?.takeIf { it.isNotBlank() } ?: (if (entry.key == currentUid) "You" else "User"),
                            avatar = prof?.avatarUrl ?: "",
                            isCurrentUser = entry.key == currentUid,
                            time = ""
                        )
                    }
                    val isUserVoted = votersForOpt.any { it.id == currentUid }
                    PollOption(
                        text = optText.trim(),
                        votes = votersForOpt.size,
                        isUserVoted = isUserVoted,
                        voters = votersForOpt
                    )
                }
                PollData(
                    id = msg.id,
                    question = question,
                    options = if (optionsList.isNotEmpty()) optionsList else listOf(PollOption(text = "Yes"), PollOption(text = "No")),
                    creatorName = senderName
                )
            } catch (_: Exception) {
                null
            }
        } else null

        val mappedMediaType = when {
            isPoll -> "poll"
            msg.messageType.equals("audio", ignoreCase = true) || msg.messageType.equals("voice", ignoreCase = true) -> "voice"
            msg.messageType.equals("image", ignoreCase = true) -> "image"
            msg.messageType.equals("video", ignoreCase = true) -> "video"
            msg.messageType.equals("call", ignoreCase = true) -> "call"
            msg.messageType.equals("file", ignoreCase = true) -> "file"
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
            text = when {
                isPoll -> null
                mappedMediaType == "voice" -> null
                mappedMediaType in listOf("image", "video", "file") && (msg.text == "🔒 Message encrypted" || msg.text.isBlank()) -> null
                else -> msg.text.takeIf { it.isNotBlank() }
            },
            mediaType = mappedMediaType,
            mediaUrl = msg.imageUrl ?: msg.mediaUrl,
            mediaUrls = listOfNotNull(msg.imageUrl ?: msg.mediaUrl),
            localUri = if (mappedMediaType == "voice") (msg.imageUrl ?: msg.mediaUrl) else null,
            fileName = if (mappedMediaType == "file") "Document" else null,
            fileSize = null,
            fileItems = emptyList(),
            voiceDuration = msg.durationSeconds ?: 4,
            waveformSamples = if (mappedMediaType == "voice") DEFAULT_WAVEFORM_SAMPLES else emptyList(),
            transcription = null,
            isTranscribing = false,
            pollData = parsedPoll,
            locationTitle = null,
            callDuration = null,
            callType = null,
            time = timeStr,
            timestamp = msg.timestamp,
            status = statusStr,
            reactions = reactionCounts,
            userReaction = myReaction,
            replyTo = replyToQuote,
            isEdited = msg.isEdited
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
            room.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy)
        } else {
            otherProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: room.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy)
        }

        val banner = otherProfile?.bannerUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_midnight_glow)

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
                    avatar = memberProf?.avatarUrl?.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy)
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
            val previewText = if (room.lastMessage.startsWith("[POLL]:")) {
                "Poll: " + room.lastMessage.removePrefix("[POLL]:").substringBefore("|||").trim()
            } else {
                room.lastMessage.ifBlank { null }
            }
            listOf(
                ChatMessage(
                    id = "last_${room.id}",
                    senderId = room.lastSender ?: "",
                    senderName = senderName,
                    text = previewText,
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
            val avatar = prof.avatarUrl.takeIf { it.isNotBlank() } ?: drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy)

            for (s in group.stories) {
                result.add(
                    Story(
                        id = s.id,
                        authorId = group.userId,
                        authorName = authorName,
                        avatar = avatar,
                        timeAgo = formatStoryTime(s.timestamp),
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


