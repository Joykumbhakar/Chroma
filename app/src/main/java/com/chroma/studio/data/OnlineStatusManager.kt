package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.ChatStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object OnlineStatusManager {
    private const val TAG = "OnlineStatusManager"
    private const val RTDB_URL = "https://chromamelt-default-rtdb.asia-southeast1.firebasedatabase.app"
    private var connectedListener: ValueEventListener? = null

    // Universal presence cache across the entire application (Chat, Profile, User List, Feed)
    private val _presenceCache = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val presenceCache: StateFlow<Map<String, UserPresence>> = _presenceCache.asStateFlow()

    private val hiddenStatusUids = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    @Volatile
    private var selfShowOnlineStatus: Boolean = true
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    fun parseLastSeenNumber(value: Any?): Long? {
        return when (value) {
            is Number -> value.toLong().takeIf { it > 0L }
            is String -> value.toLongOrNull()?.takeIf { it > 0L }
            else -> null
        }
    }

    fun parsePresenceSnapshot(uid: String, snapshot: DataSnapshot): UserPresence {
        if (hiddenStatusUids[uid] == true) {
            return UserPresence(online = false, lastOnline = null)
        }
        val rawOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
        val hasConnectionsNode = snapshot.hasChild("connections")
        val hasActiveConn = if (hasConnectionsNode) snapshot.child("connections").childrenCount > 0 else true
        val parsedLastOnline = parseLastSeenNumber(snapshot.child("lastOnline").value)
            ?: parseLastSeenNumber(snapshot.child("lastSeen").value)
        val cachedLastOnline = _presenceCache.value[uid]?.lastOnline
        val lastOnline = parsedLastOnline ?: cachedLastOnline
        // Guard against stale "online = true" when all connections are gone
        val effectiveOnline = rawOnline && hasActiveConn
        return UserPresence(online = effectiveOnline, lastOnline = lastOnline)
    }

    fun seedProfilePresence(profile: com.chroma.studio.model.UserProfile?) {
        val uid = profile?.uid?.takeIf { it.isNotBlank() } ?: return
        if (!profile.showOnlineStatus) {
            hiddenStatusUids[uid] = true
            _presenceCache.value = _presenceCache.value + (uid to UserPresence(online = false, lastOnline = null))
            return
        }
        hiddenStatusUids.remove(uid)
        val current = _presenceCache.value[uid]
        val profileLastSeen = profile.lastSeen?.takeIf { it > 0L }
        val mergedLastSeen = when {
            current?.lastOnline != null && profileLastSeen != null -> maxOf(current.lastOnline, profileLastSeen)
            else -> current?.lastOnline ?: profileLastSeen
        }
        val mergedOnline = current?.online ?: profile.online
        _presenceCache.value = _presenceCache.value + (uid to UserPresence(online = mergedOnline, lastOnline = mergedLastSeen))
    }

    fun updateSelfShowOnlineStatus(showOnlineStatus: Boolean) {
        selfShowOnlineStatus = showOnlineStatus
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!showOnlineStatus) {
            hiddenStatusUids[uid] = true
            _presenceCache.value = _presenceCache.value + (uid to UserPresence(online = false, lastOnline = null))
            presenceScope.launch {
                try {
                    val userRef = getDatabase().getReference("users/$uid")
                    userRef.child("online").setValue(false)
                } catch (_: Exception) {}
            }
        } else {
            hiddenStatusUids.remove(uid)
            val foreground = try { ChatStateManager.isAppInForeground.value } catch (_: Exception) { true }
            presenceScope.launch {
                try {
                    val userRef = getDatabase().getReference("users/$uid")
                    if (foreground) {
                        userRef.child("online").setValue(true)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun isSelfStatusVisible(): Boolean = selfShowOnlineStatus

    fun getUserPresence(uid: String?): UserPresence {
        if (uid.isNullOrBlank()) return UserPresence(false, null)
        return _presenceCache.value[uid] ?: UserPresence(false, null)
    }

    fun isUserOnline(uid: String?): Boolean {
        if (uid.isNullOrBlank()) return false
        return _presenceCache.value[uid]?.online == true
    }
    
    fun getDatabase(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }
    
    fun initialize() {
        val auth = FirebaseAuth.getInstance()
        
        // Listen to Auth state changes to manage presence
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                setupPresence(user.uid)
            } else {
                cleanupPresence()
            }
        }
    }

    private fun setupPresence(uid: String) {
        val db = getDatabase()
        val myConnectionsRef = db.getReference("users/$uid/connections")
        val lastOnlineRef = db.getReference("users/$uid/lastOnline")
        val onlineRef = db.getReference("users/$uid/online")
        val connectedRef = db.getReference(".info/connected")

        // Clean up previous listener if exists
        cleanupPresence()

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase RTDB")
                    // Only mark online if the user has the app open AND online visibility is enabled
                    if (ChatStateManager.isAppInForeground.value && selfShowOnlineStatus) {
                        onlineRef.setValue(true)
                        startForegroundHeartbeat(uid)
                    } else if (!selfShowOnlineStatus) {
                        onlineRef.setValue(false)
                    }

                    // Add this device to connections list
                    val con = myConnectionsRef.push()
                    if (selfShowOnlineStatus) {
                        con.setValue(true)
                    }
                    con.onDisconnect().removeValue()

                    // When disconnected, set online to false and update lastOnline
                    onlineRef.onDisconnect().setValue(false)
                    lastOnlineRef.onDisconnect().setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listener was cancelled at .info/connected")
            }
        }
        connectedRef.addValueEventListener(connectedListener!!)
    }

    private fun cleanupPresence() {
        connectedListener?.let {
            getDatabase().getReference(".info/connected").removeEventListener(it)
            connectedListener = null
        }
    }

    /**
     * Observes real-time online status and lastSeen for a single user.
     * Updates the universal presence cache and returns a lambda to unsubscribe.
     */
    fun observeUserPresence(uid: String, onUpdate: (UserPresence) -> Unit): () -> Unit {
        if (uid.isBlank() || FirebaseAuth.getInstance().currentUser == null) {
            return {}
        }
        val ref = getDatabase().getReference("users/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presence = parsePresenceSnapshot(uid, snapshot)
                _presenceCache.value = _presenceCache.value + (uid to presence)
                onUpdate(presence)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "observeUserPresence cancelled for $uid: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /**
     * Composable that observes a user's real-time presence (online status & last seen timestamp)
     * using the exact same Firebase Realtime Database status engine as the chat/message screen.
     */
    @Composable
    fun rememberUserPresence(uid: String?): UserPresence {
        if (uid.isNullOrBlank()) return UserPresence(false, null)
        var presence by remember(uid) {
            mutableStateOf(_presenceCache.value[uid] ?: UserPresence(false, null))
        }
        DisposableEffect(uid) {
            val unsubscribe = observeUserPresence(uid) { updated ->
                presence = updated
            }
            onDispose {
                unsubscribe()
            }
        }
        return presence
    }

    /**
     * Composable returning whether a user is currently online.
     */
    @Composable
    fun rememberIsUserOnline(uid: String?): Boolean {
        return rememberUserPresence(uid).online
    }

    /**
     * Batch presence observer for lists of users (e.g. Followers, Following, Stories).
     */
    fun observeUsersPresence(uids: Collection<String>): () -> Unit {
        val cleanUids = uids.filter { it.isNotBlank() }.distinct()
        if (cleanUids.isEmpty() || FirebaseAuth.getInstance().currentUser == null) return {}
        val unsubscribes = cleanUids.map { observeUserPresence(it) {} }
        return { unsubscribes.forEach { try { it() } catch (_: Exception) {} } }
    }

    /**
     * Explicitly update online/offline presence when app foregrounds or backgrounds.
     */
    /**
     * Sets real-time typing/recording activity for the current user in a specific chat room.
     * Activity can be "typing", "recording", or null (to clear).
     */
    fun setChatActivity(roomId: String, activity: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (roomId.isBlank()) return
        val db = getDatabase()
        val ref = db.getReference("status/$roomId/$uid")
        try {
            if (activity != null) {
                val payload = mapOf(
                    "status" to activity,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
                )
                ref.setValue(payload)
                ref.onDisconnect().removeValue()
            } else {
                ref.removeValue()
                ref.onDisconnect().cancel()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update chat activity for $roomId: ${e.message}")
        }
    }

    /**
     * Observes real-time typing and recording activity for all users in a specific chat room.
     * Automatically parses both map payloads and string payloads.
     */
    fun observeRoomActivity(roomId: String, onUpdate: (Map<String, String>) -> Unit): () -> Unit {
        if (roomId.isBlank()) return {}
        val db = getDatabase()
        val ref = db.getReference("status/$roomId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activityMap = mutableMapOf<String, String>()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val status = when (val value = userSnapshot.value) {
                        is String -> value
                        is Map<*, *> -> value["status"] as? String ?: value["activity"] as? String
                        else -> null
                    }
                    if (!status.isNullOrBlank()) {
                        activityMap[userId] = status
                    }
                }
                onUpdate(activityMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeRoomActivity cancelled for $roomId: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /**
     * Computes the deterministic, shared room ID for a 1-on-1 direct chat between two users.
     */
    fun getDirectRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    fun normalizeRoomId(roomId: String, currentUid: String, otherUid: String? = null): String {
        if (roomId.startsWith("group_")) return roomId
        if (roomId.contains("_")) {
            val parts = roomId.split("_")
            if (parts.size == 2) {
                val u1 = parts[0]
                val u2 = parts[1]
                return if (u1 < u2) "${u1}_$u2" else "${u2}_$u1"
            }
        }
        val targetUid = otherUid ?: roomId.removePrefix("direct_")
        if (targetUid.isNotBlank() && currentUid.isNotBlank() && targetUid != currentUid) {
            return if (currentUid < targetUid) "${currentUid}_$targetUid" else "${targetUid}_$currentUid"
        }
        return roomId
    }

    private var backgroundGraceJob: Job? = null
    private val presenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startForegroundHeartbeat(uid: String) {
        heartbeatJob?.cancel()
        heartbeatJob = presenceScope.launch {
            while (true) {
                delay(90_000L)
                if (!selfShowOnlineStatus || !ChatStateManager.isAppInForeground.value) break
                try {
                    getDatabase().getReference("users/$uid/lastOnline")
                        .setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                } catch (_: Exception) {}
            }
        }
    }

    fun setAppForegrounded(isForeground: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = getDatabase()
        val onlineRef = db.getReference("users/$uid/online")
        val lastOnlineRef = db.getReference("users/$uid/lastOnline")

        if (isForeground) {
            backgroundGraceJob?.cancel()
            backgroundGraceJob = null
            if (!selfShowOnlineStatus) {
                try {
                    onlineRef.setValue(false)
                } catch (_: Exception) {}
                return
            }
            try {
                onlineRef.setValue(true)
                startForegroundHeartbeat(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update foreground presence: ${e.message}")
            }
        } else {
            heartbeatJob?.cancel()
            heartbeatJob = null
            // WhatsApp-grade 4,000ms grace period for transient network switches/app switches
            backgroundGraceJob?.cancel()
            backgroundGraceJob = presenceScope.launch {
                delay(4000L)
                val now = System.currentTimeMillis()
                try {
                    onlineRef.setValue(false)
                    lastOnlineRef.setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update background presence: ${e.message}")
                }
                if (selfShowOnlineStatus) {
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update(
                                mapOf(
                                    "online" to false,
                                    "lastSeen" to now
                                )
                            )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Formats offline timestamps according to exact WhatsApp guidelines:
     * - < 60s ago: "last seen just now"
     * - Today: "last seen today at 3:45 PM"
     * - Yesterday: "last seen yesterday at 11:20 AM"
     * - 2-6 days: "last seen on Tuesday at 4:15 PM"
     * - Same year: "last seen 24 Oct at 10:15 AM"
     * - Diff year: "last seen 15 Aug 2023 at 10:15 AM"
     */
    fun formatWhatsAppLastSeen(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return "Offline"
        return try {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { timeInMillis = timestamp }
            val diffMs = now.timeInMillis - timestamp

            // Handle clock skew between devices/servers (up to 30s into future is considered 'just now')
            if (diffMs < 60_000L && diffMs >= -30_000L) {
                return "last seen just now"
            }

            val is24Hour = try {
                android.text.format.DateFormat.is24HourFormat(com.chroma.studio.ChromaApp.instance)
            } catch (_: Exception) {
                false
            }
            val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
            val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
            val formattedTime = timeFormat.format(Date(timestamp))

            val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
            if (isToday) {
                return "last seen today at $formattedTime"
            }

            val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
            if (isYesterday) {
                return "last seen yesterday at $formattedTime"
            }

            val daysDiff = diffMs / (24 * 60 * 60 * 1000L)
            if (daysDiff in 2..6) {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                return "last seen on ${dayFormat.format(Date(timestamp))} at $formattedTime"
            }

            if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
                val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                return "last seen ${dateFormat.format(Date(timestamp))} at $formattedTime"
            }

            val fullDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            "last seen ${fullDateFormat.format(Date(timestamp))} at $formattedTime"
        } catch (e: Exception) {
            "Offline"
        }
    }

}

data class UserPresence(
    val online: Boolean = false,
    val lastOnline: Long? = null
)
