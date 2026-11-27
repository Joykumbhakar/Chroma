package com.chroma.studio.webrtc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// APPLE iOS DESIGN TOKENS
// ============================================================================
private object IosColors {
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val Blue = Color(0xFF0A84FF)
    val Amber = Color(0xFFFF9F0A)
    val Sheet = Color(0xFF1C1C1E)
    val Surface = Color(0xFF2C2C2E)
    val Divider = Color(0xFF38383A)
    val GlassIdle = Color.White.copy(alpha = 0.17f)
    val GlassActive = Color.White.copy(alpha = 0.38f)
    val SkyAccent = Color(0xFF7DD3FC)
}

// ============================================================================
// CALL & UI STATE MODELS
// ============================================================================
enum class CallStatus {
    INCOMING,
    ACTIVE,
    ENDED
}

enum class AudioDeviceType {
    IPHONE,
    SPEAKER,
    BLUETOOTH
}

data class AudioOutputDevice(
    val name: String,
    val type: AudioDeviceType,
    val batteryLabel: String? = null,
    val deviceId: Int? = null
)

enum class VideoLayoutMode(val label: String) {
    FLOATING("Floating"),
    SPLIT_VERTICAL("Top & Bottom"),
    SPLIT_HORIZONTAL("Side by Side"),
    FOCUS_YOU("Focus You")
}

enum class PipCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class ActiveSheet {
    NONE,
    REMIND,
    MESSAGE,
    ADD_CALL,
    CONTACTS,
    LAYOUT
}

private val FallbackAudioDevices = listOf(
    AudioOutputDevice("Phone", AudioDeviceType.IPHONE),
    AudioOutputDevice("Speaker", AudioDeviceType.SPEAKER)
)

// ============================================================================
// HOISTED CALL STATE HOLDER (Drivable from CallManager / ViewModel / Activity)
// ============================================================================
@Stable
class AppleCallState(
    initialStatus: CallStatus = CallStatus.INCOMING,
    initialVideoOn: Boolean = false,
    initialAudioDevice: AudioOutputDevice = FallbackAudioDevices[0]
) {
    var callStatus by mutableStateOf(initialStatus)
    var elapsedSeconds by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isOnHold by mutableStateOf(false)
    var isVideoOn by mutableStateOf(initialVideoOn)
    var isRemoteVideoOn by mutableStateOf(false)
    var showRemoteVideoDialog by mutableStateOf(false)
    var showReinviteVideoLink by mutableStateOf(false)
    var isCameraMirrored by mutableStateOf(true)
    var isLocalCameraLive by mutableStateOf(initialVideoOn)
    var mergedParticipant by mutableStateOf<String?>(null)
    var endedReason by mutableStateOf("Call Ended")

    var selectedAudioDevice by mutableStateOf(initialAudioDevice)
    var showAudioDropdown by mutableStateOf(false)
    var activeSheet by mutableStateOf(ActiveSheet.NONE)

    var videoLayout by mutableStateOf(VideoLayoutMode.FLOATING)
    var pipCorner by mutableStateOf(PipCorner.TOP_RIGHT)
    var overlaysVisible by mutableStateOf(true)
    var overlayInteractionTick by mutableIntStateOf(0)

    var toastMessage by mutableStateOf<String?>(null)
    var toastTick by mutableIntStateOf(0)

    val isOverlayBlocked: Boolean
        get() = showAudioDropdown || showRemoteVideoDialog || activeSheet != ActiveSheet.NONE

    fun showToast(message: String) {
        toastMessage = message
        toastTick++
    }

    fun pokeOverlays() {
        overlaysVisible = true
        overlayInteractionTick++
    }

    fun acceptCall() {
        if (callStatus == CallStatus.ACTIVE) return
        callStatus = CallStatus.ACTIVE
        elapsedSeconds = 0
        activeSheet = ActiveSheet.NONE
        showAudioDropdown = false
        overlaysVisible = true
    }

    fun endCall(customReason: String? = null): String {
        val wasActive = callStatus == CallStatus.ACTIVE
        callStatus = CallStatus.ENDED
        showAudioDropdown = false
        showRemoteVideoDialog = false
        activeSheet = ActiveSheet.NONE
        isVideoOn = false
        isRemoteVideoOn = false
        showReinviteVideoLink = false
        isLocalCameraLive = false
        videoLayout = VideoLayoutMode.FLOATING
        pipCorner = PipCorner.TOP_RIGHT
        overlaysVisible = true

        val reason = customReason ?: if (wasActive) {
            "Duration ${formatCallTime(elapsedSeconds)}"
        } else {
            "Call Declined"
        }
        endedReason = reason
        return reason
    }

    fun resetCall() {
        callStatus = CallStatus.INCOMING
        elapsedSeconds = 0
        isMuted = false
        isOnHold = false
        isVideoOn = false
        isRemoteVideoOn = false
        showRemoteVideoDialog = false
        showReinviteVideoLink = false
        isLocalCameraLive = false
        mergedParticipant = null
        videoLayout = VideoLayoutMode.FLOATING
        pipCorner = PipCorner.TOP_RIGHT
        overlaysVisible = true
        activeSheet = ActiveSheet.NONE
        showAudioDropdown = false
    }

    fun toggleMute(nextMuted: Boolean = !isMuted) {
        isMuted = nextMuted
        overlayInteractionTick++
        showToast(if (isMuted) "Microphone muted" else "Microphone unmuted")
    }

    fun toggleHold(nextHold: Boolean = !isOnHold) {
        isOnHold = nextHold
        overlayInteractionTick++
        showToast(if (isOnHold) "Call placed on hold" else "Call resumed")
    }

    fun toggleVideo(nextVideo: Boolean = !isVideoOn) {
        isVideoOn = nextVideo
        if (!isVideoOn) {
            isRemoteVideoOn = false
            showRemoteVideoDialog = false
            showReinviteVideoLink = false
            isLocalCameraLive = false
            videoLayout = VideoLayoutMode.FLOATING
            pipCorner = PipCorner.TOP_RIGHT
            overlaysVisible = true
            showToast("Video turned off")
        } else {
            isLocalCameraLive = true
            pokeOverlays()
        }
    }

    fun respondRemoteVideo(accepted: Boolean, callerName: String) {
        showRemoteVideoDialog = false
        if (accepted) {
            isRemoteVideoOn = true
            showReinviteVideoLink = false
            pokeOverlays()
            showToast("$callerName turned on video")
        } else {
            isRemoteVideoOn = false
            showReinviteVideoLink = true
            showToast("$callerName stayed on audio")
        }
    }

    fun selectVideoLayout(layout: VideoLayoutMode) {
        videoLayout = layout
        activeSheet = ActiveSheet.NONE
        overlayInteractionTick++
        showToast("Layout: ${layout.label}")
    }

    fun swapFloatingFocus() {
        if (!isVideoOn) return
        val next = if (videoLayout == VideoLayoutMode.FOCUS_YOU) {
            VideoLayoutMode.FLOATING
        } else {
            VideoLayoutMode.FOCUS_YOU
        }
        videoLayout = next
        overlayInteractionTick++
        showToast("Layout: ${next.label}")
    }
}

@Composable
fun rememberAppleCallState(
    initialStatus: CallStatus = CallStatus.INCOMING,
    initialVideoOn: Boolean = false,
    initialAudioDevice: AudioOutputDevice = FallbackAudioDevices[0]
): AppleCallState = remember(initialStatus, initialVideoOn, initialAudioDevice) {
    AppleCallState(
        initialStatus = initialStatus,
        initialVideoOn = initialVideoOn,
        initialAudioDevice = initialAudioDevice
    )
}

