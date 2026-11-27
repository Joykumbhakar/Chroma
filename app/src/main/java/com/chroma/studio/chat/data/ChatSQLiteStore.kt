package com.chroma.studio.chat.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.google.gson.Gson

/**
 * Built-in WAL-mode SQLite persistence layer for WhatsApp-style instant chat loading.
 *
 * Design notes:
 * - Zero external dependencies (pure Android `SQLiteOpenHelper`), keeping `--offline` builds fast.
 * - Multi-account isolated by `uid` on every row and primary key.
 * - Non-blocking reads under Write-Ahead Logging (WAL).
 * - Automatically bypassed in bare-JVM unit tests (`ChatLocalCacheManager.isTestEnvironment()`)
 *   so tests use the JSON disk fallback without needing Robolectric.
 */
internal object ChatSQLiteStore {
    private const val TAG = "ChatSQLiteStore"
    private const val DB_NAME = "chroma_chat_store.db"
    private const val DB_VERSION = 1

    private const val TABLE_ROOMS = "chat_rooms"
    private const val TABLE_MESSAGES = "chat_messages"
    private const val TABLE_DECRYPTED = "decrypted_texts"

    @Volatile
    private var dbHelper: ChatDbHelper? = null

    @Volatile
    private var sqliteUnavailable: Boolean = false

