package com.chroma.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStudioHelpersTest {

    @Test
    fun `mergeEvent deduplicates and orders newest first`() {
        val older = LiveStudioEventRow(id = "e1", body = "old", createdAtMs = 100L)
        val newer = LiveStudioEventRow(id = "e2", body = "new", createdAtMs = 200L)
        val updatedOlder = LiveStudioEventRow(id = "e1", body = "edited", createdAtMs = 100L)

        val merged = LiveStudioHelpers.mergeEvent(listOf(older), newer)
        assertEquals(listOf("e2", "e1"), merged.map { it.id })

        val updated = LiveStudioHelpers.mergeEvent(merged, updatedOlder)
        assertEquals(2, updated.size)
        assertEquals("edited", updated.first { it.id == "e1" }.body)
    }

    @Test
    fun `toggleLike updates likedBy and likesCount cleanly`() {
        val base = LiveStudioEventRow(id = "e1", likedBy = emptyList(), likesCount = 0)
        val liked = LiveStudioHelpers.toggleLike(base, "u1")
        assertTrue(liked.likedBy.contains("u1"))
        assertEquals(1, liked.likesCount)

        val unliked = LiveStudioHelpers.toggleLike(liked, "u1")
        assertFalse(unliked.likedBy.contains("u1"))
        assertEquals(0, unliked.likesCount)
    }

    @Test
    fun `votePoll switches user vote atomically across options`() {
        val base = LiveStudioEventRow(
            id = "p1",
            eventType = "poll",
            pollQuestion = "Best LUT?",
            pollOptions = listOf(
                LiveStudioPollOption("opt_1", "Warm Film", listOf("u2")),
                LiveStudioPollOption("opt_2", "Cool Cobalt", emptyList())
            )
        )
        val votedA = LiveStudioHelpers.votePoll(base, "opt_1", "u1")
        assertEquals(2, votedA.pollOptions.first { it.id == "opt_1" }.voters.size)

        val switchedB = LiveStudioHelpers.votePoll(votedA, "opt_2", "u1")
        assertFalse(switchedB.pollOptions.first { it.id == "opt_1" }.voters.contains("u1"))
        assertTrue(switchedB.pollOptions.first { it.id == "opt_2" }.voters.contains("u1"))
    }

    @Test
    fun `parseHexPaletteInput validates normalizes and caps swatches`() {
        val parsed = LiveStudioHelpers.parseHexPaletteInput("0f172a, #38bdf8 invalid #F43F5E 10B981 #AABBCC #112233")
        assertEquals(listOf("#0F172A", "#38BDF8", "#F43F5E", "#10B981", "#AABBCC"), parsed)
    }
}