// ============================================================================
// TACTILE APPLE SPRING PRESS MODIFIER
// ============================================================================
private fun Modifier.iosClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "iosPressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// ============================================================================
// MAIN COMPOSABLE SCREEN
// ============================================================================
@Composable
fun AppleIncomingCallScreen(
    modifier: Modifier = Modifier,
    callerName: String = "Oscellion User",
    callerSubtitle: String = "Oscellion Audio",
    callerPhoneNumber: String = "",
    profileImageUrl: String = "",
    isVerified: Boolean = false,
    initialStatus: CallStatus = CallStatus.INCOMING,
    initialVideoOn: Boolean = false,
    callManager: CallManager? = null,
    conferenceCandidates: List<String> = emptyList(),
    canAnswerWithoutPrompt: (() -> Boolean)? = null,
    onCallAccepted: () -> Unit = {},
    onCallEnded: (reason: String) -> Unit = {},
    onQuickMessageReply: ((String) -> Unit)? = null,
    onRemindLater: ((String) -> Unit)? = null,
    onAudioRouteChanged: (AudioOutputDevice) -> Unit = {},
    localCameraContent: (@Composable (isMirrored: Boolean, isPip: Boolean) -> Unit)? = null,
    remoteVideoContent: (@Composable (isPip: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val initialAudioDevice = remember(initialVideoOn) {
        if (initialVideoOn) AudioOutputDevice("Speaker", AudioDeviceType.SPEAKER)
        else AudioOutputDevice("Phone", AudioDeviceType.IPHONE)
    }
    val state = rememberAppleCallState(
        initialStatus = initialStatus,
        initialVideoOn = initialVideoOn,
        initialAudioDevice = initialAudioDevice
    )

    val haptics = LocalHapticFeedback.current
    val supportsNativeBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    // Real audio routes queried from AppAudioManager
    var availableAudioDevices by remember(callManager) {
        mutableStateOf(
            callManager?.audioManager?.getAvailableOutputDevices()?.takeIf { it.isNotEmpty() }
                ?: FallbackAudioDevices
        )
    }
    LaunchedEffect(state.showAudioDropdown, callManager) {
        if (state.showAudioDropdown && callManager != null) {
            availableAudioDevices = callManager.audioManager.getAvailableOutputDevices()
                .takeIf { it.isNotEmpty() } ?: FallbackAudioDevices
        }
    }

    // Live CallManager synchronization
    val rtcDurationFlow = remember(callManager) { callManager?.callDurationSec }
    val rtcMutedFlow = remember(callManager) { callManager?.isMicMuted }
    val rtcCamFlow = remember(callManager) { callManager?.isCameraEnabled }
    val rtcRemoteVideoFlow = remember(callManager) { callManager?.hasRemoteVideo }
    val rtcSpeakerFlow = remember(callManager) { callManager?.isSpeakerOn }
    val rtcStateFlow = remember(callManager) { callManager?.callState }

    val rtcDuration by (rtcDurationFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()
    val rtcMuted by (rtcMutedFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val rtcCamEnabled by (rtcCamFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(initialVideoOn) }).collectAsState()
    val rtcHasRemoteVideo by (rtcRemoteVideoFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val rtcSpeakerOn by (rtcSpeakerFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(initialVideoOn) }).collectAsState()
    val rtcCallState by (rtcStateFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(CallState.IDLE) }).collectAsState()

    LaunchedEffect(rtcDuration, state.callStatus) {
        if (callManager != null && state.callStatus == CallStatus.ACTIVE) {
            state.elapsedSeconds = rtcDuration
        }
    }
    LaunchedEffect(rtcMuted) {
        if (callManager != null) state.isMuted = rtcMuted
    }
    LaunchedEffect(rtcCamEnabled, state.callStatus) {
        if (callManager != null && state.callStatus == CallStatus.ACTIVE) {
            state.isVideoOn = rtcCamEnabled
            state.isLocalCameraLive = rtcCamEnabled
        }
    }
    LaunchedEffect(rtcHasRemoteVideo) {
        if (callManager != null) {
            state.isRemoteVideoOn = rtcHasRemoteVideo
            if (rtcHasRemoteVideo) {
                state.showRemoteVideoDialog = false
                state.showReinviteVideoLink = false
            }
        }
    }
    LaunchedEffect(rtcSpeakerOn) {
        if (callManager != null && state.selectedAudioDevice.type != AudioDeviceType.BLUETOOTH) {
            state.selectedAudioDevice = if (rtcSpeakerOn) {
                AudioOutputDevice("Speaker", AudioDeviceType.SPEAKER)
            } else {
                AudioOutputDevice("Phone", AudioDeviceType.IPHONE)
            }
        }
    }
    LaunchedEffect(rtcCallState) {
        if (callManager != null && rtcCallState == CallState.ACTIVE && state.callStatus == CallStatus.INCOMING) {
            state.acceptCall()
        }
    }

    val safeName = callerName.ifBlank { "Oscellion User" }
    val callerInitials = remember(safeName) {
        safeName.trim()
            .split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    // Auto-dismiss floating toast after 2.4s
    LaunchedEffect(state.toastTick) {
        if (state.toastMessage != null) {
            delay(2400)
            state.toastMessage = null
        }
    }

    // Fallback duration timer when rendered without active CallManager
    LaunchedEffect(state.callStatus, state.isOnHold, callManager, rtcCallState) {
        if (state.callStatus == CallStatus.ACTIVE && callManager == null) {
            while (true) {
                delay(1000)
                if (!state.isOnHold) {
                    state.elapsedSeconds++
                }
            }
        } else if (callManager != null && rtcCallState == CallState.CALLING) {
            state.elapsedSeconds = 0
        }
    }

    // Auto-fade controls & caller header after 3.5s of live FaceTime video
    LaunchedEffect(
        state.callStatus,
        state.isRemoteVideoOn,
        state.overlaysVisible,
        state.isOverlayBlocked,
        state.overlayInteractionTick
    ) {
        if (
            state.callStatus == CallStatus.ACTIVE &&
            state.isRemoteVideoOn &&
            state.overlaysVisible &&
            !state.isOverlayBlocked
        ) {
            delay(3500)
            state.overlaysVisible = false
        }
    }

    // Prompt only in preview mode (when no live WebRTC CallManager)
    LaunchedEffect(state.isVideoOn, state.callStatus, callManager) {
        if (callManager == null && state.isVideoOn && state.callStatus == CallStatus.ACTIVE && !state.isRemoteVideoOn) {
            delay(360)
            if (state.isVideoOn && state.callStatus == CallStatus.ACTIVE && !state.isRemoteVideoOn) {
                state.showRemoteVideoDialog = true
            }
        }
    }

    val displayedCallerName = state.mergedParticipant?.let {
        "${safeName.substringBefore(" ")} & ${it.substringBefore(" ")}"
    } ?: safeName

    val statusText = when (state.callStatus) {
        CallStatus.INCOMING -> callerSubtitle
        CallStatus.ENDED -> state.endedReason
        CallStatus.ACTIVE -> when {
            callManager != null && rtcCallState == CallState.CALLING -> "Calling…"
            state.isOnHold -> "On Hold"
            state.isRemoteVideoOn -> "FaceTime Video"
            state.isVideoOn && state.showReinviteVideoLink -> "Your Video On"
            state.isVideoOn -> "Video Connecting…"
            else -> "Connected"
        }
    }

    fun handleAccept() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val canImmediatelyTransition = canAnswerWithoutPrompt?.invoke() ?: true
        if (canImmediatelyTransition) {
            state.acceptCall()
        }
        onCallAccepted()
    }

    fun handleEndCall(customReason: String? = null) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val reason = state.endCall(customReason)
        onCallEnded(reason)
    }

    fun handleToggleVideo() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (callManager != null && state.callStatus == CallStatus.ACTIVE) {
            val enabled = callManager.toggleCamera()
            state.toggleVideo(enabled)
            return
        }
        state.toggleVideo()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
            .pointerInput(
                state.callStatus,
                state.isRemoteVideoOn,
                state.overlaysVisible,
                state.isOverlayBlocked
            ) {
                detectTapGestures {
                    if (state.callStatus == CallStatus.ACTIVE && state.isRemoteVideoOn && !state.isOverlayBlocked) {
                        state.overlaysVisible = !state.overlaysVisible
                        if (state.overlaysVisible) state.overlayInteractionTick++
                    }
                }
            }
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val pipWidth = 114.dp
        val pipHeight = 164.dp

        val topSafe = 96.dp
        val bottomSafe = if (state.overlaysVisible) 116.dp else 36.dp
        val sideMargin = 14.dp

        fun cornerOffset(corner: PipCorner): Pair<Dp, Dp> {
            val leftX = sideMargin
            val rightX = (screenW - pipWidth - sideMargin).coerceAtLeast(sideMargin)
            val topY = topSafe
            val bottomY = (screenH - pipHeight - bottomSafe).coerceAtLeast(topSafe)
            return when (corner) {
                PipCorner.TOP_LEFT -> leftX to topY
                PipCorner.TOP_RIGHT -> rightX to topY
                PipCorner.BOTTOM_LEFT -> leftX to bottomY
                PipCorner.BOTTOM_RIGHT -> rightX to bottomY
            }
        }

        val (snappedPipX, snappedPipY) = cornerOffset(state.pipCorner)

        // ====================================================================
        // 1. VIDEO STAGE LAYER (Fixed Composition Order + zIndex Morphing)
        // ====================================================================
        val remoteIsPip = state.isVideoOn && state.videoLayout == VideoLayoutMode.FOCUS_YOU
        val localIsPip = state.isVideoOn && state.videoLayout == VideoLayoutMode.FLOATING

        val remoteTargetX = if (remoteIsPip) snappedPipX else 0.dp
        val remoteTargetY = if (remoteIsPip) snappedPipY else 0.dp
        val remoteTargetW = when {
            remoteIsPip -> pipWidth
            state.isVideoOn && state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL -> screenW / 2
            else -> screenW
        }
        val remoteTargetH = when {
            remoteIsPip -> pipHeight
            state.isVideoOn && state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL -> screenH / 2
            else -> screenH
        }
        val remoteCornerRadius by animateDpAsState(
            targetValue = if (remoteIsPip) 22.dp else 0.dp,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "remoteCornerRadius"
        )

        val localTargetX = when {
            localIsPip -> snappedPipX
            state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL -> screenW / 2
            else -> 0.dp
        }
        val localTargetY = when {
            localIsPip -> snappedPipY
            state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL -> screenH / 2
            else -> 0.dp
        }
        val localTargetW = when {
            localIsPip -> pipWidth
            state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL -> screenW / 2
            else -> screenW
        }
        val localTargetH = when {
            localIsPip -> pipHeight
            state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL -> screenH / 2
            else -> screenH
        }
        val localCornerRadius by animateDpAsState(
            targetValue = if (localIsPip) 22.dp else 0.dp,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "localCornerRadius"
        )

        val bgBlurRadius by animateDpAsState(
            targetValue = if (state.isRemoteVideoOn) 0.dp else 31.dp,
            animationSpec = tween(550, easing = FastOutSlowInEasing),
            label = "bgBlur"
        )
        val bgDarkAlpha by animateFloatAsState(
            targetValue = when {
                state.isRemoteVideoOn -> 0.05f
                supportsNativeBlur -> 0.10f
                else -> 0.34f
            },
            animationSpec = tween(500),
            label = "bgDarkAlpha"
        )
        val scrimAlpha by animateFloatAsState(
            targetValue = if (state.isRemoteVideoOn && state.overlaysVisible) 1f else 0f,
            animationSpec = tween(400),
            label = "scrimAlpha"
        )

        val liveMotionTransition = rememberInfiniteTransition(label = "liveVideoMotion")
        val liveVideoScale by liveMotionTransition.animateFloat(
            initialValue = 1.03f,
            targetValue = 1.055f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveVideoScale"
        )

        // Remote Video Tile (fixed composition slot; promoted via zIndex + SurfaceView overlay when swapped)
        DraggableVideoTileContainer(
            zIndex = if (remoteIsPip) 3f else 0f,
            isDraggablePip = remoteIsPip,
            targetX = remoteTargetX,
            targetY = remoteTargetY,
            targetW = remoteTargetW,
            targetH = remoteTargetH,
            cornerRadius = remoteCornerRadius,
            screenW = screenW,
            screenH = screenH,
            onSnapCorner = { newCorner ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                state.pipCorner = newCorner
                state.overlayInteractionTick++
            },
            onDoubleTapPip = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                state.swapFloatingFocus()
            },
            onDragActiveChanged = { dragging ->
                if (!dragging) state.overlayInteractionTick++
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1D1D20))
            ) {
                if (state.isRemoteVideoOn && remoteVideoContent != null) {
                    remoteVideoContent(remoteIsPip)
                } else if (profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = safeName,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (state.isRemoteVideoOn) liveVideoScale else 1.14f)
                            .then(if (supportsNativeBlur) Modifier.blur(bgBlurRadius) else Modifier)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1F2937),
                                        Color(0xFF111827),
                                        Color(0xFF09090B)
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = bgDarkAlpha))
                )

                if (!remoteIsPip && scrimAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.TopCenter)
                            .alpha(scrimAlpha)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .align(Alignment.BottomCenter)
                            .alpha(scrimAlpha)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.46f))
                                )
                            )
                    )
                }

                if (state.isVideoOn && state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                } else if (state.isVideoOn && state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .align(Alignment.CenterEnd)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                }

                if (
                    state.isVideoOn &&
                    (state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL ||
                        state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL)
                ) {
                    Text(
                        text = safeName,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }

                if (remoteIsPip) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .width(28.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.55f))
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.52f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = safeName.substringBefore(" "),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            MiniPipActionCircle(
                                background = Color.White.copy(alpha = 0.22f),
                                onClick = { state.activeSheet = ActiveSheet.LAYOUT }
                            ) {
                                LayoutGridGlyph(size = 13.dp)
                            }
                            MiniPipActionCircle(
                                background = IosColors.Red.copy(alpha = 0.92f),
                                onClick = { handleToggleVideo() }
                            ) {
                                CloseCrossGlyph(size = 12.dp)
                            }
                        }
                    }
                }
            }
        }

        // Local ("You") Video Tile
        if (state.isVideoOn) {
            DraggableVideoTileContainer(
                zIndex = if (localIsPip) 3f else 1f,
                isDraggablePip = localIsPip,
                targetX = localTargetX,
                targetY = localTargetY,
                targetW = localTargetW,
                targetH = localTargetH,
                cornerRadius = localCornerRadius,
                screenW = screenW,
                screenH = screenH,
                onSnapCorner = { newCorner ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.pipCorner = newCorner
                    state.overlayInteractionTick++
                },
                onDoubleTapPip = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.swapFloatingFocus()
                },
                onDragActiveChanged = { dragging ->
                    if (!dragging) state.overlayInteractionTick++
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1D))
                ) {
                    if (localCameraContent != null && (state.isLocalCameraLive || callManager != null)) {
                        localCameraContent(state.isCameraMirrored, localIsPip)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = if (localIsPip) 12.dp else 16.dp,
                                    bottom = if (localIsPip) 40.dp else 16.dp,
                                    start = 8.dp,
                                    end = 8.dp
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MacOsGeometricAvatar(
                                startColor = if (state.isLocalCameraLive) Color(0xFF10B981) else Color(0xFF64748B),
                                endColor = if (state.isLocalCameraLive) Color(0xFF065F46) else Color(0xFF1E293B),
                                modifier = Modifier
                                    .size(if (localIsPip) 48.dp else 80.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.isLocalCameraLive) {
                                    if (state.isCameraMirrored) "Front Cam" else "Rear Cam"
                                } else "You",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = if (localIsPip) 11.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (localIsPip) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .iosClickable {
                                            state.isLocalCameraLive = !state.isLocalCameraLive
                                            state.showToast(
                                                if (state.isLocalCameraLive) "Camera preview active"
                                                else "Switched to avatar preview"
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (state.isLocalCameraLive) "Live" else "Use camera",
                                        color = IosColors.SkyAccent,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (localIsPip) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                                .width(28.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.45f))
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.52f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniPipActionCircle(
                                background = Color.White.copy(alpha = 0.20f),
                                onClick = {
                                    state.isCameraMirrored = !state.isCameraMirrored
                                    callManager?.switchCamera()
                                    state.showToast(if (state.isCameraMirrored) "Front camera" else "Rear camera")
                                }
                            ) {
                                FlipCameraGlyph(size = 13.dp)
                            }
                            MiniPipActionCircle(
                                background = Color.White.copy(alpha = 0.20f),
                                onClick = { state.activeSheet = ActiveSheet.LAYOUT }
                            ) {
                                LayoutGridGlyph(size = 13.dp)
                            }
                            MiniPipActionCircle(
                                background = IosColors.Red.copy(alpha = 0.92f),
                                onClick = { handleToggleVideo() }
                            ) {
                                CloseCrossGlyph(size = 12.dp)
                            }
                        }
                    } else if (
                        state.videoLayout == VideoLayoutMode.SPLIT_VERTICAL ||
                        state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(
                                    top = if (state.videoLayout == VideoLayoutMode.SPLIT_HORIZONTAL) 94.dp else 10.dp,
                                    start = 12.dp,
                                    end = 12.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "You",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniPipActionCircle(
                                    background = Color.Black.copy(alpha = 0.48f),
                                    onClick = {
                                        state.isCameraMirrored = !state.isCameraMirrored
                                        callManager?.switchCamera()
                                        state.showToast(if (state.isCameraMirrored) "Front camera" else "Rear camera")
                                    }
                                ) {
                                    FlipCameraGlyph(size = 13.dp)
                                }
                                MiniPipActionCircle(
                                    background = Color.Black.copy(alpha = 0.48f),
                                    onClick = { state.activeSheet = ActiveSheet.LAYOUT }
                                ) {
                                    LayoutGridGlyph(size = 13.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 2. FOREGROUND CALL INTERFACE (Dynamic Island, Header, Controls)
        // ====================================================================
        val headerAlpha by animateFloatAsState(
            targetValue = if (state.overlaysVisible) 1f else 0f,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "headerAlpha"
        )
        val headerOffsetY by animateDpAsState(
            targetValue = if (state.overlaysVisible) 0.dp else (-14).dp,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "headerOffsetY"
        )
        val controlsAlpha by animateFloatAsState(
            targetValue = if (state.overlaysVisible) 1f else 0f,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "controlsAlpha"
        )
        val controlsOffsetY by animateDpAsState(
            targetValue = if (state.overlaysVisible) 0.dp else 18.dp,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "controlsOffsetY"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            DynamicIslandCallPill(
                isActive = state.callStatus == CallStatus.ACTIVE,
                elapsedSeconds = state.elapsedSeconds,
                isMuted = state.isMuted,
                isOnHold = state.isOnHold,
                onClick = {
                    if (state.callStatus == CallStatus.ACTIVE && state.isRemoteVideoOn) {
                        state.pokeOverlays()
                    }
                }
            )

            val isFloatingLayout =
                state.videoLayout == VideoLayoutMode.FLOATING ||
                    state.videoLayout == VideoLayoutMode.FOCUS_YOU
            val headerAlignment = when {
                !state.isRemoteVideoOn -> Alignment.CenterHorizontally
                isFloatingLayout && state.pipCorner == PipCorner.TOP_LEFT -> Alignment.End
                else -> Alignment.Start
            }
            val headerStartPad by animateDpAsState(
                targetValue = if (state.isRemoteVideoOn && isFloatingLayout && state.pipCorner == PipCorner.TOP_LEFT) 136.dp else 24.dp,
                animationSpec = tween(360),
                label = "headerStartPad"
            )
            val headerEndPad by animateDpAsState(
                targetValue = if (state.isRemoteVideoOn && isFloatingLayout && state.pipCorner == PipCorner.TOP_RIGHT) 136.dp else 24.dp,
                animationSpec = tween(360),
                label = "headerEndPad"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (state.isRemoteVideoOn) 14.dp else 28.dp,
                        start = headerStartPad,
                        end = headerEndPad
                    )
                    .offset(y = headerOffsetY)
                    .alpha(headerAlpha),
                horizontalAlignment = headerAlignment
            ) {
                AnimatedVisibility(
                    visible = !state.isRemoteVideoOn,
                    enter = fadeIn(tween(320)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.78f)
                ) {
                    val haloTransition = rememberInfiniteTransition(label = "avatarHalo")
                    val haloScale by haloTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.24f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "haloScale"
                    )
                    val haloAlpha by haloTransition.animateFloat(
                        initialValue = 0.30f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "haloAlpha"
                    )

                    Box(
                        modifier = Modifier.padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.callStatus == CallStatus.INCOMING) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .scale(haloScale)
                                    .alpha(haloAlpha)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .shadow(16.dp, CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                                .border(2.dp, Color.White.copy(alpha = 0.36f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = callerInitials,
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp
                            )
                            if (profileImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = safeName,
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayedCallerName,
                        color = Color.White,
                        fontSize = if (state.isRemoteVideoOn) 23.sp else 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.6).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.mergedParticipant == null && isVerified) {
                        VerifiedBadgeSeal(
                            modifier = Modifier.size(if (state.isRemoteVideoOn) 18.dp else 22.dp)
                        )
                    }
                }

                if (state.callStatus == CallStatus.ACTIVE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCallTime(state.elapsedSeconds),
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                if (
                    state.callStatus == CallStatus.ACTIVE &&
                    state.isVideoOn &&
                    !state.isRemoteVideoOn &&
                    state.showReinviteVideoLink
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask ${safeName.substringBefore(" ")} to turn on video ›",
                        color = IosColors.SkyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .iosClickable(enabled = state.overlaysVisible) {
                                state.showRemoteVideoDialog = true
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            when (state.callStatus) {
                CallStatus.INCOMING -> {
                    IncomingCallControlsSection(
                        onRemindClick = { state.activeSheet = ActiveSheet.REMIND },
                        onMessageClick = { state.activeSheet = ActiveSheet.MESSAGE },
                        onDeclineClick = { handleEndCall("Call Declined") },
                        onAcceptSlide = { handleAccept() },
                        onTapHint = { state.showToast("Slide right to answer") }
                    )
                }

                CallStatus.ACTIVE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = controlsOffsetY)
                            .alpha(controlsAlpha)
                    ) {
                        ActiveCallControlsSection(
                            enabled = state.overlaysVisible,
                            isMuted = state.isMuted,
                            isVideoOn = state.isVideoOn,
                            isOnHold = state.isOnHold,
                            selectedAudioDevice = state.selectedAudioDevice,
                            onMuteClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val next = callManager?.toggleMicrophone() ?: !state.isMuted
                                state.toggleMute(next)
                            },
                            onVideoClick = { handleToggleVideo() },
                            onAudioDropdownClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                state.showAudioDropdown = !state.showAudioDropdown
                                state.overlayInteractionTick++
                            },
                            onAddCallClick = { state.activeSheet = ActiveSheet.ADD_CALL },
                            onHoldClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val nextHold = !state.isOnHold
                                callManager?.setOnHold(nextHold)
                                state.toggleHold(nextHold)
                            },
                            onContactsClick = { state.activeSheet = ActiveSheet.CONTACTS },
                            onEndCallClick = { handleEndCall() }
                        )
                    }
                }

                CallStatus.ENDED -> {
                    CallEndedControlsSection(
                        reason = state.endedReason
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(128.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.58f))
            )
        }

        // ====================================================================
        // 3. SOLID APPLE DARK AUDIO / BLUETOOTH OUTPUT DROPDOWN + SCRIM
        // ====================================================================
        if (state.showAudioDropdown && state.callStatus == CallStatus.ACTIVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        state.showAudioDropdown = false
                        state.overlayInteractionTick++
                    }
            )
        }

        AnimatedVisibility(
            visible = state.showAudioDropdown && state.callStatus == CallStatus.ACTIVE,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(5f)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 248.dp)
        ) {
            AudioOutputDropdownCard(
                devices = availableAudioDevices,
                selectedDevice = state.selectedAudioDevice,
                onSelectDevice = { device ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.selectedAudioDevice = device
                    state.showAudioDropdown = false
                    state.overlayInteractionTick++
                    callManager?.selectAudioDevice(device)
                    onAudioRouteChanged(device)
                    state.showToast("Audio routed to ${device.name}")
                }
            )
        }

        // ====================================================================
        // 4. SOLID POP-UP DIALOG FOR OTHER USER: TURN ON THEIR VIDEO REQUEST
        // ====================================================================
        AnimatedVisibility(
            visible = state.showRemoteVideoDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.zIndex(6f)
        ) {
            RemoteVideoRequestDialog(
                callerName = safeName,
                onKeepAudio = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.respondRemoteVideo(accepted = false, callerName = safeName)
                },
                onTurnOnVideo = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.respondRemoteVideo(accepted = true, callerName = safeName)
                }
            )
        }

        // ====================================================================
        // 5. UNIFIED SOLID DARK-MODE iOS ACTION SHEETS
        // ====================================================================
        AnimatedVisibility(
            visible = state.activeSheet != ActiveSheet.NONE,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.zIndex(5f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        state.activeSheet = ActiveSheet.NONE
                        state.overlayInteractionTick++
                    }
            )
        }

        AnimatedVisibility(
            visible = state.activeSheet != ActiveSheet.NONE,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(6f)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            when (state.activeSheet) {
                ActiveSheet.REMIND -> {
                    IosQuickActionSheet(
                        header = "Remind Me Later",
                        actions = listOf(
                            "In 1 hour" to {
                                onRemindLater?.invoke("In 1 hour")
                                handleEndCall("Reminder set for 1 hour")
                            },
                            "When I leave" to {
                                onRemindLater?.invoke("When I leave")
                                handleEndCall("Reminder set for when I leave")
                            }
                        ),
                        onDismiss = { state.activeSheet = ActiveSheet.NONE }
                    )
                }

                ActiveSheet.MESSAGE -> {
                    val m1 = "Sorry, I can’t talk right now."
                    val m2 = "I’m on my way."
                    val m3 = "Can I call you later?"
                    IosQuickActionSheet(
                        header = "Respond with Text",
                        actions = listOf(
                            m1 to {
                                onQuickMessageReply?.invoke(m1)
                                handleEndCall("Sent: $m1")
                            },
                            m2 to {
                                onQuickMessageReply?.invoke(m2)
                                handleEndCall("Sent: $m2")
                            },
                            m3 to {
                                onQuickMessageReply?.invoke(m3)
                                handleEndCall("Sent: $m3")
                            }
                        ),
                        onDismiss = { state.activeSheet = ActiveSheet.NONE }
                    )
                }

                ActiveSheet.ADD_CALL -> {
                    AddCallConferenceSheet(
                        participants = conferenceCandidates,
                        onMerge = { participant ->
                            state.mergedParticipant = participant
                            state.activeSheet = ActiveSheet.NONE
                            state.overlayInteractionTick++
                            state.showToast("Merged $participant into call")
                        },
                        onDismiss = {
                            state.activeSheet = ActiveSheet.NONE
                            state.overlayInteractionTick++
                        }
                    )
                }

                ActiveSheet.CONTACTS -> {
                    val displayHandle = callerPhoneNumber.ifBlank {
                        "@${safeName.lowercase().replace(" ", "")}"
                    }
                    ContactsCardSheet(
                        callerName = safeName,
                        phoneNumber = displayHandle,
                        profileImageUrl = profileImageUrl,
                        isVerified = isVerified,
                        onCopyNumber = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Contact Handle", displayHandle))
                            state.showToast("Contact info copied")
                        },
                        onDismiss = {
                            state.activeSheet = ActiveSheet.NONE
                            state.overlayInteractionTick++
                        }
                    )
                }

                ActiveSheet.LAYOUT -> {
                    VideoLayoutSelectorSheet(
                        currentLayout = state.videoLayout,
                        onSelectLayout = { layout ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.selectVideoLayout(layout)
                        },
                        onDismiss = {
                            state.activeSheet = ActiveSheet.NONE
                            state.overlayInteractionTick++
                        }
                    )
                }

                ActiveSheet.NONE -> Unit
            }
        }

        // ====================================================================
        // 6. FLOATING SOLID APPLE iOS TOAST NOTIFICATION
        // ====================================================================
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(7f)
                .statusBarsPadding()
                .padding(top = 50.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = IosColors.Sheet,
                border = BorderStroke(1.dp, IosColors.Divider),
                shadowElevation = 14.dp
            ) {
                Text(
                    text = state.toastMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ============================================================================
// DRAGGABLE WHATSAPP-STYLE VIDEO TILE CONTAINER (4-CORNER VELOCITY SNAP)
// ============================================================================
@Composable
private fun DraggableVideoTileContainer(
    zIndex: Float,
    isDraggablePip: Boolean,
    targetX: Dp,
    targetY: Dp,
    targetW: Dp,
    targetH: Dp,
    cornerRadius: Dp,
    screenW: Dp,
    screenH: Dp,
    onSnapCorner: (PipCorner) -> Unit,
    onDoubleTapPip: () -> Unit,
    onDragActiveChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    var lastVelX by remember { mutableFloatStateOf(0f) }
    var lastVelY by remember { mutableFloatStateOf(0f) }

    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "tileX"
    )
    val animY by animateDpAsState(
        targetValue = targetY,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "tileY"
    )
    val animW by animateDpAsState(
        targetValue = targetW,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tileW"
    )
    val animH by animateDpAsState(
        targetValue = targetH,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tileH"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.045f else 1f,
        animationSpec = tween(150),
        label = "pipDragScale"
    )

    val currentTargetXPx = with(density) { animX.toPx() }
    val currentTargetYPx = with(density) { animY.toPx() }
    val screenWPx = with(density) { screenW.toPx() }
    val screenHPx = with(density) { screenH.toPx() }
    val tileWPx = with(density) { targetW.toPx() }
    val tileHPx = with(density) { targetH.toPx() }

    val latestTargetXPx by rememberUpdatedState(currentTargetXPx)
    val latestTargetYPx by rememberUpdatedState(currentTargetYPx)
    val latestTileWPx by rememberUpdatedState(tileWPx)
    val latestTileHPx by rememberUpdatedState(tileHPx)
    val latestOnSnap by rememberUpdatedState(onSnapCorner)
    val latestOnDoubleTap by rememberUpdatedState(onDoubleTapPip)
    val latestOnDragChanged by rememberUpdatedState(onDragActiveChanged)

    val displayX = if (isDragging) {
        (currentTargetXPx + dragDeltaX).coerceIn(8f, (screenWPx - tileWPx - 8f).coerceAtLeast(8f))
    } else {
        currentTargetXPx
    }
    val displayY = if (isDragging) {
        (currentTargetYPx + dragDeltaY).coerceIn(48f, (screenHPx - tileHPx - 24f).coerceAtLeast(48f))
    } else {
        currentTargetYPx
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            .offset { IntOffset(displayX.roundToInt(), displayY.roundToInt()) }
            .width(animW)
            .height(animH)
            .scale(dragScale)
            .then(
                if (isDraggablePip) {
                    Modifier
                        .shadow(if (isDragging) 24.dp else 14.dp, shape, clip = false)
                        .clip(shape)
                        .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                        .pointerInput(isDraggablePip) {
                            detectTapGestures(
                                onDoubleTap = { latestOnDoubleTap() }
                            )
                        }
                        .pointerInput(isDraggablePip, screenWPx, screenHPx) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    lastVelX = 0f
                                    lastVelY = 0f
                                    latestOnDragChanged(true)
                                },
                                onDragEnd = {
                                    val finalLeft = latestTargetXPx + dragDeltaX
                                    val finalTop = latestTargetYPx + dragDeltaY
                                    val projectedCenterX = finalLeft + latestTileWPx / 2f + lastVelX * 10f
                                    val projectedCenterY = finalTop + latestTileHPx / 2f + lastVelY * 10f

                                    val isRight = projectedCenterX >= screenWPx / 2f
                                    val isBottom = projectedCenterY >= screenHPx / 2f
                                    val targetCorner = when {
                                        isBottom && isRight -> PipCorner.BOTTOM_RIGHT
                                        isBottom && !isRight -> PipCorner.BOTTOM_LEFT
                                        !isBottom && isRight -> PipCorner.TOP_RIGHT
                                        else -> PipCorner.TOP_LEFT
                                    }

                                    isDragging = false
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    latestOnSnap(targetCorner)
                                    latestOnDragChanged(false)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    latestOnDragChanged(false)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDeltaX += dragAmount.x
                                    dragDeltaY += dragAmount.y
                                    lastVelX = dragAmount.x
                                    lastVelY = dragAmount.y
                                }
                            )
                        }
                } else {
                    Modifier.clip(shape)
                }
            )
    ) {
        content()
    }
}

