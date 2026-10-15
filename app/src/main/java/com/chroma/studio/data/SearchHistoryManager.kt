package com.chroma.studio.data

import android.content.Context
import android.content.SharedPreferences
import com.chroma.studio.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val HISTORY_KEY = "user_search_history"

    fun getSearchHistory(): List<UserProfile> {
        val json = prefs.getString(HISTORY_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun addSearch(user: UserProfile) {
        val currentHistory = getSearchHistory().toMutableList()
        // Remove if it already exists to move it to top
        currentHistory.removeAll { it.uid == user.uid }
        currentHistory.add(0, user) // Add to top
        
        // Keep max 20 history items
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        saveHistory(currentHistory)
    }

    fun removeSearch(uid: String) {
        val currentHistory = getSearchHistory().toMutableList()
        currentHistory.removeAll { it.uid == uid }
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        prefs.edit().remove(HISTORY_KEY).apply()
    }

    private fun saveHistory(history: List<UserProfile>) {
        val json = gson.toJson(history)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
}
