package com.chroma.studio.chat.media

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private var playerScope: CoroutineScope? = null

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0)
    val totalDurationMs: StateFlow<Int> = _totalDurationMs.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun play(id: String, filePathOrUri: String, context: Context) {
        if (_currentPlayingId.value == id && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            val player = MediaPlayer()
            if (filePathOrUri.startsWith("http://") || filePathOrUri.startsWith("https://")) {
                player.setDataSource(filePathOrUri)
            } else if (filePathOrUri.startsWith("content://")) {
                player.setDataSource(context, Uri.parse(filePathOrUri))
            } else {
                val file = File(filePathOrUri)
                if (file.exists()) {
                    player.setDataSource(file.absolutePath)
                } else {
                    Log.w(TAG, "Audio file does not exist: $filePathOrUri")
                    stop()
                    return
                }
            }

            player.prepare()
            applySpeed(player, _playbackSpeed.value)
            player.start()

            mediaPlayer = player
            _currentPlayingId.value = id
            _isPlaying.value = true
            _totalDurationMs.value = player.duration.coerceAtLeast(1)
            _currentPositionMs.value = 0
            _playbackProgress.value = 0f

            player.setOnCompletionListener {
                stop()
            }

            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}", e)
            stop()
        }
    }



    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    fun seekTo(progress: Float) {
        val targetMs = (_totalDurationMs.value * progress.coerceIn(0f, 1f)).toInt()
        try {
            mediaPlayer?.seekTo(targetMs)
            _currentPositionMs.value = targetMs
            _playbackProgress.value = progress
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun cycleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        mediaPlayer?.let { applySpeed(it, nextSpeed) }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.e(TAG, "Error setting playback speed", e)
            }
        }
    }

    private fun startProgressTracker() {
        playerScope?.cancel()
        playerScope = CoroutineScope(Dispatchers.Default + Job())
        playerScope?.launch {
            while (_isPlaying.value) {
                val pos = try {
                    mediaPlayer?.currentPosition ?: 0
                } catch (e: Exception) {
                    0
                }
                val dur = _totalDurationMs.value.coerceAtLeast(1)
                _currentPositionMs.value = pos
                _playbackProgress.value = (pos.toFloat() / dur).coerceIn(0f, 1f)
                delay(40)
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
            playerScope?.cancel()
            playerScope = null
            _currentPlayingId.value = null
            _isPlaying.value = false
            _currentPositionMs.value = 0
            _playbackProgress.value = 0f
        }
    }
}

