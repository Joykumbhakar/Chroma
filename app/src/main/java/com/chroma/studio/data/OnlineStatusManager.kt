package com.chroma.studio.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object OnlineStatusManager {
    private const val TAG = "OnlineStatusManager"
    private const val RTDB_URL = "https://chromamelt-default-rtdb.asia-southeast1.firebasedatabase.app"
    private var connectedListener: ValueEventListener? = null
    
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
                    // When connected, set online to true
                    onlineRef.setValue(true)
                    
                    // Add this device to connections list
                    val con = myConnectionsRef.push()
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
     * Returns a lambda to unsubscribe.
     */
    fun observeUserPresence(uid: String, onUpdate: (UserPresence) -> Unit): () -> Unit {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return {}
        }
        val ref = getDatabase().getReference("users/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastOnline").getValue(Long::class.java)
                onUpdate(UserPresence(online = isOnline, lastOnline = lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "observeUserPresence cancelled for $uid: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /**
     * Explicitly update online/offline presence when app foregrounds or backgrounds.
     */
    fun setAppForegrounded(isForeground: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = getDatabase()
        val onlineRef = db.getReference("users/$uid/online")
        val lastOnlineRef = db.getReference("users/$uid/lastOnline")
        try {
            if (isForeground) {
                onlineRef.setValue(true)
            } else {
                onlineRef.setValue(false)
                lastOnlineRef.setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update foreground presence: ${e.message}")
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
