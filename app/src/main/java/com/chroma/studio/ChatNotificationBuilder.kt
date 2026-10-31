@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation

/**
 * System notification builder creating Heads-Up alerts with Direct Reply,
 * Lock Screen wakeup, and options to Mark as Read when the app is in the background or closed.
 */
object ChatNotificationBuilder {
    private const val TAG = "ChatNotifBuilder"
    const val CHANNEL_ID = "chat_notifications_heads_up"
    const val CHANNEL_NAME = "Chat Messages"
    const val KEY_TEXT_REPLY = "KEY_TEXT_REPLY"
    const val ACTION_REPLY = "ACTION_REPLY"
    const val ACTION_MARK_READ = "ACTION_MARK_READ"

    /**
     * Wakes up the screen when the device is locked/asleep so that incoming notifications
     * are immediately visible on the lock screen.
     */
    fun wakeUpScreen(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                    "oscellion:chat_lockscreen_wake"
                )
                wakeLock.acquire(4000L) // 4-second timeout to illuminate lock screen
            }
        } catch (e: Exception) {
            Log.w(TAG, "Screen wakeup failed", e)
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            val notifPrefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
            val selectedToneId = notifPrefs.getString("selected_notification_sound", "default")
            val toneObj = com.chroma.studio.chat.sound.AVAILABLE_NOTIFICATION_TONES.find { it.id == selectedToneId }
                ?: com.chroma.studio.chat.sound.AVAILABLE_NOTIFICATION_TONES.first()
            val defaultSoundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/" + toneObj.resId)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val currentChannelId = "${CHANNEL_ID}_$selectedToneId"
            val activeChannel = notificationManager.getNotificationChannel(currentChannelId)
            if (activeChannel == null) {
                val channel = NotificationChannel(
                    currentChannelId,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High-priority heads-up chat alerts with direct reply and lockscreen display"
                    enableLights(true)
                    lightColor = 0xFF007AFF.toInt()
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(defaultSoundUri, audioAttributes)
                    setBypassDnd(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            } else {
                activeChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }

    fun showHeadsUpNotification(
        context: Context,
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        isLocked: Boolean = false
    ) {
        // 1. Wake the display if asleep / locked
        wakeUpScreen(context)

        // 2. Ensure high-importance public channel
        ensureNotificationChannel(context)

        val notificationId = if (roomId.isNotBlank()) {
            Math.abs(roomId.hashCode())
        } else if (senderId.isNotBlank()) {
            Math.abs(senderId.hashCode())
        } else {
            (System.currentTimeMillis() % 100000).toInt()
        }

        // Tap action: open HomeActivity to this chat
        val contentIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("room_id", roomId)
            putExtra("sender_id", senderId)
            putExtra("open_chat", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // 3. Direct Reply Action with RemoteInput (omitted for locked chats to prevent unauthorized replies)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        val replyIntent = Intent(context, ChatNotificationReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra("room_id", roomId)
            putExtra("sender_id", senderId)
            putExtra("notification_id", notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_oscellion_notification,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // 4. Mark as Read Action
        val markReadIntent = Intent(context, ChatNotificationReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra("room_id", roomId)
            putExtra("sender_id", senderId)
            putExtra("notification_id", notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val markReadAction = NotificationCompat.Action.Builder(
            0,
            "Mark as Read",
            markReadPendingIntent
        ).build()

        val notifPrefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
        val selectedToneId = notifPrefs.getString("selected_notification_sound", "default") ?: "default"
        val toneObj = com.chroma.studio.chat.sound.AVAILABLE_NOTIFICATION_TONES.find { it.id == selectedToneId }
            ?: com.chroma.studio.chat.sound.AVAILABLE_NOTIFICATION_TONES.first()
        val customSoundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/" + toneObj.resId)
        val currentChannelId = "${CHANNEL_ID}_$selectedToneId"

        // 5. Try fetching sender avatar bitmap for rich circular icon
        var avatarBitmap: Bitmap? = null
        if (avatarUrl.isNotBlank() && !isLocked) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .transformations(CircleCropTransformation())
                    .allowHardware(false)
                    .build()
                val result = kotlinx.coroutines.runBlocking {
                    imageLoader.execute(request)
                }
                avatarBitmap = result.drawable?.toBitmap(128, 128)
            } catch (e: Throwable) {
                Log.d(TAG, "Avatar image download skipped: ${e.message}")
            }
        }

        val displayContent = if (isLocked) "🔒 Locked chat: 1 new message" else messageText

        // 6. Build Person & MessagingStyle
        val personBuilder = Person.Builder()
            .setName(senderName)
            .setKey(senderId)
        if (avatarBitmap != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap))
        }
        val person = personBuilder.build()

        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(senderName)
            .setGroupConversation(false)
            .addMessage(displayContent, System.currentTimeMillis(), person)

        val builder = NotificationCompat.Builder(context, currentChannelId)
            .setSmallIcon(R.drawable.ic_oscellion_notification)
            .setContentTitle(senderName)
            .setContentText(displayContent)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(customSoundUri)
            .setLights(0xFF007AFF.toInt(), 500, 500)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(contentPendingIntent, false) // Instant heads-up popup on lock screen
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (!isLocked) {
            builder.addAction(replyAction)
        }
        builder.addAction(markReadAction)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
