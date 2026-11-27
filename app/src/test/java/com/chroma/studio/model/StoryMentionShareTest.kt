package com.chroma.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryMentionShareTest {

    @Test
    fun extractStoryMentionHandles_parsesBodyAndFormattedTitleHandles() {
        val handles = extractStoryMentionHandles(
            textContent = "Sunset walk with @Alex_Design and @maya.art!",
            textFormat = """{"title":"Shoutout @SAM_99"}"""
        )

        assertEquals(listOf("alex_design", "maya.art", "sam_99"), handles)
    }

    @Test
    fun isViewerMentionedInStory_matchesByUidHandleAndExcludesOwnerOrPending() {
        val story = Story(
            id = "story_1",
            userId = "creator_1",
            textContent = "Collab day with @maya.art",
            mentionedUserIds = listOf("user_42"),
            mentionedUsernames = listOf("maya.art")
        )

        assertTrue(isViewerMentionedInStory(story, viewerUid = "user_42", viewerUsername = "other"))
        assertTrue(isViewerMentionedInStory(story, viewerUid = "user_99", viewerUsername = "@Maya.Art"))
        assertFalse(isViewerMentionedInStory(story, viewerUid = "creator_1", viewerUsername = "maya.art"))
        assertFalse(isViewerMentionedInStory(story, viewerUid = "user_99", viewerUsername = "someone_else"))
        assertFalse(
            isViewerMentionedInStory(
                story.copy(id = "temp_uploading_1", isUploading = true),
                viewerUid = "user_42",
                viewerUsername = "maya.art"
            )
        )
    }

    @Test
    fun buildRepostedMentionStory_preservesMediaResetsCountersAndAttributesCreator() {
        val original = Story(
            id = "orig_123",
            userId = "alice_uid",
            mediaUrl = "https://cdn.example.com/story.jpg",
            textContent = "Look @bob_dev!",
            textFormat = "{\"fontFamily\":\"Inter\"}",
            backgroundColor = "#FF112233",
            views = 95,
            likes = 18,
            shares = 4,
            viewedBy = listOf("bob_uid"),
            likedBy = listOf("bob_uid"),
            isHighlight = true,
            highlightId = "hl_1",
            highlightTitle = "Trip",
            mentionedUserIds = listOf("bob_uid"),
            mentionedUsernames = listOf("bob_dev")
        )

        val now = 1_700_000_000_000L
        val repost = buildRepostedMentionStory(
            sourceStory = original,
            sharerUid = "bob_uid",
            newStoryId = "repost_456",
            originalAuthorUsername = "@alice_studio",
            originalAuthorDisplayName = "Alice Studio",
            nowMs = now
        )

        assertEquals("repost_456", repost.id)
        assertEquals("bob_uid", repost.userId)
        assertEquals(original.mediaUrl, repost.mediaUrl)
        assertEquals(original.textContent, repost.textContent)
        assertEquals(original.textFormat, repost.textFormat)
        assertEquals(original.backgroundColor, repost.backgroundColor)
        assertEquals("orig_123", repost.repostedFromStoryId)
        assertEquals("alice_uid", repost.repostedFromUserId)
        assertEquals("alice_studio", repost.repostedFromUsername)
        assertEquals("Alice Studio", repost.repostedFromDisplayName)
        assertEquals(0, repost.views)
        assertEquals(0, repost.likes)
        assertEquals(0, repost.shares)
        assertFalse(repost.isHighlight)
        assertEquals(now + 86_400_000L, repost.expiresAt)
        assertEquals("From @alice_studio", formatStoryRepostCredit(repost.repostedFromUsername, repost.repostedFromDisplayName))
        assertNull(formatStoryRepostCredit("", ""))
    }

    @Test
    fun notificationMentionHelpers_identifyIncomingStoryMentionsOnly() {
        val incoming = ChromaNotification(
            type = NotificationActionType.MENTION,
            actionText = "mentioned you in their story",
            targetId = "story:story_99"
        )
        val repostReceipt = ChromaNotification(
            type = NotificationActionType.MENTION,
            actionText = "added your mentioned story to their story",
            targetId = "story:repost_100"
        )
        val commentMention = ChromaNotification(
            type = NotificationActionType.MENTION,
            actionText = "mentioned you in a comment",
            targetId = "post_55"
        )

        assertTrue(isStoryMentionNotification(incoming))
        assertEquals("story_99", extractMentionedStoryId(incoming.targetId))
        assertFalse(isStoryMentionNotification(repostReceipt))
        assertFalse(isStoryMentionNotification(commentMention))
        assertNull(extractMentionedStoryId(commentMention.targetId))
    }
}
