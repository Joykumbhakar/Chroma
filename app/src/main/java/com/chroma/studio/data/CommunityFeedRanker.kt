package com.chroma.studio.data

import com.chroma.studio.model.DiscoverPost
import kotlin.math.exp
import kotlin.math.ln

/**
 * Runtime signals used by [CommunityFeedRanker] to personalize and diversify the Community feed
 * so users see fresh, varied posts instead of a static chronological list.
 */
data class FeedRankingSignals(
    val followingUserIds: Set<String> = emptySet(),
    val preferredTags: Set<String> = emptySet(),
    val seenPostIds: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val nowMs: Long = System.currentTimeMillis(),
    val refreshSeed: Long = 0L
)

/**
 * Pure JVM discovery ranker balancing freshness, engagement quality, creator/tag affinity,
 * seen-post demotion, deterministic session jitter, and creator diversity spacing.
 */
object CommunityFeedRanker {

    private const val ONE_HOUR_MS = 3_600_000.0
    private const val OWN_FRESH_PIN_WINDOW_MS = 5 * 60_000L
    private const val UNSEEN_TIER_BONUS = 1_000.0
    private const val OWN_PIN_BONUS = 10_000.0
    private const val DIVERSITY_LOOKAHEAD = 5

    /**
     * Ranks [candidates] for the "For You" Community stream using [signals].
     */
    fun rank(
        candidates: List<DiscoverPost>,
        signals: FeedRankingSignals
    ): List<DiscoverPost> {
        if (candidates.size <= 1) return candidates

        val normalizedPreferredTags = signals.preferredTags
            .map { it.trim().removePrefix("#").lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        val scored = candidates
            .distinctBy { it.id }
            .map { post ->
                val isPinned = post.id.startsWith("temp_") || (
                    !signals.currentUserId.isNullOrBlank() &&
                        post.creatorId == signals.currentUserId &&
                        (signals.nowMs - post.timestamp) in 0L..OWN_FRESH_PIN_WINDOW_MS
                    )
                val isSeen = !isPinned && signals.seenPostIds.contains(post.id)
                val score = computeScore(post, signals, normalizedPreferredTags, isPinned, isSeen)
                ScoredPost(post = post, isPinned = isPinned, isSeen = isSeen, score = score)
            }
            .sortedWith(
                compareByDescending<ScoredPost> { it.score }
                    .thenByDescending { it.post.timestamp }
            )

        return applyCreatorDiversity(scored)
    }

    /**
     * Derives normalized affinity tags from posts the current user liked or saved.
     */
    fun extractPreferredTags(
        works: List<DiscoverPost>,
        likedWorkIds: Set<String>,
        savedWorkIds: Set<String>,
        maxTags: Int = 24
    ): Set<String> {
        if (likedWorkIds.isEmpty() && savedWorkIds.isEmpty()) return emptySet()
        val tagWeights = HashMap<String, Int>()
        for (work in works) {
            val weight = when {
                work.id in savedWorkIds && work.id in likedWorkIds -> 3
                work.id in savedWorkIds -> 2
                work.id in likedWorkIds -> 1
                else -> 0
            }
            if (weight == 0) continue
            for (rawTag in work.tags) {
                val clean = rawTag.trim().removePrefix("#").lowercase()
                if (clean.isNotEmpty()) {
                    tagWeights[clean] = (tagWeights[clean] ?: 0) + weight
                }
            }
        }
        return tagWeights.entries
            .sortedByDescending { it.value }
            .take(maxTags)
            .map { it.key }
            .toSet()
    }

    /**
     * Computes a deterministic `[0.0, 1.0]` jitter value for ([postId], [refreshSeed]).
     */
    fun deterministicJitter(postId: String, refreshSeed: Long): Double {
        var h = refreshSeed * 0x4F1BBCDCBA74565L
        for (ch in postId) {
            h = (h * 31L) xor ch.code.toLong()
        }
        // SplitMix-style finalizer for uniform distribution across similar post IDs
        h = (h xor (h ushr 30)) * 0x5F5A2B4E8D1C3A9L
        h = (h xor (h ushr 27)) * 0x6C078965E1B3A7BL
        h = h xor (h ushr 31)
        val positiveBits = h ushr 1
        return (positiveBits % 100_000L).toDouble() / 100_000.0
    }

    private fun computeScore(
        post: DiscoverPost,
        signals: FeedRankingSignals,
        preferredTags: Set<String>,
        isPinned: Boolean,
        isSeen: Boolean
    ): Double {
        if (isPinned) {
            return OWN_PIN_BONUS + (post.timestamp / 1_000_000_000_000.0)
        }

        val ageHours = ((signals.nowMs - post.timestamp).coerceAtLeast(0L) / ONE_HOUR_MS)
        // Smooth 36-hour half-life freshness curve in [0.0, 28.0]
        val freshnessScore = 28.0 * exp(-ageHours / 36.0)

        val weightedEngagement = (
            post.likesCount * 1.2 +
                post.effectiveCommentsCount * 2.5 +
                post.remixCount * 3.0 +
                post.saveCount * 2.0
            ).coerceAtLeast(0.0)
        // Logarithmic saturation keeps viral hits prominent without starving newer creators
        val engagementScore = ln(1.0 + weightedEngagement) * 5.0

        val followingBoost = if (!post.creatorId.isBlank() && signals.followingUserIds.contains(post.creatorId)) {
            14.0
        } else {
            0.0
        }

        val matchingTagCount = if (preferredTags.isEmpty()) 0 else {
            post.tags.count { raw ->
                preferredTags.contains(raw.trim().removePrefix("#").lowercase())
            }
        }
        val tagAffinityBoost = (matchingTagCount * 4.5).coerceAtMost(13.5)

        val trendingLift = if (post.isTrending) 6.0 else 0.0
        val unseenBoost = if (!isSeen) UNSEEN_TIER_BONUS else 0.0

        // Up to 22 points of refresh-specific rotation so pull-to-refresh produces a genuinely fresh order
        val jitter = deterministicJitter(post.id, signals.refreshSeed) * 22.0

        return unseenBoost + freshnessScore + engagementScore + followingBoost + tagAffinityBoost + trendingLift + jitter
    }

    /**
     * Prevents adjacent duplicate-creator runs while keeping pinned items in place and preserving seen-tier order.
     */
    private fun applyCreatorDiversity(sorted: List<ScoredPost>): List<DiscoverPost> {
        val remaining = sorted.toMutableList()
        val result = ArrayList<DiscoverPost>(sorted.size)
        val recentCreators = ArrayDeque<String>(3)

        while (remaining.isNotEmpty()) {
            val head = remaining.first()
            if (head.isPinned || head.post.creatorId.isBlank()) {
                remaining.removeAt(0)
                result.add(head.post)
                if (head.post.creatorId.isNotBlank()) {
                    recordRecentCreator(recentCreators, head.post.creatorId)
                }
                continue
            }

            // Pick highest-scoring item within lookahead window that avoids back-to-back same creator
            var chosenIndex = 0
            if (recentCreators.isNotEmpty()) {
                val lastCreator = recentCreators.last()
                if (head.post.creatorId == lastCreator) {
                    val altIdx = (1 until minOf(DIVERSITY_LOOKAHEAD, remaining.size)).firstOrNull { idx ->
                        val cand = remaining[idx]
                        cand.isSeen == head.isSeen &&
                            cand.post.creatorId.isNotBlank() &&
                            cand.post.creatorId != lastCreator &&
                            !recentCreators.contains(cand.post.creatorId)
                    } ?: (1 until minOf(DIVERSITY_LOOKAHEAD, remaining.size)).firstOrNull { idx ->
                        val cand = remaining[idx]
                        cand.isSeen == head.isSeen && cand.post.creatorId != lastCreator
                    }
                    if (altIdx != null) {
                        chosenIndex = altIdx
                    }
                }
            }

            val picked = remaining.removeAt(chosenIndex)
            result.add(picked.post)
            if (picked.post.creatorId.isNotBlank()) {
                recordRecentCreator(recentCreators, picked.post.creatorId)
            }
        }
        return result
    }

    private fun recordRecentCreator(queue: ArrayDeque<String>, creatorId: String) {
        if (queue.size >= 2) {
            queue.removeFirst()
        }
        queue.addLast(creatorId)
    }

    private data class ScoredPost(
        val post: DiscoverPost,
        val isPinned: Boolean,
        val isSeen: Boolean,
        val score: Double
    )
}
