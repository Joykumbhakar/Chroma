package com.chroma.studio.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkRepository(application)

    val works = mutableStateListOf<ChromaWork>()

    init {
        refresh()
    }

    fun refresh() {
        works.clear()
        works.addAll(repository.loadAll())
    }

    fun delete(id: String) {
        repository.delete(id)
        works.removeIf { it.id == id }
    }

    fun rename(id: String, name: String, description: String) {
        val idx = works.indexOfFirst { it.id == id }
        if (idx < 0) return
        val updated = works[idx].copy(
            name = name.ifBlank { "Untitled" },
            description = description,
            lastModifiedAt = System.currentTimeMillis()
        )
        repository.save(updated)
        works[idx] = updated
    }
}