// ============================================================================
// DYNAMIC ISLAND LIVE CALL PILL
// ============================================================================
@Composable
private fun DynamicIslandCallPill(
    isActive: Boolean,
    elapsedSeconds: Int,
    isMuted: Boolean,
    isOnHold: Boolean,
    onClick: () -> Unit = {}
) {
    val islandWidth by animateDpAsState(
        targetValue = if (isActive) 176.dp else 112.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "islandWidth"
    )

    val accentColor = when {
        isOnHold -> IosColors.Amber
        isMuted -> IosColors.Red
        else -> IosColors.Green
    }

    val eqTransition = rememberInfiniteTransition(label = "eqBars")
    val barScales = listOf(0, 180, 360, 110).mapIndexed { index, delayMs ->
        eqTransition.animateFloat(
            initialValue = 0.30f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520 + index * 70, delayMillis = delayMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eqBar$index"
        )
    }

    Box(
        modifier = Modifier
            .width(islandWidth)
            .height(30.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.Black)
            .iosClickable(enabled = isActive, pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(150))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = formatCallTime(elapsedSeconds),
                        color = accentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(14.dp)
                ) {
                    val baseHeights = listOf(9.dp, 13.dp, 7.dp, 11.dp)
                    val paused = isOnHold || isMuted
                    barScales.forEachIndexed { idx, animState ->
                        Box(
                            modifier = Modifier
                                .width(2.2.dp)
                                .height(baseHeights[idx] * (if (paused) 0.42f else animState.value))
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = if (paused) 0.45f else 1f))
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// INCOMING CALL CONTROLS (REMIND ME, MESSAGE, DECLINE, SLIDE TO ANSWER)
// ============================================================================
@Composable
private fun IncomingCallControlsSection(
    onRemindClick: () -> Unit,
    onMessageClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onAcceptSlide: () -> Unit,
    onTapHint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassActionButton(
                label = "Remind Me",
                circleSize = 56.dp,
                onClick = onRemindClick
            ) {
                RemindMe3DIcon(modifier = Modifier.size(34.dp))
            }

            GlassActionButton(
                label = "Message",
                circleSize = 56.dp,
                onClick = onMessageClick
            ) {
                Message3DIcon(modifier = Modifier.size(34.dp))
            }
        }

        Spacer(modifier = Modifier.height(34.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(14.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(IosColors.Red)
                        .iosClickable { onDeclineClick() }
                        .semantics { contentDescription = "Decline incoming call" },
                    contentAlignment = Alignment.Center
                ) {
                    HandsetIcon(
                        modifier = Modifier.size(34.dp),
                        rotationDegrees = 135f
                    )
                }
                Text(
                    text = "Decline",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlideToAnswerTrack(
                    onSlideComplete = onAcceptSlide,
                    onTapWithoutSlide = onTapHint
                )
                Text(
                    text = "Accept",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SlideToAnswerTrack(
    onSlideComplete: () -> Unit,
    onTapWithoutSlide: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    var trackWidthPx by remember { mutableIntStateOf(0) }
    var crossedAnswerThreshold by remember { mutableStateOf(false) }
    val thumbSizeDp = 64.dp
    val trackPaddingDp = 3.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val trackPaddingPx = with(density) { trackPaddingDp.toPx() }
    val maxSlidePx = (trackWidthPx - thumbSizePx - trackPaddingPx * 2).coerceAtLeast(1f)

    val slideOffset = remember { Animatable(0f) }
    val slideProgress = (slideOffset.value / maxSlidePx).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "slideAnimations")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.52f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0
                0f at 1500
                -11f at 1640
                11f at 1780
                -8f at 1920
                8f at 2060
                0f at 2220
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "wiggleAngle"
    )
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )
    val chevronAlphas = listOf(0, 200, 400).mapIndexed { i, delayMs ->
        infiniteTransition.animateFloat(
            initialValue = 0.28f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, delayMillis = delayMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chevron$i"
        )
    }

    val latestOnSlideComplete by rememberUpdatedState(onSlideComplete)
    val latestOnTapWithoutSlide by rememberUpdatedState(onTapWithoutSlide)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .onSizeChanged { trackWidthPx = it.width }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.14f))
                ),
                shape = CircleShape
            )
            .semantics { contentDescription = "Slide right to answer call" },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (thumbSizePx + trackPaddingPx + slideOffset.value).toDp() })
                .clip(CircleShape)
                .background(IosColors.Green.copy(alpha = 0.36f))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 70.dp, end = 12.dp)
                .alpha((1f - slideProgress * 1.65f).coerceIn(0f, 1f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White,
                    Color.White.copy(alpha = 0.55f)
                ),
                start = Offset(x = -120f + shimmerPhase * 420f, y = 0f),
                end = Offset(x = 40f + shimmerPhase * 420f, y = 0f)
            )
            Text(
                text = "slide to answer",
                style = TextStyle(
                    brush = shimmerBrush,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                chevronAlphas.forEach { alphaState ->
                    ChevronRightGlyph(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(alphaState.value)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(start = trackPaddingDp)
                .offset { IntOffset(slideOffset.value.roundToInt(), 0) }
                .size(thumbSizeDp)
                .pointerInput(maxSlidePx) {
                    detectTapGestures {
                        scope.launch {
                            slideOffset.animateTo(
                                targetValue = with(density) { 16.dp.toPx() }.coerceAtMost(maxSlidePx),
                                animationSpec = tween(160)
                            )
                            slideOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
                            )
                        }
                        latestOnTapWithoutSlide()
                    }
                }
                .pointerInput(maxSlidePx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            crossedAnswerThreshold = false
                        },
                        onDragEnd = {
                            scope.launch {
                                if (slideOffset.value >= maxSlidePx * 0.72f) {
                                    slideOffset.animateTo(maxSlidePx, tween(110))
                                    latestOnSlideComplete()
                                } else {
                                    slideOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                                crossedAnswerThreshold = false
                            }
                        },
                        onDragCancel = {
                            crossedAnswerThreshold = false
                            scope.launch {
                                slideOffset.animateTo(0f, spring(dampingRatio = 0.75f))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (slideOffset.value + dragAmount).coerceIn(0f, maxSlidePx)
                            val isNowOverThreshold = next >= maxSlidePx * 0.72f
                            if (isNowOverThreshold != crossedAnswerThreshold) {
                                crossedAnswerThreshold = isNowOverThreshold
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            scope.launch {
                                slideOffset.snapTo(next)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .alpha(pulseAlpha * (1f - slideProgress))
                    .clip(CircleShape)
                    .background(IosColors.Green)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(IosColors.Green),
                contentAlignment = Alignment.Center
            ) {
                HandsetIcon(
                    modifier = Modifier
                        .size(31.dp)
                        .graphicsLayer { rotationZ = wiggleAngle },
                    rotationDegrees = 0f
                )
            }
        }
    }
}

// ============================================================================
// ACTIVE IN-CALL CONTROLS (3x2 LAYERED 3D GLASS ICON GRID + END CALL)
// ============================================================================
@Composable
private fun ActiveCallControlsSection(
    enabled: Boolean,
    isMuted: Boolean,
    isVideoOn: Boolean,
    isOnHold: Boolean,
    selectedAudioDevice: AudioOutputDevice,
    onMuteClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioDropdownClick: () -> Unit,
    onAddCallClick: () -> Unit,
    onHoldClick: () -> Unit,
    onContactsClick: () -> Unit,
    onEndCallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 304.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassActionButton(
                    label = if (isMuted) "unmute" else "mute",
                    isActive = isMuted,
                    enabled = enabled,
                    onClick = onMuteClick
                ) {
                    Mic3DIcon(isMuted = isMuted, modifier = Modifier.size(36.dp))
                }

                GlassActionButton(
                    label = if (isVideoOn) "video on" else "video",
                    isActive = isVideoOn,
                    enabled = enabled,
                    onClick = onVideoClick
                ) {
                    VideoCamera3DIcon(modifier = Modifier.size(36.dp))
                }

                GlassActionButton(
                    label = selectedAudioDevice.name.lowercase(),
                    isActive = selectedAudioDevice.type != AudioDeviceType.IPHONE,
                    enabled = enabled,
                    onClick = onAudioDropdownClick
                ) {
                    if (selectedAudioDevice.type == AudioDeviceType.BLUETOOTH) {
                        Bluetooth3DIcon(modifier = Modifier.size(36.dp))
                    } else {
                        Speaker3DIcon(modifier = Modifier.size(36.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassActionButton(
                    label = "add call",
                    enabled = enabled,
                    onClick = onAddCallClick
                ) {
                    AddCall3DIcon(modifier = Modifier.size(36.dp))
                }

                GlassActionButton(
                    label = if (isOnHold) "resume" else "hold",
                    isActive = isOnHold,
                    enabled = enabled,
                    onClick = onHoldClick
                ) {
                    Hold3DIcon(modifier = Modifier.size(36.dp))
                }

                GlassActionButton(
                    label = "contacts",
                    enabled = enabled,
                    onClick = onContactsClick
                ) {
                    Contacts3DIcon(modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(14.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(IosColors.Red)
                .iosClickable(enabled = enabled) { onEndCallClick() }
                .semantics { contentDescription = "End call" },
            contentAlignment = Alignment.Center
        ) {
            HandsetIcon(
                modifier = Modifier.size(34.dp),
                rotationDegrees = 135f
            )
        }
    }
}

@Composable
private fun GlassActionButton(
    label: String,
    circleSize: Dp = 64.dp,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val rimBrush = if (isActive) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.55f))
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.12f))
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(if (isActive) IosColors.GlassActive else IosColors.GlassIdle)
                .border(
                    width = if (isActive) 1.5.dp else 0.9.dp,
                    brush = rimBrush,
                    shape = CircleShape
                )
                .iosClickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CallEndedControlsSection(
    reason: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = reason,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RemoteVideoRequestDialog(
    callerName: String,
    onKeepAudio: () -> Unit,
    onTurnOnVideo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 296.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = IosColors.Sheet,
            border = BorderStroke(1.dp, IosColors.Divider),
            shadowElevation = 24.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IosColors.Surface)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(IosColors.Green)
                        )
                        Text(
                            text = "${callerName.uppercase()}'S SCREEN",
                            color = Color(0xFFD1D5DB),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(IosColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoCamera3DIcon(modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Turn On Your Video?",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "The other caller turned on their camera. Would you like to switch on your video too?",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = IosColors.Divider, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onKeepAudio() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep Audio",
                            color = Color(0xFFD1D5DB),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(IosColors.Divider)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTurnOnVideo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Turn On Video",
                            color = IosColors.Green,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioOutputDropdownCard(
    devices: List<AudioOutputDevice>,
    selectedDevice: AudioOutputDevice,
    onSelectDevice: (AudioOutputDevice) -> Unit
) {
    val hasBluetooth = devices.any { it.type == AudioDeviceType.BLUETOOTH }
    Surface(
        modifier = Modifier.width(248.dp),
        shape = RoundedCornerShape(18.dp),
        color = IosColors.Sheet,
        border = BorderStroke(1.dp, IosColors.Divider),
        shadowElevation = 24.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIO OUTPUT",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = if (hasBluetooth) "Bluetooth Ready" else "Audio Route",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider(color = IosColors.Divider, thickness = 1.dp)

            devices.forEachIndexed { idx, device ->
                val isSelected = device.name == selectedDevice.name && device.type == selectedDevice.type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDevice(device) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IosColors.Surface)
                                .border(0.8.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (device.type) {
                                AudioDeviceType.IPHONE -> PhoneGlyph(Color(0xFF7DD3FC), Modifier.size(15.dp))
                                AudioDeviceType.SPEAKER -> SpeakerSimpleGlyph(Color(0xFFA5B4FC), Modifier.size(15.dp))
                                AudioDeviceType.BLUETOOTH -> BluetoothSimpleGlyph(Color(0xFF93C5FD), Modifier.size(15.dp))
                            }
                        }
                        Column {
                            Text(
                                text = device.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (device.batteryLabel != null) {
                                Text(
                                    text = device.batteryLabel,
                                    color = if (device.batteryLabel.startsWith("8")) Color(0xFF34D399) else Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (isSelected) {
                        CheckmarkGlyph(color = IosColors.Blue, modifier = Modifier.size(16.dp))
                    }
                }
                if (idx < devices.lastIndex) {
                    HorizontalDivider(color = IosColors.Divider, thickness = 0.8.dp)
                }
            }
        }
    }
}

@Composable
private fun IosQuickActionSheet(
    header: String,
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = IosColors.Sheet,
            border = BorderStroke(1.dp, IosColors.Divider)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = header,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                actions.forEach { (label, onClick) ->
                    HorizontalDivider(color = IosColors.Divider, thickness = 0.8.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() }
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = IosColors.Blue,
                            fontSize = 15.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        SheetDismissButton(label = "Cancel", onClick = onDismiss)
    }
}

@Composable
private fun AddCallConferenceSheet(
    participants: List<String>,
    onMerge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = listOf(
        Color(0xFF3B82F6) to Color(0xFF1E3A8A),
        Color(0xFFE11D48) to Color(0xFF881337),
        Color(0xFF10B981) to Color(0xFF065F46)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = IosColors.Sheet,
            border = BorderStroke(1.dp, IosColors.Divider)
        ) {
            Column {
                Text(
                    text = "Add Participant to Call",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
                if (participants.isEmpty()) {
                    HorizontalDivider(color = IosColors.Divider, thickness = 0.8.dp)
                    Text(
                        text = "No recent direct contacts available",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    participants.forEachIndexed { idx, name ->
                        val (cStart, cEnd) = palette[idx % palette.size]
                        HorizontalDivider(color = IosColors.Divider, thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMerge(name) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MacOsGeometricAvatar(
                                    startColor = cStart,
                                    endColor = cEnd,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                )
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Merge",
                                color = IosColors.Blue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        SheetDismissButton(label = "Cancel", onClick = onDismiss)
    }
}

@Composable
private fun ContactsCardSheet(
    callerName: String,
    phoneNumber: String,
    profileImageUrl: String,
    isVerified: Boolean,
    onCopyNumber: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = IosColors.Sheet,
            border = BorderStroke(1.dp, IosColors.Divider)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = callerName,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IosColors.Surface)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    } else {
                        MacOsGeometricAvatar(
                            startColor = Color(0xFF38BDF8),
                            endColor = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = callerName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isVerified) {
                                VerifiedBadgeSeal(modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "Oscellion",
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosColors.Surface)
                        .border(1.dp, IosColors.Divider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "handle", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                        Text(
                            text = phoneNumber,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosColors.Blue.copy(alpha = 0.20f))
                            .clickable { onCopyNumber() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Copy",
                            color = IosColors.Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        SheetDismissButton(label = "Done", onClick = onDismiss)
    }
}

@Composable
private fun VideoLayoutSelectorSheet(
    currentLayout: VideoLayoutMode,
    onSelectLayout: (VideoLayoutMode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = IosColors.Sheet,
            border = BorderStroke(1.dp, IosColors.Divider)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Video Layout",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Drag floating video or double-tap PiP to swap",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                val rows = VideoLayoutMode.entries.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { mode ->
                                val selected = mode == currentLayout
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(IosColors.Surface)
                                        .border(
                                            width = 2.dp,
                                            color = if (selected) IosColors.Green else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectLayout(mode) }
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LayoutThumbnailPreview(mode = mode)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (selected) {
                                            CheckmarkGlyph(color = IosColors.Green, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SheetDismissButton(label = "Done", onClick = onDismiss)
    }
}

@Composable
private fun LayoutThumbnailPreview(mode: VideoLayoutMode) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(IosColors.Sheet)
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
    ) {
        when (mode) {
            VideoLayoutMode.FLOATING -> {
                Box(modifier = Modifier.fillMaxSize().background(IosColors.Green.copy(alpha = 0.22f)))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .width(16.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                )
            }
            VideoLayoutMode.SPLIT_VERTICAL -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(IosColors.Green.copy(alpha = 0.28f)))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White.copy(alpha = 0.78f)))
                }
            }
            VideoLayoutMode.SPLIT_HORIZONTAL -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosColors.Green.copy(alpha = 0.28f)))
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White.copy(alpha = 0.78f)))
                }
            }
            VideoLayoutMode.FOCUS_YOU -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.78f)))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .width(16.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(IosColors.Green.copy(alpha = 0.88f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun SheetDismissButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = IosColors.Sheet,
        border = BorderStroke(1.dp, IosColors.Divider)
    ) {
        Text(
            text = label,
            color = IosColors.Blue,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun MiniPipActionCircle(
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(background)
            .iosClickable(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ============================================================================
// CUSTOM 3D LAYERED GRADIENT + FROSTED-GLASS VECTOR ICONS (CANVAS)
// ============================================================================
private val IconLayerShadow = Color(0xFF0F172A).copy(alpha = 0.22f)

private fun glassFillBrush(w: Float, h: Float) = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.82f),
        Color.White.copy(alpha = 0.34f),
        Color.White.copy(alpha = 0.54f)
    ),
    start = Offset(0f, 0f),
    end = Offset(w, h)
)

private fun glassRimBrush(w: Float, h: Float) = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.96f),
        Color.White.copy(alpha = 0.30f),
        Color.White.copy(alpha = 0.82f)
    ),
    start = Offset(0f, 0f),
    end = Offset(w, h)
)

@Composable
private fun RemindMe3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFFFFD60A), Color(0xFFFF9F0A), Color(0xFFFF375F)),
            Offset(4f * s, 4f * s), Offset(30f * s, 30f * s)
        )
        drawLine(grad, Offset(11.2f * s, 6.4f * s), Offset(6.8f * s, 10.8f * s), strokeWidth = 2.8f * s, cap = StrokeCap.Round)
        drawLine(grad, Offset(24.8f * s, 6.4f * s), Offset(29.2f * s, 10.8f * s), strokeWidth = 2.8f * s, cap = StrokeCap.Round)
        drawCircle(grad, radius = 9.5f * s, center = Offset(15.5f * s, 17f * s))
        drawCircle(IconLayerShadow, radius = 9.5f * s, center = Offset(20f * s, 21.1f * s))
        drawCircle(glassFillBrush(size.width, size.height), radius = 9.5f * s, center = Offset(20f * s, 19.5f * s))
        drawCircle(glassRimBrush(size.width, size.height), radius = 9.5f * s, center = Offset(20f * s, 19.5f * s), style = Stroke(1.2f * s))
        drawLine(Color.White, Offset(20f * s, 14.8f * s), Offset(20f * s, 19.6f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(20f * s, 19.6f * s), Offset(23.3f * s, 21.5f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        drawCircle(Color.White, radius = 1.4f * s, center = Offset(20f * s, 19.6f * s))
    }
}

@Composable
private fun Message3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF3B82F6), Color(0xFF6366F1)),
            Offset(4f * s, 4f * s), Offset(30f * s, 30f * s)
        )
        drawOval(grad, topLeft = Offset(4.5f * s, 6f * s), size = Size(21.4f * s, 18.2f * s))
        val backTail = Path().apply {
            moveTo(7.4f * s, 20.6f * s)
            lineTo(5.8f * s, 25.4f * s)
            lineTo(11.4f * s, 23.4f * s)
            close()
        }
        drawPath(backTail, grad)

        val glass = glassFillBrush(size.width, size.height)
        val rim = glassRimBrush(size.width, size.height)
        drawOval(IconLayerShadow, topLeft = Offset(12.6f * s, 14.4f * s), size = Size(18.4f * s, 15.2f * s))
        drawOval(glass, topLeft = Offset(12.6f * s, 12.8f * s), size = Size(18.4f * s, 15.2f * s))
        drawOval(rim, topLeft = Offset(12.6f * s, 12.8f * s), size = Size(18.4f * s, 15.2f * s), style = Stroke(1.2f * s))
        drawLine(Color.White, Offset(17.5f * s, 18.8f * s), Offset(26.1f * s, 18.8f * s), strokeWidth = 2.1f * s, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(17.5f * s, 22.3f * s), Offset(22.9f * s, 22.3f * s), strokeWidth = 2.1f * s, cap = StrokeCap.Round)
    }
}

