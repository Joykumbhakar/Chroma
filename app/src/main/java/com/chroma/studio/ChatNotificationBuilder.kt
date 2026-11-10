package com.chroma.studio

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * WhatsApp-style chat notifications:
 *  - heads-up popup (IMPORTANCE_HIGH channel) that also shows on the lock screen
 *  - screen wakeup when the device is locked/sleeping so popup is immediately visible
 *  - MessagingStyle: messages from the same chat STACK in one notification
 *  - circular sender avatar (falls back to a coloured initial)
 *  - Reply (inline, RemoteInput) + Mark as read actions
 *  - conversation shortcut -> shows in the "Conversations" section on Android 11+
 *  - bundle summary when several chats are active
 *  - locked chats show "New message" only, no preview and no Reply
 *  - sent replies update the notification in place (no endless spinner)
 */
object ChatNotificationBuilder {
    private const val TAG = "ChatNotificationBuilder"

    // Bump the version if you ever change channel importance/sound: channels are immutable.
    const val CHANNEL_ID = "chat_messages_v2"
    private const val GROUP_KEY = "com.chroma.studio.CHAT_MESSAGES"
    private const val SUMMARY_ID = 0x7001
    private const val MAX_STACKED = 10

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_ROOM_ID = "roomId"
    const val EXTRA_SENDER_ID = "senderId"
    const val ACTION_REPLY = "com.chroma.studio.action.CHAT_REPLY"
    const val ACTION_MARK_READ = "com.chroma.studio.action.CHAT_MARK_READ"
    const val ACTION_DISMISSED = "com.chroma.studio.action.CHAT_DISMISSED"
    const val ACTION_QUICK_REPLY = "com.chroma.studio.action.ACTION_QUICK_REPLY"
    const val EXTRA_QUICK_REPLY_TEXT = "EXTRA_QUICK_REPLY_TEXT"

    private data class Entry(val sender: String, val senderId: String, val text: String, val ts: Long, val self: Boolean)
    private data class Meta(val senderName: String, val senderId: String, val groupName: String, val avatarUrl: String, val locked: Boolean)

    private val avatarCache = LruCache<String, Bitmap>(16)

