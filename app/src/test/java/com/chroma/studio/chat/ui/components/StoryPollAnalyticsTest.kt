package com.chroma.studio.chat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryPollAnalyticsTest {

    @Test
    fun `extracts compact poll graph bars with percentages and winner`() {
        val payload = """
            {
              "stageWdp": 360.0,
              "stageHdp": 640.0,
              "studioWidgets": [
                {
                  "id": "poll_1",
                  "widgetType": "POLL",
                  "question": "Which theme fits best?",
                  "optA": "Minimal White",
                  "optB": "Pure Dark"
                },
                {
                  "id": "music_1",
                  "widgetType": "MUSIC",
                  "musicTitle": "Track"
                }
              ]
            }
        """.trimIndent()

        val summaries = buildStoryPollSummaries(payload) { widgetId, isQuiz ->
            if (widgetId == "poll_1" && !isQuiz) mapOf(0 to 3, 1 to 1) else emptyMap()
        }

        assertEquals(1, summaries.size)
        val poll = summaries.first()
        assertEquals("Which theme fits best?", poll.question)
        assertFalse(poll.isQuiz)
        assertEquals(4, poll.totalVotes)
        assertEquals(2, poll.options.size)

        assertEquals("Minimal White", poll.options[0].label)
        assertEquals(3, poll.options[0].votes)
        assertEquals(75, poll.options[0].percentage)
        assertTrue(poll.options[0].isWinner)

        assertEquals("Pure Dark", poll.options[1].label)
        assertEquals(1, poll.options[1].votes)
        assertEquals(25, poll.options[1].percentage)
        assertFalse(poll.options[1].isWinner)
    }

    @Test
    fun `handles zero-vote poll safely without NaN or winners`() {
        val payload = """
            {
              "studioWidgets": [
                {
                  "id": "poll_zero",
                  "widgetType": "POLL",
                  "question": "Ready to publish?",
                  "optA": "Yes",
                  "optB": "Wait"
                }
              ]
            }
        """.trimIndent()

        val summaries = buildStoryPollSummaries(payload)
        assertEquals(1, summaries.size)
        val poll = summaries.first()
        assertEquals(0, poll.totalVotes)
        assertEquals(0, poll.options[0].percentage)
        assertEquals(0, poll.options[1].percentage)
        assertFalse(poll.options[0].isWinner)
        assertFalse(poll.options[1].isWinner)
    }
}