@Composable
private fun Mic3DIcon(isMuted: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF6366F1)),
            Offset(8f * s, 4f * s), Offset(26f * s, 28f * s)
        )
        drawRoundRect(grad, Offset(11.5f * s, 4.5f * s), Size(10.5f * s, 17f * s), CornerRadius(5.25f * s))
        drawArc(
            color = Color.White.copy(alpha = 0.9f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(8.5f * s, 7f * s),
            size = Size(19f * s, 19f * s),
            style = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
        )
        drawLine(Color.White.copy(alpha = 0.9f), Offset(18f * s, 26f * s), Offset(18f * s, 30.8f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(13.5f * s, 30.8f * s), Offset(22.5f * s, 30.8f * s), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        drawRoundRect(IconLayerShadow, Offset(14f * s, 8.1f * s), Size(10.5f * s, 16.5f * s), CornerRadius(5.25f * s))
        drawRoundRect(glassFillBrush(size.width, size.height), Offset(14f * s, 6.5f * s), Size(10.5f * s, 16.5f * s), CornerRadius(5.25f * s))
        drawRoundRect(glassRimBrush(size.width, size.height), Offset(14f * s, 6.5f * s), Size(10.5f * s, 16.5f * s), CornerRadius(5.25f * s), style = Stroke(1.15f * s))
        drawLine(Color.White.copy(alpha = 0.9f), Offset(17.2f * s, 12f * s), Offset(21.3f * s, 12f * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(17.2f * s, 15.2f * s), Offset(21.3f * s, 15.2f * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)

        if (isMuted) {
            drawLine(IosColors.Red, Offset(7f * s, 7f * s), Offset(29f * s, 29f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun VideoCamera3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF059669)),
            Offset(4f * s, 6f * s), Offset(30f * s, 28f * s)
        )
        drawRoundRect(grad, Offset(4.5f * s, 9f * s), Size(18f * s, 15f * s), CornerRadius(4.5f * s))
        val wedge = Path().apply {
            moveTo(22.5f * s, 14f * s)
            lineTo(31.2f * s, 9.6f * s)
            lineTo(31.2f * s, 23.4f * s)
            lineTo(22.5f * s, 19f * s)
            close()
        }
        drawPath(wedge, grad)
        drawRoundRect(IconLayerShadow, Offset(7f * s, 13.1f * s), Size(17.5f * s, 14.5f * s), CornerRadius(4.2f * s))
        drawRoundRect(glassFillBrush(size.width, size.height), Offset(7f * s, 11.5f * s), Size(17.5f * s, 14.5f * s), CornerRadius(4.2f * s))
        drawRoundRect(glassRimBrush(size.width, size.height), Offset(7f * s, 11.5f * s), Size(17.5f * s, 14.5f * s), CornerRadius(4.2f * s), style = Stroke(1.2f * s))
        drawCircle(Color.White.copy(alpha = 0.28f), radius = 3f * s, center = Offset(13.8f * s, 18.7f * s))
        drawCircle(Color.White, radius = 3f * s, center = Offset(13.8f * s, 18.7f * s), style = Stroke(1.6f * s))
        drawCircle(Color.White, radius = 1.15f * s, center = Offset(19.6f * s, 15.2f * s))
    }
}

@Composable
private fun Bluetooth3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF4F46E5)),
            Offset(5f * s, 4f * s), Offset(29f * s, 30f * s)
        )
        drawRoundRect(grad, Offset(6.5f * s, 5.5f * s), Size(17f * s, 22f * s), CornerRadius(8.5f * s))
        drawRoundRect(IconLayerShadow, Offset(11.5f * s, 9.1f * s), Size(17.5f * s, 22f * s), CornerRadius(8.75f * s))
        drawRoundRect(glassFillBrush(size.width, size.height), Offset(11.5f * s, 7.5f * s), Size(17.5f * s, 22f * s), CornerRadius(8.75f * s))
        drawRoundRect(glassRimBrush(size.width, size.height), Offset(11.5f * s, 7.5f * s), Size(17.5f * s, 22f * s), CornerRadius(8.75f * s), style = Stroke(1.2f * s))
        val bt = Path().apply {
            moveTo(16.2f * s, 13.5f * s)
            lineTo(23.7f * s, 20.3f * s)
            lineTo(19.9f * s, 23.8f * s)
            lineTo(19.9f * s, 10.2f * s)
            lineTo(23.7f * s, 13.7f * s)
            lineTo(16.2f * s, 20.5f * s)
        }
        drawPath(bt, Color.White, style = Stroke(width = 2.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun Speaker3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF8B5CF6)),
            Offset(4f * s, 6f * s), Offset(28f * s, 28f * s)
        )
        val backSpeaker = Path().apply {
            moveTo(4.5f * s, 14f * s)
            lineTo(9.2f * s, 14f * s)
            lineTo(17.5f * s, 7.5f * s)
            lineTo(17.5f * s, 28.5f * s)
            lineTo(9.2f * s, 22f * s)
            lineTo(4.5f * s, 22f * s)
            close()
        }
        drawPath(backSpeaker, grad)

        val shadowSpeaker = Path().apply {
            moveTo(7f * s, 16.5f * s)
            lineTo(11f * s, 16.5f * s)
            lineTo(19f * s, 10.5f * s)
            lineTo(19f * s, 30.5f * s)
            lineTo(11f * s, 24.5f * s)
            lineTo(7f * s, 24.5f * s)
            close()
        }
        drawPath(shadowSpeaker, IconLayerShadow)

        val frontSpeaker = Path().apply {
            moveTo(7f * s, 15f * s)
            lineTo(11f * s, 15f * s)
            lineTo(19f * s, 9f * s)
            lineTo(19f * s, 29f * s)
            lineTo(11f * s, 23f * s)
            lineTo(7f * s, 23f * s)
            close()
        }
        drawPath(frontSpeaker, glassFillBrush(size.width, size.height))
        drawPath(frontSpeaker, glassRimBrush(size.width, size.height), style = Stroke(1.15f * s, join = StrokeJoin.Round))

        drawArc(Color.White, -42f, 84f, false, Offset(17f * s, 12.2f * s), Size(9f * s, 12.6f * s), style = Stroke(2.1f * s, cap = StrokeCap.Round))
        drawArc(Color.White, -45f, 90f, false, Offset(19f * s, 8.6f * s), Size(12.5f * s, 19.8f * s), style = Stroke(2.1f * s, cap = StrokeCap.Round))
    }
}

