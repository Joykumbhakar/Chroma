package com.chroma.studio.chat.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.chroma.studio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundsEnabled = true

    private var soundPool: SoundPool? = null
    private var scrubSoundId: Int = 0
    private var tapSoundId: Int = 0
    private var dynamicIslandSoundId: Int = 0
    private var messageSentSoundId: Int = 0
    private var messageSent2ndSoundId: Int = 0
    private var refreshedSoundId: Int = 0
    private var notificationSoundId: Int = 0
    private var isPoolLoaded = false
    private var previewPlayer: android.media.MediaPlayer? = null
    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    private var lastScrubTimestamp = 0L

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build()

            val appContext = context.applicationContext
            fun safeLoad(resId: Int): Int {
                return try {
                    pool.load(appContext, resId, 1)
                } catch (_: Throwable) {
                    0
                }
            }
            scrubSoundId = safeLoad(R.raw.scrub_frame_sound)
            tapSoundId = safeLoad(R.raw.tap_sound)
            dynamicIslandSoundId = safeLoad(R.raw.dynamic_island_notification)
            messageSentSoundId = safeLoad(R.raw.message_sent_default_audio_sound_effect)
            messageSent2ndSoundId = safeLoad(R.raw.message_sent_default_audio_sound_effect_2nd)
            refreshedSoundId = safeLoad(R.raw.refreshed_audio)
            notificationSoundId = safeLoad(R.raw.notification_sound_default)

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    isPoolLoaded = true
                    loadedSoundIds.add(sampleId)
                }
            }

            soundPool = pool
        } catch (_: Throwable) {
            // Gracefully fallback to synthesized audio
        }
    }

    fun playScrub(context: Context? = null, force: Boolean = false, pitchRate: Float = 1.0f) {
        if (!isSoundsEnabled) return
        val now = System.currentTimeMillis()
        if (!force && (now - lastScrubTimestamp < 28L)) return // Throttled to prevent audio stutter during rapid drag scrub
        lastScrubTimestamp = now

        if (soundPool == null && context != null) {
            init(context)
        }

        val pool = soundPool
        if (pool != null && scrubSoundId != 0 && (isPoolLoaded || loadedSoundIds.contains(scrubSoundId))) {
            pool.play(scrubSoundId, 0.65f, 0.65f, 1, 0, pitchRate.coerceIn(0.8f, 1.25f))
        } else {
            playTone(frequency = 720.0, durationMs = 20, amplitude = 0.25f)
        }
    }

    fun playTap(context: Context? = null) {
        // Disabled: Keep UI taps silent
    }

    fun playDynamicIsland(context: Context? = null) {
        if (!isSoundsEnabled) return
        if (soundPool == null && context != null) {
            init(context)
        }

        val pool = soundPool
        if (pool != null && dynamicIslandSoundId != 0 && isPoolLoaded) {
            pool.play(dynamicIslandSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
        } else {
            playRing()
        }
    }

    fun playSend(context: Context? = null) {
        if (!isSoundsEnabled) return
        if (soundPool == null && context != null) {
            init(context)
        }

        val pool = soundPool
        if (pool != null && messageSentSoundId != 0 && isPoolLoaded) {
            pool.play(messageSentSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
        } else {
            playTone(frequency = 523.25, durationMs = 60, amplitude = 0.4f) // C5
        }
    }

    fun playReceive(context: Context? = null) {
        if (!isSoundsEnabled) return
        if (soundPool == null && context != null) {
            init(context)
        }

        val pool = soundPool
        if (pool != null && messageSent2ndSoundId != 0 && isPoolLoaded) {
            pool.play(messageSent2ndSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
        } else {
            scope.launch {
                playTone(frequency = 659.25, durationMs = 60, amplitude = 0.35f) // E5
                kotlinx.coroutines.delay(65)
                playTone(frequency = 783.99, durationMs = 80, amplitude = 0.35f) // G5
            }
        }
    }

    /**
     * Plays the crisp refreshed audio cue when user pulls or taps to refresh the feed
     */
    fun playRefresh(context: Context? = null) {
        if (!isSoundsEnabled) return
        if (soundPool == null && context != null) {
            init(context)
        }

        val pool = soundPool
        if (pool != null && refreshedSoundId != 0 && (isPoolLoaded || loadedSoundIds.contains(refreshedSoundId))) {
            pool.play(refreshedSoundId, 0.85f, 0.85f, 1, 0, 1.0f)
        } else if (context != null) {
            try {
                val mp = android.media.MediaPlayer.create(context.applicationContext, R.raw.refreshed_audio)
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Plays an in-app notification chime using the user's selected tone
     */
    fun playNotification(context: Context? = null) {
        if (!isSoundsEnabled) return
        if (context == null) {
            val pool = soundPool
            if (pool != null && notificationSoundId != 0 && isPoolLoaded) {
                pool.play(notificationSoundId, 0.85f, 0.85f, 1, 0, 1.0f)
            }
            return
        }

        val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif_sounds", true)) return

        val toneId = prefs.getString("selected_notification_sound", "default")
        val tone = AVAILABLE_NOTIFICATION_TONES.find { it.id == toneId } ?: AVAILABLE_NOTIFICATION_TONES.first()
        playTonePreview(context, tone.resId)
    }

    /**
     * Previews a notification tone from the Notification Sound Pack
     */
    fun playTonePreview(context: Context, resId: Int) {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null

            val mp = android.media.MediaPlayer.create(context.applicationContext, resId)
            mp?.setOnCompletionListener {
                it.release()
                if (previewPlayer == it) previewPlayer = null
            }
            previewPlayer = mp
            mp?.start()
        } catch (_: Throwable) {}
    }

    fun playReaction() {
        // Disabled: Keep UI taps and reactions silent
    }

    fun playPop() {
        // Disabled: Keep UI pops silent
    }

    fun playRing() {
        if (!isSoundsEnabled) return
        playTone(frequency = 587.33, durationMs = 150, amplitude = 0.45f) // D5
    }

    private fun playTone(frequency: Double, durationMs: Int, amplitude: Float = 0.4f) {
        scope.launch {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 44100
                val numSamples = (durationMs * sampleRate) / 1000
                val generatedSnd = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                    // Apply linear attack and decay envelope to prevent popping
                    val envelope = when {
                        i < numSamples * 0.1 -> i / (numSamples * 0.1)
                        i > numSamples * 0.7 -> (numSamples - i) / (numSamples * 0.3)
                        else -> 1.0
                    }
                    val sample = (sin(angle) * Short.MAX_VALUE * amplitude * envelope).toInt().toShort()
                    generatedSnd[i] = sample
                }

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                // Release AudioTrack cleanly after playback completes to prevent native memory leaks
                kotlinx.coroutines.delay(durationMs + 60L)
            } catch (_: Throwable) {
                // Ignore audio errors gracefully
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Throwable) {}
            }
        }
    }
}


/**
 * Representation of an individual notification sound option from the Notification Sound Pack
 */
data class NotificationTone(
    val id: String,
    val name: String,
    val subtitle: String,
    val resId: Int
)

val AVAILABLE_NOTIFICATION_TONES = listOf(
    NotificationTone("default", "Galaxy Crystal", "Default crystal chime", R.raw.notification_sound_default),
    NotificationTone("chime_pop", "Chime Pop", "Subtle acoustic pop", R.raw.notification_sound2),
    NotificationTone("breeze", "Breeze", "Soft airy notification", R.raw.notification_sound1),
    NotificationTone("pulse", "Pulse", "Quick energetic alert", R.raw.notification_sound3),
    NotificationTone("cosmic", "Cosmic Drop", "Ambient glass tone", R.raw.notification_sound5),
    NotificationTone("bloom", "Harmonic Bloom", "Resonant melodic chime", R.raw.notification_sound6),
    NotificationTone("starlight", "Starlight", "High acoustic shimmer", R.raw.notification_sound7),
    NotificationTone("echo_wave", "Echo Wave", "Modern double wave alert", R.raw.notification_sound8),
    NotificationTone("velvet", "Velvet Note", "Smooth warm acoustic bell", R.raw.notification_sound9),
    NotificationTone("crystal_bell", "Crystal Bell", "Clear acoustic chime", R.raw.notification_sound),
    NotificationTone("celestial", "Celestial Harmony", "Extended harmonic chime", R.raw.notification_sound10),
    NotificationTone("flute", "Radha Krishna Flute", "Devotional serene flute", R.raw.radha_krishan_flute),
    NotificationTone("playful", "Playful Melody", "Upbeat whistle tune", R.raw.notification_sound_playful)
)
