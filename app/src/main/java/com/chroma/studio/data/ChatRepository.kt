package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val usersCollection = firestore.collection("users")

    private fun getChatRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    suspend fun getOrCreateChatRoom(otherUserId: String): ChatRoom? {
        val currentUser = auth.currentUser ?: return null
        val roomId = getChatRoomId(currentUser.uid, otherUserId)
        
        return try {
            val doc = chatsCollection.document(roomId).get().await()
            if (doc.exists()) {
                doc.toObject(ChatRoom::class.java)
            } else {
                // Fetch lightweight profiles
                val currentUserDoc = usersCollection.document(currentUser.uid).get().await()
                val otherUserDoc = usersCollection.document(otherUserId).get().await()
                
                val currentRaw = currentUserDoc.toObject(UserProfile::class.java)
                val otherRaw = otherUserDoc.toObject(UserProfile::class.java)
                
                val currentUserProfile = UserProfile(
                    uid = currentUser.uid,
                    displayName = currentRaw?.displayName ?: "Me",
                    username = currentRaw?.username ?: "",
                    avatarUrl = currentRaw?.avatarUrl ?: "",
                    verified = currentRaw?.verified ?: false
                )
                val otherUserProfile = UserProfile(
                    uid = otherUserId,
                    displayName = otherRaw?.displayName ?: "User",
                    username = otherRaw?.username ?: "",
                    avatarUrl = otherRaw?.avatarUrl ?: "",
                    verified = otherRaw?.verified ?: false
                )
                
                val newRoom = ChatRoom(
                    id = roomId,
                    participants = listOf(currentUser.uid, otherUserId),
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = mapOf(currentUser.uid to 0, otherUserId to 0)
                )
                chatsCollection.document(roomId).set(newRoom).await()
                newRoom
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting/creating chat room", e)
            null
        }
    }
    suspend fun createGroup(
        groupName: String,
        groupDescription: String? = null,
        participantIds: List<String> = emptyList(),
        imageUrl: String? = null,
        preGeneratedId: String? = null,
        onlyAdminsCanPost: Boolean = false
    ): ChatRoom? {
        val currentUser = auth.currentUser ?: return null
        val roomId = preGeneratedId ?: "group_${java.util.UUID.randomUUID()}"
        
        return try {
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
            sendMessage(roomId, "Group created", messageType = "system")
            newRoom
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating group", e)
            null
        }
    }

    suspend fun createGroup(groupName: String): ChatRoom? =
        createGroup(groupName = groupName, participantIds = emptyList())

    suspend fun sendMessage(
        roomId: String,
        text: String,
        workId: String? = null,
        imageUrl: String? = null,
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
        flashVideoDurationMs: Long? = null
    ): Boolean {
        val currentUser = auth.currentUser ?: return false
        return try {
            val messageId = preGeneratedId ?: java.util.UUID.randomUUID().toString()
            val message = ChatMessage(
                id = messageId,
                senderId = currentUser.uid,
                text = text,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                workId = workId,
                imageUrl = imageUrl,
                isPending = false,
                messageType = messageType,
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
                flashVideoDurationMs = flashVideoDurationMs
            )
            
            // Add message to subcollection
            chatsCollection.document(roomId).collection("messages").document(message.id).set(message).await()
            
            // Update chat room last message
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null) {
                    val otherUserIds = room.participants.filter { it != currentUser.uid }
                    val updatedUnreadCount = room.unreadCount.toMutableMap()
                    for (id in otherUserIds) {
                        updatedUnreadCount[id] = (updatedUnreadCount[id] ?: 0) + 1
                    }
                    val displayMessage = if (messageType == "flash_video" || flashVideoId != null) "⚡ Shared a Flash video"
                                         else if (storyThumbnailUrl != null || storyId != null) "Replied to story: $text"
                                         else if (imageUrl != null) "Sent an image" 
                                         else if (workId != null) "Shared a work" 
                                         else text
                    transaction.update(roomRef, "lastMessage", displayMessage)
                    transaction.update(roomRef, "lastMessageTime", message.timestamp)
                    transaction.update(roomRef, "lastSender", currentUser.uid)
                    transaction.update(roomRef, "lastMessageId", message.id)
                    transaction.update(roomRef, "lastMessageIsRead", false)
                    transaction.update(roomRef, "lastMessageIsDelivered", false)
                    transaction.update(roomRef, "unreadCount", updatedUnreadCount)
                    transaction.update(roomRef, "deletedBy", emptyList<String>())
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
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

    suspend fun markAsRead(roomId: String) {
        val currentUser = auth.currentUser ?: return
        try {
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null) {
                    val updatedUnreadCount = room.unreadCount.toMutableMap()
                    updatedUnreadCount[currentUser.uid] = 0
                    transaction.update(roomRef, "unreadCount", updatedUnreadCount)
                    if (room.lastSender != null && room.lastSender != currentUser.uid) {
                        transaction.update(roomRef, "lastMessageIsRead", true)
                        transaction.update(roomRef, "lastMessageIsDelivered", true)
                    }
                }
            }.await()
            
            // Mark individual messages as read and delivered
            val recentMessages = roomRef.collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            val docsToUpdate = recentMessages.documents.filter { doc ->
                val senderId = doc.getString("senderId")
                val isRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: false
                !isRead && senderId != null && senderId != currentUser.uid
            }
            if (docsToUpdate.isNotEmpty()) {
                docsToUpdate.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    for (doc in chunk) {
                        batch.update(doc.reference, mapOf(
                            "isRead" to true,
                            "read" to true,
                            "isDelivered" to true,
                            "delivered" to true
                        ))
                    }
                    batch.commit().await()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking as read", e)
        }
    }

    suspend fun markAsDelivered(roomId: String) {
        val currentUser = auth.currentUser ?: return
        try {
            val roomRef = chatsCollection.document(roomId)
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                if (room != null && room.lastSender != null && room.lastSender != currentUser.uid && !room.lastMessageIsDelivered) {
                    transaction.update(roomRef, "lastMessageIsDelivered", true)
                }
            }.await()

            val recentMessages = roomRef.collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            val docsToUpdate = recentMessages.documents.filter { doc ->
                val senderId = doc.getString("senderId")
                val isDelivered = doc.getBoolean("isDelivered") ?: doc.getBoolean("delivered") ?: false
                !isDelivered && senderId != null && senderId != currentUser.uid
            }
            if (docsToUpdate.isNotEmpty()) {
                docsToUpdate.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    for (doc in chunk) {
                        batch.update(doc.reference, mapOf(
                            "isDelivered" to true,
                            "delivered" to true
                        ))
                    }
                    batch.commit().await()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking as delivered", e)
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
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatRepository", "Listen failed for chats", e)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { it.toObject(ChatRoom::class.java) } ?: emptyList()
                val activeChats = chats.filter { !it.deletedBy.contains(currentUser.uid) }
                trySend(activeChats)
            }
            
        awaitClose { listener.remove() }
    }

    fun observeMessages(roomId: String, limit: Long = 50): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatsCollection.document(roomId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatRepository", "Listen failed for messages", e)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }
            
        awaitClose { listener.remove() }
    }

    suspend fun toggleFavorite(roomId: String, userId: String, isFavorite: Boolean) {
        try {
            val update = if (isFavorite) {
                FieldValue.arrayUnion(userId)
            } else {
                FieldValue.arrayRemove(userId)
            }
            chatsCollection.document(roomId).update("favoritedBy", update).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error toggling favorite status", e)
        }
    }

    suspend fun getRoom(roomId: String): ChatRoom? {
        return try {
            val doc = chatsCollection.document(roomId).get().await()
            if (doc.exists()) doc.toObject(ChatRoom::class.java) else null
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting room $roomId", e)
            null
        }
    }

    suspend fun updateGroupName(roomId: String, name: String) {
        try {
            chatsCollection.document(roomId).update("name", name).await()
            sendMessage(roomId, "Group name changed to \"$name\"", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error updating group name", e)
        }
    }

    suspend fun updateGroupImage(roomId: String, imageUrl: String) {
        try {
            chatsCollection.document(roomId).update("avatarUrl", imageUrl).await()
            sendMessage(roomId, "Group image updated", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error updating group image", e)
        }
    }

    suspend fun updateGroupDescription(roomId: String, description: String) {
        try {
            chatsCollection.document(roomId).update("groupDescription", description).await()
            sendMessage(roomId, "Group description updated", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error updating group description", e)
        }
    }

    suspend fun updateOnlyAdminsCanPost(roomId: String, onlyAdminsCanPost: Boolean) {
        try {
            chatsCollection.document(roomId).update("onlyAdminsCanPost", onlyAdminsCanPost).await()
            val notice = if (onlyAdminsCanPost) "Only admins can send messages" else "All members can send messages"
            sendMessage(roomId, "Group settings updated: $notice", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error updating onlyAdminsCanPost", e)
        }
    }

    suspend fun addParticipants(roomId: String, userIds: List<String>) {
        if (userIds.isEmpty()) return
        try {
            chatsCollection.document(roomId).update("participants", FieldValue.arrayUnion(*userIds.toTypedArray())).await()
            sendMessage(roomId, "New members added to the group", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error adding participants", e)
        }
    }

    suspend fun removeParticipant(roomId: String, userId: String) {
        try {
            chatsCollection.document(roomId).update(
                "participants", FieldValue.arrayRemove(userId),
                "admins", FieldValue.arrayRemove(userId)
            ).await()
            sendMessage(roomId, "A member was removed from the group", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error removing participant", e)
        }
    }

    suspend fun makeAdmin(roomId: String, userId: String) {
        try {
            chatsCollection.document(roomId).update("admins", FieldValue.arrayUnion(userId)).await()
            sendMessage(roomId, "A member is now a group admin", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error making admin", e)
        }
    }

    suspend fun demoteAdmin(roomId: String, userId: String) {
        try {
            chatsCollection.document(roomId).update("admins", FieldValue.arrayRemove(userId)).await()
            sendMessage(roomId, "A member is no longer a group admin", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error demoting admin", e)
        }
    }

    suspend fun leaveGroup(roomId: String) {
        val currentUser = auth.currentUser ?: return
        try {
            chatsCollection.document(roomId).update(
                "participants", FieldValue.arrayRemove(currentUser.uid),
                "admins", FieldValue.arrayRemove(currentUser.uid)
            ).await()
            sendMessage(roomId, "A member left the group", messageType = "system")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error leaving group", e)
        }
    }

    companion object {
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository().also { instance = it }
            }
        }
    }
}
