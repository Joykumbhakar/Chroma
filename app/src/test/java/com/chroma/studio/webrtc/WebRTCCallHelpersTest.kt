package com.chroma.studio.webrtc

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebRTCCallHelpersTest {

    @Test
    fun `formatCallDuration formats seconds minutes and hours`() {
        assertEquals("0:00", formatCallDuration(-4))
        assertEquals("0:09", formatCallDuration(9))
        assertEquals("1:05", formatCallDuration(65))
        assertEquals("1:02:03", formatCallDuration(3723))
    }

    @Test
    fun `formatCallStatusText reflects dialing connecting reconnecting and live duration`() {
        assertEquals("Calling...", formatCallStatusText(CallState.CALLING, CallConnectionQuality.CONNECTING, 0, isCaller = true))
        assertEquals("Connecting...", formatCallStatusText(CallState.CALLING, CallConnectionQuality.CONNECTING, 0, isCaller = false))
        assertEquals("Incoming call", formatCallStatusText(CallState.RINGING, CallConnectionQuality.CONNECTING, 0, isCaller = false))
        assertEquals("Connecting...", formatCallStatusText(CallState.ACTIVE, CallConnectionQuality.CONNECTING, 0, isCaller = false))
        assertEquals("0:12", formatCallStatusText(CallState.ACTIVE, CallConnectionQuality.GOOD, 12, isCaller = true))
        assertEquals("Reconnecting...", formatCallStatusText(CallState.ACTIVE, CallConnectionQuality.RECONNECTING, 12, isCaller = true))
        assertEquals("Call ended", formatCallStatusText(CallState.ENDED, CallConnectionQuality.DISCONNECTED, 12, isCaller = true))
    }

    @Test
    fun `resolveRemotePeerInfo picks opposite participant and falls back cleanly`() {
        val call = CallModel(
            id = "c1",
            callerId = "u_alice",
            callerName = "Alice",
            callerAvatar = "alice.jpg",
            calleeId = "u_bob",
            calleeName = "Bob",
            calleeAvatar = "bob.jpg"
        )

        assertEquals("Bob" to "bob.jpg", resolveRemotePeerInfo(call, "ChatName", "chat.jpg", "u_alice"))
        assertEquals("Alice" to "alice.jpg", resolveRemotePeerInfo(call, "ChatName", "chat.jpg", "u_bob"))
        assertEquals("ChatName" to "chat.jpg", resolveRemotePeerInfo(null, "ChatName", "chat.jpg", "u_alice"))
        assertEquals("Oscellion User" to "", resolveRemotePeerInfo(null, "", "", "u_alice"))
    }

    @Test
    fun `selectCommunicationAudioDeviceType prioritizes speaker or headsets over earpiece`() {
        val allDevices = listOf(
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        )

        assertEquals(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, selectCommunicationAudioDeviceType(allDevices, preferSpeaker = true))
        assertEquals(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, selectCommunicationAudioDeviceType(allDevices, preferSpeaker = false))
        assertEquals(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            selectCommunicationAudioDeviceType(
                listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, AudioDeviceInfo.TYPE_WIRED_HEADSET),
                preferSpeaker = false
            )
        )
        assertEquals(
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            selectCommunicationAudioDeviceType(listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE), preferSpeaker = false)
        )
        assertNull(selectCommunicationAudioDeviceType(emptyList(), preferSpeaker = true))
    }

    @Test
    fun `resolveIncomingCallPresentation enforces full screen vs popup rules across all 4 states`() {
        // 1. App backgrounded & screen not in use (off or locked) -> FULL_SCREEN
        assertEquals(
            CallPresentationMode.FULL_SCREEN,
            resolveIncomingCallPresentation(
                isAppForeground = false,
                isScreenInteractive = false,
                isDeviceLocked = true,
                isOnChatScreen = false
            )
        )
        assertEquals(
            CallPresentationMode.FULL_SCREEN,
            resolveIncomingCallPresentation(
                isAppForeground = false,
                isScreenInteractive = true,
                isDeviceLocked = true,
                isOnChatScreen = false
            )
        )

        // 2. App backgrounded & screen in use (unlocked, user in another app) -> POPUP_NOTIFICATION
        assertEquals(
            CallPresentationMode.POPUP_NOTIFICATION,
            resolveIncomingCallPresentation(
                isAppForeground = false,
                isScreenInteractive = true,
                isDeviceLocked = false,
                isOnChatScreen = false
            )
        )

        // 3. App in foreground & user is on a Chat -> FULL_SCREEN
        assertEquals(
            CallPresentationMode.FULL_SCREEN,
            resolveIncomingCallPresentation(
                isAppForeground = true,
                isScreenInteractive = true,
                isDeviceLocked = false,
                isOnChatScreen = true
            )
        )

        // 4. App in foreground & user is on other screens/tabs -> POPUP_NOTIFICATION
        assertEquals(
            CallPresentationMode.POPUP_NOTIFICATION,
            resolveIncomingCallPresentation(
                isAppForeground = true,
                isScreenInteractive = true,
                isDeviceLocked = false,
                isOnChatScreen = false
            )
        )
    }
}
