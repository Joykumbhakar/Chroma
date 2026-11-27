package com.chroma.studio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class LiveStudioRoom(
    val id: String,
    val title: String,
    val subtitle: String
) {
    GENERAL("general", "General", "Open creative studio lounge"),
    COLOR_GRADING("color_grading", "Color & LUTs", "Live color palettes & tone curves"),
    CRITIQUE("critique", "Critique", "Real-time composition feedback"),
    FLASH_HOOKS("flash_hooks", "Flash Hooks", "Short-form pacing & edit ideas")
}

enum class LiveStudioConnectionState {
    CONNECTING,
    LIVE,
    OFFLINE
}

enum class LiveStudioComposerMode(val label: String) {
    NOTE("Note"),
    PALETTE("Palette"),
    POLL("Poll")
}

@Serializable
data class LiveStudioPollOption(
    val id: String,
    val label: String,
    val voters: List<String> = emptyList()
)

@Serializable
data class LiveStudioEventRow(
    val id: String = "",
    @SerialName("room_id")
    val roomId: String = LiveStudioRoom.GENERAL.id,
    @SerialName("event_type")
    val eventType: String = "note",
    @SerialName("author_uid")
    val authorUid: String = "",
    @SerialName("author_name")
    val authorName: String = "",
    @SerialName("author_handle")
    val authorHandle: String = "",
    @SerialName("author_avatar_url")
    val authorAvatarUrl: String = "",
    @SerialName("author_verified")
    val authorVerified: Boolean = false,
    @SerialName("author_verified_tone")
    val authorVerifiedTone: String? = null,
    val body: String = "",
    @SerialName("palette_hexes")
    val paletteHexes: List<String> = emptyList(),
    @SerialName("poll_question")
    val pollQuestion: String = "",
    @SerialName("poll_options_json")
    val pollOptions: List<LiveStudioPollOption> = emptyList(),
    @SerialName("liked_by")
    val likedBy: List<String> = emptyList(),
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class LivePulseReaction(
    val id: String = "",
    val emoji: String = "✨",
    @SerialName("sender_name")
    val senderName: String = "",
    @SerialName("timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class LivePresenceMember(
    val uid: String = "",
    val name: String = "Creator",
    @SerialName("avatar_url")
    val avatarUrl: String = ""
)

data class CuratedStudioPalette(
    val title: String,
    val hexes: List<String>
)

object LiveStudioHelpers {
    val CuratedPalettes = listOf(
        CuratedStudioPalette("Cinematic Golden", listOf("#0F172A", "#312E81", "#EA580C", "#FBBF24")),
        CuratedStudioPalette("Nordic Frost", listOf("#0B132B", "#1C2541", "#5BC0BE", "#E2E8F0")),
        CuratedStudioPalette("Editorial Mono", listOf("#111114", "#3F3F46", "#A1A1AA", "#FAFAFA")),
        CuratedStudioPalette("Emerald Film", listOf("#022C22", "#065F46", "#10B981", "#A7F3D0"))
    )

    fun mergeEvent(
        current: List<LiveStudioEventRow>,
        incoming: LiveStudioEventRow
    ): List<LiveStudioEventRow> {
        val withoutDuplicate = current.filterNot { it.id == incoming.id }
        return (listOf(incoming) + withoutDuplicate)
            .sortedByDescending { it.createdAtMs }
    }

    fun removeEvent(
        current: List<LiveStudioEventRow>,
        eventId: String
    ): List<LiveStudioEventRow> = current.filterNot { it.id == eventId }

    fun toggleLike(
        row: LiveStudioEventRow,
        userId: String
    ): LiveStudioEventRow {
        if (userId.isBlank()) return row
        val alreadyLiked = row.likedBy.contains(userId)
        val nextLikedBy = if (alreadyLiked) row.likedBy - userId else (row.likedBy + userId).distinct()
        return row.copy(
            likedBy = nextLikedBy,
            likesCount = nextLikedBy.size
        )
    }

    fun votePoll(
        row: LiveStudioEventRow,
        optionId: String,
        userId: String
    ): LiveStudioEventRow {
        if (userId.isBlank() || row.pollOptions.isEmpty()) return row
        val currentSelection = row.pollOptions.firstOrNull { it.voters.contains(userId) }?.id
        if (currentSelection == optionId) return row

        val updatedOptions = row.pollOptions.map { option ->
            val cleanedVoters = option.voters - userId
            if (option.id == optionId) {
                option.copy(voters = (cleanedVoters + userId).distinct())
            } else {
                option.copy(voters = cleanedVoters)
            }
        }
        return row.copy(pollOptions = updatedOptions)
    }

    fun parseHexPaletteInput(raw: String): List<String> {
        return raw.split(",", " ", "\n")
            .map { token -> token.trim().removePrefix("#").uppercase() }
            .filter { token -> token.matches(Regex("^[0-9A-F]{6}$")) }
            .distinct()
            .take(5)
            .map { "#$it" }
    }
}
