package com.chroma.studio.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Audio Player Manager to handle playback of voice notes in chat bubbles.
 */
class ChatAudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    var currentPlayingUrl by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set
    var currentPositionMs by mutableIntStateOf(0)
        private set
    var durationMs by mutableIntStateOf(0)
        private set

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun togglePlay(url: String, fallbackDurationSec: Int? = null) {
        if (currentPlayingUrl == url && isPlaying) {
            pause()
        } else if (currentPlayingUrl == url && !isPlaying && mediaPlayer != null) {
            resume()
        } else {
            play(url, fallbackDurationSec)
        }
    }

    fun play(url: String, fallbackDurationSec: Int? = null) {
        stop()
        currentPlayingUrl = url
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    durationMs = if (mp.duration > 0) mp.duration else (fallbackDurationSec ?: 0) * 1000
                    mp.start()
                    this@ChatAudioPlayerManager.isPlaying = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    this@ChatAudioPlayerManager.isPlaying = false
                    progress = 0f
                    currentPositionMs = 0
                    stopProgressTracker()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("ChatAudioPlayer", "Playback error: what=$what, extra=$extra")
                    stop()
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("ChatAudioPlayer", "Failed to start media player", e)
                stop()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                stopProgressTracker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            isPlaying = true
            startProgressTracker()
        }
    }

    fun seekTo(fraction: Float) {
        mediaPlayer?.let {
            val total = if (durationMs > 0) durationMs else it.duration
            if (total > 0) {
                val targetMs = (total * fraction.coerceIn(0f, 1f)).toInt()
                it.seekTo(targetMs)
                currentPositionMs = targetMs
                progress = fraction.coerceIn(0f, 1f)
            }
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w("ChatAudioPlayer", "Error releasing player", e)
        }
        mediaPlayer = null
        currentPlayingUrl = null
        isPlaying = false
        progress = 0f
        currentPositionMs = 0
        durationMs = 0
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive && isPlaying) {
                mediaPlayer?.let { mp ->
                    try {
                        val current = mp.currentPosition
                        val total = if (durationMs > 0) durationMs else mp.duration
                        currentPositionMs = current
                        if (total > 0) {
                            progress = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}

/**
 * Audio Recorder Manager to handle voice note recording with live waveform amplitudes.
 */
class ChatAudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var tempAudioFile: File? = null
    private var startTimeMs: Long = 0L

    var isRecording by mutableStateOf(false)
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var amplitudeLevel by mutableFloatStateOf(0.1f)
        private set

    private var trackerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun startRecording(): Boolean {
        try {
            cancelRecording()
            val tempFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            tempAudioFile = tempFile

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            isRecording = true
            startTimeMs = System.currentTimeMillis()
            elapsedSeconds = 0

            trackerJob = scope.launch {
                while (isActive && isRecording) {
                    val duration = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    elapsedSeconds = duration
                    val maxAmp = try { recorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }
                    amplitudeLevel = (maxAmp / 32767f).coerceIn(0.08f, 1f)
                    delay(100)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("ChatAudioRecorder", "Failed to start audio recording", e)
            cancelRecording()
            return false
        }
    }

    fun stopAndGetAudio(): Pair<ByteArray, Int>? {
        if (!isRecording) return null
        trackerJob?.cancel()
        val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt().coerceAtLeast(1)
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("ChatAudioRecorder", "Error stopping recorder", e)
        }
        recorder = null
        isRecording = false
        elapsedSeconds = 0
        amplitudeLevel = 0.1f

        val file = tempAudioFile
        return if (file != null && file.exists() && file.length() > 0) {
            val bytes = file.readBytes()
            file.delete()
            tempAudioFile = null
            Pair(bytes, durationSec)
        } else {
            file?.delete()
            tempAudioFile = null
            null
        }
    }

    fun cancelRecording() {
        trackerJob?.cancel()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        }
        recorder = null
        isRecording = false
        elapsedSeconds = 0
        amplitudeLevel = 0.1f
        tempAudioFile?.delete()
        tempAudioFile = null
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }
}
