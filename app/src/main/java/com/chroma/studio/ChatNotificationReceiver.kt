package com.chroma.studio

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.chroma.studio.data.SupabaseChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val roomId = intent.getStringExtra("room_id") ?: return
        val notificationId = intent.getIntExtra("notification_id", 0)
        val action = intent.action
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (action == "ACTION_MARK_READ") {
            CoroutineScope(Dispatchers.IO).launch {
                SupabaseChatRepository().markAsRead(roomId)
                notificationManager.cancel(notificationId)
            }
        } else if (action == "ACTION_REPLY") {
            val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("KEY_TEXT_REPLY")?.toString()
            if (!replyText.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    SupabaseChatRepository().sendMessage(roomId, replyText)
                    notificationManager.cancel(notificationId)
                }
            }
        }
    }
}
