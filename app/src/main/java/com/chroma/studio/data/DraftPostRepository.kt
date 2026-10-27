package com.chroma.studio.data

import android.content.Context
import com.chroma.studio.model.CreatorPostDraft
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Thread-safe local repository for saving, loading, and managing creator post drafts across
 * Post, Flash, and Work formats. Persists drafts to SharedPreferences in JSON format.
 */
class DraftPostRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _draftsFlow = MutableStateFlow<List<CreatorPostDraft>>(emptyList())
    val draftsFlow: StateFlow<List<CreatorPostDraft>> = _draftsFlow.asStateFlow()

    init {
        _draftsFlow.value = getAllDrafts()
    }

    @Synchronized
    fun getAllDrafts(): List<CreatorPostDraft> {
        val json = prefs.getString(KEY_DRAFTS_JSON, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CreatorPostDraft>>() {}.type
            val list: List<CreatorPostDraft>? = gson.fromJson(json, type)
            list?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getDraftById(id: String): CreatorPostDraft? {
        return getAllDrafts().firstOrNull { it.id == id }
    }

    @Synchronized
    fun saveDraft(draft: CreatorPostDraft) {
        val currentList = getAllDrafts().toMutableList()
        currentList.removeAll { it.id == draft.id }
        currentList.add(0, draft.copy(timestamp = System.currentTimeMillis()))

        val json = gson.toJson(currentList)
        prefs.edit().putString(KEY_DRAFTS_JSON, json).apply()
        _draftsFlow.value = currentList
    }

    @Synchronized
    fun deleteDraft(id: String) {
        val currentList = getAllDrafts().toMutableList()
        currentList.removeAll { it.id == id }

        val json = gson.toJson(currentList)
        prefs.edit().putString(KEY_DRAFTS_JSON, json).apply()
        _draftsFlow.value = currentList
    }

    @Synchronized
    fun renameDraft(id: String, newTitle: String): Boolean {
        val currentList = getAllDrafts().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = currentList[index].copy(
                title = newTitle,
                timestamp = System.currentTimeMillis()
            )
            currentList[index] = updated
            val json = gson.toJson(currentList)
            prefs.edit().putString(KEY_DRAFTS_JSON, json).apply()
            _draftsFlow.value = currentList
            return true
        }
        return false
    }

    @Synchronized
    fun duplicateDraft(id: String): CreatorPostDraft? {
        val currentList = getAllDrafts().toMutableList()
        val target = currentList.firstOrNull { it.id == id } ?: return null
        val copy = target.copy(
            id = UUID.randomUUID().toString(),
            title = "${target.title.ifBlank { "Untitled Project" }} (Copy)",
            timestamp = System.currentTimeMillis()
        )
        currentList.add(0, copy)
        val json = gson.toJson(currentList)
        prefs.edit().putString(KEY_DRAFTS_JSON, json).apply()
        _draftsFlow.value = currentList
        return copy
    }

    @Synchronized
    fun clearAllDrafts() {
        prefs.edit().remove(KEY_DRAFTS_JSON).apply()
        _draftsFlow.value = emptyList()
    }

    companion object {
        private const val PREFS_NAME = "creator_post_drafts"
        private const val KEY_DRAFTS_JSON = "drafts_json_v1"
    }
}
