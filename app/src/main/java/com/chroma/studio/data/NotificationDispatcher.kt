package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.ChromaNotification
import com.chroma.studio.model.NotificationActionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Real-time notification dispatcher and management pipeline for ChromaStudio / ChromaMelt.
 * Centralizes dispatching, deleting, reading, and clearing activity notifications in Firestore.
 */
object NotificationDispatcher {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Dedicated CHAT PUSH: Fetches sender profile once, then pushes to all recipients in parallel.
     * Automatically skips users who muted the chat or blocked the sender, and purges dead FCM tokens.
     */
    suspend fun sendChatPush(
        recipientIds: List<String>,
        roomId: String,
        groupName: String?,
        body: String,
        messageType: String,
        messageId: String = ""
    ) = withContext(Dispatchers.IO) {
        val me = auth.currentUser ?: return@withContext
        val actor = try { db.collection("users").document(me.uid).get().await() } catch (e: Exception) { null }
        val actorName = actor?.getString("displayName")?.ifBlank { null } ?: me.displayName ?: "New message"
        val avatar = actor?.getString("avatarUrl") ?: ""

        recipientIds.filter { it.isNotBlank() && it != me.uid }.map { uid ->
            async {
                try {
                    val userDoc = db.collection("users").document(uid).get().await()
                    val token = userDoc.getString("fcmToken")
                    if (token.isNullOrBlank()) return@async

                    val blocked = (userDoc.get("blockedUsers") as? List<*>)?.contains(me.uid) == true
                    if (blocked) return@async

                    val muted = try {
                        db.collection("user_chat_prefs").document("${uid}_$roomId").get().await()
                            .getBoolean("muted") == true
                    } catch (_: Exception) { false }
                    if (muted) return@async

                    val result = FcmPushSender.sendPushNotification(
                        context = com.chroma.studio.ChromaApp.instance,
                        targetFcmToken = token,
                        roomId = roomId,
                        senderId = me.uid,
                        senderName = actorName,
                        messageText = body,
                        avatarUrl = avatar,
                        messageType = messageType,
                        groupName = groupName ?: "",
                        messageId = messageId
                    )
                    if (result == FcmResult.UNREGISTERED) {
                        db.collection("users").document(uid).update("fcmToken", FieldValue.delete())
                    }
                } catch (e: Exception) {
                    Log.w("NotificationDispatcher", "Chat push to $uid failed: ${e.message}")
                }
            }
        }.awaitAll()
    }

    /**
     * Dispatch an activity notification to a target user.
     * Prevents self-notifications, reads the acting user's profile metadata,
     * and atomically writes the notification payload to `users/{targetUserId}/notifications/{notifId}`.
     */
    suspend fun sendNotification(
        targetUserId: String,
        type: NotificationActionType,
        actionText: String,
        detailText: String? = null,
        targetPreviewUrl: String? = null,
        targetId: String? = null,
        messageType: String = "text"
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext false
        if (targetUserId.isBlank() || targetUserId == currentUid) {
            return@withContext false // Never notify the acting user
        }

        try {
            // Retrieve current user's profile data for rich notification attribution
            val actorDoc = db.collection("users").document(currentUid).get().await()
            val actorName = actorDoc.getString("displayName")?.ifBlank { null }
                ?: auth.currentUser?.displayName
                ?: "Chroma Creator"
            val rawUsername = actorDoc.getString("username")
            val actorHandle = if (!rawUsername.isNullOrBlank()) {
                if (rawUsername.startsWith("@")) rawUsername else "@$rawUsername"
            } else "@user"
            val actorAvatarUrl = actorDoc.getString("avatarUrl")
            val actorGender = actorDoc.getString("gender")
            val isVerified = actorDoc.getBoolean("verified") ?: false

            val notifId = UUID.randomUUID().toString()
            val notification = ChromaNotification(
                id = notifId,
                type = type,
                actorUid = currentUid,
                actorName = actorName,
                actorHandle = actorHandle,
                actorAvatarUrl = actorAvatarUrl,
                actorGender = actorGender,
                isVerified = isVerified,
                actionText = actionText,
                detailText = detailText,
                targetPreviewUrl = targetPreviewUrl,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                targetId = targetId
            )

            db.collection("users")
                .document(targetUserId)
                .collection("notifications")
                .document(notifId)
                .set(notification.toMap())
                .await()

            // Trigger real-time FCM push notification to target device
            try {
                val targetUserDoc = db.collection("users").document(targetUserId).get().await()
                val targetFcmToken = targetUserDoc.getString("fcmToken")
                if (!targetFcmToken.isNullOrBlank()) {
                    val pushTitle = if (type == NotificationActionType.MESSAGE) {
                        if (actionText.contains(" in ")) {
                            val groupName = actionText.substringAfter(" in ")
                            "$actorName in $groupName"
                        } else {
                            actorName
                        }
                    } else "$actorName ($actorHandle)"
                    val pushBody = detailText ?: actionText
                    FcmPushSender.sendPushNotification(
                        context = com.chroma.studio.ChromaApp.instance,
                        targetFcmToken = targetFcmToken,
                        roomId = targetId ?: "",
                        senderId = currentUid,
                        senderName = pushTitle,
                        messageText = pushBody,
                        avatarUrl = actorAvatarUrl ?: "",
                        messageType = messageType
                    )
                }
            } catch (e: Exception) {
                Log.w("NotificationDispatcher", "Could not trigger FCM push: ${e.message}")
            }

            true
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Failed to dispatch notification to $targetUserId", e)
            false
        }
    }

