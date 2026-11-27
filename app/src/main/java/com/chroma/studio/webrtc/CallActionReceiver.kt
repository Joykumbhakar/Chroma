package com.chroma.studio.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chroma.studio.data.NotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles Decline & Dismiss actions from the incoming call heads-up/lockscreen notification
 * without forcing the UI open.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val callId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALL_ID).orEmpty()
        val callerId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLER_ID).orEmpty()

        when (intent.action) {
            IncomingCallNotificationManager.ACTION_DISMISS_CALL -> {
                CallRingtoneController.stop(callId.ifBlank { null })
            }

            IncomingCallNotificationManager.ACTION_DECLINE_CALL -> {
                if (callId.isBlank()) return
                IncomingCallNotificationManager.cancelIncomingCallNotification(app, callId)
                CallManager.getInstance(app).handleRemoteCallCancelledFromPush(callId, "rejected")

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        withTimeoutOrNull(6_000) {
                            CallSignalingClient().endCall(callId, "rejected")
                            if (callerId.isNotBlank()) {
                                NotificationDispatcher.sendCallEndedPush(
                                    targetUserId = callerId,
                                    callId = callId,
                                    status = "rejected"
                                )
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w("CallActionReceiver", "Decline call signal warning: ${e.message}")
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
