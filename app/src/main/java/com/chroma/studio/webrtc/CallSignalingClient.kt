package com.chroma.studio.webrtc

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate

class CallSignalingClient {
    private val firestore = FirebaseFirestore.getInstance()
    private val callsCollection = firestore.collection("calls")

    suspend fun createCall(call: CallModel): Boolean {
        if (call.id.isBlank()) return false
        return try {
            callsCollection.document(call.id).set(call).await()
            true
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error creating call document", e)
            false
        }
    }

    suspend fun updateCallStatus(callId: String, status: String) {
        if (callId.isBlank()) return
        try {
            callsCollection.document(callId).update("status", status).await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error updating call status to $status", e)
        }
    }

    suspend fun sendOffer(callId: String, offerSdp: String) {
        if (callId.isBlank()) return
        try {
            val offerMap = mapOf("type" to "OFFER", "sdp" to offerSdp)
            callsCollection.document(callId).update("offer", offerMap, "status", "calling").await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error sending offer", e)
        }
    }

    suspend fun sendAnswer(callId: String, answerSdp: String) {
        if (callId.isBlank()) return
        try {
            val answerMap = mapOf("type" to "ANSWER", "sdp" to answerSdp)
            callsCollection.document(callId).update("answer", answerMap, "status", "accepted").await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error sending answer", e)
        }
    }

    suspend fun addCallerCandidate(callId: String, candidate: IceCandidate) {
        if (callId.isBlank()) return
        try {
            val candModel = IceCandidateModel(
                sdpMid = candidate.sdpMid ?: "",
                sdpMLineIndex = candidate.sdpMLineIndex,
                sdpCandidate = candidate.sdp,
                serverUrl = candidate.serverUrl ?: ""
            )
            callsCollection.document(callId).collection("callerCandidates").add(candModel).await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error adding caller candidate", e)
        }
    }

    suspend fun addCalleeCandidate(callId: String, candidate: IceCandidate) {
        if (callId.isBlank()) return
        try {
            val candModel = IceCandidateModel(
                sdpMid = candidate.sdpMid ?: "",
                sdpMLineIndex = candidate.sdpMLineIndex,
                sdpCandidate = candidate.sdp,
                serverUrl = candidate.serverUrl ?: ""
            )
            callsCollection.document(callId).collection("calleeCandidates").add(candModel).await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error adding callee candidate", e)
        }
    }

    fun listenToCall(callId: String): Flow<CallModel?> = callbackFlow {
        if (callId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = callsCollection.document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallSignalingClient", "Listen call error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val call = snapshot.toObject(CallModel::class.java)
                        trySend(call)
                    } catch (e: Exception) {
                        Log.e("CallSignalingClient", "Parsing call model error", e)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenCallerCandidates(callId: String): Flow<IceCandidate> = callbackFlow {
        if (callId.isBlank()) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = callsCollection.document(callId)
            .collection("callerCandidates")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        try {
                            val model = change.document.toObject(IceCandidateModel::class.java)
                            val ice = IceCandidate(model.sdpMid, model.sdpMLineIndex, model.sdpCandidate)
                            trySend(ice)
                        } catch (e: Exception) {
                            Log.e("CallSignalingClient", "Error parsing caller candidate", e)
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenCalleeCandidates(callId: String): Flow<IceCandidate> = callbackFlow {
        if (callId.isBlank()) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = callsCollection.document(callId)
            .collection("calleeCandidates")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        try {
                            val model = change.document.toObject(IceCandidateModel::class.java)
                            val ice = IceCandidate(model.sdpMid, model.sdpMLineIndex, model.sdpCandidate)
                            trySend(ice)
                        } catch (e: Exception) {
                            Log.e("CallSignalingClient", "Error parsing callee candidate", e)
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenForIncomingCalls(calleeUid: String): Flow<CallModel?> = callbackFlow {
        if (calleeUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        // Find calls where calleeId == current user and status == calling
        val listener: ListenerRegistration = callsCollection
            .whereEqualTo("calleeId", calleeUid)
            .whereEqualTo("status", "calling")
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (!snapshots.isEmpty) {
                    try {
                        val firstDoc = snapshots.documents.first()
                        val call = firstDoc.toObject(CallModel::class.java)
                        trySend(call)
                    } catch (e: Exception) {
                        Log.e("CallSignalingClient", "Error parsing incoming call", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun endCall(callId: String, status: String = "ended") {
        if (callId.isBlank()) return
        try {
            callsCollection.document(callId).update("status", status).await()
        } catch (e: Exception) {
            Log.e("CallSignalingClient", "Error ending call", e)
        }
    }
}
