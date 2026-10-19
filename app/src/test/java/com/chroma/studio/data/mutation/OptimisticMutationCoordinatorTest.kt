package com.chroma.studio.data.mutation

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class OptimisticMutationCoordinatorTest {

    @Test
    fun testImmediateOptimisticUpdate() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        var uiLiked = false

        coordinator.mutate(
            key = "like:post_1",
            desiredState = true,
            onOptimisticApply = { uiLiked = it },
            performServerSync = { true },
            onRollback = { uiLiked = false }
        )

        // Verifies immediate synchronous execution on caller
        assertTrue("UI should update immediately to true before network finishes", uiLiked)
    }

    @Test
    fun testSafeRollbackOnFailureWhenNotSuperseded() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        var uiLiked = false
        var rollbackInvoked = false
        var errorInvoked = false

        coordinator.mutate(
            key = "like:post_2",
            desiredState = true,
            onOptimisticApply = { uiLiked = it },
            performServerSync = {
                // Network failure
                false
            },
            onRollback = {
                uiLiked = false
                rollbackInvoked = true
            },
            onError = {
                errorInvoked = true
            }
        )

        // Fast-forward coroutines
        testScheduler.advanceUntilIdle()

        assertFalse("UI should roll back to false after server failure", uiLiked)
        assertTrue("onRollback callback should have been invoked", rollbackInvoked)
        assertTrue("onError callback should have been invoked", errorInvoked)
    }

    @Test
    fun testRapidRepeatedTapsCoalesceToLatestState() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        var uiLiked = false
        val serverCallCount = AtomicInteger(0)
        var lastServerSyncedState: Boolean? = null

        // Rapid tap sequence: Like -> Unlike -> Like
        // Tap 1: Like
        coordinator.mutate(
            key = "like:post_3",
            desiredState = true,
            onOptimisticApply = { uiLiked = it },
            performServerSync = { state ->
                serverCallCount.incrementAndGet()
                delay(100) // Simulate network latency
                lastServerSyncedState = state
                true
            },
            onRollback = { uiLiked = false }
        )
        assertTrue(uiLiked)

        // Tap 2: Unlike
        coordinator.mutate(
            key = "like:post_3",
            desiredState = false,
            onOptimisticApply = { uiLiked = it },
            performServerSync = { state ->
                serverCallCount.incrementAndGet()
                delay(100)
                lastServerSyncedState = state
                true
            },
            onRollback = { uiLiked = true }
        )
        assertFalse(uiLiked)

        // Tap 3: Like again
        coordinator.mutate(
            key = "like:post_3",
            desiredState = true,
            onOptimisticApply = { uiLiked = it },
            performServerSync = { state ->
                serverCallCount.incrementAndGet()
                delay(100)
                lastServerSyncedState = state
                true
            },
            onRollback = { uiLiked = false }
        )
        assertTrue(uiLiked)

        // Advance time to allow background synchronization to settle
        testScheduler.advanceUntilIdle()

        assertTrue("Final UI state must be true", uiLiked)
        assertEquals("Final server state must converge on latest desired state", true, lastServerSyncedState)
    }

    @Test
    fun testNoRollbackWhenSupersededByNewerInteraction() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        var uiLiked = false
        var rollbackCount = 0

        // Tap 1: Like (which will fail after delay)
        coordinator.mutate(
            key = "like:post_4",
            desiredState = true,
            onOptimisticApply = { uiLiked = it },
            performServerSync = {
                delay(150)
                false // fails!
            },
            onRollback = {
                uiLiked = false
                rollbackCount++
            }
        )
        assertTrue(uiLiked)

        // While Tap 1 is still in flight (at 50ms), user explicitly changes their mind to Unlike!
        testScheduler.advanceTimeBy(50)
        coordinator.mutate(
            key = "like:post_4",
            desiredState = false,
            onOptimisticApply = { uiLiked = it },
            performServerSync = {
                delay(50)
                true // succeeds
            },
            onRollback = {
                uiLiked = true
                rollbackCount++
            }
        )
        assertFalse("UI should immediately show false", uiLiked)

        // Advance to let both finish
        testScheduler.advanceUntilIdle()

        // Since the user changed their mind before Tap 1 finished, Tap 1's failure must NOT
        // roll back the UI to an incorrect stale state!
        assertFalse("UI should remain false as intended by the user", uiLiked)
        assertEquals("Rollback must not be triggered for superseded mutation", 0, rollbackCount)
    }

    @Test
    fun testIndependentEntitiesMutateConcurrently() = runTest {
        val coordinator = OptimisticMutationCoordinator(this)
        var entityALiked = false
        var entityBLiked = false

        coordinator.mutate(
            key = "like:entity_A",
            desiredState = true,
            onOptimisticApply = { entityALiked = it },
            performServerSync = { delay(100); true },
            onRollback = { entityALiked = false }
        )

        coordinator.mutate(
            key = "like:entity_B",
            desiredState = true,
            onOptimisticApply = { entityBLiked = it },
            performServerSync = { delay(100); true },
            onRollback = { entityBLiked = false }
        )

        assertTrue(entityALiked)
        assertTrue(entityBLiked)

        testScheduler.advanceUntilIdle()

        assertTrue(entityALiked)
        assertTrue(entityBLiked)
    }
}
