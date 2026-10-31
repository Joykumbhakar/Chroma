package com.chroma.studio

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.chroma.studio.data.SupabaseChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver handling background notification actions:
 * - ACTION_REPLY: Sends direct reply text via SupabaseChatRepository
 * - ACTION_MARK_READ: Marks chat room as read
 */
class ChatNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val rawRoomId = intent.getStringExtra("room_id") ?: intent.getStringExtra("roomId")
        val senderId = intent.getStringExtra("sender_id") ?: intent.getStringExtra("senderId")
        val notificationId = intent.getIntExtra("notification_id", 0)
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == "ACTION_MARK_READ") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val targetRoomId = if (!rawRoomId.isNullOrBlank()) {
                        rawRoomId
                    } else if (!senderId.isNullOrBlank()) {
                        SupabaseChatRepository().getOrCreateChatRoom(senderId)?.id
                    } else null

                    if (!targetRoomId.isNullOrBlank()) {
                        SupabaseChatRepository().markAsRead(targetRoomId)
                    }
                    notificationManager.cancel(notificationId)
                } catch (e: Exception) {
                    Log.e("ChatNotificationRcvr", "Failed to mark as read", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == "ACTION_REPLY") {
            val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("KEY_TEXT_REPLY")?.toString()
            if (!replyText.isNullOrBlank()) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val targetRoomId = if (!rawRoomId.isNullOrBlank()) {
                            rawRoomId
                        } else if (!senderId.isNullOrBlank()) {
                            SupabaseChatRepository().getOrCreateChatRoom(senderId)?.id
                        } else null

                        if (!targetRoomId.isNullOrBlank()) {
                            SupabaseChatRepository().sendMessage(targetRoomId, replyText)
                        }
                        notificationManager.cancel(notificationId)
                    } catch (e: Exception) {
                        Log.e("ChatNotificationRcvr", "Failed to send reply", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (action == "com.chroma.studio.TEST_HEADS_UP") {
            val title = intent.getStringExtra("title") ?: "Oscellion Studio"
            val message = intent.getStringExtra("message") ?: "New message arrived! Lock screen heads-up test."
            val testSenderId = intent.getStringExtra("sender_id") ?: "test_sender"
            val testRoomId = intent.getStringExtra("room_id") ?: "test_room"
            val avatar = intent.getStringExtra("avatar_url") ?: ""
            ChatNotificationBuilder.showHeadsUpNotification(
                context = context,
                roomId = testRoomId,
                senderId = testSenderId,
                senderName = title,
                messageText = message,
                avatarUrl = avatar
            )
        }
    }
}
