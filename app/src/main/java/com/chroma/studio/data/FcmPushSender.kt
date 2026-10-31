package com.chroma.studio.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
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

/**
 * Direct Firebase Cloud Messaging (FCM HTTP v1) Push Dispatcher.
 * Automatically handles Google Service Account OAuth2 token minting,
 * in-memory token caching with refresh, and high-priority heads-up push dispatch.
 */
object FcmPushSender {
    private const val TAG = "FcmPushSender"

    private var cachedToken: String? = null
    private var tokenExpiryEpochMs: Long = 0L

    private var clientEmail: String? = null
    private var privateKey: PrivateKey? = null
    private var projectId: String = "chromamelt"

    /**
     * Initializes service account credentials from asset bundle.
     */
    @Synchronized
    fun init(context: Context) {
        if (privateKey != null) return
        try {
            val jsonString = context.assets.open("serviceAccountKey.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            projectId = json.optString("project_id", "chromamelt")
            clientEmail = json.getString("client_email")

            val rawPem = json.getString("private_key")
            val cleanPem = rawPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")
                .trim()

            val decodedKey = Base64.decode(cleanPem, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(decodedKey)
            val keyFactory = KeyFactory.getInstance("RSA")
            privateKey = keyFactory.generatePrivate(keySpec)
            Log.d(TAG, "Initialized FCM service account for $clientEmail (project: $projectId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load service account credentials", e)
        }
    }

    /**
     * Obtains a valid Google OAuth2 Bearer token, minting or refreshing if expired.
     */
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < (tokenExpiryEpochMs - 60_000)) {
            return@withContext cachedToken
        }

        init(context)
        val email = clientEmail ?: return@withContext null
        val pk = privateKey ?: return@withContext null

        try {
            val nowSec = now / 1000
            val expSec = nowSec + 3600

            // Header
            val headerJson = JSONObject().apply {
                put("alg", "RS256")
                put("typ", "JWT")
            }
            val headerEnc = Base64.encodeToString(
                headerJson.toString().toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            // Claim set
            val claimJson = JSONObject().apply {
                put("iss", email)
                put("sub", email)
                put("aud", "https://oauth2.googleapis.com/token")
                put("iat", nowSec)
                put("exp", expSec)
                put("scope", "https://www.googleapis.com/auth/firebase.messaging")
            }
            val claimEnc = Base64.encodeToString(
                claimJson.toString().toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val unsignedToken = "$headerEnc.$claimEnc"

            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(pk)
            signer.update(unsignedToken.toByteArray(Charsets.UTF_8))
            val sigBytes = signer.sign()
            val sigEnc = Base64.encodeToString(
                sigBytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val signedJwt = "$unsignedToken.$sigEnc"

            // Exchange assertion for access token
            val url = URL("https://oauth2.googleapis.com/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val postData = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                    "&assertion=" + URLEncoder.encode(signedJwt, "UTF-8")

            OutputStreamWriter(conn.outputStream).use { it.write(postData) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val respJson = JSONObject(responseStr)
                val token = respJson.getString("access_token")
                val expiresInSec = respJson.optLong("expires_in", 3600L)
                cachedToken = token
                tokenExpiryEpochMs = now + (expiresInSec * 1000)
                Log.d(TAG, "Minted new FCM OAuth2 bearer token successfully")
                return@withContext token
            } else {
                val errStr = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Log.e(TAG, "OAuth2 token exchange failed ($responseCode): $errStr")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error minting OAuth token", e)
            return@withContext null
        }
    }

    /**
     * Sends an FCM HTTP v1 message to [targetFcmToken].
     */
    suspend fun sendPushNotification(
        context: Context,
        targetFcmToken: String,
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        if (targetFcmToken.isBlank()) return@withContext false
        val token = getAccessToken(context) ?: return@withContext false

        try {
            val url = URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; UTF-8")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val messagePayload = JSONObject().apply {
                val msg = JSONObject().apply {
                    put("token", targetFcmToken)
                    // High-priority data-only payload ensures onMessageReceived is ALWAYS triggered
                    // even when app is closed or on lock screen, allowing ChatNotificationBuilder
                    // to execute PowerManager screen wakeup, MessagingStyle, and Direct Reply.
                    put("data", JSONObject().apply {
                        put("roomId", roomId)
                        put("senderId", senderId)
                        put("senderName", senderName)
                        put("title", senderName)
                        put("body", messageText)
                        put("message", messageText)
                        put("avatarUrl", avatarUrl)
                    })
                    put("android", JSONObject().apply {
                        put("priority", "HIGH")
                    })
                }
                put("message", msg)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(messagePayload.toString())
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "FCM message successfully dispatched to $targetFcmToken for room $roomId")
                return@withContext true
            } else {
                val errStr = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                Log.w(TAG, "FCM push dispatch returned code $responseCode: $errStr")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send FCM push to $targetFcmToken", e)
            return@withContext false
        }
    }
}
