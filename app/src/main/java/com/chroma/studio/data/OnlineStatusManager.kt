package com.chroma.studio.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
}

data class UserPresence(
    val online: Boolean = false,
    val lastOnline: Long? = null
)
