package com.chroma.studio.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(
    context: Context,
    private val observer: PeerConnection.Observer
) {
    private val context = context.applicationContext

    val rootEglBase: EglBase by lazy {
        EglBase.create()
    }
    val eglBaseContext: EglBase.Context
        get() = rootEglBase.eglBaseContext

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        try {
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
                .let { PeerConnectionFactory.initialize(it) }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "PeerConnectionFactory.initialize error", e)
        }

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
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    val peerConnection: PeerConnection? by lazy {
        try {
            val config = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
            peerConnectionFactory.createPeerConnection(config, observer)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Failed to create peer connection", e)
            null
        }
    }

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

    fun startLocalVideoCapture(surfaceViewRenderer: SurfaceViewRenderer? = null) {
        try {
            startLocalAudio()

            if (localVideoTrack == null) {
                localVideoSource = peerConnectionFactory.createVideoSource(false)
                val videoTrack = peerConnectionFactory.createVideoTrack("video_track", localVideoSource)
                localVideoTrack = videoTrack

                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
                videoCapturer = createCameraCapturer(Camera2Enumerator(context))
                videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                videoCapturer?.startCapture(1280, 720, 30)

                peerConnection?.addTrack(videoTrack, listOf("ARDAMS"))

                // Attach any pending local sinks
                for (sink in localSinks) {
                    try { videoTrack.addSink(sink) } catch (_: Throwable) {}
                }
            }

            surfaceViewRenderer?.let {
                attachLocalSink(it)
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error starting local video capture", e)
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

    fun createOffer(onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        try {
            peerConnection?.createOffer(object : SdpObserver {
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

    fun createAnswer(onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        try {
            peerConnection?.createAnswer(object : SdpObserver {
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

    fun setLocalDescription(desc: SessionDescription, onComplete: () -> Unit = {}) {
        try {
            peerConnection?.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() { onComplete() }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {
                    Log.e("WebRTCClient", "Failed to set local description: $p0")
                }
            }, desc)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting local description", e)
        }
    }

    fun setRemoteDescription(desc: SessionDescription, onComplete: () -> Unit = {}) {
        try {
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() { onComplete() }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {
                    Log.e("WebRTCClient", "Failed to set remote description: $p0")
                }
            }, desc)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error setting remote description", e)
        }
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        try {
            peerConnection?.addIceCandidate(iceCandidate)
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error adding ice candidate", e)
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        try {
            val deviceNames = enumerator.deviceNames
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) return capturer
                }
            }
            for (deviceName in deviceNames) {
                if (enumerator.isBackFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) return capturer
                }
            }
        } catch (e: Throwable) {
            Log.e("WebRTCClient", "Error creating camera capturer", e)
        }
        return null
    }

    fun switchCamera(handler: CameraVideoCapturer.CameraSwitchHandler? = null) {
        try {
            (videoCapturer as? CameraVideoCapturer)?.switchCamera(handler)
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
        try { videoCapturer?.stopCapture() } catch (_: Throwable) {}
        try { videoCapturer?.dispose() } catch (_: Throwable) {}
        try { localVideoTrack?.dispose() } catch (_: Throwable) {}
        try { localAudioTrack?.dispose() } catch (_: Throwable) {}
        try { localVideoSource?.dispose() } catch (_: Throwable) {}
        try { localAudioSource?.dispose() } catch (_: Throwable) {}
        try { peerConnection?.close(); peerConnection?.dispose() } catch (_: Throwable) {}
        try { peerConnectionFactory.dispose() } catch (_: Throwable) {}
        try { rootEglBase.release() } catch (_: Throwable) {}
    }
}
