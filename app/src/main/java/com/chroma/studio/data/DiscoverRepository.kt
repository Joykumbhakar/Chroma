package com.chroma.studio.data

import com.chroma.studio.model.ChromaWork
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import android.util.Log
import android.net.Uri

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth

import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.data.CloudinaryClient

class DiscoverRepository {
    companion object {
        @Volatile
        private var INSTANCE: DiscoverRepository? = null

        fun getInstance(): DiscoverRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiscoverRepository().also { INSTANCE = it }
            }
        }

        private val profileCache = java.util.concurrent.ConcurrentHashMap<String, UserProfile>()
    }
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("community_works")
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")

    private fun mapFlashToDiscoverPost(fv: com.chroma.studio.model.FlashVideo, docId: String = ""): DiscoverPost {
        return DiscoverPost(
            id = docId.ifBlank { fv.id },
            creatorId = fv.creatorId,
            authorUsername = fv.authorUsername,
            authorDisplayName = fv.authorDisplayName,
            authorAvatarUrl = fv.authorAvatarUrl,
            authorVerified = fv.authorVerified,
            title = fv.caption.ifBlank { "Flash Video" },
            description = fv.caption,
            aspectRatio = "9:16",
            videoUrl = fv.videoUrl,
            thumbnailUrl = fv.thumbnailUrl,
            postType = "video",
            tags = fv.hashtags,
            mentions = fv.mentions,
            likesCount = fv.likesCount,
            likedBy = fv.likedBy,
            remixCount = fv.remixCount,
            remixedBy = fv.remixedBy,
            shareCount = fv.shareCount,
            sharedBy = fv.sharedBy,
            saveCount = fv.saveCount,
            savedBy = fv.savedBy,
            commentsCount = fv.commentCount,
            commentCount = fv.commentCount,
            remixedFromPostId = fv.remixedFromPostId,
            remixedFromAuthorUsername = fv.remixedFromAuthorUsername,
            timestamp = fv.timestamp,
            hideLikesCount = fv.hideLikesCount,
            turnOffComments = fv.turnOffComments,
            turnOffRemix = fv.turnOffRemix
        )
    }

    suspend fun getRecentWorks(limit: Long = 10, lastTimestamp: Long? = null): List<DiscoverPost> {
        return try {
            var query = collection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }
            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DiscoverPost::class.java)?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else (doc.toObject(DiscoverPost::class.java)?.id ?: "")
                )
            }.filter { !it.isArchived }

            val flashes = try {
                var flashQuery = db.collection("flash_videos").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
                if (lastTimestamp != null) {
                    flashQuery = flashQuery.startAfter(lastTimestamp)
                }
                val fSnap = flashQuery.get().await()
                fSnap.documents.mapNotNull { doc ->
                    val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                    if (fv != null && !fv.isArchived) mapFlashToDiscoverPost(fv, doc.id) else null
                }
            } catch (_: Exception) {
                emptyList()
            }

            (posts + flashes)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
                .take(limit.toInt())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching community works", e)
            emptyList()
        }
    }

    suspend fun publishWork(work: ChromaWork, aspectRatio: String = "4:3", tags: List<String> = emptyList(), mentions: List<String> = emptyList()) {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
        
        val newWorkId = collection.document().id
        val communityWork = DiscoverPost(
            id = newWorkId,
            title = work.name,
            description = work.description,
            jsonUrl = com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson),
            postType = "work",
            tags = tags,
            mentions = mentions,
            creatorId = user.uid,
            authorUsername = userProfile.username,
            authorDisplayName = userProfile.displayName,
            authorAvatarUrl = userProfile.avatarUrl,
            authorVerified = userProfile.verified,
            aspectRatio = aspectRatio,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            remixedFromPostId = work.remixedFromPostId,
            remixedFromAuthorId = work.remixedFromAuthorId,
            remixedFromAuthorUsername = work.remixedFromAuthorUsername,
            remixedFromAuthorDisplayName = work.remixedFromAuthorDisplayName,
            remixedFromAuthorAvatar = work.remixedFromAuthorAvatar,
            remixedFromAuthorVerified = work.remixedFromAuthorVerified
        )
        collection.document(newWorkId).set(communityWork).await()
        
        // Increment remix count on the original post if this is a remix (1 count per user)
        if (!work.remixedFromPostId.isNullOrEmpty()) {
            incrementRemixCount(work.remixedFromPostId, user.uid)
        }
    }

    suspend fun uploadImagePost(
        imageBytes: ByteArray,
        caption: String,
        description: String,
        aspectRatio: String,
        tags: List<String>,
        mentions: List<String>
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
            
            // Upload image to Cloudinary CDN
            val imageUrl = CloudinaryClient.uploadImage(imageBytes, folder = user.uid)
                ?: throw Exception("Image upload to Cloudinary failed. Check your cloud name and upload preset in local.properties.")
            
            val newWorkId = collection.document().id
            val communityWork = DiscoverPost(
                id = newWorkId,
                title = caption,
                description = description,
                imageUrl = imageUrl,
                postType = "image",
                tags = tags,
                mentions = mentions,
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                likesCount = 0
            )
            collection.document(newWorkId).set(communityWork).await()
            Log.d("DiscoverRepository", "Image post created with Cloudinary URL: $imageUrl")
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error uploading image post", e)
            false
        }
    }

    suspend fun uploadMultiImagePost(
        imageBytesList: List<ByteArray>,
        caption: String,
        description: String,
        aspectRatio: String,
        tags: List<String>,
        mentions: List<String>,
        categoryTag: String? = null,
        hideLikesCount: Boolean = false,
        turnOffComments: Boolean = false,
        turnOffRemix: Boolean = false
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")

            val uploadedUrls = mutableListOf<String>()
            for (bytes in imageBytesList) {
                val url = CloudinaryClient.uploadImage(bytes, folder = user.uid)
                if (url != null) {
                    uploadedUrls.add(url)
                }
            }
            if (uploadedUrls.isEmpty()) {
                throw Exception("Failed to upload images")
            }

            val newWorkId = collection.document().id
            val allTags = if (!categoryTag.isNullOrBlank()) (tags + categoryTag).distinct() else tags
            val communityWork = DiscoverPost(
                id = newWorkId,
                title = caption,
                description = description,
                imageUrl = uploadedUrls.firstOrNull(),
                imageUrls = uploadedUrls,
                postType = "image",
                tags = allTags,
                mentions = mentions,
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                aspectRatio = aspectRatio,
                trendingTag = categoryTag,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                hideLikesCount = hideLikesCount,
                hideRemixesCount = turnOffRemix,
                turnOffComments = turnOffComments,
                turnOffRemix = turnOffRemix
            )
            collection.document(newWorkId).set(communityWork).await()
            Log.d("DiscoverRepository", "Multi-image post created: $newWorkId with ${uploadedUrls.size} images")
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error uploading multi-image post", e)
            false
        }
    }

    suspend fun publishMultiWork(
        works: List<ChromaWork>,
        caption: String,
        description: String,
        aspectRatio: String,
        tags: List<String>,
        mentions: List<String>,
        categoryTag: String? = null,
        hideLikesCount: Boolean = false,
        turnOffComments: Boolean = false,
        turnOffRemix: Boolean = false
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")

            val compressedJsonList = works.map { work ->
                com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson)
            }

            val newWorkId = collection.document().id
            val allTags = if (!categoryTag.isNullOrBlank()) (tags + categoryTag).distinct() else tags
            val primaryWork = works.firstOrNull()
            val communityWork = DiscoverPost(
                id = newWorkId,
                title = if (caption.isNotBlank()) caption else (primaryWork?.name ?: "Untitled Work"),
                description = if (description.isNotBlank()) description else (primaryWork?.description ?: ""),
                jsonUrl = compressedJsonList.firstOrNull() ?: "",
                workUrls = compressedJsonList,
                postType = "work",
                tags = allTags,
                mentions = mentions,
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                aspectRatio = aspectRatio,
                trendingTag = categoryTag,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                hideLikesCount = hideLikesCount,
                hideRemixesCount = turnOffRemix,
                turnOffComments = turnOffComments,
                turnOffRemix = turnOffRemix,
                remixedFromPostId = primaryWork?.remixedFromPostId,
                remixedFromAuthorId = primaryWork?.remixedFromAuthorId,
                remixedFromAuthorUsername = primaryWork?.remixedFromAuthorUsername,
                remixedFromAuthorDisplayName = primaryWork?.remixedFromAuthorDisplayName,
                remixedFromAuthorAvatar = primaryWork?.remixedFromAuthorAvatar,
                remixedFromAuthorVerified = primaryWork?.remixedFromAuthorVerified ?: false
            )
            collection.document(newWorkId).set(communityWork).await()
            if (!primaryWork?.remixedFromPostId.isNullOrEmpty()) {
                incrementRemixCount(primaryWork.remixedFromPostId!!, user.uid)
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error publishing multi work", e)
            false
        }
    }

    /**
     * Upload a video post to the Community (community_works) feed.
     * Expects an already-processed/trimmed video [videoUrl] and optional [thumbnailUrl]
     * — the ViewModel handles all FFmpeg trimming/compression before calling this.
     */
    suspend fun uploadCommunityVideoPost(
        videoUrl: String,
        thumbnailUrl: String,
        caption: String,
        description: String,
        aspectRatio: String,
        tags: List<String>,
        mentions: List<String>,
        duration: Long = 0L,
        categoryTag: String? = null,
        hideLikesCount: Boolean = false,
        turnOffComments: Boolean = false,
        turnOffRemix: Boolean = false,
        hideDownloadsCount: Boolean = false
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")

            val newWorkId = collection.document().id
            val communityWork = DiscoverPost(
                id = newWorkId,
                title = caption,
                description = description,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                postType = "video",
                tags = tags,
                mentions = mentions,
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                aspectRatio = aspectRatio,
                trendingTag = categoryTag,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                hideLikesCount = hideLikesCount,
                hideRemixesCount = turnOffRemix,
                hideDownloadsCount = hideDownloadsCount,
                turnOffComments = turnOffComments,
                turnOffRemix = turnOffRemix
            )
            collection.document(newWorkId).set(communityWork).await()
            Log.d("DiscoverRepository", "Community video post created: $newWorkId, url=$videoUrl")
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error uploading community video post", e)
            false
        }
    }

    suspend fun incrementDownloadCount(postId: String, userId: String) {
        if (postId.isBlank() || userId.isBlank()) return
        try {
            val docRef = collection.document(postId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val downloadedBy = snapshot.get("downloadedBy") as? List<String> ?: emptyList()
                val currentDownloads = snapshot.getLong("downloadCount") ?: 0L
                if (!downloadedBy.contains(userId)) {
                    transaction.update(
                        docRef,
                        "downloadedBy", FieldValue.arrayUnion(userId),
                        "downloadCount", currentDownloads + 1
                    )
                }
            }.await()
        } catch (e: Exception) {
            Log.w("DiscoverRepository", "Could not increment download count", e)
        }
    }

    suspend fun incrementRemixCount(postId: String, userId: String) {
        if (postId.isBlank() || userId.isBlank()) return
        try {
            val docRef = collection.document(postId)
            docRef.update(
                mapOf(
                    "remixedBy" to FieldValue.arrayUnion(userId),
                    "remixCount" to FieldValue.increment(1)
                )
            ).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DiscoverRepository", "Could not increment remix count", e)
        }
    }

    suspend fun fetchPostById(postId: String): DiscoverPost? {
        return try {
            val doc = collection.document(postId).get().await()
            doc.toObject(DiscoverPost::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching post by ID", e)
            null
        }
    }

    suspend fun getWorksByUser(creatorId: String): List<DiscoverPost> {
        return try {
            val snapshot = collection
                .whereEqualTo("creatorId", creatorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DiscoverPost::class.java)?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else (doc.toObject(DiscoverPost::class.java)?.id ?: "")
                )
            }.filter { !it.isArchived }

            val flashes = try {
                val fSnap = db.collection("flash_videos")
                    .whereEqualTo("creatorId", creatorId)
                    .get()
                    .await()
                fSnap.documents.mapNotNull { doc ->
                    val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                    if (fv != null && !fv.isArchived) mapFlashToDiscoverPost(fv, doc.id) else null
                }
            } catch (_: Exception) {
                emptyList()
            }

            (posts + flashes)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching works for user", e)
            emptyList()
        }
    }

    suspend fun getArchivedWorksByUser(creatorId: String): List<DiscoverPost> {
        return try {
            val snapshot = collection
                .whereEqualTo("creatorId", creatorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DiscoverPost::class.java)?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else (doc.toObject(DiscoverPost::class.java)?.id ?: "")
                )
            }
            posts.filter { it.isArchived }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching archived works for user", e)
            emptyList()
        }
    }

    suspend fun archivePost(postId: String, archive: Boolean) {
        try {
            collection.document(postId).update("isArchived", archive).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error archiving post", e)
            throw e
        }
    }

    suspend fun shareWork(work: ChromaWork, authorName: String, authorUsername: String, creatorId: String, authorAvatarUrl: String, authorVerified: Boolean, aspectRatio: String): Boolean {
        return try {
            val communityWork = DiscoverPost(
                id = work.id, 
                title = work.name,
                description = work.description,
                jsonUrl = com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson),
                creatorId = creatorId,
                authorUsername = authorUsername,
                authorDisplayName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                authorVerified = authorVerified,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                likesCount = 0
            )
            collection.document(communityWork.id).set(communityWork).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error sharing work", e)
            false
        }
    }

    suspend fun setLike(workId: String, userId: String, targetLiked: Boolean): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        return try {
            val workRef = collection.document(workId)
            val likeRef = collection.document(workId).collection("likes").document(userId)
            val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(workId)
            val now = System.currentTimeMillis()

            if (targetLiked) {
                // 1. Atomically update community_works post: add to likedBy array and increment likesCount
                val updates = mapOf(
                    "likedBy" to FieldValue.arrayUnion(userId),
                    "likesCount" to FieldValue.increment(1)
                )
                try {
                    workRef.update(updates).await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    workRef.set(mapOf("likedBy" to listOf(userId), "likesCount" to 1), SetOptions.merge()).await()
                }

                // 2. Resilient subcollection updates
                try {
                    likeRef.set(mapOf("timestamp" to now)).await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("DiscoverRepository", "likeRef set warning", e)
                }
                try {
                    userLikeRef.set(mapOf("likedAt" to now)).await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("DiscoverRepository", "userLikeRef set warning", e)
                }

                // 3. Dispatch real notification to post author
                try {
                    val postDoc = workRef.get().await()
                    val creatorId = postDoc.getString("creatorId")
                    val title = postDoc.getString("title") ?: "post"
                    val thumb = postDoc.getString("thumbnailUrl") ?: postDoc.getString("imageUrl")
                    if (!creatorId.isNullOrBlank() && creatorId != userId) {
                        NotificationDispatcher.sendNotification(
                            targetUserId = creatorId,
                            type = com.chroma.studio.model.NotificationActionType.LIKE,
                            actionText = "liked your post",
                            detailText = title,
                            targetPreviewUrl = thumb,
                            targetId = workId
                        )
                    }
                } catch (_: Exception) {}
            } else {
                // 1. Atomically update community_works post: remove from likedBy array and decrement likesCount
                val updates = mapOf(
                    "likedBy" to FieldValue.arrayRemove(userId),
                    "likesCount" to FieldValue.increment(-1)
                )
                try {
                    workRef.update(updates).await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("DiscoverRepository", "workRef unlike update warning", e)
                }

                // 2. Remove from subcollections
                try {
                    likeRef.delete().await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("DiscoverRepository", "likeRef delete warning", e)
                }
                try {
                    userLikeRef.delete().await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("DiscoverRepository", "userLikeRef delete warning", e)
                }
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error setting like", e)
            false
        }
    }

    suspend fun toggleLike(workId: String, userId: String, forceLike: Boolean = false): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        val currentlyLiked = hasLiked(workId, userId)
        val target = if (forceLike) true else !currentlyLiked
        return setLike(workId, userId, target)
    }

    suspend fun deleteWork(workId: String): Boolean {
        return try {
            collection.document(workId).delete().await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error deleting work", e)
            false
        }
    }

    suspend fun updateWork(workId: String, newTitle: String, newDescription: String): Boolean {
        return try {
            collection.document(workId).update(
                mapOf(
                    "title" to newTitle,
                    "description" to newDescription
                )
            ).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error updating work", e)
            false
        }
    }

    suspend fun checkIsFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val followingRef = usersCollection.document(currentUserId).collection("following").document(targetUserId)
            followingRef.get().await().exists()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    suspend fun setFollow(currentUserId: String, targetUserId: String, targetFollow: Boolean): Boolean {
        if (currentUserId.isBlank() || targetUserId.isBlank() || currentUserId == targetUserId) return false
        return try {
            val currentUserFollowingRef = usersCollection.document(currentUserId).collection("following").document(targetUserId)
            val targetUserFollowersRef = usersCollection.document(targetUserId).collection("followers").document(currentUserId)
            val currentUserDoc = usersCollection.document(currentUserId)
            val targetUserDoc = usersCollection.document(targetUserId)

            val succeeded = db.runTransaction { transaction ->
                val followingDoc = transaction.get(currentUserFollowingRef)
                val alreadyFollowing = followingDoc.exists()
                if (alreadyFollowing == targetFollow) {
                    return@runTransaction true // idempotent success
                }
                if (targetFollow) {
                    val timestamp = mapOf("timestamp" to System.currentTimeMillis())
                    transaction.set(currentUserFollowingRef, timestamp)
                    transaction.set(targetUserFollowersRef, timestamp)
                    transaction.set(currentUserDoc, mapOf("followingCount" to FieldValue.increment(1)), SetOptions.merge())
                    transaction.set(targetUserDoc, mapOf("followersCount" to FieldValue.increment(1)), SetOptions.merge())
                } else {
                    transaction.delete(currentUserFollowingRef)
                    transaction.delete(targetUserFollowersRef)
                    transaction.set(currentUserDoc, mapOf("followingCount" to FieldValue.increment(-1)), SetOptions.merge())
                    transaction.set(targetUserDoc, mapOf("followersCount" to FieldValue.increment(-1)), SetOptions.merge())
                }
                true
            }.await()

            if (succeeded && targetFollow) {
                try {
                    val targetSnap = targetUserDoc.get().await()
                    val isPrivate = targetSnap.getBoolean("privateAccount") ?: false
                    val notifType = if (isPrivate) {
                        com.chroma.studio.model.NotificationActionType.FOLLOW_REQUEST
                    } else {
                        com.chroma.studio.model.NotificationActionType.FOLLOW
                    }
                    val actionMsg = if (isPrivate) "requested to follow you" else "started following you"
                    NotificationDispatcher.sendNotification(
                        targetUserId = targetUserId,
                        type = notifType,
                        actionText = actionMsg,
                        targetId = currentUserId
                    )
                } catch (e: Exception) {
                    Log.w("DiscoverRepository", "Failed to dispatch follow notification", e)
                }
            }

            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error setting follow", e)
            false
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String): Boolean {
        val currentlyFollowing = checkIsFollowing(currentUserId, targetUserId)
        return setFollow(currentUserId, targetUserId, !currentlyFollowing)
    }
    
    suspend fun hasLiked(workId: String, userId: String): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        return try {
            val doc = collection.document(workId).get().await()
            val likedBy = doc.get("likedBy") as? List<String>
            if (likedBy != null) {
                likedBy.contains(userId)
            } else {
                val likeDoc = collection.document(workId).collection("likes").document(userId).get().await()
                likeDoc.exists()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return try {
            val doc = usernamesCollection.document(username.trim().lowercase()).get().await()
            !doc.exists()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    suspend fun suggestAvailableUsernames(baseUsername: String): List<String> {
        val clean = baseUsername.trim().lowercase().filter { it.isLetterOrDigit() }
        if (clean.length < 2) return emptyList()
        
        val random = java.util.Random()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
        
        val candidates = listOf(
            "${clean}${random.nextInt(90) + 10}",
            "${clean}${currentYear}",
            "the${clean}",
            "${clean}_hq",
            "${clean}${random.nextInt(900) + 100}",
            "real${clean}"
        ).distinct()

        val available = mutableListOf<String>()
        for (candidate in candidates) {
            if (checkUsernameAvailable(candidate)) {
                available.add(candidate)
                if (available.size >= 3) break
            }
        }
        return available
    }

    suspend fun getUserByUsername(username: String): UserProfile? {
        return try {
            val userDoc = usernamesCollection.document(username.trim().lowercase()).get().await()
            val uid = userDoc.getString("uid") ?: return null
            getUserProfile(uid)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun registerUser(uid: String, displayName: String, username: String): Boolean {
        return registerDetailedUser(uid, displayName, username, null, "")
    }

    suspend fun registerDetailedUser(
        uid: String, 
        displayName: String, 
        username: String, 
        dob: Long?, 
        avatarUrl: String,
        interests: List<String> = emptyList(),
        gender: String = "",
        agreedToTermsAt: Long? = System.currentTimeMillis()
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()
            // Check availability again in transaction
            db.runTransaction { transaction ->
                val usernameRef = usernamesCollection.document(username)
                val userRef = usersCollection.document(uid)
                
                val usernameDoc = transaction.get(usernameRef)
                if (usernameDoc.exists() && usernameDoc.getString("uid") != uid) {
                    throw Exception("Username is already taken")
                }
                
                val profile = UserProfile(
                    uid = uid,
                    username = username,
                    displayName = displayName,
                    dob = dob,
                    avatarUrl = avatarUrl,
                    interests = interests,
                    gender = gender,
                    agreedToTermsAt = agreedToTermsAt ?: now,
                    createdAt = now,
                    online = true,
                    lastSeen = now,
                    followersCount = 0,
                    followingCount = 0,
                    publicKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                )
                
                transaction.set(usernameRef, mapOf("uid" to uid, "createdAt" to now))
                transaction.set(userRef, profile)
            }.await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error registering detailed user", e)
            false
        }
    }

    suspend fun updateUserProfile(updatedProfile: UserProfile): Result<Unit> {
        return try {
            val uid = updatedProfile.uid.ifBlank { auth.currentUser?.uid }
                ?: return Result.failure(IllegalStateException("User is not authenticated"))

            val cleanUsername = updatedProfile.username.trim().lowercase()
            val cleanDisplayName = updatedProfile.displayName.trim()

            if (cleanDisplayName.isBlank()) {
                return Result.failure(IllegalArgumentException("Display name cannot be empty"))
            }

            if (cleanUsername.length < 3 || cleanUsername.length > 30) {
                return Result.failure(IllegalArgumentException("Username must be between 3 and 30 characters"))
            }

            if (!cleanUsername.matches(Regex("^[a-z0-9._]+$"))) {
                return Result.failure(IllegalArgumentException("Username can only contain lowercase letters, numbers, periods, and underscores"))
            }

            // Fetch current profile to check if username changed
            val currentProfile = getUserProfile(uid)
            val oldUsername = currentProfile?.username?.trim()?.lowercase()
            val usernameChanged = oldUsername != null && oldUsername != cleanUsername

            val now = System.currentTimeMillis()

            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)

                if (usernameChanged) {
                    val newUsernameRef = usernamesCollection.document(cleanUsername)
                    val newUsernameDoc = transaction.get(newUsernameRef)
                    if (newUsernameDoc.exists() && newUsernameDoc.getString("uid") != uid) {
                        throw IllegalArgumentException("Username @$cleanUsername is already taken")
                    }

                    // Reserve new username in usernamesCollection index
                    transaction.set(newUsernameRef, mapOf("uid" to uid, "updatedAt" to now))

                    // Free old username
                    if (!oldUsername.isNullOrBlank()) {
                        val oldUsernameRef = usernamesCollection.document(oldUsername)
                        transaction.delete(oldUsernameRef)
                    }
                } else if (cleanUsername.isNotBlank()) {
                    // Ensure current username document is indexed in usernamesCollection
                    val usernameRef = usernamesCollection.document(cleanUsername)
                    transaction.set(usernameRef, mapOf("uid" to uid, "updatedAt" to now), SetOptions.merge())
                }

                // Prepare safe update map (merging into user doc without clobbering createdAt, counters, verified, etc.)
                val updates = mutableMapOf<String, Any?>(
                    "uid" to uid,
                    "displayName" to cleanDisplayName,
                    "username" to cleanUsername,
                    "avatarUrl" to updatedProfile.avatarUrl.trim(),
                    "bannerUrl" to updatedProfile.bannerUrl.trim(),
                    "bannerWorkId" to updatedProfile.bannerWorkId,
                    "bio" to updatedProfile.bio.trim(),
                    "role" to updatedProfile.role.trim(),
                    "dob" to updatedProfile.dob,
                    "dateOfBirth" to updatedProfile.dob,
                    "gender" to updatedProfile.gender,
                    "contentTags" to updatedProfile.contentTags,
                    "dataVisibility" to updatedProfile.dataVisibility,
                    "socialLinks" to updatedProfile.socialLinks,
                    "location" to updatedProfile.location.trim(),
                    "officeLocation" to updatedProfile.officeLocation.trim(),
                    "homeLocation" to updatedProfile.homeLocation.trim(),
                    "countryLocation" to updatedProfile.countryLocation.trim(),
                    "isPrivateAccount" to updatedProfile.isPrivateAccount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                transaction.set(userRef, updates, SetOptions.merge())
            }.await()

            // Synchronize Firebase Auth User Profile
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && firebaseUser.uid == uid) {
                    val builder = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanDisplayName)
                    if (updatedProfile.avatarUrl.isNotBlank() && !updatedProfile.avatarUrl.startsWith("preset:")) {
                        builder.setPhotoUri(Uri.parse(updatedProfile.avatarUrl.trim()))
                    }
                    firebaseUser.updateProfile(builder.build()).await()
                }
            } catch (e: Exception) {
                Log.w("DiscoverRepository", "Non-fatal error updating FirebaseAuth profile: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error updating user profile", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        profileCache[uid]?.let { return it }
        return try {
            val doc = usersCollection.document(uid).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) {
                profileCache[uid] = profile
            }
            profile
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getProfilesByIds(userIds: List<String>, limit: Int = 10): List<UserProfile> {
        val targetIds = userIds.filter { it.isNotBlank() }.distinct().take(limit)
        if (targetIds.isEmpty()) return emptyList()
        return try {
            coroutineScope {
                targetIds.map { uid ->
                    async { getUserProfile(uid) }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting profiles by ids", e)
            emptyList()
        }
    }

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserProfile::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getSuggestedCreators(limit: Long = 10): List<UserProfile> {
        return try {
            val currentUid = auth.currentUser?.uid
            val snap = usersCollection.limit(limit).get().await()
            snap.toObjects(UserProfile::class.java).filter { it.uid != currentUid && it.uid.isNotBlank() }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        return try {
            val cleanQuery = query.removePrefix("@")
            
            val usernameSnapshot = usersCollection
                .orderBy("username")
                .startAt(cleanQuery)
                .endAt(cleanQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                
            val displayNameSnapshot = usersCollection
                .orderBy("displayName")
                .startAt(cleanQuery)
                .endAt(cleanQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                
            val results = mutableSetOf<UserProfile>()
            results.addAll(usernameSnapshot.toObjects(UserProfile::class.java))
            results.addAll(displayNameSnapshot.toObjects(UserProfile::class.java))
            
            // Also try lowercase username just in case they typed uppercase
            if (cleanQuery.any { it.isUpperCase() }) {
                val lowerQuery = cleanQuery.lowercase()
                val lowerUserSnapshot = usersCollection
                    .orderBy("username")
                    .startAt(lowerQuery)
                    .endAt(lowerQuery + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()
                results.addAll(lowerUserSnapshot.toObjects(UserProfile::class.java))
            }
            
            results.toList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error searching users", e)
            emptyList()
        }
    }

    suspend fun getFollowers(userId: String): List<UserProfile> {
        return try {
            val snapshot = usersCollection.document(userId).collection("followers").get().await()
            val ids = snapshot.documents.map { it.id }
            coroutineScope {
                ids.map { id ->
                    async { getUserProfile(id) }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting followers", e)
            emptyList()
        }
    }

    suspend fun getFollowing(userId: String): List<UserProfile> {
        return try {
            val snapshot = usersCollection.document(userId).collection("following").get().await()
            val ids = snapshot.documents.map { it.id }
            coroutineScope {
                ids.map { id ->
                    async { getUserProfile(id) }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting following", e)
            emptyList()
        }
    }

    suspend fun getMutualFollowers(currentUserId: String, targetUserId: String, limit: Long = 20): List<UserProfile> {
        return try {
            val targetFollowersSnapshot = usersCollection.document(targetUserId).collection("followers").get().await()
            val targetFollowerIds = targetFollowersSnapshot.documents.map { it.id }.toSet()

            val myFollowingSnapshot = usersCollection.document(currentUserId).collection("following").get().await()
            val myFollowingIds = myFollowingSnapshot.documents.map { it.id }.toSet()

            val mutualIds = targetFollowerIds.intersect(myFollowingIds).take(limit.toInt())

            coroutineScope {
                mutualIds.map { id ->
                    async { getUserProfile(id) }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting mutual followers", e)
            emptyList()
        }
    }

    /** Alias scoped only to username prefix — used by Flash @mention autocomplete. */
    suspend fun searchUsersByUsername(usernamePrefix: String): List<UserProfile> {
        if (usernamePrefix.isBlank()) return emptyList()
        return try {
            val snapshot = usersCollection
                .orderBy("username")
                .startAt(usernamePrefix.lowercase())
                .endAt(usernamePrefix.lowercase() + "\uf8ff")
                .limit(8)
                .get()
                .await()
            snapshot.toObjects(UserProfile::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    // ── Trending Posts ─────────────────────────────────────────────────────────
    suspend fun getTrendingWorks(limit: Long = 30): List<DiscoverPost> {
        return try {
            val snapshot = collection
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val p = doc.toObject(DiscoverPost::class.java)
                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: p?.commentsCount?.toLong() ?: 0L).toInt()
                p?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else (p.id),
                    commentsCount = cCount,
                    commentCount = cCount
                )
            }.filter { !it.isArchived && it.likesCount > 100 && it.effectiveCommentsCount >= 5 }

            val flashes = try {
                val fSnap = db.collection("flash_videos")
                    .orderBy("likesCount", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()
                fSnap.documents.mapNotNull { doc ->
                    val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                    if (fv != null && !fv.isArchived) {
                        val mapped = mapFlashToDiscoverPost(fv, doc.id)
                        if (mapped.likesCount > 100 && mapped.effectiveCommentsCount >= 5) mapped else null
                    } else null
                }
            } catch (_: Exception) {
                emptyList()
            }

            (posts + flashes)
                .distinctBy { it.id }
                .filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
                .sortedByDescending { it.likesCount }
                .take(limit.toInt())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching trending works", e)
            emptyList()
        }
    }

    suspend fun incrementShareCount(postId: String, userId: String) {
        if (postId.isBlank() || userId.isBlank()) return
        try {
            val docRef = collection.document(postId)
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
            Log.w("DiscoverRepository", "Could not increment share count", e)
        }
    }


    suspend fun updatePostCounterVisibility(
        postId: String,
        hideLikesCount: Boolean? = null,
        hideSharesCount: Boolean? = null,
        hideRemixesCount: Boolean? = null,
        hideDownloadsCount: Boolean? = null,
        hideSavesCount: Boolean? = null
    ): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>()
            hideLikesCount?.let { updates["hideLikesCount"] = it }
            hideSharesCount?.let { updates["hideSharesCount"] = it }
            hideRemixesCount?.let { updates["hideRemixesCount"] = it }
            hideDownloadsCount?.let { updates["hideDownloadsCount"] = it }
            hideSavesCount?.let { updates["hideSavesCount"] = it }
            if (updates.isNotEmpty()) {
                collection.document(postId).update(updates).await()
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error updating post counter visibility", e)
            false
        }
    }

    // ── Save / Bookmark ────────────────────────────────────────────────────────
    suspend fun getSavedWorkIds(userId: String): Set<String> {
        return try {
            val snapshot = usersCollection.document(userId).collection("saved_posts").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptySet()
        }
    }

    suspend fun setSave(workId: String, userId: String, targetSaved: Boolean): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        return try {
            val saveRef = usersCollection.document(userId).collection("saved_posts").document(workId)
            val workRef = collection.document(workId)
            db.runTransaction { transaction ->
                val saveDoc = transaction.get(saveRef)
                val alreadySaved = saveDoc.exists()
                if (alreadySaved == targetSaved) {
                    return@runTransaction true // idempotent success
                }
                val workDoc = transaction.get(workRef)
                val currentSaves = if (workDoc.exists()) (workDoc.getLong("saveCount") ?: 0L) else 0L
                if (targetSaved) {
                    transaction.set(saveRef, mapOf("savedAt" to System.currentTimeMillis()))
                    if (workDoc.exists()) {
                        transaction.update(
                            workRef,
                            "savedBy", FieldValue.arrayUnion(userId),
                            "saveCount", currentSaves + 1
                        )
                    }
                } else {
                    transaction.delete(saveRef)
                    if (workDoc.exists()) {
                        transaction.update(
                            workRef,
                            "savedBy", FieldValue.arrayRemove(userId),
                            "saveCount", maxOf(0, currentSaves - 1)
                        )
                    }
                }
                true
            }.await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error setting save", e)
            false
        }
    }

    suspend fun toggleSave(workId: String, userId: String): Boolean {
        val savedIds = getSavedWorkIds(userId)
        val currentlySaved = savedIds.contains(workId)
        return setSave(workId, userId, !currentlySaved)
    }

    // ── Activity History (Likes, Saves, Shares) ──────────────────────────────
    suspend fun getLikedWorks(userId: String): List<DiscoverPost> {
        return try {
            val snapshot = usersCollection.document(userId).collection("liked_posts")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val postIds = snapshot.documents.map { it.id }
            if (postIds.isNotEmpty()) {
                coroutineScope {
                    postIds.map { id ->
                        async { fetchPostById(id) }
                    }.awaitAll().filterNotNull().filter { !it.isArchived }
                }
            } else {
                // Fallback: check recent works that user has liked
                val recentWorks = getRecentWorks(limit = 40)
                coroutineScope {
                    recentWorks.map { work ->
                        async { if (hasLiked(work.id, userId)) work else null }
                    }.awaitAll().filterNotNull()
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching liked works", e)
            emptyList()
        }
    }

    suspend fun getSavedWorks(userId: String): List<DiscoverPost> {
        return try {
            val snapshot = usersCollection.document(userId).collection("saved_posts")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val postIds = snapshot.documents.map { it.id }
            if (postIds.isNotEmpty()) {
                coroutineScope {
                    postIds.map { id ->
                        async { fetchPostById(id) }
                    }.awaitAll().filterNotNull().filter { !it.isArchived }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching saved works", e)
            emptyList()
        }
    }

    suspend fun getSharedWorks(userId: String): List<DiscoverPost> {
        return try {
            val snapshot = usersCollection.document(userId).collection("shared_posts")
                .orderBy("sharedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val postIds = snapshot.documents.map { it.id }
            if (postIds.isNotEmpty()) {
                coroutineScope {
                    postIds.map { id ->
                        async { fetchPostById(id) }
                    }.awaitAll().filterNotNull().filter { !it.isArchived }
                }
            } else {
                // Also fallback to works created / shared by this user
                getWorksByUser(userId)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching shared works", e)
            emptyList()
        }
    }

    suspend fun recordShare(workId: String, userId: String) {
        try {
            val sharedRef = usersCollection.document(userId).collection("shared_posts").document(workId)
            sharedRef.set(mapOf("sharedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            incrementShareCount(workId, userId)
        } catch (e: Exception) {
            Log.w("DiscoverRepository", "Could not record share", e)
        }
    }

    // ── Report ─────────────────────────────────────────────────────────────────
    suspend fun reportWork(workId: String, reporterId: String, reason: String): Boolean {
        return try {
            db.collection("reports").document().set(
                mapOf(
                    "workId" to workId,
                    "reporterId" to reporterId,
                    "reason" to reason,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }
}
