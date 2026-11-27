package com.chroma.studio

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.chroma.studio.data.SupabaseChatRepository
import com.chroma.studio.ui.components.DynamicIslandManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data payload for in-app chat notifications and Dynamic Island overlay events.
 */
data class ChatInAppEvent(
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val messageText: String,
    val avatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
    val isVerified: Boolean = false,
    val messageId: String = ""
) {
    fun canonicalKey(): String {
        return if (messageId.isNotBlank()) {
            "msg:${roomId}:${messageId}"
        } else {
            "fp:${roomId}:${senderId}:${timestamp}:${messageText.hashCode()}"
        }
    }
}

/**
 * Global ChatStateManager:
 * Tracks four key states:
 * 1. Current user ID (currentUserId)
 * 2. Whether the app is in the foreground (isAppInForeground)
 * 3. Active chat recipient ID (activeRecipientId) [and activeRoomId]
 * 4. Flow for in-app events (inAppEvents)
 */
object ChatStateManager {
    private const val TAG = "ChatStateManager"
    private val eventDeduplicator = com.chroma.studio.chat.data.InAppEventDeduplicator(ttlMs = 120_000L)

    // 1. Current user ID
    private val _currentUserId = MutableStateFlow<String?>(FirebaseAuth.getInstance().currentUser?.uid)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
        Log.d(TAG, "Current user ID updated: $userId")
    }

    // 2. Whether the app is in the foreground
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
        Log.d(TAG, "App in foreground updated: $inForeground")
    }

    // 3. Active chat recipient ID (and active room ID)
    private val _activeRecipientId = MutableStateFlow<String?>(null)
    val activeRecipientId: StateFlow<String?> = _activeRecipientId.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _isChatSurfaceVisible = MutableStateFlow(false)
    val isChatSurfaceVisible: StateFlow<Boolean> = _isChatSurfaceVisible.asStateFlow()

    fun setChatSurfaceVisible(visible: Boolean) {
        if (_isChatSurfaceVisible.value != visible) {
            _isChatSurfaceVisible.value = visible
            Log.d(TAG, "Chat surface visible updated: $visible")
        }
    }

    fun setActiveRecipientId(recipientId: String?) {
        _activeRecipientId.value = recipientId
        Log.d(TAG, "Active recipient ID updated: $recipientId")
    }

    fun setActiveRoomId(roomId: String?) {
        _activeRoomId.value = roomId
        DynamicIslandManager.currentlyActiveRoomId = roomId
        Log.d(TAG, "Active room ID updated: $roomId")
    }

    fun setActiveChat(recipientId: String?, roomId: String? = null) {
        _activeRecipientId.value = recipientId
        _activeRoomId.value = roomId
        DynamicIslandManager.currentlyActiveRoomId = roomId
        Log.d(TAG, "Active chat updated: recipientId=$recipientId, roomId=$roomId")
    }

    fun clearActiveChat() {
        _activeRecipientId.value = null
        _activeRoomId.value = null
        _isChatSurfaceVisible.value = false
        DynamicIslandManager.currentlyActiveRoomId = null
        Log.d(TAG, "Active chat cleared")
    }

    /**
     * Checks whether the user currently has this exact room open, foregrounded, and visible.
     * Normalizes DM room IDs so uidA_uidB / uidB_uidA / direct_uidB resolve identically.
     */
    fun isChatScreenVisible(roomId: String?): Boolean {
        if (!_isAppInForeground.value || !_isChatSurfaceVisible.value) return false
        val targetRoom = roomId?.takeIf { it.isNotBlank() } ?: return false
        val activeRoom = (_activeRoomId.value ?: DynamicIslandManager.currentlyActiveRoomId)
            ?.takeIf { it.isNotBlank() } ?: return false
        if (activeRoom == targetRoom) return true

        val uid = _currentUserId.value ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isBlank()) return false
        val normActive = com.chroma.studio.data.OnlineStatusManager.normalizeRoomId(activeRoom, uid)
        val normTarget = com.chroma.studio.data.OnlineStatusManager.normalizeRoomId(targetRoom, uid)
        return normActive == normTarget
    }

    /**
     * Determines whether the user is actively viewing this specific chat room or recipient.
     */
    fun isChatActive(recipientId: String?, roomId: String? = null): Boolean {
        if (!_isAppInForeground.value || !_isChatSurfaceVisible.value) return false
        val curRecip = _activeRecipientId.value
        val curRoom = _activeRoomId.value ?: DynamicIslandManager.currentlyActiveRoomId

        val matchesRecipient = !recipientId.isNullOrBlank() && (curRecip == recipientId || curRoom == recipientId)
        val matchesRoom = isChatScreenVisible(roomId) || (!roomId.isNullOrBlank() && (curRoom == roomId || curRecip == roomId))

        return matchesRecipient || matchesRoom
    }

    /**
     * Atomically checks and records whether an incoming event key has already been notified.
     * Prevents dual delivery from FCM push + Firestore snapshot + repeated MODIFIED snapshots.
     */
    fun markAndCheckEventNotDuplicate(key: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        return eventDeduplicator.shouldProcess(key, nowMs)
    }

    // 4. Flow for in-app events
    private val _inAppEvents = MutableSharedFlow<ChatInAppEvent>(extraBufferCapacity = 64)
    val inAppEvents: SharedFlow<ChatInAppEvent> = _inAppEvents.asSharedFlow()

    // Navigation callback registered by HomeActivity to navigate to a room when Dynamic Island is tapped
    var onNavigateToRoom: ((String) -> Unit)? = null

    /**
     * Emits a chat message to the in-app event flow and triggers the Dynamic Island HUD overlay,
     * deduplicating repeat emissions for the same message within the TTL window.
     */
    fun emitInAppEvent(event: ChatInAppEvent) {
        val dedupKey = event.canonicalKey()
        if (!markAndCheckEventNotDuplicate(dedupKey)) {
            Log.d(TAG, "Dropped duplicate in-app event for key=$dedupKey")
            return
        }
        _inAppEvents.tryEmit(event)
        Log.d(TAG, "Emitted in-app event for room=${event.roomId}, msg=${event.messageId}, sender=${event.senderName}")

        // Trigger the Dynamic Island in-app overlay HUD
        DynamicIslandManager.showChatMessage(
            roomId = event.roomId,
            messageId = event.messageId,
            senderName = event.senderName,
            messageText = event.messageText,
            avatarUrl = event.avatarUrl,
            timeText = "now",
            isVerified = event.isVerified,
            onReply = { replyText ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = SupabaseChatRepository()
                        val sent = repo.sendMessage(event.roomId, replyText)
                        if (sent) {
                            repo.markAsRead(event.roomId)
                        } else {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                com.chroma.studio.ui.components.ToastManager.showToast("Failed to send reply")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send in-app reply", e)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            com.chroma.studio.ui.components.ToastManager.showToast("Failed to send reply")
                        }
                    }
                }
            },
            onMarkAsRead = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SupabaseChatRepository().markAsRead(event.roomId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark as read in-app", e)
                    }
                }
            },
            onQuickReact = { emoji ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = SupabaseChatRepository()
                        val sent = repo.sendMessage(event.roomId, emoji)
                        if (sent) {
                            repo.markAsRead(event.roomId)
                        } else {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                com.chroma.studio.ui.components.ToastManager.showToast("Failed to send reaction")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send quick reaction in-app", e)
                    }
                }
            },
            onMute = if (event.roomId.isNotBlank()) ({
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SupabaseChatRepository().muteChat(event.roomId, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mute chat in-app", e)
                    }
                }
            }) else null,
            onClick = {
                if (event.roomId.isNotBlank()) {
                    onNavigateToRoom?.invoke(event.roomId)
                }
            }
        )
    }

    fun emitInAppEvent(
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
        isVerified: Boolean = false,
        messageId: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        emitInAppEvent(
            ChatInAppEvent(
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
                timestamp = timestamp,
                isVerified = isVerified,
                messageId = messageId
            )
        )
    }

    // 5. Visibility of bottom action options / sheets (hides main bottom nav bar when active)
    private val _isBottomOptionsVisible = MutableStateFlow(false)
    val isBottomOptionsVisible: StateFlow<Boolean> = _isBottomOptionsVisible.asStateFlow()

    fun setBottomOptionsVisible(visible: Boolean) {
        _isBottomOptionsVisible.value = visible
        Log.d(TAG, "Bottom options visibility updated: $visible")
    }

    init {
        // Automatically sync current user ID with Firebase Auth state
        try {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                _currentUserId.value = auth.currentUser?.uid
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing FirebaseAuth listener", e)
        }
    }
}

/**
 * Application lifecycle observer using DefaultLifecycleObserver to track
 * when the application is in foreground (onStart) or background (onStop).
 */
class AppLifecycleObserver : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        ChatStateManager.setAppInForeground(true)
        try {
            com.chroma.studio.data.OnlineStatusManager.setAppForegrounded(true)
        } catch (e: Exception) {
            Log.w("AppLifecycleObserver", "Error setting foreground status: ${e.message}")
        }
        Log.d("AppLifecycleObserver", "App entered foreground")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        ChatStateManager.setAppInForeground(false)
        try {
            com.chroma.studio.data.OnlineStatusManager.setAppForegrounded(false)
        } catch (e: Exception) {
            Log.w("AppLifecycleObserver", "Error setting background status: ${e.message}")
        }
        Log.d("AppLifecycleObserver", "App entered background")
    }
}