@Composable
private fun AddCall3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFEA580C)),
            Offset(5f * s, 5f * s), Offset(29f * s, 29f * s)
        )
        drawRoundRect(grad, Offset(5.5f * s, 5.5f * s), Size(19f * s, 19f * s), CornerRadius(7f * s))
        drawRoundRect(IconLayerShadow, Offset(11.5f * s, 13.1f * s), Size(19f * s, 19f * s), CornerRadius(7f * s))
        drawRoundRect(glassFillBrush(size.width, size.height), Offset(11.5f * s, 11.5f * s), Size(19f * s, 19f * s), CornerRadius(7f * s))
        drawRoundRect(glassRimBrush(size.width, size.height), Offset(11.5f * s, 11.5f * s), Size(19f * s, 19f * s), CornerRadius(7f * s), style = Stroke(1.2f * s))
        drawLine(Color.White, Offset(21f * s, 16.5f * s), Offset(21f * s, 25.5f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(16.5f * s, 21f * s), Offset(25.5f * s, 21f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    }
}

@Composable
private fun Hold3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFFFDA4AF), Color(0xFFFB7185), Color(0xFFE11D48)),
            Offset(6f * s, 5f * s), Offset(28f * s, 29f * s)
        )
        drawRoundRect(grad, Offset(6.5f * s, 5.5f * s), Size(17f * s, 21f * s), CornerRadius(5.5f * s))
        drawRoundRect(IconLayerShadow, Offset(12.5f * s, 11.1f * s), Size(17f * s, 21f * s), CornerRadius(5.5f * s))
        drawRoundRect(glassFillBrush(size.width, size.height), Offset(12.5f * s, 9.5f * s), Size(17f * s, 21f * s), CornerRadius(5.5f * s))
        drawRoundRect(glassRimBrush(size.width, size.height), Offset(12.5f * s, 9.5f * s), Size(17f * s, 21f * s), CornerRadius(5.5f * s), style = Stroke(1.2f * s))
        drawRoundRect(Color.White, Offset(17.2f * s, 15f * s), Size(2.8f * s, 10f * s), CornerRadius(1.4f * s))
        drawRoundRect(Color.White, Offset(22.1f * s, 15f * s), Size(2.8f * s, 10f * s), CornerRadius(1.4f * s))
    }
}

