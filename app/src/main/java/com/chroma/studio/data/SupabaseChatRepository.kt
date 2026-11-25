package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import io.github.jan.supabase.postgrest.query.Order

/**
 * Advanced Chat Repository using Supabase for PostgreSQL Messages & Firebase for Auth/Profiles.
 */
class SupabaseChatRepository {
    private val client = SupabaseClient.client
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // We still use Firebase for profiles and room metadata temporarily, or move it all to Supabase.
    // For this hybrid approach:
    // Firebase: ChatRoom metadata (last message, unread count), Users
    // Supabase: Messages table (for heavy writing and reading with real-time sockets)

    private val chatsCollection = firestore.collection("chats")
    private val usersCollection = firestore.collection("users")
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // High-performance in-memory cache to eliminate repeated Firestore round-trips when sending messages
    private val roomParticipantsCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val roomGroupNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val roomIsGroupCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val userPublicKeyCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val decryptedMessageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val roomDeletedAtCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    @Volatile
    private var syncedPublicKeyUserId: String? = null

    fun clearCaches() {
        roomParticipantsCache.clear()
        roomGroupNameCache.clear()
        roomIsGroupCache.clear()
        userPublicKeyCache.clear()
        decryptedMessageCache.clear()
        roomDeletedAtCache.clear()
        syncedPublicKeyUserId = null
        NotificationDispatcher.clearPushCaches()
    }

    /** Pre-warms room participants, RSA public keys, FCM token & push metadata in parallel when opening a room. */
    suspend fun prefetchRoomMetadata(roomId: String, knownParticipants: List<String>? = null) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val participants = knownParticipants?.takeIf { it.isNotEmpty() }
                ?: roomParticipantsCache[roomId]
                ?: run {
                    val snap = chatsCollection.document(roomId).get().await()
                    val room = snap.toObject(ChatRoom::class.java)
                    if (room != null) {
                        roomParticipantsCache[roomId] = room.participants
                        roomIsGroupCache[roomId] = room.isGroup
                        if (!room.name.isNullOrBlank()) roomGroupNameCache[roomId] = room.name
                        room.participants
                    } else emptyList()
                }
            if (participants.isNotEmpty()) {
                roomParticipantsCache[roomId] = participants
            }

            // Ensure current user's publicKey is synced at most once per session
            if (syncedPublicKeyUserId != uid) {
                val myPubKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                if (myPubKey.isNotEmpty()) {
                    syncedPublicKeyUserId = uid
                    usersCollection.document(uid).update("publicKey", myPubKey)
                }
            }

