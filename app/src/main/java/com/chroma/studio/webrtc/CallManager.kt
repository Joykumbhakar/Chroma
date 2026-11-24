package com.chroma.studio.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class CallManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val signaling = CallSignalingClient()
    val audioManager = AppAudioManager(appContext)

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _activeCall = MutableStateFlow<CallModel?>(null)
    val activeCall: StateFlow<CallModel?> = _activeCall.asStateFlow()

    private val _incomingCall = MutableStateFlow<CallModel?>(null)
    val incomingCall: StateFlow<CallModel?> = _incomingCall.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _callDurationSec = MutableStateFlow(0)
    val callDurationSec: StateFlow<Int> = _callDurationSec.asStateFlow()

    var webRTCClient: WebRTCClient? = null
        private set

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var timerJob: Job? = null
    private var callSessionJob: Job? = null
    private var incomingCallJob: Job? = null
    private var candidateJob: Job? = null
    private var ringingTimeoutJob: Job? = null

    var onCallEndedListener: ((call: CallModel, durationSec: Int, status: String) -> Unit)? = null

    fun startListeningForIncomingCalls(currentUserId: String) {
        if (currentUserId.isBlank()) return
        incomingCallJob?.cancel()
        incomingCallJob = scope.launch {
            signaling.listenForIncomingCalls(currentUserId).collect { call ->
                if (call != null) {
                    if (_callState.value == CallState.IDLE) {
                        _incomingCall.value = call
                        _callState.value = CallState.RINGING
                    } else if (_activeCall.value?.id != call.id) {
                        // Busy line if already in another call
                        scope.launch {
                            try {
                                signaling.endCall(call.id, "busy")
                            } catch (_: Throwable) {}
                        }
                    }
                } else if (_callState.value == CallState.RINGING) {
                    _incomingCall.value = null
                    _callState.value = CallState.IDLE
                }
            }
        }
    }

    fun startCall(
        roomId: String,
        callerId: String,
        callerName: String,
        callerAvatar: String,
        calleeId: String,
        calleeName: String,
        calleeAvatar: String,
        isVideo: Boolean
    ) {
        if (_callState.value != CallState.IDLE) return

        val callId = "${if (roomId.isNotBlank()) roomId.replace("/", "_") else "call"}_${System.currentTimeMillis()}"
        val call = CallModel(
            id = callId,
            roomId = roomId,
            callerId = callerId,
            callerName = callerName,
            callerAvatar = callerAvatar,
            calleeId = calleeId,
            calleeName = calleeName,
            calleeAvatar = calleeAvatar,
            type = if (isVideo) "video" else "audio",
            status = "calling",
            timestamp = System.currentTimeMillis()
        )

        _activeCall.value = call
        _callState.value = CallState.CALLING
        _isCameraEnabled.value = isVideo
        _isSpeakerOn.value = isVideo
        _isMicMuted.value = false

        // 45-second Ringing Timeout
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = scope.launch {
            delay(45_000L)
            if (_callState.value == CallState.CALLING) {
                Log.w("CallManager", "Call timed out with no answer after 45s")
                endCall("missed")
            }
        }

        try {
            audioManager.start(isSpeakerDefault = isVideo)
            initWebRTC(isVideo)

            // Capture local camera if video
            if (isVideo) {
                webRTCClient?.startLocalVideoCapture(localRenderer)
            } else {
                webRTCClient?.startLocalAudio()
            }

            scope.launch {
                try {
                    signaling.createCall(call)

                    // Create Offer
                    webRTCClient?.createOffer(
                        onSuccess = { sdp ->
                            try {
                                webRTCClient?.setLocalDescription(sdp) {
                                    scope.launch {
                                        try {
                                            signaling.sendOffer(callId, sdp.description)
                                        } catch (e: Throwable) {
                                            Log.e("CallManager", "Error sending offer", e)
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e("CallManager", "Error setting local description", e)
                            }
                        },
                        onError = { error ->
                            Log.e("CallManager", "Create offer error: $error")
                            endCall()
                        }
                    )

                    // Listen for answer and state changes
                    listenToCallSession(callId, isCaller = true)
                } catch (e: Throwable) {
                    Log.e("CallManager", "Error in startCall coroutine", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("CallManager", "Error starting call", e)
            cleanupAndEnd()
        }
    }

    fun answerCall(call: CallModel) {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        _incomingCall.value = null
        _activeCall.value = call
        _callState.value = CallState.ACTIVE

        val isVideo = call.isVideo
        _isCameraEnabled.value = isVideo
        _isSpeakerOn.value = isVideo
        _isMicMuted.value = false

        try {
            audioManager.start(isSpeakerDefault = isVideo)
            initWebRTC(isVideo)

            if (isVideo) {
                webRTCClient?.startLocalVideoCapture(localRenderer)
            } else {
                webRTCClient?.startLocalAudio()
            }

            scope.launch {
                try {
                    signaling.updateCallStatus(call.id, "accepted")

                    // Set remote offer
                    val offerSdp = call.offer?.get("sdp")
                    if (offerSdp != null) {
                        val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                        webRTCClient?.setRemoteDescription(sdp) {
                            try {
                                webRTCClient?.createAnswer(
                                    onSuccess = { answerSdp ->
                                        try {
                                            webRTCClient?.setLocalDescription(answerSdp) {
                                                scope.launch {
                                                    try {
                                                        signaling.sendAnswer(call.id, answerSdp.description)
                                                    } catch (e: Throwable) {
                                                        Log.e("CallManager", "Error sending answer", e)
                                                    }
                                                }
                                            }
                                        } catch (e: Throwable) {
                                            Log.e("CallManager", "Error setting answer local description", e)
                                        }
                                    },
                                    onError = { error ->
                                        Log.e("CallManager", "Create answer error: $error")
                                        endCall("error")
                                    }
                                )
                            } catch (e: Throwable) {
                                Log.e("CallManager", "Error creating answer", e)
                            }
                        }
                    }

                    listenToCallSession(call.id, isCaller = false)
                    startCallTimer()
                } catch (e: Throwable) {
                    Log.e("CallManager", "Error in answerCall coroutine", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("CallManager", "Error answering call", e)
            cleanupAndEnd("error")
        }
    }

    fun rejectCall(call: CallModel) {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        scope.launch {
            try {
                signaling.endCall(call.id, "rejected")
            } catch (e: Throwable) {
                Log.e("CallManager", "Error rejecting call", e)
            }
        }
        _incomingCall.value = null
        _callState.value = CallState.IDLE
        try {
            onCallEndedListener?.invoke(call, 0, "rejected")
        } catch (e: Throwable) {
            Log.e("CallManager", "Error invoking onCallEndedListener on reject", e)
        }
    }

    private fun listenToCallSession(callId: String, isCaller: Boolean) {
        callSessionJob?.cancel()
        callSessionJob = scope.launch {
            signaling.listenToCall(callId).collect { session ->
                if (session == null || session.status in listOf("ended", "rejected", "busy", "missed")) {
                    cleanupAndEnd(session?.status ?: "ended")
                    return@collect
                }

                _activeCall.value = session

                if (session.status == "accepted") {
                    ringingTimeoutJob?.cancel()
                    ringingTimeoutJob = null
                }

                if (isCaller && session.status == "accepted" && session.answer != null) {
                    val answerSdp = session.answer["sdp"]
                    if (answerSdp != null && _callState.value != CallState.ACTIVE) {
                        val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                        webRTCClient?.setRemoteDescription(sdp)
                        _callState.value = CallState.ACTIVE
                        startCallTimer()
                    }
                }
            }
        }

        // Listen for candidates from peer
        candidateJob?.cancel()
        candidateJob = scope.launch {
            if (isCaller) {
                signaling.listenCalleeCandidates(callId).collect { candidate ->
                    webRTCClient?.addIceCandidate(candidate)
                }
            } else {
                signaling.listenCallerCandidates(callId).collect { candidate ->
                    webRTCClient?.addIceCandidate(candidate)
                }
            }
        }
    }

    private fun initWebRTC(isVideo: Boolean) {
        try {
            val observer = object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                        state == PeerConnection.IceConnectionState.FAILED ||
                        state == PeerConnection.IceConnectionState.CLOSED) {
                        scope.launch { cleanupAndEnd() }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { ice ->
                        val call = _activeCall.value ?: return
                        val isCaller = call.callerId != "" && call.callerId != call.calleeId
                        scope.launch {
                            try {
                                if (_callState.value == CallState.CALLING || call.status == "calling") {
                                    signaling.addCallerCandidate(call.id, ice)
                                } else {
                                    signaling.addCalleeCandidate(call.id, ice)
                                }
                            } catch (e: Throwable) {
                                Log.e("CallManager", "Error adding ice candidate", e)
                            }
                        }
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {
                    if (stream != null && stream.videoTracks.isNotEmpty()) {
                        val remoteTrack = stream.videoTracks[0]
                        webRTCClient?.setRemoteVideoTrack(remoteTrack)
                    }
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        webRTCClient?.setRemoteVideoTrack(track)
                    }
                }
            }

            webRTCClient = WebRTCClient(appContext, observer)
        } catch (e: Throwable) {
            Log.e("CallManager", "Error creating WebRTCClient", e)
            webRTCClient = null
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        _callDurationSec.value = 0
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDurationSec.value += 1
            }
        }
    }

    fun attachLocalSurface(renderer: SurfaceViewRenderer) {
        try {
            localRenderer = renderer
            webRTCClient?.attachLocalSink(renderer)
        } catch (e: Throwable) {
            Log.e("CallManager", "Error attaching local surface", e)
        }
    }

    fun detachLocalSurface(renderer: SurfaceViewRenderer) {
        try {
            if (localRenderer == renderer) localRenderer = null
            webRTCClient?.detachLocalSink(renderer)
        } catch (e: Throwable) {
            Log.e("CallManager", "Error detaching local surface", e)
        }
    }

    fun attachRemoteSurface(renderer: SurfaceViewRenderer) {
        try {
            remoteRenderer = renderer
            webRTCClient?.attachRemoteSink(renderer)
        } catch (e: Throwable) {
            Log.e("CallManager", "Error attaching remote surface", e)
        }
    }

    fun detachRemoteSurface(renderer: SurfaceViewRenderer) {
        try {
            if (remoteRenderer == renderer) remoteRenderer = null
            webRTCClient?.detachRemoteSink(renderer)
        } catch (e: Throwable) {
            Log.e("CallManager", "Error detaching remote surface", e)
        }
    }

    fun toggleMicrophone(): Boolean {
        val next = !_isMicMuted.value
        _isMicMuted.value = next
        webRTCClient?.setAudioEnabled(!next)
        audioManager.setMicrophoneMute(next)
        return next
    }

    fun toggleCamera(): Boolean {
        val next = !_isCameraEnabled.value
        _isCameraEnabled.value = next
        webRTCClient?.setVideoEnabled(next)
        return next
    }

    fun toggleSpeaker(): Boolean {
        val next = !_isSpeakerOn.value
        _isSpeakerOn.value = next
        audioManager.setSpeakerphone(next)
        return next
    }

    fun switchCamera() {
        webRTCClient?.switchCamera()
    }

    fun endCall(reason: String = "ended") {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        val currentCall = _activeCall.value
        if (currentCall != null) {
            scope.launch {
                try {
                    signaling.endCall(currentCall.id, reason)
                } catch (e: Throwable) {
                    Log.e("CallManager", "Error ending call", e)
                }
            }
        }
        cleanupAndEnd(reason)
    }

    private fun cleanupAndEnd(reason: String = "ended") {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        timerJob?.cancel()
        timerJob = null
        callSessionJob?.cancel()
        callSessionJob = null
        candidateJob?.cancel()
        candidateJob = null

        audioManager.stop()

        webRTCClient?.destroy()
        webRTCClient = null
        localRenderer = null
        remoteRenderer = null

        val lastCall = _activeCall.value
        val lastDuration = _callDurationSec.value

        _activeCall.value = null
        _incomingCall.value = null
        _callDurationSec.value = 0
        _callState.value = CallState.ENDED

        if (lastCall != null) {
            try {
                onCallEndedListener?.invoke(lastCall, lastDuration, reason)
            } catch (e: Throwable) {
                Log.e("CallManager", "Error invoking onCallEndedListener", e)
            }
        }

        scope.launch {
            delay(500)
            if (_callState.value == CallState.ENDED) {
                _callState.value = CallState.IDLE
            }
        }
    }
}
