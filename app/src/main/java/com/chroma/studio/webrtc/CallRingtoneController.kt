package com.chroma.studio.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Coordinates continuous incoming call ringing + vibration without restarting or leaking
 * when transitioning between Dynamic Island pop-up, heads-up notification, and full-screen call UI.
 */
object CallRingtoneController {
    private const val TAG = "CallRingtoneController"

    @Volatile private var activeCallId: String? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var vibrator: Vibrator? = null

    @Synchronized
    fun start(context: Context, callId: String) {
        if (callId.isBlank()) return
        if (activeCallId == callId) return
        stop()

        activeCallId = callId
        val app = context.applicationContext

        val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (ringtoneUri != null) {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(app, ringtoneUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Unable to start MediaPlayer ringtone: ${e.message}")
            }
        }

        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            try {
                val vib = resolveVibrator(app)
                vibrator = vib
                val pattern = longArrayOf(0, 750, 950)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vib?.vibrate(pattern, 0)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Unable to start call vibration: ${e.message}")
            }
        }
    }

    @Synchronized
    fun stop(callId: String? = null) {
        if (callId != null && activeCallId != null && activeCallId != callId) {
            return
        }
        activeCallId = null
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
        } catch (_: Throwable) {
        } finally {
            mediaPlayer = null
        }
        try {
            vibrator?.cancel()
        } catch (_: Throwable) {
        } finally {
            vibrator = null
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
