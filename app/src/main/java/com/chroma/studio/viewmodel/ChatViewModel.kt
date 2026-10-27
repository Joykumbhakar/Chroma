package com.chroma.studio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chroma.studio.data.ChatRepository
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
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
    private var onlineStatusListener: ValueEventListener? = null
    private var currentOtherUserId: String? = null

    private var messageLimit = 50L

    private val _isLoadingChats = MutableStateFlow(true)
    val isLoadingChats: StateFlow<Boolean> = _isLoadingChats.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _mutedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedChatIds: StateFlow<Set<String>> = _mutedChatIds.asStateFlow()

    init {
        observeChats()
        loadStories()
        loadMutedChats()
        loadBlockedUsers()
    }

    private fun loadStories() {
        viewModelScope.launch {
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

    private val presenceListeners = mutableMapOf<String, () -> Unit>()

    private fun observeUsersPresence(userIds: List<String>) {
        val currentUids = userIds.toSet()
        // Remove listeners for users no longer present
        val removed = presenceListeners.keys - currentUids
        for (uid in removed) {
            presenceListeners.remove(uid)?.invoke()
            _onlineUsersPresence.value = _onlineUsersPresence.value - uid
        }
        // Add listeners for new users
        for (uid in currentUids) {
            if (presenceListeners.containsKey(uid)) continue
            val unsubscribe = com.chroma.studio.data.OnlineStatusManager.observeUserPresence(uid) { presence ->
                _onlineUsersPresence.value = _onlineUsersPresence.value + (uid to presence)
                if (uid == currentOtherUserId) {
                    _isOtherUserOnline.value = presence.online
                    _otherUserLastSeen.value = presence.lastOnline
                }
            }
            presenceListeners[uid] = unsubscribe
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
                _isOtherUserOnline.value = isOnline
                _otherUserLastSeen.value = lastSeen
                _onlineUsersPresence.value = _onlineUsersPresence.value + (uid to com.chroma.studio.data.UserPresence(isOnline, lastSeen))
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(onlineStatusListener!!)
    }

    private fun observeChats() {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            _isLoadingChats.value = true
            repository.observeRecentChats().collect { chats ->
                _recentChats.value = chats
                _isLoadingChats.value = false

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
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val otherUids = if (currentUid != null) allUids.filter { it != currentUid } else allUids
                observeUsersPresence(otherUids)

                observeTypingStatuses(chats)
            }
        }
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

        // Switching to a new/different room: reset previous messages so they don't flash or leak
        if (previousRoomId != roomId) {
            _serverMessages.value = emptyList()
            _localMessages.value = emptyList()
        }
        _activeRoomId.value = roomId

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

        // 1. INSTANT OFFLINE PRE-LOAD (Telegram / WhatsApp technique):
        // First check in-memory L1 cache (0ms synchronous)
        val inMemoryCached = com.chroma.studio.chat.data.ChatLocalCacheManager.getMemoryCachedMessages(roomId)
        if (!inMemoryCached.isNullOrEmpty()) {
            _serverMessages.value = inMemoryCached
        } else {
            // Asynchronously load disk L2 cache on Dispatchers.IO to never block the main thread
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val diskCached = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedMessages(
                        com.chroma.studio.ChromaApp.instance,
                        roomId
                    )
                    if (diskCached.isNotEmpty() && _activeRoomId.value == roomId && _serverMessages.value.isEmpty()) {
                        _serverMessages.value = diskCached
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // 2. Initial viewport limit: 30 messages (prevents overload crashes on open)
        messageLimit = 30L
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

    fun createGroup(groupName: String, groupDescription: String? = null, participantIds: List<String>, imageUrl: String? = null, onRoomReady: (String) -> Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val roomId = "group_${java.util.UUID.randomUUID()}"

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
                _serverMessages.value = msgs
                // Asynchronously save to local offline cache so next open is instant
                if (msgs.isNotEmpty()) {
                    try {
                        com.chroma.studio.chat.data.ChatLocalCacheManager.saveMessages(
                            com.chroma.studio.ChromaApp.instance,
                            roomId,
                            msgs
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    fun sendMessage(text: String, workId: String? = null, replyToMessageId: String? = null) {
        val roomId = _activeRoomId.value ?: return
        sendMessageToRoom(roomId, text, workId, replyToMessageId)
    }

    fun sendMessageToRoom(roomId: String, text: String, workId: String? = null, replyToMessageId: String? = null) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (text.isBlank() && workId == null) return

        if (_activeRoomId.value == roomId) {
            setTypingStatus(false)
        }
        val msgId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val resolvedMessageType = if (text.startsWith("[POLL]:")) "poll" else "text"
        val pendingMsg = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = text,
            timestamp = timestamp,
            isPending = true,
            workId = workId,
            replyToMessageId = replyToMessageId,
            messageType = resolvedMessageType
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
            val previewText = if (workId != null) "Shared a work" else text
            currentChats.add(0, chat.copy(lastMessage = previewText, lastMessageTime = timestamp))
            _recentChats.value = currentChats
        }

        viewModelScope.launch {
            val success = repository.sendMessage(roomId, text, workId = workId, preGeneratedId = msgId, replyToMessageId = replyToMessageId, messageType = resolvedMessageType)
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

    private var typingDebounceJob: kotlinx.coroutines.Job? = null

    fun setTypingStatus(isTyping: Boolean) {
        if (isTyping) {
            setChatActivity("typing")
            typingDebounceJob?.cancel()
            typingDebounceJob = viewModelScope.launch {
                kotlinx.coroutines.delay(3500)
                setChatActivity(null)
            }
        } else {
            typingDebounceJob?.cancel()
            setChatActivity(null)
        }
    }

    fun setRecordingStatus(isRecording: Boolean) {
        typingDebounceJob?.cancel()
        setChatActivity(if (isRecording) "recording" else null)
    }

    private var currentActivityStatus: String? = null

    fun setChatActivity(activity: String?) {
        if (currentActivityStatus == activity) return
        currentActivityStatus = activity
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.setActivityStatus(roomId, activity)
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

    fun deleteMessage(messageId: String) {
        val roomId = _activeRoomId.value ?: return
        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local removal
        _serverMessages.value = _serverMessages.value.filter { it.id != messageId }
        _localMessages.value = _localMessages.value.filter { it.id != messageId }

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

    fun deleteMessageForMe(messageId: String) {
        val roomId = _activeRoomId.value ?: return
        val prevServer = _serverMessages.value
        val prevLocal = _localMessages.value

        // 1. Optimistic instant local removal
        _serverMessages.value = _serverMessages.value.filter { it.id != messageId }
        _localMessages.value = _localMessages.value.filter { it.id != messageId }

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
        _activeRoomId.value = null
        messagesJob?.cancel()
        _serverMessages.value = emptyList()
        _localMessages.value = emptyList()
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
                val msgType = message.messageType ?: if (mediaUrl != null) "image" else "text"
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

        // Clean up all per-room typing listeners
        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        for ((roomId, listener) in typingListeners) {
            db.getReference("status/$roomId").removeEventListener(listener)
        }
        typingListeners.clear()
        // Clear own activity status on exit
        val activeRoom = _activeRoomId.value
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (activeRoom != null && uid != null) {
            db.getReference("status/$activeRoom/$uid").removeValue()
        }
    }

    private var typingJob: kotlinx.coroutines.Job? = null
    private val typingListeners = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()

    private fun observeTypingStatuses(chats: List<ChatRoom>) {
        val db = com.chroma.studio.data.OnlineStatusManager.getDatabase()
        val currentRoomIds = chats.map { it.id }.toSet()

        // Remove listeners for rooms we're no longer in
        val removedRooms = typingListeners.keys - currentRoomIds
        for (roomId in removedRooms) {
            typingListeners.remove(roomId)?.let {
                db.getReference("status/$roomId").removeEventListener(it)
            }
            // Clear from status map
            _typingStatuses.value = _typingStatuses.value - roomId
        }

        // Add listeners for new rooms
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        for (roomId in currentRoomIds) {
            if (typingListeners.containsKey(roomId)) continue

            val roomRef = db.getReference("status/$roomId")
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val activityMap = mutableMapOf<String, String>()
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        val status = userSnapshot.getValue(String::class.java)
                        if (status != null) {
                            activityMap[userId] = status
                        }
                    }
                    _typingStatuses.value = if (activityMap.isNotEmpty()) {
                        _typingStatuses.value + (roomId to activityMap)
                    } else {
                        _typingStatuses.value - roomId
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.d("ChatViewModel", "Typing status cancelled for $roomId: ${error.message}")
                }
            }
            roomRef.addValueEventListener(listener)
            typingListeners[roomId] = listener
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
