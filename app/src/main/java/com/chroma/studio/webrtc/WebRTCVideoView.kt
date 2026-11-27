package com.chroma.studio.webrtc

import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun WebRTCVideoView(
    callManager: CallManager,
    isLocal: Boolean,
    modifier: Modifier = Modifier,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
) {
    val activeClient by callManager.webRTCClientFlow.collectAsState()
    val isFrontCamera by callManager.isFrontCamera.collectAsState()
    val eglContext = remember(activeClient) {
        try {
            activeClient?.eglBaseContext
        } catch (e: Throwable) {
            Log.e("WebRTCVideoView", "Error getting EGL context", e)
            null
        }
    }

    if (eglContext == null) return

    key(activeClient, isLocal) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    try {
                        init(eglContext, null)
                        setScalingType(scalingType)
                        setEnableHardwareScaler(true)
                        if (isLocal) {
                            setMirror(isFrontCamera)
                            setZOrderMediaOverlay(true)
                            callManager.attachLocalSurface(this)
                        } else {
                            setMirror(false)
                            setZOrderMediaOverlay(false)
                            callManager.attachRemoteSurface(this)
                        }
                    } catch (e: Throwable) {
                        Log.e("WebRTCVideoView", "Error initializing SurfaceViewRenderer", e)
                    }
                }
            },
            update = { renderer ->
                try {
                    renderer.setScalingType(scalingType)
                    if (isLocal) {
                        renderer.setMirror(isFrontCamera)
                    }
                } catch (_: Throwable) {}
            },
            onRelease = { renderer ->
                try {
                    if (isLocal) {
                        callManager.detachLocalSurface(renderer)
                    } else {
                        callManager.detachRemoteSurface(renderer)
                    }
                    renderer.release()
                } catch (_: Throwable) {}
            }
        )
    }
}
