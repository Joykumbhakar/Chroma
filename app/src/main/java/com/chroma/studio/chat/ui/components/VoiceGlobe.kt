package com.chroma.studio.chat.ui.components

import com.chroma.studio.chat.data.VoiceGlobeStyleType


import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors

/**
 * Android JavaScript Bridge to communicate between HTML VoiceGlobe and Kotlin.
 */
class VoiceGlobeBridge(
    private val onCancelCallback: () -> Unit,
    private val onTranscribeCallback: () -> Unit,
    private val onSendCallback: (Int) -> Unit
) {
    @JavascriptInterface
    fun onCancel() {
        onCancelCallback()
    }

    @JavascriptInterface
    fun onTranscribe() {
        onTranscribeCallback()
    }

    @JavascriptInterface
    fun onSend(durationSec: Int) {
        onSendCallback(durationSec)
    }
}

/**
 * Exact 1:1 replica of VoiceGlobe.html running the Chromium web engine with hardware acceleration.
 *
 * It renders the exact CSS3 liquid plasma orb, glass reflection lens, expanding radar waves,
 * shimmer title, tabular timer badge, and Phosphor icons.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VoiceRecorderCard(
    recordingDurationSec: Int,
    statusText: String = "Listening...",
    globeStyle: VoiceGlobeStyleType = VoiceGlobeStyleType.ORIGINAL_BLUE,
    onCancel: () -> Unit,
    onTranscribe: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val isDark = !colors.isLight
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Update status text in web view if it changes
    LaunchedEffect(statusText) {
        webViewRef?.evaluateJavascript("if (typeof setStatus === 'function') setStatus('$statusText');", null)
    }

    // Update theme if it changes
    LaunchedEffect(isDark) {
        webViewRef?.evaluateJavascript("if (typeof setTheme === 'function') setTheme($isDark);", null)
    }

    // Update globe style variation if it changes
    LaunchedEffect(globeStyle) {
        webViewRef?.evaluateJavascript("if (typeof setGlobeStyle === 'function') setGlobeStyle('${globeStyle.id}');", null)
    }

    Box(
        modifier = modifier
            .width(320.dp)
            .height(330.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(0) // Transparent background
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    addJavascriptInterface(
                        VoiceGlobeBridge(
                            onCancelCallback = {
                                post {
                                    SoundManager.playReaction()
                                    onCancel()
                                }
                            },
                            onTranscribeCallback = {
                                post {
                                    SoundManager.playReaction()
                                    onTranscribe()
                                }
                            },
                            onSendCallback = {
                                post {
                                    SoundManager.playSend()
                                    onSend()
                                }
                            }
                        ),
                        "AndroidBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(
                                "setTheme($isDark); setGlobeStyle('${globeStyle.id}'); startTimer($recordingDurationSec);",
                                null
                            )
                        }
                    }

                    loadUrl("file:///android_asset/voice_globe.html")
                    webViewRef = this
                }
            },
            update = { view ->
                view.evaluateJavascript("setTheme($isDark); setGlobeStyle('${globeStyle.id}');", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Animated recording indicator dot with blinking glow (matching .voice-rec-dot).
 */
@Composable
fun VoiceRecDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "VoiceRecDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Box(
        modifier = modifier
            .size(7.dp)
            .scale(scale)
            .graphicsLayer(alpha = alpha)
            .background(color, CircleShape)
    )
}

/**
 * Legacy alias for BlinkingDot.
 */
@Composable
fun BlinkingDot(color: Color, modifier: Modifier = Modifier) {
    VoiceRecDot(color = color, modifier = modifier)
}




