package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class StoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storiesCol = db.collection("stories")
    private val usersCol = db.collection("users")

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
        // Highlighted stories live forever (Long.MAX_VALUE), regular stories expire after 24 hrs
        val expiryTime = if (isHighlight) Long.MAX_VALUE else now + 86_400_000L

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
                val hlDocId = "hl_${cleanTitle.lowercase().replace("\\s+".toRegex(), "_")}"
                usersCol.document(user.uid).collection("highlights").document(hlDocId).set(
                    mapOf(
                        "id" to hlDocId,
                        "title" to cleanTitle,
                        "emoji" to "✨",
                        "coverUrl" to (if (highlightCoverUrl.isNotBlank()) highlightCoverUrl else mediaUrl),
                        "gradientIndex" to 0,
                        "storyIds" to FieldValue.arrayUnion(docRef.id),
                        "createdAt" to now
                    ),
                    SetOptions.merge()
                ).await()
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
        // Highlighted stories live forever (Long.MAX_VALUE), regular stories expire after 24 hrs
        val expiryTime = if (isHighlight) Long.MAX_VALUE else now + 86_400_000L

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
                val hlDocId = "hl_${cleanTitle.lowercase().replace("\\s+".toRegex(), "_")}"
                usersCol.document(user.uid).collection("highlights").document(hlDocId).set(
                    mapOf(
                        "id" to hlDocId,
                        "title" to cleanTitle,
                        "emoji" to "✨",
                        "coverUrl" to "",
                        "gradientIndex" to 0,
                        "storyIds" to FieldValue.arrayUnion(docRef.id),
                        "createdAt" to now
                    ),
                    SetOptions.merge()
                ).await()
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
                snap.documents.mapNotNull { it.toObject(Story::class.java) }
                    .filter { it.expiresAt > now }
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
            val allStories = snap.documents.mapNotNull { it.toObject(Story::class.java) }
                .filter { it.expiresAt > now }
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
            snap.toObjects(Story::class.java).filter { it.expiresAt > now }.sortedBy { it.timestamp }
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

    /** Delete a story (owner only). */
    suspend fun deleteStory(storyId: String) {
        try { storiesCol.document(storyId).delete().await() }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
            val isAlive = story.isHighlight || (if (story.expiresAt > 0L) story.expiresAt > now else (now - story.timestamp) < 86_400_000L)
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

    /** Save an existing story to user highlights so it lives forever (never expires). */
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
            val chosenCover = if (!coverUrl.isNullOrBlank()) coverUrl else media
            val cleanTitle = highlightTitle.trim().ifBlank { "Highlights" }

            // Make the story immortal by setting expiresAt to Long.MAX_VALUE
            storiesCol.document(storyId).update(
                mapOf(
                    "isHighlight" to true,
                    "highlightTitle" to cleanTitle,
                    "highlightCoverUrl" to chosenCover,
                    "expiresAt" to Long.MAX_VALUE
                )
            ).await()

            // Save or append to user's highlights subcollection
            val highlightDocId = "hl_${cleanTitle.lowercase().replace("\\s+".toRegex(), "_")}"
            usersCol.document(user.uid).collection("highlights").document(highlightDocId).set(
                mapOf(
                    "id" to highlightDocId,
                    "title" to cleanTitle,
                    "emoji" to emoji,
                    "coverUrl" to chosenCover,
                    "storyIds" to FieldValue.arrayUnion(storyId),
                    "createdAt" to now
                ),
                SetOptions.merge()
            ).await()

            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error saving story to highlight", e)
            false
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
            snap.toObjects(Story::class.java).sortedBy { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("StoryRepository", "Error fetching highlight stories", e)
            emptyList()
        }
    }
}
