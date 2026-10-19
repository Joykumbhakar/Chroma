package com.chroma.studio.data.mutation

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OptimisticFeaturesTest {

    @Before
    fun setup() {
        OptimisticFollowManager.followSyncHandler = { _, _, _ -> true }
    }

    @After
    fun tearDown() {
        OptimisticFollowManager.followSyncHandler = null
    }

    @Test
    fun testOptimisticCommentLifecycle() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        val tempId = UUID.randomUUID().toString()

        data class MockComment(
            val id: String,
            val text: String,
            val isPending: Boolean,
            val isError: Boolean
        )

        var commentList = listOf<MockComment>()

        // 1. User sends comment -> Immediately shown with temporary ID and isPending = true
        val pendingComment = MockComment(id = tempId, text = "Amazing gradient!", isPending = true, isError = false)
        commentList = commentList + pendingComment

        assertTrue("Comment must appear immediately in local list", commentList.any { it.id == tempId && it.isPending })

        // 2. Simulate server failure
        commentList = commentList.map {
            if (it.id == tempId) it.copy(isPending = false, isError = true) else it
        }

        assertTrue("Failed comment must show retry/error state", commentList.any { it.id == tempId && it.isError })

        // 3. User taps retry -> returns to pending state
        commentList = commentList.map {
            if (it.id == tempId) it.copy(isPending = true, isError = false) else it
        }
        assertTrue("Retried comment returns to pending state", commentList.any { it.id == tempId && it.isPending })

        // 4. Server confirms -> reconciled
        val serverComment = MockComment(id = tempId, text = "Amazing gradient!", isPending = false, isError = false)
        commentList = commentList.map {
            if (it.id == tempId) serverComment else it
        }

        val confirmed = commentList.first { it.id == tempId }
        assertFalse(confirmed.isPending)
        assertFalse(confirmed.isError)
        assertEquals("Amazing gradient!", confirmed.text)
    }

    @Test
    fun testOptimisticFollowManagerCrossScreenSync() = runTest {
        val currentUserId = "user_me"
        val targetUserId = "creator_pro"

        // Initial state: not following
        assertFalse(OptimisticFollowManager.isFollowing(targetUserId))
        assertEquals(0, OptimisticFollowManager.getFollowerDelta(targetUserId))

        // Screen A (Profile Screen) triggers follow
        OptimisticFollowManager.toggleFollow(
            currentUserId = currentUserId,
            targetUserId = targetUserId,
            baseFollowing = false
        )

        // Screen B (User List Screen) observes immediately
        assertTrue("Follow state must be true immediately on all observing screens", OptimisticFollowManager.isFollowing(targetUserId))
        assertEquals("Follower delta must be +1 immediately", 1, OptimisticFollowManager.getFollowerDelta(targetUserId))

        // Screen B triggers unfollow
        OptimisticFollowManager.toggleFollow(
            currentUserId = currentUserId,
            targetUserId = targetUserId,
            baseFollowing = true
        )

        // Screen A observes immediately
        assertFalse("Follow state must be false immediately on all observing screens", OptimisticFollowManager.isFollowing(targetUserId))
        assertEquals("Follower delta must be 0 immediately", 0, OptimisticFollowManager.getFollowerDelta(targetUserId))
    }
}