@Composable
private fun Contacts3DIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        val grad = Brush.linearGradient(
            listOf(Color(0xFF60A5FA), Color(0xFF818CF8), Color(0xFFC084FC)),
            Offset(5f * s, 4f * s), Offset(29f * s, 30f * s)
        )
        drawCircle(grad, radius = 5f * s, center = Offset(15.5f * s, 11.5f * s))
        drawRoundRect(grad, Offset(6.8f * s, 19f * s), Size(17.4f * s, 10f * s), CornerRadius(8f * s))
        val glass = glassFillBrush(size.width, size.height)
        val rim = glassRimBrush(size.width, size.height)
        drawCircle(IconLayerShadow, radius = 4.8f * s, center = Offset(20.2f * s, 15f * s))
        drawRoundRect(IconLayerShadow, Offset(11.6f * s, 22.5f * s), Size(17.2f * s, 9.5f * s), CornerRadius(7.5f * s))
        drawCircle(glass, radius = 4.8f * s, center = Offset(20.2f * s, 13.5f * s))
        drawCircle(rim, radius = 4.8f * s, center = Offset(20.2f * s, 13.5f * s), style = Stroke(1.15f * s))
        drawRoundRect(glass, Offset(11.6f * s, 21f * s), Size(17.2f * s, 9.5f * s), CornerRadius(7.5f * s))
        drawRoundRect(rim, Offset(11.6f * s, 21f * s), Size(17.2f * s, 9.5f * s), CornerRadius(7.5f * s), style = Stroke(1.15f * s))
    }
}