            val others = participants.filter { it != uid }
            coroutineScope {
                val pushPrep = async { NotificationDispatcher.preloadChatPushContext(others, roomId) }
                val fcmWarmup = async { FcmPushSender.warmup(com.chroma.studio.ChromaApp.instance) }
                val keyJobs = others.filter { userPublicKeyCache[it].isNullOrEmpty() }.map { pid ->
                    async {
                        try {
                            val fetchedKey = usersCollection.document(pid).get().await().getString("publicKey")
                            if (!fetchedKey.isNullOrEmpty()) userPublicKeyCache[pid] = fetchedKey
                        } catch (_: Exception) {}
                    }
                }
                pushPrep.await()
                fcmWarmup.await()
                keyJobs.awaitAll()
            }
        } catch (_: Exception) {}
    }

    private fun getChatRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    suspend fun getOrCreateChatRoom(otherUserId: String): ChatRoom? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext null
        val roomId = getChatRoomId(currentUser.uid, otherUserId)

        try {
            val doc = chatsCollection.document(roomId).get().await()
            if (doc.exists()) {
                val room = doc.toObject(ChatRoom::class.java)
                // If the current user previously deleted this chat, un-delete so it reappears
                // but keep the deletedAt timestamp so old messages remain hidden
                if (room != null && room.deletedBy.contains(currentUser.uid)) {
                    chatsCollection.document(roomId).update(
                        "deletedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUser.uid)
                    ).await()
                    val res = room.copy(
                        deletedBy = room.deletedBy - currentUser.uid
                    )
                    roomParticipantsCache[res.id] = res.participants
                    return@withContext res
                }
                if (room != null) {
                    roomParticipantsCache[room.id] = room.participants
                }
                room
            } else {
                val newRoom = ChatRoom(
                    id = roomId,
                    participants = listOf(currentUser.uid, otherUserId),
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = mapOf(currentUser.uid to 0, otherUserId to 0)
                )
                chatsCollection.document(roomId).set(newRoom).await()
                roomParticipantsCache[newRoom.id] = newRoom.participants
                newRoom
            }
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error getting/creating chat room", e)
            null
        }
    }

    suspend fun toggleFavorite(roomId: String, userId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        try {
            val update = if (isFavorite) {
                FieldValue.arrayUnion(userId)
            } else {
                FieldValue.arrayRemove(userId)
            }
            chatsCollection.document(roomId).update("favoritedBy", update).await()
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error toggling favorite status", e)
        }
    }

    suspend fun createGroup(
        groupName: String,
        groupDescription: String? = null,
        participantIds: List<String>,
        imageUrl: String? = null,
        preGeneratedId: String? = null,
        onlyAdminsCanPost: Boolean = false
    ): ChatRoom? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext null
        val roomId = preGeneratedId ?: "group_${java.util.UUID.randomUUID()}"

        try {
            val allUsers = participantIds.toMutableList()
            if (!allUsers.contains(currentUser.uid)) allUsers.add(currentUser.uid)

            val unreadMap = allUsers.associateWith { 0 }

            val newRoom = ChatRoom(
                id = roomId,
                name = groupName,
                groupDescription = groupDescription,
                isGroup = true,
                avatarUrl = imageUrl,
                participants = allUsers,
                admins = listOf(currentUser.uid),
                createdBy = currentUser.uid,
                lastMessage = "Group created",
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = unreadMap,
                onlyAdminsCanPost = onlyAdminsCanPost
            )
            chatsCollection.document(roomId).set(newRoom).await()
            roomParticipantsCache[newRoom.id] = newRoom.participants
            sendMessage(roomId, "Group created", messageType = "system")
            newRoom
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error creating group", e)
            null
        }
    }

    private suspend fun encryptPayload(
        roomId: String,
        text: String,
        currentUserId: String
    ): Triple<String, String, Map<String, String>> {
        if (text.isBlank()) return Triple(text, "", emptyMap())
        return try {
            val aesKey = com.chroma.studio.utils.CryptoHelper.generateAESKey()
            val aesBytes = aesKey.encoded
            val (encryptedMsg, iv) = com.chroma.studio.utils.CryptoHelper.encryptMessageWithAES(text, aesKey)

            val encryptedKeysMap = mutableMapOf<String, String>()
            val myPubKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()

            // Always encrypt for current user with their active local public key; only sync once per session
            if (myPubKey.isNotEmpty()) {
                val myEncKey = com.chroma.studio.utils.CryptoHelper.encryptAESKeyWithRSA(aesBytes, myPubKey)
                encryptedKeysMap[currentUserId] = myEncKey
                if (syncedPublicKeyUserId != currentUserId) {
                    syncedPublicKeyUserId = currentUserId
                    usersCollection.document(currentUserId).update("publicKey", myPubKey)
                }
            }

            // Room fallback encryption
            val roomKey = com.chroma.studio.utils.CryptoHelper.deriveRoomKey(roomId)
            val (roomEncAes, roomAesIv) = com.chroma.studio.utils.CryptoHelper.encryptMessageWithAES(
                android.util.Base64.encodeToString(aesBytes, android.util.Base64.NO_WRAP),
                javax.crypto.spec.SecretKeySpec(roomKey, "AES")
            )
            encryptedKeysMap["_room_fallback"] = "$roomEncAes:$roomAesIv"

            // Retrieve room participants using high-speed in-memory cache
            val participants = roomParticipantsCache[roomId] ?: run {
                val roomSnapshot = chatsCollection.document(roomId).get().await()
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null) {
                    roomParticipantsCache[roomId] = room.participants
                    roomIsGroupCache[roomId] = room.isGroup
                    if (!room.name.isNullOrBlank()) roomGroupNameCache[roomId] = room.name
                    room.participants
                } else emptyList()
            }

            val others = participants.filter { it != currentUserId }
            val missingKeys = others.filter { userPublicKeyCache[it].isNullOrEmpty() }
            if (missingKeys.isNotEmpty()) {
                // Parallel fetch with short timeout so missing keys never stall message sending
                withTimeoutOrNull(1500L) {
                    coroutineScope {
                        missingKeys.map { participantId ->
                            async {
                                try {
                                    val fetchedKey = usersCollection.document(participantId).get().await().getString("publicKey")
                                    if (!fetchedKey.isNullOrEmpty()) {
                                        userPublicKeyCache[participantId] = fetchedKey
                                    }
                                } catch (_: Exception) {}
                            }
                        }.awaitAll()
                    }
                }
            }

            for (participantId in others) {
                val pubKeyStr = userPublicKeyCache[participantId]
                if (!pubKeyStr.isNullOrEmpty()) {
                    try {
                        val encKey = com.chroma.studio.utils.CryptoHelper.encryptAESKeyWithRSA(aesBytes, pubKeyStr)
                        encryptedKeysMap[participantId] = encKey
                    } catch (_: Exception) {}
                }
            }
            Triple(encryptedMsg, iv, encryptedKeysMap)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseChatRepository", "Encryption failed", e)
            Triple(text, "", emptyMap())
        }
    }

    suspend fun sendMessage(
        roomId: String,
        text: String,
        imageUrl: String? = null,
        workId: String? = null,
        preGeneratedId: String? = null,
        messageType: String = "text",
        durationSeconds: Int? = null,
        replyToMessageId: String? = null,
        storyId: String? = null,
        storyThumbnailUrl: String? = null,
        flashVideoId: String? = null,
        flashVideoThumbnailUrl: String? = null,
        flashVideoUrl: String? = null,
        flashVideoCaption: String? = null,
        flashVideoAuthorUsername: String? = null,
        flashVideoAuthorAvatarUrl: String? = null,
        flashVideoDurationMs: Long? = null,
        isForwarded: Boolean = false,
        callDuration: String? = null,
        callType: String? = null,
        mediaType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val messageId = preGeneratedId ?: java.util.UUID.randomUUID().toString()
            val minTimestamp = (roomDeletedAtCache[roomId] ?: 0L) + 1L
            val timestamp = maxOf(System.currentTimeMillis(), minTimestamp)
            val (finalMessageText, finalIv, finalEncryptedKeys) = if (messageType != "system" && messageType != "flash_video" && messageType != "call" && text.isNotBlank()) {
                encryptPayload(roomId, text, currentUser.uid)
            } else {
                Triple(text, "", emptyMap<String, String>())
            }

            // Pre-populate decryption cache so sender's observeMessages skips KeyStore RSA decryption on this message
            if (finalIv.isNotEmpty() && finalMessageText.isNotEmpty()) {
                decryptedMessageCache["${messageId}_${finalMessageText.hashCode()}_$finalIv"] = text
            }

            val message = ChatMessage(
                id = messageId,
                senderId = currentUser.uid,
                text = finalMessageText,
                timestamp = timestamp,
                isRead = false,
                imageUrl = imageUrl,
                mediaType = mediaType ?: if (messageType == "call") "call" else null,
                workId = workId,
                messageType = messageType,
                iv = finalIv,
                encryptedAesKeys = finalEncryptedKeys,
                durationSeconds = durationSeconds,
                replyToMessageId = replyToMessageId,
                storyId = storyId,
                storyThumbnailUrl = storyThumbnailUrl,
                flashVideoId = flashVideoId,
                flashVideoThumbnailUrl = flashVideoThumbnailUrl,
                flashVideoUrl = flashVideoUrl,
                flashVideoCaption = flashVideoCaption,
                flashVideoAuthorUsername = flashVideoAuthorUsername,
                flashVideoAuthorAvatarUrl = flashVideoAuthorAvatarUrl,
                flashVideoDurationMs = flashVideoDurationMs,
                isForwarded = isForwarded,
                callDuration = callDuration,
                callType = callType
            )

            val roomRef = chatsCollection.document(roomId)
            val msgRef = roomRef.collection("messages").document(message.id)

            // Resolve participants from cache first (0 extra Firestore reads!)
            val cachedParticipants = roomParticipantsCache[roomId]
            val participants: List<String> = cachedParticipants ?: run {
                val roomSnap = try { roomRef.get().await() } catch (_: Exception) { null }
                val room = roomSnap?.toObject(ChatRoom::class.java)
                if (room != null) {
                    roomParticipantsCache[roomId] = room.participants
                    roomIsGroupCache[roomId] = room.isGroup
                    if (!room.name.isNullOrBlank()) roomGroupNameCache[roomId] = room.name
                    room.participants
                } else emptyList()
            }

            val displayMessage = if (messageType == "call") {
                val type = callType ?: "Audio"
                val dur = if (!callDuration.isNullOrBlank()) " • $callDuration" else ""
                "$type call$dur"
            } else if (messageType == "flash_video" || flashVideoId != null) {
                "⚡ Shared a Flash video"
            } else if (storyThumbnailUrl != null || storyId != null) {
                "Replied to story: $text"
            } else if (imageUrl != null) {
                if (messageType == "video") "Sent a video" else if (messageType == "audio") "Voice message" else "Sent an image"
            } else if (workId != null) {
                "Shared a work"
            } else if (text.startsWith("[POLL]:")) {
                val q = text.removePrefix("[POLL]:").substringBefore("|||").trim()
                "Poll: $q"
            } else {
                text
            }

            // 1 single atomic WriteBatch: creates message doc AND updates room metadata + unread counts in 1 RTT (zero transaction locks/retries!)
            val roomUpdates = mutableMapOf<String, Any?>(
                "lastMessage" to displayMessage,
                "lastMessageTime" to message.timestamp,
                "lastSender" to currentUser.uid,
                "lastMessageId" to message.id,
                "lastMessageIsRead" to false,
                "lastMessageIsDelivered" to false,
                "deletedBy" to emptyList<String>()
            )
            for (participantId in participants) {
                if (participantId != currentUser.uid) {
                    roomUpdates["unreadCount.$participantId"] = FieldValue.increment(1L)
                }
            }

            try {
                val batch = firestore.batch()
                batch.set(msgRef, message)
                if (participants.isNotEmpty()) {
                    batch.update(roomRef, roomUpdates)
                }
                batch.commit().await()
            } catch (batchErr: Exception) {
                // Fallback if parent room doc doesn't exist yet on first-ever race
                msgRef.set(message).await()
                try {
                    roomRef.update(roomUpdates).await()
                } catch (_: Exception) {}
            }

            // 2. Dispatch FCM push asynchronously in parallel without blocking message send completion
            val isGroup = roomIsGroupCache[roomId] ?: roomId.startsWith("group_")
            val groupName = if (isGroup) roomGroupNameCache[roomId] else null
            val recipients = participants.filter { it != currentUser.uid }.ifEmpty {
                if (!roomId.startsWith("group_") && roomId.contains("_")) {
                    roomId.split("_").filter { it.isNotBlank() && it != currentUser.uid }
                } else emptyList()
            }
            if (recipients.isNotEmpty()) {
                val displaySnippet = when {
                    messageType == "flash_video" || flashVideoId != null -> "⚡ Shared a Flash video"
                    messageType == "audio" || messageType == "voice" -> "🎤 Voice message"
                    messageType == "video" -> "🎥 Sent a video"
                    messageType == "image" || (imageUrl != null && messageType == "text") -> "📷 Sent a photo"
                    imageUrl != null && messageType == "audio" -> "🎤 Voice message"
                    imageUrl != null -> "📎 Sent a file"
                    workId != null -> "🎨 Shared a work"
                    storyId != null || storyThumbnailUrl != null -> "↩️ Replied to your story"
                    text.startsWith("[POLL]:") -> {
                        val question = text.removePrefix("[POLL]:").substringBefore("|||").trim()
                        "📊 Poll: $question"
                    }
                    text.isNotBlank() -> text.take(200)
                    else -> "New message"
                }
                pushScope.launch {
                    try {
                        NotificationDispatcher.sendChatPush(
                            recipientIds = recipients,
                            roomId = roomId,
                            groupName = groupName,
                            body = displaySnippet,
                            messageType = messageType,
                            messageId = messageId
                        )
                    } catch (_: Exception) {}
                }
            }

            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error sending message", e)
            false
        }
    }

    suspend fun sendFlashVideoMessage(
        roomId: String,
        flashVideo: com.chroma.studio.model.FlashVideo,
        captionText: String? = null
    ): Boolean {
        return sendMessage(
            roomId = roomId,
            text = captionText ?: flashVideo.caption,
            messageType = "flash_video",
            imageUrl = flashVideo.thumbnailUrl,
            flashVideoId = flashVideo.id,
            flashVideoThumbnailUrl = flashVideo.thumbnailUrl,
            flashVideoUrl = flashVideo.videoUrl,
            flashVideoCaption = flashVideo.caption,
            flashVideoAuthorUsername = flashVideo.authorUsername,
            flashVideoAuthorAvatarUrl = flashVideo.authorAvatarUrl,
            flashVideoDurationMs = flashVideo.duration
        )
    }

    suspend fun markAsRead(roomId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)

            // Fetch up to 30 recent messages first so we can commit room + message updates in 1 atomic WriteBatch (zero transaction locks!)
            val recentDocs = try {
                roomRef.collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(30)
                    .get()
                    .await()
                    .documents
            } catch (_: Exception) {
                emptyList()
            }

            val docsToUpdate = recentDocs.filter { doc ->
                val senderId = doc.getString("senderId")
                val isRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: false
                !isRead && senderId != null && senderId != currentUser.uid
            }

            val batch = firestore.batch()
            val roomUpdates = mutableMapOf<String, Any>(
                "unreadCount.${currentUser.uid}" to 0
            )
            // If latest message came from someone else, also mark room-level read/delivered flags
            val latestDoc = recentDocs.firstOrNull()
            val latestSender = latestDoc?.getString("senderId")
            if (latestSender == null || latestSender != currentUser.uid) {
                roomUpdates["lastMessageIsRead"] = true
                roomUpdates["lastMessageIsDelivered"] = true
            }
            batch.update(roomRef, roomUpdates)

            for (doc in docsToUpdate) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "isRead" to true,
                        "read" to true,
                        "isDelivered" to true,
                        "delivered" to true
                    )
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error marking as read", e)
        }
    }

    suspend fun markAsDelivered(roomId: String, targetMessageId: String? = null) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)

            // Fast 0-read WriteBatch path when triggered directly from an FCM push carrying messageId
            if (!targetMessageId.isNullOrBlank()) {
                try {
                    val batch = firestore.batch()
                    batch.update(roomRef, "lastMessageIsDelivered", true)
                    batch.update(
                        roomRef.collection("messages").document(targetMessageId),
                        mapOf("isDelivered" to true, "delivered" to true)
                    )
                    batch.commit().await()
                    return@withContext
                } catch (_: Exception) {
                    // Fall through to query-based fallback if target doc is missing/locked
                }
            }

            val recentMessages = try {
                roomRef.collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()
                    .documents
            } catch (_: Exception) {
                emptyList()
            }

            val docsToUpdate = recentMessages.filter { doc ->
                val senderId = doc.getString("senderId")
                val isDelivered = doc.getBoolean("isDelivered") ?: doc.getBoolean("delivered") ?: false
                !isDelivered && senderId != null && senderId != currentUser.uid
            }

            val batch = firestore.batch()
            val latestSender = recentMessages.firstOrNull()?.getString("senderId")
            var hasWrites = false
            if (latestSender == null || latestSender != currentUser.uid) {
                batch.update(roomRef, "lastMessageIsDelivered", true)
                hasWrites = true
            }
            for (doc in docsToUpdate) {
                batch.update(
                    doc.reference,
                    mapOf("isDelivered" to true, "delivered" to true)
                )
                hasWrites = true
            }
            if (hasWrites) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error marking as delivered", e)
        }
    }

    fun observeRecentChats(targetUid: String? = null): Flow<List<ChatRoom>> = callbackFlow {
        val uid = targetUid ?: auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = chatsCollection
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SupabaseChatRepository", "Listen failed for chats (offline/error): ${e.message}")
                    val cached = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(
                        com.chroma.studio.ChromaApp.instance,
                        uid
                    )
                    if (cached.isNotEmpty()) {
                        trySend(cached)
                    }
                    return@addSnapshotListener
                }

                val rawChats = snapshot?.documents?.mapNotNull { it.toObject(ChatRoom::class.java) }
                    ?.filter { uid in it.participants }
                    ?: emptyList()
                for (chat in rawChats) {
                    roomParticipantsCache[chat.id] = chat.participants
                    roomIsGroupCache[chat.id] = chat.isGroup
                    if (!chat.name.isNullOrBlank()) roomGroupNameCache[chat.id] = chat.name
                    val dAt = chat.deletedAt[uid] ?: 0L
                    roomDeletedAtCache[chat.id] = dAt
                }
                val activeChats = rawChats.filter { !it.deletedBy.contains(uid) && !it.archivedBy.contains(uid) }
                trySend(activeChats.sortedByDescending { it.lastMessageTime })
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun observeMessages(roomId: String, limit: Long = 50, targetUid: String? = null): Flow<List<ChatMessage>> = callbackFlow {
        val currentUid = targetUid ?: auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val deletedAt = roomDeletedAtCache[roomId] ?: 0L
        val snapshotSeqGen = java.util.concurrent.atomic.AtomicLong(0L)
        val latestEmittedSeq = java.util.concurrent.atomic.AtomicLong(0L)

        var query = chatsCollection.document(roomId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

        if (deletedAt > 0L) {
            query = query.whereGreaterThan("timestamp", deletedAt)
        }

        val firestoreListener = query.limit(limit)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SupabaseChatRepository", "Listen failed for messages in room $roomId (offline/error): ${e.message}")
                    val cached = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedMessages(
                        com.chroma.studio.ChromaApp.instance,
                        roomId,
                        currentUid
                    )
                    if (cached.isNotEmpty()) {
                        trySend(cached)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val seq = snapshotSeqGen.incrementAndGet()
                    launch(Dispatchers.Default) {
                        val activeUid = targetUid ?: auth.currentUser?.uid
                        val messages = coroutineScope {
                            snapshot.documents.map { doc ->
                                async {
                                    val rawMsg = doc.toObject(ChatMessage::class.java) ?: return@async null
                                    val docIsRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: rawMsg.isRead
                                    val docIsDelivered = doc.getBoolean("isDelivered") ?: doc.getBoolean("delivered") ?: rawMsg.isDelivered
                                    val msg = rawMsg.copy(
                                        id = if (rawMsg.id.isBlank() || rawMsg.id != doc.id) doc.id else rawMsg.id,
                                        isRead = docIsRead,
                                        isDelivered = docIsDelivered || docIsRead
                                    )

                                    // Filter out messages deleted by current user for themselves
                                    if (activeUid != null && msg.deletedFor.contains(activeUid)) {
                                        return@async null
                                    }

                                    if (msg.messageType == "system" || msg.iv.isEmpty() || msg.text.isEmpty()) {
                                        return@async msg
                                    }

                                    val cacheKey = "${msg.id}_${msg.text.hashCode()}_${msg.iv}"
                                    val cached = decryptedMessageCache[cacheKey]
                                    if (cached != null) {
                                        return@async msg.copy(text = cached)
                                    }

                                    var decryptedText = msg.text
                                    var isDecrypted = false

                                    // 1. Try ultra-fast room key fallback first if available (pure CPU AES ~0.05ms vs AndroidKeyStore RSA ~30ms), then user-specific RSA
                                    try {
                                        val roomKey = com.chroma.studio.utils.CryptoHelper.deriveRoomKey(roomId)
                                        val fallbackEntry = msg.encryptedAesKeys["_room_fallback"]
                                        if (!fallbackEntry.isNullOrEmpty() && fallbackEntry.contains(":")) {
                                            val parts = fallbackEntry.split(":", limit = 2)
                                            val aesBase64 = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(parts[0], parts[1], roomKey)
                                            if (aesBase64.isNotEmpty()) {
                                                val aesBytes = android.util.Base64.decode(aesBase64, android.util.Base64.NO_WRAP)
                                                val res = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, aesBytes)
                                                if (res.isNotEmpty() || msg.text.isEmpty()) {
                                                    decryptedText = res
                                                    isDecrypted = true
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}

                                    // 2. Fallback to user-specific RSA decryption if room fallback wasn't present
                                    if (!isDecrypted && currentUid != null && msg.encryptedAesKeys.containsKey(currentUid)) {
                                        try {
                                            val myEncryptedAes = msg.encryptedAesKeys[currentUid]
                                            if (!myEncryptedAes.isNullOrEmpty()) {
                                                val aesBytes = com.chroma.studio.utils.CryptoHelper.decryptAESKeyWithRSA(myEncryptedAes)
                                                if (aesBytes != null) {
                                                    val res = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, aesBytes)
                                                    if (res.isNotEmpty() || msg.text.isEmpty()) {
                                                        decryptedText = res
                                                        isDecrypted = true
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    if (!isDecrypted) {
                                        try {
                                            val roomKey = com.chroma.studio.utils.CryptoHelper.deriveRoomKey(roomId)
                                            val direct = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, roomKey)
                                            if (direct.isNotEmpty()) {
                                                decryptedText = direct
                                                isDecrypted = true
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    if (!isDecrypted) {
                                        val isMedia = msg.imageUrl != null || msg.mediaUrl != null || msg.messageType in listOf("image", "video", "voice", "audio", "file")
                                        decryptedText = if (isMedia) {
                                            ""
                                        } else if (msg.text.isNotEmpty() && !msg.text.contains(" ") && msg.text.length > 20) {
                                            "🔒 Message encrypted"
                                        } else {
                                            msg.text
                                        }
                                    }

                                    decryptedMessageCache[cacheKey] = decryptedText
                                    msg.copy(text = decryptedText)
                                }
                            }.awaitAll().filterNotNull()
                        }
                        if (seq >= latestEmittedSeq.getAndUpdate { prev -> maxOf(prev, seq) }) {
                            trySend(messages)
                        }
                    }
                }
            }
        awaitClose { firestoreListener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun updateGroupName(roomId: String, name: String) = withContext(Dispatchers.IO) {
        try {
            chatsCollection.document(roomId).update("name", name).await()
            val currentUser = auth.currentUser
            val userName = currentUser?.let {
                usersCollection.document(it.uid).get().await().getString("displayName")
            } ?: "Someone"
            sendMessage(roomId, "$userName changed the group name to \"$name\"", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating group name", e)
        }
    }

    suspend fun updateGroupImage(roomId: String, imageUrl: String) = withContext(Dispatchers.IO) {
        try {
            chatsCollection.document(roomId).update("avatarUrl", imageUrl).await()
            val currentUser = auth.currentUser
            val userName = currentUser?.let {
                usersCollection.document(it.uid).get().await().getString("displayName")
            } ?: "Someone"
            sendMessage(roomId, "$userName changed the group image", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating group image", e)
        }
    }

    suspend fun updateGroupDescription(roomId: String, description: String) = withContext(Dispatchers.IO) {
        try {
            chatsCollection.document(roomId).update("groupDescription", description).await()
            val currentUser = auth.currentUser
            val userName = currentUser?.let {
                usersCollection.document(it.uid).get().await().getString("displayName")
            } ?: "Someone"
            sendMessage(roomId, "$userName changed the group description", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating group description", e)
        }
    }

    suspend fun demoteAdmin(roomId: String, userId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var demotedName = "Someone"

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can change admin permissions")
                    }
                    val newAdmins = room.admins.filter { it != userId }
                    if (newAdmins.isEmpty() && room.participants.isNotEmpty()) {
                        throw Exception("Cannot demote the sole group admin")
                    }
                    transaction.update(roomRef, "admins", newAdmins)
                }
            }.await()

            demotedName = try {
                usersCollection.document(userId).get().await().getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: usersCollection.document(userId).get().await().getString("username")?.takeIf { it.isNotBlank() }
                    ?: "Someone"
            } catch (_: Exception) { "Someone" }

            sendMessage(roomId, "$demotedName is no longer an admin", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error demoting admin", e)
        }
    }

    suspend fun addParticipants(roomId: String, userIds: List<String>) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        if (userIds.isEmpty()) return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var updatedParticipants = emptyList<String>()

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can add participants")
                    }
                    val newParticipants = (room.participants + userIds).distinct()
                    updatedParticipants = newParticipants
                    val updatedUnread = room.unreadCount.toMutableMap()
                    userIds.forEach { uid ->
                        if (!updatedUnread.containsKey(uid)) {
                            updatedUnread[uid] = 0
                        }
                    }

                    transaction.update(roomRef, "participants", newParticipants)
                    transaction.update(roomRef, "unreadCount", updatedUnread)
                }
            }.await()

            if (updatedParticipants.isNotEmpty()) {
                roomParticipantsCache[roomId] = updatedParticipants
            }

            // Construct system message
            val names = userIds.map { uid ->
                try {
                    usersCollection.document(uid).get().await().getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: usersCollection.document(uid).get().await().getString("username")?.takeIf { it.isNotBlank() }
                        ?: "Someone"
                } catch (_: Exception) { "Someone" }
            }
            val systemMsg = if (names.size == 1) {
                "${names[0]} was added to the group"
            } else {
                "${names.joinToString(", ")} were added to the group"
            }
            sendMessage(roomId, systemMsg, messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error adding participants", e)
        }
    }

    suspend fun removeParticipant(roomId: String, userId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var removedName = "Someone"
            var wasRemoved = false
            var updatedParticipants = emptyList<String>()

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can remove participants")
                    }
                    val newParticipants = room.participants.filter { it != userId }
                    updatedParticipants = newParticipants
                    val newAdmins = room.admins.filter { it != userId }
                    val finalAdmins = if (newAdmins.isEmpty() && newParticipants.isNotEmpty()) {
                        listOf(currentUser.uid.takeIf { newParticipants.contains(it) } ?: newParticipants.first())
                    } else {
                        newAdmins
                    }
                    val updatedUnread = room.unreadCount.toMutableMap().apply { remove(userId) }

                    transaction.update(roomRef, "participants", newParticipants)
                    transaction.update(roomRef, "admins", finalAdmins)
                    transaction.update(roomRef, "unreadCount", updatedUnread)
                    wasRemoved = true
                }
            }.await()

            if (wasRemoved) {
                roomParticipantsCache[roomId] = updatedParticipants
                removedName = try {
                    usersCollection.document(userId).get().await().getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: usersCollection.document(userId).get().await().getString("username")?.takeIf { it.isNotBlank() }
                        ?: "Someone"
                } catch (_: Exception) { "Someone" }
                sendMessage(roomId, "$removedName was removed from the group", messageType = "system")
            }
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error removing participant", e)
        }
    }

    suspend fun makeAdmin(roomId: String, userId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var adminName = "Someone"

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can make other users admin")
                    }
                    if (!room.participants.contains(userId)) {
                        throw Exception("User is not a participant in this group")
                    }
                    val newAdmins = (room.admins + userId).distinct()
                    transaction.update(roomRef, "admins", newAdmins)
                }
            }.await()

            adminName = try {
                usersCollection.document(userId).get().await().getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: usersCollection.document(userId).get().await().getString("username")?.takeIf { it.isNotBlank() }
                    ?: "Someone"
            } catch (_: Exception) { "Someone" }
            sendMessage(roomId, "$adminName is now an admin", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error making admin", e)
        }
    }

    suspend fun leaveGroup(roomId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            val leftName = try {
                val uDoc = usersCollection.document(currentUser.uid).get().await()
                uDoc.getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: uDoc.getString("username")?.takeIf { it.isNotBlank() }
                    ?: "Someone"
            } catch (_: Exception) { "Someone" }

            // Send system "left the group" message BEFORE removing self from participants (required by Firestore rules)
            try {
                sendMessage(roomId, "$leftName left the group", messageType = "system")
            } catch (ex: Exception) {
                Log.w("SupabaseChatRepository", "Could not send leave system message", ex)
            }

            var updatedParticipants = emptyList<String>()

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    val newParticipants = room.participants.filter { it != currentUser.uid }
                    updatedParticipants = newParticipants
                    val newAdmins = room.admins.filter { it != currentUser.uid }
                    val finalAdmins = if (newAdmins.isEmpty() && newParticipants.isNotEmpty()) {
                        listOf(newParticipants.first())
                    } else {
                        newAdmins
                    }
                    val updatedUnread = room.unreadCount.toMutableMap().apply { remove(currentUser.uid) }

                    transaction.update(roomRef, "participants", newParticipants)
                    transaction.update(roomRef, "admins", finalAdmins)
                    transaction.update(roomRef, "unreadCount", updatedUnread)
                }
            }.await()

            roomParticipantsCache[roomId] = updatedParticipants
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error leaving group", e)
        }
    }

    suspend fun updateOnlyAdminsCanPost(roomId: String, onlyAdminsCanPost: Boolean) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var adminName = "Admin"
            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can modify group posting permissions")
                    }
                    transaction.update(roomRef, "onlyAdminsCanPost", onlyAdminsCanPost)
                }
            }.await()

            adminName = try {
                usersCollection.document(currentUser.uid).get().await().getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: usersCollection.document(currentUser.uid).get().await().getString("username")?.takeIf { it.isNotBlank() }
                    ?: "Admin"
            } catch (_: Exception) { "Admin" }

            val notice = if (onlyAdminsCanPost) {
                "$adminName changed group settings: Only admins can send messages"
            } else {
                "$adminName changed group settings: All members can send messages"
            }
            sendMessage(roomId, notice, messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating onlyAdminsCanPost", e)
        }
    }

    suspend fun getRoom(roomId: String): ChatRoom? = withContext(Dispatchers.IO) {
        try {
            val doc = chatsCollection.document(roomId).get().await()
            if (doc.exists()) {
                val room = doc.toObject(ChatRoom::class.java)
                if (room != null) {
                    roomParticipantsCache[room.id] = room.participants
                }
                room
            } else null
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error getting room $roomId", e)
            null
        }
    }

    suspend fun setTypingStatus(roomId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        setActivityStatus(roomId, if (isTyping) "typing" else null)
    }

    suspend fun setRecordingStatus(roomId: String, isRecording: Boolean) = withContext(Dispatchers.IO) {
        setActivityStatus(roomId, if (isRecording) "recording" else null)
    }

    suspend fun setActivityStatus(roomId: String, activity: String?) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        val normalizedRoomId = OnlineStatusManager.normalizeRoomId(roomId, currentUser.uid)
        OnlineStatusManager.setChatActivity(normalizedRoomId, activity)
    }

    // Helper for uploading chat media to Cloudinary CDN (reserving Supabase exclusively for avatars/banners)
    suspend fun uploadImageToSupabase(
        imageBytes: ByteArray,
        extension: String = "jpg",
        onProgress: ((Int) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            CloudinaryClient.uploadImage(imageBytes, folder = "chat_media", onProgress = onProgress)
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error uploading chat image to Cloudinary", e)
            null
        }
    }

    // Helper for uploading audio voice note to Cloudinary CDN
        suspend fun uploadVideoToSupabase(
            videoBytes: ByteArray,
            extension: String = "mp4",
            onProgress: ((Int) -> Unit)? = null
        ): String? = withContext(Dispatchers.IO) {
        try {
            CloudinaryClient.uploadVideo(videoBytes, folder = "chat_videos", onProgress = onProgress)
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error uploading video", e)
            null
        }
    }

