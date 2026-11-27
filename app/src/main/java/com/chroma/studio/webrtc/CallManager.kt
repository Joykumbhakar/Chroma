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

    private val _connectionQuality = MutableStateFlow(CallConnectionQuality.CONNECTING)
    val connectionQuality: StateFlow<CallConnectionQuality> = _connectionQuality.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _hasRemoteVideo = MutableStateFlow(false)
    val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private val _isCaller = MutableStateFlow(false)
    val isCaller: StateFlow<Boolean> = _isCaller.asStateFlow()

    private val _webRTCClient = MutableStateFlow<WebRTCClient?>(null)
    val webRTCClientFlow: StateFlow<WebRTCClient?> = _webRTCClient.asStateFlow()

    var webRTCClient: WebRTCClient?
        get() = _webRTCClient.value
        private set(value) {
            _webRTCClient.value = value
        }

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var timerJob: Job? = null
    private var callSessionJob: Job? = null
    private var incomingCallJob: Job? = null
    private var candidateJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private var iceDisconnectJob: Job? = null

    // Synchronization flags preventing ICE/SDP/role race conditions
    @Volatile private var listeningUserId: String? = null
    @Volatile private var isCallerRole: Boolean = false
    @Volatile private var callDocReady: Boolean = false
    @Volatile private var hasRemoteDescription: Boolean = false
    @Volatile private var lastAppliedRemoteOfferSdp: String? = null
    @Volatile private var lastAppliedRemoteAnswerSdp: String? = null
    @Volatile private var timerStarted: Boolean = false
    @Volatile private var isCleaningUp: Boolean = false
    private val iceLock = Any()
    private val pendingLocalCandidates = mutableListOf<IceCandidate>()
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()
    private val addedRemoteCandidateKeys = HashSet<String>()

    var onCallEndedListener: ((call: CallModel, durationSec: Int, status: String) -> Unit)? = null

    fun startListeningForIncomingCalls(currentUserId: String) {
        if (currentUserId.isBlank()) return
        if (listeningUserId == currentUserId && incomingCallJob?.isActive == true) return
        listeningUserId = currentUserId
        incomingCallJob?.cancel()
        incomingCallJob = scope.launch {
            signaling.listenForIncomingCalls(currentUserId).collect { call ->
                if (call != null) {
                    if (_callState.value == CallState.IDLE) {
                        _incomingCall.value = call
                        _callState.value = CallState.RINGING
                    } else if (_activeCall.value?.id != call.id && _incomingCall.value?.id != call.id) {
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

    fun stopListeningForIncomingCalls() {
        listeningUserId = null
        incomingCallJob?.cancel()
        incomingCallJob = null
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

        isCleaningUp = false
        isCallerRole = true
        _isCaller.value = true
        callDocReady = false
        hasRemoteDescription = false
        lastAppliedRemoteOfferSdp = null
        lastAppliedRemoteAnswerSdp = null
        timerStarted = false
        synchronized(iceLock) {
            pendingLocalCandidates.clear()
            pendingRemoteCandidates.clear()
            addedRemoteCandidateKeys.clear()
        }

        _activeCall.value = call
        _callState.value = CallState.CALLING
        _connectionQuality.value = CallConnectionQuality.CONNECTING
        _hasRemoteVideo.value = false
        _isFrontCamera.value = true
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

            // Capture local camera if video, or local audio track
            if (isVideo) {
                val started = webRTCClient?.startLocalVideoCapture(localRenderer) ?: false
                if (!started) {
                    _isCameraEnabled.value = false
                }
            } else {
                webRTCClient?.startLocalAudio()
            }

            // Start listening immediately for peer status changes / answer / candidates
            listenToCallSession(callId, isCaller = true)

            scope.launch {
                try {
                    // Create Offer FIRST, publish call doc with offer populated,
                    // then set local description and flush buffered local ICE candidates.
                    webRTCClient?.createOffer(
                        isVideo = isVideo,
                        onSuccess = { sdp ->
                            scope.launch {
                                val callWithOffer = call.copy(
                                    offer = mapOf("type" to "OFFER", "sdp" to sdp.description)
                                )
                                val created = signaling.createCall(callWithOffer)
                                if (!created) {
                                    Log.e("CallManager", "Failed to create Firestore call document")
                                    cleanupAndEnd("error")
                                    return@launch
                                }
                                callDocReady = true
                                _activeCall.value = callWithOffer

                                webRTCClient?.setLocalDescription(
                                    desc = sdp,
                                    onError = { err ->
                                        Log.e("CallManager", "Set local offer error: $err")
                                        scope.launch { cleanupAndEnd("error") }
                                    }
                                ) {
                                    flushPendingLocalCandidates(callId)
                                }
                            }
                        },
                        onError = { error ->
                            Log.e("CallManager", "Create offer error: $error")
                            endCall("error")
                        }
                    )
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
        if (_callState.value == CallState.ACTIVE && _activeCall.value?.id == call.id) return
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        isCleaningUp = false
        isCallerRole = false
        _isCaller.value = false
        callDocReady = true
        hasRemoteDescription = false
        lastAppliedRemoteOfferSdp = null
        lastAppliedRemoteAnswerSdp = null
        timerStarted = false
        synchronized(iceLock) {
            pendingLocalCandidates.clear()
            pendingRemoteCandidates.clear()
            addedRemoteCandidateKeys.clear()
        }

        _incomingCall.value = null
        _activeCall.value = call
        _callState.value = CallState.ACTIVE
        _connectionQuality.value = CallConnectionQuality.CONNECTING
        _hasRemoteVideo.value = false
        _isFrontCamera.value = true

        val isVideo = call.isVideo
        _isCameraEnabled.value = isVideo
        _isSpeakerOn.value = isVideo
        _isMicMuted.value = false

        try {
            audioManager.start(isSpeakerDefault = isVideo)
            initWebRTC(isVideo)

            if (isVideo) {
                val started = webRTCClient?.startLocalVideoCapture(localRenderer) ?: false
                if (!started) {
                    _isCameraEnabled.value = false
                }
            } else {
                webRTCClient?.startLocalAudio()
            }

            listenToCallSession(call.id, isCaller = false)

            scope.launch {
                try {
                    signaling.updateCallStatus(call.id, "accepted")
                    val offerSdp = call.offer?.get("sdp")
                    if (!offerSdp.isNullOrBlank()) {
                        applyRemoteOfferAndSendAnswer(call.id, offerSdp, isVideo)
                    }
                } catch (e: Throwable) {
                    Log.e("CallManager", "Error in answerCall coroutine", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("CallManager", "Error answering call", e)
            cleanupAndEnd("error")
        }
    }

    private fun applyRemoteOfferAndSendAnswer(callId: String, offerSdp: String, isVideo: Boolean) {
        if (lastAppliedRemoteOfferSdp == offerSdp || isCleaningUp) return
        lastAppliedRemoteOfferSdp = offerSdp
        val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        webRTCClient?.setRemoteDescription(
            desc = sdp,
            onError = { err ->
                Log.e("CallManager", "Set remote offer failed: $err")
                scope.launch { cleanupAndEnd("error") }
            }
        ) {
            hasRemoteDescription = true
            flushPendingRemoteCandidates()
            webRTCClient?.createAnswer(
                isVideo = isVideo || _isCameraEnabled.value,
                onSuccess = { answerSdp ->
                    webRTCClient?.setLocalDescription(
                        desc = answerSdp,
                        onError = { err ->
                            Log.e("CallManager", "Set local answer failed: $err")
                            scope.launch { cleanupAndEnd("error") }
                        }
                    ) {
                        scope.launch {
                            try {
                                signaling.sendAnswer(callId, answerSdp.description)
                                flushPendingLocalCandidates(callId)
                                startCallTimerOnce()
                            } catch (e: Throwable) {
                                Log.e("CallManager", "Error sending answer", e)
                            }
                        }
                    }
                },
                onError = { error ->
                    Log.e("CallManager", "Create answer error: $error")
                    endCall("error")
                }
            )
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
                if (session == null || session.status in listOf("ended", "rejected", "busy", "missed", "error")) {
                    // Caller might start listening right before createCall commits on slow networks
                    if (session == null && isCaller && !callDocReady) return@collect
                    cleanupAndEnd(session?.status ?: "ended")
                    return@collect
                }

                _activeCall.value = session

                if (session.status == "accepted") {
                    ringingTimeoutJob?.cancel()
                    ringingTimeoutJob = null
                }

                val offerSdp = session.offer?.get("sdp")
                if (!offerSdp.isNullOrBlank() && offerSdp != lastAppliedRemoteOfferSdp && (!isCaller || session.status == "accepted")) {
                    // Handles initial callee offer as well as mid-call renegotiation offers
                    if (!isCaller || session.offer["type"] == "OFFER") {
                        if (!isCaller) {
                            applyRemoteOfferAndSendAnswer(callId, offerSdp, session.isVideo)
                        }
                    }
                }

                val answerSdp = session.answer?.get("sdp")
                if (!answerSdp.isNullOrBlank() && answerSdp != lastAppliedRemoteAnswerSdp && isCaller) {
                    lastAppliedRemoteAnswerSdp = answerSdp
                    val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                    webRTCClient?.setRemoteDescription(
                        desc = sdp,
                        onError = { err ->
                            Log.e("CallManager", "Set remote answer failed: $err")
                        }
                    ) {
                        hasRemoteDescription = true
                        flushPendingRemoteCandidates()
                        scope.launch {
                            if (_callState.value != CallState.ACTIVE) {
                                _callState.value = CallState.ACTIVE
                            }
                            startCallTimerOnce()
                        }
                    }
                }
            }
        }

        // Listen for candidates from peer, buffering until remoteDescription is set and deduplicating
        candidateJob?.cancel()
        candidateJob = scope.launch {
            val flow = if (isCaller) signaling.listenCalleeCandidates(callId) else signaling.listenCallerCandidates(callId)
            flow.collect { candidate ->
                val key = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.sdp}"
                val shouldAddNow = synchronized(iceLock) {
                    if (!addedRemoteCandidateKeys.add(key)) {
                        return@collect
                    }
                    if (hasRemoteDescription) true
                    else {
                        pendingRemoteCandidates.add(candidate)
                        false
                    }
                }
                if (shouldAddNow) {
                    webRTCClient?.addIceCandidate(candidate)
                }
            }
        }
    }

    private fun flushPendingLocalCandidates(callId: String) {
        val toSend = synchronized(iceLock) {
            val copy = pendingLocalCandidates.toList()
            pendingLocalCandidates.clear()
            copy
        }
        if (toSend.isEmpty()) return
        scope.launch {
            for (ice in toSend) {
                try {
                    if (isCallerRole) signaling.addCallerCandidate(callId, ice)
                    else signaling.addCalleeCandidate(callId, ice)
                } catch (e: Throwable) {
                    Log.e("CallManager", "Error flushing local ICE candidate", e)
                }
            }
        }
    }

    private fun flushPendingRemoteCandidates() {
        val toApply = synchronized(iceLock) {
            val copy = pendingRemoteCandidates.toList()
            pendingRemoteCandidates.clear()
            copy
        }
        for (ice in toApply) {
            webRTCClient?.addIceCandidate(ice)
        }
    }

    private fun initWebRTC(isVideo: Boolean) {
        try {
            val observer = object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.NEW,
                        PeerConnection.IceConnectionState.CHECKING -> {
                            _connectionQuality.value = CallConnectionQuality.CONNECTING
                        }
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            iceDisconnectJob?.cancel()
                            iceDisconnectJob = null
                            _connectionQuality.value = CallConnectionQuality.GOOD
                            scope.launch { startCallTimerOnce() }
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            _connectionQuality.value = CallConnectionQuality.RECONNECTING
                            // Give ICE 8s to recover across transient Wi-Fi / 4G switches before ending
                            iceDisconnectJob?.cancel()
                            iceDisconnectJob = scope.launch {
                                delay(8_000L)
                                if (!isCleaningUp && _callState.value == CallState.ACTIVE) {
                                    _connectionQuality.value = CallConnectionQuality.DISCONNECTED
                                    cleanupAndEnd("ended")
                                }
                            }
                        }
                        PeerConnection.IceConnectionState.FAILED,
                        PeerConnection.IceConnectionState.CLOSED -> {
                            _connectionQuality.value = CallConnectionQuality.DISCONNECTED
                            scope.launch {
                                if (!isCleaningUp && _callState.value != CallState.IDLE) {
                                    cleanupAndEnd("ended")
                                }
                            }
                        }
                        null -> {}
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { ice ->
                        val call = _activeCall.value ?: return
                        val sendNow = synchronized(iceLock) {
                            if (callDocReady) true
                            else {
                                pendingLocalCandidates.add(ice)
                                false
                            }
                        }
                        if (!sendNow) return
                        scope.launch {
                            try {
                                if (isCallerRole) {
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
                        _hasRemoteVideo.value = true
                        webRTCClient?.setRemoteVideoTrack(remoteTrack)
                    }
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        _hasRemoteVideo.value = true
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

    private fun startCallTimerOnce() {
        if (timerStarted || isCleaningUp) return
        timerStarted = true
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
        if (next && webRTCClient?.localVideoTrack == null) {
            val started = webRTCClient?.startLocalVideoCapture(localRenderer) ?: false
            if (!started) {
                _isCameraEnabled.value = false
                return false
            }
            _isCameraEnabled.value = true
            _isFrontCamera.value = webRTCClient?.isFrontCamera?.value ?: true
            // Renegotiate if an active audio-only call is upgrading to video mid-call
            val activeId = _activeCall.value?.id
            val signalingStable = webRTCClient?.peerConnection?.signalingState() == PeerConnection.SignalingState.STABLE
            if (!activeId.isNullOrBlank() && _callState.value == CallState.ACTIVE && signalingStable && isCallerRole) {
                webRTCClient?.createOffer(
                    isVideo = true,
                    onSuccess = { offerSdp ->
                        webRTCClient?.setLocalDescription(offerSdp) {
                            scope.launch { signaling.sendRenegotiationOffer(activeId, offerSdp.description) }
                        }
                    }
                )
            }
            return true
        }
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
        webRTCClient?.switchCamera(onSwitched = { isFront ->
            _isFrontCamera.value = isFront
        })
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
        if (isCleaningUp) return
        isCleaningUp = true
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = null
        iceDisconnectJob?.cancel()
        iceDisconnectJob = null
        timerJob?.cancel()
        timerJob = null
        callSessionJob?.cancel()
        callSessionJob = null
        candidateJob?.cancel()
        candidateJob = null
        callDocReady = false
        hasRemoteDescription = false
        lastAppliedRemoteOfferSdp = null
        lastAppliedRemoteAnswerSdp = null
        timerStarted = false
        synchronized(iceLock) {
            pendingLocalCandidates.clear()
            pendingRemoteCandidates.clear()
            addedRemoteCandidateKeys.clear()
        }

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
        _connectionQuality.value = CallConnectionQuality.DISCONNECTED
        _hasRemoteVideo.value = false
        _isCaller.value = false
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
