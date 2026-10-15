package com.chroma.studio.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.ChromaWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshots.Snapshot

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = WorkRepository(application)
    val discoverRepository = DiscoverRepository()

    val works = mutableStateListOf<ChromaWork>()
    val deletedWorks = mutableStateListOf<ChromaWork>()

    var selectedWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val selectionMode: Boolean
        get() = derivedStateOf { selectedWorkIds.isNotEmpty() }.value

    // Trash Selection state
    var trashSelectedWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val trashSelectionMode: Boolean
        get() = trashSelectedWorkIds.isNotEmpty()
        
    // Dark mode state
    var isDarkMode by mutableStateOf(false)
        private set
        
    var customHomeBgLayersJson by mutableStateOf<String?>(null)
        private set
        
    var customHomeBgShape by mutableStateOf("full")
        private set

    var isCurrentUserVerified by mutableStateOf(false)
        private set

    init {
        refresh()
        initPresence()
    }

    private fun initPresence() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val myConnectionsRef = db.getReference("users/$uid/online")
        val lastOnlineRef = db.getReference("users/$uid/lastOnline")
        val connectedRef = db.getReference(".info/connected")
        
        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    myConnectionsRef.onDisconnect().setValue(false)
                    lastOnlineRef.onDisconnect().setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                    myConnectionsRef.setValue(true)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.loadActiveWorks()
            val deleted = repository.loadDeletedWorks()
            
            val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val customLayers = prefs.getString("custom_home_background_layers", null)
            val customShape = prefs.getString("custom_home_background_shape", "full") ?: "full"
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            var verified = false
            auth.currentUser?.uid?.let { uid ->
                val profile = discoverRepository.getUserProfile(uid)
                verified = profile?.verified == true
                
                // E2EE: Generate and sync active public key to Firestore
                val currentDeviceKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                if (currentDeviceKey.isNotEmpty() && (profile == null || profile.publicKey != currentDeviceKey)) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update("publicKey", currentDeviceKey)
                }
            }

            withContext(Dispatchers.Main) {
                Snapshot.withMutableSnapshot {
                    isCurrentUserVerified = verified
                    works.clear()
                    works.addAll(active)
                }
                Snapshot.withMutableSnapshot {
                    deletedWorks.clear()
                    deletedWorks.addAll(deleted)
                }
                customHomeBgLayersJson = customLayers
                customHomeBgShape = customShape
            }
        }
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

    fun updateSelection(newSelection: Set<String>) {
        selectedWorkIds = newSelection
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
    
    fun shareToDiscover(work: ChromaWork, aspectRatio: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                discoverRepository.publishWork(work, aspectRatio)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}
