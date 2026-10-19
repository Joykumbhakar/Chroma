package com.chroma.studio.data.mutation

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-quality Optimistic UI Mutation Coordinator.
 *
 * Guarantees:
 * 1. Immediate UI responsiveness on the calling thread (optimistic update).
 * 2. Coalescing of rapid repeated taps onto the latest desired state.
 * 3. Serialization of network requests per entity, preventing concurrent out-of-order race conditions.
 * 4. Stale response suppression (out-of-order responses cannot overwrite newer local states).
 * 5. Safe rollback on failure if and only if the user has not superseded the action with a newer interaction.
 * 6. Idempotent synchronization convergence.
 */
class OptimisticMutationCoordinator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private class MutationRecord<T>(
        val desiredState: T,
        val version: Long,
        val performServerSync: suspend (T) -> Boolean,
        val onRollback: () -> Unit,
        val onError: ((Throwable?) -> Unit)?
    )

    private val pendingMutations = ConcurrentHashMap<String, MutationRecord<*>>()
    private val lastSyncedVersions = ConcurrentHashMap<String, Long>()
    private val entityLocks = ConcurrentHashMap<String, Mutex>()
    private val currentVersions = ConcurrentHashMap<String, Long>()

    /**
     * Executes an optimistic mutation for [key].
     *
     * @param key Unique key for the entity and mutation type (e.g., "like:post_123", "follow:user_456")
     * @param desiredState The intended state (e.g. true for liked, false for unliked)
     * @param onOptimisticApply Invoked immediately on caller to update the local UI/StateFlow.
     * @param performServerSync Suspending call to send the desired state to the authoritative server. Returns true if successful.
     * @param onRollback Invoked if the network request fails and the user has NOT changed their mind since.
     * @param onError Optional callback invoked on failure.
     */
    fun <T> mutate(
        key: String,
        desiredState: T,
        onOptimisticApply: (T) -> Unit,
        performServerSync: suspend (T) -> Boolean,
        onRollback: () -> Unit,
        onError: ((Throwable?) -> Unit)? = null
    ) {
        val newVersion: Long
        synchronized(this) {
            val prev = currentVersions[key] ?: 0L
            newVersion = prev + 1L
            currentVersions[key] = newVersion
            pendingMutations[key] = MutationRecord(
                desiredState = desiredState,
                version = newVersion,
                performServerSync = performServerSync,
                onRollback = onRollback,
                onError = onError
            )
        }

        // 1. Immediately apply optimistic UI change
        onOptimisticApply(desiredState)

        // 2. Schedule serialized background sync
        val mutex = entityLocks.computeIfAbsent(key) { Mutex() }
        scope.launch {
            mutex.withLock {
                while (true) {
                    val currentMutation: MutationRecord<Any?>
                    synchronized(this@OptimisticMutationCoordinator) {
                        @Suppress("UNCHECKED_CAST")
                        currentMutation = pendingMutations[key] as? MutationRecord<Any?> ?: break
                        val lastSynced = lastSyncedVersions[key] ?: 0L
                        if (currentMutation.version <= lastSynced) {
                            break
                        }
                    }

                    var syncSuccess = false
                    var syncError: Throwable? = null
                    try {
                        syncSuccess = currentMutation.performServerSync(currentMutation.desiredState)
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                        syncSuccess = false
                        syncError = e
                    }

                    val latestMutation: MutationRecord<Any?>?
                    synchronized(this@OptimisticMutationCoordinator) {
                        @Suppress("UNCHECKED_CAST")
                        latestMutation = pendingMutations[key] as? MutationRecord<Any?>
                    }

                    if (latestMutation == null) break

                    if (!syncSuccess) {
                        // If no new tap superseded this mutation, perform rollback!
                        if (latestMutation.version == currentMutation.version) {
                            lastSyncedVersions[key] = currentMutation.version
                            try {
                                withContext(Dispatchers.Main.immediate) {
                                    currentMutation.onRollback()
                                    currentMutation.onError?.invoke(syncError)
                                }
                            } catch (_: Exception) {
                                currentMutation.onRollback()
                                currentMutation.onError?.invoke(syncError)
                            }
                            break
                        }
                        // Otherwise, superseded by newer mutation; loop to process the newer mutation!
                    } else {
                        // Sync succeeded!
                        if (latestMutation.version == currentMutation.version) {
                            lastSyncedVersions[key] = currentMutation.version
                            break
                        }
                        // Newer tap arrived; loop to sync the latest desired state.
                    }
                }
            }
        }
    }

    /**
     * Returns the current pending desired state for [key], or null if not mutating.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getDesiredState(key: String): T? = pendingMutations[key]?.desiredState as? T

    fun clear(key: String) {
        pendingMutations.remove(key)
        lastSyncedVersions.remove(key)
        currentVersions.remove(key)
    }
}
