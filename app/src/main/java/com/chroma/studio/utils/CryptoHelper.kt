package com.chroma.studio.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val RSA_KEY_ALIAS = "OscellionRSAKey"
    private const val TAG = "CryptoHelper"

    @Volatile
    private var cachedPrivateKey: PrivateKey? = null
    @Volatile
    private var cachedPublicKey: PublicKey? = null
    private val publicKeyCache = java.util.concurrent.ConcurrentHashMap<String, PublicKey>()
    private val roomKeyCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    // Generate or retrieve RSA KeyPair in Android KeyStore
    @Synchronized
    fun getOrGenerateRSAKeyPair(): PublicKey {
        cachedPublicKey?.let { return it }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(RSA_KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    RSA_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
            )
            keyPairGenerator.generateKeyPair()
        }
        val certificate = keyStore.getCertificate(RSA_KEY_ALIAS)
        val pub = certificate.publicKey
        cachedPublicKey = pub
        return pub
    }

    // Get Base64 encoded public key to upload to Firestore
    fun getPublicKeyBase64(): String {
        return try {
            val publicKey = getOrGenerateRSAKeyPair()
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting public key", e)
            ""
        }
    }

    @Synchronized
    private fun getPrivateKey(): PrivateKey? {
        cachedPrivateKey?.let { return it }
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(RSA_KEY_ALIAS)) {
                getOrGenerateRSAKeyPair()
            }
            val priv = keyStore.getKey(RSA_KEY_ALIAS, null) as? PrivateKey
            cachedPrivateKey = priv
            priv
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving private key", e)
            null
        }
    }

    fun getPublicKeyFromString(base64Key: String): PublicKey {
        publicKeyCache[base64Key]?.let { return it }
        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactory.generatePublic(spec)
        publicKeyCache[base64Key] = key
        return key
    }

    // Encrypt AES Key with RSA Public Key
    fun encryptAESKeyWithRSA(aesKey: ByteArray, publicKeyBase64: String): String {
        return try {
            val publicKey = getPublicKeyFromString(publicKeyBase64)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(aesKey)
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "encryptAESKeyWithRSA failed", e)
            ""
        }
    }

    // Decrypt AES Key with local RSA Private Key
    fun decryptAESKeyWithRSA(encryptedAesKeyBase64: String): ByteArray? {
        return try {
            val privKey = getPrivateKey() ?: return null
            val encryptedBytes = Base64.decode(encryptedAesKeyBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privKey)
            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) {
            Log.w(TAG, "decryptAESKeyWithRSA failed: ${e.message}")
            null
        }
    }

    // Deterministic 256-bit room fallback key derived from Room ID
    fun deriveRoomKey(roomId: String): ByteArray {
        roomKeyCache[roomId]?.let { return it }
        val digest = MessageDigest.getInstance("SHA-256")
        val key = digest.digest((roomId + "_Oscellion_EndToEnd_v1_Salt").toByteArray(Charsets.UTF_8))
        roomKeyCache[roomId] = key
        return key
    }

    // Encrypt message with AES-GCM
    fun encryptMessageWithAES(message: String, secretKey: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    // Decrypt message with AES-GCM given raw AES key bytes
    fun decryptMessageWithAES(encryptedMessageBase64: String, ivBase64: String, aesKey: ByteArray): String {
        return try {
            val encryptedBytes = Base64.decode(encryptedMessageBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(aesKey, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    // Generate a random AES key
    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
}
