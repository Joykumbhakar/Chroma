package com.chroma.studio.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MentionHelpersTest {

    @Test
    fun `extractMentionedUsernames parses handles, strips trailing dots, and ignores emails`() {
        val text = "Shoutout to @Example_User and @alice.jones! Contact support@oscellion.app or ping @BOB."
        val mentions = extractMentionedUsernames(text)

        assertEquals(listOf("example_user", "alice.jones", "bob"), mentions)
    }

    @Test
    fun `mergeMentionedUsernames deduplicates explicit handles and inline mentions`() {
        val merged = mergeMentionedUsernames(
            explicitMentions = listOf("@Alice", "charlie"),
            "Collab with @alice and @dev_team in this post"
        )
        assertEquals(listOf("alice", "charlie", "dev_team"), merged)
    }

    @Test
    fun `findNewlyAddedMentions only notifies net-new mentions on profile or post edits`() {
        val newlyAdded = findNewlyAddedMentions(
            previousTexts = listOf("Designer @alice building things"),
            newTexts = listOf("Designer @alice & @new_creator building things")
        )
        assertEquals(listOf("new_creator"), newlyAdded)
    }

    @Test
    fun `mention surfaces use clear human-readable action descriptions and routing prefixes`() {
        assertEquals("mentioned you in their profile", MentionSurface.PROFILE.actionText)
        assertEquals("profile:", MentionSurface.PROFILE.targetPrefix)
        assertEquals("mentioned you in comments", MentionSurface.COMMENT.actionText)
        assertEquals("mentioned you in their post", MentionSurface.POST.actionText)
        assertEquals("mentioned you in their story", MentionSurface.STORY.actionText)
        assertEquals("story:", MentionSurface.STORY.targetPrefix)
        assertEquals("mentioned you in chat", MentionSurface.CHAT.actionText)
        assertEquals("chat:", MentionSurface.CHAT.targetPrefix)
    }
}
