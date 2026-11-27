package com.chroma.studio.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.concurrent.atomic.AtomicBoolean

class WebRTCClient(
    context: Context,
    private val observer: PeerConnection.Observer
) {
    private val context = context.applicationContext
    private val isDestroyed = AtomicBoolean(false)
    @Volatile
    private var eglCreated = false

    companion object {
        @Volatile
        private var factoryInitialized = false

        fun ensureFactoryInitialized(context: Context) {
            if (factoryInitialized) return
            synchronized(this) {
                if (factoryInitialized) return
                try {
                    val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                    PeerConnectionFactory.initialize(options)
                    factoryInitialized = true
                } catch (e: Throwable) {
                    Log.e("WebRTCClient", "PeerConnectionFactory.initialize error", e)
                }
            }
        }
    }

    val rootEglBase: EglBase by lazy {
        eglCreated = true
        EglBase.create()
    }
    val eglBaseContext: EglBase.Context
        get() = rootEglBase.eglBaseContext

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        ensureFactoryInitialized(this.context)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(
            rootEglBase.eglBaseContext
        )

        PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    val peerConnection: PeerConnection? by lazy {
        if (isDestroyed.get()) return@lazy null
        try {
            val config = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            }
            peerConnectionFactory.createPeerConnection(config, observer)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Failed to create peer connection", e)
            null
        }
    }

    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null

    var localVideoTrack: VideoTrack? = null
        private set
    var localAudioTrack: AudioTrack? = null
        private set
    var remoteVideoTrack: VideoTrack? = null
        private set

    private val localSinks = java.util.Collections.synchronizedSet(mutableSetOf<VideoSink>())
    private val remoteSinks = java.util.Collections.synchronizedSet(mutableSetOf<VideoSink>())

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    fun startLocalAudio() {
        if (localAudioTrack != null) return
        try {
            localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
            val audioTrack = peerConnectionFactory.createAudioTrack("audio_track", localAudioSource)
            localAudioTrack = audioTrack
            peerConnection?.addTrack(audioTrack, listOf("ARDAMS"))
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error starting local audio", e)
        }
    }

    fun startLocalVideoCapture(surfaceViewRenderer: SurfaceViewRenderer? = null): Boolean {
        return try {
            startLocalAudio()

            if (localVideoTrack == null && !isDestroyed.get()) {
                val enumerator = Camera2Enumerator(context)
                var selectedIsFront = true
                val capturer = createCameraCapturer(enumerator) { isFront ->
                    selectedIsFront = isFront
                } ?: run {
                    Log.w("WebRTCClient", "No camera capturer available")
                    return false
                }

                val textureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
                surfaceTextureHelper = textureHelper
                localVideoSource = peerConnectionFactory.createVideoSource(false)
                val videoTrack = peerConnectionFactory.createVideoTrack("video_track", localVideoSource)

                capturer.initialize(textureHelper, context, localVideoSource?.capturerObserver)
                capturer.startCapture(1280, 720, 30)

                videoCapturer = capturer
                localVideoTrack = videoTrack
                _isFrontCamera.value = selectedIsFront
                peerConnection?.addTrack(videoTrack, listOf("ARDAMS"))

                // Attach any pending local sinks
                synchronized(localSinks) {
                    for (sink in localSinks) {
                        try { videoTrack.addSink(sink) } catch (_: Throwable) {}
                    }
                }
            }

            surfaceViewRenderer?.let {
                attachLocalSink(it)
            }
            localVideoTrack != null
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error starting local video capture", e)
            false
        }
    }

    fun attachLocalSink(sink: VideoSink) {
        try {
            if (localSinks.add(sink)) {
                localVideoTrack?.addSink(sink)
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error attaching local sink", e)
        }
    }

    fun detachLocalSink(sink: VideoSink) {
        try {
            if (localSinks.remove(sink)) {
                localVideoTrack?.removeSink(sink)
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error detaching local sink", e)
        }
    }

    fun attachRemoteSink(sink: VideoSink) {
        try {
            if (remoteSinks.add(sink)) {
                remoteVideoTrack?.addSink(sink)
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error attaching remote sink", e)
        }
    }

    fun detachRemoteSink(sink: VideoSink) {
        try {
            if (remoteSinks.remove(sink)) {
                remoteVideoTrack?.removeSink(sink)
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error detaching remote sink", e)
        }
    }

    fun setRemoteVideoTrack(track: VideoTrack) {
        remoteVideoTrack = track
        for (sink in remoteSinks) {
            try {
                track.addSink(sink)
            } catch (e: Throwable) {
                Log.e("WebRTCClient", "Error adding pending remote sink", e)
            }
        }
    }

    fun createOffer(isVideo: Boolean = true, onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        }
        try {
            val pc = peerConnection ?: run {
                onError("PeerConnection unavailable")
                return
            }
            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    if (desc != null) {
                        onSuccess(desc)
                    } else {
                        onError("Created null offer")
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) { onError(error ?: "Unknown offer error") }
                override fun onSetFailure(error: String?) { onError(error ?: "Unknown set error") }
            }, constraints)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error creating offer", e)
            onError(e.message ?: "Exception creating offer")
        }
    }

    fun createAnswer(isVideo: Boolean = true, onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        }
        try {
            val pc = peerConnection ?: run {
                onError("PeerConnection unavailable")
                return
            }
            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    if (desc != null) {
                        onSuccess(desc)
                    } else {
                        onError("Created null answer")
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) { onError(error ?: "Unknown answer error") }
                override fun onSetFailure(error: String?) { onError(error ?: "Unknown set error") }
            }, constraints)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error creating answer", e)
            onError(e.message ?: "Exception creating answer")
        }
    }

    fun setLocalDescription(
        desc: SessionDescription,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        try {
            val pc = peerConnection ?: run {
                onError("PeerConnection unavailable")
                return
            }
            pc.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() { onComplete() }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {
                    val msg = p0 ?: "Failed to set local description"
                    Log.e("WebRTCClient", "Failed to set local description: $msg")
                    onError(msg)
                }
            }, desc)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting local description", e)
            onError(e.message ?: "Exception setting local description")
        }
    }

    fun setRemoteDescription(
        desc: SessionDescription,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        try {
            val pc = peerConnection ?: run {
                onError("PeerConnection unavailable")
                return
            }
            pc.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() { onComplete() }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {
                    val msg = p0 ?: "Failed to set remote description"
                    Log.e("WebRTCClient", "Failed to set remote description: $msg")
                    onError(msg)
                }
            }, desc)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting remote description", e)
            onError(e.message ?: "Exception setting remote description")
        }
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        try {
            peerConnection?.addIceCandidate(iceCandidate)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error adding ice candidate", e)
        }
    }

    private fun createCameraCapturer(
        enumerator: CameraEnumerator,
        onSelectedFacing: (Boolean) -> Unit = {}
    ): VideoCapturer? {
        try {
            val deviceNames = enumerator.deviceNames
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        onSelectedFacing(true)
                        return capturer
                    }
                }
            }
            for (deviceName in deviceNames) {
                if (enumerator.isBackFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        onSelectedFacing(false)
                        return capturer
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error creating camera capturer", e)
        }
        return null
    }

    fun switchCamera(
        onSwitched: ((Boolean) -> Unit)? = null,
        handler: CameraVideoCapturer.CameraSwitchHandler? = null
    ) {
        try {
            (videoCapturer as? CameraVideoCapturer)?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    _isFrontCamera.value = isFrontCamera
                    onSwitched?.invoke(isFrontCamera)
                    handler?.onCameraSwitchDone(isFrontCamera)
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Log.e("WebRTCClient", "Switch camera error: $errorDescription")
                    handler?.onCameraSwitchError(errorDescription)
                }
            })
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error switching camera", e)
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        try {
            localAudioTrack?.setEnabled(enabled)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting audio enabled", e)
        }
    }

    fun setVideoEnabled(enabled: Boolean) {
        try {
            localVideoTrack?.setEnabled(enabled)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting video enabled", e)
        }
    }

    fun destroy() {
        if (!isDestroyed.compareAndSet(false, true)) return
        synchronized(localSinks) {
            for (s in localSinks) {
                try { localVideoTrack?.removeSink(s) } catch (_: Throwable) {}
            }
            localSinks.clear()
        }
        synchronized(remoteSinks) {
            for (s in remoteSinks) {
                try { remoteVideoTrack?.removeSink(s) } catch (_: Throwable) {}
            }
            remoteSinks.clear()
        }
        try { videoCapturer?.stopCapture() } catch (_: Throwable) {}
        try { videoCapturer?.dispose() } catch (_: Throwable) {}
        try { surfaceTextureHelper?.dispose() } catch (_: Throwable) {}
        surfaceTextureHelper = null
        try { localVideoTrack?.dispose() } catch (_: Throwable) {}
        try { localAudioTrack?.dispose() } catch (_: Throwable) {}
        try { localVideoSource?.dispose() } catch (_: Throwable) {}
        try { localAudioSource?.dispose() } catch (_: Throwable) {}
        try { peerConnection?.close(); peerConnection?.dispose() } catch (_: Throwable) {}
        try { peerConnectionFactory.dispose() } catch (_: Throwable) {}
        if (eglCreated) {
            try { rootEglBase.release() } catch (_: Throwable) {}
        }
    }
}
