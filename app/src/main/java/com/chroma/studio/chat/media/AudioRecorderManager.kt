package com.chroma.studio.chat.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

data class RecordingResult(
    val file: File,
    val durationSeconds: Int,
    val waveformSamples: List<Float>
)

object AudioRecorderManager {
    private const val TAG = "AudioRecorderManager"

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var recordingScope: CoroutineScope? = null
    private var startTimeMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _waveformSamples = MutableStateFlow<List<Float>>(emptyList())
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    fun startRecording(context: Context): Boolean {
        if (_isRecording.value) {
            cancelRecording()
        }

        try {
            val outputDir = File(context.cacheDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(32000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            _isPaused.value = false
            _durationSec.value = 0
            _currentAmplitude.value = 0f
            _waveformSamples.value = emptyList()

            startAmplitudePolling()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanup()
            return false
        }
    }

    private fun startAmplitudePolling() {
        recordingScope?.cancel()
        recordingScope = CoroutineScope(Dispatchers.Default + Job())
        recordingScope?.launch {
            val samples = mutableListOf<Float>()
            while (_isRecording.value) {
                if (!_isPaused.value) {
                    val rawAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    // Apply perceptual speech expansion curve (0.15f to 1.0f)
                    val ampRatio = (rawAmp.toFloat() / 14000f).coerceIn(0f, 1f)
                    val normalized = java.lang.Math.pow(ampRatio.toDouble(), 0.55).toFloat().coerceIn(0.18f, 1f)
                    _currentAmplitude.value = normalized
                    samples.add(normalized)
                    _waveformSamples.value = samples.toList()

                    val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    _durationSec.value = elapsedSec
                }
                delay(65)
            }
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null

        val finalDuration = _durationSec.value.coerceAtLeast(1)
        val finalSamples = _waveformSamples.value.ifEmpty { List(25) { 0.35f } }
        val finalFile = currentFile

        cleanup()

        return if (finalFile != null && finalFile.exists() && finalFile.length() > 0) {
            RecordingResult(
                file = finalFile,
                durationSeconds = finalDuration,
                waveformSamples = finalSamples
            )
        } else {
            null
        }
    }

    fun cancelRecording() {
        currentFile?.delete()
        cleanup()
    }

    private fun cleanup() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            recordingScope?.cancel()
            recordingScope = null
            _isRecording.value = false
            _isPaused.value = false
            _durationSec.value = 0
            _currentAmplitude.value = 0f
            _waveformSamples.value = emptyList()
        }
    }
}

