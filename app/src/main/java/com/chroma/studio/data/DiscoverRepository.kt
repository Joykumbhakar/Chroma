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

import com.chroma.studio.model.*
import com.chroma.studio.data.CloudinaryClient

/** Returns true for admin/staff account roles that client profile edits must never alter. */
fun isPrivilegedAccountRole(role: String?): Boolean {
    return role?.trim() in setOf("Admin", "Moderator", "SuperAdmin")
}

/** Resolves the safe persisted role during client profile identity updates. */
fun resolveSafeProfileRole(existingRole: String?, requestedRole: String?): String {
    val current = existingRole?.trim().orEmpty()
    if (isPrivilegedAccountRole(current)) return current
    val requested = requestedRole?.trim().orEmpty()
    val allowedCreatorRoles = setOf(
        "", "Creator", "Developer", "Designer", "Photographer",
        "Video Editor", "Manager", "Student", "Artist", "Other"
    )
    return if (requested in allowedCreatorRoles) requested else current.ifBlank { "Creator" }
}

/** Validates handle format for Profile Identity edits. */
fun validateProfileHandleUpdate(displayName: String, username: String): String? {
    val cleanName = displayName.trim()
    val cleanUser = username.trim().lowercase()
    return when {
        cleanName.isBlank() -> "Display name cannot be empty"
        cleanUser.length !in 3..30 -> "Username must be between 3 and 30 characters"
        !cleanUser.matches(Regex("^[a-z0-9._]+$")) ->
            "Username can only contain lowercase letters, numbers, periods, and underscores"
        else -> null
    }
}

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

    fun mapFlashToDiscoverPost(fv: com.chroma.studio.model.FlashVideo, docId: String = ""): DiscoverPost {
        return DiscoverPost(
            id = docId.ifBlank { fv.id },
            creatorId = fv.creatorId,
            authorUsername = fv.authorUsername,
            authorDisplayName = fv.authorDisplayName,
            authorAvatarUrl = fv.authorAvatarUrl,
            authorVerified = fv.authorVerified,
            authorVerifiedTone = fv.authorVerifiedTone,
            title = fv.caption.ifBlank { "Flash Video" },
            description = fv.caption,
            aspectRatio = "9:16",
            videoUrl = fv.videoUrl,
            thumbnailUrl = fv.thumbnailUrl,
            postType = "video",
            tags = fv.hashtags,
            mentions = fv.mentions,
            collaborators = fv.collaborators,
            pendingCollaboratorIds = fv.pendingCollaboratorIds,
            collaboratorIds = fv.collaboratorIds,
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
            turnOffRemix = fv.turnOffRemix,
            isArchived = fv.isArchived,
            isTrending = fv.isTrending,
            trendingExcluded = fv.trendingExcluded,
            trendingOrder = fv.trendingOrder,
            trendingTag = fv.trendingTag,
            isFeatured = fv.isFeatured,
            isTakedown = fv.isTakedown
        )
    }

    private suspend fun hydrateAuthorProfiles(posts: List<DiscoverPost>): List<DiscoverPost> {
        if (posts.isEmpty()) return posts
        val creatorIds = posts.map { it.creatorId }
        val collabIds = posts.flatMap { it.collaborators.map { c -> c.uid } }
        val allUserIds = (creatorIds + collabIds).filter { it.isNotBlank() }.distinct()
        if (allUserIds.isEmpty()) return posts
        return try {
            val profilesMap = getProfilesByIds(allUserIds, limit = allUserIds.size).associateBy { it.uid }
            posts.map { post ->
                val profile = profilesMap[post.creatorId]
                val hydratedCollabs = if (post.collaborators.isEmpty()) {
                    post.collaborators
                } else {
                    post.collaborators.map { collab ->
                        val cProf = profilesMap[collab.uid]
                        if (cProf != null) {
                            com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(cProf)
                            com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(cProf)
                            collab.copy(
                                username = cProf.username.ifBlank { collab.username },
                                displayName = cProf.displayName.ifBlank { collab.displayName },
                                avatarUrl = cProf.avatarUrl.ifBlank { collab.avatarUrl },
                                verified = cProf.verified,
                                verifiedTone = cProf.verifiedTone ?: collab.verifiedTone
                            )
                        } else {
                            collab
                        }
                    }
                }
                if (profile != null) {
                    com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
                    com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(profile)
                    post.copy(
                        authorUsername = profile.username.ifBlank { post.authorUsername },
                        authorDisplayName = profile.displayName.ifBlank { post.authorDisplayName },
                        authorAvatarUrl = profile.avatarUrl.ifBlank { post.authorAvatarUrl },
                        authorVerified = profile.verified,
                        authorVerifiedTone = profile.verifiedTone ?: post.authorVerifiedTone,
                        collaborators = hydratedCollabs
                    )
                } else {
                    if (post.authorVerified && !post.authorVerifiedTone.isNullOrBlank()) {
                        com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(post.creatorId, post.authorVerifiedTone)
                    }
                    post.copy(collaborators = hydratedCollabs)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            posts
        }
    }

    suspend fun getRecentWorks(limit: Long = 30, lastTimestamp: Long? = null): List<DiscoverPost> {
        return try {
            var query = collection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }
            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(DiscoverPost::class.java)
                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: obj?.commentsCount?.toLong() ?: 0L).toInt()
                obj?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else obj.id,
                    commentsCount = cCount,
                    commentCount = cCount
                )
            }.filter { !it.isArchived && !it.isTakedown }

            val flashes = try {
                var flashQuery = db.collection("flash_videos").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
                if (lastTimestamp != null) {
                    flashQuery = flashQuery.startAfter(lastTimestamp)
                }
                val fSnap = flashQuery.get().await()
                fSnap.documents.mapNotNull { doc ->
                    val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                    if (fv != null && !fv.isArchived && !fv.isTakedown) mapFlashToDiscoverPost(fv, doc.id) else null
                }
            } catch (_: Exception) {
                emptyList()
            }

            val combined = (posts + flashes)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
                .take(limit.toInt())

            val finalRecent = hydrateAuthorProfiles(combined)
            if (lastTimestamp == null && finalRecent.isNotEmpty()) {
                OfflineDataCacheManager.saveRecentPosts(com.chroma.studio.ChromaApp.instance, finalRecent)
            }
            finalRecent
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching community works", e)
            if (lastTimestamp == null) {
                OfflineDataCacheManager.getRecentPosts(com.chroma.studio.ChromaApp.instance)
            } else emptyList()
        }
    }

    /**
     * Fetches a diverse multi-source candidate pool (latest creations, top-liked evergreen posts,
     * Flash videos, and a seed-rotated older window slice) so the Community discovery ranker can
     * surface varied content rather than returning only the newest 10 chronological posts.
     */
    suspend fun getDiscoveryWorks(limit: Long = 45, rotationSeed: Long = 0L): List<DiscoverPost> {
        return try {
            coroutineScope {
                val recentPostsDef = async {
                    try {
                        collection.orderBy("timestamp", Query.Direction.DESCENDING).limit(25).get().await()
                            .documents.mapNotNull { doc ->
                                val obj = doc.toObject(DiscoverPost::class.java)
                                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: obj?.commentsCount?.toLong() ?: 0L).toInt()
                                obj?.copy(
                                    id = if (doc.id.isNotBlank()) doc.id else obj.id,
                                    commentsCount = cCount,
                                    commentCount = cCount
                                )
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }

                val recentFlashesDef = async {
                    try {
                        db.collection("flash_videos").orderBy("timestamp", Query.Direction.DESCENDING).limit(15).get().await()
                            .documents.mapNotNull { doc ->
                                val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                                if (fv != null) mapFlashToDiscoverPost(fv, doc.id) else null
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }

                val popularPostsDef = async {
                    try {
                        collection.orderBy("likesCount", Query.Direction.DESCENDING).limit(20).get().await()
                            .documents.mapNotNull { doc ->
                                val obj = doc.toObject(DiscoverPost::class.java)
                                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: obj?.commentsCount?.toLong() ?: 0L).toInt()
                                obj?.copy(
                                    id = if (doc.id.isNotBlank()) doc.id else obj.id,
                                    commentsCount = cCount,
                                    commentCount = cCount
                                )
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }

                val archiveSliceDef = async {
                    try {
                        // Rotate an older cursor cutoff between 1 and 28 days back using rotationSeed
                        val windowDays = ((kotlin.math.abs(rotationSeed) % 27L) + 1L)
                        val cutoffMs = System.currentTimeMillis() - (windowDays * 86_400_000L)
                        collection.orderBy("timestamp", Query.Direction.DESCENDING)
                            .startAfter(cutoffMs)
                            .limit(15)
                            .get().await()
                            .documents.mapNotNull { doc ->
                                val obj = doc.toObject(DiscoverPost::class.java)
                                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: obj?.commentsCount?.toLong() ?: 0L).toInt()
                                obj?.copy(
                                    id = if (doc.id.isNotBlank()) doc.id else obj.id,
                                    commentsCount = cCount,
                                    commentCount = cCount
                                )
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }

                val combined = (
                    recentPostsDef.await() +
                        recentFlashesDef.await() +
                        popularPostsDef.await() +
                        archiveSliceDef.await()
                    )
                    .distinctBy { it.id }
                    .filter { !it.isArchived && !it.isTakedown }
                    .take(limit.toInt())

                val hydrated = hydrateAuthorProfiles(combined)
                if (hydrated.isNotEmpty()) {
                    OfflineDataCacheManager.saveRecentPosts(com.chroma.studio.ChromaApp.instance, hydrated)
                }
                hydrated
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching discovery pool", e)
            OfflineDataCacheManager.getRecentPosts(com.chroma.studio.ChromaApp.instance)
        }
    }

    suspend fun publishWork(
        work: ChromaWork,
        aspectRatio: String = "4:3",
        tags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        collaborators: List<CollabParticipant> = emptyList(),
        musicTrack: AttachedMusicSelection? = null
    ) {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")

        val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
        val pendingCollabIds = normalizedCollabs.map { it.uid }

        val newWorkId = collection.document().id
        val communityWork = DiscoverPost(
            id = newWorkId,
            title = work.name,
            description = work.description,
            jsonUrl = com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson),
            postType = "work",
            tags = tags,
            mentions = mentions,
            collaborators = normalizedCollabs,
            pendingCollaboratorIds = pendingCollabIds,
            collaboratorIds = emptyList(),
            creatorId = user.uid,
            authorUsername = userProfile.username,
            authorDisplayName = userProfile.displayName,
            authorAvatarUrl = userProfile.avatarUrl,
            authorVerified = userProfile.verified,
            authorVerifiedTone = userProfile.verifiedTone,
            aspectRatio = aspectRatio,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            remixedFromPostId = work.remixedFromPostId,
            remixedFromAuthorId = work.remixedFromAuthorId,
            remixedFromAuthorUsername = work.remixedFromAuthorUsername,
            remixedFromAuthorDisplayName = work.remixedFromAuthorDisplayName,
            remixedFromAuthorAvatar = work.remixedFromAuthorAvatar,
            remixedFromAuthorVerified = work.remixedFromAuthorVerified,
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
        collection.document(newWorkId).set(communityWork).await()
        if (musicTrack != null) {
            try { MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId) } catch (_: Exception) {}
        }

        try {
            if (pendingCollabIds.isNotEmpty()) {
                NotificationDispatcher.dispatchCollabInviteNotifications(
                    invitedUids = pendingCollabIds,
                    postId = newWorkId,
                    isFlash = false,
                    titleSnippet = work.name
                )
            }
            NotificationDispatcher.dispatchMentionNotifications(
                texts = listOf(work.name, work.description),
                explicitMentions = mentions,
                surface = MentionSurface.POST,
                targetId = newWorkId,
                detailSnippet = work.description.ifBlank { work.name }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }

        // Increment remix count on the original post if this is a remix (1 count per user)
        if (!work.remixedFromPostId.isNullOrEmpty()) {
            incrementRemixCount(work.remixedFromPostId, user.uid)
        }
    }

    suspend fun repostWithRemix(
        originalPost: DiscoverPost,
        remixTitle: String,
        remixDescription: String,
        tags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        collaborators: List<CollabParticipant> = emptyList()
    ): DiscoverPost {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")

        val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
        val pendingCollabIds = normalizedCollabs.map { it.uid }

        val newWorkId = collection.document().id
        val remixedPost = DiscoverPost(
            id = newWorkId,
            title = remixTitle.ifBlank { "Remix: ${originalPost.title.ifBlank { "Creation" }}" },
            description = remixDescription,
            postType = originalPost.postType,
            jsonUrl = originalPost.jsonUrl,
            imageUrl = originalPost.imageUrl,
            imageUrls = originalPost.imageUrls,
            workUrls = originalPost.workUrls,
            videoUrl = originalPost.videoUrl,
            thumbnailUrl = originalPost.thumbnailUrl,
            aspectRatio = originalPost.aspectRatio,
            tags = if (tags.isNotEmpty()) tags else originalPost.tags,
            mentions = mentions,
            collaborators = normalizedCollabs,
            pendingCollaboratorIds = pendingCollabIds,
            collaboratorIds = emptyList(),
            creatorId = user.uid,
            authorUsername = userProfile.username,
            authorDisplayName = userProfile.displayName,
            authorAvatarUrl = userProfile.avatarUrl,
            authorVerified = userProfile.verified,
            authorVerifiedTone = userProfile.verifiedTone,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            likedBy = emptyList(),
            remixCount = 0,
            remixedBy = emptyList(),
            shareCount = 0,
            sharedBy = emptyList(),
            saveCount = 0,
            savedBy = emptyList(),
            commentsCount = 0,
            commentCount = 0,
            remixedFromPostId = originalPost.id,
            remixedFromAuthorId = originalPost.creatorId,
            remixedFromAuthorUsername = originalPost.authorUsername,
            remixedFromAuthorDisplayName = originalPost.authorDisplayName,
            remixedFromAuthorAvatar = originalPost.authorAvatarUrl,
            remixedFromAuthorVerified = originalPost.authorVerified
        )

        collection.document(newWorkId).set(remixedPost).await()

        val isFlashRemix = originalPost.postType == "video" || !originalPost.videoUrl.isNullOrBlank()
        if (isFlashRemix) {
            try {
                val flashDoc = db.collection("flash_videos").document(newWorkId)
                val flashVideo = com.chroma.studio.model.FlashVideo(
                    id = newWorkId,
                    creatorId = user.uid,
                    authorUsername = userProfile.username,
                    authorDisplayName = userProfile.displayName,
                    authorAvatarUrl = userProfile.avatarUrl,
                    authorVerified = userProfile.verified,
                    authorVerifiedTone = userProfile.verifiedTone,
                    videoUrl = originalPost.videoUrl ?: "",
                    thumbnailUrl = originalPost.thumbnailUrl ?: originalPost.imageUrl ?: "",
                    caption = "${remixTitle.ifBlank { "Remix" }}: $remixDescription".trim(),
                    hashtags = if (tags.isNotEmpty()) tags else originalPost.tags,
                    mentions = mentions,
                    collaborators = normalizedCollabs,
                    pendingCollaboratorIds = pendingCollabIds,
                    collaboratorIds = emptyList(),
                    likesCount = 0,
                    likedBy = emptyList(),
                    commentCount = 0,
                    remixCount = 0,
                    remixedBy = emptyList(),
                    shareCount = 0,
                    sharedBy = emptyList(),
                    saveCount = 0,
                    savedBy = emptyList(),
                    remixedFromPostId = originalPost.id,
                    remixedFromAuthorUsername = originalPost.authorUsername,
                    timestamp = System.currentTimeMillis()
                )
                flashDoc.set(flashVideo).await()
            } catch (e: Exception) {
                Log.w("DiscoverRepository", "Flash video mirror write skipped: ${e.message}")
            }
        }

        incrementRemixCount(originalPost.id, user.uid)

        // Dispatch real-time REMIX notification to original creator
        try {
            if (!originalPost.creatorId.isNullOrBlank() && originalPost.creatorId != user.uid) {
                NotificationDispatcher.sendNotification(
                    targetUserId = originalPost.creatorId,
                    type = com.chroma.studio.model.NotificationActionType.REMIX,
                    actionText = "remixed your artwork",
                    detailText = remixTitle.ifBlank { originalPost.title },
                    targetPreviewUrl = remixedPost.imageUrl ?: originalPost.imageUrl,
                    targetId = newWorkId
                )
            }
        } catch (e: Exception) {
            Log.w("DiscoverRepository", "Failed to dispatch remix notification: ${e.message}")
        }

        try {
            if (pendingCollabIds.isNotEmpty()) {
                NotificationDispatcher.dispatchCollabInviteNotifications(
                    invitedUids = pendingCollabIds,
                    postId = newWorkId,
                    isFlash = isFlashRemix,
                    previewUrl = remixedPost.thumbnailUrl ?: remixedPost.imageUrl ?: originalPost.imageUrl,
                    titleSnippet = remixTitle.ifBlank { originalPost.title }
                )
            }
            NotificationDispatcher.dispatchMentionNotifications(
                texts = listOf(remixTitle, remixDescription),
                explicitMentions = mentions,
                surface = MentionSurface.POST,
                targetId = newWorkId,
                targetPreviewUrl = remixedPost.imageUrl ?: originalPost.imageUrl,
                detailSnippet = remixDescription.ifBlank { remixTitle }
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }

        return remixedPost
    }

    suspend fun uploadImagePost(
        imageBytes: ByteArray,
        caption: String,
        description: String,
        aspectRatio: String,
        tags: List<String>,
        mentions: List<String>,
        collaborators: List<CollabParticipant> = emptyList(),
        musicTrack: AttachedMusicSelection? = null
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
            val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
            val pendingCollabIds = normalizedCollabs.map { it.uid }

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
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                authorVerifiedTone = userProfile.verifiedTone,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
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
            collection.document(newWorkId).set(communityWork).await()
            if (musicTrack != null) {
                try { MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId) } catch (_: Exception) {}
            }
            try {
                if (pendingCollabIds.isNotEmpty()) {
                    NotificationDispatcher.dispatchCollabInviteNotifications(
                        invitedUids = pendingCollabIds,
                        postId = newWorkId,
                        isFlash = false,
                        previewUrl = imageUrl,
                        titleSnippet = caption.ifBlank { description }
                    )
                }
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(caption, description),
                    explicitMentions = mentions,
                    surface = MentionSurface.POST,
                    targetId = newWorkId,
                    targetPreviewUrl = imageUrl,
                    detailSnippet = description.ifBlank { caption }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
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
        turnOffRemix: Boolean = false,
        collaborators: List<CollabParticipant> = emptyList(),
        musicTrack: AttachedMusicSelection? = null
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
            val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
            val pendingCollabIds = normalizedCollabs.map { it.uid }

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
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                authorVerifiedTone = userProfile.verifiedTone,
                aspectRatio = aspectRatio,
                trendingTag = categoryTag,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                hideLikesCount = hideLikesCount,
                hideRemixesCount = turnOffRemix,
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
            collection.document(newWorkId).set(communityWork).await()
            if (musicTrack != null) {
                try { MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId) } catch (_: Exception) {}
            }
            try {
                if (pendingCollabIds.isNotEmpty()) {
                    NotificationDispatcher.dispatchCollabInviteNotifications(
                        invitedUids = pendingCollabIds,
                        postId = newWorkId,
                        isFlash = false,
                        previewUrl = uploadedUrls.firstOrNull(),
                        titleSnippet = caption.ifBlank { description }
                    )
                }
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(caption, description),
                    explicitMentions = mentions,
                    surface = MentionSurface.POST,
                    targetId = newWorkId,
                    targetPreviewUrl = uploadedUrls.firstOrNull(),
                    detailSnippet = description.ifBlank { caption }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
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
        turnOffRemix: Boolean = false,
        collaborators: List<CollabParticipant> = emptyList(),
        musicTrack: AttachedMusicSelection? = null
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
            val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
            val pendingCollabIds = normalizedCollabs.map { it.uid }

            val compressedJsonList = works.map { work ->
                com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson)
            }

            val newWorkId = collection.document().id
            val allTags = if (!categoryTag.isNullOrBlank()) (tags + categoryTag).distinct() else tags
            val primaryWork = works.firstOrNull()
            val resolvedTitle = if (caption.isNotBlank()) caption else (primaryWork?.name ?: "Untitled Work")
            val communityWork = DiscoverPost(
                id = newWorkId,
                title = resolvedTitle,
                description = if (description.isNotBlank()) description else (primaryWork?.description ?: ""),
                jsonUrl = compressedJsonList.firstOrNull() ?: "",
                workUrls = compressedJsonList,
                postType = "work",
                tags = allTags,
                mentions = mentions,
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                authorVerifiedTone = userProfile.verifiedTone,
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
                remixedFromAuthorVerified = primaryWork?.remixedFromAuthorVerified ?: false,
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
            collection.document(newWorkId).set(communityWork).await()
            if (musicTrack != null) {
                try { MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId) } catch (_: Exception) {}
            }
            try {
                if (pendingCollabIds.isNotEmpty()) {
                    NotificationDispatcher.dispatchCollabInviteNotifications(
                        invitedUids = pendingCollabIds,
                        postId = newWorkId,
                        isFlash = false,
                        titleSnippet = resolvedTitle
                    )
                }
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(caption, description, primaryWork?.name, primaryWork?.description),
                    explicitMentions = mentions,
                    surface = MentionSurface.POST,
                    targetId = newWorkId,
                    detailSnippet = description.ifBlank { caption }.ifBlank { primaryWork?.name }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
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
        hideDownloadsCount: Boolean = false,
        collaborators: List<CollabParticipant> = emptyList()
    ): Boolean {
        return try {
            val user = auth.currentUser ?: throw Exception("User not authenticated")
            val userProfile = getUserProfile(user.uid) ?: throw Exception("User profile not found")
            val normalizedCollabs = normalizeInvitedCollaborators(user.uid, collaborators)
            val pendingCollabIds = normalizedCollabs.map { it.uid }

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
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                creatorId = user.uid,
                authorUsername = userProfile.username,
                authorDisplayName = userProfile.displayName,
                authorAvatarUrl = userProfile.avatarUrl,
                authorVerified = userProfile.verified,
                authorVerifiedTone = userProfile.verifiedTone,
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
            try {
                if (pendingCollabIds.isNotEmpty()) {
                    NotificationDispatcher.dispatchCollabInviteNotifications(
                        invitedUids = pendingCollabIds,
                        postId = newWorkId,
                        isFlash = false,
                        previewUrl = thumbnailUrl.ifBlank { null },
                        titleSnippet = caption.ifBlank { description }
                    )
                }
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(caption, description),
                    explicitMentions = mentions,
                    surface = MentionSurface.POST,
                    targetId = newWorkId,
                    targetPreviewUrl = thumbnailUrl.ifBlank { null },
                    detailSnippet = description.ifBlank { caption }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
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
        if (postId.isBlank()) return null
        return try {
            val doc = collection.document(postId).get().await()
            val fromWork = if (doc.exists()) {
                doc.toObject(DiscoverPost::class.java)?.copy(id = doc.id)
            } else null

            val resolved = fromWork ?: try {
                val fDoc = db.collection("flash_videos").document(postId).get().await()
                val fv = if (fDoc.exists()) fDoc.toObject(com.chroma.studio.model.FlashVideo::class.java) else null
                if (fv != null) mapFlashToDiscoverPost(fv, fDoc.id) else null
            } catch (_: Exception) {
                null
            } ?: return null

            hydrateAuthorProfiles(listOf(resolved)).firstOrNull() ?: resolved
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching post by ID", e)
            null
        }
    }

    suspend fun getWorksByUser(creatorId: String): List<DiscoverPost> {
        return try {
            val ownSnap = collection
                .whereEqualTo("creatorId", creatorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val ownPosts = ownSnap.documents.mapNotNull { doc ->
                doc.toObject(DiscoverPost::class.java)?.copy(
                    id = if (doc.id.isNotBlank()) doc.id else (doc.toObject(DiscoverPost::class.java)?.id ?: "")
                )
            }.filter { !it.isArchived && !it.isTakedown }

            val collabPosts = try {
                collection
                    .whereArrayContains("collaboratorIds", creatorId)
                    .get()
                    .await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(DiscoverPost::class.java)?.copy(
                            id = if (doc.id.isNotBlank()) doc.id else (doc.toObject(DiscoverPost::class.java)?.id ?: "")
                        )
                    }.filter { !it.isArchived && !it.isTakedown }
            } catch (_: Exception) {
                emptyList()
            }

            val ownFlashes = try {
                db.collection("flash_videos")
                    .whereEqualTo("creatorId", creatorId)
                    .get()
                    .await()
                    .documents.mapNotNull { doc ->
                        val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                        if (fv != null && !fv.isArchived && !fv.isTakedown) mapFlashToDiscoverPost(fv, doc.id) else null
                    }
            } catch (_: Exception) {
                emptyList()
            }

            val collabFlashes = try {
                db.collection("flash_videos")
                    .whereArrayContains("collaboratorIds", creatorId)
                    .get()
                    .await()
                    .documents.mapNotNull { doc ->
                        val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                        if (fv != null && !fv.isArchived && !fv.isTakedown) mapFlashToDiscoverPost(fv, doc.id) else null
                    }
            } catch (_: Exception) {
                emptyList()
            }

            val combined = mergeCreatorAndCollabPosts(
                creatorPosts = ownPosts + ownFlashes,
                collabPosts = collabPosts + collabFlashes
            )
            val sortedWorks = hydrateAuthorProfiles(combined)
            if (sortedWorks.isNotEmpty()) {
                OfflineDataCacheManager.saveUserPosts(com.chroma.studio.ChromaApp.instance, creatorId, sortedWorks)
            }
            sortedWorks
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching works for user", e)
            OfflineDataCacheManager.getUserPosts(com.chroma.studio.ChromaApp.instance, creatorId)
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
            collection.document(postId).update("archived", archive).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error archiving post", e)
            throw e
        }
    }

    suspend fun shareWork(
        work: ChromaWork,
        authorName: String,
        authorUsername: String,
        creatorId: String,
        authorAvatarUrl: String,
        authorVerified: Boolean,
        aspectRatio: String,
        collaborators: List<CollabParticipant> = emptyList(),
        musicTrack: AttachedMusicSelection? = null
    ): Boolean {
        return try {
            val normalizedCollabs = normalizeInvitedCollaborators(creatorId, collaborators)
            val pendingCollabIds = normalizedCollabs.map { it.uid }
            val communityWork = DiscoverPost(
                id = work.id, 
                title = work.name,
                description = work.description,
                jsonUrl = com.chroma.studio.utils.DataCompressionUtils.compressText(work.layersJson),
                collaborators = normalizedCollabs,
                pendingCollaboratorIds = pendingCollabIds,
                collaboratorIds = emptyList(),
                creatorId = creatorId,
                authorUsername = authorUsername,
                authorDisplayName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                authorVerified = authorVerified,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
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
            collection.document(communityWork.id).set(communityWork).await()
            if (musicTrack != null) {
                try { MusicRepository.getInstance().incrementTrackUsage(musicTrack.trackId) } catch (_: Exception) {}
            }
            try {
                if (pendingCollabIds.isNotEmpty()) {
                    NotificationDispatcher.dispatchCollabInviteNotifications(
                        invitedUids = pendingCollabIds,
                        postId = communityWork.id,
                        isFlash = false,
                        titleSnippet = work.name
                    )
                }
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(work.name, work.description),
                    surface = MentionSurface.POST,
                    targetId = communityWork.id,
                    detailSnippet = work.description.ifBlank { work.name }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error sharing work", e)
            false
        }
    }

    suspend fun setLike(workId: String, userId: String, targetLiked: Boolean): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        // Ensure Firebase Auth token is fresh before Firestore writes
        try { auth.currentUser?.getIdToken(false)?.await() } catch (_: Exception) {}
        return try {
            val workRef = collection.document(workId)
            val flashRef = db.collection("flash_videos").document(workId)
            val likeRef = collection.document(workId).collection("likes").document(userId)
            val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(workId)
            val now = System.currentTimeMillis()

            var postAuthorId: String? = null
            var postTitle: String? = null
            var postThumb: String? = null

            var updatedAnyTarget = false
            var hadTransactionError = false

            // 1. Transactional atomic update for community_works
            try {
                val commUpdated = db.runTransaction { transaction ->
                    val postDoc = transaction.get(workRef)
                    if (postDoc.exists()) {
                        postAuthorId = postDoc.getString("creatorId")
                        postTitle = postDoc.getString("title") ?: "post"
                        postThumb = postDoc.getString("thumbnailUrl") ?: postDoc.getString("imageUrl")
                        @Suppress("UNCHECKED_CAST")
                        val likedBy = postDoc.get("likedBy") as? List<String> ?: emptyList()
                        val currentLikes = maxOf(postDoc.getLong("likesCount") ?: 0L, likedBy.size.toLong())
                        val alreadyLiked = likedBy.contains(userId)

                        if (targetLiked && !alreadyLiked) {
                            val newCount = maxOf(currentLikes + 1L, (likedBy.size + 1).toLong())
                            transaction.update(
                                workRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayUnion(userId),
                                    "likesCount" to newCount
                                )
                            )
                        } else if (!targetLiked && alreadyLiked) {
                            val newCount = maxOf(0L, currentLikes - 1L)
                            transaction.update(
                                workRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayRemove(userId),
                                    "likesCount" to newCount
                                )
                            )
                        }
                        true
                    } else {
                        false
                    }
                }.await()
                if (commUpdated) updatedAnyTarget = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                hadTransactionError = true
                Log.w("DiscoverRepository", "community_works like update transaction warning", e)
            }

            // 2. Transactional atomic update for flash_videos (if applicable)
            try {
                val flashUpdated = db.runTransaction { transaction ->
                    val flashDoc = transaction.get(flashRef)
                    if (flashDoc.exists()) {
                        if (postAuthorId.isNullOrBlank()) {
                            postAuthorId = flashDoc.getString("creatorId")
                            postTitle = flashDoc.getString("caption") ?: flashDoc.getString("title") ?: "video"
                            postThumb = flashDoc.getString("thumbnailUrl")
                        }
                        @Suppress("UNCHECKED_CAST")
                        val likedBy = flashDoc.get("likedBy") as? List<String> ?: emptyList()
                        val currentLikes = maxOf(flashDoc.getLong("likesCount") ?: 0L, likedBy.size.toLong())
                        val alreadyLiked = likedBy.contains(userId)

                        if (targetLiked && !alreadyLiked) {
                            val newCount = maxOf(currentLikes + 1L, (likedBy.size + 1).toLong())
                            transaction.update(
                                flashRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayUnion(userId),
                                    "dislikedBy" to FieldValue.arrayRemove(userId),
                                    "likesCount" to newCount
                                )
                            )
                        } else if (!targetLiked && alreadyLiked) {
                            val newCount = maxOf(0L, currentLikes - 1L)
                            transaction.update(
                                flashRef,
                                mapOf(
                                    "likedBy" to FieldValue.arrayRemove(userId),
                                    "likesCount" to newCount
                                )
                            )
                        }
                        true
                    } else {
                        false
                    }
                }.await()
                if (flashUpdated) {
                    updatedAnyTarget = true
                    if (targetLiked) {
                        try { flashRef.collection("likes").document(userId).set(mapOf("timestamp" to now)).await() } catch (_: Exception) {}
                    } else {
                        try { flashRef.collection("likes").document(userId).delete().await() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                hadTransactionError = true
                Log.w("DiscoverRepository", "flash_videos like update transaction warning", e)
            }

            // If both target collections failed to update due to an error, signal failure so UI rolls back honestly
            if (!updatedAnyTarget && hadTransactionError) {
                return false
            }

            // 3. Subcollection updates
            if (targetLiked) {
                try { likeRef.set(mapOf("timestamp" to now)).await() } catch (_: Exception) {}
                try { userLikeRef.set(mapOf("likedAt" to now)).await() } catch (_: Exception) {}

                // 4. Dispatch real notification to post creator and accepted collaborators
                try {
                    val workSnap = try { workRef.get().await() } catch (_: Exception) { null }
                    val flashSnap = if (workSnap == null || !workSnap.exists()) {
                        try { flashRef.get().await() } catch (_: Exception) { null }
                    } else null
                    @Suppress("UNCHECKED_CAST")
                    val collabIds = (workSnap?.get("collaboratorIds") as? List<String>)
                        ?: (flashSnap?.get("collaboratorIds") as? List<String>)
                        ?: emptyList()
                    val targetAuthor = postAuthorId
                    val recipients = (listOfNotNull(targetAuthor) + collabIds)
                        .filter { it.isNotBlank() && it != userId }
                        .distinct()
                    for (recipientUid in recipients) {
                        NotificationDispatcher.sendNotification(
                            targetUserId = recipientUid,
                            type = com.chroma.studio.model.NotificationActionType.LIKE,
                            actionText = if (recipientUid == targetAuthor) "liked your post" else "liked your collab post",
                            detailText = postTitle ?: "post",
                            targetPreviewUrl = postThumb,
                            targetId = workId
                        )
                    }
                } catch (_: Exception) {}
            } else {
                try { likeRef.delete().await() } catch (_: Exception) {}
                try { userLikeRef.delete().await() } catch (_: Exception) {}
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
        if (workId.isBlank()) return false
        val currentUid = auth.currentUser?.uid ?: return false
        var deletedAny = false

        // 1. Try deleting from community_works
        try {
            val commDoc = collection.document(workId).get().await()
            if (commDoc.exists()) {
                val creatorId = commDoc.getString("creatorId") ?: commDoc.getString("userId")
                if (creatorId == currentUid || creatorId.isNullOrBlank()) {
                    collection.document(workId).delete().await()
                    deletedAny = true
                    Log.d("DiscoverRepository", "Deleted $workId from community_works")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DiscoverRepository", "Error checking/deleting community_works doc: $workId", e)
        }

        // 2. Try deleting from flash_videos (cross-collection consistency)
        try {
            val flashDoc = db.collection("flash_videos").document(workId).get().await()
            if (flashDoc.exists()) {
                val creatorId = flashDoc.getString("creatorId") ?: flashDoc.getString("userId")
                if (creatorId == currentUid || creatorId.isNullOrBlank()) {
                    db.collection("flash_videos").document(workId).delete().await()
                    deletedAny = true
                    Log.d("DiscoverRepository", "Deleted $workId from flash_videos")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DiscoverRepository", "Error checking/deleting flash_videos doc: $workId", e)
        }

        // 3. Fallback direct delete if neither document was confirmed via get()
        if (!deletedAny) {
            try {
                collection.document(workId).delete().await()
                deletedAny = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            try {
                db.collection("flash_videos").document(workId).delete().await()
                deletedAny = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }

        // 4. Clean up user interactions, likes subcollections, and associated comments
        if (deletedAny) {
            try {
                collection.document(workId).collection("likes").document(currentUid).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("flash_videos").document(workId).collection("likes").document(currentUid).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("liked_posts").document(workId).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("saved_posts").document(workId).delete().await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(currentUid).collection("shared_posts").document(workId).delete().await()
            } catch (_: Exception) {}
            try {
                val commentsSnapshot = db.collection("flash_comments")
                    .whereEqualTo("videoId", workId)
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

    suspend fun updateWork(workId: String, newTitle: String, newDescription: String): Boolean {
        return try {
            val existingDoc = try { collection.document(workId).get().await() } catch (_: Exception) { null }
            val oldTitle = existingDoc?.getString("title")
            val oldDescription = existingDoc?.getString("description")
            val previewUrl = existingDoc?.getString("thumbnailUrl") ?: existingDoc?.getString("imageUrl")

            collection.document(workId).update(
                mapOf(
                    "title" to newTitle,
                    "description" to newDescription
                )
            ).await()

            try {
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(newTitle, newDescription),
                    previousTexts = listOf(oldTitle, oldDescription),
                    surface = MentionSurface.POST,
                    targetId = workId,
                    targetPreviewUrl = previewUrl,
                    detailSnippet = newDescription.ifBlank { newTitle }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error updating work", e)
            false
        }
    }

    private fun serializeCollabParticipants(list: List<CollabParticipant>): List<Map<String, Any?>> =
        list.map { p ->
            mapOf(
                "uid" to p.uid,
                "username" to p.username,
                "displayName" to p.displayName,
                "avatarUrl" to p.avatarUrl,
                "verified" to p.verified,
                "verifiedTone" to p.verifiedTone,
                "status" to p.status,
                "invitedAt" to p.invitedAt,
                "respondedAt" to p.respondedAt
            )
        }

    /**
     * Accepts or declines a pending collaboration invite for [postId] atomically across `community_works` and `flash_videos`.
     */
    suspend fun respondToCollabInvite(postId: String, accept: Boolean): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (postId.isBlank()) return false
        return try {
            val workRef = collection.document(postId)
            val flashRef = db.collection("flash_videos").document(postId)
            val workSnap = try { workRef.get().await() } catch (_: Exception) { null }
            val flashSnap = try { flashRef.get().await() } catch (_: Exception) { null }
            val sourceSnap = workSnap?.takeIf { it.exists() } ?: flashSnap?.takeIf { it.exists() } ?: return false

            val sourcePost = sourceSnap.toObject(DiscoverPost::class.java)
            val flashObj = if (sourcePost == null || !workSnap?.exists()!!) {
                flashSnap?.toObject(com.chroma.studio.model.FlashVideo::class.java)
            } else null

            val currentCollabs = sourcePost?.collaborators ?: flashObj?.collaborators ?: emptyList()
            val currentPending = sourcePost?.pendingCollaboratorIds ?: flashObj?.pendingCollaboratorIds ?: emptyList()
            val currentAccepted = sourcePost?.collaboratorIds ?: flashObj?.collaboratorIds ?: emptyList()
            val creatorUid = sourcePost?.creatorId ?: flashObj?.creatorId.orEmpty()
            val previewUrl = sourcePost?.thumbnailUrl ?: sourcePost?.imageUrl ?: flashObj?.thumbnailUrl
            val isFlash = (flashSnap != null && flashSnap.exists()) || sourcePost?.postType == "video"

            val nextState = applyCollabInviteResponse(
                participants = currentCollabs,
                pendingIds = currentPending,
                acceptedIds = currentAccepted,
                responderUid = currentUid,
                accept = accept
            )
            if (!nextState.changed) return false

            val updates = mapOf(
                "collaborators" to serializeCollabParticipants(nextState.collaborators),
                "pendingCollaboratorIds" to nextState.pendingCollaboratorIds,
                "collaboratorIds" to nextState.collaboratorIds
            )
            val batch = db.batch()
            if (workSnap != null && workSnap.exists()) batch.update(workRef, updates)
            if (flashSnap != null && flashSnap.exists()) batch.update(flashRef, updates)
            batch.commit().await()

            try {
                NotificationDispatcher.dispatchCollabResponseNotification(
                    creatorUid = creatorUid,
                    postId = postId,
                    isFlash = isFlash,
                    accepted = accept,
                    previewUrl = previewUrl
                )
            } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "respondToCollabInvite failed for $postId", e)
            false
        }
    }

    /**
     * Allows an accepted collaborator to opt out ("Stop sharing"), removing the shared post only from their profile.
     */
    suspend fun leaveCollab(postId: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (postId.isBlank()) return false
        return try {
            val workRef = collection.document(postId)
            val flashRef = db.collection("flash_videos").document(postId)
            val workSnap = try { workRef.get().await() } catch (_: Exception) { null }
            val flashSnap = try { flashRef.get().await() } catch (_: Exception) { null }
            val sourceSnap = workSnap?.takeIf { it.exists() } ?: flashSnap?.takeIf { it.exists() } ?: return false

            val sourcePost = sourceSnap.toObject(DiscoverPost::class.java)
            val flashObj = if (sourcePost == null) flashSnap?.toObject(com.chroma.studio.model.FlashVideo::class.java) else null

            val currentCollabs = sourcePost?.collaborators ?: flashObj?.collaborators ?: emptyList()
            val currentPending = sourcePost?.pendingCollaboratorIds ?: flashObj?.pendingCollaboratorIds ?: emptyList()
            val currentAccepted = sourcePost?.collaboratorIds ?: flashObj?.collaboratorIds ?: emptyList()

            val nextState = removeCollabParticipant(
                participants = currentCollabs,
                pendingIds = currentPending,
                acceptedIds = currentAccepted,
                targetUid = currentUid
            )
            if (!nextState.changed) return false

            val updates = mapOf(
                "collaborators" to serializeCollabParticipants(nextState.collaborators),
                "collaboratorIds" to nextState.collaboratorIds
            )
            val batch = db.batch()
            if (workSnap != null && workSnap.exists()) batch.update(workRef, updates)
            if (flashSnap != null && flashSnap.exists()) batch.update(flashRef, updates)
            batch.commit().await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "leaveCollab failed for $postId", e)
            false
        }
    }

    /**
     * Allows the primary creator to invite new collaborators or remove existing ones after publishing.
     */
    suspend fun updateCollaboratorsAsCreator(
        postId: String,
        addedCollaborators: List<CollabParticipant> = emptyList(),
        removedUids: List<String> = emptyList()
    ): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (postId.isBlank()) return false
        return try {
            val workRef = collection.document(postId)
            val flashRef = db.collection("flash_videos").document(postId)
            val workSnap = try { workRef.get().await() } catch (_: Exception) { null }
            val flashSnap = try { flashRef.get().await() } catch (_: Exception) { null }
            val sourceSnap = workSnap?.takeIf { it.exists() } ?: flashSnap?.takeIf { it.exists() } ?: return false

            val sourcePost = sourceSnap.toObject(DiscoverPost::class.java)
            val flashObj = if (sourcePost == null) flashSnap?.toObject(com.chroma.studio.model.FlashVideo::class.java) else null
            val creatorUid = sourcePost?.creatorId ?: flashObj?.creatorId ?: return false
            if (creatorUid != currentUid) return false

            val existingCollabs = sourcePost?.collaborators ?: flashObj?.collaborators ?: emptyList()
            val removeSet = removedUids.toSet()
            val keptCollabs = existingCollabs.filterNot { it.uid in removeSet }.toMutableList()
            val keptUids = keptCollabs.map { it.uid }.toMutableSet()
            val normalizedAdditions = normalizeInvitedCollaborators(currentUid, addedCollaborators)
                .filter { it.uid !in keptUids && keptCollabs.size < MAX_COLLAB_PARTICIPANTS }
            for (newP in normalizedAdditions) {
                if (keptCollabs.size < MAX_COLLAB_PARTICIPANTS && keptUids.add(newP.uid)) {
                    keptCollabs.add(newP)
                }
            }
            val nextPending = keptCollabs.filter { CollabInviteStatus.fromWire(it.status) == CollabInviteStatus.PENDING }.map { it.uid }
            val nextAccepted = keptCollabs.filter { CollabInviteStatus.fromWire(it.status) == CollabInviteStatus.ACCEPTED }.map { it.uid }

            val updates = mapOf(
                "collaborators" to serializeCollabParticipants(keptCollabs),
                "pendingCollaboratorIds" to nextPending,
                "collaboratorIds" to nextAccepted
            )
            val batch = db.batch()
            if (workSnap != null && workSnap.exists()) batch.update(workRef, updates)
            if (flashSnap != null && flashSnap.exists()) batch.update(flashRef, updates)
            batch.commit().await()

            val newlyInvitedUids = normalizedAdditions.map { it.uid }
            if (newlyInvitedUids.isNotEmpty()) {
                val isFlash = (flashSnap != null && flashSnap.exists()) || sourcePost?.postType == "video"
                val preview = sourcePost?.thumbnailUrl ?: sourcePost?.imageUrl ?: flashObj?.thumbnailUrl
                val title = sourcePost?.title ?: flashObj?.caption
                NotificationDispatcher.dispatchCollabInviteNotifications(
                    invitedUids = newlyInvitedUids,
                    postId = postId,
                    isFlash = isFlash,
                    previewUrl = preview,
                    titleSnippet = title
                )
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "updateCollaboratorsAsCreator failed for $postId", e)
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
            val targetRequestRef = usersCollection.document(targetUserId).collection("follow_requests").document(currentUserId)

            val targetSnap = targetUserDoc.get().await()
            val isPrivate = (targetSnap.getBoolean("privateAccount") ?: targetSnap.getBoolean("isPrivateAccount")) ?: false

            if (isPrivate) {
                if (targetFollow) {
                    val alreadyFollowing = currentUserFollowingRef.get().await().exists()
                    if (alreadyFollowing) return true

                    // For private accounts: record pending follow request without instant following
                    val reqData = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "requesterUid" to currentUserId
                    )
                    targetRequestRef.set(reqData).await()

                    try {
                        NotificationDispatcher.sendNotification(
                            targetUserId = targetUserId,
                            type = com.chroma.studio.model.NotificationActionType.FOLLOW_REQUEST,
                            actionText = "requested to follow you",
                            targetId = currentUserId
                        )
                    } catch (e: Exception) {
                        Log.w("DiscoverRepository", "Failed to dispatch follow request notification", e)
                    }
                } else {
                    // Cancel follow request if pending, or unfollow if already following
                    try {
                        targetRequestRef.delete().await()
                    } catch (_: Exception) {}

                    val wasFollowing = currentUserFollowingRef.get().await().exists()
                    if (wasFollowing) {
                        db.runTransaction { transaction ->
                            transaction.delete(currentUserFollowingRef)
                            transaction.delete(targetUserFollowersRef)
                            transaction.set(currentUserDoc, mapOf("followingCount" to FieldValue.increment(-1)), SetOptions.merge())
                            transaction.set(targetUserDoc, mapOf("followersCount" to FieldValue.increment(-1)), SetOptions.merge())
                            true
                        }.await()
                    }

                    // Recall/delete pending FOLLOW_REQUEST notification from target user's notifications
                    try {
                        val notifSnapshot = usersCollection.document(targetUserId)
                            .collection("notifications")
                            .whereEqualTo("actorUid", currentUserId)
                            .whereEqualTo("type", "FOLLOW_REQUEST")
                            .get().await()
                        for (doc in notifSnapshot.documents) {
                            doc.reference.delete().await()
                        }
                    } catch (_: Exception) {}
                }
                return true
            }

            // Public Account: direct follow/unfollow transaction
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
                    NotificationDispatcher.sendNotification(
                        targetUserId = targetUserId,
                        type = com.chroma.studio.model.NotificationActionType.FOLLOW,
                        actionText = "started following you",
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

    suspend fun approveFollowRequest(requesterUid: String, currentUserId: String): Boolean {
        if (requesterUid.isBlank() || currentUserId.isBlank() || requesterUid == currentUserId) return false
        return try {
            val requesterFollowingRef = usersCollection.document(requesterUid).collection("following").document(currentUserId)
            val currentUserFollowersRef = usersCollection.document(currentUserId).collection("followers").document(requesterUid)
            val requesterDoc = usersCollection.document(requesterUid)
            val currentUserDoc = usersCollection.document(currentUserId)
            val requestRef = usersCollection.document(currentUserId).collection("follow_requests").document(requesterUid)

            db.runTransaction { transaction ->
                val timestamp = mapOf("timestamp" to System.currentTimeMillis())
                transaction.set(requesterFollowingRef, timestamp)
                transaction.set(currentUserFollowersRef, timestamp)
                transaction.set(requesterDoc, mapOf("followingCount" to FieldValue.increment(1)), SetOptions.merge())
                transaction.set(currentUserDoc, mapOf("followersCount" to FieldValue.increment(1)), SetOptions.merge())
                transaction.delete(requestRef)
                true
            }.await()

            try {
                NotificationDispatcher.sendNotification(
                    targetUserId = requesterUid,
                    type = com.chroma.studio.model.NotificationActionType.FOLLOW,
                    actionText = "accepted your follow request",
                    targetId = currentUserId
                )
            } catch (e: Exception) {
                Log.w("DiscoverRepository", "Failed to dispatch follow accepted notification", e)
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error approving follow request", e)
            false
        }
    }

    suspend fun rejectFollowRequest(requesterUid: String, currentUserId: String): Boolean {
        if (requesterUid.isBlank() || currentUserId.isBlank()) return false
        return try {
            val requestRef = usersCollection.document(currentUserId).collection("follow_requests").document(requesterUid)
            requestRef.delete().await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error rejecting follow request", e)
            false
        }
    }

    suspend fun checkHasFollowRequest(currentUserId: String, targetUserId: String): Boolean {
        if (currentUserId.isBlank() || targetUserId.isBlank()) return false
        return try {
            val requestRef = usersCollection.document(targetUserId).collection("follow_requests").document(currentUserId)
            requestRef.get().await().exists()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String): Boolean {
        val currentlyFollowing = checkIsFollowing(currentUserId, targetUserId)
        return setFollow(currentUserId, targetUserId, !currentlyFollowing)
    }
    
    suspend fun getLikedWorkIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        // Ensure Firebase Auth token is fresh before reading protected subcollection
        try { auth.currentUser?.getIdToken(false)?.await() } catch (_: Exception) {}
        return try {
            val snapshot = usersCollection.document(userId).collection("liked_posts").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DiscoverRepository", "liked_posts fetch failed (may be auth timing), falling back to likedBy scan", e)
            // Fallback: scan community_works for posts where likedBy contains userId
            try {
                val fallbackSnap = collection
                    .whereArrayContains("likedBy", userId)
                    .limit(100)
                    .get().await()
                fallbackSnap.documents.map { it.id }.toSet()
            } catch (fe: Exception) {
                if (fe is kotlinx.coroutines.CancellationException) throw fe
                Log.e("DiscoverRepository", "Error fetching liked work IDs (fallback also failed)", fe)
                emptySet()
            }
        }
    }

    suspend fun hasLiked(workId: String, userId: String): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        return try {
            // 1. Authoritative check on user's own liked_posts subcollection
            val userLikeDoc = usersCollection.document(userId).collection("liked_posts").document(workId).get().await()
            if (userLikeDoc.exists()) return true

            // 2. Check community_works document
            val doc = collection.document(workId).get().await()
            if (doc.exists()) {
                val likedBy = doc.get("likedBy") as? List<String>
                if (likedBy != null) {
                    if (likedBy.contains(userId)) return true
                } else {
                    val likeDoc = collection.document(workId).collection("likes").document(userId).get().await()
                    if (likeDoc.exists()) return true
                }
            } else {
                // 3. Check flash_videos document
                val flashDoc = db.collection("flash_videos").document(workId).get().await()
                if (flashDoc.exists()) {
                    val likedBy = flashDoc.get("likedBy") as? List<String>
                    if (likedBy?.contains(userId) == true) return true
                    val flashLikeDoc = flashDoc.reference.collection("likes").document(userId).get().await()
                    if (flashLikeDoc.exists()) return true
                }
            }
            false
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

    suspend fun getUserByEmail(email: String): UserProfile? {
        if (email.isBlank()) return null
        val cleanEmail = email.trim().lowercase()
        return try {
            val query = usersCollection.whereEqualTo("email", cleanEmail).limit(1).get().await()
            val doc = query.documents.firstOrNull()
            var prof = doc?.toObject(UserProfile::class.java)
            if (prof == null && cleanEmail != email.trim()) {
                val fallback = usersCollection.whereEqualTo("email", email.trim()).limit(1).get().await()
                prof = fallback.documents.firstOrNull()?.toObject(UserProfile::class.java)
            }
            if (prof != null) {
                profileCache[prof.uid] = prof
            }
            prof
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting user by email", e)
            null
        }
    }

    suspend fun getUserByGoogleId(googleId: String): UserProfile? {
        if (googleId.isBlank()) return null
        return try {
            val query = usersCollection.whereEqualTo("googleId", googleId.trim()).limit(1).get().await()
            val doc = query.documents.firstOrNull()
            val prof = doc?.toObject(UserProfile::class.java)
            if (prof != null) {
                profileCache[prof.uid] = prof
            }
            prof
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error getting user by googleId", e)
            null
        }
    }

    suspend fun registerDetailedUser(
        uid: String, 
        displayName: String, 
        username: String, 
        dob: Long?, 
        avatarUrl: String,
        interests: List<String> = emptyList(),
        gender: String = "",
        agreedToTermsAt: Long? = System.currentTimeMillis(),
        email: String = "",
        googleId: String = "",
        bio: String = "",
        isOnboarded: Boolean = true,
        customEmail: String = ""
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val cleanUsername = username.trim().lowercase()
            val finalCustomEmail = if (customEmail.isNotBlank()) customEmail.trim().lowercase() else "${cleanUsername}@oscellion.com"
            var createdProfile: UserProfile? = null

            // Check availability and register atomically in transaction
            db.runTransaction { transaction ->
                val usernameRef = usernamesCollection.document(cleanUsername)
                val userRef = usersCollection.document(uid)
                
                val usernameDoc = transaction.get(usernameRef)
                if (usernameDoc.exists() && usernameDoc.getString("uid") != uid) {
                    throw Exception("Username @$cleanUsername is already taken")
                }
                
                val profile = UserProfile(
                    uid = uid,
                    username = cleanUsername,
                    displayName = displayName.trim(),
                    bio = bio.trim(),
                    dob = dob,
                    dateOfBirth = dob,
                    avatarUrl = avatarUrl.trim(),
                    interests = interests,
                    gender = gender,
                    agreedToTermsAt = agreedToTermsAt ?: now,
                    createdAt = now,
                    online = true,
                    lastSeen = now,
                    followersCount = 0,
                    followingCount = 0,
                    email = email.trim().lowercase(),
                    customEmail = finalCustomEmail,
                    googleId = googleId.trim(),
                    isOnboarded = isOnboarded,
                    publicKey = com.chroma.studio.utils.CryptoHelper.getPublicKeyBase64()
                )
                
                transaction.set(usernameRef, mapOf(
                    "uid" to uid,
                    "createdAt" to now,
                    "customEmail" to finalCustomEmail,
                    "email" to email.trim().lowercase()
                ))
                transaction.set(userRef, profile)
                createdProfile = profile
            }.await()
            
            createdProfile?.let { profileCache[uid] = it }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error registering detailed user", e)
            false
        }
    }

    suspend fun updateUserProfile(updatedProfile: UserProfile): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: updatedProfile.uid.ifBlank { null }
                ?: return Result.failure(IllegalStateException("User is not authenticated"))

            val cleanUsername = updatedProfile.username.trim().lowercase()
            val cleanDisplayName = updatedProfile.displayName.trim()

            validateProfileHandleUpdate(cleanDisplayName, cleanUsername)?.let { errMsg ->
                return Result.failure(IllegalArgumentException(errMsg))
            }

            // Fetch current profile (bypassing stale cache if needed) to check username & staff role
            val currentProfile = try {
                usersCollection.document(uid).get().await().toUserProfileSafe()
            } catch (_: Exception) {
                null
            } ?: getUserProfile(uid)

            val oldUsername = currentProfile?.username?.trim()?.lowercase()
            val usernameChanged = !oldUsername.isNullOrBlank() && oldUsername != cleanUsername
            val safeRole = resolveSafeProfileRole(currentProfile?.role, updatedProfile.role)
            val now = System.currentTimeMillis()

            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val newUsernameRef = usernamesCollection.document(cleanUsername)

                // Perform ALL reads before ANY writes per Firestore transaction requirements
                val newUsernameDoc = transaction.get(newUsernameRef)
                val oldUsernameRef = if (usernameChanged) usernamesCollection.document(oldUsername!!) else null
                val oldUsernameDoc = oldUsernameRef?.let { transaction.get(it) }

                if (newUsernameDoc.exists() && newUsernameDoc.getString("uid") != uid) {
                    throw IllegalArgumentException("Username @$cleanUsername is already taken")
                }

                if (usernameChanged) {
                    transaction.set(newUsernameRef, mapOf("uid" to uid, "updatedAt" to now))
                    // Only delete old username index doc if it actually exists and is owned by this user
                    if (oldUsernameDoc != null && oldUsernameDoc.exists() && oldUsernameDoc.getString("uid") == uid) {
                        transaction.delete(oldUsernameRef)
                    }
                } else if (cleanUsername.isNotBlank()) {
                    transaction.set(newUsernameRef, mapOf("uid" to uid, "updatedAt" to now), SetOptions.merge())
                }

                // Prepare safe update map (never clobbering immutable verified/isPro/banned/staff fields)
                val updates = mutableMapOf<String, Any?>(
                    "uid" to uid,
                    "displayName" to cleanDisplayName,
                    "username" to cleanUsername,
                    "avatarUrl" to updatedProfile.avatarUrl.trim(),
                    "bannerUrl" to updatedProfile.bannerUrl.trim(),
                    "bannerWorkId" to updatedProfile.bannerWorkId,
                    "bio" to updatedProfile.bio.trim(),
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
                    "privateAccount" to updatedProfile.isPrivateAccount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Only include role if not a protected staff role and changed or non-empty
                if (!isPrivilegedAccountRole(currentProfile?.role)) {
                    updates["role"] = safeRole
                }

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

            profileCache[uid] = updatedProfile

            // Notify any newly mentioned users in profile bio immediately
            try {
                NotificationDispatcher.dispatchMentionNotifications(
                    texts = listOf(updatedProfile.bio),
                    previousTexts = listOf(currentProfile?.bio),
                    surface = MentionSurface.PROFILE,
                    targetId = uid,
                    targetPreviewUrl = updatedProfile.avatarUrl.ifBlank { null },
                    detailSnippet = updatedProfile.bio.trim()
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
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
        profileCache[uid]?.let { cached ->
            com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(
                com.chroma.studio.ChromaApp.instance,
                cached
            )
            return cached
        }
        OfflineDataCacheManager.getUserProfile(com.chroma.studio.ChromaApp.instance, uid)?.let { diskCached ->
            profileCache[uid] = diskCached
        }
        return try {
            val doc = usersCollection.document(uid).get().await()
            val profile = doc.toUserProfileSafe()
            if (profile != null) {
                profileCache[uid] = profile
                com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
                com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(
                    com.chroma.studio.ChromaApp.instance,
                    profile
                )
                com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(profile)
                OfflineDataCacheManager.saveUserProfile(com.chroma.studio.ChromaApp.instance, profile)
            }
            profile ?: profileCache[uid]
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val fallback = profileCache[uid] ?: OfflineDataCacheManager.getUserProfile(com.chroma.studio.ChromaApp.instance, uid)
            com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(
                com.chroma.studio.ChromaApp.instance,
                fallback
            )
            fallback
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
                val profile = snapshot.toUserProfileSafe()
                if (profile != null) {
                    profileCache[uid] = profile
                    com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
                    com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(
                        com.chroma.studio.ChromaApp.instance,
                        profile
                    )
                    com.chroma.studio.data.OnlineStatusManager.seedProfilePresence(profile)
                }
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun invalidateProfile(uid: String) {
        if (uid.isNotBlank()) {
            profileCache.remove(uid)
        }
    }

    fun clearProfileCache() {
        profileCache.clear()
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
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val cleanQuery = trimmed.removePrefix("@").trim()
        if (cleanQuery.isBlank()) return emptyList()

        return try {
            val lower = cleanQuery.lowercase()
            val upper = cleanQuery.uppercase()
            val capitalized = cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val words = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val titleCasedWords = words.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            // Distinct prefix variants to query across Firestore indexes
            val prefixVariants = mutableSetOf(cleanQuery, lower, capitalized, upper, titleCasedWords)
            if (words.size > 1) {
                for (w in words) {
                    if (w.length >= 2) {
                        prefixVariants.add(w)
                        prefixVariants.add(w.lowercase())
                        prefixVariants.add(w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                    }
                }
            }

            val collectedProfiles = coroutineScope {
                val deferredList = mutableListOf<kotlinx.coroutines.Deferred<List<UserProfile>>>()

                // 1. Query username prefix for each variant
                val usernameVariants = prefixVariants.map { it.lowercase() }.toSet()
                for (v in usernameVariants) {
                    deferredList.add(async {
                        try {
                            usersCollection
                                .orderBy("username")
                                .startAt(v)
                                .endAt(v + "\uf8ff")
                                .limit(25)
                                .get()
                                .await()
                                .toObjects(UserProfile::class.java)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList()
                        }
                    })
                }

                // 2. Query displayName prefix for each variant
                for (v in prefixVariants) {
                    deferredList.add(async {
                        try {
                            usersCollection
                                .orderBy("displayName")
                                .startAt(v)
                                .endAt(v + "\uf8ff")
                                .limit(25)
                                .get()
                                .await()
                                .toObjects(UserProfile::class.java)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList()
                        }
                    })
                }

                // 3. Query broad user pool to catch middle-name/substring/bio matches
                deferredList.add(async {
                    try {
                        usersCollection
                            .limit(80)
                            .get()
                            .await()
                            .toObjects(UserProfile::class.java)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                })

                deferredList.awaitAll().flatten()
            }

            // Deduplicate by UID (and filter out blanks)
            val uniqueCandidates = collectedProfiles
                .filter { it.uid.isNotBlank() }
                .distinctBy { it.uid }

            // Filter candidates: candidate must match query case-insensitively
            // in username, displayName, bio, customEmail, or individual words
            val filtered = uniqueCandidates.filter { user ->
                val uName = user.username.lowercase()
                val dName = user.displayName.lowercase()
                val bio = user.bio.lowercase()
                val email = user.customEmail.lowercase()

                // 1. Single or full query substring match
                if (uName.contains(lower) || dName.contains(lower) || bio.contains(lower) || email.contains(lower)) {
                    return@filter true
                }

                // 2. Multi-word: All query words match in username or displayName
                if (words.size > 1 && words.all { w ->
                    val wl = w.lowercase()
                    uName.contains(wl) || dName.contains(wl)
                }) {
                    return@filter true
                }

                // 3. Any query word of length >= 2 matches word boundary in displayName
                if (words.any { w ->
                    val wl = w.lowercase()
                    wl.length >= 2 && dName.split("\\s+".toRegex()).any { it.startsWith(wl) }
                }) {
                    return@filter true
                }

                false
            }

            // Relevance scoring: lower number = higher ranking
            fun calculateScore(user: UserProfile): Int {
                val uName = user.username.lowercase()
                val dName = user.displayName.lowercase()
                val bio = user.bio.lowercase()

                // Exact matches
                if (uName == lower) return 0
                if (dName == lower) return 1

                // Prefix matches
                if (uName.startsWith(lower)) return 2
                if (dName.startsWith(lower)) return 3

                // Word prefix in displayName (e.g. "Carter" matching "Alex Carter")
                val nameWords = dName.split("\\s+".toRegex())
                if (nameWords.any { it.startsWith(lower) }) return 4

                // Contains in username
                if (uName.contains(lower)) return 5

                // Contains in displayName
                if (dName.contains(lower)) return 6

                // All words match
                if (words.size > 1 && words.all { w -> uName.contains(w.lowercase()) || dName.contains(w.lowercase()) }) return 7

                // Bio or email match
                if (bio.contains(lower) || user.customEmail.lowercase().contains(lower)) return 8

                return 9
            }

            filtered.sortedWith(
                compareBy<UserProfile> { calculateScore(it) }
                    .thenByDescending { it.verified }
                    .thenByDescending { it.followersCount }
                    .thenBy { it.displayName.lowercase() }
            )
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

    /** Scoped username search — used by Flash @mention autocomplete with case-insensitive matching and relevance ranking. */
    suspend fun searchUsersByUsername(usernamePrefix: String): List<UserProfile> {
        val clean = usernamePrefix.trim().removePrefix("@")
        if (clean.isBlank()) return emptyList()
        val lower = clean.lowercase()
        return try {
            coroutineScope {
                val d1 = async {
                    try {
                        usersCollection
                            .orderBy("username")
                            .startAt(lower)
                            .endAt(lower + "\uf8ff")
                            .limit(12)
                            .get()
                            .await()
                            .toObjects(UserProfile::class.java)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }
                val d2 = async {
                    if (clean != lower) {
                        try {
                            usersCollection
                                .orderBy("username")
                                .startAt(clean)
                                .endAt(clean + "\uf8ff")
                                .limit(12)
                                .get()
                                .await()
                                .toObjects(UserProfile::class.java)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList()
                        }
                    } else emptyList()
                }
                val all = (d1.await() + d2.await())
                    .filter { it.uid.isNotBlank() }
                    .distinctBy { it.uid }

                all.sortedWith(
                    compareBy<UserProfile> {
                        val u = it.username.lowercase()
                        when {
                            u == lower -> 0
                            u.startsWith(lower) -> 1
                            u.contains(lower) -> 2
                            else -> 3
                        }
                    }
                        .thenByDescending { it.verified }
                        .thenByDescending { it.followersCount }
                ).take(8)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    // ── Trending Config & Posts ───────────────────────────────────────────────
    suspend fun getTrendingConfig(): com.chroma.studio.model.TrendingConfig {
        return try {
            val snap = db.collection("app_config").document("trending").get().await()
            if (snap.exists()) {
                val modeRaw = snap.getString("mode")
                val mode = if (modeRaw == "curated_only") "curated_only" else "curated_first"
                val maxSpot = (snap.getLong("maxSpotlight") ?: 6L).toInt().coerceIn(3, 20)
                com.chroma.studio.model.TrendingConfig(mode = mode, maxSpotlight = maxSpot).also {
                    OfflineDataCacheManager.saveTrendingConfig(com.chroma.studio.ChromaApp.instance, it)
                }
            } else {
                OfflineDataCacheManager.getTrendingConfig(com.chroma.studio.ChromaApp.instance)
                    ?: com.chroma.studio.model.TrendingConfig()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            OfflineDataCacheManager.getTrendingConfig(com.chroma.studio.ChromaApp.instance)
                ?: com.chroma.studio.model.TrendingConfig()
        }
    }

    suspend fun getTrendingWorks(
        limit: Long = 30,
        config: com.chroma.studio.model.TrendingConfig? = null
    ): List<DiscoverPost> {
        return try {
            val resolvedConfig = config ?: getTrendingConfig()
            coroutineScope {
                val pinnedPostsDeferred = async {
                    try {
                        collection.whereEqualTo("isTrending", true).limit(20).get().await()
                            .documents.mapNotNull { doc ->
                                val p = doc.toObject(DiscoverPost::class.java)
                                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: p?.commentsCount?.toLong() ?: 0L).toInt()
                                p?.copy(
                                    id = if (doc.id.isNotBlank()) doc.id else p.id,
                                    commentsCount = cCount,
                                    commentCount = cCount
                                )
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }
                val pinnedFlashDeferred = async {
                    try {
                        db.collection("flash_videos").whereEqualTo("isTrending", true).limit(20).get().await()
                            .documents.mapNotNull { doc ->
                                val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                                if (fv != null) mapFlashToDiscoverPost(fv, doc.id) else null
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }
                val organicPostsDeferred = async {
                    if (resolvedConfig.mode == "curated_only") emptyList()
                    else try {
                        collection.orderBy("likesCount", Query.Direction.DESCENDING).limit(limit).get().await()
                            .documents.mapNotNull { doc ->
                                val p = doc.toObject(DiscoverPost::class.java)
                                val cCount = (doc.getLong("commentsCount") ?: doc.getLong("commentCount") ?: p?.commentsCount?.toLong() ?: 0L).toInt()
                                p?.copy(
                                    id = if (doc.id.isNotBlank()) doc.id else p.id,
                                    commentsCount = cCount,
                                    commentCount = cCount
                                )
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }
                val organicFlashDeferred = async {
                    if (resolvedConfig.mode == "curated_only") emptyList()
                    else try {
                        db.collection("flash_videos").orderBy("likesCount", Query.Direction.DESCENDING).limit(limit).get().await()
                            .documents.mapNotNull { doc ->
                                val fv = doc.toObject(com.chroma.studio.model.FlashVideo::class.java)
                                if (fv != null) mapFlashToDiscoverPost(fv, doc.id) else null
                            }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                }

                val allFetched = (
                    pinnedPostsDeferred.await() +
                        pinnedFlashDeferred.await() +
                        organicPostsDeferred.await() +
                        organicFlashDeferred.await()
                    )
                    .distinctBy { it.id }
                    .filter { !it.isArchived && !it.isTakedown && !it.trendingExcluded }
                    .filter { resolvedConfig.mode != "curated_only" || it.isTrending }

                val sorted = allFetched.sortedWith(
                    compareByDescending<DiscoverPost> { it.isTrending }
                        .thenBy { if (it.isTrending) (it.trendingOrder ?: 9999) else Int.MAX_VALUE }
                        .thenByDescending { it.likesCount + it.effectiveCommentsCount * 2 + it.remixCount * 3 }
                ).take(limit.toInt())

                val hydratedTrending = hydrateAuthorProfiles(sorted)
                if (hydratedTrending.isNotEmpty()) {
                    OfflineDataCacheManager.saveTrendingPosts(com.chroma.studio.ChromaApp.instance, hydratedTrending)
                }
                hydratedTrending
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching trending works", e)
            OfflineDataCacheManager.getTrendingPosts(com.chroma.studio.ChromaApp.instance)
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
        if (userId.isBlank()) return emptySet()
        // Ensure Firebase Auth token is fresh before reading protected subcollection
        try { auth.currentUser?.getIdToken(false)?.await() } catch (_: Exception) {}
        return try {
            val snapshot = usersCollection.document(userId).collection("saved_posts").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DiscoverRepository", "saved_posts fetch failed (may be auth timing), falling back to savedBy scan", e)
            try {
                val fallbackSnap = collection
                    .whereArrayContains("savedBy", userId)
                    .limit(100)
                    .get().await()
                fallbackSnap.documents.map { it.id }.toSet()
            } catch (fe: Exception) {
                if (fe is kotlinx.coroutines.CancellationException) throw fe
                emptySet()
            }
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
