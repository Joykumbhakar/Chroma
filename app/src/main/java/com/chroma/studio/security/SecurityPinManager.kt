package com.chroma.studio.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.chroma.studio.chat.util.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Enterprise-grade Security & PIN Manager for Oscellion App Lock.
 * Manages 4-digit PIN setup, salt + SHA-256 hash storage, constant-time validation,
 * and session unlock lifecycle alongside Biometrics.
 */
object SecurityPinManager {
    private const val PREFS_NAME = "chroma_chat_security_prefs"

    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_PIN_SALT = "app_lock_pin_salt"
    private const val KEY_APP_LOCK_ENABLED = "biometric_passkey_lock"
    private const val KEY_CHAT_LOCK_ENABLED = "chat_screen_lock_enabled"
    private const val KEY_BIOMETRIC_PREF = "app_lock_biometric_enabled"

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns whether app lock is enabled in settings. */
    fun isAppLockEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) ||
               prefs.getBoolean(KEY_CHAT_LOCK_ENABLED, false)
    }

    /** Updates the app lock enabled state across preferences. */
    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .putBoolean(KEY_CHAT_LOCK_ENABLED, enabled)
            .apply()

        if (!enabled) {
            _isSessionUnlocked.value = true
        }
    }

    /** Returns true if a 4-digit security PIN is already configured. */
    fun hasPin(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hash = prefs.getString(KEY_PIN_HASH, null)
        val salt = prefs.getString(KEY_PIN_SALT, null)
        return !hash.isNullOrBlank() && !salt.isNullOrBlank()
    }

    /**
     * Hashes the 4-digit PIN with a cryptographically secure random salt using SHA-256
     * and securely persists it.
     */
    fun setPin(context: Context, pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        return try {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

            val hashBase64 = hashPin(pin, saltBase64)

            getPrefs(context).edit()
                .putString(KEY_PIN_HASH, hashBase64)
                .putString(KEY_PIN_SALT, saltBase64)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifies the entered PIN against the stored salt + hash using
     * constant-time byte comparison to prevent timing side-channel attacks.
     */
    fun verifyPin(context: Context, pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        return try {
            val computedHash = hashPin(pin, storedSalt)
            val storedBytes = Base64.decode(storedHash, Base64.NO_WRAP)
            val computedBytes = Base64.decode(computedHash, Base64.NO_WRAP)
            MessageDigest.isEqual(storedBytes, computedBytes)
        } catch (e: Exception) {
            false
        }
    }

    /** Removes the configured PIN. */
    fun removePin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    /** Checks whether the device hardware supports biometrics (fingerprint/face). */
    fun isBiometricSupported(context: Context): Boolean {
        return BiometricHelper.canAuthenticate(context)
    }

    /** Marks current app session as unlocked. */
    fun markUnlocked() {
        _isSessionUnlocked.value = true
    }

    /** Locks the current session (e.g. when app goes to background). */
    fun lockSession() {
        _isSessionUnlocked.value = false
    }

    private fun hashPin(pin: String, saltBase64: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        md.update(saltBytes)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
