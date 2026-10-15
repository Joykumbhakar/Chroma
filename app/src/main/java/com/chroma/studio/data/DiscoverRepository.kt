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

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth

import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.data.CloudinaryClient

class DiscoverRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("community_works")
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")

    suspend fun getRecentWorks(limit: Long = 10, lastTimestamp: Long? = null): List<DiscoverPost> {
        return try {
            var query = collection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }
            val snapshot = query.get().await()
            val posts = snapshot.toObjects(DiscoverPost::class.java)
            posts.filter { !it.isArchived }
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
        
        // Increment remix count on the original post if this is a remix
        if (!work.remixedFromPostId.isNullOrEmpty()) {
            try {
                collection.document(work.remixedFromPostId).update("remixCount", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                Log.w("DiscoverRepository", "Could not increment remix count", e)
            }
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

    suspend fun incrementDownloadCount(postId: String, userId: String) {
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

    suspend fun fetchPostById(postId: String): DiscoverPost? {
        return try {
            val doc = collection.document(postId).get().await()
            doc.toObject(DiscoverPost::class.java)
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
            val posts = snapshot.toObjects(DiscoverPost::class.java)
            posts.filter { !it.isArchived }
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
            val posts = snapshot.toObjects(DiscoverPost::class.java)
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

    suspend fun toggleLike(workId: String, userId: String, forceLike: Boolean = false): Boolean {
        if (workId.isBlank() || userId.isBlank()) return false
        return try {
            val likeRef = collection.document(workId).collection("likes").document(userId)
            val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(workId)
            val workRef = collection.document(workId)
            
            db.runTransaction { transaction ->
                val workDoc = transaction.get(workRef)
                val currentLikes = workDoc.getLong("likesCount") ?: 0L
                val likeDoc = transaction.get(likeRef)
                
                if (likeDoc.exists()) {
                    if (forceLike) {
                        return@runTransaction true // already liked, do not toggle off
                    }
                    transaction.delete(likeRef)
                    transaction.delete(userLikeRef)
                    transaction.update(workRef, "likesCount", maxOf(0, currentLikes - 1))
                    false
                } else {
                    val now = System.currentTimeMillis()
                    transaction.set(likeRef, mapOf("timestamp" to now))
                    transaction.set(userLikeRef, mapOf("likedAt" to now))
                    transaction.update(workRef, "likesCount", currentLikes + 1)
                    true
                }
            }.await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error toggling like", e)
            false
        }
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

    suspend fun toggleFollow(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val currentUserFollowingRef = usersCollection.document(currentUserId).collection("following").document(targetUserId)
            val targetUserFollowersRef = usersCollection.document(targetUserId).collection("followers").document(currentUserId)
            
            val currentUserDoc = usersCollection.document(currentUserId)
            val targetUserDoc = usersCollection.document(targetUserId)

            db.runTransaction { transaction ->
                val followingDoc = transaction.get(currentUserFollowingRef)
                if (followingDoc.exists()) {
                    // Unfollow
                    transaction.delete(currentUserFollowingRef)
                    transaction.delete(targetUserFollowersRef)
                    transaction.set(currentUserDoc, mapOf("followingCount" to FieldValue.increment(-1)), SetOptions.merge())
                    transaction.set(targetUserDoc, mapOf("followersCount" to FieldValue.increment(-1)), SetOptions.merge())
                } else {
                    // Follow
                    val timestamp = mapOf("timestamp" to System.currentTimeMillis())
                    transaction.set(currentUserFollowingRef, timestamp)
                    transaction.set(targetUserFollowersRef, timestamp)
                    transaction.set(currentUserDoc, mapOf("followingCount" to FieldValue.increment(1)), SetOptions.merge())
                    transaction.set(targetUserDoc, mapOf("followersCount" to FieldValue.increment(1)), SetOptions.merge())
                }
            }.await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error toggling follow", e)
            false
        }
    }
    
    suspend fun hasLiked(workId: String, userId: String): Boolean {
        return try {
            val likeDoc = collection.document(workId).collection("likes").document(userId).get().await()
            likeDoc.exists()
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

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
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

    suspend fun getMutualFollowers(currentUserId: String, targetUserId: String, limit: Long = 3): List<UserProfile> {
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
    suspend fun getTrendingWorks(limit: Long = 5): List<DiscoverPost> {
        return try {
            val snapshot = collection
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .whereEqualTo("isArchived", false)
                .limit(limit)
                .get()
                .await()
            snapshot.toObjects(DiscoverPost::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error fetching trending works", e)
            emptyList()
        }
    }

    suspend fun incrementShareCount(postId: String, userId: String) {
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

    suspend fun toggleSave(workId: String, userId: String): Boolean {
        return try {
            val saveRef = usersCollection.document(userId).collection("saved_posts").document(workId)
            val workRef = collection.document(workId)
            db.runTransaction { transaction ->
                val saveDoc = transaction.get(saveRef)
                val workDoc = transaction.get(workRef)
                val currentSaves = if (workDoc.exists()) (workDoc.getLong("saveCount") ?: 0L) else 0L
                if (saveDoc.exists()) {
                    transaction.delete(saveRef)
                    if (workDoc.exists()) {
                        transaction.update(workRef, "saveCount", maxOf(0, currentSaves - 1))
                    }
                } else {
                    transaction.set(saveRef, mapOf("savedAt" to System.currentTimeMillis()))
                    if (workDoc.exists()) {
                        transaction.update(workRef, "saveCount", currentSaves + 1)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DiscoverRepository", "Error toggling save", e)
            false
        }
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
