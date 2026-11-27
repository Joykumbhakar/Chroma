package com.chroma.studio.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentSectionHelpersTest {

    @Test
    fun `toggleReactionDataMap adds decrements and removes cleanly`() {
        val added = toggleReactionDataMap(emptyMap(), "🔥")
        assertEquals(1, added["🔥"]?.count)
        assertTrue(added["🔥"]?.activeByMe == true)

        val incremented = toggleReactionDataMap(mapOf("🔥" to ReactionData(2, false)), "🔥")
        assertEquals(3, incremented["🔥"]?.count)
        assertTrue(incremented["🔥"]?.activeByMe == true)

        val decremented = toggleReactionDataMap(incremented, "🔥")
        assertEquals(2, decremented["🔥"]?.count)
        assertFalse(decremented["🔥"]?.activeByMe == true)

        val removed = toggleReactionDataMap(added, "🔥")
        assertNull(removed["🔥"])
    }

    @Test
    fun `resolveVerifiedBadgeType respects verified flag and tier tones`() {
        assertEquals(VerifiedBadgeType.NONE, resolveVerifiedBadgeType(false, "gold"))
        assertEquals(VerifiedBadgeType.GOLD, resolveVerifiedBadgeType(true, "GOLD"))
        assertEquals(VerifiedBadgeType.EMERALD, resolveVerifiedBadgeType(true, "chroma"))
        assertEquals(VerifiedBadgeType.BLUE, resolveVerifiedBadgeType(true, null))
    }

    @Test
    fun `computeDeletedCommentCountDelta accounts for child replies`() {
        val author = CommentAuthor("A", "a")
        val reply = CommentReply("r1", author, "hi", 100L)
        val parentWithReplies = PostComment(
            id = "c1",
            author = author,
            text = "parent",
            createdAtMillis = 50L,
            replies = listOf(reply, reply.copy(id = "r2"))
        )
        assertEquals(-3, computeDeletedCommentCountDelta(parentWithReplies, null))
        assertEquals(-1, computeDeletedCommentCountDelta(parentWithReplies, "r1"))
    }
}