    private class ChatDbHelper(context: Context) : SQLiteOpenHelper(
        context.applicationContext ?: context,
        DB_NAME,
        null,
        DB_VERSION
    ) {
        init {
            try {
                setWriteAheadLoggingEnabled(true)
            } catch (_: Throwable) {
            }
        }

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            try {
                db.enableWriteAheadLogging()
            } catch (_: Throwable) {
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_ROOMS (
                    uid TEXT NOT NULL,
                    id TEXT NOT NULL,
                    last_message_time INTEGER NOT NULL,
                    json TEXT NOT NULL,
                    PRIMARY KEY (uid, id)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_chat_rooms_uid_time ON $TABLE_ROOMS (uid, last_message_time DESC)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                    uid TEXT NOT NULL,
                    room_id TEXT NOT NULL,
                    id TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    json TEXT NOT NULL,
                    PRIMARY KEY (uid, id)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_chat_messages_room_time ON $TABLE_MESSAGES (uid, room_id, timestamp DESC)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_DECRYPTED (
                    uid TEXT NOT NULL,
                    cache_key TEXT NOT NULL,
                    plain_text TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (uid, cache_key)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Schema v1; preserve data cleanly on future migrations.
        }
    }

    fun isAvailable(context: Context): Boolean {
        if (sqliteUnavailable || ChatLocalCacheManager.isTestEnvironment()) return false
        return getHelper(context) != null
    }

    private fun getHelper(context: Context): ChatDbHelper? {
        if (sqliteUnavailable || ChatLocalCacheManager.isTestEnvironment()) return null
        dbHelper?.let { return it }
        return synchronized(this) {
            dbHelper?.let { return it }
            try {
                val appCtx = context.applicationContext ?: context
                val helper = ChatDbHelper(appCtx)
                // Probe writableDatabase lazily to confirm Android SQLite runtime is present
                helper.writableDatabase
                dbHelper = helper
                helper
            } catch (t: Throwable) {
                // Bare JVM unit tests throw RuntimeException("Method ... not mocked")
                sqliteUnavailable = true
                null
            }
        }
    }

    fun getRecentRooms(context: Context, uid: String, gson: Gson): List<ChatRoom>? {
        val db = try {
            getHelper(context)?.readableDatabase ?: return null
        } catch (_: Throwable) {
            return null
        }
        val result = ArrayList<ChatRoom>()
        try {
            db.rawQuery(
                "SELECT json FROM $TABLE_ROOMS WHERE uid = ? ORDER BY last_message_time DESC",
                arrayOf(uid)
            ).use { cursor ->
                val jsonCol = cursor.getColumnIndex("json")
                while (cursor.moveToNext()) {
                    val json = if (jsonCol >= 0) cursor.getString(jsonCol) else null
                    if (!json.isNullOrBlank()) {
                        try {
                            val room = gson.fromJson(json, ChatRoom::class.java)
                            if (room != null && uid in room.participants) {
                                result.add(room)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading recent rooms from SQLite", e)
            return null
        }
    }

    fun replaceUserRooms(context: Context, uid: String, rooms: List<ChatRoom>, gson: Gson): Boolean {
        val db = try {
            getHelper(context)?.writableDatabase ?: return false
        } catch (_: Throwable) {
            return false
        }
        val owned = rooms.filter { uid in it.participants && it.id.isNotBlank() }
        return try {
            db.beginTransactionNonExclusive()
            try {
                db.delete(TABLE_ROOMS, "uid = ?", arrayOf(uid))
                val cv = ContentValues(4)
                for (room in owned) {
                    cv.clear()
                    cv.put("uid", uid)
                    cv.put("id", room.id)
                    cv.put("last_message_time", room.lastMessageTime)
                    cv.put("json", gson.toJson(room))
                    db.insertWithOnConflict(TABLE_ROOMS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed replacing rooms in SQLite", e)
            false
        }
    }

    fun upsertRoom(context: Context, uid: String, room: ChatRoom, gson: Gson): Boolean {
        if (room.id.isBlank() || uid !in room.participants) return false
        val db = try {
            getHelper(context)?.writableDatabase ?: return false
        } catch (_: Throwable) {
            return false
        }
        return try {
            val cv = ContentValues(4).apply {
                put("uid", uid)
                put("id", room.id)
                put("last_message_time", room.lastMessageTime)
                put("json", gson.toJson(room))
            }
            db.insertWithOnConflict(TABLE_ROOMS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed upserting room ${room.id} in SQLite", e)
            false
        }
    }

    fun getMessages(
        context: Context,
        uid: String,
        roomId: String,
        limit: Int,
        beforeTimestamp: Long? = null,
        gson: Gson
    ): List<ChatMessage>? {
        val db = try {
            getHelper(context)?.readableDatabase ?: return null
        } catch (_: Throwable) {
            return null
        }
        val result = ArrayList<ChatMessage>()
        val sql: String
        val args: Array<String>
        if (beforeTimestamp != null) {
            sql = "SELECT json FROM $TABLE_MESSAGES WHERE uid = ? AND room_id = ? AND timestamp < ? ORDER BY timestamp DESC LIMIT ?"
            args = arrayOf(uid, roomId, beforeTimestamp.toString(), limit.coerceAtLeast(1).toString())
        } else {
            sql = "SELECT json FROM $TABLE_MESSAGES WHERE uid = ? AND room_id = ? ORDER BY timestamp DESC LIMIT ?"
            args = arrayOf(uid, roomId, limit.coerceAtLeast(1).toString())
        }
        return try {
            db.rawQuery(sql, args).use { cursor ->
                val jsonCol = cursor.getColumnIndex("json")
                while (cursor.moveToNext()) {
                    val json = if (jsonCol >= 0) cursor.getString(jsonCol) else null
                    if (!json.isNullOrBlank()) {
                        try {
                            val msg = gson.fromJson(json, ChatMessage::class.java)
                            if (msg != null) result.add(msg)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading messages from SQLite for room $roomId", e)
            null
        }
    }

    fun upsertMessages(
        context: Context,
        uid: String,
        roomId: String,
        messages: List<ChatMessage>,
        gson: Gson
    ): Boolean {
        if (messages.isEmpty()) return true
        val db = try {
            getHelper(context)?.writableDatabase ?: return false
        } catch (_: Throwable) {
            return false
        }
        return try {
            db.beginTransactionNonExclusive()
            try {
                val cv = ContentValues(5)
                for (msg in messages) {
                    if (msg.id.isBlank()) continue
                    cv.clear()
                    cv.put("uid", uid)
                    cv.put("room_id", roomId)
                    cv.put("id", msg.id)
                    cv.put("timestamp", msg.timestamp)
                    cv.put("json", gson.toJson(msg))
                    db.insertWithOnConflict(TABLE_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed upserting messages into SQLite for room $roomId", e)
            false
        }
    }

    fun removeMessage(context: Context, uid: String, messageId: String): Boolean {
        if (messageId.isBlank()) return false
        val db = try {
            getHelper(context)?.writableDatabase ?: return false
        } catch (_: Throwable) {
            return false
        }
        return try {
            db.delete(TABLE_MESSAGES, "uid = ? AND id = ?", arrayOf(uid, messageId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed removing message $messageId from SQLite", e)
            false
        }
    }

    fun getPlaintext(context: Context, uid: String, cacheKey: String): String? {
        if (cacheKey.isBlank()) return null
        val db = try {
            getHelper(context)?.readableDatabase ?: return null
        } catch (_: Throwable) {
            return null
        }
        return try {
            db.rawQuery(
                "SELECT plain_text FROM $TABLE_DECRYPTED WHERE uid = ? AND cache_key = ? LIMIT 1",
                arrayOf(uid, cacheKey)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun savePlaintext(context: Context, uid: String, cacheKey: String, plainText: String): Boolean {
        if (cacheKey.isBlank()) return false
        val db = try {
            getHelper(context)?.writableDatabase ?: return false
        } catch (_: Throwable) {
            return false
        }
        return try {
            val cv = ContentValues(4).apply {
                put("uid", uid)
                put("cache_key", cacheKey)
                put("plain_text", plainText)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_DECRYPTED, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearRoom(context: Context, uid: String, roomId: String) {
        val db = try {
            getHelper(context)?.writableDatabase ?: return
        } catch (_: Throwable) {
            return
        }
        try {
            db.delete(TABLE_MESSAGES, "uid = ? AND room_id = ?", arrayOf(uid, roomId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing room $roomId in SQLite", e)
        }
    }

    fun clearUser(context: Context, uid: String) {
        val db = try {
            getHelper(context)?.writableDatabase ?: return
        } catch (_: Throwable) {
            return
        }
        try {
            db.beginTransactionNonExclusive()
            try {
                db.delete(TABLE_ROOMS, "uid = ?", arrayOf(uid))
                db.delete(TABLE_MESSAGES, "uid = ?", arrayOf(uid))
                db.delete(TABLE_DECRYPTED, "uid = ?", arrayOf(uid))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing user $uid in SQLite", e)
        }
    }

    fun clearAll(context: Context) {
        val db = try {
            getHelper(context)?.writableDatabase ?: return
        } catch (_: Throwable) {
            return
        }
        try {
            db.beginTransactionNonExclusive()
            try {
                db.delete(TABLE_ROOMS, null, null)
                db.delete(TABLE_MESSAGES, null, null)
                db.delete(TABLE_DECRYPTED, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing SQLite store on logout", e)
        }
    }
}
