package com.chroma.studio.chat.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Manages 4-digit PIN security, authentication, and hidden chat IDs for the Hidden Chats Vault.
 */
object HiddenChatSecurityManager {
    private const val PREFS_NAME = "chroma_chat_security_prefs"
    private const val KEY_PIN_HASH = "chat_vault_pin_hash"
    private const val KEY_PIN_SALT = "chat_vault_pin_salt"
    private const val KEY_HIDDEN_CHAT_IDS = "hidden_chat_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a 4-digit PIN has been created.
     */
    fun isPinSet(context: Context): Boolean {
        val hash = getPrefs(context).getString(KEY_PIN_HASH, null)
        return !hash.isNullOrBlank()
    }

    /**
     * Sets or updates the 4-digit PIN with salt and SHA-256 hash.
     */
    fun setPin(context: Context, pin: String) {
        val salt = java.util.UUID.randomUUID().toString()
        val hash = hashPin(pin, salt)
        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    /**
     * Verifies the entered 4-digit PIN against the stored salted hash.
     */
    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val computedHash = hashPin(enteredPin, storedSalt)
        return computedHash == storedHash
    }

    /**
     * Returns the set of conversation IDs marked as hidden.
     */
    fun getHiddenChatIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_HIDDEN_CHAT_IDS, emptySet())?.toSet() ?: emptySet()
    }

    /**
     * Adds a chat ID to the hidden set.
     */
    fun hideChat(context: Context, chatId: String) {
        val current = getHiddenChatIds(context).toMutableSet()
        current.add(chatId)
        getPrefs(context).edit().putStringSet(KEY_HIDDEN_CHAT_IDS, current).apply()
    }

    /**
     * Removes a chat ID from the hidden set (unhiding it back to the main list).
     */
    fun unhideChat(context: Context, chatId: String) {
        val current = getHiddenChatIds(context).toMutableSet()
        current.remove(chatId)
        getPrefs(context).edit().putStringSet(KEY_HIDDEN_CHAT_IDS, current).apply()
    }

    /**
     * Hides multiple chats at once (e.g. from selection mode).
     */
    fun hideMultipleChats(context: Context, chatIds: Collection<String>) {
        val current = getHiddenChatIds(context).toMutableSet()
        current.addAll(chatIds)
        getPrefs(context).edit().putStringSet(KEY_HIDDEN_CHAT_IDS, current).apply()
    }

    /**
     * Clears PIN (for reset).
     */
    fun resetPin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((pin + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
