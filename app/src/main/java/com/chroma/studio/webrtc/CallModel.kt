package com.chroma.studio.webrtc

import androidx.annotation.Keep

@Keep
enum class CallState {
    IDLE,
    CALLING,
    RINGING,
    ACTIVE,
    ENDED
}

@Keep
data class CallModel(
    val id: String = "",
    val roomId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val calleeAvatar: String = "",
    val type: String = "audio", // "audio" or "video"
    val status: String = "calling", // "calling", "ringing", "accepted", "rejected", "ended", "busy", "missed"
    val offer: Map<String, String>? = null,
    val answer: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    @get:com.google.firebase.firestore.Exclude
    val isVideo: Boolean get() = type.equals("video", ignoreCase = true)
}

@Keep
data class IceCandidateModel(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdpCandidate: String = "",
    val serverUrl: String = ""
)
