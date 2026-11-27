package com.chroma.studio.data

/**
 * Surfaces across Oscellion where a user can type `@username` mentions.
 */
enum class MentionSurface(
    val actionText: String,
    val targetPrefix: String
) {
    PROFILE("mentioned you in their profile", "profile:"),
    COMMENT("mentioned you in comments", ""),
    POST("mentioned you in their post", ""),
    STORY("mentioned you in their story", "story:"),
    STORY_REPLY("mentioned you in a story reply", "chat:"),
    CHAT("mentioned you in chat", "chat:")
}

private val MENTION_TOKEN_REGEX = Regex("""(?<![A-Za-z0-9_.])@([A-Za-z0-9_.]{1,30})""")

/**
 * Normalizes a username or handle (`@Example_User.` -> `example_user`).
 */
fun normalizeMentionHandle(raw: String): String {
    val stripped = raw.trim().removePrefix("@").trimEnd('.')
    if (stripped.isBlank() || stripped.all { it == '.' || it == '_' }) return ""
    return stripped.lowercase()
}

/**
 * Extracts distinct normalized usernames mentioned via `@username` across one or more texts.
 * Ignores email addresses (e.g. `name@domain.com`) and preserves first-seen order.
 */
fun extractMentionedUsernames(vararg texts: String?): List<String> {
    val result = LinkedHashSet<String>()
    for (text in texts) {
        if (text.isNullOrBlank()) continue
        for (match in MENTION_TOKEN_REGEX.findAll(text)) {
            val normalized = normalizeMentionHandle(match.groupValues[1])
            if (normalized.isNotEmpty()) {
                result.add(normalized)
            }
        }
    }
    return result.toList()
}

/**
 * Combines explicit mention handles with `@username` tokens parsed from text fields.
 */
fun mergeMentionedUsernames(
    explicitMentions: List<String> = emptyList(),
    vararg texts: String?
): List<String> {
    val merged = LinkedHashSet<String>()
    for (raw in explicitMentions) {
        val normalized = normalizeMentionHandle(raw)
        if (normalized.isNotEmpty()) merged.add(normalized)
    }
    merged.addAll(extractMentionedUsernames(*texts))
    return merged.toList()
}

/**
 * Returns only newly introduced `@username` handles when updating existing content (e.g., profile bio or edited post/comment).
 */
fun findNewlyAddedMentions(
    previousTexts: List<String?>,
    newTexts: List<String?>,
    explicitNewMentions: List<String> = emptyList()
): List<String> {
    val previousSet = extractMentionedUsernames(*previousTexts.toTypedArray()).toSet()
    val currentList = mergeMentionedUsernames(explicitNewMentions, *newTexts.toTypedArray())
    return currentList.filter { it !in previousSet }
}
