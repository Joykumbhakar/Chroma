package com.chroma.studio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChromaMessagingService : FirebaseMessagingService() {

    companion object {
        fun syncTokenForCurrentUser() {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(
                                    mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    Log.d("ChromaMessagingService", "FCM token synced successfully for $uid")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("ChromaMessagingService", "Failed to retrieve FCM token", e)
                    }
            } catch (e: Exception) {
                Log.e("ChromaMessagingService", "Error syncing FCM token", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("ChromaMessagingService", "Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token.isNullOrBlank()) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("ChromaMessagingService", "FCM registration token synced to users/$uid")
                }
                .addOnFailureListener { e ->
                    Log.w("ChromaMessagingService", "Could not sync FCM token to users/$uid", e)
                }
        } catch (e: Exception) {
            Log.e("ChromaMessagingService", "Failed to save FCM token to Firestore", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("ChromaMessagingService", "From: ${remoteMessage.from}")

        var title: String? = null
        var body: String? = null

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("ChromaMessagingService", "Message Notification Body: ${it.body}")
            title = it.title
            body = it.body
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("ChromaMessagingService", "Message data payload: ${remoteMessage.data}")
            if (title == null) title = remoteMessage.data["title"]
            if (body == null) body = remoteMessage.data["body"]
        }

        if (title != null || body != null) {
            sendNotification(title ?: "Oscellion", body ?: "")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent().setClassName(this, "com.chroma.studio.HomeActivity")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_oscellion_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo, a notification channel is needed.
        // We now initialize it globally in ChromaApp.kt at startup to ensure 
        // background notifications from Firebase always have an active channel.

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
