package com.chroma.studio.chat.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * WhatsApp-grade sender activity presence engine:
 * 1. Constructive Keystroke Detection: Distinguishes constructive character additions vs. non-constructive editing.
 * 2. 1,500ms Inter-Packet Throttling: Enforces [MIN_INTER_PACKET_INTERVAL_MS] during rapid typing.
 * 3. 2,500ms Paused Debounce: Emits null/paused when user stops typing for 2.5s.
 * 4. 3,500ms Sustained Refresh Ticker: Sustains active indicator during long continuous typing bursts.
 * 5. Voice Recording Management: Immediate "recording" dispatch + 3.5s refresh ticker, immediate clear on discard/send.
 */
class ChatActivityPresenceController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val onDispatchActivity: (roomId: String, activity: String?) -> Unit
) {
    companion object {
        const val MIN_INTER_PACKET_INTERVAL_MS = 1_500L
        const val TYPING_DEBOUNCE_PAUSED_MS = 2_500L
        const val SUSTAINED_ACTIVITY_REFRESH_MS = 3_500L
    }

    private var lastDispatchedTime: Long = 0L
    private var lastDispatchedActivity: String? = null
    private var previousTextLength: Int = 0

    private var debounceJob: Job? = null
    private var sustainedTickerJob: Job? = null
    private var currentRoomId: String? = null

    /**
     * Handles text changes in the message input field.
     */
    fun onTextChanged(newText: String, activeRoomId: String) {
        currentRoomId = activeRoomId
        val newLength = newText.length

        // Case 1: Input cleared to empty -> Immediate PAUSED
        if (newLength == 0) {
            previousTextLength = 0
            cancelAllJobs()
            dispatch(activeRoomId, null)
            return
        }

        // Case 2: Constructive typing detection (text length increased or first character)
        val isConstructive = newLength > previousTextLength || previousTextLength == 0
        previousTextLength = newLength

        if (!isConstructive && lastDispatchedActivity == "typing") {
            // Non-constructive deletion while remaining non-empty:
            // Just refresh the debounce timer without sending a network packet
            resetDebounceTimer(activeRoomId)
            return
        }

        val now = System.currentTimeMillis()
        val shouldEmitImmediately = lastDispatchedActivity != "typing" || (now - lastDispatchedTime >= MIN_INTER_PACKET_INTERVAL_MS)

        if (shouldEmitImmediately) {
            dispatch(activeRoomId, "typing")
            startSustainedTicker(activeRoomId, "typing")
        }

        resetDebounceTimer(activeRoomId)
    }

    /**
     * Voice note recording started (mic pressed/locked).
     */
    fun onRecordingStarted(activeRoomId: String) {
        currentRoomId = activeRoomId
        cancelAllJobs()
        dispatch(activeRoomId, "recording")
        startSustainedTicker(activeRoomId, "recording")
    }

    /**
     * Voice note recording stopped.
     * @param isDispatched true if audio message is sent, false if cancelled/trashed.
     */
    fun onRecordingStopped(activeRoomId: String, isDispatched: Boolean = false) {
        currentRoomId = activeRoomId
        cancelAllJobs()
        dispatch(activeRoomId, null)
    }

    /**
     * Message dispatched by sender.
     */
    fun onMessageSent(activeRoomId: String) {
        currentRoomId = activeRoomId
        previousTextLength = 0
        cancelAllJobs()
        dispatch(activeRoomId, null)
    }

    /**
     * Chat screen paused or application went to background.
     */
    fun onPauseOrBackground() {
        val room = currentRoomId
        cancelAllJobs()
        if (room != null && lastDispatchedActivity != null) {
            dispatch(room, null)
        }
    }

    private fun resetDebounceTimer(roomId: String) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(TYPING_DEBOUNCE_PAUSED_MS)
            // User paused typing for 2.5s -> emit null/paused
            sustainedTickerJob?.cancel()
            sustainedTickerJob = null
            dispatch(roomId, null)
        }
    }

    private fun startSustainedTicker(roomId: String, activity: String) {
        sustainedTickerJob?.cancel()
        sustainedTickerJob = scope.launch {
            while (isActive) {
                delay(SUSTAINED_ACTIVITY_REFRESH_MS)
                if (lastDispatchedActivity == activity) {
                    dispatch(roomId, activity)
                }
            }
        }
    }

    private fun dispatch(roomId: String, activity: String?) {
        lastDispatchedTime = System.currentTimeMillis()
        lastDispatchedActivity = activity
        onDispatchActivity(roomId, activity)
    }

    /**
     * Cancels all scheduled background jobs and releases resources.
     */
    fun dispose() {
        cancelAllJobs()
    }

    private fun cancelAllJobs() {
        debounceJob?.cancel()
        debounceJob = null
        sustainedTickerJob?.cancel()
        sustainedTickerJob = null
    }
}
