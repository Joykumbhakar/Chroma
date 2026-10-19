package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.model.FlashVideo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FlashRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("flash_videos")
    private val usersCollection = db.collection("users")

    // ── Feed ────────────────────────────────────────────────────────────────

    /** Paginated feed ordered by timestamp DESC. Filters archived posts. */
    suspend fun getFlashFeed(limit: Long = 12, lastTimestamp: Long? = null): List<FlashVideo> {
        return try {
            var query = collection
                .whereEqualTo("archived", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }
            val snapshot = query.get().await()
            snapshot.toObjects(FlashVideo::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "Error fetching flash feed", e)
            emptyList()
        }
    }

    /** Real-time Flow of the latest 30 flashes — used for live updates. */
    fun listenToFlashFeed(): Flow<List<FlashVideo>> = callbackFlow {
        val listener = collection
            .whereEqualTo("archived", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FlashRepository", "Snapshot error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(FlashVideo::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // ── Publish ─────────────────────────────────────────────────────────────

    suspend fun publishFlash(
        videoUrl: String,
        thumbnailUrl: String,
        caption: String,
        hashtags: List<String>,
        mentions: List<String>,
        duration: Long,
        remixedFromPostId: String? = null,
        remixedFromAuthorUsername: String? = null,
        hideLikesCount: Boolean = false,
        turnOffComments: Boolean = false,
        turnOffRemix: Boolean = false
    ): String {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userDoc = usersCollection.document(user.uid).get().await()
        val username = userDoc.getString("username") ?: ""
        val displayName = userDoc.getString("displayName") ?: ""
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
        val verified = userDoc.getBoolean("verified") ?: false

        val docRef = collection.document()
        val flash = FlashVideo(
            id = docRef.id,
            creatorId = user.uid,
            authorUsername = username,
            authorDisplayName = displayName,
            authorAvatarUrl = avatarUrl,
            authorVerified = verified,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            caption = caption,
            hashtags = hashtags,
            mentions = mentions,
            duration = duration.coerceAtMost(30_000L),
            remixedFromPostId = remixedFromPostId,
            remixedFromAuthorUsername = remixedFromAuthorUsername,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            hideLikesCount = hideLikesCount,
            turnOffComments = turnOffComments,
            turnOffRemix = turnOffRemix
        )
        docRef.set(flash).await()
        return docRef.id
    }

    // ── Interactions ────────────────────────────────────────────────────────

    suspend fun setFlashLike(videoId: String, userId: String, targetLiked: Boolean): Boolean {
        return try {
            val docRef = collection.document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val currentLikes = snapshot.getLong("likesCount") ?: 0L
                val alreadyLiked = likedBy.contains(userId)

                if (alreadyLiked == targetLiked) {
                    return@runTransaction true // idempotent success
                }

                if (targetLiked) {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayUnion(userId),
                        "likesCount", currentLikes + 1
                    )
                } else {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", maxOf(0, currentLikes - 1)
                    )
                }
                true
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setFlashLike error", e)
            false
        }
    }

    suspend fun toggleLike(videoId: String, userId: String, forceLike: Boolean = false): Boolean {
        return try {
            val docRef = collection.document(videoId)
            val isLiked = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val alreadyLiked = likedBy.contains(userId)
                if (forceLike && alreadyLiked) true else !alreadyLiked
            }.await()
            setFlashLike(videoId, userId, isLiked)
            isLiked
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "toggleLike error", e)
            false
        }
    }

    suspend fun incrementShare(videoId: String, userId: String) {
        try {
            val docRef = collection.document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val sharedBy = snapshot.get("sharedBy") as? List<String> ?: emptyList()
                val currentShares = snapshot.getLong("shareCount") ?: 0L
                
                if (!sharedBy.contains(userId)) {
                    transaction.update(
                        docRef,
                        "sharedBy", FieldValue.arrayUnion(userId),
                        "shareCount", currentShares + 1
                    )
                }
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "incrementShare error", e)
        }
    }

    suspend fun incrementRemix(videoId: String, userId: String) {
        try {
            val docRef = collection.document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val remixedBy = snapshot.get("remixedBy") as? List<String> ?: emptyList()
                val currentRemixes = snapshot.getLong("remixCount") ?: 0L
                
                if (!remixedBy.contains(userId)) {
                    transaction.update(
                        docRef,
                        "remixedBy", FieldValue.arrayUnion(userId),
                        "remixCount", currentRemixes + 1
                    )
                }
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    suspend fun setFlashSave(videoId: String, userId: String, targetSaved: Boolean): Boolean {
        return try {
            val docRef = collection.document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val savedBy = snapshot.get("savedBy") as? List<String> ?: emptyList()
                val currentSaves = snapshot.getLong("saveCount") ?: 0L
                val alreadySaved = savedBy.contains(userId)

                if (alreadySaved == targetSaved) {
                    return@runTransaction true // idempotent success
                }

                if (targetSaved) {
                    transaction.update(
                        docRef,
                        "savedBy", FieldValue.arrayUnion(userId),
                        "saveCount", currentSaves + 1
                    )
                } else {
                    transaction.update(
                        docRef,
                        "savedBy", FieldValue.arrayRemove(userId),
                        "saveCount", maxOf(0, currentSaves - 1)
                    )
                }
                true
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setFlashSave error", e)
            false
        }
    }

    suspend fun toggleSave(videoId: String, userId: String): Boolean {
        return try {
            val docRef = collection.document(videoId)
            val isSaved = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val savedBy = snapshot.get("savedBy") as? List<String> ?: emptyList()
                val alreadySaved = savedBy.contains(userId)
                !alreadySaved
            }.await()
            setFlashSave(videoId, userId, isSaved)
            isSaved
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "toggleSave error", e)
            false
        }
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    suspend fun getComments(videoId: String): List<com.chroma.studio.model.FlashComment> {
        return try {
            val snapshot = db.collection("flash_comments")
                .whereEqualTo("videoId", videoId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            snapshot.toObjects(com.chroma.studio.model.FlashComment::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "Error fetching comments", e)
            emptyList()
        }
    }

    suspend fun addComment(
        videoId: String,
        text: String,
        replyToCommentId: String? = null,
        replyFlashVideoUrl: String? = null,
        preGeneratedId: String? = null
    ): com.chroma.studio.model.FlashComment {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userDoc = usersCollection.document(user.uid).get().await()
        val username = userDoc.getString("username") ?: ""
        val displayName = userDoc.getString("displayName") ?: ""
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
        val verified = userDoc.getBoolean("verified") ?: false

        val docRef = if (!preGeneratedId.isNullOrBlank()) {
            db.collection("flash_comments").document(preGeneratedId)
        } else {
            db.collection("flash_comments").document()
        }
        val comment = com.chroma.studio.model.FlashComment(
            id = docRef.id,
            videoId = videoId,
            userId = user.uid,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            verified = verified,
            text = text,
            timestamp = System.currentTimeMillis(),
            replyToCommentId = replyToCommentId,
            replyFlashVideoUrl = replyFlashVideoUrl
        )
        docRef.set(comment).await()
        
        // Increment comment count
        try {
            collection.document(videoId).update("commentCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("FlashRepository", "Error incrementing comment count", e)
        }
        
        return comment
    }

    suspend fun setCommentLike(commentId: String, userId: String, targetLiked: Boolean): Boolean {
        return try {
            val docRef = db.collection("flash_comments").document(commentId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val currentLikes = snapshot.getLong("likesCount") ?: 0L
                val alreadyLiked = likedBy.contains(userId)

                if (alreadyLiked == targetLiked) {
                    return@runTransaction true // idempotent success
                }

                if (targetLiked) {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayUnion(userId),
                        "likesCount", currentLikes + 1
                    )
                } else {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", maxOf(0, currentLikes - 1)
                    )
                }
                true
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentLike error", e)
            false
        }
    }

    suspend fun toggleCommentLike(commentId: String, userId: String): Boolean {
        return try {
            val docRef = db.collection("flash_comments").document(commentId)
            val isLiked = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val alreadyLiked = likedBy.contains(userId)
                !alreadyLiked
            }.await()
            setCommentLike(commentId, userId, isLiked)
            isLiked
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "toggleCommentLike error", e)
            false
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteFlash(videoId: String): Boolean {
        return try {
            collection.document(videoId).delete().await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "deleteFlash error", e)
            false
        }
    }

    // ── User feed ────────────────────────────────────────────────────────────

    suspend fun getFlashesByUser(userId: String): List<FlashVideo> {
        return try {
            val snapshot = collection
                .whereEqualTo("creatorId", userId)
                .get().await()
            snapshot.toObjects(FlashVideo::class.java)
                .filter { !it.isArchived }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }
}
