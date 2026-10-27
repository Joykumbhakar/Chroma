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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    // High-performance in-memory cache to eliminate repeated Firestore round-trips when sending messages
    private val roomParticipantsCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val userPublicKeyCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val decryptedMessageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val roomDeletedAtCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

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

    suspend fun createGroup(groupName: String, groupDescription: String? = null, participantIds: List<String>, imageUrl: String? = null, preGeneratedId: String? = null): ChatRoom? = withContext(Dispatchers.IO) {
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
                unreadCount = unreadMap
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

            // Always encrypt for current user with their active local public key
            if (myPubKey.isNotEmpty()) {
                val myEncKey = com.chroma.studio.utils.CryptoHelper.encryptAESKeyWithRSA(aesBytes, myPubKey)
                encryptedKeysMap[currentUserId] = myEncKey
                usersCollection.document(currentUserId).update("publicKey", myPubKey)
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
                    room.participants
                } else emptyList()
            }

            if (participants.isNotEmpty()) {
                for (participantId in participants) {
                    if (participantId == currentUserId) continue
                    var pubKeyStr = userPublicKeyCache[participantId]
                    if (pubKeyStr.isNullOrEmpty()) {
                        try {
                            val userDoc = usersCollection.document(participantId).get().await()
                            val fetchedKey = userDoc.getString("publicKey")
                            if (!fetchedKey.isNullOrEmpty()) {
                                userPublicKeyCache[participantId] = fetchedKey
                                pubKeyStr = fetchedKey
                            }
                        } catch (_: Exception) {}
                    }
                    if (!pubKeyStr.isNullOrEmpty()) {
                        try {
                            val encKey = com.chroma.studio.utils.CryptoHelper.encryptAESKeyWithRSA(aesBytes, pubKeyStr)
                            encryptedKeysMap[participantId] = encKey
                        } catch (_: Exception) {}
                    }
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
        replyToMessageId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        try {
            val messageId = preGeneratedId ?: java.util.UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val (finalMessageText, finalIv, finalEncryptedKeys) = if (messageType != "system" && text.isNotBlank()) {
                encryptPayload(roomId, text, currentUser.uid)
            } else {
                Triple(text, "", emptyMap<String, String>())
            }

            val message = ChatMessage(
                id = messageId,
                senderId = currentUser.uid,
                text = finalMessageText,
                timestamp = timestamp,
                isRead = false,
                imageUrl = imageUrl,
                workId = workId,
                messageType = messageType,
                iv = finalIv,
                encryptedAesKeys = finalEncryptedKeys,
                durationSeconds = durationSeconds,
                replyToMessageId = replyToMessageId
            )

            // 1. Save directly to Firestore subcollection for 100% reliable persistence & realtime listeners
            chatsCollection.document(roomId).collection("messages").document(message.id).set(message).await()

            // 2. Update chat room last message in Firebase for the ChatList
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null) {
                    val updatedUnreadCount = room.unreadCount.toMutableMap()
                    for (participantId in room.participants) {
                        if (participantId != currentUser.uid) {
                            updatedUnreadCount[participantId] = (updatedUnreadCount[participantId] ?: 0) + 1
                        }
                    }
                    val displayMessage = if (imageUrl != null) {
                        if (messageType == "video") "Sent a video" else if (messageType == "audio") "Voice message" else "Sent an image"
                    } else if (workId != null) {
                        "Shared a work"
                    } else if (text.startsWith("[POLL]:")) {
                        val q = text.removePrefix("[POLL]:").substringBefore("|||").trim()
                        "Poll: $q"
                    } else {
                        text
                    }

                    transaction.update(roomRef, "lastMessage", displayMessage)
                    transaction.update(roomRef, "lastMessageTime", message.timestamp)
                    transaction.update(roomRef, "lastSender", currentUser.uid)
                    transaction.update(roomRef, "unreadCount", updatedUnreadCount)
                    // Clear deletedBy for everyone so the chat resurrects in both inboxes.
                    // Old messages will still be hidden via the deletedAt timestamp.
                    transaction.update(roomRef, "deletedBy", emptyList<String>())
                }
            }.await()

            // 3. Dispatch activity notification to recipient(s)
            try {
                val roomSnap = roomRef.get().await()
                val room = roomSnap.toObject(ChatRoom::class.java)
                if (room != null) {
                    val displaySnippet = if (imageUrl != null) {
                        if (messageType == "video") "Sent a video" else if (messageType == "audio") "Sent a voice message" else "Sent a photo"
                    } else if (text.startsWith("[POLL]:")) {
                        "Created a poll"
                    } else {
                        text
                    }
                    if (room.isGroup) {
                        val otherParticipants = room.participants.filter { it != currentUser.uid }
                        for (recipientId in otherParticipants) {
                            NotificationDispatcher.sendNotification(
                                targetUserId = recipientId,
                                type = com.chroma.studio.model.NotificationActionType.MESSAGE,
                                actionText = "sent a message in ${room.name ?: "Group"}",
                                detailText = displaySnippet,
                                targetPreviewUrl = imageUrl,
                                targetId = roomId
                            )
                        }
                    } else {
                        val recipientId = room.participants.firstOrNull { it != currentUser.uid }
                        if (!recipientId.isNullOrBlank()) {
                            NotificationDispatcher.sendNotification(
                                targetUserId = recipientId,
                                type = com.chroma.studio.model.NotificationActionType.MESSAGE,
                                actionText = "sent you a message",
                                detailText = displaySnippet,
                                targetPreviewUrl = imageUrl,
                                targetId = roomId
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            true
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error sending message", e)
            false
        }
    }

    suspend fun markAsRead(roomId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            // Update Firebase unread count
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null) {
                    val updatedUnreadCount = room.unreadCount.toMutableMap()
                    updatedUnreadCount[currentUser.uid] = 0
                    transaction.update(roomRef, "unreadCount", updatedUnreadCount)
                }
            }.await()

            // Update Firestore individual messages as read
            try {
                val unreadMessages = roomRef.collection("messages")
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                val docsToUpdate = unreadMessages.documents.filter { doc ->
                    val msg = doc.toObject(ChatMessage::class.java)
                    msg != null && msg.senderId != currentUser.uid
                }
                docsToUpdate.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    for (doc in chunk) {
                        batch.update(doc.reference, "isRead", true)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                // ignore
            }



        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error marking as read", e)
        }
    }

    fun observeRecentChats(): Flow<List<ChatRoom>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = chatsCollection
            .whereArrayContains("participants", currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SupabaseChatRepository", "Listen failed for chats", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val rawChats = snapshot?.documents?.mapNotNull { it.toObject(ChatRoom::class.java) } ?: emptyList()
                for (chat in rawChats) {
                    roomParticipantsCache[chat.id] = chat.participants
                    val dAt = chat.deletedAt[currentUser.uid] ?: 0L
                    roomDeletedAtCache[chat.id] = dAt
                }
                val activeChats = rawChats.filter { !it.deletedBy.contains(currentUser.uid) }
                trySend(activeChats.sortedByDescending { it.lastMessageTime })
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun observeMessages(roomId: String, limit: Long = 50): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Instant synchronous retrieval from memory cache (0ms delay) to immediately attach snapshot listener
        val deletedAt = roomDeletedAtCache[roomId] ?: 0L

        var query = chatsCollection.document(roomId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

        if (deletedAt > 0L) {
            query = query.whereGreaterThan("timestamp", deletedAt)
        }

        val firestoreListener = query.limit(limit)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    launch(Dispatchers.Default) {
                        val currentUid = auth.currentUser?.uid
                        val messages = snapshot.documents.mapNotNull { doc ->
                            val msg = doc.toObject(ChatMessage::class.java) ?: return@mapNotNull null

                            // Filter out messages deleted by current user for themselves
                            if (currentUid != null && msg.deletedFor.contains(currentUid)) {
                                return@mapNotNull null
                            }

                            if (msg.messageType == "system" || msg.iv.isEmpty() || msg.text.isEmpty()) {
                                return@mapNotNull msg
                            }

                            val cacheKey = "${msg.id}_${msg.text.hashCode()}_${msg.iv}"
                            val cached = decryptedMessageCache[cacheKey]
                            if (cached != null) {
                                return@mapNotNull msg.copy(text = cached)
                            }

                            var decryptedText = msg.text
                            var isDecrypted = false

                            // 1. Try user-specific RSA decryption
                            if (currentUid != null && msg.encryptedAesKeys.containsKey(currentUid)) {
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
                                } catch (ex: Exception) {
                                    // fall through to room fallback
                                }
                            }

                            // 2. Try room-level fallback decryption
                            if (!isDecrypted) {
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
                                    } else {
                                        val direct = com.chroma.studio.utils.CryptoHelper.decryptMessageWithAES(msg.text, msg.iv, roomKey)
                                        if (direct.isNotEmpty()) {
                                            decryptedText = direct
                                            isDecrypted = true
                                        }
                                    }
                                } catch (ex: Exception) {
                                    // fall through
                                }
                            }

                            // 3. If still not decrypted, check if msg.text was already plaintext or a media message without caption
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
                        trySend(messages)
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
        try {
            chatsCollection.document(roomId).update("admins", FieldValue.arrayRemove(userId)).await()
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error demoting admin", e)
        }
    }

    suspend fun addParticipants(roomId: String, userIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    val newParticipants = (room.participants + userIds).distinct()
                    transaction.update(roomRef, "participants", newParticipants)
                }
            }.await()

            // Add system messages
            for (userId in userIds) {
                val userName = usersCollection.document(userId).get().await().getString("displayName") ?: "Someone"
                sendMessage(roomId, "$userName was added to the group", messageType = "system")
            }
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

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    // Check if current user is admin
                    if (!room.admins.contains(currentUser.uid)) {
                        throw Exception("Only admins can remove participants")
                    }
                    val newParticipants = room.participants.filter { it != userId }
                    val newAdmins = room.admins.filter { it != userId }

                    transaction.update(roomRef, "participants", newParticipants)
                    transaction.update(roomRef, "admins", newAdmins)
                    wasRemoved = true
                }
            }.await()

            if (wasRemoved) {
                removedName = usersCollection.document(userId).get().await().getString("displayName") ?: "Someone"
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
                    val newAdmins = (room.admins + userId).distinct()
                    transaction.update(roomRef, "admins", newAdmins)
                }
            }.await()

            adminName = usersCollection.document(userId).get().await().getString("displayName") ?: "Someone"
            sendMessage(roomId, "$adminName is now an admin", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error making admin", e)
        }
    }

    suspend fun leaveGroup(roomId: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        try {
            val roomRef = chatsCollection.document(roomId)
            var leftName = "Someone"

            firestore.runTransaction { transaction ->
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                if (room != null) {
                    val newParticipants = room.participants.filter { it != currentUser.uid }
                    val newAdmins = room.admins.filter { it != currentUser.uid }

                    transaction.update(roomRef, "participants", newParticipants)
                    transaction.update(roomRef, "admins", newAdmins)
                }
            }.await()

            leftName = usersCollection.document(currentUser.uid).get().await().getString("displayName") ?: "Someone"
            sendMessage(roomId, "$leftName left the group", messageType = "system")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error leaving group", e)
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
        try {
            val db = OnlineStatusManager.getDatabase()
            val ref = db.getReference("status/$roomId/${currentUser.uid}")
            if (activity != null) {
                ref.setValue(activity)
                // Auto-clear on disconnect so stale indicators don't persist on crash
                ref.onDisconnect().removeValue()
            } else {
                ref.removeValue()
                ref.onDisconnect().cancel()
            }
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error updating activity status", e)
        }
    }

    // Helper for uploading chat media to Cloudinary CDN (reserving Supabase exclusively for avatars/banners)
    suspend fun uploadImageToSupabase(imageBytes: ByteArray, extension: String = "jpg"): String? = withContext(Dispatchers.IO) {
        try {
            CloudinaryClient.uploadImage(imageBytes, folder = "chat_media")
        } catch (e: Exception) {
            Log.e("SupabaseChatRepository", "Error uploading chat image to Cloudinary", e)
            null
        }
    }

    // Helper for uploading audio voice note to Cloudinary CDN
        suspend fun uploadVideoToSupabase(videoBytes: ByteArray, extension: String = "mp4"): String? = withContext(Dispatchers.IO) {
        try {
            CloudinaryClient.uploadVideo(videoBytes, folder = "chat_videos")
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

    suspend fun deleteMessage(messageId: String, roomId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from Firestore
            chatsCollection.document(roomId).collection("messages").document(messageId).delete().await()
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
            val messagesRef = chatsCollection.document(roomId).collection("messages")
            var snapshot = messagesRef.limit(100).get().await()
            while (snapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                snapshot = messagesRef.limit(100).get().await()
            }
            android.util.Log.d("SupabaseChatRepo", "Purged all messages from $roomId")
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
