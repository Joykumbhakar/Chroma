package com.chroma.studio.chat.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object SpeechRecognitionManager {
    private const val TAG = "SpeechRecognitionMgr"

    private var speechRecognizer: SpeechRecognizer? = null
    private var resultCallback: ((String) -> Unit)? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscribedText = MutableStateFlow("")
    val liveTranscribedText: StateFlow<String> = _liveTranscribedText.asStateFlow()

    private val _speechRms = MutableStateFlow(0f)
    val speechRms: StateFlow<Float> = _speechRms.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        context: Context,
        onLivePartial: (String) -> Unit = {},
        onFinalResult: (String) -> Unit
    ) {
        if (!isAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device."
            return
        }

        stopListening()
        resultCallback = onFinalResult
        _errorMessage.value = null
        _liveTranscribedText.value = ""
        _speechRms.value = 0f

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize RMS dB typically ranging from -2dB to 10dB
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    _speechRms.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _speechRms.value = 0f
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                        else -> "Speech recognition error ($error)"
                    }
                    Log.w(TAG, "Speech recognition error: $message")
                    _errorMessage.value = message
                    _isListening.value = false
                    _speechRms.value = 0f
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _speechRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: _liveTranscribedText.value
                    if (text.isNotBlank()) {
                        _liveTranscribedText.value = text
                        resultCallback?.invoke(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        _liveTranscribedText.value = text
                        onLivePartial(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _errorMessage.value = "Failed to start speech recognition: ${e.message}"
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.apply {
                stopListening()
                destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _speechRms.value = 0f
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.apply {
                cancel()
                destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _speechRms.value = 0f
            _liveTranscribedText.value = ""
        }
    }
}

