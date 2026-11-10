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

    val currentMessages: StateFlow<List<ChatMessage>> = combine(_serverMessages, _localMessages) { server, local ->
        val serverIds = server.map { it.id }.toSet()
        val remainingLocal = local.filter { it.id !in serverIds }
        (server + remainingLocal).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

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
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            _userProfiles.value = currentMap
        }
    }

    private var chatsJob: Job? = null
    private var messagesJob: Job? = null
    private var storiesJob: Job? = null
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
        // Immediately load recent chats from local storage so conversation list appears on Frame 0 offline
        try {
            val cachedChats = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(
                com.chroma.studio.ChromaApp.instance
            )
            if (cachedChats.isNotEmpty()) {
                _recentChats.value = cachedChats
                _isLoadingChats.value = false
            }
        } catch (_: Exception) {}

        val initialUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val initialUid = initialUser?.uid
        if (!initialUid.isNullOrBlank()) {
            currentAuthUserId = initialUid
            reloadForUser(initialUid)
        } else {
            _isLoadingChats.value = false
        }

        authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            val newUid = auth.currentUser?.uid
            if (newUid != currentAuthUserId) {
                val oldUid = currentAuthUserId
                currentAuthUserId = newUid
                if (!newUid.isNullOrBlank()) {
                    reloadForUser(newUid)
                } else if (oldUid != null) {
                    resetForLogout()
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
            try {
                val unsubscribe = com.chroma.studio.data.OnlineStatusManager.observeUserPresence(uid) { presence ->
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
        val ref = com.chroma.studio.data.OnlineStatusManager.getDatabase().getReference("users/$uid")

        onlineStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastOnline").getValue(Long::class.java)
                    ?: snapshot.child("lastSeen").getValue(Long::class.java)
                _isOtherUserOnline.value = isOnline
                _otherUserLastSeen.value = lastSeen
                _onlineUsersPresence.value = _onlineUsersPresence.value + (uid to com.chroma.studio.data.UserPresence(isOnline, lastSeen))
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
                repository.observeRecentChats(currentUid).collect { chats ->
                    if (chats.isNotEmpty()) {
                        _recentChats.value = chats
                        _isLoadingChats.value = false
                        try {
                            com.chroma.studio.chat.data.ChatLocalCacheManager.saveRecentChats(
                                com.chroma.studio.ChromaApp.instance,
                                chats
                            )
                        } catch (_: Exception) {}
                    } else if (_recentChats.value.isEmpty()) {
                        _recentChats.value = emptyList()
                        _isLoadingChats.value = false
                    }

                    // Pre-load top recent chat messages into memory cache in background (0ms open)
                    try {
                        com.chroma.studio.chat.data.ChatLocalCacheManager.preloadRooms(
                            com.chroma.studio.ChromaApp.instance,
                            chats.map { it.id }
                        )
                    } catch (e: Exception) {
                        // ignore
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
                                repository.markAsDelivered(chat.id)
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

        val isSameUser = currentAuthUserId == uid
        val isAlreadyObserving = chatsJob?.isActive == true && _recentChats.value.isNotEmpty()
        if (isSameUser && isAlreadyObserving) {
            return
        }

        currentAuthUserId = uid
        com.chroma.studio.ChatStateManager.setCurrentUserId(uid)

        observeChats(uid)
        loadStories()
        loadMutedChats()
        loadBlockedUsers()

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
        chatsJob?.cancel()
        chatsJob = null
        messagesJob?.cancel()
        messagesJob = null
        storiesJob?.cancel()
        storiesJob = null

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
        _activeRoomId.value = null
        _isLoadingChats.value = false
        _isOtherUserOnline.value = false
        _otherUserLastSeen.value = null
        _onlineUsersPresence.value = emptyMap()
        _stories.value = emptyList()
        _userProfiles.value = emptyMap()
        _typingStatuses.value = emptyMap()
        _blockedUserIds.value = emptySet()
        _mutedChatIds.value = emptySet()

        repository.clearCaches()
        com.chroma.studio.chat.data.ChatLocalCacheManager.clearMemoryCache()
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

        openChat(roomId)
        onRoomReady(roomId)
        fetchProfilesForUsers(listOf(currentUser.uid, otherUserId))

        // Fetch profiles immediately & do real creation in background
        viewModelScope.launch {
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
        }
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
            com.chroma.studio.ChromaApp.instance,
            roomId
        )
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

        // 2. Viewport limit: start with 30 messages or size of cached
        messageLimit = 30L.coerceAtLeast(cached.size.toLong())
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

        viewModelScope.launch {
            repository.markAsRead(roomId)
            try {
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid != null) {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userDoc = firestore.collection("users").document(currentUid).get().await()
                    if (userDoc.getString("publicKey").isNullOrBlank()) {
                        val pubKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                        firestore.collection("users").document(currentUid).update("publicKey", pubKey)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun loadMoreMessages() {
        val roomId = _activeRoomId.value ?: return
        messageLimit += 30L
        observeMessages(roomId)
    }

    fun createGroup(groupName: String, groupDescription: String? = null, participantIds: List<String>, imageUrl: String? = null, preGeneratedRoomId: String? = null, onRoomReady: (String) -> Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val roomId = preGeneratedRoomId ?: "group_${java.util.UUID.randomUUID()}"

        // Optimistic UI update
        val allUsers = participantIds.toMutableList()
        if (!allUsers.contains(currentUser.uid)) allUsers.add(currentUser.uid)

        val optimisticRoom = ChatRoom(
            id = roomId,
            name = groupName,
            groupDescription = groupDescription,
            isGroup = true,
            avatarUrl = imageUrl,
            participants = allUsers,
            admins = listOf(currentUser.uid),
            createdBy = currentUser.uid,
            lastMessage = "Group created",
            lastMessageTime = System.currentTimeMillis()
        )
        val currentChats = _recentChats.value.toMutableList()
        currentChats.add(0, optimisticRoom)
        _recentChats.value = currentChats

        openChat(roomId)
        onRoomReady(roomId)
        fetchProfilesForUsers(allUsers)

        viewModelScope.launch {
            repository.createGroup(groupName, groupDescription, participantIds, imageUrl, roomId)
        }
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
            repository.observeMessages(roomId, messageLimit).collect { msgs ->
                // CRITICAL OFFLINE PROTECTION:
                // Only update server messages if new messages arrived from server or Firestore cache.
                // Never wipe out non-empty cached messages if an empty list arrives during offline transitions.
                if (msgs.isNotEmpty()) {
                    _serverMessages.value = msgs
                    // WhatsApp-grade immediate eviction of typing indicator on message arrival
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserId != null) {
                        val otherSenders = msgs.map { it.senderId }.filter { it != currentUserId }.toSet()
                        otherSenders.forEach { senderId ->
                            evictUserTypingActivity(roomId, senderId)
                        }

                        // Auto-mark incoming messages as read when user is actively viewing this room, or delivered otherwise
                        if (_activeRoomId.value == roomId) {
                            val hasUnreadFromOther = msgs.any { it.senderId != currentUserId && !it.isRead }
                            if (hasUnreadFromOther) {
                                launch(Dispatchers.IO) {
                                    repository.markAsRead(roomId)
                                }
                            }
                        } else {
                            val hasUndeliveredFromOther = msgs.any { it.senderId != currentUserId && !it.isDelivered }
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
                            com.chroma.studio.ChromaApp.instance,
                            roomId,
                            msgs
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                } else if (_serverMessages.value.isEmpty()) {
                    _serverMessages.value = emptyList()
                }
            }
        }
    }

    fun sendMessage(
        text: String,
        workId: String? = null,
        replyToMessageId: String? = null,
        storyId: String? = null,
        storyThumbnailUrl: String? = null
    ) {
        val roomId = _activeRoomId.value ?: return
        sendMessageToRoom(roomId, text, workId, replyToMessageId, storyId, storyThumbnailUrl)
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

    fun sendMessageToRoom(
        roomId: String,
        text: String,
        workId: String? = null,
        replyToMessageId: String? = null,
        storyId: String? = null,
        storyThumbnailUrl: String? = null
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
            storyThumbnailUrl = storyThumbnailUrl
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

        // Optimistically update recent chats list
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats.removeAt(index)
            val previewText = when {
                storyThumbnailUrl != null || storyId != null -> "Replied to story: $text"
                workId != null -> "Shared a work"
                else -> text
            }
            currentChats.add(0, chat.copy(lastMessage = previewText, lastMessageTime = timestamp))
            _recentChats.value = currentChats
        }

        viewModelScope.launch {
            val success = repository.sendMessage(
                roomId = roomId,
                text = text,
                workId = workId,
                preGeneratedId = msgId,
                replyToMessageId = replyToMessageId,
                messageType = resolvedMessageType,
                storyId = storyId,
                storyThumbnailUrl = storyThumbnailUrl
            )
            if (!success && _activeRoomId.value == roomId) {
                // Mark as failed
                _localMessages.value = _localMessages.value.map {
                    if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                }
            }
        }
    }

    fun retryMessage(msgId: String) {
        val roomId = _activeRoomId.value ?: return
        val targetMsg = _localMessages.value.firstOrNull { it.id == msgId } ?: return
        if (!targetMsg.isError) return

        // Set back to pending
        _localMessages.value = _localMessages.value.map {
            if (it.id == msgId) it.copy(isPending = true, isError = false) else it
        }

        viewModelScope.launch {
            val success = when (targetMsg.messageType) {
                "audio" -> {
                    val mediaUrl = targetMsg.mediaUrl ?: targetMsg.imageUrl
                    if (mediaUrl != null && (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://"))) {
                        repository.sendMessage(
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
                            repository.sendMessage(
                                roomId = roomId,
                                text = targetMsg.text,
                                imageUrl = uploadedUrl,
                                preGeneratedId = msgId,
                                messageType = "audio",
                                durationSeconds = targetMsg.durationSeconds,
                                replyToMessageId = targetMsg.replyToMessageId
                            )
                        } else false
                    } else false
                }
                "image", "video" -> {
                    val mediaUrl = targetMsg.mediaUrl ?: targetMsg.imageUrl
                    if (mediaUrl != null && (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://"))) {
                        repository.sendMessage(
                            roomId = roomId,
                            text = targetMsg.text,
                            imageUrl = mediaUrl,
                            preGeneratedId = msgId,
                            messageType = targetMsg.messageType,
                            replyToMessageId = targetMsg.replyToMessageId
                        )
                    } else if (mediaUrl != null && java.io.File(mediaUrl).exists()) {
                        val bytes = java.io.File(mediaUrl).readBytes()
                        val ext = if (targetMsg.messageType == "video") "mp4" else "jpg"
                        val uploadedUrl = if (targetMsg.messageType == "video") {
                            repository.uploadVideoToSupabase(bytes, ext)
                        } else {
                            repository.uploadImageToSupabase(bytes, ext)
                        }
                        if (uploadedUrl != null) {
                            repository.sendMessage(
                                roomId = roomId,
                                text = targetMsg.text,
                                imageUrl = uploadedUrl,
                                preGeneratedId = msgId,
                                messageType = targetMsg.messageType,
                                replyToMessageId = targetMsg.replyToMessageId
                            )
                        } else false
                    } else false
                }
                else -> {
                    repository.sendMessage(
                        roomId,
                        targetMsg.text,
                        workId = targetMsg.workId,
                        preGeneratedId = msgId,
                        replyToMessageId = targetMsg.replyToMessageId
                    )
                }
            }
            if (!success) {
                _localMessages.value = _localMessages.value.map {
                    if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                }
                com.chroma.studio.ui.components.ToastManager.showToast("Message failed to send. Tap to retry.")
            }
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

        // Optimistically update recent chats list
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats.removeAt(index)
            val durationText = String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
            currentChats.add(0, chat.copy(lastMessage = "🎤 Voice message ($durationText)", lastMessageTime = timestamp))
            _recentChats.value = currentChats
        }

        viewModelScope.launch {
            val audioUrl = repository.uploadAudioToSupabase(audioBytes, "m4a")
            if (audioUrl != null) {
                val success = repository.sendMessage(
                    roomId = roomId,
                    text = "",
                    imageUrl = audioUrl,
                    preGeneratedId = msgId,
                    messageType = "audio",
                    durationSeconds = durationSeconds,
                    replyToMessageId = replyToMessageId
                )
                if (!success) {
                    _localMessages.value = _localMessages.value.map {
                        if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                    }
                }
            } else {
                _localMessages.value = _localMessages.value.map {
                    if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                }
            }
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

    fun sendMediaMessage(imageBytes: ByteArray, extension: String, text: String = "", replyToMessageId: String? = null, localPreviewUri: String? = null) {
        val maxLimit = if (extension.lowercase() in listOf("mp4", "mov", "avi", "mkv", "3gp", "webm")) {
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
        val messageType = if (extension.lowercase() in listOf("mp4", "mov", "avi", "mkv")) "video" else "image"

        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = text,
            timestamp = timestamp,
            isPending = true,
            messageType = messageType,
            imageUrl = localPreviewUri,
            mediaUrl = localPreviewUri,
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

        // Optimistically update recent chats list
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val chat = currentChats.removeAt(index)
            val previewText = if (text.isNotBlank()) text else if (messageType == "video") "Video" else "Image"
            currentChats.add(0, chat.copy(lastMessage = previewText, lastMessageTime = timestamp))
            _recentChats.value = currentChats
        }

        viewModelScope.launch {
            // Compress image bytes before uploading to save storage & bandwidth
            val compressedBytes = if (messageType == "image") {
                com.chroma.studio.utils.ImageUtils.compressChatImageBytes(imageBytes) ?: imageBytes
            } else {
                imageBytes
            }
            val mediaUrl = repository.uploadImageToSupabase(compressedBytes, extension)
            if (mediaUrl != null) {
                val success = repository.sendMessage(roomId, text, imageUrl = mediaUrl, preGeneratedId = msgId, messageType = messageType, replyToMessageId = replyToMessageId)
                if (!success) {
                    _localMessages.value = _localMessages.value.map {
                        if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                    }
                }
            } else {
                _localMessages.value = _localMessages.value.map {
                    if (it.id == msgId) it.copy(isPending = false, isError = true) else it
                }
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
        viewModelScope.launch { repository.updateGroupName(roomId, name) }
    }

    fun updateGroupImage(roomId: String, imageUrl: String) {
        viewModelScope.launch { repository.updateGroupImage(roomId, imageUrl) }
    }

    fun updateGroupDescription(roomId: String, description: String) {
        viewModelScope.launch { repository.updateGroupDescription(roomId, description) }
    }

    fun demoteAdmin(roomId: String, userId: String) {
        viewModelScope.launch { repository.demoteAdmin(roomId, userId) }
    }

    fun addParticipants(roomId: String, userIds: List<String>) {
        viewModelScope.launch { repository.addParticipants(roomId, userIds) }
    }

    fun removeParticipant(roomId: String, userId: String) {
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
                pinnedBy = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
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

    fun initializeCallManager(context: android.content.Context) {
        if (callManager == null) {
            callManager = com.chroma.studio.webrtc.CallManager(context.applicationContext, repository)
        }
    }

    fun startCall(roomId: String) {
        callManager?.startCall(roomId)
    }

    fun answerCall(roomId: String) {
        callManager?.answerCall(roomId)
    }

    fun toggleMute(muted: Boolean) {
        callManager?.setMicrophoneMute(muted)
    }

    fun toggleCamera(enabled: Boolean) {
        callManager?.setCameraEnabled(enabled)
    }

    fun switchCamera() {
        callManager?.switchCamera()
    }

    fun endCall() {
        callManager?.endCall()
    }

    fun unpinMessage(roomId: String) {
        val currentChats = _recentChats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == roomId }
        if (index != -1) {
            val room = currentChats[index]
            currentChats[index] = room.copy(
                pinnedMessageId = null,
                pinnedMessageText = null,
                pinnedBy = null,
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
                    messageType = msgType
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
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
