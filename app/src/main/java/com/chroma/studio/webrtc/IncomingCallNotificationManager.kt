package com.chroma.studio.webrtc

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.chroma.studio.ChatNotificationBuilder
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles system-level incoming call presentation when the app is not in foreground:
 * - Screen off / device locked -> Wakes screen + FullScreenIntent + launches IncomingCallActivity
 * - Screen unlocked & user running another app -> Compact heads-up CallStyle pop-up notification (no full-screen takeover)
 */
object IncomingCallNotificationManager {
    private const val TAG = "IncomingCallNotif"
    const val CALL_CHANNEL_ID = "incoming_calls_v2"

    const val ACTION_DECLINE_CALL = "com.chroma.studio.action.CALL_DECLINE"
    const val ACTION_DISMISS_CALL = "com.chroma.studio.action.CALL_DISMISSED"

    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_ROOM_ID = "extra_room_id"
    const val EXTRA_CALLER_ID = "extra_caller_id"
    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALLER_AVATAR = "extra_caller_avatar"
    const val EXTRA_CALLEE_ID = "extra_callee_id"
    const val EXTRA_CALL_TYPE = "extra_call_type"
    const val EXTRA_CALL_TIMESTAMP = "extra_call_timestamp"
    const val EXTRA_CALLER_HANDLE = "extra_caller_handle"
    const val EXTRA_CALLER_VERIFIED = "extra_caller_verified"
    const val EXTRA_AUTO_ANSWER = "extra_auto_answer"

    private val avatarCache = LruCache<String, Bitmap>(16)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CALL_CHANNEL_ID) != null) return

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming Oscellion audio and video calls"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 750, 950, 750)
            enableLights(true)
            lightColor = Color.parseColor("#34C759")
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            if (ringtoneUri != null) {
                setSound(
                    ringtoneUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
        nm.createNotificationChannel(channel)
    }

    fun notificationIdForCall(callId: String): Int {
        return ("incoming_call:" + callId).hashCode()
    }

    @SuppressLint("MissingPermission")
    fun showIncomingCallNotification(
        context: Context,
        call: CallModel,
        callerHandle: String = "",
        isVerified: Boolean = false
    ) {
        if (call.id.isBlank()) return
        val app = context.applicationContext
        ensureChannel(app)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS missing; cannot show incoming call notification")
            return
        }
        val nm = NotificationManagerCompat.from(app)
        if (!nm.areNotificationsEnabled()) return

        val powerManager = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = app.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isScreenInteractive = powerManager?.isInteractive ?: true
        val isDeviceLocked = keyguardManager?.isKeyguardLocked ?: false

        val mode = resolveIncomingCallPresentation(
            isAppForeground = false,
            isScreenInteractive = isScreenInteractive,
            isDeviceLocked = isDeviceLocked,
            isOnChatScreen = false
        )

        val callerName = call.callerName.ifBlank { "Oscellion User" }
        val callLabel = if (call.isVideo) "FaceTime Video" else "Oscellion Audio"
        val avatar = loadCircularAvatar(call.callerAvatar, callerName)

        val fullScreenIntent = buildActivityIntent(app, call, callerHandle, isVerified, autoAnswer = false)
        val answerIntent = buildActivityIntent(app, call, callerHandle, isVerified, autoAnswer = true)

        val notifId = notificationIdForCall(call.id)
        val fullScreenPi = PendingIntent.getActivity(
            app,
            notifId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val answerPi = PendingIntent.getActivity(
            app,
            notifId + 1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declineIntent = Intent(app, CallActionReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALL_ID, call.id)
            putExtra(EXTRA_ROOM_ID, call.roomId)
            putExtra(EXTRA_CALLER_ID, call.callerId)
        }
        val declinePi = PendingIntent.getBroadcast(
            app,
            notifId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = Intent(app, CallActionReceiver::class.java).apply {
            action = ACTION_DISMISS_CALL
            putExtra(EXTRA_CALL_ID, call.id)
        }
        val dismissPi = PendingIntent.getBroadcast(
            app,
            notifId + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .setIcon(IconCompat.createWithBitmap(avatar))
            .build()

        val smallIconRes = try {
            val resId = app.resources.getIdentifier("ic_oscellion_notification", "drawable", app.packageName)
            if (resId != 0) resId else app.applicationInfo.icon
        } catch (_: Exception) {
            app.applicationInfo.icon
        }

        val builder = NotificationCompat.Builder(app, CALL_CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setColor(Color.parseColor("#34C759"))
            .setContentTitle(callerName)
            .setContentText("Incoming $callLabel")
            .setLargeIcon(avatar)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(48_000L)
            .setContentIntent(fullScreenPi)
            .setDeleteIntent(dismissPi)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(callerPerson, declinePi, answerPi)
                    .setIsVideo(call.isVideo)
            )
            .addPerson(callerPerson)

        if (mode == CallPresentationMode.FULL_SCREEN) {
            ChatNotificationBuilder.wakeUpScreen(app)
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        nm.notify(notifId, notification)

        // When screen is off or locked, launch IncomingCallActivity directly as well as fullScreenIntent
        // so the full-screen Apple interface opens reliably across OEM lock-screen policies.
        if (mode == CallPresentationMode.FULL_SCREEN) {
            try {
                app.startActivity(fullScreenIntent)
            } catch (e: Throwable) {
                Log.w(TAG, "Direct startActivity from background skipped (relying on fullScreenIntent): ${e.message}")
            }
        }
    }

    fun cancelIncomingCallNotification(context: Context, callId: String) {
        if (callId.isBlank()) return
        val app = context.applicationContext
        CallRingtoneController.stop(callId)
        try {
            NotificationManagerCompat.from(app).cancel(notificationIdForCall(callId))
        } catch (_: Throwable) {}
        IncomingCallActivity.dismissIfShowing(callId)
    }

    fun buildActivityIntent(
        context: Context,
        call: CallModel,
        callerHandle: String = "",
        isVerified: Boolean = false,
        autoAnswer: Boolean = false
    ): Intent {
        return Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, call.id)
            putExtra(EXTRA_ROOM_ID, call.roomId)
            putExtra(EXTRA_CALLER_ID, call.callerId)
            putExtra(EXTRA_CALLER_NAME, call.callerName)
            putExtra(EXTRA_CALLER_AVATAR, call.callerAvatar)
            putExtra(EXTRA_CALLEE_ID, call.calleeId)
            putExtra(EXTRA_CALL_TYPE, call.type)
            putExtra(EXTRA_CALL_TIMESTAMP, call.timestamp)
            putExtra(EXTRA_CALLER_HANDLE, callerHandle)
            putExtra(EXTRA_CALLER_VERIFIED, isVerified)
            putExtra(EXTRA_AUTO_ANSWER, autoAnswer)
        }
    }

    private fun loadCircularAvatar(url: String, name: String): Bitmap {
        val size = 192
        if (url.startsWith("http")) {
            avatarCache.get(url)?.let { return it }
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    val circle = toCircle(bmp, size)
                    avatarCache.put(url, circle)
                    return circle
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        return initialAvatar(name, size)
    }

    private fun toCircle(src: Bitmap, size: Int): Bitmap {
        val min = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - min) / 2, (src.height - min) / 2, min, min)
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }

    private fun initialAvatar(name: String, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawCircle(
            size / 2f,
            size / 2f,
            size / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0A84FF") }
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.44f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "O"
        c.drawText(initial, size / 2f, size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
        return out
    }
}
