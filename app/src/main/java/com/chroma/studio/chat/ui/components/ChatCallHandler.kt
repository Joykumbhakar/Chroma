package com.chroma.studio.chat.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.viewmodel.ChatViewModel
import com.chroma.studio.webrtc.CallModel
import com.chroma.studio.webrtc.IncomingCallOverlay

class ChatCallActions(
    val startAudioCall: (ChatConversation) -> Unit,
    val startVideoCall: (ChatConversation) -> Unit,
    val startCall: (ChatConversation, isVideo: Boolean) -> Unit
)

@Composable
fun rememberChatCallActions(
    context: Context,
    chatViewModel: ChatViewModel?,
    currentUid: String,
    activeChatId: String,
    showToast: (String) -> Unit,
    showIsland: (String, String?, IslandType) -> Unit,
    onCallUiOpened: (callType: String) -> Unit
): ChatCallActions {
    var pendingCallIsVideo by remember { mutableStateOf(false) }
    var pendingCallChat by remember { mutableStateOf<ChatConversation?>(null) }

    fun executeStartCall(chat: ChatConversation, isVideo: Boolean) {
        val targetChatId = if (chat.id.isNotBlank()) chat.id else if (activeChatId.isNotBlank()) activeChatId else "call_${System.currentTimeMillis()}"
        val otherUid = chat.otherUserId ?: chat.participantUids.firstOrNull { it != currentUid } ?: ""
        if (otherUid.isBlank()) {
            showToast("Cannot start call: recipient is not available")
            return
        }
        try {
            val callType = if (isVideo) "Video Call" else "Audio Call"
            onCallUiOpened(callType)
            chatViewModel?.initializeCallManager(context)
            chatViewModel?.startCall(
                roomId = targetChatId,
                otherUserId = otherUid,
                otherUserName = chat.name,
                otherUserAvatar = chat.avatar,
                isVideo = isVideo
            )
            SoundManager.playRing()
        } catch (e: Throwable) {
            Log.e("ChatCallHandler", "Error starting call", e)
            showToast("Unable to start call: ${e.message}")
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val audioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = perms[Manifest.permission.CAMERA] == true
        val targetChat = pendingCallChat ?: return@rememberLauncherForActivityResult
        if (pendingCallIsVideo) {
            if (audioGranted && cameraGranted) {
                executeStartCall(targetChat, isVideo = true)
            } else {
                showIsland("Camera & mic permissions needed for video calls", null, IslandType.WARNING)
            }
        } else {
            if (audioGranted) {
                executeStartCall(targetChat, isVideo = false)
            } else {
                showIsland("Microphone permission needed for audio calls", null, IslandType.WARNING)
            }
        }
    }

    fun initiateCallWithPermissions(chat: ChatConversation, isVideo: Boolean) {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        pendingCallChat = chat
        pendingCallIsVideo = isVideo
        if (isVideo) {
            if (hasAudio && hasCamera) {
                executeStartCall(chat, isVideo = true)
            } else {
                callPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                )
            }
        } else {
            if (hasAudio) {
                executeStartCall(chat, isVideo = false)
            } else {
                callPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }

    return remember(context, chatViewModel, currentUid, activeChatId, callPermissionLauncher) {
        ChatCallActions(
            startAudioCall = { chat -> initiateCallWithPermissions(chat, isVideo = false) },
            startVideoCall = { chat -> initiateCallWithPermissions(chat, isVideo = true) },
            startCall = { chat, isVideo -> initiateCallWithPermissions(chat, isVideo = true == isVideo) }
        )
    }
}

@Composable
fun ChatCallOverlays(
    context: Context,
    chatViewModel: ChatViewModel?,
    activeChat: ChatConversation,
    activeCallType: String,
    isFullCallModalOpen: Boolean,
    callDurationSeconds: Int,
    onCallUiOpened: (String) -> Unit,
    onHangup: () -> Unit,
    showToast: (String) -> Unit
) {
    // Fullscreen Call Modal
    AnimatedVisibility(
        visible = isFullCallModalOpen,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
    ) {
        var isMicMuted by remember { mutableStateOf(false) }
        var isCamEnabled by remember { mutableStateOf(activeCallType.contains("Video", ignoreCase = true)) }
        var isSpeakerOn by remember { mutableStateOf(activeCallType.contains("Video", ignoreCase = true)) }

        FullCallModal(
            chat = activeChat,
            callType = activeCallType,
            callDurationSec = callDurationSeconds,
            callManager = chatViewModel?.callManager,
            onToggleMic = {
                val muted = chatViewModel?.toggleMute() ?: false
                isMicMuted = muted
                showToast(if (muted) "Microphone muted" else "Microphone unmuted")
            },
            onToggleCam = {
                val camOn = chatViewModel?.toggleCamera() ?: true
                isCamEnabled = camOn
                showToast(if (camOn) "Camera enabled" else "Camera disabled")
            },
            onToggleSpeaker = {
                val spkOn = chatViewModel?.toggleSpeaker() ?: false
                isSpeakerOn = spkOn
                showToast(if (spkOn) "Speaker on" else "Earpiece active")
            },
            onSwitchCamera = {
                chatViewModel?.switchCamera()
            },
            onHangup = onHangup
        )
    }

    // WebRTC Incoming Call Overlay (Best User-Friendly Incoming Call UX)
    val defaultIncoming = remember { kotlinx.coroutines.flow.MutableStateFlow<CallModel?>(null) }
    val rtcIncomingCall by (chatViewModel?.incomingCall ?: defaultIncoming).collectAsState()

    AnimatedVisibility(
        visible = rtcIncomingCall != null,
        enter = fadeIn(tween(250)) + slideInVertically { -it },
        exit = fadeOut(tween(200)) + slideOutVertically { -it }
    ) {
        IncomingCallOverlay(
            call = rtcIncomingCall,
            onAccept = {
                val call = rtcIncomingCall ?: return@IncomingCallOverlay
                try {
                    val callType = if (call.isVideo) "Video Call" else "Audio Call"
                    onCallUiOpened(callType)
                    chatViewModel?.initializeCallManager(context)
                    chatViewModel?.answerCall(call)
                } catch (e: Throwable) {
                    Log.e("ChatCallHandler", "Error answering call", e)
                    showToast("Unable to answer call: ${e.message}")
                }
            },
            onDecline = {
                val call = rtcIncomingCall ?: return@IncomingCallOverlay
                chatViewModel?.rejectCall(call)
            }
        )
    }
}
