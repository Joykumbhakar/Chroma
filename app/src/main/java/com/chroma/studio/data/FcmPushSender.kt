package com.chroma.studio.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

enum class FcmResult { SENT, FAILED, UNREGISTERED }

/**
 * Direct FCM HTTP v1 push sender with mutex-locked token minting,
 * in-memory token caching with refresh, and high-priority heads-up push dispatch.
 */
object FcmPushSender {
    private const val TAG = "FcmPushSender"

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiryEpochMs: Long = 0L
    private val tokenMutex = Mutex()

    private var clientEmail: String? = null
    private var privateKey: PrivateKey? = null
    private var projectId: String = "chromamelt"

    @Synchronized
    fun init(context: Context) {
        if (privateKey != null) return
        try {
            val jsonString = try {
                context.assets.open("serviceAccountKey.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.d(TAG, "serviceAccountKey.json not bundled; local push dispatch disabled.")
                return
            }
            val json = JSONObject(jsonString)
            projectId = json.optString("project_id", "chromamelt")
            clientEmail = json.getString("client_email")

            val cleanPem = json.getString("private_key")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")
                .trim()

            val keySpec = PKCS8EncodedKeySpec(Base64.decode(cleanPem, Base64.DEFAULT))
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            Log.d(TAG, "Initialized FCM service account for $clientEmail (project: $projectId)")
        } catch (e: Exception) {
            Log.w(TAG, "FCM service account credentials not loaded: ${e.message}")
        }
    }

    /** Returns a cached OAuth2 token, minting a new one (once, even under parallel sends) if needed. */
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fast = cachedToken
        if (fast != null && now < tokenExpiryEpochMs - 60_000) return@withContext fast

        tokenMutex.withLock {
            val n = System.currentTimeMillis()
            val c = cachedToken
            if (c != null && n < tokenExpiryEpochMs - 60_000) c else mintToken(context, n)
        }
    }

    private fun b64Url(bytes: ByteArray) =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun mintToken(context: Context, now: Long): String? {
        init(context)
        val email = clientEmail ?: return null
        val pk = privateKey ?: return null
        var conn: HttpURLConnection? = null
        try {
            val nowSec = now / 1000
            val header = JSONObject().put("alg", "RS256").put("typ", "JWT")
            val claim = JSONObject()
                .put("iss", email).put("sub", email)
                .put("aud", "https://oauth2.googleapis.com/token")
                .put("iat", nowSec).put("exp", nowSec + 3600)
                .put("scope", "https://www.googleapis.com/auth/firebase.messaging")

            val unsigned = b64Url(header.toString().toByteArray()) + "." + b64Url(claim.toString().toByteArray())
            val sig = Signature.getInstance("SHA256withRSA").apply {
                initSign(pk); update(unsigned.toByteArray(Charsets.UTF_8))
            }.sign()
            val jwt = unsigned + "." + b64Url(sig)

            conn = URL("https://oauth2.googleapis.com/token").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val post = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                    "&assertion=" + URLEncoder.encode(jwt, "UTF-8")
            OutputStreamWriter(conn.outputStream).use { it.write(post) }

            return if (conn.responseCode == 200) {
                val resp = JSONObject(BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() })
                cachedToken = resp.getString("access_token")
                tokenExpiryEpochMs = now + resp.optLong("expires_in", 3600L) * 1000
                cachedToken
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Log.e(TAG, "OAuth2 token exchange failed (${conn.responseCode}): $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error minting OAuth token", e)
            return null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Sends a DATA-ONLY, high-priority message. Data-only is deliberate: it guarantees
     * onMessageReceived runs (app killed / screen locked) so ChatNotificationBuilder can build the
     * MessagingStyle heads-up with Direct Reply.
     */
    suspend fun sendPushNotification(
        context: Context,
        targetFcmToken: String,
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        messageType: String = "text",
        groupName: String = "",
        messageId: String = ""
    ): FcmResult = withContext(Dispatchers.IO) {
        if (targetFcmToken.isBlank()) return@withContext FcmResult.FAILED
        val token = getAccessToken(context) ?: return@withContext FcmResult.FAILED

        val body = when {
            messageText.isNotBlank() -> messageText
            messageType == "audio" || messageType == "voice" -> "🎤 Voice message"
            messageType == "video" -> "🎥 Sent a video"
            messageType == "image" -> "📷 Sent a photo"
            messageType == "file" -> "📎 Sent a file"
            else -> "New message"
        }

        var conn: HttpURLConnection? = null
        try {
            conn = URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val payload = JSONObject().put("message", JSONObject().apply {
                put("token", targetFcmToken)
                put("data", JSONObject().apply {
                    put("roomId", roomId)
                    put("senderId", senderId)
                    put("senderName", senderName)
                    put("groupName", groupName)
                    put("title", if (groupName.isNotBlank()) "$senderName in $groupName" else senderName)
                    put("body", body)
                    put("message", body)
                    put("avatarUrl", avatarUrl)
                    put("messageType", messageType)
                    put("messageId", messageId)
                    put("sentAt", System.currentTimeMillis().toString())
                })
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                    put("ttl", "86400s") // Retry up to 24h if device is offline
                    put("direct_boot_ok", true)
                    put("fcm_options", JSONObject().put("analytics_label", "chat_message"))
                })
            })

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }

            val code = conn.responseCode
            if (code == 200) return@withContext FcmResult.SENT

            val err = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
            Log.w(TAG, "FCM push returned $code: $err")
            if (code == 404 && err.contains("UNREGISTERED")) FcmResult.UNREGISTERED else FcmResult.FAILED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send FCM push", e)
            FcmResult.FAILED
        } finally {
            conn?.disconnect()
        }
    }
}
