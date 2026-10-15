package com.chroma.studio.data

import android.content.Context
import android.util.Log
import com.chroma.studio.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SavedAccount(
    val uid: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String = "",
    val email: String = "",
    val gender: String = "",
    val lastLoginMillis: Long = System.currentTimeMillis()
)

object SavedAccountManager {

    private const val PREFS_NAME = "chroma_saved_accounts"
    private const val KEY_ACCOUNTS_JSON = "saved_accounts_list"
    private val gson = Gson()

    fun getSavedAccounts(context: Context): List<SavedAccount> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return emptyList()
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            val list: List<SavedAccount> = gson.fromJson(json, type) ?: emptyList()
            list.sortedByDescending { it.lastLoginMillis }
        } catch (e: Exception) {
            Log.e("SavedAccountManager", "Error reading saved accounts", e)
            emptyList()
        }
    }

    fun saveAccount(context: Context, account: SavedAccount) {
        try {
            val currentList = getSavedAccounts(context).filter { it.uid != account.uid }.toMutableList()
            currentList.add(0, account.copy(lastLoginMillis = System.currentTimeMillis()))
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ACCOUNTS_JSON, gson.toJson(currentList)).apply()
        } catch (e: Exception) {
            Log.e("SavedAccountManager", "Error saving account", e)
        }
    }

    fun saveFromProfile(context: Context, profile: UserProfile, email: String? = null) {
        if (profile.uid.isBlank()) return
        val saved = SavedAccount(
            uid = profile.uid,
            displayName = profile.displayName.ifBlank { "User" },
            username = profile.username,
            avatarUrl = profile.avatarUrl,
            email = email ?: "",
            gender = profile.gender,
            lastLoginMillis = System.currentTimeMillis()
        )
        saveAccount(context, saved)
    }

    fun removeAccount(context: Context, uid: String) {
        try {
            val updated = getSavedAccounts(context).filter { it.uid != uid }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ACCOUNTS_JSON, gson.toJson(updated)).apply()
        } catch (e: Exception) {
            Log.e("SavedAccountManager", "Error removing account", e)
        }
    }

    fun clearAllAccounts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ACCOUNTS_JSON).apply()
    }
}