    // ------------------------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------------------------

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "New chat messages"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableLights(true)
            lightColor = Color.parseColor("#7C4DFF")
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(channel)
    }

    /** Compatibility alias for ChromaApp lifecycle calls */
    fun ensureNotificationChannel(context: Context) = ensureChannel(context)

    /**
     * Wakes up the screen when the device is locked/asleep so that incoming notifications
     * are immediately visible on the lock screen as a heads-up popup.
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
                    "oscellion:chat_notification_wake"
                )
                wakeLock.acquire(3000)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Screen wakeup failed: ${e.message}")
        }
    }

    /** Call from onMessageReceived when the app is in background / killed / screen locked. */
    fun showHeadsUpNotification(
        context: Context,
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String,
        isLocked: Boolean = false,
        groupName: String = "",
        messageType: String = "text"
    ) {
        val app = context.applicationContext
        val key = roomId.ifBlank { senderId }
        if (key.isBlank()) return
        ensureChannel(app)
        wakeUpScreen(app)

        val meta = Meta(senderName, senderId, groupName, avatarUrl, isLocked)
        val text = if (isLocked) "New message" else messageText
        val entries = Store.append(app, key, meta, Entry(senderName, senderId, text, System.currentTimeMillis(), false))
        post(app, key, roomId, meta, entries, silent = false)
    }

    /**
     * Called after an inline reply was sent (or failed) so the notification stops showing the
     * progress spinner and shows "You: <reply>" like WhatsApp.
     */
    fun appendOwnReply(context: Context, roomId: String, senderId: String, text: String, failed: Boolean = false) {
        val app = context.applicationContext
        val key = roomId.ifBlank { senderId }
        val meta = Store.loadMeta(app, key) ?: return
        val shown = if (failed) "⚠ Not sent: $text" else text
        val entries = Store.append(app, key, meta, Entry("You", "self", shown, System.currentTimeMillis(), true))
        post(app, key, roomId, meta, entries, silent = true)
    }

    /** Call when the user opens the chat, taps Mark as read, etc. */
    fun cancelForRoom(context: Context, roomId: String, senderId: String = "") {
        val app = context.applicationContext
        val key = roomId.ifBlank { senderId }
        if (key.isBlank()) return
        Store.clear(app, key)
        val nm = NotificationManagerCompat.from(app)
        nm.cancel(key.hashCode())
        removeSummaryIfEmpty(app)
    }

    fun clearHistory(context: Context, key: String) = Store.clear(context.applicationContext, key)

    // ------------------------------------------------------------------------------------------
    // Posting
    // ------------------------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun post(context: Context, key: String, roomId: String, meta: Meta, entries: List<Entry>, silent: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted - notification skipped")
            return
        }
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val notifId = key.hashCode()
        val avatar = if (meta.locked) null else loadAvatar(meta.avatarUrl, meta.senderName)
        val isGroup = meta.groupName.isNotBlank()

        val me = Person.Builder().setName("You").setKey("self").build()
        val senderPerson = Person.Builder()
            .setName(meta.senderName)
            .setKey(meta.senderId.ifBlank { key })
            .apply { avatar?.let { setIcon(IconCompat.createWithBitmap(it)) } }
            .build()

        val style = NotificationCompat.MessagingStyle(me)
        if (isGroup) {
            style.conversationTitle = meta.groupName
            style.isGroupConversation = true
        }
        entries.forEach { e ->
            val p = when {
                e.self -> me
                e.senderId == meta.senderId -> senderPerson
                else -> Person.Builder().setName(e.sender).setKey(e.senderId).build()
            }
            style.addMessage(e.text, e.ts, p)
        }

        // Conversation shortcut -> "Conversations" section + better lock-screen treatment (API 30+)
        val launch = launchIntent(context, roomId, meta.senderId)
        val shortcutId = "chat_$key"
        try {
            val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(if (isGroup) meta.groupName else meta.senderName)
                .setLongLived(true)
                .setIntent(Intent(launch).apply { action = Intent.ACTION_VIEW })
                .setPerson(senderPerson)
                .apply { avatar?.let { setIcon(IconCompat.createWithAdaptiveBitmap(it)) } }
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Exception) {
            Log.w(TAG, "Shortcut push failed: ${e.message}")
        }

        val contentPi = PendingIntent.getActivity(
            context, notifId, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPi = receiverPi(context, ACTION_DISMISSED, key, roomId, meta.senderId, mutable = false)
        val markReadPi = receiverPi(context, ACTION_MARK_READ, key, roomId, meta.senderId, mutable = false)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon(context))
            .setContentTitle(meta.senderName)
            .setContentText(if (meta.locked) "New message" else entries.lastOrNull()?.text ?: "New message")
            .setColor(Color.parseColor("#7C4DFF"))
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon(context))
            .setColor(Color.parseColor("#7C4DFF"))
            .setStyle(style)
            .setContentTitle(if (isGroup) meta.groupName else meta.senderName)
            .setContentText(entries.lastOrNull()?.text ?: "")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(if (meta.locked) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicVersion)
            .setShortcutId(shortcutId)
            .setWhen(entries.lastOrNull()?.ts ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(silent)
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(contentPi)
            .setDeleteIntent(dismissPi)
            .apply { avatar?.let { setLargeIcon(it) } }

        if (!meta.locked) {
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply").build()
            val replyPi = receiverPi(context, ACTION_REPLY, key, roomId, meta.senderId, mutable = true)
            builder.addAction(
                NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "Reply", replyPi)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setShowsUserInterface(false)
                    .build()
            )
        }
        builder.addAction(
            NotificationCompat.Action.Builder(android.R.drawable.ic_menu_view, "Mark as read", markReadPi)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build()
        )

        nm.notify(notifId, builder.build())

        // Summary so multiple chats bundle neatly (required for grouping below Android 7 / some OEMs)
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon(context))
            .setColor(Color.parseColor("#7C4DFF"))
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("New messages"))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()
        nm.notify(SUMMARY_ID, summary)
    }

    private fun removeSummaryIfEmpty(context: Context) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val children = nm.activeNotifications.count { it.id != SUMMARY_ID && it.notification.group == GROUP_KEY }
            if (children == 0) NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    /**
     * Sharp monochrome notification icon designed specifically for notifications.
     */
    private fun smallIcon(context: Context): Int {
        return try {
            val resId = context.resources.getIdentifier("ic_oscellion_notification", "drawable", context.packageName)
            if (resId != 0) resId else context.applicationInfo.icon
        } catch (_: Exception) {
            context.applicationInfo.icon
        }
    }

    private fun launchIntent(context: Context, roomId: String, senderId: String): Intent {
        val base = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        return base.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_SENDER_ID, senderId)
            putExtra("from_notification", true)
        }
    }

    private fun receiverPi(
        context: Context, action: String, key: String, roomId: String, senderId: String, mutable: Boolean
    ): PendingIntent {
        val intent = Intent(context, ChatActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_SENDER_ID, senderId)
        }
        // RemoteInput PendingIntents MUST be mutable on Android 12+.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or when {
            mutable && Build.VERSION.SDK_INT >= 31 -> PendingIntent.FLAG_MUTABLE
            !mutable -> PendingIntent.FLAG_IMMUTABLE
            else -> 0
        }
        return PendingIntent.getBroadcast(context, key.hashCode() * 31 + action.hashCode(), intent, flags)
    }

    // ---- Avatar --------------------------------------------------------------------------------

    private fun loadAvatar(url: String, name: String): Bitmap {
        val size = 192
        if (url.startsWith("http")) {
            avatarCache.get(url)?.let { return it }
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    val circle = toCircle(bmp, size)
                    avatarCache.put(url, circle)
                    return circle
                }
            } catch (e: Exception) {
                Log.w(TAG, "Avatar load failed: ${e.message}")
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
        val palette = intArrayOf(
            0xFF7C4DFF.toInt(), 0xFF00B8D4.toInt(), 0xFFFF6D00.toInt(),
            0xFF00C853.toInt(), 0xFFD500F9.toInt(), 0xFFFF4081.toInt()
        )
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette[(name.hashCode() and 0x7fffffff) % palette.size]
        })
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = size * 0.46f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        c.drawText(initial, size / 2f, size / 2f - (text.descent() + text.ascent()) / 2f, text)
        return out
    }

    // ---- Persistent stack of recent messages per chat (survives process death) ---------------------

    private object Store {
        private const val PREFS = "chat_notif_store"

        @Synchronized
        fun append(context: Context, key: String, meta: Meta, entry: Entry): List<Entry> {
            val list = loadEntries(context, key).toMutableList()
            list.add(entry)
            while (list.size > MAX_STACKED) list.removeAt(0)
            val obj = JSONObject().apply {
                put("meta", JSONObject().apply {
                    put("senderName", meta.senderName); put("senderId", meta.senderId)
                    put("groupName", meta.groupName); put("avatarUrl", meta.avatarUrl); put("locked", meta.locked)
                })
                put("entries", JSONArray().apply {
                    list.forEach {
                        put(JSONObject().put("s", it.sender).put("i", it.senderId)
                            .put("t", it.text).put("ts", it.ts).put("self", it.self))
                    }
                })
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, obj.toString()).apply()
            return list
        }

        @Synchronized
        fun loadMeta(context: Context, key: String): Meta? = try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
            raw?.let {
                val m = JSONObject(it).getJSONObject("meta")
                Meta(m.getString("senderName"), m.getString("senderId"), m.getString("groupName"),
                    m.getString("avatarUrl"), m.getBoolean("locked"))
            }
        } catch (_: Exception) { null }

        private fun loadEntries(context: Context, key: String): List<Entry> = try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
            if (raw == null) emptyList() else {
                val arr = JSONObject(raw).getJSONArray("entries")
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    Entry(o.getString("s"), o.getString("i"), o.getString("t"), o.getLong("ts"), o.getBoolean("self"))
                }
            }
        } catch (_: Exception) { emptyList() }

        @Synchronized
        fun clear(context: Context, key: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
        }
    }
}
