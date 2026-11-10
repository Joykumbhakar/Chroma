package com.chroma.studio.services

import android.util.Log
import com.chroma.studio.ChatInAppEvent
import com.chroma.studio.ChatNotificationBuilder
import com.chroma.studio.ChatStateManager
import com.chroma.studio.data.SupabaseChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

open class ChromaMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ChromaMessagingService"
        private val seenMessageIds = object : LinkedHashMap<String, Boolean>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 50
        }

        fun syncTokenForCurrentUser() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), SetOptions.merge())
                                .addOnSuccessListener { Log.d(TAG, "FCM token synced for $uid") }
                        }
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to retrieve FCM token", e) }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing FCM token", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), SetOptions.merge())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleMessagePayload(remoteMessage)
    }

    /** Runs a short network call to completion before the service is allowed to die. */
    private fun runBounded(block: suspend () -> Unit) {
        try {
            runBlocking { withTimeoutOrNull(5_000) { block() } }
        } catch (e: Exception) {
            Log.w(TAG, "Bounded task failed: ${e.message}")
        }
    }

    private fun handleMessagePayload(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty() && remoteMessage.notification == null) return

        val senderId = data["senderId"] ?: data["sender_id"] ?: data["userId"] ?: ""
        val recipientId = data["recipientId"] ?: data["recipient_id"] ?: ""
        val roomId = data["roomId"] ?: data["room_id"] ?: ""
        val messageType = data["messageType"] ?: data["message_type"] ?: "text"
        val groupName = data["groupName"] ?: ""
        val messageId = data["messageId"] ?: ""
        val senderName = data["senderName"] ?: data["sender_name"] ?: data["title"]
            ?: remoteMessage.notification?.title ?: "New Message"
        val rawBody = data["body"] ?: data["text"] ?: data["message"] ?: remoteMessage.notification?.body ?: ""
        val body = when {
            rawBody.isNotBlank() -> rawBody
            messageType == "audio" || messageType == "voice" -> "🎤 Voice message"
            messageType == "video" -> "🎥 Sent a video"
            messageType == "image" -> "📷 Sent a photo"
            messageType == "file" -> "📎 Sent a file"
            else -> "You have a new message."
        }
        val avatarUrl = data["avatarUrl"] ?: data["avatar_url"] ?: data["senderAvatar"] ?: ""

        // De-duplicate (FCM can redeliver the same message)
        if (messageId.isNotBlank()) {
            synchronized(seenMessageIds) {
                if (seenMessageIds.containsKey(messageId)) return
                seenMessageIds[messageId] = true
            }
        }

        val currentUserId = ChatStateManager.currentUserId.value ?: FirebaseAuth.getInstance().currentUser?.uid

        // Rule 1: own message -> nothing
        if (senderId.isNotBlank() && currentUserId != null && senderId == currentUserId) return

        val isForeground = ChatStateManager.isAppInForeground.value
        val isUserInActiveChat = ChatStateManager.isChatActive(
            recipientId = senderId.ifBlank { recipientId },
            roomId = roomId
        )

        // Rule 2: foreground + inside this chat -> no popup, mark read
        if (isForeground && isUserInActiveChat) {
            if (roomId.isNotBlank()) runBounded {
                val repo = SupabaseChatRepository()
                repo.markAsDelivered(roomId)
                repo.markAsRead(roomId)
            }
            return
        }

        // Rule 3: foreground, different screen -> in-app Dynamic Island overlay
        if (isForeground) {
            ChatStateManager.emitInAppEvent(
                ChatInAppEvent(
                    roomId = roomId,
                    senderId = senderId,
                    senderName = if (groupName.isNotBlank()) "$senderName in $groupName" else senderName,
                    messageText = body,
                    avatarUrl = avatarUrl
                )
            )
            com.chroma.studio.chat.sound.SoundManager.playNotification(this)
            if (roomId.isNotBlank()) runBounded { SupabaseChatRepository().markAsDelivered(roomId) }
            return
        }

        // Rule 4: background / killed / locked -> WhatsApp-style heads-up popup.
        val prefs = getSharedPreferences("chat_security_preferences", MODE_PRIVATE)
        val lockedIds = prefs.getStringSet("locked_chat_ids", emptySet()) ?: emptySet()
        val isLocked = (roomId.isNotBlank() && roomId in lockedIds) || (senderId.isNotBlank() && senderId in lockedIds)

        // Show the notification FIRST (time-critical), then report "delivered" (double grey ticks).
        ChatNotificationBuilder.showHeadsUpNotification(
            context = this,
            roomId = roomId,
            senderId = senderId,
            senderName = senderName,
            messageText = body,
            avatarUrl = avatarUrl,
            isLocked = isLocked,
            groupName = groupName,
            messageType = messageType
        )
        if (roomId.isNotBlank()) runBounded { SupabaseChatRepository().markAsDelivered(roomId) }
    }
}
