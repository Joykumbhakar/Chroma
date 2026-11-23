package com.chroma.studio.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AppAudioManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneOn: Boolean = false
    private var previousMicrophoneMute: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null

    fun start(isSpeakerDefault: Boolean = false) {
        try {
            previousAudioMode = audioManager.mode
            previousSpeakerphoneOn = audioManager.isSpeakerphoneOn
            previousMicrophoneMute = audioManager.isMicrophoneMute

            // Request audio focus
            requestAudioFocus()

            // Set communication mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = false
            setSpeakerphone(isSpeakerDefault)
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error starting audio manager", e)
        }
    }

    fun setSpeakerphone(on: Boolean) {
        try {
            audioManager.isSpeakerphoneOn = on
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error setting speakerphone", e)
        }
    }

    fun isSpeakerphoneOn(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    fun setMicrophoneMute(mute: Boolean) {
        try {
            audioManager.isMicrophoneMute = mute
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error muting mic", e)
        }
    }

    fun stop() {
        try {
            setSpeakerphone(previousSpeakerphoneOn)
            audioManager.isMicrophoneMute = previousMicrophoneMute
            audioManager.mode = previousAudioMode
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error stopping audio manager", e)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { /* handle focus changes if needed */ }
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
