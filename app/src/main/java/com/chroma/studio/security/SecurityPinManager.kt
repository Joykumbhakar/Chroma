package com.chroma.studio.security

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import com.chroma.studio.chat.util.BiometricHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
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
    private const val KEY_WALLPAPER_URI = "app_lock_wallpaper_uri"
    private const val KEY_WALLPAPER_DIMMING = "app_lock_wallpaper_dimming"
    private const val KEY_CUSTOM_WALLPAPERS = "app_lock_custom_wallpapers"
    private const val KEY_AUTO_LOCK_DELAY_MS = "app_lock_auto_lock_delay_ms"

    const val DEFAULT_WALLPAPER = "file:///android_asset/wallpaper/wallpaper_2.jpg"
    const val DEFAULT_DIMMING = 0.45f

    const val DELAY_IMMEDIATE_MS = 0L
    const val DELAY_15S_MS = 15_000L
    const val DELAY_1M_MS = 60_000L
    const val DELAY_5M_MS = 300_000L

    @Volatile
    var lastBackgroundAtMs: Long = 0L
        private set

    @Volatile
    private var suppressNextBackgroundLockUntilMs: Long = 0L

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private val _lockTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val lockTrigger: SharedFlow<Unit> = _lockTrigger.asSharedFlow()

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

    /** Returns whether Biometric unlock is enabled (defaults to true if hardware supported). */
    fun isBiometricEnabled(context: Context): Boolean {
        if (!isBiometricSupported(context)) return false
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_PREF, true)
    }

    /** Enables or disables Biometric unlock as an option alongside PIN. */
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_PREF, enabled).apply()
    }

    /** Marks current app session as unlocked. */
    fun markUnlocked() {
        lastBackgroundAtMs = 0L
        suppressNextBackgroundLockUntilMs = 0L
        _isSessionUnlocked.value = true
    }

    /** Temporarily exempts internal screen/picker transitions from triggering auto-lock. */
    fun markInternalNavigation(graceMs: Long = 3_000L) {
        suppressNextBackgroundLockUntilMs = System.currentTimeMillis() + graceMs
    }

    /** Records when the app entered the background. */
    fun markAppBackgrounded() {
        val now = System.currentTimeMillis()
        if (now < suppressNextBackgroundLockUntilMs) {
            return
        }
        lastBackgroundAtMs = now
    }

    /** Returns true if auto-lock timeout has elapsed since the app went to background. */
    fun shouldLockOnForegroundReturn(context: Context): Boolean {
        if (!isAppLockEnabled(context)) return false
        val bgAt = lastBackgroundAtMs
        if (bgAt <= 0L) return false
        val delayMs = getAutoLockDelayMs(context)
        return (System.currentTimeMillis() - bgAt) >= delayMs
    }

    /** Returns configured auto-lock timeout in milliseconds (0 = Immediately). */
    fun getAutoLockDelayMs(context: Context): Long {
        return getPrefs(context).getLong(KEY_AUTO_LOCK_DELAY_MS, DELAY_15S_MS)
    }

    /** Saves auto-lock timeout preference. */
    fun setAutoLockDelayMs(context: Context, delayMs: Long) {
        getPrefs(context).edit().putLong(KEY_AUTO_LOCK_DELAY_MS, delayMs).apply()
    }

    /** Human-readable label for configured auto-lock delay. */
    fun getAutoLockDelayLabel(delayMs: Long): String = when (delayMs) {
        DELAY_IMMEDIATE_MS -> "Immediately"
        DELAY_15S_MS -> "After 15 sec"
        DELAY_1M_MS -> "After 1 min"
        DELAY_5M_MS -> "After 5 min"
        else -> "After ${delayMs / 1000} sec"
    }

    /** Locks the current session immediately (e.g. Lock App Now). */
    fun lockSession() {
        lastBackgroundAtMs = System.currentTimeMillis()
        _isSessionUnlocked.value = false
        _lockTrigger.tryEmit(Unit)
    }

    /** Returns the active lock screen wallpaper URI. */
    fun getWallpaperUri(context: Context): String {
        return getPrefs(context).getString(KEY_WALLPAPER_URI, DEFAULT_WALLPAPER) ?: DEFAULT_WALLPAPER
    }

    /** Updates the active lock screen wallpaper URI. */
    fun setWallpaperUri(context: Context, uri: String) {
        getPrefs(context).edit().putString(KEY_WALLPAPER_URI, uri).apply()
    }

    /** Returns the active lock screen dimming level (0.0f - 0.85f). */
    fun getWallpaperDimming(context: Context): Float {
        return getPrefs(context).getFloat(KEY_WALLPAPER_DIMMING, DEFAULT_DIMMING)
    }

    /** Updates the active lock screen dimming level. */
    fun setWallpaperDimming(context: Context, dimming: Float) {
        getPrefs(context).edit().putFloat(KEY_WALLPAPER_DIMMING, dimming.coerceIn(0f, 0.85f)).apply()
    }

    /** Returns all imported custom wallpaper URIs (ordered from newest to oldest). */
    fun getCustomWallpapers(context: Context): List<String> {
        val list = mutableListOf<String>()
        val raw = getPrefs(context).getString(KEY_CUSTOM_WALLPAPERS, null)
        if (raw != null) {
            try {
                val jsonArray = JSONArray(raw)
                for (i in 0 until jsonArray.length()) {
                    val uri = jsonArray.getString(i)
                    if (uri.startsWith("file://")) {
                        val path = Uri.parse(uri).path
                        if (path != null && File(path).exists() && !list.contains(uri)) {
                            list.add(uri)
                        }
                    } else if (!list.contains(uri)) {
                        list.add(uri)
                    }
                }
            } catch (_: Exception) {}
        }

        // Also check if active wallpaper is a custom local file that wasn't tracked yet
        val activeWallpaper = getWallpaperUri(context)
        if (activeWallpaper.startsWith("file://") && !activeWallpaper.contains("android_asset")) {
            val path = Uri.parse(activeWallpaper).path
            if (path != null && File(path).exists() && !list.contains(activeWallpaper)) {
                list.add(0, activeWallpaper)
            }
        }

        // Auto-discover any saved custom wallpapers in filesDir that aren't yet in the list
        try {
            context.filesDir.listFiles { file ->
                file.name.startsWith("custom_wallpaper_") && file.name.endsWith(".jpg")
            }?.sortedByDescending { it.lastModified() }?.forEach { file ->
                val uri = Uri.fromFile(file).toString()
                if (!list.contains(uri)) {
                    list.add(uri)
                }
            }
        } catch (_: Exception) {}

        return list
    }

    /** Adds or promotes a custom wallpaper URI to the front of history. */
    fun addCustomWallpaper(context: Context, uri: String) {
        val current = getCustomWallpapers(context).toMutableList()
        current.remove(uri)
        current.add(0, uri)
        saveCustomWallpapersList(context, current)
    }

    /** Deletes an imported custom wallpaper from history and removes the file if stored locally. */
    fun deleteCustomWallpaper(context: Context, uri: String) {
        val current = getCustomWallpapers(context).toMutableList()
        current.remove(uri)
        saveCustomWallpapersList(context, current)

        // If active wallpaper was the deleted one, reset it to default
        if (getWallpaperUri(context) == uri) {
            setWallpaperUri(context, DEFAULT_WALLPAPER)
        }

        try {
            if (uri.startsWith("file://")) {
                val path = Uri.parse(uri).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.parentFile == context.filesDir) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveCustomWallpapersList(context: Context, list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        getPrefs(context).edit().putString(KEY_CUSTOM_WALLPAPERS, jsonArray.toString()).apply()
    }

    private fun hashPin(pin: String, saltBase64: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        md.update(saltBytes)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
