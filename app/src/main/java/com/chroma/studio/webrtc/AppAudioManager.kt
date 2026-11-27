package com.chroma.studio.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Pure audio route selector prioritizing Speaker when requested, or Bluetooth / Wired headset
 * before falling back to the built-in earpiece.
 */
fun selectCommunicationAudioDeviceType(
    availableTypes: List<Int>,
    preferSpeaker: Boolean
): Int? {
    if (preferSpeaker) {
        return availableTypes.firstOrNull { it == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }
    val preferredNonSpeakerOrder = listOf(
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
    )
    for (type in preferredNonSpeakerOrder) {
        if (availableTypes.contains(type)) return type
    }
    return null
}

class AppAudioManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneOn: Boolean = false
    private var previousMicrophoneMute: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null

    fun start(isSpeakerDefault: Boolean = false) {
        try {
            previousAudioMode = audioManager.mode
            @Suppress("DEPRECATION")
            previousSpeakerphoneOn = audioManager.isSpeakerphoneOn
            previousMicrophoneMute = audioManager.isMicrophoneMute

            requestAudioFocus()

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = false
            setSpeakerphone(isSpeakerDefault)
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error starting audio manager", e)
        }
    }

    /**
     * Queries real available communication output routes on the device (Earpiece, Speaker,
     * plus connected Bluetooth / USB / Wired headsets).
     */
    fun getAvailableOutputDevices(): List<AudioOutputDevice> {
        val result = mutableListOf<AudioOutputDevice>()
        val seenNames = HashSet<String>()

        try {
            val rawDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.availableCommunicationDevices
            } else {
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            }

            rawDevices.forEach { info ->
                when (info.type) {
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        val name = info.productName?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "Bluetooth"
                        if (seenNames.add("bt:$name")) {
                            result.add(AudioOutputDevice(name = name, type = AudioDeviceType.BLUETOOTH, deviceId = info.id))
                        }
                    }
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        val name = info.productName?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "Wired Headset"
                        if (seenNames.add("wired:$name")) {
                            result.add(AudioOutputDevice(name = name, type = AudioDeviceType.BLUETOOTH, deviceId = info.id))
                        }
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                        if (seenNames.add("earpiece")) {
                            result.add(AudioOutputDevice(name = "Phone", type = AudioDeviceType.IPHONE, deviceId = info.id))
                        }
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        if (seenNames.add("speaker")) {
                            result.add(AudioOutputDevice(name = "Speaker", type = AudioDeviceType.SPEAKER, deviceId = info.id))
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w("AppAudioManager", "Could not enumerate communication audio devices: ${e.message}")
        }

        if (seenNames.add("earpiece") && result.none { it.type == AudioDeviceType.IPHONE }) {
            result.add(0, AudioOutputDevice("Phone", AudioDeviceType.IPHONE))
        }
        if (result.none { it.type == AudioDeviceType.SPEAKER }) {
            result.add(AudioOutputDevice("Speaker", AudioDeviceType.SPEAKER))
        }
        return result.sortedBy {
            when (it.type) {
                AudioDeviceType.IPHONE -> 0
                AudioDeviceType.SPEAKER -> 1
                AudioDeviceType.BLUETOOTH -> 2
            }
        }
    }

    fun selectOutputDevice(device: AudioOutputDevice) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val exactMatch = device.deviceId?.let { id -> devices.firstOrNull { it.id == id } }
                val fallbackMatch = exactMatch ?: when (device.type) {
                    AudioDeviceType.SPEAKER -> devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    AudioDeviceType.IPHONE -> devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    AudioDeviceType.BLUETOOTH -> devices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    }
                }
                if (fallbackMatch != null) {
                    audioManager.setCommunicationDevice(fallbackMatch)
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = (fallbackMatch.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                    return
                }
            }
            @Suppress("DEPRECATION")
            when (device.type) {
                AudioDeviceType.SPEAKER -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = true
                }
                AudioDeviceType.IPHONE -> {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                    audioManager.isSpeakerphoneOn = false
                }
                AudioDeviceType.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
            }
        } catch (e: Throwable) {
            Log.e("AppAudioManager", "Error selecting audio device ${device.name}", e)
        }
    }

    fun setSpeakerphone(on: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val targetType = selectCommunicationAudioDeviceType(
                    availableTypes = devices.map { it.type },
                    preferSpeaker = on
                )
                val targetDevice = targetType?.let { wanted -> devices.firstOrNull { it.type == wanted } }
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                }
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        } catch (e: Exception) {
            Log.e("AppAudioManager", "Error setting speakerphone", e)
        }
    }

    fun isSpeakerphoneOn(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val active = audioManager.communicationDevice
            if (active != null) {
                return active.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        }
        @Suppress("DEPRECATION")
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try { audioManager.clearCommunicationDevice() } catch (_: Exception) {}
            } else {
                @Suppress("DEPRECATION")
                try {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                } catch (_: Exception) {}
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = previousSpeakerphoneOn
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
                .setOnAudioFocusChangeListener { }
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
