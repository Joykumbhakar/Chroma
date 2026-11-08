package com.chroma.studio.chat.controller

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatActivityPresenceControllerTest {

    @Test
    fun testImmediateTypingEmissionOnFirstChar() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("H", "room123")
            runCurrent()

            assertEquals(1, dispatchedEvents.size)
            assertEquals("room123" to "typing", dispatchedEvents.first())
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testNonConstructiveDeletionDoesNotEmitNewPacket() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("Hello", "room123")
            runCurrent()
            assertEquals(1, dispatchedEvents.size)

            // Non-constructive backspace: length decreases from 5 to 4
            controller.onTextChanged("Hell", "room123")
            runCurrent()

            // Should NOT emit another packet over network
            assertEquals(1, dispatchedEvents.size)
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testClearingInputDispatchesNullImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("Hi", "room123")
            runCurrent()
            assertEquals(listOf("room123" to "typing"), dispatchedEvents)

            // Clear input to empty
            controller.onTextChanged("", "room123")
            runCurrent()

            assertEquals(2, dispatchedEvents.size)
            assertEquals("room123" to null, dispatchedEvents.last())
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testDebounceEmitsNullAfter2500ms() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("Typing something...", "room123")
            runCurrent()
            assertEquals(listOf("room123" to "typing"), dispatchedEvents)

            // Advance 2,400ms: still within 2,500ms debounce window
            advanceTimeBy(2_400L)
            runCurrent()
            assertEquals(1, dispatchedEvents.size)

            // Advance past 2,500ms debounce
            advanceTimeBy(150L)
            runCurrent()

            assertEquals(2, dispatchedEvents.size)
            assertEquals("room123" to null, dispatchedEvents.last())
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testVoiceRecordingStartAndStop() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onRecordingStarted("room123")
            runCurrent()
            assertEquals(listOf("room123" to "recording"), dispatchedEvents)

            controller.onRecordingStopped("room123")
            runCurrent()
            assertEquals(2, dispatchedEvents.size)
            assertEquals("room123" to null, dispatchedEvents.last())
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testOnMessageSentDispatchesNullImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("Sending this now", "room123")
            runCurrent()
            assertEquals(1, dispatchedEvents.size)

            controller.onMessageSent("room123")
            runCurrent()
            assertEquals(2, dispatchedEvents.size)
            assertEquals("room123" to null, dispatchedEvents.last())
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun testOnPauseOrBackgroundDispatchesNull() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val dispatchedEvents = mutableListOf<Pair<String, String?>>()

        val controller = ChatActivityPresenceController(
            scope = testScope,
            onDispatchActivity = { roomId, activity ->
                dispatchedEvents.add(roomId to activity)
            }
        )

        try {
            controller.onTextChanged("Drafting...", "room123")
            runCurrent()
            assertEquals(listOf("room123" to "typing"), dispatchedEvents)

            controller.onPauseOrBackground()
            runCurrent()
            assertEquals(2, dispatchedEvents.size)
            assertEquals("room123" to null, dispatchedEvents.last())
        } finally {
            controller.dispose()
        }
    }
}
