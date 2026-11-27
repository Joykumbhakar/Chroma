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

    /** Retrieve a single Flash Video by ID */
    suspend fun getFlashById(videoId: String): FlashVideo? {
        return try {
            val doc = collection.document(videoId).get().await()
            if (doc.exists()) doc.toObject(FlashVideo::class.java) else null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "Error fetching flash by id $videoId", e)
            null
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
        turnOffRemix: Boolean = false,
        collaborators: List<com.chroma.studio.model.CollabParticipant> = emptyList(),
        musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null
    ): String {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userDoc = usersCollection.document(user.uid).get().await()
        val username = userDoc.getString("username") ?: ""
        val displayName = userDoc.getString("displayName") ?: ""
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
        val verified = userDoc.getBoolean("verified") ?: false
        val verifiedTone = userDoc.getString("verifiedTone")

        val normalizedCollabs = com.chroma.studio.model.normalizeInvitedCollaborators(user.uid, collaborators)
        val pendingCollabIds = normalizedCollabs.map { it.uid }

        val docRef = collection.document()
        val flash = FlashVideo(
            id = docRef.id,
            creatorId = user.uid,
            authorUsername = username,
            authorDisplayName = displayName,
            authorAvatarUrl = avatarUrl,
            authorVerified = verified,
            authorVerifiedTone = verifiedTone,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            caption = caption,
            hashtags = hashtags,
            mentions = mentions,
            collaborators = normalizedCollabs,
            pendingCollaboratorIds = pendingCollabIds,
            collaboratorIds = emptyList(),
            duration = duration.coerceAtMost(60_000L),
            remixedFromPostId = remixedFromPostId,
            remixedFromAuthorUsername = remixedFromAuthorUsername,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            hideLikesCount = hideLikesCount,
            turnOffComments = turnOffComments,
            turnOffRemix = turnOffRemix,
            musicTrackId = musicTrack?.trackId,
            musicTitle = musicTrack?.title,
            musicArtist = musicTrack?.artist,
            musicAlbum = musicTrack?.album,
            musicCoverUrl = musicTrack?.coverUrl,
            musicAudioUrl = musicTrack?.audioUrl,
            musicStartSec = musicTrack?.startSec ?: 0,
            musicDurationSec = musicTrack?.durationSec ?: 0,
            musicShowLyrics = musicTrack?.showLyricsSticker ?: false
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
                authorVerifiedTone = verifiedTone,
                title = caption.ifBlank { "Flash Video" },
                description = caption,
                aspectRatio = "9:16",
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                postType = "video",
                tags = hashtags,
                mentions = mentions,
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                remixedFromPostId = remixedFromPostId,
                remixedFromAuthorUsername = remixedFromAuthorUsername,
                timestamp = flash.timestamp,
                hideLikesCount = hideLikesCount,
                turnOffComments = turnOffComments,
                turnOffRemix = turnOffRemix,
                musicTrackId = musicTrack?.trackId,
                musicTitle = musicTrack?.title,
                musicArtist = musicTrack?.artist,
                musicAlbum = musicTrack?.album,
                musicCoverUrl = musicTrack?.coverUrl,
                musicAudioUrl = musicTrack?.audioUrl,
                musicStartSec = musicTrack?.startSec ?: 0,
                musicDurationSec = musicTrack?.durationSec ?: 0,
                musicShowLyrics = musicTrack?.showLyricsSticker ?: false
            )
            db.collection("community_works").document(docRef.id).set(communityPost).await()
        } catch (ce: Exception) {
            Log.e("FlashRepository", "Error syncing flash to community_works", ce)
        }

        if (musicTrack != null) {
            try {
                com.chroma.studio.data.MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId)
            } catch (me: Exception) {
                if (me is kotlinx.coroutines.CancellationException) throw me
            }
        }

        try {
            if (pendingCollabIds.isNotEmpty()) {
                NotificationDispatcher.dispatchCollabInviteNotifications(
                    invitedUids = pendingCollabIds,
                    postId = docRef.id,
                    isFlash = true,
                    previewUrl = thumbnailUrl.ifBlank { null },
                    titleSnippet = caption
                )
            }
            NotificationDispatcher.dispatchMentionNotifications(
                texts = listOf(caption),
                explicitMentions = mentions,
                surface = MentionSurface.POST,
                targetId = docRef.id,
                targetPreviewUrl = thumbnailUrl.ifBlank { null },
                detailSnippet = caption
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }

        return docRef.id
    }

    // ── Interactions ────────────────────────────────────────────────────────

    suspend fun setFlashLike(videoId: String, userId: String, targetLiked: Boolean): Boolean {
        if (videoId.isBlank() || userId.isBlank()) return false
        // Ensure Firebase Auth token is fresh before Firestore writes
        try { auth.currentUser?.getIdToken(false)?.await() } catch (_: Exception) {}
        return try {
            val docRef = collection.document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                @Suppress("UNCHECKED_CAST")
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val currentLikes = maxOf(snapshot.getLong("likesCount") ?: 0L, likedBy.size.toLong())
                val alreadyLiked = likedBy.contains(userId)

                if (alreadyLiked == targetLiked) {
                    return@runTransaction true // idempotent success
                }

                if (targetLiked) {
                    val newCount = maxOf(currentLikes + 1L, (likedBy.size + 1).toLong())
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayUnion(userId),
                        "dislikedBy", FieldValue.arrayRemove(userId),
                        "likesCount", newCount
                    )
                } else {
                    val newCount = maxOf(0L, currentLikes - 1L)
                    transaction.update(
                        docRef,
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", newCount
                    )
                }
                true
            }.await()

            // Synchronize with community_works (clamped atomically so likesCount never goes negative) and user liked_posts records
            val now = System.currentTimeMillis()
            val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(videoId)
            val flashLikeSubRef = collection.document(videoId).collection("likes").document(userId)

            try {
                val commRef = db.collection("community_works").document(videoId)
                db.runTransaction { tx ->
                    val commDoc = tx.get(commRef)
                    if (commDoc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val commLikedBy = commDoc.get("likedBy") as? List<String> ?: emptyList()
                        val commLikes = maxOf(commDoc.getLong("likesCount") ?: 0L, commLikedBy.size.toLong())
                        val commAlreadyLiked = commLikedBy.contains(userId)
                        if (targetLiked && !commAlreadyLiked) {
                            tx.update(
                                commRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayUnion(userId),
                                    "likesCount" to maxOf(commLikes + 1L, (commLikedBy.size + 1).toLong())
                                )
                            )
                        } else if (!targetLiked && commAlreadyLiked) {
                            tx.update(
                                commRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayRemove(userId),
                                    "likesCount" to maxOf(0L, commLikes - 1L)
                                )
                            )
                        }
                    }
                    null
                }.await()
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
                    @Suppress("UNCHECKED_CAST")
                    val collabIds = videoDoc.get("collaboratorIds") as? List<String> ?: emptyList()
                    val recipients = (listOfNotNull(creatorId) + collabIds).filter { it.isNotBlank() && it != userId }.distinct()
                    for (recipientUid in recipients) {
                        NotificationDispatcher.sendNotification(
                            targetUserId = recipientUid,
                            type = com.chroma.studio.model.NotificationActionType.LIKE,
                            actionText = if (recipientUid == creatorId) "liked your video" else "liked your collab video",
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

    /** Real-time Flow of comments for a video or community work, reading from flash_comments collection with subcollection fallback. */
    fun observeComments(videoId: String): Flow<List<com.chroma.studio.model.FlashComment>> = callbackFlow {
        if (videoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Authoritative real-time listener on flash_comments where videoId matches
        val registration = db.collection("flash_comments")
            .whereEqualTo("videoId", videoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FlashRepository", "flash_comments listener error for $videoId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.toObjects(com.chroma.studio.model.FlashComment::class.java)
                        .sortedBy { it.timestamp }
                    trySend(list)
                } else {
                    // Check subcollection in flash_videos as fallback
                    collection.document(videoId).collection("comments")
                        .get()
                        .addOnSuccessListener { subSnap ->
                            if (subSnap != null && !subSnap.isEmpty) {
                                val list = subSnap.toObjects(com.chroma.studio.model.FlashComment::class.java)
                                    .sortedBy { it.timestamp }
                                trySend(list)
                            } else {
                                trySend(emptyList())
                            }
                        }
                        .addOnFailureListener {
                            trySend(emptyList())
                        }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getComments(videoId: String): List<com.chroma.studio.model.FlashComment> {
        return try {
            val snapshot = db.collection("flash_comments")
                .whereEqualTo("videoId", videoId)
                .get().await()
            if (!snapshot.isEmpty) {
                snapshot.toObjects(com.chroma.studio.model.FlashComment::class.java).sortedBy { it.timestamp }
            } else {
                val subSnap = collection.document(videoId).collection("comments").get().await()
                subSnap.toObjects(com.chroma.studio.model.FlashComment::class.java).sortedBy { it.timestamp }
            }
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
        replyToUsername: String? = null,
        replyFlashVideoUrl: String? = null,
        preGeneratedId: String? = null
    ): com.chroma.studio.model.FlashComment {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userDoc = usersCollection.document(user.uid).get().await()
        val rawUsername = userDoc.getString("username")
        val rawDisplayName = userDoc.getString("displayName")
        val avatarUrl = userDoc.getString("avatarUrl") ?: user.photoUrl?.toString() ?: ""
        val verified = userDoc.getBoolean("verified") ?: false
        val verifiedTone = userDoc.getString("verifiedTone")

        val username = if (!rawUsername.isNullOrBlank()) rawUsername else (user.displayName?.replace(" ", "")?.lowercase() ?: "user_${user.uid.take(5)}")
        val displayName = if (!rawDisplayName.isNullOrBlank()) rawDisplayName else (user.displayName ?: username)

        val commentId = if (!preGeneratedId.isNullOrBlank()) preGeneratedId else java.util.UUID.randomUUID().toString()

        val comment = com.chroma.studio.model.FlashComment(
            id = commentId,
            videoId = videoId,
            userId = user.uid,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            verified = verified,
            verifiedTone = verifiedTone,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            replyToCommentId = replyToCommentId,
            replyToUsername = replyToUsername,
            replyFlashVideoUrl = replyFlashVideoUrl
        )

        // Resilient Database Persistence:
        // 1. Primary write to authoritative root collection `flash_comments/{commentId}`
        db.collection("flash_comments").document(commentId).set(comment).await()

        // 2. Mirror write to subcollections if available
        try {
            collection.document(videoId).collection("comments").document(commentId).set(comment).await()
        } catch (e: Exception) {
            Log.d("FlashRepository", "Subcollection mirror skipped or not found: ${e.message}")
        }
        try {
            db.collection("community_works").document(videoId).collection("comments").document(commentId).set(comment).await()
        } catch (_: Exception) {}

        // 3. Increment reply count on parent comment if replying
        if (!replyToCommentId.isNullOrBlank()) {
            try {
                db.collection("flash_comments").document(replyToCommentId)
                    .update("replyCount", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                Log.w("FlashRepository", "Could not increment replyCount on root parent comment", e)
            }
            try {
                collection.document(videoId).collection("comments").document(replyToCommentId)
                    .update("replyCount", FieldValue.increment(1)).await()
            } catch (_: Exception) {}
        }

        // 4. Safely update counter on parent document (flash_videos or community_works)
        try {
            val flashDoc = collection.document(videoId).get().await()
            if (flashDoc.exists()) {
                collection.document(videoId).update("commentCount", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) {
            Log.d("FlashRepository", "flash_videos document count update skipped: ${e.message}")
        }
        try {
            val commDoc = db.collection("community_works").document(videoId).get().await()
            if (commDoc.exists()) {
                db.collection("community_works").document(videoId).update("commentsCount", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) {
            Log.d("FlashRepository", "community_works document count update skipped: ${e.message}")
        }

        // Dispatch real notification to video creator & accepted collaborators
        try {
            val videoDoc = collection.document(videoId).get().await().takeIf { it.exists() }
                ?: db.collection("community_works").document(videoId).get().await()
            val creatorId = videoDoc.getString("creatorId")
            val thumb = videoDoc.getString("thumbnailUrl") ?: videoDoc.getString("imageUrl")
            @Suppress("UNCHECKED_CAST")
            val collabIds = videoDoc.get("collaboratorIds") as? List<String> ?: emptyList()
            val recipients = (listOfNotNull(creatorId) + collabIds).filter { it.isNotBlank() && it != user.uid }.distinct()
            for (recipientUid in recipients) {
                NotificationDispatcher.sendNotification(
                    targetUserId = recipientUid,
                    type = com.chroma.studio.model.NotificationActionType.COMMENT,
                    actionText = if (recipientUid == creatorId) "commented on your post" else "commented on your collab post",
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
                    val videoDoc = collection.document(videoId).get().await().takeIf { it.exists() }
                        ?: db.collection("community_works").document(videoId).get().await()
                    val thumb = videoDoc.getString("thumbnailUrl") ?: videoDoc.getString("imageUrl")
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
            val videoDoc = collection.document(videoId).get().await().takeIf { it.exists() }
                ?: db.collection("community_works").document(videoId).get().await()
            val thumb = videoDoc.getString("thumbnailUrl") ?: videoDoc.getString("imageUrl")
            NotificationDispatcher.dispatchMentionNotifications(
                texts = listOf(text),
                surface = MentionSurface.COMMENT,
                targetId = videoId,
                targetPreviewUrl = thumb,
                detailSnippet = text
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }

        return comment
    }

    suspend fun setCommentLike(commentId: String, videoId: String = "", userId: String, targetLiked: Boolean): Boolean {
        return try {
            val rootDoc = db.collection("flash_comments").document(commentId)
            val updates = if (targetLiked) {
                mapOf(
                    "likedBy" to FieldValue.arrayUnion(userId),
                    "dislikedBy" to FieldValue.arrayRemove(userId),
                    "likesCount" to FieldValue.increment(1)
                )
            } else {
                mapOf(
                    "likedBy" to FieldValue.arrayRemove(userId),
                    "likesCount" to FieldValue.increment(-1)
                )
            }

            // Always update root collection
            rootDoc.update(updates).await()

            // Best-effort update subcollections
            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
            }

            if (targetLiked) {
                try {
                    val commentDoc = rootDoc.get().await()
                    val authorId = commentDoc.getString("userId")
                    val commentText = commentDoc.getString("text") ?: ""
                    val targetVideoId = videoId.ifBlank { commentDoc.getString("videoId") ?: "" }
                    if (!authorId.isNullOrBlank() && authorId != userId) {
                        var thumb: String? = null
                        if (targetVideoId.isNotBlank()) {
                            val videoDoc = collection.document(targetVideoId).get().await().takeIf { it.exists() }
                                ?: db.collection("community_works").document(targetVideoId).get().await()
                            thumb = videoDoc.getString("thumbnailUrl") ?: videoDoc.getString("imageUrl")
                        }
                        NotificationDispatcher.sendNotification(
                            targetUserId = authorId,
                            type = com.chroma.studio.model.NotificationActionType.LIKE_COMMENT,
                            actionText = "liked your comment",
                            detailText = commentText,
                            targetPreviewUrl = thumb,
                            targetId = targetVideoId.ifBlank { commentId }
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

    suspend fun toggleCommentLike(commentId: String, videoId: String = "", userId: String): Boolean {
        return try {
            val docRef = db.collection("flash_comments").document(commentId)
            val snapshot = docRef.get().await()
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            val isLiked = !likedBy.contains(userId)
            setCommentLike(commentId, videoId, userId, isLiked)
            isLiked
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "toggleCommentLike error", e)
            false
        }
    }

    suspend fun setCommentDislike(commentId: String, videoId: String = "", userId: String, targetDisliked: Boolean): Boolean {
        return try {
            val rootDoc = db.collection("flash_comments").document(commentId)
            val updates = if (targetDisliked) {
                mapOf(
                    "dislikedBy" to FieldValue.arrayUnion(userId),
                    "likedBy" to FieldValue.arrayRemove(userId)
                )
            } else {
                mapOf(
                    "dislikedBy" to FieldValue.arrayRemove(userId)
                )
            }

            rootDoc.update(updates).await()

            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentDislike error", e)
            false
        }
    }

    suspend fun setCommentPinned(commentId: String, videoId: String, isPinned: Boolean): Boolean {
        return try {
            val rootColl = db.collection("flash_comments")
            if (isPinned && videoId.isNotBlank()) {
                val currentPinned = rootColl.whereEqualTo("videoId", videoId).whereEqualTo("isPinned", true).get().await()
                for (doc in currentPinned.documents) {
                    if (doc.id != commentId) {
                        try {
                            doc.reference.update("isPinned", false).await()
                            collection.document(videoId).collection("comments").document(doc.id).update("isPinned", false).await()
                        } catch (_: Exception) {}
                    }
                }
            }
            rootColl.document(commentId).update("isPinned", isPinned).await()
            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).update("isPinned", isPinned).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).update("isPinned", isPinned).await()
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentPinned error", e)
            false
        }
    }

    suspend fun setCommentCreatorHeart(commentId: String, videoId: String = "", isHearted: Boolean): Boolean {
        return try {
            db.collection("flash_comments").document(commentId).update("isCreatorHearted", isHearted).await()
            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).update("isCreatorHearted", isHearted).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).update("isCreatorHearted", isHearted).await()
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentCreatorHeart error", e)
            false
        }
    }

    suspend fun deleteComment(commentId: String, videoId: String, replyToCommentId: String? = null): Boolean {
        return try {
            var deletedCount = 1L
            try {
                db.collection("flash_comments").document(commentId).delete().await()
            } catch (e: Exception) {
                Log.w("FlashRepository", "Could not delete root comment $commentId", e)
            }
            if (replyToCommentId.isNullOrBlank()) {
                try {
                    val childReplies = db.collection("flash_comments")
                        .whereEqualTo("replyToCommentId", commentId)
                        .get()
                        .await()
                    deletedCount += childReplies.documents.size
                    for (childDoc in childReplies.documents) {
                        try {
                            childDoc.reference.delete().await()
                            if (videoId.isNotBlank()) {
                                try {
                                    collection.document(videoId).collection("comments").document(childDoc.id).delete().await()
                                } catch (_: Exception) {}
                                try {
                                    db.collection("community_works").document(videoId).collection("comments").document(childDoc.id).delete().await()
                                } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).delete().await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).delete().await()
                } catch (_: Exception) {}
                try {
                    val flashDoc = collection.document(videoId).get().await()
                    if (flashDoc.exists()) {
                        collection.document(videoId).update("commentCount", FieldValue.increment(-deletedCount)).await()
                    }
                } catch (_: Exception) {}
                try {
                    val commDoc = db.collection("community_works").document(videoId).get().await()
                    if (commDoc.exists()) {
                        db.collection("community_works").document(videoId).update("commentsCount", FieldValue.increment(-deletedCount)).await()
                    }
                } catch (_: Exception) {}
            }
            if (!replyToCommentId.isNullOrBlank()) {
                try {
                    db.collection("flash_comments").document(replyToCommentId).update("replyCount", FieldValue.increment(-1)).await()
                } catch (_: Exception) {}
                if (videoId.isNotBlank()) {
                    try {
                        collection.document(videoId).collection("comments").document(replyToCommentId).update("replyCount", FieldValue.increment(-1)).await()
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "deleteComment error", e)
            false
        }
    }

    suspend fun editComment(commentId: String, videoId: String = "", newText: String): Boolean {
        val cleaned = newText.trim()
        if (commentId.isBlank() || cleaned.isEmpty()) return false
        return try {
            val existingDoc = try { db.collection("flash_comments").document(commentId).get().await() } catch (_: Exception) { null }
            val oldText = existingDoc?.getString("text")
            val resolvedVideoId = videoId.ifBlank { existingDoc?.getString("videoId").orEmpty() }

            val updates = mapOf(
                "text" to cleaned,
                "edited" to true
            )
            db.collection("flash_comments").document(commentId).update(updates).await()
            if (resolvedVideoId.isNotBlank()) {
                try {
                    collection.document(resolvedVideoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(resolvedVideoId).collection("comments").document(commentId).update(updates).await()
                } catch (_: Exception) {}
            }
            try {
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(cleaned),
                    previousTexts = listOf(oldText),
                    surface = MentionSurface.COMMENT,
                    targetId = resolvedVideoId.ifBlank { commentId },
                    detailSnippet = cleaned
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "editComment error", e)
            false
        }
    }

    suspend fun setCommentReaction(
        commentId: String,
        videoId: String = "",
        userId: String,
        emoji: String,
        add: Boolean
    ): Boolean {
        if (commentId.isBlank() || userId.isBlank() || emoji.isBlank()) return false
        return try {
            val fieldPath = com.google.firebase.firestore.FieldPath.of("reactions", emoji)
            val op = if (add) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId)
            db.collection("flash_comments").document(commentId).update(fieldPath, op).await()
            if (videoId.isNotBlank()) {
                try {
                    collection.document(videoId).collection("comments").document(commentId).update(fieldPath, op).await()
                } catch (_: Exception) {}
                try {
                    db.collection("community_works").document(videoId).collection("comments").document(commentId).update(fieldPath, op).await()
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "setCommentReaction error", e)
            false
        }
    }

    suspend fun reportComment(commentId: String, videoId: String, reason: String): Boolean {
        val reporterId = auth.currentUser?.uid ?: return false
        if (commentId.isBlank()) return false
        return try {
            val reportPayload = hashMapOf(
                "commentId" to commentId,
                "videoId" to videoId,
                "reporterId" to reporterId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("comment_reports").add(reportPayload).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FlashRepository", "reportComment error", e)
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
            val ownSnap = collection
                .whereEqualTo("creatorId", userId)
                .get().await()
            val ownList = ownSnap.toObjects(FlashVideo::class.java)
                .filter { !it.isArchived && !it.isTakedown }

            val collabList = try {
                collection
                    .whereArrayContains("collaboratorIds", userId)
                    .get().await()
                    .toObjects(FlashVideo::class.java)
                    .filter { !it.isArchived && !it.isTakedown }
            } catch (_: Exception) {
                emptyList()
            }

            com.chroma.studio.model.mergeCreatorAndCollabFlashes(ownList, collabList)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }
}
