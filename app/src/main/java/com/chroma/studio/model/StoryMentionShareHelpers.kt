package com.chroma.studio.model

import com.chroma.studio.data.extractMentionedUsernames

private const val STORY_REPOST_LIFETIME_MS = 86_400_000L

/**
 * Extracts normalized `@username` handles mentioned in a story's body text or formatted payload.
 */
fun extractStoryMentionHandles(textContent: String?, textFormat: String?): List<String> {
    return extractMentionedUsernames(textContent, textFormat)
}

/**
 * Returns `true` when [viewerUid] or [viewerUsername] is mentioned in [story] and the viewer
 * is not the story's owner (and the story is not an unsent local upload placeholder).
 */
fun isViewerMentionedInStory(
    story: Story,
    viewerUid: String?,
    viewerUsername: String?
): Boolean {
    if (story.id.isBlank() || story.id.startsWith("temp_") || story.isFailed) return false
    if (!viewerUid.isNullOrBlank() && story.userId == viewerUid) return false

    if (!viewerUid.isNullOrBlank() && story.mentionedUserIds.contains(viewerUid)) {
        return true
    }

    val normalizedViewerHandle = viewerUsername
        ?.trim()
        ?.removePrefix("@")
        ?.lowercase()
        .orEmpty()
    if (normalizedViewerHandle.isBlank()) return false

    if (story.mentionedUsernames.any { it.trim().removePrefix("@").equals(normalizedViewerHandle, ignoreCase = true) }) {
        return true
    }

    val extracted = extractStoryMentionHandles(story.textContent, story.decompressedTextFormat)
    return normalizedViewerHandle in extracted
}

/**
 * Returns `true` when [notification] represents an incoming story-mention notification
 * eligible for one-tap story resharing.
 */
fun isStoryMentionNotification(notification: ChromaNotification): Boolean {
    if (notification.type != NotificationActionType.MENTION) return false
    val target = notification.targetId.orEmpty()
    if (target.startsWith("story:") && notification.actionText.contains("added your mentioned story", ignoreCase = true)) {
        return false
    }
    return target.startsWith("story:") || notification.actionText.contains("mentioned you in their story", ignoreCase = true)
}

/**
 * Extracts the underlying Firestore story ID from a notification [targetId] such as `"story:<id>"`.
 */
fun extractMentionedStoryId(targetId: String?): String? {
    val raw = targetId?.trim().orEmpty()
    if (!raw.startsWith("story:")) return null
    return raw.removePrefix("story:").trim().takeIf { it.isNotBlank() }
}

/**
 * Builds the new [Story] document when [sharerUid] reshares [sourceStory] to their own profile/story ring,
 * preserving visual media/text styling and recording attribution back to the original creator.
 */
fun buildRepostedMentionStory(
    sourceStory: Story,
    sharerUid: String,
    newStoryId: String,
    originalAuthorUsername: String,
    originalAuthorDisplayName: String,
    audienceType: String = "public",
    customAudienceIds: List<String> = emptyList(),
    nowMs: Long = System.currentTimeMillis()
): Story {
    val rootStoryId = sourceStory.repostedFromStoryId.ifBlank { sourceStory.id }
    val rootUserId = sourceStory.repostedFromUserId.ifBlank { sourceStory.userId }
    val rootUsername = sourceStory.repostedFromUsername.ifBlank {
        originalAuthorUsername.trim().removePrefix("@")
    }
    val rootDisplayName = sourceStory.repostedFromDisplayName.ifBlank {
        originalAuthorDisplayName.trim()
    }

    return Story(
        id = newStoryId,
        userId = sharerUid,
        mediaUrl = sourceStory.mediaUrl,
        textContent = sourceStory.textContent,
        textFormat = sourceStory.textFormat,
        backgroundColor = sourceStory.backgroundColor,
        audienceType = audienceType,
        customAudienceIds = customAudienceIds,
        timestamp = nowMs,
        expiresAt = nowMs + STORY_REPOST_LIFETIME_MS,
        views = 0,
        likes = 0,
        shares = 0,
        viewedBy = emptyList(),
        likedBy = emptyList(),
        isHighlight = false,
        highlightId = "",
        highlightTitle = "",
        highlightCoverUrl = "",
        mentionedUserIds = emptyList(),
        mentionedUsernames = emptyList(),
        repostedFromStoryId = rootStoryId,
        repostedFromUserId = rootUserId,
        repostedFromUsername = rootUsername,
        repostedFromDisplayName = rootDisplayName,
        musicTrackId = sourceStory.musicTrackId,
        musicTitle = sourceStory.musicTitle,
        musicArtist = sourceStory.musicArtist,
        musicAlbum = sourceStory.musicAlbum,
        musicCoverUrl = sourceStory.musicCoverUrl,
        musicAudioUrl = sourceStory.musicAudioUrl,
        musicStartSec = sourceStory.musicStartSec,
        musicDurationSec = sourceStory.musicDurationSec,
        musicShowLyrics = sourceStory.musicShowLyrics
    )
}

/**
 * Formats a concise attribution label (e.g. `"From @alex"`) for a reposted story, or `null`
 * if the story is original.
 */
fun formatStoryRepostCredit(
    repostedFromUsername: String?,
    repostedFromDisplayName: String?
): String? {
    val handle = repostedFromUsername?.trim()?.removePrefix("@").orEmpty()
    if (handle.isNotBlank()) return "From @$handle"
    val name = repostedFromDisplayName?.trim().orEmpty()
    if (name.isNotBlank()) return "From $name"
    return null
}
