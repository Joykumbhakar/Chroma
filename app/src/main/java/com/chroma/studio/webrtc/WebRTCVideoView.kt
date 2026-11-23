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
    val eglContext = remember(callManager) {
        try {
            callManager.webRTCClient?.eglBaseContext
        } catch (e: Throwable) {
            Log.e("WebRTCVideoView", "Error getting EGL context", e)
            null
        }
    }

    if (eglContext == null) return

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
                        setMirror(true)
                        setZOrderMediaOverlay(true)
                        callManager.attachLocalSurface(this)
                    } else {
                        setMirror(false)
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
