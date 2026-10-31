package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.ChromaNotification
import com.chroma.studio.model.NotificationActionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
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
        targetId: String? = null
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
                    val pushTitle = if (type == NotificationActionType.MESSAGE) actorName else "$actorName ($actorHandle)"
                    val pushBody = detailText ?: actionText
                    FcmPushSender.sendPushNotification(
                        context = com.chroma.studio.ChromaApp.instance,
                        targetFcmToken = targetFcmToken,
                        roomId = targetId ?: "",
                        senderId = currentUid,
                        senderName = pushTitle,
                        messageText = pushBody,
                        avatarUrl = actorAvatarUrl ?: ""
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
