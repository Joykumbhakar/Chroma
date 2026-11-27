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
    onMinimize: (() -> Unit)? = null,
    onHangup: () -> Unit,
    showToast: (String) -> Unit
) {
    val camUpgradePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val camOn = chatViewModel?.toggleCamera() ?: true
            showToast(if (camOn) "Camera enabled" else "Camera disabled")
        } else {
            showToast("Camera permission needed to turn on video")
        }
    }

    // Fullscreen Call Modal
    AnimatedVisibility(
        visible = isFullCallModalOpen,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
    ) {
        FullCallModal(
            chat = activeChat,
            callType = activeCallType,
            callDurationSec = callDurationSeconds,
            callManager = chatViewModel?.callManager,
            onToggleMic = {
                val muted = chatViewModel?.toggleMute() ?: false
                showToast(if (muted) "Microphone muted" else "Microphone unmuted")
            },
            onToggleCam = {
                val currentlyOn = chatViewModel?.callManager?.isCameraEnabled?.value ?: false
                if (!currentlyOn) {
                    val hasCamera = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasCamera) {
                        camUpgradePermissionLauncher.launch(Manifest.permission.CAMERA)
                        return@FullCallModal
                    }
                }
                val camOn = chatViewModel?.toggleCamera() ?: true
                showToast(if (camOn) "Camera enabled" else "Camera disabled")
            },
            onToggleSpeaker = {
                val spkOn = chatViewModel?.toggleSpeaker() ?: false
                showToast(if (spkOn) "Speaker on" else "Earpiece active")
            },
            onSwitchCamera = {
                chatViewModel?.switchCamera()
            },
            onMinimize = onMinimize,
            onHangup = onHangup
        )
    }
}
