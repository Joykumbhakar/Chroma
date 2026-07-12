package com.chroma.studio.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = WorkRepository(application)

    val works = mutableStateListOf<ChromaWork>()
    val deletedWorks = mutableStateListOf<ChromaWork>()

    var selectedWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val selectionMode: Boolean
        get() = selectedWorkIds.isNotEmpty()

    // Trash Selection state
    var trashSelectedWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val trashSelectionMode: Boolean
        get() = trashSelectedWorkIds.isNotEmpty()
        
    // Dark mode state
    var isDarkMode by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        works.clear()
        works.addAll(repository.loadActiveWorks())
        
        deletedWorks.clear()
        deletedWorks.addAll(repository.loadDeletedWorks())
    }
    
    fun updateDarkMode(dark: Boolean) {
        isDarkMode = dark
    }

    // --- Selection ---
    fun toggleSelection(id: String) {
        val newSet = selectedWorkIds.toMutableSet()
        if (newSet.contains(id)) {
            newSet.remove(id)
        } else {
            newSet.add(id)
        }
        selectedWorkIds = newSet
    }

    fun clearSelection() {
        selectedWorkIds = emptySet()
    }

    fun softDeleteSelected() {
        selectedWorkIds.forEach { id ->
            repository.softDelete(id)
        }
        clearSelection()
        refresh()
    }

    fun permanentDeleteSelected() {
        selectedWorkIds.forEach { id ->
            repository.permanentDelete(id)
        }
        clearSelection()
        refresh()
    }
    
    fun restoreSelected() {
        selectedWorkIds.forEach { id ->
            repository.restore(id)
        }
        clearSelection()
        refresh()
    }

    // --- Trash Selection ---
    fun toggleTrashSelection(id: String) {
        val newSet = trashSelectedWorkIds.toMutableSet()
        if (newSet.contains(id)) {
            newSet.remove(id)
        } else {
            newSet.add(id)
        }
        trashSelectedWorkIds = newSet
    }

    fun clearTrashSelection() {
        trashSelectedWorkIds = emptySet()
    }

    fun permanentDeleteTrashSelected() {
        trashSelectedWorkIds.forEach { id ->
            repository.permanentDelete(id)
        }
        clearTrashSelection()
        refresh()
    }

    fun restoreTrashSelected() {
        trashSelectedWorkIds.forEach { id ->
            repository.restore(id)
        }
        clearTrashSelection()
        refresh()
    }

    // --- Single Operations ---
    fun softDelete(id: String) {
        repository.softDelete(id)
        refresh()
    }
    
    fun permanentDelete(id: String) {
        repository.permanentDelete(id)
        refresh()
    }
    
    fun restore(id: String) {
        repository.restore(id)
        refresh()
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

    fun duplicate(work: ChromaWork) {
        val copy = work.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${work.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis()
        )
        repository.save(copy)
        refresh()
    }
}
