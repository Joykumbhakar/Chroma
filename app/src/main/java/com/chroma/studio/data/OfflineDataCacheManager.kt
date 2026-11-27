package com.chroma.studio.data

import android.content.Context
import android.util.Log
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.TrendingConfig
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified Memory + Local Disk Cache for commonly used app data:
 * - Discover & Trending Posts (including text/caption posts, works & metadata)
 * - User Profiles (including verified tones, avatars, bios, last seen)
 * - Story Groups & User Bookmarks/Likes IDs
 *
 * Ensures instant Frame-0 startup and full offline viewing when network is unavailable.
 */
object OfflineDataCacheManager {
    private const val TAG = "OfflineDataCacheManager"
    private const val CACHE_DIR_NAME = "offline_cache"
    private val gson = Gson()

    private val profileMemoryCache = ConcurrentHashMap<String, UserProfile>()
    @Volatile
    private var recentPostsMemory: List<DiscoverPost>? = null
    @Volatile
    private var trendingPostsMemory: List<DiscoverPost>? = null
    @Volatile
    private var trendingConfigMemory: TrendingConfig? = null
    private val userPostsMemory = ConcurrentHashMap<String, List<DiscoverPost>>()
    private val storyGroupsMemory = ConcurrentHashMap<String, List<UserStoryGroup>>()
    @Volatile
    private var profilesDiskLoaded = false

    private fun getCacheDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitize(key: String): String =
        key.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    // =========================================================================
    // WARMUP / PRELOAD
    // =========================================================================

    fun warmupOnStartup(context: Context) {
        try {
            loadAllProfilesFromDisk(context)
            if (recentPostsMemory == null) getRecentPosts(context)
            if (trendingPostsMemory == null) getTrendingPosts(context)
            if (trendingConfigMemory == null) getTrendingConfig(context)
        } catch (e: Exception) {
            Log.w(TAG, "Warmup skipped: ${e.message}")
        }
    }

    // =========================================================================
    // DISCOVER & TRENDING POSTS
    // =========================================================================

    fun saveRecentPosts(context: Context, posts: List<DiscoverPost>) {
        if (posts.isEmpty()) return
        val capped = posts.take(40)
        recentPostsMemory = capped
        writeJson(context, "recent_posts.json", capped)
        // Also extract embedded author info into profile registry if missing
        capped.forEach { post ->
            if (post.authorVerified && !post.authorVerifiedTone.isNullOrBlank()) {
                com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(post.creatorId, post.authorVerifiedTone)
            }
        }
    }

    fun getRecentPosts(context: Context): List<DiscoverPost> {
        recentPostsMemory?.let { return it }
        val loaded: List<DiscoverPost> = readJsonList(
            context,
            "recent_posts.json",
            object : TypeToken<List<DiscoverPost>>() {}.type
        )
        if (loaded.isNotEmpty()) {
            recentPostsMemory = loaded
            loaded.forEach { post ->
                if (post.authorVerified && !post.authorVerifiedTone.isNullOrBlank()) {
                    com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(post.creatorId, post.authorVerifiedTone)
                }
            }
        }
        return loaded
    }

    fun saveTrendingPosts(context: Context, posts: List<DiscoverPost>) {
        if (posts.isEmpty()) return
        val capped = posts.take(30)
        trendingPostsMemory = capped
        writeJson(context, "trending_posts.json", capped)
    }

    fun getTrendingPosts(context: Context): List<DiscoverPost> {
        trendingPostsMemory?.let { return it }
        val loaded: List<DiscoverPost> = readJsonList(
            context,
            "trending_posts.json",
            object : TypeToken<List<DiscoverPost>>() {}.type
        )
        if (loaded.isNotEmpty()) {
            trendingPostsMemory = loaded
        }
        return loaded
    }

    fun saveTrendingConfig(context: Context, config: TrendingConfig) {
        trendingConfigMemory = config
        writeJson(context, "trending_config.json", config)
    }