@Composable
internal fun HandsetIcon(modifier: Modifier = Modifier, rotationDegrees: Float = 0f) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 36f
        rotate(rotationDegrees) {
            val handset = Path().apply {
                moveTo(27.3f * s, 22.4f * s)
                cubicTo(25.65f * s, 22.4f * s, 24.06f * s, 22.14f * s, 22.57f * s, 21.64f * s)
                cubicTo(22.05f * s, 21.46f * s, 21.48f * s, 21.6f * s, 21.13f * s, 21.98f * s)
                lineTo(18.95f * s, 24.46f * s)
                cubicTo(15.13f * s, 22.54f * s, 11.77f * s, 19.18f * s, 9.85f * s, 15.36f * s)
                lineTo(12.33f * s, 13.18f * s)
                cubicTo(12.71f * s, 12.83f * s, 12.85f * s, 12.26f * s, 12.67f * s, 11.74f * s)
                cubicTo(12.17f * s, 10.25f * s, 11.91f * s, 8.66f * s, 11.91f * s, 7.01f * s)
                cubicTo(11.91f * s, 6.21f * s, 11.26f * s, 5.56f * s, 10.46f * s, 5.56f * s)
                lineTo(6.12f * s, 5.56f * s)
                cubicTo(5.25f * s, 5.56f * s, 4.5f * s, 6.08f * s, 4.5f * s, 7.15f * s)
                cubicTo(4.5f * s, 19.86f * s, 14.82f * s, 30.18f * s, 27.53f * s, 30.18f * s)
                cubicTo(28.6f * s, 30.18f * s, 29.12f * s, 29.43f * s, 29.12f * s, 28.56f * s)
                lineTo(29.12f * s, 23.85f * s)
                cubicTo(29.12f * s, 23.05f * s, 28.47f * s, 22.4f * s, 27.67f * s, 22.4f * s)
                close()
            }
            drawPath(
                path = handset,
                brush = Brush.linearGradient(
                    listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
                    Offset(4f * s, 4f * s), Offset(30f * s, 30f * s)
                )
            )
        }
    }
}