suspend fun uploadAudioToSupabase(audioBytes: ByteArray, extension: String = "m4a"): String? = withContext(Dispatchers.IO) {
        try {
            CloudinaryClient.uploadAudio(audioBytes, extension = extension, folder = "chat_audio")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error uploading chat audio to Cloudinary", e)
            null
        }
    }

    suspend fun toggleReaction(messageId: String, roomId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            // 1. Update Firestore
            val msgRef = chatsCollection.document(roomId).collection("messages").document(messageId)
            firestore.runTransaction { tx ->
                val snap = tx.get(msgRef)
                if (snap.exists()) {
                    val msg = snap.toObject(ChatMessage::class.java)
                    if (msg != null) {
                        val updatedReactions = msg.reactions.toMutableMap()
                        if (updatedReactions[currentUser.uid] == emoji) {
                            updatedReactions.remove(currentUser.uid)
                        } else {
                            updatedReactions[currentUser.uid] = emoji
                        }
                        tx.update(msgRef, "reactions", updatedReactions)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error toggling reaction", e)
            false
        }
    }


    suspend fun addReaction(messageId: String, roomId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val msgRef = chatsCollection.document(roomId).collection("messages").document(messageId)
            firestore.runTransaction { tx ->
                val snap = tx.get(msgRef)
                if (snap.exists()) {
                    val msg = snap.toObject(ChatMessage::class.java)
                    if (msg != null) {
                        val updatedReactions = msg.reactions.toMutableMap()
                        if (updatedReactions[currentUser.uid] != emoji) {
                            updatedReactions[currentUser.uid] = emoji
                            tx.update(msgRef, "reactions", updatedReactions)
                        }
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error adding reaction", e)
            false
        }
    }

    suspend fun pinMessage(roomId: String, messageId: String?, messageText: String?): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val roomRef = chatsCollection.document(roomId)
            if (messageId != null && messageText != null) {
                roomRef.update(
                    mapOf(
                        "pinnedMessageId" to messageId,
                        "pinnedMessageText" to messageText,
                        "pinnedBy" to currentUser.uid,
                        "pinnedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                roomRef.update(
                    mapOf(
                        "pinnedMessageId" to null,
                        "pinnedMessageText" to null,
                        "pinnedBy" to null,
                        "pinnedAt" to 0L
                    )
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error pinning/unpinning message", e)
            false
        }
    }

    suspend fun blockUser(targetUserId: String, isBlocked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val userRef = usersCollection.document(currentUser.uid)
            val operation = if (isBlocked) {
                com.google.firebase.firestore.FieldValue.arrayUnion(targetUserId)
            } else {
                com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId)
            }
            userRef.update("blockedUsers", operation).await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating blocked status for $targetUserId", e)
            false
        }
    }

    suspend fun reportUser(
        reportedUserId: String?,
        roomId: String,
        reason: String,
        snippetMessages: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val reportDoc = firestore.collection("reports").document()
            val reportData = mapOf(
                "id" to reportDoc.id,
                "reporterId" to currentUser.uid,
                "reportedUserId" to (reportedUserId ?: ""),
                "roomId" to roomId,
                "reason" to reason,
                "snippetMessages" to snippetMessages,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            reportDoc.set(reportData).await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error submitting abuse report", e)
            false
        }
    }

    suspend fun loadBlockedUsers(): Set<String> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext emptySet()
        try {
            val doc = usersCollection.document(currentUser.uid).get().await()
            val list = doc.get("blockedUsers") as? List<*> ?: emptyList<Any>()
            list.mapNotNull { it?.toString() }.toSet()
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error loading blocked users", e)
            emptySet()
        }
    }

    suspend fun deleteMessage(messageId: String, roomId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from Firestore
            chatsCollection.document(roomId).collection("messages").document(messageId).delete().await()

            // Optimistically update room's lastMessage if the deleted message was the latest one
            try {
                val latestDocs = chatsCollection.document(roomId).collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                val latestMsgDoc = latestDocs.documents.firstOrNull()
                val (newLastMessage, newLastMessageTime) = if (latestMsgDoc != null) {
                    val msg = latestMsgDoc.toObject(ChatMessage::class.java)
                    val text = when {
                        msg == null -> ""
                        msg.messageType == "video" -> "Sent a video"
                        msg.messageType == "audio" || msg.messageType == "voice" -> "Voice message"
                        msg.messageType == "sticker" -> "Sent a sticker"
                        msg.messageType == "image" || msg.imageUrl != null -> "Sent an image"
                        msg.messageType == "poll" || msg.text.startsWith("[POLL]:") -> "📊 Poll"
                        msg.messageType == "file" -> "Sent a file"
                        msg.iv.isNotEmpty() -> {
                            val cacheKey = "${msg.id}_${msg.text.hashCode()}_${msg.iv}"
                            val cached = decryptedMessageCache[cacheKey]
                            if (cached != null) cached else {
                                var plain = ""
                                val uid = auth.currentUser?.uid
                                if (uid != null && msg.encryptedAesKeys.containsKey(uid)) {
                                    try {
                                        val aesBytes = com.chroma.studio.utils.CryptoHelper.decryptAESKeyWithRSA(msg.encryptedAesKeys[uid]!!)
                                        if (aesBytes != null) {
                                            plain = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, aesBytes)
                                        }
                                    } catch (_: Exception) {}
                                }
                                if (plain.isEmpty()) {
                                    try {
                                        val fb = msg.encryptedAesKeys["_room_fallback"]
                                        if (!fb.isNullOrEmpty() && fb.contains(":")) {
                                            val parts = fb.split(":", limit = 2)
                                            val rk = com.chroma.studio.utils.CryptoHelper.deriveRoomKey(roomId)
                                            val aesB64 = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(parts[0], parts[1], rk)
                                            if (aesB64.isNotEmpty()) {
                                                val aesBytes = android.util.Base64.decode(aesB64, android.util.Base64.NO_WRAP)
                                                plain = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, aesBytes)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                                plain.ifEmpty { "Message" }
                            }
                        }
                        else -> msg.text
                    }
                    Pair(text, msg?.timestamp ?: System.currentTimeMillis())
                } else {
                    Pair("", 0L)
                }

                chatsCollection.document(roomId).update(
                    mapOf(
                        "lastMessage" to newLastMessage,
                        "lastMessageTime" to newLastMessageTime
                    )
                ).await()
            } catch (ex: Exception) {
                Log.w("SupabaseChatRepository", "Could not refresh lastMessage after delete", ex)
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error deleting message", e)
            false
        }
    }

    suspend fun editMessage(messageId: String, roomId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val (finalText, finalIv, finalEncryptedKeys) = encryptPayload(roomId, newText, currentUser.uid)
            chatsCollection.document(roomId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "text" to finalText,
                        "iv" to finalIv,
                        "encryptedAesKeys" to finalEncryptedKeys,
                        "isEdited" to true
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error editing message", e)
            false
        }
    }

    suspend fun deleteMessageForMe(messageId: String, roomId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            chatsCollection.document(roomId).collection("messages").document(messageId)
                .update("deletedFor", FieldValue.arrayUnion(currentUser.uid))
                .await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error deleting message for me", e)
            false
        }
    }

    suspend fun deleteChat(roomId: String) = withContext(Dispatchers.IO) {
        try {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
            val roomRef = chatsCollection.document(roomId)
            val deleteTimestamp = System.currentTimeMillis()

            // Use a transaction to atomically mark deletion and check if all users deleted
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(ChatRoom::class.java) ?: return@runTransaction

                // Add to deletedBy and set deletion timestamp
                val updatedDeletedBy = room.deletedBy.toMutableList()
                if (!updatedDeletedBy.contains(currentUserId)) {
                    updatedDeletedBy.add(currentUserId)
                }
                val updatedDeletedAt = room.deletedAt.toMutableMap()
                updatedDeletedAt[currentUserId] = deleteTimestamp

                transaction.update(roomRef, mapOf(
                    "deletedBy" to updatedDeletedBy,
                    "deletedAt" to updatedDeletedAt
                ))
            }.await()

            // After the transaction, check if all participants have deleted
            val roomDoc = roomRef.get().await()
            val room = roomDoc.toObject(ChatRoom::class.java)
            if (room != null) {
                val allDeleted = room.participants.all { room.deletedBy.contains(it) }
                if (allDeleted) {
                    // Both users deleted — permanently purge all messages
                    purgeMessages(roomId)
                    // Reset room to clean state (keeps the document for future chats)
                    roomRef.update(mapOf(
                        "lastMessage" to "",
                        "lastSender" to null,
                        "lastMessageTime" to 0L,
                        "unreadCount" to room.participants.associateWith { 0 }
                    )).await()
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("SupabaseChatRepo", "deleteChat error", e)
        }
    }

    private suspend fun purgeMessages(roomId: String) {
        try {
            val uid = auth.currentUser?.uid ?: return
            val messagesRef = chatsCollection.document(roomId).collection("messages")
            // Only purge messages authored by currentUser to comply with Firestore senderId delete rule
            var snapshot = messagesRef.whereEqualTo("senderId", uid).limit(100).get().await()
            while (snapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                snapshot = messagesRef.whereEqualTo("senderId", uid).limit(100).get().await()
            }
            android.util.Log.d("SupabaseChatRepo", "Purged own messages from $roomId")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseChatRepo", "Error purging messages for $roomId", e)
        }
    }

    suspend fun uploadGroupImageAndUpdate(roomId: String, imageBytes: ByteArray, extension: String) {
        try {
            val imageUrl = uploadImageToSupabase(imageBytes, extension)
            if (imageUrl != null) {
                updateGroupImage(roomId, imageUrl)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("SupabaseChatRepo", "uploadGroupImageAndUpdate error", e)
        }
    }

    suspend fun loadMutedChatIds(): Set<String> {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return emptySet()
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = db.collection("user_chat_prefs")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("muted", true)
                .get().await()
            snapshot.documents.mapNotNull { it.getString("roomId") }.toSet()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptySet()
        }
    }

    suspend fun muteChat(roomId: String, mute: Boolean) {
        try {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val ref = db.collection("user_chat_prefs").document("${currentUserId}_${roomId}")
            if (mute) {
                ref.set(mapOf("muted" to true, "roomId" to roomId, "userId" to currentUserId)).await()
            } else {
                ref.delete().await()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("SupabaseChatRepo", "muteChat error", e)
        }
    }

    suspend fun archiveChat(roomId: String, archive: Boolean) = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            if (archive) {
                roomRef.update("archivedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)).await()
            } else {
                roomRef.update("archivedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)).await()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("SupabaseChatRepo", "archiveChat error", e)
        }
    }

    suspend fun clearChatHistory(roomId: String) = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            roomRef.update("deletedAt.$currentUserId", System.currentTimeMillis()).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("SupabaseChatRepo", "clearChatHistory error", e)
        }
    }

    suspend fun votePoll(
        roomId: String,
        messageId: String,
        optionIndex: Int,
        allowMultiple: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val msgRef = chatsCollection.document(roomId).collection("messages").document(messageId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(msgRef)
                val msg = snapshot.toObject(ChatMessage::class.java) ?: return@runTransaction
                val currentVotes = msg.pollVotes.toMutableMap()
                val userCurrentVote = currentVotes[currentUser.uid] ?: ""
                val userIndices = if (userCurrentVote.isEmpty()) mutableListOf() else userCurrentVote.split(",").filter { it.isNotBlank() }.toMutableList()

                val optStr = optionIndex.toString()
                if (userIndices.contains(optStr)) {
                    userIndices.remove(optStr)
                } else {
                    if (!allowMultiple) {
                        userIndices.clear()
                    }
                    userIndices.add(optStr)
                }

                if (userIndices.isEmpty()) {
                    currentVotes.remove(currentUser.uid)
                } else {
                    currentVotes[currentUser.uid] = userIndices.joinToString(",")
                }
                transaction.update(msgRef, "pollVotes", currentVotes)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error voting on poll", e)
            false
        }
    }


    suspend fun retractPollVote(
        roomId: String,
        messageId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val msgRef = chatsCollection.document(roomId).collection("messages").document(messageId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(msgRef)
                val msg = snapshot.toObject(ChatMessage::class.java) ?: return@runTransaction
                val currentVotes = msg.pollVotes.toMutableMap()
                currentVotes.remove(currentUser.uid)
                transaction.update(msgRef, "pollVotes", currentVotes)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error retracting poll vote", e)
            false
        }
    }

    suspend fun closePoll(
        roomId: String,
        messageId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val msgRef = chatsCollection.document(roomId).collection("messages").document(messageId)
            msgRef.update("isPollClosed", true).await()
            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error closing poll", e)
            false
        }
    }
}
