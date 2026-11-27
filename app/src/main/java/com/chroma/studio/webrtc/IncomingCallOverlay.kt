package com.chroma.studio.webrtc

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.webrtc.RendererCommon

@Composable
fun IncomingCallOverlay(
    call: CallModel?,
    callManager: CallManager? = null,
    callerHandle: String = "",
    isVerified: Boolean = false,
    initialStatus: CallStatus = CallStatus.INCOMING,
    conferenceCandidates: List<String> = emptyList(),
    canAnswerWithoutPrompt: (() -> Boolean)? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEndActiveCall: (reason: String) -> Unit = { onDecline() },
    onQuickMessageReply: ((String) -> Unit)? = null,
    onRemindLater: ((String) -> Unit)? = null
) {
    if (call == null) return
    val isOutgoing by (callManager?.isCaller ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val displayName = if (isOutgoing) {
        call.calleeName.ifBlank { "Oscellion User" }
    } else {
        call.callerName.ifBlank { "Oscellion User" }
    }
    val displayAvatar = if (isOutgoing) call.calleeAvatar else call.callerAvatar

    AppleIncomingCallScreen(
        modifier = Modifier.fillMaxSize(),
        callerName = displayName,
        callerSubtitle = if (call.isVideo) "FaceTime Video" else "Oscellion Audio",
        callerPhoneNumber = callerHandle,
        profileImageUrl = displayAvatar,
        isVerified = isVerified,
        initialStatus = initialStatus,
        initialVideoOn = call.isVideo,
        callManager = callManager,
        conferenceCandidates = conferenceCandidates,
        canAnswerWithoutPrompt = canAnswerWithoutPrompt,
        onCallAccepted = onAccept,
        onCallEnded = { reason ->
            if (initialStatus == CallStatus.INCOMING && callManager?.callState?.value == CallState.RINGING) {
                onDecline()
            } else {
                onEndActiveCall(reason)
            }
        },
        onQuickMessageReply = onQuickMessageReply,
        onRemindLater = onRemindLater,
        localCameraContent = if (callManager != null) {
            { _, isPip ->
                WebRTCVideoView(
                    callManager = callManager,
                    isLocal = true,
                    isPipOverlay = isPip,
                    scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else null,
        remoteVideoContent = if (callManager != null) {
            { isPip ->
                WebRTCVideoView(
                    callManager = callManager,
                    isLocal = false,
                    isPipOverlay = isPip,
                    scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else null
    )
}

