package com.chroma.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.chroma.studio.data.SupabaseChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles the notification actions: inline Reply, Mark as read, and swipe-dismiss.
 * goAsync() keeps the process alive (~10s) while the network work finishes.
 */
class ChatActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val roomId = intent.getStringExtra(ChatNotificationBuilder.EXTRA_ROOM_ID).orEmpty()
        val senderId = intent.getStringExtra(ChatNotificationBuilder.EXTRA_SENDER_ID).orEmpty()
        val key = roomId.ifBlank { senderId }

        when (intent.action) {
            ChatNotificationBuilder.ACTION_DISMISSED -> ChatNotificationBuilder.clearHistory(app, key)

            ChatNotificationBuilder.ACTION_MARK_READ -> {
                ChatNotificationBuilder.cancelForRoom(app, roomId, senderId)
                if (roomId.isBlank()) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        withTimeoutOrNull(8_000) { SupabaseChatRepository().markAsRead(roomId) }
                    } catch (e: Exception) {
                        Log.w("ChatActionReceiver", "markAsRead failed: ${e.message}")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ChatNotificationBuilder.ACTION_REPLY -> {
                val reply = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(ChatNotificationBuilder.KEY_TEXT_REPLY)?.toString()?.trim()
                if (reply.isNullOrBlank() || roomId.isBlank()) {
                    ChatNotificationBuilder.cancelForRoom(app, roomId, senderId)
                    return
                }
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    var ok = false
                    try {
                        val repo = SupabaseChatRepository()
                        ok = withTimeoutOrNull(8_000) { repo.sendMessage(roomId, reply) } == true
                        if (ok) repo.markAsRead(roomId)
                    } catch (e: Exception) {
                        Log.w("ChatActionReceiver", "Reply failed: ${e.message}")
                    } finally {
                        // Always update the notification so the reply spinner goes away.
                        ChatNotificationBuilder.appendOwnReply(app, roomId, senderId, reply, failed = !ok)
                        pending.finish()
                    }
                }
            }
        }
    }
}
