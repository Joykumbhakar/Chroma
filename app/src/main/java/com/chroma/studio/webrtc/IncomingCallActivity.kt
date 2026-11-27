package com.chroma.studio.webrtc

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.chroma.studio.data.SupabaseChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon
import java.lang.ref.WeakReference

/**
 * Full-screen Apple Incoming & Active Call Activity used when:
 * - Device is locked / screen was off and an incoming call wakes the phone
 * - User taps the incoming call heads-up notification (or its Answer button) while in another app
 */
class IncomingCallActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var currentRef: WeakReference<IncomingCallActivity>? = null

        @Volatile
        private var currentCallId: String? = null

        fun dismissIfShowing(callId: String) {
            if (callId.isBlank()) return
            val act = currentRef?.get() ?: return
            if (currentCallId == callId && !act.isFinishing && !act.isDestroyed) {
                act.runOnUiThread {
                    if (!act.isFinishing && !act.isDestroyed) {
                        act.finish()
                    }
                }
            }
        }
    }

    private var activeCallState by mutableStateOf<CallModel?>(null)
    private var callerHandleState by mutableStateOf("")
    private var callerVerifiedState by mutableStateOf(false)
    private var autoAnswerState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenWindow()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        readIntentExtras(intent)
        val initialCall = activeCallState
        if (initialCall == null) {
            finish()
            return
        }

        currentRef = WeakReference(this)
        currentCallId = initialCall.id

        val callManager = CallManager.getInstance(applicationContext)
        callManager.presentIncomingCallFromPush(initialCall)

        setContent {
            val call = activeCallState
            if (call == null) {
                LaunchedEffect(Unit) { finish() }
                return@setContent
            }

            val rtcCallState by callManager.callState.collectAsState()
            var isAnswered by remember(call.id) {
                mutableStateOf(
                    callManager.callState.value == CallState.ACTIVE &&
                        callManager.activeCall.value?.id == call.id
                )
            }
            var waitingPermissionToAnswer by remember { mutableStateOf(false) }

            fun completeAnswer() {
                if (isAnswered) return
                isAnswered = true
                CallRingtoneController.stop(call.id)
                IncomingCallNotificationManager.cancelIncomingCallNotification(applicationContext, call.id)
                dismissKeyguardIfAppropriate()
                callManager.answerCall(call)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val audioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
                val cameraGranted = !call.isVideo || perms[Manifest.permission.CAMERA] == true
                if (audioGranted && cameraGranted) {
                    completeAnswer()
                } else {
                    waitingPermissionToAnswer = false
                }
            }

            fun requestAndAnswer() {
                val hasAudio = ContextCompat.checkSelfPermission(
                    this@IncomingCallActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                val hasCamera = !call.isVideo || ContextCompat.checkSelfPermission(
                    this@IncomingCallActivity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasAudio && hasCamera) {
                    completeAnswer()
                } else {
                    waitingPermissionToAnswer = true
                    val needed = if (call.isVideo) {
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    } else {
                        arrayOf(Manifest.permission.RECORD_AUDIO)
                    }
                    permissionLauncher.launch(needed)
                }
            }

            LaunchedEffect(call.id, autoAnswerState) {
                if (autoAnswerState && !isAnswered && !waitingPermissionToAnswer) {
                    requestAndAnswer()
                } else if (!isAnswered) {
                    CallRingtoneController.start(applicationContext, call.id)
                }
            }

            // Watch remote Firestore signaling doc so if caller cancels while ringing, activity closes immediately
            LaunchedEffect(call.id) {
                callManager.signaling.listenToCall(call.id).collect { remote ->
                    if (remote != null && remote.status in listOf("ended", "rejected", "busy", "missed", "error")) {
                        CallRingtoneController.stop(call.id)
                        IncomingCallNotificationManager.cancelIncomingCallNotification(applicationContext, call.id)
                        delay(250)
                        finish()
                    }
                }
            }

            LaunchedEffect(rtcCallState) {
                if (rtcCallState == CallState.ENDED) {
                    CallRingtoneController.stop(call.id)
                    delay(450)
                    finish()
                }
            }

            DisposableEffect(call.id) {
                onDispose {
                    CallRingtoneController.stop(call.id)
                }
            }

            BackHandler(enabled = true) {
                // Keep call screen protected while ringing/active unless declined/ended
            }

            AppleIncomingCallScreen(
                modifier = Modifier.fillMaxSize(),
                callerName = call.callerName.ifBlank { "Oscellion User" },
                callerSubtitle = if (call.isVideo) "FaceTime Video" else "Oscellion Audio",
                callerPhoneNumber = callerHandleState,
                profileImageUrl = call.callerAvatar,
                isVerified = callerVerifiedState,
                initialStatus = if (isAnswered) CallStatus.ACTIVE else CallStatus.INCOMING,
                initialVideoOn = call.isVideo,
                callManager = callManager,
                canAnswerWithoutPrompt = {
                    val hasAudio = ContextCompat.checkSelfPermission(
                        this@IncomingCallActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCamera = !call.isVideo || ContextCompat.checkSelfPermission(
                        this@IncomingCallActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    hasAudio && hasCamera
                },
                onCallAccepted = { requestAndAnswer() },
                onCallEnded = { reason ->
                    CallRingtoneController.stop(call.id)
                    IncomingCallNotificationManager.cancelIncomingCallNotification(applicationContext, call.id)
                    if (!isAnswered && callManager.callState.value == CallState.RINGING) {
                        callManager.rejectCall(call)
                    } else {
                        callManager.endCall(reason)
                    }
                    finish()
                },
                onQuickMessageReply = { replyText ->
                    val roomId = call.roomId
                    if (roomId.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                SupabaseChatRepository().sendMessage(roomId, replyText)
                            } catch (_: Throwable) {}
                        }
                    }
                },
                localCameraContent = { _, isPip ->
                    WebRTCVideoView(
                        callManager = callManager,
                        isLocal = true,
                        isPipOverlay = isPip,
                        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                remoteVideoContent = { isPip ->
                    WebRTCVideoView(
                        callManager = callManager,
                        isLocal = false,
                        isPipOverlay = isPip,
                        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntentExtras(intent)
    }

    override fun onDestroy() {
        val id = activeCallState?.id
        CallRingtoneController.stop(id)
        if (currentRef?.get() == this) {
            currentRef = null
            currentCallId = null
        }
        super.onDestroy()
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun dismissKeyguardIfAppropriate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
            if (km.isKeyguardLocked) {
                try {
                    km.requestDismissKeyguard(this, null)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun readIntentExtras(intent: Intent?) {
        if (intent == null) return
        val callId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALL_ID).orEmpty()
        if (callId.isBlank()) return

        activeCallState = CallModel(
            id = callId,
            roomId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_ROOM_ID).orEmpty(),
            callerId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLER_ID).orEmpty(),
            callerName = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLER_NAME).orEmpty(),
            callerAvatar = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLER_AVATAR).orEmpty(),
            calleeId = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLEE_ID).orEmpty(),
            type = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALL_TYPE) ?: "audio",
            status = "ringing",
            timestamp = intent.getLongExtra(
                IncomingCallNotificationManager.EXTRA_CALL_TIMESTAMP,
                System.currentTimeMillis()
            )
        )
        callerHandleState = intent.getStringExtra(IncomingCallNotificationManager.EXTRA_CALLER_HANDLE).orEmpty()
        callerVerifiedState = intent.getBooleanExtra(IncomingCallNotificationManager.EXTRA_CALLER_VERIFIED, false)
        autoAnswerState = intent.getBooleanExtra(IncomingCallNotificationManager.EXTRA_AUTO_ANSWER, false)
    }
}