    fun getTrendingConfig(context: Context): TrendingConfig? {
        trendingConfigMemory?.let { return it }
        return try {
            val file = File(getCacheDir(context), "trending_config.json")
            if (!file.exists()) return null
            gson.fromJson(file.readText(), TrendingConfig::class.java)?.also {
                trendingConfigMemory = it
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveUserPosts(context: Context, creatorId: String, posts: List<DiscoverPost>) {
        if (creatorId.isBlank() || posts.isEmpty()) return
        val capped = posts.take(40)
        userPostsMemory[creatorId] = capped
        writeJson(context, "user_posts_${sanitize(creatorId)}.json", capped)
    }

    fun getUserPosts(context: Context, creatorId: String): List<DiscoverPost> {
        if (creatorId.isBlank()) return emptyList()
        userPostsMemory[creatorId]?.let { return it }
        val loaded: List<DiscoverPost> = readJsonList(
            context,
            "user_posts_${sanitize(creatorId)}.json",
            object : TypeToken<List<DiscoverPost>>() {}.type
        )
        if (loaded.isNotEmpty()) userPostsMemory[creatorId] = loaded
        return loaded
    }

    // =========================================================================
    // SAVED WORK IDS & TEXT DRAFTS / COMMON DATA
    // =========================================================================

    fun saveSavedWorkIds(context: Context, uid: String, ids: Set<String>) {
        if (uid.isBlank()) return
        try {
            context.applicationContext
                .getSharedPreferences("chroma_community_likes_cache", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("cached_saved_works_$uid", HashSet(ids))
                .apply()
        } catch (_: Exception) {}
    }

    fun getSavedWorkIds(context: Context, uid: String): Set<String> {
        if (uid.isBlank()) return emptySet()
        return try {
            context.applicationContext
                .getSharedPreferences("chroma_community_likes_cache", Context.MODE_PRIVATE)
                .getStringSet("cached_saved_works_$uid", emptySet())
                ?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    // =========================================================================
    // USER PROFILES (Memory + Disk Map)
    // =========================================================================

    fun saveUserProfile(context: Context, profile: UserProfile?) {
        if (profile == null || profile.uid.isBlank()) return
        profileMemoryCache[profile.uid] = profile
        com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
        com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(context, profile)
        OnlineStatusManager.seedProfilePresence(profile)
        persistProfilesSnapshot(context)
    }

    fun saveUserProfiles(context: Context, profiles: Collection<UserProfile>) {
        var changed = false
        profiles.forEach { profile ->
            if (profile.uid.isNotBlank()) {
                profileMemoryCache[profile.uid] = profile
                com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(profile)
                com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(context, profile)
                OnlineStatusManager.seedProfilePresence(profile)
                changed = true
            }
        }
        if (changed) {
            persistProfilesSnapshot(context)
        }
    }

    fun getUserProfile(context: Context, uid: String): UserProfile? {
        if (uid.isBlank()) return null
        if (!profilesDiskLoaded) loadAllProfilesFromDisk(context)
        return profileMemoryCache[uid]
    }

    fun getAllCachedProfiles(context: Context): Map<String, UserProfile> {
        if (!profilesDiskLoaded) loadAllProfilesFromDisk(context)
        return profileMemoryCache.toMap()
    }

    private fun loadAllProfilesFromDisk(context: Context) {
        if (profilesDiskLoaded) return
        synchronized(this) {
            if (profilesDiskLoaded) return
            try {
                val file = File(getCacheDir(context), "profiles_cache.json")
                if (file.exists()) {
                    val type = object : TypeToken<List<UserProfile>>() {}.type
                    val list: List<UserProfile> = gson.fromJson(file.readText(), type) ?: emptyList()
                    list.forEach { p ->
                        if (p.uid.isNotBlank()) {
                            profileMemoryCache.putIfAbsent(p.uid, p)
                            com.chroma.studio.ui.components.VerifiedBadgeRegistry.register(p)
                            com.chroma.studio.ui.components.OscellionLuxuryBrandController.updateForProfileIfCurrentUser(context, p)
                            OnlineStatusManager.seedProfilePresence(p)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading cached profiles: ${e.message}")
            } finally {
                profilesDiskLoaded = true
            }
        }
    }

    private fun persistProfilesSnapshot(context: Context) {
        try {
            // Keep up to 150 most recently cached profiles compactly on disk
            val list = profileMemoryCache.values.take(150)
            writeJson(context, "profiles_cache.json", list)
        } catch (_: Exception) {}
    }

    // =========================================================================
    // STORIES CACHE
    // =========================================================================

    fun saveStoryGroups(context: Context, uid: String, groups: List<UserStoryGroup>) {
        if (uid.isBlank() || groups.isEmpty()) return
        storyGroupsMemory[uid] = groups
        writeJson(context, "story_groups_${sanitize(uid)}.json", groups)
    }

    fun getStoryGroups(context: Context, uid: String): List<UserStoryGroup> {
        if (uid.isBlank()) return emptyList()
        storyGroupsMemory[uid]?.let { return it }
        val loaded: List<UserStoryGroup> = readJsonList(
            context,
            "story_groups_${sanitize(uid)}.json",
            object : TypeToken<List<UserStoryGroup>>() {}.type
        )
        if (loaded.isNotEmpty()) storyGroupsMemory[uid] = loaded
        return loaded
    }

    // =========================================================================
    // GENERIC JSON DISK I/O
    // =========================================================================

    private fun writeJson(context: Context, filename: String, payload: Any) {
        try {
            val file = File(getCacheDir(context), filename)
            val tmp = File(getCacheDir(context), "$filename.tmp")
            tmp.writeText(gson.toJson(payload))
            if (!tmp.renameTo(file)) {
                file.writeText(gson.toJson(payload))
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing $filename: ${e.message}")
        }
    }

    private fun <T> readJsonList(context: Context, filename: String, type: java.lang.reflect.Type): List<T> {
        return try {
            val file = File(getCacheDir(context), filename)
            if (!file.exists()) return emptyList()
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading $filename: ${e.message}")
            emptyList()
        }
    }
}
