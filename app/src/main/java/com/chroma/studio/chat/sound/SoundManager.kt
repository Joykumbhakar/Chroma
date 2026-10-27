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
    private var isPoolLoaded = false
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
