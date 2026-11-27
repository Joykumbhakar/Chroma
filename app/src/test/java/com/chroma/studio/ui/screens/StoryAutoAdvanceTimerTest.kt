package com.chroma.studio.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryAutoAdvanceTimerTest {

    @Test
    fun `short photo or text stories use 5s base duration`() {
        assertEquals(
            StoryAutoAdvanceDefaults.MIN_DURATION_MS,
            computeStoryAutoAdvanceDurationMs(hasVisualMedia = true, textContent = "")
        )
        assertEquals(
            StoryAutoAdvanceDefaults.MIN_DURATION_MS,
            computeStoryAutoAdvanceDurationMs(hasVisualMedia = false, textContent = "Quick morning update")
        )
    }

    @Test
    fun `long text stories extend reading duration and clamp at max`() {
        val mediumText = List(20) { "word" }.joinToString(" ")
        val mediumDuration = computeStoryAutoAdvanceDurationMs(
            hasVisualMedia = false,
            textContent = mediumText
        )
        // 5000 + (20 - 8) * 220 = 7640ms
        assertEquals(7640L, mediumDuration)

        val veryLongText = List(100) { "word" }.joinToString(" ")
        val cappedDuration = computeStoryAutoAdvanceDurationMs(
            hasVisualMedia = false,
            textContent = veryLongText
        )
        assertEquals(StoryAutoAdvanceDefaults.MAX_DURATION_MS, cappedDuration)
    }

    @Test
    fun `interactive widgets grant bonus viewing time`() {
        val formatJson = """{"widgets":[{"type":"poll","question":"Coffee or Tea?"}]}"""
        assertTrue(hasInteractiveStoryWidgets(formatJson))
        assertFalse(hasInteractiveStoryWidgets("""{"font":"serif"}"""))

        val withPoll = computeStoryAutoAdvanceDurationMs(
            hasVisualMedia = true,
            textContent = "",
            hasInteractiveWidgets = true
        )
        assertEquals(7000L, withPoll)
    }

    @Test
    fun `advanceStoryProgress preserves progress across pause and resume`() {
        val duration = 5000L
        val afterFirstBurst = advanceStoryProgress(0f, deltaMs = 2000L, totalDurationMs = duration)
        assertEquals(0.4f, afterFirstBurst, 0.0001f)

        // Simulated pause (0ms delta) leaves progress intact
        val whilePaused = advanceStoryProgress(afterFirstBurst, deltaMs = 0L, totalDurationMs = duration)
        assertEquals(0.4f, whilePaused, 0.0001f)

        // Resuming advances smoothly to completion
        val afterResume = advanceStoryProgress(whilePaused, deltaMs = 3000L, totalDurationMs = duration)
        assertEquals(1.0f, afterResume, 0.0001f)
        assertTrue(isStoryProgressComplete(afterResume))
    }

    @Test
    fun `computeRemainingStoryDurationMs scales accurately without resetting`() {
        assertEquals(5000, computeRemainingStoryDurationMs(0f, 5000L))
        assertEquals(2500, computeRemainingStoryDurationMs(0.5f, 5000L))
        assertEquals(0, computeRemainingStoryDurationMs(1.0f, 5000L))
        assertEquals(0, computeRemainingStoryDurationMs(0.9995f, 5000L))
    }
}
