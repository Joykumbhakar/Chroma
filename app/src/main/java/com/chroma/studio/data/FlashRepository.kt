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
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }
            val snapshot = query.get().await()
            val items = snapshot.toObjects(FlashVideo::class.java)
            items.filter { !it.isArchived }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "Error fetching flash feed", e)
            emptyList()
        }
    }

    /** Real-time Flow of the latest 30 flashes — used for live updates. */
    fun listenToFlashFeed(): Flow<List<FlashVideo>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FlashRepository", "Snapshot error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(FlashVideo::class.java)?.filter { !it.isArchived } ?: emptyList()
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
            duration = duration.coerceAtMost(60_000L),
            remixedFromPostId = remixedFromPostId,
            remixedFromAuthorUsername = remixedFromAuthorUsername,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            hideLikesCount = hideLikesCount,
            turnOffComments = turnOffComments,
            turnOffRemix = turnOffRemix
        )
        docRef.set(flash).await()

        // Also write to community_works collection so it appears everywhere in Community tabs
        try {
            val communityPost = com.chroma.studio.model.DiscoverPost(
                id = docRef.id,
                creatorId = user.uid,
                authorUsername = username,
                authorDisplayName = displayName,
                authorAvatarUrl = avatarUrl,
                authorVerified = verified,
                title = caption.ifBlank { "Flash Video" },
                description = caption,
                aspectRatio = "9:16",
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                postType = "video",
                tags = hashtags,
                mentions = mentions,
                remixedFromPostId = remixedFromPostId,
                remixedFromAuthorUsername = remixedFromAuthorUsername,
                timestamp = flash.timestamp,
                hideLikesCount = hideLikesCount,
                turnOffComments = turnOffComments,
                turnOffRemix = turnOffRemix
            )
            db.collection("community_works").document(docRef.id).set(communityPost).await()
        } catch (ce: Exception) {
            Log.e("FlashRepository", "Error syncing flash to community_works", ce)
        }

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
                        "dislikedBy", FieldValue.arrayRemove(userId),
                        "likesCount", FieldValue.increment(1)
                    )
                } else {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", if (currentLikes <= 1L) 0L else FieldValue.increment(-1)
                    )
                }
                true
            }.await()

            // Synchronize with community_works and user liked_posts records
            val now = System.currentTimeMillis()
            val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(videoId)
            val flashLikeSubRef = collection.document(videoId).collection("likes").document(userId)

            try {
                val commRef = db.collection("community_works").document(videoId)
                if (targetLiked) {
                    commRef.update(
                        mapOf(
                            "likedBy" to FieldValue.arrayUnion(userId),
                            "likesCount" to FieldValue.increment(1)
                        )
                    ).await()
                } else {
                    commRef.update(
                        mapOf(
                            "likedBy" to FieldValue.arrayRemove(userId),
                            "likesCount" to FieldValue.increment(-1)
                        )
                    ).await()
                }
            } catch (_: Exception) {}

            try {
                if (targetLiked) {
                    userLikeRef.set(mapOf("likedAt" to now)).await()
                    flashLikeSubRef.set(mapOf("timestamp" to now)).await()
                } else {
                    userLikeRef.delete().await()
                    flashLikeSubRef.delete().await()
                }
            } catch (_: Exception) {}

            if (targetLiked) {
                try {
                    val videoDoc = collection.document(videoId).get().await()
                    val creatorId = videoDoc.getString("creatorId")
                    val thumb = videoDoc.getString("thumbnailUrl")
                    if (!creatorId.isNullOrBlank() && creatorId != userId) {
                        NotificationDispatcher.sendNotification(
                            targetUserId = creatorId,
                            type = com.chroma.studio.model.NotificationActionType.LIKE,
                            actionText = "liked your video",
                            targetPreviewUrl = thumb,
                            targetId = videoId
                        )
                    }
                } catch (e: Exception) {
                    Log.w("FlashRepository", "Failed to dispatch video like notification", e)
                }
            }
            true
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

    /** Real-time Flow of comments for a video or community post. */
    fun observeComments(videoId: String): Flow<List<com.chroma.studio.model.FlashComment>> = callbackFlow {
        if (videoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("flash_comments")
            .whereEqualTo("videoId", videoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FlashRepository", "Comments snapshot listener error for $videoId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(com.chroma.studio.model.FlashComment::class.java)
                        .sortedBy { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getComments(videoId: String): List<com.chroma.studio.model.FlashComment> {
        return try {
            val snapshot = db.collection("flash_comments")
                .whereEqualTo("videoId", videoId)
                .get().await()
            snapshot.toObjects(com.chroma.studio.model.FlashComment::class.java)
                .sortedBy { it.timestamp }
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
        val rawUsername = userDoc.getString("username")
        val rawDisplayName = userDoc.getString("displayName")
        val avatarUrl = userDoc.getString("avatarUrl") ?: user.photoUrl?.toString() ?: ""
        val verified = userDoc.getBoolean("verified") ?: false

        val username = if (!rawUsername.isNullOrBlank()) rawUsername else (user.displayName?.replace(" ", "")?.lowercase() ?: "user_${user.uid.take(5)}")
        val displayName = if (!rawDisplayName.isNullOrBlank()) rawDisplayName else (user.displayName ?: username)

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
        
        // Increment comment count on flash_videos
        try {
            collection.document(videoId).update("commentCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("FlashRepository", "Error incrementing flash comment count", e)
        }

        // Increment comment count on community_works
        try {
            db.collection("community_works").document(videoId).update("commentsCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("FlashRepository", "Error incrementing community_works comment count", e)
        }

        // Dispatch real notification to video creator
        try {
            val videoDoc = collection.document(videoId).get().await()
            val creatorId = videoDoc.getString("creatorId")
            val thumb = videoDoc.getString("thumbnailUrl")
            if (!creatorId.isNullOrBlank() && creatorId != user.uid) {
                NotificationDispatcher.sendNotification(
                    targetUserId = creatorId,
                    type = com.chroma.studio.model.NotificationActionType.COMMENT,
                    actionText = "commented on your video",
                    detailText = text,
                    targetPreviewUrl = thumb,
                    targetId = videoId
                )
            }
        } catch (_: Exception) {}

        // Dispatch reply notification if replying to a comment
        if (!replyToCommentId.isNullOrBlank()) {
            try {
                val parentDoc = db.collection("flash_comments").document(replyToCommentId).get().await()
                val parentAuthorId = parentDoc.getString("userId")
                if (!parentAuthorId.isNullOrBlank() && parentAuthorId != user.uid) {
                    val videoDoc = collection.document(videoId).get().await()
                    val thumb = videoDoc.getString("thumbnailUrl")
                    NotificationDispatcher.sendNotification(
                        targetUserId = parentAuthorId,
                        type = com.chroma.studio.model.NotificationActionType.REPLY,
                        actionText = "replied to your comment",
                        detailText = text,
                        targetPreviewUrl = thumb,
                        targetId = videoId
                    )
                }
            } catch (e: Exception) {
                Log.w("FlashRepository", "Failed to dispatch reply notification", e)
            }
        }

        // Dispatch mention notifications for any @username in comment
        try {
            val mentionRegex = Regex("""@([a-zA-Z0-9_.]+)""")
            val matches = mentionRegex.findAll(text).map { it.groupValues[1].lowercase() }.toSet()
            if (matches.isNotEmpty()) {
                val videoDoc = collection.document(videoId).get().await()
                val thumb = videoDoc.getString("thumbnailUrl")
                for (targetUsername in matches) {
                    try {
                        val usernameDoc = db.collection("usernames").document(targetUsername).get().await()
                        val mentionedUid = usernameDoc.getString("uid")
                        if (!mentionedUid.isNullOrBlank() && mentionedUid != user.uid) {
                            NotificationDispatcher.sendNotification(
                                targetUserId = mentionedUid,
                                type = com.chroma.studio.model.NotificationActionType.MENTION,
                                actionText = "mentioned you in a comment",
                                detailText = text,
                                targetPreviewUrl = thumb,
                                targetId = videoId
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        
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
                        "likesCount", FieldValue.increment(1)
                    )
                } else {
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", if (currentLikes <= 1L) 0L else FieldValue.increment(-1)
                    )
                }
                true
            }.await()

            if (targetLiked) {
                try {
                    val commentDoc = docRef.get().await()
                    val authorId = commentDoc.getString("userId")
                    val commentText = commentDoc.getString("text") ?: ""
                    val videoId = commentDoc.getString("videoId") ?: ""
                    if (!authorId.isNullOrBlank() && authorId != userId) {
                        var thumb: String? = null
                        if (videoId.isNotBlank()) {
                            val videoDoc = collection.document(videoId).get().await()
                            thumb = videoDoc.getString("thumbnailUrl")
                        }
                        NotificationDispatcher.sendNotification(
                            targetUserId = authorId,
                            type = com.chroma.studio.model.NotificationActionType.LIKE_COMMENT,
                            actionText = "liked your comment",
                            detailText = commentText,
                            targetPreviewUrl = thumb,
                            targetId = videoId.ifBlank { commentId }
                        )
                    }
                } catch (e: Exception) {
                    Log.w("FlashRepository", "Failed to dispatch comment like notification", e)
                }
            }
            true
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

    suspend fun setCommentDislike(commentId: String, userId: String, targetDisliked: Boolean): Boolean {
        return try {
            val docRef = db.collection("flash_comments").document(commentId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val dislikedBy = snapshot.get("dislikedBy") as? List<String> ?: emptyList()
                val alreadyDisliked = dislikedBy.contains(userId)

                if (alreadyDisliked == targetDisliked) {
                    return@runTransaction true
                }

                if (targetDisliked) {
                    transaction.update(
                        docRef,
                        "dislikedBy", FieldValue.arrayUnion(userId),
                        "likedBy", FieldValue.arrayRemove(userId)
                    )
                } else {
                    transaction.update(
                        docRef,
                        "dislikedBy", FieldValue.arrayRemove(userId)
                    )
                }
                true
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentDislike error", e)
            false
        }
    }

    suspend fun setCommentPinned(commentId: String, videoId: String, isPinned: Boolean): Boolean {
        return try {
            val commentsRef = db.collection("flash_comments")
            if (isPinned) {
                val currentPinned = commentsRef.whereEqualTo("videoId", videoId).whereEqualTo("isPinned", true).get().await()
                val batch = db.batch()
                for (doc in currentPinned.documents) {
                    if (doc.id != commentId) {
                        batch.update(doc.reference, "isPinned", false)
                    }
                }
                batch.update(commentsRef.document(commentId), "isPinned", true)
                batch.commit().await()
            } else {
                commentsRef.document(commentId).update("isPinned", false).await()
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentPinned error", e)
            false
        }
    }

    suspend fun setCommentCreatorHeart(commentId: String, isHearted: Boolean): Boolean {
        return try {
            db.collection("flash_comments").document(commentId).update("isCreatorHearted", isHearted).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentCreatorHeart error", e)
            false
        }
    }

    suspend fun deleteComment(commentId: String, videoId: String): Boolean {
        return try {
            db.collection("flash_comments").document(commentId).delete().await()
            try {
                collection.document(videoId).update("commentCount", FieldValue.increment(-1)).await()
            } catch (_: Exception) {}
            try {
                db.collection("community_works").document(videoId).update("commentsCount", FieldValue.increment(-1)).await()
            } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "deleteComment error", e)
            false
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteFlash(videoId: String): Boolean {
        if (videoId.isBlank()) return false
        val currentUid = auth.currentUser?.uid ?: return false
        var deletedAny = false

        // 1. Delete from flash_videos
        try {
            val flashDoc = collection.document(videoId).get().await()
            if (flashDoc.exists()) {
                val creatorId = flashDoc.getString("creatorId") ?: flashDoc.getString("userId")
                if (creatorId == currentUid || creatorId.isNullOrBlank()) {
                    collection.document(videoId).delete().await()
                    deletedAny = true
                    Log.d("FlashRepository", "Deleted $videoId from flash_videos")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("FlashRepository", "Error deleting from flash_videos: $videoId", e)
        }

        // 2. Also delete from community_works
        try {
            val commDoc = db.collection("community_works").document(videoId).get().await()
            if (commDoc.exists()) {
                val creatorId = commDoc.getString("creatorId") ?: commDoc.getString("userId")
                if (creatorId == currentUid || creatorId.isNullOrBlank()) {
                    db.collection("community_works").document(videoId).delete().await()
                    deletedAny = true
                    Log.d("FlashRepository", "Deleted $videoId from community_works")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("FlashRepository", "Error deleting from community_works: $videoId", e)
        }

        // 3. Fallback direct delete
        if (!deletedAny) {
            try {
                collection.document(videoId).delete().await()
                deletedAny = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            try {
                db.collection("community_works").document(videoId).delete().await()
                deletedAny = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }

        // 4. Cleanup likes, saves, interactions and comments
        if (deletedAny) {
            try {
                collection.document(videoId).collection("likes").document(currentUid).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("community_works").document(videoId).collection("likes").document(currentUid).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("liked_posts").document(videoId).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("saved_posts").document(videoId).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("shared_posts").document(videoId).delete().await()
            } catch (_: Exception) {}
            try {
                val commentsSnapshot = db.collection("flash_comments")
                    .whereEqualTo("videoId", videoId)
                    .get().await()
                for (doc in commentsSnapshot.documents) {
                    try {
                        if (doc.getString("userId") == currentUid) {
                            doc.reference.delete().await()
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        return deletedAny
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
