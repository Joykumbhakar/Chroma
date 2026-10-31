package com.chroma.studio.webrtc

import android.content.Context
import org.webrtc.*

class WebRTCClient(
    context: Context,
    private val observer: PeerConnection.Observer
) {
    private val context = context.applicationContext
    private val rootEglBase: EglBase = EglBase.create()
    val eglBaseContext: EglBase.Context = rootEglBase.eglBaseContext

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
            .let { PeerConnectionFactory.initialize(it) }

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(
            rootEglBase.eglBaseContext
        )

        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private val peerConnection: PeerConnection? by lazy {
        val config = PeerConnection.RTCConfiguration(iceServers)
        peerConnectionFactory.createPeerConnection(config, observer)
    }

    private val localVideoSource: VideoSource by lazy {
        peerConnectionFactory.createVideoSource(false)
    }
    private val localAudioSource: AudioSource by lazy {
        peerConnectionFactory.createAudioSource(MediaConstraints())
    }

    private var videoCapturer: VideoCapturer? = null
    var localVideoTrack: VideoTrack? = null
        private set
    private var localAudioTrack: AudioTrack? = null

    init {
        localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", localAudioSource)
    }

    fun startLocalVideoCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        videoCapturer = createCameraCapturer(Camera2Enumerator(context))
        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack?.addSink(surfaceViewRenderer)

        // Add stream to connection
        val mediaStream = peerConnectionFactory.createLocalMediaStream("media_stream")
        mediaStream.addTrack(localVideoTrack)
        mediaStream.addTrack(localAudioTrack)
        peerConnection?.addStream(mediaStream)
    }

    fun call(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(sdpObserver, constraints)
    }

    fun answer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(sdpObserver, constraints)
    }

    fun setLocalDescription(observer: SdpObserver, sessionDescription: SessionDescription) {
        peerConnection?.setLocalDescription(observer, sessionDescription)
    }

    fun setRemoteDescription(observer: SdpObserver, sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(observer, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun switchCamera(handler: CameraVideoCapturer.CameraSwitchHandler? = null) {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(handler)
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun destroy() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            peerConnection?.close()
            peerConnectionFactory.dispose()
            rootEglBase.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
