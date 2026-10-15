package com.chroma.studio.chat.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundsEnabled = true

    fun playSend() {
        if (!isSoundsEnabled) return
        playTone(frequency = 523.25, durationMs = 60, amplitude = 0.4f) // C5
    }

    fun playReceive() {
        if (!isSoundsEnabled) return
        scope.launch {
            playTone(frequency = 659.25, durationMs = 60, amplitude = 0.35f) // E5
            kotlinx.coroutines.delay(65)
            playTone(frequency = 783.99, durationMs = 80, amplitude = 0.35f) // G5
        }
    }

    fun playReaction() {
        if (!isSoundsEnabled) return
        playTone(frequency = 880.0, durationMs = 50, amplitude = 0.3f) // A5
    }

    fun playPop() {
        if (!isSoundsEnabled) return
        playTone(frequency = 1046.5, durationMs = 35, amplitude = 0.25f) // C6 quick pop
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