@Composable
internal fun VerifiedBadgeSeal(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension * 0.48f
        val innerR = size.minDimension * 0.39f
        val lobes = 10

        val sealPath = Path()
        for (i in 0 until lobes * 2) {
            val angle = (i * PI / lobes) - PI / 2.0
            val nextAngle = ((i + 1) * PI / lobes) - PI / 2.0
            val r = if (i % 2 == 0) outerR else innerR
            val nextR = if (i % 2 == 0) innerR else outerR
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            val midAngle = (angle + nextAngle) / 2.0
            val ctrlR = (r + nextR) * 0.56f
            val ctrlX = cx + (cos(midAngle) * ctrlR).toFloat()
            val ctrlY = cy + (sin(midAngle) * ctrlR).toFloat()
            val nextX = cx + (cos(nextAngle) * nextR).toFloat()
            val nextY = cy + (sin(nextAngle) * nextR).toFloat()
            if (i == 0) sealPath.moveTo(x, y)
            sealPath.quadraticBezierTo(ctrlX, ctrlY, nextX, nextY)
        }
        sealPath.close()

        drawPath(
            path = sealPath,
            brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)))
        )
        drawPath(
            path = sealPath,
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.3f))
            ),
            style = Stroke(width = size.minDimension * 0.03f, join = StrokeJoin.Round)
        )

        val s = size.minDimension / 24f
        val check = Path().apply {
            moveTo(7.2f * s, 12.6f * s)
            lineTo(10.3f * s, 15.7f * s)
            lineTo(17.1f * s, 8.9f * s)
        }
        drawPath(
            path = check,
            color = Color.White,
            style = Stroke(width = 2.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun MacOsGeometricAvatar(
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 512f
        val circleClip = Path().apply {
            addOval(Rect(0f, 0f, size.width, size.height))
        }
        drawCircle(
            brush = Brush.verticalGradient(listOf(startColor, endColor)),
            radius = size.minDimension / 2f
        )
        clipPath(circleClip) {
            val silhouetteBrush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFD5D9E0))
            )
            drawCircle(silhouetteBrush, radius = 92f * s, center = Offset(256f * s, 168f * s))
            drawCircle(silhouetteBrush, radius = 244f * s, center = Offset(256f * s, 526f * s))
        }
    }
}

@Composable
private fun ChevronRightGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.18f)
            lineTo(w * 0.72f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.82f)
        }
        drawPath(path, Color.White, style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CheckmarkGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.76f)
            lineTo(w * 0.84f, h * 0.26f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun FlipCameraGlyph(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        drawRoundRect(Color.White, Offset(3.5f * s, 7f * s), Size(17f * s, 11.5f * s), CornerRadius(2.5f * s), style = Stroke(2f * s))
        drawCircle(Color.White, radius = 3.1f * s, center = Offset(12f * s, 12.7f * s), style = Stroke(1.9f * s))
        drawLine(Color.White, Offset(8.5f * s, 5.2f * s), Offset(15.5f * s, 5.2f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
    }
}

@Composable
private fun LayoutGridGlyph(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val r = CornerRadius(1.8f * s)
        val stroke = Stroke(2f * s)
        drawRoundRect(Color.White, Offset(3.5f * s, 3.5f * s), Size(7f * s, 7f * s), r, style = stroke)
        drawRoundRect(Color.White, Offset(13.5f * s, 3.5f * s), Size(7f * s, 7f * s), r, style = stroke)
        drawRoundRect(Color.White, Offset(3.5f * s, 13.5f * s), Size(7f * s, 7f * s), r, style = stroke)
        drawRoundRect(Color.White, Offset(13.5f * s, 13.5f * s), Size(7f * s, 7f * s), r, style = stroke)
    }
}

@Composable
private fun CloseCrossGlyph(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val p = this.size.minDimension * 0.22f
        val e = this.size.minDimension - p
        drawLine(Color.White, Offset(p, p), Offset(e, e), strokeWidth = this.size.minDimension * 0.18f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(e, p), Offset(p, e), strokeWidth = this.size.minDimension * 0.18f, cap = StrokeCap.Round)
    }
}

@Composable
private fun PhoneGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        drawRoundRect(color, Offset(7f * s, 2.5f * s), Size(10f * s, 19f * s), CornerRadius(2.5f * s), style = Stroke(2f * s))
        drawLine(color, Offset(10.5f * s, 18f * s), Offset(13.5f * s, 18f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
    }
}

@Composable
private fun SpeakerSimpleGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val path = Path().apply {
            moveTo(3.5f * s, 9f * s)
            lineTo(7f * s, 9f * s)
            lineTo(12f * s, 5f * s)
            lineTo(12f * s, 19f * s)
            lineTo(7f * s, 15f * s)
            lineTo(3.5f * s, 15f * s)
            close()
        }
        drawPath(path, color, style = Stroke(1.9f * s, join = StrokeJoin.Round))
        drawArc(color, -45f, 90f, false, Offset(13f * s, 8f * s), Size(5f * s, 8f * s), style = Stroke(1.9f * s, cap = StrokeCap.Round))
        drawArc(color, -45f, 90f, false, Offset(14.5f * s, 5f * s), Size(7f * s, 14f * s), style = Stroke(1.9f * s, cap = StrokeCap.Round))
    }
}

@Composable
private fun BluetoothSimpleGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val path = Path().apply {
            moveTo(7f * s, 7f * s)
            lineTo(17f * s, 16.5f * s)
            lineTo(12f * s, 21f * s)
            lineTo(12f * s, 3f * s)
            lineTo(17f * s, 7.5f * s)
            lineTo(7f * s, 17f * s)
        }
        drawPath(path, color, style = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun formatCallTime(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%02d:%02d".format(mins, secs)
}
