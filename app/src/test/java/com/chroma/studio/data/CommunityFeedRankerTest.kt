package com.chroma.studio.data

import com.chroma.studio.model.DiscoverPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityFeedRankerTest {

    private fun samplePost(
        id: String,
        creatorId: String = "creator_$id",
        timestamp: Long = 1_700_000_000_000L,
        likes: Int = 5,
        comments: Int = 1,
        remixes: Int = 0,
        tags: List<String> = emptyList(),
        isTrending: Boolean = false
    ) = DiscoverPost(
        id = id,
        creatorId = creatorId,
        title = "Post $id",
        timestamp = timestamp,
        likesCount = likes,
        commentsCount = comments,
        remixCount = remixes,
        tags = tags,
        isTrending = isTrending
    )

    @Test
    fun rankPrioritizesUnseenPostsAheadOfPreviouslySeenPosts() {
        val now = 1_700_010_000_000L
        val olderUnseen = samplePost(id = "unseen_1", timestamp = now - 72_000_000L, likes = 2)
        val newerSeen = samplePost(id = "seen_1", timestamp = now - 60_000L, likes = 40)

        val ranked = CommunityFeedRanker.rank(
            candidates = listOf(newerSeen, olderUnseen),
            signals = FeedRankingSignals(
                seenPostIds = setOf("seen_1"),
                nowMs = now,
                refreshSeed = 42L
            )
        )

        assertEquals(listOf("unseen_1", "seen_1"), ranked.map { it.id })
    }

    @Test
    fun rankPinsTempAndBrandNewOwnPostsAtTopEvenWhenFollowingOrEngagementExists() {
        val now = 1_700_010_000_000L
        val viralOther = samplePost(id = "viral", timestamp = now - 3_600_000L, likes = 500, isTrending = true)
        val freshOwn = samplePost(id = "own_new", creatorId = "me", timestamp = now - 30_000L, likes = 0)
        val optimistic = samplePost(id = "temp_99", creatorId = "me", timestamp = now - 5_000L, likes = 0)

        val ranked = CommunityFeedRanker.rank(
            candidates = listOf(viralOther, freshOwn, optimistic),
            signals = FeedRankingSignals(
                currentUserId = "me",
                nowMs = now,
                refreshSeed = 7L
            )
        )

        assertEquals("temp_99", ranked.first().id)
        assertEquals("own_new", ranked[1].id)
        assertEquals("viral", ranked[2].id)
    }

    @Test
    fun rankReordersSeenOrEqualCandidatesWhenRefreshSeedChanges() {
        val now = 1_700_010_000_000L
        val pool = (1..12).map { idx ->
            samplePost(
                id = "p_$idx",
                creatorId = "c_$idx",
                timestamp = now - (idx * 3_600_000L),
                likes = 10
            )
        }

        val orderA = CommunityFeedRanker.rank(
            candidates = pool,
            signals = FeedRankingSignals(nowMs = now, refreshSeed = 101L)
        ).map { it.id }

        val orderB = CommunityFeedRanker.rank(
            candidates = pool,
            signals = FeedRankingSignals(nowMs = now, refreshSeed = 2026L)
        ).map { it.id }

        assertEquals(pool.size, orderA.size)
        assertEquals(pool.size, orderB.size)
        assertNotEquals(orderA, orderB)
    }

    @Test
    fun rankPreventsConsecutiveSameCreatorClusteringWhenOtherCreatorsExist() {
        val now = 1_700_010_000_000L
        val spamA1 = samplePost(id = "a1", creatorId = "spammer", timestamp = now - 1_000L, likes = 200)
        val spamA2 = samplePost(id = "a2", creatorId = "spammer", timestamp = now - 2_000L, likes = 195)
        val spamA3 = samplePost(id = "a3", creatorId = "spammer", timestamp = now - 3_000L, likes = 190)
        val creatorB = samplePost(id = "b1", creatorId = "artist_b", timestamp = now - 4_000L, likes = 10)
        val creatorC = samplePost(id = "c1", creatorId = "artist_c", timestamp = now - 5_000L, likes = 8)

        val ranked = CommunityFeedRanker.rank(
            candidates = listOf(spamA1, spamA2, spamA3, creatorB, creatorC),
            signals = FeedRankingSignals(nowMs = now, refreshSeed = 1L)
        )

        assertTrue(ranked[0].creatorId != ranked[1].creatorId)
        assertTrue(ranked[1].creatorId != ranked[2].creatorId)
    }

    @Test
    fun extractPreferredTagsWeightsSavedAndLikedPosts() {
        val works = listOf(
            samplePost(id = "w1", tags = listOf("#Neon", "Cyberpunk")),
            samplePost(id = "w2", tags = listOf("cyberpunk", "3D")),
            samplePost(id = "w3", tags = listOf("watercolor"))
        )

        val tags = CommunityFeedRanker.extractPreferredTags(
            works = works,
            likedWorkIds = setOf("w1"),
            savedWorkIds = setOf("w1", "w2")
        )

        assertTrue(tags.contains("cyberpunk"))
        assertTrue(tags.contains("neon"))
        assertTrue(tags.contains("3d"))
        assertTrue(!tags.contains("watercolor"))
    }
}
