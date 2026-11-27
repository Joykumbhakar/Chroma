package com.chroma.studio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.ChatRepository
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SupabaseChatRepository
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {
    private val repository = SupabaseChatRepository()
    private val discoverRepository = DiscoverRepository()
    private val presenceListeners = mutableMapOf<String, () -> Unit>()
    private var typingDebounceJob: kotlinx.coroutines.Job? = null
    private var typingHeartbeatJob: kotlinx.coroutines.Job? = null
    private var lastTypingSendTime: Long = 0L
    private var recordingHeartbeatJob: kotlinx.coroutines.Job? = null
    private var currentActivityStatus: String? = null
    private val recipientExpiryJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val activityPresenceController = com.chroma.studio.chat.controller.ChatActivityPresenceController(
        scope = viewModelScope,
        onDispatchActivity = { roomId, activity ->
            currentActivityStatus = activity
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val normalizedRoomId = com.chroma.studio.data.OnlineStatusManager.normalizeRoomId(roomId, currentUid, currentOtherUserId)
            viewModelScope.launch {
                com.chroma.studio.data.OnlineStatusManager.setChatActivity(normalizedRoomId, activity)
            }
        }
    )
    private var statusTtlMonitorJob: kotlinx.coroutines.Job? = null
    private val receivedActivityTimestamps = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, Long>>()
    private val typingListeners = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()

    private val _recentChats = MutableStateFlow<List<ChatRoom>>(emptyList())
    val recentChats: StateFlow<List<ChatRoom>> = _recentChats.asStateFlow()

    private val _serverMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _localMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()
    private val _outboxRoomStatuses = MutableStateFlow<Map<String, com.chroma.studio.chat.data.OutboxRoomStatus>>(emptyMap())
    val outboxRoomStatuses: StateFlow<Map<String, com.chroma.studio.chat.data.OutboxRoomStatus>> = _outboxRoomStatuses.asStateFlow()
    private val inFlightSendIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    val currentMessages: StateFlow<List<ChatMessage>> = combine(_serverMessages, _localMessages, _activeRoomId, _recentChats) { server, local, roomId, rooms ->
        val uid = currentAuthUserId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val participants = rooms.find { it.id == roomId }?.participants.orEmpty()
        val localById = local.associateBy { it.id }
        val serverIds = HashSet<String>(server.size)
        val reconciledServer = server.map { serverMsg ->
            serverIds.add(serverMsg.id)
            com.chroma.studio.chat.data.MessageStatusMachine.mergeMonotonic(
                previous = localById[serverMsg.id],
                incoming = serverMsg,
                currentUid = uid,
                groupParticipants = participants
            )
        }
        val remainingLocal = local.filter { it.id !in serverIds }
        (reconciledServer + remainingLocal)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOtherUserOnline = MutableStateFlow(false)
    val isOtherUserOnline: StateFlow<Boolean> = _isOtherUserOnline.asStateFlow()

    private val _otherUserLastSeen = MutableStateFlow<Long?>(null)
    val otherUserLastSeen: StateFlow<Long?> = _otherUserLastSeen.asStateFlow()

    private val _onlineUsersPresence = MutableStateFlow<Map<String, com.chroma.studio.data.UserPresence>>(emptyMap())
    val onlineUsersPresence: StateFlow<Map<String, com.chroma.studio.data.UserPresence>> = _onlineUsersPresence.asStateFlow()

    private val _stories = MutableStateFlow<List<com.chroma.studio.model.UserStoryGroup>>(emptyList())
    val stories: StateFlow<List<com.chroma.studio.model.UserStoryGroup>> = _stories.asStateFlow()

    private val _userProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val userProfiles: StateFlow<Map<String, UserProfile>> = _userProfiles.asStateFlow()

    private val _typingStatuses = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val typingStatuses: StateFlow<Map<String, Map<String, String>>> = _typingStatuses.asStateFlow()

    private val _draftsByRoom = MutableStateFlow<Map<String, String>>(emptyMap())
    val draftsByRoom: StateFlow<Map<String, String>> = _draftsByRoom.asStateFlow()

    fun getDraft(roomId: String): String = _draftsByRoom.value[roomId].orEmpty()

    fun saveDraft(roomId: String, text: String) {
        if (roomId.isBlank()) return
        val current = _draftsByRoom.value
        if (text.isBlank()) {
            if (current.containsKey(roomId)) {
                _draftsByRoom.value = current - roomId
            }
        } else if (current[roomId] != text) {
            _draftsByRoom.value = current + (roomId to text)
        }
    }

    fun getUserProfile(userId: String): UserProfile? = _userProfiles.value[userId]

    fun fetchProfilesForUsers(userIds: List<String>) {
        if (userIds.isEmpty()) return
        viewModelScope.launch {
            val currentMap = _userProfiles.value.toMutableMap()
            val missing = userIds.filter { !currentMap.containsKey(it) || currentMap[it]?.displayName.isNullOrBlank() || currentMap[it]?.avatarUrl.isNullOrBlank() }
            if (missing.isEmpty()) return@launch
            for (uid in missing) {
                try {
                    val profile = discoverRepository.getUserProfile(uid)
                    if (profile != null) {
                        currentMap[uid] = profile
                        com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
                        com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(profile)
                        if (uid == currentAuthUserId) {
                            com.chroma.studio.data.OnlineStatusManager.updateSelfShowOnlineStatus(profile.showOnlineStatus)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            _userProfiles.value = currentMap
            try {
                com.chroma.studio.data.OfflineDataCacheManager.saveUserProfiles(
                    com.chroma.studio.ChromaApp.instance,
                    currentMap.values
                )
            } catch (_: Exception) {}
        }
    }

    private var chatsJob: Job? = null
    private var messagesJob: Job? = null
    private var storiesJob: Job? = null
    private val pendingRoomCreations = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<ChatRoom?>>()
    private var onlineStatusListener: ValueEventListener? = null
    private var currentOtherUserId: String? = null
    private var authStateListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null
    private var currentAuthUserId: String? = null

    private var messageLimit = 50L

    private val _isLoadingChats = MutableStateFlow(true)
    val isLoadingChats: StateFlow<Boolean> = _isLoadingChats.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _mutedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedChatIds: StateFlow<Set<String>> = _mutedChatIds.asStateFlow()

    init {
        val initialUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val initialUid = initialUser?.uid
        if (!initialUid.isNullOrBlank()) {
            // Preload cached user profiles for instant offline display of names, avatars, badges & last seen
            try {
                val cachedProfiles = com.chroma.studio.data.OfflineDataCacheManager.getAllCachedProfiles(
                    com.chroma.studio.ChromaApp.instance
                )
                if (cachedProfiles.isNotEmpty()) {
                    _userProfiles.value = cachedProfiles
                }
            } catch (_: Exception) {}
            // Immediately load recent chats strictly scoped to this UID for Frame 0 offline launch
            try {
                val cachedChats = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(
                    com.chroma.studio.ChromaApp.instance,
                    initialUid
                )
                if (cachedChats.isNotEmpty()) {
                    _recentChats.value = cachedChats
                    _isLoadingChats.value = false
                }
            } catch (_: Exception) {}
            reloadForUser(initialUid)
        } else {
            _recentChats.value = emptyList()
            _isLoadingChats.value = false
        }

        authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            val newUid = auth.currentUser?.uid
            val oldUid = currentAuthUserId
            if (newUid != oldUid) {
                if (oldUid != null) {
                    resetForLogout()
                }
                if (!newUid.isNullOrBlank()) {
                    reloadForUser(newUid)
                }
            }
        }
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error adding auth state listener", e)
        }
    }

    private fun loadStories() {
        storiesJob?.cancel()
        storiesJob = viewModelScope.launch {
            _recentChats.collect { chats ->
                refreshStoriesInternal(chats)
            }
        }
    }

    fun refreshStories() {
        viewModelScope.launch {
            refreshStoriesInternal(_recentChats.value)
        }
    }

    private suspend fun refreshStoriesInternal(chats: List<com.chroma.studio.model.ChatRoom>) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val chatUserIds = chats.mapNotNull { chat ->
            if (chat.isGroup) null
            else chat.participants.firstOrNull { it != currentUid }
        }.distinct()

        try {
            val storyRepo = com.chroma.studio.data.StoryRepository()
            val followingIds = storyRepo.getFollowingIds()
            val allUserIds = (chatUserIds + followingIds + listOfNotNull(currentUid)).distinct()

            val realStories = if (allUserIds.isNotEmpty()) {
                val targeted = storyRepo.fetchStoriesForUsers(allUserIds)
                if (targeted.size < 5) {
                    val publicStories = storyRepo.fetchRecentPublicStories()
                    (targeted + publicStories).distinctBy { it.userId }
                } else targeted
            } else {
                storyRepo.fetchRecentPublicStories()
            }
            _stories.value = realStories
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("ChatViewModel", "loadStories error", e)
        }
    }

    private fun observeUsersPresence(userIds: List<String>) {
        val currentUids = userIds.toSet()
        val listeners = presenceListeners ?: return
        // Remove listeners for users no longer present
        val removed = listeners.keys - currentUids
        for (uid in removed) {
            listeners.remove(uid)?.invoke()
            _onlineUsersPresence.value = _onlineUsersPresence.value - uid
        }
        // Add listeners for new users
        for (uid in currentUids) {
            if (listeners.containsKey(uid)) continue
            // Seed with cached Firestore profile lastSeen if available
            _userProfiles.value[uid]?.let { com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(it) }
            try {
                val unsubscribe = com.chroma.studio.data.OnlineStatusManager.observeUserPresence(uid) { rawPresence ->
                    val profile = _userProfiles.value[uid]
                    val presence = if (profile != null && !profile.showOnlineStatus) {
                        com.chroma.studio.data.UserPresence(online = false, lastOnline = null)
                    } else {
                        val fallbackLastSeen = rawPresence.lastOnline ?: profile?.lastSeen?.takeIf { it > 0L }
                        com.chroma.studio.data.UserPresence(online = rawPresence.online, lastOnline = fallbackLastSeen)
                    }
                    _onlineUsersPresence.value = _onlineUsersPresence.value + (uid to presence)
                    if (uid == currentOtherUserId) {
                        _isOtherUserOnline.value = presence.online
                        _otherUserLastSeen.value = presence.lastOnline
                    }
                }
                listeners[uid] = unsubscribe
            } catch (e: Exception) {
                // Ignore presence observation failures in offline/guest mode
            }
        }
    }

    fun observeUserOnlineStatus(uid: String) {
        if (currentOtherUserId == uid && onlineStatusListener != null) return

        currentOtherUserId?.let {
            onlineStatusListener?.let { listener ->
                com.chroma.studio.data.OnlineStatusManager.getDatabase().getReference("users/$it").removeEventListener(listener)
            }
        }

        currentOtherUserId = uid
        val initialProfile = _userProfiles.value[uid]
        if (initialProfile != null) {
            com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(initialProfile)
            if (!initialProfile.showOnlineStatus) {
                _isOtherUserOnline.value = false
                _otherUserLastSeen.value = null
            } else if (initialProfile.lastSeen != null && initialProfile.lastSeen > 0L && _otherUserLastSeen.value == null) {
                _otherUserLastSeen.value = initialProfile.lastSeen
            }
        }
        val ref = com.chroma.studio.data.OnlineStatusManager.getDatabase().getReference("users/$uid")

        onlineStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parsed = com.chroma.studio.data.OnlineStatusManager.parsePresenceSnapshot(uid, snapshot)
                val profile = _userProfiles.value[uid]
                val finalPresence = if (profile != null && !profile.showOnlineStatus) {
                    com.chroma.studio.data.UserPresence(online = false, lastOnline = null)
                } else {
                    val lastSeen = parsed.lastOnline ?: profile?.lastSeen?.takeIf { it > 0L }
                    com.chroma.studio.data.UserPresence(online = parsed.online, lastOnline = lastSeen)
                }
                _isOtherUserOnline.value = finalPresence.online
                _otherUserLastSeen.value = finalPresence.lastOnline
                _onlineUsersPresence.value = _onlineUsersPresence.value + (uid to finalPresence)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("ChatViewModel", "observeUserOnlineStatus cancelled for $uid: ${error.message}")
            }
        }
        ref.addValueEventListener(onlineStatusListener!!)
    }

    fun observeChats(targetUid: String? = null) {
        val currentUid = targetUid ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _recentChats.value = emptyList()
            _isLoadingChats.value = false
            return
        }

        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            _isLoadingChats.value = true
            try {
                repository.observeRecentChats(currentUid).collect { rawChats ->
                    // Strictly isolate rooms to the active user; never leak another account's chats
                    if (currentAuthUserId != null && currentAuthUserId != currentUid) return@collect
                    val chats = rawChats.filter { currentUid in it.participants }
                    _recentChats.value = chats
                    _isLoadingChats.value = false
                    try {
                        com.chroma.studio.chat.data.ChatLocalCacheManager.saveRecentChats(
                            com.chroma.studio.ChromaApp.instance,
                            chats,
                            currentUid
                        )
                    } catch (_: Exception) {}

                    // Pre-load top recent chat messages into memory cache in background (0ms open)
                    if (chats.isNotEmpty()) {
                        try {
                            com.chroma.studio.chat.data.ChatLocalCacheManager.preloadRooms(
                                com.chroma.studio.ChromaApp.instance,
                                chats.map { it.id },
                                currentUid
                            )
                        } catch (_: Exception) {}
                    }

                    // Asynchronously fetch profiles for all chat participants
                    val allUids = chats.flatMap { it.participants }.distinct()
                    fetchProfilesForUsers(allUids)

                    // Observe real-time online presence for all contacts
                    val otherUids = allUids.filter { it != currentUid }
                    observeUsersPresence(otherUids)

                    observeTypingStatuses(chats)

                    // Auto-mark incoming undelivered messages as delivered in realtime
                    launch(Dispatchers.IO) {
                        for (chat in chats) {
                            if (chat.lastSender != null && chat.lastSender != currentUid && !chat.lastMessageIsDelivered) {
                                repository.markAsDelivered(chat.id, chat.lastMessageId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("ChatViewModel", "observeChats error", e)
                _isLoadingChats.value = false
            }
        }
    }

    fun reloadForUser(userId: String? = null) {
        val uid = userId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            resetForLogout()
            return
        }

        if (currentAuthUserId != null && currentAuthUserId != uid) {
            // Switching directly from one account to another — wipe all previous account state first
            resetForLogout()
        } else if (currentAuthUserId == uid && chatsJob?.isActive == true) {
            return
        }

        currentAuthUserId = uid
        com.chroma.studio.ChatStateManager.setCurrentUserId(uid)

        // Seed only this user's isolated cache (empty list if fresh account) so old account data never lingers
        val scopedCached = try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(
                com.chroma.studio.ChromaApp.instance,
                uid
            )
        } catch (_: Exception) { emptyList() }
        _recentChats.value = scopedCached
        if (scopedCached.isNotEmpty()) {
            try {
                com.chroma.studio.chat.data.ChatLocalCacheManager.preloadRooms(
                    com.chroma.studio.ChromaApp.instance,
                    scopedCached.map { it.id },
                    uid
                )
            } catch (_: Exception) {}
        }

        observeChats(uid)
        loadStories()
        loadMutedChats()
        loadBlockedUsers()
        callManager?.startListeningForIncomingCalls(uid)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.getString("publicKey").isNullOrBlank()) {
                    val pubKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                    if (pubKey.isNotBlank()) {
                        firestore.collection("users").document(uid).update("publicKey", pubKey)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resetForLogout() {
        currentAuthUserId = null
        try { callManager?.endCall("ended") } catch (_: Exception) {}
        callManager?.stopListeningForIncomingCalls()
        chatsJob?.cancel()
        chatsJob = null
        messagesJob?.cancel()
        messagesJob = null
        storiesJob?.cancel()
        storiesJob = null
        pendingRoomCreations.values.forEach { it.cancel() }
        pendingRoomCreations.clear()

        currentOtherUserId?.let {
            onlineStatusListener?.let { listener ->
                try {
                    com.chroma.studio.data.OnlineStatusManager.getDatabase().getReference("users/$it").removeEventListener(listener)
                } catch (_: Exception) {}
            }
        }
        onlineStatusListener = null
        currentOtherUserId = null

        for (unsubscribe in presenceListeners.values) {
            try { unsubscribe() } catch (_: Exception) {}
        }
        presenceListeners.clear()

        typingDebounceJob?.cancel()
        typingDebounceJob = null
        typingHeartbeatJob?.cancel()
        typingHeartbeatJob = null
        recordingHeartbeatJob?.cancel()
        recordingHeartbeatJob = null
        statusTtlMonitorJob?.cancel()
        statusTtlMonitorJob = null

        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        for ((roomId, listener) in typingListeners) {
            try {
                db.getReference("status/$roomId").removeEventListener(listener)
            } catch (_: Exception) {}
        }
        typingListeners.clear()
        receivedActivityTimestamps.clear()

        _recentChats.value = emptyList()
        _serverMessages.value = emptyList()
        _localMessages.value = emptyList()
        _outboxRoomStatuses.value = emptyMap()
        inFlightSendIds.clear()
        _activeRoomId.value = null
        _isLoadingChats.value = false
        _isOtherUserOnline.value = false
        _otherUserLastSeen.value = null
        _onlineUsersPresence.value = emptyMap()
        _stories.value = emptyList()
        _userProfiles.value = emptyMap()
        _typingStatuses.value = emptyMap()
        _draftsByRoom.value = emptyMap()
        _blockedUserIds.value = emptySet()
        _mutedChatIds.value = emptySet()

        repository.clearCaches()
        com.chroma.studio.chat.data.ChatLocalCacheManager.clearAllOnLogout(com.chroma.studio.ChromaApp.instance)
        com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = null
        com.chroma.studio.ChatStateManager.clearActiveChat()
        com.chroma.studio.ChatStateManager.setCurrentUserId(null)
    }

    fun startChat(otherUserId: String, onRoomReady: (String) -> Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val roomId = if (currentUser.uid < otherUserId) "${currentUser.uid}_$otherUserId" else "${otherUserId}_${currentUser.uid}"

        // Optimistically add to recent chats to avoid waiting
        val currentChats = _recentChats.value.toMutableList()
        if (currentChats.none { it.id == roomId }) {
            val optimisticRoom = ChatRoom(
                id = roomId,
                participants = listOf(currentUser.uid, otherUserId),
                lastMessageTime = System.currentTimeMillis()
            )
            currentChats.add(0, optimisticRoom)
            _recentChats.value = currentChats
        }

        // Start Firestore room creation BEFORE opening chat so observeMessages() awaits
        // parent room existence and never races Firestore rules with PERMISSION_DENIED.
        val creationDeferred = viewModelScope.async {
            try {
                val realRoom = repository.getOrCreateChatRoom(otherUserId)
                if (realRoom != null) {
                    val updatedChats = _recentChats.value.toMutableList()
                    val idx = updatedChats.indexOfFirst { it.id == roomId }
                    if (idx != -1) {
                        updatedChats[idx] = realRoom
                    } else {
                        updatedChats.add(0, realRoom)
                    }
                    _recentChats.value = updatedChats
                }
                realRoom
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("ChatViewModel", "startChat room creation error for $roomId", e)
                null
            } finally {
                pendingRoomCreations.remove(roomId)
            }
        }
        pendingRoomCreations[roomId] = creationDeferred

        openChat(roomId)
        onRoomReady(roomId)
        fetchProfilesForUsers(listOf(currentUser.uid, otherUserId))
    }

    fun openChat(roomId: String) {
        val previousRoomId = _activeRoomId.value
        val isAlreadyActiveForRoom = previousRoomId == roomId && messagesJob?.isActive == true
        if (isAlreadyActiveForRoom) {
            return
        }

        _activeRoomId.value = roomId
        com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = roomId

        // 1. INSTANT OFFLINE LOCAL STORAGE LOAD (WhatsApp / Telegram architecture):
        // Synchronously load from local storage (L1 memory or L2 disk) BEFORE anything else.
        // This guarantees that when entering a chat, historical messages appear on Frame 0 (0ms)
        // with ZERO blank screen, ZERO opening from zero, and 100% offline access in Airplane mode.
        val cached = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedMessages(
            context = com.chroma.studio.ChromaApp.instance,
            roomId = roomId,
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
            limit = 40
        ).map { msg ->
            if (msg.isPending && msg.id !in inFlightSendIds) {
                msg.copy(
                    isPending = false,
                    isError = true,
                    errorReason = msg.errorReason ?: "Send interrupted. Tap to retry.",
                    uploadProgress = null
                )
            } else msg
        }
        _serverMessages.value = cached
        _localMessages.value = emptyList()

        observeRoomTyping(roomId)

        // Observe participant presence in real-time
        val currentFirebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val currentUid = currentFirebaseUser?.uid
        val currentRoom = _recentChats.value.find { it.id == roomId }
        val otherUid = currentRoom?.participants?.firstOrNull { it != currentUid }
            ?: if (!roomId.startsWith("group_") && roomId.contains("_") && currentUid != null) {
                roomId.split("_").firstOrNull { it != currentUid }
            } else null

        if (otherUid != null) {
            observeUserOnlineStatus(otherUid)
        }
        com.chroma.studio.ChatStateManager.setActiveChat(recipientId = otherUid, roomId = roomId)

        // 2. Viewport limit: start with 30-40 messages on Firestore while older messages page from SQLite
        messageLimit = 30L.coerceAtLeast(cached.size.toLong().coerceAtMost(40L))
        observeMessages(roomId)

        // Optimistically mark as read in local state
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val currentChats = _recentChats.value.toMutableList()
            val index = currentChats.indexOfFirst { it.id == roomId }
            if (index != -1) {
                val chat = currentChats[index]
                val updatedUnread = chat.unreadCount.toMutableMap()
                updatedUnread[currentUser.uid] = 0
                currentChats[index] = chat.copy(unreadCount = updatedUnread)
                _recentChats.value = currentChats
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Pre-warm room participants, public keys, push metadata, and FCM OAuth2 token BEFORE user sends a message
            launch {
                try {
                    repository.prefetchRoomMetadata(roomId, currentRoom?.participants)
                } catch (_: Exception) {}
            }
            try {
                pendingRoomCreations[roomId]?.await()
            } catch (_: Exception) {}
            try {
                if (com.chroma.studio.ChatStateManager.isChatScreenVisible(roomId)) {
                    repository.markAsRead(roomId)
                }
            } catch (_: Exception) {}
            try {
                val activeUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (activeUid != null) {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userDoc = firestore.collection("users").document(activeUid).get().await()
                    if (userDoc.getString("publicKey").isNullOrBlank()) {
                        val pubKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                        firestore.collection("users").document(activeUid).update("publicKey", pubKey)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMoreMessages() {
        val roomId = _activeRoomId.value ?: return
        val oldestLoadedTs = _serverMessages.value.minOfOrNull { it.timestamp }
        if (oldestLoadedTs != null) {
            try {
                val olderCached = com.chroma.studio.chat.data.ChatLocalCacheManager.getOlderCachedMessages(
                    context = com.chroma.studio.ChromaApp.instance,
                    roomId = roomId,
                    beforeTimestamp = oldestLoadedTs,
                    limit = 40,
                    userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                )
                if (olderCached.isNotEmpty()) {
                    _serverMessages.value = (_serverMessages.value + olderCached)
                        .distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                }
            } catch (_: Exception) {}
        }
        messageLimit += 30L
        observeMessages(roomId)
    }

    fun createGroupFromConfig(
        context: android.content.Context,
        config: com.chroma.studio.chat.ui.components.GroupCreationConfig,
        preGeneratedRoomId: String? = null,
        onRoomReady: (String) -> Unit
    ) {
        val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val realParticipantIds = config.members
            .mapNotNull { it.id.takeIf { id -> id.isNotBlank() && id != currentUserUid } }
            .distinct()
        val roomId = preGeneratedRoomId ?: "group_${java.util.UUID.randomUUID()}"

        viewModelScope.launch {
            var localBytes: ByteArray? = null
            var localExt = "jpg"
            val avatarStr = config.groupAvatar
            if (avatarStr.startsWith("content://") || avatarStr.startsWith("file://")) {
                try {
                    val parsedUri = android.net.Uri.parse(avatarStr)
                    localBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
                    }
                    localExt = context.contentResolver.getType(parsedUri)?.substringAfter("/")
                        ?: if (avatarStr.endsWith(".png", ignoreCase = true)) "png" else "jpg"
                } catch (_: Exception) {}
            }
            createGroup(
                groupName = config.groupName,
                groupDescription = config.groupDescription.ifBlank { null },
                participantIds = realParticipantIds,
                imageUrl = avatarStr,
                preGeneratedRoomId = roomId,
                onlyAdminsCanPost = config.onlyAdminsCanPost,
                coAdminIds = config.coAdminIds,
                disappearingHours = config.disappearingHours,
                inviteCode = config.inviteCode,
                isPublicGroup = config.isPublicGroup,
                editInfoAdminsOnly = config.editInfoAdminsOnly,
                welcomeMessage = config.welcomeMessage.ifBlank { null },
                localImageBytes = localBytes,
                localImageExt = localExt,
                onRoomReady = onRoomReady
            )
        }
    }

    fun createGroup(
        groupName: String,
        groupDescription: String? = null,
        participantIds: List<String>,
        imageUrl: String? = null,
        preGeneratedRoomId: String? = null,
        onlyAdminsCanPost: Boolean = false,
        coAdminIds: List<String> = emptyList(),
        disappearingHours: Int = 0,
        inviteCode: String? = null,
        isPublicGroup: Boolean = false,
        editInfoAdminsOnly: Boolean = true,
        welcomeMessage: String? = null,
        localImageBytes: ByteArray? = null,
        localImageExt: String = "jpg",
        onRoomReady: (String) -> Unit
    ) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val roomId = preGeneratedRoomId ?: "group_${java.util.UUID.randomUUID()}"

        // Optimistic UI update
        val allUsers = (participantIds + currentUser.uid).filter { it.isNotBlank() }.distinct()
        val allAdmins = (listOf(currentUser.uid) + coAdminIds).filter { it in allUsers }.distinct()
        val initPreview = welcomeMessage?.trim()?.takeIf { it.isNotBlank() }?.let { "👋 Welcome to $groupName! $it" } ?: "Group created"

        val optimisticRoom = ChatRoom(
            id = roomId,
            name = groupName,
            groupDescription = groupDescription,
            isGroup = true,
            avatarUrl = imageUrl,
            participants = allUsers,
            admins = allAdmins,
            createdBy = currentUser.uid,
            lastMessage = initPreview,
            lastMessageTime = System.currentTimeMillis(),
            onlyAdminsCanPost = onlyAdminsCanPost,
            disappearingHours = disappearingHours,
            inviteCode = inviteCode,
            isPublicGroup = isPublicGroup,
            editInfoAdminsOnly = editInfoAdminsOnly
        )
        val currentChats = _recentChats.value.toMutableList()
        currentChats.add(0, optimisticRoom)
        _recentChats.value = currentChats

        // Start Firestore group creation BEFORE opening chat so observeMessages() awaits
        // parent room document write before attaching the messages subcollection listener.
        val creationDeferred = viewModelScope.async {
            try {
                val uploadedUrl = if (localImageBytes != null) {
                    uploadGroupImageBytes(localImageBytes, localImageExt) ?: imageUrl
                } else {
                    imageUrl
                }
                if (!uploadedUrl.isNullOrBlank() && uploadedUrl != imageUrl) {
                    val updatedList = _recentChats.value.toMutableList()
                    val idx = updatedList.indexOfFirst { it.id == roomId }
                    if (idx != -1) {
                        updatedList[idx] = updatedList[idx].copy(avatarUrl = uploadedUrl)
                        _recentChats.value = updatedList
                    }
                }
                repository.createGroup(
                    groupName = groupName,
                    groupDescription = groupDescription,
                    participantIds = participantIds,
                    imageUrl = uploadedUrl,
                    preGeneratedId = roomId,
                    onlyAdminsCanPost = onlyAdminsCanPost,
                    coAdminIds = coAdminIds,
                    disappearingHours = disappearingHours,
                    inviteCode = inviteCode,
                    isPublicGroup = isPublicGroup,
                    editInfoAdminsOnly = editInfoAdminsOnly,
                    welcomeMessage = welcomeMessage
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("ChatViewModel", "createGroup error for $roomId", e)
                null
            } finally {
                pendingRoomCreations.remove(roomId)
            }
        }
        pendingRoomCreations[roomId] = creationDeferred

        openChat(roomId)
        onRoomReady(roomId)
        fetchProfilesForUsers(allUsers)
    }

    fun createGroup(
        groupName: String,
        groupDescription: String? = null,
        participantIds: List<String>,
        imageUrl: String? = null,
        preGeneratedRoomId: String? = null,
        onlyAdminsCanPost: Boolean = false,
        onRoomReady: (String) -> Unit
    ) = createGroup(
        groupName = groupName,
        groupDescription = groupDescription,
        participantIds = participantIds,
        imageUrl = imageUrl,
        preGeneratedRoomId = preGeneratedRoomId,
        onlyAdminsCanPost = onlyAdminsCanPost,
        coAdminIds = emptyList(),
        onRoomReady = onRoomReady
    )

    fun createGroup(
        groupName: String,
        groupDescription: String? = null,
        participantIds: List<String>,
        imageUrl: String? = null,
        preGeneratedRoomId: String? = null,
        onRoomReady: (String) -> Unit
    ) = createGroup(
        groupName = groupName,
        groupDescription = groupDescription,
        participantIds = participantIds,
        imageUrl = imageUrl,
        preGeneratedRoomId = preGeneratedRoomId,
        onlyAdminsCanPost = false,
        onRoomReady = onRoomReady
    )

    suspend fun fetchRoom(roomId: String): ChatRoom? {
        val existing = _recentChats.value.find { it.id == roomId }
        if (existing != null) return existing
        return repository.getRoom(roomId)
    }


    fun toggleFavorite(roomId: String, isFavorite: Boolean) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Optimistic UI update
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats[index]
            val newFavorites = if (isFavorite) chat.favoritedBy + currentUserId else chat.favoritedBy - currentUserId
            currentChats[index] = chat.copy(favoritedBy = newFavorites)
            _recentChats.value = currentChats
        }

        viewModelScope.launch {
            repository.toggleFavorite(roomId, currentUserId, isFavorite)
        }
    }

    private fun observeMessages(roomId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            try {
                // Wait for any in-flight room creation (new group or DM) to finish writing
                // chats/{roomId} to Firestore before attaching the messages subcollection listener.
                try {
                    pendingRoomCreations[roomId]?.await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }
                repository.observeMessages(roomId, messageLimit).collect { msgs ->
                    // CRITICAL OFFLINE PROTECTION:
                    // Only update server messages if new messages arrived from server or Firestore cache.
                    // Never wipe out non-empty cached messages if an empty list arrives during offline transitions.
                    if (msgs.isNotEmpty()) {
                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        val participants = _recentChats.value.find { it.id == roomId }?.participants.orEmpty()
                        val prevById = _serverMessages.value.associateBy { it.id }
                        val mergedIncoming = msgs.map { incoming ->
                            com.chroma.studio.chat.data.MessageStatusMachine.mergeMonotonic(
                                previous = prevById[incoming.id],
                                incoming = incoming,
                                currentUid = currentUserId ?: "",
                                groupParticipants = participants
                            )
                        }
                        val minServerTs = mergedIncoming.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE
                        val serverIds = mergedIncoming.map { it.id }.toSet()
                        val preservedOlderLocal = _serverMessages.value.filter {
                            it.id !in serverIds && it.timestamp < minServerTs
                        }
                        _serverMessages.value = (mergedIncoming + preservedOlderLocal)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }

                        // Clear local outbox pending/error badge once Firestore server confirms commit
                        val activeOutbox = _outboxRoomStatuses.value[roomId]
                        if (activeOutbox != null) {
                            val confirmedMatch = mergedIncoming.firstOrNull { it.id == activeOutbox.messageId && !it.isPending }
                            if (confirmedMatch != null) {
                                _outboxRoomStatuses.value = _outboxRoomStatuses.value - roomId
                            }
                        }

                        // WhatsApp-grade immediate eviction of typing indicator on message arrival
                        if (currentUserId != null) {
                            val otherSenders = mergedIncoming.map { it.senderId }.filter { it != currentUserId }.toSet()
                            otherSenders.forEach { senderId ->
                                evictUserTypingActivity(roomId, senderId)
                            }

                            val isGroupChat = roomId.startsWith("group_") ||
                                (_recentChats.value.find { it.id == roomId }?.isGroup == true)
                            // ONLY mark messages as read when the chat screen is genuinely foregrounded & visible;
                            // otherwise ONLY mark as delivered!
                            if (com.chroma.studio.ChatStateManager.isChatScreenVisible(roomId)) {
                                val hasUnreadFromOther = mergedIncoming.any {
                                    it.senderId != currentUserId &&
                                        if (isGroupChat) !it.readBy.containsKey(currentUserId) else !it.isRead
                                }
                                if (hasUnreadFromOther) {
                                    launch(Dispatchers.IO) {
                                        repository.markAsRead(roomId)
                                    }
                                }
                            } else {
                                val hasUndeliveredFromOther = mergedIncoming.any {
                                    it.senderId != currentUserId &&
                                        if (isGroupChat) !it.deliveredTo.containsKey(currentUserId) else !it.isDelivered
                                }
                                if (hasUndeliveredFromOther) {
                                    launch(Dispatchers.IO) {
                                        repository.markAsDelivered(roomId)
                                    }
                                }
                            }
                        }
                        // Asynchronously save to local offline cache so next open is instant and media is cached
                        try {
                            com.chroma.studio.chat.data.ChatLocalCacheManager.saveMessages(
                                context = com.chroma.studio.ChromaApp.instance,
                                roomId = roomId,
                                messages = mergedIncoming,
                                userId = currentUserId
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                    } else if (_serverMessages.value.isEmpty()) {
                        _serverMessages.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("ChatViewModel", "observeMessages error for room $roomId: ${e.message}")
            }
        }
    }

    fun markActiveRoomReadIfVisible(roomId: String? = _activeRoomId.value) {
        val targetId = roomId?.takeIf { it.isNotBlank() } ?: return
        if (!com.chroma.studio.ChatStateManager.isChatScreenVisible(targetId)) return
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasUnread = _serverMessages.value.any { it.senderId != currentUid && !it.readBy.containsKey(currentUid) && !it.isRead } ||
            ((_recentChats.value.find { it.id == targetId }?.unreadCount?.get(currentUid) ?: 0) > 0)
        if (!hasUnread) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAsRead(targetId)
        }
    }

    private fun applyOptimisticRoomSend(
        roomId: String,
        msgId: String,
        currentUserId: String,
        previewText: String,
        timestamp: Long
    ) {
        inFlightSendIds.add(msgId)
        _outboxRoomStatuses.value = _outboxRoomStatuses.value + (
            roomId to com.chroma.studio.chat.data.OutboxRoomStatus(
                messageId = msgId,
                status = com.chroma.studio.chat.data.MessageDeliveryStatus.PENDING
            )
        )
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats.removeAt(index)
            currentChats.add(
                0,
                chat.copy(
                    lastMessage = previewText,
                    lastMessageTime = timestamp,
                    lastSender = currentUserId,
                    lastMessageId = msgId,
                    lastMessageIsRead = false,
                    lastMessageIsDelivered = false
                )
            )
            _recentChats.value = currentChats
        }
    }

    private fun handleSendResult(
        roomId: String,
        msgId: String,
        result: com.chroma.studio.chat.data.SendMessageResult,
        showToastOnFailure: Boolean = true
    ) {
        inFlightSendIds.remove(msgId)
        val success = result is com.chroma.studio.chat.data.SendMessageResult.Success
        val errorMsg = (result as? com.chroma.studio.chat.data.SendMessageResult.Failure)?.userMessage

        val mapTransform: (ChatMessage) -> ChatMessage = { msg ->
            if (msg.id == msgId) {
                msg.copy(
                    isPending = false,
                    isError = !success,
                    errorReason = errorMsg,
                    uploadProgress = null
                )
            } else msg
        }

        if (_activeRoomId.value == roomId) {
            _localMessages.value = _localMessages.value.map(mapTransform)
            _serverMessages.value = _serverMessages.value.map(mapTransform)
        }

        val updatedMsg = (_localMessages.value.firstOrNull { it.id == msgId }
            ?: _serverMessages.value.firstOrNull { it.id == msgId })
        if (updatedMsg != null) {
            try {
                com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                    com.chroma.studio.ChromaApp.instance,
                    roomId,
                    updatedMsg
                )
            } catch (_: Exception) {}
        }

        val currentOutbox = _outboxRoomStatuses.value
        if (success) {
            if (currentOutbox[roomId]?.messageId == msgId) {
                _outboxRoomStatuses.value = currentOutbox - roomId
            }
        } else {
            _outboxRoomStatuses.value = currentOutbox + (
                roomId to com.chroma.studio.chat.data.OutboxRoomStatus(
                    messageId = msgId,
                    status = com.chroma.studio.chat.data.MessageDeliveryStatus.ERROR,
                    errorReason = errorMsg
                )
            )
            if (showToastOnFailure && !errorMsg.isNullOrBlank()) {
                com.chroma.studio.ui.components.ToastManager.showToast(errorMsg)
            }
        }
    }

    fun sendMessage(
        text: String,
        workId: String? = null,
        replyToMessageId: String? = null,
        storyId: String? = null,
        storyThumbnailUrl: String? = null,
        fontStyle: String? = null
    ) {
        val roomId = _activeRoomId.value ?: return
        sendMessageToRoom(roomId, text, workId, replyToMessageId, storyId, storyThumbnailUrl, fontStyle)
    }

    fun sendStoryReply(
        roomId: String,
        text: String,
        storyId: String?,
        storyThumbnailUrl: String?
    ) {
        sendMessageToRoom(
            roomId = roomId,
            text = text,
            storyId = storyId,
            storyThumbnailUrl = storyThumbnailUrl
        )
    }

    fun sendFlashVideo(
        roomId: String,
        flashVideo: com.chroma.studio.model.FlashVideo,
        caption: String? = null
    ) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (_activeRoomId.value == roomId) {
            setTypingStatus(false)
        }
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val messageText = caption ?: flashVideo.caption
        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = messageText,
            timestamp = timestamp,
            isPending = true,
            imageUrl = flashVideo.thumbnailUrl,
            mediaUrl = flashVideo.videoUrl,
            messageType = "flash_video",
            flashVideoId = flashVideo.id,
            flashVideoThumbnailUrl = flashVideo.thumbnailUrl,
            flashVideoUrl = flashVideo.videoUrl,
            flashVideoCaption = flashVideo.caption,
            flashVideoAuthorUsername = flashVideo.authorUsername,
            flashVideoAuthorAvatarUrl = flashVideo.authorAvatarUrl,
            flashVideoDurationMs = flashVideo.duration
        )

        // Add to local immediately if this room is actively open
        if (_activeRoomId.value == roomId) {
            _localMessages.value = listOf(pendingMsg) + _localMessages.value
        }

        // Persist to offline cache
        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (_: Exception) {}

        // Optimistically update recent chats list (resetting prior read/delivered flags!)
        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = "⚡ Shared a Flash video",
            timestamp = timestamp
        )

        viewModelScope.launch(Dispatchers.IO) {
            try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
            val result = repository.sendMessageDetailed(
                roomId = roomId,
                text = messageText,
                preGeneratedId = msgId,
                messageType = "flash_video",
                imageUrl = flashVideo.thumbnailUrl,
                flashVideoId = flashVideo.id,
                flashVideoThumbnailUrl = flashVideo.thumbnailUrl,
                flashVideoUrl = flashVideo.videoUrl,
                flashVideoCaption = flashVideo.caption,
                flashVideoAuthorUsername = flashVideo.authorUsername,
                flashVideoAuthorAvatarUrl = flashVideo.authorAvatarUrl,
                flashVideoDurationMs = flashVideo.duration
            )
            handleSendResult(roomId, msgId, result)
        }
    }

    fun sendMusicTrack(
        roomId: String,
        selection: com.chroma.studio.data.AttachedMusicSelection
    ) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (_activeRoomId.value == roomId) {
            setTypingStatus(false)
        }
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val displayTitle = "${selection.title} · ${selection.artist}"
        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = displayTitle,
            timestamp = timestamp,
            isPending = true,
            imageUrl = selection.coverUrl.takeIf { it.isNotBlank() },
            mediaUrl = selection.audioUrl,
            messageType = "music",
            musicTrackId = selection.trackId,
            musicTitle = selection.title,
            musicArtist = selection.artist,
            musicAlbum = selection.album,
            musicCoverUrl = selection.coverUrl,
            musicAudioUrl = selection.audioUrl,
            musicStartSec = selection.startSec,
            musicDurationSec = selection.durationSec
        )

        if (_activeRoomId.value == roomId) {
            _localMessages.value = listOf(pendingMsg) + _localMessages.value
        }

        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (_: Exception) {}

        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = "🎵 ${selection.title}",
            timestamp = timestamp
        )

        viewModelScope.launch(Dispatchers.IO) {
            try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
            val result = repository.sendMessageDetailed(
                roomId = roomId,
                text = displayTitle,
                preGeneratedId = msgId,
                messageType = "music",
                imageUrl = selection.coverUrl.takeIf { it.isNotBlank() },
                musicTrackId = selection.trackId,
                musicTitle = selection.title,
                musicArtist = selection.artist,
                musicAlbum = selection.album,
                musicCoverUrl = selection.coverUrl,
                musicAudioUrl = selection.audioUrl,
                musicStartSec = selection.startSec,
                musicDurationSec = selection.durationSec
            )
            if (result is com.chroma.studio.chat.data.SendMessageResult.Success) {
                try {
                    com.chroma.studio.data.MusicRepository.getInstance().incrementTrackUsage(selection.trackId)
                } catch (_: Exception) {}
            }
            handleSendResult(roomId, msgId, result)
        }
    }

    fun sendMessageToRoom(
        roomId: String,
        text: String,
        workId: String? = null,
        replyToMessageId: String? = null,
        storyId: String? = null,
        storyThumbnailUrl: String? = null,
        fontStyle: String? = null
    ) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (text.isBlank() && workId == null && storyThumbnailUrl == null) return

        if (_activeRoomId.value == roomId) {
            setTypingStatus(false)
        }
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val resolvedMessageType = when {
            storyThumbnailUrl != null || storyId != null -> "story_reply"
            text.startsWith("[POLL]:") -> "poll"
            else -> "text"
        }
        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = text,
            timestamp = timestamp,
            isPending = true,
            workId = workId,
            replyToMessageId = replyToMessageId,
            messageType = resolvedMessageType,
            storyId = storyId,
            storyThumbnailUrl = storyThumbnailUrl,
            fontStyle = fontStyle
        )
        // Add to local immediately if this room is actively open
        if (_activeRoomId.value == roomId) {
            _localMessages.value = listOf(pendingMsg) + _localMessages.value
        }

        // Immediately persist to offline cache so outgoing message is preserved even if offline
        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (e: Exception) {
            // ignore
        }

        // Optimistically update recent chats list (clearing previous message read/delivered flags!)
        val previewText = when {
            storyThumbnailUrl != null || storyId != null -> "Replied to story: $text"
            workId != null -> "Shared a work"
            else -> text
        }
        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = previewText,
            timestamp = timestamp
        )

        viewModelScope.launch(Dispatchers.IO) {
            try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
            val result = repository.sendMessageDetailed(
                roomId = roomId,
                text = text,
                workId = workId,
                preGeneratedId = msgId,
                replyToMessageId = replyToMessageId,
                messageType = resolvedMessageType,
                storyId = storyId,
                storyThumbnailUrl = storyThumbnailUrl,
                fontStyle = fontStyle
            )
            handleSendResult(roomId, msgId, result)
        }
    }

    fun retryMessage(msgId: String) {
        val roomId = _activeRoomId.value ?: return
        val targetMsg = _localMessages.value.firstOrNull { it.id == msgId }
            ?: _serverMessages.value.firstOrNull { it.id == msgId }
            ?: return
        if (!targetMsg.isError) return

        inFlightSendIds.add(msgId)
        val resetTransform: (ChatMessage) -> ChatMessage = { msg ->
            if (msg.id == msgId) msg.copy(isPending = true, isError = false, errorReason = null) else msg
        }
        _localMessages.value = _localMessages.value.map(resetTransform)
        _serverMessages.value = _serverMessages.value.map(resetTransform)
        _outboxRoomStatuses.value = _outboxRoomStatuses.value + (
            roomId to com.chroma.studio.chat.data.OutboxRoomStatus(
                messageId = msgId,
                status = com.chroma.studio.chat.data.MessageDeliveryStatus.PENDING
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
            val uploadFailure = com.chroma.studio.chat.data.SendMessageResult.Failure(
                reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                userMessage = "Media upload failed. Tap to retry.",
                isRetryable = true
            )
            val result: com.chroma.studio.chat.data.SendMessageResult = when (targetMsg.messageType) {
                "audio" -> {
                    val mediaUrl = targetMsg.mediaUrl ?: targetMsg.imageUrl
                    if (mediaUrl != null && (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://"))) {
                        repository.sendMessageDetailed(
                            roomId = roomId,
                            text = targetMsg.text,
                            imageUrl = mediaUrl,
                            preGeneratedId = msgId,
                            messageType = "audio",
                            durationSeconds = targetMsg.durationSeconds,
                            replyToMessageId = targetMsg.replyToMessageId
                        )
                    } else if (mediaUrl != null && java.io.File(mediaUrl).exists()) {
                        val audioBytes = java.io.File(mediaUrl).readBytes()
                        val uploadedUrl = repository.uploadAudioToSupabase(audioBytes, "m4a")
                        if (uploadedUrl != null) {
                            repository.sendMessageDetailed(
                                roomId = roomId,
                                text = targetMsg.text,
                                imageUrl = uploadedUrl,
                                preGeneratedId = msgId,
                                messageType = "audio",
                                durationSeconds = targetMsg.durationSeconds,
                                replyToMessageId = targetMsg.replyToMessageId
                            )
                        } else uploadFailure
                    } else uploadFailure
                }
                "image", "video", "sticker" -> {
                    val mediaUrl = targetMsg.mediaUrl ?: targetMsg.imageUrl
                    if (mediaUrl != null && (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://"))) {
                        repository.sendMessageDetailed(
                            roomId = roomId,
                            text = targetMsg.text,
                            imageUrl = mediaUrl,
                            preGeneratedId = msgId,
                            messageType = targetMsg.messageType,
                            replyToMessageId = targetMsg.replyToMessageId
                        )
                    } else if (!mediaUrl.isNullOrBlank()) {
                        val parsedUri = android.net.Uri.parse(mediaUrl)
                        val bytes = try {
                            if (parsedUri.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
                                java.io.File(parsedUri.path!!).takeIf { it.exists() }?.readBytes()
                            } else if (parsedUri.scheme == "content") {
                                com.chroma.studio.ChromaApp.instance.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
                            } else {
                                java.io.File(mediaUrl).takeIf { it.exists() }?.readBytes()
                            }
                        } catch (_: Exception) { null }
                        if (bytes != null && bytes.isNotEmpty()) {
                            val ext = when (targetMsg.messageType) {
                                "video" -> "mp4"
                                "sticker" -> "png"
                                else -> "jpg"
                            }
                            val uploadedUrl = when (targetMsg.messageType) {
                                "video" -> repository.uploadVideoToSupabase(bytes, ext)
                                "sticker" -> repository.uploadImageToSupabase(bytes, "png")
                                else -> repository.uploadImageToSupabase(bytes, ext)
                            }
                            if (uploadedUrl != null) {
                                repository.sendMessageDetailed(
                                    roomId = roomId,
                                    text = targetMsg.text,
                                    imageUrl = uploadedUrl,
                                    preGeneratedId = msgId,
                                    messageType = targetMsg.messageType,
                                    replyToMessageId = targetMsg.replyToMessageId
                                )
                            } else uploadFailure
                        } else uploadFailure
                    } else uploadFailure
                }
                "file" -> {
                    val sourceUriStr = targetMsg.mediaUrl ?: targetMsg.imageUrl
                    val resolvedName = targetMsg.fileName?.takeIf { it.isNotBlank() } ?: "Document"
                    val ext = (targetMsg.fileExtension?.takeIf { it.isNotBlank() }
                        ?: resolvedName.substringAfterLast('.', "bin")).lowercase()
                    if (sourceUriStr != null && (sourceUriStr.startsWith("http://") || sourceUriStr.startsWith("https://"))) {
                        repository.sendMessageDetailed(
                            roomId = roomId,
                            text = targetMsg.text,
                            imageUrl = sourceUriStr,
                            preGeneratedId = msgId,
                            messageType = "file",
                            replyToMessageId = targetMsg.replyToMessageId,
                            fileName = resolvedName,
                            fileSizeBytes = targetMsg.fileSizeBytes,
                            fileExtension = ext.uppercase()
                        )
                    } else if (!sourceUriStr.isNullOrBlank()) {
                        val uri = android.net.Uri.parse(sourceUriStr)
                        val bytes = try {
                            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                                java.io.File(uri.path!!).takeIf { it.exists() }?.readBytes()
                            } else if (uri.scheme == "content") {
                                com.chroma.studio.ChromaApp.instance.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            } else {
                                java.io.File(sourceUriStr).takeIf { it.exists() }?.readBytes()
                            }
                        } catch (_: Exception) { null }
                        if (bytes != null && bytes.isNotEmpty()) {
                            updateLocalUploadProgress(msgId, 8)
                            val uploadedUrl = com.chroma.studio.data.SupabaseMediaStorage.uploadFile(
                                bytes = bytes,
                                fileName = resolvedName,
                                bucket = "chat-media",
                                onProgress = { pct ->
                                    if (_activeRoomId.value == roomId) {
                                        updateLocalUploadProgress(msgId, (8 + (pct * 92) / 100).coerceIn(8, 100))
                                    }
                                }
                            )
                            if (uploadedUrl != null) {
                                updateLocalUploadProgress(msgId, 100)
                                repository.sendMessageDetailed(
                                    roomId = roomId,
                                    text = targetMsg.text,
                                    imageUrl = uploadedUrl,
                                    preGeneratedId = msgId,
                                    messageType = "file",
                                    replyToMessageId = targetMsg.replyToMessageId,
                                    fileName = resolvedName,
                                    fileSizeBytes = targetMsg.fileSizeBytes ?: bytes.size.toLong(),
                                    fileExtension = ext.uppercase()
                                )
                            } else uploadFailure
                        } else uploadFailure
                    } else uploadFailure
                }
                else -> {
                    repository.sendMessageDetailed(
                        roomId = roomId,
                        text = targetMsg.text,
                        workId = targetMsg.workId,
                        preGeneratedId = msgId,
                        messageType = targetMsg.messageType.ifBlank { "text" },
                        replyToMessageId = targetMsg.replyToMessageId,
                        storyId = targetMsg.storyId,
                        storyThumbnailUrl = targetMsg.storyThumbnailUrl,
                        flashVideoId = targetMsg.flashVideoId,
                        flashVideoThumbnailUrl = targetMsg.flashVideoThumbnailUrl,
                        flashVideoUrl = targetMsg.flashVideoUrl,
                        flashVideoCaption = targetMsg.flashVideoCaption,
                        flashVideoAuthorUsername = targetMsg.flashVideoAuthorUsername,
                        flashVideoAuthorAvatarUrl = targetMsg.flashVideoAuthorAvatarUrl,
                        flashVideoDurationMs = targetMsg.flashVideoDurationMs,
                        fontStyle = targetMsg.fontStyle,
                        musicTrackId = targetMsg.musicTrackId,
                        musicTitle = targetMsg.musicTitle,
                        musicArtist = targetMsg.musicArtist,
                        musicAlbum = targetMsg.musicAlbum,
                        musicCoverUrl = targetMsg.musicCoverUrl,
                        musicAudioUrl = targetMsg.musicAudioUrl,
                        musicStartSec = targetMsg.musicStartSec,
                        musicDurationSec = targetMsg.musicDurationSec
                    )
                }
            }
            handleSendResult(roomId, msgId, result)
        }
    }

    /**
     * Typing indicator with single-send + 3-second idle timeout:
     * - Sends "typing" to Firebase ONCE when user starts typing (first keystroke).
     * - Does NOT re-send on every subsequent keystroke to avoid excessive network writes.
     * - Resets a 3-second inactivity timer on each keystroke; if no key is pressed
     *   for 3 seconds, clears the indicator immediately.
     * - A sparse 5-second heartbeat keeps the indicator alive on the receiver side
     *   during long continuous typing sessions.
     * - Immediately clears when input becomes blank or a message is dispatched.
     */
    fun onTextInputChanged(newText: String, roomIdOverride: String? = null) {
        val rawRoomId = roomIdOverride ?: _activeRoomId.value ?: return
        activityPresenceController.onTextChanged(newText, rawRoomId)
    }

    fun setTypingStatus(isTyping: Boolean, roomIdOverride: String? = null) {
        val rawRoomId = roomIdOverride ?: _activeRoomId.value ?: return
        if (isTyping) {
            activityPresenceController.onTextChanged("...", rawRoomId)
        } else {
            activityPresenceController.onTextChanged("", rawRoomId)
        }
    }

    fun setRecordingStatus(isRecording: Boolean, roomIdOverride: String? = null) {
        val rawRoomId = roomIdOverride ?: _activeRoomId.value ?: return
        if (isRecording) {
            activityPresenceController.onRecordingStarted(rawRoomId)
        } else {
            activityPresenceController.onRecordingStopped(rawRoomId)
        }
    }

    fun evictUserTypingActivity(roomId: String, userId: String) {
        val compositeKey = "$roomId:$userId"
        recipientExpiryJobs.remove(compositeKey)?.cancel()
        val currentMap = _typingStatuses.value
        val roomActivities = currentMap[roomId] ?: return
        if (roomActivities.containsKey(userId)) {
            val updatedRoomActivities = roomActivities - userId
            val newMap = if (updatedRoomActivities.isNotEmpty()) {
                currentMap + (roomId to updatedRoomActivities)
            } else {
                currentMap - roomId
            }
            _typingStatuses.value = newMap
        }
    }

    fun setChatActivity(activity: String?, force: Boolean = false, roomIdOverride: String? = null) {
        if (!force && currentActivityStatus == activity) return
        currentActivityStatus = activity
        val rawRoomId = roomIdOverride ?: _activeRoomId.value ?: return
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val normalizedRoomId = com.chroma.studio.data.OnlineStatusManager.normalizeRoomId(rawRoomId, currentUid, currentOtherUserId)
        viewModelScope.launch {
            com.chroma.studio.data.OnlineStatusManager.setChatActivity(normalizedRoomId, activity)
        }
    }

    fun sendAudioMessage(audioBytes: ByteArray, durationSeconds: Int, replyToMessageId: String? = null, localAudioPath: String? = null) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        setRecordingStatus(false)
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = "",
            timestamp = timestamp,
            isPending = true,
            messageType = "audio",
            imageUrl = localAudioPath,
            mediaUrl = localAudioPath,
            durationSeconds = durationSeconds,
            replyToMessageId = replyToMessageId
        )
        // Add to local immediately
        _localMessages.value = listOf(pendingMsg) + _localMessages.value

        // Immediately persist to offline cache
        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (e: Exception) {
            // ignore
        }

        // Optimistically update recent chats list (clearing previous read/delivered flags)
        val durationText = String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = "🎤 Voice message ($durationText)",
            timestamp = timestamp
        )

        viewModelScope.launch(Dispatchers.IO) {
            try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
            val audioUrl = repository.uploadAudioToSupabase(audioBytes, "m4a")
            val result = if (audioUrl != null) {
                repository.sendMessageDetailed(
                    roomId = roomId,
                    text = "",
                    imageUrl = audioUrl,
                    preGeneratedId = msgId,
                    messageType = "audio",
                    durationSeconds = durationSeconds,
                    replyToMessageId = replyToMessageId
                )
            } else {
                com.chroma.studio.chat.data.SendMessageResult.Failure(
                    reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                    userMessage = "Voice note upload failed. Tap to retry.",
                    isRetryable = true
                )
            }
            handleSendResult(roomId, msgId, result)
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) {
                    val reactions = msg.reactions.toMutableMap()
                    if (reactions[currentUserId] == emoji) {
                        reactions.remove(currentUserId)
                    } else {
                        reactions[currentUserId] = emoji
                    }
                    msg.copy(reactions = reactions)
                } else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local apply
        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.toggleReaction(messageId, roomId, emoji)
            if (!success) {
                // Graceful rollback on network failure
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not update reaction.")
            }
        }
    }


    fun addOrEnsureReaction(messageId: String, emoji: String) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) {
                    val reactions = msg.reactions.toMutableMap()
                    reactions[currentUserId] = emoji
                    msg.copy(reactions = reactions)
                } else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local apply (never toggles off / dislikes)
        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.addReaction(messageId, roomId, emoji)
            if (!success) {
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
            }
        }
    }

    fun deleteMessage(messageId: String, roomIdOverride: String? = null) {
        val roomId = roomIdOverride ?: _activeRoomId.value ?: return
        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local removal
        _serverMessages.value = _serverMessages.value.filter { it.id != messageId }
        _localMessages.value = _localMessages.value.filter { it.id != messageId }

        // Remove from local disk cache immediately
        com.chroma.studio.chat.data.ChatLocalCacheManager.removeMessage(
            com.chroma.studio.ChromaApp.instance,
            roomId,
            messageId
        )

        viewModelScope.launch {
            val success = repository.deleteMessage(messageId, roomId)
            if (!success) {
                // Graceful rollback on network failure
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not delete message.")
            }
        }
    }

    fun deleteMessageForMe(messageId: String, roomIdOverride: String? = null) {
        val roomId = roomIdOverride ?: _activeRoomId.value ?: return
        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local removal
        _serverMessages.value = _serverMessages.value.filter { it.id != messageId }
        _localMessages.value = _localMessages.value.filter { it.id != messageId }

        // Remove from local disk cache immediately
        com.chroma.studio.chat.data.ChatLocalCacheManager.removeMessage(
            com.chroma.studio.ChromaApp.instance,
            roomId,
            messageId
        )

        viewModelScope.launch {
            val success = repository.deleteMessageForMe(messageId, roomId)
            if (!success) {
                // Graceful rollback on network failure
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not delete message.")
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        val roomId = _activeRoomId.value ?: return
        if (newText.isBlank()) return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) msg.copy(text = newText) else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local edit
        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.editMessage(messageId, roomId, newText)
            if (!success) {
                // Graceful rollback on network failure
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not edit message.")
            }
        }
    }

    private fun updateLocalUploadProgress(msgId: String, progress: Int) {
        val clamped = progress.coerceIn(0, 100)
        _localMessages.value = _localMessages.value.map { msg ->
            if (msg.id == msgId && msg.uploadProgress != clamped) {
                msg.copy(uploadProgress = clamped)
            } else {
                msg
            }
        }
    }

    private suspend fun uploadAndCommitMedia(
        roomId: String,
        msgId: String,
        mediaBytes: ByteArray,
        extLower: String,
        messageType: String,
        text: String,
        replyToMessageId: String?,
        scaleMinPct: Int = 0,
        fileName: String? = null,
        fileSizeBytes: Long? = null
    ) {
        val scalePct: (Int) -> Int = { raw ->
            if (scaleMinPct <= 0) raw
            else (scaleMinPct + ((raw.coerceIn(0, 100) * (100 - scaleMinPct)) / 100)).coerceIn(scaleMinPct, 100)
        }
        val trackProgress: ((Int) -> Unit)? = if (messageType == "image" || messageType == "video" || messageType == "file") {
            { pct ->
                if (_activeRoomId.value == roomId) {
                    updateLocalUploadProgress(msgId, scalePct(pct))
                }
            }
        } else null

        val mediaUrl = when (messageType) {
            "video" -> repository.uploadVideoToSupabase(mediaBytes, extLower, onProgress = trackProgress)
            "audio" -> repository.uploadAudioToSupabase(mediaBytes, extLower)
            "sticker" -> repository.uploadImageToSupabase(mediaBytes, "png")
            "file" -> {
                com.chroma.studio.data.SupabaseMediaStorage.uploadFile(
                    bytes = mediaBytes,
                    fileName = fileName?.takeIf { it.isNotBlank() } ?: "chat_${msgId}.$extLower",
                    bucket = "chat-media",
                    onProgress = trackProgress
                )
            }
            else -> {
                val compressedBytes = com.chroma.studio.utils.ImageUtils.compressChatImageBytes(mediaBytes) ?: mediaBytes
                repository.uploadImageToSupabase(compressedBytes, extLower, onProgress = trackProgress)
            }
        }

        try { pendingRoomCreations[roomId]?.await() } catch (_: Exception) {}
        if (mediaUrl != null) {
            if (_activeRoomId.value == roomId && trackProgress != null) {
                updateLocalUploadProgress(msgId, 100)
            }
            val result = repository.sendMessageDetailed(
                roomId = roomId,
                text = text,
                imageUrl = mediaUrl,
                preGeneratedId = msgId,
                messageType = messageType,
                replyToMessageId = replyToMessageId,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                fileExtension = if (messageType == "file") extLower.uppercase() else null
            )
            handleSendResult(roomId, msgId, result)
        } else {
            val failMsg = if (messageType == "file") "File upload failed. Tap message to retry." else "Media upload failed. Tap to retry."
            handleSendResult(
                roomId = roomId,
                msgId = msgId,
                result = com.chroma.studio.chat.data.SendMessageResult.Failure(
                    reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                    userMessage = failMsg,
                    isRetryable = true
                )
            )
        }
    }

    fun sendMediaUri(
        context: android.content.Context,
        uri: android.net.Uri,
        caption: String = "",
        replyToMessageId: String? = null,
        explicitMessageType: String? = null
    ) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val isVideo = explicitMessageType == "video" || com.chroma.studio.chat.media.ChatMediaLimiter.isVideoUri(context, uri)
        val messageType = explicitMessageType ?: if (isVideo) "video" else "image"

        val maxLimit = if (isVideo) {
            com.chroma.studio.chat.media.ChatMediaLimiter.MAX_VIDEO_SIZE_BYTES
        } else {
            com.chroma.studio.chat.media.ChatMediaLimiter.MAX_FILE_SIZE_BYTES
        }
        val rawSize = com.chroma.studio.chat.media.ChatMediaLimiter.getMediaSizeBytes(context, uri)
        if (rawSize > maxLimit) {
            val formatted = com.chroma.studio.chat.media.ChatMediaLimiter.formatFileSize(rawSize)
            com.chroma.studio.ui.components.ToastManager.showToast("File size exceeds 100 MB limit ($formatted).")
            return
        }

        setTypingStatus(false)
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val previewStr = uri.toString()

        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = caption,
            timestamp = timestamp,
            isPending = true,
            uploadProgress = if (messageType == "image" || messageType == "video") 0 else null,
            messageType = messageType,
            imageUrl = previewStr,
            mediaUrl = previewStr,
            replyToMessageId = replyToMessageId
        )
        _localMessages.value = listOf(pendingMsg) + _localMessages.value

        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (_: Exception) {
        }

        val previewText = if (caption.isNotBlank()) caption else if (messageType == "video") "Video" else "Image"
        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = previewText,
            timestamp = timestamp
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (messageType == "image" || messageType == "video") {
                updateLocalUploadProgress(msgId, 5)
            }
            val bytes = if (isVideo) {
                com.chroma.studio.chat.media.ChatMediaLimiter.processAndCompressVideo(context, uri)
                    ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                com.chroma.studio.chat.media.ChatMediaLimiter.processAndCompressPhoto(context, uri)
                    ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            val extLower = if (isVideo) "mp4" else (com.chroma.studio.chat.media.MediaStoreHelper.getFileExtension(context, uri)?.lowercase() ?: "jpg")
            if (bytes != null) {
                if (messageType == "image" || messageType == "video") {
                    updateLocalUploadProgress(msgId, 12)
                }
                uploadAndCommitMedia(
                    roomId = roomId,
                    msgId = msgId,
                    mediaBytes = bytes,
                    extLower = extLower,
                    messageType = messageType,
                    text = caption,
                    replyToMessageId = replyToMessageId,
                    scaleMinPct = 12
                )
            } else {
                handleSendResult(
                    roomId = roomId,
                    msgId = msgId,
                    result = com.chroma.studio.chat.data.SendMessageResult.Failure(
                        reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                        userMessage = "Could not read selected media. Tap to retry.",
                        isRetryable = true
                    )
                )
            }
        }
    }

    fun sendMediaMessage(
        imageBytes: ByteArray,
        extension: String,
        text: String = "",
        replyToMessageId: String? = null,
        localPreviewUri: String? = null,
        explicitMessageType: String? = null
    ) {
        val extLower = extension.lowercase()
        val videoExts = setOf("mp4", "mov", "avi", "mkv", "3gp", "webm")
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp")
        val audioExts = setOf("mp3", "m4a", "wav", "aac", "ogg", "flac")
        val maxLimit = if (extLower in videoExts) {
            com.chroma.studio.chat.media.ChatMediaLimiter.MAX_VIDEO_SIZE_BYTES
        } else {
            com.chroma.studio.chat.media.ChatMediaLimiter.MAX_FILE_SIZE_BYTES
        }
        if (imageBytes.size > maxLimit) {
            val formatted = com.chroma.studio.chat.media.ChatMediaLimiter.formatFileSize(imageBytes.size.toLong())
            com.chroma.studio.ui.components.ToastManager.showToast("File size exceeds 100 MB limit ($formatted).")
            return
        }
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        setTypingStatus(false)
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val messageType = explicitMessageType ?: when (extLower) {
            in videoExts -> "video"
            in imageExts -> "image"
            in audioExts -> "audio"
            else -> "file"
        }

        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = text,
            timestamp = timestamp,
            isPending = true,
            uploadProgress = if (messageType == "image" || messageType == "video" || messageType == "file") 0 else null,
            messageType = messageType,
            imageUrl = localPreviewUri,
            mediaUrl = localPreviewUri,
            replyToMessageId = replyToMessageId,
            fileName = if (messageType == "file") text.takeIf { it.isNotBlank() } else null,
            fileSizeBytes = if (messageType == "file") imageBytes.size.toLong() else null,
            fileExtension = if (messageType == "file") extLower.uppercase() else null
        )
        // Add to local immediately
        _localMessages.value = listOf(pendingMsg) + _localMessages.value

        // Immediately persist to offline cache
        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                com.chroma.studio.ChromaApp.instance,
                roomId,
                pendingMsg
            )
        } catch (e: Exception) {
            // ignore
        }

        // Optimistically update recent chats list (resetting previous read/delivered flags)
        val previewText = if (messageType == "file") {
            com.chroma.studio.chat.data.formatFileRoomPreview(text, null)
        } else if (text.isNotBlank()) text else when (messageType) {
            "video" -> "Video"
            "sticker" -> "Sticker"
            "audio" -> "Audio"
            else -> "Image"
        }
        applyOptimisticRoomSend(
            roomId = roomId,
            msgId = msgId,
            currentUserId = currentUserId,
            previewText = previewText,
            timestamp = timestamp
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            uploadAndCommitMedia(
                roomId = roomId,
                msgId = msgId,
                mediaBytes = imageBytes,
                extLower = extLower,
                messageType = messageType,
                text = text,
                replyToMessageId = replyToMessageId,
                fileName = if (messageType == "file") text.takeIf { it.isNotBlank() } else null,
                fileSizeBytes = if (messageType == "file") imageBytes.size.toLong() else null
            )
        }
    }

    /**
     * Validates, caches optimistically, and uploads one or more picked files/documents
     * as structured `"file"` messages with real progress tracking and optional caption.
     */
    fun sendFileItems(
        context: android.content.Context,
        files: List<com.chroma.studio.chat.data.ChatFileItem>,
        caption: String = "",
        replyToMessageId: String? = null
    ) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (files.isEmpty()) return

        setTypingStatus(false)
        val cleanCaption = caption.trim()

        files.forEachIndexed { index, file ->
            val rawUri = file.uri ?: return@forEachIndexed
            val parsedUri = android.net.Uri.parse(rawUri)
            val probedSize = com.chroma.studio.chat.media.ChatMediaLimiter.getMediaSizeBytes(context, parsedUri)
            if (probedSize > com.chroma.studio.chat.media.ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                val formatted = com.chroma.studio.chat.media.ChatMediaLimiter.formatFileSize(probedSize)
                com.chroma.studio.ui.components.ToastManager.showToast("'${file.name}' exceeds 100 MB limit ($formatted).")
                return@forEachIndexed
            }

            val msgId = java.util.UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis() + index
            val extLower = file.extension.ifBlank {
                file.name.substringAfterLast('.', "bin")
            }.lowercase()
            val perFileCaption = if (index == files.lastIndex && cleanCaption.isNotBlank()) cleanCaption else ""

            val pendingMsg = ChatMessage(
                id = msgId,
                senderId = currentUserId,
                text = perFileCaption,
                timestamp = timestamp,
                isPending = true,
                uploadProgress = 0,
                messageType = "file",
                imageUrl = rawUri,
                mediaUrl = rawUri,
                replyToMessageId = if (index == 0) replyToMessageId else null,
                fileName = file.name,
                fileSizeBytes = probedSize.takeIf { it > 0L },
                fileExtension = extLower.uppercase()
            )
            _localMessages.value = listOf(pendingMsg) + _localMessages.value
            try {
                com.chroma.studio.chat.data.ChatLocalCacheManager.appendOrUpdateMessage(
                    com.chroma.studio.ChromaApp.instance,
                    roomId,
                    pendingMsg
                )
            } catch (_: Exception) {}

            applyOptimisticRoomSend(
                roomId = roomId,
                msgId = msgId,
                currentUserId = currentUserId,
                previewText = com.chroma.studio.chat.data.formatFileRoomPreview(file.name, null),
                timestamp = timestamp
            )

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                updateLocalUploadProgress(msgId, 5)
                val bytes = try {
                    if (parsedUri.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
                        java.io.File(parsedUri.path!!).takeIf { it.exists() }?.readBytes()
                    } else {
                        context.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
                    }
                } catch (_: Exception) {
                    null
                }

                if (bytes == null || bytes.isEmpty()) {
                    handleSendResult(
                        roomId = roomId,
                        msgId = msgId,
                        result = com.chroma.studio.chat.data.SendMessageResult.Failure(
                            reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                            userMessage = "Could not read '${file.name}'.",
                            isRetryable = true
                        )
                    )
                    return@launch
                }

                if (bytes.size.toLong() > com.chroma.studio.chat.media.ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                    val formatted = com.chroma.studio.chat.media.ChatMediaLimiter.formatFileSize(bytes.size.toLong())
                    handleSendResult(
                        roomId = roomId,
                        msgId = msgId,
                        result = com.chroma.studio.chat.data.SendMessageResult.Failure(
                            reason = com.chroma.studio.chat.data.SendFailureReason.MEDIA_UPLOAD_FAILED,
                            userMessage = "'${file.name}' exceeds 100 MB limit ($formatted).",
                            isRetryable = false
                        )
                    )
                    return@launch
                }

                val resolvedByteSize = bytes.size.toLong()
                if (_activeRoomId.value == roomId && pendingMsg.fileSizeBytes == null) {
                    _localMessages.value = _localMessages.value.map {
                        if (it.id == msgId) it.copy(fileSizeBytes = resolvedByteSize) else it
                    }
                }

                updateLocalUploadProgress(msgId, 10)
                uploadAndCommitMedia(
                    roomId = roomId,
                    msgId = msgId,
                    mediaBytes = bytes,
                    extLower = extLower,
                    messageType = "file",
                    text = perFileCaption,
                    replyToMessageId = if (index == 0) replyToMessageId else null,
                    scaleMinPct = 10,
                    fileName = file.name,
                    fileSizeBytes = resolvedByteSize
                )
            }
        }
    }

    fun closeChat() {
        // Clear any active typing/recording indicators before leaving
        activityPresenceController.onPauseOrBackground()
        typingDebounceJob?.cancel()
        typingHeartbeatJob?.cancel()
        typingHeartbeatJob = null
        recordingHeartbeatJob?.cancel()
        lastTypingSendTime = 0L
        if (currentActivityStatus != null) {
            setChatActivity(null)
        }
        _activeRoomId.value = null
        com.chroma.studio.ui.components.DynamicIslandManager.currentlyActiveRoomId = null
        com.chroma.studio.ChatStateManager.clearActiveChat()
        messagesJob?.cancel()
        _serverMessages.value = emptyList()
        _localMessages.value = emptyList()
    }

    fun markRoomAsRead(roomId: String) {
        markMultipleAsRead(listOf(roomId))
    }



    suspend fun uploadGroupImageBytes(imageBytes: ByteArray, extension: String): String? {
        return try {
            val compressed = com.chroma.studio.utils.ImageUtils.compressAvatarBytesForSupabase(imageBytes) ?: imageBytes
            repository.uploadImageToSupabase(compressed, extension)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    fun uploadGroupImage(roomId: String, imageBytes: ByteArray, extension: String) {
        viewModelScope.launch {
            try {
                val compressed = com.chroma.studio.utils.ImageUtils.compressAvatarBytesForSupabase(imageBytes) ?: imageBytes
                repository.uploadGroupImageAndUpdate(roomId, compressed, extension)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun updateGroupName(roomId: String, name: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(name = name)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateGroupName(roomId, name) }
    }

    fun updateGroupImage(roomId: String, imageUrl: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(avatarUrl = imageUrl)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateGroupImage(roomId, imageUrl) }
    }

    fun updateGroupDescription(roomId: String, description: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(groupDescription = description)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateGroupDescription(roomId, description) }
    }

    fun updateOnlyAdminsCanPost(roomId: String, onlyAdminsCanPost: Boolean) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(onlyAdminsCanPost = onlyAdminsCanPost)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateOnlyAdminsCanPost(roomId, onlyAdminsCanPost) }
    }

    fun updateDisappearingHours(roomId: String, hours: Int) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(disappearingHours = hours)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateDisappearingHours(roomId, hours) }
    }

    fun updateInviteCode(roomId: String, inviteCode: String?) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(inviteCode = inviteCode)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateInviteCode(roomId, inviteCode) }
    }

    fun updateEditInfoAdminsOnly(roomId: String, adminsOnly: Boolean) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            currentChats[index] = currentChats[index].copy(editInfoAdminsOnly = adminsOnly)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.updateEditInfoAdminsOnly(roomId, adminsOnly) }
    }

    fun joinGroupByInviteCode(codeOrRoomId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val room = repository.joinGroupByInviteCode(codeOrRoomId)
            if (room != null) {
                val list = _recentChats.value.toMutableList()
                val idx = list.indexOfFirst { it.id == room.id }
                if (idx != -1) list[idx] = room else list.add(0, room)
                _recentChats.value = list
                fetchProfilesForUsers(room.participants)
                openChat(room.id)
                onResult(room.id)
            } else {
                onResult(null)
            }
        }
    }

    fun demoteAdmin(roomId: String, userId: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats[index]
            currentChats[index] = chat.copy(admins = chat.admins.filter { it != userId })
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.demoteAdmin(roomId, userId) }
    }

    fun addParticipants(roomId: String, userIds: List<String>) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats[index]
            val newParticipants = (chat.participants + userIds).distinct()
            currentChats[index] = chat.copy(participants = newParticipants)
            _recentChats.value = currentChats
        }
        fetchProfilesForUsers(userIds)
        viewModelScope.launch { repository.addParticipants(roomId, userIds) }
    }

    fun removeParticipant(roomId: String, userId: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats[index]
            val newParticipants = chat.participants.filter { it != userId }
            val newAdmins = chat.admins.filter { it != userId }
            currentChats[index] = chat.copy(participants = newParticipants, admins = newAdmins)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.removeParticipant(roomId, userId) }
    }

    fun makeAdmin(roomId: String, userId: String) {
        // Optimistic UI update for admin
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats[index]
            if (!chat.admins.contains(userId)) {
                currentChats[index] = chat.copy(admins = chat.admins + userId)
                _recentChats.value = currentChats
            }
        }
        viewModelScope.launch { repository.makeAdmin(roomId, userId) }
    }

    fun leaveGroup(roomId: String) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1 && currentUserId != null) {
            val chat = currentChats[index]
            val newParticipants = chat.participants.filter { it != currentUserId }
            val newAdmins = chat.admins.filter { it != currentUserId }
            currentChats[index] = chat.copy(participants = newParticipants, admins = newAdmins)
            _recentChats.value = currentChats
        }
        viewModelScope.launch { repository.leaveGroup(roomId) }
    }

    fun deleteChat(roomId: String) {
        _recentChats.value = _recentChats.value.filter { it.id != roomId }
        viewModelScope.launch {
            try { repository.deleteChat(roomId) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }


    private fun loadMutedChats() {
        viewModelScope.launch {
            try {
                val muted = repository.loadMutedChatIds()
                _mutedChatIds.value = muted
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun muteChat(roomId: String, mute: Boolean) {
        val current = _mutedChatIds.value.toMutableSet()
        if (mute) current.add(roomId) else current.remove(roomId)
        _mutedChatIds.value = current
        viewModelScope.launch {
            try { repository.muteChat(roomId, mute) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val revert = _mutedChatIds.value.toMutableSet()
                if (mute) revert.remove(roomId) else revert.add(roomId)
                _mutedChatIds.value = revert
            }
        }
    }

    fun deleteMultipleChats(roomIds: List<String>) {
        _recentChats.value = _recentChats.value.filter { it.id !in roomIds }
        viewModelScope.launch {
            roomIds.forEach { id ->
                try { repository.deleteChat(id) } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }
            }
        }
    }

    fun muteMultipleChats(roomIds: List<String>, mute: Boolean) {
        val current = _mutedChatIds.value.toMutableSet()
        if (mute) current.addAll(roomIds) else current.removeAll(roomIds.toSet())
        _mutedChatIds.value = current
        viewModelScope.launch {
            roomIds.forEach { id ->
                try { repository.muteChat(id, mute) } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }
            }
        }
    }

    fun toggleFavoriteMultiple(roomIds: List<String>, favorite: Boolean) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentChats = _recentChats.value.toMutableList()
        roomIds.forEach { id ->
            val index = currentChats.indexOfFirst { it.id == id }
            if (index != -1) {
                val chat = currentChats[index]
                val newFavorites = if (favorite) chat.favoritedBy + currentUserId else chat.favoritedBy - currentUserId
                currentChats[index] = chat.copy(favoritedBy = newFavorites)
            }
        }
        _recentChats.value = currentChats
        viewModelScope.launch {
            roomIds.forEach { id ->
                repository.toggleFavorite(id, currentUserId, favorite)
            }
        }
    }

    fun markAsRead(roomId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAsRead(roomId)
        }
    }

    fun markAsDelivered(roomId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAsDelivered(roomId)
        }
    }

    fun markMultipleAsRead(roomIds: List<String>) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val currentChats = _recentChats.value.toMutableList()
        roomIds.forEach { id ->
            val index = currentChats.indexOfFirst { it.id == id }
            if (index != -1) {
                val chat = currentChats[index]
                val updatedUnread = chat.unreadCount.toMutableMap()
                updatedUnread[currentUser.uid] = 0
                currentChats[index] = chat.copy(unreadCount = updatedUnread)
            }
        }
        _recentChats.value = currentChats
        viewModelScope.launch {
            roomIds.forEach { id ->
                repository.markAsRead(id)
            }
        }
    }

    fun archiveChat(roomId: String, archive: Boolean = true) {
        _recentChats.value = _recentChats.value.filter { it.id != roomId }
        viewModelScope.launch {
            try {
                repository.archiveChat(roomId, archive)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            _blockedUserIds.value = repository.loadBlockedUsers()
        }
    }

    fun toggleBlockUser(targetUserId: String, isBlocked: Boolean) {
        val current = _blockedUserIds.value.toMutableSet()
        if (isBlocked) current.add(targetUserId) else current.remove(targetUserId)
        _blockedUserIds.value = current
        viewModelScope.launch {
            repository.blockUser(targetUserId, isBlocked)
        }
    }

    fun reportUser(reportedUserId: String?, roomId: String, reason: String = "Inappropriate content", snippetMessages: List<String> = emptyList()) {
        viewModelScope.launch {
            repository.reportUser(reportedUserId, roomId, reason, snippetMessages)
        }
    }

    fun pinMessage(roomId: String, messageId: String, messageText: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val room = currentChats[index]
            currentChats[index] = room.copy(
                pinnedMessageId = messageId,
                pinnedMessageText = messageText,
                pinnedMessageBy = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                pinnedAt = System.currentTimeMillis()
            )
            _recentChats.value = currentChats
        }
        viewModelScope.launch {
            repository.pinMessage(roomId, messageId, messageText)
        }
    }


    var callManager: com.chroma.studio.webrtc.CallManager? = null
        private set

    val incomingCall: kotlinx.coroutines.flow.StateFlow<com.chroma.studio.webrtc.CallModel?>
        get() = callManager?.incomingCall ?: kotlinx.coroutines.flow.MutableStateFlow<com.chroma.studio.webrtc.CallModel?>(null)

    val activeCall: kotlinx.coroutines.flow.StateFlow<com.chroma.studio.webrtc.CallModel?>
        get() = callManager?.activeCall ?: kotlinx.coroutines.flow.MutableStateFlow<com.chroma.studio.webrtc.CallModel?>(null)

    val callState: kotlinx.coroutines.flow.StateFlow<com.chroma.studio.webrtc.CallState>
        get() = callManager?.callState ?: kotlinx.coroutines.flow.MutableStateFlow<com.chroma.studio.webrtc.CallState>(com.chroma.studio.webrtc.CallState.IDLE)

    val callDurationSec: kotlinx.coroutines.flow.StateFlow<Int>
        get() = callManager?.callDurationSec ?: kotlinx.coroutines.flow.MutableStateFlow(0)

    fun initializeCallManager(context: android.content.Context) {
        val mgr = callManager ?: com.chroma.studio.webrtc.CallManager.getInstance(context.applicationContext).also {
            callManager = it
        }
        mgr.onCallEndedListener = { call, durationSec, status ->
            logCallHistory(call, durationSec, status)
        }
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            mgr.startListeningForIncomingCalls(uid)
        }
    }

    private fun logCallHistory(call: com.chroma.studio.webrtc.CallModel, durationSec: Int, status: String) {
        val targetRoomId = call.roomId.takeIf { it.isNotBlank() } ?: return
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Only the caller logs the call message to Firestore to prevent duplicates
        if (call.callerId != currentUid) return

        viewModelScope.launch {
            try {
                val callTypeStr = if (call.isVideo) "Video" else "Audio"
                val durationStr = when (status) {
                    "missed" -> "Missed"
                    "rejected" -> "Declined"
                    "busy" -> "Busy"
                    else -> {
                        val m = durationSec / 60
                        val s = durationSec % 60
                        if (m > 0) "${m}m ${s}s" else "${s}s"
                    }
                }
                val textSnippet = when (status) {
                    "missed" -> "Missed $callTypeStr call"
                    "rejected" -> "Declined $callTypeStr call"
                    "busy" -> "$callTypeStr call • Busy"
                    else -> "$callTypeStr call ended • $durationStr"
                }

                repository.sendMessage(
                    roomId = targetRoomId,
                    text = textSnippet,
                    messageType = "call",
                    mediaType = "call",
                    callDuration = durationStr,
                    callType = callTypeStr
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("ChatViewModel", "Error logging call history", e)
            }
        }
    }

    fun startCall(
        roomId: String,
        otherUserId: String,
        otherUserName: String,
        otherUserAvatar: String,
        isVideo: Boolean
    ) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val currentProfile = userProfiles.value[user.uid]
        callManager?.startCall(
            roomId = roomId,
            callerId = user.uid,
            callerName = currentProfile?.displayName ?: user.displayName ?: "User",
            callerAvatar = currentProfile?.avatarUrl ?: "",
            calleeId = otherUserId,
            calleeName = otherUserName,
            calleeAvatar = otherUserAvatar,
            isVideo = isVideo
        )
    }

    fun answerCall(call: com.chroma.studio.webrtc.CallModel) {
        callManager?.answerCall(call)
    }

    fun rejectCall(call: com.chroma.studio.webrtc.CallModel) {
        callManager?.rejectCall(call)
    }

    fun toggleMute(): Boolean {
        return callManager?.toggleMicrophone() ?: false
    }

    fun toggleCamera(): Boolean {
        return callManager?.toggleCamera() ?: false
    }

    fun toggleSpeaker(): Boolean {
        return callManager?.toggleSpeaker() ?: false
    }

    fun switchCamera() {
        callManager?.switchCamera()
    }

    fun endCall(reason: String = "ended") {
        callManager?.endCall(reason)
    }

    fun unpinMessage(roomId: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val room = currentChats[index]
            currentChats[index] = room.copy(
                pinnedMessageId = null,
                pinnedMessageText = null,
                pinnedMessageBy = null,
                pinnedAt = 0L
            )
            _recentChats.value = currentChats
        }
        viewModelScope.launch {
            repository.pinMessage(roomId, null, null)
        }
    }

    fun clearChatHistory(roomId: String) {
        if (_activeRoomId.value == roomId) {
            _serverMessages.value = emptyList()
            _localMessages.value = emptyList()
        }
        try {
            com.chroma.studio.chat.data.ChatLocalCacheManager.clearRoom(
                com.chroma.studio.ChromaApp.instance,
                roomId
            )
        } catch (e: Exception) {
            // ignore
        }
        viewModelScope.launch {
            try {
                repository.clearChatHistory(roomId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun forwardMessage(message: ChatMessage, targetRoomId: String) {
        viewModelScope.launch {
            try {
                val mediaUrl = message.imageUrl ?: message.mediaUrl
                val msgType = if (message.messageType.isNotBlank()) message.messageType else if (mediaUrl != null) "image" else "text"
                repository.sendMessage(
                    roomId = targetRoomId,
                    text = message.text,
                    imageUrl = mediaUrl,
                    messageType = msgType,
                    isForwarded = true
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { activityPresenceController.onPauseOrBackground() } catch (_: Exception) {}
        authStateListener?.let { listener ->
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(listener)
            } catch (_: Exception) {}
        }
        authStateListener = null

        currentOtherUserId?.let {
            onlineStatusListener?.let { listener ->
                com.chroma.studio.data.OnlineStatusManager.getDatabase().getReference("users/$it").removeEventListener(listener)
            }
        }
        // Clean up multi-user presence listeners
        for (unsubscribe in presenceListeners.values) {
            try { unsubscribe() } catch (_: Exception) {}
        }
        presenceListeners.clear()

        // Clean up active jobs and listeners
        typingDebounceJob?.cancel()
        recordingHeartbeatJob?.cancel()
        statusTtlMonitorJob?.cancel()

        // Clean up all per-room typing listeners
        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        for ((roomId, listener) in typingListeners) {
            db.getReference("status/$roomId").removeEventListener(listener)
        }
        typingListeners.clear()
        receivedActivityTimestamps.clear()

        // Clear own activity status on exit
        val activeRoom = _activeRoomId.value
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (activeRoom != null && uid != null) {
            db.getReference("status/$activeRoom/$uid").removeValue()
        }
    }

    private fun startStatusTtlMonitor() {
        if (statusTtlMonitorJob != null) return
        statusTtlMonitorJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(2000)
                val now = System.currentTimeMillis()
                var changed = false
                val currentMap = _typingStatuses.value.toMutableMap()

                for ((roomId, roomActivities) in currentMap.toList()) {
                    val roomTimestamps = receivedActivityTimestamps.get(roomId) ?: continue
                    val filtered = roomActivities.filter { (uid, _) ->
                        val lastSeen = roomTimestamps[uid] ?: 0L
                        (now - lastSeen) <= 7000L // Expire stale indicators after 7s (matches 5s sender heartbeat)
                    }
                    if (filtered.size != roomActivities.size) {
                        changed = true
                        if (filtered.isNotEmpty()) {
                            currentMap[roomId] = filtered
                        } else {
                            currentMap.remove(roomId)
                        }
                    }
                }
                if (changed) {
                    _typingStatuses.value = currentMap
                }
            }
        }
    }

    fun observeRoomTyping(rawRoomId: String) {
        if (rawRoomId.isBlank()) return
        startStatusTtlMonitor()
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val roomId = com.chroma.studio.data.OnlineStatusManager.normalizeRoomId(rawRoomId, currentUid, currentOtherUserId)
        if (typingListeners.containsKey(roomId)) return
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        val roomRef = db.getReference("status/$roomId")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val activityMap = mutableMapOf<String, String>()
                val now = System.currentTimeMillis()
                val roomTimestamps = receivedActivityTimestamps.getOrPut(roomId) { java.util.concurrent.ConcurrentHashMap() }

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val status = when (val value = userSnapshot.value) {
                        is String -> value
                        is Map<*, *> -> value["status"] as? String ?: value["activity"] as? String
                        else -> null
                    }
                    val compositeKey = "$roomId:$userId"
                    if (!status.isNullOrBlank()) {
                        activityMap[userId] = status
                        roomTimestamps[userId] = now
                        // WhatsApp 5-second per-participant fallback expiry timer
                        recipientExpiryJobs[compositeKey]?.cancel()
                        recipientExpiryJobs[compositeKey] = viewModelScope.launch {
                            kotlinx.coroutines.delay(5000L)
                            evictUserTypingActivity(roomId, userId)
                            evictUserTypingActivity(rawRoomId, userId)
                        }
                    } else {
                        roomTimestamps.remove(userId)
                        recipientExpiryJobs.remove(compositeKey)?.cancel()
                    }
                }
                val newStatus = if (activityMap.isNotEmpty()) {
                    _typingStatuses.value + (rawRoomId to activityMap) + (roomId to activityMap)
                } else {
                    _typingStatuses.value - rawRoomId - roomId
                }
                _typingStatuses.value = newStatus
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.d("ChatViewModel", "Typing status cancelled for $roomId: ${error.message}")
            }
        }
        roomRef.addValueEventListener(listener)
        typingListeners[roomId] = listener
    }

    private fun observeTypingStatuses(chats: List<ChatRoom>) {
        startStatusTtlMonitor()
        val listeners = typingListeners ?: return
        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        val currentRoomIds = chats.map { it.id }.toSet()

        // Remove listeners for rooms we're no longer in, keeping active room alive
        val activeId = _activeRoomId.value
        val removedRooms = listeners.keys.filter { it !in currentRoomIds && it != activeId }
        for (roomId in removedRooms) {
            listeners.remove(roomId)?.let {
                try {
                    db.getReference("status/$roomId").removeEventListener(it)
                } catch (e: Exception) {
                    // ignore
                }
            }
            receivedActivityTimestamps?.remove(roomId)
            _typingStatuses.value = _typingStatuses.value - roomId
        }

        // Add listeners for current rooms
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        for (roomId in currentRoomIds) {
            observeRoomTyping(roomId)
        }
    }


    fun votePoll(messageId: String, optionIndex: Int, allowMultiple: Boolean = false) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) {
                    val currentVotes = msg.pollVotes.toMutableMap()
                    val userCurrentVote = currentVotes[currentUserId] ?: ""
                    val userIndices = if (userCurrentVote.isEmpty()) mutableListOf() else userCurrentVote.split(",").filter { it.isNotBlank() }.toMutableList()
                    val optStr = optionIndex.toString()
                    if (userIndices.contains(optStr)) {
                        userIndices.remove(optStr)
                    } else {
                        if (!allowMultiple) userIndices.clear()
                        userIndices.add(optStr)
                    }
                    if (userIndices.isEmpty()) {
                        currentVotes.remove(currentUserId)
                    } else {
                        currentVotes[currentUserId] = userIndices.joinToString(",")
                    }
                    msg.copy(pollVotes = currentVotes)
                } else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local apply (0ms perceived latency)
        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.votePoll(roomId, messageId, optionIndex, allowMultiple)
            if (!success) {
                // Graceful rollback
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not record vote.")
            }
        }
    }


    fun retractPollVote(messageId: String) {
        val roomId = _activeRoomId.value ?: return
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) {
                    val currentVotes = msg.pollVotes.toMutableMap()
                    currentVotes.remove(currentUserId)
                    msg.copy(pollVotes = currentVotes)
                } else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.retractPollVote(roomId, messageId)
            if (!success) {
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not retract vote.")
            }
        }
    }

    fun closePoll(messageId: String) {
        val roomId = _activeRoomId.value ?: return

        fun updateList(list: List<ChatMessage>): List<ChatMessage> {
            return list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(isPollClosed = true)
                } else msg
            }
        }

        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        _serverMessages.value = updateList(_serverMessages.value)
        _localMessages.value = updateList(_localMessages.value)

        viewModelScope.launch {
            val success = repository.closePoll(roomId, messageId)
            if (!success) {
                _serverMessages.value = prevServer
                _localMessages.value = prevLocal
                com.chroma.studio.ui.components.ToastManager.showToast("Could not close poll.")
            }
        }
    }
}
