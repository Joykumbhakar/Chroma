@file:OptIn(SupabaseExperimental::class)

package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.LivePresenceMember
import com.chroma.studio.model.LivePulseReaction
import com.chroma.studio.model.LiveStudioConnectionState
import com.chroma.studio.model.LiveStudioEventRow
import com.chroma.studio.model.LiveStudioPollOption
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed class LiveStudioStreamUpdate {
    data class ConnectionChanged(val state: LiveStudioConnectionState, val errorMessage: String? = null) : LiveStudioStreamUpdate()
    data class InitialSnapshot(val events: List<LiveStudioEventRow>) : LiveStudioStreamUpdate()
    data class Upserted(val event: LiveStudioEventRow) : LiveStudioStreamUpdate()
    data class Deleted(val eventId: String) : LiveStudioStreamUpdate()
    data class PresenceUpdated(val members: List<LivePresenceMember>) : LiveStudioStreamUpdate()
    data class ReactionBurst(val reaction: LivePulseReaction) : LiveStudioStreamUpdate()
}

@Serializable
private data class LiveStudioLikePatch(
    @kotlinx.serialization.SerialName("liked_by")
    val likedBy: List<String>,
    @kotlinx.serialization.SerialName("likes_count")
    val likesCount: Int
)

@Serializable
private data class LiveStudioPollPatch(
    @kotlinx.serialization.SerialName("poll_options_json")
    val pollOptions: List<LiveStudioPollOption>
)

class SupabaseLiveStudioRepository {
    companion object {
        private const val TAG = "SupabaseLiveStudioRepo"
        private const val TABLE_NAME = "live_studio_events"
    }

    private val client = SupabaseClient.client

    @Volatile
    private var activeChannel: RealtimeChannel? = null

    fun observeRoomStream(
        roomId: String,
        member: LivePresenceMember
    ): Flow<LiveStudioStreamUpdate> = channelFlow {
        send(LiveStudioStreamUpdate.ConnectionChanged(LiveStudioConnectionState.CONNECTING))

        val topicName = "live_studio:$roomId"
        val channel = client.channel(topicName)
        activeChannel = channel

        // Register flows BEFORE subscribe() per supabase-kt realtime-kt 2.2.3 contract
        val pgFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = TABLE_NAME
            filter("room_id", FilterOperator.EQ, roomId)
        }
        val presenceFlow = channel.presenceDataFlow<LivePresenceMember>()
        val broadcastFlow = channel.broadcastFlow<LivePulseReaction>(event = "pulse_reaction")

        pgFlow.onEach { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    runCatching { action.decodeRecord<LiveStudioEventRow>() }
                        .onSuccess { trySend(LiveStudioStreamUpdate.Upserted(it)) }
                }
                is PostgresAction.Update -> {
                    runCatching { action.decodeRecord<LiveStudioEventRow>() }
                        .onSuccess { trySend(LiveStudioStreamUpdate.Upserted(it)) }
                }
                is PostgresAction.Delete -> {
                    val deletedId = runCatching {
                        action.decodeOldRecordOrNull<LiveStudioEventRow>()?.id
                    }.getOrNull()
                    if (!deletedId.isNullOrBlank()) {
                        trySend(LiveStudioStreamUpdate.Deleted(deletedId))
                    }
                }
                else -> Unit
            }
        }.launchIn(this)

        presenceFlow.onEach { members ->
            val distinct = (members + member).filter { it.uid.isNotBlank() }.distinctBy { it.uid }
            trySend(LiveStudioStreamUpdate.PresenceUpdated(distinct))
        }.launchIn(this)

        broadcastFlow.onEach { reaction ->
            trySend(LiveStudioStreamUpdate.ReactionBurst(reaction))
        }.launchIn(this)

        // Fetch initial Postgres rows
        try {
            val rows = withContext(Dispatchers.IO) {
                client.postgrest.from(TABLE_NAME)
                    .select {
                        filter { eq("room_id", roomId) }
                        order("created_at_ms", Order.DESCENDING)
                        limit(80)
                    }
                    .decodeList<LiveStudioEventRow>()
            }
            send(LiveStudioStreamUpdate.InitialSnapshot(rows))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Initial fetch failed for room=$roomId: ${e.message}")
            send(LiveStudioStreamUpdate.InitialSnapshot(emptyList()))
        }

        // Open Realtime WebSocket + subscribe channel
        try {
            withContext(Dispatchers.IO) {
                client.realtime.connect()
                channel.subscribe(true)
                channel.track(
                    JsonObject(
                        mapOf(
                            "uid" to JsonPrimitive(member.uid),
                            "name" to JsonPrimitive(member.name),
                            "avatar_url" to JsonPrimitive(member.avatarUrl)
                        )
                    )
                )
            }
            send(LiveStudioStreamUpdate.PresenceUpdated(listOf(member)))
            send(LiveStudioStreamUpdate.ConnectionChanged(LiveStudioConnectionState.LIVE))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Realtime subscription failed for $topicName: ${e.message}")
            send(
                LiveStudioStreamUpdate.ConnectionChanged(
                    state = LiveStudioConnectionState.OFFLINE,
                    errorMessage = "Live sync paused (offline or awaiting migration)"
                )
            )
        }

        awaitClose {
            activeChannel = null
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { channel.untrack() }
                runCatching { channel.unsubscribe() }
                runCatching { client.realtime.removeChannel(channel) }
            }
        }
    }

    suspend fun insertEvent(row: LiveStudioEventRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from(TABLE_NAME).insert(row)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "insertEvent error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateLikes(eventId: String, likedBy: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from(TABLE_NAME).update(
                LiveStudioLikePatch(likedBy = likedBy, likesCount = likedBy.size)
            ) {
                filter { eq("id", eventId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "updateLikes error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updatePollOptions(eventId: String, options: List<LiveStudioPollOption>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from(TABLE_NAME).update(
                LiveStudioPollPatch(pollOptions = options)
            ) {
                filter { eq("id", eventId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "updatePollOptions error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String, authorUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from(TABLE_NAME).delete {
                filter {
                    eq("id", eventId)
                    eq("author_uid", authorUid)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "deleteEvent error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun broadcastReaction(reaction: LivePulseReaction) = withContext(Dispatchers.IO) {
        val channel = activeChannel ?: return@withContext
        runCatching {
            channel.broadcast(event = "pulse_reaction", message = reaction)
        }
    }
}
