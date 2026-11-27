package com.chroma.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.LiveStudioStreamUpdate
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.data.SupabaseLiveStudioRepository
import com.chroma.studio.model.LivePresenceMember
import com.chroma.studio.model.LivePulseReaction
import com.chroma.studio.model.LiveStudioConnectionState
import com.chroma.studio.model.LiveStudioEventRow
import com.chroma.studio.model.LiveStudioHelpers
import com.chroma.studio.model.LiveStudioPollOption
import com.chroma.studio.model.LiveStudioRoom
import com.chroma.studio.ui.components.ToastManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LiveStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SupabaseLiveStudioRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _selectedRoom = MutableStateFlow(LiveStudioRoom.GENERAL)
    val selectedRoom: StateFlow<LiveStudioRoom> = _selectedRoom.asStateFlow()

    private val _connectionState = MutableStateFlow(LiveStudioConnectionState.CONNECTING)
    val connectionState: StateFlow<LiveStudioConnectionState> = _connectionState.asStateFlow()

    private val _connectionHint = MutableStateFlow<String?>(null)
    val connectionHint: StateFlow<String?> = _connectionHint.asStateFlow()

    private val _events = MutableStateFlow<List<LiveStudioEventRow>>(emptyList())
    val events: StateFlow<List<LiveStudioEventRow>> = _events.asStateFlow()

    private val _presenceMembers = MutableStateFlow<List<LivePresenceMember>>(emptyList())
    val presenceMembers: StateFlow<List<LivePresenceMember>> = _presenceMembers.asStateFlow()

    private val _reactionBursts = MutableStateFlow<List<LivePulseReaction>>(emptyList())
    val reactionBursts: StateFlow<List<LivePulseReaction>> = _reactionBursts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var subscriptionJob: Job? = null

    fun connectRoom(room: LiveStudioRoom = _selectedRoom.value) {
        _selectedRoom.value = room
        subscriptionJob?.cancel()
        _isLoading.value = true
        _connectionHint.value = null

        val member = currentPresenceMember()
        subscriptionJob = viewModelScope.launch {
            try {
                repository.observeRoomStream(room.id, member).collect { update ->
                    when (update) {
                        is LiveStudioStreamUpdate.ConnectionChanged -> {
                            _connectionState.value = update.state
                            _connectionHint.value = update.errorMessage
                        }
                        is LiveStudioStreamUpdate.InitialSnapshot -> {
                            _events.value = update.events.sortedByDescending { it.createdAtMs }
                            _isLoading.value = false
                        }
                        is LiveStudioStreamUpdate.Upserted -> {
                            _events.value = LiveStudioHelpers.mergeEvent(_events.value, update.event)
                        }
                        is LiveStudioStreamUpdate.Deleted -> {
                            _events.value = LiveStudioHelpers.removeEvent(_events.value, update.eventId)
                        }
                        is LiveStudioStreamUpdate.PresenceUpdated -> {
                            _presenceMembers.value = update.members
                        }
                        is LiveStudioStreamUpdate.ReactionBurst -> {
                            emitLocalBurst(update.reaction)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _connectionState.value = LiveStudioConnectionState.OFFLINE
                _isLoading.value = false
            }
        }
    }

    fun disconnectRoom() {
        subscriptionJob?.cancel()
        subscriptionJob = null
    }

    fun postNote(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        val row = buildBaseRow(eventType = "note", body = cleaned) ?: return
        publishOptimisticRow(row)
    }

    fun postPalette(title: String, hexes: List<String>) {
        if (hexes.isEmpty()) return
        val cleanTitle = title.trim().ifBlank { "Color Palette" }
        val row = buildBaseRow(
            eventType = "palette",
            body = cleanTitle,
            paletteHexes = hexes
        ) ?: return
        publishOptimisticRow(row)
    }

    fun postPoll(question: String, options: List<String>) {
        val cleanQuestion = question.trim()
        val cleanOptions = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanQuestion.isEmpty() || cleanOptions.size < 2) return
        val pollOptions = cleanOptions.mapIndexed { idx, label ->
            LiveStudioPollOption(id = "opt_${idx + 1}", label = label)
        }
        val row = buildBaseRow(
            eventType = "poll",
            pollQuestion = cleanQuestion,
            pollOptions = pollOptions
        ) ?: return
        publishOptimisticRow(row)
    }

    fun toggleLike(eventId: String) {
        val uid = currentUidOrNull() ?: return
        val existing = _events.value.firstOrNull { it.id == eventId } ?: return
        val updated = LiveStudioHelpers.toggleLike(existing, uid)
        _events.value = LiveStudioHelpers.mergeEvent(_events.value, updated)

        viewModelScope.launch {
            val result = repository.updateLikes(eventId, updated.likedBy)
            if (result.isFailure) {
                _events.value = LiveStudioHelpers.mergeEvent(_events.value, existing)
                ToastManager.showToast("Could not sync like")
            }
        }
    }

    fun votePoll(eventId: String, optionId: String) {
        val uid = currentUidOrNull() ?: return
        val existing = _events.value.firstOrNull { it.id == eventId } ?: return
        val updated = LiveStudioHelpers.votePoll(existing, optionId, uid)
        if (updated == existing) return
        _events.value = LiveStudioHelpers.mergeEvent(_events.value, updated)

        viewModelScope.launch {
            val result = repository.updatePollOptions(eventId, updated.pollOptions)
            if (result.isFailure) {
                _events.value = LiveStudioHelpers.mergeEvent(_events.value, existing)
                ToastManager.showToast("Could not sync vote")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        val uid = currentUidOrNull() ?: return
        val existing = _events.value.firstOrNull { it.id == eventId } ?: return
        if (existing.authorUid != uid) return
        _events.value = LiveStudioHelpers.removeEvent(_events.value, eventId)

        viewModelScope.launch {
            val result = repository.deleteEvent(eventId, uid)
            if (result.isFailure) {
                _events.value = LiveStudioHelpers.mergeEvent(_events.value, existing)
                ToastManager.showToast("Could not delete item")
            } else {
                ToastManager.showToast("Removed from Live Studio")
            }
        }
    }

    fun sendReactionBurst(emoji: String) {
        val member = currentPresenceMember()
        val burst = LivePulseReaction(
            id = UUID.randomUUID().toString(),
            emoji = emoji,
            senderName = member.name,
            timestampMs = System.currentTimeMillis()
        )
        emitLocalBurst(burst)
        viewModelScope.launch {
            repository.broadcastReaction(burst)
        }
    }

    private fun emitLocalBurst(burst: LivePulseReaction) {
        _reactionBursts.value = (_reactionBursts.value + burst).takeLast(16)
        viewModelScope.launch {
            delay(1450)
            _reactionBursts.value = _reactionBursts.value.filterNot { it.id == burst.id }
        }
    }

    private fun publishOptimisticRow(row: LiveStudioEventRow) {
        _events.value = LiveStudioHelpers.mergeEvent(_events.value, row)
        viewModelScope.launch {
            val result = repository.insertEvent(row)
            if (result.isFailure) {
                ToastManager.showToast("Saved locally (Live sync offline)")
            }
        }
    }

    private fun buildBaseRow(
        eventType: String,
        body: String = "",
        paletteHexes: List<String> = emptyList(),
        pollQuestion: String = "",
        pollOptions: List<LiveStudioPollOption> = emptyList()
    ): LiveStudioEventRow? {
        val uid = currentUidOrNull() ?: return null
        val ctx = getApplication<Application>()
        val saved = SavedAccountManager.getSavedAccounts(ctx).firstOrNull { it.uid == uid }
        val user = auth.currentUser
        val handle = saved?.username?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.replace(" ", "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: "creator_${uid.take(5)}"
        val displayName = saved?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: handle
        val avatarUrl = saved?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: user?.photoUrl?.toString().orEmpty()

        return LiveStudioEventRow(
            id = UUID.randomUUID().toString(),
            roomId = _selectedRoom.value.id,
            eventType = eventType,
            authorUid = uid,
            authorName = displayName,
            authorHandle = handle,
            authorAvatarUrl = avatarUrl,
            body = body,
            paletteHexes = paletteHexes,
            pollQuestion = pollQuestion,
            pollOptions = pollOptions,
            createdAtMs = System.currentTimeMillis()
        )
    }

    private fun currentUidOrNull(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun currentPresenceMember(): LivePresenceMember {
        val uid = currentUidOrNull() ?: "guest"
        val ctx = getApplication<Application>()
        val saved = SavedAccountManager.getSavedAccounts(ctx).firstOrNull { it.uid == uid }
        val name = saved?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "Creator"
        val avatarUrl = saved?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.photoUrl?.toString().orEmpty()
        return LivePresenceMember(uid = uid, name = name, avatarUrl = avatarUrl)
    }
}
