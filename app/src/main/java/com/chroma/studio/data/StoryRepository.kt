package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
        customAudienceIds: List<String> = emptyList()
    ): String {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val docRef = storiesCol.document()
        val now = System.currentTimeMillis()
        val story = Story(
            id = docRef.id,
            userId = user.uid,
            mediaUrl = mediaUrl,
            audienceType = audienceType,
            customAudienceIds = customAudienceIds,
            timestamp = now,
            expiresAt = now + 86_400_000L
        )
        docRef.set(story).await()
        return docRef.id
    }

    suspend fun publishTextStory(
        textContent: String,
        textFormat: String,
        backgroundColor: String,
        audienceType: String,
        customAudienceIds: List<String>
    ): String {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val docRef = storiesCol.document()
        val now = System.currentTimeMillis()
        val story = Story(
            id = docRef.id,
            userId = user.uid,
            mediaUrl = "text:story",
            textContent = textContent,
            textFormat = textFormat,
            backgroundColor = backgroundColor,
            audienceType = audienceType,
            customAudienceIds = customAudienceIds,
            timestamp = now,
            expiresAt = now + 86_400_000L
        )
        docRef.set(story).await()
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

    /** Toggle like on a story. Returns the new liked state. */
    suspend fun toggleLike(storyId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val docRef = storiesCol.document(storyId)
            val isNowLiked = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val currentLikes = snapshot.getLong("likes") ?: 0L
                val alreadyLiked = likedBy.contains(uid)
                
                if (alreadyLiked) {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(uid),
                        "likes", maxOf(0, currentLikes - 1)
                    )
                    false
                } else {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayUnion(uid),
                        "likes", currentLikes + 1
                    )
                    true
                }
            }.await()

            if (isNowLiked) {
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
            }
            isNowLiked
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
        return try {
            usersCol.document(uid).collection("following").get().await()
                .documents.map { it.id }
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
}
