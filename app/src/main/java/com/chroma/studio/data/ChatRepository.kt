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
    suspend fun createGroup(groupName: String): ChatRoom? {
        val currentUser = auth.currentUser ?: return null
        val roomId = "group_${java.util.UUID.randomUUID()}"
        
        return try {
            val newRoom = ChatRoom(
                id = roomId,
                name = groupName,
                isGroup = true,
                participants = listOf(currentUser.uid),
                admins = listOf(currentUser.uid),
                createdBy = currentUser.uid,
                lastMessage = "Group created",
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = mapOf(currentUser.uid to 0)
            )
            chatsCollection.document(roomId).set(newRoom).await()
            newRoom
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating group", e)
            null
        }
    }

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
        storyThumbnailUrl: String? = null
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
                storyThumbnailUrl = storyThumbnailUrl
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
                    val displayMessage = if (storyThumbnailUrl != null || storyId != null) "Replied to story: $text"
                                         else if (imageUrl != null) "Sent an image" 
                                         else if (workId != null) "Shared a work" 
                                         else text
                    transaction.update(roomRef, "lastMessage", displayMessage)
                    transaction.update(roomRef, "lastMessageTime", message.timestamp)
                    transaction.update(roomRef, "lastSender", currentUser.uid)
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
                }
            }.await()
            
            // Mark individual messages as read (optional optimization: only update unread messages)
            val unreadMessages = roomRef.collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            val batch = firestore.batch()
            for (doc in unreadMessages) {
                val msg = doc.toObject(ChatMessage::class.java)
                if (msg.senderId != currentUser.uid) {
                    batch.update(doc.reference, "isRead", true)
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking as read", e)
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
}
