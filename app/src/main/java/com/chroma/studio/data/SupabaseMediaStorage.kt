package com.chroma.studio.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.chroma.studio.utils.ImageUtils
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resilient, high-performance Supabase storage manager for profile avatars and cover banners.
 *
 * Features:
 * - Multi-bucket resolution: automatically tries candidates ("avatars", "avatar", "profiles" for avatars;
 *   "banners", "banner", "avatars" for banners).
 * - Cache busting: stores files with unique timestamp paths ($uid/avatar_$timestamp.jpg) so Coil,
 *   Glide, and Supabase CDN immediately show updated artwork.
 * - Aspect ratio integrity: avatars are squared (320x320), banners preserve landscape (1080x450).
 * - EXIF orientation correction for mobile camera captures.
 * - Automatic fallback: falls back to Cloudinary CDN if Supabase storage is unavailable or misconfigured.
 */
object SupabaseMediaStorage {
    private const val TAG = "SupabaseMediaStorage"

    // Candidate bucket names in Supabase
    private val AVATAR_BUCKET_CANDIDATES = listOf("avatars", "avatar", "profiles")
    private val BANNER_BUCKET_CANDIDATES = listOf("banners", "banner", "avatars")

    /**
     * Uploads an avatar image from raw bytes to Supabase.
     */
    suspend fun uploadAvatarBytes(uid: String, imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val compressed = ImageUtils.compressAvatarBytesForSupabase(imageBytes) ?: imageBytes
        val filename = "avatar_${System.currentTimeMillis()}.jpg"
        val path = "$uid/$filename"

        for (bucketName in AVATAR_BUCKET_CANDIDATES) {
            try {
                val bucket = SupabaseClient.client.storage[bucketName]
                bucket.upload(path, compressed, upsert = true)
                val publicUrl = bucket.publicUrl(path)
                Log.d(TAG, "Avatar successfully uploaded to Supabase bucket '$bucketName': $publicUrl")
                return@withContext publicUrl
            } catch (e: Exception) {
                Log.w(TAG, "Failed uploading avatar to bucket '$bucketName': ${e.message}")
            }
        }

        // Fallback to Cloudinary CDN if all Supabase candidate buckets fail
        Log.i(TAG, "Falling back to Cloudinary CDN for avatar upload...")
        try {
            val cloudUrl = CloudinaryClient.uploadImage(compressed, folder = "avatars")
            if (!cloudUrl.isNullOrBlank()) {
                Log.d(TAG, "Avatar successfully uploaded via Cloudinary fallback: $cloudUrl")
                return@withContext cloudUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary fallback also failed for avatar", e)
        }

        null
    }

    /**
     * Uploads an avatar from an Android Uri (with EXIF auto-rotation and compression).
     */
    suspend fun uploadAvatarFromUri(context: Context, uid: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val bytes = ImageUtils.compressAvatarForSupabase(context, uri)
        if (bytes == null) {
            Log.e(TAG, "Failed to compress avatar from uri: $uri")
            return@withContext null
        }
        uploadAvatarBytes(uid, bytes)
    }

    /**
     * Uploads a cover banner from raw bytes to Supabase without squaring.
     */
    suspend fun uploadBannerBytes(uid: String, imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val compressed = ImageUtils.compressBannerBytesForSupabase(imageBytes) ?: imageBytes
        val filename = "banner_${System.currentTimeMillis()}.jpg"
        val path = "$uid/$filename"

        for (bucketName in BANNER_BUCKET_CANDIDATES) {
            try {
                val bucket = SupabaseClient.client.storage[bucketName]
                bucket.upload(path, compressed, upsert = true)
                val publicUrl = bucket.publicUrl(path)
                Log.d(TAG, "Banner successfully uploaded to Supabase bucket '$bucketName': $publicUrl")
                return@withContext publicUrl
            } catch (e: Exception) {
                Log.w(TAG, "Failed uploading banner to bucket '$bucketName': ${e.message}")
            }
        }

        // Fallback to Cloudinary CDN if all Supabase candidate buckets fail
        Log.i(TAG, "Falling back to Cloudinary CDN for banner upload...")
        try {
            val cloudUrl = CloudinaryClient.uploadImage(compressed, folder = "banners")
            if (!cloudUrl.isNullOrBlank()) {
                Log.d(TAG, "Banner successfully uploaded via Cloudinary fallback: $cloudUrl")
                return@withContext cloudUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary fallback also failed for banner", e)
        }

        null
    }

    /**
     * Uploads a cover banner from an Android Uri (with EXIF auto-rotation and landscape compression).
     */
    suspend fun uploadBannerFromUri(context: Context, uid: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val bytes = ImageUtils.compressBannerForSupabase(context, uri)
        if (bytes == null) {
            Log.e(TAG, "Failed to compress banner from uri: $uri")
            return@withContext null
        }
        uploadBannerBytes(uid, bytes)
    }
}
