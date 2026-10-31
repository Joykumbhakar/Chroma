@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_IS_NOT_ENABLED")
package com.chroma.studio.services

import android.util.Log
import com.chroma.studio.ChatInAppEvent
import com.chroma.studio.ChatNotificationBuilder
import com.chroma.studio.ChatStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
class ChromaMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ChromaMessagingService"

        fun syncTokenForCurrentUser() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(
                                    mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()),
                                    SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    Log.d(TAG, "FCM token synced successfully for $uid")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to retrieve FCM token", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing FCM token", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token refreshed: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), SetOptions.merge())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleMessagePayload(remoteMessage)
    }

    private fun handleMessagePayload(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty() && remoteMessage.notification == null) return

        val senderId = data["senderId"] ?: data["sender_id"] ?: data["userId"] ?: ""
        val recipientId = data["recipientId"] ?: data["recipient_id"] ?: ""
        val roomId = data["roomId"] ?: data["room_id"] ?: ""
        val title = data["title"] ?: data["senderName"] ?: data["sender_name"] ?: remoteMessage.notification?.title ?: "New Message"
        val body = data["body"] ?: data["text"] ?: data["message"] ?: remoteMessage.notification?.body ?: "You have a new message."
        val avatarUrl = data["avatarUrl"] ?: data["avatar_url"] ?: data["senderAvatar"] ?: ""

        val currentUserId = ChatStateManager.currentUserId.value 
            ?: FirebaseAuth.getInstance().currentUser?.uid

        // Rule 1: If the message is from the current user -> suppress both
        if (senderId.isNotBlank() && currentUserId != null && senderId == currentUserId) {
            Log.d(TAG, "Suppressed: message from current user ($senderId)")
            return
        }

        val isForeground = ChatStateManager.isAppInForeground.value
        val isUserInActiveChat = ChatStateManager.isChatActive(
            recipientId = senderId.ifBlank { recipientId },
            roomId = roomId
        )

        // Rule 2: If the app is in foreground and user is in active chat -> suppress both
        if (isForeground && isUserInActiveChat) {
            Log.d(TAG, "Suppressed: app in foreground & user in active chat ($roomId / $senderId)")
            return
        }

        // Rule 3: If the app is in the foreground but user is NOT in that specific chat ->
        // emit to the Dynamic Island event flow to trigger the in-app overlay, suppressing system notification
        if (isForeground) {
            Log.d(TAG, "App in foreground: emitting to Dynamic Island event flow & suppressing system notification")
            ChatStateManager.emitInAppEvent(
                ChatInAppEvent(
                    roomId = roomId,
                    senderId = senderId,
                    senderName = title,
                    messageText = body,
                    avatarUrl = avatarUrl
                )
            )
            com.chroma.studio.chat.sound.SoundManager.playNotification(this)
            return
        }

        // Rule 4: If the app is in the background or closed ->
        // system notification builder creates a Heads-Up alert with Direct Reply and options to Mark as Read.
        // Check if this chat is protected with Biometric Chat Lock:
        val prefs = getSharedPreferences("chat_security_preferences", android.content.Context.MODE_PRIVATE)
        val lockedIds = prefs.getStringSet("locked_chat_ids", emptySet()) ?: emptySet()
        val isLocked = (roomId.isNotBlank() && roomId in lockedIds) || (senderId.isNotBlank() && senderId in lockedIds)

        Log.d(TAG, "App in background or closed: building Heads-Up alert (locked=$isLocked) with Direct Reply & Mark as Read")
        ChatNotificationBuilder.showHeadsUpNotification(
            context = this,
            roomId = roomId,
            senderId = senderId,
            senderName = title,
            messageText = body,
            avatarUrl = avatarUrl,
            isLocked = isLocked
        )
    }
}