    /**
     * Dispatch an official welcome notification to initialize the activity feed for a new or active user.
     */
    suspend fun sendWelcomeNotification(userId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext false
        if (userId.isBlank()) return@withContext false
        try {
            val notifId = "welcome_$userId"
            val notifRef = db.collection("users").document(userId).collection("notifications").document(notifId)
            val doc = notifRef.get().await()
            if (!doc.exists()) {
                val welcomeNotif = ChromaNotification(
                    id = notifId,
                    type = NotificationActionType.SYSTEM_DROP,
                    actorUid = currentUid,
                    actorName = "Oscellion",
                    actorHandle = "@oscellion",
                    actorAvatarUrl = null,
                    isVerified = true,
                    actionText = "Welcome to Oscellion Studio! Start creating, messaging, and sharing with the global community.",
                    detailText = "Your creative canvas and encrypted messaging studio are ready to explore.",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                notifRef.set(welcomeNotif.toMap()).await()
            }
            true
        } catch (e: Exception) {
            Log.w("NotificationDispatcher", "Could not create welcome notification: ${e.message}")
            false
        }
    }

    /**
     * Mark all unread notifications as read for a given user.
     */
    suspend fun markAllAsRead(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        try {
            val col = db.collection("users").document(userId).collection("notifications")
            val snapshot = col.whereEqualTo("isRead", false).limit(200).get().await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            true
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Failed to mark all notifications read for $userId", e)
            false
        }
    }

    /**
     * Delete a single notification.
     */
    suspend fun deleteNotification(userId: String, notifId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || notifId.isBlank()) return@withContext false
        try {
            db.collection("users").document(userId)
                .collection("notifications").document(notifId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Failed to delete notification $notifId for $userId", e)
            false
        }
    }

    /**
     * Toggle or update read status for a single notification.
     */
    suspend fun updateNotificationRead(userId: String, notifId: String, isRead: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || notifId.isBlank()) return@withContext false
        try {
            db.collection("users").document(userId)
                .collection("notifications").document(notifId)
                .update("isRead", isRead)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Failed to update notification read status for $userId", e)
            false
        }
    }

    /**
     * Clear all notifications for a user in batches.
     */
    suspend fun clearAllNotifications(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        try {
            val col = db.collection("users").document(userId).collection("notifications")
            val snapshot = col.limit(300).get().await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            true
        } catch (e: Exception) {
            Log.e("NotificationDispatcher", "Failed to clear notifications for $userId", e)
            false
        }
    }
}
