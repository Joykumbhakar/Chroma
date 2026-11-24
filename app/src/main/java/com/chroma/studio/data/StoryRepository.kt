package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.Story
import com.chroma.studio.model.StoryHighlight
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class StoryRepository {
    companion object {
        const val STORY_LIFETIME_MS = 86_400_000L

        /**
         * Determines whether a story should be shown in the active 24-hour story bar.
         * Highlights never extend a story's 24-hour feed lifetime: even if a legacy story
         * document had `expiresAt = Long.MAX_VALUE`, its active feed lifetime is still
         * bounded by `timestamp + 24h`.
         */
        fun isStoryActiveInFeed(story: Story, now: Long = System.currentTimeMillis()): Boolean {
            val cutoff24h = if (story.timestamp > 0L) story.timestamp + STORY_LIFETIME_MS else story.expiresAt
            val effectiveExpiresAt = when {
                story.expiresAt <= 0L -> cutoff24h
                story.timestamp > 0L && story.expiresAt > cutoff24h -> cutoff24h
                else -> story.expiresAt
            }
            return effectiveExpiresAt > now
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storiesCol = db.collection("stories")
    private val usersCol = db.collection("users")

    /**
     * Updates highlight metadata on a story document without ever extending `expiresAt`.
     * Also heals legacy corrupted `expiresAt` values (> timestamp + 24h) back to the true
     * 24h expiration timestamp so old stories never resurrect in the active story bar.
     */
    private suspend fun syncStoryHighlightState(
        storyId: String,
        isHighlight: Boolean,
        highlightTitle: String? = null,
        highlightCoverUrl: String? = null
    ) {
        try {
            val ref = storiesCol.document(storyId)
            val snap = ref.get().await()
            if (!snap.exists()) return
            val ts = snap.getLong("timestamp") ?: 0L
            val exp = snap.getLong("expiresAt") ?: 0L
            val updates = mutableMapOf<String, Any>("isHighlight" to isHighlight)
            if (highlightTitle != null) {
                updates["highlightTitle"] = highlightTitle
            }
            if (highlightCoverUrl != null) {
                updates["highlightCoverUrl"] = highlightCoverUrl
            }
            if (ts > 0L && exp > ts + STORY_LIFETIME_MS) {
                updates["expiresAt"] = ts + STORY_LIFETIME_MS
            }
            ref.update(updates).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("StoryRepository", "Failed to sync highlight state for story $storyId", e)
        }
    }

    /** Appends a newly published story to a matching highlight album or creates a new one. */
    private suspend fun appendStoryToHighlightAlbumByTitle(
        userId: String,
        storyId: String,
        cleanTitle: String,
        coverUrl: String,
        now: Long
    ) {
        val highlightsCol = usersCol.document(userId).collection("highlights")
        val existingSnap = highlightsCol.whereEqualTo("title", cleanTitle).limit(1).get().await()
        val existingDoc = existingSnap.documents.firstOrNull()
        if (existingDoc != null) {
            val updates = mutableMapOf<String, Any>(
                "storyIds" to FieldValue.arrayUnion(storyId),
                "updatedAt" to now
            )
            if ((existingDoc.getString("coverUrl") ?: "").isBlank() && coverUrl.isNotBlank()) {
                updates["coverUrl"] = coverUrl
            }
            existingDoc.reference.update(updates).await()
        } else {
            val hlDocId = "hl_${cleanTitle.lowercase().replace("\\s+".toRegex(), "_")}"
            val hlRef = highlightsCol.document(hlDocId)
            val snap = hlRef.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "storyIds" to FieldValue.arrayUnion(storyId),
                    "updatedAt" to now
                )
                if ((snap.getString("coverUrl") ?: "").isBlank() && coverUrl.isNotBlank()) {
                    updates["coverUrl"] = coverUrl
                }
                hlRef.update(updates).await()
            } else {
                hlRef.set(
                    mapOf(
                        "id" to hlDocId,
                        "userId" to userId,
                        "title" to cleanTitle,
                        "emoji" to "✨",
                        "coverUrl" to coverUrl,
                        "gradientIndex" to 0,
                        "storyIds" to listOf(storyId),
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                ).await()
            }
        }
    }

    /** Publish a new story for the current user. Returns the story ID. */
    suspend fun publishStory(
        mediaUrl: String,
        audienceType: String = "public",
        customAudienceIds: List<String> = emptyList(),
        textContent: String = "",
        textFormat: String = "",
        backgroundColor: String = "",
        isHighlight: Boolean = false,
        highlightTitle: String = "",
        highlightCoverUrl: String = ""
    ): String {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val docRef = storiesCol.document()
        val now = System.currentTimeMillis()
        // All stories expire after 24 hrs — highlights fetch stories by ID separately
        val expiryTime = now + 86_400_000L

        val story = Story(
            id = docRef.id,
            userId = user.uid,
            mediaUrl = mediaUrl,
            textContent = textContent,
            textFormat = if (textFormat.isNotBlank()) com.chroma.studio.utils.DataCompressionUtils.compressText(textFormat) else "",
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = customAudienceIds,
            timestamp = now,
            expiresAt = expiryTime,
            isHighlight = isHighlight,
            highlightTitle = highlightTitle,
            highlightCoverUrl = if (highlightCoverUrl.isNotBlank()) highlightCoverUrl else mediaUrl
        )
        docRef.set(story).await()

        if (isHighlight && highlightTitle.isNotBlank()) {
            try {
                val cleanTitle = highlightTitle.trim()
                val effectiveCover = if (highlightCoverUrl.isNotBlank()) highlightCoverUrl else mediaUrl
                appendStoryToHighlightAlbumByTitle(
                    userId = user.uid,
                    storyId = docRef.id,
                    cleanTitle = cleanTitle,
                    coverUrl = effectiveCover,
                    now = now
                )
            } catch (e: Exception) {
                Log.w("StoryRepository", "Failed to register story to highlights collection", e)
            }
        }

        return docRef.id
    }

    suspend fun publishTextStory(
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        audienceType: String,
        customAudienceIds: List<String>,
        isHighlight: Boolean = false,
        highlightTitle: String = ""
    ): String {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val docRef = storiesCol.document()
        val now = System.currentTimeMillis()
        // All stories expire after 24 hrs — highlights fetch stories by ID separately
        val expiryTime = now + 86_400_000L

        val story = Story(
            id = docRef.id,
            userId = user.uid,
            mediaUrl = "text:story",
            textContent = textContent,
            textFormat = com.chroma.studio.utils.DataCompressionUtils.compressText(textFormat),
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = customAudienceIds,
            timestamp = now,
            expiresAt = expiryTime,
            isHighlight = isHighlight,
            highlightTitle = highlightTitle
        )
        docRef.set(story).await()

        if (isHighlight && highlightTitle.isNotBlank()) {
            try {
                val cleanTitle = highlightTitle.trim()
                appendStoryToHighlightAlbumByTitle(
                    userId = user.uid,
                    storyId = docRef.id,
                    cleanTitle = cleanTitle,
                    coverUrl = "",
                    now = now
                )
            } catch (e: Exception) {
                Log.w("StoryRepository", "Failed to register text story to highlights", e)
            }
        }

        return docRef.id
    }

    /** Fetch non-expired stories for a list of userIds, grouped by user. */
    suspend fun fetchStoriesForUsers(userIds: List<String>): List<UserStoryGroup> {
        if (userIds.isEmpty()) return emptyList()
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        val now = System.currentTimeMillis()
        return try {
            val batches = userIds.chunked(30)
            val allStories = mutableListOf<Story>()
            for (batch in batches) {
                val snap = storiesCol
                    .whereIn("userId", batch)
                    .get().await()
                snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) }
                    .filter { isStoryActiveInFeed(it, now) }
                    .filter { story ->
                        story.userId == currentUid ||
                        story.audienceType == "public" ||
                        (story.audienceType == "only_friends" && story.customAudienceIds.contains(currentUid))
                    }
                    .let { allStories.addAll(it) }
            }
            val userIdsWithStories = allStories.map { it.userId }.distinct()
            val profiles = mutableMapOf<String, UserProfile>()
            for (uid in userIdsWithStories) {
                usersCol.document(uid).get().await()
                    .toObject(UserProfile::class.java)?.let { profiles[uid] = it }
            }
            userIdsWithStories.mapNotNull { uid ->
                val profile = profiles[uid] ?: return@mapNotNull null
                val userStories = allStories.filter { it.userId == uid }.sortedBy { it.timestamp }
                val allSeen = userStories.all { it.viewedBy.contains(currentUid) }
                UserStoryGroup(userId = uid, userProfile = profile, stories = userStories, allSeen = allSeen)
            }.sortedBy { it.allSeen }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "fetchStoriesForUsers error", e)
            emptyList()
        }
    }

    /** Fetch active public stories from the community. */
    suspend fun fetchRecentPublicStories(limit: Long = 20): List<UserStoryGroup> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        val now = System.currentTimeMillis()
        return try {
            val snap = storiesCol
                .limit(limit)
                .get().await()
            val allStories = snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) }
                .filter { isStoryActiveInFeed(it, now) }
                .filter { story ->
                    story.userId == currentUid ||
                    story.audienceType == "public" ||
                    (story.audienceType == "only_friends" && story.customAudienceIds.contains(currentUid))
                }
            val userIdsWithStories = allStories.map { it.userId }.distinct()
            val profiles = mutableMapOf<String, UserProfile>()
            for (uid in userIdsWithStories) {
                usersCol.document(uid).get().await()
                    .toObject(UserProfile::class.java)?.let { profiles[uid] = it }
            }
            userIdsWithStories.mapNotNull { uid ->
                val profile = profiles[uid] ?: return@mapNotNull null
                val userStories = allStories.filter { it.userId == uid }.sortedBy { it.timestamp }
                val allSeen = userStories.all { it.viewedBy.contains(currentUid) }
                UserStoryGroup(userId = uid, userProfile = profile, stories = userStories, allSeen = allSeen)
            }.sortedBy { it.allSeen }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "fetchRecentPublicStories error", e)
            emptyList()
        }
    }

    /** Fetch only the current user's own active stories. */
    suspend fun fetchMyStories(): List<Story> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val now = System.currentTimeMillis()
        return try {
            val snap = storiesCol
                .whereEqualTo("userId", uid)
                .get().await()
            snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) }
                .filter { isStoryActiveInFeed(it, now) }
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /** Mark a story as viewed by the current user. */
    suspend fun markViewed(storyId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            storiesCol.document(storyId).update(
                "viewedBy", FieldValue.arrayUnion(uid),
                "views", FieldValue.increment(1)
            ).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "markViewed error", e)
        }
    }

    /**
     * Sets the like state for a story idempotently.
     * Uses atomic FieldValue operations compatible with offline queuing.
     * Returns true if the remote write succeeded, false if offline/error.
     */
    suspend fun setStoryLike(storyId: String, targetLiked: Boolean): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (storyId.isBlank()) return false
        return try {
            val docRef = storiesCol.document(storyId)
            if (targetLiked) {
                val updates = mapOf(
                    "likedBy" to FieldValue.arrayUnion(uid),
                    "likes" to FieldValue.increment(1)
                )
                docRef.update(updates).await()

                // Dispatch notification to story author
                try {
                    val storyDoc = docRef.get().await()
                    val authorId = storyDoc.getString("userId")
                    val mediaUrl = storyDoc.getString("mediaUrl")
                    if (!authorId.isNullOrBlank() && authorId != uid) {
                        NotificationDispatcher.sendNotification(
                            targetUserId = authorId,
                            type = com.chroma.studio.model.NotificationActionType.STORY_LIKE,
                            actionText = "liked your story",
                            targetPreviewUrl = mediaUrl,
                            targetId = storyId
                        )
                    }
                } catch (e: Exception) {
                    Log.w("StoryRepository", "Failed to dispatch story like notification", e)
                }
            } else {
                val updates = mapOf(
                    "likedBy" to FieldValue.arrayRemove(uid),
                    "likes" to FieldValue.increment(-1)
                )
                docRef.update(updates).await()
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "setStoryLike error", e)
            false
        }
    }

    /** Toggle like on a story. Returns the new liked state. */
    suspend fun toggleLike(storyId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (storyId.isBlank()) return false
        return try {
            val docRef = storiesCol.document(storyId)
            val snapshot = docRef.get().await()
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            val currentlyLiked = likedBy.contains(uid)
            val targetLiked = !currentlyLiked
            setStoryLike(storyId, targetLiked)
            targetLiked
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    /** Delete a story (owner only) and remove it from any highlight albums. */
    suspend fun deleteStory(storyId: String) {
        val uid = auth.currentUser?.uid
        try {
            storiesCol.document(storyId).delete().await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
        if (!uid.isNullOrBlank()) {
            try {
                val hlSnap = usersCol.document(uid).collection("highlights")
                    .whereArrayContains("storyIds", storyId)
                    .get().await()
                hlSnap.documents.forEach { hlDoc ->
                    hlDoc.reference.update("storyIds", FieldValue.arrayRemove(storyId)).await()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("StoryRepository", "Failed removing deleted story $storyId from highlights", e)
            }
        }
    }

    /** Get IDs of users the current user follows. */
    suspend fun getFollowingIds(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val localIds = com.chroma.studio.data.mutation.FollowingLocalStorageManager.getFollowingIds(null, uid)
        if (localIds.isNotEmpty()) {
            return localIds.toList()
        }
        return try {
            val serverIds = usersCol.document(uid).collection("following").get().await()
                .documents.map { it.id }
            if (serverIds.isNotEmpty()) {
                com.chroma.studio.data.mutation.FollowingLocalStorageManager.saveFollowingIds(null, uid, serverIds.toSet())
            }
            serverIds
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /** Get user IDs on the current user's Only Friend close friends list. */
    suspend fun getOnlyFriends(userId: String): List<String> {
        return try {
            val doc = usersCol.document(userId).get().await()
            (doc.get("onlyFriends") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    /** Save updated Only Friend list for the given user. */
    suspend fun saveOnlyFriends(userId: String, onlyFriends: List<String>) {
        try {
            usersCol.document(userId).update("onlyFriends", onlyFriends).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "saveOnlyFriends error", e)
        }
    }

    /**
     * Fetch a story and its parent story group by story ID.
     * Enforces the 24-hour freshness rule (expiresAt > now || (now - timestamp) < 24h).
     * Returns null if the story does not exist or has expired.
     */
    suspend fun fetchStoryGroupForStory(storyId: String): Pair<UserStoryGroup, Story>? {
        if (storyId.isBlank()) return null
        return try {
            val storyDoc = storiesCol.document(storyId).get().await()
            if (!storyDoc.exists()) return null

            val story = storyDoc.toObject(Story::class.java)?.copy(id = storyDoc.id) ?: return null
            val now = System.currentTimeMillis()
            val isAlive = isStoryActiveInFeed(story, now)
            if (!isAlive) return null

            val userDoc = usersCol.document(story.userId).get().await()
            val userProfile = userDoc.toObject(UserProfile::class.java)?.copy(uid = story.userId)
                ?: UserProfile(uid = story.userId, displayName = "Story Creator")

            val activeStoriesSnapshot = try {
                val cutoff = now - 86_400_000L
                storiesCol.whereEqualTo("userId", story.userId)
                    .whereGreaterThan("timestamp", cutoff)
                    .get().await()
            } catch (_: Exception) { null }

            val groupStories = if (activeStoriesSnapshot != null && !activeStoriesSnapshot.isEmpty) {
                activeStoriesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Story::class.java)?.copy(id = doc.id)
                }.sortedBy { it.timestamp }
            } else {
                listOf(story)
            }

            val group = UserStoryGroup(
                userId = story.userId,
                userProfile = userProfile,
                stories = if (groupStories.any { it.id == story.id }) groupStories else groupStories + story,
                allSeen = false
            )
            Pair(group, story)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching story for $storyId", e)
            null
        }
    }

    /** Save an existing story to user highlights (never extends 24h story bar expiry). */
    suspend fun saveStoryToHighlight(
        storyId: String,
        highlightTitle: String,
        emoji: String = "✨",
        coverUrl: String? = null
    ): Boolean {
        val user = auth.currentUser ?: return false
        val now = System.currentTimeMillis()
        return try {
            val storyDoc = storiesCol.document(storyId).get().await()
            if (!storyDoc.exists()) return false

            val media = storyDoc.getString("mediaUrl") ?: ""
            val chosenCover = if (!coverUrl.isNullOrBlank()) coverUrl else media.takeIf { it != "text:story" } ?: ""
            val cleanTitle = highlightTitle.trim().ifBlank { "Highlights" }

            syncStoryHighlightState(
                storyId = storyId,
                isHighlight = true,
                highlightTitle = cleanTitle,
                highlightCoverUrl = chosenCover
            )

            // Save or append to user's highlights subcollection
            appendStoryToHighlightAlbumByTitle(
                userId = user.uid,
                storyId = storyId,
                cleanTitle = cleanTitle,
                coverUrl = chosenCover,
                now = now
            )

            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error saving story to highlight", e)
            false
        }
    }

    /** Fetch all highlights for a user sorted by updatedAt descending. */
    suspend fun fetchUserHighlights(userId: String): List<StoryHighlight> {
        return try {
            val snap = usersCol.document(userId).collection("highlights").get().await()
            snap.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: "Highlights"
                val coverUrl = doc.getString("coverUrl") ?: ""
                @Suppress("UNCHECKED_CAST")
                val storyIds = (doc.get("storyIds") as? List<String>) ?: emptyList()
                val createdAt = doc.getLong("createdAt") ?: 0L
                val updatedAt = doc.getLong("updatedAt") ?: createdAt
                StoryHighlight(
                    id = id,
                    userId = userId,
                    title = title,
                    coverUrl = coverUrl,
                    storyIds = storyIds,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching user highlights", e)
            emptyList()
        }
    }

    /** Create a new highlight album with a list of stories. */
    suspend fun createHighlight(
        title: String,
        coverUrl: String,
        storyIds: List<String>
    ): StoryHighlight? {
        val user = auth.currentUser ?: return null
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "Highlights" }
        val docRef = usersCol.document(user.uid).collection("highlights").document()
        val chosenCover = coverUrl.trim().ifBlank { "" }

        return try {
            // Mark included stories as belonging to a highlight without ever extending expiresAt
            storyIds.forEach { sId ->
                syncStoryHighlightState(
                    storyId = sId,
                    isHighlight = true,
                    highlightTitle = cleanTitle
                )
            }

            val highlight = StoryHighlight(
                id = docRef.id,
                userId = user.uid,
                title = cleanTitle,
                coverUrl = chosenCover,
                storyIds = storyIds,
                createdAt = now,
                updatedAt = now
            )

            docRef.set(
                mapOf(
                    "id" to highlight.id,
                    "userId" to highlight.userId,
                    "title" to highlight.title,
                    "emoji" to "✨",
                    "coverUrl" to highlight.coverUrl,
                    "storyIds" to highlight.storyIds,
                    "createdAt" to highlight.createdAt,
                    "updatedAt" to highlight.updatedAt
                )
            ).await()

            highlight
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error creating highlight", e)
            null
        }
    }

    /** Add a story to an existing highlight album. */
    suspend fun addStoryToHighlight(highlightId: String, storyId: String): Boolean {
        val user = auth.currentUser ?: return false
        val now = System.currentTimeMillis()
        return try {
            // Update highlight document
            val hlRef = usersCol.document(user.uid).collection("highlights").document(highlightId)
            val docSnap = hlRef.get().await()
            val currentCover = docSnap.getString("coverUrl") ?: ""
            val hlTitle = docSnap.getString("title")

            // Mark story as belonging to a highlight without modifying expiresAt
            val storyDoc = storiesCol.document(storyId).get().await()
            val mediaUrl = storyDoc.getString("mediaUrl") ?: ""
            syncStoryHighlightState(
                storyId = storyId,
                isHighlight = true,
                highlightTitle = hlTitle
            )

            val updates = mutableMapOf<String, Any>(
                "storyIds" to FieldValue.arrayUnion(storyId),
                "updatedAt" to now
            )
            if (currentCover.isBlank() && mediaUrl.isNotBlank() && mediaUrl != "text:story") {
                updates["coverUrl"] = mediaUrl
            }

            hlRef.update(updates).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error adding story to highlight", e)
            false
        }
    }

    /** Update highlight title, cover image, and story IDs list. */
    suspend fun updateHighlight(
        highlightId: String,
        title: String,
        coverUrl: String,
        storyIds: List<String>
    ): Boolean {
        val user = auth.currentUser ?: return false
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "Highlights" }
        return try {
            val hlRef = usersCol.document(user.uid).collection("highlights").document(highlightId)
            @Suppress("UNCHECKED_CAST")
            val oldStoryIds = (hlRef.get().await().get("storyIds") as? List<String>) ?: emptyList()

            hlRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "coverUrl" to coverUrl,
                    "storyIds" to storyIds,
                    "updatedAt" to now
                )
            ).await()

            // Ensure included stories are flagged as highlights without changing expiresAt
            storyIds.forEach { sId ->
                syncStoryHighlightState(
                    storyId = sId,
                    isHighlight = true,
                    highlightTitle = cleanTitle
                )
            }

            // Clear highlight flag from stories removed from this album if not in any other album
            val removedIds = oldStoryIds.filterNot { it in storyIds }
            if (removedIds.isNotEmpty()) {
                val allHighlights = fetchUserHighlights(user.uid)
                val remainingHighlightedIds = allHighlights.flatMap { it.storyIds }.toSet()
                removedIds.filterNot { it in remainingHighlightedIds }.forEach { orphanId ->
                    syncStoryHighlightState(
                        storyId = orphanId,
                        isHighlight = false,
                        highlightTitle = ""
                    )
                }
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error updating highlight", e)
            false
        }
    }

    /** Delete a highlight album (does not delete stories from user archive). */
    suspend fun deleteHighlight(highlightId: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val hlRef = usersCol.document(user.uid).collection("highlights").document(highlightId)
            @Suppress("UNCHECKED_CAST")
            val removedStoryIds = (hlRef.get().await().get("storyIds") as? List<String>) ?: emptyList()
            hlRef.delete().await()

            if (removedStoryIds.isNotEmpty()) {
                val allHighlights = fetchUserHighlights(user.uid)
                val remainingHighlightedIds = allHighlights.flatMap { it.storyIds }.toSet()
                removedStoryIds.filterNot { it in remainingHighlightedIds }.forEach { orphanId ->
                    syncStoryHighlightState(
                        storyId = orphanId,
                        isHighlight = false,
                        highlightTitle = ""
                    )
                }
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error deleting highlight", e)
            false
        }
    }

    /** Fetch full Story objects by list of story IDs in preserved order. */
    suspend fun fetchStoriesByIds(storyIds: List<String>): List<Story> {
        if (storyIds.isEmpty()) return emptyList()
        return try {
            val result = mutableListOf<Story>()
            storyIds.chunked(30).forEach { chunk ->
                val snap = storiesCol.whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get().await()
                result.addAll(snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) })
            }
            val map = result.associateBy { it.id }
            storyIds.mapNotNull { map[it] }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching stories by IDs", e)
            emptyList()
        }
    }

    /** Fetch all stories (active & past archive) published by a user. */
    suspend fun fetchUserStoryArchive(userId: String): List<Story> {
        return try {
            val snap = storiesCol.whereEqualTo("userId", userId).get().await()
            snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching user story archive", e)
            emptyList()
        }
    }

    /** Fetch all permanent stories belonging to a user's highlight reel (live forever). */
    suspend fun fetchHighlightStories(userId: String, highlightTitle: String? = null): List<Story> {
        return try {
            var query: Query = storiesCol.whereEqualTo("userId", userId).whereEqualTo("isHighlight", true)
            if (!highlightTitle.isNullOrBlank()) {
                query = query.whereEqualTo("highlightTitle", highlightTitle)
            }
            val snap = query.get().await()
            snap.documents.mapNotNull { doc -> doc.toObject(Story::class.java)?.copy(id = doc.id) }
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching highlight stories", e)
            emptyList()
        }
    }
}
