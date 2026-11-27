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
enum class CallConnectionQuality {
    CONNECTING,
    GOOD,
    RECONNECTING,
    POOR,
    DISCONNECTED
}

/** Formats call duration in MM:SS (or H:MM:SS if >= 1 hour). */
fun formatCallDuration(durationSec: Int): String {
    val safeSec = durationSec.coerceAtLeast(0)
    val hours = safeSec / 3600
    val minutes = (safeSec % 3600) / 60
    val seconds = safeSec % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/** Formats human-readable live status subtitle for the call screen header. */
fun formatCallStatusText(
    callState: CallState,
    connectionQuality: CallConnectionQuality,
    durationSec: Int,
    isCaller: Boolean
): String = when (callState) {
    CallState.IDLE -> "Idle"
    CallState.CALLING -> if (isCaller) "Calling..." else "Connecting..."
    CallState.RINGING -> "Incoming call"
    CallState.ENDED -> "Call ended"
    CallState.ACTIVE -> when (connectionQuality) {
        CallConnectionQuality.RECONNECTING -> "Reconnecting..."
        CallConnectionQuality.CONNECTING -> if (durationSec > 0) formatCallDuration(durationSec) else "Connecting..."
        CallConnectionQuality.DISCONNECTED -> "Disconnected"
        CallConnectionQuality.GOOD,
        CallConnectionQuality.POOR -> formatCallDuration(durationSec)
    }
}

/** Resolves remote participant display name and avatar using the active call record with chat fallback. */
fun resolveRemotePeerInfo(
    activeCall: CallModel?,
    fallbackName: String,
    fallbackAvatar: String,
    currentUserId: String
): Pair<String, String> {
    if (activeCall == null) {
        return fallbackName.ifBlank { "Oscellion User" } to fallbackAvatar
    }
    val iAmCaller = currentUserId.isNotBlank() && activeCall.callerId == currentUserId
    val name = if (iAmCaller) activeCall.calleeName else activeCall.callerName
    val avatar = if (iAmCaller) activeCall.calleeAvatar else activeCall.callerAvatar
    return name.ifBlank { fallbackName }.ifBlank { "Oscellion User" } to avatar.ifBlank { fallbackAvatar }
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
