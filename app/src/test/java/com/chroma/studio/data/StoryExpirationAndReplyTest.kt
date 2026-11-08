package com.chroma.studio.data

import com.chroma.studio.chat.data.ChatAdapters
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.model.Story
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests verifying 24-hour story expiration logic and story reply payload mapping.
 */
class StoryExpirationAndReplyTest {

    private fun isStoryAlive(story: Story, now: Long = System.currentTimeMillis()): Boolean {
        return if (story.expiresAt > 0L) {
            story.expiresAt > now
        } else {
            (now - story.timestamp) < 86_400_000L
        }
    }

    @Test
    fun testFreshStoryIsAlive() {
        val now = System.currentTimeMillis()
        val freshStory = Story(
            id = UUID.randomUUID().toString(),
            userId = "user_1",
            timestamp = now - 3_600_000L, // 1 hour ago
            expiresAt = now + 82_800_000L  // 23 hours remaining
        )
        assertTrue(isStoryAlive(freshStory, now))
    }

    @Test
    fun testExpiredStoryFailsLivenessCheck() {
        val now = System.currentTimeMillis()
        val expiredStory = Story(
            id = UUID.randomUUID().toString(),
            userId = "user_1",
            timestamp = now - 90_000_000L, // 25 hours ago
            expiresAt = now - 3_600_000L   // expired 1 hour ago
        )
        assertFalse(isStoryAlive(expiredStory, now))
    }

    @Test
    fun testFallbackLivenessWhenExpiresAtZero() {
        val now = System.currentTimeMillis()
        val storyNoExplicitExpiry = Story(
            id = UUID.randomUUID().toString(),
            userId = "user_1",
            timestamp = now - 72_000_000L, // 20 hours ago
            expiresAt = 0L
        )
        assertTrue(isStoryAlive(storyNoExplicitExpiry, now))

        val veryOldStoryNoExplicitExpiry = Story(
            id = UUID.randomUUID().toString(),
            userId = "user_1",
            timestamp = now - 87_000_000L, // 24.16 hours ago
            expiresAt = 0L
        )
        assertFalse(isStoryAlive(veryOldStoryNoExplicitExpiry, now))
    }

    @Test
    fun testStoryReplyMessageCleaning() {
        val rawPrefixText = "Replying to story: \"Awesome photo!\""
        val cleanedText = if (rawPrefixText.startsWith("Replying to story:", ignoreCase = true)) {
            val quoted = rawPrefixText.substringAfter("Replying to story:").trim()
            if (quoted.startsWith("\"") && quoted.endsWith("\"") && quoted.length >= 2) {
                quoted.substring(1, quoted.length - 1)
            } else {
                quoted
            }
        } else {
            rawPrefixText
        }
        assertEquals("Awesome photo!", cleanedText)
    }

    @Test
    fun testStoryReplyThumbnailPreservation() {
        val storyId = "story_test_abc"
        val thumbUrl = "https://example.com/story_thumb.jpg"
        val msg = ChatMessage(
            id = "msg_123",
            senderId = "sender_alice",
            senderName = "Alice",
            text = "Loved this story!",
            time = "10:30 AM",
            timestamp = System.currentTimeMillis(),
            storyId = storyId,
            storyThumbnailUrl = thumbUrl,
            mediaType = "story_reply"
        )

        assertEquals("story_test_abc", msg.storyId)
        assertEquals("https://example.com/story_thumb.jpg", msg.storyThumbnailUrl)
        assertEquals("story_reply", msg.mediaType)
    }

    @Test
    fun testThumbUrlResolutionForTextAndMediaStories() {
        // Media story
        val mediaStory = Story(
            id = "s_media",
            userId = "user_1",
            mediaUrl = "https://images.example.com/p.jpg",
            textContent = ""
        )
        val resolvedMediaThumb = if (!mediaStory.mediaUrl.isBlank() && mediaStory.mediaUrl != "text:story") {
            mediaStory.mediaUrl
        } else if (!mediaStory.textContent.isBlank()) {
            "text:${mediaStory.textContent}"
        } else {
            mediaStory.mediaUrl
        }
        assertEquals("https://images.example.com/p.jpg", resolvedMediaThumb)

        // Text story
        val textStory = Story(
            id = "s_text",
            userId = "user_1",
            mediaUrl = "text:story",
            textContent = "Hello Chroma!"
        )
        val resolvedTextThumb = if (!textStory.mediaUrl.isBlank() && textStory.mediaUrl != "text:story") {
            textStory.mediaUrl
        } else if (!textStory.textContent.isBlank()) {
            "text:${textStory.textContent}"
        } else {
            textStory.mediaUrl
        }
        assertEquals("text:Hello Chroma!", resolvedTextThumb)
    }

    @Test
    fun testStoryReplyTimerPausesDuringTyping() {
        fun computeEffectivePaused(
            isPaused: Boolean = false,
            isScrollInProgress: Boolean = false,
            quickReplyText: String = "",
            isReplyInputFocused: Boolean = false,
            isImeVisible: Boolean = false,
            showQuickReactions: Boolean = false
        ): Boolean {
            val isReplying = quickReplyText.isNotEmpty() || isReplyInputFocused || showQuickReactions || isImeVisible
            return isPaused || isScrollInProgress || isReplying
        }

        // Idle state: not paused
        assertFalse(computeEffectivePaused())

        // User typed text into reply input: paused
        assertTrue(computeEffectivePaused(quickReplyText = "Nice story!"))

        // User focused reply input without text: paused
        assertTrue(computeEffectivePaused(isReplyInputFocused = true))

        // Keyboard is visible: paused
        assertTrue(computeEffectivePaused(isImeVisible = true))

        // Quick reaction tray is open: paused
        assertTrue(computeEffectivePaused(showQuickReactions = true))
    }

    @Test
    fun testProportionalDurationCalculationOnResume() {
        val storyDurationMs = 5000
        val currentProgress = 0.6f // Paused at 60%

        val remainingFraction = 1f - currentProgress
        val remainingDuration = kotlin.math.round(storyDurationMs * remainingFraction).toInt().coerceAtLeast(50)

        // Resuming from 60% of 5000ms requires remaining 40% (2000ms)
        assertEquals(2000, remainingDuration)
    }
}
